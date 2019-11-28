/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************
 * Copyright © 2013 Antonin Stefanutti (antonin.stefanutti@gmail.com)
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
 *******************************************************************************/
package com.ibm.ws.microprofile.metrics.cdi.decorator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Annotated;

public class AnnotatedDecorator implements Annotated {

    private final Annotated decorated;

    private final Set<Annotation> annotations;

    AnnotatedDecorator(Annotated decorated, Set<Annotation> annotations) {
        this.decorated = decorated;
        this.annotations = annotations;
    }

    @Override
    public Type getBaseType() {
        return decorated.getBaseType();
    }

    @Override
    public Set<Type> getTypeClosure() {
        return decorated.getTypeClosure();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        T annotation = getDecoratingAnnotation(annotationType);
        if (annotation != null)
            return annotation;
        else
            return decorated.getAnnotation(annotationType);
    }

    @Override
    public Set<Annotation> getAnnotations() {
        Set<Annotation> annotations = new HashSet<>(this.annotations);
        annotations.addAll(decorated.getAnnotations());
        return Collections.unmodifiableSet(annotations);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        return getDecoratingAnnotation(annotationType) != null || decorated.isAnnotationPresent(annotationType);
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> T getDecoratingAnnotation(Class<T> annotationType) {
        for (Annotation annotation : annotations)
            if (annotationType.isAssignableFrom(annotation.annotationType()))
                return (T) annotation;

        return null;
    }
}
