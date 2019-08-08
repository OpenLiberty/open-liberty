/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package failover1serv.web;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/Failover1ServerTestServlet")
public class Failover1ServerTestServlet extends FATServlet {
    private static final long POLL_INTERVAL_MS = 500;

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    @Resource
    private UserTransaction tx;

    /**
     * Cancel a task so that no subsequent executions occur.
     */
    public void testCancelTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        executor.getStatus(taskId).cancel(false);
    }

    /**
     * Schedules a one-time task. The task id is written to the servlet output
     */
    public void testScheduleOneTimeTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long initialDelayMS = Long.parseLong(request.getParameter("initialDelayMS"));
        String testIdentifier = request.getParameter("test");

        IncTask task = new IncTask(testIdentifier);

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        TaskStatus<Integer> status = executor.schedule(task, initialDelayMS, TimeUnit.MILLISECONDS);
        long taskId = status.getTaskId();

        response.getWriter().println("Task id is " + taskId + ".");
    }

    /**
     * Schedules a one-time task. The task id is written to the servlet output
     */
    public void testScheduleRepeatingTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long initialDelayMS = Long.parseLong(request.getParameter("initialDelayMS"));
        long delayMS = Long.parseLong(request.getParameter("delayMS"));
        String testIdentifier = request.getParameter("test");

        IncTask task = new IncTask(testIdentifier);

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        TaskStatus<Integer> status = executor.schedule(task, new DelayTrigger(initialDelayMS, delayMS));
        long taskId = status.getTaskId();

        response.getWriter().println("Task id is " + taskId + ".");
    }

    /**
     * Confirms that a task has completed and verifies the result.
     */
    public void testTaskCompleted(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));
        int expected = Integer.parseInt(request.getParameter("expectedResult"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        TaskStatus<Integer> status;
        for (long start = System.nanoTime(); (status = executor.getStatus(taskId)).getNextExecutionTime() != null && System.nanoTime() - start < TIMEOUT_NS; )
            Thread.sleep(POLL_INTERVAL_MS);

        assertEquals(Integer.valueOf(expected), status.get());
    }

    /**
     * Verifies that a repeating task continues to run periodically.
     */
    public void testTaskIsRunning(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        TaskStatus<Integer> status = executor.getStatus(taskId);
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && !status.hasResult(); status = executor.getStatus(taskId))
            Thread.sleep(POLL_INTERVAL_MS);
        if (!status.hasResult())
            throw new Exception("Task did not complete any executions within allotted interval. " + status);

        int result1 = status.getResult();
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && result1 == status.getResult(); status = executor.getStatus(taskId))
            Thread.sleep(POLL_INTERVAL_MS);

        int result2 = status.getResult();
        if (result1 == result2)
            throw new Exception("Did not see new result for repeating task within allotted interval. Result: " + result1 + ", Status: " + status);
    }

    /**
     * Verifies that a repeating task has run at least once.
     */
    public void testTaskStarted(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        TaskStatus<Integer> status = executor.getStatus(taskId);
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && !status.hasResult(); status = executor.getStatus(taskId))
            Thread.sleep(POLL_INTERVAL_MS);
        if (!status.hasResult())
            throw new Exception("Task did not complete any executions within allotted interval. " + status);
    }
}
