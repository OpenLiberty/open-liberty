/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
 * Represents the <commonjTimerManager> element in server.xml
 */
public class CommonjTimerManager extends ConfigElement {

    // attributes
    private String contextServiceRef;
    private String jndiName;
    private String maxConcurrency;

    // nested elements
    @XmlElement(name = "contextService")
    private ConfigElementList<ContextService> contextServices;

    public String getContextServiceRef() {
        return contextServiceRef;
    }

    public String getJndiName() {
        return jndiName;
    }

    public String getMaxConcurrency() {
        return maxConcurrency;
    }

    // only one nested element of each type is valid, but we must allow for testing invalid config, too

    public ConfigElementList<ContextService> getContextServices() {
        return contextServices == null ? (contextServices = new ConfigElementList<ContextService>()) : contextServices;
    }

    @XmlAttribute
    public void setContextServiceRef(String contextServiceRef) {
        this.contextServiceRef = contextServiceRef;
    }

    @XmlAttribute
    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    @XmlAttribute
    public void setMaxConcurrency(String maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(getClass().getSimpleName()).append('{');
        if (getId() != null)
            b.append("id=").append(getId()).append(' ');
        if (contextServiceRef != null)
            b.append("contextServiceRef=").append(contextServiceRef).append(' ');
        if (jndiName != null)
            b.append("jndiName=").append(jndiName).append(' ');
        if (maxConcurrency != null)
            b.append("maxConcurrency=").append(maxConcurrency).append(' ');
        if (contextServices != null)
            b.append(contextServices).append(' ');
        b.append('}');
        return b.toString();
    }
}
