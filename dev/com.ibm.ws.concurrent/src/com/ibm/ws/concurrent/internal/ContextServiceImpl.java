/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedTask;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.context.service.serializable.ContextualCallable;
import com.ibm.ws.context.service.serializable.ContextualInvocationHandler;
import com.ibm.ws.context.service.serializable.ContextualObject;
import com.ibm.ws.context.service.serializable.ContextualRunnable;
import com.ibm.ws.context.service.serializable.ThreadContextManager;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.resource.ResourceInfo;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Captures and propagates thread context.
 *
 * All declarative services annotations on this class are ignored.
 * The annotations on
 * com.ibm.ws.concurrent.ee.ContextServiceImpl and
 * com.ibm.ws.concurrent.mp.ThreadContextImpl
 * apply instead.
 */
@Component(name = "com.ibm.ws.context.service",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ResourceFactory.class, ContextService.class, WSContextService.class, ApplicationRecycleComponent.class },
           property = { "creates.objectClass=javax.enterprise.concurrent.ContextService" })
public class ContextServiceImpl implements ContextService, ResourceFactory, WSContextService, ApplicationRecycleComponent {
    private static final TraceComponent tc = Tr.register(ContextServiceImpl.class);

    // Names of references
    private static final String BASE_INSTANCE = "baseInstance",
                    THREAD_CONTEXT_MANAGER = "threadContextManager";

    // Names of supported properties
    private static final String CONFIG_ID = "config.displayId",
                    BASE_CONTEXT_REF = "baseContextRef",
                    ID = "id",
                    JNDI_NAME = "jndiName";

    /**
     * List of supported properties
     */
    private static final List<String> SUPPORTED_PROPERTIES = Arrays.asList(BASE_CONTEXT_REF,
                                                                           ResourceFactory.CREATES_OBJECT_CLASS,
                                                                           ID,
                                                                           JNDI_NAME,
                                                                           Constants.OBJECTCLASS,
                                                                           OnErrorUtil.CFG_KEY_ON_ERROR);

    /**
     * Component context for this contextService instance.
     */
    private ComponentContext componentContext;

    /**
     * Lock for reading and updating configuration.
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * These listeners (other contextService instances which are using this instance as the base instance)
     * need to be notified when we are modified so that they can uninitialize and pick up the new configuration
     * the next time they are used.
     */
    private final List<ContextServiceImpl> modificationListeners = new LinkedList<ContextServiceImpl>();

    /**
     * Name of this thread context service.
     * The name is the jndiName if specified, otherwise the config id.
     */
    protected String name; // TODO this is temporarily switched from private to protected in order to accommodate test case

    /**
     * Service properties.
     */
    private Dictionary<String, ?> properties;

    /**
     * Map of thread context provider name to configured thread context.
     *
     * This value will be NULL when the context service hasn't (re)initialized yet.
     */
    private Map<String, Map<String, ?>> threadContextConfigurations;

    /**
     * Centralized service that holds all of the registered thread context providers.
     */
    private ThreadContextManager threadContextMgr;

    /**
     * Names of applications using this ResourceFactory
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     */
    @Trivial
    @Activate
    protected void activate(ComponentContext context) {
        Dictionary<String, ?> props = context.getProperties();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "activate", props);

        String contextSvcName = (String) props.get(JNDI_NAME);
        if (contextSvcName == null)
            contextSvcName = (String) props.get(CONFIG_ID);

        lock.writeLock().lock();
        try {
            componentContext = context;
            properties = props;
            name = contextSvcName;
        } finally {
            lock.writeLock().unlock();
        }

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "activate");
    }

    /**
     * This notification is sent by the base instance (if any) when it is modified
     * so that we can uninitialize, so that the next time we are used we can honor
     * the updates to the base instance.
     */
    private void baseInstanceModified() {
        ContextServiceImpl[] listeners;

        lock.writeLock().lock();
        try {
            listeners = modificationListeners.toArray(new ContextServiceImpl[modificationListeners.size()]);
            modificationListeners.clear();
            threadContextConfigurations = null;
        } finally {
            lock.writeLock().unlock();
        }

        for (ContextServiceImpl listener : listeners)
            listener.baseInstanceModified();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Trivial
    public ThreadContextDescriptor captureThreadContext(Map<String, String> executionProperties,
                                                        Map<String, ?>... additionalThreadContextConfig) {
        return captureThreadContext(executionProperties, null, null, additionalThreadContextConfig);
    }

    /**
     * Capture thread context.
     *
     * @param execProps execution properties. Custom property keys must not begin with "javax.enterprise.concurrent."
     * @param task the task for which we are capturing context. This is optional and is used to compute a default value for the IDENTITY_NAME execution property.
     * @param internalNames list to be updated with names of internally added execution properties. Null if this information should not be tracked.
     * @param additionalThreadContextConfig list of additional thread context configurations to use when capturing thread context.
     * @return captured thread context.
     */
    private ThreadContextDescriptor captureThreadContext(Map<String, String> execProps, Object task, Set<String> internalNames,
                                                         Map<String, ?>... additionalThreadContextConfig) {
        execProps = execProps == null ? new TreeMap<String, String>() : new TreeMap<String, String>(execProps);
        if (internalNames == null || !execProps.containsKey(TASK_OWNER)) {
            execProps.put(TASK_OWNER, name);
            if (internalNames != null)
                internalNames.add(TASK_OWNER);
        }
        if (task != null && (internalNames == null || !execProps.containsKey(ManagedTask.IDENTITY_NAME))) {
            execProps.put(ManagedTask.IDENTITY_NAME, task.getClass().getName());
            if (internalNames != null)
                internalNames.add(ManagedTask.IDENTITY_NAME);
        }

        lock.readLock().lock();
        try {
            if (threadContextConfigurations == null) {
                // Switch to write lock for lazy initialization
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (threadContextConfigurations == null)
                        init();
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }

            Map<String, Map<String, ?>> threadContextConfig = threadContextConfigurations;
            if (additionalThreadContextConfig != null && additionalThreadContextConfig.length > 0) {
                threadContextConfig = new HashMap<String, Map<String, ?>>();
                threadContextConfig.putAll(threadContextConfigurations);
                for (Map<String, ?> config : additionalThreadContextConfig) {
                    String providerName = (String) config.get(THREAD_CONTEXT_PROVIDER);
                    if (providerName == null)
                        throw new IllegalArgumentException("additionalThreadContextConfig: " + config.toString());
                    threadContextConfig.put(providerName, config);
                }
            }
            return threadContextMgr.captureThreadContext(threadContextConfig, execProps);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T createContextualProxy(ThreadContextDescriptor threadContextDescriptor, T instance, Class<T> intf) {
        return threadContextMgr.createContextualProxy(threadContextDescriptor, instance, intf);
    }

    /**
     * @see javax.enterprise.concurrent.ContextService#createContextualProxy(java.lang.Object, java.lang.Class<?>[])
     */
    @Override
    @Trivial
    public Object createContextualProxy(Object instance, Class<?>... interfaces) {
        return createContextualProxy(instance, null, interfaces);
    }

    /**
     * @see javax.enterprise.concurrent.ContextService#createContextualProxy(java.lang.Object, java.util.Map, java.lang.Class<?>[])
     */
    @Override
    public Object createContextualProxy(final Object instance, Map<String, String> executionProperties, final Class<?>... interfaces) {
        // validation
        if (interfaces == null || interfaces.length == 0)
            throw new IllegalArgumentException(interfaces == null ? null : Arrays.asList(interfaces).toString());
        for (Class<?> intf : interfaces)
            if (intf == null || !intf.isInstance(instance))
                throw new IllegalArgumentException(instance + ", " + (intf == null ? null : intf.getName()));

        Set<String> internalPropNames = executionProperties == null ? null : new HashSet<String>();
        @SuppressWarnings("unchecked")
        ThreadContextDescriptor threadContextDescriptor = captureThreadContext(executionProperties, instance, internalPropNames);

        Object proxy = null;
        // optimization for Callable/Runnable
        if (interfaces.length == 1)
            if (Callable.class.equals(interfaces[0]))
                proxy = new ContextualCallable<Object>(threadContextDescriptor, (Callable<Object>) instance, internalPropNames);
            else if (Runnable.class.equals(interfaces[0]))
                proxy = new ContextualRunnable(threadContextDescriptor, (Runnable) instance, internalPropNames);

        if (proxy == null) {
            final InvocationHandler handler = new ContextualInvocationHandler(threadContextDescriptor, instance, internalPropNames);
            proxy = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    return Proxy.newProxyInstance(instance.getClass().getClassLoader(), interfaces, handler);
                }
            });
        }

        return proxy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T createContextualProxy(T instance, Class<T> intf) {
        @SuppressWarnings("unchecked")
        ThreadContextDescriptor threadContextDescriptor = captureThreadContext(null, instance, null);
        return threadContextMgr.createContextualProxy(threadContextDescriptor, instance, intf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T createContextualProxy(T instance, Map<String, String> executionProperties, final Class<T> intf) {
        Set<String> internalPropNames = executionProperties == null ? null : new HashSet<String>();
        @SuppressWarnings("unchecked")
        ThreadContextDescriptor threadContextDescriptor = captureThreadContext(executionProperties, instance, internalPropNames);

        if (intf == null || !intf.isInstance(instance))
            throw new IllegalArgumentException(instance + ", " + (intf == null ? null : intf.getName()));

        T proxy;
        if (Callable.class.equals(intf)) {
            @SuppressWarnings("unchecked")
            Callable<Object> callable = (Callable<Object>) instance;
            proxy = intf.cast(new ContextualCallable<Object>(threadContextDescriptor, callable, internalPropNames));
        } else if (Runnable.class.equals(intf)) {
            proxy = intf.cast(new ContextualRunnable(threadContextDescriptor, (Runnable) instance, internalPropNames));
        } else {
            final InvocationHandler handler = new ContextualInvocationHandler(threadContextDescriptor, instance, internalPropNames);
            proxy = AccessController.doPrivileged(new PrivilegedAction<T>() {
                @Override
                public T run() {
                    return intf.cast(Proxy.newProxyInstance(intf.getClassLoader(), new Class<?>[] { intf }, handler));
                }
            });
        }
        return proxy;
    }

    /**
     * @see com.ibm.wsspi.resource.ResourceFactory#createResource(com.ibm.ws.resource.ResourceInfo)
     */
    @Override
    public Object createResource(ResourceInfo ref) throws Exception {
        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cData != null)
            applications.add(cData.getJ2EEName().getApplication());

        return this;
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param context for this component instance
     */
    @Deactivate
    protected void deactivate(ComponentContext context) {
        lock.writeLock().lock();
        try {
            componentContext = null;
            properties = null;
            threadContextConfigurations = null;
        } finally {
            lock.writeLock().unlock();
        }
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
     * Adds each thread context configuration from this - the base instance - to a another context service
     * if the thread context configuration is not already present on the context service.
     * Precondition: invoker must have a write lock on the contextSvc parameter.
     *
     * @param contextSvc ContextService that is using this instance as a base instance.
     */
    private void addComplementaryThreadContextConfigurationsTo(ContextServiceImpl contextSvc) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        lock.writeLock().lock();
        try {
            // Detect and stop infinite recursion from baseContextRef
            if (lock.getWriteHoldCount() > 1) {
                IllegalArgumentException x = ignoreWarnOrFail(null, IllegalArgumentException.class, "CWWKC1020.baseContextRef.infinite", name);
                if (x == null)
                    return;
                else
                    throw x;
            } else if (threadContextConfigurations == null)
                init();

            modificationListeners.add(contextSvc);

            for (Map.Entry<String, Map<String, ?>> threadContextConfig : threadContextConfigurations.entrySet()) {
                String name = threadContextConfig.getKey();
                if (!contextSvc.threadContextConfigurations.containsKey(name)) {
                    contextSvc.threadContextConfigurations.put(name, threadContextConfig.getValue());
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "adding " + name, threadContextConfig.getValue());
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Names of methods to which we should apply context.
     * When not configured, this returns null, in which case the invoker should
     * default to all methods that aren't defined on java.lang.Object.
     * So, for example, myTask.doSomething would run with context but .toString or .equals would not.
     *
     * @return list of methods to which we should apply context. Null for default.
     */
    @Trivial
    Collection<String> getContextualMethods() {
        lock.readLock().lock();
        try {
            @SuppressWarnings("unchecked")
            Collection<String> contextualMethods = (Collection<String>) properties.get(CONTEXTUAL_METHODS);
            return contextualMethods;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @see javax.enterprise.concurrent.ContextService#getExecutionProperties(java.lang.Object)
     */
    @Override
    public Map<String, String> getExecutionProperties(final Object contextualProxy) {
        ContextualObject<?> contextualObject = null;
        if (contextualProxy != null && Proxy.isProxyClass(contextualProxy.getClass())) {
            InvocationHandler handler = AccessController.doPrivileged(new PrivilegedAction<InvocationHandler>() {
                @Override
                public InvocationHandler run() {
                    return Proxy.getInvocationHandler(contextualProxy);
                }
            });
            if (handler instanceof ContextualObject)
                contextualObject = (ContextualObject<?>) handler;
        } else if (contextualProxy instanceof ContextualObject)
            contextualObject = (ContextualObject<?>) contextualProxy;

        if (contextualObject == null)
            throw new IllegalArgumentException(contextualProxy == null ? null : contextualProxy.getClass().getName());

        return contextualObject.getExecutionProperties();
    }

    /**
     * Ignore, warn, or fail when a configuration error occurs.
     * This is copied from Tim's code in tWAS and updated slightly to
     * override with the Liberty ignore/warn/fail setting.
     * Precondition: invoker must have lock on this context service, in order to read the onError property.
     *
     * @param throwable an already created Throwable object, which can be used if the desired action is fail.
     * @param exceptionClassToRaise the class of the Throwable object to return
     * @param msgKey the NLS message key
     * @param objs list of objects to substitute in the NLS message
     * @return either null or the Throwable object
     */
    private <T extends Throwable> T ignoreWarnOrFail(Throwable throwable, final Class<T> exceptionClassToRaise, String msgKey, Object... objs) {

        // Read the value each time in order to allow for changes to the onError setting
        switch ((OnError) properties.get(OnErrorUtil.CFG_KEY_ON_ERROR)) {
            case IGNORE:
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(this, tc, "ignoring error: " + msgKey, objs);
                return null;
            case WARN:
                Tr.warning(tc, msgKey, objs);
                return null;
            case FAIL:
                try {
                    if (throwable != null && exceptionClassToRaise.isInstance(throwable))
                        return exceptionClassToRaise.cast(throwable);

                    Constructor<T> con = AccessController.doPrivileged(new PrivilegedExceptionAction<Constructor<T>>() {
                        @Override
                        public Constructor<T> run() throws NoSuchMethodException {
                            return exceptionClassToRaise.getConstructor(String.class);
                        }
                    });
                    String message = msgKey == null ? throwable.getMessage() : Tr.formatMessage(tc, msgKey, objs);
                    T failure = con.newInstance(message);
                    failure.initCause(throwable);
                    return failure;
                } catch (PrivilegedActionException e) {
                    throw new RuntimeException(e.getCause());
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
        }

        return null;
    }

    /**
     * Lazy initialization.
     * Precondition: invoker must have write lock on this context service
     */
    private void init() {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Map<String, Map<String, Object>> threadContextConfigs = new HashMap<String, Map<String, Object>>();

        for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            // Properties of flattened thread context configurations have the form:
            // threadContextConfigRef.0.*
            if (key.length() > 25 && key.charAt(22) == '.') {
                int dot = key.indexOf('.', 23);
                if (dot > 0) {
                    String group = key.substring(23, dot);
                    Map<String, Object> config = threadContextConfigs.get(group);
                    if (config == null)
                        threadContextConfigs.put(group, config = new TreeMap<String, Object>());
                    config.put(key.substring(dot + 1), properties.get(key));
                }
            } else if (trace && tc.isDebugEnabled() && !SUPPORTED_PROPERTIES.contains(key) && key.indexOf('.') < 0) {
                Tr.debug(this, tc, "unrecognized attribute: " + key);
                // TODO: once we have stricter variant of onError, do the following for it:
                //IllegalArgumentException x = ignoreWarnOrFail(null, IllegalArgumentException.class, "CWWKC1000.unrecognized.property", name, key);
                //if (x != null)
                //    throw x;
            }
        }

        threadContextConfigurations = new TreeMap<String, Map<String, ?>>();
        for (Map<String, Object> threadContextConfig : threadContextConfigs.values()) {
            String provider = (String) threadContextConfig.get("threadContextProvider");
            if (provider == null)
                provider = (String) threadContextConfig.get("config.referenceType") + ".provider";
            Map<String, ?> previous = threadContextConfigurations.put(provider, threadContextConfig);
            if (previous != null)
                throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKC1002.provider.cardinality.violation", name, previous.get("config.referenceType")));
        }

        // Inherit complementary thread context config from base instance
        ContextServiceImpl baseInstance = AccessController.doPrivileged(new PrivilegedAction<ContextServiceImpl>() {
            @Override
            public ContextServiceImpl run() {
                return (ContextServiceImpl) componentContext.locateService(BASE_INSTANCE);
            }
        });
        if (baseInstance != null)
            baseInstance.addComplementaryThreadContextConfigurationsTo(this);

        if (trace && tc.isDebugEnabled())
            Tr.debug(this, tc, "context configuration", threadContextConfigurations);
    }

    /**
     * Called by Declarative Services to modify service config properties
     *
     * @param context DeclarativeService defined/populated component context
     */
    @Trivial
    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<String, ?> props = context.getProperties();
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc, "modified", props);

        String contextSvcName = (String) props.get(JNDI_NAME);
        if (contextSvcName == null)
            contextSvcName = (String) props.get(CONFIG_ID);

        ContextServiceImpl[] listeners;

        lock.writeLock().lock();
        try {
            listeners = modificationListeners.toArray(new ContextServiceImpl[modificationListeners.size()]);
            modificationListeners.clear();

            componentContext = context;
            properties = props;
            name = contextSvcName;
            threadContextConfigurations = null;
        } finally {
            lock.writeLock().unlock();
        }

        for (ContextServiceImpl listener : listeners)
            listener.baseInstanceModified();

        if (trace && tc.isEntryEnabled())
            Tr.exit(this, tc, "modified");
    }

    /**
     * Declarative Services method for setting the service reference to the base contextService instance.
     *
     * @param ref reference to the service
     */
    @Reference(name = BASE_INSTANCE,
               service = ContextService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               target = "(id=unbound)")
    protected void setBaseInstance(ServiceReference<ContextService> ref) {
        lock.writeLock().lock();
        try {
            threadContextConfigurations = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Declarative Services method for setting the thread context manager.
     *
     * @param svc the service
     */
    @Reference(name = THREAD_CONTEXT_MANAGER,
               service = WSContextService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.STATIC,
               target = "(component.name=com.ibm.ws.context.manager)")
    protected void setThreadContextManager(WSContextService svc) {
        threadContextMgr = (ThreadContextManager) svc;
    }

    /**
     * Declarative Services method for unsetting the service reference to the base contextService instance.
     *
     * @param ref reference to the service
     */
    protected void unsetBaseInstance(ServiceReference<ContextService> ref) {
        lock.writeLock().lock();
        try {
            threadContextConfigurations = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Declarative Services method for unsetting the thread context manager.
     *
     * @param svc the service
     */
    protected void unsetThreadContextManager(WSContextService svc) {
        threadContextMgr = null;
    }
}