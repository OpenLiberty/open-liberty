/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.oidc.certification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonValue.ValueType;
import javax.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.Constants.JsonCheckType;
import com.ibm.ws.security.fat.common.Constants.StringCheckType;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.JsonObjectExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.social.MessageConstants;
import com.ibm.ws.security.fat.common.social.oidc.certification.Constants;
import com.ibm.ws.security.openidconnect.client.fat.expectations.UserInfoJsonExpectation;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OidcCertificationRPBasicProfileTests extends com.ibm.ws.security.fat.common.social.oidc.certification.OidcCertificationRPBasicProfileTests {

    public static Class<?> thisClass = OidcCertificationRPBasicProfileTests.class;

    @Server("com.ibm.ws.security.openidconnect.client-1.0_fat.rp")
    public static LibertyServer thisServer;

    public static final String CERTIFICATION_RP_ID = "was-liberty";
    public static final String RP_ID_FOR_PROFILE = CERTIFICATION_RP_ID + "-code";

    private static final String CWWKG0011W_CONFIG_VALIDATION_FAILED = "CWWKG0011W";
    private static final String CWWKG0081E_CONFIG_ATTRIBUTE_VALUE_INVALID = "CWWKG0081E";
    private static final String CWWKS3103W_NO_USERS_IN_REGISTRY = "CWWKS3103W";

    @BeforeClass
    public static void setUp() throws Exception {
        setServerAndTestSpecificValues();

        serverTracker.addServer(server);

        List<String> waitForMessages = new ArrayList<String>();
        waitForMessages.add(MessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + "oidcclient");

        List<String> ignoreStartupMessages = new ArrayList<String>();
        ignoreStartupMessages.add(CWWKS3103W_NO_USERS_IN_REGISTRY);
        // The below messages are all emitted because we're using variables in the server config and setting the variables after startup
        ignoreStartupMessages.add(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "signatureAlgorithm");
        ignoreStartupMessages.add(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "tokenEndpointAuthMethod");
        ignoreStartupMessages.add(CWWKG0011W_CONFIG_VALIDATION_FAILED + ".*" + CWWKG0081E_CONFIG_ATTRIBUTE_VALUE_INVALID + ".*" + "userInfoEndpointEnabled");
        ignoreStartupMessages.add(MessageConstants.CWWKG0083W_CONFIG_INVALID_VALUE_USING_DEFAULT + ".*" + "userInfoEndpointEnabled");
        server.addIgnoredErrors(ignoreStartupMessages);

        server.startServerUsingConfiguration(Constants.CONFIGS_DIR + "rp_server_oidcCertification.xml", waitForMessages);
    }

    /**
     * Sets the member variables defined in the parent class that are required to be set in order for the tests to work.
     */
    static void setServerAndTestSpecificValues() {
        server = thisServer;
        server.setBvtPortPropertyName("RP_HTTP_default");
        server.setBvtSecurePortPropertyName("RP_HTTP_default.secure");

        protectedUrl = "https://" + server.getHostname() + ":" + server.getBvtSecurePort() + "/formlogin/SimpleServlet";
        clientId = "client01";
        rpId = RP_ID_FOR_PROFILE;
        certificationBaseUrl = CERTIFICATION_HOST_AND_PORT + "/" + rpId;
        defaultTokenEndpointAuthMethod = "post";
    }

    @Override
    protected String getRedirectUriForClient(String clientId) {
        return "https://" + server.getHostname() + ":" + server.getBvtSecurePort() + "/oidcclient/redirect/" + clientId;
    }

    @Override
    protected Expectations getUnauthorizedResponseExpectations() {
        Expectations expectations = super.getUnauthorizedResponseExpectations();
        expectations.addExpectation(new JsonObjectExpectation("error", ValueType.NUMBER, HttpServletResponse.SC_UNAUTHORIZED));
        return expectations;
    }

    /**
     * The social login code does not emit a warning message for setting the signature algorithm to "none," but the OIDC client
     * code does. Hence, that warning message needs to be added as an additional expectation for this test.
     */
    @Override
    protected Expectations getTestExpectations_rp_id_token_sig_none(String conformanceTestName, String assignedUserName) {
        Expectations expectations = super.getTestExpectations_rp_id_token_sig_none(conformanceTestName, assignedUserName);
        String expectedMessage = com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants.CWWKS1741W_OIDC_CLIENT_NONE_ALG + ".+" + clientId;
        expectations.addExpectation(new ServerMessageExpectation(server, expectedMessage));
        return expectations;
    }

    /**
     * This method has to be overridden because the OIDC client's tokenEndpointAuthMethod value options are different from the
     * social login metatype. The OIDC client config uses "basic" and "post" as options while the social login metatype uses
     * "client_secret_basic" and "client_secret_post," respectively.
     */
    @Override
    protected Map<String, String> getUpdatedConfigVariables_rp_token_endpoint_client_secret_basic() {
        Map<String, String> varsToSet = new HashMap<String, String>();
        varsToSet.put(Constants.CONFIG_VAR_TOKEN_ENDPOINT_AUTH_METHOD, "basic");
        return varsToSet;
    }

    @Override
    protected Expectations getTestExpectations_rp_userinfo_bearer_header(String conformanceTestName, String assignedUserName) {
        Expectations expectations = getSuccessfulConformanceTestExpectations(conformanceTestName, assignedUserName, UserInfo.ENABLED);
        expectations.addExpectation(new UserInfoJsonExpectation("sub", StringCheckType.EQUALS, assignedUserName));
        expectations.addExpectation(new UserInfoJsonExpectation("name", JsonCheckType.KEY_DOES_NOT_EXIST, null));
        expectations.addExpectation(new UserInfoJsonExpectation("address", JsonCheckType.KEY_DOES_NOT_EXIST, null));
        expectations.addExpectation(new UserInfoJsonExpectation("email", JsonCheckType.KEY_DOES_NOT_EXIST, null));
        expectations.addExpectation(new UserInfoJsonExpectation("phone_number", JsonCheckType.KEY_DOES_NOT_EXIST, null));
        return expectations;
    }

    @Override
    protected Expectations getTestExpectations_rp_scope_userinfo_claims(String conformanceTestName, String assignedUserName) {
        Expectations expectations = getSuccessfulConformanceTestExpectations(conformanceTestName, assignedUserName, UserInfo.ENABLED);
        expectations.addExpectation(new UserInfoJsonExpectation("sub", StringCheckType.EQUALS, assignedUserName));
        expectations.addExpectation(new UserInfoJsonExpectation("name"));
        expectations.addExpectation(new UserInfoJsonExpectation("address", ValueType.OBJECT));
        expectations.addExpectation(new UserInfoJsonExpectation("email"));
        expectations.addExpectation(new UserInfoJsonExpectation("phone_number"));
        return expectations;
    }

    @Override
    protected Expectations getServletOutputUserInfoPresenceExpectations(UserInfo userInfo) {
        Expectations expectations = new Expectations();
        if (userInfo == UserInfo.ENABLED) {
            // If UserInfo is enabled, the UserInfo information must, at at minimum, include the "sub" claim
            expectations.addExpectation(new UserInfoJsonExpectation("sub"));
        } else {
            expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_DOES_NOT_MATCH, UserInfoJsonExpectation.USER_INFO_SERVLET_OUTPUT_REGEX, "UserInfo string in the subject's private credentials should have been missing because the UserInfo endpoint is not enabled."));
        }
        return expectations;
    }

}
