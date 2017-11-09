package com.ibm.ws.security.javaeesec.fat;

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
import com.ibm.ws.security.javaeesec.fat_helper.Constants;
import com.ibm.ws.security.javaeesec.fat_helper.WCApplicationHelper;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.OnlyRunInJava7Rule;
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

    protected static LibertyServer myServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.javaeesec.fat");
    protected static Class<?> logClass = FormHttpAuthenticationMechanismTest.class;
    protected static String[] warList = { "JavaEESecBasicAuthServlet.war", "JavaEESecAnnotatedBasicAuthServlet.war",
                                          "JavaEEsecFormAuth.war", "JavaEEsecFormAuthRedirect.war" };
    protected static String urlBase;
    protected static String JAR_NAME = "JavaEESecBase.jar";

    protected DefaultHttpClient httpclient;
    /**
     * Nearly empty server configuration. This should just contain the feature manager configuration with no
     * registries or federated repository configured.
     */
    private static ServerConfiguration emptyConfiguration = null;

    private static EmbeddedApacheDS ldapServer = null;
    private static final String BASE_DN = "o=ibm,c=us";
    private static final String USER = "jaspildapuser1";
    private static final String USER_DN = "uid=" + USER + "," + BASE_DN;
    private static final String PASSWORD = "s3cur1ty";

    public FormHttpAuthenticationMechanismTest() {
        super(myServer, logClass);
    }

    private static void setupldapServer() throws Exception {
        ldapServer = new EmbeddedApacheDS("HTTPAuthLDAP");
        ldapServer.addPartition("test", "o=ibm,c=us");
        ldapServer.startServer(Integer.parseInt(System.getProperty("ldap.1.port")));

        Entry entry = ldapServer.newEntry("o=ibm,c=us");
        entry.add("objectclass", "organization");
        entry.add("o", "ibm");
        ldapServer.add(entry);

        entry = ldapServer.newEntry("uid=jaspildapuser1,o=ibm,c=us");
        entry.add("objectclass", "inetorgperson");
        entry.add("uid", "jaspildapuser1");
        entry.add("sn", "jaspildapuser1sn");
        entry.add("cn", "jaspiuser1");
        entry.add("userPassword", "s3cur1ty");
        ldapServer.add(entry);

    }

    @BeforeClass
    public static void setUp() throws Exception {
        // if (!OnlyRunInJava7Rule.IS_JAVA_7_OR_HIGHER)
        // return; // skip the test setup

        setupldapServer();

//        LDAPUtils.addLDAPVariables(myServer);
//        myServer.installUserBundle("security.jaspi.user.feature.test_1.0");
//        myServer.installUserFeature("jaspicUserTestFeature-1.0");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.basic");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEESecAnnotatedBasicAuthServlet.war", true, JAR_NAME, false, "web.jar.base", "web.war.annotatedbasic");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEEsecFormAuth.war", true, JAR_NAME, false, "web.jar.base", "web.war.formlogin");
        WCApplicationHelper.addWarToServerApps(myServer, "JavaEEsecFormAuthRedirect.war", true, JAR_NAME, false, "web.jar.base", "web.war.redirectformlogin");
        myServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatures/javaeesecinternals-1.0.mf");

        myServer.startServer(true);
//        myServer.addInstalledAppForValidation(DEFAULT_APP);
//        verifyServerStartedWithJaspiFeature(myServer);
        urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort();

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (!OnlyRunInJava7Rule.IS_JAVA_7_OR_HIGHER)
            return; // skip the test teardown
        myServer.stopServer();
//        myServer.uninstallUserBundle("security.jaspi.user.feature.test_1.0");
//        myServer.uninstallUserFeature("jaspicUserTestFeature-1.0");

        if (ldapServer != null) {
            try {
                ldapServer.stopService();
            } catch (Exception e) {
                Log.error(logClass, "teardown", e, "LDAP server threw error while stopping. " + e.getMessage());
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
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiFormLoginValidUserInRole_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());
        // Execute Form login expect a forward to happen.
        myServer.setMarkToEndOfLog();
        executeFormLogin(httpclient, urlBase + Constants.DEFAULT_FORM_LOGIN_PAGE, Constants.javaeesec_basicRoleLDAPUser,
                         Constants.javaeesec_basicRolePwd, false);
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
    @Mode(TestMode.LITE)
    @Test
    public void testJaspiFormLoginValidUserInRoleRedirect_AllowedAccess() throws Exception {
        Log.info(logClass, getCurrentTestName(), "-----Entering " + getCurrentTestName());

        // Execute Form login and get redirect location.
        myServer.setMarkToEndOfLog();
        executeFormLogin(httpclient, urlBase + Constants.DEFAULT_REDIRECT_FORM_LOGIN_PAGE, Constants.javaeesec_basicRoleLDAPUser,
                         Constants.javaeesec_basicRolePwd, true);
    }

}
