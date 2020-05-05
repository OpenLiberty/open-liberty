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
 * <p>Java class for OrgContainer complex type.
 *
 * <p> The OrgContainer object represents either and Organization or OrganizationalUnit, and
 * extends the {@link Party} object.
 *
 *
 * <p>Below is a list of supported properties for {@link OrgContainer}.
 *
 * <p>
 * <ul>
 * <li><b>o</b>: contains the names of an organization.</li>
 * <li><b>ou</b>: contains the names of an organizational unit.</li>
 * <li><b>dc</b>: a string holding one component, a label, of a DNS domain name.</li>
 * <li><b>cn</b>: contains names of an object.</li>
 * <li><b>telephoneNumber</b>: contains telephone numbers.</li>
 * <li><b>facsimileTelephoneNumber</b>: contains telephone numbers for facsimile terminals.</li>
 * <li><b>postalAddress</b>: contains addresses used by a Postal Service to perform services for the object.</li>
 * <li><b>l</b>: contains a short form of the 'localityName' property.</li>
 * <li><b>localityName</b>: contains names of a locality or place, such as a city, county, or other geographic region.</li>
 * <li><b>st</b>: contains a short form of the 'stateOrProvinceName' property.</li>
 * <li><b>stateOrProvinceName</b>: contains the full names of states or provinces.</li>
 * <li><b>street</b>: contains site information from a postal address (i.e., the street name, place, avenue, and the house number).</li>
 * <li><b>postalCode</b>: contains codes used by a Postal Service to identify postal service zones.</li>
 * <li><b>businessAddress</b>: contains a business address for this object.</li>
 * <li><b>description</b>: contains human-readable descriptive phrases about this object.</li>
 * <li><b>businessCategory</b>: describes the kinds of business performed by this object.</li>
 * <li><b>seeAlso</b>: contains distinguished names of objects that are related to this object.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link Party} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = OrgContainer.TYPE_NAME, propOrder = {
                                                      "o",
                                                      "ou",
                                                      "dc",
                                                      "cn",
                                                      "telephoneNumber",
                                                      "facsimileTelephoneNumber",
                                                      "postalAddress",
                                                      "l",
                                                      "localityName",
                                                      "st",
                                                      "stateOrProvinceName",
                                                      "street",
                                                      "postalCode",
                                                      "businessAddress",
                                                      "description",
                                                      "businessCategory",
                                                      "seeAlso"
})
public class OrgContainer extends Party {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "OrgContainer";

    /** Property name constant for the <b>o</b> property. */
    private static final String PROP_O = "o";

    /** Property name constant for the <b>ou</b> property. */
    private static final String PROP_OU = "ou";

    /** Property name constant for the <b>dc</b> property. */
    private static final String PROP_DC = "dc";

    /** Property name constant for the <b>cn</b> property. */
    private static final String PROP_CN = "cn";

    /** Property name constant for the <b>telephoneNumber</b> property. */
    private static final String PROP_TELEPHONE_NUMBER = "telephoneNumber";

    /** Property name constant for the <b>facsimileTelephoneNumber</b> property. */
    private static final String PROP_FACSIMILE_TELEPHONE_NUMBER = "facsimileTelephoneNumber";

    /** Property name constant for the <b>postalAddress</b> property. */
    private static final String PROP_POSTAL_ADDRESS = "postalAddress";

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

    /** Property name constant for the <b>postalCode</b> property. */
    private static final String PROP_POSTAL_CODE = "postalCode";

    /** Property name constant for the <b>businessAddress</b> property. */
    private static final String PROP_BUSINESS_ADDRESS = "businessAddress";

    /** Property name constant for the <b>description</b> property. */
    private static final String PROP_DESCRIPTION = "description";

    /** Property name constant for the <b>businessCategory</b> property. */
    private static final String PROP_BUSINESS_CATEGORY = "businessCategory";

    /** Property name constant for the <b>seeAlso</b> property. */
    private static final String PROP_SEE_ALSO = "seeAlso";

    /** Contains the names of an organization. */
    @XmlElement(name = PROP_O)
    protected String o;

    /** Contains the names of an organizational unit. */
    @XmlElement(name = PROP_OU)
    protected String ou;

    /** A string holding one component, a label, of a DNS domain name. */
    @XmlElement(name = PROP_DC)
    protected String dc;

    /** Contains names of an object. */
    @XmlElement(name = PROP_CN)
    protected String cn;

    /** Contains telephone numbers. */
    @XmlElement(name = PROP_TELEPHONE_NUMBER)
    protected List<String> telephoneNumber;

    /** Contains telephone numbers for facsimile terminals. */
    @XmlElement(name = PROP_FACSIMILE_TELEPHONE_NUMBER)
    protected List<String> facsimileTelephoneNumber;

    /** Contains addresses used by a Postal Service to perform services for the object. */
    @XmlElement(name = PROP_POSTAL_ADDRESS)
    protected List<String> postalAddress;

    /** Contains a short form of the 'localityName' property. */
    @XmlElement(name = PROP_L)
    protected List<String> l;

    /** Contains names of a locality or place, such as a city, county, or other geographic region. */
    @XmlElement(name = PROP_LOCALITY_NAME)
    protected List<String> localityName;

    /** Contains a short form of the 'stateOrProvinceName' property. */
    @XmlElement(name = PROP_ST)
    protected List<String> st;

    /** Contains the full names of states or provinces. */
    @XmlElement(name = PROP_STATE_OR_PROVINCE_NAME)
    protected List<String> stateOrProvinceName;

    /** Contains site information from a postal address (i.e., the street name, place, avenue, and the house number). */
    @XmlElement(name = PROP_STREET)
    protected List<String> street;

    /** Contains codes used by a Postal Service to identify postal service zones. */
    @XmlElement(name = PROP_POSTAL_CODE)
    protected List<String> postalCode;

    /** Contains a business address for this object. */
    @XmlElement(name = PROP_BUSINESS_ADDRESS)
    protected AddressType businessAddress;

    /** Contains human-readable descriptive phrases about this object. */
    @XmlElement(name = PROP_DESCRIPTION)
    protected List<String> description;

    /** Describes the kinds of business performed by this object. Example: "software development" */
    @XmlElement(name = PROP_BUSINESS_CATEGORY)
    protected List<String> businessCategory;

    /** Contains distinguished names of objects that are related to this object. */
    @XmlElement(name = PROP_SEE_ALSO)
    protected List<String> seeAlso;

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
        MULTI_VALUED_PROPERTIES.add(PROP_TELEPHONE_NUMBER);
        MULTI_VALUED_PROPERTIES.add(PROP_FACSIMILE_TELEPHONE_NUMBER);
        MULTI_VALUED_PROPERTIES.add(PROP_POSTAL_ADDRESS);
        MULTI_VALUED_PROPERTIES.add(PROP_L);
        MULTI_VALUED_PROPERTIES.add(PROP_LOCALITY_NAME);
        MULTI_VALUED_PROPERTIES.add(PROP_ST);
        MULTI_VALUED_PROPERTIES.add(PROP_STATE_OR_PROVINCE_NAME);
        MULTI_VALUED_PROPERTIES.add(PROP_STREET);
        MULTI_VALUED_PROPERTIES.add(PROP_POSTAL_CODE);
        MULTI_VALUED_PROPERTIES.add(PROP_DESCRIPTION);
        MULTI_VALUED_PROPERTIES.add(PROP_BUSINESS_CATEGORY);
        MULTI_VALUED_PROPERTIES.add(PROP_SEE_ALSO);
    }

    /**
     * Gets the value of the <b>o</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getO() {
        return o;
    }

    /**
     * Sets the value of the <b>o</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setO(String value) {
        this.o = value;
    }

    /**
     * Check if the <b>o</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetO() {
        return (this.o != null);
    }

    /**
     * Gets the value of the <b>ou</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getOu() {
        return ou;
    }

    /**
     * Sets the value of the <b>ou</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setOu(String value) {
        this.ou = value;
    }

    /**
     * Check if the <b>ou</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetOu() {
        return (this.ou != null);
    }

    /**
     * Gets the value of the <b>dc</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getDc() {
        return dc;
    }

    /**
     * Sets the value of the <b>dc</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setDc(String value) {
        this.dc = value;
    }

    /**
     * Check if the <b>dc</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */

    public boolean isSetDc() {
        return (this.dc != null);
    }

    /**
     * Gets the value of the <b>cn</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getCn() {
        return cn;
    }

    /**
     * Sets the value of the <b>cn</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setCn(String value) {
        this.cn = value;
    }

    /**
     * Check if the <b>cn</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetCn() {
        return (this.cn != null);
    }

    /**
     * Gets the value of the <b>telephoneNumber</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>telephoneNumber</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getTelephoneNumber().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getTelephoneNumber() {
        if (telephoneNumber == null) {
            telephoneNumber = new ArrayList<String>();
        }
        return this.telephoneNumber;
    }

    /**
     * Check if the <b>telephoneNumber</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetTelephoneNumber() {
        return ((this.telephoneNumber != null) && (!this.telephoneNumber.isEmpty()));
    }

    /**
     * Unset the <b>telephoneNumber</b> property.
     */
    public void unsetTelephoneNumber() {
        this.telephoneNumber = null;
    }

    /**
     * Gets the value of the <b>facsimileTelephoneNumber</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>facsimileTelephoneNumber</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getFacsimileTelephoneNumber().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getFacsimileTelephoneNumber() {
        if (facsimileTelephoneNumber == null) {
            facsimileTelephoneNumber = new ArrayList<String>();
        }
        return this.facsimileTelephoneNumber;
    }

    /**
     * Check if the <b>facsimileTelephoneNumber</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetFacsimileTelephoneNumber() {
        return ((this.facsimileTelephoneNumber != null) && (!this.facsimileTelephoneNumber.isEmpty()));
    }

    /**
     * Unset the <b>facsimileTelephoneNumber</b> property.
     */
    public void unsetFacsimileTelephoneNumber() {
        this.facsimileTelephoneNumber = null;
    }

    /**
     * Gets the value of the <b>postalAddress</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>postalAddress</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getPostalAddress().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getPostalAddress() {
        if (postalAddress == null) {
            postalAddress = new ArrayList<String>();
        }
        return this.postalAddress;
    }

    /**
     * Check if the <b>postalAddress</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetPostalAddress() {
        return ((this.postalAddress != null) && (!this.postalAddress.isEmpty()));
    }

    /**
     * Unset the <b>postalAddress</b> property.
     */
    public void unsetPostalAddress() {
        this.postalAddress = null;
    }

    /**
     * Gets the value of the <b>l</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>l</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getL().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getL() {
        if (l == null) {
            l = new ArrayList<String>();
        }
        return this.l;
    }

    /**
     * Check if the <b>l</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetL() {
        return ((this.l != null) && (!this.l.isEmpty()));
    }

    /**
     * Unset the <b>l</b> property.
     */
    public void unsetL() {
        this.l = null;
    }

    /**
     * Gets the value of the <b>localityName</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>localityName</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getLocalityName().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getLocalityName() {
        if (localityName == null) {
            localityName = new ArrayList<String>();
        }
        return this.localityName;
    }

    /**
     * Check if the <b>localityName</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetLocalityName() {
        return ((this.localityName != null) && (!this.localityName.isEmpty()));
    }

    /**
     * Unset the <b>localityName</b> property.
     */
    public void unsetLocalityName() {
        this.localityName = null;
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
     * Check if the <b>st</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetSt() {
        return ((this.st != null) && (!this.st.isEmpty()));
    }

    /**
     * Unset the <b>st</b> property.
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
     * Check if the <b>stateOrProvinceName</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetStateOrProvinceName() {
        return ((this.stateOrProvinceName != null) && (!this.stateOrProvinceName.isEmpty()));
    }

    /**
     * Unset the <b>stateOrProvinceName</b> property.
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
     * Check if the <b>street</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetStreet() {
        return ((this.street != null) && (!this.street.isEmpty()));
    }

    /**
     * Unset the <b>street</b> property.
     */
    public void unsetStreet() {
        this.street = null;
    }

    /**
     * Gets the value of the <b>postalCode</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>postalCode</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getPostalCode().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getPostalCode() {
        if (postalCode == null) {
            postalCode = new ArrayList<String>();
        }
        return this.postalCode;
    }

    /**
     * Check if the <b>postalCode</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetPostalCode() {
        return ((this.postalCode != null) && (!this.postalCode.isEmpty()));
    }

    /**
     * Unset the <b>postalCode</b> property.
     */
    public void unsetPostalCode() {
        this.postalCode = null;
    }

    /**
     * Gets the value of the <b>businessAddress</b> property.
     *
     * @return
     *         possible object is {@link AddressType }
     */
    public AddressType getBusinessAddress() {
        return businessAddress;
    }

    /**
     * Sets the value of the <b>businessAddress</b> property.
     *
     * @param value
     *            allowed object is {@link AddressType }
     */
    public void setBusinessAddress(AddressType value) {
        this.businessAddress = value;
    }

    /**
     * Check if the <b>businessAddress</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetBusinessAddress() {
        return (this.businessAddress != null);
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
     * Check if the <b>description</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetDescription() {
        return ((this.description != null) && (!this.description.isEmpty()));
    }

    /**
     * Unset the <b>description</b> property.
     */
    public void unsetDescription() {
        this.description = null;
    }

    /**
     * Gets the value of the <b>businessCategory</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>businessCategory</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getBusinessCategory().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getBusinessCategory() {
        if (businessCategory == null) {
            businessCategory = new ArrayList<String>();
        }
        return this.businessCategory;
    }

    /**
     * Check if the <b>businessCategory</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetBusinessCategory() {
        return ((this.businessCategory != null) && (!this.businessCategory.isEmpty()));
    }

    /**
     * Unset the <b>businessCategory</b> property.
     */
    public void unsetBusinessCategory() {
        this.businessCategory = null;
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
     * Check if the <b>seeAlso</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetSeeAlso() {
        return ((this.seeAlso != null) && (!this.seeAlso.isEmpty()));
    }

    /**
     * Unset the <b>seeAlso</b> property.
     */
    public void unsetSeeAlso() {
        this.seeAlso = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_O)) {
            return getO();
        }
        if (propName.equals(PROP_OU)) {
            return getOu();
        }
        if (propName.equals(PROP_DC)) {
            return getDc();
        }
        if (propName.equals(PROP_CN)) {
            return getCn();
        }
        if (propName.equals(PROP_TELEPHONE_NUMBER)) {
            return getTelephoneNumber();
        }
        if (propName.equals(PROP_FACSIMILE_TELEPHONE_NUMBER)) {
            return getFacsimileTelephoneNumber();
        }
        if (propName.equals(PROP_POSTAL_ADDRESS)) {
            return getPostalAddress();
        }
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
        if (propName.equals(PROP_POSTAL_CODE)) {
            return getPostalCode();
        }
        if (propName.equals(PROP_BUSINESS_ADDRESS)) {
            return getBusinessAddress();
        }
        if (propName.equals(PROP_DESCRIPTION)) {
            return getDescription();
        }
        if (propName.equals(PROP_BUSINESS_CATEGORY)) {
            return getBusinessCategory();
        }
        if (propName.equals(PROP_SEE_ALSO)) {
            return getSeeAlso();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_O)) {
            return isSetO();
        }
        if (propName.equals(PROP_OU)) {
            return isSetOu();
        }
        if (propName.equals(PROP_DC)) {
            return isSetDc();
        }
        if (propName.equals(PROP_CN)) {
            return isSetCn();
        }
        if (propName.equals(PROP_TELEPHONE_NUMBER)) {
            return isSetTelephoneNumber();
        }
        if (propName.equals(PROP_FACSIMILE_TELEPHONE_NUMBER)) {
            return isSetFacsimileTelephoneNumber();
        }
        if (propName.equals(PROP_POSTAL_ADDRESS)) {
            return isSetPostalAddress();
        }
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
        if (propName.equals(PROP_POSTAL_CODE)) {
            return isSetPostalCode();
        }
        if (propName.equals(PROP_BUSINESS_ADDRESS)) {
            return isSetBusinessAddress();
        }
        if (propName.equals(PROP_DESCRIPTION)) {
            return isSetDescription();
        }
        if (propName.equals(PROP_BUSINESS_CATEGORY)) {
            return isSetBusinessCategory();
        }
        if (propName.equals(PROP_SEE_ALSO)) {
            return isSetSeeAlso();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_O)) {
            setO(((String) value));
        }
        if (propName.equals(PROP_OU)) {
            setOu(((String) value));
        }
        if (propName.equals(PROP_DC)) {
            setDc(((String) value));
        }
        if (propName.equals(PROP_CN)) {
            setCn(((String) value));
        }
        if (propName.equals(PROP_TELEPHONE_NUMBER)) {
            getTelephoneNumber().add(((String) value));
        }
        if (propName.equals(PROP_FACSIMILE_TELEPHONE_NUMBER)) {
            getFacsimileTelephoneNumber().add(((String) value));
        }
        if (propName.equals(PROP_POSTAL_ADDRESS)) {
            getPostalAddress().add(((String) value));
        }
        if (propName.equals(PROP_L)) {
            getL().add(((String) value));
        }
        if (propName.equals(PROP_LOCALITY_NAME)) {
            getLocalityName().add(((String) value));
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
        if (propName.equals(PROP_POSTAL_CODE)) {
            getPostalCode().add(((String) value));
        }
        if (propName.equals(PROP_BUSINESS_ADDRESS)) {
            setBusinessAddress(((AddressType) value));
        }
        if (propName.equals(PROP_DESCRIPTION)) {
            getDescription().add(((String) value));
        }
        if (propName.equals(PROP_BUSINESS_CATEGORY)) {
            getBusinessCategory().add(((String) value));
        }
        if (propName.equals(PROP_SEE_ALSO)) {
            getSeeAlso().add(((String) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_TELEPHONE_NUMBER)) {
            unsetTelephoneNumber();
        }
        if (propName.equals(PROP_FACSIMILE_TELEPHONE_NUMBER)) {
            unsetFacsimileTelephoneNumber();
        }
        if (propName.equals(PROP_POSTAL_ADDRESS)) {
            unsetPostalAddress();
        }
        if (propName.equals(PROP_L)) {
            unsetL();
        }
        if (propName.equals(PROP_LOCALITY_NAME)) {
            unsetLocalityName();
        }
        if (propName.equals(PROP_ST)) {
            unsetSt();
        }
        if (propName.equals(PROP_STATE_OR_PROVINCE_NAME)) {
            unsetStateOrProvinceName();
        }
        if (propName.equals(PROP_STREET)) {
            unsetStreet();
        }
        if (propName.equals(PROP_POSTAL_CODE)) {
            unsetPostalCode();
        }
        if (propName.equals(PROP_DESCRIPTION)) {
            unsetDescription();
        }
        if (propName.equals(PROP_BUSINESS_CATEGORY)) {
            unsetBusinessCategory();
        }
        if (propName.equals(PROP_SEE_ALSO)) {
            unsetSeeAlso();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Get a list of all property names available for this type.
     *
     * @param entityTypeName The type name.
     * @return The list of properties for this type.
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.add(PROP_O);
            names.add(PROP_OU);
            names.add(PROP_DC);
            names.add(PROP_CN);
            names.add(PROP_TELEPHONE_NUMBER);
            names.add(PROP_FACSIMILE_TELEPHONE_NUMBER);
            names.add(PROP_POSTAL_ADDRESS);
            names.add(PROP_L);
            names.add(PROP_LOCALITY_NAME);
            names.add(PROP_ST);
            names.add(PROP_STATE_OR_PROVINCE_NAME);
            names.add(PROP_STREET);
            names.add(PROP_POSTAL_CODE);
            names.add(PROP_BUSINESS_ADDRESS);
            names.add(PROP_DESCRIPTION);
            names.add(PROP_BUSINESS_CATEGORY);
            names.add(PROP_SEE_ALSO);
            names.addAll(Party.getPropertyNames(Party.TYPE_NAME));
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
        dataTypeMap.put(PROP_O, "String");
        dataTypeMap.put(PROP_OU, "String");
        dataTypeMap.put(PROP_DC, "String");
        dataTypeMap.put(PROP_CN, "String");
        dataTypeMap.put(PROP_TELEPHONE_NUMBER, "String");
        dataTypeMap.put(PROP_FACSIMILE_TELEPHONE_NUMBER, "String");
        dataTypeMap.put(PROP_POSTAL_ADDRESS, "String");
        dataTypeMap.put(PROP_L, "String");
        dataTypeMap.put(PROP_LOCALITY_NAME, "String");
        dataTypeMap.put(PROP_ST, "String");
        dataTypeMap.put(PROP_STATE_OR_PROVINCE_NAME, "String");
        dataTypeMap.put(PROP_STREET, "String");
        dataTypeMap.put(PROP_POSTAL_CODE, "String");
        dataTypeMap.put(PROP_BUSINESS_ADDRESS, AddressType.TYPE_NAME);
        dataTypeMap.put(PROP_DESCRIPTION, "String");
        dataTypeMap.put(PROP_BUSINESS_CATEGORY, "String");
        dataTypeMap.put(PROP_SEE_ALSO, "String");
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
     * Set the list of super-types for this type.
     */
    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList<String>();
        }
        superTypeList.add(Party.TYPE_NAME);
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
     * Set the set of sub-types for this type.
     */
    private static synchronized void setSubTypes() {
        if (subTypeSet == null) {
            subTypeSet = new HashSet<String>();
        }
    }

    /**
     * Get the set of sub-types of this type.
     *
     * @return The sub-types of this type.
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
