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

package org.apache.cxf.jaxws;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.apache.cxf.endpoint.ClientCallback;

class JaxwsClientCallback<T> extends ClientCallback {
    final AsyncHandler<T> handler;
    final Object proxy;

    JaxwsClientCallback(final AsyncHandler<T> handler, Object p) {
        this.handler = handler;
        this.proxy = p;
    }
    public void handleResponse(Map<String, Object> ctx, Object[] res) {
        context = ctx;
        
        // The handler has to be called *before* future completes
        if (handler != null) {
            handler.handleResponse(new Response<T>() {
                protected boolean cancelled;
                
                public Map<String, Object> getContext() {
                    return context;
                }

                public boolean cancel(boolean mayInterruptIfRunning) {
                    cancelled = true;
                    return true;
                }

                @SuppressWarnings("unchecked")
                public T get() throws InterruptedException, ExecutionException {
                    return (T)res[0];
                }

                @SuppressWarnings("unchecked")
                public T get(long timeout, TimeUnit unit) throws InterruptedException,
                    ExecutionException, TimeoutException {
                    return (T)res[0];
                }

                public boolean isCancelled() {
                    return cancelled;
                }

                public boolean isDone() {
                    return true;
                }

            });
        }
        
        delegate.complete(res);
        
        synchronized (this) {
            notifyAll();
        }
    }

    @Override
    public void handleException(Map<String, Object> ctx, final Throwable ex) {
        context = ctx;
        
        if (handler != null) {
            handler.handleResponse(new Response<T>() {
                protected boolean cancelled;

                public Map<String, Object> getContext() {
                    return context;
                }

                public boolean cancel(boolean mayInterruptIfRunning) {
                    cancelled = true;
                    return true;
                }

                public T get() throws InterruptedException, ExecutionException {
                    throw new ExecutionException(ex);
                }

                public T get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {

                    throw new ExecutionException(ex);
                }

                public boolean isCancelled() {
                    return cancelled;
                }

                public boolean isDone() {
                    return true;
                }

            });
        }

        // The handler has to be called *before* future completes
        delegate.completeExceptionally(mapThrowable(ex));
        
        synchronized (this) {
            notifyAll();
        }
    }
    
    protected Throwable mapThrowable(Throwable t) {
        return t;
    }
}