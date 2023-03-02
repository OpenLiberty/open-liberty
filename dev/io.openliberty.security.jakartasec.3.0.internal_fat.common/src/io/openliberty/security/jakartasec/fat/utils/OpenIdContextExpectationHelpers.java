/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.security.jakartasec.fat.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;

import jakarta.security.enterprise.authentication.mechanism.http.openid.OpenIdConstant;

public class OpenIdContextExpectationHelpers {

    public static void getOpenIdContextExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        if (rspValues.getBaseApp().equals(Constants.DEFAULT_SERVLET)) {
            return;
        }
        // TODO
        String updatedRequester = requester + ServletMessageConstants.OPENID_CONTEXT;
        getOpenIdContextSubjectExpectations(action, expectations, updatedRequester, rspValues, false);
        getOpenIdContextAccessTokenExpectations(action, expectations, updatedRequester, rspValues);
        getOpenIdContextIdTokenExpectations(action, expectations, updatedRequester, rspValues, false);
        getOpenIdContextIssuerExpectations(action, expectations, updatedRequester, rspValues);
        getOpenIdContextTokenTypeExpectations(action, expectations, updatedRequester, rspValues);
        getOpenIdContextStoredValueExpectations(action, expectations, updatedRequester, rspValues, false);

    }

    public static void getOpenIdContextFromRefreshedTokenExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        // TODO
        String updatedRequester = requester + ServletMessageConstants.OPENID_CONTEXT;
        getOpenIdContextSubjectExpectations(action, expectations, updatedRequester, rspValues, true);
        getOpenIdContextAccessTokenExpectations(action, expectations, updatedRequester, rspValues);
        getOpenIdContextIdTokenExpectations(action, expectations, updatedRequester, rspValues, true);
//        getOpenIdContextIssuerExpectations(action, expectations, updatedRequester, rspValues);
        getOpenIdContextTokenTypeExpectations(action, expectations, updatedRequester, rspValues);
        getOpenIdContextStoredValueExpectations(action, expectations, updatedRequester, rspValues, true);

    }

    public static void getOpenIdContextSubjectExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues,
                                                           boolean refreshedToken) throws Exception {

        expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.CONTEXT_SUBJECT
                                                                                                   + rspValues.getSubject(), "Did not find the correct subject in the OpenIdContext."));
        if (rspValues.getSubject() != null && !refreshedToken) {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.ID_TOKEN + ServletMessageConstants.CLAIM
                                                                                                       + ServletMessageConstants.KEY + PayloadConstants.PAYLOAD_SUBJECT + " "
                                                                                                       + ServletMessageConstants.VALUE
                                                                                                       + rspValues.getSubject(), "Did not find the correct subject in the claim."));
        } else {
            if (rspValues.getSubject() == null) {
                expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester
                                                                                                           + ServletMessageConstants.NULL_CLAIMS, "Claims were not null"));
            } else {
                expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester
                                                                                                           + ServletMessageConstants.CLAIMS_SUBJECT, rspValues.getSubject()));
            }
        }
        // TODO enable once claims are workingexpectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.SUBS_MATCH, "Did not find the correct subject in the OpenIdContext."));

    }

    public static void getOpenIdContextAccessTokenExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        if (rspValues.getSubject() != null) {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_DOES_NOT_CONTAIN, requester + ServletMessageConstants.ACCESS_TOKEN
                                                                                                               + ServletMessageConstants.NULL, "Did not find an access_token in the OpenIdContext."));
        } else {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.ACCESS_TOKEN
                                                                                                       + ServletMessageConstants.NULL, "Found an access_token in the OpenIdContext and should NOT have."));

        }

    }

    public static void getOpenIdContextIdTokenExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues,
                                                           boolean refreshedToken) throws Exception {

        if (rspValues.getSubject() != null && !refreshedToken) {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_DOES_NOT_CONTAIN, requester + ServletMessageConstants.ID_TOKEN
                                                                                                               + ServletMessageConstants.NULL, "Did not find an id token in the OpenIdContext."));
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.ID_TOKEN + ServletMessageConstants.CLAIM
                                                                                                       + ServletMessageConstants.KEY
                                                                                                       + PayloadConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS, "Did not find an exp claim in the id token in the OpenIdContext."));
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, buildIssuedAtTimeString(requester), "Did not find an iat claim in the id token in the OpenIdContext."));
            if (rspValues.getUseNonce()) {
                expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, buildNonceString(requester), "Did not find an nonce claim in the id token in the OpenIdContext."));
            }
            // TODO - remove sid check - will go away once the beta flag is removed
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.ID_TOKEN + ServletMessageConstants.CLAIM
                                                                                                       + ServletMessageConstants.KEY
                                                                                                       + PayloadConstants.PAYLOAD_SESSION_ID, "Did not find an sid claim in the id token in the OpenIdContext."));
        } else {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.ID_TOKEN
                                                                                                       + ServletMessageConstants.NULL, "Found an id token in the OpenIdContext and should not have."));

        }
        // issuer checked elsewhwere
    }

    public static void getOpenIdContextIssuerExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        if (rspValues.getIssuer() == null) {
            return;
        }
        expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.ID_TOKEN + ServletMessageConstants.CLAIM
                                                                                                   + ServletMessageConstants.KEY
                                                                                                   + PayloadConstants.PAYLOAD_ISSUER, "Did not find an issuer claim in the id token in the OpenIdContext."));
        expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.ID_TOKEN + ServletMessageConstants.CLAIM
                                                                                                   + ServletMessageConstants.KEY + PayloadConstants.PAYLOAD_ISSUER
                                                                                                   + " " + ServletMessageConstants.VALUE
                                                                                                   + rspValues.getIssuer(), "Did not find the correct value for the issuer claim in the id token in the OpenIdContext."));

    }

    public static void getOpenIdContextTokenTypeExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues) throws Exception {

        if (rspValues.getTokenType() != null) {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.TOKEN_TYPE
                                                                                                       + Constants.TOKEN_TYPE_BEARER, "Did not find the token_type set to Bearer in the OpenIdContext."));
        } else {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_CONTAINS, requester + ServletMessageConstants.TOKEN_TYPE
                                                                                                       + ServletMessageConstants.NULL, "Found a token_type in the OpenIdContext and should not have."));
        }

    }

    public static void getOpenIdContextStoredValueExpectations(String action, Expectations expectations, String requester, ResponseValues rspValues,
                                                               boolean refreshedToken) throws Exception {

        List<NameValuePair> parms = rspValues.getParms();

        if (rspValues.getOriginalRequest() != null && rspValues.getOriginalRequest().contains(ServletMessageConstants.UNAUTH_SESSION_REQUEST_EXCEPTION)) {
            expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_MATCHES, requester + ServletMessageConstants.STORED_VALUE
                                                                                                      + ".*"
                                                                                                      + rspValues.getOriginalRequest()
                                                                                                      + ".*", "Did not find the original request in the Stored Value in the OpenIdContext."));
        } else {
            if (!refreshedToken) {
                if (parms != null) {
                    for (NameValuePair parm : parms) {
                        expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_MATCHES, requester + ServletMessageConstants.STORED_VALUE
                                                                                                                  + OpenIdConstant.ORIGINAL_REQUEST
                                                                                                                  + ".*" + rspValues.getOriginalRequest()
                                                                                                                  + ".*" + parm.getName() + "="
                                                                                                                  + parm.getValue(), "Did not find the original request in the Stored Value in the OpenIdContext."));

                    }
                } else {
                    expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_MATCHES, requester + ServletMessageConstants.STORED_VALUE
                                                                                                              + OpenIdConstant.ORIGINAL_REQUEST
                                                                                                              + ".*"
                                                                                                              + rspValues.getOriginalRequest(), "Did not find the original request in the Stored Value in the OpenIdContext."));
                }
            } else {
                expectations.addExpectation(new ResponseFullExpectation(action, Constants.STRING_MATCHES, requester + ServletMessageConstants.STORED_VALUE
                                                                                                          + OpenIdConstant.ORIGINAL_REQUEST
                                                                                                          + ".*"
                                                                                                          + ServletMessageConstants.EMPTY, "Did not find the original request in the Stored Value in the OpenIdContext."));
            }
        }
    }

    public static void getOpenIdContextAccessTokenScopeExpectations(String action, Expectations expectations, String requester, String[] scopes) throws Exception {
        expectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, buildAccessTokenScopeString(requester,
                                                                                                                             scopes), "The access token scope claim returned by the server does not match what is configured by the client."));
    }

    public static void getOpenIdContextMockUserInfoExpectations(String action, Expectations expectations, String requester, String[] scopes) throws Exception {
        Set<String> scopeSet = new HashSet<>(Arrays.asList(scopes));

        // relates to JsonUserInfoScopeServlet - only uses a subset of the profile scope claims for brevity
        String profileScopeCheckType = scopeSet.contains(OpenIdConstant.PROFILE_SCOPE) ? Constants.STRING_DOES_NOT_CONTAIN : Constants.STRING_CONTAINS;
        expectations.addExpectation(new ResponseFullExpectation(null, profileScopeCheckType, buildUserInfoClaimString(requester, OpenIdConstant.NAME,
                                                                                                                      ServletMessageConstants.OPTIONAL_EMPTY), "The userinfo name claim was not as expected."));
        expectations.addExpectation(new ResponseFullExpectation(null, profileScopeCheckType, buildUserInfoClaimString(requester, OpenIdConstant.PREFERRED_USERNAME,
                                                                                                                      ServletMessageConstants.OPTIONAL_EMPTY), "The userinfo preferred_username claim was not as expected."));
        expectations.addExpectation(new ResponseFullExpectation(null, profileScopeCheckType, buildUserInfoClaimString(requester, OpenIdConstant.BIRTHDATE,
                                                                                                                      ServletMessageConstants.OPTIONAL_EMPTY), "The userinfo birthdate claim was not as expected."));
        expectations.addExpectation(new ResponseFullExpectation(null, profileScopeCheckType, buildUserInfoClaimString(requester, OpenIdConstant.LOCALE,
                                                                                                                      ServletMessageConstants.OPTIONAL_EMPTY), "The userinfo locale claim was not as expected."));

        String emailScopeCheckType = scopeSet.contains(OpenIdConstant.EMAIL_SCOPE) ? Constants.STRING_DOES_NOT_CONTAIN : Constants.STRING_CONTAINS;
        expectations.addExpectation(new ResponseFullExpectation(null, emailScopeCheckType, buildUserInfoClaimString(requester, OpenIdConstant.EMAIL,
                                                                                                                    ServletMessageConstants.OPTIONAL_EMPTY), "The userinfo email claim was not as expected."));
        expectations.addExpectation(new ResponseFullExpectation(null, emailScopeCheckType, buildUserInfoClaimString(requester, OpenIdConstant.EMAIL_VERIFIED,
                                                                                                                    ServletMessageConstants.OPTIONAL_EMPTY), "The userinfo email_verified claim was not as expected."));

    }

    public static String buildNonceString(String requester) throws Exception {

        return requester + ServletMessageConstants.ID_TOKEN + ServletMessageConstants.CLAIM + ServletMessageConstants.KEY + PayloadConstants.PAYLOAD_NONCE;
    }

    public static String buildIssuedAtTimeString(String requester) throws Exception {
        return requester + ServletMessageConstants.ID_TOKEN + ServletMessageConstants.CLAIM + ServletMessageConstants.KEY + PayloadConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS;
    }

    public static String buildContextString(String requester) throws Exception {
        return requester + ServletMessageConstants.OPENID_CONTEXT + ServletMessageConstants.OPENID_CONTEXT;
    }

    public static String buildAccessTokenScopeString(String requester, String... scopes) throws Exception {
        return requester + ServletMessageConstants.ACCESS_TOKEN + ServletMessageConstants.CLAIM
               + ServletMessageConstants.KEY + Constants.SCOPE + " "
               + ServletMessageConstants.VALUE + "[" + String.join(", ", scopes) + "]";
    }

    public static String buildUserInfoClaimString(String requester, String claim, String value) throws Exception {
        return requester + ServletMessageConstants.USERINFO + ServletMessageConstants.KEY + claim + " " + ServletMessageConstants.VALUE + value;
    }
}
