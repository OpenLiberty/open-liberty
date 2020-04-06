package com.ibm.ws.Transaction.JTA;

import com.ibm.tx.TranConstants;
/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

public class ResourceWrapper implements StatefulResource {
    private static final TraceComponent tc = Tr.register(
                                                         ResourceWrapper.class,
                                                         TranConstants.TRACE_GROUP,
                                                         TranConstants.NLS_FILE);

    // StatefulResource.NONE has to be 0
    private int _resourceStatus;// = StatefulResource.NONE;

    /**
     * @return
     */
    @Override
    public int getResourceStatus() {
        return _resourceStatus;
    }

    /**
     * @param status
     */
    @Override
    public void setResourceStatus(int status) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setResourceStatus",
                     "from " +
                                              printResourceStatus(_resourceStatus) +
                                              " to " +
                                              printResourceStatus(status));

        _resourceStatus = status;
    }

    public static String printResourceStatus(int status) {
        switch (status) {
            case StatefulResource.NONE:
                return "NONE";
            case StatefulResource.REGISTERED:
                return "REGISTERED";
            case StatefulResource.PREPARED:
                return "PREPARED";
            case StatefulResource.COMPLETING:
                return "COMPLETING";
            case StatefulResource.COMPLETED:
                return "COMPLETED";
            case StatefulResource.COMMITTED:
                return "COMMITTED";
            case StatefulResource.ROLLEDBACK:
                return "ROLLEDBACK";
            case StatefulResource.HEURISTIC_COMMIT:
                return "HEURISTIC_COMMIT";
            case StatefulResource.HEURISTIC_ROLLBACK:
                return "HEURISTIC_ROLLBACK";
            case StatefulResource.HEURISTIC_MIXED:
                return "HEURISTIC_MIXED";
            case StatefulResource.HEURISTIC_HAZARD:
                return "HEURISTIC_HAZARD";
            case StatefulResource.COMPLETING_ONE_PHASE:
                return "COMPLETING_ONE_PHASE";
            default:
                return "ILLEGAL STATE";
        }
    }
}