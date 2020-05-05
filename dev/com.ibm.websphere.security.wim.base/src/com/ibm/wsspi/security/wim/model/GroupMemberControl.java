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
 * <p>Java class for GroupMemberControl complex type.
 *
 * <p> The GroupMemberControl object extends the abstract {@link GroupControl} object.
 *
 * <p> The {@link GroupControl} object contains the following properties that are inherited by GroupMemberControl:
 * <b>level</b>, <b>properties</b>, <b>searchBases</b>, <b>countLimit</b>, <b>timeLimit</b>, <b>expression</b>,
 * <b>treeView</b>, and <b>modifyMode</b>.
 *
 * <p> GroupMemberControl is used in the input {@link Root} object of both the get() and update() APIs.
 * In the get() API, it is used for requesting members of this group. If it is added to the input {@link Root} object,
 * it indicates the members of group that will be returned.
 *
 * <p> GroupMemberControl can also be used to specify the properties to be returned for members as well
 * as the level of nested members to be returned.
 *
 * <ul>
 * <li><b>level</b>
 * <ul>
 * <li><b>0</b>: will return all nested members</li>
 * <li><b>1</b>: will cause only the immediate members to be returned. This is the default value. </li>
 * <li><b>greater than 1</b>: will return the specified level number of members. For example, a level of 2 will return immediate
 * members and their immediate members.</li>
 * </ul>
 * </ul>
 *
 *
 * <p> In the update() API, GroupMembersControl can be used to specify the assign or un-assign mode through
 * the modifyMode property. Multiple entities can be assigned or un-assigned in a single call.
 * If there is only partial success when assigning or un-assigning multiple entities, an exception will be thrown.
 * It is responsibility of the caller to perform any clean-up needed in the event of an exception.
 *
 * <ul>
 * <li><b>modifyMode</b>
 * <ul>
 * <li><b>1</b>: will cause the members in the group object to be added to this group as its members. This is the default setting.</li>
 * <li><b>2</b>: will cause the members contained in the group object to be added to the group as its members,
 * and all of the existing members of the group will be removed.</li>
 * <li><b>3</b>: will cause the members contained in the group object to be un-assigned from the group.</li>
 * </ul>
 * </ul>
 * <p> Since GroupMemberControl is also extended from {@link SearchControl}, it is possible to specify the property <b>expression</b>
 * and other search properties like <b>countLimit</b>, and <b>timeLimit</b> in GroupMemberControl to only return those members
 * which satisfy the search criteria. For example, the property <b>expression</b> with value: @xsi:type='Person' will
 * only return the members which are of the {@link Person} entity type.
 *
 * <ul>
 * <li><b>treeView</b>: used for indicating whether the hierarchy of the nested members should be kept
 * in the output {@link Root} object or not. If it is set to true, hierarchy relationship of the immediate members and
 * nested members of different levels are kept in the {@link Root} object. If it is set to false, all members are put
 * in a flat structure by pointing to the same members property of the group.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = GroupMemberControl.TYPE_NAME)
public class GroupMemberControl extends GroupControl {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "GroupMemberControl";

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
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.addAll(GroupControl.getPropertyNames(GroupControl.TYPE_NAME));
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
     * Create the list of sub-types for this type.
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
