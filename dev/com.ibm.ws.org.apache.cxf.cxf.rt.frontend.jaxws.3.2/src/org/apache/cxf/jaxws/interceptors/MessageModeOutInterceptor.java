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
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.ibm.websphere.ras.annotation.Sensitive;

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
import org.apache.cxf.common.logging.LogUtils;
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

// Liberty Change - This class has no Liberty specific changes other than the Sensitive annotation 
// It is required as an overlay because of Liberty specific changes to MessageImpl.put(). Any call
// to SoapMessage.put() will cause a NoSuchMethodException in the calling class if the class is not recompiled.
// If a solution to this compilation issue can be found, this class should be removed as an overlay. 
public class MessageModeOutInterceptor extends AbstractPhaseInterceptor<Message> {
    MessageModeOutInterceptorInternal internal;
    SAAJOutInterceptor saajOut;
    Class<?> type;
    QName bindingName;

    private static final Logger LOG = LogUtils.getLogger(MessageModeOutInterceptor.class); // Liberty Change #26529
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
    public void handleMessage(@Sensitive Message message) throws Fault { // Liberty Change
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);  // Liberty Change #26529
        BindingOperationInfo bop = message.getExchange().getBindingOperationInfo();
        if (bop != null && !bindingName.equals(bop.getBinding().getName())) {
            if(isFinestEnabled)  {   // Liberty Change begin #26529
                LOG.finest("BindingOperationInfo is null or binding qname is different than the one provided in constructor. Returning.");
            } // Liberty Change end #26529
            return;
        }
        if (saajOut != null) {
            doSoap(message);
        } else if (DataSource.class.isAssignableFrom(type)) {
            //datasource stuff, must check if multi-source
            MessageContentsList list = (MessageContentsList)message.getContent(List.class);
            DataSource ds = (DataSource)list.get(0);
            if(isFinestEnabled)  {   // Liberty Change begin #26529
                LOG.finest("First element of messageContentsList that is obtained from message that will be cast to DataSource: " + list.get(0));
            }  // Liberty Change end #26529
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
                    if(isFinestEnabled)  {  // Liberty Change begin #26529
                        LOG.finest("The InputStream that is obtained from DataSource is copied over to OutputStream that is obtained from message content."); 
                    } // Liberty Change end #26529
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
            if(isFinestEnabled)  {   // Liberty Change begin #26529
                LOG.finest("Source that is obtained from message content: " + ds);
            } // Liberty Change end #26529
            if (!(ds instanceof DOMSource)) {
                try {
                    ds = new DOMSource(StaxUtils.read(ds));
                    if(isFinestEnabled)  {   // Liberty Change begin #26529
                        LOG.finest("Source is an instance of DOMSource. A new DOMSource will be constructed with it: " + ds);
                    } // Liberty Change end #26529
                } catch (XMLStreamException e) {
                    throw new Fault(e);
                }
                list.set(0,  ds);
                if(isFinestEnabled)  {   // Liberty Change begin #26529
                    LOG.finest("MessageContentsList(list): " + list);
                } // Liberty Change end #26529
                validatePossibleFault(message, bop, ((DOMSource)ds).getNode());
            }
        }
    }


    private void validatePossibleFault(@Sensitive Message message, BindingOperationInfo bop, Node ds) { // Liberty Change Start
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);  // Liberty Change #26529
        Element el = DOMUtils.getFirstElement(ds);
        if (!"Fault".equals(el.getLocalName())) {
            if(isFinestEnabled)  {  // Liberty Change begin #26529
                LOG.finest("Node already contains a fault. No extra validation required. Returning. " + ds); 
            } // Liberty Change end #26529
            return;
        }
        message.put(Message.RESPONSE_CODE, 500);

        el = DOMUtils.getFirstElement(el);
        while (el != null && !"detail".equals(el.getLocalName())) {
            el = DOMUtils.getNextElement(el);
        }
        if(isFinestEnabled)  {   // Liberty Change begin #26529
            LOG.finest("Detail sub section of the fault" + el);
        } // Liberty Change end #26529
        if (el != null) {
            Schema schema = EndpointReferenceUtils.getSchema(message.getExchange().getService()
                                                             .getServiceInfos().get(0),
                                                         message.getExchange().getBus());
            if(isFinestEnabled)  {   // Liberty Change begin #26529
                LOG.finest("Schema that is obtained from EndpointReferenceUtils" + schema);
            } // Liberty Change end #26529
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
    private void validateFault(@Sensitive SoapMessage message, SOAPFault fault, BindingOperationInfo bop) { // Liberty Change
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


    private void doSoap(@Sensitive Message message) { // Liberty Change
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);  // Liberty Change  #26529
        
        MessageContentsList list = (MessageContentsList)message.getContent(List.class);
        if (list == null || list.isEmpty()) {
            if(isFinestEnabled)  {   // Liberty Change begin #26529
                LOG.finest("MessageContentsList is null or empty. Returning.");
            } // Liberty Change end #26529
            return;
        }
        Object o = list.get(0);
        if(isFinestEnabled)  {   // Liberty Change begin #26529
            LOG.finest("First element of MessageContentsList that is obtained from message: " + o);
        } // Liberty Change end #26529
        if (o instanceof SOAPMessage) {
            SOAPMessage soapMessage = (SOAPMessage)o;

            if (soapMessage.countAttachments() > 0) {
                message.put("write.attachments", Boolean.TRUE);
                if(isFinestEnabled)  {   // Liberty Change begin #26529
                    LOG.finest("write.attachments is set t true in message.");
                } // Liberty Change end #26529
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
                        if(isFinestEnabled)  {   // Liberty Change begin #26529
                            LOG.finest("Message version is SOAP 1.2 " + cxfSoapMessage.getVersion());
                            LOG.finest("Message content type is set to cxfSoapMessage version's content type: " + cxfSoapMessage.get(Message.CONTENT_TYPE));
                        } // Liberty Change end #26529
                    }
                }
            } catch (SOAPException e) {
                //ignore
                if(isFinestEnabled)  {   // Liberty Change begin #26529
                    LOG.finest("Ignored SOAPException(message): " + e.getMessage());
                } // Liberty Change end #26529
            }
            try {
                Object enc = soapMessage.getProperty(SOAPMessage.CHARACTER_SET_ENCODING);
                if (enc instanceof String) {
                    message.put(Message.ENCODING, enc);
                    if(isFinestEnabled)  {   // Liberty Change begin #26529
                        LOG.finest("Message encoding is set to : " + enc);
                    } // Liberty Change end #26529
                }
            } catch (SOAPException e) {
                //ignore
                if(isFinestEnabled)  {   // Liberty Change begin #26529
                    LOG.finest("Ignored SOAPException(message): " + e.getMessage());
                } // Liberty Change end #26529
            }
            try {
                Object xmlDec = soapMessage.getProperty(SOAPMessage.WRITE_XML_DECLARATION);
                if (xmlDec != null) {
                    boolean b = PropertyUtils.isTrue(xmlDec);
                    message.put(StaxOutInterceptor.FORCE_START_DOCUMENT, b);
                }
            } catch (SOAPException e) {
                //ignore
                if(isFinestEnabled)  {   // Liberty Change begin #26529
                    LOG.finest("Ignored SOAPException(message): " + e.getMessage());
                } // Liberty Change end #26529
            }
        }
        message.getInterceptorChain().add(internal);
    }

    private class MessageModeOutInterceptorInternal extends AbstractSoapInterceptor {
        MessageModeOutInterceptorInternal() {
            super(Phase.PRE_PROTOCOL);
            addBefore(SAAJOutInterceptor.class.getName());
        }

        public void handleMessage(@Sensitive SoapMessage message) throws Fault { // Liberty Change
            
            boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);  // Liberty Change #26529
            
            MessageContentsList list = (MessageContentsList)message.getContent(List.class);
            Object o = list.remove(0);
            SOAPMessage soapMessage = null; // Liberty Change

            if (o instanceof SOAPMessage) {
                soapMessage = (SOAPMessage)o;
                if (soapMessage.countAttachments() > 0) {
                    message.put("write.attachments", Boolean.TRUE);
                    if(isFinestEnabled)  {   // Liberty Change begin #26529
                        LOG.finest("write.attachments is set to true in message.");
                    } // Liberty Change end #26529
                }
            } else {
                try {
                    MessageFactory factory = saajOut.getFactory(message);
                    soapMessage = factory.createMessage();
                    SOAPPart part = soapMessage.getSOAPPart();
                    if (o instanceof Source) {
                        StaxUtils.copy((Source)o, new SAAJStreamWriter(part));
                        if(isFinestEnabled)  {   // Liberty Change begin #26529
                            LOG.finest("Source that is obtained from message content is copied over SAAJStreamWriter instance with the parameter SOAPPart: " + o);
                        } // Liberty Change end #26529
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
                if(isFinestEnabled)  {   // Liberty Change begin #26529
                    LOG.finest("Nodes are removed from body and added to newly instantiated DocumentFragment: " + frag);
                } // Liberty Change end #26529

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
                if(isFinestEnabled)  {   // Liberty Change begin #26529
                    LOG.finest("DocumentFragment is set in MessageContentsList: " + frag + ",  with index: " + index);
                } // Liberty Change end #26529


                //No need to buffer this as we're already a DOM,
                //but only do so if someone hasn't actually configured this
                Object buffer = message
                    .getContextualProperty(AbstractOutDatabindingInterceptor.OUT_BUFFERING);
                if (buffer == null) {
                    message.put(AbstractOutDatabindingInterceptor.OUT_BUFFERING, Boolean.FALSE);
                    if(isFinestEnabled)  {   // Liberty Change begin #26529
                        LOG.finest("org.apache.cxf.output.buffering is set to false.");
                    } // Liberty Change end #26529
                }

            } catch (Exception ex) {
                throw new Fault(ex);
            }
            if (bop != null && bop.isUnwrapped()) {
                bop = bop.getWrappedOperation();
                message.getExchange().put(BindingOperationInfo.class, bop);
                if(isFinestEnabled)  {   // Liberty Change begin #26529
                    LOG.finest("BindingOperationInfo is switched to wrapped version in exchange.");
                } // Liberty Change end #26529
            }

            // Add a final interceptor to write the message
            message.getInterceptorChain().add(SAAJOutEndingInterceptor.INSTANCE);
        }
    }



}
