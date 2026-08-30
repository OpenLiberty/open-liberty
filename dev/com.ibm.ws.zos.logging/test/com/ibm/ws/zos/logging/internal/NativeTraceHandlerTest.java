/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import static com.ibm.ws.zos.core.utils.DirectBufferHelper.GIGABYTE;
import static com.ibm.ws.zos.logging.internal.NativeTraceHandler.TRC_KEY_CHAR;
import static com.ibm.ws.zos.logging.internal.NativeTraceHandler.TRC_KEY_DOUBLE;
import static com.ibm.ws.zos.logging.internal.NativeTraceHandler.TRC_KEY_EBCDIC_STRING;
import static com.ibm.ws.zos.logging.internal.NativeTraceHandler.TRC_KEY_HEX_INT;
import static com.ibm.ws.zos.logging.internal.NativeTraceHandler.TRC_KEY_HEX_LONG;
import static com.ibm.ws.zos.logging.internal.NativeTraceHandler.TRC_KEY_INT;
import static com.ibm.ws.zos.logging.internal.NativeTraceHandler.TRC_KEY_LONG;
import static com.ibm.ws.zos.logging.internal.NativeTraceHandler.TRC_KEY_POINTER;
import static com.ibm.ws.zos.logging.internal.NativeTraceHandler.TRC_KEY_RAW_DATA;
import static com.ibm.ws.zos.logging.internal.NativeTraceHandler.TRC_KEY_SHORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.zos.core.utils.DirectBufferHelper;

public class NativeTraceHandlerTest {

    final static Charset charset = Charset.forName("IBM-1047");
    final static Random random = new Random(System.currentTimeMillis());
    final static Map<Integer, String> TRACE_DESCRIPTIONS = getTraceDescriptions();
    final static Map<Long, ByteBuffer> TRACE_DESCRIPTION_BUFFERS = getTraceDescriptionBuffers();

    class HandlerTestBufferHelper extends LoggingTestBufferHelper {
        @Override
        public ByteBuffer getSlice(long address, int length) {
            int segmentOffset = getSegmentOffset(address);
            ByteBuffer segment = getSegment(address);
            ByteBuffer slice = segment.duplicate();
            slice.rewind();
            slice.limit(Math.min(length, slice.capacity()));
            slice.position(segmentOffset);
            return slice;
        }
    }

    class NativeData {
        private final int traceLevel;
        private final int tracePoint;
        private final long argListAddress;
        private final int ntv_traceWrittenRc;
        private final int ntv_stopRc;

        NativeData(int traceLevel, int tracePoint, long argListAddress,
                   int ntv_traceWrittenRc, int ntv_stopRc) {
            this.traceLevel = traceLevel;
            this.tracePoint = tracePoint;
            this.argListAddress = argListAddress;
            this.ntv_traceWrittenRc = ntv_traceWrittenRc;
            this.ntv_stopRc = ntv_stopRc;
        }

        int getTraceLevel() {
            return traceLevel;
        }

        int getTracePoint() {
            return tracePoint;
        }

        long getAargListAddress() {
            return argListAddress;
        }

        int getNtv_traceWrittenRc() {
            return ntv_traceWrittenRc;
        }

        int getNtv_stopRc() {
            return ntv_stopRc;
        }
    }

    class TestNativeTraceHandler extends NativeTraceHandler {
        public final static long THREAD_ELEMENT_PTR = 0x0000008002222224l;

        private NativeData nativeData;

        private final DirectBufferHelper bufferHelper;

        /**
         * @param bufferHelper
         */
        TestNativeTraceHandler(DirectBufferHelper bufferHelper) {
            super(bufferHelper, null);
            this.bufferHelper = bufferHelper;
        }

        @Override
        protected synchronized DirectBufferHelper getDirectBufferInstance() {
            return this.bufferHelper;
        }

        void setNativeData(NativeData nativeData) {
            this.nativeData = nativeData;
        }

        @Override
        protected long ntv_getThreadData() {
            //return 0x0000008002222225l; // test failure
            return THREAD_ELEMENT_PTR;
        }

        @Override
        protected byte[] ntv_getTraces(long threadElementPtr) {
            if (threadElementPtr == THREAD_ELEMENT_PTR) {
                ntv_getTraces_parm_good = true;
            } else {
                ntv_getTraces_parm_good = false;
            }
            byte[] localTraceData = new byte[256];

            ByteBuffer buf = ByteBuffer.allocate(256);

            buf.putInt(NativeTraceData.TRACE_LEVEL_OFFSET, nativeData.getTraceLevel());

            buf.putInt(NativeTraceData.TRACE_POINT_OFFSET, nativeData.getTracePoint());

            int expectedValue = 9999999;

            int traceKey = TRC_KEY_INT;
            DataOutputStream dos;
            ByteBuffer argList = null;
            try {
                dos = va_start();

                addPrimitiveTraceData(dos, traceKey, (short) (Integer.SIZE / Byte.SIZE), expectedValue);
                argList = va_end(dos);
            } catch (IOException e) {
                e.printStackTrace();
            }

            loggingTestBufferHelper.addBuffer(nativeData.getAargListAddress(), argList);

            buf.putLong(NativeTraceData.VA_LIST_OFFSET, nativeData.getAargListAddress());

            buf.rewind();
            buf.get(localTraceData, 0, 16);
            return localTraceData;
        }

        @Override
        protected int ntv_stopListeningForTraces(long threadElementPtr) {
            if (threadElementPtr == THREAD_ELEMENT_PTR) {
                ntv_stopListeningForTraces_parm_good = true;
            } else {
                ntv_stopListeningForTraces_parm_good = false;
            }
            return nativeData.getNtv_stopRc();
        }

        @Override
        protected int ntv_traceWritten(long threadElementPtr) {
            if (threadElementPtr == THREAD_ELEMENT_PTR) {
                ntv_traceWritten_parm_good = true;
            } else {
                ntv_traceWritten_parm_good = false;
            }
            synchronized (thisThread) {
                thisThread.notify();
            }
            return nativeData.getNtv_traceWrittenRc();
        }
    }

    static Map<Integer, String> getTraceDescriptions() {
        Map<Integer, String> desc = new HashMap<Integer, String>();

        desc.put(0, "Overall trace description");
        desc.put(TRC_KEY_RAW_DATA, "Raw data description");
        desc.put(TRC_KEY_EBCDIC_STRING, "EBCDIC string description");
        desc.put(TRC_KEY_RAW_DATA, "Raw data description");
        desc.put(TRC_KEY_EBCDIC_STRING, "EBCDIC string description");
        desc.put(TRC_KEY_INT, "Integer description");
        desc.put(TRC_KEY_DOUBLE, "Double description");
        desc.put(TRC_KEY_POINTER, "Pointer description");
        desc.put(TRC_KEY_LONG, "Long description");
        desc.put(TRC_KEY_SHORT, "Short description");
        desc.put(TRC_KEY_CHAR, "Character description");
        desc.put(TRC_KEY_HEX_INT, "Hex integer description");
        desc.put(TRC_KEY_HEX_LONG, "Hex long description");
        desc.put(TRC_KEY_HEX_LONG + 1, "Unknown key description");

        return desc;
    }

    final static long TRACE_DESCRIPTION_BASE = 0;
    final static long TRACE_DATA_BASE = 50;

    static long getDescriptionBufferAddress(int traceKey) {
        return (TRACE_DESCRIPTION_BASE + traceKey) * GIGABYTE;
    }

    static long getReferencedDataAddress(int traceKey) {
        return (TRACE_DATA_BASE + traceKey) * GIGABYTE;
    }

    static Map<Long, ByteBuffer> getTraceDescriptionBuffers() {
        Map<Long, ByteBuffer> buffers = new HashMap<Long, ByteBuffer>();

        for (Map.Entry<Integer, String> entry : getTraceDescriptions().entrySet()) {
            buffers.put(getDescriptionBufferAddress(entry.getKey()), charset.encode(entry.getValue()));
        }

        return buffers;
    }

    static void addPrimitiveTraceData(DataOutputStream dos, int traceKey, short itemLength, long item) throws IOException {
        // Random data is used to represent possibly dirty storage
        dos.writeInt(random.nextInt());
        dos.writeInt(traceKey);
        dos.writeInt(random.nextInt());
        dos.writeInt(itemLength);
        switch (itemLength) {
            case 8:
                dos.writeLong(item);
                break;
            case 4:
                dos.writeInt(random.nextInt());
                dos.writeInt((int) item);
                break;
            case 2:
                dos.writeInt(random.nextInt());
                dos.writeShort((short) random.nextInt());
                dos.writeShort((short) item);
                break;
        }
        dos.writeInt(random.nextInt());
        dos.writeInt(TRACE_DESCRIPTION_BUFFERS.get(getDescriptionBufferAddress(traceKey)).capacity());
        dos.writeLong(getDescriptionBufferAddress(traceKey));
    }

    static void addReferencedTraceData(DataOutputStream dos, int traceKey, ByteBuffer item, long address) throws IOException {
        // Random data is used to represent possibly dirty storage
        dos.writeInt(random.nextInt());
        dos.writeInt(traceKey);
        dos.writeInt(random.nextInt());
        dos.writeInt(item.capacity());
        dos.writeLong(address);
        dos.writeInt(random.nextInt());
        dos.writeInt(TRACE_DESCRIPTION_BUFFERS.get(getDescriptionBufferAddress(traceKey)).capacity());
        dos.writeLong(getDescriptionBufferAddress(traceKey));
    }

    LoggingTestBufferHelper loggingTestBufferHelper;
    TestNativeTraceHandler nativeTraceHandler;
    Map<DataOutputStream, ByteArrayOutputStream> dataStreams;
    Thread thisThread;
    boolean ntv_getTraces_parm_good;
    boolean ntv_stopListeningForTraces_parm_good;
    boolean ntv_traceWritten_parm_good;

    DataOutputStream va_start() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dataStreams.put(dos, baos);

        return dos;
    }

    ByteBuffer va_end(DataOutputStream dos) throws IOException {
        dos.writeLong(0L);

        return ByteBuffer.wrap(dataStreams.get(dos).toByteArray());
    }

    @Before
    public void setUp() throws Exception {
        loggingTestBufferHelper = new HandlerTestBufferHelper();
        nativeTraceHandler = new TestNativeTraceHandler(loggingTestBufferHelper);
        dataStreams = new HashMap<DataOutputStream, ByteArrayOutputStream>();
        loggingTestBufferHelper.buffers.putAll(TRACE_DESCRIPTION_BUFFERS);
        ntv_getTraces_parm_good = false;
        ntv_stopListeningForTraces_parm_good = false;
        ntv_traceWritten_parm_good = false;
    }

    @After
    public void tearDown() throws Exception {
        loggingTestBufferHelper = null;
        nativeTraceHandler = null;
        dataStreams = null;
    }

    @Test
    public void testTraceDataRawData() throws Exception {
        ByteBuffer rawdataBuffer = ByteBuffer.allocate(1024);
        long rawdataAddress = 64 * GIGABYTE;
        for (int i = 0; i < rawdataBuffer.capacity(); i++) {
            rawdataBuffer.put(i, (byte) i);
        }
        loggingTestBufferHelper.addBuffer(rawdataAddress, rawdataBuffer);

        int traceKey = TRC_KEY_RAW_DATA;
        DataOutputStream dos = va_start();
        addReferencedTraceData(dos, traceKey, rawdataBuffer, rawdataAddress);
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
    };

    @Test
    public void testTraceDataString() throws IOException {
        String expectedString = "My IBM-1047 string data is short and sweet.";
        ByteBuffer ebcdicStringBuffer = charset.encode(expectedString);
        long ebcdicStringAddress = 64 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(ebcdicStringAddress, ebcdicStringBuffer);

        int traceKey = TRC_KEY_EBCDIC_STRING;
        DataOutputStream dos = va_start();
        addReferencedTraceData(dos, traceKey, ebcdicStringBuffer, ebcdicStringAddress);
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals(expectedString, tracedData.getItem());
        assertTrue(tracedData.getFormatted().contains(expectedString));
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataInt() throws IOException {
        int expectedValue = 9999999;

        int traceKey = TRC_KEY_INT;
        DataOutputStream dos = va_start();
        addPrimitiveTraceData(dos, traceKey, (short) (Integer.SIZE / Byte.SIZE), expectedValue);
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals(Integer.valueOf(expectedValue), tracedData.getItem());
        assertTrue(tracedData.getFormatted().contains(Integer.toString(expectedValue)));
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataDouble() throws IOException {
        double expectedValue = 1234.5678;

        int traceKey = TRC_KEY_DOUBLE;
        DataOutputStream dos = va_start();
        addPrimitiveTraceData(dos, traceKey, (short) (Double.SIZE / Byte.SIZE), Double.doubleToLongBits(expectedValue));
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals(Double.valueOf(expectedValue), tracedData.getItem());
        assertTrue(tracedData.getFormatted().contains(Double.toString(expectedValue)));
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataDoubleNaN() throws IOException {
        double expectedValue = Double.NaN;

        int traceKey = TRC_KEY_DOUBLE;
        DataOutputStream dos = va_start();
        addPrimitiveTraceData(dos, traceKey, (short) (Double.SIZE / Byte.SIZE), Double.doubleToLongBits(expectedValue));
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals(Double.valueOf(expectedValue), tracedData.getItem());
        assertTrue(tracedData.getFormatted().contains(Double.toString(expectedValue)));
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataDoublePositiveInfinity() throws IOException {
        double expectedValue = Double.POSITIVE_INFINITY;

        int traceKey = TRC_KEY_DOUBLE;
        DataOutputStream dos = va_start();
        addPrimitiveTraceData(dos, traceKey, (short) (Double.SIZE / Byte.SIZE), Double.doubleToLongBits(expectedValue));
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals(Double.valueOf(expectedValue), tracedData.getItem());
        assertTrue(tracedData.getFormatted().contains(Double.toString(expectedValue)));
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataDoubleNegativeInfinity() throws IOException {
        double expectedValue = Double.NEGATIVE_INFINITY;

        int traceKey = TRC_KEY_DOUBLE;
        DataOutputStream dos = va_start();
        addPrimitiveTraceData(dos, traceKey, (short) (Double.SIZE / Byte.SIZE), Double.doubleToLongBits(expectedValue));
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals(Double.valueOf(expectedValue), tracedData.getItem());
        assertTrue(tracedData.getFormatted().contains(Double.toString(expectedValue)));
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataPointer() throws IOException {
        long expectedValue = 0x1cafebabeL;

        int traceKey = TRC_KEY_POINTER;
        DataOutputStream dos = va_start();
        addPrimitiveTraceData(dos, traceKey, (short) (Long.SIZE / Byte.SIZE), expectedValue);
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals(Long.valueOf(expectedValue), tracedData.getItem());
        assertTrue(tracedData.getFormatted().contains(Long.toHexString(expectedValue)));
        assertEquals(16, tracedData.getFormatted().length());
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataLong() throws IOException {
        long expectedValue = 1234567890;

        int traceKey = TRC_KEY_LONG;
        DataOutputStream dos = va_start();
        addPrimitiveTraceData(dos, traceKey, (short) (Long.SIZE / Byte.SIZE), expectedValue);
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals(Long.valueOf(expectedValue), tracedData.getItem());
        assertTrue(tracedData.getFormatted().contains(Long.toString(expectedValue)));
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataShort() throws IOException {
        short expectedValue = Short.MAX_VALUE;

        int traceKey = TRC_KEY_SHORT;
        DataOutputStream dos = va_start();
        addPrimitiveTraceData(dos, traceKey, (short) (Short.SIZE / Byte.SIZE), expectedValue);
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals(Short.valueOf(expectedValue), tracedData.getItem());
        assertTrue(tracedData.getFormatted().contains(Short.toString(expectedValue)));
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataChar() throws IOException {
        char expectedValue = 0xC9; // EBCDIC 'I'

        int traceKey = TRC_KEY_CHAR;
        DataOutputStream dos = va_start();
        addPrimitiveTraceData(dos, traceKey, (short) (Character.SIZE / Byte.SIZE), expectedValue);
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals('I', tracedData.getItem());
        assertEquals('I', tracedData.getFormatted().charAt(0));
        assertEquals(1, tracedData.getFormatted().length());
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataHexInt() throws IOException {
        int expectedValue = 0xcafebabe;

        int traceKey = TRC_KEY_HEX_INT;
        DataOutputStream dos = va_start();
        addPrimitiveTraceData(dos, traceKey, (short) (Integer.SIZE / Byte.SIZE), expectedValue);
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals(Integer.valueOf(expectedValue), tracedData.getItem());
        assertTrue(tracedData.getFormatted().contains(Integer.toHexString(expectedValue)));
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataHexLong() throws IOException {
        long expectedValue = 0x100cafebabeL;

        int traceKey = TRC_KEY_HEX_LONG;
        DataOutputStream dos = va_start();
        addPrimitiveTraceData(dos, traceKey, (short) (Long.SIZE / Byte.SIZE), expectedValue);
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals(TRACE_DESCRIPTIONS.get(traceKey), tracedData.getDescription());
        assertEquals(Long.valueOf(expectedValue), tracedData.getItem());
        assertTrue(tracedData.getFormatted().contains(Long.toHexString(expectedValue)));
        assertTrue(tracedData.toString().contains(tracedData.getDescription()));
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testTraceDataUnknown() throws IOException {
        int traceKey = 999;
        DataOutputStream dos = va_start();
        dos.writeInt(random.nextInt());
        dos.writeInt(traceKey);
        dos.writeLong(random.nextLong());
        dos.writeLong(random.nextLong());
        dos.writeInt(random.nextInt());
        dos.writeInt(TRACE_DESCRIPTION_BUFFERS.get(getDescriptionBufferAddress(TRC_KEY_RAW_DATA)).capacity());
        dos.writeLong(getDescriptionBufferAddress(TRC_KEY_RAW_DATA));
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(1, traceObjects.size());

        TracedData tracedData = traceObjects.get(0);
        assertNotNull(tracedData);
        assertEquals(traceKey, tracedData.getTraceKey());
        assertEquals("<UNKNOWN TRACE KEY>", tracedData.getFormatted());
        assertTrue(tracedData.toString().contains(tracedData.getFormatted()));
    }

    @Test
    public void testGetTraceObjectsEmpty() throws IOException {
        DataOutputStream dos = va_start();
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertTrue(traceObjects.isEmpty());
    }

    @Test
    public void testGetTraceObjectsComplex() throws IOException {
        ByteBuffer rawdataBuffer = ByteBuffer.allocate(1099);
        long rawdataAddress = 64 * GIGABYTE;
        for (int i = 0; i < rawdataBuffer.capacity(); i++) {
            rawdataBuffer.put(i, (byte) i);
        }
        loggingTestBufferHelper.addBuffer(rawdataAddress, rawdataBuffer);

        String expectedString = "My IBM-1047 string data is short and sweet.";
        ByteBuffer ebcdicStringBuffer = charset.encode(expectedString);
        long ebcdicStringAddress = 65 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(ebcdicStringAddress, ebcdicStringBuffer);

        DataOutputStream dos = va_start();
        addReferencedTraceData(dos, TRC_KEY_RAW_DATA, rawdataBuffer, rawdataAddress);
        addReferencedTraceData(dos, TRC_KEY_EBCDIC_STRING, ebcdicStringBuffer, ebcdicStringAddress);
        addPrimitiveTraceData(dos, TRC_KEY_INT, (short) (Integer.SIZE / Byte.SIZE), Integer.MIN_VALUE);
        addPrimitiveTraceData(dos, TRC_KEY_DOUBLE, (short) (Double.SIZE / Byte.SIZE), Double.doubleToLongBits(Double.MAX_VALUE));
        addPrimitiveTraceData(dos, TRC_KEY_POINTER, (short) (Long.SIZE / Byte.SIZE), Long.MAX_VALUE);
        addPrimitiveTraceData(dos, TRC_KEY_LONG, (short) (Long.SIZE / Byte.SIZE), Long.MIN_VALUE);
        addPrimitiveTraceData(dos, TRC_KEY_SHORT, (short) (Short.SIZE / Byte.SIZE), Short.MAX_VALUE);
        addPrimitiveTraceData(dos, TRC_KEY_CHAR, (short) (Character.SIZE / Byte.SIZE), ' ');
        addPrimitiveTraceData(dos, TRC_KEY_HEX_INT, (short) (Integer.SIZE / Byte.SIZE), Integer.MAX_VALUE);
        addPrimitiveTraceData(dos, TRC_KEY_HEX_LONG, (short) (Long.SIZE / Byte.SIZE), Long.MAX_VALUE);
        ByteBuffer argList = va_end(dos);

        long argListAddress = 100 * GIGABYTE;
        loggingTestBufferHelper.addBuffer(argListAddress, argList);

        List<TracedData> traceObjects = nativeTraceHandler.getTraceObjects(argListAddress);
        assertEquals(10, traceObjects.size());
        for (int i = 0; i < TRC_KEY_HEX_LONG; i++) {
            TracedData tracedData = traceObjects.get(i);
            assertNotNull(tracedData);
            assertNotNull(tracedData.toString());
            assertEquals(i + 1, tracedData.getTraceKey());
            assertEquals(TRACE_DESCRIPTIONS.get(tracedData.getTraceKey()), tracedData.getDescription());
        }
    }

    @Test
    public void testAsDoubleGutterEmpty() {
        byte[] empty = new byte[0];
        long address = 0xdeadbeefL;
        String formatted = nativeTraceHandler.asDoubleGutter(address, empty);

        // Formatted string should only contain address and length
        assertTrue(formatted, formatted.contains("data_address=00000000_deadbeef"));
        assertTrue(formatted, formatted.contains("data_length=0"));
    }

    @Test
    public void testAsDoubleGutterThreeBytesEbcdic() {
        byte[] test = { (byte) 0xC9, (byte) 0xC2, (byte) 0xD4 };
        long address = 0xcafeb00bL;
        String formatted = nativeTraceHandler.asDoubleGutter(address, test);

        String[] lines = formatted.split("\n");
        for (int i = 1; i < lines.length; i++) {
            assertEquals(78, lines[i].length());
        }
        assertTrue(lines[0].contains("data_address=00000000_cafeb00b"));
        assertTrue(lines[0].contains("data_length=3"));
        assertTrue(lines[1].matches("^\\s*\\+\\-{74}\\+$"));
        assertTrue(lines[2].matches("^\\s*\\|OSet\\| A=\\w{16} Length=\\d{7} \\|\\s+EBCDIC\\s+\\|\\s+ASCII\\s+\\|$"));
        assertTrue(lines[4].matches("^\\s*\\|0000\\|C9C2D4\\s+\\|IBM.*"));
    }

    @Test
    public void testAsDoubleGutterThreeBytesAscii() {
        byte[] test = { 'I', 'B', 'M' };
        long address = 0x0badf00d;
        String formatted = nativeTraceHandler.asDoubleGutter(address, test);
        System.out.println(formatted);

        String[] lines = formatted.split("\n");
        for (int i = 1; i < lines.length; i++) {
            assertEquals(78, lines[i].length());
        }
        assertTrue(lines[4].matches("\\s*\\|0000\\|49424D\\s+\\|\\S{3}.*\\|IBM.*"));
    }

    @Test
    public void testTraceListenerThread() {
        int traceLevel = 0x00000002;
        int tracePoint = 0x02001002;
        long argListAddress = 100 * GIGABYTE;
        NativeData nativeData = new NativeData(traceLevel, tracePoint, argListAddress, 0, 0);
        nativeTraceHandler.setNativeData(nativeData);
        nativeTraceHandler.startTraceHandlerThread();
        thisThread = Thread.currentThread();
        try {
            synchronized (thisThread) {
                thisThread.wait();
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        nativeTraceHandler.stopTraceHandlerWriter();
        assertTrue(ntv_getTraces_parm_good);
        assertTrue(ntv_stopListeningForTraces_parm_good);
        assertTrue(ntv_traceWritten_parm_good);
    }

    // @Test fails intermittently need to figure out why.
    // I think it is a timing issue where setUp has run and turned off the
    // flags and the thread from the previous test is still running and
    // calls ntv_traceWritten which notifies the test thread which then
    // continues before the newly created thread has had a chance to call ntv_traceWritten.
    public void testTraceListenerThreadBadTracePoint() {
        int traceLevel = 0x00000001;
        int tracePoint = 0x00000000;
        long argListAddress = 100 * GIGABYTE;
        NativeData nativeData = new NativeData(traceLevel, tracePoint, argListAddress, 0, 0);
        nativeTraceHandler.setNativeData(nativeData);
        nativeTraceHandler.startTraceHandlerThread();
        thisThread = Thread.currentThread();
        try {
            synchronized (thisThread) {
                thisThread.wait();
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        nativeTraceHandler.stopTraceHandlerWriter();
        assertTrue(ntv_getTraces_parm_good);
        assertTrue(ntv_stopListeningForTraces_parm_good);
        assertTrue(ntv_traceWritten_parm_good);
    }

    //@Test
    public void testTraceListenerThreadBadTraceLevel() {
        int traceLevel = 0x00000000;
        int tracePoint = 0x02001002;
        long argListAddress = 100 * GIGABYTE;
        NativeData nativeData = new NativeData(traceLevel, tracePoint, argListAddress, 0, 0);
        nativeTraceHandler.setNativeData(nativeData);

        nativeTraceHandler.startTraceHandlerThread();
        thisThread = Thread.currentThread();
        try {
            synchronized (thisThread) {
                thisThread.wait();
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        nativeTraceHandler.stopTraceHandlerWriter();
        assertTrue(ntv_getTraces_parm_good);
        assertTrue(ntv_stopListeningForTraces_parm_good);
        assertTrue(ntv_traceWritten_parm_good);
    }

    //@Test
    public void testTraceListenerThreadBadArgList() {
        int traceLevel = 0x00000003;
        int tracePoint = 0x02001002;
        long argListAddress = 0x0000000000000000L;
        NativeData nativeData = new NativeData(traceLevel, tracePoint, argListAddress, 0, 0);
        nativeTraceHandler.setNativeData(nativeData);

        nativeTraceHandler.startTraceHandlerThread();
        thisThread = Thread.currentThread();
        try {
            synchronized (thisThread) {
                thisThread.wait();
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        nativeTraceHandler.stopTraceHandlerWriter();
        assertTrue(ntv_getTraces_parm_good);
        assertTrue(ntv_stopListeningForTraces_parm_good);
        assertTrue(ntv_traceWritten_parm_good);
    }

    //@Test
    public void testTraceListenerThread_ntv_traceWrittenRc8() {
        int traceLevel = 0x00000002;
        int tracePoint = 0x02001002;
        long argListAddress = 100 * GIGABYTE;
        NativeData nativeData = new NativeData(traceLevel, tracePoint, argListAddress, 8, 0);
        nativeTraceHandler.setNativeData(nativeData);
        nativeTraceHandler.startTraceHandlerThread();
        thisThread = Thread.currentThread();
        try {
            synchronized (thisThread) {
                thisThread.wait();
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        nativeTraceHandler.stopTraceHandlerWriter();
        assertTrue(ntv_getTraces_parm_good);
        assertTrue(ntv_stopListeningForTraces_parm_good);
        assertTrue(ntv_traceWritten_parm_good);
    }

    //@Test
    public void testTraceListenerThread_ntv_stopRc8() {
        int traceLevel = 0x00000002;
        int tracePoint = 0x02001002;
        long argListAddress = 100 * GIGABYTE;
        NativeData nativeData = new NativeData(traceLevel, tracePoint, argListAddress, 0, 8);
        nativeTraceHandler.setNativeData(nativeData);
        nativeTraceHandler.startTraceHandlerThread();
        thisThread = Thread.currentThread();
        try {
            synchronized (thisThread) {
                thisThread.wait();
            }
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        nativeTraceHandler.stopTraceHandlerWriter();
        assertTrue(ntv_getTraces_parm_good);
        assertTrue(ntv_stopListeningForTraces_parm_good);
        assertTrue(ntv_traceWritten_parm_good);
    }

}
