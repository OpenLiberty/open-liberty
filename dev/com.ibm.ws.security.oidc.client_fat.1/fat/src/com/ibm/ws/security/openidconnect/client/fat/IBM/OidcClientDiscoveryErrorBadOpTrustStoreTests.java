/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.DiscoveryUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test Purpose:
 * <OL>
 * <LI>With discovery configured at the RP, attempt to access a test servlet specifying valid OP URL.
 * <LI>The OP trust store is missing the SSL certificate for the RP.
 * <LI>Results in 401 error with messages logged.
 * </OL>
 * <P>
 * Expected Results:
 * <OL>
 * <LI>The discovery should be attempted and should fail with 401 when the RP tries process the authentication request.
 * </OL>
 **/
// different versions of java issue different exceptions - allow appropriate exceptions from any of our supported java versions
@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException", "java.security.cert.CertPathBuilderException", "sun.security.validator.ValidatorException", "javax.net.ssl.SSLHandshakeException", "com.ibm.websphere.ssl.SSLException", "java.lang.Exception" })
@RunWith(FATRunner.class)
public class OidcClientDiscoveryErrorBadOpTrustStoreTests extends CommonTest {

    public static Class<?> thisClass = OidcClientDiscoveryErrorBadOpTrustStoreTests.class;

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", "op_server_ssl_bad_truststore_clientAuth.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        DiscoveryUtils.waitForOPDiscoveryToBeReady(testSettings);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rpd", "rp_server_orig.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        testRPServer.addIgnoredServerException("CWWKS1859E"); // Ignore exceptions from bad client secret test
        testRPServer.addIgnoredServerException("CWWKS1741W"); // Ignore from sigAlgMismatch2 - message is not always logged
        testRPServer.addIgnoredServerException("CWWKS1524E"); // Ignore message indicating unsuccessful response from endpoint
        testRPServer.addIgnoredServerException("CWWKS1525E"); // Ignore message indicating discovery failed
        testRPServer.addIgnoredServerException("CWWKO0801E"); // Ignore message indicating connection failure
        testRPServer.addIgnoredServerException("CWPKI0022E"); // Ignore message indicating Handshake failure

        // try to wait for discovery to have populated the RP config
        // Don't stop if this fails (there is a chance it could be ready by the time the tests actually run
        DiscoveryUtils.waitForRPDiscoveryToBeReady(testSettings);
    }

    @Test
    public void test_discoveryError_BadOPTrustStoreWithClientAuth() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(updatedTestSettings.getTestURL().replace(Constants.DEFAULT_SERVLET, "simple/goodSSL"));

        List<validationData> expectations = validationTools.add401Responses(Constants.GET_LOGIN_PAGE);
        validationTools.addMessageExpectation(testRPServer, expectations, Constants.GET_LOGIN_PAGE, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Did not find CWWKS1534E message saying the authorization endpoint URL was missing.", MessageConstants.CWWKS1534E_MISSING_AUTH_ENDPOINT);

        WebConversation wc = new WebConversation();

        try {
            genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);
        } catch (Exception e) {
            // there can be one of multiple exceptions thrown for this erroneous condition - the exceptions vary by JDK - validating the exception
            // here allows the test to handle instances where we can get one of multiple possible error better than using an expectation.
            String[] allowedExceptions = new String[] { "javax.net.ssl.SSLException", "javax.net.ssl.SSLHandshakeException", "java.net.SocketException", "java.io.IOException" };
            msgUtils.assertTrueAndLog(_testName, "Expected one of the following in the exception: " + Arrays.toString(allowedExceptions) + ". Actually received: " + e,
                    validationTools.foundOneErrorInException(e, allowedExceptions));
        }

    }

}
