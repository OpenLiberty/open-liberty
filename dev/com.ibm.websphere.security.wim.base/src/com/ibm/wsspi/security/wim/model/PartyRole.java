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
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for PartyRole complex type.
 *
 * <p> The PartyRole object extends the RolePlayer object and defines a role.
 *
 * <p>Below is a list of supported properties for {@link Locality}.
 *
 * <ul>
 * <li><b>primaryRolePlayer</b>: a containment property which is used to link to the entity who is the primary
 * role player of the PartyRole.</li>
 * <li><b>relatedRolePlayers</b>: a containment property which is used to link to the other role players who
 * are related to the primary role player through this PartyRole.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link RolePlayer} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = PartyRole.TYPE_NAME, propOrder = {
                                                   "primaryRolePlayer",
                                                   "relatedRolePlayer"
})
public class PartyRole extends RolePlayer {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "PartyRole";

    /** Property name constant for the <b>primaryRolePlayer</b> property. */
    private static final String PROP_PRIMARY_ROLE_PLAYER = "primaryRolePlayer";

    /** Property name constant for the <b>relatedRolePlayer</b> property. */
    private static final String PROP_RELATED_ROLE_PLAYER = "relatedRolePlayer";

    /**
     * Used to link to the entity who is the primary role player of the PartyRole.
     */
    @XmlElement(name = PROP_PRIMARY_ROLE_PLAYER, required = true)
    protected RolePlayer primaryRolePlayer;

    /**
     * Used to link to the other role players who are related to the primary role player through this PartyRole.
     */
    @XmlElement(name = PROP_RELATED_ROLE_PLAYER)
    protected List<RolePlayer> relatedRolePlayer;

    /** The list of properties that comprise this type. */
    private static List<String> propertyNames = null;

    /** A mapping of property names to data types. */
    private static HashMap<String, String> dataTypeMap = null;

    /** A list of super-types of this type. */
    private static ArrayList<String> superTypeList = null;

    /** A set of sub-types of this type. */
    private static HashSet<String> subTypeSet = null;

    /** The set of multi-valued properties for this type. */
    private static final Set<String> MULTI_VALUED_PROPERTIES;

    static {
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();

        MULTI_VALUED_PROPERTIES = new HashSet<String>();
        MULTI_VALUED_PROPERTIES.add(PROP_RELATED_ROLE_PLAYER);
    }

    /**
     * Gets the value of the <b>primaryRolePlayer</b> property.
     *
     * @return
     *         possible object is {@link RolePlayer }
     */
    public RolePlayer getPrimaryRolePlayer() {
        return primaryRolePlayer;
    }

    /**
     * Sets the value of the <b>primaryRolePlayer</b> property.
     *
     * @param value
     *            allowed object is {@link RolePlayer }
     */
    public void setPrimaryRolePlayer(RolePlayer value) {
        this.primaryRolePlayer = value;
    }

    /**
     * Check if the <b>primaryRolePlayer</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetPrimaryRolePlayer() {
        return (this.primaryRolePlayer != null);
    }

    /**
     * Gets the value of the <b>relatedRolePlayer</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>relatedRolePlayer</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getRelatedRolePlayer().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link RolePlayer }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<RolePlayer> getRelatedRolePlayer() {
        if (relatedRolePlayer == null) {
            relatedRolePlayer = new ArrayList<RolePlayer>();
        }
        return this.relatedRolePlayer;
    }

    /**
     * Check if the <b>relatedRolePlayer</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetRelatedRolePlayer() {
        return ((this.relatedRolePlayer != null) && (!this.relatedRolePlayer.isEmpty()));
    }

    /**
     * Unset the <b>relatedRolePlayer</b> property.
     */
    public void unsetRelatedRolePlayer() {
        this.relatedRolePlayer = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_PRIMARY_ROLE_PLAYER)) {
            return getPrimaryRolePlayer();
        }
        if (propName.equals(PROP_RELATED_ROLE_PLAYER)) {
            return getRelatedRolePlayer();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_PRIMARY_ROLE_PLAYER)) {
            return isSetPrimaryRolePlayer();
        }
        if (propName.equals(PROP_RELATED_ROLE_PLAYER)) {
            return isSetRelatedRolePlayer();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_PRIMARY_ROLE_PLAYER)) {
            setPrimaryRolePlayer(((RolePlayer) value));
        }
        if (propName.equals(PROP_RELATED_ROLE_PLAYER)) {
            getRelatedRolePlayer().add(((RolePlayer) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_RELATED_ROLE_PLAYER)) {
            unsetRelatedRolePlayer();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Get the list of property names for this type.
     *
     * @param entityTypeName The entity type name.
     * @return The list of property names.
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            List<String> names = new ArrayList<String>();
            names.add(PROP_PRIMARY_ROLE_PLAYER);
            names.add(PROP_RELATED_ROLE_PLAYER);
            names.addAll(RolePlayer.getPropertyNames(RolePlayer.TYPE_NAME));
            propertyNames = Collections.unmodifiableList(names);
            return propertyNames;
        }
    }

    /**
     * Create the property name to data type mapping.
     */
    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap<String, String>();
        }
        dataTypeMap.put(PROP_PRIMARY_ROLE_PLAYER, RolePlayer.TYPE_NAME);
        dataTypeMap.put(PROP_RELATED_ROLE_PLAYER, RolePlayer.TYPE_NAME);
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
        superTypeList.add(RolePlayer.TYPE_NAME);
        superTypeList.add(Entity.TYPE_NAME);
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
     * Get the set of sub-types for this type.
     *
     * @return The set of sub-types.
     */
    public static HashSet<String> getSubTypes() {
        if (subTypeSet == null) {
            setSubTypes();
        }
        return subTypeSet;
    }

    @Override
    public boolean isMultiValuedProperty(String propName) {
        return MULTI_VALUED_PROPERTIES.contains(propName) || super.isMultiValuedProperty(propName);
    }
}
