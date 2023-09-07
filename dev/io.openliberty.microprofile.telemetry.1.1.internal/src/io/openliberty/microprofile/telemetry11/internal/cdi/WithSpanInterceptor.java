/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry11.internal.cdi;

import io.openliberty.microprofile.telemetry.internal.common.cdi.AbstractWithSpanInterceptor;
import io.openliberty.microprofile.telemetry.internal.common.cdi.MethodRequest;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.annotation.support.MethodSpanAttributesExtractor;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;

@WithSpan
@Interceptor
@Priority(100)
public class WithSpanInterceptor extends AbstractWithSpanInterceptor {
	
    @Inject
    public WithSpanInterceptor(OpenTelemetry openTelemetry) {
        super(openTelemetry);
    }

    @Override
    protected MethodSpanAttributesExtractor<MethodRequest, Void> getMethodSpanAttributesExtractor() {
        return MethodSpanAttributesExtractor.create(MethodRequest::getMethod,
                                                         getNewWithSpanParameterAttributeNamesExtractor(),
                                                         MethodRequest::getArgs);
    }
}
