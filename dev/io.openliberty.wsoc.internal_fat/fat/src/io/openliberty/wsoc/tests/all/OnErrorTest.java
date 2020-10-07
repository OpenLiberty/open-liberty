/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests.all;

import org.junit.Assert;

import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.util.wsoc.WsocTestRunner;
import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.Utils;
import io.openliberty.wsoc.endpoints.client.basic.BasicClientEP;

/**
 * Tests WebSocket Stuff
 * 
 * @author Rashmi Hunt
 */
public class OnErrorTest {

    public static int DEFAULT_TIMEOUT = Constants.getDefaultTimeout();

    private WsocTest wsocTest = null;

    public OnErrorTest(WsocTest test) {
        this.wsocTest = test;
    }

    //error test case
    protected void runEchoErrorTest(Object tep, String resource, Object[] data) throws Exception {
        runEchoErrorTest(tep, resource, data, Constants.getDefaultTimeout());
    }

    protected void runEchoErrorTest(Object tep, String resource, Object[] data, int timeout) throws Exception {
        boolean success = false;
        WsocTestContext testdata = wsocTest.runWsocTest(tep, resource, WsocTestRunner.getDefaultConfig(), data.length, timeout);
        if ((testdata.getMessage() == null) || testdata.getMessage().isEmpty()) {
            success = true;
        }
        Assert.assertTrue(success);
    }

    protected void runEchoTest(Object tep, String resource, Object[] data, Object[] expectedData) throws Exception {
        runEchoTest(tep, resource, data, expectedData, Constants.getDefaultTimeout());
    }

    protected void runEchoTest(Object tep, String resource, Object[] data, Object[] expectedData, int timeout) throws Exception {
        WsocTestContext testdata = wsocTest.runWsocTest(tep, resource, WsocTestRunner.getDefaultConfig(), data.length, timeout);
        testdata.reThrowException();
        Object[] actualData = testdata.getMessage().toArray();
        Assert.assertArrayEquals(expectedData, actualData);
    }

    /*
     * Negative test for runtime exception during @PathParam value processing
     * 
     * ServerEndpoint - @see OnErrorTestEP - TestOnMessageError
     * 
     * @ExpectedFFDC({ "java.lang.reflect.InvocationTargetException" })
     */
    public void TestOnMessageError() throws Exception {
        //server endpoint uri path is /basic/OnErrorTestEP/OnMessageError
        String path = "/basic/OnErrorTestEP/OnMessageError";

        String[] readerValues1 = { "FirstMessage" };
        runEchoErrorTest(new BasicClientEP.TestOnError(readerValues1), path, readerValues1, Constants.getShortTimeout());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {

        }

        String[] readerValues2 = { "SecondMessage" };
        String[] expectedData2 = { "Test success" };
        runEchoTest(new BasicClientEP.TestOnError(readerValues2), path, readerValues2, expectedData2, Constants.getShortTimeout());

        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }

    /*
     * onError test for programmatic endpoint
     * 
     * ServerEndpoint - @see ProgrammaticServerEP - OnErrorEndpoint
     * 
     * @ExpectedFFDC({ "java.lang.NullPointerException" })
     */
    public void TestProgramticEndpointError() throws Exception {

        //server endpoint uri path is /basic/OnErrorTestEP/OnMessageError
        String path = "/basic/codedOnError";

        String[] readerValues1 = { "FirstMessage" };
        runEchoErrorTest(new BasicClientEP.TestOnError(readerValues1), path, readerValues1, Constants.getShortTimeout());

        String[] readerValues2 = { "SecondMessage" };
        String[] expectedData2 = { "Test success" };
        runEchoTest(new BasicClientEP.TestOnError(readerValues2), path, readerValues2, expectedData2, Constants.getShortTimeout());

        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }

    /*
     * onError test for programmatic endpoint
     * 
     * 
     * ServerEndpoint - @see BinaryDecodeEncodeServerEP
     * 
     * @ExpectedFFDC({ "javax.websocket.DecodeException" })
     */
    public void TestDecoderError() throws Exception {

        //server endpoint uri path is /basic/OnErrorTestEP/OnMessageError
        String path = "/basic/BinaryDecodeEncode";

        String[] input = { "Error decoder case" };
        String[] expectedData = { "successfull" };
        runEchoTest(new BasicClientEP.TestOnError(input), path, input, expectedData);

        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }

    /*
     * onError test for programmatic endpoint
     * 
     * ServerEndpoint - @see BinaryDecodeEncodeServerEP
     * 
     * @ExpectedFFDC({ "javax.websocket.EncodeException" })
     */
    public void TestEncoderError() throws Exception {

        //server endpoint uri path is /basic/OnErrorTestEP/OnMessageError
        String path = "/basic/BinaryDecodeEncode";

        String[] input = { "Error encoder case" };
        String[] expectedData = { "successfull" };
        runEchoTest(new BasicClientEP.TestOnError(input), path, input, expectedData);

        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }

    public void TCKTestEncoderRuntimeException() throws Exception {

        String path = "/basic/TCKEncodeException";

        String[] input = { "Error encoder case" };
        String[] expectedData = { "SUCCESS" };
        runEchoTest(new BasicClientEP.TestOnError(input), path, input, expectedData);

        // Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }

}