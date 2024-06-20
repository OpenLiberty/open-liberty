/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.zos.logging.internal.NativeTraceData;

/**
 *
 */
public class NativeTraceDataTest {
    NativeTraceData nativeTraceData;

    public final static int TRACE_LEVEL_BASIC = 0x00000002;
    public final static int TRACE_POINT = 0x02001002;
    public final static long VALIST_PTR = 0x0000008002222224l;
    public final static long CREATE_TIME = 0x0000008002222229l;
    public final static int CREATE_TCB = 0x00981000;
    public final static int CREATE_STATE = 1;
    public final static int CREATE_KEY = 2;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        byte[] localTraceData = new byte[256];

        ByteBuffer buf = ByteBuffer.allocate(256);
        int traceLevel = TRACE_LEVEL_BASIC;
        buf.putInt(NativeTraceData.TRACE_LEVEL_OFFSET, traceLevel);
        int tracepoint = TRACE_POINT;
        buf.putInt(NativeTraceData.TRACE_POINT_OFFSET, tracepoint);
        long valistPtr = VALIST_PTR;
        buf.putLong(NativeTraceData.VA_LIST_OFFSET, valistPtr);
        long createTime = CREATE_TIME;
        buf.putLong(NativeTraceData.TRACE_CREATE_TIME_OFFSET, createTime);
        int tcbAddress = CREATE_TCB;
        buf.putInt(NativeTraceData.TRACE_CREATE_TCB_OFFSET, tcbAddress);
        int state = CREATE_STATE;
        buf.putInt(NativeTraceData.TRACE_CREATE_STATE_OFFSET, state);
        int key = CREATE_KEY;
        buf.putInt(NativeTraceData.TRACE_CREATE_KEY_OFFSET, key);

        buf.rewind();
        buf.get(localTraceData, 0, 36);

        nativeTraceData = new NativeTraceData(localTraceData);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        nativeTraceData = null;
    }

    /**
     * Test method for {@link com.ibm.ws.zos.logging.internal.NativeTraceData#getTraceLevel()}.
     */
    @Test
    public void test_getTraceLevel() {
        // get the trace level
        int traceLevel = nativeTraceData.getTraceLevel();
        assertEquals(Integer.valueOf(traceLevel), Integer.valueOf(TRACE_LEVEL_BASIC));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.logging.internal.NativeTraceData#getTracePoint()}.
     */
    @Test
    public void test_getTracePoint() {
        // get the trace level
        int tracePoint = nativeTraceData.getTracePoint();
        assertEquals(Integer.valueOf(tracePoint), Integer.valueOf(TRACE_POINT));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.logging.internal.NativeTraceData#getVarargListPointer()}.
     */
    @Test
    public void test_getValistPtr() {
        // get the valist ptr
        long valistPtr = nativeTraceData.getVarargListPointer();
        assertEquals(Long.valueOf(valistPtr), Long.valueOf(VALIST_PTR));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.logging.internal.NativeTraceData#getCreateTime()}.
     */
    @Test
    public void test_getCreateTime() {
        // get the time the trace was created
        long createTime = nativeTraceData.getCreateTime();
        assertEquals(Long.valueOf(createTime), Long.valueOf(CREATE_TIME));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.logging.internal.NativeTraceData#getCreatingTcb()}.
     */
    @Test
    public void test_getCreatingTcb() {
        // get the tcb address
        int tcbAddress = nativeTraceData.getCreatingTcb();
        assertEquals(Integer.valueOf(tcbAddress), Integer.valueOf(CREATE_TCB));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.logging.internal.NativeTraceData#getCreateState()}.
     */
    @Test
    public void test_getCreateState() {
        // get the state
        int state = nativeTraceData.getCreateState();
        assertEquals(Integer.valueOf(state), Integer.valueOf(CREATE_STATE));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.logging.internal.NativeTraceData#getCreateKey()}.
     */
    @Test
    public void test_getCreateKey() {
        // get the key
        int key = nativeTraceData.getCreateKey();
        assertEquals(Integer.valueOf(key), Integer.valueOf(CREATE_KEY));
    }

}
