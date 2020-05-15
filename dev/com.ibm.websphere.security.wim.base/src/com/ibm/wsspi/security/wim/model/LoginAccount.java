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
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.wim.ras.WIMTraceHelper;

/**
 * <p>Java class for LoginAccount complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="LoginAccount">
 * &lt;complexContent>
 * &lt;extension base="{http://www.ibm.com/websphere/wim}Party">
 * &lt;sequence>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}principalName" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}password" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}realm" minOccurs="0"/>
 * &lt;element ref="{http://www.ibm.com/websphere/wim}certificate" maxOccurs="unbounded" minOccurs="0"/>
 * &lt;/sequence>
 * &lt;/extension>
 * &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 * <p> The LoginAccount object is extended from the Party object, and ultimately the RolePlayer object,
 * to enable LoginAccounts to play roles.
 *
 * <p> The LoginAccount object is an entity type which contains the common properties for all sub-types of this
 * object. It has the following properties defined:
 *
 * <ul>
 *
 * <li><b>principalName</b>: specifies the name of the user associated with this LoginAccount.</li>
 *
 * <li><b>password</b>: specifies the password for the user specified in <b>principalName</b>.</li>
 *
 * <li><b>realm</b>: specifies the realm in which this user exists.</li>
 *
 * <li><b>certificate</b> property specifies the certificate the user may be authenticated with.</li>
 * </ul>
 *
 * <p> A principal may have multiple LoginAccounts.
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LoginAccount", propOrder = {
                                              "principalName",
                                              "password",
                                              "realm",
                                              "certificate"
})
@XmlSeeAlso({ PersonAccount.class })
@Trivial
public class LoginAccount extends Party {
    private static final String PROP_PRINCIPAL_NAME = "principalName";
    private static final String PROP_PASSWORD = "password";
    private static final String PROP_REALM = "realm";
    private static final String PROP_CERTIFICATE = "certificate";

    protected String principalName;
    protected byte[] password;
    protected String realm;
    protected List<byte[]> certificate;

    private static List mandatoryProperties = null;
    private static List transientProperties = null;
    private static List propertyNames = null;
    private static HashMap dataTypeMap = null;
    private static ArrayList superTypeList = null;
    private static HashSet subTypeList = null;
    private final static TraceComponent tc = Tr.register(LoginAccount.class);

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
     *
     */
    public String getPrincipalName() {
        return principalName;
    }

    /**
     * Sets the value of the <b>principalName</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setPrincipalName(String value) {
        this.principalName = value;
    }

    /**
     * Returns a true if the <b>principalName</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     *
     */

    public boolean isSetPrincipalName() {
        return (this.principalName != null);
    }

    /**
     * Gets the value of the ><b>password</b> property.
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
     *
     */

    public boolean isSetPassword() {
        return (this.password != null);
    }

    /**
     * Gets the value of the <b>realm</b> property.
     *
     * @return
     *         possible object is {@link String }
     *
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Sets the value of the <b>realm</b> property.
     *
     * @param value
     *            allowed object is {@link String }
     *
     */
    public void setRealm(String value) {
        this.realm = value;
    }

    /**
     * Returns a true if the <b>realm</b> property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean}
     *
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
     * returned list will be present inside the JAXB object.
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
     *
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

    /**
     * Returns true if the requested property is set; false, otherwise.
     *
     * @return
     *         returned object is {@link boolean }
     *
     */

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

    /**
     * Sets the value of provided property to null.
     *
     * @param propName
     *            allowed object is {@link String}
     *
     */

    @Override
    public void unset(String propName) {
        if (propName.equals(PROP_CERTIFICATE)) {
            unsetCertificate();
        }
        super.unset(propName);
    }

    /**
     * Gets the name of this model object, <b>LoginAccount</b>
     *
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String getTypeName() {
        return "LoginAccount";
    }

    private static synchronized void setMandatoryPropertyNames() {
        if (mandatoryProperties != null) {
            return;
        }
        mandatoryProperties = new ArrayList();
    }

    private static synchronized void setTransientPropertyNames() {
        if (transientProperties != null) {
            return;
        }
        transientProperties = new ArrayList();
        transientProperties.add(PROP_CERTIFICATE);
        transientProperties.addAll(Party.getTransientProperties());
    }

    @Override
    public boolean isMandatory(String propName) {
        if (mandatoryProperties == null) {
            setMandatoryPropertyNames();
        }
        if (mandatoryProperties.contains(propName)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isPersistentProperty(String propName) {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        if (transientProperties.contains(propName)) {
            return false;
        } else {
            return true;
        }
    }

    protected static List getTransientProperties() {
        if (transientProperties == null) {
            setTransientPropertyNames();
        }
        return transientProperties;
    }

    /**
     * Gets a list of all supported properties for this model object, <b>LoginAccount</b>
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
                names.add(PROP_PRINCIPAL_NAME);
                names.add(PROP_PASSWORD);
                names.add(PROP_REALM);
                names.add(PROP_CERTIFICATE);
                names.addAll(Party.getPropertyNames("Party"));
                propertyNames = Collections.unmodifiableList(names);
                return propertyNames;
            }
        }
    }

    private static synchronized void setDataTypeMap() {
        if (dataTypeMap == null) {
            dataTypeMap = new HashMap();
        }
        dataTypeMap.put(PROP_PRINCIPAL_NAME, "String");
        dataTypeMap.put(PROP_PASSWORD, "byte[]");
        dataTypeMap.put(PROP_REALM, "String");
        dataTypeMap.put(PROP_CERTIFICATE, "byte[]");
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
        superTypeList.add("Party");
        superTypeList.add("RolePlayer");
        superTypeList.add("Entity");
    }

    /**
     * Gets a list of any model objects which this model object, <b>LoginAccount</b>, is
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
        subTypeList.add("PersonAccount");

    }

    /**
     * Gets a set of any model objects which extend this model object, <b>LoginAccount</b>
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
     * Returns this model object, <b>LoginAccount</b>, and its contents as a String
     *
     * @return
     *         returned object is {@link String}
     */

    @Override
    public String toString() {
        return WIMTraceHelper.trace(this);
    }

    @Override
    public boolean isMultiValuedProperty(String propName) {
        return MULTI_VALUED_PROPERTIES.contains(propName) || super.isMultiValuedProperty(propName);
    }
}
