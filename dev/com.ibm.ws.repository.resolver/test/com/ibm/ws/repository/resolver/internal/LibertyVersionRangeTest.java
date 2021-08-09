/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.resolver.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.ibm.ws.repository.resolver.internal.LibertyVersion;
import com.ibm.ws.repository.resolver.internal.LibertyVersionRange;

/**
 * Tests for {@link LibertyVersionRange}.
 */
public class LibertyVersionRangeTest {

    /**
     * Test that you can parse a version range
     */
    @Test
    public void testVersionRange() {
        String versionRangeString = "[8.5.5.4,8.5.5.6]";
        String versionRangeString2 = "[8.5.5.4, 8.5.5.6]";
        LibertyVersionRange range = LibertyVersionRange.valueOf(versionRangeString);
        LibertyVersionRange range2 = LibertyVersionRange.valueOf(versionRangeString2);
        assertEquals(versionRangeString, range.toString());
        assertEquals(range, range2);
        assertEquals(LibertyVersion.valueOf("8.5.5.4"), range.getMinVersion());
        assertEquals(LibertyVersion.valueOf("8.5.5.6"), range.getMaxVersion());
    }

    /**
     * Tests that if you pass in a single version it is parsed as a version range
     */
    @Test
    public void testVersion() {
        String versionRangeString = "8.5.5.4";
        LibertyVersionRange range = LibertyVersionRange.valueOf(versionRangeString);
        assertEquals(versionRangeString, range.toString());
        assertEquals(LibertyVersion.valueOf("8.5.5.4"), range.getMinVersion());
        assertNull(range.getMaxVersion());
    }

    /**
     * Tests that there is tolerance for rubbish data
     */
    @Test
    public void testInvalidInput() {
        assertNull(LibertyVersionRange.valueOf(null));
        assertNull(LibertyVersionRange.valueOf("[wibble,wobble]"));
        assertNull(LibertyVersionRange.valueOf("fish"));
        assertNull(LibertyVersionRange.valueOf("[8.5.5.4,wobble]"));
    }

}
