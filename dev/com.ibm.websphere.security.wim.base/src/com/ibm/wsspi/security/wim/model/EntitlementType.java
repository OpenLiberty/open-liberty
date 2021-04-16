/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for EntitlementType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="EntitlementType">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;attribute name="method" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;attribute name="object" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;attribute name="attribute" type="{http://www.w3.org/2001/XMLSchema}string" />
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@Trivial
public class EntitlementType {

    protected String method;
    protected String object;
    protected String attribute;
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
     * Gets the value of the <b>method</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the value of the <b>method</b> property.
     *
     * @param value
     *                  allowed object is {@link String }
     *
     */
    public void setMethod(String value) {
        this.method = value;
    }

    /**
     * Returns true if the <b>method</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */

    public boolean isSetMethod() {
        return (this.method != null);
    }

    /**
     * Gets the value of the <b>object</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getObject() {
        return object;
    }

    /**
     * Sets the value of the <b>object</b> property.
     *
     * @param value
     *                  allowed object is {@link String }
     *
     */
    public void setObject(String value) {
        this.object = value;
    }

    /**
     * Returns true if the <b>object</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetObject() {
        return (this.object != null);
    }

    /**
     * Gets the value of the <b>attribute</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getAttribute() {
        return attribute;
    }

    /**
     * Sets the value of the <b>attribute</b> property.
     *
     * @param value
     *                  allowed object is {@link String }
     *
     */
    public void setAttribute(String value) {
        this.attribute = value;
    }

    /**
     * Returns true if the <b>attribute</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetAttribute() {
        return (this.attribute != null);
    }

    /**
     * Gets the value of the requested property
     *
     * @param propName
     *                     allowed object is {@link String}
     *
     * @return
     *         returned object is {@link Object}
     *
     */
    public Object get(String propName) {
        if (propName.equals("method")) {
            return getMethod();
        }
        if (propName.equals("object")) {
            return getObject();
        }
        if (propName.equals("attribute")) {
            return getAttribute();
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
        if (propName.equals("method")) {
            return isSetMethod();
        }
        if (propName.equals("object")) {
            return isSetObject();
        }
        if (propName.equals("attribute")) {
            return isSetAttribute();
        }
        return false;
    }

    /**
     * Sets the value of the provided property to the provided value.
     *
     * @param propName
     *                     allowed object is {@link String}
     * @param value
     *                     allowed object is {@link Object}
     *
     */
    public void set(String propName, Object value) {
        if (propName.equals("method")) {
            setMethod(((String) value));
        }
        if (propName.equals("object")) {
            setObject(((String) value));
        }
        if (propName.equals("attribute")) {
            setAttribute(((String) value));
        }
    }

    /**
     * Sets the value of provided property to null.
     *
     * @param propName
     *                     allowed object is {@link String}
     *
     */
    public void unset(String propName) {
    }

    /**
     * Gets the name of this model object, <b>EntitlementType</b>
     *
     * @return
     *         returned object is {@link String}
     */
    public String getTypeName() {
        return "EntitlementType";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>EntitlementType</b>
     *
     * @param entityTypeName
     *                           allowed object is {@link String}
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
                names.add("method");
                names.add("object");
                names.add("attribute");
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("method", "String");
        dataTypeMap.put("object", "String");
        dataTypeMap.put("attribute", "String");
    }

    /**
     * Gets the Java type of the value of the provided property. For example: String, List
     *
     * @param propName
     *                     allowed object is {@link String}
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
     * Gets a list of any model objects which this model object, <b>EntitlementType</b>, is
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
     *                          allowed object is {@link String}
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
     * Gets a set of any model objects which extend this model object, <b>EntitlementType</b>
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
     * Returns this model object, <b>EntitlementType</b>, and its contents as a String
     *
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }
}
