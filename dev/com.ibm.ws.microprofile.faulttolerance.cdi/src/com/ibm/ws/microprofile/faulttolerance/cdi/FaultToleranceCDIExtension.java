/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.faulttolerance.cdi;

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.tx.jta.cdi.interceptors.TransactionalInterceptor;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.AnnotationConfigFactory;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.AsynchronousConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.BulkheadConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.CircuitBreakerConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.FallbackConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.RetryConfig;
import com.ibm.ws.microprofile.faulttolerance.cdi.config.TimeoutConfig;

@Component(service = WebSphereCDIExtension.class, immediate = true, property = { "service.vendor=IBM", "application.bdas.visible=true" })
public class FaultToleranceCDIExtension implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(FaultToleranceCDIExtension.class);

    private static final String shuffleInterceptorsPropertyName = "com.ibm.ws.microprofile.faulttolerance.before.transactional";

    private static AnnotationConfigFactory annotationConfigFactory;

    public static AnnotationConfigFactory getAnnotationConfigFactory() {
        return annotationConfigFactory;
    }

    @Reference
    protected void setAnnotationConfigFactory(AnnotationConfigFactory factory) {
        FaultToleranceCDIExtension.annotationConfigFactory = factory;
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        //register the interceptor binding and in the interceptor itself
        AnnotatedType<FaultTolerance> bindingType = beanManager.createAnnotatedType(FaultTolerance.class);
        beforeBeanDiscovery.addInterceptorBinding(bindingType);
        AnnotatedType<FaultToleranceInterceptor> interceptorType = beanManager.createAnnotatedType(FaultToleranceInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(interceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(interceptorType, this.getClass()));
        AnnotatedType<FaultToleranceInterceptor.ExecutorCleanup> executorCleanup = beanManager.createAnnotatedType(FaultToleranceInterceptor.ExecutorCleanup.class);
        beforeBeanDiscovery.addAnnotatedType(executorCleanup, CDIServiceUtils.getAnnotatedTypeIdentifier(executorCleanup, this.getClass()));
    }

    public <T> void processAnnotatedType(@Observes @WithAnnotations({ Asynchronous.class, Fallback.class, Timeout.class, CircuitBreaker.class, Retry.class,
                                                                      Bulkhead.class }) ProcessAnnotatedType<T> processAnnotatedType,
                                         BeanManager beanManager) {

        //TODO this method generates all of the Config objects in order to validate them. These should be cached
        //so that we can use them again at runtime and don't have to re-parse and re-validate

        FTEnablementConfig config = FaultToleranceCDIComponent.getEnablementConfig();

        Set<AnnotatedMethod<?>> interceptedMethods = new HashSet<AnnotatedMethod<?>>();
        boolean interceptedClass = false;
        Asynchronous classLevelAsync = null;

        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();
        //get the target class
        Class<?> clazz = processAnnotatedType.getAnnotatedType().getJavaClass();
        //look at the class level annotations
        Set<Annotation> annotations = annotatedType.getAnnotations();
        for (Annotation annotation : annotations) {
            if (config.isFaultTolerance(annotation)) {
                //if we find any of the fault tolerance annotations on the class then we will add the intereceptor binding to the class
                if (config.isAnnotationEnabled(annotation, clazz)) {
                    interceptedClass = true;
                    if (annotation.annotationType() == Asynchronous.class) {
                        AsynchronousConfig asynchronousConfig = annotationConfigFactory.createAsynchronousConfig(clazz, (Asynchronous) annotation);
                        asynchronousConfig.validate();
                        classLevelAsync = (Asynchronous) annotation;
                    } else if (annotation.annotationType() == Retry.class) {
                        RetryConfig retry = new RetryConfig(clazz, (Retry) annotation);
                        retry.validate();
                    } else if (annotation.annotationType() == Timeout.class) {
                        TimeoutConfig timeout = new TimeoutConfig(clazz, (Timeout) annotation);
                        timeout.validate();
                    } else if (annotation.annotationType() == CircuitBreaker.class) {
                        CircuitBreakerConfig circuitBreaker = annotationConfigFactory.createCircuitBreakerConfig(clazz, (CircuitBreaker) annotation);
                        circuitBreaker.validate();
                    } else if (annotation.annotationType() == Bulkhead.class) {
                        BulkheadConfig bulkhead = new BulkheadConfig(clazz, (Bulkhead) annotation);
                        bulkhead.validate();
                    }
                } else {
                    if (isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Annotation {0} on {1} was disabled and will be ignored", annotation.annotationType().getSimpleName(), clazz.getCanonicalName());
                    }
                }
            }
        }

        //now loop through the methods
        Set<AnnotatedMethod<? super T>> methods = annotatedType.getMethods();
        for (AnnotatedMethod<?> method : methods) {
            boolean needsIntercepting = processMethod(method, clazz, classLevelAsync);
            if (needsIntercepting) {
                interceptedMethods.add(method);
            }
        }

        //TODO we really ought to get rid of the "synthetic" FaultTolerance interceptor binding annotation
        //it isn't needed since all of the API annotations are also interceptor bindings

        //if there were any FT annotations on the class or methods then add the interceptor binding to the methods
        if (interceptedClass || !interceptedMethods.isEmpty()) {
            addFaultToleranceAnnotation(beanManager, processAnnotatedType, interceptedClass, interceptedMethods);
        }
    }

    /**
     * Validate a method and return whether it has fault tolerance annotations which require us to add the FT interceptor
     *
     * @param method          the method to process
     * @param clazz           the class which declares the method
     * @param classLevelAsync whether the declaring class is annotated with {@code @Asynchronous}
     * @return true if the method requries the FT interceptor, false otherwise
     */
    private <T> boolean processMethod(AnnotatedMethod<T> method, Class<?> clazz, Asynchronous classLevelAsync) {
        FTEnablementConfig config = FaultToleranceCDIComponent.getEnablementConfig();
        Method javaMethod = method.getJavaMember();

        if (javaMethod.isBridge()) {
            // Skip all validation for bridge methods
            // Bridge methods are created when a class overrides a method but provides more specific return or parameter types
            // (usually when implementing a generic interface)

            // In these cases, the bridge method matches the signature of the overridden method after type erasure and delegates directly to the overriding method
            // In some cases, the signature of the overriding method is valid for some microprofile annotation, but the signature of the bridge method is not
            // However, the user's code is valid, and weld seems to make sure that any interceptors get called with the real method in the InvocationContext.
            return false;
        }

        if (classLevelAsync != null) {
            AsynchronousConfig asynchronous = annotationConfigFactory.createAsynchronousConfig(javaMethod, clazz, classLevelAsync);
            asynchronous.validate();
        }

        boolean needsIntercepting = false;
        Set<Annotation> annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
            if (config.isFaultTolerance(annotation)) {
                if (config.isAnnotationEnabled(annotation, clazz, method.getJavaMember())) {
                    needsIntercepting = true;
                    if (annotation.annotationType() == Asynchronous.class) {
                        AsynchronousConfig asynchronous = annotationConfigFactory.createAsynchronousConfig(javaMethod, clazz, (Asynchronous) annotation);
                        asynchronous.validate();
                    } else if (annotation.annotationType() == Fallback.class) {
                        FallbackConfig fallback = annotationConfigFactory.createFallbackConfig(javaMethod, clazz, (Fallback) annotation);
                        fallback.validate();
                    } else if (annotation.annotationType() == Retry.class) {
                        RetryConfig retry = new RetryConfig(javaMethod, clazz, (Retry) annotation);
                        retry.validate();
                    } else if (annotation.annotationType() == Timeout.class) {
                        TimeoutConfig timeout = new TimeoutConfig(javaMethod, clazz, (Timeout) annotation);
                        timeout.validate();
                    } else if (annotation.annotationType() == CircuitBreaker.class) {
                        CircuitBreakerConfig circuitBreaker = annotationConfigFactory.createCircuitBreakerConfig(javaMethod, clazz, (CircuitBreaker) annotation);
                        circuitBreaker.validate();
                    } else if (annotation.annotationType() == Bulkhead.class) {
                        BulkheadConfig bulkhead = new BulkheadConfig(javaMethod, clazz, (Bulkhead) annotation);
                        bulkhead.validate();
                    }
                } else {
                    if (isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Annotation {0} on {1} was disabled and will be ignored", annotation.annotationType().getSimpleName(),
                                 clazz.getCanonicalName() + "." + method.getJavaMember().getName());
                    }
                }
            }
        }

        return needsIntercepting;
    }

    private <T> void addFaultToleranceAnnotation(BeanManager beanManager, ProcessAnnotatedType<T> processAnnotatedType, boolean interceptedClass,
                                                 Set<AnnotatedMethod<?>> interceptedMethods) {
        AnnotatedTypeWrapper<T> wrapper = new AnnotatedTypeWrapper<T>(beanManager, processAnnotatedType.getAnnotatedType(), interceptedClass, interceptedMethods);
        processAnnotatedType.setAnnotatedType(wrapper);
    }

    // Move the FaultToleranceInterceptor up before the Transactional interceptors if required
    public void afterTypeDiscovery(@Observes AfterTypeDiscovery atd) {
        if (ConfigProvider.getConfig().getOptionalValue(shuffleInterceptorsPropertyName, Boolean.class).orElse(false)) {

            /* Run along the list of interceptors and find the indices of the first @Transactional one and the Fault Tolerance one */
            int faultToleranceInterceptorIndex = -1;
            int transactionalInterceptorIndex = -1;
            int interceptorIndex = 0;
            for (Iterator<Class<?>> iterator = atd.getInterceptors().iterator(); iterator.hasNext()
                                                                                 && !(faultToleranceInterceptorIndex >= 0 && transactionalInterceptorIndex >= 0);) {
                Class<?> i = iterator.next();
                if (FaultToleranceInterceptor.class.equals(i)) {
                    /* Found the Fault Tolerance interceptor */
                    faultToleranceInterceptorIndex = interceptorIndex;
                } else if (transactionalInterceptorIndex < 0) {
                    if (TransactionalInterceptor.class.isAssignableFrom(i)) {
                        /* Found the first @Transactional interceptor */
                        transactionalInterceptorIndex = interceptorIndex;
                    }
                }
                interceptorIndex++;
            }

            /* If we found both types of interceptor we need to move the Fault Tolerance one up before the @Transactional one */
            if (faultToleranceInterceptorIndex >= 0 && transactionalInterceptorIndex >= 0) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Reordering fault tolerance with respect to @Transactional");
                Class<?> c = atd.getInterceptors().remove(faultToleranceInterceptorIndex);
                atd.getInterceptors().add(transactionalInterceptorIndex, c);
            }
        }
    }
}
