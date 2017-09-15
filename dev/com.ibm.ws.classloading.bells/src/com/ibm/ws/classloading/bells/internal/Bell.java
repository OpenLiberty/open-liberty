/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.bells.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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
@Component(name = "com.ibm.ws.classloading.bell", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Bell implements LibraryChangeListener {
    private static final TraceComponent tc = Tr.register(Bell.class);

    /**
     * Holds the meta-inf/services configuration entry and the name of the implementation class for the service
     */
    private static final class ServiceInfo {
        final ArtifactEntry providerConfigFile;
        final String implClass;

        ServiceInfo(final ArtifactEntry providerConfigFile, final String implClass) {
            this.providerConfigFile = providerConfigFile;
            this.implClass = implClass;
        }

        @Override
        public String toString() {
            return "[" + implClass + "] from: [" + providerConfigFile.getResource() + "]";
        }
    }

    private static final String SERVICE_ATT = "service";
    private final ReentrantLock trackerLock = new ReentrantLock();
    private ServiceTracker<Library, List<ServiceRegistration<?>>> tracker;

    private Library library;

    private ComponentContext componentContext;
    private Map<String, Object> config;

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
     * Unregisters all OSGi services associated with this bell
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
     * @param context the bundle context
     * @param executor the executor service
     * @param config the configuration settings
     */
    void update() {
        final BundleContext context = componentContext.getBundleContext();
        // determine the service filter to use for discovering the Library service this bell is for
        String libraryRef = library.id();
        // it is unclear if only looking at the id would work here.
        // other examples in classloading use both id and service.pid to look up so doing the same here.
        String libraryStatusFilter = String.format("(&(objectClass=%s)(|(id=%s)(service.pid=%s)))", Library.class.getName(), libraryRef, libraryRef);
        Filter filter;
        try {
            filter = context.createFilter(libraryStatusFilter);
        } catch (InvalidSyntaxException e) {
            // should not happen, but blow up if it does
            throw new RuntimeException(e);
        }
        final Set<String> serviceNames = getServiceNames((String[]) config.get(SERVICE_ATT));
        // create a tracker that will register the services once the library becomes available
        ServiceTracker<Library, List<ServiceRegistration<?>>> newTracker = null;
        newTracker = new ServiceTracker<Library, List<ServiceRegistration<?>>>(context, filter, new ServiceTrackerCustomizer<Library, List<ServiceRegistration<?>>>() {
            @Override
            public List<ServiceRegistration<?>> addingService(ServiceReference<Library> libraryRef) {
                Library library = context.getService(libraryRef);
                // Got the library not regisrter the services.
                // The list of registrations is returned so we don't have to store them ourselves.
                return registerLibraryServices(library, serviceNames);
            }

            @Override
            public void modifiedService(ServiceReference<Library> libraryRef, List<ServiceRegistration<?>> metaInfServices) {
                // don't care
            }

            @Override
            @FFDCIgnore(IllegalStateException.class)
            public void removedService(ServiceReference<Library> libraryRef, List<ServiceRegistration<?>> metaInfServices) {
                // THe library is going away; need to unregister the services
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

    private static Set<String> getServiceNames(String[] configuredServices) {
        if (configuredServices == null || configuredServices.length == 0) {
            return Collections.emptySet();
        }
        return new HashSet<String>(Arrays.asList(configuredServices));
    }

    /**
     * 1) Retrieve all the meta-inf services entries from the library
     * 2) For each line in each services file, create an instance of the specified implementation class
     * 3) Use the library's gateway bundle context to register each instance in the service registry with the following properties:
     * - implementation.class=com.acme.whatever.MyImpl
     * - exported.from=LibraryId
     * 4) Store the service registrations in a collection, indexed by library (by library instance not library id, since library ID can be null when we remove it)
     */
    private List<ServiceRegistration<?>> registerLibraryServices(final Library library, Set<String> serviceNames) {
        final BundleContext context = getGatewayBundleContext(library);
        if (context == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "registerLibraryServices: can't find bundle ", library.id());
            }
            return Collections.emptyList();
        }

        final List<ServiceInfo> serviceInfos = new LinkedList<ServiceInfo>();
        for (final ArtifactContainer ac : library.getContainers()) {
            final ArtifactEntry servicesFolder = ac.getEntry("META-INF/services");
            if (servicesFolder == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No META-INF services folder in the container: ", ac);
                }
                continue;
            }
            serviceInfos.addAll(getListOfServicesForContainer(servicesFolder.convertToContainer(), library, serviceNames));
        }

        final List<ServiceRegistration<?>> libServices;
        if (serviceInfos.size() == 0) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
                Tr.warning(tc, "bell.no.services.found", library.id());
            }
            libServices = Collections.emptyList();
        } else {
            libServices = registerServices(serviceInfos, library, context);
        }
        return libServices;
    }

    private BundleContext getGatewayBundleContext(Library library) {
        // TODO reflection is used here because there is no external way outside of classloading
        // bundle to get a hold of the gateway bundle.
        // Perhaps if LibertyLoader implemented BundleReference, but that may cause issues with Aries jndi code
        ClassLoader loader = library.getClassLoader();
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

    private static BufferedReader createReader(final ArtifactEntry providerConfigFile) throws IOException {
        final InputStream is = providerConfigFile.getInputStream();
        final InputStreamReader input = new InputStreamReader(is, Charset.forName("UTF8"));
        return new BufferedReader(input) {
            private static final String COMMENT_CHAR = "#";

            @Override
            public void close() throws IOException {
                super.close();
                input.close();
                is.close();
            }

            @Override
            public String readLine() throws IOException {
                String line;
                do {
                    line = super.readLine();
                    if (line == null) {
                        return line;
                    }
                    // strip comments
                    final int startOfComment = line.indexOf(COMMENT_CHAR);
                    if (startOfComment != -1) {
                        line = line.substring(0, startOfComment);
                    }
                    line = line.trim();
                } while (line.isEmpty());

                return line;
            }
        };
    }

    private static List<ServiceInfo> getListOfServicesForContainer(final ArtifactContainer servicesFolder, final Library library, Set<String> serviceNames) {
        final List<ServiceInfo> serviceInfos = new LinkedList<ServiceInfo>();
        if (serviceNames.isEmpty()) {
            // just exposing all mete-inf services
            for (ArtifactEntry providerConfigFile : servicesFolder) {
                getServiceInfos(providerConfigFile, providerConfigFile.getName(), library, serviceInfos);
            }
        } else {
            // only look for services that have been specified
            for (String serviceName : serviceNames) {
                ArtifactEntry providerConfigFile = servicesFolder.getEntry(serviceName);
                getServiceInfos(providerConfigFile, serviceName, library, serviceInfos);
            }
        }
        return serviceInfos;
    }

    private static void getServiceInfos(ArtifactEntry providerConfigFile, String serviceName, Library library, List<ServiceInfo> serviceInfos) {
        if (providerConfigFile == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "bell.no.services.config", serviceName, library.id());
            }
        } else {
            try {
                final BufferedReader reader = createReader(providerConfigFile);
                try {
                    String implClass;
                    while ((implClass = reader.readLine()) != null) {
                        serviceInfos.add(new ServiceInfo(providerConfigFile, implClass));
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

    private static List<ServiceRegistration<?>> registerServices(final List<ServiceInfo> serviceInfos, final Library library, final BundleContext context) {
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
            final Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put("implementation.class", serviceInfo.implClass);
            properties.put("exported.from", library.id());

            final ServiceRegistration<?> reg = context.registerService(interfaceName, createServiceFactory(serviceInfo, library, fileUrl), properties);
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
                        Object service = context.getService(reg.getReference());
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
    private static PrototypeServiceFactory<?> createServiceFactory(final ServiceInfo serviceInfo, final Library library, final URL fileUrl) {
        // A prototype factory is used in case the consumer wants to get multiple instances of the service object.
        // A typical user of the service will simply use the R5 ways to get the service which will fall back to
        // behaving like a normal ServiceFactory.
        return new PrototypeServiceFactory() {
            @Override
            public Object getService(Bundle bundle, ServiceRegistration registration) {
                // Not the following methods will produce messages if something goes wrong
                Class<?> serviceType = findClass(serviceInfo.implClass, library, fileUrl);
                if (serviceType != null) {
                    return createService(serviceType, library.id(), fileUrl);
                }
                // something went wrong have to return null.
                // TODO may want to throw a ServiceException with our message here so we don't
                // get a generic message from the framework when null is returned.
                return null;
            }

            @Override
            public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {}
        };
    }

    /**
     * Try to load and create an instance of the {@code implClass} service.
     *
     * @return the service instance if successful, else {@code null}.
     */
    private static Class<?> findClass(final String implClass, final Library library, final URL fileUrl) {
        final ClassLoader containerClassLoader = library.getClassLoader();
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

    private static Object createService(final Class<?> serviceType, final String libID, final URL fileUrl) {
        Object service = null;
        try {
            service = serviceType.newInstance();
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
