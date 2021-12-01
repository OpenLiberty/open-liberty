/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.IBM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.FATSuite;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.CommonTests.JaxRSClientAPITests;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;

// TODO
/** NOTE: I've added some hooks for future testing of a userinfo endpoint that can return not just json, but a JWS or JWE **/
/** For now, just ignore comments, checks and method calls involving "userinfo" - for now, it means nothing. **/

/**
 * This is the test class that will run Propagation tests (RP to RS).
 * This test class extends JaxRSClientAPITests.
 * JaxRSClientAPITests contains common tests for all propagation tests.
 *
 * These tests will verify propagation when:
 * 1) The OP generates an opaque access token - the test setup randomly chooses the validation endpoint (userinfo or introspect)
 * 2) The OP generates an JWT (JWS) access token - the test setup randomly chooses the validation endpoint (userinfo or
 * introspect)
 * 3) The OP generates a JWT (JWS) access token, but the RP config specifies an alternate value for the token endpoint - that
 * endpoint
 * will create a JWE (To ensure that the RS will consume it) - the test setup randomly chooses the validation endpoint (userinfo
 * or introspect)
 * 4) The OP generates a JWT (JWS) (it could create a JWE or opaque) access token (these tests will use JWS) - the test setup will
 * set the validation
 * method to userinfo and set the endpoint to an alternate endpoint that will return userinfo in the form of a JWS. This is to
 * ensure that
 * the RS will be able to consume userinfo data in the form of a JWS.
 * 5) The OP generates a JWT (JWS) (it could create a JWE or opaque) access token (these tests will use JWS) - the test setup will
 * set the validation
 * method to userinfo and set the endpoint to an alternate endpoint that will return userinfo in the form of a JWE. This is to
 * ensure that
 * the RS will be able to consume userinfo data in the form of a JWE.
 *
 * We don't need to test all combinations of tokens from the tokenEndpoint and the info from userinfo - our OP can not generate
 * encrypted data at this
 * time and the 2 functions are separate.
 *
 **/

@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException" })
@RunWith(FATRunner.class)
public class OidcJaxRSClientAPITests extends JaxRSClientAPITests {

    @ClassRule
    public static RepeatTests repeat = RepeatTests
            .with(new SecurityTestRepeatAction(Constants.OPAQUE_TOKEN_FORMAT))
            .andWith(new SecurityTestRepeatAction(Constants.JWS_TOKEN_FORMAT))
            .andWith(new SecurityTestRepeatAction(Constants.JWE_TOKEN_FORMAT));

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        thisClass = OidcJaxRSClientAPITests.class;

        List<String> rs_apps = new ArrayList<String>() {
            {
                add(Constants.HELLOWORLD_SERVLET);
            }
        };
        List<String> op_apps = new ArrayList<String>() {
            {
                add(Constants.TOKENENDPT_APP);
            }
        };

        Log.info(thisClass, "setup", "Repeat flag: " + FATSuite.repeatFlag);

        // Since the Liberty OP does not yet create JWE's (it only creates JWS'), we need to use test token and userinfo endpoints
        // when the OP does support returning JWE's, we can remove most of the following code and just startup the OP server with unique configs
        // that will produce the correct JWT and userinfo response.
        String authorizeEndpointUri = "\"http://localhost:${bvt.prop." + Constants.SYS_PROP_PORT_OP_HTTP_DEFAULT + "}/oidc/endpoint/OidcConfigSample/authorize\"";
        String tokenEndpointUri = "\"http://localhost:${bvt.prop." + Constants.SYS_PROP_PORT_OP_HTTP_DEFAULT + "}/oidc/endpoint/OidcConfigSample/token\"";
        String validationEndpointUri = "\"http://localhost:${bvt.prop." + Constants.SYS_PROP_PORT_OP_HTTP_DEFAULT + "}/oidc/endpoint/OidcConfigSample/${oAuthOidcRSValidationEndpoint}\"";
        String decryptionKey = "";
        String rp_rs_sslRef = "";
        String sigAlg = "${" + Constants.BOOT_PROP_OIDC_SIG_ALG + "}";
        String trustAlias = null;
        String trustStoreRef = null;

        testSettings = new TestSettings();
        // all but the opapue token repeat type should use some form of a JWT token (JWS or JWE) - so set the token type to JWT by default
        String tokenType = Constants.JWT_TOKEN;
        if (Constants.OPAQUE_TOKEN_FORMAT.equals(FATSuite.repeatFlag)) {
            tokenType = Constants.ACCESS_TOKEN_KEY;
        }
        if (Constants.JWE_TOKEN_FORMAT.equals(FATSuite.repeatFlag)) {
            // set the token endpoint to our test endpoint to return a JWE
            authorizeEndpointUri = "http://localhost:${bvt.prop." + Constants.SYS_PROP_PORT_OP_HTTP_DEFAULT + "}/TokenEndpointServlet/getToken";
            tokenEndpointUri = "http://localhost:${bvt.prop." + Constants.SYS_PROP_PORT_OP_HTTP_DEFAULT + "}/TokenEndpointServlet/getToken";
            decryptionKey = "rs256";
            rp_rs_sslRef = "allSigAlgSSLSettings";
            sigAlg = "rs256";
            trustAlias = "rs256";
            trustStoreRef = "trust_allSigAlg";
        }
        // if we're testing userinfo returning a jwt response (vs a json response), we need to set the validation method to userinfo - the methods that set
        //   the validation method also set the validation endpoint variable value to userinfo (or introspect), but, for these tests, we're going to use unique
        //   variables in our server.xml to define the validation endpoint.
        // I assume that at some point when our OP's userinfo endpoint can produce JWS' or JWE's, we'll just need to set an OP config property to
        //  set what type of response to create.  For now, we'll just reference our test userinfo empoints
        if (Constants.JWS_USERINFO_DATA.equals(FATSuite.repeatFlag) || Constants.JWE_USERINFO_DATA.equals(FATSuite.repeatFlag)) {
            classOverrideValidationEndpointValue = Constants.USERINFO_ENDPOINT;
            if (Constants.JWS_USERINFO_DATA.equals(FATSuite.repeatFlag)) {
                // unique endpoint for JWS
                validationEndpointUri = "http://localhost:${bvt.prop." + Constants.SYS_PROP_PORT_OP_HTTP_DEFAULT + "}/UserinfoEndpointServlet/getJws";
            } else {
                // unique endpoint for JWE
                validationEndpointUri = "http://localhost:${bvt.prop." + Constants.SYS_PROP_PORT_OP_HTTP_DEFAULT + "}/UserinfoEndpointServlet/getJwe";
            }
        } else {
            // let test framework dynamically choose userinfo or instrospect
            classOverrideValidationEndpointValue = null;
        }

        Log.info(thisClass, "setup", "setting up: " + validationEndpointUri);
        Log.info(thisClass, "setup", "setting up: " + tokenEndpointUri);
        HashMap<String, String> validationSettings = new HashMap<String, String>();
        validationSettings.put("special_authorizeEndpoint", authorizeEndpointUri); // need to override the authorize token endpoint in the implicit flow when using JWE
        validationSettings.put("special_tokenEndpoint", tokenEndpointUri);
        validationSettings.put("special_validationEndpoint", validationEndpointUri);
        validationSettings.put("special_keyManagementKeyAlias", decryptionKey);
        validationSettings.put("special_sslref", rp_rs_sslRef);
        validationSettings.put("speical_sigAlg", sigAlg);
        validationSettings.put("special_trustAlias", trustAlias);
        validationSettings.put("special_trustStoreRef", trustStoreRef);
        setMiscBootstrapParms(validationSettings);

        // Start the Generic/App Server
        genericTestServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.rs", "rs_server_api_orig.xml", Constants.GENERIC_SERVER, rs_apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE);
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKG0033W_CONFIG_REFERENCE_NOT_FOUND);

        // Start the OIDC OP server - tell it to generate JWT access tokens
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.opWithStub", "op_server_encrypt.xml", Constants.OIDC_OP, op_apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, null, null, true, true, tokenType, Constants.X509_CERT);
        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.jaxrs.rp", "rp_server_api_orig.xml", Constants.OIDC_RP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
        testRPServer.addIgnoredServerException(MessageConstants.CWWKG0033W_CONFIG_REFERENCE_NOT_FOUND);

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
        map.put(Constants.TOKEN_CONTENT, Constants.API_VALUE);
        map.put(Constants.CONTEXT_SET, "true");
        if (Constants.JWE_TOKEN_FORMAT.equals(FATSuite.repeatFlag)) {
            map.put("skipApiValidation", "true");
        }
        testSettings.setRequestParms(map);
        testSettings.setTokenEndpt(testSettings.getTokenEndpt().replace("oidc/endpoint/OidcConfigSample/token", "TokenEndpointServlet").replace("oidc/providers/OidcConfigSample/token", "TokenEndpointServlet") + "/saveToken");
        testSettings.setUserinfoEndpt(testSettings.getUserinfoEndpt().replace("oidc/endpoint/OidcConfigSample/userinfo", "UserinfoEndpointServlet").replace("oidc/providers/OidcConfigSample/userinfo", "UserinfoEndpointServlet") + "/saveToken");

    }

    @Override
    @Before
    public void setTestName() throws Exception {
        super.setTestName();
        // Invoke the test TokenEndpoint stub.  It will invoke the Jwt Builder to create a JWT Token (using the builder specified in the builderId passed in via parms
        // The TokenEndpoint stub will save that token and it will be returned when the RP uses it's TokenEnpdointUrl specified in it's config
        //  (That url is:  http://localhost:${bvt.prop.security_1_HTTP_default}/TokenEndpointServlet/getToken)
        if (Constants.JWE_TOKEN_FORMAT.equals(FATSuite.repeatFlag)) {
            List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "builderId", "defaultJWT");
            genericInvokeEndpointWithHttpUrlConn(_testName, null, testSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, null);
        }
        // TODO - prep for future testing
        // save a Token - for these tests, the content doesn't matter - just trying to validate the behavior with propagation
        if (Constants.JWS_USERINFO_DATA.equals(FATSuite.repeatFlag)) {
            saveJWSUserInfoResponse();
        }
        if (Constants.JWE_USERINFO_DATA.equals(FATSuite.repeatFlag)) {
            saveJWEUserInfoResponse();
        }
    }

}
