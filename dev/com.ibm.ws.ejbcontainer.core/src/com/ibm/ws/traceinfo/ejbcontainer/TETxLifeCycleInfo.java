/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.traceinfo.ejbcontainer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Processor to write out and read in a EJB Transaction life cycle state records.
 */
public class TETxLifeCycleInfo implements TEInfoConstants
{
    private static final TraceComponent tc = Tr.register(TETxLifeCycleInfo.class,
                                                         "TEExplorer",
                                                         "com.ibm.ws.traceinfo.ejbcontainer");

    /**
     * This is called by the EJB container server code to write a
     * set transaction record to the trace log, if enabled.
     */
    public static void traceSetTxCommon(int opType, String txId, String desc)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            StringBuffer sbuf = new StringBuffer();

            sbuf
                            .append(TxLifeCycle_Set_Tx_Type_Str).append(DataDelimiter)
                            .append(TxLifeCycle_Set_Tx_Type).append(DataDelimiter)
                            .append(opType).append(DataDelimiter)
                            .append(txId).append(DataDelimiter)
                            .append(desc);
            Tr.debug(tc, sbuf.toString());
        }
    }

    /**
     * This is called by the EJB container server code to write a
     * set local transaction record to the trace log, if enabled.
     */
    public static void traceSetLocalTx(String txId, String desc)
    {
        traceSetTxCommon(Tx_State_Set_Local_Tx_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * set global transaction record to the trace log, if enabled.
     */
    public static void traceSetGlobalTx(String txId, String desc)
    {
        traceSetTxCommon(Tx_State_Set_Global_Tx_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * set user transaction record to the trace log, if enabled.
     */
    public static void traceSetUserTx(String txId, String desc)
    {
        traceSetTxCommon(Tx_State_Set_User_Tx_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * common record to the trace log, if enabled.
     */
    public static void traceCommon(int opType, String txId, String desc)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            StringBuffer sbuf = new StringBuffer();

            sbuf
                            .append(TxLifeCycle_State_Type_Str).append(DataDelimiter)
                            .append(TxLifeCycle_State_Type).append(DataDelimiter)
                            .append(opType).append(DataDelimiter)
                            .append(txId).append(DataDelimiter)
                            .append(desc);

            Tr.debug(tc, sbuf.toString());
        }
    }

    /**
     * This is called by the EJB container server code to write a
     * local transaction begin record to the trace log, if enabled.
     */
    public static void traceLocalTxBegin(String txId, String desc)
    {
        traceCommon(Tx_State_Local_Begin_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * local transaction commit record to the trace log, if enabled.
     */
    public static void traceLocalTxCommit(String txId, String desc)
    {
        traceCommon(Tx_State_Local_Commit_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * local transaction rollback record to the trace log, if enabled.
     */
    public static void traceLocalTxRollback(String txId, String desc)
    {
        traceCommon(Tx_State_Local_Rollback_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * local transaction suspend record to the trace log, if enabled.
     */
    public static void traceLocalTxSuspend(String txId, String desc)
    {
        traceCommon(Tx_State_Local_Suspend_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * local transaction resume record to the trace log, if enabled.
     */
    public static void traceLocalTxResume(String txId, String desc)
    {
        traceCommon(Tx_State_Local_Resume_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * local transaction set timeout record to the trace log, if enabled.
     */
    public static void traceLocalTxSetTimeout(String txId, String desc)
    {
        traceCommon(Tx_State_Local_Set_Timeout_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * local transaction timeout record to the trace log, if enabled.
     */
    public static void traceLocalTxTimeout(String txId, String desc)
    {
        traceCommon(Tx_State_Local_Timeout_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * local transaction before completion record to the trace log, if enabled.
     */
    public static void traceLocalTxBeforeCompletion(String txId, String desc)
    {
        traceCommon(Tx_State_Local_BeforeCompletion_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * local transaction before completion record to the trace log, if enabled.
     */
    public static void traceLocalTxAfterCompletion(String txId, String desc)
    {
        traceCommon(Tx_State_Local_AfterCompletion_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * global transaction begin record to the trace log, if enabled.
     */
    public static void traceGlobalTxBegin(String txId, String desc)
    {
        traceCommon(Tx_State_Global_Begin_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * global transaction commit record to the trace log, if enabled.
     */
    public static void traceGlobalTxCommit(String txId, String desc)
    {
        traceCommon(Tx_State_Global_Commit_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * global transaction rollback record to the trace log, if enabled.
     */
    public static void traceGlobalTxRollback(String txId, String desc)
    {
        traceCommon(Tx_State_Global_Rollback_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * global transaction suspend record to the trace log, if enabled.
     */
    public static void traceGlobalTxSuspend(String txId, String desc)
    {
        traceCommon(Tx_State_Global_Suspend_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * global transaction resume record to the trace log, if enabled.
     */
    public static void traceGlobalTxResume(String txId, String desc)
    {
        traceCommon(Tx_State_Global_Resume_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * global transaction set timeout record to the trace log, if enabled.
     */
    public static void traceGlobalTxSetTimeout(String txId, String desc)
    {
        traceCommon(Tx_State_Global_Set_Timeout_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * global transaction timeout record to the trace log, if enabled.
     */
    public static void traceGlobalTxTimeout(String txId, String desc)
    {
        traceCommon(Tx_State_Global_Timeout_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * global transaction before completion record to the trace log, if enabled.
     */
    public static void traceGlobalTxBeforeCompletion(String txId, String desc)
    {
        traceCommon(Tx_State_Global_BeforeCompletion_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * global transaction before completion record to the trace log, if enabled.
     */
    public static void traceGlobalTxAfterCompletion(String txId, String desc)
    {
        traceCommon(Tx_State_Global_AfterCompletion_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * user transaction begin record to the trace log, if enabled.
     */
    public static void traceUserTxBegin(String txId, String desc)
    {
        traceCommon(Tx_State_User_Begin_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * user transaction commit record to the trace log, if enabled.
     */
    public static void traceUserTxCommit(String txId, String desc)
    {
        traceCommon(Tx_State_User_Commit_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * user transaction rollback record to the trace log, if enabled.
     */
    public static void traceUserTxRollback(String txId, String desc)
    {
        traceCommon(Tx_State_User_Rollback_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * user transaction suspend record to the trace log, if enabled.
     */
    public static void traceUserTxSuspend(String txId, String desc)
    {
        traceCommon(Tx_State_User_Suspend_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * user transaction resume record to the trace log, if enabled.
     */
    public static void traceUserTxResume(String txId, String desc)
    {
        traceCommon(Tx_State_User_Resume_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * user transaction set timeout record to the trace log, if enabled.
     */
    public static void traceUserTxSetTimeout(String txId, String desc)
    {
        traceCommon(Tx_State_User_Set_Timeout_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * user transaction timeout record to the trace log, if enabled.
     */
    public static void traceUserTxTimeout(String txId, String desc)
    {
        traceCommon(Tx_State_User_Timeout_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * user transaction before completion record to the trace log, if enabled.
     */
    public static void traceUserTxBeforeCompletion(String txId, String desc)
    {
        traceCommon(Tx_State_User_BeforeCompletion_Type, txId, desc);
    }

    /**
     * This is called by the EJB container server code to write a
     * user transaction before completion record to the trace log, if enabled.
     */
    public static void traceUserTxAfterCompletion(String txId, String desc)
    {
        traceCommon(Tx_State_User_AfterCompletion_Type, txId, desc);
    }

    // PQ74774 Begins
    /**
     * Returns true if trace for this class is enabled. This is used to guard the
     * caller to avoid unncessary processing before the trace is depositied.
     */
    public static boolean isTraceEnabled()
    {
        return (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled());
    }
    // PQ74774 Ends
}
