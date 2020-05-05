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
 * <p>Java class for Group complex type.
 *
 * <p>The Group object extends the {@link Party} object and is used to define the properties of a group.
 *
 * <p>Below is a list of supported properties for {@link Group}.
 *
 * <ul>
 * <li><b>cn</b>: represents the common name of the Group.</li>
 * <li><b>members</b>: references 0 to n Entity objects which are associated with this Group.
 * A member may be a reference to a Person or another Group entity.</li>
 * <li><b>displayName</b>: references the full name associated with the Group.</li>
 * <li><b>description</b>: provides a means to describe the Group.</li>
 * <li><b>businessCategory</b>: the business category for the Group.</li>
 * <li><b>seeAlso</b>: contains distinguished names of objects that are related to this Group.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link Party} and its
 * super-classes are supported.
 *
 * <p>The {@link Group} schema can be extended by including extended properties in the server.xml configuration. For example,
 * the following server.xml configuration would create the extended property "myProperty" of type String in {@link Group}.
 *
 * <code>
 *
 * <pre>
 * &lt;federatedRepository&gt;
 *     &lt;extendedProperty name="myProperty" dataType="String" entityType="Group" multiValued="false" /&gt;
 * &lt;/federatedRepository&gt;
 * </pre>
 *
 * </code>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Group.TYPE_NAME, propOrder = {
                                               "cn",
                                               "members",
                                               "displayName",
                                               "description",
                                               "businessCategory",
                                               "seeAlso",
                                               "extendedProperties"
})
public class Group extends Party {

    private static final TraceComponent tc = Tr.register(Group.class);

    /** The type name for this data type. */
    public static final String TYPE_NAME = "Group";

    /** Property name constant for the <b>cn</b> property. */
    private static final String PROP_CN = "cn";

    /** Property name constant for the <b>members</b> property. */
    private static final String PROP_MEMBERS = "members";

    /** Property name constant for the <b>displayName</b> property. */
    private static final String PROP_DISPLAY_NAME = "displayName";

    /** Property name constant for the <b>description</b> property. */
    private static final String PROP_DESCRIPTION = "description";

    /** Property name constant for the <b>businessCategory</b> property. */
    private static final String PROP_BUSINESS_CATEGORY = "businessCategory";

    /** Property name constant for the <b>seeAlso</b> property. */
    private static final String PROP_SEE_ALSO = "seeAlso";

    /** The common name of the group. */
    @XmlElement(name = PROP_CN, required = true)
    protected String cn;

    /**
     * References 0 to n Entity objects which are associated with this group.
     * A member may be a reference to a Person or another Group entity.
     */
    @XmlElement(name = PROP_MEMBERS)
    protected List<Entity> members;

    /**
     * References the full name associated with the group.
     */
    @XmlElement(name = PROP_DISPLAY_NAME)
    protected List<String> displayName;

    /**
     * A means to describe the group.
     */
    @XmlElement(name = PROP_DESCRIPTION)
    protected List<String> description;

    /**
     * The business category for the group.
     */
    @XmlElement(name = PROP_BUSINESS_CATEGORY)
    protected List<String> businessCategory;

    /**
     * Distinguished names of objects that are related to this group.
     */
    @XmlElement(name = PROP_SEE_ALSO)
    protected List<String> seeAlso;

    /**
     * Properties that extend the group schema.
     */
    @XmlElement(name = "extendedProperties")
    @XmlJavaTypeAdapter(ExtendedPropertyXmlAdapter.class)
    protected Map<String, Object> extendedProperties = new HashMap<String, Object>();

    /** List of mandatory properties. */
    private static List<String> mandatoryProperties = null;

    /** List of transient properties. */
    private static List<String> transientProperties = null;

    /** The list of properties that comprise this type. */
    private static List<String> propertyNames = null;

    /** A mapping of property names to data types. */
    private static HashMap<String, String> dataTypeMap = null;

    /** A list of super-types of this type. */
    private static ArrayList<String> superTypeList = null;

    /** A set of sub-types of this type. */
    private static HashSet<String> subTypeSet = null;

    /** Mapping of property names to data type of any extended properties. */
    private static Map<String, String> extendedPropertiesDataType = new HashMap<String, String>();

    /** Mapping of property names to default values of any extended properties. */
    private static Map<String, Object> extendedPropertiesDefaultValue = new HashMap<String, Object>();

    /** Set of property names of any extended properties that are multi-valued. */
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
        MULTI_VALUED_PROPERTIES.add(PROP_MEMBERS);
        MULTI_VALUED_PROPERTIES.add(PROP_DISPLAY_NAME);
        MULTI_VALUED_PROPERTIES.add(PROP_DESCRIPTION);
        MULTI_VALUED_PROPERTIES.add(PROP_BUSINESS_CATEGORY);
        MULTI_VALUED_PROPERTIES.add(PROP_SEE_ALSO);
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
     * Returns true if the <b>cn</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetCn() {
        return (this.cn != null);
    }

    /**
     * Gets the value of the <b>members</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the members property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getMembers().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link Entity }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<Entity> getMembers() {
        if (members == null) {
            members = new ArrayList<Entity>();
        }
        return this.members;
    }

    /**
     * Returns true if the <b>members</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetMembers() {
        return ((this.members != null) && (!this.members.isEmpty()));
    }

    /**
     * Resets the value of the <b>members</b> property to null
     */
    public void unsetMembers() {
        this.members = null;
    }

    /**
     * Gets the value of the <b>displayName</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the displayName property.
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
     * Returns true if the <b>displayName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetDisplayName() {
        return ((this.displayName != null) && (!this.displayName.isEmpty()));
    }

    /**
     * Resets the value of the <b>displayName</b> property to null
     */
    public void unsetDisplayName() {
        this.displayName = null;
    }

    /**
     * Gets the value of the <b>description</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the description property.
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
     * Returns true if the <b>description</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetDescription() {
        return ((this.description != null) && (!this.description.isEmpty()));
    }

    /**
     * Resets the value of the <b>description</b> property to null
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
     * Returns true if the <b>businessCategory</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetBusinessCategory() {
        return ((this.businessCategory != null) && (!this.businessCategory.isEmpty()));
    }

    /**
     * Resets the value of the <b>businessCategory</b> property to null
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
     * This is why there is not a <CODE>set</CODE> method for the seeAlso property.
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
     * Returns true if the <b>seeAlso</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetSeeAlso() {
        return ((this.seeAlso != null) && (!this.seeAlso.isEmpty()));
    }

    /**
     * Resets the value of the <b>seeAlso</b> property to null
     */
    public void unsetSeeAlso() {
        this.seeAlso = null;
    }

    @Override
    public Object get(String propName) {

        /*
         * Require a property name.
         */
        if (propName == null || propName.trim().isEmpty()) {
            return null;
        }

        if (propName.equals(PROP_CN)) {
            return getCn();
        }
        if (propName.equals(PROP_MEMBERS)) {
            return getMembers();
        }
        if (propName.equals(PROP_DISPLAY_NAME)) {
            return getDisplayName();
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

        if (propName.equals(PROP_CN)) {
            return isSetCn();
        }
        if (propName.equals(PROP_MEMBERS)) {
            return isSetMembers();
        }
        if (propName.equals(PROP_DISPLAY_NAME)) {
            return isSetDisplayName();
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

        if (extendedPropertiesDataType.containsKey(propName))
            return isSetExtendedProperty(propName);

        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_CN)) {
            setCn(((String) value));
        }
        if (propName.equals(PROP_MEMBERS)) {
            getMembers().add(((com.ibm.wsspi.security.wim.model.Entity) value));
        }
        if (propName.equals(PROP_DISPLAY_NAME)) {
            getDisplayName().add(((String) value));
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

        if (propName.equals(PROP_MEMBERS)) {
            unsetMembers();
        }
        if (propName.equals(PROP_DISPLAY_NAME)) {
            unsetDisplayName();
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

        if (extendedPropertiesDataType.containsKey(propName))
            unSetExtendedProperty(propName);

        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Set the list of mandatory properties for this entity.
     */
    private static synchronized void setMandatoryPropertyNames() {
        if (mandatoryProperties != null) {
            return;
        }
        mandatoryProperties = new ArrayList<String>();
        mandatoryProperties.add(PROP_CN);
    }

    /**
     * Set the list of transient properties for this entity.
     */
    private static synchronized void setTransientPropertyNames() {
        if (transientProperties != null) {
            return;
        }
        transientProperties = new ArrayList<String>();
        transientProperties.add(PROP_MEMBERS);
        transientProperties.addAll(Party.getTransientProperties());
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
     * Get the list transient properties.
     *
     * @return The list transient properties
     */
    protected static List<String> getTransientProperties() {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        return Collections.unmodifiableList(transientProperties);
    }

    /**
     * Reinitialize the property names.
     */
    public static synchronized void reInitializePropertyNames() {
        propertyNames = null;
        Entity.reInitializePropertyNames();
    }

    /**
     * Gets a list of all supported properties for this type.
     *
     * @param entityTypeName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link List}
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.add(PROP_CN);
            names.add(PROP_MEMBERS);
            names.add(PROP_DISPLAY_NAME);
            names.add(PROP_DESCRIPTION);
            names.add(PROP_BUSINESS_CATEGORY);
            names.add(PROP_SEE_ALSO);

            if (extendedPropertiesDataType != null && extendedPropertiesDataType.keySet().size() > 0) {
                names.addAll(extendedPropertiesDataType.keySet());
            }

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
        dataTypeMap.put(PROP_CN, "String");
        dataTypeMap.put(PROP_MEMBERS, Entity.TYPE_NAME);
        dataTypeMap.put(PROP_DISPLAY_NAME, "String");
        dataTypeMap.put(PROP_DESCRIPTION, "String");
        dataTypeMap.put(PROP_BUSINESS_CATEGORY, "String");
        dataTypeMap.put(PROP_SEE_ALSO, "String");
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
     * Gets a set of any types which extend this type.
     *
     * @return
     *         returned object is {@link HashSet}
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
     * @param propName The name of the property to get the value for.
     * @return The value if set, the default value if a value is not set and the extended property has a default value, otherwise null.
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
     * Is the extended property set?
     *
     * @param property The property to check if is set.
     * @return True if the property is set; otherwise, false.
     */
    private boolean isSetExtendedProperty(String property) {
        return extendedProperties.containsKey(property) || extendedPropertiesDefaultValue.containsKey(property);
    }

    /**
     * Unset the extended property.
     *
     * @param property The property to unset.
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
            String msg = "Could not set extended property for Group property '" + property + "'. " + type + " is incompatible with " + dataType;
            throw new ClassCastException(msg);
        }
    }

    /**
     * Allows for an extended property, or a property not pre-defined as part of this Group entity type, to be
     * added to the Group entity
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

        if (extendedPropertiesDataType.containsKey(propName)) {
            Tr.warning(tc, WIMMessageKey.DUPLICATE_PROPERTY_EXTENDED, new Object[] { propName, TYPE_NAME });
            return;
        }

        if (getPropertyNames(TYPE_NAME).contains(propName)) {
            Tr.warning(tc, WIMMessageKey.DUPLICATE_PROPERTY_ENTITY, new Object[] { propName, TYPE_NAME });
            return;
        }

        extendedPropertiesDataType.put(propName, dataType);
        if (defaultValue != null)
            extendedPropertiesDefaultValue.put(propName, defaultValue);
        if (multiValued)
            extendedMultiValuedProperties.add(propName);
    }

    /**
     * Removes all extended properties defined in this Group entity
     */
    public static void clearExtendedProperties() {
        extendedPropertiesDataType.clear();
        extendedPropertiesDefaultValue.clear();
        extendedMultiValuedProperties.clear();
        reInitializePropertyNames();
    }

    /**
     * Returns a list of extended property names added to this Group entity
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
