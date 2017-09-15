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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Represents the <managedExecutorService> element in server.xml
 */
public class ManagedExecutorService extends ConfigElement {

    private String contextServiceRef;
    private String jndiName;

    @XmlElement(name = "contextService")
    private ConfigElementList<ContextService> contextServices;

    public String getContextServiceRef() {
        return contextServiceRef;
    }

    public String getJndiName() {
        return jndiName;
    }

    // only one nested <ContextService> is valid, but we must allow for testing invalid config, too
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

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (contextServiceRef != null)
            buf.append("contextServiceRef=").append(contextServiceRef).append(' ');
        if (jndiName != null)
            buf.append("jndiName=").append(jndiName).append(' ');
        if (contextServices != null)
            buf.append(contextServices).append(' ');
        buf.append('}');
        return buf.toString();
    }
}
