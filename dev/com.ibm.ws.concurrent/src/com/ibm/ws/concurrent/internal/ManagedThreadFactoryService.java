/*******************************************************************************
 * Copyright (c) 2013,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.concurrent.ManagedThreadFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.ContextualAction;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Resource factory for ManagedThreadFactory.
 * Unlike ManagedExecutorService and ManagedScheduledExecutorService, we need a separate instance of ManagedThreadFactory
 * for each lookup/injection because, per the spec, thread context is captured at that point.
 */
public class ManagedThreadFactoryService implements ResourceFactory, ApplicationRecycleComponent {
    private static final TraceComponent tc = Tr.register(ManagedThreadFactoryService.class);

    /**
     * Name of the unique identifier property
     */
    private static final String CONFIG_ID = "config.displayId";

    /**
     * Name of property that specifies whether or not to create daemon threads.
     */
    private static final String CREATE_DAEMON_THREADS = "createDaemonThreads";

    /**
     * Name of property for default thread priority.
     */
    private static final String DEFAULT_PRIORITY = "defaultPriority";

    /**
     * Name of property for maximum thread priority.
     */
    private static final String MAX_PRIORITY = "maxPriority";

    /**
     * Names of applications using this ResourceFactory
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Privileged action to lazily obtain the context service.
     */
    private final PrivilegedAction<WSContextService> contextSvcAccessor = new PrivilegedAction<WSContextService>() {
        @Override
        public WSContextService run() {
            return contextSvcRef.getServiceWithException();
        }
    };

    /**
     * Reference to the context service for this managed thread factory service.
     */
    private final AtomicServiceReference<WSContextService> contextSvcRef = new AtomicServiceReference<WSContextService>("contextService");

    /**
     * Specifies whether or not to create daemon threads.
     */
    boolean createDaemonThreads;

    /**
     * Count of threads created by this thread factory.
     */
    private final AtomicInteger createdThreadCount = new AtomicInteger();

    /**
     * Default execution properties for thread context capture.
     */
    private Map<String, String> defaultExecutionProperties;

    /**
     * Default thread priority. Null if unspecified.
     */
    Integer defaultPriority;

    /**
     * Indicates if this instance has been marked for shutdown.
     */
    final AtomicBoolean isShutdown = new AtomicBoolean();

    /**
     * Name of this managed thread factory.
     * The name is the jndiName if specified, otherwise the config id.
     */
    private String name;

    /**
     * Thread group for the managed thread factory.
     */
    private ThreadGroup threadGroup;

    /**
     * Tracks thread groups for application components.
     */
    private ThreadGroupTracker threadGroupTracker;

    /**
     * The metadata identifier service.
     */
    private MetaDataIdentifierService metadataIdentifierService;

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param componentContext DeclarativeService defined/populated component context
     */
    @Trivial
    protected void activate(ComponentContext componentContext) {
        Dictionary<String, ?> properties = componentContext.getProperties();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate", properties);

        contextSvcRef.activate(componentContext);

        String jndiName = (String) properties.get(JNDI_NAME);
        name = jndiName == null ? (String) properties.get(CONFIG_ID) : jndiName;

        defaultExecutionProperties = new TreeMap<String, String>();
        defaultExecutionProperties.put(WSContextService.DEFAULT_CONTEXT, WSContextService.UNCONFIGURED_CONTEXT_TYPES);
        defaultExecutionProperties.put(WSContextService.TASK_OWNER, name);

        createDaemonThreads = (Boolean) properties.get(CREATE_DAEMON_THREADS);
        defaultPriority = (Integer) properties.get(DEFAULT_PRIORITY);
        Integer maxPriority = (Integer) properties.get(MAX_PRIORITY);
        threadGroup = AccessController.doPrivileged(new CreateThreadGroupAction(name + " Thread Group", maxPriority), threadGroupTracker.serverAccessControlContext);

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param componentContext DeclarativeService defined/populated component context
     */
    protected void deactivate(ComponentContext componentContext) {
        isShutdown.set(true);

        contextSvcRef.deactivate(componentContext);

        threadGroupTracker.threadFactoryDestroyed(name, threadGroup);
    }

    @Override
    public ApplicationRecycleContext getContext() {
        return null;
    }

    @Override
    public Set<String> getDependentApplications() {
        Set<String> members = new HashSet<String>(applications);
        applications.removeAll(members);
        return members;
    }

    /**
     * @see com.ibm.wsspi.resource.ResourceFactory#createResource(com.ibm.wsspi.resource.ResourceInfo)
     */
    @Override
    public Object createResource(ResourceInfo info) throws Exception {
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cData != null)
            applications.add(cData.getJ2EEName().getApplication());

        return new ManagedThreadFactoryImpl(threadGroupTracker.serverAccessControlContext);
    }

    /**
     * Declarative Services method for setting the context service reference
     *
     * @param ref reference to the service
     */
    protected void setContextService(ServiceReference<WSContextService> ref) {
        contextSvcRef.setReference(ref);
    }

    /**
     * Declarative Services method for setting the ThreadGroupTracker service
     *
     * @param the service
     */
    protected void setThreadGroupTracker(ThreadGroupTracker svc) {
        threadGroupTracker = svc;
    }

    /**
     * Declarative Services method for setting the metadata identifier service
     *
     * @param the service
     */
    protected void setMetadataIdentifierService(MetaDataIdentifierService svc) {
        metadataIdentifierService = svc;
    }

    /**
     * Declarative Services method for unsetting the context service reference
     *
     * @param ref reference to the service
     */
    protected void unsetContextService(ServiceReference<WSContextService> ref) {
        contextSvcRef.unsetReference(ref);
    }

    /**
     * Declarative Services method for unsetting the ThreadGroupTracker service
     *
     * @param ref reference to the service
     */
    protected void unsetThreadGroupTracker(ThreadGroupTracker svc) {
        threadGroupTracker = null;
    }

    /**
     * Declarative Services method for unsetting the metadata identifier service
     *
     * @param ref reference to the service
     */
    protected void unsetMetadataIdentifierService(MetaDataIdentifierService svc) {
        metadataIdentifierService = null;
    }

    /**
     * Creates a thread group.
     */
    @Trivial
    private static class CreateThreadGroupAction implements PrivilegedAction<ThreadGroup> {
        private final Integer maxPriority;
        private final String name;

        /**
         * Construct a privileged action that creates a thread group.
         *
         * @param name thread group name
         * @param maxPriority maximum priority for the threads
         */
        private CreateThreadGroupAction(String name, Integer maxPriority) {
            this.maxPriority = maxPriority;
            this.name = name;
        }

        /**
         * Create a thread group and optionally assign max priority and whether it's a daemon thread group.
         */
        @Override
        public ThreadGroup run() {
            ThreadGroup group = new ThreadGroup(name);
            group.setDaemon(false);
            if (maxPriority != null)
                group.setMaxPriority(maxPriority);
            return group;
        }
    }

    /**
     * Injected or looked up managed thread factory instance.
     */
    class ManagedThreadFactoryImpl implements ManagedThreadFactory {
        /**
         * Identifier of the component metadata (if any) that looked up or injected this managed thread factory.
         */
        private final String identifier;

        /**
         * Server access control context, which we can use to run certain privileged operations that aren't available to application threads.
         */
        final AccessControlContext serverAccessControlContext;

        /**
         * Managed thread factory service.
         */
        final ManagedThreadFactoryService service = ManagedThreadFactoryService.this;

        /**
         * Captured thread context.
         */
        final ThreadContextDescriptor threadContextDescriptor;

        /**
         * Thread group.
         */
        final ThreadGroup threadGroup;

        /**
         * Capture the current thread context and construct a ManagedThreadFactory.
         *
         * @param serverAccessControlContext server access control context, which we can use to run certain privileged operations
         *            that aren't available to application threads.
         */
        ManagedThreadFactoryImpl(AccessControlContext serverAccessControlContext) {
            this.serverAccessControlContext = serverAccessControlContext;

            WSContextService contextSvc = AccessController.doPrivileged(service.contextSvcAccessor);
            threadContextDescriptor = contextSvc.captureThreadContext(defaultExecutionProperties);

            ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            identifier = cData == null ? null : metadataIdentifierService.getMetaDataIdentifier(cData);
            if (identifier == null)
                threadGroup = ManagedThreadFactoryService.this.threadGroup;
            else
                threadGroup = threadGroupTracker.getThreadGroup(identifier, name, service.threadGroup);
        }

        /**
         * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
         */
        @Override
        public Thread newThread(Runnable runnable) {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(ManagedThreadFactoryService.this, tc, "newThread", this, runnable);

            // EE Concurrency 3.4.1: If a ManagedThreadFactory instance is stopped, all subsequent calls to newThread() must throw a
            // java.lang.IllegalStateException
            if (isShutdown.get())
                throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1100.resource.unavailable", name));

            // There is no spec requirement to allow contextual actions here, so reject until there is shown to be a need
            if (runnable instanceof ContextualAction)
                throw new IllegalArgumentException(runnable.getClass().getName());

            String threadName = name + "-thread-" + createdThreadCount.incrementAndGet();
            ManagedThreadImpl thread = new ManagedThreadImpl(this, runnable, threadName);

            if (trace && tc.isEntryEnabled())
                Tr.exit(ManagedThreadFactoryService.this, tc, "newThread", thread);
            return thread;
        }

        boolean sameMetaDataIdentity() {
            // Return false if our identity is null (even if the current component's metadata or metadata identity is also null).
            ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            return identifier == null ? false : identifier.equals(metadataIdentifierService.getMetaDataIdentifier(cData));
        }
    }
}
