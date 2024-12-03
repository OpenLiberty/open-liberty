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

@AllowedFFDC(value = { "java.net.SocketException", "com.ibm.tx.jta.ut.util.AlreadyDumpedException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "java.io.IOException", "java.io.EOFException", "org.apache.cxf.binding.soap.SoapFault", "javax.xml.stream.XMLStreamException", "com.ctc.wstx.exc.WstxIOException" })
@RunWith(FATRunner.class)
public class MultiRecoveryTest1 extends MultiRecoveryTest {

	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException" })
	public void WSTXMPR001AFVT() throws Exception {
		recoveryTest(server1, server2, "101","server1");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
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
	@AllowedFFDC(value = { "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "javax.resource.spi.ResourceAllocationException"})
	public void WSTXMPR002AFVT() throws Exception {
		recoveryTest(server1, server2, "201","server1");
	}
	
	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
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
}
