/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

import java.security.Key;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;

import org.jose4j.base64url.Base64;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.websphere.security.jwt.KeyException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.ws.security.common.jwk.impl.JwkKidBuilder;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.jwt.config.MpConfigProperties;
import com.ibm.ws.security.jwt.internal.BuilderImpl;
import com.ibm.ws.security.jwt.internal.JwtTokenException;

public class JweHelper {

    private static final TraceComponent tc = Tr.register(JweHelper.class);

    @FFDCIgnore({ Exception.class })
    public static String createJweString(String jws, JwtData jwtData) throws Exception {
        JweHelper helper = new JweHelper();
        JwtConfig jwtConfig = jwtData.getConfig();
        try {
            JsonWebEncryption jwe = new JsonWebEncryption();
            BuilderImpl builder = jwtData.getBuilder();
            helper.setJweKeyData(jwe, builder, jwtConfig);
            helper.setJweHeaders(jwe, builder, jwtConfig);
            jwe.setPayload(jws);
            return helper.getJwtString(jwe);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "ERROR_BUILDING_SIGNED_JWE", new Object[] { jwtConfig.getId(), e });
            throw new Exception(errorMsg, e);
        }
    }

    public static boolean isJws(String jwtString) {
        if (jwtString == null || jwtString.isEmpty()) {
            return false;
        }
        String notPeriod = "[^\\.]";
        return jwtString.matches("^(" + notPeriod + "*\\.){2}" + notPeriod + "*$");
    }

    public static boolean isJwe(String jwtString) {
        if (jwtString == null || jwtString.isEmpty()) {
            return false;
        }
        String notPeriod = "[^\\.]";
        return jwtString.matches("^(" + notPeriod + "*\\.){4}" + notPeriod + "*$");
    }

    /**
     * Returns whether the current request must only accept JWS tokens, per the MP JWT 1.2 specification. Per the spec, if the
     * {@value MpConfigProperties.DECRYPT_KEY_LOCATION} MP Config property (or, in our case, the keyManagementKeyAlias config
     * attribute) is NOT set, then we must only accept JWS tokens; tokens in JWE format should be rejected.
     *
     * For more information, see
     * {@link https://github.com/eclipse/microprofile-jwt-auth/blob/master/spec/src/main/asciidoc/configuration.asciidoc#requirements-for-accepting-signed-and-encrypted-tokens}
     */
    public static boolean isJwsRequired(JwtConsumerConfig config, MpConfigProperties mpConfigProps) {
        return !isJweRequired(config, mpConfigProps);
    }

    /**
     * Returns whether the current request must only accept JWE tokens, per the MP JWT 1.2 specification. Per the spec, if the
     * {@value MpConfigProperties.DECRYPT_KEY_LOCATION} MP Config property (or, in our case, the keyManagementKeyAlias config
     * attribute) is set, then we must only accept JWE tokens; tokens in JWS format should be rejected.
     *
     * For more information, see
     * {@link https://github.com/eclipse/microprofile-jwt-auth/blob/master/spec/src/main/asciidoc/configuration.asciidoc#requirements-for-accepting-signed-and-encrypted-tokens}
     */
    public static boolean isJweRequired(JwtConsumerConfig config, MpConfigProperties mpConfigProps) {
        String keyAlias = config.getKeyManagementKeyAlias();
        String keyLocation = mpConfigProps.get(MpConfigProperties.DECRYPT_KEY_LOCATION);
        return (keyAlias != null || keyLocation != null);
    }

    @FFDCIgnore({ Exception.class })
    public static String extractJwsFromJweToken(String jweString, JwtConsumerConfig config, MpConfigProperties mpConfigProps) throws InvalidTokenException {
        JweHelper helper = new JweHelper();
        String payload = null;
        try {
            payload = helper.getJwePayload(jweString, config, mpConfigProps);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE", new Object[] { config.getId(), e });
            throw new InvalidTokenException(errorMsg, e);
        }
        if (!isJws(payload)) {
            String errorMsg = Tr.formatMessage(tc, "NESTED_JWS_REQUIRED_BUT_NOT_FOUND");
            throw new InvalidTokenException(errorMsg);
        }
        return payload;
    }

    String getJwePayload(String jweString, JwtConsumerConfig config, MpConfigProperties mpConfigProps) throws JoseException, Exception, InvalidTokenException {
        JweHelper helper = new JweHelper();
        JsonWebEncryption jwe = new JsonWebEncryption();
        jwe.setCompactSerialization(jweString);
        Key decryptionKey = helper.getJweDecryptionKey(config, mpConfigProps, helper.getKidFromJweString(jweString));
        jwe.setKey(decryptionKey);
        String payload = jwe.getPayload();
        if (isJws(payload)) {
            helper.verifyContentType(jwe);
        }
        return payload;
    }

    void verifyContentType(JsonWebEncryption jwe) throws InvalidTokenException {
        String requiredContentType = "JWT";
        String cty = jwe.getContentTypeHeaderValue();
        if (cty == null || !requiredContentType.equalsIgnoreCase(cty)) {
            String errorMsg = Tr.formatMessage(tc, "CTY_NOT_JWT_FOR_NESTED_JWS", new Object[] { "\"cty\"", cty, "\"" + requiredContentType + "\"" });
            throw new InvalidTokenException(errorMsg);
        }
    }

    @FFDCIgnore(Exception.class)
    String getKidFromJweString(String jweString) {
        String kid = null;
        try {
            String headerString = jweString.substring(0, jweString.indexOf("."));
            headerString = new String(Base64.decode(headerString));
            kid = (String) JwtUtils.claimFromJsonObject(headerString, "kid");
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting kid from JWE string: " + e);
            }
        }
        return kid;
    }

    void setJweKeyData(JsonWebEncryption jwe, BuilderImpl builder, JwtConfig jwtConfig) throws KeyStoreException, CertificateException, InvalidTokenException {
        Key keyManagementKey = getKeyManagementKey(builder, jwtConfig);
        if (keyManagementKey == null) {
            String errorMsg = Tr.formatMessage(tc, "KEY_MANAGEMENT_KEY_NOT_FOUND", new Object[] { jwtConfig.getId(), jwtConfig.getKeyManagementKeyAlias(), jwtConfig.getTrustStoreRef() });
            throw new KeyStoreException(errorMsg);
        }
        jwe.setKey(keyManagementKey);
        setJweKidHeader(jwe, keyManagementKey);
    }

    Key getKeyManagementKey(BuilderImpl builder, JwtConfig jwtConfig) throws KeyStoreException, CertificateException, InvalidTokenException {
        Key keyManagementKey = builder.getKeyManagementKey();
        if (keyManagementKey == null) {
            keyManagementKey = getKeyManagementKeyFromTrustStore(jwtConfig);
        }
        return keyManagementKey;
    }

    PublicKey getKeyManagementKeyFromTrustStore(JwtConfig jwtConfig) throws KeyStoreException, CertificateException, InvalidTokenException {
        String keyAlias = jwtConfig.getKeyManagementKeyAlias();
        String trustStoreRef = jwtConfig.getTrustStoreRef();
        return JwtUtils.getPublicKey(keyAlias, trustStoreRef);
    }

    @Sensitive
    PrivateKey getJweDecryptionKey(JwtConsumerConfig config, MpConfigProperties mpConfigProps, String kid) throws Exception {
        String keyAlias = config.getKeyManagementKeyAlias();
        if (keyAlias != null) {
            // Server configuration takes precedence over MP Config property values
            String keyStoreRef = config.getKeyStoreRef();
            return JwtUtils.getPrivateKey(keyAlias, keyStoreRef);
        }
        return getJweDecryptionKeyFromMpConfigProps(config, mpConfigProps, kid);
    }

    @Sensitive
    private PrivateKey getJweDecryptionKeyFromMpConfigProps(JwtConsumerConfig config, MpConfigProperties mpConfigProps, String kid) throws Exception {
        String keyLocation = mpConfigProps.get(MpConfigProperties.DECRYPT_KEY_LOCATION);
        checkDecryptKeyLocationForInlineKey(keyLocation);
        JwKRetriever jwkRetriever = new JwKRetriever(config.getJwkSet());
        jwkRetriever.setSignatureAlgorithm(mpConfigProps.getConfiguredSignatureAlgorithm(config));
        jwkRetriever.setKeyLocation(keyLocation);
        return jwkRetriever.getPrivateKeyFromJwk(kid, config.getUseSystemPropertiesForHttpClientConnections());
    }

    void checkDecryptKeyLocationForInlineKey(@Sensitive String location) throws KeyException {
        if (location == null || location.isEmpty()) {
            return;
        }
        if (location.contains("BEGIN ")) {
            String errorMsg = Tr.formatMessage(tc, "DECRYPT_KEY_LOCATION_INLINE_KEY", new Object[] { MpConfigProperties.DECRYPT_KEY_LOCATION });
            throw new KeyException(errorMsg);
        }
    }

    void setJweKidHeader(JsonWebEncryption jwe, Key keyManagementKey) {
        JwkKidBuilder kidbuilder = new JwkKidBuilder();
        String keyId = kidbuilder.buildKeyId(keyManagementKey);
        if (keyId != null) {
            jwe.setKeyIdHeaderValue(keyId);
        }
    }

    void setJweHeaders(JsonWebEncryption jwe, BuilderImpl builder, JwtConfig jwtConfig) {
        jwe.setAlgorithmHeaderValue(getKeyManagementKeyAlgorithm(builder, jwtConfig));
        jwe.setEncryptionMethodHeaderParameter(getContentEncryptionAlgorithm(builder, jwtConfig));
        jwe.setHeader("typ", "JOSE");
        jwe.setHeader("cty", "jwt");
    }

    String getKeyManagementKeyAlgorithm(BuilderImpl builder, JwtConfig jwtConfig) {
        String keyManagementAlg = builder.getKeyManagementAlg();
        if (keyManagementAlg == null) {
            keyManagementAlg = getKeyManagementKeyAlgFromConfig(jwtConfig);
        }
        return keyManagementAlg;
    }

    String getKeyManagementKeyAlgFromConfig(JwtConfig jwtConfig) {
        String configuredKeyManagementAlg = jwtConfig.getKeyManagementKeyAlgorithm();
        if (configuredKeyManagementAlg == null) {
            configuredKeyManagementAlg = KeyManagementAlgorithmIdentifiers.RSA_OAEP;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Key management algorithm not specified in server config. Defaulting to [" + configuredKeyManagementAlg + "]");
            }
        }
        return configuredKeyManagementAlg;
    }

    String getContentEncryptionAlgorithm(BuilderImpl builder, JwtConfig jwtConfig) {
        String contentEncryptionAlg = builder.getContentEncryptionAlg();
        if (contentEncryptionAlg == null) {
            contentEncryptionAlg = getContentEncryptionAlgorithmFromConfig(jwtConfig);
        }
        return contentEncryptionAlg;
    }

    String getContentEncryptionAlgorithmFromConfig(JwtConfig jwtConfig) {
        String configuredContentEncryptionAlg = jwtConfig.getContentEncryptionAlgorithm();
        if (configuredContentEncryptionAlg == null) {
            configuredContentEncryptionAlg = ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Content encryption algorithm not specified in server config. Defaulting to [" + configuredContentEncryptionAlg + "]");
            }
        }
        return configuredContentEncryptionAlg;
    }

    String getJwtString(JsonWebEncryption jwe) throws JwtTokenException {
        String jwt = null;
        try {
            jwt = jwe.getCompactSerialization();
        } catch (Exception e) {
            throw new JwtTokenException(e.getLocalizedMessage(), e);
        }
        return jwt;
    }

}
