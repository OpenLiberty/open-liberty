/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.jaxrs.config;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.security.saml.fat.jaxrs.config.IDPInitiated.RSSamlIDPInitiatedMapToUserRegistryConfigTests;
import com.ibm.ws.security.saml.fat.jaxrs.config.IDPInitiated.RSSamlIDPInitiatedMiscConfigTests;
import com.ibm.ws.security.saml.fat.jaxrs.config.IDPInitiated.RSSamlIDPInitiatedSSLConfigTests;

@RunWith(Suite.class)
@SuiteClasses({

        RSSamlIDPInitiatedMiscConfigTests.class,
        RSSamlIDPInitiatedMapToUserRegistryConfigTests.class,
        RSSamlIDPInitiatedSSLConfigTests.class

})
/**
 * Purpose: This suite collects and runs all known good test suites.
 */
public class FATSuite {

}
