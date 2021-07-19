/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class URAPIs_TDSLDAP_SearchBaseTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.tds.searchbase");
    private static final Class<?> c = URAPIs_TDSLDAP_SearchBaseTest.class;
    private static UserRegistryServletConnection servlet;

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(server);
        Log.info(c, "setUp", "Starting the server... (will wait for userRegistry servlet to start)");
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
            server.stopServer("CWIML4529E", "CWIML4537E");
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user.
     */
    @Test
    public void getGroupsForUser() throws Exception {
        String user = "eddard_vmmUser";
        Log.info(c, "getGroupsForUser", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertTrue(list.contains("stark_vmmGroup"));
    }

    /**
     * Hit the test servlet to see if getGroupsForUser works when supplied with a valid user
     * that is part of groups outside of the group search base. It should find the user, but
     * not any groups.
     */
    @Test
    public void getGroupsForUserWithInvalidUser() throws Exception {
        String user = "vmmtestuser";
        Log.info(c, "getGroupsForUserWithInvalidUser", "Checking with an invalid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertTrue(list.isEmpty());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied with a valid user.
     */
    @Test
    public void getUniqueGroupIds() throws Exception {
        String user = "uid=eddard_vmmUser,ou=jUsers,o=ibm,c=us";
        Log.info(c, "getUniqueGroupIds", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertTrue(list.contains("cn=stark_vmmGroup,ou=jGroups,o=ibm,c=us"));
        assertEquals("The number of entries did not match.", 1, list.size());
    }

    /**
     * Hit the test servlet to see if getUniqueGroupIdsForUser works when supplied
     * with a valid uniqueName. The groups associated with this user are outside
     * the group search base and should return an empty list.
     */
    @Test
    public void getUniqueGroupIdsOutsideSearchBase() throws Exception {
        String user = "vmmtestuser";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertTrue(list.isEmpty());
    }
}