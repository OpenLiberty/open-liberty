/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder;

import org.junit.AfterClass;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;
import com.unboundid.ldap.sdk.Entry;

import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run basic JWT Builder tests with LDAP.
 *
 **/

public class JwtBuilderCommonLDAPFat extends CommonSecurityFat {

    protected static ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();

    protected static InMemoryLDAPServer ds;
    private static final String BASE_DN = "o=ibm,c=us";
    protected static final String USER = "aTestUser";
    private static final String USER_DN = "uid=" + USER + "," + BASE_DN;

    @AfterClass
    public static void commonAfterClass() throws Exception {
        Log.info(thisClass, "commonAfterClass", " from JwtBuilderAPILDAP");
        if (ds != null) {
            try {
                ds.shutDown(true);
            } catch (Exception e) {
                Log.error(thisClass, "teardown", e, "LDAP server threw error while shutting down. " + e.getMessage());
            }
        }
        CommonSecurityFat.commonAfterClass();
    }

    public static void setupLdapServer(LibertyServer builderServer) throws Exception {
        initLdapServer();
        int ldapPort = ds.getListenPort();
        bootstrapUtils.writeBootstrapProperty(builderServer, "MY_LDAP_PORT", Integer.toString(ldapPort));

    }

    public static void initLdapServer() throws Exception {
        ds = new InMemoryLDAPServer(BASE_DN);

        /*
         * Add the partition entries.
         */
        Entry entry = new Entry(BASE_DN);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "ibm");
        ds.add(entry);

        /*
         * Create the user.
         */
        entry = new Entry(USER_DN);
        entry.addAttribute("objectclass", "wiminetorgperson");
        entry.addAttribute("uid", USER);
        entry.addAttribute("sn", USER);
        entry.addAttribute("cn", USER);
        entry.addAttribute("homeStreet", "Burnet");
        entry.addAttribute("nickName", USER + " nick name");
        entry.addAttribute("userPassword", "testuserpwd");
        ds.add(entry);
    }

}
