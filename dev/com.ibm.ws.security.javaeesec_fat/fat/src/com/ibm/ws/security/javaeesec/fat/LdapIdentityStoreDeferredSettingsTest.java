/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.javaeesec.fat;

import static com.ibm.ws.security.javaeesec.fat_helper.Constants.getRemoteUserFound;
import static com.ibm.ws.security.javaeesec.fat_helper.Constants.getUserPrincipalFound;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;
import com.ibm.ws.security.javaeesec.fat_helper.FATHelper;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;
import com.ibm.ws.security.javaeesec.identitystore.LdapIdentityStore;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import web.war.annotatedbasic.deferred.LdapSettingsBean;
import web.war.database.deferred.DatabaseSettingsBean;

/**
 * Test for {@link LdapIdentityStore} configured with deferred EL expressions.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class LdapIdentityStoreDeferredSettingsTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.ldapidstore.deferred.fat");
    protected static Class<?> logClass = LdapIdentityStoreDeferredSettingsTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
//    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);
    protected DefaultHttpClient httpclient;

    private static EmbeddedApacheDS ldapServer = null;

    // LDAP Partitions
    private static final String LDAP_ROOT_PARTITION = "o=ibm,c=us";
    private static final String LDAP_SUBTREE_PARTITION = "ou=subtree,o=ibm,c=us";

    // LDAP Users
    private static final String LDAP_USER1_UID = "ldapuser1";
    private static final String LDAP_USER1_PASSWORD = LDAP_USER1_UID + "pass";
    private static final String LDAP_USER2_UID = "ldapuser2";
    private static final String LDAP_USER2_PASSWORD = LDAP_USER2_UID + "pass";
    private static final String LDAP_USER3_UID = "ldapuser3";
    private static final String LDAP_USER3_PASSWORD = LDAP_USER3_UID + "pass";
    private static final String LDAP_USER4_UID = "ldapuser4";
    private static final String LDAP_USER4_PASSWORD = LDAP_USER4_UID + "pass";

    public LdapIdentityStoreDeferredSettingsTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        setupldapServer();

        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecAnnotatedBasicAuthServletDeferred.war", true, JAR_NAME, false, "web.jar.base",
                                               "web.war.annotatedbasic.deferred");

        myServer.startServer(true);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort()
                  + "/JavaEESecAnnotatedBasicAuthServletDeferred/JavaEESecAnnotatedBasicDeferred";

    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer("CWWKS1916W", "CWWKS3400W", "CWWKS3401E", "CWWKS3402E", "CWWKS3405W", "CWWKS3406W");
        } finally {
            if (ldapServer != null) {
                try {
                    ldapServer.stopService();
                } catch (Exception e) {
                    Log.error(logClass, "teardown", e, "LDAP server threw error while stopping. " + e.getMessage());
                }
            }
        }
    }

    /**
     * Configure the embedded LDAP server with a single LDAP user (jaspildapuser1).
     *
     * @throws Exception If there was an issue configuring the LDAP server.
     */
    private static void setupldapServer() throws Exception {
        ldapServer = new EmbeddedApacheDS("HTTPAuthLDAP");
        ldapServer.addPartition("test", LDAP_ROOT_PARTITION);
        ldapServer.startServer(Integer.parseInt(System.getProperty("ldap.1.port")));

        Entry entry = ldapServer.newEntry(LDAP_ROOT_PARTITION);
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        ldapServer.add(entry);

        entry = ldapServer.newEntry(LDAP_SUBTREE_PARTITION);
        entry.add("objectclass", "organizationalunit");
        entry.add("ou", "level2");
        ldapServer.add(entry);

        /*
         * Create LDAP users and groups.
         *
         * Users 1 and 2 are in the root of the main partition. Users 3 and 4 are in the
         * subtree. Groups 1 is in the main partition, while group 2 is in the sub tree.
         *
         * Users 1 and 3 have authorization directly. Users 2 and 4 only have
         * authorization through group membership.
         */
        final String LDAP_USER1_DN = "uid=" + LDAP_USER1_UID + "," + LDAP_ROOT_PARTITION;
        final String LDAP_USER2_DN = "uid=" + LDAP_USER2_UID + "," + LDAP_ROOT_PARTITION;
        final String LDAP_USER3_DN = "uid=" + LDAP_USER3_UID + "," + LDAP_SUBTREE_PARTITION;
        final String LDAP_USER4_DN = "uid=" + LDAP_USER4_UID + "," + LDAP_SUBTREE_PARTITION;
        final String LDAP_GROUP1_DN = "cn=ldapgroup1," + LDAP_ROOT_PARTITION;
        final String LDAP_GROUP2_DN = "cn=ldapgroup2," + LDAP_SUBTREE_PARTITION;

        entry = ldapServer.newEntry(LDAP_USER1_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", LDAP_USER1_UID);
        entry.add("sn", LDAP_USER1_UID + "sn");
        entry.add("cn", LDAP_USER1_UID + "cn");
        entry.add("userPassword", LDAP_USER1_PASSWORD);
        ldapServer.add(entry);

        entry = ldapServer.newEntry(LDAP_USER2_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("objectclass", "simulatedMicrosoftSecurityPrincipal");
        entry.add("uid", LDAP_USER2_UID);
        entry.add("samaccountname", LDAP_USER2_UID);
        entry.add("sn", LDAP_USER2_UID + "sn");
        entry.add("cn", LDAP_USER2_UID + "cn");
        entry.add("memberOf", LDAP_GROUP1_DN);
        entry.add("userPassword", LDAP_USER2_PASSWORD);
        ldapServer.add(entry);

        entry = ldapServer.newEntry(LDAP_USER3_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", LDAP_USER3_UID);
        entry.add("sn", LDAP_USER3_UID + "sn");
        entry.add("cn", LDAP_USER3_UID + "cn");
        entry.add("userPassword", LDAP_USER3_PASSWORD);
        ldapServer.add(entry);

        entry = ldapServer.newEntry(LDAP_USER4_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("objectclass", "simulatedMicrosoftSecurityPrincipal");
        entry.add("uid", LDAP_USER4_UID);
        entry.add("samaccountname", LDAP_USER4_UID);
        entry.add("sn", LDAP_USER4_UID + "sn");
        entry.add("cn", LDAP_USER4_UID + "cn");
        entry.add("memberOf", LDAP_GROUP2_DN);
        entry.add("userPassword", LDAP_USER4_PASSWORD);
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=ldapgroup1," + LDAP_ROOT_PARTITION);
        entry.add("objectclass", "groupofnames");
        entry.add("cn", "ldapgroup1");
        entry.add("member", LDAP_USER2_DN);
        ldapServer.add(entry);

        entry = ldapServer.newEntry("cn=ldapgroup2," + LDAP_SUBTREE_PARTITION);
        entry.add("objectclass", "groupofnames");
        entry.add("cn", "ldapgroup2");
        entry.add("member", LDAP_USER4_DN);
        ldapServer.add(entry);
    }

    @Before
    public void setupConnection() {
        // disable auto redirect.
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);

        httpclient = new DefaultHttpClient(httpParams);
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    public void resetConnection() {
        cleanupConnection();
        setupConnection();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify that the user responses are as expected.
     *
     * @param code1 Expected response code for ldapuser1.
     * @param code2 Expected response code for ldapuser2.
     * @param code3 Expected response code for ldapuser3.
     * @param code4 Expected response code for ldapuser4.
     * @throws Exception If there was an error processing the request.
     */
    private void verifyAuthorization(int code1, int code2, int code3, int code4) throws Exception {

        /* ldapuser1 */
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase, LDAP_USER1_UID, LDAP_USER1_PASSWORD, code1);
        if (code1 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + LDAP_USER1_UID, getRemoteUserFound + LDAP_USER1_UID);
        }
//        passwordChecker.checkForPasswordInAnyFormat(LDAP_USER1_PASSWORD); TODO Uncomment when ApacheDS logs are clean

        resetConnection();

        /* ldapuser2 */
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase, LDAP_USER2_UID, LDAP_USER2_PASSWORD, code2);
        if (code2 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + LDAP_USER2_UID, getRemoteUserFound + LDAP_USER2_UID);
        }
//        passwordChecker.checkForPasswordInAnyFormat(LDAP_USER2_PASSWORD); TODO Uncomment when ApacheDS logs are clean

        resetConnection();

        /* ldapuser3 */
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase, LDAP_USER3_UID, LDAP_USER3_PASSWORD, code3);
        if (code3 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + LDAP_USER3_UID, getRemoteUserFound + LDAP_USER3_UID);
        }
//        passwordChecker.checkForPasswordInAnyFormat(LDAP_USER3_PASSWORD); TODO Uncomment when ApacheDS logs are clean

        resetConnection();
        /* ldapuser4 */
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase, LDAP_USER4_UID, LDAP_USER4_PASSWORD, code4);
        if (code4 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + LDAP_USER4_UID, getRemoteUserFound + LDAP_USER4_UID);
        }
//        passwordChecker.checkForPasswordInAnyFormat(LDAP_USER4_PASSWORD); TODO Uncomment when ApacheDS logs are clean
    }

    /**
     * This test will verify that all users in the nominal configuration can authenticate and
     * are authorized to access the servlet.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - authorized via group</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void baselineTest() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        updateLdapSettingsBean(new HashMap<String, String>());

        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the bindDn setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (cannot bind with admin credentials to search for user)</li>
     * <li>ldapuser2 - unauthorized (cannot bind with admin credentials to search for user)</li>
     * <li>ldapuser3 - unauthorized (cannot bind with admin credentials to search for user)</li>
     * <li>ldapuser4 - unauthorized (cannot bind with admin credentials to search for user)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "javax.naming.AuthenticationException" })
    public void bindDn() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("bindDn", "uid=nosuchuser,o=ibm,c=us");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a an bindDn EL expression that resolves to null is handled.
     * The bindDn will be defaulted to an empty string resulting in an anonymous bind.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (anonymous bind, so no access to search for user)</li>
     * <li>ldapuser2 - unauthorized (anonymous bind, so no access to search for user)</li>
     * <li>ldapuser3 - unauthorized (anonymous bind, so no access to search for user)</li>
     * <li>ldapuser4 - unauthorized (anonymous bind, so no access to search for user)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "javax.naming.NoPermissionException" })
    public void bindDn_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("bindDn", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'bindDn' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the bindDnPassword setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (cannot bind with admin credentials to search for user)</li>
     * <li>ldapuser2 - unauthorized (cannot bind with admin credentials to search for user)</li>
     * <li>ldapuser3 - unauthorized (cannot bind with admin credentials to search for user)</li>
     * <li>ldapuser4 - unauthorized (cannot bind with admin credentials to search for user)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "javax.naming.AuthenticationException" })
    public void bindDnPassword() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("bindDnPassword", "badbinddnpassword");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a an bindDnPassword EL expression that resolves to null is handled.
     * The bindDnPassword will be defaulted to an empty string resulting in an anonymous bind.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (anonymous bind, so no access to search for user)</li>
     * <li>ldapuser2 - unauthorized (anonymous bind, so no access to search for user)</li>
     * <li>ldapuser3 - unauthorized (anonymous bind, so no access to search for user)</li>
     * <li>ldapuser4 - unauthorized (anonymous bind, so no access to search for user)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void bindDnPassword_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("bindDnPassword", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(myServer);
        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'bindDnPassword' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the callerBaseDn setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (callerBaseDn generates non-existent caller DN)</li>
     * <li>ldapuser2 - unauthorized (callerBaseDn generates non-existent caller DN)</li>
     * <li>ldapuser3 - unauthorized (callerBaseDn generates non-existent caller DN)</li>
     * <li>ldapuser4 - unauthorized (callerBaseDn generates non-existent caller DN)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC("javax.naming.AuthenticationException")
    public void callerBaseDn() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("callerBaseDn", "o=ibm,c=uk");
        overrides.put("callerSearchBase", ""); // Needs to be empty to use callerBaseDn
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a an callerBaseDn EL expression that resolves to null is handled.
     * The callerBaseDn will be defaulted to an empty string resulting in an InvalidNameException on search.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (null callerBaseDn generates non-existent caller DN)</li>
     * <li>ldapuser2 - unauthorized (null callerBaseDn generates non-existent caller DN)</li>
     * <li>ldapuser3 - unauthorized (null callerBaseDn generates non-existent caller DN)</li>
     * <li>ldapuser4 - unauthorized (null callerBaseDn generates non-existent caller DN)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC("javax.naming.InvalidNameException")
    public void callerBaseDn_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("callerBaseDn", "NULL");
        overrides.put("callerSearchBase", ""); // Needs to be empty to use callerBaseDn
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'callerBaseDn' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the callerNameAttribute setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (cannot find user with invalid attribute)</li>
     * <li>ldapuser2 - unauthorized (cannot find user with invalid attribute)</li>
     * <li>ldapuser3 - unauthorized (cannot find user with invalid attribute)</li>
     * <li>ldapuser4 - unauthorized (cannot find user with invalid attribute)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void callerNameAttribute() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("callerNameAttribute", "badcallernameattribute");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a an callerNameAttribute EL expression that resolves to null is handled.
     * The callerNameAttribute will be defaulted to 'uid'.
     *
     * <ul>
     * <li>ldapuser1 - authorized</li>
     * <li>ldapuser2 - authorized</li>
     * <li>ldapuser3 - authorized</li>
     * <li>ldapuser4 - authorized</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void callerNameAttribute_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("callerNameAttribute", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'callerNameAttribute' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the callerSearchBase setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (cannot find user with invalid search base)</li>
     * <li>ldapuser2 - unauthorized (cannot find user with invalid search base)</li>
     * <li>ldapuser3 - unauthorized (cannot find user with invalid search base)</li>
     * <li>ldapuser4 - unauthorized (cannot find user with invalid search base)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "javax.naming.NameNotFoundException" })
    public void callerSearchBase() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("callerSearchBase", "o=ibm,c=uk");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a an callerSearchBase EL expression that resolves to null is handled.
     * The callerSearchBase will be defaulted to an empty string resulting in InvalidNameException on search.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (cannot find user with invalid search base)</li>
     * <li>ldapuser2 - unauthorized (cannot find user with invalid search base)</li>
     * <li>ldapuser3 - unauthorized (cannot find user with invalid search base)</li>
     * <li>ldapuser4 - unauthorized (cannot find user with invalid search base)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC("javax.naming.InvalidNameException")
    public void callerSearchBase_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("callerSearchBase", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'callerSearchBase' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the callerSearchFilter setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (cannot find user with invalid search filter)</li>
     * <li>ldapuser2 - unauthorized (cannot find user with invalid search filter)</li>
     * <li>ldapuser3 - unauthorized (cannot find user with invalid search filter)</li>
     * <li>ldapuser4 - unauthorized (cannot find user with invalid search filter)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void callerSearchFilter() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("callerSearchFilter", "(&(uid=%s)(objectclass=nosuchclass))");
        updateLdapSettingsBean(overrides);
        verifyAuthorization(SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a an callerSearchFilter EL expression that resolves to null is handled.
     * The callerSearchFilter will be defaulted to an empty string which will not match any users.
     *
     * Having an empty callerSearchFilter won't necessarily work, based on configuration, but it won't
     * automatically fail. We build the filter based on callerNameAttribute and the callerDn.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized</li>
     * <li>ldapuser2 - unauthorized</li>
     * <li>ldapuser3 - unauthorized</li>
     * <li>ldapuser4 - unauthorized</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void callerSearchFilter_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("callerSearchFilter", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the callerSearchScope setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - unauthorized (user is out of search scope, cannot authenticate)</li>
     * <li>ldapuser4 - unauthorized (user is out of search scope, cannot authenticate)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void callerSearchScope() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("callerSearchScope", "ONE_LEVEL");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_OK, SC_OK, SC_UNAUTHORIZED, SC_UNAUTHORIZED);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a an callerSearchScope EL expression that resolves to null is handled.
     * The callerSearchScope will be defaulted to SUBTREE.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - unauthorized (user is out of search scope, cannot authenticate)</li>
     * <li>ldapuser4 - unauthorized (user is out of search scope, cannot authenticate)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void callerSearchScope_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("callerSearchScope", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'callerSearchScope/callerSearchScopeExpression' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the groupMemberAttribute setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - unauthorized (group membership not captured due to bad groupMemberAttribute)</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - unauthorized (group membership not captured due to bad groupMemberAttribute)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupMemberAttribute() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupMemberAttribute", "badgroupmemberattribute");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_OK, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a an groupMemberAttribute EL expression that resolves to null is handled.
     * The groupMemberAttribute will be defaulted to 'member'.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - authorized via group</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupMemberAttribute_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupMemberAttribute", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'groupMemberAttribute' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the groupMemberOfAttribute setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - authorized via group</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupMemberOfAttribute_1() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupMemberOfAttribute", "memberof");
        overrides.put("groupSearchBase", ""); // Either groupSearchBase or groupSearchFilter needs to be empty to get into memberOf checks
        overrides.put("groupSearchFilter", ""); // Either groupSearchBase or groupSearchFilter needs to be empty to get into memberOf checks
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the groupMemberOfAttribute setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - unauthorized (group membership not captured due to bad groupMemberOfAttribute)</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - unauthorized (group membership not captured due to bad groupMemberOfAttribute)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupMemberOfAttribute_2() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupMemberOfAttribute", "badgroupmemberofattribute");
        overrides.put("groupSearchBase", ""); // Either groupSearchBase or groupSearchFilter needs to be empty to get into memberOf checks
        overrides.put("groupSearchFilter", ""); // Either groupSearchBase or groupSearchFilter needs to be empty to get into memberOf checks
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_OK, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a an groupMemberOfAttribute EL expression that resolves to null is handled.
     * The groupMemberOfAttribute will be defaulted to 'memberOf'.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - authorized via group</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupMemberOfAttribute_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupMemberOfAttribute", "NULL");
        overrides.put("groupSearchBase", ""); // Either groupSearchBase or groupSearchFilter needs to be empty to get into memberOf checks
        overrides.put("groupSearchFilter", ""); // Either groupSearchBase or groupSearchFilter needs to be empty to get into memberOf checks
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'groupMemberOfAttribute' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the groupNameAttribute setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - unauthorized (group membership not captured due to bad groupNameAttribute)</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - unauthorized (group membership not captured due to bad groupNameAttribute)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupNameAttribute() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupNameAttribute", "badgroupnameattribute");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_OK, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a groupNameAttribute EL expression that resolves to null is handled.
     * The groupNameAttribute will be defaulted to 'cn'.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - unauthorized (group membership not captured due to bad groupNameAttribute)</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - unauthorized (group membership not captured due to bad groupNameAttribute)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupNameAttribute_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupNameAttribute", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'groupNameAttribute' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the groupSearchBase setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - unauthorized (group membership not captured due to bad groupSearchBase)</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - unauthorized (group membership not captured due to bad groupSearchBase)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "javax.naming.NameNotFoundException" })
    public void groupSearchBase() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupSearchBase", "o=ibm,c=uk");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a groupSearchBase EL expression that resolves to null is handled.
     * The groupSearchBase will be defaulted to an empty string.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - unauthorized (group membership not captured due to bad groupSearchBase)</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - unauthorized (group membership not captured due to bad groupSearchBase)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupSearchBase_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupSearchBase", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_OK, SC_FORBIDDEN);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'groupSearchBase' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the groupSearchFilter setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - unauthorized (group membership not captured due to bad groupSearchFilter)</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - unauthorized (group membership not captured due to bad groupSearchFilter)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupSearchFilter() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupSearchFilter", "(objectclass=nosuchclass)");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_OK, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a groupSearchFilter EL expression that resolves to null is handled.
     * The groupSearchFilter will be defaulted to an empty string.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - unauthorized (group membership not captured due to bad groupSearchFilter)</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - unauthorized (group membership not captured due to bad groupSearchFilter)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupSearchFilter_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupSearchFilter", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_OK, SC_FORBIDDEN);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'groupSearchFilter' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the groupSearchScope setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - unauthorized (authorized group out of search scope)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupSearchScope() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupSearchScope", "ONE_LEVEL");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a groupSearchScope EL expression that resolves to null is handled.
     * The groupSearchScope will be defaulted to SUBTREE.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - authorized via group</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void groupSearchScope_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupSearchScope", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'groupSearchScope/groupSearchScopeExpression' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the priority setting via a deferred EL expression.
     *
     * <ul>
     * <li>DB_USER1 - authorized</li>
     * <li>DB_USER2 - authorized</li>
     * <li>DB_USER3 - unauthorized (not authorized by user or group)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @Ignore("Test hangs on reloadApplications() in remote buids but not on local builds")
    public void priority() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("priority", "100");
        updateLdapSettingsBean(overrides);

        /*
         * TODO We reload the applications since Java 8 doesn't work with injected entry/exit trace yet.
         * When it does, we can remove this.
         */
        FATHelper.reloadApplications(server, Stream.of("DatabaseIdstoreDeferred").collect(Collectors.toCollection(HashSet::new)));;
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInTrace("IdentityStore from module BeanManager.*priority : 100");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a priority EL expression that resolves to null is handled.
     * The priority will be defaulted to 80.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - authorized via group</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @Ignore("Test hangs on reloadApplications() in remote buids but not on local builds")
    public void priority_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("priority", "NULL");
        updateLdapSettingsBean(overrides);

        /*
         * TODO We reload the applications since Java 8 doesn't work with injected entry/exit trace yet.
         * When it does, we can remove this.
         */
        FATHelper.reloadApplications(server, Stream.of("DatabaseIdstoreDeferred").collect(Collectors.toCollection(HashSet::new)));;
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'priority/priorityExpression' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the readTimeout setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - authorized via group</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void readTimeout() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("readTimeout", "100");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInTrace("searchScope: 2, timeLimit: 100");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a readTimeout EL expression that resolves to null is handled.
     * The readTimeout will be defaulted to 0.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - authorized via group</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void readTimeout_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("readTimeout", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'readTimeout/readTimeoutExpression' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the url setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (cannot connect to LDAP server to authenticate)</li>
     * <li>ldapuser2 - unauthorized (cannot connect to LDAP server to authenticate)</li>
     * <li>ldapuser3 - unauthorized (cannot connect to LDAP server to authenticate)</li>
     * <li>ldapuser4 - unauthorized (cannot connect to LDAP server to authenticate)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalStateException", "javax.naming.CommunicationException" })
    public void url() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("url", "ldap://nosuchhost");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a url EL expression that resolves to null is handled.
     * The url will be defaulted to an empty string resulting in failure to connect.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (cannot connect to LDAP server to authenticate)</li>
     * <li>ldapuser2 - unauthorized (cannot connect to LDAP server to authenticate)</li>
     * <li>ldapuser3 - unauthorized (cannot connect to LDAP server to authenticate)</li>
     * <li>ldapuser4 - unauthorized (cannot connect to LDAP server to authenticate)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    //@Test - Temporary disabled
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void url_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("url", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'url' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the useFor setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized</li>
     * <li>ldapuser2 - unauthorized (identity store doesn't support PROVIDE_GROUPS)</li>
     * <li>ldapuser3 - authorized</li>
     * <li>ldapuser4 - unauthorized (identity store doesn't support PROVIDE_GROUPS)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useFor_1() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("useFor", "VALIDATE");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_OK, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the useFor setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - authorized via group</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useFor_2() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("useFor", "");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'useFor/useForExpression' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a useFor EL expression that resolves to null is handled.
     * The useFor will be defaulted to both VALIDATE and PROVIDE_GROUPS.
     *
     * <ul>
     * <li>ldapuser1 - authorized via user</li>
     * <li>ldapuser2 - authorized via group</li>
     * <li>ldapuser3 - authorized via user</li>
     * <li>ldapuser4 - authorized via group</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useFor_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("useFor", "NULL");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'useFor/useForExpression' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Update the {@link LdapSettingsBean} settings that can be read back by the deferred EL expressions
     * set in the servlet's annotations.
     *
     * @param overrides The properties to override the default value(s) of.
     * @throws IOException If there was an error writing to the backing file.
     */
    private void updateLdapSettingsBean(Map<String, String> overrides) throws IOException {

        Properties props = new Properties();
        props.put("bindDn", "uid=" + LDAP_USER1_UID + "," + LDAP_ROOT_PARTITION);
        props.put("bindDnPassword", LDAP_USER1_PASSWORD);
        props.put("callerBaseDn", "");
        props.put("callerNameAttribute", "uid");
        props.put("callerSearchBase", LDAP_ROOT_PARTITION);
        props.put("callerSearchFilter", "(&(objectclass=person)(uid=%s))");
        props.put("callerSearchScope", "SUBTREE");
        props.put("groupMemberAttribute", "member");
        props.put("groupMemberOfAttribute", "");
        props.put("groupNameAttribute", "cn");
        props.put("groupSearchBase", LDAP_ROOT_PARTITION);
        props.put("groupSearchFilter", "(objectclass=groupofnames)");
        props.put("groupSearchScope", "SUBTREE");
        props.put("priority", "0");
        props.put("readTimeout", "0");
        props.put("url", "ldap://localhost:" + System.getProperty("ldap.1.port"));
        props.put("useFor", "VALIDATE PROVIDE_GROUPS");

        props.putAll(overrides);

        FileOutputStream fout = new FileOutputStream(server.getServerRoot() + "/LdapSettingsBean.props");
        props.store(fout, "");
        fout.close();

        if (!overrides.isEmpty()) {
            for (int i = 0; i < 3; i++) { // if the build machines are struggling, we can have timing issues reading in updated values.
                Properties checkProps = new Properties();
                checkProps.load(new FileReader(server.getServerRoot() + "/LdapSettingsBean.props"));

                boolean allprops = true;
                for (String prop : overrides.keySet()) {
                    String fileProp = (String) checkProps.get(prop);
                    if (fileProp == null) {
                        Log.info(DatabaseSettingsBean.class, "updateLdapSettingsBean", "could not find " + prop + " in LdapSettingsBean.props");
                        allprops = false;
                        break;
                    } else if (!fileProp.equals(overrides.get(prop))) {
                        Log.info(DatabaseSettingsBean.class, "updateLdapSettingsBean", "did not change " + prop + " to " + overrides.get(prop) + " yet.");
                        allprops = false;
                        break;
                    } else {
                        Log.info(DatabaseSettingsBean.class, "updateLdapSettingsBean", prop + " set to " + fileProp);
                    }
                }

                if (allprops) {
                    Log.info(DatabaseSettingsBean.class, "updateLdapSettingsBean", "LdapSettingsBean.props are good.");
                    break;
                }

                if (i == 3) {
                    throw new IllegalStateException("Failed to update LdapSettingsBean.props for EL testing");
                }

                Log.info(DatabaseSettingsBean.class, "updateLdapSettingsBean", "sleep and check LdapSettingsBean.props again.");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
            }
        }
    }
}
