/*******************************************************************************
 * Copyright (c) 2019,2020 IBM Corporation and others.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
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

        TaskStatus<?> status = executor.getStatus(taskId);
        if (status != null)
            status.cancel(false);
    }

    /**
     * testGetPartitionId - verify that a partition id for the specified executor can be found within an allotted interval.
     */
    public void testGetPartitionId(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String executor = request.getParameter("executor");

        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");
        try (Connection con = ds.getConnection()) {
            PreparedStatement st = con.prepareStatement("SELECT ID FROM WLPPART WHERE EXECUTOR=?");
            st.setString(1, executor);

            ResultSet result;
            boolean hasNext;
            for (long start = System.nanoTime(); !(hasNext = (result = st.executeQuery()).next()) && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL_MS))
                ;

            if (hasNext) {
                long partitionId = result.getLong(1);
                response.getWriter().println("Partition id is " + partitionId + ".");
            } else
                fail("Partition id for " + executor + " is not found.");
        }
    }

    /**
     * testRemovePartition - removes the specified partition entry.
     */
    public void testRemovePartition(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String executor = request.getParameter("executor");

        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");
        try (Connection con = ds.getConnection()) {
            PreparedStatement st = con.prepareStatement("DELETE FROM WLPPART WHERE EXECUTOR=?");
            st.setString(1, executor);
            assertEquals(1, st.executeUpdate());
        }
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
     * testTablesExist - ensure that tables within the persistent task store exist by looking up a persistent executor
     * and performing a simple operation on it.
     */
    public void testTablesExist(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Ensure the database tables are present
        String jndiName = request.getParameter("jndiName");
        PersistentExecutor executor = InitialContext.doLookup(jndiName);
        executor.getProperty("testTablesExist");
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

        assertNull("Task did not complete within allotted interval " + status, status.getNextExecutionTime());
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
                              throw new RuntimeException("Task " + status + " did not complete any executions within the allotted interval.");
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

    /**
     * Transfer tasks to the specified instance. The transfer operation is performed via the PersistentExecutor MBean.
     */
    public void testTransferWithMBean(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        String[] taskIds = request.getParameterValues("taskId");

        PersistentExecutor executor = InitialContext.doLookup(jndiName);
        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName obn = new ObjectName("WebSphere:type=PersistentExecutorMBean,jndiName=" + jndiName + ",*");
        Set<ObjectInstance> s = mbs.queryMBeans(obn, null);
        if (s.size() != 1) {
            for (ObjectInstance i : s)
                System.out.println("  Found MBean: " + i.getObjectName());
            throw new Exception("Expected to find exactly 1 MBean, instead found " + s.size());
        }
        ObjectInstance mbean = s.iterator().next();
        String[] paramTypes = { "java.lang.Long", "long" };

        for (String taskIdString : taskIds) {
            long taskId = Long.valueOf(taskIdString);
            // The only way to find the value stored in a task's PARTN column is to query the database
            long oldValue;
            try (Connection con = ds.getConnection()) {
                PreparedStatement st = con.prepareStatement("SELECT PARTN FROM WLPTASK WHERE ID=?");
                st.setLong(1, taskId);
                ResultSet result = st.executeQuery();
                if (!result.next())
                    throw new Exception("Task " + taskId + " is not found.");
                oldValue = result.getLong(1);
            }

            // Reassign using the mbean
            int tasksTransferred = (Integer) mbs.invoke(mbean.getObjectName(), "transfer",
                    new Long[] { taskId, oldValue },
                    new String[] { "java.lang.Long", "long" });

            if (tasksTransferred < 1)
                throw new Exception("Task " + taskId + " with " + oldValue + " is not found by mbean " + mbean);
        }
    }

    /**
     * Transfer tasks to the specified instance. The transfer operation is performed by directly updating the database.
     */
    public void testTransferWithoutMBean(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String jndiName = request.getParameter("jndiName");
        String[] taskIds = request.getParameterValues("taskId");

        PersistentExecutor executor = InitialContext.doLookup(jndiName);
        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");

        for (String taskIdString : taskIds) {
            long taskId = Long.valueOf(taskIdString);
            // The only way to find the value stored in a task's PARTN column is to query the database
            try (Connection con = ds.getConnection()) {
                // querying only EXECUTOR and ignoring HOSTNAME, USERDIR, LSERVER columns because there is only one instance
                PreparedStatement st = con.prepareStatement("SELECT ID FROM WLPPART WHERE EXECUTOR=?");
                st.setString(1, "persistentExecRFR");
                ResultSet result = st.executeQuery();
                if (!result.next())
                    throw new Exception("Partition entry of current instance is not found." +
                            " Typically that would indicate the instance hasn't been used yet in order to generate a partion id," +
                            " but for this particular test, we know it has been used, so this is an error.");
                long newValue = result.getLong(1);
                st.close();

                st = con.prepareStatement("UPDATE WLPTASK SET PARTN=? WHERE ID=?");
                st.setLong(1, newValue);
                st.setLong(2, taskId);
                int numUpdates = st.executeUpdate();
                if (numUpdates < 1)
                    throw new Exception("Task " + taskId + " is not found in the database.");
            }
        }
    }
}
