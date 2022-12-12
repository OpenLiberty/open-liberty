/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import org.junit.Assume;
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

/**
 * Test operational attribute group membership. The InMemoryLDAPServer does not have operational
 * attribute support, so this test uses the physical TDS server. Additionally, in order to hit
 * getGroupsByOperationalAttribute in the code, we must define groupMemeberIdMap in server.xml.
 * This is why we use idsFilters instead of groupProperties.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class OperationalAttributeTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.wim.adapter.ldap.fat.operationalattribute");
    private static final Class<?> c = OperationalAttributeTest.class;
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
     * Call getGroupsForUser on vmmtestuser. vmmtestuser is part of groups outside
     * of the specified group search base of ou=jgroups,o=ibm,c=us. These groups should
     * not be returned. This test will fail without the additional group validation
     * for operational attributes.
     */
    @Test
    public void getGroupsForUserWithInvalidUser() throws Exception {
        String user = "vmmtestuser";
        Log.info(c, "getGroupsForUserWithInvalidUser", "Checking with an invalid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertTrue(list.isEmpty());
    }

    /**
     * Call getUniqueGroupIdsForUser on vmmtestuser. vmmtestuser is part of groups outside
     * of the specified group search base of ou=jgroups,o=ibm,c=us. These groups should
     * not be returned. This test will fail without the additional group validation
     * for operational attributes.
     */
    @Test
    public void getUniqueGroupIdsOutsideSearchBase() throws Exception {
        String user = "vmmtestuser";
        Log.info(c, "getUniqueGroupIdsForUser", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertTrue(list.isEmpty());
    }

    /**
     * Call getGroupsForUser on eddard_vmmUser. This user does have groups within
     * the group search base of ou=jgroups,o=ibm,c=us. Ensure that these groups
     * are returned. This test verifies group membership is still functioning
     * properly. This test will not work with local LDAP as operational
     * attributes are not supported.
     */
    @Test
    public void getGroupsForUser() throws Exception {
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = "eddard_vmmUser";
        Log.info(c, "getGroupsForUser", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertTrue(list.contains("cn=stark_vmmGroup,ou=jGroups,o=ibm,c=us"));
        assertEquals("We expected exactly 1 entry returned.", 1, list.size());
    }

    /**
     * Call getUniqueGroupIdsForUser on eddard_vmmUser. This user does have groups within
     * the group search base of ou=jgroups,o=ibm,c=us. Ensure that these groups
     * are returned. This test verifies group membership is still functioning
     * properly. This test will not work with local LDAP as operational
     * attributes are not supported.
     */
    @Test
    public void getUniqueGroupIdsForUser() throws Exception {
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = "uid=eddard_vmmUser,ou=jUsers,o=ibm,c=us";
        Log.info(c, "getUniqueGroupIds", "Checking with a valid user.");
        List<String> list = servlet.getUniqueGroupIdsForUser(user);
        assertTrue(list.contains("cn=stark_vmmGroup,ou=jGroups,o=ibm,c=us"));
        assertEquals("We expected exactly 1 entry returned.", 1, list.size());
    }
}