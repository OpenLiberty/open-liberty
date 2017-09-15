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
 * Represents special-subject configuration. See /com.ibm.ws.javaee.dd/resources/OSGI-INF/metatype/metatype.xml
 * 
 */
public class SpecialSubject extends ConfigElement {

    /**
     * Enumerates all allowed values for type attribute
     */
    public static enum Type {
        ALL_AUTHENTICATED_USERS,
        EVERYONE
    }

    private String type;

    /**
     * @param type the subject type
     */
    public void set(Type type) {
        this.setType(type == null ? null : type.toString());
    }

    /**
     * @return One of the following special subject types: ALL_AUTHENTICATED_USERS, EVERYONE.
     */
    public String getType() {
        return this.type;
    }

    /**
     * @param name One of the following special subject types: ALL_AUTHENTICATED_USERS, EVERYONE.
     */
    @XmlAttribute
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("SpecialSubject{");
        if (type != null)
            buf.append("type=\"" + type + "\" ");
        buf.append("}");
        return buf.toString();
    }

    @Override
    public SpecialSubject clone() throws CloneNotSupportedException {
        return (SpecialSubject) super.clone();
    }

}