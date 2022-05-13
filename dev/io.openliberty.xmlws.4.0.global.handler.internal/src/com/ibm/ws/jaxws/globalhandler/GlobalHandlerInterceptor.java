/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxws.globalhandler;

import java.util.List;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.handler.logical.LogicalMessageContextImpl;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.staxutils.StaxUtils;

import com.ibm.webservices.handler.impl.GlobalHandlerService;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.bus.LibertyApplicationBus;
import com.ibm.ws.jaxws.bus.LibertyApplicationBus.Type;
import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

public class GlobalHandlerInterceptor
                extends AbstractJAXWSGlobalHandlerInterceptor<Message> {

    private final String flow;
    private final LibertyApplicationBus.Type busType;

    private static final TraceComponent tc = Tr.register(GlobalHandlerInterceptor.class);

    private List<Handler> handlersList;

    public GlobalHandlerInterceptor(String phase, String flowType, LibertyApplicationBus.Type bus) {
        super(phase);

        flow = flowType.toLowerCase();
        busType = bus;
        // when in inbound chain, call global handers BEFORE JAXWS SPEC SOAPHandlers and LOGICALHandlers
        if (flowType.equalsIgnoreCase("in")) {
            addBefore(SOAPHandlerInterceptor.class.getName());
        }
        // when in outbound chain, call global handers AFTER JAXWS SPEC SOAPHandlers and LOGICALHandlers
        if (flowType.equalsIgnoreCase("out")) {
            addAfter(SOAPHandlerInterceptor.class.getName() + ".ENDING");
        }

    }

    @Override
    public void handleMessage(Message message) {

        //Callback to check whether there are new added or removed handlers.
        handlersList = getHanderList(busType, flow);
        //call handers one by one if handlerList is not empty
        if (this.handlersList.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "no registered global handlers");
            }
            return;
        }

        //in Server side IN flow, if we can get WSDL_OPERATION then set it before hand       
        LogicalMessageContextImpl lctx = new LogicalMessageContextImpl(message);
        boolean requestor = isRequestor(message);
        if (!requestor) {
            setupBindingOperationInfo(message.getExchange(), lctx);
        }
        Exchange ex = message.getExchange();
        BindingOperationInfo bop = ex.getBindingOperationInfo();
        if (message == ex.getInMessage() && bop != null && ex.getBindingOperationInfo() == null) {
            ex.put(Message.WSDL_OPERATION, bop.getName());
        }

        GlobalHandlerChainInvoker invoker = new GlobalHandlerChainInvoker();

        GlobalHandlerJaxWsMessageContext ctx = new GlobalHandlerJaxWsMessageContext(message);
        invoker.invokeGlobalHandlers(ctx, handlersList);

        //when in In Flow , call SAAJInInterceptor to replace headers first.
        if (!MessageUtils.isOutbound(message)) {
            //inFlowInterceptor.handleMessage((SoapMessage) message);

            SOAPMessage msg = message.getContent(SOAPMessage.class);
            if (msg != null) {
                XMLStreamReader xmlReader = createXMLStreamReaderFromSOAPMessage(msg);
                message.setContent(XMLStreamReader.class, xmlReader);
                // replace headers
                try {
                    SAAJInInterceptor.replaceHeaders(msg, (SoapMessage) message);
                } catch (SOAPException e) {
                }
            }
        }

    }

    @Override
    public void handleFault(Message message) {
        // nothing need to do here as all logic has been handled by handle message method.
        // when there is error occurs in handleMessage() method for global handler, the interceptor will call all previously called
        // global handlers' handleFault() method
        // code logic can be seen in GlobalHandlerChainInvoker.invokeHandleMessage() method
    }

    private List<Handler> getHanderList(LibertyApplicationBus.Type busType, String flow) {
        GlobalHandlerService globalHandlerService = JaxwsGlobalHandlerServiceImpl.globalHandlerServiceSR.getService();
        List<Handler> registeredHandlers1 = globalHandlerService.getJAXWSClientSideInFlowGlobalHandlers();
        List<Handler> registeredHandlers2 = globalHandlerService.getJAXWSClientSideOutFlowGlobalHandlers();
        List<Handler> registeredHandlers3 = globalHandlerService.getJAXWSServerSideInFlowGlobalHandlers();
        List<Handler> registeredHandlers4 = globalHandlerService.getJAXWSServerSideOutFlowGlobalHandlers();
        if (busType == Type.SERVER && flow.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN)) {
            return registeredHandlers3;
        }
        if (busType == Type.SERVER && flow.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
            return registeredHandlers4;
        }
        if (busType == Type.CLIENT && flow.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN)) {
            return registeredHandlers1;
        }
        if (busType == Type.CLIENT && flow.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
            return registeredHandlers2;
        }
        return null;

    }

    private XMLStreamReader createXMLStreamReaderFromSOAPMessage(SOAPMessage soapMessage) {
       XMLStreamReader xmlReader = null;
        try {
            DOMSource bodySource = new DOMSource(soapMessage.getSOAPPart().getEnvelope().getBody());
            xmlReader = StaxUtils.createXMLStreamReader(bodySource);
            xmlReader.nextTag();
            xmlReader.nextTag(); // move past body tag
        } catch (SOAPException e) {
            // Do nothing
            throw new Fault(e);
        } catch (XMLStreamException e) {
            //  Do nothing

        }
        return xmlReader;
    }

}
