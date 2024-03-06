/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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

import java.util.logging.Logger;

import org.junit.Assert;

import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.endpoints.client.basic.AnnotatedClientEP;
import io.openliberty.wsoc.endpoints.client.basic.ProgrammaticClientEP;
import io.openliberty.wsoc.endpoints.client.context.SimpleClientEP;
import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.util.wsoc.WsocTestRunner;

/**
 *
 */
public class CdiTest {

    private static final Logger LOG = Logger.getLogger(CdiTest.class.getName());

    // hardcode string that must match what the server side gives back
    private final String successForTestCdiInterceptor = "Intercepted:" + "receiveMessageCDI";

    private final String successForTestCdiIntject = ":ProducerBean Field Injected:SimpleWebSocketBean Inside Contructor:SimpleWebSocketBean Inside PostContruct:SimpleWebSocketBean Inside getResponse";

    private static int sequenceCounter = 0;
    private static int sequenceCounter12 = 0;
    private static int sequenceCounter20 = 0;

    private WsocTest wsocTest = null;

    public CdiTest(WsocTest test) {
        this.wsocTest = test;
    }

    public static void resetTests() {
        sequenceCounter = 0;
        sequenceCounter12 = 0;
        sequenceCounter20 = 0;
    }

    /*
     * ServerEndpoint - @see AnnotatedEndpointCDI1
     */
    public void testCdiInterceptor() throws Exception {

        int testTimeout = Constants.getLongTimeout();
        String writeData = "From testCdiInterceptor";
        WsocTestContext testdata = wsocTest.runWsocTest(new io.openliberty.wsoc.endpoints.client.context.SimpleClientEP.WriteAndRead(writeData),
                                                        "/cdi/SimpleAnnotatedCDI1", WsocTestRunner.getDefaultConfig(), 1, testTimeout);

        String resultMessage = testdata.getSingleMessage();
        testdata.reThrowException(); // throw an exception if there was one

        if ((resultMessage.compareTo(successForTestCdiInterceptor)) != 0) {
            Assert.fail("test failed with following output: " + resultMessage);
        }
    }

    /*
     * ServerEndpoint - @see AnnotatedEndpointFieldInjection
     */
    public void testCdiInject() throws Exception {

        int testTimeout = Constants.getLongTimeout();
        String writeData = "From testCdiInject";
        WsocTestContext testdata = wsocTest.runWsocTest(new SimpleClientEP.WriteAndRead(writeData),
                                                        "/cdi/EPFieldInjectionCDI", WsocTestRunner.getDefaultConfig(), 1, testTimeout);

        String resultMessage = testdata.getSingleMessage();
        testdata.reThrowException(); // throw an exception if there was one

        if ((resultMessage.compareTo(successForTestCdiIntject)) != 0) {
            Assert.fail("test failed with following output: " + resultMessage);
        }

    }

    /*
     * ServerEndpoint - @see AnnotatedEndpointFieldInjection
     */
    public void testCdiInjectCDI12() throws Exception {

        int testTimeout = Constants.getLongTimeout();
        String writeData = "From testCdiInject";
        WsocTestContext testdata = wsocTest.runWsocTest(new SimpleClientEP.WriteAndRead(writeData),
                                                        "/cdi/EPFieldInjectionCDI12", WsocTestRunner.getDefaultConfig(), 1, testTimeout);

        String resultMessage = testdata.getSingleMessage();
        testdata.reThrowException(); // throw an exception if there was one

        if ((resultMessage.compareTo(successForTestCdiIntject)) != 0) {
            Assert.fail("test failed with following output: " + resultMessage);
        }

    }

    /*
     * ServerEndpoint - @see AnnotatedTxEndpointFieldInjection
     */
    public void testCdiTxNeverInjectCDI12() throws Exception {

        int testTimeout = Constants.getLongTimeout();
        String writeData = "From testCdiInject";
        WsocTestContext testdata = wsocTest.runWsocTest(new SimpleClientEP.WriteAndRead(writeData),
                                                        "/cditx/EPTxNeverFieldInjectionCDI12", WsocTestRunner.getDefaultConfig(), 1, testTimeout);

        String resultMessage = testdata.getSingleMessage();
        testdata.reThrowException(); // throw an exception if there was one

        if ((resultMessage.compareTo(successForTestCdiIntject)) != 0) {
            Assert.fail("test failed with following output: " + resultMessage);
        }

    }

    public void testCdiTxRequiredInjectCDI12() throws Exception {

        int testTimeout = Constants.getLongTimeout();
        String writeData = "From testCdiInject";
        WsocTestContext testdata = wsocTest.runWsocTest(new SimpleClientEP.WriteAndRead(writeData),
                                                        "/cditx/EPTxRequiredFieldInjectionCDI12", WsocTestRunner.getDefaultConfig(), 2, testTimeout);

        String resultMessage = testdata.getSingleMessage();
        testdata.reThrowException(); // throw an exception if there was one

        if ((resultMessage.compareTo(successForTestCdiIntject)) != 0) {
            Assert.fail("test failed with following output: " + resultMessage);
        }

    }

    public void testCdiTxMandatoryInjectCDI12() throws Exception {

        int testTimeout = Constants.getLongTimeout();
        String writeData = "From testCdiInject";
        WsocTestContext testdata = wsocTest.runWsocTest(new SimpleClientEP.WriteAndRead(writeData),
                                                        "/cditx/EPTxMandatoryFieldInjectionCDI12", WsocTestRunner.getDefaultConfig(), 2, testTimeout);

        String resultMessage = testdata.getSingleMessage();
        testdata.reThrowException(); // throw an exception if there was one

        if ((resultMessage.compareTo(successForTestCdiIntject)) != 0) {
            Assert.fail("test failed with following output: " + resultMessage);
        }

    }

    public void testCdiTxNotSupportedInjectCDI12() throws Exception {

        int testTimeout = Constants.getLongTimeout();
        String writeData = "From testCdiInject";
        WsocTestContext testdata = wsocTest.runWsocTest(new SimpleClientEP.WriteAndRead(writeData),
                                                        "/cditx/EPTxNotSupportedFieldInjectionCDI12", WsocTestRunner.getDefaultConfig(), 2, testTimeout);

        String resultMessage = testdata.getSingleMessage();
        testdata.reThrowException(); // throw an exception if there was one

        if ((resultMessage.compareTo(successForTestCdiIntject)) != 0) {
            Assert.fail("test failed with following output: " + resultMessage);
        }

    }

    public void testCdiTxRequiresNewInjectCDI12() throws Exception {

        int testTimeout = Constants.getLongTimeout();
        String writeData = "From testCdiInject";
        WsocTestContext testdata = wsocTest.runWsocTest(new SimpleClientEP.WriteAndRead(writeData),
                                                        "/cditx/EPTxRequiresNewFieldInjectionCDI12", WsocTestRunner.getDefaultConfig(), 2, testTimeout);

        String resultMessage = testdata.getSingleMessage();
        testdata.reThrowException(); // throw an exception if there was one

        if ((resultMessage.compareTo(successForTestCdiIntject)) != 0) {
            Assert.fail("test failed with following output: " + resultMessage);
        }

    }

    public void testCdiTxSupportsInjectCDI12() throws Exception {

        int testTimeout = Constants.getLongTimeout();
        String writeData = "From testCdiInject";
        WsocTestContext testdata = wsocTest.runWsocTest(new SimpleClientEP.WriteAndRead(writeData),
                                                        "/cditx/EPTxSupportsFieldInjectionCDI12", WsocTestRunner.getDefaultConfig(), 2, testTimeout);

        String resultMessage = testdata.getSingleMessage();
        testdata.reThrowException(); // throw an exception if there was one

        if ((resultMessage.compareTo(successForTestCdiIntject)) != 0) {
            Assert.fail("test failed with following output: " + resultMessage);
        }

    }

    /*
     * ServerEndpoint - @see ProgrammaticExtendEndpointCDI
     */
    public void testCdiProgrammaticEndpoint() throws Exception {

        String s1 = "Message1FromClient";
        String e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 3 SessionScopedCounter: 4";

        // Since test order is not a given, and since application scope test spans this test and the next one, we need to see if we are first or not and set
        // expectations accordingly
        if (sequenceCounter == 1) {
            e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 27 SessionScopedCounter: 4";
        }
        sequenceCounter++;

        String[] textValues = { s1 };
        String[] expected = { e1 };

        wsocTest.runEchoTest(new ProgrammaticClientEP.TextTest(textValues), "/cdi/ProgrammaticExtendEndpointCDI", expected, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see ProgrammaticExtendEndpointCDI
     */
    public void testCdiProgrammaticEndpointMultipleOnMessage() throws Exception {

        String s1 = "Message1FromClient";
        String s2 = "Message2FromClient";
        String s3 = "Message3FromClient";
        String s4 = "Message4FromClient";
        String s5 = "Message5FromClient";
        String s6 = "Message6FromClient";
        String s7 = "Message7FromClient";
        String s8 = "Message8FromClient";

        String e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 3 SessionScopedCounter: 4";
        String e2 = "Dependent Scoped Counter: 4 ApplicationScopedCounter: 6 SessionScopedCounter: 8";
        String e3 = "Dependent Scoped Counter: 6 ApplicationScopedCounter: 9 SessionScopedCounter: 12";
        String e4 = "Dependent Scoped Counter: 8 ApplicationScopedCounter: 12 SessionScopedCounter: 16";
        String e5 = "Dependent Scoped Counter: 10 ApplicationScopedCounter: 15 SessionScopedCounter: 20";
        String e6 = "Dependent Scoped Counter: 12 ApplicationScopedCounter: 18 SessionScopedCounter: 24";
        String e7 = "Dependent Scoped Counter: 14 ApplicationScopedCounter: 21 SessionScopedCounter: 28";
        String e8 = "Dependent Scoped Counter: 16 ApplicationScopedCounter: 24 SessionScopedCounter: 32";

        // Since test order is not a given, and since application scope test spans this test and the next one, we need to see if we are first or not and set
        // expectations accordingly
        if (sequenceCounter == 1) {
            // don't you just love writing test code, no need for time wasting elegance.
            e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 6 SessionScopedCounter: 4";
            e2 = "Dependent Scoped Counter: 4 ApplicationScopedCounter: 9 SessionScopedCounter: 8";
            e3 = "Dependent Scoped Counter: 6 ApplicationScopedCounter: 12 SessionScopedCounter: 12";
            e4 = "Dependent Scoped Counter: 8 ApplicationScopedCounter: 15 SessionScopedCounter: 16";
            e5 = "Dependent Scoped Counter: 10 ApplicationScopedCounter: 18 SessionScopedCounter: 20";
            e6 = "Dependent Scoped Counter: 12 ApplicationScopedCounter: 21 SessionScopedCounter: 24";
            e7 = "Dependent Scoped Counter: 14 ApplicationScopedCounter: 24 SessionScopedCounter: 28";
            e8 = "Dependent Scoped Counter: 16 ApplicationScopedCounter: 27 SessionScopedCounter: 32";
        }
        sequenceCounter++;

        String[] textValues = { s1, s2, s3, s4, s5, s6, s7, s8 };
        String[] expected = { e1, e2, e3, e4, e5, e6, e7, e8 };

        wsocTest.runEchoTest(new ProgrammaticClientEP.TextTest(textValues), "/cdi/ProgrammaticExtendEndpointCDI", expected, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see ProgrammaticExtendEndpointCDI12
     */
    public void testCdiProgrammaticEndpointCDI12() throws Exception {

        String s1 = "Message1FromClient";
        String e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 3";

        // Since test order is not a given, and since application scope test spans this test and the next one, we need to see if we are first or not and set
        // expectations accordingly
        if (sequenceCounter12 == 1) {
            e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 27";
        }
        sequenceCounter12++;

        String[] textValues = { s1 };
        String[] expected = { e1 };

        wsocTest.runEchoTest(new ProgrammaticClientEP.TextTest(textValues), "/cdi/ProgrammaticExtendEndpointCDI12", expected, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see ProgrammaticExtendEndpointCDI12
     */
    public void testCdiProgrammaticEndpointMultipleOnMessageCDI12() throws Exception {

        String s1 = "Message1FromClient";
        String s2 = "Message2FromClient";
        String s3 = "Message3FromClient";
        String s4 = "Message4FromClient";
        String s5 = "Message5FromClient";
        String s6 = "Message6FromClient";
        String s7 = "Message7FromClient";
        String s8 = "Message8FromClient";

        String e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 3";
        String e2 = "Dependent Scoped Counter: 4 ApplicationScopedCounter: 6";
        String e3 = "Dependent Scoped Counter: 6 ApplicationScopedCounter: 9";
        String e4 = "Dependent Scoped Counter: 8 ApplicationScopedCounter: 12";
        String e5 = "Dependent Scoped Counter: 10 ApplicationScopedCounter: 15";
        String e6 = "Dependent Scoped Counter: 12 ApplicationScopedCounter: 18";
        String e7 = "Dependent Scoped Counter: 14 ApplicationScopedCounter: 21";
        String e8 = "Dependent Scoped Counter: 16 ApplicationScopedCounter: 24";

        // Since test order is not a given, and since application scope test spans this test and the next one, we need to see if we are first or not and set
        // expectations accordingly
        if (sequenceCounter12 == 1) {
            // don't you just love writing test code, no need for time wasting elegance.
            e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 6";
            e2 = "Dependent Scoped Counter: 4 ApplicationScopedCounter: 9";
            e3 = "Dependent Scoped Counter: 6 ApplicationScopedCounter: 12";
            e4 = "Dependent Scoped Counter: 8 ApplicationScopedCounter: 15";
            e5 = "Dependent Scoped Counter: 10 ApplicationScopedCounter: 18";
            e6 = "Dependent Scoped Counter: 12 ApplicationScopedCounter: 21";
            e7 = "Dependent Scoped Counter: 14 ApplicationScopedCounter: 24";
            e8 = "Dependent Scoped Counter: 16 ApplicationScopedCounter: 27";
        }
        sequenceCounter12++;

        String[] textValues = { s1, s2, s3, s4, s5, s6, s7, s8 };
        String[] expected = { e1, e2, e3, e4, e5, e6, e7, e8 };

        wsocTest.runEchoTest(new ProgrammaticClientEP.TextTest(textValues), "/cdi/ProgrammaticExtendEndpointCDI12", expected, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see ProgrammaticExtendEndpointCDI20
     */
    public void testCdiProgrammaticEndpointCDI20() throws Exception {

        String s1 = "Message1FromClient";
        String e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 3";

        // Since test order is not a given, and since application scope test spans this test and the next one, we need to see if we are first or not and set
        // expectations accordingly
        if (sequenceCounter20 == 1) {
            e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 27";
        }
        sequenceCounter20++;

        String[] textValues = { s1 };
        String[] expected = { e1 };

        wsocTest.runEchoTest(new ProgrammaticClientEP.TextTest(textValues), "/cdi/ProgrammaticExtendEndpointCDI20", expected, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see ProgrammaticExtendEndpointCDI20
     */
    public void testCdiProgrammaticEndpointMultipleOnMessageCDI20() throws Exception {

        String s1 = "Message1FromClient";
        String s2 = "Message2FromClient";
        String s3 = "Message3FromClient";
        String s4 = "Message4FromClient";
        String s5 = "Message5FromClient";
        String s6 = "Message6FromClient";
        String s7 = "Message7FromClient";
        String s8 = "Message8FromClient";

        String e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 3";
        String e2 = "Dependent Scoped Counter: 4 ApplicationScopedCounter: 6";
        String e3 = "Dependent Scoped Counter: 6 ApplicationScopedCounter: 9";
        String e4 = "Dependent Scoped Counter: 8 ApplicationScopedCounter: 12";
        String e5 = "Dependent Scoped Counter: 10 ApplicationScopedCounter: 15";
        String e6 = "Dependent Scoped Counter: 12 ApplicationScopedCounter: 18";
        String e7 = "Dependent Scoped Counter: 14 ApplicationScopedCounter: 21";
        String e8 = "Dependent Scoped Counter: 16 ApplicationScopedCounter: 24";

        // Since test order is not a given, and since application scope test spans this test and the next one, we need to see if we are first or not and set
        // expectations accordingly
        if (sequenceCounter20 == 1) {
            // don't you just love writing test code, no need for time wasting elegance.
            e1 = "Dependent Scoped Counter: 2 ApplicationScopedCounter: 6";
            e2 = "Dependent Scoped Counter: 4 ApplicationScopedCounter: 9";
            e3 = "Dependent Scoped Counter: 6 ApplicationScopedCounter: 12";
            e4 = "Dependent Scoped Counter: 8 ApplicationScopedCounter: 15";
            e5 = "Dependent Scoped Counter: 10 ApplicationScopedCounter: 18";
            e6 = "Dependent Scoped Counter: 12 ApplicationScopedCounter: 21";
            e7 = "Dependent Scoped Counter: 14 ApplicationScopedCounter: 24";
            e8 = "Dependent Scoped Counter: 16 ApplicationScopedCounter: 27";
        }
        sequenceCounter20++;

        String[] textValues = { s1, s2, s3, s4, s5, s6, s7, s8 };
        String[] expected = { e1, e2, e3, e4, e5, e6, e7, e8 };

        wsocTest.runEchoTest(new ProgrammaticClientEP.TextTest(textValues), "/cdi/ProgrammaticExtendEndpointCDI20", expected, Constants.getLongTimeout());

    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP - SessionIdleTimeoutEndpoint
     */
    public void testCdiInjectWithIdleTimeout() throws Exception {

        Object[] result = { ":ProducerBean Field Injected1", 1006 };
        long startTime = System.currentTimeMillis();
        wsocTest.runEchoTest(new AnnotatedClientEP.SessionIdleTest(), "/cdi/annotatedIdleTimeoutCDI", result, Constants.getLongTimeout());
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        if ((totalTime < 15000) || (totalTime > 40000)) {
            Assert.fail("Test - phase 1 - ran for " + totalTime
                        + " which is either less than 10 seconds or more than 25 seconds,  outside of an acceptable session timeout range.");
        }

        Object[] result2 = { ":ProducerBean Field Injected1:OnClose Called:ProducerBean Field Injected2", 1006 };
        startTime = System.currentTimeMillis();
        wsocTest.runEchoTest(new AnnotatedClientEP.SessionIdleTest(), "/cdi/annotatedIdleTimeoutCDI", result2, Constants.getLongTimeout());
        endTime = System.currentTimeMillis();
        totalTime = endTime - startTime;
        if ((totalTime < 3000) || (totalTime > 20000)) {
            Assert.fail("Test - phase 2 - ran for " + totalTime + " which is either less than 3 seconds or more than 10 seconds,  outside of an acceptable session timeout range.");
        }

    }

    /*
     * MSN RENABLE
     * public void testClientCDIOne() throws Exception {
     * this.verifyResponse("testClientCDIOne");
     * }
     *
     * @Test
     * public void testClientCDITwo() throws Exception {
     * this.verifyResponse("testClientCDITwo");
     * }
     *
     * protected WebResponse verifyResponse(String testName) throws Exception {
     * return this.verifyResponse(createWebBrowserForTestCase(), "/cdi/RequestCDI?testname=" + testName, "SuccessfulTest");
     * }
     */

}
