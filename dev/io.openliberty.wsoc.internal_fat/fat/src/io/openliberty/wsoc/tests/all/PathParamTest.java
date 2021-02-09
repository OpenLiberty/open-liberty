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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;

import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.util.wsoc.WsocTestRunner;
import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.Utils;
import io.openliberty.wsoc.endpoints.client.basic.BasicClientEP;
import io.openliberty.wsoc.endpoints.client.basic.PathParamClientEP;

/**
 * Tests WebSocket Stuff
 * 
 * @author unknown
 */
public class PathParamTest {

    public static int DEFAULT_TIMEOUT = Constants.getDefaultTimeout();
    private static final Logger LOG = Logger.getLogger(PathParamTest.class.getName());

    private WsocTest wsocTest = null;

    public PathParamTest(WsocTest test) {
        this.wsocTest = test;
    }

    protected void runEchoTest(Object tep, String resource, Object[] data, Object[] expectedData) throws Exception {
        runEchoTest(tep, resource, data, expectedData, Constants.getDefaultTimeout());
    }

    protected void runEchoTest(Object tep, String resource, Object[] data, Object[] expectedData, int timeout) throws Exception {
        WsocTestContext testdata = wsocTest.runWsocTest(tep, resource, WsocTestRunner.getDefaultConfig(), data.length, timeout);

        testdata.reThrowException();

        //actual response from ServerEndpoint, PathParamServerEP
        Object[] actualData = testdata.getMessage().toArray();

        LOG.log(Level.INFO, "actualData", actualData[0]);
        Assert.assertArrayEquals(expectedData, actualData);
    }

    protected void runEchoErrorTest(Object tep, String resource, Object[] data, Object[] expectedData, int timeout) throws Exception {
        boolean success = false;
        WsocTestContext testdata = wsocTest.runWsocTest(tep, resource, WsocTestRunner.getDefaultConfig(), data.length, timeout);
        if (testdata.getMessage().isEmpty()) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    /*
     * ServerEndpoint - @see PathParamServerEP
     */
    public void testAnnotatedPathParamSuccess() throws Exception {

        String[] textValues = { "WEREWR", "ERERE" };
        //At the ServerEndpoint (PathParamServerEP) @pathparam values gets appended to the returning response 
        String[] expectedData = { "WEREWR,testString,c,2,1", "ERERE,testString,c,2,1" };

        //server endpoint uri path is /basic/pathparamtest/{String-var}/{char-var}/{int-var}/{Integer-var}/{Long-var}/{long-var}/{Double-var}/{double-var}/{Short-var}/{short-var} 
        String path = "/basic/pathparamtest/testString/c/1/2/3/4/5/6/7/8";
        runEchoTest(new PathParamClientEP.TextTest(textValues), path, textValues, expectedData);

    }

    /*
     * ServerEndpoint - @see PathParamServerEP - ShortTest
     */
    public void testShortTypePathParamMessage() throws Exception {

        String[] textValues = { "-32768", "44" };
        //At the ServerEndpoint (PathParamServerEP) @pathparam values gets appended to the returning response 
        String[] expectedData = { "-32768", "44" };

        //server endpoint uri path is /basic/pathparamtest/{String-var}/{char-var}/{int-var}/{Integer-var}/{Long-var}/{long-var}/{Double-var}/{double-var}/{Short-var}/{short-var} 
        String path = "/basic/shorttest/8";
        runEchoTest(new PathParamClientEP.TestShort(textValues), path, textValues, expectedData);

    }

    /*
     * ServerEndpoint - @see PathParamServerEP
     */
    public void testAnotatedReaderPathParamSuccess() throws Exception {
        //server endpoint uri path is /basic/pathparamtest/{String-var}/{Double-var}/{double-var}/{Short-var}/{short-var}/{Byte-var}/{byte-var}/{Boolean-var}/{boolean-var}/{Float-var}/{float-var}
        String path = "/basic/pathparamtest/testString/5/6/7/8/9/10/false/true/11/12";

        String[] readerValues = { "testReader", "12345678910" };

        //At the ServerEndpoint (PathParamServerEP) @pathparam values gets appended to the returning response 
        String[] expectedData = { "testReader,5.0,6.0,7,8,9,10,false,true,11.0,12.0", "12345678910,5.0,6.0,7,8,9,10,false,true,11.0,12.0" };

        runEchoTest(new PathParamClientEP.ReaderTest(readerValues), path, readerValues, expectedData, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see PathParamBasicServerEP - TestOnOpen
     */
    public void TestOnOpenAndTestOnClose() throws Exception {
        //server endpoint uri path is /basic/pathparamonopentest/{String-var}/{Integer-var}
        String path = "/basic/pathparamonopentest/testString/1";

        String[] readerValues = { "msg1" };
        //onOpen() uses Intger-var = 1 as pathparam,which is passed in through this uri which gets 
        //appended to message.
        String[] expectedData = { "msg1,1" };

        runEchoTest(new BasicClientEP.TestOnOpen(readerValues), path, readerValues, expectedData);

        //server endpoint uri path is /basic/pathparamonerrortest/{String-var}/{Integer-var}
        String path2 = "/basic/pathparamonclosetest/testString/1";

        String[] readerValues2 = { "msg1" };
        //'testString' from onClose() method of previous test gets appended to the return mssage 
        String[] expectedData2 = { "msg1,testString" };

        runEchoTest(new BasicClientEP.TestOnClose(readerValues2), path2, readerValues2, expectedData2);
    }

    /*
     * ServerEndpoint - @see PathParamBasicServerEP - TestOnOpen
     */
    public void TestOnOpenThroughUpgrade(String path) throws Exception {

        //server endpoint uri path is /basic/pathparamonopentest/{String-var}/{Integer-var}
        //String path = "/basic/pathUpgradeServlet/testString/1";

        String[] readerValues = { "msg1" };
        //onOpen() uses Intger-var = 1 as pathparam,which is passed in through this uri which gets 
        //appended to message.
        String[] expectedData = { "msg1,1,TEST1,TEST2" };

        runEchoTest(new BasicClientEP.TestOnOpen(readerValues), path, readerValues, expectedData);
    }

    /*
     * Negative test for runtime exception during @PathParam value processing
     * 
     * ServerEndpoint - @see PathParamBasicServerEP - TestOnError
     * 
     * @ExpectedFFDC({ "java.lang.NumberFormatException", "javax.websocket.DecodeException" })
     */
    public void TestOnError() throws Exception {
        //server endpoint uri path is /basic/pathparamonclosetest/{String-var}/{Integer-var}
        String path = "/basic/pathparamonerrortest/testString/xyz"; //pass 'xyz' for {Integer-var} which will fail while processing @PathParam 

        String[] readerValues = { "msg1" };
        String[] expectedData = { "msg1,testString" };

        runEchoErrorTest(new BasicClientEP.TestOnError(readerValues), path, readerValues, expectedData, Constants.getShortTimeout());
        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }

    /*
     * Negative test for runtime exception during @PathParam value processing
     * 
     * ServerEndpoint - @see PathParamServerEP - UnmatchedStringPathParamTest
     * 
     * @ExpectedFFDC({ "javax.websocket.DecodeException" })
     */
    public void TestUnmatchedNonStringPathParamTest() throws Exception {
        //server endpoint uri path is /basic/pathparamtest/{param1}
        String path = "/basic/unmatchednonstring/xyz";

        String[] readerValues = { "msg1" };
        String[] expectedData = { "Error" };

        runEchoTest(new PathParamClientEP.TextTest(readerValues), path, readerValues, expectedData);
        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }

    /*
     * Negative test for runtime exception during @PathParam value processing
     * 
     * ServerEndpoint - @see PathParamServerEP - UnmatchedStringPathParamTest
     */
    public void TestUnmatchedStringPathParamTest() throws Exception {
        //server endpoint uri path is /basic/pathparamtest/{param}
        String path = "/basic/unmatchedstring/8";
        String[] readerValues = { "msg1" };
        String[] expectedData = { "success" };

        runEchoTest(new PathParamClientEP.TextTest(readerValues), path, readerValues, expectedData);
    }

    /**
     * This tests session.getPathParameters() in annotated endpoint
     * 
     * ServerEndpoint - @see PathParamServerEP - SessionPathParamTest
     */
    public void TestSessionPathParamTest() throws Exception {
        String path = "/basic/pathparamsessiontest/1";
        String[] readerValues = { "msg1" };
        //return value is '[pathparamVarNames] [pathparamVarValues]'
        String[] expectedData = { "[guest-id][1]" };

        runEchoTest(new PathParamClientEP.TextTest(readerValues), path, readerValues, expectedData);
    }

    public void RuntimeExceptionTCKTest() throws Exception {
        String path = "/basic/runtimeexceptionTCK/1";

        String[] input = { "msg1" };
        String[] output = { "1" };

        runEchoTest(new PathParamClientEP.TextTest(input), path, input, output);

        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }
}