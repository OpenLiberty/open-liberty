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

import static org.junit.Assert.assertTrue;
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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 *
 */
@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
@MinimumJavaLevel(javaLevel = 8)
public class S4U2ProxyTest extends CommonTest {

    private static final Class<?> c = S4U2ProxyTest.class;
    private static final String tokenAPIServletName = "SPNEGOTokenHelperServlet";
    private static final String tokenAPIServletRootContext = "/SPNEGOTokenHelperFVTWeb";
    private static final String servletURL = "/SPNEGOTokenHelperFVTWeb/SPNEGOTokenHelperServlet";

    // Positive test expected results
    private static final String responseCheckForSubjectTest = "Call to SpnegoTokenHelper.buildSpnegoAuthorizationFromSubject Succeeded  ";
    private static final String responseCheckForS4U2Proxy = "Call to SpnegoHelper.buildS4U2proxyAuthorization Succeeded  ";

    private static final String responseCheckForFailedSubjectTest = "Call to SpnegoTokenHelper.buildSpnegoAuthorizationFromSubject failed.";
    private static final String responseCheckForFailedS4U2ProxyTest = "Call to SpnegoHelper.buildS4U2proxyAuthorization failed.";

    private static final String key_parmsFromSubjectS4U2Proxy = "parmsFromSubjectS4U2Proxy";
    private static final String key_parmsFromSubject = "parmsFromSubject";
    private static final String key_parmsFromSubjectS4U2Proxy2 = "parmsFromSubjectS4U2Proxy2";
    private static final String key_parmsFromSubjectS4U2Proxy3 = "parmsFromSubjectS4U2Proxy3";
    private static final String key_parmsFromUserId = "parmsFromUserId";
    private static final String key_parmsFromUpn = "parmsFromUpn";
    private static final String key_parmsNegative = "parmsNegative";

    static String localHostMachine = "localhost";

    //modified the JAASCLIENT to JAASClientUseKeytab from JAASClient
    private static final String servletURLParms = "?ActiveUserid=" + InitClass.FIRST_USER + "&ActivePwd=" + InitClass.FIRST_USER_PWD
                                                  + "&JAAS=JAASClientUseKeytab&" + "UPN=" + InitClass.FIRST_USER + "@" + InitClass.KDC_REALM;
    private static final String envDelegationParm = "ENV=Delegation";
    private static final String targetServerSpnParm = "SPN=HTTP/" + TARGET_SERVER;
    private static final String targetServerBackenedServer = "SPN=HTTP/" + SPNEGOConstants.S4U_BACKEND_SERVICE;

    private static final String localhostSpnParm = "SPN=HTTP/" + localHostMachine;

    private static final String parmsFromSubjectS4U2Proxy = servletURLParms + "&" + envDelegationParm + "&" + targetServerBackenedServer + "&TEST=fromsubject";

    private static final String parmsFromSubject = servletURLParms + "&" + envDelegationParm + "&" + targetServerSpnParm + "&TEST=fromsubject";
    private static final String parmsFromSubjectS4U2Proxy2 = servletURLParms + "&" + envDelegationParm + "&" + targetServerBackenedServer + "&TEST=froms4u2proxy";
    private static final String parmsFromSubjectS4U2Proxy3 = servletURLParms + "&" + envDelegationParm + "&" + targetServerBackenedServer + "&TEST=s4u2proxymulti";
    private static final String parmsFromUserId = servletURLParms + "&" + envDelegationParm + "&" + localhostSpnParm + "&TEST=fromuserid";
    private static final String parmsFromUpn = servletURLParms + "&" + envDelegationParm + "&" + targetServerSpnParm + "&TEST=fromupn";
    private static final String parmsNegative = servletURLParms + "&" + envDelegationParm + "&" + targetServerSpnParm + "&TEST=negative";
    private static Map<String, String> parametersList = new HashMap<String, String>();
    static {

        parametersList.put(key_parmsFromSubjectS4U2Proxy, parmsFromSubjectS4U2Proxy);
        parametersList.put(key_parmsFromSubject, parmsFromSubject);
        parametersList.put(key_parmsFromSubjectS4U2Proxy2, parmsFromSubjectS4U2Proxy2);
        parametersList.put(key_parmsFromSubjectS4U2Proxy3, parmsFromSubjectS4U2Proxy3);
        parametersList.put(key_parmsFromUserId, parmsFromUserId);
        parametersList.put(key_parmsFromUpn, parmsFromUpn);
        parametersList.put(key_parmsNegative, parmsNegative);
    }

    // these args are to ensure we have User Account Control setup with trust delegation,trust auth delegation; and no trusted service setup
    public final static Map<String, String> S4U2_PROXY_NO_TRUSTED_SERVICE_CMD_ARGS = new HashMap<String, String>();
    static {
        S4U2_PROXY_NO_TRUSTED_SERVICE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_DELEGATION, "true");
        S4U2_PROXY_NO_TRUSTED_SERVICE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_AUTH_DELEGATION, "true");
        S4U2_PROXY_NO_TRUSTED_SERVICE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_ALLOWED_TO_DELEGATE, "false");
        S4U2_PROXY_NO_TRUSTED_SERVICE_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_PASSWORD_NEVER_EXPIRES, "true");
        S4U2_PROXY_NO_TRUSTED_SERVICE_CMD_ARGS.put(SPNEGOConstants.ARG_DISABLE_KERBEROS_PRE_AUTH, "false");

    }

    public final static Map<String, String> S4U2_PROXY_NO_UAC_4_DELEGATION_CMD_ARGS = new HashMap<String, String>();
    static {
        S4U2_PROXY_NO_UAC_4_DELEGATION_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_DELEGATION, "false");
        S4U2_PROXY_NO_UAC_4_DELEGATION_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_TRUSTED_FOR_AUTH_DELEGATION, "false");
        S4U2_PROXY_NO_UAC_4_DELEGATION_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_ALLOWED_TO_DELEGATE, "true");
        S4U2_PROXY_NO_UAC_4_DELEGATION_CMD_ARGS.put(SPNEGOConstants.ARG_ENABLE_PASSWORD_NEVER_EXPIRES, "true");
        S4U2_PROXY_NO_UAC_4_DELEGATION_CMD_ARGS.put(SPNEGOConstants.ARG_DISABLE_KERBEROS_PRE_AUTH, "false");

    }

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";

        if (InitClass.isRndHostName) {
            Log.info(c, thisMethod, "Not running S4U2ProxyTest because randomized hostname is used.");
            Assume.assumeTrue(false); //This disables this test class. None of the tests in the class will be run.
        } else {
            Log.info(c, thisMethod, "Starting the server and kerberos setup ...");
            spnegoTokencommonSetUp("S4U2ProxyTest", "serverSpnego.xml", SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS,
                                   SPNEGOConstants.DONT_CREATE_SSL_CLIENT, SPNEGOConstants.DONT_CREATE_SPN_AND_KEYTAB,
                                   SPNEGOConstants.DEFAULT_REALM, SPNEGOConstants.CREATE_SPNEGO_TOKEN, SPNEGOConstants.SET_AS_COMMON_TOKEN, SPNEGOConstants.USE_CANONICAL_NAME,
                                   SPNEGOConstants.USE_COMMON_KEYTAB, SPNEGOConstants.DONT_START_SERVER,
                                   tokenAPIServletName, tokenAPIServletRootContext, SPNEGOConstants.USE_USER1);
        }

        FATSuite.transformApps(myServer, "basicauth.war", "SPNEGOTokenHelperFVT.ear");
    }

    @Before
    public void beforeTest() {
        if (wasCommonTokenRefreshed) {
            Log.info(c, name.getMethodName(), "Common SPNEGO token was refreshed, so resetting the basic servlet URL parameters");
            Log.info(c, name.getMethodName(), "Resetting URL parameters to use user: " + InitClass.FIRST_USER);
            for (String paramListKey : parametersList.keySet()) {
                String paramList = parametersList.get(paramListKey);
                paramList = paramList.replaceAll("user[12]", InitClass.FIRST_USER);
                parametersList.put(paramListKey, paramList);
            }
        }
    }

    /**
     * Test description:
     * - Invoke a servlet not passing in GSSCred from client that will take the parms passed to create a SPNEGO token via an API
     * - verify GssCredential was found in Subject, and verify Krb5ProxyCredential was found as well as Self and Client lines
     * Expected results:
     * - We should see expected results in servlet response.
     */

    @Test
    public void testS4U2ProxyFromSubjectNotPassClientGssCredTest_spnegoToken() {
        Log.info(c, name.getMethodName(), "Access servlet with SPNEGO token");
        testS4U2ProxyFromSubjectNotPassClientGssCredTest(Krb5Helper.SPNEGO_MECH_OID);
    }

    /**
     * Test description:
     * - Invoke a servlet not passing in GSSCred from client that will take the parms passed to create a Kerberos token via an API
     * - verify GssCredential was found in Subject, and verify Krb5ProxyCredential was found as well as Self and Client lines
     * Expected results:
     * - We should see expected results in servlet response.
     */

    @Test
    public void testS4U2ProxyFromSubjectNotPassClientGssCredTest_kerberosToken() {
        Log.info(c, name.getMethodName(), "Access servlet with Kerberos token");
        testS4U2ProxyFromSubjectNotPassClientGssCredTest(Krb5Helper.KRB5_MECH_OID);
    }

    /**
     *
     */
    private void testS4U2ProxyFromSubjectNotPassClientGssCredTest(Oid mechOid) {

        // using TARGET_SERVER in url for server so as not to use localHost
        String urlBase = "http://" + TARGET_SERVER + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlS4u2Proxy = urlBase + parametersList.get(key_parmsFromSubjectS4U2Proxy);
        //       String urlNonDelegation = urlBase + parmsFromCallerSubjectNonDelegation;

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlS4u2Proxy + " with" + krb5Helper.mechOidString(mechOid));
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("S4U2Proxy_serverSpnego.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);

            // create token with the mechOid with no delegateClientGssCred
            String spnegoToken = testHelper.createToken(InitClass.FIRST_USER_KRB5_FQN, InitClass.FIRST_USER_KRB5_FQN_PWD, TARGET_SERVER, null, null,
                                                        SPNEGOConstants.KRB5_CONF_FILE,
                                                        krb5Helper, false, mechOid);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + spnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            // Access the servlet using SPNEGO token with no clientGssCred but forwardable token
            String response = myClient.accessWithHeaders(urlS4u2Proxy, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2_responseContainsToken(response, responseCheckForSubjectTest);
            String tokenString = extractTokenFromResponse(response);
            Log.info(c, name.getMethodName(), "Token string is: " + tokenString);

            expectation.s4u2_validateKerberosAndGSSCred(response);

            myClient.resetClientState();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Invoke a servlet not passing in GSSCred from client that will take the parms passed to create a SPNEGO token via the
     * SpnegoHelper.buildS4U2proxyAuthorization() API
     * - Verify GSSCredential was found in Subject, and verify Krb5ProxyCredential was found as well as Self and Client lines
     * - Use the SPNEGO token received from the API in a request to access a protected resource on a second server
     * - Verify that the S4U2Proxy GSSCredential was found in the subject output from the protected resource on the second server
     * Expected results:
     * - We should see expected results in servlet response.
     * - Access to the protected resource on the second server should succeed.
     */

    @Test
    public void testSpnegoS4U2ProxyAPITest_spnegoToken() {
        Log.info(c, name.getMethodName(), "Accessing servlet with SPNEGO token");
        testSpnegoS4U2ProxyAPITest(Krb5Helper.SPNEGO_MECH_OID);
    }

    /**
     * Test description:
     * - Invoke a servlet not passing in GSSCred from client that will take the parms passed to create a SPNEGO token via the
     * SpnegoHelper.buildS4U2proxyAuthorization() API
     * - Verify GSSCredential was found in Subject, and verify Krb5ProxyCredential was found as well as Self and Client lines
     * - Use the SPNEGO token received from the API in a request to access a protected resource on a second server
     * - Verify that the S4U2Proxy GSSCredential was found in the subject output from the protected resource on the second server
     * Expected results:
     * - We should see expected results in servlet response.
     * - Access to the protected resource on the second server should succeed.
     */

    @Test
    public void testSpnegoS4U2ProxyAPITest_KerberosToken() {
        Log.info(c, name.getMethodName(), "Accessing servlet with Kerberos token");
        testSpnegoS4U2ProxyAPITest(Krb5Helper.KRB5_MECH_OID);
    }

    private void testSpnegoS4U2ProxyAPITest(Oid mechOid) {

        // using TARGET_SERVER in url for server so as not to use localHost
        String urlBase = "http://" + TARGET_SERVER + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlS4u2Proxy = urlBase + parametersList.get(key_parmsFromSubjectS4U2Proxy2);
        //       String urlNonDelegation = urlBase + parmsFromCallerSubjectNonDelegation;

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlS4u2Proxy);
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("S4U2Proxy_serverSpnego.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);

            // create SPNEGO token with no delegateClientGssCred
            String spnegoToken = testHelper.createToken(InitClass.FIRST_USER_KRB5_FQN, InitClass.FIRST_USER_KRB5_FQN_PWD, TARGET_SERVER, null, null,
                                                        SPNEGOConstants.SERVER_KRB5_CONFIG_FILE, krb5Helper, false, mechOid);

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + spnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            // Access the servlet using SPNEGO token with no clientGssCred but forwardable token
            String response = myClient.accessWithHeaders(urlS4u2Proxy, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2_responseContainsToken(response, responseCheckForS4U2Proxy);
            String tokenString = extractTokenFromResponse(response);
            Log.info(c, name.getMethodName(), "Token string is: " + tokenString);

            expectation.s4u2_validateKerberosAndGSSCred(response);

            myClient.resetClientState();

            response = performTokenValidation(tokenString, InitClass.FIRST_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Invoke a servlet not passing in GSSCred from client that will take the parms passed to create a SPNEGO token via an API
     * - verify GssCredential was found in Subject, and verify Krb5ProxyCredential was found as well as Self and Client lines
     * Expected results:
     * - We should see expected results in servlet response.
     */

    @Test
    public void testSpnegoS4U2ProxyAPIMultipleCallTest() {

        // using TARGET_SERVER in url for server so as not to use localHost
        String urlBase = "http://" + TARGET_SERVER + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlS4U2Proxy = urlBase + parametersList.get(key_parmsFromSubjectS4U2Proxy3);
        //       String urlNonDelegation = urlBase + parmsFromCallerSubjectNonDelegation;

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlS4U2Proxy);
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("S4U2Proxy_serverSpnego.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
            // create SPNEGO token with no delegateClientGssCred
            String spnegoToken = testHelper.createSpnegoToken(InitClass.FIRST_USER_KRB5_FQN, InitClass.FIRST_USER_KRB5_FQN_PWD, TARGET_SERVER, null, null,
                                                              SPNEGOConstants.KRB5_CONF_FILE,
                                                              krb5Helper, false);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + spnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            // Access the servlet using SPNEGO token with no clientGssCred but forwardable token
            String response = myClient.accessWithHeaders(urlS4U2Proxy, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2_responseContainsToken(response, responseCheckForS4U2Proxy, true);
            Log.info(c, name.getMethodName(), "Servlet response: " + response);

            expectation.s4u2_validateKerberosAndGSSCred(response);

            myClient.resetClientState();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Invoke a servlet not passing in GSSCred from client that will take parms passed and attempt to create a SPNEGO token via an API
     * - we will set the S4U2ProxyEnabled flag to false and expect failures (ffdc for PrivelegedAction)
     * Expected results:
     * - We should see expected results in servlet response.
     */

    @AllowedFFDC({ "java.security.PrivilegedActionException" })
    @Test
    public void testS4U2ProxyWithProxyEnabledFalseTest() {

        // using TARGET_SERVER in url for server so as not to use localHost
        String urlBase = "http://" + TARGET_SERVER + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlS4U2Proxy = urlBase + parametersList.get(key_parmsFromSubjectS4U2Proxy);
        //       String urlNonDelegation = urlBase + parmsFromCallerSubjectNonDelegation;

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlS4U2Proxy);
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("S4U2ProxyEnabled_false.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);

            // create SPNEGO token with no delegateClientGssCred
            String spnegoToken = testHelper.createSpnegoToken(InitClass.FIRST_USER_KRB5_FQN, InitClass.FIRST_USER_KRB5_FQN_PWD, TARGET_SERVER, null, null,
                                                              SPNEGOConstants.KRB5_CONF_FILE,
                                                              krb5Helper, false);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + spnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            // Access the servlet using SPNEGO token with no clientGssCred but forwardable token
            String response = myClient.accessWithHeaders(urlS4U2Proxy, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2_checkForFailedSubject(response, responseCheckForFailedSubjectTest);

            String tokenString = extractTokenFromResponse(response);
            Log.info(c, name.getMethodName(), "Token string is: " + tokenString);
            // check for expected failure

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
     * - Invoke a servlet not passing in GSSCred from client that will take parms passed and attempt to create a SPNEGO token via an API
     * - we will set the S4U2ProxyEnabled flag to false and expect failures
     * Expected results:
     * - We should see expected results in servlet response.
     */
    @Test
    public void testS4U2ProxyAPIWithEnabeldFalseTest() {

        // using TARGET_SERVER in url for server so as not to use localHost
        String urlBase = "http://" + TARGET_SERVER + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlS4U2Proxy = urlBase + parametersList.get(key_parmsFromSubjectS4U2Proxy2);
        //       String urlNonDelegation = urlBase + parmsFromCallerSubjectNonDelegation;

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlS4U2Proxy);
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("S4U2ProxyEnabled_false.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);

            // create SPNEGO token with no delegateClientGssCred
            String spnegoToken = testHelper.createSpnegoToken(InitClass.FIRST_USER_KRB5_FQN, InitClass.FIRST_USER_KRB5_FQN_PWD, TARGET_SERVER, null, null,
                                                              SPNEGOConstants.KRB5_CONF_FILE,
                                                              krb5Helper, false);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + spnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            // Access the servlet using SPNEGO token with no clientGssCred but forwardable token
            String response = myClient.accessWithHeaders(urlS4U2Proxy, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2_checkForFailedSubject(response, responseCheckForFailedS4U2ProxyTest);
            expectation.s4u2Proxy_NotEnabled(response);

            String tokenString = extractTokenFromResponse(response);
            Log.info(c, name.getMethodName(), "Token string is: " + tokenString);
            // check for proper failure

            myClient.resetClientState();

            testHelper.setShutdownMessages(MessageConstants.S4U2PROXY_SELF_S4U2PROXY_IS_NOT_ENABLED_CWWKS4343E,
                                           MessageConstants.GSSCREDENTIALS_NOT_RECEIVED_FOR_USER_CWWKS4310W);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Invoke a servlet and passing in GSSCred from client that will take the parms passed to create a SPNEGO token via an API
     * - verify GssCredential was found in Subject, and verify no Krb5ProxyCredential was found as well as no Self lines
     * Expected results:
     * - We should see expected results in servlet response.
     */

    @Test
    public void testS4U2ProxyPassClientGssCredTest() {

        // using TARGET_SERVER in url for server so as not to use localHost
        String urlBase = "http://" + TARGET_SERVER + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlS4U2Proxy = urlBase + parametersList.get(key_parmsFromSubjectS4U2Proxy2);
        //       String urlNonDelegation = urlBase + parmsFromCallerSubjectNonDelegation;

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlS4U2Proxy);
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("S4U2Proxy_serverSpnego.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);

            // create SPNEGO token with no delegateClientGssCred
            String spnegoToken = testHelper.createSpnegoToken(InitClass.FIRST_USER_KRB5_FQN, InitClass.FIRST_USER_KRB5_FQN_PWD, TARGET_SERVER, null, null,
                                                              SPNEGOConstants.KRB5_CONF_FILE,
                                                              krb5Helper, true);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + spnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            // Access the servlet using SPNEGO token with no clientGssCred but forwardable token
            String response = myClient.accessWithHeaders(urlS4U2Proxy, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2_responseContainsToken(response, responseCheckForS4U2Proxy);
            String tokenString = extractTokenFromResponse(response);
            Log.info(c, name.getMethodName(), "Token string is: " + tokenString);

            expectation.s4u2_krbCredNotPresent(response);
            myClient.resetClientState();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Invoke a servlet and not passing in GSSCred from client that will take parms passed and attempt to create a SPNEGO token via an API
     * - there is no constarinedDelegation-1.0 feature defined and API call should fail
     * Expected results:
     * - We should see expected results in servlet response.
     */

    @Test
    public void testS4U2ProxyNoConstrainedDelegationFeatureTest() {

        // using TARGET_SERVER in url for server so as not to use localHost
        String urlBase = "http://" + TARGET_SERVER + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlS4U2Proxy = urlBase + parametersList.get(key_parmsFromSubjectS4U2Proxy2);
        //       String urlNonDelegation = urlBase + parmsFromCallerSubjectNonDelegation;

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlS4U2Proxy);
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("S4U2Proxy_noFeature.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);

            // create SPNEGO token with no delegateClientGssCred
            String spnegoToken = testHelper.createSpnegoToken(InitClass.FIRST_USER_KRB5_FQN, InitClass.FIRST_USER_KRB5_FQN_PWD, TARGET_SERVER, null, null,
                                                              SPNEGOConstants.KRB5_CONF_FILE,
                                                              krb5Helper, true);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + spnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            // Access the servlet using SPNEGO token with no clientGssCred but forwardable token
            String response = myClient.accessWithHeaders(urlS4U2Proxy, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2Proxy_NoClassDefFoundError(response);
            myClient.resetClientState();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Invoke a servlet using basicauth
     * - setup SPN to not have a trusted Service but have User Account Control set with trust for delegation and trust for auth delegation
     * - call SpnegoHelper.buildS4U2proxyAuthorization
     * Expected results:
     * - We should see expected results in servlet response.
     * - Expect failure on new API invocation for bogus setup (privelegedException and GssException but cannot catch the secondary)
     */

    @AllowedFFDC({ "java.security.PrivilegedActionException" })
    @Test
    public void testSpnegoS4U2ProxyAPINoTrustedServiceTest() {

        // using TARGET_SERVER in url for server so as not to use localHost
        String urlBase = "http://" + TARGET_SERVER + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlUserID = urlBase + parametersList.get(key_parmsFromSubjectS4U2Proxy2);
        //       String urlNonDelegation = urlBase + parmsFromCallerSubjectNonDelegation;

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlUserID);

        try {

            KdcHelper testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);

            testKdcHelper.createSpnAndKeytab(null, SPNEGOConstants.USE_CANONICAL_NAME, S4U2_PROXY_NO_TRUSTED_SERVICE_CMD_ARGS);
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("S4U2Proxy_BasicAuth.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
            String response = myClient.accessProtectedServletWithAuthorizedCredentials("/" + tokenAPIServletName + parametersList.get(key_parmsFromSubjectS4U2Proxy2),
                                                                                       InitClass.FIRST_USER,
                                                                                       InitClass.FIRST_USER_PWD);
            expectation.responseContainsInvalidCredentialError(response);
            myClient.resetClientState();

            testKdcHelper.createSpnAndKeytab(null, SPNEGOConstants.USE_CANONICAL_NAME, SPNEGOConstants.DEFAULT_CMD_ARGS);

        } catch (Exception e) {
            String message = CommonTest.maskHostnameAndPassword(e.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Invoke a servlet using basicauth
     * - setup SPN to have a trusted Service but have User Account Control not set with trust for delegation and trust for auth delegation
     * - call SpnegoHelper.buildS4U2proxyAuthorization
     * Expected results:
     * - We should see expected results in servlet response.
     * - Expect failure on new API invocation for bogus setup (privelegedException and GssException but cannot catch the secondary)
     */
    @AllowedFFDC({ "java.security.PrivilegedActionException" })
    @Test
    public void testSpnegoS4U2ProxyAPINoUserAccountControlForDelegationTest() {

        // using TARGET_SERVER in url for server so as not to use localHost
        String urlBase = "http://" + TARGET_SERVER + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlUserID = urlBase + parametersList.get(key_parmsFromSubjectS4U2Proxy2);
        //       String urlNonDelegation = urlBase + parmsFromCallerSubjectNonDelegation;

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlUserID);

        try {

            KdcHelper testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);

            testKdcHelper.createSpnAndKeytab(null, SPNEGOConstants.USE_CANONICAL_NAME, S4U2_PROXY_NO_UAC_4_DELEGATION_CMD_ARGS);
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("S4U2Proxy_BasicAuth.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
            String response = myClient.accessProtectedServletWithAuthorizedCredentials("/" + tokenAPIServletName + parametersList.get(key_parmsFromSubjectS4U2Proxy2),
                                                                                       InitClass.FIRST_USER,
                                                                                       InitClass.FIRST_USER_PWD);
            expectation.responseContainsInvalidCredentialError(response);
            myClient.resetClientState();
            testKdcHelper.createSpnAndKeytab(null, SPNEGOConstants.USE_CANONICAL_NAME, SPNEGOConstants.DEFAULT_CMD_ARGS);
        } catch (Exception e) {
            String message = CommonTest.maskHostnameAndPassword(e.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
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
        FATSuite.transformApps(backendServer, "basicauth.war");

        try {
            setupServer(backendServer, null, SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS, SPNEGOConstants.USE_COMMON_KEYTAB, SPNEGOConstants.START_SERVER);

            // Use the token returned from the API to access a resource on the second server
            Log.info(c, thisMethod, "Using provided SPNEGO token to access a protected resource on a second server");

            BasicAuthClient secondClient = new BasicAuthClient(testHelper.getTestSystemFullyQualifiedDomainName(), backendServer
                            .getHttpSecondaryPort(), BasicAuthClient.DEFAULT_REALM, SPNEGOConstants.SIMPLE_SERVLET_NAME, "/basicauth");

            Map<String, String> headers = testHelper.setTestHeaders(spnegoToken, SPNEGOConstants.FIREFOX, SPNEGOConstants.S4U_BACKEND_SERVICE, null);
            response = secondClient.accessProtectedServletWithValidHeaders(SPNEGOConstants.SIMPLE_SERVLET, headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            assertTrue("Expected to receive a successful response but found a problem.", secondClient.verifyResponse(response, user, isEmployee, isManager));
            assertTrue("Did not find expected principal value in backend servlet response.", response.contains("Principal: " + user + "@" + InitClass.KDC_REALM));

        } catch (Exception e) {
            Log.info(c, thisMethod, "Unexpected exception: " + CommonTest.maskHostnameAndPassword(e.getMessage()));
            throw e;
        } finally {
            try {
                if (backendServer != null) {
                    backendServer.stopServer();
                }
            } catch (Exception ex) {
                Log.warning(c, "Unexpected exception thrown while shutting down backend server");
                Log.error(c, thisMethod, ex);
            }
        }

        return response;
    }

}
