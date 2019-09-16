/*******************************************************************************
 * Copyright (c) 2018,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.mp.fat.cdi.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.junit.Test;
import org.test.context.location.CurrentLocation;
import org.test.context.location.TestContextTypes;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MPConcurrentCDITestServlet")
public class MPConcurrentCDITestServlet extends FATServlet {

    static final int TIMEOUT_MIN = 2;

    @Inject
    ConcurrencyBean bean;

    @Inject
    BeanManager beanManager;

    @Inject
    RequestScopedBean requestBean;

    @Inject
    SessionScopedBean sessionBean;

    @Inject
    TransactionScopedBean txBean;

    @Inject
    ConversationScopeBean conversationBean;

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    public @interface AppContext {
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    public @interface CDIContext {
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    public @interface TxContext {
    }

    @Produces
    @ApplicationScoped
    @AppContext
    ThreadContext appContext = ThreadContext.builder()
                    .propagated(ThreadContext.APPLICATION)
                    .unchanged(TestContextTypes.STATE)
                    .cleared(ThreadContext.ALL_REMAINING)
                    .build();

    @Produces
    @ApplicationScoped
    @AppContext
    @TxContext
    ManagedExecutor appTxExecutor = ManagedExecutor.builder()
                    .propagated(ThreadContext.APPLICATION, ThreadContext.TRANSACTION)
                    .cleared()
                    .build();

    @Produces
    @ApplicationScoped
    @AppContext
    @CDIContext
    ManagedExecutor appCDIExecutor = ManagedExecutor.builder()
                    .propagated(ThreadContext.APPLICATION, ThreadContext.CDI)
                    .cleared()
                    .build();

    @Produces
    @ApplicationScoped
    ManagedExecutor noContextExecutor = ManagedExecutor.builder()
                    .propagated()
                    .cleared(ThreadContext.ALL_REMAINING)
                    .build();

    @Produces
    @ApplicationScoped
    @AppContext
    @CDIContext
    ThreadContext threadContextWithDefaults = ThreadContext.builder()
                    .build();

    @Resource
    UserTransaction tx;

    @Test
    public void testCDI_ME_Ctx_Propagate() throws Exception {
        checkCDIPropagation(true, "testCDI_ME_Ctx_Propagate-REQUEST", appCDIExecutor, requestBean);
        checkCDIPropagation(true, "testCDI_ME_Ctx_Propagate-SESSION", appCDIExecutor, sessionBean);
        checkCDIPropagation(true, "testCDI_ME_Ctx_Propagate-CONVERSATION", appCDIExecutor, conversationBean);
    }

    @Test
    public void testCDI_ME_Ctx_Clear() throws Exception {
        checkCDIPropagation(false, "testCDI_ME_Ctx_Clear-REQUEST", noContextExecutor, requestBean);
        checkCDIPropagation(false, "testCDI_ME_Ctx_Clear-SESSION", noContextExecutor, sessionBean);
        checkCDIPropagation(false, "testCDI_ME_Ctx_Clear-CONVERSATION", noContextExecutor, conversationBean);
    }

    private void checkCDIPropagation(boolean expectPropagate, String stateToPropagate, ManagedExecutor me, AbstractBean bean) throws Exception {
        bean.setState(stateToPropagate);
        CompletableFuture<String> cf = me.supplyAsync(() -> {
            String state = bean.getState();
            System.out.println(stateToPropagate + " state=" + state);
            return state;
        });
        assertEquals(expectPropagate ? stateToPropagate : AbstractBean.UNINITIALIZED, cf.get(TIMEOUT_MIN, TimeUnit.MINUTES));
    }

    @Test
    public void testCDI_TC_Ctx_Propagate() throws Exception {
        requestBean.setState("testCDIContextPropagate-STATE2");
        Callable<String> getState = threadContextWithDefaults.contextualCallable(() -> {
            String state = requestBean.getState();
            System.out.println("testCDIContextPropagate#2 state=" + state);
            return state;
        });
        assertEquals("testCDIContextPropagate-STATE2", getState.call());
    }

    @Test
    public void testCDI_TC_Ctx_Clear() throws Exception {
        ThreadContext clearAllCtx = ThreadContext.builder()
                        .propagated() // propagate nothing
                        .cleared(ThreadContext.ALL_REMAINING)
                        .build();

        requestBean.setState("testCDIThreadCtxClear-STATE1");

        Callable<String> getState = clearAllCtx.contextualCallable(() -> {
            String state = requestBean.getState();
            System.out.println("testCDIThreadCtxClear#1 state=" + state);
            return state;
        });
        assertEquals("UNINITIALIZED", getState.call());
    }

    @Test
    public void testAppDefinedQualifier() {
        assertNotNull(bean.getMyQualifier());
    }

    /**
     * Propagate CDI context across completion stages after having first accessed a request scoped bean.
     */
    @Test
    public void testCDIContextPropagationAcrossMultipleStagesBeanAccessedFirst() throws Exception {
        requestBean.getState();

        CompletableFuture<Void> cf = appCDIExecutor.runAsync(() -> {
            requestBean.setState(requestBean.getState() + ",1");
        }).thenRunAsync(() -> {
            requestBean.setState(requestBean.getState() + ",2");
        }).thenRunAsync(() -> {
            requestBean.setState(requestBean.getState() + ",3");
        });
        cf.join();

        assertEquals("UNINITIALIZED,1,2,3", requestBean.getState());
    }

    /**
     * Propagate CDI context to a completion stage. Access (and modify) a request scoped bean
     * within the stage, but not before the context snapshot is taken. Lazily enlisting CDI beans
     * should be rejected.
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalStateException")
    public void testCDIContextPropagationBeanFirstUsedInCompletionStage() throws Exception {
        sessionBean.setState("foo");

        CompletableFuture<Void> cf = appCDIExecutor.runAsync(() -> {
            requestBean.setState("A");
        });
        try {
            cf.join();
            fail("Should not be able to lazily enlist a CDI bean when CDI context is propagated.");
        } catch (CompletionException x) {
            if (x.getCause() instanceof IllegalStateException && x.getCause().getMessage().contains("CWWKC1158E")) {
                System.out.println("Caught expected IllegalStateException");
            } else {
                throw x;
            }
        }
        assertTrue("CompletableFuture should have been marked done", cf.isDone());
        assertTrue("CompletableFuture should have been marked as completed exceptionally", cf.isCompletedExceptionally());
    }

    /**
     * Verify that a ThreadContext instance behaves according to the specified configuration attributes of its builder.
     */
    @Test
    public void testThreadContextThatPropagatesApplicationContextOnly() throws Exception {
        // config: propagated = APPLICATION, unchanged = "State", cleared = ALL_REMAINING
        assertNotNull(appContext);

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        tx.begin();
        try {
            CurrentLocation.setLocation("Grand Forks", "North Dakota");
            Runnable task = appContext.contextualRunnable(() -> {
                try {
                    UserTransaction tx2 = InitialContext.doLookup("java:comp/UserTransaction");
                    tx2.begin(); // valid because prior transaction context is cleared (suspended) during task
                    tx2.commit();
                    // Should be possible to load application classes from class loader that is propagated by Application context
                    Thread.currentThread().getContextClassLoader().loadClass(BlockableIncrementFunction.class.getName());
                } catch (Exception x) {
                    throw new CompletionException(x);
                }
                assertEquals("", CurrentLocation.getCity()); // cleared
                assertEquals("Minnesota", CurrentLocation.getState()); // unchanged
                CurrentLocation.setLocation("Sioux Falls", "South Dakota");
            });

            CurrentLocation.setLocation("Vermillion", "Minnesota");
            Thread.currentThread().setContextClassLoader(null);

            task.run();

            assertEquals("Vermillion", CurrentLocation.getCity()); // restored after task
            assertEquals("South Dakota", CurrentLocation.getState()); // unchanged from task
        } finally {
            // restore context
            CurrentLocation.clear();
            tx.commit();
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Verify that a ThreadContext instance behaves according to vendor-specific defaults that are
     * used when neither the builder nor MP Config specify values to use.
     */
    @Test
    public void testThreadContextDefaults() throws Exception {
        assertNotNull(threadContextWithDefaults);

        tx.begin();
        try {
            CurrentLocation.setLocation("Byron", "Minnesota");

            Callable<String> getLocationName = threadContextWithDefaults.contextualCallable(() -> {
                UserTransaction tx2 = InitialContext.doLookup("java:comp/UserTransaction");
                tx2.begin();
                try {
                    return CurrentLocation.getCity() + ", " + CurrentLocation.getState();
                } finally {
                    tx2.commit();
                }
            });

            CurrentLocation.setLocation("Bismarck", "North Dakota");

            assertEquals("Byron, Minnesota", getLocationName.call());

            assertEquals(Status.STATUS_ACTIVE, tx.getStatus());
        } finally {
            CurrentLocation.clear();
            tx.commit();
        }
    }

    /**
     * Verify that we disallow propagating global transactions, but do allow propagating the absence of any transaction.
     */
    @AllowedFFDC("java.lang.IllegalStateException") // test attempts to propagate active transaction to 2 threads at once
    @Test
    public void testTransactionContextPropagation() throws Exception {
        ManagedExecutor executor = appTxExecutor; // propagates ThreadContext.TRANSACTION

        // valid to propagate empty transaction context
        CompletableFuture<Integer> cf1 = executor.newIncompleteFuture();
        CompletableFuture<Integer> cf2 = cf1.thenApply(i -> {
            try {
                return tx.getStatus();
            } catch (SystemException x) {
                throw new CompletionException(x);
            }
        });

        tx.begin();
        try {
            cf1.complete(50);
            assertEquals(Integer.valueOf(Status.STATUS_NO_TRANSACTION), cf2.get());

            assertEquals(Status.STATUS_ACTIVE, tx.getStatus());

            Future<?> f = executor.submit(() -> System.out.println("Should not be able to submit this task."));
            try {
                f.get(TIMEOUT_MIN, TimeUnit.MINUTES);
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof IllegalStateException)) // Active transaction cannot be propagated to 2 threads at once
                    throw x;
            }
        } finally {
            if (tx.getStatus() == Status.STATUS_ACTIVE)
                tx.commit();
            else
                tx.rollback();
        }

        // valid to propagate empty transaction context
        CompletableFuture<String> cf3 = cf2.thenApplyAsync(i -> "done");
        assertEquals("done", cf3.get(TIMEOUT_MIN, TimeUnit.MINUTES));
    }

    /**
     * Verify that TransactionScope beans reflect the propagation of an empty transaction context
     * and the restoration of the transaction context on the thread afterward.
     * Verify that the presence of CDI context propagation does not interfere.
     */
    @Test
    public void testTransactionScopeWithCDIContextPropagation() throws Exception {
        ThreadContext txAndCDIContext = ThreadContext.builder()
                        .propagated(ThreadContext.CDI, ThreadContext.TRANSACTION)
                        .cleared(ThreadContext.ALL_REMAINING)
                        .build();

        Runnable verifyContextNotActive = txAndCDIContext.contextualRunnable(() -> {
            try {
                String state = txBean.getState();
                throw new RuntimeException("TransactionScoped context should not be active when the absence of a transaction is propagated");
            } catch (ContextNotActiveException x) {
                // expected
            }
        });

        Callable<Boolean> updateStateWithinNewTransaction = txAndCDIContext.contextualCallable(() -> {
            tx.begin();
            try {
                assertEquals(AbstractBean.UNINITIALIZED, txBean.getState());
                txBean.setState("testTransactionScope-D");
                return true;
            } finally {
                tx.commit();
            }
        });

        tx.begin();
        try {
            txBean.setState("testTransactionScope-C");

            verifyContextNotActive.run();

            assertEquals("testTransactionScope-C", txBean.getState());

            assertEquals(Boolean.TRUE, updateStateWithinNewTransaction.call());

            assertEquals("testTransactionScope-C", txBean.getState());
        } finally {
            tx.commit();
        }
    }

    /**
     * Verify that TransactionScope beans reflect the propagation of an empty transaction context
     * and the restoration of the transaction context on the thread afterward.
     * Verify that the clearing of CDI context does not interfere.
     */
    @Test
    public void testTransactionScopeWithoutCDIContextPropagation() throws Exception {
        ManagedExecutor executor = appTxExecutor; // propagates ThreadContext.TRANSACTION

        CompletableFuture<Boolean> readyToVerifyContextNotActive = executor.newIncompleteFuture();
        CompletableFuture<String> verifyContextNotActive = readyToVerifyContextNotActive.thenApply(b -> {
            try {
                return txBean.getState();
            } catch (ContextNotActiveException x) {
                return "ContextNotActiveException";
            }
        });

        CompletableFuture<Boolean> readyToUpdateState = executor.newIncompleteFuture();
        CompletableFuture<Boolean> updateStateWithinNewTransaction = readyToUpdateState.thenApply(b -> {
            try {
                tx.begin();
                try {
                    assertEquals(AbstractBean.UNINITIALIZED, txBean.getState());
                    txBean.setState("testTransactionScope-B");
                    return true;
                } finally {
                    tx.commit();
                }
            } catch (Exception x) {
                throw new CompletionException(x);
            }
        });

        tx.begin();
        try {
            txBean.setState("testTransactionScope-A");

            readyToVerifyContextNotActive.complete(true);
            assertEquals("ContextNotActiveException", verifyContextNotActive.join());

            assertEquals("testTransactionScope-A", txBean.getState());

            readyToUpdateState.complete(true);
            assertEquals(Boolean.TRUE, updateStateWithinNewTransaction.join());

            assertEquals("testTransactionScope-A", txBean.getState());
        } finally {
            tx.commit();
        }
    }
}
