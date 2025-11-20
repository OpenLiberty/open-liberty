/*
 * Copyright The RestEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.core.providerfactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.RxInvoker;
import jakarta.ws.rs.client.RxInvokerProvider;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Configurable;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.RuntimeDelegate;
import jakarta.ws.rs.ext.WriterInterceptor;

import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.core.MediaTypeMap;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.se.ResteasySeConfiguration;
import org.jboss.resteasy.core.se.ResteasySeInstance;
import org.jboss.resteasy.plugins.delegates.CacheControlDelegate;
import org.jboss.resteasy.plugins.delegates.CookieHeaderDelegate;
import org.jboss.resteasy.plugins.delegates.DateDelegate;
import org.jboss.resteasy.plugins.delegates.EntityTagDelegate;
import org.jboss.resteasy.plugins.delegates.LinkDelegate;
import org.jboss.resteasy.plugins.delegates.LinkHeaderDelegate;
import org.jboss.resteasy.plugins.delegates.LocaleDelegate;
import org.jboss.resteasy.plugins.delegates.MediaTypeHeaderDelegate;
import org.jboss.resteasy.plugins.delegates.NewCookieHeaderDelegate;
import org.jboss.resteasy.plugins.delegates.UriHeaderDelegate;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.AsyncClientResponseProvider;
import org.jboss.resteasy.spi.AsyncResponseProvider;
import org.jboss.resteasy.spi.AsyncStreamProvider;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.ContextInjector;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.spi.HeaderValueProcessor;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.LinkHeader;
import org.jboss.resteasy.spi.PriorityServiceLoader;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.StringParameterUnmarshaller;
import org.jboss.resteasy.spi.concurrent.ThreadContext;
import org.jboss.resteasy.spi.concurrent.ThreadContexts;
import org.jboss.resteasy.spi.config.Options;
import org.jboss.resteasy.spi.interception.JaxrsInterceptorRegistry;
import org.jboss.resteasy.spi.metadata.ResourceBuilder;
import org.jboss.resteasy.spi.metadata.ResourceClassProcessor;
import org.jboss.resteasy.spi.statistics.StatisticsController;
import org.jboss.resteasy.spi.util.PickConstructor;
import org.jboss.resteasy.spi.util.Types;
import org.jboss.resteasy.statistics.StatisticsControllerImpl;
import org.jboss.resteasy.tracing.RESTEasyTracingLogger;
import org.jboss.resteasy.util.FeatureContextDelegate;
import org.jboss.resteasy.util.snapshot.SnapshotMap;
import org.jboss.resteasy.util.snapshot.SnapshotSet;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ResteasyProviderFactoryImpl extends ResteasyProviderFactory
        implements Providers, HeaderValueProcessor, Configurable<ResteasyProviderFactory>, Configuration {
    protected ClientHelper clientHelper;
    protected ServerHelper serverHelper;

    protected SnapshotSet<Class<?>> providerClasses;
    protected SnapshotSet<Object> providerInstances;
    protected SnapshotMap<Class<?>, Map<Class<?>, Integer>> classContracts;
    protected SnapshotMap<String, Object> properties;

    protected SnapshotMap<Class<?>, HeaderDelegate> headerDelegates;
    protected SnapshotMap<Type, ContextInjector> contextInjectors;
    protected SnapshotMap<Type, ContextInjector> asyncContextInjectors;
    protected SnapshotMap<Class<?>, Class<? extends StringParameterUnmarshaller>> stringParameterUnmarshallers;
    protected SnapshotSet<Feature> enabledFeatures;

    protected boolean attachedContextResolvers;
    protected Map<Class<?>, MediaTypeMap<SortedKey<ContextResolver>>> contextResolvers;
    protected boolean attachedParamConverterProviders;
    protected Set<ExtSortedKey<ParamConverterProvider>> sortedParamConverterProviders;

    protected Set<Class<?>> alreadyEstablishedNullHeaderDelegate = ConcurrentHashMap.newKeySet();
    protected boolean builtinsRegistered = false;
    protected boolean registerBuiltins = true;
    protected InjectorFactory injectorFactory;
    protected ResourceBuilder resourceBuilder;
    protected boolean initialized = false;
    protected boolean lockSnapshots;
    protected StatisticsControllerImpl statisticsController = new StatisticsControllerImpl();

    private final boolean defaultExceptionManagerEnabled;

    public ResteasyProviderFactoryImpl() {
        this(Options.ENABLE_DEFAULT_EXCEPTION_MAPPER.getValue());
    }

    /**
     * Creates a new factory.
     *
     * @param defaultExceptionManagerEnabled {@code true} if the default exception manager should be enabled
     */
    public ResteasyProviderFactoryImpl(final boolean defaultExceptionManagerEnabled) {
        // NOTE!!! It is important to put all initialization into initialize() as ThreadLocalResteasyProviderFactory
        // subclasses and delegates to this class.
        initialize();
        this.defaultExceptionManagerEnabled = defaultExceptionManagerEnabled;
    }

    /**
     * Create factory optimized for a Client
     *
     * @param runtimeType
     */
    public ResteasyProviderFactoryImpl(final RuntimeType runtimeType) {
        if (runtimeType != RuntimeType.CLIENT)
            throw new IllegalStateException();
        this.clientHelper = new ClientHelper(this);
        this.serverHelper = NOOPServerHelper.SINGLETON;
        initializeCommon(null, true, false);
        // don't know when client will be made shareable so just do it here
        lockSnapshots();
        this.defaultExceptionManagerEnabled = Options.ENABLE_DEFAULT_EXCEPTION_MAPPER.getValue();
    }

    /**
     *
     * @param runtimeType
     */
    /**
     * Create factory optimized for a specific RuntimeType that is a copy of its parent (shallow copy if possible)
     *
     * @param runtimeType
     * @param parent
     */
    public ResteasyProviderFactoryImpl(final RuntimeType runtimeType, final ResteasyProviderFactory parent) {
        if (runtimeType == RuntimeType.CLIENT) {
            ResteasyProviderFactoryImpl impl = (ResteasyProviderFactoryImpl) parent;
            this.clientHelper = new ClientHelper(this, impl.clientHelper);
            this.serverHelper = NOOPServerHelper.SINGLETON;
            this.lockSnapshots = true;
            initializeCommon(impl, true, false);
            // don't know when client will be made shareable so just do it here
            lockSnapshots();
        } else {
            ResteasyProviderFactoryImpl parentImpl = (ResteasyProviderFactoryImpl) parent;
            clientHelper = NOOPClientHelper.SINGLETON;
            serverHelper = new ServerHelper(this, parentImpl.serverHelper);
            initializeCommon(parentImpl, false, true);
        }
        this.defaultExceptionManagerEnabled = Options.ENABLE_DEFAULT_EXCEPTION_MAPPER.getValue();
    }

    protected void registerBuiltin() {
        RegisterBuiltin.register(this);
    }

    protected void initializeCommon(ResteasyProviderFactoryImpl parent, boolean lockSnapshots, boolean snapFirst) {
        properties = parent == null ? new SnapshotMap<>(lockSnapshots)
                : new SnapshotMap<>(parent.properties, true, lockSnapshots, snapFirst);

        providerClasses = parent == null ? new SnapshotSet<>(lockSnapshots)
                : new SnapshotSet<>(parent.providerClasses, true, lockSnapshots, snapFirst);
        providerInstances = parent == null ? new SnapshotSet<>(lockSnapshots)
                : new SnapshotSet<>(parent.providerInstances, true, lockSnapshots, snapFirst);
        classContracts = parent == null ? new SnapshotMap<>(lockSnapshots)
                : new SnapshotMap<>(parent.classContracts, true, lockSnapshots, snapFirst);

        enabledFeatures = parent == null ? new SnapshotSet<>(lockSnapshots)
                : new SnapshotSet<>(parent.enabledFeatures, true, lockSnapshots, snapFirst);
        if (parent != null) {
            if (snapFirst) {
                // resourcemethod invoker factory
                // we don't want to copy these
                attachedParamConverterProviders = true;
                sortedParamConverterProviders = parent.sortedParamConverterProviders;
                attachedContextResolvers = true;
                contextResolvers = parent.contextResolvers;
            } else {
                contextResolvers = new ConcurrentHashMap<>();
                for (Entry<Class<?>, MediaTypeMap<SortedKey<ContextResolver>>> entry : parent.contextResolvers.entrySet()) {
                    contextResolvers.put(entry.getKey(), new MediaTypeMap<>(entry.getValue()));
                }
                sortedParamConverterProviders = Collections
                        .synchronizedSortedSet(new TreeSet<>(parent.sortedParamConverterProviders));
            }
        } else {
            contextResolvers = new ConcurrentHashMap<>();
            sortedParamConverterProviders = Collections.synchronizedSortedSet(new TreeSet<>());
        }

        resourceBuilder = new ResourceBuilder();
        headerDelegates = parent == null ? new SnapshotMap<>(lockSnapshots)
                : new SnapshotMap<>(parent.getHeaderDelegates(), true, lockSnapshots, snapFirst);
        if (parent == null) {
            // parent should always have these delegates
            addHeaderDelegateIfAbsent(MediaType.class, MediaTypeHeaderDelegate.INSTANCE);
            addHeaderDelegateIfAbsent(NewCookie.class, NewCookieHeaderDelegate.INSTANCE);
            addHeaderDelegateIfAbsent(Cookie.class, CookieHeaderDelegate.INSTANCE);
            addHeaderDelegateIfAbsent(URI.class, UriHeaderDelegate.INSTANCE);
            addHeaderDelegateIfAbsent(EntityTag.class, EntityTagDelegate.INSTANCE);
            addHeaderDelegateIfAbsent(CacheControl.class, CacheControlDelegate.INSTANCE);
            addHeaderDelegateIfAbsent(Locale.class, LocaleDelegate.INSTANCE);
            addHeaderDelegateIfAbsent(LinkHeader.class, LinkHeaderDelegate.INSTANCE);
            addHeaderDelegateIfAbsent(jakarta.ws.rs.core.Link.class, LinkDelegate.INSTANCE);
            addHeaderDelegateIfAbsent(Date.class, DateDelegate.INSTANCE);
        }

        builtinsRegistered = false;
        registerBuiltins = true;

        stringParameterUnmarshallers = parent == null ? new SnapshotMap<>(lockSnapshots)
                : new SnapshotMap<>(parent.stringParameterUnmarshallers, true, lockSnapshots, snapFirst);
        contextInjectors = parent == null ? new SnapshotMap<>(lockSnapshots)
                : new SnapshotMap<>(parent.contextInjectors, true, lockSnapshots, snapFirst);
        asyncContextInjectors = parent == null ? new SnapshotMap<>(lockSnapshots)
                : new SnapshotMap<>(parent.asyncContextInjectors, true, lockSnapshots, snapFirst);

        injectorFactory = parent == null ? InjectorFactoryImpl.INSTANCE : parent.getInjectorFactory();
        initialized = true;
    }

    private void copyResolversIfNeeded() {
        if (!attachedContextResolvers)
            return;
        Map<Class<?>, MediaTypeMap<SortedKey<ContextResolver>>> newResolvers = new ConcurrentHashMap<>();
        for (Entry<Class<?>, MediaTypeMap<SortedKey<ContextResolver>>> entry : contextResolvers.entrySet()) {
            newResolvers.put(entry.getKey(), new MediaTypeMap<>(entry.getValue()));
        }
        contextResolvers = newResolvers;
        attachedContextResolvers = false;
    }

    protected void initialize() {
        initializeCommon(null, false, false);
        clientHelper = new ClientHelper(this);
        serverHelper = new ServerHelper(this);
    }

    public void lockSnapshots() {
        lockSnapshots = true;
        if (providerClasses != null)
            providerClasses.lockSnapshots();
        if (providerInstances != null)
            providerInstances.lockSnapshots();
        if (classContracts != null)
            classContracts.lockSnapshots();
        if (properties != null)
            properties.lockSnapshots();
        if (headerDelegates != null)
            headerDelegates.lockSnapshots();
        if (contextInjectors != null)
            contextInjectors.lockSnapshots();
        if (asyncContextInjectors != null)
            asyncContextInjectors.lockSnapshots();
        if (stringParameterUnmarshallers != null)
            stringParameterUnmarshallers.lockSnapshots();
        if (enabledFeatures != null)
            enabledFeatures.lockSnapshots();
        clientHelper.lockSnapshots();
        serverHelper.lockSnapshots();
    }

    public Set<DynamicFeature> getServerDynamicFeatures() {
        return serverHelper.getDynamicFeatures();
    }

    public Set<DynamicFeature> getClientDynamicFeatures() {
        return clientHelper.getDynamicFeatures();
    }

    protected MediaTypeMap<SortedKey<MessageBodyReader>> getServerMessageBodyReaders() {
        return serverHelper.getMessageBodyReaders();
    }

    protected MediaTypeMap<SortedKey<MessageBodyWriter>> getServerMessageBodyWriters() {
        return serverHelper.getMessageBodyWriters();
    }

    protected MediaTypeMap<SortedKey<MessageBodyReader>> getClientMessageBodyReaders() {
        return clientHelper.getMessageBodyReaders();
    }

    protected MediaTypeMap<SortedKey<MessageBodyWriter>> getClientMessageBodyWriters() {
        return clientHelper.getMessageBodyWriters();
    }

    private Map<Class<?>, SortedKey<ExceptionMapper>> getSortedExceptionMappers() {
        return serverHelper.getExceptionMappers();
    }

    public Map<Class<?>, AsyncResponseProvider> getAsyncResponseProviders() {
        return serverHelper.getAsyncResponseProviders();
    }

    public Map<Class<?>, AsyncStreamProvider> getAsyncStreamProviders() {
        return serverHelper.getAsyncStreamProviders();
    }

    public Map<Class<?>, AsyncClientResponseProvider> getAsyncClientResponseProviders() {
        return clientHelper.getAsyncClientResponseProviders();
    }

    public Map<Type, ContextInjector> getContextInjectors() {
        return contextInjectors;
    }

    public Map<Type, ContextInjector> getAsyncContextInjectors() {
        return asyncContextInjectors;
    }

    protected Map<Class<?>, MediaTypeMap<SortedKey<ContextResolver>>> getContextResolvers() {
        return contextResolvers;
    }

    protected Set<ExtSortedKey<ParamConverterProvider>> getSortedParamConverterProviders() {
        return sortedParamConverterProviders;
    }

    protected Map<Class<?>, Class<? extends StringParameterUnmarshaller>> getStringParameterUnmarshallers() {
        return stringParameterUnmarshallers;
    }

    /**
     * Gets provide classes.
     *
     * @return set of provider classes
     */
    public Set<Class<?>> getProviderClasses() {
        return Collections.unmodifiableSet(providerClasses);
    }

    /**
     * Gets provider instances.
     *
     * @return set of provider instances
     */
    public Set<Object> getProviderInstances() {
        return Collections.unmodifiableSet(providerInstances);
    }

    public Map<Class<?>, Map<Class<?>, Integer>> getClassContracts() {
        return classContracts;
    }

    public <T> T getContextData(Class<T> rawType, Type genericType, Annotation[] annotations, boolean unwrapAsync) {
        T ret = (T) ResteasyContext.getContextDataMap().get(rawType);
        if (ret != null)
            return ret;
        ContextInjector contextInjector = getContextInjectors().get(genericType);
        boolean async = false;
        if (contextInjector == null && unwrapAsync) {
            contextInjector = getAsyncContextInjectors().get(Types.boxPrimitives(genericType));
            async = true;
        }

        if (contextInjector != null) {
            ret = (T) contextInjector.resolve(rawType, genericType, annotations);
            if (async && ret != null) {
                Type wrappedType = Types.getActualTypeArgumentsOfAnInterface(contextInjector.getClass(),
                        ContextInjector.class)[0];
                Class<?> rawWrappedType = Types.getRawType(wrappedType);
                AsyncResponseProvider converter = getAsyncResponseProvider(rawWrappedType);
                // OK this is plain lying
                ret = (T) converter.toCompletionStage(ret);
            }
        }
        return ret;
    }

    public boolean isRegisterBuiltins() {
        return registerBuiltins;
    }

    public void setRegisterBuiltins(boolean registerBuiltins) {
        this.registerBuiltins = registerBuiltins;
    }

    public InjectorFactory getInjectorFactory() {
        return injectorFactory;
    }

    public void setInjectorFactory(InjectorFactory injectorFactory) {
        this.injectorFactory = injectorFactory;
    }

    public JaxrsInterceptorRegistry<ReaderInterceptor> getServerReaderInterceptorRegistry() {
        return serverHelper.getReaderInterceptorRegistry();
    }

    public JaxrsInterceptorRegistry<WriterInterceptor> getServerWriterInterceptorRegistry() {
        return serverHelper.getWriterInterceptorRegistry();
    }

    public JaxrsInterceptorRegistry<ContainerRequestFilter> getContainerRequestFilterRegistry() {
        return serverHelper.getRequestFilters();
    }

    public JaxrsInterceptorRegistry<ContainerResponseFilter> getContainerResponseFilterRegistry() {
        return serverHelper.getResponseFilters();
    }

    public JaxrsInterceptorRegistry<ReaderInterceptor> getClientReaderInterceptorRegistry() {
        return clientHelper.getReaderInterceptorRegistry();
    }

    public JaxrsInterceptorRegistry<WriterInterceptor> getClientWriterInterceptorRegistry() {
        return clientHelper.getWriterInterceptorRegistry();
    }

    public JaxrsInterceptorRegistry<ClientRequestFilter> getClientRequestFilterRegistry() {
        return clientHelper.getRequestFilters();
    }

    public JaxrsInterceptorRegistry<ClientResponseFilter> getClientResponseFilters() {
        return clientHelper.getResponseFilters();
    }

    public boolean isBuiltinsRegistered() {
        return builtinsRegistered;
    }

    public void setBuiltinsRegistered(boolean builtinsRegistered) {
        this.builtinsRegistered = builtinsRegistered;
    }

    public UriBuilder createUriBuilder() {
        return Utils.createUriBuilder();
    }

    public Response.ResponseBuilder createResponseBuilder() {
        return Utils.createResponseBuilder();
    }

    public Variant.VariantListBuilder createVariantListBuilder() {
        return Utils.createVariantListBuilder();
    }

    public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> tClass) {
        if (tClass == null)
            throw new IllegalArgumentException(Messages.MESSAGES.tClassParameterNull());
        return Utils.createHeaderDelegate(headerDelegates, alreadyEstablishedNullHeaderDelegate, tClass);
    }

    protected Map<Class<?>, HeaderDelegate> getHeaderDelegates() {
        return headerDelegates;
    }

    public void addHeaderDelegate(Class clazz, HeaderDelegate header) {
        headerDelegates.put(clazz, header);
    }

    protected void addHeaderDelegateIfAbsent(Class clazz, HeaderDelegate header) {
        if (!headerDelegates.containsKey(clazz)) {
            addHeaderDelegate(clazz, header);
        }
    }

    @Deprecated
    public <T> MessageBodyReader<T> getServerMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        MediaTypeMap<SortedKey<MessageBodyReader>> availableReaders = getServerMessageBodyReaders();
        return resolveMessageBodyReader(type, genericType, annotations, mediaType, availableReaders);
    }

    public <T> MessageBodyReader<T> getServerMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, RESTEasyTracingLogger tracingLogger) {
        MediaTypeMap<SortedKey<MessageBodyReader>> availableReaders = getServerMessageBodyReaders();
        return resolveMessageBodyReader(type, genericType, annotations, mediaType, availableReaders, tracingLogger);
    }

    /**
     * Always returns server MBRs.
     *
     * @param type        the class of the object that is to be read.
     * @param genericType the type of object to be produced. E.g. if the
     *                    message body is to be converted into a method parameter, this will be
     *                    the formal type of the method parameter as returned by
     *                    {@code Class.getGenericParameterTypes}.
     * @param annotations an array of the annotations on the declaration of the
     *                    artifact that will be initialized with the produced instance. E.g. if
     *                    the message body is to be converted into a method parameter, this will
     *                    be the annotations on that parameter returned by
     *                    {@code Class.getParameterAnnotations}.
     * @param mediaType   the media type of the data that will be read.
     * @param <T>         type
     * @return message reader
     */
    public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        MediaTypeMap<SortedKey<MessageBodyReader>> availableReaders = getServerMessageBodyReaders();
        MessageBodyReader<T> reader = resolveMessageBodyReader(type, genericType, annotations, mediaType,
                availableReaders);
        if (reader != null)
            LogMessages.LOGGER.debugf("MessageBodyReader: %s", reader.getClass().getName());
        return reader;
    }

    public <T> MessageBodyReader<T> getClientMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        MediaTypeMap<SortedKey<MessageBodyReader>> availableReaders = getClientMessageBodyReaders();
        if (availableReaders == null)
            return null;
        return resolveMessageBodyReader(type, genericType, annotations, mediaType, availableReaders);
    }

    @Deprecated
    private <T> MessageBodyReader<T> resolveMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MediaTypeMap<SortedKey<MessageBodyReader>> availableReaders) {
        if (availableReaders == null)
            return null;
        List<SortedKey<MessageBodyReader>> readers = availableReaders.getPossible(mediaType, type);

        //logger.info("******** getMessageBodyReader *******");
        for (SortedKey<MessageBodyReader> reader : readers) {
            //logger.info("     matching reader: " + reader.getClass().getName());
            if (reader.getObj().isReadable(type, genericType, annotations, mediaType)) {
                LogMessages.LOGGER.debugf("MessageBodyReader: %s", reader.getClass().getName());
                return (MessageBodyReader<T>) reader.getObj();
            }
        }
        return null;
    }

    private <T> MessageBodyReader<T> resolveMessageBodyReader(Class<T> type, Type genericType,
            Annotation[] annotations, MediaType mediaType, MediaTypeMap<SortedKey<MessageBodyReader>> availableReaders,
            RESTEasyTracingLogger tracingLogger) {
        if (availableReaders == null)
            return null;
        List<SortedKey<MessageBodyReader>> readers = availableReaders.getPossible(mediaType, type);

        if (tracingLogger.isLogEnabled("MBR_FIND")) {
            tracingLogger.log("MBR_FIND", type.getName(),
                    (genericType instanceof Class ? ((Class) genericType).getName() : genericType), mediaType,
                    java.util.Arrays.toString(annotations));
        }

        MessageBodyReader<T> result = null;

        Iterator<SortedKey<MessageBodyReader>> iterator = readers.iterator();

        while (iterator.hasNext()) {
            final SortedKey<MessageBodyReader> reader = iterator.next();

            if (reader.getObj().isReadable(type, genericType, annotations, mediaType)) {
                LogMessages.LOGGER.debugf("MessageBodyReader: %s", reader.getClass().getName());
                result = (MessageBodyReader<T>) reader.getObj();
                tracingLogger.log("MBR_SELECTED", reader);
                break;
            }
            tracingLogger.log("MBR_NOT_READABLE", result);
        }

        if (tracingLogger.isLogEnabled("MBR_SKIPPED")) {
            while (iterator.hasNext()) {
                final SortedKey<MessageBodyReader> reader = iterator.next();
                tracingLogger.log("MBR_SKIPPED", reader.getObj());
            }
        }
        return result;
    }

    private void addContextInjector(ContextInjector provider, Class providerClass) {
        Type[] typeArgs = Types.getActualTypeArgumentsOfAnInterface(providerClass, ContextInjector.class);
        Utils.injectProperties(this, provider.getClass(), provider);

        contextInjectors.put(typeArgs[0], provider);

        if (!Objects.equals(typeArgs[0], typeArgs[1])) {
            asyncContextInjectors.put(typeArgs[1], provider);
        }
    }

    private void addContextResolver(ContextResolver provider, int priority, Class providerClass, boolean builtin) {
        // RESTEASY-1725
        if (providerClass.getName().contains("$$Lambda$")) {
            throw new RuntimeException(Messages.MESSAGES.registeringContextResolverAsLambda());
        }
        copyResolversIfNeeded();
        Type[] typeParameters = Types.getActualTypeArgumentsOfAnInterface(providerClass, ContextResolver.class);
        Type typeParameter;
        if (typeParameters.length == 0) {
            // This may be an indication that this is a CDI client proxy. In some cases, the ContextResolver interface
            // may appear as implemented by the proxy, in other words providerClass.getGenericTypes() may return the
            // ContextResolver interface. However, this is not a ParameterizedType, therefore the generic type cannot
            // be resolved on the client proxy. For this reason, we will check the super class as it should be the
            // non-proxied type we'd expect and we can get the generic type for the ContextResolver from there.
            final Class<?> superType = providerClass.getSuperclass();
            if (superType != null) {
                typeParameters = Types.getActualTypeArgumentsOfAnInterface(superType, ContextResolver.class);
            } else {
                throw Messages.MESSAGES.couldNotDetermineGenericType(providerClass.getName(), ContextResolver.class.getName());
            }
            if (typeParameters.length == 0) {
                throw Messages.MESSAGES.couldNotDetermineGenericType(providerClass.getName(), ContextResolver.class.getName());
            }
        }
        typeParameter = typeParameters[0];
        Utils.injectProperties(this, providerClass, provider);
        Class<?> parameterClass = Types.getRawType(typeParameter);
        MediaTypeMap<SortedKey<ContextResolver>> resolvers = contextResolvers.get(parameterClass);
        if (resolvers == null) {
            resolvers = new MediaTypeMap<SortedKey<ContextResolver>>();
            contextResolvers.put(parameterClass, resolvers);
        }
        Produces produces = provider.getClass().getAnnotation(Produces.class);
        SortedKey<ContextResolver> key = new SortedKey<ContextResolver>(ContextResolver.class, provider, providerClass,
                priority, builtin);
        if (produces != null) {
            for (String produce : produces.value()) {
                resolvers.add(produce, key);
            }
        } else {
            resolvers.add(MediaType.WILDCARD, key);
        }
    }

    public void addStringParameterUnmarshaller(Class<? extends StringParameterUnmarshaller> provider) {
        Type[] intfs = provider.getGenericInterfaces();
        for (Type type : intfs) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                if (pt.getRawType().equals(StringParameterUnmarshaller.class)) {
                    Class<?> aClass = Types.getRawType(pt.getActualTypeArguments()[0]);
                    stringParameterUnmarshallers.put(aClass, provider);
                }
            }
        }
    }

    public List<ContextResolver> getContextResolvers(final Class<?> clazz, MediaType type) {
        if (getContextResolvers() == null)
            return null;
        MediaTypeMap<SortedKey<ContextResolver>> resolvers = getContextResolvers().get(clazz);
        if (resolvers == null)
            return null;
        List<ContextResolver> rtn = new ArrayList<ContextResolver>();
        List<SortedKey<ContextResolver>> list = resolvers.getPossible(type);
        list.forEach(resolver -> rtn.add(resolver.getObj()));
        return rtn;
    }

    public ParamConverter getParamConverter(Class clazz, Type genericType, Annotation[] annotations) {
        try {
            ResteasyContext.pushContext(ResteasyProviderFactory.class, this); // For MultiValuedParamConverterProvider
            for (SortedKey<ParamConverterProvider> provider : getSortedParamConverterProviders()) {
                ParamConverter converter = provider.getObj().getConverter(clazz, genericType, annotations);
                if (converter != null)
                    return converter;
            }
            return null;
        } finally {
            ResteasyContext.popContextData(ResteasyProviderFactory.class);
        }
    }

    public <T> StringParameterUnmarshaller<T> createStringParameterUnmarshaller(Class<T> clazz) {
        if (getStringParameterUnmarshallers() == null || getStringParameterUnmarshallers().isEmpty())
            return null;
        Class<? extends StringParameterUnmarshaller> un = getStringParameterUnmarshallers().get(clazz);
        if (un == null)
            return null;
        StringParameterUnmarshaller<T> provider = injectedInstance(un);
        return provider;

    }

    public void registerProvider(Class provider) {
        registerProvider(provider, false);
    }

    /**
     * Convert an object to a string. First try StringConverter then, object.ToString()
     *
     * @param object      object
     * @param clazz       class
     * @param genericType generic type
     * @param annotations array of annotation
     * @return string representation
     */
    public String toString(Object object, Class clazz, Type genericType, Annotation[] annotations) {
        if (object instanceof String)
            return (String) object;
        ParamConverter paramConverter = getParamConverter(clazz, genericType, annotations);
        if (paramConverter != null) {
            return paramConverter.toString(object);
        }
        return object.toString();
    }

    @Override
    public String toHeaderString(Object object) {
        if (object == null)
            return "";
        if (object instanceof String)
            return (String) object;
        Class<?> aClass = object.getClass();
        ParamConverter paramConverter = getParamConverter(aClass, null, null);
        if (paramConverter != null) {
            return paramConverter.toString(object);
        }
        HeaderDelegate delegate = getHeaderDelegate(aClass);
        if (delegate != null)
            return delegate.toString(object);
        else
            return object.toString();

    }

    /**
     * Checks to see if RuntimeDelegate is a ResteasyProviderFactory
     * If it is, then use that, otherwise use this.
     *
     * @param aClass class of the header
     * @return header delegate
     */
    public HeaderDelegate getHeaderDelegate(Class<?> aClass) {
        HeaderDelegate delegate = null;
        // Stupid idiotic TCK calls RuntimeDelegate.setInstance()
        if (RuntimeDelegate.getInstance() instanceof ResteasyProviderFactory) {
            delegate = createHeaderDelegate(aClass);
        } else {
            delegate = RuntimeDelegate.getInstance().createHeaderDelegate(aClass);
        }
        return delegate;
    }

    /**
     * Register a @Provider class. Can be a MessageBodyReader/Writer or ExceptionMapper.
     *
     * @param provider  provider class
     * @param isBuiltin built-in
     */
    public void registerProvider(Class provider, boolean isBuiltin) {
        registerProvider(provider, null, isBuiltin, null);
    }

    public void registerProvider(Class provider, Integer priorityOverride, boolean isBuiltin,
            Map<Class<?>, Integer> contracts) {
        Map<Class<?>, Map<Class<?>, Integer>> classContracts = getClassContracts();
        if (classContracts.containsKey(provider)) {
            LogMessages.LOGGER.providerClassAlreadyRegistered(provider.getName());
            return;
        }
        final Map<Class<?>, Integer> newContracts = new HashMap<>();
        processProviderContracts(provider, priorityOverride, isBuiltin, contracts, newContracts);
        providerClasses.add(provider);
        classContracts.put(provider, newContracts);
    }

    public Set<Class<?>> getMutableProviderClasses() {
        return providerClasses;
    }

    private void processProviderContracts(Class provider, Integer priorityOverride, boolean isBuiltin,
            Map<Class<?>, Integer> contracts, Map<Class<?>, Integer> newContracts) {
        clientHelper.processProviderContracts(provider, priorityOverride, isBuiltin, contracts, newContracts);
        serverHelper.processProviderContracts(provider, priorityOverride, isBuiltin, contracts, newContracts);

        if (Utils.isA(provider, ParamConverterProvider.class, contracts)) {
            int priority = Utils.getPriority(priorityOverride, contracts, ParamConverterProvider.class, provider);
            addParameterConverterProvider(provider, isBuiltin, priority);
            newContracts.put(ParamConverterProvider.class, priority);
        }
        if (Utils.isA(provider, ContextResolver.class, contracts)) {
            try {
                int priority = Utils.getPriority(priorityOverride, contracts, ContextResolver.class, provider);
                addContextResolver(provider, isBuiltin, priority);
                newContracts.put(ContextResolver.class, priority);
            } catch (Exception e) {
                throw new RuntimeException(Messages.MESSAGES.unableToInstantiateContextResolver(), e);
            }
        }
        if (Utils.isA(provider, ContextInjector.class, contracts)) {
            try {
                addContextInjector(provider);
                int priority = Utils.getPriority(priorityOverride, contracts, ContextInjector.class, provider);
                newContracts.put(ContextInjector.class, priority);
            } catch (Exception e) {
                throw new RuntimeException(Messages.MESSAGES.unableToInstantiateContextInjector(), e);
            }
        }
        if (Utils.isA(provider, StringParameterUnmarshaller.class, contracts)) {
            addStringParameterUnmarshaller(provider);
            int priority = Utils.getPriority(priorityOverride, contracts, StringParameterUnmarshaller.class, provider);
            newContracts.put(StringParameterUnmarshaller.class, priority);
        }
        if (Utils.isA(provider, InjectorFactory.class, contracts)) {
            try {
                addInjectorFactory(provider);
                newContracts.put(InjectorFactory.class, 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (Utils.isA(provider, Feature.class, contracts)) {
            ConstrainedTo constrainedTo = (ConstrainedTo) provider.getAnnotation(ConstrainedTo.class);
            int priority = Utils.getPriority(priorityOverride, contracts, Feature.class, provider);
            if (constrainedTo == null || constrainedTo.value() == getRuntimeType()) {
                addFeature(provider);
            }
            newContracts.put(Feature.class, priority);
        }
        if (Utils.isA(provider, ResourceClassProcessor.class, contracts)) {
            int priority = Utils.getPriority(priorityOverride, contracts, ResourceClassProcessor.class, provider);
            addResourceClassProcessor(provider, priority);
            newContracts.put(ResourceClassProcessor.class, priority);
        }
        if (Utils.isA(provider, HeaderDelegate.class, contracts)) {
            addHeaderDelegate(provider);
        }
        if (Utils.isA(provider, ThreadContext.class, contracts)) {
            ResteasyContext.computeIfAbsent(ThreadContexts.class, ThreadContexts::new)
                    .add(createProviderInstance((Class<? extends ThreadContext>) provider));
        }
    }

    public void addHeaderDelegate(Class provider) {
        Type[] headerTypes = Types.getActualTypeArgumentsOfAnInterface(provider, HeaderDelegate.class);
        if (headerTypes.length == 0) {
            LogMessages.LOGGER.cannotRegisterheaderDelegate(provider);
        } else {
            Class<?> headerClass = Types.getRawType(headerTypes[0]);
            addHeaderDelegate(provider, headerClass);
        }
    }

    public ClientHelper getClientHelper() {
        return clientHelper;
    }

    public ServerHelper getServerHelper() {
        return serverHelper;
    }

    public void addHeaderDelegate(Class<? extends HeaderDelegate> provider, Class<?> headerClass) {
        HeaderDelegate<?> delegate = createProviderInstance(provider);
        addHeaderDelegate(headerClass, delegate);
    }

    public void addFeature(Class<? extends Feature> provider) {
        Feature feature = injectedInstance(provider);
        if (feature.configure(new FeatureContextDelegate(this))) {
            enabledFeatures.add(feature);
        }
    }

    public void addInjectorFactory(Class provider) throws InstantiationException, IllegalAccessException {
        this.injectorFactory = (InjectorFactory) provider.newInstance();
    }

    public void addContextInjector(Class provider) {
        addContextInjector(createProviderInstance((Class<? extends ContextInjector>) provider), provider);
    }

    public void addContextResolver(Class provider, boolean isBuiltin, int priority) {
        addContextResolver(createProviderInstance((Class<? extends ContextResolver>) provider), priority, provider, isBuiltin);
    }

    public void addParameterConverterProvider(Class provider, boolean isBuiltin, int priority) {
        ParamConverterProvider paramConverterProvider = (ParamConverterProvider) injectedInstance(provider);
        injectProperties(provider);
        copyParamConvertsIfNeeded();
        sortedParamConverterProviders
                .add(new ExtSortedKey<>(null, paramConverterProvider, provider, priority, isBuiltin));
    }

    private void copyParamConvertsIfNeeded() {
        if (attachedParamConverterProviders) {
            sortedParamConverterProviders = Collections.synchronizedSortedSet(new TreeSet<>(sortedParamConverterProviders));
            attachedParamConverterProviders = false;
        }
    }

    /**
     * Register a @Provider object. Can be a MessageBodyReader/Writer or ExceptionMapper.
     *
     * @param provider provider instance
     */
    public void registerProviderInstance(Object provider) {
        registerProviderInstance(provider, null, null, false);
    }

    public void registerProviderInstance(Object provider, Map<Class<?>, Integer> contracts, Integer priorityOverride,
            boolean builtIn) {
        Class<?> providerClass = provider.getClass();
        Map<Class<?>, Map<Class<?>, Integer>> classContracts = getClassContracts();
        if (classContracts.containsKey(providerClass)) {
            LogMessages.LOGGER.providerInstanceAlreadyRegistered(providerClass.getName());
            return;
        }
        Map<Class<?>, Integer> newContracts = new HashMap<>();
        processProviderInstanceContracts(provider, contracts, priorityOverride, builtIn, newContracts);
        providerInstances.add(provider);
        classContracts.put(providerClass, newContracts);
    }

    private void processProviderInstanceContracts(Object provider, Map<Class<?>, Integer> contracts,
            Integer priorityOverride, boolean builtIn, Map<Class<?>, Integer> newContracts) {
        clientHelper.processProviderInstanceContracts(provider, contracts, priorityOverride, builtIn, newContracts);
        serverHelper.processProviderInstanceContracts(provider, contracts, priorityOverride, builtIn, newContracts);

        if (Utils.isA(provider, ParamConverterProvider.class, contracts)) {
            injectProperties(provider);
            int priority = Utils.getPriority(priorityOverride, contracts, ParamConverterProvider.class, provider.getClass());
            copyParamConvertsIfNeeded();
            sortedParamConverterProviders.add(
                    new ExtSortedKey<>(null, (ParamConverterProvider) provider, provider.getClass(), priority, builtIn));
            newContracts.put(ParamConverterProvider.class, priority);
        }
        if (Utils.isA(provider, ContextResolver.class, contracts)) {
            try {
                int priority = Utils.getPriority(priorityOverride, contracts, ContextResolver.class, provider.getClass());
                addContextResolver((ContextResolver) provider, priority, provider.getClass(), false);
                newContracts.put(ContextResolver.class, priority);
            } catch (Exception e) {
                throw new RuntimeException(Messages.MESSAGES.unableToInstantiateContextResolver(), e);
            }
        }
        if (Utils.isA(provider, ContextInjector.class, contracts)) {
            try {
                addContextInjector((ContextInjector) provider, provider.getClass());
                int priority = Utils.getPriority(priorityOverride, contracts, ContextInjector.class, provider.getClass());
                newContracts.put(ContextInjector.class, priority);
            } catch (Exception e) {
                throw new RuntimeException(Messages.MESSAGES.unableToInstantiateContextInjector(), e);
            }
        }
        if (Utils.isA(provider, InjectorFactory.class, contracts)) {
            this.injectorFactory = (InjectorFactory) provider;
            newContracts.put(InjectorFactory.class, 0);
        }
        if (Utils.isA(provider, Feature.class, contracts)) {
            Feature feature = (Feature) provider;
            Utils.injectProperties(this, provider.getClass(), provider);
            ConstrainedTo constrainedTo = (ConstrainedTo) provider.getClass().getAnnotation(ConstrainedTo.class);
            if (constrainedTo == null || constrainedTo.value() == getRuntimeType()) {
                if (feature.configure(new FeatureContextDelegate(this))) {
                    enabledFeatures.add(feature);
                }
            }
            int priority = Utils.getPriority(priorityOverride, contracts, Feature.class, provider.getClass());
            newContracts.put(Feature.class, priority);

        }
        if (Utils.isA(provider, ResourceClassProcessor.class, contracts)) {
            int priority = Utils.getPriority(priorityOverride, contracts, ResourceClassProcessor.class, provider.getClass());
            addResourceClassProcessor((ResourceClassProcessor) provider, priority);
            newContracts.put(ResourceClassProcessor.class, priority);
        }
        if (Utils.isA(provider, HeaderDelegate.class, contracts)) {
            Type[] headerTypes = Types.getActualTypeArgumentsOfAnInterface(provider.getClass(), HeaderDelegate.class);
            if (headerTypes.length == 0) {
                LogMessages.LOGGER.cannotRegisterheaderDelegate(provider.getClass());
            } else {
                Class<?> headerClass = Types.getRawType(headerTypes[0]);
                addHeaderDelegate(headerClass, (HeaderDelegate) provider);
            }
        }
        if (Utils.isA(provider, ThreadContext.class, contracts)) {
            ResteasyContext.computeIfAbsent(ThreadContexts.class, ThreadContexts::new)
                    .add((ThreadContext<?>) provider);
        }
    }

    @Override
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
        Class exceptionType = type;
        SortedKey<ExceptionMapper> mapper = null;
        Map<Class<?>, SortedKey<ExceptionMapper>> mappers = getSortedExceptionMappers();
        final boolean defaultExceptionManagerEnabled = isDefaultExceptionManagerEnabled();
        if (mappers == null && defaultExceptionManagerEnabled) {
            return (ExceptionMapper<T>) DefaultExceptionMapper.INSTANCE;
        }
        while (mapper == null) {
            if (exceptionType == null)
                break;
            mapper = mappers.get(exceptionType);
            if (mapper == null)
                exceptionType = exceptionType.getSuperclass();
        }
        return mapper != null ? mapper.getObj()
                : (defaultExceptionManagerEnabled ? (ExceptionMapper<T>) DefaultExceptionMapper.INSTANCE : null);
    }

    public <T extends Throwable> ExceptionMapper<T> getExceptionMapperForClass(Class<T> type) {
        Map<Class<?>, SortedKey<ExceptionMapper>> mappers = getSortedExceptionMappers();
        if (mappers == null)
            return null;
        SortedKey<ExceptionMapper> mapper = mappers.get(type);
        return mapper != null ? mapper.getObj() : null;
    }

    //   @Override
    public <T> AsyncResponseProvider<T> getAsyncResponseProvider(Class<T> type) {
        Class asyncType = type;
        AsyncResponseProvider<T> mapper = null;
        while (mapper == null) {
            if (asyncType == null)
                break;
            mapper = findProvider(asyncType, getAsyncResponseProviders());
            if (mapper == null)
                asyncType = asyncType.getSuperclass();
        }
        return mapper;
    }

    public <T> AsyncClientResponseProvider<T> getAsyncClientResponseProvider(Class<T> type) {
        Class asyncType = type;
        AsyncClientResponseProvider<T> mapper = null;
        while (mapper == null) {
            if (asyncType == null)
                break;
            mapper = findProvider(asyncType, getAsyncClientResponseProviders());
            if (mapper == null)
                asyncType = asyncType.getSuperclass();
        }
        return mapper;
    }

    // @Override
    public <T> AsyncStreamProvider<T> getAsyncStreamProvider(Class<T> type) {
        Class asyncType = type;
        AsyncStreamProvider<T> mapper = null;
        while (mapper == null) {
            if (asyncType == null)
                break;
            mapper = findProvider(asyncType, getAsyncStreamProviders());
            if (mapper == null)
                asyncType = asyncType.getSuperclass();
        }
        return mapper;
    }

    public MediaType getConcreteMediaTypeFromMessageBodyWriters(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        List<SortedKey<MessageBodyWriter>> writers = getServerMessageBodyWriters().getPossible(mediaType, type);
        for (SortedKey<MessageBodyWriter> writer : writers) {
            if (writer.getObj().isWriteable(type, genericType, annotations, mediaType)) {
                MessageBodyWriter mbw = writer.getObj();
                Class writerType = Types.getTemplateParameterOfInterface(mbw.getClass(), MessageBodyWriter.class);
                if (writerType == null || writerType.equals(Object.class) || !writerType.isAssignableFrom(type))
                    continue;
                Produces produces = mbw.getClass().getAnnotation(Produces.class);
                if (produces == null)
                    continue;
                for (String produce : produces.value()) {
                    MediaType mt = MediaType.valueOf(produce);
                    if (mt.isWildcardType() || mt.isWildcardSubtype())
                        continue;
                    return mt;
                }
            }
        }
        return null;
    }

    public Map<MessageBodyWriter<?>, Class<?>> getPossibleMessageBodyWritersMap(Class type, Type genericType,
            Annotation[] annotations, MediaType accept) {
        Map<MessageBodyWriter<?>, Class<?>> map = new HashMap<MessageBodyWriter<?>, Class<?>>();
        MediaTypeMap<SortedKey<MessageBodyWriter>> serverMessageBodyWriters = getServerMessageBodyWriters();
        if (serverMessageBodyWriters == null)
            return map;
        List<SortedKey<MessageBodyWriter>> writers = serverMessageBodyWriters.getPossible(accept, type);
        for (SortedKey<MessageBodyWriter> writer : writers) {
            if (writer.getObj().isWriteable(type, genericType, annotations, accept)) {
                Class<?> mbwc = writer.getObj().getClass();
                if (!mbwc.isInterface() && mbwc.getSuperclass() != null && !mbwc.getSuperclass().equals(Object.class)
                        && mbwc.isSynthetic()) {
                    mbwc = mbwc.getSuperclass();
                }
                Class writerType = Types.getTemplateParameterOfInterface(mbwc, MessageBodyWriter.class);
                if (writerType == null || !writerType.isAssignableFrom(type))
                    continue;
                map.put(writer.getObj(), writerType);
            }
        }
        return map;
    }

    // use the tracingLogger enabled version please
    @Deprecated
    public <T> MessageBodyWriter<T> getServerMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        MediaTypeMap<SortedKey<MessageBodyWriter>> availableWriters = getServerMessageBodyWriters();
        return resolveMessageBodyWriter(type, genericType, annotations, mediaType, availableWriters);
    }

    public <T> MessageBodyWriter<T> getServerMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, RESTEasyTracingLogger tracingLogger) {

        MediaTypeMap<SortedKey<MessageBodyWriter>> availableWriters = getServerMessageBodyWriters();
        return resolveMessageBodyWriter(type, genericType, annotations, mediaType, availableWriters, tracingLogger);
    }

    /**
     * Always gets server MBW.
     *
     * @param type        the class of the object that is to be written.
     * @param genericType the type of object to be written. E.g. if the
     *                    message body is to be produced from a field, this will be
     *                    the declared type of the field as returned by {@code Field.getGenericType}.
     * @param annotations an array of the annotations on the declaration of the
     *                    artifact that will be written. E.g. if the
     *                    message body is to be produced from a field, this will be
     *                    the annotations on that field returned by
     *                    {@code Field.getDeclaredAnnotations}.
     * @param mediaType   the media type of the data that will be written.
     * @param <T>         type
     * @return message writer
     */
    public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        MediaTypeMap<SortedKey<MessageBodyWriter>> availableWriters = getServerMessageBodyWriters();
        MessageBodyWriter<T> writer = resolveMessageBodyWriter(type, genericType, annotations, mediaType,
                availableWriters);
        if (writer != null)
            LogMessages.LOGGER.debugf("MessageBodyWriter: %s", writer.getClass().getName());
        return writer;
    }

    public <T> MessageBodyWriter<T> getClientMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        MediaTypeMap<SortedKey<MessageBodyWriter>> availableWriters = getClientMessageBodyWriters();
        return resolveMessageBodyWriter(type, genericType, annotations, mediaType, availableWriters);
    }

    private <T> T findProvider(Class asyncType, Map<Class<?>, T> asyncResponseProviders) {
        if (asyncResponseProviders != null) {
            for (Class<?> aClass : asyncResponseProviders.keySet()) {
                if (aClass.isAssignableFrom(asyncType)) {
                    return asyncResponseProviders.get(aClass);
                }
            }
        }
        return null;
    }

    @Deprecated
    private <T> MessageBodyWriter<T> resolveMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MediaTypeMap<SortedKey<MessageBodyWriter>> availableWriters) {
        if (availableWriters == null)
            return null;
        List<SortedKey<MessageBodyWriter>> writers = availableWriters.getPossible(mediaType, type);
        /*
         * logger.info("*******   getMessageBodyWriter(" + type.getName() + ", " + mediaType.toString() + ")****");
         * for (SortedKey<MessageBodyWriter> writer : writers)
         * {
         * logger.info("     possible writer: " + writer.obj.getClass().getName());
         * }
         */

        for (SortedKey<MessageBodyWriter> writer : writers) {
            if (writer.getObj().isWriteable(type, genericType, annotations, mediaType)) {
                LogMessages.LOGGER.debugf("MessageBodyWriter: %s", writer.getClass().getName());
                //logger.info("   picking: " + writer.obj.getClass().getName());
                return (MessageBodyWriter<T>) writer.getObj();
            }
        }
        return null;
    }

    private <T> MessageBodyWriter<T> resolveMessageBodyWriter(Class<T> type, Type genericType,
            Annotation[] annotations, MediaType mediaType, MediaTypeMap<SortedKey<MessageBodyWriter>> availableWriters,
            RESTEasyTracingLogger tracingLogger) {
        if (availableWriters == null)
            return null;
        List<SortedKey<MessageBodyWriter>> writers = availableWriters.getPossible(mediaType, type);

        if (tracingLogger.isLogEnabled("MBW_FIND")) {
            tracingLogger.log("MBW_FIND", type.getName(),
                    (genericType instanceof Class ? ((Class) genericType).getName() : genericType), mediaType,
                    java.util.Arrays.toString(annotations));
        }

        MessageBodyWriter<T> result = null;

        Iterator<SortedKey<MessageBodyWriter>> iterator = writers.iterator();

        while (iterator.hasNext()) {
            final SortedKey<MessageBodyWriter> writer = iterator.next();
            if (writer.getObj().isWriteable(type, genericType, annotations, mediaType)) {
                LogMessages.LOGGER.debugf("MessageBodyWriter: %s", writer.getClass().getName());
                result = (MessageBodyWriter<T>) writer.getObj();
                tracingLogger.log("MBW_SELECTED", result);
                break;
            }
            tracingLogger.log("MBW_NOT_WRITEABLE", result);
        }

        if (tracingLogger.isLogEnabled("MBW_SKIPPED")) {
            while (iterator.hasNext()) {
                final SortedKey<MessageBodyWriter> writer = iterator.next();
                tracingLogger.log("MBW_SKIPPED", writer.getObj());
            }
        }
        return result;
    }

    /**
     * This is a spec method that is unsupported. It is an optional method anyways.
     *
     * @param applicationConfig application
     * @param endpointType      endpoint type
     * @return endpoint
     * @throws IllegalArgumentException      if applicationConfig is null
     * @throws UnsupportedOperationException allways throw since this method is not supported
     */
    public <T> T createEndpoint(Application applicationConfig, Class<T> endpointType)
            throws IllegalArgumentException, UnsupportedOperationException {
        if (applicationConfig == null)
            throw new IllegalArgumentException(Messages.MESSAGES.applicationParamNull());
        throw new UnsupportedOperationException();
    }

    public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
        final List<ContextResolver> resolvers = getContextResolvers(contextType, mediaType);
        if (resolvers == null)
            return null;
        if (resolvers.size() == 1)
            return resolvers.get(0);
        return new ContextResolver<T>() {
            public T getContext(Class type) {
                for (ContextResolver resolver : resolvers) {
                    Object rtn = resolver.getContext(type);
                    if (rtn != null)
                        return (T) rtn;
                }
                return null;
            }
        };
    }

    /**
     * Create an instance of a class using provider allocation rules of the specification as well as the InjectorFactory
     * only does constructor injection.
     *
     * @param clazz class
     * @param <T>   type
     * @return provider instance of type T
     */
    public <T> T createProviderInstance(Class<? extends T> clazz) {
        return Utils.createProviderInstance(this, clazz);
    }

    /**
     * Property and constructor injection using the InjectorFactory.
     *
     * @param clazz class
     * @param <T>   type
     * @return instance of type T
     */
    public <T> T injectedInstance(Class<? extends T> clazz) {
        Constructor<?> constructor = PickConstructor.pickSingletonConstructor(clazz);
        ConstructorInjector constructorInjector = getInjectorFactory().createConstructor(constructor, this);
        Object obj = constructorInjector.construct(false);
        PropertyInjector propertyInjector = getInjectorFactory().createPropertyInjector(clazz, this);
        if (obj instanceof CompletionStage) {
            CompletionStage<Object> stage = (CompletionStage<Object>) obj;
            return (T) stage.thenCompose(target -> {
                CompletionStage<Void> propertyStage = propertyInjector.inject(target, false);
                if (propertyStage != null) {
                    return propertyStage
                            .thenApply(v -> target);
                } else {
                    return CompletableFuture.completedFuture(target);
                }
            }).toCompletableFuture().getNow(null);
        }
        CompletionStage<Void> propertyStage = propertyInjector.inject(obj, false);
        if (propertyStage == null)
            return (T) obj;
        return (T) propertyStage.thenApply(v -> obj).toCompletableFuture().getNow(null);
    }

    /**
     * Property and constructor injection using the InjectorFactory.
     *
     * @param clazz    class
     * @param request  http request
     * @param response http response
     * @param <T>      type
     * @return instance of type T
     */
    public <T> T injectedInstance(Class<? extends T> clazz, HttpRequest request, HttpResponse response) {
        Constructor<?> constructor = PickConstructor.pickSingletonConstructor(clazz);
        Object obj = null;
        if (constructor == null) {
            throw new IllegalArgumentException(Messages.MESSAGES.unableToFindPublicConstructorForClass(clazz.getName()));
        } else {
            ConstructorInjector constructorInjector = getInjectorFactory().createConstructor(constructor, this);
            obj = constructorInjector.construct(request, response, false);
            if (obj instanceof CompletionStage) {
                obj = ((CompletionStage<Object>) obj).toCompletableFuture().getNow(null);
            }
        }
        PropertyInjector propertyInjector = getInjectorFactory().createPropertyInjector(clazz, this);

        CompletionStage<Void> propertyStage = propertyInjector.inject(request, response, obj, false);
        if (propertyStage != null)
            propertyStage.toCompletableFuture().getNow(null);
        return (T) obj;
    }

    // Configurable
    public Map<String, Object> getMutableProperties() {
        return properties;
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    public ResteasyProviderFactory setProperties(Map<String, Object> properties) {
        this.properties = new SnapshotMap<>(properties, false, lockSnapshots, false);
        return this;
    }

    @Override
    public ResteasyProviderFactory property(String name, Object value) {
        if (value == null)
            properties.remove(name);
        else
            properties.put(name, value);
        return this;
    }

    public Collection<Feature> getEnabledFeatures() {
        return Collections.unmodifiableSet(enabledFeatures);
    }

    @Override
    public ResteasyProviderFactory register(Class<?> providerClass) {
        registerProvider(providerClass);
        return this;
    }

    @Override
    public ResteasyProviderFactory register(Object provider) {
        registerProviderInstance(provider);
        return this;
    }

    @Override
    public ResteasyProviderFactory register(Class<?> componentClass, int priority) {
        registerProvider(componentClass, priority, false, null);
        return this;
    }

    @Override
    public ResteasyProviderFactory register(Class<?> componentClass, Class<?>... contracts) {
        if (contracts == null || contracts.length == 0) {
            LogMessages.LOGGER.attemptingToRegisterEmptyContracts(componentClass.getName());
            return this;
        }
        Map<Class<?>, Integer> cons = new HashMap<Class<?>, Integer>();
        for (Class<?> contract : contracts) {
            if (!contract.isAssignableFrom(componentClass)) {
                LogMessages.LOGGER.attemptingToRegisterUnassignableContract(componentClass.getName());
                return this;
            }
            cons.put(contract, Priorities.USER);
        }
        registerProvider(componentClass, null, false, cons);
        return this;
    }

    @Override
    public ResteasyProviderFactory register(Object component, int priority) {
        registerProviderInstance(component, null, priority, false);
        return this;
    }

    @Override
    public ResteasyProviderFactory register(Object component, Class<?>... contracts) {
        if (contracts == null || contracts.length == 0) {
            LogMessages.LOGGER.attemptingToRegisterEmptyContracts(component.getClass().getName());
            return this;
        }
        Map<Class<?>, Integer> cons = new HashMap<Class<?>, Integer>();
        for (Class<?> contract : contracts) {
            if (!contract.isAssignableFrom(component.getClass())) {
                LogMessages.LOGGER.attemptingToRegisterUnassignableContract(component.getClass().getName());
                return this;
            }
            cons.put(contract, Priorities.USER);
        }
        registerProviderInstance(component, cons, null, false);
        return this;
    }

    @Override
    public ResteasyProviderFactory register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        for (Class<?> contract : contracts.keySet()) {
            if (!contract.isAssignableFrom(componentClass)) {
                LogMessages.LOGGER.attemptingToRegisterUnassignableContract(componentClass.getName());
                return this;
            }
        }
        registerProvider(componentClass, null, false, contracts);
        return this;
    }

    @Override
    public ResteasyProviderFactory register(Object component, Map<Class<?>, Integer> contracts) {
        for (Class<?> contract : contracts.keySet()) {
            if (!contract.isAssignableFrom(component.getClass())) {
                LogMessages.LOGGER.attemptingToRegisterUnassignableContract(component.getClass().getName());
                return this;
            }
        }
        registerProviderInstance(component, contracts, null, false);
        return this;
    }

    @Override
    public Configuration getConfiguration() {
        return this;
    }

    @Override
    public RuntimeType getRuntimeType() {
        return RuntimeType.SERVER;
    }

    @Override
    public Collection<String> getPropertyNames() {
        return getProperties().keySet();
    }

    @Override
    public boolean isEnabled(Feature feature) {
        return getEnabledFeatures().contains(feature);
    }

    @Override
    public boolean isEnabled(Class<? extends Feature> featureClass) {
        Collection<Feature> enabled = getEnabledFeatures();
        //logger.info("isEnabled(Class): " + featureClass.getName() + " # enabled: " + enabled.size());
        if (enabled == null)
            return false;
        for (Feature feature : enabled) {
            //logger.info("  looking at: " + feature.getClass());
            if (featureClass.equals(feature.getClass())) {
                //logger.info("   found: " + featureClass.getName());
                return true;
            }
        }
        //logger.info("not enabled class: " + featureClass.getName());
        return false;
    }

    @Override
    public boolean isRegistered(Object component) {
        return getProviderInstances().contains(component);
    }

    @Override
    public boolean isRegistered(Class<?> componentClass) {
        return getClassContracts().containsKey(componentClass);
    }

    @Override
    public Map<Class<?>, Integer> getContracts(Class<?> componentClass) {
        Map<Class<?>, Integer> classIntegerMap = classContracts.get(componentClass);
        if (classIntegerMap == null)
            return Collections.emptyMap();
        return classIntegerMap;
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> providerClasses = getProviderClasses();
        return (providerClasses == null || providerClasses.isEmpty())
                ? Collections.emptySet()
                : Collections.unmodifiableSet(providerClasses);
    }

    @Override
    public Set<Object> getInstances() {
        Set<Object> providerInstances = getProviderInstances();
        return (providerInstances == null || providerInstances.isEmpty())
                ? Collections.emptySet()
                : Collections.unmodifiableSet(providerInstances);
    }

    @Override
    public Link.Builder createLinkBuilder() {
        return Utils.createLinkBuilder();
    }

    @Override
    public boolean hasProperty(final String name) {
        return properties.containsKey(name);
    }
  
   /**
    * Liberty does not support the optional Java SE Bootstrap API.
    */
   @Override
   public SeBootstrap.Configuration.Builder createConfigurationBuilder() {
      throw new UnsupportedOperationException("Liberty does not support the optional Jakarta Rest Java SE Bootstrap API.");  //Liberty change                                                   
//      return ResteasySeConfiguration.builder();
   }

   /**
    * Liberty does not support the optional Java SE Bootstrap API.
    */
   @Override
   public CompletionStage<SeBootstrap.Instance> bootstrap(final Application application,
                                                          final SeBootstrap.Configuration configuration) {                                                      
      throw new UnsupportedOperationException("Liberty does not support the optional Jakarta Rest Java SE Bootstrap API.");  //Liberty change                                                   
//      return ResteasySeInstance.create(Objects.requireNonNull(application, Messages.MESSAGES.nullParameter("application")),
//              configuration);
   }

   /**
    * Liberty does not support the optional Java SE Bootstrap API.
    */
   @Override
   public CompletionStage<SeBootstrap.Instance> bootstrap(final Class<? extends Application> clazz,
                                                          final SeBootstrap.Configuration configuration) {
      throw new UnsupportedOperationException("Liberty does not support the optional Jakarta Rest Java SE Bootstrap API.");  //Liberty change                                                   
 //     return ResteasySeInstance.create(Objects.requireNonNull(clazz, Messages.MESSAGES.nullParameter("clazz")),
 //             configuration);
   }

    @Override
    public EntityPart.Builder createEntityPartBuilder(final String partName) throws IllegalArgumentException {
        if (partName == null) {
            throw new IllegalArgumentException(Messages.MESSAGES.nullParameter("partName"));
        }
        final Function<Class<? extends EntityPart.Builder>, EntityPart.Builder> constructor = builderClass -> {
            try {
                final Constructor<? extends EntityPart.Builder> c = builderClass.getConstructor(String.class);
                return c.newInstance(partName);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw Messages.MESSAGES.failedToConstructClass(e, builderClass);
            }
        };
        final Optional<EntityPart.Builder> found = PriorityServiceLoader.load(EntityPart.Builder.class, constructor)
                .first();
        return found.orElseThrow(() -> Messages.MESSAGES.noImplementationFound(EntityPart.Builder.class.getName()));
    }

    public <I extends RxInvoker> RxInvokerProvider<I> getRxInvokerProvider(Class<I> clazz) {
        for (Entry<Class<?>, Map<Class<?>, Integer>> entry : classContracts.entrySet()) {
            if (entry.getValue().containsKey(RxInvokerProvider.class)) {
                RxInvokerProvider<?> rip = (RxInvokerProvider<?>) createProviderInstance(entry.getKey());
                if (rip.isProviderFor(clazz)) {
                    return (RxInvokerProvider<I>) rip;
                }
            }
        }
        return null;
    }

    public RxInvokerProvider<?> getRxInvokerProviderFromReactiveClass(Class<?> clazz) {
        return clientHelper.getRxInvokerProviderFromReactiveClass(clazz);
    }

    public boolean isReactive(Class<?> clazz) {
        return clientHelper.isReactive(clazz);
    }

    public void addResourceClassProcessor(Class<ResourceClassProcessor> processorClass, int priority) {
        ResourceClassProcessor processor = createProviderInstance(processorClass);
        addResourceClassProcessor(processor, priority);
    }

    private void addResourceClassProcessor(ResourceClassProcessor processor, int priority) {
        resourceBuilder.registerResourceClassProcessor(processor, priority);
    }

    public ResourceBuilder getResourceBuilder() {
        return resourceBuilder;
    }

    public <T> T getContextData(Class<T> type) {
        return ResteasyContext.getContextData(type);
    }

    public void initializeClientProviders(ResteasyProviderFactory factory) {
        clientHelper.initializeClientProviders(factory);
    }

    public void injectProperties(Object obj) {
        Utils.injectProperties(this, obj);
    }

    public void injectProperties(Object obj, HttpRequest request, HttpResponse response) {
        Utils.injectProperties(this, obj, request, response);
    }

    public StatisticsController getStatisticsController() {
        return statisticsController;
    }

    @Override
    public ExceptionMapper<Throwable> getThrowableExceptionMapper() {
        final ExceptionMapper<Throwable> result = getExceptionMapperForClass(Throwable.class);
        return result != null ? result : DefaultExceptionMapper.INSTANCE;
    }

    @Override
    protected boolean isOnServer() {
        return ResteasyContext.searchContextData(Dispatcher.class) != null;
    }

    /**
     * Indicates whether the default exception manager is enabled.
     *
     * @return {@code true} if the default exception is enabled, otherwise {@code false}
     */
    public boolean isDefaultExceptionManagerEnabled() {
        return defaultExceptionManagerEnabled;
    }
}
