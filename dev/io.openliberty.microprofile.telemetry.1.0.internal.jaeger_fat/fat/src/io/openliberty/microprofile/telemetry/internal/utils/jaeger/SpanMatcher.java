/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.utils.jaeger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import com.google.protobuf.ByteString;

import io.jaegertracing.api_v2.Model.KeyValue;
import io.jaegertracing.api_v2.Model.Log;
import io.jaegertracing.api_v2.Model.Span;
import io.jaegertracing.api_v2.Model.SpanRef;
import io.jaegertracing.api_v2.Model.SpanRefType;

/**
 * Hamcrest {@link Matcher} for performing assertions against {@link Span} objects retrieved from Jaeger
 */
public class SpanMatcher extends TypeSafeDiagnosingMatcher<Span> {

    List<KeyValue> expectedTags = new ArrayList<>();
    List<List<KeyValue>> expectedLogs = new ArrayList<>();
    ByteString expectedTraceId = null;
    ByteString expectedParentSpanId = null;
    Boolean expectHasParent = null;

    /*
     * Describes what we're looking for
     */
    @Override
    public void describeTo(Description desc) {
        desc.appendText("Span");

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

        if (!expectedLogs.isEmpty()) {
            desc.appendText("\n  with logs: ");
            desc.appendValue(expectedLogs);
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

        for (List<KeyValue> expectedLogFields : expectedLogs) {
            boolean foundExpectedLog = false;
            for (Log actualLog : span.getLogsList()) {
                if (logMatches(actualLog, expectedLogFields)) {
                    foundExpectedLog = true;
                    break;
                }
            }
            if (!foundExpectedLog) {
                return false;
            }
        }

        return true;
    }

    private boolean logMatches(Log actual, List<KeyValue> expected) {
        return actual.getFieldsCount() == expected.size()
               && actual.getFieldsList().containsAll(expected);
    }

    public SpanMatcher withEventLog(String name) {
        List<KeyValue> log = Collections.singletonList(KeyValue.newBuilder().setKey("event").setVStr(name).build());
        expectedLogs.add(log);
        return this;
    }

    public SpanMatcher withTraceId(String traceId) {
        expectedTraceId = JaegerQueryClient.convertTraceId(traceId);
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

    public SpanMatcher withTag(String key, String value) {
        KeyValue tag = KeyValue.newBuilder().setKey(key).setVStr(value).build();
        expectedTags.add(tag);
        return this;
    }

    public static SpanMatcher span() {
        return new SpanMatcher();
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

    public static SpanMatcher hasParent() {
        return span().withParent();
    }

    public static SpanMatcher hasNoParent() {
        return span().withNoParent();
    }

    public static SpanMatcher hasParentSpanId(ByteString spanId) {
        return span().withParentSpanId(spanId);
    }

}
