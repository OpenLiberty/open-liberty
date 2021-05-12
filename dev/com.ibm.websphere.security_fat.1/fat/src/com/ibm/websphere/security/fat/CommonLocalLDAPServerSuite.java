/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.security.fat;

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
        HashMap<String, ArrayList<String>> testServers = LocalLDAPServerSuite.addTestServer(LDAPUtils.LDAP_SERVER_4_NAME, LDAPUtils.LDAP_SERVER_4_SSL_PORT, true,
                                                                                            LDAPUtils.LDAP_SERVER_4_BINDDN, LDAPUtils.LDAP_SERVER_4_BINDPWD,
                                                                                            LDAPUtils.LDAP_SERVER_8_NAME, LDAPUtils.LDAP_SERVER_8_SSL_PORT, true,
                                                                                            LDAPUtils.LDAP_SERVER_8_BINDDN, LDAPUtils.LDAP_SERVER_8_BINDPWD, null);

        Log.info(c, "setUp", "Calling LocalLDAPServerSuite.setUpUsingServers()");
        LocalLDAPServerSuite.setUpUsingServers(testServers);
    }

    @AfterClass
    public static void tearDown() throws InterruptedException {
        Log.info(c, "tearDown", "Calling LocalLDAPServerSuite.tearDown()");
        LocalLDAPServerSuite.tearDown();
    }
}
