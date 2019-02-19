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

package org.apache.cxf.transport.http;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationCallback;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;

/**
 *
 */
public class Servlet3ContinuationProvider implements ContinuationProvider {
    HttpServletRequest req;
    HttpServletResponse resp;
    Message inMessage;
    Servlet3Continuation continuation;

    public Servlet3ContinuationProvider(HttpServletRequest req,
                                        HttpServletResponse resp,
                                        Message inMessage) {
        this.inMessage = inMessage;
        this.req = req;
        this.resp = resp;
    }

    public void complete() {
        if (continuation != null) {
            continuation.reset();
            continuation = null;
        }
    }


    /** {@inheritDoc}*/
    public Continuation getContinuation() {
        if (inMessage.getExchange().isOneWay()) {
            return null;
        }

        if (continuation == null) {
            continuation = new Servlet31Continuation();
        } else {
            continuation.startAsyncAgain();
        }
        return continuation;
    }

    public class Servlet3Continuation implements Continuation, AsyncListener {
        private static final String BLOCK_RESTART = "org.apache.cxf.continuation.block.restart";
        AsyncContext context;
        volatile boolean isNew = true;
        volatile boolean isResumed;
        volatile boolean isPending;
        volatile boolean isComplete;
        volatile boolean isTimeout;
        volatile Object obj;
        private ContinuationCallback callback;
        private boolean blockRestart;
        
        public Servlet3Continuation() {
            req.setAttribute(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE,
                             inMessage.getExchange().getInMessage());
            callback = inMessage.getExchange().get(ContinuationCallback.class);
            blockRestart = PropertyUtils.isTrue(inMessage.getContextualProperty(BLOCK_RESTART));
            context = req.startAsync();
            context.addListener(this);
        }

        void startAsyncAgain() {
            if (blockRestart) {
                return;
            }
            AsyncContext old = context;
            try {
                context = req.startAsync();
                context.addListener(this);
                isComplete = false;
            } catch (IllegalStateException ex) {
                context = old;
            }
        }

        public boolean suspend(long timeout) {
            if (isPending && timeout != 0) {
                long currentTimeout = context.getTimeout();
                timeout = currentTimeout + timeout;
            } else {
                isPending = true;
            }
            isNew = false;
            isResumed = false;

            context.setTimeout(timeout);

            updateMessageForSuspend();
            return true;
        }
        protected void updateMessageForSuspend() {
            inMessage.getExchange().getInMessage().getInterceptorChain().suspend();
        }
        public void redispatch() {
            if (!isComplete) {
                context.dispatch();
            }
        }
        public void resume() {
            isResumed = true;
            isPending = false;
            redispatch();
        }

        public void reset() {
            isComplete = true;
            try {
                context.complete();
            } catch (IllegalStateException ex) {
                // ignore
            }
            isPending = false;
            isResumed = false;
            isNew = false;
            isTimeout = false;
            obj = null;
            if (callback != null) {
                final Exception ex = inMessage.getExchange().get(Exception.class);
                Throwable cause = isCausedByIO(ex);

                if (cause != null && isClientDisconnected(cause)) {
                    callback.onDisconnect();
                }
            }
        }

        public boolean isNew() {
            return isNew;
        }

        public boolean isPending() {
            return isPending;
        }

        public boolean isResumed() {
            return isResumed;
        }

        public Object getObject() {
            return obj;
        }

        public void setObject(Object o) {
            obj = o;
        }

        public void onComplete(AsyncEvent event) throws IOException {
            inMessage.getExchange().getInMessage()
                .remove(AbstractHTTPDestination.CXF_CONTINUATION_MESSAGE);
            if (callback != null) {
                final Exception ex = inMessage.getExchange().get(Exception.class);
                if (ex == null) {
                    callback.onComplete();
                } else {
                    callback.onError(ex);
                }
            }
            isResumed = false;
            isPending = false;
        }
        public void onError(AsyncEvent event) throws IOException {
            if (callback != null) {
                callback.onError(event.getThrowable());
            }
        }
        public void onStartAsync(AsyncEvent event) throws IOException {
        }
        public void onTimeout(AsyncEvent event) throws IOException {
            resume();
            isTimeout = true;
        }

        private Throwable isCausedByIO(final Exception ex) {
            Throwable cause = ex;

            while (cause != null && !(cause instanceof IOException)) {
                cause = cause.getCause();
            }

            return cause;
        }

        private boolean isClientDisconnected(Throwable ex) {
            String exName = (String)inMessage.getContextualProperty("disconnected.client.exception.class");
            if (exName != null) {
                return exName.equals(IOException.class.getName()) || exName.equals(ex.getClass().getName());
            }
            return false;
        }

        @Override
        public boolean isReadyForWrite() {
            return true;
        }

        protected ServletOutputStream getOutputStream() {
            try {
                return resp.getOutputStream();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public boolean isTimeout() {
            return isTimeout;
        }
    }
    public class Servlet31Continuation extends Servlet3Continuation {
        public Servlet31Continuation() {
        }

        @Override
        protected void updateMessageForSuspend() {
            Message currentMessage = PhaseInterceptorChain.getCurrentMessage();
            if (currentMessage.get(WriteListener.class) != null) {
                // CXF Continuation WriteListener will likely need to be introduced
                // for NIO supported with non-Servlet specific mechanisms
                getOutputStream().setWriteListener(currentMessage.get(WriteListener.class));
                currentMessage.getInterceptorChain().suspend();
            } else {
                inMessage.getExchange().getInMessage().getInterceptorChain().suspend();
            }
        }
        
        @Override
        public boolean isReadyForWrite() {
            return getOutputStream().isReady();
        }
    }
}