/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
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

import io.openliberty.wsoc.util.wsoc.MultiClientTestContext;
import io.openliberty.wsoc.util.wsoc.PublishTask;
import io.openliberty.wsoc.util.wsoc.TestHelper;
import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.util.wsoc.WsocTestRunner;
import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.common.Utils;
import io.openliberty.wsoc.endpoints.client.basic.AnnotatedClientEP;
import io.openliberty.wsoc.endpoints.client.basic.PathParamClientEP;
import io.openliberty.wsoc.endpoints.client.trace.AnnotatedConfiguratorClientEP;
import io.openliberty.wsoc.endpoints.client.trace.ProgrammaticClientEP; // this should be .trace.
import io.openliberty.wsoc.endpoints.client.trace.ProgrammaticConfiguratorClientEP;

/**
 * Tests WebSocket Stuff
 *
 * @author unknown
 */
public class TraceEnabledTest {

    public static int DEFAULT_TIMEOUT = Constants.getDefaultTimeout();
    private WsocTest wsocTest = null;
    private static final Logger LOG = Logger.getLogger(TraceEnabledTest.class.getName());

    public TraceEnabledTest(WsocTest test) {
        this.wsocTest = test;
    }

    protected void runMultiTest(Object[] receiveEdps, Object publishEdp, String resource, Object[] data) throws Exception {

        MultiClientTestContext mctr = wsocTest.runMultiClientWsocTest(receiveEdps, publishEdp, null, resource, WsocTestRunner.getDefaultConfig(),
                                                                      data.length, Constants.getDefaultTimeout(), Constants.getDefaultTimeout(), false);

        for (WsocTestContext wtc : mctr.getReceivers()) {
            wtc.reThrowException();
            LOG.log(Level.INFO, "Actual message array", wtc.getMessage().toArray());
            Assert.assertArrayEquals(data, wtc.getMessage().toArray());
        }
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

    public void RuntimeExceptionTCKTest() throws Exception {
        String path = "/trace/runtimeexceptionTCK/1";

        String[] input = { "msg1" };
        String[] output = { "1" };

        runEchoTest(new PathParamClientEP.TextTest(input), path, input, output);

        Utils.waitForFFDCToBeGenerated(Constants.longFFDCWait);
    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP - CloseEndpoint
     */
    public void testProgrammaticCloseSuccess() throws Exception {

        String[] textValues = { "1007:THIS IS A TEST CLOSING STATUS" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.CloseTest(textValues), "/trace/codedClose", textValues);

    }

    /*
     * ServerEndpoint - @see ProgrammaticServerEP -CloseEndpointOnOpen
     */
    public void testProgrammaticCloseSuccessOnOpen() throws Exception {

        String[] textValues = { "1001:THIS IS A TEST CLOSING STATUS FROM onOPEN" };
        wsocTest.runEchoTest(new ProgrammaticClientEP.CloseTestOnOpen(textValues), "/trace/codedCloseOnOpen", textValues);

    }

    /*
     * ServerEndpoint - @see AnnotatedServerEP -TextTest
     */
    public void testAsyncAnnotatedTextSuccess() throws Exception {

        String[] textValues = { "WEREWR", "ERERE" };
        wsocTest.runEchoTest(new AnnotatedClientEP.AsyncTextTest(textValues), "/trace/annotatedAsyncText", textValues);

    }

    /*
     * ServerEndpoint - @see AnnotatedConfiguratorServerEP - ConfiguratorTest
     */
    public void testConfiguratorSuccess() throws Exception {

        // Annotated
        WsocTestContext testdata = wsocTest.runWsocTest(new AnnotatedConfiguratorClientEP.ConfiguratorTest(),
                                                        "/trace/annotatedModifyHandshake", WsocTestRunner.getDefaultConfig(), 1, DEFAULT_TIMEOUT);

        testdata.reThrowException(); // throw an exception if there was one
        if (!testdata.getSingleMessage().equals("SUCCESS")) {
            Assert.fail("Configurator failed to modify request and add needed header. msg: " + testdata.getSingleMessage());
        }
        //Programmatic
        testdata = wsocTest.runWsocTest(new ProgrammaticConfiguratorClientEP.ConfiguratorTest(), "/trace/extendedModifyHandshake",
                                        WsocTestRunner.getConfig(new ProgrammaticConfiguratorClientEP.ClientConfigurator()),
                                        1, DEFAULT_TIMEOUT);
        testdata.reThrowException(); // throw an exception if there was one
        if (!testdata.getSingleMessage().equals("SUCCESS")) {
            Assert.fail("Configurator failed to modify request and add needed header. msg: " + testdata.getSingleMessage());
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
            receivers[x] = new io.openliberty.wsoc.endpoints.client.trace.MultiClientEP.SimplePublisherTest(textValues);
        }

        runMultiTest(receivers, null, "/trace/multiText", textValues);
    }

    /*
     * ServerEndpoint - @see SinglePublisherMultiClientServerEP
     */
    public void testSinglePublisherMultipleReciverTextSuccess() throws Exception {

        final String[] textValues = { "WEREWR", "ERERE", "ERWEREW", "ADSFSDFDS", "WERWEREWR", "33423423423432" };

        int numClients = Constants.getClientsCount();

        Object[] receivers = new TestHelper[numClients];
        for (int x = 0; x < numClients; x++) {
            receivers[x] = new io.openliberty.wsoc.endpoints.client.trace.MultiClientEP.SimpleReceiverTest();
        }

        Object publisher = new io.openliberty.wsoc.endpoints.client.trace.MultiClientEP.NoPublishNoReceiveTest();

        MultiClientTestContext mctr = wsocTest.runMultiClientWsocTest(receivers, publisher,
                                                                      new PublishTask() {

                                                                          @Override
                                                                          public void run() {
                                                                              for (int x = 0; x < textValues.length; x++) {
                                                                                  try {
                                                                                      getMultiTestContext().getPublisher().getSession().getBasicRemote().sendText(textValues[x]);
                                                                                      //java.lang.Thread.sleep(25);
                                                                                  } catch (Exception e) {
                                                                                      e.printStackTrace();
                                                                                  }
                                                                              }
                                                                              WsocTestContext.completeLatch.countDown();
                                                                          }
                                                                      },
                                                                      "/trace/singlePubMultiReceive",
                                                                      WsocTestRunner.getDefaultConfig(),
                                                                      textValues.length,
                                                                      Constants.getLongTimeout(),
                                                                      Constants.getDefaultTimeout(),
                                                                      true);
        for (WsocTestContext wtc : mctr.getReceivers()) {
            wtc.reThrowException();
            Assert.assertArrayEquals(textValues, wtc.getMessage().toArray());
        }
    }
}