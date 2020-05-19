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
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for CacheControl complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CacheControl">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}Control">
 * &lt;sequence>
 * &lt;element name="mode" type="{http://www.w3.org/2001/XMLSchema}string"/>
 * &lt;/sequence>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * <p> The CacheControl object extends the Control object and defines a single property: <b>mode</b>.
 *
 * <p> The CacheControl object specifies whether the repository adapter cache should be cleared before an operation is performed
 * and the mode of clearing the cache. The CacheControl object can be passed to the get(), search(), and update() APIs.
 * The LDAP adapter uses the CacheControl to clear its cache; the other out-of-the-box adapters ignore the CacheControl as they
 * not have a cache. If you are using a custom adapter, you can implement its own handling for clearing cache using the CacheControl.
 *
 * <p> The <b>mode</b> property specifies the mode of clearing the repository adapter cache before an operation is performed. Valid values
 * are:
 * <ul>
 * <li><b>clearEntity</b>: clears the cache for the specified entity. This value does not have any effect on the search() API</li>
 * <li><b>clearAll</b>: clears all of the cached information in the adapter.</li>
 * </ul>
 *
 * <p> The values are not case-sensitive. There is no default value for this property. If you do not specify a value, or specify a
 * value other than clearEntity or clearAll, an error message appears.
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CacheControl", propOrder = {
                                              "mode"
})
@Trivial
public class CacheControl extends Control {

    @XmlElement(required = true)
    protected String mode;
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
     * Gets the value of the <b>mode</b> property.
     *
     * @return
     *         returned object is {@link String }
     *
     */
    public String getMode() {
        return mode;
    }

    /**
     * Sets the value of the <b>mode</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setMode(String value) {
        this.mode = value;
    }

    /**
     * Returns true if the <b>mode</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetMode() {
        return (this.mode != null);
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
        if (propName.equals("mode")) {
            return getMode();
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
        if (propName.equals("mode")) {
            return isSetMode();
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
        if (propName.equals("mode")) {
            setMode(((String) value));
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
        super.unset(propName);
    }

    /**
     * Gets the name of this model object, <b>CacheControl</b>
     *
     * @return
     *         returned object is {@link String}
     */
    @Override
    public String getTypeName() {
        return "CacheControl";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>CacheControl</b>
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
                names.add("mode");
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
        dataTypeMap.put("mode", "String");
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
     * Gets a list of any model objects which this model object, <b>CacheControl</b>, is
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
     * Gets a set of any model objects which extend this model object, <b>CacheControl</b>
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
     * Returns this model object, <b>CacheControl</b>, and its contents as a String
     *
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
