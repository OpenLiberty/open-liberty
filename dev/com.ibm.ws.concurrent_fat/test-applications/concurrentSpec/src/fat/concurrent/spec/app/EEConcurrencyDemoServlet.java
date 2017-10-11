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
package fat.concurrent.spec.app;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedExecutors;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.Trigger;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * To run the demo:
 * wlp\bin>server run concurrent.spec.fat
 * observe which port number is being used, for example 8010,
 * then browse to:
 * localhost:8010/concurrentSpec/demo?interval=10000&maxExecutionsA=7&maxExecutionsB=4
 */
@WebServlet("/demo")
public class EEConcurrencyDemoServlet extends HttpServlet {
    private static final long serialVersionUID = 7412850662837588934L;

    @Resource
    private ManagedScheduledExecutorService managedScheduledExecutor;

    @Resource(lookup = "concurrent/threadFactory-jee-metadata-context")
    private ThreadFactory managedThreadFactory;

    private static ConcurrentLinkedQueue<Future<?>> futures = new ConcurrentLinkedQueue<Future<?>>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            PrintWriter out = response.getWriter();
            out.println("<h3>EE Concurrency Demo</h3>");

            String test = request.getParameter("testMethod");
            if ("testContextService".equals(test))
                testContextService(out);
            else if ("testOneTimeScheduledTask".equals(test))
                testOneTimeScheduledTask(out, Long.parseLong(request.getParameter("interval")));
            else if ("testRepeatingTask".equals(test))
                testRepeatingTask(out,
                                  Long.parseLong(request.getParameter("interval")),
                                  Boolean.parseBoolean(request.getParameter("isManagedTask")));
            else if ("testManagedThreadFactory".equals(test))
                testManagedThreadFactory(out, Long.parseLong(request.getParameter("interval")));
            else if ("testCancel".equals(test))
                testCancel(out);
            else if ("testRepeatingTaskWithTrigger".equals(test))
                testRepeatingTaskWithTrigger(out,
                                             Long.parseLong(request.getParameter("interval")),
                                             Integer.parseInt(request.getParameter("maxExecutions")));
            else
                out.println("missing or unrecognized test name parameter: " + test);

            out.println("<!--COMPLETED SUCCESSFULLY-->");
        } catch (Exception x) {
            throw new ServletException(x);
        }
    }

    // localhost:8010/concurrentbvt/demo?test=testContextService
    void testContextService(final PrintWriter out) throws Exception {
        ContextService contextService = (ContextService) new InitialContext().lookup("java:comp/DefaultContextService");

        Runnable barrierAction = new Runnable() {
            @Override
            public void run() {
                try {
                    Object value = new InitialContext().lookup("java:comp/env/entryA");
                    out.println("CyclicBarrier action looked up this value: " + value);
                } catch (Exception x) {
                    x.printStackTrace(System.out);
                }
            }
        };

        barrierAction = contextService.createContextualProxy(barrierAction, Runnable.class);

        final CyclicBarrier barrier = new CyclicBarrier(2, barrierAction);

        new Thread() {
            @Override
            public void run() {
                try {
                    barrier.await();
                } catch (Exception x) {
                    x.printStackTrace(System.out);
                }
            }
        }.start();

        new Thread() {
            @Override
            public void run() {
                try {
                    barrier.await();
                } catch (Exception x) {
                    x.printStackTrace(System.out);
                }
            }
        }.start();

        Thread.sleep(1000);
    }

    // localhost:8010/concurrentbvt/demo?test=testOneTimeScheduledTask&interval=8000
    void testOneTimeScheduledTask(final PrintWriter out, final long interval) throws Exception {

        ScheduledExecutorService scheduledExecutor = (ScheduledExecutorService) new InitialContext().lookup("java:comp/DefaultManagedScheduledExecutorService");

        ScheduledFuture<?> future = scheduledExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    Integer value = (Integer) new InitialContext().lookup("java:comp/env/entryA");
                    System.out.println("One-time task looked up this value: " + value);
                } catch (Exception x) {
                    x.printStackTrace(System.out);
                }
            }
        }, interval, TimeUnit.MILLISECONDS);

        out.println("one-time task has been scheduled for " + future.getDelay(TimeUnit.MILLISECONDS) + " ms from now");
    }

    // localhost:8010/concurrentbvt/demo?test=testRepeatingTask&interval=5000&isManagedTask=false
    // localhost:8010/concurrentbvt/demo?test=testCancel
    // localhost:8010/concurrentbvt/demo?test=testRepeatingTask&interval=5000&isManagedTask=true
    // localhost:8010/concurrentbvt/demo?test=testCancel
    void testRepeatingTask(final PrintWriter out, final long interval, boolean isManagedTask) {
        Runnable task = new Runnable() {
            private final AtomicInteger numExecutions = new AtomicInteger();

            @Override
            public void run() {
                try {
                    Object value = new InitialContext().lookup("java:comp/env/entryA");
                    System.out.println("Repeating task execution#" + numExecutions.incrementAndGet() + " looked up this value: " + value);
                } catch (Exception x) {
                    x.printStackTrace(System.out);
                }
            }
        };

        if (isManagedTask) {
            ManagedTaskListener listener = new ManagedTaskListener() {
                @Override
                public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
                    String taskName = ((ManagedTask) task).getExecutionProperties().get(ManagedTask.IDENTITY_NAME);
                    System.out.println(taskName + " submitted");
                }

                @Override
                public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
                    String taskName = ((ManagedTask) task).getExecutionProperties().get(ManagedTask.IDENTITY_NAME);
                    System.out.println(taskName + " starting");
                }

                @Override
                public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable failure) {
                    String taskName = ((ManagedTask) task).getExecutionProperties().get(ManagedTask.IDENTITY_NAME);
                    System.out.println(taskName + " aborted due to " + failure);
                }

                @Override
                public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable failure) {
                    String taskName = ((ManagedTask) task).getExecutionProperties().get(ManagedTask.IDENTITY_NAME);
                    System.out.println(taskName + " done " + (failure == null ? "" : failure));
                }
            };

            Map<String, String> execProps = Collections.singletonMap(ManagedTask.IDENTITY_NAME, "myRepeatingTask");
            task = ManagedExecutors.managedTask(task, execProps, listener);
        }

        final ScheduledFuture<?> future = managedScheduledExecutor.scheduleAtFixedRate(task, 0, interval, TimeUnit.MILLISECONDS);
        futures.add(future);

        out.println("Repeating task has been scheduled");
    }

    // localhost:8010/concurrentbvt/demo?test=testManagedThreadFactory&interval=8000
    void testManagedThreadFactory(final PrintWriter out, final long interval) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, managedThreadFactory);

        Callable<Integer> task = new Callable<Integer>() {
            private final AtomicInteger numExecutions = new AtomicInteger();

            @Override
            public Integer call() throws Exception {
                int numExecution = numExecutions.incrementAndGet();
                Object value = new InitialContext().lookup("java:comp/env/entryA");
                System.out.println("Task execution#" + numExecution + " looked up this value: " + value);
                return numExecution;
            }
        };

        ScheduledFuture<Integer> future1 = executor.schedule(task, interval, TimeUnit.MILLISECONDS);
        ScheduledFuture<Integer> future2 = executor.schedule(task, interval * 2, TimeUnit.MILLISECONDS);

        out.println("Two tasks have been scheduled to a Java SE executor that uses a managed thread factory.");
        out.println("<br>task 1 for " + future1.getDelay(TimeUnit.MILLISECONDS) + " ms from now.");
        out.println("<br>task 2 for " + future2.getDelay(TimeUnit.MILLISECONDS) + " ms from now.");
    }

    // localhost:8010/concurrentbvt/demo?test=testRepeatingTaskWithTrigger&interval=5000&maxExecutions=5
    void testRepeatingTaskWithTrigger(final PrintWriter out, final long interval, final int maxExecutions) {
        Callable<Integer> task = new Callable<Integer>() {
            private final AtomicInteger numExecutions = new AtomicInteger();

            @Override
            public Integer call() throws Exception {
                Object value = new InitialContext().lookup("java:comp/env/entryA");
                System.out.println("Repeating task looked up this value: " + value);
                return numExecutions.incrementAndGet();
            }
        };

        Trigger trigger = new Trigger() {
            @Override
            public Date getNextRunTime(LastExecution execution, Date taskScheduledTime) {
                if (execution == null) // first time
                    return taskScheduledTime;
                else if ((Integer) execution.getResult() >= maxExecutions) // stop when we reach maxExecutions
                    return null;
                else
                    return new Date(execution.getRunStart().getTime() + interval);
            }

            @Override
            public boolean skipRun(LastExecution execution, Date scheduledRunTime) {
                // skip if attempting to run more than 5 seconds late
                return System.currentTimeMillis() > scheduledRunTime.getTime() + 5000;
            }
        };

        ScheduledFuture<Integer> future = managedScheduledExecutor.schedule(task, trigger);

        out.println("Task has been scheduled using a Trigger. Next execution will be " + future.getDelay(TimeUnit.MILLISECONDS) + " ms from now.");
    }

    void testCancel(final PrintWriter out) {
        for (Future<?> future = futures.poll(); future != null; future = futures.poll()) {
            future.cancel(false);
            out.println("Task has been canceled<br>");
        }
    }
}