/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
public class CVEReportingElement extends ConfigElement {

    private String enabled;

    private String urlLink;

    public String getEnabled() {
        return enabled;
    }

    public String getUrlLink() {
        return urlLink;
    }

    @XmlAttribute(name = "enabled")
    public void setEnabled(String b) {
        this.enabled = b;
    }

    public void setEnabled(Boolean b) {
        this.enabled = b == null ? null : b.toString();
    }

    @XmlAttribute(name = "urlLink")
    public void setUrlLink(String link) {
        this.urlLink = link;
    }

}