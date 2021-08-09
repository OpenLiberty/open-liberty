/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
public class ApplicationMonitorElement extends ConfigElement {
    private Boolean enabled;
    private String dropins;

    public Boolean getEnabled() {
        return enabled;
    }

    public String getDropins() {
        return dropins;
    }

    @XmlAttribute(name = "enabled")
    public void setEnabled(Boolean b) {
        this.enabled = b;
    }

    @XmlAttribute(name = "dropins")
    public void setDropins(String s) {
        this.dropins = s;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("ApplicationMonitorElement{");
        if (enabled != null)
            buf.append("enabled=\"" + enabled + "\" ");
        if (dropins != null)
            buf.append("dropins=\"" + dropins + "\"");
        buf.append("}");
        return buf.toString();
    }
}
