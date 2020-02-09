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

package org.apache.cxf.jaxws.handler.logical;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Binding;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.jaxws.handler.AbstractJAXWSHandlerInterceptor;
import org.apache.cxf.jaxws.handler.HandlerChainInvoker;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;

public class LogicalHandlerInInterceptor
    extends AbstractJAXWSHandlerInterceptor<Message> {

    public LogicalHandlerInInterceptor(Binding binding) {
        super(binding, Phase.PRE_PROTOCOL_FRONTEND);
        addAfter(SOAPHandlerInterceptor.class.getName());
    }

    @Override
    public void handleMessage(Message message) {
        if (binding.getHandlerChain().isEmpty()) {
            return;
        }
        HandlerChainInvoker invoker = getInvoker(message);
        if (invoker.getLogicalHandlers().isEmpty()) {
            return;
        }

        LogicalMessageContextImpl lctx = new LogicalMessageContextImpl(message);
        invoker.setLogicalMessageContext(lctx);
        boolean requestor = isRequestor(message);
        if (!requestor) {
            setupBindingOperationInfo(message.getExchange(), lctx);
        }

        if (!invoker.invokeLogicalHandlers(requestor, lctx)) {
            if (!requestor) {
                //server side
                handleAbort(message, null);
            } else {
                //Client side inbound, thus no response expected, do nothing, the close will
                //be handled by MEPComplete later
            }
        }

        //If this is the inbound and end of MEP, call MEP completion
        if (!isOutbound(message) && isMEPComlete(message)) {
            onCompletion(message);
        }
    }

    private void handleAbort(Message message, W3CDOMStreamWriter writer) {
        message.getInterceptorChain().abort();

        if (!message.getExchange().isOneWay()) {
            //server side inbound
            Endpoint e = message.getExchange().getEndpoint();
            Message responseMsg = new MessageImpl();
            responseMsg.setExchange(message.getExchange());
            responseMsg = e.getBinding().createMessage(responseMsg);

            message.getExchange().setOutMessage(responseMsg);
            XMLStreamReader reader = message.getContent(XMLStreamReader.class);
            if (reader == null && writer != null) {
                reader = StaxUtils.createXMLStreamReader(writer.getDocument());
            }

            InterceptorChain chain = OutgoingChainInterceptor
                .getOutInterceptorChain(message.getExchange());
            responseMsg.setInterceptorChain(chain);
            responseMsg.put("LogicalHandlerInterceptor.INREADER", reader);

            chain.doIntercept(responseMsg);
        }
    }

    protected QName getOpQName(Exchange ex, Object data) {
        LogicalMessageContextImpl sm = (LogicalMessageContextImpl)data;
        Source src = sm.getMessage().getPayload();
        if (src instanceof DOMSource) {
            DOMSource dsrc = (DOMSource)src;
            String ln = dsrc.getNode().getLocalName();
            String ns = dsrc.getNode().getNamespaceURI();
            return new QName(ns, ln);
        }
        return null;
    }

}
