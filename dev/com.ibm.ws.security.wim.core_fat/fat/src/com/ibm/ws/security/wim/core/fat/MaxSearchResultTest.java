/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
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

package com.ibm.ws.security.wim.core.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class MaxSearchResultTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.core.fat.maxSearchResult");
    private static final Class<?> c = MaxSearchResultTest.class;
    private static UserRegistryServletConnection servlet;

    @BeforeClass
    public static void setUp() throws Exception {

        /*
         * Transform any applications into EE9 when necessary.
         */
        FATSuite.transformApps(server, "dropins/userRegistry.war");

        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(server);
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");

        // install our user feature
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/securitylibertyinternals-1.0.mf");
        server.addInstalledAppForValidation("userRegistry");
        server.startServer(c.getName() + ".log");

        //Make sure the application has come up before proceeding
        assertNotNull("Application userRegistry does not appear to have started.",
                      server.waitForStringInLog("CWWKZ0001I:.*userRegistry"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I"));

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(server.getHostname(), server.getHttpDefaultPort());

        if (servlet.getRealm() == null) {
            Thread.sleep(5000);
            servlet.getRealm();
        }

    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");

        try {
            server.stopServer("CWIML1018E", "CWWKE1102W");
            //added "CWWKE1102W" to ignore server quiesce timeouts due to slow test machines
        } finally {
            server.removeInstalledAppForValidation("userRegistry");
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * Hit the test servlet to see if getRealm works.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("maxsearchresult", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if getUsers works when passed in a valid user pattern
     * and a limit of 2; should get com.ibm.websphere.wim.exception.MaxResultsExceededException
     * as maxSearchResult is set to 1 in server.xml
     */
    @Test
    public void getUsers() throws Exception {
        String user = "test*";
        Log.info(c, "getUsers", "Checking with a valid pattern and limit of 2.");

        try {
            servlet.getUsers(user, 2);
            fail("Expected RegistryException.");
        } catch (RegistryException re) {
            String msg = re.getMessage();
            assertTrue("Expected a CWIML1018E exception message. Message: " + msg, msg.contains("CWIML1018E"));
        }
    }

    /**
     * Hit the test servlet to see if getGroups works when passed in a valid group pattern
     * and a limit of 2; should get com.ibm.websphere.wim.exception.MaxResultsExceededException
     * as maxSearchResult is set to 1 in server.xml
     */
    @Test
    public void getGroups() throws Exception {
        String group = "gr*";
        Log.info(c, "getGroups", "Checking with a valid pattern and limit of 2.");

        try {
            servlet.getGroups(group, 2);
            fail("Expected RegistryException.");
        } catch (RegistryException re) {
            String msg = re.getMessage();
            assertTrue("Expected a CWIML1018E exception message. Message: " + msg, msg.contains("CWIML1018E"));
        }
    }
}