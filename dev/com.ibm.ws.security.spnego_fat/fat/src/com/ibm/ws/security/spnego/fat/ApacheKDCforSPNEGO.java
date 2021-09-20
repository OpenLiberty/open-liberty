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
package com.ibm.ws.security.spnego.fat;

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
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.ApacheDSandKDC;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FileUtils;

@RunWith(FATRunner.class)
public class ApacheKDCforSPNEGO extends ApacheDSandKDC {

    private static final Class<?> c = ApacheKDCforSPNEGO.class;

    public static String SPN;

    public static String SPN_PASSWORD = "httppwd";

    private static String spnegoUserDN;

    public static String canonicalHostname = "canhostname1";

    private static String SpnegoSPNKeytabFile = null;

    //ADDED ON
    //KDC Users
    public static String KRB5_USER = bindUserName;
    public static String KRB5_USER_PWD = bindPassword;

    public static String KRB5_USER1 = KRB5_USER; //"user1";
    public static String KRB5_USER1_PWD = KRB5_USER_PWD; //"user1pwd";
    public static String KRB5_USER2 = KRB5_USER; //"user2";
    public static String KRB5_USER2_PWD = KRB5_USER_PWD; //"user2pwd";

    public static String COMMON_TOKEN_USER = KRB5_USER1;
    public static String COMMON_TOKEN_USER_PWD = KRB5_USER1_PWD;
    public static boolean COMMON_TOKEN_USER_IS_EMPLOYEE = true;
    public static boolean COMMON_TOKEN_USER_IS_MANAGER = false;

    public static String COMMON_SPNEGO_TOKEN = null;
    public static String KEYTAB_FILE_LOCATION = null;
    public static long COMMON_TOKEN_CREATION_DATE = 0;
    public static final double TOKEN_REFRESH_LIFETIME_SECONDS = 180;
    public static boolean LOCALHOST_DEFAULT_IP_ADDRESS = true; //127.0.0.1       localhost

    public static boolean IBM_JDK_V8_LOWER = false;
    public static boolean OTHER_SUPPORT_JDKS = false;
    public static boolean SUN_ORACLE_JDK_V8_HIGHER = false;
    public static boolean IBM_HYBRID_JDK = false;

    public static String serverShortHostName = "shortHostName";

    public static String KDC_REALM = DOMAIN;

    @BeforeClass
    public static void setup() throws Exception {

        if (!InitClass.RUN_TESTS)
            return;

        WHICH_FAT = "SPNEGO";

        bindUserName = "user1";
        bindPassword = "user1pwd";
        bindPrincipalName = bindUserName + "@" + DOMAIN;
        KRB5_USER = bindUserName;
        KRB5_USER_PWD = bindPassword;
        KRB5_USER1 = KRB5_USER;
        KRB5_USER1_PWD = KRB5_USER_PWD;
        COMMON_TOKEN_USER = KRB5_USER1;

        KRB5_USER2 = "user2";
        KRB5_USER2_PWD = "user2pwd";

        spnegoUserDN = "uid=" + canonicalHostname + "," + BASE_DN;

        SPN = "HTTP/" + canonicalHostname + "@" + DOMAIN;

        setupService();

        ApacheKDCCommonTest.setGlobalLoggingLevel(Level.INFO);

        createPrincipal(KRB5_USER2, KRB5_USER2_PWD);

        createSpnegoSPNEntry();

        createSpnegoSPNKeytab();
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
    public static void createSpnegoSPNEntry() throws Exception {
        String methodName = "createSpnegoSPNEntry";
        Log.info(c, methodName, "Creating KDC user entries");

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

        Log.info(c, methodName, "Created " + entry.getDn());
    }

    /**
     * Create a keytab file with the default bindUser and password
     *
     * @throws Exception
     */
    public static void createSpnegoSPNKeytab() throws Exception {
        String methodName = "createSpnegoSPNKeytab";
        Log.info(c, methodName, "Creating keytab for " + SPN);
        File keyTabTemp = File.createTempFile(canonicalHostname + "_http", ".keytab");
        if (!FAT_TEST_LOCALRUN) {
            keyTabTemp.deleteOnExit();
        }

        Keytab keytab = Keytab.getInstance();

        List<KeytabEntry> entries = addKerberosKeysToKeytab(SPN, SPN_PASSWORD);

        keytab.setEntries(entries);
        keytab.write(keyTabTemp);

        SpnegoSPNKeytabFile = keyTabTemp.getAbsolutePath();
        KEYTAB_FILE_LOCATION = SpnegoSPNKeytabFile;

        Log.info(c, methodName, "Created keytab: " + SpnegoSPNKeytabFile);
        Log.info(c, methodName, "Keytab actual contents: " + FileUtils.readFile(SpnegoSPNKeytabFile));
    }

    /**
     * @return the libertyServerSPNKeytabFile
     */
    public static String getLibertyServerSPNKeytabFile() {
        return SpnegoSPNKeytabFile;
    }
}
