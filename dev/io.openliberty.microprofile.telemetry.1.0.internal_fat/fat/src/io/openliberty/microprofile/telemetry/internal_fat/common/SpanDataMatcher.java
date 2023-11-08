/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.common;

import io.openliberty.microprofile.telemetry.internal_fat.shared.spans.AbstractSpanMatcher;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

/**
 * A hamcrest matcher for matching {@link SpanData} instances
 * <p>
 * Example usage:
 *
 * <pre>
 * assertThat(span, isSpan().withName("foo").withKind(SERVER));
 *
 * assertThat(span, hasName("foo"));
 * </pre>
 */
public class SpanDataMatcher extends AbstractSpanMatcher<SpanData, SpanDataMatcher> {

    protected SpanDataMatcher() {
        super(SpanData.class);
    }

    /** {@inheritDoc} */
    @Override
    protected String getTraceId(SpanData span) {
        return span.getTraceId();
    }

    /** {@inheritDoc} */
    @Override
    protected String getName(SpanData span) {
        return span.getName();
    }

    /** {@inheritDoc} */
    @Override
    protected String getParentSpanId(SpanData span) {
        String spanId = span.getParentSpanId();
        if (SpanId.isValid(spanId)) {
            return spanId;
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected SpanKind getKind(SpanData span) {
        return span.getKind();
    }

    /** {@inheritDoc} */
    @Override
    protected StatusCode getStatusCode(SpanData span) {
        return span.getStatus().getStatusCode();
    }

    /** {@inheritDoc} */
    @Override
    protected <T> boolean hasAttribute(SpanData span, AttributeData<T> attributeData) {
        return attributeData.value.equals(span.getAttributes().get(attributeData.key));
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasEvent(SpanData span, String event) {
        for (EventData eventData : span.getEvents()) {
            if (event.equals(eventData.getName())) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasException(SpanData span, Class<?> exceptionClass) {
        String expectedClassName = exceptionClass.getCanonicalName();

        for (EventData eventData : span.getEvents()) {
            if (SemanticAttributes.EXCEPTION_EVENT_NAME.equals(eventData.getName())) {
                String exceptionClassName = eventData.getAttributes().get(SemanticAttributes.EXCEPTION_TYPE);
                if (expectedClassName.equals(exceptionClassName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    protected String getServiceName(SpanData span) {
        return span.getResource().getAttribute(ResourceAttributes.SERVICE_NAME);
    }

    /** {@inheritDoc} */
    @Override
    protected <T> boolean hasResourceAttribute(SpanData span, AttributeData<T> attributeData) {
        return attributeData.value.equals(span.getResource().getAttribute(attributeData.key));
    }

    /** {@inheritDoc} */
    @Override
    protected SpanDataMatcher self() {
        return this;
    }

    // Static construction methods
    // ---------------------------

    public static SpanDataMatcher isSpan() {
        return new SpanDataMatcher();
    }

    public static SpanDataMatcher hasEventLog(String name) {
        return isSpan().withEventLog(name);
    }

    public static SpanDataMatcher hasExceptionLog(Class<?> exceptionClass) {
        return isSpan().withExceptionLog(exceptionClass);
    }

    public static SpanDataMatcher hasTraceId(String traceId) {
        return isSpan().withTraceId(traceId);
    }

    public static SpanDataMatcher hasName(String name) {
        return isSpan().withName(name);
    }

    public static SpanDataMatcher hasNoParent() {
        return isSpan().withNoParent();
    }

    public static SpanDataMatcher hasParent() {
        return isSpan().withParent();
    }

    public static SpanDataMatcher hasParentSpanId(String spanId) {
        return isSpan().withParentSpanId(spanId);
    }

    public static SpanDataMatcher hasKind(SpanKind kind) {
        return isSpan().withKind(kind);
    }

    public static <T> SpanDataMatcher hasAttribute(AttributeKey<T> key, T value) {
        return isSpan().withAttribute(key, value);
    }

    public static SpanDataMatcher hasStatus(StatusCode status) {
        return isSpan().withStatus(status);
    }

    public static SpanDataMatcher hasServiceName(String serviceName) {
        return isSpan().withServiceName(serviceName);
    }

    public static <T> SpanDataMatcher hasResourceAttribute(AttributeKey<T> key, T value) {
        return isSpan().withResourceAttribute(key, value);
    }

}
