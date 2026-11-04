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

import org.junit.Test;

/**
 *
 */
public class OTMAMultiSegmentParseTest {

    private static boolean llzz;
    private static boolean reqMsg;
    private static OTMAMessageParseTestUtils util = new OTMAMessageParseTestUtils();
    private static OTMATestMessageGenerator tmg = new OTMATestMessageGenerator();
    private static String olaParms = ("OLAB93  *NODEBUG4     *NODEBUG4     *NODEBUG20    *NODEBUG40    *NODEBUG80    *NODEBUG160   ");

    @Test
    public void testValidMultiSegReq() {

        String failureMsg = null;
        boolean testSuccess = true;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 30;
            messageLengths[i] = 26;
        }
        llzz = true;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValidMultiSegReqLLLL() {

        String failureMsg = null;
        boolean testSuccess = true;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 30;
            messageLengths[i] = 26;
        }
        llzz = false;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValidMultiSegReqDiffSizeSeg() {

        String failureMsg = null;
        boolean testSuccess = true;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 30 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        llzz = true;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValidMultiSegReqDiffSizeSegLLLL() {

        String failureMsg = null;
        boolean testSuccess = true;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 30 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        llzz = false;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValidMultiSegReqMax() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 32768;
            messageLengths[i] = 32764;
        }
        llzz = true;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegReqMaxLLLL() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 32766;
            messageLengths[i] = 32762;
        }
        llzz = false;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegReqMaxFirst() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];

        headerLLValues[0] = 32768;
        messageLengths[0] = 32764;

        for (int i = 1; i < 3; i++) {
            headerLLValues[i] = 30 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        llzz = true;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegReqMaxFirstLLLL() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        headerLLValues[0] = 32766;
        messageLengths[0] = 32762;
        for (int i = 1; i < 3; i++) {
            headerLLValues[i] = 30 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        llzz = false;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegReqMaxLast() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];

        for (int i = 0; i < 2; i++) {
            headerLLValues[i] = 30 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        headerLLValues[2] = 32768;
        messageLengths[2] = 32764;

        llzz = true;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegReqMaxLastLLLL() {
        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 2; i++) {
            headerLLValues[i] = 30 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        headerLLValues[2] = 32766;
        messageLengths[2] = 32762;
        llzz = false;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegReqMin() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        headerLLValues[0] = 12; //Min 1st Seg
        messageLengths[0] = 8;
        headerLLValues[1] = 4; // Min seg 1+
        messageLengths[1] = 0;
        headerLLValues[2] = 4;
        messageLengths[2] = 0;

        llzz = true;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegReqMinLLLL() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLLLValues = new int[3];
        int[] messageLengths = new int[3];
        headerLLLLValues[0] = 12; //Min 1st Seg
        messageLengths[0] = 8;
        headerLLLLValues[1] = 4; // Min seg 1+
        messageLengths[1] = 0;
        headerLLLLValues[2] = 4;
        messageLengths[2] = 0;
        llzz = false;
        reqMsg = true;
        try {
            util.validateRequestTest(headerLLLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValid_OLABC806_Msg() {
        //Request 3 Segments Valid LLZZ based on request message from HSCB800Bean.java in tWAS Ola Fat bucket
        //Message layout below:
        //1200OLAB93  1800*NODEBUG4     1800*NODEBUG4     1800*NODEBUG20
        boolean testSuccess = true;
        String failureMsg = null;
        String tran = "OLAB93  ";
        String seg1msg = "NODEBUG4     ";
        String seg2msg = "NODEBUG4     ";
        String seg3msg = "NODEBUG20    ";
        ByteBuffer msg = ByteBuffer.allocate(tran.length() + 4 +
                                             seg1msg.length() + 4 +
                                             seg2msg.length() + 4 +
                                             seg3msg.length() + 4);
        msg.putShort((short) (tran.length() + 4));
        msg.putShort((short) 0);
        msg.put(tran.getBytes());

        msg.putShort((short) (seg1msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg1msg.getBytes());

        msg.putShort((short) (seg2msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg2msg.getBytes());

        msg.putShort((short) (seg3msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg3msg.getBytes());

        byte[] msgByte = msg.array();

        try {
            OTMAMessageParser mp = new OTMAMessageParser();
            mp.parseOTMARequestMessage(msgByte, true);
            int[] headers = mp.getRequestHeaderSegments();
            assertTrue("Header int array size is invalid expected " + 5 + " received " + headers.length, 5 == headers.length);
            for (int i = 0; i < headers.length; i++) {
                if (i == 0)
                    assertTrue("Count does not match size expected, received" + headers[i] + " expected " + 4, headers[i] == 4);
                else if (i == 1) {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + tran.length(),
                               headers[i] == tran.length());
                } else {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + seg1msg.length(),
                               headers[i] == seg1msg.length());
                }
            }
            byte[] message = mp.getRequestMessageData();
            int expectedMsgLen = tran.length()
                                 + seg1msg.length()
                                 + seg2msg.length()
                                 + seg3msg.length();
            String assertFailMsg = "Message data length is not equal expected " + expectedMsgLen + " received  " + message.length;
            assertTrue(assertFailMsg, message.length == expectedMsgLen);

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValid_OLABC806_MsgLLLL() {
        //Request 3 Segments Valid LLLLZZ based on request message from HSCB800Bean.java in tWAS OLA Fat bucket
        //Message layout below:
        //001200OLAB93  001800*NODEBUG4     001800*NODEBUG4     001800*NODEBUG20
        boolean testSuccess = true;
        String failureMsg = null;
        String tran = "OLAB93  ";
        String seg1msg = "NODEBUG4     ";
        String seg2msg = "NODEBUG4     ";
        String seg3msg = "NODEBUG20    ";
        ByteBuffer msg = ByteBuffer.allocate(tran.length() + 6 +
                                             seg1msg.length() + 6 +
                                             seg2msg.length() + 6 +
                                             seg3msg.length() + 6);
        msg.putInt((tran.length() + 4));
        msg.putShort((short) 0);
        msg.put(tran.getBytes());

        msg.putInt((seg1msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg1msg.getBytes());

        msg.putInt((seg1msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg2msg.getBytes());

        msg.putInt((seg1msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg3msg.getBytes());

        byte[] msgByte = msg.array();

        try {
            OTMAMessageParser mp = new OTMAMessageParser();
            mp.parseOTMARequestMessage(msgByte, false);
            int[] headers = mp.getRequestHeaderSegments();
            assertTrue("Header int array size is invalid expected " + 5 + " received " + headers.length, 5 == headers.length);
            for (int i = 0; i < headers.length; i++) {
                if (i == 0)
                    assertTrue("Count does not match size expected, received" + headers[i] + " expected " + 4, headers[i] == 4);
                else if (i == 1) {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + tran.length(),
                               headers[i] == tran.length());
                } else {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + seg1msg.length(),
                               headers[i] == seg1msg.length());
                }
            }
            byte[] message = mp.getRequestMessageData();
            int expectedMsgLen = tran.length()
                                 + seg1msg.length()
                                 + seg2msg.length()
                                 + seg3msg.length();
            String assertFailMsg = "Message data length is not equal expected " + expectedMsgLen + " received  " + message.length;
            assertTrue(assertFailMsg, message.length == expectedMsgLen);

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValid_OLABC806_MsgSegLenNotEqual() {
        //Request 3 Segments Valid LLZZ based on request message from HSCB800Bean.java in tWAS Ola Fat bucket
        //Message layout below:
        //1200OLAB93  1800*NODEBUG4     1800*NODEBUG4     1800*NODEBUG20
        boolean testSuccess = true;
        String failureMsg = null;
        String tran = "OLAB93  ";
        String seg1msg = "NODEBUG4     *";
        String seg2msg = "NODEBUG4         *";
        String seg3msg = "NODEBUG20 *";
        ByteBuffer msg = ByteBuffer.allocate(tran.length() + 4 +
                                             seg1msg.length() + 4 +
                                             seg2msg.length() + 4 +
                                             seg3msg.length() + 4);
        msg.putShort((short) (tran.length() + 4));
        msg.putShort((short) 0);
        msg.put(tran.getBytes());

        msg.putShort((short) (seg1msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg1msg.getBytes());

        msg.putShort((short) (seg2msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg2msg.getBytes());

        msg.putShort((short) (seg3msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg3msg.getBytes());

        byte[] msgByte = msg.array();

        try {
            OTMAMessageParser mp = new OTMAMessageParser();
            mp.parseOTMARequestMessage(msgByte, true);
            int[] headers = mp.getRequestHeaderSegments();
            assertTrue("Header int array size is invalid expected " + 5 + " received " + headers.length, 5 == headers.length);
            for (int i = 0; i < headers.length; i++) {
                if (i == 0)
                    assertTrue("Count does not match size expected, received" + headers[i] + " expected " + 4, headers[i] == 4);
                else if (i == 1) {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + tran.length(),
                               headers[i] == tran.length());
                } else if (i == 2) {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + seg1msg.length(),
                               headers[i] == seg1msg.length());
                } else if (i == 3) {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + seg2msg.length(),
                               headers[i] == seg2msg.length());
                } else if (i == 4) {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + seg3msg.length(),
                               headers[i] == seg3msg.length());
                }
            }
            byte[] message = mp.getRequestMessageData();
            int expectedMsgLen = tran.length()
                                 + seg1msg.length()
                                 + seg2msg.length()
                                 + seg3msg.length();
            String assertFailMsg = "Message data length is not equal expected " + expectedMsgLen + " received  " + message.length;
            assertTrue(assertFailMsg, message.length == expectedMsgLen);

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValid_OLABC806_MsgSegLenNotEqualLLLL() {
        //Request 3 Segments Valid LLLLZZ based on request message from HSCB800Bean.java in tWAS OLA Fat bucket
        //Message layout below:
        //001200OLAB93  001800*NODEBUG4     001800*NODEBUG4     001800*NODEBUG20
        boolean testSuccess = true;
        String failureMsg = null;
        String tran = "OLAB93  ";
        String seg1msg = "NODEBUG4     *";
        String seg2msg = "NODEBUG4         *";
        String seg3msg = "NODEBUG20 *";
        ByteBuffer msg = ByteBuffer.allocate(tran.length() + 6 +
                                             seg1msg.length() + 6 +
                                             seg2msg.length() + 6 +
                                             seg3msg.length() + 6);
        msg.putInt((tran.length() + 4));
        msg.putShort((short) 0);
        msg.put(tran.getBytes());

        msg.putInt((seg1msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg1msg.getBytes());

        msg.putInt((seg2msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg2msg.getBytes());

        msg.putInt((seg3msg.length() + 4));
        msg.putShort((short) 0);
        msg.put(seg3msg.getBytes());

        byte[] msgByte = msg.array();

        try {
            OTMAMessageParser mp = new OTMAMessageParser();
            mp.parseOTMARequestMessage(msgByte, false);
            int[] headers = mp.getRequestHeaderSegments();
            assertTrue("Header int array size is invalid expected " + 5 + " received " + headers.length, 5 == headers.length);
            for (int i = 0; i < headers.length; i++) {
                if (i == 0)
                    assertTrue("Count does not match size expected, received" + headers[i] + " expected " + 4, headers[i] == 4);
                else if (i == 1) {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + tran.length(),
                               headers[i] == tran.length());
                } else if (i == 2) {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + seg1msg.length(),
                               headers[i] == seg1msg.length());
                } else if (i == 3) {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + seg2msg.length(),
                               headers[i] == seg2msg.length());
                } else if (i == 4) {
                    assertTrue("Header size does not match size expected, received " + headers[i] + " expected " + seg2msg.length(),
                               headers[i] == seg3msg.length());
                }
            }
            byte[] message = mp.getRequestMessageData();
            int expectedMsgLen = tran.length()
                                 + seg1msg.length()
                                 + seg2msg.length()
                                 + seg3msg.length();
            String assertFailMsg = "Message data length is not equal expected " + expectedMsgLen + " received  " + message.length;
            assertTrue(assertFailMsg, message.length == expectedMsgLen);

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

/*
 * Begin Invalid Request message tests
 */
    @Test
    public void testInValidMSReqHeaderGTBufferFirst() {
        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        headerValues[0] = 60;
        messageLengths[0] = 20;
        for (int i = 1; i < headerValues.length; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }
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
                found = tmp.indexOf("for segment 1");
            if (found >= 0)
                testSuccess = true;
            else
                failureMsg = "Either message incorrect or segment invalid. Exception = " + mpe.toString();

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSReqHeaderGTBufferLast() {
        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < headerValues.length - 1; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }
        headerValues[2] = 60;
        messageLengths[2] = 20;
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
                found = tmp.indexOf("for segment 2");
            if (found >= 0)
                testSuccess = true;
            else
                failureMsg = "Either message incorrect or segment invalid. Exception = " + mpe.toString();

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSReqHeaderGTBufferFirstLLLL() {
        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        headerValues[0] = 60;
        messageLengths[0] = 20;
        for (int i = 1; i < headerValues.length; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }
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
                found = tmp.indexOf("for segment number 1");
            if (found >= 0)
                testSuccess = true;
            else
                failureMsg = "Either message incorrect or segment invalid. Exception = " + mpe.toString();
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSReqHeaderGTBufferLastLLLL() {
        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < headerValues.length - 1; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }
        headerValues[2] = 60;
        messageLengths[2] = 20;
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
                found = tmp.indexOf("for segment 2");
            if (found >= 0)
                testSuccess = true;
            else
                failureMsg = "Either message incorrect or segment invalid. Exception = " + mpe.toString();
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSReqHeaderLTMinFirst() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        headerValues[0] = 8;
        messageLengths[0] = 4;
        for (int i = 1; i < 3; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }
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
            else
                failureMsg = "Error generated not expected. Received  " + mpe.toString() + " instead ";
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSLLLLReqHeaderLTMinFirst() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        headerValues[0] = 8;
        messageLengths[0] = 4;
        for (int i = 1; i < 3; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }

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
    public void testInValidMSReqHeaderLTMinLast() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 2; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }
        headerValues[0] = 1;
        messageLengths[0] = 1;

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
            else
                failureMsg = "Error generated not expected. Received  " + mpe.toString() + " instead ";
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSLLLLReqHeaderLTMinLast() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 2; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }
        headerValues[0] = 1;
        messageLengths[0] = 1;

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
    public void testInValidMSReqHeaderLTBufferFirst() {
        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        headerValues[0] = 14;
        messageLengths[0] = 20;
        for (int i = 1; i < headerValues.length; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }
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
                found = tmp.indexOf("for segment 1");
            if (found >= 0)
                testSuccess = true;
            else
                failureMsg = "Either message incorrect or segment invalid. Exception = " + mpe.toString();

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSReqHeaderLTBufferFirstLLLL() {
        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        headerValues[0] = 14;
        messageLengths[0] = 20;
        for (int i = 1; i < headerValues.length; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }
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
                found = tmp.indexOf("for segment number 1");
            if (found >= 0)
                testSuccess = true;
            else
                failureMsg = "Either message incorrect or segment invalid. Exception = " + mpe.toString();

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSReqHeaderLTBufferLast() {
        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < headerValues.length - 1; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }
        headerValues[2] = 10;
        messageLengths[2] = 20;
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
                found = tmp.indexOf("for segment 3");
            if (found >= 0)
                testSuccess = true;
            else
                failureMsg = "Either message incorrect or segment invalid. Exception = " + mpe.toString();

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSReqHeaderLTBufferLastLLLL() {
        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < headerValues.length - 1; i++) {
            headerValues[i] = 24;
            messageLengths[i] = 20;
        }
        headerValues[2] = 10;
        messageLengths[2] = 20;
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
                found = tmp.indexOf("for segment number 3"); //trying to parse segment 3 because of the length buffer mismatch
            if (found >= 0)
                testSuccess = true;
            else
                failureMsg = "Either message incorrect or segment invalid. Exception = " + mpe.toString();

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

/*
 * Begin Response message Testing
 */
    @Test
    public void testValidMultiSegResp() {

        boolean testSuccess = true;
        String errorMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 26;
            messageLengths[i] = 26;
        }
        llzz = true;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            testSuccess = false;
            errorMsg = "Error testing Valid Multi Segment response. Error: " + e.toString();
        }
        assertTrue(errorMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegRespLLLL() {

        boolean testSuccess = true;
        String errorMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 26;
            messageLengths[i] = 26;
        }
        llzz = false;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            testSuccess = false;
            errorMsg = "Error testing Valid Multi Segment response. Error: " + e.toString();
        }
        assertTrue(errorMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegRespDiffSize() {
        boolean testSuccess = true;
        String errorMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 26 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        llzz = true;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            testSuccess = false;
            errorMsg = "Error testing Valid Multi Segment response. Error: " + e.toString();
        }
        assertTrue(errorMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegRespDiffSizeLLLL() {

        boolean testSuccess = true;
        String errorMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 26 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        llzz = false;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            testSuccess = false;
            errorMsg = "Error testing Valid Multi Segment response. Error: " + e.toString();
        }
        assertTrue(errorMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegRespMax() {
        boolean testSuccess = true;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 32764;
            messageLengths[i] = 32764;
        }
        llzz = true;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);

        } catch (Exception e) {
            testSuccess = false;
            assertTrue("Error testing Valid single Segment response MAX Error: " + e.toString(), testSuccess);
        }
    }

    @Test
    public void testValidMultiSegRespMaxLLLL() {

        boolean testSuccess = true;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 3; i++) {
            headerLLValues[i] = 32762;
            messageLengths[i] = 32762;
        }
        llzz = false;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);

        } catch (Exception e) {
            testSuccess = false;
            assertTrue("Error testing Valid single Segment response MAX Error: " + e.toString(), testSuccess);
        }
    }

    @Test
    public void testValidMultiSegRespMaxFirst() {
        boolean testSuccess = true;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        headerLLValues[0] = 32764;
        messageLengths[0] = 32764;

        for (int i = 0; i < 2; i++) {
            headerLLValues[i] = 26 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        llzz = true;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);

        } catch (Exception e) {
            testSuccess = false;
            assertTrue("Error testing Valid single Segment response MAX Error: " + e.toString(), testSuccess);
        }
    }

    @Test
    public void testValidMultiSegRespMaxFirstLLLL() {

        boolean testSuccess = true;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        headerLLValues[0] = 32762;
        messageLengths[0] = 32762;

        for (int i = 0; i < 2; i++) {
            headerLLValues[i] = 26 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        llzz = false;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);

        } catch (Exception e) {
            testSuccess = false;
            assertTrue("Error testing Valid single Segment response MAX Error: " + e.toString(), testSuccess);
        }
    }

    @Test
    public void testValidMultiSegRespMaxLast() {
        boolean testSuccess = true;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];

        for (int i = 1; i < 2; i++) {
            headerLLValues[i] = 26 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        headerLLValues[2] = 32764;
        messageLengths[2] = 32764;
        llzz = true;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);

        } catch (Exception e) {
            testSuccess = false;
            assertTrue("Error testing Valid single Segment response MAX Error: " + e.toString(), testSuccess);
        }
    }

    @Test
    public void testValidMultiSegRespMaxLastLLLL() {

        boolean testSuccess = true;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 2; i++) {
            headerLLValues[i] = 26 + (i * 2);
            messageLengths[i] = 26 + (i * 2);
        }
        headerLLValues[2] = 32762;
        messageLengths[2] = 32762;
        llzz = false;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);

        } catch (Exception e) {
            testSuccess = false;
            assertTrue("Error testing Valid single Segment response MAX Error: " + e.toString(), testSuccess);
        }
    }

    @Test
    public void testValidMultiSegRespMin() {
        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        headerLLValues[0] = 0; //Min 1st Seg
        messageLengths[0] = 0;
        headerLLValues[1] = 0; // Min seg for s
        messageLengths[1] = 0;
        headerLLValues[2] = 0;
        messageLengths[2] = 0;
        llzz = true;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegRespMinLLLL() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLLLValues = new int[3];
        int[] messageLengths = new int[3];
        headerLLLLValues[0] = 0;
        messageLengths[0] = 0;
        headerLLLLValues[1] = 0;
        messageLengths[1] = 0;
        headerLLLLValues[2] = 0;
        messageLengths[2] = 0;
        llzz = false;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValidMultiSegRespMinFirst() {
        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        headerLLValues[0] = 0; //Min 1st Seg
        messageLengths[0] = 0;
        headerLLValues[1] = 26; // Min seg for s
        messageLengths[1] = 26;
        headerLLValues[2] = 38;
        messageLengths[2] = 38;
        llzz = true;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegRespMinFirstLLLL() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLLLValues = new int[3];
        int[] messageLengths = new int[3];
        headerLLLLValues[0] = 0;
        messageLengths[0] = 0;
        headerLLLLValues[1] = 26;
        messageLengths[1] = 26;
        headerLLLLValues[2] = 38;
        messageLengths[2] = 38;
        llzz = false;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testValidMultiSegRespMinLast() {
        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLValues = new int[3];
        int[] messageLengths = new int[3];
        headerLLValues[0] = 26; //Min 1st Seg
        messageLengths[0] = 26;
        headerLLValues[1] = 26; // Min seg for s
        messageLengths[1] = 26;
        headerLLValues[2] = 0;
        messageLengths[2] = 0;
        llzz = true;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);

    }

    @Test
    public void testValidMultiSegRespMinFirstLast() {

        boolean testSuccess = true;
        String failureMsg = null;
        int[] headerLLLLValues = new int[3];
        int[] messageLengths = new int[3];
        headerLLLLValues[0] = 26;
        messageLengths[0] = 26;
        headerLLLLValues[1] = 26;
        messageLengths[1] = 26;
        headerLLLLValues[2] = 0;
        messageLengths[2] = 0;
        llzz = false;
        reqMsg = false;
        try {
            util.validateResponseTest(headerLLLLValues, messageLengths, llzz, reqMsg);
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

/*
 * begin Invalid message tests
 */
    @Test
    public void testInValidMSRespHeaderGTBufferFirst() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        headerValues[0] = 65;
        messageLengths[0] = 20;
        for (int i = 1; i < 3; i++) {
            headerValues[i] = 20;
            messageLengths[i] = 20;
        }
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
                found = tmp.indexOf("for message segment 0");
            if (found >= 0)
                testSuccess = true;

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSRespHeaderGTBufferFirstLLLL() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        headerValues[0] = 65;
        messageLengths[0] = 20;
        for (int i = 1; i < 3; i++) {
            headerValues[i] = 20;
            messageLengths[i] = 20;
        }
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
                found = tmp.indexOf("for message segment 0");
            if (found >= 0)
                testSuccess = true;
            else
                failureMsg = "Either message incorrect or segment invalid. Exception = " + tmp;
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSRespHeaderGTBufferLast() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 2; i++) {
            headerValues[i] = 20;
            messageLengths[i] = 20;
        }
        headerValues[2] = 65;
        messageLengths[2] = 20;
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
                found = tmp.indexOf("for message segment 2");
            if (found >= 0)
                testSuccess = true;

        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

    @Test
    public void testInValidMSRespHeaderGTBufferLastLLLL() {

        boolean testSuccess = false;
        String failureMsg = null;
        int[] headerValues = new int[3];
        int[] messageLengths = new int[3];
        for (int i = 0; i < 2; i++) {
            headerValues[i] = 20;
            messageLengths[i] = 20;
        }
        headerValues[2] = 65;
        messageLengths[2] = 20;

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
                found = tmp.indexOf("for message segment 2");
            if (found >= 0)
                testSuccess = true;
            else
                failureMsg = "Either message incorrect or segment invalid. Exception = " + tmp;
        } catch (Exception e) {
            failureMsg = e.toString();
            testSuccess = false;
        }
        assertTrue(failureMsg, testSuccess);
    }

}
