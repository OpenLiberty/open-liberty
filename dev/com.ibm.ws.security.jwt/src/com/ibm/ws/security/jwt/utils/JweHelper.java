/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.jwt.utils;

import java.security.Key;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jose4j.base64url.Base64;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.websphere.security.jwt.KeyException;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.ws.security.common.jwk.impl.JwkKidBuilder;
import com.ibm.ws.security.jwt.config.JwtConfig;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.jwt.config.MpConfigProperties;
import com.ibm.ws.security.jwt.internal.BuilderImpl;
import com.ibm.ws.security.jwt.internal.JwtTokenException;

public class JweHelper {

    private static final TraceComponent tc = Tr.register(JweHelper.class);

    private static final String NOT_PERIOD = "[^\\.]";
    private static final Pattern JWS_PATTERN = Pattern.compile("^(" + NOT_PERIOD + "*\\.){2}" + NOT_PERIOD + "*$");
    private static final Pattern JWE_PATTERN = Pattern.compile("^(" + NOT_PERIOD + "*\\.){4}" + NOT_PERIOD + "*$");

    @FFDCIgnore({ Exception.class })
    public static String createJweString(String jws, JwtData jwtData) throws Exception {
        JwtConfig jwtConfig = jwtData.getConfig();
        try {
            JsonWebEncryption jwe = new JsonWebEncryption();
            BuilderImpl builder = jwtData.getBuilder();
            setJweKeyData(jwe, builder, jwtConfig);
            setJweHeaders(jwe, builder, jwtConfig);
            jwe.setPayload(jws);
            return getJwtString(jwe);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "ERROR_BUILDING_SIGNED_JWE", new Object[] { jwtConfig.getId(), e });
            throw new Exception(errorMsg, e);
        }
    }

    public static boolean isJws(String jwtString) {
        if (jwtString == null || jwtString.isEmpty()) {
            return false;
        }

        Matcher m = JWS_PATTERN.matcher(jwtString);
        return m.matches();
    }

    public static boolean isJwe(String jwtString) {
        if (jwtString == null || jwtString.isEmpty()) {
            return false;
        }

        Matcher m = JWE_PATTERN.matcher(jwtString);
        return m.matches();
    }

    /**
     * Returns whether the given configuration must only accept JWS tokens. If the keyManagementKeyAlias config attribute is NOT
     * set, then we must only accept JWS tokens; tokens in JWE format should be rejected.
     */
    public static boolean isJwsRequired(JwtConsumerConfig config) {
        return !isJweRequired(config);
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
     * Returns whether the given configuration must only accept JWE tokens. If the keyManagementKeyAlias config attribute is set,
     * then we must only accept JWE tokens; tokens in JWS format should be rejected.
     */
    public static boolean isJweRequired(JwtConsumerConfig config) {
        String keyAlias = config.getKeyManagementKeyAlias();
        return (keyAlias != null);
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

    public static String extractJwsFromJweToken(String jweString, JwtConsumerConfig config, MpConfigProperties mpConfigProps) throws InvalidTokenException {
        JwtClaims jweHeaderParameters = getJweHeaderParams(jweString);
        return extractJwsFromJweToken(jweString, config, mpConfigProps, jweHeaderParameters);
    }

    public static String extractJwsFromJweToken(String jweString, JwtConsumerConfig config, MpConfigProperties mpConfigProps, JwtClaims jweHeaderParameters) throws InvalidTokenException {
        String payload = extractPayloadFromJweToken(jweString, config, mpConfigProps, jweHeaderParameters);
        if (!isJws(payload)) {
            String errorMsg = Tr.formatMessage(tc, "NESTED_JWS_REQUIRED_BUT_NOT_FOUND");
            throw new InvalidTokenException(errorMsg);
        }
        return payload;
    }

    public static String extractPayloadFromJweToken(String jweString, JwtConsumerConfig config, MpConfigProperties mpConfigProps) throws InvalidTokenException {
        JwtClaims jweHeaderParameters = JweHelper.getJweHeaderParams(jweString);
        return extractPayloadFromJweToken(jweString, config, mpConfigProps, jweHeaderParameters);
    }

    @FFDCIgnore({ Exception.class })
    public static String extractPayloadFromJweToken(String jweString, JwtConsumerConfig config, MpConfigProperties mpConfigProps, JwtClaims jweHeaderParameters) throws InvalidTokenException {
        String payload = null;
        try {
            payload = getJwePayload(jweString, config, mpConfigProps, jweHeaderParameters);
        } catch (Exception e) {
            String errorMsg = Tr.formatMessage(tc, "ERROR_EXTRACTING_JWS_PAYLOAD_FROM_JWE", new Object[] { config.getId(), e });
            throw new InvalidTokenException(errorMsg, e);
        }
        return payload;
    }

    static String getJwePayload(String jweString, JwtConsumerConfig config, MpConfigProperties mpConfigProps, JwtClaims jweHeaderParameters) throws Exception {
        Key decryptionKey = getJweDecryptionKey(config, mpConfigProps, (String) jweHeaderParameters.getClaimValue("kid"));
        if (decryptionKey == null) {
            String errorMsg = Tr.formatMessage(tc, "JWE_DECRYPTION_KEY_MISSING", new Object[] { JwtUtils.CFG_KEY_KEY_MANAGEMENT_KEY_ALIAS, config.getKeyManagementKeyAlias() });
            throw new InvalidTokenException(errorMsg);
        }
        return getJwePayload(jweString, decryptionKey);
    }

    static String getJwePayload(String jweString, @Sensitive Key decryptionKey) throws JoseException, InvalidTokenException {
        JsonWebEncryption jwe = new JsonWebEncryption();
        jwe.setCompactSerialization(jweString);
        jwe.setKey(decryptionKey);
        String payload = null;
        Object token = ThreadIdentityManager.runAsServer();
        try {
            payload = jwe.getPayload();
        } finally {
            ThreadIdentityManager.reset(token);
        }
        if (isJws(payload)) {
            verifyContentType(jwe);
        }
        return payload;
    }

    static void verifyContentType(JsonWebEncryption jwe) throws InvalidTokenException {
        String requiredContentType = "JWT";
        String cty = jwe.getContentTypeHeaderValue();
        if (cty == null || !requiredContentType.equalsIgnoreCase(cty)) {
            String errorMsg = Tr.formatMessage(tc, "CTY_NOT_JWT_FOR_NESTED_JWS", new Object[] { "\"cty\"", cty, "\"" + requiredContentType + "\"" });
            throw new InvalidTokenException(errorMsg);
        }
    }

    @FFDCIgnore(Exception.class)
    public static JwtClaims getJweHeaderParams(String jweString) {
        JwtClaims jweHeaderParameters = null;
        try {
            String headerString = jweString.substring(0, jweString.indexOf('.'));
            headerString = new String(Base64.decode(headerString));
            jweHeaderParameters = JwtClaims.parse(headerString);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting header from JWE string: " + e);
            }
        }
        return jweHeaderParameters == null ? new JwtClaims() : jweHeaderParameters;
    }

    String getKidFromJweString(String jweString) {
        JwtClaims jweHeaderParameters = getJweHeaderParams(jweString);
        return (String) jweHeaderParameters.getClaimValue("kid");
    }

    static void setJweKeyData(JsonWebEncryption jwe, BuilderImpl builder, JwtConfig jwtConfig) throws KeyStoreException, CertificateException, InvalidTokenException {
        Key keyManagementKey = getKeyManagementKey(builder, jwtConfig);
        if (keyManagementKey == null) {
            String errorMsg = Tr.formatMessage(tc, "KEY_MANAGEMENT_KEY_NOT_FOUND", new Object[] { jwtConfig.getId(), jwtConfig.getKeyManagementKeyAlias(), jwtConfig.getTrustStoreRef() });
            throw new KeyStoreException(errorMsg);
        }
        jwe.setKey(keyManagementKey);
        setJweKidHeader(jwe, keyManagementKey);
    }

    static Key getKeyManagementKey(BuilderImpl builder, JwtConfig jwtConfig) throws KeyStoreException, CertificateException, InvalidTokenException {
        Key keyManagementKey = builder.getKeyManagementKey();
        if (keyManagementKey == null) {
            keyManagementKey = getKeyManagementKeyFromTrustStore(jwtConfig);
        }
        return keyManagementKey;
    }

    static PublicKey getKeyManagementKeyFromTrustStore(JwtConfig jwtConfig) throws KeyStoreException, CertificateException, InvalidTokenException {
        String keyAlias = jwtConfig.getKeyManagementKeyAlias();
        String trustStoreRef = jwtConfig.getTrustStoreRef();
        return JwtUtils.getPublicKey(keyAlias, trustStoreRef);
    }

    @Sensitive
    static Key getJweDecryptionKey(JwtConsumerConfig config, MpConfigProperties mpConfigProps, String kid) throws Exception {
        Key key = config.getJweDecryptionKey();
        if (key != null) {
            // Server configuration takes precedence over MP Config property values
            return key;
        }
        return getJweDecryptionKeyFromMpConfigProps(config, mpConfigProps, kid);
    }

    @Sensitive
    private static Key getJweDecryptionKeyFromMpConfigProps(JwtConsumerConfig config, MpConfigProperties mpConfigProps, String kid) throws Exception {
        if (mpConfigProps == null) {
            return null;
        }
        String keyLocation = mpConfigProps.get(MpConfigProperties.DECRYPT_KEY_LOCATION);
        checkDecryptKeyLocationForInlineKey(keyLocation);
        JwKRetriever jwkRetriever = new JwKRetriever(config.getJwkSet());
        jwkRetriever.setSignatureAlgorithm(mpConfigProps.getConfiguredSignatureAlgorithm(config));
        jwkRetriever.setKeyLocation(keyLocation);
        return jwkRetriever.getPrivateKeyFromJwk(kid, config.getUseSystemPropertiesForHttpClientConnections());
    }

    static void checkDecryptKeyLocationForInlineKey(@Sensitive String location) throws KeyException {
        if (location == null || location.isEmpty()) {
            return;
        }
        if (location.contains("BEGIN ")) {
            String errorMsg = Tr.formatMessage(tc, "DECRYPT_KEY_LOCATION_INLINE_KEY", new Object[] { MpConfigProperties.DECRYPT_KEY_LOCATION });
            throw new KeyException(errorMsg);
        }
    }

    static void setJweKidHeader(JsonWebEncryption jwe, Key keyManagementKey) {
        JwkKidBuilder kidbuilder = new JwkKidBuilder();
        String keyId = kidbuilder.buildKeyId(keyManagementKey);
        if (keyId != null) {
            jwe.setKeyIdHeaderValue(keyId);
        }
    }

    static void setJweHeaders(JsonWebEncryption jwe, BuilderImpl builder, JwtConfig jwtConfig) {
        jwe.setAlgorithmHeaderValue(getKeyManagementKeyAlgorithm(builder, jwtConfig));
        jwe.setEncryptionMethodHeaderParameter(getContentEncryptionAlgorithm(builder, jwtConfig));
        jwe.setHeader("typ", "JOSE");
        jwe.setHeader("cty", "jwt");
    }

    static String getKeyManagementKeyAlgorithm(BuilderImpl builder, JwtConfig jwtConfig) {
        String keyManagementAlg = builder.getKeyManagementAlg();
        if (keyManagementAlg == null) {
            keyManagementAlg = getKeyManagementKeyAlgFromConfig(jwtConfig);
        }
        return keyManagementAlg;
    }

    static String getKeyManagementKeyAlgFromConfig(JwtConfig jwtConfig) {
        String configuredKeyManagementAlg = jwtConfig.getKeyManagementKeyAlgorithm();
        if (configuredKeyManagementAlg == null) {
            // If FIPS140-3 is enabled, use ECDH-ES as the default, else use RSA-OAEP
            configuredKeyManagementAlg = CryptoUtils.isFips140_3EnabledWithBetaGuard() ? KeyManagementAlgorithmIdentifiers.ECDH_ES: KeyManagementAlgorithmIdentifiers.RSA_OAEP;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Key management algorithm not specified in server config. Defaulting to [" + configuredKeyManagementAlg + "]");
            }
        }
        return configuredKeyManagementAlg;
    }

    static String getContentEncryptionAlgorithm(BuilderImpl builder, JwtConfig jwtConfig) {
        String contentEncryptionAlg = builder.getContentEncryptionAlg();
        if (contentEncryptionAlg == null) {
            contentEncryptionAlg = getContentEncryptionAlgorithmFromConfig(jwtConfig);
        }
        return contentEncryptionAlg;
    }

    static String getContentEncryptionAlgorithmFromConfig(JwtConfig jwtConfig) {
        String configuredContentEncryptionAlg = jwtConfig.getContentEncryptionAlgorithm();
        if (configuredContentEncryptionAlg == null) {
            configuredContentEncryptionAlg = ContentEncryptionAlgorithmIdentifiers.AES_256_GCM;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Content encryption algorithm not specified in server config. Defaulting to [" + configuredContentEncryptionAlg + "]");
            }
        }
        return configuredContentEncryptionAlg;
    }

    static String getJwtString(JsonWebEncryption jwe) throws JwtTokenException {
        String jwt = null;
        Object token = ThreadIdentityManager.runAsServer();
        try {
            jwt = jwe.getCompactSerialization();
        } catch (Exception e) {
            throw new JwtTokenException(e.getLocalizedMessage(), e);
        } finally {
            ThreadIdentityManager.reset(token);
        }
        return jwt;
    }

}
