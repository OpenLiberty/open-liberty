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
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for Locality complex type.
 *
 * <p> The Locality represents a geographic area.
 *
 * <p>Below is a list of supported properties for {@link Locality}.
 *
 * <ul>
 * <li><b>l</b>: a short form for the <b>localityName</b>.</li>
 * <li><b>localityName</b>: contains the name of a locality, such as a city, county or other geographic region.</li>
 * <li><b>st</b>: a short form for <b>stateOrProvinceName</b>.</li>
 * <li><b>stateOrProvinceName</b>: contains the full name of a state or province (stateOrProvinceName).</li>
 * <li><b>street</b>: contains the physical address of the object to which the entry corresponds, such as an address for package delivery.</li>
 * <li><b>seeAlso</b>: contains distinguished names of objects that are related to this Locality.</li>
 * <li><b>description</b>: describes this object.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link GeographicLocation} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Locality.TYPE_NAME, propOrder = {
                                                  "l",
                                                  "localityName",
                                                  "st",
                                                  "stateOrProvinceName",
                                                  "street",
                                                  "seeAlso",
                                                  "description"
})
public class Locality extends GeographicLocation {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "Locality";

    /** Property name constant for the <b>l</b> property. */
    private static final String PROP_L = "l";

    /** Property name constant for the <b>localityName</b> property. */
    private static final String PROP_LOCALITY_NAME = "localityName";

    /** Property name constant for the <b>st</b> property. */
    private static final String PROP_ST = "st";

    /** Property name constant for the <b>stateOrProvinceName</b> property. */
    private static final String PROP_STATE_OR_PROVINCE_NAME = "stateOrProvinceName";

    /** Property name constant for the <b>street</b> property. */
    private static final String PROP_STREET = "street";

    /** Property name constant for the <b>seeAlso</b> property. */
    private static final String PROP_SEE_ALSO = "seeAlso";

    /** Property name constant for the <b>description</b> property. */
    private static final String PROP_DESCRIPTION = "description";

    /**
     * A short form for the <b>localityName</b>.
     */
    @XmlElement(name = PROP_L)
    protected String l;

    /**
     * The name of a locality, such as a city, county or other geographic region.
     */
    @XmlElement(name = PROP_LOCALITY_NAME)
    protected String localityName;

    /**
     * A short form for <b>stateOrProvinceName</b>.
     */
    @XmlElement(name = PROP_ST)
    protected List<String> st;

    /**
     * The full name of a state or province (stateOrProvinceName).
     */
    @XmlElement(name = PROP_STATE_OR_PROVINCE_NAME)
    protected List<String> stateOrProvinceName;

    /**
     * The physical address of the object to which the entry corresponds, such as an address for package delivery.
     */
    @XmlElement(name = PROP_STREET)
    protected List<String> street;

    /** Distinguished names of objects that are related to this Locality. */
    @XmlElement(name = PROP_SEE_ALSO)
    protected List<String> seeAlso;

    /**
     * Describes this object.
     */
    @XmlElement(name = PROP_DESCRIPTION)
    protected List<String> description;

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
        MULTI_VALUED_PROPERTIES.add(PROP_ST);
        MULTI_VALUED_PROPERTIES.add(PROP_STATE_OR_PROVINCE_NAME);
        MULTI_VALUED_PROPERTIES.add(PROP_STREET);
        MULTI_VALUED_PROPERTIES.add(PROP_SEE_ALSO);
        MULTI_VALUED_PROPERTIES.add(PROP_DESCRIPTION);
    }

    /**
     * Gets the value of the <b>l</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getL() {
        return l;
    }

    /**
     * Sets the value of the <b>l</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setL(String value) {
        this.l = value;
    }

    /**
     * Returns a true if the <b>l</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetL() {
        return (this.l != null);
    }

    /**
     * Gets the value of the <b>localityName</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getLocalityName() {
        return localityName;
    }

    /**
     * Sets the value of the <b>localityName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setLocalityName(String value) {
        this.localityName = value;
    }

    /**
     * Returns a true if the <b>localityName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetLocalityName() {
        return (this.localityName != null);
    }

    /**
     * Gets the value of the <b>st</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>st</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getSt().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getSt() {
        if (st == null) {
            st = new ArrayList<String>();
        }
        return this.st;
    }

    /**
     * Returns a true if the <b>st</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetSt() {
        return ((this.st != null) && (!this.st.isEmpty()));
    }

    /**
     * Resets the value of the <b>st</b> property to null
     */
    public void unsetSt() {
        this.st = null;
    }

    /**
     * Gets the value of the <b>stateOrProvinceName</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>stateOrProvinceName</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getStateOrProvinceName().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getStateOrProvinceName() {
        if (stateOrProvinceName == null) {
            stateOrProvinceName = new ArrayList<String>();
        }
        return this.stateOrProvinceName;
    }

    /**
     * Returns a true if the <b>stateOrProvinceName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetStateOrProvinceName() {
        return ((this.stateOrProvinceName != null) && (!this.stateOrProvinceName.isEmpty()));
    }

    /**
     * Resets the value of the <b>stateOrProvinceName</b> property to null
     */
    public void unsetStateOrProvinceName() {
        this.stateOrProvinceName = null;
    }

    /**
     * Gets the value of the <b>street</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>street</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getStreet().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getStreet() {
        if (street == null) {
            street = new ArrayList<String>();
        }
        return this.street;
    }

    /**
     * Returns a true if the <b>street</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetStreet() {
        return ((this.street != null) && (!this.street.isEmpty()));
    }

    /**
     * Resets the value of the <b>street</b> property to null
     */
    public void unsetStreet() {
        this.street = null;
    }

    /**
     * Gets the value of the <b>seeAlso</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>seeAlso</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getSeeAlso().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getSeeAlso() {
        if (seeAlso == null) {
            seeAlso = new ArrayList<String>();
        }
        return this.seeAlso;
    }

    /**
     * Returns a true if the <b>seeAlso</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetSeeAlso() {
        return ((this.seeAlso != null) && (!this.seeAlso.isEmpty()));
    }

    /**
     * Resets the value of the <b>seeAlso</b> property to null
     */
    public void unsetSeeAlso() {
        this.seeAlso = null;
    }

    /**
     * Gets the value of the <b>description</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>description</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getDescription().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getDescription() {
        if (description == null) {
            description = new ArrayList<String>();
        }
        return this.description;
    }

    /**
     * Returns a true if the <b>description</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetDescription() {
        return ((this.description != null) && (!this.description.isEmpty()));
    }

    /**
     * Resets the value of the <b>description</b> property to null
     */
    public void unsetDescription() {
        this.description = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_L)) {
            return getL();
        }
        if (propName.equals(PROP_LOCALITY_NAME)) {
            return getLocalityName();
        }
        if (propName.equals(PROP_ST)) {
            return getSt();
        }
        if (propName.equals(PROP_STATE_OR_PROVINCE_NAME)) {
            return getStateOrProvinceName();
        }
        if (propName.equals(PROP_STREET)) {
            return getStreet();
        }
        if (propName.equals(PROP_SEE_ALSO)) {
            return getSeeAlso();
        }
        if (propName.equals(PROP_DESCRIPTION)) {
            return getDescription();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_L)) {
            return isSetL();
        }
        if (propName.equals(PROP_LOCALITY_NAME)) {
            return isSetLocalityName();
        }
        if (propName.equals(PROP_ST)) {
            return isSetSt();
        }
        if (propName.equals(PROP_STATE_OR_PROVINCE_NAME)) {
            return isSetStateOrProvinceName();
        }
        if (propName.equals(PROP_STREET)) {
            return isSetStreet();
        }
        if (propName.equals(PROP_SEE_ALSO)) {
            return isSetSeeAlso();
        }
        if (propName.equals(PROP_DESCRIPTION)) {
            return isSetDescription();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_L)) {
            setL(((String) value));
        }
        if (propName.equals(PROP_LOCALITY_NAME)) {
            setLocalityName(((String) value));
        }
        if (propName.equals(PROP_ST)) {
            getSt().add(((String) value));
        }
        if (propName.equals(PROP_STATE_OR_PROVINCE_NAME)) {
            getStateOrProvinceName().add(((String) value));
        }
        if (propName.equals(PROP_STREET)) {
            getStreet().add(((String) value));
        }
        if (propName.equals(PROP_SEE_ALSO)) {
            getSeeAlso().add(((String) value));
        }
        if (propName.equals(PROP_DESCRIPTION)) {
            getDescription().add(((String) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_ST)) {
            unsetSt();
        }
        if (propName.equals(PROP_STATE_OR_PROVINCE_NAME)) {
            unsetStateOrProvinceName();
        }
        if (propName.equals(PROP_STREET)) {
            unsetStreet();
        }
        if (propName.equals(PROP_SEE_ALSO)) {
            unsetSeeAlso();
        }
        if (propName.equals(PROP_DESCRIPTION)) {
            unsetDescription();
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
            names.add(PROP_L);
            names.add(PROP_LOCALITY_NAME);
            names.add(PROP_ST);
            names.add(PROP_STATE_OR_PROVINCE_NAME);
            names.add(PROP_STREET);
            names.add(PROP_SEE_ALSO);
            names.add(PROP_DESCRIPTION);
            names.addAll(GeographicLocation.getPropertyNames(GeographicLocation.TYPE_NAME));
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
        dataTypeMap.put(PROP_L, "String");
        dataTypeMap.put(PROP_LOCALITY_NAME, "String");
        dataTypeMap.put(PROP_ST, "String");
        dataTypeMap.put(PROP_STATE_OR_PROVINCE_NAME, "String");
        dataTypeMap.put(PROP_STREET, "String");
        dataTypeMap.put(PROP_SEE_ALSO, "String");
        dataTypeMap.put(PROP_DESCRIPTION, "String");
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
        superTypeList.add(GeographicLocation.TYPE_NAME);
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
    public boolean isMultiValuedProperty(String propName) {
        return MULTI_VALUED_PROPERTIES.contains(propName) || super.isMultiValuedProperty(propName);
    }
}
