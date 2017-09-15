/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
public class IncludeElement extends ConfigElement {
    private String location;

    public String getLocation() {
        return location;
    }

    @XmlAttribute(name = "location")
    public void setLocation(String s) {
        this.location = s;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("IncludeElement{");
        if (this.getId() != null)
            buf.append("id=\"" + this.getId() + "\" ");
        if (location != null)
            buf.append("location=\"" + location + "\" ");

        buf.append("}");
        return buf.toString();
    }
}
