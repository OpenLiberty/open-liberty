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
package com.ibm.ws.microprofile.faulttolerance.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.AsynchronousConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.BulkheadConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.CircuitBreakerConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.FTGlobalConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.FallbackConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.RetryConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.TimeoutConfig;

@Component(service = WebSphereCDIExtension.class, immediate = true)
public class FaultToleranceCDIExtension implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(FaultToleranceCDIExtension.class);

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        //register the interceptor binding and in the interceptor itself
        AnnotatedType<FaultTolerance> bindingType = beanManager.createAnnotatedType(FaultTolerance.class);
        beforeBeanDiscovery.addInterceptorBinding(bindingType);
        AnnotatedType<FaultToleranceInterceptor> interceptorType = beanManager.createAnnotatedType(FaultToleranceInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(interceptorType);
    }

    public <T> void processAnnotatedType(@Observes @WithAnnotations({ Asynchronous.class, Fallback.class, Timeout.class, CircuitBreaker.class, Retry.class,
                                                                      Bulkhead.class }) ProcessAnnotatedType<T> processAnnotatedType,
                                         BeanManager beanManager) {

        //TODO this method generates all of the Config objects in order to validate them. These should be cached
        //so that we can use them again at runtime and don't have to re-parse and re-validate

        Set<AnnotatedMethod<?>> interceptedMethods = new HashSet<AnnotatedMethod<?>>();
        boolean interceptedClass = false;
        Asynchronous classLevelAsync = null;

        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();
        //get the target class
        Class<?> clazz = processAnnotatedType.getAnnotatedType().getJavaClass();
        //look at the class level annotations
        Set<Annotation> annotations = annotatedType.getAnnotations();
        for (Annotation annotation : annotations) {
            //if we find any of the fault tolerance annotations on the class then we will add the intereceptor binding to the class
            if (FTGlobalConfig.getActiveAnnotations(clazz).contains(annotation.annotationType())) {
                interceptedClass = true;
                if (annotation.annotationType() == Asynchronous.class) {
                    AsynchronousConfig asynchronousConfig = new AsynchronousConfig(clazz, (Asynchronous) annotation);
                    asynchronousConfig.validate();
                    classLevelAsync = asynchronousConfig;
                } else if (annotation.annotationType() == Retry.class) {
                    RetryConfig retry = new RetryConfig(clazz, (Retry) annotation);
                    retry.validate();
                } else if (annotation.annotationType() == Timeout.class) {
                    TimeoutConfig timeout = new TimeoutConfig(clazz, (Timeout) annotation);
                    timeout.validate();
                } else if (annotation.annotationType() == CircuitBreaker.class) {
                    CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig(clazz, (CircuitBreaker) annotation);
                    circuitBreaker.validate();
                } else if (annotation.annotationType() == Bulkhead.class) {
                    BulkheadConfig bulkhead = new BulkheadConfig(clazz, (Bulkhead) annotation);
                    bulkhead.validate();
                }
            }
        }

        //now loop through the methods
        Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
        for (AnnotatedMethod<?> method : methods) {
            validateMethod(method, clazz, classLevelAsync);

            annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                if (FTGlobalConfig.getActiveAnnotations(clazz).contains(annotation.annotationType())) {
                    interceptedMethods.add(method);
                }
            }
        }

        //TODO we really ought to get rid of the "synthetic" FaultTolerance interceptor binding annotation
        //it isn't needed since all of the API annotations are also interceptor bindings

        //if there were any FT annotations on the class or methods then add the interceptor binding to the methods
        if (interceptedClass || !interceptedMethods.isEmpty()) {
            addFaultToleranceAnnotation(beanManager, processAnnotatedType, interceptedClass, interceptedMethods);
        }
    }

    private <T> void validateMethod(AnnotatedMethod<T> method, Class<?> clazz, Asynchronous classLevelAsync) {
        Method javaMethod = method.getJavaMember();

        if (javaMethod.isBridge()) {
            // Skip all validation for bridge methods
            // Bridge methods are created when a class overrides a method but provides more specific return or parameter types
            // (usually when implementing a generic interface)

            // In these cases, the bridge method matches the signature of the overridden method after type erasure and delegates directly to the overriding method
            // In some cases, the signature of the overriding method is valid for some microprofile annotation, but the signature of the bridge method is not
            // However, the user's code is valid, and weld seems to make sure that any interceptors get called with the real method in the InvocationContext.
            return;
        }

        if (classLevelAsync != null) {
            AsynchronousConfig asynchronous = new AsynchronousConfig(javaMethod, clazz, classLevelAsync);
            asynchronous.validate();
        }

        Set<Annotation> annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            if (FTGlobalConfig.getActiveAnnotations(clazz).contains(annotation.annotationType())) {
                if (annotation.annotationType() == Asynchronous.class) {
                    AsynchronousConfig asynchronous = new AsynchronousConfig(javaMethod, clazz, (Asynchronous) annotation);
                    asynchronous.validate();
                } else if (annotation.annotationType() == Fallback.class) {
                    FallbackConfig fallback = new FallbackConfig(javaMethod, clazz, (Fallback) annotation);
                    fallback.validate();
                } else if (annotation.annotationType() == Retry.class) {
                    RetryConfig retry = new RetryConfig(javaMethod, clazz, (Retry) annotation);
                    retry.validate();
                } else if (annotation.annotationType() == Timeout.class) {
                    TimeoutConfig timeout = new TimeoutConfig(javaMethod, clazz, (Timeout) annotation);
                    timeout.validate();
                } else if (annotation.annotationType() == CircuitBreaker.class) {
                    CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig(javaMethod, clazz, (CircuitBreaker) annotation);
                    circuitBreaker.validate();
                } else if (annotation.annotationType() == Bulkhead.class) {
                    BulkheadConfig bulkhead = new BulkheadConfig(javaMethod, clazz, (Bulkhead) annotation);
                    bulkhead.validate();
                }
            }
        }
    }

    private <T> void addFaultToleranceAnnotation(BeanManager beanManager, ProcessAnnotatedType<T> processAnnotatedType, boolean interceptedClass,
                                                 Set<AnnotatedMethod<?>> interceptedMethods) {
        AnnotatedTypeWrapper<T> wrapper = new AnnotatedTypeWrapper<T>(beanManager, processAnnotatedType.getAnnotatedType(), interceptedClass, interceptedMethods);
        processAnnotatedType.setAnnotatedType(wrapper);
    }
}
