/* ============================================================================
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 * ============================================================================
 */
package com.ibm.ws.messaging.open_comms.fat;

import static org.junit.Assert.assertNotNull;

import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.impl.LibertyServer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.annotation.AllowedFFDC;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.AfterClass;
import org.junit.BeforeClass;

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class FeatureUpdate extends FATBase {
  static {
    server_ = LibertyServerFactory.getLibertyServer("com.ibm.ws.messaging.open_comms.server");
    client_ = LibertyServerFactory.getLibertyServer("com.ibm.ws.messaging.open_comms.client");
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    Util.TRACE_ENTRY();
    setup();
    Util.TRACE_EXIT();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    Util.TRACE_ENTRY();
    cleanup();
    Util.TRACE_EXIT();
  }

  @Test
  public void testSendReceive2LP() throws Exception {
    Util.TRACE_ENTRY();
    setMarkMakeConfigUpdate(client_, "client.xml");
    setMarkMakeConfigUpdate(server_, "server.xml");

    runTest(client_,"CommsLP","testQueueSendMessage");
    runTest(client_,"CommsLP","testQueueReceiveMessages");

    String msg = client_.waitForStringInLog("Queue Message", client_.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the queue message in the message log", msg);
    Util.TRACE_EXIT();
  }

  @AllowedFFDC({"com.ibm.websphere.sib.exception.SIResourceException"
               ,"com.ibm.ws.sib.jfapchannel.JFapConnectionBrokenException"
               ,"com.ibm.ws.sib.jfapchannel.JFapHeartbeatTimeoutException"
               ,"javax.resource.ResourceException"
               ,"javax.resource.spi.ResourceAllocationException"
               })
  @Test
  public void testwasJmsSecurityFeatureUpdate() throws Exception {
    Util.TRACE_ENTRY();
    setMarkMakeConfigUpdate(client_, "client.xml");
    setMarkMakeConfigUpdate(server_, "SecurityDisabledServer.xml");
    Util.TRACE("marker set , running test in servelet");
    runTest(client_,"CommsLP","testQueueSendMessageExpectException");

    String message = client_.waitForStringInLogUsingMark("SIResourceException was correctly thrown"
                                                       ,client_.getMatchingLogFile("messages.log")
                                                       );
    assertNotNull("Could not find expected exception in the messages.log", message);

    Util.TRACE("stop server");
    server_.stopServer();
    Util.TRACE("changing server.xml to SecurityEnabledServer");
    server_.setServerConfigurationFile("SecurityEnabledServer.xml");
    Util.TRACE("starting server ");
    server_.startServer();
    Util.TRACE_EXIT();
  }

  @AllowedFFDC({"com.ibm.websphere.sib.exception.SIResourceException"
               ,"com.ibm.ws.sib.jfapchannel.JFapConnectionBrokenException"
               ,"com.ibm.ws.sib.jfapchannel.JFapHeartbeatTimeoutException"
               ,"javax.resource.ResourceException"
               ,"javax.resource.spi.ResourceAllocationException"
               ,"com.ibm.wsspi.channelfw.exception.InvalidChainNameException"
               })
  @Test
  public void testSSLFeatureUpdate() throws Exception {
    Util.TRACE_ENTRY();
    // When the client AppServer stops it might observe FFDCs left behind by prior tests, hence the allowed FFDCs.
    client_.stopServer();
    client_.setServerConfigurationFile("SecurityDisabledClient.xml");
    client_.startServer();

    Util.TRACE("server started again");

    String message = client_.waitForStringInLog("CWWKF0011I:.*", client_.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the server start info message in the message log", message);

    Util.TRACE("running test");
    runTest(client_,"CommsLP","testQueueSendMessageExpectException");

    message = client_.waitForStringInLogUsingMark("SIResourceException was correctly thrown"
                                                ,client_.getMatchingLogFile("messages.log")
                                                );
    assertNotNull("Could not find expected exception in the messages.log", message);
    Util.TRACE_EXIT();
  }

  static void setMarkMakeConfigUpdate(LibertyServer serv, String newServerXml) throws Exception {
    Util.TRACE_ENTRY();
    // set a new mark
    serv.setMarkToEndOfLog(server_.getDefaultLogFile());

    // make the configuration update
    serv.setServerConfigurationFile(newServerXml);

    // wait for configuration update to complete
    serv.waitForStringInLogUsingMark("CWWKG001[78]I");
    Util.TRACE_EXIT();
  }
}
