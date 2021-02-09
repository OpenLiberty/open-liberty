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

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.ibm.wsspi.security.wim.model package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups. Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
@Trivial
public class ObjectFactory {

    private final static QName _SeeAlso_QNAME = new QName("http://www.ibm.com/websphere/wim", "seeAlso");
    private final static QName _PostalAddress_QNAME = new QName("http://www.ibm.com/websphere/wim", "postalAddress");
    private final static QName _Secretary_QNAME = new QName("http://www.ibm.com/websphere/wim", "secretary");
    private final static QName _Manager_QNAME = new QName("http://www.ibm.com/websphere/wim", "manager");
    private final static QName _Children_QNAME = new QName("http://www.ibm.com/websphere/wim", "children");
    private final static QName _KerberosId_QNAME = new QName("http://www.ibm.com/websphere/wim", "kerberosId");
    private final static QName _City_QNAME = new QName("http://www.ibm.com/websphere/wim", "city");
    private final static QName _Description_QNAME = new QName("http://www.ibm.com/websphere/wim", "description");
    private final static QName _Mail_QNAME = new QName("http://www.ibm.com/websphere/wim", "mail");
    private final static QName _Root_QNAME = new QName("http://www.ibm.com/websphere/wim", "Root");
    private final static QName _DisplayName_QNAME = new QName("http://www.ibm.com/websphere/wim", "displayName");
    private final static QName _Uid_QNAME = new QName("http://www.ibm.com/websphere/wim", "uid");
    private final static QName _Initials_QNAME = new QName("http://www.ibm.com/websphere/wim", "initials");
    private final static QName _Realm_QNAME = new QName("http://www.ibm.com/websphere/wim", "realm");
    private final static QName _CreateTimestamp_QNAME = new QName("http://www.ibm.com/websphere/wim", "createTimestamp");
    private final static QName _PrincipalName_QNAME = new QName("http://www.ibm.com/websphere/wim", "principalName");
    private final static QName _EmployeeNumber_QNAME = new QName("http://www.ibm.com/websphere/wim", "employeeNumber");
    private final static QName _ModifyTimestamp_QNAME = new QName("http://www.ibm.com/websphere/wim", "modifyTimestamp");
    private final static QName _PostalCode_QNAME = new QName("http://www.ibm.com/websphere/wim", "postalCode");
    private final static QName _JpegPhoto_QNAME = new QName("http://www.ibm.com/websphere/wim", "jpegPhoto");
    private final static QName _StateOrProvinceName_QNAME = new QName("http://www.ibm.com/websphere/wim", "stateOrProvinceName");
    private final static QName _LabeledURI_QNAME = new QName("http://www.ibm.com/websphere/wim", "labeledURI");
    private final static QName _Cn_QNAME = new QName("http://www.ibm.com/websphere/wim", "cn");
    private final static QName _Parent_QNAME = new QName("http://www.ibm.com/websphere/wim", "parent");
    private final static QName _IbmJobTitle_QNAME = new QName("http://www.ibm.com/websphere/wim", "ibm-jobTitle");
    private final static QName _Street_QNAME = new QName("http://www.ibm.com/websphere/wim", "street");
    private final static QName _IbmPrimaryEmail_QNAME = new QName("http://www.ibm.com/websphere/wim", "ibm-primaryEmail");
    private final static QName _Sn_QNAME = new QName("http://www.ibm.com/websphere/wim", "sn");
    private final static QName _Ou_QNAME = new QName("http://www.ibm.com/websphere/wim", "ou");
    private final static QName _Dc_QNAME = new QName("http://www.ibm.com/websphere/wim", "dc");
    private final static QName _St_QNAME = new QName("http://www.ibm.com/websphere/wim", "st");
    private final static QName _Certificate_QNAME = new QName("http://www.ibm.com/websphere/wim", "certificate");
    private final static QName _LocalityName_QNAME = new QName("http://www.ibm.com/websphere/wim", "localityName");
    private final static QName _Title_QNAME = new QName("http://www.ibm.com/websphere/wim", "title");
    private final static QName _GivenName_QNAME = new QName("http://www.ibm.com/websphere/wim", "givenName");
    private final static QName _DepartmentNumber_QNAME = new QName("http://www.ibm.com/websphere/wim", "departmentNumber");
    private final static QName _CarLicense_QNAME = new QName("http://www.ibm.com/websphere/wim", "carLicense");
    private final static QName _Mobile_QNAME = new QName("http://www.ibm.com/websphere/wim", "mobile");
    private final static QName _HomePostalAddress_QNAME = new QName("http://www.ibm.com/websphere/wim", "homePostalAddress");
    private final static QName _CountryName_QNAME = new QName("http://www.ibm.com/websphere/wim", "countryName");
    private final static QName _C_QNAME = new QName("http://www.ibm.com/websphere/wim", "c");
    private final static QName _Members_QNAME = new QName("http://www.ibm.com/websphere/wim", "members");
    private final static QName _Groups_QNAME = new QName("http://www.ibm.com/websphere/wim", "groups");
    private final static QName _Pager_QNAME = new QName("http://www.ibm.com/websphere/wim", "pager");
    private final static QName _PreferredLanguage_QNAME = new QName("http://www.ibm.com/websphere/wim", "preferredLanguage");
    private final static QName _BusinessAddress_QNAME = new QName("http://www.ibm.com/websphere/wim", "businessAddress");
    private final static QName _L_QNAME = new QName("http://www.ibm.com/websphere/wim", "l");
    private final static QName _O_QNAME = new QName("http://www.ibm.com/websphere/wim", "o");
    private final static QName _FacsimileTelephoneNumber_QNAME = new QName("http://www.ibm.com/websphere/wim", "facsimileTelephoneNumber");
    private final static QName _Password_QNAME = new QName("http://www.ibm.com/websphere/wim", "password");
    private final static QName _RoomNumber_QNAME = new QName("http://www.ibm.com/websphere/wim", "roomNumber");
    private final static QName _EmployeeType_QNAME = new QName("http://www.ibm.com/websphere/wim", "employeeType");
    private final static QName _BusinessCategory_QNAME = new QName("http://www.ibm.com/websphere/wim", "businessCategory");
    private final static QName _TelephoneNumber_QNAME = new QName("http://www.ibm.com/websphere/wim", "telephoneNumber");
    private final static QName _HomeAddress_QNAME = new QName("http://www.ibm.com/websphere/wim", "homeAddress");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.wsspi.security.wim.model
     *
     */
    public ObjectFactory() {}

    /**
     * Create an instance of {@link PropertyControl }
     *
     */
    public PropertyControl createPropertyControl() {
        return new PropertyControl();
    }

    /**
     * Create an instance of {@link Root }
     *
     */
    public Root createRoot() {
        return new Root();
    }

    /**
     * Create an instance of {@link AddressType }
     *
     */
    public AddressType createAddressType() {
        return new AddressType();
    }

    /**
     * Create an instance of {@link Entity }
     *
     */
    public Entity createEntity() {
        return new Entity();
    }

    /**
     * Create an instance of {@link IdentifierType }
     *
     */
    public IdentifierType createIdentifierType() {
        return new IdentifierType();
    }

    /**
     * Create an instance of {@link Group }
     *
     */
    public Group createGroup() {
        return new Group();
    }

    /**
     * Create an instance of {@link DescendantControl }
     *
     */
    public DescendantControl createDescendantControl() {
        return new DescendantControl();
    }

    /**
     * Create an instance of {@link Locality }
     *
     */
    public Locality createLocality() {
        return new Locality();
    }

    /**
     * Create an instance of {@link GroupMemberControl }
     *
     */
    public GroupMemberControl createGroupMemberControl() {
        return new GroupMemberControl();
    }

    /**
     * Create an instance of {@link Party }
     *
     */
    public Party createParty() {
        return new Party();
    }

    /**
     * Create an instance of {@link GroupMembershipControl }
     *
     */
    public GroupMembershipControl createGroupMembershipControl() {
        return new GroupMembershipControl();
    }

    /**
     * Create an instance of {@link HierarchyControl }
     *
     */
    public HierarchyControl createHierarchyControl() {
        return new HierarchyControl();
    }

    /**
     * Create an instance of {@link Context }
     *
     */
    public Context createContext() {
        return new Context();
    }

    /**
     * Create an instance of {@link DeleteControl }
     *
     */
    public DeleteControl createDeleteControl() {
        return new DeleteControl();
    }

    /**
     * Create an instance of {@link LoginControl }
     *
     */
    public LoginControl createLoginControl() {
        return new LoginControl();
    }

    /**
     * Create an instance of {@link RolePlayer }
     *
     */
    public RolePlayer createRolePlayer() {
        return new RolePlayer();
    }

    /**
     * Create an instance of {@link CacheControl }
     *
     */
    public CacheControl createCacheControl() {
        return new CacheControl();
    }

    /**
     * Create an instance of {@link SortKeyType }
     *
     */
    public SortKeyType createSortKeyType() {
        return new SortKeyType();
    }

    /**
     * Create an instance of {@link PartyRole }
     *
     */
    public PartyRole createPartyRole() {
        return new PartyRole();
    }

    /**
     * Create an instance of {@link OrgContainer }
     *
     */
    public OrgContainer createOrgContainer() {
        return new OrgContainer();
    }

    /**
     * Create an instance of {@link Container }
     *
     */
    public Container createContainer() {
        return new Container();
    }

    /**
     * Create an instance of {@link CheckPointType }
     *
     */
    public CheckPointType createCheckPointType() {
        return new CheckPointType();
    }

    /**
     * Create an instance of {@link LoginAccount }
     *
     */
    public LoginAccount createLoginAccount() {
        return new LoginAccount();
    }

    /**
     * Create an instance of {@link EntitlementType }
     *
     */
    public EntitlementType createEntitlementType() {
        return new EntitlementType();
    }

    /**
     * Create an instance of {@link ExternalNameControl }
     *
     */
    public ExternalNameControl createExternalNameControl() {
        return new ExternalNameControl();
    }

    /**
     * Create an instance of {@link ViewIdentifierType }
     *
     */
    public ViewIdentifierType createViewIdentifierType() {
        return new ViewIdentifierType();
    }

    /**
     * Create an instance of {@link LangType }
     *
     */
    public LangType createLangType() {
        return new LangType();
    }

    /**
     * Create an instance of {@link AncestorControl }
     *
     */
    public AncestorControl createAncestorControl() {
        return new AncestorControl();
    }

    /**
     * Create an instance of {@link ChangeResponseControl }
     *
     */
    public ChangeResponseControl createChangeResponseControl() {
        return new ChangeResponseControl();
    }

    /**
     * Create an instance of {@link GeographicLocation }
     *
     */
    public GeographicLocation createGeographicLocation() {
        return new GeographicLocation();
    }

    /**
     * Create an instance of {@link ChangeControl }
     *
     */
    public ChangeControl createChangeControl() {
        return new ChangeControl();
    }

    /**
     * Create an instance of {@link PageResponseControl }
     *
     */
    public PageResponseControl createPageResponseControl() {
        return new PageResponseControl();
    }

    /**
     * Create an instance of {@link PageControl }
     *
     */
    public PageControl createPageControl() {
        return new PageControl();
    }

    /**
     * Create an instance of {@link CheckGroupMembershipControl }
     *
     */
    public CheckGroupMembershipControl createCheckGroupMembershipControl() {
        return new CheckGroupMembershipControl();
    }

    /**
     * Create an instance of {@link SearchResponseControl }
     *
     */
    public SearchResponseControl createSearchResponseControl() {
        return new SearchResponseControl();
    }

    /**
     * Create an instance of {@link SearchControl }
     *
     */
    public SearchControl createSearchControl() {
        return new SearchControl();
    }

    /**
     * Create an instance of {@link Person }
     *
     */
    public Person createPerson() {
        return new Person();
    }

    /**
     * Create an instance of {@link EntitlementInfoType }
     *
     */
    public EntitlementInfoType createEntitlementInfoType() {
        return new EntitlementInfoType();
    }

    /**
     * Create an instance of {@link Country }
     *
     */
    public Country createCountry() {
        return new Country();
    }

    /**
     * Create an instance of {@link PersonAccount }
     *
     */
    public PersonAccount createPersonAccount() {
        return new PersonAccount();
    }

    /**
     * Create an instance of {@link GroupControl }
     *
     */
    public GroupControl createGroupControl() {
        return new GroupControl();
    }

    /**
     * Create an instance of {@link SortControl }
     *
     */
    public SortControl createSortControl() {
        return new SortControl();
    }

    /**
     * Create an instance of {@link PropertyControl.ContextProperties }
     *
     */
    public PropertyControl.ContextProperties createPropertyControlContextProperties() {
        return new PropertyControl.ContextProperties();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "seeAlso")
    public JAXBElement<String> createSeeAlso(String value) {
        return new JAXBElement<String>(_SeeAlso_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "postalAddress")
    public JAXBElement<String> createPostalAddress(String value) {
        return new JAXBElement<String>(_PostalAddress_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IdentifierType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "secretary")
    public JAXBElement<IdentifierType> createSecretary(IdentifierType value) {
        return new JAXBElement<IdentifierType>(_Secretary_QNAME, IdentifierType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IdentifierType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "manager")
    public JAXBElement<IdentifierType> createManager(IdentifierType value) {
        return new JAXBElement<IdentifierType>(_Manager_QNAME, IdentifierType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Entity }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "children")
    public JAXBElement<Entity> createChildren(Entity value) {
        return new JAXBElement<Entity>(_Children_QNAME, Entity.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "kerberosId")
    public JAXBElement<String> createKerberosId(String value) {
        return new JAXBElement<String>(_KerberosId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "city")
    public JAXBElement<String> createCity(String value) {
        return new JAXBElement<String>(_City_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "description")
    public JAXBElement<String> createDescription(String value) {
        return new JAXBElement<String>(_Description_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "mail")
    public JAXBElement<String> createMail(String value) {
        return new JAXBElement<String>(_Mail_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Root }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "Root")
    public JAXBElement<Root> createRoot(Root value) {
        return new JAXBElement<Root>(_Root_QNAME, Root.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "displayName")
    public JAXBElement<String> createDisplayName(String value) {
        return new JAXBElement<String>(_DisplayName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "uid")
    public JAXBElement<String> createUid(String value) {
        return new JAXBElement<String>(_Uid_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "initials")
    public JAXBElement<String> createInitials(String value) {
        return new JAXBElement<String>(_Initials_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "realm")
    public JAXBElement<String> createRealm(String value) {
        return new JAXBElement<String>(_Realm_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "createTimestamp")
    public JAXBElement<XMLGregorianCalendar> createCreateTimestamp(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_CreateTimestamp_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "principalName")
    public JAXBElement<String> createPrincipalName(String value) {
        return new JAXBElement<String>(_PrincipalName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "employeeNumber")
    public JAXBElement<String> createEmployeeNumber(String value) {
        return new JAXBElement<String>(_EmployeeNumber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "modifyTimestamp")
    public JAXBElement<XMLGregorianCalendar> createModifyTimestamp(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_ModifyTimestamp_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "postalCode")
    public JAXBElement<String> createPostalCode(String value) {
        return new JAXBElement<String>(_PostalCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <byte[]>}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "jpegPhoto")
    public JAXBElement<byte[]> createJpegPhoto(byte[] value) {
        return new JAXBElement<byte[]>(_JpegPhoto_QNAME, byte[].class, null, (value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "stateOrProvinceName")
    public JAXBElement<String> createStateOrProvinceName(String value) {
        return new JAXBElement<String>(_StateOrProvinceName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "labeledURI")
    public JAXBElement<String> createLabeledURI(String value) {
        return new JAXBElement<String>(_LabeledURI_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "cn")
    public JAXBElement<String> createCn(String value) {
        return new JAXBElement<String>(_Cn_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Entity }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "parent")
    public JAXBElement<Entity> createParent(Entity value) {
        return new JAXBElement<Entity>(_Parent_QNAME, Entity.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "ibm-jobTitle")
    public JAXBElement<String> createIbmJobTitle(String value) {
        return new JAXBElement<String>(_IbmJobTitle_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "street")
    public JAXBElement<String> createStreet(String value) {
        return new JAXBElement<String>(_Street_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "ibm-primaryEmail")
    public JAXBElement<String> createIbmPrimaryEmail(String value) {
        return new JAXBElement<String>(_IbmPrimaryEmail_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "sn")
    public JAXBElement<String> createSn(String value) {
        return new JAXBElement<String>(_Sn_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "ou")
    public JAXBElement<String> createOu(String value) {
        return new JAXBElement<String>(_Ou_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "dc")
    public JAXBElement<String> createDc(String value) {
        return new JAXBElement<String>(_Dc_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "st")
    public JAXBElement<String> createSt(String value) {
        return new JAXBElement<String>(_St_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <byte[]>}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "certificate")
    public JAXBElement<byte[]> createCertificate(byte[] value) {
        return new JAXBElement<byte[]>(_Certificate_QNAME, byte[].class, null, (value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "localityName")
    public JAXBElement<String> createLocalityName(String value) {
        return new JAXBElement<String>(_LocalityName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "title")
    public JAXBElement<String> createTitle(String value) {
        return new JAXBElement<String>(_Title_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "givenName")
    public JAXBElement<String> createGivenName(String value) {
        return new JAXBElement<String>(_GivenName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "departmentNumber")
    public JAXBElement<String> createDepartmentNumber(String value) {
        return new JAXBElement<String>(_DepartmentNumber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "carLicense")
    public JAXBElement<String> createCarLicense(String value) {
        return new JAXBElement<String>(_CarLicense_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "mobile")
    public JAXBElement<String> createMobile(String value) {
        return new JAXBElement<String>(_Mobile_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "homePostalAddress")
    public JAXBElement<String> createHomePostalAddress(String value) {
        return new JAXBElement<String>(_HomePostalAddress_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "countryName")
    public JAXBElement<String> createCountryName(String value) {
        return new JAXBElement<String>(_CountryName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "c")
    public JAXBElement<String> createC(String value) {
        return new JAXBElement<String>(_C_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Entity }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "members")
    public JAXBElement<Entity> createMembers(Entity value) {
        return new JAXBElement<Entity>(_Members_QNAME, Entity.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Group }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "groups")
    public JAXBElement<Group> createGroups(Group value) {
        return new JAXBElement<Group>(_Groups_QNAME, Group.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "pager")
    public JAXBElement<String> createPager(String value) {
        return new JAXBElement<String>(_Pager_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "preferredLanguage")
    public JAXBElement<String> createPreferredLanguage(String value) {
        return new JAXBElement<String>(_PreferredLanguage_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddressType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "businessAddress")
    public JAXBElement<AddressType> createBusinessAddress(AddressType value) {
        return new JAXBElement<AddressType>(_BusinessAddress_QNAME, AddressType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "l")
    public JAXBElement<String> createL(String value) {
        return new JAXBElement<String>(_L_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "o")
    public JAXBElement<String> createO(String value) {
        return new JAXBElement<String>(_O_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "facsimileTelephoneNumber")
    public JAXBElement<String> createFacsimileTelephoneNumber(String value) {
        return new JAXBElement<String>(_FacsimileTelephoneNumber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <byte[]>}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "password")
    public JAXBElement<byte[]> createPassword(byte[] value) {
        return new JAXBElement<byte[]>(_Password_QNAME, byte[].class, null, (value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "roomNumber")
    public JAXBElement<String> createRoomNumber(String value) {
        return new JAXBElement<String>(_RoomNumber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "employeeType")
    public JAXBElement<String> createEmployeeType(String value) {
        return new JAXBElement<String>(_EmployeeType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "businessCategory")
    public JAXBElement<String> createBusinessCategory(String value) {
        return new JAXBElement<String>(_BusinessCategory_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "telephoneNumber")
    public JAXBElement<String> createTelephoneNumber(String value) {
        return new JAXBElement<String>(_TelephoneNumber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddressType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://www.ibm.com/websphere/wim", name = "homeAddress")
    public JAXBElement<AddressType> createHomeAddress(AddressType value) {
        return new JAXBElement<AddressType>(_HomeAddress_QNAME, AddressType.class, null, value);
    }

}
