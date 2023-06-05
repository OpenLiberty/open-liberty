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

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class SingleRecoveryTest2 extends SingleRecoveryTest {

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	public void WSTXREC016FVT() throws Exception {
		recoveryTest("16");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	public void WSTXREC017FVT() throws Exception {
		recoveryTest("17");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException"})
	public void WSTXREC018FVT() throws Exception {
		recoveryTest("18");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"com.ibm.tx.jta.XAResourceNotAvailableException" })
	public void WSTXREC037FVT() throws Exception {
		recoveryTest("37");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"com.ibm.tx.jta.XAResourceNotAvailableException" })
	public void WSTXREC038FVT() throws Exception {
		recoveryTest("38");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"javax.transaction.SystemException" })
	public void WSTXREC039FVT() throws Exception {
		recoveryTest("39");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"javax.transaction.SystemException" })
	public void WSTXREC040FVT() throws Exception {
		recoveryTest("40");
	}

	@Test
	public void WSTXREC041FVT() throws Exception {
		recoveryTest("41");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.RollbackException", "javax.transaction.xa.XAException" })
	public void WSTXREC042FVT() throws Exception {
		recoveryTest("42");
	}

	@Test
	public void WSTXREC043FVT() throws Exception {
		recoveryTest("43");
	}

	@Test
	public void WSTXREC044FVT() throws Exception {
		recoveryTest("44");
	}

	@Test
	@ExpectedFFDC(value = { "javax.transaction.xa.XAException",
			"com.ibm.tx.jta.XAResourceNotAvailableException" })
	public void WSTXREC045FVT() throws Exception {
		recoveryTest("45");
	}

	@Test
	public void WSTXREC046FVT() throws Exception {
		recoveryTest("46");
	}

	@Test
	public void WSTXREC047FVT() throws Exception {
		recoveryTest("47");
	}

	@Test
	@ExpectedFFDC(value = { "java.lang.RuntimeException", "com.ibm.tx.jta.XAResourceNotAvailableException" })
	public void WSTXREC048FVT() throws Exception {
		recoveryTest("48");
	}
}
