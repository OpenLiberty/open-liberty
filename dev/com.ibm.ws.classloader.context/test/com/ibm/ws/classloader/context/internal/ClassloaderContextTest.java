package com.ibm.ws.classloader.context.internal;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.LinkedList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wsspi.threadcontext.ThreadContext;

import test.common.SharedOutputManager;

/**
 * Unit tests for ClassloaderContextImpl.
 */
public class ClassloaderContextTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    /**
     * Tests that the context provider creates a context when getContext is invoked.
     *
     * @throws Exception
     */
    @Test
    public void testCtxProviderGetContext() throws Exception {
        assertTrue(getContext() != null);
    }

    /**
     * Tests the correct population and item removal from a context's thread stack.
     *
     * @throws Exception
     */
    @Test
    public void testCLContextTaskStartTaskStopStackPopulationAndRemoval() throws Exception {
        int STACK_ELEMENT_COUNT = 10;

        // Get the current context classLoader on the thread.
        Thread currentThread = Thread.currentThread();
        final ClassLoader origClassloader = currentThread.getContextClassLoader();

        // Remember the original context.
        final ThreadContext cc = getContext();

        // Put a new context classLoader on the thread.
        currentThread.setContextClassLoader(new URLClassLoader(new URL[] {}));
        final ClassLoader testClassloader = currentThread.getContextClassLoader();
        cc.taskStarting();

        try {
            // Call taskStarting a few times with a new context.
            ClassLoader prevClassLoader = testClassloader;
            LinkedList<ThreadContext> stack = new LinkedList<ThreadContext>();
            for (int i = 0; i < STACK_ELEMENT_COUNT; i++) {
                currentThread.setContextClassLoader(new URLClassLoader(new URL[] {}));
                ClassLoader nextClassLoader = currentThread.getContextClassLoader();
                assertTrue(prevClassLoader != nextClassLoader);
                prevClassLoader = nextClassLoader;
                ThreadContext c = cc.clone();
                c.taskStarting();
                stack.addLast(c);
                assertTrue(currentThread.getContextClassLoader() == origClassloader);
            }

            // Call taskStopping as many times.
            for (int i = 0; i < STACK_ELEMENT_COUNT; i++) {
                ThreadContext c = stack.removeLast();
                c.taskStopping();
            }

            // Make sure that we end up with the testClassloader on the thread.
            cc.taskStopping();
            final ClassLoader lastClassloader = currentThread.getContextClassLoader();
            assertTrue(testClassloader == lastClassloader);

            // Make sure the stack for this thread is empty.
            cc.taskStopping();
        } finally {
            // Tidy up.
            currentThread.setContextClassLoader(origClassloader);
            assertTrue(currentThread.getContextClassLoader() == origClassloader);
        }
    }

    /**
     * Tests taskStarting and taskStopping. Normal completion expected.
     *
     * @throws Exception
     */
    @Test
    public void testClassloaderContextTaskStartingStoppingNormalCompletion() throws Exception {

        // Get the current context classLoader on the thread.
        Thread currentThread = Thread.currentThread();
        final ClassLoader origClassloader = currentThread.getContextClassLoader();

        // Remember the current context classLoader.
        final ThreadContext cc = getContext();

        // Set a new  classLoader on the thread.
        currentThread.setContextClassLoader(new URLClassLoader(new URL[] {}));
        final ClassLoader testCL = currentThread.getContextClassLoader();
        assertTrue(testCL != null);

        try {
            // Start the new thread and wait for max of 5 seconds for it to finish.
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Thread currentThread_t2 = Thread.currentThread();

                    // On entry verify that the current context classLoader is testCL.
                    ClassLoader classLoader = currentThread_t2.getContextClassLoader();
                    assertTrue(classLoader != null && classLoader == testCL);

                    // Put the original context classLoader on the thread.
                    cc.taskStarting();

                    // Make sure the original classLoader is truly on the thread.
                    classLoader = currentThread_t2.getContextClassLoader();
                    assertTrue(classLoader != null && classLoader == origClassloader);

                    // Remove the original classLoader from the thread.
                    cc.taskStopping();

                    // Make sure the original context classLoader is off the thread and testCL is back on.
                    classLoader = currentThread_t2.getContextClassLoader();
                    assertTrue(classLoader == testCL);
                }
            };

            ThreadWrapper threadWrapper = new ThreadWrapper(runnable);
            threadWrapper.start();
            threadWrapper.join(5000);

            // Make sure the context classLoader we put on the thread is still the same.
            assertTrue(currentThread.getContextClassLoader() == testCL);
        } finally {
            // Tidy up.
            Thread.currentThread().setContextClassLoader(origClassloader);
            assertTrue(currentThread.getContextClassLoader() == origClassloader);
        }
    }

    @Test
    public void testDefaultContext() {
        // Get the current context classLoader on the thread.
        Thread currentThread = Thread.currentThread();
        final ClassLoader origClassloader = currentThread.getContextClassLoader();

        // Remember the current context classLoader.
        final ThreadContext defaultContext = getDefaultContext();

        // Set a new  classLoader on the thread.
        final ClassLoader newClassloader = new URLClassLoader(new URL[] {});
        currentThread.setContextClassLoader(newClassloader);

        try {
            defaultContext.taskStarting();
            assertSame(ClassLoader.getSystemClassLoader(), currentThread.getContextClassLoader());

            defaultContext.taskStopping();
            assertSame(newClassloader, currentThread.getContextClassLoader());
        } finally {
            Thread.currentThread().setContextClassLoader(origClassloader);
        }
    }

    /**
     * Gets a context object reference.
     *
     * @return The transaction context.
     */
    private ThreadContext getContext() {
        ClassloaderContextProviderImpl clCtxProvider = new ClassloaderContextProviderImpl();
        ThreadContext context = clCtxProvider.captureThreadContext(Collections.<String, String> emptyMap(), null);
        return context;
    }

    private ThreadContext getDefaultContext() {
        ClassloaderContextProviderImpl clCtxProvider = new ClassloaderContextProviderImpl();
        ThreadContext context = clCtxProvider.createDefaultThreadContext(Collections.<String, String> emptyMap());
        return context;
    }

    /**
     * Capture stdout/stderr output to the manager.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * Final teardown work when class is exiting.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    /**
     * Thread wrapper to be able to tie the new spawned thread to the current one for
     * junit assertion error and exception reporting.
     */
    public class ThreadWrapper {
        /** The Thread being wrapped. */
        Thread thread;

        /** Holds any runnable exception being thrown by the runnable execution. */
        Exception exception;

        /** Holds any junit error exception being thrown by the runnable execution. */
        Error error;

        /**
         * Constructor.
         *
         * @param runnable The runnable to execute.
         */
        public ThreadWrapper(final Runnable runnable) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        exception = e;
                    } catch (Error err) {
                        error = err;
                    }
                }
            });
        }

        /**
         * Calls thread.start.
         */
        public void start() {
            thread.start();
        }

        /**
         * Calls thread.join() and checks for any errors.
         *
         * @param time_millis The time for join to wait in milliseconds.
         * @throws Exception
         */
        public void join(long time_millis) throws Exception {
            thread.join(time_millis);
            if (exception != null) {
                throw exception;
            }
            if (error != null) {
                throw error;
            }
        }
    }
}
