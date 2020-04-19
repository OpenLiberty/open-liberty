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

package org.apache.cxf.jaxws.interceptors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.activation.DataSource;
import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJOutInterceptor.SAAJOutEndingInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJStreamWriter;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.ServiceUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

public class MessageModeOutInterceptor extends AbstractPhaseInterceptor<Message> {
    MessageModeOutInterceptorInternal internal;
    SAAJOutInterceptor saajOut;
    Class<?> type;
    QName bindingName;

    public MessageModeOutInterceptor(SAAJOutInterceptor saajOut, QName bname) {
        super(Phase.PREPARE_SEND);
        this.saajOut = saajOut;
        this.bindingName = bname;
        internal = new MessageModeOutInterceptorInternal();
    }
    public MessageModeOutInterceptor(Class<?> t, QName bname) {
        super(Phase.PREPARE_SEND);
        type = t;
        this.bindingName = bname;
    }
    public void handleMessage(Message message) throws Fault {
        BindingOperationInfo bop = message.getExchange().getBindingOperationInfo();
        if (bop != null && !bindingName.equals(bop.getBinding().getName())) {
            return;
        }
        if (saajOut != null) {
            doSoap(message);
        } else if (DataSource.class.isAssignableFrom(type)) {
            //datasource stuff, must check if multi-source
            MessageContentsList list = (MessageContentsList)message.getContent(List.class);
            DataSource ds = (DataSource)list.get(0);
            String ct = ds.getContentType();
            if (ct.toLowerCase().contains("multipart/related")) {
                Message msg = new MessageImpl();
                msg.setExchange(message.getExchange());
                msg.put(Message.CONTENT_TYPE, ct);
                try {
                    msg.setContent(InputStream.class, ds.getInputStream());
                    AttachmentDeserializer deser = new AttachmentDeserializer(msg);
                    deser.initializeAttachments();
                } catch (IOException ex) {
                    throw new Fault(ex);
                }
                message.setAttachments(msg.getAttachments());
                final InputStream in = msg.getContent(InputStream.class);
                final String ct2 = (String)msg.get(Message.CONTENT_TYPE);
                list.set(0, new DataSource() {

                    public String getContentType() {
                        return ct2;
                    }

                    public InputStream getInputStream() throws IOException {
                        return in;
                    }

                    public String getName() {
                        return ct2;
                    }

                    public OutputStream getOutputStream() throws IOException {
                        return null;
                    }

                });
            } else if (!ct.toLowerCase().contains("xml")) {
                //not XML based, need to stream out directly.  This is a bit tricky as
                //we don't want the stax stuff triggering and such
                OutputStream out = message.getContent(OutputStream.class);
                message.put(Message.CONTENT_TYPE, ct);
                try {
                    InputStream in = ds.getInputStream();
                    IOUtils.copy(in, out);
                    in.close();
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    throw new Fault(e);
                }
                list.remove(0);
                out = new CachedOutputStream();
                message.setContent(OutputStream.class, out);
                XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(out);
                message.setContent(XMLStreamWriter.class, writer);
            }
        } else if (ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, message)
            && Source.class.isAssignableFrom(type)) {
            //if schema validation is on, we'll end up converting to a DOMSource anyway,
            //let's convert and check for a fault
            MessageContentsList list = (MessageContentsList)message.getContent(List.class);
            Source ds = (Source)list.get(0);
            if (!(ds instanceof DOMSource)) {
                try {
                    ds = new DOMSource(StaxUtils.read(ds));
                } catch (XMLStreamException e) {
                    throw new Fault(e);
                }
                list.set(0,  ds);
                validatePossibleFault(message, bop, ((DOMSource)ds).getNode());
            }
        }

    }


    private void validatePossibleFault(Message message, BindingOperationInfo bop, Node ds) {
        Element el = DOMUtils.getFirstElement(ds);
        if (!"Fault".equals(el.getLocalName())) {
            return;
        }
        message.put(Message.RESPONSE_CODE, 500);

        el = DOMUtils.getFirstElement(el);
        while (el != null && !"detail".equals(el.getLocalName())) {
            el = DOMUtils.getNextElement(el);
        }
        if (el != null) {
            Schema schema = EndpointReferenceUtils.getSchema(message.getExchange().getService()
                                                             .getServiceInfos().get(0),
                                                         message.getExchange().getBus());
            try {
                validateFaultDetail(el, schema, bop);
            } catch (Exception e) {
                throw new Fault(e);
            }

            //We validated what we can from a fault standpoint
            message.put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.FALSE);
        }
    }
    private void validateFaultDetail(Element detail, Schema schema, BindingOperationInfo bop) throws Exception {
        if (detail != null) {
            Element el = DOMUtils.getFirstElement(detail);
            while (el != null) {
                QName qn = DOMUtils.getElementQName(el);
                for (BindingFaultInfo bfi : bop.getFaults()) {
                    if (bfi.getFaultInfo().getMessagePartByIndex(0).getConcreteName().equals(qn)) {
                        //Found a fault with the correct QName, we can validate it
                        schema.newValidator().validate(new DOMSource(DOMUtils.getDomElement(el)));
                    }
                }
                el = DOMUtils.getNextElement(el);
            }
        }
    }
    private void validateFault(SoapMessage message, SOAPFault fault, BindingOperationInfo bop) {
        if (ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, message)) {
            Schema schema = EndpointReferenceUtils.getSchema(message.getExchange().getService()
                                                             .getServiceInfos().get(0),
                                                         message.getExchange().getBus());
            Detail d = fault.getDetail();
            try {
                validateFaultDetail(d, schema, bop);
            } catch (Exception e) {
                throw new SoapFault(e.getMessage(), e, message.getVersion().getReceiver());
            }

            //We validated what we can from a fault standpoint
            message.put(Message.SCHEMA_VALIDATION_ENABLED, Boolean.FALSE);
        }
    }


    private void doSoap(Message message) {
        MessageContentsList list = (MessageContentsList)message.getContent(List.class);
        if (list == null || list.isEmpty()) {
            return;
        }
        Object o = list.get(0);
        if (o instanceof SOAPMessage) {
            SOAPMessage soapMessage = (SOAPMessage)o;

            if (soapMessage.countAttachments() > 0) {
                message.put("write.attachments", Boolean.TRUE);
            }
            try {
                if (message instanceof org.apache.cxf.binding.soap.SoapMessage) {
                    org.apache.cxf.binding.soap.SoapMessage cxfSoapMessage =
                            (org.apache.cxf.binding.soap.SoapMessage)message;
                    String cxfNamespace = cxfSoapMessage.getVersion().getNamespace();
                    SOAPHeader soapHeader = soapMessage.getSOAPHeader();
                    String namespace = soapHeader == null ? null : soapHeader.getNamespaceURI();
                    if (Soap12.SOAP_NAMESPACE.equals(namespace) && !namespace.equals(cxfNamespace)) {
                        cxfSoapMessage.setVersion(Soap12.getInstance());
                        cxfSoapMessage.put(Message.CONTENT_TYPE, cxfSoapMessage.getVersion().getContentType());
                    }
                }
            } catch (SOAPException e) {
                //ignore
            }
            try {
                Object enc = soapMessage.getProperty(SOAPMessage.CHARACTER_SET_ENCODING);
                if (enc instanceof String) {
                    message.put(Message.ENCODING, enc);
                }
            } catch (SOAPException e) {
                //ignore
            }
            try {
                Object xmlDec = soapMessage.getProperty(SOAPMessage.WRITE_XML_DECLARATION);
                if (xmlDec != null) {
                    boolean b = PropertyUtils.isTrue(xmlDec);
                    message.put(StaxOutInterceptor.FORCE_START_DOCUMENT, b);
                }
            } catch (SOAPException e) {
                //ignore
            }
        }
        message.getInterceptorChain().add(internal);
    }

    private class MessageModeOutInterceptorInternal extends AbstractSoapInterceptor {
        MessageModeOutInterceptorInternal() {
            super(Phase.PRE_PROTOCOL);
            addBefore(SAAJOutInterceptor.class.getName());
        }

        public void handleMessage(SoapMessage message) throws Fault {
            MessageContentsList list = (MessageContentsList)message.getContent(List.class);
            Object o = list.remove(0);
            SOAPMessage soapMessage = null;

            if (o instanceof SOAPMessage) {
                soapMessage = (SOAPMessage)o;
                if (soapMessage.countAttachments() > 0) {
                    message.put("write.attachments", Boolean.TRUE);
                }
            } else {
                try {
                    MessageFactory factory = saajOut.getFactory(message);
                    soapMessage = factory.createMessage();
                    SOAPPart part = soapMessage.getSOAPPart();
                    if (o instanceof Source) {
                        StaxUtils.copy((Source)o, new SAAJStreamWriter(part));
                    }
                } catch (SOAPException | XMLStreamException e) {
                    throw new SoapFault("Error creating SOAPMessage", e,
                                        message.getVersion().getSender());
                }
            }

            BindingOperationInfo bop = message.getExchange().getBindingOperationInfo();
            DocumentFragment frag = soapMessage.getSOAPPart().createDocumentFragment();
            try {
                Node body = SAAJUtils.getBody(soapMessage);
                Node nd = body.getFirstChild();
                while (nd != null) {
                    if (nd instanceof SOAPFault) {
                        message.put(Message.RESPONSE_CODE, 500);
                        validateFault(message, (SOAPFault)nd, bop);
                    }
                    body.removeChild(nd);
                    nd = DOMUtils.getDomElement(nd);
                    frag.appendChild(nd);
                    nd = SAAJUtils.getBody(soapMessage).getFirstChild();
                }

                message.setContent(SOAPMessage.class, soapMessage);

                if (!message.containsKey(SAAJOutInterceptor.ORIGINAL_XML_WRITER)) {
                    XMLStreamWriter origWriter = message.getContent(XMLStreamWriter.class);
                    message.put(SAAJOutInterceptor.ORIGINAL_XML_WRITER, origWriter);
                }
                W3CDOMStreamWriter writer = new SAAJStreamWriter(soapMessage.getSOAPPart());
                // Replace stax writer with DomStreamWriter
                message.setContent(XMLStreamWriter.class, writer);
                message.setContent(SOAPMessage.class, soapMessage);

                int index = 0;

                boolean client = isRequestor(message);
                BindingMessageInfo bmsg = null;

                if (client && bop != null) {
                    bmsg = bop.getInput();
                } else if (bop != null && bop.getOutput() != null) {
                    bmsg = bop.getOutput();
                }
                if (bmsg != null && bmsg.getMessageParts() != null
                    && bmsg.getMessageParts().size() > 0) {
                    index = bmsg.getMessageParts().get(0).getIndex();
                }

                list.set(index, frag);


                //No need to buffer this as we're already a DOM,
                //but only do so if someone hasn't actually configured this
                Object buffer = message
                    .getContextualProperty(AbstractOutDatabindingInterceptor.OUT_BUFFERING);
                if (buffer == null) {
                    message.put(AbstractOutDatabindingInterceptor.OUT_BUFFERING, Boolean.FALSE);
                }

            } catch (Exception ex) {
                throw new Fault(ex);
            }
            if (bop != null && bop.isUnwrapped()) {
                bop = bop.getWrappedOperation();
                message.getExchange().put(BindingOperationInfo.class, bop);
            }

            // Add a final interceptor to write the message
            message.getInterceptorChain().add(SAAJOutEndingInterceptor.INSTANCE);
        }
    }



}
