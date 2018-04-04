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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for Entity complex type.
 *
 * <p> The Entity object represents a VMM entity. All other entity types, like {@link Person}, {@link Group}, and
 * {@link OrgContainer} are extended from this Entity object.
 *
 * <p>Below is a list of supported properties for {@link Entity}.
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
 * <li><b>entitlementInfo</b>: contains entitlement info for the Entity.</li>
 * <li><b>changeType</b>: indicates the operation being performed on this Entity: add, delete, modify or rename.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Entity.TYPE_NAME, propOrder = {
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
public class Entity {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "Entity";

    /** Property name constant for the <b>identifier</b> property. */
    private static final String PROP_IDENTIFIER = "identifier";

    /** Property name constant for the <b>viewIdentifiers</b> property. */
    private static final String PROP_VIEW_IDENTIFIERS = "viewIdentifiers";

    /** Property name constant for the <b>parent</b> property. */
    private static final String PROP_PARENT = "parent";

    /** Property name constant for the <b>children</b> property. */
    private static final String PROP_CHILDREN = "children";

    /** Property name constant for the <b>groups</b> property. */
    private static final String PROP_GROUPS = "groups";

    /** Property name constant for the <b>createTimestamp</b> property. */
    private static final String PROP_CREATE_TIMESTAMP = "createTimestamp";

    /** Property name constant for the <b>modifyTimestamp</b> property. */
    private static final String PROP_MODIFY_TIMESTAMP = "modifyTimestamp";

    /** Property name constant for the <b>entitlementInfo</b> property. */
    private static final String PROP_ENTITLEMENT_INFO = "entitlementInfo";

    /** Property name constant for the <b>changeType</b> property. */
    private static final String PROP_CHANGE_TYPE = "changeType";

    /**
     * A single IdentifierType object.
     */
    @XmlElement(name = PROP_IDENTIFIER)
    protected IdentifierType identifier;

    /**
     * A list of ViewIdentifierType objects.
     */
    @XmlElement(name = PROP_VIEW_IDENTIFIERS)
    protected List<ViewIdentifierType> viewIdentifiers;

    /**
     * A containment property which is used to link to the parent of the entity in the VMM
     * hierarchy. It only contains a single Entity object since an entity can only have one parent. Also, since
     * any entity can be a parent of any entity, the object in the property is of Entity type.
     */
    @XmlElement(name = PROP_PARENT)
    protected Entity parent;

    /**
     * A containment property which is used to link to the children of the entity in the
     * VMM hierarchy. It contains multiple Entity objects since an entity can have multiple children. Also,
     * since any entity can be a child of another entity, the object in the property is of Entity type.
     */
    @XmlElement(name = PROP_CHILDREN)
    protected List<Entity> children;

    /**
     * A containment property which is used to link to the groups this entity belongs to. It
     * contains multiple Group objects since an entity can belong to multiple groups.
     */
    @XmlElement(name = PROP_GROUPS)
    protected List<Group> groups;

    /**
     * Indicates when the Entity was created.
     */
    @XmlElement(name = PROP_CREATE_TIMESTAMP)
    @XmlSchemaType(name = "dateTime")
    protected Date createTimestamp;

    /**
     * Indicates when the Entity was last modified.
     */
    @XmlElement(name = PROP_MODIFY_TIMESTAMP)
    @XmlSchemaType(name = "dateTime")
    protected Date modifyTimestamp;

    /**
     * Contains entitlement info for the Entity.
     */
    @XmlElement(name = PROP_ENTITLEMENT_INFO)
    protected EntitlementInfoType entitlementInfo;

    /**
     * Indicates the operation being performed on this Entity: add, delete, modify or rename.
     */
    @XmlElement(name = PROP_CHANGE_TYPE)
    protected String changeType;

    /** Properties whose value have been deleted. */
    private List<String> deletedProperties = null;

    /** Properties which are mandatory. */
    private static List<String> mandatoryProperties = null;

    /** Transient properties. */
    private static List<String> transientProperties = null;

    /** Map of entity types to properties. */
    private static HashMap<String, List<String>> properties = null;

    /** The list of properties that comprise this type. */
    private static List<String> propertyNames = null;

    /** A mapping of property names to data types. */
    private static HashMap<String, String> dataTypeMap = null;

    /** A list of super-types of this type. */
    private static ArrayList<String> superTypeList = null;

    /** A set of sub-types of this type. */
    private static HashSet<String> subTypeSet = null;

    /** Map of entity types to sub types. */
    private static HashMap<String, Set<String>> subTypeMap = null;

    /** The set of multi-valued properties for this type. */
    private static final Set<String> MULTI_VALUED_PROPERTIES;

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
     */
    public IdentifierType getIdentifier() {
        return identifier;
    }

    /**
     * Sets the value of the <b>identifier</b> property.
     *
     * @param value
     *            allowed object is {@link IdentifierType }
     */
    public void setIdentifier(IdentifierType value) {
        this.identifier = value;
    }

    /**
     * Returns true if the <b>identifier</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
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
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the viewIdentifiers property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getViewIdentifiers().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link ViewIdentifierType }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<ViewIdentifierType> getViewIdentifiers() {
        if (viewIdentifiers == null) {
            viewIdentifiers = new ArrayList<ViewIdentifierType>();
        }
        return this.viewIdentifiers;
    }

    /**
     * Returns true if the <b>viewIdentifiers</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetViewIdentifiers() {
        return ((this.viewIdentifiers != null) && (!this.viewIdentifiers.isEmpty()));
    }

    /**
     * Resets the <b>viewIdentifiers</b> property to null.
     */
    public void unsetViewIdentifiers() {
        this.viewIdentifiers = null;
    }

    /**
     * Gets the value of the <b>parent</b> property.
     *
     * @return
     *         possible object is {@link Entity }
     */
    public Entity getParent() {
        return parent;
    }

    /**
     * Sets the value of the <b>parent</b> property.
     *
     * @param value
     *            allowed object is {@link Entity }
     */
    public void setParent(Entity value) {
        this.parent = value;
    }

    /**
     * Returns true if the <b>parent</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
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
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the children property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getChildren().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link Entity }
     *
     * @return
     *         returned object is {@link List}
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
     */
    public boolean isSetChildren() {
        return ((this.children != null) && (!this.children.isEmpty()));
    }

    /**
     * Resets the <b>children</b> property to null.
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
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the groups property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getGroups().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link Group }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<Group> getGroups() {
        if (groups == null) {
            groups = new ArrayList<Group>();
        }
        return this.groups;
    }

    /**
     * Returns true if the <b>groups</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetGroups() {
        return ((this.groups != null) && (!this.groups.isEmpty()));
    }

    /**
     * Resets the <b>groups</b> property to null.
     */
    public void unsetGroups() {
        this.groups = null;
    }

    /**
     * Gets the value of the <b>createTimestamp</b> property.
     *
     * @return
     *         possible object is {@link Date }
     */
    public Date getCreateTimestamp() {
        return createTimestamp;
    }

    /**
     * Sets the value of the <b>createTimestamp</b> property.
     *
     * @param value
     *            allowed object is {@link Date }
     */
    public void setCreateTimestamp(Date value) {
        this.createTimestamp = value;
    }

    /**
     * Returns true if the <b>createTimestamp</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetCreateTimestamp() {
        return (this.createTimestamp != null);
    }

    /**
     * Gets the value of the <b>modifyTimestamp</b> property.
     *
     * @return
     *         possible object is {@link Date }
     */
    public Date getModifyTimestamp() {
        return modifyTimestamp;
    }

    /**
     * Sets the value of the <b>modifyTimestamp</b> property.
     *
     * @param value
     *            allowed object is {@link Date }
     */
    public void setModifyTimestamp(Date value) {
        this.modifyTimestamp = value;
    }

    /**
     * Returns true if the <b>modifyTimeStamp</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetModifyTimestamp() {
        return (this.modifyTimestamp != null);
    }

    /**
     * Gets the value of the <b>entitlementInfo</b> property.
     *
     * @return
     *         possible object is {@link EntitlementInfoType }
     */
    public EntitlementInfoType getEntitlementInfo() {
        return entitlementInfo;
    }

    /**
     * Sets the value of the <b>entitlementInfo</b> property.
     *
     * @param value
     *            allowed object is {@link EntitlementInfoType }
     */
    public void setEntitlementInfo(EntitlementInfoType value) {
        this.entitlementInfo = value;
    }

    /**
     * Returns true if the <b>entitlementInfo</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetEntitlementInfo() {
        return (this.entitlementInfo != null);
    }

    /**
     * Gets the value of the <b>changeType</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getChangeType() {
        return changeType;
    }

    /**
     * Sets the value of the <b>changeType</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setChangeType(String value) {
        this.changeType = value;
    }

    /**
     * Returns true if the <b>changeType</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
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
     * @param propName
     *            Property name to check if set.
     * @return
     *         returned object is {@link boolean }
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
     */
    public void set(String propName, Object value) {
        if (propName.equals(PROP_IDENTIFIER)) {
            setIdentifier(((IdentifierType) value));
        }
        if (propName.equals(PROP_VIEW_IDENTIFIERS)) {
            getViewIdentifiers().add(((ViewIdentifierType) value));
        }
        if (propName.equals(PROP_PARENT)) {
            setParent(((Entity) value));
        }
        if (propName.equals(PROP_CHILDREN)) {
            getChildren().add(((Entity) value));
        }
        if (propName.equals(PROP_GROUPS)) {
            getGroups().add(((Group) value));
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
            deletedProperties = new ArrayList<String>();
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
        return TYPE_NAME;
    }

    /**
     * Set the names of any mandatory properties.
     */
    private static synchronized void setMandatoryPropertyNames() {
        if (mandatoryProperties != null) {
            return;
        }
        mandatoryProperties = new ArrayList<String>();
        mandatoryProperties.add(PROP_IDENTIFIER);
        mandatoryProperties.add(PROP_CREATE_TIMESTAMP);
    }

    /**
     * Set the names of any transient properties.
     */
    private static synchronized void setTransientPropertyNames() {
        if (transientProperties != null) {
            return;
        }
        transientProperties = new ArrayList<String>();
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
     * @param propName
     *            The name of the property to check.
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
     * @param propName
     *            The name of the property to check.
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

    /**
     * Get the list of transient properties.
     *
     * @return The list of transient properties.
     */
    protected static List<String> getTransientProperties() {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        return Collections.unmodifiableList(transientProperties);
    }

    /**
     * Re-initialize the property names for all entities.
     */
    public static synchronized void reInitializePropertyNames() {
        setPropertyNames();
    }

    /**
     * Configure the mapping of types to supported properties.
     */
    private static synchronized void setPropertyNames() {
        if (properties == null) {
            properties = new HashMap<String, List<String>>();
        }
        properties.put(Entity.TYPE_NAME, Entity.getPropertyNames(Entity.TYPE_NAME));
        properties.put(Group.TYPE_NAME, Group.getPropertyNames(Group.TYPE_NAME));
        properties.put(Locality.TYPE_NAME, Locality.getPropertyNames(Locality.TYPE_NAME));
        properties.put(Party.TYPE_NAME, Party.getPropertyNames(Party.TYPE_NAME));
        properties.put(RolePlayer.TYPE_NAME, RolePlayer.getPropertyNames(RolePlayer.TYPE_NAME));
        properties.put(PartyRole.TYPE_NAME, PartyRole.getPropertyNames(PartyRole.TYPE_NAME));
        properties.put(OrgContainer.TYPE_NAME, OrgContainer.getPropertyNames(OrgContainer.TYPE_NAME));
        properties.put(Container.TYPE_NAME, Container.getPropertyNames(Container.TYPE_NAME));
        properties.put(LoginAccount.TYPE_NAME, LoginAccount.getPropertyNames(LoginAccount.TYPE_NAME));
        properties.put(GeographicLocation.TYPE_NAME, GeographicLocation.getPropertyNames(GeographicLocation.TYPE_NAME));
        properties.put(Person.TYPE_NAME, Person.getPropertyNames(Person.TYPE_NAME));
        properties.put(Country.TYPE_NAME, Country.getPropertyNames(Country.TYPE_NAME));
        properties.put(PersonAccount.TYPE_NAME, PersonAccount.getPropertyNames(PersonAccount.TYPE_NAME));
    }

    /**
     * Gets a list of all supported properties for this type.
     *
     * @param entityTypeName
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link List}
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (entityTypeName == null) {
            return null;
        }
        if (!entityTypeName.equals(Entity.TYPE_NAME)) {
            return properties.get(entityTypeName);
        }

        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
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
        dataTypeMap.put(PROP_IDENTIFIER, IdentifierType.TYPE_NAME);
        dataTypeMap.put(PROP_VIEW_IDENTIFIERS, ViewIdentifierType.TYPE_NAME);
        dataTypeMap.put(PROP_PARENT, Entity.TYPE_NAME);
        dataTypeMap.put(PROP_CHILDREN, Entity.TYPE_NAME);
        dataTypeMap.put(PROP_GROUPS, Group.TYPE_NAME);
        dataTypeMap.put(PROP_CREATE_TIMESTAMP, "Date");
        dataTypeMap.put(PROP_MODIFY_TIMESTAMP, "Date");
        dataTypeMap.put(PROP_ENTITLEMENT_INFO, EntitlementInfoType.TYPE_NAME);
        dataTypeMap.put(PROP_CHANGE_TYPE, "String");
    }

    /**
     * Gets the Java type of the value of the provided property. For example: String
     *
     * @param propName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link String}
     */
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return (dataTypeMap.get(propName));
        } else {
            return null;
        }
    }

    /**
     * Create the list of super-types for this type.
     */
    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList<String>();
        }
    }

    /**
     * Gets a list of any types which this type is an extension of.
     *
     * @return
     *         returned object is {@link ArrayList}
     */
    public ArrayList<String> getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    /**
     * Returns a true if the provided type is one that this type extends; false, otherwise.
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

    /**
     * Create the set of sub-types for this type.
     */
    private static synchronized void setSubTypes() {
        if (subTypeSet == null) {
            subTypeSet = new HashSet<String>();
        }
        subTypeSet.add(Group.TYPE_NAME);
        subTypeSet.add(Locality.TYPE_NAME);
        subTypeSet.add(Party.TYPE_NAME);
        subTypeSet.add(RolePlayer.TYPE_NAME);
        subTypeSet.add(PartyRole.TYPE_NAME);
        subTypeSet.add(OrgContainer.TYPE_NAME);
        subTypeSet.add(Container.TYPE_NAME);
        subTypeSet.add(LoginAccount.TYPE_NAME);
        subTypeSet.add(GeographicLocation.TYPE_NAME);
        subTypeSet.add(Person.TYPE_NAME);
        subTypeSet.add(Country.TYPE_NAME);
        subTypeSet.add(PersonAccount.TYPE_NAME);
    }

    /**
     * Create the map of types to sub-types.
     */
    private static synchronized void setSubTypeMap() {
        if (subTypeMap == null) {
            subTypeMap = new HashMap<String, Set<String>>();
        }
        subTypeMap.put(Entity.TYPE_NAME, Entity.getSubTypes());
        subTypeMap.put(Group.TYPE_NAME, Group.getSubTypes());
        subTypeMap.put(Locality.TYPE_NAME, Locality.getSubTypes());
        subTypeMap.put(Party.TYPE_NAME, Party.getSubTypes());
        subTypeMap.put(RolePlayer.TYPE_NAME, RolePlayer.getSubTypes());
        subTypeMap.put(PartyRole.TYPE_NAME, PartyRole.getSubTypes());
        subTypeMap.put(OrgContainer.TYPE_NAME, OrgContainer.getSubTypes());
        subTypeMap.put(Container.TYPE_NAME, Container.getSubTypes());
        subTypeMap.put(LoginAccount.TYPE_NAME, LoginAccount.getSubTypes());
        subTypeMap.put(GeographicLocation.TYPE_NAME, GeographicLocation.getSubTypes());
        subTypeMap.put(Person.TYPE_NAME, Person.getSubTypes());
        subTypeMap.put(Country.TYPE_NAME, Country.getSubTypes());
        subTypeMap.put(PersonAccount.TYPE_NAME, PersonAccount.getSubTypes());
    }

    /**
     * Get the sub-types for the specified type.
     *
     * @param entityTypeName
     *            The type name to retrieve sub-types for.
     * @return
     *         The set of sub-entities.
     */
    public static HashSet<String> getSubEntityTypes(String entityTypeName) {
        HashSet<String> hs = ((HashSet<String>) subTypeMap.get(entityTypeName));
        if (hs == null) {
            if (LoginAccount.TYPE_NAME.equals(entityTypeName)) {
                subTypeMap.put(LoginAccount.TYPE_NAME, LoginAccount.getSubTypes());
            }
        }

        return ((HashSet<String>) subTypeMap.get(entityTypeName));
    }

    /**
     * Gets a set of any types which extend this type.
     *
     * @return
     *         returned object is {@link Set}
     */
    public static HashSet<String> getSubTypes() {
        if (subTypeSet == null) {
            setSubTypes();
        }
        return subTypeSet;
    }

    @Override
    public String toString() {
        return WIMTraceHelper.traceJaxb(this);
    }

    /**
     * Returns true if the requested property is unset; false, otherwise.
     *
     * @param propName
     *            Property name to check if unset.
     * @return
     *         returned object is {@link boolean }
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
