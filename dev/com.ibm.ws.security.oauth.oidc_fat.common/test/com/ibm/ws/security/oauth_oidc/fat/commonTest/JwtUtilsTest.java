/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue.ValueType;

import org.jmock.Expectations;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class JwtUtilsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.oauth_oidc.fat.*=all");

    TestSettings settings = mockery.mock(TestSettings.class);

    private static final String KEY_ALG = "alg";
    private static final String KEY_KID = "kid";
    private static final String KEY_ISSUER = "iss";
    private static final String KEY_IAT = "iat";
    private static final String KEY_EXP = "exp";
    private static final String KEY_SUB = "sub";
    private static final String KEY_UNIQUE_SECURITY_NAME = "uniqueSecurityName";
    private static final String KEY_REALM_NAME = "realmName";
    private static final String KEY_AT_HASH = "at_hash";
    private static final String ALG_HS256 = "HS256";
    private static final String DEFAULT_KEY_ID = "autokeyid";

    private final static String JWT_REGEX = "[^.]+\\.[^.]+\\.[^.]+";

    private final String issuer = "http://my.issuer.com/with/path";
    private final String clientSecret = "my client secret";
    private final String adminUser = "myAdminUser";
    private final String realm = "A Realm";

    JwtUtils utils = new JwtUtils();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
        utils = new JwtUtils();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /************************************** buildOldJwtString **************************************/

    /**
     * Tests:
     * - Null issuer in test settings
     * Expects:
     * - Non-null, validly formatted JWT string should be created
     * - JWT should NOT include issuer claim
     */
    @Test
    public void test_buildOldJwtString_nullIssuer() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(settings).getIssuer();
                    will(returnValue(null));
                    one(settings).getClientSecret();
                    will(returnValue(clientSecret));
                    allowing(settings).getAdminUser();
                    will(returnValue(adminUser));
                    one(settings).getRealm();
                    will(returnValue(realm));
                }
            });
            String jwtString = utils.buildOldJwtString(settings);
            assertNotNull("Result should not have been null but was.", jwtString);
            assertTrue("Result did not match expected JWT regex (" + JWT_REGEX + "). Result was [" + jwtString + "].", Pattern.matches(JWT_REGEX, jwtString));

            String[] jwtParts = jwtString.split("\\.");

            verifyJwtHeader(jwtParts[0]);

            // JWT payload should not include issuer claim
            Map<String, Object> expectedPayloadEntries = getDefaultPayloadEntries(issuer, adminUser, adminUser, realm);
            expectedPayloadEntries.remove(KEY_ISSUER);
            verifyJwtPayload(jwtParts[1], expectedPayloadEntries);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Null client secret in test settings
     * Expects:
     * - Exception should be thrown saying the test settings didn't include a client secret
     */
    @Test
    public void test_buildOldJwtString_nullClientSecret() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(settings).getIssuer();
                    will(returnValue(issuer));
                    one(settings).getClientSecret();
                    will(returnValue(null));
                }
            });
            try {
                String jwtString = utils.buildOldJwtString(settings);
                fail("Should have thrown an exception, but got JWT string: [" + jwtString + "].");
            } catch (Exception e) {
                verifyException(e, "JWT cannot be created.+test settings do not include a client secret");
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Null admin user in test settings
     * Expects:
     * - Non-null, validly formatted JWT string should be created
     * - JWT should NOT include sub or uniqueSecurityName claims
     */
    @Test
    public void test_buildOldJwtString_nullAdminUser() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(settings).getIssuer();
                    will(returnValue(issuer));
                    one(settings).getClientSecret();
                    will(returnValue(clientSecret));
                    allowing(settings).getAdminUser();
                    will(returnValue(null));
                    one(settings).getRealm();
                    will(returnValue(realm));
                }
            });
            String jwtString = utils.buildOldJwtString(settings);
            assertNotNull("Result should not have been null but was.", jwtString);
            assertTrue("Result did not match expected JWT regex (" + JWT_REGEX + "). Result was [" + jwtString + "].", Pattern.matches(JWT_REGEX, jwtString));

            String[] jwtParts = jwtString.split("\\.");

            verifyJwtHeader(jwtParts[0]);

            // JWT payload should not include sub or uniqueSecurityName claims
            Map<String, Object> expectedPayloadEntries = getDefaultPayloadEntries(issuer, adminUser, adminUser, realm);
            expectedPayloadEntries.remove(KEY_SUB);
            expectedPayloadEntries.remove(KEY_UNIQUE_SECURITY_NAME);
            verifyJwtPayload(jwtParts[1], expectedPayloadEntries);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Null realm in test settings
     * Expects:
     * - Non-null, validly formatted JWT string should be created
     * - JWT should NOT include realm claim
     */
    @Test
    public void test_buildOldJwtString_nullRealm() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(settings).getIssuer();
                    will(returnValue(issuer));
                    one(settings).getClientSecret();
                    will(returnValue(clientSecret));
                    allowing(settings).getAdminUser();
                    will(returnValue(adminUser));
                    one(settings).getRealm();
                    will(returnValue(null));
                }
            });
            String jwtString = utils.buildOldJwtString(settings);
            assertNotNull("Result should not have been null but was.", jwtString);
            assertTrue("Result did not match expected JWT regex (" + JWT_REGEX + "). Result was [" + jwtString + "].", Pattern.matches(JWT_REGEX, jwtString));

            String[] jwtParts = jwtString.split("\\.");

            verifyJwtHeader(jwtParts[0]);

            // JWT payload should not include sub or uniqueSecurityName claims
            Map<String, Object> expectedPayloadEntries = getDefaultPayloadEntries(issuer, adminUser, adminUser, realm);
            expectedPayloadEntries.remove(KEY_REALM_NAME);
            verifyJwtPayload(jwtParts[1], expectedPayloadEntries);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Tests:
     * - Non-null, standard values used for token
     * Expects:
     * - Non-null, validly formatted JWT string should be created
     */
    @Test
    public void test_buildOldJwtString_nonNullValidValues() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(settings).getIssuer();
                    will(returnValue(issuer));
                    one(settings).getClientSecret();
                    will(returnValue(clientSecret));
                    allowing(settings).getAdminUser();
                    will(returnValue(adminUser));
                    one(settings).getRealm();
                    will(returnValue(realm));
                }
            });
            String jwtString = utils.buildOldJwtString(settings);
            assertNotNull("Result should not have been null but was.", jwtString);
            assertTrue("Result did not match expected JWT regex (" + JWT_REGEX + "). Result was [" + jwtString + "].", Pattern.matches(JWT_REGEX, jwtString));

            String[] jwtParts = jwtString.split("\\.");

            verifyJwtHeader(jwtParts[0]);

            Map<String, Object> expectedPayloadEntries = getDefaultPayloadEntries(issuer, adminUser, adminUser, realm);
            verifyJwtPayload(jwtParts[1], expectedPayloadEntries);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /********************************************** Helper methods **********************************************/

    private void verifyJwtHeader(String headerString) {
        JsonObject headerJson = convertBase64StringToJson(headerString);
        Map<String, Object> expectedHeaderEntries = getDefaultHeaderEntries();
        assertJsonContainsExpectedValues(expectedHeaderEntries, headerJson);
        assertJsonContainsOnlyExpectedEntries(expectedHeaderEntries, headerJson);
    }

    private void verifyJwtPayload(String payloadString, Map<String, Object> expectedPayloadEntries) {
        JsonObject payloadJson = convertBase64StringToJson(payloadString);
        assertJsonContainsExpectedValues(expectedPayloadEntries, payloadJson);
        assertJsonContainsOnlyExpectedEntries(expectedPayloadEntries, payloadJson);
    }

    private JsonObject convertBase64StringToJson(String base64EncodedJsonString) {
        try {
            String newEncodedString = padBase64EncodedString(base64EncodedJsonString);
            String decodedJsonString = new String(Base64Coder.base64DecodeString(newEncodedString), "UTF-8");
            JsonReader reader = Json.createReader(new StringReader(decodedJsonString));
            return reader.readObject();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            fail("Caught unexpected exception converting string to JSON: " + e);
        }
        return null;
    }

    /**
     * The decoding methods in Base64Coder can't handle strings whose byte array representation has a length that isn't evenly
     * divisible by 4. This method pads the string with the "=" character to ensure decoding proceeds successfully.
     */
    private String padBase64EncodedString(String base64EncodedJsonString) throws UnsupportedEncodingException {
        while (base64EncodedJsonString.getBytes("UTF-8").length % 4 != 0) {
            base64EncodedJsonString += "=";
        }
        return base64EncodedJsonString;
    }

    private Map<String, Object> getDefaultHeaderEntries() {
        Map<String, Object> defaultHeaderEntries = new HashMap<String, Object>();
        defaultHeaderEntries.put(KEY_ALG, ALG_HS256);
        defaultHeaderEntries.put(KEY_KID, DEFAULT_KEY_ID);
        return defaultHeaderEntries;
    }

    private Map<String, Object> getDefaultPayloadEntries(String issuer, String sub, String uniqueSecName, String realm) {
        Map<String, Object> defaultPayloadEntries = new HashMap<String, Object>();
        defaultPayloadEntries.put(KEY_ISSUER, issuer);
        defaultPayloadEntries.put(KEY_IAT, JwtUtils.OLD_IAT);
        defaultPayloadEntries.put(KEY_EXP, JwtUtils.OLD_EXP);
        defaultPayloadEntries.put(KEY_SUB, sub);
        defaultPayloadEntries.put(KEY_UNIQUE_SECURITY_NAME, uniqueSecName);
        defaultPayloadEntries.put(KEY_REALM_NAME, realm);
        defaultPayloadEntries.put(KEY_AT_HASH, JwtUtils.DEFAULT_AT_HASH);
        return defaultPayloadEntries;
    }

    private void assertJsonContainsExpectedValues(Map<String, Object> expectedEntryValues, JsonObject json) {
        for (Entry<String, Object> entry : expectedEntryValues.entrySet()) {
            String expectedKey = entry.getKey();
            assertTrue("JSON object is missing expected key \"" + expectedKey + "\". Keys were: " + json.keySet(), json.containsKey(expectedKey));
            Object value = getJsonValueAsPojo(json, expectedKey);
            assertEquals("Value for key \"" + expectedKey + "\" did not equal expected value.", entry.getValue(), value);
        }
    }

    private Object getJsonValueAsPojo(JsonObject json, String key) {
        Object value = null;
        ValueType valueType = json.get(key).getValueType();
        if (ValueType.STRING == valueType) {
            value = json.getString(key);
        } else if (ValueType.NUMBER == valueType) {
            value = json.getJsonNumber(key).longValueExact();
        } else if (ValueType.TRUE == valueType || ValueType.FALSE == valueType) {
            value = json.getBoolean(key);
        } else if (ValueType.ARRAY == valueType) {
            value = json.getJsonArray(key);
        } else if (ValueType.OBJECT == valueType) {
            value = json.getJsonObject(key);
        }
        return value;
    }

    private void assertJsonContainsOnlyExpectedEntries(Map<String, Object> expectedEntryValues, JsonObject json) {
        Set<String> headerKeys = new HashSet<String>(json.keySet());
        headerKeys.removeAll(expectedEntryValues.keySet());
        assertEquals("Unexpected number of entries in JSON object. Extra entries were: " + headerKeys, expectedEntryValues.size(), json.keySet().size());
    }

}
