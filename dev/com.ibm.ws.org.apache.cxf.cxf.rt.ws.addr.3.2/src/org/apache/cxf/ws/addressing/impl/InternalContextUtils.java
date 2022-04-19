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

package org.apache.cxf.ws.addressing.impl;


import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.WebFault;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.NullConduitSelector;
import org.apache.cxf.endpoint.PreexistingConduitSelector;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.Extensible;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.workqueue.OneShotAsyncExecutor;
import org.apache.cxf.workqueue.SynchronousExecutor;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.FaultAction;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.Names;



/**
 * Holder for utility methods relating to contexts.
 */
final class InternalContextUtils {
    private static final class DecoupledDestination implements Destination {
        private final EndpointInfo ei;
        private final EndpointReferenceType reference;

        private DecoupledDestination(EndpointInfo ei, EndpointReferenceType reference) {
            this.ei = ei;
            this.reference = reference;
        }

        public EndpointReferenceType getAddress() {
            return reference;
        }

        public Conduit getBackChannel(Message inMessage) throws IOException {
            if (ContextUtils.isNoneAddress(reference)) {
                return null;
            }
            Bus bus = inMessage.getExchange().getBus();
            //this is a response targeting a decoupled endpoint.   Treat it as a oneway so
            //we don't wait for a response.
            inMessage.getExchange().setOneWay(true);
            ConduitInitiator conduitInitiator
                = bus.getExtension(ConduitInitiatorManager.class)
                    .getConduitInitiatorForUri(reference.getAddress().getValue());
            if (conduitInitiator != null) {
                Conduit c = conduitInitiator.getConduit(ei, reference, bus);
                // ensure decoupled back channel input stream is closed
                c.setMessageObserver(new MessageObserver() {
                    public void onMessage(Message m) {
                        InputStream is = m.getContent(InputStream.class);
                        if (is != null) {
                            try {
                                is.close();
                            } catch (Exception e) {
                                // ignore
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
    }

    private static final Logger LOG = LogUtils.getL7dLogger(InternalContextUtils.class);

   /**
    * Prevents instantiation.
    */
    private InternalContextUtils() {
    }


    /**
     * Rebase response on replyTo
     *
     * @param reference the replyTo reference
     * @param inMAPs the inbound MAPs
     * @param inMessage the current message
     */
    //CHECKSTYLE:OFF Max executable statement count limitation
    public static void rebaseResponse(EndpointReferenceType reference,
                                      AddressingProperties inMAPs,
                                      final Message inMessage) {

        String namespaceURI = inMAPs.getNamespaceURI();
        if (!ContextUtils.retrievePartialResponseSent(inMessage)) {
            ContextUtils.storePartialResponseSent(inMessage);
            Exchange exchange = inMessage.getExchange();
            Message fullResponse = exchange.getOutMessage();
            Message partialResponse = ContextUtils.createMessage(exchange);
            ensurePartialResponseMAPs(partialResponse, namespaceURI);

            // ensure the inbound MAPs are available in the partial response
            // message (used to determine relatesTo etc.)
            ContextUtils.propogateReceivedMAPs(inMAPs, partialResponse);
            Destination target = inMessage.getDestination();
            if (target == null) {
                return;
            }

            try {
                if (reference == null) {
                    reference = ContextUtils.getNoneEndpointReference();
                }
                Conduit backChannel = target.getBackChannel(inMessage);
                Exception exception = inMessage.getContent(Exception.class);
                //Add this to handle two way faultTo
                //TODO:Look at how to refactor
                if (backChannel != null && !inMessage.getExchange().isOneWay()
                    && ContextUtils.isFault(inMessage)) {
                    // send the fault message to faultTo Endpoint
                    exchange.setOutMessage(ContextUtils.createMessage(exchange));
                    exchange.put(ConduitSelector.class, new NullConduitSelector());
                    exchange.put("org.apache.cxf.http.no_io_exceptions", true);
                    Destination destination = createDecoupledDestination(exchange, reference);
                    exchange.setDestination(destination);

                    if (ContextUtils.retrieveAsyncPostResponseDispatch(inMessage)) {
                        DelegatingInputStream in = inMessage.getContent(DelegatingInputStream.class);
                        if (in != null) {
                            in.cacheInput();
                        }
                        inMessage.getInterceptorChain().reset();
                        //cleanup pathinfo
                        if (inMessage.get(Message.PATH_INFO) != null) {
                            inMessage.remove(Message.PATH_INFO);
                        }
                        inMessage.getInterceptorChain().doIntercept(inMessage);

                    }

                    // send the partial response to requester
                    partialResponse.put("forced.faultstring",
                                        "The server sent HTTP status code :"
                                            + inMessage.getExchange().get(Message.RESPONSE_CODE));
                    partialResponse.setContent(Exception.class, exception);
                    partialResponse.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS,
                                        inMessage.get(Message.PROTOCOL_HEADERS));
                    partialResponse.put(org.apache.cxf.message.Message.ENCODING,
                                        inMessage.get(Message.ENCODING));
                    partialResponse.put(ContextUtils.ACTION, inMessage.get(ContextUtils.ACTION));
                    partialResponse.put("javax.xml.ws.addressing.context.inbound",
                                        inMessage.get("javax.xml.ws.addressing.context.inbound"));
                    partialResponse.put("javax.xml.ws.addressing.context.outbound",
                                        inMessage.get("javax.xml.ws.addressing.context.outbound"));
                    exchange.setOutMessage(partialResponse);
                    PhaseInterceptorChain newChian = ((PhaseInterceptorChain)inMessage.getInterceptorChain())
                        .cloneChain();
                    partialResponse.setInterceptorChain(newChian);
                    exchange.setDestination(target);
                    exchange.setOneWay(false);
                    exchange.put(ConduitSelector.class,
                                 new PreexistingConduitSelector(backChannel, exchange.getEndpoint()));
                    if (newChian != null && !newChian.doIntercept(partialResponse)
                        && partialResponse.getContent(Exception.class) != null) {
                        if (partialResponse.getContent(Exception.class) instanceof Fault) {
                            throw (Fault)partialResponse.getContent(Exception.class);
                        }
                        throw new Fault(partialResponse.getContent(Exception.class));
                    }
                    return;
                }

                if (backChannel != null) {
                    partialResponse.put(Message.PARTIAL_RESPONSE_MESSAGE, Boolean.TRUE);
                    partialResponse.put(Message.EMPTY_PARTIAL_RESPONSE_MESSAGE, Boolean.TRUE);
                    boolean robust =
                        MessageUtils.getContextualBoolean(inMessage, Message.ROBUST_ONEWAY, false);

                    if (robust) {
                        BindingOperationInfo boi = exchange.getBindingOperationInfo();
                        // insert the executor in the exchange to fool the OneWayProcessorInterceptor
                        exchange.put(Executor.class, getExecutor(inMessage));
                        // pause dispatch on current thread and resume...
                        inMessage.getInterceptorChain().pause();
                        inMessage.getInterceptorChain().resume();
                        MessageObserver faultObserver = inMessage.getInterceptorChain().getFaultObserver();
                        if (null != inMessage.getContent(Exception.class) && null != faultObserver) {
                            // return the fault over the response fault channel
                            inMessage.getExchange().setOneWay(false);
                            faultObserver.onMessage(inMessage);
                            return;
                        }
                        // restore the BOI for the partial response handling
                        exchange.put(BindingOperationInfo.class, boi);
                    }


                    // set up interceptor chains and send message
                    InterceptorChain chain =
                        fullResponse != null
                        ? fullResponse.getInterceptorChain()
                        : OutgoingChainInterceptor.getOutInterceptorChain(exchange);
                    exchange.setOutMessage(partialResponse);
                    partialResponse.setInterceptorChain(chain);
                    exchange.put(ConduitSelector.class,
                                 new PreexistingConduitSelector(backChannel,
                                                                exchange.getEndpoint()));
                    if (ContextUtils.retrieveAsyncPostResponseDispatch(inMessage) && !robust) {
                        //need to suck in all the data from the input stream as
                        //the transport might discard any data on the stream when this
                        //thread unwinds or when the empty response is sent back
                        DelegatingInputStream in = inMessage.getContent(DelegatingInputStream.class);
                        if (in != null) {
                            in.cacheInput();
                        }
                    }
                    if (chain != null && !chain.doIntercept(partialResponse)
                        && partialResponse.getContent(Exception.class) != null) {
                        if (partialResponse.getContent(Exception.class) instanceof Fault) {
                            throw (Fault)partialResponse.getContent(Exception.class);
                        }
                        throw new Fault(partialResponse.getContent(Exception.class));
                    }
                    if (chain != null) {
                        chain.reset();
                    }
                    exchange.put(ConduitSelector.class, new NullConduitSelector());

                    if (fullResponse == null) {
                        fullResponse = ContextUtils.createMessage(exchange);
                    }
                    exchange.setOutMessage(fullResponse);

                    Destination destination = createDecoupledDestination(
                        exchange,
                        reference);
                    exchange.setDestination(destination);


                    if (ContextUtils.retrieveAsyncPostResponseDispatch(inMessage) && !robust) {

                        // async service invocation required *after* a response
                        // has been sent (i.e. to a oneway, or a partial response
                        // to a decoupled twoway)

                        //cleanup pathinfo
                        if (inMessage.get(Message.PATH_INFO) != null) {
                            inMessage.remove(Message.PATH_INFO);
                        }
                        // pause dispatch on current thread ...
                        inMessage.getInterceptorChain().pause();

                        try {
                            // ... and resume on executor thread
                            getExecutor(inMessage).execute(new Runnable() {
                                public void run() {
                                    inMessage.getInterceptorChain().resume();
                                }
                            });
                        } catch (RejectedExecutionException e) {
                            LOG.warning(
                                        "Executor queue is full, use the caller thread."
                                        + "  Users can specify a larger executor queue to avoid this.");
                            // only block the thread if the prop is unset or set to false, otherwise let it go
                            if (!MessageUtils.getContextualBoolean(inMessage,
                                    "org.apache.cxf.oneway.rejected_execution_exception")) {
                                //the executor queue is full, so run the task in the caller thread
                                inMessage.getInterceptorChain().resume();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "SERVER_TRANSPORT_REBASE_FAILURE_MSG", e);
            }
        }
    }
    //CHECKSTYLE:ON

    public static Destination createDecoupledDestination(
        Exchange exchange, final EndpointReferenceType reference) {
        final EndpointInfo ei = exchange.getEndpoint().getEndpointInfo();
        return new DecoupledDestination(ei, reference);
    }

    /**
     * Construct and store MAPs for partial response.
     *
     * @param partialResponse the partial response message
     * @param namespaceURI the current namespace URI
     */
    private static void ensurePartialResponseMAPs(Message partialResponse,
                                                 String namespaceURI) {
        // ensure there is a MAPs instance available for the outbound
        // partial response that contains appropriate To and ReplyTo
        // properties (i.e. anonymous & none respectively)
        AddressingProperties maps = new AddressingProperties();
        maps.setTo(EndpointReferenceUtils.getAnonymousEndpointReference());
        maps.setReplyTo(ContextUtils.WSA_OBJECT_FACTORY.createEndpointReferenceType());
        maps.getReplyTo().setAddress(ContextUtils.getAttributedURI(Names.WSA_NONE_ADDRESS));
        maps.setAction(ContextUtils.getAttributedURI(""));
        maps.exposeAs(namespaceURI);
        ContextUtils.storeMAPs(maps, partialResponse, true, true, false);
    }




    /**
     * Construct the Action URI.
     *
     * @param message the current message
     * @return the Action URI
     */
    public static AttributedURIType getAction(Message message) {
        String action = null;
        LOG.fine("Determining action");
        Exception fault = message.getContent(Exception.class);

        if (fault instanceof Fault
            && Names.WSA_NAMESPACE_NAME.equals(((Fault)fault).getFaultCode().getNamespaceURI())) {
            // wsa relevant faults should use the wsa-fault action value
            action = Names.WSA_DEFAULT_FAULT_ACTION;
        } else {
            FaultAction annotation = null;
            if (fault != null) {
                annotation = fault.getClass().getAnnotation(FaultAction.class);
            }
            if ((annotation != null) && (annotation.value() != null)) {
                action = annotation.value();
            } else {
                action = getActionFromServiceModel(message, fault);
            }
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("action: " + action);
        }
        return action != null ? ContextUtils.getAttributedURI(action) : null;
    }

    /**
     * Get action from service model.
     *
     * @param message the current message
     * @param fault the fault if one is set
     */
    private static String getActionFromServiceModel(Message message,
                                                    Exception fault) {
        String action = null;
        BindingOperationInfo bindingOpInfo =
            message.getExchange().getBindingOperationInfo();
        if (bindingOpInfo != null) {
            if (bindingOpInfo.isUnwrappedCapable()) {
                bindingOpInfo = bindingOpInfo.getUnwrappedOperation();
            }
            if (fault == null) {
                action = (String)message.get(ContextUtils.ACTION);
                if (StringUtils.isEmpty(action)) {
                    action = (String) message.get(SoapBindingConstants.SOAP_ACTION);
                }
                if (action == null || "".equals(action)) {
                    MessageInfo msgInfo =
                        ContextUtils.isRequestor(message)
                        ? bindingOpInfo.getOperationInfo().getInput()
                        : bindingOpInfo.getOperationInfo().getOutput();
                    String cachedAction = (String)msgInfo.getProperty(ContextUtils.ACTION);
                    if (cachedAction == null) {
                        action = getActionFromMessageAttributes(msgInfo);
                    } else {
                        action = cachedAction;
                    }
                    if (action == null && ContextUtils.isRequestor(message)) {
                        SoapOperationInfo soi = getSoapOperationInfo(bindingOpInfo);
                        action = soi == null ? null : soi.getAction();
                        action = StringUtils.isEmpty(action) ? null : action;
                    }
                }
            } else {
                Throwable t = fault.getCause();

                // FaultAction attribute is not defined in
                // http://www.w3.org/2005/02/addressing/wsdl schema
                for (BindingFaultInfo bfi : bindingOpInfo.getFaults()) {
                    FaultInfo fi = bfi.getFaultInfo();
                    if (fi.size() == 0) {
                        continue;
                    }
                    if (t != null && matchFault(t, fi)) {
                        if (fi.getExtensionAttributes() == null) {
                            continue;
                        }
                        String attr = (String)
                            fi.getExtensionAttributes().get(Names.WSAW_ACTION_QNAME);
                        if (attr == null) {
                            attr = (String)
                                fi.getExtensionAttributes()
                                    .get(new QName(Names.WSA_NAMESPACE_WSDL_NAME_OLD,
                                                    Names.WSAW_ACTION_NAME));
                        }
                        if (attr != null) {
                            action = attr;
                            break;
                        }
                    }
                }
            }
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("action determined from service model: " + action);
        }
        return action;
    }

    private static boolean matchFault(Throwable t, FaultInfo fi) {
        //REVISIT not sure if this class-based comparison works in general as the fault class defined
        // in the service interface has no direct relationship to the message body's type.
        MessagePartInfo fmpi = fi.getFirstMessagePart();
        Class<?> fiTypeClass = fmpi.getTypeClass();
        if (fiTypeClass != null && t.getClass().isAssignableFrom(fiTypeClass)) {
            return true;
        }
        // CXF-6575
        QName fiName = fmpi.getConcreteName();
        WebFault wf = t.getClass().getAnnotation(WebFault.class);
        return wf != null  && fiName != null
            && wf.targetNamespace() != null && wf.targetNamespace().equals(fiName.getNamespaceURI())
            && wf.name() != null && wf.name().equals(fiName.getLocalPart());
    }

    public static SoapOperationInfo getSoapOperationInfo(BindingOperationInfo bindingOpInfo) {
        SoapOperationInfo soi = bindingOpInfo.getExtensor(SoapOperationInfo.class);
        if (soi == null && bindingOpInfo.isUnwrapped()) {
            soi = bindingOpInfo.getWrappedOperation()
                .getExtensor(SoapOperationInfo.class);
        }
        return soi;
    }

    /**
     * Get action from attributes on MessageInfo
     *
     * @param msgInfo the current MessageInfo
     * @return the action if set
     */
    private static String getActionFromMessageAttributes(MessageInfo msgInfo) {
        String action = null;
        if (msgInfo != null
            && msgInfo.getExtensionAttributes() != null) {
            String attr = getAction(msgInfo);
            if (!StringUtils.isEmpty(attr)) {
                action = attr;
                msgInfo.setProperty(ContextUtils.ACTION, action);
            }
        }
        return action;
    }

    public static String getAction(Extensible ext) {
        Object o = ext.getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME);
        if (o == null) {
            o = ext.getExtensionAttributes().get(new QName(Names.WSA_NAMESPACE_WSDL_METADATA,
                                                           Names.WSAW_ACTION_NAME));
        }
        if (o == null) {
            o = ext.getExtensionAttributes().get(new QName(JAXWSAConstants.NS_WSA, Names.WSAW_ACTION_NAME));
        }
        if (o == null) {
            o = ext.getExtensionAttributes().get(new QName(Names.WSA_NAMESPACE_WSDL_NAME_OLD,
                                                   Names.WSAW_ACTION_NAME));
        }
        if (o instanceof QName) {
            return ((QName)o).getLocalPart();
        }
        return o == null ? null : o.toString();
    }

    /**
     * Get the Executor for this invocation.
     * @param message the current Message
     * @return the executor for this invocation
     */
    private static Executor getExecutor(final Message message) {
        Endpoint endpoint = message.getExchange().getEndpoint();
        Executor executor = endpoint.getService().getExecutor();

        if (executor == null || SynchronousExecutor.isA(executor)) {
            // need true asynchrony
            Bus bus = message.getExchange().getBus();
            if (bus != null) {
                WorkQueueManager workQueueManager =
                    bus.getExtension(WorkQueueManager.class);
                Executor autoWorkQueue =
                    workQueueManager.getNamedWorkQueue("ws-addressing");
                executor = autoWorkQueue != null
                           ? autoWorkQueue
                           :  workQueueManager.getAutomaticWorkQueue();
            } else {
                executor = OneShotAsyncExecutor.getInstance();
            }
        }
        message.getExchange().put(Executor.class, executor);
        return executor;
    }

}
