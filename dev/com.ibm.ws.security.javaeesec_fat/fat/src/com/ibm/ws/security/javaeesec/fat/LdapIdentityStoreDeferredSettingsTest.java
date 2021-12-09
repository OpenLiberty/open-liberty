/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
import com.ibm.ws.com.unboundid.InMemoryLDAPServer;
import com.ibm.ws.security.javaeesec.fat_helper.FATHelper;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;
import com.ibm.ws.security.javaeesec.identitystore.LdapIdentityStore;
import com.unboundid.ldap.sdk.Entry;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;
import web.war.annotatedbasic.deferred.LdapSettingsBean;

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

    private static InMemoryLDAPServer ldapServer = null;

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

    //LDAP bind pass
    private final String password = "ldapuser1pass";

    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);

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
                    ldapServer.shutDown();
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
        ldapServer = new InMemoryLDAPServer(false, Integer.getInteger("ldap.1.port", 10389), 0, false, LDAP_ROOT_PARTITION);

        Entry entry = new Entry(LDAP_ROOT_PARTITION);
        entry.addAttribute("objectclass", "organization");
        entry.addAttribute("o", "ibm");
        ldapServer.add(entry);

        entry = new Entry(LDAP_SUBTREE_PARTITION);
        entry.addAttribute("objectclass", "organizationalunit");
        entry.addAttribute("ou", "level2");
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

        entry = new Entry(LDAP_USER1_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("objectclass", "person");
        entry.addAttribute("uid", LDAP_USER1_UID);
        entry.addAttribute("sn", LDAP_USER1_UID + "sn");
        entry.addAttribute("cn", LDAP_USER1_UID + "cn");
        entry.addAttribute("userPassword", LDAP_USER1_PASSWORD);
        ldapServer.add(entry);

        entry = new Entry(LDAP_USER2_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("objectclass", "person");
        entry.addAttribute("objectclass", "simulatedMicrosoftSecurityPrincipal");
        entry.addAttribute("uid", LDAP_USER2_UID);
        entry.addAttribute("samaccountname", LDAP_USER2_UID);
        entry.addAttribute("sn", LDAP_USER2_UID + "sn");
        entry.addAttribute("cn", LDAP_USER2_UID + "cn");
        entry.addAttribute("memberOf", LDAP_GROUP1_DN);
        entry.addAttribute("userPassword", LDAP_USER2_PASSWORD);
        ldapServer.add(entry);

        entry = new Entry(LDAP_USER3_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("objectclass", "person");
        entry.addAttribute("uid", LDAP_USER3_UID);
        entry.addAttribute("sn", LDAP_USER3_UID + "sn");
        entry.addAttribute("cn", LDAP_USER3_UID + "cn");
        entry.addAttribute("userPassword", LDAP_USER3_PASSWORD);
        ldapServer.add(entry);

        entry = new Entry(LDAP_USER4_DN);
        entry.addAttribute("objectclass", "inetorgperson");
        entry.addAttribute("objectclass", "person");
        entry.addAttribute("objectclass", "simulatedMicrosoftSecurityPrincipal");
        entry.addAttribute("uid", LDAP_USER4_UID);
        entry.addAttribute("samaccountname", LDAP_USER4_UID);
        entry.addAttribute("sn", LDAP_USER4_UID + "sn");
        entry.addAttribute("cn", LDAP_USER4_UID + "cn");
        entry.addAttribute("memberOf", LDAP_GROUP2_DN);
        entry.addAttribute("userPassword", LDAP_USER4_PASSWORD);
        ldapServer.add(entry);

        entry = new Entry("cn=ldapgroup1," + LDAP_ROOT_PARTITION);
        entry.addAttribute("objectclass", "groupofnames");
        entry.addAttribute("cn", "ldapgroup1");
        entry.addAttribute("member", LDAP_USER2_DN);
        ldapServer.add(entry);

        entry = new Entry("cn=ldapgroup2," + LDAP_SUBTREE_PARTITION);
        entry.addAttribute("objectclass", "groupofnames");
        entry.addAttribute("cn", "ldapgroup2");
        entry.addAttribute("member", LDAP_USER4_DN);
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
     * @param code1 Expected response code for ldapuser1. Skip call for ldapuser1 if null.
     * @param code2 Expected response code for ldapuser2. Skip call for ldapuser2 if null.
     * @param code3 Expected response code for ldapuser3. Skip call for ldapuser3 if null.
     * @param code4 Expected response code for ldapuser4. Skip call for ldapuser4 if null.
     * @throws Exception If there was an error processing the request.
     */
    private void verifyAuthorization(Integer code1, Integer code2, Integer code3, Integer code4) throws Exception {

        String response;

        /* ldapuser1 */
        if (code1 != null) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase, LDAP_USER1_UID, LDAP_USER1_PASSWORD, code1);
            if (code1 == SC_OK) {
                verifyUserResponse(response, getUserPrincipalFound + LDAP_USER1_UID, getRemoteUserFound + LDAP_USER1_UID);
            }
//        passwordChecker.checkForPasswordInAnyFormat(LDAP_USER1_PASSWORD); TODO Uncomment when ApacheDS logs are clean

            resetConnection();
        }

        /* ldapuser2 */
        if (code2 != null) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase, LDAP_USER2_UID, LDAP_USER2_PASSWORD, code2);
            if (code2 == SC_OK) {
                verifyUserResponse(response, getUserPrincipalFound + LDAP_USER2_UID, getRemoteUserFound + LDAP_USER2_UID);
            }
//        passwordChecker.checkForPasswordInAnyFormat(LDAP_USER2_PASSWORD); TODO Uncomment when ApacheDS logs are clean

            resetConnection();
        }

        /* ldapuser3 */
        if (code3 != null) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase, LDAP_USER3_UID, LDAP_USER3_PASSWORD, code3);
            if (code3 == SC_OK) {
                verifyUserResponse(response, getUserPrincipalFound + LDAP_USER3_UID, getRemoteUserFound + LDAP_USER3_UID);
            }
//        passwordChecker.checkForPasswordInAnyFormat(LDAP_USER3_PASSWORD); TODO Uncomment when ApacheDS logs are clean

            resetConnection();
        }

        /* ldapuser4 */
        if (code4 != null) {
            response = executeGetRequestBasicAuthCreds(httpclient, urlBase, LDAP_USER4_UID, LDAP_USER4_PASSWORD, code4);
            if (code4 == SC_OK) {
                verifyUserResponse(response, getUserPrincipalFound + LDAP_USER4_UID, getRemoteUserFound + LDAP_USER4_UID);
            }
//        passwordChecker.checkForPasswordInAnyFormat(LDAP_USER4_PASSWORD); TODO Uncomment when ApacheDS logs are clean

            resetConnection();
        }
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

        passwordChecker.checkForPasswordInAnyFormat(password);
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

        passwordChecker.checkForPasswordInAnyFormat(password);
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

        passwordChecker.checkForPasswordInAnyFormat(password);
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
    @Ignore("Test hangs on reloadApplications() in remote builds but not on local builds")
    public void priority() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("priority", "100");
        updateLdapSettingsBean(overrides);

        /*
         * TODO We reload the applications since Java 8 doesn't work with injected entry/exit trace yet.
         * When it does, we can remove this.
         */
        FATHelper.reloadApplications(server, Stream.of("JavaEESecAnnotatedBasicAuthServletDeferred").collect(Collectors.toCollection(HashSet::new)));;
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInTrace("IdentityStore from module BeanManager.*priority : 100");

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
    public void useFor() throws Exception {
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
    public void useFor_EMPTY() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("useFor", "");
        updateLdapSettingsBean(overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_OK, SC_OK);
        server.findStringsInLogsAndTraceUsingMark("CWWKS1916W: An error occurs when the program resolves the 'useFor/useForExpression' configuration for the identity store.");

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
        props.put("maxResults", "1000");
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
                FileReader fr = new FileReader(server.getServerRoot() + "/LdapSettingsBean.props");
                try {
                    checkProps.load(fr);
                } finally {
                    if (fr != null) {
                        fr.close();
                    }
                }

                boolean allprops = true;
                for (String prop : overrides.keySet()) {
                    String fileProp = (String) checkProps.get(prop);
                    if (fileProp == null) {
                        Log.info(logClass, "updateLdapSettingsBean", "could not find " + prop + " in LdapSettingsBean.props");
                        allprops = false;
                        break;
                    } else if (!fileProp.equals(overrides.get(prop))) {
                        Log.info(LdapSettingsBean.class, "updateLdapSettingsBean", "did not change " + prop + " to " + overrides.get(prop) + " yet.");
                        allprops = false;
                        break;
                    } else {
                        Log.info(logClass, "updateLdapSettingsBean", prop + " set to " + fileProp);
                    }
                }

                if (allprops) {
                    Log.info(logClass, "updateLdapSettingsBean", "LdapSettingsBean.props are good.");
                    break;
                }

                if (i == 3) {
                    throw new IllegalStateException("Failed to update LdapSettingsBean.props for EL testing");
                }

                Log.info(logClass, "updateLdapSettingsBean", "sleep and check LdapSettingsBean.props again.");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
            }
        }
    }

    /**
     * This test will verify that when initialization of the LdapIdentityStoreDefinitionWrapper fails for deferred EL expressions, that we will attempt to re-evaluate the
     * expression when requesting the value at a later time.
     *
     * <p/>This also verifies handling of NULL values that are returned by a bean in a deferred EL expression.
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "java.lang.IllegalArgumentException", "javax.naming.NoPermissionException", "java.lang.IllegalStateException" })
    public void testReevaluation() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put("bindDn", "NULL");
        overrides.put("bindDnPassword", "NULL");
        overrides.put("callerBaseDn", "NULL");
        overrides.put("callerNameAttribute", "NULL");
        overrides.put("callerSearchBase", "NULL");
        overrides.put("callerSearchFilter", "NULL");
        overrides.put("callerSearchScope", "NULL");
        overrides.put("groupMemberAttribute", "NULL");
        overrides.put("groupMemberOfAttribute", "NULL");
        overrides.put("groupNameAttribute", "NULL");
        overrides.put("groupSearchBase", "NULL");
        overrides.put("groupSearchFilter", "NULL");
        overrides.put("groupSearchScope", "NULL");
        overrides.put("maxResults", "NULL");
        overrides.put("priority", "NULL");
        overrides.put("readTimeout", "NULL");
        overrides.put("url", "NULL");
        overrides.put("useFor", "NULL");
        updateLdapSettingsBean(overrides);

        /*
         * Reload applications and make the first call so that the identity store reinitializes.
         */
        FATHelper.reloadApplications(server, Stream.of("JavaEESecAnnotatedBasicAuthServletDeferred").collect(Collectors.toCollection(HashSet::new)));
        verifyAuthorization(SC_FORBIDDEN, null, null, null);
        assertStringsInLogsAndTraceUsingMark("Returning null since bindDn is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since bindDnPassword is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since callerBaseDn is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since callerNameAttribute is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since callerSearchBase is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since callerSearchFilter is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since callerSearchScopeExpression is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since groupMemberAttribute is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since groupMemberOfAttribute is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since groupNameAttribute is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since groupSearchBase is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since groupSearchFilter is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since groupSearchScopeExpression is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since maxResultsExpression is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since priorityExpression is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since readTimeoutExpression is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since url is a deferred expression and this is called on initialization.");
        assertStringsInLogsAndTraceUsingMark("Returning null since useForExpression is a deferred expression and this is called on initialization.");
        assertStringsInLogsUsingMark("CWWKS1916W:.*bindDn.*cannot be resolved to a valid value", false); // Fail before we need this
        assertStringsInLogsUsingMark("CWWKS1916W:.*bindDnPassword.*cannot be resolved to a valid value", false); // Fail before we need this
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerBaseDn.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerNameAttribute.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchBase.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchFilter.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchScope/callerSearchScopeExpression.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupMemberAttribute.*cannot be resolved to a valid value", false); // using groupMemberOfAttribute
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupMemberOfAttribute.*cannot be resolved to a valid value", false); // Fail before we need this
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupNameAttribute.*cannot be resolved to a valid value", false); // Fail before we need this
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchBase.*cannot be resolved to a valid value", false); // Fail before we need this
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchFilter.*cannot be resolved to a valid value", false); // Fail before we need this
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchScope/groupSearchScopeExpression.*cannot be resolved to a valid value", false); // using groupMemberOfAttribute
        assertStringsInLogsUsingMark("CWWKS1916W:.*maxResults/maxResultsExpression.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*priority/priorityExpression.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*readTimeout/readTimeoutExpression.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*url.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*useFor/useForExpression.*cannot be resolved to a valid value");

        /*
         * Clear the override values of those attributes we had CWWKS1916W messages for above.
         *
         * We will now fail with a javax.naming.NoPermissionException b/c we can't do an anonymous op.
         */
        overrides.remove("callerBaseDn");
        overrides.remove("callerNameAttribute");
        overrides.remove("callerSearchBase");
        overrides.remove("callerSearchFilter");
        overrides.remove("callerSearchScope");
        overrides.remove("maxResults");
        overrides.remove("priority");
        overrides.remove("readTimeout");
        overrides.remove("url");
        overrides.remove("useFor");
        updateLdapSettingsBean(overrides);
        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_FORBIDDEN, null, null, null);
        assertStringsInLogsAndTraceUsingMark("Returning null since", false); // These should only be thrown on init
        assertStringsInLogsUsingMark("CWWKS1916W:.*bindDn.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*bindDnPassword.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerBaseDn.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerNameAttribute.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchBase.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchFilter.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchScope/callerSearchScopeExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupMemberAttribute.*cannot be resolved to a valid value", false); // using groupMemberOfAttribute
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupMemberOfAttribute.*cannot be resolved to a valid value", false); // Fail before we need this
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupNameAttribute.*cannot be resolved to a valid value", false); // Fail before we need this
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchBase.*cannot be resolved to a valid value", false); // Fail before we need this
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchFilter.*cannot be resolved to a valid value", false); // Fail before we need this
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchScope/groupSearchScopeExpression.*cannot be resolved to a valid value", false); // using groupMemberOfAttribute
        assertStringsInLogsUsingMark("CWWKS1916W:.*maxResults/maxResultsExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*priority/priorityExpression.*cannot be resolved to a valid value", false); // Only checked on init
        assertStringsInLogsUsingMark("CWWKS1916W:.*readTimeout/readTimeoutExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*url.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*useFor/useForExpression.*cannot be resolved to a valid value", false);

        /*
         * Clear the override values of those attributes we had CWWKS1916W messages for above.
         *
         * We should succeed now and use use the groupMemberOfAttributes.
         */
        overrides.remove("bindDn");
        overrides.remove("bindDnPassword");
        updateLdapSettingsBean(overrides);
        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, null, null, null);
        assertStringsInLogsAndTraceUsingMark("Returning null since", false); // These should only be thrown on init
        assertStringsInLogsUsingMark("CWWKS1916W:.*bindDn.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*bindDnPassword.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerBaseDn.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerNameAttribute.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchBase.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchFilter.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchScope/callerSearchScopeExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupMemberAttribute.*cannot be resolved to a valid value", false); // using groupMemberOfAttribute
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupMemberOfAttribute.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupNameAttribute.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchBase.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchFilter.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchScope/groupSearchScopeExpression.*cannot be resolved to a valid value", false); // using groupMemberOfAttribute
        assertStringsInLogsUsingMark("CWWKS1916W:.*maxResults/maxResultsExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*priority/priorityExpression.*cannot be resolved to a valid value", false); // Only checked on init
        assertStringsInLogsUsingMark("CWWKS1916W:.*readTimeout/readTimeoutExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*url.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*useFor/useForExpression.*cannot be resolved to a valid value", false);

        /*
         * Clear the override values of those attributes we had CWWKS1916W messages for above. Also set groupMemberAttribute and groupSearchScope so we get CWWKS1916W messages for
         * them both.
         */
        overrides.put("groupMemberAttribute", "NULL");
        overrides.remove("groupMemberOfAttribute");
        overrides.remove("groupNameAttribute");
        overrides.remove("groupSearchBase");
        overrides.remove("groupSearchFilter");
        overrides.put("groupSearchScope", "NULL");
        updateLdapSettingsBean(overrides);
        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, null, null, null);
        assertStringsInLogsAndTraceUsingMark("Returning null since", false); // These should only be thrown on init
        assertStringsInLogsUsingMark("CWWKS1916W:.*bindDn.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*bindDnPassword.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerBaseDn.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerNameAttribute.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchBase.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchFilter.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchScope/callerSearchScopeExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupMemberAttribute.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupMemberOfAttribute.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupNameAttribute.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchBase.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchFilter.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchScope/groupSearchScopeExpression.*cannot be resolved to a valid value");
        assertStringsInLogsUsingMark("CWWKS1916W:.*maxResults/maxResultsExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*priority/priorityExpression.*cannot be resolved to a valid value", false); // Only checked on init
        assertStringsInLogsUsingMark("CWWKS1916W:.*readTimeout/readTimeoutExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*url.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*useFor/useForExpression.*cannot be resolved to a valid value", false);

        /*
         * Run one more time with default settings. No errors should occur.
         */
        overrides.clear();
        updateLdapSettingsBean(overrides);
        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, null, null, null);
        assertStringsInLogsAndTraceUsingMark("Returning null since", false); // These should only be thrown on init
        assertStringsInLogsUsingMark("CWWKS1916W:.*bindDn.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*bindDnPassword.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerBaseDn.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerNameAttribute.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchBase.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchFilter.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*callerSearchScope/callerSearchScopeExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupMemberAttribute.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupMemberOfAttribute.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupNameAttribute.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchBase.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchFilter.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*groupSearchScope/groupSearchScopeExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*maxResults/maxResultsExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*priority/priorityExpression.*cannot be resolved to a valid value", false); // Only checked on init
        assertStringsInLogsUsingMark("CWWKS1916W:.*readTimeout/readTimeoutExpression.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*url.*cannot be resolved to a valid value", false);
        assertStringsInLogsUsingMark("CWWKS1916W:.*useFor/useForExpression.*cannot be resolved to a valid value", false);
    }
}
