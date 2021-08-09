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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.ibm.ws.repository.resolver.internal.LibertyVersion;

/**
 * Test for {@link LibertyVersion}
 */
public class LibertyVersionTest {

    /**
     * Tests you can construct versions and they implement equals and compare to correctly
     */
    @Test
    public void testLibertyVersion() {
        String s8550 = "8.5.5.0";
        String s8551 = "8.5.5.1";
        String s8560 = "8.5.6.0";
        String s8650 = "8.6.5.0";
        String s9550 = "9.5.5.0";
        LibertyVersion v8550 = LibertyVersion.valueOf(s8550);
        LibertyVersion v8550_2 = LibertyVersion.valueOf(s8550);
        LibertyVersion v8551 = LibertyVersion.valueOf(s8551);
        LibertyVersion v8560 = LibertyVersion.valueOf(s8560);
        LibertyVersion v8650 = LibertyVersion.valueOf(s8650);
        LibertyVersion v9550 = LibertyVersion.valueOf(s9550);

        assertEquals(s8550, v8550.toString());
        assertTrue(v8550.equals(v8550_2));
        assertEquals(0, v8550.compareTo(v8550_2));

        assertTrue(v8550.matchesToMicros(null));
        assertTrue(v8550.matchesToMicros(v8551));
        assertTrue(v8551.matchesToMicros(v8550));

        assertFalse(v8550.matchesToMicros(v8560));
        assertFalse(v8550.matchesToMicros(v8650));
        assertFalse(v8550.matchesToMicros(v9550));
        assertFalse(v8560.matchesToMicros(v8550));
        assertFalse(v8650.matchesToMicros(v8550));
        assertFalse(v9550.matchesToMicros(v8550));

        testUnequalVersion(s8551, v8550, v8551);
        testUnequalVersion(s8560, v8550, v8560);
        testUnequalVersion(s8650, v8550, v8650);
        testUnequalVersion(s9550, v8550, v9550);
    }

    /**
     * Tests invalid strings on the version
     */
    @Test
    public void testErrorHandling() {
        assertNull(LibertyVersion.valueOf(null));
        assertNull(LibertyVersion.valueOf("8.5.5.a"));
        assertNull(LibertyVersion.valueOf("8.5.5.0.0"));
        assertNull(LibertyVersion.valueOf(""));
        assertNull(LibertyVersion.valueOf("fish"));
        LibertyVersion v8550 = LibertyVersion.valueOf("8.5.5.0");
        try {
            v8550.compareTo(null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            // pass
        }
    }

    /**
     * Tests to versions against each other. Will test the string matches the toString on the higher value and that equals and compare to work and are symetrical.
     * 
     * @param versionString
     * @param lower
     * @param higher
     */
    private void testUnequalVersion(String versionString, LibertyVersion lower, LibertyVersion higher) {
        assertEquals(versionString, higher.toString());
        assertFalse(higher.equals(lower));
        assertFalse(higher.equals(null));
        assertTrue(higher.equals(higher));
        assertFalse(lower.equals(higher));
        assertTrue(higher.compareTo(lower) > 0);
        assertTrue(lower.compareTo(higher) < 0);
        assertTrue(higher.matchesToMicros(null));
    }

}
