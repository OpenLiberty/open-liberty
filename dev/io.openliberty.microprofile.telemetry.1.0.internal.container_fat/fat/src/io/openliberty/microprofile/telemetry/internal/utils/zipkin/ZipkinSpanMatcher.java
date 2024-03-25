/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.utils.zipkin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import io.openliberty.microprofile.telemetry.internal.utils.zipkin.ZipkinSpan.Annotation;
import io.opentelemetry.api.trace.SpanKind;

/**
 * Hamcrest {@link Matcher} for performing assertions against {@link ZipkinSpan} objects retrieved from Zipkin
 */
public class ZipkinSpanMatcher extends TypeSafeDiagnosingMatcher<ZipkinSpan> {

    Map<String, String> expectedTags = new HashMap<>();
    List<Matcher<String>> expectedAnnotations = new ArrayList<>();
    String expectedTraceId = null;
    String expectedKind = null;
    String expectedParentSpanId = null;
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

        if (expectedKind != null) {
            desc.appendText("\n  with kind: ");
            desc.appendText(expectedKind.toString());
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

        if (!expectedAnnotations.isEmpty()) {
            desc.appendText("\n  with annotations: ");
            desc.appendValue(expectedAnnotations);
        }
    }

    /*
     * Returns whether the span matches and adds a description of why it didn't match
     */
    @Override
    protected boolean matchesSafely(ZipkinSpan span, Description desc) {
        desc.appendValue(span);

        if (expectedTraceId != null && !expectedTraceId.equals(span.getTraceId())) {
            return false;
        }

        if (expectedKind != null && !expectedKind.equals(span.getKind())) {
            return false;
        }

        if (expectedParentSpanId != null) {
            if (!Objects.equals(expectedParentSpanId, span.getParentId())) {
                return false;
            }
        }

        if (expectHasParent != null) {
            if (expectHasParent && span.getParentId() == null) {
                return false;
            }
            if (!expectHasParent && span.getParentId() != null) {
                return false;
            }
        }

        for (Entry<String, String> tag : expectedTags.entrySet()) {
            if (!span.getTags().entrySet().contains(tag)) {
                return false;
            }
        }

        for (Matcher<String> expectedAnnotation : expectedAnnotations) {
            Optional<Annotation> anno = span.getAnnotations()
                                            .stream()
                                            .filter(a -> expectedAnnotation.matches(a.getValue()))
                                            .findAny();
            if (!anno.isPresent()) {
                return false;
            }
        }

        return true;
    }

    public ZipkinSpanMatcher withAnnotation(String name) {
        expectedAnnotations.add(Matchers.equalTo(name));
        return this;
    }

    public ZipkinSpanMatcher withAnnotation(Matcher<String> matcherString) {
        expectedAnnotations.add(matcherString);
        return this;
    }

    public ZipkinSpanMatcher withTraceId(String traceId) {
        expectedTraceId = traceId.toLowerCase();
        return this;
    }

    public ZipkinSpanMatcher withNoParent() {
        expectHasParent = false;
        return this;
    }

    public ZipkinSpanMatcher withParent() {
        expectHasParent = true;
        return this;
    }

    public ZipkinSpanMatcher withParentSpanId(String spanId) {
        expectedParentSpanId = spanId.toLowerCase();
        return this;
    }

    public ZipkinSpanMatcher withTag(String key, String value) {
        expectedTags.put(key, value);
        return this;
    }

    public ZipkinSpanMatcher withKind(SpanKind kind) {
        expectedKind = kind.name();
        return this;
    }

    public static ZipkinSpanMatcher hasKind(SpanKind kind) {
        return span().withKind(kind);
    }
    
    public static ZipkinSpanMatcher span() {
        return new ZipkinSpanMatcher();
    }

    public static ZipkinSpanMatcher hasTag(String key, String value) {
        return span().withTag(key, value);
    }

    public static ZipkinSpanMatcher hasTraceId(String traceId) {
        return span().withTraceId(traceId);
    }

    public static ZipkinSpanMatcher hasAnnotation(String name) {
        return span().withAnnotation(name);
    }

    public static ZipkinSpanMatcher hasAnnotation(Matcher<String> matcherString) {
        return span().withAnnotation(matcherString);
    }

    public static ZipkinSpanMatcher hasParent() {
        return span().withParent();
    }

    public static ZipkinSpanMatcher hasNoParent() {
        return span().withNoParent();
    }

    public static ZipkinSpanMatcher hasParentSpanId(String spanId) {
        return span().withParentSpanId(spanId);
    }
}
