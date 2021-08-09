/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testtooling.msgcli.smc;

import javax.ejb.Remove;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;

import com.ibm.ws.testtooling.msgcli.DataPacket;
import com.ibm.ws.testtooling.msgcli.MessagingClient;
import com.ibm.ws.testtooling.msgcli.MessagingException;
import com.ibm.ws.testtooling.msgcli.jms.JMSClientConfig;
import com.ibm.ws.testtooling.msgcli.jms.JMSClientFactory;
import com.ibm.ws.testtooling.testinfo.TestSessionSignature;

@TransactionManagement(javax.ejb.TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class StatefulMessengerClientBean implements MessagingClient {
    private String identity = null;

    private boolean fullDuplexMode = false; // If true, then transmitterClient = receiverClient
    private MessagingClient transmitterClient = null;
    private MessagingClient receiverClient = null;

    public void initialize(String clientName, JMSClientConfig fullDuplexConfig) throws MessagingException {
        identity = clientName;
        fullDuplexMode = true;

        MessagingClient mc = JMSClientFactory.createJMSMessagingClient(clientName, fullDuplexConfig);
        transmitterClient = mc;
        receiverClient = mc;
    }

    public void initialize(String clientName, JMSClientConfig receiver, JMSClientConfig sender) throws MessagingException {
        identity = clientName;
        fullDuplexMode = false;

        transmitterClient = JMSClientFactory.createJMSMessagingClient(clientName + "-sender", sender);
        receiverClient = JMSClientFactory.createJMSMessagingClient(clientName + "-receiver", receiver);
    }

    @Override
    public String clientIdentity() {
        return identity;
    }

    @Override
    public boolean canReceiveMessages() {
        if (receiverClient != null) {
            return receiverClient.canReceiveMessages();
        } else {
            return false;
        }
    }

    @Override
    public boolean canTransmitMessages() {
        if (transmitterClient != null) {
            return transmitterClient.canTransmitMessages();
        } else {
            return false;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void transmitPacket(DataPacket packet) throws MessagingException {
        transmitterClient.transmitPacket(packet);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public DataPacket receivePacket(long timeout) throws MessagingException {
        return receiverClient.receivePacket(timeout);
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public DataPacket receivePacket(long timeout, TestSessionSignature testSessionSig) throws MessagingException {
        return receiverClient.receivePacket(timeout, testSessionSig);
    }

    @Override
    @Remove
    public void close() {
        transmitterClient.close();

        if (!fullDuplexMode) {
            receiverClient.close();
        }
    }
}