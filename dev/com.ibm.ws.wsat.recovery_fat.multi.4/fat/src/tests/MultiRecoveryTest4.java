/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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

@SuppressWarnings("restriction")
@AllowedFFDC(value = { "java.net.SocketException", "com.ibm.tx.jta.ut.util.AlreadyDumpedException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "java.io.IOException", "java.io.EOFException", "org.apache.cxf.binding.soap.SoapFault", "javax.xml.stream.XMLStreamException", "com.ctc.wstx.exc.WstxIOException" })
@RunWith(FATRunner.class)
public class MultiRecoveryTest4 extends MultiRecoveryTest {

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException"})
	public void WSTXMPR009AFVT() throws Exception {
		recoveryTest(server1, server2, "901","server1");
	}

	@Test
	// XAException should be expected here but halt is no longer halting the JVM so extra protocol
	// messages can get through after the dumpState.
	public void WSTXMPR009BFVT() throws Exception {
		recoveryTest(server1, server2, "902","server2");
	}

	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	public void WSTXMPR009CFVT() throws Exception {
		recoveryTest(server1, server2, "903","both");
	}

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException"})
	public void WSTXMPR010AFVT() throws Exception {
		recoveryTest(server1, server2, "1001","server1");
	}

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException"})
	// XAException should be expected here but halt is no longer halting the JVM so extra protocol
	// messages can get through after the dumpState.
	public void WSTXMPR010BFVT() throws Exception {
		recoveryTest(server1, server2, "1002","server2");
	}

	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	// Can get HeuristicRollbackException if commit gets through to subordinate after resources were already dumped
	@AllowedFFDC(value={"javax.transaction.HeuristicRollbackException"})
	public void WSTXMPR010CFVT() throws Exception {
		recoveryTest(server1, server2, "1003","both");
	}

	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR011AFVT() throws Exception {
		recoveryTest(server1, server2, "1101","server1");
	}

	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR011BFVT() throws Exception {
		recoveryTest(server1, server2, "1102","server2");
	}
}
