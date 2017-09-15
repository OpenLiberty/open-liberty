/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;
import test.shared.TestUtils;

import com.ibm.ws.kernel.boot.SharedBootstrapConfig;

/**
 *
 */
public class ServerRunningTest {
    static SharedOutputManager outputMgr;
    static SharedBootstrapConfig config;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        File f = TestUtils.createTempFile("ResourceUtilsTest", "tmp");
        f.delete();
        boolean suc = f.mkdir();

        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();

        // Bootstrap configuration/locations
        config = SharedBootstrapConfig.createSharedConfig(outputMgr);

        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
        TestUtils.cleanTempFiles();
    }

    /**
     * Test to make sure .sRunning file is created and exists as server is running and that it isn't
     * deleted as part of a cleaning of workarea.
     */
    @Test
    public void testServerRunning() {

        final String m = "testServerRunning";
        File sRunningFile = null;

        try {
            // Must create parent workarea - server lock code ensures this exists
            File serverWorkArea = config.getWorkareaFile(null);
            if (!serverWorkArea.exists()) {
                boolean suc = serverWorkArea.mkdirs();
            }

            ServerLock.createServerRunningMarkerFile(config); // Unit test runtime code
            sRunningFile = config.getWorkareaFile(BootstrapConstants.SERVER_RUNNING_FILE);
            assertTrue("Running marker file exists after call to obtain", sRunningFile.exists());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            if (config != null) {
                // This method skips/preserves .sRunning file
                FileUtils.recursiveClean(config.getWorkareaFile(null));
            }
        }
        assertTrue("Running marke file exists after call to clean workspace", sRunningFile.exists());
    }

    /**
     * Test to make sure workspace area is cleaned after a JVM ABEND
     */
    @Test
    public void testServerRunningAfterABEND() {

        final String m = "testServerRunningAfterABEND";
        File sRunningFile = null;

        try {
            // Must create parent workarea - server lock code ensures this exists
            File serverWorkArea = config.getWorkareaFile(null);
            if (!serverWorkArea.exists()) {
                boolean suc = serverWorkArea.mkdirs();
            }

            sRunningFile = config.getWorkareaFile(BootstrapConstants.SERVER_RUNNING_FILE);
            sRunningFile.delete(); // Make sure sRunning file isn't there. 

            ServerLock.createServerRunningMarkerFile(config); // Unit test runtime code
            boolean cleanStart = config.checkCleanStart();
            assertFalse("Clean start should not be requested", cleanStart);

            ServerLock.createServerRunningMarkerFile(config); // 2nd call - file will exist, appears as ABEND
            cleanStart = config.checkCleanStart();
            assertTrue("Clean start should be requested", cleanStart);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            if (config != null) {
                // This method skips/preserves .sRunning file
                FileUtils.recursiveClean(config.getWorkareaFile(null));
            }
        }
    }
}
