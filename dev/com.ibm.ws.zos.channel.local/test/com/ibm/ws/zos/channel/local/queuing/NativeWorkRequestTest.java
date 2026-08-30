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
package com.ibm.ws.zos.channel.local.queuing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.zos.channel.local.LocalCommClientConnHandle;

import test.common.SharedOutputManager;

/**
 *
 */
public class NativeWorkRequestTest {
    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor:
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
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
     * Test NativeWorkRequest.toHexString().
     */
    @Test
    public void testByteArrayToHexString() {
        final String m = "testByteArrayToHexString";
        try {
            byte[] b = new byte[] { (byte) 0x00, (byte) 0x0a, (byte) 0x0f };

            String output = NativeWorkRequest.toHexString(b);

            assertEquals("000a0f", output);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test basics.
     */
    @Test
    public void testBasicConnectRequest() {

        // Create a dummy conn handle.
        ByteBuffer bb1 = ByteBuffer.allocate(NativeWorkRequest.LHDL_POINTER_LENGTH);
        bb1.putLong(0, 0x03L);
        bb1.putLong(8, 0x04L);
        LocalCommClientConnHandle connHandle = new LocalCommClientConnHandle(bb1.array());

        // Create the raw data for the NativeWorkRequest.
        ByteBuffer bb = ByteBuffer.allocate(NativeWorkRequest.NATIVE_WORK_ELEMENT_SIZE);
        bb.putLong(NativeWorkRequest.EyeCatcherOffset, NativeWorkRequest.EyeCatcher);
        bb.putLong(NativeWorkRequest.NEXT_WORKREQUEST_OFFSET, 0x02L);
        bb.putShort(NativeWorkRequest.REQUEST_TYPE_OFFSET, (short) NativeWorkRequestType.REQUESTTYPE_CONNECT.getNativeValue());
        ((ByteBuffer) bb.position(NativeWorkRequest.LHDL_POINTER_OFFSET)).put(connHandle.getBytes());
        bb.putLong(NativeWorkRequest.REQUESTSPECIFIC_PARMS_OFFSET + NativeWorkRequest.CONNREQ_SHAREDMEM_USERTOKEN_OFFSET, 0x07L);
        bb.rewind();

        // Create the NativeWorkRequest and verify the parsing.
        NativeWorkRequest nativeWorkRequest = new NativeWorkRequest(bb);

        assertEquals(0x02L, nativeWorkRequest.getNextWorkRequestPtr());
        assertEquals(NativeWorkRequestType.REQUESTTYPE_CONNECT, nativeWorkRequest.getRequestType());
        assertEquals(0, nativeWorkRequest.getRequestFlags());
        assertEquals(0, nativeWorkRequest.getCreateTime());
        assertEquals(0x07L, nativeWorkRequest.getSharedMemoryToken());
        assertArrayEquals(connHandle.getBytes(), nativeWorkRequest.getClientConnectionHandle().getBytes());
        assertNotSame(connHandle, nativeWorkRequest.getClientConnectionHandle());
        assertSame(nativeWorkRequest.getClientConnectionHandle(), nativeWorkRequest.getClientConnectionHandle());
    }

    /**
     * Test basics.
     */
    @Test
    public void testBasicReadReadyRequest() {

        // Create a dummy conn handle.
        ByteBuffer bb1 = ByteBuffer.allocate(NativeWorkRequest.LHDL_POINTER_LENGTH);
        bb1.putLong(0, 0x03L);
        bb1.putLong(8, 0x04L);
        LocalCommClientConnHandle connHandle = new LocalCommClientConnHandle(bb1.array());

        // Create the raw data for the NativeWorkRequest.
        ByteBuffer bb = ByteBuffer.allocate(NativeWorkRequest.NATIVE_WORK_ELEMENT_SIZE);
        bb.putLong(NativeWorkRequest.EyeCatcherOffset, NativeWorkRequest.EyeCatcher);
        bb.putLong(NativeWorkRequest.NEXT_WORKREQUEST_OFFSET, 0x02L);
        bb.putShort(NativeWorkRequest.REQUEST_TYPE_OFFSET, (short) NativeWorkRequestType.REQUESTTYPE_READREADY.getNativeValue());
        ((ByteBuffer) bb.position(NativeWorkRequest.LHDL_POINTER_OFFSET)).put(connHandle.getBytes());

        // Note: despite setting the shared memory user token, getSharedMemoryToken shall return 0,
        // since this is NOT a connect request.
        bb.putLong(NativeWorkRequest.REQUESTSPECIFIC_PARMS_OFFSET + NativeWorkRequest.CONNREQ_SHAREDMEM_USERTOKEN_OFFSET, 0x07L);
        bb.rewind();

        // Create the NativeWorkRequest and verify the parsing.
        NativeWorkRequest nativeWorkRequest = new NativeWorkRequest(bb);

        assertEquals(0x02L, nativeWorkRequest.getNextWorkRequestPtr());
        assertEquals(NativeWorkRequestType.REQUESTTYPE_READREADY, nativeWorkRequest.getRequestType());
        assertEquals(0, nativeWorkRequest.getRequestFlags());
        assertEquals(0, nativeWorkRequest.getCreateTime());
        assertEquals(0, nativeWorkRequest.getSharedMemoryToken());
        assertArrayEquals(connHandle.getBytes(), nativeWorkRequest.getClientConnectionHandle().getBytes());
        assertNotSame(connHandle, nativeWorkRequest.getClientConnectionHandle());
        assertSame(nativeWorkRequest.getClientConnectionHandle(), nativeWorkRequest.getClientConnectionHandle());
    }
}
