/*
* IBM Confidential
*
* OCO Source Materials
*
* WLP Copyright IBM Corp. 2018
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.security.javaeesec.fat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.JavaEESecTestBase;
import com.ibm.ws.security.javaeesec.fat_helper.LocalLdapServer;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@MinimumJavaLevel(javaLevel = 1.8, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)

public class EAREJBModuleTest extends JavaEESecTestBase {
    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = MultipleModuleTest.class;
    protected static String urlBase;
    protected static String TEMP_DIR = "test_temp";
    protected static String EJB_BEAN_JAR_NAME = "SecurityEJBinWAR.jar";
    protected static String EJB_SERVLET_NAME = "SecurityEJBBaseServlet";
    protected static String EJB_WAR_NAME = "ejbinwarservlet.war";
    protected static String EJB_EAR_NAME = "securityejbinwar.ear";
    protected static String EJB_APP_NAME = "securityejbinwar.ear";
    protected static String XML_NAME = "ejbserver.xml";

    protected static String REALM1_USER = "realm1user";
    protected static String REALM1_PASSWORD = "s3cur1ty";
    protected static String REALM2_USER = "realm2user";
    protected static String REALM2_PASSWORD = "s3cur1ty";
    protected static String COMMON_USER1 = "commonuser1";
    protected static String COMMON_USER2 = "commonuser2";
    protected static String IS1_REALM_NAME = "127.0.0.1:10389";
    protected static String IS2_REALM_NAME = "localhost:10389";
    protected static String REALM1_REALM_NAME = "Realm1";
    protected static String REALM2_REALM_NAME = "Realm2";
    protected static String COMMON_REALM_NAME = "CommonIdentityStore";

    protected static String IS1_GROUP_REALM_NAME = "group:127.0.0.1:10389/";
    protected static String IS2_GROUP_REALM_NAME = "group:localhost:10389/";

    protected static String IS1_GROUPS = "group:127.0.0.1:10389/grantedgroup2, group:127.0.0.1:10389/grantedgroup, group:127.0.0.1:10389/group1";
    protected static String IS2_GROUPS = "group:localhost:10389/grantedgroup2, group:localhost:10389/anothergroup1, group:localhost:10389/grantedgroup";
    protected static String REALM1_GROUPS = "group:Realm1/grantedgroup2, group:Realm1/grantedgroup, group:Realm1/realm1group1, group:Realm1/realm1group2";
    protected static String REALM2_GROUPS = "group:Realm2/grantedgroup2, group:Realm2/realm2group2, group:Realm2/realm2group1, group:Realm2/grantedgroup";
    protected static String COMMONUSER_GROUPS = "group:CommonIdentityStore/grantedgroup2, group:CommonIdentityStore/commonGroup2, group:CommonIdentityStore/commonGroup1, group:CommonIdentityStore/grantedgroup";

    protected DefaultHttpClient httpclient;

    protected static LocalLdapServer ldapServer;

    public EAREJBModuleTest() {
        super(myServer, logClass);
    }

    @Rule
    public TestName name = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {

        ldapServer = new LocalLdapServer();
        ldapServer.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();

        if (ldapServer != null) {
            ldapServer.stop();
        }
        myServer.setServerConfigurationFile("server.xml");
    }

    @Before
    public void setupConnection() {
        // disable auto redirect.
        HttpParams httpParams = new BasicHttpParams();
        httpParams.setParameter(ClientPNames.HANDLE_REDIRECTS, Boolean.FALSE);

        httpclient = new DefaultHttpClient(httpParams);
    }

    @After
    public void cleanupConnection() throws Exception {
        httpclient.getConnectionManager().shutdown();
        myServer.stopServer();
    }

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    protected void startServer(String config, String appName) throws Exception {
        myServer.setServerConfigurationFile(config);
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(appName);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> An ear file which contains two war files. Each war files contains one LdapIdentityStoreDefinision, one custom identity store.
     * and one FormHttpAuthenticationMechanismDefinision which points to different form.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> In this case, the IdentityStores which are defined by LdapIdentityStoreDefinision are visible from any module, however,
     * the one which are bundled with each module is only visible within the module.
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void testMultipleModuleWars() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // create ejbinwarservlet.war,
        WCApplicationHelper.createWar(myServer, TEMP_DIR, EJB_WAR_NAME, true, EJB_BEAN_JAR_NAME, true, "web.ejb.jar.bean", "web.war.ejb.servlet");

        // add the servlet war inside the ear
        WCApplicationHelper.packageWarsToEar(myServer, TEMP_DIR, EJB_EAR_NAME, true, EJB_WAR_NAME);

        //add ear to the server
        WCApplicationHelper.addEarToServerApps(myServer, TEMP_DIR, EJB_EAR_NAME);

        startServer(XML_NAME, EJB_APP_NAME);
//
        //myServer.removeInstalledAppForValidation(EJB_APP_NAME);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

/* ------------------------ support methods ---------------------- */
    protected String getViewState(String form) {
        Pattern p = Pattern.compile("[\\s\\S]*value=\"(.+)\".*autocomplete[\\s\\S]*");
        Matcher m = p.matcher(form);
        String viewState = null;
        if (m.matches()) {
            viewState = m.group(1);
        }
        return viewState;
    }

    protected void verifyResponse(String response, String user, String realm, String invalidRealm, String groups) {
        verifyUserResponse(response, Constants.getUserPrincipalFound + user, Constants.getRemoteUserFound + user);
        verifyRealm(response, realm);
        if (invalidRealm != null) {
            verifyNotInGroups(response, invalidRealm); // make sure that there is no realm name from the second IdentityStore.
        }
        verifyGroups(response, groups);
    }
}
