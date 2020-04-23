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

package org.apache.cxf.ws.addressing;


import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;

import static org.apache.cxf.ws.addressing.JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES;

/**
 * Holder for utility methods relating to contexts.
 */
public final class ContextUtils {

    public static final ObjectFactory WSA_OBJECT_FACTORY = new ObjectFactory();
    public static final String ACTION = ContextUtils.class.getName() + ".ACTION";

    private static final EndpointReferenceType NONE_ENDPOINT_REFERENCE = new EndpointReferenceType();

    private static final Logger LOG = LogUtils.getL7dLogger(ContextUtils.class);

    /**
     * Used to fabricate a Uniform Resource Name from a UUID string
     */
    private static final String URN_UUID = "urn:uuid:";

    /**
     * Used by MAPAggregator to cache bad MAP fault name
     */
    private static final String MAP_FAULT_NAME_PROPERTY =
        "org.apache.cxf.ws.addressing.map.fault.name";

    /**
     * Used by MAPAggregator to cache bad MAP fault reason
     */
    private static final String MAP_FAULT_REASON_PROPERTY =
        "org.apache.cxf.ws.addressing.map.fault.reason";

    /**
     * Indicates a partial response has already been sent
     */
    private static final String PARTIAL_RESPONSE_SENT_PROPERTY =
        "org.apache.cxf.ws.addressing.partial.response.sent";

    static {
        NONE_ENDPOINT_REFERENCE.setAddress(new AttributedURIType());
        NONE_ENDPOINT_REFERENCE.getAddress().setValue(Names.WSA_NONE_ADDRESS);
    }

   /**
    * Prevents instantiation.
    */
    private ContextUtils() {
    }

   /**
    * Determine if message is outbound.
    *
    * @param message the current Message
    * @return true iff the message direction is outbound
    */
    public static boolean isOutbound(Message message) {
        return message != null
               && message.getExchange() != null
               && (message == message.getExchange().getOutMessage()
                   || message == message.getExchange().getOutFaultMessage());
    }

   /**
    * Determine if message is fault.
    *
    * @param message the current Message
    * @return true iff the message is a fault
    */
    public static boolean isFault(Message message) {
        return message != null
               && message.getExchange() != null
               && (message == message.getExchange().getInFaultMessage()
                   || message == message.getExchange().getOutFaultMessage());
    }

   /**
    * Determine if current messaging role is that of requestor.
    *
    * @param message the current Message
    * @return true if the current messaging role is that of requestor
    */
    public static boolean isRequestor(Message message) {
        //Liberty code change start
        Boolean requestor = (Boolean)((MessageImpl) message).getRequestorRole();
        //Liberty code change end
        return requestor != null && requestor.booleanValue();
    }

    /**
     * Get appropriate property name for message addressing properties.
     *
     * @param isRequestor true if the current messaging role is that of
     * requestor
     * @param isProviderContext true if the binding provider request context
     * available to the client application as opposed to the message context
     * visible to handlers
     * @param isOutbound true if the message is outbound
     * @return the property name to use when caching the MAPs in the context
     */
    public static String getMAPProperty(boolean isRequestor,
                                        boolean isProviderContext,
                                        boolean isOutbound) {
        if (isRequestor && isProviderContext) {
            return CLIENT_ADDRESSING_PROPERTIES;
        }
        return isOutbound
            ? ADDRESSING_PROPERTIES_OUTBOUND
            : ADDRESSING_PROPERTIES_INBOUND;
    }

    /**
     * Store MAPs in the message.
     *
     * @param message the current message
     * @param isOutbound true if the message is outbound
     */
    public static void storeMAPs(AddressingProperties maps,
                                 Message message,
                                 boolean isOutbound) {
        storeMAPs(maps, message, isOutbound, isRequestor(message), false);
    }

    /**
     * Store MAPs in the message.
     *
     * @param maps the MAPs to store
     * @param message the current message
     * @param isOutbound true if the message is outbound
     * @param isRequestor true if the current messaging role is that of requestor
     */
    public static void storeMAPs(AddressingProperties maps,
                                 Message message,
                                 boolean isOutbound,
                                 boolean isRequestor) {
        storeMAPs(maps, message, isOutbound, isRequestor, false);
    }

    /**
     * Store MAPs in the message.
     *
     * @param maps the MAPs to store
     * @param message the current message
     * @param isOutbound true if the message is outbound
     * @param isRequestor true if the current messaging role is that of requestor
     * @param isProviderContext true if the binding provider request context
     */
    public static void storeMAPs(AddressingProperties maps,
                                 Message message,
                                 boolean isOutbound,
                                 boolean isRequestor,
                                 boolean isProviderContext) {
        if (maps != null) {
            String mapProperty = getMAPProperty(isRequestor, isProviderContext, isOutbound);
            LOG.log(Level.FINE,
                    "associating MAPs with context property {0}",
                    mapProperty);
            message.put(mapProperty, maps);
        }
    }

    /**
     * @param message the current message
     * @param isProviderContext true if the binding provider request context
     * available to the client application as opposed to the message context
     * visible to handlers
     * @param isOutbound true if the message is outbound
     * @return the current addressing properties
     */
    public static AddressingProperties retrieveMAPs(
                                                   Message message,
                                                   boolean isProviderContext,
                                                   boolean isOutbound) {
        return retrieveMAPs(message, isProviderContext, isOutbound, true);
    }

    /**
     * @param message the current message
     * @param isProviderContext true if the binding provider request context
     * available to the client application as opposed to the message context
     * visible to handlers
     * @param isOutbound true if the message is outbound
     * @param warnIfMissing log a warning  message if properties cannot be retrieved
     * @return the current addressing properties
     */
    public static AddressingProperties retrieveMAPs(
                                                   Message message,
                                                   boolean isProviderContext,
                                                   boolean isOutbound,
                                                   boolean warnIfMissing) {
        boolean isRequestor = ContextUtils.isRequestor(message);
        String mapProperty =
            ContextUtils.getMAPProperty(isRequestor,
                                        isProviderContext,
                                        isOutbound);
        LOG.log(Level.FINE,
                "retrieving MAPs from context property {0}",
                mapProperty);

        AddressingProperties maps =
            (AddressingProperties)message.get(mapProperty);
        if (maps == null && isOutbound && !isRequestor
            && message.getExchange() != null && message.getExchange().getInMessage() != null) {
            maps = (AddressingProperties)message.getExchange().getInMessage().get(mapProperty);
        }

        if (maps != null) {
            LOG.log(Level.FINE, "current MAPs {0}", maps);
        } else if (!isProviderContext) {
            LogUtils.log(LOG, warnIfMissing ? Level.WARNING : Level.FINE,
                "MAPS_RETRIEVAL_FAILURE_MSG");
        }
        return maps;
    }

    /**
     * Helper method to get an attributed URI.
     *
     * @param uri the URI
     * @return an AttributedURIType encapsulating the URI
     */
    public static AttributedURIType getAttributedURI(String uri) {
        AttributedURIType attributedURI =
            WSA_OBJECT_FACTORY.createAttributedURIType();
        attributedURI.setValue(uri);
        return attributedURI;
    }

    /**
     * Helper method to get a RealtesTo instance.
     *
     * @param uri the related URI
     * @return a RelatesToType encapsulating the URI
     */
    public static RelatesToType getRelatesTo(String uri) {
        RelatesToType relatesTo =
            WSA_OBJECT_FACTORY.createRelatesToType();
        relatesTo.setValue(uri);
        return relatesTo;
    }
    
    private static boolean startsWith(String value, String ref) {
        if (StringUtils.isEmpty(value)) {
            return false;
        }
        return value.startsWith(ref);
    }

    /**
     * Helper method to determine if an EPR address is generic (either null,
     * none or anonymous).
     *
     * @param ref the EPR under test
     * @return true if the address is generic
     */
    public static boolean isGenericAddress(EndpointReferenceType ref) {
        return ref == null
               || ref.getAddress() == null
               || startsWith(ref.getAddress().getValue(), Names.WSA_ANONYMOUS_ADDRESS)
               || startsWith(ref.getAddress().getValue(), Names.WSA_NONE_ADDRESS);
    }

    /**
     * Helper method to determine if an EPR address is anon (either null,
     * anonymous).
     *
     * @param ref the EPR under test
     * @return true if the address is generic
     */
    public static boolean isAnonymousAddress(EndpointReferenceType ref) {
        return ref == null
               || ref.getAddress() == null
               || startsWith(ref.getAddress().getValue(), Names.WSA_ANONYMOUS_ADDRESS);
    }

    /**
     * Helper method to determine if an EPR address is none.
     *
     * @param ref the EPR under test
     * @return true if the address is generic
     */
    public static boolean isNoneAddress(EndpointReferenceType ref) {
        return ref != null
               && ref.getAddress() != null
               && Names.WSA_NONE_ADDRESS.equals(ref.getAddress().getValue());
    }

    /**
     * Helper method to determine if an MAPs Action is empty (a null action
     * is considered empty, whereas a zero length action suppresses
     * the propagation of the Action property).
     *
     * @param maps the MAPs Action under test
     * @return true if the Action is empty
     */
    public static boolean hasEmptyAction(AddressingProperties maps) {
        boolean empty = maps.getAction() == null;
        if (maps.getAction() != null
            && maps.getAction().getValue().isEmpty()) {
            maps.setAction(null);
            empty = false;
        }
        return empty;
    }


    /**
     * Propagate inbound MAPs onto full reponse & fault messages.
     *
     * @param inMAPs the inbound MAPs
     * @param exchange the current Exchange
     */
    public static void propogateReceivedMAPs(AddressingProperties inMAPs,
                                              Exchange exchange) {
        if (exchange.getOutMessage() == null) {
            exchange.setOutMessage(createMessage(exchange));
        }
        propogateReceivedMAPs(inMAPs, exchange.getOutMessage());
        if (exchange.getOutFaultMessage() == null) {
            exchange.setOutFaultMessage(createMessage(exchange));
        }
        propogateReceivedMAPs(inMAPs, exchange.getOutFaultMessage());
    }

    /**
     * Propogate inbound MAPs onto reponse message if applicable
     * (not applicable for oneways).
     *
     * @param inMAPs the inbound MAPs
     * @param responseMessage
     */
    public static void propogateReceivedMAPs(AddressingProperties inMAPs,
                                             Message responseMessage) {
        if (responseMessage != null) {
            storeMAPs(inMAPs, responseMessage, false, false, false);
        }
    }


    /**
     * Store bad MAP fault name in the message.
     *
     * @param faultName the fault name to store
     * @param message the current message
     */
    public static void storeMAPFaultName(String faultName,
                                         Message message) {
        message.put(MAP_FAULT_NAME_PROPERTY, faultName);
    }

    /**
     * Retrieve MAP fault name from the message.
     *
     * @param message the current message
     * @return the retrieved fault name
     */
    public static String retrieveMAPFaultName(Message message) {
        return (String)message.get(MAP_FAULT_NAME_PROPERTY);
    }

    /**
     * Store MAP fault reason in the message.
     *
     * @param reason the fault reason to store
     * @param message the current message
     */
    public static void storeMAPFaultReason(String reason,
                                           Message message) {
        message.put(MAP_FAULT_REASON_PROPERTY, reason);
    }

    /**
     * Retrieve MAP fault reason from the message.
     *
     * @param message the current message
     * @return the retrieved fault reason
     */
    public static String retrieveMAPFaultReason(Message message) {
        return (String)message.get(MAP_FAULT_REASON_PROPERTY);
    }

    /**
     * Store an indication that a partial response has been sent.
     * Relavant if *both* the replyTo & faultTo are decoupled,
     * and a fault occurs, then we would already have sent the
     * partial response (pre-dispatch) for the replyTo, so
     * no need to send again.
     *
     * @param message the current message
     */
    public static void storePartialResponseSent(Message message) {
        message.put(PARTIAL_RESPONSE_SENT_PROPERTY, Boolean.TRUE);
    }

    /**
     * Retrieve indication that a partial response has been sent.
     *
     * @param message the current message
     * @return the retrieved indication that a partial response
     * has been sent
     */
    public static boolean retrievePartialResponseSent(Message message) {
        Boolean ret = (Boolean)message.get(PARTIAL_RESPONSE_SENT_PROPERTY);
        return ret != null && ret.booleanValue();
    }

    /**
     * Store indication that a deferred uncorrelated message abort is
     * supported
     *
     * @param message the current message
     */
    public static void storeDeferUncorrelatedMessageAbort(Message message) {
        if (message.getExchange() != null) {
            message.getExchange().put("defer.uncorrelated.message.abort", Boolean.TRUE);
        }
    }

    /**
     * Retrieve indication that a deferred uncorrelated message abort is
     * supported
     *
     * @param message the current message
     * @return the retrieved indication
     */
    public static boolean retrieveDeferUncorrelatedMessageAbort(Message message) {
        Boolean ret = message.getExchange() != null
                      ? (Boolean)message.getExchange().get("defer.uncorrelated.message.abort")
                      : null;
        return ret != null && ret.booleanValue();
    }

    /**
     * Store indication that a deferred uncorrelated message abort should
     * occur
     *
     * @param message the current message
     */
    public static void storeDeferredUncorrelatedMessageAbort(Message message) {
        if (message.getExchange() != null) {
            message.getExchange().put("deferred.uncorrelated.message.abort", Boolean.TRUE);
        }
    }

    /**
     * Retrieve indication that a deferred uncorrelated message abort should
     * occur.
     *
     * @param message the current message
     * @return the retrieved indication
     */
    public static boolean retrieveDeferredUncorrelatedMessageAbort(Message message) {
        Boolean ret = message.getExchange() != null
                      ? (Boolean)message.getExchange().get("deferred.uncorrelated.message.abort")
                      : null;
        return ret != null && ret.booleanValue();
    }

    /**
     * Retrieve indication that an async post-response service invocation
     * is required.
     *
     * @param message the current message
     * @return the retrieved indication that an async post-response service
     * invocation is required.
     */
    public static boolean retrieveAsyncPostResponseDispatch(Message message) {
        //Liberty code change start
        Boolean ret = (Boolean)((MessageImpl) message).getAsyncPostDispatch();
        //Liberty code change end
        return ret != null && ret.booleanValue();
    }

    /**
     * @return a generated UUID
     */
    public static String generateUUID() {
        return URN_UUID + UUID.randomUUID();
    }

    /**
     * Retreive Conduit from Exchange if not already available
     *
     * @param conduit the current value for the Conduit
     * @param message the current message
     * @return the Conduit if available
     */
    public static Conduit getConduit(Conduit conduit, Message message) {
        if (conduit == null) {
            Exchange exchange = message.getExchange();
            conduit = exchange != null ? exchange.getConduit(message) : null;
        }
        return conduit;
    }

    public static EndpointReferenceType getNoneEndpointReference() {
        return NONE_ENDPOINT_REFERENCE;
    }

    public static void applyReferenceParam(EndpointReferenceType toEpr, Object el) {
        if (null == toEpr.getReferenceParameters()) {
            toEpr.setReferenceParameters(WSA_OBJECT_FACTORY.createReferenceParametersType());
        }
        toEpr.getReferenceParameters().getAny().add(el);
    }

    /**
     * Create a Binding specific Message.
     *
     * @param exchange the current exchange
     * @return the Method from the BindingOperationInfo
     */
    public static Message createMessage(Exchange exchange) {
        Endpoint ep = exchange.getEndpoint();
        Message msg = null;
        if (ep != null) {
            msg = new MessageImpl();
            msg.setExchange(exchange);
            if (ep.getBinding() != null) {
                msg = ep.getBinding().createMessage(msg);
            }
        }
        return msg;
    }

    public static Destination createDecoupledDestination(Exchange exchange,
                                                         final EndpointReferenceType reference) {
        final EndpointInfo ei = exchange.getEndpoint().getEndpointInfo();
        return new Destination() {
            public EndpointReferenceType getAddress() {
                return reference;
            }
            public Conduit getBackChannel(Message inMessage) throws IOException {
                Bus bus = inMessage.getExchange().getBus();
                //this is a response targeting a decoupled endpoint.   Treat it as a oneway so
                //we don't wait for a response.
                inMessage.getExchange().setOneWay(true);
                ConduitInitiator conduitInitiator
                    = bus.getExtension(ConduitInitiatorManager.class)
                        .getConduitInitiatorForUri(reference.getAddress().getValue());
                if (conduitInitiator != null) {
                    Conduit c = conduitInitiator.getConduit(ei, reference, bus);
                    //ensure decoupled back channel input stream is closed
                    c.setMessageObserver(new MessageObserver() {
                        public void onMessage(Message m) {
                            InputStream is = m.getContent(InputStream.class);
                            if (is != null) {
                                try {
                                    is.close();
                                } catch (Exception e) {
                                    //ignore
                                }
                            }
                        }
                    });
                    return c;
                }
                return null;
            }
            public MessageObserver getMessageObserver() {
                return null;
            }
            public void shutdown() {
            }
            public void setMessageObserver(MessageObserver observer) {
            }
        };
    }
}
