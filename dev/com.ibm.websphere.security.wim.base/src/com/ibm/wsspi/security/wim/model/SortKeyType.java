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
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for SortKeyType complex type.
 *
 * <p>The SortKeyType provides control over how to sort results by property name.
 *
 * <p>Below is a list of supported properties for {@link SortKeyType}.
 *
 * <ul>
 * <li><b>propertyName</b>: defines the key by which to do the sort.</li>
 * <li><b>ascendingOrder</b>: is set to true by default and returns the
 * sorted objects in ascendingOrder. To sort by descending order, set the property
 * to false.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = SortKeyType.TYPE_NAME, propOrder = {
                                                     "propertyName",
                                                     "ascendingOrder"
})
public class SortKeyType {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "SortKeyType";

    /** Property name constant for the <b>propertyName</b> property. */
    private static final String PROP_PROPERTY_NAME = "propertyName";

    /** Property name constant for the <b>ascendingOrder</b> property. */
    private static final String PROP_ASCENDING_ORDER = "ascendingOrder";

    /**
     * The key by which to do the sort.
     */
    @XmlElement(name = PROP_PROPERTY_NAME, required = true)
    protected String propertyName;

    /**
     * Set to true (default) to return the sorted objects in ascendingOrder. To sort by
     * descending order, set the property to false.
     */
    @XmlElement(name = PROP_ASCENDING_ORDER, defaultValue = "true")
    protected boolean ascendingOrder;

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
     * Gets the value of the <b>propertyName</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Sets the value of the <b>propertyName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setPropertyName(String value) {
        this.propertyName = value;
    }

    /**
     * Returns true if the <b>propertyName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetPropertyName() {
        return (this.propertyName != null);
    }

    /**
     * Gets the value of the <b>ascendingOrder</b> property.
     *
     * @return
     *         possible object is {@link boolean}
     */
    public boolean isAscendingOrder() {
        return ascendingOrder;
    }

    /**
     * Sets the value of the <b>ascendingOrder</b> property.
     *
     * @param value
     *            allowed object is {@link boolean}
     */
    public void setAscendingOrder(boolean value) {
        this.ascendingOrder = value;
    }

    /**
     * Returns true if the <b>ascendingOrder</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetAscendingOrder() {
        return true;
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
        if (propName.equals(PROP_PROPERTY_NAME)) {
            return getPropertyName();
        }
        if (propName.equals(PROP_ASCENDING_ORDER)) {
            return isAscendingOrder();
        }
        return null;
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @param propName
     *            The property name to check if set.
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSet(String propName) {
        if (propName.equals(PROP_PROPERTY_NAME)) {
            return isSetPropertyName();
        }
        if (propName.equals(PROP_ASCENDING_ORDER)) {
            return isSetAscendingOrder();
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
        if (propName.equals(PROP_PROPERTY_NAME)) {
            setPropertyName(((String) value));
        }
        if (propName.equals(PROP_ASCENDING_ORDER)) {
            setAscendingOrder(((Boolean) value));
        }
    }

    /**
     * Sets the value of provided property to null.
     *
     * @param propName
     *            allowed object is {@link String}
     *
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
     * @return
     *         returned object is {@link List}
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.add(PROP_PROPERTY_NAME);
            names.add(PROP_ASCENDING_ORDER);
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
        dataTypeMap.put(PROP_PROPERTY_NAME, "String");
        dataTypeMap.put(PROP_ASCENDING_ORDER, "boolean");
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
