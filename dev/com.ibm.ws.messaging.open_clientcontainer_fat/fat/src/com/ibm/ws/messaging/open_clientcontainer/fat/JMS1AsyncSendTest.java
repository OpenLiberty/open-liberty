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
package com.ibm.ws.messaging.open_clientcontainer.fat;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.Ignore;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.custom.junit.runner.FATRunner;

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
@AllowedFFDC({ "com.ibm.ws.sib.jfapchannel.JFapConnectionBrokenException"
              ,"com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException"
              ,"com.ibm.ws.sib.processor.exceptions.SIMPLimitExceededException"
             })
public class JMS1AsyncSendTest extends FATBase {
  static {
    client_ = LibertyClientFactory.getLibertyClient("com.ibm.ws.open_clientcontainer.fat.JMS1Container");
    server_ = LibertyServerFactory.getLibertyServer("com.ibm.ws.open_clientcontainer.fat.Server");
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    try {
      Util.TRACE_ENTRY();
      deployApplication("JMS1AsyncSend");
      // CWSIA0281E - expected invalid destination
      // CWSIC2008E - FFDC for commit/rollback error in testJMS1TransactedListener
      client_.addIgnoreErrors("CWSIA0281E","CWSIC2008E");
      start();
    } finally {
      Util.TRACE_EXIT();
    }
  }

  @AfterClass
  public static void afterClass() throws Exception {
    try {
      Util.TRACE_ENTRY();
      Util.CODEPATH();
      // CWSIC2018E - comms error caused by...
      // CWSIJ0047E - ... dropped connection
      // CWSIC2009E - Message send failure, typically caused by expected CWSIK0025E
      // CWSIK0025E - Queue full; expected from several operations
      server_.stopServer("CWSIC2018E","CWSIJ0047E","CWSIC2009E","CWSIK0025E");
    } finally {
      Util.TRACE_EXIT();
    }
  }

  @Test
  public void testJMS1AsyncSend() throws Exception { runTest(); }
  @Test
  public void testJMS1ExceptionMessageThreshhold() throws Exception { runTest(); }
  @Test
  public void testJMS1AsyncSendException() throws Exception { runTest(); }
  @Test
  public void testJMS1MessageOrderingSingleProducer() throws Exception { runTest(); }
  @Test
  public void testJMS1MessageOrderingMultipleProducers() throws Exception { runTest(); }
  @Test
  public void testJMS1MessageOrderingMultipleSessions() throws Exception { runTest(); }
  @Test
  public void testJMS1CloseSession() throws Exception { runTest(); }
  @Test
  public void testJMS1CloseConnection() throws Exception { runTest(); }
  @Test
  public void testJMS1AsyncSendUnidentifiedProducerUnidentifiedDestination() throws Exception { runTest(); }
  @Test
  public void testJMS1AsyncSendNullListener() throws Exception { runTest(); }
  @Test
  public void testJMS1AsyncSendNoDestination() throws Exception { runTest(); }
  @Test
  public void testJMS1CompletionListener() throws Exception { runTest(); }
  @Test
  public void testJMS1SessionInListener() throws Exception { runTest(); }
  @Test
  public void testJMS1TransactionAndListener() throws Exception { runTest(); }
  @Ignore // disabling for now: issue 9842
  @Test
  public void testJMS1TimeToLive() throws Exception { runTest(); }
  @Test
  public void testJMS1Priority() throws Exception { runTest(); }
  @Test
  public void testJMS1NegativePriority() throws Exception { runTest(); }
  @Test
  public void testJMS1DeliveryMode() throws Exception { runTest(); }
  @Test
  public void testJMS1NullEmptyMessage() throws Exception { runTest(); }
}
