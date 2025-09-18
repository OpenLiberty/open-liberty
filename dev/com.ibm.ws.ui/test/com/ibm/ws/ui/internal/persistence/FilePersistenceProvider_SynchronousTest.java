/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.ui.internal.persistence;

import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.websphere.jsonsupport.JSON;
import com.ibm.ws.ui.internal.v1.ICatalog;
import com.ibm.ws.ui.persistence.IPersistenceDebugger;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;

/**
 * Multi-threaded concurrency test. Two threads contend for the same
 * resource, while a second pair of thread contends for an alternate
 * resource. This alternate resource design is on purpose because of
 * how things are implemented in the FilePersistenceProvider.
 * 
 * Thread 1: persist nameA
 * Thread 2: persist nameA
 * Thread 3: persist nameB
 * Thread 4: persist nameB
 * 
 * Perform 10,000 operations in each thread.
 * 
 * The test model is not so much around the ultimate correctness of
 * the access sequence as it is to test that:
 * 1. nameA and nameB are only created one time.
 * 2. Invocations to each resource are properly within count.
 * 
 * How can we check order? We essentially can not because we can not predict
 * the order of the operations. There is no way to know which operation will
 * ultimately win out and execute last, and because the synchronicity is handled
 * inside the method, we can't really sense with a mock expectations which
 * thread will set up the mock and get into the inner lock first.
 * 
 * Note that the use of the ICatalog interface is not special. It was chosen
 * as its an interface and is therefore mockable.
 */
public class FilePersistenceProvider_SynchronousTest {
    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<WsLocationAdmin> locRef = mock.mock(ServiceReference.class);
    private final WsLocationAdmin loc = mock.mock(WsLocationAdmin.class);
    private final WsResource resource = mock.mock(WsResource.class);
    private final IPersistenceDebugger mockDebugger = mock.mock(IPersistenceDebugger.class);
    private final JSON mockJson = mock.mock(JSON.class);

    private final int THREAD_LOOP_COUNT = 10000;

    private static volatile Throwable inThreadThrowable = null;
    private FilePersistenceProvider persist;

    /**
     * Activate the service using with mocks. Also reset the static thread
     * Throwable detector to null.
     */
    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(FilePersistenceProvider.KEY_LOCATION_SERVICE, locRef);
                will(returnValue(loc));
            }
        });

        persist = new FilePersistenceProvider(mockJson, mockDebugger);
        persist.setLocationService(locRef);
        persist.activate(cc);

        inThreadThrowable = null;
    }

    /**
     * Deactive the service.
     */
    @After
    public void tearDown() {
        persist.deactivate();
        persist.unsetLocationService(locRef);
        persist = null;

        mock.assertIsSatisfied();
    }

    /**
     * Create a mock File which will successfully create its parent directories.
     * 
     * @return The mock File
     */
    private File createMockFile(final String name, final boolean isStorePath, final int count) {
        final File mockFile = mock.mock(File.class, name);
        final File mockParentFile = mock.mock(File.class, name + ".parent");

        mock.checking(new Expectations() {
            {
                allowing(mockFile).getAbsolutePath();
                will(returnValue("mockFile"));
            }
        });

        if (isStorePath)
            mock.checking(new Expectations() {
                {
                    exactly(count).of(mockFile).getParentFile();
                    will(returnValue(mockParentFile));

                    exactly(1).of(mockParentFile).exists();
                    will(returnValue(false));

                    exactly(1).of(mockParentFile).mkdirs();
                    will(returnValue(true));

                    exactly(count - 1).of(mockParentFile).exists();
                    will(returnValue(true));
                }
            });

        return mockFile;
    }

    /**
     * Sets up the mock so that the requested name will return a File object.
     * That File can be the file passed in, or it will be created if null.
     * 
     * @param name Expected persistence name
     * @param mockFile Desired file to return. Can be {@code null}
     * @return The File (which may be a mock)
     */
    private File setMockFileLocation(final String name, final File mockFile) {
        final File f = (mockFile != null) ? mockFile : new File("build/unittest/testdata/" + name + ".json");

        mock.checking(new Expectations() {
            {
                one(loc).resolveResource(FilePersistenceProvider.DEFAULT_PERSIST_LOCATION + name + ".json");
                will(returnValue(resource));

                one(resource).asFile();
                will(returnValue(f));
            }
        });

        return f;
    }

    /**
     * Set the mapper to expect read calls to the given file and class.
     * 
     * @param f Expected file
     * @param c Expected class
     * @param expectedCalls Expected number of invocations
     */
    private void setMapperExpectedLoad(final File f, final Class<?> c, final int expectedCalls) throws Exception {
        mock.checking(new Expectations() {
            {
                exactly(expectedCalls).of(mockJson).parse(f, c);
            }
        });
    }

    /**
     * Set the mapper to expect write calls to the given file and class.
     * 
     * @param f Expected file
     * @param o Expected object
     * @param expectedCalls Expected number of invocations
     */
    private void setMapperExpectedStore(final File f, final Object o, final int expectedCalls) throws Exception {
        mock.checking(new Expectations() {
            {
                exactly(expectedCalls).of(mockJson).serializeToFile(f, o);
            }
        });
    }

    /**
     * Set the mapper to expect write calls to the given file and class.
     * 
     * @param f Expected file
     * @param expectedCalls Expected number of invocations
     */
    private void setFileExpecteLastModified(final File f, final int expectedCalls) throws Exception {
        mock.checking(new Expectations() {
            {
                exactly(expectedCalls).of(f).lastModified();
            }
        });
    }

    /**
     * Define a LatchedRunnable, in which the latch is setable.
     */
    abstract class LatchedRunnable implements Runnable {
        CountDownLatch latch = null;

        void setCountDownLatch(CountDownLatch latch) {
            this.latch = latch;
        }
    }

    /**
     * Spawn all of the threads in the list and wait for them all to finish.
     * 
     * @param threads
     * @throws InterruptedException
     */
    private void spawnThreads(final List<LatchedRunnable> threads) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(threads.size());

        // Set the latch
        for (LatchedRunnable thread : threads) {
            thread.setCountDownLatch(latch);
        }

        // Spawn the threads
        for (Runnable thread : threads) {
            thread.run();
        }

        // Wait for all threads to finish
        latch.await();
    }

    class LoadTest extends LatchedRunnable {
        final String threadId;
        final int max;
        final String persistName;
        final Class<?> c;

        LoadTest(final String threadId, final int max, final String persistName, final Class<?> c) {
            this.threadId = threadId;
            this.max = max;
            this.persistName = persistName;
            this.c = c;
        }

        @Override
        public void run() {
            for (int i = 0; (i < max) && (inThreadThrowable == null); i++) {
                try {
                    persist.load(persistName, c);
                } catch (Throwable t) {
                    inThreadThrowable = t;
                    t.printStackTrace();
                    break;
                }
            }
            latch.countDown();
        }
    }

    @Test
    public void loadConcurrentAccess() throws Exception {
        final String nameA = "nameA";
        final String nameB = "nameB";
        final Class<ICatalog> c = ICatalog.class;

        final File fileA = createMockFile(nameA, false, 0);
        setMockFileLocation(nameA, fileA);

        final File fileB = createMockFile(nameB, false, 0);
        setMockFileLocation(nameB, fileB);

        setMapperExpectedLoad(fileA, c, 2 * THREAD_LOOP_COUNT);
        setMapperExpectedLoad(fileB, c, 2 * THREAD_LOOP_COUNT);

        final List<LatchedRunnable> threads = new ArrayList<LatchedRunnable>();
        threads.add(new LoadTest("t1", THREAD_LOOP_COUNT, nameA, c));
        threads.add(new LoadTest("t2", THREAD_LOOP_COUNT, nameA, c));
        threads.add(new LoadTest("t3", THREAD_LOOP_COUNT, nameB, c));
        threads.add(new LoadTest("t4", THREAD_LOOP_COUNT, nameB, c));

        spawnThreads(threads);

        assertNull("FAIL: An exception was caught in at least one of the concurrency invocations: " + inThreadThrowable,
                   inThreadThrowable);
    }

    class StoreTest extends LatchedRunnable {
        final String threadId;
        final int max;
        final String persistName;
        final Object pojo;

        StoreTest(final String threadId, final int max, final String persistName, final Object pojo) {
            this.threadId = threadId;
            this.max = max;
            this.persistName = persistName;
            this.pojo = pojo;
        }

        @Override
        public void run() {
            for (int i = 0; (i < max) && (inThreadThrowable == null); i++) {
                try {
                    persist.store(persistName, pojo);
                } catch (Throwable t) {
                    inThreadThrowable = t;
                    t.printStackTrace();
                    break;
                }
            }
            latch.countDown();
        }
    }

    /**
     * Build a StoreTest and set the appropriate expectations.
     */
    private StoreTest createStoreTestWithExpectations(final String threadId,
                                                      final int loopCount,
                                                      final String persistName,
                                                      final Object pojo,
                                                      final File persistFile) throws Exception {
        setMapperExpectedStore(persistFile, pojo, loopCount);
        return new StoreTest(threadId, loopCount, persistName, pojo);
    }

    @Test
    public void storeConcurrentAccess() throws Exception {
        final String nameA = "nameA";
        final String nameB = "nameB";

        final File fileA = createMockFile(nameA, true, 2 * THREAD_LOOP_COUNT);
        setMockFileLocation(nameA, fileA);

        final File fileB = createMockFile(nameB, true, 2 * THREAD_LOOP_COUNT);
        setMockFileLocation(nameB, fileB);

        final List<LatchedRunnable> threads = new ArrayList<LatchedRunnable>();
        threads.add(createStoreTestWithExpectations("t1", THREAD_LOOP_COUNT, nameA, "dataA1", fileA));
        threads.add(createStoreTestWithExpectations("t2", THREAD_LOOP_COUNT, nameA, "dataA2", fileA));
        threads.add(createStoreTestWithExpectations("t3", THREAD_LOOP_COUNT, nameB, "dataB3", fileB));
        threads.add(createStoreTestWithExpectations("t4", THREAD_LOOP_COUNT, nameB, "dataB4", fileB));

        spawnThreads(threads);

        assertNull("FAIL: An exception was caught in at least one of the concurrency invocations: " + inThreadThrowable,
                   inThreadThrowable);
    }

    class LastModifiedTest extends LatchedRunnable {
        final String threadId;
        final int max;
        final String persistName;

        LastModifiedTest(final String threadId, final int max, final String persistName) {
            this.threadId = threadId;
            this.max = max;
            this.persistName = persistName;
        }

        @Override
        public void run() {
            for (int i = 0; (i < max) && (inThreadThrowable == null); i++) {
                try {
                    persist.getLastModified(persistName);
                } catch (Throwable t) {
                    inThreadThrowable = t;
                    t.printStackTrace();
                    break;
                }
            }
            latch.countDown();
        }
    }

    @Test
    public void getLastModifiedConcurrentAccess() throws Exception {
        final String nameA = "nameA";
        final String nameB = "nameB";

        final File fileA = createMockFile(nameA, false, 0);
        setMockFileLocation(nameA, fileA);

        final File fileB = createMockFile(nameB, false, 0);
        setMockFileLocation(nameB, fileB);

        setFileExpecteLastModified(fileA, 2 * THREAD_LOOP_COUNT);
        setFileExpecteLastModified(fileB, 2 * THREAD_LOOP_COUNT);

        final List<LatchedRunnable> threads = new ArrayList<LatchedRunnable>();
        threads.add(new LastModifiedTest("t1", THREAD_LOOP_COUNT, nameA));
        threads.add(new LastModifiedTest("t2", THREAD_LOOP_COUNT, nameA));
        threads.add(new LastModifiedTest("t3", THREAD_LOOP_COUNT, nameB));
        threads.add(new LastModifiedTest("t4", THREAD_LOOP_COUNT, nameB));

        spawnThreads(threads);

        assertNull("FAIL: An exception was caught in at least one of the concurrency invocations: " + inThreadThrowable,
                   inThreadThrowable);
    }
}
