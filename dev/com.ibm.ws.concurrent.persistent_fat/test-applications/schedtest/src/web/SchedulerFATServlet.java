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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.SkippedException;
import javax.enterprise.concurrent.Trigger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;
import com.ibm.ws.concurrent.persistent.ejb.TimerStatus;
import com.ibm.ws.concurrent.persistent.ejb.TimerTrigger;
import com.ibm.ws.concurrent.persistent.ejb.TimersPersistentExecutor;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;

@WebServlet("/*")
public class SchedulerFATServlet extends HttpServlet {
    private static final long serialVersionUID = 8447513765214641067L;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Resource(lookup = "concurrent/myScheduler")
    private PersistentExecutor scheduler;

    // DefaultDataSource
    @Resource(name = "java:module/env/jdbc/testDBRef")
    private DataSource testDB;

    // JDBC resource used to set up the scheduler DB when using Derby.
    @Resource(lookup = "jdbc/schedDB", name = "java:module/env/jdbc/schedDBRef")
    private DataSource schedDB;

    /**
     * Interval in milliseconds between polling for task results.
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(1);

    @Resource
    private UserTransaction tran;

    /**
     * Executor that runs on an unmanaged thread.
     */
    private ExecutorService unmanagedExecutor;

    @Override
    public void destroy() {
        unmanagedExecutor.shutdown();

        // TODO: to help guard against the possibility of tasks running extra times,
        // we could track the expected results that each test leaves in MYTABLE upon completion,
        // and then verify at the end of all tests, that no further changes have been made.
        // That would provide some coverage against extra unexpected executions without having
        // to add sleeps in each test.
    }

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("testMethod");
        String fileName = request.getParameter("fileName");
        PrintWriter out = response.getWriter();

        try {
            out.println(getClass().getSimpleName() + " is starting " + test + "<br>");
            System.out.println("-----> " + test + " starting");
            if (fileName == null) {
                getClass().getMethod(test, PrintWriter.class).invoke(this, out);
            } else {
                getClass().getMethod(test, PrintWriter.class, String.class).invoke(this, out, fileName);
            }
            System.out.println("<----- " + test + " successful");
            out.println(test + " " + SUCCESS_MESSAGE);
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

    /**
     * Utility method that processes a list of query results and copies them into a map indexed by task id.
     */
    private static Map<Long, TaskStatus<?>> getResultsByTaskIdAndClose(List<TaskStatus<?>> results) throws Exception {
        Map<Long, TaskStatus<?>> map = new LinkedHashMap<Long, TaskStatus<?>>();
        for (TaskStatus<?> status : results)
            map.put(status.getTaskId(), status);
        return map;
    }

    /**
     * Initialize the scheduler tables and a table used by the application
     */
    @Override
    public void init() throws ServletException {
        unmanagedExecutor = Executors.newSingleThreadExecutor();

        try {
            Connection con = testDB.getConnection();
            try {
                Statement stmt = con.createStatement();
                try {
                    stmt.executeUpdate("DELETE FROM MYTABLE"); // delete any entries from previous test run
                } catch (SQLException x) {
                    stmt.executeUpdate("CREATE TABLE MYTABLE (MYKEY VARCHAR(80) NOT NULL PRIMARY KEY, MYVALUE INT)");
                }
            } finally {
                con.close();
            }
        } catch (SQLException x) {
            throw new ServletException(x);
        }
    }

    /**
     * Set-up method which creates the persistent executor tables from the DDL
     * which was generated by the persistence service.
     */
    public void createTablesDerby(PrintWriter out, String ddlFileName) throws Exception {
        // Go open the file and parse out the statements.  We assume that there is
        // a single statement on each line of the DDL.
        List<String> ddlStatementList = new java.util.ArrayList<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ddlFileName)));
        while (br.ready()) {
            ddlStatementList.add(br.readLine());
        }
        br.close();

        try {
            Connection con = schedDB.getConnection();
            try {
                String dbProduct = con.getMetaData().getDatabaseProductName();
                if ((dbProduct == null) || (dbProduct.toLowerCase().contains("apache derby") == false)) {
                    throw new Exception("The database product name is " + dbProduct + ".  This method should only be used by Derby.");
                }

                for (String x : ddlStatementList) {
                    System.out.println("Running SQL statement: " + x);
                    Statement stmt = con.createStatement();
                    stmt.execute(x);
                }
            } finally {
                con.close();
            }
        } catch (SQLException x) {
            throw new ServletException(x);
        }
    }

    /**
     * Utility method that waits for a task status with the specified result.
     *
     * @return task status if found within the allotted interval, otherwise null.
     */
    private <T> TaskStatus<T> pollForResult(long taskId, T result) throws Exception {
        TaskStatus<T> status = null;
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL)) {
            TaskStatus<T> newStatus = scheduler.getStatus(taskId);
            status = newStatus;
            try {
                if (status == null || status.hasResult() && result.equals(status.getResult()))
                    return status;
            } catch (SkippedException x) {
                // allow for skips
            }
        } ;

        return status;
    }

    /**
     * Utility method that waits for a task status that says the task was skipped.
     */
    private <T> TaskStatus<T> pollForSkip(long taskId) throws Exception {
        TaskStatus<T> status = null;
        for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL)) {
            TaskStatus<T> newStatus = scheduler.getStatus(taskId);
            status = newStatus;
            if (status.hasResult())
                try {
                    status.getResult();
                } catch (SkippedException x) {
                    break;
                }
        } ;

        return status;
    }

    /**
     * Utility method that waits for a key/value to appear in the database.
     */
    private void pollForTableEntry(String key, int expectedValue) throws Exception {
        Integer value = null;
        Connection con = testDB.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("SELECT MYVALUE FROM MYTABLE WHERE MYKEY=?");
            pstmt.setString(1, key);
            for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL)) {
                ResultSet result = pstmt.executeQuery();
                if (result.next()) {
                    value = result.getInt(1);
                    if (value != null && value.equals(expectedValue))
                        return;
                } else
                    value = null;
            } ;
        } finally {
            con.close();
        }
        if (value == null)
            throw new Exception("Entry with key " + key + " not found");
        else
            throw new Exception("Unexpected value " + value + " for entry with key " + key);
    }

    /**
     * Utility method that waits for a key/minimum value to appear in the database.
     */
    private void pollForTableEntryMinimumValue(String key, int minimumValue) throws Exception {
        Integer value = null;
        Connection con = testDB.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("SELECT MYVALUE FROM MYTABLE WHERE MYKEY=?");
            pstmt.setString(1, key);
            for (long start = System.nanoTime(); System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL)) {
                ResultSet result = pstmt.executeQuery();
                if (result.next()) {
                    value = result.getInt(1);
                    if (value != null && value >= minimumValue)
                        return;
                } else
                    value = null;
            } ;
        } finally {
            con.close();
        }
        if (value == null)
            throw new Exception("Entry with key " + key + " not found");
        else
            throw new Exception("Unexpected value " + value + " for entry with key " + key);
    }

    /**
     * Schedule a task that rolls back on every execution attempt, exceeding the failure limit,
     * and is auto purged from the persistent store.
     */
    @Test
    public void testAbortRolledBackTaskAndAutoPurge(PrintWriter out) throws Exception {
        SharedRollbackTask.counter.set(0);
        SharedRollbackTask.execProps.put(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.toString());
        SharedRollbackTask.rollBackOn.add(1l);
        SharedRollbackTask.rollBackOn.add(2l);
        SharedRollbackTask.rollBackOn.add(3l);
        try {
            Callable<Long> task = new SharedRollbackTask();
            TaskStatus<Long> status = scheduler.schedule(task, -20, TimeUnit.MICROSECONDS);

            for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                status = scheduler.getStatus(status.getTaskId());

            if (status != null)
                throw new Exception("Task was not aborted a timely manner or was not autopurged. " + status);

            long counter = SharedRollbackTask.counter.get();
            if (counter != 2)
                throw new Exception("Task should be attempted exactly 2 times (with both attempts rolling back). Instead " + counter);
        } finally {
            SharedRollbackTask.execProps.clear();
            SharedRollbackTask.rollBackOn.clear();
        }
    }

    /**
     * Schedule a task that rolls back on every execution attempt, exceeding the failure limit,
     * and remains in the persistent store as an aborted task.
     */
    @Test
    public void testAbortRolledBackTaskNoAutoPurge(PrintWriter out) throws Exception {
        SharedRollbackTask.counter.set(0);
        SharedRollbackTask.rollBackOn.add(1l);
        SharedRollbackTask.rollBackOn.add(2l);
        SharedRollbackTask.rollBackOn.add(3l);
        try {
            Callable<Long> task = new SharedRollbackTask();
            TaskStatus<Long> status = scheduler.schedule(task, 21, TimeUnit.MICROSECONDS);

            for (long start = System.nanoTime(); status.getNextExecutionTime() != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                status = scheduler.getStatus(status.getTaskId());

            if (!status.isDone() || status.isCancelled())
                throw new Exception("Task in unexpected state. " + status);

            try {
                Long result = status.get();
                throw new Exception("Task should have been aborted. Instead result is: " + result);
            } catch (AbortedException x) {
                if (x.getMessage() == null
                    || !x.getMessage().contains("CWWKC1555E")
                    || !x.getMessage().contains(" " + status.getTaskId() + " ")
                    || !x.getMessage().contains(" 2 "))
                    throw x;
            }
        } finally {
            SharedRollbackTask.execProps.clear();
            SharedRollbackTask.rollBackOn.clear();
        }
    }

    /**
     * Attempt to cancel all tasks that are completed. Result should be that no tasks are canceled.
     */
    @Test
    public void testCancelCompletedTasksNoOp(PrintWriter out) throws Exception {
        int numCanceled = scheduler.cancel(null, null, TaskState.ENDED, true);
        if (numCanceled != 0)
            throw new Exception("Should not be able to cancel " + numCanceled + " tasks that have ended");

        int numRemoved = ((TimersPersistentExecutor) scheduler).removeTimers("someOtherApp", null, null, TaskState.ANY, true);
        if (numRemoved != 0)
            throw new Exception("Should not find " + numRemoved + " tasks from non-existing application for removal");
    }

    /**
     * Cancel a task while it is running. The task entry does not autopurge.
     */
    @Test
    public void testCancelRunningTask(PrintWriter out) throws Exception {
        CancelableTask task = new CancelableTask("testCancelRunningTask", false);
        String taskName = task.getExecutionProperties().get(ManagedTask.IDENTITY_NAME);

        TaskStatus<Integer> status = scheduler.submit((Callable<Integer>) task);

        CancelableTask.waitForStart(taskName);

        boolean canceled = status.cancel(false);
        if (canceled)
            throw new Exception("Task " + status.getTaskId() + " should complete so we should not be able to cancel it.");

        status = scheduler.getStatus(status.getTaskId());
        if (status != null)
            throw new Exception("Task should be autopurged. Instead " + status);
    }

    /**
     * Cancel a task while it is running. The task's transaction must be suspended while running. The task entry does not autopurge.
     */
    @Test
    public void testCancelRunningTaskSuspendTransaction(PrintWriter out) throws Exception {
        CancelableTask task = new CancelableTask("testCancelRunningTaskSuspendTransaction", true);
        String taskName = task.getExecutionProperties().get(ManagedTask.IDENTITY_NAME);
        task.getExecutionProperties().put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);

        TaskStatus<Integer> status = scheduler.submit((Callable<Integer>) task);

        CancelableTask.waitForStart(taskName);

        boolean canceled = status.cancel(false);
        if (!canceled)
            throw new Exception("Unable to cancel task " + status.getTaskId());

        CancelableTask.notifyTaskCanceled(taskName);

        // wait for task result in database
        pollForTableEntry("testCancelRunningTaskSuspendTransaction", 1);

        status = scheduler.getStatus(status.getTaskId());
        if (!status.isCancelled() || !status.isDone())
            throw new Exception("Status must be canceled and done. Instead " + status);

        if (!status.hasResult())
            throw new Exception("Status lacks a result. " + status);

        try {
            Integer result = status.get();
            throw new Exception("Result must be a cancellation exception. Instead " + result + " for " + status);
        } catch (CancellationException x) {
            // pass
        }
    }

    /**
     * Cancel (and autopurge) a task while it is running. The task's transaction must be suspended while running.
     */
    @Test
    public void testCancelRunningTaskSuspendTransactionAndAutoPurge(PrintWriter out) throws Exception {
        CancelableTask task = new CancelableTask("testCancelRunningTaskSuspendTransactionAndAutoPurge", true);
        String taskName = task.getExecutionProperties().get(ManagedTask.IDENTITY_NAME);
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.name());
        task.getExecutionProperties().put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);

        TaskStatus<Integer> status = scheduler.submit((Callable<Integer>) task);

        CancelableTask.waitForStart(taskName);

        boolean canceled = status.cancel(false);
        if (!canceled)
            throw new Exception("Unable to cancel task " + status.getTaskId());

        CancelableTask.notifyTaskCanceled(taskName);

        // wait for task result in database
        pollForTableEntry("testCancelRunningTaskSuspendTransactionAndAutoPurge", 1);

        status = scheduler.getStatus(status.getTaskId());
        if (status != null)
            throw new Exception("Task entry did not autopurge " + status);
    }

    /**
     * Test that persistent executor properties can be used to coordinate single scheduling of a task
     * across multiple servers. To simulate this, we just invoke the createProperty operation twice.
     */
    @Test
    public void testCoordinateTaskScheduleAndRemove(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testCoordinateTaskScheduleAndRemove");
        String taskName = task.getExecutionProperties().get(ManagedTask.IDENTITY_NAME);

        // First insert attempt should succeed
        tran.begin();
        try {
            if (scheduler.createProperty(taskName, " "))
                scheduler.schedule((Runnable) task, 56, TimeUnit.MINUTES);
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }

        // Second insert attempt must not be possible
        tran.begin();
        try {
            if (scheduler.createProperty(taskName, " "))
                throw new Exception("Shouldn't be able to insert a duplicate");
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }

        // Remove the task
        tran.begin();
        try {
            if (scheduler.removeProperty(taskName)) {
                int numRemoved = scheduler.remove(DBIncrementTask.class.getSimpleName() + "-testCoordinateTaskScheduleAndRemove", null, TaskState.SCHEDULED, true);
                if (numRemoved != 1)
                    throw new Exception("Unexpected removal count: " + numRemoved);
            } else
                throw new Exception("Property not found for removal");
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }
    }

    /**
     * Test that persistent executor properties can be used to coordinate single scheduling of a task
     * across multiple servers and remembering the task id for removal.
     * To simulate this, we just invoke the createProperty operation twice
     * and use setProperty to update with the task id within the same transaction.
     */
    @Test
    public void testCoordinateTaskScheduleAndRemoveById(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testCoordinateTaskScheduleAndRemoveById");
        String taskName = task.getExecutionProperties().get(ManagedTask.IDENTITY_NAME);

        // First insert attempt should succeed
        tran.begin();
        try {
            if (scheduler.createProperty(taskName, " ")) {
                TaskStatus<?> status = scheduler.schedule((Runnable) task, 57, TimeUnit.MINUTES);
                scheduler.setProperty(taskName, String.valueOf(status.getTaskId()));
            }
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }

        // Second insert attempt must not be possible
        tran.begin();
        try {
            if (scheduler.createProperty(taskName, " "))
                throw new Exception("Shouldn't be able to insert a duplicate");
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }

        // Remove the task
        tran.begin();
        try {
            String taskId = scheduler.getProperty(taskName);
            if (taskId == null)
                throw new Exception("Property not found");
            else if (scheduler.removeProperty(taskName))
                if (!scheduler.remove(Long.parseLong(taskId)))
                    throw new Exception("Unable to remove task " + taskId);
        } finally {
            if (tran.getStatus() == Status.STATUS_MARKED_ROLLBACK)
                tran.rollback();
            else
                tran.commit();
        }
    }

    /**
     * Use the createProperty/setProperty interfaces to simulate creating a task group.
     * Use the getProperty interface to simulate querying for existence of a task group.
     * Use the removeProperty interface to simulate removal of a task group.
     */
    @Test
    public void testCreateAndRemoveTaskGroup(PrintWriter out) throws Exception {
        TimersPersistentExecutor timersExecutor = (TimersPersistentExecutor) scheduler;

        DBIncrementTask taskA = new DBIncrementTask("testCreateAndRemoveTaskGroup-A");
        taskA.getExecutionProperties().put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);
        taskA.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        taskA.getExecutionProperties().put(PersistentExecutor.TRANSACTION_TIMEOUT, "0"); // 0 means default transaction timeout
        DBIncrementTask taskB = new DBIncrementTask("testCreateAndRemoveTaskGroup-B");
        String value;

        TaskStatus<Integer> statusA;
        tran.begin();
        try {
            if (!scheduler.createProperty("testCreateAndRemoveTaskGroup", " "))
                throw new Exception("Unable to create the property.");

            statusA = scheduler.schedule((Callable<Integer>) taskA, 46, TimeUnit.NANOSECONDS);
            TaskStatus<?> statusB = scheduler.schedule((Runnable) taskB, 47, TimeUnit.DAYS);

            value = statusA.getTaskId() + "," + statusB.getTaskId();
            timersExecutor.setProperty("testCreateAndRemoveTaskGroup", value);
        } finally {
            tran.commit();
        }

        if (scheduler.createProperty("testCreateAndRemoveTaskGroup", "try to overwrite the value"))
            throw new Exception("Should not be able to create a property that already exists.");

        String value1 = scheduler.getProperty("testCreateAndRemoveTaskGroup");
        if (!value.equals(value1))
            throw new Exception("Missing or unexpected value: " + value1 + ". Expected: " + value);

        // Wait for taskA to complete
        for (long start = System.nanoTime(); !statusA.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            statusA = scheduler.getStatus(statusA.getTaskId());
        Integer resultA = statusA.get();
        if (resultA != 1)
            throw new Exception("Unexpected result of taskA: " + resultA);

        tran.begin();
        try {
            String value2 = scheduler.getProperty("testCreateAndRemoveTaskGroup");
            if (!value.equals(value2))
                throw new Exception("Missing or unexpected value: " + value2 + ". Expected: " + value);

            if (!scheduler.removeProperty("testCreateAndRemoveTaskGroup"))
                throw new Exception("Unable to remove the property.");

            for (String taskIdString : value2.split(",")) {
                long taskId = Long.parseLong(taskIdString);
                if (!scheduler.remove(taskId))
                    throw new Exception("Unable to remove task id " + taskId + ". Property value was " + value2);
            }
        } finally {
            tran.commit();
        }
    }

    /**
     * Use the createProperty/setProperty interfaces to simulate creating a multiple task groups.
     * Use the getProperties/removeProperties interfaces to simulate removal of multiple task groups
     * and the tasks within them.
     */
    @Test
    public void testCreateAndRemoveTaskGroups(PrintWriter out) throws Exception {
        TimersPersistentExecutor timersExecutor = (TimersPersistentExecutor) scheduler;

        DBIncrementTask taskA = new DBIncrementTask("testCreateAndRemoveTaskGroups-A");
        taskA.getExecutionProperties().put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);
        taskA.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        DBIncrementTask taskB = new DBIncrementTask("testCreateAndRemoveTaskGroups-B");

        TaskStatus<Integer> statusA;
        tran.begin();
        try {
            if (!scheduler.createProperty("testCreateAndRemoveTaskGroups_GroupA", " "))
                throw new Exception("Unable to create the first property.");

            statusA = scheduler.schedule((Callable<Integer>) taskA, 48, TimeUnit.MICROSECONDS);

            timersExecutor.setProperty("testCreateAndRemoveTaskGroups_GroupA", Long.toString(statusA.getTaskId()));
        } finally {
            tran.commit();
        }

        TaskStatus<?> statusB;
        tran.begin();
        try {
            if (!scheduler.createProperty("testCreateAndRemoveTaskGroups_GroupB", " "))
                throw new Exception("Unable to create the second property.");

            statusB = scheduler.schedule((Runnable) taskB, 49, TimeUnit.HOURS);

            timersExecutor.setProperty("testCreateAndRemoveTaskGroups_GroupB", Long.toString(statusB.getTaskId()));
        } finally {
            tran.commit();
        }

        if (!scheduler.createProperty("testCreateAndRemoveTaskGroups_NonMatchingGroupC", "33,56,65"))
            throw new Exception("Unable to create the third property.");

        // Match the first two groups
        Map<String, String> propMap;
        tran.begin();
        try {
            propMap = timersExecutor.findProperties("testCreateAndRemoveTaskGroups\\_Group_", '\\');
            if (propMap.size() != 2)
                throw new Exception("Should match exactly 2 properties. Instead " + propMap);

            int removed = timersExecutor.removeProperties("testCreateAndRemoveTaskGroups\\_Group_", '\\');
            if (removed != 2)
                throw new Exception("Should remove exactly 2 properties. Instead " + removed);

            for (Map.Entry<String, String> entry : propMap.entrySet()) {
                long taskId = Long.parseLong(entry.getValue());
                if (!scheduler.remove(taskId))
                    throw new Exception("Unable to remove task id " + taskId + ". Map is: " + propMap);
            }
        } finally {
            tran.commit();
        }

        List<String> expectedKeys = Arrays.asList("testCreateAndRemoveTaskGroups_GroupA", "testCreateAndRemoveTaskGroups_GroupB");
        List<String> expectedValues = Arrays.asList(Long.toString(statusA.getTaskId()), Long.toString(statusB.getTaskId()));

        if (!propMap.keySet().containsAll(expectedKeys))
            throw new Exception("Expected keys " + expectedKeys + " not found in " + propMap);

        if (!propMap.values().containsAll(expectedValues))
            throw new Exception("Expected values " + expectedValues + " not found in " + propMap);

        String value = scheduler.getProperty("testCreateAndRemoveTaskGroups_NonMatchingGroupC");
        if (!"33,56,65".equals(value))
            throw new Exception("Missing or unexpected value " + value + " for property.");
    }

    /**
     * Submit a persistent task to run immediately and verify that it runs successfully.
     */
    @Test
    public void testExecute(PrintWriter out) throws Exception {
        Runnable task = new DBIncrementTask("testExecute");
        scheduler.execute(task);

        pollForTableEntry("testExecute", 1);
    }

    /**
     * Schedule persistent tasks for the distant future and query for them based on their id.
     */
    @Test
    public void testFindById(PrintWriter out) throws Exception {
        Runnable taskA = new DBIncrementTask("testFindById-A");
        Callable<Integer> taskB = new DBIncrementTask("testFindById-B");

        TaskStatus<?> statusA = scheduler.schedule(taskA, 11, TimeUnit.HOURS);
        TaskStatus<Integer> statusB = scheduler.schedule(taskB, 12, TimeUnit.DAYS);

        final long idA = statusA.getTaskId();
        final long idB = statusB.getTaskId();

        if (idB != idA + 1)
            throw new Exception("Should generate sequential unique identifiers, not " + idA + ", " + idB);

        TaskStatus<?> statusAA = scheduler.getStatus(idA);
        if (!statusA.equals(statusAA))
            throw new Exception("Status for taskA does not match: " + statusA + " *** " + statusAA);

        TaskStatus<?> statusAAA = unmanagedExecutor.submit(new Callable<TaskStatus<?>>() {
            @Override
            public TaskStatus<?> call() throws Exception {
                Thread.currentThread().setContextClassLoader(null);
                return scheduler.getStatus(idA);
            }
        }).get();
        if (statusAAA != null)
            throw new Exception("Should not be able to find the task from a non-application thread. Result: " + statusAAA);

        final TaskStatus<Integer> statusBB = scheduler.getStatus(idB);
        if (!statusB.equals(statusBB))
            throw new Exception("Status for taskB does not match: " + statusB + " *** " + statusBB);

        TaskStatus<?> statusN1 = scheduler.getStatus(-1);
        if (statusN1 != null)
            throw new Exception("Shouldn't find any status for task id -1 given that sequences start at 1");

        if (statusAA.equals(statusBB))
            throw new Exception("Status should not match for different tasks: " + statusAA + " *** " + statusBB);

        long hours = statusAA.getDelay(TimeUnit.HOURS);
        if (hours < 10 || hours > 11)
            throw new Exception("Unexpected delay: " + hours + " hours");

        Date nextExecTimeA = statusAA.getNextExecutionTime();
        hours = TimeUnit.MILLISECONDS.toHours(nextExecTimeA.getTime() - System.currentTimeMillis());
        if (hours < 10 || hours > 11)
            throw new Exception("Unexpected delay: " + hours + " hours computed from next execution time: " + nextExecTimeA);

        long days = statusBB.getDelay(TimeUnit.DAYS);
        if (days < 11 || days > 12)
            throw new Exception("Unexpected delay: " + days + " days");

        hours = statusBB.getDelay(TimeUnit.HOURS);
        if (hours < 287 || hours > 288)
            throw new Exception("Unexpected converted delay: " + hours + " hours");

        Date nextExecTimeB = statusBB.getNextExecutionTime();
        hours = TimeUnit.MILLISECONDS.toHours(nextExecTimeB.getTime() - System.currentTimeMillis());
        if (hours < 287 || hours > 288)
            throw new Exception("Unexpected delay of " + hours + " hours computed from next execution time: " + nextExecTimeB);

        int comparison = statusA.compareTo(statusA);
        if (comparison != 0)
            throw new Exception("TaskA should compare equally to itself, not " + comparison);

        comparison = statusA.compareTo(statusAA);
        if (comparison != 0)
            throw new Exception("TaskA should compare equally to the subsequent snapshot, not " + comparison);

        comparison = statusB.compareTo(statusBB);
        if (comparison != 0)
            throw new Exception("TaskB should compare equally to the subsequent snapshot, not " + comparison);

        comparison = statusAA.compareTo(statusBB);
        if (comparison >= 0)
            throw new Exception("TaskA should compare less than taskB. Instead: " + comparison);

        comparison = statusBB.compareTo(statusAA);
        if (comparison <= 0)
            throw new Exception("TaskB should compare greater than taskA. Instead: " + comparison);

        Delayed twentyHourDelay = new Delayed() {
            @Override
            public int compareTo(Delayed o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getDelay(TimeUnit unit) {
                return unit.convert(20, TimeUnit.HOURS);
            }
        };

        comparison = statusAA.compareTo(twentyHourDelay);
        if (comparison >= 0)
            throw new Exception("TaskA should compare less than a Delayed for 20 hours. Instead: " + comparison);

        comparison = statusBB.compareTo(twentyHourDelay);
        if (comparison <= 0)
            throw new Exception("TaskB should compare greater than a Delayed for 20 hours. Instead: " + comparison);

        try {
            comparison = statusAA.compareTo(null);
            throw new Exception("Should not be able to compare to null: " + comparison);
        } catch (NullPointerException x) {
        }

        try {
            boolean canceled = statusAA.isCancelled();
            throw new Exception("TaskA shouldn't have canceled status until it ends because our snapshot does not allow for changes to state. " + canceled);
        } catch (IllegalStateException x) {
            if (x.getMessage() == null
                || !x.getMessage().contains("CWWKC1550")
                || !x.getMessage().contains("isCancelled"))
                throw x;
        }

        try {
            boolean canceled = statusBB.isCancelled();
            throw new Exception("TaskB shouldn't have canceled status until it ends because our snapshot does not allow for changes to state. " + canceled);
        } catch (IllegalStateException x) {
        }

        try {
            boolean done = statusAA.isDone();
            throw new Exception("TaskA shouldn't have done status until it ends because our snapshot does not allow for changes to state. " + done);
        } catch (IllegalStateException x) {
            if (x.getMessage() == null
                || !x.getMessage().contains("CWWKC1550")
                || !x.getMessage().contains("isDone"))
                throw x;
        }

        try {
            boolean done = statusBB.isDone();
            throw new Exception("TaskB shouldn't have done status until it ends because our snapshot does not allow for changes to state. " + done);
        } catch (IllegalStateException x) {
        }

        if (statusAA.hasResult())
            throw new Exception("TaskA shouldn't have a result yet");

        if (statusBB.hasResult())
            throw new Exception("TaskB shouldn't have a result yet");

        Object resultA = statusAA.getResult();
        if (resultA != null)
            throw new Exception("TaskA shouldn't have a result yet: " + resultA);

        Integer resultB = statusBB.getResult();
        if (resultB != null)
            throw new Exception("TaskB shouldn't have a result yet: " + resultB);

        // remove the task A and query again

        boolean removed = unmanagedExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return scheduler.remove(idA);
            }
        }).get();
        if (removed)
            throw new Exception("Should only be able to remove from thread associated with the application.");

        if (!scheduler.remove(idA))
            throw new Exception("Failed to remove taskA with ID=" + idA);

        if (scheduler.remove(idA))
            throw new Exception("Should not be able to remove taskA with ID=" + idA + " twice");

        Date time = statusA.getNextExecutionTime();
        if (time == null || !time.equals(nextExecTimeA))
            throw new Exception("Original snapshot of taskA with ID=" + idA + " should be unimpacted by removal: " + statusA);

        time = statusAA.getNextExecutionTime();
        if (time == null || !time.equals(nextExecTimeA))
            throw new Exception("Second snapshot of taskA with ID=" + idA + " should be unimpacted by removal: " + statusAA);

        TaskStatus<?> statusAAAA = scheduler.getStatus(idA);
        if (statusAAAA != null)
            throw new Exception("Entry for taskA with ID=" + idA + " should have been removed. Instead: " + statusAAAA);

        // cancel task B and query again

        boolean canceled = unmanagedExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return statusBB.cancel(false);
            }
        }).get();
        if (!canceled)
            throw new Exception("Unable to cancel taskB with ID=" + idB);

        if (statusBB.cancel(true))
            throw new Exception("Should not be able to cancel taskB with ID=" + idB + " twice");

        final TaskStatus<Integer> statusBBB = scheduler.getStatus(idB);
        if (!statusBBB.isCancelled())
            throw new Exception("Status of taskB with ID=" + idB + " should be canceled. Instead " + statusBBB);

        if (!statusBBB.isDone())
            throw new Exception("Status of taskB with ID=" + idB + " should be done. Instead " + statusBBB);

        try {
            Integer result = statusBBB.get();
            throw new Exception("Unexpected result of taskB with ID=" + idB + ". Result= " + result + ", " + statusBBB);
        } catch (CancellationException x) {
        }

        if (!scheduler.remove(idB))
            throw new Exception("Should be able to remove canceled entry with for taskB with ID=" + idB + " because cancel should not have autopurged it.");

        time = statusBB.getNextExecutionTime();
        if (time == null || !time.equals(nextExecTimeB))
            throw new Exception("Snapshot of TaskB should not be impacted. " + statusBB);

        time = statusB.getNextExecutionTime();
        if (time == null || !time.equals(nextExecTimeB))
            throw new Exception("Original snapshot of TaskB should not be impacted. " + statusB);
    }

    /**
     * Schedule persistent tasks for the distant future and query for them based on their name.
     */
    @Test
    public void testFindByName(PrintWriter out) throws Exception {
        Runnable taskA = new DBIncrementTask("testFindByName-A_a");
        Callable<Integer> taskB = new DBIncrementTask("testFindByName-B%b");
        Callable<Integer> taskC = new DBIncrementTask("testFindByName-C\\c");
        Callable<Integer> taskD = new DBIncrementTask("testFindByName-D_d");
        ((ManagedTask) taskB).getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.toString());
        ((ManagedTask) taskC).getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        ((ManagedTask) taskD).getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());

        TaskStatus<?> statusA = scheduler.schedule(taskA, 35, TimeUnit.HOURS);
        TaskStatus<Integer> statusB = scheduler.schedule(taskB, 36, TimeUnit.DAYS);
        TaskStatus<Integer> statusC = scheduler.schedule(taskC, TimeUnit.MILLISECONDS.toNanos(37) - 1, TimeUnit.NANOSECONDS);
        TaskStatus<?> statusD = scheduler.submit(taskD);

        // Look for canceled tasks, should be none
        String pattern = DBIncrementTask.class.getSimpleName() + "-testFindByName-___";
        List<TaskStatus<?>> results = scheduler.findTaskStatus(pattern, '\\', TaskState.CANCELED, true, 0L, 110);
        Map<Long, TaskStatus<?>> resultsByTaskId = getResultsByTaskIdAndClose(results);
        if (!resultsByTaskId.isEmpty())
            throw new Exception("Should not find any canceled tasks. Instead: " + resultsByTaskId);

        // Look for non-canceled tasks, should be all
        pattern = DBIncrementTask.class.getSimpleName() + "-testFindByName-___";
        tran.begin();
        try {
            results = scheduler.findTaskStatus(pattern, '\\', TaskState.CANCELED, false, null, 120);
            resultsByTaskId = getResultsByTaskIdAndClose(results);
        } finally {
            tran.commit();
        }
        if (resultsByTaskId.size() != 4)
            throw new Exception("Should find exactly 4 non-canceled tasks. Instead: " + resultsByTaskId);

        // Compare some values in the task status
        TaskStatus<?> statusAA = resultsByTaskId.get(statusA.getTaskId());
        long delay = statusAA.getDelay(TimeUnit.HOURS);
        if (delay < 34 || delay > 35)
            throw new Exception("Unexpected delay " + delay + " for Task A " + statusAA);

        TaskStatus<?> statusBB = resultsByTaskId.get(statusB.getTaskId());
        int comparison = statusAA.compareTo(statusBB);
        if (comparison >= 0)
            throw new Exception("Status A should compare as less than Status B. Instead: " + comparison + ". " + statusAA + " " + statusBB);

        if (!statusA.equals(statusAA) || !statusAA.equals(statusA))
            throw new Exception("Initial status for task A " + statusA + " does not equal new status " + statusAA);

        try {
            boolean canceled = statusAA.isCancelled();
            throw new Exception("Should not have canceled status " + canceled
                                + " for task that hasn't ever run, because our snapshot implementation would be unable to deal with changes " + statusAA);
        } catch (IllegalStateException x) {
        }

        if (statusAA.hasResult())
            throw new Exception("Task A shouldn't have a result: " + statusAA);

        try {
            Object result = statusAA.get();
            throw new Exception("Should not get result " + result + " for task that hasn't ever run " + statusAA);
        } catch (IllegalStateException x) {
            if (!x.getMessage().contains("CWWKC1551E"))
                throw x;
        }

        // Use an escape character to search for a literal %
        pattern = DBIncrementTask.class.getSimpleName() + "-testFindByName-_\\%_";
        results = scheduler.findTaskStatus(pattern, '\\', TaskState.ANY, true, -1L, 1100);
        resultsByTaskId = getResultsByTaskIdAndClose(results);
        if (resultsByTaskId.size() != 1
            || !resultsByTaskId.containsKey(statusB.getTaskId()))
            throw new Exception("Expecting task " + statusB + " for findTaskStatus containing literal %. Instead: " + resultsByTaskId);

        // Use an escape character to search for a literal _
        pattern = DBIncrementTask.class.getSimpleName() + "-testFindByName-_@__";
        results = scheduler.findTaskStatus(pattern, '@', TaskState.SKIPRUN_FAILED, false, Long.MIN_VALUE, 1200);
        resultsByTaskId = getResultsByTaskIdAndClose(results);
        if (resultsByTaskId.size() != 2
            || !resultsByTaskId.containsKey(statusA.getTaskId())
            || !resultsByTaskId.containsKey(statusD.getTaskId()))
            throw new Exception("Expecting tasks " + statusA.getTaskId() + " and " + statusD.getTaskId() + " for findTaskStatus containing literal _. Instead: " + resultsByTaskId);

        // find task IDs only
        List<Long> ids = scheduler.findTaskIds(pattern, '@', TaskState.SUSPENDED, false, 0L, 500);
        if (ids.size() != 2)
            throw new Exception("Expecting tasks " + statusA.getTaskId() + " and " + statusD.getTaskId() + " for findTaskIds containing literal _. Instead: " + ids);

        if (!ids.containsAll(resultsByTaskId.keySet()))
            throw new Exception("Task ids " + resultsByTaskId + " do not match IDs from equivalent status " + resultsByTaskId);

        // Tasks should be invisible outside of the application
        resultsByTaskId = unmanagedExecutor.submit(new Callable<Map<Long, TaskStatus<?>>>() {
            @Override
            public Map<Long, TaskStatus<?>> call() throws Exception {
                Thread.currentThread().setContextClassLoader(null);
                String pattern = DBIncrementTask.class.getSimpleName() + "-testFindByName-___";
                List<TaskStatus<?>> results = scheduler.findTaskStatus(pattern, '\\', TaskState.CANCELED, false, 1110L, null);
                return getResultsByTaskIdAndClose(results);
            }
        }).get();
        if (!resultsByTaskId.isEmpty())
            throw new Exception("Should not be able to see tasks from outside of application. " + resultsByTaskId);

        // Wait for tasks C and D to complete their only execution.
        pollForResult(statusC.getTaskId(), 1);
        pollForResult(statusD.getTaskId(), 1);

        // Look for successfully completed tasks, should be 2 (C,D)
        pattern = DBIncrementTask.class.getSimpleName() + "-testFindByName-%";
        results = scheduler.findTaskStatus(pattern, 'Z', TaskState.SUCCESSFUL, true, -200l, 400);
        resultsByTaskId = getResultsByTaskIdAndClose(results);
        if (resultsByTaskId.size() != 2
            || !resultsByTaskId.containsKey(statusC.getTaskId())
            || !resultsByTaskId.containsKey(statusD.getTaskId()))
            throw new Exception("Expecting tasks " + statusC.getTaskId() + " and " + statusD.getTaskId() + " for findTaskStatus(SUCCESSFUL,true). Instead: " + resultsByTaskId);

        // Look for unattempted tasks, should be 2 (A,B)
        pattern = DBIncrementTask.class.getSimpleName() + "-testFindByName-%";
        results = scheduler.findTaskStatus(pattern, null, TaskState.UNATTEMPTED, true, null, Integer.MAX_VALUE);
        resultsByTaskId = getResultsByTaskIdAndClose(results);
        if (resultsByTaskId.size() != 2
            || !resultsByTaskId.containsKey(statusA.getTaskId())
            || !resultsByTaskId.containsKey(statusB.getTaskId()))
            throw new Exception("Expecting tasks " + statusA.getTaskId() + " and " + statusB.getTaskId() + " for findTaskStatus(UNATTEMPTED,true). Instead: " + resultsByTaskId);

        // Look for not ended tasks, should be 2 (A,B)
        pattern = DBIncrementTask.class.getSimpleName() + "--testFindByName--%";
        results = scheduler.findTaskStatus(pattern, '-', TaskState.ENDED, false, null, null);
        resultsByTaskId = getResultsByTaskIdAndClose(results);
        if (resultsByTaskId.size() != 2
            || !resultsByTaskId.containsKey(statusA.getTaskId())
            || !resultsByTaskId.containsKey(statusB.getTaskId()))
            throw new Exception("Expecting tasks " + statusA.getTaskId() + " and" + statusB.getTaskId() + " for findByName(ENDED,false). Instead: " + resultsByTaskId);

        // Look for all non-skipped tasks in table, should be at least 4, and include (A,B,C,D)
        pattern = "%";
        results = scheduler.findTaskStatus(pattern, '^', TaskState.SKIPPED, false, null, 1220);
        resultsByTaskId = getResultsByTaskIdAndClose(results);
        if (resultsByTaskId.size() < 4
            || !resultsByTaskId.containsKey(statusA.getTaskId())
            || !resultsByTaskId.containsKey(statusB.getTaskId())
            || !resultsByTaskId.containsKey(statusC.getTaskId())
            || !resultsByTaskId.containsKey(statusD.getTaskId()))
            throw new Exception("Expecting at least 4 tasks for findTaskStatus(%,SKIPPED,false). Instead: " + resultsByTaskId);

        // find task IDs
        ids = scheduler.findTaskIds(pattern, '^', TaskState.SKIPPED, false, null, null);
        if (ids.size() < 4)
            throw new Exception("Expecting at least 4 task ids for findTaskIds(%,SKIPPED,false). Instead: " + ids);
        if (!ids.containsAll(resultsByTaskId.keySet()))
            throw new Exception("Task ids " + resultsByTaskId + " do not match ids from equivalent status " + resultsByTaskId);

        // first 3 tasks ids task IDs
        List<Long> subset1 = scheduler.findTaskIds(pattern, '^', TaskState.SKIPPED, false, null, 3);
        if (subset1.size() != 3)
            throw new Exception("Expecting only 3 task ids when max results is 3. Instead: " + subset1);
        if (!ids.containsAll(subset1))
            throw new Exception("Elements of first subset " + subset1 + " are not all found within " + ids);
        Iterator<Long> iterator = subset1.iterator();
        long id1 = iterator.next();
        long id2 = iterator.next();
        long id3 = iterator.next();
        if (id1 > id2 || id1 > id3 || id2 > id3)
            throw new Exception("findTaskIds is not ordered by task id. Instead: " + subset1);

        // second subset should have at least 1
        List<Long> subset2 = scheduler.findTaskIds(pattern, '^', TaskState.SKIPPED, false, id3 + 1, 3);
        if (subset2.size() < 1)
            throw new Exception("Expecting at least 1 task id in second subset.");
        if (!ids.containsAll(subset2))
            throw new Exception("Elements of second subset " + subset2 + " are not all found within " + ids);
        Set<Long> intersection = new HashSet<Long>(subset1);
        intersection.retainAll(subset2);
        if (!intersection.isEmpty())
            throw new Exception("Subsets of first 3 " + subset1 + " and next 3 " + subset2 + " should not overlap. " + intersection);

        // Verify the results in the task status for completed tasks
        TaskStatus<?> statusCC = resultsByTaskId.get(statusC.getTaskId());
        if (!statusCC.isDone() || statusCC.isCancelled())
            throw new Exception("Unexpected state of task C " + statusCC);
        Object value = statusCC.get();
        if (!Integer.valueOf(1).equals(value))
            throw new Exception("Unexpected result " + value + " for task C " + statusCC);
        Date nextExecTimeCC = statusCC.getNextExecutionTime();
        if (nextExecTimeCC != null)
            throw new Exception("Unexpected next execution time " + nextExecTimeCC + " for task C " + statusCC);

        TaskStatus<?> statusDD = resultsByTaskId.get(statusD.getTaskId());
        if (!statusDD.isDone() || statusDD.isCancelled())
            throw new Exception("Unexpected state of task D " + statusDD);
        value = statusDD.get();
        if (!Integer.valueOf(1).equals(value))
            throw new Exception("Unexpected result " + value + " for task D " + statusDD);
        Date nextExecTimeDD = statusDD.getNextExecutionTime();
        if (nextExecTimeDD != null)
            throw new Exception("Unexpected next execution time " + nextExecTimeDD + " for task D " + statusDD);

        tran.begin();
        try {
            // Look for ended tasks, should be 2 (C,D)
            pattern = DBIncrementTask.class.getSimpleName() + "-testFindByName-%";
            results = scheduler.findTaskStatus(pattern, '~', TaskState.ENDED, true, -1L, null);
            resultsByTaskId = getResultsByTaskIdAndClose(results);
            if (resultsByTaskId.size() != 2
                || !resultsByTaskId.containsKey(statusC.getTaskId())
                || !resultsByTaskId.containsKey(statusD.getTaskId()))
                throw new Exception("Expecting tasks " + statusC + " and " + statusD + " for findTaskStatus(ENDED,true). Instead: " + resultsByTaskId);

            // Look for all non-failed tasks in table, should (A,B,C,D). Remove each of them.
            pattern = DBIncrementTask.class.getSimpleName() + "-testFindByName-%";
            results = scheduler.findTaskStatus(pattern, '!', TaskState.FAILURE_LIMIT_REACHED, false, Long.MIN_VALUE, Integer.MAX_VALUE);
            resultsByTaskId = new LinkedHashMap<Long, TaskStatus<?>>();
            for (TaskStatus<?> taskStatus : results) {
                long taskId = taskStatus.getTaskId();
                resultsByTaskId.put(taskId, taskStatus);
                if (taskId == statusA.getTaskId() || taskId == statusB.getTaskId()) {
                    if (!taskStatus.cancel(false))
                        throw new Exception("Failed to cancel task " + taskStatus);
                } else {
                    if (!scheduler.remove(taskStatus.getTaskId()))
                        throw new Exception("Failed to remove task " + taskStatus);
                }
            }
            if (resultsByTaskId.size() != 4
                || !resultsByTaskId.containsKey(statusA.getTaskId())
                || !resultsByTaskId.containsKey(statusB.getTaskId())
                || !resultsByTaskId.containsKey(statusC.getTaskId())
                || !resultsByTaskId.containsKey(statusD.getTaskId()))
                throw new Exception("Expecting all 4 tasks for findTaskStatus(FAILURE_LIMIT_REACHED,false). Instead: " + resultsByTaskId);
        } finally {
            tran.commit();
        }

        tran.begin();
        try {
            // Look for all scheduled tasks, should be none because we removed or canceled them above
            pattern = DBIncrementTask.class.getSimpleName() + "-testFindByName-%";
            results = scheduler.findTaskStatus(pattern, '*', TaskState.SCHEDULED, true, 0L, 11000);
            resultsByTaskId = getResultsByTaskIdAndClose(results);
            if (!resultsByTaskId.isEmpty())
                throw new Exception("Expecting no tasks after remove. Instead: " + resultsByTaskId);
        } finally {
            tran.commit();
        }

        // Look for canceled tasks. Should only have A which did not autopurge.
        pattern = DBIncrementTask.class.getSimpleName() + "-testFindByName-%";
        results = scheduler.findTaskStatus(pattern, '#', TaskState.CANCELED, true, 0L, 12000);
        resultsByTaskId = getResultsByTaskIdAndClose(results);
        if (resultsByTaskId.size() != 1
            || !resultsByTaskId.containsKey(statusA.getTaskId()))
            throw new Exception("Expecting only task A " + statusA.getTaskId() + " to be canceled. Instead: " + resultsByTaskId);
    }

    /**
     * Find every task for this application that is in the persistent store.
     */
    @Test
    public void testFindEveryTask(PrintWriter out) throws Exception {
        TaskStatus<?> status = scheduler.schedule((Runnable) new DBIncrementTask("testFindEveryTaskId"), 68, TimeUnit.HOURS);
        List<Long> taskIds = scheduler.findTaskIds(null, null, TaskState.ANY, true, null, null);
        if (!taskIds.contains(status.getTaskId()))
            throw new Exception("Task id for " + status + " not found in list of all: " + taskIds);

        // remove every task, but then roll back
        tran.begin();
        try {
            int numRemoved = scheduler.remove(null, null, TaskState.ANY, true);
            if (numRemoved < 1)
                throw new Exception("At least one task " + status + " should have been removed. " + numRemoved);
        } finally {
            tran.rollback();
        }

        List<TaskStatus<?>> statusList = scheduler.findTaskStatus(null, null, TaskState.ANY, true, null, null);
        if (!statusList.contains(status))
            throw new Exception(status + " not found in list of all: " + statusList);

        List<TimerStatus<?>> timerStatusList = ((TimersPersistentExecutor) scheduler).findTimerStatus("schedtest", null, null, TaskState.ANY, true, null, null);
        if (!timerStatusList.contains(status))
            throw new Exception(status + " not found in list of all timer status: " + statusList);
    }

    /**
     * Submit a persistent task to run immediately and verify that it runs successfully.
     */
    @Test
    public void testImmediateCallable(PrintWriter out) throws Exception {
        ManagedTriggerTask task = new ManagedTriggerTask();
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        task.getExecutionProperties().put(PersistentExecutor.TRANSACTION_TIMEOUT, "600");
        TaskStatus<Integer> status = scheduler.submit(task);

        try {
            boolean canceled = status.isCancelled();
            throw new Exception("Task should not have canceled status " + canceled + " until it ends.");
        } catch (IllegalStateException x) {
        }

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        if (status.hasResult())
            throw new Exception("Task status initial snapshot should not have a result. " + status);

        if (!status.equals(status))
            throw new Exception("Task status does not equal itself");

        String toString = status.toString();
        if (!toString.contains("SCHEDULED"))
            throw new Exception("toString output does not contain the state of the task. Instead: " + toString);

        TaskStatus<Integer> updatedStatus = pollForResult(status.getTaskId(), 1);

        if (updatedStatus == null || !updatedStatus.isDone())
            throw new Exception("Task not completed in allotted interval. Status: " + status);

        Integer result = updatedStatus.get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected result: " + result);

        long delay = updatedStatus.getDelay(TimeUnit.NANOSECONDS);
        if (delay > 0)
            throw new Exception("Delay should never be positive for a completed task. " + updatedStatus);

        if (updatedStatus.cancel(true))
            throw new Exception("Should not be able to cancel a completed task.");

        if (updatedStatus.isCancelled())
            throw new Exception("Task should not have been canceled.");

        result = updatedStatus.get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Result should not change: " + result);
    }

    /**
     * Submit a persistent task to run immediately and verify that it runs successfully.
     */
    @Test
    public void testImmediateRunnable(PrintWriter out) throws Exception {
        Runnable task = new DBIncrementTask("testImmediateRunnable");
        Future<?> status = scheduler.submit(task);

        try {
            boolean canceled = status.isCancelled();
            throw new Exception("Task should not have canceled status " + canceled + " until it ends.");
        } catch (IllegalStateException x) {
        }

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        if (!status.equals(status))
            throw new Exception("Task status does not equal itself");

        String toString = status.toString();
        if (!toString.contains("DBIncrementTask-testImmediateRunnable"))
            throw new Exception("toString output does not contain the IDENTITY_NAME of task. Instead: " + toString);

        pollForTableEntry("testImmediateRunnable", 1);
    }

    /**
     * Submit a persistent task to run immediately and verify that it runs successfully and returns the specified result.
     */
    @Test
    @AllowedFFDC("javax.persistence.RollbackException")
    public void testImmediateRunnableWithResult(PrintWriter out) throws Exception {
        String unicode = "\u215C";
        String taskKey = "testImmediateRunnableWithResult-2" + unicode;

        DBIncrementTask task = new DBIncrementTask(taskKey);
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        TaskStatus<CustomResult> status;
        try {
            status = scheduler.submit(task, new CustomResult(5, 55));
        } catch (RuntimeException x) {
            if (x.getClass().getName().equals("javax.persistence.RollbackException")
                && x.getStackTrace()[0].toString().contains("UnicodeFilteringPreparedStatement")) {
                // If unicode is not supported, resubmit the task without unicode,
                taskKey = "testImmediateRunnableWithResult-UnicodeNotSupported";
                task = new DBIncrementTask(taskKey);
                task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
                status = scheduler.submit(task, new CustomResult(5, 55));
            } else
                throw x;
        }

        try {
            boolean canceled = status.isCancelled();
            throw new Exception("Task should not have canceled status " + canceled + " until it ends.");
        } catch (IllegalStateException x) {
        }

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        if (status.hasResult())
            throw new Exception("Task status initial snapshot should not have a result. " + status);

        if (!status.equals(status))
            throw new Exception("Task status does not equal itself");

        String toString = status.toString();
        if (!toString.contains("DBIncrementTask-" + taskKey))
            throw new Exception("toString output does not contain the IDENTITY_NAME of task. Instead: " + toString);

        pollForTableEntry(taskKey, 1);

        // Query with a unicode character
        List<TaskStatus<?>> statusList = scheduler.findTaskStatus("%-" + taskKey, '\\', TaskState.ANY, true, null, null);
        if (statusList.size() != 1)
            throw new Exception("Expecting exactly one result. Instead: " + statusList);

        @SuppressWarnings("unchecked")
        TaskStatus<CustomResult> statusUpdate = (TaskStatus<CustomResult>) statusList.iterator().next();

        String str = statusUpdate.toString();
        if (!str.contains(taskKey))
            throw new Exception("toString did not match expected character sequence with unicode. Status: " + str
                                + " Code point: " + str.codePointAt(str.length() - 1));

        Object expectedResult = new CustomResult(5, 55);

        CustomResult result1 = statusUpdate.get();
        if (!expectedResult.equals(result1))
            throw new Exception("Unexpected fixed result for Runnable: " + result1);

        // Change the result and ensure it has no impact on the next get/getResult attempt
        result1.part1 = 1;
        result1.part2 = 1;

        CustomResult result2 = statusUpdate.get();
        if (!expectedResult.equals(result2))
            throw new Exception("Result must not change for Runnable: " + result2);

        if (result1 == result2)
            throw new Exception("Same result instance should not be returned by multiple calls to get: " + result1);

        if (statusUpdate.isCancelled())
            throw new Exception("Submitted runnable should not have been canceled. " + statusUpdate);

        if (!statusUpdate.isDone())
            throw new Exception("Submitted runnable should report done after one execution. " + statusUpdate);

        long delay = statusUpdate.getDelay(TimeUnit.NANOSECONDS);
        if (delay > 0)
            throw new Exception("Delay must be 0 or negative or one-shot task that already executed. Instead: " + delay);
    }

    /**
     * Schedule a basic one-shot persistent task and verify that it runs successfully.
     */
    @Test
    public void testOneShotCallable(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testOneShotCallable");
        task.getExecutionProperties().put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);
        task.getExecutionProperties().put(PersistentExecutor.TRANSACTION_TIMEOUT, "1010");
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, 101, TimeUnit.MILLISECONDS);

        try {
            boolean canceled = status.isCancelled();
            throw new Exception("Task should not have canceled status " + canceled + " until it ends.");
        } catch (IllegalStateException x) {
        }

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        if (!status.equals(status))
            throw new Exception("Task status does not equal itself");

        String toString = status.toString();
        if (!toString.contains("DBIncrementTask-testOneShotCallable"))
            throw new Exception("toString output does not contain the IDENTITY_NAME of task. Instead: " + toString);

        pollForTableEntry("testOneShotCallable", 1);

        // Wait for completion (and autopurge)
        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (status != null)
            throw new Exception("Task did not complete and autopurge in allotted interval. " + status);
    }

    /**
     * Schedule a basic one-shot persistent task and verify that it runs successfully.
     */
    @Test
    public void testOneShotRunnable(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testOneShotRunnable");
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        task.getExecutionProperties().put(PersistentExecutor.TRANSACTION_TIMEOUT, "0"); // 0 means default transaction timeout
        TaskStatus<?> status = scheduler.schedule((Runnable) task, 102, TimeUnit.MILLISECONDS);

        try {
            boolean canceled = status.isCancelled();
            throw new Exception("Task should not have canceled status " + canceled + " until it ends.");
        } catch (IllegalStateException x) {
        }

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        if (status.hasResult())
            throw new Exception("Task status initial snapshot should not have a result. " + status);

        if (!status.equals(status))
            throw new Exception("Task status does not equal itself");

        String toString = status.toString();
        if (!toString.contains("DBIncrementTask-testOneShotRunnable"))
            throw new Exception("toString output does not contain the IDENTITY_NAME of task. Instead: " + toString);

        pollForTableEntry("testOneShotRunnable", 1);

        status = scheduler.getStatus(status.getTaskId());

        String name = status.getTaskName();
        if (!"DBIncrementTask-testOneShotRunnable".equals(name))
            throw new Exception("Unexpected task name: " + name);

        Object result = status.get();
        if (result != null)
            throw new Exception("Result must be null for Runnable. Instead: " + result);

        result = status.get();
        if (result != null)
            throw new Exception("Result must still be null for Runnable. Instead: " + result);

        if (!status.isDone())
            throw new Exception("One-shot runnable should report done after one execution. " + status);

        if (status.isCancelled())
            throw new Exception("One-shot runnable should not have been canceled. " + status);

        long delay = status.getDelay(TimeUnit.NANOSECONDS);
        if (delay > 0)
            throw new Exception("Delay must be 0 or negative or one-shot task that already executed. Instead: " + delay);
    }

    /**
     * Remove tasks based on their name and state.
     */
    @Test
    public void testRemoveByName(PrintWriter out) throws Exception {
        DBIncrementTask taskA = new DBIncrementTask("testRemoveByName-A");
        TaskStatus<Integer> statusA = scheduler.schedule((Callable<Integer>) taskA, 38, TimeUnit.DAYS);
        System.out.println("Task A is " + statusA.getTaskId());

        // Match name, but don't match state
        String pattern = DBIncrementTask.class.getSimpleName() + "-testRemoveByName-A";
        int numRemoved = scheduler.remove(pattern, '\\', TaskState.ENDED, true);
        if (numRemoved != 0)
            throw new Exception("remove(task A, ENDED) should not remove any tasks. Instead " + numRemoved);

        // Match state, but don't match name
        pattern = DBIncrementTask.class.getSimpleName() + "-testRemoveByName-B";
        numRemoved = scheduler.remove(pattern, '\\', TaskState.SCHEDULED, true);
        if (numRemoved != 0)
            throw new Exception("remove(non-existent task name, SCHEDULED) should not remove any tasks. Instead " + numRemoved);

        DBIncrementTask taskB = new DBIncrementTask("testRemoveByName-B");
        taskB.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        TaskStatus<Integer> statusB = scheduler.schedule((Callable<Integer>) taskB, 39, TimeUnit.HOURS);
        System.out.println("Task B is " + statusB.getTaskId());
        if (!statusB.cancel(true))
            throw new Exception("Failed to cancel task B " + statusB);

        DBIncrementTask taskC = new DBIncrementTask("testRemoveByName-C");
        taskC.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        TaskStatus<Integer> statusC = scheduler.schedule((Callable<Integer>) taskC, 40, TimeUnit.MINUTES);
        System.out.println("Task C is " + statusC.getTaskId());
        if (!statusC.cancel(true))
            throw new Exception("Failed to cancel task C " + statusC);

        // Match name and state, in a transaction, and then roll back
        tran.begin();
        try {
            pattern = DBIncrementTask.class.getSimpleName() + "-testRemoveByName-B";
            numRemoved = scheduler.remove(pattern, '\\', TaskState.CANCELED, true);
            if (numRemoved != 1)
                throw new Exception("remove(task B, CANCELED) ought to remove 1 task, not " + numRemoved);
        } finally {
            tran.rollback();
        }

        // Match by absence of state, in a transaction, and then commit
        tran.begin();
        try {
            pattern = DBIncrementTask.class.getSimpleName() + "-testRemoveByName-%";
            numRemoved = scheduler.remove(pattern, null, TaskState.ENDED, false);
            if (numRemoved != 1)
                throw new Exception("remove(not ENDED) ought to remove 1 task, not " + numRemoved);
        } finally {
            tran.commit();
        }

        DBIncrementTask taskD = new DBIncrementTask("testRemoveByName-D");
        TaskStatus<Integer> statusD = scheduler.schedule((Callable<Integer>) taskD, 41, TimeUnit.DAYS);
        System.out.println("Task D is " + statusD.getTaskId());

        DBIncrementTask taskE = new DBIncrementTask("testRemoveByName-E");
        TaskStatus<Integer> statusE = scheduler.schedule((Callable<Integer>) taskE, 42, TimeUnit.HOURS);
        System.out.println("Task E is " + statusE.getTaskId());

        // At this point, tasks in the table should be:
        // Task B: ENDED CANCELED
        // Task C: ENDED CANCELED
        // Task D: SCHEDULED UNATTEMPTED
        // Task E: SCHEDULED UNATTEMPTED

        // Match by name and state, no escape character
        pattern = DBIncrementTask.class.getSimpleName() + "-testRemoveByName-E%";
        numRemoved = scheduler.remove(pattern, null, TaskState.SCHEDULED, true);
        if (numRemoved != 1)
            throw new Exception("remove(task E, SCHEDULED) ought to remove 1 task, not " + numRemoved);

        // Match by state, use escape character
        pattern = DBIncrementTask.class.getSimpleName() + "--testRemoveByName--_";
        numRemoved = scheduler.remove(pattern, '-', TaskState.CANCELED, true);
        if (numRemoved != 2)
            throw new Exception("remove(CANCELED) ought to remove 2 tasks, not " + numRemoved);

        DBIncrementTask taskF = new DBIncrementTask("testRemoveByName-F");
        TaskStatus<Integer> statusF = scheduler.schedule((Callable<Integer>) taskF, 43, TimeUnit.HOURS);
        System.out.println("Task F is " + statusF.getTaskId());

        DBIncrementTask taskG = new DBIncrementTask("testRemoveByName-G");
        taskC.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        TaskStatus<Integer> statusG = scheduler.schedule((Callable<Integer>) taskG, 44, TimeUnit.MINUTES);
        System.out.println("Task G is " + statusG.getTaskId());
        if (!statusG.cancel(true))
            throw new Exception("Failed to cancel task G " + statusG);

        // At this point, tasks in the table should be:
        // Task D: SCHEDULED UNATTEMPTED
        // Task F: SCHEDULED UNATTEMPTED
        // Task G: ENDED CANCELED

        // Match ANY state, but don't match name
        pattern = DBIncrementTask.class.getSimpleName() + "-testRemoveByName-__";
        numRemoved = scheduler.remove(pattern, null, TaskState.ANY, true);
        if (numRemoved != 0)
            throw new Exception("remove(non matching name, ANY) should not remove any tasks. Instead " + numRemoved);

        // Match ANY state, match all names
        pattern = DBIncrementTask.class.getSimpleName() + "-testRemoveByName-_";
        numRemoved = scheduler.remove(pattern, null, TaskState.ANY, true);
        if (numRemoved != 3)
            throw new Exception("remove(ANY) should remove 3 tasks. Instead " + numRemoved);

        // Match ANY state, match all names, but with no tasks left
        pattern = DBIncrementTask.class.getSimpleName() + "-testRemoveByName-_";
        numRemoved = scheduler.remove(pattern, null, TaskState.ANY, true);
        if (numRemoved != 0)
            throw new Exception("remove(ANY) should remove 0 tasks. Instead " + numRemoved);
    }

    /**
     * Attempt to remove a task while it is running.
     */
    @Test
    public void testRemoveRunningTask(PrintWriter out) throws Exception {
        CancelableTask task = new CancelableTask("testRemoveRunningTask", false);
        String taskName = task.getExecutionProperties().get(ManagedTask.IDENTITY_NAME);
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.name());

        TaskStatus<Integer> status = scheduler.submit((Callable<Integer>) task);

        CancelableTask.waitForStart(taskName);

        boolean removed = scheduler.remove(status.getTaskId());
        if (!removed)
            throw new Exception("Unable to remove task " + status.getTaskId());

        // wait for task result in database
        pollForTableEntry("testRemoveRunningTask", 1);

        status = scheduler.getStatus(status.getTaskId());
        if (status != null)
            throw new Exception("Task entry was not removed " + status);
    }

    /**
     * Attempt to remove a task (which autopurges on successful completion) while it is running.
     */
    @Test
    public void testRemoveRunningTaskAutoPurge(PrintWriter out) throws Exception {
        CancelableTask task = new CancelableTask("testRemoveRunningTaskAutoPurge", false);
        String taskName = task.getExecutionProperties().get(ManagedTask.IDENTITY_NAME);

        TaskStatus<Integer> status = scheduler.submit((Callable<Integer>) task);

        CancelableTask.waitForStart(taskName);

        boolean removed = scheduler.remove(status.getTaskId());
        if (removed)
            throw new Exception("Task should have autopurged on completion " + status.getTaskId());

        // wait for task result in database
        pollForTableEntry("testRemoveRunningTaskAutoPurge", 1);
    }

    /**
     * Remove a task while it is running. The task's transaction must be suspended while running.
     */
    @Test
    public void testRemoveRunningTaskSuspendTransaction(PrintWriter out) throws Exception {
        CancelableTask task = new CancelableTask("testRemoveRunningTaskSuspendTransaction", true);
        String taskName = task.getExecutionProperties().get(ManagedTask.IDENTITY_NAME);
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.name());
        task.getExecutionProperties().put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);

        TaskStatus<Integer> status = scheduler.submit((Callable<Integer>) task);

        CancelableTask.waitForStart(taskName);

        boolean removed = scheduler.remove(status.getTaskId());
        if (!removed)
            throw new Exception("Unable to remove task " + status.getTaskId());

        CancelableTask.notifyTaskCanceled(taskName);

        // wait for task result in database
        pollForTableEntry("testRemoveRunningTaskSuspendTransaction", 1);

        status = scheduler.getStatus(status.getTaskId());
        if (status != null)
            throw new Exception("Task entry did not autopurge " + status);
    }

    /**
     * Schedule a repeating task at a fixed rate. Verify that it runs successfully a few times. Then cancel it.
     */
    @Test
    public void testRepeatAtFixedRate(PrintWriter out) throws Exception {
        Runnable task = new DBIncrementTask("testRepeatAtFixedRate");
        TaskStatus<?> status = scheduler.scheduleAtFixedRate(task, 108, 208, TimeUnit.MILLISECONDS);

        try {
            boolean canceled = status.isCancelled();
            throw new Exception("Task should not have canceled status " + canceled + " until it ends.");
        } catch (IllegalStateException x) {
        }

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        if (status.hasResult())
            throw new Exception("Task status initial snapshot should not have a result. " + status);

        if (!status.equals(status))
            throw new Exception("Task status does not equal itself");

        String toString = status.toString();
        if (!toString.contains("DBIncrementTask-testRepeatAtFixedRate"))
            throw new Exception("toString output does not contain the IDENTITY_NAME of task. Instead: " + toString);

        pollForTableEntryMinimumValue("testRepeatAtFixedRate", 3);

        // Cancel by removal
        if (!scheduler.remove(status.getTaskId()))
            throw new Exception("Unable to remove task. Status is " + status);

        status = scheduler.getStatus(status.getTaskId());
        if (status != null)
            throw new Exception("Task was not removed. " + status);
    }

    /**
     * Schedule a repeating task with a fixed delay. Verify that it runs successfully a few times. Then cancel it.
     */
    @Test
    public void testRepeatWithFixedDelay(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testRepeatWithFixedDelay");
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        TaskStatus<?> status = scheduler.scheduleAtFixedRate(task, -109, 209, TimeUnit.MILLISECONDS);

        try {
            boolean canceled = status.isCancelled();
            throw new Exception("Task should not have canceled status " + canceled + " until it ends.");
        } catch (IllegalStateException x) {
        }

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        if (status.hasResult())
            throw new Exception("Task status initial snapshot should not have a result. " + status);

        if (!status.equals(status))
            throw new Exception("Task status does not equal itself");

        String toString = status.toString();
        if (!toString.contains("DBIncrementTask-testRepeatWithFixedDelay"))
            throw new Exception("toString output does not contain the IDENTITY_NAME of task. Instead: " + toString);

        pollForTableEntryMinimumValue("testRepeatWithFixedDelay", 3);

        if (!status.cancel(false))
            throw new Exception("Unable to cancel task. Status is " + status);

        long taskId = status.getTaskId();
        status = scheduler.getStatus(taskId);

        if (!status.isCancelled())
            throw new Exception("Task should be canceled. " + status);

        if (!status.isDone())
            throw new Exception("Canceled task should be done. " + status);

        status.getDelay(TimeUnit.MICROSECONDS);
        // Cannot enforce any requirements on the delay for a canceled task because the JavaDoc is unclear
        // and Java SE ScheduledFuture implementations continue to return a positive delay after cancellation.

        Date nextExecTime = status.getNextExecutionTime();
        if (nextExecTime != null)
            throw new Exception("Next execution time must be null for canceled task. Instead " + nextExecTime + " for " + status);

        long taskId2 = status.getTaskId();
        if (taskId != taskId2)
            throw new Exception("Task id should not change from " + taskId + " to " + taskId2);

        if (status.cancel(true))
            throw new Exception("Cancel must return false when we have already canceled it.");

        if (!scheduler.remove(taskId))
            throw new Exception("Unable to remove canceled task.");

        TaskStatus<?> statusAfterRemove = scheduler.getStatus(taskId);
        if (statusAfterRemove != null)
            throw new Exception("Should not see status for removed task: " + statusAfterRemove);

        if (status.cancel(true))
            throw new Exception("Should not be able to cancel removed task.");

        if (scheduler.remove(taskId))
            throw new Exception("Should not be able to remove the task twice.");
    }

    /**
     * Schedule a task that runs once immediately, and additional times in the future that we won't wait for.
     * It should be possible to access the result of the first execution, even though subsequent executions
     * have not been attempted.
     */
    @Test
    public void testResultOfFirstExecution(PrintWriter out) throws Exception {
        // Trigger that runs once immediately, and then with 29 minutes before/between the second, third, and fourth executions.
        Trigger trigger = new VaryingRepeatTrigger(0, 29 * 60000, 29 * 60000, 29 * 60000);
        DBIncrementTask task = new DBIncrementTask("testResultOfFirstExecution");
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        // Wait for the first execution to complete
        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (!status.hasResult())
            throw new Exception("Task did not execute within the allotted interval. " + status);

        try {
            Integer result = status.get();
            throw new Exception("Should not be able to obtain result via future.get because future executions might change the result and our snapshot would not be valid. "
                                + result);
        } catch (IllegalStateException x) {
        }

        Integer result = status.getResult();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected result " + result + " for " + status);

        long nextExecutionTime = status.getNextExecutionTime().getTime();
        long delay = TimeUnit.MILLISECONDS.toMinutes(nextExecutionTime - System.currentTimeMillis());
        if (delay < 28 || delay > 29)
            throw new Exception("Unexpected delay " + delay + " minutes for " + status);

        final TimersPersistentExecutor timersExecutor = ((TimersPersistentExecutor) scheduler);
        Date nextExecutionDate = timersExecutor.getNextExecutionTime(status.getTaskId());
        nextExecutionTime = nextExecutionDate.getTime();
        delay = TimeUnit.MILLISECONDS.toMinutes(nextExecutionTime - System.currentTimeMillis());
        if (delay < 28 || delay > 29)
            throw new Exception("Unexpected refreshed delay " + delay + " minutes for " + status);

        if (trigger.skipRun(null, new Date()))
            throw new Exception("Unexpected value for skipRun: true");
        // This isn't exactly the same since we are passing a null LastExecution, but close enough since the first execution
        // runs as soon as it can
        nextExecutionDate = trigger.getNextRunTime(null, new Date());
        nextExecutionTime = nextExecutionDate.getTime();
        delay = TimeUnit.MILLISECONDS.toMinutes(nextExecutionTime - System.currentTimeMillis());
        if (delay < 28 || delay > 29)
            throw new Exception("Unexpected delay " + delay + " minutes from Trigger " + trigger + " for " + status);

        // Use the EJB timer interface that can remove tasks regardless of which application is on the metadata for the thread
        final long taskId = status.getTaskId();
        boolean removed = unmanagedExecutor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return timersExecutor.removeTimer(taskId);
            }
        }).get();
        if (!removed)
            throw new Exception("Failed to remove task from unmanaged thread");
    }

    /**
     * Schedule a task that rolls back on its first execution attempt, but runs successfully the next time,
     * and remains in the persistent store upon completion.
     */
    @Test
    public void testRetryRolledBackTask(PrintWriter out) throws Exception {
        SharedRollbackTask.counter.set(0);
        SharedRollbackTask.execProps.put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        SharedRollbackTask.rollBackOn.add(1l);
        try {
            Callable<Long> task = new SharedRollbackTask();
            TaskStatus<Long> status = scheduler.schedule(task, 19, TimeUnit.NANOSECONDS);

            for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                status = scheduler.getStatus(status.getTaskId());

            if (!status.hasResult())
                throw new Exception("Task did not complete within the allotted interval. " + status);

            Long result = status.get();
            if (!Long.valueOf(2).equals(result))
                throw new Exception("Task should be attempted exactly 2 times (with the first attempting rolling back). Instead " + result);
        } finally {
            SharedRollbackTask.execProps.clear();
            SharedRollbackTask.rollBackOn.clear();
        }
    }

    /**
     * Roll back cancelation of a task.
     */
    @Test
    public void testRollBackCancelation(PrintWriter out) throws Exception {
        Runnable task = new DBIncrementTask("testRollBackCancelation");
        TaskStatus<?> status = scheduler.scheduleAtFixedRate(task, TimeUnit.DAYS.toNanos(14) - 14, TimeUnit.DAYS.toNanos(1), TimeUnit.NANOSECONDS);
        tran.begin();
        try {
            boolean canceled = status.cancel(true);
            if (!canceled)
                throw new Exception("Unable to cancel task " + status);
        } finally {
            tran.rollback();
        }

        status = scheduler.getStatus(status.getTaskId());

        try {
            if (status == null || status.isCancelled())
                throw new Exception("Cancelation should have been rolled back. " + status);
        } catch (IllegalStateException x) {
        }

        if (status.hasResult())
            throw new Exception("Task should not have a result. " + status);

        long nextExecutionTime = status.getNextExecutionTime().getTime();
        long delay = TimeUnit.MILLISECONDS.toDays(nextExecutionTime - System.currentTimeMillis());
        if (delay < 13 || delay > 14)
            throw new Exception("Unexpected delay " + delay + " for " + status);

        boolean canceled = status.cancel(false);
        if (!canceled)
            throw new Exception("Unable to cancel task after previous cancel was rolled back.");
    }

    /**
     * Roll back removal of a task.
     */
    @Test
    public void testRollBackRemoval(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testRollBackRemoval");
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        TaskStatus<?> status = scheduler.scheduleWithFixedDelay(task, 15, 5, TimeUnit.DAYS);
        boolean canceled = status.cancel(true);
        if (!canceled)
            throw new Exception("Unable to cancel task " + status);

        tran.begin();
        try {
            boolean removed = scheduler.remove(status.getTaskId());
            if (!removed)
                throw new Exception("Unable to remove task " + status);
        } finally {
            tran.rollback();
        }

        status = scheduler.getStatus(status.getTaskId());

        if (status == null)
            throw new Exception("Removal should have been rolled back.");

        if (!status.isCancelled() || !status.isDone())
            throw new Exception("Task should be canceled and done. " + status);

        boolean removed = scheduler.remove(status.getTaskId());
        if (!removed)
            throw new Exception("Unable to remove task after previous remove was rolled back.");
    }

    /**
     * Roll back task submission.
     */
    @Test
    public void testRollBackTaskSubmit(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testRollBackTaskSubmit");

        TaskStatus<?> status;

        tran.begin();
        try {
            status = scheduler.schedule((Callable<Integer>) task, 16, TimeUnit.DAYS);
        } finally {
            tran.rollback();
        }

        status = scheduler.getStatus(status.getTaskId());

        if (status != null)
            throw new Exception("Task submission was not rolled back. " + status);
    }

    /**
     * Schedule a task and cancel it within the same transaction.
     */
    @Test
    public void testScheduleAndCancelInSameTransaction(PrintWriter out) throws Exception {
        Callable<Integer> task = new DBIncrementTask("testScheduleAndCancelInSameTransaction");
        TaskStatus<?> status;
        tran.begin();
        try {
            status = scheduler.schedule(task, 17, TimeUnit.DAYS);
            boolean canceled = status.cancel(true);
            if (!canceled)
                throw new Exception("Unable to cancel task " + status);
        } finally {
            tran.commit();
        }

        status = scheduler.getStatus(status.getTaskId());

        if (!status.isCancelled())
            throw new Exception("Task was not canceled. " + status);
    }

    /**
     * Schedule a task that cancels itself when it runs the second time.
     * The task entry should remain in the persistent store.
     */
    @Test
    public void testSelfCancelingTask(PrintWriter out) throws Exception {
        Callable<Integer> task = new SelfCancelingTask("testSelfCancelingTask", 2, false); // Cancel on the second update
        Trigger trigger = new FixedRepeatTrigger(5, 22); // Run up to 5 times, but we should cancel at 2
        TaskStatus<Integer> status = scheduler.schedule(task, trigger);

        pollForTableEntry("testSelfCancelingTask", 2);

        status = scheduler.getStatus(status.getTaskId());

        if (!status.isCancelled())
            throw new Exception("Task was not canceled. " + status);

        try {
            Integer result = status.get();
            throw new Exception("Should not be able to retrieve a result (" + result + ") from a canceled task. " + status);
        } catch (CancellationException x) {
        }

        TimersPersistentExecutor timersExecutor = (TimersPersistentExecutor) scheduler;
        Date nextExecution = timersExecutor.getNextExecutionTime(status.getTaskId());
        if (nextExecution != null)
            throw new Exception("Expecting null getNextExecution for canceled task. Instead: " + nextExecution + ". Status: " + status);

        trigger = timersExecutor.getTimer(status.getTaskId());
        if (trigger != null)
            throw new Exception("Expecting null trigger for canceled task. Instead: " + trigger + ". Status: " + status);
    }

    /**
     * Schedule a task that cancels itself when it runs the third time.
     * The task entry should be removed from the persistent store.
     */
    @Test
    public void testSelfCancelingTaskAndAutoPurge(PrintWriter out) throws Exception {
        SelfCancelingTask task = new SelfCancelingTask("testSelfCancelingTaskAndAutoPurge", 3, false); // Cancel on the third update
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.toString());
        Trigger trigger = new FixedRepeatTrigger(5, 23); // Run up to 5 times, but we should cancel at 3
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        pollForTableEntry("testSelfCancelingTaskAndAutoPurge", 3);

        TaskStatus<Integer> newStatus = scheduler.getStatus(status.getTaskId());

        if (newStatus != null)
            throw new Exception("Task was not autopurged. " + newStatus);

        TimersPersistentExecutor timersExecutor = (TimersPersistentExecutor) scheduler;
        Date nextExecution = timersExecutor.getNextExecutionTime(status.getTaskId());
        if (nextExecution != null)
            throw new Exception("Expecting null getNextExecution for canceled task. Instead: " + nextExecution + ". Original status: " + status);

        trigger = timersExecutor.getTimer(status.getTaskId());
        if (trigger != null)
            throw new Exception("Expecting null trigger for canceled task. Instead: " + trigger + ". Original status: " + status);
    }

    /**
     * Schedule a task that runs with the persistent executor transaction suspended,
     * and cancels itself when it runs the third time.
     * The task entry should remain in the persistent store.
     */
    @Test
    public void testSelfCancelingTaskSuspendTransaction(PrintWriter out) throws Exception {
        SelfCancelingTask task = new SelfCancelingTask("testSelfCancelingTaskSuspendTransaction", 3, false); // Cancel on the third update
        task.getExecutionProperties().put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);
        Trigger trigger = new FixedRepeatTrigger(6, 33); // Run up to 6 times, but we should cancel at 3
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        for (long start = System.nanoTime(); status.getNextExecutionTime() != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (!status.isCancelled())
            throw new Exception("Task was not canceled. " + status);

        pollForTableEntry("testSelfCancelingTaskSuspendTransaction", 3);

        try {
            Integer result = status.get();
            throw new Exception("Should not be able to retrieve a result (" + result + ") from a canceled task. " + status);
        } catch (CancellationException x) {
        }
    }

    /**
     * Schedule a task that runs with the persistent executor transaction suspended,
     * and cancels itself when it runs the second time.
     * The task entry should be removed from the persistent store.
     */
    @Test
    public void testSelfCancelingTaskSuspendTransactionAndAutoPurge(PrintWriter out) throws Exception {
        SelfCancelingTask task = new SelfCancelingTask("testSelfCancelingTaskSuspendTransactionAndAutoPurge", 2, false); // Cancel on the second update
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.toString());
        task.getExecutionProperties().put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);
        Trigger trigger = new FixedRepeatTrigger(6, 34); // Run up to 6 times, but we should cancel at 2
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (status != null)
            throw new Exception("Task was not autopurged. " + status);

        pollForTableEntry("testSelfCancelingTaskSuspendTransactionAndAutoPurge", 2);
    }

    /**
     * Schedule a task that removes itself when it runs the second time.
     */
    @Test
    public void testSelfRemovingTask(PrintWriter out) throws Exception {
        Callable<Integer> task = new SelfRemovingTask("testSelfRemovingTask", 2, false); // Remove on the second update
        Trigger trigger = new FixedRepeatTrigger(4, 24); // Run up to 4 times, but we should remove at 2
        TaskStatus<Integer> status = scheduler.schedule(task, trigger);

        pollForTableEntry("testSelfRemovingTask", 2);

        status = scheduler.getStatus(status.getTaskId());

        if (status != null)
            throw new Exception("Task was not removed. " + status);
    }

    /**
     * Schedule a task that runs with the persistent executor transaction suspended,
     * and removes itself when it runs the third time.
     */
    @Test
    public void testSelfRemovingTaskSuspendTransaction(PrintWriter out) throws Exception {
        SelfRemovingTask task = new SelfRemovingTask("testSelfRemovingTaskSuspendTransaction", 3, true); // Remove on the third update
        task.getExecutionProperties().put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);
        Trigger trigger = new FixedRepeatTrigger(4, 32); // Run up to 4 times, but we should remove at 3
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (status != null)
            throw new Exception("Task was not removed. " + status);

        pollForTableEntry("testSelfRemovingTaskSuspendTransaction", 3);
    }

    /**
     * Skips the first execution of a task.
     */
    @Test
    public void testSkipFirstExecution(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testSkipFirstExecution");
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        Trigger trigger = new FixedRepeatTrigger(2, 59, 1);
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        status = pollForResult(status.getTaskId(), 1);

        if (!status.isDone())
            throw new Exception("Task should be done. " + status);

        long delay = status.getDelay(TimeUnit.MICROSECONDS);
        if (delay > 0)
            throw new Exception("Delay must not be positive for completed task. " + status);

        Integer result = status.get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected result " + result + " for task. " + status);

        if (status.cancel(true))
            throw new Exception("Should not be able to cancel task that already completed its final execution. " + status);

        if (status.isCancelled())
            throw new Exception("Task should not be canceled. " + status);
    }

    /**
     * Skips the last execution of a task. The task should be autopurged because it was not failed or canceled.
     * In terms of autopurging, it should make no difference whether the first, last, or any other executions were skipped.
     */
    @Test
    public void testSkipLastExecution(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testSkipLastExecution");
        Trigger trigger = new FixedRepeatTrigger(2, 67, 2);
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        // First execution should complete and update database
        pollForTableEntry("testSkipLastExecution", 1);

        // Final execution should be skipped and autopurge
        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (status != null)
            throw new Exception("Task did not complete or did not autopurge upon completion. " + status);
    }

    /**
     * Skips the last execution of a task. Verify that the task entry remains in the persistent store
     * because we disabled autopurge.
     */
    @Test
    public void testSkipLastExecutionNoAutoPurge(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testSkipLastExecutionNoAutoPurge");
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        Trigger trigger = new FixedRepeatTrigger(2, 30, 2);
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        status = pollForSkip(status.getTaskId());

        if (!status.isDone())
            throw new Exception("Task should be done after the final execution is skipped. " + status);

        Date nextExecTime = status.getNextExecutionTime();
        if (nextExecTime != null)
            throw new Exception("No further executions should remain after final execution is skipped. " + status);

        long delay = status.getDelay(TimeUnit.NANOSECONDS);
        if (delay > 0)
            throw new Exception("Delay must not be positive for completed task. " + status);

        try {
            Integer result = status.get();
            throw new Exception("Task was not skipped. Instead: " + result);
        } catch (SkippedException x) {
            if (x.getCause() != null)
                throw x;
        }

        if (status.cancel(true))
            throw new Exception("Should not be able to cancel task that already skipped its final execution. " + status);

        if (status.isCancelled())
            throw new Exception("Task should not be canceled. " + status);

        // Look for successfully completed task
        String pattern = DBIncrementTask.class.getSimpleName() + "-testSkipLastExecutionNoAutoPurge";
        List<TaskStatus<?>> successfulTasks = scheduler.findTaskStatus(pattern, '\\', TaskState.SUCCESSFUL, true, null, null);
        if (successfulTasks.size() != 1 || successfulTasks.iterator().next().getTaskId() != status.getTaskId())
            throw new Exception("Expecting task to show as SUCCESSFUL even though the final execution was skipped. Instead found: " + successfulTasks);
    }

    /**
     * Skips the middle executions of a task.
     */
    @Test
    public void testSkipMiddleExecutions(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testSkipMiddleExecutions");
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        Trigger trigger = new FixedRepeatTrigger(4, 39, 2, 3);
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        status = pollForResult(status.getTaskId(), 2);

        if (!status.isDone())
            throw new Exception("Task should be done after the final execution completes. " + status);

        long delay = status.getDelay(TimeUnit.MILLISECONDS);
        if (delay > 0)
            throw new Exception("Delay must not be positive for completed task. " + status);

        Integer result = status.get();
        if (!Integer.valueOf(2).equals(result))
            throw new Exception("Unexpected result " + result + " for task. " + status);

        if (status.cancel(true))
            throw new Exception("Should not be able to cancel task that already completed its final execution. " + status);

        if (status.isCancelled())
            throw new Exception("Task should not be canceled. " + status);
    }

    /**
     * Skip the only execution of a task. The task should be autopurged because it was not failed or canceled.
     * In terms of autopurging, it should make no difference whether the first, last, or any other executions were skipped.
     */
    @Test
    public void testSkipOnlyExecution(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testSkipOnlyExecution");
        Trigger trigger = new FixedRepeatTrigger(1, 42, 1);
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        // Only execution should be skipped and autopurge
        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (status != null)
            throw new Exception("Task did not complete or did not autopurge upon completion. " + status);
    }

    /**
     * Skip the only execution of a task. Verify that the task entry remains in the persistent store
     * because we disabled autopurge.
     */
    @Test
    public void testSkipOnlyExecutionNoAutoPurge(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testSkipOnlyExecutionNoAutoPurge");
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        Trigger trigger = new FixedRepeatTrigger(1, 31, 1);
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        status = pollForSkip(status.getTaskId());

        if (!status.isDone())
            throw new Exception("Task should be done after the only execution is skipped. " + status);

        long delay = status.getDelay(TimeUnit.NANOSECONDS);
        if (delay > 0)
            throw new Exception("Delay must not be positive for completed task. " + status);

        try {
            Integer result = status.get();
            throw new Exception("Task was not skipped. Instead: " + result);
        } catch (SkippedException x) {
            if (x.getCause() != null)
                throw x;
        }

        if (status.cancel(true))
            throw new Exception("Should not be able to cancel task that already skipped the only execution. " + status);

        if (status.isCancelled())
            throw new Exception("Task should not be canceled. " + status);
    }

    /**
     * Schedule a task that cancels two other tasks. One that autopurges on cancel, and one that does not.
     */
    @Test
    public void testTaskCancelsAnotherTask(PrintWriter out) throws Exception {
        DBIncrementTask taskA = new DBIncrementTask("testTaskCancelsAnotherTask-A");
        taskA.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.toString());
        TaskStatus<?> statusA = scheduler.schedule((Runnable) taskA, 27, TimeUnit.DAYS);

        DBIncrementTask taskB = new DBIncrementTask("testTaskCancelsAnotherTask-B");
        TaskStatus<?> statusB = scheduler.schedule((Runnable) taskB, 28, TimeUnit.DAYS);

        CancelingTask taskC = new CancelingTask();
        taskC.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        taskC.taskIdsToCancel.put(1, new Long[] { statusA.getTaskId(), statusB.getTaskId() });
        TaskStatus<List<Long>> statusC = scheduler.submit(taskC);

        for (long start = System.nanoTime(); !statusC.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            statusC = scheduler.getStatus(statusC.getTaskId());

        if (!statusC.isDone() || statusC.isCancelled())
            throw new Exception("Task C did not run in a timely manner. " + statusC);

        List<Long> canceledIds = statusC.get();
        if (!canceledIds.contains(statusA.getTaskId()) || !canceledIds.contains(statusB.getTaskId()))
            throw new Exception("Task C did not report successfully canceling both Task A (" + statusA.getTaskId() +
                                ") and Task B ( " + statusB.getTaskId() + "). Results are " + canceledIds);

        statusB = scheduler.getStatus(statusB.getTaskId());
        if (statusB == null || !statusB.isCancelled() || !statusB.isDone())
            throw new Exception("Task B should be canceled. Instead " + statusB);

        statusA = scheduler.getStatus(statusA.getTaskId());
        if (statusA != null)
            throw new Exception("Task A should have been canceled and autopurged. Instead " + statusA);
    }

    /**
     * Schedule a task that schedules another task. Ensure that both tasks run successfully.
     */
    @Test
    public void testTaskSchedulesAnotherTask(PrintWriter out) throws Exception {
        SchedulingTask task = new SchedulingTask();
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        TaskStatus<Long> status = scheduler.schedule(task, 26, TimeUnit.MICROSECONDS);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (!status.isDone() || status.isCancelled())
            throw new Exception("Task did not complete successfully in allotted interval. " + status);

        Long idOfTaskScheduledByTask = status.get();

        TaskStatus<Integer> status2 = scheduler.getStatus(idOfTaskScheduledByTask);

        for (long start = System.nanoTime(); !status2.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status2 = scheduler.getStatus(status2.getTaskId());

        if (!status2.isDone() || status2.isCancelled())
            throw new Exception("Second task did not complete successfully in allotted interval. " + status2);

        Integer result = status2.get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected result for task scheduled by task. " + result);
    }

    /**
     * Schedule a task that simulates being an EJB timer task/trigger.
     * It should be possible to schedule from an unmanaged thread without the proper metadata or classloader
     * because the TimerTrigger interface supplies that information.
     */
    @Test
    public void testTimerTask(PrintWriter out) throws Exception {
        @SuppressWarnings("unchecked")
        Constructor<? extends TimerTrigger> TimerTriggerTask = (Constructor<? extends TimerTrigger>) Class.forName("test.feature.sim.ejb.timer.TimerTriggerTask").getConstructor(String.class,
                                                                                                                                                                                 Date[].class);

        Date execTime1 = new Date();
        Date execTime2 = new Date(execTime1.getTime() + 58);
        Date execTime3 = new Date(execTime1.getTime() + TimeUnit.DAYS.toMillis(5));
        final TimerTrigger timerTrigger = TimerTriggerTask.newInstance("testTimerTask-A", new Date[] { execTime1, execTime2, execTime3 });
        final TimersPersistentExecutor timersExecutor = (TimersPersistentExecutor) scheduler;

        // Schedule from a thread that lacks the proper metadata/classloader
        TimerStatus<Integer> status = unmanagedExecutor.submit(new Callable<TimerStatus<Integer>>() {
            @Override
            public TimerStatus<Integer> call() throws Exception {
                ClassLoader previousClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
                try {
                    return timersExecutor.schedule(timerTrigger);
                } finally {
                    Thread.currentThread().setContextClassLoader(previousClassLoader);
                }
            }
        }).get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        Date initialExecTime = status.getNextExecutionTime();
        if (!execTime1.equals(initialExecTime))
            throw new Exception("Unexpected initial next execution time: " + initialExecTime.getTime() + ". Expected " + execTime1.getTime());

        TimerTrigger tt = status.getTimer();
        Date secondExecTime = tt.getNextRunTime(null, null);
        if (!execTime2.equals(secondExecTime))
            throw new Exception("Unexpected second execution time: " + secondExecTime.getTime() + ". Expected " + execTime2.getTime());

        // Wait for first result
        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = timersExecutor.getTimerStatus(status.getTaskId());

        // Wait for second result
        for (long start = System.nanoTime(); status.getResult() == 2 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = timersExecutor.getTimerStatus(status.getTaskId());

        Integer result = status.getResult();
        if (result != 1)
            throw new Exception("Unexpected result " + result + " for " + status);

        tt = status.getTimer();
        Date nextRunTime = status.getNextExecutionTime();
        if (!execTime3.equals(nextRunTime))
            throw new Exception("Unexpected final next execution time from status " + nextRunTime.getTime() + ". Expecting: " + execTime3.getTime());

        Date runTimeAfterFinalRunTime = tt.getNextRunTime(null, null);
        if (runTimeAfterFinalRunTime != null)
            throw new Exception("Should not compute further run time after " + execTime3.getTime() + ". Instead " + runTimeAfterFinalRunTime.getTime());

        // Schedule another task, with all executions in the future
        execTime1 = new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(59));
        execTime2 = new Date(execTime1.getTime() + TimeUnit.DAYS.toMillis(59));
        execTime3 = new Date(execTime1.getTime() + TimeUnit.DAYS.toMillis(590));
        Date execTime4 = new Date(execTime1.getTime() + TimeUnit.DAYS.toMillis(5900));
        TimerTrigger timerTriggerB = TimerTriggerTask.newInstance("testTimerTask-B", new Date[] { execTime1, execTime2, execTime3, execTime4 });
        timersExecutor.schedule(timerTriggerB);

        List<TimerStatus<?>> timerStatusList = timersExecutor.findTimerStatus("schedtest",
                                                                              "testTimerTask-B",
                                                                              null,
                                                                              TaskState.SCHEDULED,
                                                                              true,
                                                                              null,
                                                                              1);
        TimerStatus<?> timerStatus = timerStatusList.get(0);

        Date baseTime = new Date();
        Trigger trigger1 = timerStatus.getTimer();
        Date time1a = trigger1.getNextRunTime(null, baseTime);

        // Use up all executions
        for (Date t = time1a; t != null; t = trigger1.getNextRunTime(null, baseTime));

        Trigger trigger2 = timerStatus.getTimer();
        Date time2a = trigger2.getNextRunTime(null, baseTime);

        if (time2a == null || !time2a.equals(time1a))
            throw new Exception("Triggers " + trigger1 + " and " + trigger2 +
                                " starting from same state should compute same next execution times. Instead " +
                                time1a.getTime() + " and " + time2a.getTime());
    }

    /**
     * Schedule a callable with a stateless trigger.
     */
    @Test
    public void testTriggerCallable(PrintWriter out) throws Exception {
        DBIncrementTask task = new DBIncrementTask("testTriggerCallable");
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        Trigger trigger = new ResultBasedTrigger();
        TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) task, trigger);

        try {
            boolean canceled = status.isCancelled();
            throw new Exception("Task should not have canceled status " + canceled + " until it ends.");
        } catch (IllegalStateException x) {
        }

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        if (status.hasResult())
            throw new Exception("Task status initial snapshot should not have a result. " + status);

        if (!status.equals(status))
            throw new Exception("Task status does not equal itself");

        String toString = status.toString();
        if (!toString.contains("DBIncrementTask-testTriggerCallable"))
            throw new Exception("toString output does not contain the IDENTITY_NAME of task. Instead: " + toString);

        pollForTableEntry("testTriggerCallable", 2);

        TaskStatus<Integer> updatedStatus = scheduler.getStatus(status.getTaskId());

        if (updatedStatus.isCancelled())
            throw new Exception("Task should not be canceled");

        if (!updatedStatus.isDone())
            throw new Exception("Task should be done");

        Integer result = updatedStatus.get();
        if (!Integer.valueOf(2).equals(result))
            throw new Exception("Unexpected task result: " + result);

        long delay = updatedStatus.getDelay(TimeUnit.MICROSECONDS);
        if (delay > 0)
            throw new Exception("Delay should not be positive for completed task: " + delay);

        if (status.equals(updatedStatus))
            throw new Exception("Status before/after execution should not be equal: " + status + " and " + updatedStatus);
    }

    /**
     * Schedule a runnable with a stateful trigger.
     */
    @Test
    public void testTriggerRunnable(PrintWriter out) throws Exception {
        Runnable task = new DBIncrementTask("testTriggerRunnable");
        Trigger trigger = new FixedRepeatTrigger(3, 107);
        TaskStatus<?> status = scheduler.schedule(task, trigger);

        try {
            boolean canceled = status.isCancelled();
            throw new Exception("Task should not have canceled status " + canceled + " until it ends.");
        } catch (IllegalStateException x) {
        }

        try {
            boolean done = status.isDone();
            throw new Exception("Task should not have done status " + done + " until it runs at least once.");
        } catch (IllegalStateException x) {
        }

        if (status.hasResult())
            throw new Exception("Task status initial snapshot should not have a result. " + status);

        if (!status.equals(status))
            throw new Exception("Task status does not equal itself");

        String toString = status.toString();
        if (!toString.contains("DBIncrementTask-testTriggerRunnable"))
            throw new Exception("toString output does not contain the IDENTITY_NAME of task. Instead: " + toString);

        pollForTableEntry("testTriggerRunnable", 3);

        status = scheduler.getStatus(status.getTaskId());
        if (status != null)
            throw new Exception("AutoPurge did not remove completed task or Trigger state was not persisted. Status: " + status);
    }

    /**
     * Schedule a non-serializable task that is also a trigger.
     * Even though both cannot persist state to the database, because the same instance is used,
     * the state must be preserved across the trigger.skipRun/task.call/trigger.getNextExecution sequence.
     */
    @Test
    public void testTriggerTask(PrintWriter out) throws Exception {
        TriggerTask triggerAndTask = new TriggerTask();
        TriggerTask.results.clear();
        scheduler.schedule(triggerAndTask, triggerAndTask);

        Integer result = TriggerTask.results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected or missing first result: " + result);

        result = TriggerTask.results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        if (!Integer.valueOf(2).equals(result))
            throw new Exception("Unexpected or missing second result: " + result);

        result = TriggerTask.results.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS);
        if (!Integer.valueOf(3).equals(result))
            throw new Exception("Unexpected or missing third result: " + result);

        if (!TriggerTask.results.isEmpty())
            throw new Exception("Unexpected extra results: " + TriggerTask.results);
    }

    /**
     * Submit two tasks from the same transaction.
     * Neither task should run until the transaction commits.
     */
    @Test
    public void testTwoTasksInOneTransaction(PrintWriter out) throws Exception {
        DBIncrementTask taskA = new DBIncrementTask("testTwoTasksInOneTransaction-A");
        DBIncrementTask taskB = new DBIncrementTask("testTwoTasksInOneTransaction-B");
        taskA.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        taskB.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        TaskStatus<Integer> statusA;
        TaskStatus<Integer> statusB;
        tran.begin();
        try {
            statusA = scheduler.submit((Callable<Integer>) taskA);
            statusB = scheduler.schedule((Callable<Integer>) taskB, 13, TimeUnit.MICROSECONDS);
            statusA = scheduler.getStatus(statusA.getTaskId());
            if (statusA.hasResult())
                throw new Exception("Task shouldn't start until transaction in which it was scheduled commits. " + statusA);
        } finally {
            tran.commit();
        }

        statusA = pollForResult(statusA.getTaskId(), 1);
        if (statusA == null || !statusA.isDone())
            throw new Exception("TaskA not completed in allotted interval. Status: " + statusA);

        statusB = pollForResult(statusB.getTaskId(), 1);
        if (statusB == null || !statusB.isDone())
            throw new Exception("TaskB not completed in allotted interval. Status: " + statusB);
    }

    /**
     * Submit a task from an unmanaged thread.
     */
    @Test
    public void testUnmanagedThreadSubmitsTask(PrintWriter out) throws Exception {
        final ClassLoader servletClassLoader = Thread.currentThread().getContextClassLoader();

        Integer result = unmanagedExecutor.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                ClassLoader unmanagedThreadClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(null);
                try {
                    TaskStatus<Integer> status = scheduler.schedule((Callable<Integer>) new DBIncrementTask("testUnmanagedThreadSubmitsTask-A"), 45, TimeUnit.SECONDS);
                    throw new Exception("Should not be able to schedule tasks without ComponentMetaData or a thread context classloader to identify the owner. " + status);
                } catch (RejectedExecutionException x) {
                    if (!x.getMessage().contains("CWWKC1540E"))
                        throw x;
                }

                Thread.currentThread().setContextClassLoader(servletClassLoader);
                try {
                    ManagedTriggerTask task = new ManagedTriggerTask();
                    task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
                    ManagedTriggerTask.results.clear();
                    TaskStatus<Integer> status = scheduler.schedule(task, 18, TimeUnit.NANOSECONDS);

                    try {
                        boolean canceled = status.isCancelled();
                        throw new Exception("Task should not have canceled status " + canceled + " until it ends.");
                    } catch (IllegalStateException x) {
                    }

                    try {
                        boolean done = status.isDone();
                        throw new Exception("Task should not have done status " + done + " until it runs at least once.");
                    } catch (IllegalStateException x) {
                    }

                    if (status.hasResult())
                        throw new Exception("Task status initial snapshot should not have a result. " + status);

                    status = pollForResult(status.getTaskId(), 3);

                    boolean removed = scheduler.remove(status.getTaskId());
                    if (!removed)
                        throw new Exception("Unable to remove task. " + status);

                    if (status.isCancelled() || !status.isDone())
                        throw new Exception("Task should have completed. " + status);

                    return status.get();
                } finally {
                    Thread.currentThread().setContextClassLoader(unmanagedThreadClassLoader);
                }
            }
        }).get();

        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected task result " + result);
    }

    /**
     * This method should be invoked after running the other tests.
     * Verify that no tasks are left running (other than ones intentionally scheduled to the distant future).
     */
    public void verifyNoTasksRunning(PrintWriter out) throws Exception {
        List<TaskStatus<?>> results = new LinkedList<TaskStatus<?>>();
        for (TaskStatus<?> status : scheduler.findTaskStatus("%", null, TaskState.SCHEDULED, true, null, null))
            if (status.getNextExecutionTime().getTime() - System.currentTimeMillis() < 5 * 60 * 1000)
                results.add(status);

        if (!results.isEmpty()) {
            // Allow some extra time and then validate each again
            Thread.sleep(TimeUnit.NANOSECONDS.toMillis(TIMEOUT_NS));

            StringBuilder runningTaskInfo = new StringBuilder();
            for (TaskStatus<?> status : results) {
                status = scheduler.getStatus(status.getTaskId());
                Date nextExecTime = status.getNextExecutionTime();
                if (nextExecTime != null && nextExecTime.getTime() - System.currentTimeMillis() < 5 * 60 * 1000)
                    runningTaskInfo.append("\r\n").append(status);
            }

            if (runningTaskInfo.length() > 0)
                throw new Exception("Found tasks that are still running. Ignore this error if any other tests failed." + runningTaskInfo);
        }
    }
}
