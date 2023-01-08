/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.opentracing30.internal.restfulws;

import java.util.concurrent.Callable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.opentracing.internal.OpentracingClientFilter;
import io.openliberty.opentracing.internal.OpentracingTracerManager;
import io.openliberty.restfulWS.client.ClientAsyncTaskWrapper;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * Wraps the restfulWS client task to capture and apply the current tracer and span
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class OpentracingClientAsyncWrapper implements ClientAsyncTaskWrapper {

    @Override
    public Runnable wrap(Runnable r) {
        Tracer tracer = OpentracingTracerManager.getTracer();
        if (tracer == null) {
            return r;
        }

        Span activeSpan = tracer.activeSpan();

        return () -> {
            Tracer oldTracer = OpentracingClientFilter.setCurrentTracer(tracer);
            try {
                if (activeSpan != null) {
                    try (Scope scope = tracer.activateSpan(activeSpan)) {
                        r.run();
                    }
                } else {
                    r.run();
                }
            } finally {
                OpentracingClientFilter.setCurrentTracer(oldTracer);
            }
        };
    }

    @Override
    public <T> Callable<T> wrap(Callable<T> c) {
        Tracer tracer = OpentracingTracerManager.getTracer();
        if (tracer == null) {
            return c;
        }

        Span activeSpan = tracer.activeSpan();

        return () -> {
            Tracer oldTracer = OpentracingClientFilter.setCurrentTracer(tracer);
            try {
                if (activeSpan != null) {
                    try (Scope scope = tracer.activateSpan(activeSpan)) {
                        return c.call();
                    }
                } else {
                    return c.call();
                }
            } finally {
                OpentracingClientFilter.setCurrentTracer(oldTracer);
            }
        };
    }

}
