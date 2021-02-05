/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.io.async;

import java.io.IOException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.tcpchannel.internal.TCPChannelMessageConstants;

abstract class AsyncChannelFuture extends AbstractAsyncFuture {

    private static final TraceComponent tc = Tr.register(AsyncChannelFuture.class,
                                                         TCPChannelMessageConstants.TCP_TRACE_NAME,
                                                         TCPChannelMessageConstants.TCP_BUNDLE);

    // new timeout code - timeout work item for this future object request
    protected TimerWorkItem timeoutTracker = null;

    /**
     * Construct a future representing an operation on the given channel.
     * 
     * @param channel
     */
    protected AsyncChannelFuture(AbstractAsyncChannel channel) {
        super(channel);
    }

    /*
     * @see com.ibm.io.async.AbstractAsyncFuture#addCompletionListener(com.ibm.io.async.ICompletionListener, java.lang.Object)
     */
    public void addCompletionListener(ICompletionListener listener, Object state) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addCompletionListener, listener " + listener);
        }

        boolean alreadyComplete = true;
        // DBG - the fullyCompleted flag doesn't get set to true until completion listener called, so why check?
        // also, why fully completed? if partial completed, it will still
        // be completed = true, and will bail on next check
        if (!this.fullyCompleted) {
            synchronized (this.completedSemaphore) {
                // check if the receiver is already completed.
                if (!this.completed) {
                    alreadyComplete = false;

                    // need to set listener in sync so future cannot be completed
                    // before listener is added
                    this.firstListener = listener;
                    this.firstListenerState = state;
                }
            }
        }

        // if already complete, listener would not have been notified
        if (alreadyComplete) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "request already complete - notifying listener");
            }
            invokeCallback(listener, this, state);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addCompletionListener, listener " + listener);
        }
    }

    /**
     * Attempts to cancel the operation represented by the AsyncFuture.
     * Cancellation will not succeed if the operation is already complete.
     * 
     * @param reason the object that will be thrown when the future results are retrieved.
     */
    public void cancel(Exception reason) {
        // IMPROVEMENT: need to rework how syncs on the future.complete() work. We really should be
        // syncing here, so we don't do the channel.cancel if the request is processing
        // future.complete() on another thread at the same time. Should just do a quick
        // sync, check the future.complete flag, then process only if !complete. That will
        // also mean we can remove a bunch of redundant checks for complete, but we need to check all
        // the paths carefully.
        if (this.channel == null) {
            return;
        }
        synchronized (this.completedSemaphore) {
            if (!this.completed) {
                try {
                    // this ends up calling future.completed()
                    this.channel.cancel(this, reason);
                } catch (Exception e) {
                    // Simply swallow the exception
                } // end try

            } else {
                if (this.channel.readFuture != null) {
                    this.channel.readFuture.setCancelInProgress(0);
                }
                if (this.channel.writeFuture != null) {
                    this.channel.writeFuture.setCancelInProgress(0);
                }
            }
        }
    }

    /**
     * When an async future has completed the following actions are performed,
     * in this order: <ol><li> The Future is marked completed <li> If
     * callbacks are registered, they are invoked in sequence, running
     * arbitrary code. </li><li> The semaphore blocking any waiting threads is
     * signalled to wake them up. </li></ol>
     * 
     */
    // Impl Assumes we are holding the completed sem lock
    protected void fireCompletionActions() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "fireCompletionActions");
        }
        if (this.firstListener != null) {
            ICompletionListener listenerToInvoke = this.firstListener;
            // reset listener so it can't be inadvertently be called on the next request
            this.firstListener = null;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "invoking callback for channel id: " + this.channel.channelIdentifier);
            }
            invokeCallback(listenerToInvoke, this, this.firstListenerState);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "no listener found for event, future: " + this);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "fireCompletionActions");
        }
    }

    /**
     * Throws the receiver's exception in its correct class.
     * 
     * This assumes that the exception is not null.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    protected void throwException() throws InterruptedException, IOException {

        if (this.exception instanceof IOException) {
            throw (IOException) this.exception;
        }
        if (this.exception instanceof InterruptedException) {
            throw (InterruptedException) this.exception;
        }
        if (this.exception instanceof RuntimeException) {
            throw (RuntimeException) this.exception;
        }
        throw new RuntimeException(this.exception);
    }

    /**
     * Store the timeout related item.
     * 
     * @param twi
     */
    public void setTimeoutWorkItem(TimerWorkItem twi) {
        this.timeoutTracker = twi;
    }

    /**
     * Access the stored timeout work item.
     * 
     * @return TimerWorkItem
     */
    public TimerWorkItem getTimeoutWorkItem() {
        return this.timeoutTracker;
    }

}