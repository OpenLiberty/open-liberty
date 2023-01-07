/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import org.eclipse.microprofile.context.ManagedExecutor;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Tracks the enabled version of MicroProfile Context Propagation and provides for
 * comparisons with a target level.
 */
@Trivial
enum MPContextPropagationVersion {
    V1_0,
    V1_1, // release was ultimately skipped due to changes in MicroProfile process
    V1_1_or_1_2_or_1_3; // spec API is identical across these

    private static final TraceComponent tc = Tr.register(MPContextPropagationVersion.class);

    private static final MPContextPropagationVersion VERSION = mpContextPropagationVersion();

    static boolean atLeast(MPContextPropagationVersion target) {
        boolean atLeast = VERSION.ordinal() >= target.ordinal();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, VERSION + " atLeast " + target + "? " + atLeast);
        return atLeast;
    }

    /**
     * Returns the version of MicroProfile Context Propagation.
     *
     * @return the version of MicroProfile Context Propagation.
     */
    @FFDCIgnore(NoSuchMethodException.class)
    private static MPContextPropagationVersion mpContextPropagationVersion() {
        try {
            ManagedExecutor.class.getMethod("getThreadContext");
            return V1_1_or_1_2_or_1_3;
        } catch (NoSuchMethodException x) {
            return V1_0;
        }
    }
}
