/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.concurrent.spi.ThreadContextProvider;
import org.eclipse.microprofile.concurrent.spi.ThreadContextSnapshot;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.concurrent.mp.ContextOp;

/**
 * This interface allows container-provided thread context types to be used
 * alongside MicroProfile ThreadContextProviders.
 */
@Trivial
public abstract class ContainerContextProvider implements ThreadContextProvider {
    static final Map<String, String> EMPTY_MAP = Collections.emptyMap();

    /**
     * Appends thread context snapshot(s) of the type provided by this provider to the specified list.
     *
     * @param op the CLEARED or PROPAGATED operation.
     * @param contextSnapshots list to which to add context snapshot(s).
     */
    public abstract void addContextSnapshot(ContextOp op, ArrayList<com.ibm.wsspi.threadcontext.ThreadContext> contextSnapshots);

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        // never used, because this class is converted to the internal container provider type
        throw new UnsupportedOperationException();
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        // never used, because this class is converted to the internal container provider type
        throw new UnsupportedOperationException();
    }
}
