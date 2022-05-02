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

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.ext.WriterInterceptor;

/**
 * Writer interceptors which support async IO.
 */
public interface AsyncWriterInterceptor extends WriterInterceptor {

    /**
     * Interceptor method wrapping calls to {@link AsyncMessageBodyWriter#asyncWriteTo} method.
     * The parameters of the wrapped method called are available from {@code context}.
     * Implementations of this method SHOULD explicitly call
     * {@link AsyncWriterInterceptorContext#asyncProceed} to invoke the next interceptor in the chain,
     * and ultimately the wrapped {@code AsyncMessageBodyWriter.asyncWriteTo} method.
     *
     * @param context invocation context.
     * @return a {@link CompletionStage} indicating completion
     * @throws java.io.IOException if an IO error arises or is thrown by the wrapped
     *                             {@code AsyncMessageBodyWriter.asyncWriteTo} method, in the returned {@link CompletionStage}.
     * @throws javax.ws.rs.WebApplicationException
     *                             thrown by the wrapped {@code AsyncMessageBodyWriter.asyncWriteTo} method, in the returned {@link CompletionStage}.
     */
    CompletionStage<Void> asyncAroundWriteTo(AsyncWriterInterceptorContext context);
}
