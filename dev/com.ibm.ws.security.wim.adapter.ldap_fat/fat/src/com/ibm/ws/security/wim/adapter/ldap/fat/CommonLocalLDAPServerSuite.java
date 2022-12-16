/*******************************************************************************
 * Copyright (c) 2014,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.ldap.fat;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.ldap.LocalLDAPServerSuite;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
public class CommonLocalLDAPServerSuite {
    private static final Class<?> c = CommonLocalLDAPServerSuite.class;

    @BeforeClass
    public static void setUp() throws Exception {
        HashMap<String, ArrayList<String>> testServers = LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_1_NAME, LDAPUtils.LDAP_SERVER_1_PORT,
                                                                                            LDAPUtils.LDAP_SERVER_4_NAME, LDAPUtils.LDAP_SERVER_4_PORT, null);
        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_1_NAME, LDAPUtils.LDAP_SERVER_1_PORT,
                                           LDAPUtils.LDAP_SERVER_5_NAME, LDAPUtils.LDAP_SERVER_5_PORT, testServers);

        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_2_NAME, LDAPUtils.LDAP_SERVER_2_PORT,
                                           LDAPUtils.LDAP_SERVER_6_NAME, LDAPUtils.LDAP_SERVER_6_PORT, testServers);
        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_2_NAME, LDAPUtils.LDAP_SERVER_2_SSL_PORT, true, LDAPUtils.LDAP_SERVER_2_BINDDN, LDAPUtils.LDAP_SERVER_2_BINDPWD,
                                           LDAPUtils.LDAP_SERVER_6_NAME, LDAPUtils.LDAP_SERVER_6_SSL_PORT, false, null, null, testServers);

        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_4_NAME, LDAPUtils.LDAP_SERVER_4_PORT,
                                           LDAPUtils.LDAP_SERVER_1_NAME, LDAPUtils.LDAP_SERVER_1_PORT, testServers);
        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_4_NAME, LDAPUtils.LDAP_SERVER_4_PORT,
                                           LDAPUtils.LDAP_SERVER_5_NAME, LDAPUtils.LDAP_SERVER_5_PORT, testServers);
        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_4_NAME, LDAPUtils.LDAP_SERVER_4_SSL_PORT, true, LDAPUtils.LDAP_SERVER_4_BINDDN, LDAPUtils.LDAP_SERVER_4_BINDPWD,
                                           LDAPUtils.LDAP_SERVER_7_NAME, LDAPUtils.LDAP_SERVER_7_SSL_PORT, true, LDAPUtils.LDAP_SERVER_7_BINDDN, LDAPUtils.LDAP_SERVER_7_BINDPWD,
                                           testServers);
        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_4_NAME, "399",
                                           LDAPUtils.LDAP_SERVER_1_NAME, LDAPUtils.LDAP_SERVER_1_PORT, testServers);
        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_4_NAME, "399",
                                           LDAPUtils.LDAP_SERVER_4_NAME, LDAPUtils.LDAP_SERVER_4_PORT, testServers);
        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_5_NAME, LDAPUtils.LDAP_SERVER_5_PORT,
                                           LDAPUtils.LDAP_SERVER_1_NAME, LDAPUtils.LDAP_SERVER_1_PORT, testServers);
        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_5_NAME, LDAPUtils.LDAP_SERVER_5_PORT,
                                           LDAPUtils.LDAP_SERVER_1_NAME, "379", testServers);
        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_5_NAME, LDAPUtils.LDAP_SERVER_5_PORT,
                                           LDAPUtils.LDAP_SERVER_4_NAME, LDAPUtils.LDAP_SERVER_4_PORT, testServers);

        Log.info(c, "setUp", "Calling LocalLDAPServerSuite.setUpUsingServers()");
        LocalLDAPServerSuite.setUpUsingServers(testServers);
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {
        Log.info(c, "tearDown", "Calling LocalLDAPServerSuite.tearDown()");
        LocalLDAPServerSuite.tearDown();
    }
}
