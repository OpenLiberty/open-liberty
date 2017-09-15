/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bytebuffer.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.bytebuffer.internal.WsByteBufferPoolManagerImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

/**
 * Test the WsByteBuffer interface.
 */
public class ByteBufferTest {
    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    /**
     * Test the various pooled buffers.
     */
    @Test
    public void testPooledBuffers() {
        try {
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(1024);
            runTests(buffer, false, 1024);
            assertEquals(WsByteBuffer.TYPE_WsByteBuffer, buffer.getType());
            buffer.release();
            buffer = ChannelFrameworkFactory.getBufferManager().allocateDirect(1024);
            runTests(buffer, true, 1024);
            assertEquals(WsByteBuffer.TYPE_WsByteBuffer, buffer.getType());
            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testPooledBuffers", t);
        }
    }

    /**
     * Test a non-pooled buffer.
     */
    @Test
    public void testNonPooledBuffer() {
        try {
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().wrap(new byte[8192]);
            assertEquals(WsByteBuffer.TYPE_WsByteBuffer, buffer.getType());
            runTests(buffer, false, 8192);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testNonPooledBuffer", t);
        }
    }

    /**
     * Test the refcount impl.
     */
    @Test
    public void testRefCountBuffer() {
        try {
            WsByteBufferPoolManagerImpl mgr = (WsByteBufferPoolManagerImpl) ChannelFrameworkFactory.getBufferManager();
            WsByteBuffer buffer = mgr.wrap(ByteBuffer.allocate(1024), true);
            assertEquals(WsByteBuffer.TYPE_WsByteBuffer, buffer.getType());
            runTests(buffer, false, 1024);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testRefCountBuffer", t);
        }
    }

    /**
     * Test the FileChannel wrapping buffer.
     */
    @Test
    public void testFCBuffer() {
        FileChannel fc = null;
        String sep = File.separator;
        try {
            File f = new File("test" + sep + "testdata");
            fc = new RandomAccessFile(f, "r").getChannel();
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocateFileChannelBuffer(fc);
            assertEquals(WsByteBuffer.TYPE_FCWsByteBuffer, buffer.getType());
            assertEquals(fc.size(), buffer.capacity());
            assertEquals(0, buffer.position());
            assertEquals(fc.size(), buffer.limit());
            assertEquals(fc.size(), buffer.remaining());
            assertEquals(WsByteBuffer.STATUS_TRANSFER_TO, buffer.getStatus());

            buffer.position(1);
            buffer.limit(5);
            assertEquals(WsByteBuffer.STATUS_TRANSFER_TO, buffer.getStatus());
            assertEquals(1, buffer.position());
            assertEquals(5, buffer.limit());
            assertEquals(4, buffer.remaining());

            // now make a call that converts from filechannel to regular
            // Note: source is a read-only FC so make sure the buffer matchs
            buffer.get();
            assertTrue(buffer.isReadOnly());
            assertTrue(buffer.getReadOnly());
            assertEquals(WsByteBuffer.STATUS_BUFFER, buffer.getStatus());
            try {
                buffer.putChar('c');
                throw new Exception("Put allowed on read-only buffer");
            } catch (ReadOnlyBufferException robe) {
                // expected failure
            }
            fc.close();
            fc = null;

            // make a read-write FC and check the conversion again
            fc = new RandomAccessFile(f, "rw").getChannel();
            buffer = ChannelFrameworkFactory.getBufferManager().allocateFileChannelBuffer(fc);
            buffer.get();
            assertFalse(buffer.isReadOnly());
            assertFalse(buffer.getReadOnly());
            buffer.putChar('c');
            fc.close();
            fc = null;
        } catch (Throwable t) {
            if (null != fc) {
                try {
                    fc.close();
                    fc = null;
                } catch (Exception e) {
                    // do nothing
                }
            }
            outputMgr.failWithThrowable("testFCBuffer", t);
        }
    }

    /**
     * Run all the API tests against the input buffer.
     * 
     * @param buffer
     * @param bDirect
     * @param size
     * @throws Exception
     */
    public void runTests(WsByteBuffer buffer, boolean bDirect, int size) throws Exception {
        // test basic boolean flags
        assertEquals(Boolean.valueOf(bDirect), Boolean.valueOf(buffer.isDirect()));
        buffer.setReadOnly(true);
        assertTrue(buffer.getReadOnly());
        buffer.setReadOnly(false);
        assertFalse(buffer.getReadOnly());
        // isReadOnly is the underlying ByteBuffer and should always be false
        assertFalse(buffer.isReadOnly());

        // test position/limit values
        assertEquals(size, buffer.capacity());
        buffer.position(0);
        assertEquals(0, buffer.position());
        buffer.limit(size);
        assertEquals(size, buffer.limit());
        assertEquals(size, buffer.remaining());
        assertTrue(buffer.hasRemaining());
        buffer.position(size);
        assertFalse(buffer.hasRemaining());

        // test backing array apis
        if (bDirect) {
            assertFalse(buffer.hasArray());
            try {
                buffer.array();
                throw new Exception("Should have failed");
            } catch (UnsupportedOperationException e) {
                // expected failure
            }
            try {
                buffer.arrayOffset();
                throw new Exception("Should have failed");
            } catch (UnsupportedOperationException e) {
                // expected failure
            }
        } else {
            assertTrue(buffer.hasArray());
            assertTrue(null != buffer.array());
            assertEquals(0, buffer.arrayOffset());
        }

        // test mark/reset
        buffer.limit(size);
        buffer.position(size - 5);
        buffer.mark();
        buffer.position(size - 2);
        buffer.reset();
        assertEquals((size - 5), buffer.position());
        buffer.position(0); // moving pos prior to mark deletes it
        try {
            buffer.reset();
            throw new Exception("Should have failed when mark not set");
        } catch (InvalidMarkException e) {
            // expected failure
        }

        // test clear
        buffer.limit(size - 1);
        buffer.position(size - 3);
        buffer.mark();
        buffer.clear();
        assertEquals(0, buffer.position());
        assertEquals(size, buffer.limit());
        try {
            buffer.reset();
            throw new Exception("Clear should have discarded mark");
        } catch (InvalidMarkException e) {
            // expected failure
        }

        // test flip
        buffer.limit(size);
        buffer.position(size - 10);
        buffer.flip();
        assertEquals(0, buffer.position());
        assertEquals((size - 10), buffer.limit());

        // test duplicate/get(index)/put(index)/release
        WsByteBuffer buffer2 = buffer.duplicate();
        buffer.position(5);
        buffer2.position(10);
        assertEquals(5, buffer.position());
        assertEquals(10, buffer2.position());
        buffer2.put(5, (byte) 'd');
        buffer.put((byte) 'c');
        assertEquals('c', buffer2.get(5));
        // test compareTo
        buffer.position(5);
        buffer.limit(6);
        buffer2.position(5);
        buffer2.limit(6);
        assertEquals(0, buffer.compareTo(buffer2));
        buffer2.release();

        // test compact
        buffer.put(0, (byte) 'a');
        buffer.position(5);
        buffer.put(5, (byte) 'c');
        buffer.limit(6);
        buffer.compact();
        assertEquals(1, buffer.position());
        assertEquals(size, buffer.limit());
        assertEquals('c', buffer.get(0));

        // test single byte usage
        buffer.clear();
        buffer.put((byte) 'q');
        buffer.flip();
        assertEquals('q', buffer.get());

        // test byte[] usage
        buffer.clear();
        buffer.put("DEADBEEF".getBytes());
        buffer.flip();
        byte[] output = new byte[8];
        buffer.get(output);
        assertEquals("DEADBEEF", new String(output));

        // test byte[] with offset usage
        buffer.clear();
        buffer.put("CATTLE".getBytes(), 1, 3);
        buffer.flip();
        output = new byte[5];
        buffer.get(output, 1, 3);
        assertEquals(0, output[0]);
        assertEquals("ATT", new String(output, 1, 3));

        // test single char usage
        buffer.clear();
        buffer.putChar('X');
        buffer.flip();
        assertEquals('X', buffer.getChar());

        // test char[] usage
        buffer.clear();
        char[] inchars = new char[8];
        "DEADBEEF".getChars(0, 8, inchars, 0);
        buffer.putChar(inchars);
        buffer.flip();
        assertEquals('D', buffer.getChar());
        assertEquals('E', buffer.getChar());
        assertEquals('A', buffer.getChar());
        assertEquals('D', buffer.getChar());
        assertEquals('B', buffer.getChar());
        assertEquals('E', buffer.getChar());
        assertEquals('E', buffer.getChar());
        assertEquals('F', buffer.getChar(14)); // chars are 2 bytes

        // test char[] with offset usage
        buffer.clear();
        inchars = new char[6];
        "CATTLE".getChars(0, 6, inchars, 0);
        buffer.putChar(inchars, 2, 4);
        buffer.putChar(6, 'Q');
        buffer.flip();
        assertEquals('T', buffer.getChar());
        assertEquals('T', buffer.getChar());
        assertEquals('L', buffer.getChar());
        assertEquals('Q', buffer.getChar());

        // test the int usage
        buffer.clear();
        buffer.putInt(7);
        buffer.flip();
        assertEquals(7, buffer.getInt());
        buffer.clear();
        buffer.putInt(1, 9);
        buffer.position(1);
        buffer.limit(5);
        assertEquals(9, buffer.getInt(1));

        // test double usage
        buffer.clear();
        buffer.putDouble(15.2);
        buffer.flip();
        assertEquals(15.2, buffer.getDouble(), 0);
        buffer.clear();
        buffer.putDouble(2, 678.567);
        buffer.position(2);
        buffer.limit(10);
        assertEquals(678.567, buffer.getDouble(2), 0);

        // test Floats
        buffer.clear();
        buffer.putFloat(new Float(100.09).floatValue());
        buffer.flip();
        assertEquals((float) 100.09, buffer.getFloat(), 0);
        buffer.clear();
        buffer.putFloat(4, (float) 9.001);
        buffer.position(4);
        buffer.limit(8);
        assertEquals((float) 9.001, buffer.getFloat(4), 0);

        // test Long values
        buffer.clear();
        buffer.putLong(Integer.MAX_VALUE + 20);
        buffer.flip();
        assertEquals((Integer.MAX_VALUE + 20), buffer.getLong());
        buffer.clear();
        buffer.putLong(10, Integer.MAX_VALUE + 123345);
        buffer.position(10);
        buffer.limit(18);
        assertEquals((Integer.MAX_VALUE + 123345), buffer.getLong(10));

        // test Short values
        buffer.clear();
        buffer.putShort((short) 10);
        buffer.flip();
        assertEquals((short) 10, buffer.getShort());
        buffer.clear();
        buffer.putShort(5, (short) 200);
        buffer.position(5);
        buffer.limit(7);
        assertEquals((short) 200, buffer.getShort(5));

        // test byte order
        buffer.clear();
        ByteOrder currentorder = buffer.order();
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(ByteOrder.LITTLE_ENDIAN, buffer.order());
        buffer.order(ByteOrder.BIG_ENDIAN);
        assertEquals(ByteOrder.BIG_ENDIAN, buffer.order());
        buffer.order(currentorder);

        // test the getWrappedByteBuffer apis
        buffer.position(4);
        buffer.limit(16);
        ByteBuffer bb = buffer.getWrappedByteBuffer();
        assertEquals(4, bb.position());
        assertEquals(16, bb.limit());
        ByteBuffer bb2 = buffer.getWrappedByteBufferNonSafe();
        assertEquals(0, bb2.compareTo(bb));

        // test put ByteBuffer
        buffer.clear();
        bb = ByteBuffer.wrap("test".getBytes());
        buffer.put(bb);
        assertEquals(4, buffer.position());
        assertEquals(size, buffer.limit());
        buffer.flip();
        assertEquals('t', buffer.get());
        assertEquals('e', buffer.get());
        assertEquals('s', buffer.get());
        assertEquals('t', buffer.get());

        // test put WsByteBuffer
        buffer.clear();
        buffer2 = ChannelFrameworkFactory.getBufferManager().wrap("pbx".getBytes());
        buffer.put(buffer2);
        assertEquals(3, buffer.position());
        assertEquals(size, buffer.limit());
        buffer.flip();
        assertEquals('p', buffer.get());
        assertEquals('b', buffer.get());
        assertEquals('x', buffer.get());

        // test put WsByteBuffer list
        buffer.clear();
        buffer2 = ChannelFrameworkFactory.getBufferManager().wrap("calico".getBytes());
        WsByteBuffer buffer3 = ChannelFrameworkFactory.getBufferManager().wrap("quasar".getBytes());
        buffer.put(new WsByteBuffer[] { buffer2, buffer3 });
        assertEquals(12, buffer.position());
        assertEquals(size, buffer.limit());
        buffer.flip();
        output = new byte[12];
        buffer.get(output);
        assertEquals("calicoquasar", new String(output));

        // test put String
        buffer.clear();
        buffer.putString("shadow");
        assertEquals('s', buffer.get(0));
        assertEquals('h', buffer.get(1));
        assertEquals('a', buffer.get(2));
        assertEquals('d', buffer.get(3));
        assertEquals('o', buffer.get(4));
        assertEquals('w', buffer.get(5));

        // test removeFromLeak -- just make sure no errors appear
        buffer.removeFromLeakDetection();

        // test rewind
        buffer.clear();
        buffer.position(20);
        buffer.mark();
        buffer.rewind();
        assertEquals(0, buffer.position());
        try {
            buffer.reset();
        } catch (InvalidMarkException e) {
            // expected failure
        }

        // test slice
        buffer.clear();
        buffer.putString("bickel");
        buffer.position(1);
        buffer.limit(4);
        buffer2 = buffer.slice();
        assertEquals(0, buffer2.position());
        assertEquals(3, buffer2.limit());
        assertEquals(3, buffer2.capacity());
        assertEquals('i', buffer2.get(0));
        assertEquals('c', buffer2.get(1));
        assertEquals('k', buffer2.get(2));
        buffer2.put(0, (byte) 'q');
        assertEquals('q', buffer.get(1));
        buffer2.release();

        // test the FC buffer status apis
        buffer.setStatus(WsByteBuffer.STATUS_BUFFER);
        assertEquals(WsByteBuffer.STATUS_BUFFER, buffer.getStatus());
        buffer.setStatus(WsByteBuffer.STATUS_TRANSFER_TO);
        assertEquals(WsByteBuffer.STATUS_TRANSFER_TO, buffer.getStatus());

        // test the buffer-action setter
        buffer.setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL);
        buffer.setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_ALL_FINAL);
        buffer.setBufferAction(WsByteBuffer.BUFFER_MGMT_COPY_WHEN_NEEDED);

        // test serialization
        buffer.clear();
        buffer.putString("quality");
        buffer.flip();
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeUnshared(buffer);
            oo.flush();

            byte[] serializedData = bo.toByteArray();
            ByteArrayInputStream bi = new ByteArrayInputStream(serializedData);
            ObjectInputStream oi = new ObjectInputStream(bi);
            buffer2 = (WsByteBuffer) oi.readObject();
            assertEquals(Boolean.valueOf(buffer.isDirect()), Boolean.valueOf(buffer2.isDirect()));
            assertEquals(Boolean.valueOf(buffer.isReadOnly()), Boolean.valueOf(buffer2.isReadOnly()));
            assertEquals(buffer.position(), buffer2.position());
            assertEquals(buffer.limit(), buffer2.limit());
            assertEquals(0, buffer.compareTo(buffer2));
            bi.close();
            oi.close();
            bo.close();
            oo.close();
        } catch (Exception exp) {
            exp.printStackTrace();
            throw new Exception("Failed on serialization: " + exp.getMessage());
        }
    }

    /**
     * Test indirect slices and backing array usage paths.
     */
    @Test
    public void testIndirectSlices() {
        try {
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            buffer.clear();
            for (int i = 0; i < 8192; i++) {
                buffer.put(((byte) 'a'));
            }
            assertTrue(buffer.hasArray());
            byte[] array1 = buffer.array();
            assertNotNull(array1);
            assertEquals(0, buffer.arrayOffset());
            buffer.position(100);
            buffer.put((byte) 'b');
            // slice from 100..8192
            buffer.position(100);
            WsByteBuffer buffer2 = buffer.slice();
            assertTrue(buffer2.hasArray());
            assertEquals(100, buffer2.arrayOffset());
            byte[] array2 = buffer2.array();
            assertNotNull(array2);
            assertEquals(array1, array2);
            assertEquals('b', buffer2.get());
            assertEquals('a', array2[0]);
            assertEquals('b', array2[0 + buffer2.arrayOffset()]);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testIndirectSlices", t);
        }
    }

    /**
     * Test bytebuffer equals() method.
     */
    @Test
    public void testEquals() {
        try {
            WsByteBufferPoolManager mgr = ChannelFrameworkFactory.getBufferManager();
            WsByteBuffer bb = mgr.allocateDirect(8192);
            System.out.println("buffer1=" + bb);
            WsByteBuffer bb2 = mgr.allocateDirect(8192);
            System.out.println("buffer2=" + bb2);
            assertTrue(bb.equals(bb));
            assertFalse(bb.equals(bb2));

            // test lists of buffers, uses equals() for finding them
            List<WsByteBuffer> list = new LinkedList<WsByteBuffer>();
            list.add(bb);
            list.add(bb2);
            assertTrue(list.contains(bb));
            assertTrue(list.contains(bb2));

            assertTrue(list.remove(bb));
            assertFalse(list.contains(bb));
            assertTrue(list.contains(bb2));
            assertTrue(1 == list.size());

            assertFalse(list.remove(bb));

            assertTrue(list.remove(bb2));
            assertTrue(0 == list.size());
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testEquals", t);
        }
    }
}
