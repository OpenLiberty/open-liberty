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

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * Java class for EntitlementInfoType complex type.
 *
 * <p>Below is a list of supported properties for {@link EntitlementInfoType}.
 *
 * <ul>
 * <li><b>roles</b>: the roles the subject has for an entity.</li>
 * <li><b>entitlements</b>: the entitlements the subject has for an entity.</li>
 * <li><b>entitlementCheckResult</b>: whether the entitlement check passed.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = EntitlementInfoType.TYPE_NAME, propOrder = {
                                                             "roles",
                                                             "entitlements",
                                                             "entitlementCheckResult"
})
public class EntitlementInfoType {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "EntitlementInfoType";

    /** Property name constant for the <b>roles</b> property. */
    private static final String PROP_ROLES = "roles";

    /** Property name constant for the <b>entitlements</b> property. */
    private static final String PROP_ENTITLEMENTS = "entitlements";

    /** Property name constant for the <b>entitlementCheckResult</b> property. */
    private static final String PROP_ENTITLEMENT_CHECK_RESULT = "entitlementCheckResult";

    /** The roles the subject has for an entity */
    @XmlElement(name = PROP_ROLES)
    protected List<String> roles;

    /** The entitlements the subject has for an entity. */
    @XmlElement(name = PROP_ENTITLEMENTS)
    protected List<EntitlementType> entitlements;

    /** Whether the entitlement check passed. */
    @XmlElement(name = PROP_ENTITLEMENT_CHECK_RESULT)
    protected boolean entitlementCheckResult;

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
     * Gets the value of the <b>roles</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the roles property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getRoles().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getRoles() {
        if (roles == null) {
            roles = new ArrayList<String>();
        }
        return this.roles;
    }

    /**
     * Returns true if the <b>roles</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetRoles() {
        return ((this.roles != null) && (!this.roles.isEmpty()));
    }

    /**
     * Resets the <b>roles</b> property to null.
     */
    public void unsetRoles() {
        this.roles = null;
    }

    /**
     * Gets the value of the <b>entitlements</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the entitlements property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getEntitlements().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link EntitlementType }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<EntitlementType> getEntitlements() {
        if (entitlements == null) {
            entitlements = new ArrayList<EntitlementType>();
        }
        return this.entitlements;
    }

    /**
     * Returns true if the <b>entitlements</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetEntitlements() {
        return ((this.entitlements != null) && (!this.entitlements.isEmpty()));
    }

    /**
     * Resets the <b>entitlements</b> property to null.
     */
    public void unsetEntitlements() {
        this.entitlements = null;
    }

    /**
     * Gets the value of the <b>entitlementCheckResult</b> property.
     *
     * @return returned object is {@link boolean}
     */
    public boolean isEntitlementCheckResult() {
        return entitlementCheckResult;
    }

    /**
     * Sets the value of the <b>entitlementCheckResult</b> property.
     *
     * @param value
     *            The value to set.
     */
    public void setEntitlementCheckResult(boolean value) {
        this.entitlementCheckResult = value;
    }

    /**
     * Returns true if the <b>entitlementCheckResult</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetEntitlementCheckResult() {
        return true;
    }

    /**
     * Gets the value of the requested property
     *
     * @param propName
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link Object}
     */
    public Object get(String propName) {
        if (propName.equals(PROP_ROLES)) {
            return getRoles();
        }
        if (propName.equals(PROP_ENTITLEMENTS)) {
            return getEntitlements();
        }
        if (propName.equals(PROP_ENTITLEMENT_CHECK_RESULT)) {
            return getEntitlements();
        }
        return null;
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @param propName
     *            The property name to check if is set.
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSet(String propName) {
        if (propName.equals(PROP_ROLES)) {
            return isSetRoles();
        }
        if (propName.equals(PROP_ENTITLEMENTS)) {
            return isSetEntitlements();
        }
        if (propName.equals(PROP_ENTITLEMENT_CHECK_RESULT)) {
            return isSetEntitlementCheckResult();
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
        if (propName.equals(PROP_ROLES)) {
            getRoles().add(((String) value));
        }
        if (propName.equals(PROP_ENTITLEMENTS)) {
            getEntitlements().add(((com.ibm.wsspi.security.wim.model.EntitlementType) value));
        }
        if (propName.equals(PROP_ENTITLEMENT_CHECK_RESULT)) {
            setEntitlementCheckResult(((Boolean) value));
        }
    }

    /**
     * Sets the value of provided property to null.
     *
     * @param propName
     *            allowed object is {@link String}
     */
    public void unset(String propName) {
        if (propName.equals(PROP_ROLES)) {
            unsetRoles();
        }
        if (propName.equals(PROP_ENTITLEMENTS)) {
            unsetEntitlements();
        }
    }

    /**
     * Gets the name of this type.
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
            names.add(PROP_ROLES);
            names.add(PROP_ENTITLEMENTS);
            names.add(PROP_ENTITLEMENT_CHECK_RESULT);
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
        dataTypeMap.put(PROP_ROLES, "String");
        dataTypeMap.put(PROP_ENTITLEMENTS, EntitlementType.TYPE_NAME);
        dataTypeMap.put(PROP_ENTITLEMENT_CHECK_RESULT, "boolean");
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
     * Create the list of super-types for this type.
     */
    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList<String>();
        }
    }

    /**
     * Gets a list of any model objects which this type is an extension of.
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
