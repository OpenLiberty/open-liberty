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
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for PageControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PageControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}Control">
 * &lt;attribute name="size" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 * &lt;attribute name="cookie" type="{http://www.w3.org/2001/XMLSchema}hexBinary" />
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The PageControl object extends the Control object, and contains two properties:
 * 
 * <ul>
 * <li><b>size</b>: indicates the size of the page.</li>
 * 
 * <li><b>cookie</b>: contains the cookie returned from a PageResponseControl
 * to the server can get the next page to the search. For the first call of a paging search,
 * this property should not be set to any value.</li>
 * 
 * </ul>
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PageControl")
@Trivial
public class PageControl
                extends Control
{

    @XmlAttribute(name = "size")
    protected Integer size;
    @XmlAttribute(name = "startIndex")
    protected Integer startIndex;
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
     * Gets the value of the <b>size</b> property.
     * 
     * @return
     *         possible object is {@link Integer }
     * 
     */
    public int getSize() {
        if (size == null) {
            return 0;
        } else {
            return size;
        }
    }

    /**
     * Sets the value of the <b>size</b> property.
     * 
     * @param value
     *            allowed object is {@link Integer }
     * 
     */
    public void setSize(int value) {
        this.size = value;
    }

    /**
     * Returns a true if the <b>size</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean}
     * 
     */

    public boolean isSetSize() {
        return (this.size != null);
    }

    /**
     * Resets the <b>size</b> property to null
     */

    public void unsetSize() {
        this.size = null;
    }

    /**
     * Gets the value of the <b>startIndex</b> property.
     * 
     * @return
     *         possible object is {@link Integer }
     * 
     */
    public int getStartIndex() {
        if (startIndex == null) {
            return 1;
        } else {
            return startIndex;
        }
    }

    /**
     * Sets the value of the <b>startIndex</b> property.
     * 
     * @param value
     *            allowed object is {@link Integer }
     * 
     */
    public void setStartIndex(int value) {
        this.startIndex = value;
    }

    /**
     * Returns a true if the <b>startIndex</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean}
     * 
     */

    public boolean isSetStartIndex() {
        return (this.startIndex != null);
    }

    /**
     * Resets the <b>startIndex</b> property to null
     */

    public void unsetStartIndex() {
        this.startIndex = null;
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
        if (propName.equals("size")) {
            return getSize();
        }
        if (propName.equals("startIndex")) {
            return getStartIndex();
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
        if (propName.equals("size")) {
            return isSetSize();
        }
        if (propName.equals("startIndex")) {
            return isSetStartIndex();
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
        if (propName.equals("size")) {
            setSize((Integer) value);
        }
        if (propName.equals("startIndex")) {
            setStartIndex((Integer) value);
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
        if (propName.equals("size")) {
            unsetSize();
        }
        if (propName.equals("startIndex")) {
            unsetStartIndex();
        }
        super.unset(propName);
    }

    /**
     * Gets the name of this model object, <b>PageControl</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String getTypeName() {
        return "PageControl";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>PageControl</b>
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
                names.add("size");
                names.add("startIndex");
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
        dataTypeMap.put("size", "Integer");
        dataTypeMap.put("startIndex", "Integer");
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
        superTypeList.add("Control");
    }

    /**
     * Gets a list of any model objects which this model object, <b>PageControl</b>, is
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
     * Gets a set of any model objects which extend this model object, <b>PageControl</b>
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
     * Returns this model object, <b>PageControl</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
