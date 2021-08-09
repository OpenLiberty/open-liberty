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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

public final class AnnotatedTypeDecorator<X> extends AnnotatedDecorator implements AnnotatedType<X> {

    private final AnnotatedType<X> decoratedType;

    private final Set<AnnotatedMethod<? super X>> decoratedMethods;

    public AnnotatedTypeDecorator(AnnotatedType<X> decoratedType, Annotation decoratingAnnotation) {
        this(decoratedType, decoratingAnnotation, Collections.<AnnotatedMethod<? super X>> emptySet());
    }

    public AnnotatedTypeDecorator(AnnotatedType<X> decoratedType, Annotation decoratingAnnotation, Set<AnnotatedMethod<? super X>> decoratedMethods) {
        super(decoratedType, Collections.singleton(decoratingAnnotation));
        this.decoratedType = decoratedType;
        this.decoratedMethods = decoratedMethods;
    }

    @Override
    public Class<X> getJavaClass() {
        return decoratedType.getJavaClass();
    }

    @Override
    public Set<AnnotatedConstructor<X>> getConstructors() {
        return decoratedType.getConstructors();
    }

    @Override
    public Set<AnnotatedMethod<? super X>> getMethods() {
        Set<AnnotatedMethod<? super X>> methods = new HashSet<>(decoratedType.getMethods());
        for (AnnotatedMethod<? super X> method : decoratedMethods) {
            methods.remove(method);
            methods.add(method);
        }

        return Collections.unmodifiableSet(methods);
    }

    @Override
    public Set<AnnotatedField<? super X>> getFields() {
        return decoratedType.getFields();
    }
}
