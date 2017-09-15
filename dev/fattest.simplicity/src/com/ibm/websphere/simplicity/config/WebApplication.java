/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
 * Represents a &lt;webApplication> configuration element
 */
public class WebApplication extends Application {

    private String contextRoot;

    /**
     * @return the contextRoot
     */
    public String getContextRoot() {
        return contextRoot;
    }

    /**
     * @param contextRoot the contextRoot to set
     */
    @XmlAttribute
    public void setContextRoot(String contextRoot) {
        this.contextRoot = contextRoot;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("WebApplication{");
        buf.append(super.toString());
        if (contextRoot != null)
            buf.append("contextRoot=\"" + contextRoot + "\" ");
        buf.append("}");

        return buf.toString();
    }

    @Override
    public WebApplication clone() throws CloneNotSupportedException {
        return (WebApplication) super.clone();
    }

}
