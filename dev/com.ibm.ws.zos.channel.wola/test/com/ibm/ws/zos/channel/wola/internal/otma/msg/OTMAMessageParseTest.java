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

import org.junit.Test;

/**
 *
 */
public class OTMAMessageParseTest {

    private static OTMATestMessageGenerator tmg = new OTMATestMessageGenerator();
    private static boolean llzz;
    private static boolean reqMsg;
    private static final byte alphabet[] = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    @Test
    public void testValidSingleSegReq() {

        String failureMsg = null;
        boolean testSuccess = true;
        int[] headerLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLValues[0] = 30;
        messageLengths[0] = 26;
        llzz = true;
        reqMsg = true;
        try {
            validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValidSingleSegResp() {

        boolean testSuccess = true;
        int[] headerLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLValues[0] = 26;
        messageLengths[0] = 26;
        llzz = true;
        reqMsg = false;
        try {
            validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            testSuccess = false;
            assertTrue("Error testing Valid single Segment response Error: " + e.toString(), testSuccess);
        }
    }

    @Test
    public void testValidSingleSegReqLLLL() {
        boolean testSuccess = true;
        String failMsg = null;
        int[] headerLLLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLLLValues[0] = alphabet.length + 4;
        messageLengths[0] = alphabet.length;
        llzz = false;
        reqMsg = true;
        try {
            validateRequestTest(headerLLLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failMsg, testSuccess);
    }

    @Test
    public void testValidSingleSegRespLLLL() {

        boolean testSuccess = true;
        int[] headerLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLValues[0] = alphabet.length;
        messageLengths[0] = alphabet.length;
        llzz = false;
        reqMsg = false;
        try {
            validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            testSuccess = false;
            assertTrue("Error testing Valid single Segment response Error: " + e.toString(), testSuccess);
        }
    }

    @Test
    public void testValidSingleSegReqMax() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLValues[0] = 32768;
        messageLengths[0] = 32764;
        llzz = true;
        reqMsg = true;
        try {
            validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidSingleSegReqMaxLLLL() {
        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLLLValues[0] = 32766; // actual header length - 2
        messageLengths[0] = 32762; // max seg len (32768) - header length (6)
        llzz = false;
        reqMsg = true;
        try {
            validateRequestTest(headerLLLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValidSingleSegRespMax() {

        boolean testSuccess = true;
        int[] headerLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLValues[0] = 32764;
        messageLengths[0] = 32764;
        llzz = true;
        reqMsg = false;
        try {
            validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);

        } catch (Exception e) {
            testSuccess = false;
            assertTrue("Error testing Valid single Segment response MAX Error: " + e.toString(), testSuccess);
        }
    }

    @Test
    public void testValidSingleSegRespMaxLLLL() {

        boolean testSuccess = true;
        int[] headerLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLValues[0] = 32762;
        messageLengths[0] = 32762;
        llzz = false;
        reqMsg = false;
        try {
            validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);

        } catch (Exception e) {
            testSuccess = false;
            assertTrue("Error testing Valid single Segment response MAX Error: " + e.toString(), testSuccess);
        }
    }

    @Test
    public void testValidSingleSegReqMin() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLValues[0] = 12;
        messageLengths[0] = 8;
        llzz = true;
        reqMsg = true;
        try {
            validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidSingleSegReqMinLLLL() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLLLValues[0] = 12; // header length (6) - 2 (for LLZZ Weirdness)
        messageLengths[0] = 8;
        llzz = false;
        reqMsg = true;
        try {
            validateRequestTest(headerLLLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValidSingleSegRespMin() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLValues[0] = 8;
        messageLengths[0] = 8;
        llzz = true;
        reqMsg = false;
        try {
            validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);

        } catch (Exception e) {
            testSuccess = false;
            failureMsg = e.toString();
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValidSingleSegRespMinLLLL() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[1];
        int[] messageLengths = new int[1];
        headerLLValues[0] = 8;
        messageLengths[0] = 8;
        llzz = false;
        reqMsg = false;
        try {
            validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);

        } catch (Exception e) {
            testSuccess = false;
            failureMsg = e.toString();
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidSSReqHeaderGTBuffer() {
        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[1];
        int[] messageLengths = new int[1];
        headerValues[0] = 60;
        messageLengths[0] = 20;
        llzz = true;
        reqMsg = true;
        try {
            tmg.generateMessage(llzz, reqMsg, messageLengths, headerValues);
            OTMAMessageParser otmaParser = new OTMAMessageParser();
            otmaParser.parseOTMARequestMessage(tmg.getRequestMessage(), llzz);
        } catch (OTMAMessageParseException mpe) {
            String tmp = mpe.toString();
            int found = tmp.indexOf("CWWKB0507E");
            if (found >= 0)
                testSuccess = true;
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidSSReqLLLHeaderGTBuffer() {
        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[1];
        int[] messageLengths = new int[1];
        headerValues[0] = 60;
        messageLengths[0] = 20;
        llzz = false;
        reqMsg = true;
        try {
            tmg.generateMessage(llzz, reqMsg, messageLengths, headerValues);
            OTMAMessageParser otmaParser = new OTMAMessageParser();
            otmaParser.parseOTMARequestMessage(tmg.getRequestMessage(), llzz);
        } catch (OTMAMessageParseException mpe) {
            String tmp = mpe.toString();
            int found = tmp.indexOf("CWWKB0507E");
            if (found >= 0)
                testSuccess = true;
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidSSRespHeaderGTBuffer() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[1];
        int[] messageLengths = new int[1];
        headerValues[0] = 60;
        messageLengths[0] = 20;
        llzz = true;
        reqMsg = false;
        try {
            tmg.generateMessage(llzz, reqMsg, messageLengths, headerValues);
            OTMAMessageParser otmaParser = new OTMAMessageParser();
            otmaParser.parseOTMAResponseMessage(tmg.getResponseSegments(), tmg.getResponseMessageData(), llzz);
        } catch (OTMAMessageParseException mpe) {
            String tmp = mpe.toString();
            int found = tmp.indexOf("CWWKB0508E");
            if (found >= 0)
                testSuccess = true;
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidSSLLLLRespHeaderGTBuffer() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[1];
        int[] messageLengths = new int[1];
        headerValues[0] = 60;
        messageLengths[0] = 20;
        llzz = false;
        reqMsg = false;
        try {
            tmg.generateMessage(llzz, reqMsg, messageLengths, headerValues);
            OTMAMessageParser otmaParser = new OTMAMessageParser();
            otmaParser.parseOTMAResponseMessage(tmg.getResponseSegments(), tmg.getResponseMessageData(), llzz);
        } catch (OTMAMessageParseException mpe) {
            String tmp = mpe.toString();
            int found = tmp.indexOf("CWWKB0508E");
            if (found >= 0)
                testSuccess = true;
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidSSReqHeaderLTMin() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[1];
        int[] messageLengths = new int[1];
        headerValues[0] = 4;
        messageLengths[0] = 4;
        llzz = true;
        reqMsg = true;
        try {
            tmg.generateMessage(llzz, reqMsg, messageLengths, headerValues);
            OTMAMessageParser otmaParser = new OTMAMessageParser();
            otmaParser.parseOTMARequestMessage(tmg.getRequestMessage(), llzz);
        } catch (OTMAMessageParseException mpe) {
            String tmp = mpe.toString();
            int found = tmp.indexOf("CWWKB0506E");
            if (found >= 0)
                testSuccess = true;
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidSSLLLLReqHeaderLTMin() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[1];
        int[] messageLengths = new int[1];
        headerValues[0] = 4;
        messageLengths[0] = 4;
        llzz = false;
        reqMsg = true;
        try {
            tmg.generateMessage(llzz, reqMsg, messageLengths, headerValues);
            OTMAMessageParser otmaParser = new OTMAMessageParser();
            otmaParser.parseOTMARequestMessage(tmg.getRequestMessage(), llzz);
        } catch (OTMAMessageParseException mpe) {
            String tmp = mpe.toString();
            int found = tmp.indexOf("CWWKB0506E");
            if (found >= 0)
                testSuccess = true;
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidSSReqHeaderGTMax() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[1];
        int[] messageLengths = new int[1];
        headerValues[0] = 32770;
        messageLengths[0] = 32770;
        llzz = true;
        reqMsg = true;
        try {
            tmg.generateMessage(llzz, reqMsg, messageLengths, headerValues);
            OTMAMessageParser otmaParser = new OTMAMessageParser();
            otmaParser.parseOTMARequestMessage(tmg.getRequestMessage(), llzz);
        } catch (OTMAMessageParseException mpe) {
            String tmp = mpe.toString();
            int found = tmp.indexOf("CWWKB0506E");
            if (found >= 0)
                testSuccess = true;
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidSSLLLLReqHeaderGTMax() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[1];
        int[] messageLengths = new int[1];
        headerValues[0] = 32770;
        messageLengths[0] = 32770;
        llzz = false;
        reqMsg = true;
        try {
            tmg.generateMessage(llzz, reqMsg, messageLengths, headerValues);
            OTMAMessageParser otmaParser = new OTMAMessageParser();
            otmaParser.parseOTMARequestMessage(tmg.getRequestMessage(), llzz);
        } catch (OTMAMessageParseException mpe) {
            String tmp = mpe.toString();
            int found = tmp.indexOf("CWWKB0506E");
            if (found >= 0)
                testSuccess = true;
        } catch (Exception e) {
            failureMsg = e.toString();
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testOLABC806SSValid() throws Exception {
        int buffLen = 2 + // LL
                      2 + // ZZ
                      8 + // tranName
                      14; // Debug setting
        String msgData = "OLAB93  *NODEBUG4     ";

        ByteBuffer olabc806Buff = ByteBuffer.allocate(buffLen);
        olabc806Buff.putShort((short) buffLen);
        olabc806Buff.putShort((short) 0);
        olabc806Buff.put(msgData.getBytes());
        OTMAMessageParser otmaParser = new OTMAMessageParser();
        otmaParser.parseOTMARequestMessage(olabc806Buff.array(), true);
        int[] headers = otmaParser.getRequestHeaderSegments();
        assertTrue("Header int array size is invalid expected " + 2 + " received " + headers.length, 2 == headers.length);
        for (int i = 0; i < 2; i++) {
            if (i == 0)
                assertTrue("Count does not match size expected, received" + headers[i] + " expected " + 1, headers[i] == 1);
            else
                assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + (buffLen - 4),
                           headers[i] == buffLen - 4);
        }
        byte[] message = otmaParser.getRequestMessageData();
        int origLen = msgData.getBytes().length;
        String assertFailMsg = "Message data length is not equal expected " + origLen + " received  " + message.length;
        assertTrue(assertFailMsg, message.length == origLen);

    }

    private void validateRequestTest(int[] headerValues, int[] messageLengths, boolean llzz, boolean reqMsg) throws Exception {

        int llzzSize = 4;
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
        String assertFailMsg = "Message data length is not equal expected " + messageLengths[0] + " received  " + message.length;
        assertTrue(assertFailMsg, message.length == messageLengths[0]);

    }

    private void validateResponseTest(int[] headerValues, int[] messageLengths, boolean llzz, boolean reqMsg) throws Exception {

        int testLength = 0;
        int expectedHeader = 0;
        int llzzSize;
        if (llzz) {
            llzzSize = 4;
            expectedHeader = llzzSize;
        } else {
            llzzSize = 6;
            expectedHeader = llzzSize - 2;
        }
        tmg.generateMessage(llzz, reqMsg, messageLengths, headerValues);
        OTMAMessageParser otmaParser = new OTMAMessageParser();
        otmaParser.parseOTMAResponseMessage(tmg.getResponseSegments(), tmg.getResponseMessageData(), llzz);
        byte[] parsedResponse = otmaParser.getResponseMessage();
        ByteBuffer testResp = ByteBuffer.wrap(parsedResponse);
        if (llzz) {
            byte[] tempLen = new byte[] { 0x00, 0x00, 0x00, 0x00 };
            // Do the following to prevent -32768 as the
            // length if you just did a straight getShort
            System.arraycopy(parsedResponse, 0, tempLen, 2, 2);
            ByteBuffer tempBuf = ByteBuffer.wrap(tempLen);
            testLength = tempBuf.getInt();
            // advance the ByteBuffer
            testResp.getShort();

        } else {
            testLength = testResp.getInt();
        }
        assertTrue("Message Length not valid Expected " + (headerValues[0] + expectedHeader) + " actual " + testLength, testLength == headerValues[0] + expectedHeader);
        testLength = testResp.getShort();
        assertTrue("ZZ value not valid Expected 0 actual " + testLength, testLength == 0);
        assertTrue("Parsed response buffer size invalid ", (parsedResponse.length - llzzSize) == messageLengths[0]);
        byte[] tmp = new byte[parsedResponse.length - llzzSize];
        byte[] compareArray = getCompareArray(tmp.length);
        System.arraycopy(parsedResponse, llzzSize, tmp, 0, parsedResponse.length - llzzSize);
        assertTrue("response data does not match expected ", Arrays.equals(tmp, compareArray));

    }

    private byte[] getCompareArray(int arraySize) {
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
