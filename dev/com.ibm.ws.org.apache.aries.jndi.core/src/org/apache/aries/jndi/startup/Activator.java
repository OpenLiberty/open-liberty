/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.jndi.startup;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;

import org.apache.aries.jndi.AugmenterInvokerImpl;
import org.apache.aries.jndi.ContextManagerServiceFactory;
import org.apache.aries.jndi.JREInitialContextFactoryBuilder;
import org.apache.aries.jndi.OSGiInitialContextFactoryBuilder;
import org.apache.aries.jndi.OSGiObjectFactoryBuilder;
import org.apache.aries.jndi.ProviderAdminServiceFactory;
import org.apache.aries.jndi.Utils;
import org.apache.aries.jndi.spi.AugmenterInvoker;
import org.apache.aries.jndi.tracker.CachingServiceTracker;
import org.apache.aries.jndi.urls.URLObjectFactoryFinder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.service.jndi.JNDIContextManager;
import org.osgi.service.jndi.JNDIProviderAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator for this bundle makes sure the static classes in it are
 * driven so they can do their magic stuff properly.
 */
public class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class.getName());

    private static final String DISABLE_BUILDER = "org.apache.aries.jndi.disable.builder";
    private static final String FORCE_BUILDER = "org.apache.aries.jndi.force.builder";

    private static volatile Activator instance;

    private BundleTracker<ServiceCache> bundleServiceCaches;

    private CachingServiceTracker<InitialContextFactoryBuilder> icfBuilders;
    private CachingServiceTracker<URLObjectFactoryFinder> urlObjectFactoryFinders;
    private CachingServiceTracker<InitialContextFactory> initialContextFactories;
    private CachingServiceTracker<ObjectFactory> objectFactories;

    private AugmenterInvoker augmenterInvoker;

    private InitialContextFactoryBuilder originalICFBuilder;
    private OSGiInitialContextFactoryBuilder icfBuilder;

    private ObjectFactoryBuilder originalOFBuilder;
    private OSGiObjectFactoryBuilder ofBuilder;

    public static Collection<ServiceReference<InitialContextFactoryBuilder>> getInitialContextFactoryBuilderServices() {
        return instance.icfBuilders.getReferences();
    }

    public static Collection<ServiceReference<InitialContextFactory>> getInitialContextFactoryServices() {
        return instance.initialContextFactories.getReferences();
    }

    public static Collection<ServiceReference<URLObjectFactoryFinder>> getURLObjectFactoryFinderServices() {
        return instance.urlObjectFactoryFinders.getReferences();
    }

    public static ServiceReference<ObjectFactory> getUrlFactory(String scheme) {
        return instance.objectFactories.find(scheme);
    }

    public static ServiceReference<InitialContextFactory> getInitialContextFactory(String interfaceName) {
        return instance.initialContextFactories.find(interfaceName);
    }

    public static AugmenterInvoker getAugmenterInvoker() {
        return instance.augmenterInvoker;
    }

    public static <T> T getService(BundleContext context, ServiceReference<T> ref) {
        ServiceCache cache = getServiceCache(context);
        return cache.getService(ref);
    }

    public static <T> Collection<ServiceReference<T>> getReferences(BundleContext context, Class<T> clazz) {
        ServiceCache cache = getServiceCache(context);
        return cache.getReferences(clazz);
    }

    public static <T> Iterable<T> getServices(BundleContext context, Class<T> clazz) {
        ServiceCache cache = getServiceCache(context);
        if (cache == null) {
            cache = new ServiceCache(context);
        }
        Collection<ServiceReference<T>> refs = cache.getReferences(clazz);
        return () -> Utils.map(refs.iterator(), ref -> Activator.getService(context, ref));
    }

    private static ServiceCache getServiceCache(BundleContext context) {
        ServiceCache cache = instance.bundleServiceCaches.getObject(context.getBundle());
        if (cache == null) {
            cache = new ServiceCache(context);
        }
        return cache;
    }

    @Override
    public void start(BundleContext context) {
        instance = this;
        BundleContext trackerBundleContext;
        /* Use system context to allow trackers full bundle/service visibility. */
        if (!Boolean.getBoolean("org.apache.aries.jndi.trackersUseLocalContext")) {
            trackerBundleContext = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
            if (trackerBundleContext == null) {
                throw new IllegalStateException("Bundle could not aquire system bundle context.");
            }
        } else {
            trackerBundleContext = context;
        }

        bundleServiceCaches = new BundleTracker<ServiceCache>(trackerBundleContext, Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING, null) {
            @Override
            public ServiceCache addingBundle(Bundle bundle, BundleEvent event) {
                return new ServiceCache(bundle.getBundleContext());
            }

            @Override
            public void modifiedBundle(Bundle bundle, BundleEvent event, ServiceCache object) {
            }
        };
        bundleServiceCaches.open();

        initialContextFactories = new CachingServiceTracker<>(trackerBundleContext, InitialContextFactory.class, Activator::getInitialContextFactoryInterfaces);
        objectFactories = new CachingServiceTracker<>(trackerBundleContext, ObjectFactory.class, Activator::getObjectFactorySchemes);
        icfBuilders = new CachingServiceTracker<>(trackerBundleContext, InitialContextFactoryBuilder.class);
        urlObjectFactoryFinders = new CachingServiceTracker<>(trackerBundleContext, URLObjectFactoryFinder.class);

        if (!isPropertyEnabled(context, DISABLE_BUILDER)) {
            try {
                OSGiInitialContextFactoryBuilder builder = new OSGiInitialContextFactoryBuilder();
                try {
                    NamingManager.setInitialContextFactoryBuilder(builder);
                } catch (IllegalStateException e) {
                    // use reflection to force the builder to be used
                    if (isPropertyEnabled(context, FORCE_BUILDER)) {
                        originalICFBuilder = swapStaticField(NamingManager.class, InitialContextFactoryBuilder.class, builder);
                    }
                }
                icfBuilder = builder;
            } catch (NamingException e) {
                LOGGER.debug("A failure occurred when attempting to register an InitialContextFactoryBuilder with the NamingManager. " +
                             "Support for calling new InitialContext() will not be enabled.", e);
            } catch (IllegalStateException e) {
                // Log the problem at info level, but only log the exception at debug level, as in many cases this is not a real issue and people
                // don't want to see stack traces at info level when everything it working as expected.
                String msg = "It was not possible to register an InitialContextFactoryBuilder with the NamingManager because " +
                             "another builder called " + getClassName(InitialContextFactoryBuilder.class)
                             + " was already registered. Support for calling new InitialContext() will not be enabled.";
                LOGGER.info(msg);
                LOGGER.debug(msg, e);
            }

            try {
                OSGiObjectFactoryBuilder builder = new OSGiObjectFactoryBuilder(context);
                try {
                    NamingManager.setObjectFactoryBuilder(builder);
                } catch (IllegalStateException e) {
                    // use reflection to force the builder to be used
                    if (isPropertyEnabled(context, FORCE_BUILDER)) {
                        swapObjectFactoryBuilderStaticField(builder);
                    }
                }
                ofBuilder = builder;
            } catch (NamingException e) {
                LOGGER.info("A failure occurred when attempting to register an ObjectFactoryBuilder with the NamingManager. " +
                            "Looking up certain objects may not work correctly.", e);
            } catch (IllegalStateException e) {
                // Log the problem at info level, but only log the exception at debug level, as in many cases this is not a real issue and people
                // don't want to see stack traces at info level when everything it working as expected.
                String msg = "It was not possible to register an ObjectFactoryBuilder with the NamingManager because " +
                             "another builder called " + getClassName(ObjectFactoryBuilder.class) + " was already registered. Looking up certain objects may not work correctly.";
                LOGGER.info(msg);
                LOGGER.debug(msg, e);
            }
        }

        context.registerService(JNDIProviderAdmin.class.getName(), new ProviderAdminServiceFactory(context), null);
        context.registerService(InitialContextFactoryBuilder.class.getName(), new JREInitialContextFactoryBuilder(), null);
        context.registerService(JNDIContextManager.class.getName(), new ContextManagerServiceFactory(), null);
        context.registerService(AugmenterInvoker.class.getName(), augmenterInvoker = new AugmenterInvokerImpl(context), null);
    }

    @Override
    public void stop(BundleContext context) {
        bundleServiceCaches.close();

        /*
         * Try to reset the InitialContextFactoryBuilder and ObjectFactoryBuilder
         * on the NamingManager.
         */
        if (icfBuilder != null) {
            swapStaticField(NamingManager.class, InitialContextFactoryBuilder.class, originalICFBuilder);
        }
        if (ofBuilder != null) {
            swapObjectFactoryBuilderStaticField(originalOFBuilder);
        }

        icfBuilders.close();
        urlObjectFactoryFinders.close();
        objectFactories.close();
        initialContextFactories.close();

        instance = null;
    }

    private void swapObjectFactoryBuilderStaticField(ObjectFactoryBuilder value) {
        try {
            // Try in NamingManager first.
            swapStaticField(NamingManager.class, ObjectFactoryBuilder.class, value);
        } catch (IllegalStateException ise) {
            // The field may have been moved to NamingManagerHelper depending on the JDK version, so let's try there.
            try {
                Class<?> namingManagerHelperClass = Class.forName("com.sun.naming.internal.NamingManagerHelper");
                swapStaticField(namingManagerHelperClass, ObjectFactoryBuilder.class, value);
            } catch (ClassNotFoundException cnfe) {
                // The NamingManagerHelper class does not exist, so the IllegalStateException may have been
                // due to another reason, so let's throw the original exception.
                throw ise;
            }
        }
    }

    private boolean isPropertyEnabled(BundleContext context, String key) {
        String value = context.getProperty(key);
        if ("false".equals(value))
            return false;
        if ("no".equals(value))
            return false;
        if (null != value)
            return true;
        Object revision = context.getBundle().adapt(BundleRevision.class);
        // in a unit test adapt() may return an incompatible object
        if (!!!(revision instanceof BundleRevision))
            return false;
        final BundleRevision bundleRevision = (BundleRevision) revision;
        return bundleRevision.getDeclaredCapabilities(key).size() > 0;
    }

    private String getClassName(Class<?> expectedType) {
        try {
            for (Field field : NamingManager.class.getDeclaredFields()) {
                if (expectedType.equals(field.getType())) {
                    field.setAccessible(true);
                    Object icf = field.get(null);
                    return icf.getClass().getName();
                }
            }
        } catch (Throwable t) {
            // Ignore
        }
        return "";
    }

    /*
     * There are no public API to reset the InitialContextFactoryBuilder or
     * ObjectFactoryBuilder on the NamingManager so try to use reflection.
     */
    private static <T> T swapStaticField(Class targetClass, Class<T> fieldType, Object value) throws IllegalStateException {
        try {
            for (Field field : targetClass.getDeclaredFields()) {
                if (fieldType.equals(field.getType())) {
                    field.setAccessible(true);
                    T original = fieldType.cast(field.get(null));
                    field.set(null, value);
                    return original;
                }
            }
        } catch (Throwable t) {
            // Ignore
            LOGGER.debug("Error setting field.", t);
            throw new IllegalStateException(t);
        }
        throw new IllegalStateException("Error setting field: no field found for type " + fieldType);
    }

    private static List<String> getInitialContextFactoryInterfaces(ServiceReference<InitialContextFactory> ref) {
        String[] interfaces = (String[]) ref.getProperty(Constants.OBJECTCLASS);
        List<String> resultList = new ArrayList<>();
        for (String interfaceName : interfaces) {
            if (!InitialContextFactory.class.getName().equals(interfaceName)) {
                resultList.add(interfaceName);
            }
        }

        return resultList;
    }

    private static List<String> getObjectFactorySchemes(ServiceReference<ObjectFactory> reference) {
        Object scheme = reference.getProperty(JNDIConstants.JNDI_URLSCHEME);
        List<String> result;

        if (scheme instanceof String) {
            result = new ArrayList<>();
            result.add((String) scheme);
        } else if (scheme instanceof String[]) {
            result = Arrays.asList((String[]) scheme);
        } else {
            result = Collections.emptyList();
        }

        return result;
    }

    private static class ServiceCache {

        private final BundleContext context;
        private final Map<ServiceReference<?>, Object> cache = new ConcurrentHashMap<>();
        private final Map<Class<?>, CachingServiceTracker<?>> trackers = new ConcurrentHashMap<>();

        ServiceCache(BundleContext context) {
            this.context = context;
        }

        @SuppressWarnings("unchecked")
        <T> T getService(ServiceReference<T> ref) {
            return (T) cache.computeIfAbsent(ref, this::doGetService);
        }

        @SuppressWarnings("unchecked")
        <T> Collection<ServiceReference<T>> getReferences(Class<T> clazz) {
            return (List) trackers.computeIfAbsent(clazz, c -> new CachingServiceTracker<>(context, c)).getReferences();
        }

        Object doGetService(ServiceReference<?> ref) {
            return Utils.doPrivileged(() -> context.getService(ref));
        }
    }

}
