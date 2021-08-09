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
package com.ibm.ws.kernel.service.location.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.kernel.service.location.internal.FileLocator;

import test.common.SharedOutputManager;
import test.utils.Utils;

public class FileLocatorTest {
    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(Utils.TEST_DATA);
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation, this keeps
        // things sane
        outputMgr.resetStreams();
    }

    @Test
    public void testNullParameters() {
        assertNull("findFileInNamedPath should return null with null file", FileLocator.findFileInNamedPath(null, null));
        assertNull("findFileInNamedPath should return null with null path", FileLocator.findFileInNamedPath("file", null));
        assertNull("findFileInNamedPath should return null with empty path", FileLocator.findFileInNamedPath("file", new ArrayList<String>()));

        assertNull("findFileInFilePath should return null with null parameter", FileLocator.findFileInFilePath(null, null));
        assertNull("findFileInFilePath should return null with null search path", FileLocator.findFileInFilePath("file", null));
        assertNull("findFileInFilePath should return null with empty search path", FileLocator.findFileInFilePath("file", new ArrayList<File>()));

        assertNull("matchFileInNamedPath should return null with null parameter", FileLocator.matchFileInNamedPath(null, null));
        assertNull("matchFileInNamedPath should return null with null search path", FileLocator.matchFileInNamedPath("file", null));
        assertNull("matchFileInNamedPath should return null with empty search path", FileLocator.matchFileInNamedPath("file", new ArrayList<String>()));

        assertNull("matchFileInFilePath should return null with null parameter", FileLocator.matchFileInFilePath(null, null));
        assertNull("matchFileInFilePath should return null with null search path", FileLocator.matchFileInFilePath("file", null));
        assertNull("matchFileInFilePath should return null with empty search path", FileLocator.matchFileInFilePath("file", new ArrayList<File>()));

        assertTrue("getMatchingFileNames should return empty list  with null parameter", FileLocator.getMatchingFileNames(null, null, false).size() == 0);
        assertTrue("getMatchingFileNames should return empty list  with empty search path", FileLocator.getMatchingFileNames("dir", null, false).size() == 0);

        assertTrue("getMatchingFiles(File, String) should return empty list with null file", FileLocator.getMatchingFiles((File) null, null).size() == 0);
        assertTrue("getMatchingFiles(File, String) should return empty list with null expression", FileLocator.getMatchingFiles(new File("tmp"), null).size() == 0);

        assertTrue("getMatchingFiles(String, String) should return empty list with null directory", FileLocator.getMatchingFiles((String) null, null).size() == 0);
        assertTrue("getMatchingFiles(String, String) should return empty list with null expression", FileLocator.getMatchingFiles("dir", null).size() == 0);
    }

    @Test
    public void testGetFilteredFiles() {
        final String m = "testGetFilteredFiles";

        try {
            File tmpFile;
            String file1, file2, file3, dir4;
            String fullPath;
            String prefix = "FileTestTmp";

            tmpFile = Utils.createTempFile(prefix, ".match");
            tmpFile.deleteOnExit();
            file1 = tmpFile.getName();
            fullPath = tmpFile.getAbsolutePath();

            final String pathName = tmpFile.getParent();

            tmpFile = Utils.createTempFile("othername", ".nomatch");
            tmpFile.deleteOnExit();
            file2 = tmpFile.getName();

            tmpFile = new File(pathName + File.separator + prefix);
            tmpFile.deleteOnExit();
            file3 = tmpFile.getName();

            File tmpDir = new File(pathName + File.separator + prefix + ".dir");
            tmpDir.mkdir();
            tmpDir.deleteOnExit();
            dir4 = tmpDir.getName();

            // This will only match files with names longer than the prefix
            // get only the partial name, not the full path
            List<String> files = FileLocator.getMatchingFileNames(pathName, prefix + ".*", false);

            assertTrue("prefix.match should be in the list", files.contains(file1));
            assertFalse("othername.nomatch should not be in the list", files.contains(file2));
            assertFalse("prefix should not be in the list", files.contains(file3)); //
            assertFalse("dir name should not be in list", files.contains(dir4));

            // This will only match files with names longer than the prefix
            // get the full path
            files = FileLocator.getMatchingFileNames(pathName, prefix + ".*", true);
            assertTrue(files.contains(fullPath)); // full path for prefix.match should
                                                  // be in the list

            // This will only match files with names longer than the prefix
            // get only the partial name, not the full path
            files = FileLocator.getMatchingFileNames(pathName, null, false);
            assertTrue("prefix.match should be in the list", files.contains(file1));
            assertTrue("othername.nomatch should be in the list", files.contains(file2));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testFindFile() {
        final String m = "testFindFile";

        File tmpFile;

        try {
            File file1, file12, file2, file22, dir1, dir12, dir2, dir22;
            String prefix = "FindFile";
            File result;

            // Create files/directories for testing
            tmpFile = Utils.createTempFile(prefix, ".tmp");
            tmpFile.deleteOnExit();

            final String pathName = tmpFile.getParent();

            dir1 = new File(pathName + File.separator + prefix + ".dir");
            dir1.mkdir();
            dir1.deleteOnExit();

            file1 = new File(dir1, prefix);
            file1.createNewFile();
            file1.deleteOnExit();

            dir12 = new File(dir1, "subdir");
            dir12.mkdir();
            dir12.deleteOnExit();

            file12 = new File(dir12, prefix);
            file12.createNewFile();
            file12.deleteOnExit();

            dir2 = new File(pathName + File.separator + prefix + ".dir with spaces");
            dir2.mkdir();
            dir2.deleteOnExit();

            file2 = new File(dir2, prefix);
            file2.createNewFile();
            file2.deleteOnExit();

            dir22 = new File(dir2, "subdir");
            dir22.mkdir();
            dir22.deleteOnExit();

            file22 = new File(dir22, prefix);
            file22.createNewFile();
            file22.deleteOnExit();

            result = FileLocator.findFileInNamedPath(prefix, Arrays.asList(new String[] { null, dir1.getAbsolutePath() }));
            assertNotNull("File in dir1 should exist", result);
            assertEquals("File found should match file1", file1.getAbsolutePath(), result.getAbsolutePath());

            result = FileLocator.findFileInNamedPath("nomatch", Arrays.asList(new String[] { null, dir1.getAbsolutePath() }));
            assertNull("File in dir1 should exist", result);

            result = FileLocator.findFileInNamedPath(prefix, Arrays.asList(new String[] { "", dir2.getAbsolutePath() }));
            assertNotNull("File in dir2 should exist", result);
            assertEquals("File found should match file2", file2.getAbsolutePath(), result.getAbsolutePath());

            result = FileLocator.findFileInNamedPath("subdir/" + prefix, Arrays.asList(new String[] { dir1.getAbsolutePath() }));
            assertNotNull("File in dir1/subdir should exist", result);
            assertEquals("File found should match file12", file12.getAbsolutePath(), result.getAbsolutePath());

            result = FileLocator.findFileInNamedPath("subdir/" + prefix, Arrays.asList(new String[] { dir2.getAbsolutePath() }));
            assertNotNull("File in dir2/subdir should exist", result);
            assertEquals("File found should match file22", file22.getAbsolutePath(), result.getAbsolutePath());

            result = FileLocator.findFileInFilePath(prefix, Arrays.asList(new File[] { null, dir1 }));
            assertNotNull("File in dir1 should exist", result);
            assertEquals("File found should match file1", file1.getAbsolutePath(), result.getAbsolutePath());

            result = FileLocator.findFileInFilePath("notexist", Arrays.asList(new File[] { null, dir1 }));
            assertNull("File in dir1 should exist", result);

            result = FileLocator.matchFileInNamedPath("Find.*", Arrays.asList(new String[] { null, "", dir1.getAbsolutePath() }));
            assertNotNull("File should exist", result);
            assertEquals("File found should match file1", file1.getAbsolutePath(), result.getAbsolutePath());

            result = FileLocator.matchFileInNamedPath("nomatch", Arrays.asList(new String[] { null, "", dir1.getAbsolutePath() }));
            assertNull("File should not exist", result);

            result = FileLocator.matchFileInFilePath("Find.*", Arrays.asList(new File[] { null, dir1 }));
            assertNotNull("File should exist", result);
            assertEquals("File found should match file1", file1.getAbsolutePath(), result.getAbsolutePath());

            result = FileLocator.matchFileInFilePath("nomatch", Arrays.asList(new File[] { null, dir1 }));
            assertNull("File should not exist", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
