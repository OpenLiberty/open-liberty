/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.metadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.ibm.websphere.ras.ProtectedString;

/**
 * The runtime information of Port Component Reference
 */
public class PortComponentRefInfo implements Serializable {

    private static final long serialVersionUID = -8791033826086394813L;

    private final String serviceEndpointInterface;

    private final QName portQName;

    private List<WebServiceFeatureInfo> webServiceFeatureInfos;

    private String portComponentLink;
    /**
     * the endpoint uri for the port.
     */
    private String address;

    private String userName;

    private transient ProtectedString password;

    private String sslRef;

    private String keyAlias;

    private Map<String, String> properties;

    public PortComponentRefInfo(String serviceEndpointInterface) {
        this.portQName = null;
        this.serviceEndpointInterface = serviceEndpointInterface;
    }

    public PortComponentRefInfo(QName portQName) {
        this.portQName = portQName;
        this.serviceEndpointInterface = null;
    }

    public void addWebServiceFeatureInfo(WebServiceFeatureInfo webServiceFeatureInfo) {
        if (webServiceFeatureInfos == null) {
            webServiceFeatureInfos = new ArrayList<WebServiceFeatureInfo>(3);
        }
        webServiceFeatureInfos.add(webServiceFeatureInfo);
    }

    public List<WebServiceFeatureInfo> getWebServiceFeatureInfos() {
        return webServiceFeatureInfos == null ? Collections.<WebServiceFeatureInfo> emptyList() : Collections.unmodifiableList(webServiceFeatureInfos);
    }

    public String getServiceEndpointInterface() {
        return serviceEndpointInterface;
    }

    public void removeWebServiceFeatureInfo(WebServiceFeatureInfo webServiceFeatureInfo) {
        if (webServiceFeatureInfos != null) {
            webServiceFeatureInfos.remove(webServiceFeatureInfo);
        }
    }

    /**
     * @return the portComponentLink
     */
    public String getPortComponentLink() {
        return portComponentLink;
    }

    /**
     * @param portComponentLink the portComponentLink to set
     */
    public void setPortComponentLink(String portComponentLink) {
        this.portComponentLink = portComponentLink;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public ProtectedString getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(ProtectedString password) {
        this.password = password;
    }

    /**
     * @return the portQName
     */
    public QName getPortQName() {
        return portQName;
    }

    /**
     * @return the sslRef
     */
    public String getSSLRef() {
        return sslRef;
    }

    /**
     * @param sslRef the sslRef to set
     */
    public void setSSLRef(String sslRef) {
        this.sslRef = sslRef;
    }

    /**
     * @return the alias
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
     * @param keyAlias the alias to set
     */
    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

}
