/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.jwt.ClaimConstants;
import com.ibm.ws.security.fat.common.jwt.HeaderConstants;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.utils.ClaimHelpers;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.jwt.fat.builder.JWTBuilderConstants;

import componenttest.topology.impl.LibertyServer;

public class BuilderHelpers {

    protected static Class<?> thisClass = BuilderHelpers.class;

    public static JSONObject setDefaultClaims(LibertyServer server) throws Exception {
        JSONObject settings = new JSONObject();
        settings.put(ClaimConstants.ISSUER, SecurityFatHttpUtils.getServerIpSecureUrlBase(server) + "jwt/defaultJWT");
        return setDefaultClaims(settings);
    }

    public static JSONObject setDefaultClaims(String builderId) throws Exception {
        JSONObject settings = new JSONObject();
        settings.put(ClaimConstants.ISSUER, builderId);
        return setDefaultClaims(settings);
    }

    public static JSONObject setDefaultClaims(JSONObject settings) throws Exception {
        //        JSONObject settings = new JSONObject();
        //        settings.put(ClaimConstants.ISSUER, createClaimContent(SecurityFatHttpUtils.getServerIpSecureUrlBase(server) + "jwt/defaultJWT", JwtConstants.JWT_BUILDER_ISSUER));
        //        settings.put(ClaimConstants.ISSUED_AT, createClaimContent(setNowLong(), JwtConstants.JWT_BUILDER_ISSUED_AT));
        //        settings.put(ClaimConstants.ISSUER, SecurityFatHttpUtils.getServerIpSecureUrlBase(server) + "jwt/defaultJWT");
        settings.put(ClaimConstants.ISSUED_AT, setNowLong());
        settings.put(ClaimConstants.EXPIRATION_TIME, setNowLong() + (2 * 60 * 60));
        settings.put(ClaimConstants.SUBJECT, null);
        settings.put(ClaimConstants.AUTHORIZED_PARTY, null);
        settings.put(ClaimConstants.JWT_ID, null);

        settings.put(HeaderConstants.ALGORITHM, JwtConstants.SIGALG_RS256);

        return settings;
    }

    public static JSONObject createClaimContent(Object value, String verboseName) throws Exception {

        JSONObject claim = new JSONObject();
        claim.put("value", value);
        claim.put("verboseName", verboseName);

        return claim;
    }

    public static Long setNowLong() throws Exception {

        NumericDate now = NumericDate.now();
        return now.getValue();
    }

    //    public static JSONObject createOverrideClaims() throws Exception{
    //
    //        JSONObject overrides = new JSONObject ;
    //    }

    /**
     * defaultSettings has the default values that we'll always start with. overrideSettings will have values that will override
     * the defaults.
     * overrideSettings can have values conatined in 2 ways either as claim/values or as claim/values in a list of claims. We'll
     * look for and
     * process all 3 sets of values and the order they're processed will dictate which value wins.
     * lowest priority: default
     * mid priority: middle
     *
     * @param defaultSettings
     * @param server
     * @param overrideSettings
     *
     * @return
     * @throws Exception
     */
    public static Expectations createGoodBuilderExpectations(String app, JSONObject defaultSettings, LibertyServer server) throws Exception {
        return createGoodBuilderExpectations(app, defaultSettings, null, server);
    }

    public static Expectations createGoodBuilderExpectations(String app, JSONObject defaultSettings, JSONObject baseSettings, LibertyServer server) throws Exception {

        // defaultSettings = {"iss":"issValue", "aud":"["aud1Val", "aud2Val"]", "overrideSettings": {"aud":"["audVal3", "audVal4"]", "claim_api": {"someclaim":"somevalue", "someclaim2":"somevalue2", "aud":"["someAud1", "someAud2"]"}}}

        Expectations expectations = buildBuilderClientAppExpectations(app, null, server);
        Log.info(thisClass, "createGoodBuilderExpectations", "original json object: " + defaultSettings.toString());

        JSONObject expectationsObject = new JSONObject();
        Set<String> keySet = defaultSettings.keySet();
        Iterator<String> keys = keySet.iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if ("overrideSettings".equals(key)) {
                continue;
            } else {
                expectationsObject.put(key, defaultSettings.get(key));
            }

        }
        Log.info(thisClass, "createGoodBuilderExpectations", "first json object: " + expectationsObject.toString());
        JSONObject overrideSettings = (JSONObject) defaultSettings.get("overrideSettings");
        if (overrideSettings != null) {
            List<String> skipKeys = Arrays.asList(JwtConstants.JWT_BUILDER_CLAIM_API, JwtConstants.JWT_BUILDER_REMOVE_API, JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_TOKEN, JwtConstants.JWT_BUILDER_CLAIMFROM_API, JwtConstants.SHARED_KEY, JwtConstants.SHARED_KEY_TYPE, JwtConstants.JWT_BUILDER_FETCH_API);

            Log.info(thisClass, "createGoodBuilderExpectations", "override json object: " + overrideSettings.toString());
            keySet = overrideSettings.keySet();
            keys = keySet.iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                if (skipKeys.contains(key)) {
                    continue;
                } else {
                    expectationsObject.put(key, overrideSettings.get(key));
                }

            }
            Log.info(thisClass, "createGoodBuilderExpectations", "second json object: " + expectationsObject.toString());

            JSONArray claimFromSettings = (JSONArray) overrideSettings.get(JwtConstants.JWT_BUILDER_CLAIMFROM_API);
            if (claimFromSettings != null) {
                Log.info(thisClass, "createGoodBuilderExpectations", "claimFromSettings json object: " + claimFromSettings.toString());
                for (int i = 0; i < claimFromSettings.size(); i++) {
                    String key = (String) claimFromSettings.get(i);
                    // copy key/value from the "from" token's settings
                    if (key != null) {
                        expectationsObject.put(key, baseSettings.get(key));
                    }
                }
            }

            JSONObject claimSettings = (JSONObject) overrideSettings.get(JwtConstants.JWT_BUILDER_CLAIM_API);
            if (claimSettings != null) {
                Log.info(thisClass, "createGoodBuilderExpectations", "claimSettings json object: " + claimSettings.toString());
                keySet = claimSettings.keySet();
                keys = keySet.iterator();
                while (keys.hasNext()) {
                    String key = keys.next();
                    expectationsObject.put(key, claimSettings.get(key));
                }
            }
            Log.info(thisClass, "createGoodBuilderExpectations", "before remove json objects: " + expectationsObject.toString());
            // now remove any claims that the test will be removing, so, we don't set expectations
            JSONArray removeSettings = (JSONArray) overrideSettings.get(JwtConstants.JWT_BUILDER_REMOVE_API);
            if (removeSettings != null) {
                Log.info(thisClass, "createGoodBuilderExpectations", "removeSettings json object: " + removeSettings.toString());
                for (int i = 0; i < removeSettings.size(); i++) {
                    String key = (String) removeSettings.get(i);
                    expectationsObject.remove(key);
                    // add explicit expectation that key should NOT exist
                    expectations = buildExpectationsForKey(expectations, key, null);
                }
            }

            Log.info(thisClass, "createGoodBuilderExpectations", "before fetch json objects: " + expectationsObject.toString());
            // now remove any claims that the test will be removing, so, we don't set expectations
            JSONArray fetchSettings = (JSONArray) overrideSettings.get(JwtConstants.JWT_BUILDER_FETCH_API);
            if (fetchSettings != null) {
                Log.info(thisClass, "createGoodBuilderExpectations", "fetchSettings json object: " + fetchSettings.toString());
                for (int i = 0; i < fetchSettings.size(); i++) {
                    String key = (String) fetchSettings.get(i);
                    // copy key/value from the "from" token's settings
                    if (key != null) {
                        expectationsObject.put(key, baseSettings.get(key));
                    }
                }
            }
        }

        Log.info(thisClass, "createGoodBuilderExpectations", "final json object: " + expectationsObject.toString());
        keySet = expectationsObject.keySet();
        keys = keySet.iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = expectationsObject.get(key);
            expectations = buildExpectationsForKey(expectations, key, value);
        }

        return expectations;
    }

    //    public static Expectations createGoodBuilderExpectationsOld(String app, String prefix, JSONObject defaultSettings, LibertyServer server) throws Exception {
    //
    //        Expectations expectations = buildBuilderClientAppExpectations(app, null, server);
    //        JSONObject overrideSettings = (JSONObject) defaultSettings.get("overrideSettings");
    //        JSONObject claimSettings = null;
    //        if (overrideSettings != null) {
    //            claimSettings = (JSONObject) overrideSettings.get(JwtConstants.JWT_BUILDER_CLAIM_API);
    //        }
    //
    //        Set<String> keySet = defaultSettings.keySet();
    //        Iterator<String> keys = keySet.iterator();
    //        while (keys.hasNext()) {
    //            //            JSONObject theValue = new JSONObject();
    //            String key = keys.next();
    //            if ("overrideSettings".equals(key)) {
    //                continue;
    //            }
    //            //            theValue = (JSONObject) defaultSettings.get(key);
    //            //            Object value = theValue.get("value");
    //            Object value = defaultSettings.get(key);
    //            //            String verboseName = (String) theValue.get("verboseName");
    //            //            String verboseName = getVerboseName(key);
    //
    //            if (overrideSettings != null && (overrideSettings.containsKey(key) || claimSettings != null && claimSettings.containsKey(key))) {
    //                // use its value, otherwise, use the value from default
    //                //                theValue = (JSONObject) overrideSettings.get(key);
    //                //                value = theValue.get("value");
    //                value = overrideSettings.get(key);
    //                //                verboseName = (String) theValue.get("verboseName");
    //                //                verboseName = getVerboseName(key);
    //
    //                // if we're adding freeform claims, they'll override defaults, and specific api settings
    //                // specific api's are run before the generic claim api's - there is no other firm rule
    //                if (claimSettings != null && claimSettings.containsKey(key)) {
    //                    value = claimSettings.get(key);
    //                }
    //            }
    //
    //            //            if (verboseName != null) {
    //            //
    //            //            }
    //            //
    //            //            ClaimHelpers.updateClaimExpectationsForJsonAttribute(expectations, prefix, key, value);
    //
    //            expectations = buildExpectationsForKey(expectations, prefix, key, value);
    //        }
    //
    //        // now build expectations for claims that are ONLY in the override settings
    //        if (overrideSettings != null) {
    //            Set<String> oKeySet = overrideSettings.keySet();
    //            Iterator<String> oKeys = oKeySet.iterator();
    //            while (oKeys.hasNext()) {
    //                //            JSONObject theValue = new JSONObject();
    //                String oKey = oKeys.next();
    //                if (JwtConstants.JWT_BUILDER_CLAIM_API.equals(oKey)) {
    //                    continue;
    //                }
    //                if (!defaultSettings.containsKey(oKey)) {
    //
    //                    //            theValue = (JSONObject) defaultSettings.get(key);
    //                    //            Object value = theValue.get("value");
    //                    Object oValue = overrideSettings.get(oKey);
    //                    //            String verboseName = (String) theValue.get("verboseName");
    //                    //                    String oVerboseName = getVerboseName(oKey);
    //                    if (claimSettings != null && claimSettings.containsKey(oKey)) {
    //                        oValue = claimSettings.get(oKey);
    //                    }
    //
    //                    expectations = buildExpectationsForKey(expectations, prefix, oKey, oValue);
    //                }
    //            }
    //
    //            if (claimSettings != null) {
    //                Set<String> cKeySet = claimSettings.keySet();
    //                Iterator<String> cKeys = cKeySet.iterator();
    //                while (cKeys.hasNext()) {
    //                    //            JSONObject theValue = new JSONObject();
    //                    String cKey = cKeys.next();
    //                    if (!overrideSettings.containsKey(cKey)) {
    //
    //                        //            theValue = (JSONObject) defaultSettings.get(key);
    //                        //            Object value = theValue.get("value");
    //                        Object cValue = claimSettings.get(cKey);
    //
    //                        expectations = buildExpectationsForKey(expectations, prefix, cKey, cValue);
    //                    }
    //                }
    //            }
    //        }
    //
    //        return expectations;
    //
    //    }

    public static Expectations buildExpectationsForKey(Expectations expectations, String key, Object value) throws Exception {

        List<String> timeClaims = Arrays.asList(ClaimConstants.EXPIRATION_TIME, ClaimConstants.ISSUED_AT, ClaimConstants.NOT_BEFORE);
        List<String> headerClaims = Arrays.asList(HeaderConstants.ALGORITHM, HeaderConstants.KEY_ID);

        String verboseName = getVerboseName(key);
        //        Log.info(thisClass, "buildExpectationsForKey", "Key: " + key);
        //        Log.info(thisClass, "buildExpectationsForKey", "value: " + value);
        //        Log.info(thisClass, "buildExpectationsForKey", "verboseName: " + verboseName);
        if (value instanceof JSONArray) {
            for (int i = 0; i < ((JSONArray) value).size(); i++) {
                Object theValue = ((JSONArray) value).get(i);
                if (key != null && timeClaims.contains(key)) {
                    expectations = ClaimHelpers.updateClaimExpectationsForJsonAttribute(expectations, JwtConstants.JWT_BUILDER_CLAIM, key, verboseName, handleTime((Long) theValue));
                } else {
                    // if the value of a member of an array is null we want to add do not find checks for that null value in the array, but, don't
                    // want to add a check for null in a specific api output
                    if (theValue == null) {
                        expectations = ClaimHelpers.updateExpectationsForJsonAttribute(expectations, JwtConstants.JWT_BUILDER_CLAIM, key, theValue);
                    } else {
                        expectations = ClaimHelpers.updateClaimExpectationsForJsonAttribute(expectations, JwtConstants.JWT_BUILDER_CLAIM, key, verboseName, theValue);
                    }
                }
            }
        } else {
            // TODO - need to update  all of the helper methods to handle true/false values - not sure that the added complication
            // is worth it for the few test cases that use booleans.  Those tests can explicitly add the expectations that they need
            if (value instanceof Boolean) {
                return expectations;
            }
            if (key != null && timeClaims.contains(key)) {
                expectations = ClaimHelpers.updateClaimExpectationsForJsonAttribute(expectations, JwtConstants.JWT_BUILDER_CLAIM, key, verboseName, handleTime((Long) value));
            } else {
                if (headerClaims.contains(key)) {
                    expectations = ClaimHelpers.updateExpectationsForJsonHeaderAttribute(expectations, JwtConstants.JWT_BUILDER_HEADER, key, value);
                } else {
                    expectations = ClaimHelpers.updateClaimExpectationsForJsonAttribute(expectations, JwtConstants.JWT_BUILDER_CLAIM, key, verboseName, value);
                }
            }
        }

        return expectations;
    }

    public static String getVerboseName(String claim) throws Exception {

        switch (claim) {
        case ClaimConstants.ISSUER:
            return JwtConstants.JWT_BUILDER_ISSUER;
        case ClaimConstants.ISSUED_AT:
            return JwtConstants.JWT_BUILDER_ISSUED_AT;
        case ClaimConstants.AUDIENCE:
            return JwtConstants.JWT_BUILDER_AUDIENCE;
        case ClaimConstants.AUTHORIZED_PARTY:
            return JwtConstants.JWT_BUILDER_AUTHORIZEDPARTY;
        case ClaimConstants.JWT_ID:
            return JwtConstants.JWT_BUILDER_JWTID;
        case ClaimConstants.NOT_BEFORE:
            return JwtConstants.JWT_BUILDER_NOTBEFORE;
        case ClaimConstants.EXPIRATION_TIME:
            return JwtConstants.JWT_BUILDER_EXPIRATION;
        case ClaimConstants.SUBJECT:
            return JwtConstants.JWT_BUILDER_SUBJECT;
        default:
            return null;
        }

    }

    public static String handleTime(Long value) throws Exception {
        if (value == null) {
            return Long.toString(-1L);
        }
        if (value <= 0L) {
            return Long.toString(value);
        } else {
            String stringValue = Long.toString(value);
            return stringValue.substring(0, stringValue.length() - 2);
        }

    }

    public static Expectations createBadBuilderExpectations(String app, String msgId, LibertyServer server) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(SecurityFatHttpUtils.getServerUrlBase(server) + app));
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, msgId, "Response did not show the expected failure."));

        return expectations;
    }

    public static JSONObject setClaimsFromToken(String jwtTokenString) throws Exception {

        String[] jwtParts = jwtTokenString.split(Pattern.quote("."));

        String payload = jwtParts[1];
        JSONObject settings = JSONObject.parse(decodeFromBase64String(payload));

        return settings;
    }

    //    public JsonObject deserialize(String jwtPart) {
    //
    //        if (jwtPart == null) {
    //            return null;
    //        }
    //        return Json.createReader(new StringReader(fromBase64ToJsonString(jwtPart))).readObject();
    //
    //    }

    public static String decodeFromBase64String(String encoded) {
        return new String(Base64.decodeBase64(encoded));
    }

    // **************************************************************
    //    public static JwtClaims setDefaultClaims(LibertyServer server) throws Exception {
    //
    //        JwtClaims claims = new JwtClaims();
    //        claims.setIssuer(SecurityFatHttpUtils.getServerIpSecureUrlBase(server) + "jwt/defaultJWT");
    //        claims.setIssuedAtToNow();
    //        claims.getExpirationTime();
    //        //        claims.setClaim("scope", "openid profile");
    //        //        claims.setSubject("testuser");
    //        //        claims.setClaim(JwtConstants.JWT_REALM_NAME, "BasicRealm");
    //        claims.setClaim("token_type", "Bearer");
    //        return claims;
    //    }

    public static Expectations addGoodBuilderCreateClientResponseAndClaimsExpectations(String currentAction, JwtClaims claims, LibertyServer builderServer) throws Exception {

        Expectations expectations = buildBuilderCreateClientAppExpectations(currentAction, builderServer);
        expectations = ClaimHelpers.updateExpectationsForClaimAppOutput(expectations, JwtConstants.JWT_BUILDER_CLAIM, currentAction, claims);
        return expectations;
    }

    public static Expectations buildBuilderCreateClientAppExpectations(String currentAction, LibertyServer builderServer) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, SecurityFatHttpUtils.getServerUrlBase(builderServer) + JWTBuilderConstants.JWT_BUILDER_CREATE_ENDPOINT));

        return expectations;
    }

    public static Expectations addGoodBuilderSetApisClientResponseAndClaimsExpectations(String currentAction, JwtClaims claims, LibertyServer builderServer) throws Exception {

        Expectations expectations = buildBuilderSetApisClientAppExpectations(currentAction, builderServer);
        expectations = ClaimHelpers.updateExpectationsForClaimAppOutput(expectations, JwtConstants.JWT_BUILDER_CLAIM, currentAction, claims);
        return expectations;
    }

    public static Expectations buildBuilderSetApisClientAppExpectations(String currentAction, LibertyServer builderServer) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, SecurityFatHttpUtils.getServerUrlBase(builderServer) + JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT));

        return expectations;
    }

    public static Expectations buildBuilderClientAppExpectations(String builderApp, String currentAction, LibertyServer builderServer) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, SecurityFatHttpUtils.getServerUrlBase(builderServer) + builderApp));

        return expectations;
    }

    public static Expectations buildBuilderClaimsNotFound(Expectations expectations, String prefix, String claim) throws Exception {
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_DOES_NOT_MATCH, prefix + JwtConstants.JWT_BUILDER_JSON + JwtConstants.JWT_BUILDER_GETALLCLAIMS + ".*" + claim + ".*", "Found unknown claim \"" + claim + "\" in the listed claims and it should not be there."));
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_DOES_NOT_MATCH, prefix + JwtConstants.JWT_BUILDER_JSON + "\\{" + ".*" + claim + ".*\\}", "Found unknown claim \"" + claim + "\" in the list of claims and it should not be there."));

        return expectations;
    }

    public static Expectations createGoodValidationEndpointExpectations(String jwtToken, String url) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(null, url));

        JSONObject jwtHeaderObject = getJSONOjbectPart(jwtToken, 0);
        String builderKid = (String) jwtHeaderObject.get(HeaderConstants.KEY_ID);
        String builderAlg = (String) jwtHeaderObject.get(HeaderConstants.ALGORITHM);

        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, "\"kid\":\"" + builderKid + "\".*", "The kid (" + builderKid + ") found in the built token does not match the kid returned by the JwkValidationEndpoint"));
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, "\"alg\":\"" + builderAlg + "\".*", "The algorithm (" + builderAlg + ") found in the built token does not match the algorithm returned by the JwkValidationEndpoint"));

        return expectations;
    }

    /**
     * Get the requested part from a JWT token string - convert it to a JSON object
     *
     * @param jwt_token
     *            - the JWT token string to get part of
     * @param index
     *            - the portion of the token to return
     * @return - the correct portion as a json object
     * @throws Exception
     */
    public static JSONObject getJSONOjbectPart(String jwt_token, int index) throws Exception {
        String[] jwt_token_parts;
        String thisMethod = "getJSONOjbectPart";

        jwt_token_parts = jwt_token.split("\\.");
        if (jwt_token_parts == null) {
            throw new Exception("Failed splitting token");
        }

        JSONObject jsonInfo = JSONObject.parse(decodeFromBase64String(jwt_token_parts[index]));

        return jsonInfo;

    }

}