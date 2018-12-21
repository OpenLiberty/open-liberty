/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.naming.ldap.LdapName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.LdapFilters;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.apacheds.PopulateDefaultLdapConfig;
import com.ibm.ws.security.registry.test.UserRegistryServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;
import componenttest.vulnerability.LeakedPasswordChecker;

/**
 * The purpose of this class is to ensure an LDAP user registry can be
 * configured in the server.xml without specifying an id.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class FATTestADNoId {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.registry.ldap.fat.ad.noId");
    private static final Class<?> c = FATTestADNoId.class;
    private static UserRegistryServletConnection servlet;
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

    private static ServerConfiguration serverConfiguration = null;
    private static EmbeddedApacheDS ldapServer = null;

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

        serverConfiguration = server.getServerConfiguration();
        ldapServer = PopulateDefaultLdapConfig.setupLdapServerAD(ldapServer, c.getSimpleName());
        assertNotNull("Failed to setup EmbeddedApacheDS server", ldapServer);
        updateLibertyServer();

        Log.info(c, "setUp", "Creating servlet connection the server");
        servlet = new UserRegistryServletConnection(server.getHostname(), server.getHttpDefaultPort());

        if (servlet.getRealm() == null) {
            Thread.sleep(5000);
            servlet.getRealm();
        }

    }

    /**
     * Update the Liberty server with the correct LDAP info for an embedded ApacheDS
     *
     * @throws Exception If the update failed for some reason.
     */
    private static void updateLibertyServer() throws Exception {
        final String methodName = "updateLibertyServer";
        Log.info(c, methodName, "Starting Liberty server update to embedded ApacheDS");

        ServerConfiguration serverConfig = serverConfiguration.clone();

        boolean foundLdapToUpdate = false;

        String ldapID = "LDAP";

        for (LdapRegistry ldap : serverConfig.getLdapRegistries()) {
            // from "com.ibm.ws.security.registry.ldap.fat.ad.noid"
            ldap.setLdapType("Custom");
            ldap.setHost("localhost");
            ldap.setPort(String.valueOf(ldapServer.getLdapServer().getPort()));
            ldap.setBindDN(EmbeddedApacheDS.getBindDN());
            ldap.setBindPassword(EmbeddedApacheDS.getBindPassword());

            LdapFilters filter = serverConfig.getActivedLdapFilterProperties().get(0);

            assertNotNull("Should have a filter to convert", filter);

            ldap.setCustomFilters(new LdapFilters(filter.getUserFilter(), filter.getGroupFilter(), filter.getUserIdMap(), filter.getGroupIdMap(), filter
                            .getGroupMemberIdMap()));

            serverConfig.getActivedLdapFilterProperties().clear();

            foundLdapToUpdate = true;

        }

        assertTrue("Did not find an LDAP id to match " + ldapID, foundLdapToUpdate);

        updateConfigDynamically(server, serverConfig);

        Log.info(c, methodName, "Finished Liberty server update");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        try {
            server.stopServer();
        } finally {
            if (ldapServer != null) {
                ldapServer.stopService();
            }

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
    public void checkPasswordWithGoodCredentials() throws Exception {
        String user = "vmmtestuser";
        String password = "vmmtestuserpwd";
        Log.info(c, "checkPasswordWithGoodCredentials", "Checking good credentials");
        assertEquals("Authentication should succeed.",
                     new LdapName("CN=vmmtestuser,cn=users,dc=secfvt2,dc=austin,dc=ibm,dc=com"), new LdapName(servlet.checkPassword(user, password)));
        passwordChecker.checkForPasswordInAnyFormat(password);
    }
}