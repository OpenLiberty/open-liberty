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
 * <p>Java class for ViewIdentifierType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ViewIdentifierType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="viewName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="viewEntryUniqueId" type="{http://www.w3.org/2001/XMLSchema}token" />
 *       &lt;attribute name="viewEntryName" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@Trivial
public class ViewIdentifierType {
    protected String viewName;
    protected String viewEntryUniqueId;
    protected String viewEntryName;
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
     * Gets the value of the viewName property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getViewName() {
        return viewName;
    }

    /**
     * Sets the value of the viewName property.
     *
     * @param value
     *                  allowed object is
     *                  {@link String }
     * 
     */
    public void setViewName(String value) {
        this.viewName = value;
    }

    public boolean isSetViewName() {
        return (this.viewName != null);
    }

    /**
     * Gets the value of the viewEntryUniqueId property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getViewEntryUniqueId() {
        return viewEntryUniqueId;
    }

    /**
     * Sets the value of the viewEntryUniqueId property.
     *
     * @param value
     *                  allowed object is
     *                  {@link String }
     * 
     */
    public void setViewEntryUniqueId(String value) {
        this.viewEntryUniqueId = value;
    }

    public boolean isSetViewEntryUniqueId() {
        return (this.viewEntryUniqueId != null);
    }

    /**
     * Gets the value of the viewEntryName property.
     *
     * @return
     *         possible object is
     *         {@link String }
     * 
     */
    public String getViewEntryName() {
        return viewEntryName;
    }

    /**
     * Sets the value of the viewEntryName property.
     *
     * @param value
     *                  allowed object is
     *                  {@link String }
     * 
     */
    public void setViewEntryName(String value) {
        this.viewEntryName = value;
    }

    public boolean isSetViewEntryName() {
        return (this.viewEntryName != null);
    }

    public Object get(String propName) {
        if (propName.equals("viewName")) {
            return getViewName();
        }
        if (propName.equals("viewEntryUniqueId")) {
            return getViewEntryUniqueId();
        }
        if (propName.equals("viewEntryName")) {
            return getViewEntryName();
        }
        return null;
    }

    public boolean isSet(String propName) {
        if (propName.equals("viewName")) {
            return isSetViewName();
        }
        if (propName.equals("viewEntryUniqueId")) {
            return isSetViewEntryUniqueId();
        }
        if (propName.equals("viewEntryName")) {
            return isSetViewEntryName();
        }
        return false;
    }

    public void set(String propName, Object value) {
        if (propName.equals("viewName")) {
            setViewName(((String) value));
        }
        if (propName.equals("viewEntryUniqueId")) {
            setViewEntryUniqueId(((String) value));
        }
        if (propName.equals("viewEntryName")) {
            setViewEntryName(((String) value));
        }
    }

    public void unset(String propName) {
    }

    public String getTypeName() {
        return "ViewIdentifierType";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("viewName");
                names.add("viewEntryUniqueId");
                names.add("viewEntryName");
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("viewName", "String");
        dataTypeMap.put("viewEntryUniqueId", "String");
        dataTypeMap.put("viewEntryName", "String");
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
