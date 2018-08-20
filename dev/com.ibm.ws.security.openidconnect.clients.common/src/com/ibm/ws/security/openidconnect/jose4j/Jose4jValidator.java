/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.jose4j;

import java.security.InvalidKeyException;
import java.security.Key;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.joda.time.Instant;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.InvalidJwtSignatureException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.openidconnect.clients.common.OidcClientRequest;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.openidconnect.common.OidcCommonClientRequest;
import com.ibm.ws.security.openidconnect.token.IDTokenValidationFailedException;
import com.ibm.ws.security.openidconnect.token.JWT;
import com.ibm.ws.security.openidconnect.token.JWTTokenValidationFailedException;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;
import com.ibm.ws.security.openidconnect.token.PayloadConstants;
import com.ibm.ws.security.openidconnect.token.TraceConstants;

public class Jose4jValidator {

    private static final TraceComponent tc = Tr.register(Jose4jValidator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    //need these for verification of token
    private String clientId = null;
    private String issuers = null;
    private String signingAlgorithm = "none";
    private final Key key;
    private long clockSkewInSeconds = 0;
    boolean rpSpecifiedSigningAlgorithm = true;
    OidcClientRequest oidcClientRequest = null;

    public Jose4jValidator(Key key, long clockSkewInSeconds,
            String issuers, String clientId,
            String signatureAlgorithm,
            OidcClientRequest oidcClientRequest) {
        this.key = key;
        this.clockSkewInSeconds = clockSkewInSeconds;
        this.issuers = issuers;
        this.clientId = clientId;
        this.signingAlgorithm = signatureAlgorithm;
        this.oidcClientRequest = oidcClientRequest;
    }

    @FFDCIgnore({ InvalidJwtSignatureException.class, InvalidJwtException.class })
    public JwtClaims parseJwtWithValidation(String jwtString,
            JwtContext jwtContext,
            JsonWebSignature signature) throws JWTTokenValidationFailedException, IllegalStateException, Exception {

        // Let check the error situations here, so we can get similar error message like old jwt
        JwtClaims jwtClaims = jwtContext.getJwtClaims();

        // We do not check the values of scope nor if it's empty
        String issuer = jwtClaims.getIssuer();

        // for audiences checking
        List<String> audiences = jwtClaims.getAudience();
        boolean emptyAudienceClaim = audiences.isEmpty();
        String okAudience = clientId; // default audience
        if (oidcClientRequest.getTokenType().equalsIgnoreCase(OidcClientRequest.TYPE_JWT_TOKEN)) {
            // check issuer
            if (oidcClientRequest.disableIssChecking()) { // enforced disableIssChecking
                if (issuer != null && !issuer.isEmpty()) {
                    throw JWTTokenValidationFailedException.format("PROPAGATION_TOKEN_ISS_CLAIM_NOT_REQUIRED_ERR", new Object[] { "iss", "disableIssChecking" });
                } // else issuer is null or empty. It's expected
            } else {
                // issuer needed.
                // This will throw exception if the verification failed
                this.checkIssuer(clientId, issuers, issuer);
            }

            // do JWT specific checking
            List<String> allowedAudiences = oidcClientRequest.getAudiences();
            if (!emptyAudienceClaim) {
                String strOkAudience = oidcClientRequest.allowedAllAudiences() ? audiences.get(0) //any audiences is accepted
                        : jwtAudienceElementCheck(allowedAudiences, audiences);
                if (strOkAudience == null) { // no ok audience was found
                    String aud = array2String(audiences);
                    throw JWTTokenValidationFailedException.format("OIDC_JWT_VERIFY_AUD_ERR",
                            new Object[] { aud, clientId,
                                    allowedAudiences == null ? null : array2String(allowedAudiences) }); // 219214
                } else {
                    // else is OK
                    okAudience = strOkAudience;
                }
            } else {
                //     * C. if JWT does not contain "aud", but openidConnectClient contains "audiences", it is an error condition.
                if (!oidcClientRequest.allowedAllAudiences() && allowedAudiences != null) {
                    String strAllowedAudiences = array2String(allowedAudiences);
                    throw JWTTokenValidationFailedException.format("OIDC_JWT_MISSING_AUD",
                            new Object[] { clientId, strAllowedAudiences });
                }
            }
            // TODO
            //    check azp which is the oidc client who request the jwt but not necessary in the audience
        } else {
            // for ID Token. Doing the same thing as before
            if (jwtClaims.getIssuedAt() == null) {
                // OIDC RP certification requires that a token missing the iat claim be rejected.
                throw oidcClientRequest.errorCommon(true, tc, "OIDC_ID_VERIFY_IAT_ERR",
                        new Object[] { clientId });
            }

            if (!JWT.checkIssuer(clientId, issuers, issuer)) {
                // issuer verification failed
                // Let's make it behave the same the old IDToken though why it failed
                // 221386
                String errMsg = OidcClientRequest.TYPE_ID_TOKEN.equals(oidcClientRequest.getTokenType()) ? "ID token validation Error[issuer]" : "Json Web Token validation Error[issuer]";
                throw new InvalidJwtException(errMsg);
            }
            // So far, we only have JWT and IDToken in this code path
            // Do some specific ID Token checking
            if (!emptyAudienceClaim && !multipleAudienceElementCheck(clientId, audiences)) {
                String aud = array2String(audiences);
                throw IDTokenValidationFailedException.format("OIDC_IDTOKEN_VERIFY_AUD_ERR", // 219214
                        new Object[] { aud, clientId }); // 219214
            }

            // azp is offer in JWT while the audience and the requesting client is not the same
            // And it should not be checked as the client ID of the audience in JWT.
            String azp = (String) jwtClaims.getClaimValue(PayloadConstants.AUTHORIZED_PARTY);
            if (azp != null) {
                if (!(azp.equals(clientId))) {
                    throw oidcClientRequest.errorCommon(true, tc, "OIDC_IDTOKEN_VERIFY_AUD_AZP_ERR",
                            new Object[] { azp, clientId });
                }
            }
        }

        // checking
        NumericDate issueAtClaim = jwtClaims.getIssuedAt();
        NumericDate expirationClaim = jwtClaims.getExpirationTime();

        Instant issuedAt = null;
        Instant expiration = null;
        if (issueAtClaim == null) {
            if (expirationClaim != null) {
                issuedAt = new Instant(0);
                expiration = new Instant(expirationClaim.getValueInMillis());
            } // no issueAt and no expiration, no checking
        } else {
            issuedAt = new Instant(issueAtClaim.getValueInMillis());
            if (expirationClaim == null) {
                expiration = new Instant(Long.MAX_VALUE);
            } else {
                expiration = new Instant(expirationClaim.getValueInMillis());
            }
        }

        if (issuedAt != null) {
            if (issuedAt.isAfter(expiration) ||
                    !JsonTokenUtil.isCurrentTimeInInterval(clockSkewInSeconds, issuedAt.getMillis(), expiration.getMillis())) {

                Object[] objects = new Object[] { this.clientId, new Instant(System.currentTimeMillis()), expiration, issuedAt };

                String failMsg = Tr.formatMessage(tc, "OIDC_JWT_VERIFY_STATE_ERR", objects);
                oidcClientRequest.setRsFailMsg(OidcCommonClientRequest.EXPIRED_TOKEN, failMsg);

                throw oidcClientRequest.errorCommon(true, tc, "OIDC_JWT_VERIFY_STATE_ERR", objects); // 219214
            }
        }

        // check nbf
        NumericDate nbf = jwtClaims.getNotBefore();
        if (nbf != null) { // 224202
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "nbf = ", nbf);
            }
            NumericDate currentSkewTime = NumericDate.now();
            currentSkewTime.addSeconds(clockSkewInSeconds);
            if (nbf.isAfter(currentSkewTime)) {
                // PROPAGATION_TOKEN_NBF_ERR=CWWKS1780E: The resource server failed the authentication request because the token which is in the request cannot be used. The not before time ("nbf") [{0}] is after the current time [{1}] and this condition is not allowed.
                Object[] objects = new Object[] { (new Date(nbf.getValueInMillis())).toString(),
                        (new Date(currentSkewTime.getValueInMillis())).toString() };
                throw oidcClientRequest.errorCommon(true, tc, "PROPAGATION_TOKEN_NBF_ERR", objects);
            }
        }

        verifySignAlgOnly(signature);

        JwtConsumerBuilder builder = new JwtConsumerBuilder();
        builder.setRequireExpirationTime()// the JWT must have an expiration time
                .setAllowedClockSkewInSeconds(Long.valueOf(clockSkewInSeconds).intValue()) // allow some leeway in validating time based claims to account for clock skew
                .setExpectedAudience(okAudience) // to whom the JWT is intended for
                .setExpectedIssuer(false, issuer); // whom the JWT needs to have been issued by.
        // Since issuers is multiple, and the issuer is verified, let's use the issuer.
        // in old JWT, we do not ensure the existing of "iss"
        if (!oidcClientRequest.getTokenType().equalsIgnoreCase(OidcClientRequest.TYPE_JWT_TOKEN)) {
            builder.setRequireSubject();
        }
        if (emptyAudienceClaim) { // no audience claim in jwtClaims
            builder.setSkipDefaultAudienceValidation();
        }
        if (!rpSpecifiedSigningAlgorithm) { // allow signatureAlgorithme as none
            builder.setDisableRequireSignature()
                    .setSkipSignatureVerification(); // Once the alg is specified as none, we trust any JWT. Do not check the signature
        } else {
            builder.setVerificationKey(key) // verify the signature with the public key
                    .setRelaxVerificationKeyValidation(); // create the JwtConsumer instance
        }

        JwtConsumer jwtConsumer = builder.build();

        try {
            JwtContext validatedJwtContext = jwtConsumer.process(jwtString);

            jwtClaims = validatedJwtContext.getJwtClaims();
        } catch (InvalidJwtSignatureException e) {
            Object[] objs = new Object[] { this.clientId, e.getLocalizedMessage(), this.signingAlgorithm };
            oidcClientRequest.errorCommon(new String[] { "OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR",
                    "OIDC_JWT_SIGNATURE_VERIFY_ERR" }, objs); // 219214

            if (OidcCommonClientRequest.TYPE_ID_TOKEN.equals(oidcClientRequest.getTokenType())) {
                throw new IDTokenValidationFailedException(e.getMessage(), e);
            } else {
                throw new JWTTokenValidationFailedException(e.getMessage(), e);
            }

        } catch (InvalidJwtException e) {
            Throwable cause = getRootCause(e);
            // java.security.InvalidKeyException: No installed provider supports this key: (null)
            if (cause instanceof InvalidKeyException) {
                if (cause.getMessage().contains("No installed provider")) {
                    oidcClientRequest.errorCommon("JWK_ENDPOINT_MISSING_ERR", new Object[] {});

                    if (OidcCommonClientRequest.TYPE_ID_TOKEN.equals(oidcClientRequest.getTokenType())) {
                        throw new IDTokenValidationFailedException(e.getMessage(), e);
                    } else {
                        throw new JWTTokenValidationFailedException(e.getMessage(), e);
                    }

                } else {
                    // Don't have enough information to output a more useful error message
                    throw e;
                }
            } else {
                // otherwise throw original Exception
                throw e;
            }
        }

        return jwtClaims;
    }

    /**
     * A. If JWT contains "aud", and one or more "aud" starts http or https,
     * and resources service URL contains the "aud" (and audiences is not configured), it is considered matched. JWT will be
     * accepted.
     * In other words, if "aud" is URL, "audiences" configuration is not required. However, if audiences is configured, the exact
     * matching is required, and resource service URL matching is not performed.
     * B. If JWT contains "aud", and openidConnectClient does not config "audiences", or configured "audience" do not match, it is
     * an error condition.
     * C. if JWT does not contain "aud", but openidConnectClient contains "audiences", it is an error condition.
     * D. ALL_AUDIENCES can be specified to ignore audience check.
     *
     * @param allowedAudiences
     * @param audiences
     * @return
     */
    String jwtAudienceElementCheck(List<String> allowedAudiences, List<String> audiences) {
        if (allowedAudiences == null) { // handle Point A.
            for (String audience : audiences) {
                if (oidcClientRequest.isPreServiceUrl(audience)) {
                    return audience;
                }
            }
            return null; // Point B.
        }
        // Point B.
        for (String audience : audiences) {
            for (String allowedAud : allowedAudiences) { // this is not null when created in the configuration instnce
                // ALL_AUDIENCES.  Point D.
                if (allowedAud != null && allowedAud.equals(audience))
                    return audience;
            }
        }
        return null;
    }

    /**
     * @param e
     * @return
     */
    Throwable getRootCause(Exception e) {
        Throwable rootCause = null;
        Throwable tmpCause = e;
        while (tmpCause != null) {
            rootCause = tmpCause;
            tmpCause = rootCause.getCause();
        }
        return rootCause;
    }

    public void verifySignAlgOnly(JsonWebSignature signature) throws JWTTokenValidationFailedException {
        String algHeader = signature.getAlgorithmHeaderValue();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Signing Algorithm from header: " + algHeader);
        }
        rpSpecifiedSigningAlgorithm = !this.signingAlgorithm.equals(Constants.SIG_ALG_NONE);
        if (rpSpecifiedSigningAlgorithm) {
            // if algorithm is not NONE, then check the signature of jwt first
            if (signature.getEncodedSignature().isEmpty()) {
                throw oidcClientRequest.errorCommon(true, tc, new String[] { "OIDC_IDTOKEN_SIGNATURE_VERIFY_MISSING_SIGNATURE_ERR",
                        "OIDC_JWT_SIGNATURE_VERIFY_MISSING_SIGNATURE_ERR" },
                        new Object[] { this.clientId, this.signingAlgorithm }); // 219214
            }

            // Doing the same thing as old jwt
            if (!(this.signingAlgorithm.equals(algHeader))) {
                throw oidcClientRequest.errorCommon(true, tc, new String[] { "OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR_ALG_MISMATCH",
                        "OIDC_JWT_SIGNATURE_VERIFY_ERR_ALG_MISMATCH" }, // 219214
                        new Object[] { this.clientId, this.signingAlgorithm, algHeader });
            }
        }
    }

    boolean multipleAudienceElementCheck(String clientId, List<String> audList) {
        Iterator<String> it = audList.iterator();
        while (it.hasNext()) {
            if (it.next().equals(clientId)) {
                return true;
            }
        }
        return false;
    }

    String array2String(List<String> strings) {
        String result = "";
        for (String string : strings) {
            if (result.isEmpty()) {
                result = string;
            } else {
                result += ", " + string;
            }
        }
        return result;
    }

    String array2String(String[] strings) {
        String result = "";
        for (String string : strings) {
            if (result.isEmpty()) {
                result = string;
            } else {
                result += ", " + string;
            }
        }
        return result;
    }

    protected boolean checkIssuer(String clientId, String issuers, String issuer) throws JWTTokenValidationFailedException {
        boolean isIssuer = false;
        if (issuer != null) {
            if (issuer.equals(issuers)) { // most cases
                isIssuer = true;
            } else {
                if (issuers != null) {
                    StringTokenizer st = new StringTokenizer(issuers, " ,");
                    while (st.hasMoreTokens()) {
                        String iss = st.nextToken();
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Token:" + iss);
                        }
                        if (issuer.equals(iss)) {
                            isIssuer = true;
                            break;
                        }
                    }
                }
            }
        }
        if (!isIssuer) {
            final String issuerIdentifierAttr = "issuerIdentifier";
            throw JWTTokenValidationFailedException.format("TOKEN_ISSUER_NOT_TRUSTED", new Object[] { clientId, issuer, issuers, issuerIdentifierAttr });
        }
        return isIssuer;
    }
}
