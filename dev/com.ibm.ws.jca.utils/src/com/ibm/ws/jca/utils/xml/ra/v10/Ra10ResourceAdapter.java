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

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 */
@XmlType(name = "resourceAdapterType", propOrder = { "managedConnectionFactoryClass", "connectionFactoryInterface", "connectionFactoryImpl", "connectionInterface",
                                                    "connectionImpl", "transactionSupport", "configProperties", "authMechanisms", "reauthenticationSupport", "permissions" })
public class Ra10ResourceAdapter {

    @XmlElement(name = "managedconnectionfactory-class", required = true)
    private String managedConnectionFactoryClass;
    @XmlElement(name = "connectionfactory-interface", required = true)
    private String connectionFactoryInterface;
    @XmlElement(name = "connectionfactory-impl-class", required = true)
    private String connectionFactoryImpl;
    @XmlElement(name = "connection-interface", required = true)
    private String connectionInterface;
    @XmlElement(name = "connection-impl-class", required = true)
    private String connectionImpl;
    @XmlElement(name = "transaction-support", required = true)
    private String transactionSupport;
    @XmlElement(name = "config-property")
    private final List<Ra10ConfigProperty> configProperties = new LinkedList<Ra10ConfigProperty>();
    @XmlElement(name = "auth-mechanism")
    private final List<Ra10AuthenticationMechanism> authMechanisms = new LinkedList<Ra10AuthenticationMechanism>();
    @XmlElement(name = "reauthentication-support", required = true)
    private String reauthenticationSupport;
    @XmlElement(name = "security-permission")
    private final List<Ra10SecurityPermission> permissions = new LinkedList<Ra10SecurityPermission>();

    /**
     * @return the managedConnectionFactoryClass
     */
    public String getManagedConnectionFactoryClass() {
        return managedConnectionFactoryClass;
    }

    /**
     * @return the connectionFactoryInterface
     */
    public String getConnectionFactoryInterface() {
        return connectionFactoryInterface;
    }

    /**
     * @return the connectionFactoryImpl
     */
    public String getConnectionFactoryImpl() {
        return connectionFactoryImpl;
    }

    /**
     * @return the connectionInterface
     */
    public String getConnectionInterface() {
        return connectionInterface;
    }

    /**
     * @return the connectionImpl
     */
    public String getConnectionImpl() {
        return connectionImpl;
    }

    /**
     * @return the transactionSupport
     */
    public String getTransactionSupport() {
        return transactionSupport;
    }

    /**
     * @return the configProperties
     */
    public List<Ra10ConfigProperty> getConfigProperties() {
        return configProperties;
    }

    /**
     * @return the authMechanisms
     */
    public List<Ra10AuthenticationMechanism> getAuthMechanisms() {
        return authMechanisms;
    }

    /**
     * @return the reauthenticationSupport
     */
    public String getReauthenticationSupport() {
        return reauthenticationSupport;
    }

    /**
     * @return the permissions
     */
    public List<Ra10SecurityPermission> getPermissions() {
        return permissions;
    }

}
