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

import static com.ibm.ws.zos.logging.internal.NativeTraceHeader.NATIVE_TRACE_KEY_2;
import static com.ibm.ws.zos.logging.internal.NativeTraceHeader.NATIVE_TRACE_KEY_8;
import static com.ibm.ws.zos.logging.internal.NativeTraceHeader.NATIVE_TRACE_PROBLEM_STATE;
import static com.ibm.ws.zos.logging.internal.NativeTraceHeader.NATIVE_TRACE_SUPERVISOR_STATE;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

/**
 * Test NativeTraceHeader class.
 */
public class NativeTraceHeaderTest {

    private static final String TEST_DATA_PATH = "../com.ibm.ws.zos.core_test/build/unittest/test_data/";
    private static final File TEST_DATA = new File(TEST_DATA_PATH);

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().logTo(TEST_DATA);

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private static final int TRACE_POINT = 0x02003001;
    private static final int TCB_ADDRESS = 0x00ab1000;
    private static final long TRACE_CREATE_TIME = 0xc89abe40c6c2bc1aL;

    /**
     * Test method for {@link com.ibm.ws.zos.logging.internal.NativeTraceHeader#toString()}.
     */
    @Test
    public void test_toStringProblemStateKey8() {
        StringBuilder sb = new StringBuilder(" t=");
        sb.append(Integer.toHexString(TCB_ADDRESS)).append(" key=P8 (").append(Integer.toHexString(TRACE_POINT)).append(")");
        String expectedString = sb.toString();

        NativeTraceHeader nativeTraceHeader = new NativeTraceHeader(TRACE_POINT, TCB_ADDRESS, NATIVE_TRACE_KEY_8, NATIVE_TRACE_PROBLEM_STATE, TRACE_CREATE_TIME);

        String traceHeader = nativeTraceHeader.toString();
        System.out.println(traceHeader);

        checkHeaderDate(traceHeader);

        assertTrue("Trace header should end with expected string=" + expectedString,
                   traceHeader.endsWith(expectedString));
    }

    /**
     * Test method for {@link com.ibm.ws.zos.logging.internal.NativeTraceHeader#toString()}.
     */
    @Test
    public void test_toStringSupervisorStateKey2() {
        StringBuilder sb = new StringBuilder(" t=");
        sb.append(Integer.toHexString(TCB_ADDRESS)).append(" key=S2 (").append(Integer.toHexString(TRACE_POINT)).append(")");
        String expectedString = sb.toString();

        NativeTraceHeader nativeTraceHeader = new NativeTraceHeader(TRACE_POINT, TCB_ADDRESS, NATIVE_TRACE_KEY_2, NATIVE_TRACE_SUPERVISOR_STATE, TRACE_CREATE_TIME);
        String traceHeader = nativeTraceHeader.toString();
        System.out.println(traceHeader);

        checkHeaderDate(traceHeader);

        assertTrue("Trace header should end with expected string=" + expectedString,
                   traceHeader.endsWith(expectedString));
    }

    private void checkHeaderDate(String traceHeader) {
        assertTrue("Trace header should start with Trace:, header=" + traceHeader,
                   traceHeader.startsWith("Trace:"));

        assertTrue("Trace header should contain 10, header=" + traceHeader,
                   traceHeader.contains("10"));

        assertTrue("Trace header should contain 31, header=" + traceHeader,
                   traceHeader.contains("31"));

        assertTrue("Trace header should contain 11, header=" + traceHeader,
                   traceHeader.contains("11"));

        assertTrue("Trace header should contain :31:18:960, header=" + traceHeader,
                   traceHeader.contains(":31:18:960"));
    }
}
