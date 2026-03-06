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
package org.apache.felix.scr.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.felix.scr.impl.helper.ConfigAdminTracker;
import org.apache.felix.scr.impl.logger.BundleLogger;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.apache.felix.scr.impl.logger.InternalLogger.Level;
import org.apache.felix.scr.impl.logger.ScrLogger;
import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.apache.felix.scr.impl.manager.ComponentActivator;
import org.apache.felix.scr.impl.manager.ComponentHolder;
import org.apache.felix.scr.impl.manager.DependencyManager;
import org.apache.felix.scr.impl.manager.ExtendedServiceEvent;
import org.apache.felix.scr.impl.manager.ExtendedServiceListener;
import org.apache.felix.scr.impl.manager.RegionConfigurationSupport;
import org.apache.felix.scr.impl.manager.ScrConfiguration;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.xml.XmlHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentException;

/**
 * The BundleComponentActivator is helper class to load and unload Components of
 * a single bundle. It will read information from the metadata.xml file
 * descriptors and create the corresponding managers.
 */
public class BundleComponentActivator implements ComponentActivator
{

    // global component registration
    private final ComponentRegistry m_componentRegistry;

    // The bundle owning the registered component
    private final Bundle m_bundle;

    // The bundle context owning the registered component
    private final BundleContext m_context;

    // This is a list of component holders that belong to a particular bundle
    private final List<ComponentHolder<?>> m_holders = new ArrayList<>();

    // thread acting upon configurations
    private final ComponentActorThread m_componentActor;

    // true as long as the dispose method is not called
    private final AtomicBoolean m_active = new AtomicBoolean( true );
    private final CountDownLatch m_closeLatch = new CountDownLatch( 1 );

    // the configuration
    private final ScrConfiguration m_configuration;

    private final ConfigAdminTracker configAdminTracker;

    private final Map<String, ListenerInfo> listenerMap = new HashMap<>();

    private final BundleLogger logger;

    private final ServiceReference<?> m_trueCondition;

    private static class ListenerInfo implements ServiceListener
    {
        List<ExtendedServiceListener<ExtendedServiceEvent>> listeners = new ArrayList<>();

        @Override
        public void serviceChanged(ServiceEvent event)
        {
            ExtendedServiceEvent extEvent = new ExtendedServiceEvent(event);
            List<ExtendedServiceListener<ExtendedServiceEvent>> listeners;
            synchronized ( this )
            {
                listeners = this.listeners;
            }

            for ( ExtendedServiceListener<ExtendedServiceEvent> forwardTo : listeners)
            {
                forwardTo.serviceChanged( extEvent );
            }

            if ( extEvent != null )
            {
                extEvent.activateManagers();
            }
        }

        public synchronized void add(ExtendedServiceListener<ExtendedServiceEvent> listener)
        {
            listeners = new ArrayList<>(listeners);
            listeners.add(listener);
        }

        public synchronized boolean remove(ExtendedServiceListener<ExtendedServiceEvent> listener)
        {
            listeners = new ArrayList<>(listeners);
            listeners.remove(listener);
            return listeners.isEmpty();
        }
    }

    @Override
    public void addServiceListener(String serviceFilterString,
        ExtendedServiceListener<ExtendedServiceEvent> listener)
    {
        ListenerInfo listenerInfo;
        synchronized ( listenerMap )
        {
            logger.log(Level.DEBUG, "serviceFilterString: " + serviceFilterString,
                null);
            listenerInfo = listenerMap.get( serviceFilterString );
            if ( listenerInfo == null )
            {
                listenerInfo = new ListenerInfo();
                listenerMap.put( serviceFilterString, listenerInfo );
                try
                {
                    m_context.addServiceListener( listenerInfo, serviceFilterString );
                }
                catch ( InvalidSyntaxException e )
                {
                    throw (IllegalArgumentException) new IllegalArgumentException(
                        "invalid class name filter" ).initCause( e );
                }
            }
            listenerInfo.add(listener);
        }
    }

    @Override
    public void removeServiceListener(String serviceFilterString,
        ExtendedServiceListener<ExtendedServiceEvent> listener)
    {
        synchronized ( listenerMap )
        {
            ListenerInfo listenerInfo = listenerMap.get( serviceFilterString );
            if ( listenerInfo != null )
            {
                if (listenerInfo.remove(listener)) {
                    listenerMap.remove(serviceFilterString);
                    m_context.removeServiceListener(listenerInfo);
                }
            }
        }
    }

    /**
     * Called upon starting of the bundle. This method invokes initialize() which
     * parses the metadata and creates the holders
     *
     * @param componentRegistry The <code>ComponentRegistry</code> used to
     *      register components with to ensure uniqueness of component names
     *      and to ensure configuration updates.
     * @param   context  The bundle context owning the components
     * @param serviceReference
     *
     * @throws ComponentException if any error occurrs initializing this class
     */
    public BundleComponentActivator(final ScrLogger scrLogger,
            final ComponentRegistry componentRegistry,
            final ComponentActorThread componentActor,
            final BundleContext context,
            final ScrConfiguration configuration,
            final List<ComponentMetadata> cachedComponentMetadata,
            final ServiceReference<?> trueConditiion)
    throws ComponentException
    {
        // create a logger on behalf of the bundle
        this.logger = scrLogger.bundle(context.getBundle());
        // keep the parameters for later
        m_componentRegistry = componentRegistry;
        m_componentActor = componentActor;
        m_context = context;
        m_bundle = context.getBundle();

        m_configuration = configuration;
        m_trueCondition = trueConditiion;

        logger.log(Level.DEBUG, "BundleComponentActivator : Bundle active", null);

        initialize(cachedComponentMetadata);
        ConfigAdminTracker tracker = null;
        for ( ComponentHolder<?> holder : m_holders )
        {
            if ( !holder.getComponentMetadata().isConfigurationIgnored() )
            {
                tracker = new ConfigAdminTracker( this );
                break;
            }
        }
        configAdminTracker = tracker;
    }

    /**
     * Gets the MetaData location, parses the meta data and requests the processing
     * of binder instances
     * @param cachedComponentMetadata
     *
     * @throws IllegalStateException If the bundle has already been uninstalled.
     */
    protected void initialize(List<ComponentMetadata> cachedComponentMetadata)
    {
        if (cachedComponentMetadata != null)
        {
            for (ComponentMetadata metadata : cachedComponentMetadata)
            {
                validateAndRegister(metadata);
            }
        }
        else
        {
            // Get the Metadata-Location value from the manifest
            String descriptorLocations = m_bundle.getHeaders("").get("Service-Component");
            if (descriptorLocations == null)
            {
                throw new ComponentException(
                    "Service-Component entry not found in the manifest");
            }

            logger.log(Level.DEBUG,
                "BundleComponentActivator : Descriptor locations {0}", null,
                descriptorLocations);

            // 112.4.1: The value of the the header is a comma separated list of XML entries within the Bundle
            StringTokenizer st = new StringTokenizer(descriptorLocations, ", ");
            // Tolerate wildcard overlap with explicit entries in list by remembering the URLs that have been loaded so
            // that duplicates can be skipped before attempting to re-parse the descriptors they resolve to.
            HashSet<String> haveBeenLoaded = new HashSet<>();
            while (st.hasMoreTokens())
            {
                String descriptorLocation = st.nextToken();
                URL[] descriptorURLs = findDescriptors(m_bundle, descriptorLocation);
                if (descriptorURLs.length == 0)
                {
                    if (descriptorLocation.contains("*")) {
                        // 112.4.1 The last component of each path in the Service-Component header may
                        // use wildcards so that Bundle.findEntries can be used to locate the XML
                        // document within the bundle and its fragments. For example:
                        //
                        // Service-Component: OSGI-INF/*.xml
                        //
                        // A Service-Component manifest header specified in a fragment is ignored by
                        // SCR. However, XML documents referenced by a bundle's Service-Component
                        // manifest header may be contained in attached fragments.

                        // in case of such wildcard, finding nothing does not mean an error (because it
                        // *might* be found in a fragment that is attached later.
                        logger.log(Level.TRACE,
                                "Component descriptor entry ''{0}'' with wildcard has no matches", null,
                                descriptorLocation);
                    } else {
                        // 112.4.1 If an XML document specified by the header cannot be located in the bundle and its attached
                        // fragments, SCR must log an error message with the Log Service, if present, and continue.
                        logger.log(Level.ERROR,
                            "Component descriptor entry ''{0}'' not found", null,
                            descriptorLocation);
                    }
                    continue;
                }


                // load from the descriptors
                for (URL descriptorURL : descriptorURLs)
                {
                    String externalForm = descriptorURL.toExternalForm();
                    if (!haveBeenLoaded.contains(externalForm))
                    {
                        loadDescriptor(descriptorURL);
                        haveBeenLoaded.add(externalForm);
                    }
                    else
                    {
                        logger.log(Level.DEBUG,
                                "Component descriptor entry ''{0}'' (matched by ''{1}'') has already been loaded",
                                null, descriptorURL.getPath(), descriptorLocation);
                    }
                }
            }
        }
    }

    /**
     * Called outside the constructor so that the m_managers field is completely initialized.
     * A component might possibly start a thread to enable other components, which could access m_managers
     */
    void initialEnable()
    {
        //enable all the enabled components
        for ( ComponentHolder<?> componentHolder : m_holders )
        {
            logger.log(Level.DEBUG,
                "BundleComponentActivator : May enable component holder {0}", null,
                componentHolder.getComponentMetadata().getName() );

            if ( componentHolder.getComponentMetadata().isEnabled() )
            {
                logger.log(Level.DEBUG,
                    "BundleComponentActivator :Enabling component holder {0}", null,
                    componentHolder.getComponentMetadata().getName() );

                try
                {
                    componentHolder.enableComponents( false );
                }
                catch ( Throwable t )
                {
                    // caught on unhandled RuntimeException or Error
                    // (e.g. ClassDefNotFoundError)

                    // make sure the component is properly disabled, just in case
                    try
                    {
                        componentHolder.disableComponents( false );
                    }
                    catch ( Throwable ignore )
                    {
                    }

                    logger.log(Level.ERROR,
                        "BundleComponentActivator : Unexpected failure enabling component holder {0}", t,
                        componentHolder.getComponentMetadata().getName() );
                }
            }
            else
            {
                logger.log(Level.DEBUG,
                    "BundleComponentActivator : Will not enable component holder {0}", null,
                    componentHolder.getComponentMetadata().getName() );
            }
        }
    }

    /**
     * Finds component descriptors based on descriptor location.
     *
     * @param bundle bundle to search for descriptor files
     * @param descriptorLocation descriptor location
     * @return array of descriptors or empty array if none found
     */
    static URL[] findDescriptors(final Bundle bundle, final String descriptorLocation)
    {
        if ( bundle == null || descriptorLocation == null || descriptorLocation.trim().length() == 0 )
        {
            return new URL[0];
        }

        // split pattern and path
        final int lios = descriptorLocation.lastIndexOf( "/" );
        final String path;
        final String filePattern;
        if ( lios > 0 )
        {
            path = descriptorLocation.substring( 0, lios );
            filePattern = descriptorLocation.substring( lios + 1 );
        }
        else
        {
            path = "/";
            filePattern = descriptorLocation;
        }

        // find the entries
        final Enumeration<URL> entries = bundle.findEntries( path, filePattern, false );
        if ( entries == null || !entries.hasMoreElements() )
        {
            return new URL[0];
        }

        // create the result list
        List<URL> urls = new ArrayList<>();
        while ( entries.hasMoreElements() )
        {
            urls.add( entries.nextElement() );
        }
        return urls.toArray( new URL[urls.size()] );
    }

    private void loadDescriptor(final URL descriptorURL)
    {
        // simple path for log messages
        final String descriptorLocation = descriptorURL.getPath();

        InputStream stream = null;
        try
        {
            stream = descriptorURL.openStream();

            XmlHandler handler = new XmlHandler( m_bundle, this.logger, getConfiguration().isFactoryEnabled(),
                getConfiguration().keepInstances(), m_trueCondition);
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            final SAXParser parser = factory.newSAXParser();

            parser.parse( stream, handler );

            // 112.4.2 Component descriptors may contain a single, root component element
            // or one or more component elements embedded in a larger document
            for ( ComponentMetadata metadata : handler.getComponentMetadataList() )
            {
                validateAndRegister(metadata);
            }
        }
        catch ( IOException ex )
        {
            // 112.4.1 If an XML document specified by the header cannot be located in the bundle and its attached
            // fragments, SCR must log an error message with the Log Service, if present, and continue.

            logger.log(Level.ERROR, "Problem reading descriptor entry ''{0}''", ex,
                descriptorLocation);
        }
        catch ( Exception ex )
        {
            logger.log(Level.ERROR, "General problem with descriptor entry ''{0}''",
                ex, descriptorLocation);
        }
        finally
        {
            if ( stream != null )
            {
                try
                {
                    stream.close();
                }
                catch ( IOException ignore )
                {
                }
            }
        }
    }

    void validateAndRegister(ComponentMetadata metadata)
    {
        final ComponentLogger componentLogger = logger.component(m_bundle, metadata.getImplementationClassName(), metadata.getName());
        ComponentRegistryKey key = null;
        try
        {
            // validate the component metadata
            metadata.validate();

            // check and reserve the component name (validate ensures it's never null)
            key = m_componentRegistry.checkComponentName(m_bundle, metadata.getName());

            // Request creation of the component manager
            ComponentHolder<?> holder = m_componentRegistry.createComponentHolder(this,
                metadata, componentLogger);

            // register the component after validation
            m_componentRegistry.registerComponentHolder(key, holder);
            m_holders.add(holder);

            componentLogger.log(Level.DEBUG,
                "BundleComponentActivator : ComponentHolder created.", null);

        }
        catch (Throwable t)
        {
            // There is a problem with this particular component, we'll log the error
            // and proceed to the next one
            componentLogger.log(Level.ERROR, "Cannot register component", t);

            // make sure the name is not reserved any more
            if (key != null)
            {
                m_componentRegistry.unregisterComponentHolder(key);
            }
        }
    }

    /**
    * Dispose of this component activator instance and all the component
    * managers.
    */
    void dispose(int reason)
    {
        if ( m_active.compareAndSet( true, false ) )
        {
            logger.log(Level.DEBUG,
                "BundleComponentActivator : Will destroy {0} instances",
                null, m_holders.size() );

            for ( ComponentHolder<?> holder : m_holders )
            {
                try
                {
                    holder.disposeComponents( reason );
                }
                catch ( Exception e )
                {
                    logger.log(Level.ERROR,
                        "BundleComponentActivator : Exception invalidating", e,
                        holder.getComponentMetadata() );
                }
                finally
                {
                    m_componentRegistry.unregisterComponentHolder( m_bundle, holder.getComponentMetadata().getName() );
                }

            }
            if ( configAdminTracker != null )
            {
                configAdminTracker.dispose();
            }

            logger.log(Level.DEBUG, "BundleComponentActivator : Bundle STOPPED",
                null );

            m_closeLatch.countDown();
        }
        else
        {
            try
            {
                m_closeLatch.await( m_configuration.lockTimeout(), TimeUnit.MILLISECONDS );
            }
            catch ( InterruptedException e )
            {
                //ignore interruption during concurrent shutdown.
                Thread.currentThread().interrupt();
            }
        }

    }

    /**
     * Returns <true> if this instance is active, that is if components
     * may be activated for this component. The active flag is set early
     * in the constructor indicating the activator is basically active
     * (not fully setup, though) and reset early in the process of
     * {@link #dispose(int) disposing} this instance.
     */
    @Override
    public boolean isActive()
    {
        return m_active.get();
    }

    /**
    * Returns the BundleContext
    *
    * @return the BundleContext
    */
    @Override
    public BundleContext getBundleContext()
    {
        return m_context;
    }

    @Override
    public ScrConfiguration getConfiguration()
    {
        return m_configuration;
    }

    /**
     * Implements the <code>ComponentContext.enableComponent(String)</code>
     * method by first finding the component(s) for the <code>name</code> and
     * enabling them.  The enable method will schedule activation.
     * <p>
     *
     * @param name The name of the component to enable or <code>null</code> to
     *      enable all components.
     */
    @Override
    public void enableComponent(final String name)
    {
        final List<ComponentHolder<?>> holder = getSelectedComponents( name );
        for ( ComponentHolder<?> aHolder : holder )
        {
            try
            {
                // TODO use component logger
                logger.log(Level.DEBUG, "Enabling Component {0}", null,
                    aHolder.getComponentMetadata().getName());
                aHolder.enableComponents( true );
            }
            catch ( Throwable t )
            {
                // TODO use component logger
                logger.log(Level.ERROR, "Cannot enable component {0}", t,
                    aHolder.getComponentMetadata().getName());
            }
        }
    }

    /**
     * Implements the <code>ComponentContext.disableComponent(String)</code>
     * method by first finding the component(s) for the <code>name</code> and
     * disabling them.  The disable method will schedule deactivation
     * <p>
     *
     * @param name The name of the component to disable or <code>null</code> to
     *      disable all components.
     */
    @Override
    public void disableComponent(final String name)
    {
        final List<ComponentHolder<?>> holder = getSelectedComponents( name );
        for ( ComponentHolder<?> aHolder : holder )
        {
            try
            {
                // TODO use component logger
                logger.log(Level.DEBUG, "Disabling Component {0}", null,
                    aHolder.getComponentMetadata().getName());
                aHolder.disableComponents( true );
            }
            catch ( Throwable t )
            {
                // TODO use component logger
                logger.log(Level.ERROR, "Cannot disable component {0}", t,
                    aHolder.getComponentMetadata().getName());
            }
        }
    }

    /**
     * Returns an array of {@link ComponentHolder} instances which match the
     * <code>name</code>. If the <code>name</code> is <code>null</code> an
     * array of all currently known component managers is returned. Otherwise
     * an array containing a single component manager matching the name is
     * returned if one is registered. Finally, if no component manager with the
     * given name is registered, <code>null</code> is returned.
     *
     * @param name The name of the component manager to return or
     *      <code>null</code> to return an array of all component managers.
     *
     * @return An array containing one or more component managers according
     *      to the <code>name</code> parameter or <code>null</code> if no
     *      component manager with the given name is currently registered.
     */
    List<ComponentHolder<?>> getSelectedComponents(String name)
    {
        // if all components are selected
        if ( name == null )
        {
            return m_holders;
        }

        ComponentHolder<?> componentHolder = m_componentRegistry.getComponentHolder( m_bundle, name );
        if ( componentHolder != null )
        {
            return Collections.<ComponentHolder<?>> singletonList( componentHolder );
        }

        // if the component is not known
        return Collections.emptyList();
    }

    //---------- Component ID support

    @Override
    public long registerComponentId(AbstractComponentManager<?> componentManager)
    {
        return m_componentRegistry.registerComponentId( componentManager );
    }

    @Override
    public void unregisterComponentId(AbstractComponentManager<?> componentManager)
    {
        m_componentRegistry.unregisterComponentId( componentManager.getId() );
    }

    //---------- Asynchronous Component Handling ------------------------------

    /**
     * Schedules the given <code>task</code> for asynchrounous execution or
     * synchronously runs the task if the thread is not running. If this instance
     * is {@link #isActive() not active}, the task is not executed.
     *
     * @param task The component task to execute
     */
    @Override
    public void schedule(Runnable task)
    {
        if ( isActive() )
        {
            ComponentActorThread cat = m_componentActor;
            if ( cat != null )
            {
                cat.schedule( task );
            }
            else
            {
                logger.log(Level.DEBUG,
                    "Component Actor Thread not running, calling synchronously", null);
                try
                {
                    synchronized ( this )
                    {
                        task.run();
                    }
                }
                catch ( Throwable t )
                {
                    logger.log(Level.WARN, "Unexpected problem executing task", t);
                }
            }
        }
        else
        {
            logger.log(Level.WARN,
                "BundleComponentActivator is not active; not scheduling {0}",
                null, task );
        }
    }

    @Override
    public BundleLogger getLogger() {
        return logger;
    }

    @Override
    public <T> boolean enterCreate(ServiceReference<T> serviceReference)
    {
        return m_componentRegistry.enterCreate( serviceReference );
    }

    @Override
    public <T> void leaveCreate(ServiceReference<T> serviceReference)
    {
        m_componentRegistry.leaveCreate( serviceReference );
    }

    @Override
    public <T> void missingServicePresent(ServiceReference<T> serviceReference)
    {
        m_componentRegistry.missingServicePresent( serviceReference, m_componentActor );
    }

    @Override
    public <S, T> void registerMissingDependency(DependencyManager<S, T> dependencyManager,
        ServiceReference<T> serviceReference, int trackingCount)
    {
        m_componentRegistry.registerMissingDependency( dependencyManager, serviceReference, trackingCount );
    }

    @Override
    public RegionConfigurationSupport setRegionConfigurationSupport(ServiceReference<ConfigurationAdmin> reference)
    {
        RegionConfigurationSupport rcs = m_componentRegistry.registerRegionConfigurationSupport( reference );
        if (rcs != null) {
            for ( ComponentHolder<?> holder : m_holders )
            {
                rcs.configureComponentHolder( holder );
            }
        }
        return rcs;
    }

    @Override
    public void unsetRegionConfigurationSupport(RegionConfigurationSupport rcs)
    {
        m_componentRegistry.unregisterRegionConfigurationSupport( rcs );
        // TODO anything needed?
    }

    @Override
    public void updateChangeCount() {
        this.m_componentRegistry.updateChangeCount();
    }

    @Override
    public ServiceReference<?> getTrueCondition()
    {
        return this.m_trueCondition;
    }
}
