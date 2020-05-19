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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for ChangeControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ChangeControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}SearchControl">
 * &lt;sequence>
 * &lt;element name="checkPoint" type="{http://www.ibm.com/websphere/wim}CheckPointType" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element name="changeTypes" type="{http://www.w3.org/2001/XMLSchema}token" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The ChangeControl object extends the SearchControl object and defines two properties: <b>checkPoint</b> and <b>changeTypes</b>.
 * 
 * <p> The ChangeControl data object provides a client application with the ability to query federated repositories for
 * changed entities, such as new, modified, and deleted entities, from a specified checkpoint onwards.
 * 
 * <p> A client application can use the ChangeControl data object to synchronize its internal cache or other repositories,
 * with the user registry, and provide a secure environment using the latest information in its cache or repositories.
 * 
 * * <p> There could be one or more instances of <b>checkPoint</b> in a ChangeControl data object, depending on the number of
 * repositories involved in a search.
 * 
 * <ul>
 * <li><b>checkPoint</b>: defines the checkpoint for repositories configured in VMM. It is a list that contains
 * the repositoryId and the repositoryCheckPoint.</li>
 * <li><b>changeTypes</b>: represents the type of changes to return. It is a list of valid change types:</li>
 * <ul>
 * <li><b>add</b>: for CHANGETYPE_ADD</li>
 * <li><b>delete</b>: for CHANGETYPE_DELETE</li>
 * <li><b>modify</b>: for CHANGETYPE_MODIFY</li>
 * <li><b>rename</b>: for CHANGETYPE_RENAME</li>
 * <li><b>*</b>: for CHANGETYPE_ALL</li>
 * </ul>
 * </ul>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChangeControl", propOrder = {
                                              "checkPoint",
                                              "changeTypes"
})
@Trivial
public class ChangeControl
                extends SearchControl
{

    protected List<com.ibm.wsspi.security.wim.model.CheckPointType> checkPoint;
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected List<String> changeTypes;
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
     * Gets the value of the checkPoint property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the checkPoint property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getCheckPoint().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.CheckPointType }
     * 
     * 
     */
    public List<com.ibm.wsspi.security.wim.model.CheckPointType> getCheckPoint() {
        if (checkPoint == null) {
            checkPoint = new ArrayList<com.ibm.wsspi.security.wim.model.CheckPointType>();
        }
        return this.checkPoint;
    }

    /**
     * Returns true if the <b>checkPoint</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */
    public boolean isSetCheckPoint() {
        return ((this.checkPoint != null) && (!this.checkPoint.isEmpty()));
    }

    /**
     * Resets the <b>checkPoint</b> property to null.
     * 
     */
    public void unsetCheckPoint() {
        this.checkPoint = null;
    }

    /**
     * Gets the value of the changeTypes property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the changeTypes property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getChangeTypes().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getChangeTypes() {
        if (changeTypes == null) {
            changeTypes = new ArrayList<String>();
        }
        return this.changeTypes;
    }

    /**
     * Returns true if the <b>changeTypes</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */
    public boolean isSetChangeTypes() {
        return ((this.changeTypes != null) && (!this.changeTypes.isEmpty()));
    }

    /**
     * Resets the <b>changeTypes</b> property to null.
     * 
     */
    public void unsetChangeTypes() {
        this.changeTypes = null;
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
        if (propName.equals("checkPoint")) {
            return getCheckPoint();
        }
        if (propName.equals("changeTypes")) {
            return getChangeTypes();
        }
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
        if (propName.equals("checkPoint")) {
            return isSetCheckPoint();
        }
        if (propName.equals("changeTypes")) {
            return isSetChangeTypes();
        }
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
        if (propName.equals("checkPoint")) {
            getCheckPoint().add(((com.ibm.wsspi.security.wim.model.CheckPointType) value));
        }
        if (propName.equals("changeTypes")) {
            getChangeTypes().add(((String) value));
        }
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
        if (propName.equals("checkPoint")) {
            unsetCheckPoint();
        }
        if (propName.equals("changeTypes")) {
            unsetChangeTypes();
        }
        super.unset(propName);
    }

    /**
     * Gets the name of this model object, <b>ChangeControl</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String getTypeName() {
        return "ChangeControl";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>ChangeControl</b>
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
                names.add("checkPoint");
                names.add("changeTypes");
                names.addAll(SearchControl.getPropertyNames("SearchControl"));
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("checkPoint", "CheckPointType");
        dataTypeMap.put("changeTypes", "String");
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
        } else {
            return super.getDataType(propName);
        }
    }

    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList();
        }
        superTypeList.add("SearchControl");
        superTypeList.add("PropertyControl");
        superTypeList.add("Control");
    }

    /**
     * Gets a list of any model objects which this model object, <b>ChangeControl</b>, is
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
     * Gets a set of any model objects which extend this model object, <b>ChangeControl</b>
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
     * Returns this model object, <b>ChangeControl</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
