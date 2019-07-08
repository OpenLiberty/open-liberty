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

import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.ClientFaultConverter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

public class Soap11FaultInInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(Soap11FaultInInterceptor.class);
    
    public Soap11FaultInInterceptor() {
        super(Phase.UNMARSHAL);
        addBefore(ClientFaultConverter.class.getName());
    }

    public void handleMessage(SoapMessage message) throws Fault {
        XMLStreamReader reader = message.getContent(XMLStreamReader.class);

        message.setContent(Exception.class, unmarshalFault(message, reader));
    }

    public static SoapFault unmarshalFault(SoapMessage message, 
                                           XMLStreamReader reader) {
        String exMessage = "";
        QName faultCode = null;
        String role = null;
        Element detail = null;
        
        try {
            while (reader.nextTag() == XMLStreamReader.START_ELEMENT) {
                if (reader.getLocalName().equals("faultcode")) {
                    faultCode = StaxUtils.readQName(reader);
                } else if (reader.getLocalName().equals("faultstring")) {
                    exMessage = reader.getElementText();
                } else if (reader.getLocalName().equals("faultactor")) {
                    role = reader.getElementText();
                } else if (reader.getLocalName().equals("detail")) {
                    //XMLStreamReader newReader = new DepthXMLStreamReader(reader);
                    detail = StaxUtils.read(reader).getDocumentElement();
                }
            }
        } catch (XMLStreamException e) {
            throw new SoapFault("Could not parse message.",
                                e,
                                message.getVersion().getSender());
        }
        // if the fault's content is invalid and fautlCode is not found, blame the receiver
        if (faultCode == null) {
            faultCode = Soap11.getInstance().getReceiver();
            exMessage = new Message("INVALID_FAULT", LOG).toString();
        }
        SoapFault fault = new SoapFault(exMessage, faultCode);
        fault.setDetail(detail);
        fault.setRole(role);
        return fault;
    }
}
