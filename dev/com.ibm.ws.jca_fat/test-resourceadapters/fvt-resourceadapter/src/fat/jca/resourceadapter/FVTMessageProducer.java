/*******************************************************************************
 * Copyright (c) 2012,2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.resourceadapter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TransactionInProgressException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.Work;

public class FVTMessageProducer implements MessageProducer {
    // some fake delivery modes that we can use to force the JCA WorkManager or Timer to be used
    private static final int DELIVERY_MODE_ASYNC = 0xA; // Use JCA WorkManager
    private static final int DELIVERY_MODE_DELAYED = 0xD; // Use JCA Timer
    private static final long TIMEOUT = 6000; // maximum amount of time we will wait for an async operation to complete

    private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
    private Destination destination;
    private final int priority = Message.DEFAULT_PRIORITY;
    private FVTSession session;
    private final long timeToLive = Message.DEFAULT_TIME_TO_LIVE;

    FVTMessageProducer(FVTSession session, Destination destination) throws JMSException {
        this.session = session;
        this.destination = destination;

        String tableName;
        if (destination instanceof Queue)
            tableName = ((Queue) destination).getQueueName();
        else if (destination instanceof Topic)
            tableName = ((Topic) destination).getTopicName();
        else
            throw new InvalidDestinationException("Destination: " + destination);

        if (session.transactionInProgress)
            throw new TransactionInProgressException("Not allowed during transaction");

        try {
            Statement stmt = session.con.mc.con.createStatement();
            try {
                stmt.executeUpdate("create table " + tableName + " (messageID integer primary key generated always as identity, messageText varchar(200))");
            } catch (SQLException x) {
            } finally {
                stmt.close();
            }
        } catch (SQLException x) {
            throw (JMSException) new JMSException(x.getMessage()).initCause(x);
        }
    }

    @Override
    public void close() throws JMSException {
        session = null;
        destination = null;
    }

    @Override
    public int getDeliveryMode() throws JMSException {
        return deliveryMode;
    }

    @Override
    public Destination getDestination() throws JMSException {
        return destination;
    }

    @Override
    public boolean getDisableMessageID() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getDisableMessageTimestamp() throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getPriority() throws JMSException {
        return priority;
    }

    @Override
    public long getTimeToLive() throws JMSException {
        return timeToLive;
    }

    private void notifyEndpoints(Destination destination, Message message, int deliveryMode) throws Exception {
        int numEndpoints = 0;
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<Object>();
        try {
            BootstrapContext bootstrapContext = session.con.mc.mcf.adapter.getBootstrapContext();
            Timer timer = deliveryMode == DELIVERY_MODE_DELAYED ? bootstrapContext.createTimer() : null;
            for (Map.Entry<ActivationSpec, MessageEndpointFactory> entry : session.con.mc.mcf.adapter.endpointFactories.entrySet()) {
                String asDestType = ((FVTActivationSpec) entry.getKey()).getDestinationType();
                String asDestName = ((FVTActivationSpec) entry.getKey()).getDestination();
                boolean match = destination instanceof Queue
                                ? Queue.class.getName().equals(asDestType) && ((Queue) destination).getQueueName().equals(asDestName)
                                : Topic.class.getName().equals(asDestType) && ((Topic) destination).getTopicName().equals(asDestName);
                if (match) {
                    FVTOnMessageWorker task = new FVTOnMessageWorker(message, entry.getValue(), results);

                    // Note: deliveryMode is being abused here as a way to let the tests decide whether we should use
                    // the JCA WorkManger, the JCA Timer, or neither to invoke onMessage
                    if (deliveryMode == DELIVERY_MODE_ASYNC)
                        bootstrapContext.getWorkManager().scheduleWork(task);
                    else if (deliveryMode == DELIVERY_MODE_DELAYED)
                        timer.schedule(task, 50);
                    else
                        task.run();

                    numEndpoints++;
                }
            }
        } finally {
            // Wait for onMessage to be invoked on all listeners, and watch for any errors
            for (int i = 0; i < numEndpoints; i++) {
                Object result = results.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                System.out.println("onMessage: " + result);
                if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                else if (result == null)
                    throw new JMSException("onMessage was not invoked in a timely manner (" + TIMEOUT + ") milliseconds");
                else if (!(result instanceof MessageListener))
                    throw new JMSException("Unexpected result: " + result);
            }
        }
    }

    @Override
    public void send(Destination destination, Message message) throws JMSException {
        send(destination, message, deliveryMode, priority, timeToLive);
    }

    @Override
    public void send(Destination destination, Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        if (!(message instanceof TextMessage))
            throw new MessageFormatException("Message: " + message);

        String tableName;
        if (destination instanceof Queue)
            tableName = ((Queue) destination).getQueueName();
        else if (destination instanceof Topic)
            tableName = ((Topic) destination).getTopicName();
        else
            throw new InvalidDestinationException("Destination: " + destination);

        PreparedStatement pstmt = null;
        try {
            if (!session.transactionInProgress) {
                session.con.mc.con.setAutoCommit(false);
                session.con.mc.notify(ConnectionEvent.LOCAL_TRANSACTION_STARTED, session.con, null);
                session.transactionInProgress = true;
            }

            pstmt = session.con.mc.con.prepareStatement("insert into " + tableName + " (messageText) values (?)");
            pstmt.setString(1, ((TextMessage) message).getText());
            pstmt.executeUpdate();

            notifyEndpoints(destination, message, deliveryMode);

        } catch (JMSException x) {
            throw x;
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw (JMSException) new JMSException(x.getMessage()).initCause(x);
        } finally {
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException x) {
                }
        }
    }

    @Override
    public void send(Message message) throws JMSException {
        send(destination, message, deliveryMode, priority, timeToLive);
    }

    @Override
    public void send(Message message, int deliveryMode, int priority, long timeToLive) throws JMSException {
        send(destination, message, deliveryMode, priority, timeToLive);
    }

    @Override
    public void setDeliveryMode(int deliveryMode) throws JMSException {
        this.deliveryMode = deliveryMode;
    }

    @Override
    public void setDisableMessageID(boolean value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDisableMessageTimestamp(boolean value) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPriority(int defaultPriority) throws JMSException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTimeToLive(long timeToLive) throws JMSException {
        throw new UnsupportedOperationException();
    }
}

class FVTOnMessageWorker extends TimerTask implements Work {
    private final AtomicBoolean canceled = new AtomicBoolean();
    private final AtomicReference<Thread> executionThread = new AtomicReference<Thread>();
    private final Message message;
    private final MessageEndpointFactory endpointFactory;
    private final LinkedBlockingQueue<Object> results;

    FVTOnMessageWorker(Message message, MessageEndpointFactory endpointFactory, LinkedBlockingQueue<Object> results) {
        this.message = message;
        this.endpointFactory = endpointFactory;
        this.results = results;
    }

    @Override
    public void release() {
        canceled.set(true);
        Thread thread = executionThread.get();
        if (thread != null)
            thread.interrupt();
    }

    @Override
    public void run() {
        try {
            if (canceled.get())
                throw new IllegalStateException("Canceled");

            Thread currentThread = Thread.currentThread();
            if (!executionThread.compareAndSet(null, currentThread))
                throw new IllegalStateException("Submitting the same instance of this Work to multiple threads is not supported");

            try {
                MessageEndpoint endpoint = endpointFactory.createEndpoint(null);
                endpoint.beforeDelivery(MessageListener.class.getMethod("onMessage", Message.class));
                ((MessageListener) endpoint).onMessage(message);
                endpoint.afterDelivery();
                results.add(endpoint);
            } finally {
                executionThread.compareAndSet(currentThread, null);
            }
        } catch (Throwable x) {
            results.add(x);
        } finally {
            cancel(); // If used a TimerTask, prevent it from being rescheduled
        }
    }
}