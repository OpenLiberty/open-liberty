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
 * Defines Ssl options for channel framework
 *
 */
public class SslOptions extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_SSL_REF = "sslRef";
    private String sslRef;

    public String getSslRef() {
        return this.sslRef;
    }

    public void setSslRef(String sslRef) {
        this.sslRef = sslRef;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("SslOptions{");
        buf.append("id=\"" + this.getId() + "\" ");
        if (sslRef != null)
            buf.append("sslRef=\"" + sslRef + "\" ");

        buf.append("}");
        return buf.toString();
    }
}
