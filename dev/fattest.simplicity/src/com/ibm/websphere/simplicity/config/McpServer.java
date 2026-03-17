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
 * Represents the <mcpServer> child element of an application in a server.xml
 */
public class McpServer extends ConfigElement {

    private String stateless;
    private String moduleName;
    private String path;

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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("McpServer {");
        if (stateless != null)
            builder.append("stateless=\"" + stateless + "\" ");
        if (moduleName != null)
            builder.append("moduleName=\"" + moduleName + "\" ");
        if (path != null)
            builder.append("path=\"" + path + "\" ");
        builder.append("}");
        return builder.toString();
    }

    @Override
    public McpServer clone() throws CloneNotSupportedException {
        McpServer clone = (McpServer) super.clone();
        return clone;
    }

}
