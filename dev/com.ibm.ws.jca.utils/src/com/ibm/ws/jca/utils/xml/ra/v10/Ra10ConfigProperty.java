/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.xml.ra.v10;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 */
@XmlType(name = "configPropertyType", propOrder = { "description", "configPropertyName", "configPropertyType", "configPropertyValue" })
public class Ra10ConfigProperty {

    @XmlElement(name = "description")
    private String description;
    @XmlElement(name = "config-property-name", required = true)
    private String configPropertyName;
    @XmlElement(name = "config-property-type", required = true)
    private String configPropertyType;
    @XmlElement(name = "config-property-value")
    private String configPropertyValue;

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the configPropertyName
     */
    public String getConfigPropertyName() {
        return configPropertyName;
    }

    /**
     * @return the configPropertyType
     */
    public String getConfigPropertyType() {
        return configPropertyType;
    }

    /**
     * @return the configPropertyValue
     */
    public String getConfigPropertyValue() {
        return configPropertyValue;
    }

}
