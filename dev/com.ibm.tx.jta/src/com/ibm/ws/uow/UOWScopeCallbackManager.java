/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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

package com.ibm.ws.uow;

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.JTA.Util;

public class UOWScopeCallbackManager {
    private static final TraceComponent tc = Tr.register(UOWScopeCallbackManager.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    // A list of callbacks that are interested in
    // UOW scope context changes
    private ArrayList<UOWScopeCallback> _callbacks;

    public void addCallback(UOWScopeCallback callback) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addCallback", callback);

        if (_callbacks == null) {
            _callbacks = new ArrayList<UOWScopeCallback>();
        }

        _callbacks.add(callback);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addCallback");
    }

    public void removeCallback(UOWScopeCallback callback) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "removeCallback", callback);

        if (_callbacks != null) {
            final boolean result = _callbacks.remove(callback);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "callback found/removed: " + result);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "removeCallback");
    }

    public void notifyCallbacks(int contextChangeType, UOWScope scope) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "notifyCallbacks", new Object[] { Util.printUOWStatusChangeType(contextChangeType), scope, this });

        if (_callbacks != null) {
            final Iterator<UOWScopeCallback> callbacks = _callbacks.iterator();

            while (callbacks.hasNext()) {
                callbacks.next().contextChange(contextChangeType, scope);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "notifyCallbacks");
    }
}
