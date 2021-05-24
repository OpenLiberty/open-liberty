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

import javax.security.auth.Subject;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.spnego.fat.config.CommonTest;
import com.ibm.ws.security.spnego.fat.config.InitClass;
import com.ibm.ws.security.spnego.fat.config.Krb5Helper;
import com.ibm.ws.security.spnego.fat.config.SPNEGOConstants;
import com.ibm.ws.webcontainer.security.test.servlets.BasicAuthClient;
import com.ibm.ws.webcontainer.security.test.servlets.ServletClientImpl;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
//@Mode(TestMode.FULL)
@Mode(TestMode.QUARANTINE)
public class InvokeAfterSSOTest extends CommonTest {

    private static final Class<?> c = InvokeAfterSSOTest.class;
    private static final String SSO_SERVLET_NAME = "AllRoleServlet";

    private final String SSO_COOKIE_NAME = "LtpaToken2";
    private final String SSO_SERVLET = "/" + SSO_SERVLET_NAME;

    @BeforeClass
    public static void setUp() throws Exception {
        String thisMethod = "setUp";
        Log.info(c, thisMethod, "Setting up...");
        commonSetUp("InvokeAfterSSOTest", null, SPNEGOConstants.NO_APPS, SPNEGOConstants.NO_PROPS, SPNEGOConstants.DONT_START_SERVER);
    }

    /**
     * Test description:
     * - invokeAfterSSO attribute is set to true in server.xml.
     * - Access a protected resource to obtain an SSO cookie for a user0.
     * - Access the SPNEGO servlet by including the SSO cookie for user0 and a valid SPNEGO token header for
     * a user1 in the request.
     *
     * Expected results:
     * - An SSO cookie for user0 should be received from accessing the first protected resource.
     * - Access to the SPNEGO servlet should be granted based on the SSO cookie for user0.
     * - Client subject should match user0.
     * - Client subject should not contain GSS credentials.
     * - Response should contain the expected SSO cookie value.
     */

    @Test
    public void testInvokeAfterSSO_True_ValidSSOCookie_ValidSpnegoToken() {
        try {
            testHelper.reconfigureServer("invokeAfterSso_true.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Obtain an SSO cookie by accessing another servlet
            Log.info(c, name.getMethodName(), "Accessing first servlet in order to obtain SSO cookie");
            String ssoCookie = getAndAssertSSOCookieForUser(InitClass.Z_USER, InitClass.Z_USER_PWD, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

            // Add the SSO cookie as a header, in addition to the SPNEGO token, and submit the request
            Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using valid SSO cookie and valid SPNEGO token headers");
            String response = successfulServletCall(SPNEGOConstants.SIMPLE_SERVLET, headers, InitClass.Z_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

            expectation.unsuccesfulSpnegoServletCall(response, SPNEGOConstants.OWNER_STRING);
            expectation.responseContainsSSOCookie(response, SSO_COOKIE_NAME, ssoCookie);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - invokeAfterSSO attribute is set to false in server.xml.
     * - Access a protected resource to obtain an SSO cookie for user0.
     * - Access the SPNEGO servlet by including the SSO cookie for user0 and a valid SPNEGO token header for
     * the user used to create the common SPNEGO token in the request.
     *
     * Expected results:
     * - An SSO cookie for user0 should be received from accessing the first protected resource.
     * - Access to the SPNEGO servlet should be granted based on the valid SPNEGO credentials for the user used to create the common
     * SPNEGO token.
     * - Client subject should contain GSS credentials matching the user used to obtain the common SPNEGO token.
     * - Response should also contain the expected SSO cookie value.
     */

    @Test
    public void testInvokeAfterSSO_False_ValidSSOCookie_ValidSpnegoToken() {
        try {
            testHelper.reconfigureServer("invokeAfterSso_false.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Obtain an SSO cookie by accessing another servlet
            Log.info(c, name.getMethodName(), "Accessing first servlet in order to obtain SSO cookie");
            String ssoCookie = getAndAssertSSOCookieForUser(InitClass.Z_USER, InitClass.Z_USER_PWD, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

            // Add the SSO cookie as a header, in addition to the SPNEGO token, and submit the request
            Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using valid SSO cookie and valid SPNEGO token headers");
            String response = successfulSpnegoServletCall(headers, FATSuite.COMMON_TOKEN_USER,
                                                          FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE, FATSuite.COMMON_TOKEN_USER_IS_MANAGER);

            expectation.responseContainsSSOCookie(response, SSO_COOKIE_NAME, ssoCookie);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - invokeAfterSSO attribute is set to true in server.xml.
     * - Access a protected resource to obtain an SSO cookie for user0.
     * - Access the SPNEGO servlet by including the SSO cookie for user0 and an invalid SPNEGO token header in
     * the request.
     *
     * Expected results:
     * - An SSO cookie for user0 should be received from accessing the first protected resource.
     * - Access to the SPNEGO servlet should be granted based on the SSO cookie for user0.
     * - Client subject should match user0.
     * - Client subject should not contain GSS credentials.
     * - Response should contain the expected SSO cookie value matching the SSO cookie for user0.
     */

    @Test
    public void testInvokeAfterSSO_True_ValidSSOCookie_InvalidSpnegoToken() {
        try {
            testHelper.reconfigureServer("invokeAfterSso_true.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Obtain an SSO cookie by accessing another servlet
            Log.info(c, name.getMethodName(), "Accessing first servlet in order to obtain SSO cookie");
            String ssoCookie = getAndAssertSSOCookieForUser(InitClass.Z_USER, InitClass.Z_USER_PWD, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

            // Add the SSO cookie as a header, in addition to the invalid SPNEGO token, and submit the request
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate: INVALID", SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            headers.put("Cookie", SSO_COOKIE_NAME + "=" + ssoCookie);

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using valid SSO cookie and invalid SPNEGO token headers");
            String response = successfulServletCall(SPNEGOConstants.SIMPLE_SERVLET, headers, InitClass.Z_USER, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

            expectation.unsuccesfulSpnegoServletCall(response, SPNEGOConstants.OWNER_STRING);
            expectation.responseContainsSSOCookie(response, SSO_COOKIE_NAME, ssoCookie);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - invokeAfterSSO attribute is set to false in server.xml.
     * - Access a protected resource to obtain an SSO cookie for user0.
     * - Access the SPNEGO servlet by including the SSO cookie for user0 and an invalid SPNEGO token header in
     * the request.
     *
     * Expected results:
     * - An SSO cookie for user0 should be received from accessing the first protected resource.
     * - Authentication will fail due to the invalid SPNEGO token, resulting in a 401.
     */

    @Test
    public void testInvokeAfterSSO_False_ValidSSOCookie_InvalidSpnegoToken() {
        try {
            testHelper.reconfigureServer("invokeAfterSso_false.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Obtain an SSO cookie by accessing another servlet
            Log.info(c, name.getMethodName(), "Accessing first servlet in order to obtain SSO cookie");
            String ssoCookie = getAndAssertSSOCookieForUser(InitClass.Z_USER, InitClass.Z_USER_PWD, SPNEGOConstants.IS_EMPLOYEE, SPNEGOConstants.IS_NOT_MANAGER);

            // Add the SSO cookie as a header, in addition to the invalid SPNEGO token, and submit the request
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate: INVALID", SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            headers.put("Cookie", SSO_COOKIE_NAME + "=" + ssoCookie);

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using valid SSO cookie and invalid SPNEGO token headers");
            unsuccessfulSpnegoServletCall(headers);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - invokeAfterSSO attribute is set to true in server.xml.
     * - Access the SPNEGO servlet by including an invalid SSO cookie and a valid SPNEGO token header in the request.
     *
     * Expected results:
     * - SSO should fail when using the invalid SSO cookie (nothing reported through messages).
     * - Access to the SPNEGO servlet should be granted based on the valid SPNEGO credentials for the common token user.
     * - Client subject should contain GSS credentials matching the user used to obtain the SPNEGO token.
     * - Response should contain a null SSO cookie value.
     */

    @Test
    public void testInvokeAfterSSO_True_InvalidSSOCookie_ValidSpnegoToken() {
        try {
            testHelper.reconfigureServer("invokeAfterSso_true.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Add the invalid SSO cookie as a header, in addition to the valid SPNEGO token, and submit the request
            String ssoCookie = "SSO_INVALID";
            Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using invalid SSO cookie and valid SPNEGO token headers");
            String response = successfulSpnegoServletCall(headers, FATSuite.COMMON_TOKEN_USER,
                                                          FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE, FATSuite.COMMON_TOKEN_USER_IS_MANAGER);

            expectation.ssoCookieIsNull(response, SSO_COOKIE_NAME, ssoCookie);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - invokeAfterSSO attribute is set to false in server.xml.
     * - Access the SPNEGO servlet by including an invalid SSO cookie and a valid SPNEGO token header in the request.
     *
     * Expected results:
     * - Access to the SPNEGO servlet should be granted based on the valid SPNEGO credentials for the common token user.
     * - Client subject should contain GSS credentials matching the user used to obtain the SPNEGO token.
     * - Response should contain the invalid SSO cookie value since the runtime accepts it as-is due to not going
     * through the SSO flow.
     */

    @Test
    public void testInvokeAfterSSO_False_InvalidSSOCookie_ValidSpnegoToken() {
        try {
            testHelper.reconfigureServer("invokeAfterSso_false.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Add the invalid SSO cookie as a header, in addition to the valid SPNEGO token, and submit the request
            String ssoCookie = "SSO_INVALID";
            Map<String, String> headers = getCommonHeadersWithSSOCookie(ssoCookie);

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using invalid SSO cookie and valid SPNEGO token headers");
            String response = successfulSpnegoServletCall(headers, FATSuite.COMMON_TOKEN_USER,
                                                          FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE, FATSuite.COMMON_TOKEN_USER_IS_MANAGER);

            expectation.ssoCookieInvalidValue(response, SSO_COOKIE_NAME, ssoCookie);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - invokeAfterSSO attribute is set to true in server.xml.
     * - Access a protected resource by including an invalid SSO cookie and an invalid SPNEGO token header in the
     * request.
     *
     * Expected results:
     * - Authentication will fail due to the invalid SSO cookie and SPNEGO token, resulting in a 401.
     */

    @Test
    public void testInvokeAfterSSO_True_InvalidSSOCookie_InvalidSpnegoToken() {
        try {
            testHelper.reconfigureServer("invokeAfterSso_true.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Add the invalid SSO cookie as a header, in addition to the invalid SPNEGO token, and submit the request
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate: INVALID", SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            headers.put("Cookie", SSO_COOKIE_NAME + "=SSO_INVALID");

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using invalid SSO cookie and invalid SPNEGO token headers");
            unsuccessfulSpnegoServletCall(headers);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - invokeAfterSSO attribute is set to false in server.xml.
     * - Access a protected resource by including an invalid SSO cookie and an invalid SPNEGO token header in the
     * request.
     *
     * Expected results:
     * - Authentication will fail due to the invalid SPNEGO token, resulting in a 401.
     */

    @Test
    public void testInvokeAfterSSO_False_InvalidSSOCookie_InvalidSpnegoToken() {
        try {
            testHelper.reconfigureServer("invokeAfterSso_false.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Add the invalid SSO cookie as a header, in addition to the invalid SPNEGO token, and submit the request
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate: INVALID", SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            headers.put("Cookie", SSO_COOKIE_NAME + "=SSO_INVALID");

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using invalid SSO cookie and invalid SPNEGO token headers");
            unsuccessfulSpnegoServletCall(headers);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Access a protected resource to obtain an SSO cookie and GSS credentials for a user.
     * - Wait for the GSS credentials to expire.
     * - Access the protected resource again by including the SSO cookie in the request.
     *
     * Expected results:
     * - An SSO cookie should be received when first accessing the protected resource.
     * - Client subject should contain GSS credentials.
     * - Access to the protected resource with the SSO cookie after the GSS credentials have expired should be denied,
     * resulting in a 401.
     */
    // TODO This test should be re-enabled once the Java defect against modifying the SPNEGO lifetime has been completed
    @Ignore
    @Test
    public void testAccessResourceWithValidSsoCookieAfterGssCredentialsExpire() {
        try {
            testHelper.reconfigureServer("authCacheDecreased_invokeAfterTrue.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Create SPNEGO token with brief GSS credential lifetime
            Log.info(c, name.getMethodName(), "Creating new SPNEGO token with a shortened lifetime");
            int credLifetimeSeconds = 1;

            String targetSpn = "HTTP/" + TARGET_SERVER;
            String krb5Config = myServer.getServerRoot() + SPNEGOConstants.SERVER_KRB5_CONFIG_FILE;
            Subject subject = krb5Helper.kerberosLogin(myServer, FATSuite.COMMON_TOKEN_USER, FATSuite.COMMON_TOKEN_USER_PWD, krb5Config);
            String token = krb5Helper.createToken(subject, FATSuite.COMMON_TOKEN_USER, targetSpn, true, 5, 6, 7, 8, Krb5Helper.SPNEGO_MECH_OID);
//            String token = krb5Helper.createSpnegoToken(subject, FATSuite.COMMON_TOKEN_USER, targetSpn, true,
//                                                        GSSCredential.DEFAULT_LIFETIME, GSSCredential.INDEFINITE_LIFETIME,
//                                                        GSSCredential.INDEFINITE_LIFETIME, GSSContext.DEFAULT_LIFETIME);

            // Access the SPNEGO servlet and obtain GSS credentials and SSO cookie
            Map<String, String> headers = testHelper.setTestHeaders("Negotiate " + token, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
            Log.info(c, name.getMethodName(), "Accessing servlet in order to obtain SSO cookie and GSS credentials");
            String ssoCookie = getAndAssertSSOCookieAndGssCredsForHeaders(headers, FATSuite.COMMON_TOKEN_USER, FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE,
                                                                          FATSuite.COMMON_TOKEN_USER_IS_MANAGER);

            // Allow the GSS credentials to expire
            Log.info(c, name.getMethodName(), "Sleeping test to allow GSS credentials to expire");
            Thread.sleep((credLifetimeSeconds + 30) * 1000);

            // Access the SPNEGO servlet again with just the SSO cookie after GSS credentials have expired
            Log.info(c, name.getMethodName(), "Accessing servlet again with only the SSO cookie");
            headers = getNonSpnegoHeadersWithSSOCookie(ssoCookie);
            String response = unsuccessfulSpnegoServletCall(headers, SPNEGOConstants.DONT_IGNORE_ERROR_CONTENT);
            // Is this the right error message?

            expectation.spnegoNotSupported(response);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Access a protected resource to obtain an SSO cookie and GSS credentials for a user.
     * - Access the protected resource again by including the SSO cookie in the request, before the GSS credentials
     * have expired.
     *
     * Expected results:
     * - An SSO cookie should be received when first accessing the protected resource.
     * - Client subject should contain GSS credentials.
     * - Access to the protected resource with just the SSO cookie should be successful.
     * - Client subject should still contain GSS credentials.
     */

    @Test
    public void testAccessResourceWithValidSsoCookie() {
        try {
            testHelper.reconfigureServer("authCacheDecreased_invokeAfterTrue.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Access the SPNEGO servlet to obtain GSS credentials and SSO cookie
            Map<String, String> headers = createCommonHeaders();
            Log.info(c, name.getMethodName(), "Accessing servlet in order to obtain SSO cookie and GSS credentials");
            String ssoCookie = getAndAssertSSOCookieAndGssCredsForHeaders(headers, FATSuite.COMMON_TOKEN_USER, FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE,
                                                                          FATSuite.COMMON_TOKEN_USER_IS_MANAGER);

            // Access the resource again by including the SSO cookie but not the SPNEGO token in the request
            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet with only the SSO cookie");
            headers = getNonSpnegoHeadersWithSSOCookie(ssoCookie);
            successfulSpnegoServletCall(headers);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Test description:
     * - Set the LTPA SSO cookie expiration time to a very low value.
     * - Access a protected resource to obtain an SSO cookie and GSS credentials for a user.
     * - Wait for the SSO cookie to expire.
     * - Access the protected resource again by including the expired SSO cookie and a good SPNEGO token in the
     * request.
     *
     * Expected results:
     * - An SSO cookie should be received when first accessing the protected resource.
     * - Client subject should contain GSS credentials.
     * - Access to the protected resource with the expired SSO cookie and good SPNEGO token should be successful.
     * - Client subject should still contain GSS credentials.
     */

    @Test
    public void testAccessResourceWithExpiredSsoCookieAndValidSpnegoToken() {
        try {
            // Add bootstrap property for the desired LTPA token expiration time
            String ltpaExpirationPropName = "security.spnego.ltpa.expiration";
            int ltpaExpirationMinutes = 1;
            String ltpaExpiration = ltpaExpirationMinutes + "m";

            Map<String, String> props = new HashMap<String, String>();
            props.put(ltpaExpirationPropName, ltpaExpiration);
            testHelper.addBootstrapProperties(myServer, props);

            testHelper.reconfigureServer("authCacheDecreased_invokeAfterTrue_ltpaExpire.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            // Access the SPNEGO servlet to obtain GSS credentials and SSO cookie
            Map<String, String> headers = createCommonHeaders();
            Log.info(c, name.getMethodName(), "Accessing servlet in order to obtain SSO cookie and GSS credentials");
            String ssoCookie = getAndAssertSSOCookieAndGssCredsForHeaders(headers, FATSuite.COMMON_TOKEN_USER, FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE,
                                                                          FATSuite.COMMON_TOKEN_USER_IS_MANAGER);

            // Allow the SSO cookie to expire
            Log.info(c, name.getMethodName(), "Sleeping test to allow SSO cookie to expire");
            Thread.sleep((ltpaExpirationMinutes + 1) * 60 * 1000);

            // Access the resource again by including the expired SSO cookie in the request
            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet with the expired SSO cookie and valid SPNEGO token");
            headers = getCommonHeadersWithSSOCookie(ssoCookie);
            successfulSpnegoServletCall(headers);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }
    }

    /**
     * Accesses a protected servlet using the given authorized credentials, verifies the response against the user,
     * asserts that an SSO LTPA cookie was NOT received.
     *
     */
    @Test
    public void testDisableLTPACookie_true() {
        try {
            Log.info(c, name.getMethodName(), "Accessing servlet to check SSO LTPA cookie is not received");

            testHelper.reconfigureServer("disableLtpaCookie_true.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headers = createCommonHeaders();

            Log.info(c, name.getMethodName(), "Accessing SPNEGO servlet using valid SPNEGO token headers");
            String response = successfulServletCall(SPNEGOConstants.SIMPLE_SERVLET, headers, FATSuite.COMMON_TOKEN_USER,
                                                    FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE, FATSuite.COMMON_TOKEN_USER_IS_MANAGER, false,
                                                    ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);

            expectation.isSSOCookieNotPresent(response, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }

    }

    /**
     * Accesses a protected servlet using SPNEGO token, verifies the response against the user,
     * asserts that an SSO LTPA cookie was received.
     *
     */
    @Test
    public void testDisableLTPACookie_false() {
        try {
            Log.info(c, name.getMethodName(), "Accessing servlet to check SSO cookie was received");

            testHelper.reconfigureServer("disableLtpaCookie_false.xml", name.getMethodName(), SPNEGOConstants.RESTART_SERVER);

            Map<String, String> headers = createCommonHeaders();
            String response = successfulServletCall(SPNEGOConstants.SIMPLE_SERVLET, headers, FATSuite.COMMON_TOKEN_USER,
                                                    FATSuite.COMMON_TOKEN_USER_IS_EMPLOYEE, FATSuite.COMMON_TOKEN_USER_IS_MANAGER, true,
                                                    ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);

            expectation.isSSOCookiePresent(response, ServletClientImpl.DEFAULT_LTPA_COOKIE_NAME);

        } catch (Exception ex) {
            String message = CommonTest.maskHostnameAndPassword(ex.getMessage());
            Log.info(c, name.getMethodName(), "Unexpected exception: " + message);
            fail("Exception was thrown: " + message);
        }

    }

    /**
     * Accesses a protected servlet using the given authorized credentials, verifies the response against the user,
     * asserts that an SSO cookie was created, and returns the SSO cookie.
     *
     * @param user
     * @param password
     * @param isEmployee
     * @param isManager
     * @return
     */
    private String getAndAssertSSOCookieForUser(String user, String password, boolean isEmployee, boolean isManager) {
        Log.info(c, name.getMethodName(), "Accessing servlet in order to obtain SSO cookie for user: " + user);
        BasicAuthClient ssoClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, SSO_SERVLET_NAME, BasicAuthClient.DEFAULT_CONTEXT_ROOT);
        String response = ssoClient.accessProtectedServletWithAuthorizedCredentials(SSO_SERVLET, user, password);
        ssoClient.verifyResponse(response, user, isEmployee, isManager);

        String ssoCookie = ssoClient.getCookieFromLastLogin();

        expectation.isSSOCookiePresent(ssoCookie);

        return ssoCookie;
    }

    /**
     * Accesses the SPNEGO servlet using the given headers, verifies the response against the user, asserts that an SSO
     * cookie was created and GSS credentials are present, and returns the SSO cookie.
     *
     * @param headers
     * @param user
     * @param isEmployee
     * @param isManager
     * @return
     */
    private String getAndAssertSSOCookieAndGssCredsForHeaders(Map<String, String> headers, String user, boolean isEmployee, boolean isManager) {
        Log.info(c, name.getMethodName(), "Accessing servlet in order to obtain SSO cookie for user: " + user);
        BasicAuthClient basicClient = new BasicAuthClient(myServer, BasicAuthClient.DEFAULT_REALM, SPNEGOConstants.SIMPLE_SERVLET_NAME, BasicAuthClient.DEFAULT_CONTEXT_ROOT);
        String response = basicClient.accessProtectedServletWithValidHeaders(SPNEGOConstants.SIMPLE_SERVLET, headers, SPNEGOConstants.IGNORE_ERROR_CONTENT,
                                                                             SPNEGOConstants.HANDLE_SSO_COOKIE);
        basicClient.verifyResponse(response, user, isEmployee, isManager);

        expectation.successfulExpectationsSpnegoServletCallForMappedUser(response, user);

        String ssoCookie = basicClient.getCookieFromLastLogin();

        expectation.isSSOCookiePresent(ssoCookie);
        return ssoCookie;
    }

    /**
     * Returns a map with the "Authorization" header set to "Negotiate " + the common SPNEGO token, "User-Agent" set to Firefox,
     * and "Host" set to TARGET_SERVER. In addition, a "Cookie" header with its value set to the specified
     * value is included in the map.
     *
     * @param ssoCookie
     * @return
     * @throws Exception
     */
    private Map<String, String> getCommonHeadersWithSSOCookie(String ssoCookie) throws Exception {
        Map<String, String> headers = createCommonHeaders();
        headers.put("Cookie", SSO_COOKIE_NAME + "=" + ssoCookie);
        return headers;
    }

    /**
     * Returns a map with the "User-Agent" header set to Firefox and "Host" set to TARGET_SERVER. In addition, a "Cookie"
     * header with its value set to the specified value is included in the map.
     *
     * @param ssoCookie
     * @return
     */
    private Map<String, String> getNonSpnegoHeadersWithSSOCookie(String ssoCookie) {
        Map<String, String> headers = testHelper.setTestHeaders(null, SPNEGOConstants.FIREFOX, TARGET_SERVER, null);
        headers.put("Cookie", SSO_COOKIE_NAME + "=" + ssoCookie);
        return headers;
    }

}
