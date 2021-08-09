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
 * wlp-ra.xml adminobject element
 */
@Trivial
@XmlType
public class WlpRaAdminObject {

    @XmlElement(name = "config-property")
    private final List<WlpRaConfigProperty> configProperties = new LinkedList<WlpRaConfigProperty>();

    // wlp-ra.xml settings
    @XmlAttribute(name = "aliasSuffix")
    private String wlp_aliasSuffix;
    @XmlAttribute(name = "adminobject-interface")
    private String wlp_adminObjectInterface;
    @XmlAttribute(name = "adminobject-class")
    private String wlp_adminObjectClass;
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

    public List<WlpRaConfigProperty> getConfigProperties() {
        return configProperties;
    }

    public String getAdminObjectInterface() {
        return wlp_adminObjectInterface;
    }

    public String getAdminObjectClass() {
        return wlp_adminObjectClass;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WlpRaAdminObject{");
        sb.append("adminobject-interface='").append(wlp_adminObjectInterface);
        sb.append("' adminobject-class='").append(wlp_adminObjectClass);
        sb.append("'}");

        return sb.toString();
    }
}
