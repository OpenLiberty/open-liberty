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
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * <p>Java class for LoginAccount complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <p>The LoginAccount object is extended from the {@link Party} object, and ultimately the {@link RolePlayer} object,
 * to enable LoginAccounts to play roles.
 *
 * <p>Below is a list of supported properties for {@link LoginAccount}.
 *
 * <ul>
 * <li><b>principalName</b>: specifies the name of the user associated with this LoginAccount.</li>
 * <li><b>password</b>: specifies the password for the user specified in <b>principalName</b>.</li>
 * <li><b>realm</b>: specifies the realm in which this user exists.</li>
 * <li><b>certificate</b>: specifies the certificate(s) a user may be authenticated with.</li>
 * </ul>
 *
 * <p>In addition to the properties in the list above, all properties from the super-class {@link Party} and its
 * super-classes are supported.
 *
 * <p>A principal may have multiple LoginAccounts.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = LoginAccount.TYPE_NAME, propOrder = {
                                                      "principalName",
                                                      "password",
                                                      "realm",
                                                      "certificate"
})
@XmlSeeAlso({ PersonAccount.class })
public class LoginAccount extends Party {

    /** The type name for this data type. */
    public static final String TYPE_NAME = "LoginAccount";

    /** Property name constant for the <b>principalName</b> property. */
    private static final String PROP_PRINCIPAL_NAME = "principalName";

    /** Property name constant for the <b>password</b> property. */
    private static final String PROP_PASSWORD = "password";

    /** Property name constant for the <b>realm</b> property. */
    private static final String PROP_REALM = "realm";

    /** Property name constant for the <b>certificate</b> property. */
    private static final String PROP_CERTIFICATE = "certificate";

    /**
     * The name of the user associated with this LoginAccount.
     */
    @XmlElement(name = PROP_PRINCIPAL_NAME)
    protected String principalName;

    /**
     * The password for the user specified in <b>principalName</b>.
     */
    @XmlElement(name = PROP_PASSWORD)
    protected byte[] password;

    /**
     * The realm in which this user exists.
     */
    @XmlElement(name = PROP_REALM)
    protected String realm;

    /**
     * The certificate(s) a user may be authenticated with.
     */
    @XmlElement(name = PROP_CERTIFICATE)
    protected List<byte[]> certificate;

    /** List of mandatory properties. */
    private static List<String> mandatoryProperties = null;

    /** List of transient properties. */
    private static List<String> transientProperties = null;

    /** The list of properties that comprise this type. */
    private static List<String> propertyNames = null;

    /** A mapping of property names to data types. */
    private static HashMap<String, String> dataTypeMap = null;

    /** A list of super-types of this type. */
    private static ArrayList<String> superTypeList = null;

    /** A set of sub-types of this type. */
    private static HashSet<String> subTypeSet = null;

    /** The set of multi-valued properties for this entity type. */
    private static final Set<String> MULTI_VALUED_PROPERTIES;

    static {
        setMandatoryPropertyNames();
        setTransientPropertyNames();
        getTransientProperties();
        setDataTypeMap();
        setSuperTypes();
        setSubTypes();

        MULTI_VALUED_PROPERTIES = new HashSet<String>();
        MULTI_VALUED_PROPERTIES.add(PROP_CERTIFICATE);
    }

    /**
     * Gets the value of the <b>principalName</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getPrincipalName() {
        return principalName;
    }

    /**
     * Sets the value of the <b>principalName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setPrincipalName(String value) {
        this.principalName = value;
    }

    /**
     * Returns a true if the <b>principalName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetPrincipalName() {
        return (this.principalName != null);
    }

    /**
     * Gets the value of the <b>password</b> property.
     *
     * @return
     *         possible object is
     *         byte[]
     */
    @Sensitive
    public byte[] getPassword() {
        return password;
    }

    /**
     * Sets the value of the <b>password</b> property.
     *
     * @param value
     *            allowed object is
     *            byte[]
     */
    @Sensitive
    public void setPassword(@Sensitive byte[] value) {
        this.password = (value);
    }

    /**
     * Returns a true if the <b>password</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetPassword() {
        return (this.password != null);
    }

    /**
     * Gets the value of the <b>realm</b> property.
     *
     * @return
     *         possible object is {@link String }
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Sets the value of the <b>realm</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     */
    public void setRealm(String value) {
        this.realm = value;
    }

    /**
     * Returns a true if the <b>realm</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetRealm() {
        return (this.realm != null);
    }

    /**
     * Gets the value of the <b>certificate</b> property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the certificate property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getCertificate().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * byte[]
     *
     * @return
     *         returned object is {@link List}
     */
    public List<byte[]> getCertificate() {
        if (certificate == null) {
            certificate = new ArrayList<byte[]>();
        }
        return this.certificate;
    }

    /**
     * Returns a true if the <b>certificate</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     */
    public boolean isSetCertificate() {
        return ((this.certificate != null) && (!this.certificate.isEmpty()));
    }

    /**
     * Resets the <b>certificate</b> property to null
     */
    public void unsetCertificate() {
        this.certificate = null;
    }

    @Override
    public Object get(String propName) {
        if (propName.equals(PROP_PRINCIPAL_NAME)) {
            return getPrincipalName();
        }
        if (propName.equals(PROP_PASSWORD)) {
            return getPassword();
        }
        if (propName.equals(PROP_REALM)) {
            return getRealm();
        }
        if (propName.equals(PROP_CERTIFICATE)) {
            return getCertificate();
        }
        return super.get(propName);
    }

    @Override
    public boolean isSet(String propName) {
        if (propName.equals(PROP_PRINCIPAL_NAME)) {
            return isSetPrincipalName();
        }
        if (propName.equals(PROP_PASSWORD)) {
            return isSetPassword();
        }
        if (propName.equals(PROP_REALM)) {
            return isSetRealm();
        }
        if (propName.equals(PROP_CERTIFICATE)) {
            return isSetCertificate();
        }
        return super.isSet(propName);
    }

    @Override
    public void set(String propName, Object value) {
        if (propName.equals(PROP_PRINCIPAL_NAME)) {
            setPrincipalName(((String) value));
        }
        if (propName.equals(PROP_PASSWORD)) {
            setPassword(((byte[]) value));
        }
        if (propName.equals(PROP_REALM)) {
            setRealm(((String) value));
        }
        if (propName.equals(PROP_CERTIFICATE)) {
            getCertificate().add(((byte[]) value));
        }
        super.set(propName, value);
    }

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_CERTIFICATE)) {
            unsetCertificate();
        }
        super.unset(propName);
    }

    @Override
    public String getTypeName() {
        return "LoginAccount";
    }

    /**
     * Set the list of mandatory properties.
     */
    private static synchronized void setMandatoryPropertyNames() {
        if (mandatoryProperties != null) {
            return;
        }
        mandatoryProperties = new ArrayList<String>();
    }

    /**
     * Set the list of transient properties.
     */
    private static synchronized void setTransientPropertyNames() {
        if (transientProperties != null) {
            return;
        }
        transientProperties = new ArrayList<String>();
        transientProperties.add(PROP_CERTIFICATE);
        transientProperties.addAll(Party.getTransientProperties());
    }

    @Override
    public boolean isMandatory(String propName) {
        if (mandatoryProperties == null) {
            setMandatoryPropertyNames();
        }
        return mandatoryProperties.contains(propName);
    }

    @Override
    public boolean isPersistentProperty(String propName) {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        return transientProperties.contains(propName);
    }

    /**
     * Get the list of transient properties.
     *
     * @return The list of transient properties.
     */
    protected static List<String> getTransientProperties() {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        return Collections.unmodifiableList(transientProperties);
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
        if (propertyNames != null) {
            return propertyNames;
        } else {
            List<String> names = new ArrayList<String>();
            names.add(PROP_PRINCIPAL_NAME);
            names.add(PROP_PASSWORD);
            names.add(PROP_REALM);
            names.add(PROP_CERTIFICATE);
            names.addAll(Party.getPropertyNames(Party.TYPE_NAME));
            propertyNames = Collections.unmodifiableList(names);
            return propertyNames;
        }
    }

    /**
     * Create the property name to data type mapping.
     */
    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap<String, String>();
        }
        dataTypeMap.put(PROP_PRINCIPAL_NAME, "String");
        dataTypeMap.put(PROP_PASSWORD, "byte[]");
        dataTypeMap.put(PROP_REALM, "String");
        dataTypeMap.put(PROP_CERTIFICATE, "byte[]");
    }

    @Override
    public String getDataType(String propName) {
        if (dataTypeMap.containsKey(propName)) {
            return (dataTypeMap.get(propName));
        } else {
            return super.getDataType(propName);
        }
    }

    /**
     * Create the list of super-types for this type.
     */
    private static synchronized void setSuperTypes() {
        if (superTypeList == null) {
            superTypeList = new ArrayList<String>();
        }
        superTypeList.add(Party.TYPE_NAME);
        superTypeList.add(RolePlayer.TYPE_NAME);
        superTypeList.add(Entity.TYPE_NAME);
    }

    @Override
    public ArrayList<String> getSuperTypes() {
        if (superTypeList == null) {
            setSuperTypes();
        }
        return superTypeList;
    }

    @Override
    public boolean isSubType(String superTypeName) {
        return superTypeList.contains(superTypeName);
    }

    /**
     * Create the list of sub-types for this type.
     */
    private static synchronized void setSubTypes() {
        if (subTypeSet == null) {
            subTypeSet = new HashSet<String>();
        }
        subTypeSet.add(PersonAccount.TYPE_NAME);
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
    public boolean isMultiValuedProperty(String propName) {
        return MULTI_VALUED_PROPERTIES.contains(propName) || super.isMultiValuedProperty(propName);
    }
}
