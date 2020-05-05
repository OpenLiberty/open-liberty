/*******************************************************************************
 * Copyright (c) 2017,2018 IBM Corporation and others.
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for Root complex type.
 *
 * <p> Root is an object and does not have any meaning other than its use as a container. It has
 * several objects: <b>contexts</b>, <b>entities</b>, and <b>controls</b>, each of which are represented themselves
 * by objects. The Root object can contain an unlimited number of each of these objects.
 *
 * <ul>
 * <li><b>contexts</b>: contains 0 to n {@link Context} objects. The {@link Context} object specifies the contextual information
 * for the registry or repository call. Examples of such information include the realm or IP address to be used for
 * the call.</li>
 *
 * <li><b>entities</b>: contains 0 to n {@link Entity} objects. Each {@link Entity} object represents a VMM entity like {@link Person}
 * or {@link Group} entity. It contains the actual data associated with the entity, like unique name, "uid" and "cn" attributes.
 * Allowing for multiple entities to be specified in the Root object provides the capability of returning multiple {@link Person}
 * or {@link Group} entries on a single get() API call, for example.</li>
 *
 * <li><b>controls</b>: contains 0 to n {@link Control} objects. The {@link Control} object is used for specifying the
 * request information and response information for the call. For example, the property names to be returned for a
 * get() API call can be specified in the {@link PropertyControl} object.</li>
 *
 * <li><b>validated</b>: a boolean indicating whether the data in this object has been validated.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = Root.TYPE_NAME, propOrder = {
                                              "contexts",
                                              "entities",
                                              "controls"
})
@XmlRootElement
public class Root {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "Root";

    /** Property name constant for the <b>contexts</b> property. */
    private static final String PROP_CONTEXTS = "contexts";

    /** Property name constant for the <b>entities</b> property. */
    private static final String PROP_ENTITIES = "entities";

    /** Property name constant for the <b>controls</b> property. */
    private static final String PROP_CONTROLS = "controls";

    /** Property name constant for the <b>validated</b> property. */
    private static final String PROP_VALIDATED = "validated";

    /**
     * Specifies the contextual information for the registry or repository call. Examples of such information include
     * the realm or IP address to be used for the call.
     */
    @XmlElement(name = PROP_CONTEXTS)
    protected List<Context> contexts;

    /**
     * Each {@link Entity} object represents a VMM entity like {@link Person}
     * or {@link Group} entity. It contains the actual data associated with the entity, like unique name, "uid" and "cn" attributes.
     * Allowing for multiple entities to be specified in the Root object provides the capability of returning multiple {@link Person}
     * or {@link Group} entries on a single get() API call, for example.
     */
    @XmlElement(name = PROP_ENTITIES)
    protected List<Entity> entities;

    /**
     * The {@link Control} object is used for specifying the request information and response information for the call.
     * For example, the property names to be returned for a get() API call can be specified in the {@link PropertyControl} object.
     */
    @XmlElement(name = PROP_CONTROLS)
    protected List<Control> controls;

    /**
     * A boolean indicating whether the data in this object has been validated.
     */
    @XmlAttribute(name = PROP_VALIDATED)
    protected Boolean validated;

    /** The list of properties that comprise this type. */
    private static List<String> propertyNames = null;

    /** A mapping of property names to data types. */
    private static HashMap<String, String> dataTypeMap = null;

    /** A list of super-types of this type. */
    private static ArrayList<String> superTypeList = null;

    /** A set of sub-types of this type. */
    private static HashSet<String> subTypeSet = null;

    static {
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();
    }

    /**
     * Gets the value of the <b>contexts</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>contexts</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getContexts().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link Context }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<Context> getContexts() {
        if (contexts == null) {
            contexts = new ArrayList<Context>();
        }
        return this.contexts;
    }

    /**
     * Returns true if the <b>contexts</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetContexts() {
        return ((this.contexts != null) && (!this.contexts.isEmpty()));
    }

    /**
     * Unset the <b>contexts</b> property.
     */
    public void unsetContexts() {
        this.contexts = null;
    }

    /**
     * Gets the value of the <b>entities</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>entities</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getEntities().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link Entity }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<Entity> getEntities() {
        if (entities == null) {
            entities = new ArrayList<Entity>();
        }
        return this.entities;
    }

    /**
     * Returns true if the <b>entities</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetEntities() {
        return ((this.entities != null) && (!this.entities.isEmpty()));
    }

    /**
     * Unset the <b>entities</b> property.
     */
    public void unsetEntities() {
        this.entities = null;
    }

    /**
     * Gets the value of the <b>controls</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the <b>controls</b> property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getControls().add(newItem);
     * </pre>
     *
     * <p>
     * Objects of the following type(s) are allowed in the list {@link Control }
     *
     * @return
     *         returned object is {@link List}
     */
    public List<Control> getControls() {
        if (controls == null) {
            controls = new ArrayList<Control>();
        }
        return this.controls;
    }

    /**
     * Returns true if the <b>controls</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetControls() {
        return ((this.controls != null) && (!this.controls.isEmpty()));
    }

    /**
     * Unset the <b>controls</b> property.
     */
    public void unsetControls() {
        this.controls = null;
    }

    /**
     * Gets the value of the <b>validated</b> property.
     *
     * @return
     *         possible object is {@link Boolean }
     */
    public boolean isValidated() {
        if (validated == null) {
            return false;
        } else {
            return validated;
        }
    }

    /**
     * Sets the value of the <b>validated</b> property.
     *
     * @param value
     *            allowed object is {@link Boolean }
     */
    public void setValidated(boolean value) {
        this.validated = value;
    }

    /**
     * Returns true if the <b>validated</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSetValidated() {
        return (this.validated != null);
    }

    /**
     * Unset the <b>validated</b> property.
     */
    public void unsetValidated() {
        this.validated = null;
    }

    /**
     * Gets the value of the requested property
     *
     * @param propName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link Object}
     */
    public Object get(String propName) {
        if (propName.equals(PROP_CONTEXTS)) {
            return getContexts();
        }
        if (propName.equals(PROP_ENTITIES)) {
            return getEntities();
        }
        if (propName.equals(PROP_CONTROLS)) {
            return getControls();
        }
        if (propName.equals(PROP_VALIDATED)) {
            return isValidated();
        }
        return null;
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @param propName
     *            The property name to check if set.
     * @return
     *         returned object is {@link boolean }
     */
    public boolean isSet(String propName) {
        if (propName.equals(PROP_CONTEXTS)) {
            return isSetContexts();
        }
        if (propName.equals(PROP_ENTITIES)) {
            return isSetEntities();
        }
        if (propName.equals(PROP_CONTROLS)) {
            return isSetControls();
        }
        if (propName.equals(PROP_VALIDATED)) {
            return isSetValidated();
        }
        return false;
    }

    /**
     * Sets the value of the provided property to the provided value.
     *
     * @param propName
     *            allowed object is {@link String}
     * @param value
     *            allowed object is {@link Object}
     */
    public void set(String propName, Object value) {
        if (propName.equals(PROP_CONTEXTS)) {
            getContexts().add(((Context) value));
        }
        if (propName.equals(PROP_ENTITIES)) {
            getEntities().add(((Entity) value));
        }
        if (propName.equals(PROP_CONTROLS)) {
            getControls().add(((Control) value));
        }
        if (propName.equals(PROP_VALIDATED)) {
            setValidated(((Boolean) value));
        }
    }

    /**
     * Sets the value of provided property to null.
     *
     * @param propName
     *            allowed object is {@link String}
     */
    public void unset(String propName) {
        if (propName.equals(PROP_CONTEXTS)) {
            unsetContexts();
        }
        if (propName.equals(PROP_ENTITIES)) {
            unsetEntities();
        }
        if (propName.equals(PROP_CONTROLS)) {
            unsetControls();
        }
        if (propName.equals(PROP_VALIDATED)) {
            unsetValidated();
        }
    }

    /**
     * Gets the name of this type.
     *
     * @return
     *         returned object is {@link String}
     */
    public String getTypeName() {
        return TYPE_NAME;
    }

    /**
     * Gets a list of all supported properties for this type.
     *
     * @param entityTypeName
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link List}
     */
    public static synchronized List<String> getPropertyNames(String entityTypeName) {
        if (propertyNames == null) {
            List<String> names = new ArrayList<String>();
            names.add(PROP_CONTEXTS);
            names.add(PROP_ENTITIES);
            names.add(PROP_CONTROLS);
            names.add(PROP_VALIDATED);
            propertyNames = Collections.unmodifiableList(names);
        }
        return propertyNames;
    }

    /**
     * Create the property name to data type mapping.
     */
    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap<String, String>();
        }
        dataTypeMap.put(PROP_CONTEXTS, Context.TYPE_NAME);
        dataTypeMap.put(PROP_ENTITIES, Entity.TYPE_NAME);
        dataTypeMap.put(PROP_CONTROLS, Control.TYPE_NAME);
        dataTypeMap.put(PROP_VALIDATED, "Boolean");
    }

    /**
     * Gets the Java type of the value of the provided property. For example: String
     *
     * @param propName
     *            allowed object is {@link String}
     *
     * @return
     *         returned object is {@link String}
     */
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return (dataTypeMap.get(propName));
        } else {
            return null;
        }
    }

    /**
     * Create the list of super-types for this type.
     */
    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList<String>();
        }
    }

    /**
     * Gets a list of any types which this type is an extension of.
     *
     * @return
     *         returned object is {@link ArrayList}
     */
    public ArrayList<String> getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    /**
     * Returns a true if the provided type is one that this type extends; false, otherwise.
     *
     * @param superTypeName
     *
     *            allowed object is {@link String}
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    /**
     * Create the set of sub-types for this type.
     */
    private static synchronized void setSubTypes() {
        if (subTypeSet == null) {
            subTypeSet = new HashSet<String>();
        }
    }

    /**
     * Gets a set of any types which extend this type.
     *
     * @return
     *         returned object is {@link HashSet}
     */
    public static HashSet<String> getSubTypes() {
        if (subTypeSet == null) {
            setSubTypes();
        }
        return subTypeSet;
    }

    @Override
    public String toString() {
        return WIMTraceHelper.traceJaxb(this);
    }
}
