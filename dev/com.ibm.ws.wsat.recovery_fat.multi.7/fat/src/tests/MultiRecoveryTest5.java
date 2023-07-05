/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

@AllowedFFDC(value = {"java.io.FileNotFoundException", "javax.xml.ws.WebServiceException", "com.ibm.tx.jta.ut.util.AlreadyDumpedException", "javax.transaction.SystemException", "javax.transaction.xa.XAException", "java.io.IOException", "java.io.EOFException", "org.apache.cxf.binding.soap.SoapFault", "javax.xml.stream.XMLStreamException" })
@RunWith(FATRunner.class)
public class MultiRecoveryTest5 extends MultiRecoveryTest {

	@Test
	public void WSTXMPR008DFVT() throws Exception {
		recoveryTest(server1, server2, "801", "server1", "none");
	}

	@Test
	public void WSTXMPR008EFVT() throws Exception {
		recoveryTest(server1, server2, "802", "server2", "none");
	}

//	@Test No front end routing
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	public void WSTXMPR008FFVT() throws Exception {
		recoveryTest(server1, server2, "803", "both", "none");
	}

	@Test
	public void WSTXMPR009DFVT() throws Exception {
		recoveryTest(server1, server2, "901", "server1", "none");
	}

	@Test
	public void WSTXMPR009EFVT() throws Exception {
		recoveryTest(server1, server2, "902", "server2", "none");
	}

//	@Test No front end routing
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	public void WSTXMPR009FFVT() throws Exception {
		recoveryTest(server1, server2, "903", "both", "none");
	}
}
