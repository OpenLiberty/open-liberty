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
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for GroupMembershipControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GroupMembershipControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}GroupControl">
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The GroupMembershipControl object extends from the abstract GroupControl object.
 * 
 * <p> The GroupControl DataObject contains the following properties that are inherited by
 * GroupMembershipControl: <b>level</b>, <b>properties</b>, <b>searchBases</b>, <b>countLimit</b>,
 * <b>timeLimit</b>, <b>modifyMode</b> and <b>expression</b>.
 * 
 * <p> GroupMembershipControl is used in the input Root object of both the get() API and update() API.
 * 
 * <p> In the get() API, GroupMembershipControl is used for requesting the groups to which an entity
 * belongs. If theGroupMembershipControl is added to the input Root object, then the groups the
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
 * </ul>
 * 
 * <p> Since GroupMembershipControl is also extended from SearchControl, you can specify property <b>expression</b>
 * and other search properties like <b>countLimit</b>, and <b>timeLimit</b> in GroupMembershipControl to only return those
 * groups which satisfy the search criteria. For example, it is possible to add the property <b>expression</b>
 * with value: @xsi:type='Group' and cn='Admin*' to only return those groups whose cn property starts
 * with Admin.
 * 
 * <li><b>treeView</b>: used to indicate whether the hierarchy of the nested groups should be
 * kept in the output Root object or not. If it is set to true, the hierarchy relationship of the
 * immediate groups and nested groups of different levels are kept in the Root object. If it is set to false,
 * all groups are put in a flat structure by pointing to the same groups property of the entity.</li>
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GroupMembershipControl")
@Trivial
public class GroupMembershipControl
                extends GroupControl
{

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
    @Override
    public Object get(String propName) {
        return super.get(propName);
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */
    @Override
    public boolean isSet(String propName) {
        return super.isSet(propName);
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
    @Override
    public void set(String propName, Object value) {
        super.set(propName, value);
    }

    /**
     * Sets the value of provided property to null.
     * 
     * @param propName
     *            allowed object is {@link String}
     * 
     */
    @Override
    public void unset(String propName) {
        super.unset(propName);
    }

    /**
     * Gets the name of this model object, <b>GroupMembershipControl</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String getTypeName() {
        return "GroupMembershipControl";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>GroupMembershipControl</b>
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
                names.addAll(GroupControl.getPropertyNames("GroupControl"));
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

    @Override
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return ((String) dataTypeMap.get(propName));
        } else {
            return super.getDataType(propName);
        }
    }

    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList();
        }
        superTypeList.add("GroupControl");
        superTypeList.add("HierarchyControl");
        superTypeList.add("SearchControl");
        superTypeList.add("PropertyControl");
        superTypeList.add("Control");
    }

    /**
     * Gets a list of any model objects which this model object, <b>GroupMembershipControl</b>, is
     * an extension of.
     * 
     * @return
     *         returned object is {@link ArrayList}
     */
    @Override
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
    @Override
    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    private static synchronized void setSubTypes() {
        if (subTypeList == null) {
            subTypeList = new HashSet();
        }
    }

    /**
     * Gets a set of any model objects which extend this model object, <b>GroupMembershipControl</b>
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
     * Returns this model object, <b>GroupMembershipControl</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
