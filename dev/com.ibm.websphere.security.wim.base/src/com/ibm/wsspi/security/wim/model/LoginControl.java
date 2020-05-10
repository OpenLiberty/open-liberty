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
//

package com.ibm.wsspi.security.wim.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for LoginControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LoginControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}SearchControl">
 * &lt;sequence>
 * &lt;element name="mappedProperties" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The LoginControl object extends the SearchControl object and defines
 * the property <b>mappedProperties</b>.
 * 
 * <ul>
 * <li><b>mappedProperties</b>: used to specify a list of alternative principal names which are mapped to existing VMM properties.</li>
 * </ul>
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LoginControl", propOrder = {
                                             "mappedProperties"
})
@Trivial
public class LoginControl
                extends SearchControl
{

    protected List<String> mappedProperties;
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
     * Gets the value of the <b>mappedProperties</b> property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mappedProperties property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     * getMappedProperties().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list {@link String }
     * 
     * 
     */
    public List<String> getMappedProperties() {
        if (mappedProperties == null) {
            mappedProperties = new ArrayList<String>();
        }
        return this.mappedProperties;
    }

    public boolean isSetMappedProperties() {
        return ((this.mappedProperties != null) && (!this.mappedProperties.isEmpty()));
    }

    /**
     * Resets the <b>mappedProperties</b> property to null
     */

    public void unsetMappedProperties() {
        this.mappedProperties = null;
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
        if (propName.equals("mappedProperties")) {
            return getMappedProperties();
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
        if (propName.equals("mappedProperties")) {
            return isSetMappedProperties();
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
        if (propName.equals("mappedProperties")) {
            getMappedProperties().add(((String) value));
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
        if (propName.equals("mappedProperties")) {
            unsetMappedProperties();
        }
        super.unset(propName);
    }

    /**
     * Gets the name of this model object, <b>LoginControl</b>
     * 
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String getTypeName() {
        return "LoginControl";
    }

    /*
     * Gets a list of all supported properties for this model object, <b>LoginControl</b>
     * 
     * @param entityTypeName
     * allowed object is {@link String}
     * 
     * @return
     * returned object is {@link List}
     */

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("mappedProperties");
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
        dataTypeMap.put("mappedProperties", "String");
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
     * Gets a list of any model objects which this model object, <b>LoginControl</b>, is
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
     * Gets a set of any model objects which extend this model object, <b>LoginControl</b>
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
     * Returns this model object, <b>LoginControl</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
