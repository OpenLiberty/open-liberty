/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

/**
 *
 */
public class SSLConfig extends ConfigElement {

    private String sslProtocol;
    private String keyStoreRef;
    private String trustStoreRef;
    private String securityLevel;
    private String enabledCiphers;
    private String clientKeyAlias;
    private String serverKeyAlias;
    private Boolean clientAuthentication;
    private Boolean clientAuthenticationSupported;

    /**
     * @return the keyStoreRef
     */
    public String getKeyStoreRef() {
        return keyStoreRef;
    }

    /**
     * @param keyStoreRef
     *                        the keyStoreRef to set
     */
    @XmlAttribute(name = "keyStoreRef")
    public void setKeyStoreRef(String keyStoreRef) {
        this.keyStoreRef = keyStoreRef;
    }

    /**
     * @return the trustStoreRef
     */
    public String getTrustStoreRef() {
        return trustStoreRef;
    }

    /**
     * @param trustStoreRef
     *                          the trustStoreRef to set
     */
    @XmlAttribute(name = "trustStoreRef")
    public void setTrustStoreRef(String trustStoreRef) {
        this.trustStoreRef = trustStoreRef;
    }

    /**
     * @return the sslProtocol
     */
    public String getSslProtocol() {
        return sslProtocol;
    }

    /**
     * @param sslProtocol
     *                        the sslProtocol to set
     */
    @XmlAttribute(name = "sslProtocol")
    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    /**
     * @return the securityLevel
     */
    public String getSecurityLevel() {
        return securityLevel;
    }

    /**
     * @param securityLevel
     *                          the securityLevel to set
     */
    @XmlAttribute(name = "securityLevel")
    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    /**
     * @return the enabledCiphers
     */
    public String getEnabledCiphers() {
        return enabledCiphers;
    }

    /**
     * @param enabledCiphers
     *                           the enabledCiphers to set
     */
    @XmlAttribute(name = "enabledCiphers")
    public void setEnabledCiphers(String enabledCiphers) {
        this.enabledCiphers = enabledCiphers;
    }

    /**
     * @return the clientKeyAlias
     */
    public String getClientKeyAlias() {
        return clientKeyAlias;
    }

    /**
     * @param clientKeyAlias
     *                           the clientKeyAlias to set
     */
    @XmlAttribute(name = "clientKeyAlias")
    public void setClientKeyAlias(String clientKeyAlias) {
        this.clientKeyAlias = clientKeyAlias;
    }

    /**
     * @return the serverKeyAlias
     */
    public String getServerKeyAlias() {
        return serverKeyAlias;
    }

    /**
     * @param serverKeyAlias
     *                           the serverKeyAlias to set
     */
    @XmlAttribute(name = "serverKeyAlias")
    public void setServerKeyAlias(String serverKeyAlias) {
        this.serverKeyAlias = serverKeyAlias;
    }

    /**
     * @return the clientAuthentication
     */

    public Boolean getClientAuthentication() {
        return clientAuthentication;
    }

    /**
     * @param clientAuthentication
     *                                 the clientAuthentication to set
     */
    @XmlAttribute(name = "clientAuthentication")
    public void setClientAuthentication(Boolean clientAuthentication) {
        this.clientAuthentication = clientAuthentication;
    }

    /**
     * @return the clientAuthenticationSupported
     */

    public Boolean getClientAuthenticationSupported() {
        return clientAuthenticationSupported;
    }

    /**
     * @param clientAuthenticationSupported
     *                                          the clientAuthenticationSupoorted to set
     */
    @XmlAttribute(name = "clientAuthenticationSupported")
    public void setClientAuthenticationSupported(Boolean clientAuthenticationSupported) {
        this.clientAuthenticationSupported = clientAuthenticationSupported;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("SSL{");
        buf.append("id=\"" + getId() + "\" ");
        if (clientAuthentication != null)
            buf.append("clientAuthentication=\"" + clientAuthentication + "\" ");
        if (clientAuthenticationSupported != null)
            buf.append("clientAuthenticationSupported=\"" + clientAuthenticationSupported + "\" ");
        if (keyStoreRef != null)
            buf.append("keyStoreRef=\"" + keyStoreRef + "\" ");
        if (trustStoreRef != null)
            buf.append("trustStoreRef=\"" + trustStoreRef + "\" ");
        if (sslProtocol != null)
            buf.append("sslProtocol=\"" + sslProtocol + "\" ");
        if (enabledCiphers != null)
            buf.append("enabledCiphers=\"" + enabledCiphers + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
