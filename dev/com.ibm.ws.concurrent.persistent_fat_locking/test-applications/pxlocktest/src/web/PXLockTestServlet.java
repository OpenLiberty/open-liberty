/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedTask;
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
public class PXLockTestServlet extends HttpServlet {
    private static final long serialVersionUID = -6224708961115493125L;

    private static final boolean CREATE_PROP = true, GET_STATUS = true;

    /**
     * Message written to servlet to indicate that is has been successfully invoked.
     */
    private static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Resource(name = "java:module/env/concurrent/myPersistentExecutorRef", lookup = "concurrent/myPersistentExecutor")
    private PersistentExecutor executor;

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
     * Schedule a task to run at a fixed rate.
     */
    public void testScheduleAtFixedRate(PrintWriter out) throws Exception {
        Runnable task = new SleeperTask("FixedRate", null, 500, false, false);
        executor.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Schedule a task to run at a fixed rate where the task creates a property each time it runs.
     */
    public void testScheduleAtFixedRateCreateProp(PrintWriter out) throws Exception {
        Runnable task = new SleeperTask("FixedRateCreate", null, 500, false, CREATE_PROP);
        executor.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Schedule a task to run at a fixed rate where the task runs with the executor's transaction suspended.
     */
    public void testScheduleAtFixedRateSuspend(PrintWriter out) throws Exception {
        Runnable task = new SleeperTask("FixedRateSuspend", ManagedTask.SUSPEND, 500, false, false);
        executor.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Schedule a task to run at a fixed rate where the task runs with the executor's transaction suspended,
     * and get the task status each time it runs.
     */
    public void testScheduleAtFixedRateSuspendGetStatus(PrintWriter out) throws Exception {
        Runnable task = new SleeperTask("FixedRateSuspendGet", ManagedTask.SUSPEND, 500, GET_STATUS, false);
        executor.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Schedule a Callable to run according to a trigger.
     */
    public void testScheduleCallableWithTrigger(PrintWriter out) throws Exception {
        Callable<Integer> task = new SleeperTask("TriggerCallable", null, 1000, false, false);
        executor.schedule(task, OneSecondTrigger.INSTANCE);
    }

    /**
     * Schedule a Callable to run according to a trigger and get the task status when it runs.
     */
    public void testScheduleCallableWithTriggerGetStatus(PrintWriter out) throws Exception {
        Callable<Integer> task = new SleeperTask("TriggerCallableGet", null, 1000, GET_STATUS, false);
        executor.schedule(task, OneSecondTrigger.INSTANCE);
    }

    /**
     * Schedule a Callable to run according to a trigger, with the executor's transaction suspended,
     * and get the task status each time it runs.
     */
    public void testScheduleCallableWithTriggerSuspendGetStatus(PrintWriter out) throws Exception {
        Callable<Integer> task = new SleeperTask("TriggerCallableSuspendGet", ManagedTask.SUSPEND, 1000, GET_STATUS, false);
        executor.schedule(task, OneSecondTrigger.INSTANCE);
    }

    /**
     * Schedule a Callable to run according to a trigger, with the executor's transaction suspended,
     * and get the task status and create a property each time it runs.
     */
    public void testScheduleCallableWithTriggerSuspendGetStatusCreateProp(PrintWriter out) throws Exception {
        Callable<Integer> task = new SleeperTask("TriggerCallableSuspendGetCreate", ManagedTask.SUSPEND, 1000, GET_STATUS, CREATE_PROP);
        executor.schedule(task, OneSecondTrigger.INSTANCE);
    }

    /**
     * Schedule a Runnable to run according to a trigger.
     */
    public void testScheduleRunnableWithTrigger(PrintWriter out) throws Exception {
        Runnable task = new SleeperTask("TriggerRunnable", null, 1000, false, false);
        executor.schedule(task, OneSecondTrigger.INSTANCE);
    }

    /**
     * Schedule a Runnable to run according to a trigger and create a property each time it runs.
     */
    public void testScheduleRunnableWithTriggerCreateProp(PrintWriter out) throws Exception {
        Runnable task = new SleeperTask("TriggerRunnableCreate", null, 1000, false, CREATE_PROP);
        executor.schedule(task, OneSecondTrigger.INSTANCE);
    }

    /**
     * Ensure that tasks are running multiple times. Then cancel them.
     */
    public void verifyTasksRunMultipleTimes(PrintWriter out) throws Exception {
        long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(5);
        long POLL_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);

        // Wait for the following properties to be written to the database
        Map<String, String> expectedValues = new HashMap<String, String>();
        expectedValues.put(SleeperTask.class.getSimpleName() + "-FixedRateCreate-1", "previous:null");
        expectedValues.put(SleeperTask.class.getSimpleName() + "-FixedRateCreate-2", "previous:null");
        expectedValues.put(SleeperTask.class.getSimpleName() + "-FixedRateCreate-3", "previous:null");
        expectedValues.put(SleeperTask.class.getSimpleName() + "-TriggerCallableSuspendGetCreate-1", "previous:null");
        expectedValues.put(SleeperTask.class.getSimpleName() + "-TriggerCallableSuspendGetCreate-2", "previous:1");
        expectedValues.put(SleeperTask.class.getSimpleName() + "-TriggerCallableSuspendGetCreate-3", "previous:2");
        expectedValues.put(SleeperTask.class.getSimpleName() + "-TriggerRunnableCreate-1", "previous:null");
        expectedValues.put(SleeperTask.class.getSimpleName() + "-TriggerRunnableCreate-2", "previous:null");
        expectedValues.put(SleeperTask.class.getSimpleName() + "-TriggerRunnableCreate-3", "previous:null");
        for (long start = System.nanoTime(); !expectedValues.isEmpty() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL_MS))
            for (Iterator<Map.Entry<String, String>> it = expectedValues.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, String> entry = it.next();
                String value = executor.getProperty(entry.getKey());
                if (entry.getValue().equals(value))
                    it.remove();
                else if (value != null)
                    throw new Exception("Expecting value of " + entry.getValue() + " for " + entry.getKey() + ". Instead: " + value);
            }

        if (!expectedValues.isEmpty())
            throw new Exception("Entries did not appear in database within a reasonable amount of time: " + expectedValues);

        System.out.println("--> findTaskStatus");
        List<TaskStatus<?>> statusList = executor.findTaskStatus(SleeperTask.class.getSimpleName() + "-%", '\\', TaskState.ANY, true, null, null);
        System.out.println("<-- findTaskStatus");
        if (statusList.size() != 10)
            throw new Exception("Expecting exactly 10 task status entries. Instead " + statusList);

        // Wait for results greater than or equal to the following
        Map<String, Integer> expectedMinResults = new HashMap<String, Integer>();
        expectedMinResults.put(SleeperTask.class.getSimpleName() + "-TriggerCallable", 8);
        expectedMinResults.put(SleeperTask.class.getSimpleName() + "-TriggerCallableGet", 8);
        expectedMinResults.put(SleeperTask.class.getSimpleName() + "-TriggerCallableSuspendGet", 8);
        expectedMinResults.put(SleeperTask.class.getSimpleName() + "-TriggerCallableSuspendGetCreate", 8);
        for (long start = System.nanoTime(); !expectedMinResults.isEmpty() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL_MS)) {
            for (TaskStatus<?> status : statusList)
                if (status.hasResult()) {
                    String taskName = status.getTaskName();
                    Object result = status.getResult();
                    Integer expectedResult = expectedMinResults.get(taskName);
                    if (expectedResult != null && expectedResult.compareTo((Integer) result) <= 0) {
                        System.out.println("Result for " + status + " is " + result);
                        expectedMinResults.remove(taskName);
                    }
                }

            if (!expectedMinResults.isEmpty()) {
                System.out.println("--> findTaskStatus");
                statusList = executor.findTaskStatus(SleeperTask.class.getSimpleName() + "-TriggerCallable%", '\\', TaskState.ANY, true, null, null);
                System.out.println("<-- findTaskStatus");
            }
        }

        System.out.println("--> findTaskStatus");
        statusList = executor.findTaskStatus(SleeperTask.class.getSimpleName() + "-%", '\\', TaskState.ANY, true, null, null);
        System.out.println("<-- findTaskStatus");
        if (statusList.size() != 10)
            throw new Exception("Expecting exactly 10 task status entries. Instead " + statusList);

        for (TaskStatus<?> status : statusList) {
            System.out.println("Canceling task " + status.getTaskName());
            if (!status.cancel(false))
                throw new Exception("Unable to cancel task " + status);
        }

    }
}
