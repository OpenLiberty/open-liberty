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
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.CommonTests.JaxRSClientGenericTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will configure the OpenID Connect RP and RS (using JWT Token with JWK cert) for discovery of OP endpoints.
 * Discovery of OP endpoints is configured in the openidConnectClient section in server.xml by specifying the discoveryEndpointUrl instead of configuring
 * each individual OP endpoint URL (such as authorizationEndpointUrl and tokenEndpointUrl) separately. This line below is used to configure RP discovery:
 *
 * discoveryEndpointUrl="https://localhost:${bvt.prop.OP_HTTP_default.secure}/oidc/endpoint/OidcConfigSample/.well-known/openid-configuration"
 *
 * With discovery, all the discovered endpoints are HTTPS. Any endpoints which are specified in the configuration will be ignored and the
 * discovered endpoints will be used instead.
 *
 * This test class extends JaxRSClientGenericTest and exercises the authorization, token, and jwk endpoints after discovery.
 * The token request results in a JWT token with JWK certificate.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class OidcJaxRSClientDiscoveryBasicTests extends JaxRSClientGenericTests {

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcJaxRSClientDiscoveryBasicTests.class;

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.HELLOWORLD_SERVLET);
            }
        };
        List<String> rp_apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for JWT token with JWK certificate
        String tokenType = Constants.JWT_TOKEN;
        String certType = Constants.JWK_CERT;

        // Start the Generic/App Server
        genericTestServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.rsd", "rs_server_orig.xml", Constants.GENERIC_SERVER, apps,
                                        Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        // We use a variable insert for the validationMethod config attribute which the config evaluator will think is invalid
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "validationMethod");

        // Start the OIDC OP server
        //CommonRoutines.startOPServer(opServer, "op_server_orig.xml");
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.op", "op_server_orig.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                                   Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, null, null, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.rpd", "rp_server_orig.xml", Constants.OIDC_RP, Constants.NO_EXTRA_APPS,
                                   Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);
        testRPServer.addIgnoredServerException("CWWKS1521W"); // Ignore jwk endpoint is specified in the config along with discovery
        // override actions that generic tests should use - Need to skip consent form as httpunit
        // cannot process the form because of embedded javascript

        test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
        test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
        test_FinalAction = Constants.LOGIN_USER;
        testSettings.setFlowType(Constants.RP_FLOW);
        testSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/Protected_JaxRSClient");

        Map<String, String> map = new HashMap<String, String>();
        map.put(Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/helloworld");
        map.put(Constants.WHERE, testSettings.getWhere());
        map.put(Constants.TOKEN_CONTENT, Constants.CURRENT_VALUE);
        testSettings.setRequestParms(map);
    }

}
