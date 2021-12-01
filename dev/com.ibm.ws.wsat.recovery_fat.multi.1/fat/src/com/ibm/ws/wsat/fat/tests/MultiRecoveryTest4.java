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

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@AllowedFFDC(value = { "javax.transaction.SystemException", "javax.transaction.xa.XAException", "java.io.IOException", "java.io.EOFException" })
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MultiRecoveryTest4 extends MultiRecoveryTest{

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR013AFVT() throws Exception {
		recoveryTest(server1, server2, "1301","server1");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.xa.XAException","javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	@ExpectedFFDC(value = {"javax.transaction.RollbackException"})
	public void WSTXMPR013BFVT() throws Exception {
		recoveryTest(server1, server2, "1302","server2");
	}
	
	@Test
	public void WSTXMPR013CFVT() throws Exception {
		recoveryTest(server1, server2, "1303","both");
	}
	
	@Test
	public void WSTXMPR014AFVT() throws Exception {
		recoveryTest(server1, server2, "1401","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException"})
	@AllowedFFDC(value = {"javax.transaction.SystemException", "java.util.concurrent.RejectedExecutionException", "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR014BFVT() throws Exception {
		recoveryTest(server1, server2, "1402","server2");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.xa.XAException"})
	public void WSTXMPR014CFVT() throws Exception {
		recoveryTest(server1, server2, "1403","both");
	}
	
	@Test
	public void WSTXMPR015AFVT() throws Exception {
		recoveryTest(server1, server2, "1501","server1");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.xa.XAException" })
	public void WSTXMPR015BFVT() throws Exception {
		recoveryTest(server1, server2, "1502","server2");
	}
	
	@Test
  @Mode(TestMode.LITE)
	public void WSTXMPR015CFVT() throws Exception {
		recoveryTest(server1, server2, "1503","both");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR016AFVT() throws Exception {
		recoveryTest(server1, server2, "1601","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR016BFVT() throws Exception {
		recoveryTest(server1, server2, "1602","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR016CFVT() throws Exception {
		recoveryTest(server1, server2, "1603","both");
	}
}
