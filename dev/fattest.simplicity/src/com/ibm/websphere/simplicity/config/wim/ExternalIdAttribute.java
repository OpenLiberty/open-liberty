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
 * <li>ldapRegistry --> attributeConfiguration --> externalIdAttribute</li>
 * </ul>
 */
public class ExternalIdAttribute extends ConfigElement {

    private Boolean autoGenerate;
    private String entityType;
    private String name;
    private String syntax;

    public ExternalIdAttribute() {}

    public ExternalIdAttribute(String name, String entityType, String syntax, Boolean autoGenerate) {
        this.name = name;
        this.entityType = entityType;
        this.syntax = syntax;
        this.autoGenerate = autoGenerate;
    }

    /**
     * @return the autoGenerate
     */
    public Boolean getAutoGenerate() {
        return autoGenerate;
    }

    /**
     * @return the entityType
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the syntax
     */
    public String getSyntax() {
        return syntax;
    }

    /**
     * @param autoGenerate the autoGenerate to set
     */
    @XmlAttribute(name = "autoGenerate")
    public void setAutoGenerate(Boolean autoGenerate) {
        this.autoGenerate = autoGenerate;
    }

    /**
     * @param entityType the entityType to set
     */
    @XmlAttribute(name = "entityType")
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * @param name the name to set
     */
    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param syntax the syntax to set
     */
    @XmlAttribute(name = "syntax")
    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (autoGenerate != null) {
            sb.append("dataType=\"").append(autoGenerate).append("\" ");
        }
        if (entityType != null) {
            sb.append("entityType=").append(entityType).append("\" ");;
        }
        if (name != null) {
            sb.append("name=\"").append(name).append("\" ");;
        }
        if (syntax != null) {
            sb.append("syntax=\"").append(syntax).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}