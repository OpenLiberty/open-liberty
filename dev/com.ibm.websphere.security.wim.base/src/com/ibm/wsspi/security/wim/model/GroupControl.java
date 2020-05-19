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
 * <p>Java class for GroupControl complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GroupControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}HierarchyControl">
 * &lt;attribute name="modifyMode" type="{http://www.w3.org/2001/XMLSchema}int" default="1" />
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * <p> The GroupControl object extends the HierarchyControl object, and defines one property: <b>modifyMode</b>.
 * 
 * <p> The GroupControl object may be used to specify the assign or un-assign mode through
 * the <b>modifyMode</b> property. Multiple entities can be assigned or un-assigned in a single call.
 * If there is only partial success when assigning or un-assigning multiple entities, an exception will be thrown.
 * It is responsibility of the caller to perform any clean-up needed in the event of an exception.
 * 
 * <ul>
 * <li><b>modifyMode</b>: controls the number of members defined in a group object to be added as members of this group</li>
 * <ul>
 * <li><b>1</b>: this default setting will cause the members in the group object to be added to this group as its members</li>
 * <li><b>2</b>: will cause the members contained in the group object to be added to the group as its members, and all the existing
 * members of the group will be removed.</li>
 * <li><b>3</b>: will cause the members contained in the group object to be un-assigned from the group.</li>
 * </ul>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GroupControl")
@XmlSeeAlso({
             GroupMemberControl.class,
             GroupMembershipControl.class
})
@Trivial
public class GroupControl
                extends HierarchyControl
{

    @XmlAttribute(name = "modifyMode")
    protected Integer modifyMode;
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
     * Gets the value of the <b>modifyMode</b> property.
     * 
     * @return
     *         possible object is {@link Integer }
     * 
     */
    public int getModifyMode() {
        if (modifyMode == null) {
            return 1;
        } else {
            return modifyMode;
        }
    }

    /**
     * Sets the value of the <b>modifyMode</b> property.
     * 
     * @param value
     *            allowed object is {@link Integer }
     * 
     */
    public void setModifyMode(int value) {
        this.modifyMode = value;
    }

    /**
     * Returns true if the <b>modifyMode</b> property is set; false, otherwise.
     * 
     * @return
     *         returned object is {@link boolean }
     * 
     */

    public boolean isSetModifyMode() {
        return (this.modifyMode != null);
    }

    /**
     * Resets the <b>modifyMode</b> property to null.
     * 
     */

    public void unsetModifyMode() {
        this.modifyMode = null;
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
        if (propName.equals("modifyMode")) {
            return getModifyMode();
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
        if (propName.equals("modifyMode")) {
            return isSetModifyMode();
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
        if (propName.equals("modifyMode")) {
            setModifyMode(((Integer) value));
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
        if (propName.equals("modifyMode")) {
            unsetModifyMode();
        }
        super.unset(propName);
    }

    /**
     * Gets the name of this model object, <b>GroupControl</b>
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String getTypeName() {
        return "GroupControl";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>GroupControl</b>
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
                names.add("modifyMode");
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
        dataTypeMap.put("modifyMode", "Integer");
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
     * Gets a list of any model objects which this model object, <b>GroupControl</b>, is
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
        subTypeList.add("GroupMemberControl");
        subTypeList.add("GroupMembershipControl");
    }

    /**
     * Gets a set of any model objects which extend this model object, <b>GroupControl</b>
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
     * Returns this model object, <b>GroupControl</b>, and its contents as a String
     * 
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
