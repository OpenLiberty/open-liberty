/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;

import com.ibm.ejs.util.dopriv.SystemGetPropertyPrivileged;
import com.ibm.ws.util.CacheHashMap;
import com.ibm.wsspi.injectionengine.factory.OverrideReferenceFactory;

/**
 * Internal context between base injection processor code and the engine. This
 * class is package private to prevent it from being used by clients.
 */
class InjectionProcessorContext
{
    public Map<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>> ivObjectFactoryMap;
    public Map<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>> ivNoOverrideObjectFactoryMap;
    public HashMap<Class<? extends Annotation>, OverrideReferenceFactory<?>[]> ivOverrideReferenceFactoryMap;

    /**
     * A map of JNDI name to injection bindings that were created during previous
     * metadata processing. This is used while dynamically adding injection
     * targets to completed metadata to ensure that the metadata specified by the
     * injection annotations is consistent with any metadata specified by
     * previous processing.
     */
    public Map<String, InjectionBinding<?>> ivCompletedInjectionBindings; // d730349

    /**
     * True if java:global/:app/:module injection bindings should be added to {@link #ivSavedGlobalInjectionBindings}, {@link #ivSavedAppInjectionBindings}, or
     * {@link #ivSavedModuleInjectionBindings}. If a binding is already present
     * in the map, then it is merged via {@link InjectionBinding#mergedSaved}.
     *
     * <p>This field should be true in the embeddable EJB container and Liberty
     * since the injection engine must ensure that java:global/:app/:module
     * metadata is consistent across components. In traditional WAS server environments,
     * app install ensures this consistency, so this field should be false during
     * component metadata processing in the server.
     */
    public boolean ivSaveNonCompInjectionBindings; // F743-33811.2

    // Map<processorClass, Map<qualifiedName, binding>>
    public Map<Class<?>, Map<String, InjectionBinding<?>>> ivSavedGlobalInjectionBindings; // F743-33811.2
    public Map<Class<?>, Map<String, InjectionBinding<?>>> ivSavedAppInjectionBindings; // F743-33811.2
    public Map<Class<?>, Map<String, InjectionBinding<?>>> ivSavedModuleInjectionBindings; // F743-33811.2

    /**
     * True if java:global/:app/:module injection bindings should be bound. In
     * server and embeddable environments, this field should be false during
     * metadata processing for components since the injection engine does the
     * processing for all java:global/:app/:module at server/app/module start.
     */
    public boolean ivBindNonCompInjectionBindings; // F743-31682

    /**
     * Cache of class to list of "set" methods across classes.
     */
    private Map<Class<?>, List<Method>> ivDeclaredSetMethodCache;

    /**
     * The list of injection bindings processed by this context.
     */
    public final List<InjectionBinding<?>> ivProcessedInjectionBindings = new ArrayList<InjectionBinding<?>>();

    /**
     * The java: naming context. This field must be set by the injection engine
     * prior to performing name space bindings.
     */
    public Context ivJavaNameSpaceContext; // d682474

    /**
     * The java:comp/env naming context. If this field is not initialized prior
     * to performing name space bindings, it is lazily initialized from {@link #ivJavaNameSpaceContext}.
     */
    private Context ivJavaCompEnvNameSpaceContext; // d682474

    /**
     * Naming contexts for java:global, java:app, java:module, and java:comp.
     * If an entry is not initialized via {@link setJavaContext} prior to
     * namespace binding, it is lazily initialized from {@link ivJavaContext}.
     */
    private final Context[] ivJavaNameSpaceContexts = new Context[InjectionScope.size()]; // d682474

    /**
     * Increase visibility to allow subclassing.
     */
    protected InjectionProcessorContext()
    {
        // for use in subclasses
    }

    Map<Class<?>, List<Method>> getDeclaredSetMethodCache()
    {
        if (ivDeclaredSetMethodCache == null)
        {
            ivDeclaredSetMethodCache = new CacheHashMap<Class<?>, List<Method>>(50);
        }
        return ivDeclaredSetMethodCache;
    }

    /**
     * @param scope the scope, or null for java:comp/env
     */
    public Context getJavaNameSpaceContext(InjectionScope scope) // d682474
    throws NamingException
    {
        if (scope == null) // F48603
        {
            return getJavaCompEnvNameSpaceContext();
        }

        int index = scope.ordinal();

        Context result = ivJavaNameSpaceContexts[index];
        if (result == null)
        {
            result = (Context) ivJavaNameSpaceContext.lookup(scope.contextName());
            ivJavaNameSpaceContexts[index] = result;
        }

        return result;
    }

    public Context getJavaCompEnvNameSpaceContext() // d682474
    throws NamingException
    {
        if (ivJavaCompEnvNameSpaceContext == null)
        {
            ivJavaCompEnvNameSpaceContext = (Context) getJavaNameSpaceContext(InjectionScope.COMP).lookup("env"); // F48603
        }
        return ivJavaCompEnvNameSpaceContext;
    }

    public void metadataProcessingComplete() // F87539
    {
        // Now that metadata processing has completed, allow the resulting
        // bindings a chance to clean-up resources that are no longer needed,
        // such as references to the ComponentNameSpaceConfiguration.      d648283
        for (InjectionBinding<?> binding : ivProcessedInjectionBindings)
        {
            binding.metadataProcessingComplete();
        }
    }

    @Override
    public String toString()
    {
        String nl = AccessController.doPrivileged(new SystemGetPropertyPrivileged("line.separator", "\n")) +
                    "                                 ";

        StringBuilder sb = new StringBuilder();
        sb.append(nl).append("*** InjectionProcessorContext ***");
        sb.append(nl).append("Active bindings    = ").append(ivCompletedInjectionBindings != null);
        sb.append(nl).append("Save non-java:comp = ").append(ivSaveNonCompInjectionBindings);
        sb.append(nl).append("  java:global      = ").append(ivSavedGlobalInjectionBindings != null);
        sb.append(nl).append("  java:app         = ").append(ivSavedAppInjectionBindings != null);
        sb.append(nl).append("  java:module      = ").append(ivSavedModuleInjectionBindings != null);
        sb.append(nl).append("Bind non-java:comp = ").append(ivBindNonCompInjectionBindings);

        return sb.toString();
    }
}
