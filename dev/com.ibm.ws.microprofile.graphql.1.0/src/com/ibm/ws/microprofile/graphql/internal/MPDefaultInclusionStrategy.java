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
package com.ibm.ws.microprofile.graphql.internal;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import javax.json.bind.annotation.JsonbTransient;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.leangen.graphql.metadata.strategy.InclusionStrategy;
import io.leangen.graphql.util.ClassUtils;
import io.leangen.graphql.util.Utils;

import org.eclipse.microprofile.graphql.Ignore;

public class MPDefaultInclusionStrategy implements InclusionStrategy {

    private final String[] basePackages;

    public MPDefaultInclusionStrategy(String... basePackages) {
        this.basePackages = basePackages;
    }

    @Override
    public boolean includeOperation(AnnotatedElement element, AnnotatedType type) {
        return !ClassUtils.hasAnnotation(element, Ignore.class) 
                && !ClassUtils.hasAnnotation(element, JsonbTransient.class)
                && !fieldHasAnnotation(element, Ignore.class)
                && !fieldHasAnnotation(element, JsonbTransient.class);
                //&& !getterHasAnnotation(element, JsonbTransient.class);
    }

    @Override
    public boolean includeArgument(Parameter parameter, AnnotatedType type) {
        return !ClassUtils.hasAnnotation(parameter, Ignore.class);
    }

    @Override
    public boolean includeInputField(Class<?> declaringClass, AnnotatedElement element, AnnotatedType elementType) {
        return !ClassUtils.hasAnnotation(element, Ignore.class)
                && !ClassUtils.hasAnnotation(element, JsonbTransient.class)
                && !fieldHasAnnotation(element, Ignore.class)
                && !fieldHasAnnotation(element, JsonbTransient.class)
                //&& !setterHasAnnotation(declaringClass, element, JsonbTransient.class)
                && (Utils.isArrayEmpty(basePackages)
                || Arrays.stream(basePackages).anyMatch(pkg -> ClassUtils.isSubPackage(declaringClass.getPackage(), pkg)));
    }

    @FFDCIgnore(NoSuchMethodException.class)
    private static boolean getterHasAnnotation(AnnotatedElement fieldElement, Class<? extends Annotation> annotation) {
        if (fieldElement instanceof Field) {
            Field field = (Field) fieldElement;
            Class<?> declaringClass = field.getDeclaringClass();
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            String getterName = Boolean.class.isAssignableFrom(fieldType) ? "is" : "get" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
            try {
                Method getter = declaringClass.getMethod(getterName, fieldType);
                return ClassUtils.hasAnnotation(getter, annotation);
            } catch (NoSuchMethodException nsme) {
                return false;
            }
            
        }
        return false;
    }

    @FFDCIgnore(NoSuchMethodException.class)
    private static boolean setterHasAnnotation(Class<?> declaringClass, AnnotatedElement fieldElement, 
                                               Class<? extends Annotation> annotation) {
        if (fieldElement instanceof Field) {
            Field field = (Field) fieldElement;
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            String setterName = "set" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
            try {
                Method setter = declaringClass.getMethod(setterName, fieldType);
                return ClassUtils.hasAnnotation(setter, annotation);
            } catch (NoSuchMethodException nsme) {
                return false;
            }
            
        }
        return false;
    }

    private static boolean fieldHasAnnotation(AnnotatedElement methodElement, Class<? extends Annotation> annotation) {
        if (methodElement instanceof Method) {
            Method m = (Method) methodElement;
            String fieldName = MethodUtils.getPropertyName(m);
            if (fieldName != null) {
                try {
                    Class<?> cls = m.getDeclaringClass();
                    Field f = cls.getDeclaredField(fieldName);
                    return ClassUtils.hasAnnotation(f, annotation);
                } catch (NoSuchFieldException nsfe) {
                    return false;
                }
            }
        }
        return false;
    }

    
}
