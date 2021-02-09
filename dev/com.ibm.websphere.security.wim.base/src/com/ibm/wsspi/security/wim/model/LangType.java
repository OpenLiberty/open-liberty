/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for LangType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LangType">
 * &lt;simpleContent>
 * &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 * &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang default="en""/>
 * &lt;/extension>
 * &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The LangType object defines the language to be used. By default, the
 * language is set to English.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LangType", propOrder = {
                                         "value"
})
@Trivial
public class LangType {

    @XmlValue
    protected String value;
    @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
    protected String lang;
    private static List propertyNames = null;
    private static HashMap dataTypeMap = null;
    private static ArrayList superTypeList = null;
    private static HashSet subTypeList = null;

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
     * 
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the <b>value</b> property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns true if the <b>value</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */

    public boolean isSetValue() {
        return (this.value != null);
    }

    /**
     * Gets the value of the <b>lang</b> property.
     * 
     * @return
     *         possible object is {@link String }
     * 
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
     * 
     */
    public void setLang(String value) {
        this.lang = value;
    }

    /**
     * Returns true if the <b>lang</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
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
     * 
     */
    public Object get(String propName) {
        if (propName.equals("value")) {
            return getValue();
        }
        if (propName.equals("lang")) {
            return getLang();
        }
        return null;
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */

    public boolean isSet(String propName) {
        if (propName.equals("value")) {
            return isSetValue();
        }
        if (propName.equals("lang")) {
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
     * 
     */

    public void set(String propName, Object value) {
        if (propName.equals("value")) {
            setValue(((String) value));
        }
        if (propName.equals("lang")) {
            setLang(((String) value));
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
     * Gets the name of this model object, <b>LangType</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    public String getTypeName() {
        return "LangType";
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
    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("value");
                names.add("lang");
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("value", "String");
        dataTypeMap.put("lang", "String");
    }

    /**
     * Gets the Java type of the value of the provided property. For example: String, List
     * 
     * @param propName
     *            allowed object is {@link String}
     * 
     * @return
     *         returned object is {@link String}
     */
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return ((String) dataTypeMap.get(propName));
        } else {
            return null;
        }
    }

    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList();
        }
    }

    /**
     * Gets a list of any model objects which this model object, <b>LangType</b>, is
     * an extension of.
     * 
     * @return
     *         returned object is {@link ArrayList}
     */

    public ArrayList getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    /**
     * Returns a true if the provided model object is one that this
     * model object extends; false, otherwise.
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

    private static synchronized void setSubTypes() {
        if (subTypeList == null) {
            subTypeList = new HashSet();
        }
    }

    /**
     * Gets a set of any model objects which extend this model object, <b>LangType</b>
     * 
     * @return
     *         returned object is {@link HashSet}
     */

    public static HashSet getSubTypes() {
        if (subTypeList == null) {
            setSubTypes();
        }
        return subTypeList;
    }

    /**
     * Returns this model object, <b>LangType</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
