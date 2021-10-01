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
public class MultiRecoveryTest3 extends MultiRecoveryTest{

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
	@AllowedFFDC(value = {"javax.transaction.xa.XAException", "javax.xml.ws.WebServiceException"})
	public void WSTXMPR009AFVT() throws Exception {
		recoveryTest(server, server2, "901","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	@AllowedFFDC(value = {"javax.transaction.SystemException"})
	//Caused by: javax.transaction.xa.XAException
	//at com.ibm.ws.wsat.tm.impl.ParticipantResource.commit(ParticipantResource.java:114)
	//Perhaps this can be ignored
	public void WSTXMPR009BFVT() throws Exception {
		recoveryTest(server, server2, "902","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	//javax.transaction.xa.XAException 
	//Caused by: com.ibm.tx.jta.XAResourceNotAvailableException
	//Need review on whether it is expected
	public void WSTXMPR009CFVT() throws Exception {
		recoveryTest(server, server2, "903","both");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.transaction.xa.XAException", "javax.xml.ws.WebServiceException"})
	public void WSTXMPR010AFVT() throws Exception {
		recoveryTest(server, server2, "1001","server1");
	}
	
	@Test
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "javax.transaction.SystemException"})
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	// Need Jon Review:
	// Caused by: javax.transaction.xa.XAException
	// at com.ibm.ws.wsat.tm.impl.ParticipantResource.commit(ParticipantResource.java:114)
	// Perhaps this can be ignored
	public void WSTXMPR010BFVT() throws Exception {
		recoveryTest(server, server2, "1002","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException" })
	// Need Jon Review:
	// javax.transaction.xa.XAException 
	// Caused by: com.ibm.tx.jta.XAResourceNotAvailableException
	// Need review on whether it is expected
	// Maybe a defect so
	// Add @ExpectedFFDC(value = {"javax.transaction.xa.XAException"})
	// Because javax.transaction.xa.XAException > at com.ibm.tx.jta.embeddable.impl.WSATParticipantWrapper.commit(WSATParticipantWrapper.java:118)
	public void WSTXMPR010CFVT() throws Exception {
		recoveryTest(server, server2, "1003","both");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR011AFVT() throws Exception {
		recoveryTest(server, server2, "1101","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR011BFVT() throws Exception {
		recoveryTest(server, server2, "1102","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	public void WSTXMPR011CFVT() throws Exception {
		recoveryTest(server, server2, "1103","both");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@AllowedFFDC(value = {"javax.xml.ws.WebServiceException", "com.ibm.ws.wsat.service.WSATException", "java.net.SocketException" })
	public void WSTXMPR012AFVT() throws Exception {
		recoveryTest(server, server2, "1201","server1");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@AllowedFFDC(value = {"javax.transaction.SystemException" })
	public void WSTXMPR012BFVT() throws Exception {
		recoveryTest(server, server2, "1202","server2");
	}
	
	@Test
	@ExpectedFFDC(value = {"javax.transaction.xa.XAException", "javax.transaction.RollbackException"})
	@Mode(TestMode.LITE)
	public void WSTXMPR012CFVT() throws Exception {
		recoveryTest(server, server2, "1203","both");
	}
}
