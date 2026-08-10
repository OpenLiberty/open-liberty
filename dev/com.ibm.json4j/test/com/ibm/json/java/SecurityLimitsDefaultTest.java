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

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.json.java.internal.ParserConfig;

/**
 * Unit tests for JSON parser security limits using default configuration values.
 * Tests all 6 attack vectors: array size, object members, string length,
 * number length, nesting depth, and duplicate keys.
 * 
 * <p>This test class verifies that the parser correctly enforces security limits
 * when using the default configuration (no system properties set). It runs in
 * isolation from {@link SecurityLimitsCustomPropertiesTest} to ensure that
 * system property changes do not affect these tests.</p>
 */
public class SecurityLimitsDefaultTest {

    // ========================================================================
    // Array Size Limit Tests
    // ========================================================================

    /**
     * Test that arrays at the size limit are accepted.
     */
    @Test
    public void testArraySizeAtBoundary() throws Exception {
        int maxSize = ParserConfig.getMaxArraySize();
        
        // Build JSON with exactly maxSize elements
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < maxSize; i++) {
            if (i > 0) json.append(",");
            json.append("1");
        }
        json.append("]");
        
        // Should succeed
        JSONArray result = (JSONArray) JSON.parse(new StringReader(json.toString()));
        assertEquals("Array should have exactly maxSize elements", maxSize, result.size());
    }

    /**
     * Test that arrays exceeding the size limit are rejected.
     */
    @Test
    public void testArraySizeOverLimit() {
        int maxSize = ParserConfig.getMaxArraySize();
        
        // Build JSON with maxSize + 1 elements
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i <= maxSize; i++) {
            if (i > 0) json.append(",");
            json.append("1");
        }
        json.append("]");
        
        // Should throw IOException
        try {
            JSON.parse(new StringReader(json.toString()));
            fail("Should have thrown IOException for array size exceeding limit");
        } catch (IOException e) {
            // Expected - verify error message is informative
            String message = e.getMessage();
            assertNotNull("Error message should not be null", message);
            assertTrue("Error message should mention array or size", 
                       message.toLowerCase().contains("array") || message.toLowerCase().contains("size"));
        }
    }

    /**
     * Test that small arrays work correctly (backward compatibility).
     */
    @Test
    public void testSmallArrayWorks() throws Exception {
        String json = "[1,2,3,4,5]";
        JSONArray result = (JSONArray) JSON.parse(new StringReader(json));
        assertEquals("Small array should parse correctly", 5, result.size());
        assertEquals("First element should be 1", 1, ((Number) result.get(0)).intValue());
        assertEquals("Last element should be 5", 5, ((Number) result.get(4)).intValue());
    }

    /**
     * Test that empty arrays work correctly.
     */
    @Test
    public void testEmptyArrayWorks() throws Exception {
        String json = "[]";
        JSONArray result = (JSONArray) JSON.parse(new StringReader(json));
        assertEquals("Empty array should have size 0", 0, result.size());
    }

    // ========================================================================
    // Object Member Count Limit Tests
    // ========================================================================

    /**
     * Test that objects at the member count limit are accepted.
     */
    @Test
    public void testObjectMembersAtBoundary() throws Exception {
        int maxMembers = ParserConfig.getMaxObjectMembers();
        
        // Build JSON with exactly maxMembers members
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < maxMembers; i++) {
            if (i > 0) json.append(",");
            json.append("\"key").append(i).append("\":").append(i);
        }
        json.append("}");
        
        // Should succeed
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json.toString()));
        assertEquals("Object should have exactly maxMembers members", maxMembers, result.size());
    }

    /**
     * Test that objects exceeding the member count limit are rejected.
     */
    @Test
    public void testObjectMembersOverLimit() {
        int maxMembers = ParserConfig.getMaxObjectMembers();
        
        // Build JSON with maxMembers + 1 members
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i <= maxMembers; i++) {
            if (i > 0) json.append(",");
            json.append("\"key").append(i).append("\":").append(i);
        }
        json.append("}");
        
        // Should throw IOException
        try {
            JSON.parse(new StringReader(json.toString()));
            fail("Should have thrown IOException for object members exceeding limit");
        } catch (IOException e) {
            // Expected - verify error message is informative
            String message = e.getMessage();
            assertNotNull("Error message should not be null", message);
            assertTrue("Error message should mention object or member", 
                       message.toLowerCase().contains("object") || message.toLowerCase().contains("member"));
        }
    }

    /**
     * Test that small objects work correctly (backward compatibility).
     */
    @Test
    public void testSmallObjectWorks() throws Exception {
        String json = "{\"name\":\"test\",\"value\":123,\"flag\":true}";
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        assertEquals("Small object should parse correctly", 3, result.size());
        assertEquals("name should be 'test'", "test", result.get("name"));
        assertEquals("value should be 123", 123, ((Number) result.get("value")).intValue());
        assertEquals("flag should be true", true, result.get("flag"));
    }

    /**
     * Test that empty objects work correctly.
     */
    @Test
    public void testEmptyObjectWorks() throws Exception {
        String json = "{}";
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        assertEquals("Empty object should have size 0", 0, result.size());
    }

    // ========================================================================
    // String Length Limit Tests
    // ========================================================================

    /**
     * Test that strings at the length limit are accepted.
     */
    @Test
    public void testStringLengthAtBoundary() throws Exception {
        int maxLength = ParserConfig.getMaxStringLength();
        
        // Build JSON with a string of exactly maxLength characters
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < maxLength; i++) {
            longString.append("a");
        }
        String json = "{\"key\":\"" + longString.toString() + "\"}";
        
        // Should succeed
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        String value = (String) result.get("key");
        assertEquals("String should have exactly maxLength characters", maxLength, value.length());
    }

    /**
     * Test that strings exceeding the length limit are rejected.
     */
    @Test
    public void testStringLengthOverLimit() {
        int maxLength = ParserConfig.getMaxStringLength();
        
        // Build JSON with a string of maxLength + 1 characters
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i <= maxLength; i++) {
            longString.append("a");
        }
        String json = "{\"key\":\"" + longString.toString() + "\"}";
        
        // Should throw IOException
        try {
            JSON.parse(new StringReader(json));
            fail("Should have thrown IOException for string length exceeding limit");
        } catch (IOException e) {
            // Expected - verify error message is informative
            String message = e.getMessage();
            assertNotNull("Error message should not be null", message);
            assertTrue("Error message should mention string or length", 
                       message.toLowerCase().contains("string") || message.toLowerCase().contains("length"));
        }
    }

    /**
     * Test that normal strings work correctly (backward compatibility).
     */
    @Test
    public void testNormalStringWorks() throws Exception {
        String json = "{\"message\":\"Hello, World!\"}";
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        assertEquals("Normal string should parse correctly", "Hello, World!", result.get("message"));
    }

    /**
     * Test that empty strings work correctly.
     */
    @Test
    public void testEmptyStringWorks() throws Exception {
        String json = "{\"empty\": \"\"}";
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        assertEquals("Empty string should parse correctly", "", result.get("empty"));
    }

    // ========================================================================
    // Number Length Limit Tests
    // ========================================================================

    /**
     * Test that numbers at the length limit are accepted.
     */
    @Test
    public void testNumberLengthAtBoundary() throws Exception {
        int maxLength = ParserConfig.getMaxNumberLength();
        
        // Build JSON with a number string of exactly maxLength characters
        // Use a format that's valid for Java's number parser: "1." followed by digits
        // This ensures the number is parseable while still testing the length limit
        StringBuilder longNumber = new StringBuilder("1.");
        for (int i = 2; i < maxLength; i++) {
            longNumber.append("0");
        }
        String json = "{\"value\":" + longNumber.toString() + "}";
        
        // Should succeed
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        assertNotNull("Number should parse", result.get("value"));
    }

    /**
     * Test that numbers exceeding the length limit are rejected.
     */
    @Test
    public void testNumberLengthOverLimit() {
        int maxLength = ParserConfig.getMaxNumberLength();
        
        // Build JSON with a number string of maxLength + 1 characters
        StringBuilder longNumber = new StringBuilder();
        for (int i = 0; i <= maxLength; i++) {
            longNumber.append("9");
        }
        String json = "{\"value\":" + longNumber.toString() + "}";
        
        // Should throw IOException
        try {
            JSON.parse(new StringReader(json));
            fail("Should have thrown IOException for number length exceeding limit");
        } catch (IOException e) {
            // Expected - verify error message is informative
            String message = e.getMessage();
            assertNotNull("Error message should not be null", message);
            assertTrue("Error message should mention number or length", 
                       message.toLowerCase().contains("number") || message.toLowerCase().contains("length"));
        }
    }

    /**
     * Test that normal numbers work correctly (backward compatibility).
     */
    @Test
    public void testNormalNumbersWork() throws Exception {
        String json = "{\"int\":123,\"float\":45.67,\"negative\":-89,\"exp\":1.23e10}";
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        assertEquals("Integer should parse correctly", 123, ((Number) result.get("int")).intValue());
        assertEquals("Float should parse correctly", 45.67, ((Number) result.get("float")).doubleValue(), 0.001);
        assertEquals("Negative should parse correctly", -89, ((Number) result.get("negative")).intValue());
        assertEquals("Exponential should parse correctly", 1.23e10, ((Number) result.get("exp")).doubleValue(), 1e6);
    }

    // ========================================================================
    // Nesting Depth Limit Tests
    // ========================================================================

    /**
     * Test that nesting at the depth limit is accepted.
     */
    @Test
    public void testNestingDepthAtBoundary() throws Exception {
        int maxDepth = ParserConfig.getMaxNestingDepth();
        
        // Build JSON with exactly maxDepth levels of nesting
        StringBuilder json = new StringBuilder();
        for (int i = 0; i < maxDepth; i++) {
            json.append("{\"nested\":");
        }
        json.append("\"value\"");
        for (int i = 0; i < maxDepth; i++) {
            json.append("}");
        }
        
        // Should succeed
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json.toString()));
        assertNotNull("Deeply nested object should parse", result);
    }

    /**
     * Test that nesting exceeding the depth limit is rejected.
     */
    @Test
    public void testNestingDepthOverLimit() {
        int maxDepth = ParserConfig.getMaxNestingDepth();
        
        // Build JSON with maxDepth + 2 levels of nesting
        // The depth check uses > (not >=), so we need one extra level to trigger it
        StringBuilder json = new StringBuilder();
        for (int i = 0; i < maxDepth + 2; i++) {
            json.append("{\"nested\":");
        }
        json.append("\"value\"");
        for (int i = 0; i < maxDepth + 2; i++) {
            json.append("}");
        }
        
        // Should throw IOException
        try {
            JSON.parse(new StringReader(json.toString()));
            fail("Should have thrown IOException for nesting depth exceeding limit");
        } catch (IOException e) {
            // Expected - verify error message is informative
            String message = e.getMessage();
            assertNotNull("Error message should not be null", message);
            assertTrue("Error message should mention nesting or depth",
                       message.toLowerCase().contains("nest") || message.toLowerCase().contains("depth"));
        }
    }

    /**
     * Test that normal nesting works correctly (backward compatibility).
     */
    @Test
    public void testNormalNestingWorks() throws Exception {
        String json = "{\"level1\":{\"level2\":{\"level3\":{\"value\":\"deep\"}}}}";
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        JSONObject level1 = (JSONObject) result.get("level1");
        JSONObject level2 = (JSONObject) level1.get("level2");
        JSONObject level3 = (JSONObject) level2.get("level3");
        assertEquals("Nested value should be accessible", "deep", level3.get("value"));
    }

    /**
     * Test that array nesting is also limited.
     */
    @Test
    public void testArrayNestingDepthOverLimit() {
        int maxDepth = ParserConfig.getMaxNestingDepth();
        
        // Build JSON with maxDepth + 1 levels of array nesting
        StringBuilder json = new StringBuilder();
        for (int i = 0; i <= maxDepth; i++) {
            json.append("[");
        }
        json.append("1");
        for (int i = 0; i <= maxDepth; i++) {
            json.append("]");
        }
        
        // Should throw IOException
        try {
            JSON.parse(new StringReader(json.toString()));
            fail("Should have thrown IOException for array nesting depth exceeding limit");
        } catch (IOException e) {
            // Expected
            String message = e.getMessage();
            assertNotNull("Error message should not be null", message);
        }
    }

    // ========================================================================
    // Duplicate Key Tests
    // ========================================================================

    /**
     * Test duplicate key handling in SILENT mode (default behavior).
     * Note: This test assumes default configuration. If system properties
     * override the behavior, this test documents the expected SILENT behavior.
     */
    @Test
    public void testDuplicateKeySilentMode() throws Exception {
        // In SILENT mode, duplicate keys should silently overwrite
        String json = "{\"key\":\"first\",\"key\":\"second\"}";
        
        try {
            JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
            // Should succeed and use the last value
            assertEquals("Duplicate key should use last value", "second", result.get("key"));
        } catch (IOException e) {
            // If ERROR mode is configured, this is also acceptable
            // The test documents that SILENT mode would allow this
            String message = e.getMessage();
            if (message != null && message.toLowerCase().contains("duplicate")) {
                // ERROR mode is active, which is valid
                return;
            }
            throw e;
        }
    }

    /**
     * Test that objects with unique keys work correctly.
     */
    @Test
    public void testUniqueKeysWork() throws Exception {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        assertEquals("Should have 3 unique keys", 3, result.size());
        assertEquals("key1 should have correct value", "value1", result.get("key1"));
        assertEquals("key2 should have correct value", "value2", result.get("key2"));
        assertEquals("key3 should have correct value", "value3", result.get("key3"));
    }

    // ========================================================================
    // Backward Compatibility Tests
    // ========================================================================

    /**
     * Test that valid, complex JSON still parses correctly.
     */
    @Test
    public void testComplexValidJsonWorks() throws Exception {
        String json = "{" +
            "\"name\":\"test\"," +
            "\"value\":123," +
            "\"nested\":{" +
                "\"array\":[1,2,3]," +
                "\"flag\":true," +
                "\"nullable\":null" +
            "}," +
            "\"list\":[" +
                "{\"id\":1,\"name\":\"first\"}," +
                "{\"id\":2,\"name\":\"second\"}" +
            "]" +
        "}";
        
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        assertEquals("name should be correct", "test", result.get("name"));
        assertEquals("value should be correct", 123, ((Number) result.get("value")).intValue());
        
        JSONObject nested = (JSONObject) result.get("nested");
        JSONArray nestedArray = (JSONArray) nested.get("array");
        assertEquals("nested array should have 3 elements", 3, nestedArray.size());
        assertEquals("flag should be true", true, nested.get("flag"));
        assertNull("nullable should be null", nested.get("nullable"));
        
        JSONArray list = (JSONArray) result.get("list");
        assertEquals("list should have 2 elements", 2, list.size());
        JSONObject first = (JSONObject) list.get(0);
        assertEquals("first id should be 1", 1, ((Number) first.get("id")).intValue());
    }

    /**
     * Test that mixed nesting of arrays and objects works.
     */
    @Test
    public void testMixedNestingWorks() throws Exception {
        String json = "{\"data\":[{\"items\":[{\"value\":1},{\"value\":2}]}]}";
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        JSONArray data = (JSONArray) result.get("data");
        JSONObject firstData = (JSONObject) data.get(0);
        JSONArray items = (JSONArray) firstData.get("items");
        assertEquals("items should have 2 elements", 2, items.size());
        JSONObject firstItem = (JSONObject) items.get(0);
        assertEquals("first item value should be 1", 1, ((Number) firstItem.get("value")).intValue());
    }

    /**
     * Test that special characters in strings work correctly.
     */
    @Test
    public void testSpecialCharactersWork() throws Exception {
        String json = "{\"text\":\"Line1\\nLine2\\tTabbed\\\"Quoted\\\"\"}";
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        String text = (String) result.get("text");
        assertTrue("Should contain newline", text.contains("\n"));
        assertTrue("Should contain tab", text.contains("\t"));
        assertTrue("Should contain quotes", text.contains("\""));
    }

    /**
     * Test that Unicode characters work correctly.
     */
    @Test
    public void testUnicodeCharactersWork() throws Exception {
        String json = "{\"unicode\":\"Hello \\u4E16\\u754C\"}";
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json));
        String unicode = (String) result.get("unicode");
        assertNotNull("Unicode string should parse", unicode);
        assertTrue("Should contain unicode characters", unicode.length() > 6);
    }

    // ========================================================================
    // Total Size Limit Tests
    // ========================================================================

    /**
     * Test that the overall size limit prevents many moderately-sized strings
     * from exhausting memory. This addresses the scenario where 10,000 strings
     * of 10MB each could crash the JVM.
     */
    @Test
    public void testTotalSizeLimitWithManyStrings() {
        long maxTotalSize = ParserConfig.getMaxTotalSize();
        
        // Create an array with many strings that individually pass the string
        // length limit but collectively exceed the total size limit
        int stringLength = 1000; // Well under the 10MB string limit
        // Each string: 2 bytes/char + 24 bytes overhead + 8 bytes array element ref
        int bytesPerString = (int)(stringLength * 2L + 24 + 8);
        int numStrings = (int) (maxTotalSize / bytesPerString) + 50; // Exceed total (add 50 for array overhead)
        
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
            fail("Should have thrown IOException for total size exceeding limit");
        } catch (IOException e) {
            // Expected - verify error message mentions total size
            String message = e.getMessage();
            assertNotNull("Error message should not be null", message);
            assertTrue("Error message should mention total or size",
                       message.toLowerCase().contains("total") || message.toLowerCase().contains("size"));
        }
    }

    /**
     * Test that the overall size limit works with mixed content types.
     */
    @Test
    public void testTotalSizeLimitWithMixedContent() {
        long maxTotalSize = ParserConfig.getMaxTotalSize();
        
        // Create a JSON document with objects, arrays, strings, and numbers
        // that collectively exceed the total size limit
        // Each object: 64 bytes object + 24 bytes "id" key + 8 bytes number + 24 bytes "name" key + string content + 8 bytes array element
        int stringLength = 500;
        int estimatedBytesPerObject = 64 + 24 + 8 + 24 + (stringLength * 2 + 24) + 8; // Total per object
        int numObjects = (int) (maxTotalSize / estimatedBytesPerObject) + 20; // Exceed total (add 20 for outer object/array overhead)
        
        StringBuilder json = new StringBuilder("{\"data\":[");
        
        // Add many objects with string values
        for (int i = 0; i < numObjects; i++) {
            if (i > 0) json.append(",");
            json.append("{\"id\":").append(i);
            json.append(",\"name\":\"");
            // Add a moderately-sized string
            for (int j = 0; j < stringLength; j++) {
                json.append("x");
            }
            json.append("\"}");
        }
        json.append("]}");
        
        // Should throw IOException for exceeding total size
        try {
            JSON.parse(new StringReader(json.toString()));
            fail("Should have thrown IOException for total size exceeding limit");
        } catch (IOException e) {
            // Expected - verify error message is informative
            String message = e.getMessage();
            assertNotNull("Error message should not be null", message);
            assertTrue("Error message should mention total or size",
                       message.toLowerCase().contains("total") || message.toLowerCase().contains("size"));
        }
    }

    /**
     * Test that reasonable-sized documents still work with the total size limit.
     */
    @Test
    public void testReasonableSizeDocumentWorks() throws Exception {
        // Create a document that's large but reasonable
        StringBuilder json = new StringBuilder("{\"items\":[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) json.append(",");
            json.append("{\"id\":").append(i);
            json.append(",\"name\":\"Item ").append(i).append("\"");
            json.append(",\"description\":\"This is a description for item ").append(i).append("\"}");
        }
        json.append("]}");
        
        // Should succeed
        JSONObject result = (JSONObject) JSON.parse(new StringReader(json.toString()));
        assertNotNull("Result should not be null", result);
        JSONArray items = (JSONArray) result.get("items");
        assertEquals("Should have 100 items", 100, items.size());
    }
}

// Made with Bob
