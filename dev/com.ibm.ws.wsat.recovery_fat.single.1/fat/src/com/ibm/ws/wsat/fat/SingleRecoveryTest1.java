/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.transaction.fat.util.FATUtils;
import com.ibm.ws.wsat.fat.tests.SingleRecoveryTest;
import com.ibm.ws.wsat.fat.util.WSATTest;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.exception.TopologyException;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class SingleRecoveryTest1 extends SingleRecoveryTest {

	@Test
	public void WSTXREC001FVT() throws Exception {
		recoveryTest("01");
	}

	@Test
	public void WSTXREC002FVT() throws Exception {
		recoveryTest("02");
	}

	@Test
	public void WSTXREC003FVT() throws Exception {
		recoveryTest("03");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"javax.transaction.RollbackException" })
	public void WSTXREC004FVT() throws Exception {
		recoveryTest("04");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"javax.transaction.RollbackException" })
	public void WSTXREC005FVT() throws Exception {
		recoveryTest("05");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"javax.transaction.RollbackException" })
	public void WSTXREC006FVT() throws Exception {
		recoveryTest("06");
	}

	@Test
	public void WSTXREC007FVT() throws Exception {
		recoveryTest("07");
	}

	@Test
	public void WSTXREC008FVT() throws Exception {
		recoveryTest("08");
	}

	@Test
	public void WSTXREC009FVT() throws Exception {
		recoveryTest("09");
	}

	@Test
	public void WSTXREC010FVT() throws Exception {
		recoveryTest("10");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
	public void WSTXREC011FVT() throws Exception {
		recoveryTest("11");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
	public void WSTXREC012FVT() throws Exception {
		recoveryTest("12");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
	public void WSTXREC013FVT() throws Exception {
		recoveryTest("13");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
	public void WSTXREC014FVT() throws Exception {
		recoveryTest("14");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	public void WSTXREC015FVT() throws Exception {
		recoveryTest("15");
	}
}