/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package tests;

import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;

@AllowedFFDC(value = { "com.ibm.tx.jta.ut.util.AlreadyDumpedException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "java.io.IOException", "java.io.EOFException" })
@RunWith(FATRunner.class)
public class MultiRecoveryTest2 extends MultiRecoveryTest {

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException" })
	public void WSTXMPR005AFVT() throws Exception {
		recoveryTest(server1, server2, "501","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	// Need Jon Review:
	// Got Exception WTRN0046E and Warning WTRN0049W, WTRN0094W during test
	// Expect XAException and RollbackException
	// Report javax.transaction.SystemException 
	public void WSTXMPR005BFVT() throws Exception {
		recoveryTest(server1, server2, "502","server2");
	}
	
	@Test
	public void WSTXMPR005CFVT() throws Exception {
		recoveryTest(server1, server2, "503","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR006AFVT() throws Exception {
		recoveryTest(server1, server2, "601","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR006BFVT() throws Exception {
		recoveryTest(server1, server2, "602","server2");
	}
	
	@Test
	public void WSTXMPR006CFVT() throws Exception {
		recoveryTest(server1, server2, "603","both");
	}
	
	@Test
	public void WSTXMPR007AFVT() throws Exception {
		recoveryTest(server1, server2, "701","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "java.util.concurrent.RejectedExecutionException", "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR007BFVT() throws Exception {
		recoveryTest(server1, server2, "702","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// Need Jon Review:
	// javax.transaction.xa.XAException 
	// Caused by: com.ibm.tx.jta.XAResourceNotAvailableException
	// Need review on whether it is expected
	// Report javax.transaction.SystemException
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR007CFVT() throws Exception {
		recoveryTest(server1, server2, "703","both");
	}
	
	@Test
	public void WSTXMPR008AFVT() throws Exception {
		recoveryTest(server1, server2, "801","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR008BFVT() throws Exception {
		recoveryTest(server1, server2, "802","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = { "com.ibm.ws.Transaction.JTA.HeuristicHazardException" })
	// Need Jon Review:
	// javax.transaction.xa.XAException 
	// Caused by: com.ibm.tx.jta.XAResourceNotAvailableException
	// Need review on whether it is expected
	// Report javax.transaction.SystemException
	// JDK8: Allow HeuristicHazardException
	public void WSTXMPR008CFVT() throws Exception {
		recoveryTest(server1, server2, "803","both");
	}
}
