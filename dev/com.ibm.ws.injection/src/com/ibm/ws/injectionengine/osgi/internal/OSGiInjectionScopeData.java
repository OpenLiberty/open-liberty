/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine.osgi.internal;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.NameClassPair;
import javax.naming.NamingException;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.naming.JavaColonNamespaceBindings;
import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.injectionengine.InjectionScopeData;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * The scope data stored for the primary java namespaces:
 * <ul>
 * <li>java:global - singleton object in injection engine</li>
 * <li>java:app - ApplicationMetaData slot</li>
 * <li>java:module - ModuleMetaData slot</li>
 * <li>java:comp - ComponentMetaData slot, or ModuleMetaData slot for modules
 * without per-component namespaces. Since the scope data stored in the
 * ModuleMetaData slot can be used for both java:module and java:comp, the
 * bindings for these two namespaces are stored in separate fields.</li>
 * </ul>
 */
public class OSGiInjectionScopeData extends InjectionScopeData implements DeferredReferenceData {
    private static final TraceComponent tc = Tr.register(OSGiInjectionScopeData.class);

    /**
     * The "primary" namespace for this scope data, which corresponds to this
     * scope data's storage location:
     * <ul>
     * <li>{@link NamingConstants.JavaColonNamespace#GLOBAL} - singleton</li>
     * <li>{@link NamingConstants.JavaColonNamespace#APP} - ApplicationMetaData</li>
     * <li>{@link NamingConstants.JavaColonNamespace#MODULE} - ModuleMetaData</li>
     * <li>{@link NamingConstants.JavaColonNamespace#COMP} - ComponentMetaData</li>
     * </ul>
     */
    private final NamingConstants.JavaColonNamespace namespace;

    /**
     * The parent scope data: COMP -> MODULE -> APP -> GLOBAL -> null.
     */
    private final OSGiInjectionScopeData parent;

    /**
     * The lock for compBindings and compEnvBindings. Only initialized for
     * scope data that supports a component namespace.
     */
    private ReentrantReadWriteLock compLock;

    /**
     * Bindings for java:comp/env (NamingConstants.JavaColonNamespace.COMP_ENV).
     * Only used if {@link #isCompAllowed} is true.
     */
    JavaColonNamespaceBindings<InjectionBinding<?>> compEnvBindings;

    /**
     * Bindings for java:comp (NamingConstants.JavaColonNamespace.COMP).
     * Only used if {@link #isCompAllowed} is true.
     */
    private JavaColonNamespaceBindings<InjectionBinding<?>> compBindings;

    /**
     * The set of bindings contributed by this component to java:app or
     * java:global. Only used if {@link #isCompAllowed} is true.
     */
    private Map<NamingConstants.JavaColonNamespace, List<NonCompBinding>> contributedBindings;

    /**
     * The lock for nonCompEnvBindings and savedNonCompEnvBindings.
     * Only used by non-ComponentMetaData scopes.
     */
    private final ReentrantReadWriteLock nonCompEnvLock;

    /**
     * The java:global, java:app, or java:module bindings.
     * Only used by non-ComponentMetaData scopes.
     */
    private JavaColonNamespaceBindings<NonCompBinding> nonCompBindings;

    /**
     * The "saved" java:global, java:app, or java:module bindings.
     * Only used by non-ComponentMetaData scopes.
     */
    private Map<Class<?>, Map<String, InjectionBinding<?>>> savedNonCompEnvBindings;

    /**
     * True if this scope data is enabled for deferred processing. This object
     * will be in its parent's deferred reference data list if and only if this
     * field is true and {@link #deferredReferenceDatas} is non-null.
     */
    private boolean deferredReferenceDataEnabled;

    /**
     * The list of reference contexts that are registered for deferred
     * processing if a non-java:comp request is made. This field is initialized
     * lazily and cleared when no longer needed.
     *
     * @see #deferredReferenceDataEnabled
     */
    private Map<DeferredReferenceData, Boolean> deferredReferenceDatas;

    public OSGiInjectionScopeData(J2EEName j2eeName, NamingConstants.JavaColonNamespace namespace, OSGiInjectionScopeData parent, ReentrantReadWriteLock nonCompEnvLock) {
        super(j2eeName);
        this.namespace = namespace;
        this.parent = parent;
        this.nonCompEnvLock = nonCompEnvLock;
    }

    public void introspect(PrintWriter writer) {
        writer.println(namespace.name() + ivJ2EEName);

        writer.println();
        writer.println("NonCompBinding :");

        nonCompEnvLock.readLock().lock();
        try {
            if (nonCompBindings != null) {
                for (Entry<String, NonCompBinding> entry : nonCompBindings.entrySet()) {
                    writer.printf("%-70s : %s", namespace + "/" + entry.getKey(), entry.getValue().binding.getInjectionClassTypeName()).println();
                }
            }
        } finally {
            nonCompEnvLock.readLock().unlock();
        }

        writer.println();
        writer.println("Deferred reference data:");

        introspectDeferredReferenceData(writer, "");
    }

    @Override
    public void introspectDeferredReferenceData(PrintWriter writer, String indent) {

        writer.println(indent + namespace);
        ArrayList<DeferredReferenceData> deferredArr = null;

        synchronized (this) {
            if (deferredReferenceDatas != null) {
                //deferredReferenceDatas copied over to avoid getting out of sync data and to avoid interruption
                deferredArr = new ArrayList<DeferredReferenceData>(deferredReferenceDatas.keySet());
            }
        }

        if (deferredArr != null) {
            indent += "\t";
            for (DeferredReferenceData entry : deferredArr) {
                entry.introspectDeferredReferenceData(writer, indent);
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        // Prevent new non-java:comp from being contributed by this scope.
        disableDeferredReferenceData();

        // Remove existing non-java:comp bindings contributed by this scope.
        if (contributedBindings != null) {
            for (List<NonCompBinding> nonCompBindings : contributedBindings.values()) {
                for (NonCompBinding nonCompBinding : nonCompBindings) {
                    nonCompBinding.unref();
                }
            }
        }
    }

    @Trivial
    public ReadWriteLock compLock() {
        if (compLock == null) {
            // The first call to this method must be called from a
            // single-threaded context while the component is being initialized,
            // so lazy initialization is safe without further synchronization.
            compLock = new ReentrantReadWriteLock();
        }
        return compLock;
    }

    @Trivial
    public boolean isCompAllowed() {
        return compLock != null;
    }

    /**
     * Add a non-env java:comp binding.
     *
     * @param name a name relative to java:comp
     */
    public void addCompBinding(String name, InjectionBinding<?> binding) {
        if (!compLock.writeLock().isHeldByCurrentThread()) {
            throw new IllegalStateException();
        }

        if (compBindings == null) {
            compBindings = new JavaColonNamespaceBindings<InjectionBinding<?>>(NamingConstants.JavaColonNamespace.COMP, InjectionBindingClassNameProvider.instance);
        }
        compBindings.bind(name, binding);
    }

    /**
     * Add java:comp/env bindings.
     */
    public void addCompEnvBindings(Map<String, InjectionBinding<?>> newBindings) {
        if (!compLock.writeLock().isHeldByCurrentThread()) {
            throw new IllegalStateException();
        }

        if (compEnvBindings == null) {
            compEnvBindings = new JavaColonNamespaceBindings<InjectionBinding<?>>(NamingConstants.JavaColonNamespace.COMP_ENV, InjectionBindingClassNameProvider.instance);
        }

        for (Map.Entry<String, InjectionBinding<?>> entry : newBindings.entrySet()) {
            compEnvBindings.bind(entry.getKey(), entry.getValue());
        }
    }

    public void validateNonCompBindings(Map<Class<?>, Map<String, InjectionBinding<?>>> newBindings) throws InjectionException {
        if (!nonCompEnvLock.writeLock().isHeldByCurrentThread()) {
            throw new IllegalStateException();
        }

        if (savedNonCompEnvBindings != null) {
            for (Map.Entry<Class<?>, Map<String, InjectionBinding<?>>> newBindingsEntry : newBindings.entrySet()) {
                Map<String, InjectionBinding<?>> injectionBindingsMap = savedNonCompEnvBindings.get(newBindingsEntry.getKey());
                if (injectionBindingsMap != null) {
                    for (Map.Entry<String, InjectionBinding<?>> newBindingEntry : newBindingsEntry.getValue().entrySet()) {
                        InjectionBinding<?> savedBinding = injectionBindingsMap.get(newBindingEntry.getKey());
                        if (savedBinding != null) {
                            InjectionBinding<?> newBinding = newBindingEntry.getValue();
                            validateNonCompBinding(savedBinding, newBinding);
                        }
                    }
                }
            }
        }
    }

    @Trivial
    private <A extends Annotation> void validateNonCompBinding(InjectionBinding<A> savedBinding,
                                                               InjectionBinding<?> newBinding) throws InjectionException {
        @SuppressWarnings("unchecked")
        InjectionBinding<A> newBindingUnchecked = (InjectionBinding<A>) newBinding;
        savedBinding.mergeSaved(newBindingUnchecked);
    }

    private boolean isContributionTracked() {
        return namespace == NamingConstants.JavaColonNamespace.APP ||
               namespace == NamingConstants.JavaColonNamespace.GLOBAL;
    }

    private Map<NamingConstants.JavaColonNamespace, List<NonCompBinding>> getContributedBindings() {
        if (contributedBindings == null) {
            contributedBindings = new EnumMap<NamingConstants.JavaColonNamespace, List<NonCompBinding>>(NamingConstants.JavaColonNamespace.class);
        }
        return contributedBindings;
    }

    public void addNonCompBindings(Map<Class<?>, Map<String, InjectionBinding<?>>> newBindings, OSGiInjectionScopeData contributor) {
        if (!newBindings.isEmpty()) {
            List<NonCompBinding> newContributedBindings = null;
            if (isContributionTracked()) {
                newContributedBindings = new ArrayList<NonCompBinding>();
            }

            addNonCompBindings(newBindings, newContributedBindings);

            if (newContributedBindings != null) {
                Map<NamingConstants.JavaColonNamespace, List<NonCompBinding>> contributedBindings = contributor.getContributedBindings();
                List<NonCompBinding> oldContributedBindings = contributedBindings.get(namespace);
                if (oldContributedBindings == null) {
                    contributedBindings.put(namespace, newContributedBindings);
                } else {
                    oldContributedBindings.addAll(newContributedBindings);
                }
            }
        }
    }

    private void addNonCompBindings(Map<Class<?>, Map<String, InjectionBinding<?>>> newBindings, Collection<NonCompBinding> contributedBindings) {
        // Update the namespace bindings.
        if (nonCompBindings == null) {
            nonCompBindings = new JavaColonNamespaceBindings<NonCompBinding>(namespace, NonCompBindingClassNameProvider.instance);
        }
        for (Map<String, InjectionBinding<?>> bindings : newBindings.values()) {
            for (Map.Entry<String, InjectionBinding<?>> entry : bindings.entrySet()) {
                InjectionBinding<?> binding = entry.getValue();
                if (binding.isResolved()) {
                    String qualifiedName = entry.getKey();
                    String name = namespace.unprefix(qualifiedName);
                    NonCompBinding nonCompBinding = nonCompBindings.get(name);
                    if (nonCompBinding == null) {
                        nonCompBinding = new NonCompBinding(this, binding);
                        nonCompBindings.bind(name, nonCompBinding);
                    } else {
                        nonCompBinding.ref();
                    }

                    if (contributedBindings != null) {
                        contributedBindings.add(nonCompBinding);
                    }
                }
            }
        }

        // Merge the new "saved" bindings into the existing "saved" bindings.
        Map<Class<?>, Map<String, InjectionBinding<?>>> savedBindings = savedNonCompEnvBindings;
        if (savedBindings == null) {
            savedNonCompEnvBindings = newBindings;
        } else {
            for (Map.Entry<Class<?>, Map<String, InjectionBinding<?>>> newBindingsEntry : newBindings.entrySet()) {
                Class<?> processorClass = newBindingsEntry.getKey();
                Map<String, InjectionBinding<?>> injectionBindingsMap = savedBindings.get(processorClass);
                if (injectionBindingsMap == null) {
                    newBindings.put(processorClass, newBindingsEntry.getValue());
                } else {
                    for (Map.Entry<String, InjectionBinding<?>> entry : newBindingsEntry.getValue().entrySet()) {
                        String qualifiedName = entry.getKey();
                        if (!injectionBindingsMap.containsKey(qualifiedName)) {
                            injectionBindingsMap.put(qualifiedName, entry.getValue());
                        }
                    }
                }
            }
        }
    }

    void removeNonCompBinding(InjectionBinding<?> binding) {
        String qualifiedName = binding.getJndiName();

        String name = namespace.unprefix(qualifiedName);
        nonCompBindings.unbind(name);

        for (Iterator<Map<String, InjectionBinding<?>>> bindingsIter = savedNonCompEnvBindings.values().iterator(); bindingsIter.hasNext();) {
            Map<String, InjectionBinding<?>> bindings = bindingsIter.next();
            if (bindings.remove(qualifiedName) != null && bindings.isEmpty()) {
                bindingsIter.remove();
            }
        }
    }

    /**
     * Gets the injection binding for a JNDI name. The caller is responsible for
     * calling {@link #processDeferredReferenceData}.
     */
    public InjectionBinding<?> getInjectionBinding(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        if (namespace == NamingConstants.JavaColonNamespace.COMP) {
            return lookup(compLock, compBindings, name);
        }
        if (namespace == NamingConstants.JavaColonNamespace.COMP_ENV) {
            return lookup(compLock, compEnvBindings, name);
        }
        if (namespace == this.namespace) {
            NonCompBinding nonCompBinding = lookup(nonCompEnvLock, nonCompBindings, name);
            return nonCompBinding == null ? null : nonCompBinding.binding;
        }
        return null;
    }

    private <T> T lookup(ReadWriteLock lock, JavaColonNamespaceBindings<T> bindings, String name) throws NamingException {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            return bindings == null ? null : bindings.lookup(name);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns true if a JNDI subcontext exists.
     */
    public boolean hasObjectWithPrefix(NamingConstants.JavaColonNamespace namespace, String name) throws NamingException {
        if (namespace == NamingConstants.JavaColonNamespace.COMP) {
            return hasObjectWithPrefix(compLock, compBindings, name);
        }
        if (namespace == NamingConstants.JavaColonNamespace.COMP_ENV) {
            return hasObjectWithPrefix(compLock, compEnvBindings, name);
        }
        if (namespace == this.namespace) {
            return hasObjectWithPrefix(nonCompEnvLock, nonCompBindings, name);
        }
        return false;
    }

    private boolean hasObjectWithPrefix(ReadWriteLock lock, JavaColonNamespaceBindings<?> bindings, String name) throws NamingException {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            return bindings != null && bindings.hasObjectWithPrefix(name);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Lists the contents of a JNDI subcontext.
     */
    public Collection<? extends NameClassPair> listInstances(NamingConstants.JavaColonNamespace namespace, String contextName) throws NamingException {
        if (namespace == NamingConstants.JavaColonNamespace.COMP) {
            return listInstances(compLock, compBindings, contextName);
        }
        if (namespace == NamingConstants.JavaColonNamespace.COMP_ENV) {
            return listInstances(compLock, compEnvBindings, contextName);
        }
        if (namespace == this.namespace) {
            return listInstances(nonCompEnvLock, nonCompBindings, contextName);
        }
        return Collections.emptyList();
    }

    private Collection<? extends NameClassPair> listInstances(ReadWriteLock lock, JavaColonNamespaceBindings<?> bindings, String contextName) throws NamingException {
        Lock readLock = lock.readLock();
        readLock.lock();
        try {
            return bindings == null ? Collections.<NameClassPair> emptyList() : bindings.listInstances(contextName);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Register this scope data with its parent if necessary for deferred
     * reference data processing.
     */
    public synchronized void enableDeferredReferenceData() {
        deferredReferenceDataEnabled = true;
        if (parent != null && deferredReferenceDatas != null) {
            parent.addDeferredReferenceData(this);
        }
    }

    /**
     * Unregister this scope data with its parent if necessary for deferred
     * reference data processing.
     */
    private synchronized void disableDeferredReferenceData() {
        deferredReferenceDataEnabled = false;
        if (parent != null && deferredReferenceDatas != null) {
            parent.removeDeferredReferenceData(this);
            deferredReferenceDatas = null;
        }
    }

    /**
     * Add a child deferred reference data to this scope.
     */
    public synchronized void addDeferredReferenceData(DeferredReferenceData refData) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "addDeferredReferenceData", "this=" + this, refData);
        }

        if (deferredReferenceDatas == null) {
            deferredReferenceDatas = new LinkedHashMap<DeferredReferenceData, Boolean>();
            if (parent != null && deferredReferenceDataEnabled) {
                parent.addDeferredReferenceData(this);
            }
        }
        deferredReferenceDatas.put(refData, null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "addDeferredReferenceData");
        }
    }

    /**
     * Remove a child deferred reference data to this scope.
     */
    public synchronized void removeDeferredReferenceData(DeferredReferenceData refData) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "removeDeferredReferenceData", "this=" + this, refData);
        }

        if (deferredReferenceDatas != null) {
            deferredReferenceDatas.remove(refData);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "removeDeferredReferenceData");
        }
    }

    /**
     * Process all child reference datas.
     *
     * @return true if any reference data was processed.
     */
    @Override
    public boolean processDeferredReferenceData() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "processDeferredReferenceData", "this=" + this);
        }

        Map<DeferredReferenceData, Boolean> deferredReferenceDatas;
        synchronized (this) {
            deferredReferenceDatas = this.deferredReferenceDatas;
            this.deferredReferenceDatas = null;

            if (parent != null) {
                parent.removeDeferredReferenceData(this);
            }
        }

        boolean any = false;

        if (deferredReferenceDatas != null) {
            for (DeferredReferenceData refData : deferredReferenceDatas.keySet()) {
                try {
                    any |= refData.processDeferredReferenceData();
                } catch (InjectionException ex) {
                    // We're processing all references in an attempt to locate
                    // non-java:comp references, so we don't care about failures
                    // (erroneous or conflicting metadata).  Any exception that
                    // is thrown will be rethrown by ReferenceContext.process
                    // when the component is actually used.
                    ex.getClass(); // findbugs
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "processDeferredReferenceData", any);
        }
        return any;
    }
}