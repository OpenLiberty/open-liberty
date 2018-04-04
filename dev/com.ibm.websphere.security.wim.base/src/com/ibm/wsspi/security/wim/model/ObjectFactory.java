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

import static com.ibm.wsspi.security.wim.SchemaConstants.WIM_NS_URI;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.ibm.wsspi.security.wim.model.PropertyControl.ContextProperties;

/**
 * This object contains factory methods for each Java content interface and Java element interface
 * generated in the com.ibm.wsspi.security.wim.model package.
 *
 * <p>An ObjectFactory allows you to programmatically construct new instances of the Java representation
 * for XML content. The Java representation of XML content can consist of schema derived interfaces
 * and classes representing the binding of schema type definitions, element declarations and model
 * groups. Factory methods for each of these are provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _SeeAlso_QNAME = new QName(WIM_NS_URI, "seeAlso");
    private final static QName _PostalAddress_QNAME = new QName(WIM_NS_URI, "postalAddress");
    private final static QName _Secretary_QNAME = new QName(WIM_NS_URI, "secretary");
    private final static QName _Manager_QNAME = new QName(WIM_NS_URI, "manager");
    private final static QName _Children_QNAME = new QName(WIM_NS_URI, "children");
    private final static QName _KerberosId_QNAME = new QName(WIM_NS_URI, "kerberosId");
    private final static QName _City_QNAME = new QName(WIM_NS_URI, "city");
    private final static QName _Description_QNAME = new QName(WIM_NS_URI, "description");
    private final static QName _Mail_QNAME = new QName(WIM_NS_URI, "mail");
    private final static QName _Root_QNAME = new QName(WIM_NS_URI, "Root");
    private final static QName _DisplayName_QNAME = new QName(WIM_NS_URI, "displayName");
    private final static QName _Uid_QNAME = new QName(WIM_NS_URI, "uid");
    private final static QName _Initials_QNAME = new QName(WIM_NS_URI, "initials");
    private final static QName _Realm_QNAME = new QName(WIM_NS_URI, "realm");
    private final static QName _CreateTimestamp_QNAME = new QName(WIM_NS_URI, "createTimestamp");
    private final static QName _PrincipalName_QNAME = new QName(WIM_NS_URI, "principalName");
    private final static QName _EmployeeNumber_QNAME = new QName(WIM_NS_URI, "employeeNumber");
    private final static QName _ModifyTimestamp_QNAME = new QName(WIM_NS_URI, "modifyTimestamp");
    private final static QName _PostalCode_QNAME = new QName(WIM_NS_URI, "postalCode");
    private final static QName _JpegPhoto_QNAME = new QName(WIM_NS_URI, "jpegPhoto");
    private final static QName _StateOrProvinceName_QNAME = new QName(WIM_NS_URI, "stateOrProvinceName");
    private final static QName _LabeledURI_QNAME = new QName(WIM_NS_URI, "labeledURI");
    private final static QName _Cn_QNAME = new QName(WIM_NS_URI, "cn");
    private final static QName _Parent_QNAME = new QName(WIM_NS_URI, "parent");
    private final static QName _IbmJobTitle_QNAME = new QName(WIM_NS_URI, "ibm-jobTitle");
    private final static QName _Street_QNAME = new QName(WIM_NS_URI, "street");
    private final static QName _IbmPrimaryEmail_QNAME = new QName(WIM_NS_URI, "ibm-primaryEmail");
    private final static QName _Sn_QNAME = new QName(WIM_NS_URI, "sn");
    private final static QName _Ou_QNAME = new QName(WIM_NS_URI, "ou");
    private final static QName _Dc_QNAME = new QName(WIM_NS_URI, "dc");
    private final static QName _St_QNAME = new QName(WIM_NS_URI, "st");
    private final static QName _Certificate_QNAME = new QName(WIM_NS_URI, "certificate");
    private final static QName _LocalityName_QNAME = new QName(WIM_NS_URI, "localityName");
    private final static QName _Title_QNAME = new QName(WIM_NS_URI, "title");
    private final static QName _GivenName_QNAME = new QName(WIM_NS_URI, "givenName");
    private final static QName _DepartmentNumber_QNAME = new QName(WIM_NS_URI, "departmentNumber");
    private final static QName _CarLicense_QNAME = new QName(WIM_NS_URI, "carLicense");
    private final static QName _Mobile_QNAME = new QName(WIM_NS_URI, "mobile");
    private final static QName _HomePostalAddress_QNAME = new QName(WIM_NS_URI, "homePostalAddress");
    private final static QName _CountryName_QNAME = new QName(WIM_NS_URI, "countryName");
    private final static QName _C_QNAME = new QName(WIM_NS_URI, "c");
    private final static QName _Members_QNAME = new QName(WIM_NS_URI, "members");
    private final static QName _Groups_QNAME = new QName(WIM_NS_URI, "groups");
    private final static QName _Pager_QNAME = new QName(WIM_NS_URI, "pager");
    private final static QName _PreferredLanguage_QNAME = new QName(WIM_NS_URI, "preferredLanguage");
    private final static QName _BusinessAddress_QNAME = new QName(WIM_NS_URI, "businessAddress");
    private final static QName _L_QNAME = new QName(WIM_NS_URI, "l");
    private final static QName _O_QNAME = new QName(WIM_NS_URI, "o");
    private final static QName _FacsimileTelephoneNumber_QNAME = new QName(WIM_NS_URI, "facsimileTelephoneNumber");
    private final static QName _Password_QNAME = new QName(WIM_NS_URI, "password");
    private final static QName _RoomNumber_QNAME = new QName(WIM_NS_URI, "roomNumber");
    private final static QName _EmployeeType_QNAME = new QName(WIM_NS_URI, "employeeType");
    private final static QName _BusinessCategory_QNAME = new QName(WIM_NS_URI, "businessCategory");
    private final static QName _TelephoneNumber_QNAME = new QName(WIM_NS_URI, "telephoneNumber");
    private final static QName _HomeAddress_QNAME = new QName(WIM_NS_URI, "homeAddress");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.ibm.wsspi.security.wim.model
     *
     */
    public ObjectFactory() {}

    /**
     * Create an instance of {@link PropertyControl }
     *
     * @return A new {@link PropertyControl} instance.
     */
    public PropertyControl createPropertyControl() {
        return new PropertyControl();
    }

    /**
     * Create an instance of {@link Root }
     *
     * @return A new {@link Root} instance.
     */
    public Root createRoot() {
        return new Root();
    }

    /**
     * Create an instance of {@link AddressType }
     *
     * @return A new {@link AddressType} instance.
     */
    public AddressType createAddressType() {
        return new AddressType();
    }

    /**
     * Create an instance of {@link Entity }
     *
     * @return A new {@link Entity} instance.
     */
    public Entity createEntity() {
        return new Entity();
    }

    /**
     * Create an instance of {@link IdentifierType }
     *
     * @return A new {@link IdentifierType} instance.
     */
    public IdentifierType createIdentifierType() {
        return new IdentifierType();
    }

    /**
     * Create an instance of {@link Group }
     *
     * @return A new {@link Group} instance.
     */
    public Group createGroup() {
        return new Group();
    }

    /**
     * Create an instance of {@link DescendantControl }
     *
     * @return A new {@link DescendantControl} instance.
     */
    public DescendantControl createDescendantControl() {
        return new DescendantControl();
    }

    /**
     * Create an instance of {@link Locality }
     *
     * @return A new {@link Locality} instance.
     */
    public Locality createLocality() {
        return new Locality();
    }

    /**
     * Create an instance of {@link GroupMemberControl }
     *
     * @return A new {@link GroupMemberControl} instance.
     */
    public GroupMemberControl createGroupMemberControl() {
        return new GroupMemberControl();
    }

    /**
     * Create an instance of {@link Party }
     *
     * @return A new {@link Party} instance.
     */
    public Party createParty() {
        return new Party();
    }

    /**
     * Create an instance of {@link GroupMembershipControl }
     *
     * @return A new {@link GroupMembershipControl} instance.
     */
    public GroupMembershipControl createGroupMembershipControl() {
        return new GroupMembershipControl();
    }

    /**
     * Create an instance of {@link HierarchyControl }
     *
     * @return A new {@link HierarchyControl} instance.
     */
    public HierarchyControl createHierarchyControl() {
        return new HierarchyControl();
    }

    /**
     * Create an instance of {@link Context }
     *
     * @return A new {@link Context} instance.
     */
    public Context createContext() {
        return new Context();
    }

    /**
     * Create an instance of {@link DeleteControl }
     *
     * @return A new {@link DeleteControl} instance.
     */
    public DeleteControl createDeleteControl() {
        return new DeleteControl();
    }

    /**
     * Create an instance of {@link LoginControl }
     *
     * @return A new {@link LoginControl} instance.
     */
    public LoginControl createLoginControl() {
        return new LoginControl();
    }

    /**
     * Create an instance of {@link RolePlayer }
     *
     * @return A new {@link RolePlayer} instance.
     */
    public RolePlayer createRolePlayer() {
        return new RolePlayer();
    }

    /**
     * Create an instance of {@link CacheControl }
     *
     * @return A new {@link CacheControl} instance.
     */
    public CacheControl createCacheControl() {
        return new CacheControl();
    }

    /**
     * Create an instance of {@link SortKeyType }
     *
     * @return A new {@link SortKeyType} instance.
     */
    public SortKeyType createSortKeyType() {
        return new SortKeyType();
    }

    /**
     * Create an instance of {@link PartyRole }
     *
     * @return A new {@link PartyRole} instance.
     */
    public PartyRole createPartyRole() {
        return new PartyRole();
    }

    /**
     * Create an instance of {@link OrgContainer }
     *
     * @return A new {@link OrgContainer} instance.
     */
    public OrgContainer createOrgContainer() {
        return new OrgContainer();
    }

    /**
     * Create an instance of {@link Container }
     *
     * @return A new {@link Container} instance.
     */
    public Container createContainer() {
        return new Container();
    }

    /**
     * Create an instance of {@link CheckPointType }
     *
     * @return A new {@link CheckPointType} instance.
     */
    public CheckPointType createCheckPointType() {
        return new CheckPointType();
    }

    /**
     * Create an instance of {@link LoginAccount }
     *
     * @return A new {@link LoginAccount} instance.
     */
    public LoginAccount createLoginAccount() {
        return new LoginAccount();
    }

    /**
     * Create an instance of {@link EntitlementType }
     *
     * @return A new {@link EntitlementType} instance.
     */
    public EntitlementType createEntitlementType() {
        return new EntitlementType();
    }

    /**
     * Create an instance of {@link ExternalNameControl }
     *
     * @return A new {@link ExternalNameControl} instance.
     */
    public ExternalNameControl createExternalNameControl() {
        return new ExternalNameControl();
    }

    /**
     * Create an instance of {@link ViewIdentifierType }
     *
     * @return A new {@link ViewIdentifierType} instance.
     */
    public ViewIdentifierType createViewIdentifierType() {
        return new ViewIdentifierType();
    }

    /**
     * Create an instance of {@link LangType }
     *
     * @return A new {@link LangType} instance.
     */
    public LangType createLangType() {
        return new LangType();
    }

    /**
     * Create an instance of {@link AncestorControl }
     *
     * @return A new {@link AncestorControl} instance.
     */
    public AncestorControl createAncestorControl() {
        return new AncestorControl();
    }

    /**
     * Create an instance of {@link ChangeResponseControl }
     *
     * @return A new {@link ChangeResponseControl} instance.
     */
    public ChangeResponseControl createChangeResponseControl() {
        return new ChangeResponseControl();
    }

    /**
     * Create an instance of {@link GeographicLocation }
     *
     * @return A new {@link GeographicLocation} instance.
     */
    public GeographicLocation createGeographicLocation() {
        return new GeographicLocation();
    }

    /**
     * Create an instance of {@link ChangeControl }
     *
     * @return A new {@link ChangeControl} instance.
     */
    public ChangeControl createChangeControl() {
        return new ChangeControl();
    }

    /**
     * Create an instance of {@link PageResponseControl }
     *
     * @return A new {@link PageResponseControl} instance.
     */
    public PageResponseControl createPageResponseControl() {
        return new PageResponseControl();
    }

    /**
     * Create an instance of {@link PageControl }
     *
     * @return A new {@link PageControl} instance.
     */
    public PageControl createPageControl() {
        return new PageControl();
    }

    /**
     * Create an instance of {@link CheckGroupMembershipControl }
     *
     * @return A new {@link CheckGroupMembershipControl} instance.
     */
    public CheckGroupMembershipControl createCheckGroupMembershipControl() {
        return new CheckGroupMembershipControl();
    }

    /**
     * Create an instance of {@link SearchResponseControl }
     *
     * @return A new {@link SearchResponseControl} instance.
     */
    public SearchResponseControl createSearchResponseControl() {
        return new SearchResponseControl();
    }

    /**
     * Create an instance of {@link SearchControl }
     *
     * @return A new {@link SearchControl} instance.
     */
    public SearchControl createSearchControl() {
        return new SearchControl();
    }

    /**
     * Create an instance of {@link Person }
     *
     * @return A new {@link Person} instance.
     */
    public Person createPerson() {
        return new Person();
    }

    /**
     * Create an instance of {@link EntitlementInfoType }
     *
     * @return A new {@link EntitlementInfoType} instance.
     */
    public EntitlementInfoType createEntitlementInfoType() {
        return new EntitlementInfoType();
    }

    /**
     * Create an instance of {@link Country }
     *
     * @return A new {@link Country} instance.
     */
    public Country createCountry() {
        return new Country();
    }

    /**
     * Create an instance of {@link PersonAccount }
     *
     * @return A new {@link PersonAccount} instance.
     */
    public PersonAccount createPersonAccount() {
        return new PersonAccount();
    }

    /**
     * Create an instance of {@link GroupControl }
     *
     * @return A new {@link GroupControl} instance.
     */
    public GroupControl createGroupControl() {
        return new GroupControl();
    }

    /**
     * Create an instance of {@link SortControl }
     *
     * @return A new {@link SortControl} instance.
     */
    public SortControl createSortControl() {
        return new SortControl();
    }

    /**
     * Create an instance of {@link ContextProperties }
     *
     * @return A new {@link ContextProperties} instance.
     */
    public ContextProperties createPropertyControlContextProperties() {
        return new ContextProperties();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "seeAlso")
    public JAXBElement<String> createSeeAlso(String value) {
        return new JAXBElement<String>(_SeeAlso_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "postalAddress")
    public JAXBElement<String> createPostalAddress(String value) {
        return new JAXBElement<String>(_PostalAddress_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IdentifierType }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "secretary")
    public JAXBElement<IdentifierType> createSecretary(IdentifierType value) {
        return new JAXBElement<IdentifierType>(_Secretary_QNAME, IdentifierType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IdentifierType }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "manager")
    public JAXBElement<IdentifierType> createManager(IdentifierType value) {
        return new JAXBElement<IdentifierType>(_Manager_QNAME, IdentifierType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Entity }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "children")
    public JAXBElement<Entity> createChildren(Entity value) {
        return new JAXBElement<Entity>(_Children_QNAME, Entity.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "kerberosId")
    public JAXBElement<String> createKerberosId(String value) {
        return new JAXBElement<String>(_KerberosId_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "city")
    public JAXBElement<String> createCity(String value) {
        return new JAXBElement<String>(_City_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "description")
    public JAXBElement<String> createDescription(String value) {
        return new JAXBElement<String>(_Description_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "mail")
    public JAXBElement<String> createMail(String value) {
        return new JAXBElement<String>(_Mail_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Root }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "Root")
    public JAXBElement<Root> createRoot(Root value) {
        return new JAXBElement<Root>(_Root_QNAME, Root.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "displayName")
    public JAXBElement<String> createDisplayName(String value) {
        return new JAXBElement<String>(_DisplayName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "uid")
    public JAXBElement<String> createUid(String value) {
        return new JAXBElement<String>(_Uid_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "initials")
    public JAXBElement<String> createInitials(String value) {
        return new JAXBElement<String>(_Initials_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "realm")
    public JAXBElement<String> createRealm(String value) {
        return new JAXBElement<String>(_Realm_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "createTimestamp")
    public JAXBElement<XMLGregorianCalendar> createCreateTimestamp(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_CreateTimestamp_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "principalName")
    public JAXBElement<String> createPrincipalName(String value) {
        return new JAXBElement<String>(_PrincipalName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "employeeNumber")
    public JAXBElement<String> createEmployeeNumber(String value) {
        return new JAXBElement<String>(_EmployeeNumber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link XMLGregorianCalendar }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "modifyTimestamp")
    public JAXBElement<XMLGregorianCalendar> createModifyTimestamp(XMLGregorianCalendar value) {
        return new JAXBElement<XMLGregorianCalendar>(_ModifyTimestamp_QNAME, XMLGregorianCalendar.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "postalCode")
    public JAXBElement<String> createPostalCode(String value) {
        return new JAXBElement<String>(_PostalCode_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <byte[]>}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "jpegPhoto")
    public JAXBElement<byte[]> createJpegPhoto(byte[] value) {
        return new JAXBElement<byte[]>(_JpegPhoto_QNAME, byte[].class, null, (value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "stateOrProvinceName")
    public JAXBElement<String> createStateOrProvinceName(String value) {
        return new JAXBElement<String>(_StateOrProvinceName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "labeledURI")
    public JAXBElement<String> createLabeledURI(String value) {
        return new JAXBElement<String>(_LabeledURI_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "cn")
    public JAXBElement<String> createCn(String value) {
        return new JAXBElement<String>(_Cn_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Entity }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "parent")
    public JAXBElement<Entity> createParent(Entity value) {
        return new JAXBElement<Entity>(_Parent_QNAME, Entity.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "ibm-jobTitle")
    public JAXBElement<String> createIbmJobTitle(String value) {
        return new JAXBElement<String>(_IbmJobTitle_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "street")
    public JAXBElement<String> createStreet(String value) {
        return new JAXBElement<String>(_Street_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "ibm-primaryEmail")
    public JAXBElement<String> createIbmPrimaryEmail(String value) {
        return new JAXBElement<String>(_IbmPrimaryEmail_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "sn")
    public JAXBElement<String> createSn(String value) {
        return new JAXBElement<String>(_Sn_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "ou")
    public JAXBElement<String> createOu(String value) {
        return new JAXBElement<String>(_Ou_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "dc")
    public JAXBElement<String> createDc(String value) {
        return new JAXBElement<String>(_Dc_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "st")
    public JAXBElement<String> createSt(String value) {
        return new JAXBElement<String>(_St_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <byte[]>}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "certificate")
    public JAXBElement<byte[]> createCertificate(byte[] value) {
        return new JAXBElement<byte[]>(_Certificate_QNAME, byte[].class, null, (value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "localityName")
    public JAXBElement<String> createLocalityName(String value) {
        return new JAXBElement<String>(_LocalityName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "title")
    public JAXBElement<String> createTitle(String value) {
        return new JAXBElement<String>(_Title_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "givenName")
    public JAXBElement<String> createGivenName(String value) {
        return new JAXBElement<String>(_GivenName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "departmentNumber")
    public JAXBElement<String> createDepartmentNumber(String value) {
        return new JAXBElement<String>(_DepartmentNumber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "carLicense")
    public JAXBElement<String> createCarLicense(String value) {
        return new JAXBElement<String>(_CarLicense_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "mobile")
    public JAXBElement<String> createMobile(String value) {
        return new JAXBElement<String>(_Mobile_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "homePostalAddress")
    public JAXBElement<String> createHomePostalAddress(String value) {
        return new JAXBElement<String>(_HomePostalAddress_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "countryName")
    public JAXBElement<String> createCountryName(String value) {
        return new JAXBElement<String>(_CountryName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "c")
    public JAXBElement<String> createC(String value) {
        return new JAXBElement<String>(_C_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Entity }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "members")
    public JAXBElement<Entity> createMembers(Entity value) {
        return new JAXBElement<Entity>(_Members_QNAME, Entity.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Group }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "groups")
    public JAXBElement<Group> createGroups(Group value) {
        return new JAXBElement<Group>(_Groups_QNAME, Group.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "pager")
    public JAXBElement<String> createPager(String value) {
        return new JAXBElement<String>(_Pager_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "preferredLanguage")
    public JAXBElement<String> createPreferredLanguage(String value) {
        return new JAXBElement<String>(_PreferredLanguage_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddressType }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "businessAddress")
    public JAXBElement<AddressType> createBusinessAddress(AddressType value) {
        return new JAXBElement<AddressType>(_BusinessAddress_QNAME, AddressType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "l")
    public JAXBElement<String> createL(String value) {
        return new JAXBElement<String>(_L_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "o")
    public JAXBElement<String> createO(String value) {
        return new JAXBElement<String>(_O_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "facsimileTelephoneNumber")
    public JAXBElement<String> createFacsimileTelephoneNumber(String value) {
        return new JAXBElement<String>(_FacsimileTelephoneNumber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <byte[]>}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "password")
    public JAXBElement<byte[]> createPassword(byte[] value) {
        return new JAXBElement<byte[]>(_Password_QNAME, byte[].class, null, (value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "roomNumber")
    public JAXBElement<String> createRoomNumber(String value) {
        return new JAXBElement<String>(_RoomNumber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "employeeType")
    public JAXBElement<String> createEmployeeType(String value) {
        return new JAXBElement<String>(_EmployeeType_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "businessCategory")
    public JAXBElement<String> createBusinessCategory(String value) {
        return new JAXBElement<String>(_BusinessCategory_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "telephoneNumber")
    public JAXBElement<String> createTelephoneNumber(String value) {
        return new JAXBElement<String>(_TelephoneNumber_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddressType }{@code >}}
     *
     * @param value The value to set.
     * @return The {@link JAXBElement} containing the value.
     */
    @XmlElementDecl(namespace = WIM_NS_URI, name = "homeAddress")
    public JAXBElement<AddressType> createHomeAddress(AddressType value) {
        return new JAXBElement<AddressType>(_HomeAddress_QNAME, AddressType.class, null, value);
    }

}
