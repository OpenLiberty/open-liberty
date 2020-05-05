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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for Control complex type.
 *
 * <p> The Control object is used for specifying control information in calls to VMM. It can be divided into two categories: request control
 * and response control.
 *
 * <p> The Request control is sent from client to server within the input {@link Root} object. It is used for specifying requesting information.
 * For example, {@link PropertyControl} is used for specifying the name of properties needing to be returned for the entity. {@link GroupMembershipControl} is
 * used for requesting the groups the entity belongs to.
 *
 * <p> The Response control is sent from server to client within the output {@link Root} object. Response control is used for sending back control
 * information. For example, {@link PageResponseControl} is used for sending back the cookie to the client so that the client can send back the cookie
 * to request next page.
 *
 * <p> The Control object is at the top level of control hierarchy. All other controls are extended from it. The Control object itself is
 * abstract and is not directly used.
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Control.TYPE_NAME)
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
public abstract class Control {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "Control";

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
     * Gets the value of the requested property
     *
     * @param propName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link Object}
     */
    public Object get(String propName) {
        return null;
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @param propName
     *            The property name to check if set.
     * @return
     *         returned object is {@link boolean }
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
     * Gets the name of this type.
     *
     * @return
     *         returned object is {@link String}
     */
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
     * Set the list of sub-types for this entity type.
     */
    private static synchronized void setSubTypes() {
        if (subTypeSet == null) {
            subTypeSet = new HashSet<String>();
        }
        subTypeSet.add(PropertyControl.TYPE_NAME);
        subTypeSet.add(DescendantControl.TYPE_NAME);
        subTypeSet.add(GroupMemberControl.TYPE_NAME);
        subTypeSet.add(GroupMembershipControl.TYPE_NAME);
        subTypeSet.add(HierarchyControl.TYPE_NAME);
        subTypeSet.add(DeleteControl.TYPE_NAME);
        subTypeSet.add(LoginControl.TYPE_NAME);
        subTypeSet.add(CacheControl.TYPE_NAME);
        subTypeSet.add(ExternalNameControl.TYPE_NAME);
        subTypeSet.add(AncestorControl.TYPE_NAME);
        subTypeSet.add(ChangeResponseControl.TYPE_NAME);
        subTypeSet.add(ChangeControl.TYPE_NAME);
        subTypeSet.add(PageResponseControl.TYPE_NAME);
        subTypeSet.add(PageControl.TYPE_NAME);
        subTypeSet.add(CheckGroupMembershipControl.TYPE_NAME);
        subTypeSet.add(SearchResponseControl.TYPE_NAME);
        subTypeSet.add(SearchControl.TYPE_NAME);
        subTypeSet.add(GroupControl.TYPE_NAME);
        subTypeSet.add(SortControl.TYPE_NAME);
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
