/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http.channel.compression;

import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * Handler for the gzip decompression method.
 * 
 */
public class GzipInputHandler implements DecompressionHandler {

    /** RAS variable */
    private static final TraceComponent tc = Tr.register(GzipInputHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

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
    /** Output buffer to decompress data into */
    private final byte[] buf = new byte[16384];
    /** Checksum utility for this stream */
    private CRC32 checksum = null;

    /** need to reset flag */
    private boolean resetNeededToProceed = false;

    // running counts including resets
    private long countRead = 0;
    private long countWritten = 0;

    /**
     * Create a handler to apply the gzip decompression method.
     * 
     */
    public GzipInputHandler() {
        this.inflater = new Inflater(true);
        this.checksum = new CRC32();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created a gzip input handler; " + this);
        }
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#isEnabled()
     */
    public boolean isEnabled() {
        return true;
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
    private int skip(byte[] data, int pos, int count) {
        int remaining = data.length - pos;
        if (remaining >= count) {
            this.parseInt = 0;
            return (pos + count);
        }
        // didn't have the entire amount, store the remaining length to skip later
        this.parseInt = (count - remaining);
        return data.length;
    }

    /**
     * Skip until it runs out of input data or finds the target byte.
     * 
     * @param data
     * @param pos
     * @param target
     * @return pos
     */
    private int skipPast(byte[] data, int pos, byte target) {
        int index = pos;
        while (index < data.length) {
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
    private int parseHeader(byte[] data) throws DataFormatException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsing gzip header; state=" + this.state);
        }
        int pos = 0;
        for (; pos < data.length && PARSE_STATE.DONE != this.state;) {
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
                    pos = skip(data, pos, 6);
                } else {
                    // previously saw the flag but didn't skip a full 6
                    pos = skip(data, pos, this.parseInt);
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
                    this.parseInt = b;
                    this.parseOffset++;
                    continue;
                } else if (1 == this.parseOffset) {
                    this.parseInt = (b << 8) | this.parseInt;
                    this.parseOffset++;
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "FEXTRA length is " + this.parseInt);
                }
                pos = skip(data, pos, this.parseInt);
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
                if (0 != b) {
                    pos = skipPast(data, pos, (byte) 0);
                }
                if (data.length == pos) {
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
                if (0 != b) {
                    pos = skipPast(data, pos, (byte) 0);
                }
                if (data.length == pos) {
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
     * @param list
     * @return new offset
     * @throws DataFormatException
     *             - if the trailer data is wrong
     */
    private int parseTrailer(byte[] input, int inOffset, List<WsByteBuffer> list) throws DataFormatException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Parsing trailer, offset=" + this.parseOffset + " val=" + this.parseInt);
        }
        int offset = inOffset;
        long val = 0L;
        // bytes are in lowest order first
        while (8 > this.parseOffset && offset < input.length) {
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
                        release(list);
                        throw new DataFormatException(msg);
                    }
                    break;
                case 7:
                    // reached the end of the "num bytes" int
                    val = ((input[offset] & 0xff) << 8) | this.parseFirstByte;
                    val = (val << 16) | this.parseInt;
                    if (this.inflater.getBytesWritten() != val) {
                        String msg = "BytesWritten does not match; inflater=" + this.inflater.getBytesWritten() + " trailer=" + val;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, msg);
                        }
                        release(list);
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

    /**
     * Release a list of buffers.
     * 
     * @param list
     */
    private void release(List<WsByteBuffer> list) {
        while (!list.isEmpty()) {
            list.remove(0).release();
        }
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
        this.countWritten += this.inflater.getBytesWritten();
        this.inflater.reset();

        this.resetNeededToProceed = false;
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#decompress(
     * com.ibm.wsspi.bytebuffer.WsByteBuffer)
     */
    public List<WsByteBuffer> decompress(WsByteBuffer inputBuffer) throws DataFormatException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "decompress", inputBuffer);
        }
        List<WsByteBuffer> list = new LinkedList<WsByteBuffer>();
        int dataSize = inputBuffer.remaining();
        byte[] input = new byte[dataSize];
        inputBuffer.get(input, 0, dataSize);
        int inOffset = 0;

        if (this.resetNeededToProceed) {
            this.reset();
        }

        // see if we need to parse the header information still
        if (PARSE_STATE.DONE != this.state) {
            inOffset = parseHeader(input);
            if (inOffset >= input.length) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "decompress, ran out of data while parsing gzip header");
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "decompress");
                }
                return list;
            }
        }
        if (!this.inflater.finished()) {
            this.inflater.setInput(input, inOffset, dataSize - inOffset);
        }
        long initialBytesRead = this.inflater.getBytesRead();
        int outOffset = 0;

        // keep decompressing until we've used up the entire input buffer or reached
        // the end of the compressed stream
        int len = -1;
        while (inOffset < input.length && !this.inflater.finished() && 0 != len) {
            try {
                len = this.inflater.inflate(this.buf, outOffset, this.buf.length - outOffset);
            } catch (DataFormatException dfe) {
                // no FFDC required, clean up any buffers we've allocated so far
                release(list);
                throw dfe;
            }
            long bytesRead = this.inflater.getBytesRead();
            inOffset += (bytesRead - initialBytesRead);
            initialBytesRead = bytesRead;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Decompressed amount=" + len + " inOffset=" + inOffset + " read=" + this.inflater.getBytesRead() + " written=" + this.inflater.getBytesWritten()
                             + " finished=" + this.inflater.finished());
            }
            outOffset += len;
            if (outOffset >= this.buf.length) {
                WsByteBuffer buffer = HttpDispatcher.getBufferManager().allocate(this.buf.length);
                buffer.put(this.buf, 0, this.buf.length);
                this.checksum.update(this.buf, 0, this.buf.length);
                buffer.flip();
                list.add(buffer);
                outOffset = 0;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Storing decompressed buffer; " + buffer);
                }
            }
        }
        // save off any extra output that is ready
        if (0 < outOffset) {
            WsByteBuffer buffer = HttpDispatcher.getBufferManager().allocate(outOffset);
            buffer.put(this.buf, 0, outOffset);
            this.checksum.update(this.buf, 0, outOffset);
            buffer.flip();
            list.add(buffer);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Stored final decompressed buffer; " + buffer);
            }
        }
        // check whether we reached the end on this pass
        if (this.inflater.finished()) {
            inOffset = parseTrailer(input, inOffset, list);
        }
        if (inOffset < dataSize) {
            // did not use the entire buffer
            inputBuffer.position(inputBuffer.position() - (dataSize - inOffset));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "input buffer has unused data; " + inputBuffer);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "decompress", list.size());
        }
        return list;
    }

    /*
     * @see com.ibm.wsspi.http.channel.compression.DecompressionHandler#close()
     */
    public void close() {
        this.inflater.end();
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#isFinished()
     */
    public boolean isFinished() {
        return this.inflater.finished();
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#getBytesRead()
     */
    public long getBytesRead() {
        return this.countRead + this.inflater.getBytesRead();
    }

    /*
     * @see
     * com.ibm.wsspi.http.channel.compression.DecompressionHandler#getBytesWritten
     * ()
     */
    public long getBytesWritten() {
        return this.countWritten + this.inflater.getBytesWritten();
    }

}
