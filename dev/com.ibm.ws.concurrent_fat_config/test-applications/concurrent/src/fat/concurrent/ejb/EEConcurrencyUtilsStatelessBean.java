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
package fat.concurrent.ejb;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;

/**
 * Test cases that run from an EJB
 */
@Stateless
public class EEConcurrencyUtilsStatelessBean {
    /**
     * Maximum number of milliseconds to wait for a task to finish.
     */
    private static final long TIMEOUT = 30000;

    @Resource(lookup = "concurrent/execSvc1")
    private ExecutorService execSvc1;

    /**
     * Verify that a managed task runs with access to the application component namespace.
     *
     * @throws Exception if it fails.
     */
    public void testJEEMetadataContextExecSvc1() throws Exception {

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
        Future<BlockingQueue<Object>> future = execSvc1.submit(javaCompLookup, results);
        try {
            result = future.get(TIMEOUT, TimeUnit.MILLISECONDS).remove();
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
     * Verify that a managed task runs without access to the application component namespace.
     *
     * @throws Exception if it fails.
     */
    public void testNoJEEMetadataContextExecSvc1() throws Exception {

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
        Future<BlockingQueue<Object>> future = execSvc1.submit(javaCompLookup, results);
        try {
            result = future.get(TIMEOUT, TimeUnit.MILLISECONDS).remove();
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
            throw new Exception("jeeMetadataContext should not be available from managedExecutorService thread. Value is: " + result);
    }
}
