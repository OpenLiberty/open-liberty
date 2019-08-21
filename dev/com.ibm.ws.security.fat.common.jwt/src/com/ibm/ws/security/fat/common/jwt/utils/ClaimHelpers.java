/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.jwt.utils;

import java.util.List;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;

import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.ClaimConstants;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.expectations.JwtClaimExpectation;

@SuppressWarnings("restriction")
public class ClaimHelpers {

    protected static Class<?> thisClass = ClaimHelpers.class;

    /**
     * Use the values in the builder to compare against the consumer output. (since we're using the builder to create the jwt
     * token that the consumer will
     * process, those values should be what we need to compare against the consumer output)
     *
     * @param expectations
     *            - existing expectations
     * @param currentAction
     *            - the action to search for the output in
     * @param builder
     *            - the builder containing the values
     * @return - the updated expectations
     * @throws Exception
     */
    public static Expectations updateExpectationsForClaimAppOutput(Expectations expectations, String prefix, String currentAction, JwtClaims claims) throws Exception {
        // Audience can be a list of string values - the order of entries could vary and each element is quoted in on of the outputs that we'll
        // need to check, so, handle each list element separately
        List<String> audiences = claims.getAudience();
        if (audiences != null) {
            for (String aud : audiences) {
                expectations = updateClaimExpectationsForJsonAttribute(expectations, prefix, ClaimConstants.AUDIENCE, JwtConstants.JWT_BUILDER_AUDIENCE, aud);
            }
        }
        expectations = updateClaimExpectationsForJsonAttribute(expectations, prefix, ClaimConstants.ISSUER, JwtConstants.JWT_BUILDER_ISSUER, claims.getIssuer());
        expectations = updateClaimExpectationsForJsonAttribute(expectations, prefix, ClaimConstants.ISSUED_AT, JwtConstants.JWT_BUILDER_ISSUED_AT,
                                                               handleTime(claims.getIssuedAt()));
        expectations = updateClaimExpectationsForJsonAttribute(expectations, prefix, ClaimConstants.EXPIRATION_TIME, JwtConstants.JWT_BUILDER_EXPIRATION,
                                                               handleTime(claims.getExpirationTime()));
        expectations = updateExpectationsForJsonAttribute(expectations, prefix, ClaimConstants.SCOPE, claims.getStringClaimValue(ClaimConstants.SCOPE));
        expectations = updateClaimExpectationsForJsonAttribute(expectations, prefix, ClaimConstants.SUBJECT, JwtConstants.JWT_BUILDER_SUBJECT, claims.getSubject());
        expectations = updateExpectationsForJsonAttribute(expectations, prefix, ClaimConstants.REALM_NAME, claims.getStringClaimValue(ClaimConstants.REALM_NAME));
        return expectations;
    }

    /**
     * adds the expectations for the attribute in the json object as well as the attribute/value obtained from the raw json (not
     * from the attr specific api)
     *
     * @param expectations
     *            - already set expectations that we'll add to
     * @param key
     *            - they key name to search for
     * @param value
     *            - the value to search for
     * @return - expecations object updated with a new expectation
     * @throws Exception
     */
    public static Expectations updateExpectationsForJsonAttribute(Expectations expectations, String prefix, String key, Object value) throws Exception {

//        if (value != null) {
        expectations.addExpectation(new JwtClaimExpectation(prefix, key, value, JwtClaimExpectation.ValidationMsgType.CLAIM_FROM_LIST));
        expectations.addExpectation(new JwtClaimExpectation(prefix, key, value, JwtClaimExpectation.ValidationMsgType.CLAIM_LIST_MEMBER));
//        }
        return expectations;

    }

    public static Expectations updateExpectationsForJsonHeaderAttribute(Expectations expectations, String prefix, String key, Object value) throws Exception {

//      if (value != null) {
        expectations.addExpectation(new JwtClaimExpectation(prefix, key, value, JwtClaimExpectation.ValidationMsgType.HEADER_CLAIM_FROM_LIST));
        expectations.addExpectation(new JwtClaimExpectation(prefix, key, value, JwtClaimExpectation.ValidationMsgType.CLAIM_LIST_MEMBER));
//      }
        return expectations;

    }

    /**
     * adds the check for the attribute/value that was obtained by invoking the <Claim.get<specificAttr>> method
     *
     * @param expectations
     *            - already set expectations that we'll add to
     * @param key
     *            - they key name to pass on so additional expectations can be added
     * @param keyLogName
     *            - they key name to search for
     * @param value
     *            - the value to search for
     * @return - expecations object updated with a new expectation
     * @throws Exception
     */
    public static Expectations updateClaimExpectationsForJsonAttribute(Expectations expectations, String prefix, String key, String keyLogName, Object value) throws Exception {

        if (keyLogName != null) {
            expectations.addExpectation(new JwtClaimExpectation(prefix, keyLogName, value, JwtClaimExpectation.ValidationMsgType.SPECIFIC_CLAIM_API));
        }
        return updateExpectationsForJsonAttribute(expectations, prefix, key, value);
    }

    public static String handleTime(NumericDate value) throws Exception {
        if (value == null) {
            return null;
        } else {
            Long longValue = value.getValue();
            String stringValue = Long.toString(longValue);
            return stringValue.substring(0, stringValue.length() - 3);
        }

    }

}