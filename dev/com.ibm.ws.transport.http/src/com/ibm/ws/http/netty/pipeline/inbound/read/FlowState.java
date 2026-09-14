/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.inbound.read;

import java.util.ArrayDeque;
import java.util.Deque;

import io.netty.util.ReferenceCountUtil;
import io.netty.handler.codec.http.HttpObject;

/**
 * This is used by the {@link ReadFlowHandler} to keep track of the state of read
 * I/O flow. The state is helpful to determine if and when the read flow handler
 * should invoke the pipeline to read more data from the channel when auto-read is
 * disabled.
 *
 * <h3>Exchange serialisation</h3>
 * HTTP/1.1 pipelining requires that responses are sent in request order. A new
 * exchange (request B) must not reach application dispatch until the previous
 * exchange (request A) has:
 * <ol>
 *   <li>received its complete inbound body ({@code LastHttpContent} observed), and</li>
 *   <li>sent its complete outbound response (terminal {@code LastHttpContent} write
 *       succeeded).</li>
 * </ol>
 *
 * The fields {@link #activeExchangeId} and {@link #pendingAdmissionQueue} implement
 * this invariant. All access to these fields must be confined to the Netty event loop
 * for the channel that owns this state.
 */
public class FlowState {

    // -----------------------------------------------------------------------
    // Existing read-scheduling flags (all accessed on the event loop)
    // -----------------------------------------------------------------------

    private volatile boolean bodyReadWanted;
    private volatile boolean headRequest;
    private volatile boolean keepAliveAllowed;
    private volatile boolean peerInputShutdown;
    private volatile boolean quiescing;
    private volatile boolean readAgain;
    private volatile boolean readPending;
    private volatile boolean requestConsumed;
    private volatile boolean responseInFlight;
    private volatile boolean stopReading;

    // -----------------------------------------------------------------------
    // Exchange-serialisation state (event-loop confined; no volatile needed)
    // -----------------------------------------------------------------------

    /**
     * Monotonically-increasing counter. Incremented each time an {@code HttpRequest}
     * is admitted to application dispatch. Completion callbacks carry the exchange id
     * that was active when they were installed; stale callbacks (from a recycled
     * connection or retried write) are ignored by comparing to this value.
     */
    private long activeExchangeId = 0L;

    /**
     * {@code HttpRequest} objects (and associated {@code HttpContent} fragments)
     * that have been decoded from the wire but have not yet been admitted to
     * application dispatch because a prior exchange is still in flight.
     *
     * Invariants:
     * <ul>
     *   <li>Entries are held in FIFO order.</li>
     *   <li>Reference counts are owned by this queue; {@link #releaseQueue()} must
     *       be called on channel closure or handler removal.</li>
     *   <li>Access is confined to the event loop.</li>
     * </ul>
     */
    private final Deque<HttpObject> pendingAdmissionQueue = new ArrayDeque<>(4);

    /**
     * {@code true} while the pending-admission queue is being drained. Guards
     * against recursive or reentrant admission when a completion callback triggers
     * a drain that itself calls code capable of triggering another drain.
     */
    private boolean draining = false;

    /**
     * Set to {@code true} when any non-terminal outbound write (headers or body
     * chunk) for the active exchange fails. Even if the terminal
     * {@code LastHttpContent} flush later succeeds, a poisoned exchange must not
     * be reused; {@link #onResponseComplete} checks this flag first.
     *
     * <p>Reset to {@code false} by {@link #nextExchangeId()} when a new exchange
     * begins, so the flag never leaks across exchanges.
     */
    private boolean exchangeWriteFailed = false;

    /**
     * FlowState constructor.
     */
    public FlowState() {
        this.bodyReadWanted = false;
        this.headRequest = false;
        this.keepAliveAllowed = true;
        this.peerInputShutdown = false;
        this.quiescing = false;
        this.readAgain = false;
        this.readPending = false;
        this.requestConsumed = true;
        this.responseInFlight = false;
        this.stopReading = false;
    }

    // -----------------------------------------------------------------------
    // Existing accessors
    // -----------------------------------------------------------------------

    public boolean isBodyReadWanted() {
        return bodyReadWanted;
    }

    /**
     * @return true if the current request is a HEAD request (no body expected).
     */
    public boolean isHeadRequest() {
        return headRequest;
    }

    /**
     * @return true if the connection is allowed to remain open after the current
     *         response, based on the response headers and the server Keep-Alive
     *         policy.
     */
    public boolean isKeepAliveAllowed() {
        return keepAliveAllowed;
    }

    /**
     * @return true if the peer has signaled that it is closed and no longer writing
     *         data. This is used to determine if the connection should be closed
     *         after the current response is fully processed.
     */
    public boolean isPeerInputShutdown() {
        return peerInputShutdown;
    }

    public boolean isQuiescing() {
        return quiescing;
    }

    public boolean isReadAgain() {
        return readAgain;
    }

    /**
     * @return true if the {@link ReadFlowHandler} has already issued a read that
     *         has not yet been marked complete via the channel read complete event.
     */
    public boolean isReadPending() {
        return readPending;
    }

    /**
     * @return true if the current request has been fully consumed (body read to
     *         completion) or when no body is expected.
     */
    public boolean isRequestConsumed() {
        return requestConsumed;
    }

    /**
     * @return if a response has been committed and remains true until the final
     *         write completes.
     */
    public boolean isResponseInFlight() {
        return responseInFlight;
    }

    public boolean stoppedReading() {
        return stopReading;
    }

    public void setBodyReadWanted(boolean bodyReadWanted) {
        this.bodyReadWanted = bodyReadWanted;
    }

    public void setHeadRequest(boolean headRequest) {
        this.headRequest = headRequest;
    }

    public void setKeepAliveAllowed(boolean keepAliveAllowed) {
        this.keepAliveAllowed = keepAliveAllowed;
    }

    public void setPeerInputShutdown(boolean peerInputShutdown) {
        this.peerInputShutdown = peerInputShutdown;
    }

    public void setQuiescing(boolean quiescing) {
        this.quiescing = quiescing;
    }

    public void setReadAgain(boolean readAgain) {
        this.readAgain = readAgain;
    }

    public void setReadPending(boolean readPending) {
        this.readPending = readPending;
    }

    public void setRequestConsumed(boolean requestConsumed) {
        this.requestConsumed = requestConsumed;
    }

    public void setResponseInFlight(boolean responseInFlight) {
        this.responseInFlight = responseInFlight;
    }

    public void setStopReading(boolean stopReading) {
        this.stopReading = stopReading;
    }

    // -----------------------------------------------------------------------
    // Exchange-serialisation accessors (event-loop confined)
    // -----------------------------------------------------------------------

    /**
     * Returns the id of the currently active exchange. The id is incremented each
     * time an {@code HttpRequest} is admitted; completion callbacks should capture
     * this value and compare it before acting to detect stale notifications.
     */
    public long getActiveExchangeId() {
        return activeExchangeId;
    }

    /**
     * Allocates a new exchange id, records it as the active exchange, resets the
     * per-exchange write-failure flag, and returns the new value. Must be called
     * on the event loop, immediately before the {@code HttpRequest} is forwarded
     * downstream.
     */
    public long nextExchangeId() {
        exchangeWriteFailed = false;
        return ++activeExchangeId;
    }

    /**
     * Returns {@code true} if any non-terminal write for the active exchange has
     * already failed. When this is set, a later successful terminal write must
     * not reuse the connection.
     */
    public boolean isExchangeWriteFailed() {
        return exchangeWriteFailed;
    }

    /**
     * Poisons the active exchange so that its terminal write cannot trigger
     * connection reuse even if the flush promise itself succeeds.
     */
    public void setExchangeWriteFailed() {
        this.exchangeWriteFailed = true;
    }

    /**
     * Returns {@code true} if there are requests waiting for admission.
     */
    public boolean hasPendingAdmission() {
        return !pendingAdmissionQueue.isEmpty();
    }

    /**
     * Enqueues an {@code HttpObject} that arrived while an exchange was active.
     * The caller must have already retained the object (or it must not need
     * releasing). Ownership transfers to this queue.
     *
     * @param obj the {@code HttpObject} to park; must not be null.
     */
    public void enqueuePending(HttpObject obj) {
        pendingAdmissionQueue.addLast(obj);
    }

    /**
     * Removes and returns the next {@code HttpObject} from the pending queue, or
     * {@code null} if the queue is empty. The caller takes ownership of the
     * returned object's reference count.
     */
    public HttpObject pollPending() {
        return pendingAdmissionQueue.pollFirst();
    }

    /**
     * Peek at the head of the pending queue without removing it.
     */
    public HttpObject peekPending() {
        return pendingAdmissionQueue.peekFirst();
    }

    /**
     * Returns {@code true} if the pending-admission drain loop is active. Used
     * to prevent recursive re-entrance.
     */
    public boolean isDraining() {
        return draining;
    }

    /**
     * Sets the draining flag. Must be called on the event loop.
     */
    public void setDraining(boolean draining) {
        this.draining = draining;
    }

    /**
     * Releases all reference-counted objects held in the pending-admission queue.
     * Must be called when the channel becomes inactive or the handler is removed.
     */
    public void releaseQueue() {
        HttpObject obj;
        while ((obj = pendingAdmissionQueue.pollFirst()) != null) {
            ReferenceCountUtil.safeRelease(obj);
        }
    }
}
