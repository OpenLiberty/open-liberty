/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * A path on the file system local to the server configuration. Note
 * that these path may be remote to the local JVM.
 *
 *
 */
public class Path extends ConfigElement {

    private String name;

    /**
     * @return value of the name attribute
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name value to use for the name attribute
     */
    @XmlAttribute
    public void setName(String name) {
        this.name = ConfigElement.getValue(name);
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("Path{");
        if (name != null)
            buf.append("name=\"" + name + "\" ");
        buf.append("}");
        return buf.toString();
    }
}
