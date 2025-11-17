/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package concurrent.cdi4.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOError;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedExecutors;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.TransactionalException;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.junit.Test;

import componenttest.app.FATServlet;
import concurrent.cdi4.web.ResourcesProducer.SampleExecutor;

@SuppressWarnings("serial")
@WebServlet("/*")
public class ConcurrentCDI4Servlet extends FATServlet {

    /**
     * Maximum number of milliseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_MS = TimeUnit.MINUTES.toMillis(2);

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MILLISECONDS.toNanos(TIMEOUT_MS);

    @Inject
    private ApplicationScopedBean appScopedBean;

    @Inject
    private DependentScopedBean dependentScopedBean;

    @Inject
    private TransactionalBean bean;

    @Resource(name = "java:comp/env/concurrent/executorRef")
    private ManagedExecutorService executor;

    @Resource(name = "java:module/env/concurrent/timeoutExecutorRef",
              lookup = "concurrent/timeoutExecutor")
    private ManagedExecutorService executorWithStartTimeout;

    @Inject
    private MyManagedBean managedBean;

    @Inject
    private RequestScopedBean requestScopedBean;

    @Inject
    private SessionScopedBean sessionScopedBean;

    @Inject
    private SingletonScopedBean singletonScopedBean;

    @Inject
    private SubmitterBean submitterBean;

    @Inject
    private TaskBean taskBean;

    private TransactionManager tm;

    @Resource
    private UserTransaction tran;

    @Inject
    private TransactionScopedBean transactionScopedBean;

    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    private TransactionSynchronizationRegistry tranSyncRegistry;

    private ExecutorService unmanagedThreads;

    @Inject
    private ManagedExecutorService injectedExec; // produced by UnqualifiedResourcesProducer.exec field

    @Inject
    @SampleExecutor
    private ManagedExecutorService sampleExec; // produced by ResourcesProducer.exec field

    @Override
    public void after() {
        unmanagedThreads.shutdownNow();
    }

    @Override
    public void before() {
        unmanagedThreads = Executors.newFixedThreadPool(5);
    }

    /**
     * Initialize the transaction service (including recovery logs) so it doesn't slow down our tests and cause timeouts.
     */
    public void initTransactionService() throws Exception {
        // reflectively invoke: tm = TransactionManagerFactory.getTransactionManager();
        Class<?> TransactionManagerFactory = Class.forName("com.ibm.tx.jta.TransactionManagerFactory");
        tm = (TransactionManager) TransactionManagerFactory.getMethod("getTransactionManager").invoke(null);
        tran.begin();
        tran.commit();
    }

    /**
     * Asynchronous method can look up a Jakarta EE default resource.
     */
    @Test
    public void testAppScopedBeanAsyncMethodLooksUpDefaultResource() throws Exception {
        ManagedExecutorService defaultExecutor = InitialContext.doLookup("java:comp/DefaultManagedExecutorService");
        CompletableFuture<?> future = appScopedBean.lookup("java:comp/DefaultManagedExecutorService");
        assertEquals(defaultExecutor.toString(), future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS).toString());
    }

    /**
     * Asynchronous method can look up a resource in the application component's name space.
     */
    @Test
    public void testAppScopedBeanAsyncMethodLooksUpResourceInJavaComp() throws Exception {
        CompletableFuture<?> future = appScopedBean.lookup("java:comp/env/concurrent/executorRef");
        assertEquals(executor.toString(), future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS).toString());
    }

    /**
     * Asynchronous method can look up UserTransaction.
     */
    @Test
    public void testAppScopedBeanAsyncMethodLooksUpUserTransaction() throws Exception {
        CompletableFuture<?> future = appScopedBean.lookup("java:comp/UserTransaction");
        assertEquals(tran, future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * Asynchronous method fails by raising CompletionException.
     */
    @Test
    public void testAppScopedBeanAsyncMethodRaisesCompletionException() throws Exception {
        CompletableFuture<?> future = appScopedBean.lookup("concurrent/not-found"); // intentionally fails
        try {
            Object result = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("Nothing should be found under the JNDI name, instead: " + result);
        } catch (ExecutionException x) {
            if (x.getCause() instanceof NameNotFoundException)
                ; // pass
            else
                throw x;
        }

        try {
            Object result = future.join();
            fail("Nothing should be found under the JNDI name, instead: " + result);
        } catch (CompletionException x) {
            if (x.getCause() instanceof NameNotFoundException)
                ; // pass
            else
                throw x;
        }
    }

    /**
     * Asynchronous method fails by raising a RuntimeException subclass.
     */
    @Test
    public void testAppScopedBeanAsyncMethodRaisesError() throws Exception {
        CompletableFuture<?> future = appScopedBean.forceError(); // intentionally fails
        try {
            Object result = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("Expecting an error, instead: " + result);
        } catch (ExecutionException x) {
            if (x.getCause() instanceof Error)
                ; // pass
            else
                throw x;
        }

        try {
            Object result = future.join();
            fail("Expecting an error, instead: " + result);
        } catch (CompletionException x) {
            if (x.getCause() instanceof Error)
                ; // pass
            else
                throw x;
        }
    }

    /**
     * Asynchronous method fails by raising a RuntimeException subclass.
     */
    @Test
    public void testAppScopedBeanAsyncMethodRaisesRuntimeException() throws Exception {
        CompletableFuture<?> future = appScopedBean.lookup(null); // intentionally fails
        try {
            Object result = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("Nothing should be found when there is no JNDI name, instead: " + result);
        } catch (ExecutionException x) {
            if (x.getCause() instanceof NullPointerException)
                ; // pass
            else
                throw x;
        }

        try {
            Object result = future.join();
            fail("Nothing should be found where there is no JNDI name, instead: " + result);
        } catch (CompletionException x) {
            if (x.getCause() instanceof NullPointerException)
                ; // pass
            else
                throw x;
        }
    }

    /**
     * A managed bean that is ApplicationScoped can have an asynchronous method
     * that returns CompletableFuture and runs asynchronously to the calling thread.
     */
    @Test
    public void testAppScopedBeanAsyncMethodReturnsCompletableFuture() throws Exception {
        String curThreadName = Thread.currentThread().getName();
        appScopedBean.setCharacter('=');
        String result = appScopedBean.appendThreadNameFuture("NameOfThread").get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertTrue(result, result.startsWith("NameOfThread="));
        assertFalse(result, result.equals("NameOfThread=" + curThreadName));
        assertTrue(result, result.contains("Default Executor-thread-")); // runs on a Liberty thread vs some other thread pool
    }

    /**
     * An asynchronous method that returns CompletionStage will run asynchronously to the calling thread.
     */
    @Test
    public void testAppScopedBeanAsyncMethodReturnsCompletionStage() throws Exception {
        String curThreadName = Thread.currentThread().getName();
        appScopedBean.setCharacter(':');
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        appScopedBean.appendThreadNameStage("ThreadName").whenComplete((result, failure) -> {
            results.add(failure == null ? result : failure);
        });
        Object result = results.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (result instanceof Throwable)
            throw new AssertionError(result);
        String str = (String) result;
        assertTrue(str, str.startsWith("ThreadName:"));
        assertFalse(str, str.equals("ThreadName:" + curThreadName));
        assertTrue(str, str.contains("Default Executor-thread-")); // runs on a Liberty thread vs some other thread pool
    }

    /**
     * A managed bean that is ApplicationScoped can have some asynchronous methods,
     * but other methods do not run asynchronously to the calling thread.
     */
    @Test
    public void testAppScopedBeanMethodWithoutAnnotationIsNotAsync() throws Exception {
        String curThreadName = Thread.currentThread().getName();
        AtomicReference<String> threadNameRef = new AtomicReference<String>();
        appScopedBean.notAsync(threadNameRef);
        assertEquals(curThreadName, threadNameRef.get());
    }

    /**
     * The Asynchronous annotation can be placed on a method of a superclass of a CDI managed bean.
     */
    @Test
    public void testAsyncOnSuperclassMethod() throws Exception {
        CompletableFuture<Long> future = requestScopedBean.getThreadId();

        // Get the result in a way that avoids being able to run inline
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && !future.isDone();)
            TimeUnit.MILLISECONDS.sleep(200);

        long asyncMethodThreadId = future.getNow(-1l);
        long curThreadId = Thread.currentThread().getId();

        assertTrue("Asynchronous method must not run on current thread " + curThreadId,
                   curThreadId != asyncMethodThreadId);
    }

    /**
     * When an asynchronous method is also annotated with Transactional(MANDATORY)
     * it must be rejected because the transaction cannot be established on the async
     * method thread in parallel to the caller.
     */
    @Test
    public void testAsyncTxMandatory() throws Exception {
        tran.begin();
        try {
            Object txKeyAsync = bean.runAsyncAsMandatory().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("TxType.MANDATORY permitted on asynchronous method: " + txKeyAsync);
        } catch (UnsupportedOperationException x) {
            String message = x.getMessage();
            if (message == null
                || !message.contains("Async")
                || !message.contains("Transactional")
                || !message.contains("MANDATORY"))
                throw x;
        } finally {
            tran.rollback();
        }
    }

    /**
     * When an asynchronous method is also annotated with Transactional(NEVER)
     * it must be rejected.
     */
    @Test
    public void testAsyncTxNever() throws Exception {
        tran.begin();
        try {
            Object txKeyAsync = bean.runAsyncAsNever().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("TxType.NEVER permitted on asynchronous method: " + txKeyAsync);
        } catch (UnsupportedOperationException x) {
            String message = x.getMessage();
            if (message == null
                || !message.contains("Async")
                || !message.contains("Transactional")
                || !message.contains("NEVER"))
                throw x;
        } finally {
            tran.rollback();
        }
    }

    /**
     * When an asynchronous method is also annotated with Transactional(NOT_SUPPORTED),
     * the Transactional interceptor must be applied second, such that the method runs
     * under no transaction.
     */
    @Test
    public void testAsyncTxNotSupported() throws Exception {
        tran.begin();
        try {
            Object txKey = tranSyncRegistry.getTransactionKey();
            assertNotNull(txKey);
            Object txKeyAsync = bean.runAsyncAsNotSupported().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertNull(txKeyAsync);
        } finally {
            tran.commit();
        }
    }

    /**
     * When an asynchronous method is also annotated with Transactional(REQUIRED)
     * it must be rejected because the transaction cannot be established on the async
     * method thread in parallel to the caller.
     */
    @Test
    public void testAsyncTxRequired() throws Exception {
        tran.begin();
        try {
            Object txKeyAsync = bean.runAsyncAsRequired().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("TxType.REQUIRED permitted on asynchronous method: " + txKeyAsync);
        } catch (UnsupportedOperationException x) {
            String message = x.getMessage();
            if (message == null
                || !message.contains("Async")
                || !message.contains("Transactional")
                || !message.contains("REQUIRED"))
                throw x;
        } finally {
            tran.rollback();
        }
    }

    /**
     * When an asynchronous method is also annotated with Transactional(REQUIRES_NEW),
     * the Transactional intercept must be applied second, such that the method runs
     * under a new transaction.
     */
    @Test
    public void testAsyncTxRequiresNew() throws Exception {
        tran.begin();
        try {
            Object txKey = tranSyncRegistry.getTransactionKey();
            Object txKeyAsync = bean.runAsyncAsRequiresNew().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertNotNull(txKeyAsync);
            assertNotSame(txKey, txKeyAsync);
        } finally {
            tran.commit();
        }
    }

    /**
     * When an asynchronous method is also annotated with Transactional(SUPPORTS)
     * it must be rejected because the transaction cannot be established on the async
     * method thread in parallel to the caller.
     */
    @Test
    public void testAsyncTxSupports() throws Exception {
        tran.begin();
        try {
            Object txKeyAsync = bean.runAsyncAsSupports().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("TxType.SUPPORTS permitted on asynchronous method: " + txKeyAsync);
        } catch (UnsupportedOperationException x) {
            String message = x.getMessage();
            if (message == null
                || !message.contains("Async")
                || !message.contains("Transactional")
                || !message.contains("SUPPORTS"))
                throw x;
        } finally {
            tran.rollback();
        }
    }

    /**
     * Inject a ManagedExecutorService that has no qualifier.
     */
    @Test
    public void testInjectedManagedExecutorService() throws Exception {
        // via @Inject annotation:
        assertNotNull(injectedExec);
        assertEquals(true, injectedExec.completedFuture(44).isDone());

        // programmatically:
        ManagedExecutorService instance = CDI.current().select(ManagedExecutorService.class).get();
        assertNotNull(instance);
        assertEquals(Character.valueOf('i'), instance.completedFuture('i').get());
    }

    /**
     * Inject a ManagedExecutorService that has a qualifier.
     */
    @Test
    public void testInjectedManagedExecutorServiceWithQualifier() throws Exception {
        // via @Inject annotation:
        assertNotNull(sampleExec);
        assertEquals(true, sampleExec.completedFuture(50).isDone());

        // programmatically:
        ManagedExecutorService instance = CDI.current().select(ManagedExecutorService.class, SampleExecutor.Literal.instance()).get();
        assertNotNull(instance);
        assertEquals("hello", instance.completedFuture("hello").get());
    }

    /**
     * Interrupt asynchronous and inline asynchronous methods.
     */
    @Test
    public void testInterruptAsyncMethod() throws Exception {
        // use up first (of 2) maxAsync,
        Exchanger<Object> method1running = new Exchanger<Object>();
        CountDownLatch method1blocker = new CountDownLatch(1);
        CompletableFuture<Boolean> method1future = managedBean.exchangeAndAwait(method1running, TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Thread method1thread = (Thread) method1running.exchange(method1blocker, TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // prepare for inline execution,
        Exchanger<Object> method3running = new Exchanger<Object>();
        CountDownLatch method3blocker = new CountDownLatch(1);

        // use up second (of 2) maxAsync to interrupt inline execution
        Future<String> task2future = sampleExec.submit(() -> {
            Thread servletThread = (Thread) method3running.exchange(method3blocker, TIMEOUT_MS, TimeUnit.MILLISECONDS);
            servletThread.interrupt();
            return "task2successful";
        });

        CompletableFuture<Boolean> method3future = managedBean.exchangeAndAwait(method3running, TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // run inline
        try {
            Boolean result = method3future.join();
            fail("Expecting inline execution to be interrupted. Instead: " + result);
        } catch (CompletionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        // also interrupt the first async method
        assertFalse(method1future.isDone());
        method1thread.interrupt();
        try {
            Boolean result = method1future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            fail("Expecting async execution to be interrupted. Instead: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        assertEquals("task2successful", task2future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * Verify that when a task runs inline, the transaction and application context are cleared
     * from the thread when the managed executor's ContextServiceDefinition lets those context types
     * default to being cleared.
     */
    @Test
    public void testInvokeInlineWithClearedTransactionAndAppContext() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:global/concurrent/allcontextclearedexecutor");
        tran.begin();
        try {
            Object txKey = tranSyncRegistry.getTransactionKey();
            Object result = executor.invokeAny(Collections.singleton(() -> {
                try {
                    Object unexpected = InitialContext.doLookup("java:global/concurrent/allcontextclearedexecutor");
                    throw new AssertionError("Must not be able to look up " + unexpected +
                                             " because Application context must be cleared per the ContextServiceDefinition");
                } catch (NamingException x) {
                    // expected
                }
                return tranSyncRegistry.getTransactionKey();
            }));
            assertNull(result);
            assertEquals(txKey, tranSyncRegistry.getTransactionKey());
            assertEquals(Status.STATUS_ACTIVE, tran.getStatus());
        } finally {
            tran.commit();
        }
    }

    /**
     * Verify that when a task runs inline, the transaction remains on the thread
     * when the managed executor's ContextServiceDefinition specifies that transaction
     * context remain unchanged.
     */
    @Test
    public void testInvokeInlineWithinSameTransaction() throws Exception {
        ManagedScheduledExecutorService executor = InitialContext.doLookup("java:comp/concurrent/appContextExecutor");
        long servletThreadId = Thread.currentThread().getId();
        Callable<Boolean> task = () -> {
            boolean inline = Thread.currentThread().getId() == servletThreadId;
            // Transaction must remain on thread when running inline:
            if (inline)
                tran.commit();
            // Application context is propagated:
            assertNotNull(InitialContext.doLookup("java:comp/concurrent/appContextExecutor"));
            return inline;
        };
        List<Future<Boolean>> futures;
        tran.begin();
        try {
            futures = executor.invokeAll(Collections.singleton(task));
            assertEquals(futures.toString(), 1, futures.size());
            boolean executedInline = futures.get(0).get(100, TimeUnit.MILLISECONDS);
            if (executedInline)
                assertEquals(Status.STATUS_NO_TRANSACTION, tran.getStatus());
        } finally {
            if (tran.getStatus() != Status.STATUS_NO_TRANSACTION)
                tran.rollback();
        }
    }

    /**
     * Verify that when a task runs inline, the application context and transaction remain
     * on the thread when the managed executor's ContextServiceDefinition specifies that
     * application and transaction context remain unchanged.
     */
    @Test
    public void testInvokeInlineWithinSameTransactionAndAppContext() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:comp/concurrent/remainingcontextunchangedexecutor");
        long servletThreadId = Thread.currentThread().getId();
        Callable<Boolean> task = () -> {
            boolean inline = Thread.currentThread().getId() == servletThreadId;
            if (inline) {
                // Transaction must remain on thread when running inline:
                tran.rollback();
                // Application context must also remain on the thread:
                assertNotNull(InitialContext.doLookup("java:comp/concurrent/remainingcontextunchangedexecutor"));
            }
            return inline;
        };
        tran.begin();
        try {
            Boolean executedInline = executor.invokeAny(Collections.singleton(task));
            if (executedInline)
                assertEquals(Status.STATUS_NO_TRANSACTION, tran.getStatus());
        } finally {
            if (tran.getStatus() != Status.STATUS_NO_TRANSACTION)
                tran.rollback();
        }
    }

    /**
     * Verify that when a task runs inline, the transaction remains on the thread
     * and the application context is cleared when the managed executor's ContextServiceDefinition
     * specifies that transaction context remain unchanged and the application context is cleared.
     */
    @Test
    public void testInvokeInlineWithinSameTransactionWithAppContextCleared() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:comp/concurrent/appcontextclearedexecutor");
        long servletThreadId = Thread.currentThread().getId();
        Callable<Boolean> task = () -> {
            boolean inline = Thread.currentThread().getId() == servletThreadId;
            // Transaction must remain on thread when running inline:
            if (inline)
                tran.setRollbackOnly();
            // Application context must not be propagated:
            try {
                Object unexpected = InitialContext.doLookup("java:comp/concurrent/appcontextclearedexecutor");
                throw new AssertionError("Must not be able to look up " + unexpected +
                                         " because Application context must be cleared per the ContextServiceDefinition");
            } catch (NamingException x) {
                // expected
            }
            return inline;
        };
        tran.begin();
        try {
            Boolean executedInline = executor.invokeAny(Collections.singleton(task));
            if (executedInline)
                assertEquals(Status.STATUS_MARKED_ROLLBACK, tran.getStatus());
        } finally {
            tran.rollback();
        }
    }

    /**
     * Verify that when a task runs inline, the application context remains on the thread
     * but the transaction does not when the managed executor's ContextServiceDefinition
     * specifies that both application and transaction context remain unchanged, but an
     * execution property overrides the transaction context to suspend it.
     */
    @Test
    public void testInvokeInlineWithSameAppContext_TranSuspendedByExecutionProperty() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:comp/concurrent/remainingcontextunchangedexecutor");

        Map<String, String> execPropsSuspendTx = new TreeMap<String, String>();
        execPropsSuspendTx.put(ManagedTask.IDENTITY_NAME, "testInvokeInlineWithSameAppContext_TranSuspendedByExecutionProperty");
        execPropsSuspendTx.put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);

        long servletThreadId = Thread.currentThread().getId();
        tran.begin();
        try {
            Callable<Boolean> task = () -> {
                // Transaction must be cleared per execution property:
                assertEquals(Status.STATUS_NO_TRANSACTION, tran.getStatus());

                boolean inline = Thread.currentThread().getId() == servletThreadId;
                if (inline) {
                    // Application context must remain on the thread:
                    assertNotNull(InitialContext.doLookup("java:comp/concurrent/remainingcontextunchangedexecutor"));
                }
                return inline;
            };

            task = ManagedExecutors.managedTask(task, execPropsSuspendTx, null);

            List<Future<Boolean>> futures = executor.invokeAll(Collections.singleton(task));

            // cause any errors that occur when running the task to be raised and fail the test
            assertNotNull(futures.get(0).get());

            assertEquals(Status.STATUS_ACTIVE, tran.getStatus());
        } finally {
            tran.rollback();
        }
    }

    /**
     * From the bean, submit a concurrent task.
     */
    @Test
    public void testBeanSubmitsManagedTask() throws Exception {
        Future<?> future = submitterBean.submit(new Callable<Object>() {
            @Override
            public Object call() {
                try {
                    InitialContext initialContext = new InitialContext();

                    UserTransaction tran = (UserTransaction) initialContext.lookup("java:comp/UserTransaction");
                    tran.begin();
                    tran.commit();

                    return initialContext.lookup("java:comp/env/concurrent/executorRef");
                } catch (RuntimeException x) {
                    throw x;
                } catch (Exception x) {
                    throw new RuntimeException(x);
                }
            }
        });
        try {
            Object result = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (result == null || !(result instanceof ExecutorService) || result instanceof ScheduledExecutorService)
                throw new RuntimeException("Unexpected resource ref result " + result);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute NEVER
     */
    @Test
    public void testBeanSubmitsManagedTaskThatInvokesTxNever() throws Exception {
        Future<?> future = submitterBean.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object beanTxKey = bean.runAsNever();
                if (beanTxKey != null)
                    throw new Exception("TX_NEVER should not run in a transaction: " + beanTxKey);

                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    bean.runAsNever();
                    throw new Exception("Should not be able to invoke TX_NEVER method when there is a transaction on the thread");
                } catch (TransactionalException x) {
                    if (x.getMessage() == null || !x.getMessage().contains("TxType.NEVER"))
                        throw x;
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute REQUIRES_NEW
     */
    @Test
    public void testBeanSubmitsManagedTaskThatInvokesTxRequiresNew() throws Exception {
        Future<?> future = submitterBean.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                assertNull(tranSyncRegistry.getTransactionKey());
                Object beanTxKey = bean.runAsRequiresNew();
                assertNotNull(beanTxKey);

                tran.begin();
                try {
                    Object txKey = tranSyncRegistry.getTransactionKey();
                    beanTxKey = bean.runAsRequiresNew();
                    assertNotNull(beanTxKey);
                    assertNotSame(txKey, beanTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute SUPPORTS
     */
    @Test
    public void testBeanSubmitsManagedTaskThatInvokesTxSupports() throws Exception {
        Future<?> future = submitterBean.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object beanTxKey = bean.runAsSupports();
                if (beanTxKey != null)
                    throw new Exception("Bean method with TX_SUPPORTS should not run in a transaction " + beanTxKey + " when there is none on the invoking thread.");

                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    beanTxKey = bean.runAsSupports();
                    Object taskTxKey = tranSyncRegistry.getTransactionKey();
                    if (!taskTxKey.equals(beanTxKey))
                        throw new Exception("Bean with TX_SUPPORTS should run in the transaction of the invoker " + taskTxKey + ", not " + beanTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Verify that an asynchronous method can run inline upon CompletableFuture.join and untimed get
     * when maxAsync prevents running on a thread from the Liberty global thread pool.
     */
    @Test
    public void testInlineAsyncMethod() throws Exception {
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch blocker = new CountDownLatch(1);

        Callable<Boolean> wait = () -> {
            started.countDown();
            return blocker.await(TIMEOUT_MS * 2, TimeUnit.MILLISECONDS);
        };

        // Use up maxAsync of 2
        try {
            sampleExec.submit(wait);
            sampleExec.submit(wait);
            assertTrue(started.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            // Queue up 2 asynchronous methods that won't be able to run yet
            CompletableFuture<Entry<Integer, Object>> futureA = //
                            dependentScopedBean.lookUpAndGetTransactionStatus("java:app/env/concurrent/sampleExecutorRef");
            CompletableFuture<Entry<Integer, Object>> futureB = //
                            dependentScopedBean.lookUpAndGetTransactionStatus("java:app/env/concurrent/sampleExecutorRef");

            // Run one of them inline,
            Entry<Integer, Object> resultsA = futureA.join();

            assertEquals(Integer.valueOf(Status.STATUS_NO_TRANSACTION), resultsA.getKey());
            assertTrue("looked up " + resultsA.getValue(), resultsA.getValue() instanceof ManagedExecutorService);

            tran.begin();

            // Should be able to queue up another
            CompletableFuture<Entry<Integer, Object>> futureC = //
                            dependentScopedBean.lookUpAndGetTransactionStatus("java:app/env/concurrent/sampleExecutorRef");

            // Run inline on an unmanaged thread,
            Future<Entry<Integer, Object>> unmanagedThreadFuture = unmanagedThreads.submit(() -> futureB.join());
            Entry<Integer, Object> resultsB = unmanagedThreadFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);

            assertEquals(Integer.valueOf(Status.STATUS_NO_TRANSACTION), resultsB.getKey());
            assertTrue("looked up " + resultsB.getValue(), resultsB.getValue() instanceof ManagedExecutorService);

            // Run inline again,
            Entry<Integer, Object> resultsC = futureC.get();

            assertEquals(Integer.valueOf(Status.STATUS_NO_TRANSACTION), resultsC.getKey());
            assertTrue("looked up " + resultsC.getValue(), resultsC.getValue() instanceof ManagedExecutorService);

            // Transaction that was active on this thread should be restored,
            assertEquals(Status.STATUS_ACTIVE, tran.getStatus());
        } finally {
            blocker.countDown();
            if (tran.getStatus() != Status.STATUS_NO_TRANSACTION)
                tran.rollback();
        }
    }

    /**
     * Invoke an asynchronous method on a CDI managed bean that isn't otherwise annotated.
     */
    @Test
    public void testManagedBeanAsyncMethod() throws Exception {
        ManagedExecutorService expectedExecutor = InitialContext.doLookup("java:app/env/concurrent/sampleExecutorRef");
        CompletableFuture<Object> future = managedBean.asyncLookup("java:app/env/concurrent/sampleExecutorRef");
        assertEquals(expectedExecutor, future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * Use the CompletableFuture obtrude methods to replace the result or exception
     * of asynchronous methods that are in progress, already completed, or queued for execution.
     */
    @Test
    public void testObtrudeAsynchronousMethod() throws Exception {
        CountDownLatch blocker1 = new CountDownLatch(1);
        CountDownLatch blocker2 = new CountDownLatch(2);
        LinkedBlockingQueue<Object> started = new LinkedBlockingQueue<Object>();
        try {
            // use up maxAsync of 2
            CompletableFuture<Boolean> future1 = sessionScopedBean.await(blocker1, TIMEOUT_MS, TimeUnit.MILLISECONDS, started);
            CompletableFuture<Boolean> future2 = sessionScopedBean.await(blocker2, TIMEOUT_MS, TimeUnit.MILLISECONDS, started);
            assertNotNull(started.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertNotNull(started.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            CompletableFuture<Executor> future3 = requestScopedBean.getExecutorOfAsyncMethods();

            // obtrude a queued asynchronous method:
            future3.obtrudeException(new IOException("Fake error to test obtrudeException."));

            // obtrude a running asynchronous method:
            future2.obtrudeValue(true);

            // obtrude an asynchronous method after it completes with a different value
            blocker1.countDown();
            assertEquals(Boolean.TRUE, future1.get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            future1.obtrudeValue(false);
            assertEquals(Boolean.FALSE, future1.join());

            // obtrude it again, this time with an exception
            future1.obtrudeException(new IOError(new IOException("Another fake error to test obtrudeException.")));

            try {
                Boolean result = future1.join();
                fail("Result should have been obtruded by an Error. Instead: " + result);
            } catch (CompletionException x) {
                if (!(x.getCause() instanceof IOError) ||
                    !(x.getCause().getCause() instanceof IOException))
                    throw x;
            }

            assertEquals(Boolean.TRUE, future2.getNow(null));

            try {
                Executor result = future3.getNow(null);
                fail("Result should have been obtruded by an Exception. Instead: " + result);
            } catch (CompletionException x) {
                if (!(x.getCause() instanceof IOException))
                    throw x;
            }
        } finally {
            blocker1.countDown();
            blocker2.countDown();
        }
    }

    /**
     * Verify that application context is propagated when the ContextServiceDefinition does not
     * configure it explicitly, but specifies to propagate all remaining context types.
     */
    @Test
    public void testRemainingContextPropagated() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:module/concurrent/txcontextcleared");
        Supplier<?> contextualSupplier = contextSvc.contextualSupplier(() -> {
            try {
                return InitialContext.doLookup("java:module/concurrent/txcontextcleared");
            } catch (NamingException x) {
                throw new CompletionException(x);
            }
        });

        CompletableFuture<?> future = CompletableFuture.supplyAsync(contextualSupplier, unmanagedThreads);

        Object result = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(result);
        assertTrue(result.toString(), result instanceof ContextService);
    }

    /**
     * Verify that transaction context and application context are left alone when the
     * ContextServiceDefinition does not configure them explicitly, but specifies to
     * leave all remaining context types unchanged.
     */
    @Test
    public void testRemainingContextUnchangedNoneCleared() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:module/concurrent/remainingcontextunchanged");
        Supplier<Object> lookUpAndGetActiveTransaction = contextSvc.contextualSupplier(() -> {
            try {
                assertNotNull(InitialContext.doLookup("java:module/concurrent/remainingcontextunchanged"));

                TransactionSynchronizationRegistry txSyncRegistry = //
                                InitialContext.doLookup("java:comp/TransactionSynchronizationRegistry");
                return txSyncRegistry.getTransactionKey();
            } catch (NamingException x) {
                throw new CompletionException(x);
            }
        });

        tran.begin();
        try {
            Object txExpected = tranSyncRegistry.getTransactionKey();
            assertEquals(txExpected, lookUpAndGetActiveTransaction.get());
            assertEquals(txExpected, tranSyncRegistry.getTransactionKey()); // original context must remain on thread afterward
        } finally {
            tran.rollback();
        }

        assertEquals(null, lookUpAndGetActiveTransaction.get());
    }

    /**
     * Verify that transaction context is left alone while application context is cleared
     * when the ContextServiceDefinition does not configure transaction context explicitly,
     * but specifies to leave all remaining context types unchanged.
     */
    @Test
    public void testRemainingContextUnchangedSomeCleared() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:module/concurrent/appcontextcleared");
        Supplier<Object> getActiveTransaction = contextSvc.contextualSupplier(() -> {
            try {
                Object unexpected = InitialContext.doLookup("java:module/concurrent/appcontextcleared");
                throw new AssertionError("Must not be able to look up " + unexpected);
            } catch (NamingException x) {
                // expected
            }

            return tranSyncRegistry.getTransactionKey();
        });

        tran.begin();
        try {
            Object txExpected = tranSyncRegistry.getTransactionKey();
            assertEquals(txExpected, getActiveTransaction.get());
            assertEquals(txExpected, tranSyncRegistry.getTransactionKey()); // original context must remain on thread afterward
        } finally {
            tran.rollback();
        }

        assertEquals(null, getActiveTransaction.get());
    }

    /**
     * Specify the asynchronous method annotation at both class and method level,
     * with conflicting values for the executor parameter. Verify that method level
     * takes precedence.
     */
    @Test
    public void testRequestScopedBeanAsyncMethodAnnotationsConflict() throws Exception {
        ManagedExecutorService expectedExecutor = InitialContext.doLookup("java:app/env/concurrent/sampleExecutorRef");
        CompletableFuture<Executor> future = requestScopedBean.getExecutorOfAsyncMethods();
        try {
            Executor asyncMethodExecutor = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertEquals(expectedExecutor.toString(), asyncMethodExecutor.toString());
        } catch (ExecutionException x) {
            if (x.getCause() instanceof NoSuchMethodException)
                ; // skip on Java 8, which lacks a CompletableFuture.defaultExecutor method
            else
                throw x;
        }
    }

    /**
     * Asynchronous method times out per the configured startTimeout.
     */
    @Test
    public void testRequestScopedBeanAsyncMethodTimesOut() throws Exception {
        CountDownLatch blocker = new CountDownLatch(1);
        // Use up the max concurrency of 1
        CompletionStage<Boolean> await1stage = requestScopedBean.await(blocker, TIMEOUT_MS, TimeUnit.MILLISECONDS);
        try {
            // Use up the single queue position
            CompletionStage<Boolean> await2stage = requestScopedBean.await(new CountDownLatch(0), TIMEOUT_MS, TimeUnit.MILLISECONDS);
            // Wait for the startTimeout to be exceeded on the above by attempting to queue another,
            CompletionStage<Boolean> await3stage = requestScopedBean.await(new CountDownLatch(0), TIMEOUT_MS, TimeUnit.MILLISECONDS);

            // Allow async methods to run
            blocker.countDown();

            // Second async method should time out
            try {
                Boolean result = await2stage.toCompletableFuture().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
                // TimeoutException from above. It never starts, but it doesn't notice the timeout either.
                fail("Second async method " + await2stage + " should have timed out per the startTimeout. Instead: " + result);
            } catch (ExecutionException x) {
                // TODO ideally the above should be AbortedException, which is a subclass of ExecutionException -
                // Consider changing it on the main code path for ManagedCompletableFuture.get, not specific to Asynchronous
                if (x.getMessage() == null || !x.getMessage().contains("CWWKE1205E") //
                    || x.getCause() == null || !x.getCause().getClass().getSimpleName().equals("StartTimeoutException"))
                    throw x;
            }

            // First async method should run
            assertEquals(Boolean.TRUE, await1stage.toCompletableFuture().get(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            // Third async method might run or time out depending on timing
            try {
                await3stage.toCompletableFuture().join();
            } catch (CompletionException x) {
                if (x.getMessage() == null || !x.getMessage().contains("CWWKE1205E") //
                    || x.getCause() == null || !x.getCause().getClass().getSimpleName().equals("StartTimeoutException"))
                    throw x;
            }
        } finally {
            blocker.countDown();
        }
    }

    /**
     * Try to use an asynchronous method for which the executor parameter points
     * to a JNDI name that is something other than a ManagedExecutorService.
     */
    @Test
    public void testSessionScopedBeanAsyncMethodExecutorNotAnExecutor() throws Exception {
        try {
            CompletionStage<String> stage = sessionScopedBean.jndiNameNotAnExecutor();
            fail("Should not be able to invoke an asynchronous method when executor JNDI name points to a UserTransaction: " + stage);
        } catch (RejectedExecutionException x) {
            if (x.getCause() instanceof ClassCastException)
                ; // pass
            else
                throw x;
        }
    }

    /**
     * Try to use an asynchronous method for which the executor parameter points
     * to an executor JNDI name that does not exist.
     */
    @Test
    public void testSessionScopedBeanAsyncMethodExecutorNotFound() throws Exception {
        try {
            CompletionStage<String> stage = sessionScopedBean.jndiNameNotFound();
            fail("Should not be able to invoke an asynchronous method with invalid executor JNDI name: " + stage);
        } catch (RejectedExecutionException x) {
            if (x.getCause() instanceof NameNotFoundException)
                ; // pass
            else
                throw x;
        }
    }

    /**
     * Verify that asynchronous methods run on the specified executor.
     * To do this, invoke methods in excess of the maximum concurrency
     * and confirm that it limits the number running at the same time,
     * and attempt to queue up async methods in excess of the maximum queue size.
     */
    @Test
    public void testSessionScopedBeanAsyncMethodRunsOnSpecifiedExecutor() throws Exception {
        LinkedBlockingQueue<Object> lookedUpResources = new LinkedBlockingQueue<Object>();
        CountDownLatch blocker = new CountDownLatch(1);

        List<CompletableFuture<Boolean>> futures = new ArrayList<CompletableFuture<Boolean>>(5);
        try {
            futures.add(sessionScopedBean.await(blocker, TIMEOUT_MS, TimeUnit.MILLISECONDS, lookedUpResources));
            futures.add(sessionScopedBean.await(blocker, TIMEOUT_MS, TimeUnit.MILLISECONDS, lookedUpResources));

            // Wait for the first 2 async methods to start running and use up the max concurrency.
            assertNotNull(lookedUpResources.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertNotNull(lookedUpResources.poll(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            // Fill the 2 queue positions
            futures.add(sessionScopedBean.await(blocker, TIMEOUT_MS, TimeUnit.MILLISECONDS, lookedUpResources));
            futures.add(sessionScopedBean.await(blocker, TIMEOUT_MS, TimeUnit.MILLISECONDS, lookedUpResources));

            // No queue positions remain for submitting additional async methods
            try {
                futures.add(sessionScopedBean.await(blocker, TIMEOUT_MS, TimeUnit.MILLISECONDS, lookedUpResources));
                fail("Queue capacity or max concurrency exceeded or methods ended too soon " + futures + ", " + lookedUpResources);
            } catch (RejectedExecutionException x) {
                // expect error for attempt to exceed queue capacity
                if (x.getMessage() == null || !x.getMessage().startsWith("CWWKE1201E"))
                    throw x;
            }

            // no more than 2 async methods should run in parallel, per the concurrency policy
            assertNull(lookedUpResources.poll(500, TimeUnit.MILLISECONDS));

            assertFalse(futures.get(0).isDone());
            assertFalse(futures.get(1).isDone());
            assertFalse(futures.get(2).isDone());
            assertFalse(futures.get(3).isDone());

            // cancel one of the queued asynchronous methods
            assertTrue(futures.get(2).cancel(false));

            // now we can submit another
            futures.add(sessionScopedBean.await(blocker, TIMEOUT_MS, TimeUnit.MILLISECONDS, lookedUpResources));

            // unblock the asynchronous methods
            blocker.countDown();

            // all async methods except the one we couldn't submit and the one we canceled
            // should complete successfully
            assertTrue(futures.get(0).get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(futures.get(1).get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(futures.get(2).isCancelled());
            assertTrue(futures.get(2).isCompletedExceptionally());
            assertTrue(futures.get(3).get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            assertTrue(futures.get(4).get(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            // from 3 and 4 (already processed for 0 and 1)
            assertNotNull(lookedUpResources.poll());
            assertNotNull(lookedUpResources.poll());
            assertNull(lookedUpResources.poll(500, TimeUnit.MILLISECONDS));

            try {
                Boolean result = futures.get(2).get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
                fail(futures.get(2) + " should have been canceled. Instead: " + result);
            } catch (CancellationException x) {
            }

            try {
                Boolean result = futures.get(2).join();
                fail(futures.get(2) + " should have been canceled. Instead: " + result);
            } catch (CancellationException x) {
            }
        } finally {
            // clean up in case of an error so that we don't block other tests
            for (CompletableFuture<Boolean> f : futures)
                f.cancel(false);
            blocker.countDown();
        }
    }

    /**
     * Verify that a bean that implements Callable can be submitted to a managed executor.
     */
    @Test
    public void testServletSubmitsBeanToManagedExecutor() throws Exception {
        appScopedBean.setCharacter('c');
        requestScopedBean.setNumber(2);
        sessionScopedBean.setText("This is some text");
        singletonScopedBean.put("Key_TaskBean", "value");

        Future<String> future = executor.submit(taskBean);
        try {
            String result = future.get();
            if (!"value1".equals(result))
                throw new Exception("Unexpected result: " + result);
        } finally {
            future.cancel(true);
        }

        char ch = appScopedBean.getCharacter();
        if (ch != 'C')
            throw new Exception("Character should have been capitalized by task. Instead: " + ch);

        boolean bool = dependentScopedBean.getBoolean();
        if (bool)
            throw new Exception("Value on @Dependent bean injected into servlet should not be impacted by @Dependent bean injected into TaskBean.");

        int num = requestScopedBean.getNumber();
        if (num != 2)
            throw new Exception("Unexpected number after running task: " + num);

        String text = sessionScopedBean.getText();
        if (!"This is some text".equals(text))
            throw new Exception("Unexpected text after running task: " + text);

        Object value = singletonScopedBean.get("Key_TaskBean");
        if (!"value and more text".equals(value))
            throw new Exception("Unexpected value in map after running task: " + value);
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute NEVER
     */
    @Test
    public void testServletSubmitsManagedTaskThatInvokesTxNever() throws Exception {
        Future<?> future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object beanTxKey = bean.runAsNever();
                if (beanTxKey != null)
                    throw new Exception("TX_NEVER should not run in a transaction: " + beanTxKey);

                tran.begin();
                try {
                    bean.runAsNever();
                    throw new Exception("Should not be able to invoke TX_NEVER method when there is a transaction on the thread");
                } catch (TransactionalException x) {
                    if (x.getMessage() == null || !x.getMessage().contains("TxType.NEVER"))
                        throw x;
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute REQUIRES_NEW
     */
    @Test
    public void testServletSubmitsManagedTaskThatInvokesTxRequiresNew() throws Exception {
        Future<?> future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                assertNull(tranSyncRegistry.getTransactionKey());
                Object beanTxKey = bean.runAsRequiresNew();
                if (beanTxKey == null)
                    throw new Exception("TxType.REQUIRES_NEW must run in a transaction: " + beanTxKey);

                tran.begin();
                try {
                    Object txKey = tranSyncRegistry.getTransactionKey();
                    beanTxKey = bean.runAsRequiresNew();
                    assertNotSame(txKey, beanTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute SUPPORTS
     */
    @Test
    public void testServletSubmitsManagedTaskThatInvokesTxSupports() throws Exception {
        Future<?> future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object beanTxKey = bean.runAsSupports();
                if (beanTxKey != null)
                    throw new Exception("Bean method with TX_SUPPORTS should not run in a transaction " + beanTxKey + " when there is none on the invoking thread.");

                tran.begin();
                try {
                    beanTxKey = bean.runAsSupports();
                    Object taskTxKey = tranSyncRegistry.getTransactionKey();
                    if (!taskTxKey.equals(beanTxKey))
                        throw new Exception("Bean with TX_SUPPORTS should run in the transaction of the invoker " + taskTxKey + ", not " + beanTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Verify that one async method can invoke another.
     * The first async method is on a Singleton bean and
     * the second async method is on a DependentScoped bean.
     */
    @Test
    public void testSingletonScopedBeanAsyncMethodInvokesAnotherAsyncMethod() throws Exception {
        CompletionStage<List<String>> stage = taskBean //
                        .lookupAll("java:comp/env/concurrent/executorRef", //
                                   "java:app/env/concurrent/sampleExecutorRef", //
                                   "java:module/env/concurrent/timeoutExecutorRef");
        List<String> lookupResults = stage.toCompletableFuture().get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(lookupResults.toString(), 3, lookupResults.size());
        assertTrue(lookupResults.toString(), lookupResults.get(0).endsWith("managedExecutorService[DefaultManagedExecutorService]"));
        assertTrue(lookupResults.toString(), lookupResults.get(1).endsWith("concurrent/sampleExecutor"));
        assertTrue(lookupResults.toString(), lookupResults.get(2).endsWith("concurrent/timeoutExecutor"));
    }

    /**
     * Verify that transaction context is cleared per the ContextServiceDefinition configuration.
     */
    @Test
    public void testTransactionContextCleared() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:module/concurrent/txcontextcleared");
        tran.begin();
        try {
            assertEquals(Status.STATUS_ACTIVE, tranSyncRegistry.getTransactionStatus());

            contextSvc.contextualCallable(() -> {
                // transaction context must be cleared
                assertEquals(Status.STATUS_NO_TRANSACTION, tranSyncRegistry.getTransactionStatus());
                // when transaction context is cleared, a new transaction can be started on the thread
                tran.begin();
                try {
                    assertEquals(Status.STATUS_ACTIVE, tranSyncRegistry.getTransactionStatus());
                } finally {
                    tran.commit();
                }
                return true;
            }).call();
        } finally {
            tran.rollback();
        }
    }

    /**
     * Verify that transaction context is propagated per the ContextServiceDefinition configuration.
     */
    @Test
    public void testTransactionContextPropagated() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:app/concurrent/txcontext");
        Object txKey;
        CompletableFuture<Object> stage1 = new CompletableFuture<Object>();
        CompletableFuture<?> stage2;
        tran.begin();
        try {
            assertEquals(Status.STATUS_ACTIVE, tranSyncRegistry.getTransactionStatus());
            txKey = tranSyncRegistry.getTransactionKey();

            stage2 = stage1.thenAcceptAsync(contextSvc.contextualConsumer(expectedTxKey -> {
                try {
                    // transaction context must be propagated to the thread
                    assertEquals(Status.STATUS_ACTIVE, tranSyncRegistry.getTransactionStatus());
                    assertEquals(expectedTxKey, tranSyncRegistry.getTransactionKey());
                    tran.commit();
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            }), unmanagedThreads);

            tm.suspend();
        } finally {
            if (tranSyncRegistry.getTransactionStatus() == Status.STATUS_ACTIVE)
                tran.rollback();
        }

        stage1.complete(txKey);
        stage2.join();
    }

    /**
     * Verify that transaction context is propagated to an asynchronous method,
     * per configuration of the ManagedExecutorService's ContextServiceDefinition.
     */
    @Test
    public void testTransactionContextPropagatedToAsyncMethod() throws Exception {
        // Use up maxAsync (1) for the managed executor so that the subsequent asynchronous
        // method can run upon join that is attempted from a different transaction.
        ManagedExecutorService executor = InitialContext.doLookup("java:module/concurrent/txexecutor");
        CountDownLatch blocker = new CountDownLatch(1);
        executor.submit(() -> blocker.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        try {
            CompletableFuture<Entry<Object, Integer>> future;
            Object tx1key, tx2key;

            tran.begin();
            try {
                tx1key = tranSyncRegistry.getTransactionKey();
                future = appScopedBean.getTransactionInfoAndCommit(tranSyncRegistry, tran);

                tm.suspend();
            } finally {
                if (tranSyncRegistry.getTransactionStatus() == Status.STATUS_ACTIVE)
                    tran.rollback();
            }

            tran.begin();
            try {
                tx2key = tranSyncRegistry.getTransactionKey();

                Entry<Object, Integer> txInfo = future.join(); // run inline
                assertEquals(tx1key, txInfo.getKey());
                assertEquals(Integer.valueOf(Status.STATUS_ACTIVE), txInfo.getValue());

                // Transaction context must be restored afterward
                assertEquals(tx2key, tranSyncRegistry.getTransactionKey());
                assertEquals(Status.STATUS_ACTIVE, tran.getStatus());
            } finally {
                tran.rollback();
            }
        } finally {
            blocker.countDown();
        }
    }

    /**
     * Verify that transaction context is propagated to an asynchronous task,
     * per configuration of the ManagedExecutorService's ContextServiceDefinition.
     */
    @Test
    public void testTransactionContextPropagatedToAsyncTask() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:module/concurrent/txexecutor");

        // In order to prevent overlap of the transaction on the submitter thread and
        // the thread of execution, we are making both run on the same managed executor,
        // which has maxAsync of 1.
        Future<Entry<Object, Future<Entry<Object, Integer>>>> submitterFuture = executor.submit(() -> {
            tran.begin();
            try {
                Object txKeyExpected = tranSyncRegistry.getTransactionKey();
                Future<Entry<Object, Integer>> future = executor.submit(() -> {
                    Object txKey = tranSyncRegistry.getTransactionKey();
                    int txStatus = tran.getStatus();
                    tran.commit();
                    return new SimpleEntry<Object, Integer>(txKey, txStatus);
                });

                tm.suspend();

                return new SimpleEntry<Object, Future<Entry<Object, Integer>>>(txKeyExpected, future);
            } finally {
                if (tran.getStatus() == Status.STATUS_ACTIVE)
                    tran.rollback();
            }
        });

        Entry<Object, Future<Entry<Object, Integer>>> submitterResults = submitterFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertNotNull(submitterResults);

        Object txKeyExpected = submitterResults.getKey();
        Future<Entry<Object, Integer>> future = submitterResults.getValue();

        Entry<Object, Integer> results = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(results);

        assertEquals(txKeyExpected, results.getKey());
        assertEquals(Integer.valueOf(Status.STATUS_ACTIVE), results.getValue());
    }

    /**
     * Verify that transaction context is propagated to an inline asynchronous task,
     * per configuration of the ManagedExecutorService's ContextServiceDefinition.
     */
    @Test
    public void testTransactionContextPropagatedToAsyncTaskInline() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:module/concurrent/txexecutor");

        // In order to prevent overlap of the transaction on the submitter thread and
        // the thread of execution, we are making both run on the same managed executor,
        // which has maxAsync of 1.
        Future<Entry<Object, Future<Entry<Object, Integer>>>> submitterFuture = executor.submit(() -> {
            tran.begin();
            try {
                Object txKeyExpected = tranSyncRegistry.getTransactionKey();
                Future<Entry<Object, Integer>> future = executor.submit(() -> {
                    Object txKey = tranSyncRegistry.getTransactionKey();
                    int txStatus = tran.getStatus();
                    tran.commit();
                    return new SimpleEntry<Object, Integer>(txKey, txStatus);
                });

                tm.suspend();

                return new SimpleEntry<Object, Future<Entry<Object, Integer>>>(txKeyExpected, future);
            } finally {
                if (tran.getStatus() == Status.STATUS_ACTIVE)
                    tran.rollback();
            }
        });

        Entry<Object, Future<Entry<Object, Integer>>> submitterResults = submitterFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);

        assertNotNull(submitterResults);

        Object txKeyExpected = submitterResults.getKey();
        Future<Entry<Object, Integer>> future = submitterResults.getValue();

        Entry<Object, Integer> results = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertNotNull(results);

        assertEquals(txKeyExpected, results.getKey());
        assertEquals(Integer.valueOf(Status.STATUS_ACTIVE), results.getValue());
    }

    /**
     * Verify that transaction context is propagated to a completion stage action,
     * per the configuration of the managed executor's ContextServiceDefinition.
     */
    @Test
    public void testTransactionContextPropagatedToCompletableFuture() throws Exception {
        ManagedExecutorService executor = InitialContext.doLookup("java:module/concurrent/txexecutor");
        Object txKey;
        CompletableFuture<Object> stage1 = executor.newIncompleteFuture();
        CompletableFuture<?> stage2;
        tran.begin();
        try {
            assertEquals(Status.STATUS_ACTIVE, tranSyncRegistry.getTransactionStatus());
            txKey = tranSyncRegistry.getTransactionKey();

            stage2 = stage1.thenAcceptAsync(expectedTxKey -> {
                try {
                    // transaction context must be propagated to the thread
                    assertEquals(expectedTxKey, tranSyncRegistry.getTransactionKey());
                    assertEquals(Status.STATUS_ACTIVE, tranSyncRegistry.getTransactionStatus());
                    tran.commit();
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            });

            tm.suspend();
        } finally {
            if (tranSyncRegistry.getTransactionStatus() == Status.STATUS_ACTIVE)
                tran.rollback();
        }

        assertTrue(stage1.complete(txKey));
        assertNull(stage2.get());
    }

    /**
     * Verify that transaction context is propagated to completion stages that
     * obtained via ContextService.withContextCapture.
     */
    @Test
    public void testTransactionContextPropagatedWithContextCapture() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:app/concurrent/txcontext");

        Object txKey;

        CompletableFuture<Object> stage1 = new CompletableFuture<Object>();
        CompletableFuture<Object> stage1copy = contextSvc.withContextCapture(stage1);
        CompletableFuture<Void> stage2;
        tran.begin();
        try {
            txKey = tranSyncRegistry.getTransactionKey();
            stage2 = stage1copy.thenAcceptAsync(txKeyExpected -> {
                assertEquals(txKeyExpected, tranSyncRegistry.getTransactionKey());
                try {
                    assertEquals(Status.STATUS_ACTIVE, tran.getStatus());
                    tran.commit();
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
            });

            tm.suspend();
        } finally {
            if (tranSyncRegistry.getTransactionStatus() == Status.STATUS_ACTIVE)
                tran.rollback();
        }

        assertTrue(stage1.complete(txKey));

        // Surface any errors that occur when stage2 tests transaction context propagation
        assertNull(stage2.get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * Verify that transaction context is left unchanged per the ContextServiceDefinition configuration.
     */
    @Test
    public void testTransactionContextUnchanged() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:comp/concurrent/txcontextunchanged");

        BiConsumer<Integer, Object> contextualConsumer = contextSvc.contextualConsumer((expectedStatus, expectedTxKey) -> {
            try {
                assertEquals(expectedStatus.intValue(), tranSyncRegistry.getTransactionStatus());
                assertEquals(expectedTxKey, tranSyncRegistry.getTransactionKey());
            } catch (Exception x) {
                throw new CompletionException(x);
            }
        });

        tran.begin();
        try {
            contextualConsumer.accept(Status.STATUS_ACTIVE, tranSyncRegistry.getTransactionKey());
        } finally {
            tran.rollback();
        }

        contextualConsumer.accept(Status.STATUS_NO_TRANSACTION, null);
    }

    /**
     * Verify that transaction context is left unchanged per the ContextServiceDefinition configuration,
     * and allows an inline completion stage action to run in the transaction that is already on the thread.
     */
    @Test
    public void testTransactionContextUnchangedForInlineCompletionStageAction() throws Exception {
        ManagedScheduledExecutorService executor = InitialContext.doLookup("java:comp/concurrent/appContextExecutor");

        CompletableFuture<Object> stage1 = executor.newIncompleteFuture();
        CompletableFuture<Void> stage2 = stage1.thenAccept(txKeyExpected -> {
            assertEquals(txKeyExpected, tranSyncRegistry.getTransactionKey());
            try {
                assertEquals(Status.STATUS_ACTIVE, tran.getStatus());
            } catch (Exception x) {
                throw new CompletionException(x);
            }
        });

        tran.begin();
        try {
            assertTrue(stage1.complete(tranSyncRegistry.getTransactionKey()));
            assertNull(stage2.join());
        } finally {
            tran.rollback();
        }
    }

    /**
     * Invoke an asynchronous method on a transaction scoped bean.
     */
    @Test
    public void testTransactionScopedBeanAsyncMethod() throws Exception {
        tran.begin();
        try {
            CompletableFuture<Integer> future = transactionScopedBean.incrementAsync(5);
            int result1 = transactionScopedBean.increment(10);
            int result2 = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            // increment can happen in either order, but the sum will always be 15,
            assertEquals(15, transactionScopedBean.get());
            assertEquals(result1 == 10 ? 15 : 5, result2);
        } finally {
            tran.commit();
        }
    }

    /**
     * When ContextServiceDefinition does not specify ALL_REMAINING for any category of context,
     * ALL_REMAINING is automatically added to the cleared types. Verify that application context
     * and transaction context are cleared from the thread while running the task.
     */
    @Test
    public void testUnspecifiedContextTypesCleared() throws Exception {
        ContextService contextSvc = InitialContext.doLookup("java:global/concurrent/allcontextcleared");

        tran.begin();
        try {
            Integer txStatusOfCallable = contextSvc.contextualCallable(() -> {
                try {
                    Object unexpected = InitialContext.doLookup("java:comp/concurrent/appContextExecutor");
                    throw new AssertionError("Application context must be cleared. Looked up " + unexpected);
                } catch (NamingException x) {
                    // expected because application component namespace is cleared
                    return tran.getStatus();
                }
            }).call();

            // transaction status must be cleared for contextual callable
            assertEquals(Integer.valueOf(Status.STATUS_NO_TRANSACTION), txStatusOfCallable);

            // transaction status must be restored on thread afterward,
            assertEquals(Status.STATUS_ACTIVE, tran.getStatus());
        } finally {
            tran.rollback();
        }
    }
}
