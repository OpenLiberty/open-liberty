/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.common.cl;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.keys.HmacKey;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.InvalidGrantException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.openidconnect.token.JWSHeader;
import com.ibm.ws.security.openidconnect.token.JWTPayload;
import com.ibm.ws.security.openidconnect.token.JsonTokenUtil;
import com.ibm.ws.security.openidconnect.token.TraceConstants;
import com.ibm.ws.security.openidconnect.token.WSJsonToken;

public class JWTVerifier {

    private static final TraceComponent tc = Tr.register(JWTVerifier.class, TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    String _tokenString = null;
    String _clientId = null;
    @Sensitive
    Object _key = null;

    long _lSkewSeconds = 0;

    static final List<String> signAlgorithms = new ArrayList<String>();
    static {
        signAlgorithms.add("RS256");
        signAlgorithms.add("HS256");
    }

    String _signAlgorithm = null;

    String[] _jwtParts = null;

    WSJsonToken _jsonToken = null;
    JWSHeader _header;
    JWTPayload _payload;

    public JWTVerifier(String clientId, @Sensitive Object key, String signAlgorithm, String tokenString, long lSkewSeconds)
            throws InvalidGrantException {
        _tokenString = tokenString;
        _clientId = clientId;
        _key = key;
        _signAlgorithm = signAlgorithm;
        _lSkewSeconds = lSkewSeconds;
        if (tokenString != null)
            _jwtParts = splitTokenString(tokenString);
    }

    public JWTVerifier(String tokenString)
            throws InvalidGrantException {
        _tokenString = tokenString;
        if (tokenString != null)
            _jwtParts = splitTokenString(tokenString);
    }

    void initJsonToken() {
        _jsonToken = JsonTokenUtil.deserialize(_jwtParts, _tokenString);
        _payload = new JWTPayload();
        JsonTokenUtil.fromJsonToken(_jsonToken, _payload);
        _header = new JWSHeader();
        JsonTokenUtil.fromJsonToken(_jsonToken, _header);
    }

    public JWSHeader getJwsHeader() {
        if (_jsonToken == null) {
            initJsonToken();
        }
        return _header;
    }

    public String getAlgHeader() {
        JWSHeader jwsHeader = getJwsHeader();
        return (String) jwsHeader.get("alg");
    }

    /**
     * return Payload
     */
    public JWTPayload getPayload() {
        if (_jsonToken == null) {
            initJsonToken();
        }
        return _payload;
    }

    public String getIssFromPayload() {
        JWTPayload payload = getPayload();
        return (String) payload.get("iss");
    }

    WSJsonToken getJsonToken() {
        if (_jsonToken == null) {
            initJsonToken();
        }
        return _jsonToken;
    }

    // These exceptions will be handled by the caller
    public boolean verifySignature() throws OAuthException {
        return verifySignature(_lSkewSeconds);
    }

    // These exceptions will be handled by the caller
    // contrary to it's name, this method is supposed to check the exp time.
    @FFDCIgnore({ InvalidJwtException.class })
    boolean verifySignature(long skewSeconds) throws OAuthException {
        if (_jwtParts == null) {
            Tr.error(tc, "JWT_JWTTOKEN_NO_TOKEN_ERR");
            throw formatException("JWT_JWTTOKEN_NO_TOKEN_ERR", null);
        }
        String jwtHeaderSegment = _jwtParts[0];
        JsonParser simpleParser = new JsonParser();
        JsonObject header = simpleParser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtHeaderSegment)).getAsJsonObject();

        JsonElement elmAlg = header.get("alg");
        String signAlgorithm = _signAlgorithm;
        if (elmAlg != null) {
            String tmpAlg = elmAlg.getAsString();
            if (!tmpAlg.equalsIgnoreCase(signAlgorithm)) {
                // TODO: We are not able to handle this kind of stuff
                // at this moment. Since no extra secret-keys are defined
                Tr.error(tc, "JWT_JWTTOKEN_SIGNATURE_VERIFY_ERR_ALG_MISMATCH",
                        _clientId, tmpAlg, signAlgorithm);
                throw formatException("JWT_JWTTOKEN_SIGNATURE_VERIFY_ERR_ALG_MISMATCH",
                        null, _clientId, tmpAlg, signAlgorithm);
            }
        }

        if (_jwtParts.length <= 2) {
            Tr.error(tc, "JWT_JWTTOKEN_SIGNATURE_VERIFY_SEGMENT_ERR", _clientId, _signAlgorithm);
            throw formatException("JWT_JWTTOKEN_SIGNATURE_VERIFY_SEGMENT_ERR", null, _clientId, _signAlgorithm);
        }

        try {
            Key theKey = null;
            if (_key instanceof String) {
                try {
                    theKey = new HmacKey(((String) _key).getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                }
            } else if (_key instanceof byte[]) {
                theKey = new HmacKey((byte[]) _key);
            } else if (_key instanceof Key) {
                theKey = (Key) _key;
            }

            JsonTokenUtil.validateTokenString(_tokenString, _signAlgorithm, theKey, _lSkewSeconds, false);

            _payload = new JWTPayload();
            // JsonTokenUtil.fromJsonToken(_jsonToken, _payload);
            JsonTokenUtil.fromJsonToken(getJsonToken(), _payload);
            _header = new JWSHeader();
            JsonTokenUtil.fromJsonToken(_jsonToken, _header);
        } catch (InvalidJwtException e) {
            Object[] objs = new Object[] { _clientId,
                    e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage() };
            Tr.error(tc, "JWT_JWTTOKEN_ILLEGAL_STATE_ERR", objs); // CWWKS1769E
            throw formatException("JWT_JWTTOKEN_ILLEGAL_STATE_ERR", e, objs);
        }

        return true;
    }

    /**
     * @param tokenString
     *            The original encoded representation of a JWT
     * @return Three components of the JWT as an array of strings
     */
    public String[] splitTokenString(String tokenString) throws InvalidGrantException {
        boolean isPlainTextJWT = false;
        if (tokenString.endsWith(".")) {
            isPlainTextJWT = true;
        }
        String[] pieces = tokenString.split(Pattern.quote(JsonTokenUtil.DELIMITER));
        if (!isPlainTextJWT && pieces.length != 3) {
            Tr.error(tc, "JWT_JWTTOKEN_BAD_SEGMENTS_ERR",
                    new Object[] { Long.valueOf(pieces.length) });
            throw formatException("JWT_JWTTOKEN_BAD_SEGMENTS_ERR", null, pieces.length);
        }
        return pieces;
    }

    private InvalidGrantException formatException(String msgKey, Throwable cause, Object... objs) {
        String message = Tr.formatMessage(tc, msgKey, objs);
        return new InvalidGrantException(message, cause);
    }

}
