/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import web.task.CancelTask;
import web.task.MultiRunSeparateTriggerFailSometimesResultTask;
import web.task.MultiRunTriggerResultTask;
import web.task.SingleRunResultTask;
import web.task.SingleRunTask;

@SuppressWarnings("serial")
@WebServlet("/*")
public class LoadTestServlet extends FATServlet {

    /**
     * Interval in milliseconds between polling for task results.
     */
    private static final long POLL_INTERVAL = 200;

    /**
     * Maximum number of nanoseconds to wait for a task to finish.
     */
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(60);

    /**
     * Test result tracker.
     */
    @Inject
    private TaskResultStore taskResultStore;

    @Inject
    Instance<SingleRunTask> singleRunTaskFactory;

    @Inject
    Instance<SingleRunResultTask> singleRunResultTaskFactory;

    @Inject
    Instance<CancelTask> cancelTaskFactory;

    @Inject
    Instance<MultiRunTriggerResultTask> multiRunTriggerResultTaskFactory;

    @Inject
    Instance<MultiRunSeparateTriggerFailSometimesResultTask> multiRunSeparateTriggerFailSometimesResultTaskFactory;

    /**
     * Should we keep waiting for results?
     */
    private boolean keepWaiting(long startNanos) {
        return ((taskResultStore.check(null) == false) && ((System.nanoTime() - startNanos) < TIMEOUT_NS));
    }

    /**
     * Check for tasks still in the database after the run is finished.
     */
    private void checkForStragglers(PersistentExecutor scheduler) throws Exception {
        List<Long> ids = scheduler.findTaskIds("%", null, TaskState.ANY, true, null, null);
        if (ids.isEmpty() == false) {
            StringBuilder sb = new StringBuilder();
            sb.append("These " + ids.size() + " tasks have not completed: ");
            for (long l : ids) {
                sb.append(l + ", ");
            }
            throw new Exception(sb.toString());
        }
    }

    /**
     * Schedule a batch of single-run tasks, and ensure that they all complete.
     */
    @Test
    @AllowedFFDC({
        "javax.transaction.RollbackException", // under load, task executions sometimes roll back due to the 4 second transaction timeout that is used by the test
        "javax.persistence.PersistenceException" // can wrap the RollbackException
    })
    public void testShortRun() throws Exception {
        // Clear results to start.
        taskResultStore.clear();

        // Use Derby once to get any one-time initialization out of the way before we start timing results
        PersistentExecutor scheduler = taskResultStore.getPeristentExecutor();
        scheduler.getStatus(0);

        // Register 200 tasks to run at the default delay.
        for (int x = 0; x < 200; x++) {
            scheduler.schedule(singleRunTaskFactory.get(), SingleRunTask.DEFAULT_DELAY, TimeUnit.MILLISECONDS);
        }

        // Register another 200 tasks to run at the default delay.
        for (int x = 0; x < 200; x++) {
            scheduler.schedule(singleRunResultTaskFactory.get(), SingleRunResultTask.DEFAULT_DELAY, TimeUnit.MILLISECONDS);
        }

        // Wait for the tasks to run.  We'll check the results every little bit to see if
        // we're done, and then we'll wait a little bit more and check again in case there
        // are any stragglers.
        long start = System.nanoTime();
        while (keepWaiting(start) == true) {
            Thread.sleep(POLL_INTERVAL);
        }

        // Check for stragglers...
        checkForStragglers(scheduler);

        // Make sure the results are good.
        if (taskResultStore.check(System.out) == false) {
            throw new Exception("Result check failed");
        }
    }

    /**
     * A more comprehensive test - run for about five minutes with both single-run
     * and repeating tasks.
     */
    @Test
    @Mode(TestMode.FULL)
    @AllowedFFDC({
        "java.lang.Exception",
        "javax.transaction.RollbackException", // under load, task executions sometimes roll back due to the 4 second transaction timeout that is used by the test
        "javax.persistence.PersistenceException" // can wrap the RollbackException
    })
    public void testFiveMinuteRun() throws Exception {
        // Clear results to start.
        taskResultStore.clear();
        final ScheduledExecutorService unmanagedExecutor = taskResultStore.getUnmanagedScheduledExecutor();
        final PersistentExecutor scheduler = taskResultStore.getPeristentExecutor();

        // Use Derby once to get any one-time initialization out of the way before we start timing
        scheduler.getStatus(0);

        final long durationMinutes = 5L;
        final long startTimeNanos = System.nanoTime();
        final long endTimeNanos = startTimeNanos + TimeUnit.MINUTES.toNanos(durationMinutes);

        // Register our repeating tasks here.
        for (int x = 0; x < 25; x++) {
            // This is a task + trigger combo that returns a result.
            MultiRunTriggerResultTask task = multiRunTriggerResultTaskFactory.get();
            final int resultID = task.initialize(TimeUnit.MINUTES.toSeconds(durationMinutes));
            TaskStatus<Long> ts = scheduler.schedule(task, task); // Schedule with trigger
            unmanagedExecutor.execute(new MultiRunTriggerResultTask.ResultGenerator(resultID, ts.getTaskId(), taskResultStore));

            // This is a task with a separate trigger that returns a result, or fails.
            MultiRunSeparateTriggerFailSometimesResultTask task2 = multiRunSeparateTriggerFailSometimesResultTaskFactory.get();
            final int resultID2 = task2.initialize(TimeUnit.MINUTES.toSeconds(durationMinutes));
            TaskStatus<Long> ts2 = scheduler.schedule(task2, task2.createTrigger()); // Schedule with trigger
            unmanagedExecutor.execute(new MultiRunSeparateTriggerFailSometimesResultTask.ResultGenerator(resultID2, ts2.getTaskId(), taskResultStore));

            // Sleep for just a short while to put some space between the tasks.
            Thread.sleep(50);
        }

        // While the test runs, schedule batches of single-run tasks.
        while (System.nanoTime() < endTimeNanos) {

            // Register 10 tasks to run at the default delay.
            for (int x = 0; x < 10; x++) {
                scheduler.schedule(singleRunTaskFactory.get(), SingleRunTask.DEFAULT_DELAY, TimeUnit.MILLISECONDS);
            }

            // Register another 10 tasks to run at the default delay.
            for (int x = 0; x < 10; x++) {
                scheduler.schedule(singleRunResultTaskFactory.get(), SingleRunResultTask.DEFAULT_DELAY, TimeUnit.MILLISECONDS);
            }

            // Register 10 tasks that will get cancelled.
            for (int x = 0; x < 10; x++) {
                CancelTask task = cancelTaskFactory.get();
                final int resultID = task.getResultID();
                final TaskStatus<?> status = scheduler.schedule(task, CancelTask.DEFAULT_DELAY, TimeUnit.MILLISECONDS);
                unmanagedExecutor.schedule(new Runnable() {

                    @Override
                    public void run() {
                        taskResultStore.taskExecuted(resultID, status.cancel(false), null);
                        scheduler.remove(status.getTaskId());
                    }

                }, 500, TimeUnit.MILLISECONDS);
            }

            Thread.sleep(1000); // Sleep for 1 second
        }

        // Wait for the tasks to run.  We'll check the results every little bit to see if
        // we're done, and then we'll wait a little bit more and check again in case there
        // are any stragglers.
        long startWaitingNanos = System.nanoTime();
        while (keepWaiting(startWaitingNanos) == true) {
            Thread.sleep(POLL_INTERVAL);
        }

        // Check for stragglers...
        checkForStragglers(scheduler);

        // Make sure the results are good.
        if (taskResultStore.check(System.out) == false) {
            throw new Exception("Result check failed");
        }
    }
}
