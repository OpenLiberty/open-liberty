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
 * <p>Java class for DescendantControl complex type.
 *
 * <p> The DescendantControl object extends the {@link HierarchyControl} object.
 *
 * <p> It is used in the input {@link Root} object of the get() API to request descendants of the entity. If the DescendantControl
 * object is added to the {@link Root} object in a get() API call, this means the descendants of the entities under the {@link Root} object
 * will be returned in the returning {@link Root} object.
 *
 * <p> Besides indicating whether or not to return descendants, DescendantControl can also be used for specifying the
 * properties to be returned for descendants as well as the level of descendants to be returned.
 *
 * <p>Below is a list of supported properties for {@link ChangeControl}.
 *
 * <ul>
 * <li><b>level</b>
 * <ul>
 * <li><b>0</b>: return all descendants</li>
 * <li><b>1</b>: return only the children</li>
 * <li><b>greater than 1</b>: return the specified number of descendants. For example, a level set to 2 will return both the children
 * and the granchildren.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link HierarchyControl} and its
 * super-classes are supported.
 *
 * <p>Since DescendantControl is also extended from {@link SearchControl}, it is possible to specify other search properties like
 * <b>countLimit</b>, and <b>timeLimit</b> in DescendantControl to only return the descendants which satisfies the search criteria.
 * For example, adding the property <b>expression</b> with value: @xsi:type='Person' to only return the descendants which are
 * of the {@link Person} entity type.
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = DescendantControl.TYPE_NAME)
public class DescendantControl extends HierarchyControl {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "DescendantControl";

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
    public Object get(String propName) {
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
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
            names.addAll(HierarchyControl.getPropertyNames(HierarchyControl.TYPE_NAME));
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
