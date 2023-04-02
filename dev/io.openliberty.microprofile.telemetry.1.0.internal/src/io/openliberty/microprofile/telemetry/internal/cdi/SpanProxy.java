/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.cdi;

import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;

/**
 * This proxy class redirects method calls to Span.current(), by doing so it allows people to use @Inject Span and get an object which will not become obsolete.
 */
public class SpanProxy implements Span {

    @Override
    public <T> Span setAttribute(AttributeKey<T> key, T value) {
        return Span.current().setAttribute(key, value);
    }

    @Override
    public Span addEvent(String name, Attributes attributes) {
        return Span.current().addEvent(name, attributes);
    }

    @Override
    public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
        return Span.current().addEvent(name, attributes, timestamp, unit);
    }

    @Override
    public Span setStatus(StatusCode statusCode, String description) {
        return Span.current().setStatus(statusCode, description);
    }

    @Override
    public Span recordException(Throwable exception, Attributes additionalAttributes) {
        return Span.current().recordException(exception, additionalAttributes);
    }

    @Override
    public Span updateName(String name) {
        return Span.current().updateName(name);
    }

    @Override
    public void end() {
        Span.current().end();
    }

    @Override
    public void end(long timestamp, TimeUnit unit) {
        Span.current().end(timestamp, unit);
    }

    @Override
    public SpanContext getSpanContext() {
        return Span.current().getSpanContext();
    }

    @Override
    public boolean isRecording() {
        return Span.current().isRecording();
    }

    @Override
    public boolean equals(Object obj) {
        return Span.current().equals(obj);
    }

    @Override
    public int hashCode() {
        return Span.current().hashCode();
    }

    @Override
    public String toString() {
        return Span.current().toString();
    }

}
