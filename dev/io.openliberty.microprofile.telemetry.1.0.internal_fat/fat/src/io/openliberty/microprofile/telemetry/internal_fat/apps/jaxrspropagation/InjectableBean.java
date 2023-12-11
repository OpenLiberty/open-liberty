/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation;

import javax.enterprise.context.ApplicationScoped;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;

/**
 *
 */
@ApplicationScoped
public class InjectableBean {

    //Creates a span for this method
    @WithSpan
    public String methodWithSpan() {
        return Span.current().getSpanContext().getSpanId();
    }

}
