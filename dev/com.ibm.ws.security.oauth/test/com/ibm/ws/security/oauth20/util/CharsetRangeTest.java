/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 *
 */
public class CharsetRangeTest {

    private final String ALL_CHARSET = "*";

    /**
     * Test that calling the constructor
     * with the given values constructs the
     * required CharsetRange object
     */
    @Test
    public void testConstructor() {

        String charSet = "UTF-8";
        CharsetRange range = new CharsetRange(charSet, 0.7f);
        assertNotNull(range);
        assertEquals(range.getType(), charSet.toLowerCase());
        assertEquals(range.getQValue().doubleValue(), 0.7, 0.002);

    }

    /**
     * Test that calling the constructor
     * with null char-set as null or empty string
     * sets it to the default value of * (all types)
     */
    @Test
    public void testDefaultObject() {

        CharsetRange range = new CharsetRange(null, 0.4f);
        assertNotNull(range);
        assertEquals(range.getType(), ALL_CHARSET);
        assertEquals(range.getQValue().doubleValue(), 0.4, 0.002);

        range = new CharsetRange("", 0.4f);
        assertNotNull(range);
        assertEquals(range.getType(), ALL_CHARSET);
        assertEquals(range.getQValue().doubleValue(), 0.4, 0.002);

    }

    /**
     * Test that calling the constructor
     * with invalid quality value leads to {@link NumberFormatException}
     */
    @Test(expected = NumberFormatException.class)
    public void testDefaultObjectInvalidQualityValue() {

        CharsetRange range = new CharsetRange("UTF-8", 2.4f);
        assertNull(range);

    }

    /**
     * Test that calling the parse method
     * with a null value or empty string
     * returns a CharSet range representing
     * all values (*) and a quality value of 1.0
     */
    @Test
    public void parseNullCharSet() {
        CharsetRange[] ranges = CharsetRange.parse(null);
        assertNotNull(ranges);
        assertEquals(ranges.length, 1);
        assertEquals(ranges[0].getType(), ALL_CHARSET);
        assertEquals(ranges[0].getQValue().doubleValue(), 1.0, 0.002);

        ranges = CharsetRange.parse("");
        assertNotNull(ranges);
        assertEquals(ranges.length, 1);
        assertEquals(ranges[0].getType(), ALL_CHARSET);
        assertEquals(ranges[0].getQValue().doubleValue(), 1.0, 0.002);
    }

    /**
     * Test that calling the parse method
     * returns an array of parsed char-set ranges
     */
    @Test
    public void parseCharSetRangeWithAllRange() {
        String charSetHeaderValue = "windows-1252,utf-8;q=0.7,*;q=0.3";
        CharsetRange[] ranges = CharsetRange.parse(charSetHeaderValue);
        assertNotNull(ranges);
        assertEquals(ranges.length, 3);
        CharsetRange[] expectedRanges = { new CharsetRange("windows-1252", 1.0f), new CharsetRange("utf-8", 0.7f), new CharsetRange("*", 0.3f) };
        assertArrayEquals(expectedRanges, ranges);

    }

    /**
     * Test that calling the parse method
     * returns an array of parsed char-set ranges
     * Test that the default char-set iso-8859-1 is
     * added with a quality range of 1.0
     * 
     */
    @Test
    public void parseCharSetRange() {
        String charSetHeaderValue = "windows-1252,utf-8;q=0.9";
        CharsetRange[] ranges = CharsetRange.parse(charSetHeaderValue);
        assertNotNull(ranges);
        assertEquals(ranges.length, 3);
        CharsetRange[] expectedRanges = { new CharsetRange("iso-8859-1", 1.0f), new CharsetRange("windows-1252", 1.0f), new CharsetRange("utf-8", 0.9f) };
        assertArrayEquals(expectedRanges, ranges);

    }

    @Test
    public void parseCharSetRangeMissingValues() {
        String charSetHeaderValue = "windows-1252,utf-8;q=0.9,utf-16;level=1";
        CharsetRange[] ranges = CharsetRange.parse(charSetHeaderValue);
        assertNotNull(ranges);
        assertEquals(ranges.length, 4);
        CharsetRange[] expectedRanges = { new CharsetRange("iso-8859-1", 1.0f), new CharsetRange("utf-16", 1.0f), new CharsetRange("windows-1252", 1.0f),
                                         new CharsetRange("utf-8", 0.9f) };
        assertArrayEquals(expectedRanges, ranges);

    }
}
