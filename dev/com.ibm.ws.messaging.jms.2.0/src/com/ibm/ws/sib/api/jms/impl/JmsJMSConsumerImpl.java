/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.impl;

import java.io.Serializable;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageFormatException;
import javax.jms.MessageFormatRuntimeException;
import javax.jms.MessageListener;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.websphere.sib.api.jms.JmsJMSConsumer;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class internally uses messageConsumer do get things done.This class maintains
 * has-a relationship with messageConsumer
 */
public class JmsJMSConsumerImpl implements JmsJMSConsumer {

    // ************************** TRACE INITIALISATION ***************************

    private static TraceComponent tc = SibTr.register(JmsJMSConsumerImpl.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);

    private JmsMsgConsumerImpl messageConsumer = null;

    /**
     * 
     */
    public JmsJMSConsumerImpl(MessageConsumer messageConsumer) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "JmsJMSConsumerImpl", messageConsumer);
        this.messageConsumer = (JmsMsgConsumerImpl) messageConsumer;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "JmsJMSConsumerImpl");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSConsumer#close()
     */
    @Override
    public void close() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "close");
        try {
            messageConsumer.close();
        } catch (JMSException jmse) {
            JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(this, tc, "close");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSConsumer#getMessageListener()
     */
    @Override
    public MessageListener getMessageListener() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMessageListener");

        MessageListener messageListener = null;

        try {
            messageListener = messageConsumer.getMessageListener();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getMessageListener", messageListener);
        }

        return messageListener;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSConsumer#getMessageSelector()
     */
    @Override
    public String getMessageSelector() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMessageSelector");

        String messageSelector = null;

        try {
            messageSelector = messageConsumer.getMessageSelector();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getMessageSelector", messageSelector);
        }
        return messageSelector;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSConsumer#receive()
     */
    @Override
    public Message receive() throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receive");

        try {
            return receive(0);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "receive");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSConsumer#receive(long)
     */
    @Override
    public Message receive(long timeout) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receive", new Object[] { timeout });

        Message message = null;

        try {
            message = messageConsumer.receive(timeout);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "receive", new Object[] { message });
        }
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSConsumer#receiveBody(java.lang.Class)
     */
    @Override
    public <T> T receiveBody(Class<T> paramClass) throws MessageFormatRuntimeException, JMSRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receiveBody", new Object[] { paramClass });

        try {
            // pass timeOut as 0
            return receiveBody(paramClass, 0);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "receiveBody");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSConsumer#receiveBody(java.lang.Class, long)
     */
    @Override
    public <T> T receiveBody(Class<T> paramClass, long timeout) throws MessageFormatRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receiveBody", new Object[] { paramClass, timeout });

        T returnValue = null;

        try {

            Message msg = null;

            // check if the paramClass is valid
            if (!(String.class.isAssignableFrom(paramClass) || Serializable.class.isAssignableFrom(paramClass) || Map.class.isAssignableFrom(paramClass)
            || byte[].class.isAssignableFrom(paramClass))) {
                // invalid paramClass, which is not compatible with any message body type
                throw (javax.jms.MessageFormatException) JmsErrorUtils.newThrowable(javax.jms.MessageFormatException.class,
                                                                                    "INVALID_CLASS_TYPE_CWSIA0118",
                                                                                    new Object[] { paramClass },
                                                                                    tc
                                );
            }

            // receive the message
            msg = receive(timeout);

            // get the message body
            if (msg != null) {
                returnValue = msg.getBody(paramClass);
                if (returnValue == null) { // has no body, hence throw MessageFormatRuntimeException as per JMS 2.0 spec
                    throw (javax.jms.MessageFormatException) JmsErrorUtils.newThrowable(javax.jms.MessageFormatException.class,
                                                                                        "NO_MSG_BODY_CWSIA0119",
                                                                                        null,
                                                                                        tc
                                    );
                }
                else
                {
                    if (!paramClass.isAssignableFrom(returnValue.getClass()))
                    {
                        // invalid paramClass, which is not compatible with message body type
                        throw (javax.jms.MessageFormatException) JmsErrorUtils.newThrowable(javax.jms.MessageFormatException.class,
                                                                                            "INVALID_CLASS_TYPE_CWSIA0118",
                                                                                            new Object[] { paramClass },
                                                                                            tc
                                        );
                    }

                }
            }
        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "receiveBody", new Object[] { returnValue });
        }
        return returnValue;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSConsumer#receiveBodyNoWait(java.lang.Class)
     */
    @Override
    public <T> T receiveBodyNoWait(Class<T> paramClass) throws MessageFormatRuntimeException, JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receiveBodyNoWait", new Object[] { paramClass });

        T returnValue = null;

        try {

            Message msg = null;

            // check if the paramClass is valid
            if (!(String.class.isAssignableFrom(paramClass) || Serializable.class.isAssignableFrom(paramClass) || Map.class.isAssignableFrom(paramClass)
            || byte[].class.isAssignableFrom(paramClass))) {
                // invalid paramClass, which is not compatible with any message body type
                throw (javax.jms.MessageFormatException) JmsErrorUtils.newThrowable(javax.jms.MessageFormatException.class,
                                                                                    "INVALID_CLASS_TYPE_CWSIA0118",
                                                                                    new Object[] { paramClass },
                                                                                    tc
                                );
            }

            // receive the message with no wait
            msg = receiveNoWait();

            // get the message body
            if (msg != null) {
                returnValue = msg.getBody(paramClass);
                if (returnValue == null) { // has no body, hence throw MessageFormatRuntimeException as per JMS 2.0 spec
                    throw (javax.jms.MessageFormatException) JmsErrorUtils.newThrowable(javax.jms.MessageFormatException.class,
                                                                                        "NO_MSG_BODY_CWSIA0119",
                                                                                        null,
                                                                                        tc
                                    );
                }
                else
                {
                    if (!paramClass.isAssignableFrom(returnValue.getClass()))
                    {
                        // invalid paramClass, which is not compatible with message body type
                        throw (javax.jms.MessageFormatException) JmsErrorUtils.newThrowable(javax.jms.MessageFormatException.class,
                                                                                            "INVALID_CLASS_TYPE_CWSIA0118",
                                                                                            new Object[] { paramClass },
                                                                                            tc
                                        );
                    }

                }
            }
        } catch (MessageFormatException mfe) {
            throw (MessageFormatRuntimeException) JmsErrorUtils.getJMS2Exception(mfe, MessageFormatRuntimeException.class);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "receiveBodyNoWait", new Object[] { returnValue });
        }
        return returnValue;

    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSConsumer#receiveNoWait()
     */
    @Override
    public Message receiveNoWait() throws JMSRuntimeException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "receiveNoWait");

        Message message = null;

        try {
            message = messageConsumer.receiveNoWait();
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "receiveNoWait", new Object[] { message });
        }
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jms.JMSConsumer#setMessageListener(javax.jms.MessageListener)
     */
    @Override
    public void setMessageListener(MessageListener listener) throws JMSRuntimeException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMessageListener", new Object[] { listener });

        try {
            messageConsumer.setMessageListener(listener);
        } catch (JMSException jmse) {
            throw (JMSRuntimeException) JmsErrorUtils.getJMS2Exception(jmse, JMSRuntimeException.class);
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setMessageListener");
        }

    }

}
