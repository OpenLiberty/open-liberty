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
 * <p>Java class for Root complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="Root">
 * &lt;complexContent>
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 * &lt;sequence>
 * &lt;element name="contexts" type="{http://www.ibm.com/websphere/wim}Context" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element name="entities" type="{http://www.ibm.com/websphere/wim}Entity" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;element name="controls" type="{http://www.ibm.com/websphere/wim}Control" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;attribute name="validated" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 * &lt;/restriction>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * <p> Root is an object and does not have any meaning other than its use as a container. It has
 * several objects: contexts, entities, and controls, each of which are represented themselves
 * by objects. The Root object can contain an unlimited number of each of these objects.
 *
 * <ul>
 * <li><b>contexts</b>: contains 0 to n Context objects. The Context object specifies the contextual information
 * for the registry or repository call. Examples of such information include the realm or ip address to be used for
 * the call.</li>
 *
 * <li><b>entities</b>: contains 0 to n Entity objects. Each entity object represents a VMM entity like Person
 * or Group entity. It contains the actual data associated with the entity, like unique name, "uid" and "cn" attributes.
 * Allowing for multiple entities to be specified in the Root object provides the capability of returning multiple Person
 * or Group entries on a single get() API call, for example.</li>
 *
 * <li><b>controls</b>:: contains 0 to n Controls objects. The Control object is used for specifying the
 * request information and response information for the call. For example, the property names to be returned for a
 * get() API call can be specified in the PropertyControl object.</li>
 *
 * </ul>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Root", propOrder = {
                                      "contexts",
                                      "entities",
                                      "controls"
})
@Trivial
public class Root {

    protected List<com.ibm.wsspi.security.wim.model.Context> contexts;
    protected List<com.ibm.wsspi.security.wim.model.Entity> entities;
    protected List<com.ibm.wsspi.security.wim.model.Control> controls;
    @XmlAttribute(name = "validated")
    protected Boolean validated;
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
     * Gets the value of the contexts property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the contexts property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getContexts().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.Context }
     *
     *
     */
    public List<com.ibm.wsspi.security.wim.model.Context> getContexts() {
        if (contexts == null) {
            contexts = new ArrayList<com.ibm.wsspi.security.wim.model.Context>();
        }
        return this.contexts;
    }

    public boolean isSetContexts() {
        return ((this.contexts != null) && (!this.contexts.isEmpty()));
    }

    public void unsetContexts() {
        this.contexts = null;
    }

    /**
     * Gets the value of the entities property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the entities property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getEntities().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.Entity }
     *
     *
     */
    public List<com.ibm.wsspi.security.wim.model.Entity> getEntities() {
        if (entities == null) {
            entities = new ArrayList<com.ibm.wsspi.security.wim.model.Entity>();
        }
        return this.entities;
    }

    public boolean isSetEntities() {
        return ((this.entities != null) && (!this.entities.isEmpty()));
    }

    public void unsetEntities() {
        this.entities = null;
    }

    /**
     * Gets the value of the controls property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the controls property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getControls().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link com.ibm.wsspi.security.wim.model.Control }
     *
     *
     */
    public List<com.ibm.wsspi.security.wim.model.Control> getControls() {
        if (controls == null) {
            controls = new ArrayList<com.ibm.wsspi.security.wim.model.Control>();
        }
        return this.controls;
    }

    public boolean isSetControls() {
        return ((this.controls != null) && (!this.controls.isEmpty()));
    }

    public void unsetControls() {
        this.controls = null;
    }

    /**
     * Gets the value of the validated property.
     *
     * @return
     *         possible object is {@link Boolean }
     *
     */
    public boolean isValidated() {
        if (validated == null) {
            return false;
        } else {
            return validated;
        }
    }

    /**
     * Sets the value of the validated property.
     *
     * @param value
     *            allowed object is {@link Boolean }
     *
     */
    public void setValidated(boolean value) {
        this.validated = value;
    }

    public boolean isSetValidated() {
        return (this.validated != null);
    }

    public void unsetValidated() {
        this.validated = null;
    }

    public Object get(String propName) {
        if (propName.equals("contexts")) {
            return getContexts();
        }
        if (propName.equals("entities")) {
            return getEntities();
        }
        if (propName.equals("controls")) {
            return getControls();
        }
        return null;
    }

    public boolean isSet(String propName) {
        if (propName.equals("contexts")) {
            return isSetContexts();
        }
        if (propName.equals("entities")) {
            return isSetEntities();
        }
        if (propName.equals("controls")) {
            return isSetControls();
        }
        if (propName.equals("validated")) {
            return isSetValidated();
        }
        return false;
    }

    public void set(String propName, Object value) {
        if (propName.equals("contexts")) {
            getContexts().add(((com.ibm.wsspi.security.wim.model.Context) value));
        }
        if (propName.equals("entities")) {
            getEntities().add(((com.ibm.wsspi.security.wim.model.Entity) value));
        }
        if (propName.equals("controls")) {
            getControls().add(((com.ibm.wsspi.security.wim.model.Control) value));
        }
        if (propName.equals("validated")) {
            setValidated(((Boolean) value));
        }
    }

    public void unset(String propName) {
        if (propName.equals("contexts")) {
            unsetContexts();
        }
        if (propName.equals("entities")) {
            unsetEntities();
        }
        if (propName.equals("controls")) {
            unsetControls();
        }
        if (propName.equals("validated")) {
            unsetValidated();
        }
    }

    public String getTypeName() {
        return "Root";
    }

    public static synchronized List getPropertyNames(String entityTypeName) {
        if (propertyNames != null) {
            return propertyNames;
        } else {
            {
                List names = new ArrayList();
                names.add("contexts");
                names.add("entities");
                names.add("controls");
                names.add("validated");
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("contexts", "Context");
        dataTypeMap.put("entities", "Entity");
        dataTypeMap.put("controls", "Control");
        dataTypeMap.put("validated", "Boolean");
    }

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

    public ArrayList getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

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
