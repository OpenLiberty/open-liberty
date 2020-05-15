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
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for Locality complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Locality">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}GeographicLocation">
 * &lt;sequence>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}l" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}localityName" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}st" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}stateOrProvinceName" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}street" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}seeAlso" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}description" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * <p> The Locality object extends the GeographicLocation object and defines several properties: <b>l</b>, <b>localityName</b>, <b>st</b>,
 * <b>stateOrProvinceName</b>, <b>street</b> and <b>description</b>. This object represents a geographic area.
 *
 * <ul>
 * <li><b>l</b>: a short form for the <b>localityName</b>.</li>
 *
 * <li><b>localityName</b>: contains the name of a locality, such as a city, county or other geographic region.</li>
 *
 * <li><b>st</b>: a short form for <b>stateOrProvinceName</b>.</li>
 *
 * <li><b>stateOrProvinceName</b>: contains the full name of a state or province (stateOrProvinceName).</li>
 *
 * <li><b>street</b>: contains the physical address of the object to which the entry corresponds, such as an address for package delivery.</li>
 *
 * <li><b>description</b>: describes this object.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Locality", propOrder = {
                                          "l",
                                          "localityName",
                                          "st",
                                          "stateOrProvinceName",
                                          "street",
                                          "seeAlso",
                                          "description"
})
@Trivial
public class Locality extends GeographicLocation {
    private static final String PROP_L = "l";
    private static final String PROP_LOCALITY_NAME = "localityName";
    private static final String PROP_ST = "st";
    private static final String PROP_STATE_OR_PROVINCE_NAME = "stateOrProvinceName";
    private static final String PROP_STREET = "street";
    private static final String PROP_SEE_ALSO = "seeAlso";
    private static final String PROP_DESCRIPTION = "description";

    protected String l;
    protected String localityName;
    protected List<String> st;
    protected List<String> stateOrProvinceName;
    protected List<String> street;
    protected List<String> seeAlso;
    protected List<String> description;

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
     *
     */
    public String getL() {
        return l;
    }

    /**
     * Sets the value of the <b>l</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setL(String value) {
        this.l = value;
    }

    /**
     * Returns a true if the <b>l</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     *
     */

    public boolean isSetL() {
        return (this.l != null);
    }

    /**
     * Gets the value of the <b>localityName<b/> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getLocalityName() {
        return localityName;
    }

    /**
     * Sets the value of the <b>localityName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setLocalityName(String value) {
        this.localityName = value;
    }

    /**
     * Returns a true if the <b>localityName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     *
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
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the st property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getSt().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     *
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
     *
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
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the stateOrProvinceName property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getStateOrProvinceName().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     *
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
     *
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
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the street property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getStreet().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     *
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
     *
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
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the seeAlso property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getSeeAlso().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     *
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
     *
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
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the description property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getDescription().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     *
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
     *
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

    /**
     * Gets the value of the requested property
     *
     * @param propName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link Object}
     *
     */

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

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
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

    /**
     * Sets the value of the provided property to the provided value.
     *
     * @param propName
     *            allowed object is {@link String}
     * @param value
     *            allowed object is {@link Object}
     *
     */

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

    /**
     * Sets the value of provided property to null.
     *
     * @param propName
     *            allowed object is {@link String}
     *
     */
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

    /**
     * Gets the name of this model object, <b>Locality</b>
     *
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String getTypeName() {
        return "Locality";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>Locality</b>
     *
     * @param entityTypeName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link List}
     */

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add(PROP_L);
                names.add(PROP_LOCALITY_NAME);
                names.add(PROP_ST);
                names.add(PROP_STATE_OR_PROVINCE_NAME);
                names.add(PROP_STREET);
                names.add(PROP_SEE_ALSO);
                names.add(PROP_DESCRIPTION);
                names.addAll(GeographicLocation.getPropertyNames("GeographicLocation"));
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put(PROP_L, "String");
        dataTypeMap.put(PROP_LOCALITY_NAME, "String");
        dataTypeMap.put(PROP_ST, "String");
        dataTypeMap.put(PROP_STATE_OR_PROVINCE_NAME, "String");
        dataTypeMap.put(PROP_STREET, "String");
        dataTypeMap.put(PROP_SEE_ALSO, "String");
        dataTypeMap.put(PROP_DESCRIPTION, "String");
    }

    /**
     * Gets the Java type of the value of the provided property. For example: String, List
     *
     * @param propName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link String}
     */

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
        superTypeList.add("GeographicLocation");
        superTypeList.add("Entity");
    }

    /**
     * Gets a list of any model objects which this model object, <b>Locality</b>, is
     * an extension of.
     *
     * @return
     *         returned object is {@link ArrayList}
     */

    @Override
    public ArrayList getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    /**
     * Returns a true if the provided model object is one that this
     * model object extends; false, otherwise.
     *
     * @param superTypeName
     *
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link boolean}
     */

    @Override
    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    private static synchronized void setSubTypes() {
        if (subTypeList == null) {
            subTypeList = new HashSet();
        }
    }

    /**
     * Gets a set of any model objects which extend this model object, <b>Locality</b>
     *
     * @return
     *         returned object is {@link HashSet}
     */

    public static HashSet getSubTypes() {
        if (subTypeList == null) {
            setSubTypes();
        }
        return subTypeList;
    }

    /**
     * Returns this model object, <b>Locality</b>, and its contents as a String
     *
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

    @Override
    public boolean isMultiValuedProperty(String propName) {
        return MULTI_VALUED_PROPERTIES.contains(propName) || super.isMultiValuedProperty(propName);
    }
}
