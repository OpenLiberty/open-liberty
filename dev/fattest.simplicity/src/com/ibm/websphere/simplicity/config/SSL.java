/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * ssl element is defined here:<br>
 * /com.ibm.ws.ssl/resources/OSGI-INF/metatype/metatype.xml
 */
public class SSL extends ConfigElement {

    private String sslProtocol;
    private String keyStoreRef;
    private String trustStoreRef;
    private Boolean clientAuthentication;
    private Boolean clientAuthenticationSupported;

    @XmlElement(name = "outboundConnection")
    private ConfigElementList<OutboundConnection> outboundConnections;

    /**
     * @return the keyStoreRef
     */
    public String getKeyStoreRef() {
        return keyStoreRef;
    }

    /**
     * @param keyStoreRef the keyStoreRef to set
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
     * @param trustStoreRef the trustStoreRef to set
     */
    @XmlAttribute(name = "trustStoreRef")
    public void setTrustStoreRef(String trustStoreRef) {
        this.trustStoreRef = trustStoreRef;
    }

    @XmlAttribute(name = "clientAuthentication")
    public void setClientAuthentication(Boolean clientAuthentication) {
        this.clientAuthentication = clientAuthentication;
    }

    public Boolean getClientAuthentication() {
        return this.clientAuthentication;
    }

    @XmlAttribute(name = "clientAuthenticationSupported")
    public void setClientAuthenticationSupported(Boolean clientAuthenticationSupported) {
        this.clientAuthenticationSupported = clientAuthenticationSupported;
    }

    public Boolean getClientAuthenticationSupported() {
        return this.clientAuthenticationSupported;
    }

    /**
     * @return the sslProtocol
     */
    public String getSslProtocol() {
        return sslProtocol;
    }

    /**
     * @param sslProtocol the sslProtocol to set
     */
    @XmlAttribute(name = "sslProtocol")
    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    /**
     * @param outboundConnection the outboundConnection to set
     */
    public void setOutboundConnection(ConfigElementList<OutboundConnection> connections) {
        this.outboundConnections = connections;
    }

    /**
     * @param outboundConnection the outboundConnection to set
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
        if (keyStoreRef != null)
            buf.append("keyStoreRef=\"" + keyStoreRef + "\" ");
        if (trustStoreRef != null)
            buf.append("trustStoreRef=\"" + trustStoreRef + "\" ");
        if (clientAuthentication != null)
            buf.append("clientAuthentication=\"" + clientAuthentication + "\" ");
        if (clientAuthenticationSupported != null)
            buf.append("clientAuthenticationSupported=\"" + clientAuthenticationSupported + "\" ");
        if (sslProtocol != null)
            buf.append("sslProtocol=\"" + sslProtocol + "\" ");
        if (outboundConnections != null)
            buf.append("outboundConnection=\"" + outboundConnections + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
