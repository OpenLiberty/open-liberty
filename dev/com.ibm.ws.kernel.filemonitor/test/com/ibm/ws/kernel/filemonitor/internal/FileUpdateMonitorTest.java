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
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;
import test.utils.TestUtils;

import com.ibm.ws.kernel.filemonitor.internal.UpdateMonitor.MonitorType;

/**
 *
 */
public class FileUpdateMonitorTest {

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

    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation, 
        // this keeps things sane
        outputMgr.resetStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.filemonitor.internal.FileUpdateMonitor#scanForUpdates(java.util.List, java.util.List, java.util.List)}.
     */
    @Test
    public void testScanForUpdates() {
        final String m = "testScanForUpdates";
        try {
            File f = new File(tmpLocation, "monitoredFile");
            f.deleteOnExit();
            assertTrue("1) File must be created: " + f, f.createNewFile());

            UpdateMonitor monitor = UpdateMonitor.getMonitor(f, MonitorType.FILE, null);
            assertTrue("2) returned monitor should be a FileUpdateMonitor: " + monitor, monitor instanceof FileUpdateMonitor);

            List<File> created = new ArrayList<File>();
            List<File> modified = new ArrayList<File>();
            List<File> deleted = new ArrayList<File>();

            monitor.init(created);
            assertEquals("3-1) monitor should return monitored file in a list as result of init", 1, created.size());
            assertEquals("3-2) monitor should return monitored file in a list as result of init", f, created.get(0));
            created.clear();

            // no change
            monitor.scanForUpdates(created, modified, deleted);
            assertTrue("4-1) list of created resources should be empty " + created, created.isEmpty());
            assertTrue("4-2) list of modified resources should be empty " + modified, modified.isEmpty());
            assertTrue("4-3) list of deleted resources should be empty " + deleted, deleted.isEmpty());

            TestUtils.appendSomething(f);

            monitor.scanForUpdates(created, modified, deleted);
            assertTrue("5-1) list of created resources should be empty " + created, created.isEmpty());
            assertEquals("5-2) list of modified resources should contain f " + modified, 1, modified.size());
            assertEquals("5-2+) list of modified resources should contain f", f, modified.get(0));
            assertTrue("5-3) list of deleted resources should be empty " + deleted, deleted.isEmpty());
            modified.clear();

            // Make it a lot older (~2min) than it is (should still be considered modified)
            f.setLastModified(System.currentTimeMillis() - 120000);

            monitor.scanForUpdates(created, modified, deleted);
            assertTrue("6-1) list of created resources should be empty " + created, created.isEmpty());
            assertEquals("6-2) list of modified resources should contain f " + modified, 1, modified.size());
            assertEquals("6-2+) list of modified resources should contain f", f, modified.get(0));
            assertTrue("6-3) list of deleted resources should be empty " + deleted, deleted.isEmpty());
            modified.clear();

            // Delete the file: it should be added to the 'deleted' list, and a
            // ResourceUpdateMonitor should be returned instead.
            assertTrue("7) File should be deleted", f.delete());

            monitor.scanForUpdates(created, modified, deleted);
            assertTrue("8-1) list of created resources should be empty " + created, created.isEmpty());
            assertTrue("8-2) list of modified resources should be empty " + modified, modified.isEmpty());
            assertEquals("8-3) list of deleted resources should contain f " + deleted, 1, deleted.size());
            assertEquals("8-3+) list of deleted resources should contain f", f, deleted.get(0));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
