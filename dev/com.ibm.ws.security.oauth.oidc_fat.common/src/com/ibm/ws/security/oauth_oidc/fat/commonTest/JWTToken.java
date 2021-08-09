/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.joda.time.Duration;
import org.joda.time.Instant;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.openidconnect.token.HeaderConstants;
import com.ibm.ws.security.openidconnect.token.PayloadConstants;

import net.oauth.jsontoken.JsonToken;
import net.oauth.jsontoken.SystemClock;
import net.oauth.jsontoken.crypto.HmacSHA256Signer;
import net.oauth.jsontoken.crypto.RsaSHA256Signer;

public class JWTToken {
    static private final Class<?> thisClass = JWTToken.class;
    public static CommonValidationTools validationTools = new CommonValidationTools();

    // need net.oauth.jsontoken-1.1/lib/gson-2.2.4.jar
    // need net.oauth.jsontoken-1.1/lib/jsontoken-1.1-r42.jar
    // need joda-time/lib/joda-time-1.6.2.jar

    // import net.oauth.jsontoken.crypto.RsaSHA256Signer;
    private static final Duration SKEW = Duration.standardMinutes(3);

    String _issuerCompany = null;
    String _keyId = null;
    byte[] _hs256Key = null;
    HmacSHA256Signer _hmacSha256Signer = null; // need
                                               // net.oauth.jsontoken-1.1/lib/jsontoken-1.1-r42.jar
    RsaSHA256Signer _rsaSha256Signer = null; // need
                                             // net.oauth.jsontoken-1.1/lib/jsontoken-1.1-r42.jar
    JsonToken _jwtToken = null; // new JsonToken(signer, clock);
    JsonObject _headerObj = null;
    JsonObject _payloadObj = null;
    Duration _expOverride = null;
    Duration _iatOverride = null;
    RSAPrivateKey _privateKey = null;

    String _signedAndSerializedString = null;

    // HashMap<String, Object> _header = new HashMap<String, Object>();
    // HashMap<String, Object> _payload = new HashMap<String, Object>();

    SystemClock _clock = null;

    /**
     * TODO: need to be able to handle both HS256 and RS256 (and none)
     * 
     * @param issuerCompany
     * @param keyId
     * @param hs256Key
     * @throws Exception
     */
    public JWTToken(String issuerCompany, String keyId, TestSettings settings) throws Exception { // InvalidKeyException

        _issuerCompany = issuerCompany;
        _clock = new SystemClock(SKEW);
        _keyId = keyId;

        String clientSecret = settings.getClientSecret();
        if (clientSecret == null) {
            throw new Exception("JWT cannot be created for test because the test settings do not include a client secret.");
        }
        _hs256Key = clientSecret.getBytes();
        _hmacSha256Signer = new HmacSHA256Signer(_issuerCompany, _keyId, _hs256Key);
        _jwtToken = new JsonToken(_hmacSha256Signer, _clock);

        _headerObj = _jwtToken.getHeader();
        _payloadObj = _jwtToken.getPayloadAsJsonObject();
    }

    /**
     * TODO: need to be able to handle both HS256 and RS256 (and none)
     * 
     * @param issuerCompany
     * @param keyId
     * @throws Exception
     */
    public JWTToken(String issuerCompany, String keyId,
            String strKeyStorePathName, String keyPassword, String keyAlias)
            throws Exception { // InvalidKeyException
        Log.info(thisClass, "PrintJWTTokenParms", "Should build RSA");

        _issuerCompany = issuerCompany;
        _clock = new SystemClock(SKEW);
        _keyId = keyId;

        _privateKey = (RSAPrivateKey) getPrivateKey(strKeyStorePathName, keyPassword, keyPassword, keyAlias);
        // TODO: get the private key
        _rsaSha256Signer = new RsaSHA256Signer(issuerCompany, keyId,
                _privateKey);
        _jwtToken = new JsonToken(_rsaSha256Signer, _clock);

        _headerObj = _jwtToken.getHeader();
        _payloadObj = _jwtToken.getPayloadAsJsonObject();
    }

    public static Key getPrivateKey(String keystoreFileName, String keystorePassword, String keyPassword, String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        // KeyStore ks = KeyStore.getInstance( "PKCS12" );//"JKS");
        ks.load(new FileInputStream(keystoreFileName), keystorePassword.toCharArray());
        if (keyPassword == null || keyPassword.length() == 0) {
            keyPassword = keystorePassword;
        }
        Key key = ks.getKey(alias, keyPassword.toCharArray());
        return key;
    }

    public void setExpOverride(Duration expOverride) {
        _expOverride = expOverride;
    }

    public void setIatOverride(Duration iatOverride) {
        _iatOverride = iatOverride;
    }

    /**
     * check com.ibm.ws.security.openidconnect.token.PayloadConstants for keys
     * 
     * @param key
     * @param listString
     */
    public void setPayloadProp(String key, List<String> listString) {
        JsonArray jsonArray = handleList(listString);
        _payloadObj.add(key, jsonArray);
    }

    /**
     * check com.ibm.ws.security.openidconnect.token.PayloadConstants for keys
     * 
     * @param key
     * @param listString
     */
    public void setPayloadProp(String key, String pString) {
        Log.info(thisClass, "setPayloadProp", "key: " + key + ", value: "
                + pString);
        _payloadObj.addProperty(key, pString);
    }

    /**
     * check com.ibm.ws.security.openidconnect.token.PayloadConstants for keys
     * 
     * @param key
     * @param listString
     */
    public void setPayloadProp(String key, Long pLong) {
        Log.info(thisClass, "setPayloadProp", "key: " + key + ", value: "
                + pLong.toString());
        _payloadObj.addProperty(key, pLong);
    }

    /**
     * check com.ibm.ws.security.openidconnect.token.PayloadConstants for keys
     * 
     * @param key
     * @param listString
     */
    public void setHeaderProp(String key, List<String> listString) {
        JsonArray jsonArray = handleList(listString);
        _headerObj.add(key, jsonArray);
    }

    /**
     * check com.ibm.ws.security.openidconnect.token.PayloadConstants for keys
     * 
     * @param key
     * @param String
     */
    public void setHeaderProp(String key, String pString) {
        Log.info(thisClass, "setHeaderProp", "key: " + key + ", value: "
                + pString);
        _headerObj.addProperty(key, pString);
    }

    /**
     * check com.ibm.ws.security.openidconnect.token.PayloadConstants for keys
     * 
     * @param key
     * @param long
     */
    public void setHeaderProp(String key, Long pLong) {
        Log.info(thisClass, "setHeaderProp", "key: " + key + ", value: "
                + pLong.toString());
        _headerObj.addProperty(key, pLong);
    }

    public long getPayloadLong(String key) {
        try {
            JsonPrimitive objLong = _payloadObj.getAsJsonPrimitive(key);
            if (objLong == null) {
                return -1L;
            }
            return objLong.getAsLong();
        } catch (Exception e) {
            return -1L;
        }
    }

    public void setPayloadInstant(String key, Instant time) {
        _jwtToken.setParam(key, time.getMillis() / 1000);
    }

    public void setPayloadIat(Instant iat) {
        // public void setPayloadIat(Long iat) {

        if (iat == null) {
            _jwtToken.setIssuedAt(_clock.now());
        } else {
            Log.info(thisClass, "setPayloadIat",
                    "passed in iat: " + iat.toString());
            // _jwtToken.setIssuedAt(new Instant(iat * 1000)) ;
            _jwtToken.setIssuedAt(iat);
        }
        // Log.info(thisClass, "setPayloadIat", "real iat: " +
        // _jwtToken.getIssuedAt());
        // Log.info(thisClass, "setPayloadIat", "real iat: " +
        // getPayloadLong(PayloadConstants.ISSUED_AT_TIME_IN_SECS));

    }

    public Instant addToCurrentTime(Long plusMilliSecs) {
        return _clock.now().plus(plusMilliSecs);
    }

    public void setPayloadExp(Long exp) {

        // to Not set an exp, just skip calling this method
        Long lIat = getPayloadLong(PayloadConstants.ISSUED_AT_TIME_IN_SECS);
        if (exp == null) {
            if (lIat != -1) {
                // lIat plus default of 2 hours
                _jwtToken.setExpiration(new Instant((lIat + 7200) * 1000));
            } else {
                _jwtToken.setExpiration(_clock.now().plus(
                        Duration.standardHours(1)));
            }
        } else {
            if (lIat != -1) {
                // lIat plus default of 1 hour
                _jwtToken.setExpiration(new Instant((lIat + exp) * 1000));
            } else {
                _jwtToken.setExpiration(_clock.now().plus(exp));
            }
        }
        // Log.info(thisClass, "setPayloadExp", "standard hour " +
        // Duration.standardHours(1)) ;
        // Log.info(thisClass, "setPayloadExp", "standard hour " +
        // Duration.standardDays(1)) ;
        // Log.info(thisClass, "setPayloadExp", "real exp: "
        // +_jwtToken.getExpiration()) ;
        // Log.info(thisClass, "setPayloadExp", "real exp: " +
        // getPayloadLong(PayloadConstants.EXPIRATION_TIME_IN_SECS));

    }

    public void printJWTParms() {
        String[] allKeys = { "iss", "sub", "aud", "exp", "nbf", "iat", "jti" };
        for (String key : allKeys) {
            Log.info(thisClass, "printParms",
                    key + ": " + _jwtToken.getParamAsPrimitive(key));
            if (key.contains("iat")) {
                Log.info(thisClass, "printJWTParms",
                        key + _jwtToken.getIssuedAt());
            }
            if (key.contains("exp")) {
                Log.info(thisClass, "printJWTParms",
                        key + _jwtToken.getExpiration());
            }
        }
        Log.info(thisClass, "PrintParms", "_jwtToken: " + _jwtToken.toString());
    }

    public String getJWTTokenString() throws Exception {

        if (_signedAndSerializedString == null) {
            try {
                printJWTParms();
                _signedAndSerializedString = _jwtToken.serializeAndSign();
                Log.info(thisClass, "getJWTTokenString",
                        "_signedAndSerializedString is: "
                                + _signedAndSerializedString);
            } catch (SignatureException e) {
                throw e;
            }
        }
        return _signedAndSerializedString;
    }

    JsonArray handleList(List<String> strlist) {
        JsonArray list = null; // need
                               // net.oauth.jsontoken-1.1/lib/gson-2.2.4.jar
        if (strlist != null) {
            list = new JsonArray();
            for (String str : strlist) {
                Log.info(thisClass, "handleList", "String entry: " + str);
                JsonPrimitive jsonPrimitiveObj = new JsonPrimitive(str);
                list.add(jsonPrimitiveObj);
            }
        }
        return list;
    }

    // Allow extra keys
    static final ArrayList<String> _payloadKeys = new ArrayList<String>();
    static {
        // regular keys
        _payloadKeys.add(PayloadConstants.EXPIRATION_TIME_IN_SECS); // "exp"
        _payloadKeys.add(PayloadConstants.NOT_BEFORE_TIME_IN_SECS); // = "nbf";
        _payloadKeys.add(PayloadConstants.ISSUED_AT_TIME_IN_SECS); // = "iat";
        _payloadKeys.add(PayloadConstants.ISSUER); // = "iss";
        _payloadKeys.add(PayloadConstants.AUDIENCE); // = "aud";
        _payloadKeys.add(PayloadConstants.JWTID); // = "jti";
        _payloadKeys.add(PayloadConstants.TYPE); // = "typ";
        _payloadKeys.add(PayloadConstants.SUBJECT); // = "sub";
        _payloadKeys.add(PayloadConstants.AUTHZ_TIME_IN_SECS); // = "auth_time";
        _payloadKeys.add(PayloadConstants.AUTHORIZED_PARTY); // = "azp";
        _payloadKeys.add(PayloadConstants.NONCE); // = "nonce";
        _payloadKeys.add(PayloadConstants.AT_HASH); // = "at_hash";
        _payloadKeys.add(PayloadConstants.CLASS_REFERENCE); // = "acr";
        _payloadKeys.add(PayloadConstants.METHODS_REFERENCE); // = "amr";
        // extra key for ietf_jwt
        _payloadKeys.add("scope"); // extra for IETF_JWT
    }

    // No extra key for now
    static final ArrayList<String> _headerKeys = new ArrayList<String>();
    static {
        _headerKeys.add(HeaderConstants.TYPE); // ="typ";
        _headerKeys.add(HeaderConstants.CONTENT_TYPE); // ="cty";
        _headerKeys.add(HeaderConstants.ALGORITHM); // ="alg";
        _headerKeys.add(HeaderConstants.JWK_URL); // ="jku";
        _headerKeys.add(HeaderConstants.JWK); // ="jwk";
        _headerKeys.add(HeaderConstants.KEY_ID); // ="kid";
        _headerKeys.add(HeaderConstants.X509_URL); // ="x5u";
        _headerKeys.add(HeaderConstants.X509_TP); // ="x5t";
        _headerKeys.add(HeaderConstants.X509_CERT); // ="x5c";
        _headerKeys.add(HeaderConstants.CRITICAL); // ="crit";
    }

    void setIat(TestSettings testSettings, String parmToOverride,
            String overrideValue) throws Exception {
        try {
            if (parmToOverride.equals(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS)) {
                if (overrideValue == null) {
                    Log.info(thisClass, "buildAJWTToken",
                            "Omitting \'iat\' from the JWT token");
                } else {
                    setPayloadIat(addToCurrentTime(Long.valueOf(overrideValue)));
                }
            } else {
                if (parmToOverride
                        .equals(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS
                                + "string")) {
                    setPayloadProp(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS,
                            overrideValue);
                } else {
                    if (parmToOverride.equals(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS + "fromToken")) {
                        // _jwtToken.setIssuedAt(Long.valueOf(overrideValue).longValue()
                        // );
                        setPayloadIat(null);

                    } else {
                        setPayloadIat(null);
                    }
                }
            }
        } catch (Exception e) {
            Log.error(thisClass, "setIat ", e, "Exception occurred");
        }
    }

    void setExp(TestSettings testSettings, String parmToOverride,
            String overrideValue) throws Exception {
        try {
            if (parmToOverride
                    .equals(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS)) {
                if (overrideValue == null) {
                    Log.info(thisClass, "buildAJWTToken",
                            "Omitting \'exp\' from the JWT token");
                } else {
                    Log.info(thisClass, "buildAJWTToken", "Overriding \'exp\' in the JWT token");
                    if (overrideValue.startsWith("hardcoded")) {
                        _jwtToken.setExpiration(new Instant(Long.valueOf(overrideValue.split(":")[1]) * 1000));
                    } else {
                        setPayloadExp(Long.valueOf(overrideValue));
                    }
                }
            } else {
                setPayloadExp(null);
            }
        } catch (Exception e) {
            Log.error(thisClass, "setExp ", e, "Exception occurred");
        }
    }

    void setAlg(TestSettings testSettings, String parmToOverride,
            String overrideValue) throws Exception {
        try {
            if (parmToOverride.equals(Constants.HEADER_ALGORITHM)) {
                setHeaderProp(Constants.HEADER_ALGORITHM, overrideValue);
            } else {
                setHeaderProp(Constants.HEADER_ALGORITHM,
                        validationTools.setExpectedSigAlg(testSettings));
            }
        } catch (Exception e) {
            Log.error(thisClass, "setAlg ", e, "Exception occurred");
        }
    }

    void setKid(TestSettings testSettings, String parmToOverride,
            String overrideValue) throws Exception {

        try {
            if (parmToOverride.equals(Constants.HEADER_KEY_ID)) {
                setHeaderProp(Constants.HEADER_KEY_ID, overrideValue);
            } else {
                if (Constants.JWK_CERT.equals(testSettings.getRsCertType())) {
                    final SecureRandom RANDOM = new SecureRandom();
                    String letters = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789+@";
                    String myKid = "";
                    for (int i = 0; i < 8; i++) {
                        int index = RANDOM.nextInt(letters.length());
                        myKid += letters.substring(index, index + 1);
                    }
                    setHeaderProp(Constants.HEADER_KEY_ID, myKid);
                }
            }
        } catch (Exception e) {
            Log.error(thisClass, "setKid ", e, "Exception occurred");
        }
    }

    void setIss(TestSettings testSettings, String parmToOverride,
            String overrideValue) throws Exception {
        try {
            if (parmToOverride.equals(Constants.PAYLOAD_ISSUER)) {
                if (overrideValue == null) {
                    Log.info(thisClass, "buildAJWTToken",
                            "Omitting \'iss\' from the JWT token");
                } else {
                    setPayloadProp(Constants.PAYLOAD_ISSUER, overrideValue);
                }
            } else {
                // set default value
                setPayloadProp(Constants.PAYLOAD_ISSUER,
                        testSettings.getClientID());
            }
        } catch (Exception e) {
            Log.error(thisClass, "setIss ", e, "Exception occurred");
        }
    }

    void setSub(TestSettings testSettings, String parmToOverride,
            String overrideValue) throws Exception {
        try {
            if (parmToOverride.equals(Constants.PAYLOAD_SUBJECT)) {
                if (overrideValue == null) {
                    Log.info(thisClass, "buildAJWTToken",
                            "Omitting \'sub\' from the JWT token");
                } else {
                    setPayloadProp(Constants.PAYLOAD_SUBJECT, overrideValue);
                }
            } else {
                // set default value
                setPayloadProp(Constants.PAYLOAD_SUBJECT,
                        testSettings.getAdminUser());
            }
        } catch (Exception e) {
            Log.error(thisClass, "setSub ", e, "Exception occurred");
        }
    }

    void setAud(TestSettings testSettings, String parmToOverride, String overrideValue) throws Exception {
        try {
            if (parmToOverride.equals(Constants.PAYLOAD_AUDIENCE)) {
                if (overrideValue == null) {
                    Log.info(thisClass, "buildAJWTToken",
                            "Omitting \'aud\' from the JWT token");
                } else {
                    if (overrideValue.contains(",")) {
                        Log.info(thisClass, "setAud", "detected a list for aud");
                        List<String> theList = Arrays.asList(overrideValue.split("\\s*,\\s*"));
                        Log.info(thisClass, "setAud", "built list is: " + theList);
                        setPayloadProp(Constants.PAYLOAD_AUDIENCE, theList);
                    } else {
                        setPayloadProp(Constants.PAYLOAD_AUDIENCE, overrideValue);
                    }
                }
            } else {
                // set default value
                setPayloadProp(Constants.PAYLOAD_AUDIENCE, buildAudience(testSettings));
            }
        } catch (Exception e) {
            Log.error(thisClass, "setAud ", e, "Exception occurred");
        }
    }

    void setNbf(TestSettings testSettings, String parmToOverride,
            String overrideValue) throws Exception {
        try {
            if (parmToOverride
                    .equals(Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS)) {
                if (overrideValue == null) {
                    // use current time in nbf
                    setPayloadInstant(
                            Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS,
                            addToCurrentTime(Long.valueOf("0")));
                } else {
                    // set nbf to current time plus the value passed in
                    setPayloadInstant(
                            Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS,
                            addToCurrentTime(Long.valueOf(overrideValue)));
                }
            }
        } catch (Exception e) {
            Log.error(thisClass, "setNbf ", e, "Exception occurred");
        }
    }

    void setJti(TestSettings testSettings, String parmToOverride, String overrideValue) throws Exception {
        try {
            if (parmToOverride.equals(Constants.PAYLOAD_JWTID)) {
                if (overrideValue == null) {
                    // not sure what the default jti would be - current INSTANT
                    // time???
                } else {
                    // set nbf to current time plus the value passed in
                    setPayloadProp(Constants.PAYLOAD_JWTID, overrideValue);
                }
            }
        } catch (Exception e) {
            Log.error(thisClass, "setJti ", e, "Exception occurred");
        }
    }

    void setOther(TestSettings testSettings, String parmToOverride,
            String overrideValue) throws Exception {
        try {
            if (parmToOverride.equals("other")) {
                if (overrideValue == null) {
                    Log.info(thisClass, "setOther",
                            "Not setting an unrecognized key");
                } else {
                    // set other
                    setPayloadProp("other", overrideValue);
                }
            }
        } catch (Exception e) {
            Log.error(thisClass, "setOther ", e, "Exception occurred");
        }
    }

    void setMisc(TestSettings testSettings, String parmToOverride, String overrideValue) throws Exception {
        Log.info(thisClass, "setMisc", "Setting key: " + parmToOverride + " to value: " + overrideValue);
        try {
            if (overrideValue == null) {
                Log.info(thisClass, "setMisc", "Not setting an unrecognized key: " + parmToOverride);
            } else {
                // set caller specified
                setPayloadProp(parmToOverride, overrideValue);
            }
        } catch (Exception e) {
            Log.error(thisClass, "setMisc ", e, "Exception occurred");
        }
    }

    public void buildAJWTToken(String testName, TestSettings testSettings,
            String parmToOverride, String overrideValue, Map<String, String> currentValues) {

        try {

            String[] supportedKeys = { Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS + "string",
                    Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, Constants.HEADER_ALGORITHM, Constants.HEADER_KEY_ID, Constants.PAYLOAD_ISSUER,
                    Constants.PAYLOAD_SUBJECT, Constants.PAYLOAD_AUDIENCE, Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS, Constants.PAYLOAD_JWTID, "other" };
            String[] override = null;
            // set iat based on callers request
            override = setOverride(testSettings, Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS, parmToOverride, overrideValue, currentValues);
            setIat(testSettings, override[0], override[1]);

            // set exp based on callers request
            override = setOverride(testSettings, Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS, parmToOverride, overrideValue, currentValues);
            setExp(testSettings, override[0], override[1]);

            // set alg based on callers request
            override = setOverride(testSettings, Constants.HEADER_ALGORITHM, parmToOverride, overrideValue, currentValues);
            setAlg(testSettings, override[0], override[1]);

            // set kid based on callers request
            override = setOverride(testSettings, Constants.HEADER_KEY_ID, parmToOverride, overrideValue, currentValues);
            setKid(testSettings, override[0], override[1]);

            // set iss based on callers request
            override = setOverride(testSettings, Constants.PAYLOAD_ISSUER, parmToOverride, overrideValue, currentValues);
            setIss(testSettings, override[0], override[1]);

            // set sub based on callers request
            override = setOverride(testSettings, Constants.PAYLOAD_SUBJECT, parmToOverride, overrideValue, currentValues);
            setSub(testSettings, override[0], override[1]);

            // set aud based on callers request
            override = setOverride(testSettings, Constants.PAYLOAD_AUDIENCE, parmToOverride, overrideValue, currentValues);
            setAud(testSettings, override[0], override[1]);

            // set nbf based on callers request
            override = setOverride(testSettings, Constants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS, parmToOverride, overrideValue, currentValues);
            setNbf(testSettings, override[0], override[1]);

            // set jti based on callers request
            override = setOverride(testSettings, Constants.PAYLOAD_JWTID, parmToOverride, overrideValue, currentValues);
            Log.info(thisClass, "override stuff", "override-0: " + override[0] + " override-1: " + override[1]);
            setJti(testSettings, override[0], override[1]);

            // set "other" an unrecognized key based on callers request
            override = setOverride(testSettings, "other", parmToOverride, overrideValue, currentValues);
            setOther(testSettings, override[0], override[1]);

            if (!validationTools.isInList(supportedKeys, parmToOverride)) {
                setMisc(testSettings, parmToOverride, overrideValue);
            }
            setMisc(testSettings, "token_type", "Bearer");
            setMisc(testSettings, "realmName", testSettings.getRealm());

        } catch (Exception e) {
            Log.error(thisClass, testName.toString(), e, "Exception occurred");
        }

    }

    String buildAudience(TestSettings settings) {

        // hack around missing audience config attr for propagation testing
        String audience = settings.getTokenEndpt();
        if (settings.getRsTokenType().equals(Constants.JWT_TOKEN) || settings.getRsTokenType().equals(Constants.BUILT_JWT_TOKEN)) {
            if (settings.getUseJwtConsumer()) {
                audience = settings.getJwtConsumerUrl();
            } else {
                audience = settings.getRSProtectedResource();
            }
        } else {
            int indexAud = audience.lastIndexOf("/");
            if (indexAud > 0) {
                audience = audience.substring(0, indexAud);
            }
        }
        return audience;
    }

    String[] setOverride(TestSettings testSettings, String currentKey, String parmToOverride, String overrideValue, Map<String, String> copiedValues) {
        String NO_OVERRIDE = "nothingToOverride";

        // if the call is overriding the value, set that
        if (parmToOverride.contains(currentKey)) {
            return new String[] { parmToOverride, overrideValue };
        } else {
            // check to see if it's in the token we're trying to copy...
            if (copiedValues != null) {
                String value = copiedValues.get(currentKey);
                if (value != null) {
                    if (currentKey.equals(Constants.PAYLOAD_ISSUED_AT_TIME_IN_SECS)) {
                        return new String[] { currentKey + "fromToken", value };
                    }
                    if (currentKey.equals(Constants.PAYLOAD_EXPIRATION_TIME_IN_SECS)) {
                        return new String[] { "hardcoded" + currentKey + "string", value };
                    }
                    return new String[] { currentKey, value };
                }
            }
        }
        return new String[] { NO_OVERRIDE, NO_OVERRIDE };
    }
}