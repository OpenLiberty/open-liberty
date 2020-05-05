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

/**
 * <p>Java class for CheckGroupMembershipControl complex type.
 *
 * <p> In order to check whether a group contains a member or not, the caller can issue the
 * get() API call with one group object and one member object, and with the CheckGroupMembershipControl.
 * The underlying repository will be checked and a boolean value will be returned in the CheckGroupMembershipControl
 * object of the returned Root object to indicate the membership relationship, i.e, if the member is a part of the group.
 *
 * <p>Below is a list of supported properties for {@link CheckGroupMembershipControl}.
 *
 * <ul>
 * <li><b>level</b>: indicates the level of members to be returned.</li>
 * <li><b>inGroup</b>: indicates the result in the returned Root object after checking the group membership.
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link Control} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = CheckGroupMembershipControl.TYPE_NAME)
public class CheckGroupMembershipControl extends Control {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "CheckGroupMembershipControl";

    /** Property name constant for the <b>level</b> property. */
    private static final String PROP_LEVEL = "level";

    /** Property name constant for the <b>inGroup</b> property. */
    private static final String PROP_IN_GROUP = "inGroup";

    /**
     * The level of members to be returned.
     */
    @XmlAttribute(name = PROP_LEVEL)
    protected Integer level;

    /**
     * The result in the returned Root object after checking the group membership.
     */
    @XmlAttribute(name = PROP_IN_GROUP)
    protected Boolean inGroup;

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
     * Gets the value of the <b>level</b> property.
     *
     * @return
     *         possible object is {@link Integer }
     */
    public int getLevel() {
        if (level == null) {
            return 1;
        } else {
            return level;
        }
    }

    /**
     * Sets the value of the <b>level</b> property.
     *
     * @param value
     *            allowed object is {@link Integer }
     */
    public void setLevel(int value) {
        this.level = value;
    }

    /**
     * Returns true if the <b>level</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetLevel() {
        return (this.level != null);
    }

    /**
     * Resets the <b>level</b> property to null.
     */
    public void unsetLevel() {
        this.level = null;
    }

    /**
     * Gets the value of the <b>inGroup</b> property.
     *
     * @return
     *         possible object is {@link Boolean }
     */
    public boolean isInGroup() {
        if (inGroup == null) {
            return false;
        } else {
            return inGroup;
        }
    }

    /**
     * Sets the value of the <b>inGroup</b> property.
     *
     * @param value
     *            allowed object is {@link Boolean }
     */
    public void setInGroup(boolean value) {
        this.inGroup = value;
    }

    /**
     * Returns true if the <b>inGroup</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetInGroup() {
        return (this.inGroup != null);
    }

    /**
     * Resets the <b>inGroup</b> property to null.
     */
    public void unsetInGroup() {
        this.inGroup = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_LEVEL)) {
            return getLevel();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_LEVEL)) {
            return isSetLevel();
        }
        if (propName.equals(PROP_IN_GROUP)) {
            return isSetInGroup();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_LEVEL)) {
            setLevel(((Integer) value));
        }
        if (propName.equals(PROP_IN_GROUP)) {
            setInGroup(((Boolean) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_LEVEL)) {
            unsetLevel();
        }
        if (propName.equals(PROP_IN_GROUP)) {
            unsetInGroup();
        }
        super.unset(propName);
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
            names.add(PROP_LEVEL);
            names.add(PROP_IN_GROUP);
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
        dataTypeMap.put(PROP_LEVEL, "Integer");
        dataTypeMap.put(PROP_IN_GROUP, "Boolean");
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
     * Create the list of super-types for this type.
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
     * Create the set of sub-types for this type.
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
