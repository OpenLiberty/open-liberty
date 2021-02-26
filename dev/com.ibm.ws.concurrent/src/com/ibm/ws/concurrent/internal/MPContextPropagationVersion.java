/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.internal;

import org.eclipse.microprofile.context.ManagedExecutor;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Tracks the enabled version of MicroProfile Context Propagation and provides for
 * comparisons with a target level.
 */
@Trivial
enum MPContextPropagationVersion {
    V1_0,
    V1_1;

    private static final MPContextPropagationVersion VERSION = mpContextPropagationVersion();

    static boolean atLeast(MPContextPropagationVersion target) {
        return VERSION.ordinal() >= target.ordinal();
    }

    static MPContextPropagationVersion get() {
        return VERSION;
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
            return V1_1;
        } catch (NoSuchMethodException x) {
            return V1_0;
        }
    }
}
