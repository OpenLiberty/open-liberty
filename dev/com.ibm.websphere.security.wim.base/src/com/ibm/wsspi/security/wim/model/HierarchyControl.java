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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for HierarchyControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="HierarchyControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}SearchControl">
 * &lt;attribute name="level" type="{http://www.w3.org/2001/XMLSchema}int" default="1" />
 * &lt;attribute name="treeView" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The HierarchyControl object extends the SearchControl object.
 * 
 * <p> It is an abstract control, which means it is not directly used in the Root object. Instead,
 * its descendants: AncestorControl, DescendantControl, GroupMemberControl and GroupMembershipControl are used in
 * the Root object.
 * 
 * <p> HierarchyControl contains all of properties of SearchControl. Additionally, it contains the following
 * properties: <b>level</b> and <b>treeView</b>.
 * 
 * <p> The property <b>level</b> is used to indicate the level of the hierarchy to be returned. It has different meaning
 * in the different descendants of HierarchyControl. As examples, in DescendantControl, it indicates the level of
 * descendants. In GroupMembershipControl, level indicates the level of nested groups.
 * 
 * <p> The property <b>treeView</b> is used to indicate whether or not to return the hierarchy structure in the output
 * Root object. It also has different meaning in the different descendants of HierarchyControl. For example, in
 * GroupMembershipControl, if treeView is set to true, the output Root object will contain the tree structure
 * of the nested groups. If it is set to false, all groups (including immediate groups and nested groups) are
 * added to the groups properties of the entity in a flat structure. The default value for treeView is set to false.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HierarchyControl")
@XmlSeeAlso({
             DescendantControl.class,
             AncestorControl.class,
             GroupControl.class
})
@Trivial
public class HierarchyControl
                extends SearchControl
{

    @XmlAttribute(name = "level")
    protected Integer level;
    @XmlAttribute(name = "treeView")
    protected Boolean treeView;
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
     * Gets the value of the <b>level</b> property.
     * 
     * @return
     *         possible object is {@link Integer }
     * 
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
     * 
     */
    public void setLevel(int value) {
        this.level = value;
    }

    /**
     * Returns true if the <b>level</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */

    public boolean isSetLevel() {
        return (this.level != null);
    }

    /**
     * Resets the <b>level</b> property to null.
     * 
     */

    public void unsetLevel() {
        this.level = null;
    }

    /**
     * Gets the value of the <b>treeView</b> property.
     * 
     * @return
     *         possible object is {@link Boolean }
     * 
     */
    public boolean isTreeView() {
        if (treeView == null) {
            return false;
        } else {
            return treeView;
        }
    }

    /**
     * Sets the value of the <b>treeView</b> property.
     * 
     * @param value
     *            allowed object is {@link Boolean }
     * 
     */
    public void setTreeView(boolean value) {
        this.treeView = value;
    }

    /**
     * Returns true if the <b>treeView</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */

    public boolean isSetTreeView() {
        return (this.treeView != null);
    }

    /**
     * Resets the <b>treeView</b> property to null.
     * 
     */

    public void unsetTreeView() {
        this.treeView = null;
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
        if (propName.equals("level")) {
            return getLevel();
        }
        return super.get(propName);
    }

    /*
     * Returns true if the requested property is set; false, otherwise.
     * 
     * @return
     * returned object is {@link boolean }
     */

    @Override
    public boolean isSet(String propName) {
        if (propName.equals("level")) {
            return isSetLevel();
        }
        if (propName.equals("treeView")) {
            return isSetTreeView();
        }
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
        if (propName.equals("level")) {
            setLevel(((Integer) value));
        }
        if (propName.equals("treeView")) {
            setTreeView(((Boolean) value));
        }
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
        if (propName.equals("level")) {
            unsetLevel();
        }
        if (propName.equals("treeView")) {
            unsetTreeView();
        }
        super.unset(propName);
    }

    /**
     * Gets the name of this model object, <b>HierarchyControl</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String getTypeName() {
        return "HierarchyControl";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>HierarchyControl</b>
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
                names.add("level");
                names.add("treeView");
                names.addAll(SearchControl.getPropertyNames("SearchControl"));
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("level", "Integer");
        dataTypeMap.put("treeView", "Boolean");
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
        superTypeList.add("SearchControl");
        superTypeList.add("PropertyControl");
        superTypeList.add("Control");
    }

    /**
     * Gets a list of any model objects which this model object, <b>HierarchyControl</b>, is
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
        subTypeList.add("DescendantControl");
        subTypeList.add("GroupMemberControl");
        subTypeList.add("GroupMembershipControl");
        subTypeList.add("AncestorControl");
        subTypeList.add("GroupControl");
    }

    /**
     * Gets a set of any model objects which extend this model object, <b>HierarchyControl</b>
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
     * Returns this model object, <b>HierarchyControl</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
