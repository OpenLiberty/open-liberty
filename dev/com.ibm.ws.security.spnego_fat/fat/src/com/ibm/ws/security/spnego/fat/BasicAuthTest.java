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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;

import org.ietf.jgss.GSSException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.Krb5Helper;
import com.ibm.ws.security.spnego.fat.config.MessageConstants;
import com.ibm.ws.security.spnego.fat.config.MsKdcHelper;
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
public class BasicAuthTest extends CommonTest {

    private static final Class<?> c = BasicAuthTest.class;

    private final static String NTLM_TOKEN = "TlRMTVNTUAABAAAAl7II4gcABwAwAAAACAAIACgAAAAGAHIXAAAAD05DMTM1MDE4VElWTEFCMQ==";
    private final static String SPNEGO_NOT_SUPPORTED_CUSTOM_ERROR_PAGE = "My SPNEGO custom error page.";
    private final static String NTLM_TOKEN_RECEIVED_CUSTOM_ERROR_PAGE = "My NTLM custom error page.";

    private static String spnegoTokenExpired;

    @BeforeClass
    public static void setUp() throws Exception {
        Log.info(c, "setUp", "Starting the server and kerberos setup...");
        commonSetUp("BasicAuthTest");
    }

    /**
     * Test description:
     * - Authenticate with Kerberos to obtain a valid subject.
     * - Use the returned subject to create a SPNEGO token.
     * - Access a protected resource by including the SPNEGO token in the request.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     */

    @Test
    public void testSpnegoSuccessful() {
        try {
            testHelper.reconfigureServer("spnegoServer.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            commonSuccessfulSpnegoServletCall();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    @Test
    public void testSpnegoSuccessful_withJwtSsoFeature() {
        try {
            testHelper.reconfigureServer("spnegoServer_withJwtSsoFeature.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS9127W");
            commonSuccessfulSpnegoServletCall();
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Authenticate with Kerberos to obtain a valid subject.
     * - Use the returned subject to create a raw Kerberos token.
     * - Access a protected resource by including the raw Kerberos token in the request.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     */

    @Test
    public void testSpnegoUsingRawKerberosTokenSuccessful() {
        try {
            testHelper.reconfigureServer("spnegoServer.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            String targetSpn = "HTTP/" + TARGET_SERVER;
            String krb5Config = myServer.getServerRoot() + SPNEGOConstants.SERVER_KRB5_CONFIG_FILE;
            Subject subject = krb5Helper.kerberosLogin(myServer, InitClass.FIRST_USER, InitClass.FIRST_USER_PWD, krb5Config);
            String token = krb5Helper.createToken(subject, InitClass.FIRST_USER, targetSpn, true, 5, 6, 7, 8, Krb5Helper.KRB5_MECH_OID);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + token, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using raw Kerberos token");
            successfulSpnegoServletCall(headers, InitClass.FIRST_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Authenticate with Kerberos to obtain a valid subject. Using and SSL Client.
     * - Use the returned subject to create a SPNEGO token.
     * - Access a protected resource by including the SPNEGO token in the request.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     */
    @Test
    public void testSpnegoSuccessfulforSSLClient() {
        try {
            testHelper.reconfigureServer("spnegoServer.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            commonSuccessfulSpnegoServletCallSSLClient();
        } catch (Exception ex) {
            Log.info(c, name.getMethodName(), "Unexpected exception: " + CommonTest.maskHostnameAndPassword(ex.getMessage()));
            fail("Exception was thrown, Check error");
        }
    }

    /**
     * Test description:
     * - Access a protected resource by including a null SPNEGO token in the request.
     *
     * Expected results:
     * - Authentication should fail, resulting in a 401.
     */

    @Test
    public void testSpnegoTokenNull() {
        try {
            testHelper.reconfigureServer("spnegoServer.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + null, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            unsuccessfulSpnegoServletCall(headers);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Access a protected resource by including an incomplete SPNEGO token in the request.
     *
     * Expected results:
     * - Authentication should fail, resulting in a 401.
     */

    @Test
    public void testInCompleteSpnegoToken() {
        try {
            testHelper.reconfigureServer("spnegoServer.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            String badSpnegoToken = "YIIKzgYGKwYBBQUT";
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + badSpnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            unsuccessfulSpnegoServletCall(headers);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Access a protected resource by including an incomplete raw Kerberos token in the request.
     *
     * Expected results:
     * - Authentication should fail, resulting in a 401.
     */

    @Test
    public void testInCompleteRawKerberosToken() {
        try {
            testHelper.reconfigureServer("spnegoServer.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            String badSpnegoToken = "YIIEpAYJKoZIhvcSAQICAQBuggSTMIIEj6ADAgEFoQMCAQ6iBwMFACAAAACjggEbYYIBFzCCAROgAwIBBaEYGxZUSVZMQUIxLk";
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + badSpnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            unsuccessfulSpnegoServletCall(headers);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Authenticate one user with Kerberos using RC4 encryption to obtain a subject.
     * - Use the returned subject for the first user and the user name for a second user to create a SPNEGO token.
     * - Access a protected resource by including the SPNEGO token in the request.
     *
     * Expected results:
     * - Authentication should fail, resulting in a 401.
     */

    @Test
    public void testSpnego_MismatchedUsers() {
        try {
            String userName = InitClass.FIRST_USER;
            String targetSpn = "HTTP/" + TARGET_SERVER;
            String krb5Config = myServer.getServerRoot() + SPNEGOConstants.SERVER_KRB5_CONFIG_FILE;
            String misMatchUserName = InitClass.SECOND_USER;
            String misMatchUserPassword = InitClass.SECOND_USER_PWD;

            testHelper.reconfigureServer("spnegoServer.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            Subject subject = krb5Helper.kerberosLogin(myServer, misMatchUserName, misMatchUserPassword, krb5Config);
            String spnegoToken = krb5Helper.createToken(subject, userName, targetSpn, false, Krb5Helper.SPNEGO_MECH_OID);
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + spnegoToken, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            unsuccessfulSpnegoServletCall(headers);
            fail("Should have thrown a GSSException but did not.");
        } catch (GSSException e) {
            // This is expected
            Log.info(c, name.getMethodName(), "Expected exception: " + CommonTest.maskHostnameAndPassword(e.getMessage()));
            expectation.spnegoInvalidCredential(e);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO.
     * - Create the header without the SPNEGO token.
     * - Access a protected resource by including the header created in the request.
     * - The response is checked to make sure it contains the default error page.
     *
     * Expected results:
     * - Authentication should fail, and the default error page for "SPNEGO not supported" should be returned.
     */

    @Test
    public void testSpnegoNotSupportedDefaultErrorPage() {
        try {
            testHelper.reconfigureServer("defaultErrorPage.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headerNotSpnegoToken = testHelper.setTestHeaders(null, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headerNotSpnegoToken, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);
            expectation.spnegoNotSupported(response);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Create the header with the SPNEGO token.
     * - Update server keytab with an invalid server hostName
     * - Specify "invalidUrl" for spnegoAuthenticationErrorPageURL in server.xml
     * - Access a protected resource by including the header created in the request.
     * - The response is checked to make sure it contains the error page.
     * - Update server keytab with an correct server hostName
     *
     * Expected results:
     * - 401 authorization failure, and the error page "SPNEGO authentication failed. Contact your system administrator to resolve the problem." should be returned.
     */

    @Test
    @AllowedFFDC({ "org.ietf.jgss.GSSException", "java.net.MalformedURLException" })
    public void testSpnegoAuthenticationFailed_DefaultErrorPage() {
        try {
            Map<String, String> validSpnegoTokenHeaders = createCommonHeaders();

            MsKdcHelper testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);
            testKdcHelper.customSpnName = "wrongHostName";
            testKdcHelper.createSpnAndKeytab(SPNEGOConstants.USE_CANONICAL_NAME);

            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("spnegoAuthFailed_customFileErrorPageBadUrl.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4317E", "CWWKS4308E", "CWWKS4309E");

            String response = myClient.accessProtectedServletWithInvalidHeaders(SPNEGOConstants.SIMPLE_SERVLET, validSpnegoTokenHeaders, false, 401);

            Log.info(c, name.getMethodName(), "Checking Response for Message: " + MessageConstants.SPNEGO_AUTHENTICATION_ERROR_CWWKS4323E);
            assertTrue("The response did not contain: " + MessageConstants.SPNEGO_AUTHENTICATION_ERROR_CWWKS4323E,
                       response.contains(MessageConstants.SPNEGO_AUTHENTICATION_ERROR_CWWKS4323E));

            myClient.resetClientState();

            testKdcHelper.customSpnName = null;
            testKdcHelper.createSpnAndKeytab(SPNEGOConstants.USE_CANONICAL_NAME);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Create the header with the SPNEGO token.
     * - Update server keytab with an invalid server hostName
     * - Access a protected resource by including the header created in the request.
     * - The response is checked to make sure it contains the error page.
     * - Update server keytab with an correct server hostName
     *
     * Expected results:
     * - 401 authorization failure, and the error page "My SPNEGO custom error page." should be returned.
     */

    @Test
    @AllowedFFDC({ "org.ietf.jgss.GSSException" })
    public void testSpnegoAuthenticationFailed_CustomErrorPage() {
        try {
            Map<String, String> validSpnegoTokenHeaders = createCommonHeaders();

            MsKdcHelper testKdcHelper = new MsKdcHelper(myServer, InitClass.KDC_USER, InitClass.KDC_USER_PWD, InitClass.KDC_REALM);
            testKdcHelper.customSpnName = "wrongHostName";
            testKdcHelper.createSpnAndKeytab(SPNEGOConstants.USE_CANONICAL_NAME);

            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS0008I");
            testHelper.reconfigureServer("spnegoAuthFailed_customFileErrorPage.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4308E", "CWWKS4309E");

            String response = myClient.accessProtectedServletWithInvalidHeaders(SPNEGOConstants.SIMPLE_SERVLET, validSpnegoTokenHeaders, false, 401);
            Log.info(c, name.getMethodName(), "Checking Response for Message: " + SPNEGO_NOT_SUPPORTED_CUSTOM_ERROR_PAGE);
            assertTrue("The response did not contain: " + SPNEGO_NOT_SUPPORTED_CUSTOM_ERROR_PAGE + "\nResponse: " + response,
                       response.contains(SPNEGO_NOT_SUPPORTED_CUSTOM_ERROR_PAGE));

            myClient.resetClientState();

            testKdcHelper.customSpnName = null;
            testKdcHelper.createSpnAndKeytab(SPNEGOConstants.USE_CANONICAL_NAME);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the spnegoNotSupportedErrorPageURL attribute with a file.
     * - Create the header without the SPNEGO token.
     * - Access a protected resource by including the header created in the request.
     * - The response is checked to make sure it contains the defined error page.
     *
     * Expected results:
     * - Authentication should fail, and the defined error page for "SPNEGO not supported" should be returned.
     */

    @Test
    public void testSpnegoNotSupportedFileErrorPage() {
        try {
            testHelper.reconfigureServer("customFileErrorPage.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headerNotSpnegoToken = testHelper.setTestHeaders(null, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headerNotSpnegoToken, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

//            assertTrue("Expected to receive the defined error message for 'SPNEGO not supported' but it was not received.",
//                       response.contains(SPNEGO_NOT_SUPPORTED_CUSTOM_ERROR_PAGE));
            expectation.spnegoNotSupportedCustomErrorPage(response, SPNEGO_NOT_SUPPORTED_CUSTOM_ERROR_PAGE);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the spnegoNotSupportedErrorPageURL attribute with a web resource.
     * - Create the header without the SPNEGO token.
     * - Access a protected resource by including the header created in the request.
     * - The response is checked to make sure it contains the defined error page.
     *
     * Expected results:
     * - Authentication should fail, and the defined error page for "SPNEGO not supported" should be returned.
     */

    @Test
    public void testSpnegoNotSupportedWebErrorPage() {
        try {
            testHelper.reconfigureServer("customWebErrorPage.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4318E");

            Map<String, String> headerNotSpnegoToken = testHelper.setTestHeaders(null, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headerNotSpnegoToken, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.spnegoNotSupportedCustomErrorPage(response, SPNEGO_NOT_SUPPORTED_CUSTOM_ERROR_PAGE);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the spnegoNotSupportedErrorPageURL attribute with a file that doesn't exist.
     * - Create the header without the SPNEGO token.
     * - Access a protected resource by including the header created in the request.
     * - The response is checked to make sure it contains the default error page.
     * - The messages.log file is checked to make sure it contains the message for "SPNEGO load custom error page error".
     *
     * Expected results:
     * - Authentication should fail and the default error page for "SPNEGO not supported" should be returned.
     * - A FileNotFoundException must be thrown.
     * - The message for "SPNEGO load custom error page error" must be included in the messages.log file.
     */

    @Test
    @AllowedFFDC({ "java.io.FileNotFoundException" })
    public void testSpnegoNotSupportedFileErrorPageWithBadUrl() {
        try {
            testHelper.reconfigureServer("customFileErrorPageBadUrl.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4318E");

            Map<String, String> headerNotSpnegoToken = testHelper.setTestHeaders(null, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headerNotSpnegoToken, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.spnegoNotSupported(response);

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_LOAD_CUSTOM_ERROR_PAGE_CWWKS4318E);
            testHelper.waitForMessages(checkMsgs, true);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the spnegoNotSupportedErrorPageURL attribute with a web resource that doesn't exist.
     * - Create the header without the SPNEGO token.
     * - Access a protected resource by including the header created in the request.
     * - The response is checked to make sure it contains the default error page.
     * - The messages.log file is checked to make sure it contains the message for "SPNEGO load custom error page error".
     *
     * Expected results:
     * - Authentication should fail and the default error page for "SPNEGO not supported" should be returned.
     * - A FileNotFoundException must be thrown.
     * - The message for "SPNEGO load custom error page error" must be included in the messages.log file.
     */

    @Test
    @AllowedFFDC({ "java.io.FileNotFoundException" })
    public void testSpnegoNotSupportedWebErrorPageWithBadUrl() {
        try {
            testHelper.reconfigureServer("customWebErrorPageBadUrl.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4318E");

            Map<String, String> headerNotSpnegoToken = testHelper.setTestHeaders(null, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headerNotSpnegoToken, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.spnegoNotSupported(response);

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_LOAD_CUSTOM_ERROR_PAGE_CWWKS4318E);
            testHelper.waitForMessages(checkMsgs, true);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the spnegoNotSupportedErrorPageURL attribute with a malformed URL.
     * - Create the header without the SPNEGO token.
     * - Access a protected resource by including the header created in the request.
     * - The response is checked to make sure it contains the default error page.
     * - The messages.log file is checked to make sure it contains the message for "SPNEGO custom error page malformed URL".
     *
     * Expected results:
     * - Authentication should fail and the default error page for "SPNEGO not supported" should be returned.
     * - A MalformedURLException must be thrown.
     * - The message for "SPNEGO custom error page malformed URL" must be included in the messages.log file.
     */

    @Test
    @AllowedFFDC({ "java.net.MalformedURLException" })
    public void testSpnegoNotSupportedErrorPageWithMalformedUrl() {
        try {
            testHelper.reconfigureServer("customErrorPageWithMalformedUrl.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4317E", "CWWKS4318E");

            Map<String, String> headerNotSpnegoToken = testHelper.setTestHeaders(null, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headerNotSpnegoToken, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.spnegoNotSupported(response);

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.MALFORMED_CUSTOM_ERROR_PAGE_CWWKS4317E);
            testHelper.waitForMessages(checkMsgs, true);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the spnegoNotSupportedErrorPageURL attribute with a file.
     * - On the new error page 'content type' and 'page encoding' are specified.
     * - Create the header without the SPNEGO token.
     * - Access a protected resource by including the header created in the request.
     * - The response is checked to make sure it contains the correct content type 'text/plain'.
     * - The response is checked to make sure it contains the correct page encoding 'US-ASCII'.
     * - The response is checked to make sure it contains the defined error page.
     *
     * Expected results:
     * - Authentication should fail, and the defined error message for "SPNEGO not supported" should be returned.
     * - Content type and page encoding must be specified on the defined error page.
     */

    @Test
    public void testSpnegoNotSupportedErrorPageWithContentTypeAndPageEncoding() {
        try {
            testHelper.reconfigureServer("customErrorPageWithContTypeAndEncode.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headerNotSpnegoToken = testHelper.setTestHeaders(null, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headerNotSpnegoToken, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.spnegoNotSupportedContentCustomErrorPage(response, true);

            //TODO MERGE
            expectation.spnegoNotSupportedContentCustomErrorPage(response, true);
            expectation.spnegoNotSupportedCustomErrorPage(response, SPNEGO_NOT_SUPPORTED_CUSTOM_ERROR_PAGE);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the spnegoNotSupportedErrorPageURL attribute with a file.
     * - On the new error page a non supported content type is specified.
     * - Create the header without the SPNEGO token.
     * - Access a protected resource by including the header created in the request.
     * - The response is checked to make sure it contains the default error page.
     * - The messages.log file is checked to make sure it contains the message for "SPNEGO custom error page content type error".
     *
     * Expected results:
     * - Authentication should fail, and the default error message for "SPNEGO not supported" should be returned.
     * - The message for "SPNEGO custom error page content type error" must be included in the messages.log file.
     */
    //This test is commented out because the content type "text/html" is being set up despite of what content type
    //is specified on the custom error page, therefore the test is always failing.
    //@Test
    public void testSpnegoNotSupportedErrorPageWithBadContentType() {
        try {
            testHelper.reconfigureServer("customErrorPageWithBadContType.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headerNotSpnegoToken = testHelper.setTestHeaders(null, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headerNotSpnegoToken, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.spnegoNotSupported(response);

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_GET_CONTENT_CUSTOM_ERROR_PAGE_CWWKS4319E);
            testHelper.waitForMessages(checkMsgs, true);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO.
     * - Create a new NTLM token.
     * - Access a protected resource by including the NTLM token in the request.
     * - The response is checked to make sure it contains the default error page.
     *
     * Expected results:
     * - Authentication should fail, and the default error page for "NTLM token received" should be returned.
     */

    @Test
    public void testNtlmTokenReceivedDefaultErrorPage() {
        try {
            testHelper.reconfigureServer("defaultErrorPage.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + NTLM_TOKEN, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.ntlmTokenReceivedErrorCode(response);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the ntlmTokenReceivedErrorPageURL attribute with a file.
     * - Create a new NTLM token.
     * - Access a protected resource by including the NTLM token in the request.
     * - The response is checked to make sure it contains the defined error page.
     *
     * Expected results:
     * - Authentication should fail, and the defined error page for "NTLM token received" should be returned.
     */

    @Test
    public void testNtlmTokenReceivedFileErrorPage() {
        try {
            testHelper.reconfigureServer("customFileErrorPage.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + NTLM_TOKEN, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.ntlmtokenReceivedCustomErrorPage(response, NTLM_TOKEN_RECEIVED_CUSTOM_ERROR_PAGE);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the ntlmTokenReceivedErrorPageURL attribute with a web resource.
     * - Create a new NTLM token.
     * - Access a protected resource by including the NTLM token in the request.
     * - The response is checked to make sure it contains the defined error page.
     *
     * Expected results:
     * - Authentication should fail, and the defined error page for "NTLM token received" should be returned.
     */

    @Test
    public void testNtlmTokenReceivedWebErrorPage() {
        try {
            testHelper.reconfigureServer("customWebErrorPage.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4318E");

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + NTLM_TOKEN, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.ntlmtokenReceivedCustomErrorPage(response, NTLM_TOKEN_RECEIVED_CUSTOM_ERROR_PAGE);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the ntlmTokenReceivedErrorPageURL attribute with a file that doesn't exist.
     * - Create a new NTLM token.
     * - Access a protected resource by including the NTLM token in the request.
     * - The response is checked to make sure it contains the default error page.
     * - The messages.log file is checked to make sure it contains the message for "SPNEGO load custom error page error".
     *
     * Expected results:
     * - Authentication should fail and the default error page for "NTLM token received" should be returned.
     * - A FileNotFoundException must be thrown.
     * - The message for "SPNEGO load custom error page error" must be included in the messages.log file.
     */

    @Test
    @AllowedFFDC({ "java.io.FileNotFoundException" })
    public void testNtlmTokenReceivedFileErrorPageWithBadUrl() {
        try {
            testHelper.reconfigureServer("customFileErrorPageBadUrl.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4318E");

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + NTLM_TOKEN, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.ntlmTokenReceivedErrorCode(response);

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_LOAD_CUSTOM_ERROR_PAGE_CWWKS4318E);
            testHelper.waitForMessages(checkMsgs, true);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the ntlmTokenReceivedErrorPageURL attribute with a web resource that doesn't exist.
     * - Create a new NTLM token.
     * - Access a protected resource by including the NTLM token in the request.
     * - The response is checked to make sure it contains the default error page.
     * - The messages.log file is checked to make sure it contains the message for "SPNEGO load custom error page error".
     *
     * Expected results:
     * - Authentication should fail and the default error page for "NTLM token received" should be returned.
     * - A FileNotFoundException must be thrown.
     * - The message for "SPNEGO load custom error page error" must be included in the messages.log file.
     */

    @Test
    @AllowedFFDC({ "java.io.FileNotFoundException" })
    public void testNtlmTokenReceivedWebErrorPageWithBadUrl() {
        try {
            testHelper.reconfigureServer("customWebErrorPageBadUrl.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4318E");

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + NTLM_TOKEN, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.ntlmTokenReceivedErrorCode(response);

            testHelper.checkForMessages(true, MessageConstants.CANNOT_LOAD_CUSTOM_ERROR_PAGE_CWWKS4318E);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the ntlmTokenReceivedErrorPageURL attribute with a malformed URL.
     * - Create a new NTLM token.
     * - Access a protected resource by including the NTLM token in the request.
     * - The response is checked to make sure it contains the default error page.
     * - The messages.log file is checked to make sure it contains the message for "SPNEGO custom error page malformed URL".
     *
     * Expected results:
     * - Authentication should fail and the default error page for "NTLM token received" should be returned.
     * - A MalformedURLException must be thrown.
     * - The message for "SPNEGO custom error page malformed URL" must be included in the messages.log file.
     */

    @Test
    @AllowedFFDC({ "java.net.MalformedURLException" })
    public void testNtlmTokenReceivedErrorPageWithMalformedUrl() {
        try {
            testHelper.reconfigureServer("customErrorPageWithMalformedUrl.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4317E");

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + NTLM_TOKEN, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

//            assertTrue("Expected to receive the default error message for 'NTLM token received' but it was not received.",
//                       response.contains(MessageConstants.NTLM_TOKEN_RECEIVED_CWWKS4307E));
            expectation.ntlmTokenReceivedErrorCode(response);

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.MALFORMED_CUSTOM_ERROR_PAGE_CWWKS4317E);
            testHelper.waitForMessages(checkMsgs, true);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the ntlmTokenReceivedErrorPageURL attribute with a file.
     * - On the new error page 'content type' and 'page encoding' are specified.
     * - Create a new NTLM token.
     * - Access a protected resource by including the NTLM token in the request.
     * - The response is checked to make sure it contains the correct content type 'text/example'.
     * - The response is checked to make sure it contains the correct page encoding 'ISO-8859-1'.
     * - The response is checked to make sure it contains the defined error page.
     *
     * Expected results:
     * - Authentication should fail, and the defined error page for "NTLM token received" should be returned.
     * - Content type and page encoding must be specified on the defined error page.
     */

    @Test
    public void testNtlmTokenReceivedErrorPageWithContentTypeAndPageEncoding() {
        try {
            testHelper.reconfigureServer("customErrorPageWithContTypeAndEncode.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + NTLM_TOKEN, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            //TODO MERGE
            expectation.ntlmTokenContentCustomErrorPage(response);
            expectation.ntlmtokenReceivedCustomErrorPage(response, NTLM_TOKEN_RECEIVED_CUSTOM_ERROR_PAGE);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO and set the value of the ntlmTokenReceivedErrorPageURL attribute with a file.
     * - On the new error page a non supported content type is specified.
     * - Create a new NTLM token.
     * - Access a protected resource by including the NTLM token in the request.
     * - The response is checked to make sure it contains the default error page.
     * - The messages.log file is checked to make sure it contains the message for "SPNEGO custom error page content type error".
     *
     * Expected results:
     * - Authentication should fail, and the defined error page for "NTLM token received" should be returned.
     * - The message for "SPNEGO custom error page content type error" must be included in the messages.log file.
     */
    //This test is commented out because the content type "text/html" is being set up despite of what content type
    //is specified on the custom error page, therefore the test is always failing.
    //@Test
    public void testNtlmTokenReceivedErrorPageWithBadContentType() {
        try {
            testHelper.reconfigureServer("customErrorPageWithBadContType.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + NTLM_TOKEN, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            String response = unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);

            expectation.ntlmTokenReceivedErrorCode(response);

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_GET_CONTENT_CUSTOM_ERROR_PAGE_CWWKS4319E);
            testHelper.waitForMessages(checkMsgs, true);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have an invalid value for skipForUnprotectedURI.
     * - Restart the server and check log results for validation of configuration.
     *
     * Expected results:
     * - Server should start, but should have a validation message for skipForUnprotectedURI and show it has default value.
     */

    @Test
    public void testInvalidSkipForUnprotectedURIValue() {
        try {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKG0083W");
            testHelper.reconfigureServer("invalidSkipForUnprotectedURIValue.xml", name.getMethodName(), startMsgs, SPNEGOConstants.RESTART_SERVER);
            testHelper.setShutdownMessages("CWWKS4318E", "CWWKG0083W");
            // check message log for error message will be done in reconfigureServer call
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * canonicalHostName=false.
     * targetSpn = HTTP/<canonicalHostName>.
     * keytab have HTTP/<canonicalHostName>.
     * launch the request SimpleServlet with localhost.
     *
     * Expected results:
     * - Server should start, and should fail to access SPNEGO protected resource with long hostname and
     * canonicalHostName set false.
     */

    //@AllowedFFDC({ "org.ietf.jgss.GSSException", "com.ibm.ws.security.authentication.AuthenticationException" })
    //@Test
    public void testcanonicalHostName_False_withLongName() {
        try {
            String shortHostName = java.net.InetAddress.getLocalHost().getHostName();
            if (shortHostName.contains(SPNEGOConstants.IBM_DOMAIN)) {
                shortHostName = shortHostName.substring(0, shortHostName.indexOf("."));
            }
            // Add bootstrap properties
            Map<String, String> bootstrapProps = new HashMap<String, String>();
            bootstrapProps.put(SPNEGOConstants.PROP_TEST_SYSTEM_SHORTHOST_NAME, shortHostName);
            testHelper.addBootstrapProperties(myServer, bootstrapProps);
            testHelper.reconfigureServer("canonicalHostNameFalse.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            // check message log for error message will be done in reconfigureServer call
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.FIREFOX, shortHostName, null);
            unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT, 403);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have a true value for canoncialHostName.
     * - Restart the server.
     *
     * Expected results:
     * - Server should start, and should succeed to access SPNEGO protected resource with long hostname and
     * canonicalHostName set true.
     */

    @Test
    public void testcanonicalHostName_True_withLongName() {
        try {
            testHelper.reconfigureServer("canonicalHostNameTrue.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            // check message log for error message will be done in reconfigureServer call
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            successfulSpnegoServletCall(headers);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Configure the server to enable JAAS login to map user1 for user2.
     * - Create a SPNEGO token using user1.
     * - Performs a call to the SPNEGO servlet that is expected to contain user2.
     * - The response is checked to make sure it contains user2 as well as the appropriate security roles, in this case "Manager" role.
     * - The response is checked for the presence of GSS credentials for user1.
     *
     * Expected results:
     * - Authentication should be successful and access to the protected resource should be granted.
     * - User1 must be replaced for User2 therefore, User2 must be included on the response.
     *
     */

    @Test
    public void testTrimKerberosRealmNameFromPrincipalUsingCustomJaasLogin() {
        try {
            testHelper.reconfigureServer("trimKerberosRealmFromPrincipal_customJaasLogin.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            successfulSpnegoServletCallForMappedUser(InitClass.FIRST_USER, InitClass.FIRST_USER_PWD, InitClass.SECOND_USER, SPNEGOConstants.IS_NOT_EMPLOYEE,
                                                     SPNEGOConstants.IS_MANAGER);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description: Checking that SPNEGO complies with specification as to sending back Negotiate in header.
     * - Update the server configuration to enable SPNEGO.
     * - Create the header with all null values.
     * - Access a protected resource.
     *
     * Expected results:
     * - Authentication should fail, and the response header should have Negotiate in WWW-Authenicate
     * - and we should verify that the responseCode is 401.
     */

    @Test
    public void testSpnegoNegotiateReturning401() {
        try {
            testHelper.reconfigureServer("spnegoServer.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            String url = "http://" + myServer.getHostname() + ":" + myServer.getHttpDefaultPort() + "/basicauth" + SPNEGOConstants.SIMPLE_SERVLET;
            Map<String, String> headers = null;
            String response = myClient.accessWithHeaders(url, 401, headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT, SPNEGOConstants.HANDLE_SSO_COOKIE);
            Log.info(c, name.getMethodName(), "Servlet response: " + response);

            expectation.spnegoNotSupported(response);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Update the server configuration to enable SPNEGO.
     * - Create a SPNEGO token using user1.
     * - Sleep the test by 61 minutes, doing this the SPNEGO token will expire since its default lifetime is 60 minutes.
     * - Access a protected resource by including the SPNEGO token expired in the request.
     * - The messages.log file is checked to make sure it contains the message for "SPNEGO can not validate token".
     * - The messages.log file is checked to make sure it contains the message "Defective token".
     *
     * NOTE: It is necessary to turn on the jgss and krb5 debugging to see the Defective token message.
     *
     * Expected results:
     * - Authentication should fail, resulting in a 403.
     * - The message for "SPNEGO can not validate token" and "Defective token"must be included in the messages.log file.
     */
    //This test must be run only locally, because it will take a lot of time to run it in Personal Builds.
    //@Test
    public void testSpnegoTokenExpired() throws Exception {
        try {
            testHelper.reconfigureServer("spnegoServer.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            spnegoTokenExpired = testHelper.createSpnegoToken(InitClass.FIRST_USER, InitClass.FIRST_USER_PWD, TARGET_SERVER, SPNEGOConstants.SERVER_KRB5_CONFIG_FILE, krb5Helper);

            Thread.sleep(3660000);

            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + spnegoTokenExpired, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT, 403);

            List<String> checkMsgs = new ArrayList<String>();
            checkMsgs.add(MessageConstants.CANNOT_VALIDATE_SPNEGO_TOKEN_CWWKS4320E);
            checkMsgs.add("Defective token");
            testHelper.waitForMessages(checkMsgs, true);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Attempt to create an SPN and keytab file using an invalid KDC host name.
     *
     * Expected results:
     * - The connection to the invalid host should fail, resulting in an exception.
     * - The exception message should contain "Unable to make remote connection with specified credentials."
     */

    @Test
    public void testInaccessibleKdc() {
        try {
            String invalidKdc = "ncINVALID.austin.ibm.com";
            Log.info(c, name.getMethodName(), "Attempting to create SPN and keytab with invalid KDC: " + invalidKdc);

            MsKdcHelper invalidKdcHelper = new MsKdcHelper(myServer, invalidKdc, "user", "password", "realm");
            invalidKdcHelper.createSpnAndKeytab(SPNEGOConstants.USE_CANONICAL_NAME);

            fail("Should have thrown an exception but did not.");
        } catch (Exception e) {
            // This is expected
            Log.info(c, name.getMethodName(), "Received expected exception");

            expectation.KDCErr_InaccessibleKDC(e);
        }
    }

    /**
     * Test description:
     * - Set server xml file to have a true value for allowLocalHost.
     * - Restart the server.
     *
     * Expected results:
     * - Server should start, and should succeed to access SPNEGO protected resource with localHost as SPN.
     */

    @Test
    public void testlocalHostforSPN() {
        try {
            testHelper.reconfigureServer("localHostForSPN.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);
            // check message log for error message will be done in reconfigureServer call
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + getCommonSPNEGOToken(), SPNEGOConstants.FIREFOX, "localhost", null);
            successfulSpnegoServletCall(headers);
        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

}
