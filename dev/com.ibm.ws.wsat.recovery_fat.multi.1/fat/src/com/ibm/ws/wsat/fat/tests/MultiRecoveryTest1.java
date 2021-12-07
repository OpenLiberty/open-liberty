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
public class MultiRecoveryTest1 extends MultiRecoveryTest{

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR001AFVT() throws Exception {
		recoveryTest(server1, server2, "101","server1");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	// Need Jon Review:
	// Got Exception WTRN0049W during test
	// Report javax.transaction.SystemException
	public void WSTXMPR001BFVT() throws Exception {
		recoveryTest(server1, server2, "102","server2");
	}
	
	@Test
	public void WSTXMPR001CFVT() throws Exception {
		recoveryTest(server1, server2, "103","both");
	}
	
	@Test
	public void WSTXMPR002AFVT() throws Exception {
		recoveryTest(server1, server2, "201","server1");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@AllowedFFDC(value = { "javax.transaction.SystemException" })
	// Need Jon Review:
	// Got Exception WTRN0049W and Warning WTRN0046E during test
	// Report javax.transaction.SystemException 
	public void WSTXMPR002BFVT() throws Exception {
		recoveryTest(server1, server2, "202","server2");
	}
	
	@Test
	public void WSTXMPR002CFVT() throws Exception {
		recoveryTest(server1, server2, "203","both");
	}

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR003AFVT() throws Exception {
		recoveryTest(server1, server2, "301","server1");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR003BFVT() throws Exception {
		recoveryTest(server1, server2, "302","server2");
	}
	
	@Mode(TestMode.LITE)
	@Test
	public void WSTXMPR003CFVT() throws Exception {
		recoveryTest(server1, server2, "303","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException"/*, "com.ibm.ws.wsat.service.WSATException" */})
	public void WSTXMPR004AFVT() throws Exception {
		recoveryTest(server1, server2, "401","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR004BFVT() throws Exception {
		recoveryTest(server1, server2, "402","server2");
	}
	
	@Test
	public void WSTXMPR004CFVT() throws Exception {
		recoveryTest(server1, server2, "403","both");
	}
}
