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

package org.apache.cxf.binding.soap.saaj;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.attachment.AttachmentDataSource;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.headers.Header;
import org.apache.cxf.headers.HeaderManager;
import org.apache.cxf.headers.HeaderProcessor;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

/**
 * Builds a SAAJ tree from the Document fragment inside the message which contains
 * the SOAP headers and from the XMLStreamReader.
 */
@NoJSR250Annotations
public class SAAJInInterceptor extends AbstractSoapInterceptor {
    public static final SAAJInInterceptor INSTANCE = new SAAJInInterceptor();
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(SAAJInInterceptor.class);
    private static final String BODY_FILLED_IN = SAAJInInterceptor.class.getName() + ".BODY_DONE";

    private SAAJPreInInterceptor preInterceptor = SAAJPreInInterceptor.INSTANCE;
    private List<PhaseInterceptor<? extends Message>> extras 
        = new ArrayList<PhaseInterceptor<? extends Message>>(1);
    public SAAJInInterceptor() {
        super(Phase.PRE_PROTOCOL);
        extras.add(preInterceptor);
    }
    public SAAJInInterceptor(String phase) {
        super(phase);
    }
    
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return extras;
    }

    /**
     * This class sets up the Document in the Message so that the ReadHeadersInterceptor
     * can read directly into the SAAJ document instead of creating a new DOM
     * that we would need to copy into the SAAJ later.
     */
    public static class SAAJPreInInterceptor extends AbstractSoapInterceptor {
        public static final SAAJPreInInterceptor INSTANCE = new SAAJPreInInterceptor();
        
        private MessageFactory factory11;
        private MessageFactory factory12;
        
        public SAAJPreInInterceptor() {
            super(Phase.READ);
            addBefore(ReadHeadersInterceptor.class.getName());
        }
        public void handleMessage(SoapMessage message) throws Fault {
            if (isGET(message)) {
                return;
            }
            if (isRequestor(message) && message.getExchange().getInMessage() == null) {
                //already processed
                return;
            }
            try {
                XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
                if (xmlReader == null) {
                    return;
                }
                if (xmlReader.nextTag() == XMLStreamConstants.START_ELEMENT) {
                    ReadHeadersInterceptor.readVersion(xmlReader, message);
                }
                MessageFactory factory = getFactory(message);
                SOAPMessage soapMessage = factory.createMessage();
                message.setContent(SOAPMessage.class, soapMessage);
                
                SOAPPart part = soapMessage.getSOAPPart();
                message.setContent(Node.class, part);
                message.put(W3CDOMStreamWriter.class, new SAAJStreamWriter(part));
                message.put(BODY_FILLED_IN, Boolean.FALSE);

            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception e) {
                throw new SoapFault("XML_STREAM_EXC", BUNDLE, e, message.getVersion().getSender());
            }
        }
        public synchronized MessageFactory getFactory(SoapMessage message) throws SOAPException {
            if (message.getVersion() instanceof Soap11) {
                if (factory11 == null) { 
                    factory11 = SAAJFactoryResolver.createMessageFactory(message.getVersion());
                } 
                return factory11;
            }
            if (message.getVersion() instanceof Soap12) {
                if (factory12 == null) {
                    factory12 = SAAJFactoryResolver.createMessageFactory(message.getVersion());
                }
                return factory12;
            }
            return SAAJFactoryResolver.createMessageFactory(null);
        }
    }
    
    
    public void handleMessage(SoapMessage message) throws Fault {
        if (isGET(message)) {
            return;
        }
        Boolean bodySet = (Boolean)message.get(BODY_FILLED_IN);
        if (bodySet != null && bodySet == Boolean.TRUE) {
            return;
        }
        message.put(BODY_FILLED_IN, Boolean.TRUE);

        try {
            SOAPMessage soapMessage = message.getContent(SOAPMessage.class);
            if (soapMessage == null) {
                MessageFactory factory = preInterceptor.getFactory(message);
                soapMessage = factory.createMessage();
                message.setContent(SOAPMessage.class, soapMessage);
            }
            XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
            final SOAPPart part = soapMessage.getSOAPPart();
            Document node = (Document) message.getContent(Node.class);
            if (node != part && node != null) {
                StaxUtils.copy(node, new SAAJStreamWriter(part));
            } else {
                SOAPEnvelope env = soapMessage.getSOAPPart().getEnvelope();
                @SuppressWarnings("unchecked")
                List<XMLEvent> envEvents = (List<XMLEvent>)message.get(ReadHeadersInterceptor.ENVELOPE_EVENTS);
                applyEvents(envEvents, env);
                SOAPBody body = soapMessage.getSOAPBody();
                @SuppressWarnings("unchecked")
                List<XMLEvent> bodyEvents = (List<XMLEvent>)message.get(ReadHeadersInterceptor.BODY_EVENTS);
                applyEvents(bodyEvents, body);
            }
            message.setContent(Node.class, soapMessage.getSOAPPart());

            Collection<Attachment> atts = message.getAttachments();
            if (atts != null) {
                for (Attachment a : atts) {
                    if (a.getDataHandler().getDataSource() instanceof AttachmentDataSource) {
                        try {
                            ((AttachmentDataSource)a.getDataHandler().getDataSource()).cache(message);
                        } catch (IOException e) {
                            throw new Fault(e);
                        }
                    }
                    AttachmentPart ap = soapMessage.createAttachmentPart(a.getDataHandler());
                    Iterator<String> i = a.getHeaderNames();
                    while (i != null && i.hasNext()) {
                        String h = i.next();
                        String val = a.getHeader(h);
                        ap.addMimeHeader(h, val);
                    }
                    if (StringUtils.isEmpty(ap.getContentId())) {
                        ap.setContentId(a.getId());
                    }
                    soapMessage.addAttachmentPart(ap);
                }
            }
            
            //replace header element if necessary
            if (message.hasHeaders()) {
                replaceHeaders(soapMessage, message);
            }

            if (soapMessage.getSOAPPart().getEnvelope().getHeader() == null) {
                soapMessage.getSOAPPart().getEnvelope().addHeader();
            }
            
            StaxUtils.readDocElements(soapMessage.getSOAPPart().getEnvelope().getBody(),
                                      xmlReader, true, true);
            DOMSource bodySource = new DOMSource(soapMessage.getSOAPPart().getEnvelope().getBody());
            xmlReader = StaxUtils.createXMLStreamReader(bodySource);
            xmlReader.nextTag();
            xmlReader.nextTag(); // move past body tag
            message.setContent(XMLStreamReader.class, xmlReader);           
        } catch (SOAPException soape) {
            throw new SoapFault(new org.apache.cxf.common.i18n.Message(
                    "SOAPHANDLERINTERCEPTOR_EXCEPTION", BUNDLE), soape,
                    message.getVersion().getSender());
        } catch (XMLStreamException e) {
            throw new SoapFault(new org.apache.cxf.common.i18n.Message(
                    "SOAPHANDLERINTERCEPTOR_EXCEPTION", BUNDLE), e, message
                    .getVersion().getSender());
        }
    }
    
    private static void applyEvents(List<XMLEvent> events, SOAPElement el) throws SOAPException {
        if (events != null) {
            for (XMLEvent ev : events) {
                if (ev.isNamespace()) {
                    el.addNamespaceDeclaration(((Namespace)ev).getPrefix(), ((Namespace)ev).getNamespaceURI());
                } else if (ev.isAttribute()) {
                    el.addAttribute(((Attribute)ev).getName(), ((Attribute)ev).getValue());
                }
            }
        }
    }

    public static void replaceHeaders(SOAPMessage soapMessage, SoapMessage message) throws SOAPException {
        SOAPHeader header = SAAJUtils.getHeader(soapMessage);
        if (header == null) {
            return;
        }
        Element elem = DOMUtils.getFirstElement(header);
        while (elem != null) {
            Bus b = message.getExchange() == null ? null : message.getExchange().get(Bus.class);
            HeaderProcessor p =  null;
            if (b != null && b.getExtension(HeaderManager.class) != null) {
                p = b.getExtension(HeaderManager.class).getHeaderProcessor(elem.getNamespaceURI());
            }
                
            Object obj;
            DataBinding dataBinding = null;
            if (p == null || p.getDataBinding() == null) {
                obj = elem;
            } else {
                obj = p.getDataBinding().createReader(Node.class).read(elem);
            }
            //TODO - add the interceptors
                
            SoapHeader shead = new SoapHeader(new QName(elem.getNamespaceURI(),
                                                        elem.getLocalName()),
                                               obj,
                                               dataBinding);
            shead.setDirection(SoapHeader.Direction.DIRECTION_IN);
                
            String mu = elem.getAttributeNS(message.getVersion().getNamespace(),
                    message.getVersion().getAttrNameMustUnderstand());
            String act = elem.getAttributeNS(message.getVersion().getNamespace(),
                    message.getVersion().getAttrNameRole());
                
            shead.setActor(act);
            shead.setMustUnderstand(Boolean.valueOf(mu) || "1".equals(mu));
            Header oldHdr = message.getHeader(
                    new QName(elem.getNamespaceURI(), elem.getLocalName()));
            if (oldHdr != null) {
                message.getHeaders().remove(oldHdr);
            } 
            message.getHeaders().add(shead);                        
            
            elem = DOMUtils.getNextElement(elem);
        }
    }
}
