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
package com.ibm.ws.http.channel.test.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.http.channel.compression.CompressionHandler;
import com.ibm.wsspi.http.channel.compression.DecompressionHandler;
import com.ibm.wsspi.http.channel.compression.DeflateInputHandler;
import com.ibm.wsspi.http.channel.compression.DeflateOutputHandler;
import com.ibm.wsspi.http.channel.compression.GzipInputHandler;
import com.ibm.wsspi.http.channel.compression.GzipOutputHandler;
import com.ibm.wsspi.http.channel.compression.IdentityInputHandler;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;

/**
 * JUnit tests for the various compression/decompression handlers.
 * 
 */
public class CompressionTest {
    private static SharedOutputManager outputMgr;

    /** Compression methods allowed */
    private enum TYPES {
        /** gzip type */
        GZIP,
        /** x-gzip type */
        XGZIP,
        /** deflate type */
        DEFLATE
    }

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
     * Create a compressed stream of data based on the input string.
     * 
     * @param type
     * @param data
     * @return byte[]
     * @throws IOException
     */
    private byte[] createCompressedData(TYPES type, String data) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DeflaterOutputStream stream = null;
        if (TYPES.GZIP == type) {
            stream = new GZIPOutputStream(os);
        } else if (TYPES.XGZIP == type) {
            stream = new GZIPOutputStream(os);
        } else if (TYPES.DEFLATE == type) {
            stream = new DeflaterOutputStream(os);
        } else {
            throw new IOException("Unhandled type: " + type);
        }
        stream.write(data.getBytes());
        stream.flush();
        stream.close();
        return os.toByteArray();
    }

    /**
     * Decompress the list of buffers into a String.
     * 
     * @param type
     * @param list
     * @return String
     * @throws IOException
     */
    private String decompressData(TYPES type, List<WsByteBuffer> list) throws IOException {
        WsByteBuffer[] buffers = new WsByteBuffer[list.size()];
        list.toArray(buffers);
        ByteArrayInputStream is = new ByteArrayInputStream(WsByteBufferUtils.asByteArray(buffers));
        InflaterInputStream stream = null;
        if (TYPES.GZIP == type) {
            stream = new GZIPInputStream(is);
        } else if (TYPES.XGZIP == type) {
            stream = new GZIPInputStream(is);
        } else if (TYPES.DEFLATE == type) {
            stream = new InflaterInputStream(is);
        } else {
            throw new IOException("Unhandled type: " + type);
        }
        String rc = "";
        byte[] data = new byte[1024];
        int num = stream.read(data, 0, data.length);
        while (-1 != num) {
            rc += new String(data, 0, num);
            num = stream.read(data, 0, data.length);
        }
        return rc;
    }

    /**
     * Compare the target string against the list of buffers.
     * 
     * @param data
     * @param list
     */
    private void compare(String data, List<WsByteBuffer> list) {
        WsByteBuffer[] buffers = new WsByteBuffer[list.size()];
        list.toArray(buffers);
        assertEquals(data, WsByteBufferUtils.asString(buffers));
    }

    /**
     * Release a list of buffers.
     * 
     * @param list
     */
    private void release(List<WsByteBuffer> list) {
        while (!list.isEmpty()) {
            list.remove(0).release();
        }
    }

    /**
     * Calculate the number of bytes on this list of buffers.
     * 
     * @param list
     * @return long
     */
    private long sizeof(List<WsByteBuffer> list) {
        long size = 0L;
        Iterator<WsByteBuffer> it = list.iterator();
        while (it.hasNext()) {
            size += it.next().remaining();
        }
        return size;
    }

    /**
     * Test decompression with the identity handler.
     */
    @Test
    public void testIdentityDecompression() {
        try {
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            List<WsByteBuffer> output = null;
            byte[] data = null;
            String testData = null;

            // test identity handler
            DecompressionHandler handler = new IdentityInputHandler();
            testData = "identity handler test";
            data = testData.getBytes();
            buffer.put(data);
            buffer.flip();
            output = handler.decompress(buffer);
            assertEquals(1, output.size());
            compare(testData, output);
            assertEquals(data.length, handler.getBytesRead());
            assertEquals(data.length, handler.getBytesWritten());
            assertFalse(handler.isEnabled());
            handler.close();

            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testIdentityDecompression", t);
        }
    }

    /**
     * Test decompression with the gzip handler.
     */
    @Test
    public void testGzipDecompression() {
        try {
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            WsByteBuffer buffer2 = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            List<WsByteBuffer> output = null;
            byte[] data = null;
            byte[] data2 = null;
            String testData = null;
            String testData2 = null;
            DecompressionHandler handler = new GzipInputHandler();
            assertTrue(handler.isEnabled());

            // test regular gzip
            testData = "regular gzip test";
            data = createCompressedData(TYPES.GZIP, testData);
            buffer.put(data, 0, data.length);
            buffer.flip();
            output = handler.decompress(buffer);
            compare(testData, output);
            assertEquals(0, buffer.remaining());
            assertEquals(data.length - 18, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            // test splitting the user data
            handler = new GzipInputHandler();
            testData = "split user gzip test";
            data = createCompressedData(TYPES.GZIP, testData);
            buffer.put(data, 0, data.length);
            buffer.flip();
            buffer.limit(data.length - 12);
            output = handler.decompress(buffer);
            assertEquals(1, output.size());
            assertEquals(0, buffer.remaining());
            assertEquals(data.length - 22, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            buffer.limit(data.length);
            output.addAll(handler.decompress(buffer));
            assertEquals(2, output.size());
            compare(testData, output);
            assertEquals(0, buffer.remaining());
            assertEquals(data.length - 18, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            // test multiple distinct chunks of compressed data (old HTTP channel client for example)
            handler = new GzipInputHandler();
            testData = "test chunk1";
            testData2 = "test chunk2";
            data = createCompressedData(TYPES.GZIP, testData);
            data2 = createCompressedData(TYPES.GZIP, testData2);
            buffer.put(data, 0, data.length);
            buffer.flip();
            buffer2.put(data2, 0, data2.length);
            buffer2.flip();
            output = handler.decompress(buffer);
            assertEquals(1, output.size());
            assertEquals(0, buffer.remaining());
            assertEquals(data.length - 18, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            // now 2nd discrete chunk should trigger the internal reset/continue path
            output.addAll(handler.decompress(buffer2));
            assertEquals(2, output.size());
            compare(testData + testData2, output);
            assertEquals(0, buffer2.remaining());
            assertEquals(data.length + data2.length - 36, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            buffer2.clear();
            release(output);
            output = null;

            // test splitting the gzip header data
            handler = new GzipInputHandler();
            testData = "split gzip header test";
            data = createCompressedData(TYPES.GZIP, testData);
            buffer.put(data, 0, 2);
            buffer.flip();
            output = handler.decompress(buffer);
            assertEquals(0, buffer.remaining());
            assertTrue(output.isEmpty());
            assertFalse(handler.isFinished());
            assertEquals(0, handler.getBytesRead());
            assertEquals(0, handler.getBytesWritten());
            // now make sure we continue with the rest fine
            buffer.clear();
            buffer.put(data, 2, data.length - 2);
            buffer.flip();
            output.addAll(handler.decompress(buffer));
            compare(testData, output);
            assertEquals(data.length - 18, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            // test splitting the gzip trailer
            handler = new GzipInputHandler();
            testData = "split gzip trailer test";
            data = createCompressedData(TYPES.GZIP, testData);
            buffer.put(data, 0, data.length);
            buffer.flip();
            buffer.limit(data.length - 3);
            output = handler.decompress(buffer);
            assertFalse(output.isEmpty());
            // now make sure we continue with the rest fine
            buffer.limit(data.length);
            output.addAll(handler.decompress(buffer));
            compare(testData, output);
            assertEquals(data.length - 18, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            // test with the FEXTRA flag set in the header
            handler = new GzipInputHandler();
            testData = "FEXTRA test";
            data = createCompressedData(TYPES.GZIP, testData);
            buffer.put((byte) 0x1f);
            buffer.put((byte) 0x8b);
            buffer.put((byte) Deflater.DEFLATED);
            buffer.put((byte) 0x4);
            for (int i = 0; i < 6; i++) {
                buffer.put((byte) 0);
            }
            // FEXTRA is 2 byte length int and that many bytes (Intel byte order)
            buffer.put((byte) 2);
            buffer.put((byte) 0);
            buffer.put((byte) 1);
            buffer.put((byte) 2);
            buffer.put(data, 10, data.length - 10);
            buffer.flip();
            assertEquals(data.length + 4, buffer.remaining());
            output = handler.decompress(buffer);
            assertEquals(0, buffer.remaining());
            compare(testData, output);
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            // test with the FEXTRA flag set in the header (length split)
            handler = new GzipInputHandler();
            testData = "split FEXTRA test";
            data = createCompressedData(TYPES.GZIP, testData);
            buffer.put((byte) 0x1f);
            buffer.put((byte) 0x8b);
            buffer.put((byte) Deflater.DEFLATED);
            buffer.put((byte) 0x4);
            for (int i = 0; i < 6; i++) {
                buffer.put((byte) 0);
            }
            // FEXTRA is 2 byte length int and that many bytes (Intel byte order)
            buffer.put((byte) 2);
            // force boundary condition by breaking the 2 bytes across parses
            buffer.flip();
            output = handler.decompress(buffer);
            assertTrue(output.isEmpty());
            buffer.clear();
            buffer.put((byte) 0);
            buffer.put((byte) 1);
            buffer.put((byte) 2);
            buffer.put(data, 10, data.length - 10);
            buffer.flip();
            output.addAll(handler.decompress(buffer));
            assertEquals(0, buffer.remaining());
            compare(testData, output);
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            // test with the FNAME flag set in the header
            handler = new GzipInputHandler();
            testData = "FNAME test";
            data = createCompressedData(TYPES.GZIP, testData);
            buffer.put((byte) 0x1f);
            buffer.put((byte) 0x8b);
            buffer.put((byte) Deflater.DEFLATED);
            buffer.put((byte) 0x8);
            for (int i = 0; i < 6; i++) {
                buffer.put((byte) 0);
            }
            // FNAME is a zero delimited string
            buffer.put((byte) 'a');
            buffer.put((byte) 'b');
            buffer.put((byte) 'c');
            buffer.put((byte) 'd');
            buffer.put((byte) 0);
            buffer.put(data, 10, data.length - 10);
            buffer.flip();
            assertEquals(data.length + 5, buffer.remaining());
            output = handler.decompress(buffer);
            assertEquals(0, buffer.remaining());
            compare(testData, output);
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            // test with the FNAME flag set in the header (split on zero)
            handler = new GzipInputHandler();
            testData = "split FNAME test";
            data = createCompressedData(TYPES.GZIP, testData);
            buffer.put((byte) 0x1f);
            buffer.put((byte) 0x8b);
            buffer.put((byte) Deflater.DEFLATED);
            buffer.put((byte) 0x8);
            for (int i = 0; i < 6; i++) {
                buffer.put((byte) 0);
            }
            // FNAME is a zero delimited string
            buffer.put((byte) 'a');
            buffer.put((byte) 'b');
            buffer.put((byte) 'c');
            buffer.put((byte) 'd');
            buffer.flip();
            output = handler.decompress(buffer);
            assertTrue(output.isEmpty());
            buffer.clear();
            // force boundary condition check by having 1st byte be delimiter
            buffer.put((byte) 0);
            buffer.put(data, 10, data.length - 10);
            buffer.flip();
            output.addAll(handler.decompress(buffer));
            assertEquals(0, buffer.remaining());
            compare(testData, output);
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            // test with the FCOMMENT flag set in the header
            handler = new GzipInputHandler();
            testData = "FCOMMENT test";
            data = createCompressedData(TYPES.GZIP, testData);
            buffer.put((byte) 0x1f);
            buffer.put((byte) 0x8b);
            buffer.put((byte) Deflater.DEFLATED);
            buffer.put((byte) 0x10);
            for (int i = 0; i < 6; i++) {
                buffer.put((byte) 0);
            }
            // FCOMMENT is a zero delimited string
            buffer.put((byte) 0);
            buffer.put(data, 10, data.length - 10);
            buffer.flip();
            assertEquals(data.length + 1, buffer.remaining());
            output = handler.decompress(buffer);
            assertEquals(0, buffer.remaining());
            compare(testData, output);
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            // test with the FHCRC flag set in the header
            handler = new GzipInputHandler();
            testData = "FHCRC test";
            data = createCompressedData(TYPES.GZIP, testData);
            buffer.put((byte) 0x1f);
            buffer.put((byte) 0x8b);
            buffer.put((byte) Deflater.DEFLATED);
            buffer.put((byte) 0x1);
            for (int i = 0; i < 6; i++) {
                buffer.put((byte) 0);
            }
            // FHCRC is simply 2 bytes (checksum of all gzip header bytes)
            buffer.put((byte) 1);
            buffer.put((byte) 2);
            buffer.put(data, 10, data.length - 10);
            buffer.flip();
            assertEquals(data.length + 2, buffer.remaining());
            output = handler.decompress(buffer);
            assertEquals(0, buffer.remaining());
            compare(testData, output);
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            buffer.release();
            buffer2.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testGzipDecompression", t);
        }
    }

    /**
     * Test decompression with the x-gzip handler.
     */
    @Test
    public void testXGzipDecompression() {
        try {
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            List<WsByteBuffer> output = null;
            byte[] data = null;
            String testData = null;
            DecompressionHandler handler = new GzipInputHandler();
            assertTrue(handler.isEnabled());

            // test x-gzip input
            testData = "xgzipdata";
            data = createCompressedData(TYPES.XGZIP, testData);
            buffer.put(data);
            buffer.flip();
            output = handler.decompress(buffer);
            compare(testData, output);
            assertEquals(data.length - 18, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);

            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testXGzipDecompression", t);
        }
    }

    /**
     * Test decompression with the deflate handler.
     */
    @Test
    public void testDeflateDecompression() {
        try {
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            WsByteBuffer buffer2 = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            List<WsByteBuffer> output = null;
            byte[] data = null;
            byte[] data2 = null;
            String testData = null;
            String testData2 = null;
            DecompressionHandler handler = null;

            // test deflate
            handler = new DeflateInputHandler();
            testData = "deflatedata";
            data = createCompressedData(TYPES.DEFLATE, testData);
            buffer.put(data);
            buffer.flip();
            output = handler.decompress(buffer);
            compare(testData, output);
            assertEquals(data.length, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            assertTrue(handler.isEnabled());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            // test splitting the user data
            handler = new DeflateInputHandler();
            testData = "split user deflate test";
            data = createCompressedData(TYPES.DEFLATE, testData);
            buffer.put(data, 0, data.length);
            buffer.flip();
            buffer.limit(data.length - 10);
            output = handler.decompress(buffer);
            assertEquals(1, output.size());
            System.out.println(WsByteBufferUtils.asString(output.get(0)));
            assertEquals(0, buffer.remaining());
            assertEquals(data.length - 10, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            buffer.limit(data.length);
            output.addAll(handler.decompress(buffer));
            assertEquals(2, output.size());
            compare(testData, output);
            assertEquals(0, buffer.remaining());
            assertEquals(data.length, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            release(output);
            output = null;

            // test multiple distinct chunks of compressed data (old HTTP channel client for example)
            handler = new DeflateInputHandler();
            testData = "test deflate chunk1";
            testData2 = "test deflate chunk2";
            data = createCompressedData(TYPES.DEFLATE, testData);
            data2 = createCompressedData(TYPES.DEFLATE, testData2);
            buffer.put(data, 0, data.length);
            buffer.flip();
            buffer2.put(data2, 0, data2.length);
            buffer2.flip();
            output = handler.decompress(buffer);
            assertEquals(1, output.size());
            assertEquals(0, buffer.remaining());
            assertEquals(data.length, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            // now 2nd discrete chunk should trigger the internal reset/continue path
            output.addAll(handler.decompress(buffer2));
            assertEquals(2, output.size());
            compare(testData + testData2, output);
            assertEquals(0, buffer2.remaining());
            assertEquals(data.length + data2.length, handler.getBytesRead());
            assertEquals(sizeof(output), handler.getBytesWritten());
            assertTrue(handler.isFinished());
            handler.close();
            buffer.clear();
            buffer2.clear();
            release(output);
            output = null;

            buffer.release();
            buffer2.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testDeflateDecompression", t);
        }
    }

    /**
     * Test compression with the gzip handler.
     */
    @Test
    public void testGzipCompression() {
        try {
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            List<WsByteBuffer> output = null;
            String data = null;
            String testData = null;
            CompressionHandler handler = null;

            // test gzip
            handler = new GzipOutputHandler(false);
            assertEquals(ContentEncodingValues.GZIP, handler.getContentEncoding());
            testData = "gzip compression test";
            buffer.put(testData.getBytes());
            buffer.flip();
            output = handler.compress(buffer);
            output.addAll(handler.finish());
            data = decompressData(TYPES.GZIP, output);
            assertEquals(testData, data);
            assertTrue(handler.isFinished());

            GzipInputHandler decompresser = new GzipInputHandler();
            List<WsByteBuffer> output2 = new LinkedList<WsByteBuffer>();
            Iterator<WsByteBuffer> it = output.iterator();
            while (it.hasNext()) {
                output2.addAll(decompresser.decompress(it.next()));
            }
            decompresser.close();
            compare(testData, output2);
            release(output);
            release(output2);

            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testGzipCompression", t);
        }
    }

    /**
     * Test compression with the x-gzip handler.
     */
    @Test
    public void testXGzipCompression() {
        try {
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            List<WsByteBuffer> output = null;
            String data = null;
            String testData = null;
            CompressionHandler handler = new GzipOutputHandler(true);

            // test x-gzip
            assertEquals(ContentEncodingValues.XGZIP, handler.getContentEncoding());
            testData = "xgzip compression test";
            buffer.put(testData.getBytes());
            buffer.flip();
            output = handler.compress(buffer);
            output.addAll(handler.finish());
            data = decompressData(TYPES.XGZIP, output);
            assertEquals(testData, data);
            assertTrue(handler.isFinished());
            release(output);

            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testXGzipCompression", t);
        }
    }

    /**
     * Test compression with the deflate handler.
     */
    @Test
    public void testDeflateCompression() {
        try {
            WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(8192);
            List<WsByteBuffer> output = null;
            String data = null;
            String testData = null;
            CompressionHandler handler = new DeflateOutputHandler();

            // test deflate
            assertEquals(ContentEncodingValues.DEFLATE, handler.getContentEncoding());
            testData = "deflate compression test";
            buffer.put(testData.getBytes());
            buffer.flip();
            output = handler.compress(buffer);
            output.addAll(handler.finish());
            data = decompressData(TYPES.DEFLATE, output);
            assertEquals(testData, data);
            assertTrue(handler.isFinished());
            release(output);

            buffer.release();
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testDeflateCompression", t);
        }
    }

}
