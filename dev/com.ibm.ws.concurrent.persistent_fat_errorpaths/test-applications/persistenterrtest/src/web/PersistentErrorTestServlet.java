/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
import java.io.NotSerializableException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.SkippedException;
import javax.enterprise.concurrent.Trigger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.ResultNotSerializableException;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

@WebServlet("/*")
public class PersistentErrorTestServlet extends HttpServlet {
    private static final long serialVersionUID = 8447513765214641067L;

    /**
     * Interval for polling task status (in milliseconds).
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(2);

    /**
     * A higher limit on the maximum number of nanoseconds to wait for a task to finish,
     * which is used when the test case intentionally causes an exception path where FFDC
     * information is logged to disk, which can randomly take several minutes on poorly
     * performing test infrastructure.
     */
    private static final long TIMEOUT_NS_FFDC_PATH = TimeUnit.MINUTES.toNanos(10);

    @Resource(name = "java:comp/env/concurrent/mySchedulerRef", lookup = "concurrent/myScheduler")
    private PersistentExecutor scheduler;

    @Resource(lookup = "jdbc/schedDBShutdown")
    private DataSource shutdownDB;

    @Resource
    private UserTransaction tran;

    /**
     * Invokes test name found in "test" parameter passed to servlet.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();

        try {
            out.println(getClass().getSimpleName() + " is starting " + test + "<br>");
            System.out.println("-----> " + test + " starting");
            getClass().getMethod(test, HttpServletRequest.class, PrintWriter.class).invoke(this, request, out);
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

    @Override
    public void init(ServletConfig config) throws ServletException {
        // Make UserTransaction available to task that otherwise runs without JEE metadata context
        WaitForRollbackTask.tran = tran;
    }

    /**
     * Schedule a task that fails all execution attempts, exceeding the task failure limit, then and auto purges.
     */
    public void testFailingTaskAndAutoPurge(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedFailingTask.clear();
        SharedFailingTask.execProps.put(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.toString());
        SharedFailingTask.failOn.add(1l);
        SharedFailingTask.failOn.add(2l);
        SharedFailingTask.failOn.add(3l);
        try {
            Callable<Long> task = new SharedFailingTask();
            TaskStatus<Long> status = scheduler.schedule(task, 19, TimeUnit.MILLISECONDS);

            for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                status = scheduler.getStatus(status.getTaskId());

            if (status != null)
                throw new Exception("Task did not complete in a timely manner or was not autopurged upon completion. " + status);

            long counter = SharedFailingTask.counter.get();
            if (counter != 2)
                throw new Exception("Task should be attempted exactly 2 times (with both attempts failing). Instead " + counter);
        } finally {
            SharedFailingTask.clear();
        }
    }

    /**
     * Schedule a task that fails all execution attempts, exceeding the task failure limit,
     * and remains in the persistent store upon completion.
     */
    public void testFailingTaskNoAutoPurge(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedFailingTask.clear();
        SharedFailingTask.execProps.put(ManagedTask.IDENTITY_NAME, "testFailingTaskNoAutoPurge");
        SharedFailingTask.failOn.add(1l);
        SharedFailingTask.failOn.add(2l);
        SharedFailingTask.failOn.add(3l);
        try {
            Callable<Long> task = new SharedFailingTask();
            TaskStatus<Long> status = scheduler.schedule(task, 20, TimeUnit.MILLISECONDS);

            for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                status = scheduler.getStatus(status.getTaskId());

            if (!status.isDone() || status.isCancelled())
                throw new Exception("Task did not complete. " + status);

            try {
                Long result = status.get();
                throw new Exception("Task ought to exceed the failure limit and fail, not complete with result = " + result);
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof IllegalStateException)
                    || x.getCause().getMessage() == null
                    || !x.getCause().getMessage().contains("Intentionally failing execution #2"))
                    throw x;
            }
        } finally {
            SharedFailingTask.clear();
        }

        // Look for successfully completed tasks from this test. Should be none.
        String pattern = "testFailingTaskNoAutoPurge";
        List<TaskStatus<?>> successfulTasks = scheduler.findTaskStatus(pattern, '!', TaskState.SUCCESSFUL, true, 0l, null);
        if (successfulTasks.size() > 0)
            throw new Exception("There shouldn't be any successful tasks named testFailingTaskNoAutoPurge. Results: " + successfulTasks);

        // Look for unsuccessful tasks from this test. Should be one.
        List<TaskStatus<?>> unsuccessfulTasks = scheduler.findTaskStatus(pattern, '!', TaskState.SUCCESSFUL, false, 0l, null);
        if (unsuccessfulTasks.size() != 1)
            throw new Exception("There should be exactly one unsuccessful task named testFailingTaskNoAutoPurge. Instead: " + successfulTasks);

    }

    /**
     * Schedule a task that fails an execution attempt and skips the retry.
     * Autopurges when the retry runs the next time after the skip.
     */
    public void testFailOnceAndSkipFirstRetryAndAutoPurge(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedFailingTask.clear();
        SharedSkippingTrigger.clear();
        SharedFailingTask task = new SharedFailingTask();
        SharedSkippingTrigger trigger = new SharedSkippingTrigger();
        try {
            SharedFailingTask.execProps.put(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.toString());
            SharedFailingTask.failOn.add(1l);
            SharedFailingTask.failOn.add(2l);
            SharedSkippingTrigger.skipExecutionAttempts.add(2);

            TaskStatus<Long> status = scheduler.schedule((Callable<Long>) task, trigger);

            // wait for the skipped status
            for (long start = System.nanoTime(); status != null && status.toString().indexOf("SKIPPED") < 0 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                status = scheduler.getStatus(status.getTaskId());

            if (status != null) {
                if (status.toString().indexOf("SKIPPED") < 0) {
                    status.cancel(true);
                    throw new Exception("Unexpected status: " + status);
                }

                try {
                    Long result = status.getResult();
                    throw new Exception("Should not be able to get result " + result + " from task status " + status);
                } catch (SkippedException x) {
                    if (x.getCause() != null)
                        throw x;

                    // wait for the done (autopurged) status
                    for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                        status = scheduler.getStatus(status.getTaskId());

                    if (status != null)
                        throw new Exception("Unexpected status after skipped: " + status);
                }
            }

            Long result = SharedFailingTask.counter.get();
            if (!Long.valueOf(2).equals(result))
                throw new Exception("Task should have been attempted exactly twice, not " + result);
        } finally {
            SharedFailingTask.clear();
            SharedSkippingTrigger.clear();
        }
    }

    /**
     * Schedule a task that fails an execution attempt and skips the retry. Do not autopurge.
     */
    public void testFailOnceAndSkipFirstRetryNoAutoPurge(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedFailingTask.clear();
        SharedSkippingTrigger.clear();
        SharedFailingTask task = new SharedFailingTask();
        SharedSkippingTrigger trigger = new SharedSkippingTrigger();
        try {
            SharedFailingTask.failOn.add(1l);
            SharedFailingTask.failOn.add(2l);
            SharedSkippingTrigger.skipExecutionAttempts.add(2);

            TaskStatus<Long> status = scheduler.schedule((Callable<Long>) task, trigger);

            // wait for the skipped status
            for (long start = System.nanoTime(); status.getNextExecutionTime() != null && status.toString().indexOf("SKIPPED") < 0
                                                 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                status = scheduler.getStatus(status.getTaskId());

            if (status.getNextExecutionTime() == null && status.isCancelled())
                throw new Exception("Unexpected canceled status: " + status);

            if (status.toString().indexOf("SKIPPED") < 0) {
                status.cancel(true);
                throw new Exception("Unexpected status: " + status);
            }

            try {
                Long result = status.getResult();
                throw new Exception("Should not be able to get result " + result + " from task status " + status);
            } catch (SkippedException x) {
                if (x.getCause() != null)
                    throw x;

                // wait for the done (non-skipped) status
                for (long start = System.nanoTime(); status.toString().indexOf("SKIPPED") >= 0 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                    status = scheduler.getStatus(status.getTaskId());

                try {
                    Long result = status.get();
                    throw new Exception("Should not be able to get result " + result + " (second time) from task status " + status);
                } catch (ExecutionException xx) {
                    if (status.getNextExecutionTime() != null
                        || !(xx.getCause() instanceof IllegalStateException)
                        || !"Intentionally failing execution #2".equals(xx.getCause().getMessage()))
                        throw xx;
                }
            } catch (ExecutionException x) {
                if (status.getNextExecutionTime() != null
                    || !(x.getCause() instanceof IllegalStateException)
                    || !"Intentionally failing execution #2".equals(x.getCause().getMessage()))
                    throw x;
            }
        } finally {
            SharedFailingTask.clear();
            SharedSkippingTrigger.clear();
        }
    }

    /**
     * Attempt to invoke TaskStatus.get(timeout, unit). Expect UnsupportedOperationException.
     */
    public void testGetWithTimeout(HttpServletRequest request, PrintWriter out) throws Exception {
        ScheduledFuture<Long> status = scheduler.schedule((Callable<Long>) new SharedCounterTask(), 14, TimeUnit.DAYS);
        try {
            Long result = status.get(14, TimeUnit.MICROSECONDS);
            throw new Exception("Not expecting get(timeout, unit) to succeed prior to task completion: " + result);
        } catch (IllegalStateException x) {
        }
    }

    /**
     * Attempt to schedule a task with the long running hint set to true. Verify it is rejected.
     */
    public void testLongRunningTask(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedFailingTask.clear();
        SharedFailingTask.execProps.put(ManagedTask.LONGRUNNING_HINT, Boolean.TRUE.toString());
        try {
            Callable<Long> task = new SharedFailingTask();
            TaskStatus<Long> status = scheduler.schedule(task, 22, TimeUnit.NANOSECONDS);
            throw new Exception("Should not be able to schedule task with long running hint set to true. " + status);
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().contains(ManagedTask.LONGRUNNING_HINT))
                throw x;
        } finally {
            SharedFailingTask.clear();
        }

    }

    /**
     * testMissedTaskThresholdBelowMinimum - attempt to use a persistent executor where the missedTaskThreshold value is less than
     * the minimum allowed. The detailed error message that is logged is tested by the caller of this method.
     */
    public void testMissedTaskThresholdBelowMinimum(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            PersistentExecutor misconfiguredExecutor = InitialContext.doLookup("concurrent/belowMinMissedTaskThreshold");
            throw new Exception("Should not be able to obtain misconfigured persistentExecutor where the missedTaskThreshold value is less than the minimum allowed " + misconfiguredExecutor);
        } catch (NamingException x) {
            // expected
        }
    }

    /**
     * testMissedTaskThresholdExceedsMaximum - attempt to use a persistent executor where the missedTaskThreshold value exceeds
     * the maximum allowed. The detailed error message that is logged is tested by the caller of this method.
     */
    public void testMissedTaskThresholdExceedsMaximum(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            PersistentExecutor misconfiguredExecutor = InitialContext.doLookup("concurrent/exceedsMaxMissedTaskThreshold");
            throw new Exception("Should not be able to obtain misconfigured persistentExecutor where missedTaskThreshold value exceeds the maximum allowed " + misconfiguredExecutor);
        } catch (NamingException x) {
            // expected
        }
    }

    /**
     * Schedule a task with a negative transaction timeout. Expect IllegalArgumentException.
     */
    public void testNegativeTransactionTimeout(HttpServletRequest request, PrintWriter out) throws Exception {
        TenSecondTask task = new TenSecondTask();
        task.getExecutionProperties().put(PersistentExecutor.TRANSACTION_TIMEOUT, "-2");
        try {
            TaskStatus<Long> status = scheduler.submit(task);
            throw new Exception("Should not be able to submit task with negative transaction timeout " + status);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null || !x.getMessage().equals("com.ibm.ws.concurrent.TRANSACTION_TIMEOUT: -2"))
                throw x;
        }
    }

    /**
     * Schedule a task that requires Classloader Context, but don't capture and propagate that type of context to the task.
     */
    public void testNoClassloaderContext(HttpServletRequest request, PrintWriter out) throws Exception {
        LoadClassTask task = new LoadClassTask();

        System.out.println("Servlet thread class loader " + Thread.currentThread().getContextClassLoader());

        // Validate that it works from the servlet thread
        Class<?> result = task.call();
        if (!PersistentErrorTestServlet.class.equals(result))
            throw new Exception("Unexpected class loaded from servlet thread: " + result);

        // Validate that it fails from persistent executor thread that lacks Classloader Context
        TaskStatus<Class<?>> status = scheduler.submit(task);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (status.isCancelled())
            throw new Exception("Task should not be canceled. " + status);

        if (!status.isDone())
            throw new Exception("Task did not complete in a timely manner. " + status);

        try {
            result = status.get();
            throw new Exception("Unexpectedly able to obtain result: " + result);
        } catch (ExecutionException x) {
            if (x.getCause() instanceof ClassNotFoundException
                || x.getCause() instanceof NullPointerException) {
                StringWriter sw = new StringWriter();
                x.getCause().printStackTrace(new PrintWriter(sw));
                String stack = sw.toString();
                if (!stack.contains("at web.LoadClassTask.call(LoadClassTask.java:"))
                    throw x;
            } else
                throw x;
        }
    }

    /**
     * Schedule a task that requires Java EE Metadata Context, but don't capture and propagate that type of context to the task.
     */
    public void testNoJavaEEMetadataContext(HttpServletRequest request, PrintWriter out) throws Exception {
        LookupTask task = new LookupTask();

        // Validate that it works from the servlet thread
        String result = task.call();
        if (!result.contains("PersistentExecutorImpl@"))
            throw new Exception("Unexpected lookup result from servlet thread: " + result);

        // Validate that it fails from persistent executor thread that lacks Java EE Metadata Context
        TaskStatus<String> status = scheduler.submit(task);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (!status.isDone())
            throw new Exception("Task did not complete in a timely manner. " + status);

        if (status.isCancelled())
            throw new Exception("Task should not be canceled. " + status);

        try {
            result = status.get();
            throw new Exception("Unexpectedly able to obtain result: " + result);
        } catch (ExecutionException x) {
            if (x.getCause() instanceof NamingException) {
                StringWriter sw = new StringWriter();
                x.getCause().printStackTrace(new PrintWriter(sw));
                String stack = sw.toString();
                if (!stack.contains("at web.LookupTask.call(LookupTask.java:"))
                    throw x;
            } else
                throw x;
        }
    }

    /**
     * Schedule a task with a transaction timeout value that isn't an integer. Expect IllegalArgumentException.
     */
    public void testNonIntegerTransactionTimeout(HttpServletRequest request, PrintWriter out) throws Exception {
        TenSecondTask task = new TenSecondTask();
        task.getExecutionProperties().put(PersistentExecutor.TRANSACTION_TIMEOUT, "1.5");
        try {
            TaskStatus<Long> status = scheduler.submit(task);
            throw new Exception("Should not be able to submit task with non-integer transaction timeout " + status);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null
                || !x.getMessage().equals("com.ibm.ws.concurrent.TRANSACTION_TIMEOUT: 1.5")
                || !(x.getCause() instanceof NumberFormatException))
                throw x;
        }
    }

    /**
     * Schedule a task that returns a non-serializable result. Expect the TaskStatus to be returned successfully,
     * but an error must occur when attempting to access the result.
     */
    public void testNonSerializableResult(HttpServletRequest request, PrintWriter out) throws Exception {
        NonSerializableTaskAndResult.resultOverride = null; // just in case any other test forgets to clean it up

        TaskStatus<?> status = scheduler.schedule(new NonSerializableTaskAndResult(), 15, TimeUnit.NANOSECONDS);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (!status.isDone())
            throw new Exception("Task did not complete in a timely manner. " + status);

        if (status.isCancelled())
            throw new Exception("Task should not be canceled. " + status);

        try {
            Object result = status.get();
            throw new Exception("Unexpectedly able to obtain non-serializable result: " + result);
        } catch (ResultNotSerializableException x) {
            if (x.getMessage().indexOf(NonSerializableTaskAndResult.class.getName()) < 0
                || !(x.getCause() instanceof NotSerializableException))
                throw x;
        }
    }

    /**
     * Attempt to create/find/remove null (and empty) property and value.
     */
    public void testNullProperty(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            boolean created = scheduler.createProperty(null, "value1");
            throw new Exception("Should not be able to create a property with null name. Result: " + created);
        } catch (IllegalArgumentException x) {
        }

        try {
            boolean created = scheduler.createProperty("testNullProperty-prop2", null);
            throw new Exception("Should not be able to create a property with null value. Result: " + created);
        } catch (IllegalArgumentException x) {
        }

        try {
            boolean created = scheduler.createProperty("", "value1");
            throw new Exception("Should not be able to create a property with an empty name. Result: " + created);
        } catch (IllegalArgumentException x) {
        }

        try {
            boolean created = scheduler.createProperty("testNullProperty-prop4", "");
            throw new Exception("Should not be able to create a property with an empty value. Result: " + created);
        } catch (IllegalArgumentException x) {
        }

        String value = scheduler.getProperty(null);
        if (value != null)
            throw new Exception("Should not find value for null property name: " + value);

        value = scheduler.getProperty("");
        if (value != null)
            throw new Exception("Should not find value for empty property name: " + value);

        boolean removed = scheduler.removeProperty(null);
        if (removed)
            throw new Exception("Should not be able to remove property with null name");

        removed = scheduler.removeProperty("");
        if (removed)
            throw new Exception("Should not be able to remove property with empty name");
    }

    /**
     * Attempt to submit null tasks. Verify the proper errors are raised.
     */
    public void testNullTasks(HttpServletRequest request, PrintWriter out) throws Exception {
        Trigger trigger = new NextTenthOfSecondTrigger();

        try {
            scheduler.execute(null);
            throw new Exception("execute(null) should fail");
        } catch (NullPointerException x) {
        }

        try {
            ScheduledFuture<Short> status = scheduler.schedule((Callable<Short>) null, trigger);
            throw new Exception("schedule(null Callable,  trigger) should fail. Instead: " + status);
        } catch (NullPointerException x) {
        }

        try {
            ScheduledFuture<Byte> status = scheduler.schedule((Callable<Byte>) null, 222, TimeUnit.HOURS);
            throw new Exception("schedule(null Callable) should fail. Instead: " + status);
        } catch (NullPointerException x) {
        }

        try {
            ScheduledFuture<?> status = scheduler.schedule((Runnable) null, trigger);
            throw new Exception("schedule(null Runnable,  trigger). Instead: " + status);
        } catch (NullPointerException x) {
        }

        try {
            ScheduledFuture<?> status = scheduler.schedule((Runnable) null, 333, TimeUnit.DAYS);
            throw new Exception("schedule(null Runnable) should fail. Instead: " + status);
        } catch (NullPointerException x) {
        }

        try {
            ScheduledFuture<?> status = scheduler.scheduleAtFixedRate(null, 44, 444, TimeUnit.MICROSECONDS);
            throw new Exception("scheduleAtFixedRate(null) should fail. Instead: " + status);
        } catch (NullPointerException x) {
        }

        try {
            ScheduledFuture<?> status = scheduler.scheduleWithFixedDelay(null, 55, 555, TimeUnit.MICROSECONDS);
            throw new Exception("scheduleWithFixedDelay(null) should fail. Instead: " + status);
        } catch (NullPointerException x) {
        }

        try {
            Future<String> status = scheduler.submit((Callable<String>) null);
            throw new Exception("submit(null Callable) should fail. Instead: " + status);
        } catch (NullPointerException x) {
        }

        try {
            scheduler.submit((Runnable) null);
            throw new Exception("submit(null Runnable) should fail");
        } catch (NullPointerException x) {
        }

        try {
            Future<String> status = scheduler.submit((Runnable) null, "ResultValue");
            throw new Exception("submit(null Runnable, result) should fail. Instead: " + status);
        } catch (NullPointerException x) {
        }
    }

    /**
     * Attempt to submit tasks with null Triggers. Verify the proper errors are raised.
     */
    public void testNullTriggers(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            ScheduledFuture<Long> status = scheduler.schedule((Callable<Long>) new SharedCounterTask(), null);
            throw new Exception("schedule(callable, null trigger) should fail. Instead: " + status);
        } catch (NullPointerException x) {
            if (x.getMessage() == null || !x.getMessage().contains(Trigger.class.getName()))
                throw x;
        }

        try {
            ScheduledFuture<?> status = scheduler.schedule((Runnable) new SharedCounterTask(), null);
            throw new Exception("schedule(runnable, null trigger). Instead: " + status);
        } catch (NullPointerException x) {
            if (x.getMessage() == null || !x.getMessage().contains(Trigger.class.getName()))
                throw x;
        }
    }

    /**
     * Attempt to submit tasks with null units. Verify the proper errors are raised.
     */
    public void testNullUnits(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            Future<Long> status = scheduler.schedule((Callable<Long>) new SharedCounterTask(), 6, null);
            throw new Exception("schedule(callable, null units) should fail. Instead: " + status);
        } catch (NullPointerException x) {
        }

        try {
            Future<?> status = scheduler.schedule((Runnable) new SharedCounterTask(), 7, null);
            throw new Exception("schedule(runnable, null units) should fail. Instead: " + status);
        } catch (NullPointerException x) {
        }

        try {
            Future<?> status = scheduler.scheduleAtFixedRate(new SharedCounterTask(), 8, 88, null);
            throw new Exception("scheduleAtFixedRate(null units) should fail. Instead: " + status);
        } catch (NullPointerException x) {
        }

        try {
            Future<?> status = scheduler.scheduleWithFixedDelay(new SharedCounterTask(), 9, 99, null);
            throw new Exception("scheduleWithFixedDelay(null units) should fail. Instead: " + status);
        } catch (NullPointerException x) {
        }
    }

    /**
     * testPollIntervalBelowMinimum - attempt to use a persistent executor where the pollInterval value is less than
     * the minimum allowed. The detailed error message that is logged is tested by the caller of this method.
     */
    public void testPollIntervalBelowMinimum(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            PersistentExecutor misconfiguredExecutor = InitialContext.doLookup("concurrent/belowMinPollInterval");
            throw new Exception("Should not be able to obtain misconfigured persistentExecutor where the pollInterval value is less than the minimum allowed. " + misconfiguredExecutor);
        } catch (NamingException x) {
            // expected
        }
    }

    /**
     * testPollIntervalThresholdExceedsMaximum - attempt to use a persistent executor where the pollInterval value exceeds
     * the maximum allowed. The detailed error message that is logged is tested by the caller of this method.
     */
    public void testPollIntervalExceedsMaximum(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            PersistentExecutor misconfiguredExecutor = InitialContext.doLookup("concurrent/exceedsMaxPollInterval");
            throw new Exception("Should not be able to obtain misconfigured persistentExecutor where the pollInterval value exceeds the maximum allowed " + misconfiguredExecutor);
        } catch (NamingException x) {
            // expected
        }
    }

    /**
     * Attempt to schedule a task with a predetermined result that declares itself serializable but fails to serialize.
     */
    public void testPredeterminedResultFailsToSerialize(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            TaskStatus<?> status = scheduler.submit(new SharedCounterTask(), new ResultThatFailsSerialization());
            throw new Exception("Task should not schedule when its predetermined result fails to serialize. " + status);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null
                || !x.getMessage().contains("ResultThatFailsSerialization@")
                || !(x.getCause() instanceof NotSerializableException))
                throw x;
        }
    }

    /**
     * Attempt to schedule a task with a predetermined result that is not serializable.
     */
    public void testPredeterminedResultIsNotSerializable(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            TaskStatus<?> status = scheduler.submit(new SharedCounterTask(), new Object());
            throw new Exception("Task should not schedule when its predetermined result is not serializable. " + status);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null
                || !x.getMessage().contains("java.lang.Object@")
                || !(x.getCause() instanceof NotSerializableException))
                throw x;
        }
    }

    /**
     * Removes tasks with the specified ids.
     */
    public void testRemoveTasks(HttpServletRequest request, PrintWriter out) throws Exception {
        for (String taskId : request.getParameterValues("taskId")) {
            if (!scheduler.remove(Long.valueOf(taskId)))
                throw new Exception("Did not find task " + taskId);
        }
    }

    /**
     * Schedule a task that returns a result that fails to serialize. Expect the TaskStatus to be returned successfully,
     * but the error must be chained to the ExecutionException (including any chained exceptions)
     * when attempting to access the result.
     */
    public void testResultFailsToSerialize(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            Throwable cause = new ServiceConfigurationError("chained cause 1").initCause(new SQLException("chained cause 2"));
            NonSerializableTaskAndResult.resultOverride = new ResultThatFailsSerialization("testResultFailsToSeralize", cause);

            TaskStatus<?> status = scheduler.schedule(new NonSerializableTaskAndResult(), 16, TimeUnit.MICROSECONDS);

            for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                status = scheduler.getStatus(status.getTaskId());

            if (!status.isDone())
                throw new Exception("Task did not complete in a timely manner. " + status);

            if (status.isCancelled())
                throw new Exception("Task should not be canceled. " + status);

            try {
                Object result = status.get();
                throw new Exception("Unexpectedly able to obtain result: " + result);
            } catch (ResultNotSerializableException x) {
                cause = x.getCause();
                if (x.getMessage().indexOf(ResultThatFailsSerialization.class.getName()) >= 0
                    && cause instanceof ExtendedIllegalArgumentException) {
                    Object value = ((ExtendedIllegalArgumentException) cause).getIllegalArgument();
                    if ("testResultFailsToSeralize".equals(value)) {
                        cause = cause.getCause();
                        if (cause instanceof ServiceConfigurationError && "chained cause 1".equals(cause.getMessage())) {
                            cause = cause.getCause();
                            if (cause instanceof SQLException && "chained cause 2".equals(cause.getMessage())) {
                                cause = cause.getCause();
                                if (cause != null)
                                    throw new Exception("Unexpected third level cause for " + x);
                            } else
                                throw new Exception("Missing or incorrect second level cause or cause message for " + x);
                        } else
                            throw new Exception("Missing or incorrect first level cause or cause message for " + x);
                    } else
                        throw new Exception("Serialized exception has missing or incorrect value " + value, x);
                } else
                    throw x;
            }
        } finally {
            NonSerializableTaskAndResult.resultOverride = null;
        }
    }

    /**
     * Schedule a task where the error that occurs when the result fails to serialize itself fails to serialize.
     * Expect the TaskStatus to be returned successfully, but a serializable copy of the error must be chained to
     * the ExecutionException (including copies of any chained exceptions) when attempting to access the result.
     */
    public void testResultSerializationFailureFailsToSerialize(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            Throwable cause = new IllegalStateException("chained cause");
            NonSerializableTaskAndResult.resultOverride = new ResultThatFailsSerialization(new ThreadGroup("this is not serializable"), cause);

            TaskStatus<?> status = scheduler.schedule(new NonSerializableTaskAndResult(), new TwoExecutionsTrigger());

            for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                status = scheduler.getStatus(status.getTaskId());

            if (!status.hasResult())
                throw new Exception("Task did not complete any executions in a timely manner. " + status);

            try {
                Object result = status.getResult();
                throw new Exception("Unexpectedly able to obtain result: " + result);
            } catch (ResultNotSerializableException x) {
                cause = x.getCause();
                if (x.getMessage().indexOf(ResultThatFailsSerialization.class.getName()) >= 0
                    && cause != null && cause.getClass().equals(RuntimeException.class)) {
                    cause = cause.getCause();
                    if (cause != null && cause.getClass().equals(RuntimeException.class) && "java.lang.IllegalStateException: chained cause".equals(cause.getMessage())) {
                        cause = cause.getCause();
                        if (cause != null)
                            throw new Exception("Unexpected second level cause for exception", x);
                    } else
                        throw new Exception("Missing or incorrect first level cause or cause message for exception", x);
                } else
                    throw x;
            }
        } finally {
            NonSerializableTaskAndResult.resultOverride = null;
        }
    }

    /**
     * Schedule a task that fails its first execution attempt, but runs successfully the next time,
     * and auto purges (by default) upon completion.
     */
    public void testRetryFailedTaskAndAutoPurge(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedFailingTask.clear();
        SharedFailingTask.failOn.add(1l);
        try {
            Callable<Long> task = new SharedFailingTask();
            TaskStatus<Long> status = scheduler.schedule(task, 17, TimeUnit.MICROSECONDS);

            for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                status = scheduler.getStatus(status.getTaskId());

            if (status != null)
                throw new Exception("Task did not complete in a timely manner or was not autopurged upon completion. " + status);

            long counter = SharedFailingTask.counter.get();
            if (counter != 2)
                throw new Exception("Task should be attempted exactly 2 times (with the first attempt failing). Instead " + counter);
        } finally {
            SharedFailingTask.clear();
        }
    }

    /**
     * Schedule a task that fails its first execution attempt, but runs successfully the next time,
     * and remains in the persistent store upon completion.
     */
    public void testRetryFailedTaskNoAutoPurge(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedFailingTask.clear();
        SharedFailingTask.execProps.put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.toString());
        SharedFailingTask.failOn.add(1l);
        try {
            Callable<Long> task = new SharedFailingTask();
            TaskStatus<Long> status = scheduler.schedule(task, 18, TimeUnit.NANOSECONDS);

            for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
                status = scheduler.getStatus(status.getTaskId());

            if (!status.isDone() || status.isCancelled())
                throw new Exception("Task did not complete successfully. " + status);

            Long result = status.get();
            if (!Long.valueOf(2).equals(result))
                throw new Exception("Task should be attempted exactly 2 times (with the first attempting failing). Instead " + result);
        } finally {
            SharedFailingTask.clear();
        }
    }

    /**
     * testRetryIntervalAndMissedTaskThresholdBothEnabled - attempt to use a persistent executor where the retryInterval and
     * the missedTaskThreshold are both enabled. The detailed error message that is logged is tested by the caller of this method.
     */
    public void testRetryIntervalAndMissedTaskThresholdBothEnabled(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            PersistentExecutor misconfiguredExecutor = InitialContext.doLookup("concurrent/retryIntervalAndMissedTaskThresholdBothEnabled");
            throw new Exception("Should not be able to obtain misconfigured persistentExecutor where the retryInterval and missedTaskThreshold are both enabled. " + misconfiguredExecutor);
        } catch (NamingException x) {
            // expected
        }
    }

    /**
     * testRollbackWhenMissedTaskThresholdExceeded - verify that a task's transaction times out by having the task wait for
     * the transaction status to be set to rollback-only.
     */
    public void testRollbackWhenMissedTaskThresholdExceeded(HttpServletRequest request, PrintWriter out) throws Exception {
        WaitForRollbackTask task = new WaitForRollbackTask();
        TaskStatus<Integer> status = scheduler.schedule(task, 4, TimeUnit.MILLISECONDS);
        for (long start = System.nanoTime(); status != null && !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; status = scheduler.getStatus(status.getTaskId()))
            Thread.sleep(POLL_INTERVAL);
        try {
            Integer result = status.get();
            throw new Exception("Should not be able to get a result from a task that always rolls back: " + result);
        } catch (AbortedException x) {
            if (x.getMessage() == null || !x.getMessage().startsWith("CWWKC1555E")
                    || !(x.getCause() instanceof RollbackException)
                    || x.getCause().getMessage() == null
                    || !x.getCause().getMessage().startsWith("CWWKC1505E")
                    || !x.getCause().getMessage().contains(" 4 ")) // value of missedTaskThreshold in seconds
                throw x;
        }
    }

    /**
     * Schedule some tasks that will remain pending for the distant future during a dump of the server.
     * Report the task IDs so that the controlling test can look for them in the output.
     */
    public void testScheduleDistantIntrospectableTasks(HttpServletRequest request, PrintWriter out) throws Exception {
        TaskStatus<?> statusA = scheduler.scheduleAtFixedRate(new SharedCounterTask(), 32, 32, TimeUnit.DAYS);

        TaskStatus<?> statusB = scheduler.schedule((Runnable) new SharedCounterTask(), 32, TimeUnit.HOURS);

        out.println("TASKS ARE " + statusA.getTaskId() + "," + statusB.getTaskId() + ".");
    }

    /**
     * Schedule some tasks that will run frequently during a dump of the server.
     * Report the task IDs so that the controlling test can look for them in the output.
     */
    public void testScheduleFrequentIntrospectableTasks(HttpServletRequest request, PrintWriter out) throws Exception {
        TaskStatus<?> statusA = scheduler.scheduleWithFixedDelay(new SharedCounterTask(), 0, 300, TimeUnit.MILLISECONDS);

        TaskStatus<?> statusB = scheduler.scheduleAtFixedRate(new SharedCounterTask(), 0, 333, TimeUnit.MILLISECONDS);

        out.println("TASKS ARE " + statusA.getTaskId() + "," + statusB.getTaskId() + ".");
    }

    /**
     * Schedule some tasks that will run or remain pending during a dump of the server.
     * Report the task IDs so that the controlling test can look for them in the output.
     */
    public void testScheduleIntrospectableTasks(HttpServletRequest request, PrintWriter out) throws Exception {
        TaskStatus<?> statusA = scheduler.scheduleWithFixedDelay(new SharedCounterTask(), 0, 250, TimeUnit.MILLISECONDS);

        TaskStatus<?> statusB = scheduler.scheduleAtFixedRate(new SharedCounterTask(), 0, 26, TimeUnit.DAYS);

        TaskStatus<?> statusC = scheduler.schedule((Runnable) new SharedCounterTask(), 27, TimeUnit.HOURS);

        out.println("TASKS ARE " + statusA.getTaskId() + "," + statusB.getTaskId() + "," + statusC.getTaskId() + ".");
    }

    /**
     * Shut down the database before a task executes. Connection errors will occur,
     * but the task should be retried and succeed on the next attempt.
     */
    public void testShutDownDerbyBeforeTaskExecution(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedCounterTask.counter.set(0);
        SharedCounterTask task = new SharedCounterTask();
        TaskStatus<?> status = scheduler.scheduleAtFixedRate(task, 1, 1, TimeUnit.SECONDS);

        try {
            shutdownDB.getConnection();
            throw new Exception("Failed to shut down the database");
        } catch (SQLException x) {
            // expected
        }

        long counter0 = SharedCounterTask.counter.get();
        for (long start = System.nanoTime(); SharedCounterTask.counter.get() == counter0 && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL));

        if (SharedCounterTask.counter.get() == counter0)
            throw new Exception("Task " + status.getTaskId() + " did not run successfully after Derby shutdown.");

        if (!status.cancel(false))
            throw new Exception("Unable to cancel task");
    }

    /**
     * Shut down the database while a task is running. Connection errors will occur,
     * but the task should be retried and succeed on the next attempt.
     */
    public void testShutDownDerbyDuringTaskExecution(HttpServletRequest request, PrintWriter out) throws Exception {
        DerbyShutdownTask task = new DerbyShutdownTask(); // auto-purges upon successful completion
        DerbyShutdownTask.counter.set(0);

        TaskStatus<?> status = scheduler.schedule(task, 0, TimeUnit.HOURS);
        long taskId = status.getTaskId();

        long start = System.nanoTime();
        while (DerbyShutdownTask.counter.get() < 2 && System.nanoTime() - start < TIMEOUT_NS)
            Thread.sleep(POLL_INTERVAL);

        // Also wait for the task to have completed (auto-purged null status), so that we know it is actually successful again vs another attempt that is still failing.
        while (status != null && System.nanoTime() - start < TIMEOUT_NS) {
            Thread.sleep(POLL_INTERVAL);
            status = scheduler.getStatus(status.getTaskId());
        }

        int count = DerbyShutdownTask.counter.get();
        if (count != 2 || status != null)
            throw new Exception("Task " + taskId + " (" + status + ") did not run exactly once successfully after Derby shutdown. Attempt count: " + count);
    }

    /**
     * Trigger.skipRun fails causing the first execution attempt to be skipped. The second attempt should succeed.
     */
    public void testSkipRunFailsOnFirstExecutionAttempt(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedCounterTask.counter.set(0);
        SharedCounterTask task = new SharedCounterTask();

        ImmediateSkippingTrigger trigger = new ImmediateSkippingTrigger(2);
        trigger.skipExecutionAttemptsWithFailure.add(1);

        TaskStatus<Long> status = scheduler.schedule((Callable<Long>) task, trigger);

        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS_FFDC_PATH; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (status != null)
            throw new Exception("Task did not complete in a timely manner or was not autopurged upon completion. " + status);

        long counter = SharedCounterTask.counter.get();
        if (counter != 1)
            throw new Exception("Task should be run exactly once (after skipping the first attempt). Instead " + counter);
    }

    /**
     * Trigger.skipRun fails causing the last execution attempt to be skipped. The first attempt should succeed.
     * The task entry should be autopurged because it was not failed or canceled.
     * In terms of autopurging, it should make no difference whether the first, last, or any other executions were skipped.
     */
    public void testSkipRunFailsOnLastExecutionAttempt(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedCounterTask.counter.set(0);
        SharedCounterTask task = new SharedCounterTask();

        ImmediateSkippingTrigger trigger = new ImmediateSkippingTrigger(2);
        trigger.skipExecutionAttemptsWithFailure.add(2);

        TaskStatus<Long> status = scheduler.schedule((Callable<Long>) task, trigger);

        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS_FFDC_PATH; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (status != null)
            throw new Exception("Task did not complete successfully or failed to autopurge upon completion. " + status);
    }

    /**
     * Trigger.skipRun fails causing the last execution attempt to be skipped. The first attempt should succeed.
     * Verify that the task entry remains in the persistent store because we disabled autopurge.
     */
    public void testSkipRunFailsOnLastExecutionAttemptNoAutoPurge(HttpServletRequest request, PrintWriter out) throws Exception {
        NonSerializableTaskAndResult task = new NonSerializableTaskAndResult();

        ImmediateSkippingTrigger trigger = new ImmediateSkippingTrigger(2);
        trigger.skipExecutionAttemptsWithFailure.add(2);

        TaskStatus<?> status = scheduler.schedule(task, trigger);

        for (long start = System.nanoTime(); !status.toString().contains("SKIPPED") && System.nanoTime() - start < TIMEOUT_NS_FFDC_PATH; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (!status.isDone() || status.isCancelled())
            throw new Exception("Task did not complete successfully. " + status);

        try {
            Object result = status.get();
            throw new Exception("Expecting the second execution attempt to be skipped. Instead result is: " + result);
        } catch (SkippedException x) {
            if (!(x.getCause() instanceof ArrayIndexOutOfBoundsException) || x.getCause().getMessage() == null || x.getCause().getMessage().indexOf(" 2.") < 0)
                throw x;
        }
    }

    /**
     * Trigger.skipRun fails causing the second and third execution attempts to be skipped.
     * The first and last attempts should succeed.
     */
    public void testSkipRunFailsOnMiddleExecutionAttempts(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedCounterTask.counter.set(0);
        SharedCounterTask task = new SharedCounterTask();

        ImmediateSkippingTrigger trigger = new ImmediateSkippingTrigger(4);
        trigger.skipExecutionAttemptsWithFailure.add(2);
        trigger.skipExecutionAttemptsWithFailure.add(3);

        TaskStatus<Long> status = scheduler.schedule((Callable<Long>) task, trigger);

        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS_FFDC_PATH; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (status != null)
            throw new Exception("Task did not complete in a timely manner or was not autopurged upon completion. " + status);

        long counter = SharedCounterTask.counter.get();
        if (counter != 2)
            throw new Exception("Task should be run exactly twice (once initially, and then after skipping the second and third attempts). Instead " + counter);
    }

    /**
     * Trigger.skipRun fails causing the only execution attempt to be skipped.
     * The task should be autopurged because it was not failed or canceled.
     * In terms of autopurging, it should make no difference whether the first, last, or any other executions were skipped.
     */
    public void testSkipRunFailsOnOnlyExecutionAttempt(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedCounterTask.counter.set(0);
        SharedCounterTask task = new SharedCounterTask();

        ImmediateSkippingTrigger trigger = new ImmediateSkippingTrigger(1);
        trigger.skipExecutionAttemptsWithFailure.add(1);

        TaskStatus<Long> status = scheduler.schedule((Callable<Long>) task, trigger);

        for (long start = System.nanoTime(); status != null && System.nanoTime() - start < TIMEOUT_NS_FFDC_PATH; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (status != null)
            throw new Exception("Task did not complete successfully or failed to autopurge upon completion. " + status);
    }

    /**
     * Trigger.skipRun fails causing the only execution attempt to be skipped.
     * Verify that the task entry remains in the persistent store because we disabled autopurge.
     */
    public void testSkipRunFailsOnOnlyExecutionAttemptNoAutoPurge(HttpServletRequest request, PrintWriter out) throws Exception {
        SharedCounterTask.counter.set(0);
        NonSerializableTaskAndResult task = new NonSerializableTaskAndResult();

        ImmediateSkippingTrigger trigger = new ImmediateSkippingTrigger(1);
        trigger.skipExecutionAttemptsWithFailure.add(1);

        TaskStatus<?> status = scheduler.schedule(task, trigger);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS_FFDC_PATH; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (!status.isDone() || status.isCancelled())
            throw new Exception("Task did not complete successfully. " + status);

        try {
            Object result = status.get();
            throw new Exception("Expecting the only execution attempt to be skipped. Instead result is: " + result);
        } catch (SkippedException x) {
            if (!(x.getCause() instanceof ArrayIndexOutOfBoundsException) || x.getCause().getMessage() == null || x.getCause().getMessage().indexOf(" 1.") < 0)
                throw x;
        }
    }

    /**
     * Attempt to schedule a task that declares itself serializable but fails to serialize.
     */
    public void testTaskFailsToSerialize(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            TaskStatus<?> status = scheduler.schedule(new TaskThatFailsSerialization(), 21, TimeUnit.DAYS);
            throw new Exception("Task should not schedule when it fails to serialize. " + status);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null
                || !x.getMessage().contains("TaskThatFailsSerialization@")
                || !x.getMessage().contains("This is a task that says it is serializable but intentionally fails serialization")
                || !(x.getCause() instanceof NotSerializableException))
                throw x;
        }
    }

    /**
     * Schedule a task that exceeds the transaction timeout when it runs.
     */
    public void testTransactionTimeout(HttpServletRequest request, PrintWriter out) throws Exception {
        TenSecondTask task = new TenSecondTask();
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.name());
        task.getExecutionProperties().put(ManagedTask.IDENTITY_NAME, "testTransactionTimeout");
        task.getExecutionProperties().put(PersistentExecutor.TRANSACTION_TIMEOUT, "1");

        TaskStatus<Long> status = scheduler.submit(task);
        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        try {
            status.get();
            throw new Exception("Task should have rolled back twice and aborted due to transaction timeout. " + status);
        } catch (AbortedException x) {
            if (x.getMessage() == null || !x.getMessage().startsWith("CWWKC1555E") || x.getCause() == null)
                throw x;
        }
    }

    /**
     * Schedule a task that exceeds the transaction timeout of the suspended persistent executor transaction when it runs.
     */
    public void testTransactionTimeoutSuspendedTransaction(HttpServletRequest request, PrintWriter out) throws Exception {
        TenSecondTask task = new TenSecondTask();
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.NEVER.name());
        task.getExecutionProperties().put(ManagedTask.IDENTITY_NAME, "testTransactionTimeoutSuspendedTransaction");
        task.getExecutionProperties().put(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);
        task.getExecutionProperties().put(PersistentExecutor.TRANSACTION_TIMEOUT, "1");

        TaskStatus<Long> status = scheduler.submit(task);
        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        try {
            status.get();
            throw new Exception("Task should have rolled back twice and aborted due to transaction timeout. " + status);
        } catch (AbortedException x) {
            if (x.getMessage() == null || !x.getMessage().startsWith("CWWKC1555E") || x.getCause() == null)
                throw x;
        }
    }

    /**
     * When Trigger.getNextRunTime fails after a task runs, the execution must be rolled back and retried until the failure is reported.
     */
    public void testTriggerFailsGetNextRunTimeAfterTaskRuns(HttpServletRequest request, PrintWriter out) throws Exception {
        Callable<Long> task = new SharedCounterTask();
        FailingTrigger trigger = new FailingTrigger();
        trigger.allowFirstGetNextRunTime = true;

        TaskStatus<Long> status = scheduler.schedule(task, trigger);

        for (long start = System.nanoTime(); !status.hasResult() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL))
            status = scheduler.getStatus(status.getTaskId());

        if (!status.isDone() || status.isCancelled())
            throw new Exception("Task did not complete. " + status);

        try {
            Long result = status.get();
            throw new Exception("Expecting all attempts to roll back. Instead result is: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }
    }

    /**
     * Attempt to schedule a task where Trigger.getNextRunTime fails.
     */
    public void testTriggerFailsInitialGetNextRunTime(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            TaskStatus<?> status = scheduler.schedule((Callable<Long>) new SharedCounterTask(), new FailingTrigger());
            throw new Exception("Task should not schedule when the Trigger fails getNextRunTime: " + status);
        } catch (RejectedExecutionException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }
    }

    /**
     * Attempt to schedule a task where the Trigger declares itself serializable but fails to serialize.
     */
    public void testTriggerFailsToSerialize(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            TaskStatus<?> status = scheduler.schedule((Runnable) new SharedCounterTask(), new TriggerThatFailsSerialization());
            throw new Exception("Task should not schedule when the Trigger fails to serialize. " + status);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null
                || !x.getMessage().contains("TriggerThatFailsSerialization@")
                || !(x.getCause() instanceof NotSerializableException))
                throw x;
        }
    }

    /**
     * Attempt to schedule a task with a trigger that has no executions. Verify the proper error is raised.
     */
    public void testTriggerWithNoExecutions(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            TaskStatus<?> status = scheduler.schedule((Callable<Long>) new SharedCounterTask(), new NoExecutionsTrigger());
            throw new Exception("Task (Callable) should not schedule when there are no executions: " + status);
        } catch (RejectedExecutionException x) {
            if (!"Trigger.getNextRunTime: null".equals(x.getMessage()))
                throw x;
        }

        try {
            TaskStatus<?> status = scheduler.schedule((Runnable) new SharedCounterTask(), new NoExecutionsTrigger());
            throw new Exception("Task (Runnable) should not schedule when there are no executions: " + status);
        } catch (RejectedExecutionException x) {
            if (!"Trigger.getNextRunTime: null".equals(x.getMessage()))
                throw x;
        }
    }

    /**
     * Attempt to invoke unsupported methods. Verify the proper errors are raised.
     */
    public void testUnsupportedOperations(HttpServletRequest request, PrintWriter out) throws Exception {
        @SuppressWarnings("unchecked")
        Collection<Callable<Long>> tasks = Arrays.asList((Callable<Long>) new SharedCounterTask(),
                                                         (Callable<Long>) new SharedCounterTask());

        try {
            boolean result = scheduler.awaitTermination(5, TimeUnit.MINUTES);
            throw new Exception("awaitTermination should not be permitted. Instead result is: " + result);
        } catch (IllegalStateException x) {
        }

        try {
            List<Future<Long>> futures = scheduler.invokeAll(tasks);
            throw new Exception("invokeAll should not be permitted. Instead result is: " + futures);
        } catch (UnsupportedOperationException x) {
        }

        try {
            List<Future<Long>> futures = scheduler.invokeAll(tasks, 1, TimeUnit.MINUTES);
            throw new Exception("invokeAll(timeout) should not be permitted. Instead result is: " + futures);
        } catch (UnsupportedOperationException x) {
        }

        try {
            Long result = scheduler.invokeAny(tasks);
            throw new Exception("invokeAny should not be permitted. Instead result is: " + result);
        } catch (UnsupportedOperationException x) {
        }

        try {
            Long result = scheduler.invokeAny(tasks, 2, TimeUnit.MINUTES);
            throw new Exception("invokeAny(timeout) should not be permitted. Instead result is: " + result);
        } catch (UnsupportedOperationException x) {
        }

        try {
            boolean result = scheduler.isShutdown();
            throw new Exception("isShutdown should not be permitted. Instead result is: " + result);
        } catch (IllegalStateException x) {
        }

        try {
            boolean result = scheduler.isTerminated();
            throw new Exception("isTerminated should not be permitted. Instead result is: " + result);
        } catch (IllegalStateException x) {
        }

        try {
            scheduler.shutdown();
            throw new Exception("shutdown should not be permitted");
        } catch (IllegalStateException x) {
        }

        try {
            List<Runnable> results = scheduler.shutdownNow();
            throw new Exception("shutdownNow should not be permitted. Instead result is: " + results);
        } catch (IllegalStateException x) {
        }
    }

    /**
     * Attempt to submit repeating tasks with non-positive intervals. Verify the proper errors are raised.
     */
    public void testZeroOrNegativeIntervals(HttpServletRequest request, PrintWriter out) throws Exception {
        try {
            Future<?> status = scheduler.scheduleAtFixedRate(new SharedCounterTask(), 10, 0, TimeUnit.HOURS);
            throw new Exception("scheduleAtFixedRate(negative interval) should fail. Instead: " + status);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null || !x.getMessage().contains("0"))
                throw x;
        }

        try {
            Future<?> status = scheduler.scheduleWithFixedDelay(new SharedCounterTask(), 11, 0, TimeUnit.DAYS);
            throw new Exception("scheduleWithFixedDelay(negative interval) should fail. Instead: " + status);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null || !x.getMessage().contains("0"))
                throw x;
        }

        try {
            Future<?> status = scheduler.scheduleAtFixedRate(new SharedCounterTask(), 12, -12, TimeUnit.MINUTES);
            throw new Exception("scheduleAtFixedRate(negative interval) should fail. Instead: " + status);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null || !x.getMessage().contains("-12"))
                throw x;
        }

        try {
            Future<?> status = scheduler.scheduleWithFixedDelay(new SharedCounterTask(), 13, -13, TimeUnit.NANOSECONDS);
            throw new Exception("scheduleWithFixedDelay(negative interval) should fail. Instead: " + status);
        } catch (IllegalArgumentException x) {
            if (x.getMessage() == null || !x.getMessage().contains("-13"))
                throw x;
        }
    }
}
