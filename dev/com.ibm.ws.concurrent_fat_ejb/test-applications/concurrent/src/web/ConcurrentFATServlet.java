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
package web;

import java.io.PrintWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRequiredException;
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import ejb.ConcurrentBMT;
import ejb.ConcurrentCMT;
import jakarta.enterprise.concurrent.ManagedExecutorService;

@SuppressWarnings("serial")
public class ConcurrentFATServlet extends FATServlet {

    /**
     * Maximum number of milliseconds to wait for a task to finish.
     */
    private static final long TIMEOUT = TimeUnit.MINUTES.toMillis(1);

    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    private ManagedExecutorService executor;

    @Resource
    private UserTransaction tran;

    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    private TransactionSynchronizationRegistry tranSyncRegistry;

    /**
     * Submit a concurrent task to invoke a bean method with bean managed transactions
     */
    @Test
    public void testBMTBeanSubmitsManagedTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        final ConcurrentBMT bean = (ConcurrentBMT) new InitialContext().lookup("java:global/concurrent/ConcurrentBMTBean!ejb.ConcurrentBMT");
        Future<?> future = bean.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    InitialContext initialContext = new InitialContext();

                    UserTransaction tran = (UserTransaction) initialContext.lookup("java:comp/UserTransaction");
                    tran.begin();
                    tran.commit();

                    ExecutorService executor1 = (ExecutorService) initialContext.lookup("java:comp/env/executor-bmt");
                    if (executor1 == null || executor1 instanceof ScheduledExecutorService)
                        throw new RuntimeException("Unexpected resource ref result " + executor1 + " for " + executor);
                } catch (RuntimeException x) {
                    throw x;
                } catch (Exception x) {
                    throw new RuntimeException(x);
                }
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }

        // Also submit a task with better access to fields of the EJB
        future = bean.submitTask();
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task from a CMT bean
     */
    @Test
    public void testCMTBeanSubmitsManagedTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");
        Future<?> future = bean.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                tran.commit();

                ExecutorService executor1 = (ExecutorService) new InitialContext().lookup("java:comp/env/executor-cmt");
                if (!(executor1 instanceof ScheduledExecutorService))
                    throw new RuntimeException("Unexpected resource ref result " + executor1 + " for " + executor);

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute MANDATORY
     */
    @Test
    @ExpectedFFDC("com.ibm.websphere.csi.CSITransactionRequiredException")
    public void testCMTBeanSubmitsManagedTaskThatInvokesTxMandatory(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");
        Future<?> future = bean.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    bean.runAsMandatory();
                    throw new Exception("Should not be able to invoke TX_MANDATORY method without a transaction");
                } catch (EJBTransactionRequiredException x) {
                } // pass

                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    Object ejbTxKey = bean.runAsMandatory();
                    Object taskTxKey = tranSyncRegistry.getTransactionKey();
                    if (!taskTxKey.equals(ejbTxKey))
                        throw new Exception("EJB with TX_MANDATORY has different transaction " + ejbTxKey + " than invoker " + taskTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute NEVER
     */
    @Test
    @ExpectedFFDC("com.ibm.websphere.csi.CSIException")
    public void testCMTBeanSubmitsManagedTaskThatInvokesTxNever(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");
        Future<?> future = bean.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object ejbTxKey = bean.runAsNever();
                if (ejbTxKey != null)
                    throw new Exception("TX_NEVER should not run in a transaction: " + ejbTxKey);

                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    bean.runAsNever();
                    throw new Exception("Should not be able to invoke TX_NEVER method when there is a transaction on the thread");
                } catch (EJBException x) {
                    if (x.getMessage() == null && !x.getMessage().contains("TX_NEVER"))
                        throw x;
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute REQUIRED
     */
    @Test
    public void testCMTBeanSubmitsManagedTaskThatInvokesTxRequired(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");
        Future<?> future = bean.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object ejbTxKey = bean.runAsRequired();
                if (ejbTxKey == null)
                    throw new Exception("EJB method with TX_REQUIRED didn't run in a transaction");

                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    ejbTxKey = bean.runAsRequired();
                    Object taskTxKey = tranSyncRegistry.getTransactionKey();
                    if (!taskTxKey.equals(ejbTxKey))
                        throw new Exception("EJB with TX_REQUIRED should run in the transaction of the invoker " + taskTxKey + ", not " + ejbTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute REQUIRED
     */
    @Test
    public void testCMTBeanSubmitsManagedTaskThatInvokesTxRequiresNew(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");
        Future<?> future = bean.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object ejbTxKey = bean.runAsRequiresNew();
                if (ejbTxKey == null)
                    throw new Exception("EJB method with TX_REQUIRES_NEW didn't run in a transaction");

                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    ejbTxKey = bean.runAsRequiresNew();
                    Object taskTxKey = tranSyncRegistry.getTransactionKey();
                    if (taskTxKey.equals(ejbTxKey))
                        throw new Exception("EJB with TX_REQUIRES_NEW should not run in the transaction of the invoker " + taskTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute NOT_SUPPORTED
     */
    @Test
    public void testCMTBeanSubmitsManagedTaskThatInvokesTxNotSupported(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");
        Future<?> future = bean.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object ejbTxKey = bean.runAsNotSupported();
                if (ejbTxKey != null)
                    throw new Exception("TX_NOT_SUPPORTED should not run in a transaction: " + ejbTxKey);

                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    ejbTxKey = bean.runAsNotSupported();
                    if (ejbTxKey != null)
                        throw new Exception("TX_NOT_SUPPORTED shouldn't run in a transaction: " + ejbTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute SUPPORTS
     */
    @Test
    public void testCMTBeanSubmitsManagedTaskThatInvokesTxSupports(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");
        Future<?> future = bean.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object ejbTxKey = bean.runAsSupports();
                if (ejbTxKey != null)
                    throw new Exception("EJB method with TX_SUPPORTS should not run in a transaction " + ejbTxKey + " when there is none on the invoking thread.");

                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    ejbTxKey = bean.runAsSupports();
                    Object taskTxKey = tranSyncRegistry.getTransactionKey();
                    if (!taskTxKey.equals(ejbTxKey))
                        throw new Exception("EJB with TX_SUPPORTS should run in the transaction of the invoker " + taskTxKey + ", not " + ejbTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Verify that a bean managed transactions EJB that implements Runnable can be submitted to a managed executor.
     */
    @Test
    public void testServletSubmitsBMTBeanToManagedExecutor(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        Runnable ejb = (Runnable) new InitialContext().lookup("java:global/concurrent/ConcurrentBMTBean!java.lang.Runnable");

        Future<?> future = executor.submit(ejb);
        try {
            Object result = future.get();
            if (result != null)
                throw new Exception("Unexpected result: " + result);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Verify that a container managed transactions EJB that implements Callable can be submitted to a managed executor.
     */
    @Test
    public void testServletSubmitsCMTBeanToManagedExecutor(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        @SuppressWarnings("unchecked")
        Callable<String> ejb = (Callable<String>) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!java.util.concurrent.Callable");

        Future<String> future = executor.submit(ejb);
        try {
            String result = future.get();
            if (!"value1".equals(result))
                throw new Exception("Unexpected result: " + result);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute MANDATORY
     */
    @Test
    @ExpectedFFDC("com.ibm.websphere.csi.CSITransactionRequiredException")
    public void testServletSubmitsManagedTaskThatInvokesTxMandatory(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        Future<?> future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");

                try {
                    bean.runAsMandatory();
                    throw new Exception("Should not be able to invoke TX_MANDATORY method without a transaction");
                } catch (EJBTransactionRequiredException x) {
                } // pass

                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    Object ejbTxKey = bean.runAsMandatory();
                    Object taskTxKey = tranSyncRegistry.getTransactionKey();
                    if (!taskTxKey.equals(ejbTxKey))
                        throw new Exception("EJB with TX_MANDATORY has different transaction " + ejbTxKey + " than invoker " + taskTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute NEVER
     */
    @Test
    @ExpectedFFDC("com.ibm.websphere.csi.CSIException")
    public void testServletSubmitsManagedTaskThatInvokesTxNever(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        Future<?> future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");

                Object ejbTxKey = bean.runAsNever();
                if (ejbTxKey != null)
                    throw new Exception("TX_NEVER should not run in a transaction: " + ejbTxKey);

                tran.begin();
                try {
                    bean.runAsNever();
                    throw new Exception("Should not be able to invoke TX_NEVER method when there is a transaction on the thread");
                } catch (EJBException x) {
                    if (x.getMessage() == null && !x.getMessage().contains("TX_NEVER"))
                        throw x;
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute REQUIRED
     */
    @Test
    public void testServletSubmitsManagedTaskThatInvokesTxRequired(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        Future<?> future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");

                Object ejbTxKey = bean.runAsRequired();
                if (ejbTxKey == null)
                    throw new Exception("EJB method with TX_REQUIRED didn't run in a transaction");

                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    ejbTxKey = bean.runAsRequired();
                    Object taskTxKey = tranSyncRegistry.getTransactionKey();
                    if (!taskTxKey.equals(ejbTxKey))
                        throw new Exception("EJB with TX_REQUIRED should run in the transaction of the invoker " + taskTxKey + ", not " + ejbTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute REQUIRED
     */
    @Test
    public void testServletSubmitsManagedTaskThatInvokesTxRequiresNew(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        Future<?> future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");

                Object ejbTxKey = bean.runAsRequiresNew();
                if (ejbTxKey == null)
                    throw new Exception("EJB method with TX_REQUIRES_NEW didn't run in a transaction");

                tran.begin();
                try {
                    ejbTxKey = bean.runAsRequiresNew();
                    Object taskTxKey = tranSyncRegistry.getTransactionKey();
                    if (taskTxKey.equals(ejbTxKey))
                        throw new Exception("EJB with TX_REQUIRES_NEW should not run in the transaction of the invoker " + taskTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute NOT_SUPPORTED
     */
    @Test
    public void testServletSubmitsManagedTaskThatInvokesTxNotSupported(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        Future<?> future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");

                Object ejbTxKey = bean.runAsNotSupported();
                if (ejbTxKey != null)
                    throw new Exception("TX_NOT_SUPPORTED should not run in a transaction: " + ejbTxKey);

                UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                tran.begin();
                try {
                    ejbTxKey = bean.runAsNotSupported();
                    if (ejbTxKey != null)
                        throw new Exception("TX_NOT_SUPPORTED shouldn't run in a transaction: " + ejbTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Submit a concurrent task to invoke a bean method with transaction attribute SUPPORTS
     */
    @Test
    public void testServletSubmitsManagedTaskThatInvokesTxSupports(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        Future<?> future = executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final ConcurrentCMT bean = (ConcurrentCMT) new InitialContext().lookup("java:global/concurrent/ConcurrentCMTBean!ejb.ConcurrentCMT");

                Object ejbTxKey = bean.runAsSupports();
                if (ejbTxKey != null)
                    throw new Exception("EJB method with TX_SUPPORTS should not run in a transaction " + ejbTxKey + " when there is none on the invoking thread.");

                tran.begin();
                try {
                    ejbTxKey = bean.runAsSupports();
                    Object taskTxKey = tranSyncRegistry.getTransactionKey();
                    if (!taskTxKey.equals(ejbTxKey))
                        throw new Exception("EJB with TX_SUPPORTS should run in the transaction of the invoker " + taskTxKey + ", not " + ejbTxKey);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    /**
     * Verify that a managed task can look up and invoke an EJB with bean managed transactions.
     */
    @Test
    public void testServletSubmitsManagedTaskThatLooksUpBMTBean(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        Future<?> future = executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Runnable ejbRunnable = (Runnable) new InitialContext().lookup("java:global/concurrent/ConcurrentBMTBean!java.lang.Runnable");
                ejbRunnable.run();

                ConcurrentBMT ejb = (ConcurrentBMT) new InitialContext().lookup("java:global/concurrent/ConcurrentBMTBean!ejb.ConcurrentBMT");
                tran.begin();
                try {
                    Object txKeyBefore = tranSyncRegistry.getTransactionKey();

                    Object ejbTxKey = ejb.getTransactionKey();
                    if (ejbTxKey != null)
                        throw new Exception("Transaction " + ejbTxKey + " found when invoking BMT bean method. Transaction on invoking thread was " + txKeyBefore);

                    ejbRunnable.run();

                    Object txKeyAfter = tranSyncRegistry.getTransactionKey();

                    if (!txKeyBefore.equals(txKeyAfter))
                        throw new Exception("Original transaction " + txKeyBefore + " not resumed on thread. Instead " + txKeyAfter);
                } finally {
                    tran.commit();
                }

                return null;
            }
        });
        try {
            future.get();
        } finally {
            future.cancel(true);
        }
    }
}
