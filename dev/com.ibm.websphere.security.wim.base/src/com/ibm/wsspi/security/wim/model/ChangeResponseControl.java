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
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for ChangeResponseControl complex type.
 *
 * <p> The ChangeResponseControl object extends the {@link SearchResponseControl} object and defines a single
 * property: <b>checkPoint</b>.
 *
 * <p> The ChangeReponseControl data object is returned to the client application with changed entities
 * as the result of a search for changed entities using the ChangeControl data object. This response
 * control also returns the checkpoint to be used during a subsequent search for changed entities.
 *
 * <p>Below is a list of supported properties for {@link ChangeResponseControl}.
 *
 * <ul>
 * <li><b>checkPoint</b>: defines the checkpoint for repositories configured in VMM. It is a list
 * that contains the repositoryId and the repositoryCheckPoint.</li>
 * </ul>
 *
 * <p> There could be one or more instances of <b>checkPoint</b> in a ChangeResponseControl data object, depending on
 * the number of repositories involved in a search.
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link SearchResponseControl} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = ChangeResponseControl.TYPE_NAME, propOrder = {
                                                               "checkPoint"
})
public class ChangeResponseControl extends SearchResponseControl {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "ChangeResponseControl";

    /** Property name constant for the <b>checkPoint</b> property. */
    private static final String PROP_CHECK_POINT = "checkPoint";

    /**
     * The checkpoint for repositories configured in VMM. It is a list that contains the
     * repositoryId and the repositoryCheckPoint.
     */
    @XmlElement(name = PROP_CHECK_POINT)
    protected List<CheckPointType> checkPoint;

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
     * Gets the value of the <b>checkPoint</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>checkPoint</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getCheckPoint().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link CheckPointType }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<CheckPointType> getCheckPoint() {
        if (checkPoint == null) {
            checkPoint = new ArrayList<CheckPointType>();
        }
        return this.checkPoint;
    }

    /**
     * Returns true if the <b>checkPoint</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetCheckPoint() {
        return ((this.checkPoint != null) && (!this.checkPoint.isEmpty()));
    }

    /**
     * Resets the <b>checkPoint</b> property to null.
     */
    public void unsetCheckPoint() {
        this.checkPoint = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_CHECK_POINT)) {
            return getCheckPoint();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_CHECK_POINT)) {
            return isSetCheckPoint();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_CHECK_POINT)) {
            getCheckPoint().add(((CheckPointType) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_CHECK_POINT)) {
            unsetCheckPoint();
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
            names.add(PROP_CHECK_POINT);
            names.addAll(SearchResponseControl.getPropertyNames(SearchResponseControl.TYPE_NAME));
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
        dataTypeMap.put(PROP_CHECK_POINT, CheckPointType.TYPE_NAME);
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
        superTypeList.add(SearchResponseControl.TYPE_NAME);
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
