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
 * <li>ldapRegistry --> ldapCache --> searchResultsCache</li>
 * </ul>
 */
public class SearchResultsCache extends ConfigElement {

    private Boolean enabled;
    private Integer resultsSizeLimit;
    private Integer size;
    private String timeout;

    public SearchResultsCache() {}

    public SearchResultsCache(Boolean enabled, Integer size, Integer resultsSizeLimit, String timeout) {
        this.enabled = enabled;
        this.size = size;
        this.resultsSizeLimit = resultsSizeLimit;
        this.timeout = timeout;
    }

    /**
     * @return the enabled
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * @return the resultsSizeLimit
     */
    public Integer getResultsSizeLimit() {
        return resultsSizeLimit;
    }

    /**
     * @return the size
     */
    public Integer getSize() {
        return size;
    }

    /**
     * @return the timeout
     */
    public String getTimeout() {
        return timeout;
    }

    /**
     * @param enabled the enabled to set
     */
    @XmlAttribute(name = "enabled")
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @param resultsSizeLimit the resultsSizeLimit to set
     */
    @XmlAttribute(name = "resultsSizeLimit")
    public void setResultsSizeLimit(Integer resultsSizeLimit) {
        this.resultsSizeLimit = resultsSizeLimit;
    }

    /**
     * @param size the size to set
     */
    @XmlAttribute(name = "size")
    public void setSize(Integer size) {
        this.size = size;
    }

    /**
     * @param timeout the timeout to set
     */
    @XmlAttribute(name = "timeout")
    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (enabled != null) {
            sb.append("enabled=\"").append(enabled).append("\" ");;
        }
        if (resultsSizeLimit != null) {
            sb.append("resultsSizeLimit=\"").append(resultsSizeLimit).append("\" ");;
        }
        if (size != null) {
            sb.append("size=\"").append(size).append("\" ");;
        }
        if (timeout != null) {
            sb.append("timeout=\"").append(timeout).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}
