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

package org.apache.cxf.endpoint;

import java.io.Closeable;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.i18n.UncheckedException;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.interceptor.ClientOutFaultObserver;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.workqueue.SynchronousExecutor;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

// Liberty Changes - Could potentially be removed when updating to CXF 3.5.5 
// Trace changes could be removed for instrumentation disabled in bundle file
@Trivial // Liberty Change: Other Liberty overrides cause tracing to print sensitive message objects
public class ClientImpl
    extends AbstractBasicInterceptorProvider
    implements Client, Retryable, MessageObserver {

    public static final String THREAD_LOCAL_REQUEST_CONTEXT = "thread.local.request.context";
    /**
     * When a synchronous request/response invoke is done using an asynchronous transport mechanism,
     * this is the timeout used for waiting for the response.  Default is 60 seconds.
     */
    public static final String SYNC_TIMEOUT = "cxf.synchronous.timeout";

    public static final String FINISHED = "exchange.finished";

    private static final Logger LOG = LogUtils.getL7dLogger(ClientImpl.class);

    protected Bus bus;
    protected ConduitSelector conduitSelector;
    protected ClientOutFaultObserver outFaultObserver;
    protected int synchronousTimeout = 60000; // default 60 second timeout

    protected PhaseChainCache outboundChainCache = new PhaseChainCache();
    protected PhaseChainCache inboundChainCache = new PhaseChainCache();

    protected Map<String, Object> currentRequestContext = new ConcurrentHashMap<>(8, 0.75f, 4);
    protected Thread latestContextThread;
    protected Map<Thread, EchoContext> requestContext
        = Collections.synchronizedMap(new WeakHashMap<Thread, EchoContext>());

    protected Map<Thread, ResponseContext> responseContext
        = Collections.synchronizedMap(new WeakHashMap<Thread, ResponseContext>());

    protected Executor executor;

    public ClientImpl(Bus b, Endpoint e) {
        this(b, e, (ConduitSelector)null);
    }

    public ClientImpl(Bus b, Endpoint e, Conduit c) {
        this(b, e, new PreexistingConduitSelector(c));
    }

    public ClientImpl(Bus b, Endpoint e, ConduitSelector sc) {
        bus = b;
        outFaultObserver = new ClientOutFaultObserver(bus);
        getConduitSelector(sc).setEndpoint(e);
        notifyLifecycleManager();
    }

    /**
     * Create a Client that uses a specific EndpointImpl.
     * @param bus
     * @param svc
     * @param port
     * @param endpointImplFactory
     */
    public ClientImpl(Bus bus, Service svc, QName port,
                      EndpointImplFactory endpointImplFactory) {
        this.bus = bus;
        outFaultObserver = new ClientOutFaultObserver(bus);
        EndpointInfo epfo = findEndpoint(svc, port);

        try {
            if (endpointImplFactory != null) {
                getConduitSelector().setEndpoint(endpointImplFactory.newEndpointImpl(bus, svc, epfo));
            } else {
                getConduitSelector().setEndpoint(new EndpointImpl(bus, svc, epfo));
            }
        } catch (EndpointException epex) {
            throw new IllegalStateException("Unable to create endpoint: " + epex.getMessage(), epex);
        }
        notifyLifecycleManager();
    }

    public Bus getBus() {
        return bus;
    }

    public void destroy() {
        if (bus == null) {
            return;
        }
        if (getEndpoint() != null) {
            for (Closeable c : getEndpoint().getCleanupHooks()) {
                try {
                    c.close();
                } catch (IOException e) {
		    LOG.finest("ClientImpl: Unexpected IOException: " + e); // Liberty Change
                    //ignore
                }
            }
        }
        ClientLifeCycleManager mgr = bus.getExtension(ClientLifeCycleManager.class);
        if (null != mgr) {
            mgr.clientDestroyed(this);
        }

        if (conduitSelector != null) {
            if (conduitSelector instanceof Closeable) {
                try {
                    ((Closeable)conduitSelector).close();
                } catch (IOException e) {
                    //ignore, we're destroying anyway
		    LOG.finest("ClientImpl: Unexpected IOException 2: " + e); // Liberty Change
                }
            } else {
                getConduit().close();
            }
        }

        bus = null;
        conduitSelector = null;
        outFaultObserver = null;
        outboundChainCache = null;
        inboundChainCache = null;

        currentRequestContext = null;
        requestContext.clear();
        requestContext = null;
        responseContext.clear();
        responseContext = null;
        executor = null;

    }

    private void notifyLifecycleManager() {
        ClientLifeCycleManager mgr = bus.getExtension(ClientLifeCycleManager.class);
        if (null != mgr) {
            mgr.clientCreated(this);
        }
    }

    private EndpointInfo findEndpoint(Service svc, QName port) {

        if (port != null) {
            EndpointInfo epfo = svc.getEndpointInfo(port);
            if (epfo == null) {
                throw new IllegalArgumentException("The service " + svc.getName()
                                                   + " does not have an endpoint " + port + ".");
            }
            return epfo;
        }
        
        for (ServiceInfo svcfo : svc.getServiceInfos()) {
            for (EndpointInfo e : svcfo.getEndpoints()) {
                BindingInfo bfo = e.getBinding();
                String bid = bfo.getBindingId();
                if ("http://schemas.xmlsoap.org/wsdl/soap/".equals(bid)
                    || "http://schemas.xmlsoap.org/wsdl/soap12/".equals(bid)) {
                    for (Object o : bfo.getExtensors().get()) {
                        try {
                            String s = (String)o.getClass().getMethod("getTransportURI").invoke(o);
                            if (s != null && s.endsWith("http")) {
                                return e;
                            }
                        } catch (Throwable t) {
		            LOG.finest("ClientImpl: Caught Unexpected Exception t: " + t); // Liberty Change
                            //ignore
                        }
                    }
                }
            }
        }
        throw new UnsupportedOperationException(
             "Only document-style SOAP 1.1 and 1.2 http are supported "
             + "for auto-selection of endpoint; none were found.");
    }

    public Endpoint getEndpoint() {
        return getConduitSelector().getEndpoint();
    }

    public void releaseThreadContexts() {
        final Thread t = Thread.currentThread();
        requestContext.remove(t);
        responseContext.remove(t);
    }

    @Override
    public Contexts getContexts() {
        return new Contexts() {
            @Override
            public void close() throws Exception {
                releaseThreadContexts();
            }
            @Override
            public Map<String, Object> getRequestContext() {
                return ClientImpl.this.getRequestContext();
            }
            @Override
            public Map<String, Object> getResponseContext() {
                return ClientImpl.this.getResponseContext();
            }
        };
    }

    public Map<String, Object> getRequestContext() {
        if (isThreadLocalRequestContext()) {
            final Thread t = Thread.currentThread();
            requestContext.computeIfAbsent(t, k -> new EchoContext(currentRequestContext));
            latestContextThread = t;
            return requestContext.get(t);
        }
        return currentRequestContext;
    }
    public Map<String, Object> getResponseContext() {
        if (!responseContext.containsKey(Thread.currentThread())) {
            final Thread t = Thread.currentThread();
            responseContext.put(t, new ResponseContext(responseContext));
        }
        return responseContext.get(Thread.currentThread());
    }
    protected Map<String, Object> setResponseContext(Map<String, Object> ctx) {
        if (ctx instanceof ResponseContext) {
            ResponseContext c = (ResponseContext)ctx;
            responseContext.put(Thread.currentThread(), c);
            return c;
        }
        ResponseContext c = new ResponseContext(ctx, responseContext);
        responseContext.put(Thread.currentThread(), c);
        return c;
    }
    public boolean isThreadLocalRequestContext() {
        Object o = currentRequestContext.get(THREAD_LOCAL_REQUEST_CONTEXT);
        if (o != null) {
            final boolean local;
            if (o instanceof Boolean) {
                local = ((Boolean)o).booleanValue();
            } else {
                local = Boolean.parseBoolean(o.toString());
            }
            return local;
        }
        return false;
    }
    public void setThreadLocalRequestContext(boolean b) {
        currentRequestContext.put(THREAD_LOCAL_REQUEST_CONTEXT, b);
    }


    public Object[] invoke(BindingOperationInfo oi, Object... params) throws Exception {
        return invoke(oi, params, null);
    }

    public Object[] invoke(String operationName, Object... params) throws Exception {
        QName q = new QName(getEndpoint().getService().getName().getNamespaceURI(), operationName);

        return invoke(q, params);
    }

    public Object[] invoke(QName operationName, Object... params) throws Exception {
        BindingOperationInfo op = getEndpoint().getEndpointInfo().getBinding().getOperation(operationName);
        if (op == null) {
            throw new UncheckedException(
                new org.apache.cxf.common.i18n.Message("NO_OPERATION", LOG, operationName));
        }

        if (op.isUnwrappedCapable()) {
            op = op.getUnwrappedOperation();
        }

        return invoke(op, params);
    }

    public Object[] invokeWrapped(String operationName, Object... params) throws Exception {
        QName q = new QName(getEndpoint().getService().getName().getNamespaceURI(), operationName);

        return invokeWrapped(q, params);
    }

    public Object[] invokeWrapped(QName operationName, Object... params) throws Exception {
        BindingOperationInfo op = getEndpoint().getEndpointInfo().getBinding().getOperation(operationName);
        if (op == null) {
            throw new UncheckedException(
                new org.apache.cxf.common.i18n.Message("NO_OPERATION", LOG, operationName));
        }
        return invoke(op, params);
    }

    public Object[] invoke(BindingOperationInfo oi,
                           Object[] params,
                           Exchange exchange) throws Exception {
        Map<String, Object> context = new HashMap<>();
        return invoke(oi, params, context, exchange);
    }
    public Object[] invoke(BindingOperationInfo oi,
                           Object[] params,
                           Map<String, Object> context) throws Exception {
        return invoke(oi, params, context, (Exchange)null);
    }

    public void invoke(ClientCallback callback,
                       String operationName,
                       Object... params) throws Exception {
        QName q = new QName(getEndpoint().getService().getName().getNamespaceURI(), operationName);
        invoke(callback, q, params);
    }

    public void invoke(ClientCallback callback,
                       QName operationName,
                       Object... params) throws Exception {
        BindingOperationInfo op = getEndpoint().getEndpointInfo().getBinding().getOperation(operationName);
        if (op == null) {
            throw new UncheckedException(
                new org.apache.cxf.common.i18n.Message("NO_OPERATION", LOG, operationName));
        }

        if (op.isUnwrappedCapable()) {
            op = op.getUnwrappedOperation();
        }

        invoke(callback, op, params);
    }


    public void invokeWrapped(ClientCallback callback,
                              String operationName,
                              Object... params)
        throws Exception {
        QName q = new QName(getEndpoint().getService().getName().getNamespaceURI(), operationName);
        invokeWrapped(callback, q, params);
    }

    public void invokeWrapped(ClientCallback callback,
                              QName operationName,
                              Object... params)
        throws Exception {
        BindingOperationInfo op = getEndpoint().getEndpointInfo().getBinding().getOperation(operationName);
        if (op == null) {
            throw new UncheckedException(
                new org.apache.cxf.common.i18n.Message("NO_OPERATION", LOG, operationName));
        }
        invoke(callback, op, params);
    }


    public void invoke(ClientCallback callback,
                       BindingOperationInfo oi,
                       Object... params) throws Exception {
        invoke(callback, oi, params, null, null);
    }

    public void invoke(ClientCallback callback,
                       BindingOperationInfo oi,
                       Object[] params,
                       Map<String, Object> context) throws Exception {
        invoke(callback, oi, params, context, null);
    }

    public void invoke(ClientCallback callback,
                       BindingOperationInfo oi,
                       Object[] params,
                       Exchange exchange) throws Exception {
        invoke(callback, oi, params, null, exchange);
    }

    public void invoke(ClientCallback callback,
                       BindingOperationInfo oi,
                       Object[] params,
                       Map<String, Object> context,
                       Exchange exchange) throws Exception {
        doInvoke(callback, oi, params, context, exchange);
    }

    public Object[] invoke(BindingOperationInfo oi,
                           Object[] params,
                           Map<String, Object> context,
                           Exchange exchange) throws Exception {
        return doInvoke(null, oi, params, context, exchange);
    }

    private Object[] doInvoke(final ClientCallback callback,
                              BindingOperationInfo oi,
                              Object[] params,
                              Map<String, Object> context,
                              Exchange exchange) throws Exception {
        Bus origBus = BusFactory.getAndSetThreadDefaultBus(bus);
        ClassLoaderHolder origLoader = null;
        Map<String, Object> resContext = null;
        try {
            ClassLoader loader = bus.getExtension(ClassLoader.class);
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            if (exchange == null) {
	        LOG.fine("doInvoke: Creating new Exchange.");
                exchange = new ExchangeImpl();
            }
            exchange.setSynchronous(callback == null);
            Endpoint endpoint = getEndpoint();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Invoke, operation info: " + oi + ", params: " + Arrays.toString(params));
            }
            Message message = endpoint.getBinding().createMessage();

            // Make sure INVOCATION CONTEXT, REQUEST_CONTEXT and RESPONSE_CONTEXT are present
            // on message
            if (context == null) {
                context = new HashMap<>();
            }
            Map<String, Object> reqContext = CastUtils.cast((Map<?, ?>)context.get(REQUEST_CONTEXT));
            resContext = CastUtils.cast((Map<?, ?>)context.get(RESPONSE_CONTEXT));
            if (reqContext == null) {
                reqContext = new HashMap<>(getRequestContext());
                context.put(REQUEST_CONTEXT, reqContext);
            }
            if (resContext == null) {
                resContext = new ResponseContext(responseContext);
                context.put(RESPONSE_CONTEXT, resContext);
            }

	    LOG.fine("Setting following requestContext on message: " + reqContext);  // Liberty Change
            message.put(Message.INVOCATION_CONTEXT, context);
            setContext(reqContext, message);
            exchange.putAll(reqContext);

            setParameters(params, message);

            if (null != oi) {
                exchange.setOneWay(oi.getOutput() == null);
            }

            exchange.setOutMessage(message);
            exchange.put(ClientCallback.class, callback);

            setOutMessageProperties(message, oi);
            setExchangeProperties(exchange, endpoint, oi);

            PhaseInterceptorChain chain = setupInterceptorChain(endpoint);
            message.setInterceptorChain(chain);
            if (callback == null) {
                chain.setFaultObserver(outFaultObserver);
            } else {
                // We need to wrap the outFaultObserver if the callback is not null
                // calling the conduitSelector.complete to make sure the fail over feature works
                chain.setFaultObserver(new MessageObserver() {
                    public void onMessage(Message message) {
                        Exception ex = message.getContent(Exception.class);
                        if (ex != null) {
                            completeExchange(message.getExchange());
                            if (message.getContent(Exception.class) == null) {
                                // handle the right response
                                Message inMsg = message.getExchange().getInMessage();
                                Map<String, Object> ctx = responseContext.get(Thread.currentThread());
                                List<Object> resList = CastUtils.cast(inMsg.getContent(List.class));
                                Object[] result = resList == null ? null : resList.toArray();
                                callback.handleResponse(ctx, result);
                                return;
                            }
                        }
                        outFaultObserver.onMessage(message);
                    }
                });
            }
            prepareConduitSelector(message);

            // add additional interceptors and such
            modifyChain(chain, message, false);
            try {
                chain.doIntercept(message);
            } catch (Fault fault) {
                enrichFault(fault);
                throw fault;
            }

            if (callback != null) {
		LOG.fine("Callback is not null, returning here.");  // Liberty Change
                return null;
            }
            return processResult(message, exchange, oi, resContext);
        } finally {
            //ensure ResponseContext has HTTP RESPONSE CODE
            if (null != exchange) {
                Integer responseCode = (Integer)exchange.get(Message.RESPONSE_CODE);
                resContext.put(MessageContext.HTTP_RESPONSE_CODE, responseCode);
                resContext.put(org.apache.cxf.message.Message.RESPONSE_CODE, responseCode);
                setResponseContext(resContext);
	        LOG.fine("setResponseContext: " + resContext);  // Liberty Change
            }
            if (origLoader != null) {
		LOG.fine("Reset origLoader");  // Liberty Change
                origLoader.reset();
            }
            if (origBus != bus) {
		LOG.fine("setThreadDefaultBus");  // Liberty Change
                BusFactory.setThreadDefaultBus(origBus);
            }
        }
    }

    private void completeExchange(Exchange exchange) {
        getConduitSelector().complete(exchange);
    }

    /**
     * TODO This is SOAP specific code and should not be in cxf core
     * @param fault
     */
    private void enrichFault(Fault fault) {
        if (fault.getCause().getCause() instanceof IOException
                || fault.getCause() instanceof IOException) {
            String soap11NS = "http://schemas.xmlsoap.org/soap/envelope/";
            String soap12NS = "http://www.w3.org/2003/05/soap-envelope";
            QName faultCode = fault.getFaultCode();
            //for SoapFault, if it's underlying cause is IOException,
            //it means something like server is down or can't create
            //connection, according to soap spec we should set fault as
            //Server Fault
            if (faultCode.getNamespaceURI().equals(
                    soap11NS)
                    && "Client".equals(faultCode.getLocalPart())) {
                faultCode = new QName(soap11NS, "Server");
		LOG.fine("enrichFault: Setting SOAP 1.1 faultcode: " + faultCode); // Liberty Change
                fault.setFaultCode(faultCode);
            }
            if (faultCode.getNamespaceURI().equals(
                    soap12NS)
                    && "Sender".equals(faultCode.getLocalPart())) {
                faultCode = new QName(soap12NS, "Receiver");
		LOG.fine("enrichFault: Setting SOAP 1.2 faultcode: " + faultCode); // Liberty Change
                fault.setFaultCode(faultCode);
            }
        }
    }

    protected Object[] processResult(Message message,
                                   Exchange exchange,
                                   BindingOperationInfo oi,
                                   Map<String, Object> resContext) throws Exception {
        Exception ex = null;
        // Check to see if there is a Fault from the outgoing chain if it's an out Message
        if (!message.get(Message.INBOUND_MESSAGE).equals(Boolean.TRUE)) {
            ex = message.getContent(Exception.class);
        }
        boolean mepCompleteCalled = false;
        if (ex != null) {
            completeExchange(exchange);
            mepCompleteCalled = true;
            if (message.getContent(Exception.class) != null) {
                throw ex;
            }
        }
        ex = message.getExchange().get(Exception.class);
        if (ex != null) {
            if (!mepCompleteCalled) {
                completeExchange(exchange);
            }
            throw ex;
        }

        //REVISIT
        // - use a protocol neutral no-content marker instead of 202?
        // - move the decoupled destination property name into api
        Integer responseCode = (Integer)exchange.get(Message.RESPONSE_CODE);
        if (null != responseCode && 202 == responseCode) {
            Endpoint ep = exchange.getEndpoint();
            if (null != ep && null != ep.getEndpointInfo() && null == ep.getEndpointInfo().
                getProperty("org.apache.cxf.ws.addressing.MAPAggregator.decoupledDestination")) {
                return null;
            }
        }

        // Wait for a response if we need to
        if (oi != null && !oi.getOperationInfo().isOneWay()) {
	    LOG.fine("processResult: Calling waitResponse...");  // Liberty Change
            waitResponse(exchange);
        }

        // leave the input stream open for the caller
        Boolean keepConduitAlive = (Boolean)exchange.get(Client.KEEP_CONDUIT_ALIVE);
        if (keepConduitAlive == null || !keepConduitAlive) {
	    LOG.fine("processResult: Complete Exchange."); // Liberty Change
            completeExchange(exchange);
        }

        // Grab the response objects if there are any
        List<Object> resList = null;
        Message inMsg = exchange.getInMessage();
        if (inMsg != null) {
            if (null != resContext) {
                resContext.putAll(inMsg);
                // remove the recursive reference if present
                resContext.remove(Message.INVOCATION_CONTEXT);
                setResponseContext(resContext);
	        LOG.fine("processResult: setResponseContext: " + resContext);  // Liberty Change
            }
            resList = CastUtils.cast(inMsg.getContent(List.class));
        }

        // check for an incoming fault
        ex = getException(exchange);

        if (ex != null) {
            throw ex;
        }

        if (resList == null   
            && oi != null && !oi.getOperationInfo().isOneWay()) {
            
            BindingOperationInfo boi = oi;
            if (boi.isUnwrapped()) {
                boi = boi.getWrappedOperation();
            }
            if (!boi.getOutput().getMessageParts().isEmpty()) {
                //we were supposed to get some output, but didn't.
                throw new IllegalEmptyResponseException("Response message did not contain proper response data."
                    + " Expected: " + boi.getOutput().getMessageParts().get(0).getConcreteName());
            }
        }
        if (resList != null) {
            return resList.toArray();
        }

        return null;
    }

    protected Exception getException(Exchange exchange) {
        if (exchange.getInFaultMessage() != null) {
            return exchange.getInFaultMessage().getContent(Exception.class);
        } else if (exchange.getOutFaultMessage() != null) {
            return exchange.getOutFaultMessage().getContent(Exception.class);
        } else if (exchange.getInMessage() != null) {
            return exchange.getInMessage().getContent(Exception.class);
        }
        return null;
    }

    @Sensitive // Liberty Code Change
    protected void setContext(Map<String, Object> ctx, Message message) {
        if (ctx != null) {
            message.putAll(ctx);
            // Liberty Change - disable liberty tracing of context
            //if (LOG.isLoggable(Level.FINE)) {
            //    LOG.fine("set requestContext to message be" ctx);
            //}
        }
    }

    protected void waitResponse(Exchange exchange) throws IOException {
        synchronized (exchange) {
            long remaining = synchronousTimeout;
            Long o = PropertyUtils.getLong(exchange.getOutMessage(), SYNC_TIMEOUT);
            if (o != null) {
                remaining = o;
            }
            while (!Boolean.TRUE.equals(exchange.get(FINISHED)) && remaining > 0) {
                long start = System.currentTimeMillis();
                try {
                    exchange.wait(remaining);
                } catch (InterruptedException ex) {
		    LOG.finest("waitResponse: Unexpected exception: " + ex);  // Liberty Change
                    // ignore
                }
                long end = System.currentTimeMillis();
                remaining -= (int)(end - start);
            }
            if (!Boolean.TRUE.equals(exchange.get(FINISHED))) {
                LogUtils.log(LOG, Level.WARNING, "RESPONSE_TIMEOUT",
                    exchange.getBindingOperationInfo().getOperationInfo().getName().toString());
                String msg = new org.apache.cxf.common.i18n.Message("RESPONSE_TIMEOUT", LOG, exchange
                    .getBindingOperationInfo().getOperationInfo().getName().toString()).toString();
                throw new IOException(msg);
            }
        }
    }

    protected void setParameters(Object[] params, Message message) {
        MessageContentsList contents = new MessageContentsList(params);
        message.setContent(List.class, contents);
    }

    public void onMessage(Message message) {
        if (bus == null) {
            throw new IllegalStateException("Message received on a Client that has been closed or destroyed.");
        }
        Endpoint endpoint = message.getExchange().getEndpoint();
        if (endpoint == null) {
            // in this case correlation will occur outside the transport,
            // however there's a possibility that the endpoint may have been
            // rebased in the meantime, so that the response will be mediated
            // via a set of in interceptors provided by a *different* endpoint
            //
            endpoint = getConduitSelector().getEndpoint();
            message.getExchange().put(Endpoint.class, endpoint);
        }
        message = endpoint.getBinding().createMessage(message);
        message.getExchange().setInMessage(message);
        message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        message.put(Message.INBOUND_MESSAGE, Boolean.TRUE);
        PhaseManager pm = bus.getExtension(PhaseManager.class);

        List<Interceptor<? extends Message>> i1 = bus.getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by bus: " + i1);
        }
        List<Interceptor<? extends Message>> i2 = getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by client: " + i2);
        }
        List<Interceptor<? extends Message>> i3 = endpoint.getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by endpoint: " + i3);
        }
        List<Interceptor<? extends Message>> i4 = endpoint.getBinding().getInInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by binding: " + i4);
        }

        PhaseInterceptorChain chain;
        if (endpoint.getService().getDataBinding() instanceof InterceptorProvider) {
            InterceptorProvider p = (InterceptorProvider)endpoint.getService().getDataBinding();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Interceptors contributed by databinding: " + p.getInInterceptors());
            }
            chain = inboundChainCache.get(pm.getInPhases(), i1, i2, i3, i4,
                                          p.getInInterceptors());
        } else {
            chain = inboundChainCache.get(pm.getInPhases(), i1, i2, i3, i4);
        }
        message.setInterceptorChain(chain);

        chain.setFaultObserver(outFaultObserver);
        modifyChain(chain, message, true);
        modifyChain(chain, message.getExchange().getOutMessage(), true);

        Bus origBus = BusFactory.getAndSetThreadDefaultBus(bus);
        // execute chain
        ClientCallback callback = message.getExchange().get(ClientCallback.class);
        try {
            if (callback != null) {
                if (callback.isCancelled()) {
                    completeExchange(message.getExchange());
                    return;
                }
                callback.start(message);
            }

            String startingAfterInterceptorID = (String) message.get(
                InterceptorChain.STARTING_AFTER_INTERCEPTOR_ID);
            String startingInterceptorID = (String) message.get(
                InterceptorChain.STARTING_AT_INTERCEPTOR_ID);
            if (startingAfterInterceptorID != null) {
                chain.doInterceptStartingAfter(message, startingAfterInterceptorID);
            } else if (startingInterceptorID != null) {
                chain.doInterceptStartingAt(message, startingInterceptorID);
            } else if (message.getContent(Exception.class) != null) {
                outFaultObserver.onMessage(message);
            } else {
                callback = message.getExchange().get(ClientCallback.class);

                if (callback != null && !isPartialResponse(message)) {
                    try {
                        chain.doIntercept(message);
                    } catch (Throwable error) {
                        //so that asyn callback handler get chance to
                        //handle non-runtime exceptions
                        message.getExchange().setInMessage(message);
                        Map<String, Object> resCtx = CastUtils
                                .cast((Map<?, ?>) message.getExchange()
                                        .getOutMessage().get(
                                                Message.INVOCATION_CONTEXT));
                        resCtx = CastUtils.cast((Map<?, ?>) resCtx
                                .get(RESPONSE_CONTEXT));
                        if (resCtx != null) {
                            setResponseContext(resCtx);
                        }
                        // remove callback so that it won't be invoked twice
                        callback = message.getExchange().remove(ClientCallback.class);
                        if (callback != null) {
                            callback.handleException(resCtx, error);
                        }
                    }
                } else {
                    chain.doIntercept(message);
                }

            }

            callback = message.getExchange().get(ClientCallback.class);
            if (callback == null || isPartialResponse(message)) {
                return;
            }

            // remove callback so that it won't be invoked twice
            callback = message.getExchange().remove(ClientCallback.class);
            if (callback != null) {
                message.getExchange().setInMessage(message);
                Map<String, Object> resCtx = CastUtils.cast((Map<?, ?>)message
                                                                .getExchange()
                                                                .getOutMessage()
                                                                .get(Message.INVOCATION_CONTEXT));
                resCtx = CastUtils.cast((Map<?, ?>)resCtx.get(RESPONSE_CONTEXT));
                if (resCtx != null && responseContext != null) {
                    setResponseContext(resCtx);
                }
                try {
                    Object[] obj = processResult(message, message.getExchange(),
                                                 null, resCtx);

                    callback.handleResponse(resCtx, obj);
                } catch (Throwable ex) {
                    callback.handleException(resCtx, ex);
                }
            }
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
            synchronized (message.getExchange()) {
                if (!isPartialResponse(message)
                    || message.getContent(Exception.class) != null) {
                    message.getExchange().put(FINISHED, Boolean.TRUE);
                    message.getExchange().setInMessage(message);
                    message.getExchange().notifyAll();
                }
            }
        }
    }

    public Conduit getConduit() {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        message.setExchange(exchange);
        message.putAll(getRequestContext());
        setExchangeProperties(exchange, getEndpoint(), null);
        return getConduitSelector().selectConduit(message);
    }

    protected void prepareConduitSelector(Message message) {
        getConduitSelector().prepare(message);
        message.getExchange().put(ConduitSelector.class, getConduitSelector());
    }

    protected void setOutMessageProperties(Message message, BindingOperationInfo boi) {
        message.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
        message.put(Message.INBOUND_MESSAGE, Boolean.FALSE);
        if (null != boi) {
            message.put(BindingMessageInfo.class, boi.getInput());
            message.put(MessageInfo.class, boi.getOperationInfo().getInput());
        }
    }

    protected void setExchangeProperties(Exchange exchange,
                                         Endpoint endpoint,
                                         BindingOperationInfo boi) {
        if (endpoint != null) {
            exchange.put(Endpoint.class, endpoint);
            exchange.put(Service.class, endpoint.getService());
            exchange.put(Binding.class, endpoint.getBinding());
        }
        if (boi != null) {
            exchange.put(BindingOperationInfo.class, boi);
        }

        if (exchange.isSynchronous() || executor == null) {
            exchange.put(MessageObserver.class, this);
        } else {
            exchange.put(Executor.class, executor);
            exchange.put(MessageObserver.class, new MessageObserver() {
                public void onMessage(final Message message) {
                    if (!message.getExchange()
                        .containsKey(Executor.class.getName() + ".USING_SPECIFIED")) {

                        executor.execute(new Runnable() {
                            public void run() {
                                ClientImpl.this.onMessage(message);
                            }
                        });
                    } else {
                        ClientImpl.this.onMessage(message);
                    }
                }
            });
        }
        exchange.put(Retryable.class, this);
        exchange.put(Client.class, this);
        exchange.put(Bus.class, bus);

        if (endpoint != null) {
            EndpointInfo endpointInfo = endpoint.getEndpointInfo();
            if (boi != null) {
                exchange.put(Message.WSDL_OPERATION, boi.getName());
            }

            QName serviceQName = endpointInfo.getService().getName();
            exchange.put(Message.WSDL_SERVICE, serviceQName);

            QName interfaceQName = endpointInfo.getService().getInterface().getName();
            exchange.put(Message.WSDL_INTERFACE, interfaceQName);

            QName portQName = endpointInfo.getName();
            exchange.put(Message.WSDL_PORT, portQName);
            URI wsdlDescription = endpointInfo.getProperty("URI", URI.class);
            if (wsdlDescription == null) {
                String address = endpointInfo.getAddress();
                try {
                    wsdlDescription = new URI(address + "?wsdl");
                } catch (URISyntaxException e) {
                    // do nothing
                }
                endpointInfo.setProperty("URI", wsdlDescription);
            }
            exchange.put(Message.WSDL_DESCRIPTION, wsdlDescription);
        }
    }

    protected PhaseInterceptorChain setupInterceptorChain(Endpoint endpoint) {

        PhaseManager pm = bus.getExtension(PhaseManager.class);

        List<Interceptor<? extends Message>> i1 = bus.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by bus: " + i1);
        }
        List<Interceptor<? extends Message>> i2 = getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by client: " + i2);
        }
        List<Interceptor<? extends Message>> i3 = endpoint.getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by endpoint: " + i3);
        }
        List<Interceptor<? extends Message>> i4 = endpoint.getBinding().getOutInterceptors();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Interceptors contributed by binding: " + i4);
        }
        List<Interceptor<? extends Message>> i5 = null;
        if (endpoint.getService().getDataBinding() instanceof InterceptorProvider) {
            i5 = ((InterceptorProvider)endpoint.getService().getDataBinding()).getOutInterceptors();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Interceptors contributed by databinding: " + i5);
            }
        }
        if (i5 != null) {
            return outboundChainCache.get(pm.getOutPhases(), i1, i2, i3, i4, i5);
        }
        return outboundChainCache.get(pm.getOutPhases(), i1, i2, i3, i4);
    }

    protected void modifyChain(InterceptorChain chain, Message ctx, boolean in) {
        if (ctx == null) {
            return;
        }
        Collection<InterceptorProvider> providers
            = CastUtils.cast((Collection<?>)ctx.get(Message.INTERCEPTOR_PROVIDERS));
        if (providers != null) {
            for (InterceptorProvider p : providers) {
                if (in) {
                    chain.add(p.getInInterceptors());
		    LOG.fine("modifyChain: Added input interceptors: " + p.getInInterceptors()); // Liberty Change
                } else {
                    chain.add(p.getOutInterceptors());
		    LOG.fine("modifyChain: Added output interceptors: " + p.getOutInterceptors()); // Liberty Change
                }
            }
        }
        String key = in ? Message.IN_INTERCEPTORS : Message.OUT_INTERCEPTORS;
        Collection<Interceptor<? extends Message>> is
            = CastUtils.cast((Collection<?>)ctx.get(key));
        if (is != null) {
            chain.add(is);
	    LOG.fine("modifyChain: Added interceptors: " + is); // Liberty Change
        }
    }

    protected void setEndpoint(Endpoint e) {
        getConduitSelector().setEndpoint(e);
    }

    public int getSynchronousTimeout() {
        return synchronousTimeout;
    }

    public void setSynchronousTimeout(int synchronousTimeout) {
        this.synchronousTimeout = synchronousTimeout;
    }

    public final ConduitSelector getConduitSelector() {
        return getConduitSelector(null);
    }

    protected final ConduitSelector getConduitSelector(
        ConduitSelector override
    ) {
        if (null == conduitSelector) {
            setConduitSelector(override);
        }
        return conduitSelector;
    }

    public final synchronized void setConduitSelector(ConduitSelector selector) {
        conduitSelector = selector == null ? new UpfrontConduitSelector() : selector;
    }

    private boolean isPartialResponse(Message in) {
        return Boolean.TRUE.equals(in.get(Message.PARTIAL_RESPONSE_MESSAGE));
    }

    @Override
    public void close() throws Exception {
        destroy();
    }


    public class EchoContext extends ConcurrentHashMap<String, Object> {
        private static final long serialVersionUID = 1L;
        public EchoContext(Map<String, Object> sharedMap) {
            super(8, 0.75f, 4);
            putAll(sharedMap);
        }

        public void reload() {
            super.clear();
            super.putAll(requestContext.get(latestContextThread));
        }
        
        @Override
        public void clear() {
            super.clear();
            try {
                for (Map.Entry<Thread, EchoContext> ent : requestContext.entrySet()) {
                    if (ent.getValue() == this) {
                        requestContext.remove(ent.getKey());
                        return;
                    }
                }
            } catch (Throwable t) {
		LOG.finest("Unexpected exception in clear(): " + t);  // Liberty Change
                //ignore
            }
        }
    }

    /**
     * Class to handle the response contexts.   The clear is overloaded to remove
     * this context from the threadLocal caches in the ClientImpl
     */
    @Trivial // Liberty Code Change
    static class ResponseContext implements Map<String, Object>, Serializable {
        private static final long serialVersionUID = 2L;
        final Map<String, Object> wrapped;
        final Map<Thread, ResponseContext> responseContext;
        
        ResponseContext(Map<String, Object> origMap, Map<Thread, ResponseContext> rc) {
            wrapped = origMap;
            responseContext = rc;
        }

        ResponseContext(Map<Thread, ResponseContext> rc) {
            wrapped = new HashMap<>();
            responseContext = rc;
        }

        @Override
        public void clear() {
            wrapped.clear();
            try {
                for (Map.Entry<Thread, ResponseContext> ent : responseContext.entrySet()) {
                    if (ent.getValue() == this) {
                        responseContext.remove(ent.getKey());
                        return;
                    }
                }
            } catch (Throwable t) {
		LOG.finest("Unexpected exception in clear(): " + t);  // Liberty Change
                //ignore
            }
        }

        @Override
        public int size() {
            return wrapped.size();
        }
        @Override
        public boolean isEmpty() {
            return wrapped.isEmpty();
        }
        @Override
        public boolean containsKey(Object key) {
            return wrapped.containsKey(key);
        }
        @Override
        public boolean containsValue(Object value) {
            return wrapped.containsKey(value);
        }
        @Override
        public Object get(Object key) {
            return wrapped.get(key);
        }
        @Override
        public Object put(String key, Object value) {
            return wrapped.put(key, value);
        }
        @Override
        public Object remove(Object key) {
            return wrapped.remove(key);
        }
        @Override
        public void putAll(Map<? extends String, ? extends Object> m) {
            wrapped.putAll(m);
        }
        @Override
        public Set<String> keySet() {
            return wrapped.keySet();
        }
        @Override
        public Collection<Object> values() {
            return wrapped.values();
        }
        @Override
        public Set<Entry<String, Object>> entrySet() {
            return wrapped.entrySet();
        }
    }

    public void setExecutor(Executor executor) {
        if (!SynchronousExecutor.isA(executor)) {
            this.executor = executor;
        }
    }

    
    public class IllegalEmptyResponseException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        public IllegalEmptyResponseException() {
            super();
        }

        public IllegalEmptyResponseException(String message) {
            super(message);
        }

        public IllegalEmptyResponseException(String message, Throwable cause) {
            super(message, cause);
        }

        public IllegalEmptyResponseException(Throwable cause) {
            super(cause);
        }
    }

}
