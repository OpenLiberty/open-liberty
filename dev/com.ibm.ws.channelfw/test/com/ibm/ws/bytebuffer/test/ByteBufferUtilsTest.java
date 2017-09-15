/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

/**
 * Test the bytebuffer utils class.
 */
public class ByteBufferUtilsTest {
    private static SharedOutputManager outputMgr;
    private static final String data = "test string";
    private static final String data2 = "secondary test data";

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

    private WsByteBuffer setupBuffer(int size, String content) {
        WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(size);
        buffer.put(content.getBytes());
        buffer.flip();
        return buffer;
    }

    private WsByteBuffer[] setupBuffers(int size, String[] content) {
        WsByteBuffer[] list = new WsByteBuffer[content.length];
        for (int i = 0; i < list.length; i++) {
            if (null != content[i]) {
                list[i] = ChannelFrameworkFactory.getBufferManager().allocate(size);
                list[i].put(content[i].getBytes());
                list[i].flip();
            } else {
                list[i] = null;
            }
        }
        return list;
    }

    private int[] getPositions(WsByteBuffer[] list) {
        int[] rc = new int[list.length];
        for (int i = 0; i < list.length; i++) {
            rc[i] = list[i].position();
        }
        return rc;
    }

    private int[] getLimits(WsByteBuffer[] list) {
        int[] rc = new int[list.length];
        for (int i = 0; i < list.length; i++) {
            rc[i] = list[i].limit();
        }
        return rc;
    }

    private void comparePositions(int[] pos, WsByteBuffer[] list) {
        assertNotNull(list);
        assertEquals(pos.length, list.length);
        for (int i = 0; i < list.length; i++) {
            assertEquals(pos[i], list[i].position());
        }
    }

    private void compareLimits(int[] limits, WsByteBuffer[] list) {
        assertNotNull(list);
        assertEquals(limits.length, list.length);
        for (int i = 0; i < list.length; i++) {
            assertEquals(limits[i], list[i].limit());
        }
    }

    private void releaseList(WsByteBuffer[] list) {
        for (int i = 0; i < list.length; i++) {
            if (null != list[i]) {
                list[i].release();
            }
        }
    }

    /**
     * Test WsByteBufferUtils.asByteArray(WsByteBuffer).
     */
    @Test
    public void test1() {
        try {
            // test null input
            assertNull(WsByteBufferUtils.asByteArray((WsByteBuffer) null));
            // test a valid buffer
            WsByteBuffer buffer = setupBuffer(1024, data);
            int pos = buffer.position();
            int lim = buffer.limit();
            byte[] testData = WsByteBufferUtils.asByteArray(buffer);
            assertEquals(pos, buffer.position());
            assertEquals(lim, buffer.limit());
            assertEquals(data, new String(testData));
            // test an empty buffer
            buffer.position(0);
            buffer.limit(0);
            assertNull(WsByteBufferUtils.asByteArray(buffer));
            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test1", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asByteArray(WsByteBuffer, int, int).
     */
    @Test
    public void test2() {
        try {
            // test null input
            assertNull(WsByteBufferUtils.asByteArray((WsByteBuffer) null, 0, 10));
            // test a valid buffer
            WsByteBuffer buffer = setupBuffer(1024, data);
            int pos = buffer.position();
            int lim = buffer.limit();
            byte[] testData = WsByteBufferUtils.asByteArray(buffer, 5, 10);
            assertNotNull(testData);
            assertEquals(5, testData.length);
            assertEquals(pos, buffer.position());
            assertEquals(lim, buffer.limit());
            assertEquals(data.substring(5, 10), new String(testData));
            // test an empty buffer
            assertNull(WsByteBufferUtils.asByteArray(buffer, 5, 5));
            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test2", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asByteArray(WsByteBuffer[]).
     */
    @Test
    public void test3() {
        try {
            // test null input
            assertNull(WsByteBufferUtils.asByteArray((WsByteBuffer[]) null));
            // test a completely empty list
            assertNull(WsByteBufferUtils.asByteArray(new WsByteBuffer[1]));
            // test a valid list
            WsByteBuffer[] list = setupBuffers(8192, new String[] { data, data2 });
            int[] pos = getPositions(list);
            int[] lim = getLimits(list);
            byte[] testData = WsByteBufferUtils.asByteArray(list);
            comparePositions(pos, list);
            compareLimits(lim, list);
            assertNotNull(testData);
            assertEquals((data.length() + data2.length()), testData.length);
            assertEquals(data, new String(testData, 0, data.length()));
            assertEquals(data2, new String(testData, data.length(), data2.length()));
            // test a list with null buffers
            list[1].release();
            list[1] = null;
            testData = WsByteBufferUtils.asByteArray(list);
            assertNotNull(testData);
            assertEquals(pos[0], list[0].position());
            assertEquals(lim[0], list[0].limit());
            assertEquals(data, new String(testData));
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test3", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asByteArray(WsByteBuffer[], int[], int[]).
     */
    @Test
    public void test4() {
        try {
            // test a null list
            assertNull(WsByteBufferUtils.asByteArray((WsByteBuffer[]) null, null, null));
            // test an empty list
            assertNull(WsByteBufferUtils.asByteArray(new WsByteBuffer[1], null, null));
            // test a valid list
            WsByteBuffer[] list = setupBuffers(8192, new String[] { data, data2 });
            int[] pos = getPositions(list);
            int[] lim = getLimits(list);
            int end1 = data.length() - 1;
            int end2 = data2.length() - 2;
            byte[] testData = WsByteBufferUtils.asByteArray(
                                                            list, new int[] { 0, 0 }, new int[] { end1, end2 });
            comparePositions(pos, list);
            compareLimits(lim, list);
            assertNotNull(testData);
            assertEquals((data.length() + data2.length() - 3), testData.length);
            assertEquals(data.substring(0, end1), new String(testData, 0, end1));
            assertEquals(data2.substring(0, end2), new String(testData, end1, end2));
            // test a list with null buffers
            list[1].release();
            list[1] = null;
            testData = WsByteBufferUtils.asByteArray(list, new int[] { 0, 0 }, new int[] { end1, end2 });
            assertNotNull(testData);
            assertEquals(pos[0], list[0].position());
            assertEquals(lim[0], list[0].limit());
            assertEquals(data.substring(0, end1), new String(testData));
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test4", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asInt(byte[]).
     */
    @Test
    public void test5() {
        try {
            // test null input
            assertEquals(-1, WsByteBufferUtils.asInt((byte[]) null));
            // test a valid positive number
            byte[] value = "5687".getBytes();
            assertEquals(5687, WsByteBufferUtils.asInt(value));
            // test a valid negative number
            value = "-1248435".getBytes();
            assertEquals(-1248435, WsByteBufferUtils.asInt(value));
            // test whitespace
            value = "   -1248435      ".getBytes();
            assertEquals(-1248435, WsByteBufferUtils.asInt(value));
            // test a non-number
            try {
                WsByteBufferUtils.asInt("11bogus".getBytes());
                fail("Should have failed on non-number");
            } catch (NumberFormatException nfe) {
                // expected failure
            }
            try {
                WsByteBufferUtils.asInt("  8192-123".getBytes());
                fail("Should have failed on non-number");
            } catch (NumberFormatException nfe) {
                // expected failure
            }
            // test empty space
            assertEquals(-1, WsByteBufferUtils.asInt("            ".getBytes()));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test5", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asInt(WsByteBuffer).
     */
    @Test
    public void test6() {
        try {
            // test null input
            assertEquals(-1, WsByteBufferUtils.asInt((WsByteBuffer) null));
            // test empty input
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(1024);
            buffer.limit(0);
            assertEquals(-1, WsByteBufferUtils.asInt(buffer));
            buffer.release();
            // test valid buffer numbers
            buffer = setupBuffer(1024, "349024");
            int pos = buffer.position();
            int lim = buffer.limit();
            assertEquals(349024, WsByteBufferUtils.asInt(buffer));
            assertEquals(pos, buffer.position());
            assertEquals(lim, buffer.limit());
            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test6", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asInt(WsByteBuffer, int, int)
     */
    @Test
    public void test7() {
        try {
            // test null input
            assertEquals(-1, WsByteBufferUtils.asInt((WsByteBuffer) null, 0, 0));
            // test empty input
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            buffer.limit(0);
            assertEquals(-1, WsByteBufferUtils.asInt(buffer, 0, 0));
            buffer.release();
            // test valid input
            buffer = setupBuffer(1024, "7830950932");
            int pos = buffer.position();
            int lim = buffer.limit();
            assertEquals(8309509, WsByteBufferUtils.asInt(buffer, 1, 8));
            assertEquals(pos, buffer.position());
            assertEquals(lim, buffer.limit());
            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test7", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asInt(WsByteBuffer[]).
     */
    @Test
    public void test8() {
        try {
            // test null input
            assertEquals(-1, WsByteBufferUtils.asInt((WsByteBuffer[]) null));
            // test empty list
            assertEquals(-1, WsByteBufferUtils.asInt(new WsByteBuffer[1]));
            // test valid input
            WsByteBuffer[] list = setupBuffers(8192, new String[] { "6783", "98712" });
            int[] pos = getPositions(list);
            int[] lim = getLimits(list);
            assertEquals(678398712, WsByteBufferUtils.asInt(list));
            comparePositions(pos, list);
            compareLimits(lim, list);
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test8", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asInt(WsByteBuffer[], int[], int[]).
     */
    @Test
    public void test9() {
        try {
            // test null input
            assertEquals(-1, WsByteBufferUtils.asInt((WsByteBuffer[]) null, null, null));
            // test empty list
            assertEquals(-1, WsByteBufferUtils.asInt(new WsByteBuffer[1], null, null));
            // test valid input
            WsByteBuffer[] list = setupBuffers(8192, new String[] { "6783", "98712" });
            int[] pos = getPositions(list);
            int[] lim = getLimits(list);
            assertEquals(6783871, WsByteBufferUtils.asInt(list,
                                                          new int[] { 0, 1 }, new int[] { 4, 4 }));
            comparePositions(pos, list);
            compareLimits(lim, list);
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test9", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asString(WsByteBuffer).
     */
    @Test
    public void test10() {
        try {
            // test null input
            assertNull(WsByteBufferUtils.asString((WsByteBuffer) null));
            // test empty input
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(1024);
            buffer.limit(0);
            assertNull(WsByteBufferUtils.asString(buffer));
            buffer.release();
            // test valid input
            buffer = setupBuffer(1024, data);
            int pos = buffer.position();
            int lim = buffer.limit();
            assertEquals(data, WsByteBufferUtils.asString(buffer));
            assertEquals(pos, buffer.position());
            assertEquals(lim, buffer.limit());
            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test10", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asString(WsByteBuffer, int, int).
     */
    @Test
    public void test11() {
        try {
            // test null input
            assertNull(WsByteBufferUtils.asString((WsByteBuffer) null, 0, 0));
            // test empty input
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(1024);
            buffer.limit(0);
            assertNull(WsByteBufferUtils.asString(buffer, 0, 0));
            buffer.release();
            // test valid input
            buffer = setupBuffer(1024, data2);
            int pos = buffer.position();
            int lim = buffer.limit();
            assertEquals(data2.substring(2, lim - 1), WsByteBufferUtils.asString(buffer, 2, lim - 1));
            assertEquals(pos, buffer.position());
            assertEquals(lim, buffer.limit());
            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test11", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asString(WsByteBuffer[]).
     */
    @Test
    public void test12() {
        try {
            // test null input
            assertNull(WsByteBufferUtils.asString((WsByteBuffer[]) null));
            // test empty list
            assertNull(WsByteBufferUtils.asString(new WsByteBuffer[1]));
            // test valid input
            WsByteBuffer[] list = setupBuffers(512, new String[] { data, data2 });
            int[] pos = getPositions(list);
            int[] lim = getLimits(list);
            assertEquals((data + data2), WsByteBufferUtils.asString(list));
            comparePositions(pos, list);
            compareLimits(lim, list);
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test12", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asString(WsByteBuffer[], int[], int[]).
     */
    @Test
    public void test13() {
        try {
            // test null input
            assertNull(WsByteBufferUtils.asString((WsByteBuffer[]) null, null, null));
            // test empty input
            assertNull(WsByteBufferUtils.asString(new WsByteBuffer[1], null, null));
            // test valid input
            WsByteBuffer[] list = setupBuffers(8192, new String[] { "6783", "98712" });
            int[] pos = getPositions(list);
            int[] lim = getLimits(list);
            assertEquals("6783871", WsByteBufferUtils.asString(list,
                                                               new int[] { 0, 1 }, new int[] { 4, 4 }));
            comparePositions(pos, list);
            compareLimits(lim, list);
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test13", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asStringBuffer(WsByteBuffer).
     */
    @Test
    public void test14() {
        try {
            // test null input
            assertEquals(0, WsByteBufferUtils.asStringBuffer((WsByteBuffer) null).length());
            // test empty input
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(512);
            buffer.limit(0);
            assertEquals(0, WsByteBufferUtils.asStringBuffer(buffer).length());
            buffer.release();
            // test valid input
            buffer = setupBuffer(1024, data);
            int pos = buffer.position();
            int lim = buffer.limit();
            StringBuffer testData = WsByteBufferUtils.asStringBuffer(buffer);
            assertEquals(pos, buffer.position());
            assertEquals(lim, buffer.limit());
            assertEquals(data, testData.toString());
            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test14", t);
        }
    }

    /**
     * Test WsByteBufferUtils.asStringBuffer(WsByteBuffer[]).
     */
    @Test
    public void test15() {
        try {
            WsByteBuffer[] list = setupBuffers(512, new String[] { data2, data });
            int[] pos = getPositions(list);
            int[] lim = getLimits(list);
            assertEquals((data2 + data), WsByteBufferUtils.asStringBuffer(list).toString());
            comparePositions(pos, list);
            compareLimits(lim, list);
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test15", t);
        }
    }

    /**
     * Test WsByteBufferUtils.clearBufferArray(WsByteBuffer[]).
     */
    @Test
    public void test16() {
        try {
            // test null input
            WsByteBufferUtils.clearBufferArray(null);
            // test empty list
            WsByteBufferUtils.clearBufferArray(new WsByteBuffer[1]);
            // test valid input
            WsByteBuffer[] list = setupBuffers(16384, new String[] { data, data2 });
            list[0].position(list[0].limit());
            list[1].position(list[1].limit());
            WsByteBufferUtils.clearBufferArray(list);
            assertEquals(0, list[0].position());
            assertEquals(0, list[1].position());
            assertEquals(16384, list[0].limit());
            assertEquals(16384, list[1].limit());
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test16", t);
        }
    }

    /**
     * Test WsByteBufferUtils.expandBufferArray(WsByteBuffer[], WsByteBuffer).
     */
    @Test
    public void test17() {
        try {
            // test null input
            assertNull(WsByteBufferUtils.expandBufferArray(null, (WsByteBuffer) null));
            // test null original
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            buffer.put(data2.getBytes());
            buffer.flip();
            WsByteBuffer[] newlist = WsByteBufferUtils.expandBufferArray(null, buffer);
            assertNotNull(newlist);
            assertEquals(1, newlist.length);
            // test existing list + valid buffer
            WsByteBuffer[] list = setupBuffers(1024, new String[] { data, data2 });
            newlist = WsByteBufferUtils.expandBufferArray(list, buffer);
            assertNotNull(newlist);
            assertEquals(3, newlist.length);
            assertEquals(1024, newlist[0].capacity());
            assertEquals(1024, newlist[1].capacity());
            assertEquals(8192, newlist[2].capacity());
            // test existing list + null buffer
            newlist = WsByteBufferUtils.expandBufferArray(list, (WsByteBuffer) null);
            assertNotNull(newlist);
            assertEquals(list.length, newlist.length);
            releaseList(list);
            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test17", t);
        }
    }

    /**
     * Test WsByteBufferUtils.expandBufferArray(WsByteBuffer[], WsByteBuffer[]).
     */
    @Test
    public void test18() {
        try {
            // test null input
            assertNull(WsByteBufferUtils.expandBufferArray(null, (WsByteBuffer[]) null));
            // test existing list + null add
            WsByteBuffer[] list = setupBuffers(8192, new String[] { data });
            WsByteBuffer[] newlist = WsByteBufferUtils.expandBufferArray(list, (WsByteBuffer[]) null);
            assertNotNull(newlist);
            assertEquals(list.length, newlist.length);
            // test null existing + new list
            newlist = WsByteBufferUtils.expandBufferArray(null, list);
            assertNotNull(newlist);
            assertEquals(list.length, newlist.length);
            // test existing + new list
            WsByteBuffer[] list2 = setupBuffers(1024, new String[] { data2 });
            newlist = WsByteBufferUtils.expandBufferArray(list, list2);
            assertNotNull(newlist);
            assertEquals(2, newlist.length);
            assertEquals(8192, newlist[0].capacity());
            assertEquals(1024, newlist[1].capacity());
            releaseList(list);
            releaseList(list2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test18", t);
        }
    }

    /**
     * Test WsByteBufferUtils.flip(WsByteBuffer[]).
     */
    @Test
    public void test19() {
        try {
            // test null input
            WsByteBufferUtils.flip(null);
            // test empty list
            WsByteBufferUtils.flip(new WsByteBuffer[1]);
            // test valid input
            WsByteBuffer[] list = setupBuffers(16384, new String[] { data, data2 });
            list[0].position(list[0].limit());
            list[1].position(list[1].limit());
            WsByteBufferUtils.flip(list);
            assertEquals(0, list[0].position());
            assertEquals(0, list[1].position());
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test19", t);
        }
    }

    /**
     * Test WsByteBufferUtils.getTotalCapacity(WsByteBuffer[]).
     */
    @Test
    public void test20() {
        try {
            // test null input
            assertEquals(0, WsByteBufferUtils.getTotalCapacity(null));
            // test empty list
            assertEquals(0, WsByteBufferUtils.getTotalCapacity(new WsByteBuffer[1]));
            // test valid list
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(1024);
            WsByteBuffer buffer2 = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            WsByteBuffer buffer3 = ChannelFrameworkFactory.getBufferManager().allocate(16384);
            WsByteBuffer[] list = new WsByteBuffer[] { buffer, buffer2, buffer3 };
            assertEquals((1024 + 8192 + 16384), WsByteBufferUtils.getTotalCapacity(list));
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test20", t);
        }
    }

    /**
     * Test WsByteBufferUtils.lengthOf(WsByteBuffer[]).
     */
    @Test
    public void test21() {
        try {
            // test a null list
            assertEquals(0, WsByteBufferUtils.lengthOf(null));
            // test a list of valid buffers
            WsByteBuffer[] list = setupBuffers(1024, new String[] { data, data, data2, data2 });
            assertEquals((2 * data.length() + 2 * data2.length()), WsByteBufferUtils.lengthOf(list));
            // test a list with a null buffer in it
            list[2].release();
            list[2] = null;
            assertEquals((2 * data.length()), WsByteBufferUtils.lengthOf(list));
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test21", t);
        }
    }

    /**
     * Test WsByteBufferUtils.putByteArrayValue(WsByteBuffer[], byte[], boolean).
     */
    @Test
    public void test22() {
        try {
            // test null input
            WsByteBufferUtils.putByteArrayValue(null, null, true);
            // test too much input
            try {
                WsByteBufferUtils.putByteArrayValue(new WsByteBuffer[1], data.getBytes(), true);
                fail("Should have failed");
            } catch (IllegalArgumentException e) {
                // expected failure
            }
            // test valid input with a flip()
            WsByteBuffer[] list = new WsByteBuffer[1];
            list[0] = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            WsByteBufferUtils.putByteArrayValue(list, data.getBytes(), true);
            assertEquals(0, list[0].position());
            assertEquals(data.length(), list[0].limit());
            // test valid input without a flip()
            list[0].clear();
            WsByteBufferUtils.putByteArrayValue(list, data2.getBytes(), false);
            assertEquals(data2.length(), list[0].position());
            assertEquals(8192, list[0].limit());
            // test valid input over >1 buffer with a flip()
            releaseList(list);
            list = new WsByteBuffer[] {
                                       ChannelFrameworkFactory.getBufferManager().allocate(10),
                                       ChannelFrameworkFactory.getBufferManager().allocate(10)
            };
            WsByteBufferUtils.putByteArrayValue(list, "1234567890abcdeg".getBytes(), true);
            assertEquals(0, list[0].position());
            assertEquals(10, list[0].limit());
            assertEquals(0, list[1].position());
            assertEquals(6, list[1].limit());
            assertEquals("1234567890abcdeg", WsByteBufferUtils.asString(list));
            // test valid input over >1 buffer without a flip()
            releaseList(list);
            list = new WsByteBuffer[] {
                                       ChannelFrameworkFactory.getBufferManager().allocate(10),
                                       ChannelFrameworkFactory.getBufferManager().allocate(10)
            };
            WsByteBufferUtils.putByteArrayValue(list, "1234567890abcdeg".getBytes(), false);
            assertEquals(0, list[0].position());
            assertEquals(10, list[0].limit());
            assertEquals(6, list[1].position());
            assertEquals(10, list[1].limit());
            assertEquals("1234567890", WsByteBufferUtils.asString(list[0]));
            list[1].flip();
            assertEquals("abcdeg", WsByteBufferUtils.asString(list[1]));
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test22", t);
        }
    }

    /**
     * Test WsByteBufferUtils.putStringValue(WsByteBuffer[], String, boolean).
     */
    @Test
    public void test23() {
        try {
            // test null input
            WsByteBufferUtils.putStringValue(null, null, true);
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(32);
            WsByteBufferUtils.putStringValue(new WsByteBuffer[] { buffer }, null, true);
            buffer.release();
            // test too much data
            try {
                WsByteBufferUtils.putStringValue(new WsByteBuffer[1], data, true);
                fail("Should have seen an exception");
            } catch (IllegalArgumentException e) {
                // expected failure
            }
            // test valid data with flip()
            WsByteBuffer[] list = new WsByteBuffer[1];
            list[0] = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            WsByteBufferUtils.putStringValue(list, data2, true);
            assertEquals(0, list[0].position());
            assertEquals(data2.length(), list[0].limit());
            // test valid data without flip()
            list[0].clear();
            WsByteBufferUtils.putStringValue(list, data, false);
            assertEquals(data.length(), list[0].position());
            assertEquals(8192, list[0].limit());
            // test valid input over >1 buffer with a flip()
            releaseList(list);
            list = new WsByteBuffer[] {
                                       ChannelFrameworkFactory.getBufferManager().allocate(10),
                                       ChannelFrameworkFactory.getBufferManager().allocate(10)
            };
            WsByteBufferUtils.putStringValue(list, "1234567890abcdeg", true);
            assertEquals(0, list[0].position());
            assertEquals(10, list[0].limit());
            assertEquals(0, list[1].position());
            assertEquals(6, list[1].limit());
            assertEquals("1234567890abcdeg", WsByteBufferUtils.asString(list));
            // test valid input over >1 buffer without a flip()
            releaseList(list);
            list = new WsByteBuffer[] {
                                       ChannelFrameworkFactory.getBufferManager().allocate(10),
                                       ChannelFrameworkFactory.getBufferManager().allocate(10)
            };
            WsByteBufferUtils.putStringValue(list, "1234567890abcdeg", false);
            assertEquals(0, list[0].position());
            assertEquals(10, list[0].limit());
            assertEquals(6, list[1].position());
            assertEquals(10, list[1].limit());
            assertEquals("1234567890", WsByteBufferUtils.asString(list[0]));
            list[1].flip();
            assertEquals("abcdeg", WsByteBufferUtils.asString(list[1]));
            releaseList(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test23", t);
        }
    }

    /**
     * Test WsByteBufferUtils.releaseBufferArray(WsByteBuffer[]).
     */
    @Test
    public void test24() {
        try {
            // test a null list
            WsByteBufferUtils.releaseBufferArray(null);
            // test a list of all valid buffers
            WsByteBuffer[] list = setupBuffers(512, new String[] { data, data2, null });
            WsByteBufferUtils.releaseBufferArray(list);
            assertNull(list[0]);
            assertNull(list[1]);
            // test a list of null buffers
            WsByteBufferUtils.releaseBufferArray(list);
        } catch (Throwable t) {
            outputMgr.failWithThrowable("test24", t);
        }
    }
}
