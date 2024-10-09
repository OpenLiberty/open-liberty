/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
public class MultiRecoveryTest5 extends MultiRecoveryTest {

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR013AFVT() throws Exception {
		recoveryTest(server1, server2, "1301","server1");
	}

	@Test
	@AllowedFFDC(value = { "javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
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
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR011CFVT() throws Exception {
		recoveryTest(server1, server2, "1103","both");
	}

	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException"})
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException", "javax.transaction.RollbackException"})
	public void WSTXMPR012AFVT() throws Exception {
		recoveryTest(server1, server2, "1201","server1");
	}

	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR012BFVT() throws Exception {
		recoveryTest(server1, server2, "1202","server2");
	}

	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR012CFVT() throws Exception {
		recoveryTest(server1, server2, "1203","both");
	}
}
