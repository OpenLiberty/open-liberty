/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 * <li>federatedRepository --> supportedEntityType</li>
 * </ul>
 */
public class SupportedEntityType extends ConfigElement {

    private String name;
    private String parent;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the parent
     */
    public String getParent() {
        return parent;
    }

    /**
     * @param name the name to set
     */
    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param parent the parent to set
     */
    @XmlAttribute(name = "parent")
    public void setParent(String parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (name != null) {
            sb.append("name=\"").append(name).append("\" ");;
        }
        if (parent != null) {
            sb.append("parent=\"").append(parent).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}