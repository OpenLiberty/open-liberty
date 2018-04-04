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

/**
 * <p>Java class for CacheControl complex type.
 *
 * <p> The CacheControl object extends the {@link Control} object and defines a single property: <b>mode</b>.
 *
 * <p> The CacheControl object specifies whether the repository adapter cache should be cleared before an operation is performed
 * and the mode of clearing the cache. The CacheControl object can be passed to the get(), search(), and update() APIs.
 * The LDAP adapter uses the CacheControl to clear its cache; the other out-of-the-box adapters ignore the CacheControl as they
 * not have a cache. If you are using a custom adapter, you can implement its own handling for clearing cache using the CacheControl.
 *
 * <p> The <b>mode</b> property specifies the mode of clearing the repository adapter cache before an operation is performed. Valid values
 * are:
 * <ul>
 * <li><b>clearEntity</b>: clears the cache for the specified entity. This value does not have any effect on the search() API</li>
 * <li><b>clearAll</b>: clears all of the cached information in the adapter.</li>
 * </ul>
 *
 * <p> The values are not case-sensitive. There is no default value for this property. If you do not specify a value, or specify a
 * value other than clearEntity or clearAll, an error message appears.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = CacheControl.TYPE_NAME, propOrder = {
                                                      "mode"
})
public class CacheControl extends Control {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "CacheControl";

    /** Property name constant for the <b>mode</b> property. */
    private static final String PROP_MODE = "mode";

    /**
     * The <b>mode</b> property specifies the mode of clearing the repository adapter cache before an operation is performed. Valid values
     * are:
     * <ul>
     * <li><b>clearEntity</b>: clears the cache for the specified entity. This value does not have any effect on the search() API</li>
     * <li><b>clearAll</b>: clears all of the cached information in the adapter.</li>
     * </ul>
     */
    @XmlElement(name = PROP_MODE, required = true)
    protected String mode;

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
     * Gets the value of the <b>mode</b> property.
     *
     * @return
     *         returned object is {@link String }
     *
     */
    public String getMode() {
        return mode;
    }

    /**
     * Sets the value of the <b>mode</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setMode(String value) {
        this.mode = value;
    }

    /**
     * Returns true if the <b>mode</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetMode() {
        return (this.mode != null);
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_MODE)) {
            return getMode();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_MODE)) {
            return isSetMode();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_MODE)) {
            setMode(((String) value));
        }
        super.set(propName, value);
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
            names.add(PROP_MODE);
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
        dataTypeMap.put(PROP_MODE, "String");
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
     * Set the list of super-types for this type.
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
     * Set the set of sub-types for this type.
     */
    private static synchronized void setSubTypes() {
        if (subTypeSet == null) {
            subTypeSet = new HashSet<String>();
        }
    }

    /**
     * Gets a set of any model objects which extend this type.
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
