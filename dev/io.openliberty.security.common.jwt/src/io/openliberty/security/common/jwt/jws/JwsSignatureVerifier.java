/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.common.jwt.jws;

import java.security.Key;
import java.util.Arrays;
import java.util.List;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.common.jwt.exceptions.JwtContextMissingJoseObjects;
import io.openliberty.security.common.jwt.exceptions.SignatureAlgorithmNotInAllowedList;
import io.openliberty.security.common.jwt.exceptions.SigningKeyNotSpecifiedException;

public class JwsSignatureVerifier {

    private static final TraceComponent tc = Tr.register(JwsSignatureVerifier.class);

    private final Key key;
    private final String signatureAlgorithm;
    private final List<String> signatureAlgorithmsSupported;

    private JwsSignatureVerifier(Builder builder) {
        this.key = builder.key;
        this.signatureAlgorithm = builder.signatureAlgorithm;
        this.signatureAlgorithmsSupported = builder.signatureAlgorithmsSupported;
    }

    public List<String> getSignatureAlgorithmsSupported() {
        if (signatureAlgorithm != null) {
            return Arrays.asList(signatureAlgorithm);
        }
        if (signatureAlgorithmsSupported != null) {
            return signatureAlgorithmsSupported;
        }
        // Default to RS256
        return Arrays.asList("RS256");
    }

    /**
     * Verifies the "alg" header in the JWT to ensure the token is signed with one of the allowed algorithms. This allows us to
     * avoid doing the work to fetch the signing key for the token if the algorithm isn't supported.
     */
    public static String verifyJwsAlgHeaderOnly(JwtContext jwtContext, List<String> signingAlgorithmsAllowed) throws JwtContextMissingJoseObjects, SignatureAlgorithmNotInAllowedList {
        JsonWebSignature jws = (JsonWebSignature) JwtParsingUtils.getJsonWebStructureFromJwtContext(jwtContext);
        String algHeader = jws.getAlgorithmHeaderValue();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Signing algorithm from header: " + algHeader);
        }
        if (!signingAlgorithmsAllowed.contains(algHeader)) {
            throw new SignatureAlgorithmNotInAllowedList(algHeader, signingAlgorithmsAllowed);
        }
        return algHeader;
    }

    /**
     * Verifies the "alg" header in the JWT to ensure the token is signed with one of the allowed algorithms, and validates the
     * signature of the token.
     */
    public JwtClaims validateJwsSignature(JwtContext jwtContext) throws JwtContextMissingJoseObjects, SignatureAlgorithmNotInAllowedList, SigningKeyNotSpecifiedException, InvalidJwtException {
        String algHeader = verifyJwsAlgHeaderOnly(jwtContext, getSignatureAlgorithmsSupported());

        JwtConsumerBuilder builder = createJwtConsumerBuilderWithConstraints(algHeader);

        JwtConsumer jwtConsumer = builder.build();
        JwtContext validatedJwtContext = jwtConsumer.process(jwtContext.getJwt());
        return validatedJwtContext.getJwtClaims();
    }

    public JwtConsumerBuilder createJwtConsumerBuilderWithConstraints(String algHeader) throws SigningKeyNotSpecifiedException {
        JwtConsumerBuilder builder = new JwtConsumerBuilder();
        setJwsAlgorithmConstraints(builder);
        builder.setSkipDefaultAudienceValidation();
        if ("none".equals(algHeader)) {
            // Signature algorithm is set to "none"; don't check the signature
            builder.setDisableRequireSignature()
                    .setSkipSignatureVerification();
        } else {
            if (key == null) {
                throw new SigningKeyNotSpecifiedException(algHeader);
            }
            builder.setVerificationKey(key)
                    .setRelaxVerificationKeyValidation();
        }
        return builder;
    }

    private void setJwsAlgorithmConstraints(JwtConsumerBuilder builder) {
        List<String> signatureAlgorithmsSupported = getSignatureAlgorithmsSupported();
        String[] algsAsArray = signatureAlgorithmsSupported.toArray(new String[signatureAlgorithmsSupported.size()]);
        builder.setJwsAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.WHITELIST, algsAsArray));
    }

    public static class Builder {

        private Key key = null;
        private String signatureAlgorithm = null;
        private List<String> signatureAlgorithmsSupported = null;

        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        public Builder signatureAlgorithm(String signatureAlgorithm) {
            this.signatureAlgorithm = signatureAlgorithm;
            return this;
        }

        public Builder signatureAlgorithmsSupported(String... signatureAlgorithmsSupported) {
            this.signatureAlgorithmsSupported = Arrays.asList(signatureAlgorithmsSupported);
            return this;
        }

        public JwsSignatureVerifier build() {
            return new JwsSignatureVerifier(this);
        }

    }

}
