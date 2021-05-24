/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat.config;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.ApacheDSandKDC;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FileUtils;

@RunWith(FATRunner.class)
public class ApacheDSandKDCforSPNEGO extends ApacheDSandKDC {

    private static final Class<?> c = ApacheDSandKDCforSPNEGO.class;

    public static String SPN;

    public static String SPN_PASSWORD = "httppwd";

    private static String spnegoUserDN;

    public static String canonicalHostname = "canhostname1";

    private static String libertyServerSPNKeytabFile = null;

    //ADDED ON
    //KDC Users
    public static String KDC_USER = bindUserName;
    public static String KDC_USER_PWD = bindPassword;
    public static String KDC2_USER = null;
    public static String KDC2_USER_PWD = null;

    public static String FIRST_USER = KDC_USER; //"user1";
    public static String FIRST_USER_PWD = KDC_USER_PWD; //"user1pwd";
    public static String FIRST_USER_KRB5_FQN = null;
    public static String FIRST_USER_KRB5_FQN_PWD = null;
    public static String SECOND_USER = KDC_USER; //"user2";
    public static String USER_PWD = null;
    public static String SECOND_USER_PWD = KDC_USER_PWD; //"user2pwd";
    public static String SECOND_USER_KRB5_FQN = null;
    public static String SECOND_USER_KRB5_FQN_PWD = null;
    public static String Z_USER = null;
    public static String Z_USER_PWD = null;

    public static String COMMON_TOKEN_USER = FIRST_USER;
    public static String COMMON_TOKEN_USER_PWD = FIRST_USER_PWD;
    public static boolean COMMON_TOKEN_USER_IS_EMPLOYEE = true;
    public static boolean COMMON_TOKEN_USER_IS_MANAGER = false;

    public static String COMMON_SPNEGO_TOKEN = null;
    public static String KEYTAB_FILE_LOCATION = null;
    public static long COMMON_TOKEN_CREATION_DATE = 0;
    public static final double TOKEN_REFRESH_LIFETIME_SECONDS = 180;
    public static boolean RUN_TESTS = true;
    public static boolean LOCALHOST_DEFAULT_IP_ADDRESS = true; //127.0.0.1       localhost

    public static boolean IBM_JDK_V8_LOWER = false;
    public static boolean OTHER_SUPPORT_JDKS = false;
    public static boolean SUN_ORACLE_JDK_V8_HIGHER = false;
    public static boolean IBM_HYBRID_JDK = false;

    public static String serverShortHostName = "shortHostName";

    public static String KDC_REALM = DOMAIN;

    @BeforeClass
    public static void setup() throws Exception {

        bindUserName = "user1";
        bindPassword = "user1pwd";
        bindPrincipalName = bindUserName + "@" + DOMAIN;
        KDC_USER = bindUserName;
        KDC_USER_PWD = bindPassword;
        FIRST_USER = KDC_USER;
        FIRST_USER_PWD = KDC_USER_PWD;
        COMMON_TOKEN_USER = FIRST_USER;

        SECOND_USER = "user2";
        SECOND_USER_PWD = "user2pwd";

        ldapUser = "ldap";

        krbtgtUser = "krbtgt";

        spnegoUserDN = "uid=" + canonicalHostname + "," + BASE_DN;

        SPN = "HTTP/" + canonicalHostname + "@" + DOMAIN;

        setupService();

        ApacheKDCCommonTest.setGlobalLoggingLevel(Level.INFO);

        createPrincipal(SECOND_USER, SECOND_USER_PWD, SECOND_USER + "@" + DOMAIN);

        createSpnegoSPNUserEntry();

        createLibertyServerSPNKeytab();

        //addBasicUserAndGroup();
    }

    @After
    public void tearDown() throws Exception {
        tearDownService();
    }

    /**
     * Create the Spnego(HTTP) SPN service/user in the KDCServer DS
     *
     * @throws Exception
     */
    public static void createSpnegoSPNUserEntry() throws Exception {
        Log.info(c, "createKerberosUserEntries", "Creating KDC user entries");

        session = kdcServer.getDirectoryService().getAdminSession();

        // spnego HTTP service
        Entry entry = new DefaultEntry(session.getDirectoryService().getSchemaManager());
        entry.setDn(spnegoUserDN);
        entry.add("objectClass", "top", "person", "inetOrgPerson", "krb5principal", "krb5kdcentry");
        entry.add("cn", "HTTP");
        entry.add("sn", "Service");
        entry.add("uid", "HTTP");
        entry.add("userPassword", SPN_PASSWORD);
        entry.add("krb5PrincipalName", SPN);
        entry.add("krb5KeyVersionNumber", "0");
        session.add(entry);

        Log.info(c, "createKerberosUserEntries", "Created " + entry.getDn());

    }

    /**
     * Create a keytab file with the default bindUser and password
     *
     * @throws Exception
     */
    public static void createLibertyServerSPNKeytab() throws Exception {
        Log.info(c, "createLibertyServerSPNKeytab", "Creating keytab for " + SPN);
        File keyTabTemp = File.createTempFile(canonicalHostname + "_http", ".keytab");
        if (!FAT_TEST_LOCALRUN) {
            keyTabTemp.deleteOnExit();
        }

        Keytab keytab = Keytab.getInstance();

        List<KeytabEntry> entries = addKerberosKeysToKeytab(SPN, SPN_PASSWORD);

        keytab.setEntries(entries);
        keytab.write(keyTabTemp);

        libertyServerSPNKeytabFile = keyTabTemp.getAbsolutePath();
        KEYTAB_FILE_LOCATION = libertyServerSPNKeytabFile;

        Log.info(c, "createLibertyServerSPNKeytab", "Created keytab: " + libertyServerSPNKeytabFile);
        Log.info(c, "createLibertyServerSPNKeytab", "Keytab actual contents: " + FileUtils.readFile(libertyServerSPNKeytabFile));
    }

    /**
     * @return the libertyServerSPNKeytabFile
     */
    public static String getLibertyServerSPNKeytabFile() {
        return libertyServerSPNKeytabFile;
    }
}
