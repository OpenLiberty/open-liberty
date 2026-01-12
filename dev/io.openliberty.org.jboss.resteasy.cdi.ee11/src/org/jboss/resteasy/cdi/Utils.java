/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2025 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// https://github.com/resteasy/resteasy/blob/e2a31761735a494b8fa0c6a0c9ef9ee843c9ca59/resteasy-cdi/src/main/java/org/jboss/resteasy/cdi/Utils.java
package org.jboss.resteasy.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.WriterInterceptor;

/**
 * Utility methods for detecting CDI scopes and JAX-RS components.
 *
 * @author Jozef Hartinger
 *
 */
public class Utils {

    private static final List<Class<?>> REST_INTERFACES = List.of(
            ContainerRequestFilter.class,
            ContainerResponseFilter.class,
            ContextResolver.class,
            DynamicFeature.class,
            ExceptionMapper.class,
            Feature.class,
            MessageBodyReader.class,
            MessageBodyWriter.class,
            ParamConverterProvider.class,
            ReaderInterceptor.class,
            WriterInterceptor.class);

    /**
     * Finds out if a given class is decorated with JAX-RS annotations.
     * Interfaces of the class are not scanned for JAX-RS annotations.
     *
     * @param clazz class
     * @return true if a given interface has @Path annotation or if any of its
     *         methods is decorated with @Path annotation or a request method
     *         designator.
     */
    public static boolean isJaxrsAnnotatedClass(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Path.class)) {
            return true;
        }
        if (clazz.isAnnotationPresent(Provider.class)) {
            return true;
        }
        return hasEndpointMethod(clazz); // Liberty Change
    }

    // Liberty Change Start
    /**
     * Checks each method on the class looking for any method annotated with {@link Path @Path} or any of the
     * {@link HttpMethod}'s.
     *
     * @param type the class to process the methods on
     *
     * @return {@code true} if this is determined to be an endpoint method
     */
    public static boolean hasEndpointMethod(final Class<?> type) {
        for (Method method : type.getMethods()) {
            if (method.isAnnotationPresent(Path.class)) {
                return true;
            }
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation.annotationType().isAnnotationPresent(HttpMethod.class)) {
                    return true;
                }
            }
        }
        return false;
    }
    // Liberty Change End

    /**
     * Returns true if and only if the given class is a JAX-RS root resource or a
     * sub-resource. The class itself as well as its interfaces are scanned for
     * JAX-RS annotations.
     *
     * @param clazz class
     * @return true if the given class is JAX-RS resource or sub-resource
     */
    public static boolean isJaxrsResource(Class<?> clazz) {
        if (isJaxrsAnnotatedClass(clazz)) {
            return true;
        }
        // Check if this implements any known Jakarta REST interfaces
        for (Class<?> intf : REST_INTERFACES) {
            if (intf.isAssignableFrom(clazz)) {
                return true;
            }
        }
        for (Class<?> intf : clazz.getInterfaces()) {
            if (isJaxrsAnnotatedClass(intf)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find out if a given class is a JAX-RS component
     *
     * @param clazz class
     * @return true if and only if a give class is a JAX-RS resource, provider or
     *         jakarta.ws.rs.core.Application subclass.
     */
    public static boolean isJaxrsComponent(Class<?> clazz) {
        return ((clazz.isAnnotationPresent(Provider.class)) || (isJaxrsResource(clazz))
                || (Application.class.isAssignableFrom(clazz)));
    }

    /**
     * Find out if a given annotated type is explicitly bound to a scope.
     *
     * @param annotatedType annotated type
     * @param manager       bean manager
     * @return true if and only if a given annotated type is annotated with a scope
     *         annotation or with a stereotype which (transitively) declares a
     *         scope
     */
    public static boolean isScopeDefined(AnnotatedType<?> annotatedType, BeanManager manager) {
        for (Annotation annotation : annotatedType.getAnnotations()) {
            if (manager.isScope(annotation.annotationType())) {
                return true;
            }
            if (manager.isStereotype(annotation.annotationType())) {
                if (isScopeDefined(annotation.annotationType(), manager)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Find out if a given class is explicitly bound to a scope.
     *
     * @param clazz   class
     * @param manager bean manager
     * @return <code>true</code> if a given class is annotated with a scope
     *         annotation or with a stereotype which (transitively) declares a
     *         scope
     */
    private static boolean isScopeDefined(Class<?> clazz, BeanManager manager) {
        for (Annotation annotation : clazz.getAnnotations()) {
            if (manager.isScope(annotation.annotationType())) {
                return true;
            }
            if (manager.isStereotype(annotation.annotationType())) {
                if (isScopeDefined(annotation.annotationType(), manager)) {
                    return true;
                }
            }
        }
        return false;
    }
}