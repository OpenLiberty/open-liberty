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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for Context complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Context">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="key" type="{http://www.w3.org/2001/XMLSchema}string"/>
 * &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}anySimpleType"/>
 * b &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The Context object is used for storing contextual information.
 * 
 * <p> A client may communicate contextual information to VMM. For example, when creating a Person,
 * the client application may need to specify the realm to create the Person under. The realm itself is not a
 * part of the Person object's data. Another example is VMM may need the client application to pass in an ip
 * address.
 * 
 * <p> The Context object is used for storing this contextual information. The Context object supports
 * arbitrary key -> object mappings in order to support attributes that are not already pre-defined in the VMM schema
 * 
 * <p> The Context object consists of key-value pair mappings of contextual information.
 * 
 * <ul>
 * <li><b>key</b>: specifies the name of a contextual property. The following are two examples
 * of possible keys: realm, ipAddress</li>
 * <li><b>value</b>: specifies the value of a contextual property. Since value can be a String or
 * any arbitrary object, the type of the property value is <b>anySimpleType</b>. This property can
 * either be a single value or multi-valued.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Context", propOrder = {
                                        "key",
                                        "value"
})
@Trivial
public class Context {

    @XmlElement(required = true)
    protected String key;
    @XmlElement(required = true)
    @XmlSchemaType(name = "anySimpleType")
    protected Object value;
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
     * Gets the value of the <b>key</b> property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the value of the <b>key</b> property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setKey(String value) {
        this.key = value;
    }

    /**
     * Returns true if the <b>key</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */
    public boolean isSetKey() {
        return (this.key != null);
    }

    /**
     * Gets the value of the <b>value</b> property.
     * 
     * @return
     *         possible object is {@link Object }
     * 
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value of the <b>value</b> property.
     * 
     * @param value
     *            allowed object is {@link Object }
     * 
     */
    public void setValue(Object value) {
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
        if (propName.equals("key")) {
            return getKey();
        }
        if (propName.equals("value")) {
            return getValue();
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
        if (propName.equals("key")) {
            return isSetKey();
        }
        if (propName.equals("value")) {
            return isSetValue();
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
        if (propName.equals("key")) {
            setKey(((String) value));
        }
        if (propName.equals("value")) {
            setValue(value);
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
     * Gets the name of this model object, <b>Context</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    public String getTypeName() {
        return "Context";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>Context</b>
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
                names.add("key");
                names.add("value");
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("key", "String");
        dataTypeMap.put("value", "Object");
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
     * Gets a list of any model objects which this model object, <b>Context</b> is
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
     * Gets a set of any model objects which extend this model object, <b>Context</b>
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
     * Returns this model object, <b>Context</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
