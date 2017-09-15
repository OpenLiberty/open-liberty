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

import javax.resource.spi.TransactionSupport.TransactionSupportLevel;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10AuthenticationMechanism;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10ResourceAdapter;

/**
 * ra.xml outbound-resourceadapter element
 */
@Trivial
@XmlType(propOrder = { "connectionDefinitions", "transactionSupport", "authenticationMechanisms", "reauthenticationSupport" })
public class RaOutboundResourceAdapter {

    private String transactionSupport;

    private String reauthenticationSupport;

    @XmlElement(name = "connection-definition")
    private final List<RaConnectionDefinition> connectionDefinitions = new LinkedList<RaConnectionDefinition>();

    private List<RaAuthenticationMechanism> authenticationMechanisms = new LinkedList<RaAuthenticationMechanism>();

    @XmlID
    @XmlAttribute(name = "id")
    private String id;

    public String getTransactionSupport() {
        return transactionSupport;
    }

    @XmlElement(name = "transaction-support")
    public void setTransactionSupport(String transSup) {
        transactionSupport = transSup == null ? null : Enum.valueOf(TransactionSupportLevel.class, transSup).name();
    }

    public String getReauthenticationSupport() {
        return reauthenticationSupport;
    }

    @XmlElement(name = "reauthentication-support")
    public void setReauthenticationSupport(String reauthenticationSupport) {
        this.reauthenticationSupport = reauthenticationSupport;
    }

    public List<RaConnectionDefinition> getConnectionDefinitions() {
        return connectionDefinitions;
    }

    public List<RaAuthenticationMechanism> getAuthenticationMechanisms() {
        return authenticationMechanisms;
    }

    @XmlElement(name = "authentication-mechanism")
    public void setAuthenticationMechanisms(List<RaAuthenticationMechanism> authenticationMechanisms) {
        this.authenticationMechanisms = authenticationMechanisms;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RaOutboundResourceAdapter{");
        if (transactionSupport != null)
            sb.append("transaction-support='").append(transactionSupport).append("' ");
        if (reauthenticationSupport != null)
            sb.append("reauthentication-support").append(reauthenticationSupport).append("' ");
        sb.append("}");
        return sb.toString();
    }

    public RaConnectionDefinition getConnectionDefinitionByInterface(String connectionFactoryInterface) {
        for (RaConnectionDefinition connectionDefinition : connectionDefinitions)
            if (connectionDefinition.getConnectionFactoryInterface().equals(connectionFactoryInterface))
                return connectionDefinition;

        return null;
    }

    public void copyRa10Settings(Ra10ResourceAdapter ra10resourceAdapter) {
        RaConnectionDefinition definition = new RaConnectionDefinition();
        definition.copyRa10Settings(ra10resourceAdapter);
        connectionDefinitions.add(definition);
        for (Ra10AuthenticationMechanism mech : ra10resourceAdapter.getAuthMechanisms()) {
            RaAuthenticationMechanism authMechanism = new RaAuthenticationMechanism();
            authMechanism.copyRa10Settings(mech);
            authenticationMechanisms.add(authMechanism);
        }
        reauthenticationSupport = ra10resourceAdapter.getReauthenticationSupport();
        transactionSupport = ra10resourceAdapter.getTransactionSupport();
    }

}
