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
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for PersonAccount complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PersonAccount">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}LoginAccount">
 * &lt;group ref="{http://www.ibm.com/websphere/wim}PersonPropertyGroup"/>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The PersonAccount object extends the LoginAccount object, and represents an account with
 * person properties.
 * 
 * <p> In addition to support the properties defined by the LoginAccount object, principalName, password, realm and certificate,
 * the PersonAccount defines the additional properties associated with this entity object such as uid, cn, sn, displayName, and
 * email.
 * 
 * <p> A PersonAccount object allows the schema of a person and his login accounts, defined by the
 * respective LoginAccount objects, to be kept independent of each other. For example, the set of properties on a PersonAccount
 * may not need to be exactly the same as the set of person-specific properties on each of that person's
 * LoginAccount entities.
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PersonAccount", propOrder = {
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
                                              "homeStreet",
                                              "homeCity",
                                              "homeStateOrProvinceName",
                                              "homePostalCode",
                                              "homeCountryName",
                                              "businessStreet",
                                              "businessCity",
                                              "businessStateOrProvinceName",
                                              "businessPostalCode",
                                              "businessCountryName",
                                              "description",
                                              "businessCategory",
                                              "seeAlso",
                                              "kerberosId",
                                              "photoURL",
                                              "photoURLThumbnail",
                                              "middleName",
                                              "honorificPrefix",
                                              "honorificSuffix",
                                              "nickName",
                                              "profileUrl",
                                              "timezone",
                                              "locale",
                                              "ims",
                                              "active"
})
public class PersonAccount
                extends LoginAccount
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
    protected String homeStreet;
    protected String homeCity;
    protected String homeStateOrProvinceName;
    protected String homePostalCode;
    protected String homeCountryName;
    protected String businessStreet;
    protected String businessCity;
    protected String businessStateOrProvinceName;
    protected String businessPostalCode;
    protected String businessCountryName;
    protected List<String> description;
    protected List<String> businessCategory;
    protected List<String> seeAlso;
    protected String kerberosId;
    protected String photoURL;
    protected String photoURLThumbnail;
    protected String middleName;
    protected String honorificPrefix;
    protected String honorificSuffix;
    protected String nickName;
    protected String profileUrl;
    protected String timezone;
    protected String locale;
    protected List<String> ims;
    protected Boolean active;

    private static List mandatoryProperties = null;
    private static List transientProperties = null;
    private static List propertyNames = null;
    private static HashMap dataTypeMap = null;
    private static ArrayList superTypeList = null;
    private static HashSet subTypeList = null;
    protected Map<String, Object> extendedPropertiesValue = new HashMap<String, Object>();
    private static Map<String, String> extendedPropertiesDatatype = new HashMap<String, String>();
    private static Map<String, Object> extendedPropertiesDefaultValue = new HashMap<String, Object>();
    private static Set<String> extendedMultiValuedProperties = new HashSet<String>();

    static {
        setMandatoryPropertyNames();
        setTransientPropertyNames();
        getTransientProperties();
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

    /**
     * Gets the value of the honorificSuffix property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getHonorificSuffix() {
        return honorificSuffix;
    }

    /**
     * Sets the value of the honorificSuffix property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setHonorificSuffix(String value) {
        this.honorificSuffix = value;
    }

    public boolean isSetHonorificSuffix() {
        return (this.honorificSuffix != null);
    }

    public void unsetHonorificSuffix() {
        this.honorificSuffix = null;
    }

    /**
     * Gets the value of the honorificPrefix property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getHonorificPrefix() {
        return honorificPrefix;
    }

    /**
     * Sets the value of the honorificPrefix property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setHonorificPrefix(String value) {
        this.honorificPrefix = value;
    }

    public boolean isSetHonorificPrefix() {
        return (this.honorificPrefix != null);
    }

    public void unsetHonorificPrefix() {
        this.honorificPrefix = null;
    }

    /**
     * Gets the value of the middleName property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * Sets the value of the middleName property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setMiddleName(String value) {
        this.middleName = value;
    }

    public boolean isSetMiddleName() {
        return (this.middleName != null);
    }

    public void unsetMiddleName() {
        this.middleName = null;
    }

    /**
     * Gets the value of the nickName property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * Sets the value of the nickName property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setNickName(String value) {
        this.nickName = value;
    }

    public boolean isSetNickName() {
        return (this.nickName != null);
    }

    public void unsetNickName() {
        this.nickName = null;
    }

    /**
     * Gets the value of the profileUrl property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getProfileUrl() {
        return profileUrl;
    }

    /**
     * Sets the value of the profileUrl property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setProfileUrl(String value) {
        this.profileUrl = value;
    }

    public boolean isSetProfileUrl() {
        return (this.profileUrl != null);
    }

    public void unsetProfileUrl() {
        this.profileUrl = null;
    }

    /**
     * Gets the value of the timezone property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getTimezone() {
        return timezone;
    }

    /**
     * Sets the value of the timezone property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setTimezone(String value) {
        this.timezone = value;
    }

    public boolean isSetTimzone() {
        return (this.timezone != null);
    }

    public void unsetTimezone() {
        this.timezone = null;
    }

    /**
     * Gets the value of the locale property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getLocale() {
        return locale;
    }

    /**
     * Sets the value of the locale property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setLocale(String value) {
        this.locale = value;
    }

    public boolean isSetLocale() {
        return (this.locale != null);
    }

    public void unsetLocale() {
        this.locale = null;
    }

    /**
     * Gets the value of the active property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * Sets the value of the active property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setActive(Boolean value) {
        this.active = value;
    }

    public boolean isSetActive() {
        return (this.active != null);
    }

    public void unsetActive() {
        this.active = null;
    }

    /**
     * @return the homeStreet
     */
    public String getHomeStreet() {
        return homeStreet;
    }

    /**
     * @param homeStreet the homeStreet to set
     */
    public void setHomeStreet(String homeStreet) {
        this.homeStreet = homeStreet;
    }

    public boolean isSetHomeStreet() {
        return (this.homeStreet != null);
    }

    public void unsetHomeStreet() {
        homeStreet = null;
    }

    /**
     * @return the homeCity
     */
    public String getHomeCity() {
        return homeCity;
    }

    /**
     * @param homeCity the homeCity to set
     */
    public void setHomeCity(String homeCity) {
        this.homeCity = homeCity;
    }

    public boolean isSetHomeCity() {
        return (this.homeCity != null);
    }

    public void unsetHomeCity() {
        homeCity = null;
    }

    /**
     * @return the homeStateOrProvinceName
     */
    public String getHomeStateOrProvinceName() {
        return homeStateOrProvinceName;
    }

    /**
     * @param homeStateOrProvinceName the homeStateOrProvinceName to set
     */
    public void setHomeStateOrProvinceName(String homeStateOrProvinceName) {
        this.homeStateOrProvinceName = homeStateOrProvinceName;
    }

    public boolean isSetHomeStateOrProvinceName() {
        return (this.homeStateOrProvinceName != null);
    }

    public void unsetHomeStateOrProvinceName() {
        homeStateOrProvinceName = null;
    }

    /**
     * @return the homePostalCode
     */
    public String getHomePostalCode() {
        return homePostalCode;
    }

    /**
     * @param homePostalCode the homePostalCode to set
     */
    public void setHomePostalCode(String homePostalCode) {
        this.homePostalCode = homePostalCode;
    }

    public boolean isSetHomePostalCode() {
        return (this.homePostalCode != null);
    }

    public void unsetHomePostalCode() {
        homePostalCode = null;
    }

    /**
     * @return the homeCountryName
     */
    public String getHomeCountryName() {
        return homeCountryName;
    }

    /**
     * @param homeCountryName the homeCountryName to set
     */
    public void setHomeCountryName(String homeCountryName) {
        this.homeCountryName = homeCountryName;
    }

    public boolean isSetHomeCountryName() {
        return (this.homeCountryName != null);
    }

    public void unsetHomeCountryName() {
        homeCountryName = null;
    }

    /**
     * @return the businessStreet
     */
    public String getBusinessStreet() {
        return businessStreet;
    }

    /**
     * @param businessStreet the businessStreet to set
     */
    public void setBusinessStreet(String businessStreet) {
        this.businessStreet = businessStreet;
    }

    public boolean isSetBusinessStreet() {
        return (this.businessStreet != null);
    }

    public void unsetBusinessStreet() {
        businessStreet = null;
    }

    /**
     * @return the businessCity
     */
    public String getBusinessCity() {
        return businessCity;
    }

    /**
     * @param businessCity the businessCity to set
     */
    public void setBusinessCity(String businessCity) {
        this.businessCity = businessCity;
    }

    public boolean isSetBusinessCity() {
        return (this.businessCity != null);
    }

    public void unsetBusinessCity() {
        businessCity = null;
    }

    /**
     * @return the businessStateOrProvinceName
     */
    public String getBusinessStateOrProvinceName() {
        return businessStateOrProvinceName;
    }

    /**
     * @param businessStateOrProvinceName the businessStateOrProvinceName to set
     */
    public void setBusinessStateOrProvinceName(String businessStateOrProvinceName) {
        this.businessStateOrProvinceName = businessStateOrProvinceName;
    }

    public boolean isSetBusinessStateOrProvinceName() {
        return (this.businessStateOrProvinceName != null);
    }

    public void unsetBusinessStateOrProvinceName() {
        businessStateOrProvinceName = null;
    }

    /**
     * @return the businessPostalCode
     */
    public String getBusinessPostalCode() {
        return businessPostalCode;
    }

    /**
     * @param businessPostalCode the businessPostalCode to set
     */
    public void setBusinessPostalCode(String businessPostalCode) {
        this.businessPostalCode = businessPostalCode;
    }

    public boolean isSetBusinessPostalCode() {
        return (this.businessPostalCode != null);
    }

    public void unsetBusinessPostalCode() {
        businessPostalCode = null;
    }

    /**
     * @return the businessCountryName
     */
    public String getBusinessCountryName() {
        return businessCountryName;
    }

    /**
     * @param businessCountryName the businessCountryName to set
     */
    public void setBusinessCountryName(String businessCountryName) {
        this.businessCountryName = businessCountryName;
    }

    public boolean isSetBusinessCountryName() {
        return (this.businessCountryName != null);
    }

    public void unsetBusinessCountryName() {
        businessCountryName = null;
    }

    public List<String> getIMs() {
        if (ims == null) {
            ims = new ArrayList<String>();
        }
        return this.ims;
    }

    public boolean isSetIMs() {
        return ((this.ims != null) && (!this.ims.isEmpty()));
    }

    public void unsetIMs() {
        this.ims = null;
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
        if (propName.equals("homeStreet")) {
            return getHomeStreet();
        }
        if (propName.equals("homeCity")) {
            return getHomeCity();
        }
        if (propName.equals("homeStateOrProvinceName")) {
            return getHomeStateOrProvinceName();
        }
        if (propName.equals("homePostalCode")) {
            return getHomePostalCode();
        }
        if (propName.equals("homeCountryName")) {
            return getHomeCountryName();
        }
        if (propName.equals("businessStreet")) {
            return getBusinessStreet();
        }
        if (propName.equals("businessCity")) {
            return getBusinessCity();
        }
        if (propName.equals("businessStateOrProvinceName")) {
            return getBusinessStateOrProvinceName();
        }
        if (propName.equals("businessPostalCode")) {
            return getBusinessPostalCode();
        }
        if (propName.equals("businessCountryName")) {
            return getBusinessCountryName();
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
        if (propName.equals("middleName")) {
            return getMiddleName();
        }
        if (propName.equals("honorificPrefix")) {
            return getHonorificPrefix();
        }
        if (propName.equals("honorificSuffix")) {
            return getHonorificSuffix();
        }
        if (propName.equals("nickName")) {
            return getNickName();
        }
        if (propName.equals("profileUrl")) {
            return getProfileUrl();
        }
        if (propName.equals("timezone")) {
            return getTimezone();
        }
        if (propName.equals("locale")) {
            return getLocale();
        }
        if (propName.equals("active")) {
            return getActive();
        }
        if (propName.equals("ims")) {
            return getIMs();
        }

        if (extendedPropertiesDatatype.containsKey(propName))
            return getExtendedProperty(propName);

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
        if (propName.equals("homeStreet")) {
            return isSetHomeStreet();
        }
        if (propName.equals("homeCity")) {
            return isSetHomeCity();
        }
        if (propName.equals("homeStateOrProvinceName")) {
            return isSetHomeStateOrProvinceName();
        }
        if (propName.equals("homePostalCode")) {
            return isSetHomePostalCode();
        }
        if (propName.equals("homeCountryName")) {
            return isSetHomeCountryName();
        }
        if (propName.equals("businessStreet")) {
            return isSetBusinessStreet();
        }
        if (propName.equals("businessCity")) {
            return isSetBusinessCity();
        }
        if (propName.equals("businessStateOrProvinceName")) {
            return isSetBusinessStateOrProvinceName();
        }
        if (propName.equals("businessPostalCode")) {
            return isSetBusinessPostalCode();
        }
        if (propName.equals("businessCountryName")) {
            return isSetBusinessCountryName();
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
        if (propName.equals("middleName")) {
            return isSetMiddleName();
        }
        if (propName.equals("honorificPrefix")) {
            return isSetHonorificPrefix();
        }
        if (propName.equals("honorificSuffix")) {
            return isSetHonorificSuffix();
        }
        if (propName.equals("nickName")) {
            return isSetNickName();
        }
        if (propName.equals("profileUrl")) {
            return isSetProfileUrl();
        }
        if (propName.equals("timezone")) {
            return isSetTimzone();
        }
        if (propName.equals("locale")) {
            return isSetLocale();
        }
        if (propName.equals("active")) {
            return isSetActive();
        }
        if (propName.equals("ims")) {
            return isSetIMs();
        }

        if (extendedPropertiesDatatype.containsKey(propName))
            return isSetExtendedProperty(propName);

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
        if (propName.equals("homeStreet")) {
            setHomeStreet((String) value);
        }
        if (propName.equals("homeCity")) {
            setHomeCity((String) value);
        }
        if (propName.equals("homeStateOrProvinceName")) {
            setHomeStateOrProvinceName((String) value);
        }
        if (propName.equals("homePostalCode")) {
            setHomePostalCode((String) value);
        }
        if (propName.equals("homeCountryName")) {
            setHomeCountryName((String) value);
        }
        if (propName.equals("businessStreet")) {
            setBusinessStreet((String) value);
        }
        if (propName.equals("businessCity")) {
            setBusinessCity((String) value);
        }
        if (propName.equals("businessStateOrProvinceName")) {
            setBusinessStateOrProvinceName((String) value);
        }
        if (propName.equals("businessPostalCode")) {
            setBusinessPostalCode((String) value);
        }
        if (propName.equals("businessCountryName")) {
            setBusinessCountryName((String) value);
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
        if (propName.equals("middleName")) {
            setMiddleName((String) value);
        }
        if (propName.equals("honorificPrefix")) {
            setHonorificPrefix((String) value);
        }
        if (propName.equals("honorificSuffix")) {
            setHonorificSuffix((String) value);
        }
        if (propName.equals("nickName")) {
            setNickName((String) value);
        }
        if (propName.equals("profileUrl")) {
            setProfileUrl((String) value);
        }
        if (propName.equals("timezone")) {
            setTimezone((String) value);
        }
        if (propName.equals("locale")) {
            setLocale((String) value);
        }
        if (propName.equals("active")) {
            setActive((Boolean) value);
        }
        if (propName.equals("ims")) {
            getIMs().add(((String) value));
        }

        if (extendedPropertiesDatatype.containsKey(propName))
            setExtendedProperty(propName, value);

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
        if (propName.equals("homeStreet")) {
            unsetHomeStreet();
        }
        if (propName.equals("homeCity")) {
            unsetHomeCity();
        }
        if (propName.equals("homeStateOrProvinceName")) {
            unsetHomeStateOrProvinceName();
        }
        if (propName.equals("homePostalCode")) {
            unsetHomePostalAddress();
        }
        if (propName.equals("homeCountryName")) {
            unsetHomeCountryName();
        }
        if (propName.equals("businessStreet")) {
            unsetBusinessStreet();
        }
        if (propName.equals("businessCity")) {
            unsetBusinessCity();
        }
        if (propName.equals("businessStateOrProvinceName")) {
            unsetBusinessStateOrProvinceName();
        }
        if (propName.equals("businessPostalCode")) {
            unsetBusinessPostalCode();
        }
        if (propName.equals("businessCountryName")) {
            unsetBusinessCountryName();
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
        if (propName.equals("middleName")) {
            unsetMiddleName();
        }
        if (propName.equals("honorificPrefix")) {
            unsetHonorificPrefix();
        }
        if (propName.equals("honorificSuffix")) {
            unsetHonorificSuffix();
        }
        if (propName.equals("nickName")) {
            unsetNickName();
        }
        if (propName.equals("profileUrl")) {
            unsetProfileUrl();
        }
        if (propName.equals("timezone")) {
            unsetTimezone();
        }
        if (propName.equals("locale")) {
            unsetLocale();
        }
        if (propName.equals("active")) {
            unsetActive();
        }
        if (propName.equals("ims")) {
            unsetIMs();
        }

        if (extendedPropertiesDatatype.containsKey(propName))
            unSetExtendedProperty(propName);

        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return "PersonAccount";
    }

    private static synchronized void setMandatoryPropertyNames() {
        if (mandatoryProperties != null) {
            return;
        }
        mandatoryProperties = new ArrayList();
        mandatoryProperties.add("sn");
        mandatoryProperties.add("cn");
    }

    private static synchronized void setTransientPropertyNames() {
        if (transientProperties != null) {
            return;
        }
        transientProperties = new ArrayList();
        transientProperties.addAll(LoginAccount.getTransientProperties());
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

    protected static List getTransientProperties() {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        return transientProperties;
    }

    public static synchronized void reInitializePropertyNames() {
        propertyNames = null;
        Entity.reInitializePropertyNames();
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
                names.add("description");
                names.add("businessCategory");
                names.add("seeAlso");
                names.add("kerberosId");
                names.add("photoURL");
                names.add("photoURLThumbnail");
                names.add("middleName");
                names.add("honorificPrefix");
                names.add("honorificSuffix");
                names.add("nickName");
                names.add("profileUrl");
                names.add("timezone");
                names.add("locale");
                names.add("active");
                names.add("homeStreet");
                names.add("homeCity");
                names.add("homeStateOrProvinceName");
                names.add("homePostalCode");
                names.add("homeCountryName");
                names.add("businessStreet");
                names.add("businessCity");
                names.add("businessStateOrProvinceName");
                names.add("businessPostalCode");
                names.add("businessCountryName");
                names.add("ims");
                if (extendedPropertiesDatatype != null && extendedPropertiesDatatype.keySet().size() > 0)
                    names.addAll(extendedPropertiesDatatype.keySet());
                names.addAll(LoginAccount.getPropertyNames("LoginAccount"));
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
        dataTypeMap.put("description", "String");
        dataTypeMap.put("businessCategory", "String");
        dataTypeMap.put("seeAlso", "String");
        dataTypeMap.put("kerberosId", "String");
        dataTypeMap.put("photoURL", "String");
        dataTypeMap.put("photoURLThumbnail", "String");
        dataTypeMap.put("middleName", "String");
        dataTypeMap.put("honorificPrefix", "String");
        dataTypeMap.put("honorificSuffix", "String");
        dataTypeMap.put("nickName", "String");
        dataTypeMap.put("profileUrl", "String");
        dataTypeMap.put("timezone", "String");
        dataTypeMap.put("locale", "String");
        dataTypeMap.put("active", "Boolean");
        dataTypeMap.put("homeStreet", "String");
        dataTypeMap.put("homeCity", "String");
        dataTypeMap.put("homeStateOrProvinceName", "String");
        dataTypeMap.put("homePostalCode", "String");
        dataTypeMap.put("homeCountryName", "String");
        dataTypeMap.put("businessStreet", "String");
        dataTypeMap.put("businessCity", "String");
        dataTypeMap.put("businessStateOrProvinceName", "String");
        dataTypeMap.put("businessPostalCode", "String");
        dataTypeMap.put("businessCountryName", "String");
        dataTypeMap.put("ims", "String");
    }

    @Override
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return ((String) dataTypeMap.get(propName));
        }
        else if (extendedPropertiesDatatype.containsKey(propName)) {
            return extendedPropertiesDatatype.get(propName);
        }
        else {
            return super.getDataType(propName);
        }
    }

    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList();
        }
        superTypeList.add("LoginAccount");
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

    private Object getExtendedProperty(String propName) {
        if (extendedPropertiesValue.containsKey(propName))
            return extendedPropertiesValue.get(propName);
        else if (extendedPropertiesDefaultValue.containsKey(propName))
            return extendedPropertiesDefaultValue.get(propName);
        else
            return null;
    }

    /**
     * @param property
     * @return
     */
    private boolean isSetExtendedProperty(String property) {
        if (extendedPropertiesValue.containsKey(property) || extendedPropertiesDefaultValue.containsKey(property))
            return true;
        else
            return false;
    }

    /**
     * @param property
     */
    private void unSetExtendedProperty(String property) {
        extendedPropertiesValue.remove(property);
    }

    private void setExtendedProperty(String property, Object value) {
        String dataType = extendedPropertiesDatatype.get(property);
        String valueClass = value.getClass().getSimpleName();
        if (dataType.equals(valueClass) && !extendedMultiValuedProperties.contains(property))
            extendedPropertiesValue.put(property, value);
        else if (dataType.equals(valueClass) && extendedMultiValuedProperties.contains(property)) {
            if (value instanceof List) {
                extendedPropertiesValue.put(property, value);
            }
            else {
                List<Object> values = (List<Object>) extendedPropertiesValue.get(property);
                if (values == null) {
                    values = new ArrayList<Object>();
                    values.add(value);
                    extendedPropertiesValue.put(property, values);
                }
            }
        }
        else
            throw new ClassCastException(value + " is not of type " + dataType);
    }

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

    /**
     * Allows for an extended property, or a property not pre-defined as part of this PersonAccount entity type, to be
     * added to the PersonAccount entity
     * 
     * @param propName: name of property
     *            <ul><li>allowed object is a {@link String}</li></ul>
     * @param dataType: Java type of property
     *            <ul><li>allowed object is a {@link String}</li></ul>
     * @param multiValued: describes if the property is a single valued or multi-valued property
     *            <ul><li>allowed object is a {@link boolean}</li></ul>
     * @param defaultValue: defines the default value for this property
     *            <ul><li>allowed object is a {@link Object}</li></ul>
     * 
     */
    public static void addExtendedProperty(String propName, String dataType, boolean multiValued, Object defaultValue) {
        if (dataType == null || "null".equalsIgnoreCase(dataType))
            return;

        extendedPropertiesDatatype.put(propName, dataType);
        if (defaultValue != null)
            extendedPropertiesDefaultValue.put(propName, defaultValue);
        if (multiValued)
            extendedMultiValuedProperties.add(propName);
    }

    /**
     * Removes all extended properties defined in this PersonAccount entity
     */
    public static void clearExtendedProperties() {
        extendedPropertiesDatatype.clear();
        extendedPropertiesDefaultValue.clear();
        extendedMultiValuedProperties.clear();
    }

    /**
     * Returns a list of extended property names added to this PersonAccount entity
     * 
     * @return
     *         returned object is a {@link Set}
     */

    public Set<String> getExtendedPropertyNames() {
        return new HashSet<String>(extendedPropertiesDatatype.keySet());
    }
}
