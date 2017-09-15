/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.xml.ra;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.metagen.MetatypeGenerator;

/**
 * ra.xml required-config-property element
 */
@Trivial
@XmlType(propOrder = { "description", "configPropertyName" })
public class RaRequiredConfigProperty {
    private String configPropertyName;
    @XmlElement(name = "description")
    private final List<RaDescription> description = new LinkedList<RaDescription>();
    @XmlID
    @XmlAttribute(name = "id")
    private String id;

    public String getConfigPropertyName() {
        return configPropertyName;
    }

    public List<RaDescription> getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    @XmlElement(name = "config-property-name", required = true)
    public void setConfigPropertyName(String name) {
        configPropertyName = MetatypeGenerator.toCamelCase(name);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RaRequiredConfigProperty{");
        sb.append("config-property-name='").append(configPropertyName).append("'}");
        return sb.toString();
    }
}
