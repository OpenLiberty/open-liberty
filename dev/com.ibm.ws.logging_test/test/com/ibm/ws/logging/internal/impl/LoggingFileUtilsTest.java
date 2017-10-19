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
package com.ibm.ws.logging.internal.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;

/**
 *
 */
public class LoggingFileUtilsTest {
    static SharedOutputManager outputMgr;

    final static File dir = new File(TestConstants.BUILD_TMP, "dir");

    final static String fileName = "file";
    final static String extensionName = ".log";

    final static String fileName2 = "file2";

    final static String fileName3 = "file3";

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();

        LoggingTestUtils.deepClean(dir);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();

        // clean up files created by test
        LoggingTestUtils.deepClean(dir);
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {}

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        LoggingTestUtils.deepClean(dir);
        outputMgr.resetStreams();
    }

    private static FileLogSet newFileLogSet(boolean rolling, File dir, String name, String ext, int max) {
        FileLogSet fls = new FileLogSet(rolling);
        fls.update(dir, name, ext, max);
        return fls;
    }

    /**
     * Test method for {@link FileLogHolder#createNewFile(String, String, String, boolean)} .
     */
    @Test
    public void testCreateNewFile() {
        final String m = "testCreateNewFile";
        try {
            File f = LoggingFileUtils.createNewFile(newFileLogSet(false, dir, fileName, extensionName, 0));
            assertNotNull("File object was not created (valid dir and file name)", f);
            assertTrue("File should exist on disk (valid dir and file name)", f.isFile());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link FileLogHolder#createNewFile(String, String, String, boolean)} .
     */
    @Test
    public void testCreateNewOutputStreamBadCharacterFile() {
        final String m = "testCreateNewOutputStreamBadCharacterFile";

        try {
            byte[] b = new byte[] { 0x00 };
            File f = LoggingFileUtils.createNewFile(newFileLogSet(false, dir, new String(b), extensionName, 0));
            assertNull("File was created with an invalid character in the file name", f);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * This tests that if when {@link FileLogHolder#createNewFile(String, String, String, boolean)} is called for a file that already exists a different file is created with a
     * different
     * name.
     */
    @Test
    public void testNewFileCreatedIfAlreadyExists() {
        final String method = "testNewFileCreatedIfAlreadyExists";
        try {
            // Create two files passing in the same name and make sure different files are created as we pass in true to say create a new file
            File f1 = LoggingFileUtils.createNewFile(newFileLogSet(false, dir, fileName2, extensionName, 0));
            File f2 = LoggingFileUtils.createNewFile(newFileLogSet(false, dir, fileName2, extensionName, 0));
            assertFalse("Two files with the same name were created, a unique file should of been created", f1.equals(f2));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(method, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ras.internal.LogStreamUtils#findNextFiles(int, java.lang.String, java.lang.String)} .
     */
    @Test
    public void testFindFilesEmptyDir() {
        final String m = "testFindFilesEmptyDir";
        try {
            assertTrue("Unable to create dir", dir.mkdir());
            Pattern p = LoggingFileUtils.compileLogFileRegex(fileName, "");
            String[] result = LoggingFileUtils.safelyFindFiles(dir, p);

            assertNotNull("safelyFindFiles should return null when no other files are present", result);
            assertEquals("safelyFindFiles should return empty array when no other files are present", 0, result.length);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.ras.internal.LogStreamUtils#findNextFiles(int, java.lang.String, java.lang.String)} .
     */
    @Test
    public void testFiles() throws Exception {
        final String m = "testFiles";
        final String fileName = "file";
        final String fileExt = ".log";
        final File file = new File(dir, fileName + fileExt);
        final File notANumber = new File(dir, fileName + fileExt + ".notANumber");

        try {
            final String[] dateStrings = { null,
                                          "_00.00.00_00.00.01",
                                          "_00.00.00_00.00.02",
                                          "_00.00.00_00.00.03",
                                          "_00.00.00_00.00.04",
            };
            final int[] dateStringNum = { 1 };
            FileLogSet fileLogSet = new FileLogSet(false) {
                @Override
                protected String getDateString() {
                    return dateStrings[dateStringNum[0]++];
                }
            };
            fileLogSet.update(dir, fileName, fileExt, 0);

            final Pattern regex = LoggingFileUtils.compileLogFileRegex(fileName, fileExt);

            assertTrue("Unable to create dir " + dir, dir.mkdir());
            assertTrue("Unable to create file " + file, file.createNewFile());
            assertTrue("Unable to create file " + notANumber, notANumber.createNewFile());

            String[] result = LoggingFileUtils.safelyFindFiles(dir, regex);
            assertEquals("safelyFindFiles should return empty array when no files matching regex are found", 0, result.length);

            File file1 = fileLogSet.createNewFile();
            assertNotNull("Unable to create file (1)", file1);
            System.out.println("Created file (1) " + file1);
            assertTrue(file1 + " should match " + regex, regex.matcher(file1.getName()).matches());

            result = LoggingFileUtils.safelyFindFiles(dir, regex);
            assertEquals("safelyFindFiles should find one matching file", 1, result.length);

            File file2 = fileLogSet.createNewFile();
            assertNotNull("Unable to create file (2)", file2);
            System.out.println("Created file (2) " + file2);
            File file3 = fileLogSet.createNewFile();
            assertNotNull("Unable to create file (3)", file3);
            System.out.println("Created file (3) " + file3);

            result = LoggingFileUtils.safelyFindFiles(dir, regex);
            assertEquals("safelyFindFiles should find three matching files", 3, result.length);

            // Clean up should be a no-op.
            fileLogSet.update(dir, fileName, fileExt, 3);
            fileLogSet.update(dir, fileName, fileExt, 4);

            assertTrue(file1 + " (1) should still exist", file1.exists());
            assertTrue(file1 + " (2) should still exist", file2.exists());
            assertTrue(file1 + " (3) should still exist", file3.exists());

            // Now delete extra numbered files
            fileLogSet.update(dir, fileName, fileExt, 2);

            // First numbered files should have been deleted
            assertFalse(file1 + " (1) should have been deleted", file1.exists());

            // most recent files should still exist
            assertTrue(file2 + " (2) should still exist", file2.exists());
            assertTrue(file3 + " (3) should still exist", file3.exists());

            File file4 = fileLogSet.createNewFile();
            assertNotNull("Unable to create file (4)", file4);
            System.out.println("Created file (4) " + file4);

            // Delete and recreate a file that sorts as later/newer.
            assertTrue("Unable to delete file (3) " + file3, file3.delete());
            System.out.println("Deleted file (3) " + file3);
            fileLogSet.update(dir, "xxx", fileExt, 0);
            fileLogSet.update(dir, fileName, fileExt, 2);

            dateStringNum[0] = 2;
            assertNotNull("Unable to create file (2) " + file2, fileLogSet.createNewFile());
            System.out.println("Recreated file (2) " + file2);
            dateStringNum[0] = 3;
            assertNotNull("Unable to create file (3) " + file3, fileLogSet.createNewFile());
            System.out.println("Recreated file (3) " + file3);

            // Later, but "not current" files should have been deleted
            assertFalse(file4 + " (4) should have been deleted", file4.exists());

            // most recent files should still exist
            assertTrue(file2 + " (2) should still exist", file2.exists());
            assertTrue(file3 + " (3) should still exist", file3.exists());

            // Call delete again with max of 1
            fileLogSet.update(dir, fileName, fileExt, 1);

            // Later, but "not current" files should have been deleted
            assertFalse(file2 + " (2) should have been deleted", file2.exists());
            assertTrue(file3 + " (3) should still exist", file3.exists());

            dateStringNum[0] = 4;
            assertNotNull("Unable to create file (4) " + file4, fileLogSet.createNewFile());

            // Oldest files should be deleted first.
            assertTrue(file + " (base) should still exist", file.exists());
            assertFalse(file3 + " (3) should have been deleted", file3.exists());
            assertTrue(file4 + " (4) should still exist", file4.exists());

            // Files w/o prefix or w/ not-numbered prefix should still exist
            assertTrue(notANumber + " should still exist", notANumber.exists());
            assertTrue(file + " should still exist", file.exists());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
