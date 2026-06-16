/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config.dsprops;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.DataSourceProperties;

/**
 * Lists data source properties specific to this driver.
 */
public class Properties_h2 extends DataSourceProperties {
    private String URL;

    @Override
    public String getElementName() {
        return H2;
    }

    public String getURL() {
        return this.URL;
    }

    @XmlAttribute(name = "URL")
    public void setURL(String URL) {
        this.URL = URL;
    }

    /**
     * Returns a String listing the properties and their values.
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("{");
        if (getPassword() != null)
            buf.append("password=\"" + getPassword() + "\" ");
        if (getUser() != null)
            buf.append("user=\"" + getUser() + "\" ");
        if (URL != null)
            buf.append("URL=\"" + URL + "\" ");
        buf.append("}");
        return buf.toString();
    }
}