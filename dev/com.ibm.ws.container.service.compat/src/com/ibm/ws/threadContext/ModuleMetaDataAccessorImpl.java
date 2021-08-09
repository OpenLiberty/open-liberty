/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threadContext;

import java.net.URL;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * 
 * <p>Accessor for ModuleMetaData while application deployment. It provides static methods for easy access.
 * It is a singleton.
 * 
 * @ibm-private-in-use
 */
public final class ModuleMetaDataAccessorImpl {
    private static final TraceComponent tc = Tr.register(ModuleMetaDataAccessorImpl.class, "Runtime", "com.ibm.ws.runtime.runtime");

    private static ModuleMetaDataAccessorImpl mmdai = new ModuleMetaDataAccessorImpl();

    private ThreadContext<Map<URL, ModuleMetaData>> threadContext = null;

    private ModuleMetaDataAccessorImpl() {
        threadContext = new ThreadContextImpl<Map<URL, ModuleMetaData>>();
    }

    /**
     * 
     * @return ModuleMetaDataAccessorImpl
     */
    public static ModuleMetaDataAccessorImpl getModuleMetaDataAccessor() {
        return mmdai;
    }

    /**
     * @return Map<URL, ModuleMetaData>
     */
    public Map<URL, ModuleMetaData> getModuleMetaDataMap() {
        return threadContext.getContext();
    }

    /**
     * @return ThreadContext
     */
    @Deprecated
    public ThreadContext<Map<URL, ModuleMetaData>> getThreadContext() {
        return threadContext;
    }

    /**
     * Begin the context for the map of ModuleMetaData provided.
     * 
     * @param Map<URL, ModuleMetaData> It Must not be null. Tr.error will be logged if it is null.
     * @return Previous Object, which was on the stack. It can be null.
     */
    public Object beginContext(Map<URL, ModuleMetaData> mmdi) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (mmdi != null && !mmdi.isEmpty())
                Tr.debug(tc, "begin context " + mmdi.values().iterator().next().getJ2EEName());
            else
                Tr.debug(tc, "null or empty object was passed.");
        }

        return threadContext.beginContext(mmdi);
    }

    /**
     * Establish default context for when there should be no Map of URL and ModuleMetaData on the thread.
     * 
     * @return Previous Map of ModuleMetaData which was on the thread. It can be null.
     */
    public Map<URL, ModuleMetaData> beginDefaultContext() {
        return threadContext.beginContext(null);
    }

    /**
     * End the context for the current Map of URL (location where the module is loaded from), ModuleMetaData
     * 
     * @return Object which was removed (pop) from the stack.
     */
    public Object endContext() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Map<URL, ModuleMetaData> mmdi = getModuleMetaDataMap();
            ModuleMetaData mmd = ((mmdi != null) && !mmdi.isEmpty()) ? mmdi.values().iterator().next() : null;
            Tr.debug(tc, "end context " + (mmd == null ? null : mmd.getJ2EEName()));
        }
        return threadContext.endContext();
    }

    /**
     * @return The index of Map<URL, ModuleMetaData>
     */
    public int getModuleMetaDataListIndex() {
        return threadContext.getContextIndex();
    }
}
