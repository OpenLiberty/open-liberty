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
 * ra.xml connection-definition element
 */
@Trivial
@XmlType
public class WlpRaConnectionDefinition {

    @XmlElement(name = "config-property")
    private final List<WlpRaConfigProperty> configProperties = new LinkedList<WlpRaConfigProperty>();

    // wlp-ra.xml settings
    @XmlAttribute(name = "aliasSuffix")
    private String wlp_aliasSuffix;
    @XmlAttribute(name = "connectionfactory-interface")
    private String wlp_connectionFactoryInterface;
    @XmlAttribute(name = "nlsKey")
    private String wlp_nlsKey;
    @XmlAttribute(name = "name")
    private String wlp_name;
    @XmlAttribute(name = "description")
    private String wlp_description;

    public String getName() {
        return wlp_name;
    }

    public String getDescription() {
        return wlp_description;
    }

    public String getNLSKey() {
        return wlp_nlsKey;
    }

    public String getAliasSuffix() {
        return wlp_aliasSuffix;
    }

    public String getConnectionFactoryInterface() {
        return wlp_connectionFactoryInterface;
    }

    public List<WlpRaConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WlpRaConnectionDefinition{");
        sb.append("connectionfactory-interface='");
        sb.append(wlp_connectionFactoryInterface);
        sb.append("'}");
        return sb.toString();
    }
}
