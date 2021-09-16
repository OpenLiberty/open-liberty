/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.TransactionalException;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import org.junit.Test;

@SuppressWarnings("serial")
@WebServlet("/*")
public class ConcurrentCDIServlet extends HttpServlet {

    /**
     * Maximum number of milliseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_MS = TimeUnit.MINUTES.toMillis(2);

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

    @Resource
    private UserTransaction tran;

    @Inject
    private TransactionScopedBean transactionScopedBean;

    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    private TransactionSynchronizationRegistry tranSyncRegistry;

    @Inject
    private ManagedExecutorService injectedExec; // produced by ResourcesProducer.exec field

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String method = request.getParameter("testMethod");

        System.out.println(">>> BEGIN: " + method);
        System.out.println("Request URL: " + request.getRequestURL() + '?' + request.getQueryString());
        PrintWriter writer = response.getWriter();
        if (method != null && method.length() > 0) {
            try {
                // Use reflection to try invoking various test method signatures:
                // 1)  method(HttpServletRequest request, HttpServletResponse response)
                // 2)  method()
                // 3)  use custom method invocation by calling invokeTest(method, request, response)
                try {
                    Method mthd = getClass().getMethod(method, HttpServletRequest.class, HttpServletResponse.class);
                    mthd.invoke(this, request, response);
                } catch (NoSuchMethodException nsme) {
                    Method mthd = getClass().getMethod(method, (Class<?>[]) null);
                    mthd.invoke(this);
                }

                writer.println("SUCCESS");
            } catch (Throwable t) {
                if (t instanceof InvocationTargetException) {
                    t = t.getCause();
                }

                System.out.println("ERROR: " + t);
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                System.err.print(sw);

                writer.println("ERROR: Caught exception attempting to call test method " + method + " on servlet " + getClass().getName());
                t.printStackTrace(writer);
            }
        } else {
            System.out.println("ERROR: expected testMethod parameter");
            writer.println("ERROR: expected testMethod parameter");
        }

        writer.flush();
        writer.close();

        System.out.println("<<< END:   " + method);
    }

    /**
     * Initialize the transaction service (including recovery logs) so it doesn't slow down our tests and cause timeouts.
     */
    public void initTransactionService() throws Exception {
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
     * A managed bean that is ApplicationScoped can be annotated Async at the class level,
     * and methods that return CompletableFuture will run asynchronously to the calling thread.
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
     * A managed bean that is ApplicationScoped can be annotated Async at the class level,
     * and methods that return CompletionStage will run asynchronously to the calling thread.
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
     * A managed bean that is ApplicationScoped can be annotated Async at the class level,
     * and methods with a void return type do not run asynchronously to the calling thread.
     */
    @Test
    public void testAppScopedBeanMethodWithVoidReturnTypeIsNotAsync() throws Exception {
        String curThreadName = Thread.currentThread().getName();
        AtomicReference<String> threadNameRef = new AtomicReference<String>();
        appScopedBean.notAsync(threadNameRef);
        assertEquals(curThreadName, threadNameRef.get());
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

    @Test
    public void testInjectedManagedExecutorService() {
        System.out.println("@AGG injected executor is: " + injectedExec);
        assertNotNull(injectedExec);
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
     * Invoke an asynchronous method on a CDI managed bean that isn't otherwise annotated.
     */
    @Test
    public void testManagedBeanAsyncMethod() throws Exception {
        ManagedExecutorService expectedExecutor = InitialContext.doLookup("java:app/env/concurrent/sampleExecutorRef");
        CompletableFuture<Object> future = managedBean.asyncLookup("java:app/env/concurrent/sampleExecutorRef");
        assertEquals(expectedExecutor, future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
            } catch (CancellationException x) {
                if (x.getMessage() == null || !x.getMessage().contains("CWWKE1205E") //
                    || x.getCause() == null || !x.getCause().getClass().getSimpleName().equals("StartTimeoutException"))
                    throw x;
            }

            // First async method should run
            assertEquals(Boolean.TRUE, await1stage.toCompletableFuture().get(TIMEOUT_MS, TimeUnit.MILLISECONDS));

            // Third async method might run or time out depending on how slow the machine running the test is
            try {
                await3stage.toCompletableFuture().join();
            } catch (CancellationException x) {
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
}
