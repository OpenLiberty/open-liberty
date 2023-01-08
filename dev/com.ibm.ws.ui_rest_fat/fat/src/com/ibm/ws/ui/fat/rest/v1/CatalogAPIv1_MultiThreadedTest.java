/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
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
package com.ibm.ws.ui.fat.rest.v1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.ui.fat.APIConstants;
import com.ibm.ws.ui.fat.FATSuite;
import com.ibm.ws.ui.fat.rest.CommonMultiThreadedTest;
import com.ibm.ws.ui.fat.Bookmark;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Validate the version 1 catalog resource add implementation is thread-safe.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class CatalogAPIv1_MultiThreadedTest extends CommonMultiThreadedTest implements APIConstants {
    private static final Class<?> c = CatalogAPIv1_MultiThreadedTest.class;

    public CatalogAPIv1_MultiThreadedTest() {
        super(c);
        url = API_V1_CATALOG;
    }

    /**
     * Reset the catalog to the default state before the test (and after too!)
     */
    @Before
    @After
    public void resetCatalog() throws Exception {
        delete(RESET_CATALOG_URL, adminUser, adminPassword, 200);
    }

    /**
     * Adds a tool to the catalog named threadId-baseVersion.i, where i is 0..max.
     * <p>
     * The expectations are that the returned JSON will match the base Tool fields provided.
     */
    class AsyncGetCatalog extends LatchedRunnable {
        final int max;

        /**
         *
         * @param threadId
         * @param max      "x.y", ".z" will be appended based on loop count
         */
        AsyncGetCatalog(final int max) {
            this.max = max;
        }

        @Override
        public void run() {
            for (int i = 0; (i < max) && (inThreadThrowable == null); i++) {
                try {
                    get(url, adminUser, adminPassword, 200);
                } catch (Throwable t) {
                    inThreadThrowable = t;
                    Log.info(c, "AsyncGetCatalog", "Caught exception: " + t.getMessage(), t);
                    break;
                }
            }
            latch.countDown();
        }
    }

    /**
     * Adds a Bookmark to the catalog named threadId-baseVersion.i, where i is 0..max.
     * <p>
     * The expectations are that the returned JSON will match the base Bookmark fields provided.
     */
    class AsyncAddTool extends LatchedRunnable {
        final String threadId;
        final int max;
        final Bookmark baseTool;

        /**
         *
         * @param threadId
         * @param max         "x.y", ".z" will be appended based on loop count
         * @param baseVersion
         * @param baseTool
         */
        AsyncAddTool(final String threadId, final int max, final Bookmark baseTool) {
            this.threadId = threadId;
            this.max = max;
            this.baseTool = baseTool;
        }

        @Override
        public void run() {
            for (int i = 0; (i < max) && (inThreadThrowable == null); i++) {
                try {
                    String name = threadId + "-" + i;

                    Bookmark newTool = new Bookmark(name, baseTool.getURL(), baseTool.getIcon(), baseTool.getDescription());
                    response = post(url + "/bookmarks", adminUser, adminPassword, newTool.toString().substring(9), 201);
                    Log.info(c, method.getMethodName(), "response: from POST " + response);

                    assertSize(response, 6);
                    assertContains(response, "id", name);
                    assertContains(response, "name", name);
                    assertContains(response, "url", baseTool.getURL());
                    assertContains(response, "icon", baseTool.getIcon());
                    assertContains(response, "description", baseTool.getDescription());                    
                } catch (Throwable t) {
                    inThreadThrowable = t;
                    Log.info(c, "AsyncAddTool-" + threadId, "Caught exception: " + t.getMessage(), t);
                    break;
                }
            }
            latch.countDown();
        }
    }

    /**
     * Gets from the catalog a tool named threadId-baseVersion.i, where i is
     * 0..max.
     * <p>
     * The expectations are that the returned JSON will match the base Tool fields provided.
     */
    class AsyncGetTool extends LatchedRunnable {
        final String threadId;
        final int max;
        final Bookmark baseTool;

        /**
         *
         * @param threadId
         * @param max         "x.y", ".z" will be appended based on loop count
         * @param baseVersion
         * @param baseTool
         */
        AsyncGetTool(final String threadId, final int max, final Bookmark baseTool) {
            this.threadId = threadId;
            this.max = max;
            this.baseTool = baseTool;
        }

        @Override
        public void run() {
            for (int i = 0; (i < max) && (inThreadThrowable == null); i++) {
                try {
                    String id = threadId + "-" + i;

                    // Get the newly created tool1 from the catalog
                    response = get(url + "/bookmarks/" + id, adminUser, adminPassword, 200);
                    Log.info(c, method.getMethodName(), "response: from GET " + response);

                    assertSize(response, 6);
                    assertContains(response, "id", id);
                    assertContains(response, "name", id);
                    assertContains(response, "url", baseTool.getURL());
                    assertContains(response, "icon", baseTool.getIcon());
                    assertContains(response, "description", baseTool.getDescription());
                } catch (Throwable t) {
                    inThreadThrowable = t;
                    Log.info(c, "AsyncGetTool-" + threadId, "Caught exception", t);
                    break;
                }
            }
            latch.countDown();
        }
    }

    /**
     * Deletes from the catalog a tool named threadId-baseVersion.i, where i is
     * 0..max.
     * <p>
     * The expectations are that the tools being removed and the returned JSON will match the base Tool fields provided.
     */
    class AsyncDeleteTool extends LatchedRunnable {
        final String threadId;
        final int max;
        final Bookmark baseTool;

        /**
         *
         * @param threadId
         * @param max         "x.y", ".z" will be appended based on loop count
         * @param baseVersion
         * @param baseTool
         */
        AsyncDeleteTool(final String threadId, final int max, final Bookmark baseTool) {
            this.threadId = threadId;
            this.max = max;
            this.baseTool = baseTool;
        }

        @Override
        public void run() {
            for (int i = 0; i < max; i++) {
                try {
                    String id = threadId + "-" + i;

                    // Get the newly created tool1 from the catalog
                    response = delete(url + "/bookmarks/" + id, adminUser, adminPassword, 200);
                    Log.info(c, method.getMethodName(), "response: from DELETE " + response);

                    assertSize(response, 6);
                    assertContains(response, "id", id);
                    assertContains(response, "name", id);
                    assertContains(response, "url", baseTool.getURL());
                    assertContains(response, "icon", baseTool.getIcon());
                    assertContains(response, "description", baseTool.getDescription());
                } catch (Throwable t) {
                    Log.info(c, "AsyncDeleteTool-" + threadId, "Caught exception", t);
                    inThreadThrowable = t;
                    break;
                }
            }
            latch.countDown();
        }
    }

    /**
     * Attempts to add many Bookmark concurrently. This is not a normally
     * anticipated flow, but we have to support it!
     */
    @Test
    public void modifyCatalogConcurrentAccess() throws Exception {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("z/os") || (os.contains("win"))) {
            // Windows and Z machines take well over 30 minutes to run this test - not worth it
            Log.info(c, "modifyCatalogConcurrentAccess", "Will not run on zOS or windows currently");
            return;
        } else {
            // A set of tools that look similar. Name and version will be overridden
            Bookmark t1Tool = new Bookmark("willOverrideWithThreadId", "http://concurrencytest.com", "https://concurrencytest.com/favicon.png", "Concurrent Test Tool for Thread 1");
            Bookmark t2Tool = new Bookmark("willOverrideWithThreadId", "http://concurrencytest.com", "https://concurrencytest.com/favicon.png", "Concurrent Test Tool for Thread 2");
            Bookmark t3Tool = new Bookmark("willOverrideWithThreadId", "http://concurrencytest.com", "https://concurrencytest.com/favicon.png", "Concurrent Test Tool for Thread 3");
            Bookmark t4Tool = new Bookmark("willOverrideWithThreadId", "http://concurrencytest.com", "https://concurrencytest.com/favicon.png", "Concurrent Test Tool for Thread 4");

            final List<LatchedRunnable> addThreads = new ArrayList<LatchedRunnable>();
            addThreads.add(new AsyncAddTool("t1", THREAD_LOOP_COUNT, t1Tool));
            addThreads.add(new AsyncAddTool("t2", THREAD_LOOP_COUNT, t2Tool));
            addThreads.add(new AsyncAddTool("t3", THREAD_LOOP_COUNT, t3Tool));
            addThreads.add(new AsyncAddTool("t4", THREAD_LOOP_COUNT, t4Tool));
            // While things are changing, get the catalog
            addThreads.add(new AsyncGetCatalog(THREAD_LOOP_COUNT));

            Log.info(c, method.getMethodName(), "Spawning " + addThreads.size() + " threads to perform a total of " + 4 * THREAD_LOOP_COUNT + " tool additions");
            spawnThreads(addThreads);

            assertNull("FAIL: An exception was caught in at least one of the 'add' concurrency invocations: " + inThreadThrowable,
                       inThreadThrowable);

            final List<LatchedRunnable> getThreads = new ArrayList<LatchedRunnable>();
            getThreads.add(new AsyncGetTool("t1", THREAD_LOOP_COUNT, t1Tool));
            getThreads.add(new AsyncGetTool("t2", THREAD_LOOP_COUNT, t2Tool));
            getThreads.add(new AsyncGetTool("t3", THREAD_LOOP_COUNT, t3Tool));
            getThreads.add(new AsyncGetTool("t4", THREAD_LOOP_COUNT, t4Tool));

            Log.info(c, method.getMethodName(), "Spawning " + getThreads.size() + " threads to perform a total of " + 4 * THREAD_LOOP_COUNT + " tool gets");
            spawnThreads(getThreads);

            assertNull("FAIL: An exception was caught in at least one of the 'get' concurrency invocations: " + inThreadThrowable,
                       inThreadThrowable);

            final List<LatchedRunnable> deleteThreads = new ArrayList<LatchedRunnable>();
            deleteThreads.add(new AsyncDeleteTool("t1", THREAD_LOOP_COUNT, t1Tool));
            deleteThreads.add(new AsyncDeleteTool("t2", THREAD_LOOP_COUNT, t2Tool));
            deleteThreads.add(new AsyncDeleteTool("t3", THREAD_LOOP_COUNT, t3Tool));
            deleteThreads.add(new AsyncDeleteTool("t4", THREAD_LOOP_COUNT, t4Tool));
            // While things are changing, get the catalog
            deleteThreads.add(new AsyncGetCatalog(THREAD_LOOP_COUNT));

            Log.info(c, method.getMethodName(), "Spawning " + deleteThreads.size() + " threads to perform a total of " + 4 * THREAD_LOOP_COUNT + " tool deletes");
            spawnThreads(deleteThreads);

            assertNull("FAIL: An exception was caught in at least one of the concurrency invocations: " + inThreadThrowable,
                       inThreadThrowable);
        }
    }

}
