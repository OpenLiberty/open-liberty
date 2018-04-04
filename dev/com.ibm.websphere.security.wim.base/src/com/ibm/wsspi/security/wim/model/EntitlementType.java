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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for EntitlementType complex type.
 *
 * <p>Below is a list of supported properties for {@link EntitlementType}.
 *
 * <ul>
 * <li><b>method</b>: the method for the entitlement</li>
 * <li><b>object</b>: the object for the entitlement</li>
 * <li><b>attribute</b>: the attribute for the entitlement</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = EntitlementType.TYPE_NAME)
public class EntitlementType {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "EntitlementType";

    /** Property name constant for the <b>method</b> property. */
    private static final String PROP_METHOD = "method";

    /** Property name constant for the <b>object</b> property. */
    private static final String PROP_OBJECT = "object";

    /** Property name constant for the <b>attribute</b> property. */
    private static final String PROP_ATTRIBUTE = "attribute";

    /** The method for the entitlement. */
    @XmlAttribute(name = PROP_METHOD)
    protected String method;

    /** The object for the entitlement. */
    @XmlAttribute(name = PROP_OBJECT)
    protected String object;

    /** The attribute for the entitlement. */
    @XmlAttribute(name = PROP_ATTRIBUTE)
    protected String attribute;

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
     * Gets the value of the <b>method</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the value of the <b>method</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setMethod(String value) {
        this.method = value;
    }

    /**
     * Returns true if the <b>method</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetMethod() {
        return (this.method != null);
    }

    /**
     * Gets the value of the <b>object</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getObject() {
        return object;
    }

    /**
     * Sets the value of the <b>object</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setObject(String value) {
        this.object = value;
    }

    /**
     * Returns true if the <b>object</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetObject() {
        return (this.object != null);
    }

    /**
     * Gets the value of the <b>attribute</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getAttribute() {
        return attribute;
    }

    /**
     * Sets the value of the <b>attribute</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setAttribute(String value) {
        this.attribute = value;
    }

    /**
     * Returns true if the <b>attribute</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetAttribute() {
        return (this.attribute != null);
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
        if (propName.equals(PROP_METHOD)) {
            return getMethod();
        }
        if (propName.equals(PROP_OBJECT)) {
            return getObject();
        }
        if (propName.equals(PROP_ATTRIBUTE)) {
            return getAttribute();
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
        if (propName.equals(PROP_METHOD)) {
            return isSetMethod();
        }
        if (propName.equals(PROP_OBJECT)) {
            return isSetObject();
        }
        if (propName.equals(PROP_ATTRIBUTE)) {
            return isSetAttribute();
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
        if (propName.equals(PROP_METHOD)) {
            setMethod(((String) value));
        }
        if (propName.equals(PROP_OBJECT)) {
            setObject(((String) value));
        }
        if (propName.equals(PROP_ATTRIBUTE)) {
            setAttribute(((String) value));
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
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.add(PROP_METHOD);
            names.add(PROP_OBJECT);
            names.add(PROP_ATTRIBUTE);
            propertyNames = Collections.unmodifiableList(names);
        }
        return propertyNames;
    }

    /**
     * Create the property name to data type mapping.
     */
    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap<String, String>();
        }
        dataTypeMap.put(PROP_METHOD, "String");
        dataTypeMap.put(PROP_OBJECT, "String");
        dataTypeMap.put(PROP_ATTRIBUTE, "String");
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
     * Create the list of super-types for this type.
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
     * Create the set of sub-types for this type.
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
