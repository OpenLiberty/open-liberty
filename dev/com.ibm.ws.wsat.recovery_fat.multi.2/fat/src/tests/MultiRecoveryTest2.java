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

@AllowedFFDC(value = { "java.net.SocketException", "com.ibm.tx.jta.ut.util.AlreadyDumpedException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "java.io.IOException", "java.io.EOFException", "org.apache.cxf.binding.soap.SoapFault", "javax.xml.stream.XMLStreamException", "com.ctc.wstx.exc.WstxIOException" })
@RunWith(FATRunner.class)
public class MultiRecoveryTest2 extends MultiRecoveryTest {

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException" })
	public void WSTXMPR005AFVT() throws Exception {
		recoveryTest(server1, server2, "501","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
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
	public void WSTXMPR003CFVT() throws Exception {
		recoveryTest(server1, server2, "303","both");
	}

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException" })
	public void WSTXMPR004AFVT() throws Exception {
		recoveryTest(server1, server2, "401","server1", "server1", "Set response from ACTIVE to ABORTED", null);
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
