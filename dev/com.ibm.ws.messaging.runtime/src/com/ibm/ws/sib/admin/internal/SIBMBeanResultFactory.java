/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin.internal;

import java.util.Locale;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.admin.mxbean.MessagingSubscription;
import com.ibm.ws.sib.admin.mxbean.QueuedMessage;
import com.ibm.ws.sib.admin.mxbean.QueuedMessageDetail;
import com.ibm.ws.sib.mfp.JsApiMessage;
import com.ibm.ws.sib.mfp.JsJmsMessage;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable;
import com.ibm.ws.sib.processor.runtime.SIMPQueuedMessageControllable;

/**
 * This class provides factory methods for creating instances of the Javabeans
 * used to contain MBean responses. These factory methods should be used rather
 * than instantiating the Javabean classes directly.
 */
public class SIBMBeanResultFactory {

    /**
     * Create a SIBQueuedMessageImpl instance from the supplied
     * SIMPQueuedMessageControllable.
     * 
     * @param qmc
     * @return
     */
    public static QueuedMessage createSIBQueuedMessage(SIMPQueuedMessageControllable qmc) {

        String id = null;
        int jsApproximateLength = 0;
        String name = null;
        String state = null;
        String transactionId = null;
        String type = null;
        String busSystemMessageId = null;

        id = qmc.getId();
        name = qmc.getName();
        state = null;
        transactionId = null;

        try {
            if (qmc.getState() != null) {
                state = qmc.getState().toString();
            }
            transactionId = qmc.getTransactionId();
        } catch (SIMPException e) {
            // No FFDC code needed
        }

        try {
            JsMessage jsMessage = qmc.getJsMessage();
            jsApproximateLength = jsMessage.getApproximateLength();
            busSystemMessageId = jsMessage.getSystemMessageId();
            type = jsMessage.getJsMessageType().toString();
        } catch (SIMPControllableNotFoundException e) {
            // No FFDC code needed
        } catch (SIMPException e) {
            // No FFDC code needed
        }

        return new QueuedMessage(id, name, jsApproximateLength, state, transactionId, type, busSystemMessageId);
    }

    /**
     * Create a SIBQueuedMessageDetailImpl instance from the supplied
     * SIMPQueuedMessageControllable.
     * 
     * @param qmc
     * @param locale
     * @return
     */
    public static QueuedMessageDetail createSIBQueuedMessageDetail(
                                                                   SIMPQueuedMessageControllable qmc, Locale locale) {

        String type = null;
        boolean fullyValid = true;
        boolean valid = false;

        // Data members of SIBusMessage
        String busDiscriminator = null;
        Integer busPriority = null;
        String busReliability = null;
        Long busTimeToLive = null;
        String busReplyDiscriminator = null;
        Integer busReplyPriority = null;
        String busReplyReliability = null;
        Long busReplyTimeToLive = null;
        String busSystemMessageId = null;
        Long busExceptionTimestamp = null;
        String busExceptionMessage = null;
        // F001333-14609
        String busExceptionProblemSubscription = null;
        String busExceptionProblemDestination = null;

        // Data members of JsMessage
        String jsMessageType = null;
        int jsApproximateLength = -1;
        Long jsTimestamp = null;
        Long jsMessageWaitTime = null;
        Long jsCurrentMEArrivalTimestamp = null;
        Integer jsRedeliveredCount = null;
        String jsSecurityUserid = null;
        String jsProducerType = null;
        byte[] jsApiMessageIdAsBytes = null;
        byte[] jsCorrelationIdAsBytes = null;

        // Data members of JsApiMessage
        String apiMessageId = null;
        String apiCorrelationId = null;
        String apiUserid = null;
        String apiFormat = null;

        // Data members of JsJmsMessage
        String jmsDeliveryMode = null;
        Long jmsExpiration = null;
        String jmsDestination = null;
        String jmsReplyTo = null;
        Boolean jmsRedelivered = null;
        String jmsType = null;
        int jmsXDeliveryCount = -1;
        String jmsXAppId = null;

        // Inherited attributes
        String id = null;
        String name = null;
        String state = null;
        long timestamp = 0; // Never set!!!
        String transactionId = null;

        JsMessage jsMessage = null; // JsMesaage is a subclass of SIBusMessage

        try {
            jsMessage = qmc.getJsMessage();
            valid = true;
        } catch (SIMPControllableNotFoundException e) {
            // No FFDC code needed
            fullyValid = false;
        } catch (SIMPException e) {
            // No FFDC code needed
            fullyValid = false;
        }

        id = qmc.getId();
        if ("-1".equals(id)) {
            fullyValid = false;
        }
        name = qmc.getName();

        try {
            if (qmc.getState() != null) {
                state = qmc.getState().toString();
            }
            transactionId = qmc.getTransactionId();
        } catch (SIMPException e) {
            // No FFDC code needed
            fullyValid = false;
        }

        busDiscriminator = jsMessage.getDiscriminator();
        busPriority = jsMessage.getPriority();
        busReliability = jsMessage.getReliability().toString();
        busTimeToLive = jsMessage.getTimeToLive();
        busReplyDiscriminator = jsMessage.getReplyDiscriminator();
        busReplyPriority = jsMessage.getReplyPriority();

        Reliability replyReliability = jsMessage.getReplyReliability();

        if (replyReliability != null) {
            busReplyReliability = replyReliability.toString();
        }

        busReplyTimeToLive = jsMessage.getReplyTimeToLive();
        busSystemMessageId = jsMessage.getSystemMessageId();

        Integer exceptionReason = jsMessage.getExceptionReason();

        if (exceptionReason != null) {

            busExceptionTimestamp = jsMessage.getExceptionTimestamp();

            if (locale == null) {
                locale = Locale.getDefault();
            }

            busExceptionMessage = TraceNLS.getFormattedMessage(JsAdminConstants.EXCEPTION_MSG_BUNDLE,
                                                               JsAdminConstants.EXCEPTION_MESSAGE_KEY_PREFIX + exceptionReason, locale, jsMessage
                                                                               .getExceptionInserts(), "Exception Message");

            // F001333-14609
            busExceptionProblemSubscription = jsMessage.getExceptionProblemSubscription();

            busExceptionProblemDestination = jsMessage.getExceptionProblemDestination();
        }

        jsMessageType = jsMessage.getJsMessageType().toString();
        jsApproximateLength = jsMessage.getApproximateLength();
        jsTimestamp = jsMessage.getTimestamp();
        jsMessageWaitTime = jsMessage.getMessageWaitTime();
        jsCurrentMEArrivalTimestamp = jsMessage.getCurrentMEArrivalTimestamp();
        jsRedeliveredCount = jsMessage.getRedeliveredCount();
        jsSecurityUserid = jsMessage.getSecurityUserid();
        jsProducerType = jsMessage.getProducerType().toString();
        jsApiMessageIdAsBytes = jsMessage.getApiMessageIdAsBytes();
        jsCorrelationIdAsBytes = jsMessage.getCorrelationIdAsBytes();

        if (jsMessage instanceof JsApiMessage) {
            JsApiMessage jsApiMessage = (JsApiMessage) jsMessage;
            apiMessageId = jsApiMessage.getApiMessageId();
            apiCorrelationId = jsApiMessage.getCorrelationId();
            apiUserid = jsApiMessage.getUserid();
            apiFormat = jsApiMessage.getFormat();
        }

        if (jsMessage instanceof JsJmsMessage) {

            JsJmsMessage jsJmsMessage = (JsJmsMessage) jsMessage;

            jmsDeliveryMode = jsJmsMessage.getJmsDeliveryMode().toString();
            jmsExpiration = jsJmsMessage.getJmsExpiration();
            type = "JMS";

            /*
             * if (jsJmsMessage.getJmsDestination() != null) {
             * try {
             * JmsDestination dest = JmsInternalsFactory.getMessageDestEncodingUtils()
             * .getDestinationFromMsgRepresentation(jsJmsMessage.getJmsDestination());
             * jmsDestination = dest.toString();
             * }
             * catch (Exception e) {
             * // No FFDC code needed
             * fullyValid = false;
             * }
             * }
             */

            // Get JMS reply to destination from full header JMS message.

            /*
             * try {
             * Message fullJMSMsg = JmsInternalsFactory.getSharedUtils().inboundMessagePath(jsJmsMessage,
             * null, null); // Added 'null' parameter for pass thru props (SIB0121)
             * Destination dest = fullJMSMsg.getJMSReplyTo();
             * 
             * if (dest != null) {
             * jmsReplyTo = dest.toString();
             * }
             * }
             * catch (Exception e) {
             * // No FFDC code needed
             * fullyValid = false;
             * }
             */

            jmsRedelivered = jsJmsMessage.getJmsRedelivered();
            jmsType = jsJmsMessage.getJmsType();
            jmsXDeliveryCount = jsJmsMessage.getJmsxDeliveryCount();
            jmsXAppId = jsJmsMessage.getJmsxAppId();
        }

        return new QueuedMessageDetail(id, name, state, timestamp, transactionId, type,
                        busDiscriminator, busPriority, busReliability, busTimeToLive, busReplyDiscriminator,
                        busReplyPriority, busReplyReliability, busReplyTimeToLive, busSystemMessageId,
                        busExceptionTimestamp, busExceptionMessage, busExceptionProblemSubscription,
                        busExceptionProblemDestination, jsMessageType, jsApproximateLength,
                        jsTimestamp, jsMessageWaitTime, jsCurrentMEArrivalTimestamp, jsRedeliveredCount,
                        jsSecurityUserid, jsProducerType, jsApiMessageIdAsBytes, jsCorrelationIdAsBytes,
                        apiMessageId, apiCorrelationId, apiUserid, apiFormat, jmsDeliveryMode, jmsExpiration,
                        jmsDestination, jmsReplyTo, jmsRedelivered, jmsType, jmsXDeliveryCount, jmsXAppId,
                        fullyValid, valid);
    }

    /**
     * Create a MessagingSubscription instance from the supplied
     * SIMPLocalSubscriptionControllable.
     * 
     * @param ls
     * @return
     */
    public static MessagingSubscription createSIBSubscription(SIMPLocalSubscriptionControllable ls) {

        long depth = 0;
        String id = null;
        int maxMsgs = 0;
        String name = null;
        String selector = null;
        String subscriberId = null;
        String[] topics = null;

        depth = ls.getNumberOfQueuedMessages();
        id = ls.getId();
        name = ls.getName();
        selector = ls.getSelector();
        subscriberId = ls.getSubscriberID();
        topics = ls.getTopics();

        return new MessagingSubscription(depth, id, maxMsgs, name, selector, subscriberId, topics);
    }

}