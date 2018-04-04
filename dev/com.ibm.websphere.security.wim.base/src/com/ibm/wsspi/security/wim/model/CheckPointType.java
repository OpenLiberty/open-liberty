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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for CheckPointType complex type.
 *
 * <p>Below is a list of supported properties for {@link CheckPointType}.
 *
 * <ul>
 * <li><b>repositoryId</b>: defines the unique identifier for a given federated repository.</li>
 * <li><b>repositoryCheckPoint</b>: defines the repository checkpoint identifier.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = CheckPointType.TYPE_NAME, propOrder = {
                                                        "repositoryId",
                                                        "repositoryCheckPoint"
})
public class CheckPointType {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "CheckPointType";

    /** Property name constant for the <b>repositoryId</b> property. */
    private static final String PROP_REPOSITORY_ID = "repositoryId";

    /** Property name constant for the <b>repositoryCheckPoint</b> property. */
    private static final String PROP_REPOSITORY_CHECK_POINT = "repositoryCheckPoint";

    /**
     * The unique identifier for a given federated repository.
     */
    @XmlElement(name = PROP_REPOSITORY_ID, required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String repositoryId;

    /**
     * The repository checkpoint identifier.
     */
    @XmlElement(name = PROP_REPOSITORY_CHECK_POINT)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String repositoryCheckPoint;

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
     * Gets the value of the <b>repositoryId</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getRepositoryId() {
        return repositoryId;
    }

    /**
     * Sets the value of the <b>repositoryId</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setRepositoryId(String value) {
        this.repositoryId = value;
    }

    /**
     * Returns true if the <b>repositoryId</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetRepositoryId() {
        return (this.repositoryId != null);
    }

    /**
     * Gets the value of the <b>repositoryCheckPoint</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getRepositoryCheckPoint() {
        return repositoryCheckPoint;
    }

    /**
     * Sets the value of the <b>repositoryCheckPoint</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setRepositoryCheckPoint(String value) {
        this.repositoryCheckPoint = value;
    }

    /**
     * Returns true if the <b>repositoryCheckPoint</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetRepositoryCheckPoint() {
        return (this.repositoryCheckPoint != null);
    }

    /**
     * Gets the value of the requested property
     *
     * @param propName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link Object}
     */
    public Object get(String propName) {
        if (propName.equals(PROP_REPOSITORY_ID)) {
            return getRepositoryId();
        }
        if (propName.equals(PROP_REPOSITORY_CHECK_POINT)) {
            return getRepositoryCheckPoint();
        }
        return null;
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @param propName
     *            The name of the property to check if is set.
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSet(String propName) {
        if (propName.equals(PROP_REPOSITORY_ID)) {
            return isSetRepositoryId();
        }
        if (propName.equals(PROP_REPOSITORY_CHECK_POINT)) {
            return isSetRepositoryCheckPoint();
        }
        return false;
    }

    /**
     * Sets the value of the provided property to the provided value.
     *
     * @param propName
     *            allowed object is {@link String}
     * @param value
     *            allowed object is {@link Object}
     */
    public void set(String propName, Object value) {
        if (propName.equals(PROP_REPOSITORY_ID)) {
            setRepositoryId(((String) value));
        }
        if (propName.equals(PROP_REPOSITORY_CHECK_POINT)) {
            setRepositoryCheckPoint(((String) value));
        }
    }

    /**
     * Sets the value of provided property to null.
     *
     * @param propName
     *            allowed object is {@link String}
     */
    public void unset(String propName) {}

    /**
     * Gets the name of this model object, <b>CheckPointType</b>
     *
     * @return
     *         returned object is {@link String}
     */
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
            names.add(PROP_REPOSITORY_ID);
            names.add(PROP_REPOSITORY_CHECK_POINT);
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
        dataTypeMap.put(PROP_REPOSITORY_ID, "String");
        dataTypeMap.put(PROP_REPOSITORY_CHECK_POINT, "String");
    }

    /**
     * Gets the Java type of the value of the provided property. For example: String
     *
     * @param propName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link String}
     */
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return (dataTypeMap.get(propName));
        } else {
            return null;
        }
    }

    /**
     * Create the list of super-types for this entity type.
     */
    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList<String>();
        }
    }

    /**
     * Gets a list of any model objects which this model object, <b>CheckPointType</b>, is
     * an extension of.
     *
     * @return
     *         returned object is {@link ArrayList}
     */
    public ArrayList<String> getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    /**
     * Returns a true if the provided type is one that this type extends; false, otherwise.
     *
     * @param superTypeName
     *
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link boolean}
     */
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

    @Override
    public String toString() {
        return WIMTraceHelper.traceJaxb(this);
    }
}
