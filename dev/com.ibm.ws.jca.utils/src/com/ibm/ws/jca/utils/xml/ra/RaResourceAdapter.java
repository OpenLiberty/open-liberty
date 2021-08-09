/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10ResourceAdapter;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10SecurityPermission;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaResourceAdapter;

/**
 * ra.xml resourceadapter element
 */
@Trivial
@XmlType(propOrder = { "resourceAdapterClass", "configProperties", "outboundResourceAdapter", "inboundResourceAdapter",
                      "adminObjects", "securityPermissions" })
public class RaResourceAdapter {
    @XmlElement(name = "resourceadapter-class")
    public void setResourceAdapterClass(String rac) {
        this.resourceAdapterClass = rac;
    }

    private String resourceAdapterClass;

    @XmlElement(name = "config-property")
    private final List<RaConfigProperty> configProperties = new LinkedList<RaConfigProperty>();
    private RaOutboundResourceAdapter outboundResourceAdapter;
    private RaInboundResourceAdapter inboundResourceAdapter;
    @XmlElement(name = "adminobject")
    private final List<RaAdminObject> adminObjects = new LinkedList<RaAdminObject>();
    private List<RaSecurityPermission> securityPermissions = new LinkedList<RaSecurityPermission>();
    @XmlID
    @XmlAttribute(name = "id")
    private String id;
    // wlp-ra.xml properties
    @XmlTransient
    private String wlp_nlsKey;
    @XmlTransient
    private String wlp_name;
    @XmlTransient
    private String wlp_description;
    @XmlTransient
    private Boolean wlp_autoStart;

    public String getName() {
        return wlp_name;
    }

    public String getDescription() {
        return wlp_description;
    }

    public String getNLSKey() {
        return wlp_nlsKey;
    }

    public Boolean getAutoStart() {
        return wlp_autoStart;
    }

    public String getResourceAdapterClass() {
        return resourceAdapterClass;
    }

    public List<RaConfigProperty> getConfigProperties() {
        return configProperties;
    }

    public String getId() {
        return id;
    }

    @XmlElement(name = "outbound-resourceadapter")
    public void setOutboundResourceAdapter(RaOutboundResourceAdapter outboundResourceAdapter) {
        this.outboundResourceAdapter = outboundResourceAdapter;
    }

    public RaOutboundResourceAdapter getOutboundResourceAdapter() {
        return outboundResourceAdapter;
    }

    @XmlElement(name = "inbound-resourceadapter")
    public void setInboundResourceAdapter(RaInboundResourceAdapter inboundResourceAdapter) {
        this.inboundResourceAdapter = inboundResourceAdapter;
    }

    public RaInboundResourceAdapter getInboundResourceAdapter() {
        return inboundResourceAdapter;
    }

    public List<RaAdminObject> getAdminObjects() {
        return adminObjects;
    }

    @XmlElement(name = "security-permission")
    public void setSecurityPermissions(List<RaSecurityPermission> securityPermissions) {
        this.securityPermissions = securityPermissions;
    }

    public List<RaSecurityPermission> getSecurityPermissions() {
        return securityPermissions;
    }

    public RaConfigProperty getConfigPropertyById(String name) {
        for (RaConfigProperty configProperty : configProperties)
            if (configProperty.getName().equals(name))
                return configProperty;

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

    public RaAdminObject getAdminObject(String adminObjectInterface, String adminObjectClass) {
        for (RaAdminObject adminObject : adminObjects)
            if (adminObject.getAdminObjectInterface().equals(adminObjectInterface) && adminObject.getAdminObjectClass().equals(adminObjectClass))
                return adminObject;

        return null;
    }

    public void copyWlpSettings(WlpRaResourceAdapter resourceAdapter) {
        wlp_nlsKey = resourceAdapter.getNLSKey();
        wlp_name = resourceAdapter.getName();
        wlp_description = resourceAdapter.getDescription();
        wlp_autoStart = resourceAdapter.getAutoStart();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RaResourceAdapter{");
        sb.append("resourceadapter-class='");
        if (resourceAdapterClass != null)
            sb.append(resourceAdapterClass);
        sb.append("'}");
        return sb.toString();
    }

    public void copyRa10Settings(Ra10ResourceAdapter ra10resourceAdapter) {
        for (Ra10SecurityPermission permission : ra10resourceAdapter.getPermissions()) {
            RaSecurityPermission securityPermission = new RaSecurityPermission();
            securityPermission.copyRa10Settings(permission);
            securityPermissions.add(securityPermission);
        }

        resourceAdapterClass = null;
        outboundResourceAdapter = new RaOutboundResourceAdapter();
        outboundResourceAdapter.copyRa10Settings(ra10resourceAdapter);
        inboundResourceAdapter = null;
    }
}
