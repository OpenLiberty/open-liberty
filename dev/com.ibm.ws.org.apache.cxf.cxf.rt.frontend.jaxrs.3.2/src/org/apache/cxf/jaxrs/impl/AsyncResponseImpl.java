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
package org.apache.cxf.jaxrs.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationCallback;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs21.component.LibertyJaxRsThreadPoolAdapter;

public class AsyncResponseImpl implements AsyncResponse, ContinuationCallback {

    private Continuation cont;
    private final Message inMessage;
    private TimeoutHandler timeoutHandler;
    private volatile boolean initialSuspend;
    private volatile boolean cancelled;
    private volatile boolean done;
    private volatile boolean resumedByApplication;
    private volatile Long pendingTimeout;

    private final List<CompletionCallback> completionCallbacks = new LinkedList<>();
    private final List<ConnectionCallback> connectionCallbacks = new LinkedList<>();
    private Throwable unmappedThrowable;
    //Liberty code change start
    //defect 168372
    protected ScheduledFuture timeoutFuture; // this is to get around TCK tests that call setTimeout in a separate thread which is illegal.
    protected ScheduledExecutorService asyncScheduler;
    //Liberty code change end

    public AsyncResponseImpl(Message inMessage) {
        inMessage.put(AsyncResponse.class, this);
        inMessage.getExchange().put(ContinuationCallback.class, this);
        this.inMessage = inMessage;
        //Liberty code change start
        //defect 168372
        //Use Liberty scheduler service
        asyncScheduler = LibertyJaxRsThreadPoolAdapter.getScheduledexecutorserviceref().getService();
        //Libert code change end

        initContinuation();
    }

    @Override
    public boolean resume(Object response) {
        return doResume(response);
    }

    @Override
    public boolean resume(Throwable response) {
        return doResume(response);
    }

    private boolean isCancelledOrNotSuspended() {
        return isCancelled() || !isSuspended();
    }

    private boolean doResume(Object response) {
        if (isCancelledOrNotSuspended()) {
            return false;
        }
        return doResumeFinal(response);
    }
    private synchronized boolean doResumeFinal(Object response) {
        inMessage.getExchange().put(AsyncResponse.class, this);
        cont.setObject(response);
        resumedByApplication = true;
        if (!initialSuspend) {
            cont.resume();
        } else {
            initialSuspend = false;
        }
        return true;
    }

    @Override
    public boolean cancel() {
        return doCancel(null);
    }

    @Override
    public boolean cancel(int retryAfter) {
        return doCancel(Integer.toString(retryAfter));
    }

    @Override
    public boolean cancel(Date retryAfter) {
        return doCancel(HttpUtils.getHttpDateFormat().format(retryAfter));
    }

    private boolean doCancel(String retryAfterHeader) {
        if (cancelled) {
            return true;
        }
        if (!isSuspended()) {
            return false;
        }

        //defect 168367
        if (resumedByApplication) {
            return false;
        }

        cancelled = true;
        ResponseBuilder rb = Response.status(503);
        if (retryAfterHeader != null) {
            rb.header(HttpHeaders.RETRY_AFTER, retryAfterHeader);
        }
        doResumeFinal(rb.build());
        return cancelled;
    }

    @Override
    public boolean isSuspended() {
        if (cancelled || resumedByApplication) {
            return false;
        }
        return initialSuspend || cont.isPending();
    }

    @Override
    public synchronized boolean isCancelled() {
        return cancelled;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public synchronized boolean setTimeout(long time, TimeUnit unit) throws IllegalStateException {
        if (isCancelledOrNotSuspended()) {
            return false;
        }
        //Liberty Code change start
        long timeout = TimeUnit.MILLISECONDS.convert(time, unit);
        //defect 168372
        //cont is pending means the setTimeout call is from a second request
        if (cont.isPending()) {
            // this is to get around TCK tests that call setTimeout in a separate thread which is illegal.
            if (timeoutFuture != null && !timeoutFuture.cancel(false)) {
                return false;
            }
            //Handle timeout with Liberty scheduler service, not AsyncContext
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    handleTimeout();
                }
            };
            //Liberty.. the folloing two lines are commented out
            //per 232620
//            pendingTimeout = timeout;
//            cont.resume();
            timeoutFuture = asyncScheduler.schedule(task, time, unit);
            return true;
        } else {
            //Liberty code change end
            setAsyncResponseOnExchange();
            initialSuspend = false;
            cont.suspend(timeout);
            return true;
        }
    }

    private void setAsyncResponseOnExchange() {
        inMessage.getExchange().put(AsyncResponse.class, this);
    }

    @Override
    public void setTimeoutHandler(TimeoutHandler handler) {
        timeoutHandler = handler;
    }

    @Override
    public Collection<Class<?>> register(Class<?> callback) throws NullPointerException {
        return register(callback, new Class<?>[]{}).get(callback);
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Class<?> callback, Class<?>... callbacks)
        throws NullPointerException {
        try {
            Object[] extraCallbacks = new Object[callbacks.length];
            for (int i = 0; i < callbacks.length; i++) {
                extraCallbacks[i] = callbacks[i].newInstance();
            }
            return register(callback.newInstance(), extraCallbacks);
        } catch (NullPointerException e) {
            throw e;
        } catch (Throwable t) {
            return Collections.emptyMap();
        }

    }

    @Override
    public Collection<Class<?>> register(Object callback) throws NullPointerException {
        return register(callback, new Object[]{}).get(callback.getClass());
    }

    @Override
    public Map<Class<?>, Collection<Class<?>>> register(Object callback, Object... callbacks)
        throws NullPointerException {
        Map<Class<?>, Collection<Class<?>>> map = new HashMap<>();

        Object[] allCallbacks = new Object[1 + callbacks.length];
        allCallbacks[0] = callback;
        System.arraycopy(callbacks, 0, allCallbacks, 1, callbacks.length);

        for (int i = 0; i < allCallbacks.length; i++) {
            if (allCallbacks[i] == null) {
                throw new NullPointerException();
            }
            Class<?> callbackCls = allCallbacks[i].getClass();
            Collection<Class<?>> knownCallbacks = map.get(callbackCls);
            if (knownCallbacks == null) {
                knownCallbacks = new HashSet<>();
                map.put(callbackCls, knownCallbacks);
            }

            if (allCallbacks[i] instanceof CompletionCallback) {
                knownCallbacks.add(CompletionCallback.class);
                completionCallbacks.add((CompletionCallback)allCallbacks[i]);
            } else if (allCallbacks[i] instanceof ConnectionCallback) {
                knownCallbacks.add(ConnectionCallback.class);
                connectionCallbacks.add((ConnectionCallback)allCallbacks[i]);
            }
        }
        return map;
    }

    @Override
    public void onComplete() {
        done = true;
        updateCompletionCallbacks(unmappedThrowable);
    }

    @Override
    public void onError(Throwable error) {
        updateCompletionCallbacks(error);
    }

    private void updateCompletionCallbacks(Throwable error) {
        Throwable actualError = error instanceof Fault ? ((Fault)error).getCause() : error;
        for (CompletionCallback completionCallback : completionCallbacks) {
            completionCallback.onComplete(actualError);
        }
    }

    @Override
    public void onDisconnect() {
        for (ConnectionCallback connectionCallback : connectionCallbacks) {
            connectionCallback.onDisconnect(this);
        }
    }

    public synchronized boolean suspendContinuationIfNeeded() {
        if (!resumedByApplication && !isDone() && !cont.isPending() && !cont.isResumed()) {
            cont.suspend(AsyncResponse.NO_TIMEOUT);
            initialSuspend = false;
            return true;
        }
        return false;
    }

    @SuppressWarnings("resource") // Response that is built here shouldn't be closed here
    public Object getResponseObject() {
        Object obj = cont.getObject();
        if (!(obj instanceof Response) && !(obj instanceof Throwable)) {
            if (obj == null) {
                obj = Response.noContent().build();
            } else {
                obj = Response.ok().entity(obj).build();
            }
        }
        return obj;
    }

    public boolean isResumedByApplication() {
        return resumedByApplication;
    }

    public synchronized void handleTimeout() {
        if (!resumedByApplication) {
            if (pendingTimeout != null) {
                setAsyncResponseOnExchange();
                cont.suspend(pendingTimeout);
                pendingTimeout = null;
            } else if (timeoutHandler != null) {
                timeoutHandler.handleTimeout(this);
            } else {
                cont.setObject(new ServiceUnavailableException());
            }
            //Liberty code change start
            //defect 168372
            //cont isPending means the timeout is come from a second setTimeout request, need to resume the continuation and complete it.
            if (cont.isPending()) {
                cont.resume();
                onComplete();
            }
            //Liberty code change end
        }
    }

    private void initContinuation() {
        ContinuationProvider provider =
            (ContinuationProvider)inMessage.get(ContinuationProvider.class.getName());
        if (provider == null) {
            throw new IllegalArgumentException(
                "Continuation not supported. " 
                + "Please ensure that all servlets and servlet filters support async operations");
        }
        cont = provider.getContinuation();
        initialSuspend = true;
    }

    public void prepareContinuation() {
        initContinuation();
    }

    public void setUnmappedThrowable(Throwable t) {
        unmappedThrowable = t;
    }
    public void reset() {
        cont.reset();
    }

}
