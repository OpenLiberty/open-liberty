/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Defines Http Options for channel framework
 */
public class HttpOptions extends ConfigElement {

    private Boolean ignoreWriteAfterCommit;
    private Integer messageSizeLimit;
    private String websocketBufferSize;
    private String readTimeout;
    private String writeTimeout;

    public Integer getMessageSizeLimit() {
        return this.messageSizeLimit;
    }
    
    @XmlAttribute
    public void setMessageSizeLimit(Integer messageSizeLimit) {
        this.messageSizeLimit = messageSizeLimit;
    }

    public Boolean isIgnoreWriteAfterCommit() {
        return this.ignoreWriteAfterCommit;
    }

    @XmlAttribute
    public void setIgnoreWriteAfterCommit(Boolean ignoreWriteAfterCommit) {
        this.ignoreWriteAfterCommit = ignoreWriteAfterCommit;
    }

    public String isWebsocketBufferSize() {
        return this.websocketBufferSize;
    }

    @XmlAttribute
    public void setWebsocketBufferSize(String websocketBufferSize) {
        this.websocketBufferSize = websocketBufferSize;
    }

    public String getReadTimeout() {
        return this.readTimeout;
    }

    @XmlAttribute
    public void setReadTimeout(String readTimeout) {
        this.readTimeout = readTimeout;
    }

    public String getWriteTimeout() {
        return this.writeTimeout;
    }

    @XmlAttribute
    public void setWriteTimeout(String writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("httpOptions{");
        if (getId() != null)
            buf.append("id=\"" + this.getId() + "\" ");
        if (ignoreWriteAfterCommit != null)
            buf.append("ignoreWriteAfterCommit=\"" + ignoreWriteAfterCommit + "\" ");
        if (messageSizeLimit != null)
            buf.append("messageSizeLimit=\"" + messageSizeLimit + "\" ");
        if (websocketBufferSize != null)
            buf.append("websocketBufferSize=\"" + websocketBufferSize + "\" ");
        if (readTimeout != null)
            buf.append("readTimeout=\"" + readTimeout + "\" ");
        if (writeTimeout != null)
            buf.append("writeTimeout=\"" + writeTimeout + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
