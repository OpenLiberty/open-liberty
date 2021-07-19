/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs.sse;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import org.apache.cxf.common.logging.LogUtils;

public class SseEventSinkImpl implements SseEventSink {
    public static final String BUFFER_SIZE_PROPERTY = "org.apache.cxf.sse.sink.buffer.size";
    
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation [] {};
    private static final Logger LOG = LogUtils.getL7dLogger(SseEventSinkImpl.class);
    private static final int DEFAULT_BUFFER_SIZE = 10000; // buffering 10000 messages

    private final AsyncContext ctx;
    private final MessageBodyWriter<OutboundSseEvent> writer;
    private final Queue<QueuedEvent> buffer;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean dispatching = new AtomicBoolean(false);
    private final AtomicReference<Throwable> throwable = new AtomicReference<>();
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final int bufferSize;

    /**
     * Create new SseEventSink implementation with the default buffer size of 10000
     * SSE events.
     * 
     * @param writer message body writer
     * @param async asynchronous response 
     * @param ctx asynchronous context
     */
    public SseEventSinkImpl(final MessageBodyWriter<OutboundSseEvent> writer, 
            final AsyncResponse async, final AsyncContext ctx) {
        this(writer, async, ctx, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Create new SseEventSink implementation with the configurable SSE events buffer 
     * size.
     * 
     * @param writer message body writer
     * @param async asynchronous response 
     * @param ctx asynchronous context
     * @param bufferSize SSE events buffer size
     */
    public SseEventSinkImpl(final MessageBodyWriter<OutboundSseEvent> writer, 
            final AsyncResponse async, final AsyncContext ctx, final int bufferSize) {
        
        this.writer = writer;
        this.buffer = new ArrayBlockingQueue<>(bufferSize);
        this.ctx = ctx;
        this.bufferSize = bufferSize;

        if (ctx == null) {
            throw new IllegalStateException("Unable to retrieve the AsyncContext for this request. "
                + "Is the Servlet configured properly?");
        }

        ctx.getResponse().setContentType(OutboundSseEventBodyWriter.SERVER_SENT_EVENTS);
        ctx.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                // This callback should be called when dequeue() has encountered an
                // error during the execution and is forced to complete the context.
                close();
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
            }

            @Override
            public void onError(AsyncEvent event) throws IOException {
                // In case of Tomcat, the context is closed automatically when client closes
                // the connection.
                if (throwable.get() != null || throwable.compareAndSet(null, event.getThrowable())) {
                    // This callback should be called when dequeue() has encountered an
                    close();
                }
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
            }
        });
    }

    public AsyncContext getAsyncContext() {
        return ctx;
    }
    
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            LOG.fine("Closing SSE sink now");
            
            // In case we are still dispatching, give the events the chance to be
            // sent over to the consumers. The good example would be sent(event) call,
            // immediately followed by the close() call.
            if (!awaitQueueToDrain(5, TimeUnit.SECONDS)) {
                LOG.warning("There are still SSE events the queue which may not be delivered (closing now)");
            }
            
            if (completed.compareAndSet(false, true)) {
                try {
                    // In case of Tomcat, the context may be already closed (f.e. due to error),
                    // in this case request is set to null.
                    if (ctx.getRequest() != null) {
                        LOG.fine("Completing the AsyncContext");
                        ctx.complete();
                    }
                } catch (final IllegalStateException ex) {
                    LOG.fine("Failed to close the AsyncContext cleanly: " + ex.getMessage());
                }
            }
            
            // Complete all the accepted but not dispatched send request with the
            // error (if any) or signal that sink has been closed already.
            Throwable ex = throwable.get();
            if (ex == null) {
                ex = new IllegalStateException("The sink has been already closed");
            }
            
            QueuedEvent queuedEvent = buffer.poll();
            while (queuedEvent != null) {
                queuedEvent.completion.completeExceptionally(ex);
                queuedEvent = buffer.poll();
            }
        }
    }

    private boolean awaitQueueToDrain(int timeout, TimeUnit unit) {
        final long parkTime = unit.toNanos(timeout) / 20;
        int attempt = 0;
        
        while (dispatching.get() && ++attempt < 20) {
            LockSupport.parkNanos(parkTime);
        }
        
        return buffer.isEmpty();
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        System.out.println("Jim... SseEventSinkImpl.send().... event = " + event);
        final CompletableFuture<?> future = new CompletableFuture<>();

        if (!closed.get() && writer != null) {
            final Throwable ex = throwable.get(); 
            if (ex != null) {
                future.completeExceptionally(ex);
            } else if (buffer.offer(new QueuedEvent(event, future))) {
                if (dispatching.compareAndSet(false, true)) {
                    ctx.start(this::dequeue);
                }
            } else {
                future.completeExceptionally(new IllegalStateException("The buffer is full (" 
                    + bufferSize + "), unable to queue SSE event for send. Please use '" 
                        + BUFFER_SIZE_PROPERTY + "' property to increase the limit."));
            }
        } else {
            future.completeExceptionally(new IllegalStateException(
                "The sink is already closed, unable to queue SSE event for send"));
        }

        return future;
    }

    /**
     * Processes the buffered events and sends the off to the output channel. There  is
     * a special handling for the IOException, which forces the sink to switch to closed 
     * state: 
     *   - when the IOException is detected, the AsyncContext is forcebly closed (unless
     *     it is already closed like in case of the Tomcat)
     *   - all unsent events are completed exceptionally
     *   - all unscheduled events are completed exceptionally (see please close() method)
     *   
     */
    private void dequeue() {
        Throwable error = throwable.get();
        
        try {
            while (true) {
                final QueuedEvent queuedEvent = buffer.poll();
                
                // Nothing queued, release the thread
                if (queuedEvent == null) {
                    break;
                }
                
                final OutboundSseEvent event = queuedEvent.event;
                final CompletableFuture<?> future = queuedEvent.completion;
    
                try {
                    if (error == null) {
                        LOG.fine("Dispatching SSE event over the wire");
                        
                        writer.writeTo(event, event.getClass(), event.getGenericType(), EMPTY_ANNOTATIONS,
                            event.getMediaType(), null, ctx.getResponse().getOutputStream());
                        ctx.getResponse().flushBuffer();
                        
                        LOG.fine("Completing the future successfully");
                        future.complete(null);
                    } else {
                        LOG.fine("Completing the future unsuccessfully (error enountered previously)");
                        future.completeExceptionally(error);
                    }
                } catch (final Exception ex) {
                    // Very likely the connection is closed by the client (but we cannot
                    // detect if for sure, container-specific).
                    if (ex instanceof IOException) {
                        error = (IOException)ex;
                    }
                    
                    LOG.fine("Completing the future unsuccessfully (error enountered)");
                    future.completeExceptionally(ex);
                }
            }
        } finally {
            final boolean shouldComplete = (error != null) && throwable.compareAndSet(null, error);
            dispatching.set(false);
            
            // Ideally, we should be rethrowing the exception here (error) and handle
            // it inside the onError() callback. However, most of the servlet containers
            // do not handle this case properly (and onError() is not called). 
            if (shouldComplete && completed.compareAndSet(false, true)) {
                LOG.fine("Prematurely completing the AsyncContext due to error encountered: " + error);
                // In case of Tomcat, the context is closed automatically when client closes
                // the connection and onError callback will be called (in this case request 
                // is set to null).
                try {
                    LOG.fine("Completing the AsyncContext");
                    // Older versions of Tomcat returned 'null', now the getRequest() throws
                    // IllegalStateException if it is 'null'.
                    if (ctx.getRequest() != null) {
                        ctx.complete();
                    }
                } catch (final IllegalStateException ex) {
                    LOG.fine("Failed to close the AsyncContext cleanly: " + ex.getMessage());
                }
            }
        }
    }

    private static class QueuedEvent {
        private final OutboundSseEvent event;
        private final CompletableFuture<?> completion;

        QueuedEvent(OutboundSseEvent event, CompletableFuture<?> completion) {
            this.event = event;
            this.completion = completion;
        }
    }
}
