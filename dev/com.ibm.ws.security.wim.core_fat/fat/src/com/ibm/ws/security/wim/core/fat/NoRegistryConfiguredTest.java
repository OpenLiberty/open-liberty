/*******************************************************************************
 * Copyright (c) 2018, 2025 IBM Corporation and others.
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

/**
 * Add a test with a failing LDAP config to ensure we get a well defined error instead of an NPE when we
 * try to use the registry.
 * Issue 2489
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class NoRegistryConfiguredTest {

    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.core.fat.noreg");
    private static final Class<?> c = NoRegistryConfiguredTest.class;
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
            server.stopServer("CWWKG0032W", "CWWKG0058E", "CWIMK0011E", "CWWKE1102W");
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
        assertEquals("Received incorrect realm", "WIMRegistry", servlet.getRealm());
    }

    /**
     * Doing a getUsers should fail and return a no registry exception
     */
    @Test
    public void getUser() throws Exception {
        String user = "user1";
        Log.info(c, "getUser", "Trying to get a user");

        try {
            servlet.getUsers(user, 1);
            fail("Expected RegistryException calling getUsers.");
        } catch (RegistryException re) {
            String msg = re.getMessage();
            assertTrue("Expected a CWIMK0011E exception message. Message: " + msg, msg.contains("CWIMK0011E"));
        }
    }

    /**
     * Doing a getGroups should fail and return a no registry exception
     */
    @Test
    public void getGroups() throws Exception {
        String group = "group1";
        Log.info(c, "getGroups", "Trying to get a group");

        try {
            servlet.getGroups(group, 2);
            fail("Expected RegistryException calling getGroups.");
        } catch (RegistryException re) {
            String msg = re.getMessage();
            assertTrue("Expected a CWIMK0011E exception message. Message: " + msg, msg.contains("CWIMK0011E"));
        }
    }

    /**
     * Checkpassword should fail and return a no registry exception
     *
     * @throws Exception
     */
    @Test
    public void checkPassword() throws Exception {
        String user = "user1";
        Log.info(c, "checkPassword", "Trying to login a user");

        try {
            servlet.checkPassword(user, "dummy");
            fail("Expected RegistryException calling checkPassword.");
        } catch (RegistryException re) {
            String msg = re.getMessage();
            assertTrue("Expected a CWIMK0011E exception message. Message: " + msg, msg.contains("CWIMK0011E"));
        }

    }

}