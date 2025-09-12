/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
/*
 * JBoss, Home of Professional Open Source.
 *
 *
 * Copyright 2025 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
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
// From RESTEasy 6.2.8
// https://github.com/resteasy/resteasy/blob/43cf382fc3ae94f071893672f88ab392be3979ee/resteasy-core/src/main/java/org/jboss/resteasy/core/ResteasyDeploymentImpl.java (6.2.8.Final)
package org.jboss.resteasy.core;

import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.plugins.interceptors.RoleBasedSecurityFeature;
import org.jboss.resteasy.plugins.providers.JaxrsServerFormUrlEncodedProvider;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.ServerFormUrlEncodedProvider;
import org.jboss.resteasy.plugins.server.resourcefactory.JndiComponentResourceFactory;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.Messages;
import org.jboss.resteasy.spi.PriorityServiceLoader;
import org.jboss.resteasy.spi.config.ConfigurationFactory;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.ResourceBuilder;
import org.jboss.resteasy.util.GetRestful;

import io.openliberty.restfulWS.internal.common.api.RestfulWSEJBUtils;
import io.openliberty.restfulWS.internal.common.components.RestfulWSEJBUtilsLocator;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Configurable;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Providers;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This class is used to configure and initialize the core components of RESTEasy.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ResteasyDeploymentImpl implements ResteasyDeployment
{
   protected boolean widerRequestMatching;
   protected boolean useContainerFormParams = false;
   protected boolean deploymentSensitiveFactoryEnabled = false;
   protected boolean asyncJobServiceEnabled = false;
   protected boolean addCharset = true;
   protected int asyncJobServiceMaxJobResults = 100;
   protected long asyncJobServiceMaxWait = 300000;
   protected int asyncJobServiceThreadPoolSize = 100;
   protected String asyncJobServiceBasePath = "/asynch/jobs";
   protected String applicationClass;
   protected String injectorFactoryClass;
   protected InjectorFactory injectorFactory;
   protected Application application;
   protected boolean registerBuiltin = true;
   protected List<String> scannedResourceClasses;
   protected List<String> scannedProviderClasses;
   protected List<String> scannedJndiComponentResources;
   protected Map<String, List<String>> scannedResourceClassesWithBuilder;
   protected List<String> jndiComponentResources;
   protected List<String> providerClasses;
   @SuppressWarnings("rawtypes")
   protected List<Class> actualProviderClasses ;
   protected List<Object> providers;
   protected boolean securityEnabled = false;
   protected List<String> jndiResources;
   protected List<String> resourceClasses;
   protected List<String> unwrappedExceptions;
   @SuppressWarnings("rawtypes")
   protected List<Class> actualResourceClasses;
   protected List<ResourceFactory> resourceFactories;
   protected List<Object> resources;
   protected Map<String, String> mediaTypeMappings;
   protected Map<String, String> languageExtensions;
   @SuppressWarnings("rawtypes")
   protected Map<Class, Object> defaultContextObjects;
   protected Map<String, String> constructedDefaultContextObjects;
   protected Registry registry;
   protected Dispatcher dispatcher;
   protected ResteasyProviderFactory providerFactory;
   protected ThreadLocalResteasyProviderFactory threadLocalProviderFactory;
   protected String paramMapping;
   protected Map<String, Object> properties;
   protected boolean statisticsEnabled;

   @SuppressWarnings("rawtypes")
   public ResteasyDeploymentImpl() {
      scannedResourceClasses = new ArrayList<String>();
      scannedProviderClasses = new ArrayList<String>();
      scannedJndiComponentResources = new ArrayList<String>();
      scannedResourceClassesWithBuilder = new HashMap<>();
      jndiComponentResources = new ArrayList<String>();
      providerClasses = new ArrayList<String>();
      actualProviderClasses = new ArrayList<Class>();
      providers = new ArrayList<Object>();
      jndiResources = new ArrayList<String>();
      resourceClasses = new ArrayList<String>();
      unwrappedExceptions = new ArrayList<String>();
      actualResourceClasses = new ArrayList<Class>();
      resourceFactories = new ArrayList<ResourceFactory>();
      resources = new ArrayList<Object>();
      mediaTypeMappings = new HashMap<String, String>();
      languageExtensions = new HashMap<String, String>();
      defaultContextObjects = new HashMap<Class, Object>();
      constructedDefaultContextObjects = new HashMap<String, String>();
      properties = new TreeMap<String, Object>();
   }

   public ResteasyDeploymentImpl(final boolean quarkus) {

   }

   public void start()
   {
      try
      {
         startInternal();
      }
      finally
      {
         ThreadLocalResteasyProviderFactory.pop();
      }
   }

   private void startInternal()
   {
      initializeFactory();
      initializeDispatcher();
      pushContext();

      try
      {
         initializeObjects();

         if (securityEnabled)
         {
            providerFactory.register(RoleBasedSecurityFeature.class);
         }


         if (registerBuiltin)
         {
            providerFactory.setRegisterBuiltins(true);
            RegisterBuiltin.register(providerFactory);

            // having problems using form parameters from container for a couple of TCK tests.  I couldn't figure out
            // why, specifically:
            // com/sun/ts/tests/jaxrs/spec/provider/standardhaspriority/JAXRSClient.java#readWriteMapProviderTest_from_standalone                                               Failed. Test case throws exception: [JAXRSCommonClient] null failed!  Check output for cause of failure.
            // com/sun/ts/tests/jaxrs/spec/provider/standardwithjaxrsclient/JAXRSClient.java#mapElementProviderTest_from_standalone                                             Failed. Test case throws exception: returned MultivaluedMap is null
            providerFactory.registerProviderInstance(new ServerFormUrlEncodedProvider(useContainerFormParams), null, null, true);
            providerFactory.registerProviderInstance(new JaxrsServerFormUrlEncodedProvider(useContainerFormParams), null, null, true);
         }
         else
         {
            providerFactory.setRegisterBuiltins(false);
         }


         // register all providers
         registration();

         registerMappers();
         ((ResteasyProviderFactoryImpl)providerFactory).lockSnapshots();
      }
      finally
      {
         ResteasyContext.removeContextDataLevel();
      }
   }

   protected void registerMappers() {
      if (paramMapping != null)
      {
         providerFactory.getContainerRequestFilterRegistry().registerSingleton(new AcceptParameterHttpPreprocessor(paramMapping));
      }

      AcceptHeaderByFileSuffixFilter suffixNegotiationFilter = null;
      if (mediaTypeMappings != null && !mediaTypeMappings.isEmpty())
      {
         Map<String, MediaType> extMap = new HashMap<String, MediaType>();
         for (Map.Entry<String, String> ext : mediaTypeMappings.entrySet())
         {
            String value = ext.getValue();
            extMap.put(ext.getKey().trim(), MediaType.valueOf(value.trim()));
         }

         if (suffixNegotiationFilter == null)
         {
            suffixNegotiationFilter = new AcceptHeaderByFileSuffixFilter();
            providerFactory.getContainerRequestFilterRegistry().registerSingleton(suffixNegotiationFilter);
         }
         suffixNegotiationFilter.setMediaTypeMappings(extMap);
      }


      if (languageExtensions != null && !languageExtensions.isEmpty())
      {
         if (suffixNegotiationFilter == null)
         {
            suffixNegotiationFilter = new AcceptHeaderByFileSuffixFilter();
            providerFactory.getContainerRequestFilterRegistry().registerSingleton(suffixNegotiationFilter);
         }
         suffixNegotiationFilter.setLanguageMappings(languageExtensions);
      }
   }

   @SuppressWarnings({"rawtypes", "unchecked"})
   protected void pushContext() {
      // push context data so we can inject it
      Map contextDataMap = ResteasyContext.getContextDataMap();
      contextDataMap.putAll(dispatcher.getDefaultContextObjects());
   }

   protected void initializeObjects() {
      if (injectorFactory == null && injectorFactoryClass != null)
      {
         try
         {
            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(injectorFactoryClass);
            injectorFactory = (InjectorFactory) clazz.newInstance();
         }
         catch (ClassNotFoundException cnfe)
         {
            throw new RuntimeException(Messages.MESSAGES.unableToFindInjectorFactory(), cnfe);
         }
         catch (Exception e)
         {
            throw new RuntimeException(Messages.MESSAGES.unableToInstantiateInjectorFactory(), e);
         }
      }
      if (injectorFactory != null)
      {
         providerFactory.setInjectorFactory(injectorFactory);
      }
      // feed context data map with constructed objects
      // see ResteasyContextParameters.RESTEASY_CONTEXT_OBJECTS
      if (constructedDefaultContextObjects != null && constructedDefaultContextObjects.size() > 0)
      {
         for (Map.Entry<String, String> entry : constructedDefaultContextObjects.entrySet())
         {
            Class<?> key = null;
            try
            {
               key = Thread.currentThread().getContextClassLoader().loadClass(entry.getKey());
            }
            catch (ClassNotFoundException e)
            {
               throw new RuntimeException(Messages.MESSAGES.unableToInstantiateContextObject(entry.getKey()), e);
            }
            Object obj = createFromInjectorFactory(entry.getValue(), providerFactory);
            LogMessages.LOGGER.creatingContextObject(entry.getKey(), entry.getValue());
            defaultContextObjects.put(key, obj);
            dispatcher.getDefaultContextObjects().put(key, obj);
            ResteasyContext.getContextDataMap().put(key, obj);

         }
      }

      if (applicationClass != null)
      {
         application = createApplication(applicationClass, dispatcher, providerFactory);
      }
   }

   protected void initializeDispatcher() {
      if (asyncJobServiceEnabled)
      {
         AsynchronousDispatcher asyncDispatcher;
         if (dispatcher == null) {
            asyncDispatcher = new AsynchronousDispatcher(providerFactory);
            dispatcher = asyncDispatcher;
         } else {
            asyncDispatcher = (AsynchronousDispatcher) dispatcher;
         }
         asyncDispatcher.setMaxCacheSize(asyncJobServiceMaxJobResults);
         asyncDispatcher.setMaxWaitMilliSeconds(asyncJobServiceMaxWait);
         asyncDispatcher.setThreadPoolSize(asyncJobServiceThreadPoolSize);
         asyncDispatcher.setBasePath(asyncJobServiceBasePath);
         if (unwrappedExceptions != null) asyncDispatcher.getUnwrappedExceptions().addAll(unwrappedExceptions);
         asyncDispatcher.start();
      }
      else
      {
         SynchronousDispatcher dis;
         if (dispatcher == null) {
            dis = new SynchronousDispatcher(providerFactory);
            dispatcher = dis;
         } else {
            dis = (SynchronousDispatcher) dispatcher;
         }
         if (unwrappedExceptions != null)  dis.getUnwrappedExceptions().addAll(unwrappedExceptions);
      }
      registry = dispatcher.getRegistry();
      if (widerRequestMatching)
      {
         ((ResourceMethodRegistry)registry).setWiderMatching(widerRequestMatching);
      }

      if (defaultContextObjects != null) dispatcher.getDefaultContextObjects().putAll(defaultContextObjects);
      dispatcher.getDefaultContextObjects().put(Configurable.class, providerFactory);
      dispatcher.getDefaultContextObjects().put(Configuration.class, providerFactory);
      dispatcher.getDefaultContextObjects().put(Providers.class, providerFactory);
      dispatcher.getDefaultContextObjects().put(Registry.class, registry);
      dispatcher.getDefaultContextObjects().put(Dispatcher.class, dispatcher);
      dispatcher.getDefaultContextObjects().put(InternalDispatcher.class, InternalDispatcher.getInstance());
      dispatcher.getDefaultContextObjects().put(ResteasyDeployment.class, this);
   }

   protected void initializeFactory() {
      // it is very important that each deployment create their own provider factory
      // this allows each WAR to have their own set of providers
      if (providerFactory == null) providerFactory = new ResteasyProviderFactoryImpl();
      providerFactory.setRegisterBuiltins(registerBuiltin);
      providerFactory.getStatisticsController().setEnabled(statisticsEnabled);

      Object tracingText;
      Object thresholdText;
      Object context = getDefaultContextObjects() == null ? null : getDefaultContextObjects().get(ResteasyConfiguration.class);

      org.jboss.resteasy.spi.config.Configuration config = ConfigurationFactory.getInstance().getConfiguration((ResteasyConfiguration) context);
      tracingText = config.getOptionalValue(ResteasyContextParameters.RESTEASY_TRACING_TYPE, String.class).orElse(null);
      thresholdText = config.getOptionalValue(ResteasyContextParameters.RESTEASY_TRACING_THRESHOLD, String.class).orElse(null);

      if (tracingText != null) {
         providerFactory.property(ResteasyContextParameters.RESTEASY_TRACING_TYPE, tracingText);
      } else {
         if (context != null) {
            tracingText = ((ResteasyConfiguration) context).getParameter(ResteasyContextParameters.RESTEASY_TRACING_TYPE);
            if (tracingText != null) {
               providerFactory.property(ResteasyContextParameters.RESTEASY_TRACING_TYPE, tracingText);
            }
         }
      }

      if (thresholdText != null) {
         providerFactory.getMutableProperties().put(ResteasyContextParameters.RESTEASY_TRACING_THRESHOLD, thresholdText);
      } else {

         if (context != null) {
            thresholdText = ((ResteasyConfiguration) context).getInitParameter(ResteasyContextParameters.RESTEASY_TRACING_THRESHOLD);
            if (thresholdText != null) {
               providerFactory.getMutableProperties().put(ResteasyContextParameters.RESTEASY_TRACING_THRESHOLD, thresholdText);
            }
         }
      }

      if (deploymentSensitiveFactoryEnabled)
      {
         // the ThreadLocalResteasyProviderFactory pushes and pops this deployments parentProviderFactory
         // on a ThreadLocal stack.  This allows each application/WAR to have their own parentProviderFactory
         // and still be able to call ResteasyProviderFactory.getInstance()
         if (!(providerFactory instanceof ThreadLocalResteasyProviderFactory))
         {
            if (ResteasyProviderFactory.peekInstance() == null || !(ResteasyProviderFactory.peekInstance() instanceof ThreadLocalResteasyProviderFactory))
            {

               threadLocalProviderFactory = new ThreadLocalResteasyProviderFactory(providerFactory);
               ResteasyProviderFactory.setInstance(threadLocalProviderFactory);
            }
            else
            {
               ThreadLocalResteasyProviderFactory.push(providerFactory);
            }
         }
         else
         {
            ThreadLocalResteasyProviderFactory.push(providerFactory);
         }
      }
      else
      {
         ResteasyProviderFactory.setInstance(providerFactory);
      }
   }

   public void merge(ResteasyDeployment other)
   {
      scannedResourceClasses.addAll(other.getScannedResourceClasses());
      scannedProviderClasses.addAll(other.getScannedProviderClasses());
      scannedJndiComponentResources.addAll(other.getScannedJndiComponentResources());
      scannedResourceClassesWithBuilder.putAll(other.getScannedResourceClassesWithBuilder());

      jndiComponentResources.addAll(other.getJndiComponentResources());
      providerClasses.addAll(other.getProviderClasses());
      actualProviderClasses.addAll(other.getActualProviderClasses());
      providers.addAll(other.getProviders());

      jndiResources.addAll(other.getJndiResources());
      resourceClasses.addAll(other.getResourceClasses());
      unwrappedExceptions.addAll(other.getUnwrappedExceptions());
      actualResourceClasses.addAll(other.getActualResourceClasses());
      resourceFactories.addAll(other.getResourceFactories());
      resources.addAll(other.getResources());

      mediaTypeMappings.putAll(other.getMediaTypeMappings());
      languageExtensions.putAll(other.getLanguageExtensions());

      defaultContextObjects.putAll(other.getDefaultContextObjects());
      constructedDefaultContextObjects.putAll(other.getConstructedDefaultContextObjects());
   }

   public static Application createApplication(String applicationClass, Dispatcher dispatcher, ResteasyProviderFactory providerFactory)
   {
      Class<?> clazz = null;
      try
      {
         clazz = Thread.currentThread().getContextClassLoader().loadClass(applicationClass);
      }
      catch (ClassNotFoundException e)
      {
         throw new RuntimeException(e);
      }

      Application app = (Application)providerFactory.createProviderInstance(clazz);
      dispatcher.getDefaultContextObjects().put(Application.class, app);
      ResteasyContext.pushContext(Application.class, app);
      PropertyInjector propertyInjector = providerFactory.getInjectorFactory().createPropertyInjector(clazz, providerFactory);
      propertyInjector.inject(app, false);
      return app;
   }

   private static Object createFromInjectorFactory(String classname, ResteasyProviderFactory providerFactory)
   {
      Class<?> clazz = null;
      try
      {
         clazz = Thread.currentThread().getContextClassLoader().loadClass(classname);
      }
      catch (ClassNotFoundException e)
      {
         throw new RuntimeException(e);
      }

      Object obj = providerFactory.injectedInstance(clazz);

      return obj;
   }

   public void registration()
   {
      boolean useScanning = registerApplication();

      if (useScanning && scannedProviderClasses != null)
      {
         for (String provider : scannedProviderClasses)
         {
            registerProvider(provider);
         }
      }

      if (providerClasses != null)
      {
         for (String provider : providerClasses)
         {
            registerProvider(provider);
         }
      }
      if (providers != null)
      {
         for (Object provider : providers)
         {
            providerFactory.registerProviderInstance(provider);
         }
      }

      if (actualProviderClasses != null) {
         for (Class<?> actualProviderClass : actualProviderClasses) {
            providerFactory.registerProvider(actualProviderClass);
         }
      }
      registerResources(useScanning);

   }

   protected void registerResources(boolean useScanning) {
      // All providers should be registered before resources because of interceptors.
      // interceptors must exist as they are applied only once when the resource is registered.
       // Liberty Change Start
       System.out.println("Adam - application=" + application);
       System.out.println("Adam - useScanning=" + useScanning);
       System.out.println("Adam - scannedJndiComponentResources=" + scannedJndiComponentResources);
       System.out.println("Adam - jndiComponentResources=" + jndiComponentResources);
       System.out.println("Adam - jndiResources=" + jndiResources);
       System.out.println("Adam - scannedResourceClasses=" + scannedResourceClasses);
       System.out.println("Adam - scannedResourceClassesWithBuilder=" + scannedResourceClassesWithBuilder);
       System.out.println("Adam - resourceClasses=" + resourceClasses);
       System.out.println("Adam - resources=" + resources);
       System.out.println("Adam - actualResourceClasses=" + actualResourceClasses);
       System.out.println("Adam - resourceFactories=" + resourceFactories);

       RestfulWSEJBUtils ejbUtils = RestfulWSEJBUtilsLocator.getRestfulWSEJBUtils();
       if (useScanning && scannedResourceClasses != null) {
          List<String> holder = new ArrayList<String>();
          for (String clazz : scannedResourceClasses) {
             // is EJB?
              List<String> jndiList = ejbUtils.getJNDIResource(clazz);
              System.out.println("Adam - clazz=" + clazz);
              System.out.println("Adam - jndi=" + jndiList);
              if (jndiList != null && !jndiList.isEmpty()) {
                  for (String jndi : jndiList) {
                   // TODO: test and make sure it's correct
                      Context ctx;
                      try {
                         ctx = new InitialContext();
                         System.out.println("Adam - lookup=" + ctx.lookup(jndi));
                      } catch (NamingException e) {
                          System.out.println("Adam - failed to look up!");
                      }

                      System.out.println("Adam - adding + " + jndi + ";" + clazz + ";true");
                      scannedJndiComponentResources.add(jndi + ";" + clazz + ";true");
//                      scannedResourceClasses.remove(clazz);
                      if (!holder.contains(clazz)) {
                          holder.add(clazz);
                      }
                  }
              }
          }
          for (String clazz : holder) {
              scannedResourceClasses.remove(clazz);
          }
       }
       // Liberty Change End

      if (useScanning && scannedJndiComponentResources != null)
      {
         for (String resource : scannedJndiComponentResources)
         {
            registerJndiComponentResource(resource);
         }
      }
      if (jndiComponentResources != null)
      {
         for (String resource : jndiComponentResources)
         {
            registerJndiComponentResource(resource);
         }
      }
      if (jndiResources != null)
      {
         for (String resource : jndiResources)
         {
            registry.addJndiResource(resource.trim());
         }
      }

      if (useScanning && scannedResourceClasses != null)
      {
         for (String resource : scannedResourceClasses)
         {
            Class<?> clazz = null;
            try
            {
               clazz = Thread.currentThread().getContextClassLoader().loadClass(resource.trim());
            }
            catch (ClassNotFoundException e)
            {
               throw new RuntimeException(e);
            }
            registry.addPerRequestResource(clazz);
         }
      }

      if (useScanning && scannedResourceClassesWithBuilder != null)
      {
         for (Map.Entry<String, List<String>> entry : scannedResourceClassesWithBuilder.entrySet())
         {
            Class<?> resourceBuilderClass;
            try
            {
               resourceBuilderClass = Thread.currentThread().getContextClassLoader().loadClass(entry.getKey().trim());
            }
            catch (Exception e)
            {
               throw new RuntimeException(e);
            }
            if (!ResourceBuilder.class.isAssignableFrom(resourceBuilderClass)) {
               throw new IllegalArgumentException("Supplied class: " + resourceBuilderClass + "must be a subclass of " + ResourceBuilder.class.getName());
            }

            ResourceBuilder resourceBuilder;
            try {
               resourceBuilder = (ResourceBuilder) resourceBuilderClass.newInstance();
            } catch (Exception e) {
               throw new RuntimeException(e);
            }

            for (String resource : entry.getValue()) {
               Class<?> resourceClass;
               try
               {
                  resourceClass = Thread.currentThread().getContextClassLoader().loadClass(resource.trim());
               }
               catch (ClassNotFoundException e)
               {
                  throw new RuntimeException(e);
               }

               registry.addPerRequestResource(resourceClass, resourceBuilder);
            }
         }
      }

      if (resourceClasses != null)
      {
         for (String resource : resourceClasses)
         {
            Class<?> clazz = null;
            try
            {
               clazz = Thread.currentThread().getContextClassLoader().loadClass(resource.trim());
            }
            catch (ClassNotFoundException e)
            {
               throw new RuntimeException(e);
            }
            registry.addPerRequestResource(clazz);
         }
      }

      if (resources != null)
      {
         for (Object obj : resources)
         {
            registry.addSingletonResource(obj);
         }
      }

      if (actualResourceClasses != null) {
         for (Class<?> actualResourceClass : actualResourceClasses) {
            registry.addPerRequestResource(actualResourceClass);
         }
      }

      if (resourceFactories != null) {
         for (ResourceFactory factory : resourceFactories) {
            registry.addResourceFactory(factory);
         }
      }
      registry.checkAmbiguousUri();
   }

   protected boolean registerApplication() {
      boolean useScanning = true;
      if (application != null)
      {
         dispatcher.getDefaultContextObjects().put(Application.class, application);
         ResteasyContext.getContextDataMap().put(Application.class, application);
         if (processApplication(application))
         {
            // Application class registered something so don't use scanning data.  See JAX-RS spec for more detail.
            useScanning = false;
         }
         // Jakarta REST 3.1 section 4.1.2 requires Feature's and DynamicFeature's to use a service loader if the
         // following property was not set on the Application.
         if (isEnabled(application.getProperties(), "jakarta.ws.rs.loadServices")) {
            actualProviderClasses.addAll(loadServices(Feature.class));
            actualProviderClasses.addAll(loadServices(DynamicFeature.class));
         }
      }
      return useScanning;
   }

   private void registerJndiComponentResource(String resource)
   {
      String[] config = resource.trim().split(";");
      if (config.length < 3)
      {
         throw new RuntimeException(Messages.MESSAGES.jndiComponentResourceNotSetCorrectly());
      }
      String jndiName = config[0];
      Class<?> clazz = null;
      try
      {
         clazz = Thread.currentThread().getContextClassLoader().loadClass(config[1]);
      }
      catch (ClassNotFoundException e)
      {
         throw new RuntimeException(Messages.MESSAGES.couldNotFindClassJndi(config[1]), e);
      }
      boolean cacheRefrence = Boolean.valueOf(config[2].trim());
      JndiComponentResourceFactory factory = new JndiComponentResourceFactory(jndiName, clazz, cacheRefrence);
      getResourceFactories().add(factory);

   }

   public void stop()
   {
      if (asyncJobServiceEnabled)
      {
         ((AsynchronousDispatcher) dispatcher).stop();
      }

      ResteasyProviderFactory.clearInstanceIfEqual(threadLocalProviderFactory);
      ResteasyProviderFactory.clearInstanceIfEqual(providerFactory);
   }

   /**
    * @param config application
    * @return whether application class registered anything. i.e. whether scanning metadata should be used or not
    */
   private boolean processApplication(Application config)
   {
      LogMessages.LOGGER.deployingApplication(Application.class.getName(), config.getClass());
      boolean registered = false;
      Set<Class<?>> classes = config.getClasses();
      if (classes != null)
      {
         for (Class<?> clazz : classes)
         {
            if (GetRestful.isRootResource(clazz))
            {
               LogMessages.LOGGER.addingClassResource(clazz.getName(), config.getClass());
               actualResourceClasses.add(clazz);
               registered = true;
            }
            else
            {
               LogMessages.LOGGER.addingProviderClass(clazz.getName(), config.getClass());
               actualProviderClasses.add(clazz);
               registered = true;
            }
         }
      }
      Set<Object> singletons = config.getSingletons();
      if (singletons != null)
      {
         for (Object obj : singletons)
         {
            if (GetRestful.isRootResource(obj.getClass()))
            {
               if (actualResourceClasses.contains(obj.getClass()))
               {
                  LogMessages.LOGGER.singletonClassAlreadyDeployed("resource", obj.getClass().getName());
               }
               else
               {
                  LogMessages.LOGGER.addingSingletonResource(obj.getClass().getName(), config.getClass());
                  resources.add(obj);
                  registered = true;
               }
            }
            else
            {
               if (actualProviderClasses.contains(obj.getClass()))
               {
                  LogMessages.LOGGER.singletonClassAlreadyDeployed("provider", obj.getClass().getName());
               }
               else
               {
                  LogMessages.LOGGER.addingProviderSingleton(obj.getClass().getName(), config.getClass());
                  providers.add(obj);
                  registered = true;
               }
            }
         }
      }
      final Map<String, Object> properties = config.getProperties();
      if (properties != null && !properties.isEmpty())
      {
         Feature applicationPropertiesRegistrationFeature = new Feature()
         {
            @Override
            public boolean configure(FeatureContext featureContext)
            {
               for (Map.Entry<String, Object> property : properties.entrySet())
               {
                  featureContext = featureContext.property(property.getKey(), property.getValue());
               }
               return true;
            }
         };
         this.providers.add(0, applicationPropertiesRegistrationFeature);
      }
      return registered;
   }

   private void registerProvider(String clazz)
   {
      Class<?> provider = null;
      try
      {
         provider = Thread.currentThread().getContextClassLoader().loadClass(clazz.trim());
      }
      catch (ClassNotFoundException e)
      {
         throw new RuntimeException(e);
      }
      providerFactory.registerProvider(provider);
   }

   public boolean isUseContainerFormParams()
   {
      return useContainerFormParams;
   }

   public void setUseContainerFormParams(boolean useContainerFormParams)
   {
      this.useContainerFormParams = useContainerFormParams;
   }

   public List<String> getJndiComponentResources()
   {
      return jndiComponentResources;
   }

   public void setJndiComponentResources(List<String> jndiComponentResources)
   {
      this.jndiComponentResources = jndiComponentResources;
   }

   public String getApplicationClass()
   {
      return applicationClass;
   }

   public void setApplicationClass(String applicationClass)
   {
      this.applicationClass = applicationClass;
   }

   public String getInjectorFactoryClass()
   {
      return injectorFactoryClass;
   }

   public void setInjectorFactoryClass(String injectorFactoryClass)
   {
      this.injectorFactoryClass = injectorFactoryClass;
   }

   public boolean isDeploymentSensitiveFactoryEnabled()
   {
      return deploymentSensitiveFactoryEnabled;
   }

   public void setDeploymentSensitiveFactoryEnabled(boolean deploymentSensitiveFactoryEnabled)
   {
      this.deploymentSensitiveFactoryEnabled = deploymentSensitiveFactoryEnabled;
   }

   public boolean isAsyncJobServiceEnabled()
   {
      return asyncJobServiceEnabled;
   }

   public void setAsyncJobServiceEnabled(boolean asyncJobServiceEnabled)
   {
      this.asyncJobServiceEnabled = asyncJobServiceEnabled;
   }

   public int getAsyncJobServiceMaxJobResults()
   {
      return asyncJobServiceMaxJobResults;
   }

   public void setAsyncJobServiceMaxJobResults(int asyncJobServiceMaxJobResults)
   {
      this.asyncJobServiceMaxJobResults = asyncJobServiceMaxJobResults;
   }

   public long getAsyncJobServiceMaxWait()
   {
      return asyncJobServiceMaxWait;
   }

   public void setAsyncJobServiceMaxWait(long asyncJobServiceMaxWait)
   {
      this.asyncJobServiceMaxWait = asyncJobServiceMaxWait;
   }

   public int getAsyncJobServiceThreadPoolSize()
   {
      return asyncJobServiceThreadPoolSize;
   }

   public void setAsyncJobServiceThreadPoolSize(int asyncJobServiceThreadPoolSize)
   {
      this.asyncJobServiceThreadPoolSize = asyncJobServiceThreadPoolSize;
   }

   public String getAsyncJobServiceBasePath()
   {
      return asyncJobServiceBasePath;
   }

   public void setAsyncJobServiceBasePath(String asyncJobServiceBasePath)
   {
      this.asyncJobServiceBasePath = asyncJobServiceBasePath;
   }

   public Application getApplication()
   {
      return application;
   }

   public void setApplication(Application application)
   {
      this.application = application;
   }

   public boolean isRegisterBuiltin()
   {
      return registerBuiltin;
   }

   public void setRegisterBuiltin(boolean registerBuiltin)
   {
      this.registerBuiltin = registerBuiltin;
   }

   public List<String> getProviderClasses()
   {
      return providerClasses;
   }

   public void setProviderClasses(List<String> providerClasses)
   {
      this.providerClasses = providerClasses;
   }

   public List<Object> getProviders()
   {
      return providers;
   }

   public void setProviders(List<Object> providers)
   {
      this.providers = providers;
   }

   @SuppressWarnings("rawtypes")
   public List<Class> getActualProviderClasses()
   {
      return actualProviderClasses;
   }

   @SuppressWarnings("rawtypes")
   public void setActualProviderClasses(List<Class> actualProviderClasses)
   {
      this.actualProviderClasses = actualProviderClasses;
   }

   @SuppressWarnings("rawtypes")
   public List<Class> getActualResourceClasses()
   {
      return actualResourceClasses;
   }

   @SuppressWarnings("rawtypes")
   public void setActualResourceClasses(List<Class> actualResourceClasses)
   {
      this.actualResourceClasses = actualResourceClasses;
   }

   public boolean isSecurityEnabled()
   {
      return securityEnabled;
   }

   public void setSecurityEnabled(boolean securityEnabled)
   {
      this.securityEnabled = securityEnabled;
   }

   public List<String> getJndiResources()
   {
      return jndiResources;
   }

   public void setJndiResources(List<String> jndiResources)
   {
      this.jndiResources = jndiResources;
   }

   public List<String> getResourceClasses()
   {
      return resourceClasses;
   }

   public void setResourceClasses(List<String> resourceClasses)
   {
      this.resourceClasses = resourceClasses;
   }

   public Map<String, String> getMediaTypeMappings()
   {
      return mediaTypeMappings;
   }

   public void setMediaTypeMappings(Map<String, String> mediaTypeMappings)
   {
      this.mediaTypeMappings = mediaTypeMappings;
   }

   public List<Object> getResources()
   {
      return resources;
   }

   public void setResources(List<Object> resources)
   {
      this.resources = resources;
   }

   public Map<String, String> getLanguageExtensions()
   {
      return languageExtensions;
   }

   public void setLanguageExtensions(Map<String, String> languageExtensions)
   {
      this.languageExtensions = languageExtensions;
   }

   public Registry getRegistry()
   {
      return registry;
   }

   public void setRegistry(Registry registry)
   {
      this.registry = registry;
   }

   public Dispatcher getDispatcher()
   {
      return dispatcher;
   }

   public void setDispatcher(Dispatcher dispatcher)
   {
      this.dispatcher = dispatcher;
   }

   public ResteasyProviderFactory getProviderFactory()
   {
      return providerFactory;
   }

   public void setProviderFactory(ResteasyProviderFactory providerFactory)
   {
      this.providerFactory = providerFactory;
   }

   public void setMediaTypeParamMapping(String paramMapping)
   {
      this.paramMapping = paramMapping;
   }

   public List<ResourceFactory> getResourceFactories()
   {
      return resourceFactories;
   }

   public void setResourceFactories(List<ResourceFactory> resourceFactories)
   {
      this.resourceFactories = resourceFactories;
   }

   public List<String> getUnwrappedExceptions()
   {
      return unwrappedExceptions;
   }

   public void setUnwrappedExceptions(List<String> unwrappedExceptions)
   {
      this.unwrappedExceptions = unwrappedExceptions;
   }

   public Map<String, String> getConstructedDefaultContextObjects()
   {
      return constructedDefaultContextObjects;
   }

   public void setConstructedDefaultContextObjects(Map<String, String> constructedDefaultContextObjects)
   {
      this.constructedDefaultContextObjects = constructedDefaultContextObjects;
   }

   @SuppressWarnings("rawtypes")
   public Map<Class, Object> getDefaultContextObjects()
   {
      return defaultContextObjects;
   }

   @SuppressWarnings("rawtypes")
   public void setDefaultContextObjects(Map<Class, Object> defaultContextObjects)
   {
      this.defaultContextObjects = defaultContextObjects;
   }

   public List<String> getScannedResourceClasses()
   {
      return scannedResourceClasses;
   }

   public void setScannedResourceClasses(List<String> scannedResourceClasses)
   {
      this.scannedResourceClasses = scannedResourceClasses;
   }

   public List<String> getScannedProviderClasses()
   {
      return scannedProviderClasses;
   }

   public void setScannedProviderClasses(List<String> scannedProviderClasses)
   {
      this.scannedProviderClasses = scannedProviderClasses;
   }

   public List<String> getScannedJndiComponentResources()
   {
      return scannedJndiComponentResources;
   }

   public void setScannedJndiComponentResources(List<String> scannedJndiComponentResources)
   {
      this.scannedJndiComponentResources = scannedJndiComponentResources;
   }

   @Override
   public Map<String, List<String>> getScannedResourceClassesWithBuilder() {
      return scannedResourceClassesWithBuilder;
   }

   @Override
   public void setScannedResourceClassesWithBuilder(Map<String, List<String>> scannedResourceClassesWithBuilder) {
      this.scannedResourceClassesWithBuilder = scannedResourceClassesWithBuilder;
   }

   public boolean isWiderRequestMatching()
   {
      return widerRequestMatching;
   }

   public void setWiderRequestMatching(boolean widerRequestMatching)
   {
      this.widerRequestMatching = widerRequestMatching;
   }

   public boolean isAddCharset()
   {
      return addCharset;
   }

   public void setAddCharset(boolean addCharset)
   {
      this.addCharset = addCharset;
   }

   public InjectorFactory getInjectorFactory()
   {
      return injectorFactory;
   }

   public void setInjectorFactory(InjectorFactory injectorFactory)
   {
      this.injectorFactory = injectorFactory;
   }

   @Override
   public Object getProperty(String key) {
      return properties.get(key);
   }

   @Override
   public void setProperty(String key, Object value) {
      properties.put(key, value);
   }

   @Override
   public void setStatisticsEnabled(boolean statisticsEnabled) {
      this.statisticsEnabled = statisticsEnabled;
   }

   private static Set<Class<?>> loadServices(final Class<?> service) {
      if (System.getSecurityManager() == null) {
         final Set<Class<?>> results = new LinkedHashSet<>();
         results.addAll(PriorityServiceLoader.load(service).getTypes());
         results.addAll(PriorityServiceLoader.load(service, service.getClassLoader()).getTypes());
         return results;
      }
      return AccessController.doPrivileged((PrivilegedAction<Set<Class<?>>>) () -> {
         final Set<Class<?>> results = new LinkedHashSet<>();
         results.addAll(PriorityServiceLoader.load(service).getTypes());
         results.addAll(PriorityServiceLoader.load(service, service.getClassLoader()).getTypes());
         return results;
      });
   }

   private static boolean isEnabled(final Map<String, Object> properties, final String name) {
      final Object value = properties.get(name);
      if (value == null) {
         return true;
      }
      if (value instanceof Boolean) {
         return (Boolean) value;
      }
      return !String.valueOf(value).equalsIgnoreCase("false");
   }
}