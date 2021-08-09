/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.ejbcontainer.jakarta.test.tools;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueReceiver;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import jakarta.jms.TopicPublisher;
import jakarta.jms.TopicSession;
import jakarta.jms.TopicSubscriber;

/**
 * @author alvinso
 *
 *         To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 *
 *         This class provides the methods for sending and receiving JMS
 *         messages.
 */
public class FATJMSHelper {

    private static final String CLASS_NAME = FATJMSHelper.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final int CONNECTION_RETRY_COUNT = 9;

    /**
     * connection factory
     */
    private QueueConnectionFactory qcf = null;

    /**
     * connection
     */
    private QueueConnection qc = null;

    /**
     * queue session
     */
    private QueueSession queueSession = null;

    /**
     * queue sender
     */
    private QueueSender sender = null;

    /**
     * queue receiver
     */
    private QueueReceiver receiver = null;

    private TopicPublisher pub = null;
    private TopicSession topicSession = null;
    private TopicConnection tc = null;
    private TopicSubscriber topicSubscriber = null;
    private final String wasUser = "none";
    private final String wasPasswd = "none";

    private TopicConnectionFactory tcf = null;

    private Topic topic = null;

    private static boolean svAbandonRetry = false;

    /**
     * constructor
     *
     * @throws Exception
     */
    public FATJMSHelper() throws Exception {

        super();

//      Properties props = FATHelper.loadProperties();
//      wasUser = props.getProperty("was.user");
//      wasPasswd = props.getProperty("was.passwd");
    }

    /**
     * Look up the Resource from JNDI
     *
     * @param name
     *            : JNDI context
     * @return Object of the resource
     * @throws NamingException
     */
    public Object lookupResource(String name) throws NamingException {
        InitialContext ic = new InitialContext();
        return ic.lookup(name);
    }

    /**
     * Send text message
     *
     * @param text
     *            : message
     * @return message ID
     * @throws JMSException
     */
    public String sendMessageToQueue(Object text) throws JMSException {
        TextMessage message = null;
        ObjectMessage objectMessage = null;
        String id = null;
        try {
            // Create a message to send to the queue...

            svLogger.info("Sending message to queue ...");

            if (text instanceof String) {
                message = queueSession.createTextMessage((String) text);
                sender.send(message);
                svLogger.info("Message :'" + text + "' is sent to the queue.");
                id = message.getJMSMessageID();
            } else if (text instanceof Vector) {
                objectMessage = queueSession.createObjectMessage((Vector<?>) text);
                sender.send(objectMessage);
                svLogger.info("Message is sent to the queue.");
                id = objectMessage.getJMSMessageID();
            } else {
                objectMessage = queueSession.createObjectMessage((Serializable) text);
                sender.send(objectMessage);
                svLogger.info("Message is sent to the queue.");
                id = objectMessage.getJMSMessageID();
            }

        } catch (JMSException je) {
            svLogger.info("sendTextMessage failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
        return id;
    }

    /**
     * Send text message
     *
     * @param text
     *            : message
     * @param jmsType
     *            : Set the JMS type
     * @return message ID
     * @throws JMSException
     */
    public String sendMessageToQueue(Object text, String jmsType) throws JMSException {
        TextMessage message = null;
        ObjectMessage objectMessage = null;
        String id = null;
        try {
            // Create a message to send to the queue...

            svLogger.info("Sending message to queue ...");

            if (text instanceof String) {
                message = queueSession.createTextMessage((String) text);
                message.setJMSType(jmsType);
                sender.send(message);
                svLogger.info("Message :'" + text + "' is sent to the queue.");
                id = message.getJMSMessageID();
            } else if (text instanceof Vector) {
                objectMessage = queueSession.createObjectMessage((Vector<?>) text);
                objectMessage.setJMSType(jmsType);
                sender.send(objectMessage);
                svLogger.info("Message is sent to the queue.");
                id = objectMessage.getJMSMessageID();
            } else {
                objectMessage = queueSession.createObjectMessage((Serializable) text);
                objectMessage.setJMSType(jmsType);
                sender.send(objectMessage);
                svLogger.info("Message is sent to the queue.");
                id = objectMessage.getJMSMessageID();
            }

        } catch (JMSException je) {
            svLogger.info("sendTextMessage failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
        return id;
    }

    // 519706 BEGIN
    public int getMessageBacklogSize(String queue) throws Exception {
        int backlogSize = 0;

        try {
            Queue receiveQueue = (Queue) lookupResource(queue);
            QueueBrowser queueBrowser = queueSession.createBrowser(receiveQueue);
            Enumeration<?> msgEnum = queueBrowser.getEnumeration();

            // Cycle through the Enumeration, since it does not have a size()
            // method.
            while (msgEnum.hasMoreElements()) {
                backlogSize++;
                msgEnum.nextElement();
            }

            queueBrowser.close();
        } catch (Exception t) {
            svLogger.info("FATJMSHelper.getMessageBacklogSize() failed with " + t);
            throw t;
        }

        return backlogSize;
    }

    // 519706 END

    /**
     * Get text message
     *
     * @param timeOut
     *            : maximum time out for getting message from queue
     * @return object of the message
     * @throws JMSException
     */
    public Object getMessage(int timeOut) throws JMSException {
        Object replyObj = null;

        try {

            // Get message
            Message inMessage = receiver.receive(timeOut);
            //
            // Check to see if the receive call has actually returned a
            // message. If it hasn't, report this and throw an exception...
            //
            if (inMessage == null) {
                svLogger.info("The attempt to read the message failed.  Maximum timeout is reached. ("
                              + timeOut + "ms.)");
                return null;
            } else {
                //
                // ...report on the message
                //
                svLogger.info("\n" + "Got message" + ": " + inMessage);

                if (inMessage instanceof TextMessage) {
                    //
                    // Extract the message content with getText()
                    //
                    replyObj = ((TextMessage) inMessage).getText();
                } else if (inMessage instanceof ObjectMessage) {
                    //
                    // Extract the message content with getText()
                    //
                    replyObj = ((ObjectMessage) inMessage).getObject();
                } else {
                    //
                    // Report that the incoming message was not of the expected
                    // type, and throw an exception
                    //
                    svLogger.info("Reply message was not a TextMessage or ObjectMessage.");
                    return null;
                }
            }
        } catch (JMSException je) {
            svLogger.info("getMessage failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
        return replyObj;
    }

    /**
     * Create queue connection
     *
     * @param queueConnectionFactory
     *            : JNDI context
     * @throws Exception
     */
    public void createQueueConnection(String queueConnectionFactory) throws Exception {
        //
        // Obtain the connection factory from JNDI
        //
        svLogger.info("Find QueueConnectionFactory '" + queueConnectionFactory + "'.");

        // At least for now, it can sometimes take JetStream several minutes
        // to get up and running on ND... so retry for up to 5 minutes. d233071
        int retry = 0;

        while (qc == null && retry < CONNECTION_RETRY_COUNT && !svAbandonRetry) {
            ++retry;
            try {
                if (retry > 1) {
                    svLogger.info("Retry in 15 seconds");
                    Thread.sleep(15000); // 15 seconds
                }

                qcf = (QueueConnectionFactory) lookupResource(queueConnectionFactory);

                //
                // Create a connection
                //
                svLogger.info("Creating connection ...");
                if (!(wasUser.equalsIgnoreCase("none") || wasPasswd.equalsIgnoreCase("none")))
                    qc = qcf.createQueueConnection(wasUser, wasPasswd);
                else
                    qc = qcf.createQueueConnection();

                if (qc == null) {
                    svLogger.info("Queue connection is null!!");
                } else {
                    svLogger.info("Starting connection...");
                    qc.start();
                }
            } catch (Exception e) {
                if (retry == CONNECTION_RETRY_COUNT - 1) {
                    svLogger.info("Abandon retry logic");
                    svAbandonRetry = true;
                    throw e;
                } else {
                    svLogger.info("Attempt #" + retry + " failed");
                }
            }
        }
    }

    // d433583, add an overload method
    // createQueueConnection(QueueConnectionFactory qcf)
    /**
     * Create queue connection
     *
     * @param qcf
     *            : QueueConnectionFactory
     * @throws Exception
     */
    public void createQueueConnection(QueueConnectionFactory qcfact) throws Exception {

        // At least for now, it can sometimes take JetStream several minutes
        // to get up and running on ND... so retry for up to 5 minutes. d233071
        int retry = 0;

        while (qc == null && retry < CONNECTION_RETRY_COUNT && !svAbandonRetry) {
            ++retry;
            try {
                if (retry > 1) {
                    svLogger.info("Retry in 15 seconds");
                    Thread.sleep(15000); // 15 seconds
                }

                qcf = qcfact;

                //
                // Create a connection
                //
                svLogger.info("Creating connection ...");
                if (!(wasUser.equalsIgnoreCase("none") || wasPasswd.equalsIgnoreCase("none")))
                    qc = qcf.createQueueConnection(wasUser, wasPasswd);
                else
                    qc = qcf.createQueueConnection();

                if (qc == null) {
                    svLogger.info("Queue connection is null!!");
                } else {
                    svLogger.info("Starting connection...");
                    qc.start();
                }
            } catch (Exception e) {
                if (retry == CONNECTION_RETRY_COUNT - 1) {
                    svLogger.info("Abandon retry logic");
                    svAbandonRetry = true;
                    throw e;
                } else {
                    svLogger.info("Attempt #" + retry + " failed");
                }
            }
        }
    }

    /**
     * create queue session
     *
     * @throws JMSException
     */
    public void createQueueSession() throws JMSException {
        boolean transacted = false;

        //
        // Create a session.
        //
        try {
            svLogger.info("Creating queue session ...");
            if (qc == null) {
                svLogger.info("Queue connection is null!!");
                throw new JMSException("Queue connection is null.");
            }
            queueSession = qc.createQueueSession(transacted,
                                                 Session.AUTO_ACKNOWLEDGE);
            if (queueSession == null) {
                svLogger.info("Queue session is null!!");
                throw new JMSException("Queue session is null.");
            }
        } catch (JMSException je) {
            svLogger.info("createQueueSession failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
    }

    /**
     * create queue sender
     *
     * @param queue
     *            : JNDI context
     * @throws JMSException
     * @throws NamingException
     */
    public void createQueueSender(String queue) throws JMSException, NamingException {
        //
        // Obtain the Destination object from JNDI
        //
        svLogger.info("Find Queue '" + queue + "'.");
        try {
            Queue sendQueue = (Queue) lookupResource(queue);

            //
            // Create a QueueSender
            //
            svLogger.info("Create queue sender + " + queue + " ..." + sendQueue.toString());
            sender = queueSession.createSender(sendQueue);
        } catch (JMSException je) {
            svLogger.info("createQueueSender failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
    }

    // d433583, add an overload method, createQueueSender(Queue queue)
    /**
     * create queue sender
     *
     * @param queue
     *            : Queue
     * @throws JMSException
     */
    public void createQueueSender(Queue queue) throws JMSException {

        try {
            // Create a QueueSender
            svLogger.info("Create queue sender ... " + queue.toString() + ".");
            sender = queueSession.createSender(queue);
        } catch (JMSException je) {
            svLogger.info("createQueueSender failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
    }

    /**
     * create queue receiver
     *
     * @param queue
     *            : JNDI context
     * @throws NamingException
     * @throws JMSException
     */
    public void createQueueReceiver(String queue) throws NamingException, JMSException {
        //
        // Obtain the Destination object from JNDI
        //
        svLogger.info("Find Queue '" + queue + "'.");
        try {
            Queue receiveQueue = (Queue) lookupResource(queue);

            //
            // Create a QueueReceiver
            //
            svLogger.info("Create queue receiver ...");
            receiver = queueSession.createReceiver(receiveQueue);
        } catch (JMSException je) {
            svLogger.info("createQueueReceiver failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
    }

    /**
     * close queue connection
     *
     * @throws JMSException
     */
    public void closeQueueConnection() throws JMSException {
        //
        // Ensure that the Sender always gets closed
        //

        svLogger.info("Closing connection ...");

        if (sender != null) {
            try {
                sender.close();
                sender = null;
            } catch (JMSException jmse) {
            }
        }

        if (receiver != null) {
            try {
                receiver.close();
                receiver = null;
            } catch (JMSException jmse) {
            }
        }

        //
        // Ensure that the Session always gets closed
        //
        if (queueSession != null) {
            try {
                queueSession.close();
                queueSession = null;
            } catch (JMSException jmse) {
                svLogger.info("Queue Session cannot be closed :" + jmse);
                throw jmse;
            }
        }
        //
        // Ensure that the Connection always gets closed
        //
        if (qc != null) {
            try {
                qc.close();
                qc = null;
            } catch (JMSException je) {
                svLogger.info("Connection cannot be closed :" + je);
                throw je;
            }
        }
    }

    /**
     * Create topic connection
     *
     * @param topicConnectionFactory
     *            : JNDI context
     * @throws JMSException
     * @throws NamingException
     * @throws InterruptedException
     */
    public void createTopicConnection(String topicConnectionFactory) throws JMSException, NamingException, InterruptedException {
        //
        // Obtain the connection factory from JNDI
        //
        svLogger.info("Find TopicConnectionFactory '" + topicConnectionFactory
                      + "'.");

        // At least for now, it can sometimes take JetStream several minutes
        // to get up and running on ND... so retry for up to 10 minutes. d233071
        int retry = 0;

        while (tc == null && retry < CONNECTION_RETRY_COUNT) // d245115
        {
            ++retry;
            try {
                if (retry > 1) {
                    svLogger.info("Retry in 1 minute");
                    Thread.sleep(30000); // 30 seconds
                }

                tcf = (TopicConnectionFactory) lookupResource(topicConnectionFactory);

                //
                // Create a connection
                //
                svLogger.info("Creating connection ...");
                if (!(wasUser.equalsIgnoreCase("none") || wasPasswd.equalsIgnoreCase("none")))
                    tc = tcf.createTopicConnection(wasUser, wasPasswd);
                else
                    tc = tcf.createTopicConnection();
                svLogger.info("Starting connection ...");
                tc.start();
            } catch (JMSException je) {
                svLogger.info("createTopicConnection failed with " + je);
                Exception le = je.getLinkedException();
                if (le != null)
                    svLogger.info("linked exception " + le);
                throw je;
            }
        }
    }

    /**
     * create topic session
     *
     * @throws JMSException
     */
    public void createTopicSession() throws JMSException {
        boolean transacted = false;

        //
        // Create a session.
        //
        try {
            svLogger.info("Creating topic session ...");

            topicSession = tc.createTopicSession(transacted,
                                                 Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException je) {
            svLogger.info("createTopicSession failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
    }

    /**
     * create Topic Publisher
     *
     * @param tname
     *            : JNDI context
     * @throws JMSException
     * @throws NamingException
     */
    public void createTopicPublisher(String tname) throws JMSException, NamingException {
        //
        // Obtain the Destination object from JNDI
        //
        svLogger.info("Find Topic '" + tname + "'.");
        try {

            // Attempt to retrieve Topic from the JNDI namespace
            //
            topic = (Topic) lookupResource(tname);

            // Use the session to create a TopicPublisher, passing in the
            // destination (the Topic object) as a parameter
            //
            svLogger.info("Creating a TopicPublisher ...");

            pub = topicSession.createPublisher(topic);

        } catch (JMSException je) {
            svLogger.info("createTopicPublisher failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
    }

    /**
     * Send text message to topic
     *
     * @param text
     *            : message
     * @throws JMSException
     */
    public void sendMessageToTopic(String text) throws JMSException {
        try {
            //
            // Use the session to create messages, create an empty TextMessage
            // and add the data passed.
            //

            svLogger.info("Creating a TextMessage ...");

            TextMessage outMessage = topicSession.createTextMessage(text);

            // Ask the TopicPublisher to send the message we have created
            //

            svLogger.info("Publish the message to " + topic.getTopicName());

            pub.publish(outMessage);

        } catch (JMSException je) {
            svLogger.info("sendTextMessage failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
    }

    /**
     * Send text message to topic
     *
     * @param text
     *            : message
     * @param jmsType
     *            : set the JMS type
     * @throws JMSException
     */
    public void sendMessageToTopic(String text, String jmsType) throws JMSException {
        try {
            //
            // Use the session to create messages, create an empty TextMessage
            // and add the data passed.
            //

            svLogger.info("Creating a TextMessage ...");

            TextMessage outMessage = topicSession.createTextMessage(text);

            // Ask the TopicPublisher to send the message we have created
            //

            svLogger.info("Publish the message to " + topic.getTopicName());

            outMessage.setJMSType(jmsType);
            pub.publish(outMessage);

        } catch (JMSException je) {
            svLogger.info("sendTextMessage failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
    }

    // Begin 496568

    public class FATTopicConnection {
        private String name = null;

        private FATJMSHelper jmsHelper = null;

        private TopicConnectionFactory tcf = null;
        private TopicConnection tc = null;
        private TopicSession ts = null;

        private Topic subscribedTopic = null;
        private TopicSubscriber tsub = null;

        public FATTopicConnection(String name, FATJMSHelper helper) {
            this.name = name;
            this.jmsHelper = helper;
        }

        public String getName() {
            return name;
        }

        public Topic getSubscribedTopic() {
            return subscribedTopic;
        }

        public void setSubscribedTopic(Topic subscribedTopic) {
            this.subscribedTopic = subscribedTopic;
        }

        public TopicConnection getTc() {
            return tc;
        }

        public void setTc(TopicConnection tc) {
            this.tc = tc;
        }

        public TopicConnectionFactory getTcf() {
            return tcf;
        }

        public void setTcf(TopicConnectionFactory tcf) {
            this.tcf = tcf;
        }

        public TopicSession getTs() {
            return ts;
        }

        public void setTs(TopicSession ts) {
            this.ts = ts;
        }

        public TopicSubscriber getTsub() {
            return tsub;
        }

        public void setTsub(TopicSubscriber tsub) {
            this.tsub = tsub;
        }

        public FATJMSHelper getJmsHelper() {
            return jmsHelper;
        }
    }

    private static Hashtable<String, FATTopicConnection> fatTopicConnectionHashTable = new Hashtable<String, FATTopicConnection>();

    public void createFATTopicConnection(String name) {
        FATTopicConnection newFATTopicConnection = new FATTopicConnection(name, this);

        fatTopicConnectionHashTable.put(name, newFATTopicConnection);
    }

    public static void removeFATTopicConnection(String name) throws JMSException {
        getFATTopicConnection(name).getJmsHelper().closeTopicConnection();
        fatTopicConnectionHashTable.remove(name);
    }

    protected static FATTopicConnection getFATTopicConnection(String name) {
        return fatTopicConnectionHashTable.get(name);
    }

    /**
     * Create topic connection
     *
     * @param topicConnectionFactory
     *            : JNDI context
     * @param fatTopicConnectionName
     *            : name of topic connection
     * @throws InterruptedException
     * @throws NamingException
     * @throws JMSException
     */
    public void createTopicConnection(String topicConnectionFactory,
                                      String fatTopicConnectionName) throws InterruptedException, NamingException, JMSException {
        FATTopicConnection fatTopicConnection = getFATTopicConnection(fatTopicConnectionName);
        if (fatTopicConnection == null) {
            svLogger.info("*** Failed to find FATTopicConnection.");
            throw new JMSException("Failed to find FATTopicConnection.");
        }
        //
        // Obtain the connection factory from JNDI
        //
        svLogger.info("Find TopicConnectionFactory '" + topicConnectionFactory
                      + "'.");

        // At least for now, it can sometimes take JetStream several minutes
        // to get up and running on ND... so retry for up to 10 minutes. d233071
        int retry = 0;

        while (fatTopicConnection.getTc() == null && retry < CONNECTION_RETRY_COUNT) // ( tc ==
        // null &&
        // retry < 20
        // ) //
        // d245115
        {
            ++retry;
            try {
                if (retry > 1) {
                    svLogger.info("Retry in 1 minute");
                    Thread.sleep(30000); // 30 seconds
                }

                fatTopicConnection.setTcf((TopicConnectionFactory) lookupResource(topicConnectionFactory));
                // tcf = (TopicConnectionFactory)
                // lookupResource(topicConnectionFactory);

                //
                // Create a connection
                //
                svLogger.info("Creating connection ...");
                if (!(wasUser.equalsIgnoreCase("none") || wasPasswd.equalsIgnoreCase("none")))
                    // tc = tcf.createTopicConnection(FATCommon.getDBUser(),
                    // FATCommon.getDBPassword());
                    fatTopicConnection.setTc(fatTopicConnection.getTcf().createTopicConnection(wasUser, wasPasswd));
                else
                    // tc = tcf.createTopicConnection();
                    fatTopicConnection.setTc(fatTopicConnection.getTcf().createTopicConnection());
                svLogger.info("Starting connection ...");
                fatTopicConnection.getTc().start();
                // tc.start();
            } catch (JMSException je) {
                svLogger.info("createTopicConnection failed with " + je);
                Exception le = je.getLinkedException();
                if (le != null)
                    svLogger.info("linked exception " + le);
                throw je;
            }
        }
    }

    /**
     * create topic session
     *
     * @param fatTopicConnectionName
     *            : name of topic connection
     * @throws JMSException
     */
    public void createTopicSession(String fatTopicConnectionName) throws JMSException {
        FATTopicConnection fatTopicConnection = getFATTopicConnection(fatTopicConnectionName);

        boolean transacted = false;

        //
        // Create a session.
        //
        try {
            svLogger.info("Creating topic session ...");

            fatTopicConnection.setTs(fatTopicConnection.getTc().createTopicSession(transacted, Session.AUTO_ACKNOWLEDGE));
            // tc.createTopicSession(transacted, Session.AUTO_ACKNOWLEDGE));
            // topicSession =
            // tc.createTopicSession(transacted, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException je) {
            svLogger.info("createTopicSession failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
    }

    /**
     * create topic receiver
     *
     * @param topic
     *            : JNDI context
     * @throws JMSException
     * @throws NamingException
     */
    public void createTopicSubcriber(String topic) throws JMSException, NamingException {
        //
        // Obtain the Destination object from JNDI
        //
        svLogger.info("Find Topic '" + topic + "'.");
        try {
            Topic receiveTopic = (Topic) lookupResource(topic);

            //
            // Create a TopicSubscriber
            //
            svLogger.info("Create topic subscriber ...");
            setTopicSubscriber(topicSession.createSubscriber(receiveTopic));
        } catch (JMSException je) {
            svLogger.info("createTopicReceiver failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
    }

    /**
     * create topic receiver
     *
     * @param topic
     *            : JNDI context
     * @param fatTopicConnectionName
     *            : name of topic connection
     * @throws JMSException
     * @throws NamingException
     */
    public void createTopicSubcriber(String topic, String fatTopicConnectionName) throws JMSException, NamingException {
        FATTopicConnection fatTopicConnection = getFATTopicConnection(fatTopicConnectionName);

        //
        // Obtain the Destination object from JNDI
        //
        svLogger.info("Find Topic '" + topic + "'.");
        try {
            Topic receiveTopic = (Topic) lookupResource(topic);

            fatTopicConnection.setSubscribedTopic(receiveTopic);
            //
            // Create a TopicSubscriber
            //
            svLogger.info("Create topic subscriber ...");
            // topicSubscriber = topicSession.createSubscriber(receiveTopic);

            fatTopicConnection.setTsub(fatTopicConnection.getTs().createSubscriber(receiveTopic));
            // topicSession.createSubscriber(receiveTopic));
        } catch (JMSException je) {
            svLogger.info("createTopicReceiver failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);

            throw je;
        }
    }

    /**
     * Get text message from a topic
     *
     * @param timeOut
     *            : maximum time out for getting message from topic
     * @param fatTopicConnectionName
     *            : name of topic connection
     * @return object of the message
     * @throws JMSException
     */
    public Object getTopicMessage(int timeOut, String fatTopicConnectionName) throws JMSException {
        FATTopicConnection fatTopicConnection = getFATTopicConnection(fatTopicConnectionName);
        Object replyObj = null;

        try {

            // Get message
            Message inMessage = fatTopicConnection.getTsub().receive(timeOut); // topicSubscriber.receive(timeOut);
            //
            // Check to see if the receive call has actually returned a
            // message. If it hasn't, report this and throw an exception...
            //
            if (inMessage == null) {
                svLogger.info("The attempt to read the message failed.  Maximum timeout is reached. ("
                              + timeOut + "ms.)");
                return null;
            } else {
                //
                // ...report on the message
                //
                svLogger.info("\n" + "Got message" + ": " + inMessage);

                if (inMessage instanceof TextMessage) {
                    //
                    // Extract the message content with getText()
                    //
                    replyObj = ((TextMessage) inMessage).getText();
                } else if (inMessage instanceof ObjectMessage) {
                    //
                    // Extract the message content with getText()
                    //
                    replyObj = ((ObjectMessage) inMessage).getObject();
                } else {
                    //
                    // Report that the incoming message was not of the expected
                    // type, and throw an exception
                    //
                    svLogger.info("Reply message was not a TextMessage or ObjectMessage.");
                    return inMessage; // null;
                }
            }
        } catch (JMSException je) {
            svLogger.info("getTopicMessage failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }
        return replyObj;
    }

    /**
     * Send text message to topic
     *
     * @param obj
     *            : message
     * @param jmsType
     *            : set the JMS type
     * @return message ID
     * @throws JMSException
     */
    public String sendObjectMessageToTopic(Serializable obj, String jmsType) throws JMSException {
        String messageID = null;
        try {
            //
            // Use the session to create messages, create an empty TextMessage
            // and add the data passed.
            //

            svLogger.info("Creating a ObjectMessage ...");

            ObjectMessage outMessage = topicSession.createObjectMessage();

            // Ask the TopicPublisher to send the message we have created
            //

            svLogger.info("Publish the  ObjectMessage to " + topic.getTopicName());

            messageID = outMessage.getJMSMessageID();

            outMessage.setObject(obj);
            outMessage.setJMSType(jmsType);

            pub.publish(outMessage);

        } catch (JMSException je) {
            svLogger.info("sendTextMessage failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }

        return messageID;
    }

    /**
     * Send text message to topic
     *
     * @param obj
     *            : message
     * @param jmsType
     *            : set the JMS type
     * @param replyToTopicName
     *            : name of Reply To Topic
     * @return message ID
     * @throws JMSException
     * @throws NamingException
     */
    public String sendObjectMessageToTopicWithReplyTo(Serializable obj,
                                                      String jmsType, String replyToTopicName) throws JMSException, NamingException {
        String messageID = null;
        try {
            //
            // Use the session to create messages, create an empty TextMessage
            // and add the data passed.
            //

            svLogger.info("Creating a ObjectMessage with ReplyTo...");

            ObjectMessage outMessage = topicSession.createObjectMessage();

            // Ask the TopicPublisher to send the message we have created
            //

            svLogger.info("Publish the  ObjectMessage with ReplyTo to "
                          + topic.getTopicName());

            messageID = outMessage.getJMSMessageID();

            outMessage.setObject(obj);
            outMessage.setJMSType(jmsType);

            // Reply To Topic

            Topic replyToTopic = (Topic) lookupResource(replyToTopicName);
            svLogger.info("Setting ReplyTo Topic: " + replyToTopic);
            outMessage.setJMSReplyTo(replyToTopic);

            pub.publish(outMessage);

        } catch (JMSException je) {
            svLogger.info("sendTextMessage failed with " + je);
            Exception le = je.getLinkedException();
            if (le != null)
                svLogger.info("linked exception " + le);
            throw je;
        }

        return messageID;
    }

    // End 496568

    /**
     * close topic connection
     *
     * @throws JMSException
     */
    public void closeTopicConnection() throws JMSException {
        //
        // Ensure that the Sender always gets closed
        //

        svLogger.info("Closing connection ...");

        //
        // Ensure the Topic Publisher is closed
        //
        if (pub != null) {
            svLogger.info("Closing Publisher");
            pub.close();
            pub = null;
            topic = null;
        }

        //
        // Ensure the Topic Session is closed
        //
        if (topicSession != null) {
            svLogger.info("Closing session");
            topicSession.close();
            topicSession = null;
        }

        //
        // Ensure the Topic Connection is closed
        //
        if (tc != null) {
            svLogger.info("Closing connection");
            tc.close();
            tc = null;
        }
    }

    public void setTopicSubscriber(TopicSubscriber topicSubscriber) {
        this.topicSubscriber = topicSubscriber;
    }

    public TopicSubscriber getTopicSubscriber() {
        return topicSubscriber;
    }
}
