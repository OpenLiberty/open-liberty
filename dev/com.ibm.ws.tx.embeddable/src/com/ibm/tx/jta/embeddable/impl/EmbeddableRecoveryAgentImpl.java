/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.embeddable.impl;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.impl.FailureScopeController;
import com.ibm.tx.jta.impl.TxRecoveryAgentImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.recoverylog.spi.FailureScope;
import com.ibm.ws.recoverylog.spi.RecoveryDirector;

/**
 *
 */
public class EmbeddableRecoveryAgentImpl extends TxRecoveryAgentImpl {

    private static final TraceComponent tc = Tr.register(EmbeddableRecoveryAgentImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /**
     * @param recoveryDirector
     * @throws Exception
     */
    public EmbeddableRecoveryAgentImpl(RecoveryDirector recoveryDirector) throws Exception {
        super(recoveryDirector);
    }

    @Override
    protected FailureScopeController createFailureScopeController(FailureScope currentFailureScope) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createFailureScopeController", currentFailureScope);

        FailureScopeController fsc = new EmbeddableFailureScopeController(currentFailureScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createFailureScopeController", fsc);

        return fsc;
    }
}
