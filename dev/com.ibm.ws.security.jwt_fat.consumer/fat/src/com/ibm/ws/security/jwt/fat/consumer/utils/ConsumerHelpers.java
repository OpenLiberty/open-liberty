/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import com.ibm.ws.security.fat.common.jwt.HeaderConstants;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.jwt.expectations.JwtApiExpectation;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.jwt.fat.consumer.JwtConsumerConstants;

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

    public Expectations addGoodConsumerAlgExpectations(String currentAction, LibertyServer consumerServer, String sigAlg) throws Exception {

        Expectations expectations = buildConsumerClientAppExpectations(currentAction, consumerServer);
        expectations.addExpectation(new JwtApiExpectation(JwtConsumerConstants.JWT_TOKEN_HEADER, HeaderConstants.ALGORITHM, sigAlg, JwtApiExpectation.ValidationMsgType.HEADER_CLAIM_FROM_LIST));
        expectations.addExpectation(new JwtApiExpectation(JwtConsumerConstants.JWT_TOKEN_HEADER, HeaderConstants.ALGORITHM, sigAlg, JwtApiExpectation.ValidationMsgType.CLAIM_LIST_MEMBER));

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
                expectations = updateConsumerExpectationsForJsonAttribute(expectations, PayloadConstants.AUDIENCE, aud);
            }
        }
        expectations = updateConsumerExpectationsForJsonAttribute(expectations, PayloadConstants.ISSUER, claims.getIssuer());
        expectations = updateConsumerExpectationsForJsonAttribute(expectations, PayloadConstants.ISSUED_AT, claims.getIssuedAt().getValue());
        expectations = updateConsumerExpectationsForJsonAttribute(expectations, PayloadConstants.EXPIRATION_TIME, claims.getExpirationTime().getValue());
        expectations = updateExpectationsForJsonAttribute(expectations, PayloadConstants.SCOPE, claims.getStringClaimValue(PayloadConstants.SCOPE));
        expectations = updateConsumerExpectationsForJsonAttribute(expectations, PayloadConstants.SUBJECT, claims.getSubject());
        expectations = updateExpectationsForJsonAttribute(expectations, PayloadConstants.REALM_NAME, claims.getStringClaimValue(PayloadConstants.REALM_NAME));
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

        expectations.addExpectation(new JwtApiExpectation(key, value, JwtApiExpectation.ValidationMsgType.CLAIM_FROM_LIST));
        expectations.addExpectation(new JwtApiExpectation(key, value, JwtApiExpectation.ValidationMsgType.CLAIM_LIST_MEMBER));

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
    public Expectations updateConsumerExpectationsForJsonAttribute(Expectations expectations, String key, Object value) throws Exception {

        expectations.addExpectation(new JwtApiExpectation(key, value, JwtApiExpectation.ValidationMsgType.SPECIFIC_CLAIM_API));

        return updateExpectationsForJsonAttribute(expectations, key, value);

    }

    public Expectations buildNegativeAttributeExpectations(String specificErrorId, String currentAction, LibertyServer consumerServer, String jwtConsumerId) throws Exception {

        Expectations expectations = buildConsumerClientAppExpectations(currentAction, consumerServer);
        expectations.addExpectation(new JwtApiExpectation(specificErrorId, jwtConsumerId));

        return expectations;
    }

    public Expectations buildConsumerClientAppExpectations(String currentAction, LibertyServer consumerServer) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, SecurityFatHttpUtils.getServerUrlBase(consumerServer) + JwtConsumerConstants.JWT_CONSUMER_ENDPOINT));

        return expectations;
    }
}