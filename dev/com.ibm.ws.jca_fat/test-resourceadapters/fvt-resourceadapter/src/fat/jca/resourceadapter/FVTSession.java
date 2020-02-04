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
package fat.jca.resourceadapter;

import java.io.Serializable;
import java.sql.SQLException;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import javax.resource.spi.ConnectionEvent;

public class FVTSession implements Session {
    FVTConnection con;
    boolean transactionInProgress;

    FVTSession(FVTConnection con) {
        this.con = con;
    }

    @Override
    public void close() throws JMSException {
        rollback();
        con = null;
    }

    @Override
    public void commit() throws JMSException {
        if (transactionInProgress)
            try {
                con.mc.con.commit();
                con.mc.con.setAutoCommit(true);
                con.mc.notify(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, con, null);
            } catch (SQLException x) {
                throw (JMSException) new JMSException(x.getMessage()).initCause(x);
            }
    }

    @Override
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BytesMessage createBytesMessage() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        return new FVTMessageConsumer(this, destination);
    }

    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MapMessage createMapMessage() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message createMessage() throws JMSException {
        return createTextMessage();
    }

    @Override
    public ObjectMessage createObjectMessage() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MessageProducer createProducer(Destination destination) throws JMSException {
        return new FVTMessageProducer(this, destination);
    }

    @Override
    public Queue createQueue(String queueName) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public StreamMessage createStreamMessage() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TextMessage createTextMessage() throws JMSException {
        return createTextMessage(null);
    }

    @Override
    public TextMessage createTextMessage(String text) throws JMSException {
        TextMessage message = new FVTTextMessage();
        message.setText(text);
        try {
            // include the user name so tests can verify the right user is used
            message.setStringProperty("userName", con.mc.con.getMetaData().getUserName());
        } catch (SQLException x) {
            throw (JMSException) new JMSException(x.getMessage()).initCause(x);
        }
        return message;
    }

    @Override
    public Topic createTopic(String topicName) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getAcknowledgeMode() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MessageListener getMessageListener() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getTransacted() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void recover() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rollback() throws JMSException {
        if (transactionInProgress)
            try {
                con.mc.con.rollback();
                con.mc.con.setAutoCommit(true);
                con.mc.notify(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, con, null);
            } catch (SQLException x) {
                throw (JMSException) new JMSException(x.getMessage()).initCause(x);
            }
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unsubscribe(String name) throws JMSException {
        throw new UnsupportedOperationException();
    }
}
