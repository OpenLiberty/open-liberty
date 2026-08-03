/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.json.java;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.internal.ParserConfig;

/**
 * Unit tests for JSON parser security limits using custom system properties.
 * Tests that custom property values are correctly applied and enforced.
 * 
 * <p>This test class sets custom system properties before ParserConfig is loaded
 * and verifies that the parser enforces those custom limits. It runs in isolation
 * from {@link SecurityLimitsDefaultTest} to ensure that system property changes
 * do not affect other tests.</p>
 * 
 * <p>Note: This test class must run in a separate JVM from other security limit
 * tests because ParserConfig reads system properties during static initialization.</p>
 */
public class SecurityLimitsCustomPropertiesTest {

    /**
     * Set custom system properties before ParserConfig is loaded.
     * These properties will be read during ParserConfig's static initialization.
     */
    @BeforeClass
    public static void setCustomProperties() {
        // Set custom limits that are smaller than defaults for easier testing
        System.setProperty("com.ibm.json4j.max.array.size", "100");
        System.setProperty("com.ibm.json4j.max.object.members", "50");
        System.setProperty("com.ibm.json4j.max.string.length", "1000");
        System.setProperty("com.ibm.json4j.max.number.length", "50");
        System.setProperty("com.ibm.json4j.max.nesting.depth", "10");
        System.setProperty("com.ibm.json4j.max.total.size", "10000");
        System.setProperty("com.ibm.json4j.duplicate.key.behavior", "ERROR");
    }

    // ========================================================================
    // Custom Array Size Limit Tests
    // ========================================================================

    /**
     * Test that custom array size limit is enforced.
     */
    @Test
    public void testCustomArraySizeLimit() {
        int customLimit = 100;
        assertEquals("Custom array size should be applied", customLimit, ParserConfig.getMaxArraySize());
        
        // Build JSON with customLimit + 1 elements
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i <= customLimit; i++) {
            if (i > 0) json.append(",");
            json.append("1");
        }
        json.append("]");
        
        // Should throw IOException
        try {
            JSON.parse(new StringReader(json.toString()));
            fail("Should have thrown IOException for array size exceeding custom limit");
        } catch (IOException e) {
            // Expected
            assertTrue("Error message should mention array or size", 
                       e.getMessage().toLowerCase().contains("array") || 
                       e.getMessage().toLowerCase().contains("size"));
        }
    }

    /**
     * Test that arrays at the custom limit are accepted.
     */
    @Test
    public void testCustomArraySizeAtBoundary() throws Exception {
        int customLimit = 100;
        
        // Build JSON with exactly customLimit elements
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < customLimit; i++) {
            if (i > 0) json.append(",");
            json.append("1");
        }
        json.append("]");
        
        // Should succeed
        JSONArray result = (JSONArray) JSON.parse(new StringReader(json.toString()));
        assertEquals("Array should have exactly customLimit elements", customLimit, result.size());
    }

    // ========================================================================
    // Custom Object Member Count Limit Tests
    // ========================================================================

    /**
     * Test that custom object member limit is enforced.
     */
    @Test
    public void testCustomObjectMembersLimit() {
        int customLimit = 50;
        assertEquals("Custom object members should be applied", customLimit, ParserConfig.getMaxObjectMembers());
        
        // Build JSON with customLimit + 1 members
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i <= customLimit; i++) {
            if (i > 0) json.append(",");
            json.append("\"key").append(i).append("\":").append(i);
        }
        json.append("}");
        
        // Should throw IOException
        try {
            JSON.parse(new StringReader(json.toString()));
            fail("Should have thrown IOException for object members exceeding custom limit");
        } catch (IOException e) {
            // Expected
            assertTrue("Error message should mention object or member", 
                       e.getMessage().toLowerCase().contains("object") || 
                       e.getMessage().toLowerCase().contains("member"));
        }
    }

    /**
     * Test that objects at the custom limit are accepted.
     */
    @Test
    public void testCustomObjectMembersAtBoundary() throws Exception {
        int customLimit = 50;
        
        // Build JSON with exactly customLimit members
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < customLimit; i++) {
            if (i > 0) json.append(",");
            json.append("\"key").append(i).append("\":").append(i);
        }
        json.append("}");
        
        // Should succeed
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json.toString()));
        assertEquals("Object should have exactly customLimit members", customLimit, result.size());
    }

    // ========================================================================
    // Custom String Length Limit Tests
    // ========================================================================

    /**
     * Test that custom string length limit is enforced.
     */
    @Test
    public void testCustomStringLengthLimit() {
        int customLimit = 1000;
        assertEquals("Custom string length should be applied", customLimit, ParserConfig.getMaxStringLength());
        
        // Build JSON with a string of customLimit + 1 characters
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i <= customLimit; i++) {
            longString.append("a");
        }
        String json = "{\"key\":\"" + longString.toString() + "\"}";
        
        // Should throw IOException
        try {
            JSON.parse(new StringReader(json));
            fail("Should have thrown IOException for string length exceeding custom limit");
        } catch (IOException e) {
            // Expected
            assertTrue("Error message should mention string or length", 
                       e.getMessage().toLowerCase().contains("string") || 
                       e.getMessage().toLowerCase().contains("length"));
        }
    }

    /**
     * Test that strings at the custom limit are accepted.
     */
    @Test
    public void testCustomStringLengthAtBoundary() throws Exception {
        int customLimit = 1000;
        
        // Build JSON with a string of exactly customLimit characters
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < customLimit; i++) {
            longString.append("a");
        }
        String json = "{\"key\":\"" + longString.toString() + "\"}";
        
        // Should succeed
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        String value = (String) result.get("key");
        assertEquals("String should have exactly customLimit characters", customLimit, value.length());
    }

    // ========================================================================
    // Custom Number Length Limit Tests
    // ========================================================================

    /**
     * Test that custom number length limit is enforced.
     */
    @Test
    public void testCustomNumberLengthLimit() {
        int customLimit = 50;
        assertEquals("Custom number length should be applied", customLimit, ParserConfig.getMaxNumberLength());
        
        // Build JSON with a number string of customLimit + 1 characters
        StringBuilder longNumber = new StringBuilder();
        for (int i = 0; i <= customLimit; i++) {
            longNumber.append("9");
        }
        String json = "{\"value\":" + longNumber.toString() + "}";
        
        // Should throw IOException
        try {
            JSON.parse(new StringReader(json));
            fail("Should have thrown IOException for number length exceeding custom limit");
        } catch (IOException e) {
            // Expected
            assertTrue("Error message should mention number or length", 
                       e.getMessage().toLowerCase().contains("number") || 
                       e.getMessage().toLowerCase().contains("length"));
        }
    }

    // ========================================================================
    // Custom Nesting Depth Limit Tests
    // ========================================================================

    /**
     * Test that custom nesting depth limit is enforced.
     */
    @Test
    public void testCustomNestingDepthLimit() {
        int customLimit = 10;
        assertEquals("Custom nesting depth should be applied", customLimit, ParserConfig.getMaxNestingDepth());
        
        // Build JSON with customLimit + 2 levels of nesting
        StringBuilder json = new StringBuilder();
        for (int i = 0; i < customLimit + 2; i++) {
            json.append("{\"nested\":");
        }
        json.append("\"value\"");
        for (int i = 0; i < customLimit + 2; i++) {
            json.append("}");
        }
        
        // Should throw IOException
        try {
            JSON.parse(new StringReader(json.toString()));
            fail("Should have thrown IOException for nesting depth exceeding custom limit");
        } catch (IOException e) {
            // Expected
            assertTrue("Error message should mention nesting or depth",
                       e.getMessage().toLowerCase().contains("nest") || 
                       e.getMessage().toLowerCase().contains("depth"));
        }
    }

    /**
     * Test that nesting at the custom limit is accepted.
     */
    @Test
    public void testCustomNestingDepthAtBoundary() throws Exception {
        int customLimit = 10;
        
        // Build JSON with exactly customLimit levels of nesting
        StringBuilder json = new StringBuilder();
        for (int i = 0; i < customLimit; i++) {
            json.append("{\"nested\":");
        }
        json.append("\"value\"");
        for (int i = 0; i < customLimit; i++) {
            json.append("}");
        }
        
        // Should succeed
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json.toString()));
        assertNotNull("Deeply nested object should parse", result);
    }

    // ========================================================================
    // Custom Duplicate Key Behavior Tests
    // ========================================================================

    /**
     * Test that custom duplicate key behavior (ERROR mode) is enforced.
     */
    @Test
    public void testCustomDuplicateKeyBehaviorError() {
        assertEquals("Custom duplicate key behavior should be ERROR", 
                     ParserConfig.DuplicateKeyBehavior.ERROR, 
                     ParserConfig.getDuplicateKeyBehavior());
        
        String json = "{\"key\":\"first\",\"key\":\"second\"}";
        
        // Should throw IOException in ERROR mode
        try {
            JSON.parse(new StringReader(json));
            fail("Should have thrown IOException for duplicate key in ERROR mode");
        } catch (IOException e) {
            // Expected
            assertTrue("Error message should mention duplicate", 
                       e.getMessage().toLowerCase().contains("duplicate"));
        }
    }

    // ========================================================================
    // Custom Total Size Limit Tests
    // ========================================================================

    /**
     * Test that custom total size limit is enforced.
     */
    @Test
    public void testCustomTotalSizeLimit() {
        long customLimit = 10000;
        assertEquals("Custom total size should be applied", customLimit, ParserConfig.getMaxTotalSize());
        
        // Create an array with many strings that collectively exceed the custom total size limit
        int stringLength = 100;
        int bytesPerString = (int)(stringLength * 2L + 24 + 8);
        int numStrings = (int) (customLimit / bytesPerString) + 10;
        
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < numStrings; i++) {
            if (i > 0) json.append(",");
            json.append("\"");
            for (int j = 0; j < stringLength; j++) {
                json.append("x");
            }
            json.append("\"");
        }
        json.append("]");
        
        // Should throw IOException for exceeding total size
        try {
            JSON.parse(new StringReader(json.toString()));
            fail("Should have thrown IOException for total size exceeding custom limit");
        } catch (IOException e) {
            // Expected
            assertTrue("Error message should mention total or size",
                       e.getMessage().toLowerCase().contains("total") || 
                       e.getMessage().toLowerCase().contains("size"));
        }
    }

    // ========================================================================
    // Verification Tests
    // ========================================================================

    /**
     * Verify that all custom properties were correctly applied.
     */
    @Test
    public void testAllCustomPropertiesApplied() {
        assertEquals("Custom array size should be 100", 100, ParserConfig.getMaxArraySize());
        assertEquals("Custom object members should be 50", 50, ParserConfig.getMaxObjectMembers());
        assertEquals("Custom string length should be 1000", 1000, ParserConfig.getMaxStringLength());
        assertEquals("Custom number length should be 50", 50, ParserConfig.getMaxNumberLength());
        assertEquals("Custom nesting depth should be 10", 10, ParserConfig.getMaxNestingDepth());
        assertEquals("Custom total size should be 10000", 10000L, ParserConfig.getMaxTotalSize());
        assertEquals("Custom duplicate key behavior should be ERROR", 
                     ParserConfig.DuplicateKeyBehavior.ERROR, 
                     ParserConfig.getDuplicateKeyBehavior());
    }
}

// Made with Bob
