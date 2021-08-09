/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.context;

import java.util.Map;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Trivial
public class CDIContextProviderHolder implements ThreadContextProvider {

    public final AtomicServiceReference<ThreadContextProvider> cdiContextProviderRef = new AtomicServiceReference<>("CDIContextProvider");

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        ThreadContextProvider provider = cdiContextProviderRef.getService();
        if (provider != null)
            return provider.currentContext(props);
        else
            // No CDI feature enabled at the point of capturing context, but may be available when applying context.
            return new DeferredClearedContextSnapshot(cdiContextProviderRef, props);
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        ThreadContextProvider provider = cdiContextProviderRef.getService();
        if (provider != null)
            return provider.clearedContext(props);
        else
            return new DeferredClearedContextSnapshot(cdiContextProviderRef, props);
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.CDI;
    }
}
