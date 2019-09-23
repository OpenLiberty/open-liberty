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
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.Trigger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import com.ibm.websphere.concurrent.persistent.AutoPurge;
import com.ibm.websphere.concurrent.persistent.PersistentExecutor;
import com.ibm.websphere.concurrent.persistent.TaskState;
import com.ibm.websphere.concurrent.persistent.TaskStatus;

@WebServlet("/*")
public class PersistentExecDisabledTestServlet extends HttpServlet {
    private static final long serialVersionUID = 8447513765214641067L;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Resource(lookup = "concurrent/myScheduler")
    private PersistentExecutor scheduler;

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
            getClass().getMethod(test, PrintWriter.class).invoke(this, out);
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
     * Cancel a task that won't run and verify that the entry for the task is purged from the persistent store.
     */
    public void testCancelAndAutoPurge(PrintWriter out) throws Exception {
        NamedTask task = new NamedTask("testCancelAndAutoPurge");
        task.getExecutionProperties().put(AutoPurge.PROPERTY_NAME, AutoPurge.ALWAYS.toString());
        TaskStatus<?> status = scheduler.submit((Runnable) task);
        if (!status.cancel(true))
            throw new Exception("Unable to cancel " + status);

        long taskId = status.getTaskId();
        status = scheduler.getStatus(taskId);
        if (status != null)
            throw new Exception("Should have been autopurged after cancel. Instead: " + status);

        if (scheduler.remove(taskId))
            throw new Exception("Should not be able to remove task because it should have been autopurged on cancel. " + status);
    }

    /**
     * Cancel a task that won't run and verify that an entry for the canceled task remains in the persistent store.
     */
    public void testCancelNoAutoPurge(PrintWriter out) throws Exception {
        TaskStatus<String> status = scheduler.submit((Callable<String>) new NamedTask("testCancelNoAutoPurge"));
        if (!status.cancel(true))
            throw new Exception("Unable to cancel " + status);

        status = scheduler.getStatus(status.getTaskId());
        if (status == null || !status.isCancelled() || !status.isDone())
            throw new Exception("Unexpected status after cancel: " + status);

        try {
            String result = status.get();
            throw new Exception("Canceled tasks shouldn't return results: " + result);
        } catch (CancellationException x) {
        }

        if (status.cancel(true))
            throw new Exception("Second cancel attempt should return false for " + status);
    }

    /**
     * Submit a task for execution to a persistent executor that is configured not to run tasks.
     */
    public void testExecute(PrintWriter out) throws Exception {
        scheduler.execute(new NamedTask("testExecute"));

        int numCanceled = scheduler.cancel("testExecute", '\\', TaskState.SUSPENDED, true);
        if (numCanceled != 0)
            throw new Exception("Should not find any SUSPENDED tasks to cancel. Instead found " + numCanceled);

        numCanceled = scheduler.cancel("testExe_ute", '\\', TaskState.SCHEDULED, true);
        if (numCanceled != 1)
            throw new Exception("Should find exactly 1 task to cancel. Instead found " + numCanceled);

        numCanceled = scheduler.cancel("testExe_ute", '\\', TaskState.CANCELED, true);
        if (numCanceled != 0)
            throw new Exception("Should not be able to cancel any tasks that already CANCELED. Instead: " + numCanceled);
    }

    /**
     * Submit a task for execution every hour. Obtain the name from the task status.
     */
    public void testNameAndTriggerOnInitialTaskStatus(PrintWriter out) throws Exception {
        String name = "testNameAndTriggerOnInitialTaskStatus";
        Runnable task = new NamedTask(name);
        Trigger trigger = new HourlyTrigger();
        TaskStatus<?> status = scheduler.schedule(task, trigger);

        String name1 = status.getTaskName();
        if (!name.equals(name1))
            throw new Exception("Initial task status has wrong name: " + name + " " + status);

        ((ManagedTask) task).getExecutionProperties().put(ManagedTask.IDENTITY_NAME, "overwritten");
        String name2 = status.getTaskName();
        if (!name.equals(name2))
            throw new Exception("After update to task, initial task status has wrong name: " + name + " " + status);

        // The ability to obtain a Trigger from TaskStatus is available only when using TimerTrigger.
    }

    /**
     * Remove a task that won't run and verify that the entry for the task is no longer present in the persistent store.
     */
    public void testRemove(PrintWriter out) throws Exception {
        TaskStatus<String> status = scheduler.submit(new NamedTask("testRemove"), "testRemove-result");
        long taskId = status.getTaskId();

        if (!scheduler.remove(taskId))
            throw new Exception("Unable to remove " + status);

        status = scheduler.getStatus(taskId);
        if (status != null)
            throw new Exception("Task entry was not removed: " + status);

        if (scheduler.remove(taskId))
            throw new Exception("Should not be able to remove task that was already removed. " + status);
    }

    /**
     * Schedule a task with fixed rate to a persistent executor that is configured not to run tasks.
     */
    public void testScheduleAtFixedRate(PrintWriter out) throws Exception {
        TaskStatus<?> status1 = scheduler.scheduleAtFixedRate(new NamedTask("testScheduleAtFixedRate"), 0, 1, TimeUnit.HOURS);

        long taskId = status1.getTaskId();
        TaskStatus<?> status2 = scheduler.getStatus(taskId);

        if (status2.hasResult() || status2.getNextExecutionTime() == null)
            throw new Exception("Unexpected status " + status2);

        try {
            boolean done = status2.isDone();
            throw new Exception("Task with no executions completed shouldn't be isDone=" + done + ". " + status2);
        } catch (IllegalStateException x) {
        }

        try {
            boolean canceled = status2.isCancelled();
            throw new Exception("Task with no executions completed shouldn't be isCancelled=" + canceled + ". " + status2);
        } catch (IllegalStateException x) {
        }

        int compare = status1.compareTo(status2);
        if (compare != 0)
            throw new Exception("Unequal status " + status1 + " AND " + status2);

        try {
            long delay = status2.getDelay(TimeUnit.SECONDS);
            throw new Exception("Should not be able to obtain delay from status for repeating task, because the next execution time from which it is computed can change. " + delay);
        } catch (IllegalStateException x) {
        }

        Date nextExecutionTime = status2.getNextExecutionTime();
        long delay = TimeUnit.MILLISECONDS.toHours(nextExecutionTime.getTime() - System.currentTimeMillis());
        if (delay > 1)
            throw new Exception("Unexpected delay " + delay + " for " + status2);
    }

    /**
     * Schedule a Callable to a persistent executor that is configured not to run tasks.
     */
    public void testScheduleCallable(PrintWriter out) throws Exception {
        scheduler.schedule((Callable<String>) new NamedTask("testScheduleCallable"), 2, TimeUnit.NANOSECONDS);
    }

    /**
     * Schedule a Callable with a Trigger to a persistent executor that is configured not to run tasks.
     */
    public void testScheduleCallableWithTrigger(PrintWriter out) throws Exception {
        scheduler.schedule((Callable<String>) new NamedTask("testScheduleCallableWithTrigger"), new ImmediateTrigger());
    }

    /**
     * Schedule a Runnable to a persistent executor that is configured not to run tasks.
     */
    public void testScheduleRunnable(PrintWriter out) throws Exception {
        scheduler.schedule((Runnable) new NamedTask("testScheduleRunnable"), 3, TimeUnit.NANOSECONDS);
    }

    /**
     * Schedule a Runnable with a Trigger to a persistent executor that is configured not to run tasks.
     */
    public void testScheduleRunnableWithTrigger(PrintWriter out) throws Exception {
        scheduler.schedule((Runnable) new NamedTask("testScheduleRunnableWithTrigger"), new ImmediateTrigger());

        int numCanceled = scheduler.cancel("testScheduleRunnableWithTrig%", '\\', TaskState.ANY, true);
        if (numCanceled != 1)
            throw new Exception("Should have canceled exactly 1 task, instead " + numCanceled);
    }

    /**
     * Schedule a task with fixed delay to a persistent executor that is configured not to run tasks.
     */
    public void testScheduleWithFixedDelay(PrintWriter out) throws Exception {
        scheduler.scheduleWithFixedDelay(new NamedTask("testScheduleWithFixedDelay"), 0, 4, TimeUnit.DAYS);

        int numCanceled = scheduler.cancel("testScheduleWithFixedDelay", '\\', TaskState.UNATTEMPTED, true);
        if (numCanceled != 1)
            throw new Exception("Should have canceled exactly 1 task, instead " + numCanceled);
    }

    /**
     * Submit a Callable to a persistent executor that is configured not to run tasks.
     */
    public void testSubmitCallable(PrintWriter out) throws Exception {
        scheduler.submit((Callable<String>) new NamedTask("testSubmitCallable"));
    }

    /**
     * Submit a Runnable to a persistent executor that is configured not to run tasks.
     */
    public void testSubmitRunnable(PrintWriter out) throws Exception {
        scheduler.submit((Runnable) new NamedTask("testSubmitRunnable"));
    }

    /**
     * Submit a Runnable to a persistent executor that is configured not to run tasks.
     */
    public void testSubmitRunnableWithResult(PrintWriter out) throws Exception {
        scheduler.submit(new NamedTask("testSubmitRunnableWithResult"), "RunnableResult");
    }

    /**
     * Verify that no tasks have executed.
     */
    public void testNoTasksExecuted(PrintWriter out) throws Exception {
        // Allow some time for unexpected task executions
        Thread.sleep(5000);

        if (!NamedTask.namesOfExecutedTasks.isEmpty())
            throw new Exception("The following tasks should not have executed: " + NamedTask.namesOfExecutedTasks);
    }
}
