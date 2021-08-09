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
 * A top level global "classloading" element, rather than an app's classloader.
 */
public class ClassloadingElement extends ConfigElement {
    private Boolean useJarUrls = false;

    public Boolean getUseJarUrls() {
        return useJarUrls;
    }

    @XmlAttribute(name = "useJarUrls")
    public void setUseJarUrls(Boolean b) {
        this.useJarUrls = b;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("ClassloadingElement{");
        if (useJarUrls != null)
            buf.append("useJarUrls=\"" + useJarUrls + "\" ");
        buf.append("}");
        return buf.toString();
    }
}
