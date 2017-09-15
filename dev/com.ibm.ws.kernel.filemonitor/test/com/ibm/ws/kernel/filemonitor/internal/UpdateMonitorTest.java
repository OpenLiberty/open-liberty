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
public class UpdateMonitorTest {

    static SharedOutputManager outputMgr;
    static File cacheLocation;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestUtils.TEST_DATA);
        outputMgr.captureStreams();

        try {
            cacheLocation = TestUtils.createTempDirectory("cache");
        } catch (Throwable t) {
            outputMgr.failWithThrowable("setupBeforeClass", t);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    List<File> created = new ArrayList<File>();
    List<File> modified = new ArrayList<File>();
    List<File> deleted = new ArrayList<File>();

    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation, 
        // this keeps things sane
        outputMgr.resetStreams();
        created.clear();
        modified.clear();
        deleted.clear();
    }

    @Test(expected = NullPointerException.class)
    public void testBadMonitoredFile() {
        UpdateMonitor.getMonitor(null, MonitorType.DIRECTORY, null);
    }

    @Test(expected = NullPointerException.class)
    public void testNullMonitoredType() {
        UpdateMonitor.getMonitor(new File("notexist"), null, null);
    }
}
