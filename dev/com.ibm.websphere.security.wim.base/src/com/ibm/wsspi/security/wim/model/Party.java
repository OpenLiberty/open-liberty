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
//

package com.ibm.wsspi.security.wim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for Party complex type.
 *
 * <p> The Party object extends the {@link RolePlayer} object, and represents a Party which is extended by {@link Person},
 * {@link Group}, {@link OrgContainer} and {@link LoginAccount}.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Party.TYPE_NAME)
@XmlSeeAlso({
              Group.class,
              OrgContainer.class,
              Person.class,
              LoginAccount.class
})
public class Party extends RolePlayer {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "Party";

    /** The list of mandatory properties. */
    private static List<String> mandatoryProperties = null;

    /** The list of transient properties. */
    private static List<String> transientProperties = null;

    /** The list of properties that comprise this type. */
    private static List<String> propertyNames = null;

    /** A mapping of property names to data types. */
    private static HashMap<String, String> dataTypeMap = null;

    /** A list of super-types of this type. */
    private static ArrayList<String> superTypeList = null;

    /** A set of sub-types of this type. */
    private static HashSet<String> subTypeSet = null;

    static {
        setMandatoryPropertyNames();
        setTransientPropertyNames();
        getTransientProperties();
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Set the list of mandatory properties.
     */
    private static synchronized void setMandatoryPropertyNames() {
        if (mandatoryProperties != null) {
            return;
        }
        mandatoryProperties = new ArrayList<String>();
    }

    /**
     * Set the list of transient properties.
     */
    private static synchronized void setTransientPropertyNames() {
        if (transientProperties != null) {
            return;
        }
        transientProperties = new ArrayList<String>();
        transientProperties.addAll(RolePlayer.getTransientProperties());
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
     * @return List of transient properties.
     */
    protected static List<String> getTransientProperties() {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        return Collections.unmodifiableList(transientProperties);
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
        subTypeSet.add(Group.TYPE_NAME);
        subTypeSet.add(OrgContainer.TYPE_NAME);
        subTypeSet.add(LoginAccount.TYPE_NAME);
        subTypeSet.add(Person.TYPE_NAME);
        subTypeSet.add(PersonAccount.TYPE_NAME);
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
}
