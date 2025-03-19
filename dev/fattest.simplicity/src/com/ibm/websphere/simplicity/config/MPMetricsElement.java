/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
public class MPMetricsElement {

    private Boolean authentication;
    private String libraryRef;

    public Boolean getAuthentication() {
        return authentication;
    }

    public String getLibraryRef() {
        return libraryRef;
    }

    @XmlAttribute(name = "authentication")
    public void setAuthentication(Boolean authentication) {
        this.authentication = authentication;
    }

    @XmlAttribute(name = "libraryRef")
    public void setLibraryRef(String libraryRef) {
        this.libraryRef = libraryRef;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("mpMetricsElement [");
        sb.append("authentication=").append(authentication);
        sb.append(",");
        sb.append("libraryRef=").append(libraryRef);
        sb.append("]");
        return sb.toString();
    }
}