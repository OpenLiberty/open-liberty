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
package com.ibm.ws.security.saml20.fat.commonTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.unboundid.ldap.sdk.Entry;

public class CommonLocalLDAPServerSuite {
    private static final Class<?> thisClass = CommonLocalLDAPServerSuite.class;

    private InMemoryLDAPServer ds;

    private static final String LDAP_PARTITION_1_DN = "O=IBM,C=US";

    private int ldapPort;
    private int ldapSSLPort;

    public int getLdapPort() {
        return ldapPort;
    }

    public int getLdapSSLPort() {
        Log.info(thisClass, "getLdapSSLPort", "LDAP SSL Port is not supported by this tool at this time");
        return ldapSSLPort;
    }

    private String buildDN(String user) throws Exception {
        return "CN=" + user + "," + LDAP_PARTITION_1_DN;
    }

    // we use the same value for the uid, sn and cn - we also use that value in the DN
    // use this shortcut to create a new user
    private void addUser(String uid, String password) throws Exception {
        addUser(buildDN(uid), uid, password, uid, uid);

    }

    private void addUser(String dn, String uid, String password, String sn, String cn) throws Exception {
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
    @BeforeClass
    public void ldapSetUp() throws Exception {
        ds = new InMemoryLDAPServer(LDAP_PARTITION_1_DN);

        ldapPort = ds.getLdapPort();
        ldapSSLPort = ds.getLdapsPort();
        Log.info(thisClass, "ldapSetUp", "LDAP Port is: " + ldapPort);
        Log.info(thisClass, "ldapSetUp", "LDAP SSL Port is: " + ldapSSLPort);

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
        addUser(SAMLConstants.IDP_USER_NAME, SAMLConstants.IDP_USER_PWD);
        addUser(SAMLConstants.SAML_USER_1_NAME, SAMLConstants.SAML_USER_1_PWD);
        addUser(SAMLConstants.SAML_USER_2_NAME, SAMLConstants.SAML_USER_2_PWD);
        addUser(SAMLConstants.SAML_USER_3_NAME, SAMLConstants.SAML_USER_3_PWD);
        addUser(SAMLConstants.SAML_USER_4_NAME, SAMLConstants.SAML_USER_4_PWD);
        addUser(SAMLConstants.SAML_USER_5_NAME, SAMLConstants.SAML_USER_5_PWD);
        //        addUser(buildDN(SAMLConstants.IDP_USER_NAME), SAMLConstants.IDP_USER_NAME, SAMLConstants.IDP_USER_PWD, SAMLConstants.IDP_USER_NAME, SAMLConstants.IDP_USER_NAME);
        // logout test users (keep these odd names JUST in case we need to fail over to a real LDAP server)
        addUser("john_vmmUser", "john_vmmUser");
        addUser("ping_vmmUser", "ping_vmmUser");
        addUser("pong_vmmUser", "pong_vmmUser");
        addUser("connect_vmmUser", "connect_vmmUser");

    }

    @AfterClass
    public void ldapTearDown() throws InterruptedException {
        if (ds != null) {
            try {
                Log.info(thisClass, "ldapTearDown", "Stopping Local LDAP service");
                ds.shutDown();
            } catch (Exception e) {
                Log.error(thisClass, "ldapTearDown", e, "LDAP server threw error while stopping. " + e.getMessage());
            }
        }
    }
}