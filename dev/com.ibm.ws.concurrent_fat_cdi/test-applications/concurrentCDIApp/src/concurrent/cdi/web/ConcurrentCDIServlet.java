/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package concurrent.cdi.web;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class ConcurrentCDIServlet extends FATServlet {

    /**
     * Maximum number of milliseconds to wait for a task to finish.
     */
    private static final long TIMEOUT = 30000;

    @Inject
    private ApplicationScopedBean appScopedBean;

    @Inject
    private DependentScopedBean dependentScopedBean;

    @Inject
    private TransactionalBean bean;

    @Resource(name = "java:comp/env/concurrent/executorRef")
    private ManagedExecutorService executor;

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

    @Resource(lookup = "java:comp/TransactionSynchronizationRegistry")
    private TransactionSynchronizationRegistry tranSyncRegistry;

    @Inject
    private ManagedExecutorService injectedExec; // produced by ResourcesProducer.exec field

    /**
     * Initialize the transaction service (including recovery logs) so it doesn't slow down our tests and cause timeouts.
     */
    public void initTransactionService() throws Exception {
        tran.begin();
        tran.commit();
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
            Object result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
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
                } catch (/* Transactional */Exception x) {
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
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
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
                } catch (/* Transactional */Exception x) {
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
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }
}
