/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for PartyRole complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="PartyRole">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}RolePlayer">
 * &lt;sequence>
 * &lt;element name="primaryRolePlayer" type="{http://www.ibm.com/websphere/wim}RolePlayer"/>
 * &lt;element name="relatedRolePlayer" type="{http://www.ibm.com/websphere/wim}RolePlayer" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * <p> The PartyRole object extends the RolePlayer object and defines a role. It defines two properties:
 *
 * <ul>
 * <li><b>primaryRolePlayer</b>: a containment property which is used to link to the entity who is the primary
 * role player of the PartyRole.</li>
 *
 * <li><b>relatedRolePlayers</b>: a containment property which is used to link to the other role players who
 * are related to the primary role player through this PartyRole.</li>
 * </ul>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PartyRole", propOrder = {
                                           "primaryRolePlayer",
                                           "relatedRolePlayer"
})
@Trivial
public class PartyRole extends com.ibm.wsspi.security.wim.model.RolePlayer {
    private static final String PROP_PRIMARY_ROLE_PLAYER = "primaryRolePlayer";
    private static final String PROP_RELATED_ROLE_PLAYER = "relatedRolePlayer";

    @XmlElement(required = true)
    protected com.ibm.wsspi.security.wim.model.RolePlayer primaryRolePlayer;
    protected List<com.ibm.wsspi.security.wim.model.RolePlayer> relatedRolePlayer;

    private static List propertyNames = null;
    private static HashMap dataTypeMap = null;
    private static ArrayList superTypeList = null;
    private static HashSet subTypeList = null;

    /** The set of multi-valued properties for this entity type. */
    private static final Set<String> MULTI_VALUED_PROPERTIES;

    static {
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();

        MULTI_VALUED_PROPERTIES = new HashSet<String>();
        MULTI_VALUED_PROPERTIES.add(PROP_RELATED_ROLE_PLAYER);
    }

    /**
     * Gets the value of the primaryRolePlayer property.
     *
     * @return
     *         possible object is {@link com.ibm.wsspi.security.wim.model.RolePlayer }
     *
     */
    public com.ibm.wsspi.security.wim.model.RolePlayer getPrimaryRolePlayer() {
        return primaryRolePlayer;
    }

    /**
     * Sets the value of the primaryRolePlayer property.
     *
     * @param value
     *            allowed object is {@link com.ibm.wsspi.security.wim.model.RolePlayer }
     *
     */
    public void setPrimaryRolePlayer(com.ibm.wsspi.security.wim.model.RolePlayer value) {
        this.primaryRolePlayer = value;
    }

    public boolean isSetPrimaryRolePlayer() {
        return (this.primaryRolePlayer != null);
    }

    /**
     * Gets the value of the relatedRolePlayer property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the relatedRolePlayer property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getRelatedRolePlayer().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.RolePlayer }
     *
     *
     */
    public List<com.ibm.wsspi.security.wim.model.RolePlayer> getRelatedRolePlayer() {
        if (relatedRolePlayer == null) {
            relatedRolePlayer = new ArrayList<com.ibm.wsspi.security.wim.model.RolePlayer>();
        }
        return this.relatedRolePlayer;
    }

    public boolean isSetRelatedRolePlayer() {
        return ((this.relatedRolePlayer != null) && (!this.relatedRolePlayer.isEmpty()));
    }

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
            setPrimaryRolePlayer(((com.ibm.wsspi.security.wim.model.RolePlayer) value));
        }
        if (propName.equals(PROP_RELATED_ROLE_PLAYER)) {
            getRelatedRolePlayer().add(((com.ibm.wsspi.security.wim.model.RolePlayer) value));
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
        return "PartyRole";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add(PROP_PRIMARY_ROLE_PLAYER);
                names.add(PROP_RELATED_ROLE_PLAYER);
                names.addAll(com.ibm.wsspi.security.wim.model.RolePlayer.getPropertyNames("RolePlayer"));
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put(PROP_PRIMARY_ROLE_PLAYER, "RolePlayer");
        dataTypeMap.put(PROP_RELATED_ROLE_PLAYER, "RolePlayer");
    }

    @Override
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return ((String) dataTypeMap.get(propName));
        } else {
            return super.getDataType(propName);
        }
    }

    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList();
        }
        superTypeList.add("RolePlayer");
        superTypeList.add("Entity");
    }

    @Override
    public ArrayList getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    @Override
    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    private static synchronized void setSubTypes() {
        if (subTypeList == null) {
            subTypeList = new HashSet();
        }
    }

    public static HashSet getSubTypes() {
        if (subTypeList == null) {
            setSubTypes();
        }
        return subTypeList;
    }

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

    @Override
    public boolean isMultiValuedProperty(String propName) {
        return MULTI_VALUED_PROPERTIES.contains(propName) || super.isMultiValuedProperty(propName);
    }
}
