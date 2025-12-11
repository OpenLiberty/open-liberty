/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

    public String getCheckInterval() {
        return checkInterval;
    }

    public String getStartupCheckInterval() {
        return startupCheckInterval;
    }

    @XmlAttribute(name = "checkInterval")
    public void setCheckInterval(String checkInterval) {
        this.checkInterval = checkInterval;
    }

    @XmlAttribute(name = "startupCheckInterval")
    public void setStartupCheckInterval(String startupCheckInterval) {
        this.startupCheckInterval = startupCheckInterval;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("mpHealthElement [");
        sb.append("checkInterval=").append(checkInterval);
        sb.append("startupCheckInterval=").append(startupCheckInterval);
        sb.append("]");
        return sb.toString();
    }
}