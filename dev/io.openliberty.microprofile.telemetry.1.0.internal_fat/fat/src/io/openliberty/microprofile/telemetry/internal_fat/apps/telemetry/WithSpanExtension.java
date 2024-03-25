/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry;

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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;

public class WithSpanExtension implements Extension {

    /**
     * Add a WithSpan annotation to {@code WithSpanServlet.SpanBean.methodAnnotatedViaExtension}
     * <p>
     * Note this is <b>way</b> easier in CDI 2.0, but this test needs to be compatible with CDI 1.2, so we need to create our own subclasses of AnnotatedType and AnnotatedMethod.
     *
     * @param pat the event
     */
    public void addWithSpanAnnotation(@Observes ProcessAnnotatedType<WithSpanServlet.SpanBean> pat) {
        UpdatedAnnotatedType<WithSpanServlet.SpanBean> aType = new UpdatedAnnotatedType<WithSpanServlet.SpanBean>(pat.getAnnotatedType());
        HashSet<AnnotatedMethod<? super WithSpanServlet.SpanBean>> methods = new HashSet<>(aType.getMethods());

        AnnotatedMethod<? super WithSpanServlet.SpanBean> method = methods.stream()
                        .filter(m -> m.getJavaMember().getName().equals("methodAnnotatedViaExtension"))
                        .findAny()
                        .orElseThrow(() -> new IllegalStateException("Could not find method methodAnnotatedViaExtension"));

        UpdatedAnnotatedMethod<? super WithSpanServlet.SpanBean> newMethod = UpdatedAnnotatedMethod.createFrom(method);

        Set<Annotation> annotations = new HashSet<>(newMethod.getAnnotations());
        annotations.add(literalWithSpan("nameFromExtension", SpanKind.PRODUCER));
        newMethod.setAnnotations(annotations);

        methods.remove(method);
        methods.add(newMethod);

        aType.setMethods(methods);
        pat.setAnnotatedType(aType);
    }

    private static class UpdatedAnnotatedType<X> implements AnnotatedType<X> {

        private final AnnotatedType<X> originalType;
        private Set<AnnotatedMethod<? super X>> methods;

        public UpdatedAnnotatedType(AnnotatedType<X> originalType) {
            this.originalType = originalType;
        }

        public void setMethods(Set<AnnotatedMethod<? super X>> methods) {
            this.methods = methods;
        }

        @Override
        public Set<AnnotatedMethod<? super X>> getMethods() {
            if (methods != null) {
                return methods;
            } else {
                return originalType.getMethods();
            }
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> arg0) {
            return originalType.getAnnotation(arg0);
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return originalType.getAnnotations();
        }

        @Override
        public Type getBaseType() {
            return originalType.getBaseType();
        }

        @Override
        public Set<AnnotatedConstructor<X>> getConstructors() {
            return originalType.getConstructors();
        }

        @Override
        public Set<AnnotatedField<? super X>> getFields() {
            return originalType.getFields();
        }

        @Override
        public Class<X> getJavaClass() {
            return originalType.getJavaClass();
        }

        @Override
        public Set<Type> getTypeClosure() {
            return originalType.getTypeClosure();
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> arg0) {
            return originalType.isAnnotationPresent(arg0);
        }
    }

    private static class UpdatedAnnotatedMethod<X> implements AnnotatedMethod<X> {
        private final AnnotatedMethod<X> originalMethod;
        private Set<Annotation> annotations;

        public UpdatedAnnotatedMethod(AnnotatedMethod<X> originalMethod) {
            this.originalMethod = originalMethod;
        }

        public static <Y> UpdatedAnnotatedMethod<Y> createFrom(AnnotatedMethod<Y> originalMethod) {
            return new UpdatedAnnotatedMethod<Y>(originalMethod);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends Annotation> T getAnnotation(Class<T> type) {
            if (annotations == null) {
                return originalMethod.getAnnotation(type);
            }
            return annotations.stream()
                            .filter(a -> a.annotationType().equals(type))
                            .map(a -> type.cast(a))
                            .findFirst()
                            .orElse(null);
        }

        public void setAnnotations(Set<Annotation> annotations) {
            this.annotations = annotations;
        }

        @Override
        public Set<Annotation> getAnnotations() {
            if (annotations == null) {
                return originalMethod.getAnnotations();
            } else {
                return annotations;
            }
        }

        @Override
        public Type getBaseType() {
            return originalMethod.getBaseType();
        }

        @Override
        public AnnotatedType<X> getDeclaringType() {
            return originalMethod.getDeclaringType();
        }

        @Override
        public Method getJavaMember() {
            return originalMethod.getJavaMember();
        }

        @Override
        public List<AnnotatedParameter<X>> getParameters() {
            return originalMethod.getParameters();
        }

        @Override
        public Set<Type> getTypeClosure() {
            return originalMethod.getTypeClosure();
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> arg0) {
            return originalMethod.isAnnotationPresent(arg0);
        }

        @Override
        public boolean isStatic() {
            return originalMethod.isStatic();
        }
    }

    @SuppressWarnings("serial")
    private static abstract class WithSpanLiteral extends AnnotationLiteral<WithSpan> implements WithSpan {
    }

    @SuppressWarnings("serial")
    private static WithSpan literalWithSpan(String name, SpanKind kind) {
        return new WithSpanLiteral() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return WithSpan.class;
            }

            @Override
            public String value() {
                return name;
            }

            @Override
            public SpanKind kind() {
                return kind;
            }
        };
    }

}
