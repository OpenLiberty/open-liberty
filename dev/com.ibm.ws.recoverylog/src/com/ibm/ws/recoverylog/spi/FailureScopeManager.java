/*******************************************************************************
 * Copyright (c) 2004,2021 IBM Corporation and others.
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

package com.ibm.ws.recoverylog.spi;

import java.util.HashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class FailureScopeManager {
    private static final TraceComponent tc = Tr.register(FailureScopeManager.class, TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    private static HashMap<Byte, FailureScopeFactory> _failureScopeFactoriesById = new HashMap<Byte, FailureScopeFactory>();
    private static HashMap<Class<?>, FailureScopeFactory> _failureScopeFactoriesByClass = new HashMap<Class<?>, FailureScopeFactory>();

    public static void registerFailureScopeFactory(Byte id, Class<?> failureScopeClass, FailureScopeFactory factory) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerFailureScopeFactory", new Object[] { id, failureScopeClass, factory });

        _failureScopeFactoriesById.put(id, factory);
        _failureScopeFactoriesByClass.put(failureScopeClass, factory);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerFailureScopeFactory");
    }

    public static byte[] toByteArray(FailureScope failureScope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "toByteArray", failureScope);

        byte[] bytes = null;

        final Class<? extends FailureScope> failureScopeClass = failureScope.getClass();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "FailureScope class: " + failureScopeClass);

        final FailureScopeFactory failureScopeFactory = _failureScopeFactoriesByClass.get(failureScopeClass);

        if (failureScopeFactory != null) {
            bytes = failureScopeFactory.toByteArray(failureScope);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "toByteArray");
        return bytes;
    }

    public static FailureScope toFailureScope(byte[] bytes) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "toFailureScope", bytes);

        // The first byte in the given array is the failure scope factory's
        // identifier. Extract it from the array and use it to lookup the
        // appropriate factory.
        final Byte factoryId = bytes[0];
        final FailureScopeFactory failureScopeFactory = _failureScopeFactoriesById.get(factoryId);

        FailureScope failureScope = null;

        if (failureScopeFactory != null) {
            failureScope = failureScopeFactory.toFailureScope(bytes);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "toFailureScope", failureScope);
        return failureScope;
    }
}
