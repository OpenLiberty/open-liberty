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

package org.apache.cxf.jaxrs.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ThreadLocalProxyCopyOnWriteArraySet;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public abstract class AbstractResourceInfo {
    private static final TraceComponent tc = Tr.register(AbstractResourceInfo.class);
    //Liberty code change start defect 169218
    //Property name of the set used to store the ThreadLocalProxy objects in bus
    public static final String PROXY_SET = "proxy-set";
    //Liberty code change end
    public static final String CONSTRUCTOR_PROXY_MAP = "jaxrs-constructor-proxy-map";
    //private static final Logger LOG = LogUtils.getL7dLogger(AbstractResourceInfo.class);
    private static final String FIELD_PROXY_MAP = "jaxrs-field-proxy-map";
    private static final String SETTER_PROXY_MAP = "jaxrs-setter-proxy-map";

    private static final Set<String> STANDARD_CONTEXT_CLASSES = new HashSet<String>();
    static {
        // JAX-RS 1.0-1.1
        STANDARD_CONTEXT_CLASSES.add(Application.class.getName());
        STANDARD_CONTEXT_CLASSES.add(UriInfo.class.getName());
        STANDARD_CONTEXT_CLASSES.add(HttpHeaders.class.getName());
        STANDARD_CONTEXT_CLASSES.add(Request.class.getName());
        STANDARD_CONTEXT_CLASSES.add(SecurityContext.class.getName());
        STANDARD_CONTEXT_CLASSES.add(Providers.class.getName());
        STANDARD_CONTEXT_CLASSES.add(ContextResolver.class.getName());
        STANDARD_CONTEXT_CLASSES.add("javax.servlet.http.HttpServletRequest");
        STANDARD_CONTEXT_CLASSES.add("javax.servlet.http.HttpServletResponse");
        STANDARD_CONTEXT_CLASSES.add("javax.servlet.ServletContext");
        // JAX-RS 2.0
        STANDARD_CONTEXT_CLASSES.add("javax.ws.rs.container.ResourceContext");
        STANDARD_CONTEXT_CLASSES.add("javax.ws.rs.container.ResourceInfo");
        STANDARD_CONTEXT_CLASSES.add("javax.ws.rs.core.Configuration");
    }

    protected boolean root;
    protected Class<?> resourceClass;
    protected Class<?> serviceClass;

    private Map<Class<?>, List<Field>> contextFields;
    private Map<Class<?>, Map<Class<?>, Method>> contextMethods;
    private final Bus bus;
    private boolean constructorProxiesAvailable;
    private boolean contextsAvailable;

    // Liberty code change start
    // There are many providers added during initialization.  This map maintains the providers we know about that are
    // processed during startup so that we don't have to determine if they have @Context annotations which can be quite expensive.
    private static final Map<String, ProviderContextInfo> CONTEXT_PROPS = new HashMap<>();

    static {
        // Using Strings instead of Class.class.getName() to avoid loading classes that aren't needed on the client / server
        // when only one of them is used in a particular environment.  Also some classes are not visible to this bundle.
        CONTEXT_PROPS.put("javax.ws.rs.client.ClientRequestFilter", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.client.ClientResponseFilter", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.container.ContainerRequestFilter", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.container.ContainerResponseFilter", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.container.DynamicFeature", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.core.Application", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.core.Feature", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.ext.ContextResolver", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.ext.ExceptionMapper", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.ext.MessageBodyWriter", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.ext.MessageBodyReader", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.ext.ParamConverterProvider", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.ext.ReaderInterceptor", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("javax.ws.rs.ext.WriterInterceptor", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.provider.BinaryDataProvider", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.provider.SourceProvider", new ProviderContextInfo(Collections.singleton("context"), null));
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.provider.DataSourceProvider", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.provider.FormEncodingProvider", new ProviderContextInfo(Collections.singleton("mc"), null));
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.provider.StringTextProvider", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.provider.PrimitiveTextProvider", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.provider.JAXBElementProvider", new ProviderContextInfo(null, Collections.singleton("setMessageContext")));
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.provider.AbstractJAXBProvider", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.provider.JAXBElementTypedProvider", new ProviderContextInfo(null, Collections.singleton("setMessageContext")));
        CONTEXT_PROPS.put("com.ibm.ws.jaxrs21.providers.json.JsonPProvider", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("com.ibm.ws.jaxrs21.providers.json.JsonBProvider", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("com.ibm.ws.jaxrs20.providers.multipart.IBMMultipartProvider", new ProviderContextInfo(Collections.singleton("mc"), null));
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.provider.MultipartProvider", new ProviderContextInfo(Collections.singleton("mc"), null));
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.nio.NioMessageBodyWriter", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.model.wadl.WadlGenerator", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("com.ibm.ws.jaxrs21.sse.LibertySseEventSinkContextProvider", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("org.apache.cxf.jaxrs.sse.SseContextProvider", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("com.ibm.ws.jaxrs20.providers.customexceptionmapper.CustomWebApplicationExceptionMapper", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("com.ibm.ws.jaxrs20.security.LibertyAuthFilter", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper", ProviderContextInfo.NO_PROCESSING_REQUIRED);
        CONTEXT_PROPS.put("org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptorFactory", ProviderContextInfo.NO_PROCESSING_REQUIRED);
    }

    private static class ProviderContextInfo {
        static final ProviderContextInfo NO_PROCESSING_REQUIRED = new ProviderContextInfo();

        // Indicates that this class AND its super classes and/or interface(s) do not have a @Context annotations
        // on fields or methods.
        final boolean processingRequired;

        // The field names in the associated class that have a @Context annotation.  Null indicates none.
        final Set<String> fieldNames;

        // The method names in the associated class that have a @Context annotation.  Null indicates none.
        final Set<String> methodNames;

        private ProviderContextInfo() {
            this.processingRequired = false;
            this.fieldNames = null;
            this.methodNames = null;
        }

        ProviderContextInfo(Set<String> fields, Set<String> methods) {
            processingRequired = true;
            // both fieldNames and methodNames may be null and processing is still required because a parent
            // class may have @Context annotations.
            fieldNames = fields;
            methodNames= methods;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString());
            sb.append(": ");
            sb.append(processingRequired).append(' ');
            sb.append(fieldNames).append(' ');
            sb.append(methodNames);
            return sb.toString();
        }
    }
    // Liberty code change end

    protected AbstractResourceInfo(Bus bus) {
        this.bus = bus;
    }

    protected AbstractResourceInfo(Class<?> resourceClass, Class<?> serviceClass,
                                   boolean isRoot, boolean checkContexts, Bus bus) {
        this(resourceClass, serviceClass, isRoot, checkContexts, null, bus, null);
    }

    protected AbstractResourceInfo(Class<?> resourceClass,
                                   Class<?> serviceClass,
                                   boolean isRoot,
                                   boolean checkContexts,
                                   Map<Class<?>, ThreadLocalProxy<?>> constructorProxies,
                                   Bus bus,
                                   Object provider) {
        this.bus = bus;
        this.serviceClass = serviceClass;
        this.resourceClass = resourceClass;
        root = isRoot;
        if (checkContexts && resourceClass != null) {
            findContexts(serviceClass, provider, constructorProxies);
        }
    }

    private void findContexts(Class<?> cls, Object provider,
                              Map<Class<?>, ThreadLocalProxy<?>> constructorProxies) {
        // Liberty code change start
        ProviderContextInfo contextInfo = CONTEXT_PROPS.get(cls.getName());
        if (contextInfo == null || contextInfo.processingRequired) {
            findContextFields(cls, provider, contextInfo);
            findContextSetterMethods(cls, provider, contextInfo);
        }
        // Liberty code change end

        if (constructorProxies != null) {
            Map<Class<?>, Map<Class<?>, ThreadLocalProxy<?>>> proxies = getConstructorProxyMap();
            proxies.put(serviceClass, constructorProxies);
            //Liberty code change start defect 169218
            //Add the constructorProxies to the set
            ThreadLocalProxyCopyOnWriteArraySet<ThreadLocalProxy<?>> proxySet = getProxySet();
            proxySet.addAll(constructorProxies.values());

            //Liberty code change end
            constructorProxiesAvailable = true;
        }

        contextsAvailable = contextFields != null && !contextFields.isEmpty()
                            || contextMethods != null && !contextMethods.isEmpty()
                            || constructorProxiesAvailable;
    }

    public boolean contextsAvailable() {
        return contextsAvailable;
    }

    public Bus getBus() {
        return bus;
    }

    public void setResourceClass(Class<?> rClass) {
        resourceClass = rClass;
        if (serviceClass.isInterface() && resourceClass != null && !resourceClass.isInterface()) {
            findContexts(resourceClass, null, null);
        }
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }

    // Liberty code change start
    private void findContextFields(final Class<?> cls, Object provider, ProviderContextInfo contextInfo) {
    // Liberty code change end
        if (cls == Object.class || cls == null) {
            return;
        }

        // Liberty code change start
        if (contextInfo == null || contextInfo.fieldNames != null) {
        // Liberty code change end
            for (Field f : ReflectionUtil.getDeclaredFields(cls)) {
                // Liberty code change start
                if (contextInfo != null && !contextInfo.fieldNames.contains(f.getName())) {
                    continue;
                }
                // Liberty code change end
                Class<?> fType = f.getType();
                for (Annotation a : f.getAnnotations()) {
                    //Liberty code change start defect 169218
                    //Add the FieldProxy to the set
                    if (a.annotationType() == Context.class) {
                        contextFields = addContextField(contextFields, f);
                        if (fType.isInterface()) {
                            checkContextClass(fType);

                            ThreadLocalProxy<?> proxy = getFieldThreadLocalProxy(f, provider);
                            if (!InjectionUtils.VALUE_CONTEXTS.contains(f.getType().getName())) {
                                if (addToMap(getFieldProxyMap(true), f, proxy)) {
                                    ThreadLocalProxyCopyOnWriteArraySet<ThreadLocalProxy<?>> proxySet = getProxySet();
                                    proxySet.add(proxy);
                                }
                            }
                        }
                        //Liberty code change end
                    }
                }
            }
        // Liberty code change start
        }
        Class<?> superClass = cls.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            ProviderContextInfo superContextInfo = CONTEXT_PROPS.get(superClass.getName());
            if (superContextInfo == null || superContextInfo.processingRequired) {
                findContextFields(superClass, provider, superContextInfo);
            }
        }
        // Liberty code change end
    }

    private static ThreadLocalProxy<?> getFieldThreadLocalProxy(Field f, Object provider) {
        if (provider != null) {
            Object proxy = null;
            synchronized (provider) {
                try {
                    proxy = InjectionUtils.extractFieldValue(f, provider);
                } catch (Throwable t) {
                    // continue
                }
                if (!(proxy instanceof ThreadLocalProxy)) {
                    proxy = InjectionUtils.createThreadLocalProxy(f.getType());
                    InjectionUtils.injectFieldValue(f, provider, proxy);
                }
            }
            return (ThreadLocalProxy<?>) proxy;
        }
        return InjectionUtils.createThreadLocalProxy(f.getType());
    }

    @FFDCIgnore(Throwable.class)
    private static ThreadLocalProxy<?> getMethodThreadLocalProxy(Method m, Object provider) {
        if (provider != null) {
            Object proxy = null;
            synchronized (provider) {
                try {
                    proxy = InjectionUtils.extractFromMethod(provider,
                                                             InjectionUtils.getGetterFromSetter(m),
                                                             false);
                } catch (Throwable t) {
                    // continue
                }
                if (!(proxy instanceof ThreadLocalProxy)) {
                    proxy = InjectionUtils.createThreadLocalProxy(m.getParameterTypes()[0]);
                    InjectionUtils.injectThroughMethod(provider, m, proxy);
                }
            }
            return (ThreadLocalProxy<?>) proxy;
        }
        return InjectionUtils.createThreadLocalProxy(m.getParameterTypes()[0]);
    }

    @SuppressWarnings("unchecked")
    private <T> Map<Class<?>, Map<T, ThreadLocalProxy<?>>> getProxyMap(String prop, boolean create) {
        // Avoid synchronizing on the bus for a ConcurrentHashMAp
        if (bus.getProperties() instanceof ConcurrentHashMap) {
            return (Map<Class<?>, Map<T, ThreadLocalProxy<?>>>) bus.getProperties().computeIfAbsent(prop, k ->
                new ConcurrentHashMap<Class<?>, Map<T, ThreadLocalProxy<?>>>(2)
            );
        }

        Object property = null;
        //synchronized (bus) {
        property = bus.getProperty(prop);
        if (property == null && create) {
            Map<Class<?>, Map<T, ThreadLocalProxy<?>>> map = new ConcurrentHashMap<>(2);
            bus.setProperty(prop, map);
            property = map;
        }
        //}
        return (Map<Class<?>, Map<T, ThreadLocalProxy<?>>>) property;
    }

    //Liberty code change start defect 169218
    //Create a CopyOnWriteArraySet to store the ThreadLocalProxy objects for convenience of clearance
    @SuppressWarnings("unchecked")
    private ThreadLocalProxyCopyOnWriteArraySet<ThreadLocalProxy<?>> getProxySet() {
        Object property = null;
        property = bus.getProperty(PROXY_SET);
        if (property == null) {
            ThreadLocalProxyCopyOnWriteArraySet<ThreadLocalProxy<?>> proxyMap = new ThreadLocalProxyCopyOnWriteArraySet<ThreadLocalProxy<?>>();

            bus.setProperty(PROXY_SET, proxyMap);
            property = proxyMap;
        }
        return (ThreadLocalProxyCopyOnWriteArraySet<ThreadLocalProxy<?>>) property;
    }

    //Liberty code change end

    public Map<Class<?>, ThreadLocalProxy<?>> getConstructorProxies() {
        if (constructorProxiesAvailable) {
            return getConstructorProxyMap().get(serviceClass);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<Class<?>, Map<Class<?>, ThreadLocalProxy<?>>> getConstructorProxyMap() {
        Object property = bus.getProperty(CONSTRUCTOR_PROXY_MAP);
        if (property == null) {
            Map<Class<?>, Map<Class<?>, ThreadLocalProxy<?>>> map
                = new ConcurrentHashMap<>(2);
            bus.setProperty(CONSTRUCTOR_PROXY_MAP, map);
            property = map;
        }
        return (Map<Class<?>, Map<Class<?>, ThreadLocalProxy<?>>>)property;
    }

    private Map<Class<?>, Map<Field, ThreadLocalProxy<?>>> getFieldProxyMap(boolean create) {
        return getProxyMap(FIELD_PROXY_MAP, create);
    }

    private Map<Class<?>, Map<Method, ThreadLocalProxy<?>>> getSetterProxyMap(boolean create) {
        return getProxyMap(SETTER_PROXY_MAP, create);
    }

    // Liberty code change start
    private void findContextSetterMethods(Class<?> cls, Object provider, ProviderContextInfo contextInfo) {
        if (contextInfo == null || contextInfo.methodNames != null) {
            // Liberty code change end

            for (Method m : cls.getMethods()) {
                // Liberty code change start
                String methodName = m.getName();
                if (contextInfo != null && !contextInfo.methodNames.contains(methodName)) {
                    continue;
                }
                if (!methodName.startsWith("set") || m.getParameterTypes().length != 1) {
                    continue;
                }
                // Liberty code change end
                for (Annotation a : m.getAnnotations()) {
                    if (a.annotationType() == Context.class) {
                        checkContextMethod(m, provider);
                        break;
                    }
                }
            }
        // Liberty code change start
        }
        // Liberty code change end
        Class<?>[] interfaces = cls.getInterfaces();
        for (Class<?> i : interfaces) {
            // Liberty code change start
            ProviderContextInfo iContextInfo = CONTEXT_PROPS.get(i.getName());
            if (iContextInfo == null || iContextInfo.processingRequired) {
                findContextSetterMethods(i, provider, iContextInfo);
            }
            // Liberty code change end
        }
        Class<?> superCls = cls.getSuperclass();
        if (superCls != null && superCls != Object.class) {
            // Liberty code change start
            ProviderContextInfo superContextInfo = CONTEXT_PROPS.get(superCls.getName());
            if (superContextInfo == null || superContextInfo.processingRequired) {
                findContextSetterMethods(superCls, provider, superContextInfo);
            }
            // Liberty code change end
        }
    }

    private void checkContextMethod(Method m, Object provider) {
        Class<?> type = m.getParameterTypes()[0];
        if (type.isInterface() || type == Application.class) {
            checkContextClass(type);
            addContextMethod(type, m, provider);
        }
    }

    private void checkContextClass(Class<?> type) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (!STANDARD_CONTEXT_CLASSES.contains(type.getName())) {
                Tr.debug(tc, "Injecting a custom context " + type.getName()
                             + ", ContextProvider is required for this type");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Map<Class<?>, Method> getContextMethods() {
        Map<Class<?>, Method> methods = contextMethods == null ? null : contextMethods.get(getServiceClass());
        return methods == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(methods);
    }

    private void addContextMethod(Class<?> contextClass, Method m, Object provider) {
        if (contextMethods == null) {
            contextMethods = new HashMap<>();
        }
        addToMap(contextMethods, contextClass, m);
        if (m.getParameterTypes()[0] != Application.class) {
            //Liberty code change start defect 169218
            //Add the MethodProxy to the set
            ThreadLocalProxy<?> proxy = getMethodThreadLocalProxy(m, provider);
            boolean added = addToMap(getSetterProxyMap(true), m, proxy);
            if (added) {
                ThreadLocalProxyCopyOnWriteArraySet<ThreadLocalProxy<?>> proxySet = getProxySet();
                proxySet.add(proxy);
            }
            //Liberty code change end
        }
    }

    public boolean isRoot() {
        return root;
    }

    public Class<?> getResourceClass() {
        return resourceClass;
    }

    public List<Field> getContextFields() {
        return getList(contextFields);
    }

    public ThreadLocalProxy<?> getContextFieldProxy(Field f) {
        return getProxy(getFieldProxyMap(true), f);
    }

    public ThreadLocalProxy<?> getContextSetterProxy(Method m) {
        return getProxy(getSetterProxyMap(true), m);
    }

    public abstract boolean isSingleton();

    @SuppressWarnings("rawtypes")
    public static void clearAllMaps() {
        Bus bus = BusFactory.getThreadDefaultBus(false);
        if (bus != null) {
            Object property = bus.getProperty(FIELD_PROXY_MAP);
            if (property != null) {
                ((Map) property).clear();
            }
            property = bus.getProperty(SETTER_PROXY_MAP);
            if (property != null) {
                ((Map) property).clear();
            }
            property = bus.getProperty(CONSTRUCTOR_PROXY_MAP);
            if (property != null) {
                ((Map) property).clear();
            }
        }
    }

    public void clearThreadLocalProxies() {
        clearProxies(getFieldProxyMap(false));
        clearProxies(getSetterProxyMap(false));
        clearProxies(getConstructorProxyMap());
    }

    private <T> void clearProxies(Map<Class<?>, Map<T, ThreadLocalProxy<?>>> tlps) {
        Map<T, ThreadLocalProxy<?>> proxies = tlps == null ? null : tlps.get(getServiceClass());
        if (proxies == null) {
            return;
        }
        for (ThreadLocalProxy<?> tlp : proxies.values()) {
            if (tlp != null) {
                tlp.remove();
            }
        }
    }

    private Map<Class<?>, List<Field>> addContextField(Map<Class<?>, List<Field>> theFields, Field f) {
        if (theFields == null) {
            theFields = new HashMap<>();
        }

        List<Field> fields = theFields.get(serviceClass);
        if (fields == null) {
            fields = new ArrayList<>();
            theFields.put(serviceClass, fields);
        }
        if (!fields.contains(f)) {
            fields.add(f);
        }
        return theFields;
    }

    //Liberty code change start defect 182967 - returns boolean instead of void
    private <T, V> boolean addToMap(Map<Class<?>, Map<T, V>> proxyMap,
                                    T f,
                                    V proxy) {
        Map<T, V> proxies = proxyMap.get(serviceClass);
        if (proxies == null) {
            proxies = new ConcurrentHashMap<>();
            proxyMap.put(serviceClass, proxies);
        }
        if (!proxies.containsKey(f)) {
            proxies.put(f, proxy);
            return true;
        }
        return false;
    }
    //Liberty code change end

    private List<Field> getList(Map<Class<?>, List<Field>> fields) {
        List<Field> ret = fields == null ? null : fields.get(getServiceClass());
        if (ret != null) {
            ret = Collections.unmodifiableList(ret);
        } else {
            ret = Collections.emptyList();
        }
        return ret;
    }

    private <T> ThreadLocalProxy<?> getProxy(Map<Class<?>, Map<T, ThreadLocalProxy<?>>> proxies,
                                             T key) {

        Map<?, ThreadLocalProxy<?>> theMap = proxies == null ? null : proxies.get(getServiceClass());
        ThreadLocalProxy<?> ret = null;
        if (theMap != null) {
            ret = theMap.get(key);
        }
        return ret;
    }
}
