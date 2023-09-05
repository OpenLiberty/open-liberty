/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2023 Red Hat, Inc., and individual contributors
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

package org.jboss.resteasy.core.registry;

import java.lang.reflect.Method;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResourceInvoker;
import org.jboss.resteasy.spi.statistics.MethodStatisticsLogger;

/**
 * A resource invoker which simply returns the given response.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class ConstantResourceInvoker implements ResourceInvoker {
    private final Response response;
    private volatile MethodStatisticsLogger logger;

    ConstantResourceInvoker(final Response response) {
        this.response = response;
    }

    @Override
    public Response invoke(final HttpRequest request, final HttpResponse response) {
        return this.response;
    }

    @Override
    public Response invoke(final HttpRequest request, final HttpResponse response, final Object target) {
        return this.response;
    }

    @Override
    public Method getMethod() {
        return null;
    }

    @Override
    public boolean hasProduces() {
        return false;
    }

    @Override
    public void setMethodStatisticsLogger(final MethodStatisticsLogger msLogger) {
        this.logger = msLogger;
    }

    @Override
    public MethodStatisticsLogger getMethodStatisticsLogger() {
        return logger;
    }
}
