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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapBody;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.AbstractInDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.wsdl.interceptors.BareInInterceptor;

public class RPCInInterceptor extends AbstractInDatabindingInterceptor {
    private static final QName SOAP12_RESULT = new QName("http://www.w3.org/2003/05/soap-rpc",
                                                         "result");
    private static final Logger LOG = LogUtils.getL7dLogger(RPCInInterceptor.class);

    public RPCInInterceptor() {
        super(Phase.UNMARSHAL);
    }

    private BindingOperationInfo getOperation(Message message, QName opName) {
        BindingOperationInfo bop = ServiceModelUtil.getOperation(message.getExchange(), opName);
        if (bop == null) {
            Endpoint ep = message.getExchange().getEndpoint();
            if (ep == null) {
                return null;
            }
            BindingInfo service = ep.getEndpointInfo().getBinding();
            boolean output = !isRequestor(message);
            for (BindingOperationInfo info : service.getOperations()) {
                if (info.getName().getLocalPart().equals(opName.getLocalPart())) {
                    final SoapBody body;
                    if (output) {
                        body = info.getOutput().getExtensor(SoapBody.class);
                    } else {
                        body = info.getInput().getExtensor(SoapBody.class);
                    }
                    if (body != null
                        && opName.getNamespaceURI().equals(body.getNamespaceURI())) {
                        return info;
                    }
                }
            }
        }
        return bop;
    }
    public void handleMessage(Message message) {
        if (isGET(message)) {
            LOG.fine("RPCInInterceptor skipped in HTTP GET method");
            return;
        }
        DepthXMLStreamReader xmlReader = getXMLStreamReader(message);

        if (!StaxUtils.toNextElement(xmlReader)) {
            message.setContent(Exception.class, new RuntimeException("There must be a method name element."));
        }
        String opName = xmlReader.getLocalName();
        if (isRequestor(message) && opName.endsWith("Response")) {
            opName = opName.substring(0, opName.length() - 8);
        }

        final BindingOperationInfo operation;
        if (message.getExchange().getBindingOperationInfo() == null) {
            operation = getOperation(message, new QName(xmlReader.getNamespaceURI(), opName));
            if (operation == null) {
                // it's doc-lit-bare
                new BareInInterceptor().handleMessage(message);
                return;
            }
            setMessage(message, operation);
        } else {
            operation = message.getExchange().getBindingOperationInfo();
            if (!operation.getName().getLocalPart().equals(opName)) {
                String sa = (String)message.get(SoapBindingConstants.SOAP_ACTION);
                throw new Fault("SOAP_ACTION_MISMATCH_OP", LOG, null, sa, opName);
            }
        }
        MessageInfo msg;
        DataReader<XMLStreamReader> dr = getDataReader(message, XMLStreamReader.class);

        if (!isRequestor(message)) {
            msg = operation.getOperationInfo().getInput();
        } else {
            msg = operation.getOperationInfo().getOutput();
        }
        message.put(MessageInfo.class, msg);

        MessageContentsList parameters = new MessageContentsList();

        StaxUtils.nextEvent(xmlReader);

        boolean hasNext = true;
        Iterator<MessagePartInfo> itr = msg.getMessageParts().iterator();
        while (itr.hasNext()) {
            MessagePartInfo part = itr.next();
            if (hasNext) {
                hasNext = StaxUtils.toNextElement(xmlReader);
            }

            if (hasNext) {
                QName qn = xmlReader.getName();
                if (qn.equals(SOAP12_RESULT)) {
                    //just ignore this.   The parts should work correctly.
                    try {
                        while (xmlReader.getEventType() != XMLStreamConstants.END_ELEMENT) {
                            xmlReader.next();
                        }
                        xmlReader.next();
                    } catch (XMLStreamException e) {
                        //ignore
                    }
                    StaxUtils.toNextElement(xmlReader);
                    qn = xmlReader.getName();
                }


                // WSI-BP states that RPC/Lit part accessors should be completely unqualified
                // However, older toolkits (Axis 1.x) are qualifying them.   We'll go
                // ahead and just match on the localpart.   The RPCOutInterceptor
                // will always generate WSI-BP compliant messages so it's unknown if
                // the non-WSI-BP toolkits will be able to understand the CXF
                // generated messages if they are expecting it to be qualified.
                Iterator<MessagePartInfo> partItr = msg.getMessageParts().iterator();
                while (!qn.getLocalPart().equals(part.getConcreteName().getLocalPart())
                    && partItr.hasNext()) {
                    part = partItr.next();
                }

                // only check the localpart as explained above
                if (!qn.getLocalPart().equals(part.getConcreteName().getLocalPart())) {
                    throw new Fault(
                                    new org.apache.cxf.common.i18n.Message(
                                                                           "UNKNOWN_RPC_LIT_PART",
                                                                           LOG,
                                                                           qn));
                }
                try {
                    parameters.put(part, dr.read(part, xmlReader));
                } catch (Fault f) {
                    if (!isRequestor(message)) {
                        f.setFaultCode(Fault.FAULT_CODE_CLIENT);
                    }
                    throw f;
                }
            }
        }

        message.setContent(List.class, parameters);
    }



    private void setMessage(Message message,
                             BindingOperationInfo operation) {
        Exchange ex = message.getExchange();
        ex.put(BindingOperationInfo.class, operation);
        ex.setOneWay(operation.getOperationInfo().isOneWay());

        //Set standard MessageContext properties required by JAX_WS, but not specific to JAX_WS.
        message.put(Message.WSDL_OPERATION, operation.getName());

        ServiceInfo si = operation.getBinding().getService();
        QName serviceQName = si.getName();
        message.put(Message.WSDL_SERVICE, serviceQName);

        QName interfaceQName = si.getInterface().getName();
        message.put(Message.WSDL_INTERFACE, interfaceQName);

        EndpointInfo endpointInfo = ex.getEndpoint().getEndpointInfo();
        QName portQName = endpointInfo.getName();
        message.put(Message.WSDL_PORT, portQName);


        URI wsdlDescription = endpointInfo.getProperty("URI", URI.class);
        if (wsdlDescription == null) {
            String address = endpointInfo.getAddress();
            try {
                wsdlDescription = new URI(address + "?wsdl");
            } catch (URISyntaxException e) {
                //do nothing
            }
            endpointInfo.setProperty("URI", wsdlDescription);
        }
        message.put(Message.WSDL_DESCRIPTION, wsdlDescription);

        // configure endpoint and operation level schema validation
        setOperationSchemaValidation(message);
    }
}
