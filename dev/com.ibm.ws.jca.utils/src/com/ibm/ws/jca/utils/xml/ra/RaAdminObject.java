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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaAdminObject;

/**
 * ra.xml adminobject element
 */
@Trivial
@XmlType(propOrder = { "adminObjectInterface", "adminObjectClass", "configProperties" })
public class RaAdminObject {
    private static final TraceComponent tc = Tr.register(RaAdminObject.class);

    public static final HashMap<String, String> parentPids = new HashMap<String, String>();
    static {
        parentPids.put("javax.jms.Destination", "com.ibm.ws.jca.jmsDestination");
        parentPids.put("javax.jms.Topic", "com.ibm.ws.jca.jmsTopic");
        parentPids.put("javax.jms.Queue", "com.ibm.ws.jca.jmsQueue");
        parentPids.put(null, "com.ibm.ws.jca.adminObject");
    }

    private String adminObjectInterface;
    private String adminObjectClass;
    @XmlElement(name = "config-property")
    private final List<RaConfigProperty> configProperties = new LinkedList<RaConfigProperty>();

    @XmlID
    @XmlAttribute(name = "id")
    private String id;

    @XmlTransient
    private String wlp_aliasSuffix;
    @XmlTransient
    private String wlp_adminObjectInterface;
    @XmlTransient
    private String wlp_adminObjectClass;
    @XmlTransient
    private String wlp_nlsKey;
    @XmlTransient
    private String wlp_name;
    @XmlTransient
    private String wlp_description;

    @XmlTransient
    private List<Class<?>> ann_adminObjectInterfaces;

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

    @XmlElement(name = "adminobject-interface", required = true)
    public void setAdminObjectInterface(String adminObjectInterface) {
        this.adminObjectInterface = adminObjectInterface;
    }

    public void setAnnAdminObjectInterfaces(List<Class<?>> interfaces) {
        ann_adminObjectInterfaces = interfaces;
    }

    public String getAdminObjectInterface() {
        return adminObjectInterface != null ? adminObjectInterface : wlp_adminObjectInterface;
    }

    public List<Class<?>> getImplementedAdminObjectInterfaces() {
        return ann_adminObjectInterfaces;
    }

    public String getMetaAdminObjectInterface() {
        if (adminObjectInterface != null)
            return adminObjectInterface;
        else {
            if (ann_adminObjectInterfaces.isEmpty())
                throw new IllegalStateException(Tr.formatMessage(tc, "J2CA9925.admobj.interface.missing", getAdminObjectClass()));
            if (ann_adminObjectInterfaces.size() > 1)
                throw new IllegalStateException(Tr.formatMessage(tc, "J2CA9926.admobj.multiple.interfaces", getAdminObjectClass()));
            return ann_adminObjectInterfaces.get(0).getName();
        }
    }

    @XmlElement(name = "adminobject-class", required = true)
    public void setAdminObjectClass(String adminObjectClass) {
        this.adminObjectClass = adminObjectClass;
    }

    public String getAdminObjectClass() {
        return adminObjectClass != null ? adminObjectClass : wlp_adminObjectClass;
    }

    public List<RaConfigProperty> getConfigProperties() {
        return configProperties;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
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

    public String getParentPid() {
        String parentPid = parentPids.get(adminObjectInterface);
        if (parentPid != null)
            return parentPid;
        else
            return parentPids.get(null);
    }

    public void copyWlpSettings(WlpRaAdminObject adminObject) {
        wlp_aliasSuffix = adminObject.getAliasSuffix();
        wlp_nlsKey = adminObject.getNLSKey();
        wlp_name = adminObject.getName();
        wlp_description = adminObject.getDescription();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RaAdminObject{");
        sb.append("adminobject-interface='").append(adminObjectInterface);
        sb.append("' adminobject-class='").append(adminObjectClass);
        sb.append("'}");

        return sb.toString();
    }

    public boolean useSpecializedConfig() {
        return parentPids.get(adminObjectInterface) != null;
    }
}
