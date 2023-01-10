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

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class RegisteredRecoveryEventListeners implements RecoveryEventListener {
    private final static TraceComponent tc = Tr.register(
                                                         RegisteredRecoveryEventListeners.class,
                                                         TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);

    private final static RegisteredRecoveryEventListeners _instance = new RegisteredRecoveryEventListeners();

    private List<RecoveryEventListener> _listeners;

    public static RegisteredRecoveryEventListeners instance() {
        return _instance;
    }

    private RegisteredRecoveryEventListeners() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "<init>");
    }

    public synchronized void add(RecoveryEventListener rel) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "add", rel);

        if (_listeners == null)
            _listeners = new ArrayList<RecoveryEventListener>();

        _listeners.add(rel);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "add");
    }

    @Override
    public void failureOccurred(FailureScope fs) {
        if (_listeners == null)
            return;

        if (tc.isEntryEnabled())
            Tr.entry(tc, "failureOccurred", fs);

        for (int i = 0; i < _listeners.size(); i++) {
            RecoveryEventListener rel = _listeners.get(i);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Notifying " + rel);

            try {
                rel.failureOccurred(fs);
            } catch (Throwable t) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Exception notifying " + rel, t);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "failureOccurred");
    }

    @Override
    public void clientRecoveryInitiated(FailureScope fs, int clientId) {
        if (_listeners == null)
            return;

        if (tc.isEntryEnabled())
            Tr.entry(tc, "clientRecoveryInitiated", fs, clientId);

        for (int i = 0; i < _listeners.size(); i++) {
            RecoveryEventListener rel = _listeners.get(i);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Notifying " + rel);

            try {
                rel.clientRecoveryInitiated(fs, clientId);
            } catch (Throwable t) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Exception notifying " + rel, t);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "clientRecoveryInitiated");
    }

    @Override
    public void clientRecoveryComplete(FailureScope fs, int clientId) {
        if (_listeners == null)
            return;

        if (tc.isEntryEnabled())
            Tr.entry(tc, "clientRecoveryComplete", fs, clientId);

        for (int i = 0; i < _listeners.size(); i++) {
            RecoveryEventListener rel = _listeners.get(i);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Notifying " + rel);

            try {
                rel.clientRecoveryComplete(fs, clientId);
            } catch (Throwable t) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Exception notifying " + rel, t);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "clientRecoveryComplete");
    }

    @Override
    public void recoveryComplete(FailureScope fs) {
        if (_listeners == null)
            return;

        if (tc.isEntryEnabled())
            Tr.entry(tc, "recoveryComplete", fs);

        for (int i = 0; i < _listeners.size(); i++) {
            RecoveryEventListener rel = _listeners.get(i);

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Notifying " + rel);

            try {
                rel.recoveryComplete(fs);
            } catch (Throwable t) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Exception notifying " + rel, t);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recoveryComplete");
    }
}
