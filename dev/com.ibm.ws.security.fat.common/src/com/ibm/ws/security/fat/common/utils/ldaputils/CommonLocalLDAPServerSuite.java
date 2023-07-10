/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.ldap.LocalLDAPServerSuite;
import componenttest.topology.utils.LDAPUtils;

public class CommonLocalLDAPServerSuite {
    private static final Class<?> c = CommonLocalLDAPServerSuite.class;

    @BeforeClass
    public static void ldapSetUp() throws Exception {

        // We prefer to use in-memory LDAP to avoid network issues
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.startsWith("z/os")) {
            System.setProperty("fat.test.really.use.local.ldap", "true");
        }

        HashMap<String, ArrayList<String>> testServers = LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_8_NAME, LDAPUtils.LDAP_SERVER_8_SSL_PORT, true,
                LDAPUtils.LDAP_SERVER_8_BINDDN, LDAPUtils.LDAP_SERVER_8_BINDPWD,
                LDAPUtils.LDAP_SERVER_4_NAME, LDAPUtils.LDAP_SERVER_4_SSL_PORT, true,
                LDAPUtils.LDAP_SERVER_4_BINDDN, LDAPUtils.LDAP_SERVER_4_BINDPWD, null);

        Log.info(c, "setUp", "Calling LocalLDAPServerSuite.setUpUsingServers()");
        try {
            LocalLDAPServerSuite.setUpUsingServers(testServers);
        } catch (Exception e) {
            Log.info(c, "setUp", "######################################################");
            Log.info(c, "setUp", "##### Failed setting up LDAP Servers for FAT     #####");
            Log.info(c, "setUp", "##### The Failure is being logged, but the       #####");
            Log.info(c, "setUp", "##### FAT will continue - expect other failures. #####");
            Log.info(c, "setUp", "######################################################");
            Log.info(c, "setUp", e.getMessage());
        }

    }

    @AfterClass
    public static void ldapTearDown() throws InterruptedException {
        Log.info(c, "tearDown", "Calling LocalLDAPServerSuite.tearDown()");
        LocalLDAPServerSuite.tearDown();
    }
}

///*******************************************************************************
// * Copyright (c) 2014, 2022 IBM Corporation and others.
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the Eclipse Public License 2.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-2.0/
// * 
// * SPDX-License-Identifier: EPL-2.0
// *
// * Contributors:
// *     IBM Corporation - initial API and implementation
// *******************************************************************************/
////package com.ibm.ws.security.saml20.fat.commonTest;
//
//import org.junit.AfterClass;
//import org.junit.BeforeClass;
//
//import com.ibm.websphere.simplicity.log.Log;
//import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
//import com.unboundid.ldap.sdk.Entry;
//
//public class CommonLocalLDAPServerSuite {
//    private static final Class<?> thisClass = CommonLocalLDAPServerSuite.class;
//
//    private InMemoryLDAPServer ds;
//
//    private static final String LDAP_PARTITION_1_DN = "O=IBM,C=US";
//
//    private int ldapPort;
//    private int ldapSSLPort;
//
//    public int getLdapPort() {
//        return ldapPort;
//    }
//
//    public int getLdapSSLPort() {
//        Log.info(thisClass, "getLdapSSLPort", "LDAP SSL Port is not supported by this tool at this time");
//        return ldapSSLPort;
//    }
//
//    private String buildDN(String user) throws Exception {
//        return "CN=" + user + "," + LDAP_PARTITION_1_DN;
//    }
//
//    // we use the same value for the uid, sn and cn - we also use that value in the DN
//    // use this shortcut to create a new user
//    private void addUser(String uid, String password) throws Exception {
//        addUser(buildDN(uid), uid, password, uid, uid);
//
//    }
//
//    private void addUser(String dn, String uid, String password, String sn, String cn) throws Exception {
//        Log.info(thisClass, "addUser", "Adding user: " + uid + " to LDAP Registry");
//        Entry entry = new Entry(dn);
//        entry.addAttribute("objectclass", "inetorgperson");
//        entry.addAttribute("uid", uid);
//        entry.addAttribute("sn", sn);
//        entry.addAttribute("cn", cn);
//        entry.addAttribute("mail", uid);
//        entry.addAttribute("userpassword", password);
//        ds.add(entry);
//    }
//
//    /**
//     * Configure the LDAP server.
//     *
//     * @throws Exception
//     *             If the server failed to start for some reason.
//     */
//    @BeforeClass
//    public void ldapSetUp() throws Exception {
//        ldapSetUp(0); // use the default ports
//    }
//
//    public void ldapSetUp(int instance) throws Exception {
//        // Choose a port for LDAP - we ran into an instance where 8020 and 8021 were chosen
//        // This caused problems starting one of the other servers that the tests use
//        // ds = new InMemoryLDAPServer(LDAP_PARTITION_1_DN);
//        ds = new InMemoryLDAPServer(true, SAMLConstants.DEFAULT_LDAP_PORT + instance, SAMLConstants.DEFAULT_LDAP_SECURE_PORT + instance, LDAP_PARTITION_1_DN);
//
//        ldapPort = ds.getLdapPort();
//        ldapSSLPort = ds.getLdapsPort();
//        Log.info(thisClass, "ldapSetUp", "LDAP Port is: " + ldapPort);
//        Log.info(thisClass, "ldapSetUp", "LDAP SSL Port is: " + ldapSSLPort);
//
//        // override the default port values that get saved in bootstrap.properties (by LDAPUtils) - the code updating bootstrap will read the system properties
//        System.setProperty("ldap.1.port", Integer.toString(ldapPort));
//        System.setProperty("ldap.2.port", Integer.toString(ldapPort));
//        System.setProperty("ldap.3.port", Integer.toString(ldapPort));
//        System.setProperty("ldap.1.ssl.port", Integer.toString(ldapSSLPort));
//        System.setProperty("ldap.2.ssl.port", Integer.toString(ldapSSLPort));
//        System.setProperty("ldap.3.ssl.port", Integer.toString(ldapSSLPort));
//
//        /*
//         * Add the partition entries.
//         */
//        Entry entry = new Entry(LDAP_PARTITION_1_DN);
//        entry.addAttribute("objectclass", "organization");
//        entry.addAttribute("o", "ibm");
//        ds.add(entry);
//
//        /*
//         * Create the users.
//         */
//        addUser(SAMLConstants.IDP_USER_NAME, SAMLConstants.IDP_USER_PWD);
//        addUser(SAMLConstants.SAML_USER_1_NAME, SAMLConstants.SAML_USER_1_PWD);
//        addUser(SAMLConstants.SAML_USER_2_NAME, SAMLConstants.SAML_USER_2_PWD);
//        addUser(SAMLConstants.SAML_USER_3_NAME, SAMLConstants.SAML_USER_3_PWD);
//        addUser(SAMLConstants.SAML_USER_4_NAME, SAMLConstants.SAML_USER_4_PWD);
//        addUser(SAMLConstants.SAML_USER_5_NAME, SAMLConstants.SAML_USER_5_PWD);
//        //        addUser(buildDN(SAMLConstants.IDP_USER_NAME), SAMLConstants.IDP_USER_NAME, SAMLConstants.IDP_USER_PWD, SAMLConstants.IDP_USER_NAME, SAMLConstants.IDP_USER_NAME);
//        // logout test users (keep these odd names JUST in case we need to fail over to a real LDAP server)
//        addUser("john_vmmUser", "john_vmmUser");
//        addUser("ping_vmmUser", "ping_vmmUser");
//        addUser("pong_vmmUser", "pong_vmmUser");
//        addUser("connect_vmmUser", "connect_vmmUser");
//
//    }
//
//    @AfterClass
//    public void ldapTearDown() throws InterruptedException {
//        if (ds != null) {
//            try {
//                Log.info(thisClass, "ldapTearDown", "Stopping Local LDAP service");
//                ds.shutDown();
//            } catch (Exception e) {
//                Log.error(thisClass, "ldapTearDown", e, "LDAP server threw error while stopping. " + e.getMessage());
//            }
//        }
//    }
//}