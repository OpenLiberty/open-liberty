/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.tx.jta.embeddable.impl;

import javax.transaction.SystemException;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.impl.FailureScopeController;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.recoverylog.spi.FailureScope;
import com.ibm.ws.recoverylog.spi.RecoveryAgent;
import com.ibm.ws.recoverylog.spi.RecoveryLog;

/**
 *
 */
public class EmbeddableFailureScopeController extends FailureScopeController {

    private static final TraceComponent tc = Tr.register(EmbeddableFailureScopeController.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    public EmbeddableFailureScopeController(FailureScope fs) throws SystemException {
        super(fs);
    }

    /**
     * Creates a RecoveryManager object instance and associates it with this FailureScopeController
     * The recovery manager handles recovery processing on behalf of the managed failure scope.
     *
     * @return String The new RecoveryManager instance.
     */
    @Override
    public void createRecoveryManager(RecoveryAgent agent, RecoveryLog tranLog, RecoveryLog xaLog, RecoveryLog recoverXaLog, byte[] defaultApplId, int defaultEpoch) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoveryManager", new Object[] { this, agent, tranLog, xaLog, recoverXaLog, defaultApplId, defaultEpoch });

        _tranLog = tranLog;
        _xaLog = xaLog;
        _recoverXaLog = recoverXaLog;
        _recoveryManager = new EmbeddableRecoveryManager(this, agent, tranLog, xaLog, recoverXaLog, defaultApplId, defaultEpoch);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoveryManager", _recoveryManager);
    }
}