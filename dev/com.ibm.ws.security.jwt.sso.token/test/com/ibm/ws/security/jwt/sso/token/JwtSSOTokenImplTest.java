/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.sso.token;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

public class JwtSSOTokenImplTest {
	protected final Mockery context = new JUnit4Mockery() {
		{
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	private final ComponentContext cc = context.mock(ComponentContext.class);

	private final JsonWebToken jwt = context.mock(JsonWebToken.class, "jwt");

	private static SharedOutputManager outputMgr = SharedOutputManager.getInstance()
			.trace("com.ibm.ws.security.jwt.sso.authenticator.*=all");

	@Rule
	public final TestName testName = new TestName();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		outputMgr.captureStreams();
	}

	@Before
	public void setUp() {
		System.out.println("Entering test: " + testName.getMethodName());
	}

	@After
	public void tearDown() throws Exception {
		context.assertIsSatisfied();
		System.out.println("Exiting test: " + testName.getMethodName());
		outputMgr.resetStreams();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		outputMgr.dumpStreams();
		outputMgr.restoreStreams();
	}

}