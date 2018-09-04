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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Priorities;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.impl.ConfigurableImpl;
import org.apache.cxf.jaxrs.impl.FeatureContextImpl;
import org.apache.cxf.jaxrs.impl.RequestPreprocessor;
import org.apache.cxf.jaxrs.impl.ResourceInfoImpl;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.apache.cxf.jaxrs.impl.tl.ThreadLocalProxy;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.BeanParamInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.FilterProviderInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ThreadLocalProxyCopyOnWriteArraySet;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public final class ServerProviderFactory extends ProviderFactory {
    private static final TraceComponent tc = Tr.register(ServerProviderFactory.class);

    private static final String WADL_PROVIDER_NAME = "org.apache.cxf.jaxrs.model.wadl.WadlGenerator";
    private static final String MAKE_DEFAULT_WAE_LEAST_SPECIFIC = "default.wae.mapper.least.specific";
    private final List<ProviderInfo<ExceptionMapper<?>>> exceptionMappers =
                    new ArrayList<ProviderInfo<ExceptionMapper<?>>>(1);

    private final List<ProviderInfo<ContainerRequestFilter>> preMatchContainerRequestFilters =
                    new ArrayList<ProviderInfo<ContainerRequestFilter>>(1);
    private final Map<NameKey, ProviderInfo<ContainerRequestFilter>> postMatchContainerRequestFilters =
                    new NameKeyMap<ProviderInfo<ContainerRequestFilter>>(true);
    private final Map<NameKey, ProviderInfo<ContainerResponseFilter>> containerResponseFilters =
                    new NameKeyMap<ProviderInfo<ContainerResponseFilter>>(false);
    private RequestPreprocessor requestPreprocessor;
    private ApplicationInfo application;
    private final Set<DynamicFeature> dynamicFeatures = new LinkedHashSet<DynamicFeature>();

    private final Map<Class<?>, BeanParamInfo> beanParams = new ConcurrentHashMap<Class<?>, BeanParamInfo>();
    private ProviderInfo<ContainerRequestFilter> wadlGenerator;

    private ServerProviderFactory(Bus bus) {
        super(bus);
        wadlGenerator = createWadlGenerator(bus);
    }

    private static ProviderInfo<ContainerRequestFilter> createWadlGenerator(Bus bus) {
        Object provider = createProvider(WADL_PROVIDER_NAME, bus);
        if (provider == null) {
            return null;
        } else {
            return new ProviderInfo<ContainerRequestFilter>((ContainerRequestFilter) provider, bus, true);
        }
    }

    public static ServerProviderFactory getInstance() {
        return createInstance(null);
    }

    public static ServerProviderFactory createInstance(Bus bus) {
        if (bus == null) {
            bus = BusFactory.getThreadDefaultBus();
        }
        ServerProviderFactory factory = new ServerProviderFactory(bus);
        ProviderFactory.initFactory(factory);
        // Liberty difference - we use our own CustomExceptionMapper so we don't use the default WebApplicationExceptionMapper
        //factory.setProviders(false, false, new WebApplicationExceptionMapper());
        factory.setBusProviders();
        return factory;
    }

    public static ServerProviderFactory getInstance(Message m) {
        Endpoint e = m.getExchange().getEndpoint();
        return (ServerProviderFactory) e.get(SERVER_FACTORY_NAME);
    }

    public List<ProviderInfo<ContainerRequestFilter>> getPreMatchContainerRequestFilters() {
        return getContainerRequestFilters(preMatchContainerRequestFilters, true);
    }

    public List<ProviderInfo<ContainerRequestFilter>> getPostMatchContainerRequestFilters(Set<String> names) {
        return getBoundFilters(postMatchContainerRequestFilters, names);

    }

    private List<ProviderInfo<ContainerRequestFilter>> getContainerRequestFilters(
                                                                                  List<ProviderInfo<ContainerRequestFilter>> filters, boolean syncNeeded) {

        if (wadlGenerator == null) {
            return filters;
        }
        if (filters.size() == 0) {
            return Collections.singletonList(wadlGenerator);
        } else if (!syncNeeded) {
            filters.add(0, wadlGenerator);
            return filters;
        } else {
            synchronized (filters) {
                if (filters.get(0) != wadlGenerator) {
                    filters.add(0, wadlGenerator);
                }
            }
            return filters;
        }
    }

    public List<ProviderInfo<ContainerResponseFilter>> getContainerResponseFilters(Set<String> names) {
        return getBoundFilters(containerResponseFilters, names);
    }

    public void addBeanParamInfo(BeanParamInfo bpi) {
        beanParams.put(bpi.getResourceClass(), bpi);
        for (Method m : bpi.getResourceClass().getMethods()) {
            if (m.getAnnotation(BeanParam.class) != null) {
                BeanParamInfo methodBpi = new BeanParamInfo(m.getParameterTypes()[0], getBus());
                addBeanParamInfo(methodBpi);
            }
        }
        for (Field f : bpi.getResourceClass().getDeclaredFields()) {
            if (f.getAnnotation(BeanParam.class) != null) {
                BeanParamInfo fieldBpi = new BeanParamInfo(f.getType(), getBus());
                addBeanParamInfo(fieldBpi);
            }
        }
    }

    public BeanParamInfo getBeanParamInfo(Class<?> beanClass) {
        return beanParams.get(beanClass);
    }

    @SuppressWarnings("unchecked")
    public <T extends Throwable> ExceptionMapper<T> createExceptionMapper(Class<?> exceptionType,
                                                                          Message m) {
        List<ProviderInfo<ExceptionMapper<?>>> candidates = new LinkedList<ProviderInfo<ExceptionMapper<?>>>();
        for (ProviderInfo<ExceptionMapper<?>> em : exceptionMappers) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ExceptionMapper:  " + em.getProvider());
            }
            if (handleMapper(em, exceptionType, m, ExceptionMapper.class, Throwable.class, true)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding candidate mapper:  " + em.getProvider());
                }
                candidates.add(em);
            }
        }
        if (candidates.size() == 0) {
            return null;
        }
        boolean makeDefaultWaeLeastSpecific =
                        MessageUtils.getContextualBoolean(m, MAKE_DEFAULT_WAE_LEAST_SPECIFIC, false);
        Collections.sort(candidates, new ExceptionProviderInfoComparator(exceptionType,
                        makeDefaultWaeLeastSpecific));
        return (ExceptionMapper<T>) candidates.get(0).getProvider();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void setProviders(boolean custom, boolean busGlobal, Object... providers) {
        List<Object> allProviders = new LinkedList<Object>();
        for (Object p : providers) {
            if (p instanceof Feature) {
                FeatureContext featureContext = createServerFeatureContext();
                Feature feature = (Feature) p;
                injectApplicationIntoFeature(feature);
                feature.configure(featureContext);
                Configuration cfg = featureContext.getConfiguration();

                for (Object featureProvider : cfg.getInstances()) {
                    Map<Class<?>, Integer> contracts = cfg.getContracts(featureProvider.getClass());
                    if (contracts != null && !contracts.isEmpty()) {
                        Class<?> providerCls = ClassHelper.getRealClass(getBus(), featureProvider);

                        allProviders.add(new FilterProviderInfo<Object>(featureProvider.getClass(), providerCls, featureProvider,
                                                                        getBus(),
                                        getFilterNameBindings(getBus(),
                                                              featureProvider), false,
                                                                        contracts));
                    } else {
                        allProviders.add(featureProvider);
                    }
                }
            } else {
                allProviders.add(p);
            }
        }

        List<ProviderInfo<ContainerRequestFilter>> postMatchRequestFilters =
                        new LinkedList<ProviderInfo<ContainerRequestFilter>>();
        List<ProviderInfo<ContainerResponseFilter>> postMatchResponseFilters =
                        new LinkedList<ProviderInfo<ContainerResponseFilter>>();

        List<ProviderInfo<? extends Object>> theProviders =
            prepareProviders(custom, busGlobal, allProviders.toArray(), application);
        super.setCommonProviders(theProviders);
        for (ProviderInfo<? extends Object> provider : theProviders) {
            Class<?> providerCls = ClassHelper.getRealClass(getBus(), provider.getProvider());

            if (filterContractSupported(provider, providerCls, ContainerRequestFilter.class)) {
                addContainerRequestFilter(postMatchRequestFilters,
                                          (ProviderInfo<ContainerRequestFilter>) provider);
            }

            if (filterContractSupported(provider, providerCls, ContainerResponseFilter.class)) {
                postMatchResponseFilters.add((ProviderInfo<ContainerResponseFilter>) provider);
            }

            if (DynamicFeature.class.isAssignableFrom(providerCls)) {
                //TODO: review the possibility of DynamicFeatures needing to have Contexts injected
                Object feature = provider.getProvider();
                dynamicFeatures.add((DynamicFeature) feature);
            }

            if (filterContractSupported(provider, providerCls, ExceptionMapper.class)) {
                addProviderToList(exceptionMappers, provider);
            }

        }

        Collections.sort(preMatchContainerRequestFilters,
                         new BindingPriorityComparator(ContainerRequestFilter.class, true));
        mapInterceptorFilters(postMatchContainerRequestFilters, postMatchRequestFilters,
                              ContainerRequestFilter.class, true);
        mapInterceptorFilters(containerResponseFilters, postMatchResponseFilters,
                              ContainerResponseFilter.class, false);

        injectContextProxies(exceptionMappers,
                             postMatchContainerRequestFilters.values(), preMatchContainerRequestFilters,
                             containerResponseFilters.values());
    }

    protected void injectApplicationIntoFeature(Feature feature) {
        if (application != null) {
            AbstractResourceInfo info = new AbstractResourceInfo(feature.getClass(), ClassHelper.getRealClass(feature), true, true, getBus()) {
                @Override
                public boolean isSingleton() {
                    return false;
                }
            };
            Method contextMethod = info.getContextMethods().get(Application.class);
            if (contextMethod != null) {
                InjectionUtils.injectThroughMethod(feature, contextMethod, application.getProvider());
                return;
            }
            for (Field contextField : info.getContextFields()) {
                if (Application.class == contextField.getType()) {
                    InjectionUtils.injectContextField(info, contextField, feature, application.getProvider());
                    break;
                }
            }
        }
    }

    @Override
    protected void injectContextProxiesIntoProvider(ProviderInfo<?> pi) {
        injectContextProxiesIntoProvider(pi, application == null ? null : application.getProvider());
    }

    @Override
    protected void injectContextValues(ProviderInfo<?> pi, Message m) {
        if (m != null) {
            InjectionUtils.injectContexts(pi.getProvider(), pi, m);
            if (application != null && application.contextsAvailable()) {
                InjectionUtils.injectContexts(application.getProvider(), application, m);
            }
        }
    }

    private void addContainerRequestFilter(
                                           List<ProviderInfo<ContainerRequestFilter>> postMatchFilters,
                                           ProviderInfo<ContainerRequestFilter> p) {
        ContainerRequestFilter filter = p.getProvider();
        if (isWadlGenerator(filter.getClass())) {
            wadlGenerator = p;
        } else {
            if (isPrematching(filter.getClass())) {
                addProviderToList(preMatchContainerRequestFilters, p);
            } else {
                postMatchFilters.add(p);
            }
        }

    }

    private static boolean isWadlGenerator(Class<?> filterCls) {
        if (filterCls == null || filterCls == Object.class) {
            return false;
        }
        if (WADL_PROVIDER_NAME.equals(filterCls.getName())) {
            return true;
        } else {
            return isWadlGenerator(filterCls.getSuperclass());
        }
    }

    public RequestPreprocessor getRequestPreprocessor() {
        return requestPreprocessor;
    }

    public void setApplicationProvider(ApplicationInfo app) {
        application = app;
    }

    public ApplicationInfo getApplicationProvider() {
        return application;
    }

    public void setRequestPreprocessor(RequestPreprocessor rp) {
        this.requestPreprocessor = rp;
    }

    public void clearExceptionMapperProxies() {
        clearProxies(exceptionMappers);
    }

    @Override
    public void clearProviders() {
        super.clearProviders();
        exceptionMappers.clear();
        preMatchContainerRequestFilters.clear();
        postMatchContainerRequestFilters.clear();
        containerResponseFilters.clear();
    }

    @Override
    public void clearThreadLocalProxies() {
        //Liberty code change start defect 169218
        //Clear saved ThreadLocalProxy object set.
        //All the created ThreadLocalProxy objects are added to the set in AbstractResourceInfo class
        @SuppressWarnings("unchecked")
        ThreadLocalProxyCopyOnWriteArraySet<ThreadLocalProxy<?>> proxySet = (ThreadLocalProxyCopyOnWriteArraySet<ThreadLocalProxy<?>>) getBus().getProperty(AbstractResourceInfo.PROXY_SET);
        if (proxySet != null) {
            Object[] proxies = proxySet.toArray();
            for (Object proxy : proxies) {
                ((ThreadLocalProxy<?>) proxy).remove();
            }
        }
        //super.clearThreadLocalProxies may clean same TheadLocalProxy object more than one times in one call.
        //Liberty code change end
//        if (application != null) {
//            application.clearThreadLocalProxies();
//        }
//        super.clearThreadLocalProxies();
    }

    public void applyDynamicFeatures(List<ClassResourceInfo> list) {
        if (dynamicFeatures.size() > 0) {
            for (ClassResourceInfo cri : list) {
                doApplyDynamicFeatures(cri);
            }
        }
    }

    @Override
    public Configuration getConfiguration(Message m) {
        return new ServerConfigurationImpl();
    }

    private void doApplyDynamicFeatures(ClassResourceInfo cri) {
        Set<OperationResourceInfo> oris = cri.getMethodDispatcher().getOperationResourceInfos();
        for (OperationResourceInfo ori : oris) {
            String nameBinding = DEFAULT_FILTER_NAME_BINDING
                + ori.getClassResourceInfo().getServiceClass().getName()
                + "."
                + ori.getMethodToInvoke().toString();
            for (DynamicFeature feature : dynamicFeatures) {
                FeatureContext featureContext = createServerFeatureContext();
                feature.configure(new ResourceInfoImpl(ori), featureContext);
                Configuration cfg = featureContext.getConfiguration();
                for (Object provider : cfg.getInstances()) {
                    Map<Class<?>, Integer> contracts = cfg.getContracts(provider.getClass());
                    if (contracts != null && !contracts.isEmpty()) {
                        Class<?> providerCls = ClassHelper.getRealClass(getBus(), provider);
                        registerUserProvider(new FilterProviderInfo<Object>(provider.getClass(), providerCls, provider,
                            getBus(),
                                        Collections.singleton(nameBinding),
                            true,
                            contracts));
                        ori.addNameBindings(Collections.singletonList(nameBinding));
                    }
                }
            }
        }
        Collection<ClassResourceInfo> subs = cri.getSubResources();
        for (ClassResourceInfo sub : subs) {
            if (sub != cri) {
                doApplyDynamicFeatures(sub);
            }
        }
    }

    private FeatureContext createServerFeatureContext() {
        final FeatureContextImpl featureContext = new FeatureContextImpl();
        final ServerConfigurableFactory factory = getBus().getExtension(ServerConfigurableFactory.class);
        final Configurable<FeatureContext> configImpl = (factory == null) ? new ServerFeatureContextConfigurable(featureContext) : factory.create(featureContext);
        featureContext.setConfigurable(configImpl);

        if (application != null) {
            Map<String, Object> appProps = application.getProvider().getProperties();
            for (Map.Entry<String, Object> entry : appProps.entrySet()) {
                configImpl.property(entry.getKey(), entry.getValue());
            }
        }
        return featureContext;
    }

    protected static boolean isPrematching(Class<?> filterCls) {
        return AnnotationUtils.getClassAnnotation(filterCls, PreMatching.class) != null;
    }

    private static class ServerFeatureContextConfigurable extends ConfigurableImpl<FeatureContext> {
        protected ServerFeatureContextConfigurable(FeatureContext mc) {
            super(mc, RuntimeType.SERVER, ServerConfigurableFactory.SERVER_FILTER_INTERCEPTOR_CLASSES);
        }
    }

    public static void clearThreadLocalProxies(Message message) {
        clearThreadLocalProxies(ServerProviderFactory.getInstance(message), message);
    }

    public static void clearThreadLocalProxies(ServerProviderFactory factory, Message message) {
        factory.clearThreadLocalProxies();
        ClassResourceInfo cri =
                        (ClassResourceInfo) message.getExchange().get(JAXRSUtils.ROOT_RESOURCE_CLASS);
        if (cri != null) {
            cri.clearThreadLocalProxies();
        }
    }

    public static void releaseRequestState(Message message) {
        releaseRequestState(ServerProviderFactory.getInstance(message), message);
    }

    public static void releaseRequestState(ServerProviderFactory factory, Message message) {
        Object rootInstance = message.getExchange().remove(JAXRSUtils.ROOT_INSTANCE);
        if (rootInstance != null) {
            Object rootProvider = message.getExchange().remove(JAXRSUtils.ROOT_PROVIDER);
            if (rootProvider != null) {
                try {
                    ((ResourceProvider) rootProvider).releaseInstance(message, rootInstance);
                } catch (Throwable tex) {
                    // ignore
                }
            }
        }

        clearThreadLocalProxies(factory, message);
    }

    private class ServerConfigurationImpl implements Configuration {
        ServerConfigurationImpl() {

        }

        @Override
        public Set<Class<?>> getClasses() {
            return application != null ? application.getProvider().getClasses()
                            : Collections.<Class<?>> emptySet();
        }

        @Override
        public Set<Object> getInstances() {
            return application != null ? application.getProvider().getSingletons()
                            : Collections.emptySet();
        }

        @Override
        public boolean isEnabled(Feature f) {
            return dynamicFeatures.contains(f);
        }

        @Override
        public boolean isEnabled(Class<? extends Feature> featureCls) {
            for (DynamicFeature f : dynamicFeatures) {
                if (featureCls.isAssignableFrom(f.getClass())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isRegistered(Object o) {
            return isRegistered(preMatchContainerRequestFilters, o)
                   || isRegistered(postMatchContainerRequestFilters.values(), o)
                   || isRegistered(containerResponseFilters.values(), o)
                   || isRegistered(readerInterceptors.values(), o)
                   || isRegistered(writerInterceptors.values(), o);
        }

        @Override
        public boolean isRegistered(Class<?> cls) {
            return isRegistered(preMatchContainerRequestFilters, cls)
                   || isRegistered(postMatchContainerRequestFilters.values(), cls)
                   || isRegistered(containerResponseFilters.values(), cls)
                   || isRegistered(readerInterceptors.values(), cls)
                   || isRegistered(writerInterceptors.values(), cls);
        }

        @Override
        public Map<Class<?>, Integer> getContracts(Class<?> cls) {
            Map<Class<?>, Integer> map = new HashMap<Class<?>, Integer>();
            if (isRegistered(cls)) {
                if (ContainerRequestFilter.class.isAssignableFrom(cls)) {
                    boolean isPreMatch = cls.getAnnotation(PreMatching.class) != null;
                    map.put(ContainerRequestFilter.class,
                            getPriority(isPreMatch ? preMatchContainerRequestFilters
                                            : postMatchContainerRequestFilters.values(), cls, ContainerRequestFilter.class));
                }
                if (ContainerResponseFilter.class.isAssignableFrom(cls)) {
                    map.put(ContainerResponseFilter.class,
                            getPriority(containerResponseFilters.values(), cls, ContainerResponseFilter.class));
                }
                if (WriterInterceptor.class.isAssignableFrom(cls)) {
                    map.put(WriterInterceptor.class,
                            getPriority(writerInterceptors.values(), cls, WriterInterceptor.class));
                }
                if (ReaderInterceptor.class.isAssignableFrom(cls)) {
                    map.put(ReaderInterceptor.class,
                            getPriority(readerInterceptors.values(), cls, ReaderInterceptor.class));
                }
            }
            return map;
        }

        @Override
        public Map<String, Object> getProperties() {
            return application != null ? application.getProperties()
                            : Collections.<String, Object> emptyMap();
        }

        @Override
        public Object getProperty(String name) {
            return getProperties().get(name);
        }

        @Override
        public Collection<String> getPropertyNames() {
            return getProperties().keySet();
        }

        @Override
        public RuntimeType getRuntimeType() {
            return RuntimeType.SERVER;
        }

        private boolean isRegistered(Collection<?> list, Object o) {
            Collection<ProviderInfo<?>> list2 = CastUtils.cast(list);
            for (ProviderInfo<?> pi : list2) {
                if (pi.getProvider() == o) {
                    return true;
                }
            }
            return false;
        }

        private boolean isRegistered(Collection<?> list, Class<?> cls) {
            Collection<ProviderInfo<?>> list2 = CastUtils.cast(list);
            for (ProviderInfo<?> p : list2) {
                Class<?> pClass = ClassHelper.getRealClass(p.getBus(), p.getProvider());
                if (cls.isAssignableFrom(pClass)) {
                    return true;
                }
            }
            return false;
        }

        private Integer getPriority(Collection<?> list, Class<?> cls, Class<?> filterClass) {
            Collection<ProviderInfo<?>> list2 = CastUtils.cast(list);
            for (ProviderInfo<?> p : list2) {
                if (p instanceof FilterProviderInfo) {
                    Class<?> pClass = ClassHelper.getRealClass(p.getBus(), p.getProvider());
                    if (cls.isAssignableFrom(pClass)) {
                        return ((FilterProviderInfo<?>)p).getPriority(filterClass);
                    }
                }
            }
            return Priorities.USER;
        }
    }

    public static class ExceptionProviderInfoComparator extends ProviderInfoClassComparator {
        private final boolean makeDefaultWaeLeastSpecific;

        public ExceptionProviderInfoComparator(Class<?> expectedCls, boolean makeDefaultWaeLeastSpecific) {
            super(expectedCls);
            this.makeDefaultWaeLeastSpecific = makeDefaultWaeLeastSpecific;
        }

        @Override
        public int compare(ProviderInfo<?> p1, ProviderInfo<?> p2) {
            // ExceptionMapper classes may be turned to proxy classes due to dependency
            // injection so use the "OldProvider" if it exists.
            if (makeDefaultWaeLeastSpecific) {
                if (p1.getOldProvider() instanceof WebApplicationExceptionMapper
                    && !p1.isCustom()) {
                    return 1;
                } else if (p2.getOldProvider() instanceof WebApplicationExceptionMapper
                           && !p2.isCustom()) {
                    return -1;
                }
            }
            int result = comp.compare(p1.getOldProvider(), p2.getOldProvider());
            if (result == 0 && defaultComp) {
                result = compareCustomStatus(p1, p2);
            }
            return result;

        }
    }

}
