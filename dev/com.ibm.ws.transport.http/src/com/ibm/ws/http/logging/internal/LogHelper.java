/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.logging.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class LogHelper {

    public void debug(TraceComponent tc, String message) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, message);
        }
    }

    public void entry(TraceComponent tc, String message) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, message);
        }
    }

    public void exit(TraceComponent tc, String methodName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, methodName);
    }
}
