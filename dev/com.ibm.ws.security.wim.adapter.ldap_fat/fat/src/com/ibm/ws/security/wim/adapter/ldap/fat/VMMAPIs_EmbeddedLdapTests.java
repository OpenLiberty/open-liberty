/*******************************************************************************
 * Copyright (c) 20148 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import static componenttest.topology.utils.LDAPFatUtils.assertDNsEqual;
import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.name.Dn;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.wim.AttributesCache;
import com.ibm.websphere.simplicity.config.wim.LdapCache;
import com.ibm.websphere.simplicity.config.wim.LdapRegistry;
import com.ibm.websphere.simplicity.config.wim.SearchResultsCache;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.security.wim.test.VmmServiceServletConnection;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPFatUtils;
import componenttest.topology.utils.LDAPUtils;
import componenttest.vulnerability.LeakedPasswordChecker;

/**
 * A general VMMService FAT test with an embedded Apache DS.
 * Server starts with mostly empty server.xml
 * Add/remove custom embedded ldapserver and libertyserver config.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class VMMAPIs_EmbeddedLdapTests {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("vmm.apis.empty");
    private static final Class<?> c = VMMAPIs_EmbeddedLdapTests.class;
    private static VmmServiceServletConnection servlet;
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

    private static EmbeddedApacheDS ldapServer = null;
    private static final String LDAP_BASE_ENTRY = "o=ibm,c=us";

    private static ServerConfiguration serverConfiguration = null;

    /**
     * Updates the sample, which is expected to be at the hard-coded path.
     * If this test is failing, check this path is correct.
     */
    @BeforeClass
    public static void setUp() throws Exception {
        // Add LDAP variables to bootstrap properties file
        LDAPUtils.addLDAPVariables(server);
        Log.info(c, "setUp", "Starting the server... (will wait for vmmapi servlet to start)");
        server.copyFileToLibertyInstallRoot("lib/features", "internalfeatures/vmmapi-1.0.mf");
        server.addInstalledAppForValidation("vmmService");
        server.startServer(c.getName() + ".log");

        //Make sure the application has come up before proceeding
        assertNotNull("Application vmmService does not appear to have started.",
                      server.waitForStringInLog("CWWKZ0001I:.*vmmService"));
        assertNotNull("Security service did not report it was ready",
                      server.waitForStringInLog("CWWKS0008I"));
        assertNotNull("Server did not came up",
                      server.waitForStringInLog("CWWKF0011I"));

        Log.info(c, "setUp", "Creating servlet connection the server");

        servlet = new VmmServiceServletConnection(server.getHostname(), server.getHttpDefaultPort());

        serverConfiguration = server.getServerConfiguration();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Log.info(c, "tearDown", "Stopping the server...");
        try {
            server.stopServer();
        } finally {
            server.deleteFileFromLibertyInstallRoot("lib/features/internalfeatures/vmmapi-1.0.mf");

        }

    }

    @After
    public void tearDownLdapserver() throws Exception {

        Log.info(c, "tearDown", "Stopping the ldap server...");
        if (ldapServer != null) {
            ldapServer.stopService();
        }
    }

    /**
     * Set up an Ldap server with very short cache times (searchCache < attributesCache)
     *
     * Access user with mail attribute.
     * Wait for search cache timeout.
     * Access user again
     * Remove mail attribute from user
     * Access user (will still get mail attribute)
     * Sleep for attributesCache - searchCache
     * Access user, should not get mail attribute.
     *
     * Written for OL Issues 5064 and 5399
     */
    @Test
    public void getUserRemoveAttribute() throws Exception {
        final String methodName = "getUserRemoveAttribute";
        Log.info(c, methodName, "Starting LDAP server setup");
        ldapServer = new EmbeddedApacheDS("getUserRemoveAttribute");
        ldapServer.addPartition("testing", LDAP_BASE_ENTRY);
        ldapServer.startServer();
        Entry entry = ldapServer.newEntry(LDAP_BASE_ENTRY);
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        ldapServer.add(entry);
        String userName = "blueuser1";
        String password = "password";
        String mailaddr = "bluemail5@ibm.com";
        String userDn = "uid=" + userName + "," + LDAP_BASE_ENTRY;
        entry = ldapServer.newEntry(userDn);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", userName);
        entry.add("sn", userName);
        entry.add("cn", userName);
        entry.add("userPassword", password);
        entry.add("mail", mailaddr);
        ldapServer.add(entry);
        Log.info(c, methodName, "Finished LDAP server setup");

        Log.info(c, methodName, "Starting Liberty server update");
        ServerConfiguration serverConfig = serverConfiguration.clone();
        LdapRegistry ldap = new LdapRegistry();
        ldap.setRealm("LdapCustom");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ldapServer.getLdapServer().getPort()));
        ldap.setBaseDN(LDAP_BASE_ENTRY);
        ldap.setBindDN(EmbeddedApacheDS.getBindDN());
        ldap.setBindPassword(EmbeddedApacheDS.getBindPassword());
        ldap.setLdapType("Custom");
        // The cache time outs are short because we need to timeout within the test. SearchCache needs to be shorter than the AttributesCache for regression testing.
        ldap.setLdapCache(new LdapCache(new AttributesCache(true, 4444, 2222, "7s"), new SearchResultsCache(true, 5555, 3333, "5s")));
        serverConfig.getLdapRegistries().add(ldap);
        LDAPFatUtils.createFederatedRepository(serverConfig, "LDAPRealmAttr", new String[] { LDAP_BASE_ENTRY });
        updateConfigDynamically(server, serverConfig);
        Log.info(c, methodName, "Finished Liberty server update");

        Log.info(c, methodName, "Login");
        String result = servlet.login(userName, password);
        System.out.println("Result from login : " + result.toString());
        assertDNsEqual("Returned uniqueName should be same ", userDn, result);

        Log.info(c, methodName, "Get User");
        Map resultMap = servlet.getUser(userDn);
        System.out.println("Result from getUser : " + result.toString());
        assertEquals("The uid did not match", userName, resultMap.get("uid"));
        assertEquals("The mail attribute did not match", mailaddr, resultMap.get("mail"));

        Thread.sleep(2100); // sleep to timeout searchCache
        Log.info(c, methodName, "Get User, refresh searchCache");
        servlet.login(userName, password);
        resultMap = servlet.getUser(userDn);
        System.out.println("Result from getUser : " + result.toString());
        assertEquals("The uid did not match", userName, resultMap.get("uid"));
        assertEquals("The mail attribute did not match", mailaddr, resultMap.get("mail"));

        Log.info(c, methodName, "Remove mail addr from Ldap");
        Attribute att = new DefaultAttribute("mail");
        Modification modification = new DefaultModification(ModificationOperation.REMOVE_ATTRIBUTE, att);
        ldapServer.modify(new Dn(userDn), modification);

        Log.info(c, methodName, "Get user again, mail attribute should still be in the attributesCache");
        resultMap = servlet.getUser(userDn);
        System.out.println("Result from getUser : " + result.toString());
        assertEquals("The uid did not match", userName, resultMap.get("uid"));
        assertEquals("The mail attribute did not match", mailaddr, resultMap.get("mail"));

        Thread.sleep(4100); // now sleep to time out the attributesCache, mail should be gone
        Log.info(c, methodName, "Get user after sleep.");
        resultMap = servlet.getUser(userDn);
        System.out.println("Result from getUser : " + result.toString());
        assertEquals("The uid did not match", userName, resultMap.get("uid"));
        assertEquals("Should not get a mail attribute back: ", "null", resultMap.get("mail"));
    }

}