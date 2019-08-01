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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.felix.scr.impl.config.ScrConfigurationImpl;
import org.apache.felix.scr.impl.inject.ClassUtils;
import org.apache.felix.scr.impl.logger.ScrLogger;
import org.apache.felix.scr.impl.manager.ComponentHolder;
import org.apache.felix.scr.impl.manager.ScrConfiguration;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.MetadataStoreHelper.MetaDataReader;
import org.apache.felix.scr.impl.metadata.MetadataStoreHelper.MetaDataWriter;
import org.apache.felix.scr.impl.runtime.ServiceComponentRuntimeImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.log.LogService;

/**
 * This activator is used to cover requirement described in section 112.8.1 @@ -27,14
 * 37,202 @@ in active bundles.
 *
 */
public class Activator extends AbstractExtender
{
    // Our configuration from bundle context properties and Config Admin
    private final ScrConfigurationImpl m_configuration;

    private BundleContext m_context;

    //Either this bundle's context or the framework bundle context, depending on the globalExtender setting.
    private BundleContext m_globalContext;

    // this bundle
    private Bundle m_bundle;

    // the log service to log messages to
    private volatile ScrLogger logger;

    // map of BundleComponentActivator instances per Bundle indexed by Bundle id
    private Map<Long, BundleComponentActivator> m_componentBundles;

    // registry of managed component
    private ComponentRegistry m_componentRegistry;

    //  thread acting upon configurations
    private ComponentActorThread m_componentActor;

    private ServiceRegistration<ServiceComponentRuntime> m_runtime_reg;

    private ComponentCommands m_componentCommands;

    private ConcurrentMap<Long, List<ComponentMetadata>> m_componentMetadataStore;

    public Activator()
    {
        m_configuration = new ScrConfigurationImpl( this );
    }

    /**
     * Registers this instance as a (synchronous) bundle listener and loads the
     * components of already registered bundles.
     *
     * @param context The <code>BundleContext</code> of the SCR implementation
     *      bundle.
     */
    @Override
    public void start(final BundleContext context) throws Exception
    {
        m_context = context;
        m_bundle = context.getBundle();
        // require the log service
        logger = new ScrLogger(m_configuration, m_context);
        // set bundle context for PackageAdmin tracker
        ClassUtils.setBundleContext( context );
        // get the configuration
        m_configuration.start( m_context ); //this will call restart, which calls super.start.
    }

    public void restart(boolean globalExtender)
    {
        m_componentMetadataStore = load(m_context, logger,
            m_configuration.cacheMetadata());
        BundleContext context = m_globalContext;
        if ( globalExtender )
        {
            m_globalContext = m_context.getBundle( Constants.SYSTEM_BUNDLE_LOCATION ).getBundleContext();
        }
        else
        {
            m_globalContext = m_context;
        }
        if ( ClassUtils.m_packageAdmin != null )
        {
            logger.log(LogService.LOG_INFO, "Stopping to restart with new globalExtender setting: {0}", null, globalExtender);

            //this really is a restart, not the initial start
            // the initial start where m_globalContext is null should skip this as m_packageAdmin should not yet be set.
            try
            {
                super.stop( context );
            }
            catch ( final Exception e )
            {
                // logger might be null
                if ( logger != null )
                {
                    logger.log(LogService.LOG_ERROR,  "Exception stopping during restart", e);
                }
            }
            // reinstantiate logger
            logger = new ScrLogger(m_configuration, m_context);
        }
        try
        {
            logger.log(LogService.LOG_INFO, "Starting with globalExtender setting: {0}", null, globalExtender);

            super.start( m_globalContext );
        }
        catch ( final Exception e )
        {
            logger.log(LogService.LOG_ERROR,  "Exception starting during restart", e);
        }

    }

    protected ComponentRegistry createComponentRegistry(final ScrConfiguration scrConfiguration, final ScrLogger logger) {
    	return new ComponentRegistry( this.m_configuration, this.logger );
    }

    @Override
    protected void doStart() throws Exception
    {

        // prepare component registry
        m_componentBundles = new HashMap<>();
        m_componentRegistry = createComponentRegistry( this.m_configuration, this.logger );

        final ServiceComponentRuntimeImpl runtime = new ServiceComponentRuntimeImpl( m_globalContext, m_componentRegistry );
        m_runtime_reg = m_context.registerService( ServiceComponentRuntime.class,
                runtime,
                m_componentRegistry.getServiceRegistrationProperties() );
        m_componentRegistry.setRegistration(m_runtime_reg);

        // log SCR startup
        logger.log( LogService.LOG_INFO, " Version = {0}",
            null, m_bundle.getVersion().toString() );

        // create and start the component actor
        m_componentActor = new ComponentActorThread( this.logger );
        Thread t = new Thread( m_componentActor, "SCR Component Actor" );
        t.setDaemon( true );
        t.start();

        super.doStart();

        m_componentCommands = new ComponentCommands(m_context, runtime, m_configuration);
        m_componentCommands.register();
        m_componentCommands.updateProvideScrInfoService(m_configuration.infoAsService());
        m_configuration.setScrCommand(m_componentCommands);
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        super.stop( context );
        m_configuration.stop();
        store(m_componentMetadataStore, context, logger, m_configuration.cacheMetadata());
    }

    @Override
    public void bundleChanged(BundleEvent event)
    {
        super.bundleChanged(event);
        if (event.getType() == BundleEvent.UPDATED
            || event.getType() == BundleEvent.UNINSTALLED)
        {
            m_componentMetadataStore.remove(event.getBundle().getBundleId());
        }
    }

    private static ConcurrentMap<Long, List<ComponentMetadata>> load(
        BundleContext context,
        ScrLogger logger, boolean loadFromCache)
    {
        try
        {
            ConcurrentMap<Long, List<ComponentMetadata>> result = new ConcurrentHashMap<>();
            if (!loadFromCache)
            {
                return result;
            }
            BundleContext systemContext = context.getBundle(
                Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();

            File store = context.getDataFile("componentMetadataStore");
            if (store.isFile())
            {
                try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(new FileInputStream(store))))
                {
                    MetaDataReader metaDataReader = new MetaDataReader();
                    if (!metaDataReader.isVersionSupported(in))
                    {
                        // the stored version is not compatible
                        return result;
                    }
                    int numStrings = in.readInt();
                    for (int i = 0; i < numStrings; i++)
                    {
                        metaDataReader.readIndexedString(in);
                    }
                    int numBundles = in.readInt();
                    for (int i = 0; i < numBundles; i++)
                    {
                        // Read all the components for the ID even if the bundle does not exist;
                        long bundleId = in.readLong();
                        int numComponents = in.readInt();
                        long lastModified = in.readLong();
                        List<ComponentMetadata> components = new ArrayList<>(
                            numComponents);
                        for (int j = 0; j < numComponents; j++)
                        {
                            components.add(ComponentMetadata.load(in, metaDataReader));
                        }
                        // Check with system context by ID to avoid hooks hiding;
                        Bundle b = systemContext.getBundle(bundleId);
                        if (b != null)
                        {
                            if (lastModified == b.getLastModified())
                            {
                                result.put(bundleId, components);
                            }
                        }
                    }
                }
                catch (IOException e)
                {
                    logger.log(LogService.LOG_WARNING,
                        "Error loading component metadata cache.", e);
                }
            }
            return result;
        }
        catch (RuntimeException re)
        {
            // avoid failing all of SCR start on cache load bug
            logger.log(LogService.LOG_ERROR,
                "Error loading component metadata cache.", re);
            return new ConcurrentHashMap<>();
        }

    }

    private static void store(Map<Long, List<ComponentMetadata>> componentsMap,
        BundleContext context, ScrLogger logger, boolean storeCache)
    {
        if (!storeCache)
        {
            return;
        }
        BundleContext systemContext = context.getBundle(
            Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
        File store = context.getDataFile("componentMetadataStore");
        try (DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(store))))
        {
            MetaDataWriter metaDataWriter = new MetaDataWriter();
            metaDataWriter.writeVersion(out);

            Set<String> allStrings = new HashSet<>();
            for (List<ComponentMetadata> components : componentsMap.values())
            {
                for (ComponentMetadata component : components)
                {
                    component.collectStrings(allStrings);
                }
            }
            // remove possible null
            allStrings.remove(null);
            out.writeInt(allStrings.size());
            for (String s : allStrings)
            {
                metaDataWriter.writeIndexedString(s, out);
            }
            out.writeInt(componentsMap.size());
            for (Entry<Long, List<ComponentMetadata>> entry : componentsMap.entrySet())
            {
                out.writeLong(entry.getKey());
                out.writeInt(entry.getValue().size());
                Bundle b = systemContext.getBundle(entry.getKey());
                out.writeLong(b == null ? -1 : b.getLastModified());
                for (ComponentMetadata component : entry.getValue())
                {
                    component.store(out, metaDataWriter);
                }
            }
        }
        catch (IOException e)
        {
            logger.log(LogService.LOG_WARNING, "Error storing component metadata cache.",
                e);
        }
    }

    /**
     * Unregisters this instance as a bundle listener and unloads all components
     * which have been registered during the active life time of the SCR
     * implementation bundle.
     */
    @Override
    public void doStop() throws Exception
    {
        // stop tracking
        super.doStop();

        if ( m_componentCommands != null )
        {
            m_componentCommands.unregister();
        }
        if ( m_runtime_reg != null )
        {
            m_runtime_reg.unregister();
            m_runtime_reg = null;
        }
        // dispose component registry
        if ( m_componentRegistry != null )
        {
            m_componentRegistry = null;
        }

        // terminate the actor thread
        if ( m_componentActor != null )
        {
            m_componentActor.terminate();
            m_componentActor = null;
        }

        // close the LogService tracker now
        if ( logger != null )
        {
            logger.close();
            logger = null;
        }
        ClassUtils.close();
    }

    //---------- Component Management -----------------------------------------

    @Override
    protected ScrExtension doCreateExtension(final Bundle bundle) throws Exception
    {
        return new ScrExtension( bundle );
    }

    protected class ScrExtension
    {

        private final Bundle bundle;
        private final Lock stateLock = new ReentrantLock();

        public ScrExtension(Bundle bundle)
        {
            this.bundle = bundle;
        }

        public void start()
        {
            boolean acquired = false;
            try
            {
                try
                {
                    acquired = stateLock.tryLock( m_configuration.stopTimeout(), TimeUnit.MILLISECONDS );

                }
                catch ( final InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    logger.log(LogService.LOG_WARNING,  "The wait for {0} being destroyed before destruction has been interrupted.", e,
                            bundle );
                }
                loadComponents( ScrExtension.this.bundle );
            }
            finally
            {
                if ( acquired )
                {
                    stateLock.unlock();
                }
            }
        }

        public void destroy()
        {
            boolean acquired = false;
            try
            {
                try
                {
                    acquired = stateLock.tryLock( m_configuration.stopTimeout(), TimeUnit.MILLISECONDS );

                }
                catch ( final InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    logger.log(LogService.LOG_WARNING,  "The wait for {0} being started before destruction has been interrupted.", e,
                            bundle );

                }
                disposeComponents( bundle );
            }
            finally
            {
                if ( acquired )
                {
                    stateLock.unlock();
                }
            }
        }
    }

    /**
     * Loads the components of the given bundle. If the bundle has no
     * <i>Service-Component</i> header, this method has no effect. The
     * fragments of a bundle are not checked for the header (112.4.1).
     * <p>
     * This method calls the {@link Bundle#getBundleContext()} method to find
     * the <code>BundleContext</code> of the bundle. If the context cannot be
     * found, this method does not load components for the bundle.
     */
    private void loadComponents(Bundle bundle)
    {
        final Long bundleId = bundle.getBundleId();
        List<ComponentMetadata> cached = m_componentMetadataStore.get(bundleId);
        if (cached != null && cached.isEmpty())
        {
            // Cached that there are no components for this bundle.
            return;
        }

        if (cached == null
            && bundle.getHeaders("").get(ComponentConstants.SERVICE_COMPONENT) == null)
        {
            // Cache that there are no components
            m_componentMetadataStore.put(bundleId,
                Collections.<ComponentMetadata> emptyList());
            // no components in the bundle, abandon
            return;
        }

        // there should be components, load them with a bundle context
        BundleContext context = bundle.getBundleContext();
        if ( context == null )
        {
            logger.log(LogService.LOG_DEBUG,  "Cannot get BundleContext of {0}.", null, bundle);

            return;
        }

        //Examine bundle for extender requirement; if present check if bundle is wired to us.
        BundleWiring wiring = bundle.adapt( BundleWiring.class );
        List<BundleWire> extenderWires = wiring.getRequiredWires( "osgi.extender" );
        for ( BundleWire wire : extenderWires )
        {
            if ( ComponentConstants.COMPONENT_CAPABILITY_NAME.equals(
                wire.getCapability().getAttributes().get( "osgi.extender" ) ) )
            {
                if ( !m_bundle.adapt( BundleRevision.class ).equals( wire.getProvider() ) )
                {
                    logger.log(LogService.LOG_DEBUG,  "{0} wired to a different extender: {1}.", null,
                            bundle, wire.getProvider().getBundle());

                    return;
                }
                break;
            }
        }

        // FELIX-1666 method is called for the LAZY_ACTIVATION event and
        // the started event. Both events cause this method to be called;
        // so we have to make sure to not load components twice
        // FELIX-2231 Mark bundle loaded early to prevent concurrent loading
        // if LAZY_ACTIVATION and STARTED event are fired at the same time
        final boolean loaded;
        synchronized ( m_componentBundles )
        {
            if ( m_componentBundles.containsKey( bundleId ) )
            {
                loaded = true;
            }
            else
            {
                m_componentBundles.put( bundleId, null );
                loaded = false;
            }
        }

        // terminate if already loaded (or currently being loaded)
        if ( loaded )
        {
            logger.log(LogService.LOG_DEBUG,  "Components for {0} already loaded. Nothing to do.", null,
                    bundle );

            return;
        }

        try
        {
            BundleComponentActivator ga = new BundleComponentActivator( this.logger, m_componentRegistry, m_componentActor,
                context, m_configuration, cached);
            ga.initialEnable();
            if (cached == null)
            {
                List<ComponentHolder<?>> components = ga.getSelectedComponents(null);
                List<ComponentMetadata> metadatas = new ArrayList<>(components.size());
                for (ComponentHolder<?> holder : ga.getSelectedComponents(null))
                {
                    metadatas.add(holder.getComponentMetadata());
                }
                m_componentMetadataStore.put(bundleId, metadatas);
            }
            // replace bundle activator in the map
            synchronized ( m_componentBundles )
            {
                m_componentBundles.put( bundleId, ga );
            }
        }
        catch ( Exception e )
        {
            // remove the bundle id from the bundles map to ensure it is
            // not marked as being loaded
            synchronized ( m_componentBundles )
            {
                m_componentBundles.remove( bundleId );
            }

            if ( e instanceof IllegalStateException && bundle.getState() != Bundle.ACTIVE )
            {
                logger.log(LogService.LOG_DEBUG,  "{0} has been stopped while trying to activate its components. Trying again when the bundles gets started again.", e,
                        bundle );
            }
            else
            {
                logger.log(LogService.LOG_ERROR,  "Error while loading components of {0}", e, bundle);
            }
        }
    }

    /**
     * Unloads components of the given bundle. If no components have been loaded
     * for the bundle, this method has no effect.
     */
    private void disposeComponents(Bundle bundle)
    {
        final BundleComponentActivator ga;
        synchronized ( m_componentBundles )
        {
            ga = m_componentBundles.remove( bundle.getBundleId() );
        }

        if ( ga != null )
        {
            try
            {
                int reason = isStopping()? ComponentConstants.DEACTIVATION_REASON_DISPOSED
                    : ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED;
                ga.dispose( reason );
            }
            catch ( Exception e )
            {
                logger.log(LogService.LOG_ERROR,  "Error while disposing components of {0}", e, bundle);
            }
        }
    }

    @Override
    protected void debug(final Bundle bundle, final String msg)
    {
        if ( logger.isLogEnabled(LogService.LOG_DEBUG) )
        {
            if ( bundle != null )
            {
                logger.log( LogService.LOG_DEBUG, "{0} : " + msg, null, bundle );
            }
            else
            {
                logger.log( LogService.LOG_DEBUG, msg, null );
            }
        }
    }

    @Override
    protected void warn(final Bundle bundle, final String msg, final Throwable t)
    {
        if ( logger.isLogEnabled(LogService.LOG_WARNING) )
        {
            if ( bundle != null )
            {
                logger.log( LogService.LOG_WARNING, "{0} : " + msg, t, bundle );
            }
            else
            {
                logger.log( LogService.LOG_WARNING, msg, t );
            }
        }
    }
}
