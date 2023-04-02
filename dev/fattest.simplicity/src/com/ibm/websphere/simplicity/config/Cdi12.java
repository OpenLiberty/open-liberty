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
 * Represents the <cdi12> element in server.xml
 */
public class Cdi12 extends ConfigElement {

    private Boolean enableImplicitBeanArchives;

    @XmlAttribute
    public Boolean getEnableImplicitBeanArchives() {
        return enableImplicitBeanArchives;
    }

    public void setEnableImplicitBeanArchives(Boolean enableImplicitBeanArchives) {
        this.enableImplicitBeanArchives = enableImplicitBeanArchives;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // attributes
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (getEnableImplicitBeanArchives() != null)
            buf.append("enableImplicitBeanArchives=").append(getEnableImplicitBeanArchives()).append(' ');
        buf.append('}');
        return buf.toString();
    }
}
