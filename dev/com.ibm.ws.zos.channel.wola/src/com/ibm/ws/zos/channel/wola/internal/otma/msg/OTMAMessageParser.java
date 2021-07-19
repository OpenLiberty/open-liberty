/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal.otma.msg;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Parses OTMA requests and OTMA responses
 *
 * OTMA Request message parsing. (request messages are sent to IMS over OTMA)
 * OTMA request message parsing will take a byte buffer containing an OTMA message and a boolean indicating the type of header the
 * message contains (LLZZ - true or LLLLZZ - false). The incoming byte array will contain message segment headers followed by the actual data
 * to be processed. The length indicated in the message header will be the length of the segment + the length of the segment
 * header. So in the case of an LLZZ format segment header the length in each segment header will be the length of the segment
 * plus 4 (the length of the header). In the case of an LLLLZZ header the additional length will be 6.
 * The parser will generate a int array and a byte array as output. The int array will contain
 * an entry with the length of the segment it represents. The first element in the int array will contain a count of the number
 * of segments to follow. As an example if the message contained 3 segments the int array would be 4 elements long. The first
 * element would contain the value 3 and each of the subsequent elements would contain the length of there respective segments.
 * The byte array returned will contain the original byte array with the headers removed.
 *
 * OTMA Response Message parsing (response messages are received from IMS over OTMA)
 * OTMA response message parsing will take an int array, a byte array and a boolean as arguments. It will return a byte array
 * containing a OTMA message with the headers added back into the message.
 * The int array will contain a count of segments in the message (1st element) and the length of each segment in the message.
 * The boolean will be used to indicate if the segment headers are of the format LLZZ(true) or LLLLZZ(false).
 * Note on LLLLZZ message headers. The length indicated in the LLLL field will be the message data length + the header length - 2
 * for a LLZZ message header the LL field will contain a length of the message data along with the length of the header.
 *
 * How to use this class.
 * 1) Call the constructor
 * 2) call the parseOTMAReqestMessage or parseOTMAResponseMmssage methods depending on the type of message
 * you want to process
 * 3) to get parsed response message data call getResponseMessage
 * or
 * 3a) to get the parsed Request message call getRequestHeaderSegments and getRequestMessageData
 *
 * The hasResponseMsg and hasRequestMsg methods will return true if a request or response messaage has been successfully
 * been parsed by this instance of the object
 *
 */
public class OTMAMessageParser {

    private static final int MAX_SEG_SIZE = 32768;
    private static final int LLZZSize = 4;
    private static final int LLLLZZSize = 6;
    private int segmentHdrSize;
    private ByteArrayOutputStream requestMsg;
    private ByteArrayOutputStream responseMsg;
    private ArrayList<Integer> requestMsgLengths = null;
    private final Object[] nofillins = null;
    private boolean hasResponseMsg = false;
    private boolean hasRequestMsg = false;

    private static final TraceComponent tc = Tr.register(OTMAMessageParser.class);

    public OTMAMessageParser() {
        segmentHdrSize = 0;
    }

/*
 * Parse response Message
 *
 * @param int[] otmaSegLengths - segment lengths (1st segment contains the number of segments all others contain segment lengths
 *
 * @oaram byte[] messageData - byte array containing the Message data
 *
 * @param boolean llzz - boolean indicating the message header type true = llzz false = llllzz
 *
 * @throws OTMAMessageParseException - any error encountered.
 */
    public void parseOTMAResponseMessage(int[] otmaSegLengths, byte[] otmaMessageData, boolean llzz) throws OTMAMessageParseException {

        hasResponseMsg = false;
        int msgSize = 0;

        //Calc Message size. add each segment length
        //skip otmaSegments[0] it has the count in it.
        for (int i = 1; i < otmaSegLengths[0]; i++) {
            msgSize = otmaSegLengths[i] + msgSize;
        }

        segmentHdrSize = 0;
        if (llzz)
            segmentHdrSize = LLZZSize;
        else
            segmentHdrSize = LLLLZZSize;

        /*
         * if (otmaSegLengths[0] != otmaSegLengths.length - 1) {
         * Object[] fillins = new Object[2];
         * fillins[0] = Integer.toString(otmaSegLengths[0]);
         * fillins[1] = Integer.toString(otmaSegLengths.length);
         * TraceNLS nls = TraceNLS.getTraceNLS(this.getClass(), "com.ibm.ws.zos.channel.wola.internal.resources.ZWOLAChannelMessages");
         * throw new OTMAMessageParseException(nls.getFormattedMessage("OTMA_RESP_COUNT_INVALID",
         * fillins,
         * "CWWKB0509E: OTMA response message count that is specified " + fillins[0]
         * + " does not match the actual number of segments " + fillins[1]));
         * }
         */
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "OTMAMessageParser.parseOTMAResponseMessage: Header count =" + otmaSegLengths[0] + " Header Array size = " + otmaSegLengths.length);
        // increment for the headers needed.
        msgSize = msgSize + otmaSegLengths[0] * segmentHdrSize;
        responseMsg = new ByteArrayOutputStream(msgSize);
        int bufferOffset = 0;
        for (int i = 1; i <= otmaSegLengths[0]; i++) {
            if ((bufferOffset < otmaMessageData.length) ||
                (otmaSegLengths[i] == 0)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Creating Response Message segment");
                parseResponseMessageSegment(otmaSegLengths[i], otmaMessageData, bufferOffset, llzz, i - 1);
                bufferOffset = bufferOffset + otmaSegLengths[i];
            } else {
                Object[] fillins = new Object[1];
                fillins[0] = Integer.valueOf(i - 1);
                TraceNLS nls = TraceNLS.getTraceNLS(this.getClass(), "com.ibm.ws.zos.channel.wola.internal.resources.ZWOLAChannelMessages");
                throw new OTMAMessageParseException(nls.getFormattedMessage("OTMA_RESP_MSG_PARSE_ERR",
                                                                            fillins,
                                                                            "CWWKB0504E: OTMA response message segment Length specified for segment" + (String) fillins[0]
                                                                                     + " does not match segment actual size."));
            }
        }

        hasResponseMsg = true;

    }

    /*
     * parseResponseMessageSegment - parses a message segment from IMS into a message that can be passed to the calling program.
     *
     * @param int message length
     *
     * @param byte[] message - byte array containing the message data
     *
     * @param int offset - offset into the message byte array indicating where to start processing
     *
     * @throws OTMAMessageParseException
     */
    private void parseResponseMessageSegment(int msgLength, byte[] msgData, int offset, boolean llzz, int segIndex) throws OTMAMessageParseException {

        ByteBuffer formattedMsg = null;
        byte[] messageData = null;
        int segHeaderLen = 0;
        int segHeaderLenValue = 0;
        short zz = 0;
        // Create new byte buffer
        if (llzz) {
            segHeaderLen = LLZZSize;
            segHeaderLenValue = LLZZSize;
            messageData = new byte[msgLength + segHeaderLen];
            formattedMsg = ByteBuffer.wrap(messageData);
            formattedMsg.putShort((short) (msgLength + segHeaderLenValue));
        } else {
            // LLLLZZ need to account for the extra 2 bytes
            // in the buffer even if they aren't counted
            // in the value of the message length
            segHeaderLen = LLLLZZSize;
            segHeaderLenValue = LLZZSize;
            messageData = new byte[msgLength + segHeaderLen];
            formattedMsg = ByteBuffer.wrap(messageData);
            formattedMsg.putInt(msgLength + segHeaderLenValue);
        }
        // put ZZ in buffer
        formattedMsg.putShort(zz);
        // Put Data in buffer
        if (msgLength <= msgData.length - offset) {
            if (msgLength > 0) // Assume we can have a message with header and no data.
                formattedMsg.put(msgData, offset, msgLength);
            try {
                responseMsg.write(messageData);
            } catch (Exception e) {
                throw new OTMAMessageParseException(e);
            }
        } else {
            TraceNLS nls = TraceNLS.getTraceNLS(this.getClass(), "com.ibm.ws.zos.channel.wola.internal.resources.ZWOLAChannelMessages");
            Object[] nlsParm = new Object[] { Integer.toString(msgLength), Integer.toString(segIndex) };
            throw new OTMAMessageParseException(nls.getFormattedMessage("OTMA_RESP_MSG_SEG_SMALL",
                                                                        nlsParm,
                                                                        "CWWKB0508E: OTMA response header Length specified " + msgLength
                                                                                 + " exceeds input buffer size for message segment " + segIndex));
        }

    }

    public byte[] getResponseMessage() throws OTMAMessageParseException {

        byte[] returnMsg;
        try {
            responseMsg.flush();
            returnMsg = responseMsg.toByteArray();
        } catch (Exception e) {
            throw new OTMAMessageParseException(e);
        }
        return returnMsg;
    }

    /*
     * Parse Request Message
     *
     * @oaram byte[] messageData - byte array containing the entire OTMA message
     *
     * @param boolean llzz - boolean indicating the message header type true = llzz false = llllzz
     *
     * @returns OTMARequestMessage = encapsulates a parsed OTMA resquest message
     *
     * @throws OTMAMessageParseException - any error encountered.
     */
    public void parseOTMARequestMessage(byte[] otmaMessage, boolean llzz) throws OTMAMessageParseException {

        hasRequestMsg = false;
        requestMsgLengths = new ArrayList<Integer>();
        try {
            requestMsg = new ByteArrayOutputStream(otmaMessage.length);
        } catch (Exception e) {
            throw new OTMAMessageParseException(e);

        }
        segmentHdrSize = 0;
        if (llzz)
            segmentHdrSize = LLZZSize;
        else
            segmentHdrSize = LLLLZZSize;

        int bytesToProcess = otmaMessage.length;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "OTMAMessageParser.parseOTMARequestMessage: message length =" + otmaMessage.length);
        int segmentCount = 0;
        while (bytesToProcess > 0) {
            int segSize = getNextRequestSegment(otmaMessage, bytesToProcess, segmentCount);
            int decrementBy = 0;
            decrementBy = segSize + segmentHdrSize;
            bytesToProcess = bytesToProcess - decrementBy;
            segmentCount++;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "OTMAMessageParser.parseOTMARequestMessage: bytes left to process = " + bytesToProcess);
        }
        hasRequestMsg = true;
    }

/*
 * gentNextRequestMessageSegment returns the next message segment from the input byte buffer
 *
 * @param byte[] otmaMessage - byte array containing the OTMA request message.
 *
 * @param int bytesLeft - indicates the number of byte left in the buffer to process
 *
 * @return OTMARequestMessageSegment - Encapsulates a segment of an otmaRequest message
 */
    private int getNextRequestSegment(byte[] otmaMessage, int bytesLeft, int segment) throws OTMAMessageParseException {

        // OTMARequestMessageSegment msgSeg = null;
        int returnLength = 0;
        // Get the header for this segment based on header type.
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "OTMAMessageParser.parseOTMARequestMessage: Byte array Header Starting point = " + (otmaMessage.length - bytesLeft));

        boolean firstSegment = false;

        int minSegLength = 0;
        if (otmaMessage.length == bytesLeft)
            firstSegment = true;

        int segmentLength = getSegmentLength(otmaMessage, bytesLeft);
        if (firstSegment)
            /* If first segment header + TRAN Name = 12 */
            minSegLength = 12;
        else
            /* segment needs a header */
            minSegLength = 4;
        if (segmentLength < minSegLength || segmentLength > MAX_SEG_SIZE) {
            Object[] messageFillins = new Object[] { Integer.toString(segmentLength), Integer.toString(segment) };
            TraceNLS nls = TraceNLS.getTraceNLS(this.getClass(), "com.ibm.ws.zos.channel.wola.internal.resources.ZWOLAChannelMessages");
            throw new OTMAMessageParseException(nls.getFormattedMessage("OTMA_REQ_HEADER_INVALID",
                                                                        messageFillins,
                                                                        "CWWKB0506E: OTMA request segment length " + (String) messageFillins[0] + " for segment number "
                                                                                        + (String) messageFillins[1] + "is invalid."));

        } else if (segmentLength > bytesLeft) {
            Object[] messageFillins = new Object[] { Integer.toString(segmentLength), Integer.toString(bytesLeft), Integer.toString(segment) };
            TraceNLS nls = TraceNLS.getTraceNLS(this.getClass(), "com.ibm.ws.zos.channel.wola.internal.resources.ZWOLAChannelMessages");
            throw new OTMAMessageParseException(nls.getFormattedMessage("OTMA_REQ_HEADER_SHORT",
                                                                        messageFillins,
                                                                        "CWWKB0507E: OTMA request segment Header length of " + (String) messageFillins[0] +
                                                                                        "exceeds remaining buffer length of " + (String) messageFillins[1] + " for segment "
                                                                                        + (String) messageFillins[2]));
        } else {
            byte[] newMsgSeg = null;
            newMsgSeg = new byte[segmentLength - LLZZSize];
            ByteBuffer newSegBuffer = ByteBuffer.wrap(newMsgSeg);
            newSegBuffer.put(otmaMessage, otmaMessage.length - bytesLeft + segmentHdrSize, segmentLength - LLZZSize);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "OTMAMessageParser.parseOTMARequestMessage: New Segment Size = " + newMsgSeg.length);
                Tr.debug(tc, "OTMAMessageParser.parseOTMARequestMessage: otmamessage offset = " + (otmaMessage.length - bytesLeft) + segmentHdrSize);
                Tr.debug(tc, "OTMAMessageParser.parseOTMARequestMessage: Segment length = " + segmentLength);
            }
            try {
                requestMsg.write(newMsgSeg);
            } catch (Exception e) {
                throw new OTMAMessageParseException(e);
            }
            returnLength = segmentLength - LLZZSize;
            requestMsgLengths.add(Integer.valueOf(returnLength));
        }
        return returnLength;
    }

    /*
     * getSegmentLength - will get the segment length from the OTMA message buffer. This assumes that the
     * byte array length - bytesLeft will point to the begining of a Header.
     *
     * @param byte[] otmaMessage - input byte array containing the OTMA message
     *
     * @param int bytesLeft - the number of bytes left in the input array to be processed
     *
     * @return int length of the segment.
     */
    private int getSegmentLength(byte[] otmaMessage, int bytesLeft) {
        //Use this to get header length otherwise max size of 32768 will be negative when using ByteBuffer.getShort
        int length = 0;
        byte[] expandedHeader = new byte[] { 0x00, 0x00, 0x00, 0x00 };
        if (segmentHdrSize == LLZZSize) {
            System.arraycopy(otmaMessage, otmaMessage.length - bytesLeft, expandedHeader, 2, 2);
        } else {
            System.arraycopy(otmaMessage, otmaMessage.length - bytesLeft, expandedHeader, 0, 4);
        }

        ByteBuffer buffHeader = ByteBuffer.wrap(expandedHeader);
        length = buffHeader.getInt();
        return length;
    }

    public byte[] getRequestMessageData() throws OTMAMessageParseException {
        byte[] returnBytes = null;

        try {
            requestMsg.flush();
            returnBytes = requestMsg.toByteArray();
        } catch (Exception e) {
            throw new OTMAMessageParseException(e);
        }
        return returnBytes;
    }

    public int[] getRequestHeaderSegments() {
        int[] segmentLengths = new int[requestMsgLengths.size() + 1];
        segmentLengths[0] = requestMsgLengths.size(); // Set segment count
        for (int i = 0; i < requestMsgLengths.size(); i++) {
            segmentLengths[i + 1] = requestMsgLengths.get(i);
        }
        return segmentLengths;
    }

    public boolean hasResponseMsg() {
        return hasResponseMsg;
    }

    public boolean hasRequestMsg() {
        return hasRequestMsg;
    }
}
