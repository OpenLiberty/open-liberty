/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.concurrent.spec.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.enterprise.concurrent.AbortedException;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.LastExecution;
import javax.enterprise.concurrent.ManageableThread;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedExecutors;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedTask;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.concurrent.SkippedException;
import javax.enterprise.concurrent.Trigger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.wsspi.uow.UOWManager;
import com.ibm.wsspi.uow.UOWManagerFactory;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import fat.concurrent.spec.app.TaskListener.CancelType;

@SuppressWarnings("serial")
@WebServlet("/")
public class EEConcurrencyTestServlet extends FATServlet {
    private static final InheritableThreadLocal<String> threadInfo = new InheritableThreadLocal<String>() {
        @Override
        public String childValue(String parentValue) {
            return "Child of " + parentValue;
        }

        @Override
        public String initialValue() {
            return Thread.currentThread().getName() + "(" + Thread.currentThread().getId() + ")";
        }
    };

    // Using this to try to speed up the bucket
    private static final ScheduledExecutorService daemon = Executors.newSingleThreadScheduledExecutor();

    @Resource
    private UserTransaction tran;

    @Resource
    private ContextService contextSvcDefault;

    @Resource
    private ExecutorService xsvcDefault;

    @Resource(lookup = "java:comp/DefaultManagedExecutorService")
    private ExecutorService xsvcDefaultLookup;

    @Resource
    private ScheduledExecutorService schedxsvcDefault;

    @Resource(lookup = "java:comp/DefaultManagedScheduledExecutorService")
    private ScheduledExecutorService schedxsvcDefaultLookup;

    @Resource(lookup = "java:comp/DefaultManagedScheduledExecutorService")
    private ManagedScheduledExecutorService mschedxsvcDefaultLookup;

    @Resource
    private ThreadFactory threadFactoryDefault;

    @Resource(lookup = "java:comp/DefaultManagedThreadFactory")
    private ThreadFactory threadFactoryDefaultLookup;

    @Resource
    private ManagedThreadFactory mthreadFactoryDefault;

    @Resource(lookup = "java:comp/DefaultManagedThreadFactory")
    private ManagedThreadFactory mthreadFactoryDefaultLookup;

    @Resource(name = "java:module/env/schedxsvc-cl-ref", lookup = "concurrent/schedxsvc-classloader-context")
    private ExecutorService xsvcClassloaderContext;

    @Resource(lookup = "concurrent/schedxsvc-classloader-context")
    private ManagedExecutorService mxsvcClassloaderContext;

    @Resource(lookup = "concurrent/schedxsvc-classloader-context")
    private ManagedScheduledExecutorService mschedxsvcClassloaderContext;

    @Resource(lookup = "concurrent/schedxsvc-classloader-context")
    private ScheduledExecutorService schedxsvcClassloaderContext;

    @Resource(lookup = "concurrent/xsvc-empty-context")
    private ManagedExecutorService mxsvcNoContext;

    @Resource(lookup = "concurrent/xsvc-empty-context")
    private ExecutorService xsvcNoContext;

    @Resource(lookup = "concurrent/threadFactory-jee-metadata-context")
    private ThreadFactory threadFactoryJEEMetadataContext;

    // Interval (in milliseconds) that tests should use for polling
    static final long POLL_INTERVAL = 100;

    // Interval (in milliseconds) up to which tests should wait for a single task to run
    static final long TIMEOUT = TimeUnit.MINUTES.toMillis(2);

    /**
     * Schedule/submit a task that is both a Callable and a Runnable.
     * Ensure that if scheduled/submitted as a Callable then call is invoked and not run,
     * and if scheduled/submitted as a Runnable then run is invoked and not call.
     */
    @Test
    public void testCallableAndRunnableTask() throws Exception {
        FailingTask task;
        Object result;

        task = new FailingTask(FailingTask.FAIL_IF_CALL_INVOKED);
        result = mxsvcNoContext.submit((Runnable) task).get();
        if (result != null)
            throw new Exception("Unexpected result for submit(runnable): " + result);

        task = new FailingTask(FailingTask.FAIL_IF_RUN_INVOKED);
        result = mxsvcNoContext.submit((Callable<Integer>) task).get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected result for submit(callable): " + result);

        task = new FailingTask(FailingTask.FAIL_IF_CALL_INVOKED);
        result = mschedxsvcClassloaderContext.schedule((Runnable) task, 39, TimeUnit.NANOSECONDS).get();
        if (result != null)
            throw new Exception("Unexpected result for schedule(runnable): " + result);

        task = new FailingTask(FailingTask.FAIL_IF_RUN_INVOKED);
        result = mschedxsvcClassloaderContext.schedule((Callable<Integer>) task, 40, TimeUnit.MICROSECONDS).get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected result for schedule(callable): " + result);
    }

    /**
     * Schedule a task, wait for it to start, and then cancel it.
     */
    @Test
    public void testCancelOneTimeTask() throws Exception {
        AtomicInteger numStarted = new AtomicInteger();
        AtomicInteger numInterruptions = new AtomicInteger();
        Runnable slowTask = new SlowTask(numStarted, numInterruptions, TIMEOUT * 2);
        ScheduledFuture<?> slowFuture = schedxsvcClassloaderContext.schedule(slowTask, 0, TimeUnit.HOURS);

        try {
            // poll to make sure it starts running
            for (long begin = System.currentTimeMillis(), time = begin; numStarted.get() == 0 && time < begin + TIMEOUT; time = System.currentTimeMillis())
                Thread.sleep(POLL_INTERVAL);

            int slowTaskStartedCount = numStarted.get();
            if (slowTaskStartedCount != 1)
                throw new Exception("slowTask should have started once, not " + slowTaskStartedCount);

            boolean canceled = slowFuture.cancel(true);
            if (!canceled)
                throw new Exception("Unable to cancel slow task that should have been running at this time.");

            try {
                Object result = slowFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
                throw new Exception("Canceled running task should not return result: " + result);
            } catch (CancellationException x) {
            } // pass

            // poll for the running task to be interrupted
            for (long begin = System.currentTimeMillis(), time = begin; numInterruptions.get() == 0 && time < begin + TIMEOUT; time = System.currentTimeMillis())
                Thread.sleep(POLL_INTERVAL);
            int slowTaskInterruptedCount = numInterruptions.get();
            if (slowTaskInterruptedCount != 1)
                throw new Exception("slowTask should have been interrupted once, not " + slowTaskInterruptedCount);
        } finally {
            slowFuture.cancel(true);
        }
    }

    /**
     * Have a scheduled task cancel itself while running.
     */
    @Test
    public void testCancelScheduledTaskWhileRunning() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        LinkedBlockingQueue<Future<?>> cancelationQueue = new LinkedBlockingQueue<Future<?>>();
        SlowTask selfCancelingTask = new SlowTask();
        selfCancelingTask.cancelationQueueRef.set(cancelationQueue);
        selfCancelingTask.interruptIfCanceled.set(true);
        Runnable managedRunnable = ManagedExecutors.managedTask((Runnable) selfCancelingTask, listener);

        ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleAtFixedRate(managedRunnable, 0, 29, TimeUnit.NANOSECONDS);
        cancelationQueue.add(future);

        // scheduleAtFixedRate: taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Unexpected delay: " + event);

        // scheduleAtFixedRate: taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Unexpected delay: " + event);

        // scheduleAtFixedRate: taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // scheduleAtFixedRate: taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Unexpected delay: " + event);
        if (!(event.exception instanceof RuntimeException) || !(event.exception.getCause() instanceof InterruptedException))
            throw new Exception("scheduleAtFixedRate/taskDone#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("scheduleAtFixedRate/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("scheduleAtFixedRate: Task should be done. " + event.future);

        if (!event.future.isCancelled())
            throw new Exception("scheduleAtFixedRate: Task should be canceled. " + event.future);

        try {
            Object result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Scheduled task ought to be canceled. Instead: " + result);
        } catch (CancellationException x) {
        } // pass

        if (!listener.events.isEmpty())
            throw new Exception("scheduleAtFixedRate: Unexpected events: " + listener.events);
    }

    /**
     * Cancels a task where the cancel is requested from the submitting thread.
     */
    @Test
    public void testCancelSlowTask() throws Exception {

        TaskListener listener = new TaskListener(true);
        SlowTask task = new SlowTask();
        Callable<Long> managedTask = ManagedExecutors.managedTask((Callable<Long>) task, listener);

        Future<Long> future = mxsvcNoContext.submit(managedTask);
        boolean canceled = future.cancel(true);

        if (!canceled)
            throw new Exception("Failed to cancel task: " + future);

        // taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("submit(callable): Unexpected first event: " + event);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(callable)/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedTask)
            throw new Exception("submit(callable)/taskSubmitted: Wrong task: " + event);
        if (event.future != future)
            throw new Exception("submit(callable)/taskSubmitted: Incorrect future for " + event + ". Actual Future: " + future);

        // Because future.cancel happens asynchronously, various sequences of events are valid:
        // A1) taskSubmitted, taskStarting, taskDone (success; task completes before cancel)
        // A2) taskSubmitted, taskStarting, taskDone (fail; task interrupted by cancel)
        // B) taskSubmitted, taskStarting, taskAborted, taskDone
        // C) taskSubmitted, taskAborted, taskDone
        int count = 1;

        // taskStarting?
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (TaskEvent.Type.taskStarting.equals(event.type)) {
            count++;
            if (event.execSvc != mxsvcNoContext)
                throw new Exception("submit(callable)/taskStarting: Wrong executor: " + event);
            if (event.task != managedTask)
                throw new Exception("submit(callable)/taskStarting: Wrong task: " + event);
            if (event.future != future)
                throw new Exception("submit(callable)/taskStarting: Incorrect future for " + event + ". Actual Future: " + future);
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // taskAborted? (due to cancellation)
        if (TaskEvent.Type.taskAborted.equals(event.type)) {
            count++;
            if (!(event.exception instanceof CancellationException))
                throw new Exception("submit(callable)/taskAborted: Wrong exception on " + event, event.exception);
            if (event.execSvc != mxsvcNoContext)
                throw new Exception("submit(callable)/taskAborted: Wrong executor: " + event);
            if (event.task != managedTask)
                throw new Exception("submit(callable)/taskAborted: Wrong task: " + event);
            if (!future.equals(event.future))
                throw new Exception("submit(callable)/taskAborted: Future does not match " + future + ". Instead " + event);
            if (!(event.failureFromFutureGet instanceof CancellationException))
                throw new Exception("submit(callable)/taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        if (count <= 1)
            throw new Exception("Did not see taskStarting or taskAborted. Instead: " + event);

        // taskDone
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("submit(callable): Unexpected last event: " + event);
        if (event.exception != null && !(event.exception instanceof InterruptedException))
            throw new Exception("submit(callable)/taskDone: Wrong exception on " + event, event.exception);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(callable)/taskDone: Wrong executor: " + event);
        if (event.task != managedTask)
            throw new Exception("submit(callable)/taskDone: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("submit(callable)/taskDone: Future does not match " + future + ". Instead " + event);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("submit(callable)/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!listener.events.isEmpty())
            throw new Exception("submit(callable): Unexpected events: " + listener.events);
    }

    /**
     * Cancels a started task that was scheduled via scheduledExecSvc.schedule(...).
     */
    @Test
    public void testCancelStartedScheduledTask() throws Exception {
        TaskAndListener task = new TaskAndListener();
        task.sleep = 5000;

        Future<?> future = schedxsvcClassloaderContext.schedule(task, 46, TimeUnit.NANOSECONDS);

        String event = task.events.poll();
        if (!"SUBMITTED".equals(event))
            throw new Exception("Missing taskSubmitted. " + event);

        if (!task.startedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS))
            throw new Exception("Task didn't start");

        event = task.events.poll();
        if (!"STARTING".equals(event))
            throw new Exception("Missing taskStarting. " + event);

        boolean canceled = future.cancel(true);
        if (!canceled)
            throw new Exception("Unable to cancel task");

        event = task.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!"ABORTED: canceled=true exception=java.util.concurrent.CancellationException".equals(event))
            throw new Exception("Missing or incorrect taskAborted. " + event);

        event = task.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!"DONE: canceled=true exception=null".equals(event))
            throw new Exception("Missing or incorrect taskDone. " + event);
    }

    /**
     * Cancels a started task that was submitted via execSvc.submit(...)
     */
    @Test
    public void testCancelStartedSubmittedTask() throws Exception {
        TaskAndListener task = new TaskAndListener();
        task.sleep = 5000;

        Future<?> future = xsvcNoContext.submit(task);

        String event = task.events.poll();
        if (!"SUBMITTED".equals(event))
            throw new Exception("Missing taskSubmitted. " + event);

        if (!task.startedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS))
            throw new Exception("Task didn't start");

        event = task.events.poll();
        if (!"STARTING".equals(event))
            throw new Exception("Missing taskStarting. " + event);

        boolean canceled = future.cancel(true);
        if (!canceled)
            throw new Exception("Unable to cancel task");

        event = task.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!"ABORTED: canceled=true exception=java.util.concurrent.CancellationException".equals(event))
            throw new Exception("Missing or incorrect taskAborted. " + event);

        event = task.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!"DONE: canceled=true exception=null".equals(event))
            throw new Exception("Missing or incorrect taskDone. " + event);
    }

    /**
     * Compare one of our ScheduledFuture impls with java.util.concurrent.ScheduledFutureTask.
     */
    @Test
    public void testCompareToScheduledFutureTask() throws Exception {
        ScheduledFuture<?> future5 = schedxsvcDefaultLookup.scheduleWithFixedDelay(new CounterTask(), 5, 1, TimeUnit.MINUTES);
        try {
            ScheduledFuture<?> future4 = daemon.schedule((Runnable) new CounterTask(), 4, TimeUnit.MINUTES);
            try {
                int compare = future5.compareTo(future4);
                if (compare <= 0)
                    throw new Exception("Our scheduled future impl should compare greater than the ScheduledFutureTask. Instead: " + compare);

                compare = future4.compareTo(future5);
                if (compare >= 0)
                    throw new Exception("The ScheduledFutureTask should compare less than our scheduled future impl. Insated: " + compare);
            } finally {
                future4.cancel(false);
            }

            ScheduledFuture<?> future6 = daemon.schedule((Runnable) new CounterTask(), 6, TimeUnit.MINUTES);
            try {
                int compare = future5.compareTo(future6);
                if (compare >= 0)
                    throw new Exception("Our scheduled future impl should compare less than the ScheduledFutureTask. Instead: " + compare);

                compare = future6.compareTo(future5);
                if (compare <= 0)
                    throw new Exception("The ScheduledFutureTask should compare greater than our scheduled future impl. Insated: " + compare);
            } finally {
                future6.cancel(false);
            }

            int compare = future5.compareTo(future5);
            if (compare != 0)
                throw new Exception("Unexpected compare of future to self: " + compare);
        } finally {
            future5.cancel(false);
        }

        int compare = future5.compareTo(future5);
        if (compare != 0)
            throw new Exception("Unexpected compare of future to self after cancel: " + compare);
    }

    /**
     * Use ContextService to contextualize ManagedTaskListener methods
     */
    @Test
    public void testContextualizeManagedTaskListener() throws Exception {
        final LinkedBlockingQueue<Object> lookupResult_taskSubmitted = new LinkedBlockingQueue<Object>();
        final LinkedBlockingQueue<Object> lookupResult_taskStarting = new LinkedBlockingQueue<Object>();
        final LinkedBlockingQueue<Object> lookupResult_taskDone = new LinkedBlockingQueue<Object>();

        ManagedTaskListener listener = new ManagedTaskListener() {
            @Override
            public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable x) {
            }

            @Override
            public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable x) {
                try {
                    lookupResult_taskDone.add(new InitialContext().lookup("java:comp/env/entry1"));
                } catch (Throwable t) {
                    lookupResult_taskDone.add(t);
                }
            }

            @Override
            public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
                try {
                    lookupResult_taskStarting.add(new InitialContext().lookup("java:comp/env/entry1"));
                } catch (Throwable t) {
                    lookupResult_taskStarting.add(t);
                }
            }

            @Override
            public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
                try {
                    lookupResult_taskSubmitted.add(new InitialContext().lookup("java:comp/env/entry1"));
                } catch (Throwable t) {
                    lookupResult_taskSubmitted.add(t);
                }
            }
        };

        ContextService defaultContextService = (ContextService) new InitialContext().lookup("java:comp/DefaultContextService");
        ManagedTaskListener contextualListener = defaultContextService.createContextualProxy(listener, ManagedTaskListener.class);

        Callable<Integer> managedTask = ManagedExecutors.managedTask((Callable<Integer>) new CounterTask(), contextualListener);

        Future<Integer> future = mxsvcNoContext.submit(managedTask);
        Integer result = future.get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected result of task: " + result);

        Object lookupResult = lookupResult_taskSubmitted.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (lookupResult instanceof Throwable)
            throw new Exception("failure during taskSubmitted", (Throwable) lookupResult);
        else if (!"value1".equals(lookupResult))
            throw new Exception("unexpected lookup result during taskSubmitted: " + lookupResult);

        lookupResult = lookupResult_taskStarting.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (lookupResult instanceof Throwable)
            throw new Exception("failure during taskStarting", (Throwable) lookupResult);
        else if (!"value1".equals(lookupResult))
            throw new Exception("unexpected lookup result during taskStarting: " + lookupResult);

        lookupResult = lookupResult_taskDone.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (lookupResult instanceof Throwable)
            throw new Exception("failure during taskDone", (Throwable) lookupResult);
        else if (!"value1".equals(lookupResult))
            throw new Exception("unexpected lookup result during taskDone: " + lookupResult);
    }

    /**
     * Use ContextService to contextualize Trigger methods
     */
    @Test
    public void testContextualizeTrigger() throws Exception {
        final LinkedBlockingQueue<Object> lookupResult_getNextRunTime = new LinkedBlockingQueue<Object>();
        final LinkedBlockingQueue<Object> lookupResult_skipRun = new LinkedBlockingQueue<Object>();

        Trigger trigger = new Trigger() {
            @Override
            public Date getNextRunTime(LastExecution lastExecution, Date taskScheduledTime) {
                try {
                    lookupResult_getNextRunTime.add(new InitialContext().lookup("java:comp/env/entry1"));
                } catch (Throwable t) {
                    lookupResult_getNextRunTime.add(t);
                }
                return lastExecution == null ? new Date() : null; // run once immediately, and never again
            }

            @Override
            public boolean skipRun(LastExecution arg0, Date arg1) {
                try {
                    lookupResult_skipRun.add(new InitialContext().lookup("java:comp/env/entry1"));
                } catch (Throwable t) {
                    lookupResult_skipRun.add(t);
                }
                return false;
            }
        };

        Trigger contextualTrigger = contextSvcDefault.createContextualProxy(trigger, Trigger.class);

        ManagedScheduledExecutorService executor = (ManagedScheduledExecutorService) new InitialContext().lookup("concurrent/schedxsvc-classloader-context");
        Future<Integer> future = executor.schedule((Callable<Integer>) new CounterTask(), contextualTrigger);
        Integer result = future.get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected result of task: " + result);

        Object lookupResult = lookupResult_getNextRunTime.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (lookupResult instanceof Throwable)
            throw new Exception("failure during first getNextRunTime", (Throwable) lookupResult);
        else if (!"value1".equals(lookupResult))
            throw new Exception("unexpected lookup result during first getNextRunTime: " + lookupResult);

        lookupResult = lookupResult_skipRun.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (lookupResult instanceof Throwable)
            throw new Exception("failure during skipRun", (Throwable) lookupResult);
        else if (!"value1".equals(lookupResult))
            throw new Exception("unexpected lookup result during skipRun: " + lookupResult);

        lookupResult_getNextRunTime.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (lookupResult instanceof Throwable)
            throw new Exception("failure during second getNextRunTime", (Throwable) lookupResult);
        else if (!"value1".equals(lookupResult))
            throw new Exception("unexpected lookup result during second getNextRunTime: " + lookupResult);
    }

    /**
     * Tests the default instance of ManagedThreadFactory, which should be accessible as ManagedThreadFactory and ThreadFactory.
     */
    @Test
    public void testDefaultThreadFactory() throws Throwable {
        final LinkedBlockingQueue<Object> resultQueue = new LinkedBlockingQueue<Object>();
        final Runnable loadClass = new Runnable() {
            @Override
            public void run() {
                try {
                    resultQueue.add(Thread.currentThread().getContextClassLoader().loadClass(EEConcurrencyTestServlet.class.getName()));
                } catch (Throwable x) {
                    resultQueue.add(x);
                }
            }
        };
        final Runnable runTransaction = new Runnable() {
            @Override
            public void run() {
                try {
                    UserTransaction tran = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
                    tran.begin();
                    tran.commit();
                    resultQueue.add(tran);
                } catch (Throwable x) {
                    resultQueue.add(x);
                }
            }
        };

        // Do this on the main thread first to get the lazy init for xa recovery done so it doesn't interfere with test timeouts
        runTransaction.run();
        Object result = resultQueue.poll();
        if (result instanceof Throwable)
            throw new Exception("Task can't even run on the main thread, see cause", (Throwable) result);

        // Switch to an unmanaged thread
        daemon.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Set the class loader to something that shouldn't have visibility to this class
                Thread.currentThread().setContextClassLoader(UserTransaction.class.getClassLoader());

                Thread thread = threadFactoryDefault.newThread(loadClass);
                Thread mthread = mthreadFactoryDefault.newThread(runTransaction);

                ThreadGroup group = thread.getThreadGroup();
                ThreadGroup mgroup = thread.getThreadGroup();
                if (!(group.equals(mgroup)))
                    throw new Exception("Thread group " + group + " doesn't match " + mgroup);

                int priority = thread.getPriority();
                int mpriority = mthread.getPriority();
                if (priority != mpriority)
                    throw new Exception("Thread priority " + priority + " doesn't match " + mpriority);

                boolean daemon = thread.isDaemon();
                boolean mdaemon = mthread.isDaemon();
                if (daemon != mdaemon)
                    throw new Exception("isDaemon " + daemon + " doesn't match " + mdaemon);

                if (((ManageableThread) thread).isShutdown())
                    throw new Exception("thread should not be shutdown");

                if (((ManageableThread) mthread).isShutdown())
                    throw new Exception("mthread should not be shutdown");

                // Default managed thread factory as ThreadFactory
                thread.start();
                Object result = resultQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                else if (!(result instanceof Class) || !EEConcurrencyTestServlet.class.getName().equals(((Class<?>) result).getName()))
                    throw new Exception("Unexpected value from thread from default thread factory: " + result);

                // Default managed thread factory as ManagedThreadFactory
                mthread.start();
                result = resultQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                else if (!(result instanceof UserTransaction))
                    throw new Exception("Unexpected value from thread from default managed thread factory: " + result);

                // Wait for threads to finish if they haven't already
                for (long timeElapsed = 0; timeElapsed < TIMEOUT && (thread.isAlive() || mthread.isAlive()); timeElapsed += POLL_INTERVAL)
                    Thread.sleep(POLL_INTERVAL);

                if (!((ManageableThread) thread).isShutdown())
                    throw new Exception("thread should be shutdown");

                if (!((ManageableThread) mthread).isShutdown())
                    throw new Exception("mthread should be shutdown");

                return null;
            }
        }).get(TIMEOUT * 5, TimeUnit.MILLISECONDS);
    }

    /**
     * Tests that a contextual proxy method can raise an exception without causing an FFDC to be logged.
     */
    @Test
    public void testExceptionOnContextualProxyMethod() throws Exception {
        Closeable contextualCloseable = contextSvcDefault.createContextualProxy(new Closeable() {
            @Override
            public void close() throws IOException {
                throw new IOException("intentional failure");
            }
        }, Closeable.class);

        try {
            contextualCloseable.close();
            fail("Exception from contextal proxy method was lost.");
        } catch (IOException x) {
            assertEquals("intentional failure", x.getMessage());
        }
    }

    /**
     * Tests ManagedExecutorService.execute
     */
    @Test
    public void testExecute() throws Exception {

        // The Liberty ExecutorService.execute implementation often doesn't run tasks very
        // quickly when submitted from a pooled thread, so use a non-pooled thread for this test.
        daemon.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                long start = System.currentTimeMillis();

                AtomicInteger counter = new AtomicInteger();
                xsvcNoContext.execute(new CounterTask(counter));

                // If necessary, wait more time for the task to run
                for (long time = start; time < start + TIMEOUT && counter.get() == 0; time = System.currentTimeMillis())
                    Thread.sleep(POLL_INTERVAL);

                int count = counter.get();
                if (count < 1)
                    throw new Exception("Task did not execute in " + TIMEOUT + " ms");

                if (count > 1)
                    throw new Exception("Task executed more than once: " + count);

                return null;
            }
        }).get(TIMEOUT * 2, TimeUnit.MILLISECONDS);
    }

    /**
     * Have ManagedTask.getExecutionProperties and ManagedTask.getManagedTaskListener raise exceptions.
     */
    @ExpectedFFDC("java.lang.IllegalMonitorStateException") // intentionally raised by test case's ManagedTask implementation
    @Test
    public void testFailManagedTask() throws Exception {
        ManagedCounterTask task = new ManagedCounterTask();
        task.failToGetExecutionProperties = true;

        try {
            schedxsvcClassloaderContext.scheduleAtFixedRate(task, 0, 37, TimeUnit.MICROSECONDS);
            throw new Exception("Should not be able to schedule task when ManagedTask.getExecutionProperties fails");
        } catch (NumberFormatException x) {
        } // pass

        try {
            schedxsvcClassloaderContext.invokeAll(Collections.singleton(task));
            throw new Exception("Should not be able to do invokeAll when ManagedTask.getExecutionProperties fails");
        } catch (NumberFormatException x) {
        } // pass

        try {
            schedxsvcClassloaderContext.submit(task, "Result");
            throw new Exception("Should not be able to submit a task when ManagedTask.getExecutionProperties fails");
        } catch (NumberFormatException x) {
        } // pass

        task.failToGetExecutionProperties = false;
        task.failToGetManagedTaskListener = true;

        try {
            schedxsvcClassloaderContext.schedule((Runnable) task, 38, TimeUnit.NANOSECONDS);
            throw new Exception("Should not be able to schedule task when ManagedTask.getManagedTaskListener fails");
        } catch (IllegalMonitorStateException x) {
        } // pass

        try {
            schedxsvcClassloaderContext.invokeAny(Collections.singleton(task));
            throw new Exception("Should not be able to do invokeAny when ManagedTask.getManagedTaskListener fails");
        } catch (IllegalMonitorStateException x) {
        } // pass

        try {
            schedxsvcClassloaderContext.submit((Callable<Integer>) task);
            throw new Exception("Should not be able to submit a task when ManagedTask.getManagedTaskListener fails");
        } catch (IllegalMonitorStateException x) {
        } // pass

    }

    /**
     * Schedule a repeating task that fails on its first execution.
     */
    @Test
    public void testFailRepeatingTaskOnFirstExecution() throws Throwable {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Runnable runnable = new FailingTask(1);
        Map<String, String> execProps = Collections.singletonMap(ManagedTask.IDENTITY_NAME, "TASK_THAT_FAILS_ON_FIRST_RUN");
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, execProps, listener);

        ScheduledFuture<?> future = schedxsvcClassloaderContext.scheduleAtFixedRate(managedRunnable, 0, 33, TimeUnit.NANOSECONDS);

        // scheduleAtFixedRate: taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Unexpected delay: " + event);

        // scheduleAtFixedRate: taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Unexpected delay: " + event);

        // scheduleAtFixedRate: taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Unexpected delay: " + event);
        if (!(event.exception instanceof ArithmeticException))
            throw new Exception("scheduleAtFixedRate/taskDone#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof ExecutionException) || !(event.failureFromFutureGet.getCause() instanceof ArithmeticException))
            throw new Exception("scheduleAtFixedRate/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("scheduleAtFixedRate: Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("scheduleAtFixedRate: Task should not be canceled. " + event.future);

        try {
            Object result = future.get();
            throw new Exception("Future should fail with ExecutionException when task execution fails. Instead: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        if (!listener.events.isEmpty())
            throw new Exception("scheduleAtFixedRate: Unexpected events: " + listener.events);
    }

    /**
     * Schedule a repeating task that fails on the last execution.
     */
    @Test
    public void testFailRepeatingTaskOnLastExecution() throws Throwable {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Runnable runnable = new FailingTask(2);
        Map<String, String> execProps = Collections.singletonMap(ManagedTask.IDENTITY_NAME, "TASK_THAT_FAILS_ON_FINAL_RUN");
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, execProps, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE);

        ScheduledFuture<?> future = mschedxsvcClassloaderContext.schedule(managedRunnable, trigger);

        // schedule(runnable, trigger): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Future.get(): Unexpected result: " + event);

        // schedule(runnable, trigger): taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#2: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskStarting #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskStarting#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskStarting#2: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskStarting#2: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Unexpected delay: " + event);
        if (!(event.exception instanceof ArithmeticException))
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof ExecutionException) || !(event.failureFromFutureGet.getCause() instanceof ArithmeticException))
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("schedule(runnable, trigger): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(runnable, trigger): Task should not be canceled. " + event.future);

        try {
            Object result = future.get();
            throw new Exception("Future should fail with ExecutionException when task execution fails. Instead: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        if (!listener.events.isEmpty())
            throw new Exception("schedule(runnable, trigger): Unexpected events: " + listener.events);
    }

    /**
     * Schedule a repeating task that fails on the second execution.
     */
    @Test
    public void testFailRepeatingTaskOnSecondExecution() throws Throwable {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Runnable runnable = new FailingTask(2);
        Map<String, String> execProps = Collections.singletonMap(ManagedTask.IDENTITY_NAME, "TASK_THAT_FAILS_ON_FINAL_RUN");
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, execProps, listener);

        ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleWithFixedDelay(managedRunnable, 0, 34, TimeUnit.NANOSECONDS);

        // scheduleWithFixedDelay: taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Future.get(): Unexpected result: " + event);

        // scheduleWithFixedDelay: taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#2: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskStarting #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskStarting#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskStarting#2: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleWithFixedDelay/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskStarting#2: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Unexpected delay: " + event);
        if (!(event.exception instanceof ArithmeticException))
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof ExecutionException) || !(event.failureFromFutureGet.getCause() instanceof ArithmeticException))
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("scheduleWithFixedDelay: Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("scheduleWithFixedDelay: Task should not be canceled. " + event.future);

        try {
            Object result = future.get();
            throw new Exception("Future should fail with ExecutionException when task execution fails. Instead: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        if (!listener.events.isEmpty())
            throw new Exception("scheduleWithFixedDelay: Unexpected events: " + listener.events);
    }

    /**
     * See what happens when ManagedTaskListener.taskAborted raises an error
     */
    @ExpectedFFDC("java.lang.ArithmeticException") // Test case's ManagedTaskListener intentionally raises this error
    @Test
    public void testFailTaskAborted() throws Throwable {
        TaskListener listener = new TaskListener(true);
        listener.whenToCancel.get(TaskEvent.Type.taskSubmitted).add(CancelType.mayNotInterruptIfRunning); // this will cause the task to be aborted
        listener.whenToFail.get(TaskEvent.Type.taskAborted).add(true);
        Runnable runnable = new CounterTask();
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, listener);

        try {
            Future<?> future = mxsvcNoContext.submit(managedRunnable);
            throw new Exception("submit(runnable): expect task to be rejected when canceled during taskSubmitted. Future: " + future);
        } catch (ArithmeticException x) {
        } // pass

        // taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("submit(runnable): Unexpected first event: " + event);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(runnable)/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("submit(runnable)/taskSubmitted: Wrong task: " + event);
        Future<?> future = event.future;
        if (event.future == null)
            throw new Exception("submit(runnable)/taskSubmitted: Missing future from " + event);

        // taskAborted (due to cancellation)
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("submit(runnable): Unexpected second event: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("submit(runnable)/taskAborted: Wrong exception on " + event).initCause(event.exception);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(runnable)/taskAborted: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("submit(runnable)/taskAborted: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("submit(runnable)/taskAborted: Future does not match " + future + ". Instead " + event);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("submit(runnable)/taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        // taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("submit(runnable): Unexpected third event: " + event);
        if (!(event.exception instanceof ArithmeticException))
            throw new Exception("submit(runnable)/taskDone: Wrong exception on " + event).initCause(event.exception);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(runnable)/taskDone: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("submit(runnable)/taskDone: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("submit(runnable)/taskDone: Future does not match " + future + ". Instead " + event);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("submit(runnable)/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!listener.events.isEmpty())
            throw new Exception("submit(runnable): Unexpected events: " + listener.events);

        listener = new TaskListener();
        listener.whenToCancel.get(TaskEvent.Type.taskSubmitted).add(CancelType.mayNotInterruptIfRunning); // this will cause the task to be aborted
        listener.whenToFail.get(TaskEvent.Type.taskAborted).add(true);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        List<Callable<Integer>> tasks = Arrays.asList(new CounterTask(), managedCallable);
        try {
            List<Future<Integer>> futures = mxsvcNoContext.invokeAll(tasks);
            throw new Exception("invokeAll: expect task to be rejected when canceled during taskSubmitted. Futures: " + futures);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToCancel.get(TaskEvent.Type.taskSubmitted).add(CancelType.mayNotInterruptIfRunning); // this will cause the task to be aborted
        listener.whenToFail.get(TaskEvent.Type.taskAborted).add(true);
        callable = new CounterTask();
        managedCallable = ManagedExecutors.managedTask(callable, listener);

        try {
            Integer result = mxsvcNoContext.invokeAny(Collections.singleton(managedCallable), 4, TimeUnit.DAYS);
            throw new Exception("invokeAny(timeout): expect task to be rejected when canceled during taskSubmitted. Result: " + result);
        } catch (ArithmeticException x) {
        } // pass

        // Wait for events to be processed so that they don't cause unexpected FFDCs during other tests
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(TaskEvent.Type.taskSubmitted, event.type);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(TaskEvent.Type.taskAborted, event.type);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(TaskEvent.Type.taskDone, event.type);

        // Also test with the cancel during taskStarting

        listener = new TaskListener(true);
        listener.whenToCancel.get(TaskEvent.Type.taskStarting).add(CancelType.mayNotInterruptIfRunning); // this will cause the task to be aborted
        listener.whenToFail.get(TaskEvent.Type.taskAborted).add(true);
        managedCallable = ManagedExecutors.managedTask(callable, listener);

        future = mxsvcNoContext.submit(managedCallable);
        try {
            Object result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("submit(callable): expect CancelationException when canceled during taskStarting. Result: " + result);
        } catch (CancellationException x) {
        } // pass

        // taskSubmitted
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("submit(callable): Unexpected first event: " + event);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(callable)/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("submit(callable)/taskSubmitted: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("submit(runnable)/taskSubmitted: Future does not match " + future + ". Instead " + event);

        // taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("submit(callable): Unexpected second event: " + event);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(callable)/taskStarting: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("submit(callable)/taskStarting: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("submit(runnable)/taskStarting: Future does not match " + future + ". Instead " + event);

        // taskAborted (due to cancellation)
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("submit(callable): Unexpected third event: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("submit(callable)/taskAborted: Wrong exception on " + event).initCause(event.exception);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(callable)/taskAborted: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("submit(callable)/taskAborted: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("submit(callable)/taskAborted: Future does not match " + future + ". Instead " + event);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("submit(callable)/taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        // taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("submit(callable): Unexpected fourth event: " + event);
        if (!(event.exception instanceof ArithmeticException))
            throw new Exception("submit(callable)/taskDone: Wrong exception on " + event).initCause(event.exception);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(callable)/taskDone: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("submit(callable)/taskDone: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("submit(callable)/taskDone: Future does not match " + future + ". Instead " + event);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("submit(callable)/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!listener.events.isEmpty())
            throw new Exception("submit(runnable): Unexpected events: " + listener.events);

        listener = new TaskListener();
        listener.whenToCancel.get(TaskEvent.Type.taskStarting).add(CancelType.mayNotInterruptIfRunning); // this will cause the task to be aborted
        listener.whenToFail.get(TaskEvent.Type.taskAborted).add(true);
        managedCallable = ManagedExecutors.managedTask(callable, listener);
        try {
            Integer result = mxsvcNoContext.invokeAny(Collections.singleton(managedCallable));
            throw new Exception("invokeAny: expect task to be rejected when canceled during taskStarting. Result: " + result);
        } catch (CancellationException x) { // task was canceled, it did not start executing and throw an exception
        } // pass

        // Wait for events to be processed so that they don't cause unexpected FFDCs during other tests
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(TaskEvent.Type.taskSubmitted, event.type);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(TaskEvent.Type.taskStarting, event.type);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(TaskEvent.Type.taskAborted, event.type);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(TaskEvent.Type.taskDone, event.type);
    }

    /**
     * When canceling a scheduled task during taskStarting, have ManagedTaskListener.taskAborted raise an error
     */
    @Test
    public void testFailTaskAbortedForCanceledStartingTask() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToCancel.get(TaskEvent.Type.taskStarting).add(CancelType.mayNotInterruptIfRunning);
        listener.whenToFail.get(TaskEvent.Type.taskAborted).add(true);
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Runnable runnable = new CounterTask();
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, listener);

        ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleWithFixedDelay(managedRunnable, 0, 27, TimeUnit.MICROSECONDS);

        // scheduleWithFixedDelay: taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // scheduleWithFixedDelay: taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Unexpected delay: " + event);
        if (!(event.exception instanceof ArithmeticException))
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("scheduleWithFixedDelay: Task should be done. " + event.future);

        if (!event.future.isCancelled())
            throw new Exception("scheduleWithFixedDelay: Task should be canceled. " + event.future);

        try {
            Object result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Scheduled task ought to be canceled. Instead: " + result);
        } catch (CancellationException x) {
        } // pass

        if (!listener.events.isEmpty())
            throw new Exception("scheduleWithFixedDelay: Unexpected events: " + listener.events);
    }

    /**
     * When skipping a scheduled task, have ManagedTaskListener.taskAborted raise an error
     */
    @Test
    public void testFailTaskAbortedForSkippedTask() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskAborted).add(true);
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Runnable runnable = new CounterTask();
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(26, ImmediateRepeatingTrigger.NO_FAILURE, 1); // skip first run

        ScheduledFuture<?> future = mschedxsvcClassloaderContext.schedule(managedRunnable, trigger);

        // schedule(runnable, trigger): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof SkippedException))
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(runnable, trigger): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (!(event.exception instanceof ArithmeticException))
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof AbortedException) || !(event.failureFromFutureGet.getCause() instanceof ArithmeticException))
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("schedule(runnable, trigger): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(runnable, trigger): Task should not be canceled. " + event.future);

        try {
            Object result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Scheduled task ought to be skipped. Instead: " + result);
        } catch (AbortedException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        if (!listener.events.isEmpty())
            throw new Exception("schedule(runnable, trigger): Unexpected events: " + listener.events);
    }

    /**
     * Raise an error from ManagedTaskListener.taskDone.
     * The result of the Future during taskDone needs to be the same as the result of the Future after taskDone,
     * which means the taskDone failure should NOT fail the Future.
     */
    @Test
    public void testFailTaskDone() throws Throwable {
        TaskListener listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskDone).add(true);
        Runnable runnable = new CounterTask();
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, listener);

        mxsvcNoContext.execute(managedRunnable);

        // taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("execute: Unexpected first event: " + event);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("execute/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("execute/taskSubmitted: Wrong task: " + event);
        Future<?> future = event.future;
        Object resultOfRunnable = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (resultOfRunnable != null)
            throw new Exception("execute: future.get should return NULL even if taskDone fails. Result: " + resultOfRunnable);

        // taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("execute: Unexpected second event: " + event);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("execute/taskStarting: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("execute/taskStarting: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("execute/taskStarting: Future does not match " + future + ". Instead " + event);

        // taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("execute: Unexpected third event: " + event);
        if (event.exception != null)
            throw new Exception("execute/taskDone: Wrong exception on " + event).initCause(event.exception);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("execute/taskDone: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("execute/taskDone: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("execute/taskDone: Future does not match " + future + ". Instead " + event);

        if (!listener.events.isEmpty())
            throw new Exception("execute: Unexpected events: " + listener.events);

        // submit(Callable)

        listener = new TaskListener(true);
        listener.whenToFail.get(TaskEvent.Type.taskDone).add(true);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        Future<Integer> futureOfSubmit = mxsvcNoContext.submit(managedCallable);

        Integer result = futureOfSubmit.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("submit(Callable): incorrect result (first attempt): " + result);

        // submit(Callable)/taskSubmitted
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("submit(Callable): Unexpected first event: " + event);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(Callable)/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("submit(Callable)/taskSubmitted: Wrong task: " + event);
        if (event.future != futureOfSubmit)
            throw new Exception("submit(Callable)/taskSubmitted: Future does not match " + futureOfSubmit + ". Instead " + event);

        // submit(Callable)/taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("submit(Callable): Unexpected second event: " + event);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(Callable)/taskStarting: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("submit(Callable)/taskStarting: Wrong task: " + event);
        if (event.future != futureOfSubmit)
            throw new Exception("submit(Callable)/taskStarting: Future does not match " + futureOfSubmit + ". Instead " + event);

        // submit(Callable)/taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("submit(Callable): Unexpected third event: " + event);
        if (event.exception != null)
            throw new Exception("submit(Callable)/taskDone: Unexpected exception on " + event, event.exception);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(Callable)/taskDone: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("submit(Callable)/taskDone: Wrong task: " + event);
        if (event.future != futureOfSubmit)
            throw new Exception("submit(Callable)/taskDone: Future does not match " + futureOfSubmit + ". Instead " + event);
        if (event.failureFromFutureGet != null)
            throw new Exception("submit(Callable)/taskDone: Future.get failed on " + event + ", see cause", event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("submit(Callable)/taskDone: Future returned incorrect result: " + event);

        result = futureOfSubmit.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("submit(Callable): incorrect result (second attempt): " + result);

        if (!listener.events.isEmpty())
            throw new Exception("submit(Callable): Unexpected events: " + listener.events);

        // invokeAll

        listener = new TaskListener(true);
        listener.whenToFail.get(TaskEvent.Type.taskDone).add(true);
        callable = new CounterTask();
        managedCallable = ManagedExecutors.managedTask(callable, listener);
        List<Callable<Integer>> tasks = Arrays.asList(new CounterTask(), managedCallable, new CounterTask());

        List<Future<Integer>> futures = mxsvcNoContext.invokeAll(tasks);
        result = futures.get(1).get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("invokeAll: incorrect result for second task: " + result);

        // invokeAny

        listener = new TaskListener(true);
        listener.whenToFail.get(TaskEvent.Type.taskDone).add(true);
        callable = new CounterTask();
        managedCallable = ManagedExecutors.managedTask(callable, listener);

        result = mxsvcNoContext.invokeAny(Collections.singleton(managedCallable));
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("invokeAny: incorrect result for task: " + result);

        // Wait for events to be processed so that they don't cause unexpected FFDCs during other tests
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(TaskEvent.Type.taskSubmitted, event.type);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(TaskEvent.Type.taskStarting, event.type);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(TaskEvent.Type.taskDone, event.type);
    }

    /**
     * Have ManagedTaskListener.taskDone for a one-time task raise an error.
     * The result of the Future during taskDone needs to be the same as the result of the Future after taskDone,
     * which means the taskDone failure should NOT fail the Future.
     */
    @Test
    public void testFailTaskDoneForOneTimeTask() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToFail.get(TaskEvent.Type.taskDone).add(true);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule(managedCallable, 30, TimeUnit.NANOSECONDS);

        // schedule(callable): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskStarting#1: Unexpected delay: " + event);

        // schedule(callable): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("schedule(callable)/taskDone#1: Unexpected result: " + event);

        if (!event.future.isDone())
            throw new Exception("schedule(callable): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(callable): Task should not be canceled. " + event.future);

        Integer result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("Future returned unexpected result: " + result);

        if (!listener.events.isEmpty())
            throw new Exception("schedule(callable): Unexpected events: " + listener.events);
    }

    /**
     * Have ManagedTaskListener.taskDone for a repeating task raise an error.
     * The result of the Future during taskDone needs to be the same as the result of the Future after taskDone,
     * which means the taskDone failure should NOT fail the Future.
     */
    @Test
    public void testFailTaskDoneForRepeatingTask() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToFail.get(TaskEvent.Type.taskDone).add(true);
        listener.whenToFail.get(TaskEvent.Type.taskDone).add(true);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);
        Trigger twoShotTrigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE);

        ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule(managedCallable, twoShotTrigger);

        // schedule(callable): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskStarting#1: Unexpected delay: " + event);

        // schedule(callable): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected result: " + event);

        // schedule(callable): taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#2: Unexpected delay: " + event);

        // schedule(callable): taskStarting #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskStarting#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskStarting#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskStarting#2: Unexpected delay: " + event);

        // schedule(callable): taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable)/taskDone#2: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(2).equals(event.result))
            throw new Exception("schedule(callable)/taskDone#2: Unexpected result: " + event);

        if (!event.future.isDone())
            throw new Exception("schedule(callable): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(callable): Task should not be canceled. " + event.future);

        Integer result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(2).equals(event.result))
            throw new Exception("Future returned unexpected result: " + result);

        if (!listener.events.isEmpty())
            throw new Exception("schedule(callable): Unexpected events: " + listener.events);
    }

    /**
     * See what happens when ManagedTaskListener.taskStarting raises an error
     */
    @Test
    public void testFailTaskStarting() throws Throwable {
        TaskListener listener = new TaskListener(true);
        listener.whenToFail.get(TaskEvent.Type.taskStarting).add(true);
        Runnable runnable = new CounterTask();
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, listener);

        Future<Integer> future = mxsvcNoContext.submit(managedRunnable, 6);
        try {
            Integer result = future.get();
            throw new Exception("submit(runnable, result): expect task to fail with error raised by taskStarting. Result: " + result);
        } catch (AbortedException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        // taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("submit(runnable): Unexpected first event: " + event);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(runnable)/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("submit(runnable)/taskSubmitted: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("submit(runnable)/taskSubmitted: Future does not match " + future + ". Instead " + event);

        // taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("submit(runnable): Unexpected second event: " + event);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(runnable)/taskStarting: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("submit(runnable)/taskStarting: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("submit(runnable)/taskStarting: Future does not match " + future + ". Instead " + event);

        // taskAborted (due to intentionally caused error)
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("submit(runnable): Unexpected third event: " + event);
        if (!(event.exception instanceof AbortedException) || !(event.exception.getCause() instanceof ArithmeticException))
            throw new Exception("submit(runnable)/taskAborted: Wrong exception on " + event).initCause(event.exception);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(runnable)/taskAborted: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("submit(runnable)/taskAborted: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("submit(runnable)/taskAborted: Future does not match " + future + ". Instead " + event);
        if (!(event.failureFromFutureGet instanceof AbortedException))
            throw new Exception("submit(runnable)/taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);
        if (!(event.failureFromFutureGet.getCause() instanceof ArithmeticException))
            throw new Exception("submit(runnable)/taskAborted: Future did not raise exception with expected cause for " + event, event.failureFromFutureGet);

        // taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("submit(runnable): Unexpected fourth event: " + event);
        if (!(event.exception instanceof ArithmeticException))
            throw new Exception("submit(runnable)/taskDone: Wrong exception on " + event).initCause(event.exception);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("submit(runnable)/taskDone: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("submit(runnable)/taskDone: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("submit(runnable)/taskDone: Future does not match " + future + ". Instead " + event);
        if (!(event.failureFromFutureGet instanceof AbortedException))
            throw new Exception("submit(runnable)/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);
        if (!(event.failureFromFutureGet.getCause() instanceof ArithmeticException))
            throw new Exception("submit(runnable)/taskDone: Future did not raise exception with expected cause for " + event, event.failureFromFutureGet);

        if (!listener.events.isEmpty())
            throw new Exception("submit(runnable): Unexpected events: " + listener.events);

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskStarting).add(true);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        future = mxsvcNoContext.submit(managedCallable);
        try {
            Integer result = future.get(22, TimeUnit.HOURS);
            throw new Exception("submit(callable): expect task to fail with error raised by taskStarting. Result: " + result);
        } catch (AbortedException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskStarting).add(true);
        callable = new CounterTask();
        managedCallable = ManagedExecutors.managedTask(callable, listener);
        List<Callable<Integer>> tasks = Arrays.asList(new CounterTask(), managedCallable, new CounterTask());

        List<Future<Integer>> futures = mxsvcNoContext.invokeAll(tasks);
        try {
            futures.get(1).get();
            throw new Exception("invokeAll: expect task to fail with error raised by taskStarting. Result: " + futures);
        } catch (AbortedException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskStarting).add(true);
        callable = new CounterTask();
        managedCallable = ManagedExecutors.managedTask(callable, listener);

        try {
            Integer result = mxsvcNoContext.invokeAny(Collections.singleton(managedCallable), 7, TimeUnit.DAYS);
            throw new Exception("invokeAny(timeout): expect task to fail with error raised by taskStarting. Result: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }
    }

    /**
     * When starting a scheduled task, have ManagedTaskListener.taskStarting raise an error.
     * Then, have ManagedTaskListener.taskAborted also raise an error.
     */
    @Test
    public void testFailTaskStartingAndTaskAbortedForScheduledTask() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskStarting).add(true);
        listener.whenToFail.get(TaskEvent.Type.taskAborted).add(true);
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(28, ImmediateRepeatingTrigger.NO_FAILURE);

        ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule(managedCallable, trigger);

        // schedule(callable, trigger): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable, trigger): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Unexpected delay: " + event);

        // schedule(callable, trigger): taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof AbortedException) || (!(event.exception.getCause() instanceof ArithmeticException)))
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof AbortedException) || (!(event.failureFromFutureGet.getCause() instanceof ArithmeticException)))
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable, trigger): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (!(event.exception instanceof ArithmeticException))
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof AbortedException) || (!(event.failureFromFutureGet.getCause() instanceof ArithmeticException)))
            throw new Exception("schedule(callable, trigger)/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("schedule(callable, trigger): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(callable, trigger): Task should not be canceled. " + event.future);

        try {
            Integer result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Scheduled task ought to be aborted when taskStarting fails. Instead: " + result);
        } catch (AbortedException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw new Exception("Unexpected error for aborted task. See cause of cause", x);
        } // pass

        if (!listener.events.isEmpty())
            throw new Exception("schedule(callable, trigger): Unexpected events: " + listener.events);
    }

    /**
     * When starting a scheduled task, have ManagedTaskListener.taskStarting raise an error
     */
    @Test
    public void testFailTaskStartingForScheduledTask() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskStarting).add(true);
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Runnable runnable = new CounterTask();
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(18, ImmediateRepeatingTrigger.NO_FAILURE);

        ScheduledFuture<?> future = mschedxsvcClassloaderContext.schedule(managedRunnable, trigger);

        // schedule(runnable, trigger): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof AbortedException) || (!(event.exception.getCause() instanceof ArithmeticException)))
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof AbortedException) || (!(event.failureFromFutureGet.getCause() instanceof ArithmeticException)))
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(runnable, trigger): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (!(event.exception instanceof ArithmeticException))
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof AbortedException) || (!(event.failureFromFutureGet.getCause() instanceof ArithmeticException)))
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("schedule(runnable, trigger): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(runnable, trigger): Task should not be canceled. " + event.future);

        try {
            Object result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Scheduled task ought to be aborted when taskStarting fails. Instead: " + result);
        } catch (AbortedException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw new Exception("Unexpected error for aborted task. See cause of cause", x);
        } // pass

        if (!listener.events.isEmpty())
            throw new Exception("schedule(runnable, trigger): Unexpected events: " + listener.events);
    }

    /**
     * See what happens when ManagedTaskListener.taskSubmitted raises an error
     */
    @ExpectedFFDC("java.lang.ArithmeticException") // Test case's ManagedTaskListener intentionally raises this error
    @Test
    public void testFailTaskSubmitted() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        Runnable runnable = new CounterTask();
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, listener);

        try {
            mxsvcNoContext.execute(managedRunnable);
            throw new Exception("Expecting taskSubmitted failure to fail execute.");
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        runnable = new CounterTask();
        managedRunnable = ManagedExecutors.managedTask(runnable, listener);

        try {
            Future<?> future = mxsvcNoContext.submit(managedRunnable);
            throw new Exception("Expecting taskSubmitted failure to fail submit(runnable). Instead: " + future);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        runnable = new CounterTask();
        managedRunnable = ManagedExecutors.managedTask(runnable, listener);

        try {
            Future<Integer> future = mxsvcNoContext.submit(managedRunnable, 5);
            throw new Exception("Expecting taskSubmitted failure to fail submit(runnable, result). Instead: " + future);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        try {
            Future<Integer> future = mxsvcNoContext.submit(managedCallable);
            throw new Exception("Expecting taskSubmitted failure to fail submit(callable). Instead: " + future);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        callable = new CounterTask();
        managedCallable = ManagedExecutors.managedTask(callable, listener);
        List<Callable<Integer>> tasks = Arrays.asList(new CounterTask(), managedCallable, new CounterTask());

        try {
            List<Future<Integer>> futures = mxsvcNoContext.invokeAll(tasks);
            throw new Exception("Expecting taskSubmitted failure to fail invokeAll. Instead: " + futures);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        callable = new CounterTask();
        managedCallable = ManagedExecutors.managedTask(callable, listener);
        List<Callable<Integer>> tasksB = Arrays.asList(new CounterTask(), managedCallable, new CounterTask());

        try {
            List<Future<Integer>> futures = mxsvcNoContext.invokeAll(tasksB, 5, TimeUnit.DAYS);
            throw new Exception("Expecting taskSubmitted failure to fail invokeAll(timeout). Instead: " + futures);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        callable = new CounterTask();
        managedCallable = ManagedExecutors.managedTask(callable, listener);
        List<Callable<Integer>> tasksC = Arrays.asList(new CounterTask(), managedCallable, new CounterTask());

        try {
            Integer result = mxsvcNoContext.invokeAny(tasksC);
            throw new Exception("Expecting taskSubmitted failure to fail invokeAny. Instead: " + result);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        callable = new CounterTask();
        managedCallable = ManagedExecutors.managedTask(callable, listener);
        List<Callable<Integer>> tasksD = Arrays.asList(new CounterTask(), managedCallable, new CounterTask());

        try {
            Integer result = mxsvcNoContext.invokeAny(tasksD, 2, TimeUnit.MINUTES);
            throw new Exception("Expecting taskSubmitted failure to fail invokeAny(timeout). Instead: " + result);
        } catch (ArithmeticException x) {
        } // pass
    }

    /**
     * When rescheduling a task, have ManagedTaskListener.taskSubmitted raise an error
     */
    @Test
    public void testFailTaskSubmittedForRescheduledTask() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(false);
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);

        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(19, ImmediateRepeatingTrigger.NO_FAILURE);

        ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule(managedCallable, trigger);
        try {
            Integer result = future.get();
            if (!Integer.valueOf(1).equals(result))
                throw new Exception("Unexpected result: " + result);
        } catch (AbortedException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw new Exception("Incorrect cause when taskSubmitted failure causes abort.", x);
        }

        // schedule(callable, trigger): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable, trigger): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Unexpected delay: " + event);

        // schedule(callable, trigger): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Non-null exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected error from Future.get for " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected result for schedule(callable, trigger): " + event);

        // schedule(callable, trigger): taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Unexpected delay: " + event);

        // schedule(callable, trigger): taskAborted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable, trigger)/taskAborted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskAborted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskAborted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskAborted#2: Unexpected delay: " + event);
        if (!(event.exception instanceof AbortedException) || (!(event.exception.getCause() instanceof ArithmeticException)))
            throw new Exception("schedule(callable, trigger)/taskAborted#2: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof AbortedException) || (!(event.failureFromFutureGet.getCause() instanceof ArithmeticException)))
            throw new Exception("schedule(callable, trigger)/taskAborted#2: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable, trigger): taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Unexpected delay: " + event);
        if (!(event.exception instanceof ArithmeticException))
            throw new Exception("schedule(callable, trigger)/taskDone#2: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof AbortedException) || (!(event.failureFromFutureGet.getCause() instanceof ArithmeticException)))
            throw new Exception("schedule(callable, trigger)/taskDone#2: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("schedule(callable, trigger): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(callable, trigger): Task should not be canceled. " + event.future);

        try {
            Integer result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Future ought to be aborted. Instead: " + result);
        } catch (AbortedException x) {
            if (!(x.getCause() instanceof ArithmeticException))
                throw x;
        }

        if (!listener.events.isEmpty())
            throw new Exception("schedule(callable, trigger): Unexpected events: " + listener.events);
    }

    /**
     * When scheduling a task, have ManagedTaskListener.taskSubmitted raise an error
     */
    @Test
    public void testFailTaskSubmittedForScheduledTask() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        Runnable runnable = new CounterTask();
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, listener);

        try {
            ScheduledFuture<?> future = schedxsvcClassloaderContext.scheduleWithFixedDelay(managedRunnable, 20, 200, TimeUnit.DAYS);
            throw new Exception("Expecting taskSubmitted failure to fail scheduleWithFixedDelay. Instead: " + future);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        runnable = new CounterTask();
        managedRunnable = ManagedExecutors.managedTask(runnable, listener);

        try {
            ScheduledFuture<?> future = schedxsvcClassloaderContext.scheduleWithFixedDelay(managedRunnable, 21, 210, TimeUnit.HOURS);
            throw new Exception("Expecting taskSubmitted failure to fail scheduleWithFixedRate. Instead: " + future);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        runnable = new CounterTask();
        managedRunnable = ManagedExecutors.managedTask(runnable, listener);

        try {
            ScheduledFuture<?> future = schedxsvcClassloaderContext.schedule(managedRunnable, 22, TimeUnit.MINUTES);
            throw new Exception("Expecting taskSubmitted failure to fail schedule(Runnable). Instead: " + future);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        runnable = new CounterTask();
        managedRunnable = ManagedExecutors.managedTask(runnable, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(23, ImmediateRepeatingTrigger.NO_FAILURE);

        try {
            ScheduledFuture<?> future = mschedxsvcClassloaderContext.schedule(managedRunnable, trigger);
            throw new Exception("Expecting taskSubmitted failure to fail schedule(Runnable, Trigger). Instead: " + future);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        try {
            ScheduledFuture<Integer> future = schedxsvcClassloaderContext.schedule(managedCallable, 24, TimeUnit.SECONDS);
            throw new Exception("Expecting taskSubmitted failure to fail schedule(Callable). Instead: " + future);
        } catch (ArithmeticException x) {
        } // pass

        listener = new TaskListener();
        listener.whenToFail.get(TaskEvent.Type.taskSubmitted).add(true);
        callable = new CounterTask();
        managedCallable = ManagedExecutors.managedTask(callable, listener);
        trigger = new ImmediateRepeatingTrigger(25, ImmediateRepeatingTrigger.NO_FAILURE);

        try {
            ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule(managedCallable, trigger);
            throw new Exception("Expecting taskSubmitted failure to fail schedule(Callable, Trigger). Instead: " + future);
        } catch (ArithmeticException x) {
        } // pass
    }

    /**
     * For a repeating task, have trigger.getNextRunTime fail to calculate the next repeat after a successful run.
     */
    @Test
    public void testFailTriggerNextRunTime() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Runnable runnable = new CounterTask();
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(31, 1); // fail the getNextRunTime attempt after the first execution

        ScheduledFuture<?> future = mschedxsvcClassloaderContext.schedule(managedRunnable, trigger);

        // schedule(runnable, trigger): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (!(event.exception instanceof IllegalStateException))
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof ExecutionException) || (!(event.failureFromFutureGet.getCause() instanceof IllegalStateException)))
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("schedule(runnable, trigger): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(runnable, trigger): Task should not be canceled. " + event.future);

        try {
            Object result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Scheduled task ought to be aborted when Trigger.getNextRunTime fails. Instead: " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof IllegalStateException))
                throw new Exception("Unexpected error for aborted task. See cause of cause", x);
        } // pass

        if (!listener.events.isEmpty())
            throw new Exception("schedule(runnable, trigger): Unexpected events: " + listener.events);
    }

    /**
     * For a repeating task, have trigger.getNextRunTime fail to calculate the next repeat after a skipped run.
     */
    @Test
    public void testFailTriggerNextRunTimeAfterSkipped() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Runnable runnable = new CounterTask();
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(32, 1, 1); // skip the first run, then fail on the getNextRunTime after that

        ScheduledFuture<?> future = mschedxsvcClassloaderContext.schedule(managedRunnable, trigger);

        // schedule(runnable, trigger): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof SkippedException))
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(runnable, trigger)/taskAborted#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(runnable, trigger): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (!(event.exception instanceof IllegalStateException))
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof AbortedException) || (!(event.failureFromFutureGet.getCause() instanceof IllegalStateException)))
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("schedule(runnable, trigger): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(runnable, trigger): Task should not be canceled. " + event.future);

        try {
            Object result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Scheduled task ought to report SkippedException when Trigger.skipRun causes a run to be skipped. Instead: " + result);
        } catch (AbortedException x) {
            if (!(x.getCause() instanceof IllegalStateException))
                throw x;
        }

        if (!listener.events.isEmpty())
            throw new Exception("schedule(runnable, trigger): Unexpected events: " + listener.events);
    }

    /**
     * Skip the first execution of a repeating task by having Trigger.skipRun fail.
     */
    @Test
    public void testFailTriggerSkipRun() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        // Schedule 2 executions, but skip the first one by failing the skipRun method
        ImmediateRepeatingTrigger trigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE, 1);
        trigger.failSkips = true;

        ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule(managedCallable, trigger);

        // schedule(callable): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable): taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskAborted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskAborted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof SkippedException) || (!(event.exception.getCause() instanceof NegativeArraySizeException)))
            throw new Exception("schedule(callable)/taskAborted#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException) || !(event.failureFromFutureGet.getCause() instanceof NegativeArraySizeException))
            throw new Exception("schedule(callable)/taskAborted#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException) || !(event.failureFromFutureGet.getCause() instanceof NegativeArraySizeException))
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#2: Unexpected delay: " + event);

        // schedule(callable): taskStarting #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskStarting#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskStarting#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskStarting#2: Unexpected delay: " + event);

        // schedule(callable): taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable)/taskDone#2: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("schedule(callable)/taskDone#2: Future.get(): Unexpected result: " + event);

        if (!event.future.isDone())
            throw new Exception("schedule(callable): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(callable): Task should not be canceled. " + event.future);

        Integer result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected result for future: " + result);

        if (!listener.events.isEmpty())
            throw new Exception("schedule(callable): Unexpected events: " + listener.events);
    }

    /**
     * Tests error conditions for ManagedScheduledExecutorService.scheduleAtFixedRate and scheduledAtFixedDelay
     */
    @Test
    public void testFixedRateFixedDelayErrors() throws Exception {
        // Futures to cancel in case the test doesn't run to completion
        final List<Future<?>> futures = new LinkedList<Future<?>>();
        try {
            // null task for scheduleWithFixedDelay
            try {
                ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleWithFixedDelay(null, 10, 30, TimeUnit.MINUTES);
                futures.add(future);
                throw new Exception("scheduleWithFixedDelay(null, ...) should cause NullPointerException");
            } catch (NullPointerException x) {
            } // pass

            // null task for scheduleAtFixedRate
            try {
                ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleAtFixedRate(null, 10, 24, TimeUnit.HOURS);
                futures.add(future);
                throw new Exception("scheduleAtFixedRate(null, ...) should cause NullPointerException");
            } catch (NullPointerException x) {
            } // pass

            // null units for scheduleWithFixedDelay
            try {
                ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleWithFixedDelay(new CounterTask(), 0, 300, null);
                futures.add(future);
                throw new Exception("scheduleWithFixedDelay(..., null TimeUnit) should cause NullPointerException");
            } catch (NullPointerException x) {
            } // pass

            // null units for scheduleAtFixedRate
            try {
                ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleAtFixedRate(new CounterTask(), 4, 40, null);
                futures.add(future);
                throw new Exception("scheduleAtFixedRate(..., null TimeUnit) should cause NullPointerException");
            } catch (NullPointerException x) {
            } // pass

            // zero delay
            try {
                ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleWithFixedDelay(new CounterTask(), 5, 0, TimeUnit.DAYS);
                futures.add(future);
                throw new Exception("scheduleWithFixedDelay(0 delay) should cause IllegalArugmentException");
            } catch (IllegalArgumentException x) {
            } // pass

            // zero rate
            try {
                ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleAtFixedRate(new CounterTask(), 6, 0, TimeUnit.NANOSECONDS);
                futures.add(future);
                throw new Exception("scheduleAtFixedRate(0 rate) should cause IllegalArugmentException");
            } catch (IllegalArgumentException x) {
            } // pass

            // negative delay
            try {
                ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleWithFixedDelay(new CounterTask(), 7, -7, TimeUnit.HOURS);
                futures.add(future);
                throw new Exception("scheduleWithFixedDelay(negative delay) should cause IllegalArugmentException");
            } catch (IllegalArgumentException x) {
            } // pass

            // negative rate
            try {
                ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleAtFixedRate(new CounterTask(), 8, -8, TimeUnit.SECONDS);
                futures.add(future);
                throw new Exception("scheduleAtFixedRate(negative rate) should cause IllegalArugmentException");
            } catch (IllegalArgumentException x) {
            } // pass

            // failed fixed delay task
            ScheduledFuture<?> fixedDelayFuture = mschedxsvcClassloaderContext.scheduleWithFixedDelay(new FailingTask(2), 0, 9, TimeUnit.MICROSECONDS);
            futures.add(fixedDelayFuture);
            try {
                Object result = null;
                for (long begin = System.currentTimeMillis(), time = begin; time < begin + TIMEOUT; time = System.currentTimeMillis())
                    try {
                        result = fixedDelayFuture.get(POLL_INTERVAL, TimeUnit.MILLISECONDS);
                        Thread.sleep(POLL_INTERVAL);
                    } catch (TimeoutException timeoutX) {
                    } // okay, keep trying
                throw new Exception("Fixed delay task should have failed on second execution. Result: " + result);
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof ArithmeticException))
                    throw x;
            }

            // failed fixed rate task
            ScheduledFuture<?> fixedRateFuture = mschedxsvcClassloaderContext.scheduleAtFixedRate(new FailingTask(3), 0, 10, TimeUnit.NANOSECONDS);
            futures.add(fixedRateFuture);
            try {
                Object result = null;
                // The timing is not exact here, but close enough
                for (long begin = System.currentTimeMillis(), time = begin; time < begin + TIMEOUT; time = System.currentTimeMillis())
                    try {
                        result = fixedRateFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
                        Thread.sleep(POLL_INTERVAL);
                    } catch (TimeoutException timeoutX) {
                    } // okay, keep trying
                throw new Exception("Fixed rate task should have failed on third execution. Result: " + result);
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof ArithmeticException))
                    throw x;
            }
        } finally {
            // clean up anything that is still running
            for (Future<?> future : futures)
                future.cancel(true);
        }
    }

    /**
     * Ensure that Future.get can be invoked on a different thread that is started from taskSubmitted.
     */
    @Test
    public void testFutureGetOnThreadStartedFromTaskSubmitted() throws Throwable {
        final LinkedBlockingQueue<Future<Long>> futures = new LinkedBlockingQueue<Future<Long>>();
        TaskAndListener task = new TaskAndListener() {
            @Override
            public void taskSubmitted(final Future<?> future, ManagedExecutorService executor, Object task) {
                super.taskSubmitted(future, executor, task);
                futures.add(daemon.submit(new Callable<Long>() {
                    @Override
                    public Long call() throws Exception {
                        long start = System.nanoTime();
                        future.get(TIMEOUT, TimeUnit.MILLISECONDS);
                        return System.nanoTime() - start;
                    }
                }));
            }
        };
        task.sleep = POLL_INTERVAL;
        Trigger trigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE);
        ManagedScheduledExecutorService executor = (ManagedScheduledExecutorService) new InitialContext().lookup(
                                                                                                                 "java:app/env/concurrent/schedxsvc-classloader-context-ref");
        executor.schedule(task, trigger);

        Future<Long> future = futures.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (future == null)
            throw new Exception("First taskSubmitted did not add future within allotted interval");
        future.get(TIMEOUT, TimeUnit.MILLISECONDS);

        future = futures.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (future == null)
            throw new Exception("Second taskSubmitted did not add future within allotted interval");
        future.get(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * For repeating tasks, the future result can change over time as each repeated task execution completes.
     */
    @Test
    public void testGetMultipleResults() throws Throwable {
        List<Integer> results = new LinkedList<Integer>();
        Trigger trigger20 = new ImmediateRepeatingTrigger(20, ImmediateRepeatingTrigger.NO_FAILURE);
        ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule((Callable<Integer>) new CounterTask(), trigger20);
        for (Integer result = future.get(), previousResult = 0; result < 20; previousResult = result, result = future.get()) {
            results.add(result);
            if (result < previousResult)
                throw new Exception("Result should not be less than a previous result: " + results);
            Thread.sleep(1);
        }
        System.out.println("testGetMultipleResults: list of results: " + results);

        if (!future.isDone())
            throw new Exception("Future should be done after getting result");

        if (future.isCancelled())
            throw new Exception("Future should not be canceled");

        long delay = future.getDelay(TimeUnit.MILLISECONDS);
        if (delay > 0)
            throw new Exception("Completed future should not have a delay: " + delay);
    }

    /**
     * Tests the precedence for the ManagedTask.IDENTITY_NAME execution property when different values are
     * specified for the Jakarta vs Java EE constant for the same task. The enabled spec must take precedence.
     */
    @Test
    public void testIdentityNamePrecedence() throws Exception {
        String disabledSpecIdentityNameConstant = ManagedTask.class.getPackage().getName().startsWith("jakarta") //
                        ? ManagedTask.IDENTITY_NAME.replace("jakarta", "javax") //
                        : ManagedTask.IDENTITY_NAME.replace("javax", "jakarta");

        GetIdentityName getIdentityName = new GetIdentityName();
        getIdentityName.execProps.put(disabledSpecIdentityNameConstant, "testIdentityNamePrecedence-DoNotUse");
        getIdentityName.execProps.put(ManagedTask.IDENTITY_NAME, "testIdentityNamePrecedence-Expected");

        ScheduledFuture<String> future = mschedxsvcClassloaderContext.schedule(getIdentityName, getIdentityName);

        final long TIMEOUT_NS = TimeUnit.MILLISECONDS.toNanos(TIMEOUT);
        for (long start = System.nanoTime(); !future.isDone() && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL));

        assertTrue(future.isDone());
        assertEquals("testIdentityNamePrecedence-Expected", future.get());
    }

    /**
     * Verify that InheritableThreadLocal values are made available to child threads when using ManagedThreadFactory.
     */
    @Test
    public void testInheritableThreadLocal() throws Exception {
        String currentThreadInfo = threadInfo.get();
        final LinkedBlockingQueue<String> newThreadInfoQueue = new LinkedBlockingQueue<String>();

        ManagedThreadFactory mtf = (ManagedThreadFactory) new InitialContext().lookup(
                                                                                      "java:module/env/concurrent/threadFactory-jee-metadata-context-ref");
        Thread thread = mtf.newThread(new Runnable() {
            @Override
            public void run() {
                newThreadInfoQueue.add(threadInfo.get());
            }
        });
        thread.start();

        String newThreadInfo = newThreadInfoQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (newThreadInfo == null)
            throw new Exception("Thread didn't run or returned a null. " + thread.getState() + " " + thread);

        if (!newThreadInfo.endsWith(currentThreadInfo))
            throw new Exception("New thread info [" + newThreadInfo + "] did not inherit from [" + currentThreadInfo + "]");

        if (newThreadInfo.equals(currentThreadInfo))
            throw new Exception("Thread info [" + newThreadInfo + "] should not be identical");
    }

    /**
     * Tests that a managed thread inherits the priority, but not the daemon status of the thread from which is was created.
     * For managed threads, isDaemon should always default to false.
     */
    @Test
    public void testInheritThreadAttributes() throws Throwable {

        final Runnable doNothing = new CounterTask();

        int servletThreadPriority = Thread.currentThread().getPriority();

        Thread newThread = threadFactoryJEEMetadataContext.newThread(doNothing);
        int newThreadPriority = newThread.getPriority();
        boolean newThreadIsDaemon = newThread.isDaemon();

        if (servletThreadPriority != newThreadPriority)
            throw new Exception("New thread created priority " + newThreadPriority + " doesn't match servlet thread priority " + servletThreadPriority);

        if (newThreadIsDaemon)
            throw new Exception("New thread should default to isDaemon=false when created from servlet thread");

        ThreadGroup group1 = newThread.getThreadGroup();

        newThread.interrupt();

        final LinkedBlockingQueue<Object> resultQueue = new LinkedBlockingQueue<Object>();
        Thread unmanagedThread = new Thread() {
            @Override
            public void run() {
                try {
                    int unmanagedThreadPriority = Thread.currentThread().getPriority();

                    Thread newThread = threadFactoryJEEMetadataContext.newThread(doNothing);
                    int newThreadPriority = newThread.getPriority();
                    boolean newThreadIsDaemon = newThread.isDaemon();

                    if (unmanagedThreadPriority != newThreadPriority)
                        throw new Exception("New thread created priority " + newThreadPriority + " doesn't match thread priority " + unmanagedThreadPriority);
                    if (newThreadIsDaemon)
                        throw new Exception("New thread should default to isDaemon=false when created from unmanaged thread");

                    ThreadGroup group = newThread.getThreadGroup();
                    newThread.interrupt();
                    resultQueue.add(group);
                } catch (Throwable x) {
                    resultQueue.add(x);
                }
            }
        };
        unmanagedThread.setPriority(servletThreadPriority - 1);
        unmanagedThread.start();

        Object result = resultQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        ThreadGroup group2;
        if (result instanceof Throwable)
            throw (Throwable) result;
        else if (result instanceof ThreadGroup)
            group2 = (ThreadGroup) result;
        else
            throw new Exception("Unexpected result: " + result);

        if (group1 != group2)
            throw new Exception("ThreadGroup does not match: " + group1 + ", " + group2);

        // Should not be able to inherit a priority that exceeds the max priority (7)
        Thread.currentThread().setPriority(8);
        newThread = threadFactoryJEEMetadataContext.newThread(doNothing);
        ThreadGroup group = newThread.getThreadGroup();

        int maxPriority = group.getMaxPriority();
        if (maxPriority != 7)
            throw new Exception("Max priority not honored for managed thread factory. Instead: " + maxPriority);

        newThreadPriority = newThread.getPriority();
        if (newThreadPriority != 7) {
            throw new Exception("Expecting new thread to have maximum priority of the thread group (7). Instead: " + newThreadPriority);
        }

        Thread.currentThread().setPriority(2);
        newThread = threadFactoryJEEMetadataContext.newThread(doNothing);
        newThreadPriority = newThread.getPriority();
        if (newThreadPriority != 2)
            throw new Exception("Expecting new thread to inherit priority of the current thread (2). Instead: " + newThreadPriority);
    }

    /**
     * Tests ManagedExecutorService.invokeAll
     */
    @Test
    public void testInvokeAll() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        LinkedList<CounterTask> tasks = new LinkedList<CounterTask>();
        for (int i = 0; i < 9; i++)
            tasks.add(new CounterTask(counter));
        tasks.add(tasks.getFirst()); // duplicate one of the tasks, just for fun

        List<Future<Integer>> futures = xsvcNoContext.invokeAll(tasks);

        int count = counter.get();
        if (count != 10)
            throw new Exception("Incorrect number of tasks " + count + " invoked by invokeAll");

        List<Integer> results = new LinkedList<Integer>();
        for (Future<Integer> future : futures)
            if (future.isDone())
                results.add(future.get());
            else
                throw new Exception("invokeAll returned before some tasks were completed");

        for (int i = 1; i <= 10; i++)
            if (!results.contains(i))
                throw new Exception(i + " is missing from results of invokeAll");

        int size = futures.size();
        if (size != 10)
            throw new Exception("Number of futures " + size + " does not match the number of tasks");

        size = tasks.size();
        if (size != 10)
            throw new Exception("Size of task list has been changed to " + size);

        System.out.println("Resubmit the same tasks to invokeAll(tasks, timeout, TimeUnit)");

        counter.set(0); // reset the counter so we have predictable results

        long start = System.currentTimeMillis();
        futures = xsvcNoContext.invokeAll(tasks, TIMEOUT * 4, TimeUnit.MILLISECONDS);
        long end = System.currentTimeMillis();

        long duration = end - start;
        if (duration > TIMEOUT * 5)
            throw new Exception("invokeAll(tasks, " + (TIMEOUT * 4) + " ms) took much longer than it should have: " + duration + "ms");

        count = counter.get();
        if (count != 10)
            throw new Exception("Incorrect number of tasks " + count + " invoked by invokeAll(tasks, timeout, unit)");

        results.clear();
        for (Future<Integer> future : futures)
            if (future.isDone())
                results.add(future.get());
            else
                throw new Exception("invokeAll(tasks, timeout, unit) returned before some tasks were completed");

        for (int i = 1; i <= 10; i++)
            if (!results.contains(i))
                throw new Exception(i + " is missing from results of invokeAll(tasks, timeout, unit)");

        size = futures.size();
        if (size != 10)
            throw new Exception("Number of futures " + size + " does not match the number of tasks");

        size = tasks.size();
        if (size != 10)
            throw new Exception("Size of task list has been changed to " + size);

        System.out.println("invokeAll on an empty collection of tasks");

        List<Future<Object>> emptyFutures = xsvcNoContext.invokeAll(Collections.<Callable<Object>> emptyList());
        if (!emptyFutures.isEmpty())
            throw new Exception("Should not have any futures for an invokeAll(empty set)");

        emptyFutures = xsvcNoContext.invokeAll(Collections.<Callable<Object>> emptySet(), 22, TimeUnit.SECONDS);
        if (!emptyFutures.isEmpty())
            throw new Exception("Should not have any futures for an invokeAll(empty set, timeout, units)");

        System.out.println("invokeAll on null collection of tasks");
        // Per the JavaDoc, invokeAll must raise NullPointerException if tasks or any of its elements are null

        try {
            xsvcNoContext.invokeAll(null);
        } catch (NullPointerException x) {
        } // expected

        try {
            xsvcNoContext.invokeAll(null, 0, TimeUnit.NANOSECONDS);
        } catch (NullPointerException x) {
        } // expected

        System.out.println("invokeAll on collection of tasks that contains a null element");

        tasks.add(7, null);

        try {
            xsvcNoContext.invokeAll(tasks);
        } catch (NullPointerException x) {
        } // expected

        try {
            xsvcNoContext.invokeAll(tasks, 0, TimeUnit.MICROSECONDS);
        } catch (NullPointerException x) {
        } // expected

        System.out.println("interrupt invokeAll(slow task)");

        // invoke a slow task, and then interrupt this thread while waiting for it
        final AtomicInteger numStarted = new AtomicInteger();
        AtomicInteger numInterruptions = new AtomicInteger();
        Set<SlowTask> oneSlowTask = Collections.singleton(new SlowTask(numStarted, numInterruptions, TIMEOUT));
        final Thread mainThread = Thread.currentThread();
        Future<Void> result = daemon.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Poll for the task to start before interrupting the main thread
                for (long start = System.currentTimeMillis(), time = start; numStarted.get() == 0 && time < start + TIMEOUT; time = System.currentTimeMillis())
                    Thread.sleep(POLL_INTERVAL);
                if (numStarted.get() == 1)
                    mainThread.interrupt();
                else
                    throw new Exception("Task didn't start in " + TIMEOUT + " ms");
                return null;
            }
        });
        try {
            List<Future<Long>> futureList = xsvcNoContext.invokeAll(oneSlowTask, TIMEOUT, TimeUnit.MILLISECONDS);
            result.get(); // allow the failure from the other thread to be reported first
            futureList.get(0).get(); // if running on the current thread, we might have interrupted the task, not the invokeAll
            throw new Exception("invokeAll should have been interrupted");
        } catch (ExecutionException x) {
            // This can happen when the task runs on the current thread and gets interrupted instead of the invokeAll
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        } catch (InterruptedException x) {
            result.get(); // ensure there were no failures on the other thread
            // Poll for the task to be canceled
            for (long begin = System.currentTimeMillis(), time = begin; numInterruptions.get() == 0 && time < begin + TIMEOUT; time = System.currentTimeMillis())
                Thread.sleep(POLL_INTERVAL);
            if (numInterruptions.get() == 0)
                throw new Exception("Task was not canceled when invokeAll was interrupted");
        }

        System.out.println("invokeAll with one task successful and the other failing");

        counter = new AtomicInteger();
        Collection<? extends Callable<Integer>> mixedTasks = Arrays.asList(new CounterTask(counter), new FailingTask(1));
        futures = xsvcNoContext.invokeAll(mixedTasks);
        size = futures.size();
        if (size != 2)
            throw new Exception("Number of futures should match number of tasks. Instead: " + size);
        count = counter.get();
        if (count != 1)
            throw new Exception("Exactly one task should have succeeded. Instead: " + count);
        // first task should have passed
        int firstResult = futures.get(0).get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (firstResult != 1)
            throw new Exception("Incorrect result of first task: " + firstResult);
        // second task should have failed
        try {
            futures.get(1).get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Future for failing (second) task should have raised an error.");
        } catch (ExecutionException x) {
            Throwable cause = x.getCause();
            if (!(cause instanceof IllegalAccessException))
                throw x;
        }

        System.out.println("invokeAll with a single task that fails");

        try {
            futures = xsvcNoContext.invokeAll(Collections.singleton(new FailingTask(1)), TIMEOUT, TimeUnit.MILLISECONDS);
            Future<Integer> future = futures.iterator().next();
            future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Future for failing task should have raised an error.");
        } catch (ExecutionException x) {
            Throwable cause = x.getCause();
            if (!(cause instanceof IllegalAccessException))
                throw x;
        }
    }

    /**
     * Interrupt invokeAll. Ensure tasks are canceled.
     */
    @Test
    public void testInvokeAllInterrupted() throws Exception {
        final LinkedBlockingQueue<Callable<Integer>> startedTasks = new LinkedBlockingQueue<Callable<Integer>>();

        Callable<Integer> task = new Callable<Integer>() {
            private final AtomicInteger count = new AtomicInteger();

            @Override
            public Integer call() throws InterruptedException {
                int executionNumber = count.incrementAndGet();
                System.out.println("ENTRY: call " + executionNumber);
                startedTasks.add(this);
                try {
                    Thread.sleep(TIMEOUT); // expect to be interrupted when task is canceled
                } catch (InterruptedException x) {
                    Thread.interrupted();
                    System.out.println("EXIT:  call " + executionNumber + " interrupted");
                    throw x;
                }
                System.out.println("EXIT:  call " + executionNumber);
                return executionNumber;
            }
        };
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        task = ManagedExecutors.managedTask(task, listener);

        // Interrupt the invokeAll operation once both tasks start
        final Thread mainThread = Thread.currentThread();
        final AtomicReference<Throwable> interrupterThreadFailure = new AtomicReference<Throwable>();
        new Thread() {
            @Override
            public void run() {
                try {
                    if (startedTasks.poll(TIMEOUT, TimeUnit.MILLISECONDS) == null)
                        throw new Exception("Task 1 did not start in a timely manner.");

                    System.out.println("Now interrupting main thread which ought to be running invokeAll");
                    mainThread.interrupt();
                } catch (Throwable x) {
                    interrupterThreadFailure.set(x);
                }
            }
        }.start();

        try {
            List<Future<Integer>> futures = xsvcNoContext.invokeAll(Arrays.asList(task));

            // one acceptable outcome of this test is for the "invokeAll" invocation to itself throw an InterruptedException,
            // which didn't happen if we made it this far...  however, it's also acceptable if one of the tests being
            // executed failed execution due to an ExecutionException that wraps an InterruptedException
            boolean taskWasInterrupted = false;
            for (Future<Integer> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException ee) {
                    if (ee.getCause() instanceof InterruptedException) {
                        taskWasInterrupted = true;
                        break;
                    }
                }
            }

            if (!taskWasInterrupted) {
                Throwable x = interrupterThreadFailure.get();
                if (x == null)
                    throw new Exception("invokeAll should have been interrupted " + futures);
                else
                    throw new Exception("failed to interrupt invokeAll, see cause", x);
            }
        } catch (InterruptedException x) {
        } // pass

        // taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("invokeAll(callable): Unexpected first event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("invokeAll(callable)/taskSubmitted: Wrong executor: " + event);
        if (event.task != task)
            throw new Exception("invokeAll(callable)/taskSubmitted: Wrong task: " + event);
        Future<?> future = event.future;
        if (future == null)
            throw new Exception("invokeAll(callable)/taskSubmitted: Null future for " + event);

        // taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("invokeAll(callable): Unexpected second event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("invokeAll(callable)/taskStarting: Wrong executor: " + event);
        if (event.task != task)
            throw new Exception("invokeAll(callable)/taskStarting: Wrong task: " + event);
        if (event.future != future)
            throw new Exception("invokeAll(callable)/taskStarting: Incorrect future for " + event + ". Actual Future: " + future);

        // taskAborted must be sent if canceled (vs interrupted only)
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (TaskEvent.Type.taskAborted.equals(event.type)) {
            if (!(event.exception instanceof CancellationException))
                throw new Exception("invokeAll(callable)/taskAborted: Incorrect or missing exception on " + event, event.exception);
            if (event.execSvc != mxsvcNoContext)
                throw new Exception("invokeAll(callable)/taskAborted: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("invokeAll(callable)/taskAborted: Wrong task: " + event);
            if (!future.equals(event.future))
                throw new Exception("invokeAll(callable)/taskAborted: Future does not match " + future + ". Instead " + event);

            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // taskDone
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("invokeAll(callable): Unexpected third event: " + event);
        if (!(event.exception instanceof InterruptedException))
            throw new Exception("invokeAll(callable)/taskDone: Incorrect or missing exception on " + event, event.exception);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("invokeAll(callable)/taskDone: Wrong executor: " + event);
        if (event.task != task)
            throw new Exception("invokeAll(callable)/taskDone: Wrong task: " + event);
        if (!future.equals(event.future))
            throw new Exception("invokeAll(callable)/taskDone: Future does not match " + future + ". Instead " + event);
        if (!(event.failureFromFutureGet instanceof CancellationException)
            && (!(event.failureFromFutureGet instanceof ExecutionException) || !(event.failureFromFutureGet.getCause() instanceof InterruptedException)))
            throw new Exception("invokeAll(callable)/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);
    }

    /**
     * Tests ManagedExecutorService.invokeAny
     */
    @Test
    public void testInvokeAny() throws Exception {
        AtomicInteger counter = new AtomicInteger();
        LinkedList<CounterTask> tasks = new LinkedList<CounterTask>();
        for (int i = 0; i < 4; i++)
            tasks.add(new CounterTask(counter));
        tasks.add(tasks.getFirst()); // duplicate one of the tasks, just for fun

        int result = xsvcNoContext.invokeAny(tasks);

        int count = counter.get();
        if (count < 1 || count > 5)
            throw new Exception("Incorrect number of tasks " + count + " invoked by invokeAny");

        if (result < 1 || result > count)
            throw new Exception("Incorrect result of invokeAny: " + result + ". Completed task count is: " + count);

        int size = tasks.size();
        if (size != 5)
            throw new Exception("Size of task list has been changed to " + size);

        System.out.println("invokeAny with timeout");

        counter = new AtomicInteger();
        tasks.clear();
        for (int i = 0; i < 4; i++)
            tasks.add(new CounterTask(counter));
        tasks.add(tasks.getFirst()); // duplicate one of the tasks, just for fun

        long start = System.currentTimeMillis();
        result = mxsvcNoContext.invokeAny(tasks, TIMEOUT * 2, TimeUnit.MILLISECONDS);
        long end = System.currentTimeMillis();

        long duration = end - start;
        if (duration > TIMEOUT * 3)
            throw new Exception("invokeAny(tasks, " + (TIMEOUT * 2) + " ms) took much longer than it should have: " + duration + "ms");

        count = counter.get();
        if (count < 1 || count > 5)
            throw new Exception("Incorrect number of tasks " + count + " invoked by invokeAny(tasks, timeout, timeunit)");

        if (result < 1 || result > count)
            throw new Exception("Incorrect result of invokeAny(tasks, timeout, timeunit): " + result + ". Completed task count is: " + count);

        size = tasks.size();
        if (size != 5)
            throw new Exception("Size of task list has been changed to " + size);

        System.out.println("invokeAny with empty collection of tasks");
        // Per the JavaDoc, invokeAny must raise IllegalAgumentException if tasks is empty

        try {
            mxsvcNoContext.invokeAny(Collections.<Callable<Object>> emptySet());
            throw new Exception("invokeAny must raise IllegalArgumentException if the list of tasks is empty");
        } catch (IllegalArgumentException x) {
        } // expected

        try {
            xsvcNoContext.invokeAny(Collections.<Callable<Object>> emptyList(), 200, TimeUnit.MILLISECONDS);
            throw new Exception("Expecting IllegalArgumentException or TimeoutException when the list of tasks is empty and a timeout is specified");
        } catch (IllegalArgumentException x) {
        } catch (TimeoutException x) {
        }

        System.out.println("invokeAny with null collection of tasks");
        // Per the JavaDoc, invokeAny must raise NullPointerException if tasks, any of its elements, or unit are null

        try {
            xsvcNoContext.invokeAny(null);
            throw new Exception("Expecting NullPointerException for invokeAny(null)");
        } catch (NullPointerException x) {
        } // expected

        try {
            mxsvcNoContext.invokeAny(null, 0, TimeUnit.NANOSECONDS);
        } catch (NullPointerException x) {
        } // expected

        System.out.println("invokeAny with null timeunit");

        counter = new AtomicInteger();
        tasks.clear();
        for (int i = 0; i < 4; i++)
            tasks.add(new CounterTask(counter));

        try {
            xsvcNoContext.invokeAny(tasks, 5, null);
            throw new Exception("Expecting NullPointerException for invokeAny(tasks, 5, null)");
        } catch (NullPointerException x) {
        } // expected

        System.out.println("invokeAny with collection of tasks that contains a null");

        counter = new AtomicInteger();
        tasks.clear();
        for (int i = 0; i < 4; i++)
            tasks.add(new CounterTask(counter));

        tasks.add(3, null);

        try {
            mxsvcNoContext.invokeAny(tasks);
            throw new Exception("Expecting NullPointerException when tasks parameter to invokeAny contains a null");
        } catch (NullPointerException x) {
        } // expected

        try {
            xsvcNoContext.invokeAny(tasks, TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Expecting NullPointerException when tasks parameter to invokeAny(tasks, timeout, unit) contains a null");
        } catch (NullPointerException x) {
        } // expected

        System.out.println("invokeAny with a single task that fails");
        // Per JavaDoc, invokeAny must raise ExecutionException if no task successfully completes

        try {
            int i = xsvcNoContext.invokeAny(Collections.singleton(new FailingTask(1)));
            throw new Exception("invokeAny must be rejected when all tasks fail. Instead: " + i);
        } catch (ExecutionException x) {
            Throwable cause = x.getCause();
            if (!(cause instanceof IllegalAccessException))
                throw x;
        }

        // Per JavaDoc, invokeAny must raise TimeoutException if the given timeout elapses before any task successfully completes

        AtomicInteger numStarted = new AtomicInteger();
        AtomicInteger interruptions = new AtomicInteger();
        try {
            List<SlowTask> slowTasks = Arrays.asList(new SlowTask(numStarted, interruptions, TIMEOUT),
                                                     new SlowTask(numStarted, interruptions, TIMEOUT + 1000),
                                                     new SlowTask(numStarted, interruptions, TIMEOUT + 2000));
            long l = xsvcNoContext.invokeAny(slowTasks, 500, TimeUnit.MILLISECONDS);
            throw new Exception("invokeAny must be rejected when no tasks complete before the timeout. Instead: " + l);
        } catch (TimeoutException x) { // expected
            // All tasks (if they started) should be canceled in response to the timeout.
            // It seems like cancel might happen AFTER the invokeAny method returns, so we will poll for it
            start = System.currentTimeMillis();
            for (long time = start; time < start + TIMEOUT && interruptions.get() < numStarted.get(); time = System.currentTimeMillis())
                Thread.sleep(POLL_INTERVAL);

            int started = numStarted.get();
            int interrupted = interruptions.get();
            if (interrupted != started)
                throw new Exception("All started tasks (" + started + ") should have been canceled due to timeout. Instead: " + interrupted);
        }
    }

    /**
     * It should be possible to supply a ManagedThreadFactory to a Java SE executor,
     * causing it to run with the thread context as of when the ManagedThreadFactory was looked up.
     */
    @Test
    public void testJavaSEExecutorUsingManagedThreadFactory() throws Exception {
        final Callable<String> javaCompLookup = new Callable<String>() {
            @Override
            public String call() throws NamingException {
                return (String) new InitialContext().lookup("java:comp/env/entry1");
            }
        };

        // Switch to an unmanaged thread
        daemon.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(3, threadFactoryJEEMetadataContext);
                try {
                    ScheduledFuture<String> future3 = scheduledThreadPool.schedule(javaCompLookup, 3, TimeUnit.MILLISECONDS);
                    ScheduledFuture<String> future2 = scheduledThreadPool.schedule(javaCompLookup, 2, TimeUnit.MILLISECONDS);
                    Future<String> future1 = scheduledThreadPool.submit(javaCompLookup);

                    String result;
                    result = future1.get(TIMEOUT, TimeUnit.MILLISECONDS);
                    if (!"value1".equals(result))
                        throw new Exception("Unexpected result for future1: " + result);
                    result = future2.get(TIMEOUT, TimeUnit.MILLISECONDS);
                    if (!"value1".equals(result))
                        throw new Exception("Unexpected result for future2: " + result);
                    result = future3.get(TIMEOUT, TimeUnit.MILLISECONDS);
                    if (!"value1".equals(result))
                        throw new Exception("Unexpected result for future3: " + result);
                } finally {
                    scheduledThreadPool.shutdownNow();
                }
                return null;
            }
        }).get(TIMEOUT * 5, TimeUnit.MILLISECONDS);
    }

    /**
     * Tests the identity name that is supplied to LastExecution, both on getNextRunTime and skipRun.
     * To do this, we schedule multiple tasks with a single Trigger, which encodes data in the task identity name
     * (an execution limit) to have the Trigger treat each task differently.
     */
    @Test
    public void testLastExecutionIdentityName() throws Exception {
        Trigger trigger = new MultiTrigger();
        Callable<Integer> task1 = new CounterTask();
        Callable<Integer> task2 = new CounterTask();
        task2 = ManagedExecutors.managedTask(task2, Collections.singletonMap(ManagedTask.IDENTITY_NAME, "testLastExecutionIdentityName-2"), null);

        ScheduledFuture<Integer> future1 = mschedxsvcClassloaderContext.schedule(task1, trigger);
        ScheduledFuture<Integer> future2 = mschedxsvcClassloaderContext.schedule(task2, trigger);

        Integer result = future1.get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("First check - Task without identity name should run exactly once. Instead: " + result);
        result = future1.get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Second check - Task without identity name should run exactly once. Instead: " + result);

        long TIMEOUT_NS = TimeUnit.MILLISECONDS.toNanos(TIMEOUT);
        for (long start = System.nanoTime(); !Integer.valueOf(2).equals(result) && System.nanoTime() - start < TIMEOUT_NS; result = future2.get())
            Thread.sleep(POLL_INTERVAL);

        if (!Integer.valueOf(2).equals(result))
            throw new Exception("First check - Task with identity name should run exactly twice. Instead: " + result);
        result = future2.get();
        if (!Integer.valueOf(2).equals(result))
            throw new Exception("Second check - Task with identity name should run exactly twice. Instead: " + result);
    }

    /**
     * Tests compliance with section 3.1.6.1 of the EE Concurrency Utilities spec,
     * which requires life cycle methods to raise IllegalStateException.
     */
    @Test
    public void testLifeCycleMethods() throws Exception {

        try {
            xsvcDefault.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("awaitTermination must raise IllegalStateException");
        } catch (IllegalStateException x) {
            if (!(x.getCause() instanceof UnsupportedOperationException))
                throw x;
        }

        try {
            schedxsvcDefault.isShutdown();
            throw new Exception("isShutdown must raise IllegalStateException");
        } catch (IllegalStateException x) {
            if (!(x.getCause() instanceof UnsupportedOperationException))
                throw x;
        }

        try {
            xsvcClassloaderContext.isTerminated();
            throw new Exception("isTerminated must raise IllegalStateException");
        } catch (IllegalStateException x) {
            if (!(x.getCause() instanceof UnsupportedOperationException))
                throw x;
        }

        try {
            mxsvcClassloaderContext.shutdown();
            throw new Exception("shutdown must raise IllegalStateException");
        } catch (IllegalStateException x) {
            if (!(x.getCause() instanceof UnsupportedOperationException))
                throw x;
        }

        try {
            xsvcDefault.shutdownNow();
            throw new Exception("shutdownNow must raise IllegalStateException");
        } catch (IllegalStateException x) {
            if (!(x.getCause() instanceof UnsupportedOperationException))
                throw x;
        }
    }

    /**
     * Tests invokeAny with tasks canceled by the managed task listener.
     */
    @Test
    public void testListenerCancelInvokeAny() throws Throwable {
        // This task will be canceled upon submission
        TaskListener listener1 = new TaskListener(true);
        listener1.whenToCancel.get(TaskEvent.Type.taskSubmitted).add(CancelType.mayNotInterruptIfRunning);
        Callable<Integer> callable1 = new CounterTask(new AtomicInteger(0));
        callable1 = ManagedExecutors.managedTask(callable1, listener1);

        List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
        tasks.add(callable1);

        try {
            Integer result = xsvcDefault.invokeAny(tasks);
            throw new Exception("invokeAny should not return result (" + result + ") when all tasks are canceled.");
        } catch (RejectedExecutionException x) {
        } // pass

        // Verify that task 1 is canceled on submit

        // invokeAny/task1: taskSubmitted
        TaskEvent event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("invokeAny/task1: Unexpected first event: " + event);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task1/taskSubmitted: Wrong executor: " + event);
        if (event.task != callable1)
            throw new Exception("invokeAny/task1/taskSubmitted: Wrong task: " + event);
        if (!Boolean.TRUE.equals(event.canceled))
            throw new Exception("invokeAny/task1/taskSubmitted: Unsuccessful attempt to cancel: " + event);

        Future<?> future = event.future;
        if (!future.isCancelled())
            throw new Exception("invokeAny/task1: Task should be canceled. " + future);

        if (!future.isDone())
            throw new Exception("invokeAny/task1: Task should be done. " + future);

        try {
            Integer resultOfFuture = (Integer) future.get();
            throw new Exception("task1 future should be canceled. Instead result is: " + resultOfFuture + ". Event is: " + event);
        } catch (CancellationException x) {
        } // pass

        // invokeAny/task1: taskAborted
        event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("invokeAny/task1: Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny/task1/taskAborted: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task1/taskAborted: Wrong executor: " + event);
        if (event.task != callable1)
            throw new Exception("invokeAny/task1/taskAborted: Wrong task: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("invokeAny/task1/taskAborted: Should have CancellationException for: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("invokeAny/task1/taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        // invokeAll/task1: taskDone
        event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("invokeAny/task1: Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny/task1/taskDone: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task1/taskDone: Wrong executor: " + event);
        if (event.task != callable1)
            throw new Exception("invokeAny/task1/taskDone: Wrong task: " + event);
        if (event.exception != null && !(event.exception instanceof IllegalStateException))
            throw new Exception("invokeAny/task1/taskDone: Unexpected exception for: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("invokeAny/task1/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!listener1.events.isEmpty())
            throw new Exception("invokeAny/task1: Unexpected events: " + listener1.events);

        // Verify that tasks 2 and 3 are canceled upon taskStarting

        // This task will be canceled when starting
        TaskListener listener2 = new TaskListener(true);
        listener2.whenToCancel.get(TaskEvent.Type.taskStarting).add(CancelType.mayNotInterruptIfRunning);
        Callable<Integer> callable2 = new CounterTask(new AtomicInteger(1));
        callable2 = ManagedExecutors.managedTask(callable2, listener2);

        // This task will be canceled when starting, and interrupted if necessary
        TaskListener listener3 = new TaskListener(true);
        listener3.whenToCancel.get(TaskEvent.Type.taskStarting).add(CancelType.mayInterruptIfRunning);
        Callable<Integer> callable3 = new CounterTask(new AtomicInteger(2));
        callable3 = ManagedExecutors.managedTask(callable3, listener3);

        tasks = new LinkedList<Callable<Integer>>();
        tasks.add(callable2);
        tasks.add(callable3);

        try {
            Integer result = xsvcDefault.invokeAny(tasks);
            throw new Exception("invokeAny should not return result (" + result + ") when all tasks are canceled.");
        } catch (CancellationException x) { // tasks were canceled, they did not start executing and throw an exception
        } // pass

        // invokeAny/task2: taskSubmitted
        event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("invokeAny/task2: Unexpected first event: " + event);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task2/taskSubmitted: Wrong executor: " + event);
        if (event.task != callable2)
            throw new Exception("invokeAny/task2/taskSubmitted: Wrong task: " + event);
        future = event.future;
        if (future == null)
            throw new Exception("invokeAny/task2/taskSubmitted: null future: " + event);

        // invokeAny/task2: taskStarting
        event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("invokeAny/task2: Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny/task2/taskStarting: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task2/taskStarting: Wrong executor: " + event);
        if (event.task != callable2)
            throw new Exception("invokeAny/task2/taskStarting: Wrong task: " + event);

        if (!Boolean.TRUE.equals(event.canceled))
            throw new Exception("invokeAny/task2/taskStarting: Unsuccessful attempt to cancel: " + event);

        if (!future.isCancelled())
            throw new Exception("invokeAny/task2: Task should be canceled. " + future);

        if (!future.isDone())
            throw new Exception("invokeAny/task2: Task should be done. " + future);

        try {
            Integer resultOfFuture = (Integer) future.get();
            throw new Exception("task2 future should be canceled. Instead result is: " + resultOfFuture + ". Event is: " + event);
        } catch (CancellationException x) {
        } // pass

        // invokeAny/task2: taskAborted
        event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("invokeAny/task2: Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny/task2/taskAborted: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task2/taskAborted: Wrong executor: " + event);
        if (event.task != callable2)
            throw new Exception("invokeAny/task2/taskAborted: Wrong task: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("invokeAny/task2/taskAborted: Should have CancellationException for: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("invokeAny/task2/taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        // invokeAll/task2: taskDone
        event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("invokeAny/task2: Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny/task2/taskDone: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task2/taskDone: Wrong executor: " + event);
        if (event.task != callable2)
            throw new Exception("invokeAny/task2/taskDone: Wrong task: " + event);
        if (event.exception != null && !(event.exception instanceof IllegalStateException))
            throw new Exception("invokeAny/task2/taskDone: Unexpected exception for: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("invokeAny/task2/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!listener2.events.isEmpty())
            throw new Exception("invokeAny/task2: Unexpected events: " + listener2.events);

        // task 3

        // invokeAny/task3: taskSubmitted
        event = listener3.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("invokeAny/task3: Unexpected first event: " + event);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task3/taskSubmitted: Wrong executor: " + event);
        if (event.task != callable3)
            throw new Exception("invokeAny/task3/taskSubmitted: Wrong task: " + event);
        future = event.future;
        if (future == null)
            throw new Exception("invokeAny/task3/taskSubmitted: null future: " + event);

        // invokeAny/task3: taskStarting
        event = listener3.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("invokeAny/task3: Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny/task3/taskStarting: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task3/taskStarting: Wrong executor: " + event);
        if (event.task != callable3)
            throw new Exception("invokeAny/task3/taskStarting: Wrong task: " + event);

        if (!Boolean.TRUE.equals(event.canceled))
            throw new Exception("invokeAny/task3/taskStarting: Unsuccessful attempt to cancel: " + event);

        if (!future.isCancelled())
            throw new Exception("invokeAny/task3: Task should be canceled. " + future);

        if (!future.isDone())
            throw new Exception("invokeAny/task3: Task should be done. " + future);

        try {
            Integer resultOfFuture = (Integer) future.get();
            throw new Exception("task3 future should be canceled. Instead result is: " + resultOfFuture + ". Event is: " + event);
        } catch (CancellationException x) {
        } // pass

        // invokeAny/task3: taskAborted
        event = listener3.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("invokeAny/task3: Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny/task3/taskAborted: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task3/taskAborted: Wrong executor: " + event);
        if (event.task != callable3)
            throw new Exception("invokeAny/task3/taskAborted: Wrong task: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("invokeAny/task3/taskAborted: Should have CancellationException for: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("invokeAny/task3/taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        // invokeAll/task3: taskDone
        event = listener3.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("invokeAny/task3: Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny/task3/taskDone: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task3/taskDone: Wrong executor: " + event);
        if (event.task != callable3)
            throw new Exception("invokeAny/task3/taskDone: Wrong task: " + event);
        if (event.exception != null && !(event.exception instanceof IllegalStateException))
            throw new Exception("invokeAny/task3/taskDone: Unexpected exception for: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("invokeAny/task3/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!listener3.events.isEmpty())
            throw new Exception("invokeAny/task3: Unexpected events: " + listener3.events);
    }

    /**
     * Tests execute with a managed task listener.
     */
    @Test
    public void testListenerExecute() throws Throwable {
        tran.begin();
        try {
            TaskListener listener = new TaskListener(true);
            Runnable runnable = new CounterTask();
            runnable = ManagedExecutors.managedTask(runnable, listener);

            mschedxsvcClassloaderContext.execute(runnable);

            // execute: taskSubmitted
            TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskSubmitted.equals(event.type))
                throw new Exception("execute: Unexpected first event: " + event);
            if (event.execSvc != mschedxsvcClassloaderContext)
                throw new Exception("execute/taskSubmitted: Wrong executor: " + event);
            if (event.task != runnable)
                throw new Exception("execute/taskSubmitted: Wrong task: " + event);
            if (event.uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
                throw new Exception("execute/taskSubmitted: transaction not suspended before invoking listener. " + event);

            // wait for task to run
            Object result = event.future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            if (result != null)
                throw new Exception("execute: Unexpected result: " + result);

            if (event.future.isCancelled())
                throw new Exception("execute: Task should not be canceled. " + event.future);

            if (!event.future.isDone())
                throw new Exception("execute: Task should be done. " + event.future);

            // execute: taskStarting
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskStarting.equals(event.type))
                throw new Exception("execute: Unexpected second event: " + event);
            if (event.execSvc != mschedxsvcClassloaderContext)
                throw new Exception("execute/taskStarting: Wrong executor: " + event);
            if (event.task != runnable)
                throw new Exception("execute/taskStarting: Wrong task: " + event);
            if (event.uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
                throw new Exception("execute/taskStarting: transaction not suspended before invoking listener. " + event);

            // execute: taskDone
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskDone.equals(event.type))
                throw new Exception("execute: Unexpected third event: " + event);
            if (event.execSvc != mschedxsvcClassloaderContext)
                throw new Exception("execute/taskDone: Wrong executor: " + event);
            if (event.task != runnable)
                throw new Exception("execute/taskDone: Wrong task: " + event);
            if (event.exception != null)
                throw new Exception("execute/taskDone: Non-null exception: " + event).initCause(event.exception);
            if (event.uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
                throw new Exception("execute/taskDone: transaction not suspended before invoking listener. " + event);
            if (event.failureFromFutureGet != null)
                throw new Exception("execute/taskDone: Future.get should have been successful for " + event, event.failureFromFutureGet);
            if (event.result != null)
                throw new Exception("execute/taskDone: runnable should have null result. Instead: " + event);

            if (!listener.events.isEmpty())
                throw new Exception("Unexpected events: " + listener.events);
        } finally {
            tran.commit();
        }
    }

    /**
     * Tests invokeAll method with a managed task listener.
     */
    @Test
    public void testListenerInvokeAll() throws Throwable {
        tran.begin();
        try {
            TaskListener listener1 = new TaskListener(true);
            Callable<Integer> callable1 = new CounterTask();
            callable1 = ManagedExecutors.managedTask(callable1, listener1);

            TaskListener listener2 = new TaskListener(true);
            Callable<Integer> callable2 = new CounterTask();
            callable2 = ManagedExecutors.managedTask(callable2, listener2);

            List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
            tasks.add(callable1);
            tasks.add(callable2);
            List<Future<Integer>> futures = xsvcDefault.invokeAll(tasks);

            Future<Integer> future1 = futures.get(0);
            Future<Integer> future2 = futures.get(1);

            // invokeAll/task1: taskSubmitted
            TaskEvent event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskSubmitted.equals(event.type))
                throw new Exception("invokeAll/task1: Unexpected first event: " + event);
            if (event.future != future1)
                throw new Exception("invokeAll/task1/taskSubmitted: Wrong future: " + event + " vs " + future1);
            if (event.execSvc != xsvcDefault)
                throw new Exception("invokeAll/task1/taskSubmitted: Wrong executor: " + event);
            if (event.task != callable1)
                throw new Exception("invokeAll/task1/taskSubmitted: Wrong task: " + event);

            if (event.future.isCancelled())
                throw new Exception("invokeAll/task1: Task should not be canceled. " + event.future);

            if (!event.future.isDone())
                throw new Exception("invokeAll/task1: Task should be done. " + event.future);

            Integer result1 = (Integer) event.future.get();
            if (result1 != 1)
                throw new Exception("invokeAll/task1: Unexpected result: " + result1);

            // invokeAll/task1: taskStarting
            event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskStarting.equals(event.type))
                throw new Exception("invokeAll/task1: Unexpected second event: " + event);
            if (event.future != future1)
                throw new Exception("invokeAll/task1/taskStarting: Wrong future: " + event + " vs " + future1);
            if (event.execSvc != xsvcDefault)
                throw new Exception("invokeAll/task1/taskStarting: Wrong executor: " + event);
            if (event.task != callable1)
                throw new Exception("invokeAll/task1/taskStarting: Wrong task: " + event);

            // invokeAll/task1: taskDone
            event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskDone.equals(event.type))
                throw new Exception("invokeAll/task1: Unexpected third event: " + event);
            if (event.future != future1)
                throw new Exception("invokeAll/task1/taskDone: Wrong future: " + event + " vs " + future1);
            if (event.execSvc != xsvcDefault)
                throw new Exception("invokeAll/task1/taskDone: Wrong executor: " + event);
            if (event.task != callable1)
                throw new Exception("invokeAll/task1/taskDone: Wrong task: " + event);
            if (event.exception != null)
                throw new Exception("invokeAll/task1/taskDone: Non-null exception: " + event).initCause(event.exception);
            if (event.failureFromFutureGet != null)
                throw new Exception("invokeAll/task1/taskDone: Future.get should have been successful for " + event, event.failureFromFutureGet);
            if (event.result != result1)
                throw new Exception("invokeAll/task1/taskDone: Result of future returned by invokeAll (" + result1 + ") does not match: " + event);

            if (!listener1.events.isEmpty())
                throw new Exception("invokeAll/task1: Unexpected events: " + listener1.events);

            // invokeAll/task2: taskSubmitted
            event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskSubmitted.equals(event.type))
                throw new Exception("invokeAll/task2: Unexpected first event: " + event);
            if (event.future != future2)
                throw new Exception("invokeAll/task2/taskSubmitted: Wrong future: " + event + " vs " + future2);
            if (event.execSvc != xsvcDefault)
                throw new Exception("invokeAll/task2/taskSubmitted: Wrong executor: " + event);
            if (event.task != callable2)
                throw new Exception("invokeAll/task2/taskSubmitted: Wrong task: " + event);

            if (event.future.isCancelled())
                throw new Exception("invokeAll/task2: Task should not be canceled. " + event.future);

            if (!event.future.isDone())
                throw new Exception("invokeAll/task2: Task should be done. " + event.future);

            Integer result2 = (Integer) event.future.get();
            if (result2 != 1)
                throw new Exception("invokeAll/task2: Unexpected result: " + result2);

            // invokeAll/task2: taskStarting
            event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskStarting.equals(event.type))
                throw new Exception("invokeAll/task2: Unexpected second event: " + event);
            if (event.future != future2)
                throw new Exception("invokeAll/task2/taskStarting: Wrong future: " + event + " vs " + future2);
            if (event.execSvc != xsvcDefault)
                throw new Exception("invokeAll/task2/taskStarting: Wrong executor: " + event);
            if (event.task != callable2)
                throw new Exception("invokeAll/task2/taskStarting: Wrong task: " + event);

            // invokeAll/task2: taskDone
            event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskDone.equals(event.type))
                throw new Exception("invokeAll/task2: Unexpected third event: " + event);
            if (event.future != future2)
                throw new Exception("invokeAll/task2/taskDone: Wrong future: " + event + " vs " + future2);
            if (event.execSvc != xsvcDefault)
                throw new Exception("invokeAll/task2/taskDone: Wrong executor: " + event);
            if (event.task != callable2)
                throw new Exception("invokeAll/task2/taskDone: Wrong task: " + event);
            if (event.exception != null)
                throw new Exception("invokeAll/task2/taskDone: Non-null exception: " + event).initCause(event.exception);
            if (event.failureFromFutureGet != null)
                throw new Exception("invokeAll/task2/taskDone: Future.get should have been successful for " + event, event.failureFromFutureGet);
            if (event.result != result1)
                throw new Exception("invokeAll/task2/taskDone: Result of future returned by invokeAll (" + result2 + ") does not match: " + event);

            if (!listener2.events.isEmpty())
                throw new Exception("invokeAll/task2: Unexpected events: " + listener2.events);
        } finally {
            tran.commit();
        }
    }

    /**
     * Tests invokeAll method that times out.
     */
    @Test
    public void testListenerInvokeAllTimeout() throws Throwable {

        AtomicInteger numStarted = new AtomicInteger();
        AtomicInteger numInterruptions = new AtomicInteger();

        TaskListener listener1 = new TaskListener(true);
        Callable<Long> callable1 = new SlowTask(numStarted, numInterruptions, TIMEOUT * 3);
        callable1 = ManagedExecutors.managedTask(callable1, listener1);

        TaskListener listener2 = new TaskListener(true);
        Callable<Long> callable2 = new SlowTask(numStarted, numInterruptions, TIMEOUT * 4);
        callable2 = ManagedExecutors.managedTask(callable2, listener2);

        List<Callable<Long>> tasks = new LinkedList<Callable<Long>>();
        tasks.add(callable1);
        tasks.add(callable2);

        List<Future<Long>> futures = xsvcNoContext.invokeAll(tasks, 500, TimeUnit.MILLISECONDS);
        Future<Long> future1 = futures.get(0);
        Future<Long> future2 = futures.get(1);

        if (!future1.isDone())
            throw new Exception("Future for task1 should be done after timeout.");

        if (!future2.isDone())
            throw new Exception("Future for task2 should be done after timeout.");

        // task1: taskSubmitted
        TaskEvent event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("task1: Unexpected first event: " + event);
        if (event.future != future1)
            throw new Exception("task1/taskSubmitted: Wrong future in " + event + ". Should be: " + future1);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("task1/taskSubmitted: Wrong executor: " + event);
        if (event.task != callable1)
            throw new Exception("task1/taskSubmitted: Wrong task: " + event);

        // Because cancel/interrupt happens asynchronously, various sequences of events are valid:
        // A) taskSubmitted, taskStarting, taskDone
        // B) taskSubmitted, taskStarting, taskAborted, taskDone
        // C) taskSubmitted, taskAborted, taskDone
        int count = 1;

        // task1: taskStarting?
        event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (TaskEvent.Type.taskStarting.equals(event.type)) {
            count++;
            if (event.future != future1)
                throw new Exception("task1/taskStarting: Future does not match: " + event + " vs " + future1);
            if (event.execSvc != xsvcNoContext)
                throw new Exception("task1/taskStarting: Wrong executor: " + event);
            if (event.task != callable1)
                throw new Exception("task1/taskStarting: Wrong task: " + event);

            event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // task1: taskAborted? (might or might not occur depending on timing of interrupt vs cancel)
        if (TaskEvent.Type.taskAborted.equals(event.type)) {
            count++;
            if (event.future != future1)
                throw new Exception("task1/taskAborted: Wrong future in " + event + ". Should be: " + future1);
            if (event.execSvc != xsvcNoContext)
                throw new Exception("task1/taskAborted: Wrong executor: " + event);
            if (event.task != callable1)
                throw new Exception("task1/taskAborted: Wrong task: " + event);
            if (!(event.exception instanceof CancellationException))
                throw new Exception("task1/taskAborted: CancellationException expected for: " + event).initCause(event.exception);
            if (!(event.failureFromFutureGet instanceof CancellationException))
                throw new Exception("task1/taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);
            if (!future1.isCancelled())
                throw new Exception("Future for task1 should be canceled after timeout.");

            event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        if (count <= 1)
            throw new Exception("task1: Did not see taskStarting or taskAborted. Instead: " + event);

        // task1: taskDone
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("task1: Unexpected third/fourth event: " + event);
        if (event.future != future1)
            throw new Exception("task1/taskDone: Wrong future in " + event + ". Should be: " + future1);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("task1/taskDone: Wrong executor: " + event);
        if (event.task != callable1)
            throw new Exception("task1/taskDone: Wrong task: " + event);
        if (event.exception != null && !(event.exception instanceof InterruptedException))
            throw new Exception("task1/taskDone: Unexpected exception for: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException)
            && (!(event.failureFromFutureGet instanceof ExecutionException) || !(event.failureFromFutureGet.getCause() instanceof InterruptedException)))
            throw new Exception("task1/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!listener1.events.isEmpty())
            throw new Exception("task1: Unexpected events: " + listener1.events);

        // task2: taskSubmitted
        event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("task2: Unexpected first event: " + event);
        if (event.future != future2)
            throw new Exception("task2/taskSubmitted: Wrong future in " + event + ". Should be: " + future2);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("task2/taskSubmitted: Wrong executor: " + event);
        if (event.task != callable2)
            throw new Exception("task2/taskSubmitted: Wrong task: " + event);

        // Because cancel/interrupt happens asynchronously, various sequences of events are valid:
        // A) taskSubmitted, taskStarting, taskDone
        // B) taskSubmitted, taskStarting, taskAborted, taskDone
        // C) taskSubmitted, taskAborted, taskDone
        count = 1;

        // task2: taskStarting?
        event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (TaskEvent.Type.taskStarting.equals(event.type)) {
            count++;
            if (event.future != future2)
                throw new Exception("task2/taskStarting: Future does not match: " + event + " vs " + future2);
            if (event.execSvc != xsvcNoContext)
                throw new Exception("task2/taskStarting: Wrong executor: " + event);
            if (event.task != callable2)
                throw new Exception("task2/taskStarting: Wrong task: " + event);

            event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // task2: taskAborted? (might or might not occur depending on timing of interrupt vs cancel)
        if (TaskEvent.Type.taskAborted.equals(event.type)) {
            count++;
            if (event.future != future2)
                throw new Exception("task2/taskAborted: Wrong future in " + event + ". Should be: " + future2);
            if (event.execSvc != xsvcNoContext)
                throw new Exception("task2/taskAborted: Wrong executor: " + event);
            if (event.task != callable2)
                throw new Exception("task2/taskAborted: Wrong task: " + event);
            if (!(event.exception instanceof CancellationException))
                throw new Exception("task2/taskAborted: CancellationException expected for: " + event).initCause(event.exception);
            if (!(event.failureFromFutureGet instanceof CancellationException))
                throw new Exception("task2/taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);
            if (!future2.isCancelled())
                throw new Exception("Future for task2 should be canceled after timeout.");

            event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        if (count <= 1)
            throw new Exception("task2: Did not see taskStarting or taskAborted. Instead: " + event);

        // task2: taskDone
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("task2: Unexpected third/fourth event: " + event);
        if (event.future != future2)
            throw new Exception("task2/taskDone: Wrong future in " + event + ". Should be: " + future2);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("task2/taskDone: Wrong executor: " + event);
        if (event.task != callable2)
            throw new Exception("task2/taskDone: Wrong task: " + event);
        if (event.exception != null && !(event.exception instanceof InterruptedException))
            throw new Exception("task2/taskDone: Unexpected exception for: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException)
            && (!(event.failureFromFutureGet instanceof ExecutionException) || !(event.failureFromFutureGet.getCause() instanceof InterruptedException)))
            throw new Exception("task2/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!listener2.events.isEmpty())
            throw new Exception("task2: Unexpected events: " + listener2.events);
    }

    /**
     * Tests invokeAll(tasks) where a task is canceled upon submit.
     */
    @Test
    public void testListenerInvokeAllCanceledOnSubmit() throws Throwable {
        CounterTask task = new CounterTask();
        TaskListener listener = new TaskListener(true);
        listener.whenToCancel.get(TaskEvent.Type.taskSubmitted).add(CancelType.mayNotInterruptIfRunning);
        Callable<Integer> managedTask = ManagedExecutors.managedTask((Callable<Integer>) task, listener);

        List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
        tasks.add(new CounterTask());
        tasks.add(managedTask);
        tasks.add(new CounterTask());

        try {
            List<Future<Integer>> futures = xsvcNoContext.invokeAll(tasks);
            throw new Exception("invokeAll should be rejected when one of the tasks is canceled on submit. Instead: " + futures);
        } catch (RejectedExecutionException x) {
        } // pass

        // taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("Unexpected first event: " + event);
        if (event.future == null)
            throw new Exception("taskSubmitted: Missing future in " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("taskSubmitted: Wrong executor: " + event);
        if (event.task != managedTask)
            throw new Exception("taskSubmitted: Wrong task: " + event);

        Future<?> future = event.future;
        if (!future.isCancelled())
            throw new Exception("Task should be canceled. " + future);

        if (!future.isDone())
            throw new Exception("Task should be done. " + future);

        try {
            Object result = event.future.get();
            throw new Exception("Future should raise CancellationException. Instead: " + result);
        } catch (CancellationException x) {
        } // pass

        // taskAborted
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("taskAborted: Wrong future: " + event + " vs " + future);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("taskAborted: Wrong executor: " + event);
        if (event.task != managedTask)
            throw new Exception("taskAborted: Wrong task: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("taskAborted: should have CancellationException on " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        // taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("taskDone: Wrong future: " + event + " vs " + future);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("taskDone: Wrong executor: " + event);
        if (event.task != managedTask)
            throw new Exception("taskDone: Wrong task: " + event);
        if (event.exception != null && !(event.exception instanceof IllegalStateException))
            throw new Exception("taskDone: Non-null exception: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!listener.events.isEmpty())
            throw new Exception("Unexpected events: " + listener.events);
    }

    /**
     * Tests invokeAll(tasks, timeout, timeunit) method with a managed task listener.
     */
    @Test
    public void testListenerInvokeAllWithTimeout() throws Throwable {

        TaskListener listener1 = new TaskListener(true);
        Callable<Integer> callable1 = new CounterTask();
        callable1 = ManagedExecutors.managedTask(callable1, listener1);

        TaskListener listener2 = new TaskListener(true);
        Callable<Integer> callable2 = new CounterTask();
        callable2 = ManagedExecutors.managedTask(callable2, listener2);

        List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
        tasks.add(callable1);
        tasks.add(new CounterTask()); // This one doesn't have a listener
        tasks.add(callable2);
        List<Future<Integer>> futures = xsvcDefault.invokeAll(tasks, TIMEOUT, TimeUnit.MILLISECONDS);

        Future<Integer> future1 = futures.get(0);
        Future<Integer> future2 = futures.get(2);

        // invokeAll/task1: taskSubmitted
        TaskEvent event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("invokeAll(timeout)/task1: Unexpected first event: " + event);
        if (event.future != future1)
            throw new Exception("invokeAll(timeout)/task1/taskSubmitted: Wrong future: " + event + " vs " + future1);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAll(timeout)/task1/taskSubmitted: Wrong executor: " + event);
        if (event.task != callable1)
            throw new Exception("invokeAll(timeout)/task1/taskSubmitted: Wrong task: " + event);

        if (event.future.isCancelled())
            throw new Exception("invokeAll(timeout)/task1: Task should not be canceled. " + event.future);

        if (!event.future.isDone())
            throw new Exception("invokeAll(timeout)/task1: Task should be done. " + event.future);

        Integer result1 = (Integer) event.future.get();
        if (result1 != 1)
            throw new Exception("invokeAll(timeout)/task1: Unexpected result: " + result1);

        // invokeAll/task1: taskStarting
        event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("invokeAll(timeout)/task1: Unexpected second event: " + event);
        if (event.future != future1)
            throw new Exception("invokeAll(timeout)/task1/taskStarting: Wrong future: " + event + " vs " + future1);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAll(timeout)/task1/taskStarting: Wrong executor: " + event);
        if (event.task != callable1)
            throw new Exception("invokeAll(timeout)/task1/taskStarting: Wrong task: " + event);

        // invokeAll/task1: taskDone
        event = listener1.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("invokeAll(timeout)/task1: Unexpected third event: " + event);
        if (event.future != future1)
            throw new Exception("invokeAll(timeout)/task1/taskDone: Wrong future: " + event + " vs " + future1);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAll(timeout)/task1/taskDone: Wrong executor: " + event);
        if (event.task != callable1)
            throw new Exception("invokeAll(timeout)/task1/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("invokeAll(timeout)/task1/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("invokeAll(timeout)/task1/taskDone: Future.get raised unexpected exception during " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("invokeAll(timeout)/task1/taskDone: Unexpected future result during: " + event);

        if (!listener1.events.isEmpty())
            throw new Exception("invokeAll(timeout)/task1: Unexpected events: " + listener1.events);

        // invokeAll/task2: taskSubmitted
        event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("invokeAll(timeout)/task2: Unexpected first event: " + event);
        if (event.future != future2)
            throw new Exception("invokeAll(timeout)/task2/taskSubmitted: Wrong future: " + event + " vs " + future2);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAll(timeout)/task2/taskSubmitted: Wrong executor: " + event);
        if (event.task != callable2)
            throw new Exception("invokeAll(timeout)/task2/taskSubmitted: Wrong task: " + event);

        if (event.future.isCancelled())
            throw new Exception("invokeAll(timeout)/task2: Task should not be canceled. " + event.future);

        if (!event.future.isDone())
            throw new Exception("invokeAll(timeout)/task2: Task should be done. " + event.future);

        Integer result2 = (Integer) event.future.get();
        if (result2 != 1)
            throw new Exception("invokeAll(timeout)/task2: Unexpected result: " + result2);

        // invokeAll/task2: taskStarting
        event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("invokeAll(timeout)/task2: Unexpected second event: " + event);
        if (event.future != future2)
            throw new Exception("invokeAll(timeout)/task2/taskStarting: Wrong future: " + event + " vs " + future2);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAll(timeout)/task2/taskStarting: Wrong executor: " + event);
        if (event.task != callable2)
            throw new Exception("invokeAll(timeout)/task2/taskStarting: Wrong task: " + event);

        // invokeAll/task2: taskDone
        event = listener2.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("invokeAll(timeout)/task2: Unexpected third event: " + event);
        if (event.future != future2)
            throw new Exception("invokeAll(timeout)/task2/taskDone: Wrong future: " + event + " vs " + future2);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAll(timeout)/task2/taskDone: Wrong executor: " + event);
        if (event.task != callable2)
            throw new Exception("invokeAll(timeout)/task2/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("invokeAll(timeout)/task2/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("invokeAll(timeout)/task2/taskDone: Future.get raised unexpected exception during " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("invokeAll(timeout)/task2/taskDone: Unexpected future result during: " + event);

        if (!listener2.events.isEmpty())
            throw new Exception("invokeAll(timeout)/task2: Unexpected events: " + listener2.events);
    }

    /**
     * Tests invokeAny method with a managed task listener.
     */
    @Test
    public void testListenerInvokeAny() throws Throwable {
        TaskListener[] listeners = new TaskListener[3];

        listeners[1] = new TaskListener(true);
        Callable<Integer> callable1 = new CounterTask(new AtomicInteger(0));
        callable1 = ManagedExecutors.managedTask(callable1, listeners[1]);

        listeners[2] = new TaskListener(true);
        Callable<Integer> callable2 = new CounterTask(new AtomicInteger(1));
        callable2 = ManagedExecutors.managedTask(callable2, listeners[2]);

        Callable<?>[] callables = new Callable<?>[] { null, callable1, callable2 };

        List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
        tasks.add(callable1);
        tasks.add(callable2);
        Integer result = xsvcDefault.invokeAny(tasks);

        // With invokeAny, at least one of the tasks should complete. We can identify which one by the result.

        // invokeAny/task[successful]: taskSubmitted
        TaskEvent event = listeners[result].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("invokeAny/task" + result + ": Unexpected first event: " + event);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task" + result + "/taskSubmitted: Wrong executor: " + event);
        if (event.task != callables[result])
            throw new Exception("invokeAny/task" + result + "/taskSubmitted: Wrong task: " + event);

        Future<?> future = event.future;
        if (future.isCancelled())
            throw new Exception("invokeAny/task" + result + ": Task should not be canceled. " + future);

        if (!future.isDone())
            throw new Exception("invokeAny/task" + result + ": Task should be done. " + future);

        Integer resultOfFuture = (Integer) future.get();
        if (resultOfFuture != result)
            throw new Exception("invokeAny/task" + result + ": Unexpected result: " + resultOfFuture);

        // invokeAny/task[successful]: taskStarting
        event = listeners[result].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("invokeAny/task" + result + ": Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny/task" + result + "/taskStarting: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task" + result + "/taskStarting: Wrong executor: " + event);
        if (event.task != callables[result])
            throw new Exception("invokeAny/task" + result + "/taskStarting: Wrong task: " + event);

        // invokeAll/task[successful]: taskDone
        event = listeners[result].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("invokeAny/task" + result + ": Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny/task" + result + "/taskDone: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny/task" + result + "/taskDone: Wrong executor: " + event);
        if (event.task != callables[result])
            throw new Exception("invokeAny/task" + result + "/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("invokeAny/task" + result + "/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("invokeAny/task" + result + "/taskDone: Future.get raised unexpected exception during " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(result).equals(event.result))
            throw new Exception("invokeAny/task" + result + "/taskDone: Unexpected future result during: " + event);

        if (!listeners[result].events.isEmpty())
            throw new Exception("invokeAny/task" + result + ": Unexpected events: " + listeners[result].events);

        // The other task might have completed or been canceled at any point.
        int other = 3 - result;

        // invokeAny/task[other]: taskSubmitted
        if (!listeners[other].events.isEmpty()) {
            event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskSubmitted.equals(event.type))
                throw new Exception("invokeAny/othertask" + other + ": Unexpected first event: " + event);
            if (event.execSvc != xsvcDefault)
                throw new Exception("invokeAny/othertask" + other + "/taskSubmitted: Wrong executor: " + event);
            if (event.task != callables[other])
                throw new Exception("invokeAny/othertask" + other + "/taskSubmitted: Wrong task: " + event);

            Future<?> otherFuture = event.future;
            try {
                // Wait for future to complete
                for (long amountSlept = 0; !otherFuture.isDone() && amountSlept < TIMEOUT; amountSlept += POLL_INTERVAL)
                    Thread.sleep(POLL_INTERVAL);
                Object otherResult = otherFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
                // The other task completed
                if (!otherResult.equals(other))
                    throw new Exception("Expecting successful other task to have result of " + other + " not " + otherResult);

                if (otherFuture.isCancelled())
                    throw new Exception("invokeAny/otherTask" + other + ": Task should not be canceled. " + otherFuture);

                // invokeAny/otherSuccessfulTask: taskStarting
                event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!TaskEvent.Type.taskStarting.equals(event.type))
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + ": Unexpected second event: " + event);
                if (event.future != otherFuture)
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + "/taskStarting: Future does not match: " + event + " vs " + otherFuture);
                if (event.execSvc != xsvcDefault)
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + "/taskStarting: Wrong executor: " + event);
                if (event.task != callables[other])
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + "/taskStarting: Wrong task: " + event);

                // invokeAny/otherSuccessfulTask: taskDone
                event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!TaskEvent.Type.taskDone.equals(event.type))
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + ": Unexpected third event: " + event);
                if (event.future != otherFuture)
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + "/taskDone: Future does not match: " + event + " vs " + otherFuture);
                if (event.execSvc != xsvcDefault)
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + "/taskDone: Wrong executor: " + event);
                if (event.task != callables[other])
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + "/taskDone: Wrong task: " + event);
                if (event.exception != null)
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + "/taskDone: Non-null exception: " + event).initCause(event.exception);
                if (event.failureFromFutureGet != null)
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + "/taskDone: Future.get raised unexpected exception during " + event, event.failureFromFutureGet);
                if (!Integer.valueOf(other).equals(event.result))
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + "/taskDone: Unexpected future result during: " + event);

                if (!listeners[other].events.isEmpty())
                    throw new Exception("invokeAny/otherSuccessfulTask" + other + ": Unexpected events: " + listeners[result].events);

            } catch (CancellationException x) {
                // The other task was cancelled...at some point
                event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);

                // invokeAll/otherCanceledTask: taskStarting?
                if (TaskEvent.Type.taskStarting.equals(event.type)) {
                    if (event.future != otherFuture)
                        throw new Exception("invokeAny/otherStartedCanceledTask" + other + "/taskStarting: Future does not match: " + event + " vs " + otherFuture);
                    if (event.execSvc != xsvcDefault)
                        throw new Exception("invokeAny/otherStartedCanceledTask" + other + "/taskStarting: Wrong executor: " + event);
                    if (event.task != callables[other])
                        throw new Exception("invokeAny/otherStartedCanceledTask" + other + "/taskStarting: Wrong task: " + event);

                    event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                }

                // invokeAll/otherCanceledTask: taskAborted?
                if (TaskEvent.Type.taskAborted.equals(event.type)) {
                    if (event.future != otherFuture)
                        throw new Exception("invokeAny/otherCanceledTask" + other + "/taskAborted: Future does not match: " + event + " vs " + otherFuture);
                    if (event.execSvc != xsvcDefault)
                        throw new Exception("invokeAny/otherCanceledTask" + other + "/taskAborted: Wrong executor: " + event);
                    if (event.task != callables[other])
                        throw new Exception("invokeAny/otherCanceledTask" + other + "/taskAborted: Wrong task: " + event);
                    if (!(event.exception instanceof CancellationException))
                        throw new Exception("invokeAny/otherCanceledTask" + other + "/taskAborted: Wrong exception: " + event).initCause(event.exception);
                    if (!(event.failureFromFutureGet instanceof CancellationException))
                        throw new Exception("invokeAny/otherCanceledTask" + other + "/taskAborted: Future.get did not raise expected exception for "
                                            + event, event.failureFromFutureGet);

                    event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                }

                // invokeAny/otherCanceledTask: taskDone
                if (!TaskEvent.Type.taskDone.equals(event.type))
                    throw new Exception("invokeAny/otherCanceledTask" + other + ": Unexpected event: " + event);
                if (event.future != otherFuture)
                    throw new Exception("invokeAny/otherCanceledTask" + other + "/taskDone: Future does not match: " + event + " vs " + otherFuture);
                if (event.execSvc != xsvcDefault)
                    throw new Exception("invokeAny/otherCanceledTask" + other + "/taskDone: Wrong executor: " + event);
                if (event.task != callables[other])
                    throw new Exception("invokeAny/otherCanceledTask" + other + "/taskDone: Wrong task: " + event);
                if (event.exception != null && !(event.exception instanceof IllegalStateException))
                    throw new Exception("invokeAny/otherCanceledTask" + other + "/taskDone: Unexpected exception for: " + event).initCause(event.exception);
                if (!(event.failureFromFutureGet instanceof CancellationException))
                    throw new Exception("invokeAny/otherCanceledTask" + other + "/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

                if (!listeners[other].events.isEmpty())
                    throw new Exception("invokeAny/otherCanceledTask" + other + ": Unexpected events: " + listeners[result].events);
            }

            if (!otherFuture.isDone())
                throw new Exception("invokeAny/otherTask" + other + ": Task should be done. " + otherFuture);
        }
    }

    /**
     * Tests invokeAny(tasks, timeout, timeunit) method with a managed task listener, where the tasks all time out.
     */
    @Test
    public void testListenerInvokeAnyTimeout() throws Throwable {

        AtomicInteger numStarted = new AtomicInteger();
        AtomicInteger interruptions = new AtomicInteger();
        SlowTask slowTask = new SlowTask(numStarted, interruptions, TIMEOUT * 5);
        TaskListener taskListener = new TaskListener(true);
        Callable<Long> slowManagedTask = ManagedExecutors.managedTask((Callable<Long>) slowTask, taskListener);
        try {
            long l = mxsvcNoContext.invokeAny(Collections.singleton(slowManagedTask), 500, TimeUnit.MILLISECONDS);
            throw new Exception("invokeAny must be rejected when no tasks complete before the timeout. Instead: " + l);
        } catch (TimeoutException x) {
        }
        // All tasks (if they started) should be canceled in response to the timeout.
        // It seems like cancel might happen AFTER the invokeAny method returns, so we will poll for it
        long start = System.currentTimeMillis();
        for (long time = start; time < start + TIMEOUT && interruptions.get() < numStarted.get(); time = System.currentTimeMillis())
            Thread.sleep(POLL_INTERVAL);

        int started = numStarted.get();
        int interrupted = interruptions.get();
        if (interrupted != started)
            throw new Exception("All started tasks (" + started + ") should have been canceled due to timeout. Instead: " + interrupted);

        // invokeAny/slowTask: taskSubmitted
        TaskEvent event = taskListener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("invokeAny(timeout)/slowTask: Unexpected first event: " + event);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("invokeAny(timeout)/slowTask/taskSubmitted: Wrong executor: " + event);
        if (event.task != slowManagedTask)
            throw new Exception("invokeAny(timeout)/slowTask/taskSubmitted: Wrong task: " + event + ". Correct task is: " + slowManagedTask);

        Future<?> slowFuture = event.future;
        try {
            Object slowResult = slowFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("invokeAny(timeout)/slowTask/taskSubmitted: future should time out, not have result: " + slowResult);
        } catch (CancellationException x) {
            if (!slowFuture.isCancelled())
                throw new Exception("invokeAny(timeout)/slowTask/taskSubmitted: future should be canceled: " + slowFuture);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        if (!slowFuture.isDone())
            throw new Exception("invokeAny(timeout)/slowTask/taskSubmitted: future should be done: " + slowFuture);

        // invokeAny/slowTask: taskStarting
        event = taskListener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (TaskEvent.Type.taskStarting.equals(event.type)) {
            if (event.future != slowFuture)
                throw new Exception("invokeAny(timeout)/slowTask/taskStarting: Future does not match: " + event + " vs " + slowFuture);
            if (event.execSvc != mxsvcNoContext)
                throw new Exception("invokeAny(timeout)/slowTask/taskStarting: Wrong executor: " + event);
            if (event.task != slowManagedTask)
                throw new Exception("invokeAny(timeout)/slowTask/taskStarting: Wrong task: " + event + ". Correct task is: " + slowManagedTask);

            event = taskListener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // invokeAny/slowTask: taskAborted - taskAborted with CancellationException will only happen if the task is canceled vs interrupted
        if (TaskEvent.Type.taskAborted.equals(event.type)) {
            if (event.future != slowFuture)
                throw new Exception("invokeAny(timeout)/slowTask/taskAborted: Future does not match: " + event + " vs " + slowFuture);
            if (event.execSvc != mxsvcNoContext)
                throw new Exception("invokeAny(timeout)/slowTask/taskAborted: Wrong executor: " + event);
            if (event.task != slowManagedTask)
                throw new Exception("invokeAny(timeout)/slowTask/taskAborted: Wrong task: " + event + ". Correct task is: " + slowManagedTask);
            if (!(event.exception instanceof CancellationException))
                throw new Exception("invokeAny(timeout)/slowTask/taskAborted: Wrong exception in " + event).initCause(event.exception);
            if (!(event.failureFromFutureGet instanceof CancellationException))
                throw new Exception("invokeAny(timeout)/slowTask/taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

            event = taskListener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // invokeAny/slowTask: taskDone
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("invokeAny(timeout)/slowTask: Unexpected last event: " + event);
        if (event.future != slowFuture)
            throw new Exception("invokeAny(timeout)/slowTask/taskDone: Future does not match: " + event + " vs " + slowFuture);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("invokeAny(timeout)/slowTask/taskDone: Wrong executor: " + event);
        if (event.task != slowManagedTask)
            throw new Exception("invokeAny(timeout)/slowTask/taskDone: Wrong task: " + event + ". Correct task is: " + slowManagedTask);
        if (!(event.exception instanceof InterruptedException))
            throw new Exception("invokeAny(timeout)/slowTask/taskDone: Expecting InterruptedException for " + event + ". Instead, see cause").initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException)
            && (!(event.failureFromFutureGet instanceof ExecutionException) || !(event.failureFromFutureGet.getCause() instanceof InterruptedException)))
            throw new Exception("invokeAny(timeout)/slowTask/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!taskListener.events.isEmpty())
            throw new Exception("invokeAny(timeout)/slowTask: Unexpected events: " + taskListener.events);
    }

    /**
     * Tests invokeAny(tasks, timeout, timeunit) method with a managed task listener.
     */
    @Test
    public void testListenerInvokeAnyWithTimeout() throws Throwable {
        TaskListener[] listeners = new TaskListener[3];

        listeners[1] = new TaskListener(true);
        Callable<Integer> callable1 = new CounterTask(new AtomicInteger(0));
        callable1 = ManagedExecutors.managedTask(callable1, listeners[1]);

        listeners[2] = new TaskListener(true);
        Callable<Integer> callable2 = new CounterTask(new AtomicInteger(1));
        callable2 = ManagedExecutors.managedTask(callable2, listeners[2]);

        Callable<?>[] callables = new Callable<?>[] { null, callable1, callable2 };

        List<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
        tasks.add(callable1);
        tasks.add(callable2);
        Integer result = xsvcDefault.invokeAny(tasks, TIMEOUT, TimeUnit.MILLISECONDS);

        // With invokeAny, at least one of the tasks should complete. We can identify which one by the result.

        // invokeAny/task[successful]: taskSubmitted
        TaskEvent event = listeners[result].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("invokeAny(timeout)/task" + result + ": Unexpected first event: " + event);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny(timeout)/task" + result + "/taskSubmitted: Wrong executor: " + event);
        if (event.task != callables[result])
            throw new Exception("invokeAny(timeout)/task" + result + "/taskSubmitted: Wrong task: " + event);

        Future<?> future = event.future;
        if (future.isCancelled())
            throw new Exception("invokeAny(timeout)/task" + result + ": Task should not be canceled. " + future);

        if (!future.isDone())
            throw new Exception("invokeAny(timeout)/task" + result + ": Task should be done. " + future);

        Integer resultOfFuture = (Integer) future.get();
        if (resultOfFuture != result)
            throw new Exception("invokeAny(timeout)/task" + result + ": Unexpected result: " + resultOfFuture);

        // invokeAny/task[successful]: taskStarting
        event = listeners[result].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("invokeAny(timeout)/task" + result + ": Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny(timeout)/task" + result + "/taskStarting: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny(timeout)/task" + result + "/taskStarting: Wrong executor: " + event);
        if (event.task != callables[result])
            throw new Exception("invokeAny(timeout)/task" + result + "/taskStarting: Wrong task: " + event);

        // invokeAll/task[successful]: taskDone
        event = listeners[result].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("invokeAny(timeout)/task" + result + ": Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("invokeAny(timeout)/task" + result + "/taskDone: Future does not match: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("invokeAny(timeout)/task" + result + "/taskDone: Wrong executor: " + event);
        if (event.task != callables[result])
            throw new Exception("invokeAny(timeout)/task" + result + "/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("invokeAny(timeout)/task" + result + "/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("invokeAny(timeout)/task" + result + "/taskDone: Future.get raised unexpected exception during " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(result).equals(event.result))
            throw new Exception("invokeAny(timeout)/task" + result + "/taskDone: Unexpected future result during: " + event);

        if (!listeners[result].events.isEmpty())
            throw new Exception("invokeAny(timeout)/task" + result + ": Unexpected events: " + listeners[result].events);

        // The other task might have completed or been canceled at any point.
        int other = 3 - result;

        // invokeAny/task[other]: taskSubmitted
        if (!listeners[other].events.isEmpty()) {
            event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskSubmitted.equals(event.type))
                throw new Exception("invokeAny(timeout)/othertask" + other + ": Unexpected first event: " + event);
            if (event.execSvc != xsvcDefault)
                throw new Exception("invokeAny(timeout)/othertask" + other + "/taskSubmitted: Wrong executor: " + event);
            if (event.task != callables[other])
                throw new Exception("invokeAny(timeout)/othertask" + other + "/taskSubmitted: Wrong task: " + event);

            Future<?> otherFuture = event.future;
            try {
                // Wait for future to complete
                for (long amountSlept = 0; !otherFuture.isDone() && amountSlept < TIMEOUT; amountSlept += POLL_INTERVAL)
                    Thread.sleep(POLL_INTERVAL);
                Object otherResult = otherFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
                // The other task completed
                if (!otherResult.equals(other))
                    throw new Exception("Expecting successful other task to have result of " + other + " not " + otherResult);

                if (otherFuture.isCancelled())
                    throw new Exception("invokeAny(timeout)/otherTask" + other + ": Task should not be canceled. " + otherFuture);

                // invokeAny/otherSuccessfulTask: taskStarting
                event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!TaskEvent.Type.taskStarting.equals(event.type))
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + ": Unexpected second event: " + event);
                if (event.future != otherFuture)
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + "/taskStarting: Future does not match: " + event + " vs " + otherFuture);
                if (event.execSvc != xsvcDefault)
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + "/taskStarting: Wrong executor: " + event);
                if (event.task != callables[other])
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + "/taskStarting: Wrong task: " + event);

                // invokeAny/otherSuccessfulTask: taskDone
                event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!TaskEvent.Type.taskDone.equals(event.type))
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + ": Unexpected third event: " + event);
                if (event.future != otherFuture)
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + "/taskDone: Future does not match: " + event + " vs " + otherFuture);
                if (event.execSvc != xsvcDefault)
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + "/taskDone: Wrong executor: " + event);
                if (event.task != callables[other])
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + "/taskDone: Wrong task: " + event);
                if (event.exception != null)
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + "/taskDone: Non-null exception: " + event).initCause(event.exception);
                if (event.failureFromFutureGet != null)
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + "/taskDone: Future.get raised unexpected exception during "
                                        + event, event.failureFromFutureGet);
                if (!Integer.valueOf(other).equals(event.result))
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + "/taskDone: Unexpected future result during: " + event);

                if (!listeners[other].events.isEmpty())
                    throw new Exception("invokeAny(timeout)/otherSuccessfulTask" + other + ": Unexpected events: " + listeners[other].events);

            } catch (CancellationException x) {
                // The other task was cancelled...at some point
                event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);

                // invokeAll/otherCanceledTask: taskStarting?
                if (TaskEvent.Type.taskStarting.equals(event.type)) {
                    if (event.future != otherFuture)
                        throw new Exception("invokeAny(timeout)/otherStartedCanceledTask" + other + "/taskStarting: Future does not match: " + event + " vs " + otherFuture);
                    if (event.execSvc != xsvcDefault)
                        throw new Exception("invokeAny(timeout)/otherStartedCanceledTask" + other + "/taskStarting: Wrong executor: " + event);
                    if (event.task != callables[other])
                        throw new Exception("invokeAny(timeout)/otherStartedCanceledTask" + other + "/taskStarting: Wrong task: " + event);

                    event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                }

                // invokeAll/otherCanceledTask: taskAborted?
                if (TaskEvent.Type.taskAborted.equals(event.type)) {
                    if (event.future != otherFuture)
                        throw new Exception("invokeAny(timeout)/otherCanceledTask" + other + "/taskAborted: Future does not match: " + event + " vs " + otherFuture);
                    if (event.execSvc != xsvcDefault)
                        throw new Exception("invokeAny(timeout)/otherCanceledTask" + other + "/taskAborted: Wrong executor: " + event);
                    if (event.task != callables[other])
                        throw new Exception("invokeAny(timeout)/otherCanceledTask" + other + "/taskAborted: Wrong task: " + event);
                    if (!(event.exception instanceof CancellationException))
                        throw new Exception("invokeAny(timeout)/otherCanceledTask" + other + "/taskAborted: Wrong exception: " + event).initCause(event.exception);
                    if (!(event.failureFromFutureGet instanceof CancellationException))
                        throw new Exception("invokeAny(timeout)/otherCanceledTask" + other + "/taskAborted: Future.get did not raise expected exception for "
                                            + event, event.failureFromFutureGet);

                    event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                }

                // invokeAny/otherCanceledTask: taskDone
                if (!TaskEvent.Type.taskDone.equals(event.type))
                    throw new Exception("invokeAny(timeout)/otherCanceledTask" + other + ": Unexpected event: " + event);
                if (event.future != otherFuture)
                    throw new Exception("invokeAny(timeout)/otherCanceledTask" + other + "/taskDone: Future does not match: " + event + " vs " + otherFuture);
                if (event.execSvc != xsvcDefault)
                    throw new Exception("invokeAny(timeout)/otherCanceledTask" + other + "/taskDone: Wrong executor: " + event);
                if (event.task != callables[other])
                    throw new Exception("invokeAny(timeout)/otherCanceledTask" + other + "/taskDone: Wrong task: " + event);
                if (!(event.failureFromFutureGet instanceof CancellationException))
                    throw new Exception("invokeAny(timeout)/otherCanceledTask" + other + "/taskDone: Future.get did not raise expected exception for "
                                        + event, event.failureFromFutureGet);

                if (!listeners[other].events.isEmpty())
                    throw new Exception("invokeAny(timeout)/otherCanceledTask" + other + ": Unexpected events: " + listeners[other].events);
            } catch (ExecutionException x) {
                if (!(x.getCause() instanceof InterruptedException))
                    throw x;

                // The other task was interrupted
                event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);

                // invokeAll/otherInterruptedTask: taskStarting
                if (!TaskEvent.Type.taskStarting.equals(event.type))
                    throw new Exception("invokeAny(timeout)/otherInterruptedTask" + other + ": unexpected second event: " + event);
                if (event.future != otherFuture)
                    throw new Exception("invokeAny(timeout)/otherInterruptedTask" + other + "/taskStarting: Future does not match: " + event + " vs " + otherFuture);
                if (event.execSvc != xsvcDefault)
                    throw new Exception("invokeAny(timeout)/otherInterruptedTask" + other + "/taskStarting: Wrong executor: " + event);
                if (event.task != callables[other])
                    throw new Exception("invokeAny(timeout)/otherInterruptedTask" + other + "/taskStarting: Wrong task: " + event);

                // invokeAny/otherInterruptedTask: taskDone
                event = listeners[other].events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (!TaskEvent.Type.taskDone.equals(event.type))
                    throw new Exception("invokeAny(timeout)/otherInterruptedTask" + other + ": Unexpected third event: " + event);
                if (event.future != otherFuture)
                    throw new Exception("invokeAny(timeout)/otherInterruptedTask" + other + "/taskDone: Future does not match: " + event + " vs " + otherFuture);
                if (event.execSvc != xsvcDefault)
                    throw new Exception("invokeAny(timeout)/otherInterruptedTask" + other + "/taskDone: Wrong executor: " + event);
                if (event.task != callables[other])
                    throw new Exception("invokeAny(timeout)/otherInterruptedTask" + other + "/taskDone: Wrong task: " + event);
                if (!(event.exception instanceof InterruptedException))
                    throw new Exception("invokeAny(timeout)/otherInterruptedTask" + other + "/taskDone: Unexpected or missing exception: " + event, event.exception);
                if (!(event.failureFromFutureGet instanceof ExecutionException) || !(event.failureFromFutureGet.getCause() instanceof InterruptedException))
                    throw new Exception("invokeAny(timeout)/otherInterruptedTask" + other + "/taskDone: Future.get did not raise expected exception for "
                                        + event, event.failureFromFutureGet);

                if (!listeners[other].events.isEmpty())
                    throw new Exception("invokeAny(timeout)/otherInterruptedTask" + other + ": Unexpected events: " + listeners[other].events);
            }

            if (!otherFuture.isDone())
                throw new Exception("invokeAny(timeout)/otherTask" + other + ": Task should be done. " + otherFuture);
        }
    }

    /**
     * Schedule one-time tasks with a managed task listener.
     */
    @Test
    public void testListenerOneTimeTasks() throws Throwable {

        // ---------------- schedule(runnable, delay) ---------------
        TaskListener listener = new TaskListener(true);
        Runnable runnable = new CounterTask();
        runnable = ManagedExecutors.managedTask(runnable, listener);
        ScheduledFuture<?> future = mschedxsvcClassloaderContext.schedule(runnable, 12, TimeUnit.MICROSECONDS);

        // schedule(runnable, delay): taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(runnable, delay): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, delay)/taskSubmitted: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, delay)/taskSubmitted: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("schedule(runnable, delay)/taskSubmitted: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, delay)/taskSubmitted: Unexpected delay: " + event);

        // schedule(runnable, delay): taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(runnable, delay): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, delay)/taskStarting: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, delay)/taskStarting: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("schedule(runnable, delay)/taskStarting: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, delay)/taskStarting: Unexpected delay: " + event);

        // schedule(runnable, delay): taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(runnable, delay): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, delay)/taskDone: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, delay)/taskDone: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("schedule(runnable, delay)/taskDone: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, delay)/taskDone: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(runnable, delay)/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(runnable, delay)/taskDone: Future.get should have been successful for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("schedule(runnable, delay)/taskDone: Unexpected result for schedule(runnable, delay): " + event);

        if (event.future.isCancelled())
            throw new Exception("schedule(runnable, delay): Task should not be canceled. " + event.future);

        if (!event.future.isDone())
            throw new Exception("schedule(runnable, delay): Task should be done. " + event.future);

        Object result = event.future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result != null)
            throw new Exception("schedule(runnable, delay): Unexpected result: " + result);

        if (!listener.events.isEmpty())
            throw new Exception("schedule(runnable, delay): Unexpected events: " + listener.events);

        // ---------------- schedule(callable, delay) ---------------
        listener = new TaskListener(true);
        Callable<Integer> callable = new CounterTask();
        callable = ManagedExecutors.managedTask(callable, listener);
        ScheduledFuture<?> scheduledFuture = mschedxsvcClassloaderContext.schedule(callable, 14, TimeUnit.NANOSECONDS);

        // schedule(callable, delay): taskSubmitted
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable, delay): Unexpected first event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable, delay)/taskSubmitted: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, delay)/taskSubmitted: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("schedule(callable, delay)/taskSubmitted: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, delay)/taskSubmitted: Unexpected delay: " + event);

        // schedule(callable, delay): taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable, delay): Unexpected second event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable, delay)/taskStarting: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, delay)/taskStarting: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("schedule(callable, delay)/taskStarting: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, delay)/taskStarting: Unexpected delay: " + event);

        // schedule(callable, delay): taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable, delay): Unexpected third event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable, delay)/taskDone: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, delay)/taskDone: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("schedule(callable, delay)/taskDone: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, delay)/taskDone: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable, delay)/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable, delay)/taskDone: Future.get should have been successful for " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("schedule(callable, delay)/taskDone: Unexpected result for schedule(callable, delay): " + event);

        if (event.future.isCancelled())
            throw new Exception("schedule(callable, delay): Task should not be canceled. " + event.future);

        if (!event.future.isDone())
            throw new Exception("schedule(callable, delay): Task should be done. " + event.future);

        result = event.future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("schedule(callable, delay): Unexpected result: " + result);

        if (!listener.events.isEmpty())
            throw new Exception("schedule(callable, delay): Unexpected events: " + listener.events);
    }

    /**
     * Schedule tasks to repeat at a fixed rate and notify a managed task listener.
     */
    @Test
    public void testListenerRepeatAtFixedRate() throws Throwable {

        // Schedule a runnable that runs exactly 3 times as quickly as possible and then cancels itself
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskSubmitted).add(1l);
        listener.whenToGet.get(TaskEvent.Type.taskSubmitted).add(Long.MAX_VALUE);
        listener.whenToGet.get(TaskEvent.Type.taskStarting).add(1l);
        listener.whenToGet.get(TaskEvent.Type.taskStarting).add(Long.MAX_VALUE);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToCancel.get(TaskEvent.Type.taskDone).add(CancelType.doNotCancel);
        listener.whenToCancel.get(TaskEvent.Type.taskDone).add(CancelType.doNotCancel);
        listener.whenToCancel.get(TaskEvent.Type.taskDone).add(CancelType.mayInterruptIfRunning);
        Runnable runnable = new CounterTask();
        runnable = ManagedExecutors.managedTask(runnable, listener);
        ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.MILLISECONDS);

        // scheduleAtFixedRate: taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Unexpected delay: " + event);
        if (!(event.failureFromFutureGet instanceof InterruptedException))
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Future.get(): missing or unexpected error: " + event, event.failureFromFutureGet);

        // scheduleAtFixedRate: taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Unexpected delay: " + event);
        if (!(event.failureFromFutureGet instanceof InterruptedException))
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Future.get(): missing or unexpected error: " + event, event.failureFromFutureGet);

        // scheduleAtFixedRate: taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Unexpected error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Unexpected result for scheduleAtFixedRate: " + event);

        // scheduleAtFixedRate: taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#2: Unexpected delay: " + event);
        if (!(event.failureFromFutureGet instanceof InterruptedException))
            throw new Exception("scheduleAtFixedRate/taskSubmitted#2: Future.get(): missing or unexpected error: " + event, event.failureFromFutureGet);

        // scheduleAtFixedRate: taskStarting #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskStarting#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskStarting#2: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskStarting#2: Unexpected delay: " + event);
        if (!(event.failureFromFutureGet instanceof InterruptedException))
            throw new Exception("scheduleAtFixedRate/taskStarting#2: Future.get(): missing or unexpected error: " + event, event.failureFromFutureGet);

        // scheduleAtFixedRate: taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskDone#2: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("scheduleAtFixedRate/taskDone#2: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("scheduleAtFixedRate/taskDone#2: Unexpected error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("scheduleAtFixedRate/taskDone#2: Unexpected result for scheduleAtFixedRate: " + event);

        // scheduleAtFixedRate: taskSubmitted #3
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected seventh event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#3: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#3: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#3: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#3: Unexpected delay: " + event);

        // scheduleAtFixedRate: taskStarting #3
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected eighth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskStarting#3: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskStarting#3: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskStarting#3: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskStarting#3: Unexpected delay: " + event);

        // scheduleAtFixedRate: taskDone #3
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected ninth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskDone#3: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskDone#3: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskDone#3: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskDone#3: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("scheduleAtFixedRate/taskDone#3: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("scheduleAtFixedRate/taskDone#3: Unexpected error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("scheduleAtFixedRate/taskDone#3: Unexpected result for scheduleAtFixedRate: " + event);

        if (!event.future.isDone())
            throw new Exception("scheduleAtFixedRate: Task should be done. " + event.future);

        if (!event.future.isCancelled())
            throw new Exception("scheduleAtFixedRate: Task should be canceled. " + event.future);

        try {
            Object result = event.future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("scheduleAtFixedRate: Unexpected result: " + result);
        } catch (CancellationException x) {
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        if (!listener.events.isEmpty())
            throw new Exception("scheduleAtFixedRate: Unexpected events: " + listener.events);
    }

    /**
     * Schedule tasks to repeat with a fixed delay and notify a managed task listener.
     */
    @Test
    public void testListenerRepeatWithFixedDelay() throws Throwable {

        // Schedule a runnable that runs exactly 3 times as quickly as possible and then cancels itself
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(Long.valueOf(POLL_INTERVAL));
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(Long.valueOf(POLL_INTERVAL));
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(Long.valueOf(POLL_INTERVAL));
        listener.whenToCancel.get(TaskEvent.Type.taskDone).add(CancelType.doNotCancel);
        listener.whenToCancel.get(TaskEvent.Type.taskDone).add(CancelType.doNotCancel);
        listener.whenToCancel.get(TaskEvent.Type.taskDone).add(CancelType.mayInterruptIfRunning);
        Runnable runnable = new CounterTask();
        runnable = ManagedExecutors.managedTask(runnable, listener);
        ScheduledFuture<?> future = mschedxsvcClassloaderContext.scheduleWithFixedDelay(runnable, 0, 2, TimeUnit.MILLISECONDS);

        // scheduleWithFixedDelay: taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 2)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskStarting#1: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Unexpected error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Unexpected result for scheduleWithFixedDelay: " + event);

        // scheduleWithFixedDelay: taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 2)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#2: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskStarting #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskStarting#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskStarting#2: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskStarting#2: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Unexpected error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#2: Unexpected result for scheduleWithFixedDelay: " + event);

        // scheduleWithFixedDelay: taskSubmitted #3
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected seventh event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#3: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#3: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#3: Wrong task: " + event);
        if (event.delay == null || event.delay > 2)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#3: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskStarting #3
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected eighth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskStarting#3: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskStarting#3: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskStarting#3: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskStarting#3: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskDone #3
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected ninth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskDone#3: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskDone#3: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskDone#3: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskDone#3: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#3: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#3: Unexpected error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#3: Unexpected result for scheduleWithFixedDelay: " + event);

        if (!event.future.isDone())
            throw new Exception("scheduleWithFixedDelay: Task should be done. " + event.future);

        if (!event.future.isCancelled())
            throw new Exception("scheduleWithFixedDelay: Task should be canceled. " + event.future);

        try {
            Object result = event.future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("scheduleWithFixedDelay: Unexpected result: " + result);
        } catch (CancellationException x) {
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        if (!listener.events.isEmpty())
            throw new Exception("scheduleWithFixedDelay: Unexpected events: " + listener.events);
    }

    /**
     * Tests schedule where a task is canceled upon starting.
     */
    @Test
    public void testListenerScheduleCanceledOnStarting() throws Throwable {
        TaskListener listener = new TaskListener(true);
        listener.whenToCancel.get(TaskEvent.Type.taskStarting).add(CancelType.mayInterruptIfRunning);
        Runnable runnable = new CounterTask();
        runnable = ManagedExecutors.managedTask(runnable, listener);
        ScheduledFuture<?> future = schedxsvcClassloaderContext.scheduleAtFixedRate(runnable, 0, 16, TimeUnit.NANOSECONDS);

        // scheduleAtFixedRate: taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Unexpected delay: " + event);

        // scheduleAtFixedRate: taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Unexpected delay: " + event);
        if (!Boolean.TRUE.equals(event.canceled))
            throw new Exception("scheduleAtFixedRate/taskStarting#1: Not able to cancel: " + event);

        // scheduleAtFixedRate: taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Unexpected or missing exception: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Unexpected or missing error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("scheduleAtFixedRate/taskAborted#1: Unexpected result for scheduleAtFixedRate: " + event);

        // scheduleAtFixedRate: taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleAtFixedRate: Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null && !(event.exception instanceof IllegalStateException))
            throw new Exception("scheduleAtFixedRate/taskDone#1: Non-null exception: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("scheduleAtFixedRate/taskDone#1: Unexpected or missing error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("scheduleAtFixedRate/taskDone#1: Unexpected result for scheduleAtFixedRate: " + event);

        if (!listener.events.isEmpty())
            throw new Exception("scheduleAtFixedRate: Unexpected events: " + listener.events);
    }

    /**
     * Tests schedule where a task is canceled upon being submitted.
     */
    @Test
    public void testListenerScheduleCanceledOnSubmitted() throws Throwable {
        TaskListener listener = new TaskListener(true);
        listener.whenToCancel.get(TaskEvent.Type.taskSubmitted).add(CancelType.mayNotInterruptIfRunning);
        Runnable runnable = new CounterTask();
        runnable = ManagedExecutors.managedTask(runnable, listener);
        ScheduledFuture<?> future = schedxsvcClassloaderContext.scheduleWithFixedDelay(runnable, 1, 17, TimeUnit.MICROSECONDS);

        // scheduleWithFixedDelay: taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskSubmitted#1: Unexpected delay: " + event);

        // scheduleWithFixedDelay: taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Unexpected or missing exception: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Unexpected or missing error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("scheduleWithFixedDelay/taskAborted#1: Unexpected result for scheduleWithFixedDelay: " + event);

        // scheduleWithFixedDelay: taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("scheduleWithFixedDelay: Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null && !(event.exception instanceof IllegalStateException))
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Non-null exception: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Unexpected or missing error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("scheduleWithFixedDelay/taskDone#1: Unexpected result for scheduleWithFixedDelay: " + event);

        if (!listener.events.isEmpty())
            throw new Exception("scheduleWithFixedDelay: Unexpected events: " + listener.events);
    }

    /**
     * Tests submit methods with a managed task listener.
     */
    @Test
    public void testListenerSubmit() throws Throwable {

        // ---------------- submit(runnable) ---------------
        TaskListener listener = new TaskListener(true);
        Runnable runnable = new CounterTask();
        runnable = ManagedExecutors.managedTask(runnable, listener);
        Future<?> future = xsvcDefault.submit(runnable);

        // submit(runnable): taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("submit(runnable): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("submit(runnable)/taskSubmitted: Wrong future: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("submit(runnable)/taskSubmitted: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("submit(runnable)/taskSubmitted: Wrong task: " + event);

        // wait for task to run
        Object result = event.future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result != null)
            throw new Exception("submit(runnable): Unexpected result: " + result);

        if (event.future.isCancelled())
            throw new Exception("submit(runnable): Task should not be canceled. " + event.future);

        if (!event.future.isDone())
            throw new Exception("submit(runnable): Task should be done. " + event.future);

        // submit(runnable): taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("submit(runnable): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("submit(runnable)/taskStarting: Wrong future: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("submit(runnable)/taskStarting: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("submit(runnable)/taskStarting: Wrong task: " + event);

        // submit(runnable): taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("submit(runnable): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("submit(runnable)/taskDone: Wrong future: " + event + " vs " + future);
        if (event.execSvc != xsvcDefault)
            throw new Exception("submit(runnable)/taskDone: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("submit(runnable)/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("submit(runnable)/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("submit(runnable)/taskDone: Future.get should have been successful for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("submit(runnable)/taskDone: Result of future from submit(runnable) should be null for: " + event);

        if (!listener.events.isEmpty())
            throw new Exception("submit(runnable): Unexpected events: " + listener.events);

        // ---------------- submit(runnable, result) ---------------
        listener = new TaskListener(true);
        runnable = new CounterTask();
        runnable = ManagedExecutors.managedTask(runnable, listener);
        Future<String> futureString = xsvcDefault.submit(runnable, "SubmitWithRunnableResult");

        // submit(runnable, result): taskSubmitted
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("submit(runnable, result): Unexpected first event: " + event);
        if (event.future != futureString)
            throw new Exception("submit(runnable, result)/taskSubmitted: Wrong future: " + event + " vs " + futureString);
        if (event.execSvc != xsvcDefault)
            throw new Exception("submit(runnable, result)/taskSubmitted: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("submit(runnable, result)/taskSubmitted: Wrong task: " + event);

        // wait for task to run
        String resultString = (String) event.future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!"SubmitWithRunnableResult".equals(resultString))
            throw new Exception("submit(runnable, result): Unexpected result: " + resultString);

        if (event.future.isCancelled())
            throw new Exception("submit(runnable, result): Task should not be canceled. " + event.future);

        if (!event.future.isDone())
            throw new Exception("submit(runnable, result): Task should be done. " + event.future);

        // submit(runnable, result): taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("submit(runnable, result): Unexpected second event: " + event);
        if (event.future != futureString)
            throw new Exception("submit(runnable, result)/taskStarting: Wrong future: " + event + " vs " + futureString);
        if (event.execSvc != xsvcDefault)
            throw new Exception("submit(runnable, result)/taskStarting: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("submit(runnable, result)/taskStarting: Wrong task: " + event);

        // submit(runnable, result): taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("submit(runnable, result): Unexpected third event: " + event);
        if (event.future != futureString)
            throw new Exception("submit(runnable, result)/taskDone: Wrong future: " + event + " vs " + futureString);
        if (event.execSvc != xsvcDefault)
            throw new Exception("submit(runnable, result)/taskDone: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("submit(runnable, result)/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("submit(runnable, result)/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("submit(runnable, result)/taskDone: Future.get should have been successful for " + event, event.failureFromFutureGet);
        if (!resultString.equals(event.result))
            throw new Exception("submit(runnable, result)/taskDone: Incorrect result of future for submit(runnable, result) during: " + event);

        if (!listener.events.isEmpty())
            throw new Exception("submit(runnable, result): Unexpected events: " + listener.events);

        // ---------------- submit(callable) ---------------
        listener = new TaskListener(true);
        Callable<Integer> callable = new CounterTask();
        callable = ManagedExecutors.managedTask(callable, listener);
        Future<Integer> futureInt = xsvcDefault.submit(callable);

        // submit(callable): taskSubmitted
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("submit(callable): Unexpected first event: " + event);
        if (event.future != futureInt)
            throw new Exception("submit(callable)/taskSubmitted: Wrong future: " + event + " vs " + futureInt);
        if (event.execSvc != xsvcDefault)
            throw new Exception("submit(callable)/taskSubmitted: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("submit(callable)/taskSubmitted: Wrong task: " + event);

        // wait for task to run
        Integer resultInt = (Integer) event.future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (resultInt != 1)
            throw new Exception("submit(callable): Unexpected result: " + resultInt);

        if (event.future.isCancelled())
            throw new Exception("submit(callable): Task should not be canceled. " + event.future);

        if (!event.future.isDone())
            throw new Exception("submit(callable): Task should be done. " + event.future);

        // submit(callable): taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("submit(callable): Unexpected second event: " + event);
        if (event.future != futureInt)
            throw new Exception("submit(callable)/taskStarting: Wrong future: " + event + " vs " + futureInt);
        if (event.execSvc != xsvcDefault)
            throw new Exception("submit(callable)/taskStarting: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("submit(callable)/taskStarting: Wrong task: " + event);

        // submit(callable): taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("submit(callable): Unexpected third event: " + event);
        if (event.future != futureInt)
            throw new Exception("submit(callable)/taskDone: Wrong future: " + event + " vs " + futureInt);
        if (event.execSvc != xsvcDefault)
            throw new Exception("submit(callable)/taskDone: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("submit(callable)/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("submit(callable)/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("submit(callable)/taskDone: Future.get should have been successful for " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("submit(callable)/taskDone: Unexpected result of future from submit(callable) during: " + event);

        if (!listener.events.isEmpty())
            throw new Exception("submit(callable): Unexpected events: " + listener.events);
    }

    /**
     * Tests that managed task listener methods do not run under a transaction.
     */
    @Test
    public void testListenerTransactionContextSuspended() throws Throwable {

        tran.begin();
        try {
            TaskListener listener = new TaskListener(true);
            SlowTask slowTask = new SlowTask();
            Callable<Long> managedCallable = ManagedExecutors.managedTask((Callable<Long>) slowTask, listener);
            Future<Long> future = xsvcDefault.submit(managedCallable);

            // encourage the liberty executor to start running the task
            try {
                Long result = future.get(1, TimeUnit.MILLISECONDS);
                throw new Exception("Result shouldn't be available after 1ms. Result: " + result);
            } catch (TimeoutException x) {
            } // pass

            // poll the task to make sure that it started
            for (long timeWaited = 0; slowTask.numStarted.get() == 0 && timeWaited < TIMEOUT; timeWaited += POLL_INTERVAL)
                Thread.sleep(POLL_INTERVAL);

            if (slowTask.numStarted.get() == 0)
                throw new Exception("Task did not start in a timely manner.");

            boolean canceled = future.cancel(true);
            if (!canceled)
                throw new Exception("Unable to cancel task.");

            try {
                Long result = future.get();
                throw new Exception("Task should have been interrupted when canceled. Instead: " + result);
            } catch (CancellationException x) {
            } // pass

            // taskSubmitted
            TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskSubmitted.equals(event.type))
                throw new Exception("Unexpected first event: " + event);
            if (event.execSvc != xsvcDefault)
                throw new Exception("taskSubmitted: Wrong executor: " + event);
            if (event.task != managedCallable)
                throw new Exception("taskSubmitted: Wrong task: " + event);
            if (event.future != future)
                throw new Exception("taskSubmitted: future from " + event + " doesn't match " + future);
            if (event.uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
                throw new Exception("taskSubmitted: should not be running in a transaction: " + event);

            // taskStarting
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskStarting.equals(event.type))
                throw new Exception("Unexpected second event: " + event);
            if (event.execSvc != xsvcDefault)
                throw new Exception("taskStarting: Wrong executor: " + event);
            if (event.task != managedCallable)
                throw new Exception("taskStarting: Wrong task: " + event);
            if (event.future != future)
                throw new Exception("taskStarting: future from " + event + " doesn't match " + future);
            if (event.uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
                throw new Exception("taskStarting: should not be running in a transaction: " + event);

            // taskCanceled
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskAborted.equals(event.type))
                throw new Exception("Unexpected third event: " + event);
            if (event.execSvc != xsvcDefault)
                throw new Exception("taskAborted: Wrong executor: " + event);
            if (event.task != managedCallable)
                throw new Exception("taskAborted: Wrong task: " + event);
            if (event.future != future)
                throw new Exception("taskAborted: future from " + event + " doesn't match " + future);
            if (!(event.exception instanceof CancellationException))
                throw new Exception("taskAborted: Unexpected exception (see cause): " + event, event.exception);

            // taskDone
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskDone.equals(event.type))
                throw new Exception("Unexpected third event: " + event);
            if (event.execSvc != xsvcDefault)
                throw new Exception("taskDone: Wrong executor: " + event);
            if (event.task != managedCallable)
                throw new Exception("taskDone: Wrong task: " + event);
            if (!(event.exception instanceof InterruptedException))
                throw new Exception("taskDone: missing or unexpected exception on " + event).initCause(event.exception);
            if (event.future != future)
                throw new Exception("taskDone: future from " + event + " doesn't match " + future);
            if (event.uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
                throw new Exception("taskDone: should not be running in a transaction: " + event);
            if (!(event.failureFromFutureGet instanceof CancellationException))
                throw new Exception("taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

            if (!listener.events.isEmpty())
                throw new Exception("Too many events: " + listener.events);
        } finally {
            tran.commit();
        }
    }

    /**
     * Using a trigger, schedule tasks that have a managed task listener.
     */
    @Test
    public void testListenerTriggeredTasks() throws Throwable {

        // ---------------- schedule(runnable, trigger) ---------------
        // Schedule a runnable to run exactly 2 times as quickly as possible
        ImmediateRepeatingTrigger twoShotTrigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE);
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Runnable runnable = new CounterTask();
        runnable = ManagedExecutors.managedTask(runnable, listener);
        ScheduledFuture<?> future = mschedxsvcClassloaderContext.schedule(runnable, twoShotTrigger);

        // schedule(runnable, trigger): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskStarting#1: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("schedule(runnable, trigger)/taskDone#1: Unexpected result for schedule(runnable, trigger): " + event);

        // schedule(runnable, trigger): taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskSubmitted#2: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskStarting #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskStarting#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskStarting#2: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("schedule(runnable, trigger)/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskStarting#2: Unexpected delay: " + event);

        // schedule(runnable, trigger): taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(runnable, trigger): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Wrong executor: " + event);
        if (event.task != runnable)
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Unexpected error from Future.get for " + event, event.failureFromFutureGet);
        if (event.result != null)
            throw new Exception("schedule(runnable, trigger)/taskDone#2: Unexpected result for schedule(runnable, trigger): " + event);

        if (!event.future.isDone())
            throw new Exception("schedule(runnable, trigger): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(runnable, trigger): Task should not be canceled. " + event.future);

        Object result = event.future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result != null)
            throw new Exception("schedule(runnable, trigger): Unexpected result: " + result);

        if (!listener.events.isEmpty())
            throw new Exception("schedule(runnable, trigger): Unexpected events: " + listener.events);

        // ---------------- schedule(callable, trigger) ---------------
        // Schedule a callable to run exactly 2 times as quickly as possible
        twoShotTrigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE);
        listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Callable<Integer> callable = new CounterTask();
        callable = ManagedExecutors.managedTask(callable, listener);
        ScheduledFuture<Integer> scheduledFuture = mschedxsvcClassloaderContext.schedule(callable, twoShotTrigger);

        // schedule(callable, trigger): taskSubmitted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected first event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong future: " + event + " vs " + scheduledFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable, trigger): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected second event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Wrong future: " + event + " vs " + scheduledFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Unexpected delay: " + event);

        // schedule(callable, trigger): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected third event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong future: " + event + " vs " + scheduledFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected error from Future.get for " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected result for schedule(callable, trigger): " + event);

        // schedule(callable, trigger): taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected first event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Wrong future: " + event + " vs " + scheduledFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Unexpected delay: " + event);

        // schedule(callable, trigger): taskStarting #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected second event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Wrong future: " + event + " vs " + scheduledFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Unexpected delay: " + event);

        // schedule(callable, trigger): taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected third event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Wrong future: " + event + " vs " + scheduledFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Wrong executor: " + event);
        if (event.task != callable)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Non-null exception: " + event).initCause(event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Unexpected error from Future.get for " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(2).equals(event.result))
            throw new Exception("schedule(callable, trigger)/taskDone#2: Unexpected result for schedule(callable, trigger): " + event);

        if (!event.future.isDone())
            throw new Exception("schedule(callable, trigger): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(callable, trigger): Task should not be canceled. " + event.future);

        result = event.future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(2).equals(result))
            throw new Exception("schedule(callable, trigger): Unexpected result: " + result);

        if (twoShotTrigger.previousExecutions.size() != 2)
            throw new Exception("Trigger should see exactly 2 executions. Instead: " + twoShotTrigger.previousExecutions);
        List<Integer> expectedResults = new LinkedList<Integer>();
        expectedResults.add(1);
        expectedResults.add(2);
        for (Iterator<LastExecution> it = twoShotTrigger.previousExecutions.iterator(); it.hasNext();) {
            LastExecution execution = it.next();
            if (execution.getRunStart().after(execution.getRunEnd()))
                throw new Exception("runStart should not be after runEnd for " + execution);
            result = execution.getResult();
            if (!expectedResults.remove(result))
                throw new Exception("Unexpected result: " + result + " for " + execution);
        }

        if (!listener.events.isEmpty())
            throw new Exception("schedule(callable, trigger): Unexpected events: " + listener.events);
    }

    /**
     * Tests that managed task listener do not run under a transaction.
     */
    @Test
    public void testListenerUsingFutureGet() throws Throwable {

        TaskListener listener = new TaskListener(true);
        listener.whenToGet.get(TaskEvent.Type.taskSubmitted).add(TIMEOUT);
        listener.whenToGet.get(TaskEvent.Type.taskStarting).add(TIMEOUT);

        CounterTask task = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask((Callable<Integer>) task, listener);
        List<Future<Integer>> futures = xsvcNoContext.invokeAll(Collections.singleton(managedCallable));
        Future<Integer> future = futures.get(0);

        // taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("Unexpected first event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("taskSubmitted: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("taskSubmitted: Wrong task: " + event);
        if (event.future != future)
            throw new Exception("taskSubmitted: future from " + event + " doesn't match " + future);
        if (event.uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
            throw new Exception("taskSubmitted: should not be running in a transaction: " + event);
        if (!(event.failureFromFutureGet instanceof InterruptedException)
            || !event.failureFromFutureGet.getMessage().startsWith("CWWKC1120E"))
            throw new Exception("taskSubmitted: missing or unexpected failure for Future.get during " + event, event.failureFromFutureGet);

        // taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("Unexpected second event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("taskStarting: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("taskStarting: Wrong task: " + event);
        if (event.future != future)
            throw new Exception("taskStarting: future from " + event + " doesn't match " + future);
        if (event.uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
            throw new Exception("taskStarting: should not be running in a transaction: " + event);
        if (!(event.failureFromFutureGet instanceof InterruptedException)
            || !event.failureFromFutureGet.getMessage().startsWith("CWWKC1120E"))
            throw new Exception("taskStarting: missing or unexpected failure for Future.get during " + event, event.failureFromFutureGet);

        // taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("Unexpected fourth event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("taskDone: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("taskDone: unexpected exception on " + event).initCause(event.exception);
        if (event.future != future)
            throw new Exception("taskDone: future from " + event + " doesn't match " + future);
        if (event.uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
            throw new Exception("taskDone: should not be running in a transaction: " + event);
        if (event.failureFromFutureGet != null)
            throw new Exception("taskDone: Future.get should not raise exception during " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("taskDone: Future.get had unexpected result during " + event);

        if (!listener.events.isEmpty())
            throw new Exception("Too many events: " + listener.events);
    }

    /**
     * Schedule a fixed rate task with a negative initial delay.
     * Java SE scheduled executor allows this, considering negative to mean 0, so we should do the same.
     */
    @Test
    public void testNegativeInitialDelay() throws Exception {
        CounterTask task = new CounterTask();
        ScheduledFuture<?> future = schedxsvcClassloaderContext.scheduleAtFixedRate(task, -600, 53, TimeUnit.MINUTES);
        try {
            long delay = -1;
            for (long start = System.currentTimeMillis(); delay < 52 && System.currentTimeMillis() - start < TIMEOUT; Thread.sleep(POLL_INTERVAL))
                delay = future.getDelay(TimeUnit.MINUTES);

            // Because -600 is interpreted as 0 (immediate) rather than 10 hours prior to current time,
            // the next computed time per the fixed rate should be 53 minutes into the future,
            // not 36 minutes (as would have been computed if based on -10 hours).
            if (delay < 52 || delay > 53)
                throw new Exception("Unexpected getDelay for next execution: " + delay);
        } finally {
            future.cancel(false);
        }
    }

    /**
     * From a managed task listener, reschedule one execution of an aborted task.
     */
    @Test
    public void testRescheduleAbortedTask() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToCheckIsDone.get(TaskEvent.Type.taskSubmitted).add(true);
        listener.whenToCheckIsDone.get(TaskEvent.Type.taskSubmitted).add(true);
        listener.whenToCheckIsDone.get(TaskEvent.Type.taskSubmitted).add(true);
        listener.whenToCheckIsDone.get(TaskEvent.Type.taskStarting).add(true);
        listener.whenToCheckIsDone.get(TaskEvent.Type.taskStarting).add(true);
        listener.whenToCheckIsDone.get(TaskEvent.Type.taskStarting).add(true);
        listener.whenToCheckIsDone.get(TaskEvent.Type.taskAborted).add(true);
        listener.whenToCheckIsDone.get(TaskEvent.Type.taskDone).add(true);
        listener.whenToCheckIsDone.get(TaskEvent.Type.taskDone).add(true);
        listener.whenToCheckIsDone.get(TaskEvent.Type.taskDone).add(true);
        listener.whenToReschedule.get(TaskEvent.Type.taskAborted).add(1l); // when aborted for the first time, reschedule 1ms in the future
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);

        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        Trigger trigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE, 1); // 2 executions, but skip the first

        ScheduledFuture<Integer> scheduleFuture = mschedxsvcClassloaderContext.schedule(managedCallable, trigger);

        // schedule(callable, trigger): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected first event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong future: " + event + " vs " + scheduleFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Unexpected delay: " + event);
        if (!Boolean.FALSE.equals(event.isDone))
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Task should not be done: " + event);

        // schedule(callable, trigger): taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected second event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Wrong future: " + event + " vs " + scheduleFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof SkippedException))
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Unexpected exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);
        if (!Boolean.TRUE.equals(event.isDone))
            throw new Exception("schedule(callable, trigger)/taskAborted#1: Task should be done: " + event);

        // Expect 7 more events between the schedule and reschedule
        Queue<TaskEvent> scheduleEvents = new LinkedList<TaskEvent>();
        Queue<TaskEvent> rescheduleEvents = new LinkedList<TaskEvent>();
        Future<?> rescheduleFuture = null;

        for (int i = 0; i < 7; i++) {
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event != null)
                if (event.future == scheduleFuture)
                    scheduleEvents.add(event);
                else {
                    rescheduleFuture = event.future;
                    rescheduleEvents.add(event);
                }
        }

        // [re]schedule(callable): taskSubmitted #1
        event = rescheduleEvents.poll();
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("[re]schedule(callable): Unexpected first event: " + event);
        if (event.future != rescheduleFuture)
            throw new Exception("[re]schedule(callable)/taskSubmitted#1: Future does not match: " + event + " vs " + rescheduleFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable)/taskSubmitted#1: Unexpected delay: " + event);
        if (!Boolean.FALSE.equals(event.isDone))
            throw new Exception("[re]schedule(callable)/taskSubmitted#1: Task should not be done: " + event);

        // [re]schedule(callable): taskStarting #1
        event = rescheduleEvents.poll();
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("[re]schedule(callable): Unexpected second event: " + event);
        if (event.future != rescheduleFuture)
            throw new Exception("[re]schedule(callable)/taskStarting#1: Future does not match: " + event + " vs " + rescheduleFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable)/taskStarting#1: Unexpected delay: " + event);
        if (!Boolean.FALSE.equals(event.isDone))
            throw new Exception("[re]schedule(callable)/taskStarting#1: Task should not be done: " + event);

        // [re]schedule(callable): taskDone #1
        event = rescheduleEvents.poll();
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("[re]schedule(callable): Unexpected third event: " + event);
        if (event.future != rescheduleFuture)
            throw new Exception("[re]schedule(callable)/taskDone#1: Future does not match: " + event + " vs " + rescheduleFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("[re]schedule(callable)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("[re]schedule(callable)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Boolean.TRUE.equals(event.isDone))
            throw new Exception("[re]schedule(callable)/taskDone#1: Task should be done: " + event);
        // result might be 1 or 2 depending on whether the reschedule runs before or after the remaining run of the original schedule
        Object rescheduleResult = event.result;
        if (!Integer.valueOf(1).equals(rescheduleResult) && !Integer.valueOf(2).equals(rescheduleResult))
            throw new Exception("[re]schedule(callable)/taskDone#1: Unexpected result for: " + event);

        // schedule(callable, trigger): taskDone #1
        event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected third event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong future: " + event + " vs " + scheduleFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable, trigger)/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);
        if (!Boolean.TRUE.equals(event.isDone))
            throw new Exception("schedule(callable, trigger)/taskDone#1: Task should be done: " + event);

        // schedule(callable, trigger): taskSubmitted #2
        event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected fourth event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Wrong future: " + event + " vs " + scheduleFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Unexpected delay: " + event);
        if (!Boolean.FALSE.equals(event.isDone))
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Task should not be done: " + event);

        // schedule(callable, trigger): taskStarting #2
        event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected fifth event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Wrong future: " + event + " vs " + scheduleFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Unexpected delay: " + event);
        if (!Boolean.FALSE.equals(event.isDone))
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Task should not be done: " + event);

        // schedule(callable, trigger): taskDone #2
        event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected sixth event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Wrong future: " + event + " vs " + scheduleFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Boolean.TRUE.equals(event.isDone))
            throw new Exception("schedule(callable, trigger)/taskDone#2: Task should be done: " + event);
        // result might be 1 or 2 depending on whether the reschedule runs before or after the remaining run of the original schedule
        Integer expectedScheduleResult = 3 - (Integer) rescheduleResult;
        if (!expectedScheduleResult.equals(event.result))
            throw new Exception("schedule(callable, trigger)/taskDone#2: Expecting + " + expectedScheduleResult + " for: " + event);

        if (!scheduleFuture.isDone())
            throw new Exception("schedule(callable): Future should be done.");

        if (!rescheduleFuture.isDone())
            throw new Exception("[re]schedule(callable, trigger): Future should be done.");

        if (scheduleFuture.isCancelled())
            throw new Exception("schedule(callable): Future should not be canceled.");

        if (rescheduleFuture.isCancelled())
            throw new Exception("[re]schedule(callable, trigger): Future should not be canceled.");

        Object result = scheduleFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!expectedScheduleResult.equals(result))
            throw new Exception("schedule(callable, trigger): Unexpected result: " + result + ". Should be " + expectedScheduleResult);

        // Result might be 1 or 2 depending on whether the reschedule runs before or after the remaining run of the original schedule.
        // Whichever it is, it better be the same as the last time we checked it!
        result = rescheduleFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!rescheduleResult.equals(result))
            throw new Exception("[re]schedule(callable): Unexpected result: " + result + ". Should be " + rescheduleResult);

        if (!listener.events.isEmpty())
            throw new Exception("Unexpected events: " + listener.events);
    }

    /**
     * From a managed task listener, reschedule a task that is done.
     */
    @Test
    public void testRescheduleDoneTask() throws Exception {
        TaskListener listener = new TaskListener();
        // Task will be rescheduled for 2 more executions once it is done
        listener.whenToReschedule.get(TaskEvent.Type.taskDone).add(new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE));
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);

        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        ScheduledFuture<Integer> scheduledFuture = schedxsvcClassloaderContext.schedule(managedCallable, 35, TimeUnit.MICROSECONDS);

        // schedule(callable): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected first event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong future: " + event + " vs " + scheduledFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable): Unexpected second event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong future: " + event + " vs " + scheduledFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskStarting#1: Unexpected delay: " + event);

        // schedule(callable): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected third event: " + event);
        if (event.future != scheduledFuture)
            throw new Exception("schedule(callable)/taskDone#1: Wrong future: " + event + " vs " + scheduledFuture);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected result: " + event);

        // [re]schedule(callable, trigger): taskSubmitted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("[re]schedule(callable, trigger): Unexpected first event: " + event);
        Future<?> future = event.future;
        if (future == scheduledFuture)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#1: Future for reschedule should not be the same as the original future: " + event);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // [re]schedule(callable, trigger): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("[re]schedule(callable, trigger): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#1: Unexpected delay: " + event);

        // [re]schedule(callable, trigger): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("[re]schedule(callable, trigger): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(2).equals(event.result))
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Future.get(): Unexpected result: " + event);

        // [re]schedule(callable, trigger): taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("[re]schedule(callable, trigger): Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#2: Unexpected delay: " + event);

        // [re]schedule(callable, trigger): taskStarting #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("[re]schedule(callable, trigger): Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#2: Unexpected delay: " + event);

        // [re]schedule(callable, trigger): taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("[re]schedule(callable, trigger): Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != schedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#2: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#2: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(3).equals(event.result))
            throw new Exception("[re]schedule(callable, trigger)/taskDone#2: Future.get(): Unexpected result: " + event);

        if (!scheduledFuture.isDone())
            throw new Exception("schedule(callable): Future should be done.");

        if (!future.isDone())
            throw new Exception("[re]schedule(callable, trigger): Future should be done.");

        if (scheduledFuture.isCancelled())
            throw new Exception("schedule(callable): Future should not be canceled.");

        if (future.isCancelled())
            throw new Exception("[re]schedule(callable, trigger): Future should not be canceled.");

        Object result = scheduledFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("schedule(callable): Unexpected result: " + result);

        result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(3).equals(result))
            throw new Exception("[re]schedule(callable, trigger): Unexpected result: " + result);

        if (!listener.events.isEmpty())
            throw new Exception("Unexpected events: " + listener.events);
    }

    /**
     * From a managed task listener, reschedule a task that is starting.
     */
    @Test
    public void testRescheduleStartingTask() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToReschedule.get(TaskEvent.Type.taskStarting).add(null);
        listener.whenToReschedule.get(TaskEvent.Type.taskStarting).add(1l);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);

        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        Trigger trigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE);

        ScheduledFuture<?> scheduleFuture = mschedxsvcClassloaderContext.schedule(managedCallable, trigger);

        // Expect 9 events between the schedule and reschedule
        Queue<TaskEvent> scheduleEvents = new LinkedList<TaskEvent>();
        Queue<TaskEvent> rescheduleEvents = new LinkedList<TaskEvent>();
        Future<?> rescheduleFuture = null;

        for (int i = 0; i < 9; i++) {
            TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event != null)
                if (event.future == scheduleFuture)
                    scheduleEvents.add(event);
                else {
                    rescheduleFuture = event.future;
                    rescheduleEvents.add(event);
                }
        }

        // schedule(callable, trigger): taskSubmitted #1
        TaskEvent event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected first event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Future does not match: " + event + " vs " + scheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable, trigger): taskStarting #1
        event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected second event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Future does not match: " + event + " vs " + scheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskStarting#1: Unexpected delay: " + event);

        // schedule(callable, trigger): taskDone #1
        event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected third event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Future does not match: " + event + " vs " + scheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable, trigger)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("schedule(callable, trigger)/taskDone#1: Future.get(): Unexpected result: " + event);

        // schedule(callable, trigger): taskSubmitted #2
        event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected fourth event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Future does not match: " + event + " vs " + scheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskSubmitted#2: Unexpected delay: " + event);

        // schedule(callable, trigger): taskStarting #2
        event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected fifth event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Future does not match: " + event + " vs " + scheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskStarting#2: Unexpected delay: " + event);

        // schedule(callable, trigger): taskDone #2
        event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable, trigger): Unexpected sixth event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Future does not match: " + event + " vs " + scheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable, trigger)/taskDone#2: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        // We don't know that order that the schedule/reschedule will complete in. Valid results for the schedule are 2 or 3.
        Object scheduleResult = event.result;
        if (!Integer.valueOf(2).equals(scheduleResult) && !Integer.valueOf(3).equals(scheduleResult))
            throw new Exception("schedule(callable, trigger)/taskDone#2: Future.get(): Unexpected value for: " + event);

        // [re]schedule(callable): taskSubmitted #1
        event = rescheduleEvents.poll();
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("[re]schedule(callable): Unexpected first event: " + event);
        if (event.future != rescheduleFuture)
            throw new Exception("[re]schedule(callable)/taskSubmitted#1: Future does not match: " + event + " vs " + rescheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable)/taskSubmitted#1: Unexpected delay: " + event);

        // [re]schedule(callable): taskStarting #1
        event = rescheduleEvents.poll();
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("[re]schedule(callable): Unexpected second event: " + event);
        if (event.future != rescheduleFuture)
            throw new Exception("[re]schedule(callable)/taskStarting#1: Future does not match: " + event + " vs " + rescheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable)/taskStarting#1: Unexpected delay: " + event);

        // [re]schedule(callable): taskDone #1
        event = rescheduleEvents.poll();
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("[re]schedule(callable): Unexpected third event: " + event);
        if (event.future != rescheduleFuture)
            throw new Exception("[re]schedule(callable)/taskDone#1: Future does not match: " + event + " vs " + rescheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("[re]schedule(callable)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("[re]schedule(callable)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        // We don't know that order that the schedule/reschedule will complete in. Valid results for the reschedule are 2 or 3.
        Object rescheduleResult = event.result;
        if (!Integer.valueOf(2).equals(rescheduleResult) && !Integer.valueOf(3).equals(rescheduleResult)
            || scheduleResult.equals(rescheduleResult))
            throw new Exception("[re]schedule(callable)/taskDone#1: Unexpected result for: " + event + ". Result of schedule was " + scheduleResult);

        if (!scheduleFuture.isDone())
            throw new Exception("schedule(callable, trigger): Future should be done.");

        if (!rescheduleFuture.isDone())
            throw new Exception("[re]schedule(callable): Future should be done.");

        if (scheduleFuture.isCancelled())
            throw new Exception("schedule(callable, trigger): Future should not be canceled.");

        if (rescheduleFuture.isCancelled())
            throw new Exception("[re]schedule(callable): Future should not be canceled.");

        Object result = scheduleFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!scheduleResult.equals(result))
            throw new Exception("schedule(callable, trigger): Unexpected result: " + result);

        result = rescheduleFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!rescheduleResult.equals(result))
            throw new Exception("[re]schedule(callable): Unexpected result: " + result);

        if (!Integer.valueOf(3).equals(scheduleResult) && !Integer.valueOf(3).equals(rescheduleResult))
            throw new Exception("Between the schedule/reschedule, one of the results should have been 3. Instead " + scheduleResult + " and " + rescheduleResult);

        if (!listener.events.isEmpty())
            throw new Exception("Unexpected events: " + listener.events);
    }

    /**
     * From a managed task listener, reschedule a submitted task.
     */
    @Test
    public void testRescheduleSubmittedTask() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToReschedule.get(TaskEvent.Type.taskSubmitted).add(new ImmediateRepeatingTrigger(1, ImmediateRepeatingTrigger.NO_FAILURE));
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);

        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        ScheduledFuture<?> scheduleFuture = schedxsvcClassloaderContext.schedule(managedCallable, 36, TimeUnit.MICROSECONDS);

        // Expect 6 events between the schedule and reschedule
        Queue<TaskEvent> scheduleEvents = new LinkedList<TaskEvent>();
        Queue<TaskEvent> rescheduleEvents = new LinkedList<TaskEvent>();
        Future<?> rescheduleFuture = null;

        for (int i = 0; i < 6; i++) {
            TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (event != null)
                if (event.future == scheduleFuture)
                    scheduleEvents.add(event);
                else {
                    rescheduleFuture = event.future;
                    rescheduleEvents.add(event);
                }
        }

        // schedule(callable): taskSubmitted #1
        TaskEvent event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected first event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable)/taskSubmitted#1: Future does not match: " + event + " vs " + scheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable): taskStarting #1
        event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable): Unexpected second event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable)/taskStarting#1: Future does not match: " + event + " vs " + scheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskStarting#1: Unexpected delay: " + event);

        // schedule(callable): taskDone #1
        event = scheduleEvents.poll();
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected third event: " + event);
        if (event.future != scheduleFuture)
            throw new Exception("schedule(callable)/taskDone#1: Future does not match: " + event + " vs " + scheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        // We don't know that order that the schedule/reschedule will complete in. Valid results for the schedule are 1 or 2.
        Object scheduleResult = event.result;
        if (!Integer.valueOf(1).equals(scheduleResult) && !Integer.valueOf(2).equals(scheduleResult))
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected value for: " + event);

        // [re]schedule(callable, trigger): taskSubmitted #1
        event = rescheduleEvents.poll();
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("[re]schedule(callable, trigger): Unexpected first event: " + event);
        if (event.future != rescheduleFuture)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#1: Future does not match: " + event + " vs " + rescheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable, trigger)/taskSubmitted#1: Unexpected delay: " + event);

        // [re]schedule(callable, trigger): taskStarting #1
        event = rescheduleEvents.poll();
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("[re]schedule(callable, trigger): Unexpected second event: " + event);
        if (event.future != rescheduleFuture)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#1: Future does not match: " + event + " vs " + rescheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable, trigger)/taskStarting#1: Unexpected delay: " + event);

        // [re]schedule(callable, trigger): taskDone #1
        event = rescheduleEvents.poll();
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("[re]schedule(callable, trigger): Unexpected third event: " + event);
        if (event.future != rescheduleFuture)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Future does not match: " + event + " vs " + rescheduleFuture);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        // We don't know that order that the schedule/reschedule will complete in. Valid results for the schedule are 1 or 2.
        Object rescheduleResult = event.result;
        if (!Integer.valueOf(1).equals(rescheduleResult) && !Integer.valueOf(2).equals(rescheduleResult) || scheduleResult.equals(rescheduleResult))
            throw new Exception("[re]schedule(callable, trigger)/taskDone#1: Unexpected result for: " + event + ". Result of schedule was " + scheduleResult);

        if (!scheduleFuture.isDone())
            throw new Exception("schedule(callable): Future should be done.");

        if (!rescheduleFuture.isDone())
            throw new Exception("[re]schedule(callable, trigger): Future should be done.");

        if (scheduleFuture.isCancelled())
            throw new Exception("schedule(callable): Future should not be canceled.");

        if (rescheduleFuture.isCancelled())
            throw new Exception("[re]schedule(callable, trigger): Future should not be canceled.");

        Object result = scheduleFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!scheduleResult.equals(result))
            throw new Exception("schedule(callable): Unexpected result: " + result);

        result = rescheduleFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!rescheduleResult.equals(result))
            throw new Exception("[re]schedule(callable, trigger): Unexpected result: " + result);

        if (!Integer.valueOf(2).equals(scheduleResult) && !Integer.valueOf(2).equals(rescheduleResult))
            throw new Exception("Between the schedule/reschedule, one of the results should have been 2. Instead " + scheduleResult + " and " + rescheduleResult);

        if (!listener.events.isEmpty())
            throw new Exception("Unexpected events: " + listener.events);
    }

    /**
     * From a managed task listener, resubmit an aborted task.
     */
    @Test
    public void testResubmitAbortedTask() throws Throwable {

        TaskListener listener = new TaskListener();
        listener.whenToCancel.get(TaskEvent.Type.taskSubmitted).add(CancelType.mayNotInterruptIfRunning);
        listener.whenToResubmit.get(TaskEvent.Type.taskAborted).add(true);
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);
        try {
            Future<Integer> future = xsvcNoContext.submit(managedCallable);
            throw new Exception("Submit should be rejected when task is canceled during taskSubmitted. Instead: " + future);
        } catch (RejectedExecutionException x) {
        } // pass

        // submit(callable): taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("submit(callable): Unexpected first event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("submit(callable)/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("submit(callable)/taskSubmitted: Wrong task: " + event);
        Future<?> future = event.future;

        try {
            Object result = future.get();
            throw new Exception("Future.get should be rejected when task is canceled during taskSubmitted. Instead: " + result);
        } catch (CancellationException x) {
        } // pass

        // submit(callable): taskAborted
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("submit(callable): Unexpected second event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("submit(callable)/taskAborted: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("submit(callable)/taskAborted: Wrong task: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("submit(callable)/taskAborted: Unexpected or missing exception on " + event).initCause(event.exception);
        if (event.future != future)
            throw new Exception("submit(callable)/taskAborted: Future does not match for " + event + ". Expecting: " + future);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("submit(callable)/taskAborted: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        LinkedList<TaskEvent> eventsForOriginalSubmit = new LinkedList<TaskEvent>();

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (event.future == future) {
            eventsForOriginalSubmit.add(event);
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // resubmit(callable): taskSubmitted
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("resubmit(callable): Unexpected first event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit(callable)/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("resubmit(callable)/taskSubmitted: Wrong task: " + event);
        Future<?> futureForResubmit = event.future;

        Object result = futureForResubmit.get();
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected result for task that was resubmitted on taskAborted: " + result);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (event.future == future) {
            eventsForOriginalSubmit.add(event);
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // resubmit(callable): taskStarting
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("resubmit(callable): Unexpected second event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit(callable)/taskStarting: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("resubmit(callable)/taskStarting: Wrong task: " + event);
        if (event.future != futureForResubmit)
            throw new Exception("resubmit(callable)/taskStarting: Future does not match for " + event + ". Expecting: " + futureForResubmit);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (event.future == future) {
            eventsForOriginalSubmit.add(event);
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // resubmit(callable): taskDone
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("resubmit(callable): Unexpected third event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit(callable)/taskDone: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("resubmit(callable)/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("resubmit(callable)/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.future != futureForResubmit)
            throw new Exception("resubmit(callable)/taskDone: Future does not match for " + event + ". Expecting: " + futureForResubmit);
        if (event.failureFromFutureGet != null)
            throw new Exception("resubmit(callable)/taskDone: Future should not raise exception for " + event, event.failureFromFutureGet);

        event = eventsForOriginalSubmit.poll();
        if (event == null)
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);

        // submit(callable): taskDone
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("submit(callable): Unexpected third event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("submit(callable)/taskDone: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("submit(callable)/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("submit(callable)/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.future != future)
            throw new Exception("submit(callable)/taskDone: Future does not match for " + event + ". Expecting: " + future);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("submit(callable)/taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!eventsForOriginalSubmit.isEmpty())
            throw new Exception("submit(callable): Unexpected events for original submit: " + eventsForOriginalSubmit);

        if (!listener.events.isEmpty())
            throw new Exception("submit(callable): Unexpected events: " + listener.events);
    }

    /**
     * From a managed task listener, resubmit a completed task.
     */
    @Test
    public void testResubmitDoneTask() throws Throwable {

        TaskListener listener = new TaskListener();
        listener.whenToResubmit.get(TaskEvent.Type.taskDone).add(true);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);
        List<Future<Integer>> futures = xsvcNoContext.invokeAll(Collections.singleton(managedCallable));
        Future<Integer> future = futures.get(0);

        // invokeAll: taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("invokeAll: Unexpected first event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("invokeAll/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("invokeAll/taskSubmitted: Wrong task: " + event);
        if (event.future != future)
            throw new Exception("invokeAll/taskSubmitted: future from " + event + " does not match " + future);

        Integer result = future.get();
        if (result != 1)
            throw new Exception("invokeAll: Unexpected result: " + result);

        // invokeAll: taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("invokeAll: Unexpected second event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("invokeAll/taskStarting: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("invokeAll/taskStarting: Wrong task: " + event);
        if (event.future != future)
            throw new Exception("invokeAll/taskStarting: Future does not match for " + event + ". Expecting: " + future);

        // invokeAll: taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("invokeAll: Unexpected third event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("invokeAll/taskDone: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("invokeAll/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("invokeAll/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.future != future)
            throw new Exception("invokeAll/taskDone: Future does not match for " + event + ". Expecting: " + future);
        if (event.failureFromFutureGet != null)
            throw new Exception("invokeAll/taskDone: Future should not raise exception for " + event, event.failureFromFutureGet);

        // resubmit: taskSubmitted
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("resubmit: Unexpected first event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("resubmit/taskSubmitted: Wrong task: " + event);
        Future<?> futureForResubmit = event.future;

        Object resultOfResubmit = futureForResubmit.get();
        if (!Integer.valueOf(2).equals(resultOfResubmit))
            throw new Exception("resubmit: Unexpected result: " + result);

        // resubmit: taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("resubmit: Unexpected second event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit/taskStarting: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("resubmit/taskStarting: Wrong task: " + event);
        if (event.future != futureForResubmit)
            throw new Exception("resubmit/taskStarting: Future does not match for " + event + ". Expecting: " + futureForResubmit);

        // resubmit: taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("resubmit: Unexpected third event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit/taskDone: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("resubmit/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("resubmit/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.future != futureForResubmit)
            throw new Exception("resubmit/taskDone: Future does not match for " + event + ". Expecting: " + futureForResubmit);
        if (event.failureFromFutureGet != null)
            throw new Exception("resubmit/taskDone: Future should not raise exception for " + event, event.failureFromFutureGet);

        if (!listener.events.isEmpty())
            throw new Exception("invokeAll&resubmit: Unexpected events: " + listener.events);
    }

    /**
     * From a managed task listener, resubmit a task that is in the process of starting.
     */
    @Test
    public void testResubmitStartingTask() throws Throwable {
        TaskListener listener = new TaskListener();
        listener.whenToResubmit.get(TaskEvent.Type.taskStarting).add(true);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);

        Integer resultOfInvokeAny = xsvcNoContext.invokeAny(Collections.singleton(managedCallable));

        // The task that we submit from taskStarting could end up running before or after the task that is starting,
        // so we need to allow for a result of either 1 or 2.

        if (resultOfInvokeAny < 1 || resultOfInvokeAny > 2)
            throw new Exception("invokeAny&resubmit: unexpected result: " + resultOfInvokeAny);

        // invokeAny: taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("invokeAny: Unexpected first event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("invokeAny/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("invokeAny/taskSubmitted: Wrong task: " + event);
        Future<?> futureForInvokeAny = event.future;
        Object result = futureForInvokeAny.get();
        if (result != resultOfInvokeAny)
            throw new Exception("invokeAny/taskSubmitted: result " + result + " does not match " + resultOfInvokeAny);

        // invokeAny: taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("invokeAny: Unexpected second event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("invokeAny/taskStarting: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("invokeAny/taskStarting: Wrong task: " + event);
        if (event.future != futureForInvokeAny)
            throw new Exception("invokeAll/taskStarting: Future does not match for " + event + ". Expecting: " + futureForInvokeAny);

        LinkedList<TaskEvent> eventsForInvokeAny = new LinkedList<TaskEvent>();

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (event.future == futureForInvokeAny) {
            eventsForInvokeAny.add(event);
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // resubmit: taskSubmitted
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("resubmit: Unexpected first event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("resubmit/taskSubmitted: Wrong task: " + event);
        Future<?> futureForResubmit = event.future;

        Integer resultExpectedForResubmit = 3 - resultOfInvokeAny;
        Object resultOfResubmit = futureForResubmit.get();
        if (!resultExpectedForResubmit.equals(resultOfResubmit))
            throw new Exception("resubmit: Expecting result of " + resultExpectedForResubmit + " for rescheule, not : " + resultOfResubmit);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (event.future == futureForInvokeAny) {
            eventsForInvokeAny.add(event);
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // resubmit: taskStarting
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("resubmit: Unexpected second event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit/taskStarting: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("resubmit/taskStarting: Wrong task: " + event);
        if (event.future != futureForResubmit)
            throw new Exception("resubmit/taskStarting: Future does not match for " + event + ". Expecting: " + futureForResubmit);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (event.future == futureForInvokeAny) {
            eventsForInvokeAny.add(event);
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        }

        // resubmit: taskDone
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("resubmit: Unexpected third event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit/taskDone: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("resubmit/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("resubmit/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.future != futureForResubmit)
            throw new Exception("resubmit/taskDone: Future does not match for " + event + ". Expecting: " + futureForResubmit);
        if (event.failureFromFutureGet != null)
            throw new Exception("resubmit/taskDone: Future should not raise exception for " + event, event.failureFromFutureGet);

        event = eventsForInvokeAny.poll();
        if (event == null)
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);

        // invokeAny: taskDone
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("invokeAny: Unexpected third event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("invokeAny/taskDone: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("invokeAny/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("invokeAny/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.future != futureForInvokeAny)
            throw new Exception("invokeAny/taskDone: Future does not match for " + event + ". Expecting: " + futureForInvokeAny);
        if (event.failureFromFutureGet != null)
            throw new Exception("invokeAny/taskDone: Future should not raise exception for " + event, event.failureFromFutureGet);

        if (!eventsForInvokeAny.isEmpty())
            throw new Exception("invokeAny: Unexpected events: " + eventsForInvokeAny);

        if (!listener.events.isEmpty())
            throw new Exception("invokeAny&resubmit: Unexpected events: " + listener.events);
    }

    /**
     * From a managed task listener, resubmit a submitted task.
     */
    @Test
    public void testResubmitSubmittedTask() throws Throwable {
        TaskListener listener = new TaskListener();
        listener.whenToResubmit.get(TaskEvent.Type.taskSubmitted).add(true);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Runnable runnable = new CounterTask();
        Runnable managedRunnable = ManagedExecutors.managedTask(runnable, listener);

        xsvcNoContext.execute(managedRunnable);

        // execute: taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("execute: Unexpected first event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("execute/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("execute/taskSubmitted: Wrong task: " + event);

        Future<?> futureForExecute = event.future;
        Object result = futureForExecute.get();
        if (result != null)
            throw new Exception("execute: unexpected result " + result);

        // resubmit: taskSubmitted
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("resubmit: Unexpected first event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit/taskSubmitted: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("resubmit/taskSubmitted: Wrong task: " + event);

        Future<?> futureForResubmit = event.future;
        Object resultOfResubmit = futureForResubmit.get();
        if (resultOfResubmit != null)
            throw new Exception("resubmit: unexpected result: " + resultOfResubmit);

        // After this point, events between the two task submissions will be intermixed.
        // Should be 2 taskStarting and 2 taskDone

        LinkedList<TaskEvent> eventsForExecute = new LinkedList<TaskEvent>();
        LinkedList<TaskEvent> eventsForResubmit = new LinkedList<TaskEvent>();

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        (event.future == futureForExecute ? eventsForExecute : eventsForResubmit).add(event);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        (event.future == futureForExecute ? eventsForExecute : eventsForResubmit).add(event);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        (event.future == futureForExecute ? eventsForExecute : eventsForResubmit).add(event);

        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        (event.future == futureForExecute ? eventsForExecute : eventsForResubmit).add(event);

        // execute: taskStarting
        event = eventsForExecute.poll();
        if (event == null)
            throw new Exception("execute: missing taskStarting/taskDone events. Execute: " + eventsForExecute + " Resubmit: " + eventsForResubmit);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("execute: Unexpected second event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("execute/taskStarting: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("execute/taskStarting: Wrong task: " + event);
        if (event.future != futureForExecute)
            throw new Exception("execute/taskStarting: Future does not match for " + event + ". Expecting: " + futureForExecute);

        // execute: taskDone
        event = eventsForExecute.poll();
        if (event == null)
            throw new Exception("execute: missing taskDone event. Execute: " + eventsForExecute + " Resubmit: " + eventsForResubmit);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("execute: Unexpected third event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("execute/taskDone: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("execute/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("execute/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.future != futureForExecute)
            throw new Exception("execute/taskDone: Future does not match for " + event + ". Expecting: " + futureForExecute);
        if (event.failureFromFutureGet != null)
            throw new Exception("execute/taskDone: Future should not raise exception for " + event, event.failureFromFutureGet);

        // resubmit: taskStarting
        event = eventsForResubmit.poll();
        if (event == null)
            throw new Exception("resubmit: missing taskStarting/taskDone events. Execute: " + eventsForExecute + " Resubmit: " + eventsForResubmit);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("resubmit: Unexpected second event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit/taskStarting: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("resubmit/taskStarting: Wrong task: " + event);
        if (event.future != futureForResubmit)
            throw new Exception("resubmit/taskStarting: Future does not match for " + event + ". Expecting: " + futureForResubmit);

        // resubmit: taskDone
        event = eventsForResubmit.poll();
        if (event == null)
            throw new Exception("resubmit: missing taskDone event. Execute: " + eventsForExecute + " Resubmit: " + eventsForResubmit);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("resubmit: Unexpected third event: " + event);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("resubmit/taskDone: Wrong executor: " + event);
        if (event.task != managedRunnable)
            throw new Exception("resubmit/taskDone: Wrong task: " + event);
        if (event.exception != null)
            throw new Exception("resubmit/taskDone: Non-null exception: " + event).initCause(event.exception);
        if (event.future != futureForResubmit)
            throw new Exception("resubmit/taskDone: Future does not match for " + event + ". Expecting: " + futureForResubmit);
        if (event.failureFromFutureGet != null)
            throw new Exception("resubmit/taskDone: Future should not raise exception for " + event, event.failureFromFutureGet);

        if (!eventsForExecute.isEmpty())
            throw new Exception("execute: Unexpected events: " + eventsForExecute);

        if (!eventsForResubmit.isEmpty())
            throw new Exception("resubmit: Unexpected events: " + eventsForResubmit);

        if (!listener.events.isEmpty())
            throw new Exception("execute&resubmit: Unexpected events: " + listener.events);

        int count = ((CounterTask) runnable).counter.get();
        if (count != 2)
            throw new Exception("Task should run exactly twice when resubmitted once. Instead: " + count);
    }

    /**
     * Tests ManagedScheduledExecutorService.schedule(runnable/callable, delay, unit)
     */
    @Test
    public void testScheduleOneTimeTasks() throws Exception {
        // Futures to cancel in case the test doesn't run to completion
        final List<Future<?>> futures = new LinkedList<Future<?>>();
        try {
            Callable<Integer> task1 = new CounterTask();

            ScheduledFuture<Integer> future2 = schedxsvcDefault.schedule(task1, 600, TimeUnit.MILLISECONDS);
            futures.add(future2);
            long delay2 = future2.getDelay(TimeUnit.MILLISECONDS);
            if (delay2 > 600 || delay2 < 0)
                throw new Exception("Delay for future2 should still be a positive number less than the scheduled delay. Instead: " + delay2);

            ScheduledFuture<Integer> future1 = schedxsvcDefaultLookup.schedule(task1, -2, TimeUnit.HOURS);
            futures.add(future1);

            CounterTask task3 = new CounterTask();
            ScheduledFuture<?> future3 = schedxsvcClassloaderContext.schedule((Runnable) task3, 0, TimeUnit.DAYS);
            futures.add(future3);

            // This task will be canceled right after we schedule it
            ScheduledFuture<?> canceledFuture = mschedxsvcDefaultLookup.schedule((Runnable) new CounterTask(), 400, TimeUnit.MILLISECONDS);
            canceledFuture.cancel(false);

            // test ScheduledFuture.compareTo

            int comparison0 = future1.compareTo(future1);
            if (comparison0 != 0)
                throw new Exception("compareTo self for scheduled future must return 0, not " + comparison0);

            int comparison1 = future1.compareTo(future2);
            if (comparison1 >= 0)
                throw new Exception("futureForEarlierTask.compareTo(futureForLaterTask) should be negative, not " + comparison1);

            int comparison2 = future2.compareTo(future1);
            if (comparison2 <= 0)
                throw new Exception("futureForLaterTask.compareTo(futureForEarlierTask) should be positive, not " + comparison2);

            try {
                int comparison3 = future2.compareTo(null);
                throw new Exception("Comparison with null returned " + comparison3);
            } catch (NullPointerException x) {
            }

            // task for future1 ought to run before the task for future2 based on the delay
            // but the timing in this test is too small to reliably expect this to happen.

            int result1 = future1.get(TIMEOUT, TimeUnit.MILLISECONDS);
            if (result1 != 1 && result1 != 2)
                throw new Exception("Unexpected result of task1: " + result1);

            int result2 = future2.get(TIMEOUT, TimeUnit.MILLISECONDS);
            if (result2 != 3 - result1) // 1 if result1 is 2, 2 if result1 is 1
                throw new Exception("Unexpected result of task2: " + result2);

            // test other ScheduledFuture methods

            if (!future1.isDone())
                throw new Exception("future1 should report that it isDone given that we have already waited for the result.");

            if (!future2.isDone())
                throw new Exception("future2 should report that it isDone given that we have already waited for the result.");

            future1.cancel(true);
            future1.cancel(false);

            long delay1 = future1.getDelay(TimeUnit.SECONDS);
            if (delay1 > 0)
                throw new Exception("Should not be a delay for future1 given that we have already waited for the result. Instead: " + delay1);

            delay2 = future1.getDelay(TimeUnit.NANOSECONDS);
            if (delay2 > 0)
                throw new Exception("Should not be a delay for future2 given that we have already waited for the result. Instead: " + delay2);

            assertEquals(Integer.valueOf(result1), future1.get());

            future3.get(TIMEOUT, TimeUnit.MILLISECONDS);
            int result3 = task3.counter.get();
            if (result3 != 1)
                throw new Exception("Unexpected result of task3: " + result3);

            // task that should already have been canceled
            try {
                Object result = canceledFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
                throw new Exception("Canceled scheduled task should not return result: " + result);
            } catch (CancellationException x) {
            } // pass

            // schedule a null callable
            try {
                ScheduledFuture<?> future4 = schedxsvcClassloaderContext.schedule((Callable<?>) null, 45, TimeUnit.MINUTES);
                futures.add(future4);
                throw new Exception("NullPointerException is required if you attempt to schedule a null Callable");
            } catch (NullPointerException x) {
            } // pass

            // schedule a null runnable
            try {
                ScheduledFuture<?> future5 = mschedxsvcClassloaderContext.schedule((Runnable) null, 52, TimeUnit.HOURS);
                futures.add(future5);
                throw new Exception("NullPointerException is required if you attempt to schedule a null Runnable");
            } catch (NullPointerException x) {
            } // pass

            // schedule a callable with null units
            try {
                ScheduledFuture<Integer> future6 = mschedxsvcClassloaderContext.schedule(task1, 60, null);
                futures.add(future6);
                throw new Exception("NullPointerException should be raised from schedule(callable, timeout, null) for null units");
            } catch (NullPointerException x) {
            } // pass

            // schedule a runnable with null units
            try {
                ScheduledFuture<?> future7 = schedxsvcClassloaderContext.schedule((Runnable) task1, 72, null);
                futures.add(future7);
                throw new Exception("NullPointerException should be raised from schedule(runnable, timeout, null) for null units");
            } catch (NullPointerException x) {
            } // pass
        } finally {
            // clean up anything that is still running
            for (Future<?> future : futures)
                future.cancel(true);
        }
    }

    /**
     * Tests ManagedScheduledExecutorService.scheduleAtFixedRate and scheduledAtFixedDelay
     */
    @Test
    public void testScheduleRepeatingTasks() throws Exception {
        // Futures to cancel in case the test doesn't run to completion
        final List<Future<?>> futures = new LinkedList<Future<?>>();
        try {
            CounterTask fixedDelayTask = new CounterTask();
            CounterTask fixedRateTask = new CounterTask();

            ScheduledFuture<?> fixedDelayFuture = mschedxsvcClassloaderContext.scheduleWithFixedDelay(fixedDelayTask, 0, 300, TimeUnit.MILLISECONDS);
            futures.add(fixedDelayFuture);

            ScheduledFuture<?> fixedRateFuture = mschedxsvcClassloaderContext.scheduleAtFixedRate(fixedRateTask, 100, 300, TimeUnit.MILLISECONDS);
            futures.add(fixedRateFuture);

            // poll for tasks to run at least 3 times each, then cancel them
            for (long begin = System.currentTimeMillis(), time = begin; (fixedDelayTask.counter.get() < 3 || fixedRateTask.counter.get() < 3)
                                                                        && time < begin + TIMEOUT * 3; time = System.currentTimeMillis())
                Thread.sleep(POLL_INTERVAL);

            int fixedDelayCount = fixedDelayTask.counter.get();
            if (fixedDelayCount < 3)
                throw new Exception("Fixed delay task only ran " + fixedDelayCount + " times. " + fixedDelayFuture);

            int fixedRateCount = fixedRateTask.counter.get();
            if (fixedRateCount < 3)
                throw new Exception("Fixed rate task only ran " + fixedRateCount + " times. " + fixedRateFuture);

            boolean canceled = fixedDelayFuture.cancel(true);
            if (!canceled)
                throw new Exception("Failed to cancel fixed delay task");

            canceled = fixedRateFuture.cancel(true);
            if (!canceled)
                throw new Exception("Failed to cancel fixed rate task");

            // Validate the delay between executions of the fixed delay task
            Long previousExecTime = null;
            for (long executionTime : fixedDelayTask.executionTimes) {
                if (previousExecTime != null) {
                    long delay = executionTime - previousExecTime;
                    if (delay < 200)
                        throw new Exception("Fixed delay between tasks not honored. Observed " + delay + "ms. Execution times: " + fixedDelayTask.executionTimes);
                    else if (delay > TIMEOUT)
                        throw new Exception("Unreasonable delay between executions of fixed delay task: " + delay + "ms. Execution times: "
                                            + fixedDelayTask.executionTimes);
                }
                previousExecTime = executionTime;
            }

            // Validate the fixed rate (at least as much as we can)
            previousExecTime = null;
            for (long executionTime : fixedRateTask.executionTimes) {
                if (previousExecTime != null) {
                    long delay = executionTime - previousExecTime;
                    if (delay > TIMEOUT)
                        throw new Exception("Unreasonable delay between executions of fixed rate task: " + delay + "ms. Execution times: "
                                            + fixedRateTask.executionTimes);
                }
                previousExecTime = executionTime;
            }
        } finally {
            // clean up anything that is still running
            for (Future<?> future : futures)
                future.cancel(true);
        }
    }

    /**
     * Tests a task that cancels itself.
     */
    @Test
    public void testSelfCancelingTask() throws Throwable {
        SlowTask task = new SlowTask();
        LinkedBlockingQueue<Future<?>> cancelationQueue = new LinkedBlockingQueue<Future<?>>();
        task.interruptIfCanceled.set(true);
        task.cancelationQueueRef.set(cancelationQueue);
        TaskListener listener = new TaskListener(true);
        Callable<Long> managedTask = ManagedExecutors.managedTask((Callable<Long>) task, listener);

        Future<Long> future = mxsvcNoContext.submit(managedTask);
        cancelationQueue.add(future); // add the task's own future to be canceled by the task

        // taskSubmitted
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("taskSubmitted: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mxsvcNoContext)
            throw new Exception("taskSubmitted: Wrong executor: " + event);
        if (event.task != managedTask)
            throw new Exception("taskSubmitted: Wrong task: " + event);

        // wait for task to run
        try {
            Long result = future.get();
            throw new Exception("Canceled future must raise CancellationException, not return value: " + result);
        } catch (CancellationException x) {
        } // pass

        if (!event.future.isCancelled())
            throw new Exception("Task should be canceled. " + event.future);

        if (!event.future.isDone())
            throw new Exception("Task should be done. " + event.future);

        // taskStarting
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("taskStarting: Wrong future: " + event + " vs " + future);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("taskStarting: Wrong executor: " + event);
        if (event.task != managedTask)
            throw new Exception("taskStarting: Wrong task: " + event);

        // taskCanceled
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("taskAborted: Wrong future: " + event + " vs " + future);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("taskAborted: Wrong executor: " + event);
        if (event.task != managedTask)
            throw new Exception("taskAborted: Wrong task: " + event);
        if (!(event.exception instanceof CancellationException))
            throw new Exception("taskAborted: Unexpected exception (see cause): " + event, event.exception);

        // taskDone
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("taskDone: Wrong future: " + event + " vs " + future);
        if (event.execSvc != xsvcNoContext)
            throw new Exception("taskDone: Wrong executor: " + event);
        if (event.task != managedTask)
            throw new Exception("taskDone: Wrong task: " + event);
        if (!(event.exception instanceof InterruptedException))
            throw new Exception("taskDone: Missing InterruptedException for: " + event).initCause(event.exception);
        if (!(event.failureFromFutureGet instanceof CancellationException))
            throw new Exception("taskDone: Future.get did not raise expected exception for " + event, event.failureFromFutureGet);

        if (!listener.events.isEmpty())
            throw new Exception("Unexpected events: " + listener.events);
    }

    /**
     * Tests a task that cancels itself.
     */
    @Test
    public void testSelfInterruptingTask() throws Throwable {
        FailingTask task = new FailingTask(1);
        task.failByInterrupt = true;
        List<Future<Integer>> futures = xsvcNoContext.invokeAll(Collections.singleton(task), TIMEOUT, TimeUnit.MILLISECONDS);
        Future<Integer> future = futures.get(0);
        try {
            Integer result = future.get();
            throw new Exception("invokeAll: Future for task that interrupts itself should not return result " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        task = new FailingTask(1);
        task.failByInterrupt = true;
        try {
            Integer result = xsvcNoContext.invokeAny(Collections.singleton(task), TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("invokeAny: task that interrupts itself should not return result " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        task = new FailingTask(1);
        task.failByInterrupt = true;
        future = xsvcNoContext.submit((Callable<Integer>) task);
        try {
            Integer result = future.get();
            throw new Exception("submit: Future for task that interrupts itself should not return result " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }

        task = new FailingTask(1);
        task.failByInterrupt = true;
        future = schedxsvcClassloaderContext.schedule((Callable<Integer>) task, 44, TimeUnit.MICROSECONDS);
        try {
            Integer result = future.get();
            throw new Exception("schedule: Future for task that interrupts itself should not return result " + result);
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof InterruptedException))
                throw x;
        }
    }

    /**
     * Verify that a task can be scheduled from a ServletContextListener
     * and that Java EE Metadata context is propagated to the task.
     */
    @Test
    public void testServletContextListener() throws Exception {
        if (MyServletContextListener.failure != null)
            throw new Exception("ServletContextListener.contextInitialized failed. See cause.", MyServletContextListener.failure);

        Object result = MyServletContextListener.futureForTaskScheduledDuringContextInitialized.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result == null)
            throw new Exception("Unexpected result: " + result);

        result = MyServletContextListener.resultQueueForThreadStartedDuringContextInitialized.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result == null)
            throw new Exception("Thread created and started from ServletContextListener.contextInitialized did not produce a result within allotted interval");
        else if (result instanceof Throwable)
            throw new Exception("Thread failed. See cause.", (Throwable) result);
    }

    /**
     * Verify that Liberty executor/scheduled executors are always returned ahead of managed ones from EE concurrency.
     * Verify that default instances are always returned ahead of a configured instances.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    @Test
    public void testServiceRankings() throws Throwable {
        // Delegate to a user feature in order to get access to the service registry:
        Object result = InitialContext.doLookup("testresource/testServiceRankings");
        if (result instanceof Throwable)
            throw (Throwable) result;
    }

    /**
     * Skip all executions of a repeating task
     */
    @Test
    public void testSkipAllExecutions() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE, 1, 2); // run task twice, but skip it both times

        ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule(managedCallable, trigger);

        // schedule(callable): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable): taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskAborted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskAborted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#2: Unexpected delay: " + event);

        // schedule(callable): taskAborted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskAborted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskAborted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskAborted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskAborted#2: Unexpected delay: " + event);
        if (!(event.exception instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#2: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#2: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskDone#2: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("schedule(callable): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(callable): Task should not be canceled. " + event.future);

        try {
            Integer result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Future should raise SkippedException due to skipped execution. Instead: " + result);
        } catch (SkippedException x) {
        }

        if (!listener.events.isEmpty())
            throw new Exception("schedule(callable): Unexpected events: " + listener.events);
    }

    /**
     * Skip consecutive executions of a repeating task (but not the first or last execution)
     */
    @Test
    public void testSkipConsecutiveExecutions() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(4, ImmediateRepeatingTrigger.NO_FAILURE, 2, 3); // schedule to run 4 times, but skip the 2nd & 3rd runs

        ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule(managedCallable, trigger);

        // schedule(callable): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskStarting#1: Unexpected delay: " + event);

        // schedule(callable): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected or missing value: " + event);

        // schedule(callable): taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#2: Unexpected delay: " + event);

        // schedule(callable): taskAborted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskAborted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskAborted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskAborted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskAborted#2: Unexpected delay: " + event);
        if (!(event.exception instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#2: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#2: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskDone#2: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskSubmitted #3
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected seventh event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#3: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#3: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#3: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#3: Unexpected delay: " + event);

        // schedule(callable): taskAborted #3
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected eigth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskAborted#3: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskAborted#3: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskAborted#3: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskAborted#3: Unexpected delay: " + event);
        if (!(event.exception instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#3: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#3: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskDone #3
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected ninth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#3: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#3: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#3: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#3: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#3: Unexpected exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskDone#3: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskSubmitted #4
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected tenth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#4: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#4: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#4: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#4: Unexpected delay: " + event);

        // schedule(callable): taskStarting #4
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable): Unexpected eleventh event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskStarting#4: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskStarting#4: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskStarting#4: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskStarting#4: Unexpected delay: " + event);

        // schedule(callable): taskDone #4
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected twelvth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#4: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#4: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#4: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#4: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#4: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable)/taskDone#4: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(2).equals(event.result))
            throw new Exception("schedule(callable)/taskDone#4: Future.get(): Unexpected result: " + event);

        if (!event.future.isDone())
            throw new Exception("schedule(callable): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(callable): Task should not be canceled. " + event.future);

        Integer result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(2).equals(result))
            throw new Exception("schedule(callable): Unexpected result: " + result);

        if (!listener.events.isEmpty())
            throw new Exception("schedule(callable): Unexpected events: " + listener.events);
    }

    /**
     * Skip the first execution of a repeating task
     */
    @Test
    public void testSkipFirstExecution() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE, 1); // run task twice, but skip the first run

        ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule(managedCallable, trigger);

        // schedule(callable): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable): taskAborted #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskAborted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskAborted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskAborted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskAborted#1: Unexpected delay: " + event);
        if (!(event.exception instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#1: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#2: Unexpected delay: " + event);

        // schedule(callable): taskStarting #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskStarting#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskStarting#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskStarting#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskStarting#2: Unexpected delay: " + event);

        // schedule(callable): taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable)/taskDone#2: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("schedule(callable)/taskDone#2: Future.get(): Unexpected result: " + event);

        if (!event.future.isDone())
            throw new Exception("schedule(callable): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(callable): Task should not be canceled. " + event.future);

        Integer result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(1).equals(result))
            throw new Exception("schedule(callable): Unexpected result: " + result);

        if (!listener.events.isEmpty())
            throw new Exception("schedule(callable): Unexpected events: " + listener.events);
    }

    /**
     * Skip the last execution of a repeating task
     */
    @Test
    public void testSkipLastExecution() throws Exception {
        TaskListener listener = new TaskListener();
        listener.whenToGet.get(TaskEvent.Type.taskAborted).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        listener.whenToGet.get(TaskEvent.Type.taskDone).add(POLL_INTERVAL);
        Callable<Integer> callable = new CounterTask();
        Callable<Integer> managedCallable = ManagedExecutors.managedTask(callable, listener);
        Trigger trigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE, 2); // run task twice, but skip the second run

        ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule(managedCallable, trigger);

        // schedule(callable): taskSubmitted #1
        TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected first event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#1: Unexpected delay: " + event);

        // schedule(callable): taskStarting #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskStarting.equals(event.type))
            throw new Exception("schedule(callable): Unexpected second event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskStarting#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskStarting#1: Unexpected delay: " + event);

        // schedule(callable): taskDone #1
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected third event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#1: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#1: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#1: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#1: Unexpected exception: " + event, event.exception);
        if (event.failureFromFutureGet != null)
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);
        if (!Integer.valueOf(1).equals(event.result))
            throw new Exception("schedule(callable)/taskDone#1: Future.get(): Unexpected or missing result: " + event);

        // schedule(callable): taskSubmitted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskSubmitted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fourth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskSubmitted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskSubmitted#2: Unexpected delay: " + event);

        // schedule(callable): taskAborted #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskAborted.equals(event.type))
            throw new Exception("schedule(callable): Unexpected fifth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskAborted#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskAborted#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskAborted#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskAborted#2: Unexpected delay: " + event);
        if (!(event.exception instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#2: Unexpected or missing exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskAborted#2: Future.get(): Unexpected or missing exception: " + event, event.failureFromFutureGet);

        // schedule(callable): taskDone #2
        event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!TaskEvent.Type.taskDone.equals(event.type))
            throw new Exception("schedule(callable): Unexpected sixth event: " + event);
        if (event.future != future)
            throw new Exception("schedule(callable)/taskDone#2: Wrong future: " + event + " vs " + future);
        if (event.execSvc != mschedxsvcClassloaderContext)
            throw new Exception("schedule(callable)/taskDone#2: Wrong executor: " + event);
        if (event.task != managedCallable)
            throw new Exception("schedule(callable)/taskDone#2: Wrong task: " + event);
        if (event.delay == null || event.delay > 1)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected delay: " + event);
        if (event.exception != null)
            throw new Exception("schedule(callable)/taskDone#2: Unexpected exception: " + event, event.exception);
        if (!(event.failureFromFutureGet instanceof SkippedException))
            throw new Exception("schedule(callable)/taskDone#2: Future.get(): Unexpected exception: " + event, event.failureFromFutureGet);

        if (!event.future.isDone())
            throw new Exception("schedule(callable): Task should be done. " + event.future);

        if (event.future.isCancelled())
            throw new Exception("schedule(callable): Task should not be canceled. " + event.future);

        try {
            Integer result = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            throw new Exception("Future should raise SkippedException due to skipped execution. Instead: " + result);
        } catch (SkippedException x) {
        }

        if (!listener.events.isEmpty())
            throw new Exception("schedule(callable): Unexpected events: " + listener.events);
    }

    /**
     * Tests ManagedExecutorService.submit obtained via default injection
     */
    @Test
    public void testSubmit() throws Exception {
        testSubmit(xsvcDefault);
    }

    /**
     * Tests ManagedExecutorService.submit obtained via lookup="java:comp/DefaultExecutorService"
     */
    @Test
    public void testLookupSubmit() throws Exception {
        testSubmit(xsvcDefaultLookup);
    }

    private void testSubmit(ExecutorService xsvc) throws Exception {
        AtomicInteger counter = new AtomicInteger();
        Callable<Integer> task1 = new CounterTask(counter);
        Runnable task2 = new CounterTask(counter);

        Future<Integer> future1 = xsvc.submit(task1);
        Future<?> future2 = xsvc.submit(task2);
        Future<Integer> future3 = xsvc.submit(task2, 3);

        int result1 = future1.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result1 < 1 || result1 > 3)
            throw new Exception("Unexpected result of task1: " + result1);

        Object result2 = future2.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result2 != null)
            throw new Exception("Unexpected result of task2: " + result2);

        int result3 = future3.get(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result3 != 3)
            throw new Exception("Unexpected result of task3: " + result3);

        int count = counter.get();
        if (count != 3)
            throw new Exception("Expecting to run 3 tasks, not " + count);

        try {
            xsvcDefault.submit((Callable<Object>) null);
            throw new Exception("submit(null callable) must raise NullPointerException");
        } catch (NullPointerException x) {
        } // expected

        try {
            xsvcDefault.submit((Runnable) null);
            throw new Exception("submit(null runnable) must raise NullPointerException");
        } catch (NullPointerException x) {
        } // expected

        try {
            xsvcDefault.submit(null, "result");
            throw new Exception("submit(null, result) must raise NullPointerException");
        } catch (NullPointerException x) {
        } // expected
    }

    /**
     * Tests that a ManagedThreadFactory is available & usable as a ThreadFactory.
     */
    @Test
    public void testThreadFactory() throws Throwable {
        final LinkedBlockingQueue<Object> resultQueue = new LinkedBlockingQueue<Object>();
        final Runnable javaCompLookup = new Runnable() {
            @Override
            public void run() {
                try {
                    resultQueue.add(new InitialContext().lookup("java:comp/env/entry1"));
                } catch (Throwable x) {
                    resultQueue.add(x);
                }
            }
        };

        // Verify it works from the current thread
        javaCompLookup.run();
        Object result = resultQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result instanceof Throwable)
            throw (Throwable) result;
        else if (!"value1".equals(result))
            throw new Exception("Unexpected value when looking up from servlet thread: " + result);

        // Verify it works from a managed thread factory (injected) thread.
        threadFactoryJEEMetadataContext.newThread(javaCompLookup).start();
        result = resultQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result instanceof Throwable)
            throw (Throwable) result;
        else if (!"value1".equals(result))
            throw new Exception("Unexpected value when looking up from managed thread factory (injected) thread: " + result);

        // Verify it works from a managed thread factory (obtained via lookup) thread.
        final ThreadFactory threadFactory1 = (ThreadFactory) new InitialContext().lookup("java:comp/env/concurrent/threadFactory-jee-metadata-context");
        threadFactory1.newThread(javaCompLookup).start();
        result = resultQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (result instanceof Throwable)
            throw (Throwable) result;
        else if (!"value1".equals(result))
            throw new Exception("Unexpected value when looking up from managed thread factory (obtained via lookup) thread: " + result);

        // Switch to an unmanaged thread
        daemon.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Verify it doesn't work from an unmanaged thread
                javaCompLookup.run();
                Object result = resultQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (result instanceof NamingException)
                    ;
                else if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                else
                    throw new Exception("Should not be able to do lookup in java:comp from unmanaged thread. Result: " + result);

                // Verify it works from a managed thread factory (injected) thread.
                threadFactoryJEEMetadataContext.newThread(javaCompLookup).start();
                result = resultQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                else if (!"value1".equals(result))
                    throw new Exception("newThread invoked from unmanaged thread: Unexpected value when looking up from managed thread factory (injected) thread: " + result);

                // Verify it works from a managed thread factory (obtained via lookup) thread.
                threadFactory1.newThread(javaCompLookup).start();
                result = resultQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                else if (!"value1".equals(result))
                    throw new Exception("newThread invoked from unmanaged thread: Unexpected value when looking up from managed thread factory (obtained via lookup) thread: "
                                        + result);

                return null;
            }
        }).get(TIMEOUT * 5, TimeUnit.MILLISECONDS);
    }

    /**
     * Verify that the expected thread name and thread group names are used.
     */
    @Test
    public void testThreadName() throws Exception {
        Thread thread = mthreadFactoryDefault.newThread(new CounterTask());
        String groupName = thread.getThreadGroup().getName();
        String threadName = thread.getName();
        String toString = thread.toString();
        if (!"managedThreadFactory[DefaultManagedThreadFactory] WEB#concurrentSpec#concurrentSpec.war Thread Group".equals(groupName))
            throw new Exception("DefaultManagedThreadFactory created thread group with unexpected name: " + groupName);
        if (!threadName.startsWith("managedThreadFactory[DefaultManagedThreadFactory]-thread-"))
            throw new Exception("DefaultManagedThreadFactory created thread with unexpected name: " + threadName);
        if (!toString.contains(threadName))
            throw new Exception("Thread name is missing from: " + toString);
        if (!toString.contains(groupName))
            throw new Exception("Thread group name is missing from: " + toString);

        thread = threadFactoryJEEMetadataContext.newThread(new CounterTask());
        groupName = thread.getThreadGroup().getName();
        threadName = thread.getName();
        if (!"concurrent/threadFactory-jee-metadata-context WEB#concurrentSpec#concurrentSpec.war Thread Group".equals(groupName))
            throw new Exception("concurrent/threadFactory-jee-metadata-context created thread group with unexpected name: " + groupName);
        if (!threadName.startsWith("concurrent/threadFactory-jee-metadata-context-thread-"))
            throw new Exception("concurrent/threadFactory-jee-metadata-context created thread with unexpected name: " + threadName);
    }

    /**
     * Set the TRANSACTION execution property to various values.
     */
    @Test
    public void testTransactionExecutionProperty() throws Exception {

        Callable<Integer> getUOWType = new Callable<Integer>() {
            @Override
            public Integer call() throws SystemException {
                return UOWManagerFactory.getUOWManager().getUOWType();
            }
        };

        Future<Integer> future = xsvcNoContext.submit(getUOWType);
        try {
            int uowType = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            if (uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
                throw new Exception("submit: no execution properties: UOW type = " + uowType);
        } finally {
            future.cancel(true);
        }

        Map<String, String> execProps_SUSPEND = Collections.singletonMap(ManagedTask.TRANSACTION, ManagedTask.SUSPEND);
        Callable<Integer> getUOWType_SUSPEND = ManagedExecutors.managedTask(getUOWType, execProps_SUSPEND, null);
        future = xsvcNoContext.submit(getUOWType_SUSPEND);
        try {
            int uowType = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            if (uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
                throw new Exception("submit: TRANSACTION=SUSPEND: UOW type = " + uowType);
        } finally {
            future.cancel(true);
        }

        // USE_TRANSACTION_OF_EXECUTION_THREAD should not be supported for managed tasks, given that Future.get
        // might be running in a transaction when it causes a task that hasn't been started to run on the same thread.
        Map<String, String> execProps_USE_TRAN_OF_EXECUTION_THREAD = Collections.singletonMap(ManagedTask.TRANSACTION, ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD);
        Callable<Integer> getUOWType_USE_TRAN_OF_EXECUTION_THREAD = ManagedExecutors.managedTask(getUOWType, execProps_USE_TRAN_OF_EXECUTION_THREAD, null);
        try {
            future = xsvcNoContext.submit(getUOWType_USE_TRAN_OF_EXECUTION_THREAD);
            future.cancel(true);
            throw new Exception("Task with USE_TRANSACTION_OF_EXECUTION_THREAD should be rejected. Instead: " + future);
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().contains(ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD))
                throw x;
        }

        future = schedxsvcClassloaderContext.schedule(getUOWType, 41, TimeUnit.MICROSECONDS);
        try {
            int uowType = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            if (uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
                throw new Exception("schedule: no execution properties: UOW type = " + uowType);
        } finally {
            future.cancel(true);
        }

        future = schedxsvcClassloaderContext.schedule(getUOWType_SUSPEND, 42, TimeUnit.NANOSECONDS);
        try {
            int uowType = future.get(TIMEOUT, TimeUnit.MILLISECONDS);
            if (uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
                throw new Exception("submit: TRANSACTION=SUSPEND: UOW type = " + uowType);
        } finally {
            future.cancel(true);
        }

        // USE_TRANSACTION_OF_EXECUTION_THREAD should not be supported for managed tasks, given that Future.get
        // might be running in a transaction when it causes a task that hasn't been started to run on the same thread.
        try {
            future = schedxsvcClassloaderContext.schedule(getUOWType_USE_TRAN_OF_EXECUTION_THREAD, 43, TimeUnit.MICROSECONDS);
            future.cancel(true);
            throw new Exception("Scheduled task with USE_TRANSACTION_OF_EXECUTION_THREAD should be rejected. Instead: " + future);
        } catch (RejectedExecutionException x) {
            if (!x.getMessage().contains(ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD))
                throw x;
        }

        RunnableFromCallable<Integer> getUOWType_Runnable = new RunnableFromCallable<Integer>(getUOWType);
        threadFactoryDefaultLookup.newThread(getUOWType_Runnable).start();
        Integer uowType = getUOWType_Runnable.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(UOWManager.UOW_TYPE_LOCAL_TRANSACTION).equals(uowType))
            throw new Exception("newThread run: UOW type = " + uowType);

        RunnableFromCallable<Integer> getUOWType_SUSPEND_Runnable = new RunnableFromCallable<Integer>(getUOWType_SUSPEND);
        mthreadFactoryDefaultLookup.newThread(getUOWType_SUSPEND_Runnable).start();
        uowType = getUOWType_SUSPEND_Runnable.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(UOWManager.UOW_TYPE_LOCAL_TRANSACTION).equals(uowType))
            throw new Exception("newThread run: TRANSACTION=SUSPEND: UOW type = " + uowType);

        RunnableFromCallable<Integer> getUOWType_USE_TRAN_OF_EXECUTION_THREAD_Runnable = new RunnableFromCallable<Integer>(getUOWType_USE_TRAN_OF_EXECUTION_THREAD);
        threadFactoryDefault.newThread(getUOWType_USE_TRAN_OF_EXECUTION_THREAD_Runnable).start();
        uowType = getUOWType_USE_TRAN_OF_EXECUTION_THREAD_Runnable.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        if (!Integer.valueOf(UOWManager.UOW_TYPE_LOCAL_TRANSACTION).equals(uowType))
            throw new Exception("newThread run: TRANSACTION=USE_TRANSACTION_OF_EXECUTION_THREAD: UOW type = " + uowType);
    }

    /**
     * Ensure that managed task listener methods don't run in the transaction of the thread which invokes
     * Future.get or executor.invokeAll
     */
    @Test
    public void testTransactionSuspendedForListenerMethods() throws Exception {
        tran.begin();
        try {
            // invokeAll - Note that our TaskListener implementation will ensure no listener methods run in a transaction.
            CounterTask task = new CounterTask();
            Callable<Integer> mtask = ManagedExecutors.managedTask((Callable<Integer>) task, new TaskListener());
            List<Callable<Integer>> tasks = Arrays.asList(mtask, mtask, mtask, mtask, mtask);
            List<Future<Integer>> futures = mxsvcClassloaderContext.invokeAll(tasks);

            if (futures.size() != tasks.size())
                throw new Exception("Wrong number of futures: " + futures);

            List<Integer> results = new LinkedList<Integer>();
            results.add(futures.get(0).get(TIMEOUT, TimeUnit.MILLISECONDS));
            results.add(futures.get(1).get(TIMEOUT, TimeUnit.MILLISECONDS));
            results.add(futures.get(2).get(TIMEOUT, TimeUnit.MILLISECONDS));
            results.add(futures.get(3).get(TIMEOUT, TimeUnit.MILLISECONDS));
            results.add(futures.get(4).get(TIMEOUT, TimeUnit.MILLISECONDS));

            if (!results.containsAll(Arrays.asList(1, 2, 3, 4, 5)))
                throw new Exception("Unexpected results: " + results);

            // submit - Again, our TaskListener implementation will ensure no listener methods run in a transaction.
            task = new CounterTask();
            Callable<Integer> mcallable = ManagedExecutors.managedTask((Callable<Integer>) task, new TaskListener());
            Runnable mrunnable = ManagedExecutors.managedTask((Runnable) task, new TaskListener());
            Future<Integer> future1 = mxsvcClassloaderContext.submit(mcallable);
            Future<Integer> future2 = mxsvcClassloaderContext.submit(mcallable);
            Future<String> future3 = mxsvcClassloaderContext.submit(mrunnable, "R");
            Future<String> future4 = mxsvcClassloaderContext.submit(mrunnable, "r");
            Future<?> future5 = mxsvcClassloaderContext.submit(mrunnable);

            future5.get();
            String stringResult = future4.get();
            if (!"r".equals(stringResult))
                throw new Exception("Unexpected result for future 4: " + stringResult);
            stringResult = future3.get();
            if (!"R".equals(stringResult))
                throw new Exception("Unexpected result for future 3: " + stringResult);
            Integer intResult = future2.get();
            if (intResult < 1 || intResult > 5)
                throw new Exception("Unexpected result for future 2: " + intResult);
            intResult = future1.get();
            if (intResult < 1 || intResult > 5)
                throw new Exception("Unexpected result for future 1: " + intResult);

            int count = task.counter.get();
            if (count != 5)
                throw new Exception("Task should run 5 times, not " + count);
        } finally {
            tran.commit();
        }
    }

    /**
     * Tests ManagedScheduledExecutorService.schedule(runnable/callable, trigger)
     */
    @Test
    public void testTrigger() throws Exception {
        // Futures to cancel in case the test doesn't run to completion
        final List<Future<?>> futures = new LinkedList<Future<?>>();
        try {
            // Schedule callable with null trigger
            try {
                ScheduledFuture<Integer> future1 = mschedxsvcClassloaderContext.schedule((Callable<Integer>) new CounterTask(), null);
                futures.add(future1);
                throw new Exception("schedule(callable, null trigger) ought to cause NullPointerException"); // TODO: or should this be RejectedExecutionException? Spec is unclear
            } catch (NullPointerException x) {
            } // pass

            // Schedule runnable with null trigger
            try {
                ScheduledFuture<?> future1 = mschedxsvcClassloaderContext.schedule((Runnable) new CounterTask(), null);
                futures.add(future1);
                throw new Exception("schedule(runnable, null trigger) ought to cause NullPointerException"); // TODO: or should this be RejectedExecutionException? Spec is unclear
            } catch (NullPointerException x) {
            } // pass

            // Schedule a task to run exactly 2 times as quickly as possible
            Trigger twoShotTrigger = new ImmediateRepeatingTrigger(2, ImmediateRepeatingTrigger.NO_FAILURE);
            ScheduledFuture<Integer> future2 = mschedxsvcClassloaderContext.schedule((Callable<Integer>) new CounterTask(), twoShotTrigger);

            // Schedule a task to run exactly 3 times as quickly as possible
            CounterTask task3 = new CounterTask();
            Trigger threeShotTrigger = new ImmediateRepeatingTrigger(3, ImmediateRepeatingTrigger.NO_FAILURE);
            ScheduledFuture<?> future3 = mschedxsvcClassloaderContext.schedule((Runnable) task3, threeShotTrigger);

            // Schedule null callable
            try {
                ScheduledFuture<?> future4 = mschedxsvcClassloaderContext.schedule((Callable<?>) null, twoShotTrigger);
                futures.add(future4);
                throw new Exception("schedule(null callable, trigger) ought to cause NullPointerException");
            } catch (NullPointerException x) {
            } // pass

            // Schedule null runnable
            try {
                ScheduledFuture<?> future5 = mschedxsvcClassloaderContext.schedule((Runnable) null, threeShotTrigger);
                futures.add(future5);
                throw new Exception("schedule(null callable, trigger) ought to cause NullPointerException");
            } catch (NullPointerException x) {
            } // pass

            // Verify that task3 ran the correct number of times
            for (long begin = 0, time = begin; time < begin + TIMEOUT || task3.counter.get() < 3; time = System.currentTimeMillis())
                Thread.sleep(POLL_INTERVAL);

            int count3 = task3.counter.get();
            if (count3 != 3)
                throw new Exception("Incorrect number of executions for task3: " + count3);

            Object result3 = future3.get(TIMEOUT, TimeUnit.MILLISECONDS);
            if (result3 != null)
                throw new Exception("Incorrect result for future3: " + result3);

            // Verify that task2 ran the correct number of times
            Integer result2 = null;
            for (long begin = System.currentTimeMillis(), time = begin; time < begin + TIMEOUT; time = System.currentTimeMillis()) {
                try {
                    result2 = future2.get(POLL_INTERVAL, TimeUnit.MILLISECONDS);
                    if (Integer.valueOf(2).equals(result2))
                        return;
                } catch (TimeoutException x) {
                } // okay, need to try again
                Thread.sleep(POLL_INTERVAL);
            }
            if (!Integer.valueOf(2).equals(result2))
                throw new Exception("Incorrect result for future2: " + result2);

            // Trigger that attempts to run 5 times, but skips on the 1st and 3rd attempts. So in total, it should run 3 times successfully.
            Trigger skippingTrigger = new ImmediateRepeatingTrigger(5, ImmediateRepeatingTrigger.NO_FAILURE, 1, 3);
            task3.counter.set(0);
            ScheduledFuture<?> future6 = mschedxsvcClassloaderContext.schedule((Runnable) task3, skippingTrigger);
            futures.add(future6);
            try {
                Object result6 = future6.get(TIMEOUT, TimeUnit.MILLISECONDS);
                throw new Exception("Expecting SkippedException. Instead: " + result6);
            } catch (SkippedException x) {
            } // pass

            // poll for all 3 runs to complete
            for (long begin = System.currentTimeMillis(), time = begin; task3.counter.get() < 3 && time < begin + TIMEOUT; time = System.currentTimeMillis())
                Thread.sleep(POLL_INTERVAL);
            int count = task3.counter.get();
            if (count != 3)
                throw new Exception("Expecting the task to still run 3 times even when some attempts are skipped. Instead: " + count);

            int numSkipped = ((ImmediateRepeatingTrigger) skippingTrigger).numSkipped;
            if (numSkipped != 2)
                throw new Exception("Expecting to have skipped 2 task execution attemps. Instead: " + numSkipped);

            // RejectedExecutionException for schedule(callable, trigger)
            Trigger failingTrigger = new ImmediateRepeatingTrigger(1, 0); // fail the initial attempt to getNextRunTime
            try {
                ScheduledFuture<Integer> future = mschedxsvcClassloaderContext.schedule((Callable<Integer>) new CounterTask(), failingTrigger);
                futures.add(future);
                throw new Exception("schedule(callable) should be rejected when trigger.getNextRunTime fails");
            } catch (RejectedExecutionException x) {
            } // pass

            // RejectedExecutionException for schedule(runnable, trigger)
            failingTrigger = new ImmediateRepeatingTrigger(1, 0); // fail the initial attempt to getNextRunTime
            try {
                ScheduledFuture<?> future = mschedxsvcClassloaderContext.schedule((Runnable) new CounterTask(), failingTrigger);
                futures.add(future);
                throw new Exception("schedule(runnable) should be rejected when trigger.getNextRunTime fails");
            } catch (RejectedExecutionException x) {
            } // pass

            // Aborted task
            Trigger failSecondExecutionTrigger = new ImmediateRepeatingTrigger(2, 1); // Attempt to run twice, but fail getNextRunTime after the first execution
            CounterTask task7 = new CounterTask();
            ScheduledFuture<?> future7 = mschedxsvcClassloaderContext.schedule((Runnable) task7, failSecondExecutionTrigger);
            futures.add(future7);
            try {
                Object result = future7.get(TIMEOUT, TimeUnit.MILLISECONDS);
                throw new Exception("Task should have been aborted when trigger.getNextRunTime fails. Instead result is: " + result);
            } catch (AbortedException x) {
            } // pass

            int count7 = task7.counter.get();
            if (count7 != 1)
                throw new Exception("task7 should have run exactly once before aborting. Instead: " + count7);

            // Trigger with no executions of Runnable
            Trigger noExecutionTrigger = new ImmediateRepeatingTrigger(0, ImmediateRepeatingTrigger.NO_FAILURE);
            try {
                ScheduledFuture<?> future8 = mschedxsvcClassloaderContext.schedule((Runnable) new CounterTask(), noExecutionTrigger);
                futures.add(future8);
                throw new Exception("schedule(runnable) should be rejected when trigger indicates no initial nextRunTime: " + future8);
            } catch (RejectedExecutionException x) {
            }

            // Trigger with no executions of Callable
            try {
                ScheduledFuture<Integer> future9 = mschedxsvcClassloaderContext.schedule((Callable<Integer>) new CounterTask(), noExecutionTrigger);
                futures.add(future9);
                throw new Exception("schedule(callable) should be rejected when trigger indicates no initial nextRunTime: " + future9);
            } catch (RejectedExecutionException x) {
            }

            // Skip the first and only execution
            Trigger skipTheOnlyExecutionTrigger = new ImmediateRepeatingTrigger(1, ImmediateRepeatingTrigger.NO_FAILURE, 1);
            CounterTask task10 = new CounterTask();
            ScheduledFuture<?> future10 = mschedxsvcClassloaderContext.schedule((Runnable) task10, skipTheOnlyExecutionTrigger);
            futures.add(future10);
            try {
                future10.get(TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (SkippedException x) {
            } // pass
            int count10 = task10.counter.get();
            if (count10 != 0)
                throw new Exception("Task should be skipped on first attempt and then never rescheduled. Instead: " + count10);
        } finally {
            // clean up anything that is still running
            for (Future<?> future : futures)
                future.cancel(true);
        }
    }

    /**
     * Test units for scheduled tasks.
     */
    @Test
    public void testUnitConversions() throws Exception {
        // Schedule a task ridiculously far into the future, check the delay, and then cancel it.
        ScheduledFuture<?> future1 = schedxsvcDefault.schedule((Runnable) new CounterTask(), 1000000, TimeUnit.DAYS);
        try {
            long days = future1.getDelay(TimeUnit.DAYS);
            if (days != 106751l) // maximum possible due to limiations of java.util.concurrent.TimeUnit
                throw new Exception("Task1: Unexpected delay in days: " + days);

            long hours = future1.getDelay(TimeUnit.HOURS);
            if (hours != 2562047l)
                throw new Exception("Task1: Unexpected delay in hours: " + hours);

            long minutes = future1.getDelay(TimeUnit.MINUTES);
            if (minutes != 153722867l)
                throw new Exception("Task1: Unexpected delay in minutes: " + minutes);

            long seconds = future1.getDelay(TimeUnit.SECONDS); // expecting 9223372036, but allow for additional time that might have elapsed
            if (seconds < 9223372030l || seconds > 9223372036l)
                throw new Exception("Task1: Unexpected delay in seconds: " + seconds);

            long millis = future1.getDelay(TimeUnit.MILLISECONDS);
            if (millis < 9223372030000l || millis > 9223372038000l)
                throw new Exception("Task1: Unexpected delay in milliseconds: " + millis);

            long micros = future1.getDelay(TimeUnit.MICROSECONDS);
            if (micros < 9223372030000000l || micros > 9223372038000000l)
                throw new Exception("Task1: Unexpected delay in microseconds: " + micros);

            long nanos = future1.getDelay(TimeUnit.NANOSECONDS);
            if (nanos < 9223372030000000000l)
                throw new Exception("Task1: Unexpected delay in nanoseconds: " + nanos);

            // Schedule a task not quite as far into the future, check the delay, compare it, and then cancel it.
            ScheduledFuture<?> future2 = schedxsvcDefault.scheduleAtFixedRate(new CounterTask(), 9223372005l, 1, TimeUnit.SECONDS);
            try {
                seconds = future2.getDelay(TimeUnit.SECONDS);
                if (seconds < 9223372000l || seconds > 9223372005l)
                    throw new Exception("Task2: Unexpected delay in seconds: " + days);

                minutes = future2.getDelay(TimeUnit.MINUTES);
                if (minutes != 153722866l)
                    throw new Exception("Task2: Unexpected delay in minutes: " + minutes);

                hours = future2.getDelay(TimeUnit.HOURS);
                if (hours != 2562047l)
                    throw new Exception("Task2: Unexpected delay in hours: " + hours);

                days = future1.getDelay(TimeUnit.DAYS);
                if (days != 106751l)
                    throw new Exception("Task1: Unexpected delay in days: " + days);

                int result = future1.compareTo(future2);
                if (result <= 0)
                    throw new Exception("Unexpected comparison: " + result);
            } finally {
                future2.cancel(false);
            }
        } finally {
            future1.cancel(false);
        }

        long days = future1.getDelay(TimeUnit.DAYS);
        if (days != 106751l)
            throw new Exception("Delay should remain unchanged after canceling task in order to be consistent with java.util.concurrent.ScheduledThreadPoolExecutor. Instead: "
                                + days);

        // Schedule a task that runs every 38 nanoseconds, and let it run 4 times before canceling.
        TaskListener listener = new TaskListener();
        listener.whenToCancel.get(TaskEvent.Type.taskDone).add(CancelType.doNotCancel);
        listener.whenToCancel.get(TaskEvent.Type.taskDone).add(CancelType.doNotCancel);
        listener.whenToCancel.get(TaskEvent.Type.taskDone).add(CancelType.doNotCancel);
        listener.whenToCancel.get(TaskEvent.Type.taskDone).add(CancelType.mayInterruptIfRunning);
        ManagedCounterTask task = new ManagedCounterTask();
        task.listener = listener;

        // Wait for the task to run at least 4 times, then cancel
        long start = System.nanoTime();
        ScheduledFuture<?> future = schedxsvcDefault.scheduleAtFixedRate(task, 0, 38, TimeUnit.NANOSECONDS);
        try {
            // taskSubmitted #1
            TaskEvent event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskSubmitted.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected first event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#1: Unexpected delay: " + event);

            // taskStarting #1
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskStarting.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected second event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskStarting#1: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskStarting#1: Unexpected delay: " + event);

            // taskDone #1
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskDone.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected third event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskDone#1: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskDone#1: Unexpected delay: " + event);

            // taskSubmitted #2
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskSubmitted.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected fourth event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#2: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#2: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#2: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#2: Unexpected delay: " + event);

            // taskStarting #2
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskStarting.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected fifth event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskStarting#2: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskStarting#2: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskStarting#2: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskStarting#2: Unexpected delay: " + event);

            // taskDone #2
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskDone.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected sixth event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskDone#2: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskDone#2: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskDone#2: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskDone#2: Unexpected delay: " + event);

            // taskSubmitted #3
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskSubmitted.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected seventh event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#3: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#3: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#3: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#3: Unexpected delay: " + event);

            // taskStarting #3
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskStarting.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected eighth event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskStarting#3: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskStarting#3: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskStarting#3: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskStarting#3: Unexpected delay: " + event);

            // taskDone #3
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskDone.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected ninth event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskDone#3: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskDone#3: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskDone#3: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskDone#3: Unexpected delay: " + event);

            // taskSubmitted #4
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskSubmitted.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected tenth event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#4: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#4: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#4: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskSubmitted#4: Unexpected delay: " + event);

            // taskStarting #4
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskStarting.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected eleventh event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskStarting#4: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskStarting#4: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskStarting#4: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskStarting#4: Unexpected delay: " + event);

            // taskDone #4
            event = listener.events.poll(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!TaskEvent.Type.taskDone.equals(event.type))
                throw new Exception("scheduleAtFixedRate: Unexpected twelfth event: " + event);
            if (event.future != future)
                throw new Exception("scheduleAtFixedRate/taskDone#4: Wrong future: " + event + " vs " + future);
            if (event.execSvc != schedxsvcDefault)
                throw new Exception("scheduleAtFixedRate/taskDone#4: Wrong executor: " + event);
            if (event.task != task)
                throw new Exception("scheduleAtFixedRate/taskDone#4: Wrong task: " + event);
            if (event.delay == null || event.delay > 1)
                throw new Exception("scheduleAtFixedRate/taskDone#4: Unexpected delay: " + event);
        } finally {
            future.cancel(false);
        }
        long duration = System.nanoTime() - start;

        System.out.println("Took " + duration + "ns for 4 exeuctions of task with fixed rate of 38ns");
    }

    /**
     * Use Java SE scheduled thread pool executor with a managed thread factory to replace work manager configuration for max alarms
     */
    @Test
    public void testWorkManagerAlarmConfig() throws Exception {
        int maxAlarms = 2;
        ManagedThreadFactory threadFactory = (ManagedThreadFactory) new InitialContext()
                        .lookup("java:comp/DefaultManagedThreadFactory");
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(maxAlarms, threadFactory);
        Callable<Integer> task = new CounterTask();
        ScheduledFuture<Integer> future = executor.schedule(task, 50, TimeUnit.MILLISECONDS);
        int result = future.get();
        if (result != 1)
            throw new Exception("Unexpected result: " + result);
    }

    /**
     * Use Java SE thread pool executor with a managed thread factory to replace work manager configuration for thread pools
     */
    @Test
    public void testWorkManagerThreadPoolConfig() throws Exception {
        int minThreads = 2;
        int maxThreads = 2;
        int workRequestQueueSize = 3;
        RejectedExecutionHandler workRequestQueueFullAction = new ThreadPoolExecutor.AbortPolicy();
        ManagedThreadFactory threadFactory = (ManagedThreadFactory) new InitialContext()
                        .lookup("java:comp/DefaultManagedThreadFactory");
        ExecutorService executor = new ThreadPoolExecutor(minThreads, maxThreads, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(workRequestQueueSize), threadFactory, workRequestQueueFullAction);
        Callable<Integer> task = new CounterTask();
        Future<Integer> future = executor.submit(task);
        int result = future.get();
        if (result != 1)
            throw new Exception("Unexpected result: " + result);
        List<Future<Long>> futures = new LinkedList<Future<Long>>();
        try {
            // Use up all threads in the pool
            AtomicInteger numStarted = new AtomicInteger();
            for (int i = 0; i < maxThreads; i++)
                futures.add(executor.submit((Callable<Long>) new SlowTask(numStarted, new AtomicInteger(), TIMEOUT * 5)));
            // Wait for tasks to start running on pooled threads
            long TIMEOUT_NS = TimeUnit.MILLISECONDS.toNanos(TIMEOUT);
            for (long start = System.nanoTime(); numStarted.get() < maxThreads && System.nanoTime() - start < TIMEOUT_NS; Thread.sleep(POLL_INTERVAL));
            int count = numStarted.get();
            if (count < maxThreads)
                throw new Exception("Expecting " + maxThreads + " tasks to start. Instead: " + count);
            // Use up both queue positions
            for (int i = 0; i < workRequestQueueSize; i++)
                futures.add(executor.submit((Callable<Long>) new SlowTask()));
            // Next submit attempt should be aborted
            try {
                futures.add(executor.submit((Callable<Long>) new SlowTask()));
                throw new Exception("Task submission with full work queue should have been rejected. Futures: " + futures);
            } catch (RejectedExecutionException x) {
                System.out.println("Task was rejected as expected. " + x);
            }
        } finally {
            for (Future<Long> f : futures)
                f.cancel(true);
        }
    }

    /**
     * Liberty approach to replace the workTimeout property of work manager
     */
    @Test
    public void testWorkTimeout() throws Exception {
        ManagedExecutorService executor = (ManagedExecutorService) new InitialContext()
                        .lookup("java:comp/DefaultManagedExecutorService");
        Callable<Long> slowTask = new SlowTask();
        slowTask = ManagedExecutors.managedTask(slowTask, new WorkTimeout(200, TimeUnit.MILLISECONDS));
        Future<Long> future = executor.submit(slowTask);
        try {
            long result = future.get(1, TimeUnit.MINUTES);
            throw new Exception("Task execution did not time out as expected. Result: " + result);
        } catch (CancellationException x) {
            System.out.println("Task was canceled as expected. " + x);
        }
    }
}