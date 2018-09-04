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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.wim.ras.WIMMessageKey;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for Group complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Group">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}Party">
 * &lt;sequence>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}cn"/>
 * &lt;element name="members" type="{http://www.ibm.com/websphere/wim}Entity" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}displayName" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}description" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}businessCategory" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}seeAlso" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * <p> The Group object extends the Party object and is used to define the properties of a group.
 *
 * <p> The Group object has several properties: <b>cn</b>, <b>members</b>, <b>displayName</b>, <b>description</b>, and <b>businessCategory</b>.
 *
 * <ul>
 * <li><b>cn</b>: represents the common name of the group.</li>
 * <li><b>members</b>: references 0 to n Entity objects which are associated with this group.
 * A member may be a reference to a Person or another Group entity.</li>
 * <li><b>displayName</b>: references the full name associated with the group.</li>
 * <li><b>description</b>: provides a means to describe the group.</li>
 * </ul>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Group", propOrder = {
                                       "cn",
                                       "members",
                                       "displayName",
                                       "description",
                                       "businessCategory",
                                       "seeAlso"
})
public class Group extends Party {
    private static final TraceComponent tc = Tr.register(Group.class);

    private static final String PROP_CN = "cn";
    private static final String PROP_MEMBERS = "members";
    private static final String PROP_DISPLAY_NAME = "displayName";
    private static final String PROP_DESCRIPTION = "description";
    private static final String PROP_BUSINESS_CATEGORY = "businessCategory";
    private static final String PROP_SEE_ALSO = "seeAlso";

    @XmlElement(required = true)
    protected String cn;
    protected List<com.ibm.wsspi.security.wim.model.Entity> members;
    protected List<String> displayName;
    protected List<String> description;
    protected List<String> businessCategory;
    protected List<String> seeAlso;

    private static List mandatoryProperties = null;
    private static List transientProperties = null;
    private static List propertyNames = null;
    private static HashMap dataTypeMap = null;
    private static ArrayList superTypeList = null;
    private static HashSet subTypeList = null;
    protected Map<String, Object> extendedPropertiesValue = new HashMap<String, Object>(); // TODO SHOULD THIS BE PRIVATE?
    private static Map<String, String> extendedPropertiesDataType = new HashMap<String, String>();
    private static Map<String, Object> extendedPropertiesDefaultValue = new HashMap<String, Object>();
    private static Set<String> extendedMultiValuedProperties = new HashSet<String>();

    /** The set of multi-valued properties for this entity type. */
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
     *
     */
    public String getCn() {
        return cn;
    }

    /**
     * Sets the value of the <b>cn</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setCn(String value) {
        this.cn = value;
    }

    /**
     * Returns true if the <b>cn</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
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
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the members property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getMembers().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.Entity }
     *
     *
     */
    public List<com.ibm.wsspi.security.wim.model.Entity> getMembers() {
        if (members == null) {
            members = new ArrayList<com.ibm.wsspi.security.wim.model.Entity>();
        }
        return this.members;
    }

    /**
     * Returns true if the <b>members</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */

    public boolean isSetMembers() {
        return ((this.members != null) && (!this.members.isEmpty()));
    }

    /**
     * Resets the value of the <b>members</b> property to null
     *
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

    /**
     * Returns true if the <b>displayName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetDisplayName() {
        return ((this.displayName != null) && (!this.displayName.isEmpty()));
    }

    /**
     * Resets the value of the <b>displayName</b> property to null
     *
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

    /**
     * Returns true if the <b>description</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetDescription() {
        return ((this.description != null) && (!this.description.isEmpty()));
    }

    /**
     * Resets the value of the <b>description</b> property to null
     *
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

    /**
     * Returns true if the <b>businessCategory</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetBusinessCategory() {
        return ((this.businessCategory != null) && (!this.businessCategory.isEmpty()));
    }

    /**
     * Resets the value of the <b>businessCategory</b> property to null
     *
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

    /**
     * Returns true if the <b>seeAlso</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetSeeAlso() {
        return ((this.seeAlso != null) && (!this.seeAlso.isEmpty()));
    }

    /**
     * Resets the value of the <b>seeAlso</b> property to null
     *
     */
    public void unsetSeeAlso() {
        this.seeAlso = null;
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

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
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

    /**
     * Sets the value of the provided property to the provided value.
     *
     * @param propName
     *            allowed object is {@link String}
     * @param value
     *            allowed object is {@link Object}
     *
     */
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

    /**
     * Sets the value of provided property to null.
     *
     * @param propName
     *            allowed object is {@link String}
     *
     */
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
        return "Group";
    }

    private static synchronized void setMandatoryPropertyNames() {
        if (mandatoryProperties != null) {
            return;
        }
        mandatoryProperties = new ArrayList();
        mandatoryProperties.add(PROP_CN);
    }

    private static synchronized void setTransientPropertyNames() {
        if (transientProperties != null) {
            return;
        }
        transientProperties = new ArrayList();
        transientProperties.add(PROP_MEMBERS);
        transientProperties.addAll(Party.getTransientProperties());
    }

    /**
     * Returns true if the provided property name is a mandatory property; false, otherwise.
     *
     * @param propName
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link boolean}
     *
     */
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

    /**
     * Returns true if the provided property name is a persistent property; false, otherwise.
     *
     * @param propName
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link boolean}
     *
     */
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

    /**
     * Gets a list of all supported properties for this model object, <b>Group</b>
     *
     * @param entityTypeName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link List}
     */
    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add(PROP_CN);
                names.add(PROP_MEMBERS);
                names.add(PROP_DISPLAY_NAME);
                names.add(PROP_DESCRIPTION);
                names.add(PROP_BUSINESS_CATEGORY);
                names.add(PROP_SEE_ALSO);
                if (extendedPropertiesDataType != null && extendedPropertiesDataType.keySet().size() > 0)
                    names.addAll(extendedPropertiesDataType.keySet());
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
        dataTypeMap.put(PROP_CN, "String");
        dataTypeMap.put(PROP_MEMBERS, "Entity");
        dataTypeMap.put(PROP_DISPLAY_NAME, "String");
        dataTypeMap.put(PROP_DESCRIPTION, "String");
        dataTypeMap.put(PROP_BUSINESS_CATEGORY, "String");
        dataTypeMap.put(PROP_SEE_ALSO, "String");
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
    @Override
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return ((String) dataTypeMap.get(propName));
        } else if (extendedPropertiesDataType.containsKey(propName)) {
            return extendedPropertiesDataType.get(propName);
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

    /**
     * Gets a list of any model objects which this model object, <b>Group</b>, is
     * an extension of.
     *
     * @return
     *         returned object is {@link ArrayList}
     */
    @Override
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
    @Override
    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    private static synchronized void setSubTypes() {
        if (subTypeList == null) {
            subTypeList = new HashSet();
        }
    }

    /**
     * Gets a set of any model objects which extend this model object, <b>Group</b>
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

    /**
     * Set an extended property's value.
     *
     * @param property The property to set.
     * @param value The value to set.
     * @throws ClassCastException If the value was not of the correct data type.
     */
    private void setExtendedProperty(String property, Object value) {
        String dataType = extendedPropertiesDataType.get(property);
        String valueClass = value.getClass().getSimpleName();

        if (dataType.equals(valueClass) && !extendedMultiValuedProperties.contains(property)) {
            extendedPropertiesValue.put(property, value);
        } else if (dataType.equals(valueClass) && extendedMultiValuedProperties.contains(property)) {
            if (value instanceof List) {
                extendedPropertiesValue.put(property, value);
            } else {
                List<Object> values = (List<Object>) extendedPropertiesValue.get(property);
                if (values == null) {
                    values = new ArrayList<Object>();
                    extendedPropertiesValue.put(property, values);
                }
                values.add(value);
            }
        } else {
            String type = value == null ? "null" : value.getClass().getName();
            String msg = "Could not set extended property for Group property '" + property + "'. " + type + " is incompatible with " + dataType;
            throw new ClassCastException(msg);
        }
    }

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
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
            Tr.warning(tc, WIMMessageKey.DUPLICATE_PROPERTY_EXTENDED, new Object[] { propName, "Group" });
            return;
        }

        if (getPropertyNames("Group").contains(propName)) {
            Tr.warning(tc, WIMMessageKey.DUPLICATE_PROPERTY_ENTITY, new Object[] { propName, "Group" });
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
