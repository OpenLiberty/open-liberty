/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.BufferUnderflowException;
import java.util.List;

import org.junit.Test;

import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 * Unit tests.
 */
public class ByteBufferVectorTest {

    /**
     * Test CTOR with byte[] parms.
     */
    @Test
    public void testByteArrayParms() {

        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] baempty = new byte[0];
        byte[] banull = null;
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0);
        bbv.append(ba1);
        bbv.append(baempty);
        bbv.append(banull);
        bbv.append(ba2);
        assertEquals(ba0.length + ba1.length + ba2.length, bbv.getLength());

        byte[] ba3 = new byte[bbv.getLength()];
        bbv.get(0, ba3);

        assertEquals("abcdefghijkl", new String(ba3));
    }

    /**
     * Test CTOR with byte[] parms.
     */
    @Test
    public void testByteBufferVectorParms() {

        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] baempty = new byte[0];
        byte[] banull = null;
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0);
        bbv = new ByteBufferVector(bbv, ba1);
        bbv = new ByteBufferVector(bbv, baempty);
        bbv = new ByteBufferVector(bbv, banull);
        bbv = new ByteBufferVector(bbv, ba2);
        assertEquals(ba0.length + ba1.length + ba2.length, bbv.getLength());

        byte[] ba3 = new byte[bbv.getLength()];
        bbv.get(0, ba3);

        assertEquals("abcdefghijkl", new String(ba3));
    }

    /**
     *
     */
    @Test
    public void testGet() {

        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0);
        bbv.append(ba1);
        bbv.append(ba2);

        assertEquals((byte) 'a', bbv.get(0));
        assertEquals((byte) 'b', bbv.get(1));
        assertEquals((byte) 'f', bbv.get(5));
        assertEquals((byte) 'c', bbv.get(2));
        assertEquals((byte) 'a', bbv.get(0));
        assertEquals((byte) 'd', bbv.get(3));
    }

    /**
     * Try an absolute get from beyond the limit.
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetOverflow() {

        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        bbv.get(bbv.getLength());
    }

    /**
     *
     */
    @Test
    public void testPut() {
        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0);
        bbv.append(ba1);
        bbv.append(ba2);

        int length = bbv.getLength();

        bbv.put(0, (byte) 'z');
        bbv.put(6, (byte) 'z');
        bbv.put(10, (byte) 'z');

        String s = bbv.getString(0, length, CodepageUtils.ASCII);
        assertEquals(s, "zbcdefzhijzl");
    }

    /**
     *
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutUnderflow() {
        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0);
        bbv.append(ba1);
        bbv.append(ba2);

        bbv.put(12, (byte) 'z');
    }

    /**
     *
     */
    @Test
    public void testGetByteArray() {

        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        byte[] ba3 = new byte[4];
        bbv.get(0, ba3);
        assertEquals("abcd", new String(ba3));

        bbv.get(4, ba3);
        assertEquals("efgh", new String(ba3));

        bbv.get(8, ba3);
        assertEquals("ijkl", new String(ba3));
    }

    /**
     *
     */
    @Test(expected = BufferUnderflowException.class)
    public void testGetByteArrayUnderflow() {

        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        byte[] ba3 = new byte[bbv.getLength()];
        bbv.get(3, ba3); // Trying to read too much!
    }

    /**
     *
     */
    @Test
    public void testGetShort() {

        byte[] ba0 = new byte[] { 0x01, (byte) 0xf2, 0x03, 0x04, 0x05 };
        byte[] ba1 = new byte[] { 0x06, 0x07, 0x08 };
        byte[] ba2 = new byte[] { 0x09, 0x0a };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        assertEquals(0x01f2, bbv.getUnsignedShort(0));
        assertEquals(0xf203, bbv.getUnsignedShort(1));
        assertEquals(0x0506, bbv.getUnsignedShort(4));
        assertEquals(0x0708, bbv.getUnsignedShort(6));
        assertEquals(0x0809, bbv.getUnsignedShort(7));
        assertEquals(0x090a, bbv.getUnsignedShort(8));
    }

    /**
     *
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetShortOverflow() {

        byte[] ba0 = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        byte[] ba1 = new byte[] { 0x06, 0x07, 0x08 };
        byte[] ba2 = new byte[] { 0x09, 0x0a };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        bbv.getUnsignedShort(9);
    }

    /**
     *
     */
    @Test
    public void testGetInt() {

        byte[] ba0 = new byte[] { 0x01, 0x02, (byte) 0xf3, 0x04, 0x05 };
        byte[] ba1 = new byte[] { 0x06, 0x07, 0x08 };
        byte[] ba2 = new byte[] { 0x09, 0x0a };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        assertEquals(0x0102f304, bbv.getInt(0));
        assertEquals(0x02f30405, bbv.getInt(1));
        assertEquals(0xf3040506, bbv.getInt(2));
        assertEquals(0x05060708, bbv.getInt(4));
        assertEquals(0x0708090a, bbv.getInt(6));
    }

    /**
     *
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetIntOverflow() {

        byte[] ba0 = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        byte[] ba1 = new byte[] { 0x06, 0x07, 0x08 };
        byte[] ba2 = new byte[] { 0x09, 0x0a };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        bbv.getInt(7);
    }

    /**
     *
     */
    @Test
    public void testGetLong() {

        byte[] ba0 = new byte[] { 0x01, 0x02, (byte) 0xf3, 0x04, 0x05 };
        byte[] ba1 = new byte[] { 0x06, 0x07, 0x08 };
        byte[] ba2 = new byte[] { 0x09, 0x0a };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        assertEquals(0x0102f30405060708L, bbv.getLong(0));
        assertEquals(0xf30405060708090aL, bbv.getLong(2));
    }

    /**
     *
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetLongOverflow() {

        byte[] ba0 = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        byte[] ba1 = new byte[] { 0x06, 0x07, 0x08 };
        byte[] ba2 = new byte[] { 0x09, 0x0a };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        bbv.getLong(3);
    }

    /**
     *
     */
    @Test
    public void testGetString() throws Exception {

        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        assertEquals("abcd", bbv.getString(0, 4, CodepageUtils.ASCII));
        assertEquals("efgh", bbv.getString(4, 4, CodepageUtils.ASCII));
        assertEquals("ijk", bbv.getString(8, 3, CodepageUtils.ASCII));
    }

    /**
     *
     */
    @Test
    public void testGetStringNullBytes() throws Exception {

        byte[] ba0 = new byte[] { 'a', 'b', 'c', 0, 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        assertEquals("abc", bbv.getString(0, 8, CodepageUtils.ASCII));
        assertEquals("", bbv.getString(3, 4, CodepageUtils.ASCII));
        assertEquals("ijk", bbv.getString(8, 3, CodepageUtils.ASCII));
    }

    /**
     *
     */
    @Test
    public void testGetStringEBCDIC() throws Exception {

        byte[] ba0 = new byte[] { (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84 }; // a,b,c,d
        byte[] ba1 = new byte[] { (byte) 0x85, (byte) 0x86 }; // e,f
        byte[] ba2 = new byte[] { (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84 }; // a,b,c,d

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        assertEquals("abcd", bbv.getString(0, 4, CodepageUtils.EBCDIC));
        assertEquals("cdefa", bbv.getString(2, 5, CodepageUtils.EBCDIC));
        assertEquals("cd", bbv.getString(8, 2, CodepageUtils.EBCDIC));
    }

    /**
     *
     */
    @Test(expected = BufferUnderflowException.class)
    public void testGetStringUnderflow() throws Exception {

        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        bbv.getString(2, bbv.getLength(), CodepageUtils.ASCII);
    }

    /**
     *
     */
    @Test
    public void testPutStringEbcdic() {

        byte[] ba0 = new byte[] { (byte) 0x81, (byte) 0x82, (byte) 0x83, (byte) 0x84 }; // a,b,c,d
        byte[] ba1 = new byte[] { (byte) 0x85, (byte) 0x86 }; // e,f
        byte[] ba2 = new byte[] { (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8a }; // g, h

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        bbv.putString(2, "aaaa", CodepageUtils.EBCDIC);

        byte[] ba3 = new byte[] { (byte) 0x82, (byte) 0x81, (byte) 0x81, (byte) 0x81, (byte) 0x81, (byte) 0x87 };
        byte[] ba4 = new byte[6];
        bbv.get(1, ba4);

        assertArrayEquals(ba3, ba4);
        assertEquals("abaaaag", bbv.getString(0, 7, CodepageUtils.EBCDIC));
    }

    /**
     *
     */
    @Test
    public void testPutStringAscii() {
        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);
        bbv.putString(2, "aaaa", CodepageUtils.ASCII);

        assertEquals("abaaaag", bbv.getString(0, 7, CodepageUtils.ASCII));
    }

    /**
     *
     */
    @Test
    public void testPutStringField() {
        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);
        bbv.putStringField(2, 2, "aaaa", CodepageUtils.ASCII); // only 2 a's should be put

        assertEquals("abaaefg", bbv.getString(0, 7, CodepageUtils.ASCII));
    }

    /**
     *
     */
    @Test
    public void testPutStringFieldPadded() {
        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);
        bbv.putStringFieldPadded(2, 4, "aa", CodepageUtils.ASCII); // should be padded out to 4 bytes

        assertEquals("abaa  g", bbv.getString(0, 7, CodepageUtils.ASCII));
    }

    /**
     *
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutStringOverflow() {
        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);
        bbv.putString(2, "abcdefghijkl", CodepageUtils.ASCII);
    }

    /**
     *
     */
    @Test
    public void testPutShort() {
        byte[] ba0 = new byte[] { 0x01, 0x02, (byte) 0xf3, 0x04, 0x05 };
        byte[] ba1 = new byte[] { 0x06, 0x07, 0x08 };
        byte[] ba2 = new byte[] { 0x09, 0x0a };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);
        bbv.putShort(4, (short) 0x1516);

        assertEquals(0x04151607, bbv.getInt(3));
        assertEquals(0x1516, bbv.getUnsignedShort(4));
    }

    /**
     *
     */
    @Test
    public void testShortAsBytes() {
        assertArrayEquals(new byte[] { 0x01, 0x02 }, ByteBufferVector.shortAsBytes((short) 0x0102));
    }

    /**
     *
     */
    @Test
    public void testToUnsignedShort() throws Exception {

        assertEquals(65535, ByteBufferVector.toUnsignedShort((short) 0xffff));
        assertEquals(65535, ByteBufferVector.toUnsignedShort((short) -1));

        assertEquals(65534, ByteBufferVector.toUnsignedShort((short) 0xfffe));
        assertEquals(65534, ByteBufferVector.toUnsignedShort((short) -2));

        assertEquals(32768, ByteBufferVector.toUnsignedShort((short) 0x8000));
        assertEquals(32768, ByteBufferVector.toUnsignedShort((short) -32768));
        assertEquals(32769, ByteBufferVector.toUnsignedShort((short) -32767));

        assertEquals(1, ByteBufferVector.toUnsignedShort((short) 1));
        assertEquals(0, ByteBufferVector.toUnsignedShort((short) 0));
        assertEquals(32767, ByteBufferVector.toUnsignedShort((short) 32767));
    }

    /**
     *
     */
    @Test
    public void testToByteArray() {

        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        byte[] ba3 = bbv.toByteArray();

        assertEquals("abcdefghijkl", new String(ba3));
    }

    /**
     *
     */
    @Test
    public void testPutByteArray() {
        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);
        bbv.put(4, new byte[] { 'x', 'y', 'z' });

        byte[] ba3 = new byte[4];
        bbv.get(0, ba3);
        assertEquals("abcd", new String(ba3));

        bbv.get(4, ba3);
        assertEquals("xyzh", new String(ba3));

        bbv.get(8, ba3);
        assertEquals("ijkl", new String(ba3));
    }

    /**
     *
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutByteArrayIndexOutOfBounds() {
        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);
        bbv.put(bbv.getLength() - 2, new byte[] { 'x', 'y', 'z' });
    }

    /**
     *
     */
    @Test
    public void testPutInt() {
        byte[] ba0 = new byte[] { 0x01, 0x02, (byte) 0xf3, 0x04, 0x05 };
        byte[] ba1 = new byte[] { 0x06, 0x07, 0x08 };
        byte[] ba2 = new byte[] { 0x09, 0x0a };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);
        bbv.putInt(4, 0x15161718);

        assertEquals(0x0102f304, bbv.getInt(0));
        assertEquals(0x02f30415, bbv.getInt(1));
        assertEquals(0xf3041516, bbv.getInt(2));
        assertEquals(0x15161718, bbv.getInt(4));
        assertEquals(0x1718090a, bbv.getInt(6));
    }

    /**
     *
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutIntIndexOutOfBounds() {
        byte[] ba0 = new byte[] { 0x01, 0x02, (byte) 0xf3, 0x04, 0x05 };
        byte[] ba1 = new byte[] { 0x06, 0x07, 0x08 };
        byte[] ba2 = new byte[] { 0x09, 0x0a };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);
        bbv.putInt(bbv.getLength() - 3, 0x15161718);
    }

    /**
     *
     */
    @Test
    public void testIntAsBytes() {

        assertArrayEquals(new byte[] { 0x01, 0x02, 0x03, 0x04 }, ByteBufferVector.intAsBytes(0x01020304));
    }

    /**
     *
     */
    @Test
    public void testAbsolutePutLong() {
        byte[] ba0 = new byte[] { 0x01, 0x02, (byte) 0xf3, 0x04, 0x05 };
        byte[] ba1 = new byte[] { 0x06, 0x07, 0x08 };
        byte[] ba2 = new byte[] { 0x09, 0x0a };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);
        bbv.putLong(1, 0x1213141516171819L);

        assertEquals(0x01121314, bbv.getInt(0));
        assertEquals(0x1213141516171819L, bbv.getLong(1));
    }

    /**
     *
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testAbsolutePutLongIndexOutOfBounds() {
        byte[] ba0 = new byte[] { 0x01, 0x02, (byte) 0xf3, 0x04, 0x05 };
        byte[] ba1 = new byte[] { 0x06, 0x07, 0x08 };
        byte[] ba2 = new byte[] { 0x09, 0x0a };

        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);
        bbv.putLong(bbv.getLength() - 7, 0x1213141516171819L);
    }

    /**
     *
     */
    @Test
    public void testLongAsBytes() {

        assertArrayEquals(new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 }, ByteBufferVector.longAsBytes(0x0102030405060708L));
    }

    /**
     *
     */
    @Test
    public void testSplit() {
        byte[] ba0 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
        byte[] ba1 = new byte[] { 'f', 'g' };
        byte[] ba2 = new byte[] { 'h', 'i', 'j', 'k', 'l' };
        ByteBufferVector bbv = new ByteBufferVector(ba0).append(ba1).append(ba2);

        List<ByteBufferVector> splitList = bbv.split(4);
        assertEquals(2, splitList.size());
        ByteBufferVector s1 = splitList.get(0);
        assertEquals(4, s1.getLength());
        assertEquals("abcd", s1.getString(0, 4, CodepageUtils.ASCII));
        ByteBufferVector s2 = splitList.get(1);
        assertEquals(8, s2.getLength());
        assertEquals("efghijkl", s2.getString(0, 8, CodepageUtils.ASCII));

        splitList = bbv.split(7);
        assertEquals(2, splitList.size());
        s1 = splitList.get(0);
        assertEquals(7, s1.getLength());
        assertEquals("abcdefg", s1.getString(0, 7, CodepageUtils.ASCII));
        s2 = splitList.get(1);
        assertEquals(5, s2.getLength());
        assertEquals("hijkl", s2.getString(0, 5, CodepageUtils.ASCII));
    }
}
