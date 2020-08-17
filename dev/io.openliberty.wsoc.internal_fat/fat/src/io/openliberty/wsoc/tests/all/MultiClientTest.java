/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
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

import io.openliberty.wsoc.util.wsoc.MultiClientTestContext;
import io.openliberty.wsoc.util.wsoc.PublishTask;
import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.util.wsoc.WsocTestRunner;
import io.openliberty.wsoc.common.Constants;

/**
 * Tests WebSocket Stuff
 * 
 * @author unknown
 */
public class MultiClientTest {

    private WsocTest wsocTest = null;

    public MultiClientTest(WsocTest test) {
        this.wsocTest = test;
    }

    protected void runMultiTest(Object[] receiveEdps, Object publishEdp, String resource, Object[] data) throws Exception {

        MultiClientTestContext mctr = wsocTest.runMultiClientWsocTest(receiveEdps, publishEdp, null, resource, WsocTestRunner.getDefaultConfig(),
                                                                      data.length, Constants.getDefaultTimeout(), Constants.getDefaultTimeout(), false);

        for (WsocTestContext wtc : mctr.getReceivers()) {
            wtc.reThrowException();
            Assert.assertArrayEquals(data, wtc.getMessage().toArray());
        }
    }

    /*
     * ServerEndpoint - @see MultiServerEP
     */
    public void testMultipleClientsPublishingandReceivingToThemselvesTextSuccess() throws Exception {

        String[] textValues = { "WEREWR", "ERERE", "ERWEREW", "ADSFSDFDS", "WERWEREWR", "33423423423432" };
        int numClients = 100;
        Object[] receivers = new TestHelper[numClients];
        for (int x = 0; x < numClients; x++) {
            receivers[x] = new io.openliberty.wsoc.endpoints.client.basic.MultiClientEP.SimplePublisherTest(textValues);
        }

        runMultiTest(receivers, null, "/basic/multiText", textValues);
    }

    /*
     * ServerEndpoint - @see SinglePublisherMultiClientServerEP
     */
    public void testSinglePublisherMultipleReciverTextSuccess() throws Exception {

        final String[] textValues = { "WEREWR", "ERERE", "ERWEREW", "ADSFSDFDS", "WERWEREWR", "33423423423432" };

        int numClients = Constants.getClientsCount();

        Object[] receivers = new TestHelper[numClients];
        for (int x = 0; x < numClients; x++) {
            receivers[x] = new io.openliberty.wsoc.endpoints.client.basic.MultiClientEP.SimpleReceiverTest();
        }

        Object publisher = new io.openliberty.wsoc.endpoints.client.basic.MultiClientEP.NoPublishNoReceiveTest();

        MultiClientTestContext mctr =
                        wsocTest.runMultiClientWsocTest(receivers, publisher,
                                                        new PublishTask() {

                                                            @Override
                                                            public void run() {
                                                                for (int x = 0; x < textValues.length; x++) {
                                                                    try {
                                                                        getMultiTestContext()
                                                                                        .getPublisher()
                                                                                        .getSession()
                                                                                        .getBasicRemote()
                                                                                        .sendText(textValues[x]);
                                                                        //java.lang.Thread.sleep(25);
                                                                    } catch (Exception e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                }
                                                                WsocTestContext.completeLatch.countDown();
                                                            }
                                                        },
                                                        "/basic/singlePubMultiReceive",
                                                        WsocTestRunner.getDefaultConfig(),
                                                        textValues.length,
                                                        Constants.getDefaultTimeout(),
                                                        Constants.getDefaultTimeout(),
                                                        true);
        for (WsocTestContext wtc : mctr.getReceivers()) {
            wtc.reThrowException();
            Assert.assertArrayEquals(textValues, wtc.getMessage().toArray());
        }
    }
}