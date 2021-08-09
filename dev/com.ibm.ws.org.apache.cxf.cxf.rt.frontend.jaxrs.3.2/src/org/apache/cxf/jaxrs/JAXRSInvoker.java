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

package org.apache.cxf.jaxrs;


import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain.State;
import org.apache.cxf.jaxrs.impl.AsyncResponseImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.ResourceContextImpl;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.invoker.AbstractInvoker;

public class JAXRSInvoker extends AbstractInvoker {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSInvoker.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSInvoker.class);
    private static final String SERVICE_LOADER_AS_CONTEXT = "org.apache.cxf.serviceloader-context";
    private static final String SERVICE_OBJECT_SCOPE = "org.apache.cxf.service.scope";
    private static final String REQUEST_SCOPE = "request";
    private static final String LAST_SERVICE_OBJECT = "org.apache.cxf.service.object.last";
    private static final String PROXY_INVOCATION_ERROR_FRAGMENT
        = "object is not an instance of declaring class";

    public JAXRSInvoker() {
    }

    @FFDCIgnore({Throwable.class, WebApplicationException.class})
    public Object invoke(Exchange exchange, Object request) {
        MessageContentsList responseList = checkExchangeForResponse(exchange);
        if (responseList != null) {
            return responseList;
        }
        AsyncResponse asyncResp = exchange.get(AsyncResponse.class);
        if (asyncResp != null) {
            AsyncResponseImpl asyncImpl = (AsyncResponseImpl)asyncResp;
            asyncImpl.prepareContinuation();
            try {
                asyncImpl.handleTimeout();
                return handleAsyncResponse(exchange, asyncImpl);
            } catch (Throwable t) {
                return handleAsyncFault(exchange, asyncImpl, t);
            }
        }

        ResourceProvider provider = getResourceProvider(exchange);
        Object rootInstance = null;
        Message inMessage = exchange.getInMessage();
        try {
            rootInstance = getServiceObject(exchange);
            Object serviceObject = getActualServiceObject(exchange, rootInstance);

            return invoke(exchange, request, serviceObject);
        } catch (WebApplicationException ex) {
            responseList = checkExchangeForResponse(exchange);
            if (responseList != null) {
                return responseList;
            }
            return handleFault(ex, inMessage);
        } finally {
            boolean suspended = isSuspended(exchange);
            if (suspended || exchange.isOneWay() || inMessage.get(Message.THREAD_CONTEXT_SWITCHED) != null) {
                ServerProviderFactory.clearThreadLocalProxies(inMessage);
            }
            if (suspended || isServiceObjectRequestScope(inMessage)) {
                persistRoots(exchange, rootInstance, provider);
            } else {
                provider.releaseInstance(inMessage, rootInstance);
            }
        }
    }

    private boolean isSuspended(Exchange exchange) {
        return exchange.getInMessage().getInterceptorChain().getState() == State.SUSPENDED;
    }

    private Object handleAsyncResponse(Exchange exchange, AsyncResponseImpl ar) {
        Object asyncObj = ar.getResponseObject();
        if (asyncObj instanceof Throwable) {
            final Throwable throwable = (Throwable)asyncObj;
            Throwable cause = throwable;

            if (throwable instanceof CompletionException) {
                cause = throwable.getCause();
            }

            return handleAsyncFault(exchange, ar, (cause != null) ? cause : throwable);
        }
        setResponseContentTypeIfNeeded(exchange.getInMessage(), asyncObj);
        return new MessageContentsList(asyncObj);
    }

    @FFDCIgnore(Fault.class)
    private Object handleAsyncFault(Exchange exchange, AsyncResponseImpl ar, Throwable t) {
        try {
            return handleFault(new Fault(t), exchange.getInMessage(), null, null);
        } catch (Fault ex) {
            ar.setUnmappedThrowable(ex.getCause() == null ? ex : ex.getCause());
            if (isSuspended(exchange)) {
                ar.reset();
                exchange.getInMessage().getInterceptorChain().unpause();
            }
            return new MessageContentsList(Response.serverError().build());
        }
    }

    private void persistRoots(Exchange exchange, Object rootInstance, Object provider) {
        exchange.put(JAXRSUtils.ROOT_INSTANCE, rootInstance);
        exchange.put(JAXRSUtils.ROOT_PROVIDER, provider);
    }

    @FFDCIgnore({Fault.class, IOException.class, WebApplicationException.class})
    @SuppressWarnings("unchecked")
    public Object invoke(Exchange exchange, Object request, Object resourceObject) {

        final OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
        final ClassResourceInfo cri = ori.getClassResourceInfo();
        final Message inMessage = exchange.getInMessage();
        final ServerProviderFactory providerFactory = ServerProviderFactory.getInstance(inMessage);
        cri.injectContexts(resourceObject, ori, inMessage);

        if (cri.isRoot()) {
            ProviderInfo<Application> appProvider = providerFactory.getApplicationProvider();
            if (appProvider != null) {
                InjectionUtils.injectContexts(appProvider.getProvider(),
                                              appProvider,
                                              inMessage);
            }
        }


        Method methodToInvoke = getMethodToInvoke(cri, ori, resourceObject);

        List<Object> params = null;
        if (request instanceof List) {
            params = CastUtils.cast((List<?>)request);
        } else if (request != null) {
            params = new MessageContentsList(request);
        }

        params = reprocessFormParams(methodToInvoke, params, inMessage); //Liberty change - CXF-7860

        Object result = null;
        ClassLoaderHolder contextLoader = null;
        AsyncResponseImpl asyncResponse = null;
        try {
            if (setServiceLoaderAsContextLoader(inMessage)) {
                contextLoader = ClassLoaderUtils
                    .setThreadContextClassloader(resourceObject.getClass().getClassLoader());
            }
            if (!ori.isSubResourceLocator()) {
                asyncResponse = (AsyncResponseImpl)inMessage.get(AsyncResponse.class);
            }
            result = invoke(exchange, resourceObject, methodToInvoke, params);
            if (asyncResponse == null && !ori.isSubResourceLocator()) {
                asyncResponse = checkFutureResponse(inMessage, checkResultObject(result));
            }
            if (asyncResponse != null) {
                if (!asyncResponse.suspendContinuationIfNeeded()) {
                    result = handleAsyncResponse(exchange, asyncResponse);
                } else {
                    providerFactory.clearThreadLocalProxies();
                }
            }
        } catch (Fault ex) {
            Object faultResponse;
            if (asyncResponse != null) {
                faultResponse = handleAsyncFault(exchange, asyncResponse,
                                                 ex.getCause() == null ? ex : ex.getCause());
            } else {
                faultResponse = handleFault(ex, inMessage, cri, methodToInvoke);
            }
            return faultResponse;
        } finally {
            exchange.put(LAST_SERVICE_OBJECT, resourceObject);
            if (contextLoader != null) {
                contextLoader.reset();
            }
        }
        ClassResourceInfo subCri = null;
        if (ori.isSubResourceLocator()) {
            try {
                MultivaluedMap<String, String> values = getTemplateValues(inMessage);
                String subResourcePath = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
                String httpMethod = (String)inMessage.get(Message.HTTP_REQUEST_METHOD);
                String contentType = (String)inMessage.get(Message.CONTENT_TYPE);
                if (contentType == null) {
                    contentType = "*/*";
                }
                List<MediaType> acceptContentType =
                    (List<MediaType>)exchange.get(Message.ACCEPT_CONTENT_TYPE);

                result = checkSubResultObject(result, subResourcePath);

                Class<?> subResponseType = null;
                if (result.getClass() == Class.class) {
                    ResourceContext rc = new ResourceContextImpl(inMessage, ori);
                    result = rc.getResource((Class<?>)result);
                    subResponseType = InjectionUtils.getActualType(methodToInvoke.getGenericReturnType());
                } else {
                    subResponseType = methodToInvoke.getReturnType();
                }
                
                subCri = cri.getSubResource(subResponseType,
                    ClassHelper.getRealClass(exchange.getBus(), result), result);
                if (subCri == null) {
                    org.apache.cxf.common.i18n.Message errorM =
                        new org.apache.cxf.common.i18n.Message("NO_SUBRESOURCE_FOUND",
                                                               BUNDLE,
                                                               subResourcePath);
                    LOG.severe(errorM.toString());
                    throw ExceptionUtils.toNotFoundException(null, null);
                }

                OperationResourceInfo subOri = JAXRSUtils.findTargetMethod(
                                                         Collections.singletonMap(subCri, values),
                                                         inMessage,
                                                         httpMethod,
                                                         values,
                                                         contentType,
                                                         acceptContentType);
                exchange.put(OperationResourceInfo.class, subOri);
                inMessage.put(URITemplate.TEMPLATE_PARAMETERS, values);
                inMessage.put(URITemplate.URI_TEMPLATE, JAXRSUtils.getUriTemplate(inMessage, subCri, ori, subOri));

                if (!subOri.isSubResourceLocator()
                    && JAXRSUtils.runContainerRequestFilters(providerFactory,
                                                             inMessage,
                                                             false,
                                                             subOri.getNameBindings())) {
                    return new MessageContentsList(exchange.get(Response.class));
                }

                // work out request parameters for the sub-resource class. Here we
                // presume InputStream has not been consumed yet by the root resource class.
                List<Object> newParams = JAXRSUtils.processParameters(subOri, values, inMessage);
                inMessage.setContent(List.class, newParams);

                return this.invoke(exchange, newParams, result);
            } catch (IOException ex) {
                Response resp = JAXRSUtils.convertFaultToResponse(ex, inMessage);
                if (resp == null) {
                    resp = JAXRSUtils.convertFaultToResponse(ex, inMessage);
                }
                return new MessageContentsList(resp);
            } catch (WebApplicationException ex) {
                Response excResponse;
                if (JAXRSUtils.noResourceMethodForOptions(ex.getResponse(),
                        (String)inMessage.get(Message.HTTP_REQUEST_METHOD))) {
                    excResponse = JAXRSUtils.createResponse(Collections.singletonList(subCri),
                                                            null, null, 200, true);
                } else {
                    excResponse = JAXRSUtils.convertFaultToResponse(ex, inMessage);
                }
                return new MessageContentsList(excResponse);
            }
        }
        setResponseContentTypeIfNeeded(inMessage, result);
        return result;
    }

    // Liberty change start - CXF-7860
    private List<Object> reprocessFormParams(Method method, List<Object> origParams, Message m) {
        Form form = null;
        boolean hasFormParamAnnotations = false;
        Object[] newValues = new Object[origParams.size()];
        java.lang.reflect.Parameter[] methodParams = method.getParameters();
        for (int i = 0; i < methodParams.length; i++) {
            if (Form.class.equals(methodParams[i].getType())) {
                form = (Form) origParams.get(i);
            }
            if (methodParams[i].getAnnotation(FormParam.class) != null) {
                hasFormParamAnnotations = true;
            } else {
                newValues[i] = origParams.get(i);
            }
        }

        if (!hasFormParamAnnotations || form == null) {
            return origParams;
        }

        for (int i = 0; i < newValues.length; i++) {
            if (newValues[i] == null) {
                String formFieldName = methodParams[i].getAnnotation(FormParam.class).value();
                List<String> values = form.asMap().get(formFieldName);
                newValues[i] = InjectionUtils.createParameterObject(values, 
                                                                    methodParams[i].getType(),
                                                                    methodParams[i].getParameterizedType(),
                                                                    methodParams[i].getAnnotations(),
                                                                    (String) origParams.get(i),
                                                                    false,
                                                                    ParameterType.FORM,
                                                                    m);
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, "replacing @FormParam value of {0} with {1}",
                        new Object[]{origParams.get(i), newValues[i]});
                }
            }
        }
        return Arrays.asList(newValues);
    }
    //Liberty change end

    protected AsyncResponseImpl checkFutureResponse(Message inMessage, Object result) {
        if (result instanceof CompletionStage) {
            final CompletionStage<?> stage = (CompletionStage<?>)result;
            final AsyncResponseImpl asyncResponse = new AsyncResponseImpl(inMessage);
            stage.whenComplete((v, t) -> {
                if (t instanceof CancellationException) {
                    asyncResponse.cancel();
                } else {
                    asyncResponse.resume(v != null ? v : t);
                }
            });
            return asyncResponse;
        }
        return null;
    }

    protected Method getMethodToInvoke(ClassResourceInfo cri, OperationResourceInfo ori, Object resourceObject) {
        Method resourceMethod = cri.getMethodDispatcher().getMethod(ori);

        Method methodToInvoke = null;
        if (Proxy.class.isInstance(resourceObject)) {
            methodToInvoke = cri.getMethodDispatcher().getProxyMethod(resourceMethod);
            if (methodToInvoke == null) {
                methodToInvoke = InjectionUtils.checkProxy(resourceMethod, resourceObject);
                cri.getMethodDispatcher().addProxyMethod(resourceMethod, methodToInvoke);
            }
        } else {
            methodToInvoke = resourceMethod;
        }
        return methodToInvoke;
    }

    private MessageContentsList checkExchangeForResponse(Exchange exchange) {
        Response r = exchange.get(Response.class);
        if (r != null) {
            JAXRSUtils.setMessageContentType(exchange.getInMessage(), r);
            return new MessageContentsList(r);
        }
        return null;
    }

    private void setResponseContentTypeIfNeeded(Message inMessage, Object response) {
        if (response instanceof Response) {
            JAXRSUtils.setMessageContentType(inMessage, (Response)response);
        }
    }
    private Object handleFault(Throwable ex, Message inMessage) {
        return handleFault(new Fault(ex), inMessage, null, null);
    }
    private Object handleFault(Fault ex, Message inMessage,
                               ClassResourceInfo cri, Method methodToInvoke) {
        String errorMessage = ex.getMessage();
        if (errorMessage != null && cri != null
            && errorMessage.contains(PROXY_INVOCATION_ERROR_FRAGMENT)) {
            org.apache.cxf.common.i18n.Message errorM =
                new org.apache.cxf.common.i18n.Message("PROXY_INVOCATION_FAILURE",
                                                       BUNDLE,
                                                       methodToInvoke,
                                                       cri.getServiceClass().getName());
            LOG.severe(errorM.toString());
        }
        Response excResponse =
            JAXRSUtils.convertFaultToResponse(ex.getCause() == null ? ex : ex.getCause(), inMessage);
        if (excResponse == null) {
            inMessage.getExchange().put(Message.PROPOGATE_EXCEPTION,
                                        ExceptionUtils.propogateException(inMessage));
            throw ex;
        }
        return new MessageContentsList(excResponse);
    }

    @SuppressWarnings("unchecked")
    protected MultivaluedMap<String, String> getTemplateValues(Message msg) {
        MultivaluedMap<String, String> values = new MetadataMap<>();
        MultivaluedMap<String, String> oldValues =
            (MultivaluedMap<String, String>)msg.get(URITemplate.TEMPLATE_PARAMETERS);
        if (oldValues != null) {
            values.putAll(oldValues);
        }
        return values;
    }

    private boolean setServiceLoaderAsContextLoader(Message inMessage) {
        Object en = inMessage.getContextualProperty(SERVICE_LOADER_AS_CONTEXT);
        return Boolean.TRUE.equals(en) || "true".equals(en);
    }

    private boolean isServiceObjectRequestScope(Message inMessage) {
        Object scope = inMessage.getContextualProperty(SERVICE_OBJECT_SCOPE);
        return REQUEST_SCOPE.equals(scope);
    }

    private ResourceProvider getResourceProvider(Exchange exchange) {
        Object provider = exchange.remove(JAXRSUtils.ROOT_PROVIDER);
        if (provider == null) {
            OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
            ClassResourceInfo cri = ori.getClassResourceInfo();
            return cri.getResourceProvider();
        }
        return (ResourceProvider)provider;
    }

    public Object getServiceObject(Exchange exchange) {

        Object root = exchange.remove(JAXRSUtils.ROOT_INSTANCE);
        if (root != null) {
            return root;
        }

        OperationResourceInfo ori = exchange.get(OperationResourceInfo.class);
        ClassResourceInfo cri = ori.getClassResourceInfo();

        return cri.getResourceProvider().getInstance(exchange.getInMessage());
    }

    protected Object getActualServiceObject(Exchange exchange, Object rootInstance) {

        Object last = exchange.get(LAST_SERVICE_OBJECT);
        return last !=  null ? last : rootInstance;
    }



    private static Object checkResultObject(Object result) {

        if (result != null) {
            if (result instanceof MessageContentsList) {
                result = ((MessageContentsList)result).get(0);
            } else if (result instanceof List) {
                result = ((List<?>)result).get(0);
            } else if (result.getClass().isArray()) {
                result = ((Object[])result)[0];
            }
        }
        return result;
    }
    private static Object checkSubResultObject(Object result, String subResourcePath) {
        result = checkResultObject(result);
        if (result == null) {
            org.apache.cxf.common.i18n.Message errorM =
                new org.apache.cxf.common.i18n.Message("NULL_SUBRESOURCE",
                                                       BUNDLE,
                                                       subResourcePath);
            LOG.info(errorM.toString());
            throw ExceptionUtils.toNotFoundException(null, null);
        }

        return result;
    }


}

