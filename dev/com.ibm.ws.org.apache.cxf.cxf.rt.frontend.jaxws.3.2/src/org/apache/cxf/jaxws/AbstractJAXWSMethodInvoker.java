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

package org.apache.cxf.jaxws;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Provider;
import javax.xml.ws.Response;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.annotations.UseAsyncMethod;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.invoker.Factory;
import org.apache.cxf.service.invoker.FactoryInvoker;
import org.apache.cxf.service.invoker.SingletonFactory;
import org.apache.cxf.service.model.BindingOperationInfo;

public abstract class AbstractJAXWSMethodInvoker extends FactoryInvoker {
    private static final String ASYNC_METHOD = "org.apache.cxf.jaxws.async.method";
    private static final String PARTIAL_RESPONSE_SENT_PROPERTY =
        "org.apache.cxf.ws.addressing.partial.response.sent";

    public AbstractJAXWSMethodInvoker(final Object bean) {
        super(new SingletonFactory(bean));
    }

    public AbstractJAXWSMethodInvoker(Factory factory) {
        super(factory);
    }

    protected SOAPFaultException findSoapFaultException(Throwable ex) {
        if (ex instanceof SOAPFaultException) {
            return (SOAPFaultException)ex;
        }
        if (ex.getCause() != null) {
            return findSoapFaultException(ex.getCause());
        }
        return null;
    }

    @Override
    protected Method adjustMethodAndParams(Method mOriginal, Exchange ex, List<Object> params,
                                           Class<?> serviceObjectClass) {
        // If class implements Provider<T> interface, use overriden method from service object class
        // to check UseAsyncMethod annotation
        Method mso = getProviderServiceObjectMethod(mOriginal, serviceObjectClass);

        UseAsyncMethod uam = mso.getAnnotation(UseAsyncMethod.class);
        if (uam != null) {
            BindingOperationInfo bop = ex.getBindingOperationInfo();
            Method ret = bop.getProperty(ASYNC_METHOD, Method.class);
            if (ret == null) {
                Class<?>[] ptypes = new Class<?>[mso.getParameterTypes().length + 1];
                System.arraycopy(mso.getParameterTypes(), 0, ptypes, 0, mso.getParameterTypes().length);
                ptypes[mso.getParameterTypes().length] = AsyncHandler.class;
                try {
                    ret = mso.getDeclaringClass().getMethod(mso.getName() + "Async", ptypes);
                    bop.setProperty(ASYNC_METHOD, ret);
                } catch (Throwable t) {
                    //ignore
                }
            }
            if (ret != null) {
                JaxwsServerHandler h = ex.get(JaxwsServerHandler.class);
                if (h != null) {
                    return ret;
                }
                ContinuationProvider cp = ex.getInMessage().get(ContinuationProvider.class);
                // Check for decoupled endpoints: if partial response already was sent, ignore continuation
                boolean decoupledEndpoints = MessageUtils
                    .getContextualBoolean(ex.getInMessage(), PARTIAL_RESPONSE_SENT_PROPERTY, false);
                if ((cp == null) && uam.always() || decoupledEndpoints) {
                    JaxwsServerHandler handler = new JaxwsServerHandler(null);
                    ex.put(JaxwsServerHandler.class, handler);
                    params.add(handler);
                    return ret;
                } else if (cp != null && cp.getContinuation() != null) {
                    final Continuation c = cp.getContinuation();
                    c.suspend(0);
                    JaxwsServerHandler handler = new JaxwsServerHandler(c);
                    ex.put(JaxwsServerHandler.class, handler);
                    params.add(handler);
                    return ret;
                }
            }
        }
        return mOriginal;
    }

    private Method getProviderServiceObjectMethod(Method m, Class<?> serviceObjectClass) {
        if (!Provider.class.isAssignableFrom(serviceObjectClass)) {
            return m;
        }
        Class<?> currentSvcClass = serviceObjectClass;
        Class<?> genericType = null;

        while (currentSvcClass != null) {
            genericType = getProviderGenericType(currentSvcClass);
            if (genericType != null) {
                break;
            }
            // Check superclass until top
            currentSvcClass = currentSvcClass.getSuperclass();
        }
        // Should never happens
        if (genericType == null) {
            return m;
        }
        try {
            return serviceObjectClass.getMethod("invoke", genericType);
        } catch (Exception e) {
            throw new ServiceConstructionException(e);
        }
    }

    private Class<?> getProviderGenericType(Class<?> svcClass) {
        Type[] interfaces = svcClass.getGenericInterfaces();
        for (Type interfaceType : interfaces) {
            if (interfaceType instanceof ParameterizedType) {
                ParameterizedType paramInterface = (ParameterizedType)interfaceType;
                if (!paramInterface.getRawType().equals(Provider.class)) {
                    continue;
                }
                Type[] typeArgs = paramInterface.getActualTypeArguments();
                if (typeArgs.length > 0) {
                    return (Class<?>)typeArgs[0];
                }
            }
        }
        return null;
    }

    class JaxwsServerHandler implements AsyncHandler<Object> {
        Response<Object> r;
        Continuation continuation;
        boolean done;

        JaxwsServerHandler(Continuation c) {
            continuation = c;
        }

        public synchronized void handleResponse(Response<Object> res) {
            r = res;
            done = true;
            if (continuation != null) {
                continuation.resume();
            }
            notifyAll();
        }
        public boolean hasContinuation() {
            return continuation != null;
        }
        public synchronized void waitForDone() {
            while (!done) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        }
        public boolean isDone() {
            return done;
        }
        public Object getObject() throws Exception {
            return r.get();
        }
    }

    @Override
    protected Object invoke(Exchange exchange,
                            final Object serviceObject, Method m,
                            List<Object> params) {
        JaxwsServerHandler h = exchange.get(JaxwsServerHandler.class);
        if (h != null && h.isDone()) {
            BindingOperationInfo bop = exchange.getBindingOperationInfo();
            if (bop.isUnwrapped()) {
                exchange.put(BindingOperationInfo.class, bop.getWrappedOperation());
            }
            try {
                return new MessageContentsList(h.getObject());
            } catch (ExecutionException ex) {
                exchange.getInMessage().put(FaultMode.class,
                                            FaultMode.CHECKED_APPLICATION_FAULT);
                throw createFault(ex.getCause(), m, params, true);
            } catch (Exception ex) {
                throw createFault(ex.getCause(), m, params, false);
            }
        }
        Object o = super.invoke(exchange, serviceObject, m, params);
        if (h != null && !h.hasContinuation()) {
            h.waitForDone();
            BindingOperationInfo bop = exchange.getBindingOperationInfo();
            if (bop.isUnwrapped()) {
                exchange.put(BindingOperationInfo.class, bop.getWrappedOperation());
            }
            try {
                return new MessageContentsList(h.getObject());
            } catch (ExecutionException ex) {
                exchange.getInMessage().put(FaultMode.class,
                                            FaultMode.CHECKED_APPLICATION_FAULT);
                throw createFault(ex.getCause(), m, params, true);
            } catch (Exception ex) {
                throw createFault(ex.getCause(), m, params, false);
            }
        }
        return o;
    }

    @Override
    protected Fault createFault(Throwable ex, Method m, List<Object> params, boolean checked) {
        //map the JAX-WS faults
        SOAPFaultException sfe = findSoapFaultException(ex);
        if (sfe != null) {
            SoapFault fault = new SoapFault(sfe.getFault().getFaultString(),
                                            ex,
                                            sfe.getFault().getFaultCodeAsQName());
            fault.setRole(sfe.getFault().getFaultActor());
            if (sfe.getFault().hasDetail()) {
                fault.setDetail(sfe.getFault().getDetail());
            }

            return fault;
        }
        return super.createFault(ex, m, params, checked);
    }

    protected Map<String, Object> removeHandlerProperties(WrappedMessageContext ctx) {
        Map<String, Scope> scopes = CastUtils.cast((Map<?, ?>)ctx.get(WrappedMessageContext.SCOPES));
        Map<String, Object> handlerScopedStuff = new HashMap<>();
        if (scopes != null) {
            for (Map.Entry<String, Scope> scope : scopes.entrySet()) {
                if (scope.getValue() == Scope.HANDLER) {
                    handlerScopedStuff.put(scope.getKey(), ctx.get(scope.getKey()));
                }
            }
            for (String key : handlerScopedStuff.keySet()) {
                ctx.remove(key);
            }
        }
        return handlerScopedStuff;
    }

    protected void addHandlerProperties(WrappedMessageContext ctx,
                                        Map<String, Object> handlerScopedStuff) {
        for (Map.Entry<String, Object> key : handlerScopedStuff.entrySet()) {
            ctx.put(key.getKey(), key.getValue(), Scope.HANDLER);
        }
    }

    private Message createResponseMessage(Exchange exchange) {
        if (exchange == null) {
            return null;
        }
        Message m = exchange.getOutMessage();
        if (m == null && !exchange.isOneWay()) {
            Endpoint ep = exchange.getEndpoint();
            m = new MessageImpl();
            m.setExchange(exchange);
            m = ep.getBinding().createMessage(m);
            exchange.setOutMessage(m);
        }
        return m;
    }

    protected void updateWebServiceContext(Exchange exchange, MessageContext ctx) {
        // Guard against wrong type associated with header list.
        // Need to copy header only if the message is going out.
        if (ctx.containsKey(Header.HEADER_LIST)
                && ctx.get(Header.HEADER_LIST) instanceof List<?>) {
            List<?> list = (List<?>) ctx.get(Header.HEADER_LIST);
            if (list != null && !list.isEmpty()) {
                SoapMessage sm = (SoapMessage) createResponseMessage(exchange);
                if (sm != null) {
                    Iterator<?> iter = list.iterator();
                    while (iter.hasNext()) {
                        sm.getHeaders().add((Header) iter.next());
                    }
                }
            }
        }
        if (exchange.getOutMessage() != null) {
            Message out = exchange.getOutMessage();
            if (out.containsKey(Message.PROTOCOL_HEADERS)) {
                Map<String, List<String>> heads = CastUtils
                    .cast((Map<?, ?>)exchange.getOutMessage().get(Message.PROTOCOL_HEADERS));
                if (heads.containsKey("Content-Type")) {
                    List<String> ct = heads.get("Content-Type");
                    exchange.getOutMessage().put(Message.CONTENT_TYPE, ct.get(0));
                    heads.remove("Content-Type");
                }
            }
        }
    }

    protected void updateHeader(Exchange exchange, MessageContext ctx) {
        if (ctx.containsKey(Header.HEADER_LIST)
                && ctx.get(Header.HEADER_LIST) instanceof List<?>) {
            List<?> list = (List<?>) ctx.get(Header.HEADER_LIST);
            if (list != null && !list.isEmpty()) {
                SoapMessage sm = (SoapMessage) createResponseMessage(exchange);
                if (sm != null) {
                    Iterator<?> iter = list.iterator();
                    while (iter.hasNext()) {
                        Header header = (Header) iter.next();
                        if (header.getDirection() != Header.Direction.DIRECTION_IN
                            && !header.getName().getNamespaceURI().
                                equals("http://docs.oasis-open.org/wss/2004/01/"
                                        + "oasis-200401-wss-wssecurity-secext-1.0.xsd")
                                   && !header.getName().getNamespaceURI().
                                       equals("http://docs.oasis-open.org/"
                                              + "wss/oasis-wss-wssecurity-secext-1.1.xsd")) {
                            //don't copy over security header, out interceptor chain will take care of it.
                            sm.getHeaders().add(header);
                        }
                    }
                }
            }
        }
    }

}