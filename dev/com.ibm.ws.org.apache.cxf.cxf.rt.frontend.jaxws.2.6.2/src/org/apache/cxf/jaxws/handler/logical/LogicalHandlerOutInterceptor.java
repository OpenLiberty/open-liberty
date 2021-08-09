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

package org.apache.cxf.jaxws.handler.logical;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Binding;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.handler.AbstractJAXWSHandlerInterceptor;
import org.apache.cxf.jaxws.handler.HandlerChainInvoker;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.MessageObserver;


public class LogicalHandlerOutInterceptor 
    extends AbstractJAXWSHandlerInterceptor<Message> {
    
    public static final String ORIGINAL_WRITER 
        = LogicalHandlerOutInterceptor.class.getName() + ".original_writer";
    private LogicalHandlerOutEndingInterceptor ending;
    
    public LogicalHandlerOutInterceptor(Binding binding) {
        super(binding, Phase.PRE_MARSHAL);
        ending = new LogicalHandlerOutEndingInterceptor(binding);
    }
    
    public void handleMessage(Message message) throws Fault {
        if (binding.getHandlerChain().isEmpty()) {
            return;
        }
        HandlerChainInvoker invoker = getInvoker(message);
        if (invoker.getLogicalHandlers().isEmpty()) {
            return;
        }
        
        try {
            
            XMLStreamWriter origWriter = message.getContent(XMLStreamWriter.class);
            
            Node nd = message.getContent(Node.class);
            SOAPMessage m = message.getContent(SOAPMessage.class);
            Document document = null;
            
            if (m != null) {
                document = m.getSOAPPart();
            } else if (nd != null) {
                document = nd.getOwnerDocument();
            } else {
                document = XMLUtils.newDocument();
                message.setContent(Node.class, document);
                nd = document;
            }
            
            W3CDOMStreamWriter writer = new W3CDOMStreamWriter(document.createDocumentFragment());
        
            // Replace stax writer with DomStreamWriter
            message.setContent(XMLStreamWriter.class, writer);        
            message.put(ORIGINAL_WRITER, origWriter);
            
            message.getInterceptorChain().add(ending);
        } catch (ParserConfigurationException e) {
            throw new Fault(e);
        }
    }
    @Override
    public void handleFault(Message message) {
        super.handleFault(message);
        XMLStreamWriter os = (XMLStreamWriter)message.get(ORIGINAL_WRITER);
        if (os != null) {
            message.setContent(XMLStreamWriter.class, os);
        }
    }
    
    private class LogicalHandlerOutEndingInterceptor 
        extends AbstractJAXWSHandlerInterceptor<Message> {
    
        public LogicalHandlerOutEndingInterceptor(Binding binding) {
            super(binding, Phase.POST_MARSHAL);
        }
    
        public void handleMessage(Message message) throws Fault {
            W3CDOMStreamWriter domWriter = (W3CDOMStreamWriter)message.getContent(XMLStreamWriter.class);
            XMLStreamWriter origWriter = (XMLStreamWriter)message
                .get(LogicalHandlerOutInterceptor.ORIGINAL_WRITER);  
            
            
            HandlerChainInvoker invoker = getInvoker(message);
            LogicalMessageContextImpl lctx = new LogicalMessageContextImpl(message);
            invoker.setLogicalMessageContext(lctx);
            boolean requestor = isRequestor(message);
            
            
            XMLStreamReader reader = (XMLStreamReader)message.get("LogicalHandlerInterceptor.INREADER");
            SOAPMessage origMessage = null;
            if (reader != null) {
                origMessage = message.getContent(SOAPMessage.class);
                message.setContent(XMLStreamReader.class, reader);
                message.removeContent(SOAPMessage.class);
            } else if (domWriter.getCurrentFragment() != null) {
                DocumentFragment frag = domWriter.getCurrentFragment();
                Node nd = frag.getFirstChild();
                while (nd != null && !(nd instanceof Element)) {
                    nd = nd.getNextSibling();
                }
                Source source = new DOMSource(nd);
                message.setContent(Source.class, source);
                message.setContent(XMLStreamReader.class,
                                   new W3CDOMStreamReader(domWriter.getCurrentFragment()));
            } else if (domWriter.getDocument().getDocumentElement() != null) {
                Source source = new DOMSource(domWriter.getDocument());
                message.setContent(Source.class, source);
                message.setContent(XMLStreamReader.class, 
                                   StaxUtils.createXMLStreamReader(domWriter.getDocument()));
            }            
            
            if (!invoker.invokeLogicalHandlers(requestor, lctx)) {
                if (requestor) {
                    // client side - abort
                    message.getInterceptorChain().abort();
                    if (!message.getExchange().isOneWay()) {
                        Endpoint e = message.getExchange().get(Endpoint.class);
                        Message responseMsg = new MessageImpl();
                        responseMsg.setExchange(message.getExchange());
                        responseMsg = e.getBinding().createMessage(responseMsg);            
    
                        MessageObserver observer = message.getExchange()
                                    .get(MessageObserver.class);
                        if (observer != null) {
                            //client side outbound, the request message becomes the response message
                            responseMsg.setContent(XMLStreamReader.class, message
                                .getContent(XMLStreamReader.class));                        
                            
                            message.getExchange().setInMessage(responseMsg);
                            responseMsg.put(PhaseInterceptorChain.STARTING_AT_INTERCEPTOR_ID,
                                            LogicalHandlerInInterceptor.class.getName());
                            observer.onMessage(responseMsg);
                        }
                        return;
                    }
                } else {
                    // server side - abort
                    // Even return false, also should try to set the XMLStreamWriter using
                    // reader or domWriter, or the SOAPMessage's body maybe empty.
                }
            }
            if (origMessage != null) {
                message.setContent(SOAPMessage.class, origMessage);
            }
            
            try {
                reader = message.getContent(XMLStreamReader.class);
                message.removeContent(XMLStreamReader.class);
                if (reader != null) {
                    StaxUtils.copy(reader, origWriter);
                } else if (domWriter.getDocument().getDocumentElement() != null) {
                    StaxUtils.copy(domWriter.getDocument(), origWriter);
                }
                message.setContent(XMLStreamWriter.class, origWriter);
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
        }
    }
    
}
