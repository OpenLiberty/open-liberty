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

import java.lang.reflect.Method;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.service.invoker.MethodDispatcher;
import org.apache.cxf.service.model.BindingOperationInfo;


/**
 * Holder for utility methods relating to messages.
 */
public final class MessageUtils {

    private static final Logger LOG = LogUtils.getL7dLogger(MessageUtils.class);

    /**
     * Prevents instantiation.
     */
    private MessageUtils() {
    }

    /**
     * Determine if message is outbound.
     *
     * @param message the current Message
     * @return true if the message direction is outbound
     */
    public static boolean isOutbound(Message message) {
        if (message == null) {
            return false;
        }

        Exchange exchange = message.getExchange();
        return exchange != null
               && (message == exchange.getOutMessage() || message == exchange.getOutFaultMessage());
    }

    /**
     * Determine if message is fault.
     *
     * @param message the current Message
     * @return true if the message is a fault
     */
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
    public static FaultMode getFaultMode(Message message) {
        if (message != null
            && message.getExchange() != null
            && message == message.getExchange().getOutFaultMessage()) {
            FaultMode mode = message.get(FaultMode.class);
            if (null != mode) {
                return mode;
            }
            return FaultMode.RUNTIME_FAULT;
        }
        return null;
    }

    /**
     * Determine if current messaging role is that of requestor.
     *
     * @param message the current Message
     * @return true if the current messaging role is that of requestor
     */
    public static boolean isRequestor(Message message) {
        if (message != null) {
            //Liberty code change start
            Boolean requestor = (Boolean)((MessageImpl) message).getRequestorRole();
            //Liberty code change end
            return requestor != null && requestor;
        }
        return false;
    }

    /**
     * Determine if the current message is a partial response.
     *
     * @param message the current message
     * @return true if the current messags is a partial response
     */
    public static boolean isPartialResponse(Message message) {
        //Liberty code change start
        return message != null && Boolean.TRUE.equals(((MessageImpl) message).getPartialResponse());
        //Liberty code change end
    }

    /**
     * Determines if the current message is an empty partial response, which
     * is a partial response with an empty content.
     *
     * @param message the current message
     * @return true if the current messags is a partial empty response
     */
    public static boolean isEmptyPartialResponse(Message message) {
        //Liberty code change start
        return message != null && Boolean.TRUE.equals(((MessageImpl) message).getEmptyPartialResponse());
        //Liberty code change end
    }

    /**
     * Returns true if a value is either the String "true" (regardless of case)  or Boolean.TRUE.
     * @param value
     * @return true if value is either the String "true" or Boolean.TRUE
     */
    @Deprecated
    public static boolean isTrue(Object value) {
        return PropertyUtils.isTrue(value);
    }

    public static boolean getContextualBoolean(Message m, String key) {
        return getContextualBoolean(m, key, false);
    }
    public static boolean getContextualBoolean(Message m, String key, boolean defaultValue) {
        if (m != null) {
            Object o = m.getContextualProperty(key);
            if (o != null) {
                return PropertyUtils.isTrue(o);
            }
        }
        return defaultValue;
    }

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

    public static Object getContextualProperty(Message m, String propPreferred, String propDefault) {
        Object prop = null;
        if (m != null) {
            prop = m.getContextualProperty(propPreferred);
            if (prop == null && propDefault != null) {
                prop = m.getContextualProperty(propDefault);
            }
        }
        return prop;
    }

    /**
     * Returns true if the underlying content format is a W3C DOM or a SAAJ message.
     */
    public static boolean isDOMPresent(Message m) {
        return m != null && m.getContent(Node.class) != null;
    }

    public static Method getTargetMethod(Message m, Supplier<RuntimeException> exceptionSupplier) {
        BindingOperationInfo bop = m.getExchange().getBindingOperationInfo();
        if (bop != null) {
            MethodDispatcher md = (MethodDispatcher) m.getExchange().getService().get(MethodDispatcher.class.getName());
            return md.getMethod(bop);
        }
        Method method = (Method) ((MessageImpl) m).getResourceMethod();
        if (method != null || exceptionSupplier == null) {
            return method;
        }
        throw exceptionSupplier.get();
    }
}
