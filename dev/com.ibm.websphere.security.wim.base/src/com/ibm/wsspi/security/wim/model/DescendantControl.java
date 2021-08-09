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
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for DescendantControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DescendantControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}HierarchyControl">
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The DescendantControl object extends the HierarchyControl object.
 * 
 * <p> It is used in the input Root object of the get() API to request descendants of the entity. If the DescendantControl
 * object is added to the Root object in a get() API call, this means the descendants of the entities under the Root object
 * will be returned in the returning Root object.
 * 
 * <p> Besides indicating whether or not to return descendants, DescendantControl can also be used for specifying the
 * properties to be returned for descendants as well as the level of descendants to be returned.
 * 
 * <ul>
 * <li><b>level</b></li>
 * <ul>
 * <li><b>0</b>: return all descendants</li>
 * <li><b>1</b>: return only the children</li>
 * <li><b>greater than 1</b>: return the specified number of descendants. For example, a level set to 2 will return both the children
 * and the granchildren.</li>
 * </ul>
 * </ul>
 * 
 * <p>Since DescendantControl is also extended from SearchControl, it is possible to specify other search properties like
 * <b>countLimit</b>, and <b>timeLimit</b> in DescendantControl to only return the descendants which satisfies the search criteria.
 * For example, adding the property <b>expression</b> with value: @xsi:type='Person' to only return the descendants which are
 * of the Person entity type.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DescendantControl")
@Trivial
public class DescendantControl
                extends HierarchyControl
{

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
        super.unset(propName);
    }

    /**
     * Gets the name of this model object, <b>DescendantControl</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String getTypeName() {
        return "DescendantControl";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>DescendantControl</b>
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
                names.addAll(HierarchyControl.getPropertyNames("HierarchyControl"));
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
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
        superTypeList.add("HierarchyControl");
        superTypeList.add("SearchControl");
        superTypeList.add("PropertyControl");
        superTypeList.add("Control");
    }

    /**
     * Gets a list of any model objects which this model object, <b>DescendantControl</b>, is
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
     * Gets a set of any model objects which extend this model object, <b>DescendantControl</b>
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
     * Returns this model object, <b>DescendantControl</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
