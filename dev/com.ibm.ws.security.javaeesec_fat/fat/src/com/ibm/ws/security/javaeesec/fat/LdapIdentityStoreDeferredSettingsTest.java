/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.http.impl.client.DefaultHttpClient;
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
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;
import com.ibm.ws.security.javaeesec.identitystore.LdapIdentityStore;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import web.war.annotatedbasic.deferred.LdapSettingsBean;

/**
 * Test for {@link LdapIdentityStore} configured with deferred EL expressions.
 */
@MinimumJavaLevel(javaLevel = 1.7)
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

        myServer.stopServer();

        if (ldapServer != null) {
            try {
                ldapServer.stopService();
            } catch (Exception e) {
                Log.error(logClass, "teardown", e, "LDAP server threw error while stopping. " + e.getMessage());
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
        httpclient = new DefaultHttpClient();
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    private void verifyAuthorization(int code1, int code2, int code3, int code4) throws Exception {

        /* LDAP_USER1 */
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase, LDAP_USER1_UID, LDAP_USER1_PASSWORD, code1);
        if (code1 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + LDAP_USER1_UID, getRemoteUserFound + LDAP_USER1_UID);
        }
//        passwordChecker.checkForPasswordInAnyFormat(LDAP_USER1_PASSWORD); TODO Uncomment when ApacheDS logs are clean

        /* LDAP_USER2 */
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase, LDAP_USER2_UID, LDAP_USER2_PASSWORD, code2);
        if (code2 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + LDAP_USER2_UID, getRemoteUserFound + LDAP_USER2_UID);
        }
//        passwordChecker.checkForPasswordInAnyFormat(LDAP_USER2_PASSWORD); TODO Uncomment when ApacheDS logs are clean

        /* LDAP_USER3 */
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase, LDAP_USER3_UID, LDAP_USER3_PASSWORD, code3);
        if (code3 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + LDAP_USER3_UID, getRemoteUserFound + LDAP_USER3_UID);
        }
//        passwordChecker.checkForPasswordInAnyFormat(LDAP_USER3_PASSWORD); TODO Uncomment when ApacheDS logs are clean

        /* LDAP_USER4 */
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
    @ExpectedFFDC("javax.naming.AuthenticationException")
    public void bindDN() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("bindDn", "uid=nosuchuser,o=ibm,c=us");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

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
    @ExpectedFFDC("javax.naming.AuthenticationException")
    public void bindDnPassword() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("bindDnPassword", "badbinddnpassword");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

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

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

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

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

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
    @ExpectedFFDC("javax.naming.NameNotFoundException")
    public void callerSearchBase() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("callerSearchBase", "o=ibm,c=uk");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

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
        overrides.put("callerSearchFilter", "(objectclass=nosuchclass)");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

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

        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN, SC_FORBIDDEN);

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
    public void groupMemberOfAttribute1() throws Exception {
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
    public void groupMemberOfAttribute2() throws Exception {
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
    @Ignore("Enable this test when the groupNameAttribute is used in the LdapIdentityStore")
    public void groupNameAttribute() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupNameAttribute", "badgroupnameattribute");
        updateLdapSettingsBean(overrides);

        /*
         * TODO We don't use groupNameAttribute for anything...
         */
        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_FORBIDDEN, SC_OK);

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
    @ExpectedFFDC("javax.naming.NameNotFoundException")
    public void groupSearchBase() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("groupSearchBase", "o=ibm,c=uk");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_OK, SC_FORBIDDEN);

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

        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK); // TODO Maybe need to check logs for timeout value?

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
    @ExpectedFFDC("javax.naming.CommunicationException")
    public void url() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("url", "ldap://nosuchhost");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the url setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - authorized</li>
     * <li>ldapuser2 - unauthorized (identity store does't support PROVIDE_GROUPS)</li>
     * <li>ldapuser3 - authorized</li>
     * <li>ldapuser4 - unauthorized (identity store does't support PROVIDE_GROUPS)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useFor1() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("useFor", "VALIDATE");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_OK, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the url setting via a deferred EL expression.
     *
     * <ul>
     * <li>ldapuser1 - unauthorized (identity store does't support VALIDATE)</li>
     * <li>ldapuser2 - unauthorized (identity store does't support PROVIDE_GROUPS)</li>
     * <li>ldapuser3 - unauthorized (identity store does't support VALIDATE)</li>
     * <li>ldapuser4 - unauthorized (identity store does't support PROVIDE_GROUPS)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void useFor2() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("useFor", "");
        updateLdapSettingsBean(overrides);

        verifyAuthorization(SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN, SC_FORBIDDEN);

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
        props.put("callerSearchFilter", "(objectclass=person)");
        props.put("callerSearchScope", "SUBTREE");
        props.put("groupMemberAttribute", "member");
        props.put("groupMemberOfAttribute", "");
        props.put("groupNameAttribute", "cn");
        props.put("groupSearchBase", LDAP_ROOT_PARTITION);
        props.put("groupSearchFilter", "(objectclass=groupofnames)");
        props.put("groupSearchScope", "SUBTREE");
        props.put("readTimeout", "0");
        props.put("url", "ldap://localhost:10389");
        props.put("useFor", "VALIDATE PROVIDE_GROUPS");

        props.putAll(overrides);

        FileOutputStream fout = new FileOutputStream(server.getServerRoot() + "/LdapSettingsBean.props");
        props.store(fout, "");
        fout.close();
    }
}
