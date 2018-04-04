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
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for GroupMembershipControl complex type.
 *
 * <p> The GroupMembershipControl object extends from the {@link GroupControl} object.
 *
 * <p> The GroupControl DataObject contains the following properties that are inherited by
 * GroupMembershipControl: <b>level</b>, <b>properties</b>, <b>searchBases</b>, <b>countLimit</b>,
 * <b>timeLimit</b>, <b>modifyMode</b> and <b>expression</b>.
 *
 * <p> GroupMembershipControl is used in the input {@link Root} object of both the get() API and update() API.
 *
 * <p> In the get() API, GroupMembershipControl is used for requesting the groups to which an entity
 * belongs. If theGroupMembershipControl is added to the input {@link Root} object, then the groups the
 * entity belongs to will be returned.
 *
 * <p> GroupMembershipControl can also be used for specifying the properties to be returned for groups
 * as well as the level of nested groups to be returned.
 *
 * <ul>
 * <li><b>level</b>
 * <ul>
 * <li><b>0</b>: will return all nested groups</li>
 * <li><b>1</b>: will cause only the immediate groups to be returned. This is the default value. </li>
 * <li><b>greater than 1</b>: will return the specified level number of groups. For example, a level of 2 will return immediate
 * groups and their immediate groups.</li>
 * </ul>
 * </ul>
 *
 * <p> In an update() API call, GroupMembershipControl can be used to specify the assign or un-assign mode
 * through the <b>modifyMode</b> property.
 *
 * <ul>
 * <li><b>modifyMode</b>
 * <ul>
 * <li><b>1</b>: the groups contained in the entity object will add the entity as their members. This is the default setting.</li>
 * <li><b>2</b>: the groups contained in the entity object will add the entity as their members and all the existing members will be removed.</li>
 * <li><b>3</b>: the groups contained in the entity object will remove the entity from their members.</li>
 * </ul>
 * <li>
 *
 * <li><b>treeView</b>: used to indicate whether the hierarchy of the nested groups should be
 * kept in the output {@link Root} object or not. If it is set to true, the hierarchy relationship of the
 * immediate groups and nested groups of different levels are kept in the {@link Root} object. If it is set to false,
 * all groups are put in a flat structure by pointing to the same groups property of the entity.</li>
 * </ul>
 *
 * <p> Since GroupMembershipControl is also extended from {@link SearchControl}, you can specify property <b>expression</b>
 * and other search properties like <b>countLimit</b>, and <b>timeLimit</b> in GroupMembershipControl to only return those
 * groups which satisfy the search criteria. For example, it is possible to add the property <b>expression</b>
 * with value: @xsi:type='Group' and cn='Admin*' to only return those groups whose cn property starts
 * with Admin.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = GroupMembershipControl.TYPE_NAME)
public class GroupMembershipControl extends GroupControl {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "GroupMembershipControl";

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
        if (propertyNames != null) {
            return propertyNames;
        } else {
            List<String> names = new ArrayList<String>();
            names.addAll(GroupControl.getPropertyNames(GroupControl.TYPE_NAME));
            propertyNames = Collections.unmodifiableList(names);
            return propertyNames;
        }
    }

    /**
     * Create the property name to data type mapping.
     */
    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap<String, String>();
        }
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
        superTypeList.add(GroupControl.TYPE_NAME);
        superTypeList.add(HierarchyControl.TYPE_NAME);
        superTypeList.add(SearchControl.TYPE_NAME);
        superTypeList.add(PropertyControl.TYPE_NAME);
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
}
