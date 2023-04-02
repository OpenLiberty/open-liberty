/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

/**
 * Lists wsAtomicTransaction properties.
 *
 */
public class WsAtomicTransaction extends ConfigElement {
    private Boolean sslEnabled;
    private String externalURLPrefix;

    @XmlAttribute(name = "sslEnabled")
    public void setSslEnabled(Boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }

    public Boolean getSslEnabled() {
        return this.sslEnabled;
    }

    @XmlAttribute(name = "externalURLPrefix")
    public void setExternalURLPrefix(String externalURLPrefix) {
        this.externalURLPrefix = externalURLPrefix;
    }

    public String getExternalURLPrefix() {
        return this.externalURLPrefix;
    }

    /**
     * Returns a String listing the properties and their values used on this
     * wsAtomicTransaction element.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("wsAtomicTransaction{");
        if (sslEnabled != null)
            buf.append("sslEnabled=\"" + sslEnabled + "\" ");
        if (externalURLPrefix != null)
            buf.append("externalURLPrefix=\"" + externalURLPrefix + "\" ");
        buf.append("}");
        return buf.toString();
    }
}