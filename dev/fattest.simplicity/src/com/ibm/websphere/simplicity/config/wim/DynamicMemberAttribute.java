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
package com.ibm.websphere.simplicity.config.wim;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.simplicity.config.ConfigElement;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>ldapRegistry --> groupConfiguration --> memberAttribute</li>
 * </ul>
 */
public class DynamicMemberAttribute extends ConfigElement {
    private String name;
    private String objectClass;

    public DynamicMemberAttribute() {}

    public DynamicMemberAttribute(String name, String objectClass) {
        this.name = name;
        this.objectClass = objectClass;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the objectClass
     */
    public String getObjectClass() {
        return objectClass;
    }

    /**
     * @param name the name to set
     */
    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param objectClass the objectClass to set
     */
    @XmlAttribute(name = "objectClass")
    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (name != null) {
            sb.append("name=\"").append(name).append("\" ");;
        }
        if (objectClass != null) {
            sb.append("objectClass=\"").append(objectClass).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}
