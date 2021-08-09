/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.app.notifications;

import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;

public class AppInstallsCalledCompletionListener implements CompletionListener<Boolean> {

    private final ReentrantLock lock = new ReentrantLock();
    private Future<Void> waitingFuture;
    private final FutureMonitor futureMonitor;

    AppInstallsCalledCompletionListener(FutureMonitor futureMonitor) {
        this.futureMonitor = futureMonitor;
    }

    @Override
    public void successfulCompletion(Future<Boolean> future, Boolean result) {
        notifyWaitingFutures();
    }

    @Override
    public void failedCompletion(Future<Boolean> future, Throwable t) {
        // even on error we notify we are done so they just continue no matter what
        notifyWaitingFutures();
    }

    private void notifyWaitingFutures() {
        Future<Void> current = null;
        lock.lock();
        try {
            if (waitingFuture == null) {
                throw new IllegalStateException("No waiting future found.");
            }
            current = waitingFuture;
            waitingFuture = null;
        } finally {
            lock.unlock();
        }
        futureMonitor.setResult(current, (Void) null);
    }

    void createInstallsCompleteFuture() {
        lock.lock();
        try {
            if (waitingFuture != null) {
                throw new IllegalStateException("Already waiting for another notification.");
            }
            waitingFuture = futureMonitor.createFuture(Void.class);
        } finally {
            lock.unlock();
        }
    }

    Future<Void> getInstallsCompleteFuture() {
        lock.lock();
        try {
            if (waitingFuture == null) {
                // no waiting; create a future that is done
                return futureMonitor.createFutureWithResult((Void) null);
            }
            return waitingFuture;
        } finally {
            lock.unlock();
        }
    }
}
