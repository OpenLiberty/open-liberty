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

/**
 * ra.xml activationspec element
 */
@Trivial
@XmlType(propOrder = { "activationSpecClass", "requiredConfigProperties", "configProperties" })
public class RaActivationSpec {
    private String activationSpecClass;
    @XmlElement(name = "required-config-property")
    private final List<RaRequiredConfigProperty> requiredConfigProperties = new LinkedList<RaRequiredConfigProperty>();
    @XmlElement(name = "config-property")
    private final List<RaConfigProperty> configProperties = new LinkedList<RaConfigProperty>();
    @XmlID
    @XmlAttribute(name = "id")
    private String id;

    public List<RaConfigProperty> getConfigProperties() {
        return configProperties;
    }

    public List<RaRequiredConfigProperty> getRequiredConfigProperties() {
        return requiredConfigProperties;
    }

    @XmlElement(name = "activationspec-class", required = true)
    public void setActivationSpecClass(String activationSpecClass) {
        this.activationSpecClass = activationSpecClass;
    }

    public String getActivationSpecClass() {
        return activationSpecClass;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RaActivationSpec{");
        sb.append("activationspec-class='");
        if (activationSpecClass != null)
            sb.append(activationSpecClass);

        sb.append("'}");

        return sb.toString();
    }

    public RaConfigProperty getConfigPropertyById(String name) {
        for (RaConfigProperty configProperty : configProperties) {
            if (configProperty.getName().equals(name))
                return configProperty;
        }

        return null;
    }

    public boolean isConfigPropertyAlreadyDefined(String configPropName) {
        RaConfigProperty configProperty = getConfigPropertyById(configPropName);
        if (configProperty == null)
            return false;
        if (configProperty.getType() != null)
            return true;
        else
            return false;
    }
}
