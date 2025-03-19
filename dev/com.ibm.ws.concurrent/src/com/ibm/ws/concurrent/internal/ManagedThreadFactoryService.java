/*******************************************************************************
 * Copyright (c) 2013,2025 IBM Corporation and others.
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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.concurrent.ManagedThreadFactory;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.ContextualAction;
import com.ibm.ws.concurrent.cdi.MTFBeanResourceInfo;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.container.service.metadata.extended.IdentifiableComponentMetaData;
import com.ibm.ws.container.service.metadata.extended.MetaDataIdentifierService;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

import io.openliberty.threading.virtual.VirtualThreadOps;

/**
 * Resource factory for ManagedThreadFactory.
 * Unlike ManagedExecutorService and ManagedScheduledExecutorService, we need a separate instance of ManagedThreadFactory
 * for each lookup/injection because, per the spec, thread context is captured at that point.
 */
@Component(configurationPid = "com.ibm.ws.concurrent.managedThreadFactory",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ResourceFactory.class, ApplicationRecycleComponent.class },
           property = { "creates.objectClass=java.util.concurrent.ThreadFactory",
                        "creates.objectClass=javax.enterprise.concurrent.ManagedThreadFactory" })
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
     * Name of the internal property for virtual threads.
     */
    private static final String VIRTUAL = "virtual";

    /**
     * Names of applications using this ResourceFactory
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Reference to the context service for this managed thread factory service.
     */
    private final AtomicServiceReference<WSContextService> contextSvcRef = //
                    new AtomicServiceReference<WSContextService>("ContextService");

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
    private final Map<String, String> defaultExecutionProperties;

    /**
     * Default thread priority. Null if unspecified.
     */
    Integer defaultPriority;

    /**
     * Indicates if this instance has been marked for shutdown.
     */
    final AtomicBoolean isShutdown = new AtomicBoolean();

    /**
     * The metadata identifier service.
     */
    private final MetaDataIdentifierService metadataIdentifierService;

    /**
     * Name of this managed thread factory.
     * The name is the jndiName if specified, otherwise the config id.
     */
    private final String name;

    /**
     * Thread group for the managed thread factory.
     */
    private final ThreadGroup threadGroup;

    /**
     * Tracks thread groups for application components.
     */
    private final ThreadGroupTracker threadGroupTracker;

    /**
     * Factory that creates virtual threads if greater than Java 21
     * or raises an error if configured to create virtual threads
     * on less than Java 21.
     * Null if not configured to create virtual threads.
     */
    private final ThreadFactory virtualThreadFactory;

    /**
     * Metadata factory for the web container.
     */
    @Reference(target = "(deferredMetaData=WEB)",
               cardinality = ReferenceCardinality.OPTIONAL,
               policyOption = ReferencePolicyOption.GREEDY)
    protected DeferredMetaDataFactory webMetadataFactory;

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param componentContext DeclarativeService defined/populated component context
     */
    @Activate
    @Trivial
    public ManagedThreadFactoryService(ComponentContext componentContext,
                                       @Reference(name = "ContextService",
                                                  service = WSContextService.class,
                                                  target = "(id=unbound)") //
                                       ServiceReference<WSContextService> ref,
                                       @Reference //
                                       MetaDataIdentifierService metadataSvc,
                                       @Reference //
                                       ThreadGroupTracker threadGroupTracker,
                                       @Reference //
                                       VirtualThreadOps virtualThreadOps) {
        Dictionary<String, ?> properties = componentContext.getProperties();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate",
                     properties,
                     "ContextService: " + ref,
                     metadataSvc,
                     threadGroupTracker,
                     virtualThreadOps);

        this.metadataIdentifierService = metadataSvc;
        this.threadGroupTracker = threadGroupTracker;

        contextSvcRef.setReference(ref);
        contextSvcRef.activate(componentContext);

        String jndiName = (String) properties.get(JNDI_NAME);
        name = jndiName == null ? (String) properties.get(CONFIG_ID) : jndiName;

        defaultExecutionProperties = new TreeMap<String, String>();
        defaultExecutionProperties.put(WSContextService.DEFAULT_CONTEXT, WSContextService.UNCONFIGURED_CONTEXT_TYPES);
        defaultExecutionProperties.put(WSContextService.TASK_OWNER, name);

        // The following are ignored for virtual threads, but are used for managed fork join threads even if virtual=true
        createDaemonThreads = (Boolean) properties.get(CREATE_DAEMON_THREADS);
        defaultPriority = (Integer) properties.get(DEFAULT_PRIORITY);
        Integer maxPriority = (Integer) properties.get(MAX_PRIORITY);
        threadGroup = AccessController.doPrivileged(new CreateThreadGroupAction(name + " Thread Group", maxPriority),
                                                    threadGroupTracker.serverAccessControlContext);

        boolean virtual = Boolean.TRUE.equals(properties.get(VIRTUAL));

        // TODO check the SPI to override virtual=true for CICS

        if (virtual)
            if (JavaInfo.majorVersion() >= 21)
                virtualThreadFactory = virtualThreadOps.createFactoryOfVirtualThreads(properties.get(CONFIG_ID) + ":", 1L, false, null);
            else
                virtualThreadFactory = new VirtualThreadsUnsupported();
        else
            virtualThreadFactory = null;

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param componentContext DeclarativeService defined/populated component context
     */
    @Deactivate
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
        String appName = null;
        String identifier = null;

        ClassLoader classLoaderToRestore = null;
        boolean restoreClassLoader = false;
        boolean restoreMetadata = false;

        ComponentMetaDataAccessorImpl accessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();

        try {
            if (info instanceof MTFBeanResourceInfo) {
                MTFBeanResourceInfo beanInfo = (MTFBeanResourceInfo) info;
                MetaData metadata = beanInfo.getDeclaringMetaData();

                ComponentMetaData cData;
                if (metadata instanceof ComponentMetaData) {
                    cData = (ComponentMetaData) metadata;
                } else if (metadata instanceof ModuleMetaData) {
                    ModuleMetaData mData = (ModuleMetaData) metadata;

                    @SuppressWarnings("deprecation")
                    ComponentMetaData[] c = mData.getComponentMetaDatas();
                    if (c != null && c.length == 1) {
                        cData = c[0];
                    } else {
                        // The DeferredMetaDataFactory for web module can construct ComponentMetaData without the component name.
                        String webMetadataIdentifier = null;
                        for (Class<?> ifc : mData.getClass().getInterfaces()) {
                            if (ifc.getName().equals("com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData")) {
                                J2EEName jeeName = mData.getJ2EEName();
                                webMetadataIdentifier = webMetadataFactory.getMetaDataIdentifier(jeeName.getApplication(),
                                                                                                 jeeName.getModule(),
                                                                                                 null);
                                break;
                            }
                        }
                        if (webMetadataIdentifier == null)
                            throw new IllegalArgumentException("Unrecognized module metadata " + mData +
                                                               " of type " + mData.getClass().getName()); // internal error

                        cData = webMetadataFactory.createComponentMetaData(webMetadataIdentifier);
                        if (cData == null) {
                            J2EEName jeeName = mData.getJ2EEName();
                            String err = Tr.formatMessage(tc,
                                                          "CWWKC1122.mod.unavail",
                                                          jeeName.getModule(),
                                                          jeeName.getApplication());
                            throw new IllegalStateException(err);
                        }
                    }
                } else {
                    // Should be unreachable because mock ComponentMetaData is created for resources defined in application.xml.
                    throw new UnsupportedOperationException("A ManagedThreadFactory resource defined is in the " +
                                                            metadata.getName() + " application artifact with " +
                                                            metadata.getClass().getName() + " metadata.");
                }

                appName = cData.getJ2EEName().getApplication();
                identifier = cData instanceof IdentifiableComponentMetaData //
                                ? metadataIdentifierService.getMetaDataIdentifier(cData) //
                                : null;

                // push class loader onto the thread for context capture
                ClassLoader declaringClassLoader = beanInfo.getDeclaringClassLoader();
                classLoaderToRestore = Thread.currentThread().getContextClassLoader();
                if (classLoaderToRestore != declaringClassLoader) {
                    Thread.currentThread().setContextClassLoader(declaringClassLoader);
                    restoreClassLoader = true;
                }

                // push metadata onto the thread for context capture
                if (accessor.getComponentMetaData() != cData) {
                    accessor.beginContext(cData);
                    restoreMetadata = true;
                }
            }

            if (appName == null) {
                ComponentMetaData cData = accessor.getComponentMetaData();
                if (cData != null) {
                    appName = cData.getJ2EEName().getApplication();

                    if (identifier == null)
                        identifier = metadataIdentifierService.getMetaDataIdentifier(cData);
                }
            }

            if (appName != null)
                applications.add(appName);

            return new ManagedThreadFactoryImpl(identifier, threadGroupTracker.serverAccessControlContext);
        } finally {
            if (restoreMetadata)
                accessor.endContext();

            if (restoreClassLoader)
                Thread.currentThread().setContextClassLoader(classLoaderToRestore);
        }
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
         * @param name        thread group name
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
         * @param identifier                 identifier of the component that created, looked up, or injected
         *                                       this managed thread factory.
         * @param serverAccessControlContext server access control context, which we can use to run certain privileged operations
         *                                       that aren't available to application threads.
         */
        @SuppressWarnings("unchecked")
        ManagedThreadFactoryImpl(String identifier, AccessControlContext serverAccessControlContext) {
            this.identifier = identifier;
            this.serverAccessControlContext = serverAccessControlContext;

            WSContextService contextSvc = contextSvcRef.getServiceWithException();
            threadContextDescriptor = contextSvc.captureThreadContext(defaultExecutionProperties);

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

            Thread thread;
            if (virtualThreadFactory == null) {
                String threadName = name + "-thread-" + createdThreadCount.incrementAndGet();
                thread = new ManagedThreadImpl(this, runnable, threadName);
            } else {
                thread = virtualThreadFactory.newThread(new ManagedVirtualThreadAction(this, runnable));
            }

            if (trace && tc.isEntryEnabled())
                Tr.exit(ManagedThreadFactoryService.this, tc, "newThread", thread);
            return thread;
        }

        /**
         * @see java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory#newThread(java.util.concurrent.ForkJoinPool)
         */
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            final boolean trace = TraceComponent.isAnyTracingEnabled();
            if (trace && tc.isEntryEnabled())
                Tr.entry(ManagedThreadFactoryService.this, tc, "newThread", this, pool);

            // EE Concurrency 3.4.1: If a ManagedThreadFactory instance is stopped, all subsequent calls to newThread() must throw a
            // java.lang.IllegalStateException
            if (isShutdown.get())
                throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1100.resource.unavailable", name));

            ManagedForkJoinWorkerThread thread = new ManagedForkJoinWorkerThread(this, pool);

            if (trace && tc.isEntryEnabled())
                Tr.exit(ManagedThreadFactoryService.this, tc, "newThread", thread);
            return thread;
        }

        boolean sameMetaDataIdentity() {
            // Return false if our identity is null (even if the current component's metadata or metadata identity is also null).
            ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            return identifier != null && metadataIdentifierService != null && identifier.equals(metadataIdentifierService.getMetaDataIdentifier(cData));
        }
    }

    /**
     * Raises an error if the application requests a virtual threads.
     */
    @Trivial
    private class VirtualThreadsUnsupported implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            throw new UnsupportedOperationException(Tr //
                            .formatMessage(tc,
                                           "CWWKC1121.virtual.invalid",
                                           "ManagedThreadFactoryDefinition",
                                           "managed-thread-factory",
                                           name));
        }
    }
}
