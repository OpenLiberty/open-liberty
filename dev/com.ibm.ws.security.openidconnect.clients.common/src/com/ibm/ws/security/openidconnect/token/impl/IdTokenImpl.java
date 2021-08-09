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
package com.ibm.ws.security.openidconnect.token.impl;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;

import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.openidconnect.token.PayloadConstants;

public class IdTokenImpl implements Serializable {

    private static final long serialVersionUID = -6131956027925854513L;

    private final Map<String, Object> mapAll = new HashMap<String, Object>();

    // JWT ID
    static final String CLIENT_ID = "azp2";
    byte[] idTokenPart2Bytes;
    String accessToken;
    String refreshToken;

    public IdTokenImpl(com.ibm.ws.security.openidconnect.token.IDToken origIdToken,
            String idTokenString, // this can not be null
            String accessToken,
            String refreshToken) {
        origIdToken.addToIdTokenImpl(this);
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.idTokenPart2Bytes = getPart2PlainText(idTokenString);
    }

    /**
     * @param idTokenString
     * @return
     */
    byte[] getPart2PlainText(String idTokenString) {
        // part1.part2.part3 or part1.part2
        int index1 = idTokenString.indexOf(".") + 1; // skip the "."
        int index2 = idTokenString.indexOf(".", index1);
        if (index2 < 0)
            index2 = idTokenString.length(); // does not have part3
        String part2 = idTokenString.substring(index1, index2);
        return Base64.decodeBase64(part2);
    }

    /**
     * Returns the JWT ID of the IdToken
     * This is not required.
     * Case sensitive
     * (key-id jti)
     *
     * @return JWT ID
     */

    public String getJwtId() {
        return (String) mapAll.get(PayloadConstants.JWTID);
    }

    /**
     * returns the Type of IdToken, such as: Bearer
     * This is not required.
     * Case sensitive
     * (key-id typ)
     *
     * @return Token Type
     */

    public String getType() {
        return (String) mapAll.get(PayloadConstants.TYPE);
    }

    /**
     * returns Issuer Identifier for the Issuer of the Response
     * This is required
     * Case sensitive
     * (claim iss)
     *
     * @return Issuer
     */

    public String getIssuer() {
        return (String) mapAll.get(PayloadConstants.ISSUER);
    }

    /**
     * returns the Subject Identifier
     * This is required.
     * Case sensitive
     * (claim sub)
     *
     * @return subject Id
     */

    public String getSubject() {
        return (String) mapAll.get(PayloadConstants.SUBJECT);
    }

    /**
     * returns the audience(s)
     * This is required
     * Maybe case sensitive
     * (claim aud)
     *
     * @return audience(s)
     */

    @SuppressWarnings("unchecked")
    public List<String> getAudience() {
        Object obj = mapAll.get(PayloadConstants.AUDIENCE);
        // Convert to List<String> in set method
        return (List<String>) obj;
    }

    /**
     * returns the client Id
     * This is not required
     * (key-id azp2)
     *
     * @return Client Id
     */

    public String getClientId() {
        return (String) mapAll.get(CLIENT_ID);
    }

    /**
     * return the expiration time of the Id Token
     * The time is represented as the number of seconds from 1970-01-01T0:0:0:0Z
     * This is required
     * (claim exp)
     *
     * @return Expiration time in seconds
     */

    public long getExpirationTimeSeconds() {
        return (Long) mapAll.get(PayloadConstants.EXPIRATION_TIME_IN_SECS);
    }

    /**
     * return the time which Id Token is not valid before it
     * The time is represented as the number of seconds from 1970-01-01T0:0:0:0Z
     * This is not required
     * (key-id nbf)
     *
     * @return Not Before Time in seconds
     */

    public long getNotBeforeTimeSeconds() {
        return (Long) mapAll.get(PayloadConstants.NOT_BEFORE_TIME_IN_SECS);
    }

    /**
     * returns the issued time of Id Token
     * This is required
     * (claim iat)
     *
     * @return the issued time in seconds
     */

    public long getIssuedAtTimeSeconds() {
        return (Long) mapAll.get(PayloadConstants.ISSUED_AT_TIME_IN_SECS);
    }

    /**
     * returns The authorization time of the Id Token
     * This is not required
     * (key-id auth_time)
     *
     * @return The authorization time in seconds
     */

    public long getAuthorizationTimeSeconds() {
        return (Long) mapAll.get(PayloadConstants.AUTHZ_TIME_IN_SECS);
    }

    /**
     * returns the value of nonce
     * This value is optional usually.
     *
     * But it's required when the request of a RP Client provides a nonce.
     * In this case, the value of nonce must be the same as the one that
     * the RP Client provides.
     *
     * case sensitive
     * (claim nonce)
     *
     * @return the value of nonce
     */

    public String getNonce() {
        return (String) mapAll.get(PayloadConstants.NONCE);
    }

    /**
     * returns the Hash code of access token
     * This is optional
     * (claim at_hash)
     *
     * @return the Hash code of the access token
     */

    public String getAccessTokenHash() {
        return (String) mapAll.get(PayloadConstants.AT_HASH);
    }

    /**
     * returns Authentication Context Class Reference
     * This is optional
     * (claim acr)
     *
     * @return Authentication Context Class Reference
     */

    public String getClassReference() {
        return (String) mapAll.get(PayloadConstants.CLASS_REFERENCE);
    }

    /**
     * returns Authentication Methods References
     * This is optional
     * (claim amr)
     *
     * @return Authentication Methods References
     */

    @SuppressWarnings("unchecked")
    public List<String> getMethodsReferences() {
        return (List<String>) mapAll.get(PayloadConstants.METHODS_REFERENCE);
    }

    /**
     * Authorized Party
     * This is optional
     * Case sensitive
     * (claim azp)
     *
     * @return Authorized Party
     */

    public String getAuthorizedParty() {
        return (String) mapAll.get(PayloadConstants.AUTHORIZED_PARTY);
    }

    /**
     * Using the key to get its value
     *
     * @param key
     *            - the claim or key-id
     * @return The value
     */

    public Object getClaim(String key) {
        return mapAll.get(key);
    }

    /**
     * get all the claims in the payload of Id Token
     *
     * @return all the claims in the payload of Id Token
     */

    public Map<String, Object> getAllClaims() {
        return new HashMap<String, Object>(mapAll); // clone it
    }

    // Issuer
    public void setJwtId(String strJwtId) {
        mapAll.put(PayloadConstants.JWTID, strJwtId);
    }

    // Type
    public void setType(String strType) {
        mapAll.put(PayloadConstants.TYPE, strType);
    }

    // Issuer
    public void setIssuer(String strIss) {
        mapAll.put(PayloadConstants.ISSUER, strIss);
    }

    // Subject Id
    public void setSubject(String strSub) {
        mapAll.put(PayloadConstants.SUBJECT, strSub);
    }

    // client id
    public void setAudience(Object objAud) {
        if (objAud instanceof List<?>) {
            mapAll.put(PayloadConstants.AUDIENCE, objAud);
        } else if (objAud instanceof String) {
            List<String> list = new ArrayList<String>();
            list.add((String) objAud);
            mapAll.put(PayloadConstants.AUDIENCE, list);
        } else {//TODO
                //Tr.error(tc, "OIDC_IDTOKEN_BAD_CLAIM_CLASS", PayloadConstants.AUDIENCE, objAud.toString());
        }
    }

    public void setClientId(String clientId) {
        mapAll.put(CLIENT_ID, clientId);
    }

    // expiration time (number)
    public void setExpirationTimeSeconds(long lExp) {
        mapAll.put(PayloadConstants.EXPIRATION_TIME_IN_SECS, Long.valueOf(lExp));
    }

    // not before time seconds (number)
    public void setNotBeforeTimeSeconds(long lNotBeforeSeconds) {
        mapAll.put(PayloadConstants.NOT_BEFORE_TIME_IN_SECS, Long.valueOf(lNotBeforeSeconds));
    }

    // IDToken(JWT) issuing time  (number)
    public void setIssuedAtTimeSeconds(long lIat) {
        mapAll.put(PayloadConstants.ISSUED_AT_TIME_IN_SECS, Long.valueOf(lIat));
    }

    public void setAuthorizationTimeSeconds(long lAT) {
        mapAll.put(PayloadConstants.AUTHZ_TIME_IN_SECS, Long.valueOf(lAT));
    }

    // nonce
    public void setNonce(String strNonce) {
        mapAll.put(PayloadConstants.NONCE, strNonce);
    }

    // access token hash value
    public void setAccessTokenHash(String strAt_hash) {
        mapAll.put(PayloadConstants.AT_HASH, strAt_hash);
    }

    // Authentication Context Context Class Reference (Not in use for now)
    public void setClassReference(String strAcr) {
        mapAll.put(PayloadConstants.CLASS_REFERENCE, strAcr);
    }

    // Authentication Methods References (Not in use for now)
    public void setMethodsReferences(List<String> listAmr) {
        mapAll.put(PayloadConstants.METHODS_REFERENCE, listAmr);
    }

    // Authorized party - The party to which the idToken is issued to
    public void setAuthorizedParty(String strAzp) {
        mapAll.put(PayloadConstants.AUTHORIZED_PARTY, strAzp);
    }

    // Other claims
    public void setOtherClaims(String key, Object value) {
        mapAll.put(key, value);
    }

    //
    // Example: Private Credential: IDToken:iss=https://localhost:8999/oidc/endpoint/OidcConfigSample, sub=testuser, aud=client01, exp=1391718615, iat=1391715015, at_hash=Xdplqpld3TOjqA0FSf7zqw
    //

    @Override
    public String toString() {
        Map<String, Object> mapTmp = getAllClaims();
        StringBuffer sb = new StringBuffer(Constants.TOKEN_TYPE_ID_TOKEN);

        if (!mapTmp.isEmpty()) {
            sb.append(":");
        }

        // required fields
        // Issuer
        if (mapTmp.get(PayloadConstants.ISSUER) != null) {
            appendClaimKey(sb, "iss").append((String) mapTmp.get(PayloadConstants.ISSUER));
        }

        if (mapTmp.get(PayloadConstants.TYPE) != null) {
            appendClaimKey(sb, "type").append((String) mapTmp.remove(PayloadConstants.TYPE));
        }

        if (mapTmp.get(CLIENT_ID) != null) {
            appendClaimKey(sb, "client_id").append((String) mapTmp.remove(CLIENT_ID));
        }

        // Subject Id
        if (mapTmp.get(PayloadConstants.SUBJECT) != null) {
            appendClaimKey(sb, "sub").append((String) mapTmp.remove(PayloadConstants.SUBJECT));
        }

        // audiences
        if (mapTmp.get(PayloadConstants.AUDIENCE) != null) {
            appendClaimKey(sb, "aud").append(getListString((List<?>) mapTmp.remove(PayloadConstants.AUDIENCE)));
        }

        // expiration time (number)
        if (mapTmp.get(PayloadConstants.EXPIRATION_TIME_IN_SECS) != null) {
            appendClaimKey(sb, "exp").append(Long.parseLong(mapTmp.remove(PayloadConstants.EXPIRATION_TIME_IN_SECS).toString())); // long
        }

        // IDToken(JWT) issuing time  (number)
        if (mapTmp.get(PayloadConstants.ISSUED_AT_TIME_IN_SECS) != null) {
            appendClaimKey(sb, "iat").append(Long.parseLong(mapTmp.remove(PayloadConstants.ISSUED_AT_TIME_IN_SECS).toString()));
        }

        // optional fields (though could be required in some conditions)
        // nonce
        if (mapTmp.get(PayloadConstants.NONCE) != null) {
            appendClaimKey(sb, "nonce").append((String) mapTmp.remove(PayloadConstants.NONCE));
        }
        // access token hash value
        if (mapTmp.get(PayloadConstants.AT_HASH) != null) {
            appendClaimKey(sb, "at_hash").append((String) mapTmp.remove(PayloadConstants.AT_HASH));
        }
        // Authentication Context Context Class Reference (Not in use for now)
        if (mapTmp.get(PayloadConstants.CLASS_REFERENCE) != null) {
            appendClaimKey(sb, "acr").append((String) mapTmp.remove(PayloadConstants.CLASS_REFERENCE));
        }
        // Authentication Methods References (Not in use for now)
        if (mapTmp.get(PayloadConstants.METHODS_REFERENCE) != null) {
            appendClaimKey(sb, "amr").append(getListString((List<?>) mapTmp.remove(PayloadConstants.METHODS_REFERENCE)));
        }
        // Authorized party - The party to which the idToken is issued to
        if (mapTmp.get(PayloadConstants.AUTHORIZED_PARTY) != null) {
            appendClaimKey(sb, "azp").append((String) mapTmp.remove(PayloadConstants.AUTHORIZED_PARTY));
        }

        // other claims
        Set<Map.Entry<String, Object>> entries = mapTmp.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            Object obj = entry.getValue();
            appendClaimKey(sb, key).append(obj);
        }

        return sb.toString();
    }

    protected StringBuffer appendClaimKey(StringBuffer sb, String claimKey) {
        if (sb.indexOf("=") > 0) {
            // Already at least one claim in the string, so add a comma
            sb.append(", ");
        }
        sb.append(claimKey).append("=");
        return sb;
    }

    protected String getListString(List<?> list) {
        if (list == null) {
            return null;
        }
        return list.toString();
    }

    //
    // Example: Private Credential: {"iss":"https://localhost:8999/oidc/endpoint/OidcConfigSample","sub":"testuser","aud":"client01","exp":1391718615,"iat":1391715015,"at_hash":"Xdplqpld3TOjqA0FSf7zqw"}
    //

    public String getAllClaimsAsJson() {
        try {
            return new String(idTokenPart2Bytes, Constants.UTF_8);
        } catch (UnsupportedEncodingException e) {
            // this should not happen
            return new String(idTokenPart2Bytes);
        }
    }

    /** {@inheritDoc} */

    public String getAccessToken() {
        return accessToken;
    }

    /** {@inheritDoc} */

    public String getRefreshToken() {
        return refreshToken; // this could be null if not exists
    }

}
