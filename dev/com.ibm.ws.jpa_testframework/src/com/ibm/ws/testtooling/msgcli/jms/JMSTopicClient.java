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
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.NamingException;

import com.ibm.ws.testtooling.msgcli.DataPacket;
import com.ibm.ws.testtooling.msgcli.MessagingException;

public class JMSTopicClient extends AbstractJMSClient {
    private TopicConnectionFactory tcf = null;
    private Topic topicSender = null;
    private Topic topicReceiver = null;

    private TopicConnection tConn = null;
    private TopicSession tSession = null;
    private TopicSubscriber tSub = null;
    private TopicPublisher tPub = null;

    private String username = null;
    private String password = null;

    JMSTopicClient(String identity, JMSClientConfig jmsClientCfg) throws NamingException, MessagingException {
        super(identity);

        String tcfName = jmsClientCfg.getConnectionFactoryName();
        if (tcfName == null) {
            throw new MessagingException("No TopicConnectionFactory specified in JMSClientConfig");
        }
        tcf = (TopicConnectionFactory) ic.lookup(tcfName);

        username = jmsClientCfg.getUsername();
        password = jmsClientCfg.getPassword();

        String receiver = jmsClientCfg.getReceiverName();
        if (receiver != null) {
            topicReceiver = (Topic) ic.lookup(receiver);
        }

        String sender = jmsClientCfg.getSenderName();
        if (sender != null) {
            topicSender = (Topic) ic.lookup(sender);
        }
    }

    @Override
    protected void doClose() {
        if (tSub != null) {
            setCanReceiveMessages(false);
            try {
                tSub.close();
            } catch (Throwable t) {

            }
            tSub = null;
        }

        if (tPub != null) {
            setCanTransmitMessages(false);
            try {
                tPub.close();
            } catch (Throwable t) {

            }
            tPub = null;
        }

        if (tSession != null) {
            try {
                tSession.close();
            } catch (Throwable t) {

            }
            tSession = null;
        }

        if (tConn != null) {
            try {
                tConn.close();
            } catch (Throwable t) {

            }
            tConn = null;
        }
    }

    /**
     * Transmits a packet to receivers.
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
            Message message = tSession.createObjectMessage((Serializable) packet);
            tPub.send(message);
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
            Message message = tSub.receive(jmsTimeout);

            if (message == null) {
                // No message received within the timeout period.  Emit a message to SystemOut so that there is
                // an eyecatcher in the server log.
                System.out.println("JMSTopicClient \"" + this.clientIdentity() + "\" did not receive a message " +
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
            System.out.println("JMSTopicClient \"" + this.clientIdentity() + "\" received unknown Message: " + message);
            return null;
        } catch (Throwable t) {
            throw new MessagingException(t);
        }
    }

    @Override
    protected void initialize() throws MessagingException {
        try {
            if (username == null) {
                tConn = tcf.createTopicConnection();
            } else {
                tConn = tcf.createTopicConnection(username, password);
            }

            tConn.start();
            tSession = (TopicSession) tConn.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            if (topicSender != null) {
                tPub = (TopicPublisher) tSession.createPublisher(topicSender);
                setCanTransmitMessages(true);
            }

            if (topicReceiver != null) {
                tSub = (TopicSubscriber) tSession.createSubscriber(topicReceiver);
                setCanReceiveMessages(true);
            }
        } catch (Throwable t) {
            throw new MessagingException(t);
        }
    }

    @Override
    public String toString() {
        return "JMSTopicClient [tcf=" + tcf + ", topicSender=" + topicSender
               + ", topicReceiver=" + topicReceiver + ", tConn=" + tConn
               + ", tSession=" + tSession + ", tSub=" + tSub + ", tPub="
               + tPub + ", username=" + username + ", password=" + password
               + ", clientIdentity()=" + clientIdentity()
               + ", canReceiveMessages()=" + canReceiveMessages()
               + ", canTransmitMessages()=" + canTransmitMessages()
               + " " + super.toString()
               + "]";
    }
}