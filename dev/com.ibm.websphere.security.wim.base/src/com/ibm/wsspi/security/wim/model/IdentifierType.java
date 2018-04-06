/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for IdentifierType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="IdentifierType"&gt;
 * &lt;complexContent&gt;
 * &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 * &lt;attribute name="uniqueId" type="{http://www.w3.org/2001/XMLSchema}token" /&gt;
 * &lt;attribute name="uniqueName" type="{http://www.w3.org/2001/XMLSchema}token" /&gt;
 * &lt;attribute name="externalId" type="{http://www.w3.org/2001/XMLSchema}token" /&gt;
 * &lt;attribute name="externalName" type="{http://www.w3.org/2001/XMLSchema}token" /&gt;
 * &lt;attribute name="repositoryId" type="{http://www.w3.org/2001/XMLSchema}token" /&gt;
 * &lt;/restriction&gt;
 * &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 * <p> The IdentifierType object provides the capability of adding certain properties on an Entity type
 * to distinguish the object. The properties <b>uniqueName</b> and <b>uniqueId</b> are used to uniquely identify an
 * Entity in VMM. Entities that are supported in VMM include the Person and Group.
 *
 * <ul>
 * <li><b>uniqueId</b>: is machine-friendly. i.e., it is not readily recognizable or readily generated by a human.
 * It should be a globally unique identifier generated either by VMM or by the underlying repositories. The client
 * should not interpret the content of <b>uniqueId</b>. The <b>uniqueId</b> property should never change and should not
 * be reused. If an entity is renamed or moved, its <b>uniqueId</b> must remain the same.</li>
 * <li><b>uniqueName</b>: is human-friendly. It is in the form of an LDAP distinguished name. A distinguished
 * name is a string that includes the location of the entity in the VMM hierarchy, and is formed by concatenating the
 * relative distinguished name of the entity and each of its ancestors all the way to the root. For example, the
 * distinguished name of the person in the container "cn=user,dc=mycompany,dc=com" would be
 * "uid=myUser,cn=users,dc=mycompany,dc=com".</li>
 * <li><b>externalId</b>: defines the external identifier generated by the underlying repository. Each repository
 * defines it's own unique externalId.
 * </ul>
 *
 * <p> By default, VMM requires the caller to identify an entity by either the uniqueId or uniqueName. If the property
 * <b>externalName</b> is specified, the externalName to used to identify the entity.
 * <ul>
 * <li><b>repositoryId</b>: defines the underlying repository in which this entity exists.</li>
 * </ul>
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdentifierType")
public class IdentifierType {

    @XmlAttribute(name = "uniqueId")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String uniqueId;
    @XmlAttribute(name = "uniqueName")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String uniqueName;
    @XmlAttribute(name = "externalId")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String externalId;
    @XmlAttribute(name = "externalName")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String externalName;
    @XmlAttribute(name = "repositoryId")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "token")
    protected String repositoryId;
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
     * Gets the value of the <b>uniqueId</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * Sets the value of the <b>uniqueId</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setUniqueId(String value) {
        this.uniqueId = value;
    }

    /**
     * Returns true if the <b>uniqueId</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetUniqueId() {
        return (this.uniqueId != null);
    }

    /**
     * Gets the value of the <b>uniqueName</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Sets the value of the <b>uniqueName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setUniqueName(String value) {
        this.uniqueName = value;
    }

    /**
     * Returns true if the <b>uniqueName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetUniqueName() {
        return (this.uniqueName != null);
    }

    /**
     * Gets the value of the <b>externalId</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * Sets the value of the <b>externalId</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setExternalId(String value) {
        this.externalId = value;
    }

    /**
     * Returns true if the <b>externalId</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetExternalId() {
        return (this.externalId != null);
    }

    /**
     * Gets the value of the <b>externalName</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getExternalName() {
        return externalName;
    }

    /**
     * Sets the value of the <b>externalName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setExternalName(String value) {
        this.externalName = value;
    }

    /**
     * Returns true if the <b>externalName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetExternalName() {
        return (this.externalName != null);
    }

    /**
     * Gets the value of the <b>repositoryId</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getRepositoryId() {
        return repositoryId;
    }

    /**
     * Sets the value of the <b>repositoryId</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setRepositoryId(String value) {
        this.repositoryId = value;
    }

    /**
     * Returns true if the <b>repositoryId</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */
    public boolean isSetRepositoryId() {
        return (this.repositoryId != null);
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

    public Object get(String propName) {
        if (propName.equals("uniqueId")) {
            return getUniqueId();
        }
        if (propName.equals("uniqueName")) {
            return getUniqueName();
        }
        if (propName.equals("externalId")) {
            return getExternalId();
        }
        if (propName.equals("externalName")) {
            return getExternalName();
        }
        if (propName.equals("repositoryId")) {
            return getRepositoryId();
        }
        return null;
    }

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */

    public boolean isSet(String propName) {
        if (propName.equals("uniqueId")) {
            return isSetUniqueId();
        }
        if (propName.equals("uniqueName")) {
            return isSetUniqueName();
        }
        if (propName.equals("externalId")) {
            return isSetExternalId();
        }
        if (propName.equals("externalName")) {
            return isSetExternalName();
        }
        if (propName.equals("repositoryId")) {
            return isSetRepositoryId();
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
     *
     */

    public void set(String propName, Object value) {
        if (propName.equals("uniqueId")) {
            setUniqueId(((String) value));
        }
        if (propName.equals("uniqueName")) {
            setUniqueName(((String) value));
        }
        if (propName.equals("externalId")) {
            setExternalId(((String) value));
        }
        if (propName.equals("externalName")) {
            setExternalName(((String) value));
        }
        if (propName.equals("repositoryId")) {
            setRepositoryId(((String) value));
        }
    }

    /**
     * Sets the value of provided property to null.
     *
     * @param propName
     *            allowed object is {@link String}
     *
     */

    public void unset(String propName) {}

    /**
     * Gets the name of this model object, <b>IdentifierType</b>
     *
     * @return
     *         returned object is {@link String}
     */

    public String getTypeName() {
        return "IdentifierType";
    }

    /**
     * Gets a list of all supported properties for this model object, <b>IdentifierType</b>
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
                names.add("uniqueId");
                names.add("uniqueName");
                names.add("externalId");
                names.add("externalName");
                names.add("repositoryId");
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put("uniqueId", "String");
        dataTypeMap.put("uniqueName", "String");
        dataTypeMap.put("externalId", "String");
        dataTypeMap.put("externalName", "String");
        dataTypeMap.put("repositoryId", "String");
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

    /**
     * Gets a list of any model objects which this model object, <b>IdentifierType</b>, is
     * an extension of.
     *
     * @return
     *         returned object is {@link ArrayList}
     */
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
    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    private static synchronized void setSubTypes() {
        if (subTypeList == null) {
            subTypeList = new HashSet();
        }
    }

    /**
     * Gets a set of any model objects which extend this model object, <b>IdentifierType</b>
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
     * Returns this model object, <b>IdentifierType</b>, and its contents as a String
     *
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

}
