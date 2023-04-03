/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//Port Resteasy change in https://issues.redhat.com/browse/RESTEASY-3161 - Liberty change
package org.jboss.resteasy.core.providerfactory;

import org.jboss.resteasy.spi.AsyncClientResponseProvider;
import org.jboss.resteasy.spi.AsyncResponseProvider;
import org.jboss.resteasy.spi.AsyncStreamProvider;
import org.jboss.resteasy.spi.ContextInjector;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.StringParameterUnmarshaller;
import org.jboss.resteasy.spi.interception.JaxrsInterceptorRegistry;
import org.jboss.resteasy.spi.metadata.ResourceBuilder;
import org.jboss.resteasy.spi.statistics.StatisticsController;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.client.RxInvoker;
import jakarta.ws.rs.client.RxInvokerProvider;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.Link.Builder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant.VariantListBuilder;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.WriterInterceptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * A ResteasyProviderFactoryDelegate.
 *
 * @author Nicolas NESMON
 */
public class ResteasyProviderFactoryDelegate extends ResteasyProviderFactory
{

   private final ResteasyProviderFactoryImpl resteasyProviderFactoryDelegator;

   public ResteasyProviderFactoryDelegate(final ResteasyProviderFactory resteasyProviderFactoryDelegator)
   {
      this.resteasyProviderFactoryDelegator = Objects.requireNonNull((ResteasyProviderFactoryImpl) resteasyProviderFactoryDelegator);
   }

   @Override
   public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations,
         MediaType mediaType)
   {
      return resteasyProviderFactoryDelegator.getMessageBodyReader(type, genericType, annotations, mediaType);
   }

   @Override
   public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations,
         MediaType mediaType)
   {
      return resteasyProviderFactoryDelegator.getMessageBodyWriter(type, genericType, annotations, mediaType);
   }

   @Override
   public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type)
   {
      return resteasyProviderFactoryDelegator.getExceptionMapper(type);
   }

   @Override
   public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType)
   {
      return resteasyProviderFactoryDelegator.getContextResolver(contextType, mediaType);
   }

   @Override
   public String toHeaderString(Object object)
   {
      return resteasyProviderFactoryDelegator.toHeaderString(object);
   }

   @Override
   public Configuration getConfiguration()
   {
      return resteasyProviderFactoryDelegator.getConfiguration();
   }

   @Override
   public ResteasyProviderFactory property(String name, Object value)
   {
      return resteasyProviderFactoryDelegator.property(name, value);
   }

   @Override
   public ResteasyProviderFactory register(Class<?> componentClass)
   {
      return resteasyProviderFactoryDelegator.register(componentClass);
   }

   @Override
   public ResteasyProviderFactory register(Class<?> componentClass, int priority)
   {
      return resteasyProviderFactoryDelegator.register(componentClass, priority);
   }

   @Override
   public ResteasyProviderFactory register(Class<?> componentClass, Class<?>... contracts)
   {
      return resteasyProviderFactoryDelegator.register(componentClass, contracts);
   }

   @Override
   public ResteasyProviderFactory register(Class<?> componentClass, Map<Class<?>, Integer> contracts)
   {
      return resteasyProviderFactoryDelegator.register(componentClass, contracts);
   }

   @Override
   public ResteasyProviderFactory register(Object component)
   {
      return resteasyProviderFactoryDelegator.register(component);
   }

   @Override
   public ResteasyProviderFactory register(Object component, int priority)
   {
      return resteasyProviderFactoryDelegator.register(component, priority);
   }

   @Override
   public ResteasyProviderFactory register(Object component, Class<?>... contracts)
   {
      return resteasyProviderFactoryDelegator.register(component, contracts);
   }

   @Override
   public ResteasyProviderFactory register(Object component, Map<Class<?>, Integer> contracts)
   {
      return resteasyProviderFactoryDelegator.register(component, contracts);
   }

   @Override
   public RuntimeType getRuntimeType()
   {
      return resteasyProviderFactoryDelegator.getRuntimeType();
   }

   @Override
   public Map<String, Object> getProperties()
   {
      return resteasyProviderFactoryDelegator.getProperties();
   }

   @Override
   public Object getProperty(String name)
   {
      return resteasyProviderFactoryDelegator.getProperty(name);
   }

   @Override
   public Collection<String> getPropertyNames()
   {
      return resteasyProviderFactoryDelegator.getPropertyNames();
   }

   @Override
   public boolean isEnabled(Feature feature)
   {
      return resteasyProviderFactoryDelegator.isEnabled(feature);
   }

   @Override
   public boolean isEnabled(Class<? extends Feature> featureClass)
   {
      return resteasyProviderFactoryDelegator.isEnabled(featureClass);
   }

   @Override
   public boolean isRegistered(Object component)
   {
      return resteasyProviderFactoryDelegator.isRegistered(component);
   }

   @Override
   public boolean isRegistered(Class<?> componentClass)
   {
      return resteasyProviderFactoryDelegator.isRegistered(componentClass);
   }

   @Override
   public Map<Class<?>, Integer> getContracts(Class<?> componentClass)
   {
      return null;
   }

   @Override
   public Set<Class<?>> getClasses()
   {
      return resteasyProviderFactoryDelegator.getClasses();
   }

   @Override
   public Set<Object> getInstances()
   {
      return resteasyProviderFactoryDelegator.getInstances();
   }

   @Override
   public Set<DynamicFeature> getServerDynamicFeatures()
   {
      return resteasyProviderFactoryDelegator.getServerDynamicFeatures();
   }

   @Override
   public Set<DynamicFeature> getClientDynamicFeatures()
   {
      return resteasyProviderFactoryDelegator.getClientDynamicFeatures();
   }

   @Override
   public Map<Class<?>, AsyncResponseProvider> getAsyncResponseProviders()
   {
      return resteasyProviderFactoryDelegator.getAsyncResponseProviders();
   }

   @Override
   public Map<Class<?>, AsyncClientResponseProvider> getAsyncClientResponseProviders()
   {
      return resteasyProviderFactoryDelegator.getAsyncClientResponseProviders();
   }

   @Override
   public Map<Class<?>, AsyncStreamProvider> getAsyncStreamProviders()
   {
      return resteasyProviderFactoryDelegator.getAsyncStreamProviders();
   }

   @Override
   public Map<Type, ContextInjector> getContextInjectors()
   {
      return resteasyProviderFactoryDelegator.getContextInjectors();
   }

   @Override
   public Map<Type, ContextInjector> getAsyncContextInjectors()
   {
      return resteasyProviderFactoryDelegator.getAsyncContextInjectors();
   }

   @Override
   public Set<Class<?>> getProviderClasses()
   {
      return resteasyProviderFactoryDelegator.getProviderClasses();
   }

   @Override
   public Set<Object> getProviderInstances()
   {
      return resteasyProviderFactoryDelegator.getProviderInstances();
   }

   @Override
   public <T> T getContextData(Class<T> type)
   {
      return resteasyProviderFactoryDelegator.getContextData(type);
   }

   @Override
   public <T> T getContextData(Class<T> rawType, Type genericType, Annotation[] annotations, boolean unwrapAsync)
   {
      return resteasyProviderFactoryDelegator.getContextData(rawType, genericType, annotations, unwrapAsync);
   }

   @Override
   protected void registerBuiltin()
   {
     throw new UnsupportedOperationException();
   }

   @Override
   public boolean isRegisterBuiltins()
   {
      return resteasyProviderFactoryDelegator.isRegisterBuiltins();
   }

   @Override
   public void setRegisterBuiltins(boolean registerBuiltins)
   {
      resteasyProviderFactoryDelegator.setRegisterBuiltins(registerBuiltins);
   }

   @Override
   public InjectorFactory getInjectorFactory()
   {
      return resteasyProviderFactoryDelegator.getInjectorFactory();
   }

   @Override
   public void setInjectorFactory(InjectorFactory injectorFactory)
   {
      resteasyProviderFactoryDelegator.setInjectorFactory(injectorFactory);
   }

   @Override
   public JaxrsInterceptorRegistry<ReaderInterceptor> getServerReaderInterceptorRegistry()
   {
      return resteasyProviderFactoryDelegator.getServerReaderInterceptorRegistry();
   }

   @Override
   public JaxrsInterceptorRegistry<WriterInterceptor> getServerWriterInterceptorRegistry()
   {
      return resteasyProviderFactoryDelegator.getServerWriterInterceptorRegistry();
   }

   @Override
   public JaxrsInterceptorRegistry<ContainerRequestFilter> getContainerRequestFilterRegistry()
   {
      return resteasyProviderFactoryDelegator.getContainerRequestFilterRegistry();
   }

   @Override
   public JaxrsInterceptorRegistry<ContainerResponseFilter> getContainerResponseFilterRegistry()
   {
      return resteasyProviderFactoryDelegator.getContainerResponseFilterRegistry();
   }

   @Override
   public JaxrsInterceptorRegistry<ReaderInterceptor> getClientReaderInterceptorRegistry()
   {
      return resteasyProviderFactoryDelegator.getClientReaderInterceptorRegistry();
   }

   @Override
   public JaxrsInterceptorRegistry<WriterInterceptor> getClientWriterInterceptorRegistry()
   {
      return resteasyProviderFactoryDelegator.getClientWriterInterceptorRegistry();
   }

   @Override
   public JaxrsInterceptorRegistry<ClientRequestFilter> getClientRequestFilterRegistry()
   {
      return resteasyProviderFactoryDelegator.getClientRequestFilterRegistry();
   }

   @Override
   public JaxrsInterceptorRegistry<ClientResponseFilter> getClientResponseFilters()
   {
      return resteasyProviderFactoryDelegator.getClientResponseFilters();
   }

   @Override
   public boolean isBuiltinsRegistered()
   {
      return resteasyProviderFactoryDelegator.isBuiltinsRegistered();
   }

   @Override
   public void setBuiltinsRegistered(boolean builtinsRegistered)
   {
      resteasyProviderFactoryDelegator.setBuiltinsRegistered(builtinsRegistered);
   }

   @Override
   public void addHeaderDelegate(Class clazz, HeaderDelegate header)
   {
      resteasyProviderFactoryDelegator.addHeaderDelegate(clazz, header);
   }

   @SuppressWarnings("deprecation")
   @Override
   public <T> MessageBodyReader<T> getServerMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations,
         MediaType mediaType)
   {
      return resteasyProviderFactoryDelegator.getServerMessageBodyReader(type, genericType, annotations, mediaType);
   }

   @Override
   public <T> MessageBodyReader<T> getClientMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations,
         MediaType mediaType)
   {
      return resteasyProviderFactoryDelegator.getClientMessageBodyReader(type, genericType, annotations, mediaType);
   }

   @Override
   public List<ContextResolver> getContextResolvers(Class<?> clazz, MediaType type)
   {
      return resteasyProviderFactoryDelegator.getContextResolvers(clazz, type);
   }

   @Override
   public ParamConverter getParamConverter(Class clazz, Type genericType, Annotation[] annotations)
   {
      return resteasyProviderFactoryDelegator.getParamConverter(clazz, genericType, annotations);
   }

   @Override
   public <T> StringParameterUnmarshaller<T> createStringParameterUnmarshaller(Class<T> clazz)
   {
      return resteasyProviderFactoryDelegator.createStringParameterUnmarshaller(clazz);
   }

   @Override
   public void registerProvider(Class provider)
   {
      resteasyProviderFactoryDelegator.registerProvider(provider);
   }

   @Override
   public String toString(Object object, Class clazz, Type genericType, Annotation[] annotations)
   {
      return resteasyProviderFactoryDelegator.toString(object, clazz, genericType, annotations);
   }

   @Override
   public HeaderDelegate getHeaderDelegate(Class<?> aClass)
   {
      return resteasyProviderFactoryDelegator.getHeaderDelegate(aClass);
   }

   @Override
   public void registerProvider(Class provider, boolean isBuiltin)
   {
      resteasyProviderFactoryDelegator.registerProvider(provider, isBuiltin);
   }

   @Override
   public void registerProvider(Class provider, Integer priorityOverride, boolean isBuiltin,
         Map<Class<?>, Integer> contracts)
   {
      resteasyProviderFactoryDelegator.registerProvider(provider, priorityOverride, isBuiltin, contracts);
   }

   @Override
   public void registerProviderInstance(Object provider)
   {
      resteasyProviderFactoryDelegator.registerProviderInstance(provider);
   }

   @Override
   public void registerProviderInstance(Object provider, Map<Class<?>, Integer> contracts, Integer priorityOverride,
         boolean builtIn)
   {
      resteasyProviderFactoryDelegator.registerProviderInstance(provider, contracts, priorityOverride, builtIn);
   }

   @Override
   public <T> AsyncResponseProvider<T> getAsyncResponseProvider(Class<T> type)
   {
      return resteasyProviderFactoryDelegator.getAsyncResponseProvider(type);
   }

   @Override
   public <T> AsyncClientResponseProvider<T> getAsyncClientResponseProvider(Class<T> type)
   {
      return resteasyProviderFactoryDelegator.getAsyncClientResponseProvider(type);
   }

   @Override
   public <T> AsyncStreamProvider<T> getAsyncStreamProvider(Class<T> type)
   {
      return resteasyProviderFactoryDelegator.getAsyncStreamProvider(type);
   }

   @Override
   public MediaType getConcreteMediaTypeFromMessageBodyWriters(Class<?> type, Type genericType,
         Annotation[] annotations, MediaType mediaType)
   {
      return resteasyProviderFactoryDelegator.getConcreteMediaTypeFromMessageBodyWriters(type, genericType, annotations, mediaType);
   }

   @Override
   public Map<MessageBodyWriter<?>, Class<?>> getPossibleMessageBodyWritersMap(Class type, Type genericType,
         Annotation[] annotations, MediaType accept)
   {
      return resteasyProviderFactoryDelegator.getPossibleMessageBodyWritersMap(type, genericType, annotations, accept);
   }

   @SuppressWarnings("deprecation")
   @Override
   public <T> MessageBodyWriter<T> getServerMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations,
         MediaType mediaType)
   {
      return resteasyProviderFactoryDelegator.getServerMessageBodyWriter(type, genericType, annotations, mediaType);
   }

   @Override
   public <T> MessageBodyWriter<T> getClientMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations,
         MediaType mediaType)
   {
      return resteasyProviderFactoryDelegator.getClientMessageBodyWriter(type, genericType, annotations, mediaType);
   }

   @Override
   public <T> T createProviderInstance(Class<? extends T> clazz)
   {
      return resteasyProviderFactoryDelegator.createProviderInstance(clazz);
   }

   @Override
   public <T> T injectedInstance(Class<? extends T> clazz)
   {
      return resteasyProviderFactoryDelegator.injectedInstance(clazz);
   }

   @Override
   public <T> T injectedInstance(Class<? extends T> clazz, HttpRequest request, HttpResponse response)
   {
      return resteasyProviderFactoryDelegator.injectedInstance(clazz, request, response);
   }

   @Override
   public void injectProperties(Object obj)
   {
      resteasyProviderFactoryDelegator.injectProperties(obj);
   }

   @Override
   public void injectProperties(Object obj, HttpRequest request, HttpResponse response)
   {
      resteasyProviderFactoryDelegator.injectProperties(obj, request, response);
   }

   @Override
   public Map<String, Object> getMutableProperties()
   {
      return resteasyProviderFactoryDelegator.getMutableProperties();
   }

   @Override
   public ResteasyProviderFactory setProperties(Map<String, Object> properties)
   {
      return resteasyProviderFactoryDelegator.setProperties(properties);
   }

   @Override
   public Collection<Feature> getEnabledFeatures()
   {
      return resteasyProviderFactoryDelegator.getEnabledFeatures();
   }

   @Override
   public <I extends RxInvoker> RxInvokerProvider<I> getRxInvokerProvider(Class<I> clazz)
   {
      return resteasyProviderFactoryDelegator.getRxInvokerProvider(clazz);
   }

   @Override
   public RxInvokerProvider<?> getRxInvokerProviderFromReactiveClass(Class<?> clazz)
   {
      return resteasyProviderFactoryDelegator.getRxInvokerProviderFromReactiveClass(clazz);
   }

   @Override
   public boolean isReactive(Class<?> clazz)
   {
      return resteasyProviderFactoryDelegator.isReactive(clazz);
   }

   @Override
   public ResourceBuilder getResourceBuilder()
   {
      return resteasyProviderFactoryDelegator.getResourceBuilder();
   }

   @Override
   public void initializeClientProviders(ResteasyProviderFactory factory)
   {
      resteasyProviderFactoryDelegator.initializeClientProviders(factory);
   }

   @Override
   public UriBuilder createUriBuilder()
   {
      return resteasyProviderFactoryDelegator.createUriBuilder();
   }

   @Override
   public ResponseBuilder createResponseBuilder()
   {
      return resteasyProviderFactoryDelegator.createResponseBuilder();
   }

   @Override
   public VariantListBuilder createVariantListBuilder()
   {
      return resteasyProviderFactoryDelegator.createVariantListBuilder();
   }

   @Override
   public <T> T createEndpoint(Application application, Class<T> endpointType)
         throws IllegalArgumentException, UnsupportedOperationException
   {
      return resteasyProviderFactoryDelegator.createEndpoint(application, endpointType);
   }

   @Override
   public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) throws IllegalArgumentException
   {
      return resteasyProviderFactoryDelegator.createHeaderDelegate(type);
   }

   @Override
   public Builder createLinkBuilder()
   {
      return resteasyProviderFactoryDelegator.createLinkBuilder();
   }

   @Override
   public StatisticsController getStatisticsController()
   {
      return resteasyProviderFactoryDelegator.getStatisticsController();
   }

   @Override
   protected boolean isOnServer() {
      return resteasyProviderFactoryDelegator.isOnServer();
   }
}
