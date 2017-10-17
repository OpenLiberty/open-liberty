/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import java.rmi.RemoteException;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.test.VmmServiceServletConnection;
import com.ibm.wsspi.security.wim.exception.WIMException;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.LDAPUtils;
import componenttest.vulnerability.LeakedPasswordChecker;

@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class VMMAPIs_TDSLDAPTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("vmm.apis.tds.ldap");
    private static final Class<?> c = VMMAPIs_TDSLDAPTest.class;
    private static VmmServiceServletConnection servlet;
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

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

        Thread.sleep(5000);
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

    /**
     * Hit the test servlet to see if getUser works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUser() throws Exception {
        String uniqueName = "cn=vmmtestuser,o=ibm,c=us";
        Log.info(c, "getUser", "Checking with a valid user.");
        Map result = servlet.getUser(uniqueName);
        System.out.println("Result from getUser : " + result.toString());

        assertTrue(result.get("uid").equals("vmmtestuser"));
        assertTrue(result.get("cn").equals("vmmtestuser"));
        assertTrue(result.get("sn").equals("vmmtestusersn"));
        assertTrue(result.get("mail").equals("vmmtestuser@ibm.com"));
        assertTrue(result.get("telephoneNumber").equals("[1 919 555 5555]")); // telephoneNumber is multivalues property so its will return list
        assertTrue(result.get("photoURL").equals("1 919 555 5555")); // photoURL property is mapped to telephoneNumber attribute
        assertTrue(result.get("photoURLThumbnail").equals("vmmtestuser@ibm.com")); // photoURLThumbnail property is mapped to mail attribute
    }

    /**
     * Hit the test servlet to see if getUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void getUserWithInvalidUser() {
        String uniqueName = "cn=invalid,o=ibm,c=us";
        Log.info(c, "getUserWithInvalidUser", "Checking with a invalid user.");
        try {
            servlet.getUser(uniqueName);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (WIMException e) {
            e.printStackTrace();
        }
        server.waitForStringInLog("CWIML4527E", 500);
        assertTrue("An invalid user should cause EntityNotFoundException ", true);
    }

    /**
     * Hit the test servlet to see if getUser works when supplied with a invalid user.
     * This verifies the various required bundles got installed and are working.
     */
    /*
     * @Test
     * public void getUserWithEntryOutsideBaseEntry() {
     * String uniqueName = "cn=vmmtestuser,o=ibm";
     * Log.info(c, "getUserWithInvaidUser", "Checking with a invalid user.");
     * try {
     * servlet.getUser(uniqueName);
     * } catch (RemoteException e) {
     * e.printStackTrace();
     * } catch (WIMException e) {
     * e.printStackTrace();
     * }
     * server.waitForStringInLog("CWIML0515E", 500);
     * assertTrue("An invalid group should cause EntryNotFoundException", true);
     * }
     */

    /**
     * Hit the test servlet to see if login works when supplied with a valid user.
     * This verifies the various required bundles got installed and are working.
     */
    @Test
    public void login() throws Exception {
        String userName = "vmmtestuser";
        String password = "vmmtestuserpwd";
        Log.info(c, "getUser", "Checking with a valid user.");
        String result = servlet.login(userName, password);
        System.out.println("Result from login : " + result.toString());

        equalDNs("Returned uniqueName should be same ", "cn=vmmtestuser,o=ibm,c=us", result);
    }

    private void equalDNs(String msg, String dn1, String dn2) throws InvalidNameException {
        LdapName ln1 = new LdapName(dn1);
        LdapName ln2 = new LdapName(dn2);
        assertEquals(msg, ln1, ln2);
    }
}