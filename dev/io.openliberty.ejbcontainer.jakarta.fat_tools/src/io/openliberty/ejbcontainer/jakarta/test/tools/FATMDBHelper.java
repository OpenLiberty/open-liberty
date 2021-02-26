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

import java.util.logging.Logger;

import jakarta.jms.Queue;
import jakarta.jms.QueueConnectionFactory;

/**
 * FATHelper class provides helper methods that do not use
 * or require websphere application server classes.
 */
public abstract class FATMDBHelper {
    private final static String CLASS_NAME = FATMDBHelper.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    /**
     * Default maximum timeout (ms) for getting message from a queue.
     */
    private static int timeOut = 3 * 60 * 1000;

    /**
     * Put a message on a specified topic
     *
     * @param text
     *            : message
     * @param tcfName
     *            : JNDI context for topic connection factory
     * @param tname
     *            : JNDI context for the topic
     * @throws Exception
     */
    public static void putTopicMessage(String text, String tcfName, String tname) throws Exception {
        svLogger.info("Requests to put message '" + text + "' with Topic Connection Factory '" + tcfName + "' and Topic '" + tname + "'.");

        FATJMSHelper jms = new FATJMSHelper();

        // Create topic connection
        jms.createTopicConnection(tcfName);

        // Create topic session
        jms.createTopicSession();

        // Create topic publisher
        jms.createTopicPublisher(tname);

        // Send message to topic
        jms.sendMessageToTopic(text);

        // Close topic connection
        jms.closeTopicConnection();
    }

    /**
     * Put a message on a specified topic
     *
     * @param text
     *            : message
     * @param jmsType
     *            : set the JMS type
     * @param tcfName
     *            : JNDI context for topic connection factory
     * @param tname
     *            : JNDI context for the topic
     * @throws Exception
     */
    public static void putTopicMessage(String text, String jmsType, String tcfName, String tname) throws Exception {
        svLogger.info("Requests to put message '" + text + "' with Topic Connection Factory '" + tcfName + "' and Topic '" + tname + "'.");

        FATJMSHelper jms = new FATJMSHelper();

        // Create topic connection
        jms.createTopicConnection(tcfName);

        // Create topic session
        jms.createTopicSession();

        // Create topic publisher
        jms.createTopicPublisher(tname);

        // Send message to topic
        jms.sendMessageToTopic(text, jmsType);

        // Close topic connection
        jms.closeTopicConnection();
    }

    /**
     * Put a text message on a specified queue
     *
     * @param text
     *            : message
     * @param queueConnectionFactory
     *            : JNDI context for queue connection factory
     * @param queue
     *            : JNDI context for the queue
     * @return Message ID
     * @throws Exception
     */
    public static String putQueueMessage(Object text, String queueConnectionFactory, String queue) throws Exception {

        svLogger.info("Requests to put message '" + text + "' with Queue Connection Factory '" + queueConnectionFactory + "' and Queue '" + queue + "'.");
        String messageID = null;

        FATJMSHelper jms = new FATJMSHelper();

        // Create a connection
        jms.createQueueConnection(queueConnectionFactory);

        // Create queue session
        jms.createQueueSession();

        // Create a QueueSender
        jms.createQueueSender(queue);

        // Create a message to send to the queue...
        messageID = jms.sendMessageToQueue(text);

        // Close the connection and queue
        jms.closeQueueConnection();

        return messageID;
    }

    // d433583, add an overload method
    /**
     * Put a text message on a specified queue
     *
     * @param text
     *            : message
     * @param queueConnectionFactory
     *            : JNDI context for queue connection factory
     * @param queue
     *            : JNDI context for the queue
     * @return Message ID
     * @throws Exception
     */
    public static String putQueueMessage(Object text, QueueConnectionFactory queueConnectionFactory, Queue queue) throws Exception {

        svLogger.info("Requests to put message '" + text + "' with Queue Connection Factory '" + queueConnectionFactory + "' and Queue '" + queue + "'.");

        String messageID = null;
        FATJMSHelper jms = new FATJMSHelper();

        // Create a connection
        jms.createQueueConnection(queueConnectionFactory);

        // Create queue session
        jms.createQueueSession();

        // Create a QueueSender
        jms.createQueueSender(queue);

        // Create a message to send to the queue...
        messageID = jms.sendMessageToQueue(text);

        // Close the connection and queue
        jms.closeQueueConnection();

        return messageID;
    }

    /**
     * Put a text message on a specified queue
     *
     * @param text
     *            : message
     * @param jmsType
     *            : Set the JMS Type
     * @param queueConnectionFactory
     *            : JNDI context for queue connection factory
     * @param queue
     *            : JNDI context for the queue
     * @return Message ID
     * @throws Exception
     */
    public static String putQueueMessage(Object text, String jmsType, String queueConnectionFactory, String queue) throws Exception {
        svLogger.info("Requests to put message '" + text + "' with Queue Connection Factory '" + queueConnectionFactory + "' and Queue '" + queue + "'.");

        String messageID = null;
        FATJMSHelper jms = new FATJMSHelper();

        // Create a connection
        jms.createQueueConnection(queueConnectionFactory);

        // Create queue session
        jms.createQueueSession();

        // Create a QueueSender
        jms.createQueueSender(queue);

        // Create a message to send to the queue...
        messageID = jms.sendMessageToQueue(text, jmsType);

        // Close the connection and queue
        jms.closeQueueConnection();

        return messageID;
    }

    /**
     * Get a message from a specified queue
     *
     * @param queueConnectionFactory
     *            : JNDI context for queue connection factory
     * @param replyQueueName
     *            : JNDI name of the reply queue
     * @return Message
     * @throws Exception
     */
    public static Object getQueueMessage(String queueConnectionFactory, String replyQueueName) throws Exception {
        Object replyObj = null;

        FATJMSHelper jms = new FATJMSHelper();

        svLogger.info("Receiving message from queue ...");

        // Create a connection
        jms.createQueueConnection(queueConnectionFactory);

        // Create queue session
        jms.createQueueSession();

        // Create a QueueReceiver
        jms.createQueueReceiver(replyQueueName);

        // Get a message from the queue...
        replyObj = jms.getMessage(timeOut);

        // Close the connection and queue
        jms.closeQueueConnection();

        svLogger.info("Message is retrieved from the queue.");
        return replyObj;
    }

    /**
     * Clean up messages from a specified queue
     *
     * @param queueConnectionFactory
     *            : JNDI context for queue connection factory
     * @param replyQueueName
     *            : JNDI name of reply queue
     * @return Number of messages removed from the queue
     * @throws Exception
     */
    public static int emptyQueue(String queueConnectionFactory, String replyQueueName) throws Exception {
        FATJMSHelper jms = new FATJMSHelper();

        svLogger.info("Removing message from queue ...");

        // Create a connection
        jms.createQueueConnection(queueConnectionFactory);

        // Create queue session
        jms.createQueueSession();

        // Create a QueueReceiver
        jms.createQueueReceiver(replyQueueName);

        // 519706 BEGIN
        int queueBacklog = jms.getMessageBacklogSize(replyQueueName);
        int counter = 0;
        if (queueBacklog > 0) {
            svLogger.info("Found a backlog of " + queueBacklog + " queue entries.  Clearing...");
            // Get a message from the queue
            while ((jms.getMessage(10000)) != null) // Just timeout for 10 seconds
            {
                counter++;
            }
        } else {
            svLogger.info("Queue is empty, nothing to clear out.");
        }
        // 519706 END

        // Close the connection and queue
        jms.closeQueueConnection();

        svLogger.info("Number of messages removed from the queue = " + counter);
        return counter;
    }

}
