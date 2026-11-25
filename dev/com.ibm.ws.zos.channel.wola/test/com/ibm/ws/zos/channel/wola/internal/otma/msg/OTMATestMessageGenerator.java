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

/**
 * Class to generate test OTMA Request and response messages.
 */
public class OTMATestMessageGenerator {

    private static final byte alphabet[] = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    private byte[] responseMsg;
    private int[] responseSegments;
    private byte[] requestMsg;

    OTMATestMessageGenerator() {

    }

/*
 * gnerateMessage generates a give OTMA message type
 *
 * @param boolean llzz - OTMA Segment header type true = llzz false = llllzz
 *
 * @param boolean reqMsg - OTMA Message Type true = request message false = response message
 *
 * @param int[] messageLengths - contains the length of the data portion of the message.
 *
 * @param int[] headerLengths - contains the length value to be held by the message headers
 *
 * @throws Exception - if something goes wrong.
 */
    public void generateMessage(boolean llzz,
                                boolean reqMsg,
                                int[] messageLengths,
                                int[] headerLengths) throws Exception {
        if (reqMsg) {
            generateRequestMsg(llzz, messageLengths, headerLengths);
        } else {
            generateResponseMsg(llzz, messageLengths, headerLengths);
        }
    }

/*
 * generate a request message
 *
 * @param boolean llzz - OTMA Segment header type true = llzz false = llllzz
 *
 * @param int[] messageLengths - contains the length of the data portion of the message.
 *
 * @param int[] headerLengths - contains the length value to be held by the message headers
 *
 * @throws Exception if something goes wrong.
 */
    private void generateRequestMsg(boolean llzz, int[] msgLengths, int[] headerLengths) throws Exception {
        int reqMsgSize = 0;
        short zz = 0;
        for (int i = 0; i < msgLengths.length; i++) {
            reqMsgSize = msgLengths[i] + reqMsgSize;
        }
        if (llzz)
            reqMsgSize = reqMsgSize + (headerLengths.length) * 4;
        else
            reqMsgSize = reqMsgSize + (headerLengths.length) * 6;
        requestMsg = new byte[reqMsgSize];
        ByteBuffer formatted = ByteBuffer.wrap(requestMsg);
        for (int i = 0; i < headerLengths.length; i++) {
            if (llzz)
                formatted.putShort((short) headerLengths[i]);
            else
                formatted.putInt(headerLengths[i]);
            formatted.putShort(zz);
            if (msgLengths.length >= i) {
                formatted.put(getMsgBytes(msgLengths[i]));
            }
        }

    }

/*
 * generates a byte array of a given size using the alphabet array as a source
 *
 * @param int length of the byte array
 *
 * @throws Exception in case of IOException or ArrayIndexOutOfBounds exception
 */
    private byte[] getMsgBytes(int msgLength) throws Exception {

        byte[] tmp = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int numAlphaBets = 0;
        int remaining = 0;
        if (msgLength > 26) {
            numAlphaBets = msgLength / 26;
            remaining = msgLength % 26;

            for (int i = 0; i < numAlphaBets; i++) {
                bos.write(alphabet);
            }
            tmp = new byte[remaining];
            ByteBuffer.wrap(alphabet).get(tmp, 0, remaining);
            bos.write(tmp);
        } else {
            tmp = new byte[msgLength];
            ByteBuffer.wrap(alphabet).get(tmp, 0, msgLength);
            bos.write(tmp);
        }
        bos.flush();

        return bos.toByteArray();
    }

/*
 * Generate an OTMA Response Message
 *
 * @param boolean llzz - OTMA Segment header type true = llzz false = llll
 *
 * @param int[] messageLengths - contains the length of the data portion of the message.
 *
 * @param int[] headerLengths - contains the length value to be held by the message headers
 */
    private void generateResponseMsg(boolean llzz, int[] msgLengths, int[] headerLengths) throws Exception {

        responseSegments = new int[headerLengths.length + 1];
        responseSegments[0] = headerLengths.length; // Set count

        //Set the rest of the length arrays
        for (int i = 1; i <= headerLengths.length; i++) {
            responseSegments[i] = headerLengths[i - 1];
        }

        int msgSize = 0;
        for (int i = 0; i < msgLengths.length; i++) {
            msgSize = msgLengths[i] + msgSize;
        }

        responseMsg = new byte[msgSize];
        ByteBuffer msgBuffer = ByteBuffer.wrap(responseMsg);
        for (int i = 0; i < msgLengths.length; i++) {
            if (msgLengths[i] > 0)
                msgBuffer.put(getMsgBytes(msgLengths[i]));
        }

    }

    public int[] getResponseSegments() {
        return responseSegments;
    }

    public byte[] getResponseMessageData() {
        return responseMsg;
    }

    public byte[] getRequestMessage() {
        return requestMsg;
    }

}
