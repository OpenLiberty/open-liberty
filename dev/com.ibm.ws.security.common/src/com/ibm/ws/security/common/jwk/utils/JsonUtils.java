/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.jwk.utils;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.jose4j.lang.JoseException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.TraceConstants;

public class JsonUtils {
    private static final TraceComponent tc = Tr.register(JsonUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    public static final String CFG_KEY_ID = "id";
    public static final String CFG_KEY_ISSUER = "issuer";
    public static final String CFG_KEY_JWK_ENABLED = "jwkEnabled";
    public static final String CFG_KEY_VALID = "expiry";
    public static final String CFG_KEY_JTI = "jti";
    public static final String CFG_KEY_SCOPE = "scope";
    public static final String CFG_KEY_AUDIENCES = "audiences";
    public static final String CFG_KEY_SIGNATURE_ALGORITHM = "signatureAlgorithm";
    public static final String CFG_KEY_CLAIMS = "claims";
    public static final String CFG_KEY_KEYSTORE_REF = "keyStoreRef";
    public static final String CFG_KEY_KEY_ALIAS_NAME = "keyAlias";
    public static final String CFG_KEY_TRUSTSTORE_REF = "trustStoreRef";
    public static final String CFG_KEY_TRUSTED_ALIAS = "trustedAlias";
    public static final String CFG_KEY_SHARED_KEY = "sharedKey";
    public static final String CFG_KEY_JWK_ROTATION_TIME = "jwkRotationTime";
    public static final String CFG_KEY_JWK_SIGNING_KEY_SIZE = "jwkSigningKeySize";
    public static final String CFG_KEY_JWK_ENDPOINT_URL = "jwkEndpointUrl";
    public static final String CFG_KEY_CLOCK_SKEW = "clockSkew";

    public static final String JCEPROVIDER_IBM = "IBMJCE";
    public static final String SECRANDOM_SHA1PRNG = "SHA1PRNG";
    public static final String SECRANDOM_IBM = "IBMSecureRandom";

    public static final String ISSUER = "iss";
    public static final String SUBJECT = "sub";
    public static final String AUDIENCE = "aud";
    public static final String SCOPE = "scope";
    public static final String EXPIRATION = "exp";
    public static final String NOT_BEFORE = "nbf";
    public static final String ISSUED_AT = "iat";
    public static final String ID = "jti";
    public static final String KEY = "signKey";
    public static final String ALG = "signAlg";
    public static final String KS = "KeyStore";
    public static final String KS_ALIAS = "KeyStore_ALIAS";
    public static final String TS = "TrustStore";
    public static final String TS_ALIAS = "TrustStore_ALIAS";

    public static final String DELIMITER = ".";

    //    private static AtomicServiceReference<VirtualHost> virtualHostRef;

    public static String convertToBase64(String source) {
        if (source == null) {
            return null;
        }
        return Base64.encodeBase64URLSafeString(StringUtils.getBytesUtf8(source));
    }

    public static String convertToBase64(byte[] source) {
        if (source == null) {
            return null;
        }
        return Base64.encodeBase64URLSafeString(source);
    }

    public static byte[] decodeFromBase64(String encoded) {
        if (encoded == null) {
            return null;
        }
        return Base64.decodeBase64(encoded);
    }

    public static String decodeFromBase64String(String encoded) {
        if (encoded == null) {
            return null;
        }
        return new String(Base64.decodeBase64(encoded));
    }

    public static boolean isBase64Encoded(String str) {
        if (!isNullEmpty(str)) {
            return Base64.isArrayByteBase64(StringUtils.getBytesUtf8(str));
        }
        return false;
    }

    public static String fromBase64ToJsonString(String source) {
        return StringUtils.newStringUtf8(Base64.decodeBase64(source));
    }

    public static String toJson(String source) {
        if (source == null) {
            return null;
        }
        try {
            return StringUtils.newStringUtf8(source.getBytes("UTF8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        }
        return null;
    }

    public static boolean isNullEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static boolean isJson(String tokenString) {
        boolean result = false;
        if (!isNullEmpty(tokenString)) {
            if ((tokenString.startsWith("{") && tokenString.endsWith("}"))
                    || (tokenString.startsWith("[") && tokenString.endsWith("]"))) {
                result = true;
            }
        }
        return result;
    }

    // either from header or payload
    public static Object claimFromJsonObject(String jsonFormattedString, String claimName) throws JoseException {
        if (jsonFormattedString == null) {
            return null;
        }

        Object claim = null;
        //JSONObject jobj = JSONObject.parse(jsonFormattedString);
        Map<String, Object> jobj = org.jose4j.json.JsonUtil.parseJson(jsonFormattedString);
        if (jobj != null) {
            claim = jobj.get(claimName);
        }

        return claim;
    }

    // assuming payload not the whole token string
    public static Map claimsFromJsonObject(String jsonFormattedString) throws JoseException {
        Map claimsMap = new ConcurrentHashMap<String, Object>();
        if (jsonFormattedString == null) {
            return claimsMap;
        }

        //JSONObject jobj = JSONObject.parse(jsonFormattedString);
        Map<String, Object> jobj = org.jose4j.json.JsonUtil.parseJson(jsonFormattedString);
        Set<Entry<String, Object>> entries = jobj.entrySet();
        Iterator<Entry<String, Object>> it = entries.iterator();

        while (it.hasNext()) {
            Entry<String, Object> entry = it.next();

            String key = entry.getKey();
            Object value = entry.getValue();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Key : " + key + ", Value: " + value);
            }
            if (!isNullEmpty(key) && value != null) {
                claimsMap.put(key, value);
            }

            // JsonElement jsonElem = entry.getValue();
        }

        // claims.putAll(jobj.entrySet());

        return claimsMap;
    }

    /**
     * Trims each of the strings in the array provided and returns a new list with each string added to it. If the trimmed string
     * is empty, that string will not be added to the final array. If no entries are present in the final array, null is returned.
     * 
     * @param strings
     * @return
     */
    public static List<String> trimIt(String[] strings) {
        if (strings == null || strings.length == 0) {
            return null;
        }

        List<String> results = new ArrayList<String>();

        for (int i = 0; i < strings.length; i++) {
            String result = trimIt(strings[i]);
            if (result != null) {
                results.add(result);
            }
        }

        if (results.size() > 0) {
            return results;
        } else {
            return null;
        }
    }

    public static String trimIt(String str) {
        if (str == null) {
            return null;
        }
        str = str.trim();
        if (str.isEmpty()) {
            return null;
        }
        return str;
    }

    public static String[] splitTokenString(String tokenString) {
        if (tokenString == null) {
            return null;
        }
        boolean isPlainTextJWT = false;
        if (tokenString.endsWith(".")) {
            isPlainTextJWT = true;
        }
        String[] pieces = tokenString.split(Pattern.quote(DELIMITER));
        if (!isPlainTextJWT && pieces.length != 3) {
            // Tr.warning("Expected JWT to have 3 segments separated by '" +
            // DELIMITER + "', but it has " + pieces.length + " segments");
            return null;
        }
        return pieces;
    }

    /**
     * NOTE: This method DOES NOT support JWE tokens. This method can currently only be used to extract payloads from JWS tokens.
     */
    public static String getPayload(String jwt) {
        // TODO - update for JWE
        String[] jwtInfo = splitTokenString(jwt);
        if (jwtInfo != null) {
            return jwtInfo[1];
        }
        return null;
    }

    public static String getRandom(int length) {
        StringBuffer result = new StringBuffer(length);
        final char[] chars = new char[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                'U', 'V', 'W', 'X', 'Y', 'Z',
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z'
        };
        Random r = getRandom();

        for (int i = 0; i < length; i++) {
            int n = r.nextInt(62);
            result.append(chars[n]);
        }

        return result.toString();
    }

    static Random getRandom() {
        Random result = null;
        try {
            if (Security.getProvider(JCEPROVIDER_IBM) != null) {
                result = SecureRandom.getInstance(SECRANDOM_IBM);
            } else {
                result = SecureRandom.getInstance(SECRANDOM_SHA1PRNG);
            }
        } catch (Exception e) {
            result = new Random();
        }
        return result;
    }

    public static long calculate(long valid) {
        // TODO Auto-generated method stub
        long lifetimeSeconds = valid * 60 * 60;
        long timeInSeconds = System.currentTimeMillis() / 1000;

        return timeInSeconds + lifetimeSeconds;
    }

    public static String toJson(Map<String, Object> claimsMap) {
        // TODO Auto-generated method stub
        return org.jose4j.json.JsonUtil.toJson(claimsMap);
    }

    public static String getDate(long current) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date resultdate = new Date(current);
        return sdf.format(resultdate);
    }
}
