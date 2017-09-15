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

package com.ibm.ws.sib.api.jmsra.impl;

import java.lang.reflect.Method;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
//Sanjay Liberty Changes
//import javax.resource.spi.ResourceAdapterInternalException;
//import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpoint;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jms.JmsInternalsFactory;
import com.ibm.ws.sib.api.jms.JmsSharedUtils;
import com.ibm.ws.sib.api.jmsra.JmsraConstants;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AbstractConsumerSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SITransaction;

/**
 * Implementation of <code>SibRaEndpointInvoker</code> for the delivery of
 * messages to core SPI message-driven beans.
 */
final class JmsJcaEndpointInvokerImpl implements SibRaEndpointInvoker {

    /**
     * An <code>onMessage</code> method.
     */
    private static Method ON_MESSAGE_METHOD;

    /**
     * Utility class for converting to JMS messages.
     */
    private JmsSharedUtils _jmsUtils;

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibTr.register(
            JmsJcaEndpointInvokerImpl.class, JmsraConstants.MSG_GROUP,
            JmsraConstants.MSG_BUNDLE);

    /**
     * Provides access to NLS enabled messages.
     */
    private static final TraceNLS NLS = TraceNLS
            .getTraceNLS(JmsraConstants.MSG_BUNDLE);

    private static final String FFDC_PROBE_1 = "1";

    private static final String FFDC_PROBE_2 = "2";

    private static final String CLASS_NAME = JmsJcaEndpointInvokerImpl.class
            .getName();

    /**
     * Properties passed through from the Activation Spec - config
     */
    private Map passThruProps;
    
    /**
     * Constructor.
     * 
     * @param passThruProps_param Administrative properties passed through from activation spec
     * @throws ResourceAdapterInternalException
     *             if the JMS utility class cannot be obtained
     */
    JmsJcaEndpointInvokerImpl(Map passThruProps_param) throws ResourceAdapterInternalException {

        final String methodName = "JmsJcaEndpointInvokerImpl";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, passThruProps_param);
        }

        try {

            _jmsUtils = JmsInternalsFactory.getSharedUtils();
            
        } catch (final JMSException exception) {

            FFDCFilter.processException(exception, CLASS_NAME
                    + ".JmsJcaEndpointInvokerImpl", FFDC_PROBE_2, this);
            if (TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exception);
            }
            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                    ("UTILITY_CLASS_CWSJR1481"), new Object[] { exception },
                    null), exception);

        }

        // Store the 'pass through properties' that have come from the ActivationSpec
        passThruProps = passThruProps_param;
        
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker#getEndpointMethod()
     */
    public Method getEndpointMethod() throws ResourceAdapterInternalException {

        final String methodName = "getEndpointMethod";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        if (ON_MESSAGE_METHOD == null) {

            try {

                ON_MESSAGE_METHOD = MessageListener.class.getMethod(
                        "onMessage", new Class[] { Message.class });

            } catch (final Exception exception) {

                FFDCFilter.processException(exception, CLASS_NAME + "."
                        + methodName, FFDC_PROBE_2, this);
                if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
                    SibTr.exception(this, TRACE, exception);
                }
                throw new ResourceAdapterInternalException(NLS
                        .getFormattedMessage("ON_MESSAGE_CWSJR1483",
                                new Object[] { exception }, null), exception);

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, ON_MESSAGE_METHOD);
        }
        return ON_MESSAGE_METHOD;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.ra.inbound.SibRaEndpointInvoker#invokeEndpoint(javax.resource.spi.endpoint.MessageEndpoint,
     *      com.ibm.wsspi.sib.core.SIBusMessage,
     *      com.ibm.wsspi.sib.core.ConsumerSession,
     *      com.ibm.wsspi.sib.core.SITransaction)
     */
    public boolean invokeEndpoint(final MessageEndpoint endpoint,
            final SIBusMessage message, final AbstractConsumerSession session,
            final SITransaction transaction, String debugMEName)
            throws ResourceAdapterInternalException {

        final String methodName = "invokeEndpoint";
        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { endpoint,
                    message, session, transaction });
        }

        // Check that endpoint is a MessageListener
        if (!(endpoint instanceof MessageListener)) {

            throw new ResourceAdapterInternalException(NLS.getFormattedMessage(
                    ("UNEXPECTED_ENDPOINT_CWSJR1482"), new Object[] { endpoint,
                            MessageListener.class }, null));

        }

        final MessageListener listener = (MessageListener) endpoint;

        boolean success;
        try {

            // Convert to JMS message - no session to pass in, but do pass in 'pass thru' props
            final Message jmsMessage = _jmsUtils.inboundMessagePath(message,
                    null, passThruProps);
            
            // This is a debug property and only set if the value is not null (it will be null unless a
            // runtime system property is set.
            if ((debugMEName != null) && (!debugMEName.equals ("")))
            {
            	if (message instanceof JsJmsMessage)
            	{
            		((JsJmsMessage) message).setObjectProperty ("MEName", debugMEName);
            	}
            	else 
            	{
                    if (TraceComponent.isAnyTracingEnabled() && TRACE.isDebugEnabled()) 
                    {
                    	SibTr.debug(TRACE, "Can not set MDB location in message as the message is not a JsJmsMessage - its a " + jmsMessage.getClass().getName());
                    }
            	}
            }
            
            // Deliver message
            listener.onMessage(jmsMessage);
            success = true;

        } catch (final Throwable exc) {

            // Failed to deliver message
            FFDCFilter.processException(exc, CLASS_NAME + "." + methodName,
                    FFDC_PROBE_1, this);
            if (TRACE.isEventEnabled()) {
                SibTr.exception(this, TRACE, exc);
            }
            success = false;

        }

        if (TraceComponent.isAnyTracingEnabled() && TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName, Boolean.valueOf(success));
        }
        return success;

    }

}
