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
import java.util.Arrays;

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
import org.apache.aries.jndi.spi.EnvironmentAugmentation;
import org.apache.aries.jndi.spi.EnvironmentUnaugmentation;
import org.apache.aries.jndi.tracker.ServiceTrackerCustomizers;
import org.apache.aries.jndi.urls.URLObjectFactoryFinder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIContextManager;
import org.osgi.service.jndi.JNDIProviderAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator for this bundle makes sure the static classes in it are
 * driven so they can do their magic stuff properly.
 * 
 * TODO This class is a copy of the open source aries jndi class with
 * modifications to support "better" handling of the NamingManager
 * statics. The behavior now is to save the original values of the statics
 * and set the aries ones. If this bundle gets uninstalled, it resets
 * them back to their original values.
 * 
 * These changes need to make it into the open source repository, and
 * when they do and the binaries are updated this source overlay should
 * no longer be necessary.
 */
public class Activator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class.getName());

    private OSGiInitialContextFactoryBuilder icfBuilder;
    private static InitialContextFactoryBuilder originalICFBuilder;
    private OSGiObjectFactoryBuilder ofBuilder;
    private static ObjectFactoryBuilder originalOFBuilder;
    private static volatile ServiceTracker icfBuilders;
    private static volatile ServiceTracker urlObjectFactoryFinders;
    private static volatile ServiceTracker initialContextFactories;
    private static volatile ServiceTracker objectFactories;
    private static volatile ServiceTracker environmentAugmentors;
    private static volatile ServiceTracker environmentUnaugmentors;

    // private BundleTracker bt = null;

    @Override
    public void start(BundleContext context) {

        initialContextFactories = initServiceTracker(context, InitialContextFactory.class, ServiceTrackerCustomizers.ICF_CACHE);
        objectFactories = initServiceTracker(context, ObjectFactory.class, ServiceTrackerCustomizers.URL_FACTORY_CACHE);
        icfBuilders = initServiceTracker(context, InitialContextFactoryBuilder.class, ServiceTrackerCustomizers.LAZY);
        urlObjectFactoryFinders = initServiceTracker(context, URLObjectFactoryFinder.class, ServiceTrackerCustomizers.LAZY);
        environmentAugmentors = initServiceTracker(context, EnvironmentAugmentation.class, null);
        environmentUnaugmentors = initServiceTracker(context, EnvironmentUnaugmentation.class, null);

        try {
            OSGiInitialContextFactoryBuilder builder = new OSGiInitialContextFactoryBuilder();
            setField(InitialContextFactoryBuilder.class, builder, true);
            icfBuilder = builder;
        } catch (IllegalStateException e) {
            // Log the problem at info level, but only log the exception at debug level, as in many cases this is not a real issue and people
            // don't want to see stack traces at info level when everything it working as expected.
            LOGGER.info(Utils.MESSAGES.getMessage("unable.to.set.static.ICFB.already.exists", getClassName(InitialContextFactoryBuilder.class)));
            LOGGER.debug(Utils.MESSAGES.getMessage("unable.to.set.static.ICFB.already.exists", getClassName(InitialContextFactoryBuilder.class)), e);
        }

        try {
            OSGiObjectFactoryBuilder builder = new OSGiObjectFactoryBuilder(context);
            setField(ObjectFactoryBuilder.class, builder, true);
            ofBuilder = builder;
        } catch (IllegalStateException e) {
            // Log the problem at info level, but only log the exception at debug level, as in many cases this is not a real issue and people
            // don't want to see stack traces at info level when everything it working as expected.
            LOGGER.info(Utils.MESSAGES.getMessage("unable.to.set.static.OFB.already.exists", getClassName(ObjectFactoryBuilder.class)));
            LOGGER.debug(Utils.MESSAGES.getMessage("unable.to.set.static.OFB.already.exists", getClassName(ObjectFactoryBuilder.class)), e);
        }

        context.registerService(JNDIProviderAdmin.class.getName(),
                                new ProviderAdminServiceFactory(context),
                                null);

        context.registerService(InitialContextFactoryBuilder.class.getName(),
                                new JREInitialContextFactoryBuilder(),
                                null);

        context.registerService(JNDIContextManager.class.getName(),
                                new ContextManagerServiceFactory(),
                                null);

        context.registerService(AugmenterInvoker.class.getName(),
                                AugmenterInvokerImpl.getInstance(),
                                null);

        //Start the bundletracker that clears out the cache. (only interested in stopping events)
        // bt = new BundleTracker(context, Bundle.STOPPING, new ServiceTrackerCustomizers.CacheBundleTrackerCustomizer());
        // bt.open();
    }

    private String getClassName(Class<?> expectedType)
    {
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

    private ServiceTracker initServiceTracker(BundleContext context,
                                              Class<?> type, ServiceTrackerCustomizer custom)
    {
        ServiceTracker t = new ServiceTracker(context, type.getName(), custom);
        t.open();
        return t;
    }

    @Override
    public void stop(BundleContext context) {
        /*
         * Try to reset the InitialContextFactoryBuilder and ObjectFactoryBuilder
         * on the NamingManager.
         */
        if (icfBuilder != null) {
            setField(InitialContextFactoryBuilder.class, originalICFBuilder, false);
        }
        if (ofBuilder != null) {
            setField(ObjectFactoryBuilder.class, originalOFBuilder, false);
        }

        icfBuilders.close();
        urlObjectFactoryFinders.close();
        objectFactories.close();
        initialContextFactories.close();
        environmentAugmentors.close();
        environmentUnaugmentors.close();

        //if (bt != null) {
        //    bt.close();
        //}
    }

    /*
     * There are no public API to reset the InitialContextFactoryBuilder or
     * ObjectFactoryBuilder on the NamingManager so try to use reflection.
     */
    private static void setField(Class<?> expectedType, Object value, boolean saveOriginal) throws IllegalStateException {
        try {
            for (Field field : NamingManager.class.getDeclaredFields()) {
                if (expectedType.equals(field.getType())) {
                    field.setAccessible(true);
                    if (saveOriginal) {
                        if (expectedType.equals(InitialContextFactoryBuilder.class)) {
                            originalICFBuilder = (InitialContextFactoryBuilder) field.get(null);
                        } else {
                            originalOFBuilder = (ObjectFactoryBuilder) field.get(null);
                        }
                    }

                    field.set(null, value);
                }
            }
        } catch (Throwable t) {
            // Ignore
            LOGGER.debug("Error setting field.", t);
            throw new IllegalStateException(t);
        }
    }

    public static ServiceReference[] getInitialContextFactoryBuilderServices()
    {
        ServiceReference[] refs = icfBuilders.getServiceReferences();

        if (refs != null) {
            Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);

            // Diagnostics: we need to know if nulls are being returned in the array
            for (int i = 0; i < refs.length; i++) {
                if (refs[i] == null) {
                    LOGGER.warn("org.apache.aries.jndi.startup.Activator.getInitialContextFactoryBuilderServices: refs[" + i + "] is null");
                }
            }
        }

        return refs;
    }

    public static ServiceReference[] getInitialContextFactoryServices()
    {
        ServiceReference[] refs = initialContextFactories.getServiceReferences();

        if (refs != null) {
            Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);

            // Diagnostics: we need to know if nulls are being returned in the array
            for (int i = 0; i < refs.length; i++) {
                if (refs[i] == null) {
                    LOGGER.warn("org.apache.aries.jndi.startup.Activator.getInitialContextFactoryServices: refs[" + i + "] is null");
                }
            }
        }

        return refs;
    }

    public static ServiceReference[] getURLObectFactoryFinderServices()
    {
        ServiceReference[] refs = urlObjectFactoryFinders.getServiceReferences();

        if (refs != null)
            Arrays.sort(refs, Utils.SERVICE_REFERENCE_COMPARATOR);

        return refs;
    }

    public static Object[] getEnvironmentAugmentors()
    {
        return environmentAugmentors.getServices();
    }

    public static Object[] getEnvironmentUnaugmentors()
    {
        return environmentUnaugmentors.getServices();
    }
}
