/*******************************************************************************
 * Copyright (c) 2013, 2019 IBM Corporation and others.
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
package io.openliberty.wsoc.tests.all;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.Utils;
import io.openliberty.wsoc.endpoints.client.basic.AnnotatedClientEP;
import io.openliberty.wsoc.endpoints.client.basic.IdleTimeoutTCKClientEP;
import io.openliberty.wsoc.endpoints.client.basic.ProgrammaticClientEP;
import io.openliberty.wsoc.endpoints.client.basic.ProgrammaticMaxMessageSizeClientEP;
import io.openliberty.wsoc.util.wsoc.TestWsocContainer;
import io.openliberty.wsoc.util.wsoc.WsocTest;
import junit.framework.Assert;

/**
 * Tests WebSocket Stuff
 *
 * @author unknown
 */
public class AnnotatedTest {

    private WsocTest wsocTest = null;

    public AnnotatedTest(WsocTest test) {
        this.wsocTest = test;
    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - SessionIdleTimeoutEndpoint
     */
    public void testMaxSessionIdleTimeout() throws Exception {

        Object[] result = { "15000", 1006 };
        long startTime = System.currentTimeMillis();
        wsocTest.runEchoTest(new AnnotatedClientEP.SessionIdleTest(), "/basic/annotatedIdleTimeout", result, Constants.getLongTimeout());
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        if ((totalTime < 15000) || (totalTime > 40000)) {
            Assert.fail("Test ran for " + totalTime + " which is either less than 10 seconds or more than 25 seconds,  outside of an acceptable session timeout range.");
        }
    }

    /**
     * ServerEndpoint - @see SessionIdleTimeOutTCKServerEP
     */
    public void testMaxSessionIdleTimeoutTCKStyle() throws Exception {
        // send 5 bytes
        // set Max idle timeout to 15000 on the server.  TCK test this by having the server onMessage sits on the thread for longer
        // than the idel timeout.

        int idleWait = 15000; // should match the value used in SessionIdleTimeOutTCKServerEP
        String[] input1 = { "Text1" };
        String[] output1 = { "SUCCESS" }; // "output" here is actually set in the onClose on the Client side
        String path = "/basic/annotatedIdleTimeoutTCK";
        //session timeout is set to 15 seconds. Wait for 1 minutes to terminate the client
        wsocTest.runEchoTest(new IdleTimeoutTCKClientEP(input1), path, output1, idleWait * 3);

    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - ByteBufferTest
     */
    public void testAnnotatedByteArraySuccess() throws Exception {

        byte[][] data = Utils.getRandomBinaryByteArray(5, 100);
        byte[][] orig = Utils.duplicateByteArray(data);
        //server endpoint uri is /annotatedByteArray/{boolean-var}
        String uri = "/basic/annotatedByteArray/true";
        wsocTest.runEchoTest(new AnnotatedClientEP.ByteArrayTest(data), uri, orig);

    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - ReaderTest
     */
    public void testAnnotatedReaderSuccess() throws Exception {

        String[] readerValues = { "blahblahblahblah", "12345678910" };
        // String[] readerValues = { "2blahblahblahblah", "343asfasdfasf" };
        wsocTest.runEchoTest(new AnnotatedClientEP.ReaderTest(readerValues), "/basic/annotatedReader", readerValues);

    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - ReaderTest
     */
    public void testAnnotatedTextSuccess() throws Exception {

        String[] textValues = { "WEREWR", "ERERE" };
        //server endpoint uri is /annotatedText/{boolean-var}
        String uri = "/basic/annotatedText/true";
        wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(textValues), uri, textValues);

    }

    // /*
    //  * ServerEndpoint - @see AnnotatedServerEP - ZosTextTest
    //  */
    // public void testZosAnnotatedTextSuccess() throws Exception {
    //     // Matches WLM Transaction Class defined in the server.xml.
    //     String[] textValues = { "mikeTC2 " };

    //     //server endpoint uri is /zannotatedText/{boolean-var}
    //     String uri = "/basic/zannotatedText/true";
    //     wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(textValues), uri, textValues);

    // }

    /*
     * ServerEndpoint - @see AnnotatedServerEP -TextTest
     */
    public void testAsyncAnnotatedTextSuccess() throws Exception {

        String[] textValues = { "WEREWR", "ERERE" };
        wsocTest.runEchoTest(new AnnotatedClientEP.AsyncTextTest(textValues), "/basic/annotatedAsyncText", textValues);

    }

    /*
     * ServerEndpoint - @see AnnotatedFutureTextServerEP
     */
    public void testFutureAnnotatedTextSuccess() throws Exception {

        String[] textValues = { "WEREWR", "ERERE" };
        wsocTest.runEchoTest(new AnnotatedClientEP.FutureTextTest(textValues), "/basic/annotatedFutureText", textValues);

    }

    /*
     * ServerEndpoint - @see AnnotatedFutureWithReturnTextServerEP
     */
    public void testFutureAnnotatedWithReturnTextSuccess() throws Exception {

        String[] input = { "OneInstance" };
        String[] output = { "OneInstance", "OneInstanceOneInstance" };
        wsocTest.runEchoTest(new AnnotatedClientEP.FutureWithReturnTextTest(input), "/basic/annotatedFutureWithReturnText", output);

    }

    /*
     * ServerEndpoint - @see AnnotatedFutureWithReturnTextServerEP
     */
    public void testFutureAnnotatedWithReturnByteSuccess() throws Exception {

        byte[] input = { Byte.valueOf("112").byteValue() };
        // first value will get 11 added to it by the server, bytes are signed, so they go from -128 to 127.
        String[] output = { "112", "123" };
        wsocTest.runEchoTest(new AnnotatedClientEP.FutureWithReturnByteTest(input), "/basic/annotatedFutureWithReturnByte", output);

    }

    String output = "123";

//    /**
//     * TODO - get this one working.
//     */
//
//    public void AnnotatedBooleanSuccess() throws Exception {
//
//        boolean[] data = { true, false, true, false, false, false, false, true, true, true };
//        WsocTestContext testdata = SHARED_SERVER.runWsocTest(new AnnotatedEndpoint.BooleanTest(data), "/basic/annotatedBoolean",
//                                                             WsocTestRunner.getDefaultConfig(), data.length, DEFAULT_TIMEOUT);
//        testdata.reThrowException();

//        for (int x = 0; x < testdata.getMessage().size(); x++) {
//            Boolean val = (Boolean) testdata.getMessage().get(x);
//            if (data[x] != val.booleanValue()) {
//                throw new WsocTestException("TEST FAILED");
//            }
//        }
//
//    }

    /*
     * ServerEndpoint - @see AnnotatedVoidReturnServerEP
     */
    public void testAnnotatedVoidOnMsgReturn() throws Exception {

        String[] textValues = { "WEREWR", "ERERE" };
        wsocTest.runEchoTest(new AnnotatedClientEP.OnMsgVoidReturnTest(textValues), "/basic/annotatedOnMsgVoidReturn", textValues);

    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - TextTest
     */
    public void testAnnotatedPingSuccess() throws Exception {

        // PLEASE DO NOT USE RANDOM TEST DATA!!!

        byte[][] data = Utils.getRandomBinaryByteArray(5, 100);
        byte[][] orig = Utils.duplicateByteArray(data);
        //server endpoint uri is /annotatedText/{boolean-var}
        String uri = "/basic/annotatedText/true";
        wsocTest.runEchoTest(new AnnotatedClientEP.PingTest(data), uri, orig);

    }

    /*
     * ServerEndpoint - @see PongServerEP
     */
    public void testAnnotatedPingPongSuccess() throws Exception {

        // !! This is really just a Pong test, because when a server sends a Ping to the Jetty client code sends
        // that to the clientapp's pong message handler.

        // PLEASE DO NOT USE RANDOM TEST DATA!!!

        byte[][] data = Utils.getRandomBinaryByteArray(5, 100);
        byte[][] orig = Utils.duplicateByteArray(data);
        //server endpoint uri is /annotatedPong/{boolean-var}
        wsocTest.runEchoTest(new AnnotatedClientEP.PingPongTest(data), "/basic/annotatedPong/true", orig);

    }

    /*
     * ServerEndpoint - @see PongServerEP
     */
    public void testAnnotatedTextMessageSuccess() throws Exception {

        String[] textValues = { "WEREWR", "ERERE" };
        //server endpoint uri is /annotatedPong/{boolean-var}
        String uri = "/basic/annotatedPong/true";
        wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(textValues), uri, textValues);

    }

    /**
     * Tests custom ServerEndpoint Configurator's getEndpointInstance() method - 3.1.7 Customizing Endpoint Creation
     *
     * ServerEndpoint - @see AnnotatedServerEP - TextTest
     */
    public void testConfiguratorGetEndpointInstance() throws Exception {

        String[] textValues = { "Text1" };
        wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(textValues), "/basic/endpointInstanceConfigurator", textValues);

    }

    /**
     * Tests custom ServerEndpoint Configurator's getEndpointInstance() method - 3.1.7 Customizing Endpoint Creation
     * this test shows how customer can share same server endpoint instance across 2 client calls.
     *
     * ServerEndpoint - @see AnnotatedServerEP - TextTest
     */
    public void testConfiguratorGetEndpointInstanceShared() throws Exception {

        String[] textValues = { "Text1" };
        wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(textValues), "/basic/sharedEndpointInstanceConfigurator", textValues);

        wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(textValues), "/basic/sharedEndpointInstanceConfigurator", textValues);
    }

    /*
     * ServerEndpoint - @see AnnotatedPartialServerEP - AnnotatedPartialByteBufferTest
     */
    public void testProgrammaticPartialBinarySuccess1() throws Exception {

        String s1 = "FirstMessage";
        String s2 = "Second";
        String s3 = "Third";

        ByteBuffer[] ba = new ByteBuffer[3];
        ba[0] = ByteBuffer.wrap(s1.getBytes());
        ba[1] = ByteBuffer.wrap(s2.getBytes());
        ba[2] = ByteBuffer.wrap(s3.getBytes());

        String[] expected = { s1, s2, s3 };

        wsocTest.runEchoTest(new ProgrammaticClientEP.PartialBinaryTest(ba), "/basic/annotatedPartialByteBuffer", expected, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see AnnotatedPartialServerEP - AnnotatedPartialByteArray
     */
    public void testProgrammaticPartialBinarySuccess2() throws Exception {

        String s1 = "FirstMessageArray";
        String s2 = "SecondArray";
        String s3 = "ThirdArray";

        ByteBuffer[] ba = new ByteBuffer[3];
        ba[0] = ByteBuffer.wrap(s1.getBytes());
        ba[1] = ByteBuffer.wrap(s2.getBytes());
        ba[2] = ByteBuffer.wrap(s3.getBytes());

        String[] expected = { s1, s2, s3 };

        wsocTest.runEchoTest(new ProgrammaticClientEP.PartialBinaryTest(ba), "/basic/annotatedPartialByteArray", expected, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see AnnotatedPartialServerEP - AnnotatedPartialTextTest
     */
    public void testProgrammaticPartialTextSuccess3() throws Exception {

        String[] textValues = { "MESSAGE1", "SecondOne", "AndTheLast" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.PartialTextTest(textValues), "/basic/annotatedPartialText", textValues, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see AnnotatedPartialServerEP - AnnotatedPartialTextTest
     */
    public void testProgrammaticPartialTextWithClientPing() throws Exception {

        String s1 = "Message1FromClient";
        String sPing = "This is a ping pong message"; // needs to match string in client side ProgrammaticEndpoint
        String s3 = "Message3FromClient";
        String s4 = "Message4FromClient";

        String[] textValues = { s1, s3, s4 };
        String[] expected = { s1, sPing, s3, s4 };

        wsocTest.runEchoTest(new ProgrammaticClientEP.PartialTextTestWithPong(textValues), "/basic/annotatedPartialText", expected, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - DoubleTest
     */
    public void testDoubleTypeMessage() throws Exception {
        String[] input = { "-32768", "44" };
        String[] output = { "-32768.0", "44.0" };
        String path = "/basic/annotatedDouble";
        wsocTest.runEchoTest(new AnnotatedClientEP.TestDouble(input), path, output);
    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - DoubleTest
     */
    public void testFloatTypeMessage() throws Exception {
        String[] input = { "20.33", "-333.456" };
        String[] output = { "20.33", "-333.456" };
        String path = "/basic/annotatedFloat";
        wsocTest.runEchoTest(new AnnotatedClientEP.TestFloat(input), path, output);
    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - FloatTest
     */
    public void testLongTypeMessage() throws Exception {

        String[] input = { "454532768", "-200" };
        String[] output = { "454532768", "-200" };
        String path = "/basic/annotatedLong";
        wsocTest.runEchoTest(new AnnotatedClientEP.TestLong(input), path, output);
    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - IntegerTest
     */
    public void testIntegerTypeMessage() throws Exception {
        String[] input = { "-1", "1" };
        String[] output = { "-1", "1" };
        String path = "/basic/annotatedInteger";
        wsocTest.runEchoTest(new AnnotatedClientEP.TestInteger(input), path, output);
    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - ShortTest
     */
    public void testShortTypeMessage() throws Exception {
        String[] input = { "-32768", "0" };
        String[] output = { "-32768", "0" };
        String path = "/basic/annotatedShort";
        wsocTest.runEchoTest(new AnnotatedClientEP.TestShort(input), path, output);
    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - ShortPrimitiveTest
     */
    public void testPrimitiveShortTypeMessage() throws Exception {
        String[] input = { "-1111", "11" };
        String[] output = { "-1111", "11" };
        String path = "/basic/annotatedshort";
        wsocTest.runEchoTest(new AnnotatedClientEP.TestPrimitiveShort(input), path, output);
    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - BooleanTest
     */
    public void testBooleanTypeMessage() throws Exception {
        String[] input = { "true", "false" };
        String[] output = { "true", "false" };
        String path = "/basic/annotatedBoolean";
        wsocTest.runEchoTest(new AnnotatedClientEP.BooleanTest(input), path, output);
    }

    /**
     * Test for this section of the spec
     * "3.1.6 Custom State or Processing Across Server Endpoint Instances
     * The developer may also implement ServerEndpointConfig.Configurator in order to hold custom application
     * state or methods for other kinds of application specific processing that is accessible from all Endpoint
     * instances of the same logical endpoint via the EndpointConfig object."
     *
     * ServerEndpoint - @see CustomStateConfigurator
     */
    public void testCustomStateConfigurator() throws Exception {

        String[] input1 = { "text1" };
        String[] output1 = { "1" };
        //call1
        wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(input1), "/basic/customStateConfigurator", output1);
        String[] input2 = { "text1" };
        String[] output2 = { "2" };
        wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(input2), "/basic/customStateConfigurator", output2);
    }

    /**
     * This test show cases MessageHandler used in inheritance scenario
     *
     * ServerEndpoint - @see WillDecodeTextServerEP
     */
    public void testWillDecode() throws Exception {
        String[] data = new String[1];
        String[] result = new String[1];
        data[0] = "A:B:C";
        //server side encoder switches 1st and 3rd entry.
        result[0] = "C:B:A";
        wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(data), "/basic/willdecodetextendpoint", result);
    }

    /**
     * This test uses parameterized types in encoders/decoders... no inheritance
     *
     * ServerEndpoint - @see ParamTypeCodingServerEP
     */
    public void testParamTypeCoding() throws Exception {
        String[] data = { "QWREQEWRWE", "1243123123" };

        wsocTest.runEchoTest(new AnnotatedClientEP.ParamTypeCodingTest(data), "/basic/complexCoding", data);
    }

    /*
     * this tests encode using Generics with inheritance
     */
    public void testEncodeGeneric() throws Exception {
        String[] data = new String[1];
        data[0] = "anything";
        String[] result = new String[1];
        result[0] = Constants.ENCODER_GENERIC_SUCCESS;

        wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(data), "/basic/EncodeGeneric", result);
    }

    /*
     * tests maxMessageSize of binary type
     * 1st client call - incoming message size is 5 which is larger than maxMessageSize. Hence @OnMessage doesn't get called. Instead,
     *
     * @OnClose gets called with closeCode TOO_BIG(1009). closeCode is set in the member variable of this endoint instance.
     * 2nd client call - incoming message size is 4 which is <= maxMessageSize. Hence @OnMessage gets invoked. @OnMessage returns the
     * closeCode set from previous client call from onClose() and output2 checks for closeCode 1009
     *
     * ServerEndpoint - @see AnnotatedServerEP - MaxBinaryMessageTest
     *
     * @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
     */
    public void testMaxMessageSize() throws Exception {

        byte[][] data = new byte[2][];
        data[0] = "1234".getBytes();
        data[1] = "12345".getBytes();

        Object[] results = new Object[2];
        results[0] = "1234".getBytes();
        results[1] = 1009;

        wsocTest.runEchoTest(new AnnotatedClientEP.MaxBinaryMessageSizeTest(data), "/basic/annotatedMaxBinaryMessage", results);

        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }

    /*
     * tests get/setMaxBinaryMessageBufferSize on the Session object, and that the initial default value on the Container object is honored.
     * 1st client call - Send initial message
     * 2nd client message - Send a message of the exact default size which is owned by the Container
     * Server should read in first two messaage, sending back a "SUCCESS". Then server side will lower the size via the Session API
     * 3rd client message - should be too big, and to onClose should see the closeCode TOO_BIG(1009)
     *
     * ServerEndpoint - @see ProgrammaticServerEP - MaxMessageSizeInSession
     *
     * @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
     */
    public void testProgrammaticMaxMessageSize() throws Exception {

        String[] input1 = { "Text1" };
        String[] output1 = { "SUCCESS", "SUCCESS", "TOO_BIG or UNEXPECTED_CONDITION" };
        String path = "/basic/programmaticMaxMessageSize";
        wsocTest.runEchoTest(new ProgrammaticMaxMessageSizeClientEP(input1), path, output1);

    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - MaxTextMessageTest
     *
     * @ExpectedFFDC({ "com.ibm.ws.wsoc.MaxMessageException" })
     */
    public void testMaxTextMessageSize() throws Exception {

        String[] data = new String[2];
        data[0] = "1234";
        data[1] = "12345";

        Object[] results = new Object[2];
        results[0] = "1234";
        results[1] = 1009;

        wsocTest.runEchoTest(new AnnotatedClientEP.MaxTextMessageSizeTest(data), "/basic/annotatedMaxTextMessage", results);

        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }

    /*
     * Tests defaults in @ServerEndpoint
     *
     * ServerEndpoint - @see DefaultsEndpointServerEP
     */
    public void testDefaults() throws Exception {
        String[] input = { "decoders" };
        String[] output = { "[class io.openliberty.wsoc.common.BinaryStreamDecoder]" };
        String path = "/basic/defaults";
        wsocTest.runEchoTest(new AnnotatedClientEP.TextTest(input), path, output);
    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - PrimiteByteTest
     */
    public void testAnnotatedPrimitiveByte() throws Exception {

        byte input = Byte.valueOf("123").byteValue();
        String output = "123";
        String uri = "/basic/annotatedbyte";
        wsocTest.runEchoSingleObjectTest(new AnnotatedClientEP.ByteTest(input), uri, output);

    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - PrimitivebyteReturnTest
     */
    public void testbyteReturnTypeMessage() throws Exception {
        String input = "123";
        byte output = Byte.valueOf("123").byteValue();
        String uri = "/basic/annotatedbytereturn";
        wsocTest.runEchoSingleObjectTest(new AnnotatedClientEP.ByteReturnTest(input), uri, output);
    }

    /*
     * ServerEndpoint - @see AnnotatedFutureTextServerEP
     */
    public void testListenerAddAnnotatedEndpointTextSuccess() throws Exception {

        String[] textValues = { "WEREWR", "ERERE" };
        wsocTest.runEchoTest(new AnnotatedClientEP.FutureTextTest(textValues), "/basic/annotatedAddEndpoint", textValues);

    }

    /*
     * ServerEndpoint - @see AnnotatedPartialServerEP - AnnotatedPartialSenderWholeReceiverTest
     */
    public void testClientProgWholeServerAnnotatedPartial() throws Exception {

        String[] textValues = { "MESSAGE1", "SecondOne", "AndTheLast" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.ProgrammaticPartialTextTest(textValues), "/basic/annotatedPartialSenderText", textValues, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - PartialSenderWholeReceiverEndpoint
     */
    public void testClientAnnoWholeServerProgPartial() throws Exception {

        String[] textValues = { "MESSAGE1", "SecondOne", "AndTheLast" };
        wsocTest.runEchoTest(new AnnotatedClientEP.AnnonotatedPartialTextTest(textValues), "/basic/codedPartialSenderText", textValues, Constants.getLongTimeout());

    }

    /*
     *
     */
    private static final Logger LOG = Logger.getLogger(AnnotatedTest.class.getName());
    public static String connectToClassResult = null;

    public void testConnectToClass() throws Exception {
        connectToClassResult = null;
        WebSocketContainer c = TestWsocContainer.getRef();

        String queryParms = "?testMe=HI";
        LOG.info("adding to uri: " + queryParms);

        Class<?> blah = AnnotatedClientEP.DoNothingTest.class;

        Session sess = c.connectToServer(blah, new URI(wsocTest.getServerUrl("/basic/codedTextQueryParms" + queryParms)));

        // wait up to 30 seconds for a result
        int i = 0;
        java.lang.Thread.sleep(2000);
        while ((connectToClassResult == null) && (i < 14)) {
            i++;
            java.lang.Thread.sleep(2000);
        }

        LOG.info("connectToClassResult: " + connectToClassResult);

        if (connectToClassResult == null) {
            Assert.fail("testConnectToClass returned no results");
        }

        if ((connectToClassResult.indexOf(Constants.SUCCESS) == -1)
            || (connectToClassResult.indexOf(Constants.FAILED) != -1)) {
            Assert.fail("testConnectToClass failed with following output: " + connectToClassResult);
        }

        LOG.info("testConnectToClass closing websocket session");

        sess.close();

    }
}