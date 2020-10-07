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
package com.ibm.ws.context.service.serializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.RejectedExecutionException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.threadcontext.ThreadContext;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializationInfo;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

/**
 * Represents captured thread context
 */
public class ThreadContextDescriptorImpl implements ThreadContextDescriptor, ThreadContextDeserializationInfo {
    private static final TraceComponent tc = Tr.register(ThreadContextDescriptorImpl.class);

    /**
     * Execution properties.
     */
    private final Map<String, String> execProps;

    /**
     * MetaData identifier for the component. Can be null.
     */
    private final String metaDataIdentifier;

    /**
     * Names of thread context providers. Ordering must match the order of the threadContext list.
     */
    private ArrayList<String> providerNames;

    /**
     * List of thread context providers to completely ignore.
     * We will neither capture thread context nor apply any default thread context for these thread context providers.
     */
    List<String> providerNamesToSkip;

    /**
     * Captured thread context. Ordering must match the order of the providerNames list.
     */
    private ArrayList<ThreadContext> threadContext;

    /**
     * Thread context manager.
     */
    private ThreadContextManager threadContextMgr;

    /**
     * Construct a thread context descriptor from bytes.
     *
     * @param execProps execution properties
     * @param bytes serialized bytes
     * @throws IOException if a deserialization error occurs.
     */
    public ThreadContextDescriptorImpl(Map<String, String> execProps, @Sensitive byte[] bytes) throws ClassNotFoundException, IOException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();
        this.execProps = execProps = execProps == null ? new TreeMap<String, String>() : new TreeMap<String, String>(execProps);

        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bytes));

        // Number of fields
        short numFields = oin.readShort();

        // 1) Bytes for captured thread context
        final byte[][] contextBytes = (byte[][]) oin.readObject();

        // 2) Metadata identifier
        metaDataIdentifier = numFields < 2 ? null : (String) oin.readObject();

        // 3) Thread context provider names
        int numProviderNames = numFields < 3 ? 0 : oin.readShort();
        providerNames = new ArrayList<String>(numProviderNames);
        for (int i = 0; i < numProviderNames; i++) {
            String name = (String) oin.readObject();
            if (name.charAt(0) == '.' && name.charAt(name.length() - 1) == '.')
                providerNames.add("com.ibm.ws" + name + "context.provider");
            else
                providerNames.add(name);
        }

        // Ignore anything else

        oin.close();

        threadContext = new ArrayList<ThreadContext>(contextBytes.length);

        String skip = execProps.get(WSContextService.SKIP_CONTEXT_PROVIDERS);
        providerNamesToSkip = skip == null ? Collections.<String> emptyList() : Arrays.asList(skip.split(","));

        // these single element arrays are used to allow the inner class for the privileged action to return multiple values
        @SuppressWarnings("unchecked")
        final ServiceReference<WSContextService>[] threadContextMgrRef = new ServiceReference[1];
        final BundleContext[] bundleContext = new BundleContext[1];

        boolean successful = false;
        try {
            ThreadContextProvider[] contextProviders = AccessController.doPrivileged(new PrivilegedExceptionAction<ThreadContextProvider[]>() {
                @Override
                public ThreadContextProvider[] run() throws InvalidSyntaxException {
                    ThreadContextProvider[] contextProviders = new ThreadContextProvider[contextBytes.length];
                    bundleContext[0] = FrameworkUtil.getBundle(ThreadContextManager.class).getBundleContext();
                    threadContextMgrRef[0] = bundleContext[0].getServiceReferences(WSContextService.class, "(component.name=com.ibm.ws.context.manager)").iterator().next();
                    threadContextMgr = (ThreadContextManager) bundleContext[0].getService(threadContextMgrRef[0]);

                    // Locate the thread context provider corresponding to each type of thread context
                    for (int i = 0; i < contextBytes.length; i++) {
                        String providerName = providerNames.get(i);
                        ThreadContextProvider contextProvider = threadContextMgr.threadContextProviders.getService(providerName);
                        if (contextProvider == null && !"com.ibm.ws.concurrent.mp.cleared.context.provider".equals(providerName))
                            throw new IllegalStateException(Tr.formatMessage(tc, "CWWKC1004.context.provider.unavailable", providerName));
                        contextProviders[i] = contextProvider;
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, providerName, contextProvider);
                    }
                    return contextProviders;
                }
            });

            // Have each context provider deserialize its own context, so that the correct class loader is used.
            for (int i = 0; i < contextBytes.length; i++) {
                if (contextProviders[i] != null) {
                    ThreadContext context = contextProviders[i].deserializeThreadContext(this, contextBytes[i]);
                    threadContext.add(context);
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, contextProviders[i].toString(), toString(context));
                }
            }
            successful = true;
        } catch (PrivilegedActionException x) {
            throw new IOException(x.getCause());
        } finally {
            if (!successful && threadContextMgrRef[0] != null)
                bundleContext[0].ungetService(threadContextMgrRef[0]);
        }
    }

    /**
     * Construct a thread context descriptor.
     *
     * @param execProps execution properties
     * @param internalExecPropNames names of internal execution properties. Null if all execution properties are internal.
     * @param initialCapacity expected maximum count of thread context types
     * @param threadContextMgr thread context manager
     */
    @Trivial
    ThreadContextDescriptorImpl(Map<String, String> execProps, int initialCapacity, ThreadContextManager threadContextMgr) {
        this.execProps = execProps;
        this.providerNames = new ArrayList<String>(initialCapacity);
        this.threadContext = new ArrayList<ThreadContext>(initialCapacity);
        this.threadContextMgr = threadContextMgr;

        String skip = execProps.get(WSContextService.SKIP_CONTEXT_PROVIDERS);
        providerNamesToSkip = skip == null ? Collections.<String> emptyList() : Arrays.asList(skip.split(","));

        ComponentMetaData cData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        metaDataIdentifier = cData == null ? null : threadContextMgr.metadataIdentifierService.getMetaDataIdentifier(cData);
    }

    /**
     * {@inheritDoc}
     */
    final void add(String providerName, ThreadContext context) {
        providerNames.add(providerName);
        threadContext.add(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ThreadContextDescriptor clone() {
        try {
            ThreadContextDescriptorImpl clone = (ThreadContextDescriptorImpl) super.clone();
            clone.providerNames = new ArrayList<String>(clone.providerNames);
            clone.providerNamesToSkip = new ArrayList<String>(clone.providerNamesToSkip);
            clone.threadContext = new ArrayList<ThreadContext>(threadContext);
            return clone;
        } catch (CloneNotSupportedException x) {
            throw new RuntimeException(x); // should never occur
        }
    }

    /**
     * Utility method that indicates whether or not a list of thread context providers contains all of the specified prerequisites.
     *
     * @param contextProviders list of thread context providers (actually a map, but the keys are used as a list)
     * @param prereqs prerequisite thread context providers
     * @return true if all prerequisites are met. Otherwise false.
     */
    @Trivial
    private static final boolean containsAll(LinkedHashMap<ThreadContextProvider, ThreadContext> contextProviders, List<ThreadContextProvider> prereqs) {
        for (ThreadContextProvider prereq : prereqs)
            if (!contextProviders.containsKey(prereq))
                return false;
        return true;
    }

    @Override
    @Trivial
    public final Map<String, String> getExecutionProperties() {
        return execProps;
    }

    /**
     * Raises IllegalStateException because the application or application component is unavailable.
     *
     * @param jeeName The metadata identifier, which is the JEE name (Application/Module/Component name with parts separated by hash signs).
     *            For now, we'll parse the string and issue the appropriate message. This may not be appropriate in the future.
     * @param taskName identifier for the task or contextual operation that cannot be performed
     * @throws IllegalStateException indicating that the task cannot run because the application or application component is not available.
     */
    public static void notAvailable(String jeeName, String taskName) {
        String message;
        int modSepIndex = jeeName.indexOf('#');
        if (modSepIndex == -1) {
            message = Tr.formatMessage(tc, "CWWKC1011.app.unavailable", taskName, jeeName);
        } else {
            String application = jeeName.substring(0, modSepIndex);
            int compSepIndex = jeeName.indexOf('#', modSepIndex + 1);
            if (compSepIndex == -1) {
                message = Tr.formatMessage(tc, "CWWKC1012.module.unavailable", taskName, jeeName.substring(modSepIndex + 1), application);
            } else {
                String module = jeeName.substring(modSepIndex + 1, compSepIndex);
                message = Tr.formatMessage(tc, "CWWKC1013.component.unavailable", taskName, jeeName.substring(compSepIndex + 1), module, application);
            }
        }
        throw new IllegalStateException(message);
    }

    /**
     * Serializes this thread context descriptor to bytes.
     *
     * @return serialized bytes representing the thread context descriptor.
     * @throws IOException if a serialization error occurs.
     */
    @Override
    public @Sensitive byte[] serialize() throws IOException {
        // Captured thread context
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int size = threadContext.size();
        byte[][] contextBytes = new byte[size][];
        for (int i = 0; i < size; i++) {
            bout.reset();
            ObjectOutputStream oout = new ObjectOutputStream(bout);
            oout.writeObject(threadContext.get(i));
            oout.flush();
            contextBytes[i] = bout.toByteArray();
            oout.close();
        }

        bout.reset();
        ObjectOutputStream oout = new ObjectOutputStream(bout);

        // Number of serialized fields (list of thread context counts as one)
        oout.writeShort(3);

        // 1) Bytes for captured thread context
        oout.writeObject(contextBytes);

        // 2) Metadata identifier
        oout.writeObject(metaDataIdentifier);

        // 3) Thread context provider names
        oout.writeShort(providerNames.size());
        for (String providerName : providerNames)
            if (providerName.startsWith("com.ibm.ws.") && providerName.endsWith(".context.provider"))
                oout.writeObject(providerName.substring(10, providerName.length() - 16));
            else
                oout.writeObject(providerName);

        // Only add primitive or compatible Java types in the future. If necessary, pre-serialize to byte[]

        oout.flush();
        oout.close();

        return bout.toByteArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void set(String providerName, ThreadContext context) {
        int index = providerNames.indexOf(providerName);
        if (index >= 0)
            threadContext.set(index, context);
        else {
            providerNames.add(providerName);
            threadContext.add(context);
        }
    }

    /**
     * Establish context on a thread before a contextual operation is started.
     *
     * @return list of thread context matching the order in which context has been applied to the thread.
     * @throws IllegalStateException if the application component is not started or deployed.
     * @throws RejectedExecutionException if context cannot be established on the thread.
     */
    @Override
    public ArrayList<ThreadContext> taskStarting() throws RejectedExecutionException {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        // EE Concurrency 3.3.4: All invocations to any of the proxied interface methods will fail with a
        // java.lang.IllegalStateException exception if the application component is not started or deployed.
        if (!"false".equalsIgnoreCase(execProps.get(WSContextService.REQUIRE_AVAILABLE_APP)) &&
            metaDataIdentifier != null && threadContextMgr.metadataIdentifierService.getMetaData(metaDataIdentifier) == null) {
            String taskName; // ManagedTask.IDENTITY_NAME
            if (threadContextMgr.eeVersion < 9) {
                taskName = execProps.get("javax.enterprise.concurrent.IDENTITY_NAME");
                if (taskName == null)
                    taskName = execProps.get("jakarta.enterprise.concurrent.IDENTITY_NAME");
            } else {
                taskName = execProps.get("jakarta.enterprise.concurrent.IDENTITY_NAME");
                if (taskName == null)
                    taskName = execProps.get("javax.enterprise.concurrent.IDENTITY_NAME");
            }
            notAvailable(metaDataIdentifier, taskName);
        }

        String defaultContextTypes = execProps.get(WSContextService.DEFAULT_CONTEXT);
        final Set<String> providerNamesForDefaultContext =
                        WSContextService.UNCONFIGURED_CONTEXT_TYPES.equals(defaultContextTypes) || WSContextService.ALL_CONTEXT_TYPES.equals(defaultContextTypes)
                                        ? new HashSet<String>(threadContextMgr.threadContextProviders.keySet())
                                        : Collections.<String> emptySet();
        providerNamesForDefaultContext.removeAll(providerNamesToSkip);

        LinkedHashMap<ThreadContextProvider, ThreadContext> contextAppliedToThread = new LinkedHashMap<ThreadContextProvider, ThreadContext>();
        try {
            // lazily obtaining services is a privileged operation
            Map<ThreadContextProvider, ThreadContext> contextNotApplied = AccessController.doPrivileged(new PrivilegedAction<Map<ThreadContextProvider, ThreadContext>>() {
                @Override
                public Map<ThreadContextProvider, ThreadContext> run() {
                    Map<ThreadContextProvider, ThreadContext> contextNotApplied = new LinkedHashMap<ThreadContextProvider, ThreadContext>();

                    // First pass through configured context, and figure out what default context is needed
                    for (int i = 0; i < threadContext.size(); i++) {
                        String providerName = providerNames.get(i);
                        providerNamesForDefaultContext.remove(providerName);
                        contextNotApplied.put(threadContextMgr.threadContextProviders.getServiceWithException(providerName), threadContext.get(i));
                    }

                    // First pass through default context
                    for (String providerName : providerNamesForDefaultContext)
                        contextNotApplied.put(threadContextMgr.threadContextProviders.getServiceWithException(providerName), null);

                    return contextNotApplied;
                }
            });

            // Make multiple passes through context that hasn't been applied until the size stops changing
            for (int count = contextNotApplied.size(), prev = count + 1; count > 0 && count < prev; prev = count, count = contextNotApplied.size())
                for (Iterator<Entry<ThreadContextProvider, ThreadContext>> it = contextNotApplied.entrySet().iterator(); it.hasNext();) {
                    Entry<ThreadContextProvider, ThreadContext> entry = it.next();
                    ThreadContextProvider provider = entry.getKey();
                    List<ThreadContextProvider> prereqs = provider.getPrerequisites();
                    if (prereqs == null || containsAll(contextAppliedToThread, prereqs)) {
                        ThreadContext context = entry.getValue();
                        context = context == null
                                        ? provider.createDefaultThreadContext(execProps)
                                        : context.clone();
                        if (trace && tc.isDebugEnabled())
                            Tr.debug(this, tc, "taskStarting " + toString(context));
                        context.taskStarting();
                        contextAppliedToThread.put(provider, context);
                        it.remove();
                    }
                }

            if (contextNotApplied.size() > 0)
                throw new IllegalStateException(contextNotApplied.keySet().toString());
        } catch (Throwable x) {
            // In the event of failure, undo all context propagation up to this point.
            ArrayList<ThreadContext> contextToRemove = new ArrayList<ThreadContext>(contextAppliedToThread.values());
            for (int c = contextToRemove.size() - 1; c >= 0; c--)
                try {
                    if (trace && tc.isDebugEnabled())
                        Tr.debug(this, tc, "taskStopping " + toString(contextToRemove.get(c)));
                    contextToRemove.get(c).taskStopping();
                } catch (Throwable stopX) {
                }

            if (x instanceof RejectedExecutionException)
                throw (RejectedExecutionException) x;
            if (x instanceof RuntimeException)
                throw (RuntimeException) x;
            if (x instanceof Error)
                throw (Error) x;
            throw new RejectedExecutionException(x);
        }

        return new ArrayList<ThreadContext>(contextAppliedToThread.values());
    }

    /**
     * Remove context from the thread (in reverse of the order in which is was applied) after a contextual operation completes.
     *
     * @param threadContext list of context previously applied to thread, ordered according to the order in which it was applied to the thread.
     */
    @Override
    public void taskStopping(ArrayList<ThreadContext> threadContext) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        Throwable failure = null;
        for (int c = threadContext.size() - 1; c >= 0; c--)
            try {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "taskStopping " + toString(threadContext.get(c)));
                threadContext.get(c).taskStopping();
            } catch (Throwable x) {
                if (failure == null)
                    failure = x;
            }

        if (failure != null)
            if (failure instanceof RuntimeException)
                throw (RuntimeException) failure;
            else if (failure instanceof Error)
                throw (Error) failure;
            else
                throw new RuntimeException(failure); // should never happen
    }

    /**
     * Returns nicely formatted text describing this thread context provider.
     *
     * @return nicely formatted text describing this thread context provider.
     */
    @Override
    @Trivial
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('@').append(Integer.toHexString(System.identityHashCode(this)))
                        .append(providerNames)
                        .toString();
    }

    /**
     * Returns text that uniquely identifies a context instance. This is only used for tracing.
     *
     * @param c a context
     * @return text formatted to include the class name and hashcode.
     */
    @Trivial
    private static String toString(ThreadContext c) {
        if (c == null)
            return null;
        String s = c.toString();
        if (s.indexOf('@') < 0)
            s = c.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(c)) + ':' + s;
        return s;
    }

    /** Deserialization info implementation */
    @Override
    public String getExecutionProperty(String name) {
        return execProps.get(name);
    }

    /** Deserialization info implementation */
    @Override
    public String getMetadataIdentifier() {
        return metaDataIdentifier;
    }
}
