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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * 
 * <p>Java class for AddressType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AddressType">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="nickName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}street" maxOccurs="unbounded"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}city"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}stateOrProvinceName"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}postalCode"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}countryName"/>
 * &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The AddressType object is used to describe the physical location of an entity, and has
 * the following property names:
 * 
 * <ul>
 * <li><b>nickName</b>: an alias name for the physical location of an entity. For example: myHomeAddress </li>
 * <li><b>street</b>: the physical address of the object to which the entry corresponds,
 * such as an address for package delivery. For example: 12345 West Lynn Street </li>
 * <li><b>city</b>: the physical city of the object to which the entry corresponds. For example, Austin</li>
 * <li><b>stateOrProvinceName</b>: the full name of a state or province. For example, Texas</li>
 * <li><b>postalCode</b>: the corresponding post office code of the entity. For example, 78758</li>
 * <li><b>countryName</b>: the name of the country associated with the entity. For example. United States</li>
 * </ul>
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AddressType", propOrder = {
                                            "nickName",
                                            "street",
                                            "city",
                                            "stateOrProvinceName",
                                            "postalCode",
                                            "countryName"
})
@Trivial
public class AddressType {

    @XmlElement(required = true)
    protected String nickName;
    @XmlElement(required = true)
    protected List<String> street;
    @XmlElement(required = true)
    protected String city;
    @XmlElement(required = true)
    protected String stateOrProvinceName;
    @XmlElement(required = true)
    protected String postalCode;
    @XmlElement(required = true)
    protected String countryName;
    private static List propertyNames = null;
    private static HashMap dataTypeMap = null;
    private static ArrayList superTypeList = null;
    private static HashSet subTypeList = null;

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
     * 
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * Sets the value of the <b>nickName</b> property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setNickName(String value) {
        this.nickName = value;
    }

    /**
     * Returns true if the <b>nickName</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
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
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the street property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
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
     * Returns true if the <b>street</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */
    public boolean isSetStreet() {
        return ((this.street != null) && (!this.street.isEmpty()));
    }

    /**
     * Resets the <b>street</b> property to null.
     * 
     */
    public void unsetStreet() {
        this.street = null;
    }

    /**
     * Gets the value of the <b>city</b> property.
     * 
     * @return
     *         returned object is {@link String }
     * 
     */
    public String getCity() {
        return city;
    }

    /**
     * Sets the value of the <b>city</b> property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setCity(String value) {
        this.city = value;
    }

    /**
     * Returns true if the <b>city</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */

    public boolean isSetCity() {
        return (this.city != null);
    }

    /**
     * Gets the value of the <b>stateOrProvinceName</b> property.
     * 
     * @return
     *         returned object is {@link String }
     * 
     */
    public String getStateOrProvinceName() {
        return stateOrProvinceName;
    }

    /**
     * Sets the value of the <b>stateOrProvinceName</b> property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setStateOrProvinceName(String value) {
        this.stateOrProvinceName = value;
    }

    /**
     * Returns true if the <b>stateOrProvinceName</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */

    public boolean isSetStateOrProvinceName() {
        return (this.stateOrProvinceName != null);
    }

    /**
     * Gets the value of the <b>postalCode</b> property.
     * 
     * @return
     *         returned object is {@link String }
     * 
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * Sets the value of the <b>postalCode</b> property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setPostalCode(String value) {
        this.postalCode = value;
    }

    /**
     * Returns a true if the <b>postCode</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean}
     * 
     */
    public boolean isSetPostalCode() {
        return (this.postalCode != null);
    }

    /**
     * Gets the value of the <b>countryName</b> property.
     * 
     * @return
     *         returned object is {@link String }
     * 
     */
    public String getCountryName() {
        return countryName;
    }

    /**
     * Sets the value of the <b>countryName</b> property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setCountryName(String value) {
        this.countryName = value;
    }

    /**
     * Returns a true if the <b>countryName</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean}
     * 
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
     * 
     */

    public Object get(String propName) {
        if (propName.equals("nickName")) {
            return getNickName();
        }
        if (propName.equals("street")) {
            return getStreet();
        }
        if (propName.equals("city")) {
            return getCity();
        }
        if (propName.equals("stateOrProvinceName")) {
            return getStateOrProvinceName();
        }
        if (propName.equals("postalCode")) {
            return getPostalCode();
        }
        if (propName.equals("countryName")) {
            return getCountryName();
        }
        return null;
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */

    public boolean isSet(String propName) {
        if (propName.equals("nickName")) {
            return isSetNickName();
        }
        if (propName.equals("street")) {
            return isSetStreet();
        }
        if (propName.equals("city")) {
            return isSetCity();
        }
        if (propName.equals("stateOrProvinceName")) {
            return isSetStateOrProvinceName();
        }
        if (propName.equals("postalCode")) {
            return isSetPostalCode();
        }
        if (propName.equals("countryName")) {
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
     * 
     */
    public void set(String propName, Object value) {
        if (propName.equals("nickName")) {
            setNickName(((String) value));
        }
        if (propName.equals("street")) {
            getStreet().add(((String) value));
        }
        if (propName.equals("city")) {
            setCity(((String) value));
        }
        if (propName.equals("stateOrProvinceName")) {
            setStateOrProvinceName(((String) value));
        }
        if (propName.equals("postalCode")) {
            setPostalCode(((String) value));
        }
        if (propName.equals("countryName")) {
            setCountryName(((String) value));
        }
    }

    /**
     * Sets the value of provided property to null.
     * 
     * @param propName
     *            allowed object is {@link String}
     * 
     */

    public void unset(String propName) {
        if (propName.equals("street")) {
            unsetStreet();
        }
    }

    /**
     * Gets the name of this model object, <b>AddressType</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    public String getTypeName() {
        return "AddressType";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>AddressType</b>
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
                names.add("nickName");
                names.add("street");
                names.add("city");
                names.add("stateOrProvinceName");
                names.add("postalCode");
                names.add("countryName");
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("nickName", "String");
        dataTypeMap.put("street", "String");
        dataTypeMap.put("city", "String");
        dataTypeMap.put("stateOrProvinceName", "String");
        dataTypeMap.put("postalCode", "String");
        dataTypeMap.put("countryName", "String");
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
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return ((String) dataTypeMap.get(propName));
        } else {
            return null;
        }
    }

    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList();
        }
    }

    /**
     * Gets a list of any model objects which this model object, <b>AddressType</b>, is
     * an extension of.
     * 
     * @return
     *         returned object is {@link ArrayList}
     */
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

    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    private static synchronized void setSubTypes() {
        if (subTypeList == null) {
            subTypeList = new HashSet();
        }
    }

    /**
     * Gets a set of any model objects which extend this model object, <b>AddressType</b>
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
     * Returns this model object, <b>AddressType</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
