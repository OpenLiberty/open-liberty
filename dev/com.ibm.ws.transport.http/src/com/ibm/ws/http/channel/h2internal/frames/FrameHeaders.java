/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal.frames;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.h2internal.Constants;
import com.ibm.ws.http.channel.h2internal.FrameReadProcessor;
import com.ibm.ws.http.channel.h2internal.FrameTypes;
import com.ibm.ws.http.channel.h2internal.H2ConnectionSettings;
import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.exceptions.FrameSizeException;
import com.ibm.ws.http.channel.h2internal.exceptions.Http2Exception;
import com.ibm.ws.http.channel.h2internal.exceptions.ProtocolException;
import com.ibm.ws.http.channel.h2internal.huffman.HuffmanEncoder;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

public class FrameHeaders extends Frame {

    /*
     * format of Headers frame
     *
     * length(24) XX XX XX - length of payload
     * type(8) 01 - HEADERS
     * flags(8) bits: 00x0 xx0x - END_STREAM (0x1), END_HEADERS (0x4), PADDED (0x8), PRIORITY (0x20)
     * R (1) 0
     * Stream (31) xx xx xx xx
     * Payload
     * Pad Length? (8)
     * Exclusive (1) X
     * Stream Dependency (31) xx xx xx xx
     * Weight? (8)
     * Header Block Fragment (*)
     * Padding (*)
     */

    private int paddingLength = 0;
    boolean exclusive = false;
    int streamDependency = 0;
    int weight = 0;

    // used in the write path
    public byte[] headerBlockFragment = null;

    // used for the read path: we only really care about the final string headers
    public StringBuilder headers;

    static public enum HeaderFieldType {
        INDEXED, // testcase complete
        LITERAL_INCREMENTAL_INDEXING, // testcase complete
        LITERAL_INCREMENTAL_INDEXING_NEW_NAME, // testcase complete
        LITERAL_WITHOUT_INDEXING_AND_INDEXED_NAME, // coded
        LITERAL_WITHOUT_INDEXING_AND_NEW_NAME, // testcase complete
        LITERAL_NEVER_INDEXED_INDEXED_NAME, // testcase complete
        LITERAL_NEVER_INDEXED_NEW_NAME // testcase complete
    }

    /**
     * Read frame constructor
     */
    public FrameHeaders(int streamId, int payloadLength, byte flags, boolean reserveBit, FrameDirection direction) {
        super(streamId, payloadLength, flags, reserveBit, direction);
        frameType = FrameTypes.HEADERS;
    }

    /**
     * Write frame constructor
     */
    public FrameHeaders(int streamId, byte[] headerBlockFragment, int streamDependency, int paddingLength, int weight,
                        boolean endStream, boolean endHeaders, boolean padded,
                        boolean priority, boolean exclusive, boolean reserveBit) {
        super(streamId, 0, (byte) 0x00, reserveBit, FrameDirection.WRITE);

        if (headerBlockFragment != null) {
            payloadLength += headerBlockFragment.length;
        }
        this.headerBlockFragment = headerBlockFragment;

        this.END_STREAM_FLAG = endStream;
        this.END_HEADERS_FLAG = endHeaders;
        if (padded) {
            this.PADDED_FLAG = padded;
            payloadLength += paddingLength + 1;
            this.paddingLength = paddingLength;
        }
        if (priority) {
            this.PRIORITY_FLAG = priority;
            payloadLength += 4 + 2;
        }
        this.exclusive = exclusive;
        this.streamDependency = streamDependency;
        this.weight = weight;

        frameType = FrameTypes.HEADERS;
        writeFrameLength += payloadLength;
        setInitialized();
    }

    public FrameHeaders(int streamId, byte[] headerBlockFragment, boolean endStream, boolean endHeaders) {
        super(streamId, 0, (byte) 0x00, false, FrameDirection.WRITE);

        if (headerBlockFragment != null) {
            payloadLength += headerBlockFragment.length;
        }
        this.headerBlockFragment = headerBlockFragment;

        this.END_STREAM_FLAG = endStream;
        this.END_HEADERS_FLAG = endHeaders;
        this.exclusive = false;
        this.weight = 0;
        frameType = FrameTypes.HEADERS;
        writeFrameLength += payloadLength;
        setInitialized();
    }

    @Override
    public void processPayload(FrameReadProcessor frp) throws FrameSizeException {
        // +---------------+
        // |Pad Length? (8)|
        // +-+-------------+-----------------------------------------------+
        // |E|                 Stream Dependency? (31)                     |
        // +-+-------------+-----------------------------------------------+
        // |  Weight? (8)  |
        // +-+-------------+-----------------------------------------------+
        // |                   Header Block Fragment (*)                 ...
        // +---------------------------------------------------------------+
        // |                           Padding (*)                       ...
        // +---------------------------------------------------------------+

        setFlags();
        int payloadIndex = 0; // iterator to keep track of current position

        if (PADDED_FLAG) {
            // The padded field is present; set the paddingLength to represent the actual size of the data we want
            paddingLength = frp.grabNextByte();
            payloadLength -= paddingLength;
            payloadIndex += 1; // Pad Length is one byte
        }

        if (PRIORITY_FLAG) {
            // get exclusivity flag, then stream priority
            byte firstPayloadByte = frp.grabNextByte();

            exclusive = utils.getReservedBit(firstPayloadByte);

            firstPayloadByte = (byte) (firstPayloadByte & Constants.MASK_7F);
            this.streamDependency = frp.grabNext24BitInt(firstPayloadByte);
            this.weight = frp.grabNextByte();
            payloadIndex += 5; // stream dependency + weight = 5 bytes
        }

        // subtract the current position from the total payload length
        int headerBlockLength = payloadLength - payloadIndex;

        this.headerBlockFragment = new byte[headerBlockLength];

        // reset the payloadIndex and copy over the header data
        for (payloadIndex = 0; payloadIndex < headerBlockLength; payloadIndex++)
            headerBlockFragment[payloadIndex] = frp.grabNextByte();

        // read any padding data (and ignore it)
        for (; payloadIndex < headerBlockLength + paddingLength; payloadIndex++)
            frp.grabNextByte();

        setInitialized();
    }

    @Override
    public WsByteBuffer buildFrameForWrite() {
        WsByteBuffer buffer = super.buildFrameForWrite();
        byte[] frame;
        if (buffer.hasArray()) {
            frame = buffer.array();
        } else {
            frame = super.createFrameArray();
        }

        // add the first 9 bytes of the array
        setFrameHeaders(frame, utils.FRAME_TYPE_HEADERS);

        // set up the frame payload
        int frameIndex = SIZE_FRAME_BEFORE_PAYLOAD;

        if (PADDED_FLAG) {
            utils.Move8BitstoByteArray(paddingLength, frame, frameIndex);
            frameIndex += 1;
        }

        if (PRIORITY_FLAG) {
            utils.Move31BitstoByteArray(streamDependency, frame, frameIndex);
            if (exclusive) {
                frame[frameIndex] = (byte) (frame[frameIndex] | 0x80);
            }
            frameIndex += 4;

            utils.Move8BitstoByteArray(weight, frame, frameIndex);
            frameIndex += 1;
        }

        for (int i = 0; i < headerBlockFragment.length; i++) {
            frame[frameIndex] = headerBlockFragment[i];
            frameIndex++;
        }

        for (int i = 0; i < paddingLength; i++) {
            frame[frameIndex] = 0x00;
            frameIndex++;
        }
        buffer.put(frame, 0, writeFrameLength);
        buffer.flip();
        return buffer;
    }

    public byte[] getHeaderBlockFragment() {
        return (initialized) ? headerBlockFragment : null;
    }

    public int getPaddingLength() {
        return this.paddingLength;
    }

    @Override
    public void validate(H2ConnectionSettings settings) throws Http2Exception {
        if (streamId == 0) {
            throw new ProtocolException("HEADERS frame streamID cannot be 0x0");
        } else if (this.getPayloadLength() <= 0) {
            throw new CompressionException("HEADERS frame must have a header block fragment");
        } else if (this.getPayloadLength() > settings.getMaxFrameSize()) {
            throw new FrameSizeException("HEADERS payload greater than allowed by the max frame size");
        } else if (this.paddingLength >= this.payloadLength) {
            throw new ProtocolException("HEADERS padding length must be less than the length of the payload");
        } else if (this.streamId == this.streamDependency) {
            ProtocolException pe = new ProtocolException("HEADERS frame stream cannot depend on itself");
            pe.setConnectionError(false);
            throw pe;
        } else if (this.paddingLength < 0) {
            throw new ProtocolException("HEADERS padding length is invalid");
        }
    }

    @Override
    protected void setFlags() {
        END_STREAM_FLAG = utils.getFlag(flags, 0);
        END_HEADERS_FLAG = utils.getFlag(flags, 2);
        PADDED_FLAG = utils.getFlag(flags, 3);
        PRIORITY_FLAG = utils.getFlag(flags, 5);
    }

    // Header formats:
    /*
     *
     * Indexed Header Field Representation
     * 0 1 2 3 4 5 6 7
     * +---+---+---+---+---+---+---+---+
     * | 1 | Index (7+) |
     * +---+---------------------------+
     *
     *
     * Literal Header Field with Incremental Indexing - Indexed Name
     * 0 1 2 3 4 5 6 7
     * +---+---+---+---+---+---+---+---+
     * | 0 | 1 | Index (6+) |
     * +---+---+-----------------------+
     * | H | Value Length (7+) |
     * +---+---------------------------+
     * | Value String (Length octets) |
     * +-------------------------------+
     * find the header with the Input Index, then add a new index with this header and value. so known header-index, but new header-index-value pair that can
     * be referenced later as INDEXED.
     *
     * Literal Header Field without Indexing -- Indexed Name
     * 0 1 2 3 4 5 6 7
     * +---+---+---+---+---+---+---+---+
     * | 0 | 0 | 0 | 0 | Index (4+) |
     * +---+---+-----------------------+
     * | H | Value Length (7+) |
     * +---+---------------------------+
     * | Value String (Length octets) |
     * +-------------------------------+
     *
     *
     * Literal Header Field Never Indexed - Indexed Name
     * 0 1 2 3 4 5 6 7
     * +---+---+---+---+---+---+---+---+
     * | 0 | 0 | 0 | 1 | Index (4+) |
     * +---+---+-----------------------+
     * | H | Value Length (7+) |
     * +---+---------------------------+
     * | Value String (Length octets) |
     * +-------------------------------+
     *
     *
     * Literal Header Field with Incremental Indexing -- New Name
     * 0 1 2 3 4 5 6 7
     * +---+---+---+---+---+---+---+---+
     * | 0 | 1 | 0 |
     * +---+---+-----------------------+
     * | H | Name Length (7+) |
     * +---+---------------------------+
     * | Name String (Length octets) |
     * +---+---------------------------+
     * | H | Value Length (7+) |
     * +---+---------------------------+
     * | Value String (Length octets) |
     * +-------------------------------+
     *
     *
     * Literal Header Field without Indexing -- New Name
     * 0 1 2 3 4 5 6 7
     * +---+---+---+---+---+---+---+---+
     * | 0 | 0 | 0 | 0 | 0 |
     * +---+---+-----------------------+
     * | H | Name Length (7+) |
     * +---+---------------------------+
     * | Name String (Length octets) |
     * +---+---------------------------+
     * | H | Value Length (7+) |
     * +---+---------------------------+
     * | Value String (Length octets) |
     * +-------------------------------+
     *
     * Literal Header Field Never Indexed -- New Name
     * 0 1 2 3 4 5 6 7
     * +---+---+---+---+---+---+---+---+
     * | 0 | 0 | 0 | 1 | 0 |
     * +---+---+-----------------------+
     * | H | Name Length (7+) |
     * +---+---------------------------+
     * | Name String (Length octets) |
     * +---+---------------------------+
     * | H | Value Length (7+) |
     * +---+---------------------------+
     * | Value String (Length octets) |
     * +-------------------------------+
     */

    public byte[] buildHeader(HeaderFieldType xIndexingType, String xHeaderName, String xHeaderValue, long xHeaderIndex) {

        byte headerStaticIndex = -1;
        byte[] retArray = null;
        LongEncoder enc = new LongEncoder();

        if (xHeaderName != null) {
            headerStaticIndex = utils.getIndexNumber(xHeaderName);
        }

        if (xIndexingType == HeaderFieldType.INDEXED) {
            if (headerStaticIndex != -1) {
                // one of the headers defined in the spec static table
                retArray = new byte[1];
                retArray[0] = (byte) (0x80 | headerStaticIndex);
            } else {
                // user passed in the headerIndex that we are to use
                retArray = enc.encode(xHeaderIndex, 7);
                retArray[0] = (byte) (0x80 | retArray[0]);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Indexed header built array: " + utils.printArray(retArray));
            }
            return retArray;
        }

        if ((xIndexingType == HeaderFieldType.LITERAL_INCREMENTAL_INDEXING)
            || (xIndexingType == HeaderFieldType.LITERAL_NEVER_INDEXED_INDEXED_NAME)
            || (xIndexingType == HeaderFieldType.LITERAL_WITHOUT_INDEXING_AND_INDEXED_NAME)) {

            byte[] idxArray = null;

            if (xHeaderName != null) {
                headerStaticIndex = utils.getIndexNumber(xHeaderName);
            }

            // user passed in the headerIndex that we are to use
            if (xIndexingType == HeaderFieldType.LITERAL_INCREMENTAL_INDEXING) {
                idxArray = enc.encode(headerStaticIndex, 6);
                // set first two bits to "01"
                idxArray[0] = (byte) (0x40 | idxArray[0]);
                idxArray[0] = (byte) (0x7F & idxArray[0]);
            } else if (xIndexingType == HeaderFieldType.LITERAL_NEVER_INDEXED_INDEXED_NAME) {
                idxArray = enc.encode(headerStaticIndex, 4);
                // set first four bits to "0001"
                idxArray[0] = (byte) (0x10 | idxArray[0]);
                idxArray[0] = (byte) (0x1F & idxArray[0]);
            } else if ((xIndexingType == HeaderFieldType.LITERAL_WITHOUT_INDEXING_AND_INDEXED_NAME)) {
                idxArray = enc.encode(headerStaticIndex, 4);
                // set first four bits to "0000"
                idxArray[0] = (byte) (0x0F & idxArray[0]);
            }

            byte[] ba = xHeaderValue.getBytes(); // may need to deal with string encoding later
            byte[] hArrayValue = HuffmanEncoder.convertAsciiToHuffman(ba);

            byte[] hArrayLength = enc.encode(hArrayValue.length, 7);
            hArrayLength[0] = (byte) (0x80 | hArrayLength[0]);

            int totalLength = idxArray.length + hArrayLength.length + hArrayValue.length;

            retArray = new byte[totalLength];

            System.arraycopy(idxArray, 0, retArray, 0, idxArray.length);
            System.arraycopy(hArrayLength, 0, retArray, idxArray.length, hArrayLength.length);
            System.arraycopy(hArrayValue, 0, retArray, idxArray.length + hArrayLength.length, hArrayValue.length);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Literal Header Field with Incremental Indexing built array: " + utils.printArray(retArray));
            }
            return retArray;

        }

        if ((xIndexingType == HeaderFieldType.LITERAL_WITHOUT_INDEXING_AND_NEW_NAME) ||
            (xIndexingType == HeaderFieldType.LITERAL_NEVER_INDEXED_NEW_NAME) ||
            (xIndexingType == HeaderFieldType.LITERAL_INCREMENTAL_INDEXING_NEW_NAME)) {

            byte[] ba = xHeaderName.getBytes(); // may need to deal with string encoding later
            byte[] hName = HuffmanEncoder.convertAsciiToHuffman(ba);
            byte[] hNameLength = enc.encode(hName.length, 7);
            hNameLength[0] = (byte) (0x80 | hNameLength[0]);

            ba = xHeaderValue.getBytes(); // may need to deal with string encoding later
            byte[] hValue = HuffmanEncoder.convertAsciiToHuffman(ba);
            byte[] hValueLength = enc.encode(hValue.length, 7);
            hValueLength[0] = (byte) (0x80 | hValueLength[0]);

            int totalLength = 1 + hNameLength.length + hName.length + hValueLength.length + hValue.length;

            retArray = new byte[totalLength];

            if (xIndexingType == HeaderFieldType.LITERAL_WITHOUT_INDEXING_AND_NEW_NAME) {
                retArray[0] = 0x00;
            } else if (xIndexingType == HeaderFieldType.LITERAL_NEVER_INDEXED_NEW_NAME) {
                retArray[0] = 0x10;
            } else if (xIndexingType == HeaderFieldType.LITERAL_INCREMENTAL_INDEXING_NEW_NAME) {
                retArray[0] = 0x40;
            }

            System.arraycopy(hNameLength, 0, retArray, 1, hNameLength.length);
            System.arraycopy(hName, 0, retArray, 1 + hNameLength.length, hName.length);
            System.arraycopy(hValueLength, 0, retArray, 1 + hNameLength.length + hName.length, hValueLength.length);
            System.arraycopy(hValue, 0, retArray, 1 + hNameLength.length + hName.length + hValueLength.length, hValue.length);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Literal Header Field without Indexing - new Name built array: "
                             + utils.printArray(retArray));
            }
            return retArray;
        }

        return null;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public int getStreamDependency() {
        return streamDependency;
    }

    public int getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        StringBuilder frameToString = new StringBuilder();

        frameToString.append(super.toString()).append("\n");

        frameToString.append("PaddingLength: " + this.getPaddingLength() + "\n");
        frameToString.append("isExclusive: " + this.isExclusive() + "\n");
        frameToString.append("StreamIdDependency: " + this.getStreamDependency() + "\n");
        frameToString.append("Weight: " + this.getWeight() + "\n");

        return frameToString.toString();
    }
}
