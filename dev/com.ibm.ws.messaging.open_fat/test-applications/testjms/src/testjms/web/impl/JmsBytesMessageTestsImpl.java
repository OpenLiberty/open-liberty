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
import testjms.web.util.OutputHelper;

import javax.jms.BytesMessage;

public class JmsBytesMessageTestsImpl extends JmsTestsBase implements JmsBytesMessageTests {

    public JmsBytesMessageTestsImpl(ConnectionFactoryType cfType) {
        super(cfType);
    }

    @Override
    public void testBytesMessage_writeByte() throws Exception {
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
    public void testBytesMessage_writeBytes() throws Exception {
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
    public void testBytesMessage_writeChar() throws Exception {
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
    public void testBytesMessage_writeDouble() throws Exception {
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
    public void testBytesMessage_writeFloat() throws Exception {
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
    public void testBytesMessage_writeInt() throws Exception {
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
    public void testBytesMessage_writeLong() throws Exception {
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
    public void testBytesMessage_writeShort() throws Exception {
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

    @Override
    public void testBytesMessage_writeUTF() throws Exception {
        try (final JmsTestFramework ts = createTestFramework()) {
            final BytesMessage m = ts.session.createBytesMessage();
            m.writeUTF(testString);
            final BytesMessage copy = ts.sendAndReceive(m, BytesMessage.class, ts.queue);
            final String r = copy.readUTF();
            if (!r.equals(testString)) {
                Assert.fail(OutputHelper.comparisonFailureDescription(testString, r));
            }
        }
    }
}
