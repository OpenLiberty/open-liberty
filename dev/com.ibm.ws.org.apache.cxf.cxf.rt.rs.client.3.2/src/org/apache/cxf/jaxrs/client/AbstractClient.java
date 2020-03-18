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
package org.apache.cxf.jaxrs.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.WriterInterceptor;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.ClientLifeCycleManager;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Retryable;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InFaultChainInitiatorObserver;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.StaxInEndingInterceptor;
import org.apache.cxf.jaxrs.client.spec.ClientRequestFilterInterceptor;
import org.apache.cxf.jaxrs.client.spec.ClientResponseFilterInterceptor;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.MessageObserver;

/**
 * Common proxy and http-centric client implementation
 *
 */
public abstract class AbstractClient implements Client {
    public static final String EXECUTOR_SERVICE_PROPERTY = "executorService";

    protected static final String REQUEST_CONTEXT = "RequestContext";
    protected static final String RESPONSE_CONTEXT = "ResponseContext";
    protected static final String KEEP_CONDUIT_ALIVE = "KeepConduitAlive";
    protected static final String HTTP_SCHEME = "http";

    private static final String ALLOW_EMPTY_PATH_VALUES = "allow.empty.path.template.value";
    private static final String PROXY_PROPERTY = "jaxrs.proxy";
    private static final String HEADER_SPLIT_PROPERTY = "org.apache.cxf.http.header.split";
    private static final String SERVICE_NOT_AVAIL_PROPERTY = "org.apache.cxf.transport.service_not_available";
    private static final String COMPLETE_IF_SERVICE_NOT_AVAIL_PROPERTY =
        "org.apache.cxf.transport.complete_if_service_not_available";

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractClient.class);
    private static final Set<String> KNOWN_METHODS = new HashSet<>(
        Arrays.asList("GET", "POST", "HEAD", "OPTIONS", "PUT", "DELETE", "TRACE"));

    protected ClientConfiguration cfg = new ClientConfiguration();
    private ClientState state;
    private final AtomicBoolean closed = new AtomicBoolean();
    protected AbstractClient(ClientState initialState) {
        this.state = initialState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client query(String name, Object...values) {
        addMatrixQueryParamsToBuilder(getCurrentBuilder(), name, ParameterType.QUERY, null, values);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client header(String name, Object... values) {
        if (values == null) {
            throw new IllegalArgumentException();
        }
        if (HttpHeaders.CONTENT_TYPE.equals(name)) {
            if (values.length > 1) {
                throw new IllegalArgumentException("Content-Type can have a single value only");
            }
            type(convertParamValue(values[0], null));
        } else {
            for (Object o : values) {
                possiblyAddHeader(name, convertParamValue(o, null));
            }
        }
        return this;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Client headers(MultivaluedMap<String, String> map) {
        //defect 213017, according to javadoc, need to clear all existing Headers.
        removeAllHeaders();
        state.getRequestHeaders().putAll(map);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client accept(MediaType... types) {
        for (MediaType mt : types) {
            possiblyAddHeader(HttpHeaders.ACCEPT, JAXRSUtils.mediaTypeToString(mt));
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client type(MediaType ct) {
        return type(JAXRSUtils.mediaTypeToString(ct));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client type(String type) {
        state.getRequestHeaders().putSingle(HttpHeaders.CONTENT_TYPE, type);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client accept(String... types) {
        for (String type : types) {
            possiblyAddHeader(HttpHeaders.ACCEPT, type);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client cookie(Cookie cookie) {
        possiblyAddHeader(HttpHeaders.COOKIE, cookie.toString());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client authorization(Object auth) {
        String value = convertParamValue(auth, null);
        state.getRequestHeaders().putSingle(HttpHeaders.AUTHORIZATION, value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client modified(Date date, boolean ifNot) {
        SimpleDateFormat dateFormat = HttpUtils.getHttpDateFormat();
        String hName = ifNot ? HttpHeaders.IF_UNMODIFIED_SINCE : HttpHeaders.IF_MODIFIED_SINCE;
        state.getRequestHeaders().putSingle(hName, dateFormat.format(date));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client language(String language) {
        state.getRequestHeaders().putSingle(HttpHeaders.CONTENT_LANGUAGE, language);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client match(EntityTag tag, boolean ifNot) {
        String hName = ifNot ? HttpHeaders.IF_NONE_MATCH : HttpHeaders.IF_MATCH;
        state.getRequestHeaders().putSingle(hName, tag.toString());
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client acceptLanguage(String... languages) {
        for (String s : languages) {
            possiblyAddHeader(HttpHeaders.ACCEPT_LANGUAGE, s);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client acceptEncoding(String... encs) {
        for (String s : encs) {
            possiblyAddHeader(HttpHeaders.ACCEPT_ENCODING, s);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client encoding(String enc) {
        state.getRequestHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, enc);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MultivaluedMap<String, String> getHeaders() {
        MultivaluedMap<String, String> map = new MetadataMap<>(false, true);
        map.putAll(state.getRequestHeaders());
        return map;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getBaseURI() {
        return state.getBaseURI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getCurrentURI() {
        return getCurrentBuilder().clone().buildFromEncoded();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getResponse() {
        return state.getResponse();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Client reset() {
        state.reset();
        return this;
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (cfg.getBus() == null) {
                return;
            }
            cfg.getEndpoint().getCleanupHooks().
                    forEach(c -> {
                        try {
                            c.close();
                        } catch (IOException e) {
                            //ignore
                        }
                    });
            ClientLifeCycleManager mgr = cfg.getBus().getExtension(ClientLifeCycleManager.class);
            if (null != mgr) {
                mgr.clientDestroyed(new FrontendClientAdapter(getConfiguration()));
            }

            if (cfg.getConduitSelector() instanceof Closeable) {
                try {
                    ((Closeable)cfg.getConduitSelector()).close();
                } catch (IOException e) {
                    //ignore, we're destroying anyway
                }
            } else {
                cfg.getConduit().close();
            }
            state.reset();
            if (cfg.isShutdownBusOnClose()) {
                cfg.getBus().shutdown(false);
            }
            state = null;
            cfg = null;
        }
    }

    public void removeAllHeaders() {
        state.getRequestHeaders().clear();
    }

    private void possiblyAddHeader(String name, String value) {
        if (!isDuplicate(name, value)) {
            state.getRequestHeaders().add(name, value);
        }
    }

    private boolean isDuplicate(String name, String value) {
        List<String> values = state.getRequestHeaders().get(name);
        return values != null && values.contains(value);
    }

    protected ClientState getState() {
        return state;
    }

    protected UriBuilder getCurrentBuilder() {
        return state.getCurrentBuilder();
    }

    protected void resetResponse() {
        state.setResponse(null);
    }

    protected void resetBaseAddress(URI uri) {
        state.setBaseURI(uri);
    }

    protected void resetCurrentBuilder(URI uri) {
        state.setCurrentBuilder(new UriBuilderImpl(uri));
    }

    protected MultivaluedMap<String, String> getTemplateParametersMap(URITemplate template,
                                                                      List<Object> values) {
        if (values != null && !values.isEmpty()) {
            List<String> vars = template.getVariables();
            MultivaluedMap<String, String> templatesMap = new MetadataMap<>(vars.size());
            for (int i = 0; i < vars.size(); i++) {
                if (i < values.size()) {
                    templatesMap.add(vars.get(i), values.get(i).toString());
                }
            }
            return templatesMap;
        }
        return null;
    }

    protected ResponseBuilder setResponseBuilder(Message outMessage, Exchange exchange) throws Exception {
        Response response = exchange.get(Response.class);
        if (response != null) {
            outMessage.getExchange().getInMessage().put(Message.PROTOCOL_HEADERS, response.getStringHeaders());
            return JAXRSUtils.fromResponse(JAXRSUtils.copyResponseIfNeeded(response));
        }

        Integer status = getResponseCode(exchange);
        ResponseBuilder currentResponseBuilder = JAXRSUtils.toResponseBuilder(status);

        Message responseMessage = exchange.getInMessage() != null
            ? exchange.getInMessage() : exchange.getInFaultMessage();
        // if there is no response message, we just send the response back directly
        if (responseMessage == null) {
            return currentResponseBuilder;
        }

        Map<String, List<Object>> protocolHeaders =
            CastUtils.cast((Map<?, ?>)responseMessage.get(Message.PROTOCOL_HEADERS));

        boolean splitHeaders = MessageUtils.getContextualBoolean(outMessage, HEADER_SPLIT_PROPERTY);
        for (Map.Entry<String, List<Object>> entry : protocolHeaders.entrySet()) {
            if (null == entry.getKey()) {
                continue;
            }
            if (entry.getValue().size() > 0) {
                if (HttpUtils.isDateRelatedHeader(entry.getKey())) {
                    currentResponseBuilder.header(entry.getKey(), entry.getValue().get(0));
                    continue;
                }
                entry.getValue().forEach(valObject -> {
                    if (splitHeaders && valObject instanceof String) {
                        String val = (String) valObject;
                        String[] values;
                        if (val.length() == 0) {
                            values = new String[]{""};
                        } else if (val.charAt(0) == '"' && val.charAt(val.length() - 1) == '"') {
                            // if the value starts with a quote and ends with a quote, we do a best
                            // effort attempt to determine what the individual values are.
                            values = parseQuotedHeaderValue(val);
                        } else {
                            boolean splitPossible = !(HttpHeaders.SET_COOKIE.equalsIgnoreCase(entry.getKey())
                                    && val.toUpperCase().contains(HttpHeaders.EXPIRES.toUpperCase()));
                            values = splitPossible ? val.split(",") : new String[]{val};
                        }
                        for (String s : values) {
                            String theValue = s.trim();
                            if (theValue.length() > 0) {
                                currentResponseBuilder.header(entry.getKey(), theValue);
                            }
                        }
                    } else {
                        currentResponseBuilder.header(entry.getKey(), valObject);
                    }
                });
            }
        }
        String ct = (String)responseMessage.get(Message.CONTENT_TYPE);
        if (ct != null) {
            currentResponseBuilder.type(ct);
        }
        InputStream mStream = responseMessage.getContent(InputStream.class);
        currentResponseBuilder.entity(mStream);

        return currentResponseBuilder;
    }

    protected <T> void writeBody(T o, Message outMessage, Class<?> cls, Type type, Annotation[] anns,
                                 OutputStream os) {

        if (o == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        MultivaluedMap<String, Object> headers =
            (MultivaluedMap<String, Object>)outMessage.get(Message.PROTOCOL_HEADERS);

        @SuppressWarnings("unchecked")
        Class<T> theClass = (Class<T>)cls;

        Object contentTypeHeader = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentTypeHeader == null) {
            contentTypeHeader = MediaType.WILDCARD;
        }
        MediaType contentType = JAXRSUtils.toMediaType(contentTypeHeader.toString());

        List<WriterInterceptor> writers = ClientProviderFactory.getInstance(outMessage)
            .createMessageBodyWriterInterceptor(theClass, type, anns, contentType, outMessage, null);
        if (writers != null) {
            try {
                JAXRSUtils.writeMessageBody(writers,
                                            o,
                                            theClass,
                                            type,
                                            anns,
                                            contentType,
                                            headers,
                                            outMessage);

                OutputStream realOs = outMessage.get(OutputStream.class);
                if (realOs != null) {
                    realOs.flush();
                }
            } catch (Exception ex) {
                reportMessageHandlerProblem("MSG_WRITER_PROBLEM", cls, contentType, ex);
            }
        } else {
            reportMessageHandlerProblem("NO_MSG_WRITER", cls, contentType, null);
        }

    }

    protected WebApplicationException convertToWebApplicationException(Response r) {
        try {
            Class<?> exceptionClass = ExceptionUtils.getWebApplicationExceptionClass(r,
                                       WebApplicationException.class);
            Constructor<?> ctr = exceptionClass.getConstructor(Response.class);
            return (WebApplicationException)ctr.newInstance(r);
        } catch (Throwable ex2) {
            return new WebApplicationException(r);
        }
    }

    protected <T> T readBody(Response r, Message outMessage, Class<T> cls,
                             Type type, Annotation[] anns) {

        if (cls == Response.class) {
            return cls.cast(r);
        }

        int status = r.getStatus();
        if ((status < 200 || status == 204) && r.getLength() <= 0 || (status >= 300 && status != 304)) {
            return null;
        }
        //defect 211445
        return ((ResponseImpl) r).doReadEntity(cls, type, anns, false);
    }

    protected boolean responseStreamCanBeClosed(Message outMessage, Class<?> cls) {
        return !JAXRSUtils.isStreamingOutType(cls)
            && MessageUtils.getContextualBoolean(outMessage, "response.stream.auto.close");
    }

    protected void completeExchange(Exchange exchange, boolean proxy) {
        // higher level conduits such as FailoverTargetSelector need to
        // clear the request state but a fair number of response objects
        // depend on InputStream being still open thus lower-level conduits
        // operating on InputStream don't have to close streams pro-actively
        exchange.put(KEEP_CONDUIT_ALIVE, true);
        getConfiguration().getConduitSelector().complete(exchange);
        String s = (String)exchange.getOutMessage().get(Message.BASE_PATH);
        if (s != null && !state.getBaseURI().toString().equals(s)) {
            // usually the (failover) conduit change will result in a retry call
            // which in turn will reset the base and current request URI.
            // In some cases, such as the "upfront" load-balancing, etc, the retries
            // won't be executed so it is necessary to reset the base address
            calculateNewRequestURI(URI.create(s), getCurrentURI(), proxy);
            return;
        }
        s = (String)exchange.getOutMessage().get("transport.retransmit.url");
        if (s != null && !state.getBaseURI().toString().equals(s)) {
            calculateNewRequestURI(URI.create(s), getCurrentURI(), proxy);
        }
    }

    protected Object[] preProcessResult(Message message) throws Exception {
        Exchange exchange = message.getExchange();

        Exception ex = message.getContent(Exception.class);
        if (ex == null) {
            ex = message.getExchange().get(Exception.class);
        }
        if (ex == null && !exchange.isOneWay()) {
            synchronized (exchange) {
                while (exchange.get("IN_CHAIN_COMPLETE") == null) {
                    exchange.wait(cfg.getSynchronousTimeout());
                }
            }
        }
        if (ex == null) {
            ex = message.getContent(Exception.class);
        }
        if (ex != null
            || PropertyUtils.isTrue(exchange.get(SERVICE_NOT_AVAIL_PROPERTY))
                && PropertyUtils.isTrue(exchange.get(COMPLETE_IF_SERVICE_NOT_AVAIL_PROPERTY))) {
            getConfiguration().getConduitSelector().complete(exchange);
        }
        if (ex != null) {
            checkClientException(message, ex);
        }
        checkClientException(message, exchange.get(Exception.class));

        List<?> result = exchange.get(List.class);
        return result != null ? result.toArray() : null;
    }

    protected void checkClientException(Message outMessage, Exception ex) throws Exception {
        Throwable actualEx = ex instanceof Fault ? ((Fault)ex).getCause() : ex;

        Exchange exchange = outMessage.getExchange();
        Integer responseCode = getResponseCode(exchange);
        if (responseCode == null
            || responseCode < 300 && !(actualEx instanceof IOException)
            || actualEx instanceof IOException && exchange.get("client.redirect.exception") != null) {
            if (actualEx instanceof ProcessingException) {
                throw (RuntimeException)actualEx;
            } else if (actualEx != null) {
                Object useProcExProp = exchange.get("wrap.in.processing.exception");
                if (actualEx instanceof RuntimeException
                    && useProcExProp != null && PropertyUtils.isFalse(useProcExProp)) {
                    throw (Exception)actualEx;
                }
                throw new ProcessingException(actualEx);
            } else if (!exchange.isOneWay() || cfg.isResponseExpectedForOneway()) {
                waitForResponseCode(exchange);
            }
        }
    }

    protected void waitForResponseCode(Exchange exchange) {
        synchronized (exchange) {
            if (getResponseCode(exchange) == null) {
                try {
                    exchange.wait(cfg.getSynchronousTimeout());
                } catch (InterruptedException ex) {
                    // ignore
                }
            } else {
                return;
            }
        }

        if (getResponseCode(exchange) == null) {
            throw new ProcessingException("Response timeout");
        }
    }

    private Integer getResponseCode(Exchange exchange) {
        Integer responseCode = (Integer)exchange.get(Message.RESPONSE_CODE);
        if (responseCode == null && exchange.getInMessage() != null) {
            responseCode = (Integer)exchange.getInMessage().get(Message.RESPONSE_CODE);
        }
        if (responseCode == null && exchange.isOneWay() && !state.getBaseURI().toString().startsWith("http")) {
            responseCode = 202;
        }
        return responseCode;
    }


    protected URI calculateNewRequestURI(Map<String, Object> reqContext) {
        URI newBaseURI = URI.create(reqContext.get(Message.ENDPOINT_ADDRESS).toString());
        URI requestURI = URI.create(reqContext.get(Message.REQUEST_URI).toString());
        return calculateNewRequestURI(newBaseURI, requestURI,
                PropertyUtils.isTrue(reqContext.get(PROXY_PROPERTY)));
    }

    private URI calculateNewRequestURI(URI newBaseURI, URI requestURI, boolean proxy) {
        String baseURIPath = newBaseURI.getRawPath();
        String reqURIPath = requestURI.getRawPath();

        UriBuilder builder = new UriBuilderImpl().uri(newBaseURI);
        String basePath = reqURIPath.startsWith(baseURIPath) ? baseURIPath : getBaseURI().getRawPath();
        String relativePath = reqURIPath.equals(basePath) ? ""
                : reqURIPath.startsWith(basePath) ? reqURIPath.substring(basePath.length()) : reqURIPath;
        builder.path(relativePath);

        String newQuery = newBaseURI.getRawQuery();
        if (newQuery == null) {
            builder.replaceQuery(requestURI.getRawQuery());
        } else {
            builder.replaceQuery(newQuery);
        }

        URI newRequestURI = builder.build();

        resetBaseAddress(newBaseURI);
        URI current = proxy ? newBaseURI : newRequestURI;
        resetCurrentBuilder(current);

        return newRequestURI;
    }

    protected void doRunInterceptorChain(Message m) {
        try {
            m.getInterceptorChain().doIntercept(m);
        } catch (Exception ex) {
            m.setContent(Exception.class, ex);
        }
    }

    @SuppressWarnings("unchecked")
    protected Object[] retryInvoke(BindingOperationInfo oi, Object[] params, Map<String, Object> context,
                              Exchange exchange) throws Exception {

        try {
            Object body = params.length == 0 ? null : params[0];
            Map<String, Object> reqContext = CastUtils.cast((Map<?, ?>)context.get(REQUEST_CONTEXT));
            MultivaluedMap<String, String> headers =
                (MultivaluedMap<String, String>)reqContext.get(Message.PROTOCOL_HEADERS);

            URI newRequestURI = calculateNewRequestURI(reqContext);
            // TODO: if failover conduit selector fails to find a failover target
            // then it will revert to the previous endpoint; that is not very likely
            // but possible - thus ideally we need to resert base and current URI only
            // if we get the same ConduitInitiatior endpoint instance before and after
            // retryInvoke.
            Object response = retryInvoke(newRequestURI, headers, body, exchange, context);
            exchange.put(List.class, getContentsList(response));
            return new Object[]{response};
        } catch (Throwable t) {
            Exception ex = t instanceof Exception ? (Exception)t : new Exception(t);
            exchange.put(Exception.class, ex);
            return null;
        }
    }

    protected abstract Object retryInvoke(URI newRequestURI,
                                 MultivaluedMap<String, String> headers,
                                 Object body,
                                 Exchange exchange,
                                 Map<String, Object> invContext) throws Throwable;


    protected void addMatrixQueryParamsToBuilder(UriBuilder ub,
                                                 String paramName,
                                                 ParameterType pt,
                                                 Annotation[] anns,
                                                 Object... pValues) {
        if (pt != ParameterType.MATRIX && pt != ParameterType.QUERY) {
            throw new IllegalArgumentException("This method currently deal "
                                               + "with matrix and query parameters only");
        }
        if (!"".equals(paramName)) {
            if (pValues != null && pValues.length > 0) {
                for (Object pValue : pValues) {
                    if (InjectionUtils.isSupportedCollectionOrArray(pValue.getClass())) {
                        Collection<?> c = pValue.getClass().isArray()
                            ? Arrays.asList((Object[]) pValue) : (Collection<?>) pValue;
                        for (Iterator<?> it = c.iterator(); it.hasNext();) {
                            convertMatrixOrQueryToBuilder(ub, paramName, it.next(), pt, anns);
                        }
                    } else {
                        convertMatrixOrQueryToBuilder(ub, paramName, pValue, pt, anns);
                    }
                }
            } else {
                addMatrixOrQueryToBuilder(ub, paramName, pt, pValues);
            }
        } else {
            Object pValue = pValues[0];
            MultivaluedMap<String, Object> values = InjectionUtils.extractValuesFromBean(pValue, "");
            values.forEach((key, value) -> {
                value.forEach(v -> {
                    convertMatrixOrQueryToBuilder(ub, key, v, pt, anns);
                });
            });
        }
    }

    private void convertMatrixOrQueryToBuilder(UriBuilder ub,
                                           String paramName,
                                           Object pValue,
                                           ParameterType pt,
                                           Annotation[] anns) {
        Object convertedValue = convertParamValue(pValue, anns);
        addMatrixOrQueryToBuilder(ub, paramName, pt, convertedValue);
    }

    private void addMatrixOrQueryToBuilder(UriBuilder ub,
                                           String paramName,
                                           ParameterType pt,
                                           Object... pValue) {
        if (pt == ParameterType.MATRIX) {
            ub.matrixParam(paramName, pValue);
        } else {
            ub.queryParam(paramName, pValue);
        }
    }


    protected String convertParamValue(Object pValue, Annotation[] anns) {
        return convertParamValue(pValue, pValue == null ? null : pValue.getClass(), anns);
    }
    protected String convertParamValue(Object pValue, Class<?> pClass, Annotation[] anns) {
        if (pValue == null && pClass == null) {
            return null;
        }
        ProviderFactory pf = ClientProviderFactory.getInstance(cfg.getEndpoint());
        if (pf != null) {
            Message m = null;
            if (pf.isParamConverterContextsAvailable()) {
                m = new MessageImpl();
                m.put(Message.REQUESTOR_ROLE, Boolean.TRUE);
                m.setExchange(new ExchangeImpl());
                m.getExchange().setOutMessage(m);
                m.getExchange().put(Endpoint.class, cfg.getEndpoint());
            }
            @SuppressWarnings("unchecked")
            ParamConverter<Object> prov =
                (ParamConverter<Object>)pf.createParameterHandler(pClass, pClass, anns, m);
            if (prov != null) {
                try {
                    return prov.toString(pValue);
                } finally {
                    if (m != null) {
                        pf.clearThreadLocalProxies();
                    }
                }
            }
        }
        final String v = pValue == null ? null : pValue.toString();
        if (anns != null && StringUtils.isEmpty(v)) {
            final PathParam pp = AnnotationUtils.getAnnotation(anns, PathParam.class);
            if (null != pp) {
                Object allowEmptyProp = getConfiguration().getBus().getProperty(ALLOW_EMPTY_PATH_VALUES);
                if (!PropertyUtils.isTrue(allowEmptyProp)) {
                    throw new IllegalArgumentException("Value for " + pp.value() + " is not specified");
                }
            }
        }
        return v;
    }

    protected static void reportMessageHandlerProblem(String name, Class<?> cls, MediaType ct, Throwable ex) {
        String errorMessage = JAXRSUtils.logMessageHandlerProblem(name, cls, ct);
        Throwable actualEx = ex instanceof Fault ? ((Fault)ex).getCause() : ex;
        throw new ProcessingException(errorMessage, actualEx);
    }

    protected static void setAllHeaders(MultivaluedMap<String, String> headers, HttpURLConnection conn) {
        headers.forEach((key, value) -> {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < value.size(); i++) {
                b.append(value.get(i));
                if (i + 1 < value.size()) {
                    b.append(',');
                }
            }
            conn.setRequestProperty(key, b.toString());
        });
    }

    protected String[] parseQuotedHeaderValue(String originalValue) {
        // this algorithm isn't perfect; see CXF-3518 for further discussion.
        List<String> results = new ArrayList<>();
        char[] chars = originalValue.toCharArray();

        int lastIndex = chars.length - 1;

        boolean quote = false;
        StringBuilder sb = new StringBuilder();

        for (int pos = 0; pos <= lastIndex; pos++) {
            char c = chars[pos];
            if (pos == lastIndex) {
                sb.append(c);
                results.add(sb.toString());
            } else {
                switch(c) {
                case '\"':
                    sb.append(c);
                    quote = !quote;
                    break;
                case '\\':
                    if (quote) {
                        pos++;
                        if (pos <= lastIndex) {
                            c = chars[pos];
                            sb.append(c);
                        }
                        if (pos == lastIndex) {
                            results.add(sb.toString());
                        }
                    } else {
                        sb.append(c);
                    }
                    break;
                case ',':
                    if (quote) {
                        sb.append(c);
                    } else {
                        results.add(sb.toString());
                        sb = new StringBuilder();
                    }
                    break;
                default:
                    sb.append(c);
                }
            }
        }
        return results.toArray(new String[0]);
    }

    public ClientConfiguration getConfiguration() {
        return cfg;
    }

    protected void setConfiguration(ClientConfiguration config) {
        cfg = config;
    }

    // Note that some conduit selectors may update Message.ENDPOINT_ADDRESS
    // after the conduit selector has been prepared but before the actual
    // invocation thus it is also important to have baseURI and currentURI
    // synched up with the latest endpoint address, after a successful proxy
    // or web client invocation has returned
    protected void prepareConduitSelector(Message m, URI currentURI, boolean proxy) {
        try {
            cfg.prepareConduitSelector(m);

        } catch (Fault ex) {
            LOG.warning("Failure to prepare a message from conduit selector");
        }
        MessageImpl message = (MessageImpl) m;
        message.getExchange().put(ConduitSelector.class, cfg.getConduitSelector());
        message.getExchange().put(Service.class, cfg.getConduitSelector().getEndpoint().getService());

        String address = (String)message.getEndpointAddress();
        // custom conduits may override the initial/current address
        if (address.startsWith(HTTP_SCHEME) && !address.equals(currentURI.toString())) {
            URI baseAddress = URI.create(address);
            currentURI = calculateNewRequestURI(baseAddress, currentURI, proxy);
            String uri = currentURI.toString();
            message.setEndpointAddress(uri);
            message.setRequestUri(uri);
        }
        message.setBasePath(getBaseURI().toString());
    }

    protected static PhaseInterceptorChain setupOutInterceptorChain(ClientConfiguration cfg) {
        PhaseManager pm = cfg.getBus().getExtension(PhaseManager.class);
        List<Interceptor<? extends Message>> i1 = cfg.getBus().getOutInterceptors();
        List<Interceptor<? extends Message>> i2 = cfg.getOutInterceptors();
        List<Interceptor<? extends Message>> i3 = cfg.getConduitSelector().getEndpoint().getOutInterceptors();
        PhaseInterceptorChain chain = new PhaseChainCache().get(pm.getOutPhases(), i1, i2, i3);
        chain.add(new ClientRequestFilterInterceptor());
        return chain;
    }

    protected static PhaseInterceptorChain setupInInterceptorChain(ClientConfiguration cfg) {
        PhaseManager pm = cfg.getBus().getExtension(PhaseManager.class);
        List<Interceptor<? extends Message>> i1 = cfg.getBus().getInInterceptors();
        List<Interceptor<? extends Message>> i2 = cfg.getInInterceptors();
        List<Interceptor<? extends Message>> i3 = cfg.getConduitSelector().getEndpoint().getInInterceptors();
        PhaseInterceptorChain chain = new PhaseChainCache().get(pm.getInPhases(), i1, i2, i3);
        chain.add(new ClientResponseFilterInterceptor());
        return chain;
    }

    protected static MessageObserver setupInFaultObserver(final ClientConfiguration cfg) {
        return new InFaultChainInitiatorObserver(cfg.getBus()) {

            @Override
            protected void initializeInterceptors(Exchange ex, PhaseInterceptorChain chain) {
                chain.add(cfg.getInFaultInterceptors());
                chain.add(new ConnectionFaultInterceptor());
            }
        };
    }

    protected void setSupportOnewayResponseProperty(Message outMessage) {
        if (!outMessage.getExchange().isOneWay()) {
            outMessage.put(Message.PROCESS_ONEWAY_RESPONSE, true);
        }
    }
    protected void checkClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Client is closed");
        }
    }

    protected Message createMessage(Object body,
                                    String httpMethod,
                                    MultivaluedMap<String, String> headers,
                                    URI currentURI,
                                    Exchange exchange,
                                    Map<String, Object> invocationContext,
                                    boolean proxy) {
        checkClosed();
        MessageImpl m = (MessageImpl) cfg.getConduitSelector().getEndpoint().getBinding().createMessage();
        m.setRequestorRole(Boolean.TRUE);
        m.setInboundMessage(Boolean.FALSE);

        setRequestMethod(m, httpMethod);
        m.setProtocolHeaders(headers);
        if (currentURI.isAbsolute() && currentURI.getScheme().startsWith(HTTP_SCHEME)) {
            m.setEndpointAddress(currentURI.toString());
        } else {
            m.setEndpointAddress(state.getBaseURI().toString());
        }

        Object requestURIProperty = cfg.getRequestContext().get(Message.REQUEST_URI);
        if (requestURIProperty == null) {
            m.setRequestUri(currentURI.toString());
        } else {
            m.setRequestUri(requestURIProperty.toString());
        }

        String ct = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        m.setContentType(ct);

        body = checkIfBodyEmpty(body, ct);
        setEmptyRequestPropertyIfNeeded(m, body);

        m.setContent(List.class, getContentsList(body));

        m.setTemplateParameters(getState().getTemplates());

        PhaseInterceptorChain chain = setupOutInterceptorChain(cfg);
        chain.setFaultObserver(setupInFaultObserver(cfg));
        m.setInterceptorChain(chain);

        exchange = createExchange(m, exchange);
        exchange.put(Message.REST_MESSAGE, Boolean.TRUE);
        exchange.setOneWay("true".equals(headers.getFirst(Message.ONE_WAY_REQUEST)));
        exchange.put(Retryable.class, new RetryableImpl());

        // context
        setContexts(m, exchange, invocationContext, proxy);

        //setup conduit selector
        prepareConduitSelector(m, currentURI, proxy);

        return m;
    }

    private void setRequestMethod(Message m, String httpMethod) {
        ((MessageImpl) m).setHttpRequestMethod(httpMethod);
        if (!KNOWN_METHODS.contains(httpMethod)) {
            if (!m.containsKey("use.async.http.conduit")) {
                // if the async conduit is loaded then let it handle this method without users
                // having to explicitly request it given that, without reflectively updating
                // HTTPUrlConnection, it will not work without the async conduit anyway
                m.put("use.async.http.conduit", true);
            }

            if (!m.containsKey("use.httpurlconnection.method.reflection")) {
                // if the async conduit is not loaded then the only way for the custom HTTP verb
                // to be supported is to attempt to reflectively modify HTTPUrlConnection
                m.put("use.httpurlconnection.method.reflection", true);
            }
        }
    }

    protected void setEmptyRequestPropertyIfNeeded(Message outMessage, Object body) {
        if (body == null) {
            outMessage.put("org.apache.cxf.empty.request", true);
        }
    }


    protected Object checkIfBodyEmpty(Object body, String contentType) {
        //CHECKSTYLE:OFF
        if (body != null
            && (body.getClass() == String.class && ((String)body).length() == 0
            || body.getClass() == Form.class && ((Form)body).asMap().isEmpty()
            || Map.class.isAssignableFrom(body.getClass()) && ((Map<?, ?>)body).isEmpty()
                && !MediaType.APPLICATION_JSON.equals(contentType)
            || body instanceof byte[] && ((byte[])body).length == 0)) {
            body = null;
        }
        //CHECKSTYLE:ON
        return body;
    }

    protected Map<String, Object> getRequestContext(Message outMessage) {
        Map<String, Object> invContext
            = CastUtils.cast((Map<?, ?>)outMessage.get(Message.INVOCATION_CONTEXT));
        return CastUtils.cast((Map<?, ?>)invContext.get(REQUEST_CONTEXT));
    }

    protected List<?> getContentsList(Object body) {
        return body == null ? new MessageContentsList() : new MessageContentsList(body);
    }

    protected Exchange createExchange(Message m, Exchange exchange) {
        if (exchange == null) {
            exchange = new ExchangeImpl();
        }
        exchange.setSynchronous(true);
        exchange.setOutMessage(m);
        exchange.put(Bus.class, cfg.getBus());
        exchange.put(MessageObserver.class, new ClientMessageObserver(cfg));
        exchange.put(Endpoint.class, cfg.getConduitSelector().getEndpoint());
        exchange.put("org.apache.cxf.transport.no_io_exceptions", true);
        //REVISIT - when response handling is actually put onto the in chain, this will likely not be needed
        exchange.put(StaxInEndingInterceptor.STAX_IN_NOCLOSE, Boolean.TRUE);
        m.setExchange(exchange);
        return exchange;
    }

    protected void setAsyncMessageObserverIfNeeded(Exchange exchange) {
        if (!exchange.isSynchronous()) {
            ExecutorService executor = (ExecutorService)cfg.getRequestContext().get(EXECUTOR_SERVICE_PROPERTY);
            if (executor != null) {
                exchange.put(Executor.class, executor);

                final ClientMessageObserver observer = new ClientMessageObserver(cfg);

                exchange.put(MessageObserver.class, message -> {
                    if (!message.getExchange().containsKey(Executor.class.getName() + ".USING_SPECIFIED")) {
                        executor.execute(() -> {
                            observer.onMessage(message);
                        });
                    } else {
                        observer.onMessage(message);
                    }
                });
            }
        }
    }

    protected void setContexts(MessageImpl message, Exchange exchange,
                               Map<String, Object> context, boolean proxy) {
        if (context == null) {
            context = new HashMap<>();
        }
        Map<String, Object> reqContext = CastUtils.cast((Map<?, ?>)context.get(REQUEST_CONTEXT));
        Map<String, Object> resContext = CastUtils.cast((Map<?, ?>)context.get(RESPONSE_CONTEXT));
        if (reqContext == null) {
            reqContext = new HashMap<>(cfg.getRequestContext());
            context.put(REQUEST_CONTEXT, reqContext);
        }
        reqContext.put(Message.PROTOCOL_HEADERS, message.getProtocolHeaders());
        reqContext.put(Message.REQUEST_URI, message.getRequestUri());
        reqContext.put(Message.ENDPOINT_ADDRESS, message.getEndpointAddress());
        reqContext.put(PROXY_PROPERTY, proxy);

        if (resContext == null) {
            resContext = new HashMap<>();
            context.put(RESPONSE_CONTEXT, resContext);
        }

        message.put(Message.INVOCATION_CONTEXT, context);
        message.putAll(reqContext);
        exchange.putAll(reqContext);
    }

    protected void setPlainOperationNameProperty(Message outMessage, String name) {
        outMessage.getExchange().put("org.apache.cxf.resource.operation.name", name);
    }

    protected static Type getCallbackType(InvocationCallback<?> callback) {
        Class<?> cls = callback.getClass();
        ParameterizedType pt = findCallbackType(cls);
        Type actualType = null;
        for (Type tp : pt.getActualTypeArguments()) {
            actualType = tp;
            break;
        }
        if (actualType instanceof TypeVariable) {
            actualType = InjectionUtils.getSuperType(cls, (TypeVariable<?>)actualType);
        }
        return actualType;
    }

    protected static ParameterizedType findCallbackType(Class<?> cls) {
        if (cls == null || cls == Object.class) {
            return null;
        }
        for (Type c2 : cls.getGenericInterfaces()) {
            if (c2 instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType)c2;
                if (InvocationCallback.class.equals(pt.getRawType())) {
                    return pt;
                }
            }
        }
        return findCallbackType(cls.getSuperclass());
    }

    protected static Class<?> getCallbackClass(Type outType) {
        Class<?> respClass = null;
        if (outType instanceof Class) {
            respClass = (Class<?>)outType;
        } else if (outType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)outType;
            if (pt.getRawType() instanceof Class) {
                respClass = (Class<?>)pt.getRawType();
            }
        } else if (outType == null) {
            respClass = Response.class;
        }
        return respClass;
    }

    protected void resetResponseStateImmediatelyIfNeeded() {
        if (state instanceof ThreadLocalClientState
            && cfg.isResetThreadLocalStateImmediately()) {
            state.reset();
        }
    }

    protected abstract class AbstractBodyWriter extends AbstractOutDatabindingInterceptor {

        public AbstractBodyWriter() {
            super(Phase.WRITE);
        }

        @Override
        public void handleMessage(Message outMessage) throws Fault {
            MessageContentsList objs = MessageContentsList.getContentsList(outMessage);
            if (objs == null || objs.isEmpty()) {
                return;
            }

            OutputStream os = outMessage.getContent(OutputStream.class);
            if (os == null) {
                XMLStreamWriter writer = outMessage.getContent(XMLStreamWriter.class);
                if (writer == null) {
                    return;
                }
            }

            Object body = objs.get(0);
            Annotation[] customAnns = (Annotation[])outMessage.get(Annotation.class.getName());
            Type t = outMessage.get(Type.class);
            doWriteBody(outMessage, body, t, customAnns, os);
        }

        protected abstract void doWriteBody(Message outMessage,
                                            Object body,
                                            Type bodyType,
                                            Annotation[] customAnns,
                                            OutputStream os) throws Fault;
    }

    private class RetryableImpl implements Retryable {

        @Override
        public Object[] invoke(BindingOperationInfo oi, Object[] params, Map<String, Object> context,
                               Exchange exchange) throws Exception {
            return AbstractClient.this.retryInvoke(oi, params, context, exchange);
        }

    }
    private static class ConnectionFaultInterceptor extends AbstractPhaseInterceptor<Message> {
        ConnectionFaultInterceptor() {
            super(Phase.PRE_STREAM);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            if (!message.getExchange().isSynchronous()) {
                Throwable ex = message.getContent(Exception.class);
                if (ex == null) {
                    ex = message.getExchange().get(Exception.class);
                }
                if (ex != null) {
                    JaxrsClientCallback<?> cb = message.getExchange().get(JaxrsClientCallback.class);
                    if (ex instanceof Fault) {
                        ex = ex.getCause();
                    }
                    ex = ex instanceof ProcessingException ? ex : new ProcessingException(ex);
                    cb.handleException(message, ex);
                }
            }
        }
    }

    protected abstract class AbstractClientAsyncResponseInterceptor extends AbstractPhaseInterceptor<Message> {
        AbstractClientAsyncResponseInterceptor() {
            super(Phase.UNMARSHAL);
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            synchronized (message.getExchange()) {
                message.getExchange().put("IN_CHAIN_COMPLETE", Boolean.TRUE);
                message.getExchange().notifyAll();
            }
            if (message.getExchange().isSynchronous()) {
                return;
            }
            handleAsyncResponse(message);
        }

        @Override
        public void handleFault(Message message) {
            synchronized (message.getExchange()) {
                message.getExchange().put("IN_CHAIN_COMPLETE", Boolean.TRUE);
                message.getExchange().notifyAll();
            }
            if (message.getExchange().isSynchronous()) {
                return;
            }
            handleAsyncFault(message);
        }

        private void handleAsyncResponse(Message message) {
            JaxrsClientCallback<?> cb = message.getExchange().get(JaxrsClientCallback.class);
            Response r = null;
            try {
                Object[] results = preProcessResult(message);
                if (results != null && results.length == 1) {
                    r = (Response)results[0];
                }
            } catch (Exception ex) {
                Throwable t = ex instanceof WebApplicationException
                    ? (WebApplicationException)ex
                    : ex instanceof ProcessingException
                    ? (ProcessingException)ex : new ProcessingException(ex);
                cb.handleException(message, t);
                return;
            }
            doHandleAsyncResponse(message, r, cb);
        }

        protected abstract void doHandleAsyncResponse(Message message, Response r, JaxrsClientCallback<?> cb);

        protected void closeAsyncResponseIfPossible(Response r, Message outMessage, JaxrsClientCallback<?> cb) {
            if (responseStreamCanBeClosed(outMessage, cb.getResponseClass())) {
                r.close();
            }
        }

        protected void handleAsyncFault(Message message) {
        }
    }
}
