/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.jaxrs.IDPInitiated;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.security.saml.fat.jaxrs.common.RSSamlPkixTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RSSamlIDPInitiatedPkixWithoutCertInServerTests extends RSSamlPkixTests {

    private static final Class<?> thisClass = RSSamlIDPInitiatedPkixWithoutCertInServerTests.class;

    @BeforeClass
    public static void testClassSetupBeforeTest() throws Exception {

        APPServerConfig = "server_pkix_serverTrustMissingIDPCert.xml";
        serverHasIDPCert = false;
        setupBeforeTest();
    }
}
