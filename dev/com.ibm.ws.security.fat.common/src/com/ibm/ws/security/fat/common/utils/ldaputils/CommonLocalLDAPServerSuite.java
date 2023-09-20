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
                Log.info(c, "setUp", "##### Failed setting up LDAP Servers for FAT     #####");
                Log.info(c, "setUp", "##### The Failure is being logged, but the       #####");
                Log.info(c, "setUp", "##### FAT will continue - expect other failures. #####");
                Log.info(c, "setUp", "######################################################");
                Log.info(c, "setUp", e.getMessage());
            }
        } else {
            Log.info(c, "setUp", "Calling CommonZOSLocalLDAP.ldapSetUp()");
            try {
                CommonZOSLocalLDAP.ldapSetUp();
            } catch (Exception e) {
                Log.info(c, "setUp", "######################################################");
                Log.info(c, "setUp", "##### Failed setting up LDAP Servers for FAT     #####");
                Log.info(c, "setUp", "##### The Failure is being logged, but the       #####");
                Log.info(c, "setUp", "##### FAT will continue - expect other failures. #####");
                Log.info(c, "setUp", "######################################################");
                Log.info(c, "setUp", e.getMessage());
            }

        }
    }

    @AfterClass
    public static void ldapTearDown() throws Exception {
        if (useNewerLdap) {
            Log.info(c, "tearDown", "Calling LocalLDAPServerSuite.tearDown()");
            LocalLDAPServerSuite.tearDown();
        } else {
            Log.info(c, "tearDown", "Calling CommonZOSLocalLDAP.tearDown()");
            CommonZOSLocalLDAP.ldapTearDown();
        }
    }
}
