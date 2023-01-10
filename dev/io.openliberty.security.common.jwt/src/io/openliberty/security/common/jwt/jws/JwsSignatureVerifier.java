/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import io.openliberty.security.common.jwt.exceptions.SignatureAlgorithmDoesNotMatchHeaderException;
import io.openliberty.security.common.jwt.exceptions.SignatureAlgorithmNotInAllowedList;

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

    public JwtClaims validateJwsSignature(JwtContext jwtContext) throws JwtContextMissingJoseObjects, SignatureAlgorithmDoesNotMatchHeaderException, SignatureAlgorithmNotInAllowedList, InvalidJwtException {
        verifyAlgHeaderOnly(jwtContext);

        JwtConsumerBuilder builder = createBuilderWithConstraints();

        JwtConsumer jwtConsumer = builder.build();
        JwtContext validatedJwtContext = jwtConsumer.process(jwtContext.getJwt());
        return validatedJwtContext.getJwtClaims();
    }

    public void verifyAlgHeaderOnly(JwtContext jwtContext) throws JwtContextMissingJoseObjects, SignatureAlgorithmDoesNotMatchHeaderException, SignatureAlgorithmNotInAllowedList {
        JsonWebSignature signature = (JsonWebSignature) JwtParsingUtils.getJsonWebStructureFromJwtContext(jwtContext);
        String algHeader = signature.getAlgorithmHeaderValue();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Signing algorithm from header: " + algHeader);
        }
        if (signatureAlgorithm != null && !(signatureAlgorithm.equals(algHeader))) {
            throw new SignatureAlgorithmDoesNotMatchHeaderException(signatureAlgorithm, algHeader);
        }
        if (signatureAlgorithm == null && (signatureAlgorithmsSupported != null && !signatureAlgorithmsSupported.contains(algHeader))) {
            throw new SignatureAlgorithmNotInAllowedList(algHeader, signatureAlgorithmsSupported);
        }
    }

    JwtConsumerBuilder createBuilderWithConstraints() {
        JwtConsumerBuilder builder = new JwtConsumerBuilder();
        setJwsAlgorithmConstraints(builder);
        builder.setSkipDefaultAudienceValidation();
        if ("none".equals(signatureAlgorithm)) {
            // Signature algorithm is set to "none"; don't check the signature
            builder.setDisableRequireSignature()
                    .setSkipSignatureVerification();
        } else {
            builder.setVerificationKey(key)
                    .setRelaxVerificationKeyValidation();
        }
        return builder;
    }

    void setJwsAlgorithmConstraints(JwtConsumerBuilder builder) {
        String[] algorithmsAllowed;
        if (signatureAlgorithm != null) {
            algorithmsAllowed = new String[] { signatureAlgorithm };
        } else if (signatureAlgorithmsSupported != null && !signatureAlgorithmsSupported.isEmpty()) {
            algorithmsAllowed = signatureAlgorithmsSupported.toArray(new String[signatureAlgorithmsSupported.size()]);
        } else {
            // Fall back to only allowing RS256
            algorithmsAllowed = new String[] { "RS256" };
        }
        builder.setJwsAlgorithmConstraints(new AlgorithmConstraints(ConstraintType.WHITELIST, algorithmsAllowed));
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
