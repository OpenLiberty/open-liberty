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
package com.ibm.websphere.simplicity.config;

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

public class ConnectionFactory extends ConfigElement {
    // attributes
    private String connectionManagerRef;
    private String containerAuthDataRef;
    private String jndiName;
    private String recoveryAuthDataRef;

    // nested elements
    @XmlElement(name = "connectionManager")
    private ConfigElementList<ConnectionManager> connectionManagers;
    @XmlElement(name = "containerAuthData")
    private ConfigElementList<AuthData> containerAuthData;
    @XmlElement(name = "properties.dcra")
    private ConfigElementList<JCAGeneratedProperties> properties_dcra;
    @XmlElement(name = "properties.FAT1")
    private ConfigElementList<JCAGeneratedProperties> properties_FAT1;
    @XmlElement(name = "properties.wasJms")
    private ConfigElementList<WasJmsProperties> wasJmsProperties;
    @XmlElement(name = "recoveryAuthData")
    private ConfigElementList<AuthData> recoveryAuthData;

    public ConfigElementList<ConnectionManager> getConnectionManager() {
        return connectionManagers == null ? (connectionManagers = new ConfigElementList<ConnectionManager>()) : connectionManagers;
    }

    public String getConnectionManagerRef() {
        return connectionManagerRef;
    }

    public ConfigElementList<AuthData> getContainerAuthData() {
        return containerAuthData == null ? (containerAuthData = new ConfigElementList<AuthData>()) : containerAuthData;
    }

    public String getContainerAuthDataRef() {
        return containerAuthDataRef;
    }

    public String getJndiName() {
        return this.jndiName;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_dcra() {
        return properties_dcra == null ? (properties_dcra = new ConfigElementList<JCAGeneratedProperties>()) : properties_dcra;
    }

    public ConfigElementList<JCAGeneratedProperties> getProperties_FAT1() {
        return properties_FAT1 == null ? (properties_FAT1 = new ConfigElementList<JCAGeneratedProperties>()) : properties_FAT1;
    }

    public ConfigElementList<AuthData> getRecoveryAuthData() {
        return recoveryAuthData == null ? (recoveryAuthData = new ConfigElementList<AuthData>()) : recoveryAuthData;
    }

    public String getRecoveryAuthDataRef() {
        return recoveryAuthDataRef;
    }

    public ConfigElementList<WasJmsProperties> getWasJmsProperties() {
        if (wasJmsProperties == null) {
            wasJmsProperties = new ConfigElementList<WasJmsProperties>();
        }
        return wasJmsProperties;
    }

    @XmlAttribute
    public void setConnectionManagerRef(String connectionManagerRef) {
        this.connectionManagerRef = connectionManagerRef;
    }

    @XmlAttribute
    public void setContainerAuthDataRef(String containerAuthDataRef) {
        this.containerAuthDataRef = containerAuthDataRef;
    }

    @XmlAttribute
    public void setJndiName(String jndiName) {
        this.jndiName = ConfigElement.getValue(jndiName);
    }

    @XmlAttribute
    public void setRecoveryAuthDataRef(String recoveryAuthDataRef) {
        this.recoveryAuthDataRef = recoveryAuthDataRef;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        if (connectionManagerRef != null)
            buf.append("connectionManagerRef=\"" + connectionManagerRef + "\" ");
        if (containerAuthDataRef != null)
            buf.append("containerAuthDataRef=\"" + containerAuthDataRef + "\" ");
        if (jndiName != null)
            buf.append("jndiName=\"" + jndiName + "\" ");
        if (recoveryAuthDataRef != null)
            buf.append("recoveryAuthDataRef=\"" + recoveryAuthDataRef + "\" ");

        @SuppressWarnings("unchecked")
        List<ConfigElementList<?>> nestedElementsList = Arrays.asList(connectionManagers,
                                                                      containerAuthData,
                                                                      properties_dcra,
                                                                      properties_FAT1,
                                                                      recoveryAuthData,
                                                                      wasJmsProperties
                        );
        for (ConfigElementList<?> nestedElements : nestedElementsList)
            if (nestedElements != null && nestedElements.size() > 0)
                for (Object o : nestedElements)
                    buf.append(", " + o);
        buf.append("}");
        return buf.toString();
    }
}
