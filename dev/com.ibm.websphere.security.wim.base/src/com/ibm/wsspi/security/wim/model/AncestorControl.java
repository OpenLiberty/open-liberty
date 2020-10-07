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
 * <p>Java class for AncestorControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AncestorControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}HierarchyControl">
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The AncestorControl object extends the HierarchyControl object.
 * 
 * <p> AncestorControl is used in the input Root object of the get() API to request ancestors of the entity.
 * If AncestorControl is added to the Root object in the get() operation, this means the ancestors of the entities
 * under the Root object will be returned in the returning Root object.
 * 
 * <p> Besides indicating whether or not to return ancestors, AncestorControl can also be used for specifying the
 * properties returned for ancestors as well as the level of ancestor to be returned.
 * 
 * <ul>
 * <li> 0 - return all ancestors</li>
 * <li> 1 - return only the parent</li>
 * <li> 2 - return the parent and grandparent</li>
 * <li> Any level greater than 2 will return that specified number of ancestors</li>
 * </ul>
 * 
 * <p> Since AncestorControl is also extended from SearchControl, it is possible to specify other search properties such as
 * <b>countLimit</b>, and <b>timeLimit</b> in AncestorControl to only return the ancestors which satisfies the search criteria.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AncestorControl")
@Trivial
public class AncestorControl
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
     * Gets the value of the requested property.
     * 
     * @param propName
     *            allowed object is {@link String}
     * 
     * @return
     *         returned object is {@link Object}
     */
    @Override
    public Object get(String propName) {
        return super.get(propName);
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     * 
     * @param propName
     *            allowed object is {@link String}
     * 
     * @return
     *         returned object is {@link boolean}
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
     * Sets the value of the provided property to null.
     * 
     * @param propName
     *            allowed object is {@link String}
     */
    @Override
    public void unset(String propName) {
        super.unset(propName);
    }

    /**
     * Gets the name of this model object, <b>AncestorControl</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String getTypeName() {
        return "AncestorControl";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>AncestorControl</b>
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
     * Gets a list of any model objects which this model object, <b>AncestorControl</b>,
     * is an extension of.
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
     * Gets a set of any model objects which extend this model object, <b>AncestorControl</b>
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
     * Returns this model object, <b>AncestorControl</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
