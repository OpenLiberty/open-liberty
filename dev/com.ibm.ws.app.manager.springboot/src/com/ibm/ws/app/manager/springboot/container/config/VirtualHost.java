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
package com.ibm.ws.app.manager.springboot.container.config;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

/**
 * Defines an HTTP endpoint (host/port mapping)
 */
public class VirtualHost extends ConfigElement {

    @XmlElement(name = "hostAlias")
    private Set<String> hostAliases;
    private String allowFromEndpoint;

    public Set<String> getHostAliases() {
        if (hostAliases == null) {
            hostAliases = new LinkedHashSet<>();
        }
        return hostAliases;
    }

    public String getAllowFromEndpoint() {
        return allowFromEndpoint;
    }

    public void setAllowFromEndpoint(String allowFromEndpoint) {
        this.allowFromEndpoint = allowFromEndpoint;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("VirtualHost{");
        if (this.hostAliases != null)
            for (String hostAlias : hostAliases)
                buf.append("hostAlias=\"" + hostAlias + "\" ");
        buf.append("}");

        return buf.toString();
    }
}
