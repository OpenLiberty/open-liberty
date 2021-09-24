/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.utils;

import java.util.List;

import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that provides test tooling for the signature Algorithm, Encryption
 * and userinfo tests.
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SignatureEncryptionUserinfoUtils extends CommonTest {

    public static Class<?> thisClass = SignatureEncryptionUserinfoUtils.class;

    public List<validationData> setBasicSigningImplicitExpectations(String sigAlgForBuilder, String sigAlgForRP, TestSettings settings) throws Exception {
        return setBasicSigningExpectations(sigAlgForBuilder, sigAlgForRP, settings, true);
    }

    public List<validationData> setBasicSigningExpectations(String sigAlgForBuilder, String sigAlgForRP, TestSettings settings) throws Exception {
        return setBasicSigningExpectations(sigAlgForBuilder, sigAlgForRP, settings, false);
    }

    public List<validationData> setBasicSigningExpectations(String sigAlgForBuilder, String sigAlgForRP, TestSettings settings, boolean isImplicit) throws Exception {

        String test_FinalAction = Constants.LOGIN_USER;

        List<validationData> expectations = null;
        // if the signature alg in the build matches what's in the RP, the test should succeed - validate status codes and token content
        if (sigAlgForBuilder.equals(sigAlgForRP)) {
            expectations = vData.addSuccessStatusCodes(null);
            expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_FinalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
            expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                    "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
            expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_FinalAction, settings);
            if (!isImplicit) {
                expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, settings);
            }
            expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_FinalAction, settings);
        } else {
            // validate that we get the correct error message(s) for tests that use the same sig alg, but have mis-matched keys
            if (sigAlgForBuilder.contains(sigAlgForRP)) {
                expectations = validationTools.add401Responses(Constants.LOGIN_USER);
                expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES,
                        "Client messages.log should contain a message indicating that there is a signature mismatch",
                        MessageConstants.CWWKS1756E_OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR + ".*client01.*" + sigAlgForRP + ".*");
            } else {
                // create negative expectations when signature algorithms don't match
                expectations = validationTools.add401Responses(Constants.LOGIN_USER);
                expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_MATCHES,
                        "Client messages.log should contain a message indicating that there is a signature mismatch",
                        MessageConstants.CWWKS1761E_SIG_ALG_MISMATCH + ".*client01.*" + sigAlgForRP + ".*" + sigAlgForBuilder + ".*");
                expectations = validationTools.addMessageExpectation(testRPServer, expectations, Constants.LOGIN_USER, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                        "Client messages.log should contain a message indicating that there is a signature mismatch",
                        MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
            }
        }

        return expectations;
    }

}
