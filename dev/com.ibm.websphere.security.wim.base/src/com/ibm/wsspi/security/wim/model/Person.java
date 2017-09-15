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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for Person complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Person">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}Party">
 * &lt;group ref="{http://www.ibm.com/websphere/wim}PersonPropertyGroup"/>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The Person object extends the Party object and is used to represent a user or principal.
 * 
 * <p> The Person object defines the various properties that can be associated with a user, such as uid, cn,
 * and mail.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Person", propOrder = {
                                       "uid",
                                       "cn",
                                       "sn",
                                       "preferredLanguage",
                                       "displayName",
                                       "initials",
                                       "mail",
                                       "ibmPrimaryEmail",
                                       "jpegPhoto",
                                       "labeledURI",
                                       "carLicense",
                                       "telephoneNumber",
                                       "facsimileTelephoneNumber",
                                       "pager",
                                       "mobile",
                                       "homePostalAddress",
                                       "postalAddress",
                                       "roomNumber",
                                       "l",
                                       "localityName",
                                       "st",
                                       "stateOrProvinceName",
                                       "street",
                                       "postalCode",
                                       "city",
                                       "employeeType",
                                       "employeeNumber",
                                       "manager",
                                       "secretary",
                                       "departmentNumber",
                                       "title",
                                       "ibmJobTitle",
                                       "c",
                                       "countryName",
                                       "givenName",
                                       "homeAddress",
                                       "businessAddress",
                                       "description",
                                       "businessCategory",
                                       "seeAlso",
                                       "kerberosId",
                                       "photoURL",
                                       "photoURLThumbnail"
})
public class Person
                extends Party
{

    protected String uid;
    protected String cn;
    protected String sn;
    protected String preferredLanguage;
    protected List<String> displayName;
    protected List<String> initials;
    protected String mail;
    @XmlElement(name = "ibm-primaryEmail")
    protected String ibmPrimaryEmail;
    protected List<byte[]> jpegPhoto;
    protected String labeledURI;
    protected List<String> carLicense;
    protected List<String> telephoneNumber;
    protected List<String> facsimileTelephoneNumber;
    protected List<String> pager;
    protected List<String> mobile;
    protected List<String> homePostalAddress;
    protected List<String> postalAddress;
    protected List<String> roomNumber;
    protected List<String> l;
    protected List<String> localityName;
    protected List<String> st;
    protected List<String> stateOrProvinceName;
    protected List<String> street;
    protected List<String> postalCode;
    protected List<String> city;
    protected String employeeType;
    protected String employeeNumber;
    protected List<com.ibm.wsspi.security.wim.model.IdentifierType> manager;
    protected List<com.ibm.wsspi.security.wim.model.IdentifierType> secretary;
    protected List<String> departmentNumber;
    protected List<String> title;
    @XmlElement(name = "ibm-jobTitle")
    protected List<String> ibmJobTitle;
    protected List<String> c;
    protected List<String> countryName;
    protected List<String> givenName;
    protected List<com.ibm.wsspi.security.wim.model.AddressType> homeAddress;
    protected List<com.ibm.wsspi.security.wim.model.AddressType> businessAddress;
    protected List<String> description;
    protected List<String> businessCategory;
    protected List<String> seeAlso;
    protected String kerberosId;
    protected String photoURL;
    protected String photoURLThumbnail;
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
     * Gets the value of the uid property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getUid() {
        return uid;
    }

    /**
     * Sets the value of the uid property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setUid(String value) {
        this.uid = value;
    }

    public boolean isSetUid() {
        return (this.uid != null);
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
     * Gets the value of the sn property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getSn() {
        return sn;
    }

    /**
     * Sets the value of the sn property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setSn(String value) {
        this.sn = value;
    }

    public boolean isSetSn() {
        return (this.sn != null);
    }

    /**
     * Gets the value of the preferredLanguage property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    /**
     * Sets the value of the preferredLanguage property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setPreferredLanguage(String value) {
        this.preferredLanguage = value;
    }

    public boolean isSetPreferredLanguage() {
        return (this.preferredLanguage != null);
    }

    /**
     * Gets the value of the displayName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the displayName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getDisplayName().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getDisplayName() {
        if (displayName == null) {
            displayName = new ArrayList<String>();
        }
        return this.displayName;
    }

    public boolean isSetDisplayName() {
        return ((this.displayName != null) && (!this.displayName.isEmpty()));
    }

    public void unsetDisplayName() {
        this.displayName = null;
    }

    /**
     * Gets the value of the initials property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the initials property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getInitials().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getInitials() {
        if (initials == null) {
            initials = new ArrayList<String>();
        }
        return this.initials;
    }

    public boolean isSetInitials() {
        return ((this.initials != null) && (!this.initials.isEmpty()));
    }

    public void unsetInitials() {
        this.initials = null;
    }

    /**
     * Gets the value of the mail property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getMail() {
        return mail;
    }

    /**
     * Sets the value of the mail property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setMail(String value) {
        this.mail = value;
    }

    public boolean isSetMail() {
        return (this.mail != null);
    }

    /**
     * Gets the value of the ibmPrimaryEmail property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getIbmPrimaryEmail() {
        return ibmPrimaryEmail;
    }

    /**
     * Sets the value of the ibmPrimaryEmail property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setIbmPrimaryEmail(String value) {
        this.ibmPrimaryEmail = value;
    }

    public boolean isSetIbmPrimaryEmail() {
        return (this.ibmPrimaryEmail != null);
    }

    /**
     * Gets the value of the jpegPhoto property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the jpegPhoto property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getJpegPhoto().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * byte[]
     * 
     */
    public List<byte[]> getJpegPhoto() {
        if (jpegPhoto == null) {
            jpegPhoto = new ArrayList<byte[]>();
        }
        return this.jpegPhoto;
    }

    public boolean isSetJpegPhoto() {
        return ((this.jpegPhoto != null) && (!this.jpegPhoto.isEmpty()));
    }

    public void unsetJpegPhoto() {
        this.jpegPhoto = null;
    }

    /**
     * Gets the value of the labeledURI property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getLabeledURI() {
        return labeledURI;
    }

    /**
     * Sets the value of the labeledURI property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setLabeledURI(String value) {
        this.labeledURI = value;
    }

    public boolean isSetLabeledURI() {
        return (this.labeledURI != null);
    }

    /**
     * Gets the value of the carLicense property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the carLicense property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getCarLicense().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getCarLicense() {
        if (carLicense == null) {
            carLicense = new ArrayList<String>();
        }
        return this.carLicense;
    }

    public boolean isSetCarLicense() {
        return ((this.carLicense != null) && (!this.carLicense.isEmpty()));
    }

    public void unsetCarLicense() {
        this.carLicense = null;
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
     * Gets the value of the pager property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the pager property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getPager().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getPager() {
        if (pager == null) {
            pager = new ArrayList<String>();
        }
        return this.pager;
    }

    public boolean isSetPager() {
        return ((this.pager != null) && (!this.pager.isEmpty()));
    }

    public void unsetPager() {
        this.pager = null;
    }

    /**
     * Gets the value of the mobile property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mobile property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getMobile().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getMobile() {
        if (mobile == null) {
            mobile = new ArrayList<String>();
        }
        return this.mobile;
    }

    public boolean isSetMobile() {
        return ((this.mobile != null) && (!this.mobile.isEmpty()));
    }

    public void unsetMobile() {
        this.mobile = null;
    }

    /**
     * Gets the value of the homePostalAddress property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the homePostalAddress property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getHomePostalAddress().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getHomePostalAddress() {
        if (homePostalAddress == null) {
            homePostalAddress = new ArrayList<String>();
        }
        return this.homePostalAddress;
    }

    public boolean isSetHomePostalAddress() {
        return ((this.homePostalAddress != null) && (!this.homePostalAddress.isEmpty()));
    }

    public void unsetHomePostalAddress() {
        this.homePostalAddress = null;
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
     * Gets the value of the roomNumber property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the roomNumber property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getRoomNumber().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getRoomNumber() {
        if (roomNumber == null) {
            roomNumber = new ArrayList<String>();
        }
        return this.roomNumber;
    }

    public boolean isSetRoomNumber() {
        return ((this.roomNumber != null) && (!this.roomNumber.isEmpty()));
    }

    public void unsetRoomNumber() {
        this.roomNumber = null;
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
     * Gets the value of the city property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the city property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getCity().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getCity() {
        if (city == null) {
            city = new ArrayList<String>();
        }
        return this.city;
    }

    public boolean isSetCity() {
        return ((this.city != null) && (!this.city.isEmpty()));
    }

    public void unsetCity() {
        this.city = null;
    }

    /**
     * Gets the value of the employeeType property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getEmployeeType() {
        return employeeType;
    }

    /**
     * Sets the value of the employeeType property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setEmployeeType(String value) {
        this.employeeType = value;
    }

    public boolean isSetEmployeeType() {
        return (this.employeeType != null);
    }

    /**
     * Gets the value of the employeeNumber property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getEmployeeNumber() {
        return employeeNumber;
    }

    /**
     * Sets the value of the employeeNumber property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setEmployeeNumber(String value) {
        this.employeeNumber = value;
    }

    public boolean isSetEmployeeNumber() {
        return (this.employeeNumber != null);
    }

    /**
     * Gets the value of the manager property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the manager property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getManager().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.IdentifierType }
     * 
     * 
     */
    public List<com.ibm.wsspi.security.wim.model.IdentifierType> getManager() {
        if (manager == null) {
            manager = new ArrayList<com.ibm.wsspi.security.wim.model.IdentifierType>();
        }
        return this.manager;
    }

    public boolean isSetManager() {
        return ((this.manager != null) && (!this.manager.isEmpty()));
    }

    public void unsetManager() {
        this.manager = null;
    }

    /**
     * Gets the value of the secretary property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the secretary property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getSecretary().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.IdentifierType }
     * 
     * 
     */
    public List<com.ibm.wsspi.security.wim.model.IdentifierType> getSecretary() {
        if (secretary == null) {
            secretary = new ArrayList<com.ibm.wsspi.security.wim.model.IdentifierType>();
        }
        return this.secretary;
    }

    public boolean isSetSecretary() {
        return ((this.secretary != null) && (!this.secretary.isEmpty()));
    }

    public void unsetSecretary() {
        this.secretary = null;
    }

    /**
     * Gets the value of the departmentNumber property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the departmentNumber property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getDepartmentNumber().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getDepartmentNumber() {
        if (departmentNumber == null) {
            departmentNumber = new ArrayList<String>();
        }
        return this.departmentNumber;
    }

    public boolean isSetDepartmentNumber() {
        return ((this.departmentNumber != null) && (!this.departmentNumber.isEmpty()));
    }

    public void unsetDepartmentNumber() {
        this.departmentNumber = null;
    }

    /**
     * Gets the value of the title property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the title property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getTitle().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getTitle() {
        if (title == null) {
            title = new ArrayList<String>();
        }
        return this.title;
    }

    public boolean isSetTitle() {
        return ((this.title != null) && (!this.title.isEmpty()));
    }

    public void unsetTitle() {
        this.title = null;
    }

    /**
     * Gets the value of the ibmJobTitle property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the ibmJobTitle property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getIbmJobTitle().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getIbmJobTitle() {
        if (ibmJobTitle == null) {
            ibmJobTitle = new ArrayList<String>();
        }
        return this.ibmJobTitle;
    }

    public boolean isSetIbmJobTitle() {
        return ((this.ibmJobTitle != null) && (!this.ibmJobTitle.isEmpty()));
    }

    public void unsetIbmJobTitle() {
        this.ibmJobTitle = null;
    }

    /**
     * Gets the value of the c property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the c property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getC().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getC() {
        if (c == null) {
            c = new ArrayList<String>();
        }
        return this.c;
    }

    public boolean isSetC() {
        return ((this.c != null) && (!this.c.isEmpty()));
    }

    public void unsetC() {
        this.c = null;
    }

    /**
     * Gets the value of the countryName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the countryName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getCountryName().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getCountryName() {
        if (countryName == null) {
            countryName = new ArrayList<String>();
        }
        return this.countryName;
    }

    public boolean isSetCountryName() {
        return ((this.countryName != null) && (!this.countryName.isEmpty()));
    }

    public void unsetCountryName() {
        this.countryName = null;
    }

    /**
     * Gets the value of the givenName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the givenName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getGivenName().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getGivenName() {
        if (givenName == null) {
            givenName = new ArrayList<String>();
        }
        return this.givenName;
    }

    public boolean isSetGivenName() {
        return ((this.givenName != null) && (!this.givenName.isEmpty()));
    }

    public void unsetGivenName() {
        this.givenName = null;
    }

    /**
     * Gets the value of the homeAddress property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the homeAddress property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getHomeAddress().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.AddressType }
     * 
     * 
     */
    public List<com.ibm.wsspi.security.wim.model.AddressType> getHomeAddress() {
        if (homeAddress == null) {
            homeAddress = new ArrayList<com.ibm.wsspi.security.wim.model.AddressType>();
        }
        return this.homeAddress;
    }

    public boolean isSetHomeAddress() {
        return ((this.homeAddress != null) && (!this.homeAddress.isEmpty()));
    }

    public void unsetHomeAddress() {
        this.homeAddress = null;
    }

    /**
     * Gets the value of the businessAddress property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the businessAddress property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getBusinessAddress().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.AddressType }
     * 
     * 
     */
    public List<com.ibm.wsspi.security.wim.model.AddressType> getBusinessAddress() {
        if (businessAddress == null) {
            businessAddress = new ArrayList<com.ibm.wsspi.security.wim.model.AddressType>();
        }
        return this.businessAddress;
    }

    public boolean isSetBusinessAddress() {
        return ((this.businessAddress != null) && (!this.businessAddress.isEmpty()));
    }

    public void unsetBusinessAddress() {
        this.businessAddress = null;
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

    /**
     * Gets the value of the kerberosId property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getKerberosId() {
        return kerberosId;
    }

    /**
     * Sets the value of the kerberosId property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setKerberosId(String value) {
        this.kerberosId = value;
    }

    public boolean isSetKerberosId() {
        return (this.kerberosId != null);
    }

    /**
     * Gets the value of the photoURL property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getPhotoUrl() {
        return photoURL;
    }

    /**
     * Sets the value of the photoURL property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setPhotoUrl(String value) {
        this.photoURL = value;
    }

    public boolean isSetPhotoUrl() {
        return (this.photoURL != null);
    }

    /**
     * Gets the value of the photoURLThumbnail property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getPhotoUrlThumbnail() {
        return photoURLThumbnail;
    }

    /**
     * Sets the value of the photoURLThumbnail property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setPhotoUrlThumbnail(String value) {
        this.photoURLThumbnail = value;
    }

    public boolean isSetPhotoUrlThumbnail() {
        return (this.photoURLThumbnail != null);
    }

    @Override
    public Object get(String propName) {
        if (propName.equals("uid")) {
            return getUid();
        }
        if (propName.equals("cn")) {
            return getCn();
        }
        if (propName.equals("sn")) {
            return getSn();
        }
        if (propName.equals("preferredLanguage")) {
            return getPreferredLanguage();
        }
        if (propName.equals("displayName")) {
            return getDisplayName();
        }
        if (propName.equals("initials")) {
            return getInitials();
        }
        if (propName.equals("mail")) {
            return getMail();
        }
        if (propName.equals("ibmPrimaryEmail")) {
            return getIbmPrimaryEmail();
        }
        if (propName.equals("jpegPhoto")) {
            return getJpegPhoto();
        }
        if (propName.equals("labeledURI")) {
            return getLabeledURI();
        }
        if (propName.equals("carLicense")) {
            return getCarLicense();
        }
        if (propName.equals("telephoneNumber")) {
            return getTelephoneNumber();
        }
        if (propName.equals("facsimileTelephoneNumber")) {
            return getFacsimileTelephoneNumber();
        }
        if (propName.equals("pager")) {
            return getPager();
        }
        if (propName.equals("mobile")) {
            return getMobile();
        }
        if (propName.equals("homePostalAddress")) {
            return getHomePostalAddress();
        }
        if (propName.equals("postalAddress")) {
            return getPostalAddress();
        }
        if (propName.equals("roomNumber")) {
            return getRoomNumber();
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
        if (propName.equals("city")) {
            return getCity();
        }
        if (propName.equals("employeeType")) {
            return getEmployeeType();
        }
        if (propName.equals("employeeNumber")) {
            return getEmployeeNumber();
        }
        if (propName.equals("manager")) {
            return getManager();
        }
        if (propName.equals("secretary")) {
            return getSecretary();
        }
        if (propName.equals("departmentNumber")) {
            return getDepartmentNumber();
        }
        if (propName.equals("title")) {
            return getTitle();
        }
        if (propName.equals("ibmJobTitle")) {
            return getIbmJobTitle();
        }
        if (propName.equals("c")) {
            return getC();
        }
        if (propName.equals("countryName")) {
            return getCountryName();
        }
        if (propName.equals("givenName")) {
            return getGivenName();
        }
        if (propName.equals("homeAddress")) {
            return getHomeAddress();
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
        if (propName.equals("kerberosId")) {
            return getKerberosId();
        }
        if (propName.equals("photoURL")) {
            return getPhotoUrl();
        }
        if (propName.equals("photoURLThumbnail")) {
            return getPhotoUrlThumbnail();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals("uid")) {
            return isSetUid();
        }
        if (propName.equals("cn")) {
            return isSetCn();
        }
        if (propName.equals("sn")) {
            return isSetSn();
        }
        if (propName.equals("preferredLanguage")) {
            return isSetPreferredLanguage();
        }
        if (propName.equals("displayName")) {
            return isSetDisplayName();
        }
        if (propName.equals("initials")) {
            return isSetInitials();
        }
        if (propName.equals("mail")) {
            return isSetMail();
        }
        if (propName.equals("ibmPrimaryEmail")) {
            return isSetIbmPrimaryEmail();
        }
        if (propName.equals("jpegPhoto")) {
            return isSetJpegPhoto();
        }
        if (propName.equals("labeledURI")) {
            return isSetLabeledURI();
        }
        if (propName.equals("carLicense")) {
            return isSetCarLicense();
        }
        if (propName.equals("telephoneNumber")) {
            return isSetTelephoneNumber();
        }
        if (propName.equals("facsimileTelephoneNumber")) {
            return isSetFacsimileTelephoneNumber();
        }
        if (propName.equals("pager")) {
            return isSetPager();
        }
        if (propName.equals("mobile")) {
            return isSetMobile();
        }
        if (propName.equals("homePostalAddress")) {
            return isSetHomePostalAddress();
        }
        if (propName.equals("postalAddress")) {
            return isSetPostalAddress();
        }
        if (propName.equals("roomNumber")) {
            return isSetRoomNumber();
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
        if (propName.equals("city")) {
            return isSetCity();
        }
        if (propName.equals("employeeType")) {
            return isSetEmployeeType();
        }
        if (propName.equals("employeeNumber")) {
            return isSetEmployeeNumber();
        }
        if (propName.equals("manager")) {
            return isSetManager();
        }
        if (propName.equals("secretary")) {
            return isSetSecretary();
        }
        if (propName.equals("departmentNumber")) {
            return isSetDepartmentNumber();
        }
        if (propName.equals("title")) {
            return isSetTitle();
        }
        if (propName.equals("ibmJobTitle")) {
            return isSetIbmJobTitle();
        }
        if (propName.equals("c")) {
            return isSetC();
        }
        if (propName.equals("countryName")) {
            return isSetCountryName();
        }
        if (propName.equals("givenName")) {
            return isSetGivenName();
        }
        if (propName.equals("homeAddress")) {
            return isSetHomeAddress();
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
        if (propName.equals("kerberosId")) {
            return isSetKerberosId();
        }
        if (propName.equals("photoURL")) {
            return isSetPhotoUrl();
        }
        if (propName.equals("photoURLThumbnail")) {
            return isSetPhotoUrlThumbnail();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals("uid")) {
            setUid(((String) value));
        }
        if (propName.equals("cn")) {
            setCn(((String) value));
        }
        if (propName.equals("sn")) {
            setSn(((String) value));
        }
        if (propName.equals("preferredLanguage")) {
            setPreferredLanguage(((String) value));
        }
        if (propName.equals("displayName")) {
            getDisplayName().add(((String) value));
        }
        if (propName.equals("initials")) {
            getInitials().add(((String) value));
        }
        if (propName.equals("mail")) {
            setMail(((String) value));
        }
        if (propName.equals("ibmPrimaryEmail")) {
            setIbmPrimaryEmail(((String) value));
        }
        if (propName.equals("jpegPhoto")) {
            getJpegPhoto().add(((byte[]) value));
        }
        if (propName.equals("labeledURI")) {
            setLabeledURI(((String) value));
        }
        if (propName.equals("carLicense")) {
            getCarLicense().add(((String) value));
        }
        if (propName.equals("telephoneNumber")) {
            getTelephoneNumber().add(((String) value));
        }
        if (propName.equals("facsimileTelephoneNumber")) {
            getFacsimileTelephoneNumber().add(((String) value));
        }
        if (propName.equals("pager")) {
            getPager().add(((String) value));
        }
        if (propName.equals("mobile")) {
            getMobile().add(((String) value));
        }
        if (propName.equals("homePostalAddress")) {
            getHomePostalAddress().add(((String) value));
        }
        if (propName.equals("postalAddress")) {
            getPostalAddress().add(((String) value));
        }
        if (propName.equals("roomNumber")) {
            getRoomNumber().add(((String) value));
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
        if (propName.equals("city")) {
            getCity().add(((String) value));
        }
        if (propName.equals("employeeType")) {
            setEmployeeType(((String) value));
        }
        if (propName.equals("employeeNumber")) {
            setEmployeeNumber(((String) value));
        }
        if (propName.equals("manager")) {
            getManager().add(((com.ibm.wsspi.security.wim.model.IdentifierType) value));
        }
        if (propName.equals("secretary")) {
            getSecretary().add(((com.ibm.wsspi.security.wim.model.IdentifierType) value));
        }
        if (propName.equals("departmentNumber")) {
            getDepartmentNumber().add(((String) value));
        }
        if (propName.equals("title")) {
            getTitle().add(((String) value));
        }
        if (propName.equals("ibmJobTitle")) {
            getIbmJobTitle().add(((String) value));
        }
        if (propName.equals("c")) {
            getC().add(((String) value));
        }
        if (propName.equals("countryName")) {
            getCountryName().add(((String) value));
        }
        if (propName.equals("givenName")) {
            getGivenName().add(((String) value));
        }
        if (propName.equals("homeAddress")) {
            getHomeAddress().add(((com.ibm.wsspi.security.wim.model.AddressType) value));
        }
        if (propName.equals("businessAddress")) {
            getBusinessAddress().add(((com.ibm.wsspi.security.wim.model.AddressType) value));
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
        if (propName.equals("kerberosId")) {
            setKerberosId(((String) value));
        }
        if (propName.equals("photoURL")) {
            setPhotoUrl(((String) value));
        }
        if (propName.equals("photoURLThumbnail")) {
            setPhotoUrlThumbnail(((String) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals("displayName")) {
            unsetDisplayName();
        }
        if (propName.equals("initials")) {
            unsetInitials();
        }
        if (propName.equals("jpegPhoto")) {
            unsetJpegPhoto();
        }
        if (propName.equals("carLicense")) {
            unsetCarLicense();
        }
        if (propName.equals("telephoneNumber")) {
            unsetTelephoneNumber();
        }
        if (propName.equals("facsimileTelephoneNumber")) {
            unsetFacsimileTelephoneNumber();
        }
        if (propName.equals("pager")) {
            unsetPager();
        }
        if (propName.equals("mobile")) {
            unsetMobile();
        }
        if (propName.equals("homePostalAddress")) {
            unsetHomePostalAddress();
        }
        if (propName.equals("postalAddress")) {
            unsetPostalAddress();
        }
        if (propName.equals("roomNumber")) {
            unsetRoomNumber();
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
        if (propName.equals("city")) {
            unsetCity();
        }
        if (propName.equals("manager")) {
            unsetManager();
        }
        if (propName.equals("secretary")) {
            unsetSecretary();
        }
        if (propName.equals("departmentNumber")) {
            unsetDepartmentNumber();
        }
        if (propName.equals("title")) {
            unsetTitle();
        }
        if (propName.equals("ibmJobTitle")) {
            unsetIbmJobTitle();
        }
        if (propName.equals("c")) {
            unsetC();
        }
        if (propName.equals("countryName")) {
            unsetCountryName();
        }
        if (propName.equals("givenName")) {
            unsetGivenName();
        }
        if (propName.equals("homeAddress")) {
            unsetHomeAddress();
        }
        if (propName.equals("businessAddress")) {
            unsetBusinessAddress();
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
        return "Person";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("uid");
                names.add("cn");
                names.add("sn");
                names.add("preferredLanguage");
                names.add("displayName");
                names.add("initials");
                names.add("mail");
                names.add("ibmPrimaryEmail");
                names.add("jpegPhoto");
                names.add("labeledURI");
                names.add("carLicense");
                names.add("telephoneNumber");
                names.add("facsimileTelephoneNumber");
                names.add("pager");
                names.add("mobile");
                names.add("homePostalAddress");
                names.add("postalAddress");
                names.add("roomNumber");
                names.add("l");
                names.add("localityName");
                names.add("st");
                names.add("stateOrProvinceName");
                names.add("street");
                names.add("postalCode");
                names.add("city");
                names.add("employeeType");
                names.add("employeeNumber");
                names.add("manager");
                names.add("secretary");
                names.add("departmentNumber");
                names.add("title");
                names.add("ibmJobTitle");
                names.add("c");
                names.add("countryName");
                names.add("givenName");
                names.add("homeAddress");
                names.add("businessAddress");
                names.add("description");
                names.add("businessCategory");
                names.add("seeAlso");
                names.add("kerberosId");
                names.add("photoURL");
                names.add("photoURLThumbnail");
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
        dataTypeMap.put("uid", "String");
        dataTypeMap.put("cn", "String");
        dataTypeMap.put("sn", "String");
        dataTypeMap.put("preferredLanguage", "String");
        dataTypeMap.put("displayName", "String");
        dataTypeMap.put("initials", "String");
        dataTypeMap.put("mail", "String");
        dataTypeMap.put("ibmPrimaryEmail", "String");
        dataTypeMap.put("jpegPhoto", "byte[]");
        dataTypeMap.put("labeledURI", "String");
        dataTypeMap.put("carLicense", "String");
        dataTypeMap.put("telephoneNumber", "String");
        dataTypeMap.put("facsimileTelephoneNumber", "String");
        dataTypeMap.put("pager", "String");
        dataTypeMap.put("mobile", "String");
        dataTypeMap.put("homePostalAddress", "String");
        dataTypeMap.put("postalAddress", "String");
        dataTypeMap.put("roomNumber", "String");
        dataTypeMap.put("l", "String");
        dataTypeMap.put("localityName", "String");
        dataTypeMap.put("st", "String");
        dataTypeMap.put("stateOrProvinceName", "String");
        dataTypeMap.put("street", "String");
        dataTypeMap.put("postalCode", "String");
        dataTypeMap.put("city", "String");
        dataTypeMap.put("employeeType", "String");
        dataTypeMap.put("employeeNumber", "String");
        dataTypeMap.put("manager", "IdentifierType");
        dataTypeMap.put("secretary", "IdentifierType");
        dataTypeMap.put("departmentNumber", "String");
        dataTypeMap.put("title", "String");
        dataTypeMap.put("ibmJobTitle", "String");
        dataTypeMap.put("c", "String");
        dataTypeMap.put("countryName", "String");
        dataTypeMap.put("givenName", "String");
        dataTypeMap.put("homeAddress", "AddressType");
        dataTypeMap.put("businessAddress", "AddressType");
        dataTypeMap.put("description", "String");
        dataTypeMap.put("businessCategory", "String");
        dataTypeMap.put("seeAlso", "String");
        dataTypeMap.put("kerberosId", "String");
        dataTypeMap.put("photoURL", "String");
        dataTypeMap.put("photoURLThumbnail", "String");
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
