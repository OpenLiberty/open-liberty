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
package org.apache.felix.scr.impl.manager;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.scr.impl.inject.ComponentMethods;
import org.apache.felix.scr.impl.inject.MethodResult;
import org.apache.felix.scr.impl.inject.RefPair;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.apache.felix.scr.impl.logger.InternalLogger.Level;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.apache.felix.scr.impl.metadata.ServiceMetadata;
import org.apache.felix.scr.impl.metadata.TargetedPID;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

/**
 * The default ComponentManager. Objects of this class are responsible for managing
 * implementation object's lifecycle.
 *
 */
public abstract class AbstractComponentManager<S> implements ComponentManager<S>
{
    //useful text for deactivation reason numbers
    static final String[] REASONS = { "Unspecified", "Component disabled", "Reference became unsatisfied",
            "Configuration modified", "Configuration deleted", "Component disabled", "Bundle stopped" };

    protected enum State
    {
        //disposed is a final state, normally only for factory components
        disposed(-1, false, false, false),
        //Since enable/disable on the component description are asynchronous, this tracks the component configuration state
        //which may differ while the enable/disable is occurring.
        disabled(-1, false, false, false),
        unsatisfiedReference(ComponentConfigurationDTO.UNSATISFIED_REFERENCE, true, false, false),
        satisfied(ComponentConfigurationDTO.SATISFIED, true, true, false),
        active(ComponentConfigurationDTO.ACTIVE, true, true, true);

        private final int specState;

        private final boolean enabled;

        private final boolean satisfed;

        private final boolean actve;

        private State(int specState, boolean enabled, boolean satisfied, boolean active)
        {
            this.specState = specState;
            this.enabled = enabled;
            this.satisfed = satisfied;
            this.actve = active;
        }

        public int getSpecState()
        {
            return specState;
        }

        public boolean isEnabled()
        {
            return enabled;
        }

        public boolean isSatisfied()
        {
            return satisfed;
        }

        public boolean isActive()
        {
            return actve;
        }

    }

    protected final ComponentContainer<S> m_container;

    //true for normal spec factory instances. False for "persistent" factory instances and obsolete use of factory component with factory configurations.
    protected final boolean m_factoryInstance;

    // the ID of this component
    private volatile long m_componentId;

    private final ComponentMethods<S> m_componentMethods;

    // The dependency managers that manage every dependency
    private final List<DependencyManager<S, ?>> m_dependencyManagers;

    private volatile boolean m_dependencyManagersInitialized;

    private final AtomicInteger m_trackingCount = new AtomicInteger();

    private final ReentrantLock m_stateLock;

    /**
     * This latch prevents concurrent enable, disable, and reconfigure.  Since the enable and disable operations may use
     * two threads and the initiating thread does not wait for the operation to complete, we can't use a regular lock.
     */
    private final AtomicReference<Deferred<Void>> m_enabledLatchRef = new AtomicReference<>(
            new Deferred<Void>());

    private final AtomicReference<State> state = new AtomicReference<>(State.disabled);

    //service event tracking
    private volatile int m_floor;

    private volatile int m_ceiling;

    private final Lock m_missingLock = new ReentrantLock();
    private final Condition m_missingCondition = m_missingLock.newCondition();
    private final Set<Integer> m_missing = new TreeSet<>();

    protected final ReentrantReadWriteLock m_activationLock = new ReentrantReadWriteLock();

    private volatile String failureReason;

    /**
     * The constructor receives both the container and the methods.
     *
     * @param container The component container
     * @param componentMethods The component methods
     */
    protected AbstractComponentManager(ComponentContainer<S> container, ComponentMethods<S> componentMethods)
    {
        this(container, componentMethods, false);
    }

    /**
     * The constructor receives both the container and the methods.
     *
     * @param container The component container
     * @param componentMethods The component methods
     * @param factoryInstance Flag whether this is a factory instance
     */
    protected AbstractComponentManager(ComponentContainer<S> container, ComponentMethods<S> componentMethods, boolean factoryInstance)
    {
        m_enabledLatchRef.get().resolve(null);
        m_factoryInstance = factoryInstance;
        m_container = container;
        m_componentMethods = componentMethods;
        m_componentId = -1;

        final ComponentMetadata metadata = container.getComponentMetadata();

        m_dependencyManagers = loadDependencyManagers(metadata);

        m_stateLock = new ReentrantLock(true);

        // dump component details
        if (m_container.getLogger().isLogEnabled(Level.DEBUG))
        {
            m_container.getLogger().log(Level.DEBUG,
                    "Component created: DS={0}, implementation={1}, immediate={2}, default-enabled={3}, factory={4}, configuration-policy={5}, activate={6}, deactivate={7}, modified={8} configuration-pid={9}",
                    null,
                    metadata.getDSVersion(), metadata.getImplementationClassName(),
                            metadata.isImmediate(), metadata.isEnabled(), metadata.getFactoryIdentifier(),
                            metadata.getConfigurationPolicy(), metadata.getActivate(), metadata.getDeactivate(),
                            metadata.getModified(), metadata.getConfigurationPid());

            if (metadata.getServiceMetadata() != null)
            {
                m_container.getLogger().log(Level.DEBUG,
                        "Component Services: scope={0}, services={1}",
                        null,
                        metadata.getServiceScope(), Arrays.toString(metadata.getServiceMetadata().getProvides()));
            }
            if (metadata.getProperties() != null)
            {
                m_container.getLogger().log(Level.DEBUG, "Component Properties: {0}",
                    null,
                         metadata.getProperties() );
            }
        }
    }

    final long getLockTimeout()
    {
        //for tests....
        if (m_container.getActivator().getConfiguration() != null)
        {
            return m_container.getActivator().getConfiguration().lockTimeout();
        }
        return ScrConfiguration.DEFAULT_LOCK_TIMEOUT_MILLISECONDS;
    }

    private void obtainLock(Lock lock)
    {
        try
        {
            if (!lock.tryLock(getLockTimeout(), TimeUnit.MILLISECONDS))
            {
                dumpThreads();
                throw new IllegalStateException("Could not obtain lock");
            }
        }
        catch (InterruptedException e)
        {
            try
            {
                if (!lock.tryLock(getLockTimeout(), TimeUnit.MILLISECONDS))
                {
                    dumpThreads();
                    throw new IllegalStateException("Could not obtain lock");
                }
            }
            catch (InterruptedException e1)
            {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted twice: Could not obtain lock");
            }
            Thread.currentThread().interrupt();
        }
    }

    final void obtainActivationReadLock()
    {
        obtainLock(m_activationLock.readLock());
    }

    final void releaseActivationReadLock()
    {
        m_activationLock.readLock().unlock();
    }

    final void obtainActivationWriteLock()
    {
        obtainLock(m_activationLock.writeLock());
    }

    final void releaseActivationWriteeLock()
    {
        if (m_activationLock.getWriteHoldCount() > 0)
        {
            m_activationLock.writeLock().unlock();
        }
    }

    final void obtainStateLock()
    {
        obtainLock(m_stateLock);
    }

    final void releaseStateLock()
    {
        m_stateLock.unlock();
    }

    final boolean isStateLocked()
    {
        return m_stateLock.getHoldCount() > 0;
    }

    final void dumpThreads()
    {
        if (m_container.getLogger().isLogEnabled(Level.DEBUG)) {
            try
            {
                String dump = new ThreadDump().call();
                m_container.getLogger().log(Level.DEBUG, dump, null);
            }
            catch (Throwable t)
            {
                m_container.getLogger().log(Level.DEBUG, "Could not dump threads", t);
            }
        }
    }

    //service event tracking
    void tracked(int trackingCount)
    {
        m_missingLock.lock();
        try
        {
            if (trackingCount == m_floor + 1)
            {
                m_floor++;
                m_missing.remove(trackingCount);
            }
            else if (trackingCount < m_ceiling)
            {
                m_missing.remove(trackingCount);
            }
            if (trackingCount > m_ceiling)
            {
                for (int i = m_ceiling + 1; i < trackingCount; i++)
                {
                    m_missing.add(i);
                }
                m_ceiling = trackingCount;
            }
            m_missingCondition.signalAll();
        }
        finally
        {
            m_missingLock.unlock();
        }
    }

    /**
     * We effectively maintain the set of completely processed service event tracking counts.  This method waits for all events prior
     * to the parameter tracking count to complete, then returns.  See further documentation in EdgeInfo.
     * @param trackingCount
     */
    void waitForTracked(int trackingCount)
    {
        m_missingLock.lock();
        try
        {
            while (m_ceiling < trackingCount || (!m_missing.isEmpty() && m_missing.iterator().next() < trackingCount))
            {
                m_container.getLogger().log(Level.DEBUG,
                    "waitForTracked trackingCount: {0} ceiling: {1} missing: {2}", null,
                         trackingCount, m_ceiling, m_missing );
                try
                {
                    if (!doMissingWait())
                    {
                        return;
                    }
                }
                catch (InterruptedException e)
                {
                    try
                    {
                        if (!doMissingWait())
                        {
                            return;
                        }
                    }
                    catch (InterruptedException e1)
                    {
                        m_container.getLogger().log(Level.ERROR,
                                "waitForTracked interrupted twice: {0} ceiling: {1} missing: {2},  Expect further errors", e1,
                                 trackingCount, m_ceiling, m_missing );
                    }
                    Thread.currentThread().interrupt();
                }
            }
        }
        finally
        {
            m_missingLock.unlock();
        }
    }

    private boolean doMissingWait() throws InterruptedException
    {
        if (!m_missingCondition.await(getLockTimeout(), TimeUnit.MILLISECONDS))
        {
            m_container.getLogger().log(Level.ERROR,
                "waitForTracked timed out: {0} ceiling: {1} missing: {2},  Expect further errors",
                null,
                     m_trackingCount, m_ceiling, m_missing);
            dumpThreads();
            m_missing.clear();
            return false;
        }
        return true;
    }

    //---------- Component ID management

    void registerComponentId()
    {
        this.m_componentId = m_container.getActivator().registerComponentId(this);
        this.m_container.getLogger().setComponentId(this.m_componentId);
    }

    void unregisterComponentId()
    {
        if (this.m_componentId >= 0)
        {
            m_container.getActivator().unregisterComponentId(this);
            this.m_componentId = -1;
            this.m_container.getLogger().setComponentId(this.m_componentId);
        }
    }

    //---------- Asynchronous frontend to state change methods ----------------
    private static final AtomicLong taskCounter = new AtomicLong();

    public final Promise<Void> enable(final boolean async)
    {
        Deferred<Void> enableLatch = null;
        try
        {
            enableLatch = enableLatchWait();
            if (!async)
            {
                enableInternal();
            }
        }
        finally
        {
            if (!async)
            {
                enableLatch.resolve(null);
            }
        }

        if (async)
        {
            final Deferred<Void> latch = enableLatch;
            m_container.getActivator().schedule(new Runnable()
            {

                long count = taskCounter.incrementAndGet();

                @Override
                public void run()
                {
                    try
                    {
                        enableInternal();
                    }
                    finally
                    {
                        latch.resolve(null);
                    }
                }

                @Override
                public String toString()
                {
                    return "Async Activate: " + getComponentMetadata().getName() + " id: " + count;
                }
            });
        }
        return enableLatch.getPromise();
    }

    /**
     * Use a CountDownLatch as a non-reentrant "lock" that can be passed between threads.
     * This lock assures that enable, disable, and reconfigure operations do not overlap.
     *
     * @return the latch to count down when the operation is complete (in the calling or another thread)
     * @throws InterruptedException
     */
    Deferred<Void> enableLatchWait()
    {
        Deferred<Void> enabledLatch;
        Deferred<Void> newEnabledLatch;
        do
        {
            enabledLatch = m_enabledLatchRef.get();
            boolean waited = false;
            boolean interrupted = false;
            while (!waited)
            {
                try
                {
                    enabledLatch.getPromise().getValue();
                    waited = true;
                }
                catch (InterruptedException e)
                {
                    interrupted = true;
                }
                catch (InvocationTargetException e)
                {
                    //this is not going to happen
                }
            }
            if (interrupted)
            {
                Thread.currentThread().interrupt();
            }
            newEnabledLatch = new Deferred<>();
        }
        while (!m_enabledLatchRef.compareAndSet(enabledLatch, newEnabledLatch));
        return newEnabledLatch;
    }

    public final Promise<Void> disable(final boolean async)
    {
        Deferred<Void> enableLatch = null;
        try
        {
            enableLatch = enableLatchWait();
            if (!async)
            {
                disableInternal();
            }
        }
        finally
        {
            if (!async)
            {
                enableLatch.resolve(null);
            }
        }

        if (async)
        {
            final Deferred<Void> latch = enableLatch;
            m_container.getActivator().schedule(new Runnable()
            {

                long count = taskCounter.incrementAndGet();

                @Override
                public void run()
                {
                    try
                    {
                        disableInternal();
                    }
                    finally
                    {
                        latch.resolve(null);
                    }
                }

                @Override
                public String toString()
                {
                    return "Async Deactivate: " + getComponentMetadata().getName() + " id: " + count;
                }

            });
        }
        return enableLatch.getPromise();
    }

    // supports the ComponentInstance.dispose() method
    void dispose()
    {
        dispose(ComponentConstants.DEACTIVATION_REASON_DISPOSED);
    }

    /**
     * Disposes off this component deactivating and disabling it first as
     * required. After disposing off the component, it may not be used anymore.
     * <p>
     * This method unlike the other state change methods immediately takes
     * action and disposes the component. The reason for this is, that this
     * method has to actually complete before other actions like bundle stopping
     * may continue.
     */
    public void dispose(int reason)
    {
        deactivateInternal(reason, true, true);
    }

    <T> void registerMissingDependency(DependencyManager<S, T> dm, ServiceReference<T> ref, int trackingCount)
    {
        m_container.getActivator().registerMissingDependency(dm, ref, trackingCount);
    }

    //---------- Component interface ------------------------------------------

    @Override
    public long getId()
    {
        return m_componentId;
    }

    /**
     * Returns the <code>Bundle</code> providing this component. If the
     * component as already been disposed off, this method returns
     * <code>null</code>.
     */
    public Bundle getBundle()
    {
        final BundleContext context = getBundleContext();
        if (context != null)
        {
            try
            {
                return context.getBundle();
            }
            catch (IllegalStateException ise)
            {
                // if the bundle context is not valid any more
            }
        }
        // already disposed off component or bundle context is invalid
        return null;
    }

    BundleContext getBundleContext()
    {
        return m_container.getActivator().getBundleContext();
    }

    protected boolean isImmediate()
    {
        return getComponentMetadata().isImmediate();

    }

    public boolean isFactory()
    {
        return false;
    }

    //-------------- atomic transition methods -------------------------------

    final void enableInternal()
    {
        State previousState;
        if ((previousState = getState()) == State.disposed)
        {
            throw new IllegalStateException("enable: " + this);
        }
        if (!m_container.getActivator().isActive())
        {
            m_container.getLogger().log(Level.DEBUG,
                "Bundle's component activator is not active; not enabling component",
                null);
            return;
        }
        if (previousState.isEnabled())
        {
            m_container.getLogger().log(Level.WARN,
                "enable called but component is already in state {0}", null,
                     previousState);
            return;
        }

        registerComponentId();
        m_container.getLogger().log(Level.DEBUG, "Updating target filters", null);
        updateTargets(getProperties());

        setState(previousState, State.unsatisfiedReference);
        m_container.getLogger().log(Level.DEBUG, "Component enabled", null);
        activateInternal();
    }

    final void activateInternal()
    {
        m_container.getLogger().log(Level.DEBUG, "ActivateInternal", null);
        State s = getState();
        if (s == State.disposed)
        {
            m_container.getLogger().log(Level.DEBUG, "ActivateInternal: disposed",
                null);
            return;
        }
        if (s == State.active)
        {
            m_container.getLogger().log(Level.DEBUG,
                "ActivateInternal: already activated", null);
            return;
        }
        if (!s.isEnabled())
        {
            m_container.getLogger().log(Level.DEBUG,
                "Component is not enabled; not activating component", null);
            return;
        }
        if (!m_container.getActivator().isActive())
        {
            m_container.getLogger().log(Level.DEBUG,
                "Bundle's component activator is not active; not activating component",
                null);
            return;
        }

        m_container.getLogger().log(Level.DEBUG, "Activating component from state {0}",
            null, getState());

        // Before creating the implementation object, we are going to
        // test that the bundle has enough permissions to register services
        if (!hasServiceRegistrationPermissions())
        {
            m_container.getLogger().log(Level.DEBUG,
                "Component is not permitted to register all services, cannot activate",
                null);
            return;
        }

        obtainActivationReadLock();
        try
        {
            // Double check conditions now that we have obtained the lock
            s = getState();
            if (s == State.disposed)
            {
                m_container.getLogger().log(Level.DEBUG, "ActivateInternal: disposed",
                    null);
                return;
            }
            if (s == State.active)
            {
                m_container.getLogger().log(Level.DEBUG,
                    "ActivateInternal: already activated", null);
                return;
            }
            if (!s.isEnabled())
            {
                m_container.getLogger().log(Level.DEBUG,
                    "Component is not enabled; not activating component", null);
                return;
            }
            // Before creating the implementation object, we are going to
            // test if all the mandatory dependencies are satisfied
            if (!verifyDependencyManagers())
            {
                m_container.getLogger().log(Level.DEBUG,
                    "Not all dependencies satisfied, cannot activate", null);
                return;
            }

            if (!registerService())
            {
                //some other thread is activating us, or we got concurrently deactivated.
                return;
            }

            if ((isImmediate() || getComponentMetadata().isFactory()))
            {
                final ServiceRegistration<S> serviceRegistration = registrationManager.getServiceRegistration();
                ServiceReference<S> ref = null;
                try
                {
                    ref = serviceRegistration == null ? null : serviceRegistration.getReference();
                }
                catch ( final IllegalStateException ise )
                {
                    // catch service already being unregistered again
                }

                if ( ref != null )
                {
                    m_container.getActivator().enterCreate( ref );
                    try
                    {
                        getServiceInternal( serviceRegistration );
                    }
                    finally
                    {
                        m_container.getActivator().leaveCreate( ref );
                    }
                }
                else
                {
                    getServiceInternal( null );
                }
            }
        }
        finally
        {
            releaseActivationReadLock();
        }
    }

    /**
     * Handles deactivating, disabling, and disposing a component manager. Deactivating a factory instance
     * always disables and disposes it.  Deactivating a factory disposes it.
     * @param reason reason for action
     * @param disable whether to also disable the manager
     * @param dispose whether to also dispose of the manager
     */
    final void deactivateInternal(int reason, boolean disable, boolean dispose)
    {
        if (!getState().isEnabled())
        {
            return;
        }
        State nextState = State.unsatisfiedReference;
        if (disable)
        {
            nextState = State.disabled;
        }
        if (dispose)
        {
            nextState = State.disposed;
        }
        m_container.getLogger().log(Level.DEBUG, "Deactivating component", null);

        // catch any problems from deleting the component to prevent the
        // component to remain in the deactivating state !
        obtainActivationReadLock();
        try
        {
            //doDeactivate may trigger a state change from active to satisfied as the registration is removed.
            doDeactivate(reason, disable || m_factoryInstance);
            setState(getState(), nextState);
        }
        finally
        {
            releaseActivationReadLock();
        }
        if (isFactory() || m_factoryInstance || dispose)
        {
            m_container.getLogger().log(Level.DEBUG,
                "Disposing component (reason: {0})", null, reason);
            clear();
        }
    }

    private void doDeactivate(int reason, boolean disable)
    {
        try
        {
            if (!unregisterService())
            {
                m_container.getLogger().log(Level.DEBUG,
                    "Component deactivation occuring on another thread", null);
            }
            obtainStateLock();
            try
            {
                //              setState(previousState, State.unsatisfiedReference);
                deleteComponent(reason);
                deactivateDependencyManagers();
                if (disable)
                {
                    disableDependencyManagers();
                }
            }
            finally
            {
                releaseStateLock();
            }
        }
        catch (Throwable t)
        {
            m_container.getLogger().log(Level.WARN,
                "Component deactivation threw an exception", t);
        }
    }

    final void disableInternal()
    {
        deactivateInternal(ComponentConstants.DEACTIVATION_REASON_DISABLED, true, false);
        unregisterComponentId();
    }

    //---------- Component handling methods ----------------------------------

    protected abstract void deleteComponent(int reason);

    boolean getServiceInternal(ServiceRegistration<S> serviceRegistration)
    {
        return false;
    }

    /**
     * All ComponentManagers are ServiceFactory instances
     *
     * @return this as a ServiceFactory.
     */
    private Object getService()
    {
        return this;
    }


    ComponentMethods<S> getComponentMethods()
    {
        return m_componentMethods;
    }

    protected String[] getProvidedServices()
    {
        if (getComponentMetadata().getServiceMetadata() != null)
        {
            String[] provides = getComponentMetadata().getServiceMetadata().getProvides();
            return provides;
        }
        return null;

    }

    final RegistrationManager<ServiceRegistration<S>> registrationManager = new RegistrationManager<ServiceRegistration<S>>()
    {

        @Override
        ServiceRegistration<S> register(String[] services)
        {
            BundleContext bundleContext = getBundleContext();
            if (bundleContext == null)
            {
                return null;
            }
            final Dictionary<String, Object> serviceProperties = getServiceProperties();
            try
            {
                @SuppressWarnings("unchecked")
                ServiceRegistration<S> serviceRegistration = (ServiceRegistration<S>) bundleContext.registerService(
                        services, getService(), serviceProperties);
                return serviceRegistration;
            }
            catch (ServiceException e)
            {
                log(Level.ERROR,
                    "Unexpected error registering component service with properties {0}",
                    e, serviceProperties);
                return null;
            }
        }

        @Override
        void postRegister(ServiceRegistration<S> t)
        {
            AbstractComponentManager.this.postRegister();
        }

        @Override
        void unregister(ServiceRegistration<S> serviceRegistration)
        {
            AbstractComponentManager.this.preDeregister();
            serviceRegistration.unregister();
        }

        @Override
        void log(Level level, String message, Throwable ex, Object... arguments)
        {
            AbstractComponentManager.this.getLogger().log(level, message, ex, arguments);
        }

        @Override
        long getTimeout()
        {
            return getLockTimeout();
        }

        @Override
        void reportTimeout()
        {
            dumpThreads();
        }

    };

    /**
     * Registers the service on behalf of the component.
     *
     */
    protected boolean registerService()
    {
        String[] services = getProvidedServices();
        if (services != null)
        {
            return registrationManager.changeRegistration(RegistrationManager.RegState.registered, services);
        }
        return true;
    }

    protected boolean unregisterService()
    {
        String[] services = getProvidedServices();
        if (services != null)
        {
            return registrationManager.changeRegistration(RegistrationManager.RegState.unregistered, services);
        }
        return true;
    }

    protected ServiceRegistration<S> getServiceRegistration()
    {
        return registrationManager.getServiceRegistration();
    }

    AtomicInteger getTrackingCount()
    {
        return m_trackingCount;
    }

    private void initDependencyManagers(final ComponentContextImpl<S> componentContext)
    {
        if (m_dependencyManagersInitialized)
        {
            return;
        }
        final Bundle bundle = getBundle();
        if (bundle == null)
        {
            m_container.getLogger().log(Level.ERROR,
                "bundle shut down while trying to load implementation object class",
                null);
            throw new IllegalStateException("bundle shut down while trying to load implementation object class");
        }

        try
        {
            @SuppressWarnings("unchecked")
            Class<S> implementationObjectClass = (Class<S>) bundle.loadClass(
                getComponentMetadata().getImplementationClassName());
            m_componentMethods.initComponentMethods(getComponentMetadata(),
                implementationObjectClass, componentContext.getLogger());
        }
        catch (ClassNotFoundException e)
        {
            m_container.getLogger().log(Level.ERROR,
                "Could not load implementation object class {0}",
                    e, getComponentMetadata().getImplementationClassName() );
            throw new IllegalStateException(
                    "Could not load implementation object class " + getComponentMetadata().getImplementationClassName());
        }

        for (DependencyManager<S, ?> dependencyManager : m_dependencyManagers)
        {
            dependencyManager.initBindingMethods(m_componentMethods.getBindMethods(dependencyManager.getName()));
        }
        m_dependencyManagersInitialized = true;
    }

    /**
     * Collect and store in m_dependencies_map all the services for dependencies, outside of any locks.
     * @param componentContext possible instance key for prototype scope references
     *
     * @return true if all references can be collected,
     *   false if some dependency is no longer available.
     */
    protected boolean collectDependencies(ComponentContextImpl<S> componentContext)
    {
        initDependencyManagers(componentContext);
        for (DependencyManager<S, ?> dependencyManager : m_dependencyManagers)
        {
            if (!dependencyManager.prebind(componentContext))
            {
                //not actually satisfied any longer
                deactivateDependencyManagers();
                m_container.getLogger().log(Level.DEBUG,
                    "Could not get required dependency for dependency manager: {0}", null,
                         dependencyManager.getName());
                return false;
            }
        }
        m_container.getLogger().log(Level.DEBUG, "This thread collected dependencies",
            null);
        return true;
    }

    /**
     * Invoke updated method
     * @return {@code true} if the component needs reactivation, {@code false} otherwise.
     */
    abstract <T> boolean invokeUpdatedMethod(DependencyManager<S, T> dependencyManager, RefPair<S, T> refPair,
            int trackingCount);

    abstract <T> void invokeBindMethod(DependencyManager<S, T> dependencyManager, RefPair<S, T> refPair,
            int trackingCount);

    abstract <T> void invokeUnbindMethod(DependencyManager<S, T> dependencyManager, RefPair<S, T> oldRefPair,
            int trackingCount);

    void notifyWaiters()
    {
        if ( registrationManager.getServiceRegistration() != null )
        {
            //see if our service has been requested but returned null....
            m_container.getLogger().log(Level.DEBUG,
                "Notifying possible clients that service might be available with activator {0}",
                null,
                m_container.getActivator()  );
            try
            {
                m_container.getActivator().missingServicePresent( registrationManager.getServiceRegistration().getReference() );
            }
            catch ( final IllegalStateException ise)
            {
                // service has been unregistered
            }
        }

    }

    //**********************************************************************************************************
    public ComponentActivator getActivator()
    {
        return m_container.getActivator();
    }

    synchronized void clear()
    {
        m_container.getActivator().unregisterComponentId(this);
    }

    public ComponentLogger getLogger() {
        return m_container.getLogger();
    }

    @Override
    public String toString()
    {
        return "Component: " + getComponentMetadata().getName() + " (" + getId() + ")";
    }

    private boolean hasServiceRegistrationPermissions()
    {
        boolean allowed = true;
        if (System.getSecurityManager() != null)
        {
            final ServiceMetadata serviceMetadata = getComponentMetadata().getServiceMetadata();
            if (serviceMetadata != null)
            {
                final String[] services = serviceMetadata.getProvides();
                if (services != null && services.length > 0)
                {
                    final Bundle bundle = getBundle();
                    for (String service : services)
                    {
                        final Permission perm = new ServicePermission(service, ServicePermission.REGISTER);
                        if (!bundle.hasPermission(perm))
                        {
                            m_container.getLogger().log(Level.DEBUG,
                                "Permission to register service {0} is denied", null,
                                    service );
                            allowed = false;
                        }
                    }
                }
            }
        }

        // no security manager or no services to register
        return allowed;
    }

    private List<DependencyManager<S, ?>> loadDependencyManagers(final ComponentMetadata metadata)
    {

        // If this component has got dependencies, create dependency managers for each one of them.
        if (!metadata.getDependencies().isEmpty())
        {
            final List<DependencyManager<S, ?>> depMgrList = new ArrayList<>(metadata.getDependencies().size());
            int index = 0;
            for (final ReferenceMetadata currentdependency : metadata.getDependencies())
            {
                @SuppressWarnings({ "unchecked", "rawtypes" })
                final DependencyManager<S, ?> depmanager = new DependencyManager(this, currentdependency, index++);

                depMgrList.add(depmanager);
            }
            return depMgrList;
        }

        return Collections.emptyList();
    }

    final void updateTargets(final Map<String, Object> properties)
    {
        for (final DependencyManager<S, ?> dm : getDependencyManagers())
        {
            dm.setTargetFilter(properties);
        }
    }

    protected boolean verifyDependencyManagers()
    {
        State previousState = getState();
        // indicates whether all dependencies are satisfied
        boolean satisfied = true;

        for (DependencyManager<S, ?> dm : getDependencyManagers())
        {

            if (!dm.hasGetPermission())
            {
                // bundle has no service get permission
                if (dm.isOptional())
                {
                    m_container.getLogger().log(Level.DEBUG,
                        "No permission to get optional dependency: {0}; assuming satisfied",
                            null, dm.getName() );
                }
                else
                {
                    m_container.getLogger().log(Level.DEBUG,
                        "No permission to get mandatory dependency: {0}; assuming unsatisfied",
                            null, dm.getName() );
                    satisfied = false;
                }
            }
            else if (!dm.isSatisfied())
            {
                // bundle would have permission but there are not enough services
                m_container.getLogger().log(Level.DEBUG,
                    "Dependency not satisfied: {0}", null, dm.getName());
                satisfied = false;
            }
        }

        //Only try to change the state if the satisfied attribute is different.
        //We only succeed if no one else has changed the state meanwhile.
        if (satisfied != previousState.isSatisfied())
        {
            setState(previousState, satisfied ? State.satisfied : State.unsatisfiedReference);
        }
        return satisfied;
    }

    /**
     * Returns an iterator over the {@link DependencyManager} objects
     * representing the declared references in declaration order
     */
    List<DependencyManager<S, ?>> getDependencyManagers()
    {
        return m_dependencyManagers;
    }

    @Override
    public List<? extends ReferenceManager<S, ?>> getReferenceManagers()
    {
        return m_dependencyManagers;
    }

    /**
     * Returns an iterator over the {@link DependencyManager} objects
     * representing the declared references in reversed declaration order
     */
    List<DependencyManager<S, ?>> getReversedDependencyManagers()
    {
        List<DependencyManager<S, ?>> list = new ArrayList<>(m_dependencyManagers);
        Collections.reverse(list);
        return list;
    }

    DependencyManager<S, ?> getDependencyManager(String name)
    {
        for (ReferenceManager<S, ?> dm : getDependencyManagers())
        {
            if (name.equals(dm.getName()))
            {
                return (DependencyManager<S, ?>) dm;
            }
        }

        // not found
        return null;
    }

    private void deactivateDependencyManagers()
    {
        m_container.getLogger().log(Level.DEBUG, "Deactivating dependency managers",
            null);
        for (DependencyManager<S, ?> dm : getDependencyManagers())
        {
            dm.deactivate();
        }
    }

    private void disableDependencyManagers()
    {
        m_container.getLogger().log(Level.DEBUG, "Disabling dependency managers",
            null);
        AtomicInteger trackingCount = new AtomicInteger();
        for (DependencyManager<S, ?> dm : getDependencyManagers())
        {
            dm.unregisterServiceListener(trackingCount);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.felix.scr.impl.manager.ComponentManager#getProperties()
     */
    @Override
    public abstract Map<String, Object> getProperties();

    public abstract void setServiceProperties(Dictionary<String, ?> serviceProperties);

    /**
     * Returns the subset of component properties to be used as service
     * properties. These properties are all component properties where property
     * name does not start with dot (.), properties which are considered
     * private.
     */
    public Dictionary<String, Object> getServiceProperties()
    {
        return copyTo(null, getProperties(), false);
    }

    /**
     * Copies the properties from the <code>source</code> <code>Dictionary</code>
     * into the <code>target</code> <code>Dictionary</code> except for private
     * properties (whose name has a leading dot) which are only copied if the
     * <code>allProps</code> parameter is <code>true</code>.
     *
     * @param target    The <code>Dictionary</code> into which to copy the
     *                  properties. If <code>null</code> a new <code>Hashtable</code> is
     *                  created.
     * @param source    The <code>Dictionary</code> providing the properties to
     *                  copy. If <code>null</code> or empty, nothing is copied.
     * @param allProps  Whether all properties (<code>true</code>) or only the
     *                  public properties (<code>false</code>) are to be copied.
     *
     * @return The <code>target</code> is returned, which may be empty if
     *         <code>source</code> is <code>null</code> or empty and
     *         <code>target</code> was <code>null</code> or all properties are
     *         private and had not to be copied
     */
    protected static Dictionary<String, Object> copyTo(Dictionary<String, Object> target, final Map<String, ?> source,
            final boolean allProps)
    {
        if (target == null)
        {
            target = new Hashtable<>();
        }

        if (source != null && !source.isEmpty())
        {
            for (Map.Entry<String, ?> entry : source.entrySet())
            {
                // cast is save, because key must be a string as per the spec
                String key = entry.getKey();
                if (allProps || key.charAt(0) != '.')
                {
                    target.put(key, entry.getValue());
                }
            }
        }

        return target;
    }

    /**
     * Copies the properties from the <code>source</code> <code>Dictionary</code>
     * into the <code>target</code> <code>Dictionary</code> except for private
     * properties (whose name has a leading dot) which are only copied if the
     * <code>allProps</code> parameter is <code>true</code>.
     * @param source    The <code>Dictionary</code> providing the properties to
     *                  copy. If <code>null</code> or empty, nothing is copied.
     * @param allProps  Whether all properties (<code>true</code>) or only the
     *                  public properties (<code>false</code>) are to be copied.
     *
     * @return The <code>target</code> is returned, which may be empty if
     *         <code>source</code> is <code>null</code> or empty and
     *         <code>target</code> was <code>null</code> or all properties are
     *         private and had not to be copied
     */
    protected static Map<String, Object> copyToMap(final Dictionary<String, ?> source, final boolean allProps)
    {
        Map<String, Object> target = new HashMap<>();

        if (source != null && !source.isEmpty())
        {
            for (Enumeration<String> ce = source.keys(); ce.hasMoreElements();)
            {
                // cast is save, because key must be a string as per the spec
                String key = ce.nextElement();
                if (allProps || key.charAt(0) != '.')
                {
                    target.put(key, source.get(key));
                }
            }
        }

        return target;
    }

    protected static Dictionary<String, Object> copyToDictionary(final Dictionary<String, ?> source,
            final boolean allProps)
    {
        Hashtable<String, Object> target = new Hashtable<>();

        if (source != null && !source.isEmpty())
        {
            for (Enumeration<String> ce = source.keys(); ce.hasMoreElements();)
            {
                // cast is save, because key must be a string as per the spec
                String key = ce.nextElement();
                if (allProps || key.charAt(0) != '.')
                {
                    target.put(key, source.get(key));
                }
            }
        }

        return target;
    }

    /**
     *
     */
    public ComponentMetadata getComponentMetadata()
    {
        return m_container.getComponentMetadata();
    }

    @Override
    public int getSpecState()
    {
        return getState().getSpecState();
    }

    State getState()
    {
        State s = state.get();
        // Trace as this is a getter and is called VERY often and 
        // has no benefit for client's own debugging
        m_container.getLogger().log(Level.TRACE, "Querying state {0}", null, s);
        return s;
    }

    @Override
    public String getFailureReason() {
        return this.failureReason;
    }

    /**
     * Set the activation failure reason
     * @param e The exception which caused the activation to fail
     */
    public void setFailureReason(final Throwable e)
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        this.failureReason = sw.toString();
    }

    void setState(final State previousState, final State newState)
    {
        if (state.compareAndSet(previousState, newState))
        {
            m_container.getLogger().log(Level.DEBUG, "Changed state from {0} to {1}",
                null, previousState, newState);
            if ( newState == State.active || newState == State.unsatisfiedReference )
            {
                this.failureReason = null;
            }
            m_container.getActivator().updateChangeCount();
        }
        else
        {
            m_container.getLogger().log(Level.DEBUG,
                "Did not change state from {0} to {1}: current state {2}",
                    null, previousState, newState, state.get() );
        }

    }

    public void setServiceProperties(MethodResult methodResult, Integer trackingCount)
    {
        if (methodResult.hasResult())
        {
            if (trackingCount != null)
            {
                tracked(trackingCount);
            }
            Dictionary<String, Object> serviceProps = (methodResult.getResult() == null) ? null
                    : new Hashtable<>(methodResult.getResult());
            setServiceProperties(serviceProps);
        }
    }

    abstract void postRegister();

    abstract void preDeregister();

    public abstract void reconfigure(Map<String, Object> configuration, boolean configurationDeleted, TargetedPID factoryPid);

    public abstract void getComponentManagers(List<AbstractComponentManager<S>> cms);

    @Override
    public final ServiceReference<S> getRegisteredServiceReference()
    {
        final ServiceRegistration<S> reg = registrationManager.getServiceRegistration();
        if ( reg != null )
        {
            try
            {
                return reg.getReference();
            }
            catch ( final IllegalStateException ise)
            {
                // service has just been unregistered, return null
            }
        }
        return null;
    }
}
