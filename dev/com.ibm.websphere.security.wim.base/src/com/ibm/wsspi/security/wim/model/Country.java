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
 * <p>Java class for Country complex type.
 *
 * <p> The Country object extends the {@link GeographicLocation} object, and represents information related to a country.
 *
 * <p>Below is a list of supported properties for {@link Country}.
 *
 * <ul>
 * <li><b>c</b>: short form for the <b>countryName</b> property.</li>
 * <li><b>countryName</b>: defines the name of the country.</li>
 * <li><b>description</b>: describes this object.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link GeographicLocation} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Country.TYPE_NAME, propOrder = {
                                                 "c",
                                                 "countryName",
                                                 "description"
})
public class Country extends GeographicLocation {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "Country";

    /** Property name constant for the <b>c</b> property. */
    private static final String PROP_C = "c";

    /** Property name constant for the <b>countryName</b> property. */
    private static final String PROP_COUNTRY_NAME = "countryName";

    /** Property name constant for the <b>description</b> property. */
    private static final String PROP_DESCRIPTION = "description";

    /**
     * Short form for the <b>countryName</b> property.
     */
    @XmlElement(name = PROP_C)
    protected String c;

    /**
     * The name of the country.
     */
    @XmlElement(name = PROP_COUNTRY_NAME)
    protected String countryName;

    /** Describes this object. */
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
        MULTI_VALUED_PROPERTIES.add(PROP_DESCRIPTION);
    }

    /**
     * Gets the value of the <b>c</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getC() {
        return c;
    }

    /**
     * Sets the value of the <b>c</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setC(String value) {
        this.c = value;
    }

    /**
     * Returns true if the <b>c</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetC() {
        return (this.c != null);
    }

    /**
     * Gets the value of the <b>countryName</b> property.
     *
     * @return
     *         possible object is {@link String }
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
     * Returns true if the <b>countryName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetCountryName() {
        return (this.countryName != null);
    }

    /**
     * Gets the value of the <b>description</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the description property.
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
     *         returned object is a {@link List}
     */
    public List<String> getDescription() {
        if (description == null) {
            description = new ArrayList<String>();
        }
        return this.description;
    }

    /**
     * Returns true if the <b>description</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetDescription() {
        return ((this.description != null) && (!this.description.isEmpty()));
    }

    /**
     * Resets the <b>description</b> property to null.
     */
    public void unsetDescription() {
        this.description = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_C)) {
            return getC();
        }
        if (propName.equals(PROP_COUNTRY_NAME)) {
            return getCountryName();
        }
        if (propName.equals(PROP_DESCRIPTION)) {
            return getDescription();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_C)) {
            return isSetC();
        }
        if (propName.equals(PROP_COUNTRY_NAME)) {
            return isSetCountryName();
        }
        if (propName.equals(PROP_DESCRIPTION)) {
            return isSetDescription();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_C)) {
            setC(((String) value));
        }
        if (propName.equals(PROP_COUNTRY_NAME)) {
            setCountryName(((String) value));
        }
        if (propName.equals(PROP_DESCRIPTION)) {
            getDescription().add(((String) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
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
            names.add(PROP_C);
            names.add(PROP_COUNTRY_NAME);
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
        dataTypeMap.put(PROP_C, "String");
        dataTypeMap.put(PROP_COUNTRY_NAME, "String");
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
     * Create the list of sub-types for this type.
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
