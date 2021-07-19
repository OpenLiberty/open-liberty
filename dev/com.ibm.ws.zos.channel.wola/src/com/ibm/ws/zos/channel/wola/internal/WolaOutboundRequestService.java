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
package com.ibm.ws.zos.channel.wola.internal;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;

/**
 * Service for sending outbound WOLA requests from Liberty to a WOLA client.
 *
 * This guy takes care of matching up outbound requests with inbound responses
 * and posting the caller when the response is ready (via a Future).
 *
 */
public class WolaOutboundRequestService {

    /**
     * The connection over which all outbound requests are made.
     */
    private final WolaConnLink wolaConnLink;

    /**
     * Atomic management of the requestIds used by this connection.
     */
    private final AtomicInteger nextRequestId = new AtomicInteger(0);

    /**
     * Map of requestIds to WolaResponseFutures. For matching responses
     * to requests.
     */
    private final ConcurrentMap<Integer, WolaResponseFuture> requestIdMap = new ConcurrentHashMap<Integer, WolaResponseFuture>();

    /**
     * CTOR.
     */
    protected WolaOutboundRequestService(WolaConnLink wolaConnLink) {
        this.wolaConnLink = wolaConnLink;
    }

    /**
     * Sends the given request outbound to the wola client. In this case
     * the wola client must have previously registered an available service.
     *
     * The request is sent asynchronously. A Future is returned to the caller.
     * The Future will be posted when the response comes back. The caller can wait
     * for the response by calling Future.get().
     *
     * @param wolaMessage - the request message
     *
     * @return A Future<WolaMessage>, which will contain the response message
     *         when the response is ready.
     *
     * @throws IOException - if the send failed.
     */
    protected Future<WolaMessage> sendRequest(WolaMessage wolaRequest) throws IOException {

        // Get the next requestId and add it to the wola message
        int requestId = nextRequestId.incrementAndGet();
        wolaRequest.setRequestId(requestId);

        // Create a future (for the response) and add it to the requestIdMap.
        WolaResponseFuture future = new WolaResponseFuture(wolaRequest);
        WolaResponseFuture prev = requestIdMap.putIfAbsent(requestId, future);

        if (prev != null) {
            // Yikes! There's already a future waiting on this requestId.
            // This shouldn't happen -- we just atomically incremented the
            // requestId.  But if it does (for whatever reason), throw an
            // exception back to the caller.
            throw new IOException("Duplicate request ID.  There already exists a WOLA response Future for this requestId: " + requestId);
        }

        try {
            // Note: this will throw a RuntimeException if the connection is closing.
            // It is not possible to get into a timing window situation where an
            // outbound request is queued and a Future returned, but is never posted
            // because the connection has been closed. If the connLink has been destroyed
            // at any time prior to now, then this line of code will throw an exception.
            // Otherwise when the connLink is destroyed it will postAll Futures in the map.
            // Note that postAll is called *after* the code that will cause the RuntimeException
            // below. So we should be covered.
            wolaConnLink.getDeviceLinkChannelAccessor().syncWrite(wolaRequest.toByteBuffer());
        } catch (IOException e) {
            requestIdMap.remove(requestId);
            throw e;
        }

        return future;
    }

    /**
     * Find the WolaResponseFuture associated with the requestId in the given
     * wolaResponse message and post the Future.
     *
     * @param wolaResponse - the response message
     *
     * @return the Future that was waiting on this response.
     */
    public Future<WolaMessage> postResponse(WolaMessage wolaResponse) {

        WolaResponseFuture future = requestIdMap.remove(wolaResponse.getRequestId());

        if (future != null) {
            future.post(wolaResponse);
        } else {
            // There's no future in the map for the given requestId.
            // Don't know why that happened.
            // TODO: log a message or something...
        }

        return future;
    }

    /**
     * Post all Futures in the requestIdMap with the given exception.
     * This method is called when the connection is closing.
     *
     * @param e - The exception to post with
     */
    public void postAll(Exception e) {
        for (WolaResponseFuture future : requestIdMap.values()) {
            future.post(e);
        }
        requestIdMap.clear();
    }

}

/**
 * Future class returned by sendRequest. The future is posted when the response
 * is received. The calling thread can wait on this future for the response.
 */
class WolaResponseFuture implements Future<WolaMessage> {

    /**
     * Handles all the messy synchronization details between posting this future
     * (done in postResponse) and Future.get.
     */
    private final BlockingQueue<WolaMessage> wolaMessageQueue = new ArrayBlockingQueue<WolaMessage>(1);

    /**
     * Indicates whether the operation has completed (for Future.isDone).
     */
    private boolean isItDone = false;

    /**
     * Indicates whether the operation has been cancelled (for Future.isCancelled).
     */
    private boolean isCancelled = false;

    /**
     * If the request suffered some sort of exception, it gets set into this field
     * and will be thrown back to the caller when he calls get().
     */
    private Exception ex;

    /**
     * A ref to the request message associated with this future.
     * Note: this field isn't used for anything, but may be useful for debugging.
     */
    @SuppressWarnings("unused")
    private final WolaMessage wolaRequest;

    /**
     * CTOR.
     */
    protected WolaResponseFuture(WolaMessage wolaRequest) {
        this.wolaRequest = wolaRequest;
    }

    /**
     * @return the wola response message
     *
     * @throws ExecutionException - if a failure occurred. The cause of the failure can be found
     *                                via ExecutionException.getCause().
     */
    @Override
    public WolaMessage get() throws InterruptedException, ExecutionException {
        WolaMessage wolaResponse = wolaMessageQueue.take();
        if (ex != null) {
            throw new ExecutionException(ex);
        }
        return wolaResponse;
    }

    /**
     * Post the Future with a wola response message. This will wake up
     * any thread waiting on get().
     */
    public synchronized void post(WolaMessage wolaResponse) {
        if (isItDone == false) {
            wolaMessageQueue.add(wolaResponse);
            isItDone = true;
        }
    }

    /**
     * Post the Future with an exception. Any callers of get() will get an
     * ExecutionException(e).
     */
    public synchronized void post(Exception e) {
        if (isItDone == false) {
            ex = (e != null) ? e : new IOException("Unknown failure. Possibly the connection is closing");
            wolaMessageQueue.add(new WolaMessage(null)); // Need to put something here to wake up Future.get.
            isItDone = true;
        }
    }

    @Override
    public boolean isDone() {
        return isItDone;
    }

    /**
     * This is called by the InterruptObject if requestTiming-1.0 is configured.
     */
    @Override
    public synchronized boolean cancel(boolean mayInterruptIfRunning) {
        if (isItDone == false) {
            post(new IOException("This thread was interrupted before the response was ready. The request may still be running."));
            isCancelled = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public WolaMessage get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException();
    }
}
