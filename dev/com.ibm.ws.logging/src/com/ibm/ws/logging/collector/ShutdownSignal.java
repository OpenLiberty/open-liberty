/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.logging.collector;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Static flag to signal collector shutdown across all tasks
 */
public class ShutdownSignal {
    private static final TraceComponent tc = Tr.register(ShutdownSignal.class);
    private static volatile boolean shutdownRequested = false;

    public static void requestShutdown() {
        shutdownRequested = true;
    }

    public static boolean isShutdownRequested() {
        boolean requested = shutdownRequested;
        return requested;
    }
}
