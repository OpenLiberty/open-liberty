/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.common.jwt.jws;

import java.security.Key;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.common.jwt.JwtParsingUtils;
import io.openliberty.security.common.jwt.exceptions.EncodedSignatureEmptyException;
import io.openliberty.security.common.jwt.exceptions.JwtContextMissingJoseObjects;
import io.openliberty.security.common.jwt.exceptions.SignatureAlgorithmDoesNotMatchHeaderException;

public class JwsSignatureVerifier {

    private static final TraceComponent tc = Tr.register(JwsSignatureVerifier.class);

    private final Key key;
    private final String signatureAlgorithm;

    private JwsSignatureVerifier(Builder builder) {
        this.key = builder.key;
        this.signatureAlgorithm = (builder.signatureAlgorithm != null) ? builder.signatureAlgorithm : "RS256";
    }

    public Key getKey() {
        return key;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public JwtClaims validateJwsSignature(JwtContext jwtContext) throws EncodedSignatureEmptyException, SignatureAlgorithmDoesNotMatchHeaderException, InvalidJwtException, JwtContextMissingJoseObjects {
        JsonWebStructure jws = JwtParsingUtils.getJsonWebStructureFromJwtContext(jwtContext);
        return validateJwsSignature((JsonWebSignature) jws, jwtContext.getJwt());
    }

    JwtClaims validateJwsSignature(JsonWebSignature signature, String jwtString) throws EncodedSignatureEmptyException, SignatureAlgorithmDoesNotMatchHeaderException, InvalidJwtException {
        verifySignAlgOnly(signature);

        JwtConsumerBuilder builder = new JwtConsumerBuilder();
        builder.setSkipDefaultAudienceValidation();
        if ("none".equals(signatureAlgorithm)) {
            // Signature algorithm is set to "none"; don't check the signature
            builder.setDisableRequireSignature()
                    .setSkipSignatureVerification();
        } else {
            builder.setVerificationKey(key)
                    .setRelaxVerificationKeyValidation();
        }
        JwtConsumer jwtConsumer = builder.build();
        JwtContext validatedJwtContext = jwtConsumer.process(jwtString);
        return validatedJwtContext.getJwtClaims();
    }

    void verifySignAlgOnly(JsonWebSignature signature) throws EncodedSignatureEmptyException, SignatureAlgorithmDoesNotMatchHeaderException {
        String algHeader = signature.getAlgorithmHeaderValue();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Signing algorithm from header: " + algHeader);
        }
        if ("none".equals(signatureAlgorithm)) {
            return;
        }
        if (signature.getEncodedSignature().isEmpty()) {
            throw new EncodedSignatureEmptyException();
        }
        if (!(signatureAlgorithm.equals(algHeader))) {
            throw new SignatureAlgorithmDoesNotMatchHeaderException(signatureAlgorithm, algHeader);
        }
    }

    public static class Builder {

        private Key key = null;
        private String signatureAlgorithm = null;

        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        public Builder signatureAlgorithm(String signatureAlgorithm) {
            this.signatureAlgorithm = signatureAlgorithm;
            return this;
        }

        public JwsSignatureVerifier build() {
            return new JwsSignatureVerifier(this);
        }

    }

}
