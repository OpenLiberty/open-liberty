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

import componenttest.topology.impl.LibertyServerFactory;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import org.junit.Test;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.AfterClass;
import org.junit.BeforeClass;

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class WasJmsOutBoundTest extends FATBase {
  static {
    server_ = LibertyServerFactory.getLibertyServer("com.ibm.ws.messaging.open_comms.WJOServer");
    client_ = LibertyServerFactory.getLibertyServer("com.ibm.ws.messaging.open_comms.WJOClient");
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
  public void testWJOSendReceive2LP() throws Exception {
    Util.TRACE_ENTRY();
    runTest(client_,"CommsLP","testQueueSendMessage");
    Util.CODEPATH();
    runTest(client_,"CommsLP","testQueueReceiveMessages");

    Util.CODEPATH();
    String msg = client_.waitForStringInLog("Queue Message", client_.getMatchingLogFile("messages.log"));
    assertNotNull("Could not find the queue message in the message log", msg);
    Util.TRACE_EXIT();
  }
}
