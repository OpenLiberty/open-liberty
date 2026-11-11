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

/**
 * Represents the {@code <eventLogging>} top-level server configuration element.
 *
 * <pre>
 * &lt;eventLogging minDuration="0ms" maxEvents="200" sampleRate="1"
 *               includeContextInfo="true" eventTypes="all" logMode="entryExit"/&gt;
 * </pre>
 */
public class EventLoggingElement extends ConfigElement {

    private String logMode;
    private String minDuration;
    private Integer maxEvents;
    private Integer sampleRate;
    private Boolean includeContextInfo;
    private String eventTypes;

    public String getLogMode() {
        return logMode;
    }

    @XmlAttribute(name = "logMode")
    public void setLogMode(String logMode) {
        this.logMode = logMode;
    }

    public String getMinDuration() {
        return minDuration;
    }

    @XmlAttribute(name = "minDuration")
    public void setMinDuration(String minDuration) {
        this.minDuration = minDuration;
    }

    public Integer getMaxEvents() {
        return maxEvents;
    }

    @XmlAttribute(name = "maxEvents")
    public void setMaxEvents(Integer maxEvents) {
        this.maxEvents = maxEvents;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    @XmlAttribute(name = "sampleRate")
    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public Boolean getIncludeContextInfo() {
        return includeContextInfo;
    }

    @XmlAttribute(name = "includeContextInfo")
    public void setIncludeContextInfo(Boolean includeContextInfo) {
        this.includeContextInfo = includeContextInfo;
    }

    public String getEventTypes() {
        return eventTypes;
    }

    @XmlAttribute(name = "eventTypes")
    public void setEventTypes(String eventTypes) {
        this.eventTypes = eventTypes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EventLoggingElement {");
        if (logMode != null)
            sb.append("logMode=\"").append(logMode).append("\" ");
        if (minDuration != null)
            sb.append("minDuration=\"").append(minDuration).append("\" ");
        if (maxEvents != null)
            sb.append("maxEvents=\"").append(maxEvents).append("\" ");
        if (sampleRate != null)
            sb.append("sampleRate=\"").append(sampleRate).append("\" ");
        if (includeContextInfo != null)
            sb.append("includeContextInfo=\"").append(includeContextInfo).append("\" ");
        if (eventTypes != null)
            sb.append("eventTypes=\"").append(eventTypes).append("\" ");
        sb.append("}");
        return sb.toString();
    }
}
