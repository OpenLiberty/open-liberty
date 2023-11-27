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

import java.util.*;

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
        builder.setClaim("upn","testuser");
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
     * testing tokenOrderTofetchCallerClaims for group claim
     * caller group claims exist in specific tokens
     * 
     * @throws Exception
     */
    private void Generic_TokenOrder_Test_For_Group_Claims(String tokenOrderOption, List<String> tokensContainGroupClaim ) throws Exception {
        
        String appName = "";
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        if(Constants.TOKEN_ORDER_IDTOKEN.equalsIgnoreCase(tokenOrderOption)){
            appName = "idtokenonly";
            expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, com.ibm.ws.security.fat.common.Constants.OK_STATUS);
            expectations = validationTools.addIdTokenStringValidation(vData, expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.IDToken_STR);
            if(tokensContainGroupClaim.contains(Constants.TOKEN_TYPE_IDTOKEN)
            ){
                expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the role printed in the app output", null, "groupIds=" + "[group:MyTestRealm/MyTestRole]");
                expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the realmName printed in the app output", null, "realmName=" + "MyTestRealm");
            }else{
                expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Did not see the role printed in the app output", null, "groupIds=" + "[group:MyTestRealm/MyTestRole]");
                expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Did not see the realmName printed in the app output", null, "realmName=" + "MyTestRealm");
            }


        }else if(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO.equalsIgnoreCase(tokenOrderOption)){
            appName = "sampleBuilder";
            expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, com.ibm.ws.security.fat.common.Constants.OK_STATUS);
            expectations = validationTools.addIdTokenStringValidation(vData, expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.IDToken_STR);
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the role printed in the app output", null, "groupIds=" + "[group:MyTestRealm/MyTestRole]");
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the realmName printed in the app output", null, "realmName=" + "MyTestRealm");

        } 

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));



        // add or update claims (To remove claims you might need to replicate what createBuilderWithDefaultClaims does and just omit the setting of the claim)
        JWTTokenBuilder builder = createBuilderWithDefaultClaims();
        builder.unsetClaim("realmName"); // realmIdentifier = realmName
        String noRoleClaimToken = builder.build();
  
        builder.setClaim("role", Arrays.asList("MyTestRole")); //group identifier=role
        builder.setClaim("realmName", "MyTestRealm"); //realm identifier = realmName
        // calling build to create a JWT token (as a string)
        String withRoleClaimToken = builder.build();

        // the built token will be passed to the test app via the overrideToken parm - it will be saved to be later returned during the auth process.
        List<endpointSettings> parms = null;
        List<endpointSettings> userinfoParms = null;

        Set<String> tokeyTypeSet = new HashSet<String>();
        for(String tokenType : tokensContainGroupClaim){
            if(Constants.TOKEN_TYPE_ACCESSTOKEN.equalsIgnoreCase(tokenType)){
                parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideToken", withRoleClaimToken);
                tokeyTypeSet.add("overrideToken");
            }else if(Constants.TOKEN_TYPE_IDTOKEN.equalsIgnoreCase(tokenType)){
                parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideIDToken", withRoleClaimToken);
                tokeyTypeSet.add("overrideIDToken");
            }if(Constants.TOKEN_TYPE_USERINFO.equalsIgnoreCase(tokenType)){
                userinfoParms = eSettings.addEndpointSettingsIfNotNull(userinfoParms, "userinfoToken", withRoleClaimToken);
                tokeyTypeSet.add("userinfoToken");
            }
        }

        if(!tokeyTypeSet.contains("overrideToken")){
            parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideToken", noRoleClaimToken);
        }

        if(!tokeyTypeSet.contains("overrideIDToken")){
            parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideIDToken", noRoleClaimToken);
        }

        if(!tokeyTypeSet.contains("userinfoToken")){
            userinfoParms = eSettings.addEndpointSettingsIfNotNull(userinfoParms, "userinfoToken", noRoleClaimToken);
        }

        // overwrite the ID Token or Access Token
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);       
        
        // overwrite user info token
        //(That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfoParms, null, expectations);
        // we created and saved a jwt for our test tooling token endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting at, idt and userinfo from the OP, it will use a
        // token endpoint and userinfo pointing to the test tooling apps that will return the tokens previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }


    /******************************* tests *******************************/
    /**
     * testing tokenOrderTofetchCallerClaims for user claim
     * caller user claims exist in specific tokens
     *
     * @throws Exception
     */
    private void Generic_TokenOrder_Test_For_User_Claims(String tokenOrderOption, List<String> tokensContainUserClaim ) throws Exception {

        String appName = "";
        List<validationData> expectations = null;
        if(Constants.TOKEN_ORDER_IDTOKEN.equalsIgnoreCase(tokenOrderOption)){
            appName = "idtokenonly";
            if(tokensContainUserClaim.contains(Constants.TOKEN_TYPE_IDTOKEN)){
                expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, com.ibm.ws.security.fat.common.Constants.OK_STATUS);
            }else{
                expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, com.ibm.ws.security.fat.common.Constants.UNAUTHORIZED_STATUS);
                expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that client failed to authenticate the JSON Web Token", MessageConstants.CWWKS1738E_JWT_MISSING_CLAIM);
            }


        }else if(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO.equalsIgnoreCase(tokenOrderOption)){
            appName = "sampleBuilder";
            expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, com.ibm.ws.security.fat.common.Constants.OK_STATUS);
        }

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));



        // add or update claims (To remove claims you might need to replicate what createBuilderWithDefaultClaims does and just omit the setting of the claim)
        JWTTokenBuilder builder = createBuilderWithDefaultClaims();
        String withUserClaimToken = builder.build();
        builder.unsetClaim("upn"); // userIdentifier = upn
        String noUserClaimToken = builder.build();


        // the built token will be passed to the test app via the overrideToken parm - it will be saved to be later returned during the auth process.
        List<endpointSettings> parms = null;
        List<endpointSettings> userinfoParms = null;

        Set<String> tokeyTypeSet = new HashSet<String>();
        for(String tokenType : tokensContainUserClaim){
            if(Constants.TOKEN_TYPE_ACCESSTOKEN.equalsIgnoreCase(tokenType)){
                parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideToken", withUserClaimToken);
                tokeyTypeSet.add("overrideToken");
            }else if(Constants.TOKEN_TYPE_IDTOKEN.equalsIgnoreCase(tokenType)){
                parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideIDToken", withUserClaimToken);
                tokeyTypeSet.add("overrideIDToken");
            }if(Constants.TOKEN_TYPE_USERINFO.equalsIgnoreCase(tokenType)){
                userinfoParms = eSettings.addEndpointSettingsIfNotNull(userinfoParms, "userinfoToken", withUserClaimToken);
                tokeyTypeSet.add("userinfoToken");
            }
        }

        if(!tokeyTypeSet.contains("overrideToken")){
            parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideToken", noUserClaimToken);
        }

        if(!tokeyTypeSet.contains("overrideIDToken")){
            parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideIDToken", noUserClaimToken);
        }

        if(!tokeyTypeSet.contains("userinfoToken")){
            userinfoParms = eSettings.addEndpointSettingsIfNotNull(userinfoParms, "userinfoToken", noUserClaimToken);
        }

        // overwrite the ID Token or Access Token
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);

        // overwrite user info token
        //(That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfoParms, null, expectations);
        // we created and saved a jwt for our test tooling token endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting at, idt and userinfo from the OP, it will use a
        // token endpoint and userinfo pointing to the test tooling apps that will return the tokens previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }

    /******************************* tests *******************************/
    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken Userinfo"
     * caller group claim exist in access token only
     * 
     * @throws Exception
     */
    @Test
    public void AllTokens_group_claim_exist_in_accesstoken_only() throws Exception {
        Generic_TokenOrder_Test_For_Group_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO, Arrays.asList(Constants.TOKEN_TYPE_ACCESSTOKEN));
    }

    /******************************* tests *******************************/
    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken UserInfo"
     * caller group claim exist in id token only
     * 
     * @throws Exception
     */
    @Test
    public void AllTokens_group_claim_exist_in_idtoken_only() throws Exception {
        Generic_TokenOrder_Test_For_Group_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO, 
                                                 Arrays.asList(Constants.TOKEN_TYPE_IDTOKEN));
    } 
    
    /******************************* tests *******************************/
    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken UserInfo"
     * caller group claim exist in userinfo only
     * 
     * @throws Exception
     */
    @Test
    public void AllTokens_group_claim_exist_in_userinfo_only() throws Exception {
        Generic_TokenOrder_Test_For_Group_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO, 
                                                Arrays.asList(Constants.TOKEN_TYPE_USERINFO));
    }   
    
    /******************************* tests *******************************/
    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken UserInfo"
     * caller group claim exist in idtoken and userinfo
     * 
     * @throws Exception
     */
    @Test
    public void AllTokens_group_claim_exist_in_idtoken_and_userinfo() throws Exception {
        Generic_TokenOrder_Test_For_Group_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO, 
                                                 Arrays.asList(Constants.TOKEN_TYPE_IDTOKEN,
                                                               Constants.TOKEN_TYPE_USERINFO));
    }

    /******************************* tests *******************************/
    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken UserInfo"
     * caller group claim exist in Id Token and Acess Token
     *
     * @throws Exception
     */
    @Test
    public void AllTokens_group_claim_exist_in_idtoken_and_accesstoken() throws Exception {
        Generic_TokenOrder_Test_For_Group_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO,
                Arrays.asList(Constants.TOKEN_TYPE_IDTOKEN,
                              Constants.TOKEN_TYPE_ACCESSTOKEN));
    }

    /******************************* tests *******************************/
    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken UserInfo"
     * caller group claim exist in AccessToken and User Info only
     *
     * @throws Exception
     */
    @Test
    public void AllTokens_group_claim_exist_in_accesstoken_and_userinfo() throws Exception {
        Generic_TokenOrder_Test_For_Group_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO,
                Arrays.asList(Constants.TOKEN_TYPE_ACCESSTOKEN,
                              Constants.TOKEN_TYPE_USERINFO));
    }


    /******************************* tests *******************************/
    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken UserInfo"
     * caller group claim exist in all tokens
     *
     * @throws Exception
     */
    @Test
    public void AllTokens_group_claim_exist_in_all_three_tokens() throws Exception {
        Generic_TokenOrder_Test_For_Group_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO,
                Arrays.asList(Constants.TOKEN_TYPE_IDTOKEN,
                        Constants.TOKEN_TYPE_ACCESSTOKEN,
                        Constants.TOKEN_TYPE_USERINFO));
    }


    /******************************* tests *******************************/
    /**
     * testing tokenOrderTofetchCallerClaims="IDToken"
     * caller group claim exist in all tokens
     *
     * @throws Exception
     */
    @Test
    public void IDToken_group_claim_exist_in_all_three_tokens() throws Exception {
        Generic_TokenOrder_Test_For_Group_Claims(Constants.TOKEN_ORDER_IDTOKEN,
                Arrays.asList(Constants.TOKEN_TYPE_IDTOKEN,
                        Constants.TOKEN_TYPE_ACCESSTOKEN,
                        Constants.TOKEN_TYPE_USERINFO));
    }


    /**
     * testing no config for tokenOrderTofetchCallerClaims, ie tokenOrderTofetchCallerClaims="IDToken"
     * caller claims exist in access token only
     * 
     * @throws Exception
     */
    @Test
    public void IDToken_group_claim_exist_in_accesstoken_only() throws Exception {
        Generic_TokenOrder_Test_For_Group_Claims(Constants.TOKEN_ORDER_IDTOKEN, 
                                                 Arrays.asList(Constants.TOKEN_TYPE_ACCESSTOKEN));
 
    }    


    /**
     * testing no config for tokenOrderTofetchCallerClaims, ie tokenOrderTofetchCallerClaims="IDToken"
     * caller claims exist in ID Token only
     * 
     * @throws Exception
     */
    @Test
    public void IDToken_group_claim_exist_in_idtoken_only() throws Exception {

         Generic_TokenOrder_Test_For_Group_Claims(Constants.TOKEN_ORDER_IDTOKEN, 
                                                 Arrays.asList(Constants.TOKEN_TYPE_IDTOKEN));
    }   
    
    
    /**
     * testing no config for tokenOrderTofetchCallerClaims, ie tokenOrderTofetchCallerClaims="IDToken"
     * caller claims exist in userinfo only
     * 
     * @throws Exception
     */
    @Test
    public void IDToken_group_claim_exist_in_userinfo_only() throws Exception {

         Generic_TokenOrder_Test_For_Group_Claims(Constants.TOKEN_ORDER_IDTOKEN, 
                                                 Arrays.asList(Constants.TOKEN_TYPE_USERINFO));
    }

    /**
     * testing tokenOrderTofetchCallerClaims="IDToken"
     * caller user claims exist in userinfo only
     *
     * @throws Exception
     */
    @Test
    public void IDToken_user_claim_exist_in_userinfo_only() throws Exception {

        Generic_TokenOrder_Test_For_User_Claims(Constants.TOKEN_ORDER_IDTOKEN,
                Arrays.asList(Constants.TOKEN_TYPE_USERINFO));
    }

    /**
     * testing tokenOrderTofetchCallerClaims="IDToken"
     * caller user claims exist in access token only
     *
     * @throws Exception
     */
    @Test
    public void IDToken_user_claim_exist_in_accesstoken_only() throws Exception {

        Generic_TokenOrder_Test_For_User_Claims(Constants.TOKEN_ORDER_IDTOKEN,
                Arrays.asList(Constants.TOKEN_TYPE_ACCESSTOKEN));
    }


    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken Userinfo"
     * caller user claims exist in id token only
     *
     * @throws Exception
     */
    @Test
    public void AllTokens_user_claim_exist_in_idtoken_only() throws Exception {

        Generic_TokenOrder_Test_For_User_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO,
                Arrays.asList(Constants.TOKEN_TYPE_IDTOKEN));
    }

    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken Userinfo"
     * caller user claims exist in access token and user info only
     *
     * @throws Exception
     */
    @Test
    public void ALLTokens_user_claim_exist_in_accesstoken_and_userinfo_only() throws Exception {

        Generic_TokenOrder_Test_For_User_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO,
                Arrays.asList(Constants.TOKEN_TYPE_ACCESSTOKEN, Constants.TOKEN_TYPE_USERINFO));
    }

    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken Userinfo"
     * caller user claims exist in ID token and user info only
     *
     * @throws Exception
     */
    @Test
    public void ALLTokens_user_claim_exist_in_idtoken_and_userinfo_only() throws Exception {

        Generic_TokenOrder_Test_For_User_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO,
                Arrays.asList(Constants.TOKEN_TYPE_IDTOKEN, Constants.TOKEN_TYPE_USERINFO));
    }

    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken Userinfo"
     * caller user claims exist in userinfo only
     *
     * @throws Exception
     */
    @Test
    public void AllTokens_user_claim_exist_in_userinfo_only() throws Exception {

        Generic_TokenOrder_Test_For_User_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO,
                Arrays.asList(Constants.TOKEN_TYPE_USERINFO));
    }

    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken Userinfo"
     * caller user claims exist in access token only
     *
     * @throws Exception
     */
    @Test
    public void AllTokens_user_claim_exist_in_accesstoken_only() throws Exception {

        Generic_TokenOrder_Test_For_User_Claims(Constants.TOKEN_ORDER_ACCESSTOKEN_IDTOKEN_USERINFO,
                Arrays.asList(Constants.TOKEN_TYPE_ACCESSTOKEN));
    }


    /**
     * testing no config for tokenOrderTofetchCallerClaims, ie tokenOrderTofetchCallerClaims="IDToken"
     * caller user claims exist in id token only
     *
     * @throws Exception
     */
    @Test
    public void IDToken_user_claim_exist_in_idtoken_only() throws Exception {

        Generic_TokenOrder_Test_For_User_Claims(Constants.TOKEN_ORDER_IDTOKEN,
                Arrays.asList(Constants.TOKEN_TYPE_IDTOKEN));
    }

    /**
     * testing no config for tokenOrderTofetchCallerClaims, ie tokenOrderTofetchCallerClaims="IDToken"
     * caller user claims exist in access token and user info only
     *
     * @throws Exception
     */
    @Test
    public void IDToken_user_claim_exist_in_accesstoken_and_userinfo_only() throws Exception {

        Generic_TokenOrder_Test_For_User_Claims(Constants.TOKEN_ORDER_IDTOKEN,
                Arrays.asList(Constants.TOKEN_TYPE_ACCESSTOKEN, Constants.TOKEN_TYPE_USERINFO));
    }

    /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken Userinfo"
     * caller claim (upn) does not exist in any token
     * 
     * @throws Exception
     */   
    @Test
    public void AllTokens_user_claim_missing() throws Exception {

        String appName = "sampleBuilder";

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.LOGIN_USER);
       
        expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, com.ibm.ws.security.fat.common.Constants.UNAUTHORIZED_STATUS);

        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that client failed to authenticate the JSON Web Token", MessageConstants.CWWKS1738E_JWT_MISSING_CLAIM);
 
        JWTTokenBuilder builder = createBuilderWithDefaultClaims();
        builder.unsetClaim("upn"); //unset userIdentifier = upn
        String jwtTokenWithNoUserClaim = builder.build();

        // the built token will be passed to the test app via the overrideToken parm - it will be saved to be later returned during the auth process.
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtTokenWithNoUserClaim);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideIDToken", jwtTokenWithNoUserClaim);
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);
  
        List<endpointSettings> userinfParms = eSettings.addEndpointSettingsIfNotNull(null, "userinfoToken", jwtTokenWithNoUserClaim);
        //(That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations);
        // we created and saved tokens for our test tooling token endpoint and userinfo endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting at, idt and userinfo from the OP, it will use a
        // token ep and userinfo ep pointing to the test tooling app that will return the tokens previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }


    /**
     * testing tokenOrderTofetchCallerClaims="IDToken"
     * caller claim (upn) does not exist in any token
     * 
     * @throws Exception
     */   
    @Test
    public void IDTokens_user_claim_missing() throws Exception {

        String appName = "idtokenonly";

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.LOGIN_USER);
       
        expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, com.ibm.ws.security.fat.common.Constants.UNAUTHORIZED_STATUS);

        expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that client failed to authenticate the JSON Web Token", MessageConstants.CWWKS1738E_JWT_MISSING_CLAIM);
 
        JWTTokenBuilder builder = createBuilderWithDefaultClaims();
        builder.unsetClaim("upn"); //unset userIdentifier = upn
        String jwtTokenWithNoUserClaim = builder.build();

        // the built token will be passed to the test app via the overrideToken parm - it will be saved to be later returned during the auth process.
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", jwtTokenWithNoUserClaim);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideIDToken", jwtTokenWithNoUserClaim);
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);
  
        List<endpointSettings> userinfParms = eSettings.addEndpointSettingsIfNotNull(null, "userinfoToken", jwtTokenWithNoUserClaim);
        //(That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations);
        // we created and saved tokens for our test tooling token endpoint and userinfo endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting at, idt and userinfo from the OP, it will use a
        // token ep and userinfo ep pointing to the test tooling app that will return the tokens previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }

   /**
     * testing tokenOrderTofetchCallerClaims="Access Token IDToken UserInfo"
     * caller claim (upn) exists in all tokens, but has a different value in each token
     * 
     * @throws Exception
     */   
    @Test
    public void AllTokens_good_claims_in_accesstoken_but_different_values_in_other_tokens() throws Exception {

        String appName = "sampleBuilder";

        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testSettings.getTestURL().replace("SimpleServlet", "simple/" + appName));

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.LOGIN_USER);
       
        expectations = vData.addResponseStatusExpectation(expectations, Constants.LOGIN_USER, com.ibm.ws.security.fat.common.Constants.OK_STATUS);

        JWTTokenBuilder builder = createBuilderWithDefaultClaims();
        builder.setClaim("upn", "userInAccessToken");
        builder.setClaim("role", Arrays.asList("MyTestRoleInAccessToken")); //group identifier=role
        builder.setClaim("realmName", "RealmInAccessToken"); //realm identifier = realmName

        String accessToken = builder.build();
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the role printed in the app output", null, "groupIds=" + "[group:RealmInAccessToken/MyTestRoleInAccessToken]");
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the realmName printed in the app output", null, "realmName=" + "RealmInAccessToken");
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the user name printed in the app output", null, "getUserPrincipal: WSPrincipal:userInAccessToken");
       

        builder.setClaim("upn","otheruserInIdtoken");
        builder.setClaim("role", Arrays.asList("MyRoleInIDToken")); //group identifier=role
        builder.setClaim("realmName", "RealmInIDToken"); //realm identifier = realmName

        String idToken = builder.build();

        builder.setClaim("upn","otheruserInUserInfo");
        builder.setClaim("role", Arrays.asList("MyRoleInUserInfo")); //group identifier=role    
        builder.setClaim("realmName", "RealmInUserInfo"); //realm identifier = realmName   
        String userInfo = builder.build(); 

        // the built token will be passed to the test app via the overrideToken parm - it will be saved to be later returned during the auth process.
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "overrideToken", accessToken);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "overrideIDToken", idToken);
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, "misc", parms, null, expectations);
  
        List<endpointSettings> userinfParms = eSettings.addEndpointSettingsIfNotNull(null, "userinfoToken", userInfo);
        //(That url that the RP will call is:  http://localhost:${bvt.prop.security_1_HTTP_default}/UserinfoEndpointServlet/getJWT)
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", userinfParms, null, expectations);
        // we created and saved tokens for our test tooling token endpoint and userinfo endpoint to return to the RP - let's invoke
        // the protected resource.  The RP will get the auth token, but, instead of getting at, idt and userinfo from the OP, it will use a
        // token ep and userinfo ep pointing to the test tooling app that will return the tokens previously obtained using a builder
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

    }

}
