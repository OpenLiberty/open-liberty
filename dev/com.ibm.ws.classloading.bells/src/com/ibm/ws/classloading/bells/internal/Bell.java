/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.bells.internal;

import static com.ibm.ws.classloading.internal.ClassLoadingConstants.GLOBAL_SHARED_LIBRARY_ID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.classloading.MetaInfServicesProvider;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.library.spi.SpiLibrary;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.library.LibraryChangeListener;

/**
 * A bell provides additional configuration for the behavior of a library.
 * Currently the only behavior that is supported is the registration of
 * meta-inf/services which are specified in the library as OSGi services.
 * This allows such services to be consumed by bundles in liberty features.
 */
@Component(name = "com.ibm.ws.classloading.bell", configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { Constants.SERVICE_RANKING + ":Integer=" + Integer.MIN_VALUE })
public class Bell implements LibraryChangeListener {

    private static final TraceComponent tc = Tr.register(Bell.class);
    private static final char COMMENT_CHAR = '#';

    /**
     * Holds the meta-inf/services configuration entry and the name of the implementation class for the service
     */
    private static final class ServiceInfo {
        final ArtifactEntry providerConfigFile;
        final String implClass;
        final Hashtable<String, String> props;

        ServiceInfo(final ArtifactEntry providerConfigFile, final String implClass, final Hashtable<String, String> props) {
            this.providerConfigFile = providerConfigFile;
            this.implClass = implClass;
            this.props = props;
        }

        @Override
        public String toString() {
            return "[" + implClass + "] from: [" + providerConfigFile.getResource() + "]";
        }
    }

    private final ReentrantLock trackerLock = new ReentrantLock();
    private ServiceTracker<Library, List<ServiceRegistration<?>>> tracker;

    private Library library;

    private ComponentContext componentContext;

    private Map<String, Object> config;

    private static final String SERVICE_ATT = "service";
    private static final String SPI_VISIBILITY_ATT = "spiVisibility";
    private static final String PROPERTIES_ATT = "properties";

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        componentContext = cc;
        config = props;
        update();
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        if (!FrameworkState.isStopping())
            unregister();
    }

    /**
     * Unregister all OSGi services associated with this bell
     */
    void unregister() {
        trackerLock.lock();
        try {
            if (tracker != null) {
                // simply closing the tracker causes the services to be unregistered
                tracker.close();
                tracker = null;
            }
        } finally {
            trackerLock.unlock();
        }
    }

    /**
     * Configures this bell with a specific library and a possible set of service names
     *
     * @param context  the bundle context
     * @param executor the executor service
     * @param config   the configuration settings
     */
    void update() {
        final BundleContext context = componentContext.getBundleContext();
        final String libraryRef = library.id();

        // Determine the service filter to use for discovering the Library service this bell is for
        // it is unclear if only looking at the id would work here. Other examples in classloading use
        // both id and service.pid to look up so doing the same here.
        String libraryStatusFilter = String.format("(&(objectClass=%s)(|(id=%s)(service.pid=%s)))", Library.class.getName(), libraryRef, libraryRef);
        Filter filter;
        try {
            filter = context.createFilter(libraryStatusFilter);
        } catch (InvalidSyntaxException e) {
            // should not happen, but blow up if it does
            throw new RuntimeException(e);
        }

        final Set<String> serviceNames = getServiceNames((String[]) config.get(SERVICE_ATT));
        final boolean spiVisibility = getSpiVisibility((Boolean) config.get(SPI_VISIBILITY_ATT), libraryRef);
        final Map<String, String> properties = getProperties(config); // PROPERTIES_ATT

        // create a tracker that will register the services once the library becomes available
        ServiceTracker<Library, List<ServiceRegistration<?>>> newTracker = null;
        newTracker = new ServiceTracker<Library, List<ServiceRegistration<?>>>(context, filter, new ServiceTrackerCustomizer<Library, List<ServiceRegistration<?>>>() {
            @Override
            public List<ServiceRegistration<?>> addingService(ServiceReference<Library> libraryRef) {
                Library library = context.getService(libraryRef);
                // Got the library now register the services.
                // The list of registrations is returned so we don't have to store them ourselves.
                return registerLibraryServices(library, serviceNames, spiVisibility, properties);
            }

            @Override
            public void modifiedService(ServiceReference<Library> libraryRef, List<ServiceRegistration<?>> metaInfServices) {
                // don't care
            }

            @Override
            @FFDCIgnore(IllegalStateException.class)
            public void removedService(ServiceReference<Library> libraryRef, List<ServiceRegistration<?>> metaInfServices) {
                // The library is going away; need to unregister the services
                for (ServiceRegistration<?> registration : metaInfServices) {
                    try {
                        registration.unregister();
                    } catch (IllegalStateException e) {
                        // ignore; already unregistered
                    }
                }
                context.ungetService(libraryRef);
            }
        });

        trackerLock.lock();
        try {
            if (tracker != null) {
                // close the existing tracker so we unregister existing services
                tracker.close();
            }
            // store and open the new tracker so we can register the configured services.
            tracker = newTracker;
            tracker.open();
        } finally {
            trackerLock.unlock();
        }
    }

    private Set<String> getServiceNames(String[] configuredServices) {
        if (configuredServices == null || configuredServices.length == 0) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(configuredServices)));
    }

    @SuppressWarnings("restriction")
    boolean getSpiVisibility(Boolean configuredSpiVisibility, String libraryId) {
        boolean spiVisibility = Boolean.TRUE.equals(configuredSpiVisibility);
        if (spiVisibility) {
            if (GLOBAL_SHARED_LIBRARY_ID.equals(libraryId)) {
                // The liberty "global" library is intended for use by EE applications, not OSGi services
                // TODO Should we establish this restriction for all BELL configurations?
                Tr.warning(tc, "bell.spi.visibility.disabled.libref.global");
                spiVisibility = false;
            } else {
                Tr.info(tc, "bell.spi.visibility.enabled", libraryId);
            }
        }
        return spiVisibility;
    }

    private static final String propKeyPrefix = PROPERTIES_ATT + ".0.";
    private static final int propKeyPrefixLen = propKeyPrefix.length();

    /**
     * Collect a mapping of property names to values from the BELL <code><properties/></code>
     * configuration.
     *
     * @return a map containing zero or more BELL properties, otherwise return null whenever
     *         the configuration lacks a properties element.
     */
    private Map<String, String> getProperties(Map<String, Object> configuration) {
        Map<String, String> pMap = null;
        if (configuration.get(propKeyPrefix + "config.referenceType") != null) {
            pMap = new HashMap<String, String>();
            for (String key : configuration.keySet()) {
                if (key.startsWith(propKeyPrefix) && !!!key.endsWith("config.referenceType")) {
                    Object pValue = configuration.get(key);
                    if (pValue instanceof String) {
                        String pName = key.substring(propKeyPrefixLen);
                        pMap.put(pName, (String) pValue);
                    }
                }
            }
        }
        return (pMap == null) ? null : Collections.unmodifiableMap(pMap);
    }

    /**
     * 1) Retrieve all the meta-inf services entries from the library
     * 2) For each line in each services file, create an instance of the specified implementation class
     * 3) Use the library's gateway bundle context to register each instance in the service registry with the following properties:
     * - implementation.class=com.acme.whatever.MyImpl
     * - exported.from=LibraryId
     * 4) Store the service registrations in a collection, indexed by library (by library instance not library id, since library ID can be null when we remove it)
     */
    private List<ServiceRegistration<?>> registerLibraryServices(final Library library, Set<String> serviceNames, boolean spiVisibility, Map<String, String> properties) {
        final BundleContext context = getGatewayBundleContext(library, spiVisibility);
        if (context == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "registerLibraryServices: can't find bundle ", library.id());
            }
            return Collections.emptyList();
        }

        Set<String> servicesNotFound = serviceNames == null || serviceNames.isEmpty() ? Collections.emptySet() : new TreeSet<String>(serviceNames);

        final List<ServiceInfo> serviceInfos = new LinkedList<ServiceInfo>();
        for (final ArtifactContainer ac : library.getContainers()) {
            final ArtifactEntry servicesFolder = ac.getEntry("META-INF/services");
            if (servicesFolder == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No META-INF services folder in the container: ", ac);
                }
                continue;
            }
            serviceInfos.addAll(getListOfServicesForContainer(servicesFolder.convertToContainer(), library, serviceNames, servicesNotFound));
        }

        for (String serviceName : servicesNotFound)
            Tr.warning(tc, "bell.no.services.config", serviceName, library.id());

        final List<ServiceRegistration<?>> libServices;
        if (serviceInfos.size() == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                Tr.warning(tc, "bell.no.services.found", library.id());
            }
            libServices = Collections.emptyList();
        } else {
            libServices = registerServices(serviceInfos, library, context, spiVisibility, properties);
        }
        return libServices;
    }

    private BundleContext getGatewayBundleContext(Library library, boolean spiVisibility) {
        // TODO reflection is used here because there is no external way outside of classloading
        // bundle to get a hold of the gateway bundle.
        // Perhaps if LibertyLoader implemented BundleReference, but that may cause issues with Aries jndi code
        ClassLoader loader = Bell.getClassLoader(library, spiVisibility);
        Class<?> loaderClass = loader.getClass();
        while (!!!"LibertyLoader".equals(loaderClass.getSimpleName()) && loaderClass.getSuperclass() != null) {
            loaderClass = loaderClass.getSuperclass();
        }
        try {
            Method getBundle = loaderClass.getDeclaredMethod("getBundle");
            getBundle.setAccessible(true);
            Bundle bundle = (Bundle) getBundle.invoke(loader);
            return bundle.getBundleContext();
        } catch (InvocationTargetException e) {
            // auto FFDC
            Throwable t = e.getTargetException();
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException(t);
        } catch (Exception e) {
            // auto FFDC
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    static ClassLoader getClassLoader(Library library, boolean spiVisibility) {
        if (spiVisibility) {
            return ((SpiLibrary) library).getSpiClassLoader("BELL");
        }
        return library.getClassLoader();
    }

    private static BufferedReader createReader(final ArtifactEntry providerConfigFile) throws IOException {
        final InputStream is = providerConfigFile.getInputStream();
        final InputStreamReader input = new InputStreamReader(is, StandardCharsets.UTF_8);
        return new BufferedReader(input);
    }

    private static List<ServiceInfo> getListOfServicesForContainer(final ArtifactContainer servicesFolder, final Library library,
                                                                   Set<String> serviceNames, Set<String> servicesNotFound) {
        final List<ServiceInfo> serviceInfos = new LinkedList<ServiceInfo>();
        if (serviceNames.isEmpty()) {
            // just exposing all meta-inf services
            for (ArtifactEntry providerConfigFile : servicesFolder) {
                getServiceInfos(providerConfigFile, providerConfigFile.getName(), servicesNotFound, library, serviceInfos);
            }
        } else {
            // only look for services that have been specified
            for (String serviceName : serviceNames) {
                ArtifactEntry providerConfigFile = servicesFolder.getEntry(serviceName);
                getServiceInfos(providerConfigFile, serviceName, servicesNotFound, library, serviceInfos);
            }
        }
        return serviceInfos;
    }

    private static void getServiceInfos(ArtifactEntry providerConfigFile, String serviceName, Set<String> servicesNotFound,
                                        Library library, List<ServiceInfo> serviceInfos) {
        if (providerConfigFile != null) {
            try {
                final BufferedReader reader = createReader(providerConfigFile);
                try {
                    String line;
                    Hashtable<String, String> props = new Hashtable<String, String>();
                    while ((line = reader.readLine()) != null) {
                        int index = line.indexOf(COMMENT_CHAR);
                        String implClass;
                        if (index != -1) {
                            String propStr = line.substring(index + 1);
                            implClass = line.substring(0, index).trim();
                            if (implClass.length() == 0) {
                                String[] prop = propStr.split("=");
                                if (prop.length == 2) {
                                    props.put(prop[0].trim(), prop[1].trim());
                                }
                            }
                        } else {
                            implClass = line.trim();
                        }

                        if (implClass.length() > 0) {
                            serviceInfos.add(new ServiceInfo(providerConfigFile, implClass, props));
                            servicesNotFound.remove(serviceName);
                            props = new Hashtable<String, String>();
                        }
                    }
                } finally {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                    }
                }
            } catch (final IOException e) {
                final URL fileUrl = providerConfigFile.getResource();
                if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                    Tr.warning(tc, "bell.io.error", fileUrl, library.id(), e);
                }
            } catch (final Throwable e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "addLibraryServices: error: ", e);
                }
            }
        }
    }

    private static List<ServiceRegistration<?>> registerServices(final List<ServiceInfo> serviceInfos, final Library library, final BundleContext context,
                                                                 final boolean spiVisibility, final Map<String, String> properties) {
        final List<ServiceRegistration<?>> registeredServices = new LinkedList<ServiceRegistration<?>>();
        // For each SerivceInfo register the service.
        // Note that no validation is done here to ensure the implementation class can be loaded
        // or that a service object can be successfully created.
        // That is left to be verified when the service factory attempts to create the service object.
        for (final ServiceInfo serviceInfo : serviceInfos) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Registering service: ", serviceInfo);
            }
            final URL fileUrl = serviceInfo.providerConfigFile.getResource();
            final String interfaceName = serviceInfo.providerConfigFile.getName();

            // Not entirely sure what these properties would be used for, but ...
            // they are currently used by the FAT tests to force all bell services to be eagerly created.
            final Hashtable<String, Object> serviceProperties = new Hashtable<String, Object>();
            serviceProperties.putAll(serviceInfo.props);
            serviceProperties.put("implementation.class", serviceInfo.implClass);
            serviceProperties.put("exported.from", library.id());

            final ServiceRegistration<?> reg = context.registerService(interfaceName, createServiceFactory(serviceInfo, library, fileUrl, spiVisibility, properties),
                                                                       serviceProperties);
            if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                Tr.info(tc, "bell.service.name", library.id(), fileUrl, serviceInfo.implClass);
            }
            registeredServices.add(reg);

            // Register interface for collaboration with classloading service to make META-INF/services
            // providers available on thread context class loader for ServiceLoader.
            MetaInfServicesProvider provider = new MetaInfServicesProvider() {
                private final AtomicReference<Class<?>> implClassRef = new AtomicReference<Class<?>>();

                @Override
                public Class<?> getProviderImplClass() {
                    Class<?> implClass = implClassRef.get();
                    if (implClass == null) {
                        Object service = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                            @Override
                            public Object run() {
                                return context.getService(reg.getReference());
                            }
                        });
                        if (service != null)
                            implClassRef.set(implClass = service.getClass());
                    }
                    return implClass;
                }
            };
            Dictionary<String, Object> metaInfProps = new Hashtable<String, Object>();
            metaInfProps.put("implementation.class", serviceInfo.implClass);
            metaInfProps.put("file.path", serviceInfo.providerConfigFile.getPath().substring(1)); // exclude initial / of /META-INF/services/...
            metaInfProps.put("file.url", fileUrl);
            ServiceRegistration<MetaInfServicesProvider> metaInfReg = context.registerService(MetaInfServicesProvider.class, provider, metaInfProps);
            registeredServices.add(metaInfReg);
        }

        return registeredServices;
    }

    @SuppressWarnings("rawtypes")
    private static PrototypeServiceFactory<?> createServiceFactory(final ServiceInfo serviceInfo, final Library library, final URL fileUrl,
                                                                   final boolean spiVisibility, final Map<String, String> properties) {
        // A prototype factory is used in case the consumer wants to get multiple instances of the service object.
        // A typical user of the service will simply use the R5 ways to get the service which will fall back to
        // behaving like a normal ServiceFactory.
        return new PrototypeServiceFactory() {
            @Override
            public Object getService(Bundle bundle, ServiceRegistration registration) {
                if (library.id() == null) {
                    // this is a case where the library has been deleted but we have not
                    // gotten to unregister the service factory yet;
                    // just return null instead of failing out here with exceptions
                    return null;
                }
                // Note the following methods will produce messages if something goes wrong
                Class<?> serviceType = findClass(serviceInfo.implClass, library, fileUrl, spiVisibility);
                if (serviceType != null) {
                    return createService(serviceType, library.id(), fileUrl, properties);
                }
                // something went wrong have to return null.
                // TODO may want to throw a ServiceException with our message here so we don't
                // get a generic message from the framework when null is returned.
                return null;
            }

            @Override
            public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
            }
        };
    }

    /**
     * Try to load and create an instance of the {@code implClass} service.
     *
     * @return the service instance if successful, else {@code null}.
     */
    private static Class<?> findClass(final String implClass, final Library library, final URL fileUrl, final boolean spiVisibility) {
        final ClassLoader containerClassLoader = Bell.getClassLoader(library, spiVisibility);
        final String libID = library.id();
        Class<?> service = null;
        try {
            service = containerClassLoader.loadClass(implClass);
        } catch (final NoClassDefFoundError e) {
            // Happens if any class referenced by the implementation cannot be found
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "bell.no.inter", implClass, fileUrl, libID, e);
            }
        } catch (final ClassNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "bell.no.impl", implClass, fileUrl, libID);
            }
        } catch (final Throwable e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "bell.error.ctor", implClass, fileUrl, libID, e);
            }
        }
        return service;
    }

    private static Object createService(final Class<?> serviceType, final String libID, final URL fileUrl, final Map<String, String> properties) {
        Object service = null;
        Constructor<?> singleArgCtor;
        Method updateMethod;
        try {
            if (properties == null) {
                service = serviceType.newInstance();
            } else if ((singleArgCtor = getConstructor(serviceType, java.util.Map.class)) != null) {
                service = singleArgCtor.newInstance(properties);
            } else if ((updateMethod = getMethod(serviceType, "updateBell", java.util.Map.class)) != null) {
                service = serviceType.newInstance();
                updateMethod.invoke(service, properties);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                    Tr.warning(tc, "bell.missing.property.injection.methods", serviceType.getName(), fileUrl, libID);
                }
                service = serviceType.newInstance();
            }
        } catch (final IllegalAccessException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "bell.illegal.access", serviceType.getName(), fileUrl, libID);
            }
        } catch (final InstantiationException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "bell.not.constructible", serviceType.getName(), fileUrl, libID);
            }
        } catch (final Throwable e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "bell.error.ctor", serviceType.getName(), fileUrl, libID, e);
            }
        }
        return service;
    }

    @FFDCIgnore(NoSuchMethodException.class)
    private static Constructor<?> getConstructor(final Class<?> serviceType, Class<?> parmType) {
        Throwable t;
        try {
            return serviceType.getConstructor(parmType);
        } catch (NoSuchMethodException e) {
            t = e; // ignore
        } catch (NullPointerException | SecurityException e) {
            t = e; // auto FFDC
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "An exception occurred getting ctor(" + parmType.getSimpleName() + ") for service type " + serviceType.getName() + ": ", t);
        }
        return null;
    }

    @FFDCIgnore(NoSuchMethodException.class)
    private static Method getMethod(final Class<?> serviceType, String methodName, Class<?> parmType) {
        Throwable t;
        try {
            return serviceType.getMethod(methodName, parmType);
        } catch (NoSuchMethodException e) {
            t = e; // ignore
        } catch (NullPointerException | SecurityException e) {
            t = e; // auto FFDC
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "An exception occurred getting method " + methodName + "(" + parmType.getSimpleName() + ") for service type " + serviceType.getName() + ": ", t);
        }
        return null;
    }

    @Reference(name = "library", target = "(id=unbound)")
    protected void setLibrary(Library lib) {
        library = lib;
    }

    protected void unsetLibrary(Library lib) {
        library = null;
    }

    @Override
    public void libraryNotification() {
        update();
    }
}
