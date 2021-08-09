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
package com.ibm.ws.kernel.filemonitor.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;
import test.utils.TestUtils;

import com.ibm.ws.kernel.filemonitor.internal.UpdateMonitor.MonitorType;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;

/**
 *
 */
public class DirectoryUpdateMonitorTest {

    final int monitorCount = 10;

    static SharedOutputManager outputMgr;
    static File tmpLocation;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestUtils.TEST_DATA);
        outputMgr.captureStreams();

        try {
            tmpLocation = TestUtils.createTempDirectory("cache");
        } catch (Throwable t) {
            outputMgr.failWithThrowable("setupBeforeClass", t);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    List<List<File>> created = new ArrayList<List<File>>(8);
    List<List<File>> modified = new ArrayList<List<File>>(8);
    List<List<File>> deleted = new ArrayList<List<File>>(8);
    List<UpdateMonitor> monitors = new ArrayList<UpdateMonitor>(8);

    @Before
    public void setUp() throws Exception {
        for (int i = 0; i < monitorCount; i++) {
            created.add(new ArrayList<File>());
            modified.add(new ArrayList<File>());
            deleted.add(new ArrayList<File>());
        }
    }

    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation, 
        // this keeps things sane
        outputMgr.resetStreams();
        clear();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.filemonitor.internal.DirectoryUpdateMonitorTest#scanForUpdates(java.util.List, java.util.List, java.util.List)}.
     */
    @Test
    public void testScanForUpdates() {
        final String m = "testScanForUpdates";
        try {

            File targetDir = new File(tmpLocation, "monitoredDirectory");
            TestUtils.recursiveClean(targetDir); // clean from any previous run
            targetDir.deleteOnExit();
            assertTrue("1) Directory must be created: " + targetDir, targetDir.mkdir());

            /* 0 */monitors.add(UpdateMonitor.getMonitor(targetDir, MonitorType.DIRECTORY_RECURSE, null));
            /* 1 */monitors.add(UpdateMonitor.getMonitor(targetDir, MonitorType.DIRECTORY_RECURSE, FileMonitor.MONITOR_FILTER_FILES_ONLY));
            /* 2 */monitors.add(UpdateMonitor.getMonitor(targetDir, MonitorType.DIRECTORY_RECURSE, FileMonitor.MONITOR_FILTER_DIRECTORIES_ONLY));
            /* 3 */monitors.add(UpdateMonitor.getMonitor(targetDir, MonitorType.DIRECTORY_RECURSE, "grand.*"));

            /* 4 */monitors.add(UpdateMonitor.getMonitor(targetDir, MonitorType.DIRECTORY, null));
            /* 5 */monitors.add(UpdateMonitor.getMonitor(targetDir, MonitorType.DIRECTORY, FileMonitor.MONITOR_FILTER_FILES_ONLY));
            /* 6 */monitors.add(UpdateMonitor.getMonitor(targetDir, MonitorType.DIRECTORY, FileMonitor.MONITOR_FILTER_DIRECTORIES_ONLY));
            /* 7 */monitors.add(UpdateMonitor.getMonitor(targetDir, MonitorType.DIRECTORY, "grand.*"));

            /* 8 */monitors.add(UpdateMonitor.getMonitor(targetDir, MonitorType.DIRECTORY_SELF, null));
            /* 9 */monitors.add(UpdateMonitor.getMonitor(targetDir, MonitorType.DIRECTORY_RECURSE_SELF, null));

            assertEquals("TESTCASE ERROR, monitor list size did not match expected, have you just added a new monitor to the test case? you need to alter the var monitorCount ",
                         monitors.size(), monitorCount);

            assertDirectoryUpdateMonitor("2");
            assertEmptyInit("3", targetDir);

            // Unzip the test files, create file objects to work with certain files
            TestUtils.unzip(new File(TestUtils.TEST_DATA, "testScanForUpdates.zip"), targetDir);
            // Creates: 
            // + monitoredDirectory
            // | + child
            // | + childDir
            // | | + grandchild
            // | | + grandchildDir
            // | | | + greatgrand

            File child = new File(targetDir, "child");
            File childDir = new File(targetDir, "childDir");
            File grandchild = new File(targetDir, "childDir/grandchild");
            File grandchildDir = new File(targetDir, "childDir/grandchildDir");
            File greatgrand = new File(targetDir, "childDir/grandchildDir/greatgrand");

            assertTrue("4-1) child file should exist", new File(targetDir, "child").exists());
            assertTrue("4-2) grandchild dir should exist", grandchildDir.exists());
            assertTrue("4-3) greatgrand file should exist", greatgrand.exists());

            // Scan for updates: find new dir contents..
            scanForUpdates("5-1");
            assertEquals("5-1-0) created should contain all resources created by unzip", 5, created.get(0).size());
            assertEquals("5-1-1) created should contain all files created by unzip", 3, created.get(1).size());
            assertEquals("5-1-2) created should contain all directories created by unzip", 2, created.get(2).size());
            assertEquals("5-1-3) created should contain resources named grand.*", 2, created.get(3).size());
            assertEquals("5-1-4) created should contain child resources created by unzip", 2, created.get(4).size());
            assertEquals("5-1-5) created should contain child files created by unzip", 1, created.get(5).size());
            assertEquals("5-1-6) created should contain child directories created by unzip", 1, created.get(6).size());
            assertEquals("5-1-7) created should contain no resources (no match to filter, not recursive)", 0, created.get(7).size());
            assertEquals("5-1-8) created should contain child resources created by unzip", 2, created.get(8).size());
            assertEquals("5-1-9) created should contain all resources created by unzip", 5, created.get(9).size());
            assertModifiedEmpty("5-2");
            assertDeletedEmpty("5-3");
            clear();

            // Make greatgrand a lot older (~2min) than it is (should still be considered modified)
            greatgrand.setLastModified(System.currentTimeMillis() - 120000);
            scanForUpdates("5-2");

            assertCreatedEmpty("6-1");
            assertEquals("6-2-0) modified should contain greatgrand (no filter)", 1, modified.get(0).size());
            assertListContainsPath("6-2-0+) modified should contain greatgrand", greatgrand.getCanonicalPath(), modified.get(0));
            assertEquals("6-2-1) modified should contain greatgrand (file)", 1, modified.get(1).size());
            assertEquals("6-2-2) modified should be empty (dirs only)", 0, modified.get(2).size());
            assertEquals("6-2-3) modified should be empty (name filter grand.*)", 0, modified.get(3).size());
            assertEquals("6-2-4) modified should be empty (not recursive)", 0, modified.get(4).size());
            assertEquals("6-2-5) modified should be empty (not recursive)", 0, modified.get(5).size());
            assertEquals("6-2-6) modified should be empty (not recursive)", 0, modified.get(6).size());
            assertEquals("6-2-7) modified should be empty (not recursive)", 0, modified.get(7).size());
            assertEquals("6-2-8) modified should be empty (not recursive)", 0, modified.get(8).size());
            assertEquals("6-2-9) modified should contain greatgrand (file)", 1, modified.get(9).size());
            assertDeletedEmpty("6-3");
            clear();

            // Write something to child
            TestUtils.appendSomething(child);
            scanForUpdates("7-2");
            assertCreatedEmpty("7-1");
            assertEquals("7-2-0) modified should contain child (no filter)", 1, modified.get(0).size());
            assertListContainsPath("7-2-0+) modified should contain child", child.getCanonicalPath(), modified.get(0));
            assertEquals("7-2-1) modified should contain child (file)", 1, modified.get(1).size());
            assertEquals("7-2-2) modified should be empty (dirs only)", 0, modified.get(2).size());
            assertEquals("7-2-3) modified should be empty (name filter grand.*)", 0, modified.get(3).size());
            assertEquals("7-2-4) modified should contain child (not recursive)", 1, modified.get(4).size());
            assertEquals("7-2-5) modified should contain child (not recursive, file)", 1, modified.get(5).size());
            assertEquals("7-2-6) modified should be empty (not recursive, directory)", 0, modified.get(6).size());
            assertEquals("7-2-7) modified should be empty (not recursive, filter mismatch)", 0, modified.get(7).size());
            assertEquals("7-2-8) modified should contain child (file)", 1, modified.get(8).size());
            assertEquals("7-2-9) modified should contain child (file)", 1, modified.get(9).size());
            assertDeletedEmpty("7-3");
            clear();

            // delete grandchildDir
            TestUtils.recursiveClean(grandchildDir);
            scanForUpdates("8-3");
            assertCreatedEmpty("8-1");
            assertModifiedEmpty("8-2");
            assertEquals("8-3-0) deleted should contain grandchildDir & greatgrand (no filter)", 2, deleted.get(0).size());
            assertListContainsPath("8-3-0+) deleted should contain grandchildDir", grandchildDir.getCanonicalPath(), deleted.get(0));
            assertListContainsPath("8-3-0+) deleted should contain greatgrand", greatgrand.getCanonicalPath(), deleted.get(0));
            assertEquals("8-3-1) deleted should contain greatgrand (file)", 1, deleted.get(1).size());
            assertEquals("8-3-2) deleted should contain grandchildDir (dirs only)", 1, deleted.get(2).size());
            assertEquals("8-3-3) deleted should contain grandchildDir (name filter grand.*)", 1, deleted.get(3).size());
            assertEquals("8-3-4) deleted should be empty (not recursive)", 0, deleted.get(4).size());
            assertEquals("8-3-5) deleted should be empty (not recursive)", 0, deleted.get(5).size());
            assertEquals("8-3-6) deleted should be empty (not recursive)", 0, deleted.get(6).size());
            assertEquals("8-3-7) deleted should be empty (not recursive)", 0, deleted.get(7).size());
            assertEquals("8-3-8) deleted should be empty (not recursive)", 0, deleted.get(8).size());
            assertEquals("8-3-9)  deleted should contain grandchildDir & greatgrand (no filter)", 2, deleted.get(9).size());
            clear();

            TestUtils.recursiveClean(targetDir); // delete the monitored directory
            scanForUpdates("9-3");
            assertCreatedEmpty("9-1");
            assertModifiedEmpty("9-2");
            // All remaining seen/matching children should now be in the deleted list
            assertEquals("9-3-0) deleted should contain child, childDir, & grandchild (no filter)", 3, deleted.get(0).size());
            assertListContainsPath("9-3-0+) deleted should contain childDir", childDir.getCanonicalPath(), deleted.get(0));
            assertListContainsPath("9-3-0+) deleted should contain grandchild", grandchild.getCanonicalPath(), deleted.get(0));
            assertListContainsPath("9-3-0+) deleted should contain child", child.getCanonicalPath(), deleted.get(0));
            assertEquals("9-3-1) deleted should contain child and grandchild (file)", 2, deleted.get(1).size());
            assertEquals("9-3-2) deleted should contain childDir (dirs only)", 1, deleted.get(2).size());
            assertEquals("9-3-3) deleted should contain grandchild (name filter grand.*)", 1, deleted.get(3).size());
            assertEquals("9-3-4) deleted should contain child & childDir (not recursive)", 2, deleted.get(4).size());
            assertEquals("9-3-5) deleted should contain child (not recursive, file)", 1, deleted.get(5).size());
            assertEquals("9-3-6) deleted should contain childDir (not recursive, directory)", 1, deleted.get(6).size());
            assertEquals("9-3-7) deleted should be empty (not recursive, filter mismatch)", 0, deleted.get(7).size());

            assertListContainsPath("9-3-8-1) deleted should contain self (not recursive, self)", targetDir.getCanonicalPath(), deleted.get(8));
            assertListContainsPath("9-3-9-1) deleted should contain self (recursive, self)", targetDir.getCanonicalPath(), deleted.get(9));

            clear();

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    private void clear() {
        for (int i = 0; i < monitorCount; i++) {
            created.get(i).clear();
            modified.get(i).clear();
            deleted.get(i).clear();
        }
    }

    private void scanForUpdates(String test) {
        for (int i = 0; i < monitorCount; i++) {
            monitors.get(i).scanForUpdates(created.get(i), modified.get(i), deleted.get(i));

            System.out.println(test + "-" + i + ") " + monitors.get(i) + " scan complete: ");
            System.out.println("\t created = " + created.get(i).size() + ": " + created.get(i));
            System.out.println("\tmodified = " + modified.get(i).size() + ": " + modified.get(i));
            System.out.println("\t deleted = " + deleted.get(i).size() + ": " + deleted.get(i));
        }
    }

    private void assertCreatedEmpty(String test) {
        for (int i = 0; i < monitorCount; i++) {
            assertTrue(test + "-" + i + ") created list should be empty: " + created.get(i), created.get(i).isEmpty());
        }
    }

    private void assertModifiedEmpty(String test) {
        for (int i = 0; i < monitorCount; i++) {
            assertTrue(test + "-" + i + ") modified list should be empty: " + modified.get(i), modified.get(i).isEmpty());
        }
    }

    private void assertModifiedEmptyExceptSelf(String test) {
        for (int i = 0; i < monitorCount; i++) {
            switch (monitors.get(i).type) {
                case FILE:
                case DIRECTORY:
                case DIRECTORY_RECURSE:
                    assertTrue(test + "-" + i + ") modified list should be empty: " + modified.get(i), modified.get(i).isEmpty());
                    break;
                case DIRECTORY_RECURSE_SELF:
                case DIRECTORY_SELF:
                    assertEquals(test + "-" + i + ") modified list should contain self: " + modified.get(i), 1, modified.get(i).size());
                    break;
            }
        }
    }

    private void assertDeletedEmpty(String test) {
        for (int i = 0; i < monitorCount; i++) {
            assertTrue(test + "-" + i + ") deleted list should be empty: " + deleted.get(i), deleted.get(i).isEmpty());
        }
    }

    private void assertDirectoryUpdateMonitor(String test) {
        for (int i = 0; i < monitorCount; i++) {
            assertTrue(test + "-" + i + ") returned monitor should be a DirectoryUpdateMonitor: " + monitors.get(i), monitors.get(i) instanceof DirectoryUpdateMonitor);
        }
    }

    private void assertEmptyInit(String test, File targetDir) throws Exception {
        for (int i = 0; i < monitorCount; i++) {
            List<File> initFile = new ArrayList<File>();
            monitors.get(i).init(initFile);
            //init for self can contain self, and will not be empty, as the targetDir already exists.
            if (monitors.get(i).type != MonitorType.DIRECTORY_RECURSE_SELF &&
                monitors.get(i).type != MonitorType.DIRECTORY_SELF) {
                assertEquals(test + "-" + i + ") " + monitors.get(i).type + " init should return an empty list: " + initFile, 0, initFile.size());
            } else {
                assertEquals(test + "-" + i + ") " + monitors.get(i).type + " init should return a list with just targetDir: " + initFile, 1, initFile.size());
            }
        }
    }

    private void assertListContainsPath(String description, String canonicalPath, List<File> testList) throws IOException {
        for (File f : testList) {
            String path = f.getCanonicalPath();
            if (path.equals(canonicalPath))
                return;
        }
        throw new AssertionError(description);
    }
}
