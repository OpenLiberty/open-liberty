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

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.internal.HttpResponseMessageImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.bytebuffer.WsByteBufferUtils;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

/**
 * Junit test cases for the GenericUtils class.
 */
public class GenericUtilsTest {
    private static SharedOutputManager outputMgr;

    /** test array length */
    private static final int TEST_ARRAY_LENGTH = 3;

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
     * Main test.
     */
    @Test
    public void testMain() {
        try {
            WsByteBufferPoolManager mgr = ChannelFrameworkFactory.getBufferManager();
            WsByteBuffer[] testWsbbArr = new WsByteBuffer[TEST_ARRAY_LENGTH];
            WsByteBuffer buffer = null;
            HttpResponseMessageImpl response = new HttpResponseMessageImpl();

            // @Tested API - fourBytes(int)
            int val = 9065;
            byte[] fourBytesRepr = GenericUtils.asBytes(val);
            assertEquals(fourBytesRepr[0], 0);
            assertEquals(fourBytesRepr[1], 0);
            assertEquals(fourBytesRepr[2], 35);
            assertEquals(fourBytesRepr[3], 105);

            // setup
            String testStr = "This_is_a_test";
            int testStrLen = testStr.length();
            int offset = 5;
            buffer = mgr.wrap(ByteBuffer.wrap(testStr.getBytes()));

            // @Tested API getBytes( WsByteBuffer, byte[], int)...adequate space
            byte[] dst = new byte[5];
            int numBytesWritten = GenericUtils.getBytes(buffer, dst, 0);
            assertEquals(dst.length, numBytesWritten);

            // @Tested API getBytes( WsByteBuffer, byte[], int)...inadequate space
            dst = new byte[21];
            buffer.rewind();
            numBytesWritten = GenericUtils.getBytes(buffer, dst, 0);
            assertEquals(testStrLen, numBytesWritten);

            // @Tested API getBytes( WsByteBuffer, byte[], int)...with offset
            buffer.rewind();
            numBytesWritten = GenericUtils.getBytes(buffer, dst, offset);
            assertEquals(testStrLen + offset, numBytesWritten);
            byte[] resultArr =
                            GenericUtils.byteSubstring(dst, 5, numBytesWritten);
            assertEquals(new String(resultArr), testStr);

            // setup
            int testVal = 9065;
            int i = 0;

            // @Tested API putInt(WsByteBuffer[], int , BNFHeadersImpl)
            // ..adequate space
            buffer = mgr.wrap(ByteBuffer.allocate(4));
            testWsbbArr[TEST_ARRAY_LENGTH - 1] = buffer;
            testWsbbArr = GenericUtils.putInt(testWsbbArr, testVal, response);
            byte[] result =
                            WsByteBufferUtils.asByteArray(testWsbbArr[TEST_ARRAY_LENGTH - 1]);
            byte[] expected = GenericUtils.asBytes(testVal);
            for (i = 0; i < expected.length & i < result.length; i++) {
                assertEquals(result[i], expected[i]);
            }

            // @Tested API putInt(WsByteBuffer[], int , BNFHeadersImpl)
            // ..inadequate space
            buffer = mgr.wrap(ByteBuffer.allocate(2));
            testWsbbArr[TEST_ARRAY_LENGTH - 1] = buffer;
            testWsbbArr =
                            GenericUtils.putInt(testWsbbArr, testVal, response);
            // first two bytes
            byte[] resultFirstPart =
                            WsByteBufferUtils.asByteArray(testWsbbArr[TEST_ARRAY_LENGTH - 1]);
            for (i = 0; i < expected.length & i < resultFirstPart.length; i++) {
                assertEquals(resultFirstPart[i], expected[i]);
            }
            // next two bytes
            byte[] resultSecondPart =
                            WsByteBufferUtils.asByteArray(testWsbbArr[TEST_ARRAY_LENGTH]);
            resultSecondPart =
                            GenericUtils.byteSubstring(resultSecondPart, 0, 2);
            for (; i < expected.length & i < resultSecondPart.length; i++) {
                assertEquals(resultSecondPart[i], expected[i]);
            }

            int expectedValue = 9065;
            fourBytesRepr = GenericUtils.asBytes(expectedValue);
            // @Tested API asInt(byte[0,0,35,140])...
            int returnedValue = GenericUtils.asInt(fourBytesRepr);
            assertEquals(returnedValue, expectedValue);

            expectedValue = 131800;
            fourBytesRepr = GenericUtils.asBytes(expectedValue);
            GenericUtils.dumpArrayToTraceLog(fourBytesRepr);
            // @Tested API asInt(byte[0,2,2,-40])...
            returnedValue = GenericUtils.asInt(fourBytesRepr);
            assertEquals(returnedValue, expectedValue);

            long expectedValueLong = 131800;
            byte[] longBytes = GenericUtils.asByteArray(expectedValueLong);
            long returnedValueLong = GenericUtils.asLongValue(longBytes);
            assertEquals(returnedValueLong, expectedValueLong);
            expectedValueLong = Integer.MAX_VALUE + 1L;
            longBytes = GenericUtils.asByteArray(expectedValueLong);
            returnedValueLong = GenericUtils.asLongValue(longBytes);
            assertEquals(returnedValueLong, expectedValueLong);
            expectedValueLong = Long.MAX_VALUE;
            longBytes = GenericUtils.asByteArray(expectedValueLong);
            returnedValueLong = GenericUtils.asLongValue(longBytes);
            assertEquals(returnedValueLong, expectedValueLong);

            // *****************************************************************
            // test the asByteArray(int) method
            // *****************************************************************

            // test positive number
            assertEquals("500", new String(GenericUtils.asByteArray(500)));

            // test negative number
            assertEquals("-5132", new String(GenericUtils.asByteArray(-5132)));

            // test zero
            assertEquals("0", new String(GenericUtils.asByteArray(0)));

            // test larger numbers
            assertEquals("2100925124",
                         new String(GenericUtils.asByteArray(2100925124)));

            // test large negative number
            assertEquals("-84003465",
                         new String(GenericUtils.asByteArray(-84003465)));

            // test maximum int
            assertEquals("2147483647",
                         new String(GenericUtils.asByteArray(2147483647)));
        } catch (Throwable t) {
            outputMgr.failWithThrowable("testMain", t);
        }
    }

    /**
     * The values of the password and client_secret parameters must be nullified.
     */
    @Test
    public void maskPasswords() {
        String dirtyString = "param1=value1&password=passwordValue&client_secret=passwordValue";
        String expectedString = "param1=value1&password=*************&client_secret=*************";
        String sanitizedString = GenericUtils.nullOutPasswords(dirtyString, (byte) '&');
        assertEquals("The password must be masked.", expectedString, sanitizedString);
    }
}
