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
import componenttest.topology.utils.FATServletClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.logging.Level;
import java.util.logging.Logger;

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class WasJmsOutBoundTest extends FATServletClient {

  public static final MessagingFATUtilities util = new MessagingFATUtilities(WasJmsOutBoundTest.class);
  public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.messaging.open_comms.WJOServer");
  public static LibertyServer client = LibertyServerFactory.getLibertyServer("com.ibm.ws.messaging.open_comms.WJOClient");
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
      util.CODEPATH();
    } catch (Exception ce) {
      util.TRACE("Exception caught whilst stopping client Liberty server.");
      e = ce;
    }
    try {
      server.stopServer();
      util.CODEPATH();
    } catch (Exception se) {
      util.TRACE("Exception caught whilst stopping server Liberty server.");
      if (null==e) e = se;
    }
    if (null!=e) {
      throw e;
    }
    util.EXIT();
  }

  @Test
  public void testWJOSendReceive2LP() throws Exception {
    util.ENTRY();
    runTest(client,"CommsLP","testQueueSendMessage");
    util.CODEPATH();
    runTest(client,"CommsLP","testQueueReceiveMessages");

    util.CODEPATH();
    String msg = client.waitForStringInLog("Queue Message", client.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the queue message in the message log", msg);
    util.EXIT();
  }
}
