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

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.wsspi.threadcontext.WSContextService;

@SuppressWarnings("serial")
public class ContextServiceFATServlet extends HttpServlet {
    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Resource
    private UserTransaction tran;

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     * Another parameter, "contextService", can be used to control which context service should be used.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        String contextSvc = request.getParameter("contextService");
        PrintWriter out = response.getWriter();

        try {
            out.println("ContextServiceFATServlet is starting " + test + "; contextService=" + contextSvc + "<br>");
            System.out.println("-----> " + test + " starting");
            getClass().getMethod(test, String.class, PrintWriter.class).invoke(this, contextSvc, out);
            System.out.println("<----- " + test + " successful");
            out.println(test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + test + " failed:");
            x.printStackTrace(System.out);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        } finally {
            out.flush();
            out.close();
        }
    }

    /**
     * Verify that a contextual task can load classes with the application's classloader.
     *
     * @param contextSvcName the context service to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testClassloaderContext(String contextSvcName, PrintWriter out) throws Exception {

        final String className = getClass().getName();

        final Callable<Class<?>> loadClass = new Callable<Class<?>>() {
            @Override
            public Class<?> call() throws Exception {
                System.out.println("running task");
                return Thread.currentThread().getContextClassLoader().loadClass(className);
            }
        };

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        // Class load from current thread should work
        Class<?> loadedClass = loadClass.call();
        if (!loadedClass.getName().equals(className))
            throw new Exception("Unexpected class loaded on current thread: " + loadedClass);

        ContextService contextSvc = (ContextService) new InitialContext().lookup(contextSvcName);
        try {
            // Capture the context
            @SuppressWarnings("unchecked")
            Callable<Class<?>> contextualLoadClass = contextSvc.createContextualProxy(loadClass, Callable.class);

            // Change the class loader to something else (it's convenient to get the classloader for WSContextService, so we will take that)
            ClassLoader newClassLoader = contextSvc.getClass().getClassLoader();
            // Verify the class doesn't load from the new class loader we have chosen
            Thread.currentThread().setContextClassLoader(newClassLoader);
            try {
                loadedClass = loadClass.call();
                throw new Exception("Should not be able to load " + className + " from WSContextService classloader " + newClassLoader);
            } catch (ClassNotFoundException x) {
            } // expected

            // Class load with context should work
            loadedClass = contextualLoadClass.call();

            if (!loadedClass.getName().equals(className))
                throw new Exception("Unexpected class loaded on current thread with context: " + loadedClass);

            // Verify that the new context classloader we previously set on the thread is still there
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (!newClassLoader.equals(cl))
                throw new Exception("Class loader that we set on the thread " + newClassLoader + " should still be present on thread. Instead: " + cl);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader); // restore to original value
        }
    }

    /**
     * Verify that a contextual task runs with access to the application component namespace.
     *
     * @param contextSvcName the context service to use.
     * @param out PrintWriter for servlet response
     * @throws Throwable if it fails.
     */
    public void testDefaultContextForAllContextTypes(String contextSvcName, PrintWriter out) throws Exception {
        Callable<?> javaCompLookup = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                InitialContext initialContext = new InitialContext();
                tran.begin();
                try {
                    return initialContext.lookup("java:comp/env/entry1");
                } catch (NamingException x) { // pass - java:comp lookup should not be allowed when thread context is cleared
                    return "EXPECTED_ERROR_OCCURRED";
                } finally {
                    tran.commit();
                }
            }
        };

        ContextService contextSvc = (ContextService) new InitialContext().lookup(contextSvcName);
        Map<String, String> execProps = Collections.singletonMap(WSContextService.DEFAULT_CONTEXT, WSContextService.ALL_CONTEXT_TYPES);
        javaCompLookup = contextSvc.createContextualProxy(javaCompLookup, execProps, Callable.class);

        tran.begin();
        try {
            Object result = javaCompLookup.call();
            if (!"EXPECTED_ERROR_OCCURRED".equals(result))
                throw new Exception("Thread context should have been cleared/defaulted. Should not be able to lookup from java:comp. Value: " + result);
        } finally {
            tran.commit();
        }
    }

    /**
     * Verify that a contextual task runs with access to the application component namespace.
     *
     * @param contextSvcName the context service to use.
     * @param out PrintWriter for servlet response
     * @throws Throwable if it fails.
     */
    public void testJEEMetadataContext(String contextSvcName, PrintWriter out) throws Throwable {

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

        // Lookup from different thread (without context) should fail
        new Thread(javaCompLookup).start();

        // wait for it to run on the thread
        result = results.poll(10, TimeUnit.SECONDS);

        if (result == null)
            throw new Exception("Taking too long (over 10 seconds) to run task");
        else if (result instanceof NamingException)
            ; // expected
        else if (result instanceof Throwable)
            throw new ExecutionException((Throwable) result);
        else
            throw new Exception("jeeMetadataContext should not be available on new thread. Value is: " + result);

        // Lookup from different thread (with context) should work
        ContextService contextSvc = (ContextService) new InitialContext().lookup(contextSvcName);

        Runnable contextualJavaCompLookup = contextSvc.createContextualProxy(javaCompLookup, Runnable.class);
        new Thread(contextualJavaCompLookup).start();

        // wait for it to run on the thread
        result = results.poll(10, TimeUnit.SECONDS);

        if (result == null)
            throw new Exception("Taking too long (over 10 seconds) to run task");
        else if (result instanceof Throwable)
            throw (Throwable) result;

        if (!"value1".equals(result))
            throw new Exception("Unexpected value for java:comp/env/entry1 from new thread: " + result);

    }

    /**
     * Verify that a contextual task does not run with the application's classloader.
     *
     * @param contextSvcName the context service to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testNoClassloaderContext(String contextSvcName, PrintWriter out) throws Exception {

        final String className = getClass().getName();

        final Callable<Class<?>> loadClass = new Callable<Class<?>>() {
            @Override
            public Class<?> call() throws Exception {
                System.out.println("running task");
                return Thread.currentThread().getContextClassLoader().loadClass(className);
            }
        };

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        // Class load from current thread should work
        Class<?> loadedClass = loadClass.call();
        if (!loadedClass.getName().equals(className))
            throw new Exception("Unexpected class loaded on current thread: " + loadedClass);

        ContextService contextSvc = (ContextService) new InitialContext().lookup(contextSvcName);
        try {
            // Capture the context
            @SuppressWarnings("unchecked")
            Callable<Class<?>> contextualLoadClass = contextSvc.createContextualProxy(loadClass, Callable.class);

            // Change the class loader to something else (it's convenient to get the classloader for WSContextService, so we will take that)
            ClassLoader newClassLoader = contextSvc.getClass().getClassLoader();
            // Verify the class doesn't load from the new class loader we have chosen
            Thread.currentThread().setContextClassLoader(newClassLoader);
            try {
                loadedClass = loadClass.call();
                throw new Exception("Should not be able to load " + className + " from WSContextService classloader " + newClassLoader);
            } catch (ClassNotFoundException x) { // expected
            }

            // Verify that the class doesn't load from the contextual task either
            try {
                loadedClass = contextualLoadClass.call();
                throw new Exception("Should not be able to load " + className + " from contextService=" + contextSvcName + " contextual task. Classloader used: " + newClassLoader);
            } catch (ClassNotFoundException x) { // expected
            }

            // Verify that the new context classloader we previously set on the thread is still there
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (!newClassLoader.equals(cl))
                throw new Exception("Class loader that we set on the thread " + newClassLoader + " should still be present on thread. Instead: " + cl);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader); // restore to original value
        }
    }

    /**
     * Verify that a contextual task runs without access to the application component namespace.
     *
     * @param contextSvcName the context service to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testNoJEEMetadataContext(String contextSvcName, PrintWriter out) throws Exception {

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

        // Lookup from different thread (without jeeMetadataContext) should fail
        ContextService contextSvc = (ContextService) new InitialContext().lookup(contextSvcName);

        Runnable contextualJavaCompLookup = contextSvc.createContextualProxy(javaCompLookup, Runnable.class);
        new Thread(contextualJavaCompLookup).start();

        // wait for it to run on the thread
        result = results.poll(10, TimeUnit.SECONDS);

        if (result == null)
            throw new Exception("Taking too long (over 10 seconds) to run task");
        else if (result instanceof NamingException)
            ; // expected
        else if (result instanceof Throwable)
            throw new ExecutionException((Throwable) result);
        else
            throw new Exception("jeeMetadataContext should not be available on new thread. Value is: " + result);
    }

    /**
     * Verify that a contextual task does not run in a new LTC.
     *
     * @param contextSvcName the context service to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testNoTransactionContext(String contextSvcName, PrintWriter out) throws Exception {
        BundleContext bundleContext = FrameworkUtil.getBundle(WSContextService.class).getBundleContext();
        ServiceReference<TransactionManager> tranMgrRef = bundleContext.getServiceReference(TransactionManager.class);
        ContextService contextSvc = (ContextService) new InitialContext().lookup(contextSvcName);

        tran.begin();
        try {
            final TransactionManager tranMgr = bundleContext.getService(tranMgrRef);

            Transaction tran1 = tranMgr.getTransaction();
            System.out.println("before task: " + tran1);

            Map<String, String> execProps = Collections.singletonMap(ManagedTask.TRANSACTION, ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD);

            @SuppressWarnings("unchecked")
            Callable<Transaction> task = contextSvc.createContextualProxy(new Callable<Transaction>() {
                @Override
                public Transaction call() throws SystemException {
                    return tranMgr.getTransaction();
                }
            }, execProps, Callable.class);

            Transaction tran2 = task.call();
            System.out.println("during task: " + tran2);
            if (!tran1.equals(tran2))
                throw new Exception("Contextual task without transactionContext configured should run in same transaction " + tran1 + ", not " + tran2);

            Transaction tran3 = tranMgr.getTransaction();
            System.out.println("after task: " + tran3);
            if (!tran1.equals(tran3))
                throw new Exception("Should have same transaction " + tran1 + " on thread after running contextual task without transactionContext configured. Instead: " + tran3);
        } finally {
            bundleContext.ungetService(tranMgrRef);
            tran.commit();
        }
    }

    /**
     * Use the SKIP_CONTEXT_PROVIDERS execution property to skip the transaction context provider.
     *
     * @param contextSvcName the context service to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testSkipTransactionContext(String contextSvcName, PrintWriter out) throws Exception {
        ContextService contextSvc = (ContextService) new InitialContext().lookup(contextSvcName);

        tran.begin();
        try {
            Map<String, String> execPropsSkipTransactionContext = new TreeMap<String, String>();
            execPropsSkipTransactionContext.put(WSContextService.SKIP_CONTEXT_PROVIDERS, "com.ibm.ws.transaction.context.provider");
            execPropsSkipTransactionContext.put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND); // should be ignored because transaction context provider is skipped

            Callable<?> task = contextSvc.createContextualProxy(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    tran.begin();
                    tran.commit();
                    return null;
                }
            }, execPropsSkipTransactionContext, Callable.class);

            try {
                task.call();
                throw new Exception("If transaction context provider is skipped, a second tran.begin should not be permitted.");
            } catch (NotSupportedException x) {
            } // expect nested transaction to be rejected because transaction context (new LTC) was not applied
        } finally {
            tran.commit();
        }
    }

    /**
     * Verify that a contextual task runs in an LTC.
     *
     * @param contextSvcName the context service to use.
     * @param out PrintWriter for servlet response
     * @throws Exception if it fails.
     */
    public void testTransactionContext(String contextSvcName, PrintWriter out) throws Exception {
        BundleContext bundleContext = FrameworkUtil.getBundle(WSContextService.class).getBundleContext();
        ServiceReference<TransactionManager> tranMgrRef = bundleContext.getServiceReference(TransactionManager.class);
        ContextService contextSvc = (ContextService) new InitialContext().lookup(contextSvcName);

        tran.begin();
        try {
            final TransactionManager tranMgr = bundleContext.getService(tranMgrRef);

            Transaction tran1 = tranMgr.getTransaction();
            System.out.println("before task: " + tran1);

            @SuppressWarnings("unchecked")
            Callable<Transaction> task = contextSvc.createContextualProxy(new Callable<Transaction>() {
                @Override
                public Transaction call() throws Exception {
                    Transaction transaction = tranMgr.getTransaction();
                    UserTransaction userTran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                    userTran.begin();
                    userTran.commit();
                    return transaction;
                }
            }, Callable.class);

            Transaction tran2 = task.call();
            System.out.println("during task: " + tran2);
            if (tran2 != null)
                throw new Exception("Contextual task configured with transactionContext should run in an LTC, not " + tran2 + ". Original transaction was " + tran1);

            Transaction tran3 = tranMgr.getTransaction();
            System.out.println("after task: " + tran3);
            if (!tran1.equals(tran3))
                throw new Exception("Should have same transaction " + tran1 + " on thread after running contextual task with transactionContext. Instead: " + tran3);
        } finally {
            bundleContext.ungetService(tranMgrRef);
            tran.commit();
        }
    }
}
