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
package org.apache.cxf.microprofile.client.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.ClientProxyImpl;
import org.apache.cxf.jaxrs.client.ClientState;
import org.apache.cxf.jaxrs.client.JaxrsClientCallback;
import org.apache.cxf.jaxrs.client.LocalClientState;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.microprofile.client.MPRestClientCallback;
import org.apache.cxf.microprofile.client.MicroProfileClientProviderFactory;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class MicroProfileClientProxyImpl extends ClientProxyImpl {

    private static final InvocationCallback<Object> NO_OP_CALLBACK = new InvocationCallback<Object>() {
        @Override
        public void failed(Throwable t) { }

        @Override
        public void completed(Object o) { }
    };

    private final MPAsyncInvocationInterceptorImpl aiiImpl = new MPAsyncInvocationInterceptorImpl();

    public MicroProfileClientProxyImpl(URI baseURI, ClassLoader loader, ClassResourceInfo cri,
                                       boolean isRoot, boolean inheritHeaders, ExecutorService executorService,
                                       Configuration configuration, Object... varValues) {
        super(new LocalClientState(baseURI), loader, cri, isRoot, inheritHeaders, varValues);
        cfg.getRequestContext().put(EXECUTOR_SERVICE_PROPERTY, executorService);
        cfg.getRequestContext().putAll(configuration.getProperties());
    }

    public MicroProfileClientProxyImpl(ClientState initialState, ClassLoader loader, ClassResourceInfo cri,
                                       boolean isRoot, boolean inheritHeaders, ExecutorService executorService,
                                       Configuration configuration, Object... varValues) {
        super(initialState, loader, cri, isRoot, inheritHeaders, varValues);
        cfg.getRequestContext().put(EXECUTOR_SERVICE_PROPERTY, executorService);
        cfg.getRequestContext().putAll(configuration.getProperties());
    }



    @SuppressWarnings("unchecked")
    @Override
    protected InvocationCallback<Object> checkAsyncCallback(OperationResourceInfo ori,
                                                            Map<String, Object> reqContext,
                                                            Message outMessage) {
        InvocationCallback<Object> callback = outMessage.getContent(InvocationCallback.class);
        if (callback == null && CompletionStage.class.equals(ori.getMethodToInvoke().getReturnType())) {
            callback = NO_OP_CALLBACK;
            outMessage.setContent(InvocationCallback.class, callback);
        }
        return callback;
    }

    protected boolean checkAsyncReturnType(OperationResourceInfo ori,
                                           Map<String, Object> reqContext,
                                           Message outMessage) {
        return CompletionStage.class.equals(ori.getMethodToInvoke().getReturnType());
    }

    @Override
    protected Object doInvokeAsync(OperationResourceInfo ori, Message outMessage,
                                   InvocationCallback<Object> asyncCallback) {
        outMessage.getInterceptorChain().add(aiiImpl);
        MPAsyncInvocationInterceptorPostAsyncImpl asyncInterceptorImpl = 
            new MPAsyncInvocationInterceptorPostAsyncImpl(aiiImpl.getInterceptors());
        try {
            cfg.getInInterceptors().add(asyncInterceptorImpl);

            super.doInvokeAsync(ori, outMessage, asyncCallback);

            JaxrsClientCallback<?> cb = outMessage.getExchange().get(JaxrsClientCallback.class);

            return cb.createFuture();
        } finally {
            cfg.getInInterceptors().remove(asyncInterceptorImpl);
        }
    }

    @Override
    protected JaxrsClientCallback<?> newJaxrsClientCallback(InvocationCallback<Object> asyncCallback,
                                                            Message outMessage,
                                                            Class<?> responseClass,
                                                            Type outGenericType) {
        return new MPRestClientCallback<Object>(asyncCallback, outMessage, responseClass, outGenericType);
    }

    @Override
    protected void checkResponse(Method m, Response r, Message inMessage) throws Throwable {
        MicroProfileClientProviderFactory factory = MicroProfileClientProviderFactory.getInstance(inMessage);
        if (r.getLocation() == null) {
            r.getMetadata().putSingle(HttpHeaders.LOCATION, URI.create((String)inMessage.get(Message.REQUEST_URI)));
        }
        List<ResponseExceptionMapper<?>> mappers = factory.createResponseExceptionMapper(inMessage,
                Throwable.class);
        for (ResponseExceptionMapper<?> mapper : mappers) {
            if (mapper.handles(r.getStatus(), r.getHeaders())) {
                Throwable t = mapper.toThrowable(r);
                if (t instanceof RuntimeException) {
                    throw t;
                } else if (t != null && m.getExceptionTypes() != null) {
                    // its a checked exception, make sure its declared
                    for (Class<?> c : m.getExceptionTypes()) {
                        if (t.getClass().isAssignableFrom(c)) {
                            throw t;
                        }
                    }
                    // TODO Log the unhandled declarable
                }
            }
        }
    }

    @Override
    protected Class<?> getReturnType(Method method, Message outMessage) {
        Class<?> returnType = super.getReturnType(method, outMessage);
        if (CompletionStage.class.isAssignableFrom(returnType)) {
            Type t = method.getGenericReturnType();
            returnType = InjectionUtils.getActualType(t);
        }
        return returnType;
    }

    @Override
    protected Message createMessage(Object body,
                                    OperationResourceInfo ori,
                                    MultivaluedMap<String, String> headers,
                                    URI currentURI,
                                    Exchange exchange,
                                    Map<String, Object> invocationContext,
                                    boolean proxy) {

        Method m = ori.getMethodToInvoke();

        Message msg = super.createMessage(body, ori, headers, currentURI, exchange, invocationContext, proxy);

        @SuppressWarnings("unchecked")
        Map<String, Object> filterProps = (Map<String, Object>) msg.getExchange()
                                                                   .get("jaxrs.filter.properties");
        if (filterProps == null) {
            filterProps = new HashMap<>();
            msg.getExchange().put("jaxrs.filter.properties", filterProps);
        }
        filterProps.put("org.eclipse.microprofile.rest.client.invokedMethod", m);
        return msg;
    }
}
