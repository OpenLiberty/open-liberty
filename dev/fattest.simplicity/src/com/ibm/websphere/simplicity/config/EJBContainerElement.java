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
    private String disableShortDefaultBindings;
    private Boolean bindToJavaGlobal;
    private Boolean bindToServerRoot;

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

    public String getDisableShortDefaultBindings() {
        return disableShortDefaultBindings;
    }

    @XmlElement(name = "disableShortDefaultBindings")
    public void setDisableShortDefaultBindings(String disableShortDefaultBindings) {
        this.disableShortDefaultBindings = disableShortDefaultBindings;
    }

    public Boolean getBindToJavaGlobal() {
        return bindToJavaGlobal;
    }

    @XmlAttribute(name = "bindToJavaGlobal")
    public void setBindToJavaGlobal(Boolean bindToJavaGlobal) {
        this.bindToJavaGlobal = bindToJavaGlobal;
    }

    public Boolean getBindToServerRoot() {
        return bindToServerRoot;
    }

    @XmlAttribute(name = "bindToServerRoot")
    public void setBindToServerRoot(Boolean bindToServerRoot) {
        this.bindToServerRoot = bindToServerRoot;
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
        if (disableShortDefaultBindings != null)
            buf.append("disableShortDefaultBindings=\"" + disableShortDefaultBindings + "\" ");
        if (bindToJavaGlobal != null)
            buf.append("bindToJavaGlobal=\"" + bindToJavaGlobal + "\" ");
        if (bindToServerRoot != null)
            buf.append("bindToServerRoot=\"" + bindToServerRoot + "\" ");
        if (asynchronous != null)
            buf.append(", " + asynchronous);
        if (timerService != null)
            buf.append(", " + timerService);

        buf.append("}");
        return buf.toString();
    }
}
