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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for Control complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Control">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The Control object is used for specifying control information in calls to VMM. It can be divided into two categories: request control
 * and response control.
 * 
 * <p> The Request control is sent from client to server within the input Root object. It is used for specifying requesting information.
 * For example, <b>PropertyControl</b> is used for specifying the name of properties needing to be returned for the entity. <b>GroupMembershipControl</b> is
 * used for requesting the groups the entity belongs to.
 * 
 * <p> The Response control is sent from server to client within the output Root object. Response control is used for sending back control
 * information. For example, <b>PageResponseControl</b> is used for sending back the cookie to the client so that the client can send back the cookie
 * to request next page.
 * 
 * <p> The Control object is at the top level of control hierarchy. All other controls are extended from it. The Control object itself is
 * abstract and is not directly used.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Control")
@XmlSeeAlso({
             DeleteControl.class,
             CacheControl.class,
             ExternalNameControl.class,
             PageResponseControl.class,
             PageControl.class,
             CheckGroupMembershipControl.class,
             SearchResponseControl.class,
             PropertyControl.class,
             SortControl.class
})
@Trivial
public abstract class Control {

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
    public void set(String propName, Object value) {}

    /**
     * Sets the value of provided property to null.
     * 
     * @param propName
     *            allowed object is {@link String}
     * 
     */
    public void unset(String propName) {}

    /**
     * Gets the name of this model object, <b>Control</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    public String getTypeName() {
        return "Control";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>Control</b>
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
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
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
     * Gets a list of any model objects which this model object, <b>Control</b>, is
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
        subTypeList.add("PropertyControl");
        subTypeList.add("DescendantControl");
        subTypeList.add("GroupMemberControl");
        subTypeList.add("GroupMembershipControl");
        subTypeList.add("HierarchyControl");
        subTypeList.add("DeleteControl");
        subTypeList.add("LoginControl");
        subTypeList.add("CacheControl");
        subTypeList.add("ExternalNameControl");
        subTypeList.add("AncestorControl");
        subTypeList.add("ChangeResponseControl");
        subTypeList.add("ChangeControl");
        subTypeList.add("PageResponseControl");
        subTypeList.add("PageControl");
        subTypeList.add("CheckGroupMembershipControl");
        subTypeList.add("SearchResponseControl");
        subTypeList.add("SearchControl");
        subTypeList.add("GroupControl");
        subTypeList.add("SortControl");
    }

    /**
     * Gets a set of any model objects which extend this model object, <b>Control</b>
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
     * Returns this model object, <b>Control</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
