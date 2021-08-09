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

/**
 *
 */
public class MPMetricsElement {

    private Boolean authentication;

    public Boolean getAuthentication() {
        return authentication;
    }

    @XmlAttribute(name = "authentication")
    public void setAuthentication(Boolean authentication) {
        this.authentication = authentication;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("mpMetricsElement [");
        sb.append("authentication=").append(authentication);
        sb.append("]");
        return sb.toString();
    }
}