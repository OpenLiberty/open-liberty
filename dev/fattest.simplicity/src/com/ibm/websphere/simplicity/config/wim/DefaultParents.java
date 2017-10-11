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

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>federatedRepository --> primaryRealm --> defaultParents</li>
 * <li>federatedRepository --> realm --> defaultParents</li>
 * </ul>
 */
public class DefaultParents {

    private String name;
    private String parentUniqueName;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the parentUniqueName
     */
    public String getParentUniqueName() {
        return parentUniqueName;
    }

    /**
     * @param name the name to set
     */
    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param parentUniqueName the parentUniqueName to set
     */
    @XmlAttribute(name = "parentUniqueName")
    public void setParentUniqueName(String parentUniqueName) {
        this.parentUniqueName = parentUniqueName;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (name != null) {
            sb.append("name=\"").append(name).append("\" ");
        }
        if (parentUniqueName != null) {
            sb.append("parentUniqueName=\"").append(parentUniqueName).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}