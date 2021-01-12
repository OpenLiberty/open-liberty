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

package org.apache.cxf.binding.soap.interceptor;


import java.io.EOFException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.model.SoapHeaderInfo;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.headers.Header;
import org.apache.cxf.headers.HeaderManager;
import org.apache.cxf.headers.HeaderProcessor;
import org.apache.cxf.helpers.ServiceUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.WriteOnCloseOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;

public class SoapOutInterceptor extends AbstractSoapInterceptor {
    public static final String WROTE_ENVELOPE_START = "wrote.envelope.start";

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(SoapOutInterceptor.class);

    private Bus bus;

    public SoapOutInterceptor(Bus b) {
        super(Phase.WRITE);
        bus = b;
    }
    public SoapOutInterceptor(Bus b, String phase) {
        super(phase);
        bus = b;
    }

    public void handleMessage(SoapMessage message) {
        // Yes this is ugly, but it avoids us from having to implement any kind of caching strategy
        boolean wroteStart = PropertyUtils.isTrue(message.get(WROTE_ENVELOPE_START));
        if (!wroteStart) {
            writeSoapEnvelopeStart(message);

            OutputStream os = message.getContent(OutputStream.class);
            // Unless we're caching the whole message in memory skip the envelope writing
            // if there's a fault later.
            if (!(os instanceof WriteOnCloseOutputStream) && !MessageUtils.isDOMPresent(message)) {
                message.put(WROTE_ENVELOPE_START, Boolean.TRUE);
            }
        }

        String cte = (String)message.get("soap.attachement.content.transfer.encoding");
        if (cte != null) {
            message.put(Message.CONTENT_TRANSFER_ENCODING, cte);
        }
        // Add a final interceptor to write end elements
        message.getInterceptorChain().add(new SoapOutEndingInterceptor());
    }

    private void writeSoapEnvelopeStart(final SoapMessage message) {
        final SoapVersion soapVersion = message.getVersion();
        try {
            XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
            String soapPrefix = xtw.getPrefix(soapVersion.getNamespace());
            if (StringUtils.isEmpty(soapPrefix)) {
                soapPrefix = "soap";
            }
            if (message.hasAdditionalEnvNs()) {
                Map<String, String> nsMap = message.getEnvelopeNs();
                for (Map.Entry<String, String> entry : nsMap.entrySet()) {
                    if (soapVersion.getNamespace().equals(entry.getValue())) {
                        soapPrefix = entry.getKey();
                    }
                }
                xtw.setPrefix(soapPrefix, soapVersion.getNamespace());
                xtw.writeStartElement(soapPrefix,
                                      soapVersion.getEnvelope().getLocalPart(),
                                      soapVersion.getNamespace());
                xtw.writeNamespace(soapPrefix, soapVersion.getNamespace());
                for (Map.Entry<String, String> entry : nsMap.entrySet()) {
                    if (!soapVersion.getNamespace().equals(entry.getValue())) {
                        xtw.writeNamespace(entry.getKey(), entry.getValue());
                    }
                }
            } else {
                xtw.setPrefix(soapPrefix, soapVersion.getNamespace());
                xtw.writeStartElement(soapPrefix,
                                      soapVersion.getEnvelope().getLocalPart(),
                                      soapVersion.getNamespace());
                String s2 = xtw.getPrefix(soapVersion.getNamespace());
                if (StringUtils.isEmpty(s2) || soapPrefix.equals(s2)) {
                    xtw.writeNamespace(soapPrefix, soapVersion.getNamespace());
                } else {
                    soapPrefix = s2;
                }
            }
            boolean preexistingHeaders = message.hasHeaders();

            if (preexistingHeaders) {
                xtw.writeStartElement(soapPrefix,
                                      soapVersion.getHeader().getLocalPart(),
                                      soapVersion.getNamespace());
                List<Header> hdrList = message.getHeaders();
                for (Header header : hdrList) {
                    XMLStreamWriter writer = xtw;
/*                  Liberty change: if block below is removed
                    if (xtw instanceof W3CDOMStreamWriter) {
                        Element nd = ((W3CDOMStreamWriter)xtw).getCurrentNode();
                        if (header.getObject() instanceof Element
                            && nd.isSameNode(((Element)header.getObject()).getParentNode())) {
                            continue;
                        }
                    } Liberty change: end */
                    if (header instanceof SoapHeader) {
                        SoapHeader soapHeader = (SoapHeader)header;
                        writer = new SOAPHeaderWriter(xtw, soapHeader, soapVersion, soapPrefix);
                    }
                    DataBinding b = header.getDataBinding();
                    if (b == null) {
                        HeaderProcessor hp = bus.getExtension(HeaderManager.class)
                                .getHeaderProcessor(header.getName().getNamespaceURI());
                        if (hp != null) {
                            b = hp.getDataBinding();
                        }
                    }
                    if (b != null) {
                        MessagePartInfo part = new MessagePartInfo(header.getName(), null);
                        part.setConcreteName(header.getName());
                        b.createWriter(XMLStreamWriter.class)
                            .write(header.getObject(), part, writer);
                    } else {
                        Element node = (Element)header.getObject();
                        StaxUtils.copy(node, writer);
                    }
                }
            }
            boolean endedHeader = handleHeaderPart(preexistingHeaders, message, soapPrefix);
            if (preexistingHeaders && !endedHeader) {
                xtw.writeEndElement();
            }

            xtw.writeStartElement(soapPrefix,
                                  soapVersion.getBody().getLocalPart(),
                                  soapVersion.getNamespace());

            // Interceptors followed such as Wrapped/RPC/Doc Interceptor will write SOAP body
        } catch (XMLStreamException e) {
            throw new SoapFault(
                new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), e, soapVersion.getSender());
        }
    }

    private boolean handleHeaderPart(boolean preexistingHeaders, SoapMessage message, String soapPrefix) {
        //add MessagePart to soapHeader if necessary
        boolean endedHeader = false;
        Exchange exchange = message.getExchange();
        BindingOperationInfo bop = exchange.getBindingOperationInfo();
        if (bop == null) {
            return endedHeader;
        }

        XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
        boolean startedHeader = false;
        BindingOperationInfo unwrappedOp = bop;
        if (bop.isUnwrapped()) {
            unwrappedOp = bop.getWrappedOperation();
        }
        boolean client = isRequestor(message);
        BindingMessageInfo bmi = client ? unwrappedOp.getInput() : unwrappedOp.getOutput();
        BindingMessageInfo wrappedBmi = client ? bop.getInput() : bop.getOutput();

        if (bmi == null) {
            return endedHeader;
        }

        if (wrappedBmi.getMessageInfo().getMessagePartsNumber() > 0) {
            MessageContentsList objs = MessageContentsList.getContentsList(message);
            if (objs == null) {
                return endedHeader;
            }
            SoapVersion soapVersion = message.getVersion();
            List<SoapHeaderInfo> headers = bmi.getExtensors(SoapHeaderInfo.class);
            if (headers == null) {
                return endedHeader;
            }


            for (SoapHeaderInfo header : headers) {
                MessagePartInfo part = header.getPart();
                if (wrappedBmi != bmi) {
                    part = wrappedBmi.getMessageInfo().addMessagePart(part.getName());
                }
                if (part.getIndex() >= objs.size()) {
                    // The optional out of band header is not a part of parameters of the method
                    continue;
                }
                Object arg = objs.get(part);
                if (arg == null) {
                    continue;
                }
                objs.remove(part);
                if (!(startedHeader || preexistingHeaders)) {
                    try {
                        xtw.writeStartElement(soapPrefix,
                                              soapVersion.getHeader().getLocalPart(),
                                              soapVersion.getNamespace());
                    } catch (XMLStreamException e) {
                        throw new SoapFault(new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE),
                            e, soapVersion.getSender());
                    }
                    startedHeader = true;
                }
                DataWriter<XMLStreamWriter> dataWriter = getDataWriter(message);
                dataWriter.write(arg, header.getPart(), xtw);
            }

            if (startedHeader || preexistingHeaders) {
                try {
                    xtw.writeEndElement();
                    endedHeader = true;
                } catch (XMLStreamException e) {
                    throw new SoapFault(new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE),
                        e, soapVersion.getSender());
                }
            }
        }
        return endedHeader;
    }

    protected DataWriter<XMLStreamWriter> getDataWriter(Message message) {
        Service service = ServiceModelUtil.getService(message.getExchange());
        DataWriter<XMLStreamWriter> dataWriter = service.getDataBinding().createWriter(XMLStreamWriter.class);
        if (dataWriter == null) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("NO_DATAWRITER", BUNDLE, service
                .getName()));
        }
        dataWriter.setAttachments(message.getAttachments());
        setDataWriterValidation(service, message, dataWriter);
        return dataWriter;
    }
    private void setDataWriterValidation(Service service, Message message, DataWriter<?> writer) {
        if (ServiceUtils.isSchemaValidationEnabled(SchemaValidationType.OUT, message)) {
            Schema schema = EndpointReferenceUtils.getSchema(service.getServiceInfos().get(0),
                                                             message.getExchange().getBus());
            writer.setSchema(schema);
        }
    }

    public class SoapOutEndingInterceptor extends AbstractSoapInterceptor {
        public SoapOutEndingInterceptor() {
            super(SoapOutEndingInterceptor.class.getName(), Phase.WRITE_ENDING);
        }

        public void handleMessage(SoapMessage message) throws Fault {
            try {
                XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
                if (xtw != null) {
                    // Write body end
                    xtw.writeEndElement();
                    // Write Envelope end element
                    xtw.writeEndElement();
                    xtw.writeEndDocument();

                    xtw.flush();
                }
            } catch (XMLStreamException e) {
                if (e.getCause() instanceof EOFException) {
                    //Nothing we can do about this, some clients will close the connection early if
                    //they fully parse everything they need
                } else {
                    SoapVersion soapVersion = message.getVersion();
                    throw new SoapFault(new org.apache.cxf.common.i18n.Message("XML_WRITE_EXC", BUNDLE), e,
                                        soapVersion.getSender());
                }
            }
        }
    }

    public static class SOAPHeaderWriter extends DelegatingXMLStreamWriter {
        final SoapHeader soapHeader;
        final SoapVersion soapVersion;
        final String soapPrefix;
        boolean firstDone;


        public SOAPHeaderWriter(XMLStreamWriter writer,
                                SoapHeader header,
                                SoapVersion version,
                                String pfx) {
            super(writer);
            soapHeader = header;
            soapVersion = version;
            soapPrefix = pfx;
        }

        public void writeAttribute(String prefix, String uri, String local, String value)
            throws XMLStreamException {
            if (soapVersion.getNamespace().equals(uri)
                && (local.equals(soapVersion.getAttrNameMustUnderstand())
                    || local.equals(soapVersion.getAttrNameRole()))) {
                return;
            }
            super.writeAttribute(prefix, uri, local, value);
        }
        public void writeAttribute(String uri, String local, String value) throws XMLStreamException {
            if (soapVersion.getNamespace().equals(uri)
                && (local.equals(soapVersion.getAttrNameMustUnderstand())
                    || local.equals(soapVersion.getAttrNameRole()))) {
                return;
            }
            super.writeAttribute(uri, local, value);
        }

        private void writeSoapAttributes() throws XMLStreamException {
            if (!firstDone) {
                firstDone = true;
                if (!StringUtils.isEmpty(soapHeader.getActor())) {
                    super.writeAttribute(soapPrefix,
                                   soapVersion.getNamespace(),
                                   soapVersion.getAttrNameRole(),
                                   soapHeader.getActor());
                }
                boolean mu = soapHeader.isMustUnderstand();
                if (mu) {
                    String mul = soapVersion.getAttrValueMustUnderstand(mu);
                    super.writeAttribute(soapPrefix,
                                   soapVersion.getNamespace(),
                                   soapVersion.getAttrNameMustUnderstand(),
                                   mul);
                }
            }
        }
        public void writeStartElement(String arg0, String arg1, String arg2)
            throws XMLStreamException {
            super.writeStartElement(arg0, arg1, arg2);
            writeSoapAttributes();
        }
        public void writeStartElement(String arg0, String arg1)
            throws XMLStreamException {
            super.writeStartElement(arg0, arg1);
            writeSoapAttributes();
        }
        public void writeStartElement(String arg0) throws XMLStreamException {
            super.writeStartElement(arg0);
            writeSoapAttributes();
        }


    };
}
