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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for SearchResponseControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SearchResponseControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}Control">
 * &lt;attribute name="hasMoreResults" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The SearchResponseControl object extends the Control object, and contains the <b>hasMoreResults</b>property.
 * 
 * <p> A SearchResponseControl object will only be returned from a search API call if the <b>countLimit</b> property in the
 * SearchControl object is set to a value greater than 0.
 * 
 * <ul>
 * <li><b>hasMoreResults</b>: will be set to true if the actual number of results from the search is greater
 * than the value of the <b>countLimit</b> property in the SearchControl object.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SearchResponseControl")
@XmlSeeAlso({
             ChangeResponseControl.class
})
@Trivial
public class SearchResponseControl
                extends Control
{

    @XmlAttribute(name = "hasMoreResults")
    protected Boolean hasMoreResults;
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
     * Gets the value of the hasMoreResults property.
     * 
     * @return
     *         possible object is {@link Boolean }
     * 
     */
    public boolean isHasMoreResults() {
        return hasMoreResults;
    }

    /**
     * Sets the value of the hasMoreResults property.
     * 
     * @param value
     *            allowed object is {@link Boolean }
     * 
     */
    public void setHasMoreResults(boolean value) {
        this.hasMoreResults = value;
    }

    public boolean isSetHasMoreResults() {
        return (this.hasMoreResults != null);
    }

    public void unsetHasMoreResults() {
        this.hasMoreResults = null;
    }

    @Override
    public Object get(String propName) {
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals("hasMoreResults")) {
            return isSetHasMoreResults();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals("hasMoreResults")) {
            setHasMoreResults(((Boolean) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals("hasMoreResults")) {
            unsetHasMoreResults();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return "SearchResponseControl";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("hasMoreResults");
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
        dataTypeMap.put("hasMoreResults", "Boolean");
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
        subTypeList.add("ChangeResponseControl");
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
