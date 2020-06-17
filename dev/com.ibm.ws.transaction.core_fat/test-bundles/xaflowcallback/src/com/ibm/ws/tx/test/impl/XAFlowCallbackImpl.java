/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tx.test.impl;

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.test.XAFlowCallback;

public class XAFlowCallbackImpl implements XAFlowCallback {
    private static final TraceComponent tc = Tr.register(XAFlowCallbackImpl.class,
                                                         TranConstants.TRACE_GROUP,
                                                         TranConstants.NLS_FILE);

    // Flow type definitions
    private static final int FORGET = 0;
    private static final int PREPARE = 1;
    private static final int COMMIT = 2;
    private static final int ROLLBACK = 3;

    @Override
    public boolean beforeXAFlow(int flowType, int flag) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "beforeXAFlow", new Object[] { getFlowType(flowType), getBeforeFlag(flag) });

        if (flowType == COMMIT) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Bringing server down");
            Runtime.getRuntime().halt(1);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "beforeXAFlow", true);
        return true;
    }

    @Override
    public boolean afterXAFlow(int flowType, int flag) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "afterXAFlow", true);
        return true;
    }

    private String getFlowType(int flowType) {
        final String strFlowType;

        switch (flowType) {
            case PREPARE:
                strFlowType = "PREPARE";
                break;
            case COMMIT:
                strFlowType = "COMMIT";
                break;
            case ROLLBACK:
                strFlowType = "ROLLBACK";
                break;
            case FORGET:
            default:
                strFlowType = "FORGET";
                break;
        }

        return strFlowType;
    }

    private String getBeforeFlag(int flag) {
        String strFlag = "";

        // Before flag definitions
        // FORGET_NORMAL         = 10;
        // PREPARE_NORMAL        = 20;
        // PREPARE_1PC_OPT       = 21;
        // COMMIT_2PC            = 30;
        // COMMIT_1PC_OPT        = 31;
        // ROLLBACK_NORMAL       = 40;
        // ROLLBACK_DUE_TO_ERROR = 41;
        switch (flag) {
            case 10:
                strFlag = "FORGET_NORMAL";
                break;
            case 20:
                strFlag = "PREPARE_NORMAL";
                break;
            case 21:
                strFlag = "PREPARE_1PC_OPT";
                break;
            case 30:
                strFlag = "COMMIT_2PC";
                break;
            case 31:
                strFlag = "COMMIT_1PC_OPT";
                break;
            case 40:
                strFlag = "ROLLBACK_NORMAL";
                break;
            case 41:
                strFlag = "ROLLBACK_DUE_TO_ERROR";
                break;
            default:
                strFlag = "UNEXPECTED FLAG " + flag;
                break;
        }

        return strFlag;
    }

    private String getAfterFlag(int flag) {
        String strFlag = "";

        // After flag definitions
        // AFTER_SUCCESS         = 50;
        // AFTER_FAIL            = 51;
        switch (flag) {
            case 50:
                strFlag = "SUCCESS";
                break;
            case 51:
                strFlag = "FAIL";
                break;
            default:
                strFlag = "UNEXPECTED FLAG " + flag;
                break;
        }

        return strFlag;
    }
}
