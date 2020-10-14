/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.utils.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DirectBufferHelperTest extends DirectBufferHelperImpl {

    /** Mocked up address of random bytes. */
    final long RANDOM_BYTES_ADDRESS = GIGABYTE * 1;

    /** Length of the array containing random bytes. */
    final int RANDOM_BYTES_LENGTH = 4096;

    TestBufferHelper testBufferHelper = null;

    byte[] sourceRandomBytes = null;

    @Before
    public void setup() {
        testBufferHelper = new TestBufferHelper();

        // Fill a buffer with random bytes
        Random random = new Random(System.currentTimeMillis());
        sourceRandomBytes = new byte[RANDOM_BYTES_LENGTH];
        random.nextBytes(sourceRandomBytes);

        ByteBuffer bb = ByteBuffer.wrap(sourceRandomBytes);
        testBufferHelper.addBuffer(RANDOM_BYTES_ADDRESS, bb);
    }

    @After
    public void after() {
        testBufferHelper = null;
        sourceRandomBytes = null;
    }

    @Test
    public void testGetSlice() {
        final long address = GIGABYTE * 4;
        final int bufferSize = 1024;
        final int sliceSize = 512;

        ByteBuffer sourceByteBuffer = ByteBuffer.allocate(bufferSize);
        testBufferHelper.addBuffer(address, sourceByteBuffer);
        sourceByteBuffer.putLong(0, address);

        ByteBuffer bb = testBufferHelper.getSlice(address, sliceSize);

        // Make sure we got something back that makes sense
        assertNotNull(bb);
        assertTrue(bb.isReadOnly());
        assertEquals(sliceSize, bb.capacity());
        assertEquals(address, bb.getLong());
        assertEquals(address, bb.getLong(0));

        // Update the source buffer to make sure we're viewing it
        sourceByteBuffer.putInt(0, 0xDEADBEEF);
        assertEquals(0xDEADBEEF, bb.getInt(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetSliceTooBig() {
        testBufferHelper.getSlice(0, Integer.MAX_VALUE);
    }

    @Test
    public void testGetLong() {
        final long address = GIGABYTE * 8;
        final int bufferSize = 1024;
        ByteBuffer sourceByteBuffer = ByteBuffer.allocate(bufferSize);
        for (byte b = 0; sourceByteBuffer.remaining() > 0; b++) {
            sourceByteBuffer.put(b);
        }

        testBufferHelper.addBuffer(address, sourceByteBuffer);
        for (int b = 0; b < bufferSize; b++) {
            assertEquals((byte) (b & -1), testBufferHelper.get(address + b));
        }
    }

    @Test
    public void testGetLongByteArray() {
        byte[] destBytes = new byte[RANDOM_BYTES_LENGTH];
        testBufferHelper.get(RANDOM_BYTES_ADDRESS, destBytes);

        // Make sure we got back the same random bytes
        assertTrue(Arrays.equals(sourceRandomBytes, destBytes));

        byte[] destBytes2 = new byte[RANDOM_BYTES_LENGTH / 2];
        final int offset = RANDOM_BYTES_LENGTH / 4;
        testBufferHelper.get(RANDOM_BYTES_ADDRESS + offset, destBytes2);
        for (int i = 0; i < destBytes2.length; i++) {
            assertEquals(sourceRandomBytes[offset + i], destBytes2[i]);
        }
    }

    @Test
    public void testGetLongByteArrayIntInt() {
        byte[] destBytes = new byte[RANDOM_BYTES_LENGTH];
        int offset = RANDOM_BYTES_LENGTH / 4;

        // Make sure the array has consistent data
        byte knownByte = 0x40;
        Arrays.fill(destBytes, knownByte);

        testBufferHelper.get(RANDOM_BYTES_ADDRESS + offset, destBytes, offset, RANDOM_BYTES_LENGTH / 2);

        // Verify first quarter of bytes haven't changed
        for (int i = 0; i < offset; i++) {
            assertEquals(knownByte, destBytes[i]);
        }

        // Verify middle match random source bytes
        for (int i = offset; i < offset + RANDOM_BYTES_LENGTH / 2; i++) {
            assertEquals(sourceRandomBytes[i], destBytes[i]);
        }

        // Verify last quarter of bytes haven't changed
        for (int i = offset + RANDOM_BYTES_LENGTH / 2; i < destBytes.length; i++) {
            assertEquals(knownByte, destBytes[i]);
        }
    }

    @Test
    public void testGetChar() {
        ByteBuffer bb = ByteBuffer.allocateDirect(1024);
        CharBuffer cb = bb.asCharBuffer();
        String string = "My really silly character sequence";
        cb.append(string);

        // Avoid stepping on random
        final long address = GIGABYTE * 8;
        assertTrue(RANDOM_BYTES_ADDRESS != address);

        testBufferHelper.addBuffer(address, bb);
        char[] stringChars = string.toCharArray();
        for (int i = 0; i < stringChars.length; i++) {
            assertEquals(stringChars[i], testBufferHelper.getChar(address + (i * Character.SIZE / Byte.SIZE)));
        }
    }

    @Test
    public void testGetDouble() {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        DoubleBuffer db = bb.asDoubleBuffer();
        double[] doubles = { Double.NaN,
                             Double.NEGATIVE_INFINITY,
                             Double.POSITIVE_INFINITY,
                             Double.MAX_VALUE,
                             Double.MIN_NORMAL,
                             Double.MIN_VALUE,
                             1.0, 1.5, 1.75, 2.0 };
        db.put(doubles);

        // Avoid stepping on random
        final long address = GIGABYTE * 8;
        assertTrue(RANDOM_BYTES_ADDRESS != address);

        testBufferHelper.addBuffer(address, bb);
        for (int i = 0; i < doubles.length; i++) {
            assertEquals(doubles[i], testBufferHelper.getDouble(address + (i * Double.SIZE / Byte.SIZE)), 0.000001);
        }
    }

    @Test
    public void testGetFloat() {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        FloatBuffer fb = bb.asFloatBuffer();
        float[] floats = { Float.NaN,
                           Float.NEGATIVE_INFINITY,
                           Float.POSITIVE_INFINITY,
                           Float.MAX_VALUE,
                           Float.MIN_NORMAL,
                           Float.MIN_VALUE,
                           1.0f, 1.5f, 1.75f, 2.0f };
        fb.put(floats);

        // Avoid stepping on random
        final long address = GIGABYTE * 8L;
        assertTrue(RANDOM_BYTES_ADDRESS != address);

        testBufferHelper.addBuffer(address, bb);
        for (int i = 0; i < floats.length; i++) {
            assertEquals(floats[i], testBufferHelper.getFloat(address + (i * Float.SIZE / Byte.SIZE)), 0.000001);
        }
    }

    @Test
    public void testGetInt() {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        IntBuffer ib = bb.asIntBuffer();
        int[] ints = { Integer.MAX_VALUE,
                       Integer.MIN_VALUE,
                       0, 1, 2, 3, 4, 5 };
        ib.put(ints);

        // Avoid stepping on random
        final long address = GIGABYTE * 8L;
        assertTrue(RANDOM_BYTES_ADDRESS != address);

        testBufferHelper.addBuffer(address, bb);
        for (int i = 0; i < ints.length; i++) {
            assertEquals(ints[i], testBufferHelper.getInt(address + (i * Integer.SIZE / Byte.SIZE)));
        }
    }

    @Test
    public void testGetLong1() {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        LongBuffer lb = bb.asLongBuffer();
        long[] longs = { Long.MAX_VALUE,
                         Long.MIN_VALUE,
                         Integer.MAX_VALUE,
                         Integer.MIN_VALUE,
                         0, 1, 2, 3, 4, 5 };
        lb.put(longs);

        // Avoid stepping on random
        final long address = GIGABYTE * 8L;
        assertTrue(RANDOM_BYTES_ADDRESS != address);

        testBufferHelper.addBuffer(address, bb);
        for (int i = 0; i < longs.length; i++) {
            assertEquals(longs[i], testBufferHelper.getLong(address + (i * Long.SIZE / Byte.SIZE)));
        }
    }

    @Test
    public void testGetShort() {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        ShortBuffer sb = bb.asShortBuffer();
        short[] shorts = { Short.MAX_VALUE,
                           Short.MIN_VALUE,
                           Byte.MAX_VALUE,
                           Byte.MIN_VALUE,
                           0, 1, 2, 3, 4, 5 };
        sb.put(shorts);

        // Avoid stepping on random
        final long address = GIGABYTE * 8L;
        assertTrue(RANDOM_BYTES_ADDRESS != address);

        testBufferHelper.addBuffer(address, bb);
        for (int i = 0; i < shorts.length; i++) {
            assertEquals(shorts[i], testBufferHelper.getShort(address + (i * Short.SIZE / Byte.SIZE)));
        }
    }

    @Test
    public void testGetSegment() {
        ByteBuffer bufferAt16 = ByteBuffer.allocate(1024);
        ByteBuffer bufferAt17 = ByteBuffer.allocate(1024);

        testBufferHelper.addBuffer(16L * GIGABYTE, bufferAt16);
        testBufferHelper.addBuffer(17L * GIGABYTE, bufferAt17);

        ByteBuffer bb = null;

        bb = testBufferHelper.getSegment(16L * GIGABYTE);
        assertNotNull(bb);
        assertNotNull(testBufferHelper.getRecentBuffer());
        assertEquals(16L * GIGABYTE, testBufferHelper.getRecentBuffer().base);
        assertSame(bb, testBufferHelper.getRecentBuffer().buffer);

        bb = testBufferHelper.getSegment(17L * GIGABYTE - 1L);
        assertNotNull(bb);
        assertEquals(16L * GIGABYTE, testBufferHelper.getRecentBuffer().base);

        bb = testBufferHelper.getSegment(17L * GIGABYTE + 1);
        assertNotNull(bb);
        assertEquals(17L * GIGABYTE, testBufferHelper.getRecentBuffer().base);
        assertSame(bb, testBufferHelper.getRecentBuffer().buffer);
    }

    @Test
    public void testGetSegmentOffset() {
        for (int i = 0; i < 50; i++) {
            assertEquals(i, testBufferHelper.getSegmentOffset(GIGABYTE + i));
        }
    }

}