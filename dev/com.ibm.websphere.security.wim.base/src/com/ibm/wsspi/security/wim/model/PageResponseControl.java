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

import static com.ibm.wsspi.security.wim.SchemaConstants.DO_PAGE_RESPONSE_CONTROL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for PageResponseControl complex type.
 *
 * <p> The PageResponseControl object extends the {@link Control} object and
 * returns information on how the paging request was handled.
 *
 * <p>Below is a list of supported properties for {@link PageResponseControl}.
 *
 * <ul>
 * <li><b>totalSize</b>: indicates the totalSize of the paging search.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link Control} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = DO_PAGE_RESPONSE_CONTROL)
public class PageResponseControl extends Control {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "PageResponseControl";

    /** Property name constant for the <b>totalSize</b> property. */
    private static final String PROP_TOTAL_SIZE = "totalSize";

    /**
     * The total size of the paging search.
     */
    @XmlAttribute(name = PROP_TOTAL_SIZE)
    protected Integer totalSize;

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
     * Gets the value of the <b>totalSize</b> property.
     *
     * @return
     *         possible object is {@link Integer }
     */
    public int getTotalSize() {
        if (totalSize == null) {
            return 0;
        } else {
            return totalSize;
        }
    }

    /**
     * Sets the value of the <b>totalSize</b> property.
     *
     * @param value
     *            allowed object is {@link Integer }
     */
    public void setTotalSize(int value) {
        this.totalSize = value;
    }

    /**
     * Check if the <b>totalSize</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetTotalSize() {
        return (this.totalSize != null);
    }

    /**
     * Unset the <b>totalSize</b> property.
     */
    public void unsetTotalSize() {
        this.totalSize = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_TOTAL_SIZE)) {
            return getTotalSize();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_TOTAL_SIZE)) {
            return isSetTotalSize();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_TOTAL_SIZE)) {
            setTotalSize(((Integer) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_TOTAL_SIZE)) {
            unsetTotalSize();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Get the list of all property names for this type.
     *
     * @param entityTypeName The entity name type.
     * @return The list of property names.
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.add(PROP_TOTAL_SIZE);
            names.addAll(Control.getPropertyNames(Control.TYPE_NAME));
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
        dataTypeMap.put(PROP_TOTAL_SIZE, "Integer");
    }

    @Override
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return (dataTypeMap.get(propName));
        } else {
            return super.getDataType(propName);
        }
    }

    /**
     * Set the list of super-types for this entity type.
     */
    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList<String>();
        }
        superTypeList.add(Control.TYPE_NAME);
    }

    @Override
    public ArrayList<String> getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    @Override
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
     * Get the set of sub-types for this type.
     *
     * @return The set of sub-types.
     */
    public static HashSet<String> getSubTypes() {
        if (subTypeSet == null) {
            setSubTypes();
        }
        return subTypeSet;
    }
}
