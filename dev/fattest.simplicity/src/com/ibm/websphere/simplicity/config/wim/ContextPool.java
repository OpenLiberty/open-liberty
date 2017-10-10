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

package com.ibm.websphere.simplicity.config.wim;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>ldapRegistry --> contextPool</li>
 * </ul>
 */
public class ContextPool extends ConfigElement {

    private Boolean enabled;
    private Integer initialSize;
    private Integer maxSize;
    private Integer preferredSize;
    private String timeout;
    private String waitTime;

    public ContextPool() {}

    public ContextPool(Boolean enabled, Integer initialSize, Integer maxSize, Integer preferredSize, String timeout, String waitTime) {
        this.enabled = enabled;
        this.initialSize = initialSize;
        this.maxSize = maxSize;
        this.preferredSize = preferredSize;
        this.timeout = timeout;
        this.waitTime = waitTime;
    }

    /**
     * @return the enabled
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * @return the initialSize
     */
    public Integer getInitialSize() {
        return initialSize;
    }

    /**
     * @return the maxSize
     */
    public Integer getMaxSize() {
        return maxSize;
    }

    /**
     * @return the preferredSize
     */
    public Integer getPreferredSize() {
        return preferredSize;
    }

    /**
     * @return the timeout
     */
    public String getTimeout() {
        return timeout;
    }

    /**
     * @return the waitTime
     */
    public String getWaitTime() {
        return waitTime;
    }

    /**
     * @param enabled the enabled to set
     */
    @XmlAttribute(name = "enabled")
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @param initialSize the initialSize to set
     */
    @XmlAttribute(name = "initialSize")
    public void setInitialSize(Integer initialSize) {
        this.initialSize = initialSize;
    }

    /**
     * @param maxSize the maxSize to set
     */
    @XmlAttribute(name = "maxSize")
    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * @param preferredSize the preferredSize to set
     */
    @XmlAttribute(name = "preferredSize")
    public void setPreferredSize(Integer preferredSize) {
        this.preferredSize = preferredSize;
    }

    /**
     * @param timeout the timeout to set
     */
    @XmlAttribute(name = "timeout")
    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    /**
     * @param waitTime the waitTime to set
     */
    @XmlAttribute(name = "waitTime")
    public void setWaitTime(String waitTime) {
        this.waitTime = waitTime;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (enabled != null) {
            sb.append("enabled=\"").append(enabled).append("\" ");
        }
        if (initialSize != null) {
            sb.append("initialSize=\"").append(initialSize).append("\" ");;
        }
        if (maxSize != null) {
            sb.append("maxSize=").append(maxSize).append("\" ");;
        }
        if (preferredSize != null) {
            sb.append("preferredSize=\"").append(preferredSize).append("\" ");;
        }
        if (timeout != null) {
            sb.append("timeout=\"").append(timeout).append("\" ");;
        }
        if (waitTime != null) {
            sb.append("waitTime=\"").append(waitTime).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}