/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.test.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

@WebServlet("/schedtest")
public class ScheduleStartServlet extends HttpServlet {

    private static final long serialVersionUID = -8767333685964217900L;

    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    private final long TIMEOUT = TimeUnit.SECONDS.toNanos(30); // 30 seconds in nanoseconds
    private final int POLL_INTERVAL = 200; // .2 seconds in milliseconds
    private final int DELAY = 500; // delay units. 

    @Resource(lookup = "concurrent/myScheduler")
    private PersistentExecutor scheduler;

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        String taskId = request.getParameter("taskId");
        PrintWriter out = response.getWriter();

        try {
            out.println(" is starting " + test + "<br>");
            System.out.println("-----> " + test + " starting");
            if (taskId == null) {
                //use this when starting a scheduled task 
                System.out.println("TaskId is null");
                getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            } else {
                // use this when checking the status of a scheduled task
                System.out.println("TaskId is " + taskId);
                getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class, String.class).invoke(this, request, response, taskId);
                out.println(test + " " + SUCCESS_MESSAGE);
            }
            System.out.println("<----- " + test + " successful");
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

    public void testScheduledPersistedTaskStart(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        //create a persisted task 
        TaskStatus<String> scheduledTask = scheduler.schedule(new SimpleTask(), 0, TimeUnit.SECONDS);
        System.out.println("current system time[" + System.nanoTime() + "]");
        System.out.println("Scheduled task " + scheduledTask.getTaskId());
        // write the task ID out to the http connection. 
        resp.getWriter().print("Scheduled Task ID = " + scheduledTask.getTaskId());
    }

    /*
     * Schedule a task and ensure it completes successfully in this instance of the server
     * Check to make sure results are persisted across a server restart
     * 
     * @param HttpServletRequest
     * HTTP request
     * 
     * @param HttpServletResponse
     * HTTP response
     */
    public void testScheduledPersistedTaskResults(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        //create a persisted task
        TaskStatus<String> scheduledTask = scheduler.schedule(new SimpleTask(), DELAY, TimeUnit.MICROSECONDS);
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT; Thread.sleep(POLL_INTERVAL)) {
            try {
                if (scheduledTask.isDone()) {
                    break;
                }
            } catch (IllegalStateException ie) {
                // Do nothing still not done. 
            }
            long finalTime = start + TIMEOUT;
            System.out.println("end time in Nano's " + finalTime);
            System.out.println("start time in nano's " + start);
            System.out.println("Current System nano's " + System.nanoTime());
            scheduledTask = scheduler.getStatus(scheduledTask.getTaskId());
        }
        // Make sure task completed successfully 
        //
        try {
            if (scheduledTask.isDone()) {
                // write the task ID out to the http connection. 
                System.out.println("Scheduled task " + scheduledTask.getTaskId());
                resp.getWriter().print("Scheduled Task ID = " + scheduledTask.getTaskId());
            }
        } catch (IllegalStateException ie) {
            System.out.println("Error task did not complete in the allotted time ");
            resp.getWriter().print("test failed");
        }

    }

    /*
     * check the status of a persisted task using the given task ID.
     * 
     * @param HttpServletRequest
     * HTTP request
     * 
     * @param HttpServletResponse
     * HTTP response
     * 
     * @param String
     * Task ID for task scheduled before server restart.
     */
    public void testCheckPersistedTaskStatus(HttpServletRequest req, HttpServletResponse resp, String taskId) throws Exception {

        String result = "failed";
        // get task based on the task ID passed in
        TaskStatus<String> persistedTaskStatus = scheduler.getStatus(new Long(taskId));
        // Make sure you have a TaskStatus
        if (persistedTaskStatus != null) {
            try {
                // Wait till the task has completed or timeout has been exceeded
                for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT; Thread.sleep(POLL_INTERVAL)) {
                    try {
                        if (persistedTaskStatus.isDone()) {
                            break;
                        }
                    } catch (IllegalStateException ie) {
                        // Do nothing still not done. 
                    }
                    long finalTime = start + TIMEOUT;
                    System.out.println("end time in Nano's " + finalTime);
                    System.out.println("start time in nano's " + start);
                    System.out.println("Current System nano's " + System.nanoTime());
                    persistedTaskStatus = scheduler.getStatus(persistedTaskStatus.getTaskId());
                }
                try {
                    // if task is done return success message returned from the task
                    if (persistedTaskStatus.isDone()) {
                        result = persistedTaskStatus.get();
                        // Make sure the message is correct if not 
                        // throw an exception
                        if (!result.equals(SUCCESS_MESSAGE))
                            throw new Exception("Message for Task is unexpected, received " + result);
                        else {
                            // log successful completion
                            System.out.println("Task completed successfully: result = " + result);
                        }
                    }

                } catch (IllegalStateException ie) {
                    // task did not complete before the timeout
                    throw new Exception(" Task " + taskId + " Did not complete in the allotted time. Task Status = " + persistedTaskStatus.toString());
                }
            } finally {
                // clean up task from DB
                scheduler.remove(new Long(taskId));
            }
        } else {
            // unable to get a task status object for the given task id
            throw new Exception("Scheduler returned a null task status for taskID " + taskId);
        }
        // Write the string results to output stream
        resp.getWriter().print(result);

    }

    /*
     * Schedule a task with no delay that is running when the server is shutdown.
     * 
     * @param HttpServletRequest
     * HTTP request
     * 
     * @param HttpServletResponse
     * HTTP response
     */
    public void testScheduledPersistedLongRunningTask(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        //create a persisted task
        TaskStatus<String> scheduledTask = scheduler.schedule(new LongRunningTask("testScehduledPersistedLongRunningTask"), 0, TimeUnit.MICROSECONDS);
        // Make sure task completed successfully 
        //
        System.out.println("Scheduled task " + scheduledTask.getTaskId());
        resp.getWriter().print("Scheduled Task ID = " + scheduledTask.getTaskId());

    }

}
