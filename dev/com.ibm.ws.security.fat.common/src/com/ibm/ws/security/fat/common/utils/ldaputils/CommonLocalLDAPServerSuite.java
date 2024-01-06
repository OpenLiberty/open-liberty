/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.JavaInfo;
import componenttest.topology.ldap.LocalLDAPServerSuite;
import componenttest.topology.utils.LDAPUtils;

public class CommonLocalLDAPServerSuite {
    private static final Class<?> c = CommonLocalLDAPServerSuite.class;
    private static boolean useNewerLdap = false;

    @BeforeClass
    public static void ldapSetUp() throws Exception {

        // We prefer to use in-memory LDAP to avoid network issues, but, the Apache LDAP servers are not running on z/OS
        // the old unbounded LDAP server works, so, we'll try to use that on z/OS - attempt to use the Apache tooling for
        // distributed and z/OS with java 17 - that path will result in distributed using Apache in memory and z/OS
        // with Java 17 using the remote servers.
        String os = System.getProperty("os.name").toLowerCase();

        if ((os.startsWith("z/os") && JavaInfo.forCurrentVM().majorVersion() >= 17)) {
            System.setProperty("fat.test.really.use.local.ldap", "false");
        } else {
            System.setProperty("fat.test.really.use.local.ldap", "true");
        }

        useNewerLdap = (!os.startsWith("z/os")) || (os.startsWith("z/os") && JavaInfo.forCurrentVM().majorVersion() >= 17);
        // non-z/OS and even z/OS with Java 17 and above - use the standard LDAP setup (in-memory for non-z/OS and remote servers with z/OS)
        // for z/OS with older versions of Java, use the old in-memory (unbounded LDAP)
        if (useNewerLdap) {
            Log.info(c, "setUp", "Calling LocalLDAPServerSuite.setUpUsingServers()");
            try {
                HashMap<String, ArrayList<String>> testServers = LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_8_NAME, LDAPUtils.LDAP_SERVER_8_SSL_PORT, true,
                        LDAPUtils.LDAP_SERVER_8_BINDDN, LDAPUtils.LDAP_SERVER_8_BINDPWD,
                        LDAPUtils.LDAP_SERVER_4_NAME, LDAPUtils.LDAP_SERVER_4_SSL_PORT, true,
                        LDAPUtils.LDAP_SERVER_4_BINDDN, LDAPUtils.LDAP_SERVER_4_BINDPWD, null);

                LocalLDAPServerSuite.setUpUsingServers(testServers);
            } catch (Exception e) {
                Log.info(c, "setUp", "######################################################");
                Log.info(c, "setUp", "##### Failed setting up LDAP Servers for FAT.    #####");
                Log.info(c, "setUp", "######################################################");
                Log.info(c, "setUp", e.getMessage());
                throw e;
            }
        } else {
            Log.info(c, "setUp", "Calling CommonZOSLocalLDAP.ldapSetUp()");
            try {
                CommonZOSLocalLDAP.ldapSetUp();
            } catch (Exception e) {
                Log.info(c, "setUp", "######################################################");
                Log.info(c, "setUp", "##### Failed setting up LDAP Servers for FAT.    #####");
                Log.info(c, "setUp", "######################################################");
                Log.info(c, "setUp", e.getMessage());
                throw e;
            }

        }
    }

    @AfterClass
    public static void ldapTearDown() throws Exception {

        Log.info(c, "ldapTearDown", "Starting LDAP tear down: " + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));
        final Object wait = new Object();
        Thread t = new Thread(
                new Runnable() {
                    // run method
                    @Override
                    public void run() {
                        // setup run
                        try {
                            if (useNewerLdap) {
                                Log.info(c, "ldapTearDown", "Calling LocalLDAPServerSuite.tearDown()");
                                LocalLDAPServerSuite.tearDown();
                            } else {
                                Log.info(c, "ldapTearDown", "Calling CommonZOSLocalLDAP.tearDown()");
                                CommonZOSLocalLDAP.ldapTearDown();
                            }

                            synchronized (wait) {
                                wait.notify();
                            }
                        } catch (Exception e) {
                            Log.info(c, "ldapTearDown", e.getMessage());
                        }
                    }
                });
        t.start();
        try {
            synchronized (wait) {
                wait.wait(120000); // give it 120 seconds
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.info(c, "ldapTearDown", "Ending LDAP tear down: " + new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()));

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