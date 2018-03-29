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
package com.ibm.ws.security.jwt.utils;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

public class TokenBuilderTest {

	private static SharedOutputManager outputMgr = SharedOutputManager.getInstance()
			.trace("com.ibm.ws.security.jwt.*=all");

	@Rule
	public final TestName testName = new TestName();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		outputMgr.captureStreams();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		outputMgr.dumpStreams();
		outputMgr.resetStreams();
		outputMgr.restoreStreams();
	}

	@Before
	public void beforeTest() {
		System.out.println("Entering test: " + testName.getMethodName());
	}

	@After
	public void tearDown() throws Exception {
		System.out.println("Exiting test: " + testName.getMethodName());
	}

	// test that user and group attributes match mp-jwt spec.
	@Test
	public void checkClaimNames() {
		assertTrue("wrong user claim name", TokenBuilder.USER_CLAIM.equals("upn"));
		assertTrue("wrong group claim name", TokenBuilder.GROUP_CLAIM.equals("groups"));

	}

}
