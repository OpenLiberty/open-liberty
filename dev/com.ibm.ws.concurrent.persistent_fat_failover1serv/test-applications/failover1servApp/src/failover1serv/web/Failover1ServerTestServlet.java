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

import java.util.Arrays;
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
     * Verifies that multiple repeating tasks continue to run periodically.
     */
    public void testTasksAreRunning(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        String[] taskIds = request.getParameterValues("taskId");

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        long start1 = System.nanoTime();
        @SuppressWarnings("unchecked")
        TaskStatus<Integer>[] statusWhenFirstComplete =
                Arrays.stream(taskIds, 0, taskIds.length)
                      .map(str -> Long.parseLong(str))
                      .map(taskId -> {
                          try {
                              TaskStatus<Integer> status = null;
                              for (; status == null || System.nanoTime() - start1 < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL_MS))
                                  if ((status = executor.getStatus(taskId)).hasResult())
                                      return status;
                              throw new RuntimeException("Task " + status + " did complete any executions within the allotted interval.");
                          } catch (InterruptedException x) {
                              throw new RuntimeException(x);
                          }
                      })
                      .toArray(TaskStatus[]::new);

        Integer[] results1 = new Integer[taskIds.length];
        Integer[] results2 = new Integer[taskIds.length];
        for (int i = 0; i < statusWhenFirstComplete.length; i++) {
            results1[i] = statusWhenFirstComplete[i].getResult();
            results2[i] = executor.<Integer>getStatus(statusWhenFirstComplete[i].getTaskId()).getResult();
        }

        boolean anySame = true;
        for (long start2 = System.nanoTime(); System.nanoTime() - start2 < TIMEOUT_NS && anySame; Thread.sleep(POLL_INTERVAL_MS)) {
            anySame = false;
            for (int i = 0; i < results1.length && !anySame; i++)
                anySame |= results1[i].equals(results2[i])
                        && results1[i].equals(results2[i] = executor.<Integer>getStatus(statusWhenFirstComplete[i].getTaskId()).getResult());
        }

        if (anySame)
            throw new Exception("Did not see new result for one or more of the repeating tasks within the allotted interval. " +
                                "Initial results: " + Arrays.asList(results1) + "; Later results: " + Arrays.asList(results2));
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
