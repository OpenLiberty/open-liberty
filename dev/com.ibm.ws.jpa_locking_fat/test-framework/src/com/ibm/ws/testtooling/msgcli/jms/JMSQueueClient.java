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

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import com.ibm.ws.testtooling.msgcli.DataPacket;
import com.ibm.ws.testtooling.msgcli.MessagingException;

/**
 * Warning: Not thread safe. Not intended to be used by multiple threads at the same time.
 *
 */
public class JMSQueueClient extends AbstractJMSClient {
    private QueueConnectionFactory qcf = null;
    private Queue queueSender = null;
    private Queue queueReceiver = null;

    private QueueConnection qConn = null;
    private QueueSession qSession = null;
    private QueueReceiver qRcvr = null;
    private QueueSender qSndr = null;

    private String username = null;
    private String password = null;

    JMSQueueClient(String identity, JMSClientConfig jmsClientCfg) throws NamingException, MessagingException {
        super(identity);

        String qcfName = jmsClientCfg.getConnectionFactoryName();
        if (qcfName == null) {
            throw new MessagingException("No QueueConnectionFactory specified in JMSClientConfig");
        }
        qcf = (QueueConnectionFactory) ic.lookup(qcfName);

        username = jmsClientCfg.getUsername();
        password = jmsClientCfg.getPassword();

        String receiver = jmsClientCfg.getReceiverName();
        if (receiver != null) {
            queueReceiver = (Queue) ic.lookup(receiver);
        }

        String sender = jmsClientCfg.getSenderName();
        if (sender != null) {
            queueSender = (Queue) ic.lookup(sender);
        }
    }

    @Override
    protected void doClose() {
        if (qRcvr != null) {
            setCanReceiveMessages(false);
            try {
                qRcvr.close();
            } catch (Throwable t) {

            }
            qRcvr = null;
        }

        if (qSndr != null) {
            setCanTransmitMessages(false);
            try {
                qSndr.close();
            } catch (Throwable t) {

            }
            qSndr = null;
        }

        if (qSession != null) {
            try {
                qSession.close();
            } catch (Throwable t) {

            }
            qSession = null;
        }

        if (qConn != null) {
            try {
                qConn.close();
            } catch (Throwable t) {

            }
            qConn = null;
        }
    }

    /**
     * Transmits a packet to a receiver.
     *
     * @param packet
     * @throws JMSException
     */
    @Override
    protected void doTransmitPacket(DataPacket packet) throws MessagingException {
        if (packet == null) {
            // Nothing to transmit.
            return;
        }

        try {
            Message message = qSession.createObjectMessage(packet);
            qSndr.send(message);
        } catch (Throwable t) {
            throw new MessagingException(t);
        }
    }

    /**
     * Receives a packet from a transmitter.
     *
     * @param timeout - maximum time to wait, in ms. A value of 0 means no wait. A value of -1 means
     *                    to wait indefinitely.
     *
     * @return An instance of TestDataPacket if one was received. Returns null if no message was received
     *         by the time the timeout expired.
     * @throws JMSException
     */
    @Override
    protected DataPacket doReceivePacket(long timeout) throws MessagingException {
        long jmsTimeout = timeout;
        if (timeout == -1) {
            jmsTimeout = 0; // In JMS, 0 means wait indefinitely.  Use with care.
        } else if (timeout == 0) {
            jmsTimeout = 1; // In JMS, there is no fail-fast, so set timeout to the min value.
        }

        try {
            Message message = qRcvr.receive(jmsTimeout);

            if (message == null) {
                // No message received within the timeout period.  Emit a message to SystemOut so that there is
                // an eyecatcher in the server log.
                System.out.println("JMSQueueClient \"" + this.clientIdentity() + "\" did not receive a message " +
                                   "within " + jmsTimeout + " ms.");
                return null;
            }

            if (message instanceof ObjectMessage) {
                Serializable msgPayload = ((ObjectMessage) message).getObject();

                if (msgPayload instanceof DataPacket) {
                    // This should be the common scenario, with TestDataPacket being the payload
                    return (DataPacket) msgPayload;
                } else {
                    // Something other than a TestDataPacket came in.  Atypical scenario.  Wrap in a TestDataPacket.
                    DataPacket wrapperPacket = new DataPacket();
                    wrapperPacket.setPayload(msgPayload);
                    return wrapperPacket;
                }
            }

            if (message instanceof TextMessage) {
                // Another atypical scenario.  Wrap in a TestDataPacket.
                DataPacket wrapperPacket = new DataPacket();
                wrapperPacket.setPayload(((TextMessage) message).getText());
                return wrapperPacket;
            }

            // Unknown message type.  Log it and return null.
            System.out.println("JMSQueueClient \"" + this.clientIdentity() + "\" received unknown Message: " + message);
            return null;
        } catch (Throwable t) {
            throw new MessagingException(t);
        }
    }

    @Override
    protected void initialize() throws MessagingException {
        try {
            if (username == null) {
                qConn = qcf.createQueueConnection();
            } else {
                qConn = qcf.createQueueConnection(username, password);
            }

            qConn.start();
            qSession = qConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            if (queueSender != null) {
                qSndr = qSession.createSender(queueSender);
                setCanTransmitMessages(true);
            }

            if (queueReceiver != null) {
                qRcvr = qSession.createReceiver(queueReceiver);
                setCanReceiveMessages(true);
            }
        } catch (Throwable t) {
            throw new MessagingException(t);
        }
    }

    @Override
    public String toString() {
        return "JMSQueueClient [qcf=" + qcf + ", queueSender=" + queueSender
               + ", queueReceiver=" + queueReceiver + ", qConn=" + qConn
               + ", qSession=" + qSession + ", qRcvr=" + qRcvr + ", qSndr="
               + qSndr + ", username=" + username + ", password=" + password
               + ", clientIdentity()=" + clientIdentity()
               + ", canReceiveMessages()=" + canReceiveMessages()
               + ", canTransmitMessages()=" + canTransmitMessages()
               + " " + super.toString()
               + "]";
    }
}