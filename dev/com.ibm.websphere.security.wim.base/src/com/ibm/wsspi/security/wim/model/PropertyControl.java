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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for PropertyControl complex type.
 *
 * <p> The PropertyControl object extends the {@link Control} object.
 *
 * <p> PropertyControl is used in the input {@link Root} object of the get() API to specify the properties of the entity to be
 * returned.
 *
 * <p>Below is a list of supported properties for {@link Locality}.
 *
 * <ul>
 * <li><b>properties</b>: a list of the properties to return from the entities which match the search criteria.
 * For example, <b>properties</b> may include the 'sn' and 'givenName' attributes of the users who are managers. If
 * this contains a wildcard value '*', all supported properties of the entity will be returned.</li>
 * <li><b>contextProperties</b>: used for adding names of the contexted properties and the meta data, for e.g.
 * the <b>description</b> property with meta data 'lang=FR'.
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link Control} and its
 * super-classes are supported.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = PropertyControl.TYPE_NAME, propOrder = {
                                                         "properties",
                                                         "contextProperties"
})
@XmlSeeAlso({
              SearchControl.class
})
public class PropertyControl extends Control {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "PropertyControl";

    /** Property name constant for the <b>properties</b> property. */
    private static final String PROP_PROPERTIES = "properties";

    /** Property name constant for the <b>contextProperties</b> property. */
    private static final String PROP_CONTEXT_PROPERTIES = "contextProperties";

    /**
     * A list of the properties to return from the entities which match the search criteria.
     * For example, <b>properties</b> may include the 'sn' and 'givenName' attributes of the users who are managers.
     */
    @XmlElement(name = PROP_PROPERTIES)
    protected List<String> properties;

    /**
     * Used for adding names of the contexted properties and the meta data, for e.g.
     * the <b>description</b> property with meta data 'lang=FR'.
     */
    @XmlElement(name = PROP_CONTEXT_PROPERTIES)
    protected List<ContextProperties> contextProperties;

    /** The list of properties that comprise this type. */
    private static List<String> propertyNames = null;

    /** A mapping of property names to data types. */
    private static HashMap<String, String> dataTypeMap = null;

    /** A list of super-types of this type. */
    private static ArrayList<String> superTypeList = null;

    /** A set of sub-types of this type. */
    private static HashSet<String> subTypeSet = null;

    static {
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();
    }

    /**
     * Gets the value of the <b>properties</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>properties</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getProperties().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<String> getProperties() {
        if (properties == null) {
            properties = new ArrayList<String>();
        }
        return this.properties;
    }

    /**
     * Returns true if the <b>properties</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetProperties() {
        return ((this.properties != null) && (!this.properties.isEmpty()));
    }

    /**
     * Unset the <b>properties</b> property.
     */
    public void unsetProperties() {
        this.properties = null;
    }

    /**
     * Gets the value of the <b>contextProperties</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>contextProperties</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getContextProperties().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link ContextProperties }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<ContextProperties> getContextProperties() {
        if (contextProperties == null) {
            contextProperties = new ArrayList<ContextProperties>();
        }
        return this.contextProperties;
    }

    /**
     * Returns true if the <b>contextProperties</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetContextProperties() {
        return ((this.contextProperties != null) && (!this.contextProperties.isEmpty()));
    }

    /**
     * Unset the <b>contextProperties</b> property.
     */
    public void unsetContextProperties() {
        this.contextProperties = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_PROPERTIES)) {
            return getProperties();
        }
        if (propName.equals(PROP_CONTEXT_PROPERTIES)) {
            return getContextProperties();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_PROPERTIES)) {
            return isSetProperties();
        }
        if (propName.equals(PROP_CONTEXT_PROPERTIES)) {
            return isSetContextProperties();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_PROPERTIES)) {
            getProperties().add(((String) value));
        }
        if (propName.equals(PROP_CONTEXT_PROPERTIES)) {
            getContextProperties().add(((ContextProperties) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_PROPERTIES)) {
            unsetProperties();
        }
        if (propName.equals(PROP_CONTEXT_PROPERTIES)) {
            unsetContextProperties();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
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
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.add(PROP_PROPERTIES);
            names.add(PROP_CONTEXT_PROPERTIES);
            names.addAll(Control.getPropertyNames(Control.TYPE_NAME));
            propertyNames = java.util.Collections.unmodifiableList(names);
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
        dataTypeMap.put(PROP_PROPERTIES, "String");
        dataTypeMap.put(PROP_CONTEXT_PROPERTIES, ContextProperties.TYPE_NAME);
    }

    @Override
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return (dataTypeMap.get(propName));
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
        superTypeList.add(Control.TYPE_NAME);
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
        subTypeSet.add(DescendantControl.TYPE_NAME);
        subTypeSet.add(GroupMemberControl.TYPE_NAME);
        subTypeSet.add(GroupMemberControl.TYPE_NAME);
        subTypeSet.add(HierarchyControl.TYPE_NAME);
        subTypeSet.add(LoginControl.TYPE_NAME);
        subTypeSet.add(AncestorControl.TYPE_NAME);
        subTypeSet.add(ChangeControl.TYPE_NAME);
        subTypeSet.add(SearchControl.TYPE_NAME);
        subTypeSet.add(GroupControl.TYPE_NAME);
    }

    /**
     * Get the set of sub-types for this type.
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
     * <p>Java class for anonymous complex type.
     *
     * <p>Below is a list of supported properties for {@link ContextProperties}.
     *
     * <ul>
     * <li><b>lang</b>: the value</li>
     * <li><b>value</b>: the language</li>
     * </ul>
     *
     * <p>In addition to the properties in the list above, all properties from the super-class {@link Control} and its
     * super-classes are supported.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
                                      "value"
    })
    public static class ContextProperties {

        /** The type name for this data type. */
        public static final String TYPE_NAME = "ContextProperties";

        /** Property name constant for the <b>lang</b> property. */
        private static final String PROP_LANG = "lang";

        /** Property name constant for the <b>value</b> property. */
        private static final String PROP_VALUE = "value";

        /** The value. */
        @XmlValue
        protected String value;

        /** The language. */
        @XmlAttribute(name = PROP_LANG, namespace = "http://www.w3.org/XML/1998/namespace")
        protected String lang;

        /** The list of properties that comprise this type. */
        private static List<String> propertyNames = null;

        /** A mapping of property names to data types. */
        private static HashMap<String, String> dataTypeMap = null;

        /** A list of super-types of this type. */
        private static ArrayList<String> superTypeList = null;

        /** A set of sub-types of this type. */
        private static HashSet<String> subTypeSet = null;

        static {
            setDataTypeMap();
            setSuperTypes();
            setSubTypes();
        }

        /**
         * Gets the value of the <b>value</b> property.
         *
         * @return
         *         possible object is {@link String }
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets the value of the <b>value</b> property.
         *
         * @param value
         *            allowed object is {@link String }
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * Returns true if the <b>value</b> property is set; false, otherwise.
         *
         * @return
         *         returned object is {@link boolean }
         */
        public boolean isSetValue() {
            return (this.value != null);
        }

        /**
         * Gets the value of the <b>lang</b> property.
         *
         * @return
         *         possible object is {@link String }
         */
        public String getLang() {
            return lang;
        }

        /**
         * Sets the value of the <b>lang</b> property.
         *
         * @param value
         *            allowed object is {@link String }
         */
        public void setLang(String value) {
            this.lang = value;
        }

        /**
         * Returns true if the <b>lang</b> property is set; false, otherwise.
         *
         * @return
         *         returned object is {@link boolean }
         */
        public boolean isSetLang() {
            return (this.lang != null);
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
            if (propName.equals(PROP_VALUE)) {
                return getValue();
            }
            if (propName.equals(PROP_LANG)) {
                return getLang();
            }
            return null;
        }

        /**
         * Returns true if the requested property is set; false, otherwise.
         *
         * @param propName
         *            The property name to check if set.
         * @return
         *         returned object is {@link boolean }
         */
        public boolean isSet(String propName) {
            if (propName.equals(PROP_VALUE)) {
                return isSetValue();
            }
            if (propName.equals(PROP_LANG)) {
                return isSetLang();
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
            if (propName.equals(PROP_VALUE)) {
                setValue(((String) value));
            }
            if (propName.equals(PROP_LANG)) {
                setLang(((String) value));
            }
        }

        /**
         * Unset the specified property name.
         *
         * @param propName The property name to unset.
         */
        public void unset(String propName) {}

        /**
         * Gets the name of this model object.
         *
         * @return
         *         returned object is {@link String}
         */
        public String getTypeName() {
            return TYPE_NAME;
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
            if (propertyNames == null) {
                List<String> names = new ArrayList<String>();
                names.add(PROP_VALUE);
                names.add(PROP_LANG);
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
            dataTypeMap.put(PROP_VALUE, "String");
            dataTypeMap.put(PROP_LANG, "String");
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
         * Get the list of super-types for this type.
         *
         * @return The list of super-types.
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
        }

        /**
         * Get the set of sub-types for this type.
         *
         * @return The set of sub-types.
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
    }
}
