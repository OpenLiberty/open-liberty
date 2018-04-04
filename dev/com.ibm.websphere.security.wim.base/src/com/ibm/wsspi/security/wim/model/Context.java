/*******************************************************************************
 * Copyright (c) 2017,2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.wim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for Context complex type.
 *
 * <p> The Context object is used for storing contextual information.
 *
 * <p> A client may communicate contextual information to VMM. For example, when creating a Person,
 * the client application may need to specify the realm to create the Person under. The realm itself is not a
 * part of the Person object's data. Another example is VMM may need the client application to pass in an IP
 * address.
 *
 * <p> The Context object is used for storing this contextual information. The Context object supports
 * arbitrary key -&gt; object mappings in order to support attributes that are not already pre-defined in the VMM schema
 *
 * <p> The Context object consists of key-value pair mappings of contextual information.
 *
 * <ul>
 * <li><b>key</b>: specifies the name of a contextual property. The following are two examples
 * of possible keys: realm, ipAddress</li>
 * <li><b>value</b>: specifies the value of a contextual property. Since value can be a String or
 * any arbitrary object, the type of the property value is <b>anySimpleType</b>. This property can
 * either be a single value or multi-valued.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Context.TYPE_NAME, propOrder = {
                                                 "key",
                                                 "value"
})
public class Context {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "Context";

    /** Property name constant for the <b>key</b> property. */
    private static final String PROP_KEY = "key";

    /** Property name constant for the <b>value</b> property. */
    private static final String PROP_VALUE = "value";

    /**
     * The name of a contextual property. The following are two examples
     * of possible keys: realm, ipAddress
     */
    @XmlElement(name = PROP_KEY, required = true)
    protected String key;

    /**
     * The value of a contextual property. Since value can be a String or
     * any arbitrary object, the type of the property value is <b>anySimpleType</b>. This property can
     * either be a single value or multi-valued.
     */
    @XmlElement(name = PROP_VALUE, required = true)
    @XmlSchemaType(name = "anySimpleType")
    protected Object value;

    /** The list of properties that comprise this type. */
    private static List<String> propertyNames = null;

    /** A mapping of property names to data types. */
    private static HashMap<String, String> dataTypeMap = null;

    /** A list of super-types of this type. */
    private static ArrayList<String> superTypeList = null;

    /** A set of sub-types of this type. */
    private static HashSet<String> subTypeSet = null;

    static {
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();
    }

    /**
     * Gets the value of the <b>key</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the value of the <b>key</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setKey(String value) {
        this.key = value;
    }

    /**
     * Returns true if the <b>key</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetKey() {
        return (this.key != null);
    }

    /**
     * Gets the value of the <b>value</b> property.
     *
     * @return
     *         possible object is {@link Object }
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value of the <b>value</b> property.
     *
     * @param value
     *            allowed object is {@link Object }
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Returns true if the <b>value</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetValue() {
        return (this.value != null);
    }

    /**
     * Gets the value of the requested property
     *
     * @param propName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link Object}
     */
    public Object get(String propName) {
        if (propName.equals(PROP_KEY)) {
            return getKey();
        }
        if (propName.equals(PROP_VALUE)) {
            return getValue();
        }
        return null;
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @param propName
     *            The name of the property to check if is set.
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSet(String propName) {
        if (propName.equals(PROP_KEY)) {
            return isSetKey();
        }
        if (propName.equals(PROP_VALUE)) {
            return isSetValue();
        }
        return false;
    }

    /**
     * Sets the value of the provided property to the provided value.
     *
     * @param propName
     *            allowed object is {@link String}
     * @param value
     *            allowed object is {@link Object}
     */
    public void set(String propName, Object value) {
        if (propName.equals(PROP_KEY)) {
            setKey(((String) value));
        }
        if (propName.equals(PROP_VALUE)) {
            setValue(value);
        }
    }

    /**
     * Sets the value of provided property to null.
     *
     * @param propName
     *            allowed object is {@link String}
     */
    public void unset(String propName) {}

    /**
     * Gets the name of this type.
     *
     * @return
     *         returned object is {@link String}
     */
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Gets a list of all supported properties for this type.
     *
     * @param entityTypeName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link List}
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            List<String> names = new ArrayList<String>();
            names.add(PROP_KEY);
            names.add(PROP_VALUE);
            propertyNames = Collections.unmodifiableList(names);
            return propertyNames;
        }
    }

    /**
     * Create the property name to data type mapping.
     */
    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap<String, String>();
        }
        dataTypeMap.put(PROP_KEY, "String");
        dataTypeMap.put(PROP_VALUE, "Object");
    }

    /**
     * Gets the Java type of the value of the provided property. For example: String
     *
     * @param propName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link String}
     */
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return (dataTypeMap.get(propName));
        } else {
            return null;
        }
    }

    /**
     * Create the list of super-types for this entity type.
     */
    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList<String>();
        }
    }

    /**
     * Gets a list of any types which this type is an extension of.
     *
     * @return
     *         returned object is {@link ArrayList}
     */
    public ArrayList<String> getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    /**
     * Returns a true if the provided type is one that this type extends; false, otherwise.
     *
     * @param superTypeName
     *
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    /**
     * Create the list of sub-types for this type.
     */
    private static synchronized void setSubTypes() {
        if (subTypeSet == null) {
            subTypeSet = new HashSet<String>();
        }
    }

    /**
     * Gets a set of any types which extend this type.
     *
     * @return
     *         returned object is {@link HashSet}
     */
    public static HashSet<String> getSubTypes() {
        if (subTypeSet == null) {
            setSubTypes();
        }
        return subTypeSet;
    }

    @Override
    public String toString() {
        return WIMTraceHelper.traceJaxb(this);
    }
}
