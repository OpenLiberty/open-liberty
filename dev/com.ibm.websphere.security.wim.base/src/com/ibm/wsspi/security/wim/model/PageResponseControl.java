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
 * <p>Java class for PageResponseControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PageResponseControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}Control">
 * &lt;attribute name="cookie" type="{http://www.w3.org/2001/XMLSchema}hexBinary" />
 * &lt;attribute name="totalSize" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The PageResponseControl object extends the Control object, and defines two
 * properties:
 * 
 * <ul>
 * <li><b>cookie</b>: contains the cookie to be used for the subsequent
 * calls in a paging search.</li>
 * 
 * <li><b>totalSize</b>: indicates the totalSize of the paging search.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PageResponseControl")
@Trivial
public class PageResponseControl
                extends Control
{

    @XmlAttribute(name = "totalSize")
    protected Integer totalSize;
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
     * Gets the value of the totalSize property.
     * 
     * @return
     *         possible object is {@link Integer }
     * 
     */
    public int getTotalSize() {
        if (totalSize == null) {
            return 0;
        } else {
            return totalSize;
        }
    }

    /**
     * Sets the value of the totalSize property.
     * 
     * @param value
     *            allowed object is {@link Integer }
     * 
     */
    public void setTotalSize(int value) {
        this.totalSize = value;
    }

    public boolean isSetTotalSize() {
        return (this.totalSize != null);
    }

    public void unsetTotalSize() {
        this.totalSize = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals("totalSize")) {
            return getTotalSize();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals("totalSize")) {
            return isSetTotalSize();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals("totalSize")) {
            setTotalSize(((Integer) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals("totalSize")) {
            unsetTotalSize();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return "PageResponseControl";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("totalSize");
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
        dataTypeMap.put("totalSize", "Integer");
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
