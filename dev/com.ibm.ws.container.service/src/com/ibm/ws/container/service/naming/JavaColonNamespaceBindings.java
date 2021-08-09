/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.naming;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.NotContextException;

/**
 * Manages the bindings for a {@link #JavaColonNamingHelper}. This object is
 * not thread-safe: accesses to the {@link #put} and {@link #remove} methods
 * must be protected by a write lock, and access to all other methods must be
 * protected by a read lock.
 * 
 * @param <T> the binding object
 */
@SuppressWarnings("serial")
public class JavaColonNamespaceBindings<T> extends HashMap<String, T> {
    // The HashMap superclass holds name-to-binding: { "jdbc/db2/ds" : object }.

    /**
     * Provides the type names for the bindings.
     * 
     * @param <T> the binding object
     */
    public interface ClassNameProvider<T> {
        /**
         * Return the type name of a binding.
         * 
         * @param binding the binding
         * @return the type name for {@link NameClassPair#getClassName}
         */
        String getBindingClassName(T binding);
    }

    /**
     * The namespace for these bindings.
     */
    private final NamingConstants.JavaColonNamespace namespace;

    /**
     * The class name provider for the bindings in this object.
     */
    private final ClassNameProvider<T> classNameProvider;

    /**
     * Map of context names to name/class pairs. This data is initialized
     * lazily. Example:
     * <code>
     * {
     * "" : { "jdbc" : NCP("jdbc", "java.lang.Context") },
     * "jdbc" : { "db2" : NCP("db2", "java.lang.Context") },
     * "jdbc/db2": { "ds" : NCP("ds", type) },
     * }
     * </code>
     */
    private volatile Map<String, Map<String, NameClassPair>> contextBindings;

    /**
     * The keys of {@link #contextBindings}. This data is initialized lazily.
     * This data is stored separately to avoid building that Map unless it's
     * needed since {@link #hasObjectWithPrefix} is much more common
     * than {@link #listInstances}.
     */
    private volatile Set<String> contexts;

    /**
     * Return the context
     * 
     * @param name
     * @return
     */
    static String getContextName(String name) {
        int index = name.lastIndexOf('/');
        return index == -1 ? "" : name.substring(0, index);
    }

    public JavaColonNamespaceBindings(NamingConstants.JavaColonNamespace namespace, ClassNameProvider<T> nameProvider) {
        this.namespace = namespace;
        this.classNameProvider = nameProvider;
    }

    /**
     * Adds a binding.
     * 
     * @param name the namespace name (e.g., "jdbc/ds")
     * @param binding the binding object
     */
    public void bind(String name, T binding) {
        put(name, binding);

        Map<String, Map<String, NameClassPair>> bindingsByContext = this.contextBindings;
        if (bindingsByContext != null) {
            addContextBinding(bindingsByContext, name, binding);
        }

        Set<String> contexts = this.contexts;
        if (contexts != null) {
            contexts.add(getContextName(name));
        }
    }

    /**
     * Removes a binding.
     * 
     * @param name the namespace name (e.g., "jdbc/ds")
     */
    public void unbind(String name) {
        super.remove(name);

        // Remove caches.  We could try to do online updates, but for now, it's
        // less error-prone to just let them be recomputed when needed.
        contextBindings = null;
        contexts = null;
    }

    /**
     * Looks up a binding.
     * 
     * @param name the name passed to {@link #bind}
     * @return the binding, or null if unavailable
     * @throws NotContextException if a lookup attempts to use a binding as a
     *             context name
     */
    public T lookup(String name) throws NamingException {
        T binding = super.get(name);
        if (binding != null) {
            return binding;
        }

        // Binding does not exist.  Check if any the contexts are bindings.
        String contextName = name;
        while (!(contextName = getContextName(contextName)).isEmpty()) {
            if (containsKey(contextName)) {
                throw new NotContextException(namespace + "/" + contextName);
            }
        }

        return null;
    }

    /**
     * @see JavaColonNamingHelper#hasObjectWithPrefix
     */
    public boolean hasObjectWithPrefix(String contextName) throws NamingException {
        return getContexts().contains(contextName);
    }

    private Set<String> getContexts() {
        Set<String> contexts = this.contexts;
        if (contexts == null) {
            contexts = createContexts();
            this.contexts = contexts;
        }
        return contexts;
    }

    private Set<String> createContexts() {
        Set<String> contexts = new HashSet<String>(4, 0.9f);
        for (String name : keySet()) {
            String contextName = name;
            do {
                contextName = getContextName(contextName);
            } while (contexts.add(contextName));
        }

        return contexts;
    }

    /**
     * @throws NotContextException if a lookup attempts to use a binding as a
     *             context name
     * @see JavaColonNamingHelper#listInstances
     */
    public Collection<? extends NameClassPair> listInstances(String contextName) throws NamingException {
        // First, check for a known context.
        Map<String, NameClassPair> bindings = getBindingsByContext().get(contextName);
        if (bindings != null) {
            return bindings.values();
        }

        // Next, check to see if any of the parent contexts is actually
        // a binding.
        do {
            if (containsKey(contextName)) {
                throw new NotContextException(namespace + "/" + contextName);
            }

            contextName = getContextName(contextName);
        } while (!contextName.isEmpty());

        return Collections.<NameClassPair> emptyList();
    }

    private Map<String, Map<String, NameClassPair>> getBindingsByContext() {
        Map<String, Map<String, NameClassPair>> bindingsByContext = this.contextBindings;
        if (bindingsByContext == null) {
            bindingsByContext = createBindingsByContext();
            this.contextBindings = bindingsByContext;
        }
        return bindingsByContext;
    }

    private Map<String, Map<String, NameClassPair>> createBindingsByContext() {
        Map<String, Map<String, NameClassPair>> bindingsByContext = new HashMap<String, Map<String, NameClassPair>>(4, 0.9f);

        for (Map.Entry<String, T> entry : entrySet()) {
            String name = entry.getKey();
            T binding = entry.getValue();
            addContextBinding(bindingsByContext, name, binding);
        }

        return bindingsByContext;
    }

    private void addContextBinding(Map<String, Map<String, NameClassPair>> bindingsByContext, String nameInNamespace, T binding) {
        String className = classNameProvider.getBindingClassName(binding);
        boolean context = false;

        // Loop foreach context.  Starting with "a/b/c", first add "c" to "a/b",
        // then add "b" to "a", then add "a" to "". 
        for (;;) {
            String contextName;
            String name;

            int index = nameInNamespace.lastIndexOf('/');
            if (index == -1) {
                contextName = "";
                name = nameInNamespace;
            } else {
                contextName = nameInNamespace.substring(0, index);
                name = nameInNamespace.substring(index + 1);
            }

            Map<String, NameClassPair> bindings = bindingsByContext.get(contextName);
            if (bindings == null) {
                bindings = new HashMap<String, NameClassPair>(4, 0.9f);
                bindingsByContext.put(contextName, bindings);
            } else if (context && bindings.containsKey(name)) {
                return;
            }

            bindings.put(name, new NameClassPair(name, className));

            if (contextName.isEmpty()) {
                return;
            }

            nameInNamespace = contextName;
            className = Context.class.getName();
            context = true;
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        // HashMap is serializable by default, but we don't want to support.
        throw new NotSerializableException();
    }
}
