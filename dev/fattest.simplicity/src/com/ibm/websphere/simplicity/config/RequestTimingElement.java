/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Represents the {@code <requestTiming>} top-level server configuration element.
 *
 * <pre>
 * &lt;requestTiming slowRequestThreshold="10s" hungRequestThreshold="60s"
 *               enableThreadDumps="true" interruptHungRequests="false"
 *               includeContextInfo="true" sampleRate="1"&gt;
 *   &lt;ejbTiming requestType="async" .../&gt;
 * &lt;/requestTiming&gt;
 * </pre>
 */
public class RequestTimingElement extends ConfigElement {

    private String slowRequestThreshold;
    private String hungRequestThreshold;
    private Boolean enableThreadDumps;
    private Boolean interruptHungRequests;
    private Boolean includeContextInfo;
    private Integer sampleRate;
    private ConfigElementList<EJBTimingElement> ejbTimings;

    public String getSlowRequestThreshold() {
        return slowRequestThreshold;
    }

    @XmlAttribute(name = "slowRequestThreshold")
    public void setSlowRequestThreshold(String slowRequestThreshold) {
        this.slowRequestThreshold = slowRequestThreshold;
    }

    public String getHungRequestThreshold() {
        return hungRequestThreshold;
    }

    @XmlAttribute(name = "hungRequestThreshold")
    public void setHungRequestThreshold(String hungRequestThreshold) {
        this.hungRequestThreshold = hungRequestThreshold;
    }

    public Boolean getEnableThreadDumps() {
        return enableThreadDumps;
    }

    @XmlAttribute(name = "enableThreadDumps")
    public void setEnableThreadDumps(Boolean enableThreadDumps) {
        this.enableThreadDumps = enableThreadDumps;
    }

    public Boolean getInterruptHungRequests() {
        return interruptHungRequests;
    }

    @XmlAttribute(name = "interruptHungRequests")
    public void setInterruptHungRequests(Boolean interruptHungRequests) {
        this.interruptHungRequests = interruptHungRequests;
    }

    public Boolean getIncludeContextInfo() {
        return includeContextInfo;
    }

    @XmlAttribute(name = "includeContextInfo")
    public void setIncludeContextInfo(Boolean includeContextInfo) {
        this.includeContextInfo = includeContextInfo;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    @XmlAttribute(name = "sampleRate")
    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * @return the list of {@code <ejbTiming>} child elements, lazy-initialised
     */
    public ConfigElementList<EJBTimingElement> getEjbTimings() {
        if (ejbTimings == null) {
            ejbTimings = new ConfigElementList<EJBTimingElement>();
        }
        return ejbTimings;
    }

    @XmlElement(name = "ejbTiming")
    public void setEjbTimings(ConfigElementList<EJBTimingElement> ejbTimings) {
        this.ejbTimings = ejbTimings;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RequestTimingElement {");
        if (slowRequestThreshold != null)
            sb.append("slowRequestThreshold=\"").append(slowRequestThreshold).append("\" ");
        if (hungRequestThreshold != null)
            sb.append("hungRequestThreshold=\"").append(hungRequestThreshold).append("\" ");
        if (enableThreadDumps != null)
            sb.append("enableThreadDumps=\"").append(enableThreadDumps).append("\" ");
        if (interruptHungRequests != null)
            sb.append("interruptHungRequests=\"").append(interruptHungRequests).append("\" ");
        if (includeContextInfo != null)
            sb.append("includeContextInfo=\"").append(includeContextInfo).append("\" ");
        if (sampleRate != null)
            sb.append("sampleRate=\"").append(sampleRate).append("\" ");
        if (ejbTimings != null && !ejbTimings.isEmpty())
            sb.append("ejbTimings=").append(ejbTimings);
        sb.append("}");
        return sb.toString();
    }
}
