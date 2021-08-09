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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedTaskListener;
import javax.enterprise.concurrent.Trigger;

import com.ibm.wsspi.uow.UOWManager;
import com.ibm.wsspi.uow.UOWManagerFactory;

/**
 * A managed task listener that records events.
 */
public class TaskListener implements ManagedTaskListener {
    enum CancelType {
        mayInterruptIfRunning(true), mayNotInterruptIfRunning(false), doNotCancel(null);

        private Boolean booleanValue;

        private CancelType(Boolean value) {
            this.booleanValue = value;
        }
    }

    final TaskEventQueue events = new TaskEventQueue();
    final Map<TaskEvent.Type, List<CancelType>> whenToCancel = new HashMap<TaskEvent.Type, List<CancelType>>();
    final Map<TaskEvent.Type, List<Boolean>> whenToCheckIsDone = new HashMap<TaskEvent.Type, List<Boolean>>();
    final Map<TaskEvent.Type, List<Boolean>> whenToFail = new HashMap<TaskEvent.Type, List<Boolean>>();
    final Map<TaskEvent.Type, List<Long>> whenToGet = new HashMap<TaskEvent.Type, List<Long>>();
    final Map<TaskEvent.Type, List<Object>> whenToReschedule = new HashMap<TaskEvent.Type, List<Object>>();
    final Map<TaskEvent.Type, List<Boolean>> whenToResubmit = new HashMap<TaskEvent.Type, List<Boolean>>();

    TaskListener() {
        this(false);
    }

    TaskListener(boolean attemptFutureGetOnTaskAbortedAndDone) {
        for (TaskEvent.Type type : TaskEvent.Type.values()) {
            whenToCancel.put(type, Collections.synchronizedList(new LinkedList<CancelType>()));
            whenToCheckIsDone.put(type, Collections.synchronizedList(new LinkedList<Boolean>()));
            whenToFail.put(type, Collections.synchronizedList(new LinkedList<Boolean>()));
            whenToGet.put(type, Collections.synchronizedList(new LinkedList<Long>()));
            whenToReschedule.put(type, Collections.synchronizedList(new LinkedList<Object>()));
            whenToResubmit.put(type, Collections.synchronizedList(new LinkedList<Boolean>()));
        }

        if (attemptFutureGetOnTaskAbortedAndDone) {
            whenToGet.get(TaskEvent.Type.taskAborted).add(EEConcurrencyTestServlet.POLL_INTERVAL);
            whenToGet.get(TaskEvent.Type.taskDone).add(EEConcurrencyTestServlet.POLL_INTERVAL);
        }
    }

    @Override
    public void taskAborted(Future<?> future, ManagedExecutorService executor, Object task, Throwable x) {
        taskEvent(TaskEvent.Type.taskAborted, future, executor, task, x);
    }

    @Override
    public void taskDone(Future<?> future, ManagedExecutorService executor, Object task, Throwable x) {
        taskEvent(TaskEvent.Type.taskDone, future, executor, task, x);
    }

    private void taskEvent(TaskEvent.Type type, Future<?> future, ManagedExecutorService executor, Object task, Throwable x) {
        TaskEvent event = new TaskEvent(type, future, executor, task, x);
        events.add(event);
        System.out.println("received " + type + ", now populating TaskEvent@" + Integer.toHexString(event.hashCode()));
        try {
            if (future instanceof ScheduledFuture)
                event.delay = ((ScheduledFuture<?>) future).getDelay(TimeUnit.MILLISECONDS);

            try {
                event.uowType = UOWManagerFactory.getUOWManager().getUOWType();
                if (event.uowType != UOWManager.UOW_TYPE_LOCAL_TRANSACTION)
                    throw new RuntimeException("There shouldn't ever be a transaction already present when invoking a ManagedTaskListener method. " + event);
            } catch (IllegalStateException isx) {
                System.out.println("This can validly happen if the task was previously canceled (interrupted) while attempting to apply thread context, in which case there will be no transaction context on the thread");
                isx.printStackTrace();
            }

            Boolean checkIsDone = whenToCheckIsDone.get(type).isEmpty() ? false : whenToCheckIsDone.get(type).remove(0);
            if (Boolean.TRUE.equals(checkIsDone))
                event.isDone = future.isDone();

            Long timeoutForGet = whenToGet.get(type).isEmpty() ? null : whenToGet.get(type).remove(0);
            if (timeoutForGet != null)
                try {
                    if (timeoutForGet == Long.MAX_VALUE)
                        event.result = future.get();
                    else
                        event.result = future.get(timeoutForGet, TimeUnit.MILLISECONDS);
                } catch (Throwable t) {
                    event.failureFromFutureGet = t;
                }

            Boolean fail = whenToFail.get(type).isEmpty() ? false : whenToFail.get(type).remove(0);
            if (Boolean.TRUE.equals(fail))
                throw new ArithmeticException("Intentionally caused failure of managed task listener.");

            CancelType cancelType = whenToCancel.get(type).isEmpty() ? null : whenToCancel.get(type).remove(0);
            if (cancelType != null && cancelType.booleanValue != null)
                event.canceled = future.cancel(cancelType.booleanValue);

            Object rescheduleInfo = whenToReschedule.get(type).isEmpty() ? null : whenToReschedule.get(type).remove(0);
            if (rescheduleInfo instanceof Long) {
                Long delay = (Long) rescheduleInfo;
                if (task instanceof Callable)
                    event.rescheduleFuture = ((ScheduledExecutorService) executor).schedule((Callable<?>) task, delay, TimeUnit.MILLISECONDS);
                else
                    event.rescheduleFuture = ((ScheduledExecutorService) executor).schedule((Runnable) task, delay, TimeUnit.MILLISECONDS);
            } else if (rescheduleInfo instanceof Trigger) {
                Trigger trigger = (Trigger) rescheduleInfo;
                if (task instanceof Callable)
                    event.rescheduleFuture = ((ManagedScheduledExecutorService) executor).schedule((Callable<?>) task, trigger);
                else
                    event.rescheduleFuture = ((ManagedScheduledExecutorService) executor).schedule((Runnable) task, trigger);
            }

            Boolean resubmit = whenToResubmit.get(type).isEmpty() ? false : whenToResubmit.get(type).remove(0);
            if (Boolean.TRUE.equals(resubmit))
                if (task instanceof Callable)
                    event.rescheduleFuture = executor.submit((Callable<?>) task);
                else
                    event.rescheduleFuture = executor.submit((Runnable) task);
        } catch (Error failure) {
            event.failureDuringEventHandler = failure;
            throw failure;
        } catch (RuntimeException failure) {
            event.failureDuringEventHandler = failure;
            throw failure;
        } catch (Exception failure) {
            event.failureDuringEventHandler = failure;
            throw new RuntimeException(failure);
        } finally {
            System.out.println("ready for poll " + event);
            event.isPopulated.countDown();
        }
    }

    @Override
    public void taskStarting(Future<?> future, ManagedExecutorService executor, Object task) {
        taskEvent(TaskEvent.Type.taskStarting, future, executor, task, null);
    }

    @Override
    public void taskSubmitted(Future<?> future, ManagedExecutorService executor, Object task) {
        taskEvent(TaskEvent.Type.taskSubmitted, future, executor, task, null);
    }
}
