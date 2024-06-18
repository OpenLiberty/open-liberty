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

package org.apache.cxf.jaxws.handler.soap;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.apache.cxf.binding.soap.HeaderUtil;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.jaxws.handler.AbstractProtocolHandlerInterceptor;
import org.apache.cxf.jaxws.handler.HandlerChainInvoker;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.Names;



import com.ibm.websphere.ras.annotation.Sensitive;

// Liberty Change - This class has no Liberty specific changes other than the Sensitive annotation 
// It is required as an overlay because of Liberty specific changes to MessageImpl.put(). Any call
// to SoapMessage.put() will cause a NoSuchMethodException in the calling class if the class is not recompiled.
// If a solution to this compilation issue can be found, this class should be removed as an overlay. 
// Liberty change: traces are added issue #26529
public class SOAPHandlerInterceptor extends
        AbstractProtocolHandlerInterceptor<SoapMessage> implements
        SoapInterceptor {
    private static final SAAJOutInterceptor SAAJ_OUT = new SAAJOutInterceptor();

    private static final Logger LOG = LogUtils.getLogger(SOAPHandlerInterceptor.class); // Liberty Change issue #26529
    AbstractSoapInterceptor ending = new AbstractSoapInterceptor(
            SOAPHandlerInterceptor.class.getName() + ".ENDING",
            Phase.USER_PROTOCOL) {

        public void handleMessage(@Sensitive SoapMessage message) throws Fault { // Liberty Change
            handleMessageInternal(message);
        }
    };

    public SOAPHandlerInterceptor(Binding binding) {
        super(binding, Phase.PRE_PROTOCOL_FRONTEND);
    }

    public Set<URI> getRoles() {
        return new HashSet<>();
    }

    public Set<QName> getUnderstoodHeaders() {
        Set<QName> understood = new HashSet<>();
        for (Handler<?> h : getBinding().getHandlerChain()) {
            if (h instanceof SOAPHandler) {
                Set<QName> headers = CastUtils.cast(((SOAPHandler<?>) h).getHeaders());
                if (headers != null) {
                    understood.addAll(headers);
                }
            }
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Headers that are obtained from SOAPHandler and it's subclasses: " + understood.toArray());   // Liberty Change issue #26529
        }
        return understood;
    }

    public void handleMessage(@Sensitive SoapMessage message) { // Liberty Change
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change issue #26529
        if (binding.getHandlerChain().isEmpty()) {
            if (isFinestEnabled) {
                LOG.finest("Handler chain is empty. handleMessage won't be executed.");   // Liberty Change issue #26529
            }
            return;
        }
        if (getInvoker(message).getProtocolHandlers().isEmpty()) {
            if (isFinestEnabled) {
                LOG.finest("Message does not contain any protocol handlers. handleMessage won't be executed.");   // Liberty Change issue #26529
            }
            return;
        }

        checkUnderstoodHeaders(message);

        if (getInvoker(message).isOutbound()) {
            if (!chainAlreadyContainsSAAJ(message)) {
                SAAJ_OUT.handleMessage(message);
            }
            if (isFinestEnabled) {
                LOG.finest(ending + " will be added to interceptor chain for outbound message.");   // Liberty Change issue #26529
            }
            message.getInterceptorChain().add(ending);
        } else {
            boolean isFault = handleMessageInternal(message);
            SOAPMessage msg = message.getContent(SOAPMessage.class);
            if (msg != null) {
                XMLStreamReader xmlReader = createXMLStreamReaderFromSOAPMessage(msg);
                message.setContent(XMLStreamReader.class, xmlReader);
                // replace headers
                try {
                    SAAJInInterceptor.replaceHeaders(msg, message);
                } catch (SOAPException e) {
                    e.printStackTrace();
                }
            }
            if (isFault) {
                Endpoint ep = message.getExchange().getEndpoint();
                message.getInterceptorChain().abort();
                if (ep.getInFaultObserver() != null) {
                    ep.getInFaultObserver().onMessage(message);
                } else {
                    if (isFinestEnabled) {
                        LOG.finest("MessageObserver(ep.getInFaultObserver())  is null. onMessage method of MessageObserver won't get executed.");   // Liberty Change issue #26529
                    }
                }
            }
        }
    }

    private void checkUnderstoodHeaders(SoapMessage soapMessage) {
        Set<QName> paramHeaders = HeaderUtil.getHeaderQNameInOperationParam(soapMessage);
        if (soapMessage.getHeaders().isEmpty() && paramHeaders.isEmpty()) {
            //the TCK expects the getHeaders method to always be
            //called.   If there aren't any headers in the message,
            //THe MustUnderstandInterceptor quickly returns without
            //trying to calculate the understood headers.   Thus,
            //we need to call it here.
            getUnderstoodHeaders();
        }
    }

    private boolean handleMessageInternal(SoapMessage message) {

        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);  // Liberty Change issue #26529
        MessageContext context = createProtocolMessageContext(message);
        if (context == null) {
            if (isFinestEnabled) {
                LOG.finest("MessageContext is null. handleMessageInternal won't be executed."); // Liberty Change issue #26529
            }
            return true;
        }

        HandlerChainInvoker invoker = getInvoker(message);
        invoker.setProtocolMessageContext(context);

        if (!invoker.invokeProtocolHandlers(isRequestor(message), context)) {
            if (isFinestEnabled) {
                LOG.finest("There is a problem invoking protocol handlers. handleAbort method will be executed."); // Liberty Change issue #26529
            }
            handleAbort(message, context);
        }

        if (isFinestEnabled) {
            LOG.finest("Is it an outbound message(isOutbound): " + isOutbound(message)); // Liberty Change issue #26529
            LOG.finest("Is it MEP complete(isMEPComlete): " + isMEPComlete(message)); // Liberty Change issue #26529
        }
        // If this is the outbound and end of MEP, call MEP completion
        if (isRequestor(message) && invoker.getLogicalHandlers().isEmpty()
            && !isOutbound(message) && isMEPComlete(message)) {
            onCompletion(message);
        } else if (isOutbound(message) && isMEPComlete(message)) {
            onCompletion(message);
        }
        return false;
    }

    private void handleAbort(SoapMessage message, MessageContext context) {
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);  // Liberty Change issue #26529
        if (isRequestor(message)) {
            // client side outbound
            if (getInvoker(message).isOutbound()) {
                message.getInterceptorChain().abort();

                MessageObserver observer = message.getExchange().get(MessageObserver.class);
                if (isFinestEnabled) {
                    LOG.finest("MessageObserver(observer): " + observer); // Liberty Change issue #26529
                }
                if (!message.getExchange().isOneWay()
                    && observer != null) {
                    Endpoint e = message.getExchange().getEndpoint();
                    Message responseMsg = new MessageImpl();
                    responseMsg.setExchange(message.getExchange());
                    responseMsg = e.getBinding().createMessage(responseMsg);

                    // the request message becomes the response message
                    message.getExchange().setInMessage(responseMsg);
                    SOAPMessage soapMessage = ((SOAPMessageContext)context).getMessage();

                    if (soapMessage != null) {
                        responseMsg.setContent(SOAPMessage.class, soapMessage);
                        XMLStreamReader xmlReader = createXMLStreamReaderFromSOAPMessage(soapMessage);
                        responseMsg.setContent(XMLStreamReader.class, xmlReader);
                    }
                    responseMsg.put(InterceptorChain.STARTING_AT_INTERCEPTOR_ID,
                                    SOAPHandlerInterceptor.class.getName());
                    if (isFinestEnabled) {
                        LOG.finest("New response message is created and will be passed as parameter to onMessage method of MessageObserver: " + responseMsg); // Liberty Change issue #26529
                    }
                    observer.onMessage(responseMsg);
                }
                //We dont call onCompletion here, as onCompletion will be called by inbound
                //LogicalHandlerInterceptor
            } else {
                if (isFinestEnabled) {
                    LOG.finest("Client side inbound onCompletion will be executed"); // Liberty Change issue #26529
                }
                // client side inbound - Normal handler message processing
                // stops, but the inbound interceptor chain still continues, dispatch the message
                //By onCompletion here, we can skip following Logical handlers
                onCompletion(message);
            }
        } else {
            if (!getInvoker(message).isOutbound()) {
                // server side inbound
                if (isFinestEnabled) {
                    LOG.finest("Server side inbound interceptor chain will be aborted."); // Liberty Change issue #26529
                }
                message.getInterceptorChain().abort();
                Endpoint e = message.getExchange().getEndpoint();
                if (!message.getExchange().isOneWay()) {
                    Message responseMsg = new MessageImpl();
                    responseMsg.setExchange(message.getExchange());
                    responseMsg = e.getBinding().createMessage(responseMsg);
                    message.getExchange().setOutMessage(responseMsg);
                    SOAPMessage soapMessage = ((SOAPMessageContext)context).getMessage();

                    responseMsg.setContent(SOAPMessage.class, soapMessage);

                    InterceptorChain chain = OutgoingChainInterceptor.getOutInterceptorChain(message
                        .getExchange());
                    responseMsg.setInterceptorChain(chain);
                    if (isFinestEnabled) {
                        LOG.finest("New response message is created and will be passed as parameter to doInterceptStartingAfter method of InterceptorChain: " + responseMsg); // Liberty Change issue #26529
                    }
                    // so the idea of starting interceptor chain from any
                    // specified point does not work
                    // well for outbound case, as many outbound interceptors
                    // have their ending interceptors.
                    // For example, we can not skip MessageSenderInterceptor.
                    chain.doInterceptStartingAfter(responseMsg,
                                                   SoapPreProtocolOutInterceptor.class.getName());
                }

            } else {
                if (isFinestEnabled) {
                    LOG.finest("Server side outbound. Do nothing."); // Liberty Change issue #26529
                }
                // server side outbound - Normal handler message processing
                // stops, but still continue the outbound interceptor chain, dispatch the message
            }
        }
    }

    @Override
    protected MessageContext createProtocolMessageContext(SoapMessage message) {        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);  // Liberty Change issue #26529
        SOAPMessageContextImpl sm = new SOAPMessageContextImpl(message);

        Exchange exch = message.getExchange();
        setupBindingOperationInfo(exch, sm);
        SOAPMessage msg = sm.getMessage();
        if (msg != null) {
            try {
                List<SOAPElement> params = new ArrayList<>();
                message.put(MessageContext.REFERENCE_PARAMETERS, params);
                SOAPHeader head = SAAJUtils.getHeader(msg);
                if (head != null) {
                    Iterator<Node> it = CastUtils.cast(head.getChildElements());
                    while (it != null && it.hasNext()) {
                        Node nd = it.next();
                        if (nd instanceof SOAPElement) {
                            SOAPElement el = (SOAPElement) nd;
                            if (el.hasAttributeNS(Names.WSA_NAMESPACE_NAME, "IsReferenceParameter")
                                    && ("1".equals(el.getAttributeNS(Names.WSA_NAMESPACE_NAME,
                                    "IsReferenceParameter"))
                                    || Boolean.parseBoolean(el.getAttributeNS(Names.WSA_NAMESPACE_NAME,
                                    "IsReferenceParameter")))) {
                                params.add(el);
                            }
                        }
                    }
                }
                if (isFinestEnabled) {
                    LOG.finest("Reference parameters that is obtained from SOAP header: " + params); // Liberty Change issue #26529
                }
                if (isRequestor(message) && msg.getSOAPPart().getEnvelope().getBody() != null
                        && msg.getSOAPPart().getEnvelope().getBody().hasFault()) {
                    if (isFinestEnabled) {
                        LOG.finest("Message body has fault on client side. Protocol message context won't be returned."); // Liberty Change issue #26529
                    }
                    return null;
                }
            } catch (SOAPException e) {
                throw new Fault(e);
            }
        }

        return sm;
    }

    private XMLStreamReader createXMLStreamReaderFromSOAPMessage(SOAPMessage soapMessage) {
        // responseMsg.setContent(SOAPMessage.class, soapMessage);
        XMLStreamReader xmlReader = null;
        try {
            DOMSource bodySource = new DOMSource(soapMessage.getSOAPPart().getEnvelope().getBody());
            xmlReader = StaxUtils.createXMLStreamReader(bodySource);
            xmlReader.nextTag();
            xmlReader.nextTag(); // move past body tag
        } catch (SOAPException | XMLStreamException e) {
            e.printStackTrace();
        }
        return xmlReader;
    }

    public void handleFault(SoapMessage message) {
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);  // Liberty Change issue #26529
        if (binding.getHandlerChain().isEmpty()) {
            if (isFinestEnabled) {
                LOG.finest("Handler chain is empty. handleFault won't be executed."); // Liberty Change issue #26529
            }
            return;
        }
        if (getInvoker(message).getProtocolHandlers().isEmpty()) {
            if (isFinestEnabled) {
                LOG.finest("Protocol handler list is empty. handleFault won't be executed."); // Liberty Change issue #26529
            }
            return;
        }
        if (getInvoker(message).isOutbound()
            && !chainAlreadyContainsSAAJ(message)) {
            SAAJ_OUT.handleFault(message);
        }
    }

    protected QName getOpQName(Exchange ex, Object data) {
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);  // Liberty Change issue #26529
        SOAPMessageContextImpl sm = (SOAPMessageContextImpl)data;
        try {
            SOAPMessage msg = sm.getMessage();
            if (msg == null) {
                if (isFinestEnabled) {
                    LOG.finest("SOAP message is null. OpQname will not be returned."); // Liberty Change issue #26529
                }
                return null;
            }
            SOAPBody body = SAAJUtils.getBody(msg);
            if (body == null) {
                if (isFinestEnabled) {
                    LOG.finest("SOAP body is null. OpQname will not be returned."); // Liberty Change issue #26529
                }
                return null;
            }
            org.w3c.dom.Node nd = body.getFirstChild();
            while (nd != null && !(nd instanceof org.w3c.dom.Element)) {
                nd = nd.getNextSibling();
            }
            if (nd != null) {
                return new QName(nd.getNamespaceURI(), nd.getLocalName());
            }
        } catch (SOAPException e) {
            //ignore, nothing we can do
        }
        return null;
    }

    private static boolean chainAlreadyContainsSAAJ(SoapMessage message) {
        ListIterator<Interceptor<? extends Message>> listIterator =
            message.getInterceptorChain().getIterator();
        while (listIterator.hasNext()) {
            if (listIterator.next() instanceof SAAJOutInterceptor) {
                return true;
            }
        }
        return false;
    }
}
