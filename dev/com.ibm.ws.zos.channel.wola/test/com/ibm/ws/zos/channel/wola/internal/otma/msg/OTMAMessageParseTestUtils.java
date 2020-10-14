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

import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 *
 */
public class OTMAMessageParseTestUtils {

    private static final byte alphabet[] = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
    OTMATestMessageGenerator tmg;

    OTMAMessageParseTestUtils() {
        tmg = new OTMATestMessageGenerator();
    }

    public void validateRequestTest(int[] headerValues, int[] messageLengths, boolean llzz, boolean reqMsg) throws Exception {

        int llzzSize = 4;
        int totalMsgLength = 0;

        tmg.generateMessage(llzz, reqMsg, messageLengths, headerValues);
        OTMAMessageParser otmaParser = new OTMAMessageParser();
        otmaParser.parseOTMARequestMessage(tmg.getRequestMessage(), llzz);
        int[] headers = otmaParser.getRequestHeaderSegments();
        assertTrue("Header int array size is invalid expected " + (headerValues.length + 1) + " received " + headers.length, headerValues.length + 1 == headers.length);
        for (int i = 0; i <= headerValues.length; i++) {
            if (i == 0)
                assertTrue("Count does not match size expected, received" + headers[i] + " expected " + headerValues[i], headers[i] == headerValues.length);
            else
                assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + (headerValues[i - 1] - llzzSize),
                           headers[i] == (headerValues[i - 1] - llzzSize));
        }
        byte[] message = otmaParser.getRequestMessageData();
        for (int i = 0; i < messageLengths.length; i++) {
            totalMsgLength = messageLengths[i] + totalMsgLength;
        }
        String assertFailMsg = "Message data length is not equal expected " + totalMsgLength + " received  " + message.length;
        assertTrue(assertFailMsg, message.length == totalMsgLength);

    }

    public void validateResponseTest(int[] headerValues, int[] messageLengths, boolean llzz, boolean reqMsg) throws Exception {

        int testLength = 0;
        int currMsgSeg = 0;
        int expectedHeader = 4;
        int llzzSize = 0;
        if (llzz) {
            llzzSize = 4;
        } else {
            llzzSize = 6;
        }
        tmg.generateMessage(llzz, reqMsg, messageLengths, headerValues);
        OTMAMessageParser otmaParser = new OTMAMessageParser();
        otmaParser.parseOTMAResponseMessage(tmg.getResponseSegments(), tmg.getResponseMessageData(), llzz);

        byte[] parsedResponse = otmaParser.getResponseMessage();
        ByteBuffer testResp = ByteBuffer.wrap(parsedResponse);
        while (testResp.hasRemaining()) {
            if (llzz) {
                // Do the following to prevent -32768 as the
                // length if you just did a straight getShort
                byte[] tempLen = new byte[] { 0x00, 0x00, 0x00, 0x00 };
                testResp.get(tempLen, 2, 2);
                ByteBuffer tempBuf = ByteBuffer.wrap(tempLen);
                testLength = tempBuf.getInt();
            } else {
                testLength = testResp.getInt();
            }
            assertTrue("Message Length for message segment " + currMsgSeg + " not valid. Expected " + (headerValues[currMsgSeg] + expectedHeader) + " actual " + testLength,
                       testLength == headerValues[currMsgSeg]
                                     + expectedHeader);
            int zzValue = testResp.getShort();
            assertTrue("ZZ value not valid Expected 0 actual " + zzValue, zzValue == 0);
            byte[] tmp = new byte[testLength - expectedHeader];
            testResp.get(tmp, 0, testLength - expectedHeader);
            byte[] compareArray = getCompareArray(headerValues[currMsgSeg]);
            assertTrue("response data does not match expected ", Arrays.equals(tmp, compareArray));
            currMsgSeg++;
        }

    }

    public byte[] getCompareArray(int arraySize) {
        byte[] compare = new byte[arraySize];
        int loops = arraySize / 26;
        int leftover = arraySize % 26;
        for (int i = 0; i < loops; i++) {
            System.arraycopy(alphabet, 0, compare, i * 26, 26);
        }
        if (leftover > 0)
            System.arraycopy(alphabet, 0, compare, loops * 26, leftover);
        return compare;
    }
}
