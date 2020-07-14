/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Priority;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.AnnotationConfigFactory;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.AsynchronousConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.BulkheadConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.CircuitBreakerConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.FallbackConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.RetryConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.TimeoutConfig;
import com.ibm.ws.microprofile.faulttolerance.spi.BulkheadPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.CircuitBreakerPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.ExecutionException;
import com.ibm.ws.microprofile.faulttolerance.spi.Executor;
import com.ibm.ws.microprofile.faulttolerance.spi.FallbackPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.RetryPolicy;
import com.ibm.ws.microprofile.faulttolerance.spi.TimeoutPolicy;

@FaultTolerance
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE) //run this interceptor after platform interceptors but before application interceptors
public class FaultToleranceInterceptor {

    @Inject
    private BeanManager beanManager;

    @Inject
    private PolicyStore policyStore;

    @Inject
    @Intercepted
    private Bean<?> bean;

    @AroundInvoke
    public Object executeFT(InvocationContext context) throws Exception {

        AggregatedFTPolicy policy = getFTPolicies(context);

        Object result = execute(context, policy);

        return result;
    }

    /**
     * @param context
     * @return
     */
    private AggregatedFTPolicy getFTPolicies(InvocationContext context) {
        AggregatedFTPolicy policy = null;
        Method method = context.getMethod();
        policy = policyStore.getOrCreate(bean, method, () -> processPolicies(context, beanManager));
        return policy;
    }

    /**
     * @param context
     * @return
     */
    private AggregatedFTPolicy processPolicies(InvocationContext context, BeanManager beanManager) {
        AsynchronousConfig asynchronous = null;
        RetryConfig retry = null;
        CircuitBreakerConfig circuitBreaker = null;
        TimeoutConfig timeout = null;
        BulkheadConfig bulkhead = null;
        FallbackConfig fallback = null;
        FTEnablementConfig enablement = FaultToleranceCDIComponent.getEnablementConfig();
        AnnotationConfigFactory annotationConfigFactory = FaultToleranceCDIExtension.getAnnotationConfigFactory();

        //first check the annotations on the target class
        Class<?> targetClass = context.getTarget().getClass();
        Annotation[] annotations = targetClass.getAnnotations();
        for (Annotation annotation : annotations) {

            //Check that the annotation has not been disabled for this specific class.
            if (enablement.isFaultTolerance(annotation) && enablement.isAnnotationEnabled(annotation, targetClass)) {

                if (annotation.annotationType().equals(Asynchronous.class)) {
                    asynchronous = annotationConfigFactory.createAsynchronousConfig(targetClass, (Asynchronous) annotation);
                    asynchronous.validate();
                } else if (annotation.annotationType().equals(Retry.class)) {
                    retry = new RetryConfig(targetClass, (Retry) annotation);
                    retry.validate();
                } else if (annotation.annotationType().equals(CircuitBreaker.class)) {
                    circuitBreaker = annotationConfigFactory.createCircuitBreakerConfig(targetClass, (CircuitBreaker) annotation);
                    circuitBreaker.validate();
                } else if (annotation.annotationType().equals(Timeout.class)) {
                    timeout = new TimeoutConfig(targetClass, (Timeout) annotation);
                    timeout.validate();
                } else if (annotation.annotationType().equals(Bulkhead.class)) {
                    bulkhead = new BulkheadConfig(targetClass, (Bulkhead) annotation);
                    bulkhead.validate();
                }
            }
        }

        //then look for annotations on the specific method
        //method level annotations override class level ones
        Method method = context.getMethod();
        annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {

            //Check that the annotation has not been disabled for this specific method.
            if (enablement.isFaultTolerance(annotation) && enablement.isAnnotationEnabled(annotation, targetClass, method)) {

                if (annotation.annotationType().equals(Asynchronous.class)) {
                    asynchronous = annotationConfigFactory.createAsynchronousConfig(method, targetClass, (Asynchronous) annotation);
                    asynchronous.validate();
                } else if (annotation.annotationType().equals(Retry.class)) {
                    retry = new RetryConfig(method, targetClass, (Retry) annotation);
                    retry.validate();
                } else if (annotation.annotationType().equals(CircuitBreaker.class)) {
                    circuitBreaker = annotationConfigFactory.createCircuitBreakerConfig(method, targetClass, (CircuitBreaker) annotation);
                    circuitBreaker.validate();
                } else if (annotation.annotationType().equals(Timeout.class)) {
                    timeout = new TimeoutConfig(method, targetClass, (Timeout) annotation);
                    timeout.validate();
                } else if (annotation.annotationType().equals(Bulkhead.class)) {
                    bulkhead = new BulkheadConfig(method, targetClass, (Bulkhead) annotation);
                    bulkhead.validate();
                } else if (annotation.annotationType().equals(Fallback.class)) {
                    fallback = annotationConfigFactory.createFallbackConfig(method, targetClass, (Fallback) annotation);
                    fallback.validate();
                }
            }
        }

        AggregatedFTPolicy aggregatedFTPolicy = new AggregatedFTPolicy();

        aggregatedFTPolicy.setMethod(method);

        if (asynchronous != null) {
            aggregatedFTPolicy.setAsynchronousResultWrapper(method.getReturnType());
        }

        //generate the TimeoutPolicy
        if (timeout != null) {
            TimeoutPolicy timeoutPolicy = timeout.generatePolicy();
            aggregatedFTPolicy.setTimeoutPolicy(timeoutPolicy);
        }

        //generate the RetryPolicy
        if (retry != null) {
            RetryPolicy retryPolicy = retry.generatePolicy();
            aggregatedFTPolicy.setRetryPolicy(retryPolicy);
        }

        //generate the CircuitBreakerPolicy
        if (circuitBreaker != null) {
            CircuitBreakerPolicy circuitBreakerPolicy = circuitBreaker.generatePolicy();
            aggregatedFTPolicy.setCircuitBreakerPolicy(circuitBreakerPolicy);
        }

        //generate the BulkheadPolicy
        if (bulkhead != null) {
            BulkheadPolicy bulkheadPolicy = bulkhead.generatePolicy();
            aggregatedFTPolicy.setBulkheadPolicy(bulkheadPolicy);
        }

        //generate the FallbackPolicy
        if (fallback != null) {
            FallbackPolicy fallbackPolicy = fallback.generatePolicy(context, beanManager);
            aggregatedFTPolicy.setFallbackPolicy(fallbackPolicy);
        }

        return aggregatedFTPolicy;
    }

    @FFDCIgnore({ ExecutionException.class })
    private Object execute(InvocationContext invocationContext, AggregatedFTPolicy aggregatedFTPolicy) throws Exception {
        Object result = null;
        //if there is a set of FaultTolerance policies then run it, otherwise just call proceed
        if (aggregatedFTPolicy != null) {

            Executor<Object> executor = aggregatedFTPolicy.getExecutor();

            Method method = invocationContext.getMethod();
            Object[] params = invocationContext.getParameters();
            ExecutionContext executionContext = executor.newExecutionContext(generateId(method), method, params);

            Callable<Object> callable = () -> {
                return invocationContext.proceed();
            };

            try {
                result = executor.execute(callable, executionContext);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                } else if (cause instanceof Error) {
                    throw (Error) cause;
                } else {
                    throw e;
                }
            }

        } else {
            result = invocationContext.proceed();
        }
        return result;
    }

    @Trivial
    private String generateId(Method method) {
        int rand = ThreadLocalRandom.current().nextInt();
        return method.getName() + "-" + Integer.toHexString(rand);
    }

}
