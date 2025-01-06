/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.utils.jaeger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import com.google.protobuf.ByteString;

import io.jaegertracing.api_v2.Model.KeyValue;
import io.jaegertracing.api_v2.Model.Log;
import io.jaegertracing.api_v2.Model.Span;
import io.jaegertracing.api_v2.Model.SpanRef;
import io.jaegertracing.api_v2.Model.SpanRefType;
import io.jaegertracing.api_v2.Model.ValueType;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;

/**
 * Hamcrest {@link Matcher} for performing assertions against {@link Span} objects retrieved from Jaeger
 */
public class SpanMatcher extends TypeSafeDiagnosingMatcher<Span> {

    List<KeyValue> expectedTags = new ArrayList<>();
    List<List<KeyValue>> expectedLogs = new ArrayList<>();
    List<String> expectedEvents = new ArrayList<>();
    List<Class<?>> expectedExceptions = new ArrayList<>();
    ByteString expectedTraceId = null;
    ByteString expectedParentSpanId = null;
    Boolean expectHasParent = null;
    String expectedName = null;
    String expectedServiceName = null;
    List<KeyValue> expectedProcessTags = new ArrayList<>();

    /*
     * Describes what we're looking for
     */
    @Override
    public void describeTo(Description desc) {
        desc.appendText("Span");

        if (expectedName != null) {
            desc.appendText("\n  with name: ");
            desc.appendText(expectedName);
        }

        if (expectedTraceId != null) {
            desc.appendText("\n  with traceId: ");
            desc.appendText(expectedTraceId.toString());
        }

        if (expectedParentSpanId != null) {
            desc.appendText("\n  with parent: ");
            desc.appendText(expectedParentSpanId.toString());
        }

        if (expectHasParent != null) {
            if (expectHasParent) {
                desc.appendText("\n with a parent");
            } else {
                desc.appendText("\n with no parent");
            }
        }

        if (!expectedTags.isEmpty()) {
            desc.appendText("\n  with tags: ");
            desc.appendValue(expectedTags);
        }

        if (!expectedEvents.isEmpty()) {
            desc.appendText("\n  with event logs: ");
            desc.appendValue(expectedEvents);
        }

        if (!expectedExceptions.isEmpty()) {
            desc.appendText("\n  with exception logs: ");
            desc.appendValue(expectedExceptions);
        }

        if (expectedServiceName != null) {
            desc.appendText("\n  with service name: ");
            desc.appendText(expectedServiceName);
        }

        if (!expectedProcessTags.isEmpty()) {
            desc.appendText("\n  with process tags: ");
            desc.appendValue(expectedProcessTags);
        }
    }

    /*
     * Returns whether the span matches and adds a description of why it didn't match
     */
    @Override
    protected boolean matchesSafely(Span span, Description desc) {
        desc.appendValue(span);

        if (expectedTraceId != null && !expectedTraceId.equals(span.getTraceId())) {
            return false;
        }

        if (expectedName != null && !expectedName.equals(span.getOperationName())) {
            return false;
        }

        Optional<SpanRef> parentRef = span.getReferencesList().stream().filter(ref -> ref.getRefType() == SpanRefType.CHILD_OF).findAny();

        if (expectedParentSpanId != null) {
            if (!parentRef.isPresent()) {
                return false;
            }

            if (!parentRef.get().getSpanId().equals(expectedParentSpanId)) {
                return false;
            }
        }

        if (expectHasParent != null) {
            if (parentRef.isPresent() != expectHasParent) {
                return false;
            }
        }

        for (KeyValue tag : expectedTags) {
            if (!span.getTagsList().contains(tag)) {
                return false;
            }
        }

        for (String eventName : expectedEvents) {
            Optional<Log> matchingLog = span.getLogsList()
                                            .stream()
                                            .filter(l -> isEventLog(l, eventName))
                                            .findAny();
            if (!matchingLog.isPresent()) {
                return false;
            }
        }

        for (Class<?> exceptionClass : expectedExceptions) {
            Optional<Log> matchingLog = span.getLogsList()
                                            .stream()
                                            .filter(l -> isExceptionLog(l, exceptionClass))
                                            .findAny();
            if (!matchingLog.isPresent()) {
                return false;
            }
        }

        if (expectedServiceName != null) {
            if (!span.hasProcess()) {
                return false;
            }
            if (!expectedServiceName.equals(span.getProcess().getServiceName())) {
                return false;
            }
        }

        if (!expectedProcessTags.isEmpty()) {
            if (!span.hasProcess()) {
                return false;
            }
            for (KeyValue tag : expectedProcessTags) {
                if (!span.getProcess().getTagsList().contains(tag)) {
                    return false;
                }
            }
        }

        return true;
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

    public SpanMatcher withEventLog(String name) {
        expectedEvents.add(name);
        return this;
    }

    public SpanMatcher withExceptionLog(Class<?> exceptionClass) {
        expectedExceptions.add(exceptionClass);
        return this;
    }

    public SpanMatcher withTraceId(String traceId) {
        expectedTraceId = JaegerQueryClient.convertTraceId(traceId);
        return this;
    }

    public SpanMatcher withName(String name) {
        expectedName = name;
        return this;
    }

    public SpanMatcher withNoParent() {
        expectHasParent = false;
        return this;
    }

    public SpanMatcher withParent() {
        expectHasParent = true;
        return this;
    }

    public SpanMatcher withParentSpanId(ByteString spanId) {
        expectedParentSpanId = spanId;
        return this;
    }

    public SpanMatcher withKind(SpanKind kind) {
        return withTag("span.kind", kind.name().toLowerCase());
    }

    public SpanMatcher withTag(String key, String value) {
        KeyValue tag = KeyValue.newBuilder().setKey(key).setVStr(value).build();
        expectedTags.add(tag);
        return this;
    }

    public SpanMatcher withStatus(StatusCode status) {
        KeyValue statusTag = KeyValue.newBuilder().setKey("otel.status_code").setVStr(status.name()).build();
        expectedTags.add(statusTag);

        KeyValue errorTag = KeyValue.newBuilder().setKey("error").setVType(ValueType.BOOL).setVBool(status == StatusCode.ERROR ? true : false).build();
        expectedTags.add(errorTag);

        return this;
    }

    public SpanMatcher withServiceName(String serviceName) {
        expectedServiceName = serviceName;
        return this;
    }

    public SpanMatcher withProcessTag(String key, String value) {
        KeyValue tag = KeyValue.newBuilder().setKey(key).setVStr(value).build();
        expectedProcessTags.add(tag);
        return this;
    }

    public static SpanMatcher span() {
        return new SpanMatcher();
    }

    public static SpanMatcher hasName(String name) {
        return span().withName(name);
    }

    public static SpanMatcher hasTag(String key, String value) {
        return span().withTag(key, value);
    }

    public static SpanMatcher hasTraceId(String traceId) {
        return span().withTraceId(traceId);
    }

    public static SpanMatcher hasEventLog(String name) {
        return span().withEventLog(name);
    }

    public static SpanMatcher hasExceptionLog(Class<?> exceptionClass) {
        return span().withExceptionLog(exceptionClass);
    }

    public static SpanMatcher hasParent() {
        return span().withParent();
    }

    public static SpanMatcher hasNoParent() {
        return span().withNoParent();
    }

    public static SpanMatcher hasParentSpanId(ByteString spanId) {
        return span().withParentSpanId(spanId);
    }

    public static SpanMatcher hasStatus(StatusCode status) {
        return span().withStatus(status);
    }

    public static SpanMatcher hasKind(SpanKind kind) {
        return span().withKind(kind);
    }

    public static SpanMatcher hasServiceName(String serviceName) {
        return span().withServiceName(serviceName);
    }

    public static SpanMatcher hasProcessTag(String key, String value) {
        return span().withProcessTag(key, value);
    }

}
