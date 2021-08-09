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

import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.simplicity.config.ConfigElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>ldapRegistry --> attributeConfiguration</li>
 * </ul>
 */
public class AttributeConfiguration extends ConfigElement {

    private ConfigElementList<Attribute> attributes;
    private ConfigElementList<ExternalIdAttribute> externalIdAttributes;

    /**
     * @return the attributes
     */
    public ConfigElementList<Attribute> getAttributes() {
        return (attributes == null) ? (attributes = new ConfigElementList<Attribute>()) : attributes;
    }

    /**
     * @return the externalIdAttribute
     */
    public ConfigElementList<ExternalIdAttribute> getExternalIdAttributes() {
        return (externalIdAttributes == null) ? (externalIdAttributes = new ConfigElementList<ExternalIdAttribute>()) : externalIdAttributes;
    }

    /**
     * @param attribute the attributes to set
     */
    @XmlElement(name = "attribute")
    public void setAttributes(ConfigElementList<Attribute> attributes) {
        this.attributes = attributes;
    }

    /**
     * @param externalIdAttribute the externalIdAttributes to set
     */
    @XmlElement(name = "externalIdAttribute")
    public void setExternalIdAttributes(ConfigElementList<ExternalIdAttribute> externalIdAttributes) {
        this.externalIdAttributes = externalIdAttributes;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (attributes != null) {
            sb.append("attributes=\"").append(attributes).append("\" ");
        }
        if (externalIdAttributes != null) {
            sb.append("externalIdAttributes=\"").append(externalIdAttributes).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}