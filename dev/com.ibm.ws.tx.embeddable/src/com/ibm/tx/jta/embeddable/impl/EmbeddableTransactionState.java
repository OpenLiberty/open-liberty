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
import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.tx.jta.impl.TransactionState;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;

/**
 *
 */
public class EmbeddableTransactionState extends TransactionState {
    private static final TraceComponent tc = Tr.register(EmbeddableTransactionState.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    /**
     * @param tran
     */
    public EmbeddableTransactionState(EmbeddableTransactionImpl tran) {
        super(tran);
    }

    @Override
    protected void logSupOrRecCoord() throws Exception
    {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "logSupOrRecCoord", this);

        final WSATRecoveryCoordinator wsatRC = ((EmbeddableTransactionImpl) _tran).getWSATRecoveryCoordinator();

        if (wsatRC != null)
        {
            final RecoverableUnitSection recoveryCoordSection = _tranLog.createSection(TransactionImpl.RECCOORD_WSAT_SECTION, true);
            recoveryCoordSection.addData(wsatRC.toLogData());

            if (traceOn && tc.isEventEnabled())
                Tr.event(tc, "WSATRecoveryCoordinator logged ", wsatRC);
        }
        else
        {
            // Throw exception to convert a prepare vote to rollback
            final NullPointerException npe = new NullPointerException("Null recovery coordinator");
            if (traceOn && tc.isEntryEnabled())
                Tr.exit(tc, "logSupOrRecCoord", npe);
            throw npe;
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "logSupOrRecCoord");
    }
}