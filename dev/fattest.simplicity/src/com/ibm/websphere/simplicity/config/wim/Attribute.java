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
 * <li>ldapRegistry --> attributeConfiguration --> attribute</li>
 * </ul>
 */
public class Attribute extends ConfigElement {

    private String defaultValue;
    private String entityType;
    private String name;
    private String propertyName;
    private String syntax;

    public Attribute() {}

    public Attribute(String name, String propertyName, String entityType, String syntax, String defaultValue) {
        this.name = name;
        this.propertyName = propertyName;
        this.entityType = entityType;
        this.syntax = syntax;
        this.defaultValue = defaultValue;
    }

    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
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
     * @return the propertyName
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @return the syntax
     */
    public String getSyntax() {
        return syntax;
    }

    /**
     * @param defaultValue the defaultValue to set
     */
    @XmlAttribute(name = "defaultValue")
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
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
     * @param propertyName the propertyName to set
     */
    @XmlAttribute(name = "propertyName")
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
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

        if (defaultValue != null) {
            sb.append("defaultValue=\"").append(defaultValue).append("\" ");
        }
        if (entityType != null) {
            sb.append("entityType=\"").append(entityType).append("\" ");;
        }
        if (name != null) {
            sb.append("name=\"").append(name).append("\"");;
        }
        if (propertyName != null) {
            sb.append("propertyName=\"").append(propertyName).append("\" ");;
        }
        if (syntax != null) {
            sb.append("syntax=\"").append(syntax).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}