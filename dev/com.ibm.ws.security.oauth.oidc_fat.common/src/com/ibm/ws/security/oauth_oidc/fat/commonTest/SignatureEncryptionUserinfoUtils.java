/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.util.List;

import org.junit.runner.RunWith;

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

    public List<validationData> setBasicSigningImplicitExpectations(String sigAlgForBuilder, String sigAlgForRP, TestSettings settings, String test_finalAction) throws Exception {
        return setBasicSigningExpectations(sigAlgForBuilder, sigAlgForRP, null, settings, test_finalAction, true);
    }

    public List<validationData> setBasicSigningExpectations(String sigAlgForBuilder, String sigAlgForRP, TestSettings settings, String test_finalAction) throws Exception {
        return setBasicSigningExpectations(sigAlgForBuilder, sigAlgForRP, null, settings, test_finalAction, false);
    }

    public List<validationData> setBasicSigningExpectations(String sigAlgForBuilder, String sigAlgForRP, TestSettings settings, String test_finalAction, boolean isImplicit) throws Exception {
        return setBasicSigningExpectations(sigAlgForBuilder, sigAlgForRP, null, settings, test_finalAction, isImplicit);
    }

    public List<validationData> setBasicSigningExpectations(String sigAlgForBuilder, String sigAlgForRP, List<String> allowedSigAlgs, TestSettings settings, String test_finalAction) throws Exception {
        return setBasicSigningExpectations(sigAlgForBuilder, sigAlgForRP, allowedSigAlgs, settings, test_finalAction, false);
    }

    public List<validationData> setBasicSigningExpectations(String sigAlgForBuilder, String sigAlgForRP, List<String> allowedSigAlgs, TestSettings settings, String test_finalAction, boolean isImplicit) throws Exception {
        
        List<validationData> expectations = null;
        // if the signature alg in the builder matches what's in the RP, the test should succeed - validate status codes and token content
        // if the sig alg for the rp is FROM_HEADER and allowed algs is null (no restrictions), the test should succeed
        // if the sig alg for the rp contains FROM_HEADER and the allowed algs contain the builder alg, the test should succeed
        if (sigAlgForBuilder.equals(sigAlgForRP) || (sigAlgForRP.contains("FROM_HEADER") && (allowedSigAlgs == null || allowedSigAlgs.contains(sigAlgForBuilder)))) {
            expectations = vData.addSuccessStatusCodes(null);
            if (test_finalAction.equals(Constants.LOGIN_USER)) {
                expectations = validationTools.addIdTokenStringValidation(vData, expectations, test_finalAction, Constants.RESPONSE_FULL, Constants.IDToken_STR);
                if (!isImplicit) {
                    expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), test_finalAction, settings);
                    expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), test_finalAction, settings);
                }
            } else {
                expectations = vData.addExpectation(expectations, test_finalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did NOT find the proper realm name containing \'TokenEndpointServlet\'.", null, "RealmName:.*TokenEndpointServlet");

            }
            expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
            expectations = validationTools.addRequestParmsExpectations(expectations, _testName, test_finalAction, settings);
        } else {
            // validate that we get the correct error message(s) for tests that use the same sig alg, but have mis-matched keys
            // If the allowed algs contain the builder alg as a partial substring match, then this is a key mismatch scenario
            if (sigAlgForBuilder.contains(sigAlgForRP) || allowedSigAlgsStringContainBuilder(allowedSigAlgs, sigAlgForBuilder)) {
                expectations = validationTools.add401Responses(test_finalAction);
                expectations = validationTools.addMessageExpectation(testRPServer, expectations, test_finalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1756E_OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR + ".*client01.*" + (sigAlgForRP.equals("FROM_HEADER") ? allowedSigAlgs : sigAlgForRP) + ".*");
            } else {
                // create negative expectations when signature algorithms don't match
                expectations = validationTools.add401Responses(test_finalAction);
                expectations = validationTools.addMessageExpectation(testRPServer, expectations, test_finalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Client messages.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1761E_SIG_ALG_MISMATCH + ".*client01.*" + (sigAlgForRP.equals("FROM_HEADER") ? allowedSigAlgs : sigAlgForRP) + ".*" + sigAlgForBuilder + ".*");
                expectations = validationTools.addMessageExpectation(testRPServer, expectations, test_finalAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Client messages.log should contain a message indicating that there is a signature mismatch", MessageConstants.CWWKS1706E_CLIENT_FAILED_TO_VALIDATE_ID_TOKEN);
            }
        }

        return expectations;
    }

    private boolean allowedSigAlgsStringContainBuilder(List<String> allowedSigAlgs, String sigAlgForBuilder) {
        if (allowedSigAlgs != null) {
            for (String allowedAlg : allowedSigAlgs) {
                if (allowedAlg.contains(sigAlgForBuilder)) {
                    return true;
                }
            }
        }
        return false;
    }

}
