/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
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
import componenttest.vulnerability.LeakedPasswordChecker;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class URAPIs_ADWildCardTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.ldap.fat.ad.wild");
    private static final Class<?> c = URAPIs_ADWildCardTest.class;
    private static UserRegistryServletConnection servlet;
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

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
            server.stopServer();
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/securitylibertyinternals-1.0.mf");
        }
    }

    /**
     * Hit the test servlet to see if getRealm works.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getRealm() throws Exception {
        Log.info(c, "getRealm", "Checking expected realm");
        assertEquals("SampleLdapADRealm", servlet.getRealm());
    }

    /**
     * Hit the test servlet to see if checkPassword works when passed in a valid user
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void checkPassword() throws Exception {
        String user = "vmmtestuser";
        String password = "vmmtestuserpwd";
        Log.info(c, "checkPassword", "Checking good credentials");

        String userReturned = servlet.checkPassword(user, password);
        assertNotNull("Authentication should succeed.", userReturned);
        assertEquals("Authentication should succeed.",
                     "cn=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com", userReturned.toLowerCase());
        passwordChecker.checkForPasswordInAnyFormat(password);
    }

    /**
     * Ensure we only get 2 groups back and that we are not sending the objectclass=* from the groupMemberIdMap
     * into the Ldap query.
     * Verify we're not getting a non-Group back on the query and that our query doesn't include the objectclass=*.
     *
     * If we include objectclass=*, then we'll also get a Person back on the query based on the server.xml config.
     *
     * Issue 1261
     */
    @Test
    public void getGroupsForUser() throws Exception {
        Assume.assumeTrue(!LDAPUtils.USE_LOCAL_LDAP_SERVER);
        String user = "persona1";
        Log.info(c, "getGroupsForUser", "Checking with a valid user.");
        List<String> list = servlet.getGroupsForUser(user);
        assertNotNull("Should receive groups.", list);
        assertTrue("Should should contain CN=g1-10,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com. List: " + list,
                   list.contains("CN=g1-10,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com"));
        assertEquals("Should return 2 groups: " + list, 2, list.size());

        String msg = "Group will excluded from group membership";
        // com.ibm.ws.security.wim.adapter.ldap.LdapAdapter             3 createEntityFromLdapEntry Expected group entity. Group will excluded from group membership.
        // We should avoid hitting this case/trace by not populated with filter with an ObjectClass=* from from the groupMemberIdMap
        List<String> errMsgs = server.findStringsInLogsAndTrace(msg);
        assertTrue("Should not have found, " + msg, errMsgs.isEmpty());

        // Should not find a filter with objectclass=*
        msg = "\\(\\|\\(objectClass=group\\)\\(objectClass=\\*";
        errMsgs = server.findStringsInLogsAndTrace(msg);
        assertTrue("Should not have found, " + msg, errMsgs.isEmpty());

        // Filter should look like this
        msg = "\\(\\&\\(objectClass=group\\)\\(\\|\\(member=CN=persona1,cn=users";
        errMsgs = server.findStringsInLogsAndTrace(msg);
        assertFalse("Should have found, " + msg, errMsgs.isEmpty());

        // Should have the correct iGroupMemberIdMap with *:distinguishedName so we could theoretically match groups and persons.
        msg = "iGroupMemberIdMap\\: group:member\\;\\*\\:distinguishedName";
        errMsgs = server.findStringsInLogsAndTrace(msg);
        assertFalse("Should have found, " + msg, errMsgs.isEmpty());

    }

}