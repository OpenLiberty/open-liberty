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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import com.ibm.ws.security.common.crypto.KeyAlgorithmChecker;
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

    private AtomicServiceReference<KeyStoreService> keyStoreService = null;

    private static TimeUtils timeUtils = new TimeUtils(TimeUtils.YearMonthDateHourMinSecZone);
    private final JtiNonceCache jtiCache = new JtiNonceCache();
    public final static String ISSUER = "mp.jwt.verify.issuer";
    public final static String PUBLIC_KEY = "mp.jwt.verify.publickey";
    public final static String KEY_LOCATION = "mp.jwt.verify.publickey.location";

    // Properties added by MP JWT 1.2 specification
    public final static String PUBLIC_KEY_ALG = "mp.jwt.verify.publickey.algorithm";
    public final static String DECRYPT_KEY_LOCATION = "mp.jwt.decrypt.key.location";
    public final static String VERIFY_AUDIENCES = "mp.jwt.verify.audiences";
    public final static String TOKEN_HEADER = "mp.jwt.token.header";
    public final static String TOKEN_COOKIE = "mp.jwt.token.cookie";

    KeyAlgorithmChecker keyAlgChecker = new KeyAlgorithmChecker();

    private Map<String, String> mpConfigProps = null;

    public ConsumerUtil(AtomicServiceReference<KeyStoreService> kss) {
        keyStoreService = kss;
    }

    public void setMpConfigProps(Map<String, String> props) {
        if (props != null) {
            mpConfigProps = new HashMap<String, String>(props);
        }
    }

    public JwtToken parseJwt(String jwtString, JwtConsumerConfig config) throws Exception {
        return parseJwt(jwtString, config, null);
    }

    public JwtToken parseJwt(String jwtString, JwtConsumerConfig config, Map<String, String> properties) throws Exception {
        setMpConfigProps(properties);
        JwtContext jwtContext = parseJwtAndGetJwtContext(jwtString, config);
        JwtTokenConsumerImpl jwtToken = new JwtTokenConsumerImpl(jwtContext);
        checkForReusedJwt(jwtToken, config);
        return jwtToken;
    }

    public String getConfiguredSignatureAlgorithm(JwtConsumerConfig config) {
        String signatureAlgorithm = config.getSignatureAlgorithm();
        if (signatureAlgorithm != null) {
            // Server configuration takes precedence over MP Config property values
            return signatureAlgorithm;
        }
        return getSignatureAlgorithmFromMpConfigProps();
    }

    JwtContext parseJwtAndGetJwtContext(String jwtString, JwtConsumerConfig config) throws Exception {
        JwtContext jwtContext = parseJwtWithoutValidation(jwtString, config);
        if (config.isValidationRequired()) {
            jwtContext = getSigningKeyAndParseJwtWithValidation(jwtString, config, jwtContext);
        }
        return jwtContext;
    }

    JwtContext getSigningKeyAndParseJwtWithValidation(String jwtString, JwtConsumerConfig config, JwtContext jwtContext) throws Exception {
        Key signingKey = getSigningKey(config, jwtContext);
        return parseJwtWithValidation(jwtString, jwtContext, config, signingKey);
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
    Key getSigningKey(JwtConsumerConfig config, JwtContext jwtContext) throws KeyException {
        Key signingKey = null;
        if (config == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JWT consumer config object is null");
            }
            return null;
        }
        signingKey = getSigningKeyBasedOnSignatureAlgorithm(config, jwtContext);
        if (signingKey == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A signing key could not be found");
            }
        }
        return signingKey;
    }

    Key getSigningKeyBasedOnSignatureAlgorithm(JwtConsumerConfig config, JwtContext jwtContext)
            throws KeyException {
        Key signingKey = null;
        String sigAlg = getConfiguredSignatureAlgorithm(config);

        if (keyAlgChecker.isHSAlgorithm(sigAlg)) {
            signingKey = getSigningKeyForHS(sigAlg, config);
        } else if (keyAlgChecker.isRSAlgorithm(sigAlg)) {
            signingKey = getSigningKeyForRS(config, jwtContext);
        } else if (keyAlgChecker.isESAlgorithm(sigAlg)) {
            signingKey = getSigningKeyForES(config, jwtContext);
        }
        if (isAsymmetricAlgorithm(sigAlg)) {
            if (!keyAlgChecker.isPublicKeyValidType(signingKey, sigAlg)) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Public key " + signingKey + " does not match the parameters of the " + sigAlg + " algorithm");
                }
                return null;
            }
        }
        return signingKey;
    }

    String getSignatureAlgorithmFromMpConfigProps() {
        String defaultAlg = "RS256";
        String publicKeyAlgMpConfigProp = getMpConfigProperty(PUBLIC_KEY_ALG);
        if (publicKeyAlgMpConfigProp == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Didn't find " + PUBLIC_KEY_ALG + " property in MP Config props; defaulting to " + defaultAlg);
            }
            return defaultAlg;
        }
        if (!isSupportedSignatureAlgorithm(publicKeyAlgMpConfigProp)) {
            Tr.warning(tc, "MP_CONFIG_PUBLIC_KEY_ALG_NOT_SUPPORTED", new Object[] { publicKeyAlgMpConfigProp, defaultAlg });
            return defaultAlg;
        }
        return publicKeyAlgMpConfigProp;
    }

    String getMpConfigProperty(String propName) {
        if (mpConfigProps == null || propName == null) {
            return null;
        }
        if (mpConfigProps.containsKey(propName)) {
            return String.valueOf(mpConfigProps.get(propName));
        }
        return null;
    }

    boolean isSupportedSignatureAlgorithm(String sigAlg) {
        if (sigAlg == null) {
            return false;
        }
        return sigAlg.matches("[RHE]S(256|384|512)");
    }

    boolean isAsymmetricAlgorithm(String sigAlg) {
        return (keyAlgChecker.isRSAlgorithm(sigAlg) || keyAlgChecker.isESAlgorithm(sigAlg));
    }

    Key getSigningKeyForHS(String signatureAlgorithm, JwtConsumerConfig config) throws KeyException {
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
     * Creates a Key object from the shared key specified in the provided configuration.
     */
    Key getSharedSecretKey(JwtConsumerConfig config) throws KeyException {
        if (config == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JWT consumer config object is null");
            }
            return null;
        }
        String sharedKey = config.getSharedKey();
        // TODO - pass in signature algorithm?
        return createKeyFromSharedKey(sharedKey);
    }

    Key createKeyFromSharedKey(String sharedKey) throws KeyException {
        if (sharedKey == null || sharedKey.isEmpty()) {
            String msg = Tr.formatMessage(tc, "JWT_MISSING_SHARED_KEY");
            throw new KeyException(msg);
        }
        try {
            // TODO - use signature algorithm?
            return new HmacKey(sharedKey.getBytes(Constants.UTF_8));
        } catch (UnsupportedEncodingException e) {
            // Should not happen - UTF-8 should be supported
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting shared key bytes: " + e.getLocalizedMessage());
            }
        }
        return null;
    }

    boolean isPublicKeyPropsPresent() {
        if (mpConfigProps == null) {
            return false;
        }
        return mpConfigProps.get(PUBLIC_KEY) != null || mpConfigProps.get(KEY_LOCATION) != null;
    }

    Key getSigningKeyForRS(JwtConsumerConfig config, JwtContext jwtContext) throws KeyException {
        return getKeyFromJwkOrTrustStore(config, jwtContext);
    }

    Key getKeyFromJwkOrTrustStore(JwtConsumerConfig config, JwtContext jwtContext) throws KeyException {
        Key signingKey = null;
        if (config.getJwkEnabled() || (config.getTrustedAlias() == null && isPublicKeyPropsPresent())) { // need change to consider MP-Config
            signingKey = getKeyForJwkEnabled(config, jwtContext);
        } else {
            signingKey = getKeyForJwkDisabled(config);
        }
        return signingKey;
    }

    Key getKeyForJwkEnabled(JwtConsumerConfig config, JwtContext jwtContext) throws KeyException {
        Key signingKey = null;
        try {
            signingKey = getJwksKey(config, jwtContext);
        } catch (Exception e) {
            String msg = Tr.formatMessage(tc, "JWT_ERROR_GETTING_JWK_KEY", new Object[] { config.getJwkEndpointUrl(), e.getLocalizedMessage() });
            throw new KeyException(msg, e);
        }
        return signingKey;
    }

    protected Key getJwksKey(JwtConsumerConfig config, JwtContext jwtContext) throws Exception {
        JsonWebStructure jwtHeader = getJwtHeader(jwtContext);
        String kid = jwtHeader.getKeyIdHeaderValue();
        JwKRetriever jwkRetriever = null;
        if (mpConfigProps != null) {
            String publickey = mpConfigProps.get(PUBLIC_KEY);
            String keyLocation = mpConfigProps.get(KEY_LOCATION);
            if (publickey != null || keyLocation != null) {
                jwkRetriever = new JwKRetriever(config.getId(), config.getSslRef(), config.getJwkEndpointUrl(),
                        config.getJwkSet(), JwtUtils.getSSLSupportService(), config.isHostNameVerificationEnabled(),
                        null, null, getConfiguredSignatureAlgorithm(config), publickey, keyLocation);
            }
        }
        if (jwkRetriever == null) {
            jwkRetriever = new JwKRetriever(config.getId(), config.getSslRef(), config.getJwkEndpointUrl(),
                    config.getJwkSet(), JwtUtils.getSSLSupportService(), config.isHostNameVerificationEnabled(), null,
                    null, getConfiguredSignatureAlgorithm(config));
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
            signingKey = getPublicKey(trustedAlias, trustStoreRef, getConfiguredSignatureAlgorithm(config));
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

    Key getSigningKeyForES(JwtConsumerConfig config, JwtContext jwtContext) throws KeyException {
        return getKeyFromJwkOrTrustStore(config, jwtContext);
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
            Key key) throws Exception {
        JwtClaims jwtClaims = jwtContext.getJwtClaims();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Key from config: " + key);
        }

        validateClaims(jwtClaims, jwtContext, config);
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

    void validateClaims(JwtClaims jwtClaims, JwtContext jwtContext, JwtConsumerConfig config)
            throws MalformedClaimException, InvalidClaimException, InvalidTokenException {
        String issuer = config.getIssuer();
        if (issuer == null) {
            issuer = (mpConfigProps == null) ? null : mpConfigProps.get(ISSUER);
        }

        validateIssuer(config.getId(), issuer, jwtClaims.getIssuer());

        validateAudience(config, jwtClaims.getAudience());

        if (!validateAMRClaim(config.getAMRClaim(), getJwtAMRList(jwtClaims))) {
            String msg = Tr.formatMessage(tc, "JWT_AMR_CLAIM_NOT_VALID",
                    new Object[] { getJwtAMRList(jwtClaims), config.getId(), config.getAMRClaim() });
            throw new InvalidClaimException(msg);
        }

        // check azp

        validateIatAndExp(jwtClaims, config.getClockSkew());

        validateNbf(jwtClaims, config.getClockSkew());

        validateAlgorithm(jwtContext, getConfiguredSignatureAlgorithm(config));
    }

    /**
     * Throws an exception if the provided key is null but the config specifies a
     * signature algorithm other than "none".
     */
    void validateSignatureAlgorithmWithKey(JwtConsumerConfig config, Key key) throws InvalidClaimException {
        String signatureAlgorithm = getConfiguredSignatureAlgorithm(config);
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

    public List<String> getConfiguredAudiences(JwtConsumerConfig config) {
        List<String> audiences = config.getAudiences();
        if (audiences != null) {
            // Server configuration takes precedence over MP Config property values
            return audiences;
        }
        return getAudiencesFromMpConfigProps();
    }

    List<String> getAudiencesFromMpConfigProps() {
        List<String> audiences = null;
        String audiencesMpConfigProp = getMpConfigProperty(VERIFY_AUDIENCES);
        if (audiencesMpConfigProp == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Didn't find " + VERIFY_AUDIENCES + " property in MP Config props; defaulting to " + audiences);
            }
            return audiences;
        }
        audiences = new ArrayList<String>();
        String[] splitAudiences = audiencesMpConfigProp.split(",");
        for (String rawAudience : splitAudiences) {
            if (!rawAudience.isEmpty()) {
                audiences.add(rawAudience);
            }
        }
        return audiences;
    }

    void validateAudience(JwtConsumerConfig config, List<String> audiences) throws InvalidClaimException {
        List<String> allowedAudiences = getConfiguredAudiences(config);
        if (allowedAudiences == null && config.ignoreAudClaimIfNotConfigured()) {
            return;
        }
        if (!validateAudience(allowedAudiences, audiences)) {
            String msg = Tr.formatMessage(tc, "JWT_AUDIENCE_NOT_TRUSTED", new Object[] { audiences, config.getId(), allowedAudiences });
            throw new InvalidClaimException(msg);
        }
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

    /**
     * Helper method to get the AMR Claim from the jwtClaims.This method checks
     * if the value is a string and return singletonList or the ArrayList of
     * amrClaims. This is called in validateCalims method
     *
     */
    List<String> getJwtAMRList(JwtClaims jwtClaims) throws MalformedClaimException {
        String claimName = "amr";
        Object amrObject = jwtClaims.getClaimValue(claimName);
        if (amrObject instanceof String) {
            return Collections.singletonList(jwtClaims.getStringClaimValue(claimName));
        } else if (!(amrObject instanceof List) && amrObject != null) {
            throw new MalformedClaimException(
                    "The value of the 'amr' claim is not an array of strings or a single string value.");
        } else {
            return jwtClaims.getStringListClaimValue(claimName);
        }
    }

    /**
     * Verifies that values specified in AMR claim is contained in the
     * authenticationMethodsReferences list. If allowedAMRClaim is not an array
     * then jwtClaims can contain more than required values. If not, then the
     * jwtClaimvalues must be a exact match of an element in the array.
     */
    boolean validateAMRClaim(List<String> allowedAmrClaim, List<String> jwtAMRClaims) {
        boolean valid = false;
        if (allowedAmrClaim != null && jwtAMRClaims != null) {
            // If it is not array just check if jwtClaim containsAll and not
            // equals
            if (allowedAmrClaim.size() == 1) {
                List<String> allowedAMRSingle = Arrays.asList(allowedAmrClaim.get(0).split(" "));
                if (jwtAMRClaims.containsAll(allowedAMRSingle)) {
                    valid = true;
                }
            } else {
                for (String allowedAMR : allowedAmrClaim) {
                    List<String> allowedAMRSingle = Arrays.asList(allowedAMR.split(" "));
                    if (jwtAMRClaims.equals(allowedAMRSingle)) {
                        valid = true;
                        break;
                    }

                }
            }
        } else if (allowedAmrClaim == null) {
            //To avoid regression, if new amr config is not specified then return true
            valid = true;
        }
        return valid;
    }

}
