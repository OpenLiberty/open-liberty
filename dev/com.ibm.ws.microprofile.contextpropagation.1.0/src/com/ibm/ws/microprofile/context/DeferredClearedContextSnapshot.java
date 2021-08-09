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

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * In Liberty, the container-provided context types are considered always available
 * even if the feature supplying the container internal org.eclipse.microprofile.context.spi.ThreadContextSnapshot
 * type isn't available. This class allows for the possibility that the feature could become
 * available at a later time, by deferring the creation of the cleared thread context snapshot
 * until the action or task is about to start.
 */
@Trivial
public class DeferredClearedContextSnapshot implements ThreadContextSnapshot {

    private final AtomicServiceReference<ThreadContextProvider> contextProviderRef;
    private final Map<String, String> props;

    DeferredClearedContextSnapshot(AtomicServiceReference<ThreadContextProvider> contextProviderRef,
                                   Map<String, String> props) {
        this.contextProviderRef = contextProviderRef;
        this.props = props;
    }

    @Override
    public ThreadContextController begin() {
        ThreadContextProvider provider = contextProviderRef.getService();
        if (provider != null) {
            return provider.clearedContext(props).begin();
        } else {
            return () -> {
                // no-op ThreadContextController
            };
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('@')
                        .append(Integer.toHexString(hashCode())) //
                        .append(" for ")
                        .append(contextProviderRef);
        return sb.toString();
    }
}
