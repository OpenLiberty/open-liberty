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
package com.ibm.ws.security.jwt.fat.consumer.utils;

import java.util.List;

import org.jose4j.jwt.JwtClaims;

import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.ClaimConstants;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.jwt.fat.consumer.JWTConsumerConstants;
import com.ibm.ws.security.jwt.fat.consumer.expectations.JwtConsumerExpectation;

import componenttest.topology.impl.LibertyServer;

@SuppressWarnings("restriction")
public class ConsumerHelpers extends JwtTokenBuilderUtils {

    protected static Class<?> thisClass = ConsumerHelpers.class;

    /**
     * Create a new JWTTokenBuilder and initialize it with default test values
     *
     * @return - an initialized JWTTokenBuilder
     * @throws Exception
     */
    public JWTTokenBuilder createBuilderWithDefaultConsumerClaims() throws Exception {
        JWTTokenBuilder builder = createBuilderWithDefaultClaims();

        builder.setScope("openid profile jwtConsumer");

        return builder;
    }

    public Expectations addGoodConsumerClientResponseAndClaimsExpectations(String currentAction, JWTTokenBuilder builder, LibertyServer consumerServer) throws Exception {

        Expectations expectations = buildConsumerClientAppExpectations(currentAction, consumerServer);
        expectations = updateExpectationsForConsumerAppOutput(expectations, currentAction, builder);
        return expectations;
    }

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
    public Expectations updateExpectationsForConsumerAppOutput(Expectations expectations, String currentAction, JWTTokenBuilder builder) throws Exception {
        JwtClaims claims = builder.getRawClaims();
        // Audience can be a list of string values - the order of entries could vary and each element is quoted in on of the outputs that we'll
        // need to check, so, handle each list element separately
        List<String> audiences = claims.getAudience();
        if (audiences != null) {
            for (String aud : audiences) {
                expectations = updateConsumerExpectationsForJsonAttribute(expectations, ClaimConstants.AUDIENCE, JWTConsumerConstants.JWT_BUILDER_AUDIENCE, aud);
            }
        }
        expectations = updateConsumerExpectationsForJsonAttribute(expectations, ClaimConstants.ISSUER, JWTConsumerConstants.JWT_BUILDER_ISSUER, claims.getIssuer());
        expectations = updateConsumerExpectationsForJsonAttribute(expectations, ClaimConstants.ISSUED_AT, JWTConsumerConstants.JWT_BUILDER_ISSUED_AT, claims.getIssuedAt().getValue());
        expectations = updateConsumerExpectationsForJsonAttribute(expectations, ClaimConstants.EXPIRATION_TIME, JWTConsumerConstants.JWT_BUILDER_EXPIRATION, claims.getExpirationTime().getValue());
        expectations = updateExpectationsForJsonAttribute(expectations, ClaimConstants.SCOPE, claims.getStringClaimValue(ClaimConstants.SCOPE));
        expectations = updateConsumerExpectationsForJsonAttribute(expectations, ClaimConstants.SUBJECT, JWTConsumerConstants.JWT_BUILDER_SUBJECT, claims.getSubject());
        expectations = updateExpectationsForJsonAttribute(expectations, ClaimConstants.REALM_NAME, claims.getStringClaimValue(ClaimConstants.REALM_NAME));
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
    public Expectations updateExpectationsForJsonAttribute(Expectations expectations, String key, Object value) throws Exception {

        expectations.addExpectation(new JwtConsumerExpectation(key, value, JwtConsumerExpectation.ValidationMsgType.CLAIM_FROM_LIST));
        expectations.addExpectation(new JwtConsumerExpectation(key, value, JwtConsumerExpectation.ValidationMsgType.CLAIM_LIST_MEMBER));

        return expectations;
    }

    /**
     * adds the check for the attribute/value that was obtained by invoking the <consumer.get<specificAttr>> method
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
    public Expectations updateConsumerExpectationsForJsonAttribute(Expectations expectations, String key, String keyLogName, Object value) throws Exception {

        expectations.addExpectation(new JwtConsumerExpectation(keyLogName, value, JwtConsumerExpectation.ValidationMsgType.SPECIFIC_CLAIM_API));

        return updateExpectationsForJsonAttribute(expectations, key, value);

    }

    public Expectations buildNegativeAttributeExpectations(String specificErrorId, String currentAction, LibertyServer consumerServer, String jwtConsumerId) throws Exception {

        Expectations expectations = buildConsumerClientAppExpectations(currentAction, consumerServer);
        expectations.addExpectation(new JwtConsumerExpectation(specificErrorId, jwtConsumerId));

        return expectations;
    }

    public Expectations buildConsumerClientAppExpectations(String currentAction, LibertyServer consumerServer) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, SecurityFatHttpUtils.getServerUrlBase(consumerServer) + JwtConstants.JWT_CONSUMER_ENDPOINT));

        return expectations;
    }
}