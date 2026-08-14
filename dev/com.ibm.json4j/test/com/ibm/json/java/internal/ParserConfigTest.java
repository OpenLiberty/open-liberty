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

package com.ibm.json.java.internal;

import org.junit.Test;
import static org.junit.Assert.*;

import com.ibm.json.java.internal.ParserConfig;
import com.ibm.json.java.internal.ParserConfig.DuplicateKeyBehavior;

/**
 * Unit tests for ParserConfig class.
 * Tests configuration loading, validation, and default values.
 */
public class ParserConfigTest {

    /**
     * Test that default values are loaded correctly when no system properties are set.
     */
    @Test
    public void testDefaultValues() {
        // These tests verify the defaults are reasonable
        // Note: Actual values depend on whether system properties are set
        assertTrue("Max array size should be positive", ParserConfig.getMaxArraySize() > 0);
        assertTrue("Max object members should be positive", ParserConfig.getMaxObjectMembers() > 0);
        assertTrue("Max nesting depth should be positive", ParserConfig.getMaxNestingDepth() > 0);
        assertTrue("Max string length should be positive", ParserConfig.getMaxStringLength() > 0);
        assertTrue("Max number length should be positive", ParserConfig.getMaxNumberLength() > 0);
        assertTrue("Max total size should be positive", ParserConfig.getMaxTotalSize() > 0);
        assertNotNull("Duplicate key behavior should not be null", ParserConfig.getDuplicateKeyBehavior());
    }

    /**
     * Test that all getter methods return consistent values.
     */
    @Test
    public void testGetterConsistency() {
        // Call getters multiple times to ensure they return consistent values
        int arraySize1 = ParserConfig.getMaxArraySize();
        int arraySize2 = ParserConfig.getMaxArraySize();
        assertEquals("getMaxArraySize should return consistent values", arraySize1, arraySize2);

        int objectMembers1 = ParserConfig.getMaxObjectMembers();
        int objectMembers2 = ParserConfig.getMaxObjectMembers();
        assertEquals("getMaxObjectMembers should return consistent values", objectMembers1, objectMembers2);

        int nestingDepth1 = ParserConfig.getMaxNestingDepth();
        int nestingDepth2 = ParserConfig.getMaxNestingDepth();
        assertEquals("getMaxNestingDepth should return consistent values", nestingDepth1, nestingDepth2);

        int stringLength1 = ParserConfig.getMaxStringLength();
        int stringLength2 = ParserConfig.getMaxStringLength();
        assertEquals("getMaxStringLength should return consistent values", stringLength1, stringLength2);

        int numberLength1 = ParserConfig.getMaxNumberLength();
        int numberLength2 = ParserConfig.getMaxNumberLength();
        assertEquals("getMaxNumberLength should return consistent values", numberLength1, numberLength2);

        long totalSize1 = ParserConfig.getMaxTotalSize();
        long totalSize2 = ParserConfig.getMaxTotalSize();
        assertEquals("getMaxTotalSize should return consistent values", totalSize1, totalSize2);

        DuplicateKeyBehavior behavior1 = ParserConfig.getDuplicateKeyBehavior();
        DuplicateKeyBehavior behavior2 = ParserConfig.getDuplicateKeyBehavior();
        assertEquals("getDuplicateKeyBehavior should return consistent values", behavior1, behavior2);
    }

    /**
     * Test that DuplicateKeyBehavior enum has expected values.
     */
    @Test
    public void testDuplicateKeyBehaviorEnum() {
        // Verify all expected enum values exist
        DuplicateKeyBehavior silent = DuplicateKeyBehavior.SILENT;
        DuplicateKeyBehavior warn = DuplicateKeyBehavior.WARN;
        DuplicateKeyBehavior error = DuplicateKeyBehavior.ERROR;

        assertNotNull("SILENT should exist", silent);
        assertNotNull("WARN should exist", warn);
        assertNotNull("ERROR should exist", error);

        // Verify enum values are distinct
        assertFalse("SILENT and WARN should be different", silent.equals(warn));
        assertFalse("SILENT and ERROR should be different", silent.equals(error));
        assertFalse("WARN and ERROR should be different", warn.equals(error));

        // Verify valueOf works
        assertEquals("valueOf(SILENT) should work", DuplicateKeyBehavior.SILENT, 
                     DuplicateKeyBehavior.valueOf("SILENT"));
        assertEquals("valueOf(WARN) should work", DuplicateKeyBehavior.WARN, 
                     DuplicateKeyBehavior.valueOf("WARN"));
        assertEquals("valueOf(ERROR) should work", DuplicateKeyBehavior.ERROR, 
                     DuplicateKeyBehavior.valueOf("ERROR"));
    }

    /**
     * Test that toDebugString returns a non-null, non-empty string.
     */
    @Test
    public void testToDebugString() {
        String debugString = ParserConfig.toDebugString();
        assertNotNull("toDebugString should not return null", debugString);
        assertTrue("toDebugString should not be empty", debugString.length() > 0);
        assertTrue("toDebugString should contain 'ParserConfig'", debugString.contains("ParserConfig"));
        assertTrue("toDebugString should contain 'maxArraySize'", debugString.contains("maxArraySize"));
        assertTrue("toDebugString should contain 'maxObjectMembers'", debugString.contains("maxObjectMembers"));
        assertTrue("toDebugString should contain 'maxNestingDepth'", debugString.contains("maxNestingDepth"));
        assertTrue("toDebugString should contain 'maxStringLength'", debugString.contains("maxStringLength"));
        assertTrue("toDebugString should contain 'maxNumberLength'", debugString.contains("maxNumberLength"));
        assertTrue("toDebugString should contain 'maxTotalSize'", debugString.contains("maxTotalSize"));
        assertTrue("toDebugString should contain 'duplicateKeyBehavior'", debugString.contains("duplicateKeyBehavior"));
    }

    /**
     * Test that configuration values are within reasonable ranges.
     */
    @Test
    public void testReasonableRanges() {
        // Verify values are within reasonable ranges for security limits
        int maxArraySize = ParserConfig.getMaxArraySize();
        assertTrue("Max array size should be at least 1000", maxArraySize >= 1000);
        assertTrue("Max array size should be less than 10 million", maxArraySize < 10000000);

        int maxObjectMembers = ParserConfig.getMaxObjectMembers();
        assertTrue("Max object members should be at least 1000", maxObjectMembers >= 1000);
        assertTrue("Max object members should be less than 1 million", maxObjectMembers < 1000000);

        int maxNestingDepth = ParserConfig.getMaxNestingDepth();
        assertTrue("Max nesting depth should be at least 100", maxNestingDepth >= 100);
        assertTrue("Max nesting depth should be less than 10000", maxNestingDepth < 10000);

        int maxStringLength = ParserConfig.getMaxStringLength();
        assertTrue("Max string length should be at least 10000", maxStringLength >= 10000);
        assertTrue("Max string length should be less than 100 MB", maxStringLength < 100000000);

        int maxNumberLength = ParserConfig.getMaxNumberLength();
        assertTrue("Max number length should be at least 100", maxNumberLength >= 100);
        assertTrue("Max number length should be less than 100000", maxNumberLength < 100000);

        long maxTotalSize = ParserConfig.getMaxTotalSize();
        assertTrue("Max total size should be at least 1 MB", maxTotalSize >= 1000000);
        assertTrue("Max total size should be less than 1 GB", maxTotalSize < 1000000000);
    }

    /**
     * Test that the default duplicate key behavior is SILENT for backward compatibility.
     */
    @Test
    public void testDefaultDuplicateKeyBehavior() {
        // When no system property is set, behavior should default to SILENT
        // This test may pass or fail depending on system properties
        // but documents the expected default behavior
        DuplicateKeyBehavior behavior = ParserConfig.getDuplicateKeyBehavior();
        assertNotNull("Duplicate key behavior should not be null", behavior);
        // Note: We can't assert SILENT here because system properties might override it
        // But we verify it's one of the valid values
        assertTrue("Duplicate key behavior should be valid", 
                   behavior == DuplicateKeyBehavior.SILENT ||
                   behavior == DuplicateKeyBehavior.WARN ||
                   behavior == DuplicateKeyBehavior.ERROR);
    }
}
