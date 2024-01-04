/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
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

package io.openliberty.checkpoint.fat.security.oidc;

import static io.openliberty.checkpoint.fat.security.common.FATSuite.getTestMethod;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.structures.ValidationDataToExpectationConverter;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.CheckpointTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerWrapper;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(FATRunner.class)
@LibertyServerWrapper
@CheckpointTest
public class OidcClientBasicTests extends CommonTest {

    private static Class<?> thisClass = OidcClientBasicTests.class;

    private static final String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    private static final String test_FinalAction = Constants.LOGIN_USER;
    private static final String TCP_CHANNEL_STARTED = "CWWKO0219I:.*defaultHttpEndpoint-ssl";

    public TestMethod testMethod;

    private static LibertyServer opServer;

    private static LibertyServer rpServer;

    private static final String OP_SERVER = "com.ibm.ws.security.openidconnect.client-1.0_fat.op";

    private static final String RP_SERVER = "com.ibm.ws.security.openidconnect.client-1.0_fat.rp";

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action().forServers(OP_SERVER, RP_SERVER).fullFATOnly())
                    .andWith(new JakartaEE10Action().forServers(OP_SERVER, RP_SERVER).fullFATOnly());

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUpServers() throws Exception {

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate in OP config files
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        skipServerStart = true;

        testOPServer = commonSetUp(OP_SERVER, "server.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                                   Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        opServer = testOPServer.getServer();

        skipServerStart = true;
        testRPServer = commonSetUp(RP_SERVER, "server.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
                                   Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        rpServer = testRPServer.getServer();
    }

    @Before
    public void setUp() throws Exception {
        testMethod = getTestMethod(TestMethod.class, testName);
        opServer.startServer(testMethod + ".log");

        rpServer.setCheckpoint(CheckpointPhase.AFTER_APP_START, false, null);
        rpServer.startServer(testMethod + ".log");
        rpServer.checkpointRestore();

        assertNotNull("Expected CWWKO0219I message not found", rpServer.waitForStringInLog(TCP_CHANNEL_STARTED));

        testSettings.setFlowType(Constants.RP_FLOW);
    }

    @Test
    public void testOidcClientTestGetMainPath() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.addRequestParms();

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, updatedTestSettings);
        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

        testUserInfo(wc);
    }

    /**
     * Test that userinfo is retrieved and available from an API call. If userinfo url is defined and enabled in metadata, then
     * upon authentication the userinfo JSON from the OP, if available, is to be stored in the subject as a string and made
     * accessible through the PropagationHelper API. Since we invoked the protected resource, we should already be authenticated.
     * This calls a jsp that invokes the PropagationHelper.getUserInfo() API to check the userinfo.
     */
    void testUserInfo(WebConversation wc) throws Exception {
        String endpoint = "https://localhost:" + testRPServer.getHttpDefaultSecurePort() + "/formlogin/propagationHelperUserInfoApiTest.jsp";

        GetMethodWebRequest request = new GetMethodWebRequest(endpoint);
        WebResponse resp = wc.getResponse(request);
        String response = resp.getText();
        Log.info(thisClass, _testName, "Got JSP response: " + response);

        String testAction = "testUserInfo";
        String expectedUser = testSettings.getAdminUser();
        Expectations expectations = new Expectations();
        expectations.addExpectation(Expectation.createResponseExpectation(testAction, "\"sub\":\"" + expectedUser + "\"",
                                                                          "Did not find expected \"sub\" claim and value in the JSP response."));
        expectations.addExpectation(Expectation.createResponseExpectation(testAction, "\"name\":\"" + expectedUser + "\"",
                                                                          "Did not find expected \"name\" claim and value in the JSP response."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_MATCHES, "\"iss\":\"http[^\"]+/OidcConfigSample\"", "Did not find expected \"iss\" claim and value in the JSP response."));
        List<validationData> convertedExpectations = ValidationDataToExpectationConverter.convertExpectations(expectations);
        validationTools.validateResult(resp, testAction, convertedExpectations, testSettings);
    }

    @After
    public void stopServers() throws Exception {
        opServer.stopServer();
        rpServer.stopServer();
    }

    static enum TestMethod {
        testOidcClientTestGetMainPath,
        unknown
    }
}
