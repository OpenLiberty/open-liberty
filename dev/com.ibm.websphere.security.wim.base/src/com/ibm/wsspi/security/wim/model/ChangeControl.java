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

/**
 * <p>Java class for ChangeControl complex type.
 *
 * <p> The ChangeControl object extends the {@link SearchControl} object and defines two properties: <b>checkPoint</b> and <b>changeTypes</b>.
 *
 * <p> The ChangeControl data object provides a client application with the ability to query federated repositories for
 * changed entities, such as new, modified, and deleted entities, from a specified checkpoint onwards.
 *
 * <p> A client application can use the ChangeControl data object to synchronize its internal cache or other repositories,
 * with the user registry, and provide a secure environment using the latest information in its cache or repositories.
 *
 * * <p> There could be one or more instances of <b>checkPoint</b> in a ChangeControl data object, depending on the number of
 * repositories involved in a search.
 *
 * <p>Below is a list of supported properties for {@link ChangeControl}.
 *
 * <ul>
 * <li><b>checkPoint</b>: defines the checkpoint for repositories configured in VMM. It is a list that contains
 * the repositoryId and the repositoryCheckPoint.</li>
 * <li><b>changeTypes</b>: represents the type of changes to return. It is a list of valid change types:
 * <ul>
 * <li><b>add</b>: for CHANGETYPE_ADD</li>
 * <li><b>delete</b>: for CHANGETYPE_DELETE</li>
 * <li><b>modify</b>: for CHANGETYPE_MODIFY</li>
 * <li><b>rename</b>: for CHANGETYPE_RENAME</li>
 * <li><b>*</b>: for CHANGETYPE_ALL</li>
 * </ul>
 * </li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link SearchControl} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = ChangeControl.TYPE_NAME, propOrder = {
                                                       "checkPoint",
                                                       "changeTypes"
})
public class ChangeControl extends SearchControl {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "ChangeControl";

    /** Property name constant for the <b>checkPoint</b> property. */
    private static final String PROP_CHECK_POINT = "checkPoint";

    /** Property name constant for the <b>changeTypes</b> property. */
    private static final String PROP_CHANGE_TYPES = "changeTypes";

    /**
     * The checkpoint for repositories configured in VMM. It is a list that contains
     * the repositoryId and the repositoryCheckPoint.
     */
    @XmlElement(name = PROP_CHECK_POINT)
    protected List<CheckPointType> checkPoint;

    /**
     * The type of changes to return. It is a list of valid change types:
     * <ul>
     * <li><b>add</b>: for CHANGETYPE_ADD</li>
     * <li><b>delete</b>: for CHANGETYPE_DELETE</li>
     * <li><b>modify</b>: for CHANGETYPE_MODIFY</li>
     * <li><b>rename</b>: for CHANGETYPE_RENAME</li>
     * <li><b>*</b>: for CHANGETYPE_ALL</li>
     * </ul>
     */
    @XmlElement(name = PROP_CHANGE_TYPES)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected List<String> changeTypes;

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

    /**
     * Gets the value of the <b>changeTypes</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>changeTypes</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getChangeTypes().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getChangeTypes() {
        if (changeTypes == null) {
            changeTypes = new ArrayList<String>();
        }
        return this.changeTypes;
    }

    /**
     * Returns true if the <b>changeTypes</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetChangeTypes() {
        return ((this.changeTypes != null) && (!this.changeTypes.isEmpty()));
    }

    /**
     * Resets the <b>changeTypes</b> property to null.
     */
    public void unsetChangeTypes() {
        this.changeTypes = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_CHECK_POINT)) {
            return getCheckPoint();
        }
        if (propName.equals(PROP_CHANGE_TYPES)) {
            return getChangeTypes();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_CHECK_POINT)) {
            return isSetCheckPoint();
        }
        if (propName.equals(PROP_CHANGE_TYPES)) {
            return isSetChangeTypes();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_CHECK_POINT)) {
            getCheckPoint().add((CheckPointType) value);
        }
        if (propName.equals(PROP_CHANGE_TYPES)) {
            getChangeTypes().add(((String) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_CHECK_POINT)) {
            unsetCheckPoint();
        }
        if (propName.equals(PROP_CHANGE_TYPES)) {
            unsetChangeTypes();
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
            names.add(PROP_CHANGE_TYPES);
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
        dataTypeMap.put(PROP_CHECK_POINT, CheckPointType.TYPE_NAME);
        dataTypeMap.put(PROP_CHANGE_TYPES, "String");
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
     * Set the list of super-types for this type.
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
     * Set the set of sub-types for this type.
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
