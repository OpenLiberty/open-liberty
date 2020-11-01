/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder.utils;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.codec.binary.Base64;
import org.jose4j.jwt.NumericDate;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.jwt.HeaderConstants;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.jwt.expectations.JwtApiExpectation;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;
import com.ibm.ws.security.jwt.fat.builder.JWTBuilderConstants;
import com.ibm.ws.security.jwt.fat.builder.expectations.JwtTimeClaimExpectation;

import componenttest.topology.impl.LibertyServer;

public class BuilderHelpers {

    protected static Class<?> thisClass = BuilderHelpers.class;
    protected static List<String> timeClaims = Arrays.asList(PayloadConstants.EXPIRATION_TIME, PayloadConstants.ISSUED_AT, PayloadConstants.NOT_BEFORE);
    protected static final int TokenHeader = 0;
    protected static final int TokenPayload = 1;

    public static JSONObject setDefaultClaims(LibertyServer server) throws Exception {
        JSONObject settings = new JSONObject();
        settings.put(PayloadConstants.ISSUER, SecurityFatHttpUtils.getServerIpSecureUrlBase(server) + "jwt/defaultJWT");
        return setDefaultClaims(settings);
    }

    public static JSONObject setDefaultClaims(String builderId) throws Exception {
        JSONObject settings = new JSONObject();
        settings.put(PayloadConstants.ISSUER, builderId);
        return setDefaultClaims(settings);
    }

    public static JSONObject setDefaultClaims(JSONObject settings) throws Exception {
        settings.put(PayloadConstants.ISSUED_AT, setNowLong());
        settings.put(PayloadConstants.EXPIRATION_TIME, setNowLong() + (2 * 60 * 60));
        settings.put(PayloadConstants.SUBJECT, null);
        settings.put(PayloadConstants.AUTHORIZED_PARTY, null);
        settings.put(PayloadConstants.JWT_ID, null);

        settings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_RS256);

        return settings;

    }

    public static JSONObject setDefaultClaimsWithEncryption(String builderId, String KeyMgmtKeyAlg, String contentEncryptAlg) throws Exception {
        JSONObject settings = new JSONObject();
        settings.put(PayloadConstants.ISSUER, builderId);
        setDefaultClaims(settings);
        settings.put(HeaderConstants.KEY_ID, "");
        settings.remove(HeaderConstants.ALGORITHM);
        settings.put(HeaderConstants.ALGORITHM, KeyMgmtKeyAlg);
        settings.put(HeaderConstants.ENCRYPTION, contentEncryptAlg);
        settings.put(HeaderConstants.TYPE, JWTBuilderConstants.JWE_TYPE);
        settings.put(HeaderConstants.CONTENT_TYPE, JWTBuilderConstants.JWE_CONTENT_TYPE);
        // when we're testing with encrypted tokens, our underlying tools don't have the info to decrypt the JWE to get to the JWS, so skip validation of the time values
        settings.remove(PayloadConstants.EXPIRATION_TIME);
        settings.remove(PayloadConstants.ISSUED_AT);
        return settings;
    }

    public static Long setNowLong() throws Exception {

        NumericDate now = NumericDate.now();
        return now.getValue();
    }

    /**
     * testSettings has the default values that we'll always start with.
     * overrideSettings will have values that will override the defaults.
     * overrideSettings can have values conatined in 2 ways either as
     * claim/values or as claim/values in a list of claims. We'll look for and
     * process all 3 sets of values and the order they're processed will dictate
     * which value wins. lowest priority: default mid priority: middle
     *
     * @param testSettings
     * @param server
     * @param overrideSettings
     *
     * @return
     * @throws Exception
     */
    public static Expectations createGoodBuilderExpectations(String app, JSONObject testSettings, LibertyServer server) throws Exception {
        return createGoodBuilderExpectations(app, testSettings, null, server);
    }

    /**
     * This method will build the expectations for what the builder apis will
     * generate. What the builder creates will be the culmination of multiple
     * requests. So, we need to look at a series of values to build the final
     * value - The order in which we do that is dictated by the order that the
     * builder processes the apis - if they change, we'll need to re-order how
     * we derive the value to expect
     *
     * The test app will create a builder from a config that is specified - that
     * config will dictate specific values be put into the builder. The test app
     * will run the claimFrom api to obtain values from another JWT Token or JWT
     * Token String The test app will then run specific set apis based on
     * key/value claim pairs that we pass into the app (the apis are specific to
     * each key - we simply run the method and pass the value). Next, the test
     * app will run the claim methods to add/modify claims that we specify (the
     * claim methods process key/value pairs generically). After that, the app
     * will run the fetch command to pull values from the registry (based on
     * fetch keys listed in the request) Finally, the app will run the remove
     * api which will remove claims from the JWT (based on remove keys listed in
     * our request)
     *
     * @param app
     * @param testSettings
     * @param claimsFromSettings
     * @param server
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static Expectations createGoodBuilderExpectations(String app, JSONObject testSettings, JSONObject claimsFromSettings, LibertyServer server) throws Exception {

        // testSettings = {"iss":"issValue", "aud":"["aud1Val", "aud2Val"]",
        // "overrideSettings": {"aud":"["audVal3", "audVal4"]", "claim_api":
        // {"someclaim":"somevalue", "someclaim2":"somevalue2",
        // "aud":"["someAud1", "someAud2"]"}}}

        // set expectations for just getting to the test app
        Expectations expectations = buildBuilderClientAppExpectations(app, null, server);

        // create a jsonObject of keys/values that will be used to create
        // expectations
        JSONObject expectedObject = new JSONObject();
        List<String> notExpectedList = new ArrayList<String>();

        processSettingsExpectations(testSettings, expectedObject, notExpectedList, claimsFromSettings);

        // now, we have all of the key/value pairs that we expect and don't
        // expect in the token
        // we've taken the original values, added any additional keys via set
        // apis and claims, we've removed
        // any keys that will be removed and added any values we'll obtain via
        // fetch
        // We'll build expectations off from what we have object
        Log.info(thisClass, "createGoodBuilderExpectations", "final json object: " + expectedObject.toString());
        Set keySet = expectedObject.keySet();
        Iterator<String> keys = keySet.iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = expectedObject.get(key);
            expectations = buildExpectationsForKey(expectations, key, value);
        }
        for (String key : notExpectedList) {
            expectations = buildExpectationsForKey(expectations, key, null);
        }

        return expectations;
    }

    /**
     * build a json object containing all of the key/value pairs as we expect
     * them to be after all of the builder apis process a request made by a test
     * case A test case can pass in request that the test app set/remove/fetch
     * the same or multiple keys. This method will set values in
     * expectationsObject in the same order that the test will process them. So,
     * if the test app will use the set api to set let's say the scope, then the
     * scope will be set via a claim, the claim value will be the value that
     * actually ends up in the token as it is the last value set.
     *
     * @param expectations
     *            - the expectations that the tests will use to validate
     * @param testSettings
     *            - the json object containing all of the default values,
     *            override values, remove values, fetch values, ...
     * @param expectationsObject
     *            - the cummuliative json object - one instance of each key - we
     *            set and override values as we proceed
     * @param claimsFromSettings
     *            - the base/default values
     * @throws Exception
     */
    public static void processSettingsExpectations(JSONObject testSettings, JSONObject expectationsObject, List<String> notExpectedList, JSONObject claimsFromSettings) throws Exception {

        // pull out the values that will override the config settings - these
        // override settings will
        // be used with the set, claims remove, ... apis in the builder test app
        // debug Log.info(thisClass, "createGoodBuilderExpectations", "first
        // json object: " + expectationsObject.toString());
        JSONObject overrideSettings = (JSONObject) testSettings.get("overrideSettings");

        processSettestSettingsExpectations(testSettings, expectationsObject);
        if (overrideSettings != null) {
            processSetOverrideSettingsExpectations(overrideSettings, expectationsObject);
            processOverrideClaimsFromSettingsExpectations(overrideSettings, expectationsObject, claimsFromSettings);
            processOverrideClaimSettingsExpectations(overrideSettings, expectationsObject);
            processOverrideRemoveSettingsExpectations(overrideSettings, expectationsObject, notExpectedList);
            processOverrideFetchSettingsExpectations(overrideSettings, expectationsObject, claimsFromSettings);
        }
    }

    /**
     * Populate expectationsObject with the default values that we would find in
     * the token (bases on the config)
     *
     * @param testSettings
     *            - the default settings object
     * @param expectationsObject
     *            - the resulting expectations Object
     * @throws Exception
     */
    public static void processSettestSettingsExpectations(JSONObject testSettings, JSONObject expectationsObject) throws Exception {
        // create a jsonObject of keys based on default values for this
        // config/test
        Set<String> keySet = testSettings.keySet();
        Iterator<String> keys = keySet.iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if ("overrideSettings".equals(key)) {
                continue;
            } else {
                expectationsObject.put(key, testSettings.get(key));
            }

        }
    }

    /**
     * override existing key/value pairs or add key/value pairs for claims that
     * will be set via the set apis
     *
     * @param overrideSettings
     *            - key/value pairs that will be used with builder apis
     * @param expectationsObject
     *            - the already set key/value pairs that we'll use to create
     *            expectations
     * @throws Exception
     */
    public static void processSetOverrideSettingsExpectations(JSONObject overrideSettings, JSONObject expectationsObject) throws Exception {

        List<String> skipKeys = Arrays.asList(JWTBuilderConstants.JWT_BUILDER_CLAIM_API, JWTBuilderConstants.JWT_BUILDER_REMOVE_API, JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_TOKEN, JWTBuilderConstants.JWT_BUILDER_CLAIMFROM_API, JWTBuilderConstants.SHARED_KEY, JWTBuilderConstants.SHARED_KEY_TYPE, JWTBuilderConstants.JWT_BUILDER_FETCH_API);

        Set<String> keySet = overrideSettings.keySet();
        Iterator<String> keys = keySet.iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            if (skipKeys.contains(key)) {
                continue;
            } else {
                expectationsObject.put(key, overrideSettings.get(key));
            }

        }
    }

    /**
     * override existing key/value pairs or add key/value pairs for claims that
     * will be set via the claimFrom api
     *
     * @param overrideSettings
     *            - key/value pairs that will be used with the builder apis
     * @param expectationsObject
     *            - the already set key/value pairs that will be used to create
     *            expectations
     * @param claimsFromSettings
     *            - the values that we should expect for the keys that will be
     *            use with the claimsFrom api
     * @throws Exception
     */
    public static void processOverrideClaimsFromSettingsExpectations(JSONObject overrideSettings, JSONObject expectationsObject, JSONObject claimsFromSettings) throws Exception {
        // Override and extend the "expectationsObject" with the values that
        // we'll use with the claimsFrom api
        JSONArray claimFromSettings = (JSONArray) overrideSettings.get(JWTBuilderConstants.JWT_BUILDER_CLAIMFROM_API);
        if (claimFromSettings != null) {
            for (int i = 0; i < claimFromSettings.size(); i++) {
                String key = (String) claimFromSettings.get(i);
                // copy key/value from the "from" token's settings
                if (key != null) {
                    expectationsObject.put(key, claimsFromSettings.get(key));
                }
            }
        }

    }

    /**
     * override existing key/value pairs or add key/value pairs for claims that
     * will be set via the claim apis
     *
     * @param overrideSettings
     *            - key/value pairs that will be used with the builder apis
     * @param expectationsObject
     *            - the already set key/value pairs that will be used to create
     *            expectations
     * @throws Exception
     */
    public static void processOverrideClaimSettingsExpectations(JSONObject overrideSettings, JSONObject expectationsObject) throws Exception {

        // Override and extend the "expectationsObject" with the values that
        // we'll use with the claim apis
        JSONObject claimSettings = (JSONObject) overrideSettings.get(JWTBuilderConstants.JWT_BUILDER_CLAIM_API);
        if (claimSettings != null) {
            Set<String> keySet = claimSettings.keySet();
            Iterator<String> keys = keySet.iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                expectationsObject.put(key, claimSettings.get(key));
            }
        }
    }

    /**
     * Remove key/value pair for keys that will be removed by the builder app.
     * Also add expectations that ensure that we dod NOT find the keys that are
     * removed
     *
     * @param expectations
     *            - the already set expectations
     * @param overrideSettings
     *            - key/value pairs that will be used with the builder apis
     * @param expectationsObject
     *            - the already set key/value pairs that will be used to create
     *            expectations
     * @throws Exception
     */
    public static void processOverrideRemoveSettingsExpectations(JSONObject overrideSettings, JSONObject expectationsObject, List<String> notExpectedList) throws Exception {
        // remove entries from "expectationsObject" based on the keys that we
        // specify for the remove api
        Log.info(thisClass, "processOverrideRemoveSettingsExpectations", "before remove json objects: " + expectationsObject.toString());
        // now remove any claims that the test will be removing, so, we don't
        // set expectations
        JSONArray removeSettings = (JSONArray) overrideSettings.get(JWTBuilderConstants.JWT_BUILDER_REMOVE_API);
        if (removeSettings != null) {
            Log.info(thisClass, "processOverrideRemoveSettingsExpectations", "removeSettings json object: " + removeSettings.toString());
            for (int i = 0; i < removeSettings.size(); i++) {
                String key = (String) removeSettings.get(i);
                expectationsObject.remove(key);
                // add explicit expectation that key should NOT exist
                notExpectedList.add(key);
            }
        }
    }

    /**
     * add/update key/value pairs for keys that will be fetched by the builder
     * app. A
     *
     * @param expectations
     *            - the already set expectations
     * @param overrideSettings
     *            - key/value pairs that will be used with the builder apis
     * @param expectationsObject
     *            - the already set key/value pairs that will be used to create
     *            expectations
     * @param claimsFromSettings
     *            - the values that we should expect for the keys that will be
     *            use with the fetch api
     * @throws Exception
     */
    public static void processOverrideFetchSettingsExpectations(JSONObject overrideSettings, JSONObject expectationsObject, JSONObject claimsFromSettings) throws Exception {

        // Override and extend the "expectationsObject" with the values that
        // we'll use with the fetch api
        // now remove any claims that the test will be removing, so, we don't
        // set expectations
        JSONArray fetchSettings = (JSONArray) overrideSettings.get(JWTBuilderConstants.JWT_BUILDER_FETCH_API);
        if (fetchSettings != null) {
            for (int i = 0; i < fetchSettings.size(); i++) {
                String key = (String) fetchSettings.get(i);
                // copy key/value from the "from" token's settings
                if (key != null) {
                    expectationsObject.put(key, claimsFromSettings.get(key));
                }
            }
        }
    }

    /**
     * Build the expectations for each key passed in. Time and header
     * expectations need to be handled a bit differently We'll be validating
     * output that looks like: Response (Full): ******************* Start of
     * jwtbuilderclient/JwtBuilderSetApisClient output *******************
     * configId: null attrs: {"sub":"user2"} JsonAttrs: {"sub":"user2"} Header:
     * JSON:
     * {"kid":"qihY-neTyJwKMXo5sorOqwnHBXzoobkjBwiydfvoW2s","typ":"JWT","alg":"RS256"}
     * Header: JSON Header: Key: kid Value:
     * qihY-neTyJwKMXo5sorOqwnHBXzoobkjBwiydfvoW2s Header: JSON Header: Key: typ
     * Value: JWT Header: JSON Header: Key: alg Value: RS256 Claim: iss:
     * https://9.24.23.93:8920/jwt/defaultJWT Claim: sub: user2 Claim: aud: null
     * Claim: exp: 1568744825 Claim: nbf: -1 Claim: iat: 1568737625 Claim: jti:
     * null Claim: azp: null Claim: JSON:
     * {"sub":"user2","iss":"https://0.0.0.0:8920/jwt/defaultJWT","token_type":"Bearer","exp":1568744825,"iat":1568737625}
     * Claim: JSON: getAllClaims: Key: sub Value: user2 Claim: JSON:
     * getAllClaims: Key: iss Value: https://0.0.0.0:8920/jwt/defaultJWT Claim:
     * JSON: getAllClaims: Key: token_type Value: Bearer Claim: JSON:
     * getAllClaims: Key: exp Value: 1568744825 Claim: JSON: getAllClaims: Key:
     * iat Value: 1568737625 Built JWT Token:
     * eyJraWQiOiJxaWhZLW5lVHlKd0tNWG81c29yT3F3bkhCWHpvb2JrakJ3aXlkZnZvVzJzIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic3ViIjoidXNlcjIiLCJpc3MiOiJodHRwczovLzkuMjQuMjMuOTM6ODkyMC9qd3QvZGVmYXVsdEpXVCIsImV4cCI6MTU2ODc0NDgyNSwiaWF0IjoxNTY4NzM3NjI1fQ.NFOLMtCXewDgxmjFu_YKFqyTkARhCs_AifPGmbAhR3555wPkt2R59unKkQOHJkIZgJPTD9p8FUVzmb9R-oH_0LIAaOdufVicrWu_lEnI4BG-WlM3HunhmlXcAk8gW6jjnW_OOkdcu4RenrDXSnoQwHEE_zaivcyUGtxoN8Whd70HZq5_AdCcGAdp9ENvt1BSvd8pHyz4Qf1gviNcC662XfRNe08Azpy1zrqVe4HbpO0bKQdTHLbs3_ahCQSzG2oZs17bMwGkHUGsb4Yw0NIIBj516hsKZtR3U3y1p_oy776YwVaOQHcqBgsrCvg4jvmZSWe05yZ8cdAgMGrKr1-UBg
     ******************* End of JWTBuilderClient output ******************* <b>Note:</b> When
     * asked to create expectations for keys that have a specific get claim
     * method, we'll need to validate that we get the "Claim: \<key\>:
     * \<value\>" entry with the correct value (keys that do NOT have specific
     * get api methods will NOT have this setting) We need to have the "Claim:
     * JSON: {"\<key\>":"\<value\>", ... }" entry And also, we need to have the
     * "Claim: JSON: getAllClaims: Key: \<key\> Value: \<value\>" entry Header
     * claims are handled in a similar fashion. Finally, time is special. The
     * expectations are created either before or after the token is created. We
     * will never be able to validate that the time is truly correct. So, we'll
     * do the following:
     * <OL>
     * <LI>Create a JwtTimeClaimExpectation expectation with the test calculated
     * time (a value that the test case deemed appropriate) (when the
     * JwtTimeClaimExpectation validate method runs, it will ensure that the
     * time for the specified key in the token is within a "fudge" factor of the
     * value in the expectation - this will ensure that this time value is good
     * an be used to validate the other strings)
     * <LI>JwtApiExpectation's for time keys will validate their "value" against
     * the value of their key within the built token in the test response (which
     * we've validated via the JwtTimeClaimExpectation).
     * </OL>
     *
     * @param expectations
     *            - the expectations object to add additional expectations to -
     *            for the specified key
     * @param key
     *            - the key to build expectations for
     * @param value
     *            - the value of the key to validate
     * @return - updated expectations
     * @throws Exception
     */
    public static Expectations buildExpectationsForKey(Expectations expectations, String key, Object value) throws Exception {

        // we'll need to process certain claims differently - create the lists
        // of claims that need special handling
        List<String> timeClaims = Arrays.asList(PayloadConstants.EXPIRATION_TIME, PayloadConstants.ISSUED_AT, PayloadConstants.NOT_BEFORE);
        List<String> headerClaims = Arrays.asList(HeaderConstants.ALGORITHM, HeaderConstants.KEY_ID, HeaderConstants.ENCRYPTION, HeaderConstants.TYPE, HeaderConstants.CONTENT_TYPE);

        if (value instanceof JSONArray) {
            for (int i = 0; i < ((JSONArray) value).size(); i++) {
                Object theValue = ((JSONArray) value).get(i);
                if (key != null && timeClaims.contains(key)) {
                    if (!theValue.toString().contains("-1")) {
                        expectations.addExpectation(new JwtTimeClaimExpectation(null, null, Constants.TIME_TYPE, key, theValue.toString(), null));
                        expectations = updateClaimExpectationsForJsonAttribute(expectations, JWTBuilderConstants.JWT_CLAIM, key, "replaceWithRealTime");
                    } else {
                        expectations = updateClaimExpectationsForJsonAttribute(expectations, JWTBuilderConstants.JWT_CLAIM, key, theValue);
                    }
                } else {
                    // if the value of a member of an array is null we want to
                    // add "do not find" checks for that null value in the
                    // array, but, don't
                    // want to add a check for null in a specific api output
                    if (theValue == null) {
                        expectations = updateExpectationsForJsonAttribute(expectations, JWTBuilderConstants.JWT_CLAIM, key, theValue);
                    } else {
                        expectations = updateClaimExpectationsForJsonAttribute(expectations, JWTBuilderConstants.JWT_CLAIM, key, theValue);
                    }
                }
            }
        } else {
            // TODO - need to update all of the helper methods to handle
            // true/false values - not sure that the added complication
            // is worth it for the few test cases that use booleans. Those tests
            // can explicitly add the expectations that they need
            if (value instanceof Boolean) {
                return expectations;
            }
            if (key != null && timeClaims.contains(key) && value != null) {
                // if (value != null) {
                expectations.addExpectation(new JwtTimeClaimExpectation(null, null, Constants.TIME_TYPE, key, value.toString(), null));
                expectations = updateClaimExpectationsForJsonAttribute(expectations, JWTBuilderConstants.JWT_CLAIM, key, "replaceWithRealTime");
            } else {
                if (headerClaims.contains(key)) {
                    expectations = updateExpectationsForJsonHeaderAttribute(expectations, JWTBuilderConstants.JWT_TOKEN_HEADER, key, value);
                } else {
                    expectations = updateClaimExpectationsForJsonAttribute(expectations, JWTBuilderConstants.JWT_CLAIM, key, value);
                }
            }
        }

        return expectations;

    }

    /**
     * adds the expectations for the attribute in the json object as well as the
     * attribute/value obtained from the raw json (not from the attr specific
     * api)
     *
     * @param expectations
     *            - already set expectations that we'll add to
     * @param key
     *            - they key name to search for
     * @param value
     *            - the value to search for
     * @return - expecations object updated with a new expectation
     * @throws Exception
     */
    public static Expectations updateExpectationsForJsonAttribute(Expectations expectations, String prefix, String key, Object value) throws Exception {
        if (key != null && timeClaims.contains(key) && value != null) {
            expectations.addExpectation(new JwtTimeClaimExpectation(prefix, key, value, JwtApiExpectation.ValidationMsgType.CLAIM_FROM_LIST));
            expectations.addExpectation(new JwtTimeClaimExpectation(prefix, key, value, JwtApiExpectation.ValidationMsgType.CLAIM_LIST_MEMBER));
        } else {
            expectations.addExpectation(new JwtApiExpectation(prefix, key, value, JwtApiExpectation.ValidationMsgType.CLAIM_FROM_LIST));
            expectations.addExpectation(new JwtApiExpectation(prefix, key, value, JwtApiExpectation.ValidationMsgType.CLAIM_LIST_MEMBER));
        }
        return expectations;
    }

    public static Expectations updateExpectationsForJsonHeaderAttribute(Expectations expectations, String prefix, String key, Object value) throws Exception {

        expectations.addExpectation(new JwtApiExpectation(prefix, key, value, JwtApiExpectation.ValidationMsgType.HEADER_CLAIM_FROM_LIST));
        expectations.addExpectation(new JwtApiExpectation(prefix, key, value, JwtApiExpectation.ValidationMsgType.CLAIM_LIST_MEMBER));
        return expectations;

    }

    /**
     * adds the check for the attribute/value that was obtained by invoking the
     * <Claim.get<specificAttr>> method
     *
     * @param expectations
     *            - already set expectations that we'll add to
     * @param key
     *            - they key name to pass on so additional expectations can be
     *            added
     * @param value
     *            - the value to search for
     * @return - expecations object updated with a new expectation
     * @throws Exception
     */
    public static Expectations updateClaimExpectationsForJsonAttribute(Expectations expectations, String prefix, String key, Object value) throws Exception {

        if (key != null && timeClaims.contains(key)) {
            if (value != null) {
                expectations.addExpectation(new JwtTimeClaimExpectation(prefix, key, value, JwtApiExpectation.ValidationMsgType.SPECIFIC_CLAIM_API));
            } else {
                expectations.addExpectation(new JwtApiExpectation(prefix, key, "-1", JwtApiExpectation.ValidationMsgType.SPECIFIC_CLAIM_API));
            }
        } else {
            List<String> claimHasGetApi = Arrays.asList(PayloadConstants.ISSUER, PayloadConstants.SUBJECT, PayloadConstants.AUDIENCE, PayloadConstants.EXPIRATION_TIME, PayloadConstants.NOT_BEFORE, PayloadConstants.ISSUED_AT, PayloadConstants.JWT_ID, PayloadConstants.AUTHORIZED_PARTY);
            if (claimHasGetApi.contains(key)) {
                expectations.addExpectation(new JwtApiExpectation(prefix, key, value, JwtApiExpectation.ValidationMsgType.SPECIFIC_CLAIM_API));
            }
        }

        return updateExpectationsForJsonAttribute(expectations, prefix, key, value);
    }

    /**
     * Create expectations for reaching the builder and getting a specific error
     * message
     *
     * @param app
     *            - the app that we should reach
     * @param msgId
     *            - the error that we should receive in the response
     * @param server
     *            - the server that is running the app and issuing the error
     * @return - expectations updated with the new expectations
     * @throws Exception
     */
    public static Expectations createBadBuilderExpectations(String app, String msgId, LibertyServer server) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(SecurityFatHttpUtils.getServerUrlBase(server) + app));
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, msgId, "Response did not show the expected failure."));

        return expectations;
    }

    public static JSONObject setClaimsFromToken(String jwtTokenString) throws Exception {
        return setClaimsFromToken(jwtTokenString, null);
    }

    public static JSONObject setClaimsFromToken(String jwtTokenString, String privateKey) throws Exception {

        JwtTokenForTest jwtTokenForTest = new JwtTokenForTest(jwtTokenString, privateKey);
        return convertJsonToJSON(jwtTokenForTest.getJsonPayload());

    }

    public static JSONObject convertJsonToJSON(JsonObject jsonObject) throws Exception {

        String jsonString = jsonObject.toString();
        Log.info(thisClass, "convertJsonToJSON", "Json to String: " + jsonString);

        if (jsonString != null && !jsonString.isEmpty()) {
            return JSONObject.parse(jsonString);
        }
        return null;
    }

    public static String decodeFromBase64String(String encoded) {
        return new String(Base64.decodeBase64(encoded));
    }

    public static Expectations buildBuilderClientAppExpectations(String builderApp, String currentAction, LibertyServer builderServer) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, SecurityFatHttpUtils.getServerUrlBase(builderServer) + builderApp));

        return expectations;
    }

    public static Expectations buildBuilderClaimsNotFound(Expectations expectations, String prefix, String claim) throws Exception {
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_DOES_NOT_MATCH, prefix + JWTBuilderConstants.JWT_JSON + JWTBuilderConstants.JWT_GETALLCLAIMS + ".*" + claim + ".*", "Found unknown claim \"" + claim + "\" in the listed claims and it should not be there."));
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_DOES_NOT_MATCH, prefix + JWTBuilderConstants.JWT_JSON + "\\{" + ".*" + claim + ".*\\}", "Found unknown claim \"" + claim + "\" in the list of claims and it should not be there."));

        return expectations;
    }

    public static Expectations createGoodValidationEndpointExpectations(String jwtToken, String url) throws Exception {
        return createGoodValidationEndpointExpectations(jwtToken, url, null);
    }

    public static Expectations createGoodValidationEndpointExpectations(String jwtToken, String url, String privateKey) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(null, url));

        JsonObject jwtHeaderObject = getJsonOjbectPart(jwtToken, 0);

        JwtTokenForTest jwtTokenForTest = null;
        if (privateKey == null) {
            jwtTokenForTest = new JwtTokenForTest(jwtToken);
        } else {
            String keyMgmtKeyAlg = jwtHeaderObject.getString(HeaderConstants.ALGORITHM);
            String contentEncryptAlg = jwtHeaderObject.getString(HeaderConstants.ENCRYPTION);
            jwtTokenForTest = new JwtTokenForTest(jwtToken, keyMgmtKeyAlg, privateKey, contentEncryptAlg);
        }

        JsonObject jwsHeader = jwtTokenForTest.getJsonHeader();

        String builderKid = jwsHeader.getString(HeaderConstants.KEY_ID);
        String builderAlg = jwsHeader.getString(HeaderConstants.ALGORITHM);
        Log.info(thisClass, "createGoodValidationEndpointExpectations", "sigAlg: " + builderAlg);
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, "\"kid\":\"" + builderKid + "\".*", "The kid (" + builderKid + ") found in the built token does not match the kid returned by the JwkValidationEndpoint"));
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, "\"alg\":\"" + builderAlg + "\".*", "The algorithm (" + builderAlg + ") found in the built token does not match the algorithm returned by the JwkValidationEndpoint"));

        return expectations;
    }

    /**
     * Get the requested part from a JWT token string - convert it to a JSON
     * object
     *
     * @param jwt_token
     *            - the JWT token string to get part of
     * @param index
     *            - the portion of the token to return
     * @return - the correct portion as a json object
     * @throws Exception
     */
    public static JsonObject getJsonOjbectPart(String jwt_token, int index) throws Exception {

        if (jwt_token == null) {
            return null;
        }

        String[] jwt_token_parts;
        jwt_token_parts = jwt_token.split("\\.");
        if (jwt_token_parts == null) {
            throw new Exception("Failed splitting token");
        }

        String decodedString = decodeFromBase64String(jwt_token_parts[index]);
        JsonObject jsonInfo = Json.createReader(new StringReader(decodedString)).readObject();

        return jsonInfo;

    }

    public static String getDecodedPayload(String jwtToken) throws Exception {
        return getDecodedPayload(jwtToken, null);
    }

    public static String getDecodedPayload(String jwtToken, String privateKey) throws Exception {

        String payload = getPayload(jwtToken, privateKey);
        String decodedPayload = decodeFromBase64String(payload);
        return decodedPayload;
    }

    public static String getPayload(String jwtToken) throws Exception {
        return getPayload(jwtToken, null);
    }

    public static String getPayload(String jwtToken, String privateKey) throws Exception {

        JwtTokenForTest jwtTokenForTest = new JwtTokenForTest(jwtToken, privateKey);

        return jwtTokenForTest.getStringPayload();

    }

    /**
     * Extract and return the jwt token found in a response (Page)
     *
     * @param response
     *            - the web response (page) containing he jwt token
     * @param tokenString
     *            - they keywork denoting the jwt token in the response
     * @return - the jwt token string
     * @throws Exception
     */
    public static String extractJwtTokenFromResponse(Object response, String tokenString) throws Exception {
        String thisMethod = "extractJwtTokenFromResponse";

        try {
            String fullResponse = WebResponseUtils.getResponseText(response);
            String[] responseLines = fullResponse.split(System.getProperty("line.separator"));
            String jwtTokenString = null;
            for (String line : responseLines) {
                if (line.contains(tokenString)) {
                    jwtTokenString = line.trim().substring(line.indexOf(tokenString) + tokenString.length());
                    // Log.info(thisClass, thisMethod, "Found: " + tokenString +
                    // " set to: " + jwtTokenString);
                }
            }
            return jwtTokenString;
        } catch (Exception e) {
            Log.error(thisClass, thisMethod, e);
            throw new Exception("Failed to extract the JWT Token from the provided response: " + e);
        }
    }

    /**
     * Extract and return just the payload portion of a jwt token found in a
     * Response (Page)
     *
     * @param response
     *            - the web response (page) containing the jwt token
     * @return - a JsonObject containing the payload claims
     * @throws Exception
     */
    public static JsonObject extractJwtPayload(Object response) throws Exception {
        return getJsonOjbectPart(extractJwtTokenFromResponse(response, JWTBuilderConstants.BUILT_JWT_TOKEN), TokenPayload);
    }

    /**
     * Extract and return just the header portion of a jwt token found in a
     * Response (Page)
     *
     * @param response
     *            - the web response (page) containing the jwt token
     * @return - a JsonObject containing the header claims
     * @throws Exception
     */
    public static JsonObject extractJwtHeader(Object response) throws Exception {
        return getJsonOjbectPart(extractJwtTokenFromResponse(response, JWTBuilderConstants.BUILT_JWT_TOKEN), TokenHeader);
    }

}
