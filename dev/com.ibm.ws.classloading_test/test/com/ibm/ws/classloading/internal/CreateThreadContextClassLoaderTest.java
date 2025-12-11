/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.TestUtil.createAppClassloader;
import static com.ibm.ws.classloading.internal.TestUtil.getClassLoadingService;
import static com.ibm.ws.classloading.internal.TestUtil.getOtherClassesURL;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.classloading.internal.TestUtil.ClassSource;
import com.ibm.wsspi.classloading.ClassLoadingService;

import test.common.SharedOutputManager;

public class CreateThreadContextClassLoaderTest {

    @Rule
    public final SharedOutputManager outputManager = SharedOutputManager.getInstance();

    /**
     * Create two ThreadContextClassLoaders from two different AppClassLoaders with the same ID.
     */
    @Test
    public void testCreatingThreadContextClassLoaders() throws Exception {
        // create two class loaders with the same ID
        String id = this.getClass().getName();
        URL url = getOtherClassesURL(ClassSource.A);
        AppClassLoader appLoader1 = createAppClassloader(id, url, false);
        AppClassLoader appLoader2 = createAppClassloader(id, url, false);

        ClassLoadingService cls = getClassLoadingService(null);
        ClassLoader tccl1 = cls.createThreadContextClassLoader(appLoader1);
        ClassLoader tccl2 = cls.createThreadContextClassLoader(appLoader2);
        assertThat("Creating ThreadContextClassLoaders from two AppClassLoaders with the same ID should produce two differents instances.",
                   tccl1,
                   is(not(sameInstance(tccl2))));
    }

    @Test
    public void testThreadContextClassLoaderMemoryLeakParentLast() throws Exception {
        testThreadContextClassLoaderMemoryLeak(true, false);
    }

    @Test
    public void testThreadContextClassLoaderMemoryLeakParentFirst() throws Exception {
        testThreadContextClassLoaderMemoryLeak(false, false);
    }

    @Test
    public void testThreadContextClassLoaderMemoryLeakParentLastSecondThread() throws Exception {
        testThreadContextClassLoaderMemoryLeak(true, true);
    }

    @Test
    public void testThreadContextClassLoaderMemoryLeakParentFirstSecondThread() throws Exception {
        testThreadContextClassLoaderMemoryLeak(false, true);
    }

    private void testThreadContextClassLoaderMemoryLeak(boolean parentLast, boolean secondThread) throws Exception {
        // create two class loaders with the same ID
        String id = "testThreadContextClassLoaderMemoryLeak " + parentLast;
        URL url = getOtherClassesURL(ClassSource.A);
        AppClassLoader appLoader = createAppClassloader(id, url, parentLast);

        ClassLoadingService cls = getClassLoadingService(null);
        ClassLoader tccl = cls.createThreadContextClassLoader(appLoader);

        assertTrue(((ThreadContextClassLoader) tccl).isFor(appLoader));

        // Set the AppClassLoader into the thread to keep it in memory outside of in this method
        Thread.currentThread().setContextClassLoader(appLoader);

        WeakReference<ClassLoader> weakRef = new WeakReference<>(appLoader);

        doGarbageCollectCheck(weakRef, false);
        assertNotNull("AppClassLoader instance should not garbage collect", weakRef.get());

        tccl.loadClass("test.DummyServlet");

        Thread.currentThread().setContextClassLoader(tccl);

        // Do a second thread scenario to show that the new thread inherits the Thread context classloader of
        // the current thread
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> exception = new AtomicReference<>();
        Thread newThread = null;
        if (secondThread) {
            newThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        latch.await();
                        doLoadClassAfterGC(outputManager, "secondThread", true);
                    } catch (Exception e) {
                        exception.set(e);
                    }
                }
            }, "secondThread");
            assertSame(tccl, newThread.getContextClassLoader());
            newThread.start();
        }

        appLoader = null;
        tccl = null;

        doGarbageCollectCheck(weakRef, true);
        assertNull("AppClassLoader instance should have been able to garbage collect", weakRef.get());

        tccl = Thread.currentThread().getContextClassLoader();

        assertTrue(((ThreadContextClassLoader) tccl).isFor(null));

        if (secondThread) {
            assertTrue("Thread isn't alive", newThread.isAlive());
            latch.countDown();
            newThread.join(10000);
            assertFalse("Thread didn't complete", newThread.isAlive());
            Exception e = exception.get();
            assertNull(e);
            outputManager.resetStreams();
        }

        doLoadClassAfterGC(outputManager, id, !secondThread);
    }

    static void doLoadClassAfterGC(SharedOutputManager outputManager, String threadName, boolean expectMessage) throws Exception {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            tccl.loadClass("test.DummyServlet");
            fail("Expected a ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // expected since the app classloader has garbage collected
        }

        // If we change to output the message for every thread, this test will fail and need to be updated to be assertTrue
        assertEquals(expectMessage, outputManager.checkForMessages("CWWKL0020W: .*" + threadName + ".*"));
        outputManager.resetStreams();

        // verify that you can still load a Java class from a ThreadContextClassLoader that has had
        // its app class loader garbage collected. If we didn't call the parent still when the app
        // class loader garbage collects this scenario would fail
        tccl.loadClass("java.nio.charset.StandardCharsets");

        // The message should only come out once.
        assertFalse(outputManager.checkForMessages("CWWKL0020W: .*" + threadName));
    }

    private void doGarbageCollectCheck(WeakReference<ClassLoader> weakRef, boolean nullExpected) {
        for (int i = 0; i < 10; ++i) {
            System.out.println("doGarbageCollectCheck calling system gc i " + i);
            System.gc();
            // give the GC some time to actually do its thing
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (nullExpected && weakRef.get() == null) {
                break;
            }

            if (!nullExpected && weakRef.get() != null) {
                break;
            }
        }
    }
}
