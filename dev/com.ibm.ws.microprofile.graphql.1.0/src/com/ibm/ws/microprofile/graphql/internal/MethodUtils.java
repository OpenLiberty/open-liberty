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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.leangen.graphql.metadata.TypedElement;

public class MethodUtils {

    public static String getPropertyName(Method method) {
        String methodName = method.getName();
        if (methodName.length() > 3 && (methodName.startsWith("set") || methodName.startsWith("get"))) {
            return methodName.substring(3,4).toLowerCase() + methodName.substring(4);
        }
        Class<?> returnType = method.getReturnType();
        if (boolean.class.equals(returnType) || Boolean.class.equals(returnType) && methodName.length() > 2 
                        && methodName.startsWith("is")) {
            return methodName.substring(2,3).toLowerCase() + methodName.substring(3);
        }
        return null;
    }

    @FFDCIgnore(NoSuchMethodException.class)
    public static Method getGetter(Class<?> cls, String propertyName, Class<?> fieldType) {
        String methodName;
        if (fieldType == Boolean.class || fieldType == boolean.class) {
            methodName = "is" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        } else {
            methodName = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        }
        try {
            return cls.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
    
    public static Method getSetter(Class<?> cls, String propertyName, Class<?> fieldType) {
        String methodName = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        try {
            return cls.getDeclaredMethod(methodName, fieldType);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
    /**
     * A TypedElement can represent a single member of a class or multiple. Assuming that the input is
     * a single class member, and can be a property as defined by JavaBeans (i.e. field x, getX, setX),
     * this method will add the additional method(s) and field necessary to turn it into a property.
     * This can be useful for determining if an annotation exists on a given property, either on the
     * getter, the setter, or the field itself.
     * 
     * If the input typedElement is null, this method will throw a NullPointerException.
     * If the input typedElement already has multiple elements, this method will throw an IllegalStateException.
     * If the input typedElement does not represent a property, this method will return the typedElement unaltered.
     * If the input typedElement contains a getter for a property, this method will add the setter and field (if they exist) to the returned (new) typedElement.
     * If the input typedElement contains a setter for a property, this method will add the getter and field (if they exist) to the returned (new) typedElement.
     * If the input typedElement contains a field for a property, this method will add the getter and setter (if they exist) to the returned (new) typedElement.
     */
    public static TypedElement propertyize(TypedElement typedElement) throws NullPointerException, IllegalStateException {
        Objects.requireNonNull(typedElement);
        AnnotatedElement annotatedElement = typedElement.getElement();
        List<AnnotatedElement> propertyElements = new ArrayList<>();
        propertyElements.add(annotatedElement);
        if (annotatedElement instanceof Field) {
            String propertyName = ((Field) annotatedElement).getName();
            Class<?> cls = ((Field) annotatedElement).getDeclaringClass();
            Class<?> fieldType = ((Field) annotatedElement).getType();
            Method getter = getGetter(cls, propertyName, fieldType);
            Method setter = getSetter(cls, propertyName, fieldType);
            if (getter != null) {
                propertyElements.add(getter);
            }
            if (setter != null) {
                propertyElements.add(setter);
            }
            return new TypedElement(typedElement.getJavaType(), propertyElements);
        }
        if (annotatedElement instanceof Method) {
            Method m = (Method) annotatedElement;
            String propertyName = getPropertyName(m);
            if (propertyName == null) {
                return typedElement;
            }
            Class<?> cls = m.getDeclaringClass();
            Class<?> fieldType = m.getReturnType();
            if (m.getName().startsWith("set")) {
                Method getter = getGetter(cls, propertyName, fieldType);
                if (getter != null) {
                    propertyElements.add(getter);
                }
            } else {
                Method setter = getSetter(cls, propertyName, fieldType);
                if (setter != null) {
                    propertyElements.add(setter);
                }
            }
            try {
                Field field = cls.getDeclaredField(propertyName);
                propertyElements.add(field);
            } catch (NoSuchFieldException ex) {
                // expected in cases where there is no field for a given property
            }
            return new TypedElement(typedElement.getJavaType(), propertyElements);
        }
        return typedElement;
    }
}
