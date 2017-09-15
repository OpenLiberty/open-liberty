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
package com.ibm.ws.wsoc;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class SendFuture implements Future<Void> {

    private static final TraceComponent tc = Tr.register(SendFuture.class);

    private WsocConnLink connLink = null;
    private ExecutionException executionException = null;

    @Trivial
    public static enum FUTURE_STATUS {
        INIT, STARTED, CANCEL_PENDING, CANCELLED, ERROR, DONE
    };

    private FUTURE_STATUS status = FUTURE_STATUS.INIT;

    public SendFuture() {}

    public void initialize(WsocConnLink link, FUTURE_STATUS _status) {
        connLink = link;
        status = _status;
    }

    public synchronized void notifyAllNow() {
        this.notifyAll();
    }

    public synchronized void setStatus(FUTURE_STATUS newValue) {
        status = newValue;
    }

    public synchronized void setStatus(FUTURE_STATUS newValue, ExecutionException _ex) {
        status = newValue;
        executionException = _ex;
    }

    public synchronized FUTURE_STATUS getStatus() {
        return status;
    }

    public synchronized boolean changeStatus(FUTURE_STATUS oldState, FUTURE_STATUS newState) {
        // conditionally change states while sync'd
        if (status == oldState) {
            status = newState;
            return true;
        }
        return false;
    }

    @Override
    @FFDCIgnore(InterruptedException.class)
    public boolean cancel(boolean mayInterruptIfRunning) {

        boolean changed = changeStatus(FUTURE_STATUS.STARTED, FUTURE_STATUS.CANCEL_PENDING);

        if (!changed) {
            // not in right state to cancel
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "not in right state to cancel");
            }
            return false;
        }

        // attempt to cancel
        boolean worked = connLink.cancelWriteBufferAsync();

        if (worked) {
            // cancel request was ok, now wait to see if cancelled worked
            synchronized (this) {
                try {
                    if (getStatus() == FUTURE_STATUS.CANCEL_PENDING) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    // do NOT allow instrumented FFDC to be used here
                }
            }

            if (getStatus() == FUTURE_STATUS.CANCELLED) {
                return true;
            }
        }

        return false;
    }

    @Override
    public synchronized Void get() throws InterruptedException, ExecutionException {

        if (status == FUTURE_STATUS.DONE) {
            // write has been done, return null to say write is done.
            return null;
        }
        if (status == FUTURE_STATUS.CANCELLED) {
            CancellationException ce = new CancellationException();
            throw ce;
        }

        if (status == FUTURE_STATUS.ERROR && executionException != null) {
            throw executionException;
        }

        // otherwise, we need to block this thread, and wait till we are done, then return null
        this.wait();

        if (status == FUTURE_STATUS.CANCELLED) {
            CancellationException ce = new CancellationException();
            throw ce;
        }
        if (executionException != null) {
            throw executionException;
        }

        if (status == FUTURE_STATUS.DONE) {
            // write has been done, return null to say write is done.
            return null;
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "status has not DONE value");
            }
            return null;
        }

    }

    @Override
    @FFDCIgnore(InterruptedException.class)
    public synchronized Void get(long timeout, TimeUnit unit) throws InterruptedException,
                    ExecutionException, TimeoutException {

        if (status == FUTURE_STATUS.DONE) {
            // write has been done, return null to say write is done.
            return null;
        }
        if (status == FUTURE_STATUS.CANCELLED) {
            CancellationException ce = new CancellationException();
            throw ce;
        }

        if (status == FUTURE_STATUS.ERROR && executionException != null) {
            throw executionException;
        }

        // convert timeout to milliseconds
        long timeoutInMillis = unit.convert(timeout, TimeUnit.MILLISECONDS);

        try {
            this.wait(timeoutInMillis);
        } catch (InterruptedException ie) {
            // do NOT allow instrumented FFDC to be used here
            throw ie;
        }

        if (status == FUTURE_STATUS.CANCELLED) {
            CancellationException ce = new CancellationException();
            throw ce;
        }
        if (executionException != null) {
            throw executionException;
        }
        // if not interrupted and no cancelled or execution Exception, and we are not done, then we must have timed out  (not that the JDK would tell us that!)
        if ((status != FUTURE_STATUS.DONE) && (status != FUTURE_STATUS.ERROR)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Timeout has expired");
            }
            TimeoutException te = new TimeoutException();
            throw te;
        }

        if (status == FUTURE_STATUS.DONE) {
            // write has been done, return null to say write is done.
            return null;
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "status is done enough (though not DONE) with value of: " + status);
            }
            return null;
        }

    }

    @Override
    public synchronized boolean isCancelled() {
        if (status == FUTURE_STATUS.CANCELLED) {
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean isDone() {
        if ((status == FUTURE_STATUS.DONE) || (status == FUTURE_STATUS.CANCELLED) || (status == FUTURE_STATUS.ERROR)) {
            return true;
        }
        return false;
    }

}
