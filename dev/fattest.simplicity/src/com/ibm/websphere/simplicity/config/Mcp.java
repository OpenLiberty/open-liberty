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
 * Represents the {@code <mcp>} child element of an application in a server.xml
 */
public class Mcp extends ConfigElement {

    private String stateless;
    private String moduleName;
    private String path;
    private String asyncTimeout;
    private String minProtocolVersion;
    private String maxProtocolVersion;

    /**
     * @return whether the MCP server is stateless
     */
    public String getStateless() {
        return stateless;
    }

    @XmlAttribute
    public void setStateless(String stateless) {
        this.stateless = stateless;
    }

    /**
     * @return the moduleName for the MCP server
     */
    public String getModuleName() {
        return moduleName;
    }

    @XmlAttribute
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * @return the path for the MCP endpoint
     */
    public String getPath() {
        return path;
    }

    @XmlAttribute
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * @return the async timeout for MCP tool calls
     */
    public String getAsyncTimeout() {
        return asyncTimeout;
    }

    @XmlAttribute
    public void setAsyncTimeout(String asyncTimeout) {
        this.asyncTimeout = asyncTimeout;
    }

    /**
     * @return the minimum accepted MCP protocol version
     */
    public String getMinProtocolVersion() {
        return minProtocolVersion;
    }

    @XmlAttribute
    public void setMinProtocolVersion(String minProtocolVersion) {
        this.minProtocolVersion = minProtocolVersion;
    }

    /**
     * @return the maximum accepted MCP protocol version
     */
    public String getMaxProtocolVersion() {
        return maxProtocolVersion;
    }

    @XmlAttribute
    public void setMaxProtocolVersion(String maxProtocolVersion) {
        this.maxProtocolVersion = maxProtocolVersion;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Mcp {");
        if (stateless != null)
            builder.append("stateless=\"" + stateless + "\" ");
        if (moduleName != null)
            builder.append("moduleName=\"" + moduleName + "\" ");
        if (path != null)
            builder.append("path=\"" + path + "\" ");
        if (asyncTimeout != null)
            builder.append("asyncTimeout=\"" + asyncTimeout + "\" ");
        if (minProtocolVersion != null)
            builder.append("minProtocolVersion=\"" + minProtocolVersion + "\" ");
        if (maxProtocolVersion != null)
            builder.append("maxProtocolVersion=\"" + maxProtocolVersion + "\" ");
        builder.append("}");
        return builder.toString();
    }

    @Override
    public Mcp clone() throws CloneNotSupportedException {
        Mcp clone = (Mcp) super.clone();
        return clone;
    }

}
