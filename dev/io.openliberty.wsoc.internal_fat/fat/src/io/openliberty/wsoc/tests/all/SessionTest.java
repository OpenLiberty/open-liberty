/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import junit.framework.Assert;

import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.util.wsoc.WsocTestRunner;
import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.Utils;
import io.openliberty.wsoc.endpoints.client.context.Session5ClientEP;
import io.openliberty.wsoc.endpoints.client.context.SimpleClientEP;

/**
 *
 */
public class SessionTest {

    private WsocTest wsocTest = null;

    public SessionTest(WsocTest test) {
        this.wsocTest = test;
    }

    /*
     * ServerEndpoint - @see SessionServerEndpoint
     */
    public void testSessionOne() throws Exception {

        int testTimeout = Constants.getLongTimeout();
        String writeData = "From testSessionOne";
        WsocTestContext testdata = wsocTest.runWsocTest(new io.openliberty.wsoc.endpoints.client.context.SimpleClientEP.WriteAndRead(writeData),
                                                        "/context/sessionEndpoint", WsocTestRunner.getDefaultConfig(), 1, testTimeout);

        String resultMessage = testdata.getSingleMessage();
        testdata.reThrowException(); // throw an exception if there was one

        if ((resultMessage.indexOf(Constants.SUCCESS) == -1)
            || (resultMessage.indexOf(Constants.FAILED) != -1)) {
            Assert.fail("test failed with following output: " + resultMessage);
        }

    }

    /*
     * ServerEndpoint - @see SessionMessageHandlerError
     */
    public void testMessageHandlerError() throws Exception {

        int testTimeout = Constants.getLongTimeout();
        String writeData = "From testMessageHandlerError";
        WsocTestContext testdata = wsocTest.runWsocTest(new SimpleClientEP.WriteAndRead(writeData),
                                                        "/context/sessionHandlerErrorEndpoint", WsocTestRunner.getDefaultConfig(), 1, testTimeout);

        String resultMessage = testdata.getSingleMessage();
        testdata.reThrowException(); // throw an exception if there was one

        if ((resultMessage.indexOf(Constants.SUCCESS) == -1)
            || (resultMessage.indexOf(Constants.FAILED) != -1)) {
            Assert.fail("test failed with following output: " + resultMessage);
        }

    }

    /*
     * ServerEndpoint - @see Session5ServerEndpoint
     */
    public void testSession5() throws Exception {

        int testTimeout = 60000*2;

        Session5ClientEP[] endpoints = new Session5ClientEP[5];
        for (int x = 0; x <= 4; x++) {
            endpoints[x] = new Session5ClientEP(x);
        }

        WsocTestContext[] testdata = wsocTest.runSession5WsocTest(endpoints, "/context/session5Endpoint",
                                                                  WsocTestRunner.getDefaultConfig(), testTimeout, 1);

        for (WsocTestContext wtc : testdata) {
            wtc.reThrowException();
            String resultMessage = wtc.getSingleMessage();

            if ((resultMessage.indexOf(Constants.SUCCESS) == -1)
                || (resultMessage.indexOf(Constants.FAILED) != -1)) {
                Assert.fail("test failed with following output: " + resultMessage);
            }
        }

    }

    /*
     * ServerEndpoint - @see SessionCloseServerEndpoint
     * 
     * @ExpectedFFDC({ "java.lang.reflect.InvocationTargetException" })
     */
    public void testSessionClose() throws Exception {

        int testTimeout = Constants.getLongTimeout();;

        Session5ClientEP[] endpoints = new Session5ClientEP[2];
        for (int x = 0; x <= 1; x++) {
            endpoints[x] = new Session5ClientEP(x);
        }

        WsocTestContext[] testdata = wsocTest.runSession5WsocTest(endpoints, "/context/sessionCloseEndpoint",
                                                                  WsocTestRunner.getDefaultConfig(), testTimeout, 2);

        for (WsocTestContext wtc : testdata) {
            wtc.reThrowException();
            String resultMessage = wtc.getSingleMessage();

            if ((resultMessage.indexOf(Constants.SUCCESS) == -1)
                || (resultMessage.indexOf(Constants.FAILED) != -1)) {
                Assert.fail("test failed with following output: " + resultMessage);
            }
        }
        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }

    /*
     * ServerEndpoint - @see ContextServerEndpoint
     * 
     * @ExpectedFFDC({ "java.lang.reflect.InvocationTargetException" })
     */
    public void testThreadContext() throws Exception {

        int testTimeout = Constants.getLongTimeout();;

        Session5ClientEP[] endpoints = new Session5ClientEP[2];
        for (int x = 0; x <= 1; x++) {
            endpoints[x] = new Session5ClientEP(x);
        }

        WsocTestContext[] testdata = wsocTest.runSession5WsocTest(endpoints, "/context/contextEndpoint",
                                                                  WsocTestRunner.getDefaultConfig(), testTimeout, 2);

        for (WsocTestContext wtc : testdata) {
            wtc.reThrowException();
            String resultMessage = wtc.getSingleMessage();

            if ((resultMessage.indexOf(Constants.SUCCESS) == -1)
                || (resultMessage.indexOf(Constants.FAILED) != -1)) {
                Assert.fail("test failed with following output: " + resultMessage);
            }
        }
        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);

    }

}
