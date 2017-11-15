/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 * Represents the <concurrencyPolicy> element in server.xml
 */
public class ConcurrencyPolicy extends ConfigElement {
    // attributes
    private String expedite;
    private String max;
    private String maxPolicy;
    private String maxQueueSize;
    private String maxWaitForEnqueue;
    private String runIfQueueFull;
    private String startTimeout;

    public String getExpedite() {
        return expedite;
    }

    public String getMax() {
        return max;
    }

    public String getMaxPolicy() {
        return maxPolicy;
    }

    public String getMaxQueueSize() {
        return maxQueueSize;
    }

    public String getMaxWaitForEnqueue() {
        return maxWaitForEnqueue;
    }

    public String runIfQueueFull() {
        return runIfQueueFull;
    }

    public String startTimeout() {
        return startTimeout;
    }

    // setters for attributes

    @XmlAttribute
    public void setExpedite(String value) {
        expedite = value;
    }

    @XmlAttribute
    public void setMax(String value) {
        max = value;
    }

    @XmlAttribute
    public void setMaxPolicy(String value) {
        maxPolicy = value;
    }

    @XmlAttribute
    public void setMaxQueueSize(String value) {
        maxQueueSize = value;
    }

    @XmlAttribute
    public void setMaxWaitForEnqueue(String value) {
        maxWaitForEnqueue = value;
    }

    @XmlAttribute
    public void setRunIfQueueFull(String value) {
        runIfQueueFull = value;
    }

    @XmlAttribute
    public void setStartTimeout(String value) {
        startTimeout = value;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // attributes
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (expedite != null)
            buf.append("expedite=").append(expedite).append(' ');
        if (max != null)
            buf.append("max=").append(max).append(' ');
        if (maxPolicy != null)
            buf.append("maxPolicy=").append(maxPolicy).append(' ');
        if (maxQueueSize != null)
            buf.append("maxQueueSize=").append(maxQueueSize).append(' ');
        if (maxWaitForEnqueue != null)
            buf.append("maxWaitForEnqueue=").append(maxWaitForEnqueue).append(' ');
        if (runIfQueueFull != null)
            buf.append("runIfQueueFull=").append(runIfQueueFull).append(' ');
        if (startTimeout != null)
            buf.append("startTimeout=").append(startTimeout).append(' ');
        // nested elements - none
        buf.append('}');
        return buf.toString();
    }
}
