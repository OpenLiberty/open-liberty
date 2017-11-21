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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.enterprise.concurrent.ManagedExecutorService;

/**
 * Records the parameters from a ManagedTaskListener event
 */
class TaskEvent {
    enum Type {
        taskAborted, taskDone, taskStarting, taskSubmitted
    }

    Boolean canceled; // only populated if an attempt is made to future.cancel during the event
    Long delay;
    final Throwable exception;
    Throwable failureDuringEventHandler;
    Throwable failureFromFutureGet;
    final ManagedExecutorService execSvc;
    final Future<?> future;
    Boolean isDone;
    final CountDownLatch isPopulated = new CountDownLatch(1); // tracks if the event is fully populated
    Future<?> rescheduleFuture; // only populated if an attempt is made to reschedule during the event
    Object result; // only populated if an attempt is made to future.get during the event
    final Object task;
    int uowType;
    final Type type;

    TaskEvent(Type type, Future<?> future, ManagedExecutorService execSvc, Object task, Throwable exception) {
        this.type = type;
        this.future = future;
        this.execSvc = execSvc;
        this.task = task;
        this.exception = exception;
    }

    @Override
    public String toString() {
        return super.toString() + ':' + type + '[' + future + "][" + execSvc + "][" + task + "][" + exception + "]["
               + "][canceled?" + canceled + "][delay:" + delay + "][result:" + result + "][uowType:" + uowType + "]"
               + "[" + isDone + "][" + failureFromFutureGet + "][" + rescheduleFuture + "][" + failureDuringEventHandler + "]";
    }
}
