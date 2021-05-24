/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
 * Represents the <heritageSettings> element in server.xml
 */
public class HeritageSettings extends ConfigElement {
    // attributes
    private String helperClass;
    private String replaceExceptions;

    public String getHelperClass() {
        return helperClass;
    }

    public String getReplaceExceptions() {
        return replaceExceptions;
    }

    // setters for attributes

    @XmlAttribute
    public void setHelperClass(String value) {
        helperClass = value;
    }

    @XmlAttribute
    public void setReplaceExceptions(String value) {
        replaceExceptions = value;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        // attributes
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (helperClass != null)
            buf.append("helperClass=").append(helperClass).append(' ');
        if (replaceExceptions != null)
            buf.append("replaceExceptions=").append(replaceExceptions).append(' ');
        // nested elements - none
        buf.append('}');
        return buf.toString();
    }
}
