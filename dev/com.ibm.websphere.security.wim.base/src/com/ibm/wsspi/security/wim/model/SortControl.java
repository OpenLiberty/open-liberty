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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for SortControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SortControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}Control">
 * &lt;sequence>
 * &lt;element name="sortKeys" type="{http://www.ibm.com/websphere/wim}SortKeyType" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element name="locale" type="{http://www.w3.org/2001/XMLSchema}language"/>
 * &lt;/sequence>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The SortControl object extends the Control object and defines two properties:
 * 
 * <ul>
 * <li><b>sortKeys</b>: contains a list of attributes will be used to do the sorting.
 * For each attribute a sorting order can be specified by the <b>ascendingOrder</b> property in
 * the SortKey object. For example, a caller wants to sort the search results by 'sn' in
 * descending order. The caller needs to set the <b>ascendingOrder</b> property to false.
 * The properties included in the SortControl object must be listed in the property list of
 * the SearchControl.</li>
 * 
 * <li><b>locale</b>: indicates which language will be used during the sorting operation.</li>
 * </ul>
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SortControl", propOrder = {
                                            "sortKeys",
                                            "locale"
})
@Trivial
public class SortControl
                extends Control
{

    protected List<com.ibm.wsspi.security.wim.model.SortKeyType> sortKeys;
    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "language")
    protected String locale;
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
     * Gets the value of the sortKeys property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sortKeys property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getSortKeys().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.SortKeyType }
     * 
     * 
     */
    public List<com.ibm.wsspi.security.wim.model.SortKeyType> getSortKeys() {
        if (sortKeys == null) {
            sortKeys = new ArrayList<com.ibm.wsspi.security.wim.model.SortKeyType>();
        }
        return this.sortKeys;
    }

    public boolean isSetSortKeys() {
        return ((this.sortKeys != null) && (!this.sortKeys.isEmpty()));
    }

    public void unsetSortKeys() {
        this.sortKeys = null;
    }

    /**
     * Gets the value of the locale property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getLocale() {
        return locale;
    }

    /**
     * Sets the value of the locale property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setLocale(String value) {
        this.locale = value;
    }

    public boolean isSetLocale() {
        return (this.locale != null);
    }

    @Override
    public Object get(String propName) {
        if (propName.equals("sortKeys")) {
            return getSortKeys();
        }
        if (propName.equals("locale")) {
            return getLocale();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals("sortKeys")) {
            return isSetSortKeys();
        }
        if (propName.equals("locale")) {
            return isSetLocale();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals("sortKeys")) {
            getSortKeys().add(((com.ibm.wsspi.security.wim.model.SortKeyType) value));
        }
        if (propName.equals("locale")) {
            setLocale(((String) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals("sortKeys")) {
            unsetSortKeys();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return "SortControl";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("sortKeys");
                names.add("locale");
                names.addAll(Control.getPropertyNames("Control"));
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("sortKeys", "SortKeyType");
        dataTypeMap.put("locale", "String");
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
