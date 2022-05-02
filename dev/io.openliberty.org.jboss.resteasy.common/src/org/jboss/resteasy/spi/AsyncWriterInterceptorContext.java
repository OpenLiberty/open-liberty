/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//Port Resteasy change in https://issues.redhat.com/browse/RESTEASY-3161 - Liberty change

package org.jboss.resteasy.spi;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.InterceptorContext;

import java.util.concurrent.CompletionStage;

/**
 * Context for {@link AsyncWriterInterceptor} which supports async IO.
 */
public interface AsyncWriterInterceptorContext extends InterceptorContext {

    /**
     * Proceed to the next interceptor in the chain.
     *
     * Interceptors MUST explicitly call this method to continue the execution chain;
     * the call to this method in the last interceptor of the chain will invoke
     * the wrapped {@link AsyncMessageBodyWriter#asyncWriteTo} method.
     *
     * @return a {@link CompletionStage} indicating completion.
     * @throws java.io.IOException if an IO error arises or is thrown by the wrapped
     *                             {@code AsyncMessageBodyWriter.asyncWriteTo} method, in the returned {@link CompletionStage}.
     * @throws javax.ws.rs.WebApplicationException
     *                             thrown by the wrapped {@code AsyncMessageBodyWriter.asyncWriteTo} method, in the returned {@link CompletionStage}.
     */
    CompletionStage<Void> asyncProceed();

    /**
     * Get object to be written as HTTP entity.
     *
     * @return object to be written as HTTP entity.
     */
    Object getEntity();

    /**
     * Update object to be written as HTTP entity.
     *
     * @param entity new object to be written.
     */
    void setEntity(Object entity);

    /**
     * Get the async output stream for the object to be written. The runtime
     * is responsible for closing the output stream.
     *
     * @return async output stream for the object to be written.
     */
    AsyncOutputStream getAsyncOutputStream();

    /**
     * Set a new async output stream for the object to be written. For example, by wrapping
     * it with another async output stream. The runtime is responsible for closing
     * the async output stream that is set.
     *
     * @param os new async output stream for the object to be written.
     */
    void setAsyncOutputStream(AsyncOutputStream os);

    /**
     * Get mutable map of HTTP headers.
     *
     * @return map of HTTP headers.
     */
    MultivaluedMap<String, Object> getHeaders();
}
