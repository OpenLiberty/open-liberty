/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.feature;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/persistent")
public class ConcurrentPersistentFeatureServlet extends HttpServlet {

    static final LinkedBlockingQueue<Integer> lbqResult = new LinkedBlockingQueue<Integer>();
    
    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Resource(lookup = "concurrent/executorService")
    private ScheduledExecutorService executorService;

    @Resource(lookup = "test/TaskStoreTester")
    // TODO how can we make this class accessible to the application at compile time?
    //private TaskStoreTester taskStoreTester;
    private Object taskStoreTester;

    /**
     * Entry point for a GET request. The test name is passed as  
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();

        try {
            out.println("ContextSerializeServlet is starting " + test + "<br>");
            System.out.println("-----> " + test + " starting");
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
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
     * Validates that an application cannot load classes such as:
     * - javax.enterprise.concurrent.ManagedScheduledExecutorService
     * - com.ibm.websphere.concurrent.persistent.PersistentExecutor
     * because concurrent-1.0 or persistentExecutor-1.0 are not enabled.
     */
    public void validateAppInabilityToAccessConcurrentPersistentArtifacts(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	try {
    	    Class.forName("javax.enterprise.concurrent.ManagedScheduledExecutorService");
    	    throw new Exception("ManagedScheduledExecutorService should not have been found. Feature concurrent-1.0 is not enabled.");
    	} catch(ClassNotFoundException cnfe) {
    		// Expected.
    	}
    	
    	try {
    	    Class.forName("com.ibm.websphere.concurrent.persistent.PersistentExecutor");
    	    throw new Exception("PersistentExecutor should not have been found. Feature persistentExecutor-1.0 is not enabled.");
    	} catch(ClassNotFoundException cnfe) {
    		// Expected.
    	}
    }
    
    /**
     * Schedule a basic callable using the ScheduledExecutorService reference.
     * This reference should point to the configured persistentExecutor. Therefore, a call to
     * future.get(timeToWait) should return an IllegalStateException for the initial snapshot of the task.
     * The result is stored in a static queue.
     * 
     * @throws Exception
     */
    public void testCallToFuterGetWithWaitTimeThrowsAUnsupportedOperationException(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	tidyupResultQueue();
    	Callable<Integer> callable = new NoDbOpTestTask(lbqResult);
        ScheduledFuture<Integer> future = executorService.schedule(callable, 0, TimeUnit.MILLISECONDS);
        
        try {
            future.get(10, TimeUnit.SECONDS);
            throw new Exception("An IllegalStateException should have been thrown when ScheduledFuture.get(timeToWait) is called.");
        } catch(IllegalStateException x) {
        	// Expected.
        }

        Integer result = lbqResult.poll(30, TimeUnit.SECONDS);
        if (result == null || result != 1000) {
        	throw new Exception("The result should have been 1000. Instead it was: " + result);
        }

    }

    /**
     * Have multiple threads invoke TaskStore.findOrCreate at the same time.
     */
    public void testFindOrCreate(HttpServletRequest request, HttpServletResponse response) throws Exception {
        taskStoreTester.getClass().getMethod("testFindOrCreate").invoke(taskStoreTester);
    }

    /**
     * Schedule a basic Runnable using the ScheduledExecutorService reference we looked up. 
     * This reference should point to the configured persistentExecutor. 
     * The result is stored in a static queue.
     * 
     * @throws Exception
     */
    public void scheduleASimpleTaskNoDatabaseExecution(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	tidyupResultQueue();
    	Runnable runnable = new NoDbOpTestTask(lbqResult);
    	executorService.schedule(runnable, 0, TimeUnit.MILLISECONDS);
        Integer result = lbqResult.poll(30, TimeUnit.SECONDS);
        if (result == null || result != 1000) {
        	throw new Exception("The result should have been 1000. Instead it was: " + result);
        }
    }
    
    /**
     * Cleans up the result queue.
     * 
     * @throws Exception
     */
    void tidyupResultQueue() throws Exception {
    	lbqResult.clear();
    	if (lbqResult.size() != 0)
    		throw new Exception("The result queue was not cleared. Queue: " + lbqResult);
    }
}
