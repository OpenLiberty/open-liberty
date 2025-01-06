/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.utils.jaeger;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hamcrest.Matcher;

import com.google.protobuf.ByteString;

import io.jaegertracing.api_v2.Model.KeyValue;
import io.jaegertracing.api_v2.Model.Log;
import io.jaegertracing.api_v2.Model.Span;
import io.jaegertracing.api_v2.Model.SpanRef;
import io.jaegertracing.api_v2.Model.SpanRefType;
import io.openliberty.microprofile.telemetry.internal_fat.shared.spans.AbstractSpanMatcher;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;

/**
 * Hamcrest {@link Matcher} for performing assertions against {@link Span} objects retrieved from Jaeger
 */
public class JaegerSpanMatcher extends AbstractSpanMatcher<Span, JaegerSpanMatcher> {

    public JaegerSpanMatcher() {
        super(Span.class);
    }

    /** {@inheritDoc} */
    @Override
    protected String getTraceId(Span span) {
        return JaegerQueryClient.convertByteString(span.getTraceId());
    }

    /** {@inheritDoc} */
    @Override
    protected String getName(Span span) {
        return span.getOperationName();
    }

    /** {@inheritDoc} */
    @Override
    protected String getParentSpanId(Span span) {
        Optional<SpanRef> parentRef = span.getReferencesList().stream().filter(ref -> ref.getRefType() == SpanRefType.CHILD_OF).findAny();
        return parentRef.map(p -> JaegerQueryClient.convertByteString(p.getSpanId())).orElse(null);
    }

    /** {@inheritDoc} */
    @Override
    protected SpanKind getKind(Span span) {
        Optional<KeyValue> kindTag = span.getTagsList().stream().filter(tag -> tag.getKey().equals("span.kind")).findAny();
        Optional<SpanKind> spanKind = kindTag.flatMap(tag -> Arrays.stream(SpanKind.values())
                                                                   .filter(kind -> kind.name().toLowerCase().equals(tag.getVStr()))
                                                                   .findAny());
        return spanKind.orElseThrow(() -> new IllegalArgumentException("Could not extract spanKind from " + span));
    }

    /** {@inheritDoc} */
    @Override
    protected StatusCode getStatusCode(Span span) {
        String status = getStringTag(span, "otel.status_code");
        return findEnum(StatusCode.class, StatusCode::name, status);
    }

    /** {@inheritDoc} */
    @Override
    protected <T> boolean hasAttribute(Span span, AttributeData<T> attributeData) {
        return getTag(span, attributeData.key.getKey()).map(tag -> tagMatches(tag, attributeData))
                                                       .orElse(false);
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasEvent(Span span, String event) {
        Optional<Log> matchingLog = span.getLogsList()
                                        .stream()
                                        .filter(l -> isEventLog(l, event))
                                        .findAny();
        return matchingLog.isPresent();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasException(Span span, Class<?> exceptionClass) {
        Optional<Log> matchingLog = span.getLogsList()
                                        .stream()
                                        .filter(l -> isExceptionLog(l, exceptionClass))
                                        .findAny();
        return matchingLog.isPresent();
    }

    /** {@inheritDoc} */
    @Override
    protected String getServiceName(Span span) {
        return span.getProcess().getServiceName();
    }

    /** {@inheritDoc} */
    @Override
    protected <T> boolean hasResourceAttribute(Span span, AttributeData<T> attributeData) {
        return span.getProcess().getTagsList().stream()
                   .filter(t -> t.getKey().equals(attributeData.key.getKey()))
                   .findAny()
                   .map(t -> tagMatches(t, attributeData))
                   .orElse(false);
    }

    /** {@inheritDoc} */
    @Override
    protected JaegerSpanMatcher self() {
        return this;
    }

    private String getStringTag(Span span, String key) {
        return getOptionalStringTag(span, key).orElseThrow(() -> new IllegalArgumentException("Could not find tag " + key + " on span: " + span));
    }

    private Optional<String> getOptionalStringTag(Span span, String key) {
        return span.getTagsList().stream()
                   .filter(tag -> tag.getKey().equals(key))
                   .map(tag -> tag.getVStr())
                   .findAny();
    }

    private Optional<KeyValue> getTag(Span span, String key) {
        return span.getTagsList().stream()
                   .filter(tag -> tag.getKey().equals(key))
                   .findAny();
    }

    private <E extends Enum<E>, V> E findEnum(Class<E> enumClazz, Function<E, V> converter, V value) {
        return Arrays.stream(enumClazz.getEnumConstants())
                     .filter(e -> converter.apply(e).equals(value))
                     .findAny()
                     .orElseThrow(() -> new IllegalArgumentException("No constant found for " + enumClazz + " matching " + value));
    }

    /**
     * Tests whether a log is an event log with the given name
     *
     * @param log the log to test
     * @param eventName the expected event name
     * @return {@code true} if the log is an event with the given name, otherwise {@code false}
     */
    private boolean isEventLog(Log log, String eventName) {
        List<KeyValue> fields = log.getFieldsList();
        return fields.size() == 1
               && anyMatches(fields, e -> e.getKey().equals("event") && e.getVStr().equals(eventName));
    }

    /**
     * Tests whether a log is an exception log for the given exception class
     *
     * @param log the log to test
     * @param exceptionClass the expected exception class
     * @return {@code true} if the log is an exception log for the given class, otherwise {@code false}
     */
    private boolean isExceptionLog(Log log, Class<?> exceptionClass) {
        List<KeyValue> fields = log.getFieldsList();
        return anyMatches(fields, f -> f.getKey().equals("event") && f.getVStr().equals("exception"))
               && anyMatches(fields, f -> f.getKey().equals("exception.type") && f.getVStr().equals(exceptionClass.getName()))
               && anyMatches(fields, f -> f.getKey().equals("exception.stacktrace") && f.getVStr().contains(exceptionClass.getName()));
    }

    /**
     * Tests if any element of a collection matches a condition
     *
     * @param <V> the element type
     * @param collection the collection to search
     * @param condition the condition to test for
     * @return {@code true} if any member matches the condition, otherwise {@code false}
     */
    private <V> boolean anyMatches(Collection<V> collection, Predicate<V> condition) {
        for (V entry : collection) {
            if (condition.test(entry)) {
                return true;
            }
        }
        return false;
    }

    private <T> boolean tagMatches(KeyValue tag, AttributeData<T> attributeData) {
        switch (attributeData.key.getType()) {
            case STRING:
                return attributeData.value.equals(tag.getVStr());
            case BOOLEAN:
                return attributeData.value.equals(tag.getVBool());
            case DOUBLE:
                return attributeData.value.equals(tag.getVFloat64());
            case LONG:
                return attributeData.value.equals(tag.getVInt64());
            default:
                throw new IllegalArgumentException("Cannot match attributes with type " + attributeData.key.getType());
        }
    }

    public static JaegerSpanMatcher isSpan() {
        return new JaegerSpanMatcher();
    }

    public static JaegerSpanMatcher hasName(String name) {
        return isSpan().withName(name);
    }

    public static <T> JaegerSpanMatcher hasAttribute(AttributeKey<T> key, T value) {
        return isSpan().withAttribute(key, value);
    }

    public static JaegerSpanMatcher hasTraceId(String traceId) {
        return isSpan().withTraceId(traceId);
    }

    public static JaegerSpanMatcher hasEventLog(String name) {
        return isSpan().withEventLog(name);
    }

    public static JaegerSpanMatcher hasExceptionLog(Class<?> exceptionClass) {
        return isSpan().withExceptionLog(exceptionClass);
    }

    public static JaegerSpanMatcher hasParent() {
        return isSpan().withParent();
    }

    public static JaegerSpanMatcher hasNoParent() {
        return isSpan().withNoParent();
    }

    public static JaegerSpanMatcher hasParentSpanId(ByteString spanId) {
        return isSpan().withParentSpanId(JaegerQueryClient.convertByteString(spanId));
    }

    public static JaegerSpanMatcher hasStatus(StatusCode status) {
        return isSpan().withStatus(status);
    }

    public static JaegerSpanMatcher hasKind(SpanKind kind) {
        return isSpan().withKind(kind);
    }

    public static JaegerSpanMatcher hasServiceName(String serviceName) {
        return isSpan().withServiceName(serviceName);
    }

    public static <T> JaegerSpanMatcher hasResourceAttribute(AttributeKey<T> key, T value) {
        return isSpan().withResourceAttribute(key, value);
    }

}
