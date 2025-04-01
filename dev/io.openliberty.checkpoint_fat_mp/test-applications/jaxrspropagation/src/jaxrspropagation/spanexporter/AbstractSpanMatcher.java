/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jaxrspropagation.spanexporter;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

/**
 * Matcher for some object which represents span data
 * <p>
 * This holds the common code for matching a span returned from Jaeger, Zipkin or the InMemorySpanExporter
 *
 * @param <SPAN> the span datatype
 * @param <SELF> the matcher type
 */
public abstract class AbstractSpanMatcher<SPAN, SELF extends AbstractSpanMatcher<SPAN, SELF>> extends TypeSafeDiagnosingMatcher<SPAN> {

    private final List<AttributeData<?>> expectedAttributes = new ArrayList<>();
    private final List<String> expectedEvents = new ArrayList<>();
    private final List<Class<?>> expectedExceptions = new ArrayList<>();
    private SpanKind expectedKind = null;
    private StatusCode expectedStatusCode = null;
    private String expectedTraceId = null;
    private String expectedParentSpanId = null;
    private Boolean expectHasParent = null;
    private String expectedName = null;
    private String expectedServiceName = null;
    private final List<AttributeData<?>> expectedResourceAttributes = new ArrayList<>();

    /**
     * Explicit constructor.
     * <p>
     * It's necessary to manually pass in the span class to avoid hamcrest doing reflection and triggering an AccessControlException
     *
     * @param spanClass the span class
     */
    protected AbstractSpanMatcher(Class<SPAN> spanClass) {
        super(spanClass);
    }

    protected static class AttributeData<T> {
        public final AttributeKey<T> key;
        public final T value;

        /**
         * @param key
         * @param value
         */
        public AttributeData(AttributeKey<T> key, T value) {
            super();
            this.key = key;
            this.value = value;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return key + " = " + value;
        }
    }

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

        if (expectedKind != null) {
            desc.appendText("\n  with kind: ");
            desc.appendValue(expectedKind);
        }

        if (expectedStatusCode != null) {
            desc.appendText("\n  with status code: ");
            desc.appendValue(expectedStatusCode);
        }

        if (!expectedAttributes.isEmpty()) {
            desc.appendText("\n  with attributes: ");
            desc.appendValue(expectedAttributes);
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

        if (!expectedResourceAttributes.isEmpty()) {
            desc.appendText("\n  with resource attributes: ");
            desc.appendValue(expectedResourceAttributes);
        }
    }

    /*
     * Returns whether the span matches and adds a description of why it didn't match
     */
    @Override
    protected boolean matchesSafely(SPAN span, Description desc) {
        // For now, we just use the toString of the span as the description of why it didn't match
        // We could list out or highlight the elements that didn't match, but usually it's obvious
        // enough from the toString().
        desc.appendValue(span);

        if (expectedTraceId != null && !expectedTraceId.equals(getTraceId(span))) {
            return false;
        }

        if (expectedName != null && !expectedName.equals(getName(span))) {
            return false;
        }

        String parentSpanId = getParentSpanId(span);

        if (expectedParentSpanId != null) {
            if (!expectedParentSpanId.equals(parentSpanId)) {
                return false;
            }
        }

        if (expectHasParent != null) {
            boolean hasParent = parentSpanId != null;
            if (hasParent != expectHasParent) {
                return false;
            }
        }

        if (expectedKind != null) {
            if (expectedKind != getKind(span)) {
                return false;
            }
        }

        if (expectedStatusCode != null) {
            if (expectedStatusCode != getStatusCode(span)) {
                return false;
            }
        }

        for (AttributeData<?> attribute : expectedAttributes) {
            if (!hasAttribute(span, attribute)) {
                return false;
            }
        }

        for (String eventName : expectedEvents) {
            if (!hasEvent(span, eventName)) {
                return false;
            }
        }

        for (Class<?> exceptionClass : expectedExceptions) {
            if (!hasException(span, exceptionClass)) {
                return false;
            }
        }

        if (expectedServiceName != null) {
            if (!expectedServiceName.equals(getServiceName(span))) {
                return false;
            }
        }

        if (!expectedResourceAttributes.isEmpty()) {
            for (AttributeData<?> attribute : expectedResourceAttributes) {
                if (!hasResourceAttribute(span, attribute)) {
                    return false;
                }
            }
        }

        return true;
    }

    // Abstract methods, to be implemented for each specific span data structure
    // -------------------------------------------------------------------------

    /**
     * Gets the traceId of the span
     *
     * @param span the span
     * @return the traceId
     */
    abstract protected String getTraceId(SPAN span);

    /**
     * Gets the name of the span
     *
     * @param span the span
     * @return the span name
     */
    abstract protected String getName(SPAN span);

    /**
     * Gets the span ID of the parent span
     *
     * @param span the span
     * @return the ID of the span's parent, or {@code null} if it has no parent
     */
    abstract protected String getParentSpanId(SPAN span);

    /**
     * Gets the span kind (CLIENT/SERVER/INTERNAL etc.)
     *
     * @param span the span
     * @return the span kind
     */
    abstract protected SpanKind getKind(SPAN span);

    /**
     * Gets the status code (OK, ERROR, UNSET)
     *
     * @param span the span
     * @return the status code
     */
    abstract protected StatusCode getStatusCode(SPAN span);

    /**
     * Returns whether the span has the given attribute with the expected value
     *
     * @param <T>   the attribute type
     * @param span  the span
     * @param key   the attribute key
     * @param value the expected value
     * @return {@code} true if {@code span} has the given attribute and it has the expected value, otherwise {@code false}
     */
    abstract protected <T> boolean hasAttribute(SPAN span, AttributeData<T> attributeData);

    /**
     * Returns whether the span has an event with the given name
     *
     * @param span  the span
     * @param event the event name
     * @return {@code true} if {@code span} has an event named {@code event}, otherwise {@code false}
     */
    abstract protected boolean hasEvent(SPAN span, String event);

    /**
     * Returns whehter the span has an exception event with the given name
     *
     * @param span           the span
     * @param exceptionClass the expected exception class
     * @return {@code true} if {@code span} has an event for an exception thrown of type {@code exceptionClass}, otherwise {@code false}
     */
    abstract protected boolean hasException(SPAN span, Class<?> exceptionClass);

    /**
     * Gets the {@link ResourceAttributes#SERVICE_NAME} from the span
     *
     * @param span the span
     * @return the service name
     */
    abstract protected String getServiceName(SPAN span);

    /**
     * Returns whether the span has the given resource attribute with the expected value
     *
     * @param <T>   the resource attribute type
     * @param span  the span
     * @param key   the resource attribute key
     * @param value the expected value
     * @return {@code} true if {@code span} has the given resource attribute and it has the expected value, otherwise {@code false}
     */
    abstract protected <T> boolean hasResourceAttribute(SPAN span, AttributeData<T> attributeData);

    /**
     * Returns {@code this}. Needed for generics.
     *
     * @return {@code this}
     */
    abstract protected SELF self();

    // Common configuration methods
    // ----------------------------

    public SELF withEventLog(String name) {
        expectedEvents.add(name);
        return self();
    }

    public SELF withExceptionLog(Class<?> exceptionClass) {
        expectedExceptions.add(exceptionClass);
        return self();
    }

    public SELF withTraceId(String traceId) {
        expectedTraceId = traceId;
        return self();
    }

    public SELF withName(String name) {
        expectedName = name;
        return self();
    }

    public SELF withNoParent() {
        expectHasParent = false;
        return self();
    }

    public SELF withParent() {
        expectHasParent = true;
        return self();
    }

    public SELF withParentSpanId(String spanId) {
        expectedParentSpanId = spanId;
        return self();
    }

    public SELF withKind(SpanKind kind) {
        expectedKind = kind;
        return self();
    }

    public <T> SELF withAttribute(AttributeKey<T> key, T value) {
        expectedAttributes.add(new AttributeData<T>(key, value));
        return self();
    }

    public SELF withStatus(StatusCode status) {
        expectedStatusCode = status;
        return self();
    }

    public SELF withServiceName(String serviceName) {
        expectedServiceName = serviceName;
        return self();
    }

    public <T> SELF withResourceAttribute(AttributeKey<T> key, T value) {
        expectedResourceAttributes.add(new AttributeData<T>(key, value));
        return self();
    }

}
