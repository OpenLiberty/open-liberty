/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.apacheds;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Starts and populates an LDAP server locally, populates the LDAP so it matches the config on the remote LDAP servers.
 *
 * As existing tests are converted to use this, they should not try to run remotely.
 *
 * This configuration works on z/OS.
 *
 * The addUsers methods were created by running the migrate methods. The migrate methods are being left so there is a
 * history in case they need to be used again.
 *
 * There are some AD configurations that are not supported in ApacheDS, tests that depend on a specific AD config will
 * still need to run to a remote Ldap.
 */
public class PopulateDefaultLdapConfig {
    private static final Class<?> c = PopulateDefaultLdapConfig.class;

    private static final String LDAP_AD_BASE_ENTRY = "DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM";
    private static final String LDAP_AD_USERS_ENTRY = "cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM";

    private static final String LDAP_TDS_BASE_ENTRY = "o=ibm,c=us";
    private static final String LDAP_TDS_GROUPS_ENTRY = "ou=iGroups," + LDAP_TDS_BASE_ENTRY;
    private static final String LDAP_TDS_USERS_ENTRY = "ou=iUsers," + LDAP_TDS_BASE_ENTRY;
    private static final String LDAP_TDS_JUSERS_ENTRY = "ou=jUsers," + LDAP_TDS_BASE_ENTRY;

    private static final String LDIF_PROJECT = "com.ibm.ws.org.apache.directory.server//";

    private static final String AD_LDIF = "AD.ldif";

    private static final String TDS_LDIF = "TDS.ldif";

    /**
     * Configure an Active Directory (AD) like LDAP server.
     *
     * @throws Exception If the server failed to start for some reason.
     */
    public static EmbeddedApacheDS setupLdapServerAD(EmbeddedApacheDS ldapServer, String name) throws Exception {
        return setupLdapServerAD(ldapServer, name, false);
    }

    public static EmbeddedApacheDS setupLdapServerAD(EmbeddedApacheDS ldapServer, String name, boolean migrateFromFile) throws Exception {
        final String methodName = "setupLdapServerAD";
        Log.info(c, methodName, "Starting LDAP server setup");

        ldapServer = new EmbeddedApacheDS(name);
        ldapServer.addPresetLdapAttributes();
        ldapServer.addPartition(name, LDAP_AD_BASE_ENTRY);
        ldapServer.startServer();

        Entry e = ldapServer.newEntry(LDAP_AD_BASE_ENTRY);
        e.add("objectclass", "domain");
        e.add("objectclass", "top");
        e.add("DC", "IBM");
        e.add("DC", "AUSTIN");
        e.add("DC", "SECFVT2");
        ldapServer.add(e);

        e = ldapServer.newEntry(LDAP_AD_USERS_ENTRY);
        e.add("objectclass", "container");
        e.add("objectclass", "top");
        e.add("cn", "Users");
        ldapServer.add(e);

        if (migrateFromFile) {
            migrateFromFileAD(ldapServer);
        } else {
            addUsersAD(ldapServer);
        }

        Log.info(c, methodName, "Finished LDAP server setup");
        return ldapServer;
    }

    private static void migrateFromFileAD(EmbeddedApacheDS ldapServer) throws Exception {
        final String methodName = "migrateFromFileAD";
        java.io.File f = new java.io.File("..//..//..//..//" + LDIF_PROJECT + AD_LDIF);
        if (!f.exists()) {
            throw new IllegalStateException("Could not find " + f.getName()
                                            + ". Current location is " + f.getAbsolutePath());
        }

        java.io.File mig = new java.io.File("migrated.txt");

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), Charset.forName("UTF8")));

            FileWriter fileWriter = new FileWriter(mig);
            PrintWriter w = new PrintWriter(fileWriter);
            try {
                String line;
                String dn = null;
                List<String> attr = null;

                boolean saveDNLine = true;
                line = br.readLine();

                while (saveDNLine || (line = br.readLine()) != null) {
                    saveDNLine = false;
                    if (line.startsWith("#") || line.trim().equals("")) {
                        continue;
                    }

                    String key = null;
                    String value = null;

                    String workingDN = null;

                    if (line.startsWith("dn")) { // fresh entry
                        //System.out.println("Process new entry " + line);
                        String[] propDN = line.split(": ");
                        if (propDN.length != 2) {
                            throw new IllegalStateException("Failed to process line: " + line);

                        }

                        if (propDN[1].equalsIgnoreCase(LDAP_AD_BASE_ENTRY) || propDN[1].equalsIgnoreCase(LDAP_AD_USERS_ENTRY)) { // already added these entries
                            continue;
                        }
                        workingDN = propDN[1];

                        Entry entry = ldapServer.newEntry(propDN[1]);
                        w.println("entry = ldapServer.newEntry(\"" + propDN[1] + "\");");

                        boolean saveLine = true;

                        line = br.readLine();
                        while (saveLine || (line = br.readLine()) != null) {
                            saveLine = false;
                            if (line.startsWith("#") || line.trim().equals("")) {
                                continue;
                            }

                            if (line.startsWith("dn")) { // fresh entry
                                saveDNLine = true;
                                break;
                            } else if (line.startsWith("objectCategory")) { // not supported on ApacheDS
                                continue;
                            } else if (line.startsWith("distinguishedName")) { // some dn's run to the next line.
                                //Log.info(c, methodName, "Special process on distinguishedName");
                                String[] prop = line.split(": ");
                                key = prop[0];
                                value = prop[1];
                                line = br.readLine();

                                if (line == null || line.trim().equals("")) {
                                    // nothing to process
                                } else if (line.startsWith("uid")) {
                                    Log.info(c, methodName, "uid " + line);
                                    saveLine = true;
                                } else { // the password wrapped to the next line
                                    value = value + line.trim();
                                }

                            } else if (line.startsWith("userPassword")) {
                                //Log.info(c, methodName, "Special process on userPassword");
                                String[] prop = line.split(":: ");
                                key = prop[0];
                                value = prop[1];
                            } else {
                                String[] prop = line.split(":: ");
                                if (prop.length != 2) {
                                    prop = line.split(": ");
                                    if (prop.length != 2) {
                                        Log.info(c, methodName, "Failed to process line: " + line);
                                    } else {
                                        key = prop[0];
                                        value = prop[1];
                                    }
                                } else {
                                    key = prop[0];
                                    value = prop[1];
                                }
                            }

                            if (key == null || value == null) {
                                Log.info(c, methodName, "nothing to process");
                            } else {
                                Log.info(c, methodName, "Adding " + key + " " + value);

                                if (key.equalsIgnoreCase("objectGUID")) {
                                    Log.info(c, methodName, "Skipping " + key);
                                    // entry.add(key, new BinaryValue(value.getBytes()));
                                } else {
                                    entry.add(key, value);
                                    w.println("entry.add(\"" + key + "\", \"" + value + "\");");
                                }
                            }

                        }
                        ldapServer.add(entry);

                        w.println("ldapServer.add(entry);");
                        w.println();
                        Log.info(c, methodName, "Lookup " + ldapServer.lookup(workingDN));

                    }
                    if (line == null) {
                        break;
                    }
                }

            } finally {
                br.close();
                w.close();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Error processing LDIF");
        }

    }

    public static EmbeddedApacheDS setupLdapServerTDS(EmbeddedApacheDS ldapServer, String name) throws Exception {
        return setupLdapServerTDS(ldapServer, name, false);
    }

    public static EmbeddedApacheDS setupLdapServerTDS(EmbeddedApacheDS ldapServer, String name, boolean migrateFromFile) throws Exception {
        final String methodName = "setupLdapServerTDS";
        Log.info(c, methodName, "Starting LDAP server setup");

        ldapServer = new EmbeddedApacheDS(name);
        ldapServer.addPresetLdapAttributes();
        ldapServer.addPartition(name, LDAP_TDS_BASE_ENTRY);
        Log.info(c, methodName, "Starting LDAP server");
        ldapServer.startServer();
        Log.info(c, methodName, "Started LDAP server");

        Entry e = ldapServer.newEntry(LDAP_TDS_BASE_ENTRY);
        e.add("objectclass", "organization");
        e.add("o", "ibm");
        ldapServer.add(e);

        e = ldapServer.newEntry(LDAP_TDS_GROUPS_ENTRY);
        e.add("objectclass", "organizationalunit");
        e.add("ou", "iGroups");
        ldapServer.add(e);

        e = ldapServer.newEntry(LDAP_TDS_USERS_ENTRY);
        e.add("objectclass", "organizationalunit");
        e.add("ou", "iUsers");
        ldapServer.add(e);

        e = ldapServer.newEntry(LDAP_TDS_JUSERS_ENTRY);
        e.add("objectclass", "organizationalunit");
        e.add("ou", "jUsers");
        ldapServer.add(e);

        if (migrateFromFile) {
            migrateFromFileTDS(ldapServer);
        } else {
            addUsersTDS(ldapServer);
        }

        Log.info(c, methodName, "Finished LDAP server setup");
        return ldapServer;
    }

    private static void migrateFromFileTDS(EmbeddedApacheDS ldapServer) throws Exception {
        final String methodName = "migrateFromFileTDS";

        java.io.File f = new java.io.File("..//..//..//..//" + LDIF_PROJECT + TDS_LDIF);
        Log.info(c, methodName, "Pull ldap in from file " + f);

        java.io.File mig = new java.io.File("migrated.txt");
        if (!f.exists()) {
            throw new IllegalStateException("Could not find " + f.getName()
                                            + ". Current location is " + f.getAbsolutePath());
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), Charset.forName("UTF8")));

            FileWriter fileWriter = new FileWriter(mig);
            PrintWriter w = new PrintWriter(fileWriter);
            try {
                String line;

                boolean saveDNLine = true;
                line = br.readLine();

                while (saveDNLine || (line = br.readLine()) != null) {
                    saveDNLine = false;

                    if (line.startsWith("#") || line.trim().equals("")) {
                        continue;
                    }

                    String key = null;
                    String value = null;

                    String workingDN = null;

                    if (line.startsWith("dn")) { // fresh entry
                        //Log.info(c, methodName, "Process new entry " + line);

                        String[] propDN = line.split(": ");
                        if (propDN.length != 2) {
                            throw new IllegalStateException("Failed to process line: " + line);

                        }

                        if (propDN[1].equalsIgnoreCase(LDAP_TDS_BASE_ENTRY) || propDN[1].equalsIgnoreCase(LDAP_TDS_GROUPS_ENTRY) || propDN[1].equalsIgnoreCase(LDAP_TDS_USERS_ENTRY)
                            || propDN[1].equalsIgnoreCase(LDAP_TDS_JUSERS_ENTRY)) { // already added these entries
                            continue;
                        }
                        workingDN = propDN[1];
                        Entry entry = ldapServer.newEntry(propDN[1]);
                        w.println("entry = ldapServer.newEntry(\"" + propDN[1] + "\");");
                        boolean saveLine = true;
                        line = br.readLine();
                        while (saveLine || (line = br.readLine()) != null) {
                            saveLine = false;
                            if (line.startsWith("#") || line.trim().equals("")) {
                                continue;
                            }

                            if (line.startsWith("dn")) { // fresh entry
                                saveDNLine = true;
                                break;
                            } else if (line.startsWith("userPassword")) {
                                Log.info(c, methodName, "Special process on userPassword");
                                String[] prop = line.split(":: ");

                                key = prop[0];
                                value = prop[1];

                                line = br.readLine();
                                if (line == null || line.trim().equals("")) {
                                    // nothing to process
                                } else if (line.startsWith("uid")) {
                                    Log.info(c, methodName, "uid " + line);
                                    saveLine = true;
                                } else { // the password wrapped to the next line
                                    value = value + line.trim();
                                }

                            } else {
                                String[] prop = line.split(":: "); // ldif file is slightly irregular.
                                if (prop.length != 2) {
                                    prop = line.split(": "); // ldif file is slightly irregular.
                                    if (prop.length != 2) {
                                        Log.info(c, methodName, "Failed to process line: " + line);
                                    } else {
                                        key = prop[0];
                                        value = prop[1];
                                    }
                                } else {
                                    key = prop[0];
                                    value = prop[1];
                                }
                            }

                            if (key == null || value == null) {
                                Log.info(c, methodName, "nothing to process: " + line);
                            } else {
                                Log.info(c, methodName, "Adding " + key + " " + value);
                                entry.add(key, value);
                                w.println("entry.add(\"" + key + "\", \"" + value + "\");");
                            }

                        }
                        ldapServer.add(entry);
                        w.println("ldapServer.add(entry);");
                        w.println();
                        Log.info(c, methodName, "Lookup " + ldapServer.lookup(workingDN));

                    }
                    if (line == null) {
                        break;
                    }
                }

            } finally {
                br.close();
                w.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Error processing LDIF " + ex);
        }
    }

    /**
     * Import of users and groups from the TDS.ldif file we used to set up a standalone/local
     * LDAP server. Modified to escape some characters (especially entries designed to be tricky)
     * and update some users with correct passwords.
     *
     * The passwords did not port so update the userPassword field if/when you
     * need it for FAT tests. This is for test use only and only valid for the runtime of a test.
     *
     * @param ldapServer
     * @throws Exception
     */
    private static void addUsersTDS(EmbeddedApacheDS ldapServer) throws Exception {
        final String methodName = "addUsersTDS";
        Log.info(c, methodName, "Adding users for a TDS config");

        Entry entry = ldapServer.newEntry("cn=\\ vmmgroup6,o=ibm,c=us");
        entry.add("cn", "IHZtbWdyb3VwNg==");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("member", "cn=\\ vmmtestuser5,o=ibm,c=us");
        entry.add("description", "Group for testing RDN starting with space.");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=\\ vmmtestuser5,o=ibm,c=us");
        entry.add("sn", "IHZtbXRlc3R1c2VyNQ==");
        entry.add("cn", "IHZtbXRlc3R1c2VyNQ==");
        entry.add("objectClass", "top");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "ePerson");
        entry.add("description", "User for testing RDN starting with space.");
        entry.add("userPassword", "cGFzc3dvcmQ=");
        entry.add("uid", "IHZtbXRlc3R1c2VyNQ==");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=\\#vmmgroup5,o=ibm,c=us");
        entry.add("cn", "#vmmgroup5");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("member", "cn=\\#vmmtestuser4,o=ibm,c=us");
        entry.add("description", "Group for testing RDN starting with pound sign.");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=\\#vmmtestuser4,o=ibm,c=us");
        entry.add("sn", "#vmmtestuser4");
        entry.add("cn", "#vmmtestuser4");
        entry.add("objectClass", "top");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "ePerson");
        entry.add("description", "User for testing RDN starting with pound sign.");
        entry.add("userPassword", "cGFzc3dvcmQ=");
        entry.add("uid", "#vmmtestuser4");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=active_vmmGroup,ou=iGroups,o=ibm,c=us");
        entry.add("member", "uid=ping_vmmUser,ou=iUsers,o=ibm,c=us");
        entry.add("cn", "active_vmmGroup");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("description", "LDAP group active_vmmGroup for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=AllAttrsGroup,o=ibm,c=us");
        entry.add("member", "cn=AllAttrsVMM,o=ibm,c=us");
        entry.add("cn", "AllAttrsGroup");
        entry.add("objectClass", "groupOfNames");
        entry.add("objectClass", "top");
        entry.add("description", "LDAP Group AllAttrsVMMGroup for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=AllAttrsVMM,o=ibm,c=us");
        entry.add("preferredLanguage", "en");
        entry.add("employeeType", "Manager");
        entry.add("pager", "1617181920");
        entry.add("mobile", "0123456789");
        entry.add("title", "Vice President");
        entry.add("userPassword", "e3NoYX1NZTBTdFl0QjF6ekFZZHVhME9VT01TQkhRcG89postOfficeBox: IN");
        entry.add("postalCode", "411033");
        entry.add("mail", "AllAttrsVMM@ibm.com");
        entry.add("initials", "Mr.");
        entry.add("givenName", "vmm");
        entry.add("telephoneNumber", "2122232425");
        entry.add("displayName", "All Attrs User");
        entry.add("sn", "AllAttrsVMMsn");
        entry.add("cn", "AllAttrsVMM");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "ePerson");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "top");
        entry.add("facsimileTelephoneNumber", "1112131415");
        entry.add("street", "SB Road");
        entry.add("st", "MH");
        entry.add("uid", "AllAttrsVMM");
        entry.add("l", "Pune");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("uid=connect_vmmUser,ou=iUsers,o=ibm,c=us");
        entry.add("sn", "connect_vmmUser");
        entry.add("cn", "connect_vmmUser");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "ePerson");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("userPassword", "Y29ubmVjdF92bW1Vc2Vy");
        entry.add("uid", "connect_vmmUser");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=dynamicGroup1,o=ibm,c=us");
        entry.add("memberURL", "ldap:///o=ibm,c=us??sub?(&(objectclass=inetorgperson)(uid=vmm*))");
        entry.add("cn", "dynamicGroup1");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupOfURLs");
        entry.add("objectclass", "groupforapache");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("uid=eddard_vmmUser,ou=jUsers,o=ibm,c=us");
        entry.add("sn", "eddard_vmmUser");
        entry.add("cn", "eddard_vmmUser");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "ePerson");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("userPassword", "ZWRkYXJkX3ZtbVVzZXI=");
        entry.add("uid", "eddard_vmmUser");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=embedded_group1,o=ibm,c=us");
        entry.add("cn", "embedded_group1");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("objectClass", "groupforapache");
        entry.add("member", "cn=ng_user1,o=ibm,c=us");
        entry.add("member", "cn=ng_user3,o=ibm,c=us");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=embedded_group2,o=ibm,c=us");
        entry.add("cn", "embedded_group2");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("objectClass", "groupforapache");
        entry.add("member", "cn=ng_user2,o=ibm,c=us");
        entry.add("member", "cn=ng_user4,o=ibm,c=us");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=group1,o=ibm,c=us");
        entry.add("cn", "group1");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupforapache");
        entry.add("member", "cn=LDAPUser1,o=ibm,c=us");
        entry.add("member", "cn=user1,o=ibm,c=us");
        entry.add("member", "cn=user3,o=ibm,c=us");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=group2,o=ibm,c=us");
        entry.add("cn", "group2");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupforapache");
        entry.add("member", "cn=LDAPUser2,o=ibm,c=us");
        entry.add("member", "cn=user2,o=ibm,c=us");
        entry.add("member", "cn=user4,o=ibm,c=us");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=grp1,o=ibm,c=us");
        entry.add("member", "cn=user1g1,o=ibm,c=us");
        entry.add("cn", "grp1");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupforapache");
        entry.add("description", "LDAP group grp1 for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=grp2,o=ibm,c=us");
        entry.add("cn", "grp2");
        entry.add("owner", "cn=user2g2, o=IBM, c=us");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("member", "cn=user2g2, o=IBM, c=us");
        entry.add("member", "cn=user4g2 o=IBM, c=us");
        entry.add("member", "cn=user6g2, o=IBM, c=us");
        entry.add("member", "cn=user8g2, o=IBM, c=us");
        entry.add("description", "LDAP group for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=grp4,o=ibm,c=us");
        entry.add("cn", "grp4");
        entry.add("owner", "cn=user1grp4, o=IBM, c=us");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("member", "cn=user1grp4, o=IBM, c=us");
        entry.add("member", "cn=user2grp4, o=IBM, c=us");
        entry.add("description", "LDAP group for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=https://nc135020.tivlab.austin.ibm.com:9443/op/openid1,o=ibm,c=us");
        entry.add("sn", "https://nc135020.tivlab.austin.ibm.com:9443/op/openid1");
        entry.add("cn", "https://nc135020.tivlab.austin.ibm.com:9443/op/openid1");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("uid", "https://nc135020.tivlab.austin.ibm.com:9443/op/openid1");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("ou=jGroups,o=ibm,c=us");
        entry.add("ou", "jGroups");
        entry.add("objectclass", "organizationalunit");
        entry.add("objectclass", "top");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("uid=john_vmmUser,ou=jUsers,o=ibm,c=us");
        entry.add("sn", "john_vmmUser");
        entry.add("cn", "john_vmmUser");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "ePerson");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("userPassword", "am9obl92bW1Vc2Vy");
        entry.add("uid", "john_vmmUser");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=LDAPUser1,o=ibm,c=us");
        entry.add("mail", "LDAPUser1@ibm.com");
        entry.add("initials", "LDAPUser1");
        entry.add("telephonenumber", "1 919 555 5555");
        entry.add("sn", "LDAPUser1");
        entry.add("cn", "LDAPUser1");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NTSEF9TUQwM2pPeExRLzMydjdjcURhOENXaUdKOTE1TzFJQmlZVzM0aWc9PQ==");
        entry.add("uid", "LDAPUser1");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=LDAPUser2,o=ibm,c=us");
        entry.add("initials", "LDAPUser2");
        entry.add("sn", "LDAPUser2");
        entry.add("cn", "LDAPUser2");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NTSEF9VHlyalo1R0RNWHF4OFBmdXpGd3BhWFVESmFUMm1ySUxiRVFrNFE9PQ==");
        entry.add("uid", "LDAPUser2");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=LDAPUser5,o=ibm,c=us");
        entry.add("initials", "LDAPUser5");
        entry.add("sn", "LDAPUser5");
        entry.add("cn", "LDAPUser5");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NTSEF9TURRamQrNGdtSDZzM0JsSlRiTTlGUGxUeXJLd1VrVlZaVzdMRHc9PQ==");
        entry.add("uid", "LDAPUser5");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=nested_g1,o=ibm,c=us");
        entry.add("cn", "nested_g1");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("objectClass", "groupforapache");
        entry.add("objectClass", "ibm-nestedGroup");
        entry.add("member", "cn=embedded_group1,o=ibm,c=us");
        entry.add("member", "cn=embedded_group2,o=ibm,c=us");
        entry.add("member", "cn=topng_user1,o=ibm,c=us");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=ng_user1,o=ibm,c=us");
        entry.add("sn", "ng_user1");
        entry.add("cn", "ng_user1");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "ePerson");
        entry.add("userPassword", "e1NTSEF9Nys4VFo0YU5ibWxIUUV0aWtqK09GUlFaM3JUeWx5UGNOaU9tOGc9PQ==");
        entry.add("uid", "ng_user1");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=ng_user2,o=ibm,c=us");
        entry.add("sn", "ng_user2");
        entry.add("cn", "ng_user2");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "top");
        entry.add("objectClass", "person");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "ePerson");
        entry.add("userPassword", "e1NTSEF9K3cvRnZIaUJaZDhGMndwa3B5NFZuQ3lETlpXd1N5c085NlFDMFE9PQ==");
        entry.add("uid", "ng_user2");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=ng_user3,o=ibm,c=us");
        entry.add("sn", "ng_user3");
        entry.add("cn", "ng_user3");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "ePerson");
        entry.add("userPassword", "e1NTSEF9ZFozUjZEczFKZGtzbi9aL2hCZ0hZaTBqZWxhU0U1M2ViZnRWc3c9PQ==");
        entry.add("uid", "ng_user3");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=ng_user4,o=ibm,c=us");
        entry.add("sn", "ng_user4");
        entry.add("cn", "ng_user4");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "ePerson");
        entry.add("userPassword", "e1NTSEF9WVVkM25uM1NGRWFzNGZsRXZud2toVG9Cazg2a3hTWHVmTGthUGc9PQ==");
        entry.add("uid", "ng_user4");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=openid1,o=ibm,c=us");
        entry.add("sn", "openid1");
        entry.add("cn", "openid1");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("uid", "openid1");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=openid1.email@acme.com,o=ibm,c=us");
        entry.add("sn", "openid1.email@acme.com");
        entry.add("cn", "openid1.email@acme.com");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NTSEF9QVRadERnd1UvL0ZOYXk2VThYVjIwek9IL28rSjdkWDFCVXNCYnc9PQ==");
        entry.add("uid", "openid1.email@acme.com");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=openid2,o=ibm,c=us");
        entry.add("sn", "openid2");
        entry.add("cn", "openid2");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("uid", "openid2");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=openid2.email@acme.com,o=ibm,c=us");
        entry.add("sn", "openid2.email@acme.com");
        entry.add("cn", "openid2.email@acme.com");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NTSEF9UFhPWWZPMkRMRVBwSHBDY1ZEbDdsdlJsR2lVMTRQcm5FWWhpdGc9PQ==");
        entry.add("uid", "openid2.email@acme.com");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=openid3.com,o=ibm,c=us");
        entry.add("sn", "openid3");
        entry.add("cn", "openid3");
        entry.add("cn", "openid3.com");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("uid", "openid3");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=openid3.email@acme.com,o=ibm,c=us");
        entry.add("sn", "openid3.email@acme.com");
        entry.add("cn", "openid3.email@acme.com");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NTSEF9TU8xSzhxRGpsWlhrODdHQWtlKzYvYUlWcjhhc1FUYWdtbC9TNWc9PQ==");
        entry.add("uid", "openid3.email@acme.com");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=openid4,o=ibm,c=us");
        entry.add("sn", "openid4");
        entry.add("cn", "openid4");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("uid", "openid4");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=openid4.email@acme.com,o=ibm,c=us");
        entry.add("sn", "openid4.email@acme.com");
        entry.add("cn", "openid4.email@acme.com");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NTSEF9cmNGV2NEMTU0dkdhTnZvakVXQWl4NXZkOUV2bWdqZm1RT2xQR0E9PQ==");
        entry.add("uid", "openid4.email@acme.com");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=openidg1,o=ibm,c=us");
        entry.add("cn", "openidg1");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("member", "cn=openid1,o=ibm,c=us");
        entry.add("member", "cn=openid1.email@acme.com,o=ibm,c=us");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=openidg2,o=ibm,c=us");
        entry.add("cn", "openidg2");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("member", "cn=openid2,o=ibm,c=us");
        entry.add("member", "cn=openid2.email@acme.com,o=ibm,c=us");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("uid=ping_vmmUser,ou=iUsers,o=ibm,c=us");
        entry.add("sn", "ping_vmmUser");
        entry.add("cn", "ping_vmmUser");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "ePerson");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("userPassword", "cGluZ192bW1Vc2Vy");
        entry.add("uid", "ping_vmmUser");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("uid=pong_vmmUser,ou=iUsers,o=ibm,c=us");
        entry.add("sn", "pong_vmmUser");
        entry.add("cn", "pong_vmmUser");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "ePerson");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("userPassword", "cG9uZ192bW1Vc2Vy");
        entry.add("uid", "pong_vmmUser");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=silent_vmmGroup,ou=iGroups,o=ibm,c=us");
        entry.add("member", "uid=pong_vmmUser,ou=iUsers,o=ibm,c=us");
        entry.add("cn", "silent_vmmGroup");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("description", "LDAP group silent_vmmGroup for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=snow_vmmGroup,ou=jGroups,o=ibm,c=us");
        entry.add("member", "uid=john_vmmUser,ou=jUsers,o=ibm,c=us");
        entry.add("cn", "snow_vmmGroup");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("description", "LDAP group snow_vmmGroup for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=stark_vmmGroup,ou=jGroups,o=ibm,c=us");
        entry.add("cn", "stark_vmmGroup");
        entry.add("objectClass", "groupOfNames");
        entry.add("objectClass", "top");
        entry.add("member", "uid=eddard_vmmUser,ou=jUsers,o=ibm,c=us");
        entry.add("description", "LDAP group stark_vmmGroup for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=talanted_vmmGroup,ou=iGroups,o=ibm,c=us");
        entry.add("member", "uid=connect_vmmUser,ou=iUsers,o=ibm,c=us");
        entry.add("cn", "talanted_vmmGroup");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("description", "LDAP group talanted_vmmGroup for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=test,o=ibm,c=us");
        entry.add("mail", "test@ibm.com");
        entry.add("initials", "test");
        entry.add("telephonenumber", "1 919 555 5555");
        entry.add("sn", "testsn");
        entry.add("cn", "test");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NIQX1tTzhIV09hcXh2bXA0UmwxU01nWkMzTEpXQjA9");
        entry.add("uid", "test");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=testuser,o=ibm,c=us");
        entry.add("mail", "testuser@ibm.com");
        entry.add("initials", "testuser");
        entry.add("telephonenumber", "1 919 555 5555");
        entry.add("sn", "testuser");
        entry.add("cn", "testuser");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NIQX02blhJZDIvTnk0aTN4Wm0rUDZIRnJpK0VPQU09");
        entry.add("uid", "testuser");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=topng_user1,o=ibm,c=us");
        entry.add("sn", "topng_user1");
        entry.add("cn", "topng_user1");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "ePerson");
        entry.add("userPassword", "e1NTSEF9Mlhndlp5aUJTaGFUNmFwelVWSmNUM0srb09oVEVuUldBNTJIU3c9PQ==");
        entry.add("uid", "topng_user1");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user1,o=ibm,c=us");
        entry.add("initials", "user1");
        entry.add("sn", "user1");
        entry.add("cn", "user1");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NTSEF9UW1TVWdGM2tuU1dXK3QxRnllWVVsSm9qRTJ2YTdJN01DUUdwNHc9PQ==");
        entry.add("uid", "user1");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user1g1,o=ibm,c=us");
        entry.add("mail", "user1g1@ibm.com");
        entry.add("initials", "user1g1");
        entry.add("telephonenumber", "1 999 555 5555");
        entry.add("sn", "user1g1sn");
        entry.add("cn", "user1g1");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "ePerson");
        entry.add("objectclass", "top");
        entry.add("title", "Test User");
        entry.add("userPassword", "e1NTSEF9TEIvK0hRRUtKUEZVSVc5TElSWmI2Tk1CbkZMcGI5RjVRSzJXWVE9PQ==");
        entry.add("uid", "user1g1");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user1grp4,o=ibm,c=us");
        entry.add("sn", "user1grp4");
        entry.add("cn", "user1grp4");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("title", "Test User");
        entry.add("uid", "user1grp4");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user2,o=ibm,c=us");
        entry.add("initials", "user2");
        entry.add("sn", "user2");
        entry.add("cn", "user2");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NTSEF9TG5mUHJaTkFrMDkweEdaNzloYms4bkw3SXlVNWp5QzlCV29Id2c9PQ==");
        entry.add("uid", "user2");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user2g2,o=ibm,c=us");
        entry.add("sn", "user2g2");
        entry.add("cn", "user2g2");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("title", "Test User");
        entry.add("uid", "user2g2");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user2grp4,o=ibm,c=us");
        entry.add("sn", "user2grp4");
        entry.add("cn", "user2grp4");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("title", "Test User");
        entry.add("uid", "user2grp4");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user3,o=ibm,c=us");
        entry.add("initials", "user3");
        entry.add("sn", "user3");
        entry.add("cn", "user3");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NTSEF9dGwzTmR2N1RnYnY1VW5EZThzUG81eDJzaVdGUGkwZFdjeURPbnc9PQ==");
        entry.add("uid", "user3");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user3g1,o=ibm,c=us");
        entry.add("sn", "user3g1");
        entry.add("cn", "user3g1");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("title", "Test User");
        entry.add("uid", "user3g1");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user4,o=ibm,c=us");
        entry.add("initials", "user4");
        entry.add("sn", "user4");
        entry.add("cn", "user4");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NTSEF9eTR1Q0VoK3cyZVVrSnRPUlBNMHZYOGJtYjlTWGJQRXhvRE1McEE9PQ==");
        entry.add("uid", "user4");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user4g2,o=ibm,c=us");
        entry.add("sn", "user4g2");
        entry.add("cn", "user4g2");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("title", "Test User");
        entry.add("uid", "user4g2");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user5g1,o=ibm,c=us");
        entry.add("sn", "user5g1");
        entry.add("cn", "user5g1");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("title", "Test User");
        entry.add("uid", "user5g1");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user6g2,o=ibm,c=us");
        entry.add("sn", "user6g2");
        entry.add("cn", "user6g2");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("title", "Test User");
        entry.add("uid", "user6g2");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user7g1,o=ibm,c=us");
        entry.add("sn", "user7g1");
        entry.add("cn", "user7g1");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("title", "Test User");
        entry.add("uid", "user7g1");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=user8g2,o=ibm,c=us");
        entry.add("sn", "user8g2");
        entry.add("cn", "user8g2");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("title", "Test User");
        entry.add("uid", "user8g2");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmm_nestedGrp,o=ibm,c=us");
        entry.add("member", "cn=vmmgroup1,o=ibm,c=us");
        entry.add("member", "cn=vmmgroup2,o=ibm,c=us");
        entry.add("member", "cn=vmmgroup3,o=ibm,c=us");
        entry.add("cn", "vmm_nestedGrp");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupforapache");
        entry.add("description", "LDAP group vmm_nestedGrp for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmattruser,o=ibm,c=us");
        entry.add("mail", "vmmattruser@ibm.com");
        entry.add("initials", "vmmattruser");
        entry.add("telephonenumber", "1 919 122 5257");
        entry.add("sn", "vmmattrusersn");
        entry.add("cn", "vmmattruser");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "ePerson");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("userPassword", "e3NoYX13V0JPd3ZWc1pIa05zNllZQUpuWlV2ck9EMVU9postOfficeBox: chinchwad");
        entry.add("uid", "vmmattruser");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup1,o=ibm,c=us");
        entry.add("member", "cn=vmmuser1,o=ibm,c=us");
        entry.add("cn", "vmmgroup1");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupforapache");
        entry.add("description", "LDAP group vmmgroup1 for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry(new Dn("cn=vmmgroup10\\\\,o=ibm,c=us"));
        entry.add("cn", "vmmgroup10\\");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("member", "cn=vmmtestuser9\\,o=ibm,c=us");
        entry.add("description", "Group for testing RDN containing slash.");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup11\\<,o=ibm,c=us");
        entry.add("cn", "vmmgroup11<");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("member", "cn=vmmtestuser10\\<,o=ibm,c=us");
        entry.add("description", "Group for testing RDN containing left-facing guillemet.");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup12\\>,o=ibm,c=us");
        entry.add("cn", "vmmgroup12>");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("member", "cn=vmmtestuser11\\>,o=ibm,c=us");
        entry.add("description", "Group for testing RDN containing right-facing guillemet.");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup13\\;,o=ibm,c=us");
        entry.add("cn", "vmmgroup13;");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("member", "cn=vmmtestuser12\\;,o=ibm,c=us");
        entry.add("description", "Group for testing RDN containing semicolon.");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup2,o=ibm,c=us");
        entry.add("member", "cn=vmmuser2,o=ibm,c=us");
        entry.add("cn", "vmmgroup2");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupforapache");
        entry.add("description", "LDAP group vmmgroup2 for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup3,o=ibm,c=us");
        entry.add("member", "cn=vmmuser3,o=ibm,c=us");
        entry.add("cn", "vmmgroup3");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupforapache");
        entry.add("description", "LDAP group vmmgroup3 for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup4    \\ ,o=ibm,c=us");
        entry.add("cn", "dm1tZ3JvdXA0ICAgICA=");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("member", "cn=vmmtestuser3    \\ ,o=ibm,c=us");
        entry.add("description", "Group for testing RDN with containing trailing spaces.");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup7\\,,o=ibm,c=us");
        entry.add("cn", "vmmgroup7,");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("member", "cn=vmmtestuser6\\,,o=ibm,c=us");
        entry.add("description", "Group for testing RDN containing comma.");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup8\\+,o=ibm,c=us");
        entry.add("cn", "vmmgroup8+");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("member", "cn=vmmtestuser7\\+,o=ibm,c=us");
        entry.add("description", "Group for testing RDN containing plus sign.");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup9\\\",o=ibm,c=us");
        entry.add("cn", "vmmgroup9\"");
        entry.add("objectClass", "top");
        entry.add("objectClass", "groupOfNames");
        entry.add("member", "cn=vmmtestuser8\\\",o=ibm,c=us");
        entry.add("description", "Group for testing RDN with containing double quotation mark.");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgrp1,o=ibm,c=us");
        entry.add("member", "cn=vmmuser1,o=ibm,c=us");
        entry.add("member", "cn=vmmuser2,o=ibm,c=us");
        entry.add("member", "cn=vmmuser3,o=ibm,c=us");
        entry.add("member", "cn=vmmtestuser,o=ibm,c=us");
        entry.add("member", "cn=vmmtest,o=ibm,c=us");
        entry.add("member", "cn=vmmtestuser$,o=ibm,c=us");
        entry.add("member", "cn=vmmgroup1,o=ibm,c=us");
        entry.add("member", "cn=vmmgroup2,o=ibm,c=us");
        entry.add("member", "cn=vmmgroup3,o=ibm,c=us");
        entry.add("member", "cn=vmmgrp2,o=IBM,c=us");
        entry.add("member", "cn=vmmgrp3,o=IBM,c=us");
        entry.add("cn", "vmmgrp1");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupforapache");
        entry.add("description", "LDAP group vmmgr1 for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgrp2,o=ibm,c=us");
        entry.add("member", "cn=vmmgroup1,o=ibm,c=us");
        entry.add("member", "cn=vmmgroup2,o=ibm,c=us");
        entry.add("member", "cn=vmmgroup3,o=ibm,c=us");
        entry.add("member", "cn=vmmgrp2,o=IBM,c=us");
        entry.add("cn", "vmmgrp2");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupforapache");
        entry.add("description", "LDAP group vmmgr2 for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgrp3,o=ibm,c=us");
        entry.add("member", "uid=dummy");
        entry.add("cn", "vmmgrp3");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupforapache");
        entry.add("description", "LDAP group vmmgr3 for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgrp4,o=ibm,c=us");
        entry.add("member", "cn=vmmgrp1,o=IBM,c=us");
        entry.add("member", "cn=vmmtestuser,o=ibm,c=us");
        entry.add("member", "cn=vmmtest,o=ibm,c=us");
        entry.add("member", "cn=vmmtestuser$,o=ibm,c=us");
        entry.add("cn", "vmmgrp4");
        entry.add("objectclass", "groupOfNames");
        entry.add("objectclass", "top");
        entry.add("objectclass", "groupforapache");
        entry.add("description", "LDAP group vmmgr4 for testing");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmLibertyUser,o=ibm,c=us");
        entry.add("mail", "vmmLibertyUser@ibm.com");
        entry.add("initials", "vmmLibertyUser");
        entry.add("telephonenumber", "1 919 555 5555");
        entry.add("sn", "vmmLibertyUserSN");
        entry.add("cn", "vmmLibertyUser");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NIQX1DejNZTm03Wm51VUE4Z0JKSFhYVXlHNXo0Mnc9");
        entry.add("uid", "vmmLibertyUserUID");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtest,o=ibm,c=us");
        entry.add("mail", "vmmtest@ibm.com");
        entry.add("initials", "vmmtest");
        entry.add("telephonenumber", "1 919 555 5555");
        entry.add("sn", "vmmtestsn");
        entry.add("cn", "vmmtest");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "vmmtestuserpwd");
        entry.add("uid", "vmmtest");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser,o=ibm,c=us");
        entry.add("mail", "vmmtestuser@ibm.com");
        entry.add("initials", "vmmtestuser");
        entry.add("telephonenumber", "1 919 555 5555");
        entry.add("sn", "vmmtestusersn");
        entry.add("cn", "vmmtestuser");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "vmmtestuserpwd");
        entry.add("uid", "vmmtestuser");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser$,o=ibm,c=us");
        entry.add("mail", "vmmtestuser@ibm.com");
        entry.add("initials", "vmmtestuser$");
        entry.add("telephonenumber", "1 919 555 5555");
        entry.add("sn", "vmmtestusersn$");
        entry.add("cn", "vmmtestuser$");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NIQX1PUUdySkhkaW5BRDU3MVpMeHdPT2M2TFdFQjg9");
        entry.add("uid", "vmmtestuser$");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser10\\<,o=ibm,c=us");
        entry.add("sn", "vmmtestuser10<");
        entry.add("cn", "vmmtestuser10<");
        entry.add("objectClass", "top");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "ePerson");
        entry.add("description", "User for testing RDN containing left-facing guillemet.");
        entry.add("userPassword", "cGFzc3dvcmQ=");
        entry.add("uid", "vmmtestuser10<");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser11\\>,o=ibm,c=us");
        entry.add("sn", "vmmtestuser11>");
        entry.add("cn", "vmmtestuser11>");
        entry.add("objectClass", "top");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "ePerson");
        entry.add("description", "User for testing RDN containing right-facing guillemet.");
        entry.add("userPassword", "cGFzc3dvcmQ=");
        entry.add("uid", "vmmtestuser11>");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser12\\;,o=ibm,c=us");
        entry.add("sn", "vmmtestuser12;");
        entry.add("cn", "vmmtestuser12;");
        entry.add("objectClass", "top");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "ePerson");
        entry.add("description", "User for testing RDN containing semicolon.");
        entry.add("userPassword", "cGFzc3dvcmQ=");
        entry.add("uid", "vmmtestuser12;");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser2,o=ibm,c=us");
        entry.add("mail", "vmmtestuser2@ibm.com");
        entry.add("initials", "vmmtestuser2");
        entry.add("telephonenumber", "1 919 555 5555");
        entry.add("sn", "vmmtestuser2sn");
        entry.add("cn", "vmmtestuser2");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e1NIQX1DOFlVWjhlcGY5UlRjK0wzV29RK1U2VHdLb2s9");
        entry.add("uid", "vmmtestuser2");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser3    \\ ,o=ibm,c=us");
        entry.add("sn", "dm1tdGVzdHVzZXIzICAgICA=");
        entry.add("cn", "dm1tdGVzdHVzZXIzICAgICA=");
        entry.add("objectClass", "top");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "ePerson");
        entry.add("description", "User for testing RDN containing multiple trailing spaces.");
        entry.add("userPassword", "cGFzc3dvcmQ=");
        entry.add("uid", "dm1tdGVzdHVzZXIzICAgICA=");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser6\\,,o=ibm,c=us");
        entry.add("sn", "vmmtestuser6,");
        entry.add("cn", "vmmtestuser6,");
        entry.add("objectClass", "top");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "ePerson");
        entry.add("description", "User for testing RDN containing comma.");
        entry.add("userPassword", "cGFzc3dvcmQ=");
        entry.add("uid", "vmmtestuser6,");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser7\\+,o=ibm,c=us");
        entry.add("sn", "vmmtestuser7+");
        entry.add("cn", "vmmtestuser7+");
        entry.add("objectClass", "top");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "ePerson");
        entry.add("description", "User for testing RDN containing plus sign.");
        entry.add("userPassword", "cGFzc3dvcmQ=");
        entry.add("uid", "vmmtestuser7+");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser8\\\",o=ibm,c=us");
        entry.add("sn", "vmmtestuser8\"");
        entry.add("cn", "vmmtestuser8\"");
        entry.add("objectClass", "top");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "ePerson");
        entry.add("description", "User for testing RDN containing double qoutation mark.");
        entry.add("userPassword", "cGFzc3dvcmQ=");
        entry.add("uid", "vmmtestuser8\"");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser9\\\\,o=ibm,c=us");
        entry.add("sn", "vmmtestuser9\\");
        entry.add("cn", "vmmtestuser9\\");
        entry.add("objectClass", "top");
        entry.add("objectClass", "inetOrgPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "ePerson");
        entry.add("description", "User for testing RDN containing slash.");
        entry.add("userPassword", "cGFzc3dvcmQ=");
        entry.add("uid", "vmmtestuser9\\");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmuser1,o=ibm,c=us");
        entry.add("mail", "vmmuser1@ibm.com");
        entry.add("initials", "vmmuser1");
        entry.add("telephonenumber", "1 919 555 5555");
        entry.add("sn", "vmmuser1sn");
        entry.add("cn", "vmmuser1");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e3NoYX11WmNIWW1Qb1UvQzh3c0NYUmdzMFFBT25LYjQ9");
        entry.add("uid", "vmmuser1");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmuser2,o=ibm,c=us");
        entry.add("mail", "vmmuser2@ibm.com");
        entry.add("initials", "vmmuser2");
        entry.add("telephonenumber", "1 919 555 5555");
        entry.add("sn", "vmmuser2sn");
        entry.add("cn", "vmmuser2");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e3NoYX1uSnlhMTV0dm1jNGpVOG03cDJ0WVJLOGVOcVU9");
        entry.add("uid", "vmmuser2");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmuser3,o=ibm,c=us");
        entry.add("mail", "vmmuser3@ibm.com");
        entry.add("initials", "vmmuser3");
        entry.add("telephonenumber", "1 919 555 5555");
        entry.add("sn", "vmmuser3sn");
        entry.add("cn", "vmmuser3");
        entry.add("objectclass", "organizationalPerson");
        entry.add("objectclass", "person");
        entry.add("objectclass", "inetOrgPerson");
        entry.add("objectclass", "top");
        entry.add("objectclass", "ePerson");
        entry.add("userPassword", "e3NoYX1YdzhndUorTllLbUZSbEQyYldldFlKSzlDVVU9");
        entry.add("uid", "vmmuser3");
        ldapServer.add(entry);

    }

    /**
     * Import of users and groups from the AD.ldif file we used to set up a standalone/local
     * LDAP server. Modified to escape some characters (especially entries designed to be tricky)
     * and update some users with correct passwords.
     *
     * The passwords did not port so update the userPassword field if/when you
     * need it for FAT tests. This is for test use only and only valid for the runtime of a test.
     *
     *
     * @param ldapServer
     * @throws Exception
     */
    private static void addUsersAD(EmbeddedApacheDS ldapServer) throws Exception {
        final String methodName = "addUsersAD";
        Log.info(c, methodName, "Adding users for an AD config");

        Entry entry = ldapServer.newEntry("cn=arya_user,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "arya_user");
        entry.add("memberOf", "CN=baratheon_group,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "arya_user");
        entry.add("givenName", "arya_user");
        entry.add("userPrincipalName", "arya_user@secfvt2.austin.ibm.com");
        entry.add("displayName", "arya_user");
        entry.add("sn", "arya_user");
        entry.add("cn", "arya_user");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("objectClass", "top");
        entry.add("distinguishedName", "CN=arya_user,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=baratheon_group,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "baratheon_group");
        entry.add("memberOf", "CN=lannister_group,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=cersei_user2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=arya_user,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "baratheon_group");
        entry.add("objectClass", "group");
        entry.add("objectClass", "top");
        entry.add("description", "QmFyYXRoZW9uIEdyb3VwIA==");
        entry.add("distinguishedName", "CN=baratheon_group,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=bolton_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "bolton_group1");
        entry.add("member", "CN=sansa_user,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "bolton_group1");
        entry.add("objectClass", "group");
        entry.add("objectClass", "top");
        entry.add("description", "Bolton Group 1");
        entry.add("distinguishedName", "CN=bolton_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=cersei_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "cersei_user1");
        entry.add("memberOf", "CN=lannister_group,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "cersei_user1");
        entry.add("givenName", "cersei_user1");
        entry.add("userPrincipalName", "cersei_user1@secfvt2.austin.ibm.com");
        entry.add("displayName", "cersei_user1");
        entry.add("sn", "cersei_user1");
        entry.add("cn", "cersei_user1");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("objectClass", "top");
        entry.add("distinguishedName", "CN=cersei_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=cersei_user2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "cersei_user2");
        entry.add("memberOf", "CN=baratheon_group,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "cersei_user2");
        entry.add("givenName", "cersei_user2");
        entry.add("userPrincipalName", "cersei_user2@secfvt2.austin.ibm.com");
        entry.add("displayName", "cersei_user2");
        entry.add("sn", "cersei_user2");
        entry.add("cn", "cersei_user2");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("objectClass", "top");
        entry.add("distinguishedName", "CN=cersei_user2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=embedded_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "embedded_group1");
        entry.add("memberOf", "CN=nested_g1,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=ng_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=ng_user3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "embedded_group1");
        entry.add("objectClass", "top");
        entry.add("objectClass", "group");
        entry.add("description", "Group under nested_g1");
        entry.add("distinguishedName", "CN=embedded_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=embedded_group2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "embedded_group2");
        entry.add("memberOf", "CN=nested_g1,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=ng_user2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=ng_user4,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "embedded_group2");
        entry.add("objectClass", "top");
        entry.add("objectClass", "group");
        entry.add("description", "Group under nested_g1");
        entry.add("distinguishedName", "CN=embedded_group2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=got_grand,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "got_grand");
        entry.add("member", "cn=got_nested,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "got_grand");
        entry.add("objectClass", "group");
        entry.add("objectClass", "top");
        entry.add("description", "R290IEdyYW5kIEdyb3VwIA==");
        entry.add("distinguishedName", "CN=got_grand,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=got_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "got_group1");
        entry.add("memberOf", "CN=got_nested,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "cn=jaime_user,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "got_group1");
        entry.add("objectClass", "group");
        entry.add("objectClass", "top");
        entry.add("description", "Got Group 1");
        entry.add("distinguishedName", "CN=got_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=got_nested,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "got_nested");
        entry.add("memberOf", "CN=got_grand,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "cn=got_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "got_nested");
        entry.add("objectClass", "group");
        entry.add("objectClass", "top");
        entry.add("description", "R290IE5lc3RlZCBHcm91cCA=");
        entry.add("distinguishedName", "CN=got_nested,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=Group Policy Creator Owners,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "Group Policy Creator Owners");
        entry.add("cn", "Group Policy Creator Owners");
        entry.add("objectClass", "top");
        entry.add("objectClass", "group");
        entry.add("distinguishedName", "cn=Group Policy Creator Owners,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=jaime_user,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "jaime_user");
        entry.add("memberOf", "CN=got_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "jaime_user");
        entry.add("givenName", "jaime_user");
        entry.add("userPrincipalName", "jaime_user@secfvt2.austin.ibm.com");
        entry.add("displayName", "jaime_user");
        entry.add("sn", "jaime_user");
        entry.add("cn", "jaime_user");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("objectClass", "top");
        entry.add("distinguishedName", "CN=jaime_user,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=johnsnow_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "johnsnow_user1");
        entry.add("memberOf", "CN=nights_watch_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "johnsnow_user1");
        entry.add("givenName", "johnsnow_user1");
        entry.add("userPrincipalName", "johnsnow_user1@secfvt2.austin.ibm.com");
        entry.add("displayName", "johnsnow_user1");
        entry.add("sn", "johnsnow_user1");
        entry.add("cn", "johnsnow_user1");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("objectClass", "top");
        entry.add("distinguishedName", "CN=johnsnow_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=johnstark_user2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "johnstark_user2");
        entry.add("memberOf", "CN=nights_watch_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "johnstark_user2");
        entry.add("givenName", "johnsnow_user2");
        entry.add("userPrincipalName", "johnstark_user2@secfvt2.austin.ibm.com");
        entry.add("displayName", "johnstark_user2");
        entry.add("sn", "johnstark_user2");
        entry.add("cn", "johnstark_user2");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("objectClass", "top");
        entry.add("distinguishedName", "CN=johnstark_user2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=lannister_group,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "lannister_group");
        entry.add("member", "CN=cersei_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=baratheon_group,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "lannister_group");
        entry.add("objectClass", "group");
        entry.add("objectClass", "top");
        entry.add("description", "TGFubmlzdGVyIEdyb3VwIA==");
        entry.add("distinguishedName", "CN=lannister_group,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=LDAPUser1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "LDAPUser1");
        entry.add("name", "LDAPUser1");
        entry.add("givenName", "LDAPUser1");
        entry.add("userPrincipalName", "LDAPUser1@secfvt2.austin.ibm.com");
        entry.add("displayName", "LDAPUser1");
        entry.add("sn", "LDAPUser1");
        entry.add("cn", "LDAPUser1");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "user");
        entry.add("description", "1339601470");
        entry.add("userPassword", "e1NIQX1Kc09ISmRNMG5YZzlSczVjazdWd2VrK21kaDQ9");
        entry.add("distinguishedName", "cn=LDAPUser1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=LDAPUser2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "LDAPUser2");
        entry.add("name", "LDAPUser2");
        entry.add("givenName", "LDAPUser2");
        entry.add("userPrincipalName", "LDAPUser2@secfvt2.austin.ibm.com");
        entry.add("displayName", "LDAPUser2");
        entry.add("sn", "LDAPUser2");
        entry.add("cn", "LDAPUser2");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "user");
        entry.add("userPassword", "e1NIQX1wbS9wdk91RDlZRDZjczdIMklFcFB4NUhHTjg9");
        entry.add("distinguishedName", "cn=LDAPUser2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=LDAPUser5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "LDAPUser5");
        entry.add("name", "LDAPUser5");
        entry.add("givenName", "LDAPUser5");
        entry.add("userPrincipalName", "LDAPUser5@secfvt2.austin.ibm.com");
        entry.add("displayName", "LDAPUser5");
        entry.add("sn", "LDAPUser5");
        entry.add("cn", "LDAPUser5");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "user");
        entry.add("distinguishedName", "cn=LDAPUser5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=nested_g1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "nested_g1");
        entry.add("member", "CN=embedded_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=embedded_group2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=topng_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "nested_g1");
        entry.add("objectClass", "top");
        entry.add("objectClass", "group");
        entry.add("description", "top level group with nested group embedded_g1 under it");
        entry.add("distinguishedName", "CN=nested_g1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=ng_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "ng_user1");
        entry.add("memberOf", "CN=embedded_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "ng_user1");
        entry.add("givenName", "ng_user1");
        entry.add("userPrincipalName", "ng_user1@secfvt2.austin.ibm.com");
        entry.add("displayName", "ng_user1");
        entry.add("sn", "ng_user1");
        entry.add("cn", "ng_user1");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "top");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("userPassword", "e1NTSEF9ZWFNWi9KWGt5R2t0YkJkbjFFRjNZdjdWSHFydkNab3lHMG9HRGc9PQ=");
        entry.add("userPassword", "e1NTSEF9ZWFNWi9KWGt5R2t0YkJkbjFFRjNZdjdWSHFydkNab3lHMG9HRGc9PQ=");
        entry.add("distinguishedName", "CN=ng_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=ng_user2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "ng_user2");
        entry.add("memberOf", "CN=embedded_group2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "ng_user2");
        entry.add("givenName", "ng_user2");
        entry.add("userPrincipalName", "ng_user2@secfvt2.austin.ibm.com");
        entry.add("displayName", "ng_user2");
        entry.add("sn", "ng_user2");
        entry.add("cn", "ng_user2");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "top");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("userPassword", "e1NTSEF9MW9QYXlqakVZMERTNzdiblpLcGxjWHJTdHdRbkJUbEp4MytqV2c9PQ=");
        entry.add("userPassword", "e1NTSEF9MW9QYXlqakVZMERTNzdiblpLcGxjWHJTdHdRbkJUbEp4MytqV2c9PQ=");
        entry.add("distinguishedName", "CN=ng_user2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=ng_user3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "ng_user3");
        entry.add("memberOf", "CN=embedded_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "ng_user3");
        entry.add("givenName", "ng_user3");
        entry.add("userPrincipalName", "ng_user3@secfvt2.austin.ibm.com");
        entry.add("displayName", "ng_user3");
        entry.add("sn", "ng_user3");
        entry.add("cn", "ng_user3");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "top");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("userPassword", "e1NTSEF9Qk96bDFGOEdlNW1SaHM1WElLYjJsQWxWSmdxZnhxWStldlJxN3c9PQ=");
        entry.add("userPassword", "e1NTSEF9Qk96bDFGOEdlNW1SaHM1WElLYjJsQWxWSmdxZnhxWStldlJxN3c9PQ=");
        entry.add("distinguishedName", "CN=ng_user3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=ng_user4,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "ng_user4");
        entry.add("memberOf", "CN=embedded_group2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "ng_user4");
        entry.add("givenName", "ng_user4");
        entry.add("userPrincipalName", "ng_user4@secfvt2.austin.ibm.com");
        entry.add("displayName", "ng_user4");
        entry.add("sn", "ng_user4");
        entry.add("cn", "ng_user4");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "top");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("userPassword", "e1NTSEF9ZU9BRG1FSnRuMFJUUEQxbnZzWmtWZXo0VVlFTWJRQzE1VXN0ZFE9PQ=");
        entry.add("userPassword", "e1NTSEF9ZU9BRG1FSnRuMFJUUEQxbnZzWmtWZXo0VVlFTWJRQzE1VXN0ZFE9PQ=");
        entry.add("distinguishedName", "CN=ng_user4,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=nights_watch_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "nights_watch_group1");
        entry.add("member", "CN=johnsnow_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=johnstark_user2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=samtarley_user3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "nights_watch_group1");
        entry.add("objectClass", "group");
        entry.add("objectClass", "top");
        entry.add("description", "Nights watch Group 1");
        entry.add("distinguishedName", "CN=nights_watch_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=samtarley_user3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "samtarley_user3");
        entry.add("memberOf", "CN=nights_watch_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "samtarley_user3");
        entry.add("givenName", "samtarley_user3");
        entry.add("userPrincipalName", "samtarley_user3@secfvt2.austin.ibm.com");
        entry.add("displayName", "samtarley_user3");
        entry.add("sn", "samtarley_user3");
        entry.add("cn", "samtarley_user3");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("objectClass", "top");
        entry.add("distinguishedName", "CN=samtarley_user3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=sansa_user,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "sansa_user");
        entry.add("memberOf", "CN=stark_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "CN=stark_group2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "CN=bolton_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "sansa_user");
        entry.add("givenName", "sansa_user");
        entry.add("userPrincipalName", "sansa_user@secfvt2.austin.ibm.com");
        entry.add("displayName", "sansa_user");
        entry.add("sn", "sansa_user");
        entry.add("cn", "sansa_user");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "user");
        entry.add("objectClass", "top");
        entry.add("distinguishedName", "CN=sansa_user,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=stark_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "stark_group1");
        entry.add("member", "CN=sansa_user,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "stark_group1");
        entry.add("objectClass", "group");
        entry.add("objectClass", "top");
        entry.add("description", "Stark Group 1");
        entry.add("distinguishedName", "CN=stark_group1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=stark_group2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "stark_group2");
        entry.add("member", "CN=sansa_user,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "stark_group2");
        entry.add("objectClass", "group");
        entry.add("objectClass", "top");
        entry.add("description", "Stark Group 2");
        entry.add("distinguishedName", "CN=stark_group2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=TelnetClients,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "TelnetClients");
        entry.add("cn", "TelnetClients");
        entry.add("objectClass", "top");
        entry.add("objectClass", "group");
        entry.add("distinguishedName", "cn=TelnetClients,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=testuser,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "testuser");
        entry.add("name", "testuser");
        entry.add("givenName", "testuser");
        entry.add("userPrincipalName", "testuser@secfvt2.austin.ibm.com");
        entry.add("displayName", "testuser");
        entry.add("sn", "testuser");
        entry.add("cn", "testuser");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "user");
        entry.add("userPassword", "e1NIQX02blhJZDIvTnk0aTN4Wm0rUDZIRnJpK0VPQU09");
        entry.add("distinguishedName", "cn=testuser,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=topng_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "topng_user1");
        entry.add("memberOf", "CN=nested_g1,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "topng_user1");
        entry.add("givenName", "topng_user1");
        entry.add("userPrincipalName", "topng_user1@secfvt2.austin.ibm.com");
        entry.add("displayName", "topng_user1");
        entry.add("sn", "topng_user1");
        entry.add("cn", "topng_user1");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "user");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("userPassword", "e1NTSEF9WG1KY21NamdOdEFyNnJNZU52c0l0bnU4U2ZVdkV0ZHI2YXpUVUE9PQ=");
        entry.add("userPassword", "e1NTSEF9WG1KY21NamdOdEFyNnJNZU52c0l0bnU4U2ZVdkV0ZHI2YXpUVUE9PQ=");
        entry.add("distinguishedName", "CN=topng_user1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmgroup1");
        entry.add("memberOf", "cn=vmmgroup5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "cn=vmmNestedGroup,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "cn=vmmuser1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmtestuser,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "vmmgroup1");
        entry.add("objectClass", "top");
        entry.add("objectClass", "group");
        entry.add("distinguishedName", "cn=vmmgroup1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmgroup2");
        entry.add("memberOf", "cn=vmmgroup5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmuser2,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmtestuser,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "vmmgroup2");
        entry.add("objectClass", "top");
        entry.add("objectClass", "group");
        entry.add("distinguishedName", "cn=vmmgroup2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmgroup3");
        entry.add("memberOf", "cn=vmmgroup4,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "cn=vmmgroup5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmuser3,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmtestuser,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "vmmgroup3");
        entry.add("objectClass", "top");
        entry.add("objectClass", "group");
        entry.add("distinguishedName", "cn=vmmgroup3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup4,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmgroup4");
        entry.add("memberOf", "cn=vmmgroup5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "cn=vmmNestedGroup,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmuser4,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmgroup3,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "vmmgroup4");
        entry.add("objectClass", "top");
        entry.add("objectClass", "group");
        entry.add("distinguishedName", "cn=vmmgroup4,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmgroup5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmgroup5");
        entry.add("member", "CN=vmmuser1,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmuser2,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmuser3,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmuser4,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmtestuser,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmgroup1,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmgroup2,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmgroup3,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmgroup4,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "vmmgroup5");
        entry.add("objectClass", "top");
        entry.add("objectClass", "group");
        entry.add("distinguishedName", "cn=vmmgroup5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmNestedGroup,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmNestedGroup");
        entry.add("member", "CN=vmmgroup1,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmgroup4,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("member", "CN=vmmuser2,CN=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("cn", "vmmNestedGroup");
        entry.add("objectClass", "top");
        entry.add("objectClass", "group");
        entry.add("distinguishedName", "cn=vmmNestedGroup,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmtestuser,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmtestuser");
        entry.add("memberOf", "cn=vmmgroup1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "cn=vmmgroup2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "cn=vmmgroup3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "cn=vmmgroup5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "vmmtestuser");
        entry.add("givenName", "vmmtestuser");
        entry.add("userPrincipalName", "vmmtestuser@secfvt2.austin.ibm.com");
        entry.add("displayName", "vmmtestuser");
        entry.add("sn", "vmmtestuser");
        entry.add("cn", "vmmtestuser");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "user");
        entry.add("userPassword", "vmmtestuserpwd");
        entry.add("distinguishedName", "cn=vmmtestuser,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmuser1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmuser1");
        entry.add("memberOf", "cn=vmmgroup1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "cn=vmmgroup5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "vmmuser1");
        entry.add("givenName", "vmmuser1");
        entry.add("userPrincipalName", "vmmuser1@secfvt2.austin.ibm.com");
        entry.add("displayName", "vmmuser1");
        entry.add("sn", "vmmuser1");
        entry.add("cn", "vmmuser1");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "user");
        entry.add("userPassword", "e1NIQX1XWjNkN3A0eUdIRkUxMXdhaVl0TkJLSi95RUU9");
        entry.add("distinguishedName", "cn=vmmuser1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmuser2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmuser2");
        entry.add("memberOf", "cn=vmmgroup2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "cn=vmmgroup5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "cn=vmmNestedGroup,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "vmmuser2");
        entry.add("givenName", "vmmuser2");
        entry.add("userPrincipalName", "vmmuser2@secfvt2.austin.ibm.com");
        entry.add("displayName", "vmmuser2");
        entry.add("sn", "vmmuser2");
        entry.add("cn", "vmmuser2");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "user");
        entry.add("userPassword", "e1NIQX1Wd2ZFZkdkcHZIcE1JdXkwbXFaT0NCSVpCcG89");
        entry.add("distinguishedName", "cn=vmmuser2,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmuser3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmuser3");
        entry.add("memberOf", "cn=vmmgroup3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "cn=vmmgroup5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "vmmuser3");
        entry.add("givenName", "vmmuser3");
        entry.add("userPrincipalName", "vmmuser3@secfvt2.austin.ibm.com");
        entry.add("displayName", "vmmuser3");
        entry.add("sn", "vmmuser3");
        entry.add("cn", "vmmuser3");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "user");
        entry.add("userPassword", "e1NIQX1VZkhzUlltMTFwditwN2gvMXZ0YzY4Q0J2TWM9");
        entry.add("distinguishedName", "cn=vmmuser3,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmuser4,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmuser4");
        entry.add("memberOf", "cn=vmmgroup4,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("memberOf", "cn=vmmgroup5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "vmmuser4");
        entry.add("givenName", "vmmuser4");
        entry.add("userPrincipalName", "vmmuser4@secfvt2.austin.ibm.com");
        entry.add("displayName", "vmmuser4");
        entry.add("sn", "vmmuser4");
        entry.add("cn", "vmmuser4");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "user");
        entry.add("userPassword", "e1NIQX1yY2Q3YXhGS1dsZ1huWEw0QXlYakxWY2V0NE09");
        entry.add("distinguishedName", "cn=vmmuser4,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=vmmuser5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "vmmuser5");
        entry.add("name", "vmmuser5");
        entry.add("givenName", "vmmuser5");
        entry.add("userPrincipalName", "vmmuser5@secfvt2.austin.ibm.com");
        entry.add("displayName", "vmmuser5");
        entry.add("sn", "vmmuser5");
        entry.add("cn", "vmmuser5");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "user");
        entry.add("userPassword", "e1NIQX1CdDBTdGZycUljNjQ4dlZ1WlBsbzNkWVNUL1U9");
        entry.add("distinguishedName", "cn=vmmuser5,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=WIMUser1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("sAMAccountName", "WIMUser1");
        entry.add("memberOf", "cn=vmmgroup1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        entry.add("name", "WIMUser1");
        entry.add("givenName", "WIMUser1");
        entry.add("userPrincipalName", "WIMUser1@secfvt2.austin.ibm.com");
        entry.add("displayName", "WIMUser1");
        entry.add("sn", "WIMUser1");
        entry.add("cn", "WIMUser1");
        entry.add("objectClass", "organizationalPerson");
        entry.add("objectClass", "person");
        entry.add("objectClass", "top");
        entry.add("objectClass", "user");
        entry.add("distinguishedName", "cn=WIMUser1,cn=Users,DC=SECFVT2,DC=AUSTIN,DC=IBM,DC=COM");
        ldapServer.add(entry);

    }

}
