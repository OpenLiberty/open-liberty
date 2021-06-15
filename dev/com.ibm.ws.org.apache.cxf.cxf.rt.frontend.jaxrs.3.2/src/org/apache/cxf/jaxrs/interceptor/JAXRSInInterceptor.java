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

package org.apache.cxf.jaxrs.interceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfoStack;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.JaxRsConstants;
import com.ibm.ws.jaxrs20.cache.LibertyJaxRsResourceMethodCache;
import com.ibm.ws.jaxrs20.cache.LibertyJaxRsResourceMethodCache.ResourceMethodCache;

public class JAXRSInInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final TraceComponent tc = Tr.register(JAXRSInInterceptor.class);
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSInInterceptor.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSInInterceptor.class);
    private static final String RESOURCE_METHOD = "org.apache.cxf.resource.method";
    private static final String RESOURCE_OPERATION_NAME = "org.apache.cxf.resource.operation.name";
    public JAXRSInInterceptor() {
        super(Phase.UNMARSHAL);
    }

    @FFDCIgnore(value = { Fault.class, RuntimeException.class })
    @Override
    public void handleMessage(Message message) {
        final Exchange exchange = message.getExchange();

        exchange.put(Message.REST_MESSAGE, Boolean.TRUE);
        Response response = exchange.get(Response.class);
        if (response == null) {
            try {
                processRequest(message, exchange);
                if (exchange.isOneWay()) {
                    ServerProviderFactory.getInstance(message).clearThreadLocalProxies();
                }
            } catch (Fault ex) {
                convertExceptionToResponseIfPossible(ex.getCause(), message);
            } catch (RuntimeException ex) {
                convertExceptionToResponseIfPossible(ex, message);
            } catch (IOException ex) {
                convertExceptionToResponseIfPossible(ex, message);
            }
        }

        response = exchange.get(Response.class);
        if (response != null) {
            createOutMessage(message, response);
            message.getInterceptorChain().doInterceptStartingAt(message,
                                                                OutgoingChainInterceptor.class.getName());
        }
    }

    @FFDCIgnore(value = { WebApplicationException.class })
    private void processRequest(Message message, Exchange exchange) throws IOException {

        ServerProviderFactory providerFactory = ServerProviderFactory.getInstance(message);

        RequestPreprocessor rp = providerFactory.getRequestPreprocessor();
        if (rp != null) {
            rp.preprocess(message, new UriInfoImpl(message, null));
        }

        // Global pre-match request filters
        if (JaxRsConstants.JAXRS_CONTAINER_FILTER_DISABLED == false) {
            if (JAXRSUtils.runContainerRequestFilters(providerFactory, message, true, null)) {
                return;
            }
        }
        // HTTP method
        String httpMethod = HttpUtils.getProtocolHeader(message, Message.HTTP_REQUEST_METHOD,
                                                        HttpMethod.POST, true);

        // Path to match
        String rawPath = HttpUtils.getPathToMatch(message, true);

        Map<String, List<String>> protocolHeaders = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));

        // Content-Type
        String requestContentType = null;
        List<String> ctHeaderValues = protocolHeaders.get(Message.CONTENT_TYPE);
        if (ctHeaderValues != null && !ctHeaderValues.isEmpty()) {
            requestContentType = ctHeaderValues.get(0);
            message.put(Message.CONTENT_TYPE, requestContentType);
        }
        if (requestContentType == null) {
            requestContentType = (String)message.get(Message.CONTENT_TYPE);

            if (requestContentType == null) {
                requestContentType = MediaType.WILDCARD;
            }
        }

        // Accept
        String acceptTypes = null;
        List<String> acceptHeaderValues = protocolHeaders.get(Message.ACCEPT_CONTENT_TYPE);
        if (acceptHeaderValues != null && !acceptHeaderValues.isEmpty()) {
            acceptTypes = acceptHeaderValues.get(0);
            message.put(Message.ACCEPT_CONTENT_TYPE, acceptTypes);
        }

        if (acceptTypes == null) {
            acceptTypes = HttpUtils.getProtocolHeader(message, Message.ACCEPT_CONTENT_TYPE, null);
            if (acceptTypes == null) {
                acceptTypes = "*/*";
                message.put(Message.ACCEPT_CONTENT_TYPE, acceptTypes);
            }
        }
        List<MediaType> acceptContentTypes = null;
        try {
            acceptContentTypes = JAXRSUtils.sortMediaTypes(acceptTypes, JAXRSUtils.MEDIA_TYPE_Q_PARAM);
        } catch (IllegalArgumentException ex) {
            throw ExceptionUtils.toNotAcceptableException(null, null);
        }
        exchange.put(Message.ACCEPT_CONTENT_TYPE, acceptContentTypes);

        //1. Matching target resource class
        List<ClassResourceInfo> resources = JAXRSUtils.getRootResources(message);

        LibertyJaxRsResourceMethodCache resourceMethodCache = exchange.getBus().getExtension(LibertyJaxRsResourceMethodCache.class);

        MultivaluedMap<String, String> matchedValues = null; // Liberty change

        OperationResourceInfo ori = null;

        OperationResourceInfoStack oriStack = null;

        boolean shouldFind = true;

        String ckey = message.get(Message.BASE_PATH) + ":" + rawPath + ":" + httpMethod + ":" + requestContentType + ":" + acceptTypes;

        if (resourceMethodCache != null) {
            ResourceMethodCache rmCache = resourceMethodCache.get(ckey);
            if (rmCache != null) {
                ori = rmCache.getOperationResourceInfo();
                matchedValues = rmCache.getValues();
                String mediaType = rmCache.getMediaType();
                if (!ori.isSubResourceLocator() && mediaType != null) {
                    message.getExchange().put(Message.CONTENT_TYPE, mediaType);
                }

                setExchangeProperties(message, exchange, ori, matchedValues, resources.size());

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "OperationResourceInfoStack on message: " + message.get(OperationResourceInfoStack.class));
                }
                if (message.get(OperationResourceInfoStack.class) == null) {
                    oriStack = rmCache.getOperationResourceInfoStack();
                    if (oriStack != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Setting OperationResourceInfoStack on message: " + oriStack);
                        }
                        message.put(OperationResourceInfoStack.class, oriStack);
                    }
                }
                shouldFind = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "shouldFind = " + shouldFind);
        }
        if (shouldFind == true) {
            matchedValues = new MetadataMap<>(); // Liberty change

            Map<ClassResourceInfo, MultivaluedMap<String, String>> matchedResources = JAXRSUtils.selectResourceClass(resources, rawPath, message);

        if (matchedResources == null) {
            org.apache.cxf.common.i18n.Message errorMsg =
                new org.apache.cxf.common.i18n.Message("NO_ROOT_EXC",
                                                   BUNDLE,
                                                   message.get(Message.REQUEST_URI),
                                                   rawPath);
            Level logLevel = JAXRSUtils.getExceptionLogLevel(message, NotFoundException.class);
            LOG.log(logLevel == null ? Level.FINE : logLevel, errorMsg.toString());
            Response resp = JAXRSUtils.createResponse(resources, message, errorMsg.toString(),
                    Response.Status.NOT_FOUND.getStatusCode(), false);
            throw ExceptionUtils.toNotFoundException(null, resp);
        }

        try {
            ori = JAXRSUtils.findTargetMethod(matchedResources, message,
                      httpMethod, matchedValues, requestContentType, acceptContentTypes, true, true);
            setExchangeProperties(message, exchange, ori, matchedValues, resources.size());

                // The oriStack should now be set.
                oriStack = message.get(OperationResourceInfoStack.class);
                if (resourceMethodCache != null) {
                    String mediaType = (String) message.getExchange().get(Message.CONTENT_TYPE);
                    resourceMethodCache.put(ckey, ori, matchedValues, mediaType, oriStack);
                }

        } catch (WebApplicationException ex) {
            if (JAXRSUtils.noResourceMethodForOptions(ex.getResponse(), httpMethod)) {
                //Liberty Change start
                // Use the matched ClassResourceInfo so that the options request returns the allowed headers for this ClassResourceInfo not the allowed headers for all of the ClassResourceInfos 
                Response response = JAXRSUtils.createResponse(new ArrayList<ClassResourceInfo>(matchedResources.keySet()), message, null, 200, true);
              //Liberty Change end
                exchange.put(Response.class, response);
                return;
            }
            throw ex;
        }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Request path is: " + rawPath);
                Tr.debug(tc, "Request HTTP method is: " + httpMethod);
                Tr.debug(tc, "Request contentType is: " + requestContentType);
                Tr.debug(tc, "Accept contentType is: " + acceptTypes);
                Tr.debug(tc, "Found operation: " + ori.getMethodToInvoke().getName());
            }
        }
        // Global and name-bound post-match request filters
        if (JaxRsConstants.JAXRS_CONTAINER_FILTER_DISABLED == false) {
            if (!ori.isSubResourceLocator()
                && JAXRSUtils.runContainerRequestFilters(providerFactory,
                                                         message,
                                                         false,
                                                         ori.getNameBindings())) {
                return;
            }
        }


        //Process parameters
        List<Object> params = JAXRSUtils.processParameters(ori, matchedValues, message);
        message.setContent(List.class, params);
    }

    private void convertExceptionToResponseIfPossible(Throwable ex, Message message) {
        Response excResponse = JAXRSUtils.convertFaultToResponse(ex, message);
        if (excResponse == null) {
            ServerProviderFactory.getInstance(message).clearThreadLocalProxies();
            message.getExchange().put(Message.PROPOGATE_EXCEPTION,
                                      ExceptionUtils.propogateException(message));
            throw ex instanceof RuntimeException ? (RuntimeException) ex
                            : JAXRSUtils.toJaxRsRuntimeException(ex); //Liberty change
        }
        message.getExchange().put(Response.class, excResponse);
        message.getExchange().put(Throwable.class, ex);
    }

    private void setExchangeProperties(Message message,
                                       Exchange exchange,
                                       OperationResourceInfo ori,
                                       MultivaluedMap<String, String> values,
                                       int numberOfResources) {
        final ClassResourceInfo cri = ori.getClassResourceInfo();
        exchange.put(OperationResourceInfo.class, ori);
        exchange.put(JAXRSUtils.ROOT_RESOURCE_CLASS, cri);
        message.put(RESOURCE_METHOD, ori.getMethodToInvoke());
        message.put(URITemplate.TEMPLATE_PARAMETERS, values);
        message.put(URITemplate.URI_TEMPLATE, JAXRSUtils.getUriTemplate(message, cri, ori));

        String plainOperationName = ori.getMethodToInvoke().getName();
        if (numberOfResources > 1) {
            plainOperationName = cri.getServiceClass().getSimpleName() + "#" + plainOperationName;
        }
        exchange.put(RESOURCE_OPERATION_NAME, plainOperationName);

        if (ori.isOneway()
            || PropertyUtils.isTrue(HttpUtils.getProtocolHeader(message, Message.ONE_WAY_REQUEST, null))) {
            exchange.setOneWay(true);
        }
        ResourceProvider rp = cri.getResourceProvider();
        if (rp instanceof SingletonResourceProvider) {
            //cri.isSingleton is not guaranteed to indicate we have a 'pure' singleton
            exchange.put(Message.SERVICE_OBJECT, rp.getInstance(message));
        }
    }

    private Message createOutMessage(Message inMessage, Response r) {
        Endpoint e = inMessage.getExchange().getEndpoint();
        Message mout = e.getBinding().createMessage();
        mout.setContent(List.class, new MessageContentsList(r));
        mout.setExchange(inMessage.getExchange());
        mout.setInterceptorChain(
             OutgoingChainInterceptor.getOutInterceptorChain(inMessage.getExchange()));
        inMessage.getExchange().setOutMessage(mout);
        if (r.getStatus() >= Response.Status.BAD_REQUEST.getStatusCode()) {
            inMessage.getExchange().put("cxf.io.cacheinput", Boolean.FALSE);
        }
        return mout;
    }
}
