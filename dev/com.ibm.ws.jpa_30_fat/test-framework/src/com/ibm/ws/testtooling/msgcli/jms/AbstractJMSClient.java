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
package com.ibm.ws.testtooling.msgcli.jms;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.ws.testtooling.msgcli.DataPacket;
import com.ibm.ws.testtooling.msgcli.MessagingClient;
import com.ibm.ws.testtooling.msgcli.MessagingException;
import com.ibm.ws.testtooling.testinfo.TestSessionSignature;

public abstract class AbstractJMSClient implements MessagingClient {
    protected InitialContext ic = null;

    private String identity;
    private boolean canReceiveMessages = false;
    private boolean canSendMessages = false;

    protected AbstractJMSClient(String identity) throws NamingException {
        ic = new InitialContext();

        this.identity = identity;
    }

    @Override
    public void close() {
        try {
            doClose();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        try {
            ic.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public final String clientIdentity() {
        return identity;
    }

    @Override
    public final boolean canReceiveMessages() {
        return canReceiveMessages;
    }

    @Override
    public final boolean canTransmitMessages() {
        return canSendMessages;
    }

    @Override
    public void transmitPacket(DataPacket packet) throws MessagingException {
        if (canSendMessages) {
            System.out.println("JMSClient: Transmitting packet using " + super.toString());
            doTransmitPacket(packet);
        }
    }

    @Override
    public DataPacket receivePacket(long timeout) throws MessagingException {
        System.out.println("JMSClient: Receiving packet using " + super.toString());
        if (canReceiveMessages) {
            return doReceivePacket(timeout);
        } else {
            return null;
        }
    }

    @Override
    public DataPacket receivePacket(long timeout, TestSessionSignature testSessionSig) throws MessagingException {
        if (canReceiveMessages && testSessionSig != null) {
            DataPacket rcvPacket = null;

            long startWaitTime = System.currentTimeMillis();
            long endWaitTime = startWaitTime + timeout;
            long currentTime = startWaitTime;

            do {
                if (timeout == -1) {
                    rcvPacket = doReceivePacket(-1);
                } else if (timeout == 0) {
                    rcvPacket = doReceivePacket(0);
                } else {
                    long calcWaitTime = endWaitTime - currentTime;
                    if (calcWaitTime < 0) {
                        // Prevent infinite waits
                        calcWaitTime = 0;
                    }
                    rcvPacket = doReceivePacket(calcWaitTime);
                }

                if (rcvPacket != null && testSessionSig.equals(rcvPacket.getTestSessionSig())) {
                    return rcvPacket;
                }

                currentTime = System.currentTimeMillis() + 1; // +1 just for time granularity diffs by OS
            } while (timeout == -1 || currentTime < endWaitTime);
        }

        return null;
    }

    protected final void setCanReceiveMessages(boolean val) {
        canReceiveMessages = val;
    }

    protected final void setCanTransmitMessages(boolean val) {
        canSendMessages = val;
    }

    protected abstract void initialize() throws MessagingException;

    protected abstract void doClose();

    protected abstract void doTransmitPacket(DataPacket packet) throws MessagingException;

    protected abstract DataPacket doReceivePacket(long timeout) throws MessagingException;
}