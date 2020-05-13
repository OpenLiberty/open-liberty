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
 * <p>Java class for DeleteControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DeleteControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}Control">
 * &lt;attribute name="deleteDescendants" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 * &lt;attribute name="returnDeleted" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The DeleteControl object extends the Control object.
 * 
 * <p> DeleteControl is used for specifying whether or not to delete descendants if the
 * entity to be deleted has descendants. It contains two properties: <b>deleteDescendants</b> and <b>returnDeleted</b>.
 * 
 * <ul>
 * <li><b>deleteDescendants</b>: indicates whether or not delete the descendants of the entity if it has
 * descendants.</li>
 * <ul>
 * <li><b>true</b>: the entity and all of its descendants will be deleted. This is the default value.</li>
 * <li><b>false</b>: an exception will be thrown if the entity has any descendants.</li>
 * </ul>
 * <li><b>returnDeleted</b>: indicates whether or not to return the entities which are actually deleted in
 * the output Root object. The default value is false, which means no output Root object is returned.</li>
 * </ul>
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeleteControl")
@Trivial
public class DeleteControl
                extends Control
{

    @XmlAttribute(name = "deleteDescendants")
    protected Boolean deleteDescendants;
    @XmlAttribute(name = "returnDeleted")
    protected Boolean returnDeleted;
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
     * Gets the value of the <b>deleteDescendants</b> property.
     * 
     * @return
     *         possible object is {@link Boolean }
     * 
     */
    public boolean isDeleteDescendants() {
        if (deleteDescendants == null) {
            return false;
        } else {
            return deleteDescendants;
        }
    }

    /**
     * Sets the value of the <b>deleteDescendants</b> property.
     * 
     * @param value
     *            allowed object is {@link Boolean }
     * 
     */
    public void setDeleteDescendants(boolean value) {
        this.deleteDescendants = value;
    }

    /**
     * Returns true if the <b>deleteDescendants</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */
    public boolean isSetDeleteDescendants() {
        return (this.deleteDescendants != null);
    }

    /**
     * Resets the <b>deleteDescendants</b> property to null.
     * 
     */
    public void unsetDeleteDescendants() {
        this.deleteDescendants = null;
    }

    /**
     * Gets the value of the <b>returnDeleted</b> property.
     * 
     * @return
     *         possible object is {@link Boolean }
     * 
     */
    public boolean isReturnDeleted() {
        if (returnDeleted == null) {
            return false;
        } else {
            return returnDeleted;
        }
    }

    /**
     * Sets the value of the <b>returnDeleted</b> property.
     * 
     * @param value
     *            allowed object is {@link Boolean }
     * 
     */
    public void setReturnDeleted(boolean value) {
        this.returnDeleted = value;
    }

    /**
     * Returns true if the <b>returnDeleted</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */
    public boolean isSetReturnDeleted() {
        return (this.returnDeleted != null);
    }

    /**
     * Resets the <b>returnDeleted</b> property to null.
     * 
     */
    public void unsetReturnDeleted() {
        this.returnDeleted = null;
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
        if (propName.equals("deleteDescendants")) {
            return isSetDeleteDescendants();
        }
        if (propName.equals("returnDeleted")) {
            return isSetReturnDeleted();
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
        if (propName.equals("deleteDescendants")) {
            setDeleteDescendants(((Boolean) value));
        }
        if (propName.equals("returnDeleted")) {
            setReturnDeleted(((Boolean) value));
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
        if (propName.equals("deleteDescendants")) {
            unsetDeleteDescendants();
        }
        if (propName.equals("returnDeleted")) {
            unsetReturnDeleted();
        }
        super.unset(propName);
    }

    /**
     * Gets the name of this model object, <b>DeleteControl</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String getTypeName() {
        return "DeleteControl";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>DeleteControl</b>
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
                names.add("deleteDescendants");
                names.add("returnDeleted");
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
        dataTypeMap.put("deleteDescendants", "Boolean");
        dataTypeMap.put("returnDeleted", "Boolean");
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
     * Gets a list of any model objects which this model object, <b>DeleteControl</b>, is
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
     * Gets a set of any model objects which extend this model object, <b>DeleteControl</b>
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
     * Returns this model object, <b>DeleteControl</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
