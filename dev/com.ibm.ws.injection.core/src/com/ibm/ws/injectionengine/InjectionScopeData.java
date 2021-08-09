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
package com.ibm.ws.injectionengine;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.Reference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InternalInjectionEngineAccessor;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 * Injection data for the global, application, module, and component scopes.
 * In server runtimes, this data is stored in a metadata slot.
 */
public class InjectionScopeData
{
    private static final String CLASS_NAME = InjectionScopeData.class.getName();
    private static final TraceComponent tc = Tr.register(InjectionScopeData.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    /**
     * The J2EEName of the scope, or <tt>null</tt> for the global scope.
     */
    protected final J2EEName ivJ2EEName;

    /**
     * The logical name for the application scope. This field will always be
     * null for other scopes.
     */
    String ivLogicalAppName; // F743-38521

    /**
     * True if this scope has been destroyed.
     */
    private boolean ivDestroyed; // F50309.7

    /**
     * A map of resolved env-entry. For non-java:comp env-entry, this indicates
     * that a value or binding was found for an env-entry in some component.
     */
    private Map<String, InjectionBinding<?>> ivInjectableEnvEntries; // F91489

    /**
     * The list of resource definitions created for this metadata scope. This
     * field will be null until a resource definition reference is added.
     */
    private Map<String, Reference> ivDefinitionReferences;

    /**
     * Common reference context for the module scope. This field will always be
     * null for other scopes.
     */
    public ReferenceContext ivReferenceContext;

    public InjectionScopeData(J2EEName j2eeName)
    {
        ivJ2EEName = j2eeName;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>: " + this);
    }

    @Override
    public String toString()
    {
        return super.toString() +
               '[' + (ivJ2EEName != null ? ivJ2EEName : "(global)") + ']';
    }

    public synchronized void destroy() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.debug(tc, "destroy: " + this);

        if (ivDefinitionReferences != null) {
            for (Map.Entry<String, Reference> entry : ivDefinitionReferences.entrySet()) {
                destroyDefinitionReference(entry.getKey(), entry.getValue());
            }
        }

        ivDestroyed = true; // F50309.7

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "destroy");
    }

    private void destroyDefinitionReference(String jndiName, Reference ref) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "destroying definition reference " + jndiName);

        try {
            InternalInjectionEngineAccessor.getInstance().destroyDefinitionReference(ref); // F58056
        } catch (Exception ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".destroyDefinitionReference",
                                        "253", this, new Object[] { jndiName, ref });
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "failed to destroy definition reference", ex);
        }
    }

    public synchronized InjectionBinding<?> getInjectableEnvEntry(String jndiName) // F91489
    {
        InjectionBinding<?> binding = ivInjectableEnvEntries == null ? null :
                        ivInjectableEnvEntries.get(jndiName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getInjectableEnvEntry: " + jndiName + " = " + binding);
        return binding;
    }

    public synchronized void addInjectableEnvEntry(InjectionBinding<?> binding) // F91489
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "addInjectableEnvEntry: " + this + ", name=" + binding.getJndiName());

        if (ivInjectableEnvEntries == null)
        {
            ivInjectableEnvEntries = new HashMap<String, InjectionBinding<?>>();
        }
        ivInjectableEnvEntries.put(binding.getJndiName(), binding);
    }

    public synchronized void removeInjectableEnvEntry(String jndiName) // d702893
    {
        if (ivInjectableEnvEntries == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "removeInjectableEnvEntry: (empty)");
            // Defensive against bad AppResourceElementCache notifications.
        }
        else
        {
            ivInjectableEnvEntries.remove(jndiName);
        }
    }

    public synchronized void addDefinitionReference(String jndiName, Reference ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "addDefinitionReference: " + this + ", name=" + jndiName);

        if (ivDestroyed) { // F50309.7
            destroyDefinitionReference(jndiName, ref);
        } else {
            if (ivDefinitionReferences == null) {
                ivDefinitionReferences = new LinkedHashMap<String, Reference>();
            }
            ivDefinitionReferences.put(jndiName, ref);
        }
    }

    public synchronized void removeDefinitionReference(String jndiName) { // F743-38521
        if (ivDefinitionReferences == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "removeDefinitionReference: (empty)");
        } else {
            Reference ref = ivDefinitionReferences.get(jndiName);
            if (ref == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "removeDefinitionReference: " + jndiName + " (unknown)");
            } else {
                destroyDefinitionReference(jndiName, ref);
            }
        }
    }
}
