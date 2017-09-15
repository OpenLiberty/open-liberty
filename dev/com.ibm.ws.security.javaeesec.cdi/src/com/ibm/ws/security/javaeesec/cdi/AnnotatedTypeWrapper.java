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
package com.ibm.ws.security.javaeesec.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

public class AnnotatedTypeWrapper<T> implements AnnotatedType<T> {

    private final AnnotatedType<T> wrapped;
    private final Set<Annotation> annotations;

    public AnnotatedTypeWrapper(AnnotatedType<T> wrapped, Set<Annotation> annotations) {
        this.wrapped = wrapped;
        this.annotations = new HashSet<Annotation>(annotations);
    }

    public void addAnnotation(Annotation annotation) {
        // When a new annotation is being added, remove the same annotation class from the list,
        // in order to make sure that only one annotation class exists.
        removeDuplicate(annotations, annotation);
        annotations.add(annotation);
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        for (Annotation annotation : annotations) {
            if (annotationType.isInstance(annotation)) {
                return (A) annotation;
            }
        }
        return null;
    }

    @Override
    public Set<AnnotatedConstructor<T>> getConstructors() {
        return wrapped.getConstructors();
    }

    @Override
    public Set<AnnotatedField<? super T>> getFields() {
        return wrapped.getFields();
    }

    @Override
    public Class<T> getJavaClass() {
        return wrapped.getJavaClass();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.Annotated#getAnnotations()
     */
    @Override
    public Set<Annotation> getAnnotations() {
        return annotations;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.Annotated#getBaseType()
     */
    @Override
    public Type getBaseType() {
        return wrapped.getBaseType();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.Annotated#getTypeClosure()
     */
    @Override
    public Set<Type> getTypeClosure() {
        return wrapped.getTypeClosure();
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.Annotated#isAnnotationPresent(java.lang.Class)
     */
    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        for (Annotation annotation : annotations) {
            if (annotationType.isInstance(annotation)) {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.enterprise.inject.spi.AnnotatedType#getMethods()
     */
    @Override
    public Set<AnnotatedMethod<? super T>> getMethods() {
        return wrapped.getMethods();
    }

    protected void removeDuplicate(Set<Annotation> annotations, Annotation annotation) {
        Annotation toBeRemoved = null;
        for (Annotation item : annotations) {
            if (item.annotationType().equals(annotation.annotationType())) {
                toBeRemoved = item;
            }
        }
        if (toBeRemoved != null) {
            annotations.remove(toBeRemoved);
        }
    }

}
