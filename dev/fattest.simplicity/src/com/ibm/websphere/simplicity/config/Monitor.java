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
 * Represents the <monitor> element in server.xml
 */
public class Monitor extends ConfigElement {
    private String enableTraditionalPMI;
    private String filter;

    public String getEnableTraditionalPMI() {
        return enableTraditionalPMI;
    }

    public String getFilter() {
        return filter;
    }

    @XmlAttribute
    public void setEnableTraditionalPMI(String value) {
        enableTraditionalPMI = value;
    }

    @XmlAttribute
    public void setFilter(String value) {
        filter = value;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (enableTraditionalPMI != null)
            buf.append("enableTraditionalPMI=").append(enableTraditionalPMI).append(' ');
        if (filter != null)
            buf.append("filter=").append(filter).append(' ');
        buf.append('}');
        return buf.toString();
    }
}
