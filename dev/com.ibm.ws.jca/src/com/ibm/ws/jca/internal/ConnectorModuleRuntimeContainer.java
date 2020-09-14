/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.resource.spi.BootstrapContext;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jca.metadata.ConnectorModuleMetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 *
 */
@Component(service = ModuleRuntimeContainer.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM", "type:String=connector" })
public class ConnectorModuleRuntimeContainer implements ModuleRuntimeContainer {

    private static final TraceComponent tc = Tr.register(ConnectorModuleRuntimeContainer.class);

    private BundleContext bundleContext = null;

    private ConfigurationAdmin configAdmin;

    private FutureMonitor futureMonitor;

    /**
     * Read/write lock for bundleContext
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private MetaDataService metaDataService;

    private final ConcurrentHashMap<String, ConnectorModuleMetatype> moduleMetatype = new ConcurrentHashMap<String, ConnectorModuleMetatype>();;

    /**
     * Mapping of resource adapter id to a list of service listeners that track the configured resources for that resource adapter.
     */
    private final ConcurrentHashMap<String, ServiceListener[]> serviceListeners = new ConcurrentHashMap<String, ServiceListener[]>();

    protected void activate(ComponentContext context) {
        lock.writeLock().lock();
        try {
            bundleContext = Utils.priv.getBundleContext(context);
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected void deactivate(ComponentContext context) {
        lock.writeLock().lock();
        try {
            bundleContext = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Reference
    protected void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = null;
    }

    @Reference(name = "futureMonitor")
    protected void setFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = futureMonitor;
    }

    protected void unsetFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = null;
    }

    @Reference(name = "metaDataService")
    protected void setMetaDataService(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

    protected void unsetMetaDataService(MetaDataService metaDataService) {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.app.deploy.ModuleRuntimeContainer#createModuleMetaData(com.ibm.ws.container.service.app.deploy.ModuleInfo)
     */
    @Override
    public ModuleMetaData createModuleMetaData(ExtendedModuleInfo moduleInfo) throws MetaDataException {
        Container container = moduleInfo.getContainer();
        ConnectorModuleMetaData cmmd;
        try {
            cmmd = container.adapt(ConnectorModuleMetaData.class);
        } catch (UnableToAdaptException e) {
            throw new MetaDataException(e);
        }
        return cmmd;
    }

    /**
     * Unregister listeners for configurations processed by the metatype provider
     *
     * @param id resource adapter id
     */
    private void removeServiceListeners(String id) {
        final ServiceListener[] listeners = serviceListeners.remove(id);
        if (listeners != null)
            for (ServiceListener listener : listeners)
                if (listener != null) {
                    lock.readLock().lock();
                    try {
                        if (bundleContext != null)
                            bundleContext.removeServiceListener(listener);
                    } finally {
                        lock.readLock().unlock();
                    }
                }
    }

    private class RARInstallListener implements ServiceListener {
        private final boolean autoStart;
        private final CountDownLatch latch = new CountDownLatch(1);
        private final Future<Boolean> future;
        private final String id;

        /**
         * Set of pids for instances observed in the configuration.
         * Only access when owning the lock on this class.
         */
        private final Set<String> instancesConfigured = new HashSet<String>();

        /**
         * Set of pids for instances that have been registered.
         * Only access when owning the lock on this class.
         */
        private final Set<String> instancesRegistered = new HashSet<String>();

        /**
         * Construct a service listener that will listen for REGISTERED events.
         *
         * @param id
         * @param bootstrapContextFactoryPid
         * @param autoStart
         */
        private RARInstallListener(String id, String bootstrapContextFactoryPid, boolean autoStart) throws InvalidSyntaxException, IOException {
            final boolean trace = TraceComponent.isAnyTracingEnabled();

            this.autoStart = autoStart;
            this.future = futureMonitor.createFuture(Boolean.class);
            this.id = id;
            instancesConfigured.add("com.ibm.ws.jca.resourceAdapter.properties");

            String filter = "(|(service.factoryPid=properties." + id + ")(service.factoryPid=properties." + id + ".*))";
            Configuration[] configs = configAdmin.listConfigurations(filter);
            if (trace && tc.isDebugEnabled())
                Tr.debug(ConnectorModuleRuntimeContainer.this, tc, "[" + id + "] " + filter, (Object[]) configs);

            // Look for unique config.parentPID similar to these:
            // com.ibm.ws.jca.connectionFactory_29
            // com.ibm.ws.jca.jmsQueue_51
            // but not these
            // com.ibm.ws.jca.embeddedResourceAdapter_10
            // com.ibm.ws.jca.resourceAdapter_5
            // connectionFactory
            // jmsQueue
            Set<String> parentPIDs = new HashSet<String>();
            if (configs != null)
                for (Configuration c : configs) {
                    String parentPID = (String) c.getProperties().get("config.parentPID");
                    if (parentPID != null && parentPID.length() > 25 && parentPID.lastIndexOf('_') > 0 && parentPID.charAt(15) != 'r' && parentPID.charAt(15) != 'e')
                        parentPIDs.add(parentPID);
                }

            if (trace && tc.isDebugEnabled())
                Tr.debug(ConnectorModuleRuntimeContainer.this, tc, "[" + id + "] parent pids", parentPIDs);

            instancesConfigured.addAll(parentPIDs);

            final ServiceListener[] listeners = new ServiceListener[4];
            serviceListeners.put(id, listeners);

            lock.readLock().lock();
            try {
                if (bundleContext != null) {
                    // properties for resourceAdapter
                    bundleContext.addServiceListener(listeners[0] = this,
                                                     "(&(objectClass=ja*.resource.spi.BootstrapContext)(id=" + id + "))");

                    // properties for adminObject, jmsDestination, jmsQueue, jmsTopic
                    bundleContext.addServiceListener(listeners[1] = new ResourceListener(),
                                                     "(&(objectClass=com.ibm.ws.jca.service.AdminObjectService)(properties.0.config.referenceType=com.ibm.ws.jca.*.properties."
                                                                                            + id + ".*))");

                    // properties for connectionFactory, jmsConnectionFactory, jmsQueueConnectionFactory, jmsTopicConnectionFactory
                    bundleContext.addServiceListener(listeners[2] = new ResourceListener(),
                                                     "(&(objectClass=com.ibm.ws.jca.service.ConnectionFactoryService)(properties.0.config.referenceType=com.ibm.ws.jca.*onnectionFactory.properties."
                                                                                            + id + ".*))");

                    // properties for activationSpec, jmsActivationSpec
                    bundleContext.addServiceListener(listeners[3] = new ResourceListener(),
                                                     "(&(objectClass=com.ibm.ws.jca.service.EndpointActivationService)(properties.0.config.referenceType=com.ibm.ws.jca.*ctivationSpec.properties."
                                                                                            + id + ".*))");
                }
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Delegates events to a common ServiceListener.
         */
        @Trivial
        private final class ResourceListener implements ServiceListener {
            /**
             * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
             */
            @Override
            public void serviceChanged(ServiceEvent event) {
                RARInstallListener.this.serviceChanged(event);
            }
        }

        /**
         * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
         */
        @Override
        public void serviceChanged(ServiceEvent event) {
            if (event.getType() == ServiceEvent.REGISTERED) {
                ServiceReference<?> serviceRef = event.getServiceReference();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(ConnectorModuleRuntimeContainer.this, tc, "[" + id + "] serviceChanged", serviceRef);

                String pid = (String) serviceRef.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);
                if ("com.ibm.ws.jca.resourceAdapter.properties".equals(pid)) {
                    latch.countDown();
                    if (autoStart) {
                        lock.readLock().lock();
                        try {
                            if (bundleContext != null)
                                Utils.priv.getService(bundleContext, event.getServiceReference());
                        } catch (Throwable x) {
                        } finally {
                            lock.readLock().unlock();
                        }
                    }
                } else {
                    pid = (String) serviceRef.getProperty("ibm.extends.source.pid");
                    if (pid == null)
                        pid = (String) serviceRef.getProperty(Constants.SERVICE_PID);
                }

                if (pid != null) {
                    boolean allInstancesAreRegistered;
                    synchronized (this) {
                        allInstancesAreRegistered = instancesRegistered.add(pid)
                                                    && instancesConfigured.contains(pid)
                                                    && instancesRegistered.containsAll(instancesConfigured);
                    }
                    if (allInstancesAreRegistered) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(this, tc, "[" + id + "] all instances registered", instancesConfigured, instancesRegistered);
                        removeServiceListeners(id);
                        futureMonitor.setResult(future, true);
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.app.deploy.ModuleRuntimeContainer#startModule(com.ibm.ws.container.service.app.deploy.ModuleInfo)
     */
    @Override
    @FFDCIgnore(TimeoutException.class)
    public Future<Boolean> startModule(ExtendedModuleInfo moduleInfo) throws StateChangeException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        ConnectorModuleMetaDataImpl metadataImpl = (ConnectorModuleMetaDataImpl) moduleInfo.getMetaData();
        String id = metadataImpl.getIdentifier();
        ResourceAdapterMetaData raMetaData = (ResourceAdapterMetaData) metadataImpl.getComponentMetaDatas()[0];

        Container container = moduleInfo.getContainer();
        ConnectorModuleMetatype cmmt;
        try {
            cmmt = container.adapt(ConnectorModuleMetatype.class);
        } catch (UnableToAdaptException e) {
            throw new StateChangeException(e);
        }

        try {
            metaDataService.fireComponentMetaDataCreated(raMetaData);
        } catch (MetaDataException mde) {
            throw new StateChangeException(mde);
        }

        try {
            cmmt.generateMetatype();
        } catch (Exception x) {
            if (x instanceof StateChangeException)
                throw (StateChangeException) x;
            throw new StateChangeException(x);
        }

        moduleMetatype.put(id.toUpperCase(), cmmt);

        String bootstrapContextFactoryPid = cmmt.getBootstrapContextFactoryPid();

        // TEMP begin
        // It's important that the service listeners be registered before the metatype provider, otherwise we could miss some events
        RARInstallListener rarInstallListener;
        try {
            rarInstallListener = new RARInstallListener(id, bootstrapContextFactoryPid, cmmt.getAutoStart());
        } catch (Throwable x) {
            throw new StateChangeException(x);
        }
        // TEMP end

        try {
            cmmt.registerMetatype();
        } catch (Exception x) {
            throw new StateChangeException(x);
        }

        // TEMP begin
        while (!FrameworkState.isStopping()) {
            try {
                if (rarInstallListener.latch.await(1000, TimeUnit.MILLISECONDS)) {
                    if (trace && tc.isEventEnabled())
                        Tr.event(tc, "[" + id + "] BootstrapContext CountDownLatch complete");
                    break;
                }
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "[" + id + "] BootstrapContext service event has not arrived");
            } catch (InterruptedException x) {
                x.getCause();
            }
        }

        if (!FrameworkState.isStopping())
            try {
                rarInstallListener.future.get(metadataImpl.getMaxWaitForResources(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException x) {
                futureMonitor.setResult(rarInstallListener.future, x);
            } catch (ExecutionException x) {
                throw new StateChangeException(x);
            } catch (TimeoutException x) {
                // Only consider this a failure if the bootstrap context singleton is missing.
                // Otherwise, it is due to one or more invalidly configured resource instance(s).
                if (trace && tc.isEventEnabled())
                    synchronized (rarInstallListener) {
                        Tr.event(tc, "[" + id + "] Timed out waiting for configured resource instances to be registered. Configured/Registered:",
                                 rarInstallListener.instancesConfigured, rarInstallListener.instancesRegistered);
                    }

                try {
                    Collection<ServiceReference<BootstrapContext>> refs;
                    lock.readLock().lock();
                    try {
                        refs = bundleContext == null ? null : Utils.priv.getServiceReferences(bundleContext, BootstrapContext.class, "(id=" + id + ')');
                    } finally {
                        lock.readLock().unlock();
                    }
                    if (refs == null || refs.isEmpty()) {
                        FFDCFilter.processException(x, getClass().getName(), "420", this, new Object[] { id, bootstrapContextFactoryPid });
                        futureMonitor.setResult(rarInstallListener.future, x);
                    } else
                        futureMonitor.setResult(rarInstallListener.future, true);
                } catch (InvalidSyntaxException e) {
                    futureMonitor.setResult(rarInstallListener.future, e);
                }
            } finally {
                removeServiceListeners(id);
            }
        // TEMP end

        return rarInstallListener.future;
    }

    /**
     * Removes all generated metatype for the resource adapter and returns once services provided by the resource adapter are deactivated
     *
     * @see com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer#stopModule(com.ibm.ws.container.service.app.deploy.ModuleInfo)
     */
    @Override
    public void stopModule(ExtendedModuleInfo moduleInfo) {
        ConnectorModuleMetaDataImpl metadataImpl = (ConnectorModuleMetaDataImpl) moduleInfo.getMetaData();
        String id = metadataImpl.getIdentifier();

        metaDataService.fireComponentMetaDataDestroyed(metadataImpl.getComponentMetaDatas()[0]);
        removeServiceListeners(id);

        // TEMP begin
        CountDownLatch bcLatch = BootstrapContextImpl.latches.get(id);
        // TEMP end

        ConnectorModuleMetatype cmmt = moduleMetatype.remove(id.toUpperCase());
        if (cmmt != null)
            try {
                cmmt.removeMetatype();
            } catch (Throwable x) {
            }

        // TEMP begin
        for (boolean stopped = bcLatch == null || FrameworkState.isStopping(); !stopped;)
            try {
                stopped = bcLatch.await(1, TimeUnit.SECONDS) || FrameworkState.isStopping();
            } catch (InterruptedException e) {
                e.getCause();
            }
        // TEMP end
    }
}
