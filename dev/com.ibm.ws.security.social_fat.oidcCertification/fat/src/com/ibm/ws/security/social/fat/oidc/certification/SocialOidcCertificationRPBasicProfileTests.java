/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.oidc.certification;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.social.MessageConstants;
import com.ibm.ws.security.fat.common.social.oidc.certification.Constants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class SocialOidcCertificationRPBasicProfileTests extends com.ibm.ws.security.fat.common.social.oidc.certification.OidcCertificationRPBasicProfileTests {

    public static Class<?> thisClass = SocialOidcCertificationRPBasicProfileTests.class;

    @Server("com.ibm.ws.security.social_fat.oidcCertification")
    public static LibertyServer thisServer;

    public static final String CERTIFICATION_RP_ID = "open-liberty";
    public static final String RP_ID_FOR_PROFILE = CERTIFICATION_RP_ID + "-code";

    @BeforeClass
    public static void setUp() throws Exception {
        setServerAndTestSpecificValues();

        serverTracker.addServer(server);

        List<String> waitForMessages = new ArrayList<String>();
        waitForMessages.add(MessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + Constants.DEFAULT_CONTEXT_ROOT);

        List<String> ignoreStartupMessages = new ArrayList<String>();
        ignoreStartupMessages.add(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "tokenEndpointAuthMethod");
        ignoreStartupMessages.add(MessageConstants.CWWKG0083W_CONFIG_INVALID_VALUE_USING_DEFAULT + ".*" + "userInfoEndpointEnabled");
        server.addIgnoredErrors(ignoreStartupMessages);

        server.startServerUsingConfiguration(Constants.CONFIGS_DIR + "server_oidcCertification.xml", waitForMessages);
    }

    /**
     * Sets the member variables defined in the parent class that are required to be set in order for the tests to work.
     */
    static void setServerAndTestSpecificValues() {
        server = thisServer;
        protectedUrl = "https://" + server.getHostname() + ":" + server.getBvtSecurePort() + "/formlogin/SimpleServlet";
        clientId = "oidcLogin1";
        rpId = RP_ID_FOR_PROFILE;
        certificationBaseUrl = CERTIFICATION_HOST_AND_PORT + "/" + rpId;
        defaultTokenEndpointAuthMethod = "client_secret_post";
    }

    @Override
    protected String getRedirectUriForClient(String clientId) {
        return "https://" + server.getHostname() + ":" + server.getBvtSecurePort() + Constants.DEFAULT_CONTEXT_ROOT + "/redirect/" + clientId;
    }

    @Override
    protected Expectations getUnauthorizedResponseExpectations() {
        Expectations expectations = super.getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MessageConstants.CWWKS5489E_PUBLIC_FACING_ERROR, "Should have found the public-facing error message in the protected resource invocation response but did not."));
        return expectations;
    }

}
