/* ============================================================================
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * ============================================================================
 */
package testjms.web.impl;

import org.junit.Assert;
import testjms.web.JmsBytesMessageTests;

import javax.jms.BytesMessage;

public class JmsBytesMessageTestsImpl extends JmsTestsBase implements JmsBytesMessageTests {
    private static final byte[] testBytes = { Byte.MIN_VALUE, -5, 3, Byte.MAX_VALUE };
    private static final char[] testChars = { Character.MIN_VALUE, '\0', '£', Character.MAX_VALUE };
    private static final double[] testDoubles = { Double.NEGATIVE_INFINITY, Double.MIN_VALUE, Double.NaN, -5.0D, 3.0D, Double.MAX_VALUE, Double.POSITIVE_INFINITY };
    private static final float[] testFloats = { Float.NEGATIVE_INFINITY, Float.MIN_VALUE, Float.NaN, -5.0F, 3.0F, Float.MAX_VALUE, Float.POSITIVE_INFINITY };
    private static final int[] testInts = { Integer.MIN_VALUE, -5, 3, Integer.MAX_VALUE };
    private static final long[] testLongs = { Long.MIN_VALUE, -5L, 3L, Long.MAX_VALUE };
    private static final short[] testShorts = { Short.MIN_VALUE, -5, 3, Short.MAX_VALUE };
    private static final int[] testCodePoints = { Character.MIN_CODE_POINT, '\0', '£', Character.MIN_SUPPLEMENTARY_CODE_POINT, (Character.MAX_CODE_POINT + Character.MIN_SUPPLEMENTARY_CODE_POINT) / 2, Character.MAX_CODE_POINT };
    private static final String testString;

    static {
        final StringBuilder sb = new StringBuilder(2 * testCodePoints.length);
        for (int cp: testCodePoints) {
            sb.append(Character.toChars(cp));
        }
        testString = sb.toString();
    }

    public JmsBytesMessageTestsImpl(ConnectionFactoryType cfType) {
        super(cfType);
    }

    @Override
    public void test_writeByte() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            for (byte b: testBytes) {
                final BytesMessage m = ts.session.createBytesMessage();
                m.writeByte(b);
                final BytesMessage copy = ts.sendAndReceive(m, BytesMessage.class, ts.queue);
                final byte r = copy.readByte();
                Assert.assertEquals(b, r);
            }
        }
    }

    @Override
    public void test_writeBytes() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final BytesMessage m = ts.session.createBytesMessage();
            m.writeBytes(testBytes);
            final BytesMessage copy = ts.sendAndReceive(m, BytesMessage.class, ts.queue);
            byte[] ra = new byte[testBytes.length];
            final int bytesRead = copy.readBytes(ra);
            Assert.assertEquals(testBytes.length, bytesRead);
            Assert.assertArrayEquals(testBytes, ra);
        }
    }

    @Override
    public void test_writeChar() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            for (char c: testChars) {
                final BytesMessage m = ts.session.createBytesMessage();
                m.writeChar(c);
                final BytesMessage copy = ts.sendAndReceive(m, BytesMessage.class, ts.queue);
                final char r = copy.readChar();
                Assert.assertEquals(c, r);
            }
        }
    }

    @Override
    public void test_writeDouble() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            for (double d: testDoubles) {
                final BytesMessage m = ts.session.createBytesMessage();
                m.writeDouble(d);
                final BytesMessage copy = ts.sendAndReceive(m, BytesMessage.class, ts.queue);
                final double r = copy.readDouble();
                Assert.assertEquals(d, r, 0.0);
            }
        }
    }

    @Override
    public void test_writeFloat() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            for (float f: testFloats) {
                final BytesMessage m = ts.session.createBytesMessage();
                m.writeFloat(f);
                final BytesMessage copy = ts.sendAndReceive(m, BytesMessage.class, ts.queue);
                final float r = copy.readFloat();
                Assert.assertEquals(f, r, 0.0);
            }
        }
    }

    @Override
    public void test_writeInt() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            for (int i: testInts) {
                final BytesMessage m = ts.session.createBytesMessage();
                m.writeInt(i);
                final BytesMessage copy = ts.sendAndReceive(m, BytesMessage.class, ts.queue);
                final int r = copy.readInt();
                Assert.assertEquals(i, r);
            }
        }
    }

    @Override
    public void test_writeLong() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            for (long l: testLongs) {
                final BytesMessage m = ts.session.createBytesMessage();
                m.writeLong(l);
                final BytesMessage copy = ts.sendAndReceive(m, BytesMessage.class, ts.queue);
                final long r = copy.readLong();
                Assert.assertEquals(l, r);
            }
        }
    }

    @Override
    public void test_writeShort() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            for (short s: testShorts) {
                final BytesMessage m = ts.session.createBytesMessage();
                m.writeShort(s);
                final BytesMessage copy = ts.sendAndReceive(m, BytesMessage.class, ts.queue);
                final short r = copy.readShort();
                Assert.assertEquals(s, r);
            }
        }
    }

    private static String toHex(String s) {
        final StringBuilder sb = new StringBuilder(2 * s.length());
        for (char c: s.toCharArray()) {
            sb.append(String.format("%04x ", (int)c));
        }
        return sb.toString();
    }

    @Override
    public void test_writeUTF() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final BytesMessage m = ts.session.createBytesMessage();
            m.writeUTF(testString);
            final BytesMessage copy = ts.sendAndReceive(m, BytesMessage.class, ts.queue);
            final String r = copy.readUTF();
            if (!r.equals(testString)) {
                Assert.fail(String.format("%n%s%nCopy:%n%s%ndoes not equal original:%n%s%n%1$s%n", "#########", toHex(r), toHex(testString)));
            }
        }
    }
}
