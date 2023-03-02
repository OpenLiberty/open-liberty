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

public class CommonRemoteLDAPServerSuite {
    private static final Class<?> c = CommonRemoteLDAPServerSuite.class;

    @BeforeClass
    public static void ldapSetUp() throws Exception {

        // force tests to use the real remote LDAP servers - we can't create the correct entries in the in-memory LDAP servers
        System.setProperty("fat.test.really.use.local.ldap", "false");

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
