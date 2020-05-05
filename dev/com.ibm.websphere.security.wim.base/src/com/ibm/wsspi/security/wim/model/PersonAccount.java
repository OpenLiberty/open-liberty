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
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.websphere.security.wim.util.ExtendedPropertyXmlAdapter;

/**
 * <p>Java class for PersonAccount complex type.
 *
 * <p>The PersonAccount object extends the {@link LoginAccount} object, and represents an account with person properties.
 *
 * <p>A PersonAccount object allows the schema of a person and his login accounts, defined by the
 * respective {@link LoginAccount} objects, to be kept independent of each other. For example, the set of properties on a PersonAccount
 * may not need to be exactly the same as the set of person-specific properties on each of that person's
 * {@link LoginAccount} entities.
 *
 * <p>Below is a table of supported properties for {@link PersonAccount}.
 *
 * <p>
 * <ul>
 * <li><b>uid</b>: contains a computer system login name for the PersonAccount.</li>
 * <li><b>cn</b>: contains names of a PersonAccount.</li>
 * <li><b>sn</b>: contains name strings for the family names of a PersonAccount.</li>
 * <li><b>preferredLanguage</b>: contains the preferred written or spoken language for a PersonAccount.</li>
 * <li><b>displayName</b>: contains the preferred name of a PersonAccount to be used for display.</li>
 * <li><b>initials</b>: contains strings of initials of some or all of an individual's names, except the surname(s).</li>
 * <li><b>mail</b>: contains an email address for the PersonAccount.</li>
 * <li><b>ibmPrimaryEmail</b>: contains the primary email address for the PersonAccount.</li>
 * <li><b>jpegPhoto</b>: contains JPEG photos for the PersonAccount.</li>
 * <li><b>labeledURI</b>: contains URIs with optional labels.</li>
 * <li><b>carLicense</b>: contains vehicle license or registration plate for the PersonAccount.</li>
 * <li><b>telephoneNumber</b>: contains telephone numbers for the PersonAccount.</li>
 * <li><b>facsimileTelephoneNumber</b>: contains telephone numbers for facsimile terminals.</li>
 * <li><b>pager</b>: contains pager numbers for the PersonAccount.</li>
 * <li><b>mobile</b>: contains mobile numbers for the PersonAccount.</li>
 * <li><b>homePostalAddress</b>: contains home addresses used by a Postal Service.</li>
 * <li><b>postalAddress</b>: contains addresses used by a Postal Service.</li>
 * <li><b>roomNumber</b>: contains the room number for the PersonAccount.</li>
 * <li><b>l</b>: a short form for the <b>localityName</b>.</li>
 * <li><b>localityName</b>: contains the name of a locality, such as a city, county or other geographic region.</li>
 * <li><b>st</b>: a short form for <b>stateOrProvinceName</b>.</li>
 * <li><b>stateOrProvinceName</b>: contains the full name of a state or province (stateOrProvinceName).</li>
 * <li><b>street</b>: contains the physical address of the object to which the entry corresponds, such as an address for package delivery.</li>
 * <li><b>postalCode</b>: contains codes used by a Postal Service to identify postal service zones.</li>
 * <li><b>city</b>: contains the city.</li>
 * <li><b>employeeType</b>: contains the employee type for the PersonAccount.</li>
 * <li><b>employeeNumber</b>: contains the employee number for the PersonAccount.</li>
 * <li><b>manager</b>: contains an identifier for the PersonAccount's manager.</li>
 * <li><b>secretary</b>: contains an identifier for the PersonAccount's secretary.</li>
 * <li><b>departmentNumber</b>: identifies a department within an organization.</li>
 * <li><b>title</b>: contains the title of a person in their organizational context.</li>
 * <li><b>ibmJobTitle</b>: contains the job title for the PersonAccount.</li>
 * <li><b>c</b>: short form for the <b>countryName</b> property.</li>
 * <li><b>countryName</b>: defines the name of the country.</li>
 * <li><b>givenName</b>: contains name strings that are the part of a person's name that is not their surname.</li>
 * <li><b>homeStreet</b>: contains the home street address for the PersonAccount.</li>
 * <li><b>homeCity</b>: contains the home city for the PersonAccount.</li>
 * <li><b>homeStateOrProvinceName</b>: contains the home state or province name for the PersonAccount.</li>
 * <li><b>homePostalCode</b>: contains the home postal code for the PersonAccount.</li>
 * <li><b>homeCountryName</b>: contains the home country name for the PersonAccount.</li>
 * <li><b>businessStreet</b>: contains the business street address for the PersonAccount.</li>
 * <li><b>businessCity</b>: contains the business city for the PersonAccount.</li>
 * <li><b>businessStateOrProvinceName</b>: contains the business state or province name for the PersonAccount.</li>
 * <li><b>businessPostalCode</b>: contains the business postal code for the PersonAccount.</li>
 * <li><b>businessCountryName</b>: contains the business country name for the PersonAccount.</li>
 * <li><b>description</b>: contains human-readable descriptive phrases about the PersonAccount.</li>
 * <li><b>businessCategory</b>: describes the kinds of business performed by a PersonAccount.</li>
 * <li><b>seeAlso</b>: contains the distinguished names of objects that are related to this PersonAccount.</li>
 * <li><b>kerberosId</b>: contains the Kerberos ID for the PersonAccount.</li>
 * <li><b>photoURL</b>: contains a URL for a photo of the PersonAccount.</li>
 * <li><b>photoURLThumbnail</b>: contains a URL for a thumbnail image of the PersonAccount.</li>
 * <li><b>middleName</b>: contains name strings that comprise the person's middle name.</li>
 * <li><b>honorificPrefix</b>: contains name strings that comprise the PersonAccounts honorary prefix(es).</li>
 * <li><b>honorificSuffix</b>: contains name strings that comprise the PersonAccounts honorary suffix(es).</li>
 * <li><b>nickName</b>: contains a nick name for the PersonAccount.</li>
 * <li><b>profileUrl</b>: contains a URL to the PersonAccount's profile.</li>
 * <li><b>timezone</b>: contains the timezone for the PersonAccount.</li>
 * <li><b>locale</b>: contains the preferred locale for a PersonAccount.</li>
 * <li><b>ims</b>: contains instant messaging addresses for the PersonAccount.</li>
 * <li><b>active</b>: describes whether the PersonAccount is active.</li>
 * </ul>
 *
 * <p/>
 * In addition to the properties in the table above, all properties from the super-class {@link LoginAccount} and its
 * super-classes are supported.
 *
 * <p/>
 * The {@link PersonAccount} schema can be extended by including extended properties in the server.xml configuration. For example,
 * the following server.xml configuration would create the extended property "myProperty" of type String in {@link PersonAccount}.
 *
 * <code>
 *
 * <pre>
 * &lt;federatedRepository&gt;
 *     &lt;extendedProperty name="myProperty" dataType="String" entityType="PersonAccount" multiValued="false" /&gt;
 * &lt;/federatedRepository&gt;
 * </pre>
 *
 * </code>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = PersonAccount.TYPE_NAME, propOrder = {
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
                                                       "active",
                                                       "extendedProperties"
})
public class PersonAccount extends LoginAccount {

    private static final TraceComponent tc = Tr.register(PersonAccount.class);

    /** The type name for this data type. */
    public static final String TYPE_NAME = "PersonAccount";

    /** Property name constant for the <b>uid</b> property. */
    private static final String PROP_UID = "uid";

    /** Property name constant for the <b>cn</b> property. */
    private static final String PROP_CN = "cn";

    /** Property name constant for the <b>sn</b> property. */
    private static final String PROP_SN = "sn";

    /** Property name constant for the <b>preferredLanguage</b> property. */
    private static final String PROP_PREFERRED_LANGUAGE = "preferredLanguage";

    /** Property name constant for the <b>displayName</b> property. */
    private static final String PROP_DISPLAY_NAME = "displayName";

    /** Property name constant for the <b>initials</b> property. */
    private static final String PROP_INITIALS = "initials";

    /** Property name constant for the <b>mail</b> property. */
    private static final String PROP_MAIL = "mail";

    /** Property name constant for the <b>ibmPrimaryEmail</b> property. */
    private static final String PROP_IBM_PRIMARY_EMAIL = "ibmPrimaryEmail";

    /** Property name constant for the <b>jpegPhoto</b> property. */
    private static final String PROP_JPEG_PHOTO = "jpegPhoto";

    /** Property name constant for the <b>labeledURI</b> property. */
    private static final String PROP_LABELED_URI = "labeledURI";

    /** Property name constant for the <b>carLicense</b> property. */
    private static final String PROP_CAR_LICENSE = "carLicense";

    /** Property name constant for the <b>telephoneNumber</b> property. */
    private static final String PROP_TELEPHONE_NUMBER = "telephoneNumber";

    /** Property name constant for the <b>facsimileTelephoneNumber</b> property. */
    private static final String PROP_FACSIMILE_TELEPHONE_NUMBER = "facsimileTelephoneNumber";

    /** Property name constant for the <b>pager</b> property. */
    private static final String PROP_PAGER = "pager";

    /** Property name constant for the <b>mobile</b> property. */
    private static final String PROP_MOBILE = "mobile";

    /** Property name constant for the <b>homePostalAddress</b> property. */
    private static final String PROP_HOME_POSTAL_ADDRESS = "homePostalAddress";

    /** Property name constant for the <b>postalAddress</b> property. */
    private static final String PROP_POSTAL_ADDRESS = "postalAddress";

    /** Property name constant for the <b>roomNumber</b> property. */
    private static final String PROP_ROOM_NUMBER = "roomNumber";

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

    /** Property name constant for the <b>city</b> property. */
    private static final String PROP_CITY = "city";

    /** Property name constant for the <b>employeeType</b> property. */
    private static final String PROP_EMPLOYEE_TYPE = "employeeType";

    /** Property name constant for the <b>employeeNumber</b> property. */
    private static final String PROP_EMPLOYEE_NUMBER = "employeeNumber";

    /** Property name constant for the <b>manager</b> property. */
    private static final String PROP_MANAGER = "manager";

    /** Property name constant for the <b>secretary</b> property. */
    private static final String PROP_SECRETARY = "secretary";

    /** Property name constant for the <b>departmentNumber</b> property. */
    private static final String PROP_DEPARTMENT_NUMBER = "departmentNumber";

    /** Property name constant for the <b>title</b> property. */
    private static final String PROP_TITLE = "title";

    /** Property name constant for the <b>ibmJobTitle</b> property. */
    private static final String PROP_IBM_JOB_TITLE = "ibmJobTitle";

    /** Property name constant for the <b>c</b> property. */
    private static final String PROP_C = "c";

    /** Property name constant for the <b>countryName</b> property. */
    private static final String PROP_COUNTRY_NAME = "countryName";

    /** Property name constant for the <b>givenName</b> property. */
    private static final String PROP_GIVEN_NAME = "givenName";

    /** Property name constant for the <b>homeStreet</b> property. */
    private static final String PROP_HOME_STREET = "homeStreet";

    /** Property name constant for the <b>homeCity</b> property. */
    private static final String PROP_HOME_CITY = "homeCity";

    /** Property name constant for the <b>homeStateOrProvinceName</b> property. */
    private static final String PROP_HOME_STATE_OR_PROVINCE_NAME = "homeStateOrProvinceName";

    /** Property name constant for the <b>homePostalCode</b> property. */
    private static final String PROP_HOME_POSTAL_CODE = "homePostalCode";

    /** Property name constant for the <b>homeCountryName</b> property. */
    private static final String PROP_HOME_COUNTRY_NAME = "homeCountryName";

    /** Property name constant for the <b>businessStreet</b> property. */
    private static final String PROP_BUSINESS_STREET = "businessStreet";

    /** Property name constant for the <b>businessCity</b> property. */
    private static final String PROP_BUSINESS_CITY = "businessCity";

    /** Property name constant for the <b>businessStateOrProvinceName</b> property. */
    private static final String PROP_BUSINESS_STATE_OR_PROVINCE_NAME = "businessStateOrProvinceName";

    /** Property name constant for the <b>businessPostalCode</b> property. */
    private static final String PROP_BUSINESS_POSTAL_CODE = "businessPostalCode";

    /** Property name constant for the <b>businessCountryName</b> property. */
    private static final String PROP_BUSINESS_COUNTRY_NAME = "businessCountryName";

    /** Property name constant for the <b>description</b> property. */
    private static final String PROP_DESCRIPTION = "description";

    /** Property name constant for the <b>businessCategory</b> property. */
    private static final String PROP_BUSINESS_CATEGORY = "businessCategory";

    /** Property name constant for the <b>seeAlso</b> property. */
    private static final String PROP_SEE_ALSO = "seeAlso";

    /** Property name constant for the <b>kerberosId</b> property. */
    private static final String PROP_KERBEROS_ID = "kerberosId";

    /** Property name constant for the <b>photoURL</b> property. */
    private static final String PROP_PHOTO_URL = "photoURL";

    /** Property name constant for the <b>photoURLThumbnail</b> property. */
    private static final String PROP_PHOTO_URL_THUMBNAIL = "photoURLThumbnail";

    /** Property name constant for the <b>middleName</b> property. */
    private static final String PROP_MIDDLE_NAME = "middleName";

    /** Property name constant for the <b>honorificPrefix</b> property. */
    private static final String PROP_HONORIFIC_PREFIX = "honorificPrefix";

    /** Property name constant for the <b>honorificSuffix</b> property. */
    private static final String PROP_HONORIFIC_SUFFIX = "honorificSuffix";

    /** Property name constant for the <b>nickName</b> property. */
    private static final String PROP_NICK_NAME = "nickName";

    /** Property name constant for the <b>profileUrl</b> property. */
    private static final String PROP_PROFILE_URL = "profileUrl";

    /** Property name constant for the <b>timezone</b> property. */
    private static final String PROP_TIMEZONE = "timezone";

    /** Property name constant for the <b>locale</b> property. */
    private static final String PROP_LOCALE = "locale";

    /** Property name constant for the <b>ims</b> property. */
    private static final String PROP_IMS = "ims";

    /** Property name constant for the <b>active</b> property. */
    private static final String PROP_ACTIVE = "active";

    /** Contains a computer system login name for the PersonAccount. */
    @XmlElement(name = PROP_UID)
    protected String uid;

    /** Contains names of a PersonAccount. */
    @XmlElement(name = PROP_CN)
    protected String cn;

    /** Contains name strings for the family names of a PersonAccount. */
    @XmlElement(name = PROP_SN)
    protected String sn;

    /** Contains the preferred written or spoken language for a PersonAccount. */
    @XmlElement(name = PROP_PREFERRED_LANGUAGE)
    protected String preferredLanguage;

    /** Contains the preferred name of a PersonAccount to be used for display. */
    @XmlElement(name = PROP_DISPLAY_NAME)
    protected List<String> displayName;

    /** Contains strings of initials of some or all of an individual's names, except the surname(s). */
    @XmlElement(name = PROP_INITIALS)
    protected List<String> initials;

    /** Contains an email address for the PersonAccount. */
    @XmlElement(name = PROP_MAIL)
    protected String mail;

    /** Contains the primary email address for the PersonAccount. */
    @XmlElement(name = PROP_IBM_PRIMARY_EMAIL)
    protected String ibmPrimaryEmail;

    /** Contains JPEG photos for the PersonAccount. */
    @XmlElement(name = PROP_JPEG_PHOTO)
    protected List<byte[]> jpegPhoto;

    /** Contains URIs with optional labels. */
    @XmlElement(name = PROP_LABELED_URI)
    protected String labeledURI;

    /** Contains vehicle license or registration plate for the PersonAccount. */
    @XmlElement(name = PROP_CAR_LICENSE)
    protected List<String> carLicense;

    /** Contains telephone numbers for the PersonAccount. */
    @XmlElement(name = PROP_TELEPHONE_NUMBER)
    protected List<String> telephoneNumber;

    /** Contains telephone numbers for facsimile terminals. */
    @XmlElement(name = PROP_FACSIMILE_TELEPHONE_NUMBER)
    protected List<String> facsimileTelephoneNumber;

    /** Contains pager numbers for the PersonAccount. */
    @XmlElement(name = PROP_PAGER)
    protected List<String> pager;

    /** Contains mobile numbers for the PersonAccount. */
    @XmlElement(name = PROP_MOBILE)
    protected List<String> mobile;

    /** Contains home addresses used by a Postal Service. */
    @XmlElement(name = PROP_HOME_POSTAL_ADDRESS)
    protected List<String> homePostalAddress;

    /** Contains addresses used by a Postal Service. */
    @XmlElement(name = PROP_POSTAL_ADDRESS)
    protected List<String> postalAddress;

    /** Contains the room number for the PersonAccount. */
    @XmlElement(name = PROP_ROOM_NUMBER)
    protected List<String> roomNumber;

    /** A short form for the <b>localityName</b>. */
    @XmlElement(name = PROP_L)
    protected List<String> l;

    /** Contains the name of a locality, such as a city, county or other geographic region. */
    @XmlElement(name = PROP_LOCALITY_NAME)
    protected List<String> localityName;

    /** A short form for <b>stateOrProvinceName</b>. */
    @XmlElement(name = PROP_ST)
    protected List<String> st;

    /** Contains the full name of a state or province (stateOrProvinceName). */
    @XmlElement(name = PROP_STATE_OR_PROVINCE_NAME)
    protected List<String> stateOrProvinceName;

    /** Contains the physical address of the object to which the entry corresponds, such as an address for package delivery. */
    @XmlElement(name = PROP_STREET)
    protected List<String> street;

    /** Contains codes used by a Postal Service to identify postal service zones. */
    @XmlElement(name = PROP_POSTAL_CODE)
    protected List<String> postalCode;

    /** Contains the city. */
    @XmlElement(name = PROP_CITY)
    protected List<String> city;

    /** Contains the employee type for the PersonAccount. */
    @XmlElement(name = PROP_EMPLOYEE_TYPE)
    protected String employeeType;

    /** Contains the employee number for the PersonAccount. */
    @XmlElement(name = PROP_EMPLOYEE_NUMBER)
    protected String employeeNumber;

    /** Contains an identifier for the PersonAccount's manager. */
    @XmlElement(name = PROP_MANAGER)
    protected List<IdentifierType> manager;

    /** Contains an identifier for the PersonAccount's secretary. */
    @XmlElement(name = PROP_SECRETARY)
    protected List<IdentifierType> secretary;

    /** Identifies a department within an organization. */
    @XmlElement(name = PROP_DEPARTMENT_NUMBER)
    protected List<String> departmentNumber;

    /** Contains the title of a person in their organizational context. */
    @XmlElement(name = PROP_TITLE)
    protected List<String> title;

    /** Contains the job title for the PersonAccount. */
    @XmlElement(name = PROP_IBM_JOB_TITLE)
    protected List<String> ibmJobTitle;

    /** Short form for the <b>countryName</b> property. */
    @XmlElement(name = PROP_C)
    protected List<String> c;

    /** Defines the name of the country. */
    @XmlElement(name = PROP_COUNTRY_NAME)
    protected List<String> countryName;

    /** Contains name strings that are the part of a person's name that is not their surname. */
    @XmlElement(name = PROP_GIVEN_NAME)
    protected List<String> givenName;

    /** Contains the home street address for the PersonAccount. */
    @XmlElement(name = PROP_HOME_STREET)
    protected String homeStreet;

    /** Contains the home city for the PersonAccount. */
    @XmlElement(name = PROP_HOME_CITY)
    protected String homeCity;

    /** Contains the home state or province name for the PersonAccount. */
    @XmlElement(name = PROP_HOME_STATE_OR_PROVINCE_NAME)
    protected String homeStateOrProvinceName;

    /** Contains the home postal code for the PersonAccount. */
    @XmlElement(name = PROP_HOME_POSTAL_CODE)
    protected String homePostalCode;

    /** Contains the home country name for the PersonAccount. */
    @XmlElement(name = PROP_HOME_COUNTRY_NAME)
    protected String homeCountryName;

    /** Contains the business street address for the PersonAccount. */
    @XmlElement(name = PROP_BUSINESS_STREET)
    protected String businessStreet;

    /** Contains the business city for the PersonAccount. */
    @XmlElement(name = PROP_BUSINESS_CITY)
    protected String businessCity;

    /** Contains the business state or province name for the PersonAccount. */
    @XmlElement(name = PROP_BUSINESS_STATE_OR_PROVINCE_NAME)
    protected String businessStateOrProvinceName;

    /** Contains the business postal code for the PersonAccount. */
    @XmlElement(name = PROP_BUSINESS_POSTAL_CODE)
    protected String businessPostalCode;

    /** Contains the business country name for the PersonAccount. */
    @XmlElement(name = PROP_BUSINESS_COUNTRY_NAME)
    protected String businessCountryName;

    /** Contains human-readable descriptive phrases about the PersonAccount. */
    @XmlElement(name = PROP_DESCRIPTION)
    protected List<String> description;

    /** Describes the kinds of business performed by a PersonAccount. */
    @XmlElement(name = PROP_BUSINESS_CATEGORY)
    protected List<String> businessCategory;

    /** Contains the distinguished names of objects that are related to this PersonAccount. */
    @XmlElement(name = PROP_SEE_ALSO)
    protected List<String> seeAlso;

    /** Contains the Kerberos ID for the PersonAccount. */
    @XmlElement(name = PROP_KERBEROS_ID)
    protected String kerberosId;

    /** Contains a URL for a photo of the PersonAccount. */
    @XmlElement(name = PROP_PHOTO_URL)
    protected String photoURL;

    /** Contains a URL for a thumbnail image of the PersonAccount. */
    @XmlElement(name = PROP_PHOTO_URL_THUMBNAIL)
    protected String photoURLThumbnail;

    /** Contains name strings that comprise the person's middle name. */
    @XmlElement(name = PROP_MIDDLE_NAME)
    protected String middleName;

    /** Contains name strings that comprise the PersonAccounts honorary prefix(es). */
    @XmlElement(name = PROP_HONORIFIC_PREFIX)
    protected String honorificPrefix;

    /** Contains name strings that comprise the PersonAccounts honorary suffix(es). */
    @XmlElement(name = PROP_HONORIFIC_SUFFIX)
    protected String honorificSuffix;

    /** Contains a nick name for the PersonAccount. */
    @XmlElement(name = PROP_NICK_NAME)
    protected String nickName;

    /** Contains a URL to the PersonAccount's profile. */
    @XmlElement(name = PROP_PROFILE_URL)
    protected String profileUrl;

    /** Contains the timezone for the PersonAccount. */
    @XmlElement(name = PROP_TIMEZONE)
    protected String timezone;

    /** Contains the preferred locale for a PersonAccount. */
    @XmlElement(name = PROP_LOCALE)
    protected String locale;

    /** Contains instant messaging addresses for the PersonAccount. */
    @XmlElement(name = PROP_IMS)
    protected List<String> ims;

    /** Describes whether the PersonAccount is active. */
    @XmlElement(name = PROP_ACTIVE)
    protected Boolean active;

    /** Map to hold extended property values. */
    @XmlElement(name = "extendedProperties")
    @XmlJavaTypeAdapter(ExtendedPropertyXmlAdapter.class)
    protected Map<String, Object> extendedProperties = new HashMap<String, Object>();

    private static List<String> mandatoryProperties = null;
    private static List<String> transientProperties = null;
    private static List<String> propertyNames = null;
    private static HashMap<String, String> dataTypeMap = null;
    private static ArrayList<String> superTypeList = null;
    private static HashSet<String> subTypeSet = null;

    private static Map<String, String> extendedPropertiesDataType = new HashMap<String, String>();
    private static Map<String, Object> extendedPropertiesDefaultValue = new HashMap<String, Object>();
    private static Set<String> extendedMultiValuedProperties = new HashSet<String>();

    /** The set of multi-valued properties for this type. */
    private static final Set<String> MULTI_VALUED_PROPERTIES;

    static {
        setMandatoryPropertyNames();
        setTransientPropertyNames();
        getTransientProperties();
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
        MULTI_VALUED_PROPERTIES.add(PROP_DESCRIPTION);
        MULTI_VALUED_PROPERTIES.add(PROP_BUSINESS_CATEGORY);
        MULTI_VALUED_PROPERTIES.add(PROP_SEE_ALSO);
        MULTI_VALUED_PROPERTIES.add(PROP_IMS);
    }

    /**
     * Gets the value of the <b>uid</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getUid() {
        return uid;
    }

    /**
     * Sets the value of the <b>uid</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setUid(String value) {
        this.uid = value;
    }

    /**
     * Check if the <b>uid</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetUid() {
        return (this.uid != null);
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
     * Gets the value of the <b>sn</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getSn() {
        return sn;
    }

    /**
     * Sets the value of the <b>sn</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setSn(String value) {
        this.sn = value;
    }

    /**
     * Check if the <b>sn</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetSn() {
        return (this.sn != null);
    }

    /**
     * Gets the value of the <b>preferredLanguage</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    /**
     * Sets the value of the <b>preferredLanguage</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setPreferredLanguage(String value) {
        this.preferredLanguage = value;
    }

    /**
     * Check if the <b>preferredLanguage</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetPreferredLanguage() {
        return (this.preferredLanguage != null);
    }

    /**
     * Gets the value of the <b>displayName</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>displayName</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getDisplayName().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getDisplayName() {
        if (displayName == null) {
            displayName = new ArrayList<String>();
        }
        return this.displayName;
    }

    /**
     * Check if the <b>displayName</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetDisplayName() {
        return ((this.displayName != null) && (!this.displayName.isEmpty()));
    }

    /**
     * Unset the <b>displayName</b> property.
     */
    public void unsetDisplayName() {
        this.displayName = null;
    }

    /**
     * Gets the value of the <b>initials</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>initials</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getInitials().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getInitials() {
        if (initials == null) {
            initials = new ArrayList<String>();
        }
        return this.initials;
    }

    /**
     * Check if the <b>initials</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetInitials() {
        return ((this.initials != null) && (!this.initials.isEmpty()));
    }

    /**
     * Unset the <b>initials</b> property.
     */
    public void unsetInitials() {
        this.initials = null;
    }

    /**
     * Gets the value of the <b>mail</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getMail() {
        return mail;
    }

    /**
     * Sets the value of the <b>mail</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setMail(String value) {
        this.mail = value;
    }

    /**
     * Check if the <b>mail</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetMail() {
        return (this.mail != null);
    }

    /**
     * Gets the value of the <b>ibmPrimaryEmail</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getIbmPrimaryEmail() {
        return ibmPrimaryEmail;
    }

    /**
     * Sets the value of the <b>ibmPrimaryEmail</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setIbmPrimaryEmail(String value) {
        this.ibmPrimaryEmail = value;
    }

    /**
     * Check if the <b>ibmPrimaryEmail</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetIbmPrimaryEmail() {
        return (this.ibmPrimaryEmail != null);
    }

    /**
     * Gets the value of the <b>jpegPhoto</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>jpegPhoto</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getJpegPhoto().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * byte[]
     *
     * @return
     *         returned object is {@link List}
     */
    public List<byte[]> getJpegPhoto() {
        if (jpegPhoto == null) {
            jpegPhoto = new ArrayList<byte[]>();
        }
        return this.jpegPhoto;
    }

    /**
     * Check if the <b>jpegPhoto</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetJpegPhoto() {
        return ((this.jpegPhoto != null) && (!this.jpegPhoto.isEmpty()));
    }

    /**
     * Unset the <b>jpegPhoto</b> property.
     */
    public void unsetJpegPhoto() {
        this.jpegPhoto = null;
    }

    /**
     * Gets the value of the <b>labeledURI</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getLabeledURI() {
        return labeledURI;
    }

    /**
     * Sets the value of the <b>labeledURI</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setLabeledURI(String value) {
        this.labeledURI = value;
    }

    /**
     * Check if the <b>labeledURI</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetLabeledURI() {
        return (this.labeledURI != null);
    }

    /**
     * Gets the value of the <b>carLicense</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>carLicense</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getCarLicense().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getCarLicense() {
        if (carLicense == null) {
            carLicense = new ArrayList<String>();
        }
        return this.carLicense;
    }

    /**
     * Check if the <b>carLicense</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetCarLicense() {
        return ((this.carLicense != null) && (!this.carLicense.isEmpty()));
    }

    /**
     * Unset the <b>carLicense</b> property.
     */
    public void unsetCarLicense() {
        this.carLicense = null;
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
     * Gets the value of the <b>pager</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>pager</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getPager().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getPager() {
        if (pager == null) {
            pager = new ArrayList<String>();
        }
        return this.pager;
    }

    /**
     * Check if the <b>pager</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetPager() {
        return ((this.pager != null) && (!this.pager.isEmpty()));
    }

    /**
     * Unset the <b>pager</b> property.
     */
    public void unsetPager() {
        this.pager = null;
    }

    /**
     * Gets the value of the <b>mobile</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>mobile</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getMobile().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getMobile() {
        if (mobile == null) {
            mobile = new ArrayList<String>();
        }
        return this.mobile;
    }

    /**
     * Check if the <b>mobile</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetMobile() {
        return ((this.mobile != null) && (!this.mobile.isEmpty()));
    }

    /**
     * Unset the <b>mobile</b> property.
     */
    public void unsetMobile() {
        this.mobile = null;
    }

    /**
     * Gets the value of the <b>homePostalAddress</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>homePostalAddress</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getHomePostalAddress().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getHomePostalAddress() {
        if (homePostalAddress == null) {
            homePostalAddress = new ArrayList<String>();
        }
        return this.homePostalAddress;
    }

    /**
     * Check if the <b>homePostalAddress</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetHomePostalAddress() {
        return ((this.homePostalAddress != null) && (!this.homePostalAddress.isEmpty()));
    }

    /**
     * Unset the <b>homePostalAddress</b> property.
     */
    public void unsetHomePostalAddress() {
        this.homePostalAddress = null;
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
     * Gets the value of the <b>roomNumber</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>roomNumber</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getRoomNumber().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getRoomNumber() {
        if (roomNumber == null) {
            roomNumber = new ArrayList<String>();
        }
        return this.roomNumber;
    }

    /**
     * Check if the <b>roomNumber</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetRoomNumber() {
        return ((this.roomNumber != null) && (!this.roomNumber.isEmpty()));
    }

    /**
     * Unset the <b>roomNumber</b> property.
     */
    public void unsetRoomNumber() {
        this.roomNumber = null;
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
     * Gets the value of the localityName property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the localityName property.
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
     * Gets the value of the <b>city</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>city</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getCity().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getCity() {
        if (city == null) {
            city = new ArrayList<String>();
        }
        return this.city;
    }

    /**
     * Check if the <b>city</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetCity() {
        return ((this.city != null) && (!this.city.isEmpty()));
    }

    /**
     * Unset the <b>city</b> property.
     */
    public void unsetCity() {
        this.city = null;
    }

    /**
     * Gets the value of the employeeType property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getEmployeeType() {
        return employeeType;
    }

    /**
     * Sets the value of the employeeType property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setEmployeeType(String value) {
        this.employeeType = value;
    }

    /**
     * Check if the <b>employeeType</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetEmployeeType() {
        return (this.employeeType != null);
    }

    /**
     * Gets the value of the <b>employeeNumber</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getEmployeeNumber() {
        return employeeNumber;
    }

    /**
     * Sets the value of the <b>employeeNumber</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setEmployeeNumber(String value) {
        this.employeeNumber = value;
    }

    /**
     * Check if the <b>employeeNumber</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetEmployeeNumber() {
        return (this.employeeNumber != null);
    }

    /**
     * Gets the value of the <b>manager</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>manager</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getManager().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link IdentifierType }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<IdentifierType> getManager() {
        if (manager == null) {
            manager = new ArrayList<IdentifierType>();
        }
        return this.manager;
    }

    /**
     * Check if the <b>manager</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetManager() {
        return ((this.manager != null) && (!this.manager.isEmpty()));
    }

    /**
     * Unset the <b>manager</b> property.
     */
    public void unsetManager() {
        this.manager = null;
    }

    /**
     * Gets the value of the <b>secretary</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>secretary</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getSecretary().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link IdentifierType }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<IdentifierType> getSecretary() {
        if (secretary == null) {
            secretary = new ArrayList<IdentifierType>();
        }
        return this.secretary;
    }

    /**
     * Check if the <b>secretary</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetSecretary() {
        return ((this.secretary != null) && (!this.secretary.isEmpty()));
    }

    /**
     * Unset the <b>secretary</b> property.
     */
    public void unsetSecretary() {
        this.secretary = null;
    }

    /**
     * Gets the value of the <b>departmentNumber</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>departmentNumber</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getDepartmentNumber().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getDepartmentNumber() {
        if (departmentNumber == null) {
            departmentNumber = new ArrayList<String>();
        }
        return this.departmentNumber;
    }

    /**
     * Check if the <b>departmentNumber</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetDepartmentNumber() {
        return ((this.departmentNumber != null) && (!this.departmentNumber.isEmpty()));
    }

    /**
     * Unset the <b>departmentNumber</b> property.
     */
    public void unsetDepartmentNumber() {
        this.departmentNumber = null;
    }

    /**
     * Gets the value of the <b>title</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>title</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getTitle().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getTitle() {
        if (title == null) {
            title = new ArrayList<String>();
        }
        return this.title;
    }

    /**
     * Check if the <b>title</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetTitle() {
        return ((this.title != null) && (!this.title.isEmpty()));
    }

    /**
     * Unset the <b>title</b> property.
     */
    public void unsetTitle() {
        this.title = null;
    }

    /**
     * Gets the value of the <b>ibmJobTitle</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>ibmJobTitle</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getIbmJobTitle().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getIbmJobTitle() {
        if (ibmJobTitle == null) {
            ibmJobTitle = new ArrayList<String>();
        }
        return this.ibmJobTitle;
    }

    /**
     * Check if the <b>ibmJobTitle</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetIbmJobTitle() {
        return ((this.ibmJobTitle != null) && (!this.ibmJobTitle.isEmpty()));
    }

    /**
     * Unset the <b>ibmJobTitle</b> property.
     */
    public void unsetIbmJobTitle() {
        this.ibmJobTitle = null;
    }

    /**
     * Gets the value of the <b>c</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>c</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getC().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getC() {
        if (c == null) {
            c = new ArrayList<String>();
        }
        return this.c;
    }

    /**
     * Check if the <b>c</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetC() {
        return ((this.c != null) && (!this.c.isEmpty()));
    }

    /**
     * Unset the <b>c</b> property.
     */
    public void unsetC() {
        this.c = null;
    }

    /**
     * Gets the value of the <b>countryName</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>countryName</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getCountryName().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getCountryName() {
        if (countryName == null) {
            countryName = new ArrayList<String>();
        }
        return this.countryName;
    }

    /**
     * Check if the <b>countryName</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetCountryName() {
        return ((this.countryName != null) && (!this.countryName.isEmpty()));
    }

    /**
     * Unset the <b>countryName</b> property.
     */
    public void unsetCountryName() {
        this.countryName = null;
    }

    /**
     * Gets the value of the <b>givenName</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>givenName</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getGivenName().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getGivenName() {
        if (givenName == null) {
            givenName = new ArrayList<String>();
        }
        return this.givenName;
    }

    /**
     * Check if the <b>givenName</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetGivenName() {
        return ((this.givenName != null) && (!this.givenName.isEmpty()));
    }

    /**
     * Unset the <b>givenName</b> property.
     */
    public void unsetGivenName() {
        this.givenName = null;
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

    /**
     * Gets the value of the <b>kerberosId</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getKerberosId() {
        return kerberosId;
    }

    /**
     * Sets the value of the <b>kerberosId</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setKerberosId(String value) {
        this.kerberosId = value;
    }

    /**
     * Check if the <b>kerberosId</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetKerberosId() {
        return (this.kerberosId != null);
    }

    /**
     * Gets the value of the <b>photoURL</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getPhotoUrl() {
        return photoURL;
    }

    /**
     * Sets the value of the <b>photoURL</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setPhotoUrl(String value) {
        this.photoURL = value;
    }

    /**
     * Check if the <b>photoUrl</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetPhotoUrl() {
        return (this.photoURL != null);
    }

    /**
     * Gets the value of the <b>photoURLThumbnail</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getPhotoUrlThumbnail() {
        return photoURLThumbnail;
    }

    /**
     * Sets the value of the <b>photoURLThumbnail</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setPhotoUrlThumbnail(String value) {
        this.photoURLThumbnail = value;
    }

    /**
     * Check if the <b>photoURLThumbnail</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetPhotoUrlThumbnail() {
        return (this.photoURLThumbnail != null);
    }

    /**
     * Gets the value of the <b>honorificSuffix</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getHonorificSuffix() {
        return honorificSuffix;
    }

    /**
     * Sets the value of the <b>honorificSuffix</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setHonorificSuffix(String value) {
        this.honorificSuffix = value;
    }

    /**
     * Check if the <b>honorificSuffix</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetHonorificSuffix() {
        return (this.honorificSuffix != null);
    }

    /**
     * Unset the <b>honorificSuffix</b> property.
     */
    public void unsetHonorificSuffix() {
        this.honorificSuffix = null;
    }

    /**
     * Gets the value of the <b>honorificPrefix</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getHonorificPrefix() {
        return honorificPrefix;
    }

    /**
     * Sets the value of the <b>honorificPrefix</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setHonorificPrefix(String value) {
        this.honorificPrefix = value;
    }

    /**
     * Check if the <b>honorificPrefix</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetHonorificPrefix() {
        return (this.honorificPrefix != null);
    }

    /**
     * Unset the <b>honorificPrefix</b> property.
     */
    public void unsetHonorificPrefix() {
        this.honorificPrefix = null;
    }

    /**
     * Gets the value of the <b>middleName</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getMiddleName() {
        return middleName;
    }

    /**
     * Sets the value of the <b>middleName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setMiddleName(String value) {
        this.middleName = value;
    }

    /**
     * Check if the <b>middleName</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetMiddleName() {
        return (this.middleName != null);
    }

    /**
     * Unset the <b>middleName</b> property.
     */
    public void unsetMiddleName() {
        this.middleName = null;
    }

    /**
     * Gets the value of the <b>nickName</b> property.
     *
     * @return
     *         possible object is {@link String }
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
     * Check if the <b>nickName</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetNickName() {
        return (this.nickName != null);
    }

    /**
     * Unset the <b>nickName</b> property.
     */
    public void unsetNickName() {
        this.nickName = null;
    }

    /**
     * Gets the value of the <b>profileUrl</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getProfileUrl() {
        return profileUrl;
    }

    /**
     * Sets the value of the <b>profileUrl</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setProfileUrl(String value) {
        this.profileUrl = value;
    }

    /**
     * Check if the <b>profileUrl</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetProfileUrl() {
        return (this.profileUrl != null);
    }

    /**
     * Unset the <b>profileUrl</b> property.
     */
    public void unsetProfileUrl() {
        this.profileUrl = null;
    }

    /**
     * Gets the value of the <b>timezone</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getTimezone() {
        return timezone;
    }

    /**
     * Sets the value of the <b>timezone</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setTimezone(String value) {
        this.timezone = value;
    }

    /**
     * Check if the <b>timezone</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetTimzone() {
        return (this.timezone != null);
    }

    /**
     * Unset the <b>timezone</b> property.
     */
    public void unsetTimezone() {
        this.timezone = null;
    }

    /**
     * Gets the value of the <b>locale</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getLocale() {
        return locale;
    }

    /**
     * Sets the value of the <b>locale</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setLocale(String value) {
        this.locale = value;
    }

    /**
     * Check if the <b>locale</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetLocale() {
        return (this.locale != null);
    }

    /**
     * Unset the <b>locale</b> property.
     */
    public void unsetLocale() {
        this.locale = null;
    }

    /**
     * Gets the value of the <b>active</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public Boolean getActive() {
        return active;
    }

    /**
     * Sets the value of the <b>active</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setActive(Boolean value) {
        this.active = value;
    }

    /**
     * Check if the <b>active</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetActive() {
        return (this.active != null);
    }

    /**
     * Unset the <b>active</b> property.
     */
    public void unsetActive() {
        this.active = null;
    }

    /**
     * Gets the value of the <b>homeStreet</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getHomeStreet() {
        return homeStreet;
    }

    /**
     * Sets the value of the <b>homeStreet</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setHomeStreet(String value) {
        this.homeStreet = value;
    }

    /**
     * Check if the <b>homeStreet</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetHomeStreet() {
        return (this.homeStreet != null);
    }

    /**
     * Unset the <b>homeStreet</b> property.
     */
    public void unsetHomeStreet() {
        homeStreet = null;
    }

    /**
     * Gets the value of the <b>homeCity</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getHomeCity() {
        return homeCity;
    }

    /**
     * Sets the value of the <b>homeCity</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setHomeCity(String value) {
        this.homeCity = value;
    }

    /**
     * Check if the <b>homeCity</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetHomeCity() {
        return (this.homeCity != null);
    }

    /**
     * Unset the <b>homeCity</b> property.
     */
    public void unsetHomeCity() {
        homeCity = null;
    }

    /**
     * Gets the value of the <b>homeStateOrProvinceName</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getHomeStateOrProvinceName() {
        return homeStateOrProvinceName;
    }

    /**
     * Sets the value of the <b>homeStateOrProvinceName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setHomeStateOrProvinceName(String value) {
        this.homeStateOrProvinceName = value;
    }

    /**
     * Check if the <b>homeStateOrProvinceName</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetHomeStateOrProvinceName() {
        return (this.homeStateOrProvinceName != null);
    }

    /**
     * Unset the <b>homeStateOrProvinceName</b> property.
     */
    public void unsetHomeStateOrProvinceName() {
        homeStateOrProvinceName = null;
    }

    /**
     * Gets the value of the <b>homePostalCode</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getHomePostalCode() {
        return homePostalCode;
    }

    /**
     * Sets the value of the <b>homePostalCode</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setHomePostalCode(String value) {
        this.homePostalCode = value;
    }

    /**
     * Check if the <b>homePostalCode</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetHomePostalCode() {
        return (this.homePostalCode != null);
    }

    /**
     * Unset the <b>homePostalCode</b> property.
     */
    public void unsetHomePostalCode() {
        homePostalCode = null;
    }

    /**
     * Gets the value of the <b>homeCountryName</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getHomeCountryName() {
        return homeCountryName;
    }

    /**
     * Sets the value of the <b>homeCountryName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setHomeCountryName(String value) {
        this.homeCountryName = value;
    }

    /**
     * Check if the <b>homeCountryName</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetHomeCountryName() {
        return (this.homeCountryName != null);
    }

    /**
     * Unset the <b>homeCountryName</b> property.
     */
    public void unsetHomeCountryName() {
        homeCountryName = null;
    }

    /**
     * Gets the value of the <b>businessStreet</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getBusinessStreet() {
        return businessStreet;
    }

    /**
     * Sets the value of the <b>businessStreet</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setBusinessStreet(String value) {
        this.businessStreet = value;
    }

    /**
     * Check if the <b>businessStreet</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetBusinessStreet() {
        return (this.businessStreet != null);
    }

    /**
     * Unset the <b>businessStreet</b> property.
     */
    public void unsetBusinessStreet() {
        businessStreet = null;
    }

    /**
     * Gets the value of the <b>businessCity</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getBusinessCity() {
        return businessCity;
    }

    /**
     * Sets the value of the <b>businessCity</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setBusinessCity(String value) {
        this.businessCity = value;
    }

    /**
     * Check if the <b>businessCity</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetBusinessCity() {
        return (this.businessCity != null);
    }

    /**
     * Unset the <b>businessCity</b> property.
     */
    public void unsetBusinessCity() {
        businessCity = null;
    }

    /**
     * Gets the value of the <b>businessStateOrProvinceName</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getBusinessStateOrProvinceName() {
        return businessStateOrProvinceName;
    }

    /**
     * Sets the value of the <b>businessStateOrProvinceName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setBusinessStateOrProvinceName(String value) {
        this.businessStateOrProvinceName = value;
    }

    /**
     * Check if the <b>businessStateOrProvinceName</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetBusinessStateOrProvinceName() {
        return (this.businessStateOrProvinceName != null);
    }

    /**
     * Unset the <b>businessStateOrProvinceName</b> property.
     */
    public void unsetBusinessStateOrProvinceName() {
        businessStateOrProvinceName = null;
    }

    /**
     * Gets the value of the <b>businessPostalCode</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getBusinessPostalCode() {
        return businessPostalCode;
    }

    /**
     * Sets the value of the <b>businessPostalCode</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setBusinessPostalCode(String value) {
        this.businessPostalCode = value;
    }

    /**
     * Check if the <b>businessPostalCode</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetBusinessPostalCode() {
        return (this.businessPostalCode != null);
    }

    /**
     * Unset the <b>businessPostalCode</b> property.
     */
    public void unsetBusinessPostalCode() {
        businessPostalCode = null;
    }

    /**
     * Gets the value of the <b>businessCountryName</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getBusinessCountryName() {
        return businessCountryName;
    }

    /**
     * Sets the value of the <b>businessCountryName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setBusinessCountryName(String value) {
        this.businessCountryName = value;
    }

    /**
     * Check if the <b>businessCountryName</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetBusinessCountryName() {
        return (this.businessCountryName != null);
    }

    /**
     * Unset the <b>businesCountryName</b> property.
     */
    public void unsetBusinessCountryName() {
        businessCountryName = null;
    }

    /**
     * Gets the value of the <b>ims</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public List<String> getIMs() {
        if (ims == null) {
            ims = new ArrayList<String>();
        }
        return this.ims;
    }

    /**
     * Check if the <b>ims</b> property is set.
     *
     * @return True if the property is set, false otherwise.
     */
    public boolean isSetIMs() {
        return ((this.ims != null) && (!this.ims.isEmpty()));
    }

    /**
     * Unset the <b>ims</b> property.
     */
    public void unsetIMs() {
        this.ims = null;
    }

    @Override
    public Object get(String propName) {

        /*
         * Require a property name.
         */
        if (propName == null || propName.trim().isEmpty()) {
            return null;
        }

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
        if (propName.equals(PROP_HOME_STREET)) {
            return getHomeStreet();
        }
        if (propName.equals(PROP_HOME_CITY)) {
            return getHomeCity();
        }
        if (propName.equals(PROP_HOME_STATE_OR_PROVINCE_NAME)) {
            return getHomeStateOrProvinceName();
        }
        if (propName.equals(PROP_HOME_POSTAL_CODE)) {
            return getHomePostalCode();
        }
        if (propName.equals(PROP_HOME_COUNTRY_NAME)) {
            return getHomeCountryName();
        }
        if (propName.equals(PROP_BUSINESS_STREET)) {
            return getBusinessStreet();
        }
        if (propName.equals(PROP_BUSINESS_CITY)) {
            return getBusinessCity();
        }
        if (propName.equals(PROP_BUSINESS_STATE_OR_PROVINCE_NAME)) {
            return getBusinessStateOrProvinceName();
        }
        if (propName.equals(PROP_BUSINESS_POSTAL_CODE)) {
            return getBusinessPostalCode();
        }
        if (propName.equals(PROP_BUSINESS_COUNTRY_NAME)) {
            return getBusinessCountryName();
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
        if (propName.equals(PROP_MIDDLE_NAME)) {
            return getMiddleName();
        }
        if (propName.equals(PROP_HONORIFIC_PREFIX)) {
            return getHonorificPrefix();
        }
        if (propName.equals(PROP_HONORIFIC_SUFFIX)) {
            return getHonorificSuffix();
        }
        if (propName.equals(PROP_NICK_NAME)) {
            return getNickName();
        }
        if (propName.equals(PROP_PROFILE_URL)) {
            return getProfileUrl();
        }
        if (propName.equals(PROP_TIMEZONE)) {
            return getTimezone();
        }
        if (propName.equals(PROP_LOCALE)) {
            return getLocale();
        }
        if (propName.equals(PROP_ACTIVE)) {
            return getActive();
        }
        if (propName.equals(PROP_IMS)) {
            return getIMs();
        }

        if (extendedPropertiesDataType.containsKey(propName))
            return getExtendedProperty(propName);

        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {

        /*
         * Require a property name.
         */
        if (propName == null || propName.trim().isEmpty()) {
            return false;
        }

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
        if (propName.equals(PROP_HOME_STREET)) {
            return isSetHomeStreet();
        }
        if (propName.equals(PROP_HOME_CITY)) {
            return isSetHomeCity();
        }
        if (propName.equals(PROP_HOME_STATE_OR_PROVINCE_NAME)) {
            return isSetHomeStateOrProvinceName();
        }
        if (propName.equals(PROP_HOME_POSTAL_CODE)) {
            return isSetHomePostalCode();
        }
        if (propName.equals(PROP_HOME_COUNTRY_NAME)) {
            return isSetHomeCountryName();
        }
        if (propName.equals(PROP_BUSINESS_STREET)) {
            return isSetBusinessStreet();
        }
        if (propName.equals(PROP_BUSINESS_CITY)) {
            return isSetBusinessCity();
        }
        if (propName.equals(PROP_BUSINESS_STATE_OR_PROVINCE_NAME)) {
            return isSetBusinessStateOrProvinceName();
        }
        if (propName.equals(PROP_BUSINESS_POSTAL_CODE)) {
            return isSetBusinessPostalCode();
        }
        if (propName.equals(PROP_BUSINESS_COUNTRY_NAME)) {
            return isSetBusinessCountryName();
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
        if (propName.equals(PROP_MIDDLE_NAME)) {
            return isSetMiddleName();
        }
        if (propName.equals(PROP_HONORIFIC_PREFIX)) {
            return isSetHonorificPrefix();
        }
        if (propName.equals(PROP_HONORIFIC_SUFFIX)) {
            return isSetHonorificSuffix();
        }
        if (propName.equals(PROP_NICK_NAME)) {
            return isSetNickName();
        }
        if (propName.equals(PROP_PROFILE_URL)) {
            return isSetProfileUrl();
        }
        if (propName.equals(PROP_TIMEZONE)) {
            return isSetTimzone();
        }
        if (propName.equals(PROP_LOCALE)) {
            return isSetLocale();
        }
        if (propName.equals(PROP_ACTIVE)) {
            return isSetActive();
        }
        if (propName.equals(PROP_IMS)) {
            return isSetIMs();
        }

        if (extendedPropertiesDataType.containsKey(propName))
            return isSetExtendedProperty(propName);

        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {

        /*
         * Require a property name.
         */
        if (propName == null || propName.trim().isEmpty()) {
            return;
        }

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
            getManager().add(((IdentifierType) value));
        }
        if (propName.equals(PROP_SECRETARY)) {
            getSecretary().add(((IdentifierType) value));
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
        if (propName.equals(PROP_HOME_STREET)) {
            setHomeStreet((String) value);
        }
        if (propName.equals(PROP_HOME_CITY)) {
            setHomeCity((String) value);
        }
        if (propName.equals(PROP_HOME_STATE_OR_PROVINCE_NAME)) {
            setHomeStateOrProvinceName((String) value);
        }
        if (propName.equals(PROP_HOME_POSTAL_CODE)) {
            setHomePostalCode((String) value);
        }
        if (propName.equals(PROP_HOME_COUNTRY_NAME)) {
            setHomeCountryName((String) value);
        }
        if (propName.equals(PROP_BUSINESS_STREET)) {
            setBusinessStreet((String) value);
        }
        if (propName.equals(PROP_BUSINESS_CITY)) {
            setBusinessCity((String) value);
        }
        if (propName.equals(PROP_BUSINESS_STATE_OR_PROVINCE_NAME)) {
            setBusinessStateOrProvinceName((String) value);
        }
        if (propName.equals(PROP_BUSINESS_POSTAL_CODE)) {
            setBusinessPostalCode((String) value);
        }
        if (propName.equals(PROP_BUSINESS_COUNTRY_NAME)) {
            setBusinessCountryName((String) value);
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
        if (propName.equals(PROP_MIDDLE_NAME)) {
            setMiddleName((String) value);
        }
        if (propName.equals(PROP_HONORIFIC_PREFIX)) {
            setHonorificPrefix((String) value);
        }
        if (propName.equals(PROP_HONORIFIC_SUFFIX)) {
            setHonorificSuffix((String) value);
        }
        if (propName.equals(PROP_NICK_NAME)) {
            setNickName((String) value);
        }
        if (propName.equals(PROP_PROFILE_URL)) {
            setProfileUrl((String) value);
        }
        if (propName.equals(PROP_TIMEZONE)) {
            setTimezone((String) value);
        }
        if (propName.equals(PROP_LOCALE)) {
            setLocale((String) value);
        }
        if (propName.equals(PROP_ACTIVE)) {
            setActive((Boolean) value);
        }
        if (propName.equals(PROP_IMS)) {
            getIMs().add(((String) value));
        }

        if (extendedPropertiesDataType.containsKey(propName))
            setExtendedProperty(propName, value);

        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        /*
         * Require a property name.
         */
        if (propName == null || propName.trim().isEmpty()) {
            return;
        }

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
        if (propName.equals(PROP_HOME_STREET)) {
            unsetHomeStreet();
        }
        if (propName.equals(PROP_HOME_CITY)) {
            unsetHomeCity();
        }
        if (propName.equals(PROP_HOME_STATE_OR_PROVINCE_NAME)) {
            unsetHomeStateOrProvinceName();
        }
        if (propName.equals(PROP_HOME_POSTAL_CODE)) {
            unsetHomePostalAddress();
        }
        if (propName.equals(PROP_HOME_COUNTRY_NAME)) {
            unsetHomeCountryName();
        }
        if (propName.equals(PROP_BUSINESS_STREET)) {
            unsetBusinessStreet();
        }
        if (propName.equals(PROP_BUSINESS_CITY)) {
            unsetBusinessCity();
        }
        if (propName.equals(PROP_BUSINESS_STATE_OR_PROVINCE_NAME)) {
            unsetBusinessStateOrProvinceName();
        }
        if (propName.equals(PROP_BUSINESS_POSTAL_CODE)) {
            unsetBusinessPostalCode();
        }
        if (propName.equals(PROP_BUSINESS_COUNTRY_NAME)) {
            unsetBusinessCountryName();
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
        if (propName.equals(PROP_MIDDLE_NAME)) {
            unsetMiddleName();
        }
        if (propName.equals(PROP_HONORIFIC_PREFIX)) {
            unsetHonorificPrefix();
        }
        if (propName.equals(PROP_HONORIFIC_SUFFIX)) {
            unsetHonorificSuffix();
        }
        if (propName.equals(PROP_NICK_NAME)) {
            unsetNickName();
        }
        if (propName.equals(PROP_PROFILE_URL)) {
            unsetProfileUrl();
        }
        if (propName.equals(PROP_TIMEZONE)) {
            unsetTimezone();
        }
        if (propName.equals(PROP_LOCALE)) {
            unsetLocale();
        }
        if (propName.equals(PROP_ACTIVE)) {
            unsetActive();
        }
        if (propName.equals(PROP_IMS)) {
            unsetIMs();
        }

        if (extendedPropertiesDataType.containsKey(propName))
            unSetExtendedProperty(propName);

        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return "PersonAccount";
    }

    /**
     * Create the list of mandatory property names.
     */
    private static synchronized void setMandatoryPropertyNames() {
        if (mandatoryProperties != null) {
            return;
        }
        mandatoryProperties = new ArrayList<String>();
        mandatoryProperties.add(PROP_SN);
        mandatoryProperties.add(PROP_CN);
    }

    /**
     * Create the list of transient property names.
     */
    private static synchronized void setTransientPropertyNames() {
        if (transientProperties != null) {
            return;
        }
        transientProperties = new ArrayList<String>();
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

    /**
     * Get the list of transient property names.
     *
     * @return The list of transient property names.
     */
    protected static List<String> getTransientProperties() {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        return transientProperties;
    }

    /**
     * Re-initialize the property names.
     */
    public static synchronized void reInitializePropertyNames() {
        propertyNames = null;
        Entity.reInitializePropertyNames();
    }

    /**
     * Get the list of property names for the provided type.
     *
     * @param entityTypeName the type name.
     * @return The list of property names.
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
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
            names.add(PROP_DESCRIPTION);
            names.add(PROP_BUSINESS_CATEGORY);
            names.add(PROP_SEE_ALSO);
            names.add(PROP_KERBEROS_ID);
            names.add(PROP_PHOTO_URL);
            names.add(PROP_PHOTO_URL_THUMBNAIL);
            names.add(PROP_MIDDLE_NAME);
            names.add(PROP_HONORIFIC_PREFIX);
            names.add(PROP_HONORIFIC_SUFFIX);
            names.add(PROP_NICK_NAME);
            names.add(PROP_PROFILE_URL);
            names.add(PROP_TIMEZONE);
            names.add(PROP_LOCALE);
            names.add(PROP_ACTIVE);
            names.add(PROP_HOME_STREET);
            names.add(PROP_HOME_CITY);
            names.add(PROP_HOME_STATE_OR_PROVINCE_NAME);
            names.add(PROP_HOME_POSTAL_CODE);
            names.add(PROP_HOME_COUNTRY_NAME);
            names.add(PROP_BUSINESS_STREET);
            names.add(PROP_BUSINESS_CITY);
            names.add(PROP_BUSINESS_STATE_OR_PROVINCE_NAME);
            names.add(PROP_BUSINESS_POSTAL_CODE);
            names.add(PROP_BUSINESS_COUNTRY_NAME);
            names.add(PROP_IMS);

            if (extendedPropertiesDataType != null && extendedPropertiesDataType.keySet().size() > 0) {
                names.addAll(extendedPropertiesDataType.keySet());
            }

            names.addAll(LoginAccount.getPropertyNames(LoginAccount.TYPE_NAME));
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
        dataTypeMap.put(PROP_MANAGER, IdentifierType.TYPE_NAME);
        dataTypeMap.put(PROP_SECRETARY, IdentifierType.TYPE_NAME);
        dataTypeMap.put(PROP_DEPARTMENT_NUMBER, "String");
        dataTypeMap.put(PROP_TITLE, "String");
        dataTypeMap.put(PROP_IBM_JOB_TITLE, "String");
        dataTypeMap.put(PROP_C, "String");
        dataTypeMap.put(PROP_COUNTRY_NAME, "String");
        dataTypeMap.put(PROP_GIVEN_NAME, "String");
        dataTypeMap.put(PROP_DESCRIPTION, "String");
        dataTypeMap.put(PROP_BUSINESS_CATEGORY, "String");
        dataTypeMap.put(PROP_SEE_ALSO, "String");
        dataTypeMap.put(PROP_KERBEROS_ID, "String");
        dataTypeMap.put(PROP_PHOTO_URL, "String");
        dataTypeMap.put(PROP_PHOTO_URL_THUMBNAIL, "String");
        dataTypeMap.put(PROP_MIDDLE_NAME, "String");
        dataTypeMap.put(PROP_HONORIFIC_PREFIX, "String");
        dataTypeMap.put(PROP_HONORIFIC_SUFFIX, "String");
        dataTypeMap.put(PROP_NICK_NAME, "String");
        dataTypeMap.put(PROP_PROFILE_URL, "String");
        dataTypeMap.put(PROP_TIMEZONE, "String");
        dataTypeMap.put(PROP_LOCALE, "String");
        dataTypeMap.put(PROP_ACTIVE, "Boolean");
        dataTypeMap.put(PROP_HOME_STREET, "String");
        dataTypeMap.put(PROP_HOME_CITY, "String");
        dataTypeMap.put(PROP_HOME_STATE_OR_PROVINCE_NAME, "String");
        dataTypeMap.put(PROP_HOME_POSTAL_CODE, "String");
        dataTypeMap.put(PROP_HOME_COUNTRY_NAME, "String");
        dataTypeMap.put(PROP_BUSINESS_STREET, "String");
        dataTypeMap.put(PROP_BUSINESS_CITY, "String");
        dataTypeMap.put(PROP_BUSINESS_STATE_OR_PROVINCE_NAME, "String");
        dataTypeMap.put(PROP_BUSINESS_POSTAL_CODE, "String");
        dataTypeMap.put(PROP_BUSINESS_COUNTRY_NAME, "String");
        dataTypeMap.put(PROP_IMS, "String");
    }

    @Override
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return (dataTypeMap.get(propName));
        } else if (extendedPropertiesDataType.containsKey(propName)) {
            return extendedPropertiesDataType.get(propName);
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
        superTypeList.add(LoginAccount.TYPE_NAME);
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
     * Create the set of sub-types for this type.
     */
    private static synchronized void setSubTypes() {
        if (subTypeSet == null) {
            subTypeSet = new HashSet<String>();
        }
    }

    /**
     * Get the sub-types for this type.
     *
     * @return The set of sub-types.
     */
    public static HashSet<String> getSubTypes() {
        if (subTypeSet == null) {
            setSubTypes();
        }
        return subTypeSet;
    }

    /**
     * Get the value for an extended property.
     *
     * @param propName The property name for the extended property.
     * @return The value for the property.
     */
    private Object getExtendedProperty(String propName) {
        if (extendedProperties.containsKey(propName))
            return extendedProperties.get(propName);
        else if (extendedPropertiesDefaultValue.containsKey(propName))
            return extendedPropertiesDefaultValue.get(propName);
        else
            return null;
    }

    /**
     * Check if an extended property is set.
     *
     * @param property The name of the extended property.
     * @return True if the property is set; otherwise, false.
     */
    private boolean isSetExtendedProperty(String property) {
        if (extendedProperties.containsKey(property) || extendedPropertiesDefaultValue.containsKey(property))
            return true;
        else
            return false;
    }

    /**
     * Unset the extended property.
     *
     * @param property The property name of the extended property to unset.
     */
    private void unSetExtendedProperty(String property) {
        extendedProperties.remove(property);
    }

    /**
     * Set an extended property's value.
     *
     * @param property The property to set.
     * @param value The value to set.
     * @throws ClassCastException If the value was not of the correct data type.
     */
    @SuppressWarnings("unchecked")
    private void setExtendedProperty(String property, Object value) {
        String dataType = extendedPropertiesDataType.get(property);
        String valueClass = value.getClass().getSimpleName();

        if (dataType.equals(valueClass) && !extendedMultiValuedProperties.contains(property)) {
            extendedProperties.put(property, value);
        } else if (dataType.equals(valueClass) && extendedMultiValuedProperties.contains(property)) {
            if (value instanceof List) {
                extendedProperties.put(property, value);
            } else {
                List<Object> values = (List<Object>) extendedProperties.get(property);
                if (values == null) {
                    values = new ArrayList<Object>();
                    extendedProperties.put(property, values);
                }
                values.add(value);
            }
        } else {
            String type = value == null ? "null" : value.getClass().getName();
            String msg = "Could not set extended property for PersonAccount property '" + property + "'. " + type + " is incompatible with " + dataType;
            throw new ClassCastException(msg);
        }
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
     */
    public static void addExtendedProperty(String propName, String dataType, boolean multiValued, Object defaultValue) {
        if (dataType == null || "null".equalsIgnoreCase(dataType))
            return;

        if (extendedPropertiesDataType.containsKey(propName)) {
            Tr.warning(tc, WIMMessageKey.DUPLICATE_PROPERTY_EXTENDED, new Object[] { propName, "PersonAccount" });
            return;
        }

        if (getPropertyNames("PersonAccount").contains(propName)) {
            Tr.warning(tc, WIMMessageKey.DUPLICATE_PROPERTY_ENTITY, new Object[] { propName, "PersonAccount" });
            return;
        }

        extendedPropertiesDataType.put(propName, dataType);
        if (defaultValue != null)
            extendedPropertiesDefaultValue.put(propName, defaultValue);
        if (multiValued)
            extendedMultiValuedProperties.add(propName);
    }

    /**
     * Removes all extended properties defined in this PersonAccount entity
     */
    public static void clearExtendedProperties() {
        extendedPropertiesDataType.clear();
        extendedPropertiesDefaultValue.clear();
        extendedMultiValuedProperties.clear();
        reInitializePropertyNames();
    }

    /**
     * Returns a list of extended property names added to this PersonAccount entity
     *
     * @return
     *         returned object is a {@link Set}
     */
    public Set<String> getExtendedPropertyNames() {
        return new HashSet<String>(extendedPropertiesDataType.keySet());
    }

    @Override
    public boolean isMultiValuedProperty(String propName) {
        return MULTI_VALUED_PROPERTIES.contains(propName) || extendedMultiValuedProperties.contains(propName) || super.isMultiValuedProperty(propName);
    }
}
