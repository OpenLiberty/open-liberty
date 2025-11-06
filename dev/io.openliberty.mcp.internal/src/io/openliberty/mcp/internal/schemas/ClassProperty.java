/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.schemas;

import static java.lang.reflect.Modifier.isStatic;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.openliberty.mcp.annotations.Tool.Annotations;

/**
 * A single property extracted from a non-record class
 *
 * @see JsonProperty
 */
public class ClassProperty implements JsonProperty {
    private static final Annotations[] EMPTY_ANNOTATIONS = new Annotations[0];

    /** The property name, extracted from the Java element names */
    private String elementName;
    private Field field;
    private Method setter;
    private Method getGetter;
    private Method isGetter;

    public ClassProperty(String name) {
        this.elementName = name;
    }

    /** {@inheritDoc} */
    @Override
    public String getElementName() {
        return elementName;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInput() {
        if (setter != null) {
            if (JsonProperty.isTransient(setter)) {
                return false;
            }
        }

        if (field != null) {
            if (JsonProperty.isTransient(field)) {
                return false;
            }
        }

        if (setter != null) {
            return Modifier.isPublic(setter.getModifiers());
        }

        if (field != null) {
            return Modifier.isPublic(field.getModifiers());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOutput() {
        Method getter = getGetter();

        if (getter != null) {
            if (JsonProperty.isTransient(getter)) {
                return false;
            }
        }

        if (field != null) {
            if (JsonProperty.isTransient(field)) {
                return false;
            }
        }

        if (getter != null) {
            return Modifier.isPublic(getter.getModifiers());
        }

        if (field != null) {
            return Modifier.isPublic(field.getModifiers());
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getInputName() {
        String name = JsonProperty.getNameFromAnnotations(setter);

        if (name == null) {
            name = JsonProperty.getNameFromAnnotations(field);
        }

        if (name == null) {
            name = elementName;
        }

        return name;
    }

    /** {@inheritDoc} */
    @Override
    public String getOutputName() {
        String name = JsonProperty.getNameFromAnnotations(getGetter());

        if (name == null) {
            name = JsonProperty.getNameFromAnnotations(field);
        }

        if (name == null) {
            name = elementName;
        }

        return name;
    }

    /** {@inheritDoc} */
    @Override
    public Type getInputType() {
        if (setter != null) {
            return setter.getGenericParameterTypes()[0];
        }

        if (field != null) {
            return field.getGenericType();
        }

        return Object.class;
    }

    /** {@inheritDoc} */
    @Override
    public Type getOutputType() {
        Method getter = getGetter();
        if (getter != null) {
            return getter.getGenericReturnType();
        }

        if (field != null) {
            return field.getGenericType();
        }

        return Object.class;
    }

    /** {@inheritDoc} */
    @Override
    public Annotation[] getInputAnnotations() {
        if (setter != null) {
            if (field != null) {
                return combineAnnotations(setter, field);
            } else {
                return setter.getAnnotations();
            }
        } else {
            if (field != null) {
                return field.getAnnotations();
            } else {
                return EMPTY_ANNOTATIONS;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Annotation[] getOutputAnnotations() {
        Method getter = getGetter();
        if (getter != null) {
            if (field != null) {
                return combineAnnotations(getter, field);
            } else {
                return getter.getAnnotations();
            }
        } else {
            if (field != null) {
                return field.getAnnotations();
            } else {
                return EMPTY_ANNOTATIONS;
            }
        }
    }

    public Field getField() {
        return field;
    }

    public void addField(Field field) {
        if (this.field == null) {
            this.field = field;
        }
    }

    public Method getSetter() {
        return setter;
    }

    public void addSetter(Method setter) {
        if (this.setter == null) {
            this.setter = setter;
        }
    }

    public Method getGetter() {
        // getXxxxx is used in preference to isXxxxx
        return this.getGetter != null ? this.getGetter : this.isGetter;
    }

    public void addGetGetter(Method getGetter) {
        if (this.getGetter == null) {
            this.getGetter = getGetter;
        }
    }

    public void addIsGetter(Method isGetter) {
        if (this.isGetter == null) {
            this.isGetter = isGetter;
        }
    }

    public static List<JsonProperty> extractFromClass(Class<?> cls) {
        Map<String, ClassProperty> properties = new HashMap<>();

        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (isStatic(field.getModifiers())) {
                    continue;
                }
                properties.computeIfAbsent(field.getName(), n -> new ClassProperty(n))
                          .addField(field);
            }

            for (Method method : current.getDeclaredMethods()) {
                if (isStatic(method.getModifiers())) {
                    continue;
                }
                processMethod(method, properties);
            }
            current = current.getSuperclass();
        }

        current = cls;
        while (current != null && current != Object.class) {
            for (Class<?> iface : current.getInterfaces()) {
                processInterface(iface, properties);
            }
            current = current.getSuperclass();
        }

        return new ArrayList<>(properties.values());
    }

    private static void processMethod(Method method, Map<String, ClassProperty> properties) {
        if (isGetGetter(method)) {
            String name = removeCamelCasePrefix(method.getName(), 3);
            properties.computeIfAbsent(name, ClassProperty::new)
                      .addGetGetter(method);
        } else if (isIsGetter(method)) {
            String name = removeCamelCasePrefix(method.getName(), 2);
            properties.computeIfAbsent(name, ClassProperty::new)
                      .addIsGetter(method);
        } else if (isSetter(method)) {
            String name = removeCamelCasePrefix(method.getName(), 3);
            properties.computeIfAbsent(name, ClassProperty::new)
                      .addSetter(method);
        }
    }

    private static void processInterface(Class<?> iface, Map<String, ClassProperty> properties) {
        for (Method method : iface.getDeclaredMethods()) {
            if (!method.isDefault()) {
                continue;
            }
            processMethod(method, properties);
        }
        for (Class<?> subIface : iface.getInterfaces()) {
            processInterface(subIface, properties);
        }
    }

    private static boolean isSetter(Method method) {
        String name = method.getName();
        return method.getReturnType() == void.class
               && name.startsWith("set")
               && name.length() > 3
               && method.getParameterCount() == 1
               && !Modifier.isStatic(method.getModifiers());
    }

    private static boolean isGetGetter(Method method) {
        String name = method.getName();
        return (name.startsWith("get") && name.length() > 3)
               && method.getReturnType() != void.class
               && method.getParameterCount() == 0
               && !Modifier.isStatic(method.getModifiers());
    }

    private static boolean isIsGetter(Method method) {
        String name = method.getName();
        return (name.startsWith("is") && name.length() > 2)
               && method.getReturnType() != void.class
               && method.getParameterCount() == 0
               && !Modifier.isStatic(method.getModifiers());
    }

    /**
     * Remove a number of characters from a string and then lower-case the first remaining character.
     * <p>
     * For example:
     * {@code removeCamelCasePrefix("getWidget", 3)} returns {@code "widget"}
     *
     * @param string the string to operate on
     * @param prefixLength the length of the prefix to remove
     * @return the camelcase string with the prefix removed
     */
    private static String removeCamelCasePrefix(String string, int prefixLength) {
        // Lower-case the first letter
        char firstChar = Character.toLowerCase(string.charAt(prefixLength));
        StringBuilder sb = new StringBuilder(string.length() - prefixLength);
        sb.append(firstChar).append(string, prefixLength + 1, string.length());
        return sb.toString();
    }

    /**
     * Combine annotations from two annotated elements, preferring those from {@code element1}
     *
     * @param element1 the first annotated element
     * @param element2 the second annotated element
     * @return the combined annotations
     */
    private static Annotation[] combineAnnotations(AnnotatedElement element1, AnnotatedElement element2) {
        Set<Class<?>> typesSeen = new HashSet<>();
        List<Annotation> result = new ArrayList<>();
        for (Annotation a : element1.getAnnotations()) {
            typesSeen.add(a.annotationType());
            result.add(a);
        }

        for (Annotation a : element2.getAnnotations()) {
            if (!typesSeen.contains(a.annotationType())) {
                result.add(a);
            }
        }

        return result.toArray(Annotation[]::new);
    }

}