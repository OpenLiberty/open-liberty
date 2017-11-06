package com.ibm.ws.security.javaeesec.fat;

import javax.servlet.http.HttpServletResponse;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.apacheds.EmbeddedApacheDS;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014, 2015
 *
 * The source code for this program is not published or other-
 * wise divested of its trade secrets, irrespective of what has
 * been deposited with the U.S. Copyright Office.
 */
/**
 * Test Description:
 *
 * The test verifies that Form Login is handled by the JASPI provider for both
 * positive and negative cases when the JASPI user feature is present in the server.xml and
 * the application web.xml contains <login-config> with <auth-method>FORM</auth-method>.
 *
 * The test access a protected servlet, verifies that the JASPI provider was invoked to
 * make the authentication decision and verifies that the servlet response contains the correct
 * values for getAuthType, getUserPrincipal and getRemoteUser after JASPI authentication.
 *
 */
@MinimumJavaLevel(javaLevel = 1.7, runSyntheticTest = false)
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class FormHttpAuthenticationMechanismTest extends JavaEESecTestBase {

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.jaspic11.fat");
    protected static Class<?> logClass = FormHttpAuthenticationMechanismTest.class;
    protected static String queryString = "/JASPIFormLoginServlet/JASPIForm";
    protected static String queryStringUnprotected = "/JASPIFormLoginServlet/JASPIUnprotected";
    protected static String urlBase;

    protected DefaultHttpClient httpclient;
    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    private static EmbeddedApacheDS ldapServer = null;
    private static final String BASE_DN = "o=ibm,c=us";
    private static final String USER = "user7";
    private static final String USER_DN = "uid=" + USER + "," + BASE_DN;
    private static final String PASSWORD = "usrpwd";

    public FormHttpAuthenticationMechanismTest() {
        super(myServer, logClass);
    }

    private static void setupldapServer() throws Exception {
        ldapServer = new EmbeddedApacheDS("contextpoolTimeoutLDAP");
        ldapServer.addPartition("test", BASE_DN);
        ldapServer.startServer();

        /*
         * Add the partition entries.
         */
        Entry entry = ldapServer.newEntry(BASE_DN);
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        entry.add("o", "com");
        ldapServer.add(entry);

        entry = ldapServer.newEntry(USER_DN);
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", USER);
        entry.add("sn", USER);
        entry.add("cn", USER);
        entry.add("userPassword", PASSWORD);
        ldapServer.add(entry);

    }

    @BeforeClass
    public static void setUp() throws Exception {
        myServer.installUserBundle("security.jaspi.user.feature.test_1.0");
        myServer.installUserFeature("jaspicUserTestFeature-1.0");
        myServer.startServer(true);
        myServer.addInstalledAppForValidation(DEFAULT_FORM_APP);

        if (myServer.getValidateApps()) { // If this build is Java 7 or above
            verifyServerStartedWithJaspiFeature(myServer);
        }

        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

        emptyConfiguration = myServer.getServerConfiguration();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        myServer.stopServer();
        myServer.uninstallUserBundle("security.jaspi.user.feature.test_1.0");
        myServer.uninstallUserFeature("jaspicUserTestFeature-1.0");

    }

    @Before
    public void setupConnection() {
        httpclient = new DefaultHttpClient();
    }

    @After
    public void cleanupConnection() {
        httpclient.getConnectionManager().shutdown();
    }

    @Rule
    public TestName name = new TestName();

    @Override
    protected String getCurrentTestName() {
        return name.getMethodName();
    }

    /**
     * Verify the following:
     * <OL>
     * <LI> Attempt to access a protected servlet configured for FORM login with JASPI activated.
     * <LI> Login with a valid userId and password in the javaeesec_form role and verify that
     * <LI> JASPI authentication occurs and establishes return values for getAuthType, getUserPrincipal and getRemoteUser.
     * </OL>
     * <P> Expected Results:
     * <OL>
     * <LI> Return code 200
     * <LI> Messages.log contains lines to show that JASPI authentication was processed on form display:
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> ---JASPI secureResponse called with auth provider=<provider_name>
     * <LI> Messages.log contains line to show validateRequest called submitting the form with valid user and password
     * <LI> ---JASPI validateRequest called with auth provider=<provider_name>
     * <LI> NOTE: Product design does not allow for secureResponse to be called here so it is not checked.
     * <LI> Servlet is accessed and it prints information about the subject: getAuthType, getUserPrincipal, getRemoteUser.
     * </OL>
     */
    @Test
    public void testJaspiFormLoginValidUserInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Send servlet query to get form login page.
        myServer.setMarkToEndOfLog();
        getFormLoginPage(httpclient, urlBase + queryString, DEFAULT_JASPI_PROVIDER);
        verifyJaspiAuthenticationProcessedInMessageLog();

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        String location = executeFormLogin(httpclient, urlBase + DEFAULT_FORM_LOGIN_PAGE, javaeesec_formRoleUser, javaeesec_formRolePwd);
        verifyJaspiAuthenticationProcessedValidateRequestInMessageLog();

        // Redirect to the given page, ensure it is the original servlet request and it returns the right response.
        String response = accessPageNoChallenge(httpclient, location, HttpServletResponse.SC_OK, DEFAULT_FORMLOGIN_SERVLET_NAME);
        verifyUserResponse(response, getUserPrincipalFound + javaeesec_formRoleUser, getRemoteUserFound + javaeesec_formRoleUser);
        Log.info(logClass, getCurrentTestName(), "-----Exiting " + getCurrentTestName());
    }

}
