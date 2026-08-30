/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.core.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.CountDownLatch;

import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * Provides task-level resmgr cleanup specifically driven during what is assumed to be
 * a hardfailure of this server.
 *
 * On z/OS we have a task-level resmgr for all terminating threads. This class will
 * start a new thread, HardFailureCleanupThread, and mark it natively as a special thread.
 * If/when this thread has the task-level resmgr driven for it, it will look into the
 * server_process_data for any natively "registered" cleanup routines and drive them.
 * There are currently two implementors: AIO and Local Comm. During their activation
 * they will "register" their cleanup routines.
 *
 * The HardFailureCleanupThread thread is started during normal server startup and
 * stopped during normal server shutdown. When this thread is started it calls
 * to native code to mark this thread for the task-level resmgr to notice, and thus,
 * drive the cleanup routines mentioned above.
 */
public class HardFailureNativeCleanup {
    protected static final TraceComponent tc = Tr.register(HardFailureNativeCleanup.class);

    static volatile HardFailureCleanupThread nativeCleanupThread = null;

    /**
     * Mark this thread in native code so that the task-level resmgr will drive
     * any "registered" native cleanup routines.
     *
     * @return int indication of success of native processing.
     */
    protected static native int ntv_hardFailureCleanupActivate();

    /**
     * unMark this thread in native code so that the task-level resmgr will NOT drive
     * any "registered" native cleanup routines.
     *
     * @return int indication of success of native processing.
     */
    protected static native int ntv_hardFailureCleanupDeactivate();

    /**
     * Reference to the native method manager that allows for JNI interaction with native code.
     */
    @Reference
    protected NativeMethodManager nativeMethodManager;

    /**
     * Create the HardFailureCleanupThread.
     */
    public HardFailureNativeCleanup(NativeMethodManager nativeMethodManager) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating HardFailureCleanupThread");
        }

        this.nativeMethodManager = nativeMethodManager;
    }

    /**
     * Start the HardFailureCleanupThread.
     *
     * @return
     */
    public void start() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Activating HardFailureCleanupThread");
        }

        // Register our native code.
        nativeMethodManager.registerNatives(HardFailureNativeCleanup.class);

        // Start a cleanup thread to cover some native cleanup if the server takes a hard failure
        // (ex. kill -9).
        startCleanupThread();
    }

    /**
     * Stop the HardFailureCleanupThread.
     *
     */
    public void stop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Deactivating HardFailureCleanupThread");
        }

        // Stop the cleanup thread that calls some native cleanup if the server takes a hard failure.
        stopCleanupThread();
    }

    /**
     * Inner class for spawning and managing a separate thread to provide cleanup
     * actions on related native code (ex. release ResultHandlers in an MVS PAUSE waiting for
     * Completed IO). This cleanup is in case the normal server shutdown is bypassed by hard failures.
     */
    static protected class HardFailureCleanupThread extends Thread {
        private boolean done = false;
        private CountDownLatch stopLatch = null;

        HardFailureCleanupThread() {
            // Create latch to synchronize shutting down this thread with the calling thread.
            stopLatch = new CountDownLatch(1);
        }

        @FFDCIgnore(value = { InterruptedException.class })
        @Override
        public void run() {
            int rc = -1;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "HardFailureCleanupThread.run() entry");
            }

            try {
                rc = ntv_hardFailureCleanupActivate();
            } finally {
                if (rc != 0) {
                    // Let the thread that may drive stop on this thread continue.
                    stopLatch.countDown();
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "HardFailureCleanupThread.ntv_hardFailureCleanupActivate() rc: " + rc);
            }

            if (rc == 0) {
                try {
                    synchronized (this) {
                        while (!done) {
                            this.wait();
                        }
                    }
                } catch (InterruptedException e) {
                } finally {
                    // Deactivate thread cleanup.
                    int rc2 = -1;

                    rc2 = ntv_hardFailureCleanupDeactivate();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "HardFailureCleanupThread.ntv_hardFailureCleanupDeactivate() rc: " + rc2);
                    }

                    // Let the thread that drove the stop on this thread continue.
                    stopLatch.countDown();
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "HardFailureCleanupThread.run() exit");
            }
        }

        public void end() {
            synchronized (this) {
                done = true;

                // Wake up this thread to terminate.
                this.notify();
            }
        }

        public void waitForStop() {
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "HardFailureCleanupThread.waitForStop() before wait for HardFailureCleanup thread to finish enough.");
                }

                stopLatch.await();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "HardFailureCleanupThread.waitForStop() after wait for HardFailureCleanup thread to finish enough.");
                }
            } catch (InterruptedException ie) {
            }
        }
    }

    private static void startCleanupThread() {
        nativeCleanupThread = AccessController.doPrivileged(new PrivilegedAction<HardFailureCleanupThread>() {
            @Override
            public HardFailureCleanupThread run() {
                HardFailureCleanupThread thread = new HardFailureCleanupThread();
                return thread;
            }
        });

        nativeCleanupThread.setName("zOS Hard failure Cleanup Thread");
        nativeCleanupThread.setDaemon(true);
        nativeCleanupThread.start();
    }

    private static void stopCleanupThread() {
        if (nativeCleanupThread != null) {
            nativeCleanupThread.end();

            // Wait this thread until the cleanup thread got far enough in termination.
            nativeCleanupThread.waitForStop();

            nativeCleanupThread = null;
        }
    }

}