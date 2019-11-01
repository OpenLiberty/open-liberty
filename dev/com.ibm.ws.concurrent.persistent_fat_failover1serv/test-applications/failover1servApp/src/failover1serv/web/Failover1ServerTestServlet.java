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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

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
     * testHeartbeatsAreRepeatedlySent - verifies that heart beats are being sent periodically, with an increasing expiry timestamp.
     */
    @Test
    public void testHeartbeatsAreRepeatedlySent() throws Exception {
        // Ensure the database tables are present
        PersistentExecutor executor = InitialContext.doLookup("persistent/exec2");
        executor.getProperty("testHeartbeatsAreRepeatedlySent");

        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");
        try (Connection con = ds.getConnection()) {
            PreparedStatement st = con.prepareStatement("SELECT EXPIRY FROM WLPPART WHERE EXECUTOR=?");
            st.setString(1, "persistentExec2");
            ResultSet result = st.executeQuery();
            assertTrue(result.next());
            long expiry1 = result.getLong(1);
            st.close();
            System.out.println("Found heartbeat from persistentExec2 with expiry at " + expiry1);

            // Poll for the expiry to be updated by a subsequent heartbeat
            st = con.prepareStatement("SELECT EXPIRY FROM WLPPART WHERE EXECUTOR=? AND EXPIRY>?");
            st.setString(1, "persistentExec2");
            st.setLong(2, expiry1);

            boolean found = false;
            for (long start = System.nanoTime();
                    !(found = (result = st.executeQuery()).next()) && System.nanoTime() - start < TIMEOUT_NS;
                    Thread.sleep(POLL_INTERVAL_MS))
                ;

            assertTrue(found);
            long expiry2 = result.getLong(1);
            System.out.println("Found heartbeat from persistentExec2 with expiry at " + expiry2);
            st.setLong(2, expiry2);

            found = false;
            for (long start = System.nanoTime();
                    !(found = (result = st.executeQuery()).next()) && System.nanoTime() - start < TIMEOUT_NS;
                    Thread.sleep(POLL_INTERVAL_MS))
                ;

            assertTrue(found);
            long expiry3 = result.getLong(1);
            System.out.println("Found heartbeat from persistentExec2 with expiry at " + expiry3);
        }
    }

    /**
     * testMissedHeartbeatsClearOldPartitionData - insert entries representing missed heartbeats directly into the
     * database. Verify that they are automatically removed (happens when heartbeat information is checked).
     */
    @Test
    public void testMissedHeartbeatsClearOldPartitionData(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Ensure the database tables are present
        PersistentExecutor executor = InitialContext.doLookup("persistent/exec2");
        executor.getProperty("testMissedHeartbeatsClearOldPartitionData");

        DataSource ds = InitialContext.doLookup("java:comp/DefaultDataSource");
        try (Connection con = ds.getConnection()) {
            PreparedStatement st = con.prepareStatement("INSERT INTO WLPPART(EXECUTOR,HOSTNAME,LSERVER,USERDIR,EXPIRY,STATES) VALUES(?,?,?,?,?,?)");

            // insert partition data to simulate a server that stopped sending heartbeats 100 days ago
            st.setString(1, "oldExecutor1");
            st.setString(2, "host1.rchland.ibm.com");
            st.setString(3, "oldServer1");
            st.setString(4, "/Users/old1/wlp/usr");
            st.setLong(5, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(100));
            st.setLong(6, 0);
            assertEquals(1, st.executeUpdate());

            // insert partition data to simulate a server that stopped sending heartbeats 200 days ago
            st.setString(1, "oldExecutor2");
            st.setString(2, "host2.rchland.ibm.com");
            st.setString(3, "oldServer2");
            st.setString(4, "/Users/old2/wlp/usr");
            st.setLong(5, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(200));
            st.setLong(6, 0);
            assertEquals(1, st.executeUpdate());

            // insert partition data to simulate another server that stopped sending heartbeats 200 days ago
            st.setString(1, "oldExecutor3");
            st.setString(2, "host2.rchland.ibm.com");
            st.setString(3, "oldServer3");
            st.setString(4, "/Users/old2/wlp/usr");
            st.setLong(5, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(200));
            st.setLong(6, 0);
            assertEquals(1, st.executeUpdate());
            st.close();

            // Wait for the above data to be removed due to missed heartbeats
            st = con.prepareStatement("SELECT EXECUTOR FROM WLPPART WHERE LSERVER LIKE 'oldServer%'");
            boolean found = true;
            for (long start = System.nanoTime(); (found = st.executeQuery().next()) && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL_MS))
                ;

            if (found) {
                StringBuilder sb = new StringBuilder();
                ResultSet result = st.executeQuery();
                while (result.next())
                    sb.append(result.getString(1)).append(' ');
                result.close();
                assertEquals("The following entries should have been removed upon detecting missed heartbeats: " + sb.toString(), 0, sb.length());
            }
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
}
