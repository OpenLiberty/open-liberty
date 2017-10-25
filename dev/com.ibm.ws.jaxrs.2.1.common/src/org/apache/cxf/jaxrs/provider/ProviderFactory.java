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
//https://issues.apache.org/jira/browse/CXF-6307
package org.apache.cxf.jaxrs.provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;
import javax.ws.rs.Produces;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.ReaderInterceptorMBR;
import org.apache.cxf.jaxrs.impl.WriterInterceptorMBW;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.FilterProviderInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs20.JaxRsConstants;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.jaxrs20.injection.InjectionRuntimeContextHelper;
import com.ibm.ws.jaxrs20.providers.jsonb.JsonBProvider;
import com.ibm.ws.jaxrs20.providers.jsonp.JsonPProvider;
import com.ibm.ws.jaxrs20.providers.multipart.IBMMultipartProvider;
import com.ibm.ws.jaxrs20.utils.CustomizerUtils;

public abstract class ProviderFactory {
    private static final TraceComponent tc = Tr.register(ProviderFactory.class);
    public static final String DEFAULT_FILTER_NAME_BINDING = "org.apache.cxf.filter.binding";
    public static final String PROVIDER_SELECTION_PROPERTY_CHANGED = "provider.selection.property.changed";
    public static final String ACTIVE_JAXRS_PROVIDER_KEY = "active.jaxrs.provider";

    protected static final String SERVER_FACTORY_NAME = "org.apache.cxf.jaxrs.provider.ServerProviderFactory";
    protected static final String CLIENT_FACTORY_NAME = "org.apache.cxf.jaxrs.client.ClientProviderFactory";
    protected static final String IGNORE_TYPE_VARIABLES = "org.apache.cxf.jaxrs.providers.ignore.typevars";

    //private static final Logger LOG = LogUtils.getL7dLogger(ProviderFactory.class);

    private static final String JAXB_PROVIDER_NAME = "org.apache.cxf.jaxrs.provider.JAXBElementProvider";
    private static final String JSON_PROVIDER_NAME = "org.apache.cxf.jaxrs.provider.json.JSONProvider";
    private static final String BUS_PROVIDERS_ALL = "org.apache.cxf.jaxrs.bus.providers";
    private static final String PROVIDER_CACHE_ALLOWED = "org.apache.cxf.jaxrs.provider.cache.allowed";
    private static final String PROVIDER_CACHE_CHECK_ALL = "org.apache.cxf.jaxrs.provider.cache.checkAllCandidates";

    protected Map<NameKey, ProviderInfo<ReaderInterceptor>> readerInterceptors = new NameKeyMap<ProviderInfo<ReaderInterceptor>>(true);
    protected Map<NameKey, ProviderInfo<WriterInterceptor>> writerInterceptors = new NameKeyMap<ProviderInfo<WriterInterceptor>>(true);

    private final List<ProviderInfo<MessageBodyReader<?>>> messageReaders =
        new CopyOnWriteArrayList<ProviderInfo<MessageBodyReader<?>>>();
    private final List<ProviderInfo<MessageBodyWriter<?>>> messageWriters =
        new CopyOnWriteArrayList<ProviderInfo<MessageBodyWriter<?>>>();
    private final List<ProviderInfo<ContextResolver<?>>> contextResolvers =
        new CopyOnWriteArrayList<ProviderInfo<ContextResolver<?>>>();
    private final List<ProviderInfo<ContextProvider<?>>> contextProviders =
        new CopyOnWriteArrayList<ProviderInfo<ContextProvider<?>>>();

    private final List<ProviderInfo<ParamConverterProvider>> paramConverters = new ArrayList<ProviderInfo<ParamConverterProvider>>(1);
    private boolean paramConverterContextsAvailable;
    // List of injected providers
    private final Collection<ProviderInfo<?>> injectedProviders = new HashSet<ProviderInfo<?>>();

    private final Bus bus;

    private Comparator<?> providerComparator;

    private final ProviderCache providerCache;
    //Liberty code change start
    //defect 178126
    //A cache for getGenericInterfaces
    private static final ConcurrentHashMap<ClassPair, Type[]> genericInterfacesCache = new ConcurrentHashMap<ClassPair, Type[]>();
    private static final Type[] emptyType = new Type[] {};

    //Liberty code change end
    protected ProviderFactory(Bus bus) {
        this.bus = bus;
        providerCache = initCache(bus);
    }

    public Bus getBus() {
        return bus;
    }

    protected static ProviderCache initCache(Bus theBus) {
        Object allowProp = theBus.getProperty(PROVIDER_CACHE_ALLOWED);
        boolean allowed = allowProp == null || PropertyUtils.isTrue(allowProp);
        if (!allowed) {
            return null;
        }
        boolean checkAll = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

            @Override
            public Boolean run() {
                Object o = System.getProperty(PROVIDER_CACHE_CHECK_ALL);
                if (o == null) {
                    o = PropertyUtils.isTrue(theBus.getProperty(PROVIDER_CACHE_CHECK_ALL));
                }
                return PropertyUtils.isTrue(o);
            }
        });
        return new ProviderCache(checkAll);
    }

    protected static void initFactory(ProviderFactory factory) {
        factory.setProviders(false,
                             false,
                             new BinaryDataProvider<Object>(),
                             new SourceProvider<Object>(),
                             new DataSourceProvider<Object>(),
                             new FormEncodingProvider<Object>(),
                             new StringTextProvider(),
                             new PrimitiveTextProvider<Object>(),
                             new JAXBElementProvider<Object>(),
                             new JAXBElementTypedProvider(),
                             //new StringProvider<Object>(), // Liberty Change for CXF
                             //new JAXBElementSubProvider(),
                             createJsonpProvider(), // Liberty Change for CXF Begin
                             createJsonbProvider(),
                             new IBMMultipartProvider(), // Liberty Change for CXF End
                             new MultipartProvider());
        Object prop = factory.getBus().getProperty("skip.default.json.provider.registration");
        if (!PropertyUtils.isTrue(prop)) {
            factory.setProviders(false, false, createProvider(JSON_PROVIDER_NAME, factory.getBus()));
        }
    }

    @FFDCIgnore(value = { Throwable.class }) // Liberty Change
    protected static Object createProvider(String className, Bus bus) {

        try {
            Class<?> cls = ClassLoaderUtils.loadClass(className, ProviderFactory.class);
            for (Constructor<?> c : cls.getConstructors()) {
                if (c.getParameterTypes().length == 1 && c.getParameterTypes()[0] == Bus.class) {
                    return c.newInstance(bus);
                }
            }
            return cls.newInstance();
        } catch (Throwable ex) {
            String message = "Problem with creating the default provider " + className;
            if (ex.getMessage() != null) {
                message += ": " + ex.getMessage();
            } else {
                message += ", exception class : " + ex.getClass().getName();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, message);
            }
        }
        return null;
    }

    // Liberty Change for CXF Begin
    private static Object createJsonpProvider() {
        JsonProvider jsonProvider = null;
        Bundle b = FrameworkUtil.getBundle(ProviderFactory.class);
        if(b != null) {
            BundleContext bc = b.getBundleContext();
            ServiceReference<JsonProvider> sr = bc.getServiceReference(JsonProvider.class);
            jsonProvider = (JsonProvider)bc.getService(sr);
        }
        return new JsonPProvider(jsonProvider);
    }
    // Liberty Change for CXF End


    @FFDCIgnore(value = { ClassNotFoundException.class })
    public static Class<?> loadClass(ClassLoader cl, String className) {
        if (cl == null)
            return null;

        Class<?> c = null;
        try {
            c = cl.loadClass(className);
        } catch (ClassNotFoundException e) {

        }

        return c;
    }
    
    private static Object createJsonbProvider() {
        JsonbProvider jsonbProvider = null;
        Bundle b = FrameworkUtil.getBundle(ProviderFactory.class);
        if(b != null) {
            BundleContext bc = b.getBundleContext();
            ServiceReference<JsonbProvider> sr = bc.getServiceReference(JsonbProvider.class);
            jsonbProvider = (JsonbProvider)bc.getService(sr);
        }
        return new JsonBProvider(jsonbProvider);
    }

    // Liberty Change for CXF End
    public abstract Configuration getConfiguration(Message message);

    public <T> ContextResolver<T> createContextResolver(Type contextType,
                                                        Message m) {
        boolean isRequestor = MessageUtils.isRequestor(m);
        Message requestMessage = isRequestor ? m.getExchange().getOutMessage()
                                             : m.getExchange().getInMessage();

        Message responseMessage = isRequestor ? m.getExchange().getInMessage()
                                              : m.getExchange().getOutMessage();
        Object ctProperty = null;
        if (responseMessage != null) {
            ctProperty = responseMessage.get(Message.CONTENT_TYPE);
        } else {
            ctProperty = requestMessage.get(Message.CONTENT_TYPE);
        }
        MediaType mt = ctProperty != null ? JAXRSUtils.toMediaType(ctProperty.toString())
            : MediaType.WILDCARD_TYPE;
        return createContextResolver(contextType, m, mt);

    }

    @SuppressWarnings("unchecked")
    public <T> ContextResolver<T> createContextResolver(Type contextType,
                                                        Message m,
                                                        MediaType type) {
        Class<?> contextCls = InjectionUtils.getActualType(contextType);
        if (contextCls == null) {
            return null;
        }
        List<ContextResolver<T>> candidates = new LinkedList<ContextResolver<T>>();
        for (ProviderInfo<ContextResolver<?>> cr : contextResolvers) {
            Type[] types = cr.getProvider().getClass().getGenericInterfaces();
            for (Type t : types) {
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) t;
                    Type[] args = pt.getActualTypeArguments();
                    if (args.length > 0) {
                        Class<?> argCls = InjectionUtils.getActualType(args[0]);

                        if (argCls != null && argCls.isAssignableFrom(contextCls)) {
                            List<MediaType> mTypes = JAXRSUtils.getProduceTypes(
                                                                                cr.getProvider().getClass().getAnnotation(Produces.class));
                            if (JAXRSUtils.intersectMimeTypes(mTypes, type).size() > 0) {
                                injectContextValues(cr, m);
                                candidates.add((ContextResolver<T>) cr.getProvider());
                            }
                        }
                    }
                }
            }
        }
        if (candidates.size() == 0) {
            return null;
        } else if (candidates.size() == 1) {
            return candidates.get(0);
        } else {
            Collections.sort(candidates, new PriorityBasedClassComparator());
            return new ContextResolverProxy<T>(candidates);
        }

    }

    @SuppressWarnings("unchecked")
    public <T> ContextProvider<T> createContextProvider(Type contextType,
                                                        Message m) {
        Class<?> contextCls = InjectionUtils.getActualType(contextType);
        if (contextCls == null) {
            return null;
        }
        for (ProviderInfo<ContextProvider<?>> cr : contextProviders) {
            Type[] types = cr.getProvider().getClass().getGenericInterfaces();
            for (Type t : types) {
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) t;
                    Type[] args = pt.getActualTypeArguments();
                    if (args.length > 0) {
                        Class<?> argCls = InjectionUtils.getActualType(args[0]);

                        if (argCls != null && argCls.isAssignableFrom(contextCls)) {
                            return (ContextProvider<T>) cr.getProvider();
                        }
                    }
                }
            }
        }
        return null;
    }



    public <T> ParamConverter<T> createParameterHandler(Class<T> paramType,
                                                        Type genericType,
                                                        Annotation[] anns,
                                                        Message m) {

        if (paramConverters != null) {
            anns = anns != null ? anns : new Annotation[] {};
            for (ProviderInfo<ParamConverterProvider> pi : paramConverters) {
                injectContextValues(pi, m);
                ParamConverter<T> converter = pi.getProvider().getConverter(paramType, genericType, anns);
                if (converter != null) {
                    return converter;
                }
                pi.clearThreadLocalProxies();
            }
        }
        return null;
    }

    /**
     * _PERF_
     */
    protected <T> boolean handleRWMapper(ProviderInfo<T> em, Class<?> expectedType, Message m,
                                         Class<?> providerClass) {
        Class<?> mapperClass = ClassHelper.getRealClass(bus, em.getOldProvider());
        Type[] types = null;
        if (m != null && MessageUtils.isTrue(m.getContextualProperty(IGNORE_TYPE_VARIABLES))) {
            types = new Type[] { mapperClass };
        } else {
            types = getGenericInterfaces(mapperClass, expectedType);
        }

        for (Type t : types) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                Type[] args = pt.getActualTypeArguments();
                for (int i = 0; i < args.length; i++) {
                    Type arg = args[i];
                    if (arg instanceof TypeVariable) {
                        TypeVariable<?> var = (TypeVariable<?>) arg;
                        Type[] bounds = var.getBounds();
                        boolean isResolved = false;
                        for (int j = 0; j < bounds.length; j++) {
                            Class<?> cls = InjectionUtils.getRawType(bounds[j]);
                            if (cls != null && cls.isAssignableFrom(expectedType)) {
                                isResolved = true;
                                break;
                            }
                        }
                        if (!isResolved) {
                            return false;
                        }
                        return true;
                    }
                    Class<?> actualClass = InjectionUtils.getRawType(arg);
                    if (actualClass == null) {
                        continue;
                    }
                    if (expectedType.isArray() && !actualClass.isArray()) {
                        expectedType = expectedType.getComponentType();
                    }
                    if (actualClass.isAssignableFrom(expectedType) || actualClass == Object.class) {
                        return true;
                    }
                }
            } else if (t instanceof Class && providerClass.isAssignableFrom((Class<?>) t)) {
                return true;
            }
        }

        return false;
    }

    protected <T> boolean handleMapper(ProviderInfo<T> em,
                                       Class<?> expectedType,
                                       Message m,
                                       Class<?> providerClass,
                                       boolean injectContext) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleMapper", new Object[]{em, expectedType, m, providerClass, injectContext});
        }
        // Liberty Change for CXF Begin
        Class<?> mapperClass = ClassHelper.getRealClass(bus, em.getOldProvider());
        // Liberty Change for CXF End
        Type[] types = null;
        if (m != null && MessageUtils.isTrue(m.getContextualProperty(IGNORE_TYPE_VARIABLES))) {
            types = new Type[] { mapperClass };
        } else {
            types = getGenericInterfaces(mapperClass, expectedType);
        }
        for (Type t : types) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                Type[] args = pt.getActualTypeArguments();
                for (int i = 0; i < args.length; i++) {
                    Type arg = args[i];
                    if (arg instanceof TypeVariable) {
                        TypeVariable<?> var = (TypeVariable<?>) arg;
                        Type[] bounds = var.getBounds();
                        boolean isResolved = false;
                        for (int j = 0; j < bounds.length; j++) {
                            Class<?> cls = InjectionUtils.getRawType(bounds[j]);
                            if (cls != null && (cls == Object.class || cls.isAssignableFrom(expectedType))) {
                                isResolved = true;
                                break;
                            }
                        }
                        if (!isResolved) {
                            return false;
                        }
                        if (injectContext) {
                            injectContextValues(em, m);
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "handleMapper return true");
                        }
                        return true;
                    }
                    Class<?> actualClass = InjectionUtils.getRawType(arg);
                    if (actualClass == null) {
                        continue;
                    }
                    if (expectedType.isArray() && !actualClass.isArray()) {
                        expectedType = expectedType.getComponentType();
                    }
                    if (actualClass.isAssignableFrom(expectedType) || actualClass == Object.class) {
                        if (injectContext) {
                            injectContextValues(em, m);
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "handleMapper return true");
                        }
                        return true;
                    }
                }
            } else if (t instanceof Class && providerClass.isAssignableFrom((Class<?>) t)) {
                if (injectContext) {
                    injectContextValues(em, m);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "handleMapper return true");
                }
                return true;
            }
        }
        return false;
    }

    public <T> List<ReaderInterceptor> createMessageBodyReaderInterceptor(Class<T> bodyType,
                                                                          Type parameterType,
                                                                          Annotation[] parameterAnnotations,
                                                                          MediaType mediaType,
                                                                          Message m,
                                                                          boolean checkMbrNow,
                                                                          Set<String> names) {
        MessageBodyReader<T> mr = !checkMbrNow ? null : createMessageBodyReader(bodyType,
                                                                                parameterType,
                                                                                parameterAnnotations,
                                                                                mediaType,
                                                                                m);
        int size = readerInterceptors.size();
        if (mr != null || size > 0) {
            ReaderInterceptor mbrReader = new ReaderInterceptorMBR(mr, m.getExchange().getInMessage());

            List<ReaderInterceptor> interceptors = null;
            if (size > 0) {
                interceptors = new ArrayList<>(size + 1);
                List<ProviderInfo<ReaderInterceptor>> readers =
                    getBoundFilters(readerInterceptors, names);
                for (ProviderInfo<ReaderInterceptor> p : readers) {
                    injectContextValues(p, m);
                    interceptors.add(p.getProvider());
                }
                interceptors.add(mbrReader);
            } else {
                interceptors = Collections.singletonList(mbrReader);
            }

            return interceptors;
        }
        return null;
    }

    public <T> List<WriterInterceptor> createMessageBodyWriterInterceptor(Class<T> bodyType,
                                                                          Type parameterType,
                                                                          Annotation[] parameterAnnotations,
                                                                          MediaType mediaType,
                                                                          Message m,
                                                                          Set<String> names) {
        MessageBodyWriter<T> mw = createMessageBodyWriter(bodyType,
                                                          parameterType,
                                                          parameterAnnotations,
                                                          mediaType,
                                                          m);
        int size = writerInterceptors.size();
        if (mw != null || size > 0) {

            @SuppressWarnings({
                                "unchecked", "rawtypes"
            })
            WriterInterceptor mbwWriter = new WriterInterceptorMBW((MessageBodyWriter) mw, m);

            List<WriterInterceptor> interceptors = null;
            if (size > 0) {
                interceptors = new ArrayList<>(size + 1);
                List<ProviderInfo<WriterInterceptor>> writers =
                    getBoundFilters(writerInterceptors, names);
                for (ProviderInfo<WriterInterceptor> p : writers) {
                    injectContextValues(p, m);
                    interceptors.add(p.getProvider());
                }
                interceptors.add(mbwWriter);
            } else {
                interceptors = Collections.singletonList(mbwWriter);
            }

            return interceptors;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> MessageBodyReader<T> createMessageBodyReader(Class<T> type,
                                                            Type genericType,
                                                            Annotation[] annotations,
                                                            MediaType mediaType,
                                                            Message m) {
        // Step1: check the cache

        if (providerCache != null) {
            for (ProviderInfo<MessageBodyReader<?>> ep : providerCache.getReaders(type, mediaType)) {
                if (isReadable(ep, type, genericType, annotations, mediaType, m)) {
                    return (MessageBodyReader<T>) ep.getProvider();
                }
            }
        }

        boolean checkAll = providerCache != null && providerCache.isCheckAllCandidates();
        List<ProviderInfo<MessageBodyReader<?>>> allCandidates =
            checkAll ? new LinkedList<ProviderInfo<MessageBodyReader<?>>>() : null;

        MessageBodyReader<T> selectedReader = null;
        for (ProviderInfo<MessageBodyReader<?>> ep : messageReaders) {
            if (matchesReaderMediaTypes(ep, mediaType)
                && handleMapper(ep, type, m, MessageBodyReader.class, false)) {
                // This writer matches Media Type and Class
                if (checkAll) {
                    allCandidates.add(ep);
                } else if (providerCache != null && providerCache.getReaders(type, mediaType).isEmpty()) {
                    providerCache.putReaders(type, mediaType, Collections.singletonList(ep));
                }
                if (selectedReader == null
                    && isReadable(ep, type, genericType, annotations, mediaType, m)) {
                    // This writer is a selected candidate
                    selectedReader = (MessageBodyReader<T>) ep.getProvider();
                    if (!checkAll) {
                        return selectedReader;
                    }
                }

            }
        }
        if (checkAll) {
            providerCache.putReaders(type, mediaType, allCandidates);
        }
        return selectedReader;
    }

    @SuppressWarnings("unchecked")
    public <T> MessageBodyWriter<T> createMessageBodyWriter(Class<T> type,
                                                            Type genericType,
                                                            Annotation[] annotations,
                                                            MediaType mediaType,
                                                            Message m) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "createMessageBodyWriter ",  new Object[]{type, genericType, annotations, mediaType, m});
        }
        
        // Step1: check the cache.
        if (providerCache != null) {
            for (ProviderInfo<MessageBodyWriter<?>> ep : providerCache.getWriters(type, mediaType)) {
                if (isWriteable(ep, type, genericType, annotations, mediaType, m)) {
                    return (MessageBodyWriter<T>) ep.getProvider();
                }
            }
        }

        // Step2: check all the registered writers

        // The cache, if enabled, may have been configured to keep the top candidate only
        boolean checkAll = providerCache != null && providerCache.isCheckAllCandidates();
        List<ProviderInfo<MessageBodyWriter<?>>> allCandidates =
            checkAll ? new LinkedList<ProviderInfo<MessageBodyWriter<?>>>() : null;

        MessageBodyWriter<T> selectedWriter = null;
        for (ProviderInfo<MessageBodyWriter<?>> ep : messageWriters) {
            if (matchesWriterMediaTypes(ep, mediaType)
                && handleMapper(ep, type, m, MessageBodyWriter.class, false)) {
                // This writer matches Media Type and Class
                if (checkAll) {
                    allCandidates.add(ep);
                } else if (providerCache != null && providerCache.getWriters(type, mediaType).isEmpty()) {
                    providerCache.putWriters(type, mediaType, Collections.singletonList(ep));
                }
                if (selectedWriter == null
                    && isWriteable(ep, type, genericType, annotations, mediaType, m)) {
                    // This writer is a selected candidate
                    selectedWriter = (MessageBodyWriter<T>) ep.getProvider();
                    if (!checkAll) {
                        return selectedWriter;
                    }
                }

            }
        }
        if (checkAll) {
            providerCache.putWriters(type, mediaType, allCandidates);
        }
        return selectedWriter;

    }

    protected void setBusProviders() {
        List<Object> extensions = new LinkedList<Object>();
        addBusExtension(extensions,
                        MessageBodyReader.class,
                        MessageBodyWriter.class,
                        ExceptionMapper.class);
        if (!extensions.isEmpty()) {
            setProviders(true, true, extensions.toArray());
        }
    }

    private void addBusExtension(List<Object> extensions, Class<?>... extClasses) {
        for (Class<?> extClass : extClasses) {
            Object ext = bus.getProperty(extClass.getName());
            if (extClass.isInstance(ext)) {
                extensions.add(ext);
            }
        }
        Object allProp = bus.getProperty(BUS_PROVIDERS_ALL);
        if (allProp instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> all = (List<Object>) allProp;
            extensions.addAll(all);
        }
    }

    protected abstract void setProviders(boolean custom, boolean busGlobal, Object... providers);

    @SuppressWarnings("unchecked")
    protected void setCommonProviders(List<ProviderInfo<? extends Object>> theProviders) {
        List<ProviderInfo<ReaderInterceptor>> readInts =
            new LinkedList<ProviderInfo<ReaderInterceptor>>();
        List<ProviderInfo<WriterInterceptor>> writeInts =
            new LinkedList<ProviderInfo<WriterInterceptor>>();
        for (ProviderInfo<? extends Object> provider : theProviders) {
            Class<?> providerCls = ClassHelper.getRealClass(bus, provider.getProvider());

            if (MessageBodyReader.class.isAssignableFrom(providerCls)) {
                addProviderToList(messageReaders, provider);
            }

            if (MessageBodyWriter.class.isAssignableFrom(providerCls)) {
                addProviderToList(messageWriters, provider);
            }

            if (ContextResolver.class.isAssignableFrom(providerCls)) {
                addProviderToList(contextResolvers, provider);
            }

            if (ContextProvider.class.isAssignableFrom(providerCls)) {
                addProviderToList(contextProviders, provider);
            }

            if (filterContractSupported(provider, providerCls, ReaderInterceptor.class)) {
                readInts.add((ProviderInfo<ReaderInterceptor>) provider);
            }

            if (filterContractSupported(provider, providerCls, WriterInterceptor.class)) {
                writeInts.add((ProviderInfo<WriterInterceptor>) provider);
            }

            if (ParamConverterProvider.class.isAssignableFrom(providerCls)) {
                paramConverters.add((ProviderInfo<ParamConverterProvider>) provider);
            }
        }
        sortReaders();
        sortWriters();
        sortContextResolvers();

        mapInterceptorFilters(readerInterceptors, readInts, ReaderInterceptor.class, true);
        mapInterceptorFilters(writerInterceptors, writeInts, WriterInterceptor.class, true);

        injectContextProxies(messageReaders, messageWriters, contextResolvers, paramConverters,
                             readerInterceptors.values(), writerInterceptors.values());
        checkParamConverterContexts();
    }

    private void checkParamConverterContexts() {
        for (ProviderInfo<ParamConverterProvider> pi : paramConverters) {
            if (pi.contextsAvailable()) {
                paramConverterContextsAvailable = true;
            }
        }

    }

    public boolean isParamConverterContextsAvailable() {
        return paramConverterContextsAvailable;
    }

    protected void injectContextValues(ProviderInfo<?> pi, Message m) {
        if (m != null) {
            InjectionUtils.injectContexts(pi.getProvider(), pi, m);
        }
    }

    protected void addProviderToList(List<?> list, ProviderInfo<?> provider) {
        List<ProviderInfo<?>> list2 = CastUtils.cast(list);
        for (ProviderInfo<?> pi : list2) {
            if (pi.getProvider() == provider.getProvider()) {
                return;
            }
        }
        list2.add(provider);
    }

    protected void injectContextProxies(Collection<?>... providerLists) {
        for (Collection<?> list : providerLists) {
            Collection<ProviderInfo<?>> l2 = CastUtils.cast(list);
            for (ProviderInfo<?> pi : l2) {
                injectContextProxiesIntoProvider(pi);
            }
        }
    }

    protected void injectContextProxiesIntoProvider(ProviderInfo<?> pi) {
        injectContextProxiesIntoProvider(pi, null);
    }

    void injectContextProxiesIntoProvider(ProviderInfo<?> pi, Application app) {
        if (pi.contextsAvailable()) {
            InjectionUtils.injectContextProxiesAndApplication(pi, pi.getProvider(), app, this);
            injectedProviders.add(pi);
        }
    }

    /*
     * sorts the available providers according to the media types they declare
     * support for. Sorting of media types follows the general rule: x/y < * x < *,
     * i.e. a provider that explicitly lists a media types is sorted before a
     * provider that lists *. Quality parameter values are also used such that
     * x/y;q=1.0 < x/y;q=0.7.
     */
    private void sortReaders() {
        if (!customComparatorAvailable(MessageBodyReader.class)) {
            Collections.sort(messageReaders, new MessageBodyReaderComparator(readerMediaTypesMap));
        } else {
            doCustomSort(messageReaders);
        }
    }

    private <T> void sortWriters() {
        if (!customComparatorAvailable(MessageBodyWriter.class)) {
            Collections.sort(messageWriters, new MessageBodyWriterComparator(writerMediaTypesMap));
        } else {
            doCustomSort(messageWriters);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder("sortWriters - sorted list:");
            for (int i = 0; i < messageWriters.size(); i++) {
                msg.append(" (" + i + ") " + messageWriters.get(i).getProvider());
            }
            Tr.debug(tc, msg.toString());
        }
    }

    private boolean customComparatorAvailable(Class<?> providerClass) {
        if (providerComparator != null) {
            Type type = ((ParameterizedType)providerComparator.getClass()
                .getGenericInterfaces()[0]).getActualTypeArguments()[0];
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType() == ProviderInfo.class) {
                    Type type2 = pt.getActualTypeArguments()[0];
                    if (type2 == providerClass
                        || type2 instanceof WildcardType
                        || type2 instanceof ParameterizedType
                           && ((ParameterizedType) type2).getRawType() == providerClass) {
                        return true;
                    }
                }
            } else if (type == Object.class) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private <T> void doCustomSort(List<?> listOfProviders) {
        Comparator<?> theProviderComparator = providerComparator;
        Type type = ((ParameterizedType)providerComparator.getClass()
            .getGenericInterfaces()[0]).getActualTypeArguments()[0];
        if (type == Object.class) {
            theProviderComparator =
                (new ProviderInfoClassComparator((Comparator<Object>)theProviderComparator));
        }
        List<T> theProviders = (List<T>) listOfProviders;
        Comparator<? super T> theComparator = (Comparator<? super T>) theProviderComparator;
        Collections.sort(theProviders, theComparator);
    }

    private void sortContextResolvers() {
        Collections.sort(contextResolvers, new ContextResolverComparator());
    }

    private final Map<MessageBodyReader<?>, List<MediaType>> readerMediaTypesMap = new HashMap<>();

    /**
     * This method attempts to optimize performance by checking a cache of known MessageBodyReaders's media types,
     * rather than calculating the media types for every provider on every request. If there is a cache miss, we
     * will look up the media types by calling JAXRSUtils.getProviderConsumeTypes(mbr).
     */
    private static List<MediaType> getProviderConsumeTypes(MessageBodyReader<?> mbr, Map<MessageBodyReader<?>, List<MediaType>> cache) {
        List<MediaType> mediaTypes = cache.get(mbr);
        if (mediaTypes == null) {
            mediaTypes = JAXRSUtils.getProviderConsumeTypes(mbr);
            cache.put(mbr, mediaTypes);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getProviderConsumeTypes - cache miss - caching " + mbr + " = " + mediaTypes);
            }
        }
        return mediaTypes;
    }

    private <T> boolean matchesReaderMediaTypes(ProviderInfo<MessageBodyReader<?>> pi,
                                                MediaType mediaType) {
        MessageBodyReader<?> ep = pi.getProvider();
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderConsumeTypes(ep);

        List<MediaType> availableMimeTypes =
            JAXRSUtils.intersectMimeTypes(Collections.singletonList(mediaType), supportedMediaTypes, false);

        return availableMimeTypes.size() != 0;
    }

    private boolean isReadable(ProviderInfo<MessageBodyReader<?>> pi,
                               Class<?> type,
                               Type genericType,
                               Annotation[] annotations,
                               MediaType mediaType,
                               Message m) {
        MessageBodyReader<?> ep = pi.getProvider();
        if (m.get(ACTIVE_JAXRS_PROVIDER_KEY) != ep) {
            injectContextValues(pi, m);
        }
        return ep.isReadable(type, genericType, annotations, mediaType);
    }

    private final Map<MessageBodyWriter<?>, List<MediaType>> writerMediaTypesMap = new HashMap<>();

    /**
     * This method attempts to optimize performance by checking a cache of known MessageBodyWriter's media types,
     * rather than calculating the media types for every provider on every request. If there is a cache miss, we
     * will look up the media types by calling JAXRSUtils.getProviderProduceTypes(mbw).
     */
    private static List<MediaType> getProviderProduceTypes(MessageBodyWriter<?> mbw, Map<MessageBodyWriter<?>, List<MediaType>> cache) {
        List<MediaType> mediaTypes = cache.get(mbw);
        if (mediaTypes == null) {
            mediaTypes = JAXRSUtils.getProviderProduceTypes(mbw);
            cache.put(mbw, mediaTypes);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getProviderProduceTypes - cache miss - caching " + mbw + " = " + mediaTypes);
            }
        }
        return mediaTypes;
    }

    private <T> boolean matchesWriterMediaTypes(ProviderInfo<MessageBodyWriter<?>> pi,
                                                MediaType mediaType) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "matchesWriterMediaTypes ",  new Object[]{pi, mediaType});
        }
        MessageBodyWriter<?> ep = pi.getProvider();
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderProduceTypes(ep);

        List<MediaType> availableMimeTypes =
            JAXRSUtils.intersectMimeTypes(Collections.singletonList(mediaType),
                                                                           supportedMediaTypes, false);

        boolean b = availableMimeTypes.size() != 0;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "matchesWriterMediaTypes return " + b);
        }
        return b;
    }

    private boolean isWriteable(ProviderInfo<MessageBodyWriter<?>> pi,
                                Class<?> type,
                                Type genericType,
                                Annotation[] annotations,
                                MediaType mediaType,
                                Message m) {
        MessageBodyWriter<?> ep = pi.getProvider();
        if (m.get(ACTIVE_JAXRS_PROVIDER_KEY) != ep) {
            injectContextValues(pi, m);
        }
        return ep.isWriteable(type, genericType, annotations, mediaType);
    }

    List<ProviderInfo<MessageBodyReader<?>>> getMessageReaders() {
        return Collections.unmodifiableList(messageReaders);
    }

    List<ProviderInfo<MessageBodyWriter<?>>> getMessageWriters() {
        return Collections.unmodifiableList(messageWriters);
    }

    List<ProviderInfo<ContextResolver<?>>> getContextResolvers() {
        return Collections.unmodifiableList(contextResolvers);
    }

    public void registerUserProvider(Object provider) {
        setUserProviders(Collections.singletonList(provider));
    }

    /**
     * Use for injection of entityProviders
     *
     * @param entityProviders the entityProviders to set
     */
    public void setUserProviders(List<?> userProviders) {
        setProviders(true, false, userProviders.toArray());
    }

    //https://issues.apache.org/jira/browse/CXF-6380
    private static class MessageBodyReaderComparator implements Comparator<ProviderInfo<MessageBodyReader<?>>> {

        private final Map<MessageBodyReader<?>, List<MediaType>> cache;

        private MessageBodyReaderComparator(Map<MessageBodyReader<?>, List<MediaType>> cache) {
            this.cache = cache;
        }

        @Override
        public int compare(ProviderInfo<MessageBodyReader<?>> p1,
                           ProviderInfo<MessageBodyReader<?>> p2) {
            MessageBodyReader<?> e1 = p1.getOldProvider();
            MessageBodyReader<?> e2 = p2.getOldProvider();

            List<MediaType> types1 = getProviderConsumeTypes(e1, cache);
            types1 = JAXRSUtils.sortMediaTypes(types1, null);
            List<MediaType> types2 = getProviderConsumeTypes(e2, cache);
            types2 = JAXRSUtils.sortMediaTypes(types2, null);

            int result = JAXRSUtils.compareSortedMediaTypes(types1, types2, null);
            if (result != 0) {
                return result;
            }
            result = compareClasses(e1, e2);
            if (result != 0) {
                return result;
            }
            result = compareCustomStatus(p1, p2);
            if (result != 0) {
                return result;
            }
            return comparePriorityStatus(p1.getProvider().getClass(), p2.getProvider().getClass());
        }
    }

    private static class MessageBodyWriterComparator implements Comparator<ProviderInfo<MessageBodyWriter<?>>> {

        private final Map<MessageBodyWriter<?>, List<MediaType>> cache;

        private MessageBodyWriterComparator(Map<MessageBodyWriter<?>, List<MediaType>> cache) {
            this.cache = cache;
        }

        @Override
        public int compare(ProviderInfo<MessageBodyWriter<?>> p1,
                           ProviderInfo<MessageBodyWriter<?>> p2) {
            MessageBodyWriter<?> e1 = p1.getOldProvider();
            MessageBodyWriter<?> e2 = p2.getOldProvider();

            int result = compareClasses(e1, e2);
            if (result != 0) {
                return result;
            }
            List<MediaType> types1 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProviderProduceTypes(e1), JAXRSUtils.MEDIA_TYPE_QS_PARAM);
            List<MediaType> types2 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProviderProduceTypes(e2), JAXRSUtils.MEDIA_TYPE_QS_PARAM);

            result = JAXRSUtils.compareSortedMediaTypes(types1, types2, JAXRSUtils.MEDIA_TYPE_QS_PARAM);
            if (result != 0) {
                return result;
            }
            result = compareCustomStatus(p1, p2);
            if (result != 0) {
                return result;
            }
            return comparePriorityStatus(p1.getProvider().getClass(), p2.getProvider().getClass());
        }
    }

    static int compareCustomStatus(ProviderInfo<?> p1, ProviderInfo<?> p2) {
        Boolean custom1 = p1.isCustom();
        Boolean custom2 = p2.isCustom();
        int result = custom1.compareTo(custom2) * -1;
        if (result == 0 && custom1) {
            Boolean busGlobal1 = p1.isBusGlobal();
            Boolean busGlobal2 = p2.isBusGlobal();
            result = busGlobal1.compareTo(busGlobal2);
        }
        return result;
    }


    static int comparePriorityStatus(Class<?> cl1, Class<?> cl2) {
        Integer value1 = AnnotationUtils.getBindingPriority(cl1);
        Integer value2 = AnnotationUtils.getBindingPriority(cl2);
        return value1.compareTo(value2);
    }

    private static class ContextResolverComparator
        implements Comparator<ProviderInfo<ContextResolver<?>>> {
        @Override
        public int compare(ProviderInfo<ContextResolver<?>> p1,
                           ProviderInfo<ContextResolver<?>> p2) {
            ContextResolver<?> e1 = p1.getOldProvider();
            ContextResolver<?> e2 = p2.getOldProvider();

            List<MediaType> types1 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProduceTypes(
                     e1.getClass().getAnnotation(Produces.class)), JAXRSUtils.MEDIA_TYPE_QS_PARAM);
            List<MediaType> types2 =
                JAXRSUtils.sortMediaTypes(JAXRSUtils.getProduceTypes(
                     e2.getClass().getAnnotation(Produces.class)), JAXRSUtils.MEDIA_TYPE_QS_PARAM);

            return JAXRSUtils.compareSortedMediaTypes(types1, types2, JAXRSUtils.MEDIA_TYPE_QS_PARAM);
        }
    }

    public void clearThreadLocalProxies() {
        clearProxies(injectedProviders);
    }

    void clearProxies(Collection<?>... lists) {
        for (Collection<?> list : lists) {
            Collection<ProviderInfo<?>> l2 = CastUtils.cast(list);
            for (ProviderInfo<?> pi : l2) {
                pi.clearThreadLocalProxies();
            }
        }
    }

    public void clearProviders() {
        messageReaders.clear();
        messageWriters.clear();
        contextResolvers.clear();
        contextProviders.clear();
        readerInterceptors.clear();
        writerInterceptors.clear();
        paramConverters.clear();
    }

    public void setBus(Bus bus) {
        if (bus == null) {
            return;
        }
        for (ProviderInfo<MessageBodyReader<?>> r : messageReaders) {
            injectProviderProperty(r.getProvider(), "setBus", Bus.class, bus);
        }
    }

    @FFDCIgnore(value = { Exception.class }) // Liberty Change
    private boolean injectProviderProperty(Object provider, String mName, Class<?> pClass,
                                           Object pValue) {
        try {
            Method m = provider.getClass().getMethod(mName, new Class[] { pClass });
            m.invoke(provider, new Object[] { pValue });
            return true;
        } catch (Exception ex) {
            // ignore
        }
        return false;
    }

    public void setSchemaLocations(List<String> schemas) {
        for (ProviderInfo<MessageBodyReader<?>> r : messageReaders) {
            injectProviderProperty(r.getProvider(), "setSchemaLocations", List.class, schemas);
        }
    }

    protected static <T> List<ProviderInfo<T>> getBoundFilters(Map<NameKey, ProviderInfo<T>> boundFilters,
                                                               Set<String> names) {
        if (boundFilters.isEmpty()) {
            return Collections.emptyList();
        }
        names = names == null ? Collections.<String> emptySet() : names;

        MultivaluedMap<ProviderInfo<T>, String> map =
            new MetadataMap<ProviderInfo<T>, String>();
        for (Map.Entry<NameKey, ProviderInfo<T>> entry : boundFilters.entrySet()) {
            String entryName = entry.getKey().getName();
            ProviderInfo<T> provider = entry.getValue();
            if (entryName.equals(DEFAULT_FILTER_NAME_BINDING)) {
                map.put(provider, Collections.<String> emptyList());
            } else {
                if (provider instanceof FilterProviderInfo) {
                    FilterProviderInfo<?> fpi = (FilterProviderInfo<?>) provider;
                    if (fpi.isDynamic() && !names.containsAll(fpi.getNameBinding())) {
                        continue;
                    }
                }
                map.add(provider, entryName);
            }
        }
        List<ProviderInfo<T>> list = new LinkedList<ProviderInfo<T>>();
        for (Map.Entry<ProviderInfo<T>, List<String>> entry : map.entrySet()) {
            List<String> values = entry.getValue();
            if (names.containsAll(values)) {
                ProviderInfo<T> provider = entry.getKey();
                list.add(provider);
            }
        }
        return list;
    }

    public void initProviders(List<ClassResourceInfo> cris) {
        Set<Object> set = getReadersWriters();
        for (Object o : set) {
            Object provider = ((ProviderInfo<?>) o).getProvider();
            if (provider instanceof AbstractConfigurableProvider) {
                ((AbstractConfigurableProvider) provider).init(cris);
            }
        }
    }

    Set<Object> getReadersWriters() {
        Set<Object> set = new HashSet<>();
        set.addAll(messageReaders);
        set.addAll(messageWriters);
        return set;
    }

    public static class ClassComparator implements
        Comparator<Object> {
        private Class<?> expectedCls;
        public ClassComparator() {
        }
        public ClassComparator(Class<?> expectedCls) {
            this.expectedCls = expectedCls;
        }

        @Override
        public int compare(Object em1, Object em2) {
            return compareClasses(expectedCls, em1, em2);
        }
    }

    static class PriorityBasedClassComparator extends ClassComparator {
        PriorityBasedClassComparator() {
            super();
        }

        PriorityBasedClassComparator(Class<?> expectedCls) {
            super(expectedCls);
        }

        @Override
        public int compare(Object em1, Object em2) {
            int result = super.compare(em1, em2);
            if (result == 0) {
                result = comparePriorityStatus(em1.getClass(), em2.getClass());
            }
            return result;
        }
    }

    public static class ProviderInfoClassComparator implements Comparator<ProviderInfo<?>> {
        final Comparator<Object> comp;
        boolean defaultComp;

        public ProviderInfoClassComparator(Class<?> expectedCls) {
            this.comp = new ClassComparator(expectedCls);
            this.defaultComp = true;
        }

        public ProviderInfoClassComparator(Comparator<Object> comp) {
            this.comp = comp;
        }

        @Override
        public int compare(ProviderInfo<?> p1, ProviderInfo<?> p2) {
            int result = comp.compare(p1.getProvider(), p2.getProvider());
            if (result == 0 && defaultComp) {
                result = compareCustomStatus(p1, p2);
            }
            return result;
        }
    }

    public static ProviderFactory getInstance(Message m) {
        Endpoint e = m.getExchange().getEndpoint();

        Message outM = m.getExchange().getOutMessage();
        boolean isClient = outM != null && MessageUtils.isRequestor(outM);
        String name = isClient ? CLIENT_FACTORY_NAME : SERVER_FACTORY_NAME;

        return (ProviderFactory) e.get(name);
    }

    protected static int compareClasses(Object o1, Object o2) {
        return compareClasses(null, o1, o2);
    }

    protected static int compareClasses(Class<?> expectedCls, Object o1, Object o2) {
        Class<?> cl1 = ClassHelper.getRealClass(o1);
        Class<?> cl2 = ClassHelper.getRealClass(o2);
        Type[] types1 = getGenericInterfaces(cl1, expectedCls);
        Type[] types2 = getGenericInterfaces(cl2, expectedCls);
        if (types1.length == 0 && types2.length == 0) {
            return 0;
        } else if (types1.length == 0 && types2.length > 0) {
            return 1;
        } else if (types1.length > 0 && types2.length == 0) {
            return -1;
        }

        Class<?> realClass1 = InjectionUtils.getActualType(types1[0]);
        Class<?> realClass2 = InjectionUtils.getActualType(types2[0]);
        if (realClass1 == realClass2) {
            return 0;
        }
        if (realClass1.isAssignableFrom(realClass2)) {
            // subclass should go first
            return 1;
        } else if (realClass2.isAssignableFrom(realClass1)) {
            return -1;
        }
        return 0;
    }

    //Liberty code change start
    //defect 178126
    //Add the result to cache before return
    private static Type[] getGenericInterfaces(Class<?> cls, Class<?> expectedClass) {
        if (Object.class == cls) {
            return emptyType;
        }
        ClassPair classPair = new ClassPair(cls, expectedClass);
        Type[] cachedTypes = genericInterfacesCache.get(classPair);
        if (cachedTypes != null)
            return cachedTypes;
        if (expectedClass != null) {
            Type genericSuperType = cls.getGenericSuperclass();
            if (genericSuperType instanceof ParameterizedType) {
                Class<?> actualType = InjectionUtils.getActualType(genericSuperType);
                if (actualType != null && actualType.isAssignableFrom(expectedClass)) {
                    Type[] tempTypes = new Type[] { genericSuperType };
                    genericInterfacesCache.put(classPair, tempTypes);
                    return tempTypes;
                } else if (expectedClass.isAssignableFrom(actualType)) {
                    genericInterfacesCache.put(classPair, emptyType);
                    return emptyType;
                }
            }
        }
        Type[] types = cls.getGenericInterfaces();
        if (types.length > 0) {
            genericInterfacesCache.put(classPair, types);
            return types;
        }
        Type[] superGenericTypes = getGenericInterfaces(cls.getSuperclass(), expectedClass);
        genericInterfacesCache.put(classPair, superGenericTypes);
        return superGenericTypes;
    }

    //Liberty code change end

    protected static class AbstractPriorityComparator {

        private final boolean ascending;

        protected AbstractPriorityComparator(boolean ascending) {
            this.ascending = ascending;
        }

        protected int compare(Integer b1Value, Integer b2Value) {
            int result = b1Value.compareTo(b2Value);
            return ascending ? result : result * -1;
        }

    }

    protected static class BindingPriorityComparator extends AbstractPriorityComparator
        implements Comparator<ProviderInfo<?>> {
        private final Class<?> providerCls;

        public BindingPriorityComparator(Class<?> providerCls, boolean ascending) {
            super(ascending);
            this.providerCls = providerCls;
        }

        @Override
        public int compare(ProviderInfo<?> p1, ProviderInfo<?> p2) {
            return compare(getFilterPriority(p1, providerCls),
                           getFilterPriority(p2, providerCls));
        }

    }

    static class ContextResolverProxy<T> implements ContextResolver<T> {
        private final List<ContextResolver<T>> candidates;

        ContextResolverProxy(List<ContextResolver<T>> candidates) {
            this.candidates = candidates;
        }

        @Override
        public T getContext(Class<?> cls) {
            for (ContextResolver<T> resolver : candidates) {
                T context = resolver.getContext(cls);
                if (context != null) {
                    return context;
                }
            }
            return null;
        }

        public List<ContextResolver<T>> getResolvers() {
            return candidates;
        }
    }

    public static ProviderInfo<? extends Object> createProviderFromConstructor(Constructor<?> c,
                                                                               Map<Class<?>, Object> values,
                                                                               Bus theBus,
                                                                               boolean checkContexts,
                                                                               boolean custom) {


        Map<Class<?>, Map<Class<?>, ThreadLocalProxy<?>>> proxiesMap =
            CastUtils.cast((Map<?, ?>)theBus.getProperty(AbstractResourceInfo.CONSTRUCTOR_PROXY_MAP));
        Map<Class<?>, ThreadLocalProxy<?>> existingProxies = null;
        if (proxiesMap != null) {
            existingProxies = proxiesMap.get(c.getDeclaringClass());
        }
        Class<?>[] paramTypes = c.getParameterTypes();
        Object[] cArgs = ResourceUtils.createConstructorArguments(c, null, false, values);
        if (existingProxies != null && existingProxies.size() <= paramTypes.length) {
            for (int i = 0; i < paramTypes.length; i++) {
                if (cArgs[i] instanceof ThreadLocalProxy) {
                    cArgs[i] = existingProxies.get(paramTypes[i]);
                }
            }
        }
        Object instance = null;
        try {
            instance = c.newInstance(cArgs);
        } catch (Throwable ex) {
            throw new RuntimeException("Resource or provider class " + c.getDeclaringClass().getName()
                                       + " can not be instantiated");
        }
        Map<Class<?>, ThreadLocalProxy<?>> proxies =
            new LinkedHashMap<Class<?>, ThreadLocalProxy<?>>();
        for (int i = 0; i < paramTypes.length; i++) {
            if (cArgs[i] instanceof ThreadLocalProxy) {
                @SuppressWarnings("unchecked")
                ThreadLocalProxy<Object> proxy = (ThreadLocalProxy<Object>) cArgs[i];
                proxies.put(paramTypes[i], proxy);
            }
        }
        boolean isApplication = Application.class.isAssignableFrom(c.getDeclaringClass());
        if (isApplication) {
            return new ApplicationInfo((Application) instance, proxies, theBus);
        } else {
            return new ProviderInfo<Object>(instance, proxies, theBus, checkContexts, custom);
        }
    }

    protected static class NameKey {
        private final String name;
        private final Integer priority;
        private final Class<?> providerCls;

        public NameKey(String name,
                       int priority,
                       Class<?> providerCls) {
            this.name = name;
            this.priority = priority;
            this.providerCls = providerCls;
        }

        public String getName() {
            return name;
        }

        public Integer getPriority() {
            return priority;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NameKey)) {
                return false;
            }
            NameKey other = (NameKey) o;
            return name.equals(other.name) && priority.equals(other.priority)
                   && providerCls == other.providerCls;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public String toString() {
            return name + ":" + priority;
        }
    }

    protected static <T> void mapInterceptorFilters(Map<NameKey, ProviderInfo<T>> map,
                                                    List<ProviderInfo<T>> filters,
                                                    Class<?> providerCls,
                                                    boolean ascending) {

        for (ProviderInfo<T> p : filters) {
            Set<String> names = getFilterNameBindings(p);

            int priority = getFilterPriority(p, providerCls);

            for (String name : names) {
                map.put(new NameKey(name, priority, p.getClass()), p);
            }
        }

    }

    protected static Set<String> getFilterNameBindings(ProviderInfo<?> p) {
        Set<String> names = null;
        if (p instanceof FilterProviderInfo) {
            names = ((FilterProviderInfo<?>) p).getNameBinding();
        }
        if (names == null) {
            Class<?> pClass = ClassHelper.getRealClass(p.getBus(), p.getProvider());
            names = AnnotationUtils.getNameBindings(pClass.getAnnotations());
        }
        if (names.isEmpty()) {
            names = Collections.singleton(DEFAULT_FILTER_NAME_BINDING);
        }
        return names;
    }

    protected static int getFilterPriority(ProviderInfo<?> p, Class<?> providerCls) {
        return p instanceof FilterProviderInfo ? ((FilterProviderInfo<?>)p).getPriority(providerCls)
            : AnnotationUtils.getBindingPriority(p.getProvider().getClass());
    }

    protected static class NameKeyComparator extends AbstractPriorityComparator
        implements Comparator<NameKey> {

        public NameKeyComparator(boolean ascending) {
            super(ascending);
        }

        @Override
        public int compare(NameKey key1, NameKey key2) {
            int result = compare(key1.getPriority(), key2.getPriority());
            if (result != 0) {
                return result;
            }
            return compare(key1.hashCode(), key2.hashCode());
        }

    }

    protected static class NameKeyMap<T> extends TreeMap<NameKey, T> {
        private static final long serialVersionUID = -4352258671270502204L;

        public NameKeyMap(boolean ascending) {
            super(new NameKeyComparator(ascending));
        }
    }

    protected static boolean filterContractSupported(ProviderInfo<?> provider,
                                                     Class<?> providerCls,
                                                     Class<?> contract) {
        boolean result = false;
        if (contract.isAssignableFrom(providerCls)) {
            Set<Class<?>> actualContracts = null;
            if (provider instanceof FilterProviderInfo) {
                actualContracts = ((FilterProviderInfo<?>) provider).getSupportedContracts();
            }
            if (actualContracts != null) {
                result = actualContracts.contains(contract);
            } else {
                result = true;
            }
        }
        return result;
    }

    // Liberty Change for CXF Begin
    protected List<ProviderInfo<? extends Object>> prepareProviders(boolean custom,
                                                                    boolean busGlobal,
                                                                    Object[] providers,
                                                                    ProviderInfo<Application> application) {
        List<ProviderInfo<? extends Object>> theProviders =
            new ArrayList<ProviderInfo<? extends Object>>(providers.length);
        @SuppressWarnings("unchecked")
        Map<String, Object> beanCustomizerContexts = (Map<String, Object>) getBus().getProperty(JaxRsConstants.ENDPOINT_BEANCUSTOMIZER_CONTEXTOBJ);

        for (Object o : providers) {
            if (o == null) {
                continue;
            }

            /**
             * using POJO object to create ProviderInfo to get info of @Context
             */
            ProviderInfo<? extends Object> pi = null;

            if (o instanceof Constructor) {
                Map<Class<?>, Object> values = CastUtils.cast(application == null ? null : Collections.singletonMap(Application.class, application.getProvider()));
                pi = createProviderFromConstructor((Constructor<?>) o, values, getBus(), true, custom);

            } else if (o instanceof ProviderInfo) {
                pi = (ProviderInfo<?>) o;
            } else {
                pi = new ProviderInfo<Object>(o, getBus(), custom);
                pi.setBusGlobal(busGlobal);
            }

            /**
             * then using EJB proxy to help CXF classify provider type
             */
            JaxRsFactoryBeanCustomizer beanCustomizer = InjectionRuntimeContextHelper.findBeanCustomizer(o.getClass(), getBus());
            if (beanCustomizer != null) {
                Object proxyObject = beanCustomizer.onSetupProviderProxy(o, beanCustomizerContexts.get(CustomizerUtils.createCustomizerKey(beanCustomizer)));

                if (proxyObject != null && (proxyObject != o || !proxyObject.equals(o))) {
                    pi.setProvider(proxyObject);
                }
            }

            if (beanCustomizer != null && DynamicFeature.class.isAssignableFrom(pi.getProvider().getClass())) {
                Object newProviderInstance = beanCustomizer.onSingletonProviderInit(pi.getProvider(), beanCustomizerContexts.get(CustomizerUtils.createCustomizerKey(beanCustomizer)),
                                                                                    null);
                if (newProviderInstance != null) {
                    pi.setProvider(newProviderInstance);
                }

            }

            theProviders.add(pi);
        }

        return theProviders;
    }

    // Liberty Change for CXF End
    public MessageBodyWriter<?> getDefaultJaxbWriter() {
        for (ProviderInfo<MessageBodyWriter<?>> pi : this.messageWriters) {
            Class<?> cls = pi.getProvider().getClass();
            if (cls.getName().equals(JAXB_PROVIDER_NAME)) {
                return pi.getProvider();
            }
        }
        return null;
    }

    public Comparator<?> getProviderComparator() {
        return providerComparator;
    }

    public void setProviderComparator(Comparator<?> providerComparator) {
        this.providerComparator = providerComparator;
        sortReaders();
        sortWriters();
    }
}

//Liberty code change start
//defect 178126
class ClassPair {
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((firstClass == null) ? 0 : firstClass.hashCode());
        result = prime * result + ((secondClass == null) ? 0 : secondClass.hashCode());
        result = prime * result + ((firstClassLoader == null) ? 0 : firstClassLoader.hashCode());
        result = prime * result + ((secondClassLoader == null) ? 0 : secondClassLoader.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClassPair other = (ClassPair) obj;
        if (firstClass == null) {
            if (other.firstClass != null)
                return false;
        } else if (!firstClass.equals(other.firstClass))
            return false;
        if (secondClass == null) {
            if (other.secondClass != null)
                return false;
        } else if (!secondClass.equals(other.secondClass))
            return false;

        if (firstClassLoader == null) {
            if (other.firstClassLoader != null)
                return false;
        } else if (!firstClassLoader.equals(other.firstClassLoader))
            return false;
        if (secondClassLoader == null) {
            if (other.secondClassLoader != null)
                return false;
        } else if (!secondClassLoader.equals(other.secondClassLoader))
            return false;

        return true;
    }

    private String getClassLoaderString(final Class<?> cls) {
        return cls == null ? null : AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                ClassLoader cl = cls.getClassLoader();
                return cl == null ? null : cl.getClass().getName() + "." + cl.hashCode();
            }
        });
    }

    private final String firstClass;
    private final String secondClass;
    private final String firstClassLoader;
    private final String secondClassLoader;

    public ClassPair(Class<?> firstClass, Class<?> secondClass) {
        this.firstClass = firstClass == null ? null : firstClass.getName();
        this.secondClass = secondClass == null ? null : secondClass.getName();
        this.firstClassLoader = getClassLoaderString(firstClass);
        this.secondClassLoader = getClassLoaderString(secondClass);
    }
}
//Liberty code change end
