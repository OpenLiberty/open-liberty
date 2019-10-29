/* ============================================================================
 * Copyright (c) 2019 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.annotation.AllowedFFDC;
import componenttest.topology.utils.FATServletClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class FeatureUpdate extends FATServletClient {

  public static final MessagingFATUtilities util = new MessagingFATUtilities(FeatureUpdate.class);
  public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.messaging.open_comms.server");
  public static LibertyServer client = LibertyServerFactory.getLibertyServer("com.ibm.ws.messaging.open_comms.client");
  static {
    util.checkAndSetDebug(client);
  }

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    util.ENTRY();

    server.startServer();

    String message = server.waitForStringInLog("CWWKF0011I:.*", server.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the server start info message in the message log", message);

    message = server.waitForStringInLog("CWWKO0219I:.*InboundJmsCommsEndpoint.*", server.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the SSL port ready message in the message log", message);

    message = server.waitForStringInLog("CWWKO0219I:.*InboundJmsCommsEndpoint-ssl.*", server.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the SSL port ready message in the message log", message);

    client.startServer();

    message = client.waitForStringInLog("CWWKF0011I:.*", client.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the server start info message in the message log", message);

    ShrinkHelper.defaultDropinApp(client, "CommsLP", "web");
    util.EXIT();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    util.ENTRY();
    Exception e = null;
    try {
      client.stopServer();
    } catch (Exception ce) {
      util.TRACE("Exception caught whilst stopping client Liberty server.");
      e = ce;
    }
    try {
      server.stopServer();
    } catch (Exception se) {
      util.TRACE("Exception caught whilst stopping server Liberty server.");
      if (null==e) e = se;
    }
    if (null!=e) {
      util.THROW_EXIT(e);
    }
    util.EXIT();
  }

  @Test
  public void testSendReceive2LP() throws Exception {
    util.ENTRY();
    setMarkMakeConfigUpdate(client, "client.xml");
    setMarkMakeConfigUpdate(server, "server.xml");

    runTest(client,"CommsLP","testQueueSendMessage");
    runTest(client,"CommsLP","testQueueReceiveMessages");

    String msg = client.waitForStringInLog("Queue Message", client.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the queue message in the message log", msg);
    util.EXIT();
  }

  @AllowedFFDC({"com.ibm.websphere.sib.exception.SIResourceException"
               ,"com.ibm.ws.sib.jfapchannel.JFapConnectionBrokenException"
               ,"javax.resource.ResourceException"
               ,"javax.resource.spi.ResourceAllocationException"
               })
  @Test
  public void testwasJmsSecurityFeatureUpdate() throws Exception {
    util.ENTRY();
    setMarkMakeConfigUpdate(client, "client.xml");
    setMarkMakeConfigUpdate(server, "SecurityDisabledServer.xml");
    util.TRACE("marker set , running test in servelet");
    runTest(client,"CommsLP","testQueueSendMessageExpectException");

    String message = client.waitForStringInLogUsingMark("SIResourceException was correctly thrown"
                                                       ,client.getMatchingLogFile("messages.log")
                                                       );
    assertNotNull("Could not find expected exception in the messages.log", message);

    util.TRACE("stop server");
    server.stopServer();
    util.TRACE("changing server.xml to SecurityEnabledServer");
    server.setServerConfigurationFile("SecurityEnabledServer.xml");
    util.TRACE("starting server ");
    server.startServer();
    util.EXIT();
  }

  @AllowedFFDC({"com.ibm.websphere.sib.exception.SIResourceException"
               ,"com.ibm.ws.sib.jfapchannel.JFapConnectionBrokenException"
               ,"javax.resource.ResourceException"
               ,"javax.resource.spi.ResourceAllocationException"
               ,"com.ibm.wsspi.channelfw.exception.InvalidChainNameException"
               })
  @Test
  public void testSSLFeatureUpdate() throws Exception {
    util.ENTRY();
    client.stopServer();
    client.setServerConfigurationFile("SecurityDisabledClient.xml");
    client.startServer();

    util.TRACE("server started again");

    String message = client.waitForStringInLog("CWWKF0011I:.*", client.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the server start info message in the message log", message);

    util.TRACE("running test");
    runTest(client,"CommsLP","testQueueSendMessageExpectException");

    message = client.waitForStringInLogUsingMark("SIResourceException was correctly thrown"
                                                ,client.getMatchingLogFile("messages.log")
                                                );
    assertNotNull("Could not find expected exception in the messages.log", message);
    util.EXIT();
  }

  static void setMarkMakeConfigUpdate(LibertyServer serv, String newServerXml) throws Exception {
    util.ENTRY();
    // set a new mark
    serv.setMarkToEndOfLog(server.getDefaultLogFile());

    // make the configuration update
    serv.setServerConfigurationFile(newServerXml);

    // wait for configuration update to complete
    serv.waitForStringInLogUsingMark("CWWKG0017I");
    util.EXIT();
  }
}
