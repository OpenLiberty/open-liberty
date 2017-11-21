/*******************************************************************************
 * Copyright (c) 2012,2017 IBM Corporation and others.
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

    // attributes
    private String concurrencyPolicyRef;
    private String contextServiceRef;
    private String jndiName;
    private String longRunningPolicyRef;

    // nested elements
    @XmlElement(name = "concurrencyPolicy")
    private ConfigElementList<ConcurrencyPolicy> concurrencyPolicies;

    @XmlElement(name = "contextService")
    private ConfigElementList<ContextService> contextServices;

    @XmlElement(name = "longRunningPolicy")
    private ConfigElementList<ConcurrencyPolicy> longRunningPolicies;

    public String getConcurrencyPolicyRef() {
        return concurrencyPolicyRef;
    }

    public String getContextServiceRef() {
        return contextServiceRef;
    }

    public String getJndiName() {
        return jndiName;
    }

    public String getLongRunningPolicyRef() {
        return longRunningPolicyRef;
    }

    // only one nested element of each type is valid, but we must allow for testing invalid config, too

    public ConfigElementList<ConcurrencyPolicy> getConcurrencyPolicies() {
        return concurrencyPolicies == null ? (concurrencyPolicies = new ConfigElementList<ConcurrencyPolicy>()) : concurrencyPolicies;
    }

    public ConfigElementList<ContextService> getContextServices() {
        return contextServices == null ? (contextServices = new ConfigElementList<ContextService>()) : contextServices;
    }

    public ConfigElementList<ConcurrencyPolicy> getLongRunningPolicies() {
        return longRunningPolicies == null ? (longRunningPolicies = new ConfigElementList<ConcurrencyPolicy>()) : longRunningPolicies;
    }

    @XmlAttribute
    public void setConcurrencyPolicyRef(String concurrencyPolicyRef) {
        this.concurrencyPolicyRef = concurrencyPolicyRef;
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
    public void setLongRunningPolicyRef(String longRunningPolicyRef) {
        this.longRunningPolicyRef = longRunningPolicyRef;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (concurrencyPolicyRef != null)
            buf.append("concurrencyPolicyRef=").append(concurrencyPolicyRef).append(' ');
        if (contextServiceRef != null)
            buf.append("contextServiceRef=").append(contextServiceRef).append(' ');
        if (jndiName != null)
            buf.append("jndiName=").append(jndiName).append(' ');
        if (longRunningPolicyRef != null)
            buf.append("longRunningPolicyRef=").append(longRunningPolicyRef).append(' ');
        if (concurrencyPolicies != null)
            buf.append(concurrencyPolicies).append(' ');
        if (contextServices != null)
            buf.append(contextServices).append(' ');
        if (longRunningPolicies != null)
            buf.append(longRunningPolicies).append(' ');
        buf.append('}');
        return buf.toString();
    }
}
