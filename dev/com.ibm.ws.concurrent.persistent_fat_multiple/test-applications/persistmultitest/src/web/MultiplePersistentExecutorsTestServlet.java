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
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedTask;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

import componenttest.app.FATServlet;

@WebServlet("/*")
public class MultiplePersistentExecutorsTestServlet extends FATServlet {
    private static final long serialVersionUID = 915174288591951694L;

    /**
     * Interval for polling task status.
     */
    private static final long POLL_INTERVAL = 200;

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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter(FATServlet.TEST_METHOD);
        String invoker = request.getParameter("invokedBy");
        PrintWriter out = response.getWriter();

        try {
            System.out.println("-----> " + test + "(invoked by " + invoker + ") starting " + request.getQueryString());
            super.doGet(request, response);
        } finally {
            out.flush();
            out.close();
            System.out.println("<----- " + test + "(invoked by " + invoker + ") completed");
        }
    }

    /**
     * Initialize the scheduler tables and a table used by the application
     */
    @Override
    public void init() throws ServletException {
        mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            DBUpdateTask.init();
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new ServletException(x);
        }
    }

    /**
     * Cancels tasks based on name pattern and state.
     */
    public void testCancelTasks(HttpServletRequest request, HttpServletResponse response) throws Exception {
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
     * Have a task share the same connection as the persistent executor transaction.
     */
    public void testConnectionSharing(HttpServletRequest request, HttpServletResponse response) throws Exception {
        int initialValue = DBUpdateTask.get();

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup("concurrent/executor1");
        TaskStatus<?> status = executor.submit(new DBUpdateTask());

        int value = DBUpdateTask.get();
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS && value == initialValue; value = DBUpdateTask.get())
            Thread.sleep(POLL_INTERVAL);

        if (value == initialValue) {
            status = executor.getStatus(status.getTaskId());
            if (status != null && status.getNextExecutionTime() == null)
                status.get();
            throw new Exception("Task did not complete successfully within allotted interval. " + status);
        }

        if (value != initialValue + 1)
            throw new Exception("Unexpected value in database: " + value + ". Expected " + (initialValue + 1));
    }

    /**
     * Verifies that partition entries exist and are owned by the Liberty server that is running the test bucket.
     */
    public void testFindPartitions(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        Set<String> executorNames = new HashSet<String>(Arrays.asList(request.getParameterValues("executorId")));

        ObjectInstance bean = getPEMBean(mbs, jndiName);
        Object obj = mbs.invoke(bean.getObjectName(), "findPartitionInfo", new Object[] { null, null, null, null },
                                new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
        String[][] records = (String[][]) obj;

        for (String[] record : records) {
            String executorName = record[4];
            executorNames.remove(executorName);
            String libertyName = record[3];
            if (!"com.ibm.ws.concurrent.persistent.fat.multiple".equals(libertyName))
                throw new Exception("Unexpected Liberty server name in partition table: " + record);
        }
        if (!executorNames.isEmpty())
            throw new Exception("Didn't find executor names " + executorNames + " in the partition query results: " + records);
    }

    /**
     * Finds a list of non-ended tasks ids assigned to the specified executor
     */
    public void testFindTaskIds(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        String jndiName = request.getParameter("jndiName");
        String executorName = request.getParameter("executorId");

        ObjectInstance bean = getPEMBean(mbs, jndiName);
        Object obj = mbs.invoke(bean.getObjectName(), "findPartitionInfo", new Object[] { null, null, null, executorName },
                                new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
        String[][] records = (String[][]) obj;
        String partitionId = records[0][0];

        obj = mbs.invoke(bean.getObjectName(), "findTaskIds", new Object[] { Long.valueOf(partitionId), "ENDED", false, null, null },
                         new String[] { "long", "java.lang.String", "boolean", "java.lang.Long", "java.lang.Integer" });
        Long[] taskIds = (Long[]) obj;

        out.print("Task ids: ");
        for (Long taskId : taskIds)
            out.print(taskId + " ");
        out.println(".");
    }

    /**
     * Run the code similar to the example in PersistentExecutorMBean.
     */
    @SuppressWarnings("deprecation")
    public void testMBeanCodeExample(HttpServletRequest request, HttpServletResponse response) throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName("WebSphere:type=PersistentExecutorMBean,id=executor3,*");
        Set<ObjectInstance> s = mbs.queryMBeans(name, null);
        if (s.size() != 1)
            throw new Exception("Unexpected count of mbeans " + s);
        ObjectName fullName = s.iterator().next().getObjectName();

        com.ibm.websphere.concurrent.persistent.mbean.PersistentExecutorMBean proxy =
                JMX.newMXBeanProxy(mbs, fullName, com.ibm.websphere.concurrent.persistent.mbean.PersistentExecutorMBean.class);

        String[][] records = proxy.findPartitionInfo(null, null, "com.ibm.ws.concurrent.persistent.fat.multiple", "executor2");
        long oldPartition = Long.valueOf(records[0][0]);

        Long[] taskIds = proxy.findTaskIds(oldPartition, "ENDED", false, null, 100);
        if (taskIds.length != 1)
            throw new Exception("Unexpected number of tasks: " + Arrays.toString(taskIds));

        int numTransferred = proxy.transfer(null, oldPartition);

        if (numTransferred < 1)
            throw new Exception("Unexpected transfer count: " + numTransferred);

        int count = proxy.removePartitionInfo(null, null, "com.ibm.ws.concurrent.persistent.fat.multiple", "executor2");

        if (count != 1)
            throw new Exception("Unexpected removal count " + count);
    }

    /**
     * Removes partition entries.
     */
    public void testRemovePartitions(HttpServletRequest request, HttpServletResponse response) throws Exception {
        int expectedUpdateCount = Integer.parseInt(request.getParameter("expectedUpdateCount"));
        String executorName = request.getParameter("executorId");
        String hostName = request.getParameter("hostName");
        String libertyServer = request.getParameter("libertyServer");
        String userDir = request.getParameter("userDir");
        String jndiName = request.getParameter("jndiName");

        ObjectInstance bean = getPEMBean(mbs, jndiName);
        Object obj = mbs.invoke(bean.getObjectName(), "removePartitionInfo", new Object[] { hostName, userDir, libertyServer, executorName },
                                new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });
        int updateCount = (Integer) obj;

        if (updateCount != expectedUpdateCount)
            throw new Exception("Expected " + expectedUpdateCount + " partition entries removed, not " + updateCount);
    }

    /**
     * Removes a task
     */
    public void testRemoveTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
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
    public void testScheduleRepeatingTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
        PrintWriter out = response.getWriter();
        String jndiName = request.getParameter("jndiName");
        long initialDelay = Long.parseLong(request.getParameter("initialDelay"));
        long interval = Long.parseLong(request.getParameter("interval"));
        String taskName = request.getParameter("taskName");

        RepeatingTask task = new RepeatingTask();
        if (taskName != null)
            task.getExecutionProperties().put(ManagedTask.IDENTITY_NAME, taskName);

        PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);
        TaskStatus<Integer> status = executor.schedule(task, new RepeatingTrigger(initialDelay, interval));
        long taskId = status.getTaskId();
        out.println("Task ids: " + taskId + ".");
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
    public void testTasksAreRunning(HttpServletRequest request, HttpServletResponse response) throws Exception {
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
    public void testTransfer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        Long maxTaskId = request.getParameter("maxTaskId") == null ? null : Long.parseLong(request.getParameter("maxTaskId"));
        String oldExecutorIdentifier = request.getParameter("oldExecutorId");
        String libertyServerName = "com.ibm.ws.concurrent.persistent.fat.multiple";

        ObjectInstance bean = getPEMBean(mbs, jndiName);
        Object obj = mbs.invoke(bean.getObjectName(), "findPartitionInfo", new Object[] { null, null, libertyServerName, oldExecutorIdentifier },
                                new String[] { "java.lang.String", "java.lang.String", "java.lang.String", "java.lang.String" });

        String[][] oldPartitions = (String[][]) obj;

        @SuppressWarnings("unused")
		PersistentExecutor executor = (PersistentExecutor) new InitialContext().lookup(jndiName);

        if (oldPartitions.length != 1)
            throw new Exception("Expecting exactly one match for " + oldExecutorIdentifier + ". Instead " + deepToString(oldPartitions));

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

        mbs.invoke(bean.getObjectName(), "transfer", new Object[] { maxTaskId, oldPartitionId }, new String[] { "java.lang.Long", "long" });
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
