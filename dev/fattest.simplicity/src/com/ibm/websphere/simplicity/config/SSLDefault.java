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
 * sslDefault element is defined here:<br>
 * /com.ibm.ws.ssl/resources/OSGI-INF/metatype/metatype.xml
 */
public class SSLDefault extends ConfigElement {

    private String sslRef;
    private String outboundSSLRef;

    /**
     * @return the sslRef
     */
    public String getSslRef() {
        return sslRef;
    }

    /**
     * @param sslRef the sslRef to set
     */
    @XmlAttribute(name = "sslRef")
    public void setSslRef(String sslRef) {
        this.sslRef = sslRef;
    }

    /**
     * @return the outboundSSLRef
     */
    public String getOutboundSSLRef() {
        return outboundSSLRef;
    }

    /**
     * @param outboundSSLRef the outboundSSLRef to set
     */
    @XmlAttribute(name = "outboundSSLRef")
    public void setOutboundSSLRef(String outboundSSLRef) {
        this.outboundSSLRef = outboundSSLRef;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("SSLDefault{");
        if (sslRef != null)
            buf.append("sslRef=\"" + sslRef + "\" ");
        if (outboundSSLRef != null)
            buf.append("outboundSSLRef=\"" + outboundSSLRef + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
