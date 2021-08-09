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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10ConfigProperty;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10ResourceAdapter;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConnectionDefinition;

/**
 * ra.xml connection-definition element
 */
@Trivial
@XmlType(propOrder = { "managedConnectionFactoryClass", "configProperties", "connectionFactoryInterface",
                      "connectionFactoryImplClass", "connectionInterface",
                      "connectionImplClass" })
public class RaConnectionDefinition {
    public static final HashMap<String, String> parentPids = new HashMap<String, String>();
    static {
        parentPids.put("javax.jms.ConnectionFactory", "com.ibm.ws.jca.jmsConnectionFactory");
        parentPids.put("javax.jms.QueueConnectionFactory", "com.ibm.ws.jca.jmsQueueConnectionFactory");
        parentPids.put("javax.jms.TopicConnectionFactory", "com.ibm.ws.jca.jmsTopicConnectionFactory");
        parentPids.put(null, "com.ibm.ws.jca.connectionFactory");
    }

    private String managedConnectionFactoryClass;
    private String connectionFactoryInterface;
    private String connectionFactoryImplClass;
    private String connectionInterface;
    private String connectionImplClass;
    private String id;
    @XmlElement(name = "config-property")
    private final List<RaConfigProperty> configProperties = new LinkedList<RaConfigProperty>();

    // wlp-ra.xml settings
    @XmlTransient
    private String wlp_aliasSuffix;
    @XmlTransient
    private String wlp_connectionFactoryInterface;
    @XmlTransient
    private String wlp_nlsKey;
    @XmlTransient
    private String wlp_name;
    @XmlTransient
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

    public String getID() {
        return id;
    }

    @XmlID
    @XmlAttribute(name = "id")
    public void setID(String id) {
        this.id = id;
    }

    @XmlElement(name = "managedconnectionfactory-class", required = true)
    public void setManagedConnectionFactoryClass(String managedConnectionFactoryClass) {
        this.managedConnectionFactoryClass = managedConnectionFactoryClass;
    }

    public String getManagedConnectionFactoryClass() {
        return managedConnectionFactoryClass;
    }

    @XmlElement(name = "connectionfactory-interface", required = true)
    public void setConnectionFactoryInterface(String connectionFactoryInterface) {
        this.connectionFactoryInterface = connectionFactoryInterface;
    }

    public String getConnectionFactoryInterface() {
        return connectionFactoryInterface != null ? connectionFactoryInterface : wlp_connectionFactoryInterface;
    }

    @XmlElement(name = "connectionfactory-impl-class", required = true)
    public void setConnectionFactoryImplClass(String connectionFactoryImplClass) {
        this.connectionFactoryImplClass = connectionFactoryImplClass;
    }

    public String getConnectionFactoryImplClass() {
        return connectionFactoryImplClass;
    }

    @XmlElement(name = "connection-interface", required = true)
    public void setConnectionInterface(String connectionInterface) {
        this.connectionInterface = connectionInterface;
    }

    public String getConnectionInterface() {
        return connectionInterface;
    }

    @XmlElement(name = "connection-impl-class", required = true)
    public void setConnectionImplClass(String connectionImplClass) {
        this.connectionImplClass = connectionImplClass;
    }

    public String getConnectionImplClass() {
        return connectionImplClass;
    }

    public List<RaConfigProperty> getConfigProperties() {
        return configProperties;
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

    public String getConnectionFactoryParentPid() {
        String parentPid = parentPids.get(connectionFactoryInterface);
        if (parentPid != null)
            return parentPid;
        else
            return parentPids.get(null);
    }

    public void copyWlpSettings(WlpRaConnectionDefinition connectionDefinition) {
        wlp_aliasSuffix = connectionDefinition.getAliasSuffix();
        wlp_nlsKey = connectionDefinition.getNLSKey();
        wlp_name = connectionDefinition.getName();
        wlp_description = connectionDefinition.getDescription();
    }

    public void copyRa10Settings(Ra10ResourceAdapter ra10resourceAdapter) {
        connectionFactoryImplClass = ra10resourceAdapter.getConnectionFactoryImpl();
        connectionFactoryInterface = ra10resourceAdapter.getConnectionFactoryInterface();
        connectionImplClass = ra10resourceAdapter.getConnectionImpl();
        connectionInterface = ra10resourceAdapter.getConnectionInterface();
        managedConnectionFactoryClass = ra10resourceAdapter.getManagedConnectionFactoryClass();
        for (Ra10ConfigProperty property : ra10resourceAdapter.getConfigProperties()) {
            RaConfigProperty configProperty = new RaConfigProperty();
            configProperty.copyRa10Settings(property);
            configProperties.add(configProperty);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RaConnectionDefinition{");
        sb.append("connectionfactory-interface='");
        if (connectionFactoryInterface != null)
            sb.append(connectionFactoryInterface);
        else
            sb.append(wlp_connectionFactoryInterface);
        sb.append("'}");
        return sb.toString();
    }

    public boolean useSpecializedConfig() {
        return parentPids.get(connectionFactoryInterface) != null;
    }
}
