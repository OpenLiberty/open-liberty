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
import javax.xml.bind.annotation.XmlValue;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for LangType complex type.
 *
 * <p> The LangType object defines the language to be used. By default, the language is set to English.
 *
 * <p>Below is a list of supported properties for {@link LangType}.
 *
 * <ul>
 * <li><b>value</b>: The value.</li>
 * <li><b>lang</b>: The language.</li>
 * </ul>
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = LangType.TYPE_NAME, propOrder = {
                                                  "value"
})
public class LangType {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "LangType";

    /** Property name constant for the <b>value</b> property. */
    private static final String PROP_VALUE = "value";

    /** Property name constant for the <b>lang</b> property. */
    private static final String PROP_LANG = "lang";

    /**
     * The value of the LangType.
     */
    @XmlValue
    protected String value;

    /**
     * The language of the LangType.
     */
    @XmlAttribute(name = PROP_LANG, namespace = "http://www.w3.org/XML/1998/namespace")
    protected String lang;

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
     * Gets the value of the <b>value</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the <b>value</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setValue(String value) {
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
     * Gets the value of the <b>lang</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getLang() {
        if (lang == null) {
            return "en";
        } else {
            return lang;
        }
    }

    /**
     * Sets the value of the <b>lang</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setLang(String value) {
        this.lang = value;
    }

    /**
     * Returns true if the <b>lang</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetLang() {
        return (this.lang != null);
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
        if (propName.equals(PROP_VALUE)) {
            return getValue();
        }
        if (propName.equals(PROP_LANG)) {
            return getLang();
        }
        return null;
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @param propName
     *            The property name to check if is set.
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSet(String propName) {
        if (propName.equals(PROP_VALUE)) {
            return isSetValue();
        }
        if (propName.equals(PROP_LANG)) {
            return isSetLang();
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
        if (propName.equals(PROP_VALUE)) {
            setValue(((String) value));
        }
        if (propName.equals(PROP_LANG)) {
            setLang(((String) value));
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
     * Gets the name of this model object, <b>LangType</b>
     *
     * @return
     *         returned object is {@link String}
     */
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Gets a list of all supported properties for this model object, <b>LangType</b>
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
            names.add(PROP_VALUE);
            names.add(PROP_LANG);
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
        dataTypeMap.put(PROP_VALUE, "String");
        dataTypeMap.put(PROP_LANG, "String");
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
     * Set the list of super-types for this type.
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
     * Set the set of sub-types for this type.
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
