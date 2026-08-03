/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.openliberty.mcp.internal.spi.MetaCarrierBuilderImpl;

public class MetaCarrierBuilderImplTest {

    private static void assertInvalid(String key) {
        try {
            MetaCarrierBuilderImpl.requireValidMetaKey(key);
            fail("Expected IllegalArgumentException for key: " + key);
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    private static void assertValid(String key) {
        MetaCarrierBuilderImpl.requireValidMetaKey(key); // must not throw
    }

    // Null key
    @Test
    public void testNullKeyIsRejected() {
        assertInvalid(null);
    }

    // Name-only keys (no prefix)
    @Test
    public void testValidNameOnly() {
        assertValid("myKey");
        assertValid("a");
        assertValid("Z");
        assertValid("0");
        assertValid("my-key");
        assertValid("my_key");
        assertValid("my.key");
        assertValid("my-key_v2.0");
        assertValid("abc123");
    }

    @Test
    public void testEmptyKeyIsValid() {
        // An empty name is explicitly allowed by the spec ("unless empty")
        assertValid("");
    }

    @Test
    public void testNameOnlyMustStartWithAlphanumeric() {
        assertInvalid("-myKey");
        assertInvalid("_myKey");
        assertInvalid(".myKey");
    }

    @Test
    public void testNameOnlyMustEndWithAlphanumeric() {
        assertInvalid("myKey-");
        assertInvalid("myKey_");
        assertInvalid("myKey.");
    }

    @Test
    public void testValidPrefixedKeys() {
        assertValid("com.example/myKey");
        assertValid("com.example/"); // empty name is allowed
        assertValid("io.openliberty/feature");
        assertValid("a.b/c");
        assertValid("x1.y2/z");
        assertValid("com.example-corp/key"); // hyphen in label interior
        assertValid("com.example/my-key_v1.0");
    }

    @Test
    public void testSingleLabelPrefix() {
        // Only one label before the slash — no second label, so not reserved
        assertValid("com/key");
        assertValid("io/feature");
    }

    // Prefixed keys — invalid labels
    @Test
    public void testEmptyPrefixIsRejected() {
        assertInvalid("/key");
    }

    @Test
    public void testLabelMustStartWithLetter() {
        assertInvalid("1com.example/key"); // first label starts with digit
        assertInvalid("-com.example/key"); // first label starts with hyphen
        assertInvalid("com.1example/key"); // second label starts with digit
    }

    @Test
    public void testLabelMustEndWithLetterOrDigit() {
        assertInvalid("com-.example/key"); // first label ends with hyphen
        assertInvalid("com.example-/key"); // last label ends with hyphen
    }

    @Test
    public void testEmptyLabelInPrefixIsRejected() {
        assertInvalid("com..example/key"); // consecutive dots → empty label
        assertInvalid(".com.example/key"); // leading dot → empty first label
        assertInvalid("com.example./key"); // trailing dot → empty last label
    }

    // putMetadata and setMetadata integration
    @Test
    public void testPutMetadataValidKey() {
        MetaCarrierBuilderImpl<?> builder = new MetaCarrierBuilderImpl<>();
        builder.putMetadata("com.example/key", "value");
        assertEquals("value", builder.metadata.get("com.example/key"));
    }

    @Test
    public void testPutMetadataInvalidKeyThrows() {
        MetaCarrierBuilderImpl<?> builder = new MetaCarrierBuilderImpl<>();
        try {
            builder.putMetadata("com.example-/key", "value");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testSetMetadataValidMap() {
        MetaCarrierBuilderImpl<?> builder = new MetaCarrierBuilderImpl<>();
        Map<String, Object> map = new HashMap<>();
        map.put("com.example/key", 42);
        map.put("plainName", "hello");
        builder.setMetadata(map);
        assertEquals(42, builder.metadata.get("com.example/key"));
        assertEquals("hello", builder.metadata.get("plainName"));
    }

    @Test
    public void testSetMetadataInvalidKeyThrows() {
        MetaCarrierBuilderImpl<?> builder = new MetaCarrierBuilderImpl<>();
        Map<String, Object> map = new HashMap<>();
        map.put("com.example-/key", "value");
        try {
            builder.setMetadata(map);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testSetMetadataNullClearsMap() {
        MetaCarrierBuilderImpl<?> builder = new MetaCarrierBuilderImpl<>();
        builder.putMetadata("com.example/key", "value");
        builder.setMetadata(null);
        assertEquals(0, builder.metadata.size());
    }
}
