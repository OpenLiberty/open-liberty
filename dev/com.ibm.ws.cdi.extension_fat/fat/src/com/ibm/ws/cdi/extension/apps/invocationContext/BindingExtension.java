/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.extension.apps.invocationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;

/**
 * Extension which adds interceptor bindings using the {@code ProcessAnnotatedType} event.
 */
public class BindingExtension implements Extension {

    @SuppressWarnings("serial")
    private static class MyAddedByExtensionBindingLiteral extends AnnotationLiteral<MyAddedByExtensionBinding> implements MyAddedByExtensionBinding {};

    private static final Annotation ADDED_BY_EXTENSION_BINDING = new MyAddedByExtensionBindingLiteral();

    /**
     * Add {@code @MyAddedByExtensionBinding} to {@code InterceptedBean}
     *
     * @param pat event
     */
    public void addClassAnnotation(@Observes ProcessAnnotatedType<InterceptedBean> pat) {
        pat.setAnnotatedType(addAnnotation(pat.getAnnotatedType(), ADDED_BY_EXTENSION_BINDING));
    }

    /**
     * Add {@code @MyAddedByExtensionBinding} to {@code InterceptedMethodsBean.iExist}
     *
     * @param pat event
     */
    public void addMethodAnnotation(@Observes ProcessAnnotatedType<InterceptedMethodsBean> pat) {
        AnnotatedType<InterceptedMethodsBean> type = pat.getAnnotatedType();
        AnnotatedMethod<? super InterceptedMethodsBean> method = type.getMethods()
                                                                     .stream()
                                                                     .filter(m -> m.getJavaMember().getName().equals("iExist"))
                                                                     .findFirst()
                                                                     .get();

        AnnotatedMethod<? super InterceptedMethodsBean> newMethod = addAnnotation(method, ADDED_BY_EXTENSION_BINDING);
        AnnotatedType<InterceptedMethodsBean> newType = replaceMethod(type, method, newMethod);

        pat.setAnnotatedType(newType);
    }

    /*
     * -----------------------------------------------------------------------------------
     * Rest of the class is long-winded helper methods for adding annotations to classes and methods.
     * This is much easier in CDI 2.0, but this test also runs on CDI 1.2.
     * -----------------------------------------------------------------------------------
     */

    private <X> AnnotatedType<X> addAnnotation(AnnotatedType<X> type, Annotation newAnnotation) {

        Set<Annotation> annotations = new HashSet<>(type.getAnnotations());
        annotations.add(newAnnotation);

        return new AnnotatedType<X>() {

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> clazz) {
                if (newAnnotation.annotationType() == clazz) {
                    return clazz.cast(newAnnotation);
                } else {
                    return type.getAnnotation(clazz);
                }
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> clazz) {
                if (newAnnotation.annotationType() == clazz) {
                    return true;
                } else {
                    return type.isAnnotationPresent(clazz);
                }
            }

            @Override
            public Set<Annotation> getAnnotations() {
                return annotations;
            }

            @Override
            public Type getBaseType() {
                return type.getBaseType();
            }

            @Override
            public Set<AnnotatedConstructor<X>> getConstructors() {
                return type.getConstructors();
            }

            @Override
            public Set<AnnotatedField<? super X>> getFields() {
                return type.getFields();
            }

            @Override
            public Class<X> getJavaClass() {
                return type.getJavaClass();
            }

            @Override
            public Set<AnnotatedMethod<? super X>> getMethods() {
                return type.getMethods();
            }

            @Override
            public Set<Type> getTypeClosure() {
                return type.getTypeClosure();
            }
        };
    }

    private <X> AnnotatedMethod<X> addAnnotation(AnnotatedMethod<X> method, Annotation newAnnotation) {
        Set<Annotation> annotations = new HashSet<>(method.getAnnotations());
        annotations.add(ADDED_BY_EXTENSION_BINDING);

        AnnotatedMethod<X> result = new AnnotatedMethod<X>() {

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> clazz) {
                if (newAnnotation.annotationType() == clazz) {
                    return clazz.cast(newAnnotation);
                } else {
                    return method.getAnnotation(clazz);
                }
            }

            @Override
            public Set<Annotation> getAnnotations() {
                return annotations;
            }

            @Override
            public Type getBaseType() {
                return method.getBaseType();
            }

            @Override
            public AnnotatedType<X> getDeclaringType() {
                return method.getDeclaringType();
            }

            @Override
            public Method getJavaMember() {
                return method.getJavaMember();
            }

            @Override
            public List<AnnotatedParameter<X>> getParameters() {
                return method.getParameters();
            }

            @Override
            public Set<Type> getTypeClosure() {
                return method.getTypeClosure();
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> clazz) {
                if (newAnnotation.annotationType() == clazz) {
                    return true;
                } else {
                    return method.isAnnotationPresent(clazz);
                }
            }

            @Override
            public boolean isStatic() {
                return method.isStatic();
            }
        };
        return result;
    }

    private <X> AnnotatedType<X> replaceMethod(AnnotatedType<X> newType,
                                               AnnotatedMethod<? super X> method,
                                               AnnotatedMethod<? super X> newMethod) {

        Set<AnnotatedMethod<? super X>> methods = new HashSet<>(newType.getMethods());
        methods.remove(method);
        methods.add(newMethod);

        AnnotatedType<X> result = new AnnotatedType<X>() {

            @Override
            public Set<AnnotatedMethod<? super X>> getMethods() {
                return methods;
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> clazz) {
                return newType.getAnnotation(clazz);
            }

            @Override
            public Set<Annotation> getAnnotations() {
                return newType.getAnnotations();
            }

            @Override
            public Type getBaseType() {
                return newType.getBaseType();
            }

            @Override
            public Set<AnnotatedConstructor<X>> getConstructors() {
                return newType.getConstructors();
            }

            @Override
            public Set<AnnotatedField<? super X>> getFields() {
                return newType.getFields();
            }

            @Override
            public Class<X> getJavaClass() {
                return newType.getJavaClass();
            }

            @Override
            public Set<Type> getTypeClosure() {
                return newType.getTypeClosure();
            }

            @Override
            public boolean isAnnotationPresent(Class<? extends Annotation> arg0) {
                return newType.isAnnotationPresent(arg0);
            }
        };
        return result;
    }

}
