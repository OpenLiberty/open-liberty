/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.ssl.fat.pkcs12;

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
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Setting LDAPUtils.USE_LOCAL_LDAP_SERVER to false to force checking of availability of physical LDAP servers");
        LDAPUtils.USE_LOCAL_LDAP_SERVER = false;

        HashMap<String, ArrayList<String>> testServers = LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_4_NAME, LDAPUtils.LDAP_SERVER_4_PORT,
                                                                                            LDAPUtils.LDAP_SERVER_5_NAME, LDAPUtils.LDAP_SERVER_5_PORT, null);
        LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_4_NAME, LDAPUtils.LDAP_SERVER_4_PORT,
                                           LDAPUtils.LDAP_SERVER_1_NAME, LDAPUtils.LDAP_SERVER_1_PORT, testServers);

        Log.info(c, "setUp", "Calling LocalLDAPServerSuite.setUpUsingServers()");
        LocalLDAPServerSuite.setUpUsingServers(testServers);
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {
        Log.info(c, "tearDown", "Calling LocalLDAPServerSuite.tearDown()");
        LocalLDAPServerSuite.tearDown();
    }
}
