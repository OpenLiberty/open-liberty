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

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.joda.time.Duration;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.jwt.consumer.JwtContext;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonToken;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;

/**
 * Some utility functions for {@link JsonToken}s.
 */
public class JsonTokenUtil {

    public static final String DELIMITER = ".";
    public static final long DEFAULT_SKEW_IN_SECONDS = 180;
    public static final Duration SKEW = Duration.standardMinutes(3);

    public static String toBase64(JsonObject json) {
        return convertToBase64(toJson(json));
    }

    public static String toJson(JsonObject json) {
        return new Gson().toJson(json);
    }

    public static String toJsonFromObj(Object json) {
        return new Gson().toJson(json);
    }

    public static String convertToBase64(String source) {
        return Base64.encodeBase64URLSafeString(StringUtils.getBytesUtf8(source));
    }

    public static String decodeFromBase64String(String encoded) {
        return new String(Base64.decodeBase64(encoded));
    }

    public static String fromBase64ToJsonString(String source) {
        return StringUtils.newStringUtf8(Base64.decodeBase64(source));
    }

    public static String toDotFormat(String... parts) {
        StringBuffer result = new StringBuffer();
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                if (i > 0 && i < parts.length) {
                    result.append(".");
                }
                String part = parts[i];
                result.append((part == null) ? "" : part);
            }
        }
        return result.toString();
    }

    public static boolean isCurrentTimeInInterval(long clockSkewInSeconds, long issuedAtMsec, long expirationMsec) {
        long skew = clockSkewInSeconds * 1000;
        long now = System.currentTimeMillis();
        boolean isAfterEarliest = now + skew > issuedAtMsec;
        boolean isBeforeLatest = now - skew < expirationMsec;
        return isAfterEarliest && isBeforeLatest;
    }

    /**
     * @param tokenString
     *            The original encoded representation of a JWT
     * @return Three components of the JWT as an array of strings
     */
    public static String[] splitTokenString(String tokenString) {
        boolean isPlainTextJWT = false;
        if (tokenString.endsWith(".")) {
            isPlainTextJWT = true;
        }
        String[] pieces = tokenString.split(Pattern.quote(JsonTokenUtil.DELIMITER));
        if (!isPlainTextJWT && pieces.length != 3) {
            throw new IllegalStateException("Expected JWT to have 3 segments separated by '" +
                    JsonTokenUtil.DELIMITER + "', but it has " + pieces.length + " segments");
        }
        return pieces;
    }

    public static WSJsonToken deserialize(String[] pieces, String tokenString) {

        String jwtHeaderSegment = pieces[0];
        String jwtPayloadSegment = pieces[1];
        //byte[] signature = Base64.decodeBase64(pieces[2]);
        JsonParser parser = new JsonParser();
        JsonObject header = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtHeaderSegment))
                .getAsJsonObject();
        JsonObject payload = parser.parse(JsonTokenUtil.fromBase64ToJsonString(jwtPayloadSegment))
                .getAsJsonObject();

        WSJsonToken jsonToken = new WSJsonToken(header, payload, SKEW, tokenString);
        return jsonToken;
    }

    public static void fromJsonToken(WSJsonToken token, JWTPayload resultPayload) {
        if (token == null || resultPayload == null) {
            return;
        }
        JsonObject payload = token.getPayload();
        if (payload == null) {
            return;
        }
        Set<Entry<String, JsonElement>> entries = payload.entrySet();
        Iterator<Entry<String, JsonElement>> it = entries.iterator();
        while (it.hasNext()) {
            Entry<String, JsonElement> entry = it.next();
            String key = entry.getKey();
            JsonElement jsonElem = entry.getValue();
            if (jsonElem.isJsonPrimitive()) {
                resultPayload.put(key, getJsonPrimitive(jsonElem.getAsJsonPrimitive()));
            } else if (jsonElem.isJsonArray()) {
                resultPayload.put(key, createListFromJsonArray(jsonElem.getAsJsonArray()));
            } else if (jsonElem.isJsonObject()) {
                resultPayload.put(key, createMapFromJsonObject(jsonElem.getAsJsonObject()));
            } else {
                resultPayload.put(key, jsonElem);
            }
        }
    }

    public static void fromJsonToken(WSJsonToken token, JWSHeader resultHeader) {
        if (token == null || resultHeader == null) {
            return;
        }
        JsonObject header = token.getHeader();
        if (header == null) {
            return;
        }
        Set<Entry<String, JsonElement>> entries = header.entrySet();
        Iterator<Entry<String, JsonElement>> it = entries.iterator();
        while (it.hasNext()) {
            Entry<String, JsonElement> entry = it.next();
            String key = entry.getKey();
            JsonElement jsonElem = entry.getValue();
            if (jsonElem.isJsonPrimitive()) {
                if (jsonElem.getAsJsonPrimitive().isString()) {
                    addToHeaderFields(resultHeader, key, jsonElem.getAsString());
                }
                resultHeader.put(key, getJsonPrimitive(jsonElem.getAsJsonPrimitive()));
            } else if (jsonElem.isJsonArray()) {
                JsonArray jsonarray = jsonElem.getAsJsonArray();
                List<String> list = new ArrayList<String>();
                for (int i = 0; i < jsonarray.size(); i++) {
                    JsonElement arrayElement = jsonarray.get(i);
                    if (arrayElement.isJsonPrimitive() && arrayElement.getAsJsonPrimitive().isString()) {
                        list.add(arrayElement.getAsString());
                    }
                }
                resultHeader.put(key, list);
                addToHeaderFields(resultHeader, key, list);
            } else if (entry.getValue().isJsonObject()) {
                //TODO
            }
        }
    }

    public static void addToHeaderFields(JWSHeader header, String key, String value) {

        HeaderParameter param = HeaderParameter.valueOf(key.toUpperCase());
        //TYP, CTY, ALG, JKU, JWK, KID, X5U, X5T, X5C, CRIT;
        switch (param) {
        case TYP:
            header.setType(value);
            break;
        case CTY:
            header.setContentType(value);
            break;
        case ALG:
            header.setAlgorithm(value);
            break;
        case JKU:
            header.setJwkUrl(value);
            break;
        case JWK:
            header.setJwk(value);
            break;
        case KID:
            header.setKeyId(value);
            break;
        case X5U:
            header.setX509Url(value);
            break;
        case X5T:
            header.setX509Thumbprint(value);
            break;
        case X5C:
            header.setX509Certificate(value);
            break;
        default:
            break;

        }
    }

    public static void addToHeaderFields(JWSHeader header, String key, List<String> value) {
        HeaderParameter param = HeaderParameter.valueOf(key.toUpperCase());
        switch (param) {
        case CRIT:
            header.setCritical(value);
            break;
        default:
            break;
        }
    }

    /**
     * Extract payload from token without validating.
     *
     */
    public static JWTPayload getPayload(String tokenString) {
        JWTPayload payload = null;
        String[] jwtParts = splitTokenString(tokenString);
        if (jwtParts.length >= 2) {
            WSJsonToken token = deserialize(jwtParts, tokenString);
            payload = new JWTPayload();
            fromJsonToken(token, payload);
        }
        return payload;
    }

    /**
     * Get Aud element.
     *
     */
    protected static String getElement(JWTPayload payload, String element) {
        String output = null;
        if (payload != null) {
            Object outObj = payload.get(element);
            if (outObj instanceof String) {
                output = (String) outObj;
            } else if (outObj instanceof List) {
                if (((List<String>) outObj).size() == 1) {
                    output = ((List<String>) outObj).get(0);
                }
                // if there are multiple values, return null.
            }
        }
        return output;
    }

    /**
     * Get Aud element.
     *
     */
    public static String getAud(JWTPayload payload) {
        return getElement(payload, PayloadConstants.AUDIENCE);
    }

    /**
     * Get Iss element.
     *
     */
    public static String getIss(JWTPayload payload) {
        return getElement(payload, PayloadConstants.ISSUER);
    }

    /**
     * Get Sub element.
     *
     */
    public static String getSub(JWTPayload payload) {
        return getElement(payload, PayloadConstants.SUBJECT);
    }

    private JsonTokenUtil() {
    }

    public static String accessTokenHash(@Sensitive String accessToken) {
        byte[] accessTokenBytes = null;
        accessTokenBytes = Base64Coder.getBytes(accessToken);
        // left 128 bits of hash value of access token
        byte[] left_hash = new byte[16];
        String atHash = null;
        String hashAlg = "SHA-256";//this.getSigner().getSignatureAlgorithm().getHashAlgorithm();
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(hashAlg);
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
            //e.printStackTrace();
            //warning message //todo
            //return atHash
        }

        if (digest != null) {
            byte[] hash = null;
            hash = digest.digest(accessTokenBytes);
            if (hash != null) {
                // Get left 128 bits of hash
                System.arraycopy(hash, 0, left_hash, 0, 16);
                // Base64 encode the left half of hash
                atHash = Base64.encodeBase64URLSafeString(left_hash);
            }
        }

        return atHash;
    }

    /**
     * check Signature, iat, and exp only, which is what the old net.oauth code used to do.
     * Will throw an exception if the token fails to validate.
     *
     * @param tokenString
     * @param alg
     * @param key
     * @param secondsClockSkew
     * @param forSignatureOnly
     * @throws InvalidJwtException
     */
    public static void validateTokenString(String tokenString, String alg, @Sensitive Key key, long secondsClockSkew, boolean forSignatureOnly) throws InvalidJwtException {

        // alg is HS256 or RS256 which is consistent with AlgorithmIdentifiers
        AlgorithmConstraints algorithmConstraints = new AlgorithmConstraints(ConstraintType.WHITELIST,
                alg);

        JwtConsumer firstPassJwtConsumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();
        JwtContext jwtContext = firstPassJwtConsumer.process(tokenString);

        JwtConsumerBuilder secondBuilder = new JwtConsumerBuilder()
                .setVerificationKey(key)
                .setJwsAlgorithmConstraints(algorithmConstraints)
                .setRelaxVerificationKeyValidation() // relaxes hs256 key length reqmt. to be consistent w. old net.oauth behavior
                .setSkipDefaultAudienceValidation();

        if (forSignatureOnly) {
            secondBuilder.setSkipAllValidators();
        } else {
            if (secondsClockSkew > Integer.MAX_VALUE) {
                // before downcasting, truncate max clock skew to 68 years if it's greater than that.
                secondsClockSkew = Integer.MAX_VALUE;
            }
            secondBuilder = secondBuilder.setAllowedClockSkewInSeconds((int) secondsClockSkew);
        }

        JwtConsumer secondPassJwtConsumer = secondBuilder.build();
        secondPassJwtConsumer.processContext(jwtContext);
    }

    static Object getJsonPrimitive(JsonPrimitive primitive) {
        if (primitive == null) {
            return primitive;
        }
        if (primitive.isNumber()) {
            return getJsonPrimitiveNumber(primitive);
        } else if (primitive.isString()) {
            return primitive.getAsString();
        } else if (primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        return primitive;
    }

    static Number getJsonPrimitiveNumber(JsonPrimitive primitive) {
        long longVal = primitive.getAsLong();
        double doubleVal = primitive.getAsDouble();
        if (longVal < doubleVal) {
            // The long value must have been rounded/truncated, so the "true" value should be a double
            return doubleVal;
        }
        return longVal;
    }

    static List<Object> createListFromJsonArray(JsonArray array) {
        if (array == null) {
            return null;
        }
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < array.size(); i++) {
            JsonElement arrayElement = array.get(i);
            if (arrayElement.isJsonPrimitive()) {
                list.add(getJsonPrimitive(arrayElement.getAsJsonPrimitive()));
            } else if (arrayElement.isJsonArray()) {
                list.add(createListFromJsonArray(arrayElement.getAsJsonArray()));
            } else if (arrayElement.isJsonObject()) {
                list.add(createMapFromJsonObject(arrayElement.getAsJsonObject()));
            } else {
                list.add(arrayElement);
            }
        }
        return list;
    }

    static Map<String, Object> createMapFromJsonObject(JsonObject object) {
        if (object == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<String, Object>();
        Iterator<Entry<String, JsonElement>> iter = object.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, JsonElement> entry = iter.next();
            String key = entry.getKey();
            JsonElement subElement = entry.getValue();
            if (subElement.isJsonPrimitive()) {
                map.put(key, getJsonPrimitive(subElement.getAsJsonPrimitive()));
            } else if (subElement.isJsonArray()) {
                map.put(key, createListFromJsonArray(subElement.getAsJsonArray()));
            } else if (subElement.isJsonObject()) {
                map.put(key, createMapFromJsonObject(subElement.getAsJsonObject()));
            } else {
                map.put(key, subElement);
            }
        }
        return map;
    }
}
