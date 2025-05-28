/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.opentelemetry.api.trace.SpanKind;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * This annotation marks that an execution of this method or constructor should result in a new
 * {@link io.opentelemetry.api.trace.Span}.
 *
 * <p>Application developers can use this annotation to signal OpenTelemetry auto-instrumentation
 * that a new span should be created whenever marked method is executed.
 *
 * <p>If you are a library developer, then probably you should NOT use this annotation, because it
 * is non-functional without the OpenTelemetry auto-instrumentation agent, or some other annotation
 * processor.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-java-instrumentation">OpenTelemetry
 *      OpenTelemetry Instrumentation for Java</a>
 */

//Adds ElementType.TYPE to target, @InterceptorBinding annotation and @Nonbinding annotations
@InterceptorBinding
@Target({ ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface WithSpan {
    /**
     * Optional name of the created span.
     *
     * <p>If not specified, an appropriate default name should be created by auto-instrumentation.
     * E.g. {@code "className"."method"}
     */
    @Nonbinding
    String value() default "";

    /** Specify the {@link SpanKind} of span to be created. Defaults to {@link SpanKind#INTERNAL}. */
    @Nonbinding
    SpanKind kind() default SpanKind.INTERNAL;
}