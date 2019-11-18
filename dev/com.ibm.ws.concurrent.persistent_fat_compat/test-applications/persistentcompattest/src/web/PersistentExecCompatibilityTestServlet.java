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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.SkippedException;
import javax.enterprise.concurrent.Trigger;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;
import ejb.MyTimerEJBInWAR;

@WebServlet(urlPatterns = "/PersistentExecCompatibilityTestServlet")
public class PersistentExecCompatibilityTestServlet extends FATServlet {
    private static final long serialVersionUID = 8447513765214641067L;

    /**
     * Interval in milliseconds between polling for task results.
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(30);

    /**
     * Update this to control the version that is written to new task identity names
     */
    private static final String VNEXT = "8.5.5.6";

    @Resource(lookup = "jdbc/myDataSource")
    private DataSource dataSource;

    @Resource(name = "java:module/env/myExecutorRef", lookup = "concurrent/myExecutor")
    private PersistentExecutor executor;

    private long executorPartitionId = -1;

    @Resource(lookup = "concurrent/mySchedulerWithContext")
    private PersistentExecutor schedulerWithContext;

    @Resource(lookup = "concurrent/mySchedulerWithoutContext")
    private PersistentExecutor schedulerWithoutContext;

    @Resource
    private UserTransaction tran;

    /**
     * Utility method that reads a task from a file within the application (/WEB-INF/serialized/*.ser)
     * and creates a task in the database for it.
     */
    private long insertTaskEntry(HttpServletRequest request, String name) throws Exception {
        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/" + name + ".ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        LinkedHashMap<String, Object> map;
        try {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> m = (LinkedHashMap<String, Object>) objectInput.readObject();
            map = m;
        } finally {
            objectInput.close();
        }

        if (executorPartitionId == -1) {
            Method getPartitionId = executor.getClass().getDeclaredMethod("getPartitionId");
            getPartitionId.setAccessible(true);
            executorPartitionId = (Long) getPartitionId.invoke(executor);
        }
        map.put("PARTN", executorPartitionId);

        String insert = "INSERT INTO EXECTASK VALUES(DEFAULT";
        for (int i = map.size(); i >= 1; i--)
            insert += ",?";
        insert += ')';

        System.out.println("Insert command for " + name + ": " + insert + " Params: " + map);

        tran.begin();
        try {
            Connection con = dataSource.getConnection();
            try {
                PreparedStatement pstmt = con.prepareStatement(insert);
                int i = 0;
                for (Object value : map.values())
                    pstmt.setObject(++i, value);
                pstmt.executeUpdate();
                pstmt.close();
                pstmt = con.prepareStatement("VALUES IDENTITY_VAL_LOCAL()");
                ResultSet result = pstmt.executeQuery();
                result.next();
                return result.getLong(1);
            } finally {
                con.close();
            }
        } finally {
            tran.commit();
        }
    }

    /**
     * Utility method that write a persisted task to a file.
     */
    private void saveTaskEntry(PersistentExecutor executor, long taskId, String name) throws Exception {
        String tablePrefix = executor == this.executor ? "EXEC" : "SCHD";
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        Connection con = dataSource.getConnection();
        try {
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM " + tablePrefix + "TASK WHERE ID=?");
            pstmt.setLong(1, taskId);
            ResultSet result = pstmt.executeQuery();
            if (!result.next())
                throw new Exception("Entry with task id " + taskId + " not found");
            ResultSetMetaData metadata = result.getMetaData();
            int numColumns = metadata.getColumnCount();
            for (int i = 1; i <= numColumns; i++) {
                String key = metadata.getColumnName(i);
                Object value = result.getObject(i);
                if (value instanceof Blob)
                    value = ((Blob) value).getBytes(1, (int) ((Blob) value).length());
                if (!"ID".equalsIgnoreCase(key))
                    map.put(key, value);
            }
        } finally {
            con.close();
        }

        ObjectOutputStream outfile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(name + ".ser")));
        try {
            outfile.writeObject(map);
        } finally {
            outfile.close();
        }
    }

    /**
     * Execute a Callable task that is also a Trigger
     * which was scheduled and persisted with the 8.5.5.6 release.
     */
    @Test
    public void testExecuteCallableTrigger_8_5_5_6(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "CallableTrigger-8.5.5.6";
        long taskId = insertTaskEntry(request, taskName);

        TaskStatus<Integer> status = executor.getStatus(taskId);
        for (long start = System.nanoTime(); status.getNextExecutionTime() != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = executor.getStatus(taskId);

        if (!status.isDone())
            throw new Exception("Task did not complete all executions within allotted interval. " + status);

        Integer result = status.get();
        if (!Integer.valueOf(3).equals(result))
            throw new Exception("Unexpected result " + result + ". Status: " + status);

        if (!taskName.equals(status.getTaskName()))
            throw new Exception("Unexpected task name " + status);
    }

    /**
     * Execute a Callable task that runs according to a Trigger
     * which was scheduled and persisted with the 8.5.5.6 release.
     */
    @Test
    public void testExecuteCallableWithTrigger_8_5_5_6(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "CallableWithTrigger-8.5.5.6";
        long taskId = insertTaskEntry(request, taskName);

        TaskStatus<Date> status = executor.getStatus(taskId);
        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = executor.getStatus(taskId);

        if (!status.hasResult())
            throw new Exception("Task did not complete any executions within allotted interval. " + status);

        Date result = status.getResult();
        if (result == null)
            throw new Exception("Unexpected result " + result + ". Status: " + status);

        if (!" ".equals(status.getTaskName()))
            throw new Exception("Unexpected task name " + status);
    }

    /**
     * Execute an EJB persistent timer
     * which was scheduled and persisted with the 8.5.5.6 release.
     */
    @Test
    public void testExecuteEJBTimer_8_5_5_6(HttpServletRequest request, HttpServletResponse response) throws Exception {
        MyTimerEJBInWAR ejb = (MyTimerEJBInWAR) new InitialContext().lookup("java:global/persistentcompattest/MyTimerEJBInWAR!ejb.MyTimerEJBInWAR");

        String name = "EJBTimer-8.5.5.6";
        long taskId = insertTaskEntry(request, name);

        Integer count = ejb.getRunCount(name);
        for (long start = System.nanoTime(); count == null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            count = ejb.getRunCount(name);

        if (!Integer.valueOf(1).equals(count))
            throw new Exception("Expecting EJB timer to run exactly once. Instead: " + count + ". " + executor.getStatus(taskId));
    }

    /**
     * Execute a Runnable task with a fixed delay between executions
     * which was scheduled and persisted with the 8.5.5.6 release.
     */
    @Test
    public void testExecuteFixedDelayTask_8_5_5_6(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "FixedDelayTask-8.5.5.6";
        long taskId = insertTaskEntry(request, taskName);

        Integer initialCount = MapCounter.counters.get(taskName);
        for (long start = System.nanoTime(); initialCount == null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            initialCount = MapCounter.counters.get(taskName);

        if (initialCount == null)
            throw new Exception("Task did not run within allotted interval " + executor.getStatus(taskId));

        Integer count = initialCount;
        for (long start = System.nanoTime(); initialCount.equals(count) && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            count = MapCounter.counters.get(taskName);

        if (initialCount.equals(count))
            throw new Exception("Task did not complete any subsequent executions within allotted interval. Initial: " + initialCount + " " + executor.getStatus(taskId));

        if (!executor.remove(taskId))
            throw new Exception("Unable to remove task " + taskId);
    }

    /**
     * Execute a Runnable task at a fixed rate
     * which was scheduled and persisted with the 8.5.5.6 release.
     */
    @Test
    public void testExecuteFixedRateTask_8_5_5_6(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "FixedRateTask-8.5.5.6";
        long taskId = insertTaskEntry(request, taskName);

        Integer initialCount = MapCounter.counters.get(taskName);
        for (long start = System.nanoTime(); initialCount == null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            initialCount = MapCounter.counters.get(taskName);

        if (initialCount == null)
            throw new Exception("Task did not run within allotted interval " + executor.getStatus(taskId));

        Integer count = initialCount;
        for (long start = System.nanoTime(); initialCount.equals(count) && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            count = MapCounter.counters.get(taskName);

        if (initialCount.equals(count))
            throw new Exception("Task did not complete any subsequent executions within allotted interval. Initial: " + initialCount + " " + executor.getStatus(taskId));

        if (!executor.remove(taskId))
            throw new Exception("Unable to remove task " + taskId);
    }

    /**
     * Execute a Callable task that runs once
     * which was scheduled and persisted with the 8.5.5.6 release.
     */
    @Test
    public void testExecuteOneShotCallable_8_5_5_6(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "OneShotCallable-8.5.5.6";
        long taskId = insertTaskEntry(request, taskName);

        TaskStatus<Integer> status = executor.getStatus(taskId);
        for (long start = System.nanoTime(); status.getNextExecutionTime() != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = executor.getStatus(taskId);

        if (!status.isDone())
            throw new Exception("Task did not complete its only execution within allotted interval. " + status);

        Integer result = status.get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected result " + result + ". Status: " + status);

        if (!taskName.equals(status.getTaskName()))
            throw new Exception("Unexpected task name " + status);
    }

    /**
     * Query for status of a task that fails when it runs
     * which previously executed on the 8.5.5.6 release.
     */
    @Test
    public void testGetFailingRunnable_8_5_5_6(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "FailingRunnable-8.5.5.6";
        long taskId = insertTaskEntry(request, taskName);

        TaskStatus<String> status = executor.getStatus(taskId);
        if (!status.isDone())
            throw new Exception("Unexpected task status. " + status);

        try {
            String result = status.get();
            throw new Exception("Unexpected result: " + result);
        } catch (Exception x) {
            if (!x.getMessage().startsWith("CWWKC1555E") || !(x.getCause() instanceof NullPointerException))
                throw new Exception("Unexpected message or cause. See cause.", x);
        }
    }

    /**
     * Query for status of a task that was skipped
     * which previously attempted on the 8.5.5.6 release.
     */
    @Test
    public void testGetSkippedCallable_8_5_5_6(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "SkippedCallable-8.5.5.6";
        long taskId = insertTaskEntry(request, taskName);

        TaskStatus<Integer> status = executor.getStatus(taskId);
        if (!status.hasResult())
            throw new Exception("Unexpected task status. " + status);

        try {
            Integer result = status.getResult();
            throw new Exception("Unexpected result: " + result);
        } catch (Exception x) {
            if (x.getCause() != null)
                throw new Exception("Unexpected cause.", x);
        }
    }

    /**
     * Query for status of a task that was skipped due to failure of skipRun
     * which was previously attempted on the 8.5.5.6 release.
     */
    @Test
    public void testGetSkipRunFailure_8_5_5_6(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "SkipRunFailure-8.5.5.6";
        long taskId = insertTaskEntry(request, taskName);

        TaskStatus<Integer> status = executor.getStatus(taskId);
        if (!status.hasResult())
            throw new Exception("Unexpected task status. " + status);

        try {
            Integer result = status.getResult();
            throw new Exception("Unexpected result: " + result);
        } catch (Exception x) {
            if (!(x.getCause() instanceof IllegalStateException))
                throw new Exception("Unexpected cause.", x);
        }
    }

    /**
     * Query for status of task that returns a non-serializable result
     * which was previously executed on the 8.5.5.6 release.
     */
    @Test
    public void testGetTaskWithNonSerializableResult_8_5_5_6(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "TaskWithNonSerializableResult-8.5.5.6";
        long taskId = insertTaskEntry(request, taskName);

        TaskStatus<ThreadGroup> status = executor.getStatus(taskId);
        if (!status.isDone())
            throw new Exception("Unexpected task status. " + status);

        try {
            ThreadGroup result = status.get();
            throw new Exception("Unexpected result: " + result);
        } catch (Exception x) {
            if (!(x.getCause() instanceof NotSerializableException))
                throw new Exception("Unexpected failure. See cause.", x);
        }
    }

    /**
     * Schedule and run a task that fails when executed (because we didn't supply an identity name).
     * Persist the task entry to a file.
     */
    @AllowedFFDC("java.lang.NullPointerException") // test intentionally causes NullPointerException by omitting the identity name where a task expects it
    @Test
    public void testPersistFailingRunnable(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "FailingRunnable-" + VNEXT;
        MapCounterRunnable task = new MapCounterRunnable();
        // Avoid specifying the identity name, which will make this particular task fail when it runs
        TaskStatus<String> status = executor.submit(task, taskName + " fixed result");

        long taskId = status.getTaskId();

        // wait for task to fail
        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = executor.getStatus(taskId);

        if (!status.isDone())
            throw new Exception("Task did not execute within allotted interval. " + status);

        try {
            String result = status.get();
            throw new Exception("Unexpected result: " + result);
        } catch (ExecutionException x) {
            if (!x.getMessage().startsWith("CWWKC1555E") || !(x.getCause() instanceof NullPointerException))
                throw new Exception("Unexpected message or cause. See cause.", x);
        }

        saveTaskEntry(executor, taskId, taskName);
    }

    /**
     * Schedule and skip execution of a task.
     * Persist the task entry to a file.
     */
    @Test
    public void testPersistSkippedCallable(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "SkippedCallable-" + VNEXT;
        CounterCallable task = new CounterCallable();
        task.getExecutionProperties().put(ManagedTask.IDENTITY_NAME, taskName);
        Trigger trigger = new SkippingTwoTimeTrigger();
        TaskStatus<Integer> status = executor.schedule(task, trigger);

        long taskId = status.getTaskId();

        // wait for task to be skipped
        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = executor.getStatus(taskId);

        if (!status.hasResult())
            throw new Exception("Task was not attempted within allotted interval. " + status);

        try {
            Integer result = status.getResult();
            throw new Exception("Unexpected result: " + result);
        } catch (SkippedException x) {
            if (x.getCause() != null)
                throw new Exception("Unexpected cause.", x);
        }

        saveTaskEntry(executor, taskId, taskName);
    }

    /**
     * Schedule and skip execution of a task due to failure of the skipRun method.
     * Persist the task entry to a file.
     */
    @AllowedFFDC("java.lang.IllegalStateException") // java.lang.IllegalStateException: Intentionally failing skipRun
    @Test
    public void testPersistSkipRunFailure(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "SkipRunFailure-" + VNEXT;
        CounterCallable task = new CounterCallable();
        task.getExecutionProperties().put(ManagedTask.IDENTITY_NAME, taskName);
        Trigger trigger = new SkipFailingTrigger();
        TaskStatus<Integer> status = executor.schedule(task, trigger);

        long taskId = status.getTaskId();

        // wait for task to be skipped
        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = executor.getStatus(taskId);

        if (!status.hasResult())
            throw new Exception("Task was not attempted within allotted interval. " + status);

        try {
            Integer result = status.getResult();
            throw new Exception("Unexpected result: " + result);
        } catch (SkippedException x) {
            if (!(x.getCause() instanceof IllegalStateException))
                throw new Exception("Unexpected cause.", x);
        }

        saveTaskEntry(executor, taskId, taskName);
    }

    /**
     * Schedule and run a task that produces a non-serializable result. Persist the result to a file.
     */
    @Test
    public void testPersistTaskWithNonSerializableResult(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "TaskWithNonSerializableResult-" + VNEXT;
        NonSerializableResultTask task = new NonSerializableResultTask();
        TaskStatus<ThreadGroup> status = executor.submit(task);
        long taskId = status.getTaskId();

        // wait for task to fail
        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = executor.getStatus(taskId);

        if (!status.isDone())
            throw new Exception("Task did not execute within allotted interval. " + status);

        try {
            ThreadGroup result = status.get();
            throw new Exception("Unexpected result: " + result);
        } catch (Exception x) {
            if (!(x.getCause() instanceof NotSerializableException))
                throw new Exception("Unexpected failure. See cause.", x);
        }

        saveTaskEntry(executor, taskId, taskName);
    }

    /**
     * Schedule a Callable task that is also a Trigger.
     */
    @Test
    public void testScheduleCallableTrigger(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "CallableTrigger-" + VNEXT;
        CounterCallableTriggerTask task = new CounterCallableTriggerTask();
        task.getExecutionProperties().put(ManagedTask.IDENTITY_NAME, taskName);
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        TaskStatus<?> status = schedulerWithoutContext.schedule(task, task);
        saveTaskEntry(schedulerWithoutContext, status.getTaskId(), taskName);
    }

    /**
     * Schedule a Callable task to run according to a Trigger.
     */
    @Test
    public void testScheduleCallableWithTrigger(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "CallableWithTrigger-" + VNEXT;
        Callable<Date> task = new ExecTimeCallable();
        Trigger trigger = new TwoTimeTrigger();
        TaskStatus<?> status = schedulerWithContext.schedule(task, trigger);
        saveTaskEntry(schedulerWithContext, status.getTaskId(), taskName);
    }

    /**
     * Schedule a persistent EJB timer.
     */
    @Test
    public void testScheduleEJBTimer(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String name = "EJBTimer-" + VNEXT;
        MyTimerEJBInWAR ejb = (MyTimerEJBInWAR) new InitialContext().lookup("java:global/persistentcompattest/MyTimerEJBInWAR!ejb.MyTimerEJBInWAR");
        Timer timer = ejb.scheduleTimer(name, 4);

        TimerHandle handle = timer.getHandle();
        System.out.println("Timer handle is " + timer.getHandle());
        String s = handle.toString();
        s = s.substring(s.indexOf('(') + 1, s.indexOf(')'));
        long taskId = Long.parseLong(s);

        saveTaskEntry(schedulerWithoutContext, taskId, name);
    }

    /**
     * Schedule a Runnable task to run with a fixed delay between executions.
     */
    @Test
    public void testScheduleFixedDelayTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "FixedDelayTask-" + VNEXT;
        MapCounterRunnable task = new MapCounterRunnable();
        task.getExecutionProperties().put(ManagedTask.IDENTITY_NAME, taskName);
        TaskStatus<?> status = schedulerWithContext.scheduleWithFixedDelay(task, 0, 101, TimeUnit.MILLISECONDS);
        saveTaskEntry(schedulerWithContext, status.getTaskId(), taskName);
    }

    /**
     * Schedule a Runnable task to run at a fixed rate.
     */
    @Test
    public void testScheduleFixedRateTask(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "FixedRateTask-" + VNEXT;
        MapCounterRunnable task = new MapCounterRunnable();
        task.getExecutionProperties().put(ManagedTask.IDENTITY_NAME, taskName);
        TaskStatus<?> status = schedulerWithContext.scheduleAtFixedRate(task, 0, 102, TimeUnit.MILLISECONDS);
        saveTaskEntry(schedulerWithContext, status.getTaskId(), taskName);
    }

    /**
     * Schedule a Callable task to run once.
     */
    @Test
    public void testScheduleOneShotCallable(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String taskName = "OneShotCallable-" + VNEXT;
        CounterCallable task = new CounterCallable();
        task.getExecutionProperties().put(ManagedTask.IDENTITY_NAME, taskName);
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        TaskStatus<?> status = schedulerWithoutContext.schedule(task, 3, TimeUnit.SECONDS);
        saveTaskEntry(schedulerWithoutContext, status.getTaskId(), taskName);
    }

    /**
     * Test that the sequence is created with the proper prefix.
     */
    @Test
    public void testSequenceCreated(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Use the persistent executor to ensure tables/sequence are lazily created in case this test runs first
        schedulerWithContext.getStatus(1);

        Connection con = dataSource.getConnection();
        try {
            ResultSet result = con.createStatement().executeQuery("VALUES (NEXT VALUE FOR SCHDSEQ)");
            if (!result.next())
                throw new Exception("Sequence not found in database");
            result.getLong(1);
        } finally {
            con.close();
        }
    }
}
