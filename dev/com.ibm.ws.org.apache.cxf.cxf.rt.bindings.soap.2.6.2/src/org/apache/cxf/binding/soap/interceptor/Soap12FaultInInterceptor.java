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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.interceptor.ClientFaultConverter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.FragmentStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;

public class Soap12FaultInInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(Soap12FaultInInterceptor.class);
    
    public Soap12FaultInInterceptor() {
        super(Phase.UNMARSHAL);
        addBefore(ClientFaultConverter.class.getName());
    }

    public void handleMessage(SoapMessage message) throws Fault {
        if (message.getVersion() == Soap11.getInstance()) {
            new Soap11FaultInInterceptor().handleMessage(message);
            return;
        }
        XMLStreamReader reader = message.getContent(XMLStreamReader.class);
        message.setContent(Exception.class, unmarshalFault(message, reader));
    }

    public static SoapFault unmarshalFault(SoapMessage message, 
                                           XMLStreamReader reader) {
        String exMessage = null;
        QName faultCode = null;
        QName subCode = null;
        String role = null;
        String node = null;
        Element detail = null;

        Map<String, String> ns = new HashMap<String, String>();
        ns.put("s", Soap12.SOAP_NAMESPACE);
        XPathUtils xu = new XPathUtils(ns);        
        try {
            Node mainNode = message.getContent(Node.class);
            Node fault = null;
            
            if (reader instanceof W3CDOMStreamReader) {
                W3CDOMStreamReader dr = (W3CDOMStreamReader)reader;
                fault = dr.getCurrentElement();
                dr.consumeFrame();
            } else if (mainNode != null) {
                Node bodyNode = (Node) xu.getValue("//s:Body",
                                                   mainNode,
                                                   XPathConstants.NODE);

                StaxUtils.readDocElements(bodyNode.getOwnerDocument(),
                                          bodyNode,
                                          new FragmentStreamReader(reader),
                                          false, false);
                fault = (Element)xu.getValue("//s:Fault", bodyNode, XPathConstants.NODE);
            } else {
                fault = StaxUtils.read(new FragmentStreamReader(reader));
            }
            Element el = (Element)xu.getValue("//s:Fault/s:Code/s:Value", 
                                      fault, 
                                      XPathConstants.NODE);
            if (el != null) {
                faultCode = XMLUtils.getQName(el.getTextContent(), el);
            }
            
            el = (Element)xu.getValue("//s:Fault/s:Code/s:Subcode/s:Value", 
                                      fault, 
                                      XPathConstants.NODE);
            if (el != null) {
                subCode = XMLUtils.getQName(el.getTextContent(), el);
            }
            
            exMessage = (String) xu.getValue("//s:Fault/s:Reason/s:Text/text()", 
                                             fault,
                                             XPathConstants.STRING);
            
            Node detailNode = (Node) xu.getValue("//s:Fault/s:Detail",
                                                 fault,
                                                 XPathConstants.NODE);
            if (detailNode != null) {
                detail = (Element) detailNode;
            }
            
            role = (String) xu.getValue("//s:Fault/s:Role/text()", 
                                        fault,
                                        XPathConstants.STRING);

            node = (String) xu.getValue("//s:Fault/s:Node/text()", 
                                        fault,
                                        XPathConstants.STRING);                       
        } catch (XMLStreamException e) {
            throw new SoapFault("Could not parse message.",
                                e,
                                message.getVersion().getSender());
        }
        // if the fault's content is invalid and fautlCode is not found, blame the receiver
        if (faultCode == null) {
            faultCode = Soap12.getInstance().getReceiver();
            exMessage = new Message("INVALID_FAULT", LOG).toString();
        }
        
        SoapFault fault = new SoapFault(exMessage, faultCode);
        fault.setSubCode(subCode);
        fault.setDetail(detail);
        fault.setRole(role);
        fault.setNode(node);
        return fault;
    }

}
