/*******************************************************************************
 * Copyright (c) 2004, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.inbound;

import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;

/**
 * Bounded gzip decoder for a sealed HTTP/2 request body.
 *
 */
final class H2BoundedGzipDecoder implements H2BoundedDecompressionHandler {

    /** RAS variable */
    private static final TraceComponent tc = Tr.register(H2BoundedGzipDecoder.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    /** Enumeration of the various parsing states */
    private enum PARSE_STATE {
        /** parsing first byte */
        ID1,
        /** parsing second byte */
        ID2,
        /** parsing the compression type flag */
        COMPRESSION,
        /** parsing the gzip header flag */
        FLAG,
        /** parsing the optional file extra flag */
        FEXTRA,
        /** parsing the optional file name */
        FNAME,
        /** parsing the optional file comment */
        FCOMMENT,
        /** parsing the optional file header checksum */
        FHCRC,
        /** parsing is complete */
        DONE
    }

    /** Parse state during the gzip header parsing sequence */
    private PARSE_STATE state = PARSE_STATE.ID1;
    /** Flag found in the header information */
    private byte gzipFlag = -1;
    /** Temp value used at various stages of parsing the header and trailer */
    private int parseInt = 0;
    /** Temp first byte of trailer ints */
    private int parseFirstByte = 0;
    /** Offset used while parsing the gzip header */
    private int parseOffset = 0;
    /** Inflater used for this handler instance */
    private Inflater inflater = null;
    /** Maximum output produced by one pull. */
    private static final int OUTPUT_CHUNK = 16 * 1024;
    /** Checksum utility for this stream */
    private CRC32 checksum = null;

    /** need to reset flag */
    private boolean resetNeededToProceed = false;

    // running counts including resets
    private long countRead = 0;

    // The caller retains the offered array until this decoder needs input again.
    private byte[] boundedInput;
    private int boundedLength;
    private int boundedOffset;
    private boolean boundedEnd;
    private boolean boundedClosed;

    /**
     * Create a decoder for one sealed body pass.
     *
     */
    H2BoundedGzipDecoder() {
        this.inflater = new Inflater(true);
        try {
            this.checksum = new CRC32();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Created a bounded gzip decoder; " + this);
            }
        } catch (RuntimeException | Error failure) {
            this.inflater.end();
            this.inflater = null;
            throw failure;
        }
    }

    /**
     * Query whether the gzip header flag had the FEXTRA bit set.
     *
     * @return boolean
     */
    private boolean isFExtraSet() {
        return ((this.gzipFlag & 4) == 4);
    }

    /**
     * Query whether the gzip header flag had the FNAME bit set.
     *
     * @return boolean
     */
    private boolean isFNameSet() {
        return ((this.gzipFlag & 8) == 8);
    }

    /**
     * Query whether the gzip header flag had the FCOMMENT bit set.
     *
     * @return boolean
     */
    private boolean isFCommentSet() {
        return ((this.gzipFlag & 16) == 16);
    }

    /**
     * Query whether the gzip header flag had the FHCRC bit set.
     *
     * @return boolean
     */
    private boolean isFHCRCSet() {
        return ((this.gzipFlag & 1) == 1);
    }

    /**
     * Utility method to skip a given amount of bytes from the input stream. This
     * will return the index after the skip (which may or may not be able to
     * skip the entire requested amount).
     *
     * @param data
     * @param pos
     * @param count
     * @return pos after skip
     */
    private int skip(byte[] data, int pos, int count, int end) {
        int remaining = end - pos;
        if (remaining >= count) {
            this.parseInt = 0;
            return (pos + count);
        }
        // didn't have the entire amount, store the remaining length to skip later
        this.parseInt = (count - remaining);
        return end;
    }

    /**
     * Skip until it runs out of input data or finds the target byte.
     *
     * @param data
     * @param pos
     * @param target
     * @return pos
     */
    private int skipPast(byte[] data, int pos, byte target, int end) {
        int index = pos;
        while (index < end) {
            if (target == data[index++]) {
                return index;
            }
        }
        return index;
    }

    /**
     * Parse the gzip header information , if it exists, from the input buffer.
     * This handles running out of data at any point in the gzip header sequence
     * and picking up with future buffers.
     *
     * @param data
     * @throws DataFormatException
     *             if the header data is corrupt
     */
    private int parseHeader(byte[] data, int start, int end) throws DataFormatException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsing gzip header; state=" + this.state);
        }
        int pos = start;
        while (pos < end && PARSE_STATE.DONE != this.state) {
            if (PARSE_STATE.FLAG == this.state && -1 != this.gzipFlag && this.parseInt > 0) {
                pos = skip(data, pos, this.parseInt, end);
                if (this.parseInt == 0) {
                    this.state = isFExtraSet() ? PARSE_STATE.FEXTRA
                                    : isFNameSet() ? PARSE_STATE.FNAME
                                    : isFCommentSet() ? PARSE_STATE.FCOMMENT
                                    : isFHCRCSet() ? PARSE_STATE.FHCRC : PARSE_STATE.DONE;
                    this.parseOffset = 0;
                }
                continue;
            }
            if (PARSE_STATE.FEXTRA == this.state && this.parseOffset == 2 && this.parseInt > 0) {
                pos = skip(data, pos, this.parseInt, end);
                if (this.parseInt == 0) {
                    this.state = isFNameSet() ? PARSE_STATE.FNAME
                                    : isFCommentSet() ? PARSE_STATE.FCOMMENT
                                    : isFHCRCSet() ? PARSE_STATE.FHCRC : PARSE_STATE.DONE;
                    this.parseOffset = 0;
                }
                continue;
            }
            byte b = data[pos++];
            if (PARSE_STATE.ID1 == this.state) {
                if ((byte) 0x1F != b) {
                    throw new DataFormatException("Invalid gzip header, first byte=" + b);
                }
                this.state = PARSE_STATE.ID2;
            } else if (PARSE_STATE.ID2 == this.state) {
                if ((byte) 0x8B != b) {
                    throw new DataFormatException("Invalid gzip header, second byte=" + b);
                }
                this.state = PARSE_STATE.COMPRESSION;
            } else if (PARSE_STATE.COMPRESSION == this.state) {
                if (Deflater.DEFLATED != b) {
                    throw new DataFormatException("Invalid gzip compression method=" + b);
                }
                this.state = PARSE_STATE.FLAG;
            } else if (PARSE_STATE.FLAG == this.state) {
                if (-1 == this.gzipFlag) {
                    // after saving the extra flag byte, skip the next 6 bytes
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "gzip header flag=" + b);
                    }
                    this.gzipFlag = b;
                    pos = skip(data, pos, 6, end);
                } else {
                    // previously saw the flag but didn't skip a full 6
                    pos = skip(data, pos, this.parseInt, end);
                }
                if (0 == this.parseInt) {
                    if (isFExtraSet()) {
                        this.state = PARSE_STATE.FEXTRA;
                    } else if (isFNameSet()) {
                        this.state = PARSE_STATE.FNAME;
                    } else if (isFCommentSet()) {
                        this.state = PARSE_STATE.FCOMMENT;
                    } else if (isFHCRCSet()) {
                        this.state = PARSE_STATE.FHCRC;
                    } else {
                        this.state = PARSE_STATE.DONE;
                    }
                    this.parseOffset = 0;
                }
            } else if (PARSE_STATE.FEXTRA == this.state) {
                // FEXTRA has length in 2 bytes, then that many bytes
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Parsing FEXTRA data, offset=" + this.parseOffset);
                }
                if (0 == this.parseOffset) {
                    this.parseInt = b & 0xff;
                    this.parseOffset++;
                    continue;
                } else if (1 == this.parseOffset) {
                    this.parseInt = ((b & 0xff) << 8) | this.parseInt;
                    this.parseOffset++;
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "FEXTRA length is " + this.parseInt);
                }
                pos = skip(data, pos, this.parseInt, end);
                if (0 == this.parseInt) {
                    if (isFNameSet()) {
                        this.state = PARSE_STATE.FNAME;
                    } else if (isFCommentSet()) {
                        this.state = PARSE_STATE.FCOMMENT;
                    } else if (isFHCRCSet()) {
                        this.state = PARSE_STATE.FHCRC;
                    } else {
                        this.state = PARSE_STATE.DONE;
                    }
                    this.parseOffset = 0;
                }
            } else if (PARSE_STATE.FNAME == this.state) {
                // FNAME is a zero delimited file name
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Parsing FNAME data");
                }
                boolean delimiter = b == 0;
                if (!delimiter) {
                    pos = skipPast(data, pos, (byte) 0, end);
                    delimiter = pos > 0 && data[pos - 1] == 0;
                }
                if (end == pos && !delimiter) {
                    return pos;
                }
                if (isFCommentSet()) {
                    this.state = PARSE_STATE.FCOMMENT;
                } else if (isFHCRCSet()) {
                    this.state = PARSE_STATE.FHCRC;
                } else {
                    this.state = PARSE_STATE.DONE;
                }
            } else if (PARSE_STATE.FCOMMENT == this.state) {
                // FCOMMENT is a zero delimited file comment
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Parsing FCOMMENT data");
                }
                boolean delimiter = b == 0;
                if (!delimiter) {
                    pos = skipPast(data, pos, (byte) 0, end);
                    delimiter = pos > 0 && data[pos - 1] == 0;
                }
                if (end == pos && !delimiter) {
                    return pos;
                }
                if (isFHCRCSet()) {
                    this.state = PARSE_STATE.FHCRC;
                } else {
                    this.state = PARSE_STATE.DONE;
                }
            } else if (PARSE_STATE.FHCRC == this.state) {
                // FHCRC has 2 extra bytes (checksum of all gzip header bytes)
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Parsing FHCRC data; offset=" + this.parseOffset);
                }
                this.parseOffset++;
                if (2 > this.parseOffset) {
                    continue;
                }
                this.state = PARSE_STATE.DONE;
            }
        } // end loop

        if (PARSE_STATE.DONE == this.state) {
            this.parseOffset = 0;
            this.parseInt = 0;
        }
        return pos;
    }

    /**
     * Parse past the GZIP trailer information. This is the two ints for the CRC32
     * checksum validation.
     *
     * @param input
     * @param inOffset
     * @param end end of the offered input segment
     * @return new offset
     * @throws DataFormatException
     *             - if the trailer data is wrong
     */
    private int parseTrailer(byte[] input, int inOffset, int end) throws DataFormatException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsing trailer, offset=" + this.parseOffset + " val=" + this.parseInt);
        }
        int offset = inOffset;
        long val = 0L;
        // bytes are in lowest order first
        while (this.parseOffset < 8 && offset < end) {
            switch (this.parseOffset) {
                // even bytes are just going to save the first byte of an int
                case 0:
                case 2:
                case 4:
                case 6:
                    this.parseFirstByte = input[offset] & 0xff;
                    break;
                // bytes 1 and 5 are the 2nd byte of that int
                case 1:
                case 5:
                    this.parseInt = ((input[offset] & 0xff) << 8) | this.parseFirstByte;
                    break;
                // 3 and 7 mark the final bytes of the 2 int values
                case 3:
                    // reached the end of the checksum int
                    val = ((input[offset] & 0xff) << 8) | this.parseFirstByte;
                    val = (val << 16) | this.parseInt;
                    if (this.checksum.getValue() != val) {
                        String msg = "Checksum does not match; crc=" + this.checksum.getValue() + " trailer=" + val;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, msg);
                        }
                        throw new DataFormatException(msg);
                    }
                    break;
                case 7:
                    // reached the end of the "num bytes" int
                    val = ((input[offset] & 0xff) << 8) | this.parseFirstByte;
                    val = (val << 16) | this.parseInt;
                    long written = this.inflater.getBytesWritten();
                    if ((written & 0xffffffffL) != val) {
                        String msg = "BytesWritten does not match; inflater=" + this.inflater.getBytesWritten() + " trailer=" + val;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, msg);
                        }
                        throw new DataFormatException(msg);
                    }

                    // having fully parsed the trailer, if we are going to re-enter decompression, then we need to reset
                    this.resetNeededToProceed = true;

                    break;
                default:
                    break;
            }
            offset++;
            this.parseOffset++;
        }
        return offset;
    }

    private void reset() {
        // re-entered after fully processing a packet, so reset to continue on
        this.state = PARSE_STATE.ID1;
        this.gzipFlag = -1;
        this.parseInt = 0;
        this.parseFirstByte = 0;
        this.parseOffset = 0;
        this.checksum = new CRC32();

        this.countRead += this.inflater.getBytesRead();
        this.inflater.reset();

        this.resetNeededToProceed = false;
    }

    @Override
    public void offer(byte[] input, int length) {
        if (boundedClosed || boundedEnd || boundedInput != null || input == null || length <= 0 || length > input.length) {
            throw new IllegalStateException("Gzip input is not ready for another bounded segment");
        }
        boundedInput = input;
        boundedLength = length;
        boundedOffset = 0;
    }

    @Override
    public boolean needsInput() {
        return boundedInput == null;
    }

    @Override
    public void endInput() {
        boundedEnd = true;
    }

    @Override
    public int pull(byte[] output, int offset, int length) throws DataFormatException {
        if (boundedClosed || output == null || offset < 0 || length < 0 || offset > output.length - length) {
            throw new IllegalStateException("Invalid bounded gzip pull");
        }
        if (length == 0) {
            return 0;
        }
        int wanted = Math.min(length, OUTPUT_CHUNK);
        while (true) {
            if (boundedInput == null) {
                if (boundedEnd) {
                    if (resetNeededToProceed) {
                        return -1; // at least one member and its trailer were validated
                    }
                    throw new DataFormatException("Truncated gzip body");
                }
                return 0;
            }
            if (boundedOffset > boundedLength) {
                throw new DataFormatException("Gzip parser passed the offered input length");
            }
            if (resetNeededToProceed) {
                if (boundedOffset == boundedLength) {
                    boundedInput = null;
                    continue;
                }
                reset(); // next concatenated member, matching the existing handler
            }
            if (state != PARSE_STATE.DONE) {
                boundedOffset = parseHeader(boundedInput, boundedOffset, boundedLength);
                if (state != PARSE_STATE.DONE) {
                    if (boundedOffset == boundedLength) {
                        boundedInput = null;
                    }
                    continue;
                }
            }
            if (!inflater.finished()) {
                if (inflater.needsInput()) {
                    if (boundedOffset < boundedLength) {
                        inflater.setInput(boundedInput, boundedOffset, boundedLength - boundedOffset);
                    }
                }
                long before = inflater.getBytesRead();
                int produced = inflater.inflate(output, offset, wanted);
                boundedOffset += (int) (inflater.getBytesRead() - before);
                if (produced > 0) {
                    checksum.update(output, offset, produced);
                    return produced;
                }
                if (!inflater.finished()) {
                    if (inflater.needsDictionary()) {
                        throw new DataFormatException("Gzip body requires a dictionary");
                    }
                    if (inflater.needsInput() && boundedOffset == boundedLength) {
                        boundedInput = null;
                        continue;
                    }
                    throw new DataFormatException("Gzip body made no progress");
                }
            }
            boundedOffset = parseTrailer(boundedInput, boundedOffset, boundedLength);
            if (parseOffset < 8) {
                if (boundedOffset == boundedLength) {
                    boundedInput = null;
                }
                continue;
            }
            if (boundedOffset == boundedLength) {
                boundedInput = null;
            }
        }
    }

    @Override
    public void close() {
        boundedClosed = true;
        boundedInput = null;
        this.inflater.end();
    }

    @Override
    public long getBytesRead() {
        return this.countRead + this.inflater.getBytesRead();
    }

}
