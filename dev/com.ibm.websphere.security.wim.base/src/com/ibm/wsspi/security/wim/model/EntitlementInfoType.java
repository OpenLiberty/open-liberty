/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vIBM 2.2.3-11/28/2011 06:21 AM(foreman)- 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.09.11 at 04:22:19 PM IST 
//

package com.ibm.wsspi.security.wim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for EntitlementInfoType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="EntitlementInfoType">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="roles" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element name="entitlements" type="{http://www.ibm.com/websphere/wim}EntitlementType" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element name="entitlementCheckResult" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 * &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EntitlementInfoType", propOrder = {
                                                    "roles",
                                                    "entitlements",
                                                    "entitlementCheckResult"
})
@Trivial
public class EntitlementInfoType {

    protected List<String> roles;
    protected List<com.ibm.wsspi.security.wim.model.EntitlementType> entitlements;
    protected boolean entitlementCheckResult;
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
     * Gets the value of the <b>roles</b> property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the roles property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getRoles().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getRoles() {
        if (roles == null) {
            roles = new ArrayList<String>();
        }
        return this.roles;
    }

    /**
     * Returns true if the <b>roles</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */
    public boolean isSetRoles() {
        return ((this.roles != null) && (!this.roles.isEmpty()));
    }

    /**
     * Resets the <b>roles</b> property to null.
     * 
     */
    public void unsetRoles() {
        this.roles = null;
    }

    /**
     * Gets the value of the <b>entitlements</b> property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the entitlements property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getEntitlements().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.EntitlementType }
     * 
     * 
     */
    public List<com.ibm.wsspi.security.wim.model.EntitlementType> getEntitlements() {
        if (entitlements == null) {
            entitlements = new ArrayList<com.ibm.wsspi.security.wim.model.EntitlementType>();
        }
        return this.entitlements;
    }

    /**
     * Returns true if the <b>entitlements</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */

    public boolean isSetEntitlements() {
        return ((this.entitlements != null) && (!this.entitlements.isEmpty()));
    }

    /**
     * Resets the <b>entitlements</b> property to null.
     * 
     */

    public void unsetEntitlements() {
        this.entitlements = null;
    }

    /**
     * Gets the value of the <b>entitlementCheckResult</b> property.
     * 
     */
    public boolean isEntitlementCheckResult() {
        return entitlementCheckResult;
    }

    /**
     * Sets the value of the <b>entitlementCheckResult</b> property.
     * 
     */
    public void setEntitlementCheckResult(boolean value) {
        this.entitlementCheckResult = value;
    }

    /**
     * Returns true if the <b>entitlementCheckResult</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */
    public boolean isSetEntitlementCheckResult() {
        return true;
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
        if (propName.equals("roles")) {
            return getRoles();
        }
        if (propName.equals("entitlements")) {
            return getEntitlements();
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
        if (propName.equals("roles")) {
            return isSetRoles();
        }
        if (propName.equals("entitlements")) {
            return isSetEntitlements();
        }
        if (propName.equals("entitlementCheckResult")) {
            return isSetEntitlementCheckResult();
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
        if (propName.equals("roles")) {
            getRoles().add(((String) value));
        }
        if (propName.equals("entitlements")) {
            getEntitlements().add(((com.ibm.wsspi.security.wim.model.EntitlementType) value));
        }
        if (propName.equals("entitlementCheckResult")) {
            setEntitlementCheckResult(((Boolean) value));
        }
    }

    /**
     * Sets the value of provided property to null.
     * 
     * @param propName
     *            allowed object is {@link String}
     * 
     */
    public void unset(String propName) {
        if (propName.equals("roles")) {
            unsetRoles();
        }
        if (propName.equals("entitlements")) {
            unsetEntitlements();
        }
    }

    /**
     * Gets the name of this model object, <b>EntitlementInfoType</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    public String getTypeName() {
        return "EntitlementInfoType";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>EntitlementInfoType</b>
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
                names.add("roles");
                names.add("entitlements");
                names.add("entitlementCheckResult");
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("roles", "String");
        dataTypeMap.put("entitlements", "EntitlementType");
        dataTypeMap.put("entitlementCheckResult", "boolean");
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
     * Gets a list of any model objects which this model object, <b>EntitlementInfoType</b>, is
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
     * Gets a set of any model objects which extend this model object, <b>EntitlementInfoType</b>
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
     * Returns this model object, <b>EntitlementInfotype</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }
}
