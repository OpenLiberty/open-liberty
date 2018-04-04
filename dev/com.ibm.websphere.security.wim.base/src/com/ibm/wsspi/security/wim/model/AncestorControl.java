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
 * <p>Java class for AncestorControl complex type.
 *
 * <p>The AncestorControl object extends the {@link HierarchyControl} object.
 *
 * <p>AncestorControl is used in the input Root object of the get() API to request ancestors of the entity.
 * If AncestorControl is added to the Root object in the get() operation, this means the ancestors of the entities
 * under the Root object will be returned in the returning Root object.
 *
 * <p>Besides indicating whether or not to return ancestors, AncestorControl can also be used for specifying the
 * properties returned for ancestors as well as the level of ancestor to be returned.
 *
 * <ul>
 * <li> 0 - return all ancestors</li>
 * <li> 1 - return only the parent</li>
 * <li> 2 - return the parent and grandparent</li>
 * <li> Any level greater than 2 will return that specified number of ancestors</li>
 * </ul>
 *
 * <p>Since AncestorControl is also extended from {@link AncestorControl}, it is possible to specify other search properties such as
 * <b>countLimit</b>, and <b>timeLimit</b> in AncestorControl to only return the ancestors which satisfies the search criteria.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = AncestorControl.TYPE_NAME)
public class AncestorControl extends HierarchyControl {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "AncestorControl";

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
     * Create a list of super-types for this type.
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
     * Create a set of sub-types for this type.
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
