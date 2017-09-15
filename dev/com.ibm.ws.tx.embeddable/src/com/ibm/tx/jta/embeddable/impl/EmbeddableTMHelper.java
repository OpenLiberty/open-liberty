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
import com.ibm.tx.jta.impl.TxRecoveryAgentImpl;
import com.ibm.tx.jta.util.TxTMHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.recoverylog.spi.RecoveryDirector;

public class EmbeddableTMHelper extends TxTMHelper {

    private static final TraceComponent tc = Tr.register(EmbeddableTMHelper.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    @Override
    protected TxRecoveryAgentImpl createRecoveryAgent(RecoveryDirector recoveryDirector) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoveryAgent", recoveryDirector);

        TxRecoveryAgentImpl txAgent = new EmbeddableRecoveryAgentImpl(recoveryDirector);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoveryAgent", txAgent);

        return txAgent;
    }

}
