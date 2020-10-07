/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.fat;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.LocalLdapServer;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class CustomIdentityStoreHandlerTest extends JavaEESecTestBase {
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = CustomIdentityStoreHandlerTest.class;
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";
    protected static String APP_NAME = "JavaEESecMultipleIS";
    protected static String WAR_NAME = APP_NAME + ".war";
    protected static String XML_NAME = "multipleIS.xml";
    protected String queryString = "/" + APP_NAME + "/MultipleISBasicAuthServlet";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;
    protected static String portNumber = "";

    public CustomIdentityStoreHandlerTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        portNumber = System.getProperty("ldap.1.port");
        ldapServer = new LocalLdapServer();
        ldapServer.start();

        WCApplicationHelper.addWarToServerApps(myServer, WAR_NAME, true, JAR_NAME, false, "web.jar.base", "web.war.servlets.basic", "web.war.identitystores.ldap.ldap1",
                                               "web.war.identitystores.ldap.ldap2", "web.war.identitystores.custom.grouponly", "web.war.identitystorehandler",
                                               "web.war.identitystores.ldap");
        myServer.setServerConfigurationFile(XML_NAME);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(APP_NAME);

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        try {
            myServer.stopServer();
        } finally {
            if (ldapServer != null) {
                ldapServer.stop();
            }
        }

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
     * Verify the following:
     * <OL>
     * <LI> Authentication is done by the first priority IdentityStroe
     * <LI> Additional groups are added by the IdentityStore which only support getting groups.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Veirfy the realm name is the same as the IdentityStore ID of the 1st IdentityStore.
     * <LI> Veirfy the list of groups contains the group name of 1st and 3rd groups only
     * <LI> Veirfy the list of groups does not contain the group name of 2nd identitystore.
     * <LI> Veirfy the customidentitystorehandler is loaded.
     * </OL>
     */
    @Test
    public void testCustomIDSHandlerBAWith1stIS_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.resetLogMarks();
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                          LocalLdapServer.PASSWORD,
                                                          HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "127.0.0.1:" + portNumber);
        verifyNotInGroups(response, "group:localhost:" + portNumber + "/"); // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response, "group:127.0.0.1:" + portNumber + "/grantedgroup2, group:127.0.0.1:" + portNumber + "/grantedgroup, group:127.0.0.1:" + portNumber + "/group1");
        verifyMessageReceivedInMessageLog("CustomIdentityStoreHandler is being used.");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is done by the second priority IdentityStroe because the user id does not exist in the first IdentityStore.
     * <LI> Additional groups are added by the IdentityStore which only support getting groups.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Veirfy the realm name is the same as the IdentityStore ID of the 2nd IdentityStore.
     * <LI> Veirfy the list of groups contains the group name of 1st and 3rd groups only
     * <LI> Veirfy the list of groups does not contain the group name of 1st identitystore.
     * </OL>
     */
    @Test
    public void testCustomIDSHandlerBAWith2ndISonly_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.ANOTHERUSER1,
                                                          LocalLdapServer.ANOTHERPASSWORD,
                                                          HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.ANOTHERUSER1, Constants.getRemoteUserFound + LocalLdapServer.ANOTHERUSER1);
        verifyRealm(response, "localhost:" + portNumber);
        verifyNotInGroups(response, "group:127.0.0.1:" + portNumber + "/"); // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response,
                     "group:localhost:" + portNumber + "/grantedgroup2, group:localhost:" + portNumber + "/anothergroup1, group:localhost:" + portNumber + "/grantedgroup");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is done by the second priority IdentityStroe because the password does not match in the 1st IdentityStore.
     * <LI> Additional groups are added by the IdentityStore which only support getting groups.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Veirfy the realm name is the same as the IdentityStore ID of the 2nd IdentityStore.
     * <LI> Veirfy the list of groups contains the group name of 1st and 3rd groups only
     * <LI> Veirfy the list of groups does not contain the group name of 1st identitystore.
     * </OL>
     */
    @AllowedFFDC({ "javax.naming.AuthenticationException" })
    @Test
    public void testCustomIDSHandlerBAWith1stISfail2ndISsuccess_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                          LocalLdapServer.ANOTHERPASSWORD,
                                                          HttpServletResponse.SC_OK);
        verifyUserResponse(response, Constants.getUserPrincipalFound + LocalLdapServer.USER1, Constants.getRemoteUserFound + LocalLdapServer.USER1);
        verifyRealm(response, "localhost:" + portNumber);
        verifyNotInGroups(response, "group:127.0.0.1:" + portNumber + "/"); // make sure that there is no realm name from the second IdentityStore.
        verifyGroups(response,
                     "group:localhost:" + portNumber + "/grantedgroup2, group:localhost:" + portNumber + "/anothergroup1, group:localhost:" + portNumber + "/grantedgroup");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is done by the first priority IdentityStroe but fails authorization check.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 401
     * </OL>
     */
    @Test
    public void testCustomIDSHandlerBANoCred_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        String response = executeGetRequestNoAuthCreds(httpclient, urlBase + queryString,
                                                       HttpServletResponse.SC_UNAUTHORIZED);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is done by the first priority IdentityStroe but fails authorization check.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Veirfy the CWWKS9104A message is logged.
     * </OL>
     */
    @Test
    public void testCustomIDSHandlerBAWith1stISuccess_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.INVALIDUSER,
                                                          LocalLdapServer.PASSWORD,
                                                          HttpServletResponse.SC_FORBIDDEN);
        verifyMessageReceivedInMessageLog("CWWKS9104A:.*" + LocalLdapServer.INVALIDUSER + ".*" + LocalLdapServer.GRANTEDGROUP);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Authentication is failed for both the 1st and 2nd IdentityStore.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 403
     * <LI> Veirfy the CWWKS1652A message is logged.
     * </OL>
     */
    @AllowedFFDC({ "javax.naming.AuthenticationException" })
    @Test
    public void testCustomIDSHandlerBAWith1st2ndFail_DeniedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        myServer.setMarkToEndOfLog();
        String response = executeGetRequestBasicAuthCreds(httpclient, urlBase + queryString, LocalLdapServer.USER1,
                                                          LocalLdapServer.INVALIDPASSWORD,
                                                          HttpServletResponse.SC_UNAUTHORIZED);
        verifyMessageReceivedInMessageLog("CWWKS1652A:.*");
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
