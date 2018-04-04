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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for ViewIdentifierType complex type.
 *
 * <p>Below is a list of supported properties for {@link ViewIdentifierType}.
 *
 * <ul>
 * <li><b>viewName</b>: the name of the view.</li>
 * <li><b>viewEntryUniqueId</b>: the unique ID of the view entry.</li>
 * <li><b>viewEntryName</b>: the view entry name.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = ViewIdentifierType.TYPE_NAME)
public class ViewIdentifierType {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "ViewIdentifierType";

    /** Property name constant for the <b>viewName</b> property. */
    private static final String PROP_VIEW_NAME = "viewName";

    /** Property name constant for the <b>viewEntryUniqueId</b> property. */
    private static final String PROP_VIEW_ENTRY_UNIQUE_ID = "viewEntryUniqueId";

    /** Property name constant for the <b>viewEntryName</b> property. */
    private static final String PROP_VIEW_ENTRY_NAME = "viewEntryName";

    /** The name of the view. */
    @XmlAttribute(name = PROP_VIEW_NAME)
    protected String viewName;

    /** The unique ID of the view entry. */
    @XmlAttribute(name = PROP_VIEW_ENTRY_UNIQUE_ID)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String viewEntryUniqueId;

    /** The view entry name. */
    @XmlAttribute(name = PROP_VIEW_ENTRY_NAME)
    protected String viewEntryName;

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
     * Gets the value of the <b>viewName</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getViewName() {
        return viewName;
    }

    /**
     * Sets the value of the <b>viewName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setViewName(String value) {
        this.viewName = value;
    }

    /**
     * Returns true if the <b>viewName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetViewName() {
        return (this.viewName != null);
    }

    /**
     * Gets the value of the <b>viewEntryUniqueId</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getViewEntryUniqueId() {
        return viewEntryUniqueId;
    }

    /**
     * Sets the value of the <b>viewEntryUniqueId</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setViewEntryUniqueId(String value) {
        this.viewEntryUniqueId = value;
    }

    public boolean isSetViewEntryUniqueId() {
        return (this.viewEntryUniqueId != null);
    }

    /**
     * Gets the value of the <b>viewEntryName</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getViewEntryName() {
        return viewEntryName;
    }

    /**
     * Sets the value of the <b>viewEntryName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setViewEntryName(String value) {
        this.viewEntryName = value;
    }

    public boolean isSetViewEntryName() {
        return (this.viewEntryName != null);
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
        if (propName.equals(PROP_VIEW_NAME)) {
            return getViewName();
        }
        if (propName.equals(PROP_VIEW_ENTRY_UNIQUE_ID)) {
            return getViewEntryUniqueId();
        }
        if (propName.equals(PROP_VIEW_ENTRY_NAME)) {
            return getViewEntryName();
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
        if (propName.equals(PROP_VIEW_NAME)) {
            return isSetViewName();
        }
        if (propName.equals(PROP_VIEW_ENTRY_UNIQUE_ID)) {
            return isSetViewEntryUniqueId();
        }
        if (propName.equals(PROP_VIEW_ENTRY_NAME)) {
            return isSetViewEntryName();
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
        if (propName.equals(PROP_VIEW_NAME)) {
            setViewName(((String) value));
        }
        if (propName.equals(PROP_VIEW_ENTRY_UNIQUE_ID)) {
            setViewEntryUniqueId(((String) value));
        }
        if (propName.equals(PROP_VIEW_ENTRY_NAME)) {
            setViewEntryName(((String) value));
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
        return ViewIdentifierType.TYPE_NAME;
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
            names.add(PROP_VIEW_NAME);
            names.add(PROP_VIEW_ENTRY_UNIQUE_ID);
            names.add(PROP_VIEW_ENTRY_NAME);
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
        dataTypeMap.put(PROP_VIEW_NAME, "String");
        dataTypeMap.put(PROP_VIEW_ENTRY_UNIQUE_ID, "String");
        dataTypeMap.put(PROP_VIEW_ENTRY_NAME, "String");
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
        return dataTypeMap.get(propName);
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
