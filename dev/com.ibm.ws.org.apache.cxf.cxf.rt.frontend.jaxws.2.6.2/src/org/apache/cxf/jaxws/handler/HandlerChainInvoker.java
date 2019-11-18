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

package org.apache.cxf.jaxws.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.soap.SOAPFaultException;

import org.w3c.dom.Node;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.saaj.SAAJFactoryResolver;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.context.WebServiceContextImpl;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;

/**
 * invoke the handlers in a registered handler chain
 */
public class HandlerChainInvoker {

    private static final Logger LOG = LogUtils.getL7dLogger(HandlerChainInvoker.class);

    private final List<Handler<?>> protocolHandlers = new ArrayList<Handler<?>>();
    private List<LogicalHandler<?>> logicalHandlers = new ArrayList<LogicalHandler<?>>();

    private final List<Handler<?>> invokedHandlers = new ArrayList<Handler<?>>();
    private final List<Handler<?>> closeHandlers = new ArrayList<Handler<?>>();

    private boolean outbound;
    private boolean isRequestor;
    private boolean responseExpected = true;
    private boolean faultExpected;
    private boolean closed;
    private boolean messageDirectionReversed;
    private Exception fault;
    private LogicalMessageContext logicalMessageContext;
    private MessageContext protocolMessageContext;

    public HandlerChainInvoker(@SuppressWarnings("rawtypes") List<Handler> hc) {
        this(hc, true);
    }

    public HandlerChainInvoker(@SuppressWarnings("rawtypes") List<Handler> hc, boolean isOutbound) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "invoker for chain size: " + (hc != null ? hc.size() : 0));
        }

        if (hc != null) {
            for (Handler<?> h : hc) {
                if (h instanceof LogicalHandler) {
                    logicalHandlers.add((LogicalHandler<?>)h);
                } else {
                    protocolHandlers.add(h);
                }
            }
        }
        outbound = isOutbound;
    }

    public List<LogicalHandler<?>> getLogicalHandlers() {
        return logicalHandlers;
    }

    public List<Handler<?>> getProtocolHandlers() {
        return protocolHandlers;
    }

    public LogicalMessageContext getLogicalMessageContext() {
        return logicalMessageContext;
    }

    public void setLogicalMessageContext(LogicalMessageContext mc) {
        logicalMessageContext = mc;
    }

    public MessageContext getProtocolMessageContext() {
        return protocolMessageContext;
    }

    public void setProtocolMessageContext(MessageContext mc) {
        protocolMessageContext = mc;
    }

    public boolean invokeLogicalHandlers(boolean requestor, LogicalMessageContext context) {
        return invokeHandlerChain(logicalHandlers, context);
    }

    public boolean invokeLogicalHandlersHandleFault(boolean requestor, LogicalMessageContext context) {
        return invokeHandlerChainHandleFault(logicalHandlers, context);
    }

    public boolean invokeProtocolHandlers(boolean requestor, MessageContext context) {
        return invokeHandlerChain(protocolHandlers, context);
    }

    public boolean invokeProtocolHandlersHandleFault(boolean requestor, MessageContext context) {
        return invokeHandlerChainHandleFault(protocolHandlers, context);
    }

    public void setResponseExpected(boolean expected) {
        responseExpected = expected;
    }

    public boolean isResponseExpected() {
        return responseExpected;
    }

    public boolean isOutbound() {
        return outbound;
    }

    public boolean isInbound() {
        return !outbound;
    }

    /**
     * We need HandlerChainInvoker behaves differently on the client and server
     * side. For the client side, as there is no inbound faultChain, we need to call
     * handleFault and close within HandlerChainInvoker directly.
     */
    public boolean isRequestor() {
        return isRequestor;
    }

    public void setRequestor(boolean requestor) {
        isRequestor = requestor;
    }

    public void setInbound() {
        outbound = false;
    }

    public void setOutbound() {
        outbound = true;
    }

    public boolean faultRaised() {
        return null != fault || faultExpected;
    }

    public Exception getFault() {
        return fault;
    }

    public void setFault(boolean fe) {
        faultExpected = fe;
    }

    /**
     * Invoke handlers at the end of an MEP calling close on each. The handlers
     * must be invoked in the reverse order that they appear in the handler
     * chain. On the server side this will not be the reverse order in which
     * they were invoked so use the handler chain directly and not simply the
     * invokedHandler list.
     */
    public void mepComplete(Message message) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "closing protocol handlers - handler count:" + invokedHandlers.size());
        }

        if (isClosed()) {
            return;
        }

        invokeReversedClose();
    }

    /**
     * Indicates that the invoker is closed. When closed, only
     * @see #mepComplete may be called. The invoker will become closed if during
     *      a invocation of handlers, a handler throws a runtime exception that
     *      is not a protocol exception and no futher handler or message
     *      processing is possible.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Allows an the logical handler chain for one invoker to be used as an
     * alternate chain for another.
     *
     * @param invoker the invoker encalsulting the alternate logical handler
     *            chain
     */
    public void adoptLogicalHandlers(HandlerChainInvoker invoker) {
        logicalHandlers = invoker.getLogicalHandlers();
    }

    List<Handler<?>> getInvokedHandlers() {
        return Collections.unmodifiableList(invokedHandlers);
    }

    private boolean invokeHandlerChain(List<? extends Handler<?>> handlerChain, MessageContext ctx) {
        if (handlerChain.isEmpty()) {
            LOG.log(Level.FINEST, "no handlers registered");
            return true;
        }

        if (isClosed()) {
            return false;
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "invoking handlers, direction: " + (outbound ? "outbound" : "inbound"));
        }

        if (!outbound) {
            handlerChain = reverseHandlerChain(handlerChain);
        }

        boolean continueProcessing = true;
        MessageContext oldCtx = null;
        try {
            oldCtx = WebServiceContextImpl.setMessageContext(ctx);
            continueProcessing = invokeHandleMessage(handlerChain, ctx);
        } finally {
            // restore the WebServiceContextImpl's ThreadLocal variable to the previous value
            if (oldCtx == null) {
                WebServiceContextImpl.clear();
            } else {
                WebServiceContextImpl.setMessageContext(oldCtx);
            }
        }

        return continueProcessing;
    }

    /*
     * REVISIT: the logic of current implemetation is if the exception is thrown
     * from previous handlers, we only invoke handleFault if it is
     * ProtocolException (per spec), if the exception is thrown from other
     * places other than handlers, we always invoke handleFault.
     */
    private boolean invokeHandlerChainHandleFault(List<? extends Handler<?>> handlerChain, 
        MessageContext ctx) {
        if (handlerChain.isEmpty()) {
            LOG.log(Level.FINEST, "no handlers registered");
            return true;
        }

        if (isClosed()) {
            return false;
        }

        //The fault is raised from previous handlers, in this case, we only invoke handleFault
        //if the fault is a ProtocolException
        if (fault != null) {
            if (!(fault instanceof ProtocolException)) {
                return true;
            } else if (!responseExpected && !messageDirectionReversed) {
                // According to jsr224 9.3.2.1,
                // If throw ProtocolException or a subclass:
                // No response, normal message processing stops, close is called on each previously invoked handler
                // in the chain, the exception is dispatched (see section 9.1.2.3).
                return true;
            }
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "invoking handlers, direction: " + (outbound ? "outbound" : "inbound"));
        }
        setMessageOutboundProperty(ctx);

        if (!outbound) {
            handlerChain = reverseHandlerChain(handlerChain);
        }

        boolean continueProcessing = true;
        MessageContext oldCtx = null;
        try {
            oldCtx = WebServiceContextImpl.setMessageContext(ctx);
            continueProcessing = invokeHandleFault(handlerChain, ctx);
        } finally {
            // restore the WebServiceContextImpl's ThreadLocal variable to the previous value
            if (oldCtx == null) {
                WebServiceContextImpl.clear();
            } else {
                WebServiceContextImpl.setMessageContext(oldCtx);
            }
        }

        return continueProcessing;
    }

    @SuppressWarnings("unchecked")
    private boolean invokeHandleFault(List<? extends Handler<?>> handlerChain, MessageContext ctx) {
        boolean continueProcessing = true;

        try {
            for (Handler<?> h : handlerChain) {
                if (invokeThisHandler(h)) {
                    closeHandlers.add(h);
                    markHandlerInvoked(h);
                    Handler<MessageContext> lh = (Handler<MessageContext>)h;
                    continueProcessing = lh.handleFault(ctx);
                }
                if (!continueProcessing) {
                    break;
                }

            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "HANDLER_RAISED_RUNTIME_EXCEPTION", e);
            continueProcessing = false;
            throw e;
        }
        return continueProcessing;
    }

    @SuppressWarnings("unchecked")
    private boolean invokeHandleMessage(List<? extends Handler<?>> handlerChain, MessageContext ctx) {
        boolean continueProcessing = true;
        try {
            for (Handler<?> h : handlerChain) {
                if (invokeThisHandler(h)) {
                    closeHandlers.add(h);
                    markHandlerInvoked(h);
                    Handler<MessageContext> lh = (Handler<MessageContext>)h;
                    continueProcessing = lh.handleMessage(ctx);
                }
                if (!continueProcessing) {
                    if (responseExpected) {
                        changeMessageDirection(ctx);
                        messageDirectionReversed = true;
                    } else {
                        invokeReversedClose();                        
                    }

                    break;
                }
            }
        } catch (ProtocolException e) {
            LOG.log(Level.FINE, "handleMessage raised exception", e);

            if (responseExpected) {
                changeMessageDirection(ctx);
                messageDirectionReversed = true;
            }

            //special case for client side, this is because we do nothing in client fault
            //observer, we have to call handleFault and close here.
            if (isRequestor()) {
                if (responseExpected) {
                    setFaultMessage(ctx, e);
                    invokeReversedHandleFault(ctx);
                } else {
                    invokeReversedClose();
                }
                continueProcessing = false;
                setFault(e);
                if (responseExpected || isInbound()) {
                    //brain dead spec - if it's one way, swallow it
                    if (e instanceof SOAPFaultException) {
                        throw mapSoapFault((SOAPFaultException)e);
                    }
                    throw e;
                }
            } else {
                continueProcessing = false;
                if (responseExpected || outbound) {
                    setFault(e);
                    if (e instanceof SOAPFaultException) {
                        throw mapSoapFault((SOAPFaultException)e);
                    }
                    throw e;
                } 
                invokeReversedClose();
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "HANDLER_RAISED_RUNTIME_EXCEPTION", e);

            if (responseExpected) {
                changeMessageDirection(ctx);
                messageDirectionReversed = true;
            }

            //special case for client side, this is because we do nothing in client fault
            //observer, we have to call close here.
            if (isRequestor()) {
                invokeReversedClose();
                continueProcessing = false;
                setFault(e);
                throw e;
            } else if (!responseExpected && !outbound) {
                invokeReversedClose();
                continueProcessing = false;
            } else {
                continueProcessing = false;
                setFault(e);
                throw e;
            }
        }
        return continueProcessing;
    }

    private SoapFault mapSoapFault(SOAPFaultException sfe) {
        SoapFault sf = new SoapFault(sfe.getFault().getFaultString(),
                                      sfe,
                                      sfe.getFault().getFaultCodeAsQName());
        sf.setRole(sfe.getFault().getFaultActor());
        if (sfe.getFault().hasDetail()) {
            sf.setDetail(sfe.getFault().getDetail());
        }
        
        return sf;        
    }

    /*
     * When the message direction is reversed, if the message is not already a
     * fault message then it is replaced with a fault message
     */
    private void setFaultMessage(MessageContext mc, Exception exception) {
        Message msg = ((WrappedMessageContext)mc).getWrappedMessage();
        msg.setContent(Exception.class, exception);
        msg.removeContent(XMLStreamReader.class);
        msg.removeContent(Source.class);

        try {
            SOAPMessage soapMessage = null;

            SoapVersion version = null;
            if (msg instanceof SoapMessage) {
                version = ((SoapMessage)msg).getVersion();
            }
            soapMessage = SAAJFactoryResolver.createMessageFactory(version).createMessage();
            msg.setContent(SOAPMessage.class, soapMessage);

            SOAPBody body = SAAJUtils.getBody(soapMessage);
            SOAPFault soapFault = body.addFault();

            if (exception instanceof SOAPFaultException) {
                SOAPFaultException sf = (SOAPFaultException)exception;
                soapFault.setFaultString(sf.getFault().getFaultString());
                SAAJUtils.setFaultCode(soapFault, sf.getFault().getFaultCodeAsQName());
                soapFault.setFaultActor(sf.getFault().getFaultActor());
                if (sf.getFault().hasDetail()) {
                    Node nd = soapMessage.getSOAPPart().importNode(sf.getFault().getDetail(),
                                                                   true);
                    nd = nd.getFirstChild();
                    soapFault.addDetail();
                    while (nd != null) {
                        soapFault.getDetail().appendChild(nd);
                        nd = nd.getNextSibling();
                    }
                }
            } else if (exception instanceof Fault) {
                SoapFault sf = SoapFault.createFault((Fault)exception, ((SoapMessage)msg).getVersion());
                soapFault.setFaultString(sf.getReason());
                SAAJUtils.setFaultCode(soapFault, sf.getFaultCode());
                if (sf.hasDetails()) {
                    soapFault.addDetail();
                    Node nd = soapMessage.getSOAPPart().importNode(sf.getDetail(), true);
                    nd = nd.getFirstChild();
                    while (nd != null) {
                        soapFault.getDetail().appendChild(nd);
                        nd = nd.getNextSibling();
                    }
                }
            } else {
                SAAJUtils.setFaultCode(soapFault, 
                                       new QName("http://cxf.apache.org/faultcode", "HandlerFault"));
                soapFault.setFaultString(exception.getMessage());
            }
        } catch (SOAPException e) {
            e.printStackTrace();
            // do nothing
        }
    }

    @SuppressWarnings("unchecked")
    private boolean invokeReversedHandleFault(MessageContext ctx) {
        boolean continueProcessing = true;

        try {
            int index = invokedHandlers.size() - 2;
            while (index >= 0 && continueProcessing) {
                Handler<? extends MessageContext> h = invokedHandlers.get(index);
                if (h instanceof LogicalHandler) {
                    LogicalHandler<LogicalMessageContext> lh = (LogicalHandler<LogicalMessageContext>)h;
                    continueProcessing = lh.handleFault(logicalMessageContext);
                } else {
                    Handler<MessageContext> ph = (Handler<MessageContext>)h; 
                    continueProcessing = ph.handleFault(protocolMessageContext);
                }

                if (!continueProcessing) {
                    invokeReversedClose();
                    break;
                }
                index--;
            }
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "HANDLER_RAISED_RUNTIME_EXCEPTION", e);
            invokeReversedClose();
            continueProcessing = false;
            closed = true;

            throw e;
        }
        invokeReversedClose();
        return continueProcessing;
    }

    /*
     * close is called on each previously invoked handler in the chain, the
     * close method is only called on handlers that were previously invoked via
     * either handleMessage or handleFault
     */
    private void invokeReversedClose() {
        int index = invokedHandlers.size() - 1;
        while (index >= 0) {
            Handler<?> handler = invokedHandlers.get(index);
            if (handler instanceof LogicalHandler) {
                handler.close(logicalMessageContext);
            } else {
                handler.close(protocolMessageContext);
            }
            invokedHandlers.remove(index);
            index--;
        }
        closed = true;
    }

    private boolean invokeThisHandler(Handler<?> h) {
        boolean ret = true;
        // when handler processing has been aborted, only invoke on
        // previously invoked handlers
        //Only invoke the next handler (take the reversed direction into account)
        if (messageDirectionReversed) {
            ret = invokedHandlers.contains(h) && !isTheLastInvokedHandler(h);
        }

        if (ret && LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "invoking handler of type " + h.getClass().getName());
        }
        return ret;
    }

    private boolean isTheLastInvokedHandler(Handler<?> h) {
        return invokedHandlers.contains(h) && invokedHandlers.indexOf(h) == (invokedHandlers.size() - 1);
    }

    private void markHandlerInvoked(Handler<?> h) {
        if (!invokedHandlers.contains(h)) {
            invokedHandlers.add(h);
        }
    }

    private void changeMessageDirection(MessageContext context) {
        outbound = !outbound;
        setMessageOutboundProperty(context);
    }

    private void setMessageOutboundProperty(MessageContext context) {
        context.put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, this.outbound);
        if (logicalMessageContext != null) {
            logicalMessageContext.put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, this.outbound);
        }
        if (protocolMessageContext != null) {
            protocolMessageContext.put(MessageContext.MESSAGE_OUTBOUND_PROPERTY, this.outbound);
        }
    }

    private <T extends Handler<?>> List<T> reverseHandlerChain(List<T> handlerChain) {
        List<T> reversedHandlerChain = new ArrayList<T>();
        reversedHandlerChain.addAll(handlerChain);
        Collections.reverse(reversedHandlerChain);
        return reversedHandlerChain;
    }

    protected final void setFault(Exception ex) {
        fault = ex;
    }
}
