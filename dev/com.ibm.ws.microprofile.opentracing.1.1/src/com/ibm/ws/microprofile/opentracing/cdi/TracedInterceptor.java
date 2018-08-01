/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.opentracing.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;

import org.eclipse.microprofile.opentracing.Traced;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.opentracing.OpenTracingService;
import com.ibm.ws.opentracing.OpentracingService;
import com.ibm.ws.opentracing.OpentracingTracerManager;

import io.opentracing.Scope;
import io.opentracing.Tracer;

/**
 * {@code #execute(InvocationContext)} is called for every method with the {@code Traced} annotation
 * or every method of a class with the {@code Traced} annotation. This interceptor is run after platform
 * interceptors but before application interceptors using {@code Interceptor.Priority.LIBRARY_BEFORE}.
 * If we figure out that the method in question is a JAXRS method, then we skip it because we know
 * it has been processed by {@code OpentracingContainerFilter} (which has the requisite knowledge
 * about the incoming URI which this class won't have).
 */
@Traced
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
public class TracedInterceptor {

    private static final TraceComponent tc = Tr.register(TracedInterceptor.class);

    /**
     * See class comment.
     *
     * @param context Information about the wrapped method.
     * @return Result of the invoking the method.
     * @throws Exception Thrown by wrapped method.
     */
    @AroundInvoke
    public Object execute(InvocationContext context) throws Exception {
        String methodName = "execute";

        String classOperationName = OpenTracingService.getClassOperationName(context.getMethod());
        String methodOperationName = OpenTracingService.getMethodOperationName(context.getMethod());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " operation names", classOperationName, methodOperationName);
        }

        boolean process = true;

        if (!OpenTracingService.isTraced(classOperationName, methodOperationName)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " skipping untraced method");
            }
            process = false;
        }

        if (process && isHandledByFilter(context.getMethod())) {
            // This is already processed as part of the OpentracingContainerFilter
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " skipping JAXRS method");
            }
            process = false;
        }

        if (process) {

            String operationName;

            if (OpenTracingService.hasExplicitOperationName(methodOperationName)) {
                operationName = methodOperationName;
            } else {
                // If the annotated method is not a JAX-RS endpoint, the default
                // operation name of the new Span for the method is:
                // <package name>.<class name>.<method name> [...]
                // If operationName is specified on a class, that operationName will be used
                // for all methods of the class unless a method explicitly overrides it with
                // its own operationName."
                // https://github.com/eclipse/microprofile-opentracing/blob/master/spec/src/main/asciidoc/microprofile-opentracing.asciidoc#321-the-traced-annotation
                if (OpenTracingService.hasExplicitOperationName(classOperationName)) {
                    operationName = classOperationName;
                } else {
                    operationName = context.getMethod().getDeclaringClass().getName() + "." + context.getMethod().getName();
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " setting default operationName", operationName);
                }
            }

            Tracer tracer = OpentracingTracerManager.getTracer();

            try (Scope scope = tracer.buildSpan(operationName).startActive(true)) {
                try {
                    Object result = context.proceed();
                    return result;
                } catch (Exception e) {
                    OpentracingService.addSpanErrorInfo(scope.span(), e);
                    throw e;
                } catch (Error e) {
                    OpentracingService.addSpanErrorInfo(scope.span(), e);
                    throw e;
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " normal processing");
            }
            return context.proceed();
        }
    }

    /**
     * Check if the {@code method} is expected to be handled by
     * {@code OpentracingContainerFilter}.
     *
     * @param method Wrapped method.
     * @return True if handled by the filter.
     */
    private boolean isHandledByFilter(Method method) {
        // "Resource classes are POJOs that have at least one method annotated
        // with @Path or a request method designator. [...]
        //
        // Resource methods are methods of a resource class annotated with a
        // request method designator. [...] A request method designator is a
        // runtime annotation that is annotated with the @HttpMethod
        // annotation."
        for (Annotation annotation : method.getAnnotations()) {
            if (HttpMethod.class.isAssignableFrom(annotation.annotationType())) {
                return true;
            }
        }

        if (method.isAnnotationPresent(Path.class)) {
            return true;
        }

        return false;
    }

}