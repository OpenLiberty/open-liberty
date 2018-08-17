/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.token;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.SignatureException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.joda.time.Instant;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.openidconnect.common.Constants;

public class JWT {

    private static final TraceComponent tc = Tr.register(JWT.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    /** Header. */
    private JWSHeader header;

    /** Payload */
    private JWTPayload payload;

    /** Key. */
    @Sensitive
    private Object key; // could be the private or public key, depending on how this object was constructed.

    /** token string. */
    private String signedAndSerializedString;

    /** string to be validated. */
    private String tokenString;

    /** plain token string. */
    private String baseString;

    private static final String ALGORITHM_HEADER = "alg";

    private long clockSkewInSeconds = 0;

    //need these for verification of token
    private String clientId = null;
    private String issuers = null;
    private String signingAlgorithm = "none";

    /**
     * @param header
     *            header
     * @param payload
     *            payload
     *            To create JWT
     */
    public JWT(JWSHeader header, JWTPayload payload, @Sensitive Object key) {
        this.header = header;
        this.payload = payload;
        setKey(key); //could be RSAPrivateKey or byte[]
    }

    /**
     * @param header
     *            header
     * @param payload
     *            payload
     *            To create JWT
     */
    public JWT(JWSHeader header, JWTPayload payload) {
        this.header = header;
        this.payload = payload;
    }

    /**
     * @param String
     *            tokenString to parse and create JWT from..
     * @param key
     *            bytes needed to verify the signature
     *            To parse and create JWT (for validation purposes)
     */

    public JWT(String tokenString, @Sensitive Object key, String clientId,
            String issuers, String signingAlgorithm) {
        this.tokenString = tokenString;
        setKey(key); //could be RSAPublicKey or byte[]
        this.clientId = clientId;
        this.issuers = issuers;
        this.signingAlgorithm = signingAlgorithm;
        this.header = new JWSHeader();
        initializeHeader();
    }

    public JWT(String tokenString, String clientId,
            String issuers, String signingAlgorithm) {
        this.tokenString = tokenString;
        this.clientId = clientId;
        this.issuers = issuers;
        this.signingAlgorithm = signingAlgorithm;
    }

    public String getClientId() {
        return this.clientId;
    }

    /**
     * Returns the header.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWSHeader getHeader() {
        return header;
    }

    /**
     * Returns the payload.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public JWTPayload getPayload() {
        return payload;
    }

    /**
     * Creates the plain text JWT.
     *
     */

    protected String createPlainTextJWT() {
        com.google.gson.JsonObject header = createHeader();
        com.google.gson.JsonObject payload = createPayload();

        String plainTextTokenString = computeBaseString(header, payload);
        StringBuffer sb = new StringBuffer(plainTextTokenString);
        sb.append(".").append("");
        return sb.toString();
    }

    protected JsonObject createHeader() {
        com.google.gson.JsonObject jsonheader = new JsonObject();

        jsonheader.addProperty(ALGORITHM_HEADER, "none");
        List<String> critList = header.getCritical();
        if (critList != null) {
            JsonArray list = handleList(critList);
            jsonheader.add("crit", list);
        }
        Set<String> keySet = header.keySet();
        if (!keySet.isEmpty()) {
            Iterator<String> it = keySet.iterator();
            while (it.hasNext()) {
                String key = it.next();
                Object value = header.get(key);
                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    JsonArray list = handleList((List<String>) value);
                    jsonheader.add(key, list);
                } else if (value instanceof String) {
                    jsonheader.addProperty(key, (String) value);
                } else if (value instanceof Number) {
                    jsonheader.addProperty(key, (Number) value);
                } else { //TODO: handle nested elements

                }
            }
        }
        return jsonheader;
    }

    protected JsonObject createPayload() {
        com.google.gson.JsonObject jsonpayload = new JsonObject();

        List<String> audienceList = payload.getAudienceAsList();
        if (audienceList != null) {
            JsonArray list = handleList(audienceList);
            jsonpayload.add(PayloadConstants.AUDIENCE, list);
        }
        Set<String> keySet = payload.keySet();
        if (!keySet.isEmpty()) {
            Iterator<String> it = keySet.iterator();
            while (it.hasNext()) {
                String key = it.next();
                Object value = payload.get(key);
                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    JsonArray list = handleList((List<String>) value);
                    jsonpayload.add(key, list);
                } else if (value instanceof String) {
                    jsonpayload.addProperty(key, (String) value);
                } else if (value instanceof Number) {
                    jsonpayload.addProperty(key, (Number) value);
                } else { //TODO: handle nested elements

                }
            }
        }
        long now = System.currentTimeMillis();
        if (payload.getIssuedAtTimeSeconds() != null) {
            //Instant expects time in milliseconds
            //Instant issuedAt = new Instant(payload.getIssuedAtTimeSeconds() * 1000);
            jsonpayload.addProperty(WSJsonToken.ISSUED_AT, payload.getIssuedAtTimeSeconds());
        } else {
            //Instant issueAt = new Instant(0);
            jsonpayload.addProperty(WSJsonToken.ISSUED_AT, now / 1000);
        }
        if (payload.getExpirationTimeSeconds() != null) {
            jsonpayload.addProperty(WSJsonToken.EXPIRATION, payload.getExpirationTimeSeconds());
        } else {
            //token.setExpiration(clock.now().withDurationAdded(60, 1));
            // Instant expireAt = clock.now().plus(Duration.standardHours(1));
            long expireAtSec = now / 1000 + (60 * 60);
            jsonpayload.addProperty(WSJsonToken.EXPIRATION, expireAtSec);
        }

        //jsonpayload.addProperty(JsonToken.EXPIRATION,
        //clock.now().withDurationAdded(60, 1).getMillis() / 1000);
        return jsonpayload;
    }

    protected JsonArray handleList(List<String> strlist) {
        JsonArray list = null;
        if (strlist != null) {
            list = new JsonArray();
            for (String str : strlist) {
                JsonPrimitive jsonPrimitiveObj = new JsonPrimitive(str);
                list.add(jsonPrimitiveObj);
            }
        }
        return list;
    }

    protected String computeBaseString(JsonObject header, JsonObject payload) {
        if (baseString != null && !baseString.isEmpty()) {
            return baseString;
        }
        baseString = JsonTokenUtil.toDotFormat(
                JsonTokenUtil.toBase64(header),
                JsonTokenUtil.toBase64(payload));

        return baseString;
    }

    /**
     * Creates the signed JWT.
     *
     */

    protected void createSignedJWT() throws SignatureException, InvalidKeyException {
        createJsonToken();
    }

    @SuppressWarnings("unchecked")
    protected WSJsonToken createJsonToken() throws SignatureException {
        WSJsonToken token = new WSJsonToken();

        Set<String> keySet = header.keySet();
        if (!keySet.isEmpty()) {
            JsonObject headerObj = token.getHeader();
            Iterator<String> it = keySet.iterator();
            while (it.hasNext()) {

                String key = it.next();
                Object value = header.get(key);
                if (value instanceof List) {
                    JsonArray list = handleList((List<String>) value);
                    headerObj.add(key, list);
                } else if (value instanceof String) {
                    headerObj.addProperty(key, (String) value);
                } else if (value instanceof Number) {
                    headerObj.addProperty(key, (Number) value);
                } else { //TODO: handle nested elements

                }
            }
        }
        keySet = payload.keySet();
        if (!keySet.isEmpty()) {
            JsonObject payloadObj = token.getPayload();
            Iterator<String> it = keySet.iterator();
            while (it.hasNext()) {
                String key = it.next();
                Object value = payload.get(key);
                if (value instanceof List) {
                    JsonArray list = handleList((List<String>) value);
                    payloadObj.add(key, list);
                } else if (value instanceof String) {
                    payloadObj.addProperty(key, (String) value);
                } else if (value instanceof Number) {
                    payloadObj.addProperty(key, (Number) value);
                } else { //TODO: handle nested elements

                }
            }
        }
        long nowSeconds = System.currentTimeMillis() / 1000;
        if (payload.getIssuedAtTimeSeconds() != null) {
            //Instant expects time in milliseconds
            token.setIssuedAt(payload.getIssuedAtTimeSeconds());
        } else {
            token.setIssuedAt(nowSeconds); //Instant issueAt = new Instant(0); //TODO
        }
        if (payload.getExpirationTimeSeconds() != null) {
            token.setExpiration(payload.getExpirationTimeSeconds());
        } else {
            //token.setExpiration(clock.now().withDurationAdded(60, 1));
            //token.setExpiration(clock.now().plus(Duration.standardHours(1))); //TODO: we need to see how we can use oauth2 defaults
            token.setExpiration(nowSeconds + (60 * 60));
        }

        try {
            signedAndSerializedString = serializeAndSign(token);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            FFDCFilter.processException(e,
                    getClass().getName(), "createJsonToken",
                    new Object[] { this.header.getAlgorithm() }); //TODO
            Tr.error(tc, "OIDC_IDTOKEN_SIGNATURE_ERR", new Object[] {
                    this.header.getAlgorithm(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName() });
            throw new SignatureException(e);
        }
        return token;

    }

    private String serializeAndSign(WSJsonToken token) throws InvalidKeyException, UnsupportedEncodingException, JoseException {
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(JsonTokenUtil.toJson(token.getPayload()));
        String alg = token.getHeader().get(ALGORITHM_HEADER).getAsString();
        jws.setAlgorithmHeaderValue(alg);
        JsonElement typ = token.getHeader().get("typ");
        if (typ != null) {
            jws.setHeader("typ", typ.getAsString());
        }
        // todo: did we miss any?
        jws.setKey(getKey(alg)); // private key
        return jws.getCompactSerialization();

    }

    public String getSignedJWTString() throws SignatureException, InvalidKeyException {

        if (signedAndSerializedString != null) {
            return this.signedAndSerializedString;
        } else {
            createSignedJWT();
            return this.signedAndSerializedString;
        }

    }

    public String getJWTString() {
        if (this.baseString != null) {
            return this.baseString;
        } else {
            this.baseString = createPlainTextJWT();
            return this.baseString;
        }
    }

    protected void fromJsonToken(WSJsonToken token) {
        this.payload = new JWTPayload();
        JsonTokenUtil.fromJsonToken(token, payload);
        this.header = new JWSHeader();
        JsonTokenUtil.fromJsonToken(token, header);
    }

    protected boolean verifyTokenIssueAndExpTime(WSJsonToken jsonToken) throws IDTokenValidationFailedException {
        boolean isValid = true;

        Instant issuedAt = new Instant(jsonToken.getIssuedAt() * 1000);
        Instant expiration = new Instant(jsonToken.getExpiration() * 1000);

        if (issuedAt != null) {
            if (issuedAt.isAfter(expiration) ||
                    !JsonTokenUtil.isCurrentTimeInInterval(clockSkewInSeconds, issuedAt.getMillis(), expiration.getMillis())) {
                // Hard coded English
                Object[] objects = new Object[] { this.clientId, "Token expired", System.currentTimeMillis(), expiration, issuedAt };
                Tr.error(tc, "OIDC_IDTOKEN_VERIFY_STATE_ERR", objects);
                throw new IllegalStateException(Tr.formatMessage(tc, "OIDC_IDTOKEN_VERIFY_STATE_ERR", objects));
            }
        }
        if (!checkIssuer(this.clientId, this.issuers, jsonToken.getIssuer())) {
            isValid = false;
        }
        return isValid;
    }

    public static boolean checkIssuer(String clientId, String issuers, String issuer) throws IDTokenValidationFailedException {
        boolean isIssuer = false;
        if (issuer != null) {
            if (issuer.equals(issuers)) { // most cases
                isIssuer = true;
            } else {
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

            if (!isIssuer) {
                Tr.error(tc, "OIDC_IDTOKEN_VERIFY_ISSUER_ERR",
                        new Object[] { clientId, issuer, issuers });

                throw IDTokenValidationFailedException.format("OIDC_IDTOKEN_VERIFY_ISSUER_ERR", clientId, issuer, issuers);

            }
        }
        return isIssuer;
    }

    public boolean verify() throws IDTokenValidationFailedException {
        return verify(JsonTokenUtil.DEFAULT_SKEW_IN_SECONDS);
    }

    @FFDCIgnore({ InvalidKeyException.class, InvalidJwtException.class })
    public boolean verifySignatureOnly() throws IDTokenValidationFailedException {
        String[] jwtParts = JsonTokenUtil.splitTokenString(this.tokenString);
        boolean rpSpecifiedSignatureAlgorithm = true;
        if (this.signingAlgorithm.equals(Constants.SIG_ALG_NONE)) {
            rpSpecifiedSignatureAlgorithm = false;
        }
        String alg = null;
        JsonObject header = null;
        if (!rpSpecifiedSignatureAlgorithm) {
            String jwtHeaderSegment = jwtParts[0];
            JsonParser parser = new JsonParser();
            header = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtHeaderSegment))
                    .getAsJsonObject();
            alg = header.get("alg").getAsString();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Signing Algorithm from header: " + alg);
            }
        } else {
            String jwtHeaderSegment = jwtParts[0];
            JsonParser parser = new JsonParser();
            header = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtHeaderSegment))
                    .getAsJsonObject();
            String algHeader = header.get("alg").getAsString();
            if (!(this.signingAlgorithm.equals(algHeader))) {
                Tr.error(tc, "OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR_ALG_MISMATCH", new Object[] { this.clientId, this.signingAlgorithm, algHeader });
                throw IDTokenValidationFailedException.format("OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR_ALG_MISMATCH", this.clientId, this.signingAlgorithm, algHeader);
            }
            alg = this.signingAlgorithm;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "RP specified Signing Algorithm : " + alg);
            }
        }

        // the old net.oauth version populated the header and payload out of the token string here, do that for consistency.
        String jwtPayloadSegment = jwtParts[1];
        JsonParser parser = new JsonParser();
        JsonObject payload = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtPayloadSegment))
                .getAsJsonObject();
        WSJsonToken tempToken = new WSJsonToken(header, payload);
        fromJsonToken(tempToken);

        try {
            // parse the token string and see if it passes
            JsonTokenUtil.validateTokenString(this.tokenString, alg, getKey(alg), this.clockSkewInSeconds, true);
        } catch (InvalidKeyException e) {
            Object[] objs = new Object[] { this.clientId,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(),
                    this.signingAlgorithm };
            Tr.error(tc, "OIDC_IDTOKEN_SIGNATURE_VERIFY_INVALIDKEY_ERR", objs);
            throw new IDTokenValidationFailedException("InvalidKeyException Message:" + e.getMessage(), e);

        } catch (InvalidJwtException e) {
            Object[] objs = new Object[] { this.clientId, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), this.signingAlgorithm };
            Tr.error(tc, "OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR", objs);
            throw new IDTokenValidationFailedException("SignatureException Message:" + e.getMessage(), e);
        } catch (Exception ise) {
            throw new IDTokenValidationFailedException("Exception Message:" + ise.getMessage(), ise);
        }
        return true; // if we got here with no exceptions, it's valid
    }

    public boolean verify(long clockSkewInSeconds)
            throws IDTokenValidationFailedException {
        return verify(clockSkewInSeconds, getKey());
    }

    @FFDCIgnore({ InvalidKeyException.class, IllegalStateException.class })
    public boolean verify(long clockSkewInSeconds, Object key)
            throws IDTokenValidationFailedException {
        boolean isValid = false;
        setKey(key);

        /*
         * The Client MUST validate the ID Token in the Token Response.
         * To do this, the Client can split the id_token at the period (".") characters,
         * take the second segment, and base64url decode it to obtain a JSON object containing the ID Token
         * Claims, which MUST be validated as follows:
         */

        //1. The Client MUST validate that the iss (issuer) Claim is valid for the Token Endpoint that
        //the id_token was received from.

        //2. The Client MUST validate that the aud (audience) Claim contains its client_id value
        //registered at the Issuer identified by the iss (issuer) Claim as an audience.
        //The ID Token MUST be rejected if the ID Token does not list the Client as a valid audience,
        //or if it contains additional audiences not trusted by the Client.

        //3. If the ID Token contains multiple audiences, the Client SHOULD verify that an azp Claim is present.

        //4. If an azp (authorized party) Claim is present, the Client SHOULD verify and that its client_id
        //is the Claim value.

        //5. The current time MUST be less than the value of the exp Claim
        //(possibly allowing for some small leeway to account for clock skew).

        //6. The iat Claim can be used to reject tokens that were issued too far away from the current time,
        //limiting the amount of time that nonces need to be stored to prevent attacks.
        //The acceptable range is Client specific.

        //7. If the acr Claim was requested, the Client SHOULD check that the asserted Claim Value is
        //appropriate. The meaning and processing of acr Claim Values is out of scope for this specification.

        //8. When a max_age request is made, the Client SHOULD check the auth_time Claim value and
        //request re-authentication if it determines too much time has elapsed
        //since the last End-User authentication.

        //setupClock(clockSkewInSeconds);
        this.clockSkewInSeconds = clockSkewInSeconds;
        String[] jwtParts = JsonTokenUtil.splitTokenString(this.tokenString);
        boolean rpSpecifiedSignatureAlgorithm = true;
        if (this.signingAlgorithm.equals(Constants.SIG_ALG_NONE)) {
            rpSpecifiedSignatureAlgorithm = false;
        }
        if (jwtParts.length == 2) {
            if (rpSpecifiedSignatureAlgorithm) {
                Tr.error(tc, "OIDC_IDTOKEN_SIGNATURE_VERIFY_MISSING_SIGNATURE_ERR", new Object[] { this.clientId, this.signingAlgorithm });
                throw IDTokenValidationFailedException.format("OIDC_IDTOKEN_SIGNATURE_VERIFY_MISSING_SIGNATURE_ERR", this.clientId, this.signingAlgorithm);
            }
            WSJsonToken token = JsonTokenUtil.deserialize(jwtParts, tokenString);
            fromJsonToken(token); // set my payload and header
            if (this.payload.get(PayloadConstants.AUDIENCE) != null) {
                CheckAudience checkAudience = new CheckAudience(clientId, this.payload);
                checkAudience.check();
            }
            isValid = verifyTokenIssueAndExpTime(token);
            return isValid;

        } else if (jwtParts.length > 2) {
            String alg = null;
            if (!rpSpecifiedSignatureAlgorithm) {
                String jwtHeaderSegment = jwtParts[0];
                JsonParser parser = new JsonParser();
                JsonObject header = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtHeaderSegment))
                        .getAsJsonObject();
                alg = header.get("alg").getAsString();

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Signing Algorithm from header: " + alg);
                }
            } else {
                String jwtHeaderSegment = jwtParts[0];
                JsonParser parser = new JsonParser();
                JsonObject header = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtHeaderSegment))
                        .getAsJsonObject();
                String algHeader = header.get("alg").getAsString();
                if (!(this.signingAlgorithm.equals(algHeader))) {
                    Tr.error(tc, "OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR_ALG_MISMATCH", new Object[] { this.clientId, this.signingAlgorithm, algHeader });
                    throw IDTokenValidationFailedException.format("OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR_ALG_MISMATCH", this.clientId, this.signingAlgorithm, algHeader);
                }
                alg = this.signingAlgorithm;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "RP specified Signing Algorithm : " + alg);
                }
            }

            try {

                JsonTokenUtil.validateTokenString(this.tokenString, alg, getKey(alg), this.clockSkewInSeconds, false);
                WSJsonToken token = JsonTokenUtil.deserialize(JsonTokenUtil.splitTokenString(this.tokenString), this.tokenString);

                fromJsonToken(token); // set my payload and header
                if (token.getExpiration() > 0 && token.getIssuedAt() > 0) {
                    // for backwards compat w. net.oauth
                    verifyTokenIssueAndExpTime(token);
                }
                if (checkIssuer(this.clientId, this.issuers, token.getIssuer())) {
                    if (this.payload.get(PayloadConstants.AUDIENCE) != null) {
                        CheckAudience checkAudience = new CheckAudience(this.clientId, this.payload);
                        checkAudience.check();
                    }
                    isValid = true;
                }

            } catch (InvalidKeyException e) {
                Object[] objs = new Object[] { this.clientId,
                        e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(),
                        this.signingAlgorithm };
                Tr.error(tc, "OIDC_IDTOKEN_SIGNATURE_VERIFY_INVALIDKEY_ERR", objs);
                throw new IDTokenValidationFailedException("InvalidKeyException Message:" + e.getMessage(), e);
            } catch (InvalidJwtException e) {
                Object[] objs = new Object[] { this.clientId, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), this.signingAlgorithm };
                Tr.error(tc, "OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR", objs);
                throw new IDTokenValidationFailedException("SignatureException Message:" + e.getMessage(), e);
            } catch (IllegalStateException e) {
                Throwable cause = e;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }

                WSJsonToken token = JsonTokenUtil.deserialize(jwtParts, this.tokenString);
                Object[] objs = new Object[] { this.clientId, cause.getMessage() == null ? e.getClass().getSimpleName() : cause.getMessage(), System.currentTimeMillis(), token.getExpiration(), token.getIssuedAt() };
                if (cause.getMessage().contains("No installed provider"))
                    Tr.error(tc, "JWK_ENDPOINT_MISSING_ERR", objs);
                else
                    Tr.error(tc, "OIDC_IDTOKEN_VERIFY_STATE_ERR", objs);
                throw new IDTokenValidationFailedException("IllegalStateException Message:" + e.getMessage(), e);
            } catch (Exception e) {
                Tr.error(tc, "OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR", new Object[] { this.clientId, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), this.signingAlgorithm });
                throw new IDTokenValidationFailedException(e.getClass().getName() + " Message:" + e.getMessage() == null ? "" : e.getMessage(), e);
            }
            /*
             * String sigInput = jwtParts[0] + "." + jwtParts[1];
             * byte[] decodedSig = Base64.decodeBase64(jwtParts[2]);
             *
             * // So first determine whether asymmetric alg is used for signing
             * //decode header and see the "alg" header parameter
             * String headerStr = JsonTokenUtil.decodeFromBase64String(jwtParts[0]);
             *
             * // Validate signature
             * //byte[] keyValue = SYMKEY_DATA.getBytes();
             * //HmacSHA256Verifier verifier = new HmacSHA256Verifier(keyValue);
             *
             * //byte[] decodedSig = Base64.decodeBase64(jwtParts[2]);
             *
             * //verifier.verifySignature(sigInput.getBytes(), decodedSig);
             */
        }

        return isValid;
    }

    private void initializeHeader() {
        String[] jwtParts = JsonTokenUtil.splitTokenString(this.tokenString);

        String jwtHeaderSegment = jwtParts[0];
        JsonParser parser = new JsonParser();
        JsonObject jHeader = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtHeaderSegment))
                .getAsJsonObject();
        if (this.header == null) {
            this.header = new JWSHeader();
        }
        JsonElement algElement = jHeader.get("alg");
        if (algElement != null) {
            String element = algElement.getAsString();
            if (element != null) {
                this.header.setAlgorithm(element);
            }
        }

        JsonElement idElement = jHeader.get(HeaderConstants.KEY_ID);
        if (idElement != null) {
            String element = idElement.getAsString();
            if (element != null) {
                this.header.setKeyId(element);
            }
        }

        idElement = jHeader.get(HeaderConstants.X509_TP);
        if (idElement != null) {
            String element = idElement.getAsString();
            if (element != null) {
                this.header.setX509Thumbprint(element);
            }
        }

        idElement = jHeader.get(HeaderConstants.X509_CERT);
        if (idElement != null) {
            String element = idElement.getAsString();
            if (element != null) {
                this.header.setX509Certificate(element);
            }
        }

        idElement = jHeader.get(HeaderConstants.X509_URL);
        if (idElement != null) {
            String element = idElement.getAsString();
            if (element != null) {
                this.header.setX509Url(element);
            }
        }

    }

    /**
     * Gets the correct type of Key object from the key field.
     * Could be either a private or public key, depending on how this object was constructed.
     *
     * @param alg
     * @return
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     */
    //@FFDCIgnore({InvalidKeyException.class})
    private Key getKey(String alg) throws UnsupportedEncodingException, InvalidKeyException {
        Key keyUsed = null;
        if ("RS256".equals(alg)) {
            keyUsed = (Key) getKey();
        } else if ("HS256".equals(alg)) {
            byte[] keyBytes = null;
            if (getKey() instanceof String) {
                keyBytes = ((String) getKey()).getBytes("UTF-8"); //TODO
            } else if (getKey() instanceof byte[]) {
                keyBytes = (byte[]) getKey();
            } else {
                throw new InvalidKeyException("Not a valid key");
            }
            keyUsed = new HmacKey(keyBytes);
        }
        return keyUsed;
    }

    public String parseAndVerify(JWT jwt) {
        return null;
    }

    public Payload createPayloadFromString(String payloadString) {
        //new Gson().
        return null;
    }

    /**
     * @return the key
     */
    public Object getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(Object key) {
        this.key = key;
    }
}
