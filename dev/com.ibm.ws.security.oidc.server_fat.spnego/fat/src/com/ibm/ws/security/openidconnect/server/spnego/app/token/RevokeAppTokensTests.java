/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.spnego.app.token;

import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego.SpnegoAppPasswordsAndTokensCommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.spnego.SpnegoOIDCConstants;
import com.ibm.ws.security.spnego.fat.config.InitClass;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Class for general app-tokens revoke testing. The extending class (the config it uses) dictates if the OP will be issuing
 * either an opaque access_token or a JWT access_token. This class will be extended by at least one class that uses access_tokens
 * and one that uses JWT access_tokens.
 *
 * The tests in this class rely on the app-tokens endpoint to create app tokens that can be revoked. The validation consists
 * of the following:
 * 1) when the test expects that the app-token should be revoked, try to access the protected resource using a revoked token
 * results in error
 * 2) when the test expects that the app-token shound NOT be revoked, ensure that the protected resource can be accessed
 *
 */
@RunWith(FATRunner.class)
public class RevokeAppTokensTests extends SpnegoAppPasswordsAndTokensCommonTest {

    /**
     * This test verifies that a user who is not tokenManager cannot revoke another user's app token by passing the other
     * user's user_id and app_id. The attempt to revoke fails with invalid_request and message: CWWKS1483E: The request sent to
     * URI /oidc/endpoint/OidcConfigSample/app-token is invalid. The user_id attribute is specified, but the authenticated
     * user does not have permission to use it. The diff user can still use the app-token to access the protected resource because
     * the token was not revoked.
     */
    @Mode(TestMode.LITE)
//    @Test
    public void test_SPNEGO_OIDC_RevokeAppTokensTests_userCannotRevokeOtherUsersAppTokenWithUserIdAppId_BadRequestNoRevoke() throws Exception {
    	TestSettings updatedTestSettings = addLocalhostToEndpoint(testSettings.copyTestSettings());
        String testuserToken = getAccessTokenWithWebClient(updatedTestSettings);
        
        updatedTestSettings = updateTestSettingsForAppTokensTests(testSettings, "genAppTok01", "secret1234", InitClass.SECOND_USER, InitClass.SECOND_USER_PWD, doNotOverrideProvider, doNotOverrideApp); 
        addLocalhostToEndpoint(updatedTestSettings);
        String diffuserToken = getAccessTokenWithWebClient(updatedTestSettings);
        TokenValues diffAppToken = createAppTokens(updatedTestSettings, diffuserToken, _testName + "2", ExpectSuccess, getGoodCreateAppTokensExpectations(updatedTestSettings));
        validateAppTokensCreateValues(diffAppToken, ninetyDays);

        revokeAppTokens(updatedTestSettings, testuserToken, InitClass.SECOND_USER, diffAppToken.getApp_id(), getNoPermissionForAttributeInRequestExpectations(Constants.INVOKE_APP_TOKENS_ENDPOINT_REVOKE));
    }


    /**
     * This test verifies that tokenManager can revoke single app token by passing access token, userId and appId on
     * request.
     *
     */
//    @Test
    public void test_SPNEGO_OIDC_RevokeAppTokensTests_tokenMgrSpecifiesUserIdWithTokenId_SuccessForSingleAppTokenRevoked() throws Exception {
    	
    	TestSettings updatedTestSettings = addLocalhostToEndpoint(testSettings.copyTestSettings());
        String userAccessToken = getAccessTokenWithWebClient(updatedTestSettings);
        
        TokenValues userAppToken = createAppTokens(updatedTestSettings, userAccessToken, _testName, ExpectSuccess, getGoodCreateAppTokensExpectations(testSettings));
        validateAppTokensCreateValues(userAppToken, ninetyDays);
         updatedTestSettings = updateTestSettingsForAppTokensTests(testSettings, "genAppTok01", "secret1234", InitClass.SECOND_USER, InitClass.SECOND_USER_PWD, doNotOverrideProvider, doNotOverrideApp);
         addLocalhostToEndpoint(updatedTestSettings);
        String tokenMgrToken = getAccessTokenWithWebClient(updatedTestSettings);
        
        revokeAppTokens(updatedTestSettings, tokenMgrToken, updatedTestSettings.getAdminUser(), userAppToken.getApp_id(), getGoodRevokeAppTokensExpectations(updatedTestSettings));


    }
    
    /**
     * This test verifies that tokenManager can revoke single app token by passing access token, userId and appId on
     * request.
     *
     */
    //@Test
    //@ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException" })
    public void test_SPNEGO_OIDC_Bad_Token_RevokeAppTokensTeststokenMgrSpecifiesUserIdWithTokenId_SuccessForSingleAppTokenRevoked() throws Exception {
    	
    	TestSettings updatedTestSettings = addLocalhostToEndpoint(testSettings.copyTestSettings());
    	
    	updatedTestSettings = updateTestSettingsForAppPasswordsTests(testSettings, "genAppPw01", "secret1234",
				SpnegoOIDCConstants.NTLM_TOKEN, SpnegoOIDCConstants.NTLM_TOKEN, Constants.OIDCCONFIGSAMPLE_APP, Constants.HELLOWORLD_SERVLET);

       getAccessTokenWithWebClient(updatedTestSettings);
        
    }


}
