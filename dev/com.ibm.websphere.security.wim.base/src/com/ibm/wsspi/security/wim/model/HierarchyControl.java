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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for HierarchyControl complex type.
 *
 * <p> The HierarchyControl object extends the {@link SearchControl} object.
 *
 * <p> It is an abstract control, which means it is not directly used in the {@link Root} object. Instead,
 * its descendants: {@link AncestorControl}, {@link DescendantControl}, {@link GroupMemberControl} and {@link GroupMembershipControl}
 * are used in the {@link Root} object.
 *
 * <p> HierarchyControl contains all of properties of {@link SearchControl}. Additionally, it contains the following
 * properties: <b>level</b> and <b>treeView</b>.
 *
 * <p> The property <b>level</b> is used to indicate the level of the hierarchy to be returned. It has different meaning
 * in the different descendants of HierarchyControl. As examples, in {@link DescendantControl}, it indicates the level of
 * descendants. In {@link GroupMembershipControl}, it indicates the level of nested groups.
 *
 * <p> The property <b>treeView</b> is used to indicate whether or not to return the hierarchy structure in the output
 * {@link Root} object. It also has different meaning in the different descendants of HierarchyControl. For example, in
 * {@link GroupMembershipControl}, if treeView is set to true, the output {@link Root} object will contain the tree structure
 * of the nested groups. If it is set to false, all groups (including immediate groups and nested groups) are
 * added to the groups properties of the entity in a flat structure. The default value for treeView is set to false.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = HierarchyControl.TYPE_NAME)
@XmlSeeAlso({
              DescendantControl.class,
              AncestorControl.class,
              GroupControl.class
})
public class HierarchyControl extends SearchControl {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "HierarchyControl";

    /** Property name constant for the <b>level</b> property. */
    private static final String PROP_LEVEL = "level";

    /** Property name constant for the <b>treeView</b> property. */
    private static final String PROP_TREE_VIEW = "treeView";

    /**
     * The level of the hierarchy to be returned. It has different meaning
     * in the different descendants of HierarchyControl. As examples, in {@link DescendantControl}, it indicates the level of
     * descendants. In {@link GroupMembershipControl}, it indicates the level of nested groups.
     */
    @XmlAttribute(name = PROP_LEVEL)
    protected Integer level;

    /**
     * Whether or not to return the hierarchy structure in the output
     * {@link Root} object. It also has different meaning in the different descendants of HierarchyControl. For example, in
     * {@link GroupMembershipControl}, if treeView is set to true, the output {@link Root} object will contain the tree structure
     * of the nested groups. If it is set to false, all groups (including immediate groups and nested groups) are
     * added to the groups properties of the entity in a flat structure. The default value for treeView is set to false.
     */
    @XmlAttribute(name = PROP_TREE_VIEW)
    protected Boolean treeView;

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
     * Gets the value of the <b>treeView</b> property.
     *
     * @return
     *         possible object is {@link Boolean }
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
     */
    public void setTreeView(boolean value) {
        this.treeView = value;
    }

    /**
     * Returns true if the <b>treeView</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetTreeView() {
        return (this.treeView != null);
    }

    /**
     * Resets the <b>treeView</b> property to null.
     */
    public void unsetTreeView() {
        this.treeView = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_LEVEL)) {
            return getLevel();
        }
        if (propName.equals(PROP_TREE_VIEW)) {
            return isTreeView();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_LEVEL)) {
            return isSetLevel();
        }
        if (propName.equals(PROP_TREE_VIEW)) {
            return isSetTreeView();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_LEVEL)) {
            setLevel(((Integer) value));
        }
        if (propName.equals(PROP_TREE_VIEW)) {
            setTreeView(((Boolean) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_LEVEL)) {
            unsetLevel();
        }
        if (propName.equals(PROP_TREE_VIEW)) {
            unsetTreeView();
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
            names.add(PROP_TREE_VIEW);
            names.addAll(SearchControl.getPropertyNames(SearchControl.TYPE_NAME));
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
        dataTypeMap.put(PROP_TREE_VIEW, "Boolean");
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
        subTypeSet.add(DescendantControl.TYPE_NAME);
        subTypeSet.add(GroupMemberControl.TYPE_NAME);
        subTypeSet.add(GroupMembershipControl.TYPE_NAME);
        subTypeSet.add(AncestorControl.TYPE_NAME);
        subTypeSet.add(GroupControl.TYPE_NAME);
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
