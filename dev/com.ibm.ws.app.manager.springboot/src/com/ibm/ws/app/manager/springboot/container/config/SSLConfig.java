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
package com.ibm.ws.app.manager.springboot.container.config;

/**
 * ssl element is defined here:<br>
 * /com.ibm.ws.ssl/resources/OSGI-INF/metatype/metatype.xml
 */
public class SSLConfig extends ConfigElement {

    public static final String XML_ATTRIBUTE_NAME_SSL_PROTOCOL = "sslProtocol";
    private String sslProtocol;

    public static final String XML_ATTRIBUTE_NAME_KEY_STORE_REF = "keyStoreRef";
    private String keyStoreRef;

    public static final String XML_ATTRIBUTE_NAME_TRUST_STORE_REF = "trustStoreRef";
    private String trustStoreRef;

    public static final String XML_ATTRIBUTE_NAME_SECURITY_LEVEL = "securityLevel";
    private String securityLevel;

    public static final String XML_ATTRIBUTE_NAME_ENABLED_CIPHERS = "enabledCiphers";
    private String enabledCiphers;

    public static final String XML_ATTRIBUTE_NAME_CLIENT_KEY_ALIAS = "clientKeyAlias";
    private String clientKeyAlias;

    public static final String XML_ATTRIBUTE_NAME_SERVER_KEY_ALIAS = "serverKeyAlias";
    private String serverKeyAlias;

    public static final String XML_ATTRIBUTE_NAME_CLIENT_AUTH = "clientAuthentication";
    private Boolean clientAuthentication;

    public static final String XML_ATTRIBUTE_NAME_CLIENT_AUTH_SUPPORTED = "clientAuthenticationSupported";
    private Boolean clientAuthenticationSupported;

    public static final String XML_ELEMENT_NAME_OUTBOUND_CONNECTION = "outboundConnection";
    private ConfigElementList<OutboundConnection> outboundConnections;

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
    public void setClientAuthenticationSupported(Boolean clientAuthenticationSupported) {
        this.clientAuthenticationSupported = clientAuthenticationSupported;
    }

    /**
     * @param connections
     *                        the outboundConnections to set
     */
    public void setOutboundConnection(ConfigElementList<OutboundConnection> connections) {
        this.outboundConnections = connections;
    }

    /**
     * @param connection
     *                       the outboundConnection to set
     */
    public void setOutboundConnectionToList(OutboundConnection connection) {
        if (this.outboundConnections == null) {
            this.outboundConnections = new ConfigElementList<OutboundConnection>();
        }

        this.outboundConnections.add(connection);
    }

    /**
     * @return the outboundConnection
     */
    public ConfigElementList<OutboundConnection> getOutboundConnections() {
        if (this.outboundConnections == null) {
            this.outboundConnections = new ConfigElementList<OutboundConnection>();
        }
        return this.outboundConnections;

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
        if (outboundConnections != null)
            buf.append("outboundConnection=\"" + outboundConnections + "\" ");
        if (enabledCiphers != null)
            buf.append("enabledCiphers=\"" + enabledCiphers + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
