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
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import org.apache.cxf.binding.soap.wsdl.extensions.SoapBody;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.NSStack;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.CachingXmlEventWriter;
import org.apache.cxf.staxutils.StaxUtils;

public class RPCOutInterceptor extends AbstractOutDatabindingInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(RPCOutInterceptor.class);

    public RPCOutInterceptor() {
        super(Phase.MARSHAL);
    }


    public void handleMessage(Message message) {
        XMLStreamWriter origXmlWriter = null;
        try {
            NSStack nsStack = new NSStack();
            nsStack.push();

            BindingOperationInfo operation = message.getExchange().getBindingOperationInfo();

            assert operation.getName() != null;

            XMLStreamWriter xmlWriter = getXMLStreamWriter(message);
            CachingXmlEventWriter cache = null;
            // need to cache the events in case validation fails or buffering is enabled
            if (shouldBuffer(message)) {
                origXmlWriter = xmlWriter;
                cache = new CachingXmlEventWriter();
                try {
                    cache.setNamespaceContext(xmlWriter.getNamespaceContext());
                } catch (XMLStreamException e) {
                    //ignorable, will just get extra namespace decls
                }
                message.setContent(XMLStreamWriter.class, cache);
                xmlWriter = cache;
            }

            List<MessagePartInfo> parts = null;

            boolean output = false;
            if (!isRequestor(message)) {
/*              Liberty change: if block is removed
                if (operation.getOutput() == null) {
                    return;
                } */
                parts = operation.getOutput().getMessageParts();
                output = true;
            } else {
                parts = operation.getInput().getMessageParts();
                output = false;
            }

            MessageContentsList objs = MessageContentsList.getContentsList(message);
            if (objs == null) {
                addOperationNode(nsStack, message, xmlWriter, output, operation);
                xmlWriter.writeEndElement();
                return;
            }

            for (MessagePartInfo part : parts) {
                if (objs.hasValue(part)) {
                    Object o = objs.get(part);
                    if (o == null) {
                        //WSI-BP R2211 - RPC/Lit parts are not allowed to be xsi:nil
                        throw new Fault(
                            new org.apache.cxf.common.i18n.Message("BP_2211_RPCLIT_CANNOT_BE_NULL",
                                                                   LOG, part.getConcreteName()));
                    }
                   //WSI-BP R2737  -RPC/LIG part name space is empty
                   // part.setConcreteName(new QName("", part.getConcreteName().getLocalPart()));
                }
            }

            addOperationNode(nsStack, message, xmlWriter, output, operation);
            writeParts(message, message.getExchange(), operation, objs, parts);

            // Finishing the writing.
            xmlWriter.writeEndElement();


            if (cache != null) {
                try {
                    for (XMLEvent event : cache.getEvents()) {
                        StaxUtils.writeEvent(event, origXmlWriter);
                    }
                } catch (XMLStreamException e) {
                    throw new Fault(e);
                }
            }
        } catch (XMLStreamException e) {
            throw new Fault(e);
        } finally {
            if (origXmlWriter != null) {
                message.setContent(XMLStreamWriter.class, origXmlWriter);
            }
        }
    }

    protected String addOperationNode(NSStack nsStack, Message message,
                                      XMLStreamWriter xmlWriter,
                                      boolean output,
                                      BindingOperationInfo boi)
        throws XMLStreamException {
        String ns = boi.getName().getNamespaceURI();
        SoapBody body = null;
        if (output) {
            body = boi.getOutput().getExtensor(SoapBody.class);
        } else {
            body = boi.getInput().getExtensor(SoapBody.class);
        }
        if (body != null) {
            final String nsUri = body.getNamespaceURI(); //do it once, as it might internally use reflection...
            if (!StringUtils.isEmpty(nsUri)) {
                ns = nsUri;
            }
        }

        nsStack.add(ns);
        String prefix = nsStack.getPrefix(ns);
        String name = output ? boi.getName().getLocalPart() + "Response" : boi.getName().getLocalPart();
        StaxUtils.writeStartElement(xmlWriter, prefix, name, ns);
        return ns;
    }

    protected XMLStreamWriter getXMLStreamWriter(Message message) {
        return message.getContent(XMLStreamWriter.class);
    }
}
