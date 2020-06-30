/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests.all;

import java.nio.ByteBuffer;

import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.Utils;
import io.openliberty.wsoc.endpoints.client.basic.ProgrammaticClientEP;

/**
 * Tests WebSocket Stuff
 * 
 * @author unknown
 */
public class ProgrammaticTest {

    public static int DEFAULT_TIMEOUT = Constants.getDefaultTimeout();

    private WsocTest wsocTest = null;

    public ProgrammaticTest(WsocTest test) {
        this.wsocTest = test;
    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - TextEndpoint
     */
    public void testProgrammaticTextSuccess() throws Exception {

        testProgrammaticTextSuccess("/basic/codedText");
        testProgrammaticTextSuccess("/basic/extendedText");

    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - TextEndpoint
     */
    public void testProgrammaticTextSuccess(String path) throws Exception {
        String[] textValues = { "WEREWR", "ERERE" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.TextTest(textValues), path, textValues);

    }

///basic/upgradeServlet/EndpointConfig/basic.war.CodedServerEndpointConfig$TextEndpointConfig
    /*
     * ServerEndpoint - @see ProgrammaticServerEP - PartialTextEndpointConfig
     */
    public void testProgrammaticPartialTextSuccess() throws Exception {

        String[] textValues = { "WEREWR", "ERERE" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.PartialTextTest(textValues), "/basic/codedPartialText", textValues, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - PartialTextEndpointConfig2
     */
    public void testProgrammaticPartialTextSuccess2() throws Exception {

        String[] textValues = { "MESSAGE1", "SecondOne", "AndTheLast" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.PartialTextTest(textValues), "/basic/codedPartialText2", textValues, Constants.getLongTimeout());
    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - PartialTextWithSendingEmbeddedPingEndpoint
     */
    public void testProgrammaticPartialTextWithServerPing() throws Exception {
        testProgrammaticPartialTextWithServerPing("/basic/PartialTextWithSendingEmbeddedPingEndpoint");
    }

    public void testProgrammaticPartialTextWithServerPing(String path) throws Exception {

        String s1 = "Message1FromClient";
        String sPing = Constants.PING_PONG_FROM_SERVER_MSG;
        String s3 = "Message3FromClient";
        String s4 = "Message4FromClient";

        String[] textValues = { s1, s3, s4 };
        String[] expected = { s1, s3, s4, sPing };

        wsocTest.runEchoTest(new ProgrammaticClientEP.PartialTextTest(textValues), path, expected, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - CloseEndpoint
     */
    public void testProgrammaticCloseSuccess() throws Exception {

        String[] textValues = { "1007:THIS IS A TEST CLOSING STATUS" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.CloseTest(textValues), "/basic/codedClose", textValues);

    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - ReaderEndpointConfig
     */
    public void testProgrammaticReaderSuccess() throws Exception {

        String[] readerValues = { "blahblahblahblah", "12345678910" };
        // String[] readerValues = { "2blahblahblahblah", "343asfasdfasf" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.ReaderTest(readerValues), "/basic/codedReader", readerValues);

        wsocTest.runEchoTest(new ProgrammaticClientEP.ReaderTest(readerValues), "/basic/extendedReader", readerValues);

    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - ByteArrayEndpointConfig
     */
    public void testProgrammaticByteArraySuccess() throws Exception {

        // PLEASE DO NOT USE RANDOM TEST DATA!!!

        byte[][] data = Utils.getRandomBinaryByteArray(5, 100);
        byte[][] orig = Utils.duplicateByteArray(data);

        wsocTest.runEchoTest(new ProgrammaticClientEP.InputStreamTest(data), "/basic/codedByteArray", orig);

        //   MSN - GETTING PERIOD FAILURES...
        //  data = Utils.getRandomBinaryByteArray(5, 100);
        // orig = Utils.duplicateByteArray(data);

        // runEchoTest(new ProgrammaticEndpoint.InputStreamTest(data), "/basic/extendedByteArray", orig);

    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - InputStreamEndpointConfig
     */
    public void testProgrammaticInputStreamSuccess() throws Exception {

        // PLEASE DO NOT USE RANDOM TEST DATA!!!

        byte[][] data = Utils.getRandomBinaryByteArray(5, 100);
        byte[][] orig = Utils.duplicateByteArray(data);

        wsocTest.runEchoTest(new ProgrammaticClientEP.ByteArrayTest(data), "/basic/codedInputStream", orig);

        data = Utils.getRandomBinaryByteArray(5, 100);
        orig = Utils.duplicateByteArray(data);

        wsocTest.runEchoTest(new ProgrammaticClientEP.ByteArrayTest(data), "/basic/extendedInputStream", orig);

    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - ByteBufferEndpoint
     */
    public void testProgrammaticByteBufferSuccess() throws Exception {

        //PLEASE DO NOT USE RANDON TEST DATA!!
        // first message needs to be within the default buffer size, so server side has time to change the default buffer size 
        ByteBuffer[] buf1 = Utils.getRandomBinaryByteBuffer(1, 32000);
        ByteBuffer[] buf4 = Utils.getRandomBinaryByteBuffer(4, 65500);
        ByteBuffer[] bufs = new ByteBuffer[] { buf1[0], buf4[0], buf4[1], buf4[2], buf4[3] };
        // ByteBuffer[] bufs = Utils.getRandomBinaryByteBuffer(5, 65500); 

        ByteBuffer[] orig = Utils.duplicateByteBuffers(bufs);

        wsocTest.runEchoTest(new ProgrammaticClientEP.ByteBufferTest(bufs), "/basic/codedByteBuffer", orig);

        buf1 = Utils.getRandomBinaryByteBuffer(1, 32000);
        buf4 = Utils.getRandomBinaryByteBuffer(4, 65500);
        bufs = new ByteBuffer[] { buf1[0], buf4[0], buf4[1], buf4[2], buf4[3] };
        // bufs = Utils.getRandomBinaryByteBuffer(5, 65500);

        orig = Utils.duplicateByteBuffers(bufs);

        wsocTest.runEchoTest(new ProgrammaticClientEP.ByteBufferTest(bufs), "/basic/extendedByteBuffer", orig);

    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - CodingEndpoint
     */
    public void testProgrammaticCodinguccess() throws Exception {

        //Must 3 send 3 Strings separated by :  - server side encoder swithces 1 and 3rd entry.
        String[] data = new String[3];
        data[0] = "AAAAAAA:BBBBBBBBBB:CCCCCC";
        data[1] = "DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD:E:F";
        data[2] = "a;slkd32:zjxcnvsf:^R&#*$&^#";

        String[] retData = new String[3];
        retData[0] = "CCCCCC:BBBBBBBBBB:AAAAAAA";
        retData[1] = "F:E:DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD";
        retData[2] = "^R&#*$&^#:zjxcnvsf:a;slkd32";

        wsocTest.runEchoTest(new ProgrammaticClientEP.CodingTest(data), "/basic/extendedCoding", retData);

    }

    /**
     * This test show cases MessageHandler used in inheritance scenario
     * 
     * ServerEndpoint - @see MsgHandlerInheritanceServerEP
     */
    public void testMsgHandlerInheritance() throws Exception {
        String[] data = new String[1];
        String[] result = new String[1];
        data[0] = "A:B:C";
        //server side encoder swithces 1 and 3rd entry.
        result[0] = "C:B:A";
        wsocTest.runEchoTest(new ProgrammaticClientEP.CodingTest(data), "/basic/msgHandlerInheritance", result);
    }

    /**
     * This tests session.getPathParameters() in programmatic endpoint
     * 
     * ServerEndpoint - @see ProgrammaticServerEP - SessionPathParamEndpoint
     */
    public void testProgEndpointSessionGetPathParamaters() throws Exception {
        String[] data = new String[1];
        String[] result = new String[1];
        data[0] = "a";
        result[0] = "[guest-id][1]"; //return value is '[pathparamVarNames] [pathparamVarValues]'
        wsocTest.runEchoTest(new ProgrammaticClientEP.CodingTest(data), "/basic/sessionpathaparam/1", result);
    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP -CloseEndpointOnOpen
     */
    public void testProgrammaticCloseSuccessOnOpen() throws Exception {

        String[] textValues = { "1001:THIS IS A TEST CLOSING STATUS FROM onOPEN" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.CloseTestOnOpen(textValues), "/basic/codedCloseOnOpen", textValues);

    }
}