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
package io.openliberty.microprofile.telemetry.common.internal.cdi;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

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
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

public abstract class AbstractWithSpanInterceptor {
	protected static final TraceComponent tc = Tr.register(AbstractWithSpanInterceptor.class);

	protected final String instrumentationName = "io.openliberty.microprofile.telemetry";

	protected final Instrumenter<MethodRequest, Void> instrumenter;

	public AbstractWithSpanInterceptor() {
		// Required public no-arg constructor for interceptor
		instrumenter = null;
	}

	@Inject
	public AbstractWithSpanInterceptor(OpenTelemetry openTelemetry) {
		MethodSpanAttributesExtractor<MethodRequest, Void> attributesExtractor = getMethodSpanAttributesExtractor();
		InstrumenterBuilder<MethodRequest, Void> builder = Instrumenter.builder(openTelemetry, instrumentationName, new MethodRequestSpanNameExtractor());

		this.instrumenter = builder.addAttributesExtractor(attributesExtractor)
				.buildInstrumenter(methodRequest -> spanKindFromMethod(methodRequest.getMethod()));
	}

	protected abstract MethodSpanAttributesExtractor<MethodRequest, Void> getMethodSpanAttributesExtractor();

	private static SpanKind spanKindFromMethod(Method method) {
		WithSpan annotation = method.getDeclaredAnnotation(WithSpan.class);
		if (annotation == null) {
			return SpanKind.INTERNAL;
		}
		return annotation.kind();
	}

	@AroundInvoke
	public Object span(final InvocationContext invocationContext) throws Exception {
		MethodRequest methodRequest = new MethodRequest(invocationContext.getMethod(), invocationContext.getParameters());

		Context parentContext = Context.current();
		Context spanContext = null;
		Scope scope = null;
		boolean shouldStart = instrumenter.shouldStart(parentContext, methodRequest);
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(this, tc, "Method " + invocationContext.getMethod().toString() + " Should start: " + shouldStart);
		}

		if (shouldStart) {
			spanContext = instrumenter.start(parentContext, methodRequest);
			scope = spanContext.makeCurrent();
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(this, tc, "spanContext " + spanContext.toString() + " has started and is now the current context");
			}
		}

		try {
			Object result = invocationContext.proceed();
			if (shouldStart) {
				instrumenter.end(spanContext, methodRequest, null, null);
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(this, tc, "spanContext " + spanContext.toString() + " has ended");
				}
			}
			return result;
		} finally {
			if (scope != null) {
				scope.close();
			}
		}
	}

	protected static final class MethodRequestSpanNameExtractor implements SpanNameExtractor<MethodRequest> {
		@Override
		public String extract(final MethodRequest methodRequest) {
			return AccessController.doPrivileged((PrivilegedAction<String>) () -> {
				WithSpan annotation = methodRequest.getMethod().getDeclaredAnnotation(WithSpan.class);
				String spanName = annotation.value();
				if (spanName.isEmpty()) {
					spanName = SpanNames.fromMethod(methodRequest.getMethod());
				}
				return spanName;
			});
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
}
