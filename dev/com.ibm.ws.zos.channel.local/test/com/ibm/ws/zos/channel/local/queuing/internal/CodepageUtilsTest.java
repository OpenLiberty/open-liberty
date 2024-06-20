/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.queuing.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Sanity tests for codepage utilites.
 */
public class CodepageUtilsTest {

    @Test
    public void testPadRight() {

        assertEquals("BB  ", CodepageUtils.padRight("BB", 4));
        assertEquals("BB  ", CodepageUtils.padRight("BB  ", 4));
        assertEquals("BBBB", CodepageUtils.padRight("BBBB", 4));
        assertEquals("BBBBBB", CodepageUtils.padRight("BBBBBB", 4));
        assertEquals("    ", CodepageUtils.padRight(null, 4));
    }

    @Test
    public void testGetEbcdicBytes() {
        assertArrayEquals(new byte[] { (byte) 0xc2, (byte) 0xc2, (byte) 0xc7, (byte) 0xe9 },
                          CodepageUtils.getEbcdicBytes("BBGZ"));

        assertEquals(0, CodepageUtils.getEbcdicBytes(null).length);
    }

    @Test
    public void testGetEbcdicBytesPadded() {

        assertArrayEquals(new byte[] { (byte) 0xc2, (byte) 0xc2, (byte) 0xc7, (byte) 0xe9, 0x40, 0x40, 0x40, 0x40 },
                          CodepageUtils.getEbcdicBytesPadded("BBGZ", 8));

        assertArrayEquals(new byte[] { (byte) 0xc2, (byte) 0xc2, (byte) 0xc7, (byte) 0xe9, (byte) 0xe6, (byte) 0xd6, (byte) 0xd3, (byte) 0xc1 },
                          CodepageUtils.getEbcdicBytesPadded("BBGZWOLAplusextrastuffthatwillbetruncated", 8));

        assertArrayEquals(new byte[] { 0x40, 0x40, 0x40, 0x40 },
                          CodepageUtils.getEbcdicBytesPadded(null, 4));
    }

    @Test
    public void testGetEbcdicBytesAsLong() {
        assertEquals(0xc2c2d6c1d4e2c740L, CodepageUtils.getEbcdicBytesAsLong("BBOAMSG "));
        assertEquals(0xc2c2d6c1c3e3e740L, CodepageUtils.getEbcdicBytesAsLong("BBOACTX")); // blank-padded
        assertEquals(0xc2c2d6c1e2d5c340L, CodepageUtils.getEbcdicBytesAsLong("BBOASNC "));
        assertEquals(0x4040404040404040L, CodepageUtils.getEbcdicBytesAsLong(""));
    }

    @Test
    public void testEbcdicBytesToString() {
        assertEquals("BBGZ", CodepageUtils.ebcdicBytesToString(new byte[] { (byte) 0xc2, (byte) 0xc2, (byte) 0xc7, (byte) 0xe9 }));
        assertEquals("BBGZ", CodepageUtils.ebcdicBytesToString(new byte[] { (byte) 0xc2, (byte) 0xc2, (byte) 0xc7, (byte) 0xe9, 0x00, (byte) 0xe9 }));
        assertEquals("", CodepageUtils.ebcdicBytesToString(null));
    }

    @Test
    public void testFirstNull() {
        assertEquals(0, CodepageUtils.firstNull(null));
        assertEquals(3, CodepageUtils.firstNull(new byte[] { 0x01, 0x01, 0x01, 0x00, 0x01, 0x01 }));
        assertEquals(5, CodepageUtils.firstNull(new byte[] { 0x01, 0x01, 0x01, 0x01, 0x01 }));
    }

}
