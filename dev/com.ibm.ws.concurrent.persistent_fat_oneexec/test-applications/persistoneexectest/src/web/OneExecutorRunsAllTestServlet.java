/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
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
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.annotation.Resource;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;
import com.ibm.ws.concurrent.persistent.ejb.TimerStatus;
import com.ibm.ws.concurrent.persistent.ejb.TimerTrigger;
import com.ibm.ws.concurrent.persistent.ejb.TimersPersistentExecutor;

@WebServlet("/*")
public class OneExecutorRunsAllTestServlet extends HttpServlet {
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
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(90);

    @Resource
    private UserTransaction tran;

    MBeanServer mbs;

    /**
     * Utility method to print the contents of a list of object arrays.
     */
    private static final String deepToString(String[][] list) {
        String str = "{";
        for (String[] entry : list)
            str += Arrays.toString(entry);
        str += "}";
        return str;
    }

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
            System.out.println("-----> " + test + "(invoked by " + invoker + ") starting " + request.getQueryString());
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
     * Initialize the scheduler tables and a table used by the application
     */
    @Override
    public void init() throws ServletException {
        mbs = ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * Simulate a scenario where we need to prevent deadlocks with EJB singletons
     */
    public void testEJBSingletonDeadlockPrevention(HttpServletRequest request, PrintWriter out) throws Exception {
        Class<?> SingletonTriggerTask = Class.forName("test.feature.ejb.singleton.SingletonTriggerTask");
        @SuppressWarnings("unchecked")
        LinkedBlockingQueue<Object> results = (LinkedBlockingQueue<Object>) SingletonTriggerTask.getField("results").get(null);
        Lock singletonLock = (Lock) SingletonTriggerTask.getField("singletonLock").get(null);
        TimerTrigger triggerTask = (TimerTrigger) SingletonTriggerTask.newInstance();

        singletonLock.lock();
        try {
            TimersPersistentExecutor executor = (TimersPersistentExecutor) new InitialContext().lookup("concurrent/executorC");
            TaskStatus<?> status = executor.schedule(triggerTask);

            Thread.sleep(2000); // encourage the task to lock the database first if it can

            // Wait for task completion
            status = executor.getStatus(status.getTaskId());
            if (status == null)
                throw new Exception("Task should not be able to complete and autopurge while main thread still holds the singleton lock.");
            if (status.hasResult())
                status.get();
        } finally {
            singletonLock.unlock();
        }

        Object result = results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        if (result == null)
            throw new Exception("Task execution attempt did not complete within the allotted interval.");
        else if (result instanceof Throwable)
            throw new Exception("Task execution attempt failed. See cause.", (Throwable) result);
    }

    /**
     * Test interfaces for find and remove that we provide to EJB container for EJB persistent timers.
     */
    public void testEJBTimersFindAndRemove(HttpServletRequest request, PrintWriter out) throws Exception {
        TimersPersistentExecutor executor = (TimersPersistentExecutor) new InitialContext().lookup("concurrent/executorC");

        // schedule some tasks
        NamedTriggerTask triggerTaskA = new NamedTriggerTask("testEJBTimersFindAndRemove#beanA", TimeUnit.HOURS.toMillis(10));
        TaskStatus<?> statusA = executor.schedule(triggerTaskA, triggerTaskA);

        NamedTriggerTask triggerTaskB1 = new NamedTriggerTask("testEJBTimersFindAndRemove#bean_B1", TimeUnit.HOURS.toMillis(21));
        TaskStatus<?> statusB1 = executor.schedule(triggerTaskB1, triggerTaskB1);

        NamedTriggerTask triggerTaskB2 = new NamedTriggerTask("testEJBTimersFindAndRemove#bean_B2", TimeUnit.HOURS.toMillis(22));
        TaskStatus<?> statusB2 = executor.schedule(triggerTaskB2, triggerTaskB2);

        // remove nothing
        int count = executor.removeTimers("anotherApp", "testEJBTimersFindAndRemove#%", '\\', TaskState.ANY, true);
        if (count != 0)
            throw new Exception("Shouldn't be anything to remove. Instead removed " + count);

        // find nothing
        List<TimerStatus<?>> results = executor.findTimerStatus("anotherApp", "testEJBTimersFindAndRemove#%", '\\', TaskState.ANY, true, null, 500);
        if (!results.isEmpty())
            throw new Exception("Non-empty result for query that shouldn't find anything: " + results);

        // find tasks that start with testEJBTimersFindAndRemove#bean_B
        results = executor.findTimerStatus("persistoneexectest", "testEJBTimersFindAndRemove#bean\\_B_", '\\', TaskState.UNATTEMPTED, true, -1000l, 10);
        if (results.size() != 2)
            throw new Exception("Unexpected number of tasks that start with testEJBTimersFindAndRemove#bean_B: " + results);
        TaskStatus<?> status1 = results.get(0);
        if (!statusB1.equals(status1) && !statusB2.equals(status1))
            throw new Exception("First result does not match: " + status1);
        TaskStatus<?> status2 = results.get(1);
        if (!statusB1.equals(status2) && !statusB2.equals(status2))
            throw new Exception("Second result does not match: " + status2);
        if (status1.equals(status2))
            throw new Exception("Results should not match eachother: " + status1 + " " + status2);

        // find all of the tasks
        results = executor.findTimerStatus("persistoneexectest", "testEJBTimersFindAndRemove#%", null, TaskState.SCHEDULED, true, null, null);
        if (results.size() != 3)
            throw new Exception("Unexpected number of tasks that start with testEJBTimersFindAndRemove#bean: " + results);

        // remove two of the tasks
        count = executor.removeTimers("persistoneexectest", "testEJBTimersFindAndRemove#bean^_B%", '^', TaskState.CANCELED, false);
        if (count != 2)
            throw new Exception("Unexpected number of tasks removed: " + count);

        // try to remove them again
        count = executor.removeTimers("persistoneexectest", "testEJBTimersFindAndRemove#bean^_B%", '^', TaskState.CANCELED, false);
        if (count != 0)
            throw new Exception("Shouldn't be able to remove tasks that we already removed: " + count);

        // find the tasks we removed
        results = executor.findTimerStatus("persistoneexectest", "testEJBTimersFindAndRemove#bean^_B%", '^', TaskState.ENDED, false, 0l, 2);
        if (!results.isEmpty())
            throw new Exception("Non-empty result for query on tasks that we removed: " + results);

        // find the task we didn't remove
        results = executor.findTimerStatus("persistoneexectest", "testEJBTimersFindAndRemove#%", '~', TaskState.ANY, true, null, null);
        if (results.size() != 1)
            throw new Exception("Unexpected number of tasks after having removed 2 of 3: " + results);
        TaskStatus<?> statusAA = results.get(0);
        if (!statusA.equals(statusAA))
            throw new Exception("Task status " + statusAA + " obtained via findTimerStatus does not match " + statusA);

        TimerStatus<?> statusAAA = executor.getTimerStatus(statusA.getTaskId());
        if (!statusA.equals(statusAAA))
            throw new Exception("Task status " + statusAAA + " obtained via getTimerStatus does not match " + statusA);
        if (!statusAA.equals(statusAAA))
            throw new Exception("Task status " + statusAAA + " obtained via getTimerStatus does not match " + statusAA + " obtained via findTimerStatus");
    }

    /**
     * Verify that the interface to EJB Timer Service indicates that fail over is enabled.
     */
    public void testFailOverIsEnabled(HttpServletRequest request, PrintWriter out) throws Exception {
        TimersPersistentExecutor executorB = (TimersPersistentExecutor) new InitialContext().lookup("concurrent/executorB");
        TimersPersistentExecutor executorC = (TimersPersistentExecutor) new InitialContext().lookup("concurrent/executorC");

        if (!executorB.isFailOverEnabled())
            throw new Exception("persistentExecutor with a positive missedTaskThreshold and with polling disabled reports that fail over is not enabled.");

        if (!executorC.isFailOverEnabled())
            throw new Exception("persistentExecutor with a positive missedTaskThreshold and with polling enabled reports that fail over is not enabled.");
    }

    /**
     * Verify that the interface to EJB Timer Service indicates that fail over is not enabled.
     */
    public void testFailOverIsNotEnabled(HttpServletRequest request, PrintWriter out) throws Exception {
        TimersPersistentExecutor executorB = (TimersPersistentExecutor) new InitialContext().lookup("concurrent/executorB");
        TimersPersistentExecutor executorC = (TimersPersistentExecutor) new InitialContext().lookup("concurrent/executorC");

        if (executorB.isFailOverEnabled())
            throw new Exception("persistentExecutor without a missedTaskThreshold and with polling disabled reports that fail over is enabled.");

        if (executorC.isFailOverEnabled())
            throw new Exception("persistentExecutor without a missedTaskThreshold and with polling enabled reports that fail over is enabled.");
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

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        TaskStatus<Integer> status = executor.schedule(new RepeatingTask(), new RepeatingTrigger(initialDelay, interval));
        long taskId = status.getTaskId();
        out.println("Task id is " + taskId + ".");
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

    /**
     * Transfer tasks from one executor's partition to another executor.
     */
    public void testTransfer(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        Long maxTaskId = request.getParameter("maxTaskId") == null ? null : Long.parseLong(request.getParameter("maxTaskId"));
        String oldExecutorIdentifier = request.getParameter("oldExecutorId");
        String libertyServerName = "com.ibm.ws.concurrent.persistent.fat.oneexec";

        ObjectInstance bean = getPEMBean(mbs, jndiName);
        Object obj = mbs.invoke(bean.getObjectName(), "findPartitionInfo", new Object[] { null, null, libertyServerName, oldExecutorIdentifier }
                                , new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
        String[][] oldPartitions = (String[][]) obj;

        String[] oldPartitionInfo = oldPartitions[0];
        long oldPartitionId = Long.valueOf(oldPartitionInfo[0]);
        if (!(oldPartitionInfo[1] instanceof String))
            throw new Exception("Incorrect or missing host name in " + deepToString(oldPartitions));
        if (!(oldPartitionInfo[2] instanceof String))
            throw new Exception("Incorrect or missing user dir in " + deepToString(oldPartitions));
        if (!libertyServerName.equals(oldPartitionInfo[3]))
            throw new Exception("Unexpected server name in " + deepToString(oldPartitions));
        if (!oldExecutorIdentifier.equals(oldPartitionInfo[4]))
            throw new Exception("Unexpected executor identifier in " + deepToString(oldPartitions));

        mbs.invoke(bean.getObjectName(), "transfer", new Object[] { maxTaskId, oldPartitionId }
                   , new String[] { "java.lang.Long", "long" });
    }

    /**
     * Obtain the Persistent Executor MBean with specified jndiName
     */
    private ObjectInstance getPEMBean(MBeanServer mbs, String jndiName) throws Exception {
        ObjectName obn = new ObjectName("WebSphere:type=PersistentExecutorMBean,jndiName=" + jndiName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            System.out.println("ERROR: Found incorrect number of MBeans (" + s.size() + ")");
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        return s.iterator().next();
    }
}
