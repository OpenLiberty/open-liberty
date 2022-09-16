/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.utils;


import java.io.IOException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.PrivilegedActionException;
import java.util.List;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.HmacKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.jwk.impl.JWKSet;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.wsspi.ssl.SSLSupport;


/**
 *
 */
public class CommonJose4jUtils {
    
    public static final TraceComponent tc = Tr.register(CommonJose4jUtils.class);
    TokenSignatureValidationBuilder tokenSignatureValidationBuilder = new TokenSignatureValidationBuilder();
    public CommonJose4jUtils() {
        
    }
    
    //Just parse without validation for now
    public static JwtContext parseJwtWithoutValidation(String jwtString) throws Exception {
        JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();

        return firstPassJwtConsumer.process(jwtString);
    }
    
    
    public JsonWebStructure getJsonWebStructureFromJwtContext(JwtContext jwtContext) throws Exception {
        List<JsonWebStructure> jsonStructures = jwtContext.getJoseObjects();
        if (jsonStructures == null || jsonStructures.isEmpty()) {
            throw new Exception("Invalid JsonWebStructure");
        }
        JsonWebStructure jsonStruct = jsonStructures.get(0);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "JsonWebStructure class: " + jsonStruct.getClass().getName() + " data:" + jsonStruct);
            if (jsonStruct instanceof JsonWebSignature) {
                JsonWebSignature signature = (JsonWebSignature) jsonStruct;
                Tr.debug(tc, "JsonWebSignature alg: " + signature.getAlgorithmHeaderValue() + " 3rd:'" + signature.getEncodedSignature() + "'");
            }
        }
        return jsonStruct;
    }
    
    public TokenSignatureValidationBuilder signaturevalidationbuilder() {
        return this.tokenSignatureValidationBuilder;
    }
    
  //TODO: use this in regular OIDC flow to do token signature validation

    public static class TokenSignatureValidationBuilder {

        private JsonWebStructure signature;
        private SSLSupport sslsupport;
        private String jwkuri;
        private String clientid;
        private String clientsecret;
        JWKSet jwkset = new JWKSet(); // TODO: this should be per Client?
        private String issuer;

        private TokenSignatureValidationBuilder() {

        }

        /**
         * @param jsonStruct
         */
        public TokenSignatureValidationBuilder signature(JsonWebStructure jsonStruct) {
            this.signature = jsonStruct;
            return this;
        }

        /**
         * @param sslSupport
         */
        public TokenSignatureValidationBuilder sslsupport(SSLSupport sslSupport) {
            this.sslsupport = sslSupport;
            return this;
        }

        /**
         * @param jwksURI
         */
        public TokenSignatureValidationBuilder jwkuri(String jwksURI) {
            this.jwkuri = jwksURI;
            return this;
        }

        /**
         * @param clientId
         */
        public TokenSignatureValidationBuilder clientid(String clientId) {
            this.clientid = clientId;
            return this;
        }

        /**
         * @param clientSecret
         */
        public TokenSignatureValidationBuilder clientsecret(@Sensitive String clientSecret) {
            this.clientsecret = clientSecret;
            return this;
        }

        /**
         * @param issuer
         * @return
         */
        public TokenSignatureValidationBuilder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        /**
         * @return
         * @throws Exception
         * @throws InterruptedException
         * @throws IOException
         * @throws PrivilegedActionException
         * @throws KeyStoreException
         */
        public Key getVerificationKey() throws KeyStoreException, PrivilegedActionException, IOException, InterruptedException, Exception {
            String kid = this.signature.getKeyIdHeaderValue();
            String x5t = this.signature.getX509CertSha1ThumbprintHeaderValue();
            if (getSignatureAlgorithm() != null && getSignatureAlgorithm().startsWith("HS")) {
                return new HmacKey(clientsecret.getBytes("UTF-8"));
            }
            return createJwkRetriever().getPublicKeyFromJwk(kid, x5t, "sig", false);
        }

        /**
         * @return
         * @throws Exception
         */
        private String getSignatureAlgorithm() throws Exception {
            String algorithm = null;
            if (this.signature != null && this.signature instanceof JsonWebSignature) {
                algorithm = ((JsonWebSignature) this.signature).getAlgorithmHeaderValue();
            }
            String supportedAlgorithms = "RS256 HS256 EC256 RS384 HS384 ES384 RS512 HS512 ES512";
            if (supportedAlgorithms.contains(algorithm)) {
                return algorithm;
            } else {
                throw new Exception(algorithm + " signing algorithm is not supported");
            }

        }

        private JwKRetriever createJwkRetriever() throws Exception {

            String algorithm = getSignatureAlgorithm();
            return new JwKRetriever(this.clientid, null, this.jwkuri, this.jwkset, this.sslsupport, false, null, null, algorithm);
        }

        /**
         * @throws Exception
         * @throws InterruptedException
         * @throws IOException
         * @throws PrivilegedActionException
         * @throws KeyStoreException
         *
         */
        public JwtClaims parseJwtWithValidation(String jwtString) throws KeyStoreException, PrivilegedActionException, IOException, InterruptedException, Exception {
            Key key = getVerificationKey();
            if (key == null) {
                throw new Exception("error getting verification key");
            }
            JwtConsumerBuilder builder = new JwtConsumerBuilder();
            builder.setRequireExpirationTime().setAllowedClockSkewInSeconds(120).setExpectedAudience(this.clientid).setExpectedIssuer(false,
                                                                                                                                      this.issuer).setRequireSubject().setSkipDefaultAudienceValidation().setVerificationKey(key).setRelaxVerificationKeyValidation();

            JwtConsumer jwtConsumer = builder.build();

            JwtContext validatedJwtContext = jwtConsumer.process(jwtString);
            return validatedJwtContext.getJwtClaims();

        }
    }

}
