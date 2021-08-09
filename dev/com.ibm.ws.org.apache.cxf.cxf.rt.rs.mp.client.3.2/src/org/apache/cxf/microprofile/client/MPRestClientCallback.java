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

package org.apache.cxf.microprofile.client;

import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.client.InvocationCallback;

import org.apache.cxf.jaxrs.client.JaxrsClientCallback;
import org.apache.cxf.message.Message;

public class MPRestClientCallback<T> extends JaxrsClientCallback<T> {

    private final ExecutorService executor;
    private Throwable exception;

    public MPRestClientCallback(InvocationCallback<T> handler,
                                Message outMessage,
                                Class<?> responseClass,
                                Type outGenericType) {
        super(handler, responseClass, outGenericType);
        ExecutorService es = outMessage.get(ExecutorService.class);
        if (es == null) {
            es = AccessController.doPrivileged((PrivilegedAction<ExecutorService>)() -> {
                return ForkJoinPool.commonPool();
            });
        }
        executor = es;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Future<T> createFuture() {
        return (Future<T>)CompletableFuture.supplyAsync(() -> {
            synchronized(this) {
                if (!isDone()) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        throw new CompletionException(e);
                    }
                }
            }
            if (exception != null) {
                throw new CompletionException(exception);
            }
            if (isCancelled()) {
                throw new CancellationException();
            }
            if (!isDone()) {
                throw new IllegalStateException("CompletionStage has been notified, indicating completion, but is not completed.");
            }
            try {
                return get()[0];
            } catch (InterruptedException | ExecutionException e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    @Override
    public void handleException(Map<String, Object> ctx, Throwable ex) {
        this.exception = ex;
        super.handleException(ctx, ex);
    }
}