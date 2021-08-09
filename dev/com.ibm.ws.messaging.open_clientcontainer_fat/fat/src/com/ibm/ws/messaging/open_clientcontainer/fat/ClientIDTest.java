/* ============================================================================
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 * ============================================================================
 */
package com.ibm.ws.messaging.open_clientcontainer.fat;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.custom.junit.runner.FATRunner;

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class ClientIDTest extends FATBase {
  static {
    client_ = null;
    server_ = null;
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    try {
      Util.TRACE_ENTRY();
      client_ = LibertyClientFactory.getLibertyClient("com.ibm.ws.open_clientcontainer.fat.ClientIDContainer");
      server_ = LibertyServerFactory.getLibertyServer("com.ibm.ws.open_clientcontainer.fat.Server");
      deployApplication("ClientID");
      start();
    } finally {
      Util.TRACE_EXIT();
    }
  }

  @AfterClass
  public static void afterClass() throws Exception {
    try {
      Util.TRACE_ENTRY();
      server_.stopServer();
    } finally {
      undeployApplication("ClientID");
      Util.TRACE_EXIT();
    }
  }

  @Test
  public void testSetClientID() throws Exception { runTest(); }

  @Test
  public void testSetClientIDTwice() throws Exception { runTest(); }

  @Test
  public void testDurableSubscriberWithoutClientID() throws Exception { runTest(); }
}
