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
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

@WebServlet("/*")
public class PersistentExecutorsTestServlet extends HttpServlet {
    private static final long serialVersionUID = 915174288591951694L;

    static final LinkedBlockingQueue<Integer> lbqResult = new LinkedBlockingQueue<Integer>();

    /**
     * Interval in milliseconds between polling for task results.
     */
    private static final long POLL_INTERVAL = 500;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     * This allows for a 27.2 minute delay that has been observed at least once in the build infrastructure.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(30);

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        String invoker = request.getParameter("invokedBy");
        PrintWriter out = response.getWriter();

        try {
            out.println(getClass().getSimpleName() + " is starting " + test + "<br>");
            System.out.println("-----> " + test + "(invoked by " + invoker + ") starting");
            getClass().getMethod(test, HttpServletRequest.class, PrintWriter.class).invoke(this, request, out);
            System.out.println("<----- " + test + "(invoked by " + invoker + ") successful");
            out.println(test + " " + SUCCESS_MESSAGE);
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + test + "(invoked by " + invoker + ") failed:");
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
     * Removes a task
     */
    public void testRemoveTask(HttpServletRequest request, PrintWriter out) throws Exception {
        boolean removed = removeTask(request, out);
        if (!removed) {
            throw new Exception("Task " + request.getParameter("taskId") + " could not be removed.");
        }
    }

    public boolean removeTask(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        String taskIdStr = request.getParameter("taskId");

        if ((taskIdStr == null) || taskIdStr.length() == 0) {
            return false;
        }
        long taskId = Long.parseLong(taskIdStr);

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        return executor.remove(taskId);
    }

    /**
     * Schedule a basic Runnable using the ScheduledExecutorService reference we looked up.
     * This reference should point to the configured persistentExecutor.
     * The result is stored in a static queue.
     * 
     * @throws Exception
     */
    public void scheduleASimpleTaskNoDatabaseExecution(HttpServletRequest request, PrintWriter out) throws Exception {
        tidyupResultQueue();
        Runnable runnable = new NoDbOpTestTask(lbqResult);
        String jndiName = request.getParameter("jndiName");
        long initialDelay = Long.parseLong(request.getParameter("initialDelay"));
        //long interval = Long.parseLong(request.getParameter("interval"));

        out.println("jndiName " + jndiName + ".");

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        TaskStatus<?> status = executor.schedule(runnable, initialDelay, TimeUnit.MILLISECONDS);
        long taskId = status.getTaskId();
        out.println("Task id is " + taskId + ".");
    }

    /**
     * Verifies that multiple tasks have not run.
     */
    public void testTasksHaveNotRun(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        String[] taskIds = request.getParameterValues("taskId");

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        @SuppressWarnings("unchecked")
        TaskStatus<Integer>[] status = new TaskStatus[taskIds.length];
        for (int i = 0; i < taskIds.length; i++) {
            status[i] = executor.getStatus(Long.parseLong(taskIds[i]));

            if (status[i] == null || status[i].hasResult())
                throw new Exception("Task was called within alotted interval. status: " + status[i]);
        }
    }

    /**
     * Verifies that a task runs.
     */
    public void testTaskDoesRun(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        TaskStatus<?> status = null;
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL)) {
            status = executor.getStatus(taskId);
            if (status == null || status.hasResult())
                return;
        }

        throw new Exception("Task " + taskId + " did not complete any executions within allotted interval. " + status);
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
