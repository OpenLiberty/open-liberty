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

package org.apache.cxf.jaxrs.provider;

import java.lang.annotation.Annotation;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;

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
import com.ibm.ws.jaxrs20.providers.multipart.IBMMultipartProvider;
import com.ibm.ws.jaxrs20.utils.CustomizerUtils;
import com.ibm.ws.jaxrs21.providers.json.JsonBProvider;
import com.ibm.ws.jaxrs21.providers.json.JsonPProvider;

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

    // Liberty start:  The following code was added with CXF 3.4.3 but causes potential issues for Liberty and
    // is therefore commented out.
 /*   static class LazyProviderClass {
        // class to Lazily call the ClassLoaderUtil.loadClass, but do it once
        // and cache the result.  Then use the class to create instances as needed.
        // This avoids calling loadClass every time a factory is initialized as
        // calling loadClass is super expensive, particularly if the class
        // cannot be found and particularly in osgi where the search is very complex.
        // This would record that the class is not found and prevent future
        // searches.
        final String className;
        volatile boolean initialized;
        Class<?> cls;

        LazyProviderClass(String cn) {
            className = cn;
        }

        synchronized void loadClass() {
            if (!initialized) {
                try {
                    cls = ClassLoaderUtils.loadClass(className, ProviderFactory.class);
                } catch (final Throwable ex) {
				    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, className + " not available, skipping");
				    }  
                }
                initialized = true;
            }
        }

        public Object tryCreateInstance(Bus bus) {
            if (!initialized) {
                loadClass();
            }
            if (cls != null) {
                try {
                    for (Constructor<?> c : cls.getConstructors()) {
                        if (c.getParameterTypes().length == 1 && c.getParameterTypes()[0] == Bus.class) {
                            return c.newInstance(bus);
                        }
                    }
                    return cls.newInstance();
                } catch (Throwable ex) {
                    String message = "Problem with creating the provider " + className;
                    if (ex.getMessage() != null) {
                        message += ": " + ex.getMessage();
                    } else {
                        message += ", exception class : " + ex.getClass().getName();
                    }
 				    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, message);
				    }  
                }
            }
            return null;
        }
    };

    private static final LazyProviderClass DATA_SOURCE_PROVIDER_CLASS =
        new LazyProviderClass("org.apache.cxf.jaxrs.provider.DataSourceProvider");
    private static final LazyProviderClass JAXB_PROVIDER_CLASS =
        new LazyProviderClass(JAXB_PROVIDER_NAME);
    private static final LazyProviderClass JAXB_ELEMENT_PROVIDER_CLASS =
        new LazyProviderClass("org.apache.cxf.jaxrs.provider.JAXBElementTypedProvider");
    private static final LazyProviderClass MULTIPART_PROVIDER_CLASS =
        new LazyProviderClass("org.apache.cxf.jaxrs.provider.MultipartProvider");
*/
    //Liberty end
    protected Map<NameKey, ProviderInfo<ReaderInterceptor>> readerInterceptors =
        new NameKeyMap<>(true);
    protected Map<NameKey, ProviderInfo<WriterInterceptor>> writerInterceptors =
        new NameKeyMap<>(true);

    //Liberty code change start
    private final AtomicReferenceProviderList<MessageBodyReader<?>> messageReaders =
        new AtomicReferenceProviderList<>();
    private final AtomicReferenceProviderList<MessageBodyWriter<?>> messageWriters =
        new AtomicReferenceProviderList<>();
    private final AtomicReferenceProviderList<ContextResolver<?>> contextResolvers =
        new AtomicReferenceProviderList<>();
    private final AtomicReferenceProviderList<ContextProvider<?>> contextProviders =
        new AtomicReferenceProviderList<>();
    private final AtomicReferenceProviderList<ParamConverterProvider> paramConverters =
                    new AtomicReferenceProviderList<>();
    //Liberty code change end


    private boolean paramConverterContextsAvailable;
    // List of injected providers
    private Collection<ProviderInfo<?>> injectedProviders =
        new HashSet<>();

    private Bus bus;

    private Comparator<?> providerComparator;

    private ProviderCache providerCache;
    //Liberty code change start
    //defect 178126
    //A cache for getGenericInterfaces
    private static final ConcurrentHashMap<ClassesKey, Type[]> genericInterfacesCache = new ConcurrentHashMap<>();
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
        //Liberty change start - CXF does not check system properties, Liberty does
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
        // Liberty change end
        return new ProviderCache(checkAll);
    }

    protected static void initFactory(ProviderFactory factory) {
        // ensure to not load providers not available in a module environment if not needed
        factory.setProviders(false,
                             false,
                     new BinaryDataProvider<Object>(),
                     new SourceProvider<Object>(),
                     //DATA_SOURCE_PROVIDER_CLASS.tryCreateInstance(factory.getBus()),
                     new DataSourceProvider<Object>(), // Liberty change - tryCreateInstance changes behavior
                     new FormEncodingProvider<Object>(),
                     new StringTextProvider(),
                     new PrimitiveTextProvider<Object>(),
                     //JAXB_PROVIDER_CLASS.tryCreateInstance(factory.getBus()),
                     new JAXBElementProvider<Object>(), // Liberty change - tryCreateInstance changes behavior
                     //JAXB_ELEMENT_PROVIDER_CLASS.tryCreateInstance(factory.getBus()),
                     new JAXBElementTypedProvider(), // Liberty change - tryCreateInstance changes behavior
                     createJsonpProvider(), // Liberty Change for CXF Begin
                     createJsonBindingProvider(factory.contextResolvers),
                     new IBMMultipartProvider(), // Liberty Change for CXF End
                     //MULTIPART_PROVIDER_CLASS.tryCreateInstance(factory.getBus()));
                     new MultipartProvider());// Liberty change - tryCreateInstance changes behavior
        
        // Liberty change begin
        // Liberty sets JSON providers above and does not ship the CXF JSONProvider
        /*Object prop = factory.getBus().getProperty("skip.default.json.provider.registration");
        if (!PropertyUtils.isTrue(prop)) {
            factory.setProviders(false, false, createProvider(JSON_PROVIDER_NAME, factory.getBus()));
        }*/
        // Liberty change end
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
    public static Object createJsonpProvider() {
        JsonProvider jsonProvider = AccessController.doPrivileged(new PrivilegedAction<JsonProvider>(){

            @Override
            public JsonProvider run() {
                try {
                Bundle b = FrameworkUtil.getBundle(ProviderFactory.class);
                if(b != null) {
                    BundleContext bc = b.getBundleContext();
                    ServiceReference<JsonProvider> sr = bc.getServiceReference(JsonProvider.class);
                    return (JsonProvider)bc.getService(sr);
                }
                } catch (NoClassDefFoundError ncdfe) {
                    // ignore - return null
                }
                return null;
            }});

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

    public static Object createJsonBindingProvider(Iterable<ProviderInfo<ContextResolver<?>>> contextResolvers) {
        JsonbProvider jsonbProvider = AccessController.doPrivileged(new PrivilegedAction<JsonbProvider>(){

            @Override
            public JsonbProvider run() {
                try {
                Bundle b = FrameworkUtil.getBundle(ProviderFactory.class);
                if(b != null) {
                    BundleContext bc = b.getBundleContext();
                    ServiceReference<JsonbProvider> sr = bc.getServiceReference(JsonbProvider.class);
                    return (JsonbProvider)bc.getService(sr);
                }
                } catch (NoClassDefFoundError ncdfe) {
                    // ignore - return null
                }
                return null;
            }});

        return new JsonBProvider(jsonbProvider, contextResolvers);
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
        List<ContextResolver<T>> candidates = new LinkedList<>();
        for (ProviderInfo<ContextResolver<?>> cr : contextResolvers) {
            Type[] types = cr.getProvider().getClass().getGenericInterfaces();
            for (Type t : types) {
                if (t instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType)t;
                    Type[] args = pt.getActualTypeArguments();
                    if (args.length > 0) {
                        Class<?> argCls = InjectionUtils.getActualType(args[0]);

                        if (argCls != null && argCls.isAssignableFrom(contextCls)) {
                            List<MediaType> mTypes = JAXRSUtils.getProduceTypes(
                                 cr.getProvider().getClass().getAnnotation(Produces.class));
                            if (JAXRSUtils.doMimeTypesIntersect(mTypes, type)) {
                                injectContextValues(cr, m);
                                candidates.add((ContextResolver<T>)cr.getProvider());
                            }
                        }
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
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
                    ParameterizedType pt = (ParameterizedType)t;
                    Type[] args = pt.getActualTypeArguments();
                    if (args.length > 0) {
                        Class<?> argCls = InjectionUtils.getActualType(args[0]);

                        if (argCls != null && argCls.isAssignableFrom(contextCls)) {
                            return (ContextProvider<T>)cr.getProvider();
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


        anns = anns != null ? anns : new Annotation[]{};
        for (ProviderInfo<ParamConverterProvider> pi : paramConverters) {
            injectContextValues(pi, m);
            ParamConverter<T> converter = pi.getProvider().getConverter(paramType, genericType, anns);
            if (converter != null) {
                return converter;
            }
            pi.clearThreadLocalProxies();
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
        if (m != null && MessageUtils.getContextualBoolean(m, IGNORE_TYPE_VARIABLES)) {
            types = new Type[] { mapperClass };
        } else {
            types = getGenericInterfaces(mapperClass, expectedType);
        }

        for (Type t : types) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                Type[] args = pt.getActualTypeArguments();
                for (Type arg : args) {
                    if (arg instanceof TypeVariable) {
                        TypeVariable<?> var = (TypeVariable<?>) arg;
                        Type[] bounds = var.getBounds();
                        boolean isResolved = false;
                        for (Type bound : bounds) {
                            Class<?> cls = InjectionUtils.getRawType(bound);
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
        return handleMapper(em, expectedType, m, providerClass, null, injectContext);
    }
    
    protected <T> boolean handleMapper(ProviderInfo<T> em,
                                       Class<?> expectedType,
                                       Message m,
                                       Class<?> providerClass,
                                       Class<?> commonBaseClass,
                                       boolean injectContext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "handleMapper", new Object[]{em, expectedType, m, providerClass, commonBaseClass, injectContext});
        }
        // Liberty Change for CXF Begin
        Class<?> mapperClass = ClassHelper.getRealClass(bus, em.getOldProvider());
        // Liberty Change for CXF End
        Type[] types = null;
        if (m != null && MessageUtils.getContextualBoolean(m, IGNORE_TYPE_VARIABLES)) {
            types = new Type[]{mapperClass};
        } else {
            types = getGenericInterfaces(mapperClass, expectedType, commonBaseClass);
        }
        for (Type t : types) {
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType)t;
                Type[] args = pt.getActualTypeArguments();
                for (Type arg : args) {
                    if (arg instanceof TypeVariable) {
                        TypeVariable<?> var = (TypeVariable<?>) arg;
                        Type[] bounds = var.getBounds();
                        boolean isResolved = false;
                        for (Type bound : bounds) {
                            Class<?> cls = InjectionUtils.getRawType(bound);
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
            } else if (t instanceof Class && providerClass.isAssignableFrom((Class<?>)t)) {
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
            ReaderInterceptor mbrReader = new ReaderInterceptorMBR(mr, getResponseMessage(m));

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
            WriterInterceptor mbwWriter = new WriterInterceptorMBW((MessageBodyWriter)mw, m);

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
                    return (MessageBodyReader<T>)ep.getProvider();
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
                    selectedReader = (MessageBodyReader<T>)ep.getProvider();
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
                    return (MessageBodyWriter<T>)ep.getProvider();
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
                    selectedWriter = (MessageBodyWriter<T>)ep.getProvider();
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
        List<Object> extensions = new LinkedList<>();
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
            List<Object> all = (List<Object>)allProp;
            extensions.addAll(all);
        }
    }

    protected abstract void setProviders(boolean custom, boolean busGlobal, Object... providers);

    @SuppressWarnings("unchecked")
    protected void setCommonProviders(List<ProviderInfo<? extends Object>> theProviders) {
        //Liberty code change start
        List<ProviderInfo<MessageBodyReader<?>>> newReaders = new ArrayList<>();
        List<ProviderInfo<MessageBodyWriter<?>>> newWriters = new ArrayList<>();
        List<ProviderInfo<ContextResolver<?>>> newResolvers = new ArrayList<>();
        List<ProviderInfo<ContextProvider<?>>> newContextProviders = new ArrayList<>();
        List<ProviderInfo<ParamConverterProvider>> newParamConverters = new ArrayList<>();
        //Liberty code change end

        List<ProviderInfo<ReaderInterceptor>> readInts =
            new LinkedList<>();
        List<ProviderInfo<WriterInterceptor>> writeInts =
            new LinkedList<>();
        for (ProviderInfo<? extends Object> provider : theProviders) {
            Class<?> providerCls = ClassHelper.getRealClass(bus, provider.getProvider());

            if (filterContractSupported(provider, providerCls, MessageBodyReader.class)) {
                //Liberty code change start
                addProviderToList(newReaders, provider);
                //Liberty code change end
            }

            if (filterContractSupported(provider, providerCls, MessageBodyWriter.class)) {
                //Liberty code change start
                addProviderToList(newWriters, provider);
                //Liberty code change end
            }

            if (filterContractSupported(provider, providerCls, ContextResolver.class)) {
                //Liberty code change start
                addProviderToList(newResolvers, provider);
                //Liberty code change end
            }

            if (ContextProvider.class.isAssignableFrom(providerCls)) {
                //Liberty code change start
                addProviderToList(newContextProviders, provider);
                //Liberty code change end
            }

            if (filterContractSupported(provider, providerCls, ReaderInterceptor.class)) {
                readInts.add((ProviderInfo<ReaderInterceptor>)provider);
            }

            if (filterContractSupported(provider, providerCls, WriterInterceptor.class)) {
                writeInts.add((ProviderInfo<WriterInterceptor>)provider);
            }

            if (filterContractSupported(provider, providerCls, ParamConverterProvider.class)) {
                //Liberty code change start
                addProviderToList(newParamConverters, provider);
                //Liberty code change end
            }
        }
        //Liberty code change start
        if (newReaders.size() > 0) {
            addAndSortReaders(newReaders, false);
        }
        if (newWriters.size() > 0) {
            addAndSortWriters(newWriters, false);
        }
        if (newResolvers.size() > 0) {
            contextResolvers.addAndSortProviders(newResolvers, new ContextResolverComparator(), false);
        }
        if (newContextProviders.size() > 0) {
            contextProviders.addProviders(newContextProviders);
        }
        if (newParamConverters.size() > 0) {
            addAndSortParamConverters(newParamConverters, false);
        }
        //Liberty code change end

        mapInterceptorFilters(readerInterceptors, readInts, ReaderInterceptor.class, true);
        mapInterceptorFilters(writerInterceptors, writeInts, WriterInterceptor.class, true);

        //Liberty code change start
        injectContextProxies(messageReaders.get(), messageWriters.get(), contextResolvers.get(), paramConverters.get(),
        //Liberty code change end
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

    protected void injectContextProxies(Collection<?> ... providerLists) {
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

    //Liberty code change start
    private void addAndSortReaders(List<ProviderInfo<MessageBodyReader<?>>> newReaders, boolean forceSort) {
        Comparator<ProviderInfo<MessageBodyReader<?>>> comparator = null;
        if (!customComparatorAvailable(MessageBodyReader.class)) {
            comparator = new MessageBodyReaderComparator(readerMediaTypesMap);
        }

        messageReaders.addAndSortProviders(newReaders, comparator, forceSort);
    }

    private void addAndSortWriters(List<ProviderInfo<MessageBodyWriter<?>>> newWriters, boolean forceSort) {
        Comparator<ProviderInfo<MessageBodyWriter<?>>> comparator = null;
        if (!customComparatorAvailable(MessageBodyWriter.class)) {
            comparator = new MessageBodyWriterComparator(writerMediaTypesMap);
        }

        messageWriters.addAndSortProviders(newWriters, comparator, forceSort);
    }

    protected class AtomicReferenceProviderList<T> implements Iterable<ProviderInfo<T>> {
        private final AtomicReference<List<ProviderInfo<T>>> referent;

        public AtomicReferenceProviderList() {
            referent = new AtomicReference<>(Collections.emptyList());
        }

        /*
         * sorts the available providers according to the media types they declare
         * support for. Sorting of media types follows the general rule: x/y < * x < *,
         * i.e. a provider that explicitly lists a media types is sorted before a
         * provider that lists *. Quality parameter values are also used such that
         * x/y;q=1.0 < x/y;q=0.7.
         */
        public void addAndSortProviders(List<ProviderInfo<T>> providers,
                                        Comparator<ProviderInfo<T>> comparator, boolean forceSort) {
            List<ProviderInfo<T>> currentProviders = null;
            List<ProviderInfo<T>> newProviders = null;
            do {
                currentProviders = referent.get();
                if (providers == null) {
                    if (currentProviders.size() <= 1) {
                        return;
                    }
                    newProviders = new ArrayList<ProviderInfo<T>>(currentProviders);
                } else {
                    if (currentProviders.isEmpty()) {
                        newProviders = providers;
                    } else {
                        newProviders = new ArrayList<ProviderInfo<T>>(currentProviders);
                        for (ProviderInfo<T> provider : providers) {
                            addProviderToList(newProviders, provider);
                        }
                    }
                }

                int newSize = newProviders.size();
                if (!forceSort && newSize == currentProviders.size()) {
                    // If we did not add any more providers because they all were already
                    // in the current list, return to avoid sort and compareAndSet call.
                    return;
                }

                if (newSize > 1) {
                    if (comparator != null) {
                        newProviders.sort(comparator);
                    } else {
                        doCustomSort(newProviders);
                    }
                }
            } while (!referent.compareAndSet(currentProviders, newProviders));

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                StringBuilder msg = new StringBuilder("sortProviders - sorted list:");
                for (int i = 0; i < newProviders.size(); i++) {
                    msg.append(" (" + i + ") " + newProviders.get(i).getProvider());
                }
                Tr.debug(tc, msg.toString());
            }
        }

        public void addProviders(List<ProviderInfo<T>> providers) {
            List<ProviderInfo<T>> currentProviders = null;
            List<ProviderInfo<T>> newProviders = null;
            do {
                currentProviders = referent.get();
                if (currentProviders.isEmpty()) {
                    newProviders = providers;
                } else {
                    newProviders = new ArrayList<ProviderInfo<T>>(currentProviders);
                    for (ProviderInfo<T> provider : providers) {
                        addProviderToList(newProviders, provider);
                    }
                }
            } while (!referent.compareAndSet(currentProviders, newProviders));
        }

        @Override
        public Iterator<ProviderInfo<T>> iterator() {
            return referent.get().iterator();
        }

        public List<ProviderInfo<T>> get() {
            return referent.get();
        }

        public void clear() {
            referent.set(Collections.emptyList());
        }
    }

    private void addAndSortParamConverters(List<ProviderInfo<ParamConverterProvider>> newParamConverters, boolean forceSort) {
        Comparator<ProviderInfo<ParamConverterProvider>> comparator = null;
        if (!customComparatorAvailable(ParamConverter.class)) {
            comparator = new ParamConverterProviderComparator();
        }

        paramConverters.addAndSortProviders(newParamConverters, comparator, forceSort);
    }
    //Liberty code change end

    private boolean customComparatorAvailable(Class<?> providerClass) {
        if (providerComparator != null) {
            Type type = ((ParameterizedType)providerComparator.getClass()
                .getGenericInterfaces()[0]).getActualTypeArguments()[0];
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType)type;
                if (pt.getRawType() == ProviderInfo.class) {
                    Type type2 = pt.getActualTypeArguments()[0];
                    if (type2 == providerClass
                        || type2 instanceof WildcardType
                        || type2 instanceof ParameterizedType
                           && ((ParameterizedType)type2).getRawType() == providerClass) {
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
                new ProviderInfoClassComparator((Comparator<Object>)theProviderComparator);
        }
        List<T> theProviders = (List<T>)listOfProviders;
        Comparator<? super T> theComparator = (Comparator<? super T>)theProviderComparator;
        theProviders.sort(theComparator);
    }

private final Map<MessageBodyReader<?>, List<MediaType>> readerMediaTypesMap = new IdentityHashMap<>();



    /**
     * This method attempts to optimize performance by checking a cache of known MessageBodyReaders's media types,
     * rather than calculating the media types for every provider on every request. If there is a cache miss, we
     * will look up the media types by calling JAXRSUtils.getProviderConsumeTypes(mbr).
     */
    static List<MediaType> getSortedProviderConsumeTypes(MessageBodyReader<?> mbr, Map<MessageBodyReader<?>, List<MediaType>> cache) {
        List<MediaType> mediaTypes = cache.get(mbr);
        if (mediaTypes == null) {
            mediaTypes = JAXRSUtils.getProviderConsumeTypes(mbr);
            // sort here before putting in the cache to avoid ConcurrentModificationException
            mediaTypes = JAXRSUtils.sortMediaTypes(mediaTypes, null);
            cache.put(mbr, mediaTypes);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getSortedProviderConsumeTypes - cache miss - caching " + mbr + " = " + mediaTypes);
            }
        }
        return mediaTypes;
    }

    private <T> boolean matchesReaderMediaTypes(ProviderInfo<MessageBodyReader<?>> pi,
                                                MediaType mediaType) {
        MessageBodyReader<?> ep = pi.getProvider();
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderConsumeTypes(ep);

        return JAXRSUtils.doMimeTypesIntersect(Collections.singletonList(mediaType), supportedMediaTypes);
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
            ep = pi.getProvider(); // now that CDI or EJB may have changed the provider impl //Liberty change
        }
        return ep.isReadable(type, genericType, annotations, mediaType);
    }

    //Liberty change start
    private final Map<MessageBodyWriter<?>, List<MediaType>> writerMediaTypesMap = new IdentityHashMap<>();

    /**
     * This method attempts to optimize performance by checking a cache of known MessageBodyWriter's media types,
     * rather than calculating the media types for every provider on every request. If there is a cache miss, we
     * will look up the media types by calling JAXRSUtils.getProviderProduceTypes(mbw).
     */
    static List<MediaType> getSortedProviderProduceTypes(MessageBodyWriter<?> mbw, Map<MessageBodyWriter<?>, List<MediaType>> cache) {
        List<MediaType> mediaTypes = cache.get(mbw);
        if (mediaTypes == null) {
            mediaTypes = JAXRSUtils.getProviderProduceTypes(mbw);
            // sort here before putting in the cache to avoid ConcurrentModificationException
            mediaTypes = JAXRSUtils.sortMediaTypes(mediaTypes, JAXRSUtils.MEDIA_TYPE_QS_PARAM);
            cache.put(mbw, mediaTypes);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getSortedProviderProduceTypes - cache miss - caching " + mbw + " = " + mediaTypes);
            }
        }
        return mediaTypes;
    }
    //Liberty change end

    private <T> boolean matchesWriterMediaTypes(ProviderInfo<MessageBodyWriter<?>> pi,
                                                MediaType mediaType) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "matchesWriterMediaTypes ",  new Object[]{pi, mediaType});
        }
        MessageBodyWriter<?> ep = pi.getProvider();
        List<MediaType> supportedMediaTypes = JAXRSUtils.getProviderProduceTypes(ep);

        return JAXRSUtils.doMimeTypesIntersect(Collections.singletonList(mediaType), supportedMediaTypes);
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
            ep = pi.getProvider(); // now that CDI or EJB may have changed the provider impl - Liberty change
        }
        return ep.isWriteable(type, genericType, annotations, mediaType);
    }

    List<ProviderInfo<MessageBodyReader<?>>> getMessageReaders() {
        //Liberty code change start
        return Collections.unmodifiableList(messageReaders.get());
        //Liberty code change end
    }

    List<ProviderInfo<MessageBodyWriter<?>>> getMessageWriters() {
        //Liberty code change start
        return Collections.unmodifiableList(messageWriters.get());
        //Liberty code change end
    }

    public List<ProviderInfo<ContextResolver<?>>> getContextResolvers() {
        //Liberty code change start
        return Collections.unmodifiableList(contextResolvers.get());
        //Liberty code change end
    }

    //Liberty change start
    public Iterable<ProviderInfo<ContextResolver<?>>> getContextResolversActual() {
        return contextResolvers;
    }
    //Liberty change end


    public void registerUserProvider(Object provider) {
        setUserProviders(Collections.singletonList(provider));
    }

    /**
     * Use for injection of entityProviders

     * @param userProviders the userProviders to set
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

            //Liberty code change start
            List<MediaType> types1 = getSortedProviderConsumeTypes(e1, cache);
            List<MediaType> types2 = getSortedProviderConsumeTypes(e2, cache);
            //Liberty code change end

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
            //Liberty code change start
            List<MediaType> types1 = getSortedProviderProduceTypes(e1, cache);
            List<MediaType> types2 = getSortedProviderProduceTypes(e2, cache);
            //Liberty code change end

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

    private static class ParamConverterProviderComparator implements Comparator<ProviderInfo<ParamConverterProvider>> {


        @Override
        public int compare(ProviderInfo<ParamConverterProvider> p1,
                           ProviderInfo<ParamConverterProvider> p2) {
            ParamConverterProvider e1 = p1.getOldProvider();
            ParamConverterProvider e2 = p2.getOldProvider();

            int result = compareClasses(e1, e2);
            if (result != 0) {
                return result;
            }

            return comparePriorityStatus(p1.getProvider().getClass(), p2.getProvider().getClass());
        }
    }

    public static int compareCustomStatus(ProviderInfo<?> p1, ProviderInfo<?> p2) {
        boolean custom1 = p1.isCustom();
        int result = Boolean.compare(p2.isCustom(), custom1);
        if (result == 0 && custom1) {
            result = Boolean.compare(p1.isBusGlobal(), p2.isBusGlobal());
        }
        return result;
    }


    static int comparePriorityStatus(Class<?> cl1, Class<?> cl2) {
        return Integer.compare(AnnotationUtils.getBindingPriority(cl1), AnnotationUtils.getBindingPriority(cl2));
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

    void clearProxies(Collection<?> ...lists) {
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
            Method m = provider.getClass().getMethod(mName, new Class[]{pClass});
            m.invoke(provider, new Object[]{pValue});
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
        names = names == null ? Collections.<String>emptySet() : names;

        MultivaluedMap<ProviderInfo<T>, String> map =
            new MetadataMap<>();
        for (Map.Entry<NameKey, ProviderInfo<T>> entry : boundFilters.entrySet()) {
            String entryName = entry.getKey().getName();
            ProviderInfo<T> provider = entry.getValue();
            if (entryName.equals(DEFAULT_FILTER_NAME_BINDING)) {
                map.put(provider, Collections.<String>emptyList());
            } else {
                if (provider instanceof FilterProviderInfo) {
                    FilterProviderInfo<?> fpi = (FilterProviderInfo<?>)provider;
                    if (fpi.isDynamic() && !names.containsAll(fpi.getNameBindings())) {
                        continue;
                    }
                }
                map.add(provider, entryName);
            }
        }
        List<ProviderInfo<T>> list = new LinkedList<>();
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
            Object provider = ((ProviderInfo<?>)o).getProvider();
            if (provider instanceof AbstractConfigurableProvider) {
                ((AbstractConfigurableProvider)provider).init(cris);
            }
        }
    }

    Set<Object> getReadersWriters() {
        Set<Object> set = new HashSet<>();
        //Liberty code change start
        set.addAll(messageReaders.get());
        set.addAll(messageWriters.get());
        //Liberty code change end
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

    public static class ProviderInfoClassComparator implements Comparator<ProviderInfo<?>> {
        Comparator<Object> comp;
        boolean defaultComp;
        public ProviderInfoClassComparator(Class<?> expectedCls) {
            this.comp = new ClassComparator(expectedCls);
            this.defaultComp = true;
        }
        public ProviderInfoClassComparator(Comparator<Object> comp) {
            this.comp = comp;
        }
        public int compare(ProviderInfo<?> p1, ProviderInfo<?> p2) {
            int result = comp.compare(p1.getProvider(), p2.getProvider());
            if (result == 0 && defaultComp) {
                result = compareCustomStatus(p1, p2);
            }
            return result;
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


    public static ProviderFactory getInstance(Message m) {
        Endpoint e = m.getExchange().getEndpoint();

        Message outM = m.getExchange().getOutMessage();
        boolean isClient = outM != null && MessageUtils.isRequestor(outM);
        String name = isClient ? CLIENT_FACTORY_NAME : SERVER_FACTORY_NAME;

        return (ProviderFactory)e.get(name);
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
        } else if (realClass2.isAssignableFrom(realClass1)) { //Liberty change
            return -1;
        }
        return 0; // Liberty change
    }


    private static Type[] getGenericInterfaces(Class<?> cls, Class<?> expectedClass) {
        return getGenericInterfaces(cls, expectedClass, Object.class);
    }
    //Liberty code change start
    //defect 178126
    //Add the result to cache before return
    private static Type[] getGenericInterfaces(Class<?> cls, Class<?> expectedClass,
                                               Class<?> commonBaseCls) {
        if (Object.class == cls) {
            return emptyType;
        }
        Type[] cachedTypes = getTypes(cls, expectedClass, commonBaseCls);
        if (cachedTypes != null)
            return cachedTypes;
        if (expectedClass != null) {
            Type genericSuperType = cls.getGenericSuperclass();
            if (genericSuperType instanceof ParameterizedType) {
                Class<?> actualType = InjectionUtils.getActualType(genericSuperType);
                if (actualType != null && actualType.isAssignableFrom(expectedClass)) {
                    Type[] tempTypes = new Type[] { genericSuperType };
                    putTypes(cls, expectedClass, commonBaseCls, tempTypes);
                    return tempTypes;
                } else if (commonBaseCls != null && commonBaseCls != Object.class 
                           && commonBaseCls.isAssignableFrom(expectedClass)
                           && commonBaseCls.isAssignableFrom(actualType)
                           || expectedClass.isAssignableFrom(actualType)) {
                    putTypes(cls, expectedClass, commonBaseCls, emptyType);
                    return emptyType;
                }
            }
        }
        Type[] types = cls.getGenericInterfaces();
        if (types.length > 0) {
            putTypes(cls, expectedClass, commonBaseCls, types);
            return types;
        }
        Type[] superGenericTypes = getGenericInterfaces(cls.getSuperclass(), expectedClass, commonBaseCls);
        putTypes(cls, expectedClass, commonBaseCls, superGenericTypes);
        return superGenericTypes;
    }

    //Liberty code change end

    protected static class AbstractPriorityComparator {

        private boolean ascending;

        protected AbstractPriorityComparator(boolean ascending) {
            this.ascending = ascending;
        }

        protected int compare(Integer b1Value, Integer b2Value) {
            int result = b1Value.compareTo(b2Value);
            return ascending ? result : result * -1;
        }

    }

    //Liberty code change start
    protected static class BindingPriorityComparator<T> extends AbstractPriorityComparator
        implements Comparator<ProviderInfo<T>> {
        private final Class<T> providerCls;

        public BindingPriorityComparator(Class<T> providerCls, boolean ascending) {
            super(ascending);
            this.providerCls = providerCls;
        }

        @Override
        public int compare(ProviderInfo<T> p1, ProviderInfo<T> p2) {
            return compare(getFilterPriority(p1, providerCls),
                           getFilterPriority(p2, providerCls));
        }

    }
    //Liberty code change end

    static class ContextResolverProxy<T> implements ContextResolver<T> {
        private List<ContextResolver<T>> candidates;
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
                                       + " can not be instantiated", ex);
        }
        Map<Class<?>, ThreadLocalProxy<?>> proxies =
            new LinkedHashMap<>();
        for (int i = 0; i < paramTypes.length; i++) {
            if (cArgs[i] instanceof ThreadLocalProxy) {
                @SuppressWarnings("unchecked")
                ThreadLocalProxy<Object> proxy = (ThreadLocalProxy<Object>)cArgs[i];
                proxies.put(paramTypes[i], proxy);
            }
        }
        boolean isApplication = Application.class.isAssignableFrom(c.getDeclaringClass());
        if (isApplication) {
            return new ApplicationInfo((Application)instance, proxies, theBus);
        }
        return new ProviderInfo<Object>(instance, proxies, theBus, checkContexts, custom);
    }

    private Message getResponseMessage(Message message) {
        Message responseMessage = message.getExchange().getInMessage();
        if (responseMessage == null) {
            responseMessage = message.getExchange().getInFaultMessage();
        }

        return responseMessage;
    }

    protected static class NameKey {
        private String name;
        private Integer priority;
        private Class<?> providerCls;
        private ProviderInfo<?> providerInfo;

        public NameKey(String name,
                       int priority,
                       Class<?> providerCls) {

            this(name, priority, providerCls, null);
        }

        public NameKey(String name,
                       int priority,
                       Class<?> providerCls,
                       ProviderInfo<?> provider) {

            this.name = name;
            this.priority = priority;
            this.providerCls = providerCls;
            this.providerInfo = provider;
        }

        public String getName() {
            return name;
        }

        public Integer getPriority() {
            return priority;
        }

        public ProviderInfo<?> getProviderInfo() {
            return providerInfo;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof NameKey)) {
                return false;
            }
            NameKey other = (NameKey)o;
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
                map.put(new NameKey(name, priority, p.getClass(), p), p);
            }
        }

    }

    protected static Set<String> getFilterNameBindings(ProviderInfo<?> p) {
        if (p instanceof FilterProviderInfo) {
            return ((FilterProviderInfo<?>)p).getNameBindings();
        } else {
            return getFilterNameBindings(p.getBus(), p.getProvider());
        }

    }
    protected static Set<String> getFilterNameBindings(Bus bus, Object provider) {
        Class<?> pClass = ClassHelper.getRealClass(bus, provider);
        Set<String> names = AnnotationUtils.getNameBindings(pClass.getAnnotations());
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

        private final Comparator<ProviderInfo<?>> comparator;

        public NameKeyComparator(boolean ascending) {
            this(null, ascending);
        }

        public NameKeyComparator(
            Comparator<ProviderInfo<?>> comparator, boolean ascending) {

            super(ascending);
            this.comparator = comparator;
        }

        @Override
        public int compare(NameKey key1, NameKey key2) {
            int result = compare(key1.getPriority(), key2.getPriority());
            if (result != 0) {
                return result;
            }

            if (comparator != null) {
                result = comparator.compare(
                    key1.getProviderInfo(), key2.getProviderInfo());

                if (result != 0) {
                    return result;
                }
            }

            return compare(key1.hashCode(), key2.hashCode());
        }

    }

    protected static class NameKeyMap<T> extends TreeMap<NameKey, T> {
        private static final long serialVersionUID = -4352258671270502204L;

        public NameKeyMap(
            Comparator<ProviderInfo<?>> comparator, boolean ascending) {

            super(new NameKeyComparator(comparator, ascending));
        }

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
                actualContracts = ((FilterProviderInfo<?>)provider).getSupportedContracts();
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
            new ArrayList<>(providers.length);
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
                Object newProviderInstance = beanCustomizer.onSingletonProviderInit(pi.getProvider(), beanCustomizerContexts.get(Integer.toString(beanCustomizer.hashCode())),
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

    @SuppressWarnings("unchecked")
    public void setProviderComparator(Comparator<?> providerComparator) {
        this.providerComparator = providerComparator;
        //Liberty code change start
        addAndSortReaders(null, true);
        addAndSortWriters(null, true);
        addAndSortParamConverters(null, true);
        //Liberty code change end
    }

    //Liberty code change start
    //defect 178126
    private static final ReferenceQueue<Class<?>> referenceQueue = new ReferenceQueue<>();

    private static void poll() {
        ClassWeakReference key;
        while ((key = (ClassWeakReference) referenceQueue.poll()) != null) {
            genericInterfacesCache.remove(key.getOwningKey());
        }
    }

    private static Type[] getTypes(Class<?> cls, Class<?> expectedCls, Class<?> commonBaseCls) {
        poll();
        return genericInterfacesCache.get(new ClassesKey(cls, expectedCls, commonBaseCls));
    }

    /**
     * Add a new expected class and Type[] to the Map of Maps. If there is only one expected class
     * for a given class, then we will use a Collections.singletonMap(). Otherwise we will convert to a
     * ConcurrentHashMap. This reduces overhead when there is only one expected Class.
     *
     * @param cls
     * @param expectedCls
     * @param types
     */
    private static void putTypes(Class<?> cls, Class<?> expectedCls, Class<?> commonBaseCls, Type[] types) {
        poll();
        genericInterfacesCache.put(new ClassesKey(referenceQueue, cls, expectedCls, commonBaseCls), types);
    }

    private static class ClassesKey {
        private final ClassWeakReference[] classes;
        private final int hash;

        ClassesKey(Class<?>... cl) {
            int length = cl.length;
            classes = new ClassWeakReference[length];
            int hashCode = 0;
            for (int i = 0; i < length; ++i) {
                if (cl[i] != null) {
                    classes[i] = new ClassWeakReference(cl[i], this);
                    hashCode += cl[i].hashCode();
                }
            }
            hash = hashCode;
        }

        ClassesKey(ReferenceQueue<Class<?>> referenceQueue, Class<?>... cl) {
            int length = cl.length;
            classes = new ClassWeakReference[length];
            int hashCode = 0;
            for (int i = 0; i < length; ++i) {
                if (cl[i] != null) {
                    classes[i] = new ClassWeakReference(cl[i], this, referenceQueue);
                    hashCode += cl[i].hashCode();
                }
            }
            hash = hashCode;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ClassesKey other = (ClassesKey) obj;
            if (!Arrays.equals(classes, other.classes))
                return false;
            return true;
        }
    }

    private static class ClassWeakReference extends WeakReference<Class<?>> {
        private final int hash;
        private final ClassesKey owningKey;

        ClassWeakReference(Class<?> referent, ClassesKey owningKey) {
            super(referent);
            this.owningKey = owningKey;
            hash = referent.hashCode();
        }

        ClassWeakReference(Class<?> referent, ClassesKey owningKey,
                           ReferenceQueue<Class<?>> referenceQueue) {
            super(referent, referenceQueue);
            this.owningKey = owningKey;
            hash = referent.hashCode();
        }

        ClassesKey getOwningKey() {
            return owningKey;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof ClassWeakReference) {
                return get() == ((ClassWeakReference) obj).get();
            }

            return false;
        }

        @Override
        public String toString() {
            Class<?> referent = get();
            return new StringBuilder("ClassWeakReference: ").append(referent == null ? null : referent.getName()).toString();
        }
    }
    //Liberty code change end
}
