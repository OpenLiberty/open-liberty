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
package com.ibm.ws.filetransfer.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
public class FileServiceUtilTest {
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
     * Test path containment utility
     */
    @Test
    public void testPathContainment() {
        final String m = "testPathContainment";
        try {
            List<String> paths = new ArrayList<String>();
            paths.add("c:/temp");
            paths.add("/myDir/nest1/nest2");
            paths.add("/other");

            assertTrue(FileServiceUtil.isPathContained(paths, "c:/temp/file.zip"));
            assertTrue(FileServiceUtil.isPathContained(paths, "c:/temp/nested1"));
            assertTrue(FileServiceUtil.isPathContained(paths, "c:/temp/nested2/file.zip"));
            assertTrue(FileServiceUtil.isPathContained(paths, "/myDir/nest1/nest2/my.ear"));
            assertTrue(FileServiceUtil.isPathContained(paths, "/myDir/nest1/nest2/nest3"));
            assertTrue(FileServiceUtil.isPathContained(paths, "/myDir/nest1/nest2/nest3/"));
            assertTrue(FileServiceUtil.isPathContained(paths, "/other/"));

            assertFalse(FileServiceUtil.isPathContained(paths, "/"));
            assertFalse(FileServiceUtil.isPathContained(paths, "c:/temp2"));
            assertFalse(FileServiceUtil.isPathContained(paths, "/myDir/nest1"));
            assertFalse(FileServiceUtil.isPathContained(paths, "/myDir/nest1/my.file"));
            assertFalse(FileServiceUtil.isPathContained(paths, "/myDir/nest1/nest3"));

            //Now test root directories
            paths.clear();
            paths.add("C:/");
            paths.add("/");

            assertTrue(FileServiceUtil.isPathContained(paths, "C:/"));
            assertTrue(FileServiceUtil.isPathContained(paths, "/"));
            assertTrue(FileServiceUtil.isPathContained(paths, "C:/java"));
            assertTrue(FileServiceUtil.isPathContained(paths, "/home"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test archive and expand utility functions
     */
    @Test
    public void testCreateExpandArchive() {
        final String m = "testCreateArchive";
        try {

            // define source/target info
            String testArchiveDir = "c:\\temp\\fileTransferArchiveTestDirectory";
            String testArchiveFile = "testFile.txt";
            String testArchiveSource = testArchiveDir + File.separator + testArchiveFile;
            String testString = "aBc";
            String testArchiveTarget = testArchiveDir + File.separator + "test.zip";

            // create a test file to be archived
            File d = new File(testArchiveDir);
            d.mkdirs();
            PrintWriter pw = new PrintWriter(testArchiveSource);
            pw.println(testString);
            pw.close();

            // archive the file
            FileServiceUtil ftau = new FileServiceUtil();
            assertTrue(ftau.createArchive(testArchiveSource, testArchiveTarget));
            assertTrue(new File(testArchiveTarget).exists());

            // delete our test file
            new File(testArchiveSource).delete();

            // expand the archive (to the same location as the original test file)
            assertTrue(ftau.expandArchive(testArchiveTarget, testArchiveDir));
            assertTrue(new File(testArchiveSource).exists());

            // verify contents of expanded file are same as those of original test file
            BufferedReader br = new BufferedReader(new FileReader(testArchiveSource));
            String s = br.readLine();
            br.close();
            assertTrue(s.equals(testString));

            // clean up
            new File(testArchiveSource).delete();
            new File(testArchiveTarget).delete();
            new File(testArchiveDir).delete();

        } catch (Throwable t) {

            outputMgr.failWithThrowable(m, t);
        }
    }
}
