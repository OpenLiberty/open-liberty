/*******************************************************************************
 * Copyright (c) 2018,2020 IBM Corporation and others.
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;
import com.ibm.ws.security.javaeesec.identitystore.DatabaseIdentityStore;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.vulnerability.LeakedPasswordChecker;
import web.war.database.deferred.DatabaseSettingsBean;

/**
 * Test for {@link DatabaseIdentityStore} configured with immediate EL expressions.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DatabaseIdentityStoreImmediateSettingsTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.dbidstore.immediate.fat");
    protected static Class<?> logClass = DatabaseIdentityStoreImmediateSettingsTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    private final LeakedPasswordChecker passwordChecker = new LeakedPasswordChecker(server);
    protected DefaultHttpClient httpclient;

    public DatabaseIdentityStoreImmediateSettingsTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        WCApplicationHelper.addWarToServerApps(myServer, "DatabaseIdstoreImmediate.war", true, JAR_NAME, false, "web.jar.base",
                                               "web.war.database.immediate");
        WCApplicationHelper.addWarToServerApps(myServer, "dbfat.war", true, JAR_NAME, false, "web.jar.base", "web.war.db");

        myServer.startServer(true);
        assertNotNull("Application DBServlet does not appear to have started.",
                      myServer.waitForStringInLog("CWWKZ0001I: Application DBServlet started"));

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort()
                  + "/DatabaseIdstoreImmediate/DatabaseIdstoreImmediate";
    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
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

        resetConnection();

        /* DB_USER2 */
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase, Constants.DB_USER2, Constants.DB_USER2_PWD, code2);
        if (code2 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + Constants.DB_USER2, getRemoteUserFound + Constants.DB_USER2);
        }
        passwordChecker.checkForPasswordInAnyFormat(Constants.DB_USER2_PWD);

        resetConnection();

        /* DB_USER3 */
        response = executeGetRequestBasicAuthCreds(httpclient, urlBase, Constants.DB_USER3, Constants.DB_USER3_PWD, code3);
        if (code3 == SC_OK) {
            verifyUserResponse(response, getUserPrincipalFound + Constants.DB_USER3, getRemoteUserFound + Constants.DB_USER3);
        }
        passwordChecker.checkForPasswordInAnyFormat(Constants.DB_USER3_PWD);

        resetConnection();
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

        Log.info(logClass, getCurrentTestName(), "-----Starting dataSourceLookup -- verify that we can't change the dataSourceLookup setting via an immediate EL expression");

        Map<String, String> overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.DS_LOOKUP, "java:comp/InvalidDataSource");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        String msg = "DataSource is stored "; // will only be in trace.log
        String msg2 = "returns: java:comp/InvalidDataSource"; // will be in trace.log and messages.log
        String msg3 = "Always evaluate Datasource: false"; //trace

        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);

        List<String> foundResults = myServer.findStringsInLogsAndTrace(msg3);
        assertEquals("Expected datasource to not be evaluated: " + msg3, 1, foundResults.size());

        foundResults = myServer.findStringsInLogsAndTrace(msg);
        assertFalse("Should save the datasource: " + msg, foundResults.isEmpty());
        foundResults = myServer.findStringsInLogs(msg2);
        assertTrue("Should not have evaluated the datasource: " + msg2, foundResults.isEmpty());

        Log.info(logClass, getCurrentTestName(), "-----Finished dataSourceLookup");

        Log.info(logClass, getCurrentTestName(), "-----Starting groupsQuery -- Verify that we can't change the groupsQuery setting via an immediate EL expression");

        overrides = new HashMap<String, String>();
        overrides.put(JavaEESecConstants.GROUPS_QUERY, "select group_name from badtable where caller_name = ?");
        DatabaseSettingsBean.updateDatabaseSettingsBean(server.getServerRoot(), overrides);

        verifyAuthorization(SC_OK, SC_OK, SC_FORBIDDEN);

        Log.info(logClass, getCurrentTestName(), "-----Finished groupsQuery");

        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }
}
