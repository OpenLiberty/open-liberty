/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@AllowedFFDC(value = { "javax.transaction.SystemException", "javax.transaction.xa.XAException" })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MultiRecoveryTest1 extends MultiRecoveryTest{

	@Server("WSATRecovery1")
	public static LibertyServer server;

	@Server("WSATRecovery2")
	public static LibertyServer server2;

	@BeforeClass
	public static void beforeTests() throws Exception {
		beforeTests(server, server2);
	}

	@Before
	public void beforeTest() throws Exception {
		startServers(server, server2);
	}

	@After
	public void tearDown() throws Exception {
		stopServers(server, server2);
	}

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR001AFVT() throws Exception {
		recoveryTest(server, server2, "101","server1");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	// Need Jon Review:
	// Got Exception WTRN0049W during test
	// Report javax.transaction.SystemException
	public void WSTXMPR001BFVT() throws Exception {
		recoveryTest(server, server2, "102","server2");
	}
	
	@Test
	public void WSTXMPR001CFVT() throws Exception {
		recoveryTest(server, server2, "103","both");
	}
	
	@Test
	public void WSTXMPR002AFVT() throws Exception {
		recoveryTest(server, server2, "201","server1");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	// Need Jon Review:
	// Got Exception WTRN0049W and Warning WTRN0046E during test
	// Report javax.transaction.SystemException 
	public void WSTXMPR002BFVT() throws Exception {
		recoveryTest(server, server2, "202","server2");
	}
	
	@Test
	public void WSTXMPR002CFVT() throws Exception {
		recoveryTest(server, server2, "203","both");
	}

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR003AFVT() throws Exception {
		recoveryTest(server, server2, "301","server1");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR003BFVT() throws Exception {
		recoveryTest(server, server2, "302","server2");
	}
	
	@Test
	public void WSTXMPR003CFVT() throws Exception {
		recoveryTest(server, server2, "303","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException"/*, "com.ibm.ws.wsat.service.WSATException" */})
	public void WSTXMPR004AFVT() throws Exception {
		recoveryTest(server, server2, "401","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR004BFVT() throws Exception {
		recoveryTest(server, server2, "402","server2");
	}
	
	@Test
	public void WSTXMPR004CFVT() throws Exception {
		recoveryTest(server, server2, "403","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException"/*, "com.ibm.ws.wsat.service.WSATException" */})
	public void WSTXMPR005AFVT() throws Exception {
		recoveryTest(server, server2, "501","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	// Need Jon Review:
	// Got Exception WTRN0046E and Warning WTRN0049W, WTRN0094W during test
	// Expect XAException and RollbackException
	// Report javax.transaction.SystemException 
	public void WSTXMPR005BFVT() throws Exception {
		recoveryTest(server, server2, "502","server2");
	}
	
	@Test
	//@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR005CFVT() throws Exception {
		recoveryTest(server, server2, "503","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR006AFVT() throws Exception {
		recoveryTest(server, server2, "601","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	public void WSTXMPR006BFVT() throws Exception {
		recoveryTest(server, server2, "602","server2");
	}
	
	@Test
	public void WSTXMPR006CFVT() throws Exception {
		recoveryTest(server, server2, "603","both");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.SystemException" })
	public void WSTXMPR007AFVT() throws Exception {
		recoveryTest(server, server2, "701","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException", "java.util.concurrent.RejectedExecutionException", "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR007BFVT() throws Exception {
		recoveryTest(server, server2, "702","server2");
	}
	
  @Mode(TestMode.LITE)
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException", "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// Need Jon Review:
	// javax.transaction.xa.XAException 
	// Caused by: com.ibm.tx.jta.XAResourceNotAvailableException
	// Need review on whether it is expected
	// Report javax.transaction.SystemException
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR007CFVT() throws Exception {
		recoveryTest(server, server2, "703","both");
	}
	
	@Test
	@AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.SystemException" })
	public void WSTXMPR008AFVT() throws Exception {
		recoveryTest(server, server2, "801","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException", "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR008BFVT() throws Exception {
		recoveryTest(server, server2, "802","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "javax.transaction.SystemException", "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// Need Jon Review:
	// javax.transaction.xa.XAException 
	// Caused by: com.ibm.tx.jta.XAResourceNotAvailableException
	// Need review on whether it is expected
	// Report javax.transaction.SystemException
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR008CFVT() throws Exception {
		recoveryTest(server, server2, "803","both");
	}
}
