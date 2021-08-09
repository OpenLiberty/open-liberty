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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.Soap12FaultOutInterceptor.Soap12FaultOutInterceptorInternal;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

public class Soap11FaultOutInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(Soap11FaultOutInterceptor.class);

    public Soap11FaultOutInterceptor() {
        super(Phase.PREPARE_SEND);
    }
    public void handleMessage(SoapMessage message) throws Fault {
        Fault f = (Fault) message.getContent(Exception.class);
        message.put(org.apache.cxf.message.Message.RESPONSE_CODE, f.getStatusCode());
        if (message.getVersion() == Soap11.getInstance()) {
            message.getInterceptorChain().add(Soap11FaultOutInterceptorInternal.INSTANCE);
        } else {
            message.getInterceptorChain().add(Soap12FaultOutInterceptorInternal.INSTANCE);
        }
    }

    static class Soap11FaultOutInterceptorInternal extends AbstractSoapInterceptor {
        static final Soap11FaultOutInterceptorInternal INSTANCE = new Soap11FaultOutInterceptorInternal();

        Soap11FaultOutInterceptorInternal() {
            super(Phase.MARSHAL);
        }
        public void handleMessage(SoapMessage message) throws Fault {
            XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
            Fault f = (Fault) message.getContent(Exception.class);

            SoapFault fault = SoapFault.createFault(f, message.getVersion());

            try {
                Map<String, String> namespaces = fault.getNamespaces();
                for (Map.Entry<String, String> e : namespaces.entrySet()) {
                    writer.writeNamespace(e.getKey(), e.getValue());
                }

                String ns = message.getVersion().getNamespace();
                String defaultPrefix = writer.getPrefix(ns);
                if (defaultPrefix == null) {
                    defaultPrefix = StaxUtils.getUniquePrefix(writer, ns, false);
                    writer.writeStartElement(defaultPrefix, "Fault", ns);
                    writer.writeNamespace(defaultPrefix, ns);
                } else {
                    writer.writeStartElement(defaultPrefix, "Fault", ns);
                }

                writer.writeStartElement("faultcode");

                String codeString = fault.getCodeString(getFaultCodePrefix(writer, fault.getFaultCode()),
                        defaultPrefix);

                writer.writeCharacters(codeString);
                writer.writeEndElement();

                writer.writeStartElement("faultstring");
                String lang = f.getLang();
                if (!StringUtils.isEmpty(lang)) {
                    writer.writeAttribute("xml", "http://www.w3.org/XML/1998/namespace", "lang", lang);
                }
                writer.writeCharacters(getFaultMessage(message, fault));
                writer.writeEndElement();
                prepareStackTrace(message, fault);

                if (fault.getRole() != null) {
                    writer.writeStartElement("faultactor");
                    writer.writeCharacters(fault.getRole());
                    writer.writeEndElement();
                }

                if (fault.hasDetails()) {
                    Element detail = fault.getDetail();
                    writer.writeStartElement("detail");

                    Node node = detail.getFirstChild();
                    while (node != null) {
                        StaxUtils.writeNode(node, writer, true);
                        node = node.getNextSibling();
                    }

                    // Details
                    writer.writeEndElement();
                }

                // Fault
                writer.writeEndElement();
            } catch (Exception xe) {
                LOG.log(Level.WARNING, "XML_WRITE_EXC", xe);
                throw f;
            }
        }
    }
}
