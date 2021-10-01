/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.concurrent.ContextService;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
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
import com.ibm.ws.concurrent.ContextualAction;
import com.ibm.ws.concurrent.mp.spi.ThreadContextConfig;
import com.ibm.ws.context.service.serializable.ContextualInvocationHandler;
import com.ibm.ws.context.service.serializable.ContextualObject;
import com.ibm.ws.context.service.serializable.ThreadContextManager;
import com.ibm.ws.javaee.version.JavaEEVersion;
import com.ibm.ws.kernel.service.util.SecureAction;
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
 * This class implements the Jakarta/Java EE ContextService as well as MicroProfile ThreadContext.
 */
@Component(name = "com.ibm.ws.context.service",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           service = { ResourceFactory.class, ContextService.class, ThreadContext.class, WSContextService.class, ApplicationRecycleComponent.class },
           property = { "creates.objectClass=javax.enterprise.concurrent.ContextService",
                        "creates.objectClass=org.eclipse.microprofile.context.ThreadContext" })
public class ContextServiceImpl implements ContextService, //
                ResourceFactory, ThreadContext, WSContextService, ApplicationRecycleComponent {
    private static final TraceComponent tc = Tr.register(ContextServiceImpl.class);

    private static final SecureAction priv = AccessController.doPrivileged(SecureAction.get());

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
                                                                           "javaCompDefaultName", // for java:comp/DefaultContextService
                                                                           JNDI_NAME,
                                                                           Constants.OBJECTCLASS,
                                                                           OnErrorUtil.CFG_KEY_ON_ERROR);

    /**
     * Component context for this contextService instance.
     * Populated only when used as a declarative services component (not for MicroProfile builders).
     */
    private ComponentContext componentContext;

    /**
     * Jakarta EE version if Jakarta EE 9 or higher. If 0, assume a lesser EE spec version.
     */
    private volatile int eeVersion;

    /**
     * Tracks the most recently bound EE version service reference. Only use this within the set/unsetEEVersion methods.
     */
    private ServiceReference<JavaEEVersion> eeVersionRef;

    /**
     * Hash code for this instance.
     */
    private final int hash;

    /**
     * Lock for reading and updating configuration.
     * Populated only when used as a declarative services component (not for MicroProfile builders).
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * MicroProfile ManagedExecutor that uses this ThreadContext. Otherwise null.
     * TODO this will be used for Jakarta EE 10 as well
     */
    ManagedExecutor managedExecutor;

    /**
     * These listeners (other contextService instances which are using this instance as the base instance)
     * need to be notified when we are modified so that they can uninitialize and pick up the new configuration
     * the next time they are used.
     * Populated only when used as a declarative services component (not for MicroProfile builders).
     */
    private final List<ContextServiceImpl> modificationListeners = new LinkedList<ContextServiceImpl>();

    /**
     * Represents the context propagation settings that are configured on the MicroProfile builder.
     * Null when used as a declarative services component.
     */
    private final ThreadContextConfig mpBuilderConfig;

    /**
     * Name of this thread context service.
     * When used as a declarative services component, the name is the jndiName if specified, otherwise the config id.
     * For MicroProfile builders, it is precomputed by the builder.
     */
    protected String name; // TODO this is temporarily switched from private to protected in order to accommodate test case

    /**
     * Service properties.
     * Populated only when used as a declarative services component (not for MicroProfile builders).
     */
    private Dictionary<String, ?> properties;

    /**
     * Map of thread context provider name to configured thread context.
     * Populated only when used as a declarative services component (not for MicroProfile builders).
     *
     * This value will be NULL when the context service hasn't (re)initialized yet.
     */
    private Map<String, Map<String, ?>> threadContextConfigurations;

    /**
     * Centralized service that holds all of the registered thread context providers.
     * Populated only when used as a declarative services component (not for MicroProfile builders).
     */
    private ThreadContextManager threadContextMgr;

    /**
     * Names of applications using this ResourceFactory
     */
    private final Set<String> applications = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * Constructor when used as a declarative services component.
     */
    public ContextServiceImpl() {
        this.hash = super.hashCode();
        this.mpBuilderConfig = null; // for MicroProfile builders only
    }

    /**
     * Constructor for MicroProfile builders.
     *
     * @param name      unique name for this instance.
     * @param int       hash hash code for this instance.
     * @param eeVersion Jakarta/Java EE version that is enabled in the Liberty server.
     * @param config    represents thread context propagation configuration.
     */
    public ContextServiceImpl(String name, int hash, int eeVersion, ThreadContextConfig config) {
        this.name = name;
        this.hash = hash;
        this.eeVersion = eeVersion;
        this.mpBuilderConfig = config;
    }

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
                                                        @SuppressWarnings("unchecked") Map<String, ?>... additionalThreadContextConfig) {
        if (mpBuilderConfig == null)
            return captureThreadContext(executionProperties, null, null, additionalThreadContextConfig);
        else
            return mpBuilderConfig.captureThreadContext();
    }

    /**
     * Capture thread context.
     * This is for the declarative services path only (not for MicroProfile builders).
     *
     * @param execProps                     execution properties. Custom property keys must not begin with "javax.enterprise.concurrent."
     * @param task                          the task for which we are capturing context. This is optional and is used to compute a default value for the IDENTITY_NAME execution
     *                                          property.
     * @param internalNames                 list to be updated with names of internally added execution properties. Null if this information should not be tracked.
     * @param additionalThreadContextConfig list of additional thread context configurations to use when capturing thread context.
     * @return captured thread context.
     */
    private ThreadContextDescriptor captureThreadContext(Map<String, String> execProps, Object task, Set<String> internalNames,
                                                         @SuppressWarnings("unchecked") Map<String, ?>... additionalThreadContextConfig) {
        // The createContextualProxy methods are not supported on instances that were
        // created by MicroProfile builders because MicroProfile ThreadContextProviders
        // do not support serialization of thread context.
        if (mpBuilderConfig != null)
            throw new UnsupportedOperationException();

        execProps = execProps == null ? new TreeMap<String, String>() : new TreeMap<String, String>(execProps);
        if (internalNames == null || !execProps.containsKey(TASK_OWNER)) {
            execProps.put(TASK_OWNER, name);
            if (internalNames != null)
                internalNames.add(TASK_OWNER);
        }
        if (task != null && (internalNames == null ||
                             !(execProps.containsKey("jakarta.enterprise.concurrent.IDENTITY_NAME") ||
                               execProps.containsKey("javax.enterprise.concurrent.IDENTITY_NAME")))) {
            String key = eeVersion < 9 ? "javax.enterprise.concurrent.IDENTITY_NAME" : "jakarta.enterprise.concurrent.IDENTITY_NAME";
            execProps.put(key, task.getClass().getName());
            if (internalNames != null)
                internalNames.add(key);
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

    @Override
    public <R> Callable<R> contextualCallable(Callable<R> callable) {
        if (callable instanceof ContextualCallable)
            throw new IllegalArgumentException(ContextualCallable.class.getSimpleName());

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = captureThreadContext(Collections.emptyMap());
        return new ContextualCallable<R>(contextDescriptor, callable);
    }

    @Override
    public <T, U> BiConsumer<T, U> contextualConsumer(BiConsumer<T, U> consumer) {
        if (consumer instanceof ContextualBiConsumer)
            throw new IllegalArgumentException(ContextualBiConsumer.class.getSimpleName());

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = captureThreadContext(Collections.emptyMap());
        return new ContextualBiConsumer<T, U>(contextDescriptor, consumer);
    }

    @Override
    public <T> Consumer<T> contextualConsumer(Consumer<T> consumer) {
        if (consumer instanceof ContextualConsumer)
            throw new IllegalArgumentException(ContextualConsumer.class.getSimpleName());

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = captureThreadContext(Collections.emptyMap());
        return new ContextualConsumer<T>(contextDescriptor, consumer);
    }

    @Override
    public <T, U, R> BiFunction<T, U, R> contextualFunction(BiFunction<T, U, R> function) {
        if (function instanceof ContextualBiFunction)
            throw new IllegalArgumentException(ContextualBiFunction.class.getSimpleName());

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = captureThreadContext(Collections.emptyMap());
        return new ContextualBiFunction<T, U, R>(contextDescriptor, function);
    }

    @Override
    public <T, R> Function<T, R> contextualFunction(Function<T, R> function) {
        if (function instanceof ContextualFunction)
            throw new IllegalArgumentException(ContextualFunction.class.getSimpleName());

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = captureThreadContext(Collections.emptyMap());
        return new ContextualFunction<T, R>(contextDescriptor, function);
    }

    @Override
    public Runnable contextualRunnable(Runnable runnable) {
        if (runnable instanceof ContextualRunnable)
            throw new IllegalArgumentException(ContextualRunnable.class.getSimpleName());

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = captureThreadContext(Collections.emptyMap());
        return new ContextualRunnable(contextDescriptor, runnable);
    }

    @Override
    public <R> Supplier<R> contextualSupplier(Supplier<R> supplier) {
        if (supplier instanceof ContextualSupplier)
            throw new IllegalArgumentException(ContextualSupplier.class.getSimpleName());

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = captureThreadContext(Collections.emptyMap());
        return new ContextualSupplier<R>(contextDescriptor, supplier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T createContextualProxy(ThreadContextDescriptor threadContextDescriptor, T instance, Class<T> intf) {
        // The createContextualProxy methods are not supported on instances that were
        // created by MicroProfile builders because MicroProfile ThreadContextProviders
        // do not support serialization of thread context.
        if (mpBuilderConfig != null)
            throw new UnsupportedOperationException();

        return threadContextMgr.createContextualProxy(threadContextDescriptor, instance, intf);
    }

    /**
     * @see javax.enterprise.concurrent.ContextService#createContextualProxy(java.lang.Object, java.lang.Class<?>[])
     */
    @Override
    @Trivial
    public Object createContextualProxy(Object instance, Class<?>... interfaces) {
        if (instance instanceof ContextualAction)
            throw new IllegalArgumentException(instance.getClass().getSimpleName());

        return createContextualProxy(instance, null, interfaces);
    }

    /**
     * @see javax.enterprise.concurrent.ContextService#createContextualProxy(java.lang.Object, java.util.Map, java.lang.Class<?>[])
     */
    @Override
    public Object createContextualProxy(final Object instance, Map<String, String> executionProperties, final Class<?>... interfaces) {
        if (instance instanceof ContextualAction)
            throw new IllegalArgumentException(instance.getClass().getSimpleName());

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
                proxy = new com.ibm.ws.context.service.serializable.ContextualCallable<Object>(threadContextDescriptor, (Callable<Object>) instance, internalPropNames);
            else if (Runnable.class.equals(interfaces[0]))
                proxy = new com.ibm.ws.context.service.serializable.ContextualRunnable(threadContextDescriptor, (Runnable) instance, internalPropNames);

        if (proxy == null) {
            final InvocationHandler handler = new ContextualInvocationHandler(threadContextDescriptor, instance, internalPropNames);
            proxy = AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                @Trivial
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
        if (instance instanceof ContextualAction)
            throw new IllegalArgumentException(instance.getClass().getSimpleName());

        @SuppressWarnings("unchecked")
        ThreadContextDescriptor threadContextDescriptor = captureThreadContext(null, instance, null);
        return threadContextMgr.createContextualProxy(threadContextDescriptor, instance, intf);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T createContextualProxy(T instance, Map<String, String> executionProperties, final Class<T> intf) {
        if (instance instanceof ContextualAction)
            throw new IllegalArgumentException(instance.getClass().getSimpleName());

        Set<String> internalPropNames = executionProperties == null ? null : new HashSet<String>();
        @SuppressWarnings("unchecked")
        ThreadContextDescriptor threadContextDescriptor = captureThreadContext(executionProperties, instance, internalPropNames);

        if (intf == null || !intf.isInstance(instance))
            throw new IllegalArgumentException(instance + ", " + (intf == null ? null : intf.getName()));

        T proxy;
        if (Callable.class.equals(intf)) {
            @SuppressWarnings("unchecked")
            Callable<Object> callable = (Callable<Object>) instance;
            proxy = intf.cast(new com.ibm.ws.context.service.serializable.ContextualCallable<Object>(threadContextDescriptor, callable, internalPropNames));
        } else if (Runnable.class.equals(intf)) {
            proxy = intf.cast(new com.ibm.ws.context.service.serializable.ContextualRunnable(threadContextDescriptor, (Runnable) instance, internalPropNames));
        } else {
            final InvocationHandler handler = new ContextualInvocationHandler(threadContextDescriptor, instance, internalPropNames);
            proxy = AccessController.doPrivileged(new PrivilegedAction<T>() {
                @Override
                @Trivial
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

    @Override
    public Executor currentContextExecutor() {
        @SuppressWarnings("unchecked")
        ThreadContextDescriptor contextDescriptor = captureThreadContext(Collections.emptyMap());
        return new ContextualExecutor(contextDescriptor);
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
     * @see javax.enterprise.concurrent.ContextService#getExecutionProperties(java.lang.Object)
     */
    @Override
    public Map<String, String> getExecutionProperties(final Object contextualProxy) {
        ContextualObject<?> contextualObject = null;
        if (contextualProxy != null && Proxy.isProxyClass(contextualProxy.getClass())) {
            InvocationHandler handler = AccessController.doPrivileged(new PrivilegedAction<InvocationHandler>() {
                @Override
                @Trivial
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

    @Override
    @Trivial
    public final int hashCode() {
        return hash;
    }

    /**
     * Ignore, warn, or fail when a configuration error occurs.
     * This is copied from Tim's code in tWAS and updated slightly to
     * override with the Liberty ignore/warn/fail setting.
     * Precondition: invoker must have lock on this context service, in order to read the onError property.
     *
     * @param throwable             an already created Throwable object, which can be used if the desired action is fail.
     * @param exceptionClassToRaise the class of the Throwable object to return
     * @param msgKey                the NLS message key
     * @param objs                  list of objects to substitute in the NLS message
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
                        @Trivial
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
        ContextServiceImpl baseInstance = (ContextServiceImpl) priv.locateService(componentContext, BASE_INSTANCE);
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
               service = WSContextService.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY,
               target = "(id=unbound)")
    protected void setBaseInstance(ServiceReference<WSContextService> ref) {
        lock.writeLock().lock();
        try {
            threadContextConfigurations = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Declarative Services method for setting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    @Reference(service = JavaEEVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setEEVersion(ServiceReference<JavaEEVersion> ref) {
        String version = (String) ref.getProperty("version");
        if (version == null) {
            eeVersion = 0;
        } else {
            int dot = version.indexOf('.');
            String major = dot > 0 ? version.substring(0, dot) : version;
            eeVersion = Integer.parseInt(major);
        }
        eeVersionRef = ref;
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

    @Override
    @Trivial
    public final String toString() {
        // TODO this preserves the toString for instances built by MicroProfile builders.
        // Should we also include the name (when present) in the toString for EE?
        return mpBuilderConfig == null ? super.toString() : name;
    }

    /**
     * Declarative Services method for unsetting the service reference to the base contextService instance.
     *
     * @param ref reference to the service
     */
    protected void unsetBaseInstance(ServiceReference<WSContextService> ref) {
        lock.writeLock().lock();
        try {
            threadContextConfigurations = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Declarative Services method for unsetting the Jakarta/Java EE version
     *
     * @param ref reference to the service
     */
    protected void unsetEEVersion(ServiceReference<JavaEEVersion> ref) {
        if (eeVersionRef == ref) {
            eeVersionRef = null;
            eeVersion = 0;
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

    @Override
    public <T> CompletableFuture<T> withContextCapture(CompletableFuture<T> stage) {
        CompletableFuture<T> newCompletableFuture;

        Executor executor = MPContextPropagationVersion.atLeast(MPContextPropagationVersion.V1_1) //
                        ? (managedExecutor == null ? new ContextualDefaultExecutor(this) : managedExecutor) //
                        : new UnusableExecutor(this);

        if (ManagedCompletableFuture.JAVA8)
            newCompletableFuture = new ManagedCompletableFuture<T>(new CompletableFuture<T>(), executor, null);
        else
            newCompletableFuture = new ManagedCompletableFuture<T>(executor, null);

        stage.whenComplete((result, failure) -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "whenComplete", result, failure);
            if (failure == null)
                newCompletableFuture.complete(result);
            else
                newCompletableFuture.completeExceptionally(failure);
        });

        return newCompletableFuture;
    }

    @Override
    public <T> CompletionStage<T> withContextCapture(CompletionStage<T> stage) {
        ManagedCompletionStage<T> newStage;

        Executor executor = MPContextPropagationVersion.atLeast(MPContextPropagationVersion.V1_1) //
                        ? (managedExecutor == null ? new ContextualDefaultExecutor(this) : managedExecutor) //
                        : new UnusableExecutor(this);

        if (ManagedCompletableFuture.JAVA8)
            newStage = new ManagedCompletionStage<T>(new CompletableFuture<T>(), executor, null);
        else
            newStage = new ManagedCompletionStage<T>(executor);

        stage.whenComplete((result, failure) -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "whenComplete", result, failure);
            if (failure == null)
                newStage.super_complete(result);
            else
                newStage.super_completeExceptionally(failure);
        });

        return newStage;
    }
}