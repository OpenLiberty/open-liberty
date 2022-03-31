/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
 * Represents the <cdi> element in server.xml
 */
public class Cdi extends ConfigElement {

    private Boolean enableImplicitBeanArchives;

    private Boolean emptyBeansXMLExplicitBeanArchive;

    @XmlAttribute
    public Boolean getEnableImplicitBeanArchives() {
        return enableImplicitBeanArchives;
    }

    public void setEnableImplicitBeanArchives(Boolean enableImplicitBeanArchives) {
        this.enableImplicitBeanArchives = enableImplicitBeanArchives;
    }

    @XmlAttribute
    public Boolean getEmptyBeansXMLExplicitBeanArchive() {
        return emptyBeansXMLExplicitBeanArchive;
    }

    public void setEmptyBeansXMLExplicitBeanArchive(Boolean emptyBeansXMLExplicitBeanArchive) {
        this.emptyBeansXMLExplicitBeanArchive = emptyBeansXMLExplicitBeanArchive;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // attributes
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (getEnableImplicitBeanArchives() != null)
            buf.append("enableImplicitBeanArchives=").append(getEnableImplicitBeanArchives()).append(' ');
        if (getEnableImplicitBeanArchives() != null)
            buf.append("emptyBeansXMLExplicitBeanArchive=").append(getEmptyBeansXMLExplicitBeanArchive()).append(' ');
        buf.append('}');
        return buf.toString();
    }
}
