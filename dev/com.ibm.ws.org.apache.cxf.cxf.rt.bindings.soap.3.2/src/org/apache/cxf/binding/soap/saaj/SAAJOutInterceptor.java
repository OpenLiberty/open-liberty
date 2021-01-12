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
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;


import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;



/**
 * Sets up the outgoing chain to build a SAAJ tree instead of writing
 * directly to the output stream. First it will replace the XMLStreamWriter
 * with one which writes to a SOAPMessage. Then it will add an interceptor
 * at the end of the chain in the SEND phase which writes the resulting
 * SOAPMessage.
 */
@NoJSR250Annotations
public class SAAJOutInterceptor extends AbstractSoapInterceptor {
    public static final String ORIGINAL_XML_WRITER
        = SAAJOutInterceptor.class.getName() + ".original.xml.writer";

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(SAAJOutInterceptor.class);

    private MessageFactory factory11;
    private MessageFactory factory12;

    public SAAJOutInterceptor() {
        super(Phase.PRE_PROTOCOL);
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
    public void handleMessage(SoapMessage message) throws Fault {
        SOAPMessage saaj = message.getContent(SOAPMessage.class);

        try {
            if (message.hasHeaders()
                && saaj != null
                && saaj.getSOAPPart().getEnvelope().getHeader() == null) {

                // creating an empty SOAPHeader at this point in the
                // pre-existing SOAPMessage avoids the <soap:body> and
                // <soap:header> appearing in reverse order when the envolope
                // is written to the wire
                //
                saaj.getSOAPPart().getEnvelope().addHeader();
            }
        } catch (SOAPException e) {
            // Liberty change: e.getMessage() parameter is removed from new Message("SOAPEXCEPTION", BUNDLE, e.getMessage())
            throw new SoapFault(new Message("SOAPEXCEPTION", BUNDLE),
                                e,
                                message.getVersion().getSender());
        }

        if (saaj == null) {
            SoapVersion version = message.getVersion();
            try {
                MessageFactory factory = getFactory(message);
                SOAPMessage soapMessage = factory.createMessage();

                SOAPPart soapPart = soapMessage.getSOAPPart();
                XMLStreamWriter origWriter = (XMLStreamWriter)message.get(ORIGINAL_XML_WRITER);
                if (origWriter == null) {
                    origWriter = message.getContent(XMLStreamWriter.class);
                }
                message.put(ORIGINAL_XML_WRITER, origWriter);
                W3CDOMStreamWriter writer = new SAAJStreamWriter(soapPart);
                // Replace stax writer with DomStreamWriter
                message.setContent(XMLStreamWriter.class, writer);
                message.setContent(SOAPMessage.class, soapMessage);
                message.setContent(Node.class, soapMessage.getSOAPPart());


            } catch (SOAPException e) {
                // Liberty change: e.getMessage() parameter is removed from new Message("SOAPEXCEPTION", BUNDLE, e.getMessage())
                throw new SoapFault(new Message("SOAPEXCEPTION", BUNDLE), e, version.getSender());
            }
        } else if (!message.containsKey(ORIGINAL_XML_WRITER)) {
            //as the SOAPMessage already has everything in place, we do not need XMLStreamWriter to write
            //anything for us, so we just set XMLStreamWriter's output to a dummy output stream.
            XMLStreamWriter origWriter = message.getContent(XMLStreamWriter.class);
            message.put(ORIGINAL_XML_WRITER, origWriter);

            XMLStreamWriter dummyWriter = StaxUtils.createXMLStreamWriter(new OutputStream() {
                    public void write(int b) throws IOException {
                    }
                    public void write(byte[] b, int off, int len) throws IOException {
                    }
                });
            message.setContent(XMLStreamWriter.class, dummyWriter);
        }

        // Add a final interceptor to write the message
        message.getInterceptorChain().add(SAAJOutEndingInterceptor.INSTANCE);
    }
    @Override
    public void handleFault(SoapMessage message) {
        super.handleFault(message);
        //need to clear these so the fault writing will work correctly
        message.removeContent(SOAPMessage.class);
        message.remove(SoapOutInterceptor.WROTE_ENVELOPE_START);
        XMLStreamWriter writer = (XMLStreamWriter)message.get(ORIGINAL_XML_WRITER);
        if (writer != null) {
            message.setContent(XMLStreamWriter.class, writer);
            message.remove(ORIGINAL_XML_WRITER);
        }
    }


    public static class SAAJOutEndingInterceptor extends AbstractSoapInterceptor {
        public static final SAAJOutEndingInterceptor INSTANCE = new SAAJOutEndingInterceptor();

        public SAAJOutEndingInterceptor() {
            super(SAAJOutEndingInterceptor.class.getName(), Phase.PRE_PROTOCOL_ENDING);
        }

        public void handleMessage(SoapMessage message) throws Fault {
            SOAPMessage soapMessage = message.getContent(SOAPMessage.class);

            if (soapMessage != null) {
                if (soapMessage.countAttachments() > 0) {
                    if (message.getAttachments() == null) {
                        message.setAttachments(new ArrayList<Attachment>(soapMessage
                                .countAttachments()));
                    }
                    Iterator<AttachmentPart> it = CastUtils.cast(soapMessage.getAttachments());
                    while (it.hasNext()) {
                        AttachmentPart part = it.next();
                        String id = AttachmentUtil.cleanContentId(part.getContentId());
                        AttachmentImpl att = new AttachmentImpl(id);
                        try {
                            att.setDataHandler(part.getDataHandler());
                        } catch (SOAPException e) {
                            throw new Fault(e);
                        }
                        Iterator<MimeHeader> it2 = CastUtils.cast(part.getAllMimeHeaders());
                        while (it2.hasNext()) {
                            MimeHeader header = it2.next();
                            att.setHeader(header.getName(), header.getValue());
                        }
                        message.getAttachments().add(att);
                    }
                }

                XMLStreamWriter writer = (XMLStreamWriter)message.get(ORIGINAL_XML_WRITER);
                message.remove(ORIGINAL_XML_WRITER);

                try {
                    if (writer != null) {
                        StaxUtils.copy(new W3CDOMStreamReader(soapMessage.getSOAPPart()), writer);
                        writer.flush();
                        message.setContent(XMLStreamWriter.class, writer);
                    }
                } catch (XMLStreamException e) {
                    if (e.getCause() instanceof ConnectException) {
                        throw new SoapFault(e.getCause().getMessage(), e,
                                            message.getVersion().getSender());
                    }
                    // Liberty change: e.getMessage() parameter is removed from new Message("SOAPEXCEPTION", BUNDLE, e.getMessage())
                    throw new SoapFault(new Message("SOAPEXCEPTION", BUNDLE), e,
                                        message.getVersion().getSender());
                }
            }
        }

    }
}
