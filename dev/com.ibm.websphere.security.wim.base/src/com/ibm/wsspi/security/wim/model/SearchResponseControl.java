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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for SearchResponseControl complex type.
 *
 * <p> The SearchResponseControl object extends the {@link Control} object.
 *
 * <p> A SearchResponseControl object will only be returned from a search API call if the <b>countLimit</b> property in the
 * {@link SearchControl} object is set to a value greater than 0.
 *
 * <p>Below is a list of supported properties for {@link SearchResponseControl}.
 *
 * <ul>
 * <li><b>hasMoreResults</b>: will be set to true if the actual number of results from the search is greater
 * than the value of the <b>countLimit</b> property in the SearchControl object.
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link Control} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = SearchResponseControl.TYPE_NAME)
@XmlSeeAlso({
              ChangeResponseControl.class
})
public class SearchResponseControl extends Control {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "SearchResponseControl";

    /** Property name constant for the <b>hasMoreResults</b> property. */
    private static final String PROP_HAS_MORE_RESULTS = "hasMoreResults";

    /**
     * Will be set to true if the actual number of results from the search is greater
     * than the value of the <b>countLimit</b> property in the SearchControl object.
     */
    @XmlAttribute(name = PROP_HAS_MORE_RESULTS)
    protected Boolean hasMoreResults;

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
     * Gets the value of the <b>hasMoreResults</b> property.
     *
     * @return
     *         possible object is {@link Boolean }
     */
    public boolean isHasMoreResults() {
        return hasMoreResults;
    }

    /**
     * Sets the value of the <b>hasMoreResults</b> property.
     *
     * @param value
     *            allowed object is {@link Boolean }
     */
    public void setHasMoreResults(boolean value) {
        this.hasMoreResults = value;
    }

    public boolean isSetHasMoreResults() {
        return (this.hasMoreResults != null);
    }

    public void unsetHasMoreResults() {
        this.hasMoreResults = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_HAS_MORE_RESULTS)) {
            return this.hasMoreResults;
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_HAS_MORE_RESULTS)) {
            return isSetHasMoreResults();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_HAS_MORE_RESULTS)) {
            setHasMoreResults(((Boolean) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_HAS_MORE_RESULTS)) {
            unsetHasMoreResults();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Gets a list of all supported properties for this Type.
     *
     * @param entityTypeName
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link List}
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.add(PROP_HAS_MORE_RESULTS);
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
        dataTypeMap.put(PROP_HAS_MORE_RESULTS, "Boolean");
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
        subTypeSet.add(ChangeResponseControl.TYPE_NAME);
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
