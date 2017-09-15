/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
 * Concurrent persistent PersistentExecutor element representation.
 */
public class PersistentExecutor extends ConfigElement {

    /** Config reference to the contextServiceRef attribute. */
    private String contextServiceRef;

    @XmlElement(name = "contextService")
    private ConfigElementList<ContextService> contextServices;

    /** Config reference to the taskStoreRef attribute. */
    private String taskStoreRef;

    /** Enable task execution attribute. */
    private String enableTaskExecution;

    /** Task execution retry limit attribute. */
    private String retryLimit;

    /** Task execution retry interval attribute. */
    private String retryInterval;

    private String initialControllerDelay;

    /** Initial server polling delay attribute. */
    private String initialPollDelay;

    /** JNDI Name attribute. */
    private String jndiName;

    /** Server polling interval attribute. */
    private String pollInterval;

    private String pollSize;

    /**
     * Sets the contextServiceRef attribute.
     * 
     * @param contextServiceRef The contextServiceRef attribute to be set.
     */
    @XmlAttribute
    public void setContextServiceRef(String contextServiceRef) {
        this.contextServiceRef = contextServiceRef;
    }

    /**
     * Returns the contextServiceRef attribute value.
     * 
     * @return The contextServiceRef attribute value.
     */
    public String getContextServiceRef() {
        return contextServiceRef;
    }

    public ConfigElementList<ContextService> getContextServices() {
        return contextServices == null ? (contextServices = new ConfigElementList<ContextService>()) : contextServices;
    }

    /**
     * Sets the taskStoreRef attribute.
     * 
     * @param taskStoreRef attribute to be set.
     */
    @XmlAttribute
    public void setTaskStoreRef(String taskStoreRef) {
        this.taskStoreRef = taskStoreRef;
    }

    /**
     * Returns the taskStoreRef attribute value.
     * 
     * @return The taskStoreRef attribute value.
     */
    public String getTaskStoreRef() {
        return taskStoreRef;
    }

    /**
     * Sets the enableTaskExecution attribute value.
     * 
     * @param schema The enableTaskExecution attribute value.
     */
    @XmlAttribute
    public void setEnableTaskExecution(String enableTaskExecution) {
        this.enableTaskExecution = enableTaskExecution;
    }

    /**
     * Returns the enableTaskExecution attribute value.
     * 
     * @return The enableTaskExecution attribute value.
     */
    public String getEnableTaskExecution() {
        return enableTaskExecution;
    }

    /**
     * Sets the retryLimit attribute value.
     * 
     * @param schema The retryLimit attribute value.
     */
    @XmlAttribute
    public void setRetryLimit(String retryLimit) {
        this.retryLimit = retryLimit;
    }

    /**
     * Returns the retryLimit attribute value.
     * 
     * @return The retryLimit attribute value.
     */
    public String getRetryLimit() {
        return retryLimit;
    }

    /**
     * Sets the retryInterval attribute value.
     * 
     * @param retryInterval The retryInterval attribute value.
     */
    @XmlAttribute
    public void setRetryInterval(String retryInterval) {
        this.retryInterval = retryInterval;
    }

    /**
     * Returns the retryInterval attribute value
     * 
     * @return The retryInterval attribute value
     */
    public String getRetryInterval() {
        return retryInterval;
    }

    @XmlAttribute
    public void setInitialControllerDelay(String initialControllerDelay) {
        this.initialControllerDelay = initialControllerDelay;
    }

    public String getInitialControllerDelay() {
        return initialControllerDelay;
    }

    /**
     * Sets the initialPollDelay attribute value.
     * 
     * @param schema The initialPollDelay attribute value.
     */
    @XmlAttribute
    public void setInitialPollDelay(String initialPollDelay) {
        this.initialPollDelay = initialPollDelay;
    }

    /**
     * Returns the initialPollDelay attribute value.
     * 
     * @return The initialPollDelay attribute value.
     */
    public String getInitialPollDelay() {
        return initialPollDelay;
    }

    /**
     * Sets the jndiName attribute value.
     * 
     * @param schema The jndiName attribute value.
     */
    @XmlAttribute
    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    /**
     * Returns the jndiName attribute value.
     * 
     * @return The jndiName attribute value.
     */
    public String getJndiName() {
        return jndiName;
    }

    /**
     * Sets the pollInterval attribute value.
     * 
     * @param schema The pollInterval attribute value.
     */
    @XmlAttribute
    public void setPollInterval(String pollInterval) {
        this.pollInterval = pollInterval;
    }

    /**
     * Returns the pollInterval attribute value.
     * 
     * @return The pollInterval attribute value.
     */
    public String getPollInterval() {
        return pollInterval;
    }

    @XmlAttribute
    public void setPollSize(String pollSize) {
        this.pollSize = pollSize;
    }

    public String getPollSize() {
        return pollSize;
    }

    /**
     * Returns a String representation of this object.
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("PersistentExecutor {");
        if (super.getId() != null)
            buf.append("id=\"" + super.getId() + "\", ");
        if (enableTaskExecution != null)
            buf.append("enableTaskExecution=\"" + enableTaskExecution + "\", ");
        if (retryLimit != null)
            buf.append("retryLimit=\"" + retryLimit + "\", ");
        if (retryInterval != null)
            buf.append("retryInterval=\"" + retryInterval + "\", ");
        if (initialControllerDelay != null)
            buf.append("initialControllerDelay=\"" + initialControllerDelay + "\", ");
        if (initialPollDelay != null)
            buf.append("initialPollDelay=\"" + initialPollDelay + "\", ");
        if (jndiName != null)
            buf.append("jndiName=\"" + jndiName + "\", ");
        if (pollInterval != null)
            buf.append("pollInterval=\"" + pollInterval + "\", ");
        if (pollSize != null)
            buf.append("pollSize=\"" + pollSize + "\", ");
        if (contextServiceRef != null)
            buf.append("contextServiceRef=\"" + contextServiceRef + "\", ");
        if (taskStoreRef != null)
            buf.append("taskStoreRef=\"" + taskStoreRef + "\", ");
        if (contextServices != null)
            buf.append(contextServices).append(' ');
        buf.append("}");
        return buf.toString();
    }
}
