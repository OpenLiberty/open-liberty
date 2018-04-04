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

import javax.naming.ldap.SortKey;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * <p>Java class for SortControl complex type.
 *
 * <p> The SortControl object extends the {@link Control} object.
 *
 * <p>Below is a list of supported properties for {@link SortControl}.
 *
 * <ul>
 * <li><b>sortKeys</b>: contains a list of attributes will be used to do the sorting.
 * For each attribute a sorting order can be specified by the <b>ascendingOrder</b> property in
 * the {@link SortKey} object. For example, a caller wants to sort the search results by 'sn' in
 * descending order. The caller needs to set the <b>ascendingOrder</b> property to false.
 * The properties included in the SortControl object must be listed in the property list of
 * the {@link SearchControl}.</li>
 * <li><b>locale</b>: indicates which language will be used during the sorting operation.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link Control} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = SortControl.TYPE_NAME, propOrder = {
                                                     "sortKeys",
                                                     "locale"
})
public class SortControl extends Control {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "SortControl";

    /** Property name constant for the <b>sortKeys</b> property. */
    private static final String PROP_SORT_KEYS = "sortKeys";

    /** Property name constant for the <b>locale</b> property. */
    private static final String PROP_LOCALE = "locale";

    /**
     * List of attributes will be used to do the sorting. For each attribute a sorting order can be
     * specified by the <b>ascendingOrder</b> property in the {@link SortKey} object. For example,
     * a caller wants to sort the search results by 'sn' in descending order. The caller needs to
     * set the <b>ascendingOrder</b> property to false. The properties included in the SortControl
     * object must be listed in the property list of the {@link SearchControl}.
     */
    @XmlElement(name = PROP_SORT_KEYS)
    protected List<SortKeyType> sortKeys;

    /**
     * Which language will be used during the sorting operation.
     */
    @XmlElement(name = PROP_LOCALE, required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "language")
    protected String locale;

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
     * Gets the value of the <b>sortKeys</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>sortKeys</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getSortKeys().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link SortKeyType }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<SortKeyType> getSortKeys() {
        if (sortKeys == null) {
            sortKeys = new ArrayList<SortKeyType>();
        }
        return this.sortKeys;
    }

    /**
     * Returns true if the <b>sortKeys</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetSortKeys() {
        return ((this.sortKeys != null) && (!this.sortKeys.isEmpty()));
    }

    /**
     * Sets the value of <b>sortKeys</b> property to null.
     */
    public void unsetSortKeys() {
        this.sortKeys = null;
    }

    /**
     * Gets the value of the <b>locale</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getLocale() {
        return locale;
    }

    /**
     * Sets the value of the <b>locale</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setLocale(String value) {
        this.locale = value;
    }

    /**
     * Returns true if the <b>locale</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetLocale() {
        return (this.locale != null);
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_SORT_KEYS)) {
            return getSortKeys();
        }
        if (propName.equals(PROP_LOCALE)) {
            return getLocale();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_SORT_KEYS)) {
            return isSetSortKeys();
        }
        if (propName.equals(PROP_LOCALE)) {
            return isSetLocale();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_SORT_KEYS)) {
            getSortKeys().add((SortKeyType) value);
        }
        if (propName.equals(PROP_LOCALE)) {
            setLocale(((String) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_SORT_KEYS)) {
            unsetSortKeys();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return SortControl.TYPE_NAME;
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
            names.add(PROP_SORT_KEYS);
            names.add(PROP_LOCALE);
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
        dataTypeMap.put(PROP_SORT_KEYS, SortKeyType.TYPE_NAME);
        dataTypeMap.put(PROP_LOCALE, "String");
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
