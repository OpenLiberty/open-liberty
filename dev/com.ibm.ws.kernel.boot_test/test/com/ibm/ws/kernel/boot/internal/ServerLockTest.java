/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;
import test.shared.TestUtils;

import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.SharedBootstrapConfig;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 *
 */
public class ServerLockTest {
    static SharedOutputManager outputMgr;
    static SharedBootstrapConfig config;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        File f = TestUtils.createTempFile("ResourceUtilsTest", "tmp");
        f.delete();
        f.mkdir();

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

    @Before
    public void setUp() {
        TestUtils.cleanTempFiles();
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.boot.internal.KernelUtils#checkServerLock(java.io.File, java.lang.String)} .
     * Additional testing of deleting the lock file at server shutdown
     */
    @Test
    public void testCheckServerLock() {
        final String m = "testCheckServerLock";

        try {
            File sLockFile = config.getWorkareaFile(BootstrapConstants.S_LOCK_FILE);
            assertTrue("Parent dirs should exist", sLockFile.getParentFile().exists() || sLockFile.getParentFile().mkdirs());

            ServerLock serverLock = ServerLock.createServerLock(config);
            try {
                // get lock for testServer1
                serverLock.obtainServerLock();
                assertTrue("Lock file exists after call to obtain", sLockFile.exists());
            } finally {
                serverLock.releaseServerLock();
                sLockFile.delete();
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            if (config != null) {
                // This method skips/preserves .sLock files
                FileUtils.recursiveClean(config.getWorkareaFile(null));
            }
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.boot.internal.KernelUtils#checkServerLock(java.io.File, java.lang.String)} .
     * Test permissions surrounding get/obtain server lock
     */
    @Test
    public void testCheckServerLockPermissions() {
        final String m = "testCheckServerLockPermissions";

        try {
            File tmpServerDir = config.getConfigFile(null);
            File workArea = config.getWorkareaFile(null);

            // Test a write-only server directory
            //we use an assume instead of an assert here because the test has no
            //meaning and should be ignored if it could not set the directory to
            //be unwritable (e.g. on Windows)
            assumeTrue(tmpServerDir.setWritable(false, false));
            try {
                // only test behavior with read-only directory if we could make the dir read-only: bug 48667
                if (tmpServerDir.canWrite() == false) {
                    assertFalse("tmpServerDir() should be false after setWritable(false)", tmpServerDir.canWrite());
                    ServerLock.createServerLock(config);
                    throw new Exception("Missed expected launch exception with unwritable server directory");
                }
            } catch (LaunchException le) {
                // Expected exception 
                String translatedMsg = le.getTranslatedMessage();
                assertNotNull("Exception should contain translated message", translatedMsg);
                assertTrue("Exception should contain console message CWWKE0044E", translatedMsg.contains("CWWKE0044E"));
                assertFalse("Server workarea should not have been created", workArea.isDirectory());
            } finally {
                tmpServerDir.setWritable(true, false);
            }

            assertTrue("couldn't create workarea", workArea.mkdirs());

            // Make allocated workarea read-only
            assertTrue("must be able to make server workarea unwritable", workArea.setWritable(false, false));
            try {
                // only test behavior with read-only directory if we could make the dir read-only: bug 48667
                if (workArea.canWrite() == false) {
                    assertFalse("workArea.canWrite() should be false after setWritable(false)", workArea.canWrite());
                    ServerLock.createServerLock(config);
                    throw new Exception("Missed expected launch exception with read-only workarea");
                }
            } catch (LaunchException le) {
                // Expected exception 
                String translatedMsg = le.getTranslatedMessage();
                assertNotNull("Exception should contain translated message", translatedMsg);
                assertTrue("Exception should contain console message CWWKE0044E", translatedMsg.contains("CWWKE0044E"));
            } finally {
                workArea.setWritable(true, false);
            }

            // Allocate lock file, make it read-only
            File lockFile = new File(workArea, BootstrapConstants.S_LOCK_FILE);
            lockFile.createNewFile();
            assertTrue("must be able to make lockfile unwritable", lockFile.setWritable(false, false));
            try {
                // only test behavior with read-only directory if we could make the dir read-only: bug 48667
                if (workArea.canWrite() == false) {
                    assertFalse("lockFile.canWrite() should be false after setWritable(false)", lockFile.canWrite());
                    ServerLock.createServerLock(config);
                    throw new Exception("Missed expected launch exception with unwritable lock file");
                }
            } catch (LaunchException le) {
                // Expected exception 
                String translatedMsg = le.getTranslatedMessage();
                assertNotNull("Exception should contain translated message", translatedMsg);
                assertTrue("Exception should contain console message CWWKE0044E", translatedMsg.contains("CWWKE0044E"));
            } finally {
                lockFile.setWritable(true, false);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            if (config != null) {
                FileUtils.recursiveClean(config.getWorkareaFile(null));
            }
        }
    }

    @Test
    public void testCheckServerLockAlreadyLocked() {
        final String m = "testCheckServerLockAlreadyLocked";

        try {
            File workArea = config.getWorkareaFile(null);
            assertTrue("Parent dirs should exist", workArea.isDirectory() || workArea.mkdirs());

            ServerLock serverLock = null;
            File lockFile = null;
            FileOutputStream fos = null;
            FileChannel fc = null;
            FileLock lock = null;

            try {
                serverLock = ServerLock.createServerLock(config);
                lockFile = new File(workArea, BootstrapConstants.S_LOCK_FILE);
                fos = new FileOutputStream(lockFile);
                fc = fos.getChannel();
                lock = fc.lock();

                // Try to obtain with lock already held.
                serverLock.obtainServerLock();
                throw new Exception("Missed expected launch exception: lock already held");
            } catch (LaunchException le) {
                // Expected exception 
                String translatedMsg = le.getTranslatedMessage();
                assertNotNull("Exception should contain translated message", translatedMsg);
                assertTrue("Exception should contain console message CWWKE0029E", translatedMsg.contains("CWWKE0029E"));
            } finally {
                if (lock != null) {
                    lock.release();
                    lockFile.delete();
                }
                if (!Utils.tryToClose(fc)) {
                    Utils.tryToClose(fos);
                }
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            if (config != null)
                FileUtils.recursiveClean(config.getWorkareaFile(null));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.boot.internal.FileUtils#recursiveClean(java.io.File)} .
     * Additional testing of not deleting the lock file.
     */
    @Test
    public void testRecursiveCleanWithLock() {
        final String m = "testRecursiveCleanWithLock";

        try {
            ServerLock.createServerLock(config);
            File workarea = config.getWorkareaFile(null);
            assertTrue("Parent dirs should exist", workarea.isDirectory() || workarea.mkdirs());

            File sLockFile = config.getWorkareaFile(BootstrapConstants.S_LOCK_FILE);
            File nonLockFile = config.getWorkareaFile("notLock.file");

            assertTrue(".sLock file should exist pre-test", sLockFile.exists() || sLockFile.createNewFile());
            assertTrue("nonLockFile should exist pre-test", nonLockFile.exists() || nonLockFile.createNewFile());

            FileUtils.recursiveClean(config.getConfigFile(null));

            assertFalse("non-lock file should not exist (deleted)", nonLockFile.exists());
            assertTrue("lock file should exist (untouched)", sLockFile.exists());
            assertTrue("workarea directory should exist (untouched-- preserve in-use .sLock file)", workarea.exists());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
