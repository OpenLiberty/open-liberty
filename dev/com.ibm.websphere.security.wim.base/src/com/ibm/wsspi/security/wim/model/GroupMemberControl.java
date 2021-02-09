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
 * <p>Java class for GroupMemberControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GroupMemberControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}GroupControl">
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The GroupMemberControl object extends the abstract GroupControl object.
 * 
 * <p> The GroupControl object contains the following properties that are inherited by GroupMemberControl:
 * <b>level</b>, <b>properties</b>, <b>searchBases</b>, <b>countLimit</b>, <b>timeLimit</b>, <b>expression</b>,
 * <b>treeView</b>, and <b>modifyMode</b>.
 * 
 * <p> GroupMemberControl is used in the input Root object of both the get() and update() APIs.
 * In the get() API, it is used for requesting members of this group. If it is added to the input Root object,
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
 * <p> Since GroupMemberControl is also extended from SearchControl, it is possible to specify the property <b>expression</b>
 * and other search properties like <b>countLimit</b>, and <b>timeLimit</b> in GroupMemberControl to only return those members
 * which satisfy the search criteria. For example, the property <b>expression</b> with value: @xsi:type='Person' will
 * only return the members which are of the Person entity type.
 * 
 * <ul>
 * <li><b>treeView</b>: used for indicating whether the hierarchy of the nested members should be kept
 * in the output Root object or not. If it is set to true, hierarchy relationship of the immediate members and
 * nested members of different levels are kept in the Root object. If it is set to false, all members are put
 * in a flat structure by pointing to the same members property of the group.</li>
 * </ul>
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GroupMemberControl")
@Trivial
public class GroupMemberControl
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
     * Gets the name of this model object, <b>GroupMemberControl</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String getTypeName() {
        return "GroupMemberControl";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>GroupMemberControl</b>
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
     * Gets a list of any model objects which this model object, <b>GroupMemberControl</b>, is
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
     * Gets a set of any model objects which extend this model object, <b>GroupMemberControl</b>
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
     * Returns this model object, <b>GroupMemberControl</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
