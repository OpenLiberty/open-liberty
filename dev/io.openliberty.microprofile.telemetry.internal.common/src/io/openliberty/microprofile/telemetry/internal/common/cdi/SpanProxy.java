/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.common.cdi;

import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

/**
 * This proxy class redirects method calls to Span.current(), by doing so it allows people to use @Inject Span and get an object which will not become obsolete.
 */
public class SpanProxy implements Span {

    private static final TraceComponent tc = Tr.register(SpanProxy.class);

    @Override
    public Scope makeCurrent() {
        //We can't do anything useful here but this will prevent the proxy itself becoming the current span.
        return Scope.noop();
    }

    @Override
    public Context storeInContext(Context context) {
        return Span.current().storeInContext(context);
    }

    @Override
    public <T> Span setAttribute(AttributeKey<T> key, T value) {
        try {
            return Span.current().setAttribute(key, value);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return this;
        }
    }

    @Override
    public Span addEvent(String name, Attributes attributes) {
        try {
            return Span.current().addEvent(name, attributes);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return this;
        }
    }

    @Override
    public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
        try {
            return Span.current().addEvent(name, attributes, timestamp, unit);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return this;
        }
    }

    @Override
    public Span setStatus(StatusCode statusCode, String description) {
        try {
            return Span.current().setStatus(statusCode, description);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return this;
        }
    }

    @Override
    public Span recordException(Throwable exception, Attributes additionalAttributes) {
        try {
            return Span.current().recordException(exception, additionalAttributes);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return this;
        }
    }

    @Override
    public Span updateName(String name) {
        try {
            return Span.current().updateName(name);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return this;
        }
    }

    @Override
    public void end() {
        try {
            Span.current().end();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
        }
    }

    @Override
    public void end(long timestamp, TimeUnit unit) {
        try {
            Span.current().end(timestamp, unit);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
        }
    }

    @Override
    public SpanContext getSpanContext() {
        try {
            return Span.current().getSpanContext();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return null;
        }
    }

    @Override
    public boolean isRecording() {
        try {
            return Span.current().isRecording();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return false;
        }
    }

    @Override
    public boolean equals(Object obj) {
        try {
            return Span.current().equals(obj);
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return false;
        }
    }

    @Override
    public int hashCode() {
        try {
            return Span.current().hashCode();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return -1;
        }
    }

    @Override
    public String toString() {
        try {
            return Span.current().toString();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return "";
        }
    }

}
