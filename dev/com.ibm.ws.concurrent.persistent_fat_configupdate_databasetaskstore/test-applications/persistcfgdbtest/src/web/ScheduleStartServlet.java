/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.PersistentStoreException;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

@WebServlet("/*")
public class ScheduleStartServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = -8767333685964217900L;

    /**
     * Interval for polling task status.
     */
    private static final long POLL_INTERVAL = 200;

    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(30);

    @Resource(lookup = "concurrent/myScheduler")
    private PersistentExecutor scheduler;

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        String invoker = request.getParameter("invokedBy");
        PrintWriter out = response.getWriter();

        try {
            out.println(" is starting " + test + "<br>");
            System.out.println("-----> " + test + "(invoked by " + invoker + ") starting " + request.getQueryString());
            getClass().getMethod(test, HttpServletRequest.class, PrintWriter.class).invoke(this, request, out);
            out.println(test + " " + SUCCESS_MESSAGE);
            System.out.println("<----- " + test + "(invoked by " + invoker + ") successful");
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

    /*
     * Schedule a task. Server is disabled for task execution.
     * This task should stay in the DB.
     * 
     * @param HttpServletRequest
     * HTTP request
     * 
     * @param HttpServletResponse
     * HTTP response
     */
    public void testScheduledPersistedTaskStart(HttpServletRequest request, PrintWriter out) throws Exception {
        //create a persisted task 
        TaskStatus<String> scheduledTask = scheduler.schedule(new SimpleTask(), 0, TimeUnit.SECONDS);
        System.out.println("Scheduled task " + scheduledTask.getTaskId());
        // write the task ID out to the http connection. 
        out.print("Scheduled Task id is " + scheduledTask.getTaskId() + ". COMPLETED SUCCESSFULLY ");
    }

    /*
     * check for the existence of a persisted task using the given task ID.
     * 
     * @param HttpServletRequest
     * HTTP request
     * 
     * @param HttpServletResponse
     * HTTP response
     * 
     * @param String
     * Task ID for task scheduled.
     */
    @FFDCIgnore(PersistentStoreException.class)
    public void testCheckPersistedAvailable(HttpServletRequest request, PrintWriter out) throws Exception {
        long taskId = Long.parseLong(request.getParameter("taskId"));
        try {
            // get task based on the task ID passed in
            TaskStatus<String> persistedTaskStatus = scheduler.getStatus(taskId);
            // Can we find Status for the task??
            if (persistedTaskStatus != null) {
                System.out.println("Retrived Task Id = " + persistedTaskStatus.getTaskId());
                out.print("Scheduled Task id is " + persistedTaskStatus.getTaskId() + ". .COMPLETED SUCCESSFULLY");
                // clean up task. 
                scheduler.remove(new Long(taskId));
            } else {
                out.print("Scheduled Task id is NOTFOUND..COMPLETED SUCCESSFULLY");
            }
        } catch (RuntimeException p) {
            if (p.getMessage().indexOf("08004") > 0) {
                out.print("Scheduled Task id is ACCESSDENIED.COMPLETED SUCCESSFULLY");
                System.out.println("Error accessing task " + p.toString());
            } else
                throw p;
        }
    }

    /**
     * Verify that we cannot find a task with the specified name
     */
    public void testCannotFindNamedTask(HttpServletRequest request, PrintWriter out) throws Exception {
        String name = request.getParameter("name");

        List<TaskStatus<?>> statusList = scheduler.findTaskStatus(name, null, TaskState.ANY, true, null, null);

        if (!statusList.isEmpty())
            throw new Exception("Should not be able to find task(s) " + statusList);
    }

    /**
     * Verify that we cannot find a task with the specified id
     */
    public void testCannotFindTask(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        TaskStatus<?> status = executor.getStatus(taskId);

        if (status != null)
            throw new Exception("Should not be able to find task " + status);
    }

    /**
     * Verify that we can find a task with the specified id
     */
    public void testFindTask(HttpServletRequest request, PrintWriter out) throws Exception {

        long taskId = Long.parseLong(request.getParameter("taskId"));

        TaskStatus<?> status = scheduler.getStatus(taskId);

        if (status == null)
            throw new Exception("Unable to find task " + taskId);
    }

    /**
     * Schedule a task and verify that it runs.
     */
    public void testScheduleAndRunTask(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        TaskStatus<Integer> status = executor.submit(new CountingTask());
        Long taskId = status.getTaskId();
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && status != null; status = executor.getStatus(taskId))
            Thread.sleep(POLL_INTERVAL);
        if (status != null)
            throw new Exception("Task did not complete with allotted interval " + status);
    }

    /**
     * Schedule a task that has a unique name.
     */
    public void testScheduleNamedTask(HttpServletRequest request, PrintWriter out) throws Exception {
        String name = request.getParameter("name");
        TaskStatus<String> status = scheduler.submit(new SimpleTask(name));
        System.out.println("Scheduled task " + status);
        out.print("Task id is " + status.getTaskId() + ". " + SUCCESS_MESSAGE);
    }

    /**
     * Schedule a repeating task and log the task id in the servlet output.
     */
    public void testScheduleRepeatingTask(HttpServletRequest request, PrintWriter out) throws Exception {
        TaskStatus<Integer> status = scheduler.schedule(new CountingTask(), new AlternatingTrigger());
        System.out.println("Scheduled task " + status);
        out.print("Task id is " + status.getTaskId() + ". " + SUCCESS_MESSAGE);
    }

    /**
     * Attempt to schedule a task and expect failure due to missing database tables.
     */
    public void testScheduleTaskWithMissingTables(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        try {
            TaskStatus<Integer> status = executor.schedule(new CountingTask(), 4, TimeUnit.MINUTES);
            throw new Exception("Should not be able to submit task because database tables should not exist " + status);
        } catch (RuntimeException x) {
            if (!"javax.persistence.PersistenceException".equals(x.getClass().getName())
                || x.getMessage() == null
                || !x.getMessage().contains("PX2PART"))
                throw x;
        }
    }

    /**
     * Verifies that a repeating task continues to run periodically.
     */
    public void testTaskIsRunning(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));
        boolean cancel = Boolean.parseBoolean(request.getParameter("cancel"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        TaskStatus<Integer> status = executor.getStatus(taskId);
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && !status.hasResult(); status = executor.getStatus(taskId))
            Thread.sleep(POLL_INTERVAL);
        if (!status.hasResult())
            throw new Exception("Task did not complete any executions within alotted interval. " + status);

        int result1 = status.getResult();
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && result1 == status.getResult(); status = executor.getStatus(taskId))
            Thread.sleep(POLL_INTERVAL);

        int result2 = status.getResult();
        if (result1 == result2)
            throw new Exception("Did not see new result for repeating task within allotted interval. Result: " + result1 + ", Status: " + status);

        if (cancel)
            if (!status.cancel(true))
                throw new Exception("Unable to cancel task with last known status of " + status);
    }
}
