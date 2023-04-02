/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.test.common.CommonTestClass;

public class ClaimsImplTest extends CommonTestClass {

    private ClaimsImpl claims;

    @Before
    public void setUp() throws Exception {
        claims = new ClaimsImpl();
    }

    @Test
    public void test_put_nullKey() {
        String key = null;
        String value = "Jan 1, 2001";

        Object putResult = claims.put(key, value);

        assertNull("Put result should have been null because the key didn't exist before, but instead got [" + putResult + "].", putResult);
        assertTrue("Should have returned true for a null claim name when one is in the claims set.", claims.containsKey(key));
        String retrievedValue = claims.getClaim(key, String.class);
        assertEquals("Retrieved claim value did not match original value.", value, retrievedValue);
    }

    @Test
    public void test_put_nullKey_keyAlreadyExists() {
        String key = null;
        String value = "Jan 1, 2001";
        String value2 = "Jan 1, 1";

        Object putResult = claims.put(key, value);
        assertNull("Put result should have been null because the key didn't exist before, but instead got [" + putResult + "].", putResult);

        putResult = claims.put(key, value2);
        assertEquals("Put result did not match the previous key's value.", value, putResult);

        assertTrue("Should have returned true for a null claim name when one is in the claims set.", claims.containsKey(key));
        String retrievedValue = claims.getClaim(key, String.class);
        assertEquals("Retrieved claim value did not match the new value.", value2, retrievedValue);
    }

    @Test
    public void test_put_emptyKey() {
        String key = "";
        String value = "test value";

        Object putResult = claims.put(key, value);

        assertNull("Put result should have been null because the key didn't exist before, but instead got [" + putResult + "].", putResult);
        assertTrue("Should have returned true for an empty string claim name when one is in the claims set.", claims.containsKey(key));
        String retrievedValue = claims.getClaim(key, String.class);
        assertEquals("Retrieved claim value did not match original value.", value, retrievedValue);
    }

    @Test
    public void test_put_nullValue() {
        String key = "my claim";
        Object value = null;

        Object putResult = claims.put(key, value);

        assertNull("Put result should have been null because the key didn't exist before, but instead got [" + putResult + "].", putResult);
        assertTrue("Should have returned true for a claim whose value is null.", claims.containsKey(key));
        Object retrievedValue = claims.getClaim(key, Object.class);
        assertEquals("Retrieved claim value did not match original value.", value, retrievedValue);
    }

    @Test
    public void test_put_nullValue_keyAlreadyExists() {
        String key = "my claim";
        String value = "I'm about to get wiped out";
        Object value2 = null;

        Object putResult = claims.put(key, value);
        assertNull("Put result should have been null because the key didn't exist before, but instead got [" + putResult + "].", putResult);

        putResult = claims.put(key, value2);
        assertEquals("Put result did not match the previous key's value.", value, putResult);

        assertTrue("Should have returned true for a null claim name when one is in the claims set.", claims.containsKey(key));
        Object retrievedValue = claims.getClaim(key, Object.class);
        assertEquals("Retrieved claim value did not match the new value.", value2, retrievedValue);
    }

    @Test
    public void test_put_emptyValue() {
        String key = "my claim";
        String value = "";

        Object putResult = claims.put(key, value);

        assertNull("Put result should have been null because the key didn't exist before, but instead got [" + putResult + "].", putResult);
        assertTrue("Should have returned true for a claim whose value is the empty string.", claims.containsKey(key));
        String retrievedValue = claims.getClaim(key, String.class);
        assertEquals("Retrieved claim value did not match original value.", value, retrievedValue);
    }

    @Test
    public void test_putAll_emptyClaims_nullMap() {
        claims.putAll(null);
        assertTrue("Claims set should have been empty but was: " + claims.toJsonString(), claims.isEmpty());
    }

    @Test
    public void test_putAll_emptyClaims_emptyMap() {
        Map<String, Object> map = new HashMap<>();
        claims.putAll(map);
        assertTrue("Claims set should have been empty but was: " + claims.toJsonString(), claims.isEmpty());
    }

    @Test
    public void test_putAll_emptyClaims_putMultipleValues() {
        Map<String, Object> map = new HashMap<>();
        Object claim1Value = null;
        Object claim2Value = "some string";
        Object claim3Value = 42;
        map.put("claim1", claim1Value);
        map.put("claim2", claim2Value);
        map.put("claim3", claim3Value);

        claims.putAll(map);

        assertFalse("Claims set should not have been empty but was.", claims.isEmpty());
        assertTrue("Claims set is missing entry for claim1. Claims were: " + claims.toJsonString(), claims.containsKey("claim1"));
        assertEquals("Value for claim1 did not match expected value.", claim1Value, claims.get("claim1"));
        assertTrue("Claims set is missing entry for claim2. Claims were: " + claims.toJsonString(), claims.containsKey("claim2"));
        assertEquals("Value for claim2 did not match expected value.", claim2Value, claims.get("claim2"));
        assertTrue("Claims set is missing entry for claim3. Claims were: " + claims.toJsonString(), claims.containsKey("claim3"));
        assertEquals("Value for claim3 did not match expected value.", claim3Value, claims.get("claim3"));
    }

    @Test
    public void test_putAll_nonEmptyClaims_putMultipleValues() {
        Object claim1Value = null;
        Object claim2Value = "some string";
        claims.put("claim1", claim1Value);
        claims.put("claim2", claim2Value);
        claims.put("claim3", 978307200);
        claims.put("claim4", this);

        Object claim3Value = null;
        Object claim4Value = "Switching value classes";
        Object claim5Value = "{}";
        Object claim6Value = false;
        Map<String, Object> map = new HashMap<>();
        map.put("claim3", claim3Value);
        map.put("claim4", claim4Value);
        map.put("claim5", claim5Value);
        map.put("claim6", claim6Value);

        claims.putAll(map);
        assertFalse("Claims set should not have been empty but was.", claims.isEmpty());
        assertTrue("Claims set is missing entry for claim1. Claims were: " + claims.toJsonString(), claims.containsKey("claim1"));
        assertEquals("Value for claim1 did not match expected value.", claim1Value, claims.get("claim1"));
        assertTrue("Claims set is missing entry for claim2. Claims were: " + claims.toJsonString(), claims.containsKey("claim2"));
        assertEquals("Value for claim2 did not match expected value.", claim2Value, claims.get("claim2"));
        assertTrue("Claims set is missing entry for claim3. Claims were: " + claims.toJsonString(), claims.containsKey("claim3"));
        assertEquals("Value for claim3 did not match expected value.", claim3Value, claims.get("claim3"));
        assertTrue("Claims set is missing entry for claim3. Claims were: " + claims.toJsonString(), claims.containsKey("claim4"));
        assertEquals("Value for claim4 did not match expected value.", claim4Value, claims.get("claim4"));
        assertTrue("Claims set is missing entry for claim3. Claims were: " + claims.toJsonString(), claims.containsKey("claim5"));
        assertEquals("Value for claim5 did not match expected value.", claim5Value, claims.get("claim5"));
        assertTrue("Claims set is missing entry for claim3. Claims were: " + claims.toJsonString(), claims.containsKey("claim6"));
        assertEquals("Value for claim6 did not match expected value.", claim6Value, claims.get("claim6"));
    }

    @Test
    public void test_getClaim_noClaims() {
        String claimName = "my claim";
        String value = claims.getClaim(claimName, String.class);
        assertNull("Result should have been null but was [" + value + "].", value);
    }

    @Test
    public void test_getClaim_claimExists_sameType() {
        String claimName = "my claim";
        boolean value = true;
        claims.put(claimName, value);
        Boolean retrievedValue = claims.getClaim(claimName, Boolean.class);
        assertEquals("Retrieved value did not match value put in.", value, retrievedValue.booleanValue());
    }

    @Test
    public void test_getClaim_claimExists_wrongType() {
        String claimName = "my claim";
        Object value = this;
        claims.put(claimName, value);
        try {
            Integer retrievedValue = claims.getClaim(claimName, Integer.class);
            fail("Should have thrown a ClassCastException, but instead retrieved a value: [" + retrievedValue + "].");
        } catch (ClassCastException e) {
            // Expected
        }
    }

    @Test
    public void test_containsKey_noClaims() {
        String claimName = "my claim";
        assertFalse("Should not have returned true for a claim that is not present.", claims.containsKey(claimName));
    }

    @Test
    public void test_containsKey_claimMissing() {
        claims.put("claim1", "value");
        claims.put("claim2", 123);
        String claimName = "my claim";
        assertFalse("Should have returned false for a claim that is not present.", claims.containsKey(claimName));
    }

    @Test
    public void test_containsKey_claimValueNull() {
        String claimName = "my claim";
        claims.put(claimName, null);
        assertTrue("Should have returned true for a claim whose value is null.", claims.containsKey(claimName));
    }

    @Test
    public void test_containsKey_claimValueEmptyString() {
        String claimName = "my claim";
        claims.put(claimName, "");
        assertTrue("Should have returned true for a claim whose value is the empty string.", claims.containsKey(claimName));
    }

    @Test
    public void test_containsKey_claimValueNonNull() {
        String claimName = "my claim";
        claims.put(claimName, 123);
        assertTrue("Should have returned true for a claim that has a value in the claims set.", claims.containsKey(claimName));
    }

    @Test
    public void test_toJsonString_noClaims() {
        String json = claims.toJsonString();
        assertEquals("Did not get the empty JSON object as expected.", "{}", json);
    }

    @Test
    public void test_toJsonString_mixOfClaims() {
        claims.put("claim1", null);
        claims.put("claim2", "If you build it, he will come");
        claims.put("claim3", 978307200);
        claims.put("claim4", new JSONObject());
        claims.put("claim5", false);

        String json = claims.toJsonString();
        String claim1Expectation = "\"claim1\":null";
        assertTrue("JSON string did not contain expected string \"" + claim1Expectation + "\" for claim1. JSON string was: " + json, json.contains(claim1Expectation));
        String claim2Expectation = "\"claim2\":\"If you build it, he will come\"";
        assertTrue("JSON string did not contain expected string \"" + claim2Expectation + "\" for claim2. JSON string was: " + json, json.contains(claim2Expectation));
        String claim3Expectation = "\"claim3\":978307200";
        assertTrue("JSON string did not contain expected string \"" + claim3Expectation + "\" for claim3. JSON string was: " + json, json.contains(claim3Expectation));
        String claim4Expectation = "\"claim4\":{}";
        assertTrue("JSON string did not contain expected string \"" + claim4Expectation + "\" for claim4. JSON string was: " + json, json.contains(claim4Expectation));
        String claim5Expectation = "\"claim5\":false";
        assertTrue("JSON string did not contain expected string \"" + claim5Expectation + "\" for claim5. JSON string was: " + json, json.contains(claim5Expectation));
    }

}
