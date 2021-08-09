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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for Entity complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Entity">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="identifier" type="{http://www.ibm.com/websphere/wim}IdentifierType" minOccurs="0"/>
 * &lt;element name="viewIdentifiers" type="{http://www.ibm.com/websphere/wim}ViewIdentifierType" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}parent" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}children" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}groups" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}createTimestamp" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}modifyTimestamp" minOccurs="0"/>
 * &lt;element name="entitlementInfo" type="{http://www.ibm.com/websphere/wim}EntitlementInfoType" minOccurs="0"/>
 * &lt;element name="changeType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * <p> The Entity object represents a VMM entity. All other entity types, like Person, Group, and
 * OrgContainer are extended from this Entity object.
 *
 * <p> The Entity object has several properties: <b>identifier</b>, <b>viewIdentifiers</b>, <b>entitlementInfo</b>, and <b>changeType</b>,
 * each of which are represented themselves by objects.
 *
 * <ul>
 * <li><b>identifier</b>: contains a single IdentifierType object.</li>
 * <li><b>viewIdentifiers</b>: contains a list of ViewIdentifierType objects.</li>
 * <li><b>parent</b>: a containment property which is used to link to the parent of the entity in the VMM
 * hierarchy. It only contains a single Entity object since an entity can only have one parent. Also, since
 * any entity can be a parent of any entity, the object in the property is of Entity type.</li>
 * <li><b>children</b>: a containment property which is used to link to the children of the entity in the
 * VMM hierarchy. It contains multiple Entity objects since an entity can have multiple children. Also,
 * since any entity can be a child of another entity, the object in the property is of Entity type.</li>
 * <li><b>groups</b>: a containment property which is used to link to the groups this entity belongs to. It
 * contains multiple Group objects since an entity can belong to multiple groups.</li>
 * <li><b>createTimestamp</b>: indicates when the Entity was created.</li>
 * <li><b>modifyTimestamp</b>: indicates when the Entity was last modified.</li>
 * <li><b>changeType</b>: indicates the operation being performed on this Entity: add, delete, modify or rename.</li>
 * </ul>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Entity", propOrder = {
                                        "identifier",
                                        "viewIdentifiers",
                                        "parent",
                                        "children",
                                        "groups",
                                        "createTimestamp",
                                        "modifyTimestamp",
                                        "entitlementInfo",
                                        "changeType"
})
@XmlSeeAlso({
              Container.class,
              GeographicLocation.class,
              RolePlayer.class
})
@Trivial
public class Entity {

    private static final String PROP_IDENTIFIER = "identifier";
    private static final String PROP_VIEW_IDENTIFIERS = "viewIdentifiers";
    private static final String PROP_PARENT = "parent";
    private static final String PROP_CHILDREN = "children";
    private static final String PROP_GROUPS = "groups";
    private static final String PROP_CREATE_TIMESTAMP = "createTimestamp";
    private static final String PROP_MODIFY_TIMESTAMP = "modifyTimestamp";
    private static final String PROP_ENTITLEMENT_INFO = "entitlementInfo";
    private static final String PROP_CHANGE_TYPE = "changeType";

    protected IdentifierType identifier;
    protected List<com.ibm.wsspi.security.wim.model.ViewIdentifierType> viewIdentifiers;
    protected Entity parent;
    protected List<Entity> children;
    protected List<com.ibm.wsspi.security.wim.model.Group> groups;
    @XmlSchemaType(name = "dateTime")
    protected Date createTimestamp;
    @XmlSchemaType(name = "dateTime")
    protected Date modifyTimestamp;
    protected EntitlementInfoType entitlementInfo;
    protected String changeType;

    private List deletedProperties = null;
    private static List mandatoryProperties = null;
    private static List transientProperties = null;
    private static HashMap properties = null;
    private static List propertyNames = null;
    private static HashMap dataTypeMap = null;
    private static ArrayList superTypeList = null;
    private static HashSet subTypeList = null;
    static HashMap subTypeMap = null;

    /** The set of multi-valued properties for this entity type. */
    private static final Set<String> MULTI_VALUED_PROPERTIES;

    @SuppressWarnings("unused")
    private final static TraceComponent tc = Tr.register(Entity.class);

    static {
        setMandatoryPropertyNames();
        setTransientPropertyNames();
        getTransientProperties();
        setPropertyNames();
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();
        setSubTypeMap();

        MULTI_VALUED_PROPERTIES = new HashSet<String>();
        MULTI_VALUED_PROPERTIES.add(PROP_CHILDREN);
        MULTI_VALUED_PROPERTIES.add(PROP_GROUPS);
        MULTI_VALUED_PROPERTIES.add(PROP_VIEW_IDENTIFIERS);
    }

    /**
     * Gets the value of the <b>identifier</b> property.
     *
     * @return
     *         possible object is {@link IdentifierType }
     *
     */
    public IdentifierType getIdentifier() {
        return identifier;
    }

    /**
     * Sets the value of the <b>identifier</b> property.
     *
     * @param value
     *            allowed object is {@link IdentifierType }
     *
     */
    public void setIdentifier(IdentifierType value) {
        this.identifier = value;
    }

    /**
     * Returns true if the <b>identifier</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */

    public boolean isSetIdentifier() {
        return (this.identifier != null);
    }

    /**
     * Gets the value of the <b>viewIdentifiers</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the viewIdentifiers property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getViewIdentifiers().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.ViewIdentifierType }
     *
     *
     */
    public List<com.ibm.wsspi.security.wim.model.ViewIdentifierType> getViewIdentifiers() {
        if (viewIdentifiers == null) {
            viewIdentifiers = new ArrayList<com.ibm.wsspi.security.wim.model.ViewIdentifierType>();
        }
        return this.viewIdentifiers;
    }

    /**
     * Returns true if the <b>viewIdentifiers</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetViewIdentifiers() {
        return ((this.viewIdentifiers != null) && (!this.viewIdentifiers.isEmpty()));
    }

    /**
     * Resets the <b>viewIdentifiers</b> property to null.
     *
     */
    public void unsetViewIdentifiers() {
        this.viewIdentifiers = null;
    }

    /**
     * Gets the value of the <b>parent</b> property.
     *
     * @return
     *         possible object is {@link Entity }
     *
     */
    public Entity getParent() {
        return parent;
    }

    /**
     * Sets the value of the <b>parent</b> property.
     *
     * @param value
     *            allowed object is {@link Entity }
     *
     */
    public void setParent(Entity value) {
        this.parent = value;
    }

    /**
     * Returns true if the <b>parent</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetParent() {
        return (this.parent != null);
    }

    /**
     * Gets the value of the <b>children</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the children property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getChildren().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link Entity }
     *
     *
     */
    public List<Entity> getChildren() {
        if (children == null) {
            children = new ArrayList<Entity>();
        }
        return this.children;
    }

    /**
     * Returns true if the <b>children</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetChildren() {
        return ((this.children != null) && (!this.children.isEmpty()));
    }

    /**
     * Resets the <b>children</b> property to null.
     *
     */
    public void unsetChildren() {
        this.children = null;
    }

    /**
     * Gets the value of the <b>groups</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the groups property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getGroups().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.Group }
     *
     *
     */
    public List<com.ibm.wsspi.security.wim.model.Group> getGroups() {
        if (groups == null) {
            groups = new ArrayList<com.ibm.wsspi.security.wim.model.Group>();
        }
        return this.groups;
    }

    /**
     * Returns true if the <b>groups</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */

    public boolean isSetGroups() {
        return ((this.groups != null) && (!this.groups.isEmpty()));
    }

    /**
     * Resets the <b>groups</b> property to null.
     *
     */

    public void unsetGroups() {
        this.groups = null;
    }

    /**
     * Gets the value of the <b>createTimestamp</b> property.
     *
     * @return
     *         possible object is {@link Date }
     *
     */
    public Date getCreateTimestamp() {
        return createTimestamp;
    }

    /**
     * Sets the value of the <b>createTimestamp</b> property.
     *
     * @param value
     *            allowed object is {@link Date }
     *
     */
    public void setCreateTimestamp(Date value) {
        this.createTimestamp = value;
    }

    /**
     * Returns true if the <b>createTimestamp</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetCreateTimestamp() {
        return (this.createTimestamp != null);
    }

    /**
     * Gets the value of the <b>modifyTimestamp</b> property.
     *
     * @return
     *         possible object is {@link Date }
     *
     */
    public Date getModifyTimestamp() {
        return modifyTimestamp;
    }

    /**
     * Sets the value of the <b>modifyTimestamp</b> property.
     *
     * @param value
     *            allowed object is {@link Date }
     *
     */
    public void setModifyTimestamp(Date value) {
        this.modifyTimestamp = value;
    }

    /**
     * Returns true if the <b>modifyTimeStamp</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetModifyTimestamp() {
        return (this.modifyTimestamp != null);
    }

    /**
     * Gets the value of the <b>entitlementInfo</b> property.
     *
     * @return
     *         possible object is {@link EntitlementInfoType }
     *
     */
    public EntitlementInfoType getEntitlementInfo() {
        return entitlementInfo;
    }

    /**
     * Sets the value of the <b>entitlementInfo</b> property.
     *
     * @param value
     *            allowed object is {@link EntitlementInfoType }
     *
     */
    public void setEntitlementInfo(EntitlementInfoType value) {
        this.entitlementInfo = value;
    }

    /**
     * Returns true if the <b>entitlementInfo</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetEntitlementInfo() {
        return (this.entitlementInfo != null);
    }

    /**
     * Gets the value of the <b>changeType</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getChangeType() {
        return changeType;
    }

    /**
     * Sets the value of the <b>changeType</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setChangeType(String value) {
        this.changeType = value;
    }

    /**
     * Returns true if the <b>changeType</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetChangeType() {
        return (this.changeType != null);
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
        if (propName.equals(PROP_IDENTIFIER)) {
            return getIdentifier();
        }
        if (propName.equals(PROP_VIEW_IDENTIFIERS)) {
            return getViewIdentifiers();
        }
        if (propName.equals(PROP_PARENT)) {
            return getParent();
        }
        if (propName.equals(PROP_CHILDREN)) {
            return getChildren();
        }
        if (propName.equals(PROP_GROUPS)) {
            return getGroups();
        }
        if (propName.equals(PROP_CREATE_TIMESTAMP)) {
            return getCreateTimestamp();
        }
        if (propName.equals(PROP_MODIFY_TIMESTAMP)) {
            return getModifyTimestamp();
        }
        if (propName.equals(PROP_ENTITLEMENT_INFO)) {
            return getEntitlementInfo();
        }
        if (propName.equals(PROP_CHANGE_TYPE)) {
            return getChangeType();
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
        if (propName.equals(PROP_IDENTIFIER)) {
            return isSetIdentifier();
        }
        if (propName.equals(PROP_VIEW_IDENTIFIERS)) {
            return isSetViewIdentifiers();
        }
        if (propName.equals(PROP_PARENT)) {
            return isSetParent();
        }
        if (propName.equals(PROP_CHILDREN)) {
            return isSetChildren();
        }
        if (propName.equals(PROP_GROUPS)) {
            return isSetGroups();
        }
        if (propName.equals(PROP_CREATE_TIMESTAMP)) {
            return isSetCreateTimestamp();
        }
        if (propName.equals(PROP_MODIFY_TIMESTAMP)) {
            return isSetModifyTimestamp();
        }
        if (propName.equals(PROP_ENTITLEMENT_INFO)) {
            return isSetEntitlementInfo();
        }
        if (propName.equals(PROP_CHANGE_TYPE)) {
            return isSetChangeType();
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
        if (propName.equals(PROP_IDENTIFIER)) {
            setIdentifier(((IdentifierType) value));
        }
        if (propName.equals(PROP_VIEW_IDENTIFIERS)) {
            getViewIdentifiers().add(((com.ibm.wsspi.security.wim.model.ViewIdentifierType) value));
        }
        if (propName.equals(PROP_PARENT)) {
            setParent(((Entity) value));
        }
        if (propName.equals(PROP_CHILDREN)) {
            getChildren().add(((com.ibm.wsspi.security.wim.model.Entity) value));
        }
        if (propName.equals(PROP_GROUPS)) {
            getGroups().add(((com.ibm.wsspi.security.wim.model.Group) value));
        }
        if (propName.equals(PROP_CREATE_TIMESTAMP)) {
            setCreateTimestamp(((Date) value));
        }
        if (propName.equals(PROP_MODIFY_TIMESTAMP)) {
            setModifyTimestamp(((Date) value));
        }
        if (propName.equals(PROP_ENTITLEMENT_INFO)) {
            setEntitlementInfo(((EntitlementInfoType) value));
        }
        if (propName.equals(PROP_CHANGE_TYPE)) {
            setChangeType(((String) value));
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
        if (propName.equals(PROP_VIEW_IDENTIFIERS)) {
            unsetViewIdentifiers();
        }
        if (propName.equals(PROP_CHILDREN)) {
            unsetChildren();
        }
        if (propName.equals(PROP_GROUPS)) {
            unsetGroups();
        }

        if (deletedProperties == null) {
            deletedProperties = new ArrayList();
        }
        deletedProperties.add(propName);
    }

    /**
     * Gets the name of this model object, <b>Entity</b>
     *
     * @return
     *         returned object is {@link String}
     */
    public String getTypeName() {
        return "Entity";
    }

    private static synchronized void setMandatoryPropertyNames() {
        if (mandatoryProperties != null) {
            return;
        }
        mandatoryProperties = new ArrayList();
        mandatoryProperties.add(PROP_IDENTIFIER);
        mandatoryProperties.add(PROP_CREATE_TIMESTAMP);
    }

    private static synchronized void setTransientPropertyNames() {
        if (transientProperties != null) {
            return;
        }
        transientProperties = new ArrayList();
        transientProperties.add(PROP_IDENTIFIER);
        transientProperties.add(PROP_VIEW_IDENTIFIERS);
        transientProperties.add(PROP_PARENT);
        transientProperties.add(PROP_CHILDREN);
        transientProperties.add(PROP_GROUPS);
        transientProperties.add(PROP_ENTITLEMENT_INFO);
        transientProperties.add(PROP_CHANGE_TYPE);
    }

    /**
     * Returns true if the provided property is a mandatory property; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */

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

    /**
     * Returns true if the provided property is a persistent property; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */

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
        setPropertyNames();
    }

    private static synchronized void setPropertyNames() {
        if (properties == null) {
            properties = new HashMap();
        }
        properties.put("Entity", Entity.getPropertyNames("Entity"));
        properties.put("Group", com.ibm.wsspi.security.wim.model.Group.getPropertyNames("Group"));
        properties.put("Locality", Locality.getPropertyNames("Locality"));
        properties.put("Party", Party.getPropertyNames("Party"));
        properties.put("RolePlayer", RolePlayer.getPropertyNames("RolePlayer"));
        properties.put("PartyRole", PartyRole.getPropertyNames("PartyRole"));
        properties.put("OrgContainer", OrgContainer.getPropertyNames("OrgContainer"));
        properties.put("Container", Container.getPropertyNames("Container"));
        properties.put("LoginAccount", com.ibm.wsspi.security.wim.model.LoginAccount.getPropertyNames("LoginAccount"));
        properties.put("GeographicLocation", GeographicLocation.getPropertyNames("GeographicLocation"));
        properties.put("Person", Person.getPropertyNames("Person"));
        properties.put("Country", Country.getPropertyNames("Country"));
        properties.put("PersonAccount", PersonAccount.getPropertyNames("PersonAccount"));
    }

    /**
     * Gets a list of all supported properties for this model object, <b>Entity</b>
     *
     * @param entityTypeName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link List}
     */
    public static synchronized List getPropertyNames(String entityTypeName) {
        if (entityTypeName == null) {
            return null;
        }
        if (!entityTypeName.equals("Entity")) {
            return (List) properties.get(entityTypeName);
        }
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add(PROP_IDENTIFIER);
                names.add(PROP_VIEW_IDENTIFIERS);
                names.add(PROP_PARENT);
                names.add(PROP_CHILDREN);
                names.add(PROP_GROUPS);
                names.add(PROP_CREATE_TIMESTAMP);
                names.add(PROP_MODIFY_TIMESTAMP);
                names.add(PROP_ENTITLEMENT_INFO);
                names.add(PROP_CHANGE_TYPE);
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put(PROP_IDENTIFIER, "IdentifierType");
        dataTypeMap.put(PROP_VIEW_IDENTIFIERS, "ViewIdentifierType");
        dataTypeMap.put(PROP_PARENT, "Entity");
        dataTypeMap.put(PROP_CHILDREN, "Entity");
        dataTypeMap.put(PROP_GROUPS, "Group");
        dataTypeMap.put(PROP_CREATE_TIMESTAMP, "Date");
        dataTypeMap.put(PROP_MODIFY_TIMESTAMP, "Date");
        dataTypeMap.put(PROP_ENTITLEMENT_INFO, "EntitlementInfoType");
        dataTypeMap.put(PROP_CHANGE_TYPE, "String");
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
     * Gets a list of any model objects which this model object, <b>Entity</b>, is
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
        subTypeList.add("Group");
        subTypeList.add("Locality");
        subTypeList.add("Party");
        subTypeList.add("RolePlayer");
        subTypeList.add("PartyRole");
        subTypeList.add("OrgContainer");
        subTypeList.add("Container");
        subTypeList.add("LoginAccount");
        subTypeList.add("GeographicLocation");
        subTypeList.add("Person");
        subTypeList.add("Country");
        subTypeList.add("PersonAccount");
    }

    private static synchronized void setSubTypeMap() {
        if (subTypeMap == null) {
            subTypeMap = new HashMap();
        }
        subTypeMap.put("Entity", Entity.getSubTypes());
        subTypeMap.put("Group", com.ibm.wsspi.security.wim.model.Group.getSubTypes());
        subTypeMap.put("Locality", Locality.getSubTypes());
        subTypeMap.put("Party", Party.getSubTypes());
        subTypeMap.put("RolePlayer", RolePlayer.getSubTypes());
        subTypeMap.put("PartyRole", PartyRole.getSubTypes());
        subTypeMap.put("OrgContainer", OrgContainer.getSubTypes());
        subTypeMap.put("Container", Container.getSubTypes());
        subTypeMap.put("LoginAccount", com.ibm.wsspi.security.wim.model.LoginAccount.getSubTypes());
        subTypeMap.put("GeographicLocation", GeographicLocation.getSubTypes());
        subTypeMap.put("Person", Person.getSubTypes());
        subTypeMap.put("Country", Country.getSubTypes());
        subTypeMap.put("PersonAccount", PersonAccount.getSubTypes());
    }

    public static HashSet getSubEntityTypes(String entityTypeName) {
        HashSet hs = ((HashSet) subTypeMap.get(entityTypeName));
        if (hs == null) {
            if ("LoginAccount".equals(entityTypeName)) {
                subTypeMap.put("LoginAccount", LoginAccount.getSubTypes());
            }
        }

        return ((HashSet) subTypeMap.get(entityTypeName));
    }

    /**
     * Gets a set of any model objects which extend this model object, <b>Entity</b>
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
     * Returns this model object, <b>Entity</b>, and its contents as a String
     *
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

    /**
     * @param propertyName
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isUnset(String propName) {
        if (deletedProperties != null) {
            if (deletedProperties.contains(propName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return whether the specified property is a multi-valued property and capable of holding
     * multiple values.
     *
     * @param propName The property name to check.
     * @return True if the property is multi-value, false otherwise.
     */
    public boolean isMultiValuedProperty(String propName) {
        return MULTI_VALUED_PROPERTIES.contains(propName);
    }
}
