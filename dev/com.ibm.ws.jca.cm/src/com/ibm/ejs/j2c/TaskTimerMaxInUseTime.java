/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ejs.j2c;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This is a utility class used by the PoolManager.
 */
public final class TaskTimerMaxInUseTime extends TaskTimer {
    private static final TraceComponent tc = Tr.register(TaskTimerMaxInUseTime.class, J2CConstants.traceSpec, J2CConstants.messageFile);

    /**
     * Create a new TaskTimerMaxInUseTime.
     */
    protected TaskTimerMaxInUseTime(PoolManager value) {
        super(value);
    }

    /**
     * This method is the implementation of Thread.run().
     */

    @Override
    public void run() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "run");
        }
        pm.executeMaxInUseTimeTask();
        pm.maxInUseTimeThreadStarted = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "run");
        }

    }
}