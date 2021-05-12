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

package org.apache.cxf.jaxrs.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.json.bind.JsonbException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.ReaderInputStream;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.ext.DefaultMethod;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.MessageContextImpl;
import org.apache.cxf.jaxrs.ext.ProtocolHeaders;
import org.apache.cxf.jaxrs.ext.ProtocolHeadersImpl;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.AsyncResponseImpl;
import org.apache.cxf.jaxrs.impl.ContainerRequestContextImpl;
import org.apache.cxf.jaxrs.impl.ContainerResponseContextImpl;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.MediaTypeHeaderProvider;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.jaxrs.impl.ProvidersImpl;
import org.apache.cxf.jaxrs.impl.ReaderInterceptorContextImpl;
import org.apache.cxf.jaxrs.impl.ReaderInterceptorMBR;
import org.apache.cxf.jaxrs.impl.RequestImpl;
import org.apache.cxf.jaxrs.impl.ResourceContextImpl;
import org.apache.cxf.jaxrs.impl.ResourceInfoImpl;
import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.impl.SecurityContextImpl;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.jaxrs.impl.WriterInterceptorContextImpl;
import org.apache.cxf.jaxrs.impl.WriterInterceptorMBW;
import org.apache.cxf.jaxrs.model.BeanParamInfo;
import org.apache.cxf.jaxrs.model.BeanResourceInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfoComparator;
import org.apache.cxf.jaxrs.model.MethodInvocationInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfoComparator;
import org.apache.cxf.jaxrs.model.OperationResourceInfoStack;
import org.apache.cxf.jaxrs.model.Parameter;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.Service;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.JaxRsRuntimeException;
import com.ibm.ws.jaxrs20.multipart.impl.AttachmentImpl;

public final class JAXRSUtils {

    public static final MediaType ALL_TYPES = new MediaType();
    public static final String ROOT_RESOURCE_CLASS = "root.resource.class";
    public static final String IGNORE_MESSAGE_WRITERS = "ignore.message.writers";
    public static final String ROOT_INSTANCE = "service.root.instance";
    public static final String ROOT_PROVIDER = "service.root.provider";
    public static final String EXCEPTION_FROM_MAPPER = "exception.from.mapper";
    public static final String SECOND_JAXRS_EXCEPTION = "second.jaxrs.exception";
    public static final String PARTIAL_HIERARCHICAL_MEDIA_SUBTYPE_CHECK = "media.subtype.partial.check";
    public static final String DOC_LOCATION = "wadl.location";
    public static final String MEDIA_TYPE_Q_PARAM = "q";
    public static final String MEDIA_TYPE_QS_PARAM = "qs";
    private static final String MEDIA_TYPE_DISTANCE_PARAM = "d";
    private static final String DEFAULT_CONTENT_TYPE = "default.content.type";
    private static final String KEEP_SUBRESOURCE_CANDIDATES = "keep.subresource.candidates";
    // using IBM RAS Tr for injected entry/exit with faster trace guards,
    // but using java.util.logging.Logger for translated messages from CXF.
    private static final TraceComponent tc = Tr.register(JAXRSUtils.class);
    //private static final Logger LOG = LogUtils.getL7dLogger(JAXRSUtils.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSUtils.class);
    private static final String PATH_SEGMENT_SEP = "/";
    private static final String REPORT_FAULT_MESSAGE_PROPERTY = "org.apache.cxf.jaxrs.report-fault-message";
    private static final String NO_CONTENT_EXCEPTION = "javax.ws.rs.core.NoContentException";
    private static final String HTTP_CHARSET_PARAM = "charset";
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
    private static final Set<Class<?>> STREAMING_OUT_TYPES = new HashSet<>(
        Arrays.asList(InputStream.class, Reader.class, StreamingOutput.class));

    private JAXRSUtils() {}

    public static boolean isStreamingOutType(Class<?> type) {
        return STREAMING_OUT_TYPES.contains(type);
    }

    public static List<PathSegment> getPathSegments(String thePath, boolean decode) {
        return getPathSegments(thePath, decode, true);
    }

    public static List<PathSegment> getPathSegments(String thePath, boolean decode,
                                                    boolean ignoreLastSlash) {
        List<PathSegment> theList = 
            Arrays.asList(thePath.split("/")).stream()
            .filter(StringUtils.notEmpty())
            .map(p -> new PathSegmentImpl(p, decode))
            .collect(Collectors.toList());
        
        int len = thePath.length();
        if (len > 0 && thePath.charAt(len - 1) == '/') {
            String value = ignoreLastSlash ? "" : "/";
            theList.add(new PathSegmentImpl(value, false));
        }
        return theList;
    }

    private static String[] getUserMediaTypes(Object provider, boolean consumes) {
        String[] values = null;
        if (AbstractConfigurableProvider.class.isAssignableFrom(provider.getClass())) {
            List<String> types = null;
            if (consumes) {
                types = ((AbstractConfigurableProvider) provider).getConsumeMediaTypes();
            } else {
                types = ((AbstractConfigurableProvider) provider).getProduceMediaTypes();
            }
            if (types != null) {
                values = !types.isEmpty() ? types.toArray(new String[0]) : new String[] { "*/*" };
            }
        }
        return values;
    }

    public static List<MediaType> getProviderConsumeTypes(MessageBodyReader<?> provider) {
        String[] values = getUserMediaTypes(provider, true);

        if (values == null) {
            return getConsumeTypes(provider.getClass().getAnnotation(Consumes.class));
        }
        return JAXRSUtils.getMediaTypes(values);
    }

    public static List<MediaType> getProviderProduceTypes(MessageBodyWriter<?> provider) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getProviderProduceTypes", provider);
        }
        List<MediaType> produceTypes;
        String[] values = getUserMediaTypes(provider, false);
        if (values == null) {
            Produces annotation = provider.getClass().getAnnotation(Produces.class);
            if (annotation == null) {
                //check interfaces - this is required by the spec for EJBs
                for (Class<?> intf : provider.getClass().getInterfaces()) {
                    annotation = intf.getAnnotation(Produces.class);
                    if (annotation != null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "getProviderProduceTypes - @Produces annotation not found on impl class, but found on interface " + intf.getName());
                        }
                        break;
                    }
                }
            }

            produceTypes = getProduceTypes(annotation);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "getProviderProduceTypes - return1",  produceTypes);
            }
            return produceTypes;
        }
        produceTypes = JAXRSUtils.getMediaTypes(values);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getProviderProduceTypes - return2", produceTypes);
        }
        return produceTypes;
    }

    public static List<MediaType> getMediaTypes(String[] values) {
        List<MediaType> supportedMimeTypes = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; i++) {
            supportedMimeTypes.addAll(parseMediaTypes(values[i]));
        }
        return supportedMimeTypes;
    }

    public static void injectParameters(OperationResourceInfo ori,
                                        Object requestObject,
                                        Message message) {
        injectParameters(ori, ori.getClassResourceInfo(), requestObject, message);
    }

    @SuppressWarnings("unchecked")
    public static void injectParameters(OperationResourceInfo ori,
                                        BeanResourceInfo bri,
                                        Object requestObject,
                                        Message message) {

        if (bri.isSingleton()
            && (!bri.getParameterMethods().isEmpty() || !bri.getParameterFields().isEmpty())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Injecting request parameters into singleton resource is not thread-safe");
            }
        }
        // Param methods
        MultivaluedMap<String, String> values = (MultivaluedMap<String, String>) message.get(URITemplate.TEMPLATE_PARAMETERS);
        for (Method m : bri.getParameterMethods()) {
            Parameter p = ResourceUtils.getParameter(0, m.getAnnotations(),
                                                     m.getParameterTypes()[0]);
            Object o;

            if (p.getType() == ParameterType.BEAN) {
                o = createBeanParamValue(message, m.getParameterTypes()[0], ori);
            } else {
                o = createHttpParameterValue(p,
                                             m.getParameterTypes()[0],
                                             m.getGenericParameterTypes()[0],
                                             m.getParameterAnnotations()[0],
                                             message,
                                             values,
                                             ori);
            }
            InjectionUtils.injectThroughMethod(requestObject, m, o, message);
        }
        // Param fields
        for (Field f : bri.getParameterFields()) {
            Parameter p = ResourceUtils.getParameter(0, f.getAnnotations(),
                                                     f.getType());
            Object o = null;

            if (p.getType() == ParameterType.BEAN) {
                o = createBeanParamValue(message, f.getType(), ori);
            } else {
                o = createHttpParameterValue(p,
                                             f.getType(),
                                             f.getGenericType(),
                                             f.getAnnotations(),
                                             message,
                                             values,
                                             ori);
            }
            InjectionUtils.injectFieldValue(f, requestObject, o);
        }
    }

    public static Map<ClassResourceInfo, MultivaluedMap<String, String>> selectResourceClass(
                                                                                             List<ClassResourceInfo> resources, String path, Message message) {

        final boolean isFineLevelLoggable = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        if (isFineLevelLoggable) {
            StringBuilder stringBuilder = new StringBuilder();
            for (ClassResourceInfo classResourceInfo : resources) {
                stringBuilder.append(classResourceInfo.getResourceClass());
                stringBuilder.append(" , ");
            }
            String stringClassResourceInfo = stringBuilder.toString();
            Tr.debug(tc, new org.apache.cxf.common.i18n.Message("START_CRI_MATCH", BUNDLE, path + " All known ClassResourceInfo: " + stringClassResourceInfo).toString());
        }

        if (resources.size() == 1) {
            MultivaluedMap<String, String> values = new MetadataMap<>();
            return resources.get(0).getURITemplate().match(path, values) ? Collections.singletonMap(resources.get(0), values) : null;
        }

        SortedMap<ClassResourceInfo, MultivaluedMap<String, String>> candidateList = new TreeMap<ClassResourceInfo, MultivaluedMap<String, String>>(new ClassResourceInfoComparator(message));

        for (ClassResourceInfo cri : resources) {
            MultivaluedMap<String, String> map = new MetadataMap<>();
            if (cri.getURITemplate().match(path, map)) {
                candidateList.put(cri, map);
                if (isFineLevelLoggable) {
                    Tr.debug(tc,
                             new org.apache.cxf.common.i18n.Message("CRI_SELECTED_POSSIBLY", BUNDLE, cri.getServiceClass().getName(), path, cri.getURITemplate().getValue()).toString());
                }
            } else if (isFineLevelLoggable) {
                Tr.debug(tc, new org.apache.cxf.common.i18n.Message("CRI_NO_MATCH", BUNDLE, path, cri.getServiceClass().getName()).toString());
            }
        }

        if (!candidateList.isEmpty()) {
            Map<ClassResourceInfo, MultivaluedMap<String, String>> cris =
                new LinkedHashMap<>(candidateList.size());
            ClassResourceInfo firstCri = null;
            for (Map.Entry<ClassResourceInfo, MultivaluedMap<String, String>> entry : candidateList.entrySet()) {
                ClassResourceInfo cri = entry.getKey();
                if (cris.isEmpty()) {
                    firstCri = cri;
                    cris.put(cri, entry.getValue());
                } else if (firstCri != null
                        && URITemplate.compareTemplates(firstCri.getURITemplate(), cri.getURITemplate()) == 0) {
                    cris.put(cri, entry.getValue());
                } else {
                    break;
                }
                if (isFineLevelLoggable) {
                    Tr.debug(tc, new org.apache.cxf.common.i18n.Message("CRI_SELECTED", BUNDLE, cri.getServiceClass().getName(), path, cri.getURITemplate().getValue()).toString());
                }
            }
            return cris;
        }

        return null;
    }

    public static OperationResourceInfo findTargetMethod(
                                                         Map<ClassResourceInfo, MultivaluedMap<String, String>> matchedResources,
                                                         Message message,
                                                         String httpMethod,
                                                         MultivaluedMap<String, String> matchedValues,
                                                         String requestContentType,
                                                         List<MediaType> acceptContentTypes) {
        return findTargetMethod(matchedResources, message, httpMethod, matchedValues,
                                requestContentType, acceptContentTypes, true, true);
    }

    public static OperationResourceInfo findTargetMethod(
                                                         Map<ClassResourceInfo, MultivaluedMap<String, String>> matchedResources,
                                                         Message message,
                                                         String httpMethod,
                                                         MultivaluedMap<String, String> matchedValues,
                                                         String requestContentType,
                                                         List<MediaType> acceptContentTypes,
                                                         boolean throwException,
                                                         boolean recordMatchedUri) {

        final boolean isFineLevelLoggable = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        final boolean getMethod = HttpMethod.GET.equals(httpMethod);

        MediaType requestType;
        try {
            requestType = toMediaType(requestContentType);
        } catch (IllegalArgumentException ex) {
            throw ExceptionUtils.toNotSupportedException(ex, null);
        }

        SortedMap<OperationResourceInfo, MultivaluedMap<String, String>> candidateList = new TreeMap<OperationResourceInfo, MultivaluedMap<String, String>>(new OperationResourceInfoComparator(message, httpMethod, getMethod, requestType, acceptContentTypes));

        int pathMatched = 0;
        int methodMatched = 0;
        int consumeMatched = 0;

        boolean resourceMethodsAdded = false;
        List<OperationResourceInfo> finalPathSubresources = null;
        for (Map.Entry<ClassResourceInfo, MultivaluedMap<String, String>> rEntry : matchedResources.entrySet()) {
            ClassResourceInfo resource = rEntry.getKey();
            MultivaluedMap<String, String> values = rEntry.getValue();

            String path = getCurrentPath(values);
            if (isFineLevelLoggable) {
                org.apache.cxf.common.i18n.Message msg = new org.apache.cxf.common.i18n.Message("START_OPER_MATCH", BUNDLE, resource.getServiceClass().getName());
                Tr.debug(tc, msg.toString());

            }

            for (OperationResourceInfo ori : resource.getMethodDispatcher().getOperationResourceInfos()) {
                boolean added = false;

                URITemplate uriTemplate = ori.getURITemplate();
                MultivaluedMap<String, String> map = new MetadataMap<>(values);
                if (uriTemplate != null && uriTemplate.match(path, map)) {
                    String finalGroup = map.getFirst(URITemplate.FINAL_MATCH_GROUP);
                    boolean finalPath = StringUtils.isEmpty(finalGroup) || PATH_SEGMENT_SEP.equals(finalGroup);

                    if (ori.isSubResourceLocator()) {
                        candidateList.put(ori, map);
                        if (finalPath) {
                            if (finalPathSubresources == null) {
                                finalPathSubresources = new LinkedList<>();
                            }
                            finalPathSubresources.add(ori);
                        }
                        added = true;
                    } else if (finalPath) {
                        pathMatched++;
                        if (matchHttpMethod(ori.getHttpMethod(), httpMethod)) {
                            methodMatched++;
                            //CHECKSTYLE:OFF
                            if (getMethod || matchConsumeTypes(requestType, ori)) {
                                consumeMatched++;
                                for (MediaType acceptType : acceptContentTypes) {
                                    if (matchProduceTypes(acceptType, ori)) {
                                        candidateList.put(ori, map);
                                        added = true;
                                        resourceMethodsAdded = true;
                                        break;
                                    }
                                }
                            }
                            //CHECKSTYLE:ON
                        }
                    }
                }
                if (isFineLevelLoggable) {
                    if (added) {
                        Tr.debug(tc, new org.apache.cxf.common.i18n.Message("OPER_SELECTED_POSSIBLY", BUNDLE, ori.getMethodToInvoke().getName()).toString());
                    } else {
                        logNoMatchMessage(ori, path, httpMethod, requestType, acceptContentTypes);
                    }
                }
            }
        }
        if (finalPathSubresources != null && resourceMethodsAdded
            && !MessageUtils.getContextualBoolean(message, KEEP_SUBRESOURCE_CANDIDATES, false)) {
            for (OperationResourceInfo key : finalPathSubresources) {
                candidateList.remove(key);
            }
        }
        if (!candidateList.isEmpty()) {
            Map.Entry<OperationResourceInfo, MultivaluedMap<String, String>> firstEntry = candidateList.entrySet().iterator().next();
            matchedValues.clear();
            matchedValues.putAll(firstEntry.getValue());
            OperationResourceInfo ori = firstEntry.getKey();
            if (headMethodPossible(ori.getHttpMethod(), httpMethod)) {
                Tr.info(tc,
                        new org.apache.cxf.common.i18n.Message("GET_INSTEAD_OF_HEAD", BUNDLE, ori.getClassResourceInfo().getServiceClass().getName(), ori.getMethodToInvoke().getName()).toString());
            }
            if (isFineLevelLoggable) {
                Tr.debug(tc,
                         new org.apache.cxf.common.i18n.Message("OPER_SELECTED", BUNDLE, ori.getMethodToInvoke().getName(), ori.getClassResourceInfo().getServiceClass().getName()).toString());
            }
            if (!ori.isSubResourceLocator()) {
                MediaType responseMediaType = intersectSortMediaTypes(acceptContentTypes,
                                                                      ori.getProduceTypes(),
                                                                      false).get(0);
                message.getExchange().put(Message.CONTENT_TYPE, mediaTypeToString(responseMediaType,
                                                                                  MEDIA_TYPE_Q_PARAM,
                                                                                  MEDIA_TYPE_QS_PARAM));
            }
            if (recordMatchedUri) {

                pushOntoStack(ori, matchedValues, message);
            }
            return ori;
        }

        if (!throwException) {
            return null;
        }

        int status;

        // criteria matched the least number of times will determine the error code;
        // priority : path, method, consumes, produces;
        if (pathMatched == 0) {
            status = 404;
        } else if (methodMatched == 0) {
            status = 405;
        } else if (consumeMatched == 0) {
            status = 415;
        } else {
            // Not a single Produces match
            status = 406;
        }
        Map.Entry<ClassResourceInfo, MultivaluedMap<String, String>> firstCri = matchedResources.entrySet().iterator().next();
        String name = firstCri.getKey().isRoot() ? "NO_OP_EXC" : "NO_SUBRESOURCE_METHOD_FOUND";
        org.apache.cxf.common.i18n.Message errorMsg = new org.apache.cxf.common.i18n.Message(name, BUNDLE, message.get(Message.REQUEST_URI), getCurrentPath(firstCri.getValue()), httpMethod, mediaTypeToString(requestType), convertTypesToString(acceptContentTypes));
        if (!"OPTIONS".equalsIgnoreCase(httpMethod)) {
            Level logLevel = getExceptionLogLevel(message, ClientErrorException.class);
            if (logLevel != null && logLevel.intValue() >= Level.WARNING.intValue()) {
                Tr.warning(tc, errorMsg.toString());
            } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, errorMsg.toString());
            }
        }
        Response response = createResponse(getRootResources(message), message, errorMsg.toString(), status, methodMatched == 0);
        throw ExceptionUtils.toHttpException(null, response);

    }

    public static Level getExceptionLogLevel(Message message, Class<? extends WebApplicationException> exClass) {
        Level logLevel = null;
        Object logLevelProp = message.get(exClass.getName() + ".log.level");
        if (logLevelProp != null) {
            if (logLevelProp instanceof Level) {
                logLevel = (Level)logLevelProp;
            } else {
                try {
                    logLevel = Level.parse(logLevelProp.toString());
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        return logLevel;
    }

    private static List<MediaType> intersectSortMediaTypes(List<MediaType> acceptTypes,
                                                          List<MediaType> producesTypes,
                                                          final boolean checkDistance) {
        List<MediaType> all = intersectMimeTypes(acceptTypes, producesTypes, true, checkDistance);
        if (all.size() > 1) {
            all.sort(new Comparator<MediaType>() {

                @Override
                public int compare(MediaType mt1, MediaType mt2) {
                    int result = compareMediaTypes(mt1, mt2, null);
                    if (result == 0) {
                        result = compareQualityAndDistance(mt1, mt2, checkDistance);
                    }
                    return result;
                }

            });
        }
        return all;
    }

    private static int compareQualityAndDistance(MediaType mt1, MediaType mt2, boolean checkDistance) {
        int result = compareMediaTypesQualityFactors(mt1, mt2, MEDIA_TYPE_Q_PARAM);
        if (result == 0) {
            result = compareMediaTypesQualityFactors(mt1, mt2, MEDIA_TYPE_QS_PARAM);
        }
        if (result == 0 && checkDistance) {
            Integer dist1 = Integer.valueOf(mt1.getParameters().get(MEDIA_TYPE_DISTANCE_PARAM));
            Integer dist2 = Integer.valueOf(mt2.getParameters().get(MEDIA_TYPE_DISTANCE_PARAM));
            result = dist1.compareTo(dist2);
        }
        return result;
    }

    private static String getCurrentPath(MultivaluedMap<String, String> values) {
        String path = values.getFirst(URITemplate.FINAL_MATCH_GROUP);
        return path == null ? "/" : path;
    }

    public static List<ClassResourceInfo> getRootResources(Message message) {
        Service service = message.getExchange().getService();
        return ((JAXRSServiceImpl) service).getClassResourceInfos();
    }

    public static boolean noResourceMethodForOptions(Response exResponse, String httpMethod) {
        return exResponse != null && exResponse.getStatus() == 405
               && "OPTIONS".equalsIgnoreCase(httpMethod);
    }

    private static void logNoMatchMessage(OperationResourceInfo ori,
                                          String path, String httpMethod, MediaType requestType, List<MediaType> acceptContentTypes) {
        org.apache.cxf.common.i18n.Message errorMsg = new org.apache.cxf.common.i18n.Message("OPER_NO_MATCH", BUNDLE, ori.getMethodToInvoke().getName(), path, ori.getURITemplate().getValue(), httpMethod, ori.getHttpMethod(), requestType.toString(), convertTypesToString(ori.getConsumeTypes()), convertTypesToString(acceptContentTypes), convertTypesToString(ori.getProduceTypes()));
        Tr.debug(tc, errorMsg.toString());
    }

    public static Response createResponse(List<ClassResourceInfo> cris, Message msg,
                                          String responseMessage, int status, boolean addAllow) {
        
        ResponseBuilder rb = toResponseBuilder(status);
        if (addAllow) {                       
            Map<ClassResourceInfo, MultivaluedMap<String, String>> matchedResources = null; //Liberty change
            Set<String> allowedMethods = new HashSet<String>();            
            for (ClassResourceInfo cri : cris) {
                //Liberty Change start
                if (cri.getParent() != null) {
                   // Sub-resource
                    allowedMethods.addAll(cri.getAllowedMethods());
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Adding All Allowed Headers " + cri.getAllowedMethods());                        
                    }
                    break;
                }
                
                for (OperationResourceInfo ori : cri.getMethodDispatcher().getOperationResourceInfos()) {
                    if(ori.isSubResourceLocator()) {
                        continue;
                    }
                    if (matchedResources == null) {
                        String messagePath = HttpUtils.getPathToMatch(msg, true);
                        matchedResources = JAXRSUtils.selectResourceClass(cris, messagePath, msg);
                    }
                    MultivaluedMap<String, String> values =  matchedResources.get(cri);
                    if (values == null) {
                        continue;
                    }
                    String httpMethod = ori.getHttpMethod();
                    if (isFinalPath(ori,values)) {                        
                        if (matchHttpMethod(httpMethod, "*")) {                                
                            allowedMethods.add(httpMethod);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Adding Allow Header " + httpMethod);
                            }                                
                        }
                    }
                }
                //Liberty Change end
            }

            for (String m : allowedMethods) {
                rb.header("Allow", m);
            }
            // "OPTIONS" are supported all the time really
            if (!allowedMethods.contains("OPTIONS")) {
                rb.header("Allow", "OPTIONS");
            }
            if (!allowedMethods.contains("HEAD") && allowedMethods.contains("GET")) {
                rb.header("Allow", "HEAD");
            }
        }
        if (msg != null && MessageUtils.getContextualBoolean(msg, REPORT_FAULT_MESSAGE_PROPERTY)) {
            rb.type(MediaType.TEXT_PLAIN_TYPE).entity(responseMessage);
        }
        return rb.build();
    }

    private static boolean matchHttpMethod(String expectedMethod, String httpMethod) {
        //Liberty Change start 
          if ("*".equals(httpMethod)) {
              return true;
          }
        //Liberty Change end 
          return expectedMethod.equalsIgnoreCase(httpMethod)
                 || headMethodPossible(expectedMethod, httpMethod)
                 || expectedMethod.equals(DefaultMethod.class.getSimpleName());
      }

    //Liberty Change start 
      private static boolean isFinalPath(OperationResourceInfo ori, MultivaluedMap<String, String> values) {
          boolean finalPath = false;
          String path = getCurrentPath(values);
          URITemplate uriTemplate = ori.getURITemplate();
          MultivaluedMap<String, String> map = new MetadataMap<>(values);
          if (uriTemplate != null && uriTemplate.match(path, map)) {
              String finalGroup = map.getFirst(URITemplate.FINAL_MATCH_GROUP);
              finalPath = StringUtils.isEmpty(finalGroup) || PATH_SEGMENT_SEP.equals(finalGroup);
          }
          return finalPath;
      }
    //Liberty Change end 

    public static boolean headMethodPossible(String expectedMethod, String httpMethod) {
        return HttpMethod.HEAD.equalsIgnoreCase(httpMethod) && HttpMethod.GET.equals(expectedMethod);
    }

    private static String convertTypesToString(List<MediaType> types) {
        StringBuilder sb = new StringBuilder();
        for (MediaType type : types) {
            sb.append(mediaTypeToString(type)).append(',');
        }
        return sb.toString();
    }

    public static List<MediaType> getConsumeTypes(Consumes cm) {
        return getConsumeTypes(cm, Collections.singletonList(ALL_TYPES));
    }

    public static List<MediaType> getConsumeTypes(Consumes cm, List<MediaType> defaultTypes) {
        return cm == null ? defaultTypes
                          : getMediaTypes(cm.value());
    }

    public static List<MediaType> getProduceTypes(Produces pm) {
        return getProduceTypes(pm, Collections.singletonList(ALL_TYPES));
    }

    public static List<MediaType> getProduceTypes(Produces pm, List<MediaType> defaultTypes) {
        return pm == null ? defaultTypes
                          : getMediaTypes(pm.value());
    }

    public static int compareSortedConsumesMediaTypes(List<MediaType> mts1, List<MediaType> mts2, MediaType ct) {
        List<MediaType> actualMts1 = getCompatibleMediaTypes(mts1, ct);
        List<MediaType> actualMts2 = getCompatibleMediaTypes(mts2, ct);
        return compareSortedMediaTypes(actualMts1, actualMts2, null);
    }

    public static int compareSortedAcceptMediaTypes(List<MediaType> mts1, List<MediaType> mts2,
                                                    List<MediaType> acceptTypes) {
        List<MediaType> actualMts1 = intersectSortMediaTypes(mts1, acceptTypes, true);
        List<MediaType> actualMts2 = intersectSortMediaTypes(mts2, acceptTypes, true);
        int size1 = actualMts1.size();
        int size2 = actualMts2.size();
        for (int i = 0; i < size1 && i < size2; i++) {
            int result = compareMediaTypes(actualMts1.get(i), actualMts2.get(i), null);
            if (result == 0) {
                result = compareQualityAndDistance(actualMts1.get(i), actualMts2.get(i), true);
            }
            if (result != 0) {
                return result;
            }
        }
        return size1 == size2 ? 0 : size1 < size2 ? -1 : 1;
    }

    private static List<MediaType> getCompatibleMediaTypes(List<MediaType> mts, MediaType ct) {
        List<MediaType> actualMts;
        if (mts.size() == 1) {
            actualMts = mts;
        } else {
            actualMts = new LinkedList<>();
            for (MediaType mt : mts) {
                if (isMediaTypeCompatible(mt, ct)) {
                    actualMts.add(mt);
                }
            }
        }
        return actualMts;
    }

    public static int compareSortedMediaTypes(List<MediaType> mts1, List<MediaType> mts2, String qs) {
        int size1 = mts1.size();
        int size2 = mts2.size();
        for (int i = 0; i < size1 && i < size2; i++) {
            int result = compareMediaTypes(mts1.get(i), mts2.get(i), qs);
            if (result != 0) {
                return result;
            }
        }
        return size1 == size2 ? 0 : size1 < size2 ? -1 : 1;
    }

    public static int compareMethodParameters(Class<?>[] paraList1, Class<?>[] paraList2) {
        int size1 = paraList1.length;
        int size2 = paraList2.length;
        for (int i = 0; i < size1 && i < size2; i++) {
            if (!paraList1[i].equals(paraList2[i])) {
                // Handling the case when bridge / synthetic methods may be taken
                // into account (f.e. when service implements generic interfaces or
                // extends the generic classes).
                if (paraList1[i].isAssignableFrom(paraList2[i])) {
                    return 1;
                } else if (paraList2[i].isAssignableFrom(paraList1[i])) {
                    return -1;
                } else {
                    int result = paraList1[i].getName().compareTo(paraList2[i].getName());
                    if (result != 0) {
                        return result;
                    }
                }
            }
        }
        return size1 == size2 ? 0 : size1 < size2 ? -1 : 1;
    }
    public static int compareMediaTypes(MediaType mt1, MediaType mt2) {
        return compareMediaTypes(mt1, mt2, MEDIA_TYPE_Q_PARAM);
    }

    public static int compareMediaTypes(MediaType mt1, MediaType mt2, String qs) {

        boolean mt1TypeWildcard = mt1.isWildcardType();
        boolean mt2TypeWildcard = mt2.isWildcardType();
        if (mt1TypeWildcard && !mt2TypeWildcard) {
            return 1;
        }
        if (!mt1TypeWildcard && mt2TypeWildcard) {
            return -1;
        }

        boolean mt1SubTypeWildcard = mt1.getSubtype().contains(MediaType.MEDIA_TYPE_WILDCARD);
        boolean mt2SubTypeWildcard = mt2.getSubtype().contains(MediaType.MEDIA_TYPE_WILDCARD);
        if (mt1SubTypeWildcard && !mt2SubTypeWildcard) {
            return 1;
        }
        if (!mt1SubTypeWildcard && mt2SubTypeWildcard) {
            return -1;
        }

        return qs != null ? compareMediaTypesQualityFactors(mt1, mt2, qs) : 0;
    }

    public static int compareMediaTypesQualityFactors(MediaType mt1, MediaType mt2) {
        float q1 = getMediaTypeQualityFactor(mt1.getParameters().get(MEDIA_TYPE_Q_PARAM));
        float q2 = getMediaTypeQualityFactor(mt2.getParameters().get(MEDIA_TYPE_Q_PARAM));
        return Float.compare(q1, q2) * -1;
    }

    public static int compareMediaTypesQualityFactors(MediaType mt1, MediaType mt2, String qs) {
        float q1 = getMediaTypeQualityFactor(mt1.getParameters().get(qs));
        float q2 = getMediaTypeQualityFactor(mt2.getParameters().get(qs));
        return Float.compare(q1, q2) * -1;
    }

    public static float getMediaTypeQualityFactor(String q) {
        if (q == null) {
            return 1;
        }
        if (q.charAt(0) == '.') {
            q = '0' + q;
        }
        try {
            return Float.parseFloat(q);
        } catch (NumberFormatException ex) {
            // default value will do
        }
        return 1;
    }

    //Message contains following information: PATH, HTTP_REQUEST_METHOD, CONTENT_TYPE, InputStream.
    public static List<Object> processParameters(OperationResourceInfo ori,
                                                 MultivaluedMap<String, String> values,
                                                 Message message) throws IOException, WebApplicationException {

        Class<?>[] parameterTypes = ori.getInParameterTypes();
        List<Parameter> paramsInfo = ori.getParameters();
        boolean preferModelParams = paramsInfo.size() > parameterTypes.length
                                    && !PropertyUtils.isTrue(message.getContextualProperty("org.apache.cxf.preferMethodParameters"));

        final int parameterTypesLength = preferModelParams ? paramsInfo.size() : parameterTypes.length;
        if (parameterTypesLength < 1) {
            return Collections.emptyList();
        }

        Type[] genericParameterTypes = ori.getInGenericParameterTypes();
        Annotation[][] anns = ori.getInParameterAnnotations();
        Object[] params = new Object[parameterTypesLength];

        // Ensure we process all request-body parameters first, then all @*Params, etc.
        ParamTuple[] tuple = new ParamTuple[parameterTypesLength];
        for (int i = 0; i < parameterTypesLength; i++) {
            tuple[i] = new ParamTuple();
            if (!preferModelParams) {
                tuple[i].param = parameterTypes[i];
                tuple[i].genericParam = InjectionUtils.processGenericTypeIfNeeded(
                    ori.getClassResourceInfo().getServiceClass(), tuple[i].param, genericParameterTypes[i]);
                tuple[i].param = InjectionUtils.updateParamClassToTypeIfNeeded(tuple[i].param, 
                                                                               tuple[i].genericParam);
                tuple[i].paramAnns = anns == null ? EMPTY_ANNOTATIONS : anns[i];
            } else {
                tuple[i].param = paramsInfo.get(i).getJavaType();
                tuple[i].genericParam = tuple[i].param;
                tuple[i].paramAnns = EMPTY_ANNOTATIONS;
            }
            if (paramsInfo.get(i).getType() == ParameterType.REQUEST_BODY) {
                params[i] = processRequestBodyParameter(tuple[i].param, 
                                                        tuple[i].genericParam, 
                                                        tuple[i].paramAnns,
                                                        message,
                                                        ori);
            }
        }
        for (int i = 0; i < parameterTypesLength; i++) {

            if (paramsInfo.get(i).getType() != ParameterType.REQUEST_BODY) {
                params[i] = processParameter(tuple[i].param,
                                             tuple[i].genericParam,
                                             tuple[i].paramAnns,
                                             paramsInfo.get(i),
                                             values,
                                             message,
                                             ori);
            }
        }

        return Arrays.asList(params);
    }

    private static class ParamTuple {
        private Class<?> param;
        private Type genericParam;
        private Annotation[] paramAnns;
    }

    private static Object processRequestBodyParameter(Class<?> parameterClass,
                                                      Type parameterType,
                                                      Annotation[] parameterAnns,
                                                      Message message,
                                                      OperationResourceInfo ori)
        throws IOException, WebApplicationException {

        if (parameterClass == AsyncResponse.class) {
            return new AsyncResponseImpl(message);
        }

        String contentType = (String)message.get(Message.CONTENT_TYPE);

        if (contentType == null) {
            String defaultCt = (String)message.getContextualProperty(DEFAULT_CONTENT_TYPE);
            contentType = defaultCt == null ? MediaType.APPLICATION_OCTET_STREAM : defaultCt;
        }

        final MediaType contentTypeMt = toMediaType(contentType);
        final MessageContext mc = new MessageContextImpl(message);

        MediaType mt = mc.getHttpHeaders().getMediaType();
        if (mt == null) {
            mt = contentTypeMt;
        }

        InputStream is;
        if (mt.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE)) {
            is = copyAndGetEntityStream(message);
        } else {
            is = message.getContent(InputStream.class);
        }

        if (is == null) {
            Reader reader = message.getContent(Reader.class);
            if (reader != null) {
                is = new ReaderInputStream(reader);
            }
        }

        return readFromMessageBody(parameterClass,
                                   parameterType,
                                   parameterAnns,
                                   is,
                                   contentTypeMt,
                                   ori,
                                   message);
    }

    private static Object processParameter(Class<?> parameterClass,
                                           Type parameterType,
                                           Annotation[] parameterAnns,
                                           Parameter parameter,
                                           MultivaluedMap<String, String> values,
                                           Message message,
                                           OperationResourceInfo ori)
        throws IOException, WebApplicationException {

        if (parameter.getType() == ParameterType.REQUEST_BODY) {
            return processRequestBodyParameter(parameterClass, parameterType, parameterAnns, message, ori);
        } else if (parameter.getType() == ParameterType.CONTEXT) {
            return createContextValue(message, parameterType, parameterClass);
        } else if (parameter.getType() == ParameterType.BEAN) {
            return createBeanParamValue(message, parameterClass, ori);
        } else {

            return createHttpParameterValue(parameter,
                                            parameterClass,
                                            parameterType,
                                            parameterAnns,
                                            message,
                                            values,
                                            ori);
        }
    }

    public static Object createHttpParameterValue(Parameter parameter,
                                                  Class<?> parameterClass,
                                                  Type genericParam,
                                                  Annotation[] paramAnns,
                                                  Message message,
                                                  MultivaluedMap<String, String> values,
                                                  OperationResourceInfo ori) {

        boolean isEncoded = parameter.isEncoded() || ori != null && ori.isEncodedEnabled();
        String defaultValue = parameter.getDefaultValue();
        if (defaultValue == null && ori != null) {
            defaultValue = ori.getDefaultParameterValue();
        }

        if (parameter.getType() == ParameterType.PATH) {
            return readFromUriParam(message, parameter.getName(), parameterClass, genericParam,
                                    paramAnns, values, defaultValue, !isEncoded);
        }

        if (parameter.getType() == ParameterType.QUERY) {
            return readQueryString(parameter.getName(), parameterClass, genericParam,
                                   paramAnns, message, defaultValue, !isEncoded);
        }

        if (parameter.getType() == ParameterType.MATRIX) {
            return processMatrixParam(message, parameter.getName(), parameterClass, genericParam,
                                      paramAnns, defaultValue, !isEncoded);
        }

        if (parameter.getType() == ParameterType.FORM) {
            return processFormParam(message, parameter.getName(), parameterClass, genericParam,
                                    paramAnns, defaultValue, !isEncoded);
        }

        if (parameter.getType() == ParameterType.COOKIE) {
            return processCookieParam(message, parameter.getName(), parameterClass, genericParam,
                                      paramAnns, defaultValue);
        }

        Object result = null;
        if (parameter.getType() == ParameterType.HEADER) {
            result = processHeaderParam(message, parameter.getName(), parameterClass, genericParam,
                                        paramAnns, defaultValue);
        }
        return result;
    }

    private static Object processMatrixParam(Message m, String key,
                                             Class<?> pClass, Type genericType,
                                             Annotation[] paramAnns,
                                             String defaultValue,
                                             boolean decode) {
        List<PathSegment> segments = JAXRSUtils.getPathSegments(
                                                                (String) m.get(Message.REQUEST_URI), decode);
        if (!segments.isEmpty()) {
            MultivaluedMap<String, String> params = new MetadataMap<>();
            for (PathSegment ps : segments) {
                MultivaluedMap<String, String> matrix = ps.getMatrixParameters();
                for (Map.Entry<String, List<String>> entry : matrix.entrySet()) {
                    for (String value : entry.getValue()) {
                        params.add(entry.getKey(), value);
                    }
                }
            }

            if ("".equals(key)) {
                return InjectionUtils.handleBean(pClass, paramAnns, params, ParameterType.MATRIX, m, false);
            }
            List<String> values = params.get(key);
            return InjectionUtils.createParameterObject(values,
                                                        pClass,
                                                        genericType,
                                                        paramAnns,
                                                        defaultValue,
                                                        false,
                                                        ParameterType.MATRIX,
                                                        m);
        }

        return null;
    }

    private static Object processFormParam(Message m, String key,
                                           Class<?> pClass, Type genericType,
                                           Annotation[] paramAnns,
                                           String defaultValue,
                                           boolean decode) {

        MessageContext mc = new MessageContextImpl(m);
        MediaType mt = mc.getHttpHeaders().getMediaType();

        @SuppressWarnings("unchecked")
        MultivaluedMap<String, String> params = (MultivaluedMap<String, String>) m.get(FormUtils.FORM_PARAM_MAP);

        if (params == null) {
            params = new MetadataMap<>();
            m.put(FormUtils.FORM_PARAM_MAP, params);

            if (mt == null || mt.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE)) {
                InputStream entityStream = copyAndGetEntityStream(m);
                String enc = HttpUtils.getEncoding(mt, StandardCharsets.UTF_8.name());  //Liberty CXF-7996
                String body = FormUtils.readBody(entityStream, enc);
                FormUtils.populateMapFromStringOrHttpRequest(params, m, body, enc, false); //Liberty CXF-7996
            } else {
                if ("multipart".equalsIgnoreCase(mt.getType())
                    && MediaType.MULTIPART_FORM_DATA_TYPE.isCompatible(mt)) {
                    MultipartBody body = AttachmentUtils.getMultipartBody(mc);
                    // Liberty change start
                    if (IAttachment.class.equals(pClass)) {
                        for (Attachment att : body.getAllAttachments()) {
                            if (key.equals(att.getContentDisposition().getParameter("name"))) {
                                return new AttachmentImpl(att);
                            }
                        }
                    }
                    //Liberty change end
                    FormUtils.populateMapFromMultipart(params, body, m, decode);
                } else {
                    org.apache.cxf.common.i18n.Message errorMsg = new org.apache.cxf.common.i18n.Message("WRONG_FORM_MEDIA_TYPE", BUNDLE, mt.toString());
                    Tr.warning(tc, errorMsg.toString());
                    throw ExceptionUtils.toNotSupportedException(null, null);
                }
            }
        }

        //Liberty start CXF-7996
        if (decode) {
            List<String> values = params.get(key);
            if (values != null) {
                values = values.stream().map(value -> HttpUtils.urlDecode(value)).collect(Collectors.toList());
                params.replace(key, values);
            }
        }
        //Liberty end CXF-7996

        if ("".equals(key)) {
            return InjectionUtils.handleBean(pClass, paramAnns, params, ParameterType.FORM, m, false);
        }
        List<String> results = params.get(key);

        return InjectionUtils.createParameterObject(results,
                                                    pClass,
                                                    genericType,
                                                    paramAnns,
                                                    defaultValue,
                                                    false,
                                                    ParameterType.FORM,
                                                    m);
    }

    public static MultivaluedMap<String, String> getMatrixParams(String path, boolean decode) {
        int index = path.indexOf(';');
        return index == -1 ? new MetadataMap<String, String>() : JAXRSUtils.getStructuredParams(path.substring(index + 1), ";", decode, false);
    }

    private static Object processHeaderParam(Message m,
                                             String header,
                                             Class<?> pClass,
                                             Type genericType,
                                             Annotation[] paramAnns,
                                             String defaultValue) {

        List<String> values = new HttpHeadersImpl(m).getRequestHeader(header);
        if (values != null && values.isEmpty()) {
            values = null;
        }
        return InjectionUtils.createParameterObject(values,
                                                    pClass,
                                                    genericType,
                                                    paramAnns,
                                                    defaultValue,
                                                    false,
                                                    ParameterType.HEADER,
                                                    m);

    }

    private static Object processCookieParam(Message m, String cookieName,
                                             Class<?> pClass, Type genericType,
                                             Annotation[] paramAnns, String defaultValue) {
        Cookie c = new HttpHeadersImpl(m).getCookies().get(cookieName);

        if (c == null && defaultValue != null) {
            c = Cookie.valueOf(cookieName + '=' + defaultValue);
        }
        if (c == null) {
            return null;
        }

        if (pClass.isAssignableFrom(Cookie.class)) {
            return c;
        }
        String value = InjectionUtils.isSupportedCollectionOrArray(pClass)
                       && InjectionUtils.getActualType(genericType) == Cookie.class ? c.toString() : c.getValue();
        return InjectionUtils.createParameterObject(Collections.singletonList(value),
                                                    pClass,
                                                    genericType,
                                                    paramAnns,
                                                    null,
                                                    false,
                                                    ParameterType.COOKIE,
                                                    m);
    }

    public static Object createBeanParamValue(Message m, Class<?> clazz, OperationResourceInfo ori) {
        BeanParamInfo bmi = ServerProviderFactory.getInstance(m).getBeanParamInfo(clazz);
        if (bmi == null) {
            // we could've started introspecting now but the fact no bean info
            // is available indicates that the one created at start up has been
            // lost and hence it is 500
            Tr.warning(tc, "Bean parameter info is not available");
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }
        Object instance;
        try {
            instance = clazz.newInstance();
        } catch (Throwable t) {
            throw ExceptionUtils.toInternalServerErrorException(t, null);
        }
        JAXRSUtils.injectParameters(ori, bmi, instance, m);

        InjectionUtils.injectContexts(instance, bmi, m);

        return instance;
    }

    public static Message getContextMessage(Message m) {

        Message contextMessage = m.getExchange() != null ? m.getExchange().getInMessage() : m;
        if (contextMessage == null && !PropertyUtils.isTrue(m.get(Message.INBOUND_MESSAGE))) {
            contextMessage = m;
        }
        return contextMessage;
    }

    public static <T> T createContextValue(Message m, Type genericType, Class<T> clazz) {

        Message contextMessage = getContextMessage(m);
        Object o = null;
        if (UriInfo.class.isAssignableFrom(clazz)) {
            o = createUriInfo(contextMessage);
        } else if (HttpHeaders.class.isAssignableFrom(clazz)
                   || ProtocolHeaders.class.isAssignableFrom(clazz)) {
            o = createHttpHeaders(contextMessage, clazz);
        } else if (SecurityContext.class.isAssignableFrom(clazz)) {
            SecurityContext customContext = contextMessage.get(SecurityContext.class);
            o = customContext == null ? new SecurityContextImpl(contextMessage) : customContext;
        } else if (MessageContext.class.isAssignableFrom(clazz)) {
            o = new MessageContextImpl(m);
        } else if (ResourceInfo.class.isAssignableFrom(clazz)) {
            o = new ResourceInfoImpl(contextMessage);
        } else if (ResourceContext.class.isAssignableFrom(clazz)) {
            // Liberty change start
            OperationResourceInfo operationResourceInfo = contextMessage.getExchange().get(OperationResourceInfo.class);
            if (operationResourceInfo != null)
                o = new ResourceContextImpl(contextMessage, operationResourceInfo);
            // Liberty change end
        } else if (Request.class.isAssignableFrom(clazz)) {
            o = new RequestImpl(contextMessage);
        } else if (Providers.class.isAssignableFrom(clazz)) {
            o = new ProvidersImpl(contextMessage);
        } else if (ContextResolver.class.isAssignableFrom(clazz)) {
            o = createContextResolver(genericType, contextMessage);
        } else if (Configuration.class.isAssignableFrom(clazz)) {
            o = ProviderFactory.getInstance(contextMessage).getConfiguration(contextMessage);
        } else if (Application.class.isAssignableFrom(clazz)) {
            ProviderInfo<?> providerInfo = (ProviderInfo<?>) contextMessage.getExchange().getEndpoint().get(Application.class.getName());
            o = providerInfo == null ? null : providerInfo.getProvider();
        } else if (contextMessage != null) {
            ContextProvider<?> provider = ProviderFactory.getInstance(contextMessage).createContextProvider(clazz, contextMessage);
            if (provider != null) {
                o = provider.createContext(contextMessage);
            }
        }
        if (o == null && contextMessage != null && !MessageUtils.isRequestor(contextMessage)) {
            o = HttpUtils.createServletResourceValue(contextMessage, clazz);
        }
        return clazz.cast(o);
    }

    @SuppressWarnings("unchecked")
    private static UriInfo createUriInfo(Message m) {
        if (MessageUtils.isRequestor(m)) {
            m = m.getExchange() != null ? m.getExchange().getOutMessage() : m;
        }
        MultivaluedMap<String, String> templateParams = (MultivaluedMap<String, String>) m.get(URITemplate.TEMPLATE_PARAMETERS);
        return new UriInfoImpl(m, templateParams);
    }

    private static Object createHttpHeaders(Message m, Class<?> ctxClass) {
        if (MessageUtils.isRequestor(m)) {
            m = m.getExchange() != null ? m.getExchange().getOutMessage() : m;
        }
        return HttpHeaders.class.isAssignableFrom(ctxClass) ? new HttpHeadersImpl(m) : new ProtocolHeadersImpl(m);
    }

    public static ContextResolver<?> createContextResolver(Type genericType, Message m) {
        if (genericType instanceof ParameterizedType) {
            return ProviderFactory.getInstance(m).createContextResolver(
                                                                        ((ParameterizedType) genericType).getActualTypeArguments()[0], m);
        } else if (m != null) {
            return ProviderFactory.getInstance(m).createContextResolver(genericType, m);
        } else {
            return null;
        }
    }

    public static Object createResourceValue(Message m, Type genericType, Class<?> clazz) {

        // lets assume we're aware of servlet types only that can be @Resource-annotated
        return createContextValue(m, genericType, clazz);
    }

    //CHECKSTYLE:OFF
    private static Object readFromUriParam(Message m,
                                           String parameterName,
                                           Class<?> paramType,
                                           Type genericType,
                                           Annotation[] paramAnns,
                                           MultivaluedMap<String, String> values,
                                           String defaultValue,
                                           boolean decoded) {
        //CHECKSTYLE:ON
        if ("".equals(parameterName)) {
            return InjectionUtils.handleBean(paramType, paramAnns, values, ParameterType.PATH, m, decoded);
        }
        List<String> results = values.get(parameterName);
        return InjectionUtils.createParameterObject(results,
                                                    paramType,
                                                    genericType,
                                                    paramAnns,
                                                    defaultValue,
                                                    decoded,
                                                    ParameterType.PATH,
                                                    m);
    }

    //TODO : multiple query string parsing, do it once
    private static Object readQueryString(String queryName,
                                          Class<?> paramType,
                                          Type genericType,
                                          Annotation[] paramAnns,
                                          Message m,
                                          String defaultValue,
                                          boolean decode) {

        MultivaluedMap<String, String> queryMap = new UriInfoImpl(m, null).getQueryParameters(decode);

        if ("".equals(queryName)) {
            return InjectionUtils.handleBean(paramType, paramAnns, queryMap, ParameterType.QUERY, m, false);
        }
        return InjectionUtils.createParameterObject(queryMap.get(queryName),
                                                    paramType,
                                                    genericType,
                                                    paramAnns,
                                                    defaultValue,
                                                    false,
                                                    ParameterType.QUERY, m);
    }

    /**
     * Retrieve map of query parameters from the passed in message
     *
     * @param message
     * @return a Map of query parameters.
     */
    public static MultivaluedMap<String, String> getStructuredParams(String query,
                                                                     String sep,
                                                                     boolean decode,
                                                                     boolean decodePlus) {
        MultivaluedMap<String, String> map = new MetadataMap<>(new LinkedHashMap<String, List<String>>());

        getStructuredParams(map, query, sep, decode, decodePlus);

        return map;
    }

    public static void getStructuredParams(MultivaluedMap<String, String> queries,
                                           String query,
                                           String sep,
                                           boolean decode,
                                           boolean decodePlus) {
        getStructuredParams(queries, query, sep, decode, decodePlus, false);
    }

    public static void getStructuredParams(MultivaluedMap<String, String> queries,
                                           String query,
                                           String sep,
                                           boolean decode,
                                           boolean decodePlus,
                                           boolean valueIsCollection) {
        if (!StringUtils.isEmpty(query)) {
            for (String part : query.split(sep)) { // fastpath expected
                int index = part.indexOf('=');
                String name = null;
                String value = null;
                if (index == -1) {
                    name = part;
                } else {
                    name = part.substring(0, index);
                    value = index < part.length() ? part.substring(index + 1) : "";
                }
                if (valueIsCollection) {
                    if (value != null) {
                        for (String s : value.split(",")) {
                            addStructuredPartToMap(queries, sep, name, s, decode, decodePlus);
                        }
                    }
                } else {
                    addStructuredPartToMap(queries, sep, name, value, decode, decodePlus);
                }
            }
        }
    }

    private static void addStructuredPartToMap(MultivaluedMap<String, String> queries,
                                               String sep,
                                               String name,
                                               String value,
                                               boolean decode,
                                               boolean decodePlus) {

        if (value != null) {
            if (decodePlus && value.contains("+")) {
                value = value.replace('+', ' ');
            }
            if (decode) {
                value = (";".equals(sep)) ? HttpUtils.pathDecode(value) : HttpUtils.urlDecode(value);
            }
        }

        queries.add(HttpUtils.urlDecode(name), value);
    }

    @FFDCIgnore({ IOException.class, WebApplicationException.class, Exception.class })
    private static Object readFromMessageBody(Class<?> targetTypeClass,
                                              Type parameterType,
                                              Annotation[] parameterAnnotations,
                                              InputStream is,
                                              MediaType contentType,
                                              OperationResourceInfo ori,
                                              Message m) throws IOException, WebApplicationException {

        List<MediaType> types = JAXRSUtils.intersectMimeTypes(ori.getConsumeTypes(), contentType);

        final ProviderFactory pf = ServerProviderFactory.getInstance(m);
        for (MediaType type : types) {
            List<ReaderInterceptor> readers = pf.createMessageBodyReaderInterceptor(
                                                                                    targetTypeClass,
                                                                                    parameterType,
                                                                                    parameterAnnotations,
                                                                                    type,
                                                                                    m,
                                                                                    true,
                                                                                    ori.getNameBindings());
            if (readers != null) {
                try {
                    return readFromMessageBodyReader(readers,
                                                     targetTypeClass,
                                                     parameterType,
                                                     parameterAnnotations,
                                                     is,
                                                     type,
                                                     m);
                } catch (IOException e) {
                    if (e.getClass().getName().equals(NO_CONTENT_EXCEPTION)) {
                        throw ExceptionUtils.toBadRequestException(e, null);
                    }
                    throw e;
                } catch (WebApplicationException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new Fault(ex);
                }
            }
        }

        logMessageHandlerProblem("NO_MSG_READER", targetTypeClass, contentType);
        throw new WebApplicationException(Response.Status.UNSUPPORTED_MEDIA_TYPE);
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore({ PrivilegedActionException.class, JsonbException.class })
    public static Object readFromMessageBodyReader(List<ReaderInterceptor> readers,
                                                   Class<?> targetTypeClass,
                                                   final Type parameterType,
                                                   final Annotation[] parameterAnnotations,
                                                   final InputStream is,
                                                   final MediaType mediaType,
                                                   final Message m) throws IOException, WebApplicationException {

        // Verbose but avoids an extra context instantiation for the typical path
        if (readers.size() > 1) {
            ReaderInterceptor first = readers.remove(0);
            ReaderInterceptorContext context = new ReaderInterceptorContextImpl(targetTypeClass, parameterType, parameterAnnotations, is, m, readers);
            return first.aroundReadFrom(context);
        }
        final MessageBodyReader<?> provider = ((ReaderInterceptorMBR) readers.get(0)).getMBR();
        @SuppressWarnings("rawtypes")
        final Class cls = targetTypeClass;
        // Liberty change start
        Message prevM = currentMessage.get();
        try {
            currentMessage.set(m);
            Object o = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws IOException, WebApplicationException {
                    return provider.readFrom(
                                             cls, parameterType, parameterAnnotations, mediaType,
                                             new HttpHeadersImpl(m).getRequestHeaders(), is);
                }
            });
            return o;
        } catch (PrivilegedActionException e) {
            Exception e1 = e.getException();
            if (e1 instanceof IOException) {
                throw (IOException) e1;
            }
            throw (WebApplicationException) e1;
        } catch (JsonbException e) { // JsonBProvider throws a RuntimeException
            // return HTTP 400 instead of 500
            throw new BadRequestException(e);
        } finally {
            currentMessage.set(prevM);
        }
        // Liberty change end
    }

    //CHECKSTYLE:OFF
    @FFDCIgnore(PrivilegedActionException.class)
    public static void writeMessageBody(List<WriterInterceptor> writers,
                                        final Object entity,
                                        final Class<?> type, final Type genericType,
                                        final Annotation[] annotations,
                                        final MediaType mediaType,
                                        final MultivaluedMap<String, Object> httpHeaders,
                                        Message message) throws WebApplicationException, IOException {

        OutputStream entityStream = message.getContent(OutputStream.class);
        if (entity.getClass().getName().equals(
                "org.apache.cxf.jaxrs.reactivestreams.server.StreamingAsyncSubscriber$StreamingResponseImpl")) {
            //cache the OutputStream when it's reactive response
            entityStream = new CacheAndWriteOutputStream(entityStream);
        }
        if (writers.size() > 1) {
            WriterInterceptor first = writers.remove(0);
            WriterInterceptorContext context = new WriterInterceptorContextImpl(entity, type, genericType, annotations, entityStream, message, writers);

            first.aroundWriteTo(context);
        } else {
            final MessageBodyWriter<Object> writer = ((WriterInterceptorMBW) writers.get(0)).getMBW();
            if (type == byte[].class) {
                long size = writer.getSize(entity, type, genericType, annotations, mediaType);
                if (size != -1) {
                    httpHeaders.putSingle(HttpHeaders.CONTENT_LENGTH, Long.toString(size));
                }
            }
            HttpUtils.convertHeaderValuesToString(httpHeaders, true);
            // Liberty change start
            final OutputStream fEntityStream = entityStream;
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Boolean run() throws IOException, WebApplicationException {
                        writer.writeTo(entity, type, genericType, annotations, mediaType,
                                       httpHeaders,
                                       fEntityStream);
                        return Boolean.TRUE;
                    }
                });
            } catch (PrivilegedActionException e) {
                Exception e1 = e.getException();
                if (e1 instanceof IOException)
                    throw (IOException) e1;
                throw (WebApplicationException) e1;
            }
            // Liberty change end
        }
    }

    //CHECKSTYLE:ON

    public static boolean matchConsumeTypes(MediaType requestContentType,
                                            OperationResourceInfo ori) {

        return doMimeTypesIntersect(ori.getConsumeTypes(), requestContentType);
    }

    public static boolean matchProduceTypes(MediaType acceptContentType,
                                            OperationResourceInfo ori) {

        return doMimeTypesIntersect(ori.getProduceTypes(), acceptContentType);
    }

    public static boolean matchMimeTypes(MediaType requestContentType,
                                         MediaType acceptContentType,
                                         OperationResourceInfo ori) {

        return doMimeTypesIntersect(ori.getConsumeTypes(), requestContentType)
                && doMimeTypesIntersect(ori.getProduceTypes(), acceptContentType);
    }

    public static List<MediaType> parseMediaTypes(String types) {
        List<MediaType> acceptValues = new ArrayList<>();

        if (types != null) {
            int x = 0;
            int y = types.indexOf(',');
            while (y > 0) {
                acceptValues.add(toMediaType(types.substring(x, y).trim()));
                x = y + 1;
                y = types.indexOf(',', x);
            }
            String lastMediaType = types.substring(x).trim();
            if (!lastMediaType.isEmpty()) {
                acceptValues.add(toMediaType(lastMediaType));
            }
        } else {
            acceptValues.add(ALL_TYPES);
        }

        return acceptValues;
    }

    public static boolean doMimeTypesIntersect(List<MediaType> mimeTypesA, MediaType mimeTypeB) {
        return doMimeTypesIntersect(mimeTypesA, Collections.singletonList(mimeTypeB));
    }

    public static boolean doMimeTypesIntersect(List<MediaType> requiredMediaTypes, List<MediaType> userMediaTypes) {
        final NonAccumulatingIntersector intersector = new NonAccumulatingIntersector();
        intersectMimeTypes(requiredMediaTypes, userMediaTypes, intersector);
        return intersector.doIntersect();
    }

    /**
     * intersect two mime types
     *
     * @param requiredMediaTypes
     * @param userMediaTypes
     * @param addRequiredParamsIfPossible
     * @return return a list of intersected mime types
     */
    public static List<MediaType> intersectMimeTypes(List<MediaType> requiredMediaTypes,
                                                     List<MediaType> userMediaTypes,
                                                     boolean addRequiredParamsIfPossible) {
        return intersectMimeTypes(requiredMediaTypes, userMediaTypes, addRequiredParamsIfPossible, false);
    }

    public static List<MediaType> intersectMimeTypes(List<MediaType> requiredMediaTypes,
                                                     List<MediaType> userMediaTypes,
                                                     boolean addRequiredParamsIfPossible,
                                                     boolean addDistanceParameter) {
        final AccumulatingIntersector intersector = new AccumulatingIntersector(addRequiredParamsIfPossible,
                addDistanceParameter);
        intersectMimeTypes(requiredMediaTypes, userMediaTypes, intersector);
        return new ArrayList<>(intersector.getSupportedMimeTypeList());
    }

    private static void intersectMimeTypes(List<MediaType> requiredMediaTypes, List<MediaType> userMediaTypes,
            MimeTypesIntersector intersector) {

        for (MediaType requiredType : requiredMediaTypes) {
            for (MediaType userType : userMediaTypes) {
                boolean isCompatible = isMediaTypeCompatible(requiredType, userType);
                if (isCompatible) {
                    boolean parametersMatched = true;
                    for (Map.Entry<String, String> entry : userType.getParameters().entrySet()) {
                        String value = requiredType.getParameters().get(entry.getKey());
                        if (value != null && entry.getValue() != null && !(stripDoubleQuotesIfNeeded(value)
                                .equals(stripDoubleQuotesIfNeeded(entry.getValue())))) {

                            if (HTTP_CHARSET_PARAM.equals(entry.getKey()) && value.equalsIgnoreCase(entry.getValue())) {
                                continue;
                            }
                            parametersMatched = false;
                            break;
                        }
                    }
                    if (!parametersMatched) {
                        continue;
                    }

                    if (!intersector.intersect(requiredType, userType)) {
                        return;
                    }
                }
            }
        }
    }

    private static String stripDoubleQuotesIfNeeded(String value) {
        if (value != null && value.startsWith("\"")
            && value.endsWith("\"") && value.length() > 1) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean isMediaTypeCompatible(MediaType requiredType, MediaType userType) {
        boolean isCompatible = requiredType.isCompatible(userType);
        if (!isCompatible && requiredType.getType().equalsIgnoreCase(userType.getType())) {
            isCompatible = compareCompositeSubtypes(requiredType, userType,
                                                    PhaseInterceptorChain.getCurrentMessage());
        }
        return isCompatible;
    }

    static boolean compareCompositeSubtypes(String requiredType, String userType,
                                            Message message) {
        return compareCompositeSubtypes(toMediaType(requiredType), toMediaType(userType), message);
    }

    private static boolean compareCompositeSubtypes(MediaType requiredType, MediaType userType,
                                                    Message message) {
        boolean isCompatible = false;
        // check if we have composite subtypes
        String subType1 = requiredType.getSubtype();
        String subType2 = userType.getSubtype();

        String subTypeAfterPlus1 = splitMediaSubType(subType1, true);
        String subTypeAfterPlus2 = splitMediaSubType(subType2, true);
        if (message != null && MessageUtils.getContextualBoolean(message, PARTIAL_HIERARCHICAL_MEDIA_SUBTYPE_CHECK)) {
            if (subTypeAfterPlus1 != null || subTypeAfterPlus2 != null) {
                boolean nullPossible = subTypeAfterPlus1 == null || subTypeAfterPlus2 == null;
                isCompatible = subTypeAfterPlus1 == null && subTypeAfterPlus2.equals(subType1)
                               || subTypeAfterPlus2 == null && subTypeAfterPlus1.equals(subType2);
                if (!isCompatible && !nullPossible) {
                    isCompatible = subTypeAfterPlus1.equalsIgnoreCase(subTypeAfterPlus2)
                                   && (subType1.charAt(0) == '*' || subType2.charAt(0) == '*');
                }

                if (!isCompatible) {
                    String subTypeBeforePlus1 = splitMediaSubType(subType1, false);
                    String subTypeBeforePlus2 = splitMediaSubType(subType2, false);
                    nullPossible = subTypeBeforePlus1 == null || subTypeBeforePlus2 == null;
                    isCompatible = subTypeBeforePlus1 == null && subTypeBeforePlus2 != null
                                    && subTypeBeforePlus2.equals(subType1)
                                    || subTypeBeforePlus2 == null && subTypeBeforePlus1 != null
                                    && subTypeBeforePlus1.equals(subType2);
                    if (!isCompatible && !nullPossible) {
                        isCompatible = subTypeBeforePlus1.equalsIgnoreCase(subTypeBeforePlus2)
                                       && (subType1.charAt(subType1.length() - 1) == '*'
                                           || subType2.charAt(subType2.length() - 1) == '*');
                    }
                }
            }
        } else {
            if (subTypeAfterPlus1 != null && subTypeAfterPlus2 != null) {

                isCompatible = subTypeAfterPlus1.equalsIgnoreCase(subTypeAfterPlus2)
                               && (subType1.charAt(0) == '*' || subType2.charAt(0) == '*');

                if (!isCompatible) {
                    String subTypeBeforePlus1 = splitMediaSubType(subType1, false);
                    String subTypeBeforePlus2 = splitMediaSubType(subType2, false);

                    isCompatible = subTypeBeforePlus1 != null && subTypeBeforePlus1.equalsIgnoreCase(subTypeBeforePlus2)
                                   && (subType1.charAt(subType1.length() - 1) == '*'
                                       || subType2.charAt(subType2.length() - 1) == '*');
                }
            }
        }
        return isCompatible;
    }

    private static String splitMediaSubType(String type, boolean after) {
        int index = type.indexOf('+');
        return index == -1 ? null : after ? type.substring(index + 1) : type.substring(0, index);
    }

    public static List<MediaType> intersectMimeTypes(List<MediaType> mimeTypesA,
                                                     MediaType mimeTypeB) {
        return intersectMimeTypes(mimeTypesA,
                                  Collections.singletonList(mimeTypeB), false);
    }

    public static List<MediaType> intersectMimeTypes(String mimeTypesA,
                                                     String mimeTypesB) {
        return intersectMimeTypes(parseMediaTypes(mimeTypesA),
                                  parseMediaTypes(mimeTypesB),
                                  false);
    }

    public static List<MediaType> sortMediaTypes(String mediaTypes, String qs) {
        return sortMediaTypes(JAXRSUtils.parseMediaTypes(mediaTypes), qs);
    }

    public static List<MediaType> sortMediaTypes(List<MediaType> types, final String qs) {
        if (types.size() > 1) {
            Collections.sort(types, new Comparator<MediaType>() {

                @Override
                public int compare(MediaType mt1, MediaType mt2) {
                    return JAXRSUtils.compareMediaTypes(mt1, mt2, qs);
                }

            });
        }
        return types;
    }

    //Liberty change start
    public static List<Charset> sortCharsets(List<?> charsetHeaderValues) {
        if (charsetHeaderValues == null || charsetHeaderValues.size() < 1) {
            return Collections.emptyList();
        }
        return charsetHeaderValues.stream()
                                  .map(CharsetQualityTuple::parseTuple)
                                  .sorted((t1, t2) -> { return Float.compare(t1.quality, t2.quality) * -1; })
                                  .filter(t -> { return t.charset != null && t.quality > 0; })
                                  .map(t -> { return t.charset; })
                                  .collect(Collectors.toList());
    }

    private static class CharsetQualityTuple {
        Charset charset;
        float quality = 1; // aka weight

        @FFDCIgnore(IllegalCharsetNameException.class)
        static CharsetQualityTuple parseTuple(Object o) {
            String s;
            if (o instanceof String) {
                s = (String) o;
            } else {
                s = o.toString();
            }
            CharsetQualityTuple tuple = new CharsetQualityTuple();
            String[] sArr = s.split(";[qQ]=");
            if (sArr.length > 1) {
                try {
                    float f = Float.parseFloat(sArr[1]);
                    tuple.quality = Float.min(1.0f, Float.max(0f, f));
                } catch (NumberFormatException ex) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid charset weight (" + s + ") - defaulting to 0.");
                    }
                    tuple.quality = 0;
                }
            }
            try {
                if (Charset.isSupported(sArr[0])) {
                    tuple.charset = Charset.forName(sArr[0]);
                } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unsupported charset, " + sArr[0]);
                }
            } catch (IllegalCharsetNameException ex) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Illegal charset name, " + sArr[0]);
                }
            }
            return tuple;
        }
    }
    //Liberty change end
    public static <T extends Throwable> Response convertFaultToResponse(T ex, Message currentMessage) {
        return ExceptionUtils.convertFaultToResponse(ex, currentMessage);
    }

    public static void setMessageContentType(Message message, Response response) {
        if (response != null) {
            Object ct = response.getMetadata().getFirst(HttpHeaders.CONTENT_TYPE);
            if (ct != null) {
                Exchange ex = message.getExchange();
                if (ex.getInMessage() == message) {
                    ex.put(Message.CONTENT_TYPE, ct.toString());
                } else {
                    message.put(Message.CONTENT_TYPE, ct.toString());
                }
            }
        }

    }

    public static QName getClassQName(Class<?> type) {
        String nsURI = PackageUtils.getNamespace(PackageUtils.getPackageName(type));
        if (nsURI.endsWith("/")) {
            nsURI = nsURI.substring(0, nsURI.length() - 1);
        }
        return new QName(nsURI, type.getSimpleName(), "ns1");
    }

    public static QName convertStringToQName(String name) {
        return DOMUtils.convertStringToQName(name, "");
    }

    public static boolean runContainerRequestFilters(ServerProviderFactory pf,
                                                     Message m,
                                                     boolean preMatch,
                                                     Set<String> names) {
        //Liberty Defect 170924: Check @NameBinding on Application first
        Set<String> appNameBindings = AnnotationUtils.getNameBindings(pf.getApplicationProvider().getResourceClass().getAnnotations());
        if (names != null) {
            names.addAll(appNameBindings);
        } else {
            names = appNameBindings;
        }

        List<ProviderInfo<ContainerRequestFilter>> containerFilters = preMatch ? pf.getPreMatchContainerRequestFilters() : pf.getPostMatchContainerRequestFilters(names);
        if (!containerFilters.isEmpty()) {
            ContainerRequestContext context = new ContainerRequestContextImpl(m, preMatch, false);
            for (ProviderInfo<ContainerRequestFilter> filter : containerFilters) {
                try {
                    InjectionUtils.injectContexts(filter.getProvider(), filter, m);
                    filter.getProvider().filter(context);
                } catch (IOException ex) {
                    throw ExceptionUtils.toInternalServerErrorException(ex, null);
                }
                Response response = m.getExchange().get(Response.class);
                if (response != null) {
                    setMessageContentType(m, response);
                    return true;
                }
            }
        }
        return false;
    }

    public static void runContainerResponseFilters(ServerProviderFactory pf,
                                                   ResponseImpl r,
                                                   Message m,
                                                   OperationResourceInfo ori,
                                                   Method invoked) throws IOException, Throwable {
        //Libert Defect 170924: Check @NameBinding on Application first
        Set<String> names = ori == null ? null : ori.getNameBindings();
        Set<String> appNameBindings = AnnotationUtils.getNameBindings(pf.getApplicationProvider().getResourceClass().getAnnotations());
        if (names != null) {
            names.addAll(appNameBindings);
        } else {
            names = appNameBindings;
        }

        List<ProviderInfo<ContainerResponseFilter>> containerFilters = pf.getContainerResponseFilters(names);
        if (!containerFilters.isEmpty()) {
            ContainerRequestContext requestContext = new ContainerRequestContextImpl(m.getExchange().getInMessage(), false, true);
            ContainerResponseContext responseContext = new ContainerResponseContextImpl(r, m, ori == null ? null : ori.getClassResourceInfo().getServiceClass(), invoked);
            for (ProviderInfo<ContainerResponseFilter> filter : containerFilters) {
                InjectionUtils.injectContexts(filter.getProvider(), filter, m);
                filter.getProvider().filter(requestContext, responseContext);
            }
        }
    }

    public static String mediaTypeToString(MediaType mt, String... ignoreParams) {
        List<String> list = ignoreParams == null || ignoreParams.length == 0 ? null : Arrays.asList(ignoreParams);

        return MediaTypeHeaderProvider.typeToString(mt, list);
    }

    public static MediaType toMediaType(String value) {
        if (value == null) {
            return ALL_TYPES;
        }
        return MediaTypeHeaderProvider.valueOf(value);
    }

    public static Response toResponse(int status) {
        return toResponseBuilder(status).build();
    }

    public static Response toResponse(Response.Status status) {
        return toResponse(status.getStatusCode());
    }

    public static ResponseBuilder toResponseBuilder(int status) {
        return new ResponseBuilderImpl().status(status);
    }

    public static ResponseBuilder toResponseBuilder(Response.Status status) {
        return toResponseBuilder(status.getStatusCode());
    }

    public static ResponseBuilder fromResponse(Response response) {
        return fromResponse(response, true);
    }

    public static ResponseBuilder fromResponse(Response response, boolean copyEntity) {
        ResponseBuilder rb = toResponseBuilder(response.getStatus());
        if (copyEntity) {
            rb.entity(response.getEntity());
        }
        for (Map.Entry<String, List<Object>> entry : response.getMetadata().entrySet()) {
            List<Object> values = entry.getValue();
            for (Object value : values) {
                rb.header(entry.getKey(), value);
            }
        }
        return rb;
    }

    public static Response copyResponseIfNeeded(Response response) {
        if (!(response instanceof ResponseImpl)) {
            Response r = fromResponse(response).build();
            Field[] declaredFields = ReflectionUtil.getDeclaredFields(response.getClass());
            for (Field f : declaredFields) {
                Class<?> declClass = f.getType();
                if (declClass == Annotation[].class) {
                    try {
                        Annotation[] fieldAnnotations = ReflectionUtil.accessDeclaredField(f, response, Annotation[].class);
                        ((ResponseImpl) r).setEntityAnnotations(fieldAnnotations);
                    } catch (Throwable ex) {
                        Tr.warning(tc, "Custom annotations if any can not be copied");
                    }
                    break;
                }
            }
            return r;
        }
        return response;
    }
    
    //Liberty change start
    private static ThreadLocal<Message> currentMessage = new ThreadLocal<>();
    public static Message getCurrentMessage() {
        Message m = currentMessage.get();
        return m != null ? m : PhaseInterceptorChain.getCurrentMessage();
    }
    //Liberty change end

    public static ClassResourceInfo getRootResource(Message m) {
        return (ClassResourceInfo) m.getExchange().get(JAXRSUtils.ROOT_RESOURCE_CLASS);
    }

    public static void pushOntoStack(OperationResourceInfo ori,
                                     MultivaluedMap<String, String> params,
                                     Message msg) {
        OperationResourceInfoStack stack = msg.get(OperationResourceInfoStack.class);
        if (stack == null) {
            stack = new OperationResourceInfoStack();
            msg.put(OperationResourceInfoStack.class, stack);
        }

        List<String> values = null;
        if (params.size() <= 1) {
            values = Collections.emptyList();
        } else {
            values = new ArrayList<>(params.size() - 1);
            addTemplateVarValues(values, params, ori.getClassResourceInfo().getURITemplate());
            addTemplateVarValues(values, params, ori.getURITemplate());
        }
        Class<?> realClass = ori.getClassResourceInfo().getServiceClass();
        stack.push(new MethodInvocationInfo(ori, realClass, values));
    }

    private static void addTemplateVarValues(List<String> values,
                                             MultivaluedMap<String, String> params,
                                             URITemplate template) {
        if (template != null) {
            for (String var : template.getVariables()) {
                List<String> paramValues = params.get(var);
                if (paramValues != null) {
                    values.addAll(paramValues);
                }
            }
        }
    }

    public static String logMessageHandlerProblem(String name, Class<?> cls, MediaType ct) {
        org.apache.cxf.common.i18n.Message errorMsg = new org.apache.cxf.common.i18n.Message(name, BUNDLE, cls.getName(), mediaTypeToString(ct));
        String errorMessage = errorMsg.toString();
        Tr.error(tc, errorMessage);
        return errorMessage;
    }

    public static JaxRsRuntimeException toJaxRsRuntimeException(Throwable ex) {
        return new JaxRsRuntimeException(ex);
    }
    
    
    /**
     * Get path URI template, combining base path, class & method & subresource templates 
     * @param message message instance
     * @param cri class resource info
     * @param ori operation resource info
     * @param subOri operation subresource info
     * @return the URI template for the method in question
     */
    public static String getUriTemplate(Message message, ClassResourceInfo cri, OperationResourceInfo ori, 
            OperationResourceInfo subOri) {
        final String template = getUriTemplate(message, cri, ori);
        final String methodPathTemplate = getUriTemplate(subOri);
        return combineUriTemplates(template, methodPathTemplate);
    }

    /**
     * Get path URI template, combining base path, class & method templates 
     * @param message message instance
     * @param cri class resource info
     * @param ori operation resource info
     * @return the URI template for the method in question
     */
    public static String getUriTemplate(Message message, ClassResourceInfo cri, OperationResourceInfo ori) {
        final String basePath = (String)message.get(Message.BASE_PATH);
        final String classPathTemplate = getUriTemplate(cri);
        final String methodPathTemplate = getUriTemplate(ori);

        // The application path (@ApplicationPath) is incorporated into Message.BASE_PATH,
        // since it is part of the address.
        String template = basePath;
        if (StringUtils.isEmpty(template)) {
            template = "/";
        } else if (!template.startsWith("/")) {
            template = "/" + template;
        }
        
        template = combineUriTemplates(template, classPathTemplate);
        return combineUriTemplates(template, methodPathTemplate);
    }
    
    /**
     * Gets the URI template of the operation from its resource info
     * to assemble final URI template 
     * @param ori operation resource info
     * @return URI template
     */
    private static String getUriTemplate(OperationResourceInfo ori) {
        final URITemplate template = ori.getURITemplate();
        if (template != null) {
            return template.getValue();
        } else {
            return null;
        }
    }
    
    /**
     * Goes over sub-resource class resource templates (through parent chain) if necessary
     * to assemble final URI template 
     * @param cri root or subresource class resource info
     * @return URI template chain
     */
    private static String getUriTemplate(ClassResourceInfo cri) {
        final URITemplate template = cri.getURITemplate();
        if (template != null) {
            return template.getValue();
        } else if (cri.getParent() != null) { /* probably subresource */
            return getUriTemplate(cri.getParent());
        } else {
            return null; /* should not happen */
        }
    }
    
    /**
     * Combines two URI templates together
     * @param parent parent URI template
     * @param child child URI template
     * @return the URI template combined from the parent and child
     */
    private static String combineUriTemplates(final String parent, final String child) {
        if (StringUtils.isEmpty(child)) {
            return parent;
        }

        // The way URI templates are normalized in org.apache.cxf.jaxrs.model.URITemplate:
        //  - empty or null become "/"
        //  - "/" is added at the start if not present 
        if ("/".equals(parent)) {
            return child;
        } else if ("/".equals(child)) {
            return parent;
        } else if (parent.endsWith("/")) {
            // Remove only last slash
            return parent.substring(0, parent.length() - 1) + child; //Liberty change
        } else {
            return parent + child;
        }
    }
    // copy the input stream so that it is not inadvertently closed
    private static InputStream copyAndGetEntityStream(Message m) {
        LoadingByteArrayOutputStream baos = new LoadingByteArrayOutputStream(); 
        try (InputStream in = m.getContent(InputStream.class)) {
            IOUtils.copy(in, baos);
        } catch (IOException e) {
            throw ExceptionUtils.toInternalServerErrorException(e, null);
        }
        m.setContent(InputStream.class, baos.createInputStream());
        return baos.createInputStream();
    }
}
