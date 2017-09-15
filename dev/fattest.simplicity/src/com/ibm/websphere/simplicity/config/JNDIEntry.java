/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
 * An authData element for holding usernames and passwords
 */
public class JNDIEntry extends ConfigElement {
    private String jndiName;
    private String value;

    /**
     * @return the jndiName
     */
    public String getJndiName() {
        return jndiName;
    }

    /**
     * @param jndiName the jndiName to set
     */
    @XmlAttribute
    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    @XmlAttribute
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns a string containing a list of the properties and their values stored
     * for this JNDIEntry object.
     * 
     * @return String representing the data
     */
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("JNDIEntry{");
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        buf.append("jndiName=\"" + (jndiName == null ? "" : jndiName) + "\"");
        buf.append("value=\"" + (value == null ? "" : value) + "\" ");
        buf.append("}");
        return buf.toString();
    }
}
