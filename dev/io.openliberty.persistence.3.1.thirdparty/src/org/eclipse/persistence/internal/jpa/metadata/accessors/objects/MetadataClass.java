/*
 * Copyright (c) 1998, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

// Contributors:
//     Oracle - initial API and implementation from Oracle TopLink
//     05/16/2008-1.0M8 Guy Pelletier
//       - 218084: Implement metadata merging functionality between mapping files
//     03/08/2010-2.1 Guy Pelletier
//       - 303632: Add attribute-type for mapping attributes to EclipseLink-ORM
//     05/04/2010-2.1 Guy Pelletier
//       - 309373: Add parent class attribute to EclipseLink-ORM
//     05/14/2010-2.1 Guy Pelletier
//       - 253083: Add support for dynamic persistence using ORM.xml/eclipselink-orm.xml
//     01/25/2011-2.3 Guy Pelletier
//       - 333488: Serializable attribute being defaulted to a variable one to one mapping and causing exception
//     07/16/2013-2.5.1 Guy Pelletier
//       - 412384: Applying Converter for parameterized basic-type for joda-time's DateTime does not work
//     09/02/2019-3.0 Alexandre Jacob
//        - 527415: Fix code when locale is tr, az or lt
package org.eclipse.persistence.internal.jpa.metadata.accessors.objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.persistence.asm.Opcodes;
import org.eclipse.persistence.internal.helper.Helper;

/**
 * INTERNAL:
 * An object to hold onto a valid JPA decorated class.
 *
 * @author Guy Pelletier
 * @since TopLink 10.1.3/EJB 3.0 Preview
 */
public class MetadataClass extends MetadataAnnotatedElement {
    protected boolean m_isLazy;
    protected boolean m_isAccessible;
    protected boolean m_isPrimitive;
    protected boolean m_isJDK;
    protected int m_modifiers;

    // Stores the implements interfaces of this class.
    protected List<String> m_interfaces;

    // Stores a list of enclosed classes found inside this metadata class.
    // E.g. inner classes, enums etc.
    protected List<MetadataClass> m_enclosedClasses;

    // Store the classes field metadata, keyed by the field's name.
    protected Map<String, MetadataField> m_fields;

    // Store the classes method metadata, keyed by the method's name.
    // Method's next is used if multiple method with the same name.
    protected Map<String, MetadataMethod> m_methods;

    protected MetadataClass m_superclass;
    protected String m_superclassName;

    /**
     * Create the metadata class with the class name.
     */
    public MetadataClass(MetadataFactory factory, String name, boolean isLazy) {
        super(factory);
        setName(name);

        // By default, set the type to be the same as the name. The canonical
        // model generator relies on types which in most cases is the name, but
        // the generator resolves generic types a little differently to
        // correctly generate model classes.
        setType(name);

        m_isAccessible = true;
        m_isLazy = isLazy;
    }

    /**
     * Create the metadata class with the class name.
     */
    public MetadataClass(MetadataFactory factory, String name) {
        this(factory, name, false);
    }

    /**
     * Create the metadata class based on the class.
     * Mainly used for primitive defaults.
     */
    public MetadataClass(MetadataFactory factory, Class<?> cls) {
        this(factory, cls.getName(), false);
        m_isPrimitive = cls.isPrimitive();
    }

    /**
     * INTERNAL:
     */
    public void addEnclosedClass(MetadataClass enclosedClass) {
        if (m_enclosedClasses == null) {
            m_enclosedClasses = new ArrayList<>();
        }

        m_enclosedClasses.add(enclosedClass);
    }

    /**
     * INTERNAL:
     */
    public void addField(MetadataField field) {
        if (m_fields == null) {
            m_fields = new HashMap<>();
        }

        m_fields.put(field.getName(), field);
    }

    /**
     * INTERNAL:
     */
    public void addInterface(String interfaceName) {
        if (m_interfaces == null) {
            m_interfaces = new ArrayList<>();
        }

        m_interfaces.add(interfaceName);
    }

    /**
     * INTERNAL:
     */
    public void addMethod(MetadataMethod method) {
        if (m_methods == null) {
            m_methods = new HashMap<>();
        }

        m_methods.put(method.getName(), method);
    }

    /**
     * Allow comparison to Java classes and Metadata classes.
     */
    public boolean isClass(Class<?> clazz) {
        if (getName() == null) {
            return false;
        }
        return getName().equals(clazz.getName());
    }

    /**
     * INTERNAL:
     * Return if this class is or extends, or super class extends the class.
     */
    public boolean extendsClass(Class<?> javaClass) {
        return extendsClass(javaClass.getName());
    }

    /**
     * INTERNAL:
     * Return if this class is or extends, or super class extends the class.
     */
    public boolean extendsClass(String className) {
        if (getName() == null) {
            return className == null;
        }

        if (getName().equals(className)) {
            return true;
        }

        if (getSuperclassName() == null) {
            return false;
        }

        if (getSuperclassName().equals(className)) {
            return true;
        }

        return getSuperclass().extendsClass(className);
    }

    /**
     * INTERNAL:
     * Return if this class is or extends, or super class extends the interface.
     */
    public boolean extendsInterface(Class<?> javaClass) {
        return extendsInterface(javaClass.getName());
    }

    /**
     * INTERNAL:
     * Return if this class is or extends, or super class extends the interface.
     */
    public boolean extendsInterface(String className) {
        if (getName() == null) {
            return false;
        }

        if (getName().equals(className)) {
            return true;
        }

        if (getInterfaces().contains(className)) {
            return true;
        }

        for (String interfaceName : getInterfaces()) {
            if (getMetadataClass(interfaceName).extendsInterface(className)) {
                return true;
            }
        }

        if (getSuperclassName() == null) {
            return false;
        }

        return getSuperclass().extendsInterface(className);
    }

    /**
     * INTERNAL:
     * Return the list of classes defined within this metadata class. E.g.
     * enums and inner classes.
     */
    public List<MetadataClass> getEnclosedClasses() {
        if (m_enclosedClasses == null) {
            m_enclosedClasses = new ArrayList<>();
        }

        return m_enclosedClasses;
    }

    /**
     * INTERNAL:
     * Return the field with the name.
     * Search for any declared or inherited field.
     */
    public MetadataField getField(String name) {
        return getField(name, true);
    }

    /**
     * INTERNAL:
     * Return the field with the name.
     * Search for any declared or inherited field.
     */
    public MetadataField getField(String name, boolean checkSuperClass) {
        MetadataField field = getFields().get(name);

        if (checkSuperClass && (field == null) && (getSuperclassName() != null)) {
            return getSuperclass().getField(name);
        }

        return field;
    }

    /**
     * INTERNAL:
     */
    public Map<String, MetadataField> getFields() {
        if (m_fields == null) {
            m_fields = new HashMap<>();

            if (m_isLazy) {
                m_factory.getMetadataClass(getName(), false);
            }
        }

        return m_fields;
    }

    /**
     * INTERNAL:
     */
    public List<String> getInterfaces() {
        if (m_interfaces == null) {
            m_interfaces = new ArrayList<>();
        }

        return m_interfaces;
    }

    /**
     * INTERNAL:
     * Return the method with the name and no arguments.
     */
    protected MetadataMethod getMethod(String name) {
        return getMethods().get(name);
    }

    /**
     * INTERNAL:
     * Return the method with the name and argument types.
     */
    public MetadataMethod getMethod(String name, Class<?>[] arguments) {
        List<String> argumentNames = new ArrayList<>(arguments.length);

        for (int index = 0; index < arguments.length; index++) {
            argumentNames.add(arguments[index].getName());
        }

        return getMethod(name, argumentNames);
    }

    /**
     * INTERNAL:
     * Return the method with the name and argument types (class names).
     */
    public MetadataMethod getMethod(String name, List<String> arguments) {
        return getMethod(name, arguments, true);
    }

    /**
     * INTERNAL:
     * Return the method with the name and argument types (class names).
     */
    public MetadataMethod getMethod(String name, List<String> arguments, boolean checkSuperClass) {
        MetadataMethod method = getMethods().get(name);

        while ((method != null) && !method.getParameters().equals(arguments)) {
            method = method.getNext();
        }

        if (checkSuperClass && (method == null) && (getSuperclassName() != null)) {
            return getSuperclass().getMethod(name, arguments);
        }

        return method;
    }

    /**
     * INTERNAL:
     * Return the method with the name and argument types (class names).
     */
    public MetadataMethod getMethod(String name, String[] arguments) {
        return getMethod(name, Arrays.asList(arguments));
    }

    /**
     * INTERNAL:
     * Return the method for the given property name.
     */
    public MetadataMethod getMethodForPropertyName(String propertyName) {
        MetadataMethod method;

        String leadingChar = String.valueOf(propertyName.charAt(0)).toUpperCase(Locale.ROOT);
        String restOfName = propertyName.substring(1);

        // Look for a getPropertyName() method
        method = getMethod(Helper.GET_PROPERTY_METHOD_PREFIX.concat(leadingChar).concat(restOfName), new String[]{});

        if (method == null) {
            // Look for an isPropertyName() method
            method = getMethod(Helper.IS_PROPERTY_METHOD_PREFIX.concat(leadingChar).concat(restOfName), new String[]{});
        }

        if (method != null) {
            method.setSetMethod(method.getSetMethod(this));
        }

        return method;
    }

    /**
     * INTERNAL:
     */
    public Map<String, MetadataMethod> getMethods() {
        if (m_methods == null) {
            m_methods = new HashMap<>();

            if (m_isLazy) {
                m_factory.getMetadataClass(getName(), false);
            }
        }
        return m_methods;
    }

    /**
     * INTERNAL:
     */
    @Override
    public int getModifiers() {
        return m_modifiers;
    }

    /**
     * INTERNAL:
     */
    public MetadataClass getSuperclass() {
        if (m_superclass == null) {
            m_superclass = getMetadataClass(m_superclassName);
        }

        return m_superclass;
    }

    /**
     * INTERNAL:
     */
    public String getSuperclassName() {
        return m_superclassName;
    }

    /**
     * Return the ASM type name.
     */
    public String getTypeName() {
        if (isArray()) {
            return getName().replace('.', '/');
        } else if (isPrimitive()) {
            if (getName().equals("int")) {
                return "I";
            } else if (getName().equals("long")) {
                return "J";
            } else if (getName().equals("short")) {
                return "S";
            } else if (getName().equals("boolean")) {
                return "Z";
            } else if (getName().equals("float")) {
                return "F";
            } else if (getName().equals("double")) {
                return "D";
            } else if (getName().equals("char")) {
                return "C";
            } else if (getName().equals("byte")) {
                return "B";
            }
        }
        return "L" + getName().replace('.', '/') + ";";
    }

    /**
     * INTERNAL:
     * Return true is this class accessible to be found.
     */
    public boolean isAccessible() {
        return m_isAccessible;
    }

    /**
     * INTERNAL:
     * Return if this class is an array type.
     */
    public boolean isArray() {
        return (getName() != null) && (getName().charAt(0) == '[');
    }

    /**
     * INTERNAL:
     * Return if this is extends Collection.
     */
    public boolean isCollection() {
        return extendsInterface(Collection.class);
    }

    /**
     * INTERNAL:
     * Return if this is extends Enum.
     */
    public boolean isEnum() {
        return extendsClass(Enum.class);
    }

    /**
     * INTERNAL:
     * Return if this is an interface (super is null).
     */
    public boolean isInterface() {
        return (Opcodes.ACC_INTERFACE & m_modifiers) != 0;
    }

    /**
     * INTERNAL:
     * Return if this is a JDK (java/javax) class.
     */
    public boolean isJDK() {
        return m_isJDK;
    }

    /**
     * INTERNAL:
     */
    public boolean isLazy() {
        return m_isLazy;
    }

    /**
     * INTERNAL:
     * Return if this is extends List.
     */
    public boolean isList() {
        return extendsInterface(List.class);
    }

    /**
     * INTERNAL:
     * Return if this is extends Map.
     */
    public boolean isMap() {
        return extendsInterface(Map.class);
    }

    /**
     * INTERNAL:
     * Return if this is Object class.
     */
    public boolean isObject() {
        return getName().equals(Object.class.getName());
    }

    /**
     * INTERNAL:
     * Return if this is a primitive.
     */
    public boolean isPrimitive() {
        return m_isPrimitive;
    }

    /**
     * INTERNAL:
     * Return if this class extends Serializable or is an array type.
     */
    public boolean isSerializable() {
        if (isArray()) {
            return true;
        }

        return extendsInterface(Serializable.class);
    }

    /**
     * INTENAL:
     * Return true is this class is the Serializable.class interface.
     */
    public boolean isSerializableInterface() {
        return getName().equals(Serializable.class.getName());
    }

    /**
     * INTERNAL:
     * Return true if this extends Set.
     */
    public boolean isSet() {
        return extendsInterface(Set.class);
    }

    /**
     * INTERNAL:
     * Return if this is the void class.
     */
    public boolean isVoid() {
        return getName().equals(void.class.getName()) || getName().equals(Void.class.getName());
    }

    /**
     * INTERNAL:
     */
    public void setIsAccessible(boolean isAccessible) {
        m_isAccessible = isAccessible;
    }

    /**
     * INTERNAL:
     */
    public void setIsJDK(boolean isJDK) {
        m_isJDK = isJDK;
    }

    /**
     * INTERNAL:
     */
    public void setIsLazy(boolean isLazy) {
        m_isLazy = isLazy;
    }

    /**
     * INTERNAL:
     */
    @Override
    public void setModifiers(int modifiers) {
        m_modifiers = modifiers;
    }

    /**
     * INTERNAL:
     */
    @Override
    public void setName(String name) {
        super.setName(name);

        if ((!MetadataFactory.ALLOW_JDK) && (name.startsWith("java.")
                || name.startsWith("javax.") || name.startsWith("jakarta.")
                || name.startsWith("org.eclipse.persistence.internal."))) {
            setIsJDK(true);
        }
    }

    /**
     * INTERNAL:
     */
    public void setSuperclass(MetadataClass superclass) {
        m_superclass = superclass;
    }

    /**
     * INTERNAL:
     */
    public void setSuperclassName(String superclass) {
        m_superclassName = superclass;
    }
}
