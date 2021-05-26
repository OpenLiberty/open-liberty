/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.MessageConstants;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class SpnegoTokenHelperApiTest extends CommonTest {

    private static final Class<?> c = SpnegoTokenHelperApiTest.class;
    private static final String tokenAPIServletName = "SPNEGOTokenHelperServlet";
    private static final String tokenAPIServletRootContext = "/SPNEGOTokenHelperFVTWeb";
    private static final String servletURL = "/SPNEGOTokenHelperFVTWeb/SPNEGOTokenHelperServlet";

    // Positive test expected results
    private static final String responseCheckForCallerSubjectTest = "Delegate From Caller Subject Test #2 Succeeded  ";
    private static final String responseCheckForSubjectTest = "Call to SpnegoTokenHelper.buildSpnegoAuthorizationFromSubject Succeeded  ";

    private static final String responseCheckForUserIdTest = "Delegate UserId Password Test #2 Succeeded";

    // Negative test expected results
    private static final String key_parmsFromCallerSubjectDelegation = "parmsFromCallerSubjectDelegation";
    private static final String key_parmsFromCallerSubjectNonDelegation = "parmsFromCallerSubjectNonDelegation";
    private static final String key_parmsFromSubjectS4U2Proxy = "parmsFromSubjectS4U2Proxy";
    private static final String key_parmsFromS4U2Self = "parmsFromS4U2Self";
    private static final String key_parmsFromS4U2SelfWithKrb5Conf = "parmsFromS4U2SelfWithConf";
    private static final String key_parmsFromS4U2SelfWithKrb5ConfTwice = "parmsFromS4U2SelfWithConfTwice";
    private static final String key_parmsFromSubject = "parmsFromSubject";
    private static final String key_parmsFromSubjectS4U2Proxy2 = "parmsFromSubjectS4U2Proxy2";
    private static final String key_parmsFromSubjectS4U2Proxy3 = "parmsFromSubjectS4U2Proxy3";
    private static final String key_parmsFromUserId = "parmsFromUserId";
    private static final String key_parmsFromUpn = "parmsFromUpn";
    private static final String key_parmsNegative = "parmsNegative";

    static String localHostMachine = "localhost";
    private static final String SERVER_INSTALL_ROOT = "{$server.install.root}";

    //modified the JAASCLIENT to JAASClientUseKeytab from JAASClient
    private static final String servletURLParms = "?ActiveUserid=" + FATSuite.COMMON_TOKEN_USER + "&ActivePwd=" + FATSuite.COMMON_TOKEN_USER_PWD
                                                  + "&JAAS=JAASClientUseKeytab&" + "UPN=" + FATSuite.COMMON_TOKEN_USER + "@" + InitClass.KDC_REALM;
    private static final String envDelegationParm = "ENV=Delegation";
    private static final String envNonDelegationParm = "ENV=NonDelegation";
    private static final String envS4U2Self = "ENV=S4U2Self";
    private static final String targetServerSpnParm = "SPN=HTTP/" + TARGET_SERVER;
    private static final String targetServerBackenedServer = "SPN=HTTP/" + SPNEGOConstants.S4U_BACKEND_SERVICE;
    private static final String delegateServerSpn = "DELEGATESPN=HTTP/" + TARGET_SERVER;

    private static final String localhostSpnParm = "SPN=HTTP/" + localHostMachine;

    private static final String KRB5KeytabPath = SERVER_INSTALL_ROOT + "/usr/servers/SpnegoTokenHelperTest/resources/security/kerberos/krb5.keytab";
    private static final String KRB5ConfPath = SERVER_INSTALL_ROOT + "/usr/servers/SpnegoTokenHelperTest/resources/security/kerberos/krb5.conf";

    private static final String parmsFromCallerSubjectDelegation = servletURLParms + "&" + envDelegationParm + "&" + targetServerSpnParm + "&TEST=fromcallersubject";
    private static final String parmsFromCallerSubjectNonDelegation = servletURLParms + "&" + envNonDelegationParm + "&" + targetServerSpnParm + "&TEST=fromcallersubject";
    private static final String parmsFromSubjectS4U2Proxy = servletURLParms + "&" + envDelegationParm + "&" + targetServerBackenedServer + "&TEST=fromsubject";
    private static final String parmsFromS4U2Self = servletURLParms + "&" + envS4U2Self + "&" + targetServerBackenedServer
                                                    + "&" + delegateServerSpn + "&KRB5KEYTAB=" + KRB5KeytabPath
                                                    + "&TEST=S4U2SelfTest";
    private static final String parmsFromS4U2SelfBasicAuth = servletURLParms + "&" + envS4U2Self + "&" + targetServerBackenedServer
                                                             + "&" + delegateServerSpn + "&KRB5KEYTAB=" + KRB5KeytabPath + "&KRB5CONF=" + KRB5ConfPath;

    private static final String parmsFromS4U2SelfWithKrb5Conf = parmsFromS4U2SelfBasicAuth + "&TEST=S4U2SelfTest";
    private static final String parmsFromS4U2SelfWithKrb5ConfTwice = parmsFromS4U2SelfBasicAuth + "&TEST=S4U2SelfTestCallAPITwice";
    private static final String parmsFromSubject = servletURLParms + "&" + envDelegationParm + "&" + targetServerSpnParm + "&TEST=fromsubject";
    private static final String parmsFromSubjectS4U2Proxy2 = servletURLParms + "&" + envDelegationParm + "&" + targetServerBackenedServer + "&TEST=froms4u2proxy";
    private static final String parmsFromSubjectS4U2Proxy3 = servletURLParms + "&" + envDelegationParm + "&" + targetServerBackenedServer + "&TEST=s4u2proxymulti";
    private static final String parmsFromUserId = servletURLParms + "&" + envDelegationParm + "&" + localhostSpnParm + "&TEST=fromuserid";
    private static final String parmsFromUpn = servletURLParms + "&" + envDelegationParm + "&" + targetServerSpnParm + "&TEST=fromupn";
    private static final String parmsNegative = servletURLParms + "&" + envDelegationParm + "&" + targetServerSpnParm + "&TEST=negative";
    private static Map<String, String> parametersList = new HashMap<String, String>();
    static {
        parametersList.put(key_parmsFromCallerSubjectDelegation, parmsFromCallerSubjectDelegation);
        parametersList.put(key_parmsFromCallerSubjectNonDelegation, parmsFromCallerSubjectNonDelegation);
        parametersList.put(key_parmsFromSubjectS4U2Proxy, parmsFromSubjectS4U2Proxy);
        parametersList.put(key_parmsFromS4U2Self, parmsFromS4U2Self);
        parametersList.put(key_parmsFromS4U2SelfWithKrb5Conf, parmsFromS4U2SelfWithKrb5Conf);
        parametersList.put(key_parmsFromS4U2SelfWithKrb5ConfTwice, parmsFromS4U2SelfWithKrb5ConfTwice);
        parametersList.put(key_parmsFromSubject, parmsFromSubject);
        parametersList.put(key_parmsFromSubjectS4U2Proxy2, parmsFromSubjectS4U2Proxy2);
        parametersList.put(key_parmsFromSubjectS4U2Proxy3, parmsFromSubjectS4U2Proxy3);
        parametersList.put(key_parmsFromUserId, parmsFromUserId);
        parametersList.put(key_parmsFromUpn, parmsFromUpn);
        parametersList.put(key_parmsNegative, parmsNegative);
    }

    private static boolean isServerRootUpdatedInUrls = false;

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";
        Log.info(c, thisMethod, "Starting the server and kerberos setup ...");
        spnegoTokencommonSetUp("SpnegoTokenHelperTest", "serverSpnego.xml", SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS,
                               SPNEGOConstants.DONT_CREATE_SSL_CLIENT, SPNEGOConstants.DONT_CREATE_SPN_AND_KEYTAB,
                               SPNEGOConstants.DEFAULT_REALM, SPNEGOConstants.DONT_CREATE_SPNEGO_TOKEN, SPNEGOConstants.DONT_SET_AS_COMMON_TOKEN,
                               SPNEGOConstants.USE_CANONICAL_NAME,
                               SPNEGOConstants.USE_COMMON_KEYTAB, SPNEGOConstants.DONT_START_SERVER,
                               tokenAPIServletName, tokenAPIServletRootContext);
    }

    @Before
    public void beforeTest() {
        if (wasCommonTokenRefreshed) {
            Log.info(c, name.getMethodName(), "Common SPNEGO token was refreshed, so resetting the basic servlet URL parameters");
            Log.info(c, name.getMethodName(), "Resetting URL parameters to use user: " + FATSuite.COMMON_TOKEN_USER);
            for (String paramListKey : parametersList.keySet()) {
                String paramList = parametersList.get(paramListKey);
                paramList = paramList.replaceAll("user[12]", FATSuite.COMMON_TOKEN_USER);
                parametersList.put(paramListKey, paramList);
            }
        }
        if (!isServerRootUpdatedInUrls) {
            Log.info(c, name.getMethodName(), "Updating parameter lists to replace server install root params");
            for (String paramListKey : parametersList.keySet()) {
                String paramList = parametersList.get(paramListKey);
                if (paramList.contains(SERVER_INSTALL_ROOT)) {
                    Log.info(c, name.getMethodName(), "Replacing server root in parameter list       : " + paramList);
                    paramList = paramList.replace(SERVER_INSTALL_ROOT, myServer.getInstallRoot());
                    Log.info(c, name.getMethodName(), "New parameter list after replacing server root: " + paramList);
                    parametersList.put(paramListKey, paramList);
                }
            }
            isServerRootUpdatedInUrls = true;
        }
    }

    /**
     * Test description:
     * - Invoke a servlet that will take the parms passed to create a SPNEGO token via an API
     * - use the token to invoke servlet protected by SPNEGO to validate the token is good
     * Expected results:
     * - We should see success in invocation of servlet using token created by new API.
     */

    @Test
    public void testSpnegoTokenDelegationFromCallerSubjectWithSPNEGOTest() {
        if (InitClass.isRndHostName) {
            Log.info(c, name.getMethodName(), "Not running " + name.getMethodName() + " because randomized hostname is used.");
            return;
        }
        try {
            String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + servletURL;
            String urlDelegation = urlBase + parametersList.get(key_parmsFromCallerSubjectDelegation);
            String urlNonDelegation = urlBase + parametersList.get(key_parmsFromCallerSubjectNonDelegation);

            Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlDelegation);
            int expectedStatusCode = 200;
            boolean ignoreErrorContent = true;

            testHelper.reconfigureServer("serverSpnego.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);

            // Access the servlet using SPNEGO token
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token from test setup");
            String response = myClient.accessWithHeaders(urlDelegation, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2_responseContainsToken(response, responseCheckForCallerSubjectTest);

            String tokenString = extractTokenFromResponse(response);
            Log.info(c, name.getMethodName(), "Token string is: " + tokenString);

            headers = testHelper.setTestHeaders(tokenString, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created using SpnegotokenHelper API");
            response = myClient.accessWithHeaders(urlNonDelegation, expectedStatusCode, headers, ignoreErrorContent);

            myClient.resetClientState();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Invoke a servlet that will take the parms passed to create a SPNEGO token via an API
     * - use the token to invoke servlet protected by SPNEGO to validate the token is good
     * Expected results:
     * - We should see success in invocation of servlet using token created by new API.
     */

    @Test
    public void testSpnegoTokenDelegationFromSubjectWithSPNEGOTest() {
        if (InitClass.isRndHostName) {
            Log.info(c, name.getMethodName(), "Not running " + name.getMethodName() + " because randomized hostname is used.");
            return;
        }
        String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlFromSubject = urlBase + parametersList.get(key_parmsFromSubject);
        String urlNonDelegation = urlBase + parametersList.get(key_parmsFromCallerSubjectNonDelegation);

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlFromSubject);
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            testHelper.reconfigureServer("serverSpnego.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);

            // Access the servlet using SPNEGO token
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            String response = myClient.accessWithHeaders(urlFromSubject, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2_responseContainsToken(response, responseCheckForSubjectTest);

            String tokenString = extractTokenFromResponse(response);
            Log.info(c, name.getMethodName(), "Token string is: " + tokenString);

            headers = testHelper.setTestHeaders(tokenString, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created using SpnegotokenHelper API");
            response = myClient.accessWithHeaders(urlNonDelegation, expectedStatusCode, headers, ignoreErrorContent);

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
     * - Invoke a servlet that will take the parms passed to create a SPNEGO token via an API
     * - use the token to invoke servlet protected by SPNEGO to validate the token is good
     * Expected results:
     * - We should see success in invocation of servlet using token created by new API.
     */

    @Test
    public void testSpnegoTokenDelegationFromUserIdWithSPNEGOTest() {
        if (InitClass.isRndHostName) {
            Log.info(c, name.getMethodName(), "Not running " + name.getMethodName() + " because randomized hostname is used.");
            return;
        }
        String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlUserID = urlBase + parametersList.get(key_parmsFromUserId);
        String urlNonDelegation = urlBase + parametersList.get(key_parmsFromCallerSubjectNonDelegation);

        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlUserID);
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            testHelper.reconfigureServer("serverSpnego.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);

            // Access the servlet using SPNEGO token
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            String response = myClient.accessWithHeaders(urlUserID, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2_responseContainsToken(response, responseCheckForUserIdTest);

            String tokenString = extractTokenFromResponse(response);
            Log.info(c, name.getMethodName(), "Token string is: " + tokenString);

            headers = testHelper.setTestHeaders(tokenString, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created using SpnegotokenHelper API");
            response = myClient.accessWithHeaders(urlNonDelegation, expectedStatusCode, headers, ignoreErrorContent);

            myClient.resetClientState();

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Invoke a servlet that will take the parms passed to create a SPNEGO token via an API
     * - use the token to invoke servlet protected by SPNEGO to validate the token is good
     * Expected results:
     * - We should see success in invocation of servlet using token created by new API.
     */
    //@Test
    public void testSpnegoTokenDelegationFromUpnWithSPNEGOTest() {

        String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlFromUpn = urlBase + parametersList.get(key_parmsFromUpn);
        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlFromUpn);
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            // Access the servlet using SPNEGO token
            testHelper.reconfigureServer("serverSpnego.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            myClient.accessWithHeaders(urlFromUpn, expectedStatusCode, headers, ignoreErrorContent);
            myClient.resetClientState();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);;
        }
    }

    /**
     * Test description:
     * - Invoke a servlet that will take the parms passed to attempt to call Token Helper API, but will fail for bad parms
     * Expected results:
     * - We should verify negative test response to ensure all were successful
     */

    @Test
    @AllowedFFDC({ "java.security.PrivilegedActionException" })
    public void testSpnegoTokenDelegationNegativeSPNEGOTest() {
        if (InitClass.isRndHostName) {
            Log.info(c, name.getMethodName(), "Not running " + name.getMethodName() + " because randomized hostname is used.");
            return;
        }
        String urlBase = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + servletURL;
        String urlNegative = urlBase + parmsNegative;
        Log.info(c, name.getMethodName(), "Accessing servlet with URL of " + urlNegative);
        int expectedStatusCode = 200;
        boolean ignoreErrorContent = true;

        try {
            // Access the servlet using SPNEGO token
            testHelper.reconfigureServer("serverSpnego.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER, SPNEGOConstants.JDK_SPECIFIC_RECONFIG);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGOTokenAPI servlet using token created");
            String response = myClient.accessWithHeaders(urlNegative, expectedStatusCode, headers, ignoreErrorContent);

            expectation.s4u2_validateNegativeResponse(response);
            myClient.resetClientState();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
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

}
