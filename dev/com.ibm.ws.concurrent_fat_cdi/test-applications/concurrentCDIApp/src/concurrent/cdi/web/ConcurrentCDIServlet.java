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
package concurrent.cdi.web;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;

import org.junit.Test;

@SuppressWarnings("serial")
@WebServlet("/*")
public class ConcurrentCDIServlet extends HttpServlet {

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
