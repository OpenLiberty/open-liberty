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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for PropertyControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PropertyControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}Control">
 * &lt;sequence>
 * &lt;element name="properties" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element name="contextProperties" maxOccurs="unbounded" minOccurs="0">
 * &lt;complexType>
 * &lt;simpleContent>
 * &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 * &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/>
 * &lt;/extension>
 * &lt;/simpleContent>
 * &lt;/complexType>
 * &lt;/element>
 * &lt;/sequence>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The PropertyControl object extends the Control object.
 * 
 * <p> PropertyControl is used in the input Root object of the get() API to specify the properties of the entity to be
 * returned.
 * 
 * <ul>
 * <li><b>properties</b>: a list of the properties to return from the entities which match the search criteria.
 * For example, <b>properties</b> may include the 'sn' and 'givenName' attributes of the users who are managers.</li>
 * 
 * <li><b>contextProperties</b>: used for adding names of the contexted properties and the meta data, for e.g.
 * the <b>description</b> property with meta data 'lang=FR'.
 * 
 * </ul>
 * 
 * <p> If the property 'properties' contains a wildcard value '*', all supported properties of the entity will be returned
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PropertyControl", propOrder = {
                                                "properties",
                                                "contextProperties"
})
@XmlSeeAlso({
             SearchControl.class
})
@Trivial
public class PropertyControl
                extends Control
{

    protected List<String> properties;
    protected List<PropertyControl.ContextProperties> contextProperties;
    private static List propertyNames = null;
    private static HashMap dataTypeMap = null;
    private static ArrayList superTypeList = null;
    private static HashSet subTypeList = null;

    static {
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();
    }

    /**
     * Gets the value of the properties property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the properties property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getProperties().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getProperties() {
        if (properties == null) {
            properties = new ArrayList<String>();
        }
        return this.properties;
    }

    public boolean isSetProperties() {
        return ((this.properties != null) && (!this.properties.isEmpty()));
    }

    public void unsetProperties() {
        this.properties = null;
    }

    /**
     * Gets the value of the contextProperties property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the contextProperties property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getContextProperties().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link PropertyControl.ContextProperties }
     * 
     * 
     */
    public List<PropertyControl.ContextProperties> getContextProperties() {
        if (contextProperties == null) {
            contextProperties = new ArrayList<PropertyControl.ContextProperties>();
        }
        return this.contextProperties;
    }

    public boolean isSetContextProperties() {
        return ((this.contextProperties != null) && (!this.contextProperties.isEmpty()));
    }

    public void unsetContextProperties() {
        this.contextProperties = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals("properties")) {
            return getProperties();
        }
        if (propName.equals("contextProperties")) {
            return getContextProperties();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals("properties")) {
            return isSetProperties();
        }
        if (propName.equals("contextProperties")) {
            return isSetContextProperties();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals("properties")) {
            getProperties().add(((String) value));
        }
        if (propName.equals("contextProperties")) {
            getContextProperties().add(((com.ibm.wsspi.security.wim.model.PropertyControl.ContextProperties) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals("properties")) {
            unsetProperties();
        }
        if (propName.equals("contextProperties")) {
            unsetContextProperties();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return "PropertyControl";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("properties");
                names.add("contextProperties");
                names.addAll(Control.getPropertyNames("Control"));
                propertyNames = java.util.Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("properties", "String");
        dataTypeMap.put("contextProperties", "ContextProperties");
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
        superTypeList.add("Control");
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
        subTypeList.add("DescendantControl");
        subTypeList.add("GroupMemberControl");
        subTypeList.add("GroupMembershipControl");
        subTypeList.add("HierarchyControl");
        subTypeList.add("LoginControl");
        subTypeList.add("AncestorControl");
        subTypeList.add("ChangeControl");
        subTypeList.add("SearchControl");
        subTypeList.add("GroupControl");
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

    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     * &lt;simpleContent>
     * &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
     * &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang"/>
     * &lt;/extension>
     * &lt;/simpleContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
                                     "value"
    })
    public static class ContextProperties {

        @XmlValue
        protected String value;
        @XmlAttribute(name = "lang", namespace = "http://www.w3.org/XML/1998/namespace")
        protected String lang;
        private static List propertyNames = null;
        private static HashMap dataTypeMap = null;
        private static ArrayList superTypeList = null;
        private static HashSet subTypeList = null;

        static {
            setDataTypeMap();
            setSuperTypes();
            setSubTypes();
        }

        /**
         * Gets the value of the value property.
         * 
         * @return
         *         possible object is {@link String }
         * 
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets the value of the value property.
         * 
         * @param value
         *            allowed object is {@link String }
         * 
         */
        public void setValue(String value) {
            this.value = value;
        }

        public boolean isSetValue() {
            return (this.value != null);
        }

        /**
         * Gets the value of the lang property.
         * 
         * @return
         *         possible object is {@link String }
         * 
         */
        public String getLang() {
            return lang;
        }

        /**
         * Sets the value of the lang property.
         * 
         * @param value
         *            allowed object is {@link String }
         * 
         */
        public void setLang(String value) {
            this.lang = value;
        }

        public boolean isSetLang() {
            return (this.lang != null);
        }

        public Object get(String propName) {
            if (propName.equals("value")) {
                return getValue();
            }
            if (propName.equals("lang")) {
                return getLang();
            }
            return null;
        }

        public boolean isSet(String propName) {
            if (propName.equals("value")) {
                return isSetValue();
            }
            if (propName.equals("lang")) {
                return isSetLang();
            }
            return false;
        }

        public void set(String propName, Object value) {
            if (propName.equals("value")) {
                setValue(((String) value));
            }
            if (propName.equals("lang")) {
                setLang(((String) value));
            }
        }

        public void unset(String propName) {}

        public String getTypeName() {
            return "ContextProperties";
        }

        public static synchronized List getPropertyNames(String entityTypeName) {
            if (propertyNames != null) {
                return propertyNames;
            } else {
                {
                    List names = new ArrayList();
                    names.add("value");
                    names.add("lang");
                    propertyNames = java.util.Collections.unmodifiableList(names);
                    return propertyNames;
                }
            }
        }

        private static synchronized void setDataTypeMap() {
            if (dataTypeMap == null) {
                dataTypeMap = new HashMap();
            }
            dataTypeMap.put("value", "String");
            dataTypeMap.put("lang", "String");
        }

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

        public ArrayList getSuperTypes() {
            if (superTypeList == null) {
                setSuperTypes();
            }
            return superTypeList;
        }

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
    }
}
