/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;

/**
 * Represents the <cloudant> element in server.xml
 */
public class Cloudant extends ConfigElement implements ModifiableConfigElement {

    private final Class<?> c = Cloudant.class;

    // attributes
    private String account;
    private String connectTimeout;
    private String containerAuthDataRef;
    private String jndiName;
    private String libraryRef;
    private String maxConnections;
    private String password;
    private String proxyPassword;
    private String proxyUrl;
    private String proxyUser;
    private String readTimeout;
    private String sslRef;
    private String url;
    private String username;
    private String fatModify;

    // nested elements
    @XmlElement(name = "containerAuthData")
    private ConfigElementList<AuthData> containerAuthDatas;

    @XmlElement(name = "library")
    private ConfigElementList<Library> libraries;

    @XmlElement(name = "ssl")
    private ConfigElementList<ConfigElement> ssls; // TODO find or create simplicity object for ssl

    public String getAccount() {
        return account;
    }

    // getters for attributes
    public String getContainerAuthDataRef() {
        return containerAuthDataRef;
    }

    public String getConnectTimeout() {
        return connectTimeout;
    }

    public String getJndiName() {
        return jndiName;
    }

    public String getLibraryRef() {
        return libraryRef;
    }

    public String getMaxConnections() {
        return maxConnections;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public String getPassword() {
        return password;
    }

    public String getReadTimeout() {
        return readTimeout;
    }

    public String getSslRef() {
        return sslRef;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getFatModify() {
        return fatModify;
    }

    // getters for nested elements
    public ConfigElementList<AuthData> getContainerAuthDatas() {
        return containerAuthDatas == null ? (containerAuthDatas = new ConfigElementList<AuthData>()) : containerAuthDatas;
    }

    public ConfigElementList<Library> getLibraries() {
        return libraries == null ? (libraries = new ConfigElementList<Library>()) : libraries;
    }

    // TODO find or create simplicity object for ssl
    public ConfigElementList<ConfigElement> getSsls() {
        return ssls == null ? (ssls = new ConfigElementList<ConfigElement>()) : ssls;
    }

    // setters for attributes
    @XmlAttribute
    public void setAccount(String value) {
        account = value;
    }

    @XmlAttribute
    public void setConnectTimeout(String value) {
        connectTimeout = value;
    }

    @XmlAttribute
    public void setContainerAuthDataRef(String value) {
        containerAuthDataRef = value;
    }

    @XmlAttribute
    public void setJndiName(String value) {
        jndiName = value;
    }

    @XmlAttribute
    public void setLibraryRef(String value) {
        libraryRef = value;
    }

    @XmlAttribute
    public void setMaxConnections(String value) {
        maxConnections = value;
    }

    @XmlAttribute
    public void setPassword(String value) {
        password = value;
    }

    @XmlAttribute
    public void setProxyPassword(String value) {
        proxyPassword = value;
    }

    @XmlAttribute
    public void setProxyUrl(String value) {
        proxyUrl = value;
    }

    @XmlAttribute
    public void setProxyUser(String value) {
        proxyUser = value;
    }

    @XmlAttribute
    public void setReadTimeout(String value) {
        readTimeout = value;
    }

    @XmlAttribute
    public void setSslRef(String value) {
        sslRef = value;
    }

    @XmlAttribute
    public void setUrl(String value) {
        url = value;
    }

    @XmlAttribute
    public void setUsername(String value) {
        username = value;
    }

    @XmlAttribute(name = "fat.modify")
    public void setFatModify(String fatModify) {
        this.fatModify = fatModify;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // attributes
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (account != null)
            buf.append("account=").append(account).append(' ');
        if (connectTimeout != null)
            buf.append("connectTimeout=").append(connectTimeout).append(' ');
        if (containerAuthDataRef != null)
            buf.append("containerAuthDataRef=").append(containerAuthDataRef).append(' ');
        if (jndiName != null)
            buf.append("jndiName=").append(jndiName).append(' ');
        if (libraryRef != null)
            buf.append("libraryRef=").append(libraryRef).append(' ');
        if (maxConnections != null)
            buf.append("maxConnections=").append(maxConnections).append(' ');
        if (password != null)
            buf.append("password=").append(password).append(' ');
        if (proxyPassword != null)
            buf.append("proxyPassword=").append(proxyPassword).append(' ');
        if (proxyUrl != null)
            buf.append("proxyUrl=").append(proxyUrl).append(' ');
        if (proxyUser != null)
            buf.append("proxyUser=").append(proxyUser).append(' ');
        if (readTimeout != null)
            buf.append("readTimeout=").append(readTimeout).append(' ');
        if (sslRef != null)
            buf.append("sslRef=").append(sslRef).append(' ');
        if (url != null)
            buf.append("url=").append(url).append(' ');
        if (username != null)
            buf.append("username=").append(username).append(' ');
        // nested elements
        if (containerAuthDatas != null)
            buf.append(containerAuthDatas).append(' ');
        if (libraries != null)
            buf.append(libraries).append(' ');
        if (ssls != null)
            buf.append(ssls).append(' ');
        buf.append('}');
        return buf.toString();
    }

    @Override
    public void modify(ServerConfiguration config) throws Exception {
        if (fatModify == null || !fatModify.toLowerCase().equals("true"))
            return;

        Bootstrap b = Bootstrap.getInstance();

        if (getAccount() != null) {
            setAccount(b.getValue(BootstrapProperty.DB_ACCOUNT.getPropertyName()));
        } else {
            boolean secure = expand(config, getUrl()).startsWith("https");
            String protocol = secure ? "https://" : "http://";
            String hostname = b.getValue(BootstrapProperty.DB_HOSTNAME.getPropertyName());
            String port = secure ? b.getValue(BootstrapProperty.DB_PORT_SECURE.getPropertyName()) :
                            b.getValue(BootstrapProperty.DB_PORT.getPropertyName());
            setUrl(protocol + hostname + ':' + port);
        }

        if (getUsername() != null)
            setUsername(b.getValue(BootstrapProperty.DB_USER1.getPropertyName()));

        if (getPassword() != null)
            setPassword(b.getValue(BootstrapProperty.DB_PASSWORD1.getPropertyName()));
    }
}
