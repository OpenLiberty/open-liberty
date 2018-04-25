/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

public class JwtSsoTest {

	/************ begin plumbing ********** */

	private static SharedOutputManager outputMgr = SharedOutputManager.getInstance()
			.trace("com.ibm.ws.security.jwtsso.*=all");

	@Rule
	public final TestName testName = new TestName();

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		outputMgr.captureStreams();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		outputMgr.dumpStreams();
		outputMgr.resetStreams();
		outputMgr.restoreStreams();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void beforeTest() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		outputMgr.resetStreams();
	}

	/************** end plumbing **************/

	@Test
	public void doesNothing() throws Exception {

	}

}