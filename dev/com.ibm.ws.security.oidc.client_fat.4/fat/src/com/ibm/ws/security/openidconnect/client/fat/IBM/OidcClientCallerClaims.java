/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that will run tests to verify <Fill in description here>
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OidcClientCallerClaims extends CommonTest {

    public static Class<?> thisClass = OidcClientCallerClaims.class;

    public static final JwtTokenBuilderUtils tokenBuilderHelpers = new JwtTokenBuilderUtils();

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        // apps are taking too long to start up for the normal app check, but, we need to be sure that they're ready before we try to use them.
        List<String> opExtraMsgs = new ArrayList<String>() {
            {
                add("CWWKZ0001I.*" + Constants.TOKEN_ENDPOINT_SERVLET);
                add("CWWKZ0001I.*" + Constants.USERINFO_ENDPOINT_SERVLET);
            }
        };

        testSettings = new TestSettings();

        // Set config parameters for Access token with X509 Certificate in OP config files
        String tokenType = Constants.ACCESS_TOKEN_KEY;
        String certType = Constants.X509_CERT;

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.4.opWithStub", "op_server_orig.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS,
                Constants.DO_NOT_USE_DERBY, opExtraMsgs, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.4.rp", "rp_server_orig.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY,
                Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, certType);

        testSettings.setFlowType(Constants.RP_FLOW);
        testSettings.setTokenEndpt(testSettings.getTokenEndpt()
                .replace("oidc/endpoint/OidcConfigSample/token", "TokenEndpointServlet")
                .replace("oidc/providers/OidcConfigSample/token", "TokenEndpointServlet") + "/saveToken");
        
        testSettings.setUserinfoEndpt(testSettings.getUserinfoEndpt()
        		.replace("oidc/endpoint/OidcConfigSample/userinfo", "UserinfoEndpointServlet")
        		.replace("oidc/providers/OidcConfigSample/userinfo", "UserinfoEndpointServlet") + "/saveToken");

    }

    public JWTTokenBuilder createBuilderWithDefaultClaims() throws Exception {

        JWTTokenBuilder builder = new JWTTokenBuilder();
        builder.setIssuer(testOPServer.getHttpString() + "/TokenEndpointServlet");
        builder.setIssuedAtToNow();
        builder.setExpirationTimeMinutesIntheFuture(5);
        builder.setScope("openid profile");
        builder.setSubject("testuser");
        builder.setRealmName("BasicRealm");
        builder.setTokenType("Bearer");
        builder.setAudience("client01");
        builder.setClaim(PayloadConstants.SESSION_ID, JwtTokenBuilderUtils.randomSessionId());
        builder.setAlorithmHeaderValue(Constants.SIGALG_RS256);
        builder.setRSAKey(testOPServer.getServer().getServerRoot() + "/RS256private-key.pem");
        return builder;
    }

    /******************************* tests *******************************/
    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken Userinfo"
     * caller claims exist in all tokens
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientCallerClaims_claims_exist_in_all_tokens() throws Exception {

        String appName = "sampleBuilder";

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.IDToken_STR);
        // add or update claims (To remove claims you might need to replicate what createBuilderWithDefaultClaims does and just omit the setting of the claim)
        JWTTokenBuilder builder = createBuilderWithDefaultClaims();
        builder.setClaim("auth_time", builder.getRawClaims().getClaimValue(PayloadConstants.ISSUED_AT));
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the auth_time printed in the app output", null, "\"auth_time\":" + builder.getRawClaims().getClaimValue("auth_time"));
        builder.setClaim(PayloadConstants.USER_PRINCIPAL_NAME, "myEmail@ibm.com");
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the " + PayloadConstants.USER_PRINCIPAL_NAME + " printed in the app output", null, "\"" + PayloadConstants.USER_PRINCIPAL_NAME + "\":\"" + builder.getRawClaims().getClaimValue(PayloadConstants.USER_PRINCIPAL_NAME) + "\"");
        builder.setClaim("unique_name", builder.getRawClaims().getClaimValue(PayloadConstants.SESSION_ID));
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the unique_name printed in the app output", null, "\"unique_name\":\"" + builder.getRawClaims().getClaimValue("unique_name") + "\"");

        // calling build to create a JWT token (as a string)
        String jwtToken = builder.build();

        // the built token will be passed to the test app via the overrideToken parm - it will be saved to be later returned during the auth process.
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken); // idt and access token claims will be same
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);

        List<endpointSettings> userinfParms = eSettings.addEndpointSettings(null, "userinfoToken", jwtToken);
        //(That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations);
        // we created and saved tokens for our test tooling token endpoint and userinfo ep to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting a at, idt and userinfo from the OP, it will use a
        // token ep and userinfo ep pointing to the test tooling app that will return the tokens previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }
    
    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken Userinfo"
     * caller claims exist in AT only
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientCallerClaims_group_claim_exist_in_AT_only() throws Exception {

        String appName = "atOnly";

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.IDToken_STR);

        // add or update claims (To remove claims you might need to replicate what createBuilderWithDefaultClaims does and just omit the setting of the claim)
        JWTTokenBuilder builder = createBuilderWithDefaultClaims();
        List<String> groups = new ArrayList<String>();
        groups.add("ATTestRole");
        builder.setClaim("role", groups); //group identifier=role
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the role printed in the app output", null, "groupIds=" + "[group:ATTestRealm/ATTestRole]");
        builder.setClaim("realmName", "ATTestRealm"); //realm identifier = realmName
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the role printed in the app output", null, "realmName=" + "ATTestRealm");
        // calling build to create a JWT token (as a string)
        String jwtToken = builder.build();
        // the built token will be passed to the test app via the overrideToken parm - it will be saved to be later returned during the auth process.
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken);
        builder = createBuilderWithDefaultClaims();
        String idt = builder.build();
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideIDToken", idt);
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);       
;
        List<endpointSettings> userinfParms = eSettings.addEndpointSettings(null, "userinfoToken", idt);
        //(That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations);
        // we created and saved a jwt for our test tooling token endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting at, idt and userinfo from the OP, it will use a
        // token endpoint and userinfo pointing to the test tooling apps that will return the tokens previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }

    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken Userinfo"
     * caller claim (upn) does not exist in any token
     * 
     * @throws Exception
     */   
    @Test
    public void OidcClientCallerClaims_useridentifier_claim_missing() throws Exception {

        String appName = "noClaim";

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.LOGIN_USER);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, com.ibm.ws.security.fat.common.Constants.UNAUTHORIZED_STATUS);

        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that client failed to authenticate the JSON Web Token", MessageConstants.CWWKS1738E_JWT_MISSING_CLAIM);
 
        JWTTokenBuilder builder = createBuilderWithDefaultClaims();
        builder.setClaim("role", "ATTestRole"); //group identifier=role
        builder.setClaim("realmName", "ATTestRealm"); //realm identifier = realmName

        String jwtToken = builder.build();

        // the built token will be passed to the test app via the overrideToken parm - it will be saved to be later returned during the auth process.
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtToken);
        builder = createBuilderWithDefaultClaims();
        String idt = builder.build();
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideIDToken", idt);
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);
  
        List<endpointSettings> userinfParms = eSettings.addEndpointSettings(null, "userinfoToken", idt);
        //(That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations);
        // we created and saved tokens for our test tooling token endpoint and userinfo endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting at, idt and userinfo from the OP, it will use a
        // token ep and userinfo ep pointing to the test tooling app that will return the tokens previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }

}
