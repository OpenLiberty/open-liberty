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
 * Represents the <managedThreadFactory> element in server.xml
 */
public class ManagedThreadFactory extends ConfigElement {

    private String contextServiceRef;
    private String createDaemonThreads;
    private String defaultPriority;
    private String jndiName;
    private String maxPriority;

    @XmlElement(name = "contextService")
    private ConfigElementList<ContextService> contextServices;

    public String getContextServiceRef() {
        return contextServiceRef;
    }

    public String getCreateDaemonThreads() {
        return createDaemonThreads;
    }

    public String getDefaultPriority() {
        return defaultPriority;
    }

    public String getJndiName() {
        return jndiName;
    }

    public String getMaxPriority() {
        return maxPriority;
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
    public void setCreateDaemonThreads(String createDaemonThreads) {
        this.createDaemonThreads = createDaemonThreads;
    }

    @XmlAttribute
    public void setDefaultPriority(String defaultPriority) {
        this.defaultPriority = defaultPriority;
    }

    @XmlAttribute
    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    @XmlAttribute
    public void setMaxPriority(String maxPriority) {
        this.maxPriority = maxPriority;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (contextServiceRef != null)
            buf.append("contextServiceRef=").append(contextServiceRef).append(' ');
        if (createDaemonThreads != null)
            buf.append("createDaemonThreads=").append(createDaemonThreads).append(' ');
        if (defaultPriority != null)
            buf.append("defaultPriority=").append(defaultPriority).append(' ');
        if (jndiName != null)
            buf.append("jndiName=").append(jndiName).append(' ');
        if (maxPriority != null)
            buf.append("maxPriority=").append(maxPriority).append(' ');
        if (contextServices != null)
            buf.append(contextServices).append(' ');
        buf.append('}');
        return buf.toString();
    }
}
