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
 *
 * <p>Java class for AddressType complex type.
 *
 * <p> The AddressType object is used to describe the physical location of an entity.
 *
 * <p>Below is a list of supported properties for {@link AddressType}.
 *
 * <ul>
 * <li><b>nickName</b>: an alias name for the physical location of an entity. For example: myHomeAddress </li>
 * <li><b>street</b>: the physical address of the object to which the entry corresponds,
 * such as an address for package delivery. For example: 12345 West Lynn Street </li>
 * <li><b>city</b>: the physical city of the object to which the entry corresponds. For example, Austin</li>
 * <li><b>stateOrProvinceName</b>: the full name of a state or province. For example, Texas</li>
 * <li><b>postalCode</b>: the corresponding post office code of the entity. For example, 78758</li>
 * <li><b>countryName</b>: the name of the country associated with the entity. For example, United States</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = AddressType.TYPE_NAME, propOrder = {
                                                     "nickName",
                                                     "street",
                                                     "city",
                                                     "stateOrProvinceName",
                                                     "postalCode",
                                                     "countryName"
})
public class AddressType {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "AddressType";

    /** Property name constant for the <b>nickName</b> property. */
    private static final String PROP_NICK_NAME = "nickName";

    /** Property name constant for the <b>street</b> property. */
    private static final String PROP_STREET = "street";

    /** Property name constant for the <b>city</b> property. */
    private static final String PROP_CITY = "city";

    /** Property name constant for the <b>stateOrProvinceName</b> property. */
    private static final String PROP_STATE_OR_PROVINCE_NAME = "stateOrProvinceName";

    /** Property name constant for the <b>postalCode</b> property. */
    private static final String PROP_POSTAL_CODE = "postalCode";

    /** Property name constant for the <b>countryName</b> property. */
    private static final String PROP_COUNTRY_NAME = "countryName";

    /**
     * An alias name for the physical location of an entity. For example: myHomeAddress.
     */
    @XmlElement(name = PROP_NICK_NAME, required = true)
    protected String nickName;

    /**
     * The physical address of the object to which the entry corresponds, such as an address for package delivery. For example: 12345 West Lynn Street
     */
    @XmlElement(name = PROP_STREET, required = true)
    protected List<String> street;

    /**
     * The physical city of the object to which the entry corresponds. For example, Austin
     */
    @XmlElement(name = PROP_CITY, required = true)
    protected String city;

    /**
     * The full name of a state or province. For example, Texas
     */
    @XmlElement(name = PROP_STATE_OR_PROVINCE_NAME, required = true)
    protected String stateOrProvinceName;

    /**
     * The corresponding post office code of the entity. For example, 78758
     */
    @XmlElement(name = PROP_POSTAL_CODE, required = true)
    protected String postalCode;

    /**
     * The name of the country associated with the entity. For example, United States
     */
    @XmlElement(name = PROP_COUNTRY_NAME, required = true)
    protected String countryName;

    /** The list of properties that comprise this entity. */
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
     * Gets the value of the <b>nickName</b> property.
     *
     * @return
     *         returned object is {@link String }
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * Sets the value of the <b>nickName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setNickName(String value) {
        this.nickName = value;
    }

    /**
     * Returns true if the <b>nickName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetNickName() {
        return (this.nickName != null);
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
     * Returns true if the <b>street</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetStreet() {
        return ((this.street != null) && (!this.street.isEmpty()));
    }

    /**
     * Resets the <b>street</b> property to null.
     */
    public void unsetStreet() {
        this.street = null;
    }

    /**
     * Gets the value of the <b>city</b> property.
     *
     * @return
     *         returned object is {@link String }
     */
    public String getCity() {
        return city;
    }

    /**
     * Sets the value of the <b>city</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setCity(String value) {
        this.city = value;
    }

    /**
     * Returns true if the <b>city</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetCity() {
        return (this.city != null);
    }

    /**
     * Gets the value of the <b>stateOrProvinceName</b> property.
     *
     * @return
     *         returned object is {@link String }
     */
    public String getStateOrProvinceName() {
        return stateOrProvinceName;
    }

    /**
     * Sets the value of the <b>stateOrProvinceName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setStateOrProvinceName(String value) {
        this.stateOrProvinceName = value;
    }

    /**
     * Returns true if the <b>stateOrProvinceName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetStateOrProvinceName() {
        return (this.stateOrProvinceName != null);
    }

    /**
     * Gets the value of the <b>postalCode</b> property.
     *
     * @return
     *         returned object is {@link String }
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * Sets the value of the <b>postalCode</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setPostalCode(String value) {
        this.postalCode = value;
    }

    /**
     * Returns a true if the <b>postCode</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetPostalCode() {
        return (this.postalCode != null);
    }

    /**
     * Gets the value of the <b>countryName</b> property.
     *
     * @return
     *         returned object is {@link String }
     */
    public String getCountryName() {
        return countryName;
    }

    /**
     * Sets the value of the <b>countryName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setCountryName(String value) {
        this.countryName = value;
    }

    /**
     * Returns a true if the <b>countryName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetCountryName() {
        return (this.countryName != null);
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
        if (propName.equals(PROP_NICK_NAME)) {
            return getNickName();
        }
        if (propName.equals(PROP_STREET)) {
            return getStreet();
        }
        if (propName.equals(PROP_CITY)) {
            return getCity();
        }
        if (propName.equals(PROP_STATE_OR_PROVINCE_NAME)) {
            return getStateOrProvinceName();
        }
        if (propName.equals(PROP_POSTAL_CODE)) {
            return getPostalCode();
        }
        if (propName.equals(PROP_COUNTRY_NAME)) {
            return getCountryName();
        }
        return null;
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @param propName
     *            The property name to check if set.
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSet(String propName) {
        if (propName.equals(PROP_NICK_NAME)) {
            return isSetNickName();
        }
        if (propName.equals(PROP_STREET)) {
            return isSetStreet();
        }
        if (propName.equals(PROP_CITY)) {
            return isSetCity();
        }
        if (propName.equals(PROP_STATE_OR_PROVINCE_NAME)) {
            return isSetStateOrProvinceName();
        }
        if (propName.equals(PROP_POSTAL_CODE)) {
            return isSetPostalCode();
        }
        if (propName.equals(PROP_COUNTRY_NAME)) {
            return isSetCountryName();
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
        if (propName.equals(PROP_NICK_NAME)) {
            setNickName(((String) value));
        }
        if (propName.equals(PROP_STREET)) {
            getStreet().add(((String) value));
        }
        if (propName.equals(PROP_CITY)) {
            setCity(((String) value));
        }
        if (propName.equals(PROP_STATE_OR_PROVINCE_NAME)) {
            setStateOrProvinceName(((String) value));
        }
        if (propName.equals(PROP_POSTAL_CODE)) {
            setPostalCode(((String) value));
        }
        if (propName.equals(PROP_COUNTRY_NAME)) {
            setCountryName(((String) value));
        }
    }

    /**
     * Sets the value of provided property to null.
     *
     * @param propName
     *            allowed object is {@link String}
     */
    public void unset(String propName) {
        if (propName.equals(PROP_STREET)) {
            unsetStreet();
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
     * @return
     *         returned object is {@link List}
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.add(PROP_NICK_NAME);
            names.add(PROP_STREET);
            names.add(PROP_CITY);
            names.add(PROP_STATE_OR_PROVINCE_NAME);
            names.add(PROP_POSTAL_CODE);
            names.add(PROP_COUNTRY_NAME);
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
        dataTypeMap.put(PROP_NICK_NAME, "String");
        dataTypeMap.put(PROP_STREET, "String");
        dataTypeMap.put(PROP_CITY, "String");
        dataTypeMap.put(PROP_STATE_OR_PROVINCE_NAME, "String");
        dataTypeMap.put(PROP_POSTAL_CODE, "String");
        dataTypeMap.put(PROP_COUNTRY_NAME, "String");
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
     * Gets a list of any types which this type is an extension of.
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
     * Gets a set of types which extend this type.
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
