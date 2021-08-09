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
 * <p>Java class for CheckPointType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CheckPointType">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="repositoryId" type="{http://www.w3.org/2001/XMLSchema}token"/>
 * &lt;element name="repositoryCheckPoint" type="{http://www.w3.org/2001/XMLSchema}token" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The CheckPointType object defines two properties: <b>repositoryId</b> and <b>repositoryCheckPoint</b>
 * 
 * <ul>
 * <li><b>repositoryId</b>: defines the unique identifier for a given federated repository.</li>
 * <li><b>repositoryCheckPoint</b>: defines the repository checkpoint identifier.</li>
 * </ul>
 * 
 **/
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CheckPointType", propOrder = {
                                               "repositoryId",
                                               "repositoryCheckPoint"
})
@Trivial
public class CheckPointType {

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String repositoryId;
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String repositoryCheckPoint;
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
     * Gets the value of the <b>repositoryId</b> property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getRepositoryId() {
        return repositoryId;
    }

    /**
     * Sets the value of the <b>repositoryId</b> property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setRepositoryId(String value) {
        this.repositoryId = value;
    }

    /**
     * Returns true if the <b>repositoryId</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */
    public boolean isSetRepositoryId() {
        return (this.repositoryId != null);
    }

    /**
     * Gets the value of the <b>repositoryCheckPoint</b> property.
     * 
     * @return
     *         possible object is {@link String }
     * 
     */
    public String getRepositoryCheckPoint() {
        return repositoryCheckPoint;
    }

    /**
     * Sets the value of the <b>repositoryCheckPoint</b> property.
     * 
     * @param value
     *            allowed object is {@link String }
     * 
     */
    public void setRepositoryCheckPoint(String value) {
        this.repositoryCheckPoint = value;
    }

    /**
     * Returns true if the <b>repositoryCheckPoint</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */
    public boolean isSetRepositoryCheckPoint() {
        return (this.repositoryCheckPoint != null);
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
        if (propName.equals("repositoryId")) {
            return getRepositoryId();
        }
        if (propName.equals("repositoryCheckPoint")) {
            return getRepositoryCheckPoint();
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
        if (propName.equals("repositoryId")) {
            return isSetRepositoryId();
        }
        if (propName.equals("repositoryCheckPoint")) {
            return isSetRepositoryCheckPoint();
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
        if (propName.equals("repositoryId")) {
            setRepositoryId(((String) value));
        }
        if (propName.equals("repositoryCheckPoint")) {
            setRepositoryCheckPoint(((String) value));
        }
    }

    /**
     * Sets the value of provided property to null.
     * 
     * @param propName
     *            allowed object is {@link String}
     * 
     */
    public void unset(String propName) {}

    /**
     * Gets the name of this model object, <b>CheckPointType</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    public String getTypeName() {
        return "CheckPointType";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>CheckPointType</b>
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
                names.add("repositoryId");
                names.add("repositoryCheckPoint");
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("repositoryId", "String");
        dataTypeMap.put("repositoryCheckPoint", "String");
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
     * Gets a list of any model objects which this model object, <b>CheckPointType</b>, is
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
    }

    /**
     * Gets a set of any model objects which extend this model object, <b>CheckPointType</b>
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
     * Returns this model object, <b>CheckPointType</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
