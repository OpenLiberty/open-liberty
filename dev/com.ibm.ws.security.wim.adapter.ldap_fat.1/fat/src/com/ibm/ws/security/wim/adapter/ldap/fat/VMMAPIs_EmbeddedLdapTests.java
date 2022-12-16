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

import static componenttest.topology.utils.LDAPFatUtils.assertDNsEqual;
import static componenttest.topology.utils.LDAPFatUtils.updateConfigDynamically;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

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
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.wim.test.VmmServiceServletConnection;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;

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

/*
 * This test is disabled for the time being due to the cache implementation.
 * The LDAP cache implementation is a 3-layered cache that swaps the levels every 1/3 of the timeout interval.
 * The timer task it uses to do this starts running from cache instantiation and therefore when the test starts running, we cannot be sure where we are in that interval.
 * Since the test can't be sure where it is in the interval (is there enough time left to run, or not?) it can't be stable enough without making the test extraordinarily long.
 * Since we have unit tests that adequately tests this functionality, we decided to disable this test and revisit it in the future.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class VMMAPIs_EmbeddedLdapTests {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("vmm.apis.empty");
    private static final Class<?> c = VMMAPIs_EmbeddedLdapTests.class;
    private static VmmServiceServletConnection servlet;
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

    private static InMemoryLDAPServer ds;
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
        try {
            if (ds != null) {
                ds.shutDown(true);
            }
        } catch (Exception e) {
            Log.error(c, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
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

        ds = new InMemoryLDAPServer(LDAP_BASE_ENTRY);

        Entry entry = new Entry(LDAP_BASE_ENTRY);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "ibm");
        ds.add(entry);
        String userName = "blueuser1";
        String password = "password";
        String mailaddr = "bluemail5@ibm.com";
        String userDn = "uid=" + userName + "," + LDAP_BASE_ENTRY;
        entry = new Entry(userDn);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", userName);
        entry.addAttribute("sn", userName);
        entry.addAttribute("cn", userName);
        entry.addAttribute("userPassword", password);
        entry.addAttribute("mail", mailaddr);
        ds.add(entry);
        Log.info(c, methodName, "Finished LDAP server setup");

        Log.info(c, methodName, "Starting Liberty server update");
        ServerConfiguration serverConfig = serverConfiguration.clone();
        LdapRegistry ldap = new LdapRegistry();
        ldap.setRealm("LdapCustom");
        ldap.setHost("localhost");
        ldap.setPort(String.valueOf(ds.getLdapPort()));
        ldap.setBaseDN(LDAP_BASE_ENTRY);
        ldap.setBindDN(InMemoryLDAPServer.getBindDN());
        ldap.setBindPassword(InMemoryLDAPServer.getBindPassword());
        ldap.setLdapType("Custom");
        // The cache time outs are short because we need to timeout within the test. SearchCache needs to be shorter than the AttributesCache for regression testing.
        ldap.setLdapCache(new LdapCache(new AttributesCache(true, 4444, 2222, "20s"), new SearchResultsCache(true, 5555, 3333, "10s")));
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

        Thread.sleep(9100); // sleep to timeout searchCache
        Log.info(c, methodName, "Get User, refresh searchCache");
        servlet.login(userName, password);
        resultMap = servlet.getUser(userDn);
        System.out.println("Result from getUser : " + result.toString());
        assertEquals("The uid did not match", userName, resultMap.get("uid"));
        assertEquals("The mail attribute did not match", mailaddr, resultMap.get("mail"));

        Log.info(c, methodName, "Remove mail addr from Ldap");
        Attribute att = new Attribute("mail");
        Modification modification = new Modification(ModificationType.DELETE, att.getName());
        ds.modify(userDn, modification);

        Log.info(c, methodName, "Get user again, mail attribute should still be in the attributesCache");
        resultMap = servlet.getUser(userDn);
        System.out.println("Result from getUser : " + result.toString());
        assertEquals("The uid did not match", userName, resultMap.get("uid"));
        assertEquals("The mail attribute did not match", mailaddr, resultMap.get("mail"));

        Thread.sleep(18100); // now sleep to time out the attributesCache, mail should be gone
        Log.info(c, methodName, "Get user after sleep.");
        resultMap = servlet.getUser(userDn);
        System.out.println("Result from getUser : " + result.toString());
        assertEquals("The uid did not match", userName, resultMap.get("uid"));
        assertEquals("Should not get a mail attribute back: ", "null", resultMap.get("mail"));
    }

}