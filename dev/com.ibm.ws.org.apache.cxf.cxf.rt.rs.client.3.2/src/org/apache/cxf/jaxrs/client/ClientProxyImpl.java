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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PrimitiveUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.FormUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Proxy-based client implementation
 *
 */
public class ClientProxyImpl extends AbstractClient implements
    InvocationHandlerAware, InvocationHandler {

    protected static final Logger LOG = LogUtils.getL7dLogger(ClientProxyImpl.class);
    protected static final ResourceBundle BUNDLE = BundleUtils.getBundle(ClientProxyImpl.class);
    protected static final String SLASH = "/";
    protected static final String BUFFER_PROXY_RESPONSE = "buffer.proxy.response";
    protected static final String PROXY_METHOD_PARAM_BODY_INDEX = "proxy.method.parameter.body.index";

    protected ClassResourceInfo cri;
    protected ClassLoader proxyLoader;
    protected boolean inheritHeaders;
    protected boolean isRoot;
    protected Map<String, Object> valuesMap = Collections.emptyMap();
    protected BodyWriter bodyWriter = new BodyWriter();
    protected Client proxy;
    public ClientProxyImpl(URI baseURI,
                           ClassLoader loader,
                           ClassResourceInfo cri,
                           boolean isRoot,
                           boolean inheritHeaders,
                           Object... varValues) {
        this(new LocalClientState(baseURI), loader, cri, isRoot, inheritHeaders, varValues);
    }

    public ClientProxyImpl(ClientState initialState,
                           ClassLoader loader,
                           ClassResourceInfo cri,
                           boolean isRoot,
                           boolean inheritHeaders,
                           Object... varValues) {
        super(initialState);
        this.proxyLoader = loader;
        this.cri = cri;
        this.isRoot = isRoot;
        this.inheritHeaders = inheritHeaders;
        initValuesMap(varValues);
        cfg.getInInterceptors().add(new ClientAsyncResponseInterceptor());
    }

    void setProxyClient(Client client) {
        this.proxy = client;
    }

    private void initValuesMap(Object... varValues) {
        if (isRoot) {
            List<String> vars = cri.getURITemplate().getVariables();
            valuesMap = new LinkedHashMap<>();
            for (int i = 0; i < vars.size(); i++) {
                if (varValues.length > 0) {
                    if (i < varValues.length) {
                        valuesMap.put(vars.get(i), varValues[i]);
                    } else {
                        org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message(
                             "ROOT_VARS_MISMATCH", BUNDLE, vars.size(), varValues.length);
                        LOG.info(msg.toString());
                        break;
                    }
                } else {
                    valuesMap.put(vars.get(i), "");
                }
            }
        }
    }

    /**
     * Updates the current state if Client method is invoked, otherwise
     * does the remote invocation or returns a new proxy if subresource
     * method is invoked. Can throw an expected exception if ResponseExceptionMapper
     * is registered
     */
    @Override
    @Trivial //Liberty change - required to avoid tracing StackOverflowException
    public Object invoke(Object o, Method m, Object[] params) throws Throwable {

        Class<?> declaringClass = m.getDeclaringClass();
        if (Client.class == declaringClass || InvocationHandlerAware.class == declaringClass
            || Object.class == declaringClass) {
            return m.invoke(this, params);
        }
        resetResponse();
        OperationResourceInfo ori = cri.getMethodDispatcher().getOperationResourceInfo(m);
        if (ori == null) {
            reportInvalidResourceMethod(m, "INVALID_RESOURCE_METHOD");
        }

        MultivaluedMap<ParameterType, Parameter> types = getParametersInfo(m, params, ori);
        List<Parameter> beanParamsList = getParameters(types, ParameterType.BEAN);

        int bodyIndex = getBodyIndex(types, ori);

        List<Object> pathParams = getPathParamValues(m, params, types, beanParamsList, ori, bodyIndex);

        UriBuilder builder = getCurrentBuilder().clone();
        if (isRoot) {
            addNonEmptyPath(builder, ori.getClassResourceInfo().getURITemplate().getValue());
        }
        addNonEmptyPath(builder, ori.getURITemplate().getValue());

        handleMatrixes(m, params, types, beanParamsList, builder);
        handleQueries(m, params, types, beanParamsList, builder);

        URI uri = builder.buildFromEncoded(pathParams.toArray()).normalize();

        MultivaluedMap<String, String> headers = getHeaders();
        MultivaluedMap<String, String> paramHeaders = new MetadataMap<>();
        handleHeaders(m, params, paramHeaders, beanParamsList, types);
        handleCookies(m, params, paramHeaders, beanParamsList, types);

        if (ori.isSubResourceLocator()) {
            ClassResourceInfo subCri = cri.getSubResource(m.getReturnType(), m.getReturnType());
            if (subCri == null) {
                reportInvalidResourceMethod(m, "INVALID_SUBRESOURCE");
            }

            MultivaluedMap<String, String> subHeaders = paramHeaders;
            if (inheritHeaders) {
                subHeaders.putAll(headers);
            }

            ClientState newState = getState().newState(uri, subHeaders,
                 getTemplateParametersMap(ori.getURITemplate(), pathParams));
            ClientProxyImpl proxyImpl =
                new ClientProxyImpl(newState, proxyLoader, subCri, false, inheritHeaders);
            proxyImpl.setConfiguration(getConfiguration());
            return JAXRSClientFactory.createProxy(m.getReturnType(), proxyLoader, proxyImpl);
        }
        headers.putAll(paramHeaders);

        getState().setTemplates(getTemplateParametersMap(ori.getURITemplate(), pathParams));

        Object body = null;
        if (bodyIndex != -1) {
            body = params[bodyIndex];
            if (body == null) {
                bodyIndex = -1;
            }
        } else if (types.containsKey(ParameterType.FORM))  {
            body = handleForm(m, params, types, beanParamsList);
        } else if (types.containsKey(ParameterType.REQUEST_BODY))  {
            body = handleMultipart(types, ori, params);
        }

        setRequestHeaders(headers, ori, types.containsKey(ParameterType.FORM),
            body == null ? null : body.getClass(), m.getReturnType());

        try {
            return doChainedInvocation(uri, headers, ori, params, body, bodyIndex, null, null);
        } finally {
            resetResponseStateImmediatelyIfNeeded();
        }

    }

    protected void addNonEmptyPath(UriBuilder builder, String pathValue) {
        if (!SLASH.equals(pathValue)) {
            builder.path(pathValue);
        }
    }

    protected MultivaluedMap<ParameterType, Parameter> getParametersInfo(Method m,
        Object[] params, OperationResourceInfo ori) {
        MultivaluedMap<ParameterType, Parameter> map = new MetadataMap<>();

        List<Parameter> parameters = ori.getParameters();
        if (parameters.isEmpty()) {
            return map;
        }
        int requestBodyParam = 0;
        int multipartParam = 0;
        for (Parameter p : parameters) {
            if (isIgnorableParameter(m, p)) {
                continue;
            }
            if (p.getType() == ParameterType.REQUEST_BODY) {
                requestBodyParam++;
                if (getMultipart(ori, p.getIndex()) != null) {
                    multipartParam++;
                }
            }
            map.add(p.getType(), p);
        }

        if (map.containsKey(ParameterType.REQUEST_BODY)) {
            if (requestBodyParam > 1 && requestBodyParam != multipartParam) {
                reportInvalidResourceMethod(ori.getMethodToInvoke(), "SINGLE_BODY_ONLY");
            }
            if (map.containsKey(ParameterType.FORM)) {
                reportInvalidResourceMethod(ori.getMethodToInvoke(), "ONLY_FORM_ALLOWED");
            }
        }
        return map;
    }

    protected boolean isIgnorableParameter(Method m, Parameter p) {
        if (p.getType() == ParameterType.CONTEXT) {
            return true;
        }
        return p.getType() == ParameterType.REQUEST_BODY
            && m.getParameterTypes()[p.getIndex()] == AsyncResponse.class;
    }

    protected static int getBodyIndex(MultivaluedMap<ParameterType, Parameter> map,
                                    OperationResourceInfo ori) {
        List<Parameter> list = map.get(ParameterType.REQUEST_BODY);
        int index = list == null || list.size() > 1 ? -1 : list.get(0).getIndex();
        if (ori.isSubResourceLocator() && index != -1) {
            reportInvalidResourceMethod(ori.getMethodToInvoke(), "NO_BODY_IN_SUBRESOURCE");
        }
        return index;
    }

    protected void checkResponse(Method m, Response r, Message inMessage) throws Throwable {
        Throwable t = null;
        int status = r.getStatus();

        if (status >= 300) {
            Class<?>[] exTypes = m.getExceptionTypes();
            if (exTypes.length == 0) {
                exTypes = new Class<?>[]{WebApplicationException.class};
            }
            for (Class<?> exType : exTypes) {
                ResponseExceptionMapper<?> mapper = findExceptionMapper(inMessage, exType);
                if (mapper != null) {
                    t = mapper.fromResponse(r);
                    if (t != null) {
                        throw t;
                    }
                }
            }

            if ((t == null) && (m.getReturnType() == Response.class) && (m.getExceptionTypes().length == 0)) {
                return;
            }

            t = convertToWebApplicationException(r);

            if (inMessage.getExchange().get(Message.RESPONSE_CODE) == null) {
                throw t;
            }

            Endpoint ep = inMessage.getExchange().getEndpoint();
            inMessage.getExchange().put(InterceptorProvider.class, getConfiguration());
            inMessage.setContent(Exception.class, new Fault(t));
            inMessage.getInterceptorChain().abort();
            if (ep.getInFaultObserver() != null) {
                ep.getInFaultObserver().onMessage(inMessage);
            }

            throw t;

        }
    }

    protected static ResponseExceptionMapper<?> findExceptionMapper(Message message, Class<?> exType) {
        ClientProviderFactory pf = ClientProviderFactory.getInstance(message);
        return pf.createResponseExceptionMapper(message, exType);
    }

    protected MultivaluedMap<String, String> setRequestHeaders(MultivaluedMap<String, String> headers,
                                                             OperationResourceInfo ori,
                                                             boolean formParams,
                                                             Class<?> bodyClass,
                                                             Class<?> responseClass) {
        if (headers.getFirst(HttpHeaders.CONTENT_TYPE) == null) {
            if (formParams || bodyClass != null && MultivaluedMap.class.isAssignableFrom(bodyClass)) {
                headers.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            } else {
                String ctType = null;
                List<MediaType> consumeTypes = ori.getConsumeTypes();
                if (!consumeTypes.isEmpty() && !consumeTypes.get(0).equals(MediaType.WILDCARD_TYPE)) {
                    ctType = JAXRSUtils.mediaTypeToString(ori.getConsumeTypes().get(0));
                }
                if (ctType != null) {
                    headers.putSingle(HttpHeaders.CONTENT_TYPE, ctType);
                }
            }
        }

        List<MediaType> accepts = getAccept(headers);
        if (accepts == null) {
            boolean produceWildcard = ori.getProduceTypes().isEmpty()
                || ori.getProduceTypes().get(0).equals(MediaType.WILDCARD_TYPE);
            if (produceWildcard) {
                accepts = InjectionUtils.isPrimitive(responseClass)
                    ? Collections.singletonList(MediaType.TEXT_PLAIN_TYPE)
                    : Collections.singletonList(MediaType.APPLICATION_XML_TYPE);
            } else if (responseClass == Void.class || responseClass == Void.TYPE) {
                accepts = Collections.singletonList(MediaType.WILDCARD_TYPE);
            } else {
                accepts = ori.getProduceTypes();
            }

            for (MediaType mt : accepts) {
                headers.add(HttpHeaders.ACCEPT, JAXRSUtils.mediaTypeToString(mt));
            }
        }

        return headers;
    }

    protected List<MediaType> getAccept(MultivaluedMap<String, String> allHeaders) {
        List<String> headers = allHeaders.get(HttpHeaders.ACCEPT);
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        return headers.stream().
                flatMap(header -> JAXRSUtils.parseMediaTypes(header).stream()).collect(Collectors.toList());
    }

    protected List<Object> getPathParamValues(Method m,
                                            Object[] params,
                                            MultivaluedMap<ParameterType, Parameter> map,
                                            List<Parameter> beanParams,
                                            OperationResourceInfo ori,
                                            int bodyIndex) {
        List<Object> list = new LinkedList<>();

        List<String> methodVars = ori.getURITemplate().getVariables();
        List<Parameter> paramsList = getParameters(map, ParameterType.PATH);
        Map<String, BeanPair> beanParamValues = new HashMap<>(beanParams.size());
        beanParams.forEach(p -> {
            beanParamValues.putAll(getValuesFromBeanParam(params[p.getIndex()], PathParam.class));
        });
        if (!beanParamValues.isEmpty() && !methodVars.containsAll(beanParamValues.keySet())) {
            List<String> classVars = ori.getClassResourceInfo().getURITemplate().getVariables();
            classVars.forEach(classVar -> {
                BeanPair pair = beanParamValues.get(classVar);
                if (pair != null) {
                    Object paramValue = convertParamValue(pair.getValue(), pair.getAnns());
                    if (isRoot) {
                        valuesMap.put(classVar, paramValue);
                    } else {
                        list.add(paramValue);
                    }
                }
            });
        }
        if (isRoot) {
            list.addAll(valuesMap.values());
        }


        Map<String, Parameter> paramsMap = new LinkedHashMap<>();
        paramsList.forEach(p -> {
            if (p.getName().length() == 0) {
                MultivaluedMap<String, Object> values = InjectionUtils.extractValuesFromBean(params[p.getIndex()], "");
                methodVars.forEach(var -> {
                    list.addAll(values.get(var));
                });
            } else {
                paramsMap.put(p.getName(), p);
            }
        });

        Object requestBody = bodyIndex == -1 ? null : params[bodyIndex];
        methodVars.forEach(varName -> {
            Parameter p = paramsMap.remove(varName);
            if (p != null) {
                list.add(convertParamValue(params[p.getIndex()],
                        m.getParameterTypes()[p.getIndex()],
                        getParamAnnotations(m, p)));
            } else if (beanParamValues.containsKey(varName)) {
                BeanPair pair = beanParamValues.get(varName);
                list.add(convertParamValue(pair.getValue(), pair.getAnns()));
            } else if (requestBody != null) {
                try {
                    Method getter = requestBody.getClass().
                            getMethod("get" + StringUtils.capitalize(varName), new Class<?>[]{});
                    list.add(getter.invoke(requestBody, new Object[]{}));
                } catch (Exception ex) {
                    // continue
                }
            }
        });

        for (Parameter p : paramsMap.values()) {
            if (valuesMap.containsKey(p.getName())) {
                int index = 0;
                for (Iterator<String> it = valuesMap.keySet().iterator(); it.hasNext(); index++) {
                    if (it.next().equals(p.getName()) && index < list.size()) {
                        list.remove(index);
                        list.add(index, convertParamValue(params[p.getIndex()], null));
                        break;
                    }
                }
            }
        }


        return list;
    }

    protected static Annotation[] getParamAnnotations(Method m, Parameter p) {
        return m.getParameterAnnotations()[p.getIndex()];
    }

    protected static List<Parameter> getParameters(MultivaluedMap<ParameterType, Parameter> map,
                                           ParameterType key) {
        return map.get(key) == null ? Collections.emptyList() : map.get(key);
    }

    protected void handleQueries(Method m,
                               Object[] params,
                               MultivaluedMap<ParameterType, Parameter> map,
                               List<Parameter> beanParams,
                               UriBuilder ub) {
        List<Parameter> qs = getParameters(map, ParameterType.QUERY);
        qs.stream().
                filter(p -> params[p.getIndex()] != null).
                forEachOrdered(p -> {
                    addMatrixQueryParamsToBuilder(ub, p.getName(), ParameterType.QUERY,
                            getParamAnnotations(m, p), params[p.getIndex()]);
                });
        beanParams.stream().
                map(p -> getValuesFromBeanParam(params[p.getIndex()], QueryParam.class)).
                forEachOrdered(values -> {
                    values.forEach((key, value) -> {
                        if (value != null) {
                            addMatrixQueryParamsToBuilder(ub, key, ParameterType.QUERY,
                                    value.getAnns(), value.getValue());
                        }
                    });
                });
    }

    protected Map<String, BeanPair> getValuesFromBeanParam(Object bean, Class<? extends Annotation> annClass) {
        Map<String, BeanPair> values = new HashMap<>();
        getValuesFromBeanParam(bean, annClass, values);
        return values;
    }

    protected Map<String, BeanPair> getValuesFromBeanParam(Object bean,
                                                         Class<? extends Annotation> annClass,
                                                         Map<String, BeanPair> values) {
        boolean completeFieldIntrospectionNeeded = false;
        for (Method m : bean.getClass().getMethods()) {
            if (m.getName().startsWith("set")) {
                try {
                    String propertyName = m.getName().substring(3);
                    Annotation methodAnnotation = m.getAnnotation(annClass);
                    boolean beanParam = m.getAnnotation(BeanParam.class) != null;
                    if (methodAnnotation != null || beanParam) {
                        Method getter = bean.getClass().getMethod("get" + propertyName, new Class<?>[]{});
                        Object value = getter.invoke(bean, new Object[]{});
                        if (value != null) {
                            if (methodAnnotation != null) {
                                String annotationValue = AnnotationUtils.getAnnotationValue(methodAnnotation);
                                values.put(annotationValue, new BeanPair(value, m.getParameterAnnotations()[0]));
                            } else {
                                getValuesFromBeanParam(value, annClass, values);
                            }
                        }
                    } else {
                        String fieldName = StringUtils.uncapitalize(propertyName);
                        Field f = InjectionUtils.getDeclaredField(bean.getClass(), fieldName);
                        if (f == null) {
                            completeFieldIntrospectionNeeded = true;
                            continue;
                        }
                        boolean jaxrsParamAnnAvailable = getValuesFromBeanParamField(bean, f, annClass, values);
                        if (!jaxrsParamAnnAvailable && f.getAnnotation(BeanParam.class) != null) {
                            Object value = ReflectionUtil.accessDeclaredField(f, bean, Object.class);
                            if (value != null) {
                                getValuesFromBeanParam(value, annClass, values);
                            }
                        }
                    }
                } catch (Throwable t) {
                    // ignore
                }
            }
            if (completeFieldIntrospectionNeeded) {
                for (Field f : bean.getClass().getDeclaredFields()) {
                    boolean jaxrsParamAnnAvailable = getValuesFromBeanParamField(bean, f, annClass, values);
                    if (!jaxrsParamAnnAvailable && f.getAnnotation(BeanParam.class) != null) {
                        Object value = ReflectionUtil.accessDeclaredField(f, bean, Object.class);
                        if (value != null) {
                            getValuesFromBeanParam(value, annClass, values);
                        }
                    }
                }
            }
        }
        return values;
    }

    protected boolean getValuesFromBeanParamField(Object bean,
                                                Field f,
                                                Class<? extends Annotation> annClass,
                                                Map<String, BeanPair> values) {
        boolean jaxrsParamAnnAvailable = false;
        Annotation fieldAnnotation = f.getAnnotation(annClass);
        if (fieldAnnotation != null) {
            jaxrsParamAnnAvailable = true;
            Object value = ReflectionUtil.accessDeclaredField(f, bean, Object.class);
            if (value != null) {
                String annotationValue = AnnotationUtils.getAnnotationValue(fieldAnnotation);
                values.put(annotationValue, new BeanPair(value, f.getAnnotations()));
            }
        }
        return jaxrsParamAnnAvailable;
    }

    protected void handleMatrixes(Method m,
                                Object[] params,
                                MultivaluedMap<ParameterType, Parameter> map,
                                List<Parameter> beanParams,
                                UriBuilder ub) {
        List<Parameter> mx = getParameters(map, ParameterType.MATRIX);
        mx.stream().
                filter(p -> params[p.getIndex()] != null).
                forEachOrdered(p -> {
                    addMatrixQueryParamsToBuilder(ub, p.getName(), ParameterType.MATRIX,
                            getParamAnnotations(m, p), params[p.getIndex()]);
                });
        beanParams.stream().
                map(p -> getValuesFromBeanParam(params[p.getIndex()], MatrixParam.class)).
                forEachOrdered(values -> {
                    values.forEach((key, value) -> {
                        if (value != null) {
                            addMatrixQueryParamsToBuilder(ub, key, ParameterType.MATRIX,
                                    value.getAnns(), value.getValue());
                        }
                    });
                });
    }

    protected MultivaluedMap<String, String> handleForm(Method m,
                                                      Object[] params,
                                                      MultivaluedMap<ParameterType, Parameter> map,
                                                      List<Parameter> beanParams) {

        MultivaluedMap<String, String> form = new MetadataMap<>();

        List<Parameter> fm = getParameters(map, ParameterType.FORM);
        fm.forEach(p -> {
            addFormValue(form, p.getName(), params[p.getIndex()], getParamAnnotations(m, p));
        });
        beanParams.stream().
                map(p -> getValuesFromBeanParam(params[p.getIndex()], FormParam.class)).
                forEachOrdered(values -> {
                    values.forEach((key, value) -> {
                        addFormValue(form, key, value.getValue(), value.getAnns());
                    });
                });

        return form;
    }

    protected void addFormValue(MultivaluedMap<String, String> form, String name, Object pValue, Annotation[] anns) {
        if (pValue != null) {
            if (InjectionUtils.isSupportedCollectionOrArray(pValue.getClass())) {
                Collection<?> c = pValue.getClass().isArray()
                    ? Arrays.asList((Object[]) pValue) : (Collection<?>) pValue;
                for (Iterator<?> it = c.iterator(); it.hasNext();) {
                    FormUtils.addPropertyToForm(form, name, convertParamValue(it.next(), anns));
                }
            } else {
                FormUtils.addPropertyToForm(form, name, name.isEmpty()
                                            ? pValue : convertParamValue(pValue, anns));
            }

        }

    }

    protected List<Attachment> handleMultipart(MultivaluedMap<ParameterType, Parameter> map,
                                             OperationResourceInfo ori,
                                             Object[] params) {

        List<Attachment> atts = new LinkedList<>();
        List<Parameter> fm = getParameters(map, ParameterType.REQUEST_BODY);
        fm.forEach(p -> {
            Multipart part = getMultipart(ori, p.getIndex());
            if (part != null) {
                Object partObject = params[p.getIndex()];
                if (partObject != null) {
                    atts.add(new Attachment(part.value(), part.type(), partObject));
                }
            }
        });
        return atts;
    }

    protected void handleHeaders(Method m,
                               Object[] params,
                               MultivaluedMap<String, String> headers,
                               List<Parameter> beanParams,
                               MultivaluedMap<ParameterType, Parameter> map) {
        List<Parameter> hs = getParameters(map, ParameterType.HEADER);
        hs.stream().
                filter(p -> params[p.getIndex()] != null).
                forEachOrdered(p -> {
                    headers.add(p.getName(), convertParamValue(params[p.getIndex()], getParamAnnotations(m, p)));
                });
        beanParams.stream().
                map(p -> getValuesFromBeanParam(params[p.getIndex()], HeaderParam.class)).
                forEachOrdered(values -> {
                    values.forEach((key, value) -> {
                        if (value != null) {
                            headers.add(key, convertParamValue(value.getValue(), value.getAnns()));
                        }
                    });
                });
    }

    protected static Multipart getMultipart(OperationResourceInfo ori, int index) {
        Method aMethod = ori.getAnnotatedMethod();
        return aMethod != null ? AnnotationUtils.getAnnotation(
            aMethod.getParameterAnnotations()[index], Multipart.class) : null;
    }

    protected void handleCookies(Method m,
                               Object[] params,
                               MultivaluedMap<String, String> headers,
                               List<Parameter> beanParams,
                               MultivaluedMap<ParameterType, Parameter> map) {
        List<Parameter> cs = getParameters(map, ParameterType.COOKIE);
        cs.stream().
                filter(p -> params[p.getIndex()] != null).
                forEachOrdered(p -> {
                    headers.add(HttpHeaders.COOKIE,
                            p.getName() + '='
                            + convertParamValue(params[p.getIndex()].toString(), getParamAnnotations(m, p)));
                });
        beanParams.stream().
                map(p -> getValuesFromBeanParam(params[p.getIndex()], CookieParam.class)).
                forEachOrdered(values -> {
                    values.forEach((key, value) -> {
                        if (value != null) {
                            headers.add(HttpHeaders.COOKIE,
                                    key + "=" + convertParamValue(value.getValue(), value.getAnns()));
                        }
                    });
                });
    }
    //CHECKSTYLE:OFF
    protected Object doChainedInvocation(URI uri,
                                       MultivaluedMap<String, String> headers,
                                       OperationResourceInfo ori,
                                       Object[] methodParams,
                                       Object body,
                                       int bodyIndex,
                                       Exchange exchange,
                                       Map<String, Object> invocationContext) throws Throwable {
    //CHECKSTYLE:ON
        Bus configuredBus = getConfiguration().getBus();
        Bus origBus = BusFactory.getAndSetThreadDefaultBus(configuredBus);
        ClassLoaderHolder origLoader = null;
        try {
            ClassLoader loader = configuredBus.getExtension(ClassLoader.class);
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            Message outMessage = createMessage(body, ori, headers, uri,
                                               exchange, invocationContext, true);
            if (bodyIndex != -1) {
                outMessage.put(Type.class, ori.getMethodToInvoke().getGenericParameterTypes()[bodyIndex]);
            }
            outMessage.getExchange().setOneWay(ori.isOneway());
            setSupportOnewayResponseProperty(outMessage);
            outMessage.setContent(OperationResourceInfo.class, ori);
            setPlainOperationNameProperty(outMessage, ori.getMethodToInvoke().getName());
            outMessage.getExchange().put(Method.class, ori.getMethodToInvoke());

            outMessage.put(Annotation.class.getName(),
                           getMethodAnnotations(ori.getAnnotatedMethod(), bodyIndex));

            outMessage.getExchange().put(Message.SERVICE_OBJECT, proxy);
            if (methodParams != null) {
                outMessage.put(List.class, Arrays.asList(methodParams));
            }
            if (body != null) {
                outMessage.put(PROXY_METHOD_PARAM_BODY_INDEX, bodyIndex);
            }
            outMessage.getInterceptorChain().add(bodyWriter);

            Map<String, Object> reqContext = getRequestContext(outMessage);
            reqContext.put(OperationResourceInfo.class.getName(), ori);
            reqContext.put(PROXY_METHOD_PARAM_BODY_INDEX, bodyIndex);

            // execute chain
            InvocationCallback<Object> asyncCallback = checkAsyncCallback(ori, reqContext, outMessage);
            if (asyncCallback != null) {
                return doInvokeAsync(ori, outMessage, asyncCallback);
            }
            doRunInterceptorChain(outMessage);

            Object[] results = preProcessResult(outMessage);
            if (results != null && results.length == 1) {
                return results[0];
            }

            try {
                return handleResponse(outMessage, ori.getClassResourceInfo().getServiceClass());
            } finally {
                completeExchange(outMessage.getExchange(), true);
            }

        } finally {
            if (origLoader != null) {
                origLoader.reset();
            }
            if (origBus != configuredBus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
        }

    }

    protected Message createMessage(Object body,
                                    OperationResourceInfo ori,
                                    MultivaluedMap<String, String> headers,
                                    URI currentURI,
                                    Exchange exchange,
                                    Map<String, Object> invocationContext,
                                    boolean isProxy) {
        return createMessage(body, ori.getHttpMethod(), headers, currentURI,
                             exchange, invocationContext, isProxy);
    }

    protected InvocationCallback<Object> checkAsyncCallback(OperationResourceInfo ori,
                                                            Map<String, Object> reqContext,
                                                            Message outMessage) {
        Object callbackProp = reqContext.get(InvocationCallback.class.getName());
        if (callbackProp != null) {
            if (callbackProp instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<InvocationCallback<Object>> callbacks = (Collection<InvocationCallback<Object>>)callbackProp;
                for (InvocationCallback<Object> callback : callbacks) {
                    if (doCheckAsyncCallback(ori, callback) != null) {
                        return callback;
                    }
                }
            } else {
                @SuppressWarnings("unchecked")
                InvocationCallback<Object> callback = (InvocationCallback<Object>)callbackProp;
                return doCheckAsyncCallback(ori, callback);
            }
        }
        return null;
    }

    protected InvocationCallback<Object> doCheckAsyncCallback(OperationResourceInfo ori,
                                                            InvocationCallback<Object> callback) {
        Type callbackOutType = getCallbackType(callback);
        Class<?> callbackRespClass = getCallbackClass(callbackOutType);

        Class<?> methodReturnType = ori.getMethodToInvoke().getReturnType();
        if (Object.class == callbackRespClass
            || callbackRespClass.isAssignableFrom(methodReturnType)
            || PrimitiveUtils.canPrimitiveTypeBeAutoboxed(methodReturnType, callbackRespClass)) {
            return callback;
        }
        return null;
    }

    protected Object doInvokeAsync(OperationResourceInfo ori, 
                                   Message outMessage,
                                   InvocationCallback<Object> asyncCallback) {
        outMessage.getExchange().setSynchronous(false);
        setAsyncMessageObserverIfNeeded(outMessage.getExchange());
        JaxrsClientCallback<?> cb = newJaxrsClientCallback(asyncCallback, outMessage, // Liberty change
            ori.getMethodToInvoke().getReturnType(), ori.getMethodToInvoke().getGenericReturnType());
        outMessage.getExchange().put(JaxrsClientCallback.class, cb);
        doRunInterceptorChain(outMessage);

        return null;
    }

    protected JaxrsClientCallback<?> newJaxrsClientCallback(InvocationCallback<Object> asyncCallback,
                                                            Message outMessage, //Liberty change
                                                            Class<?> responseClass,
                                                            Type outGenericType) {
        return new JaxrsClientCallback<>(asyncCallback, responseClass, outGenericType);
    }

    @Override
    protected Object retryInvoke(URI newRequestURI,
                                 MultivaluedMap<String, String> headers,
                                 Object body,
                                 Exchange exchange,
                                 Map<String, Object> invContext) throws Throwable {

        Map<String, Object> reqContext = CastUtils.cast((Map<?, ?>)invContext.get(REQUEST_CONTEXT));
        int bodyIndex = body != null ? (Integer)reqContext.get(PROXY_METHOD_PARAM_BODY_INDEX) : -1;
        OperationResourceInfo ori =
            (OperationResourceInfo)reqContext.get(OperationResourceInfo.class.getName());
        return doChainedInvocation(newRequestURI, headers, ori, null,
                                   body, bodyIndex, exchange, invContext);
    }

    protected Object handleResponse(Message outMessage, Class<?> serviceCls)
        throws Throwable {
        try {
            Response r = setResponseBuilder(outMessage, outMessage.getExchange()).build();
            ((ResponseImpl)r).setOutMessage(outMessage);
            getState().setResponse(r);

            Method method = outMessage.getExchange().get(Method.class);
            checkResponse(method, r, outMessage);
            if (method.getReturnType() == Void.class || method.getReturnType() == Void.TYPE) {
                return null;
            }
            if (method.getReturnType() == Response.class
                && (r.getEntity() == null || InputStream.class.isAssignableFrom(r.getEntity().getClass())
                    && ((InputStream)r.getEntity()).available() == 0)) {
                return r;
            }
            if (PropertyUtils.isTrue(super.getConfiguration().getResponseContext().get(BUFFER_PROXY_RESPONSE))) {
                r.bufferEntity();
            }

            Class<?> returnType = getReturnType(method, outMessage);
            Type genericType =
                InjectionUtils.processGenericTypeIfNeeded(serviceCls,
                                                          returnType,
                                                          method.getGenericReturnType());
            returnType = InjectionUtils.updateParamClassToTypeIfNeeded(returnType, genericType);
            return readBody(r,
                            outMessage,
                            returnType,
                            genericType,
                            method.getDeclaredAnnotations());
        } finally {
            ClientProviderFactory.getInstance(outMessage).clearThreadLocalProxies();
        }
    }

    protected Class<?> getReturnType(Method method, Message outMessage) {
        return method.getReturnType();
    }

    public Object getInvocationHandler() {
        return this;
    }

    protected static void reportInvalidResourceMethod(Method m, String name) {
        org.apache.cxf.common.i18n.Message errorMsg =
            new org.apache.cxf.common.i18n.Message(name,
                                                   BUNDLE,
                                                   m.getDeclaringClass().getName(),
                                                   m.getName());
        LOG.severe(errorMsg.toString());
        throw new ProcessingException(errorMsg.toString());
    }

    protected static Annotation[] getMethodAnnotations(Method aMethod, int bodyIndex) {
        return aMethod == null || bodyIndex == -1 ? new Annotation[0]
            : aMethod.getParameterAnnotations()[bodyIndex];
    }

    protected class BodyWriter extends AbstractBodyWriter {

        @Override
        protected void doWriteBody(Message outMessage,
                                   Object body,
                                   Type bodyType,
                                   Annotation[] customAnns,
                                   OutputStream os) throws Fault {


            OperationResourceInfo ori = outMessage.getContent(OperationResourceInfo.class);
            if (ori == null) {
                return;
            }

            Method method = ori.getMethodToInvoke();
            int bodyIndex = (Integer)outMessage.get(PROXY_METHOD_PARAM_BODY_INDEX);

            Annotation[] anns = customAnns != null ? customAnns
                : getMethodAnnotations(ori.getAnnotatedMethod(), bodyIndex);
            try {
                if (bodyIndex != -1) {
                    Class<?> paramClass = method.getParameterTypes()[bodyIndex];
                    Class<?> bodyClass =
                        paramClass.isAssignableFrom(body.getClass()) ? paramClass : body.getClass();
                    Type genericType = method.getGenericParameterTypes()[bodyIndex];
                    if (bodyType != null) {
                        genericType = bodyType;
                    }
                    genericType = InjectionUtils.processGenericTypeIfNeeded(
                        ori.getClassResourceInfo().getServiceClass(), bodyClass, genericType);
                    bodyClass = InjectionUtils.updateParamClassToTypeIfNeeded(bodyClass, genericType);
                    writeBody(body, outMessage, bodyClass, genericType, anns, os);
                } else {
                    Type paramType = body.getClass();
                    if (bodyType != null) {
                        paramType = bodyType;
                    }
                    writeBody(body, outMessage, body.getClass(), paramType,
                              anns, os);
                }
            } catch (Exception ex) {
                throw new Fault(ex);
            }

        }

    }

    protected static class BeanPair {
        protected Object value;
        protected Annotation[] anns;
        BeanPair(Object value, Annotation[] anns) {
            this.value = value;
            this.anns = anns;
        }
        public Object getValue() {
            return value;
        }
        public Annotation[] getAnns() {
            return anns;
        }
    }
    class ClientAsyncResponseInterceptor extends AbstractClientAsyncResponseInterceptor {
        @FFDCIgnore(Throwable.class)
        @Override
        protected void doHandleAsyncResponse(Message message, Response r, JaxrsClientCallback<?> cb) {
            try {
                Object entity = handleResponse(message.getExchange().getOutMessage(),
                                               cb.getResponseClass());
                cb.handleResponse(message, new Object[] {entity});
            } catch (Throwable t) {
                cb.handleException(message, t);
            } finally {
                completeExchange(message.getExchange(), false);
                closeAsyncResponseIfPossible(r, message, cb);
            }
        }
    }
}
