/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.authentication.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test that the subject is not found in the authentication cache since it is disabled.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class AuthCacheDisabledTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.authentication.fat.noauthcache");
    private final Class<?> thisClass = AuthCacheDisabledTest.class;
    private static final String GOOD_USER = "user1";
    private static final String GOOD_USER_PWD = "user1pwd";
    private static final String ACTION_VALIDATECACHE = "validateCache";
    private static CommonTestHelper testHelper;

    @BeforeClass
    public static void setUp() throws Exception {
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        server.addInstalledAppForValidation("authenticationService");
        server.startServer("AuthCacheDisabledTest.log");
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("The application did not report is was started",
                      server.waitForStringInLog("CWWKZ0001I"));

        testHelper = new CommonTestHelper(server.getHostname(), String.valueOf(server.getHttpDefaultPort()));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testSubjectIsDifferentAfterSecondAuthenticate() throws Exception {
        Log.info(thisClass, "testSubjectIsDifferentAfterSecondAuthenticate", "Test that the subject is not found in the auth cache since it is disabled.");

        String response = testHelper.performAction(ACTION_VALIDATECACHE, GOOD_USER, GOOD_USER_PWD, JaasLoginConfigConstants.SYSTEM_WEB_INBOUND);

        assertTrue("Authentication cache is disabled. The subject must not be found in the authentication cache.", response.contains("false"));
    }

}
