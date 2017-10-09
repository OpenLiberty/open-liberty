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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.threading.PolicyTaskCallback;

/**
 * Callback that records information about the parameters that are supplied to it.
 */
public class ParameterInfoCallback extends PolicyTaskCallback {
    public static final int SUBMIT = 0, START = 1, CANCEL = 2, END = 3;
    public final Future<?>[] future = new Future<?>[END + 1];
    public final Boolean[] isCanceled = new Boolean[END + 1];
    public final Boolean[] isDone = new Boolean[END + 1];
    public final Object[] result = new Object[END + 1];
    public Object startContext;
    public final Object[] task = new Object[END + 1];

    @Override
    public void onCancel(Object task, Future<?> future, boolean timedOut, boolean whileRunning) {
        System.out.println("onCancel " + future);
        this.future[CANCEL] = future;
        this.isCanceled[CANCEL] = future.isCancelled();
        this.isDone[CANCEL] = future.isDone();
        this.task[CANCEL] = task;
        try {
            this.result[CANCEL] = future.get(1, TimeUnit.NANOSECONDS);
        } catch (Throwable x) {
            this.result[CANCEL] = x;
        }
    }

    @Override
    public void onEnd(Object task, Future<?> future, Object startObj, boolean aborted, int pending, Throwable failure) {
        System.out.println("onEnd " + future);
        this.future[END] = future;
        this.isCanceled[END] = future.isCancelled();
        this.isDone[END] = future.isDone();
        this.task[END] = task;
        try {
            this.result[END] = future.get(1, TimeUnit.NANOSECONDS);
        } catch (Throwable x) {
            this.result[END] = x;
        }
    }

    @Override
    public Object onStart(Object task, Future<?> future) {
        System.out.println("onStart " + future);
        this.future[START] = future;
        this.isCanceled[START] = future.isCancelled();
        this.isDone[START] = future.isDone();
        this.task[START] = task;
        return this;
    }

    @Override
    public void onSubmit(Object task, Future<?> future, int invokeAnyCount) {
        System.out.println("onSubmit " + future);
        this.future[SUBMIT] = future;
        this.isCanceled[SUBMIT] = future.isCancelled();
        this.isDone[SUBMIT] = future.isDone();
        this.task[SUBMIT] = task;
    }
}
