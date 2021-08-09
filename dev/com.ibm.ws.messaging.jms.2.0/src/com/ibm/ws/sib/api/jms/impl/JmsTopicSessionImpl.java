/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
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
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.TemporaryQueue;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsMsgConsumer;
import com.ibm.websphere.sib.api.jms.JmsMsgProducer;
import com.ibm.ws.sib.api.jmsra.JmsJcaSession;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SICoreConnection;

public class JmsTopicSessionImpl extends JmsSessionImpl implements TopicSession
{

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsTopicSessionImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    // ***************************** CONSTRUCTORS ********************************

    protected JmsTopicSessionImpl(boolean trans, int ack, SICoreConnection coreConnection, JmsTopicConnectionImpl jmsTopicConnection, JmsJcaSession jcaSession) throws JMSException {
        super(trans, ack, coreConnection, jmsTopicConnection, jcaSession);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsTopicSessionImpl");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsTopicSessionImpl");
    }

    // *************************** INTERFACE METHODS *****************************

    /**
     * @see javax.jms.TopicSession#createPublisher(Topic)
     */
    @Override
    public TopicPublisher createPublisher(Topic topic) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createPublisher", topic);
        TopicPublisher jmsTopicPublisher = null;

        // Checks for foreign implementation will be handled in the parent class.

        // createProducer() will call this.instantiateProducer()
        try {
            jmsTopicPublisher = (TopicPublisher) createProducer(topic);
        } catch (JMSException e) {
            // No FFDC code needed
            // d238447 FFDC review. Don't call process throwable in this case
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "exception caught: ", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createPublisher", jmsTopicPublisher);
        return jmsTopicPublisher;
    }

    /**
     * @see javax.jms.Session#createQueue(String)
     */
    @Override
    public Queue createQueue(String queueName) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createQueue", queueName);
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0483",
                                                                           new Object[] { "createQueue", "TopicSession" },
                                                                           tc);
    }

    /**
     * @see javax.jms.Session#createBrowser(Queue)
     */
    @Override
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createBrowser", queue);
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0483",
                                                                           new Object[] { "createBrowser", "TopicSession" },
                                                                           tc);
    }

    /**
     * @see javax.jms.Session#createBrowser(Queue, String)
     */
    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createBrowser", new Object[] { queue, messageSelector });
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0483",
                                                                           new Object[] { "createBrowser", "TopicSession" },
                                                                           tc);
    }

    /**
     * @see javax.jms.TopicSession#createSubscriber(Topic)
     */
    @Override
    public TopicSubscriber createSubscriber(Topic topic) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSubscriber", topic);
        TopicSubscriber topicSubscriber = null;

        try {
            topicSubscriber = createSubscriber(topic, null, false);
        } catch (JMSException e) {
            // No FFDC code needed
            // d238447 FFDC review. Don't call process throwable in this case
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "exception caught: ", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createSubscriber", topicSubscriber);
        return topicSubscriber;
    }

    /**
     * @see javax.jms.TopicSession#createSubscriber(Topic, String, boolean)
     */
    @Override
    public TopicSubscriber createSubscriber(Topic topic, String messageSelector, boolean noLocal) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createSubscriber", new Object[] { topic, messageSelector, noLocal });
        TopicSubscriber jmsTopicSubscriber = null;

        // Checks for null and foreign implementation are handled in the parent class.

        // createConsumer() will call this.instantiateConsumer()
        try {
            jmsTopicSubscriber = (TopicSubscriber) createConsumer(topic, messageSelector, noLocal);
        } catch (JMSException e) {
            // No FFDC code needed
            // d238447 FFDC review. Don't call process throwable in this case
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "exception caught: ", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createSubscriber", jmsTopicSubscriber);
        return jmsTopicSubscriber;
    }

    /**
     * @see javax.jms.Session#createTemporaryQueue()
     */
    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createTemporaryQueue");
        throw (javax.jms.IllegalStateException) JmsErrorUtils.newThrowable(javax.jms.IllegalStateException.class,
                                                                           "INVALID_OP_FOR_CLASS_CWSIA0483",
                                                                           new Object[] { "createTemporaryQueue", "TopicSession" },
                                                                           tc);
    }

    // ************************* IMPLEMENTATION METHODS **************************

    /**
     * @see com.ibm.ws.sib.api.jms.impl.JmsSessionImpl#instantiateConsumer(ConsumerProperties)
     */
    @Override
    JmsMsgConsumer instantiateConsumer(ConsumerProperties _props) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateConsumer", _props);
        JmsTopicSubscriberImpl jmsTopicSubscriber = null;

        try {
            jmsTopicSubscriber = new JmsTopicSubscriberImpl(getCoreConnection(), this, _props);
        } catch (JMSException e) {
            // No FFDC code needed
            // d238447 FFDC review. Don't call process throwable in this case
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "exception caught: ", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateConsumer", jmsTopicSubscriber);
        return jmsTopicSubscriber;
    }

    /**
     * @see com.ibm.ws.sib.api.jms.impl.JmsSessionImpl#instantiateProducer(Destination)
     */
    @Override
    JmsMsgProducer instantiateProducer(Destination jsDest) throws JMSException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "instantiateProducer", jsDest);
        JmsTopicPublisherImpl jmsTopicPublisher = null;

        try {
            jmsTopicPublisher = new JmsTopicPublisherImpl(jsDest, getCoreConnection(), this);
        } catch (JMSException e) {
            // No FFDC code needed
            // d238447 FFDC review. Don't call process throwable in this case, FFDCs generated by lower levels
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "exception caught: ", e);
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "instantiateProducer", jmsTopicPublisher);
        return jmsTopicPublisher;
    }
}
