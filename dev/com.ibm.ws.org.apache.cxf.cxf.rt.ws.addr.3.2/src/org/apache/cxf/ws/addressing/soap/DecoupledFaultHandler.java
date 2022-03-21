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

package org.apache.cxf.ws.addressing.soap;

import java.util.Iterator;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.Names;

/**
 * Utility interceptor for dealing with faults occurred during processing
 * the one way requests with WSA FaultTo EPR pointing to a decoupled destination.
 *
 * Note that this interceptor is not currently installed by default.
 * It can be installed using @InInterceptors and @OutInterceptors
 * annotations or explicitly added to the list of interceptors.
 */
public class DecoupledFaultHandler extends AbstractSoapInterceptor {

    public static final String WSA_ACTION = "http://schemas.xmlsoap.org/wsdl/soap/envelope/fault";

    public DecoupledFaultHandler() {
        super(Phase.PRE_PROTOCOL);
        addBefore(MAPCodec.class.getName());
    }

    public void handleMessage(SoapMessage message) {
        // complete
    }

    // Ideally, this code will instead be executed as part of the Fault chain
    // but at the moment PhaseInterceptorChain needs to be tricked that this is
    // a two way request for a fault chain be invoked
    public void handleFault(SoapMessage message) {
        if (!ContextUtils.isRequestor(message)) {

            Exchange exchange = message.getExchange();
            Message inMessage = exchange.getInMessage();
            final AddressingProperties maps =
                ContextUtils.retrieveMAPs(inMessage, false, false, true);

            if (maps != null && !ContextUtils.isGenericAddress(maps.getFaultTo())) {
                //Just keep the wsa headers to remove the not understand headers
                if (exchange.getOutMessage() != null) {
                    message = (SoapMessage)exchange.getOutMessage();
                }
                Iterator<Header> iterator = message.getHeaders().iterator();
                while (iterator.hasNext()) {
                    Header header = iterator.next();
                    if (!isWSAHeader(header)) {
                        iterator.remove();
                    }
                }
                exchange.setOneWay(false);
                exchange.setOutMessage(message);
                //manually set the action
                message.put(ContextUtils.ACTION, WSA_ACTION);
                Destination destination = createDecoupledDestination(
                                               exchange, maps.getFaultTo());
                exchange.setDestination(destination);
            }

        }
    }

    protected Destination createDecoupledDestination(Exchange exchange, EndpointReferenceType epr) {
        return ContextUtils.createDecoupledDestination(exchange, epr);
    }

    private boolean isWSAHeader(Header header) {
        return header.getName().getNamespaceURI().startsWith(Names.WSA_NAMESPACE_NAME);
    }
}
