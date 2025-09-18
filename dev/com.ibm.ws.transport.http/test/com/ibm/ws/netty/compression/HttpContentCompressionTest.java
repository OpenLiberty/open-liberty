/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.netty.compression;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Test;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.ws.bytebuffer.internal.WsByteBufferImpl;
import com.ibm.wsspi.http.channel.compression.DecompressionHandler;

import io.openliberty.http.netty.compression.HttpContentDecompressor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPOutputStream;

import com.ibm.ws.http.channel.internal.HttpChannelConfig;

/**
 * Test suite for {@link HttpContentDecompressor} that verifies decompressing behavior under
 * the following conditions:
 * -> Failing decompression when tolerance limit is hit
 * -> Successful decompression within tolerance
 * -> Handling when no decompressed chunks are produced
 * -> Multi-cycle compression
 * -> Auto decompression is disabled
 * -> Empty buffer is provided
 */
public class HttpContentCompressionTest {

    private WsByteBuffer bufferUnderTest;

    @After
    public void tearDown() {
        // If any test allocated a real buffer, release and nullify it.
        if (bufferUnderTest != null) {
            bufferUnderTest.release();
            bufferUnderTest = null;
        }
    }

    /**
     * Tests that decompression fails with a DataFormatException when the decompression ratio exceeds tolerance.
     *
     * @throws Exception if an error occurs during compression or decompression
     */
    @Test(expected = DataFormatException.class)
    public void testGzipDecompressionFailsTolerance() throws Exception {
        String payload = new String(new char[1000]).replace("\0", "a");
        byte[] compressed = compressWithGzip(payload.getBytes("UTF-8"));
        ByteBuffer compressedBuffer = ByteBuffer.wrap(compressed);

        WsByteBufferImpl wsBuffer = new WsByteBufferImpl();
        wsBuffer.setByteBuffer(compressedBuffer);
        bufferUnderTest = wsBuffer;

        HttpChannelConfig config = createMockConfig(true, 1, 0);

        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        decompressor.decompress(wsBuffer, config, "gzip");
    }

    /**
     * Tests that decompression succeeds within tolerance and produces the expected output size.
     *
     * @throws DataFormatException if the decompression process fails unexpectedly
     */
    @Test
    public void testDecompressionWithinTolerance() throws DataFormatException {
        WsByteBuffer inputBuffer = createMockInputBuffer(100, true, false);
        WsByteBuffer outputBuffer = mock(WsByteBuffer.class);
        when(outputBuffer.remaining()).thenReturn(100);
        when(outputBuffer.getWrappedByteBuffer()).thenReturn(ByteBuffer.allocate(100));
        doNothing().when(outputBuffer).release();

        DecompressionHandler handler = createMockHandler(Arrays.asList(outputBuffer), 100L, 100L);
        HttpChannelConfig config = createMockConfig(true, 100, 3);

        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        WsByteBuffer result = decompressor.decompress(inputBuffer, config, handler);

        assertNotNull(result);
        assertEquals(100, result.remaining());
    }

    /**
     * Tests that if the decompression handler produces no output chunks, an empty buffer is returned.
     *
     * @throws DataFormatException if decompression fails unexpectedly
     */
    @Test
    public void testNoDecompressionChunksProduced() throws DataFormatException {
        WsByteBuffer inputBuffer = createMockInputBuffer(50, true, false);
        DecompressionHandler handler = createMockHandler(Collections.emptyList(), 50L, 0L);
        HttpChannelConfig config = createMockConfig(true, 100, 3);

        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        WsByteBuffer result = decompressor.decompress(inputBuffer, config, handler);

        assertNotNull(result);
        assertEquals(0, result.remaining());
    }

    /**
     * Tests that multi-cycle decompression correctly aggregates output from successive iterations.
     *
     * @throws DataFormatException if decompression fails unexpectedly
     */
    @Test
    public void testMultiCycleDecompression() throws DataFormatException {
        WsByteBuffer inputBuffer = mock(WsByteBuffer.class);
        when(inputBuffer.remaining()).thenReturn(60, 40);
        when(inputBuffer.hasRemaining()).thenReturn(true, true, false);
        doNothing().when(inputBuffer).release();

        WsByteBuffer outputBuffer1 = mock(WsByteBuffer.class);
        when(outputBuffer1.remaining()).thenReturn(60);
        // Ensure a valid ByteBuffer is returned.
        when(outputBuffer1.getWrappedByteBuffer()).thenReturn(ByteBuffer.allocate(60));
        doNothing().when(outputBuffer1).release();

        WsByteBuffer outputBuffer2 = mock(WsByteBuffer.class);
        when(outputBuffer2.remaining()).thenReturn(40);
        when(outputBuffer2.getWrappedByteBuffer()).thenReturn(ByteBuffer.allocate(40));
        doNothing().when(outputBuffer2).release();

        DecompressionHandler handler = mock(DecompressionHandler.class);
        when(handler.decompress(any(WsByteBuffer.class))).thenReturn(Arrays.asList(outputBuffer1)).thenReturn(Arrays.asList(outputBuffer2));
        when(handler.getBytesRead()).thenReturn(60L, 100L);
        when(handler.getBytesWritten()).thenReturn(60L, 100L);
        when(handler.isEnabled()).thenReturn(true);

        HttpChannelConfig config = createMockConfig(true, 100, 3);
        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        WsByteBuffer result = decompressor.decompress(inputBuffer, config, handler);

        assertNotNull(result);
        assertEquals(100, result.remaining());
    }

    /** 
     * Tests that decompression fails with a DataFormatException when the tolerance for 
     * bad ratios is met.
     * @throws DataFormatException if decompression fails as expected
     */
    @Test(expected = DataFormatException.class)
    public void testDecompressionExceedingTolerance() throws DataFormatException {
        WsByteBuffer inputBuffer = createMockInputBuffer(100, true, false);
        WsByteBuffer outputBuffer = mock(WsByteBuffer.class);
        when(outputBuffer.remaining()).thenReturn(200);
        when(outputBuffer.getWrappedByteBuffer()).thenReturn(ByteBuffer.allocate(200));
        doNothing().when(outputBuffer).release();

        DecompressionHandler handler = createMockHandler(Arrays.asList(outputBuffer), 100L, 200L);
        HttpChannelConfig config = createMockConfig(true, 1, 0);

        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        decompressor.decompress(inputBuffer, config, handler);
    }

    /**
     * Tests that when auto decompression is disabled, the original compressed data is returned 
     * unchanged. 
     * @throws DataFormatException
     * @throws IOException
     */
    @Test
    public void testAutoDecompressionDisabled() throws DataFormatException, IOException {
        String payload = "This is a test payload that should remain compressed.";
        byte[] compressed = compressWithGzip(payload.getBytes("UTF-8"));
        ByteBuffer compressedBuffer = ByteBuffer.wrap(compressed);

        WsByteBufferImpl wsBuffer = new WsByteBufferImpl();
        wsBuffer.setByteBuffer(compressedBuffer);
        bufferUnderTest = wsBuffer;

        HttpChannelConfig config = createMockConfig(false, 100, 3);

        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        WsByteBuffer result = decompressor.decompress(wsBuffer, config, "gzip");


        ByteBuffer original = wsBuffer.getWrappedByteBuffer().duplicate();
        original.rewind();
        ByteBuffer output = result.getWrappedByteBuffer().duplicate();
        output.rewind();


        byte[] originalBytes = new byte[original.remaining()];
        original.get(originalBytes);
        byte[] outputBytes = new byte[output.remaining()];
        output.get(outputBytes);

        assertArrayEquals("When auto-decompression is disabled, the output should match the input",
                          originalBytes, outputBytes);
    }

    /**
     * Tests that an empty input buffer produces an empty output buffer. 
     * @throws DataFormatException if decompression fails unexpectedly
     */
    @Test
    public void testEmptyInputBuffer() throws DataFormatException {
        WsByteBufferImpl inputBuffer = new WsByteBufferImpl();
        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);
        inputBuffer.setByteBuffer(emptyBuffer);
        bufferUnderTest = inputBuffer;

        DecompressionHandler handler = createMockHandler(Collections.emptyList(), 0L, 0L);
        HttpChannelConfig config = createMockConfig(true, 100, 3);

        HttpContentDecompressor decompressor = new HttpContentDecompressor();
        WsByteBuffer result = decompressor.decompress(inputBuffer, config, handler);

        assertNotNull(result);
        assertEquals("The output buffer should be empty if the input is empty", 0, result.remaining());
    }

    /**
     * Creates a mock for {@HttpChannelConfig} with the desired decompression HttpOptions.
     * @param decompressionEnabled sets the AutoDecompression HttpOption
     * @param ratioLimit sets the decompressionRatioLimit HttpOption
     * @param tolerance sets the decompressionTolerance HttpOption
     * @return a testable mock HttpChannelConfig instance
     */
    private HttpChannelConfig createMockConfig(boolean decompressionEnabled, int ratioLimit, int tolerance) {
        HttpChannelConfig config = mock(HttpChannelConfig.class);
        when(config.isAutoDecompressionEnabled()).thenReturn(decompressionEnabled);
        when(config.getDecompressionRatioLimit()).thenReturn(ratioLimit);
        when(config.getDecompressionTolerance()).thenReturn(tolerance);
        return config;
    }

    /**
     * Creates a mock for {@link WsByteBuffer} with a given number of bytes remaining. Used when
     * we don't need to create a real WsByteBuffer instance. 
     * @param remaining remaining number of bytes in the buffer
     * @param hasRemainingSequence a sequence of return values for successive invocations of hasRemaining()
     * @return a testable mock WsByteBuffer instance
     */
    private WsByteBuffer createMockInputBuffer(int remaining, Boolean... hasRemainingSequence) {
        WsByteBuffer inputBuffer = mock(WsByteBuffer.class);
        when(inputBuffer.remaining()).thenReturn(remaining);
        when(inputBuffer.hasRemaining()).thenReturn(hasRemainingSequence[0], hasRemainingSequence);
        doNothing().when(inputBuffer).release();
        return inputBuffer;
    }

    /**
     * Creates a mock for {@link DecompressionHandler} that returns the specified chunks and reports
     * the configured 'read bytes' and 'bytes written'. This is used to simplify testing decompression 
     * algoriths with values that align with the decompression ratios being tested. 
     * 
     * @param outputChunks a list of chunks to return
     * @param bytesRead the number of bytes expected to be reported to have been read
     * @param bytesWritten the number of bytes expected to be reported as written
     * @return a testable mock DecompressionHandler instance
     * @throws DataFormatException not thrown by this mock stub
     */
    private DecompressionHandler createMockHandler(List<WsByteBuffer> outputChunks, long bytesRead, long bytesWritten) throws DataFormatException {
        DecompressionHandler handler = mock(DecompressionHandler.class);
        when(handler.decompress(any(WsByteBuffer.class))).thenReturn(outputChunks);
        when(handler.getBytesRead()).thenReturn(bytesRead);
        when(handler.getBytesWritten()).thenReturn(bytesWritten);
        when(handler.isEnabled()).thenReturn(true);
        return handler;
    }

    /**
     * Helper method to quickly compress data with the GZIP algorithm
     * @param data
     * @return
     * @throws IOException
     */
    byte[] compressWithGzip(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(data);
        }
        return baos.toByteArray();
    }

  
}