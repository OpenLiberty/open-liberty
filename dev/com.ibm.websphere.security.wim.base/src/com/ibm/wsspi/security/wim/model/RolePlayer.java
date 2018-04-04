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
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for RolePlayer complex type.
 *
 * <p> The RolePlayer object extends from the {@link Entity} object.
 *
 * <p>Below is a list of supported properties for {@link Locality}.
 *
 * <ul>
 * <li><b>partyroles</b>: a containment property which is used to link the party roles the role player is
 * playing. <b>partyRoles</b> may contain multiple {@link PartyRole} objects since a role player can play multiple roles.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link Entity} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = RolePlayer.TYPE_NAME, propOrder = {
                                                    "partyRoles"
})
@XmlSeeAlso({
              PartyRole.class,
              Party.class
})
public class RolePlayer extends Entity {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "RolePlayer";

    /** Property name constant for the <b>partyRoles</b> property. */
    private static final String PROP_PARTY_ROLES = "partyRoles";

    /**
     * Used to link the party roles the role player is
     * playing. <b>partyRoles</b> may contain multiple {@link PartyRole} objects since a role player can play multiple roles.
     */
    @XmlElement(name = PROP_PARTY_ROLES)
    protected List<PartyRole> partyRoles;

    /** A list of mandatory properties. */
    private static List<String> mandatoryProperties = null;

    /** A list of transient properties. */
    private static List<String> transientProperties = null;

    /** The list of properties that comprise this type. */
    private static List<String> propertyNames = null;

    /** A mapping of property names to data types. */
    private static HashMap<String, String> dataTypeMap = null;

    /** A list of super-types of this type. */
    private static ArrayList<String> superTypeList = null;

    /** A set of sub-types of this type. */
    private static HashSet<String> subTypeSet = null;

    /** The set of multi-valued properties for this entity type. */
    private static final Set<String> MULTI_VALUED_PROPERTIES;

    static {
        setMandatoryPropertyNames();
        setTransientPropertyNames();
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();

        MULTI_VALUED_PROPERTIES = new HashSet<String>();
        MULTI_VALUED_PROPERTIES.add(PROP_PARTY_ROLES);
    }

    /**
     * Gets the value of the <b>partyRoles</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>partyRoles</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getPartyRoles().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link PartyRole }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<PartyRole> getPartyRoles() {
        if (partyRoles == null) {
            partyRoles = new ArrayList<PartyRole>();
        }
        return this.partyRoles;
    }

    /**
     * Returns true if the <b>partyRoles</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetPartyRoles() {
        return ((this.partyRoles != null) && (!this.partyRoles.isEmpty()));
    }

    /**
     * Resets the <b>partyRoles</b> property to null.
     */
    public void unsetPartyRoles() {
        this.partyRoles = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_PARTY_ROLES)) {
            return getPartyRoles();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_PARTY_ROLES)) {
            return isSetPartyRoles();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_PARTY_ROLES)) {
            getPartyRoles().add(((PartyRole) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_PARTY_ROLES)) {
            unsetPartyRoles();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Set the list of mandatory property names.
     */
    private static synchronized void setMandatoryPropertyNames() {
        if (mandatoryProperties != null) {
            return;
        }
        mandatoryProperties = new ArrayList<String>();
    }

    /**
     * Set the list of transient property names.
     */
    private static synchronized void setTransientPropertyNames() {
        if (transientProperties != null) {
            return;
        }
        transientProperties = new ArrayList<String>();
        transientProperties.addAll(Entity.getTransientProperties());
    }

    @Override
    public boolean isMandatory(String propName) {
        if (mandatoryProperties == null) {
            setMandatoryPropertyNames();
        }
        if (mandatoryProperties.contains(propName)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isPersistentProperty(String propName) {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        if (transientProperties.contains(propName)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Get the list of transient properties.
     *
     * @return The list of transient properties.
     */
    protected static List<String> getTransientProperties() {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        return transientProperties;
    }

    /**
     * Gets a list of all supported properties for this type.
     *
     * @param entityTypeName
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link List}
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.add(PROP_PARTY_ROLES);
            names.addAll(Entity.getPropertyNames(Entity.TYPE_NAME));
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
        dataTypeMap.put(PROP_PARTY_ROLES, PartyRole.TYPE_NAME);
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
        subTypeSet.add(Group.TYPE_NAME);
        subTypeSet.add(Party.TYPE_NAME);
        subTypeSet.add(PartyRole.TYPE_NAME);
        subTypeSet.add(OrgContainer.TYPE_NAME);
        subTypeSet.add(LoginAccount.TYPE_NAME);
        subTypeSet.add(Person.TYPE_NAME);
        subTypeSet.add(PersonAccount.TYPE_NAME);
    }

    /**
     * Get the set of sub-types for this type.
     *
     * @return the set of sub-types.
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
