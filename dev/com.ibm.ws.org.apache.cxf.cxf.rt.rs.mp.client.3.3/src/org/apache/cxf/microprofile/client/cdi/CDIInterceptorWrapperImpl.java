/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.microprofile.client.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;

import org.apache.cxf.common.logging.LogUtils;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;


class CDIInterceptorWrapperImpl implements CDIInterceptorWrapper {
    private static final Logger LOG = LogUtils.getL7dLogger(CDIInterceptorWrapperImpl.class);
    private static final Class<?> FT_ANNO_CLASS = getFTAnnotationClass(); //Liberty change

    private final CreationalContext<?> creationalContext;
    private final Map<Method, List<InterceptorInvoker>> interceptorInvokers;

    CDIInterceptorWrapperImpl(Class<?> restClient, Object beanManagerObject) {
        BeanManager beanManager = (BeanManager) beanManagerObject;
        creationalContext = beanManager != null ? beanManager.createCreationalContext(null) : null;
        interceptorInvokers = initInterceptorInvokers(beanManager, creationalContext, restClient);
    }

    // Liberty change begin
    @FFDCIgnore(PrivilegedActionException.class)
    private static Class<?> getFTAnnotationClass() {
        try {
            return AccessController.doPrivileged(
                (PrivilegedExceptionAction<Class<?>>) () -> {
                    return Class.forName("com.ibm.ws.microprofile.faulttolerance.cdi.FaultTolerance");
            });
        } catch (PrivilegedActionException pae) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "Exception checking for MP Fault Tolerance class - " +
                                      "expected if FT feature isnot enabled", pae);
            }
            return null;
        }
    }

    private static Annotation getFTAnnotation() {
        return new Annotation() {

            @SuppressWarnings("unchecked")
            @Override
            public Class<? extends Annotation> annotationType() {
                return (Class<? extends Annotation>) FT_ANNO_CLASS;
            }};
    }

    private static boolean containsFTannotation(List<Annotation> interceptorBindings) {
        for (Annotation anno : interceptorBindings) {
            String className = anno.annotationType().getName();
            if (className.startsWith("org.eclipse.microprofile.faulttolerance")) {
                return true;
            }
        }
        return false;
    }

    private static List<Interceptor<?>> getFTInterceptor(BeanManager beanManager) {
        Annotation ftAnno = getFTAnnotation();
        return beanManager.resolveInterceptors(InterceptionType.AROUND_INVOKE, ftAnno);
    }
    // Liberty change end

    private static Map<Method, List<InterceptorInvoker>> initInterceptorInvokers(BeanManager beanManager,
                                                                                 CreationalContext<?> creationalContext,
                                                                                 Class<?> restClient) {
        Map<Method, List<InterceptorInvoker>> invokers = new HashMap<>();
        // Interceptor as a key in a map is not entirely correct (custom interceptors) but should work in most cases
        Map<Interceptor<?>, Object> interceptorInstances = new HashMap<>();
        
        AnnotatedType<?> restClientType = beanManager.createAnnotatedType(restClient);

        List<Annotation> classBindings = getBindings(restClientType.getAnnotations(), beanManager);

        for (AnnotatedMethod<?> method : restClientType.getMethods()) {
            Method javaMethod = method.getJavaMember();
            if (javaMethod.isDefault() || method.isStatic()) {
                continue;
            }
            List<Annotation> methodBindings = getBindings(method.getAnnotations(), beanManager);

            if (!classBindings.isEmpty() || !methodBindings.isEmpty()) {
                // Liberty change begin - handle aggregated FT annotation
                if (FT_ANNO_CLASS != null && containsFTannotation(methodBindings)) {
                    methodBindings.add(getFTAnnotation());
                }
                // Liberty change end
                Annotation[] interceptorBindings = merge(methodBindings, classBindings);

                List<Interceptor<?>> interceptors =
                    new ArrayList<>(beanManager.resolveInterceptors(InterceptionType.AROUND_INVOKE, 
                                                                    interceptorBindings));
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Resolved interceptors from beanManager, " + beanManager + ":" + interceptors);
                }

                if (!interceptors.isEmpty()) {
                    List<InterceptorInvoker> chain = new ArrayList<>();
                    for (Interceptor<?> interceptor : interceptors) {
                        chain.add(
                            new InterceptorInvoker(interceptor, 
                                                   interceptorInstances.computeIfAbsent(interceptor, 
                                                       i -> beanManager.getReference(i, 
                                                                                     i.getBeanClass(), 
                                                                                     creationalContext))));
                    }
                    invokers.put(javaMethod, chain);
                }
            }
        }
        return invokers.isEmpty() ? Collections.emptyMap() : invokers;
    }

    private static Annotation[] merge(List<Annotation> methodBindings, List<Annotation> classBindings) {
        Set<Class<? extends Annotation>> types = methodBindings.stream()
                                                               .map(a -> a.annotationType())
                                                               .collect(Collectors.toSet());
        List<Annotation> merged = new ArrayList<>(methodBindings);
        for (Annotation annotation : classBindings) {
            if (!types.contains(annotation.annotationType())) {
                merged.add(annotation);
            }
        }
        return merged.toArray(new Annotation[] {});
    }

    private static List<Annotation> getBindings(Set<Annotation> annotations, BeanManager beanManager) {
        if (annotations == null || annotations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Annotation> bindings = new ArrayList<>();
        for (Annotation annotation : annotations) {
            if (beanManager.isInterceptorBinding(annotation.annotationType())) {
                bindings.add(annotation);
            }
        }
        return bindings;
    }

    @Override
    @Trivial // Liberty change - avoids StackOverflowError when traced
    public Object invoke(Object restClient, Method method, Object[] params, Callable<Object> callable) 
        throws Exception {

        List<InterceptorInvoker> invokers = interceptorInvokers.get(method);
        if (invokers == null || invokers.isEmpty()) {
            return callable.call();
        }
        return new MPRestClientInvocationContextImpl(restClient, method, params, invokers, callable).proceed();
    }
}
