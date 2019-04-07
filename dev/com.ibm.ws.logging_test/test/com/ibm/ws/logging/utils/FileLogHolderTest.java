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
package com.ibm.ws.logging.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.logging.internal.impl.BaseTraceFormatter;
import com.ibm.ws.logging.internal.impl.FileLogHeader;
import com.ibm.ws.logging.internal.impl.LoggingConstants;
import com.ibm.ws.logging.internal.impl.LoggingConstants.TraceFormat;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;
import test.common.TestFile;

/**
 *
 */
public class FileLogHolderTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().logTo(TestConstants.BUILD_TMP);
    static File testLogDir;

    @Rule
    public TestRule outputRule = outputMgr;

    @BeforeClass
    public static void doReflection() throws Exception {
        testLogDir = TestFile.createTempDirectory("FileLogHolderTest");

        // Make sure we're starting clean
        TestFile.recursiveClean(testLogDir);
    }

    @AfterClass
    public static void doCleanup() {
        // Clean up on the way out, too
        TestFile.recursiveClean(testLogDir);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.logging.utils.FileLogHolder#createFileLogHolder(com.ibm.ws.logging.utils.FileLogHolder, java.lang.String, java.io.File, java.lang.String, int, long)}
     * .
     */
    @Test
    public void testFileLogHolder() throws Exception {
        // This is a fairly long sequence of events w/in one method, to ensure configuration
        // changes have the expected effect.

        final String bannerLine = BaseTraceFormatter.banner + LoggingConstants.nl;
        String headerLine = "header line" + LoggingConstants.nl;
        String header = bannerLine + headerLine + bannerLine;
        int headerLen = header.length();

        String aRecord = "a record";
        int aRecordLen = aRecord.length() + LoggingConstants.nl.length();

        String rolledRecord = "rolled record";
        int rolledRecordLen = rolledRecord.length() + LoggingConstants.nl.length();

        // Create a log file with 50byte limit that will keep at most 2 files.
        FileLogHolder a1 = FileLogHolder.createFileLogHolder(null, new FileLogHeader(headerLine, false, false, false),
                                                             testLogDir, "a.log", 2, headerLen + aRecordLen + (rolledRecordLen / 2));
        a1.writeRecord(aRecord);

        assertEquals("fileName should match expected", "a", a1.fileLogSet.getFileName());
        assertEquals("fileExtension should match expected", ".log", a1.fileLogSet.getFileExtension());
        assertNotNull("fileRegex should be set", a1.fileLogSet.getFilePattern());
        assertTrue("fileRegex should contain file name: " + a1.fileLogSet.getFilePattern().pattern(), a1.fileLogSet.getFilePattern().pattern().contains("a"));
        assertTrue("fileRegex should contain file extension: " + a1.fileLogSet.getFilePattern().pattern(), a1.fileLogSet.getFilePattern().pattern().contains("log"));

        // at this point, we should have written the header (header line), and
        // the first record (a record), including line endings: check our math
        long expected = headerLen + aRecordLen;
        assertEquals("Check our math, we should have written " + (header + aRecord) + " (" + expected + " bytes", expected, a1.currentCountingStream.count());
        assertEquals("Check our math, our counting should be accurate", a1.currentCountingStream.count(), getLength(a1));
        assertEquals("Only one file should exist", 1, getLogFiles(aLogFilter).length);

        // The limit we configured has space for aRecord but not rolledRecord,
        // so after we write this, we should have a new file with the header.
        a1.writeRecord(rolledRecord);

        expected = headerLen + rolledRecordLen;
        assertEquals("Check our math, we should have written " + expected + " bytes", expected, a1.currentCountingStream.count());
        assertEquals("Check our math, our counting should be accurate", a1.currentCountingStream.count(), getLength(a1));
        assertEquals("Two files should exist", 2, getLogFiles(aLogFilter).length);

        // Let's write another record, this should also roll.
        // because max files is 2, a file should get pruned.
        a1.writeRecord(rolledRecord);
        assertEquals("Check our math, we should have written " + expected + " bytes", expected, a1.currentCountingStream.count());
        assertEquals("Check our math, our counting should be accurate", a1.currentCountingStream.count(), getLength(a1));
        assertEquals("Two files should exist", 2, getLogFiles(aLogFilter).length);

        // Now lets change the maxNumFiles and maxFileSizeBytes and repeat.
        // a1 and a2 should be the same instance (updates applied)
        FileLogHolder a2 = FileLogHolder.createFileLogHolder(a1, new FileLogHeader(headerLine, false, false, false),
                                                             testLogDir, "a.log", 1, headerLen + 20);
        assertSame("a1 and a2 should be the same instance", a1, a2);
        assertEquals("maxNumFiles should have changed to new value", 1, a2.fileLogSet.getMaxFiles());
        assertEquals("maxFileSizeBytes should have changed to new value", headerLen + 20, a2.maxFileSizeBytes);
        assertEquals("One file should exist", 1, getLogFiles(aLogFilter).length);

        // Now we'll write another log, which should trigger both a roll, AND a prune
        a1.writeRecord(rolledRecord);
        assertEquals("Check our math, we should have written " + expected + " bytes, despite 20byte limit", expected, a1.currentCountingStream.count());
        assertEquals("Check our math, our counting should be accurate", a1.currentCountingStream.count(), getLength(a1));
        assertEquals("Only one file should exist", 1, getLogFiles(aLogFilter).length);

        // Now, let's change the log name to b*. Nothing should happen to the a* log file.
        // a1 and b1 should still be the same log holder instance
        // It should have a new regex pattern for the new filename
        FileLogHolder b1 = FileLogHolder.createFileLogHolder(a1, null, testLogDir, "b.log", 1, headerLen + 20);
        assertSame("a1 and b1 should be the same instance", a1, b1);
        assertEquals("fileName should match expected", "b", b1.fileLogSet.getFileName());
        assertEquals("fileExtension should match expected", ".log", b1.fileLogSet.getFileExtension());
        assertNotNull("fileRegex should be set", b1.fileLogSet.getFilePattern());
        assertTrue("fileRegex should contain file name: " + b1.fileLogSet.getFilePattern().pattern(), b1.fileLogSet.getFilePattern().pattern().contains("b"));
        assertTrue("fileRegex should contain file extension: " + b1.fileLogSet.getFilePattern().pattern(), b1.fileLogSet.getFilePattern().pattern().contains("log"));

        // Now we'll write another log, which should trigger both a roll, AND a prune of the b* log file
        b1.writeRecord(rolledRecord);
        assertEquals("Check our math, we should have written " + expected + " bytes, despite 20byte limit", expected, b1.currentCountingStream.count());
        assertEquals("Check our math, our counting should be accurate", b1.currentCountingStream.count(), getLength(b1));
        assertEquals("Only one a* file should exist", 1, getLogFiles(aLogFilter).length);
        assertEquals("Only one b* file should exist", 1, getLogFiles(bLogFilter).length);

        // Turn off log-rolling
        FileLogHolder b2 = FileLogHolder.createFileLogHolder(b1, null, testLogDir, "b.log", 1, 0);
        assertSame("b1 and b2 should be the same instance", b1, b2);
        assertEquals("maxFileSizeBytes should have changed to new value", 0, b2.maxFileSizeBytes);
        assertEquals("maxFiles should be preserved", 1, b2.fileLogSet.getMaxFiles());
        assertEquals("Only one a* file should exist", 1, getLogFiles(aLogFilter).length);
        assertEquals("Only one b* file should exist", 1, getLogFiles(bLogFilter).length);

        b2.writeRecord("unlimited1");
        b2.writeRecord("unlimited2");

        // write an additional 20 + line endings to what we had before..
        expected = expected + (10 + LoggingConstants.nl.length()) * 2;
        assertEquals("We should have have written 48 bytes", expected, b2.currentCountingStream.count());
        assertEquals("Check our math, our counting should be accurate", b2.currentCountingStream.count(), getLength(b2));
        assertEquals("Only one b* file should exist", 1, getLogFiles(bLogFilter).length);

        // Close the stream
        b2.close();

        // Get the contents of a.log
        String fileContents = LoggingTestUtils.readFile(new File(testLogDir, "a.log"));

        BaseTraceFormatter f = new BaseTraceFormatter(TraceFormat.ENHANCED);
        System.out.println("==========");
        System.out.println(fileContents);
        System.out.println("==========");
        System.out.println(f.formatObj(fileContents.getBytes()));
        System.out.println("==========");

        // Ensure we got the expected content
        assertTrue("a.log should start with header line; " + printExpected(f, header), fileContents.startsWith(header));
        assertTrue("a.log should end with rolled record; " + printExpected(f, rolledRecord + LoggingConstants.nl),
                   fileContents.endsWith(rolledRecord + LoggingConstants.nl));

        // Get the contents of b.log
        fileContents = LoggingTestUtils.readFile(new File(testLogDir, "b.log"));
        System.out.println("==========");
        System.out.println(fileContents);
        System.out.println("==========");
        System.out.println(f.formatObj(fileContents.getBytes()));
        System.out.println("==========");
        assertTrue("b.log should start with header line; " + printExpected(f, header), fileContents.startsWith(header));
        assertTrue("a.log should contain rolled record; " + printExpected(f, rolledRecord), fileContents.contains(rolledRecord));
        assertTrue("b.log should contain unlimited1; " + printExpected(f, "unlimited1"), fileContents.contains("unlimited1"));
        assertTrue("b.log should end with unlimited2; " + printExpected(f, "unlimited2"), fileContents.endsWith("unlimited2" + LoggingConstants.nl));

        // Now let's create a new c.log. the previous a and b logs should stay present.
        // Purpose is to test whether or not lock on file can be released.
        FileLogHolder c1 = FileLogHolder.createFileLogHolder(null, null, testLogDir, "c.log", 1, 0);

        // Verify that c.log does not exist, write text to log, and check that file exists
        File cLog = new File(testLogDir, "c.log");
        assertTrue("c.log should not exist", !cLog.exists());
        c1.writeRecord("Pre-release");
        assertTrue("c.log should exist", cLog.exists());

        // Ensure contents are as expected
        fileContents = LoggingTestUtils.readFile(cLog);
        assertTrue("c.log should contain Pre-release; " + printExpected(f, "Pre-release"), fileContents.contains("Pre-release"));

        // Release the c.log and delete log, check that it does not exist.
        c1.releaseFile();
        assertTrue("c.log should have been deleted", cLog.delete());
        assertTrue("c.log should not exist", !cLog.exists());

        // Write to log, ensure it exists with correct contents
        c1.writeRecord("Post-release");
        assertTrue("c.log should exist", cLog.exists());
        fileContents = LoggingTestUtils.readFile(cLog);
        assertTrue("c.log should contain Post-release; " + printExpected(f, "Post-release"), fileContents.contains("Post-release"));

        // Close the stream
        c1.close();
    }

    private String printExpected(BaseTraceFormatter f, String expected) {
        return "expected: " + f.formatObj(expected.getBytes());

    }

    private long getLength(FileLogHolder f) throws IllegalArgumentException, IOException, IllegalAccessException {
        return f.currentFileStream.getChannel().size();
    }

    /**
     * Gets all of the logs from the logs dir. Returns an empty array if there are no files in there.
     *
     * @return An array of the log files
     */
    private File[] getLogFiles(FilenameFilter filter) {
        File[] logFiles = testLogDir.listFiles(filter);

        if (logFiles == null)
            return new File[0];
        return logFiles;
    }

    static FilenameFilter aLogFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith("a") && name.endsWith(".log");
        }
    };

    static FilenameFilter bLogFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith("b") && name.endsWith(".log");
        }
    };

    /**
     * Issue 4364: Check that we refill the existing file.
     *
     * @throws Exception
     */
    @Test
    public void testFillingExistingFile() throws Exception {
        // Test both rolling behaviors
        testFilling("d.log", true);
        testFilling("e.log", false);
    }

    private void testFilling(String logName, boolean newLogsOnStart) {
        final String bannerLine = BaseTraceFormatter.banner + LoggingConstants.nl;
        String headerLine = "header line" + LoggingConstants.nl;
        String header = bannerLine + headerLine + bannerLine;

        String record = "record";

        writeFileOnce(headerLine, record, logName, newLogsOnStart);
        writeFileOnce(headerLine, record, logName, newLogsOnStart);

        File f = new File(testLogDir, logName);

        int expected = header.length() + record.length() + LoggingConstants.nlen;

        if (!newLogsOnStart) {
            expected <<= 1;
        }

        assertTrue("Incorrect file length for " + logName + ". Length: " + f.length() + ", Expected: " + expected + ", NewLogsOnStart: " + newLogsOnStart, f.length() == expected);
    }

    private void writeFileOnce(String headerLine, String record, String logName, boolean newLogsOnStart) {
        FileLogHolder d1 = FileLogHolder.createFileLogHolder(null, new FileLogHeader(headerLine, false, false, false),
                                                             testLogDir, logName, 2, 0, newLogsOnStart);

        d1.writeRecord(record);

        d1.close();
    }
}
