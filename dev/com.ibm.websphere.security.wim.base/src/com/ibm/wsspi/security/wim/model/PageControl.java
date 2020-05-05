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

/**
 * <p>Java class for PageControl complex type.
 *
 * <p>The PageControl object extends the {@link Control} object and allows control over
 * how paging is handled for a request.
 *
 * <p>Below is a list of supported properties for {@link PageControl}.
 *
 * <ul>
 * <li><b>size</b>: indicates the size of the page.</li>
 * <li><b>startIndex</b>: indicates the starting index for the page.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link Control} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = PageControl.TYPE_NAME)
public class PageControl extends Control {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "PageControl";

    /** Property name constant for the <b>size</b> property. */
    private static final String PROP_SIZE = "size";

    /** Property name constant for the <b>startIndex</b> property. */
    private static final String PROP_START_INDEX = "startIndex";

    /**
     * The size of the page.
     */
    @XmlAttribute(name = PROP_SIZE)
    protected Integer size;

    /**
     * The starting index for the page.
     */
    @XmlAttribute(name = PROP_START_INDEX)
    protected Integer startIndex;

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
     * Gets the value of the <b>size</b> property.
     *
     * @return
     *         possible object is {@link Integer }
     */
    public int getSize() {
        if (size == null) {
            return 0;
        } else {
            return size;
        }
    }

    /**
     * Sets the value of the <b>size</b> property.
     *
     * @param value
     *            allowed object is {@link Integer }
     */
    public void setSize(int value) {
        this.size = value;
    }

    /**
     * Returns a true if the <b>size</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetSize() {
        return (this.size != null);
    }

    /**
     * Resets the <b>size</b> property to null
     */
    public void unsetSize() {
        this.size = null;
    }

    /**
     * Gets the value of the <b>startIndex</b> property.
     *
     * @return
     *         possible object is {@link Integer }
     */
    public int getStartIndex() {
        if (startIndex == null) {
            return 1;
        } else {
            return startIndex;
        }
    }

    /**
     * Sets the value of the <b>startIndex</b> property.
     *
     * @param value
     *            allowed object is {@link Integer }
     */
    public void setStartIndex(int value) {
        this.startIndex = value;
    }

    /**
     * Returns a true if the <b>startIndex</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetStartIndex() {
        return (this.startIndex != null);
    }

    /**
     * Resets the <b>startIndex</b> property to null
     */
    public void unsetStartIndex() {
        this.startIndex = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_SIZE)) {
            return getSize();
        }
        if (propName.equals(PROP_START_INDEX)) {
            return getStartIndex();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_SIZE)) {
            return isSetSize();
        }
        if (propName.equals(PROP_START_INDEX)) {
            return isSetStartIndex();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_SIZE)) {
            setSize((Integer) value);
        }
        if (propName.equals(PROP_START_INDEX)) {
            setStartIndex((Integer) value);
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_SIZE)) {
            unsetSize();
        }
        if (propName.equals(PROP_START_INDEX)) {
            unsetStartIndex();
        }
        super.unset(propName);
    }

    @Override
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
            names.add(PROP_SIZE);
            names.add(PROP_START_INDEX);
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
        dataTypeMap.put(PROP_SIZE, "Integer");
        dataTypeMap.put(PROP_START_INDEX, "Integer");
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
     * Create the list of super-types for this type.
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
}
