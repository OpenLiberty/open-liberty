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
 * <p>Java class for DeleteControl complex type.
 *
 * <p> The DeleteControl object extends the {@link Control} object.
 *
 * <p> DeleteControl is used for specifying whether or not to delete descendants if the
 * entity to be deleted has descendants.
 *
 * <p>Below is a list of supported properties for {@link DeleteControl}.
 *
 * <ul>
 * <li><b>deleteDescendants</b>: indicates whether or not delete the descendants of the entity if it has
 * descendants.
 * <ul>
 * <li><b>true</b>: the entity and all of its descendants will be deleted. This is the default value.</li>
 * <li><b>false</b>: an exception will be thrown if the entity has any descendants.</li>
 * </ul>
 * </li>
 * <li><b>returnDeleted</b>: indicates whether or not to return the entities which are actually deleted in
 * the output {@link Root} object. The default value is false, which means no output {@link Root} object is returned.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link Control} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = DeleteControl.TYPE_NAME)
public class DeleteControl extends Control {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "DeleteControl";

    /** Property name constant for the <b>deleteDescendants</b> property. */
    private static final String PROP_DELETE_DESCENDANTS = "deleteDescendants";

    /** Property name constant for the <b>returnDeleted</b> property. */
    private static final String PROP_RETURN_DELETED = "returnDeleted";

    /**
     * Whether or not delete the descendants of the entity if it has
     * descendants.
     */
    @XmlAttribute(name = PROP_DELETE_DESCENDANTS)
    protected Boolean deleteDescendants;

    /**
     * Whether or not to return the entities which are actually deleted in
     * the output {@link Root} object. The default value is false, which means no output {@link Root} object is returned.
     */
    @XmlAttribute(name = PROP_RETURN_DELETED)
    protected Boolean returnDeleted;

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
     * Gets the value of the <b>deleteDescendants</b> property.
     *
     * @return
     *         possible object is {@link Boolean }
     */
    public boolean isDeleteDescendants() {
        if (deleteDescendants == null) {
            return false;
        } else {
            return deleteDescendants;
        }
    }

    /**
     * Sets the value of the <b>deleteDescendants</b> property.
     *
     * @param value
     *            allowed object is {@link Boolean }
     */
    public void setDeleteDescendants(boolean value) {
        this.deleteDescendants = value;
    }

    /**
     * Returns true if the <b>deleteDescendants</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetDeleteDescendants() {
        return (this.deleteDescendants != null);
    }

    /**
     * Resets the <b>deleteDescendants</b> property to null.
     */
    public void unsetDeleteDescendants() {
        this.deleteDescendants = null;
    }

    /**
     * Gets the value of the <b>returnDeleted</b> property.
     *
     * @return
     *         possible object is {@link Boolean }
     */
    public boolean isReturnDeleted() {
        if (returnDeleted == null) {
            return false;
        } else {
            return returnDeleted;
        }
    }

    /**
     * Sets the value of the <b>returnDeleted</b> property.
     *
     * @param value
     *            allowed object is {@link Boolean }
     */
    public void setReturnDeleted(boolean value) {
        this.returnDeleted = value;
    }

    /**
     * Returns true if the <b>returnDeleted</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetReturnDeleted() {
        return (this.returnDeleted != null);
    }

    /**
     * Resets the <b>returnDeleted</b> property to null.
     */
    public void unsetReturnDeleted() {
        this.returnDeleted = null;
    }

    @Override
    public Object get(String propName) {
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_DELETE_DESCENDANTS)) {
            return isSetDeleteDescendants();
        }
        if (propName.equals(PROP_RETURN_DELETED)) {
            return isSetReturnDeleted();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_DELETE_DESCENDANTS)) {
            setDeleteDescendants(((Boolean) value));
        }
        if (propName.equals(PROP_RETURN_DELETED)) {
            setReturnDeleted(((Boolean) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_DELETE_DESCENDANTS)) {
            unsetDeleteDescendants();
        }
        if (propName.equals(PROP_RETURN_DELETED)) {
            unsetReturnDeleted();
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
            names.add(PROP_DELETE_DESCENDANTS);
            names.add(PROP_RETURN_DELETED);
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
        dataTypeMap.put(PROP_DELETE_DESCENDANTS, "Boolean");
        dataTypeMap.put(PROP_RETURN_DELETED, "Boolean");
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
     * Create the list of sub-types for this type.
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
