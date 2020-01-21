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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.ClientProxyImpl;
import org.apache.cxf.jaxrs.client.ClientState;
import org.apache.cxf.jaxrs.client.JaxrsClientCallback;
import org.apache.cxf.jaxrs.client.LocalClientState;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.microprofile.client.MPRestClientCallback;
import org.apache.cxf.microprofile.client.MicroProfileClientProviderFactory;
import org.apache.cxf.microprofile.client.cdi.CDIInterceptorWrapper;
import org.apache.cxf.microprofile.client.cdi.CDIFacade;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;


import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_CONNECTION_TIMEOUT_PROP;
import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_RECEIVE_TIMEOUT_PROP;

public class MicroProfileClientProxyImpl extends ClientProxyImpl {
    private static final Logger LOG = LogUtils.getL7dLogger(MicroProfileClientProxyImpl.class);

    private static final InvocationCallback<Object> NO_OP_CALLBACK = new InvocationCallback<Object>() {
        @Override
        public void failed(Throwable t) { }

        @Override
        public void completed(Object o) { }
    };

    private static final Method JAXRS_UTILS_GET_CURRENT_MESSAGE_METHOD = getJAXRSGetCurrentMessageMethod();
    
    //Liberty change start
    @FFDCIgnore({Throwable.class})
    private static Method getJAXRSGetCurrentMessageMethod() {
        Method m;
        try {
            Class<?> jaxrsUtilsClass = Class.forName("org.apache.cxf.jaxrs.utils.JAXRSUtils");
            m = ReflectionUtil.getDeclaredMethod(jaxrsUtilsClass, "getCurrentMessage");
        } catch (Throwable t) {
            // expected in non-JAX-RS server environments
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "Caught exception getting JAXRUtils class", t);
            }
            m = null;
        }
        return m;
    }
    //Liberty change end

    private final CDIInterceptorWrapper interceptorWrapper;
    private Object objectInstance;
    private Map<Class<ClientHeadersFactory>, ProviderInfo<ClientHeadersFactory>> clientHeaderFactories = new WeakHashMap<>(); //PreChange

    //CHECKSTYLE:OFF
    public MicroProfileClientProxyImpl(URI baseURI, ClassLoader loader, ClassResourceInfo cri,
                                       boolean isRoot, boolean inheritHeaders, ExecutorService executorService,
                                       Configuration configuration, CDIInterceptorWrapper interceptorWrapper, 
                                       Object... varValues) {
        super(new LocalClientState(baseURI), loader, cri, isRoot, inheritHeaders, varValues);
        this.interceptorWrapper = interceptorWrapper;
        init(executorService, configuration); //Liberty change
    }

    public MicroProfileClientProxyImpl(ClientState initialState, ClassLoader loader, ClassResourceInfo cri,
                                       boolean isRoot, boolean inheritHeaders, ExecutorService executorService,
                                       Configuration configuration, CDIInterceptorWrapper interceptorWrapper,
                                       Object... varValues) {
        super(initialState, loader, cri, isRoot, inheritHeaders, varValues);
        this.interceptorWrapper = interceptorWrapper;
        init(executorService, configuration); //Liberty change
    }
    //CHECKSTYLE:ON

    //Liberty change start
    private void init(ExecutorService executorService, Configuration configuration) {
        cfg.getRequestContext().put(EXECUTOR_SERVICE_PROPERTY, executorService);
        cfg.getRequestContext().putAll(configuration.getProperties());

        List<Interceptor<? extends Message>>inboundChain = cfg.getInInterceptors();
        inboundChain.add(new MPAsyncInvocationInterceptorPostAsyncImpl());
        inboundChain.add(new MPAsyncInvocationInterceptorRemoveContextImpl());
    }
    //Liberty change end

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
        MPAsyncInvocationInterceptorImpl aiiImpl = new MPAsyncInvocationInterceptorImpl(outMessage);
        outMessage.getInterceptorChain().add(aiiImpl);

        setTimeouts(cfg.getRequestContext());
        super.doInvokeAsync(ori, outMessage, asyncCallback);

        JaxrsClientCallback<?> cb = outMessage.getExchange().get(JaxrsClientCallback.class);

        return cb.createFuture();

    }

    @Override
    protected void doRunInterceptorChain(Message message) {
        setTimeouts(cfg.getRequestContext());
        super.doRunInterceptorChain(message);
    }

    @Override
    protected JaxrsClientCallback<?> newJaxrsClientCallback(InvocationCallback<Object> asyncCallback,
                                                            Message outMessage, //Liberty change
                                                            Class<?> responseClass,
                                                            Type outGenericType) {
        return new MPRestClientCallback<Object>(asyncCallback, outMessage /*Liberty change*/, responseClass, outGenericType);
    }

    @Override
    protected void checkResponse(Method m, Response r, Message inMessage) throws Throwable {
        MicroProfileClientProviderFactory factory = MicroProfileClientProviderFactory.getInstance(inMessage);

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

    @FFDCIgnore({Exception.class})
    protected void setTimeouts(Map<String, Object> props) {
        try {
            Long connectTimeout = getIntFromProps(props, HTTP_CONNECTION_TIMEOUT_PROP);
            Long readTimeout = getIntFromProps(props, HTTP_RECEIVE_TIMEOUT_PROP);
            if (connectTimeout > -1) {
                cfg.getHttpConduit().getClient().setConnectionTimeout(connectTimeout);
            }
            if (readTimeout > -1) {
                cfg.getHttpConduit().getClient().setReceiveTimeout(readTimeout);
            }
        } catch (Exception ex) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "Caught exception setting timeouts", ex);
            }
        }
    }

    @FFDCIgnore({NumberFormatException.class})
    private Long getIntFromProps(Map<String, Object> props, String key) {
        Object o = props.get(key);
        if (o == null) {
            return -1L; // not declared
        }
        Long l;
        if (o instanceof Long) {
            l = (Long) o;
        } else if (o instanceof String) {
            try {
                l = Long.parseLong((String)o);
            } catch (NumberFormatException ex) {
                LOG.log(Level.WARNING, "INVALID_TIMEOUT_PROPERTY", new Object[]{key, o});
                return -1L; // 
            }
        } else {
            LOG.log(Level.WARNING, "INVALID_TIMEOUT_PROPERTY", new Object[]{key, o});
            return -1L;
        }
        if (l < 0) {
            LOG.log(Level.WARNING, "INVALID_TIMEOUT_PROPERTY", new Object[]{key, o});
            return -1L;
        }
        return l;
    }

    @FFDCIgnore({ClassNotFoundException.class, NoSuchMethodException.class, NoSuchMethodException.class, Throwable.class})
    private String invokeBestFitComputeMethod(Class<?> clientIntf, ClientHeaderParam anno) throws Throwable {
        String methodName = anno.value()[0];
        methodName = methodName.substring(1, methodName.length() - 1);
        Class<?> computeClass = clientIntf;
        if (methodName.contains(".")) {
            String className = methodName.substring(0, methodName.lastIndexOf('.'));
            methodName = methodName.substring(methodName.lastIndexOf('.') + 1);
            try {
                computeClass = ClassLoaderUtils.loadClass(className, clientIntf);
            } catch (ClassNotFoundException ex) {
                LOG.warning("Cannot find specified computeValue class, " + className);
                return null;
            }
        }
        Method m = null;
        boolean includeHeaderName = false;
        try {
            m = computeClass.getMethod(methodName, String.class);
            includeHeaderName = true;
        } catch (NoSuchMethodException expected) {
            try {
                m = computeClass.getMethod(methodName);
            } catch (NoSuchMethodException expected2) { }
        }

        String value;
        if (m == null) {
            value = null;
            LOG.warning("Cannot find specified computeValue method, "
                + methodName + ", on client interface, " + clientIntf.getName());
        } else {
            try {
                Object valueFromComputeMethod;
                if (includeHeaderName) {
                    valueFromComputeMethod = m.invoke(computeClass == clientIntf ? objectInstance : null,
                                                      anno.name());
                } else {
                    valueFromComputeMethod = m.invoke(computeClass == clientIntf ? objectInstance : null);
                }
                if (valueFromComputeMethod instanceof String[]) {
                    value = HttpUtils.getHeaderString(Arrays.asList((String[]) valueFromComputeMethod));
                } else {
                    value = (String) valueFromComputeMethod;
                }
            } catch (Throwable t) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, "Caught exception invoking compute method", t);
                }
                if (t instanceof InvocationTargetException) {
                    t = t.getCause();
                }
                throw t;
            }
        }
        return value;
    }

    @FFDCIgnore({Throwable.class})
    private Parameter createClientHeaderParameter(ClientHeaderParam anno, Class<?> clientIntf) {
        Parameter p = new Parameter(ParameterType.HEADER, anno.name());
        String[] values = anno.value();
        String headerValue;
        if (values[0] != null && values[0].length() > 2 && values[0].startsWith("{") && values[0].endsWith("}")) {
            try {
                headerValue = invokeBestFitComputeMethod(clientIntf, anno);
            } catch (Throwable t) {
                if (anno.required()) {
                    throwException(t);
                }
                return null;
            }
        } else {
            headerValue = HttpUtils.getHeaderString(Arrays.asList(values));
        }

        p.setDefaultValue(headerValue);
        return p;
    }

    @FFDCIgnore({Throwable.class})
    @Override
    @SuppressWarnings("unchecked")
    protected void handleHeaders(Method m,
                                 Object[] params,
                                 MultivaluedMap<String, String> headers,
                                 List<Parameter> beanParams,
                                 MultivaluedMap<ParameterType, Parameter> map) {
        super.handleHeaders(m, params, headers, beanParams, map);

        try {
            Class<?> declaringClass = m.getDeclaringClass();
            ClientHeaderParam[] clientHeaderAnnosOnInterface = declaringClass
                .getAnnotationsByType(ClientHeaderParam.class);
            ClientHeaderParam[] clientHeaderAnnosOnMethod = m.getAnnotationsByType(ClientHeaderParam.class);
            RegisterClientHeaders headersFactoryAnno = declaringClass.getAnnotation(RegisterClientHeaders.class);
            if (clientHeaderAnnosOnInterface.length < 1 && clientHeaderAnnosOnMethod.length < 1
                && headersFactoryAnno == null) {
                return;
            }
            
            for (ClientHeaderParam methodAnno : clientHeaderAnnosOnMethod) {
                String headerName = methodAnno.name();
                if (!headers.containsKey(headerName)) {
                    Parameter p = createClientHeaderParameter(methodAnno, declaringClass);
                    if (p != null) {
                        headers.putSingle(p.getName(), p.getDefaultValue());
                    }
                }
            }
            for (ClientHeaderParam intfAnno : clientHeaderAnnosOnInterface) {
                String headerName = intfAnno.name();
                if (!headers.containsKey(headerName)) {
                    Parameter p = createClientHeaderParameter(intfAnno, declaringClass);
                    if (p != null) {
                        headers.putSingle(p.getName(), p.getDefaultValue());
                    }
                }
            }

            if (headersFactoryAnno != null) {
                Class<ClientHeadersFactory> headersFactoryClass = 
                                (Class<ClientHeadersFactory>) headersFactoryAnno.value();
                mergeHeaders(headersFactoryClass, headers);
            }
        } catch (Throwable t) {
            throwException(t);
        }
    }

    @FFDCIgnore(Throwable.class)
    private void mergeHeaders(Class<ClientHeadersFactory> factoryCls, MultivaluedMap<String, String> existingHeaders) {

        try {
            ClientHeadersFactory factory = CDIFacade.getInstanceFromCDI(factoryCls).orElse(factoryCls.newInstance());

            MultivaluedMap<String, String> jaxrsHeaders;
            if (JAXRS_UTILS_GET_CURRENT_MESSAGE_METHOD != null) {
                Message m = (Message) JAXRS_UTILS_GET_CURRENT_MESSAGE_METHOD.invoke(null);
                if (m != null) {
                    ProviderInfo<ClientHeadersFactory> pi = clientHeaderFactories.computeIfAbsent(factoryCls, k -> {
                        return new ProviderInfo<ClientHeadersFactory>(factory, m.getExchange().getBus(), true);
                    });
                    InjectionUtils.injectContexts(factory, pi, m);
                }
                jaxrsHeaders = getJaxrsHeaders(m);
            } else {
                jaxrsHeaders = new MultivaluedHashMap<>();
            }

            MultivaluedMap<String, String> updatedHeaders = factory.update(jaxrsHeaders, existingHeaders);
            existingHeaders.putAll(updatedHeaders);
        } catch (Throwable t) {
            // expected if not running in a JAX-RS server environment.
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "Caught exception getting JAX-RS incoming headers", t);
            }
        }
    }

    @Trivial
    @Override
    public Object invoke(Object o, Method m, Object[] params) throws Throwable {
        checkClosed();
        objectInstance = o;
        return interceptorWrapper.invoke(o, m, params, new Invoker(o, m, params, this));
    }

    @Trivial
    private Object invokeActual(Object o, Method m, Object[] params) throws Throwable {
        return super.invoke(o, m, params);
    }

    private static class Invoker implements Callable<Object> {
        private final Object targetObject;
        private final Method method;
        private final Object[] params;
        private final MicroProfileClientProxyImpl proxy;

        @Trivial
        Invoker(Object o, Method m, Object[] params, MicroProfileClientProxyImpl proxy) {
            this.targetObject = o;
            this.method = m;
            this.params = params;
            this.proxy = proxy;
        }

        @FFDCIgnore({Throwable.class})
        @Trivial
        @Override
        public Object call() throws Exception {
            try {
                return proxy.invokeActual(targetObject, method, params);
            } catch (Throwable t) {
                if (t instanceof Exception) {
                    throw (Exception) t;
                }
                throw new RuntimeException(t);
            }
        }
    }
    
    private void throwException(Throwable t) {
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        throw new RuntimeException(t);
    }

    private static MultivaluedMap<String, String> getJaxrsHeaders(Message m) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        if (m != null) {
            headers.putAll(CastUtils.cast((Map<?, ?>) m.get(Message.PROTOCOL_HEADERS)));
        }
        return headers;
    }
}
