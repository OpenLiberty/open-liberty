/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.threading.listeners.CompletionListener;

// eventually consider move to kernel service utils
public class FutureCollectionCompletionListener implements CompletionListener<Boolean> {
    private final CompletionListener<Boolean> completionListener;
    private final AtomicInteger completionCount;
    private final AtomicBoolean completionResult = new AtomicBoolean(true);

    /*
     * Caller is responsible for ensuring that the futureConditions collection is immutable.
     */
    public static void newFutureCollectionCompletionListener(Collection<ApplicationDependency> futureConditions, CompletionListener<Boolean> newCL) {
        if (futureConditions.isEmpty()) {
            newCL.successfulCompletion(null, true);
        } else {
            FutureCollectionCompletionListener futureListener = new FutureCollectionCompletionListener(futureConditions.size(), newCL);
            futureListener.onCompletion(futureConditions);
        }
    }

    private FutureCollectionCompletionListener(int numDeps, CompletionListener<Boolean> completionListener) {
        assert numDeps > 0;
        this.completionListener = completionListener;
        completionCount = new AtomicInteger(numDeps);
    }

    // d131982: This step must be after the constructor finishes, not within
    // the constructor.  The dependencies will generally access the listener
    // in independent threads.
    // See "http://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.5"
    // "final Field Semantics".        
    void onCompletion(Collection<ApplicationDependency> dependencies) {
        if (!dependencies.isEmpty()) {
            for (ApplicationDependency dependency : dependencies) {
                dependency.onCompletion(this);
            }
        }
    }

    @Override
    public void successfulCompletion(Future<Boolean> future, Boolean result) {
        completionResult.compareAndSet(true, result);
        final int count = completionCount.decrementAndGet();
        if (count == 0) {
            completionListener.successfulCompletion(null, completionResult.get());
        }
    }

    @Override
    public void failedCompletion(Future<Boolean> future, Throwable t) {
        completionListener.failedCompletion(future, t);
    }
}