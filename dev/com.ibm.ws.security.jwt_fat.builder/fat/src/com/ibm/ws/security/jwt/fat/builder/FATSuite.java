/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
		// // Ported list of tests (some already renamed)
		// AlwaysRunAndPassTest.class,
		//
		// // Basic Functional tests
		// JwtBuilderApiBasicTests.class,
		// JwtBuilderApiWithLDAPBasicTests.class,
		JwkEndpointValidationUrlTests.class,
		//
		// // Configuration Tests
		JwtBuilderAPIConfigTests.class,
		// JwtBuilderAPIConfigAltKeyStoreTests.class,
		// JwtBuilderAPIWithLDAPConfigTests.class,
		// JwtBuilderAPIMinimumConfigTests.class,
		// JwtBuilderAPIMinimumRunnableConfigTests.class,
		// JwtBuilderAPIMinimumSSLConfigGlobalTests.class,
		// JwtBuilderAPIMinimumSSLConfigBuilderTests.class

})

public class FATSuite {

	public static boolean runAsCollection = false;

}
