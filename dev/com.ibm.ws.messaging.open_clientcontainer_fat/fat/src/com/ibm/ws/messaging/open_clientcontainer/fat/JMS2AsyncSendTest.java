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

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyClientFactory;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.custom.junit.runner.FATRunner;

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
@AllowedFFDC({ "com.ibm.ws.sib.jfapchannel.JFapConnectionBrokenException"
              ,"com.ibm.ws.sib.processor.exceptions.SIMPLimitExceededException"
            })
public class JMS2AsyncSendTest extends FATBase {
  static {
    client_ = LibertyClientFactory.getLibertyClient("com.ibm.ws.open_clientcontainer.fat.JMS2Container");
    server_ = LibertyServerFactory.getLibertyServer("com.ibm.ws.open_clientcontainer.fat.Server");
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    try {
      Util.TRACE_ENTRY();
      deployApplication("JMS2AsyncSend");
      // CWSIC2008E -  FFDC for commit/rollback error in testJMS2ContextInListener
      client_.addIgnoreErrors("CWSIC2008E");
      start();
    } finally {
      Util.TRACE_EXIT();
    }
  }

  @AfterClass
  public static void afterClass() throws Exception {
    try {
      Util.TRACE_ENTRY();
      // CWSIJ0051E - Destination not found as expected for testJMS2InvalidDestination
      // CWSIK0015E - The destination QUEUE4 was not found on messaging engine defaultME. - expected for testJMS2InvalidDestination
      // CWSIC2009E - CWSIK0025E was reported
      // CWSIK0025E - Queue full; expected.
      server_.stopServer("CWSIJ0051E","CWSIK0015E","CWSIC2009E","CWSIK0025E");
    } finally {
      Util.TRACE_EXIT();
    }
  }

  @Test
  public void testJMS2NoAsync()throws Exception { runTest(); }
  @Test
  public void testJMS2SetAsync()throws Exception { runTest(); }
  @Test
  public void testJMS2GetAsync() throws Exception { runTest(); }
  @Test
  public void testJMS2CompletionListener() throws Exception { runTest(); }
  @Test
  public void testJMS2ExceptionUndefinedQueue()throws Exception { runTest(); }
  @Test
  public void testJMS2MessageOrderingSingleProducer() throws Exception { runTest(); }
  @Test
  public void testJMS2MessageOrderingMultipleProducers() throws Exception { runTest(); }
  @Test
  public void testJMS2MessageOrderingMultipleContexts() throws Exception { runTest(); }
  @Test
  public void testJMS2MessageOrderingSyncAsyncMix() throws Exception { runTest(); }
  @Test
  public void testJMS2TransactionAndListener() throws Exception { runTest(); }
  @Test
  public void testJMS2Close() throws Exception { runTest(); }
  @Test
  public void testJMS2Commit() throws Exception { runTest(); }
  @Test
  public void testJMS2RollBack() throws Exception { runTest(); }
  @Test
  public void testJMS2MessageTypes() throws Exception { runTest(); }
  @Test
  public void testJMS2MessageTypesOrder() throws Exception { runTest(); }
  @Test
  public void testJMS2InvalidDestination() throws Exception { runTest(); }
  @Test
  public void testJMS2ContextInListener() throws Exception { runTest(); }
  @Test
  public void testJMS2DefaultConnectionFactory() throws Exception { runTest(); }
  @Test
  public void testJMS2DefaultConnectionFactoryVariation() throws Exception { runTest(); }
}
