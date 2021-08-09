/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.message;

import java.util.logging.Logger;

import org.w3c.dom.Node;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

import org.apache.cxf.common.logging.LogUtils;

/**
 * Holder for utility methods relating to messages.
 */
public final class MessageUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(MessageUtils.class);

    /**
     * Prevents instantiation.
     */
    @Trivial
    private MessageUtils() {
    }

    @Trivial
    public static int getContextualInteger(Message m, String key, int defaultValue) {
        if (m != null) {
            Object o = m.getContextualProperty(key);
            if (o instanceof String) {
                try {
                    int i = Integer.parseInt((String)o);
                    if (i > 0) {
                        return i;
                    }
                } catch (NumberFormatException ex) {
                    LOG.warning("Incorrect integer value of " + o + " specified for: " + key);
                }
            }
        }
        return defaultValue;
    }
    /**
     * Determine if message is outbound.
     * 
     * @param message the current Message
     * @return true if the message direction is outbound
     */
    public static boolean isOutbound(@Sensitive Message message) {
        LOG.entering("MessageUtils", "isOutbound");
        Exchange exchange = message.getExchange();
        LOG.exiting("MessageUtils", "isOutbound");
        return exchange != null
               && (message == exchange.getOutMessage() || message == exchange.getOutFaultMessage());
    }

    /**
     * Determine if message is fault.
     * 
     * @param message the current Message
     * @return true if the message is a fault
     */
    @Trivial
    public static boolean isFault(Message message) {
        return message != null
               && message.getExchange() != null
               && (message == message.getExchange().getInFaultMessage() || message == message.getExchange()
                   .getOutFaultMessage());
    }
    
    /**
     * Determine the fault mode for the underlying (fault) message 
     * (for use on server side only).
     * 
     * @param message the fault message
     * @return the FaultMode
     */
    @Trivial
    public static FaultMode getFaultMode(Message message) {
        if (message != null
            && message.getExchange() != null
            && message == message.getExchange().getOutFaultMessage()) {
            FaultMode mode = message.get(FaultMode.class);
            if (null != mode) {
                return mode;
            } else {
                return FaultMode.RUNTIME_FAULT;
            }
        }
        return null;    
    }

    /**
     * Determine if current messaging role is that of requestor.
     * 
     * @param message the current Message
     * @return true if the current messaging role is that of requestor
     */
    public static boolean isRequestor(@Sensitive Message message) {
        LOG.entering("MessageUtils", "isRequestor");
        Boolean requestor = (Boolean)message.get(Message.REQUESTOR_ROLE);
        LOG.exiting("MessageUtils", "isRequestor");
        return requestor != null && requestor.booleanValue();
    }
    
    /**
     * Determine if the current message is a partial response.
     * 
     * @param message the current message
     * @return true if the current messags is a partial response
     */
    public static boolean isPartialResponse(@Sensitive Message message) {
        LOG.entering("MessageUtils", "isPartialResponse");
        LOG.exiting("MessageUtils", "isPartialResponse");
        return Boolean.TRUE.equals(message.get(Message.PARTIAL_RESPONSE_MESSAGE));
    }
    
    /**
     * Determines if the current message is an empty partial response, which
     * is a partial response with an empty content.
     * 
     * @param message the current message
     * @return true if the current messags is a partial empty response
     */
    @Trivial
    public static boolean isEmptyPartialResponse(Message message) {
        return Boolean.TRUE.equals(message.get(Message.EMPTY_PARTIAL_RESPONSE_MESSAGE));
    }

    /**
     * Returns true if a value is either the String "true" (regardless of case)  or Boolean.TRUE.
     * @param value
     * @return true if value is either the String "true" or Boolean.TRUE
     */
    @Trivial
    public static boolean isTrue(Object value) {
        if (value == null) {
            return false;
        }

        if (Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(value.toString())) {
            return true;
        }
        
        return false;
    }
    

    public static boolean getContextualBoolean(@Sensitive Message m, String key, boolean defaultValue) {
        LOG.entering("MessageUtils", "getContextualBoolean");
        Object o = m.getContextualProperty(key);
        if (o != null) {
            return isTrue(o);
        }
        LOG.exiting("MessageUtils", "getContextualBoolean");
        return defaultValue;
    }
    
    /**
     * Returns true if the underlying content format is a W3C DOM or a SAAJ message.
     */
    @Trivial
    public static boolean isDOMPresent(Message m) {
        return m.getContent(Node.class) != null;
        /*
        for (Class c : m.getContentFormats()) {
            if (c.equals(Node.class) || c.getName().equals("javax.xml.soap.SOAPMessage")) {
                return true;
            }   
        }
        return false;
        */
    }

}
