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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedTask;
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

@WebServlet("/*")
public class SimulatedControllerPXTestServlet extends HttpServlet {
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

    @Resource
    private UserTransaction tran;

    MBeanServer mbs;

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
     * Initialize the scheduler tables and a table used by the application
     */
    @Override
    public void init() throws ServletException {
        mbs = ManagementFactory.getPlatformMBeanServer();
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
     * Verifies that partition entries exist and are owned by the Liberty server that is running the test bucket.
     */
    public void testFindPartitions(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");

        tran.begin();
        try {

            ObjectInstance bean = getPEMBean(mbs, jndiName);
            Object obj = mbs.invoke(bean.getObjectName(), "findPartitionInfo", new Object[] { null, null, null, null }
                                    , new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
            String[][] records = (String[][]) obj;

            Set<String> executorNames = new HashSet<String>(Arrays.asList(request.getParameterValues("executorId")));
            for (String[] record : records) {
                String executorName = record[4];
                executorNames.remove(executorName);
                String libertyName = record[3];
                if (!"com.ibm.ws.concurrent.persistent.fat.simctrl".equals(libertyName))
                    throw new Exception("Unexpected Liberty server name in partition table: " + record);
            }
            if (!executorNames.isEmpty())
                throw new Exception("Didn't find executor names " + executorNames + " in the partition query results: " + records);
        } finally {
            tran.commit();
        }
    }

    /**
     * Removes partition entries.
     */
    public void testRemovePartitions(HttpServletRequest request, PrintWriter out) throws Exception {
        int expectedUpdateCount = Integer.parseInt(request.getParameter("expectedUpdateCount"));
        String executorName = request.getParameter("executorId");
        String hostName = request.getParameter("hostName");
        String libertyServer = request.getParameter("libertyServer");
        String userDir = request.getParameter("userDir");
        String jndiName = request.getParameter("jndiName");

        tran.begin();
        try {
            ObjectInstance bean = getPEMBean(mbs, jndiName);
            Object obj = mbs.invoke(bean.getObjectName(), "removePartitionInfo", new Object[] { hostName, userDir, libertyServer, executorName }
                                    , new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
            int updateCount = (Integer) obj;

            if (updateCount != expectedUpdateCount)
                throw new Exception("Expected " + expectedUpdateCount + " partition entries removed, not " + updateCount);
        } finally {
            tran.commit();
        }
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

        RepeatTask task = new RepeatTask();
        if (taskName != null)
            task.getExecutionProperties().put(ManagedTask.IDENTITY_NAME, taskName);

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        TaskStatus<Integer> status = executor.schedule(task, new RepeatTrigger(initialDelay, interval));
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
     * Verifies that a task runs successfully at least once, and then cancel it.
     */
    public void testVerifyTaskRunsAndCancel(HttpServletRequest request, PrintWriter out) throws Exception {
        String jndiName = request.getParameter("jndiName");
        long taskId = Long.parseLong(request.getParameter("taskId"));

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        TaskStatus<Integer> status = executor.getStatus(taskId);
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && !status.hasResult(); status = executor.getStatus(taskId))
            Thread.sleep(POLL_INTERVAL);
        if (!status.hasResult())
            throw new Exception("Task did not complete any executions within alotted interval. " + status);

        if (!status.cancel(false))
            throw new Exception("Unable to cancel task " + status);
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
