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
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
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
@Trivial
public class Person extends Party {
    private static final String PROP_UID = "uid";
    private static final String PROP_CN = "cn";
    private static final String PROP_SN = "sn";
    private static final String PROP_PREFERRED_LANGUAGE = "preferredLanguage";
    private static final String PROP_DISPLAY_NAME = "displayName";
    private static final String PROP_INITIALS = "initials";
    private static final String PROP_MAIL = "mail";
    private static final String PROP_IBM_PRIMARY_EMAIL = "ibmPrimaryEmail";
    private static final String PROP_JPEG_PHOTO = "jpegPhoto";
    private static final String PROP_LABELED_URI = "labeledURI";
    private static final String PROP_CAR_LICENSE = "carLicense";
    private static final String PROP_TELEPHONE_NUMBER = "telephoneNumber";
    private static final String PROP_FACSIMILE_TELEPHONE_NUMBER = "facsimileTelephoneNumber";
    private static final String PROP_PAGER = "pager";
    private static final String PROP_MOBILE = "mobile";
    private static final String PROP_HOME_POSTAL_ADDRESS = "homePostalAddress";
    private static final String PROP_POSTAL_ADDRESS = "postalAddress";
    private static final String PROP_ROOM_NUMBER = "roomNumber";
    private static final String PROP_L = "l";
    private static final String PROP_LOCALITY_NAME = "localityName";
    private static final String PROP_ST = "st";
    private static final String PROP_STATE_OR_PROVINCE_NAME = "stateOrProvinceName";
    private static final String PROP_STREET = "street";
    private static final String PROP_POSTAL_CODE = "postalCode";
    private static final String PROP_CITY = "city";
    private static final String PROP_EMPLOYEE_TYPE = "employeeType";
    private static final String PROP_EMPLOYEE_NUMBER = "employeeNumber";
    private static final String PROP_MANAGER = "manager";
    private static final String PROP_SECRETARY = "secretary";
    private static final String PROP_DEPARTMENT_NUMBER = "departmentNumber";
    private static final String PROP_TITLE = "title";
    private static final String PROP_IBM_JOB_TITLE = "ibmJobTitle";
    private static final String PROP_C = "c";
    private static final String PROP_COUNTRY_NAME = "countryName";
    private static final String PROP_GIVEN_NAME = "givenName";
    private static final String PROP_HOME_ADDRESS = "homeAddress";
    private static final String PROP_BUSINESS_ADDRESS = "businessAddress";
    private static final String PROP_DESCRIPTION = "description";
    private static final String PROP_BUSINESS_CATEGORY = "businessCategory";
    private static final String PROP_SEE_ALSO = "seeAlso";
    private static final String PROP_KERBEROS_ID = "kerberosId";
    private static final String PROP_PHOTO_URL = "photoURL";
    private static final String PROP_PHOTO_URL_THUMBNAIL = "photoURLThumbnail";

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

    /** The set of multi-valued properties for this entity type. */
    private static final Set<String> MULTI_VALUED_PROPERTIES;

    static {
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();

        MULTI_VALUED_PROPERTIES = new HashSet<String>();
        MULTI_VALUED_PROPERTIES.add(PROP_DISPLAY_NAME);
        MULTI_VALUED_PROPERTIES.add(PROP_INITIALS);
        MULTI_VALUED_PROPERTIES.add(PROP_JPEG_PHOTO);
        MULTI_VALUED_PROPERTIES.add(PROP_CAR_LICENSE);
        MULTI_VALUED_PROPERTIES.add(PROP_TELEPHONE_NUMBER);
        MULTI_VALUED_PROPERTIES.add(PROP_FACSIMILE_TELEPHONE_NUMBER);
        MULTI_VALUED_PROPERTIES.add(PROP_PAGER);
        MULTI_VALUED_PROPERTIES.add(PROP_MOBILE);
        MULTI_VALUED_PROPERTIES.add(PROP_HOME_POSTAL_ADDRESS);
        MULTI_VALUED_PROPERTIES.add(PROP_POSTAL_ADDRESS);
        MULTI_VALUED_PROPERTIES.add(PROP_ROOM_NUMBER);
        MULTI_VALUED_PROPERTIES.add(PROP_L);
        MULTI_VALUED_PROPERTIES.add(PROP_LOCALITY_NAME);
        MULTI_VALUED_PROPERTIES.add(PROP_ST);
        MULTI_VALUED_PROPERTIES.add(PROP_STATE_OR_PROVINCE_NAME);
        MULTI_VALUED_PROPERTIES.add(PROP_STREET);
        MULTI_VALUED_PROPERTIES.add(PROP_POSTAL_CODE);
        MULTI_VALUED_PROPERTIES.add(PROP_CITY);
        MULTI_VALUED_PROPERTIES.add(PROP_MANAGER);
        MULTI_VALUED_PROPERTIES.add(PROP_SECRETARY);
        MULTI_VALUED_PROPERTIES.add(PROP_DEPARTMENT_NUMBER);
        MULTI_VALUED_PROPERTIES.add(PROP_TITLE);
        MULTI_VALUED_PROPERTIES.add(PROP_IBM_JOB_TITLE);
        MULTI_VALUED_PROPERTIES.add(PROP_C);
        MULTI_VALUED_PROPERTIES.add(PROP_COUNTRY_NAME);
        MULTI_VALUED_PROPERTIES.add(PROP_GIVEN_NAME);
        MULTI_VALUED_PROPERTIES.add(PROP_HOME_ADDRESS);
        MULTI_VALUED_PROPERTIES.add(PROP_BUSINESS_ADDRESS);
        MULTI_VALUED_PROPERTIES.add(PROP_DESCRIPTION);
        MULTI_VALUED_PROPERTIES.add(PROP_BUSINESS_CATEGORY);
        MULTI_VALUED_PROPERTIES.add(PROP_SEE_ALSO);
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
        if (propName.equals(PROP_UID)) {
            return getUid();
        }
        if (propName.equals(PROP_CN)) {
            return getCn();
        }
        if (propName.equals(PROP_SN)) {
            return getSn();
        }
        if (propName.equals(PROP_PREFERRED_LANGUAGE)) {
            return getPreferredLanguage();
        }
        if (propName.equals(PROP_DISPLAY_NAME)) {
            return getDisplayName();
        }
        if (propName.equals(PROP_INITIALS)) {
            return getInitials();
        }
        if (propName.equals(PROP_MAIL)) {
            return getMail();
        }
        if (propName.equals(PROP_IBM_PRIMARY_EMAIL)) {
            return getIbmPrimaryEmail();
        }
        if (propName.equals(PROP_JPEG_PHOTO)) {
            return getJpegPhoto();
        }
        if (propName.equals(PROP_LABELED_URI)) {
            return getLabeledURI();
        }
        if (propName.equals(PROP_CAR_LICENSE)) {
            return getCarLicense();
        }
        if (propName.equals(PROP_TELEPHONE_NUMBER)) {
            return getTelephoneNumber();
        }
        if (propName.equals(PROP_FACSIMILE_TELEPHONE_NUMBER)) {
            return getFacsimileTelephoneNumber();
        }
        if (propName.equals(PROP_PAGER)) {
            return getPager();
        }
        if (propName.equals(PROP_MOBILE)) {
            return getMobile();
        }
        if (propName.equals(PROP_HOME_POSTAL_ADDRESS)) {
            return getHomePostalAddress();
        }
        if (propName.equals(PROP_POSTAL_ADDRESS)) {
            return getPostalAddress();
        }
        if (propName.equals(PROP_ROOM_NUMBER)) {
            return getRoomNumber();
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
        if (propName.equals(PROP_CITY)) {
            return getCity();
        }
        if (propName.equals(PROP_EMPLOYEE_TYPE)) {
            return getEmployeeType();
        }
        if (propName.equals(PROP_EMPLOYEE_NUMBER)) {
            return getEmployeeNumber();
        }
        if (propName.equals(PROP_MANAGER)) {
            return getManager();
        }
        if (propName.equals(PROP_SECRETARY)) {
            return getSecretary();
        }
        if (propName.equals(PROP_DEPARTMENT_NUMBER)) {
            return getDepartmentNumber();
        }
        if (propName.equals(PROP_TITLE)) {
            return getTitle();
        }
        if (propName.equals(PROP_IBM_JOB_TITLE)) {
            return getIbmJobTitle();
        }
        if (propName.equals(PROP_C)) {
            return getC();
        }
        if (propName.equals(PROP_COUNTRY_NAME)) {
            return getCountryName();
        }
        if (propName.equals(PROP_GIVEN_NAME)) {
            return getGivenName();
        }
        if (propName.equals(PROP_HOME_ADDRESS)) {
            return getHomeAddress();
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
        if (propName.equals(PROP_KERBEROS_ID)) {
            return getKerberosId();
        }
        if (propName.equals(PROP_PHOTO_URL)) {
            return getPhotoUrl();
        }
        if (propName.equals(PROP_PHOTO_URL_THUMBNAIL)) {
            return getPhotoUrlThumbnail();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_UID)) {
            return isSetUid();
        }
        if (propName.equals(PROP_CN)) {
            return isSetCn();
        }
        if (propName.equals(PROP_SN)) {
            return isSetSn();
        }
        if (propName.equals(PROP_PREFERRED_LANGUAGE)) {
            return isSetPreferredLanguage();
        }
        if (propName.equals(PROP_DISPLAY_NAME)) {
            return isSetDisplayName();
        }
        if (propName.equals(PROP_INITIALS)) {
            return isSetInitials();
        }
        if (propName.equals(PROP_MAIL)) {
            return isSetMail();
        }
        if (propName.equals(PROP_IBM_PRIMARY_EMAIL)) {
            return isSetIbmPrimaryEmail();
        }
        if (propName.equals(PROP_JPEG_PHOTO)) {
            return isSetJpegPhoto();
        }
        if (propName.equals(PROP_LABELED_URI)) {
            return isSetLabeledURI();
        }
        if (propName.equals(PROP_CAR_LICENSE)) {
            return isSetCarLicense();
        }
        if (propName.equals(PROP_TELEPHONE_NUMBER)) {
            return isSetTelephoneNumber();
        }
        if (propName.equals(PROP_FACSIMILE_TELEPHONE_NUMBER)) {
            return isSetFacsimileTelephoneNumber();
        }
        if (propName.equals(PROP_PAGER)) {
            return isSetPager();
        }
        if (propName.equals(PROP_MOBILE)) {
            return isSetMobile();
        }
        if (propName.equals(PROP_HOME_POSTAL_ADDRESS)) {
            return isSetHomePostalAddress();
        }
        if (propName.equals(PROP_POSTAL_ADDRESS)) {
            return isSetPostalAddress();
        }
        if (propName.equals(PROP_ROOM_NUMBER)) {
            return isSetRoomNumber();
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
        if (propName.equals(PROP_CITY)) {
            return isSetCity();
        }
        if (propName.equals(PROP_EMPLOYEE_TYPE)) {
            return isSetEmployeeType();
        }
        if (propName.equals(PROP_EMPLOYEE_NUMBER)) {
            return isSetEmployeeNumber();
        }
        if (propName.equals(PROP_MANAGER)) {
            return isSetManager();
        }
        if (propName.equals(PROP_SECRETARY)) {
            return isSetSecretary();
        }
        if (propName.equals(PROP_DEPARTMENT_NUMBER)) {
            return isSetDepartmentNumber();
        }
        if (propName.equals(PROP_TITLE)) {
            return isSetTitle();
        }
        if (propName.equals(PROP_IBM_JOB_TITLE)) {
            return isSetIbmJobTitle();
        }
        if (propName.equals(PROP_C)) {
            return isSetC();
        }
        if (propName.equals(PROP_COUNTRY_NAME)) {
            return isSetCountryName();
        }
        if (propName.equals(PROP_GIVEN_NAME)) {
            return isSetGivenName();
        }
        if (propName.equals(PROP_HOME_ADDRESS)) {
            return isSetHomeAddress();
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
        if (propName.equals(PROP_KERBEROS_ID)) {
            return isSetKerberosId();
        }
        if (propName.equals(PROP_PHOTO_URL)) {
            return isSetPhotoUrl();
        }
        if (propName.equals(PROP_PHOTO_URL_THUMBNAIL)) {
            return isSetPhotoUrlThumbnail();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_UID)) {
            setUid(((String) value));
        }
        if (propName.equals(PROP_CN)) {
            setCn(((String) value));
        }
        if (propName.equals(PROP_SN)) {
            setSn(((String) value));
        }
        if (propName.equals(PROP_PREFERRED_LANGUAGE)) {
            setPreferredLanguage(((String) value));
        }
        if (propName.equals(PROP_DISPLAY_NAME)) {
            getDisplayName().add(((String) value));
        }
        if (propName.equals(PROP_INITIALS)) {
            getInitials().add(((String) value));
        }
        if (propName.equals(PROP_MAIL)) {
            setMail(((String) value));
        }
        if (propName.equals(PROP_IBM_PRIMARY_EMAIL)) {
            setIbmPrimaryEmail(((String) value));
        }
        if (propName.equals(PROP_JPEG_PHOTO)) {
            getJpegPhoto().add(((byte[]) value));
        }
        if (propName.equals(PROP_LABELED_URI)) {
            setLabeledURI(((String) value));
        }
        if (propName.equals(PROP_CAR_LICENSE)) {
            getCarLicense().add(((String) value));
        }
        if (propName.equals(PROP_TELEPHONE_NUMBER)) {
            getTelephoneNumber().add(((String) value));
        }
        if (propName.equals(PROP_FACSIMILE_TELEPHONE_NUMBER)) {
            getFacsimileTelephoneNumber().add(((String) value));
        }
        if (propName.equals(PROP_PAGER)) {
            getPager().add(((String) value));
        }
        if (propName.equals(PROP_MOBILE)) {
            getMobile().add(((String) value));
        }
        if (propName.equals(PROP_HOME_POSTAL_ADDRESS)) {
            getHomePostalAddress().add(((String) value));
        }
        if (propName.equals(PROP_POSTAL_ADDRESS)) {
            getPostalAddress().add(((String) value));
        }
        if (propName.equals(PROP_ROOM_NUMBER)) {
            getRoomNumber().add(((String) value));
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
        if (propName.equals(PROP_CITY)) {
            getCity().add(((String) value));
        }
        if (propName.equals(PROP_EMPLOYEE_TYPE)) {
            setEmployeeType(((String) value));
        }
        if (propName.equals(PROP_EMPLOYEE_NUMBER)) {
            setEmployeeNumber(((String) value));
        }
        if (propName.equals(PROP_MANAGER)) {
            getManager().add(((com.ibm.wsspi.security.wim.model.IdentifierType) value));
        }
        if (propName.equals(PROP_SECRETARY)) {
            getSecretary().add(((com.ibm.wsspi.security.wim.model.IdentifierType) value));
        }
        if (propName.equals(PROP_DEPARTMENT_NUMBER)) {
            getDepartmentNumber().add(((String) value));
        }
        if (propName.equals(PROP_TITLE)) {
            getTitle().add(((String) value));
        }
        if (propName.equals(PROP_IBM_JOB_TITLE)) {
            getIbmJobTitle().add(((String) value));
        }
        if (propName.equals(PROP_C)) {
            getC().add(((String) value));
        }
        if (propName.equals(PROP_COUNTRY_NAME)) {
            getCountryName().add(((String) value));
        }
        if (propName.equals(PROP_GIVEN_NAME)) {
            getGivenName().add(((String) value));
        }
        if (propName.equals(PROP_HOME_ADDRESS)) {
            getHomeAddress().add(((com.ibm.wsspi.security.wim.model.AddressType) value));
        }
        if (propName.equals(PROP_BUSINESS_ADDRESS)) {
            getBusinessAddress().add(((com.ibm.wsspi.security.wim.model.AddressType) value));
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
        if (propName.equals(PROP_KERBEROS_ID)) {
            setKerberosId(((String) value));
        }
        if (propName.equals(PROP_PHOTO_URL)) {
            setPhotoUrl(((String) value));
        }
        if (propName.equals(PROP_PHOTO_URL_THUMBNAIL)) {
            setPhotoUrlThumbnail(((String) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_DISPLAY_NAME)) {
            unsetDisplayName();
        }
        if (propName.equals(PROP_INITIALS)) {
            unsetInitials();
        }
        if (propName.equals(PROP_JPEG_PHOTO)) {
            unsetJpegPhoto();
        }
        if (propName.equals(PROP_CAR_LICENSE)) {
            unsetCarLicense();
        }
        if (propName.equals(PROP_TELEPHONE_NUMBER)) {
            unsetTelephoneNumber();
        }
        if (propName.equals(PROP_FACSIMILE_TELEPHONE_NUMBER)) {
            unsetFacsimileTelephoneNumber();
        }
        if (propName.equals(PROP_PAGER)) {
            unsetPager();
        }
        if (propName.equals(PROP_MOBILE)) {
            unsetMobile();
        }
        if (propName.equals(PROP_HOME_POSTAL_ADDRESS)) {
            unsetHomePostalAddress();
        }
        if (propName.equals(PROP_POSTAL_ADDRESS)) {
            unsetPostalAddress();
        }
        if (propName.equals(PROP_ROOM_NUMBER)) {
            unsetRoomNumber();
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
        if (propName.equals(PROP_CITY)) {
            unsetCity();
        }
        if (propName.equals(PROP_MANAGER)) {
            unsetManager();
        }
        if (propName.equals(PROP_SECRETARY)) {
            unsetSecretary();
        }
        if (propName.equals(PROP_DEPARTMENT_NUMBER)) {
            unsetDepartmentNumber();
        }
        if (propName.equals(PROP_TITLE)) {
            unsetTitle();
        }
        if (propName.equals(PROP_IBM_JOB_TITLE)) {
            unsetIbmJobTitle();
        }
        if (propName.equals(PROP_C)) {
            unsetC();
        }
        if (propName.equals(PROP_COUNTRY_NAME)) {
            unsetCountryName();
        }
        if (propName.equals(PROP_GIVEN_NAME)) {
            unsetGivenName();
        }
        if (propName.equals(PROP_HOME_ADDRESS)) {
            unsetHomeAddress();
        }
        if (propName.equals(PROP_BUSINESS_ADDRESS)) {
            unsetBusinessAddress();
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
        return "Person";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add(PROP_UID);
                names.add(PROP_CN);
                names.add(PROP_SN);
                names.add(PROP_PREFERRED_LANGUAGE);
                names.add(PROP_DISPLAY_NAME);
                names.add(PROP_INITIALS);
                names.add(PROP_MAIL);
                names.add(PROP_IBM_PRIMARY_EMAIL);
                names.add(PROP_JPEG_PHOTO);
                names.add(PROP_LABELED_URI);
                names.add(PROP_CAR_LICENSE);
                names.add(PROP_TELEPHONE_NUMBER);
                names.add(PROP_FACSIMILE_TELEPHONE_NUMBER);
                names.add(PROP_PAGER);
                names.add(PROP_MOBILE);
                names.add(PROP_HOME_POSTAL_ADDRESS);
                names.add(PROP_POSTAL_ADDRESS);
                names.add(PROP_ROOM_NUMBER);
                names.add(PROP_L);
                names.add(PROP_LOCALITY_NAME);
                names.add(PROP_ST);
                names.add(PROP_STATE_OR_PROVINCE_NAME);
                names.add(PROP_STREET);
                names.add(PROP_POSTAL_CODE);
                names.add(PROP_CITY);
                names.add(PROP_EMPLOYEE_TYPE);
                names.add(PROP_EMPLOYEE_NUMBER);
                names.add(PROP_MANAGER);
                names.add(PROP_SECRETARY);
                names.add(PROP_DEPARTMENT_NUMBER);
                names.add(PROP_TITLE);
                names.add(PROP_IBM_JOB_TITLE);
                names.add(PROP_C);
                names.add(PROP_COUNTRY_NAME);
                names.add(PROP_GIVEN_NAME);
                names.add(PROP_HOME_ADDRESS);
                names.add(PROP_BUSINESS_ADDRESS);
                names.add(PROP_DESCRIPTION);
                names.add(PROP_BUSINESS_CATEGORY);
                names.add(PROP_SEE_ALSO);
                names.add(PROP_KERBEROS_ID);
                names.add(PROP_PHOTO_URL);
                names.add(PROP_PHOTO_URL_THUMBNAIL);
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
        dataTypeMap.put(PROP_UID, "String");
        dataTypeMap.put(PROP_CN, "String");
        dataTypeMap.put(PROP_SN, "String");
        dataTypeMap.put(PROP_PREFERRED_LANGUAGE, "String");
        dataTypeMap.put(PROP_DISPLAY_NAME, "String");
        dataTypeMap.put(PROP_INITIALS, "String");
        dataTypeMap.put(PROP_MAIL, "String");
        dataTypeMap.put(PROP_IBM_PRIMARY_EMAIL, "String");
        dataTypeMap.put(PROP_JPEG_PHOTO, "byte[]");
        dataTypeMap.put(PROP_LABELED_URI, "String");
        dataTypeMap.put(PROP_CAR_LICENSE, "String");
        dataTypeMap.put(PROP_TELEPHONE_NUMBER, "String");
        dataTypeMap.put(PROP_FACSIMILE_TELEPHONE_NUMBER, "String");
        dataTypeMap.put(PROP_PAGER, "String");
        dataTypeMap.put(PROP_MOBILE, "String");
        dataTypeMap.put(PROP_HOME_POSTAL_ADDRESS, "String");
        dataTypeMap.put(PROP_POSTAL_ADDRESS, "String");
        dataTypeMap.put(PROP_ROOM_NUMBER, "String");
        dataTypeMap.put(PROP_L, "String");
        dataTypeMap.put(PROP_LOCALITY_NAME, "String");
        dataTypeMap.put(PROP_ST, "String");
        dataTypeMap.put(PROP_STATE_OR_PROVINCE_NAME, "String");
        dataTypeMap.put(PROP_STREET, "String");
        dataTypeMap.put(PROP_POSTAL_CODE, "String");
        dataTypeMap.put(PROP_CITY, "String");
        dataTypeMap.put(PROP_EMPLOYEE_TYPE, "String");
        dataTypeMap.put(PROP_EMPLOYEE_NUMBER, "String");
        dataTypeMap.put(PROP_MANAGER, "IdentifierType");
        dataTypeMap.put(PROP_SECRETARY, "IdentifierType");
        dataTypeMap.put(PROP_DEPARTMENT_NUMBER, "String");
        dataTypeMap.put(PROP_TITLE, "String");
        dataTypeMap.put(PROP_IBM_JOB_TITLE, "String");
        dataTypeMap.put(PROP_C, "String");
        dataTypeMap.put(PROP_COUNTRY_NAME, "String");
        dataTypeMap.put(PROP_GIVEN_NAME, "String");
        dataTypeMap.put(PROP_HOME_ADDRESS, "AddressType");
        dataTypeMap.put(PROP_BUSINESS_ADDRESS, "AddressType");
        dataTypeMap.put(PROP_DESCRIPTION, "String");
        dataTypeMap.put(PROP_BUSINESS_CATEGORY, "String");
        dataTypeMap.put(PROP_SEE_ALSO, "String");
        dataTypeMap.put(PROP_KERBEROS_ID, "String");
        dataTypeMap.put(PROP_PHOTO_URL, "String");
        dataTypeMap.put(PROP_PHOTO_URL_THUMBNAIL, "String");
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

    @Override
    public boolean isMultiValuedProperty(String propName) {
        return MULTI_VALUED_PROPERTIES.contains(propName) || super.isMultiValuedProperty(propName);
    }
}
