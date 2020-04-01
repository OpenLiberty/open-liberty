/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
 *
 */
public class EJBContainerElement extends ConfigElement {

    private Integer cacheSize;
    private Long poolCleanupInterval;
    private Long cacheCleanupInterval;
    private Boolean startEJBsAtAppStart;
    private EJBAsynchronousElement asynchronous;
    private EJBTimerServiceElement timerService;
    private String customBindingsOnError;

    public Integer getCacheSize() {
        return cacheSize;
    }

    @XmlAttribute(name = "cacheSize")
    public void setCacheSize(Integer cacheSize) {
        this.cacheSize = cacheSize;
    }

    public Long getPoolCleanupInterval() {
        return poolCleanupInterval;
    }

    @XmlAttribute(name = "poolCleanupInterval")
    public void setPoolCleanupInterval(Long poolCleanupInterval) {
        this.poolCleanupInterval = poolCleanupInterval;
    }

    public Long getCacheCleanupInterval() {
        return cacheCleanupInterval;
    }

    @XmlAttribute(name = "cacheCleanupInterval")
    public void setCacheCleanupInterval(Long cacheCleanupInterval) {
        this.cacheCleanupInterval = cacheCleanupInterval;
    }

    public Boolean getStartEJBsAtAppStart() {
        return startEJBsAtAppStart;
    }

    @XmlAttribute(name = "startEJBsAtAppStart")
    public void setStartEJBsAtAppStart(Boolean startEJBsAtAppStart) {
        this.startEJBsAtAppStart = startEJBsAtAppStart;
    }

    public EJBAsynchronousElement getAsynchronous() {
        return asynchronous;
    }

    @XmlElement(name = "asynchronous")
    public void setAsynchronous(EJBAsynchronousElement asynchronous) {
        this.asynchronous = asynchronous;
    }

    public EJBTimerServiceElement getTimerService() {
        return timerService;
    }

    @XmlElement(name = "timerService")
    public void setTimerService(EJBTimerServiceElement timerService) {
        this.timerService = timerService;
    }

    public String getCustomBindingsOnError() {
        return customBindingsOnError;
    }

    @XmlElement(name = "customBindingsOnError")
    public void setCustomBindingsOnError(String customBindingsOnError) {
        this.customBindingsOnError = customBindingsOnError;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("EJBContainerElement {");

        if (cacheSize != null)
            buf.append("cacheSize=\"" + cacheSize + "\" ");
        if (poolCleanupInterval != null)
            buf.append("poolCleanupInterval=\"" + poolCleanupInterval + "\" ");
        if (cacheCleanupInterval != null)
            buf.append("cacheCleanupInterval=\"" + cacheCleanupInterval + "\" ");
        if (customBindingsOnError != null)
            buf.append("customBindingsOnError=\"" + customBindingsOnError + "\" ");
        if (asynchronous != null)
            buf.append(", " + asynchronous);
        if (timerService != null)
            buf.append(", " + timerService);

        buf.append("}");
        return buf.toString();
    }
}
