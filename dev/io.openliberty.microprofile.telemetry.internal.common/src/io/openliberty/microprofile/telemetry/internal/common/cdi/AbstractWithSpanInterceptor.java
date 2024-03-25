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
package io.openliberty.microprofile.telemetry.internal.common.cdi;

import static java.util.function.Predicate.not;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryAccessor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.annotation.support.MethodSpanAttributesExtractor;
import io.opentelemetry.instrumentation.api.annotation.support.ParameterAttributeNamesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.util.SpanNames;

/**
 * This intercepter is responsible for creating Spans and managing the span context for all methods annotated @Span
 */
public abstract class AbstractWithSpanInterceptor {
    protected static final TraceComponent tc = Tr.register(AbstractWithSpanInterceptor.class);

    protected final String instrumentationName = "io.openliberty.microprofile.telemetry";

    protected final Instrumenter<InvocationContext, Void> instrumenter;

    public AbstractWithSpanInterceptor() {
        // Required public no-arg constructor for interceptor
        instrumenter = null;
    }

    @Inject
    public AbstractWithSpanInterceptor(OpenTelemetry openTelemetry) {
        MethodSpanAttributesExtractor<InvocationContext, Void> attributesExtractor = getMethodSpanAttributesExtractor();
        InstrumenterBuilder<InvocationContext, Void> builder = Instrumenter.builder(openTelemetry, instrumentationName, new MethodRequestSpanNameExtractor());

        this.instrumenter = builder.addAttributesExtractor(attributesExtractor).buildInstrumenter(context -> spanKindFromMethod(context));
    }

    //This method is abstract because requires calling an API that changes across different upstream versions of OpenTelemetry
    protected abstract MethodSpanAttributesExtractor<InvocationContext, Void> getMethodSpanAttributesExtractor();

    private SpanKind spanKindFromMethod(InvocationContext context) {
        return getWithSpanBinding(context).map(WithSpan::kind)
                                          .orElse(SpanKind.INTERNAL);
    }

    @FFDCIgnore(Throwable.class)
    @AroundInvoke
    public Object span(final InvocationContext invocationContext) throws Exception {

        Context parentContext = Context.current();
        Context spanContext = null;
        Scope scope = null;
        boolean shouldStart = instrumenter != null && instrumenter.shouldStart(parentContext, invocationContext);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Method " + invocationContext.getMethod().toString() + " Should start: " + shouldStart);
        }

        if (shouldStart) {
            spanContext = instrumenter.start(parentContext, invocationContext);
            scope = spanContext.makeCurrent();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "spanContext " + spanContext.toString() + " has started and is now the current context");
            }
        }

        try {
            Object result = invocationContext.proceed();
            if (shouldStart) {
                instrumenter.end(spanContext, invocationContext, null, null);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "spanContext " + spanContext.toString() + " has ended");
                }
            }
            return result;
        } catch (Throwable error) {
            if (shouldStart) {
                instrumenter.end(spanContext, invocationContext, null, error);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "spanContext " + spanContext.toString() + " has ended with an exception", error);
                }
            }
            throw error;
        } finally {
            if (scope != null) {
                scope.close();
            }
        }
    }

    protected final class MethodRequestSpanNameExtractor implements SpanNameExtractor<InvocationContext> {
        @Override
        public String extract(final InvocationContext context) {
            return getWithSpanBinding(context).map(WithSpan::value) // If present, use the value as the name ...
                                              .filter(not(String::isEmpty)) // ... as long as it's not empty ...
                                              .orElse(getNameFromMethod(context.getMethod())); // ... otherwise compute a name for the method
        }

        private String getNameFromMethod(Method method) {
            if (System.getSecurityManager() != null) {
                return AccessController.doPrivileged((PrivilegedAction<String>) () -> SpanNames.fromMethod(method));
            } else {
                return SpanNames.fromMethod(method);
            }
        }
    }

    protected static WithSpanParameterAttributeNamesExtractor getNewWithSpanParameterAttributeNamesExtractor() {
        return new WithSpanParameterAttributeNamesExtractor();
    }

    protected static final class WithSpanParameterAttributeNamesExtractor implements ParameterAttributeNamesExtractor {
        protected static String attributeName(Parameter parameter) {
            SpanAttribute spanAttribute = parameter.getDeclaredAnnotation(SpanAttribute.class);
            if (spanAttribute == null) {
                return null;
            }
            String value = spanAttribute.value();
            if (!value.isEmpty()) {
                return value;
            } else if (parameter.isNamePresent()) {
                return parameter.getName();
            } else {
                return null;
            }
        }

        @Override
        public String[] extract(final Method method, final Parameter[] parameters) {
            String[] attributeNames = new String[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                attributeNames[i] = attributeName(parameters[i]);
            }
            return attributeNames;
        }
    }

    /**
     * Get the WithSpan annotation used to bind this interceptor, if one can be found
     * <p>
     * In almost all cases, it's expected that there will be a WithSpan binding, unless a CDI extension has messed around with the interceptor
     *
     * @param context the invocation context
     * @return the WithSpan annotation, or an empty {@code Optional} if one could not be found
     */
    private Optional<WithSpan> getWithSpanBinding(InvocationContext context) {
        Set<Annotation> bindings = OpenTelemetryAccessor.getInterceptorBindingsFromInvocationContext(context);
        for (Annotation binding : bindings) {
            if (binding.annotationType().equals(WithSpan.class)) {
                return Optional.of((WithSpan) binding);
            }
        }
        return Optional.empty();
    }
}
