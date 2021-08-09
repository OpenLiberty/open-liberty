/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.test.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.Test;

import com.ibm.ws.annocache.util.internal.UtilImpl_ReadBufferPartial;
import com.ibm.ws.annocache.util.internal.UtilImpl_WriteBuffer;

import junit.framework.Assert;

@SuppressWarnings("deprecation")
public class TestBuffers {
    public static File prepareStrings(String fileName) {
        File testFile = new File(fileName);

        if ( testFile.exists() ) {
            testFile.delete();
            if ( testFile.exists() ) {
                Assert.fail("Test file [ " + testFile.getAbsolutePath() + " ] still exists");
            }

        } else {
            File testParent = testFile.getParentFile();
            if ( testParent.exists() ) {
                if ( !testParent.isDirectory() ) {
                    Assert.fail("Test parent [ " + testParent.getAbsolutePath() + " ] is not a directory");
                }
            } else {
                testParent.mkdirs();
                if ( !testParent.exists() ) {
                    Assert.fail("Test parent [ " + testParent.getAbsolutePath() + " ] does not exist");
                }
            }
        }

        return testFile;
    }

    //

    public static class TestProfile {
        public final int testId;

        public final int payloads;
        public final int payloadSize;

        public final int writeBufferSize;
        public final int readBufferSize;

        public TestProfile(int... parms) {
            this.testId = parms[0];
            this.payloads = parms[1];
            this.payloadSize = parms[2];
            this.writeBufferSize = parms[3];
            this.readBufferSize = parms[4];
        }

        public static final String TEST_FILE_NAME = "build/buffers/test";

        public File prepareTestFile() {
            return TestBuffers.prepareStrings(TEST_FILE_NAME + testId);
        }

        public byte[] allocateBytes() {
            return new byte[payloadSize];
        }

        public static final int PAYLOAD_AS_INT = 
            ( ((0x01 & 0xFF) << 24) |
              ((0x02 & 0xFF) << 16) |
              ((0x03 & 0xFF) <<  8) |
              ((0x04 & 0xFF) <<  0) );

        public void setBytes(byte[] bytes) {
            for ( int byteNo = 0; byteNo < payloadSize; byteNo++ ) {
                bytes[byteNo] = (byte) byteNo;
            }
        }

        public void verifyBytes(int payloadNo, byte[] bytes) {
            for ( int byteNo = 0; byteNo < payloadSize; byteNo++ ) {
                int actualByte = bytes[byteNo];
                if ( actualByte != byteNo ) {
                    Assert.assertTrue(
                        "Byte [ " + byteNo + " ] of payload [ " + payloadNo + " ]:" +
                        "expected [ " +  byteNo + " ] actual [ " + actualByte + " ]",
                        (actualByte == byteNo) );
                }
            }
        }

        public void verifyPayload(
            UtilImpl_ReadBufferPartial readBuffer,
            int payloadNo, byte[] bytes) throws IOException {

            int actualPayloadNo = readBuffer.readSmallInt();
            if ( actualPayloadNo != payloadNo ) {
                Assert.assertTrue(
                    "Expected payload number [ " + payloadNo + " ] actual [ " + actualPayloadNo + " ]",
                    (actualPayloadNo == payloadNo));
            }

            readBuffer.read(bytes);

            verifyBytes(payloadNo, bytes);
        }
    }

    // Payloads must have at least 4 bytes for the long read and write tests.

    protected static final TestProfile[] profiles = {
        new TestProfile(0,   50,   5,   16,   16), // Activity smaller than buffers.
        new TestProfile(1,   50,  21,   16,   16), // Activity larger than buffers.

        new TestProfile(2,   50,   6,   16,   16), // Payloads which are an even fraction of the buffer
        new TestProfile(3,   50,  30,   16,   16), // Payloads which are an even multiple of the buffer.

        new TestProfile(4,   50,   5, 1024, 1024), // All activity in one buffer.
        new TestProfile(5, 1000, 101, 1024, 1024)  // Larger, more realistic data
    };

    public static enum ProfileSelector {
        SmallerPayloads(0),
        LargerPayloads(1),

        EvenFractionalPayloads(2),
        EvenMutliplePayloads(3),

        SpanningBuffers(4),
        LargeData(5);

        private ProfileSelector(int offset) {
            this.offset = offset;
        }

        public final int offset;

        public TestProfile getProfile() {
            return profiles[offset];
        }
    }

    public TestProfile getTestProfile(ProfileSelector selector) {
        return selector.getProfile();
    }

    @Test
    public void testBuffers_SmallerPayloads() throws Exception {
        testBuffers( ProfileSelector.SmallerPayloads.getProfile() );
    }

    @Test
    public void testBuffers_LargerPayloads() throws Exception {
        testBuffers( ProfileSelector.LargerPayloads.getProfile() );
    }

    @Test
    public void testBuffers_EvenFractionalPayloads() throws Exception {
        testBuffers( ProfileSelector.EvenFractionalPayloads.getProfile() );
    }

    @Test
    public void testBuffers_EvenMultiplePayloads() throws Exception {
        testBuffers( ProfileSelector.EvenMutliplePayloads.getProfile() );
    }

    @Test
    public void testBuffers_SpanningBuffers() throws Exception {
        testBuffers( ProfileSelector.SpanningBuffers.getProfile() );
    }

    @Test
    public void testBuffers_LargeData() throws Exception {
        testBuffers( ProfileSelector.LargeData.getProfile() );
    }

    public void testBuffers(TestProfile profile) throws Exception {
        File testFile = profile.prepareTestFile();

        populate(profile, testFile);
        verify(profile, testFile);
    }

    private void populate(TestProfile profile, File testFile) throws IOException {
        FileOutputStream testOutputStream = null;
        UtilImpl_WriteBuffer writeBuffer = null;

        try {
            testOutputStream = new FileOutputStream(testFile);
            writeBuffer =
                new UtilImpl_WriteBuffer(
                    testFile.getAbsolutePath(), testOutputStream,
                    profile.writeBufferSize);

            byte[] writeBytes = profile.allocateBytes();
            profile.setBytes(writeBytes);

            for ( int payloadNo = 0; payloadNo < profile.payloads; payloadNo++ ) {
                writeBuffer.writeSmallInt(payloadNo);
                writeBuffer.write(writeBytes);
            }

        } finally { 
            if ( writeBuffer != null ) {
                writeBuffer.close();
            } else if ( testOutputStream != null ) {
                testOutputStream.close();
            }
        }

        Assert.assertTrue( "Test file does not exist [ " + testFile.getAbsolutePath() + " ]", testFile.exists() );
    }

    public interface Failable<E extends Exception> {
        void run () throws E;
    }

    private <E extends Exception> void verifyFailure(String message, Failable<E> failable) {
        Exception expected;
        try {
            failable.run();
            expected = null;
        } catch ( Exception e ) {
            expected = e;
        }
        Assert.assertTrue( message, (expected != null) );
    }

    private void verify(final TestProfile profile, File testFile) throws IOException {
        RandomAccessFile randomTestFile = new RandomAccessFile(testFile, "r");

        final UtilImpl_ReadBufferPartial readBuffer =
            new UtilImpl_ReadBufferPartial(
                testFile.getAbsolutePath(),
                randomTestFile,
                profile.readBufferSize);

        try {
            byte[] readBytes = profile.allocateBytes();

            // First pass: Read and validate the test data.

            for ( int payloadNo = 0; payloadNo < profile.payloads; payloadNo++ ) {
                profile.verifyPayload(readBuffer, payloadNo, readBytes);
            }

            // Next read should be past the end.

            // verifyFailure( "Read 1 past EOF after read did not fail", () -> { readBuffer.read(); } );
            Failable<Exception> readAction = new Failable<Exception>() {
                @Override
                public void run() throws Exception {
                    readBuffer.read();
                }
            };
            verifyFailure("Read 1 past EOF after read did not fail", readAction);

            // Plus two bytes, which is the write size of the payload number.
            final int fullPayload = 2 + profile.payloadSize;

            // Skip 5 and 2/5 of test data packages, then skip the last 3/5.

            readBuffer.seek(5 * fullPayload + 2);
            for ( int remainder = 0; remainder < (fullPayload - 2); remainder++ ) {
                readBuffer.read();
            }

            // Verify the position by reading test packets.

            for ( int payloadNo = 6; payloadNo < 10; payloadNo++ ) {
                profile.verifyPayload(readBuffer, payloadNo, readBytes);
            }

            // Try a few different ways to cause read failures.

            readBuffer.seek(profile.payloads * fullPayload);
            // verifyFailure( "Read 1 past EOF after seek did not fail", () -> { readBuffer.read(); } );
            verifyFailure("Read 1 past EOF after seek did not fail", readAction);

            readBuffer.seek((profile.payloads - 1) * fullPayload);
            profile.verifyPayload(readBuffer, profile.payloads - 1, readBytes);
            // verifyFailure( "Read 1 past EOF after seek and read did not fail", () -> { readBuffer.read(); } );            
            verifyFailure("Read 1 past EOF after seek and read did not fail", readAction);

            readBuffer.seek(0);
            profile.verifyPayload(readBuffer, 0, readBytes);
            // verifyFailure( "Seek before beginning of file did not fail", () -> { readBuffer.seek(-1); } );
            Failable<Exception> backupAction = new Failable<Exception>() {
                @Override
                public void run() throws Exception {
                    readBuffer.seek(-1);
                }
            };
            verifyFailure( "Seek before beginning of file did not fail", backupAction);

            readBuffer.seek(0);
            profile.verifyPayload(readBuffer, 0, readBytes);
            // verifyFailure("Seek past EOF did not fail", () -> { readBuffer.seek(profile.payloads * fullPayload + 1); } ); 
            Failable<Exception> longSeekAction = new Failable<Exception>() {
                @Override
                public void run() throws Exception {
                    readBuffer.seek(profile.payloads * fullPayload + 1);
                }
            };
            verifyFailure("Seek past EOF did not fail", longSeekAction);

            readBuffer.seek((profile.payloads - 1) * fullPayload);
            // verifyFailure( "Read past EOF did not fail", () -> { readBuffer.read(null, 0, 10); } );
            Failable<Exception> read10Action = new Failable<Exception>() {
                @Override
                public void run() throws Exception {
                    readBuffer.read(null, 0, 10);
                }
            };
            verifyFailure( "Read past EOF did not fail", read10Action);

            // Verify random(ish) seeks and reads.

            int scramble = 104729; // 10,000'th prime
            for ( int payloadNo = 0; payloadNo < profile.payloads; payloadNo++ ) {
                int actualPayloadNo = (payloadNo * scramble) % profile.payloads;
                readBuffer.seek(actualPayloadNo * fullPayload);
                profile.verifyPayload(readBuffer, actualPayloadNo, readBytes);
            }

            // Verify large int operations.

            byte[] largeIntBytes = new byte[4];

            for ( int intValue = 0; intValue < 1024; intValue++ ) {
                int initialInt = scramble * intValue;
                UtilImpl_WriteBuffer.convertLargeInt(initialInt, largeIntBytes);
                int finalInt = UtilImpl_ReadBufferPartial.convertLargeInt(largeIntBytes);

                if ( initialInt != finalInt ) {
                    Assert.assertTrue(
                        "Initial large int [ " + initialInt + " ] recovered as [ " + finalInt + " ]",
                        (initialInt == finalInt));
                }
            }

            readBuffer.seek(0);
            readBuffer.readSmallInt(); // Skip the payload number.
            readBuffer.read(); // Skip the first payload byte, which is zero.
            int largeInt = readBuffer.readLargeInt(); // Read payload bytes { 0x01, 0x02, 0x03, 0x04 }.
            if ( largeInt != TestProfile.PAYLOAD_AS_INT ) {
                Assert.assertTrue(
                    "Payload int [ " + TestProfile.PAYLOAD_AS_INT + " ] recovered as [ " + largeInt + " ]",
                    (TestProfile.PAYLOAD_AS_INT == largeInt));
            }

        } finally {
            readBuffer.close();
        }
    }

    //

    // public String readString() throws IOException;
    // public String readString(int width) throws IOException;
    // public void requireByte(byte fieldByte) throws IOException;
    // public String requireString(byte fieldByte, int width) throws IOException;

    // public void write(String value) throws IOException;
    // public void write(byte fieldByte, String value) throws IOException;
    // public void write(byte fieldByte, String value, int width) throws IOException;

    public static final String STRINGS_FILE_NAME = "build/buffers/strings";

    public static final byte[] STRINGS_BYTES = {
        0x01, 0x02,
        0x03, 0x04, 0x05,
        0x06 };

    public static final String[] STRINGS_STRINGS = {
        "",
        "Z",

        "AAAAAAAAAA",
        "ABCDEFGHIJ",
        "0123456789",

        "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
    };

    public static final int[] STRINGS_WIDTHS = {
        10, 20,
        40, 40, 40,
        60
    };

    public static final int STRINGS_REPETITIONS = 100;

    public static final int STRINGS_WRITE_BUFFER_SIZE = 1024;
    public static final int STRINGS_READ_BUFFER_SIZE = 1024;

    private void populateStrings(File testFile) throws Exception {
        FileOutputStream testOutputStream = null;
        UtilImpl_WriteBuffer writeBuffer = null;

        try {
            testOutputStream = new FileOutputStream(testFile);
            writeBuffer = new UtilImpl_WriteBuffer(
                testFile.getAbsolutePath(), testOutputStream,
                STRINGS_WRITE_BUFFER_SIZE);

            for ( int repNo = 0; repNo < STRINGS_REPETITIONS; repNo++ ) {
                for ( int strNo = 0; strNo < STRINGS_STRINGS.length; strNo++ ) {
                    byte testByte = STRINGS_BYTES[strNo];
                    String testString = STRINGS_STRINGS[strNo];
                    int testWidth = STRINGS_WIDTHS[strNo];

                    writeBuffer.write(testString);
                    writeBuffer.write(testByte, testString);
                    writeBuffer.write(testByte, testString, testWidth);
                }
            }

        } finally { 
            if ( writeBuffer != null ) {
                writeBuffer.close();
            } else if ( testOutputStream != null ) {
                testOutputStream.close();
            }
        }

        Assert.assertTrue( "Test file does not exist [ " + testFile.getAbsolutePath() + " ]", testFile.exists() );
    }

    @Test
    public void testStringIO() throws Exception {
        File stringsTestFile = prepareStrings(STRINGS_FILE_NAME);
        populateStrings(stringsTestFile);
        verifyStrings(stringsTestFile);
    }

    private void verify(String expected, String actual) {
        if ( !expected.equals(actual) ) {
            Assert.assertTrue(
                "Wrote [ " + expected + " ] read [ " + actual + " ]",
                expected.equals(actual) );
        }
    }

    private void verifyStrings(File testFile) throws IOException {
        RandomAccessFile randomTestFile = new RandomAccessFile(testFile, "r");

        UtilImpl_ReadBufferPartial readBuffer =
            new UtilImpl_ReadBufferPartial(
                testFile.getAbsolutePath(), randomTestFile, STRINGS_READ_BUFFER_SIZE);

        try {
            for ( int repNo = 0; repNo < STRINGS_REPETITIONS; repNo++ ) {
                for ( int strNo = 0; strNo < STRINGS_STRINGS.length; strNo++ ) {
                    byte testByte = STRINGS_BYTES[strNo];
                    String testString = STRINGS_STRINGS[strNo];
                    int testWidth = STRINGS_WIDTHS[strNo];

                    String actualString_1 = readBuffer.readString();
                    verify(testString, actualString_1);

                    String actualString_2 = readBuffer.requireField(testByte);
                    verify(testString, actualString_2);

                    String actualString_3 = readBuffer.requireField(testByte, testWidth);
                    verify(testString, actualString_3);
                }
            }

        } finally {
            readBuffer.close();
        }
    }
}
