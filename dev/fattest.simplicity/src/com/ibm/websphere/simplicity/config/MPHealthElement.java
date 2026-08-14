/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
 *
 */
public class MPHealthElement {

    private String checkInterval;

    private String startupCheckInterval;

    private String enableEndpoints;

    public String getCheckInterval() {
        return checkInterval;
    }

    public String getStartupCheckInterval() {
        return startupCheckInterval;
    }

    public String getEnableEndpoints() {
        return enableEndpoints;
    }

    @XmlAttribute(name = "checkInterval")
    public void setCheckInterval(String checkInterval) {
        this.checkInterval = checkInterval;
    }

    @XmlAttribute(name = "startupCheckInterval")
    public void setStartupCheckInterval(String startupCheckInterval) {
        this.startupCheckInterval = startupCheckInterval;
    }

    @XmlAttribute(name = "enableEndpoints")
    public void setEnableEndpoints(String enableEndpoints) {
        this.enableEndpoints = enableEndpoints;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("mpHealthElement [");
        sb.append("checkInterval=").append(checkInterval);
        sb.append(", startupCheckInterval=").append(startupCheckInterval);
        sb.append(", enableEndpoints=").append(enableEndpoints);
        sb.append("]");
        return sb.toString();
    }
}