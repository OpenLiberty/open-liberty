/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;

/**
 * Wrapper for a Future<Boolean> that also provided diagnostics to show
 * what that future represents.
 */
@Trivial
public final class ApplicationDependency implements CompletionListener<Boolean> {
    private final FutureMonitor futureMonitor;
    private final Future<Boolean> future;
    private final String desc;
    private final int seqNo;

    private static final AtomicInteger _appDepCounter = new AtomicInteger();

    public ApplicationDependency(FutureMonitor futureMonitor, String desc) {
        this(futureMonitor, futureMonitor.createFuture(Boolean.class), desc);
    }

    public ApplicationDependency(FutureMonitor futureMonitor, Future<Boolean> future, String desc) {
        this.futureMonitor = futureMonitor;
        this.future = future;
        this.desc = desc;
        this.seqNo = _appDepCounter.getAndIncrement();
    }

    public Future<Boolean> getFuture() {
        return future;
    }

    public void setResult(boolean result) {
        futureMonitor.setResult(future, result);
    }

    public void setResult(Throwable t) {
        futureMonitor.setResult(future, t);
    }

    public void onCompletion(CompletionListener<Boolean> completionListener) {
        futureMonitor.onCompletion(future, completionListener);
    }

    @Override
    public String toString() {
        return "AppDep[" + seqNo + "]: desc=\"" + desc + "\", future=" + future;
    }

    public boolean isDone() {
        return future.isDone();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.threading.listeners.CompletionListener#successfulCompletion(java.util.concurrent.Future, java.lang.Object)
     */
    @Override
    public void successfulCompletion(Future<Boolean> future, Boolean result) {
        setResult(result);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.threading.listeners.CompletionListener#failedCompletion(java.util.concurrent.Future, java.lang.Throwable)
     */
    @Override
    public void failedCompletion(Future<Boolean> future, Throwable t) {
        setResult(t);
    }

    @Override
    public int hashCode() {
        return desc.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if ((obj != null) && (obj instanceof ApplicationDependency)) {
            return desc.equals(((ApplicationDependency) obj).desc);
        }
        return false;
    }
}
