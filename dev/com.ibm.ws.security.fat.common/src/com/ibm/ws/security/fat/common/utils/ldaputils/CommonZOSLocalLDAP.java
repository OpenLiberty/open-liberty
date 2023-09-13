/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.fat.common.utils.ldaputils;

import org.junit.AfterClass;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.fat.common.Constants;
import com.unboundid.ldap.sdk.Entry;

public class CommonZOSLocalLDAP {
    private static final Class<?> thisClass = CommonZOSLocalLDAP.class;

    private static InMemoryLDAPServer ds;

    private static final String LDAP_PARTITION_1_DN = "O=IBM,C=US";

    private static int ldapPort;
    private static int ldapSSLPort;

    public static int getLdapPort() {
        return ldapPort;
    }

    public int getLdapSSLPort() {
        Log.info(thisClass, "getLdapSSLPort", "LDAP SSL Port is not supported by this tool at this time");
        return ldapSSLPort;
    }

    private static String buildDN(String user) throws Exception {
        return "CN=" + user + "," + LDAP_PARTITION_1_DN;
    }

    // we use the same value for the uid, sn and cn - we also use that value in the DN
    // use this shortcut to create a new user
    private static void addUser(String uid, String password) throws Exception {
        addUser(buildDN(uid), uid, password, uid, uid);

    }

    private static void addUser(String dn, String uid, String password, String sn, String cn) throws Exception {
        Log.info(thisClass, "addUser", "Adding user: " + uid + " to LDAP Registry");
        Entry entry = new Entry(dn);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("uid", uid);
        entry.addAttribute("sn", sn);
        entry.addAttribute("cn", cn);
        entry.addAttribute("mail", uid);
        entry.addAttribute("userpassword", password);
        ds.add(entry);
    }

    /**
     * Configure the LDAP server.
     *
     * @throws Exception
     *             If the server failed to start for some reason.
     */
    public static void ldapSetUp() throws Exception {
        // Choose a port for LDAP - we ran into an instance where 8020 and 8021 were chosen
        // This caused problems starting one of the other servers that the tests use
        // ds = new InMemoryLDAPServer(LDAP_PARTITION_1_DN);
        //        ds = new InMemoryLDAPServer(true, Constants.DEFAULT_LDAP_PORT + instance, Constants.DEFAULT_LDAP_SECURE_PORT + instance, LDAP_PARTITION_1_DN);
        try {
            Log.info(thisClass, "ldapSetUp", "Setting up LDAP");
            Log.info(thisClass, "ldapSetUp", "LDAP Port " + Constants.DEFAULT_LDAP_PORT);
            Log.info(thisClass, "ldapSetUp", "LDAP Secure Port " + Constants.DEFAULT_LDAP_SECURE_PORT);
            ds = new InMemoryLDAPServer(true, Constants.DEFAULT_LDAP_PORT, Constants.DEFAULT_LDAP_SECURE_PORT, LDAP_PARTITION_1_DN);

            ldapPort = ds.getLdapPort();
            ldapSSLPort = ds.getLdapsPort();
            Log.info(thisClass, "ldapSetUp", "LDAP Port is: " + ldapPort);
            Log.info(thisClass, "ldapSetUp", "LDAP SSL Port is: " + ldapSSLPort);

            // override the default port values that get saved in bootstrap.properties (by LDAPUtils) - the code updating bootstrap will read the system properties
            System.setProperty("ldap.1.port", Integer.toString(ldapPort));
            System.setProperty("ldap.2.port", Integer.toString(ldapPort));
            System.setProperty("ldap.3.port", Integer.toString(ldapPort));
            System.setProperty("ldap.1.ssl.port", Integer.toString(ldapSSLPort));
            System.setProperty("ldap.2.ssl.port", Integer.toString(ldapSSLPort));
            System.setProperty("ldap.3.ssl.port", Integer.toString(ldapSSLPort));

            /*
             * Add the partition entries.
             */
            Entry entry = new Entry(LDAP_PARTITION_1_DN);
            entry.addAttribute("objectclass", "organization");
            entry.addAttribute("o", "ibm");
            ds.add(entry);

            /*
             * Create the users.
             */
            addUser(LDAPConstants.TEST_USER_NAME, LDAPConstants.TEST_USER_PWD);
            addUser(LDAPConstants.USER_1_NAME, LDAPConstants.USER_1_PWD);
            addUser(LDAPConstants.USER_2_NAME, LDAPConstants.USER_2_PWD);
            addUser(LDAPConstants.USER_3_NAME, LDAPConstants.USER_3_PWD);
            addUser(LDAPConstants.USER_4_NAME, LDAPConstants.USER_4_PWD);
            addUser(LDAPConstants.USER_5_NAME, LDAPConstants.USER_5_PWD);
            //        addUser(buildDN(SAMLConstants.IDP_USER_NAME), SAMLConstants.IDP_USER_NAME, SAMLConstants.IDP_USER_PWD, SAMLConstants.IDP_USER_NAME, SAMLConstants.IDP_USER_NAME);
            // logout test users (keep these odd names JUST in case we need to fail over to a real LDAP server)
            addUser("john_vmmUser", "john_vmmUser");
            addUser("ping_vmmUser", "ping_vmmUser");
            addUser("pong_vmmUser", "pong_vmmUser");
            addUser("connect_vmmUser", "connect_vmmUser");

        } catch (Exception e) {
            Log.info(thisClass, "ldapSetUp", "Exception setting up the unbounded in Memory LDAP server: " + e.getMessage());
            throw e;
        }
    }

    @AfterClass
    public static void ldapTearDown() throws InterruptedException {
        if (ds != null) {
            try {
                Log.info(thisClass, "ldapTearDown", "Stopping Local LDAP service");
                ds.shutDown();
                Log.info(thisClass, "ldapTearDown", "Local LDAP service has been stopped");
            } catch (Exception e) {
                Log.error(thisClass, "ldapTearDown", e, "LDAP server threw error while stopping. " + e.getMessage());
            }
        }
    }
}