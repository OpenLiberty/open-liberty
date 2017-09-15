/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for OrgContainer complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="OrgContainer">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}Party">
 * &lt;sequence>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}o" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}ou" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}dc" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}cn" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}telephoneNumber" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}facsimileTelephoneNumber" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}postalAddress" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}l" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}localityName" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}st" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}stateOrProvinceName" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}street" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}postalCode" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}businessAddress" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}description" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}businessCategory" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}seeAlso" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The OrgContainer object represents either and Organization or OrganizationalUnit, and
 * extends the Party object.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "OrgContainer", propOrder = {
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
public class OrgContainer
                extends Party
{

    protected String o;
    protected String ou;
    protected String dc;
    protected String cn;
    protected List<String> telephoneNumber;
    protected List<String> facsimileTelephoneNumber;
    protected List<String> postalAddress;
    protected List<String> l;
    protected List<String> localityName;
    protected List<String> st;
    protected List<String> stateOrProvinceName;
    protected List<String> street;
    protected List<String> postalCode;
    protected AddressType businessAddress;
    protected List<String> description;
    protected List<String> businessCategory;
    protected List<String> seeAlso;
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
     * Gets the value of the o property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getO() {
        return o;
    }

    /**
     * Sets the value of the o property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setO(String value) {
        this.o = value;
    }

    public boolean isSetO() {
        return (this.o != null);
    }

    /**
     * Gets the value of the ou property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getOu() {
        return ou;
    }

    /**
     * Sets the value of the ou property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setOu(String value) {
        this.ou = value;
    }

    public boolean isSetOu() {
        return (this.ou != null);
    }

    /**
     * Gets the value of the dc property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getDc() {
        return dc;
    }

    /**
     * Sets the value of the dc property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setDc(String value) {
        this.dc = value;
    }

    public boolean isSetDc() {
        return (this.dc != null);
    }

    /**
     * Gets the value of the cn property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getCn() {
        return cn;
    }

    /**
     * Sets the value of the cn property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setCn(String value) {
        this.cn = value;
    }

    public boolean isSetCn() {
        return (this.cn != null);
    }

    /**
     * Gets the value of the telephoneNumber property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the telephoneNumber property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getTelephoneNumber().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getTelephoneNumber() {
        if (telephoneNumber == null) {
            telephoneNumber = new ArrayList<String>();
        }
        return this.telephoneNumber;
    }

    public boolean isSetTelephoneNumber() {
        return ((this.telephoneNumber != null) && (!this.telephoneNumber.isEmpty()));
    }

    public void unsetTelephoneNumber() {
        this.telephoneNumber = null;
    }

    /**
     * Gets the value of the facsimileTelephoneNumber property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the facsimileTelephoneNumber property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getFacsimileTelephoneNumber().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getFacsimileTelephoneNumber() {
        if (facsimileTelephoneNumber == null) {
            facsimileTelephoneNumber = new ArrayList<String>();
        }
        return this.facsimileTelephoneNumber;
    }

    public boolean isSetFacsimileTelephoneNumber() {
        return ((this.facsimileTelephoneNumber != null) && (!this.facsimileTelephoneNumber.isEmpty()));
    }

    public void unsetFacsimileTelephoneNumber() {
        this.facsimileTelephoneNumber = null;
    }

    /**
     * Gets the value of the postalAddress property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the postalAddress property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getPostalAddress().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getPostalAddress() {
        if (postalAddress == null) {
            postalAddress = new ArrayList<String>();
        }
        return this.postalAddress;
    }

    public boolean isSetPostalAddress() {
        return ((this.postalAddress != null) && (!this.postalAddress.isEmpty()));
    }

    public void unsetPostalAddress() {
        this.postalAddress = null;
    }

    /**
     * Gets the value of the l property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the l property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getL().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getL() {
        if (l == null) {
            l = new ArrayList<String>();
        }
        return this.l;
    }

    public boolean isSetL() {
        return ((this.l != null) && (!this.l.isEmpty()));
    }

    public void unsetL() {
        this.l = null;
    }

    /**
     * Gets the value of the localityName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the localityName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getLocalityName().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getLocalityName() {
        if (localityName == null) {
            localityName = new ArrayList<String>();
        }
        return this.localityName;
    }

    public boolean isSetLocalityName() {
        return ((this.localityName != null) && (!this.localityName.isEmpty()));
    }

    public void unsetLocalityName() {
        this.localityName = null;
    }

    /**
     * Gets the value of the st property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the st property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
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

    public boolean isSetSt() {
        return ((this.st != null) && (!this.st.isEmpty()));
    }

    public void unsetSt() {
        this.st = null;
    }

    /**
     * Gets the value of the stateOrProvinceName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the stateOrProvinceName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
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

    public boolean isSetStateOrProvinceName() {
        return ((this.stateOrProvinceName != null) && (!this.stateOrProvinceName.isEmpty()));
    }

    public void unsetStateOrProvinceName() {
        this.stateOrProvinceName = null;
    }

    /**
     * Gets the value of the street property.
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

    public boolean isSetStreet() {
        return ((this.street != null) && (!this.street.isEmpty()));
    }

    public void unsetStreet() {
        this.street = null;
    }

    /**
     * Gets the value of the postalCode property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the postalCode property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getPostalCode().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getPostalCode() {
        if (postalCode == null) {
            postalCode = new ArrayList<String>();
        }
        return this.postalCode;
    }

    public boolean isSetPostalCode() {
        return ((this.postalCode != null) && (!this.postalCode.isEmpty()));
    }

    public void unsetPostalCode() {
        this.postalCode = null;
    }

    /**
     * Gets the value of the businessAddress property.
     * 
     * @return
     *         possible object is {@link AddressType }
     * 
     */
    public AddressType getBusinessAddress() {
        return businessAddress;
    }

    /**
     * Sets the value of the businessAddress property.
     * 
     * @param value
     *            allowed object is {@link AddressType }
     * 
     */
    public void setBusinessAddress(AddressType value) {
        this.businessAddress = value;
    }

    public boolean isSetBusinessAddress() {
        return (this.businessAddress != null);
    }

    /**
     * Gets the value of the description property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the description property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
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

    public boolean isSetDescription() {
        return ((this.description != null) && (!this.description.isEmpty()));
    }

    public void unsetDescription() {
        this.description = null;
    }

    /**
     * Gets the value of the businessCategory property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the businessCategory property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getBusinessCategory().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getBusinessCategory() {
        if (businessCategory == null) {
            businessCategory = new ArrayList<String>();
        }
        return this.businessCategory;
    }

    public boolean isSetBusinessCategory() {
        return ((this.businessCategory != null) && (!this.businessCategory.isEmpty()));
    }

    public void unsetBusinessCategory() {
        this.businessCategory = null;
    }

    /**
     * Gets the value of the seeAlso property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the seeAlso property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
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

    public boolean isSetSeeAlso() {
        return ((this.seeAlso != null) && (!this.seeAlso.isEmpty()));
    }

    public void unsetSeeAlso() {
        this.seeAlso = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals("o")) {
            return getO();
        }
        if (propName.equals("ou")) {
            return getOu();
        }
        if (propName.equals("dc")) {
            return getDc();
        }
        if (propName.equals("cn")) {
            return getCn();
        }
        if (propName.equals("telephoneNumber")) {
            return getTelephoneNumber();
        }
        if (propName.equals("facsimileTelephoneNumber")) {
            return getFacsimileTelephoneNumber();
        }
        if (propName.equals("postalAddress")) {
            return getPostalAddress();
        }
        if (propName.equals("l")) {
            return getL();
        }
        if (propName.equals("localityName")) {
            return getLocalityName();
        }
        if (propName.equals("st")) {
            return getSt();
        }
        if (propName.equals("stateOrProvinceName")) {
            return getStateOrProvinceName();
        }
        if (propName.equals("street")) {
            return getStreet();
        }
        if (propName.equals("postalCode")) {
            return getPostalCode();
        }
        if (propName.equals("businessAddress")) {
            return getBusinessAddress();
        }
        if (propName.equals("description")) {
            return getDescription();
        }
        if (propName.equals("businessCategory")) {
            return getBusinessCategory();
        }
        if (propName.equals("seeAlso")) {
            return getSeeAlso();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals("o")) {
            return isSetO();
        }
        if (propName.equals("ou")) {
            return isSetOu();
        }
        if (propName.equals("dc")) {
            return isSetDc();
        }
        if (propName.equals("cn")) {
            return isSetCn();
        }
        if (propName.equals("telephoneNumber")) {
            return isSetTelephoneNumber();
        }
        if (propName.equals("facsimileTelephoneNumber")) {
            return isSetFacsimileTelephoneNumber();
        }
        if (propName.equals("postalAddress")) {
            return isSetPostalAddress();
        }
        if (propName.equals("l")) {
            return isSetL();
        }
        if (propName.equals("localityName")) {
            return isSetLocalityName();
        }
        if (propName.equals("st")) {
            return isSetSt();
        }
        if (propName.equals("stateOrProvinceName")) {
            return isSetStateOrProvinceName();
        }
        if (propName.equals("street")) {
            return isSetStreet();
        }
        if (propName.equals("postalCode")) {
            return isSetPostalCode();
        }
        if (propName.equals("businessAddress")) {
            return isSetBusinessAddress();
        }
        if (propName.equals("description")) {
            return isSetDescription();
        }
        if (propName.equals("businessCategory")) {
            return isSetBusinessCategory();
        }
        if (propName.equals("seeAlso")) {
            return isSetSeeAlso();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals("o")) {
            setO(((String) value));
        }
        if (propName.equals("ou")) {
            setOu(((String) value));
        }
        if (propName.equals("dc")) {
            setDc(((String) value));
        }
        if (propName.equals("cn")) {
            setCn(((String) value));
        }
        if (propName.equals("telephoneNumber")) {
            getTelephoneNumber().add(((String) value));
        }
        if (propName.equals("facsimileTelephoneNumber")) {
            getFacsimileTelephoneNumber().add(((String) value));
        }
        if (propName.equals("postalAddress")) {
            getPostalAddress().add(((String) value));
        }
        if (propName.equals("l")) {
            getL().add(((String) value));
        }
        if (propName.equals("localityName")) {
            getLocalityName().add(((String) value));
        }
        if (propName.equals("st")) {
            getSt().add(((String) value));
        }
        if (propName.equals("stateOrProvinceName")) {
            getStateOrProvinceName().add(((String) value));
        }
        if (propName.equals("street")) {
            getStreet().add(((String) value));
        }
        if (propName.equals("postalCode")) {
            getPostalCode().add(((String) value));
        }
        if (propName.equals("businessAddress")) {
            setBusinessAddress(((AddressType) value));
        }
        if (propName.equals("description")) {
            getDescription().add(((String) value));
        }
        if (propName.equals("businessCategory")) {
            getBusinessCategory().add(((String) value));
        }
        if (propName.equals("seeAlso")) {
            getSeeAlso().add(((String) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals("telephoneNumber")) {
            unsetTelephoneNumber();
        }
        if (propName.equals("facsimileTelephoneNumber")) {
            unsetFacsimileTelephoneNumber();
        }
        if (propName.equals("postalAddress")) {
            unsetPostalAddress();
        }
        if (propName.equals("l")) {
            unsetL();
        }
        if (propName.equals("localityName")) {
            unsetLocalityName();
        }
        if (propName.equals("st")) {
            unsetSt();
        }
        if (propName.equals("stateOrProvinceName")) {
            unsetStateOrProvinceName();
        }
        if (propName.equals("street")) {
            unsetStreet();
        }
        if (propName.equals("postalCode")) {
            unsetPostalCode();
        }
        if (propName.equals("description")) {
            unsetDescription();
        }
        if (propName.equals("businessCategory")) {
            unsetBusinessCategory();
        }
        if (propName.equals("seeAlso")) {
            unsetSeeAlso();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return "OrgContainer";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("o");
                names.add("ou");
                names.add("dc");
                names.add("cn");
                names.add("telephoneNumber");
                names.add("facsimileTelephoneNumber");
                names.add("postalAddress");
                names.add("l");
                names.add("localityName");
                names.add("st");
                names.add("stateOrProvinceName");
                names.add("street");
                names.add("postalCode");
                names.add("businessAddress");
                names.add("description");
                names.add("businessCategory");
                names.add("seeAlso");
                names.addAll(Party.getPropertyNames("Party"));
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("o", "String");
        dataTypeMap.put("ou", "String");
        dataTypeMap.put("dc", "String");
        dataTypeMap.put("cn", "String");
        dataTypeMap.put("telephoneNumber", "String");
        dataTypeMap.put("facsimileTelephoneNumber", "String");
        dataTypeMap.put("postalAddress", "String");
        dataTypeMap.put("l", "String");
        dataTypeMap.put("localityName", "String");
        dataTypeMap.put("st", "String");
        dataTypeMap.put("stateOrProvinceName", "String");
        dataTypeMap.put("street", "String");
        dataTypeMap.put("postalCode", "String");
        dataTypeMap.put("businessAddress", "AddressType");
        dataTypeMap.put("description", "String");
        dataTypeMap.put("businessCategory", "String");
        dataTypeMap.put("seeAlso", "String");
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
        superTypeList.add("Party");
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

}
