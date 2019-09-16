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
package web;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedTask;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

@WebServlet("/*")
public class PersistentExecutorConfigUpdateTestServlet extends HttpServlet {
    private static final long serialVersionUID = 915174288591951694L;

    /**
     * Interval for polling task status.
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(30);

    @Resource(name = "java:module/env/jdbc/persistcfgdbRef", lookup = "jdbc/persistcfgdb")
    private DataSource ds;

    @Resource
    private UserTransaction tran;

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
     * Cancels tasks based on name pattern and state.
     */
    public void testCancelTasks(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        String pattern = request.getParameter("pattern");
        TaskState state = TaskState.valueOf(request.getParameter("state"));
        boolean inState = !Boolean.FALSE.toString().equalsIgnoreCase(request.getParameter("inState"));
        int expected = Integer.parseInt(request.getParameter("numCancelsExpected"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        int count = executor.cancel(pattern, '\\', state, inState);

        if (count < expected)
            throw new Exception("Expecting " + expected + " tasks to be canceled, but only " + count + " were canceled");
    }

    /**
     * Removes a task
     */
    public void testRemoveTask(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        boolean removed = executor.remove(taskId);
        if (!removed)
            throw new Exception("Task " + taskId + " could not be removed.");
    }

    /**
     * Schedules a repeating task. The task id is written to the servlet output
     */
    public void testScheduleRepeatingTask(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long initialDelay = Long.parseLong(request.getParameter("initialDelay"));
        long interval = Long.parseLong(request.getParameter("interval"));
        String taskName = request.getParameter("taskName");

        FixedDelayTriggerTask triggerTask = new FixedDelayTriggerTask(initialDelay, interval);
        if (taskName != null)
            triggerTask.getExecutionProperties().put(ManagedTask.IDENTITY_NAME, taskName);

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        TaskStatus<Integer> status = executor.schedule(triggerTask, triggerTask);
        long taskId = status.getTaskId();
        out.println("Task id is " + taskId + ".");
    }

    /**
     * Verifies that a task fails to look up a resource reference.
     */
    public void testTaskFailsWithLookupError(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        TaskStatus<Integer> status = executor.getStatus(taskId);
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && !status.hasResult(); status = executor.getStatus(taskId))
            Thread.sleep(POLL_INTERVAL);
        if (!status.hasResult())
            throw new Exception("Task did not complete any executions within alotted interval. " + status);

        try {
            Integer result = status.getResult();
            throw new Exception("Task should not complete any executions. Current result: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof NamingException))
                throw x;
        }

        String expectedNumAttempts = request.getParameter("expectedNumAttempts");
        if (expectedNumAttempts != null) {
            int expectedAttempts = Integer.parseInt(expectedNumAttempts);

            int numAttempts = FixedDelayTriggerTask.inMemoryAttemptCounter.get();
            if (numAttempts != expectedAttempts)
                throw new Exception("Expecting task to be tried " + expectedAttempts + " times before stopping due to failure. Instead " + numAttempts);
        }
    }

    /**
     * Verifies that a task did not run yet.
     */
    public void testTaskDidNotRun(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        TaskStatus<Integer> status = executor.getStatus(taskId);
        if (status.hasResult())
            throw new Exception("Unexpected result for task " + status + ". Result is " + status.getResult());
    }

    /**
     * Verifies that a repeating task continues to run periodically.
     */
    public void testTaskIsRunning(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));

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
    }

    /**
     * Verifies that multiple repeating tasks continue to run periodically.
     */
    public void testTasksAreRunning(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        String[] taskIds = request.getParameterValues("taskId");

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        @SuppressWarnings("unchecked")
        TaskStatus<Integer>[] status = new TaskStatus[taskIds.length];
        int[] results = new int[taskIds.length], newResults = new int[taskIds.length];
        for (int i = 0; i < taskIds.length; i++) {
            status[i] = executor.getStatus(Long.parseLong(taskIds[i]));
            for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && !status[i].hasResult(); status[i] = executor.getStatus(Long.parseLong(taskIds[i])))
                Thread.sleep(POLL_INTERVAL);
            if (!status[i].hasResult())
                throw new Exception("Task did not complete any executions within alotted interval. " + status[i]);

            newResults[i] = results[i] = status[i].getResult();
        }

        boolean allTasksHaveNewResults = false;
        for (long start = System.nanoTime(); !allTasksHaveNewResults && System.nanoTime() - start < TIMEOUT_NS;) {
            Thread.sleep(POLL_INTERVAL);
            allTasksHaveNewResults = true;
            for (int i = 0; i < taskIds.length; i++)
                if (results[i] == newResults[i]) {
                    status[i] = executor.getStatus(status[i].getTaskId());
                    allTasksHaveNewResults &= results[i] != (newResults[i] = status[i].getResult());
                }
        }

        if (!allTasksHaveNewResults)
            throw new Exception("Did not see new result for repeating task within allotted interval. Original results: "
                                + Arrays.toString(results) + ", New Results: " + Arrays.toString(newResults) + " Status: " + Arrays.toString(status));
    }
}
