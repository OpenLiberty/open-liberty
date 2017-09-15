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
package com.ibm.ws.jca.utils.xml.wlp.ra;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * ra.xml activationspec element
 */
@Trivial
@XmlType
public class WlpRaActivationSpec {
    private String activationSpecClass;

    @XmlElement(name = "config-property")
    private final List<WlpRaConfigProperty> configProperties = new LinkedList<WlpRaConfigProperty>();

    public List<WlpRaConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @XmlAttribute(name = "activationspec-class")
    public void setActivationSpecClass(String activationSpecClass) {
        this.activationSpecClass = activationSpecClass;
    }

    public String getActivationSpecClass() {
        return activationSpecClass;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WlpRaActivationSpec{");
        sb.append("activationspec-class='");
        if (activationSpecClass != null)
            sb.append(activationSpecClass);

        sb.append("'}");

        return sb.toString();
    }

    public WlpRaConfigProperty getConfigPropertyById(String name) {
        for (WlpRaConfigProperty configProperty : configProperties) {
            if (configProperty.getWlpPropertyName().equals(name))
                return configProperty;
        }
        return null;
    }

}
