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
package com.ibm.ws.security.openidconnect.server.spnego.app.password;

import org.junit.Test;
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
 * Class for general app-password revoke testing. The extending class (the config it uses) dictates if the OP will be issuing
 * either an access_token or a JWT access_token. This class will be extended by at least one class that uses access_tokens and one
 * that uses JWT access_tokens.
 *
 * The tests in this class rely on the app-password endpoint to create app passwords that can be revoked. The validation consists
 * of the following:
 * 1) when the test expects that the app-password should be revoked, try to create a new access token from the revoked
 * app-password and expect failure since the app-password has been revoked.
 * 2) when the test expects that the app-password shound NOT be revoked, ensure that a new access token can be
 * requested from the app-password.
 * 3) some tests also invoke a protected resource on the RS with an access token obtained from the app-password endpoint.
 *
 */
@RunWith(FATRunner.class)
public class RevokeAppPasswordsTests extends SpnegoAppPasswordsAndTokensCommonTest {

    /**
     * This test verifies that a user who is not tokenManager cannot revoke another user's app password by passing the other
     * user's user_id and app_id. The attempt to revoke fails with invalid_request and message: CWWKS1483E: The request sent to
     * URI /oidc/endpoint/OidcConfigSample/app-password is invalid. The user_id attribute is specified, but the authenticated
     * user does not have permission to use it. The other user can still use the app_password to generate a token because it was
     * not revoked.
     */
   @Mode(TestMode.LITE)
   @Test
    public void test_SPNEGO_OIDC_RevokeAppPasswordsTests_userCannotRevokeOtherUsersAppPwdWithUserIdAppId_BadRequestNoRevoke() throws Exception {
		if (InitClass.isRndHostName) {
    		return;
        }
	   TestSettings updatedTestSettings = addLocalhostToEndpoint(testSettings.copyTestSettings());
        String testuserToken = getAccessTokenWithWebClient(updatedTestSettings);
        TokenValues userAppPwd = createAppPasswordswebClient(updatedTestSettings, testuserToken, _testName + "1", ExpectSuccess, getGoodCreateAppPasswordsExpectations(updatedTestSettings));
        validateAppPasswordsCreateValues(userAppPwd, ninetyDays);

        updatedTestSettings = updateTestSettingsForAppPasswordsTests(updatedTestSettings, "genAppPw01", "secret1234", "diffuser", "diffuserpwd", Constants.OIDCCONFIGSAMPLE_APP, Constants.HELLOWORLD_SERVLET);
        updatedTestSettings=addLocalhostToEndpoint(updatedTestSettings);
        
        String diffuserToken = getAccessTokenWithWebClient(updatedTestSettings);
        TokenValues diffAppPwd = createAppPasswordswebClient(updatedTestSettings, diffuserToken, _testName + "2", ExpectSuccess, getGoodCreateAppPasswordsExpectations(updatedTestSettings));
        validateAppPasswordsCreateValues(diffAppPwd, ninetyDays);

        revokeAppPasswords(updatedTestSettings, testuserToken, "diffuser", diffAppPwd.getApp_id(), getNoPermissionForAttributeInRequestExpectations(Constants.INVOKE_APP_PASSWORDS_ENDPOINT_REVOKE));
    }

    /**
     * This test verifies that a user in tokenManager role can invoke the app_password endpoint with revoke and specify the userId
     * for which the app_password should be revoked. Once the app_password is revoked, it cannot be used to obtain a new
     * access token.
     *
     */
    @Test
    public void test_SPNEGO_OIDC_RevokeAppPasswordsTests_tokenMgrSpecifiesSingleUserIdNoAppId_SuccessForSingleUserAppPwdRevoked() throws Exception {
    	if (InitClass.isRndHostName) {
    		return;
        }
    	TestSettings updatedTestSettings = addLocalhostToEndpoint(testSettings.copyTestSettings());

		String userAccessToken = getAccessTokenWithWebClient(updatedTestSettings);
		TokenValues userAp = createAppPasswordswebClient(updatedTestSettings, userAccessToken, _testName, ExpectSuccess,
				getGoodCreateAppPasswordsExpectations(updatedTestSettings));
		validateAppPasswordsCreateValues(userAp, ninetyDays);

		updatedTestSettings = updateTestSettingsForAppPasswordsTests(testSettings, "genAppPw01", "secret1234",
				InitClass.SECOND_USER, InitClass.SECOND_USER_PWD, Constants.OIDCCONFIGSAMPLE_APP, Constants.HELLOWORLD_SERVLET);
		updatedTestSettings=addLocalhostToEndpoint(updatedTestSettings);

		String tokenMgrToken = getAccessTokenWithWebClient(updatedTestSettings);
		revokeAppPasswords(updatedTestSettings, tokenMgrToken, updatedTestSettings.getAdminUser(), appIdNotSetOrUnknown,
				getGoodRevokeAppPasswordsExpectations(updatedTestSettings));
	}
    
    /**
     * This test verifies that a user in tokenManager role can invoke the app_password endpoint with revoke and specify the userId
     * for which the app_password should be revoked. Once the app_password is revoked, it cannot be used to obtain a new
     * access token.
     * 
     * The test will fail because the SPNEGO token that was sent is a invalid one. 
     *
     */
//    @Test
//    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException" })
    public void test_SPNEGO_OIDC_Bad_Token_RevokeAppPasswordsTests_tokenMgrSpecifiesSingleUserIdNoAppId_SuccessForSingleUserAppPwdRevoked() throws Exception {
    	if (InitClass.isRndHostName) {
    		return;
        }
    	TestSettings updatedTestSettings = addLocalhostToEndpoint(testSettings.copyTestSettings());
		
		updatedTestSettings = updateTestSettingsForAppPasswordsTests(testSettings, "genAppPw01", "secret1234",
				SpnegoOIDCConstants.NTLM_TOKEN, SpnegoOIDCConstants.NTLM_TOKEN, Constants.OIDCCONFIGSAMPLE_APP, Constants.HELLOWORLD_SERVLET);

		getAccessTokenWithWebClient(updatedTestSettings);
		
	}

}
