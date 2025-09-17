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
 * Defines Http Options for channel framework
 */
public class HttpOptions extends ConfigElement {

    private Boolean ignoreWriteAfterCommit;

    public Boolean isIgnoreWriteAfterCommit() {
        return this.ignoreWriteAfterCommit;
    }

    @XmlAttribute
    public void setIgnoreWriteAfterCommit(Boolean ignoreWriteAfterCommit) {
        this.ignoreWriteAfterCommit = ignoreWriteAfterCommit;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("httpOptions{");
        if (getId() != null)
            buf.append("id=\"" + this.getId() + "\" ");
        if (ignoreWriteAfterCommit != null)
            buf.append("ignoreWriteAfterCommit=\"" + ignoreWriteAfterCommit + "\" ");
        buf.append("}");
        return buf.toString();
    }

}
