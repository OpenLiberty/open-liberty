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
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for SortKeyType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SortKeyType">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="propertyName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 * &lt;element name="ascendingOrder" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 * &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The SortKeyType object defines two properties:
 * 
 * <ul>
 * <li><b>propertyName</b>: defines the key by which to do the sort.</li>
 * 
 * <li><b>ascendingOrder</b>: is set to true by default and returns the
 * sorted objects in ascendingOrder. To sort by descending order, set the property
 * to false.</li>
 * </ul>
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SortKeyType", propOrder = {
                                            "propertyName",
                                            "ascendingOrder"
})
@Trivial
public class SortKeyType {

    @XmlElement(required = true)
    protected String propertyName;
    @XmlElement(defaultValue = "true")
    protected boolean ascendingOrder;
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
     * Gets the value of the propertyName property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Sets the value of the propertyName property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setPropertyName(String value) {
        this.propertyName = value;
    }

    public boolean isSetPropertyName() {
        return (this.propertyName != null);
    }

    /**
     * Gets the value of the ascendingOrder property.
     * 
     */
    public boolean isAscendingOrder() {
        return ascendingOrder;
    }

    /**
     * Sets the value of the ascendingOrder property.
     * 
     */
    public void setAscendingOrder(boolean value) {
        this.ascendingOrder = value;
    }

    public boolean isSetAscendingOrder() {
        return true;
    }

    public Object get(String propName) {
        if (propName.equals("propertyName")) {
            return getPropertyName();
        }
        return null;
    }

    public boolean isSet(String propName) {
        if (propName.equals("propertyName")) {
            return isSetPropertyName();
        }
        if (propName.equals("ascendingOrder")) {
            return isSetAscendingOrder();
        }
        return false;
    }

    public void set(String propName, Object value) {
        if (propName.equals("propertyName")) {
            setPropertyName(((String) value));
        }
        if (propName.equals("ascendingOrder")) {
            setAscendingOrder(((Boolean) value));
        }
    }

    public void unset(String propName) {}

    public String getTypeName() {
        return "SortKeyType";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("propertyName");
                names.add("ascendingOrder");
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("propertyName", "String");
        dataTypeMap.put("ascendingOrder", "boolean");
    }

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

    public ArrayList getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    private static synchronized void setSubTypes() {
        if (subTypeList == null) {
            subTypeList = new HashSet();
        }
    }

    public static HashSet getSubTypes() {
        if (subTypeList == null) {
            setSubTypes();
        }
        return subTypeList;
    }

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
