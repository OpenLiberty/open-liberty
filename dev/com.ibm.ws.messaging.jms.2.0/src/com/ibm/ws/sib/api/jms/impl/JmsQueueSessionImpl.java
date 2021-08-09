/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.impl;

import javax.jms.Destination;
import javax.jms.IllegalStateException;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TemporaryTopic;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsMsgConsumer;
import com.ibm.websphere.sib.api.jms.JmsMsgProducer;
import com.ibm.ws.sib.api.jmsra.JmsJcaSession;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

public class JmsQueueSessionImpl extends JmsSessionImpl implements QueueSession {

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsQueueSessionImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // ***************************** CONSTRUCTORS ********************************

    protected JmsQueueSessionImpl(boolean trans, int ack, SICoreConnection coreConnection, JmsQueueConnectionImpl jmsQueueConnection, JmsJcaSession jcaSession) throws JMSException {
        super(trans, ack, coreConnection, jmsQueueConnection, jcaSession);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsQueueSessionImpl");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsQueueSessionImpl");
    }

    // *************************** INTERFACE METHODS *****************************

    /**
     * @see javax.jms.Session#createDurableSubscriber(Topic, String, String, boolean)
     */
    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableSubscriber", new Object[] { topic, name, messageSelector, noLocal });
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0481",
                                                                           new Object[] { "createDurableSubscriber", "QueueSession" },
                                                                           tc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createDurableConsumer(javax.jms.Topic, java.lang.String)
     */
    @Override
    public MessageConsumer createDurableConsumer(Topic topic,
                                                 String name)
                    throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableConsumer", new Object[] { topic, name });
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0481",
                                                                           new Object[] { "createDurableConsumer", "QueueSession" },
                                                                           tc);

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createDurableConsumer(javax.jms.Topic, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public MessageConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createDurableConsumer", new Object[] { topic, name, messageSelector, noLocal });
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0481",
                                                                           new Object[] { "createDurableConsumer", "QueueSession" },
                                                                           tc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createSharedConsumer(javax.jms.Topic, java.lang.String)
     */
    @Override
    public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedConsumer", new Object[] { topic, sharedSubscriptionName });
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0481",
                                                                           new Object[] { "createSharedConsumer", "QueueSession" },
                                                                           tc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createSharedConsumer(javax.jms.Topic, java.lang.String, java.lang.String)
     */
    @Override
    public MessageConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedConsumer", new Object[] { topic, sharedSubscriptionName, messageSelector });
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0481",
                                                                           new Object[] { "createSharedConsumer", "QueueSession" },
                                                                           tc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createSharedDurableConsumer(javax.jms.Topic, java.lang.String)
     */
    @Override
    public MessageConsumer createSharedDurableConsumer(Topic topic, String name) throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedDurableConsumer", new Object[] { topic, name });
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0481",
                                                                           new Object[] { "createSharedDurableConsumer", "QueueSession" },
                                                                           tc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.Session#createSharedDurableConsumer(javax.jms.Topic, java.lang.String, java.lang.String)
     */
    @Override
    public MessageConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSharedDurableConsumer", new Object[] { topic, name, messageSelector });
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0481",
                                                                           new Object[] { "createSharedDurableConsumer", "QueueSession" },
                                                                           tc);
    }

    /**
     * @see javax.jms.QueueSession#createReceiver(Queue)
     */
    @Override
    public QueueReceiver createReceiver(Queue queue) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createReceiver", queue);
        QueueReceiver queueReceiver = null;

        try {
            queueReceiver = createReceiver(queue, null);
        } catch (JMSException e) {
            // No FFDC code needed
            // d238447 FFDC review - don't call processThrowable here
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "exception caught: ", e);
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createReceiver", queueReceiver);
        return queueReceiver;
    }

    /**
     * @see javax.jms.QueueSession#createReceiver(Queue, String)
     */
    @Override
    public QueueReceiver createReceiver(Queue queue, String messageSelector) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createReceiver", new Object[] { queue, messageSelector });
        QueueReceiver jmsQueueReceiver = null;

        // Checks for null and foreign implementation are done in the parent class.

        // createConsumer() will call this.instantiateConsumer()
        try {
            jmsQueueReceiver = (QueueReceiver) createConsumer(queue, messageSelector);
        } catch (JMSException e) {
            // No FFDC code needed
            // d238447 FFDC review. Don't call processThrowable
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "exception caught: ", e);
            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createReceiver", jmsQueueReceiver);
        return jmsQueueReceiver;
    }

    /**
     * @see javax.jms.QueueSession#createSender(Queue)
     */
    @Override
    public QueueSender createSender(Queue queue) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSender", queue);
        QueueSender jmsQueueSender = null;

        // Checks for foreign implementation are handled in the parent class.
        // (null is allowed here as an unidentified producer).

        // createProducer() will call this.instantiateProducer()
        try {
            jmsQueueSender = (QueueSender) createProducer(queue);
        } catch (JMSException e) {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "exception caught: ", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createSender", jmsQueueSender);
        return jmsQueueSender;
    }

    /**
     * @see javax.jms.Session#createTemporaryTopic()
     */
    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTemporaryTopic");
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0481",
                                                                           new Object[] { "createTemporaryTopic", "QueueSession" },
                                                                           tc);
    }

    /**
     * @see javax.jms.Session#createTopic(String)
     */
    @Override
    public Topic createTopic(String topicName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTopic", topicName);
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0481",
                                                                           new Object[] { "createTopic", "QueueSession" },
                                                                           tc);
    }

    /**
     * @see javax.jms.Session#unsubscribe(String)
     */
    @Override
    public void unsubscribe(String name) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unsubscribe", name);
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0481",
                                                                           new Object[] { "unsubscribe", "QueueSession" },
                                                                           tc);
    }

    // ************************* IMPLEMENTATION METHODS **************************

    /**
     * @see com.ibm.ws.sib.api.jms.impl.JmsSessionImpl#instantiateConsumer(ConsumerProperties)
     */
    @Override
    JmsMsgConsumer instantiateConsumer(ConsumerProperties newProps) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateConsumer", newProps);
        JmsQueueReceiverImpl jmsQueueReceiver = null;

        try {
            jmsQueueReceiver = new JmsQueueReceiverImpl(getCoreConnection(), this, newProps);
        } catch (JMSException e) {
            // No FFDC code needed
            // d238447 FFDC review. FFDCs generated above, don't call processThrowable
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "exception caught: ", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateConsumer", jmsQueueReceiver);
        return jmsQueueReceiver;
    }

    /**
     * @see com.ibm.ws.sib.api.jms.impl.JmsSessionImpl#instantiateProducer(Destination)
     */
    @Override
    JmsMsgProducer instantiateProducer(Destination jsDest) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateProducer", jsDest);
        JmsQueueSenderImpl jmsQueueSender = null;

        try {
            jmsQueueSender = new JmsQueueSenderImpl(jsDest, getCoreConnection(), this);
        } catch (JMSException e) {
            // No FFDC code needed
            // d238447 FFDC review. Don't call processThrowable here
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "exception caught: ", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateProducer", jmsQueueSender);
        return jmsQueueSender;
    }
}
