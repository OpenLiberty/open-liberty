/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.microprofile.telemetry10.internal.cdi;

import io.openliberty.microprofile.telemetry.internal.common.cdi.AbstractWithSpanInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.annotation.support.MethodSpanAttributesExtractor;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@WithSpan
@Interceptor
@Priority(100)
public class WithSpanInterceptor extends AbstractWithSpanInterceptor {

    @Inject
    public WithSpanInterceptor(OpenTelemetry openTelemetry) {
        super(openTelemetry);
    }

    //OpenTelemetry renamed MethodSpanAttributesExtractor.newInstance to MethodSpanAttributesExtractor.create
    //So we move this method to non-common code.
    @Override
    protected MethodSpanAttributesExtractor<InvocationContext, Void> getMethodSpanAttributesExtractor() {
        return MethodSpanAttributesExtractor.newInstance(InvocationContext::getMethod,
                                                         getNewWithSpanParameterAttributeNamesExtractor(),
                                                         InvocationContext::getParameters);
    }
}
