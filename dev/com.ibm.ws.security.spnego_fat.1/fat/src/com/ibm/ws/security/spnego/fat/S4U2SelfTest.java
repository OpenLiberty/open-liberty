/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.spnego.fat;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ietf.jgss.Oid;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.KdcHelper;
import com.ibm.ws.security.spnego.fat.config.Krb5Helper;
import com.ibm.ws.security.spnego.fat.config.MessageConstants;
import com.ibm.ws.security.spnego.fat.config.MsKdcHelper;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.QUARANTINE)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class S4U2SelfTest extends CommonTest {

    private static final Class<?> c = S4U2SelfTest.class;
    private static final String tokenAPIServletName = "SPNEGOTokenHelperServlet";
    private static final String tokenAPIServletRootContext = "/SPNEGOTokenHelperFVTWeb";
    private static final String servletURL = "/SPNEGOTokenHelperFVTWeb/SPNEGOTokenHelperServlet";

    // Positive test expected results
    private static final String responseCheckForUserIdTest = "Delegate UserId Password Test #2 Succeeded";

    private static final String key_parmsFromS4U2Self = "parmsFromS4U2Self";
    private static final String key_parmsFromS4U2SelfWithKrb5Conf = "parmsFromS4U2SelfWithConf";
    private static final String key_parmsFromS4U2SelfWithKrb5ConfTwice = "parmsFromS4U2SelfWithConfTwice";

    private static final String key_parmsFromUserId = "parmsFromUserId";

    static String localHostMachine = "localhost";
    private static final String SERVER_INSTALL_ROOT = "{$server.install.root}";

    //modified the JAASCLIENT to JAASClientUseKeytab from JAASClient
    private static final String servletURLParms = "?ActiveUserid=" + InitClass.FIRST_USER + "&ActivePwd=" + InitClass.FIRST_USER_PWD
                                                  + "&JAAS=JAASClientUseKeytab&" + "UPN=" + InitClass.FIRST_USER + "@" + InitClass.KDC_REALM;
    private static final String envDelegationParm = "ENV=Delegation";
    private static final String envS4U2Self = "ENV=S4U2Self";
    private static final String targetServerBackenedServer = "SPN=HTTP/" + SPNEGOConstants.S4U_BACKEND_SERVICE;
    private static final String delegateServerSpn = "DELEGATESPN=HTTP/" + TARGET_SERVER;
    private static final String localhostSpnParm = "SPN=HTTP/" + localHostMachine;

    private static final String KRB5KeytabPath = SERVER_INSTALL_ROOT + "/usr/servers/S4U2SelfTest/resources/security/kerberos/krb5.keytab";
    private static final String KRB5ConfPath = SERVER_INSTALL_ROOT + "/usr/servers/S4U2SelfTest/resources/security/kerberos/krb5.conf";

    private static final String parmsFromS4U2Self = servletURLParms + "&" + envS4U2Self + "&" + targetServerBackenedServer
                                                    + "&" + delegateServerSpn + "&KRB5KEYTAB=" + KRB5KeytabPath
                                                    + "&TEST=S4U2SelfTest";
    private static final String parmsFromS4U2SelfBasicAuth = servletURLParms + "&" + envS4U2Self + "&" + targetServerBackenedServer
                                                             + "&" + delegateServerSpn + "&KRB5KEYTAB=" + KRB5KeytabPath + "&KRB5CONF=" + KRB5ConfPath;

    private static final String parmsFromS4U2SelfWithKrb5Conf = parmsFromS4U2SelfBasicAuth + "&TEST=S4U2SelfTest";
    private static final String parmsFromS4U2SelfWithKrb5ConfTwice = parmsFromS4U2SelfBasicAuth + "&TEST=S4U2SelfTestCallAPITwice";
    private static final String parmsFromUserId = servletURLParms + "&" + envDelegationParm + "&" + localhostSpnParm + "&TEST=fromuserid";

    private static Map<String, String> parametersList = new HashMap<String, String>();
    static {

        parametersList.put(key_parmsFromS4U2Self, parmsFromS4U2Self);
        parametersList.put(key_parmsFromS4U2SelfWithKrb5Conf, parmsFromS4U2SelfWithKrb5Conf);
        parametersList.put(key_parmsFromS4U2SelfWithKrb5ConfTwice, parmsFromS4U2SelfWithKrb5ConfTwice);
        parametersList.put(key_parmsFromUserId, parmsFromUserId);

    }

    private static boolean isServerRootUpdatedInUrls = false;

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";

        if (InitClass.isRndHostName) {
            Log.info(c, thisMethod, "Not running S4U2SelfTest because randomized hostname is used.");
            Assume.assumeTrue(false); //This disables this test class. None of the tests in the class will be run.
        } else {
            Log.info(c, thisMethod, "Starting the server and kerberos setup ...");
            spnegoTokencommonSetUp("S4U2SelfTest", "serverSpnego.xml", SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS,
                                   SPNEGOConstants.DONT_CREATE_SSL_CLIENT, SPNEGOConstants.DONT_CREATE_SPN_AND_KEYTAB,
                                   SPNEGOConstants.DEFAULT_REALM, SPNEGOConstants.CREATE_SPNEGO_TOKEN, SPNEGOConstants.SET_AS_COMMON_TOKEN, SPNEGOConstants.USE_CANONICAL_NAME,
                                   SPNEGOConstants.USE_COMMON_KEYTAB, SPNEGOConstants.DONT_START_SERVER,
                                   tokenAPIServletName, tokenAPIServletRootContext, SPNEGOConstants.USE_USER1);
        }
    }

    @Before
    public void beforeTest() throws Exception {
        Log.info(c, name.getMethodName(), "Common SPNEGO token was refreshed, so resetting the basic servlet URL parameters");
        Log.info(c, name.getMethodName(), "Resetting URL parameters to use user: " + InitClass.FIRST_USER);
        for (String paramListKey : parametersList.keySet()) {
            String paramList = parametersList.get(paramListKey);
            paramList = paramList.replaceAll("user[12]", InitClass.FIRST_USER);
            parametersList.put(paramListKey, paramList);
        }

        if (!isServerRootUpdatedInUrls) {
            Log.info(c, name.getMethodName(), "Updating parameter lists to replace server install root params");
            for (String paramListKey : parametersList.keySet()) {
                String paramList = parametersList.get(paramListKey);
                if (paramList.contains(SERVER_INSTALL_ROOT)) {
                    Log.info(c, name.getMethodName(), "Replacing server root in parameter list.");
                    paramList = paramList.replace(SERVER_INSTALL_ROOT, myServer.getInstallRoot());
                    Log.info(c, name.getMethodName(), "New parameter list after replacing server root");
                    parametersList.put(paramListKey, paramList);
                }
            }
            isServerRootUpdatedInUrls = true;
        }
        generateSpnegoTokenInsideTest();
    }

    public void generateSpnegoTokenInsideTest() throws Exception {
        if (FATSuite.OTHER_SUPPORT_JDKS) {
            createNewSpnegoToken(SPNEGOConstants.CREATE_SPNEGO_TOKEN, SPNEGOConstants.USE_USER1);
        }
    }

    /**
     * Test description:
     * - The test only run when using java 1.8 and on IBM JDK.
     * - This test will reconfigure the server.
     * - Then, it will create a new SPNEGO token.
     * - Next it will proceed to access the SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self() with that spnego token that was just created.
     * - The API is expected to create a new spnego tokens which can be used for outbound calls.
     *
     * Expected Results:
     * - A new Spnego tokens should be generated.
     *
     */

    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testS4U2SelfCommingInWithSpnegoToken() {

        System.setProperty("com.ibm.security.krb5.Krb5Debug", "all");
        System.setProperty("com.ibm.security.jgss.debug", "all");
        String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlUserID = urlBase + parametersList.get(key_parmsFromUserId);
        String urlS4U2Self = urlBase + parametersList.get(key_parmsFromS4U2Self);

        Log.info(c, name.getMethodName(), "Accessing servlet with URL for user id");
        Log.info(c, name.getMethodName(), "After that, we will access URL with S4u2Self ");
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("serverS4U2Self.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);

            // Access the servlet using SPNEGO token
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            String response = myClient.accessWithHeaders(urlUserID, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2_responseContainsToken(response, responseCheckForUserIdTest);

            String tokenString = extractTokenFromResponse(response);
            Log.info(c, name.getMethodName(), "Token string is: " + tokenString);

            headers = testHelper.setTestHeaders(tokenString, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);

            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created using SpnegotokenHelper API");
            response = myClient.accessWithHeaders(urlS4U2Self, expectedStatusCode, headers, ignoreErrorContent);

            expectation.spnegoTokenNotFound(response);
            myClient.resetClientState();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - The test only run when using java 1.8 and on IBM JDK.
     * - This test will reconfigure the server.
     * - Then, it will create a new SPNEGO token with the delegate option set to false. This will not forward the GSSCredentials to other servers.
     * - Next it will proceed to access the SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self() with that spnego token that was just created.
     * - The API is expected to create a new spnego tokens which can be used for outbound calls.
     *
     * Expected Results:
     * - A new Spnego tokens should be generated.
     *
     */
    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testS4U2SelfComingInWithSpnegoTokenAndDelegateFalse() {
        Log.info(c, name.getMethodName(), "Accessing servlet with SPNEGO token");
        testS4U2SelfAndDelegateFalse(Krb5Helper.SPNEGO_MECH_OID);
    }

    /**
     * Test description:
     * - The test only run when using java 1.8 and on IBM JDK.
     * - This test will reconfigure the server.
     * - Then, it will create a new Kerberos token with the delegate option set to false. This will not forward the GSSCredentials to other servers.
     * - Next it will proceed to access the SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self() with that spnego token that was just created.
     * - The API is expected to create a new spnego tokens which can be used for outbound calls.
     *
     * Expected Results:
     * - A new Spnego tokens should be generated.
     *
     */
    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testS4U2SelfComingInWithKerberosTokenAndDelegateFalse() {
        Log.info(c, name.getMethodName(), "Accessing servlet with Kerberos token");
        testS4U2SelfAndDelegateFalse(Krb5Helper.KRB5_MECH_OID);
    }

    public void testS4U2SelfAndDelegateFalse(Oid mechOid) {

        String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlUserID = urlBase + parametersList.get(key_parmsFromUserId);
        String urlS4U2Self = urlBase + parametersList.get(key_parmsFromS4U2Self);

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlUserID);
        Log.info(c, name.getMethodName(), "After that, we will access URL " + urlS4U2Self);
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("serverS4U2Self.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
            generateSpnegoTokenInsideTest();

            // Access the servlet using SPNEGO token
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String testSystemCanonicalHostName = InitClass.serverCanonicalHostName;
            String tokenString = testHelper.createToken(InitClass.FIRST_USER, InitClass.FIRST_USER_PWD, testSystemCanonicalHostName, InitClass.KDC_REALM,
                                                        InitClass.KDC_HOSTNAME, KRB5ConfPath, krb5Helper, false, mechOid);
            tokenString = "Negotiate " + tokenString;
            Log.info(c, name.getMethodName(), "Token string is: " + tokenString);

            headers = testHelper.setTestHeaders(tokenString, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);

            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created using SpnegotokenHelper API");
            String response = myClient.accessWithHeaders(urlS4U2Self, expectedStatusCode, headers, ignoreErrorContent);

            expectation.spnegoTokenNotFound(response);
            myClient.resetClientState();

            testHelper.setShutdownMessages(MessageConstants.GSSCREDENTIALS_NOT_RECEIVED_FOR_USER_CWWKS4310W);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - The test only run when using java 1.8 and on IBM JDK.
     * - This test will reconfigure the server.
     * - Next it will proceed to call the self api and will pass in the user id and password.
     * - The API is expected to create a spnego tokens
     *
     * Expected Results:
     * - A Spnego tokens should be generated.
     *
     */

    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testS4U2SelfWithUserIdPassword() {

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("serverS4U2BasicAuth.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
            generateSpnegoTokenInsideTest();

            String response = myClient.accessProtectedServletWithAuthorizedCredentials("/" + tokenAPIServletName + parametersList.get(key_parmsFromS4U2SelfWithKrb5Conf),
                                                                                       InitClass.FIRST_USER,
                                                                                       InitClass.FIRST_USER_PWD);

            expectation.spnegoTokenNotFound(response);
            myClient.resetClientState();

            String tokenString = extractTokenFromResponse(response);
            Log.info(c, name.getMethodName(), "Token string is: " + tokenString);
            generateSpnegoTokenInsideTest();

            response = performTokenValidation(tokenString, InitClass.FIRST_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - The test only run when using java 1.8 and on IBM JDK.
     * - This test will reconfigure the server.
     * - Next it will proceed to pass in userId and password to invoke a servlet.
     * - The method from the servlet that will be call, invokes the SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self API twice.
     * - The SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self() API is expected to create two SPNEGO tokens.
     *
     * Expected Results:
     * - Two Spnego tokens should be generated.
     *
     */
    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testS4U2SelfWithUserIdPasswordTwice() throws Exception {

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKS0008I");
        testHelper.reconfigureServer("serverS4U2BasicAuth.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
        generateSpnegoTokenInsideTest();
        String response = myClient.accessProtectedServletWithAuthorizedCredentials("/" + tokenAPIServletName + parametersList.get(key_parmsFromS4U2SelfWithKrb5ConfTwice),
                                                                                   InitClass.FIRST_USER,
                                                                                   InitClass.FIRST_USER_PWD);

        expectation.s4u2_responseContainsDifferentTokens(response);
        myClient.resetClientState();
    }

    /**
     * Test description:
     * - The test only run when using java 1.8 and on IBM JDK.
     * - This test will reconfigure the server to one that does not include the constrainedDelegation-1.0 feature.
     * - It will then proceed to invoke the SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self using userId and password.
     * - Because the feature is not part of the config a NoClassDefFoundError should appear on the logs.
     *
     * Expected Results:
     * - A NoClassDefFoundError should appear on the logs.
     *
     */
    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testConstrainedDelegationNoFeature() throws Exception {

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKS0008I");
        testHelper.reconfigureServer("serverNoConstrainedDelegationFeature.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER,
                                     SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
        generateSpnegoTokenInsideTest();
        String response = myClient.accessProtectedServletWithAuthorizedCredentials("/" + tokenAPIServletName + parametersList.get(key_parmsFromS4U2SelfWithKrb5ConfTwice),
                                                                                   InitClass.FIRST_USER,
                                                                                   InitClass.FIRST_USER_PWD);

        expectation.s4u2Proxy_NoClassDefFoundError(response);
        myClient.resetClientState();
    }

    /**
     * Test description:
     * - The test only run when using java 1.8 and on IBM JDK.
     * - The test will reconfigure the server to set the s4u2Self element to false.
     * - It will then try to call the s4u2self API SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self coming with UserId and password but will fail with the following
     * error CWWKS4342E.
     *
     * Expected Results:
     * - The following messages should appear on the logs CWWKS4342E.
     *
     */
    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testS4U2SelfElementNotEnabled() {

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("serverS4U2SelfNotEnabled.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
            generateSpnegoTokenInsideTest();
            myClient.accessProtectedServletWithAuthorizedCredentials("/" + tokenAPIServletName + parametersList.get(key_parmsFromS4U2Self),
                                                                     InitClass.FIRST_USER,
                                                                     InitClass.FIRST_USER_PWD);
            testHelper.checkForMessages(true, MessageConstants.S4U2SELF_IS_NOT_ENABLED_CWWKS4342E);
            myClient.resetClientState();

            testHelper.setShutdownMessages(MessageConstants.S4U2SELF_IS_NOT_ENABLED_CWWKS4342E);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - The test only run when using java 1.8..
     * - This test will reconfigure the server.
     * - Next it will proceed to create a new user with the s4u2 self attributes except the Delegate.
     * - Then it will proceed to hit the servlet that calls the SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self S4U2self api.
     * - The API will fail to create the SPNEGO Token with the following error:CWWKS4340E
     *
     * Expected Results:
     * - The following error should come CWWKS4340E
     *
     */
    //
    @AllowedFFDC({ "org.ietf.jgss.GSSException" })
    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testS4U2SelfWithUserIdPasswordSPNDoesNotAllowDelegation() throws Exception {

        KdcHelper testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);

        testKdcHelper.createSpnAndKeytab(null, true, SPNEGOConstants.S4U2_SELF_ALLOW_DELEGATION_FALSE_CMD_ARGS);
        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKS0008I");
        testHelper.reconfigureServer("serverS4U2BasicAuth.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
        generateSpnegoTokenInsideTest();

        String paramList = parametersList.get(key_parmsFromS4U2SelfWithKrb5Conf);
        myClient.accessProtectedServletWithAuthorizedCredentials("/" + tokenAPIServletName + paramList,
                                                                 InitClass.FIRST_USER,
                                                                 InitClass.FIRST_USER_PWD);
        testHelper.checkForMessages(true, MessageConstants.S4U2SELF_COULD_NOT_IMPERSONATE_USER_CWWKS4340E);
        testKdcHelper.createSpnAndKeytab(null, true, SPNEGOConstants.DEFAULT_CMD_ARGS);
        generateSpnegoTokenInsideTest();
        myClient.resetClientState();

        testHelper.setShutdownMessages(MessageConstants.S4U2SELF_COULD_NOT_IMPERSONATE_USER_CWWKS4340E);

    }

    /**
     * Test description:
     * - The test only run when using java 1.8 and on IBM JDK.
     * - This test will reconfigure the server.
     * - Next it will proceed to create a new user with the s4u2 self attributes without the Trusted Accouns Services.
     * - Then it will proceed to hit the servlet that calls the SpnegoHelper.buildS4U2ProxyAuthorizationUsingS4U2Self S4U2self api.
     * - The API will fail to create the SPNEGO Token with the following error:CWWKS4340E
     *
     * Expected Results:
     * - The following error should come CWWKS4340E
     *
     */
    //
    @AllowedFFDC({ "org.ietf.jgss.GSSException" })
    @Test
    @MinimumJavaLevel(javaLevel = 8)
    public void testS4U2SelfWithUserIdPasswordSPNDoesTustedAccountServiceAreFalse() throws Exception {

        KdcHelper testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);

        testKdcHelper.createSpnAndKeytab(null, true, SPNEGOConstants.S4U2_SELF_ALLOW_ACCOUNT_TRUSTED_FALSE_CMD_ARGS);
        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKS0008I");
        testHelper.reconfigureServer("serverS4U2BasicAuth.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
        generateSpnegoTokenInsideTest();

        String paramList = parametersList.get(key_parmsFromS4U2SelfWithKrb5Conf);
        myClient.accessProtectedServletWithAuthorizedCredentials("/" + tokenAPIServletName + paramList,
                                                                 InitClass.FIRST_USER,
                                                                 InitClass.FIRST_USER_PWD);
        testHelper.checkForMessages(true, MessageConstants.S4U2SELF_COULD_NOT_IMPERSONATE_USER_CWWKS4340E);
        testKdcHelper.createSpnAndKeytab(null, true, SPNEGOConstants.DEFAULT_CMD_ARGS);
        generateSpnegoTokenInsideTest();
        myClient.resetClientState();
        testHelper.setShutdownMessages(MessageConstants.S4U2SELF_COULD_NOT_IMPERSONATE_USER_CWWKS4340E);
    }

    private String extractTokenFromResponse(String response) {
        String thisMethod = "extractTokenFromResponse";
        Log.info(c, thisMethod, "Extracting token from response");
        String stringToFind = "token:";

        int stringToFindIndex = response.lastIndexOf(stringToFind);
        if (stringToFindIndex < 0) {
            Log.info(c, thisMethod, "Token was not found in the response");
            return null;
        }

        String token = response.substring(stringToFindIndex + stringToFind.length(), response.length());

        Log.info(c, thisMethod, "Found token string: " + token);
        return token;
    }

    /**
     * A second active Liberty server will be used to verify the SPNEGO obtained from an earlier API call. That SPNEGO token will
     * be sent in the authorization header in a call to a protected resource on the second server. If the SPNEGO token is valid,
     * access to the protected resource should be successful and appropriate subject information will be output.
     *
     * @param spnegoToken
     * @param user
     * @param isEmployee
     * @param isManager
     * @return Response from the protected resource invocation
     * @throws Exception
     */
    private String performTokenValidation(String spnegoToken, String user, boolean isEmployee, boolean isManager) throws Exception {
        String thisMethod = "performTokenValidation";
        Log.info(c, thisMethod, "Performing token validation");

        String response = null;
        LibertyServer backendServer = LibertyServerFactory.getLibertyServer("BackendServer");

        try {
            setupServer(backendServer, null, SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS, SPNEGOConstants.USE_COMMON_KEYTAB, SPNEGOConstants.START_SERVER);

            // Use the token returned from the API to access a resource on the second server
            Log.info(c, thisMethod, "Using provided SPNEGO token to access a protected resource on a second server");

            BasicAuthClient secondClient = new BasicAuthClient(testHelper.getTestSystemFullyQualifiedDomainName(), backendServer
                            .getHttpSecondaryPort(), BasicAuthClient.DEFAULT_REALM, SPNEGOConstants.SIMPLE_SERVLET_NAME, "/basicauth");

            Map<String, String> headers = testHelper.setTestHeaders(spnegoToken, SPNEGOConstants.FIREFOX, SPNEGOConstants.S4U_BACKEND_SERVICE, null);
            response = secondClient.accessProtectedServletWithValidHeaders(SPNEGOConstants.SIMPLE_SERVLET, headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.successfulServletResponse(response, secondClient, user, isEmployee, isManager);
            expectation.s4u2_responseFromBackendServer(response, user);

            backendServer.stopServer();

        } catch (Exception e) {
            Log.info(c, thisMethod, "Unexpected exception: " + CommonTest.maskHostnameAndPassword(e.getMessage()));
            try {
                if (backendServer != null) {
                    backendServer.stopServer();
                }
            } catch (Exception ex) {
                Log.warning(c, "Unexpected exception thrown while shutting down backend server");
                Log.error(c, thisMethod, ex);
            }
            // Re-throw the original exception since that should be the real problem
            throw e;
        }
        return response;
    }

}
