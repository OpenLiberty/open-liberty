/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config.context;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Represents the <zosWLMContext> element which can be nested under <contextService>
 */
public class ZOSWLMContext extends ConfigElement {
    private String daemonTransactionClass;
    private String defaultTransactionClass;
    private String wlm;

    public String getDaemonTransactionClass() {
        return daemonTransactionClass;
    }

    public String getDefaultTransactionClass() {
        return defaultTransactionClass;
    }

    public String getWLM() {
        return wlm;
    }

    @XmlAttribute(name = "daemonTransactionClass")
    public void setDaemonTransactionClass(String value) {
        daemonTransactionClass = value;
    }

    @XmlAttribute(name = "defaultTransactionClass")
    public void setDefaultTransactionClass(String value) {
        defaultTransactionClass = value;
    }

    @XmlAttribute(name = "wlm")
    public void setWLM(String value) {
        wlm = value;
    }

    /**
     * Returns a string containing a list of the properties and their values.
     * 
     * @return String representing the data
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("zosWLMContext{");
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (wlm != null)
            buf.append("wlm=").append(wlm).append(' ');
        if (daemonTransactionClass != null)
            buf.append("daemonTransactionClass=").append(daemonTransactionClass).append(' ');
        if (defaultTransactionClass != null)
            buf.append("defaultTransactionClass=").append(defaultTransactionClass).append(' ');
        buf.append("}");
        return buf.toString();
    }
}