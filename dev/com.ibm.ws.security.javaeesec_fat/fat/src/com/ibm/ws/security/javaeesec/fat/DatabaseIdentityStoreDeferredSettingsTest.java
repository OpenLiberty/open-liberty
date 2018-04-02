/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.FATHelper;
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
import componenttest.vulnerability.LeakedPasswordChecker;
import web.war.database.deferred.DatabaseSettingsBean;

/**
 * Test for {@link LdapIdentityStore} configured with deferred EL expressions.
 */
@MinimumJavaLevel(javaLevel = 1.7)
@RunWith(FATRunner.class)
@Mode(TestMode.LITE)
public class DatabaseIdentityStoreDeferredSettingsTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.dbidstore.deferred.fat");
    protected static Class<?> logClass = DatabaseIdentityStoreDeferredSettingsTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);
    protected DefaultHttpClient httpclient;

    public DatabaseIdentityStoreDeferredSettingsTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        WCApplicationHelper.addWarToServerApps(myServer, "DatabaseIdstoreDeferred.war", true, JAR_NAME, false, "web.jar.base",
                                               "web.war.database.deferred");
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat.war", true, JAR_NAME, false, "web.jar.base", "web.war.db");

        myServer.startServer(true);
        assertNotNull("Application DBServlet does not appear to have started.",
                      myServer.waitForStringInLog("CWWKZ0001I: Application DBServlet started"));

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort()
                  + "/DatabaseIdstoreDeferred/DatabaseIdstoreDeferred";
    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer("CWWKS1916W", "CWWKS1919W", "CWWKS1918E");
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

    /**
     * Verify that the user responses are as expected.
     *
     * @param code1 Expected response code for DB_USER1.
     * @param code2 Expected response code for DB_USER2.
     * @param code3 Expected response code for DB_USER3.
     * @throws Exception If there was an error processing the request.
     */
    private void verifyAuthorization(int code1, int code2, int code3) throws Exception {

        /* DB_USER1 */
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase, Constants.DB_USER1, Constants.DB_USER1_PWD, code1);
        if (code1 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + Constants.DB_USER1, getRemoteUserFound + Constants.DB_USER1);
        }
        passwordChecker.checkForPasswordInAnyFormat(Constants.DB_USER1_PWD);

        /* DB_USER2 */
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase, Constants.DB_USER2, Constants.DB_USER2_PWD, code2);
        if (code2 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + Constants.DB_USER2, getRemoteUserFound + Constants.DB_USER2);
        }
        passwordChecker.checkForPasswordInAnyFormat(Constants.DB_USER2_PWD);

        /* DB_USER3 */
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase, Constants.DB_USER3, Constants.DB_USER3_PWD, code3);
        if (code3 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + Constants.DB_USER3, getRemoteUserFound + Constants.DB_USER3);
        }
        passwordChecker.checkForPasswordInAnyFormat(Constants.DB_USER3_PWD);

    }

    /**
     * This test will verify that all users in the nominal configuration can authenticate and
     * are authorized to access the servlet.
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - authorized via group</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void baselineTest() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), new HashMap<String, String>());

        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the callerQuery setting via a deferred EL expression.
     *
     * <ul>
     * <li>DB_USER1 - unauthorized (can't find user with bad caller query)</li>
     * <li>DB_USER2 - unauthorized (can't find user with bad caller query)</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC("java.sql.SQLSyntaxErrorException")
    public void callerQuery() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.CALLER_QUERY, "select password from badtable where name = ?");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        verifyAuthorization(SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED);

        String msg = "CWWKS1918E";
        List<String> errorResults = myServer.findStringsInLogsAndTraceUsingMark(msg);
        assertTrue("Did not find '" + msg + "' in trace: " + errorResults, !errorResults.isEmpty());

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a callerQuery EL expression that resolves to null is handled.
     * The callerQuery will be defaulted to an empty string resulting in no user being found.
     *
     * <ul>
     * <li>DB_USER1 - unauthorized (can't find user with bad caller query)</li>
     * <li>DB_USER2 - unauthorized (can't find user with bad caller query)</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void callerQuery_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.CALLER_QUERY, "NULL");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'callerQuery' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the dataSourceLookup setting via a deferred EL expression.
     *
     * <ul>
     * <li>DB_USER1 - unauthorized (can't find to datasource)</li>
     * <li>DB_USER2 - unauthorized (can't find to datasource)</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC("javax.naming.NameNotFoundException")
    public void dataSourceLookup() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.DS_LOOKUP, "java:comp/InvalidDataSource");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        verifyAuthorization(SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a datasource that points to an unreachable database is handled.
     *
     * <ul>
     * <li>DB_USER1 - unauthorized (can't find to datasource)</li>
     * <li>DB_USER2 - unauthorized (can't find to datasource)</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLException", "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException", "javax.resource.spi.ResourceAllocationException" })
    public void dataSourceLookup_NoDB() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.DS_LOOKUP, "jdbc/NoDatabase");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        verifyAuthorization(SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a dataSourceLookup EL expression that resolves to null is handled.
     * The dataSourceLookup will be defaulted to "java:comp/DefaultDataSource".
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - authorized via group</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void dataSourceLookup_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.DS_LOOKUP, "NULL");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'dataSourceLookup' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a dataSourceLookup EL expression that resolves to "" (empty string) is handled.
     * The dataSourceLookup will be defaulted to "java:comp/DefaultDataSource".
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - authorized via group</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void dataSourceLookup_Empty() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.DS_LOOKUP, "");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_UNAUTHORIZED, SC_UNAUTHORIZED, SC_UNAUTHORIZED);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'dataSourceLookup' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the hashAlgorithmParameters setting via a deferred EL expression.
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - authorized via group</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @Ignore("Test hangs on reloadApplications() in remote buids but not on local builds")
    public void hashAlgorithmParameters_Array() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String parameters = "Pbkdf2PasswordHash.Algorithm=PBKDF2WithHmacSHA512, Pbkdf2PasswordHash.Iterations=4096, Pbkdf2PasswordHash.SaltSizeBytes=64, Pbkdf2PasswordHash.KeySizeBytes=64";
        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.PWD_HASH_PARAMETERS, "[" + parameters + "]");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        // Reload the application to re-init the identity store so hash parameters are re-read.
        FATHelper.reloadApplications(server, Stream.of("DatabaseIdstoreDeferred").collect(Collectors.toCollection(HashSet::new)));
        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);
        server.findStringsInTrace("Processed HashAlgorithmParameters: \\{" + parameters + "\\}");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the hashAlgorithmParameters setting via a deferred EL expression.
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - authorized via group</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @Ignore("Test hangs on reloadApplications() in remote buids but not on local builds")
    public void hashAlgorithmParameters_Stream() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String parameters = "Pbkdf2PasswordHash.Algorithm=PBKDF2WithHmacSHA512, Pbkdf2PasswordHash.Iterations=4096, Pbkdf2PasswordHash.SaltSizeBytes=64, Pbkdf2PasswordHash.KeySizeBytes=128";
        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.PWD_HASH_PARAMETERS, "{" + parameters + "}");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        // Reload the application to re-init the identity store so hash parameters are re-read.
        FATHelper.reloadApplications(server, Stream.of("DatabaseIdstoreDeferred").collect(Collectors.toCollection(HashSet::new)));;
        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);
        server.findStringsInTrace("Processed HashAlgorithmParameters: \\{" + parameters + "\\}");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the hashAlgorithmParameters setting via a deferred EL expression.
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - authorized via group</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @Ignore("Test hangs on reloadApplications() in remote buids but not on local builds")
    public void hashAlgorithmParameters_String() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        String parameters = "Pbkdf2PasswordHash.Iterations=4096";
        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.PWD_HASH_PARAMETERS, parameters);
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        // Reload the application to re-init the identity store so hash parameters are re-read.
        FATHelper.reloadApplications(server, Stream.of("DatabaseIdstoreDeferred").collect(Collectors.toCollection(HashSet::new)));
        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);
        server.findStringsInTrace("Processed HashAlgorithmParameters: \\{" + parameters + "\\}");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a hashAlgorithmParameters EL expression that resolves to null is handled.
     * The hashAlgorithmParameters will be defaulted to an empty string.
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - authorized via group</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @Ignore("Test hangs on reloadApplications() in remote buids but not on local builds")
    public void hashAlgorithmParameters_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.PWD_HASH_PARAMETERS, "NULL");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        // Reload the application to re-init the identity store so hash parameters are re-read.
        FATHelper.reloadApplications(server, Stream.of("DatabaseIdstoreDeferred").collect(Collectors.toCollection(HashSet::new)));
        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'hashAlgorithmParameters[0]' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the groupsQuery setting via a deferred EL expression.
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - unauthorized (can't find group with bad group query)</li>
     * <li>DB_USER3 - unauthorized (never authorized)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @ExpectedFFDC("java.sql.SQLSyntaxErrorException")
    public void groupsQuery() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.GROUPS_QUERY, "select group_name from badtable where caller_name = ?");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_FORBIDDEN);

        String msg = "CWWKS1919W";
        List<String> errorResults = myServer.findStringsInLogsAndTraceUsingMark(msg);
        assertTrue("Did not find '" + msg + "' in trace: " + errorResults, !errorResults.isEmpty());

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the priority setting via a deferred EL expression.
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - authorized via group</li>
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
        overrides.put(JavaEESecConstants.PRIORITY, "100");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        /*
         * TODO We reload the applications since Java 8 doesn't work with injected entry/exit trace yet.
         * When it does, we can remove this.
         */
        FATHelper.reloadApplications(server, Stream.of("DatabaseIdstoreDeferred").collect(Collectors.toCollection(HashSet::new)));
        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);
        server.findStringsInTrace("IdentityStore from module BeanManager.*priority : 100");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a priority EL expression that resolves to null is handled.
     * The priority will be defaulted to 80.
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - authorized via group</li>
     * <li>DB_USER3 - unauthorized (not authorized by user or group)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    @Ignore("Test hangs on reloadApplications() in remote buids but not on local builds")
    public void priority_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.PRIORITY, "NULL");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        /*
         * TODO We reload the applications since Java 8 doesn't work with injected entry/exit trace yet.
         * When it does, we can remove this.
         */
        FATHelper.reloadApplications(server, Stream.of("DatabaseIdstoreDeferred").collect(Collectors.toCollection(HashSet::new)));
        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'priority/priorityExpression' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the useFor setting via a deferred EL expression.
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - unauthorized (identity store doesn't support PROVIDE_GROUPS)</li>
     * <li>DB_USER3 - unauthorized (not authorized by user or group)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useFor_1() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.USE_FOR, "VALIDATE");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        verifyAuthorization(SC_OK, SC_FORBIDDEN, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that we can change the useFor setting via a deferred EL expression.
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - authorized via group</li>
     * <li>DB_USER3 - unauthorized (identity store doesn't support VALIDATE)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useFor_2() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.USE_FOR, "");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'useFor/useForExpression' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * This test will verify that a useFor EL expression that resolves to null is handled.
     * The useFor will be defaulted to have both VALIDATE and PROVIDE_GROUPS.
     *
     * <ul>
     * <li>DB_USER1 - authorized via user</li>
     * <li>DB_USER2 - authorized via group</li>
     * <li>DB_USER3 - unauthorized (identity store doesn't support VALIDATE)</li>
     * </ul>
     *
     * @throws Exception If the test failed for some unforeseen reason.
     */
    @Test
    public void useFor_NULL() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.USE_FOR, "NULL");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        FATHelper.resetMarksInLogs(server);
        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);
        server.findStringsInLogsAndTrace("CWWKS1916W: An error occurs when the program resolves the 'useFor/useForExpression' configuration for the identity store.");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }
}
