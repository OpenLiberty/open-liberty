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

import java.util.List;

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

    private ThreadContext<List<ModuleMetaData>> threadContext = null;

    private ModuleMetaDataAccessorImpl() {
        threadContext = new ThreadContextImpl<List<ModuleMetaData>>();
    }

    /**
     * 
     * @return ModuleMetaDataAccessorImpl
     */
    public static ModuleMetaDataAccessorImpl getModuleMetaDataAccessor() {
        return mmdai;
    }

    /**
     * @return List<ModuleMetaData>
     */
    public List<ModuleMetaData> getModuleMetaDataList() {
        return threadContext.getContext();
    }

    /**
     * @return ThreadContext
     */
    @Deprecated
    public ThreadContext<List<ModuleMetaData>> getThreadContext() {
        return threadContext;
    }

    /**
     * Begin the context for the List of ModuleMetaData provided.
     * 
     * @param List<ModuleMetaData> It Must not be null. Tr.error will be logged if it is null.
     * @return Previous Object, which was on the stack. It can be null.
     */
    public Object beginContext(List<ModuleMetaData> mmdi) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (mmdi != null && !mmdi.isEmpty())
                Tr.debug(tc, "begin context " + mmdi.get(0).getJ2EEName());
            else
                Tr.debug(tc, "null or empty object was passed.");
        }

        return threadContext.beginContext(mmdi);
    }

    /**
     * Establish default context for when there should be no List of ModuleMetaData on the thread.
     * 
     * @return Previous List of ModuleMetaData which was on the thread. It can be null.
     */
    public List<ModuleMetaData> beginDefaultContext() {
        return threadContext.beginContext(null);
    }

    /**
     * End the context for the current List of ModuleMetaData
     * 
     * @return Object which was removed (pop) from the stack.
     */
    public Object endContext() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            List<ModuleMetaData> mmdi = getModuleMetaDataList();
            ModuleMetaData mmd = ((mmdi != null) && !mmdi.isEmpty()) ? mmdi.get(0) : null;
            Tr.debug(tc, "end context " + (mmd == null ? null : mmd.getJ2EEName()));
        }
        return threadContext.endContext();
    }

    /**
     * @return The index of List<ModuleMetaData>
     */
    public int getModuleMetaDataListIndex() {
        return threadContext.getContextIndex();
    }
}
