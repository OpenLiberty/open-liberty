/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.jaxrs;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM.OIDCTokenMappingResolverGenericTest;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM.OidcJaxRSClientAPITests;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM.OidcJaxRSClientBasicTests;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM.OidcJaxRSClientDiscoveryBasicTests;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM.OidcJaxRSClientReAuthnTests;

import componenttest.custom.junit.runner.AlwaysPassesTest;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                OidcJaxRSClientBasicTests.class,
                OidcJaxRSClientAPITests.class,
                OIDCTokenMappingResolverGenericTest.class,
                OidcJaxRSClientReAuthnTests.class,
                OidcJaxRSClientDiscoveryBasicTests.class,
//                OidcJaxRSClientRequestFilterTests.class
})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

}
