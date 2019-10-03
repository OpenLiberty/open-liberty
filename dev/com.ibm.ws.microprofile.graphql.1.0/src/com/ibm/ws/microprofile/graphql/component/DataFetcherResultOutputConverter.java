/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.graphql.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

import javax.json.bind.annotation.JsonbDateFormat;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.graphql.internal.MethodUtils;

import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.generator.mapping.OutputConverter;
import io.leangen.graphql.metadata.Operation;
import io.leangen.graphql.metadata.TypedElement;
import io.leangen.graphql.util.Directives;

/**
 * OutputConverter to ensure that DataFetcherResult can be returned instead of the
 * expected return type object.
 */
public class DataFetcherResultOutputConverter implements OutputConverter<Object, Object> {
    private final static TraceComponent tc = Tr.register(DataFetcherResultOutputConverter.class);

    Map<AnnotatedType, BiFunction<Object, AnnotatedType, Object>> resultProcessingMap = new WeakHashMap<>();

    @Override
    public Object convertOutput(Object original, AnnotatedType type, ResolutionEnvironment env) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "convertOutput: " + original + " type: " + (type==null?"null":type.getType()));
        }
        
        return resultProcessingMap.computeIfAbsent(type, k -> { return determineResultProcessingFunction(k, env);})
                                  .apply(original, type);
    }

    @Override
    public boolean supports(AnnotatedType type) {
        return true;
    }

    private static BiFunction<Object, AnnotatedType, Object> determineResultProcessingFunction(AnnotatedType annoType, ResolutionEnvironment env) {
        Type type = annoType.getType();
        Operation op = Directives.getMappedOperation(env.dataFetchingEnvironment.getFieldDefinition()).orElse(null);
        TypedElement fieldOrMethod = op.getTypedElement();

        JsonbDateFormat dateFormatAnno = (JsonbDateFormat) findAnnotationOnMemberOrParent(fieldOrMethod, JsonbDateFormat.class);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "determineResultProcessingFunction type: " + (type==null?"null":type) + " @JsonbDateFormat: " + (dateFormatAnno==null?"null":dateFormatAnno.value()));
        }
        if (dateFormatAnno != null && type instanceof Class && TemporalAccessor.class.isAssignableFrom(((Class<?>)type))) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormatAnno.value(), Locale.forLanguageTag(dateFormatAnno.locale()));
            return new DateFormatBiFunction(dtf);
        }
        return DIRECT_RESULT_FUNC;
    }

    private static Annotation findAnnotationOnMemberOrParent(TypedElement typedElement, Class<? extends Annotation> annotationClass) {
        List<? extends AnnotatedElement> elements = typedElement.getElements();
        if (elements == null) {
            return null;
        }
        
        if (elements.size() == 1) {
            elements = MethodUtils.propertyize(typedElement).getElements();
        }

        Annotation anno = null;
        for (AnnotatedElement element : elements) {
            anno = element.getAnnotation(annotationClass);
            if (anno != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Annotation found on member element " + element);
                }
                return anno;
            }
            if (element instanceof Member) {
                Class<?> declaringClass = ((Member)element).getDeclaringClass();
                anno = declaringClass.getAnnotation(annotationClass);
                if (anno != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Annotation found on class " + declaringClass);
                    }
                    return anno;
                }
                Package pkg = declaringClass.getPackage();
                anno = pkg.getAnnotation(annotationClass);
                if (anno != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Annotation found on package " + pkg);
                    }
                    return anno;
                }
            }
        }
        return null;
    }
    private final static BiFunction<Object, AnnotatedType, Object> DIRECT_RESULT_FUNC = 
                    new BiFunction<Object, AnnotatedType, Object>() {

        @Override
        public Object apply(Object o, AnnotatedType type) {
            return o;
        }
    };

    private static class DateFormatBiFunction implements BiFunction<Object, AnnotatedType, Object> {
        DateTimeFormatter dtf;
        DateFormatBiFunction(DateTimeFormatter dtf) {
            this.dtf = dtf;
        }
        
        @Override
        public Object apply(Object o, AnnotatedType annoType) {
            if (o == null) {
                return null;
            }
            if (o instanceof TemporalAccessor) {
                return dtf.format((TemporalAccessor) o);
            }
            throw new IllegalArgumentException("Cannot convert data fetcher type of " + o.getClass() + " to a TemporalAccessor");
        }
    }
}
