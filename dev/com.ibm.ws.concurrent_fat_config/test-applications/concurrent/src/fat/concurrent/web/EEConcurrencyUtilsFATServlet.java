/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.concurrent.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import componenttest.app.FATServlet;
import fat.concurrent.ejb.EEConcurrencyUtilsStatelessBean;
import jakarta.enterprise.concurrent.ManagedTask;

@SuppressWarnings("serial")
@WebServlet("/*")
public class EEConcurrencyUtilsFATServlet extends FATServlet {

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    /**
     * Map of futures to save across servlet invocations.
     */
    private Map<String, Future<?>> futures;

    @Resource
    private UserTransaction tran;

    @SuppressWarnings("unchecked")
    @Override
    public void init() throws ServletException {
        // An OSGi service is used to cache our list of futures across application restart
        org.osgi.framework.BundleContext bundleContext = org.osgi.framework.FrameworkUtil.getBundle(getClass().getClassLoader().getClass()).getBundleContext();
        @SuppressWarnings("rawtypes")
        Collection<org.osgi.framework.ServiceReference<Map>> refs;
        try {
            refs = bundleContext.getServiceReferences(Map.class, "(id=EEConcurrencyUtilsFATServlet.futures)");
        } catch (org.osgi.framework.InvalidSyntaxException x) {
            throw new ServletException(x);
        }
        if (refs.isEmpty()) {
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put("id", "EEConcurrencyUtilsFATServlet.futures");
            bundleContext.registerService(Map.class, futures = new HashMap<String, Future<?>>(), props);
        } else {
            futures = (Map<String, Future<?>>) bundleContext.getService(refs.iterator().next());
        }
    }

    @Override
    protected void invokeTest(String method, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String execSvc = request.getParameter("managedExecutorService");
        PrintWriter out = response.getWriter();
        getClass().getMethod(method, String.class, PrintWriter.class).invoke(this, execSvc, out);
    }

    /**
     * Verify that a managed task can load classes with the application's classloader.
     *
     * @param execSvcJNDIName the managed executor service to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testClassloaderContext(String execSvcJNDIName, PrintWriter out) throws Throwable {

        final String className = getClass().getName();

        final Callable<Class<?>> loadClass = new Callable<Class<?>>() {
            @Override
            public Class<?> call() throws Exception {
                System.out.println("running task");
                return Thread.currentThread().getContextClassLoader().loadClass(className);
            }
        };

        // Class load from current thread should work
        Class<?> loadedClass = loadClass.call();
        if (!loadedClass.getName().equals(className))
            throw new Exception("Unexpected class loaded on current thread: " + loadedClass);

        // Class load from managed executor service thread (with classloaderContext) should work
        ExecutorService execSvc = (ExecutorService) new InitialContext().lookup(execSvcJNDIName);
        Future<Class<?>> future = execSvc.submit(loadClass);
        try {
            loadedClass = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        } finally {
            future.cancel(true);
        }

        if (!loadedClass.getName().equals(className))
            throw new Exception("Unexpected class loaded on managed executor service thread: " + loadedClass);
    }

    /**
     * Verify that a managed task runs with access to the application component namespace.
     *
     * @param execSvcJNDIName the managed executor service to use
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testJEEMetadataContext(String execSvcJNDIName, PrintWriter out) throws Exception {

        final BlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        final Runnable javaCompLookup = new Runnable() {
            @Override
            public void run() {
                System.out.println("running task");
                try {
                    results.add(new InitialContext().lookup("java:comp/env/entry1"));
                } catch (Throwable x) {
                    results.add(x);
                }
            }
        };

        // Lookup from current thread should work
        javaCompLookup.run();
        Object result = results.poll();
        if (result instanceof Throwable)
            throw new ExecutionException((Throwable) result);
        if (!"value1".equals(result))
            throw new Exception("Unexpected value for java:comp/env/entry1 from current thread: " + result);

        // Lookup from managed executor service thread (with jeeMetadataContext) should work
        ExecutorService execSvc = (ExecutorService) new InitialContext().lookup(execSvcJNDIName);
        Future<BlockingQueue<Object>> future = execSvc.submit(javaCompLookup, results);
        try {
            result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS).remove();
        } finally {
            future.cancel(true);
        }

        if (result instanceof Exception)
            throw (Exception) result;
        else if (result instanceof Throwable)
            throw new ExecutionException((Throwable) result);

        if (!"value1".equals(result))
            throw new Exception("Unexpected value for java:comp/env/entry1 from new thread: " + result);
    }

    /**
     * Verify that a managed task submitted from and EJB runs with access to the application component namespace.
     *
     * @param unused ignored
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testJEEMetadataContextFromEJB(String unused, PrintWriter out) throws Exception {
        EEConcurrencyUtilsStatelessBean ejb = (EEConcurrencyUtilsStatelessBean) new InitialContext()
                        .lookup("java:global/concurrent/EEConcurrencyUtilsStatelessBean!" + EEConcurrencyUtilsStatelessBean.class.getCanonicalName());
        ejb.testJEEMetadataContextExecSvc1();
    }

    /**
     * Submit 1 long running task and verify it completes successfully.
     */
    public void testLongRunningTaskSuccessful(String execSvcJNDIName, PrintWriter out) throws Exception {
        ExecutorService executor = InitialContext.doLookup(execSvcJNDIName);
        IncrementTask task = new IncrementTask(null, null, null, null);
        task.getExecutionProperties().put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());
        Future<Integer> future = executor.submit(task);
        assertEquals(Integer.valueOf(1), future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Verify that a managed task does not run with the application's classloader.
     *
     * @param execSvcJNDIName the managed executor service to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testNoClassloaderContext(String execSvcJNDIName, PrintWriter out) throws Exception {
        final Callable<ClassLoader> getClassLoader = new Callable<ClassLoader>() {
            @Override
            public ClassLoader call() throws Exception {
                System.out.println("running task");
                return Thread.currentThread().getContextClassLoader();
            }
        };

        // Class load from current thread should work
        ClassLoader servletClassLoader = getClassLoader.call();

        // Class loader from managed executor service thread (without classloaderContext) should not match the class loader of the servlet
        ExecutorService execSvc = (ExecutorService) new InitialContext().lookup(execSvcJNDIName);
        Future<ClassLoader> future = execSvc.submit(getClassLoader);
        try {
            ClassLoader taskClassLoader = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            if (servletClassLoader.equals(taskClassLoader))
                throw new Exception("Class loader of servlet " + servletClassLoader + " should not be propagated to managed task " + taskClassLoader);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Verify that a managed task runs without access to the application component namespace.
     *
     * @param execSvcJNDIName the executor service to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testNoJEEMetadataContext(String execSvcJNDIName, PrintWriter out) throws Exception {

        final BlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        final Runnable javaCompLookup = new Runnable() {
            @Override
            public void run() {
                System.out.println("running task");
                try {
                    results.add(new InitialContext().lookup("java:comp/env/entry1"));
                } catch (Throwable x) {
                    results.add(x);
                }
            }
        };

        // Lookup from current thread should work
        javaCompLookup.run();
        Object result = results.remove();
        if (result instanceof Throwable)
            throw new ExecutionException((Throwable) result);
        if (!"value1".equals(result))
            throw new Exception("Unexpected value for java:comp/env/entry1 from current thread: " + result);

        // Lookup from managed executor service thread (without jeeMetadataContext) should fail
        ExecutorService execSvc = (ExecutorService) new InitialContext().lookup(execSvcJNDIName);
        Future<BlockingQueue<Object>> future = execSvc.submit(javaCompLookup, results);
        try {
            result = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS).remove();
        } finally {
            future.cancel(true);
        }

        if (result instanceof NamingException)
            ; // expected
        else if (result instanceof Exception)
            throw (Exception) result;
        else if (result instanceof Throwable)
            throw new ExecutionException((Throwable) result);
        else
            throw new Exception("jeeMetadataContext should not be available from managedExecutorService " + execSvcJNDIName + " thread. Value is: " + result);
    }

    /**
     * Verify that a managed task submitted from an EJB runs without access to the application component namespace.
     *
     * @param unused ignored
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testNoJEEMetadataContextFromEJB(String unused, PrintWriter out) throws Exception {
        EEConcurrencyUtilsStatelessBean ejb = (EEConcurrencyUtilsStatelessBean) new InitialContext()
                        .lookup("java:global/concurrent/EEConcurrencyUtilsStatelessBean!" + EEConcurrencyUtilsStatelessBean.class.getCanonicalName());
        ejb.testNoJEEMetadataContextExecSvc1();
    }

    /**
     * Verify that a managed task does not run in the transaction of the thread from which it was submitted.
     *
     * @param execSvcJNDIName the executor service to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testNoTransactionContext(String execSvcJNDIName, PrintWriter out) throws Exception {

        ExecutorService execSvc = (ExecutorService) new InitialContext().lookup(execSvcJNDIName);
        Future<Transaction> future = null;

        org.osgi.framework.BundleContext bundleContext = org.osgi.framework.FrameworkUtil.getBundle(getClass().getClassLoader().getClass()).getBundleContext();
        org.osgi.framework.ServiceReference<TransactionManager> tranMgrRef = bundleContext.getServiceReference(TransactionManager.class);
        List<org.osgi.framework.ServiceReference<?>> refs = new LinkedList<org.osgi.framework.ServiceReference<?>>();
        tran.begin();
        try {
            final TransactionManager tranMgr = bundleContext.getService(tranMgrRef);
            refs.add(tranMgrRef);

            Transaction tran1 = tranMgr.getTransaction();
            System.out.println("before task: " + tran1);

            future = execSvc.submit(new Callable<Transaction>() {
                @Override
                public Transaction call() throws SystemException {
                    return tranMgr.getTransaction();
                }
            });

            Transaction tran2 = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            System.out.println("during task: " + tran2);
            if (tran2 != null)
                throw new Exception("Managed task without transactionContext configured should run in an LTC or no transaction at all, not " + tran2
                                    + ". Original transaction was " + tran1);

            Transaction tran3 = tranMgr.getTransaction();
            System.out.println("after task: " + tran3);
            if (!tran1.equals(tran3))
                throw new Exception("Should have same transaction " + tran1 + " on thread after running managed task without transactionContext configured. Instead: " + tran3);
        } finally {
            if (future != null)
                future.cancel(true);
            for (org.osgi.framework.ServiceReference<?> ref : refs)
                bundleContext.ungetService(ref);
            tran.commit();
        }
    }

    /**
     * Submit 1 task and verify it completes successfully.
     */
    public void testTaskSuccessful(String execSvcJNDIName, PrintWriter out) throws Exception {
        ExecutorService executor = InitialContext.doLookup(execSvcJNDIName);
        Future<Integer> future = executor.submit(new IncrementTask(null, null, null, null));
        assertEquals(Integer.valueOf(1), future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
    }

    /**
     * Submit 2 tasks, where the first task waits for the second to start. Task Futures are saved for later use.
     */
    public void testTask1BlockedByTask2(String execSvcJNDIName, PrintWriter out) throws Exception {
        ExecutorService executor = InitialContext.doLookup(execSvcJNDIName);
        CountDownLatch task2StartedLatch = new CountDownLatch(1);
        Future<Integer> future1 = executor.submit(new IncrementTask(null, null, null, task2StartedLatch));
        Future<Integer> future2 = executor.submit(new IncrementTask(null, null, task2StartedLatch, null));
        futures.put("testTask1BlockedByTask2-future1-" + execSvcJNDIName, future1);
        futures.put("testTask1BlockedByTask2-future2-" + execSvcJNDIName, future2);
    }

    /**
     * Verify that futures previously submitted are completed now.
     */
    public void testTask1BlockedByTask2Canceled(String execSvcJNDIName, PrintWriter out) throws Exception {
        @SuppressWarnings("unchecked")
        Future<Integer> future1 = (Future<Integer>) futures.remove("testTask1BlockedByTask2-future1-" + execSvcJNDIName);
        @SuppressWarnings("unchecked")
        Future<Integer> future2 = (Future<Integer>) futures.remove("testTask1BlockedByTask2-future2-" + execSvcJNDIName);

        assertTrue(future1.isDone());
        assertTrue(future2.isDone());
        assertTrue(future1.isCancelled());
        assertTrue(future2.isCancelled());

        try {
            fail("Task 1 should have been canceled. Instead, result is: " + future1.get(0, TimeUnit.SECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("Task 2 should have been canceled. Instead, result is:  " + future2.get(0, TimeUnit.SECONDS));
        } catch (CancellationException x) {
        } // pass
    }

    /**
     * Verify that futures previously submitted are completed now.
     */
    public void testTask1BlockedByTask2Completed(String execSvcJNDIName, PrintWriter out) throws Exception {
        @SuppressWarnings("unchecked")
        Future<Integer> future1 = (Future<Integer>) futures.remove("testTask1BlockedByTask2-future1-" + execSvcJNDIName);
        @SuppressWarnings("unchecked")
        Future<Integer> future2 = (Future<Integer>) futures.remove("testTask1BlockedByTask2-future2-" + execSvcJNDIName);

        assertEquals(Integer.valueOf(1), future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Integer.valueOf(1), future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future1.isDone());
        assertTrue(future2.isDone());
        assertFalse(future1.isCancelled());
        assertFalse(future2.isCancelled());
    }

    /**
     * Submit 2 long running tasks, where the first task waits for the second to start. Task Futures are saved for later use.
     */
    public void testTask1BlockedByTask2LongRunning(String execSvcJNDIName, PrintWriter out) throws Exception {
        ExecutorService executor = InitialContext.doLookup(execSvcJNDIName);
        CountDownLatch task2StartedLatch = new CountDownLatch(1);
        IncrementTask task1 = new IncrementTask(null, null, null, task2StartedLatch);
        IncrementTask task2 = new IncrementTask(null, null, task2StartedLatch, null);
        task1.getExecutionProperties().put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());
        task2.getExecutionProperties().put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());
        Future<Integer> future1 = executor.submit(task1);
        Future<Integer> future2 = executor.submit(task2);
        futures.put("testTask1BlockedByTask2LongRunning-future1-" + execSvcJNDIName, future1);
        futures.put("testTask1BlockedByTask2LongRunning-future2-" + execSvcJNDIName, future2);
    }

    /**
     * Verify that futures for long running tasks previously submitted are canceled now.
     */
    public void testTask1BlockedByTask2LongRunningCanceled(String execSvcJNDIName, PrintWriter out) throws Exception {
        @SuppressWarnings("unchecked")
        Future<Integer> future1 = (Future<Integer>) futures.remove("testTask1BlockedByTask2LongRunning-future1-" + execSvcJNDIName);
        @SuppressWarnings("unchecked")
        Future<Integer> future2 = (Future<Integer>) futures.remove("testTask1BlockedByTask2LongRunning-future2-" + execSvcJNDIName);

        assertTrue(future1.isDone());
        assertTrue(future2.isDone());
        assertTrue(future1.isCancelled());
        assertTrue(future2.isCancelled());

        try {
            fail("Long running task 1 should have been canceled. Instead, result is:  " + future1.get(0, TimeUnit.SECONDS));
        } catch (CancellationException x) {
        } // pass

        try {
            fail("Long running task 2 should have been canceled. Instead, result is:  " + future2.get(0, TimeUnit.SECONDS));
        } catch (CancellationException x) {
        } // pass
    }

    /**
     * Verify that futures for long running tasks previously submitted are completed now.
     */
    public void testTask1BlockedByTask2LongRunningCompleted(String execSvcJNDIName, PrintWriter out) throws Exception {
        @SuppressWarnings("unchecked")
        Future<Integer> future1 = (Future<Integer>) futures.remove("testTask1BlockedByTask2LongRunning-future1-" + execSvcJNDIName);
        @SuppressWarnings("unchecked")
        Future<Integer> future2 = (Future<Integer>) futures.remove("testTask1BlockedByTask2LongRunning-future2-" + execSvcJNDIName);

        assertEquals(Integer.valueOf(1), future1.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertEquals(Integer.valueOf(1), future2.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
        assertTrue(future1.isDone());
        assertTrue(future2.isDone());
        assertFalse(future1.isCancelled());
        assertFalse(future2.isCancelled());
    }

    /**
     * Verify that a managed thread factory uses a thread group with max priority = 4
     *
     * @param threadFactoryJNDIName the thread factory to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testThreadGroupMaxPriority4(String threadFactoryJNDIName, PrintWriter out) throws Exception {

        ThreadFactory threadFactory = (ThreadFactory) new InitialContext().lookup(threadFactoryJNDIName);
        ThreadInfoRunnable threadInfoRunnable = new ThreadInfoRunnable();
        Thread thread = threadFactory.newThread(threadInfoRunnable);
        thread.start();
        ThreadGroup group = threadInfoRunnable.threadGroupQ.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        int maxPriority = group.getMaxPriority();

        if (maxPriority != 4)
            throw new Exception("Unexpected max priority: " + maxPriority);
    }

    /**
     * Verify that a managed thread factory uses a thread group with max priority = 6
     *
     * @param threadFactoryJNDIName the thread factory to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testThreadGroupMaxPriority6(String threadFactoryJNDIName, PrintWriter out) throws Exception {

        ThreadFactory threadFactory = (ThreadFactory) new InitialContext().lookup(threadFactoryJNDIName);
        Thread thread = threadFactory.newThread(new ThreadInfoRunnable());
        ThreadGroup group = thread.getThreadGroup();
        int maxPriority = group.getMaxPriority();

        if (maxPriority != 6)
            throw new Exception("Unexpected max priority: " + maxPriority);
    }

    /**
     * Verify that a managed thread factory creates daemon threads
     *
     * @param threadFactoryJNDIName the thread factory to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testThreadIsDaemon(String threadFactoryJNDIName, PrintWriter out) throws Exception {

        ThreadFactory threadFactory = (ThreadFactory) new InitialContext().lookup(threadFactoryJNDIName);
        ThreadInfoRunnable threadInfoRunnable = new ThreadInfoRunnable();
        Thread thread = threadFactory.newThread(threadInfoRunnable);
        thread.start();
        Boolean isDaemon = threadInfoRunnable.threadIsDaemonQ.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        if (!Boolean.TRUE.equals(isDaemon))
            throw new Exception("Managed thread should be a daemon thread. Instead: " + isDaemon);
    }

    /**
     * Verify that a managed thread factory does not create daemon threads
     *
     * @param threadFactoryJNDIName the thread factory to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testThreadIsNotDaemon(String threadFactoryJNDIName, PrintWriter out) throws Exception {

        ThreadFactory threadFactory = (ThreadFactory) new InitialContext().lookup(threadFactoryJNDIName);
        Thread thread = threadFactory.newThread(new ThreadInfoRunnable());
        boolean isDaemon = thread.isDaemon();

        if (isDaemon)
            throw new Exception("Managed thread should not be a daemon thread");
    }

    /**
     * Verify that a managed thread factory creates threads with priority = 3
     *
     * @param threadFactoryJNDIName the thread factory to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testThreadPriority3(String threadFactoryJNDIName, PrintWriter out) throws Exception {

        ThreadFactory threadFactory = (ThreadFactory) new InitialContext().lookup(threadFactoryJNDIName);
        Thread thread = threadFactory.newThread(new ThreadInfoRunnable());
        int priority = thread.getPriority();

        if (priority != 3)
            throw new Exception("Unexpected thread priority: " + priority);
    }

    /**
     * Verify that a managed thread factory creates threads with priority = 5
     *
     * @param threadFactoryJNDIName the thread factory to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testThreadPriority5(String threadFactoryJNDIName, PrintWriter out) throws Exception {

        ThreadFactory threadFactory = (ThreadFactory) new InitialContext().lookup(threadFactoryJNDIName);
        ThreadInfoRunnable threadInfoRunnable = new ThreadInfoRunnable();
        Thread thread = threadFactory.newThread(threadInfoRunnable);
        thread.start();
        Integer priority = threadInfoRunnable.threadPriorityQ.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        if (!Integer.valueOf(5).equals(priority))
            throw new Exception("Unexpected thread priority: " + priority);
    }

    /**
     * Verify that a managed thread factory creates threads with priority = 8
     *
     * @param threadFactoryJNDIName the thread factory to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testThreadPriority8(String threadFactoryJNDIName, PrintWriter out) throws Exception {

        ThreadFactory threadFactory = (ThreadFactory) new InitialContext().lookup(threadFactoryJNDIName);
        ThreadInfoRunnable threadInfoRunnable = new ThreadInfoRunnable();
        Thread thread = threadFactory.newThread(threadInfoRunnable);
        thread.start();
        Integer priority = threadInfoRunnable.threadPriorityQ.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        if (!Integer.valueOf(8).equals(priority))
            throw new Exception("Unexpected thread priority: " + priority);
    }

    /**
     * Verify that a managed task runs in an LTC.
     *
     * @param execSvcJNDIName the executor service to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testTransactionContext(String execSvcJNDIName, PrintWriter out) throws Exception {

        ExecutorService execSvc = (ExecutorService) new InitialContext().lookup(execSvcJNDIName);
        Future<Transaction> future = null;

        org.osgi.framework.BundleContext bundleContext = org.osgi.framework.FrameworkUtil.getBundle(getClass().getClassLoader().getClass()).getBundleContext();
        org.osgi.framework.ServiceReference<TransactionManager> tranMgrRef = bundleContext.getServiceReference(TransactionManager.class);
        List<org.osgi.framework.ServiceReference<?>> refs = new LinkedList<org.osgi.framework.ServiceReference<?>>();
        tran.begin();
        try {
            final TransactionManager tranMgr = bundleContext.getService(tranMgrRef);
            refs.add(tranMgrRef);

            Transaction tran1 = tranMgr.getTransaction();
            System.out.println("before task: " + tran1);

            future = execSvc.submit(new Callable<Transaction>() {
                @Override
                public Transaction call() throws SystemException {
                    return tranMgr.getTransaction();
                }
            });

            Transaction tran2 = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            System.out.println("during task: " + tran2);
            if (tran2 != null)
                throw new Exception("Managed task configured with transactionContext should run in an LTC, not " + tran2 + ". Original transaction was " + tran1);

            Transaction tran3 = tranMgr.getTransaction();
            System.out.println("after task: " + tran3);
            if (!tran1.equals(tran3))
                throw new Exception("Should have same transaction " + tran1 + " on thread after running managed task with transactionContext. Instead: " + tran3);
        } finally {
            if (future != null)
                future.cancel(true);
            for (org.osgi.framework.ServiceReference<?> ref : refs)
                bundleContext.ungetService(ref);
            tran.commit();
        }
    }
}
