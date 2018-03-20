/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 * Models the httpSessionCache configuration element
 */
public class HttpSessionCache extends ConfigElement {
    // attributes - all values are String, which allows for testing of invalid values as well as valid ones
    private String libraryRef;
    private String scheduleInvalidationFirstHour;
    private String scheduleInvalidationSecondHour;
    private String writeContents;
    private String writeFrequency;
    private String writeInterval;
    private String uri;
    private String useInvalidatedId;

    // nested element names
    @XmlElement(name = "library")
    private ConfigElementList<Library> libraries;

    @XmlElement(name = "properties")
    private ConfigElementList<HttpSessionCacheProperties> properties;

    public ConfigElementList<Library> getLibraries() {
        return libraries == null ? (libraries = new ConfigElementList<Library>()) : libraries;
    }

    public String getLibraryRef() {
        return libraryRef;
    }

    public ConfigElementList<HttpSessionCacheProperties> getProperties() {
        return properties == null ? (properties = new ConfigElementList<HttpSessionCacheProperties>()) : properties;
    }

    public String getScheduleInvalidationFirstHour() {
        return this.scheduleInvalidationFirstHour;
    }

    public String getScheduleInvalidationSecondHour() {
        return this.scheduleInvalidationSecondHour;
    }

    public String getUri() {
        return uri;
    }

    public String getUseInvalidatedId() {
        return useInvalidatedId;
    }

    public String getWriteContents() {
        return this.writeContents;
    }

    public String getWriteFrequency() {
        return this.writeFrequency;
    }

    public String getWriteInterval() {
        return this.writeInterval;
    }

    @XmlAttribute
    public void setLibraryRef(String libraryRef) {
        this.libraryRef = libraryRef;
    }

    @XmlAttribute
    public void setScheduleInvalidationFirstHour(String scheduleInvalidationFirstHour) {
        this.scheduleInvalidationFirstHour = scheduleInvalidationFirstHour;
    }

    @XmlAttribute
    public void setScheduleInvalidationSecondHour(String scheduleInvalidationSecondHour) {
        this.scheduleInvalidationSecondHour = scheduleInvalidationSecondHour;
    }

    @XmlAttribute
    public void setUri(String uri) {
        this.uri = uri;
    }

    @XmlAttribute
    public void setUseInvalidatedId(String useInvalidatedId) {
        this.useInvalidatedId = useInvalidatedId;
    }

    @XmlAttribute
    public void setWriteContents(String writeContents) {
        this.writeContents = writeContents;
    }

    @XmlAttribute
    public void setWriteFrequency(String writeFrequency) {
        this.writeFrequency = writeFrequency;
    }

    @XmlAttribute
    public void setWriteInterval(String writeInterval) {
        this.writeInterval = writeInterval;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (libraryRef != null)
            buf.append("libraryRef=").append(libraryRef).append(' ');
        if (scheduleInvalidationFirstHour != null)
            buf.append("scheduleInvalidationFirstHour=").append(scheduleInvalidationFirstHour).append(' ');
        if (scheduleInvalidationSecondHour != null)
            buf.append("scheduleInvalidationSecondHour=").append(scheduleInvalidationSecondHour).append(' ');
        if (uri != null)
            buf.append("uri=").append(uri).append(' ');
        if (useInvalidatedId != null)
            buf.append("useInvalidatedId=").append(useInvalidatedId).append(' ');
        if (writeContents != null)
            buf.append("writeContents=").append(writeContents).append(' ');
        if (writeFrequency != null)
            buf.append("writeFrequency=").append(writeFrequency).append(' ');
        if (writeInterval != null)
            buf.append("writeInterval=").append(writeInterval).append(' ');
        if (libraries != null)
            buf.append(libraries).append(' ');
        if (properties != null)
            buf.append(properties).append(' ');
        buf.append('}');
        return buf.toString();
    }
}
