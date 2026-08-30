/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *
 * Description:  This class implements junit tests to simulate and validate native
 *               trace records that are logged by the Java tracing infrastructure.
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import static com.ibm.ws.zos.logging.internal.NativeTraceHeader.NATIVE_TRACE_KEY_8;
import static com.ibm.ws.zos.logging.internal.NativeTraceHeader.NATIVE_TRACE_PROBLEM_STATE;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.zos.core.utils.DirectBufferHelper;
import com.ibm.ws.zos.logging.internal.NativeTraceHandler.NativeTraceComponentDefinition;

import junit.framework.AssertionFailedError;
import test.common.SharedOutputManager;

public class NativeTraceLogVerifierTest {

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

    //The keys corresponding to the trace handler case statement
    static final int TRC_KEY_RAW_DATA = 1;
    static final int TRC_KEY_EBCDIC_STRING = 2;
    static final int TRC_KEY_INT = 3;
    static final int TRC_KEY_DOUBLE = 4;
    static final int TRC_KEY_POINTER = 5;
    static final int TRC_KEY_LONG = 6;
    static final int TRC_KEY_SHORT = 7;
    static final int TRC_KEY_CHAR = 8;
    static final int TRC_KEY_HEX_INT = 9;
    static final int TRC_KEY_HEX_LONG = 10;

    /*
     * The maximum size of the native C stack. No variable argument list
     * can exceed this size.
     */
    private final static int MAX_BUFFER_SIZE = 4 * 1024 * 1024;
    private static ByteBuffer sourceByteBuffer = null;

    private static final long GIGABYTE = 1L << 30;
    private static int addressCnt = 0;
    private static final int NATIVE_TRACE_LEVEL = 3;
    private static final int RAS_COMP_SERVER = 0x02000000;
    private static final int RAS_COMP_SERVER_MODULE = 0x0200300a;
    private static final int RAS_MODULE_SECURITY_SAF_AUTHORIZATION = (0x03000000 + 0x1000);
    private static final int RAS_COMP_UTILITIES_MVS_UTILS = 0x04002000;
    private static NativeTraceComponentDefinition[] testDefs = null;
    private static final long NATIVE_TRACE_CREATE_TIME = 0x0000000000000001L;
    private static final int NATIVE_TRACE_TCB_ADDRESS = 0x00780000;

    class TestNativeTraceHandler extends NativeTraceHandler {
        private final DirectBufferHelper bufferHelper;

        /**
         * @param bufferHelper
         */
        TestNativeTraceHandler(DirectBufferHelper bufferHelper, NativeTraceComponentDefinition[] testDefs) {
            super(bufferHelper, testDefs);
            this.bufferHelper = bufferHelper;
        }

        @Override
        protected synchronized DirectBufferHelper getDirectBufferInstance() {
            return this.bufferHelper;
        }
    }

    @Test
    public void testWriteNativeTraceDisabled() {

        setTraceSpec("");
        LoggingTestBufferHelper aTestBufferHelper = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(aTestBufferHelper, testDefs);
        long vaListPointer = 1;
        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);
    }

    @Test
    public void testWriteNativeTraceNullPointer() {

        setTraceSpec("zos.native.02=all");
        LoggingTestBufferHelper aTestBufferHelper = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(aTestBufferHelper, testDefs);
        long vaListPointer = 0;
        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(-2L, rc);
    }

    @Test
    public void testWriteNativeTraceBadPointer() {

        setTraceSpec("zos.native.02=all");
        LoggingTestBufferHelper aTestBufferHelper = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(aTestBufferHelper, testDefs);
        long vaListPointer = 12345;
        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(-1L, rc);
    }

    @Test
    public void testWriteNativeTraceInt() {

        //need to produce a real trace record.
        setTraceSpec("zos.native.02=all");

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_INT); //key for trace INT
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //not used
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, 1); // value of int is 1
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_INT";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
            description.position(0);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);

        //validate the log file contents
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceLong() {

        //need to produce a real trace record.
        setTraceSpec("zos.native.02=all");

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_LONG); //key for trace Long
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //not used
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, Long.MAX_VALUE); // value MAX long
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_LONG";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);

        //validate the log file contents
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_LONG: ");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace(new Long(Long.MAX_VALUE).toString());
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceShort() {

        //need to produce a real trace record.
        setTraceSpec("zos.native.02=all");

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_SHORT); //key for trace short
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //not used
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, Short.MAX_VALUE); // value of short
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_SHORT";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);

        //validate the log file contents
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_SHORT: ");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace(new Short(Short.MAX_VALUE).toString());
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceDouble() {

        //need to produce a real trace record.
        setTraceSpec("zos.native.02=all");

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_DOUBLE); //key for trace double
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //not used
        idx = idx + 4;
        sourceByteBuffer.putDouble(idx, Double.MAX_VALUE); // value of short
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_DOUBLE";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);

        //validate the log file contents
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_DOUBLE: ");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace(new Double(Double.MAX_VALUE).toString());
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceChar() {

        //need to produce a real trace record.
        setTraceSpec("zos.native.02=all");

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_CHAR); //key for trace char
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //not used
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, 193L); // EBCDIC Char 'A'
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_CHAR";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);

        //validate the log file contents
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_CHAR: ");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("A");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceEBCDICString() {

        //need to produce a real trace record.
        setTraceSpec("zos.native.02=all");

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        String someDataStr = "Some EBCDIC Data";

        //create a buffer to hold the data in ebcdic
        ByteBuffer dataBuffer = ByteBuffer.allocate(someDataStr.length());
        try {
            dataBuffer.put(someDataStr.getBytes("Cp1047"), 0, someDataStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }

        //add dataBuffer to Map with unique address
        long dataAddress = this.getNextAddress();
        directBufferUtils.addBuffer(dataAddress, dataBuffer);

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_EBCDIC_STRING); //key for EBCDIC Data
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, someDataStr.length()); //length of EBCDIC data string
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, dataAddress); // EBCDIC Data Ptr
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_EBCDIC_STRING";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);

        //validate the log file contents
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_EBCDIC_STRING: ");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("Some EBCDIC Data");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceHexLong() {

        //need to produce a real trace record.
        setTraceSpec("zos.native.02=all");

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_HEX_LONG); //key for trace Hex Long
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //not used
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, Long.MAX_VALUE); // value of Max Long
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_HEX_LONG";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);

        //validate the log file contents
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_HEX_LONG: ");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("7fffffffffffffff");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceHexInt() {

        //need to produce a real trace record.
        setTraceSpec("zos.native.02=all");

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_HEX_INT); //key for trace Hex INT
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //not used
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, 50); // value of int is 50
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_HEX_INT";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);

        //validate the log file contents
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_HEX_INT: 32");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTracePointer() {

        //need to produce a real trace record.
        setTraceSpec("zos.native.02=all");

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_POINTER); //key for trace Hex INT
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //not used
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, vaListPointer); // value of vaListPointer
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_POINTER";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);

        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);

        //validate the log file contents
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_POINTER: ");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace(String.format("%016x", vaListPointer));
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceRawBinaryData() {

        //need to produce a real trace record.
        setTraceSpec("zos.native.02=all");

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        ///create a byte array of size add and add to the buffer
        byte[] aByteArray = { 1, 2, 3, 4, 5, 6, 7, 8 };
        ByteBuffer byteArrayBuffer = ByteBuffer.allocate(8);
        byteArrayBuffer.put(aByteArray);

        //add dataBuffer to Map with unique address
        long byteArrayAddress = this.getNextAddress();
        directBufferUtils.addBuffer(byteArrayAddress, byteArrayBuffer);

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_RAW_DATA); //key for trace Raw Binary Byte Array
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, aByteArray.length); // size off raw data
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, byteArrayAddress); // address of raw data byte array
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_RAW_DATA";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);

        //validate the log file contents
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_RAW_DATA: ");
        assertEquals(true, aBoolean);

        String addressString = String.format("%016x", byteArrayAddress);
        aBoolean = outputMgr.checkForTrace("data_address=" + addressString.substring(0, 8) + "_" + addressString.substring(8, 16) + ", data_length=8");
        assertEquals(true, aBoolean);
    }

    @Test
    public void testWriteNativeTraceMultiple() {

        //need to produce a real trace record.
        setTraceSpec("zos.native.02=all");

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //add first trace entry

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        ///create a byte array of size add and add to the buffer
        byte[] aByteArray = { 1, 2, 3, 4, 5, 6, 7, 8 };
        ByteBuffer byteArrayBuffer = ByteBuffer.allocate(8);
        byteArrayBuffer.put(aByteArray);

        //add dataBuffer to Map with unique address
        long byteArrayAddress = this.getNextAddress();
        directBufferUtils.addBuffer(byteArrayAddress, byteArrayBuffer);

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_RAW_DATA); //key for trace Raw Binary Byte Array
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, aByteArray.length); // size off raw data
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, byteArrayAddress); // address of raw data byte array
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_RAW_DATA";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer
        idx = idx + 8;

        //add second trace entry

        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_POINTER); //key for trace Hex INT
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //not used
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, vaListPointer); // value of vaListPointer
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        descriptionStr = "TRC_KEY_POINTER";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer
        idx = idx + 8;

        //add third entry
        String someDataStr = "Some EBCDIC Data";

        //create a buffer to hold the data in ebcdic
        ByteBuffer dataBuffer = ByteBuffer.allocate(someDataStr.length());
        try {
            dataBuffer.put(someDataStr.getBytes("Cp1047"), 0, someDataStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }

        //add dataBuffer to Map with unique address
        long dataAddress = this.getNextAddress();
        directBufferUtils.addBuffer(dataAddress, dataBuffer);

        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_EBCDIC_STRING); //key for EBCDIC Data
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, someDataStr.length()); //length of EBCDIC data string
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, dataAddress); // EBCDIC Data Ptr
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        descriptionStr = "TRC_KEY_EBCDIC_STRING";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            assertEquals(0L, 1L);
            return;
        }
        //add description to Map with unique address
        descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        //end of trace entries

        int rc = traceHandler.writeNativeTrace(NATIVE_TRACE_LEVEL, RAS_COMP_SERVER, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        assertEquals(0L, rc);

        //validate the log file contents

        //1st entry
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_RAW_DATA: ");
        assertEquals(true, aBoolean);
        String addressString = String.format("%016x", byteArrayAddress);
        aBoolean = outputMgr.checkForTrace("data_address=" + addressString.substring(0, 8) + "_" + addressString.substring(8, 16) + ", data_length=8");
        assertEquals(true, aBoolean);
        //2nd entry
        aBoolean = outputMgr.checkForTrace("TRC_KEY_POINTER: ");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace(String.format("%016x", vaListPointer));
        assertEquals(true, aBoolean);
        //3rd entry
        aBoolean = outputMgr.checkForTrace("TRC_KEY_EBCDIC_STRING: ");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("Some EBCDIC Data");
        assertEquals(true, aBoolean);
    }

    @Test
    public void testWriteNativeTraceSpec1() {

        //Expected output is a trace record is produced as component matches
        setTraceSpec("zos.native.02=all");

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec2() {

        //Expected output is a trace record is not produced as component does not match
        setTraceSpec("zos.native.03=all");

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec3() {

        //Expected output is a trace record is produced as effective trace level is equal
        setTraceSpec("zos.native=debug");

        int nativeTraceLevel = 3;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is produced as effective trace level is higher
        outputMgr.resetStreams();
        nativeTraceLevel = 2;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec4() {

        //fine/event native trace 0,1,2

        //Expected output is a trace record is produced as effective trace level is equal
        setTraceSpec("zos.native=fine");

        int nativeTraceLevel = 2;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is not produced as effective trace level is < native
        outputMgr.resetStreams();
        nativeTraceLevel = 0;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(false, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        //Expected output is a trace record is produced as effective trace level is higher
        outputMgr.resetStreams();
        setTraceSpec("zos.native=event");
        nativeTraceLevel = 1;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is not produced as effective trace level is < native
        outputMgr.resetStreams();
        nativeTraceLevel = 0;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(false, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec5() {

        //finer/finest/debug/dump native trace 2,3

        //Expected output is a trace record is produced as effective trace level is equal
        setTraceSpec("zos.native=finer");

        int nativeTraceLevel = 3;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is produced as effective trace level is > native
        outputMgr.resetStreams();
        nativeTraceLevel = 2;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is produced as effective trace level is equal
        outputMgr.resetStreams();
        setTraceSpec("zos.native=finest");
        nativeTraceLevel = 3;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is not produced as effective trace level is > native
        outputMgr.resetStreams();
        nativeTraceLevel = 1;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is produced as effective trace level is equal
        outputMgr.resetStreams();
        setTraceSpec("zos.native=debug");
        nativeTraceLevel = 3;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is not produced as effective trace level is < native
        outputMgr.resetStreams();
        nativeTraceLevel = 4; //this is bogus as 4 (specific) is no longer supported
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        //Expected output is a trace record is produced as effective trace level is equal
        outputMgr.resetStreams();
        setTraceSpec("zos.native=dump");
        nativeTraceLevel = 3;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is not produced as native trace level is invalid
        outputMgr.resetStreams();
        nativeTraceLevel = 4; //this is bogus as 4 (specific) is no longer supported
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec6() {

        //warning/info/config/severe/fatal/error/audit/detail

        //Expected output is a trace record is not produced for any of these as there is currently
        //no native equivalent

        setTraceSpec("zos.native=warning");

        int nativeTraceLevel = 3;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zos.native=info");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zos.native=config");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zos.native=severe");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zos.native=fatal");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zos.native=error");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zos.native=audit");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zos.native=detail");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zos.native=garbage");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec7() {

        //Expected output is a trace record is produced as component matches
        setTraceSpec("zos.native.02=all");

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        outputMgr.resetStreams();
        //Expected output is a no trace records is produced as trace specs have been removed
        setTraceSpec("");

        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec8() {

        //Expected output is a trace record is produced as component matches
        setTraceSpec("zos.native.server=all");

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec9() {

        //Expected output is a trace record is not produced as component does not match
        setTraceSpec("zos.native.security=all");

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec10() {

        //Expected output is a trace record is produced as effective trace level is equal
        setTraceSpec("zNative=debug");

        int nativeTraceLevel = 3;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is produced as effective trace level is higher
        outputMgr.resetStreams();
        nativeTraceLevel = 2;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec11() {

        //fine/event native trace 0,1,2

        //Expected output is a trace record is produced as effective trace level is equal
        setTraceSpec("zNative=fine");

        int nativeTraceLevel = 2;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is not produced as effective trace level is < native
        outputMgr.resetStreams();
        nativeTraceLevel = 0;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(false, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        //Expected output is a trace record is produced as effective trace level is higher
        outputMgr.resetStreams();
        setTraceSpec("zNative=event");
        nativeTraceLevel = 1;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is not produced as effective trace level is < native
        outputMgr.resetStreams();
        nativeTraceLevel = 0;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(false, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec12() {

        //finer/finest/debug/dump native trace 2,3

        //Expected output is a trace record is produced as effective trace level is equal
        setTraceSpec("zNative=finer");

        int nativeTraceLevel = 3;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is produced as effective trace level is > native
        outputMgr.resetStreams();
        nativeTraceLevel = 2;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is produced as effective trace level is equal
        outputMgr.resetStreams();
        setTraceSpec("zNative=finest");
        nativeTraceLevel = 3;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is produced as effective trace level is > native
        outputMgr.resetStreams();
        nativeTraceLevel = 1;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is produced as effective trace level is equal
        outputMgr.resetStreams();
        setTraceSpec("zNative=debug");
        nativeTraceLevel = 3;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is not produced as effective trace level is < native
        outputMgr.resetStreams();
        nativeTraceLevel = 4; //this is bogus as 4 (specific) is no longer supported
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        //Expected output is a trace record is produced as effective trace level is equal
        outputMgr.resetStreams();
        setTraceSpec("zNative=dump");
        nativeTraceLevel = 3;
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        //Expected output is a trace record is not produced as native trace level is invalid
        outputMgr.resetStreams();
        nativeTraceLevel = 4; //this is bogus as 4 (specific) is no longer supported
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec13() {

        //warning/info/config/severe/fatal/error/audit/detail

        //Expected output is a trace record is not produced for any of these as there is currently
        //no native equivalent

        setTraceSpec("zNative=warning");

        int nativeTraceLevel = 3;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zNative=info");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zNative=config");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zNative=severe");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zNative=fatal");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zNative=error");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zNative=audit");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zNative=detail");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        outputMgr.resetStreams();
        setTraceSpec("zNative=garbage");
        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);
        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec14() {

        //Expected output is a trace record is produced as component matches
        setTraceSpec("zNative=all");

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

        outputMgr.resetStreams();
        //Expected output is a no trace records is produced as trace specs have been removed
        setTraceSpec("");

        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpec15() {

        //Expected output is a trace record is not produced as component does not match
        setTraceSpec("zos.native.security.saf.authorization=event");

        int nativeTraceLevel = 1;
        int compModuleId = RAS_COMP_UTILITIES_MVS_UTILS;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

        //Expected output is a trace record is produced as component matches

        nativeTraceLevel = 1;
        compModuleId = RAS_MODULE_SECURITY_SAF_AUTHORIZATION;

        result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpecHierarchy1() {

        //Expected output is a trace record is produced as module overrides component
        setTraceSpec("zos.native=disabled:zos.native.02=debug");

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER_MODULE;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpecHierarchy2() {

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER_MODULE;

        //Expected output is a trace record is not produced as module level doesn't match
        setTraceSpec("zos.native=disabled:zos.native.03=debug");

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpecHierarchy3() {

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER_MODULE;

        //Expected output is a trace record is not produced as trace level set is lower than native
        setTraceSpec("zos.native=event:zos.native.02.003=fine");

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpecHierarchy4() {

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER_MODULE;

        //Expected output is a trace record is produced as trace level set is equal to native
        setTraceSpec("zos.native=event:zos.native.02.003=finest");

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpecHierarchy5() {

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER_MODULE;

        //Expected output is a trace record is produced as the trace component reflects
        //the combined highest level and zos.native has all enabled.
        setTraceSpec("zos.native.02=all:zos.native.02.003=disabled");

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpecHierarchy6() {

        //Expected output is a trace record is produced as module overrides component
        setTraceSpec("zNative=disabled:zServer=debug");

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER_MODULE;

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpecHierarchy7() {

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER_MODULE;

        //Expected output is a trace record is not produced as module level doesn't match
        setTraceSpec("zNative=disabled:zSecurity=debug");

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpecHierarchy8() {

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER_MODULE;

        //Expected output is a trace record is not produced as trace level set is lower than native
        setTraceSpec("zos.native=event:zos.native.server.sample.functions=fine");

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(false, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(false, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpecHierarchy9() {

        int nativeTraceLevel = 2;
        int compModuleId = RAS_COMP_SERVER_MODULE;

        //Expected output is a trace record is produced as trace level set is equal to native
        setTraceSpec("zos.native=event:zos.native.server.sample.functions=finest");

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

    }

    @Test
    public void testWriteNativeTraceSpecHierarchy10() {

        int nativeTraceLevel = NATIVE_TRACE_LEVEL;
        int compModuleId = RAS_COMP_SERVER_MODULE;

        //Expected output is a trace record is produced as the trace component reflects
        //the combined highest level and zos.native has all enabled.
        setTraceSpec("zos.native.server=all:zos.native.server.sample.functions=disabled");

        boolean result = this.executeTest(nativeTraceLevel, compModuleId);
        assertEquals(true, result);

        //validate the log file contents against test expectation
        boolean aBoolean = outputMgr.checkForTrace("Trace:");
        assertEquals(true, aBoolean);
        aBoolean = outputMgr.checkForTrace("TRC_KEY_INT: 1234");
        assertEquals(true, aBoolean);

    }

    /*
     * This is just a basic test that is used by multiple testcases looking to see if
     * a given trace record was logged or not based on the trace specification provided.
     */
    private boolean executeTest(int nativeTraceLevel, int compModuleId) {

        //prime the Logging writer with the mocked version of the BufferUtils utility
        LoggingTestBufferHelper directBufferUtils = new LoggingTestBufferHelper();
        NativeTraceHandler traceHandler = new TestNativeTraceHandler(directBufferUtils, testDefs);

        //allocate a byte buffer to build a valist structure and add it to the
        //buffer Map.
        long vaListPointer = this.getNextAddress();
        directBufferUtils.addBuffer(vaListPointer, this.getByteBuffer());

        //populate the buffer with the tracing data in valist format
        int idx = 0;
        sourceByteBuffer.putInt(idx, 0);// skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, TRC_KEY_INT); //key for trace INT
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;
        sourceByteBuffer.putInt(idx, 0); //not used
        idx = idx + 4;
        sourceByteBuffer.putLong(idx, 1234); // value of int is 1234
        idx = idx + 8;
        sourceByteBuffer.putInt(idx, 0); //skip 4 slack
        idx = idx + 4;

        String descriptionStr = "TRC_KEY_INT";
        sourceByteBuffer.putInt(idx, descriptionStr.length()); //length of description
        idx = idx + 4;
        //create a buffer to hold the description
        ByteBuffer description = ByteBuffer.allocate(descriptionStr.length());
        try {
            description.put(descriptionStr.getBytes("Cp1047"), 0, descriptionStr.getBytes().length); //add as EBCDIC to simulate native data
            description.position(0);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
        //add description to Map with unique address
        long descAddress = this.getNextAddress();
        directBufferUtils.addBuffer(descAddress, description);
        sourceByteBuffer.putLong(idx, descAddress); // put address of description's buffer

        int rc = traceHandler.writeNativeTrace(nativeTraceLevel, compModuleId, vaListPointer,
                                               NATIVE_TRACE_CREATE_TIME, NATIVE_TRACE_TCB_ADDRESS,
                                               NATIVE_TRACE_PROBLEM_STATE, NATIVE_TRACE_KEY_8);
        if (rc == 0)
            return true;
        else
            return false;

    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        //create some mock Trace component definitions
        NativeTraceComponentDefinition aDef = null;
        NativeTraceComponentDefinition[] defs = new NativeTraceComponentDefinition[5];
        aDef = new NativeTraceComponentDefinition(0x02000000, "zos.native.server", "zNative,zServer");
        defs[0] = aDef;
        aDef = new NativeTraceComponentDefinition(0x03000000, "zos.native.security", "zNative,zSecurity");
        defs[1] = aDef;
        aDef = new NativeTraceComponentDefinition((0x02000000 + 0x3000), "zos.native.server.sample.functions", "zNative,zServer");
        defs[2] = aDef;
        aDef = new NativeTraceComponentDefinition((0x03000000 + 0x1000), "zos.native.security.saf.authorization", null);
        defs[3] = aDef;
        aDef = new NativeTraceComponentDefinition((0x04000000 + 0x2000), "zos.native.tx.rrs.services.jni", "");
        defs[4] = aDef;

        testDefs = defs;
    }

    private long getNextAddress() {

        return (++addressCnt * GIGABYTE);

    }

    private ByteBuffer getByteBuffer() {

        if (sourceByteBuffer == null)
            sourceByteBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);

        Arrays.fill(sourceByteBuffer.array(), (byte) 0);
        sourceByteBuffer.clear();
        return sourceByteBuffer;
    }

    private void setTraceSpec(String trace) {
        try {
            Method m = TrConfigurator.class.getDeclaredMethod("setTraceSpec", String.class);
            m.setAccessible(true);
            m.invoke(null, trace);
        } catch (Exception e) {
            Error error = new AssertionFailedError("Unable to set trace spce");
            error.initCause(e);
            throw error;
        }
    }
}
