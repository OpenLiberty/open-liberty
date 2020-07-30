/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.InvalidJwtSignatureException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;
import org.jose4j.jwx.JsonWebStructure;
import org.jose4j.keys.HmacKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.InvalidClaimException;
import com.ibm.websphere.security.jwt.InvalidTokenException;
// import com.ibm.websphere.security.jwt.JwtConsumer;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.websphere.security.jwt.KeyException;
import com.ibm.websphere.security.jwt.KeyStoreServiceException;
import com.ibm.ws.security.common.jwk.impl.JwKRetriever;
import com.ibm.ws.security.common.time.TimeUtils;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.jwt.utils.Constants;
import com.ibm.ws.security.jwt.utils.JtiNonceCache;
import com.ibm.ws.security.jwt.utils.JwtUtils;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class ConsumerUtil {
    private static final TraceComponent tc = Tr.register(ConsumerUtil.class);
    private static final Class<?> thisClass = ConsumerUtil.class;

    private AtomicServiceReference<KeyStoreService> keyStoreService = null;

    private static TimeUtils timeUtils = new TimeUtils(TimeUtils.YearMonthDateHourMinSecZone);
    private final JtiNonceCache jtiCache = new JtiNonceCache();
    public final static String ISSUER = "mp.jwt.verify.issuer";
    public final static String PUBLIC_KEY = "mp.jwt.verify.publickey";
    public final static String KEY_LOCATION = "mp.jwt.verify.publickey.location";

    public ConsumerUtil(AtomicServiceReference<KeyStoreService> kss) {
        keyStoreService = kss;
    }

    public JwtToken parseJwt(String jwtString, JwtConsumerConfig config) throws Exception {
        return parseJwt(jwtString, config, null);
    }

    public JwtToken parseJwt(String jwtString, JwtConsumerConfig config, Map properties) throws Exception {
        JwtContext jwtContext = parseJwtAndGetJwtContext(jwtString, config, properties);
        JwtTokenConsumerImpl jwtToken = new JwtTokenConsumerImpl(jwtContext);
        checkForReusedJwt(jwtToken, config);
        return jwtToken;
    }

    JwtContext parseJwtAndGetJwtContext(String jwtString, JwtConsumerConfig config, Map properties) throws Exception {
        JwtContext jwtContext = parseJwtWithoutValidation(jwtString, config);
        if (config.isValidationRequired()) {
            jwtContext = getSigningKeyAndParseJwtWithValidation(jwtString, config, jwtContext, properties);
        }
        return jwtContext;
    }

    JwtContext getSigningKeyAndParseJwtWithValidation(String jwtString, JwtConsumerConfig config, JwtContext jwtContext,
            Map properties) throws Exception {
        Key signingKey = getSigningKey(config, jwtContext, properties);
        return parseJwtWithValidation(jwtString, jwtContext, config, signingKey, properties);
    }

    /**
     * Throws an exception if JWTs are not allowed to be reused (as configured by
     * the provided config option) AND a token with a matching "jti" and "issuer"
     * claim already exists in the cache.
     */
    void checkForReusedJwt(JwtTokenConsumerImpl jwt, JwtConsumerConfig config) throws InvalidTokenException {
        // Only throw an error if tokens are not allowed to be reused
        if (!config.getTokenReuse()) {
            throwExceptionIfJwtReused(jwt);
        }
    }

    void throwExceptionIfJwtReused(JwtTokenConsumerImpl jwt) throws InvalidTokenException {
        if (jtiCache.contains(jwt)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JWT token can only be submitted once. The issuer is " + jwt.getClaims().getIssuer()
                        + ", and JTI is " + jwt.getClaims().getJwtId());
            }
            String errorMsg = Tr.formatMessage(tc, "JWT_DUP_JTI_ERR",
                    new Object[] { jwt.getClaims().getIssuer(), jwt.getClaims().getJwtId() });
            throw new InvalidTokenException(errorMsg);
        }
    }

    /**
     * Get the appropriate signing key based on the signature algorithm specified in
     * the config.
     */
    Key getSigningKey(JwtConsumerConfig config, JwtContext jwtContext, Map properties) throws KeyException {
        Key signingKey = null;
        if (config == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JWT consumer config object is null");
            }
            return null;
        }
        signingKey = getSigningKeyBasedOnSignatureAlgorithm(config, jwtContext, properties);
        if (signingKey == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A signing key could not be found");
            }
        }
        return signingKey;
    }

    Key getSigningKeyBasedOnSignatureAlgorithm(JwtConsumerConfig config, JwtContext jwtContext, Map properties)
            throws KeyException {
        Key signingKey = null;
        String sigAlg = config.getSignatureAlgorithm();

        if (Constants.SIGNATURE_ALG_HS256.equals(sigAlg)) {
            signingKey = getSigningKeyForHS256(config);
        } else if (Constants.SIGNATURE_ALG_RS256.equals(sigAlg)) {
            signingKey = getSigningKeyForRS256(config, jwtContext, properties);
        } else if (Constants.SIGNATURE_ALG_ES256.equals(sigAlg)) {
            signingKey = getSigningKeyForES256(config, jwtContext, properties);
        }
        return signingKey;
    }

    Key getSigningKeyForHS256(JwtConsumerConfig config) throws KeyException {
        Key signingKey = null;
        try {
            signingKey = getSharedSecretKey(config);
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "JWT_ERROR_GETTING_SHARED_KEY", new Object[] { e.getLocalizedMessage() });
            throw new KeyException(msg, e);
        }
        return signingKey;
    }

    /**
     * Creates a Key object from the shared key specified in the provided
     * configuration.
     */
    Key getSharedSecretKey(JwtConsumerConfig config) throws KeyException {
        if (config == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JWT consumer config object is null");
            }
            return null;
        }
        String sharedKey = config.getSharedKey();
        return createKeyFromSharedKey(sharedKey);
    }

    Key createKeyFromSharedKey(String sharedKey) throws KeyException {
        if (sharedKey == null || sharedKey.isEmpty()) {
            String msg = Tr.formatMessage(tc, "JWT_MISSING_SHARED_KEY");
            throw new KeyException(msg);
        }
        try {
            return new HmacKey(sharedKey.getBytes(Constants.UTF_8));
        } catch (UnsupportedEncodingException e) {
            // Should not happen - UTF-8 should be supported
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting shared key bytes: " + e.getLocalizedMessage());
            }
        }
        return null;
    }

    boolean isPublicKeyPropsPresent(Map props) {
        if (props == null) {
            return false;
        }
        return props.get(PUBLIC_KEY) != null || props.get(KEY_LOCATION) != null;
    }

    Key getSigningKeyForRS256(JwtConsumerConfig config, JwtContext jwtContext, Map properties) throws KeyException {
        return getKeyFromJwkOrTrustStore(config, jwtContext, properties);
    }

    Key getKeyFromJwkOrTrustStore(JwtConsumerConfig config, JwtContext jwtContext, Map properties) throws KeyException {
        Key signingKey = null;
        if (config.getJwkEnabled() || (config.getTrustedAlias() == null && isPublicKeyPropsPresent(properties))) { // need change to consider MP-Config
            signingKey = getKeyForJwkEnabled(config, jwtContext, properties);
        } else {
            signingKey = getKeyForJwkDisabled(config);
        }
        return signingKey;
    }

    Key getKeyForJwkEnabled(JwtConsumerConfig config, JwtContext jwtContext, Map properties) throws KeyException {
        Key signingKey = null;
        try {
            signingKey = getJwksKey(config, jwtContext, properties);
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "JWT_ERROR_GETTING_JWK_KEY", new Object[] { config.getJwkEndpointUrl(), e.getLocalizedMessage() });
            throw new KeyException(msg, e);
        }
        return signingKey;
    }

    protected Key getJwksKey(JwtConsumerConfig config, JwtContext jwtContext, Map properties) throws Exception {
        JsonWebStructure jwtHeader = getJwtHeader(jwtContext);
        String kid = jwtHeader.getKeyIdHeaderValue();
        JwKRetriever jwkRetriever = null;
        if (properties != null) {
            String publickey = (String) properties.get(PUBLIC_KEY);
            String keyLocation = (String) properties.get(KEY_LOCATION);
            if (publickey != null || keyLocation != null) {
                jwkRetriever = new JwKRetriever(config.getId(), config.getSslRef(), config.getJwkEndpointUrl(),
                        config.getJwkSet(), JwtUtils.getSSLSupportService(), config.isHostNameVerificationEnabled(),
                        null, null, publickey, keyLocation);
            }
        }
        if (jwkRetriever == null) {
            jwkRetriever = new JwKRetriever(config.getId(), config.getSslRef(), config.getJwkEndpointUrl(),
                    config.getJwkSet(), JwtUtils.getSSLSupportService(), config.isHostNameVerificationEnabled(), null,
                    null);
        }
        Key signingKey = jwkRetriever.getPublicKeyFromJwk(kid, null,
                config.getUseSystemPropertiesForHttpClientConnections()); // only kid or x5t will work but not both
        return signingKey;
    }

    JsonWebStructure getJwtHeader(JwtContext jwtContext) throws Exception {
        List<JsonWebStructure> jsonStructures = jwtContext.getJoseObjects();
        if (jsonStructures == null || jsonStructures.isEmpty()) {
            // TODO - NLS message
            throw new Exception("Invalid JsonWebStructure");
        }
        JsonWebStructure jwtHeader = jsonStructures.get(0);
        debugJwtHeader(jwtHeader);
        return jwtHeader;
    }

    void debugJwtHeader(JsonWebStructure jwtHeader) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "JsonWebStructure class: " + jwtHeader.getClass().getName() + " data:" + jwtHeader);
            if (jwtHeader instanceof JsonWebSignature) {
                JsonWebSignature signature = (JsonWebSignature) jwtHeader;
                Tr.debug(tc, "JsonWebSignature alg: " + signature.getAlgorithmHeaderValue() + " 3rd:'"
                        + signature.getEncodedSignature() + "'");
            }
        }
    }

    Key getKeyForJwkDisabled(JwtConsumerConfig config) throws KeyException {
        Key signingKey = null;
        String trustedAlias = config.getTrustedAlias();
        String trustStoreRef = config.getTrustStoreRef();
        try {
            signingKey = getPublicKey(trustedAlias, trustStoreRef, config.getSignatureAlgorithm());
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "JWT_ERROR_GETTING_PUBLIC_KEY", new Object[] { trustedAlias, trustStoreRef, e.getLocalizedMessage() });
            throw new KeyException(msg, e);
        }
        return signingKey;
    }

    /**
     * Creates a Key object from the certificate stored in the trust store and alias
     * provided.
     */
    Key getPublicKey(String trustedAlias, String trustStoreRef, String signatureAlgorithm) throws KeyStoreServiceException, KeyException {
        Key signingKey = getPublicKeyFromKeystore(trustedAlias, trustStoreRef, signatureAlgorithm);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Trusted alias: " + trustedAlias + ", Truststore: " + trustStoreRef);
            Tr.debug(tc, "RSAPublicKey: " + (signingKey instanceof RSAPublicKey));
        }
        if (signingKey != null && !(signingKey instanceof PublicKey)) {
            signingKey = null;
        }
        return signingKey;
    }

    Key getPublicKeyFromKeystore(String trustedAlias, String trustStoreRef, String signatureAlgorithm) throws KeyException {
        try {
            if (keyStoreService == null) {
                String msg = Tr.formatMessage(tc, "JWT_TRUSTSTORE_SERVICE_NOT_AVAILABLE");
                throw new KeyStoreServiceException(msg);
            }
            return JwtUtils.getPublicKey(trustedAlias, trustStoreRef, keyStoreService.getService());
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "JWT_NULL_SIGNING_KEY_WITH_ERROR", new Object[] { signatureAlgorithm, Constants.SIGNING_KEY_X509, e.getLocalizedMessage() });
            throw new KeyException(msg, e);
        }
    }

    Key getSigningKeyForES256(JwtConsumerConfig config, JwtContext jwtContext, Map properties) throws KeyException {
        return getKeyFromJwkOrTrustStore(config, jwtContext, properties);
    }

    protected JwtContext parseJwtWithoutValidation(String jwtString, JwtConsumerConfig config) throws Exception {
        if (jwtString == null || jwtString.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "JWT_CONSUMER_NULL_OR_EMPTY_STRING",
                    new Object[] { config.getId(), jwtString });
            throw new InvalidTokenException(errorMsg);
        }
        JwtConsumerBuilder builder = initializeJwtConsumerBuilderWithoutValidation(config);
        JwtConsumer firstPassJwtConsumer = builder.build();
        return firstPassJwtConsumer.process(jwtString);
    }

    protected JwtContext parseJwtWithValidation(String jwtString, JwtContext jwtContext, JwtConsumerConfig config,
            Key key, Map properties) throws Exception {
        JwtClaims jwtClaims = jwtContext.getJwtClaims();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Key from config: " + key);
        }

        validateClaims(jwtClaims, jwtContext, config, properties);
        validateSignatureAlgorithmWithKey(config, key);

        JwtConsumerBuilder consumerBuilder = initializeJwtConsumerBuilderWithValidation(config, jwtClaims, key);
        JwtConsumer jwtConsumer = consumerBuilder.build();
        return processJwtStringWithConsumer(jwtConsumer, jwtString);
    }

    JwtConsumerBuilder initializeJwtConsumerBuilderWithoutValidation(JwtConsumerConfig config) {
        JwtConsumerBuilder builder = new JwtConsumerBuilder();
        builder.setSkipAllValidators();
        builder.setDisableRequireSignature();
        builder.setSkipSignatureVerification();
        builder.setAllowedClockSkewInSeconds((int) ((config.getClockSkew()) / 1000));
        return builder;
    }

    JwtConsumerBuilder initializeJwtConsumerBuilderWithValidation(JwtConsumerConfig config, JwtClaims jwtClaims,
            Key key) throws MalformedClaimException {
        JwtConsumerBuilder builder = new JwtConsumerBuilder();
        builder.setExpectedIssuer(jwtClaims.getIssuer());
        builder.setSkipDefaultAudienceValidation();
        builder.setRequireExpirationTime();
        builder.setVerificationKey(key);
        builder.setRelaxVerificationKeyValidation();
        builder.setAllowedClockSkewInSeconds((int) (config.getClockSkew() / 1000));
        return builder;
    }

    void validateClaims(JwtClaims jwtClaims, JwtContext jwtContext, JwtConsumerConfig config, Map properties)
            throws MalformedClaimException, InvalidClaimException, InvalidTokenException {
        String issuer = config.getIssuer();
        if (issuer == null) {
            issuer = (properties == null) ? null : (String) properties.get(ISSUER);
        }

        validateIssuer(config.getId(), issuer, jwtClaims.getIssuer());

        if (!validateAudience(config.getAudiences(), jwtClaims.getAudience())) {
            String msg = Tr.formatMessage(tc, "JWT_AUDIENCE_NOT_TRUSTED",
                    new Object[] { jwtClaims.getAudience(), config.getId(), config.getAudiences() });
            throw new InvalidClaimException(msg);
        }

        // check azp

        validateIatAndExp(jwtClaims, config.getClockSkew());

        validateNbf(jwtClaims, config.getClockSkew());

        validateAlgorithm(jwtContext, config.getSignatureAlgorithm());
    }

    /**
     * Throws an exception if the provided key is null but the config specifies a
     * signature algorithm other than "none".
     */
    void validateSignatureAlgorithmWithKey(JwtConsumerConfig config, Key key) throws InvalidClaimException {
        String signatureAlgorithm = config.getSignatureAlgorithm();
        if (key == null && signatureAlgorithm != null && !signatureAlgorithm.equalsIgnoreCase("none")) {
            String msg = Tr.formatMessage(tc, "JWT_MISSING_KEY", new Object[] { signatureAlgorithm });
            throw new InvalidClaimException(msg);
        }
    }

    /**
     * Verifies that tokenIssuer is one of the values specified in the
     * comma-separated issuers string.
     */
    boolean validateIssuer(String consumerConfigId, String issuers, String tokenIssuer) throws InvalidClaimException {
        boolean isIssuer = false;
        if (issuers == null || issuers.isEmpty()) {
            String msg = Tr.formatMessage(tc, "JWT_TRUSTED_ISSUERS_NULL",
                    new Object[] { tokenIssuer, consumerConfigId });
            throw new InvalidClaimException(msg);
        }

        StringTokenizer st = new StringTokenizer(issuers, ",");
        while (st.hasMoreTokens()) {
            String iss = st.nextToken().trim();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Trusted issuer: " + iss);
            }
            if (Constants.ALL_ISSUERS.equals(iss) || (tokenIssuer != null && tokenIssuer.equals(iss))) {
                isIssuer = true;
                break;
            }
        }

        if (!isIssuer) {
            String msg = Tr.formatMessage(tc, "JWT_ISSUER_NOT_TRUSTED",
                    new Object[] { tokenIssuer, consumerConfigId, issuers });
            throw new InvalidClaimException(msg);
        }
        return isIssuer;
    }

    /**
     * Verifies that at least one of the values specified in audiences is contained
     * in the allowedAudiences list.
     */
    boolean validateAudience(List<String> allowedAudiences, List<String> audiences) {
        boolean valid = false;

        if (allowedAudiences != null && allowedAudiences.contains(Constants.ALL_AUDIENCES)) {
            return true;
        }
        if (allowedAudiences != null && audiences != null) {
            for (String audience : audiences) {
                for (String allowedAud : allowedAudiences) {
                    if (allowedAud.equals(audience)) {
                        valid = true;
                        break;
                    }
                }
            }
        } else if (allowedAudiences == null && (audiences == null || audiences.isEmpty())) {
            valid = true;
        }

        return valid;
    }

    /**
     * Validates the the {@value Claims#ISSUED_AT} and {@value Claims#EXPIRATION}
     * claims are present and properly formed. Also verifies that the
     * {@value Claims#ISSUED_AT} time is after the {@value Claims#EXPIRATION} time.
     */
    void validateIatAndExp(JwtClaims jwtClaims, long clockSkewInMilliseconds) throws InvalidClaimException {
        if (jwtClaims == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Missing JwtClaims object");
            }
            return;
        }
        NumericDate issueAtClaim = getIssuedAtClaim(jwtClaims);
        NumericDate expirationClaim = getExpirationClaim(jwtClaims);

        debugCurrentTimes(clockSkewInMilliseconds, issueAtClaim, expirationClaim);

        validateIssuedAtClaim(issueAtClaim, expirationClaim, clockSkewInMilliseconds);
        validateExpirationClaim(expirationClaim, clockSkewInMilliseconds);

    }

    void debugCurrentTimes(long clockSkewInMilliseconds, NumericDate issueAtClaim, NumericDate expirationClaim) {
        if (tc.isDebugEnabled()) {
            long now = (new Date()).getTime();
            NumericDate currentTimeMinusSkew = NumericDate.fromMilliseconds(now - clockSkewInMilliseconds);
            NumericDate currentTimePlusSkew = NumericDate.fromMilliseconds(now + clockSkewInMilliseconds);
            Tr.debug(tc, "Checking iat [" + createDateString(issueAtClaim) + "] and exp ["
                    + createDateString(expirationClaim) + "]");
            Tr.debug(tc, "Comparing against current time (minus clock skew of " + (clockSkewInMilliseconds / 1000)
                    + " seconds) [" + createDateString(currentTimeMinusSkew) + "]");
            Tr.debug(tc, "Comparing against current time (plus clock skew of " + (clockSkewInMilliseconds / 1000)
                    + " seconds) [" + createDateString(currentTimePlusSkew) + "]");
        }
    }

    void validateIssuedAtClaim(NumericDate issueAtClaim, NumericDate expirationClaim, long clockSkewInMilliseconds)
            throws InvalidClaimException {
        long now = (new Date()).getTime();
        NumericDate currentTimePlusSkew = NumericDate.fromMilliseconds(now + clockSkewInMilliseconds);

        if (issueAtClaim != null && expirationClaim != null) {
            if (issueAtClaim.isAfter(currentTimePlusSkew)) {
                String msg = Tr.formatMessage(tc, "JWT_IAT_AFTER_CURRENT_TIME",
                        new Object[] { createDateString(issueAtClaim), createDateString(currentTimePlusSkew),
                                (clockSkewInMilliseconds / 1000) });
                throw new InvalidClaimException(msg);
            }
            if (issueAtClaim.isOnOrAfter(expirationClaim)) {
                String msg = Tr.formatMessage(tc, "JWT_IAT_AFTER_EXP",
                        new Object[] { createDateString(issueAtClaim), createDateString(expirationClaim) });
                throw new InvalidClaimException(msg);
            }
        } else {
            // TODO - what if one or the other is missing? is that an error
            // condition?
        }
    }

    void validateExpirationClaim(NumericDate expirationClaim, long clockSkewInMilliseconds)
            throws InvalidClaimException {
        long now = (new Date()).getTime();
        NumericDate currentTimeMinusSkew = NumericDate.fromMilliseconds(now - clockSkewInMilliseconds);

        // Check that expiration claim is in the future, accounting for the
        // clock skew
        if (expirationClaim == null || (!expirationClaim.isAfter(currentTimeMinusSkew))) {
            JwtUtils.setJwtSsoValidationPathExiredToken();
            String msg = Tr.formatMessage(tc, "JWT_TOKEN_EXPIRED", new Object[] { createDateString(expirationClaim),
                    createDateString(currentTimeMinusSkew), (clockSkewInMilliseconds / 1000) });
            throw new InvalidClaimException(msg);
        }
    }

    /**
     * Validates the the {@value Claims#NOT_BEFORE} claim is present and properly
     * formed. Also
     */
    void validateNbf(JwtClaims jwtClaims, long clockSkewInMilliseconds) throws InvalidClaimException {
        if (jwtClaims == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Missing JwtClaims object");
            }
            return;
        }
        NumericDate nbf = getNotBeforeClaim(jwtClaims);
        validateNotBeforeClaim(nbf, clockSkewInMilliseconds);
    }

    void validateNotBeforeClaim(NumericDate nbfClaim, long clockSkewInMilliseconds) throws InvalidClaimException {
        long now = (new Date()).getTime();
        NumericDate currentTimePlusSkew = NumericDate.fromMilliseconds(now + clockSkewInMilliseconds);

        // Check that nbf claim is in the past, accounting for the clock skew
        if (nbfClaim != null && (nbfClaim.isOnOrAfter(currentTimePlusSkew))) {
            String msg = Tr.formatMessage(tc, "JWT_TOKEN_BEFORE_NBF", new Object[] { createDateString(nbfClaim),
                    createDateString(currentTimePlusSkew), (clockSkewInMilliseconds / 1000) });
            throw new InvalidClaimException(msg);
        }
    }

    NumericDate getIssuedAtClaim(JwtClaims jwtClaims) throws InvalidClaimException {
        NumericDate iatClaim = null;
        try {
            iatClaim = jwtClaims.getIssuedAt();
        } catch (MalformedClaimException e) {
            String msg = Tr.formatMessage(tc, "JWT_CONSUMER_MALFORMED_CLAIM",
                    new Object[] { Claims.ISSUED_AT, e.getLocalizedMessage() });
            throw new InvalidClaimException(msg, e);
        }
        return iatClaim;
    }

    NumericDate getExpirationClaim(JwtClaims jwtClaims) throws InvalidClaimException {
        NumericDate expClaim = null;
        try {
            expClaim = jwtClaims.getExpirationTime();
        } catch (MalformedClaimException e) {
            String msg = Tr.formatMessage(tc, "JWT_CONSUMER_MALFORMED_CLAIM",
                    new Object[] { Claims.EXPIRATION, e.getLocalizedMessage() });
            throw new InvalidClaimException(msg, e);
        }
        return expClaim;
    }

    NumericDate getNotBeforeClaim(JwtClaims jwtClaims) throws InvalidClaimException {
        NumericDate nbfClaim = null;
        try {
            nbfClaim = jwtClaims.getNotBefore();
        } catch (MalformedClaimException e) {
            String msg = Tr.formatMessage(tc, "JWT_CONSUMER_MALFORMED_CLAIM",
                    new Object[] { Claims.NOT_BEFORE, e.getLocalizedMessage() });
            throw new InvalidClaimException(msg, e);
        }
        return nbfClaim;
    }

    void validateAlgorithm(JwtContext jwtContext, String requiredAlg) throws InvalidTokenException {
        if (requiredAlg == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No required signature algorithm was specified");
            }
            return;
        }
        String tokenAlg = getAlgorithmFromJwtHeader(jwtContext);
        validateAlgorithm(requiredAlg, tokenAlg);
    }

    void validateAlgorithm(String requiredAlg, String tokenAlg) throws InvalidTokenException {
        if (tokenAlg == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Signature algorithm was not found in the JWT");
            }
            String msg = Tr.formatMessage(tc, "JWT_MISSING_ALG_HEADER", new Object[] { requiredAlg });
            throw new InvalidTokenException(msg);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JWT is signed with algorithm: ", tokenAlg);
            Tr.debug(tc, "JWT is required to be signed with algorithm: ", requiredAlg);
        }
        if (!requiredAlg.equals(tokenAlg)) {
            String msg = Tr.formatMessage(tc, "JWT_ALGORITHM_MISMATCH", new Object[] { tokenAlg, requiredAlg });
            throw new InvalidTokenException(msg);
        }
    }

    JwtContext processJwtStringWithConsumer(JwtConsumer jwtConsumer, String jwtString)
            throws InvalidTokenException, InvalidJwtException {
        JwtContext validatedJwtContext = null;
        try {
            validatedJwtContext = jwtConsumer.process(jwtString);
        } catch (InvalidJwtSignatureException e) {
            String msg = Tr.formatMessage(tc, "JWT_INVALID_SIGNATURE", new Object[] { e.getLocalizedMessage() });
            throw new InvalidTokenException(msg, e);
        } catch (InvalidJwtException e) {
            Throwable cause = getRootCause(e);
            if (cause != null && cause instanceof InvalidKeyException) {
                throw e;
            } else {
                // Don't have enough information to output a more useful error
                // message
                throw e;
            }
        }
        return validatedJwtContext;
    }

    String getAlgorithmFromJwtHeader(JwtContext jwtContext) {
        if (jwtContext == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JwtContext is null");
            }
            return null;
        }
        JsonWebStructure jwtHeader = null;
        try {
            jwtHeader = getJwtHeader(jwtContext);
        } catch (Exception e) {
            // TODO - NLS message?
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to obtain JWT header");
            }
            return null;
        }
        String algHeader = jwtHeader.getAlgorithmHeaderValue();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JWT is signed with algorithm: ", algHeader);
        }
        return algHeader;
    }

    Throwable getRootCause(Exception e) {
        Throwable rootCause = null;
        Throwable tmpCause = e;
        while (tmpCause != null) {
            rootCause = tmpCause;
            tmpCause = rootCause.getCause();
        }
        return rootCause;
    }

    String createDateString(NumericDate date) {
        if (date == null) {
            return null;
        }
        // NumericDate.getValue() returns a value in seconds, so convert to
        // milliseconds
        return timeUtils.createDateString(1000 * date.getValue());
    }

}
