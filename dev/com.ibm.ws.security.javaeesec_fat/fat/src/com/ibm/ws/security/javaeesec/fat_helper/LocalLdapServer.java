/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat_helper;

import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.unboundid.ldap.sdk.Entry;

public class LocalLdapServer {

    private InMemoryLDAPServer ldapServer = null;

    public static final String BASE_DN = "o=ibm,c=us";
    public static final String USERS = "users";
    public static final String ANOTHERUSERS = "anotherusers";
    public static final String GROUPS = "groups";
    public static final String ANOTHERGROUPS = "anothergroups";
    public static final String ADMINUSER = "admin";
    public static final String USER1 = "user1";
    public static final String USER2 = "user2";
    public static final String USER3 = "user3";
    public static final String RUNASUSER1 = "runasuser1";
    public static final String INVALIDUSER = "invalidUSER";
    public static final String ANOTHERUSER1 = "anotheruser1";
    public static final String ANOTHERUSER2 = "anotheruser2";
    public static final String ANOTHERRUNASUSER1 = "anotherrunasuser1";
    public static final String CERTUSER1 = "certuser1";
    public static final String CERTUSER2 = "certuser2";
    public static final String PASSWORD = "s3cur1ty";
    public static final String ANOTHERPASSWORD = "an0thers3cur1ty";
    public static final String INVALIDPASSWORD = "invalid";
    public static final String GROUP1 = "group1";
    public static final String GROUP2 = "group2";
    public static final String RUNASGROUP1 = "runasgroup1";
    public static final String ANOTHERGROUP1 = "anothergroup1";
    public static final String ANOTHERGROUP2 = "anothergroup2";
    public static final String ANOTHERRUNASGROUP1 = "anotherrunasgroup1";
    public static final String GRANTEDGROUP = "grantedgroup"; // this group is allowed to access the servlet. this group is added if user id contains "user"
    public static final String GRANTEDGROUP2 = "grantedgroup2";
    public static final String CERTGROUP1 = "certgroup1";

    public void start() throws Exception {
        ldapServer = new InMemoryLDAPServer(false, Integer.getInteger("ldap.1.port", 10389), 0, BASE_DN);

        /*
         * Create the base entry.
         */
        Entry entry = new Entry(BASE_DN);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "ibm");
        ldapServer.add(entry);

        // create ou for users and groups.
        String[] CREATE_OUS = { USERS, ANOTHERUSERS, GROUPS, ANOTHERGROUPS };
        for (String ou : CREATE_OUS) {
            addOU(BASE_DN, ou);
        }

        String USERS_BASE = "ou=" + USERS + "," + BASE_DN;
        addUser(USERS_BASE, ADMINUSER, PASSWORD);
        addUser(USERS_BASE, USER1, PASSWORD);
        addUser(USERS_BASE, USER2, PASSWORD);
        addUser(USERS_BASE, USER3, PASSWORD);
        addUser(USERS_BASE, INVALIDUSER, PASSWORD);
        addUser(USERS_BASE, RUNASUSER1, PASSWORD);
        addUser(USERS_BASE, CERTUSER1, PASSWORD);
        addUser(USERS_BASE, CERTUSER2, PASSWORD);

        String GROUPS_BASE = "ou=" + GROUPS + "," + BASE_DN;
        String[] GROUP1_MEMBERS = { "uid=" + USER1 + "," + USERS_BASE, "uid=" + USER2 + "," + USERS_BASE };
        addGroup(GROUPS_BASE, GROUP1, GROUP1_MEMBERS);
        String[] GROUP2_MEMBERS = { "uid=" + USER2 + "," + USERS_BASE };
        addGroup(GROUPS_BASE, GROUP2, GROUP2_MEMBERS);
        String[] RUNASGROUP1_MEMBERS = { "uid=" + RUNASUSER1 + "," + USERS_BASE };
        addGroup(GROUPS_BASE, RUNASGROUP1, RUNASGROUP1_MEMBERS);
        String[] CERTGROUP1_MEMBERS ={"uid=" + CERTUSER1 + "," + USERS_BASE, "uid=" + USER3 + "," + USERS_BASE,};
        addGroup(GROUPS_BASE, CERTGROUP1, CERTGROUP1_MEMBERS);

        String ANOTHERUSERS_BASE = "ou=" + ANOTHERUSERS + "," + BASE_DN;
        addUser(ANOTHERUSERS_BASE, ADMINUSER, ANOTHERPASSWORD);
        addUser(ANOTHERUSERS_BASE, USER1, ANOTHERPASSWORD);
        addUser(ANOTHERUSERS_BASE, USER2, ANOTHERPASSWORD);
        addUser(ANOTHERUSERS_BASE, ANOTHERUSER1, ANOTHERPASSWORD);
        addUser(ANOTHERUSERS_BASE, INVALIDUSER, ANOTHERPASSWORD);
        addUser(ANOTHERUSERS_BASE, ANOTHERRUNASUSER1, ANOTHERPASSWORD);

        String ANOTHERGROUPS_BASE = "ou=" + ANOTHERGROUPS + "," + BASE_DN;
        String[] ANOTHERGROUP1_MEMBERS = { "uid=" + USER1 + "," + ANOTHERUSERS_BASE, "uid=" + USER2 + "," + ANOTHERUSERS_BASE, "uid=" + ANOTHERUSER1 + "," + ANOTHERUSERS_BASE,
                                           "uid=" + ANOTHERUSER2 + "," + ANOTHERUSERS_BASE };
        addGroup(ANOTHERGROUPS_BASE, ANOTHERGROUP1, ANOTHERGROUP1_MEMBERS);
        String[] ANOTHERGROUP2_MEMBERS = { "uid=" + USER2 + "," + ANOTHERUSERS_BASE };
        addGroup(ANOTHERGROUPS_BASE, ANOTHERGROUP2, ANOTHERGROUP2_MEMBERS);
        String[] ANOTHERRUNASGROUP1_MEMBERS = { "uid=" + ANOTHERRUNASUSER1 + "," + ANOTHERUSERS_BASE };
        addGroup(ANOTHERGROUPS_BASE, ANOTHERRUNASGROUP1, ANOTHERRUNASGROUP1_MEMBERS);

    }

    public void stop() throws Exception {
        if (ldapServer != null) {
            ldapServer.shutDown();
        }
    }

    private void addOU(String base, String ou) throws Exception {
        Entry entry = new Entry("ou=" + ou + "," + base);
        entry.addAttribute("objectclass", "organizationalunit");
        entry.addAttribute("ou", ou);
        ldapServer.add(entry);
    }

    private void addGroup(String base, String cn, String[] members) throws Exception {
        Entry entry = new Entry("cn=" + cn + "," + base);
        entry.addAttribute("objectclass", "groupOfNames");
        for (String member : members) {
            entry.addAttribute("member", member);
        }
        ldapServer.add(entry);
    }

    private void addUser(String base, String uid, String password) throws Exception {
        Entry entry = new Entry("uid=" + uid + "," + base);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("objectclass", "person");
        entry.addAttribute("uid", uid);
        entry.addAttribute("sn", uid + "_sn");
        entry.addAttribute("cn", uid = "_cn");
        entry.addAttribute("userPassword", password);
        ldapServer.add(entry);
    }
}
