/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.traceinfo.ejbcontainer;

/**
 * Constants definitions of:
 * <ul>
 * <li>type identifiers for all the EJB Info trace object.
 * <li>String constants for other processings.
 * </ul>
 */
public interface TEInfoConstants
{
    // Values used for the type attribute.
    public static final int Unknown_Type = -1;
    public static final String Unknown_Type_Str = ">UNKWON";
    public static final int BMD_Type = 0;
    public static final String BMD_Type_Str = ">EJCBMD";
    public static final int MthdPreInvokeEntry_Type = 1;
    public static final String MthdPreInvokeEntry_Type_Str = ">EJCMPI";
    public static final int MthdPreInvokeExit_Type = 2;
    public static final String MthdPreInvokeExit_Type_Str = ">EJCMPO";
    public static final int MthdPreInvokeException_Type = 3;
    public static final String MthdPreInvokeException_Type_Str = ">EJCMPX";
    public static final int MthdPostInvokeEntry_Type = 4;
    public static final String MthdPostInvokeEntry_Type_Str = ">EJCMOI";
    public static final int MthdPostInvokeExit_Type = 5;
    public static final String MthdPostInvokeExit_Type_Str = ">EJCMOO";
    public static final int MthdPostInvokeException_Type = 6;
    public static final String MthdPostInvokeException_Type_Str = ">EJCMOX";
    public static final int BeanLifeCycle_EJBCallEntry_Type = 7;
    public static final String BeanLifeCycle_EJBCallEntry_Type_Str = ">EJCBCI";
    public static final int BeanLifeCycle_EJBCallExit_Type = 8;
    public static final String BeanLifeCycle_EJBCallExit_Type_Str = ">EJCBCO";
    public static final int BeanLifeCycle_State_Type = 9;
    public static final String BeanLifeCycle_State_Type_Str = ">EJCBLS";
    public static final int TxLifeCycle_Set_Tx_Type = 10;
    public static final String TxLifeCycle_Set_Tx_Type_Str = ">EJCTST";
    public static final int TxLifeCycle_State_Type = 11;
    public static final String TxLifeCycle_State_Type_Str = ">EJCTLS";

    // Transaction information identifiers
    static final String UnSpecifiedTxType = "Unspecified Tx";
    static final String LocalTxType = "Local Tx";
    static final String GlobalTxType = "Global Tx";
    static final String UserTxType = "User Tx";

    public static final int Tx_State_Unspecify_Type = 00;
    public static final int Tx_State_Local_Type = 10;
    public static final int Tx_State_Global_Type = 20;
    public static final int Tx_State_User_Type = 30;
    public static final int Tx_State_Set_Type = 40;

    public static final int Tx_State_Begin_Type = 0;
    public static final int Tx_State_Commit_Type = 1;
    public static final int Tx_State_Rollback_Type = 2;
    public static final int Tx_State_Suspend_Type = 3;
    public static final int Tx_State_Resume_Type = 4;
    public static final int Tx_State_Set_Timeout_Type = 5;
    public static final int Tx_State_Timeout_Type = 6;
    public static final int Tx_State_BeforeCompletion_Type = 7;
    public static final int Tx_State_AfterCompletion_Type = 8;

    public static final int Tx_State_Local_Begin_Type = Tx_State_Local_Type + Tx_State_Begin_Type;
    public static final int Tx_State_Local_Commit_Type = Tx_State_Local_Type + Tx_State_Commit_Type;
    public static final int Tx_State_Local_Rollback_Type = Tx_State_Local_Type + Tx_State_Rollback_Type;
    public static final int Tx_State_Local_Suspend_Type = Tx_State_Local_Type + Tx_State_Suspend_Type;
    public static final int Tx_State_Local_Resume_Type = Tx_State_Local_Type + Tx_State_Resume_Type;
    public static final int Tx_State_Local_Set_Timeout_Type = Tx_State_Local_Type + Tx_State_Set_Timeout_Type;
    public static final int Tx_State_Local_Timeout_Type = Tx_State_Local_Type + Tx_State_Timeout_Type;
    public static final int Tx_State_Local_BeforeCompletion_Type = Tx_State_Local_Type + Tx_State_BeforeCompletion_Type;
    public static final int Tx_State_Local_AfterCompletion_Type = Tx_State_Local_Type + Tx_State_AfterCompletion_Type;

    public static final int Tx_State_Global_Begin_Type = Tx_State_Global_Type + Tx_State_Begin_Type;
    public static final int Tx_State_Global_Commit_Type = Tx_State_Global_Type + Tx_State_Commit_Type;
    public static final int Tx_State_Global_Rollback_Type = Tx_State_Global_Type + Tx_State_Rollback_Type;
    public static final int Tx_State_Global_Suspend_Type = Tx_State_Global_Type + Tx_State_Suspend_Type;
    public static final int Tx_State_Global_Resume_Type = Tx_State_Global_Type + Tx_State_Resume_Type;
    public static final int Tx_State_Global_Set_Timeout_Type = Tx_State_Global_Type + Tx_State_Set_Timeout_Type;
    public static final int Tx_State_Global_Timeout_Type = Tx_State_Global_Type + Tx_State_Timeout_Type;
    public static final int Tx_State_Global_BeforeCompletion_Type = Tx_State_Global_Type + Tx_State_BeforeCompletion_Type;
    public static final int Tx_State_Global_AfterCompletion_Type = Tx_State_Global_Type + Tx_State_AfterCompletion_Type;

    public static final int Tx_State_User_Begin_Type = Tx_State_User_Type + Tx_State_Begin_Type;
    public static final int Tx_State_User_Commit_Type = Tx_State_User_Type + Tx_State_Commit_Type;
    public static final int Tx_State_User_Rollback_Type = Tx_State_User_Type + Tx_State_Rollback_Type;
    public static final int Tx_State_User_Suspend_Type = Tx_State_User_Type + Tx_State_Suspend_Type;
    public static final int Tx_State_User_Resume_Type = Tx_State_User_Type + Tx_State_Resume_Type;
    public static final int Tx_State_User_Set_Timeout_Type = Tx_State_User_Type + Tx_State_Set_Timeout_Type;
    public static final int Tx_State_User_Timeout_Type = Tx_State_User_Type + Tx_State_Timeout_Type;
    public static final int Tx_State_User_BeforeCompletion_Type = Tx_State_User_Type + Tx_State_BeforeCompletion_Type;
    public static final int Tx_State_User_AfterCompletion_Type = Tx_State_User_Type + Tx_State_AfterCompletion_Type;

    public static final int Tx_State_Set_Local_Tx_Type = Tx_State_Set_Type + 0;
    public static final int Tx_State_Set_Global_Tx_Type = Tx_State_Set_Type + 1;
    public static final int Tx_State_Set_User_Tx_Type = Tx_State_Set_Type + 2;

    public static final/* TE */String Tx_State_Text[][] =
    { // static final String UnSpecifiedTxType                           = "Unspecified Tx";
    { "Unspecified Transaction",
    },
                    // static final String LocalTxType                                 = "Local Tx";
                    { "Local Transaction - Begin",
                     "Local Transaction - Commit",
                     "Local Transaction - Rollback",
                     "Local Transaction - Suspend",
                     "Local Transaction - Resume",
                     "Local Transaction - Set_Timeout",
                     "Local Transaction - Timeout",
                     "Local Transaction - BeforeCompletion",
                     "Local Transaction - AfterCompletion",
    },
                    // static final String GlobalTxType                                = "Global Tx";
                    { "Global/User Transaction - Begin",
                     "Global/User Transaction - Commit",
                     "Global/User Transaction - Rollback",
                     "Global/User Transaction - Suspend",
                     "Global/User Transaction - Resume",
                     "Global/User Transaction - Set_Timeout",
                     "Global/User Transaction - Timeout",
                     "Global/User Transaction - BeforeCompletion",
                     "Global/User Transaction - AfterCompletion",
    },
                    // static final String UserTxType                                  = "User Tx";
                    { "User Transaction - Begin",
                     "User Transaction - Commit",
                     "User Transaction - Rollback",
                     "User Transaction - Suspend",
                     "User Transaction - Resume",
                     "User Transaction - Set_Timeout",
                     "User Transaction - Timeout",
                     "User Transaction - BeforeCompletion",
                     "User Transaction - AfterCompletion",
    },
    };

    public static final String TransactionNotSpecified = UnSpecifiedTxType;
    public static final String NoTransactionId = "No_Tx_Id";
    public static final String NoTransaction = TransactionNotSpecified + ":" + NoTransactionId;

    // Home bean identifiers.
    public static final String HomeMethodBeanId = "_HomeOfHome:Home_Method";
    public static final String HomeOfHomeJ2eeName = "__homeOfHomes#__homeOfHomes#__homeOfHomes";
    public static final String HomeOfHomeJ2eeName35 = "__homeOfHomes"; // d163197

    // Bean identifiers
    public static final String UnknownJ2eeName = "Non_EJB_Invocation";
    public static final String UnknownBeanId = "UnknownBeanId";
    public static final String NonEJBInvocationBeanId = UnknownJ2eeName + ":" + UnknownBeanId;

    // EJB method marker.
    public static final String EJBMethodMarker = "[[<<EJB>>]]";

    // Miscellaneous markers
    public static final String NullPointerMarker = "**NuLl**";
    public static final String UnknownValue = "Unknown";
    public static final String NotApplicable = "N/A";
    public static final String DataDelimiter = ">|<";
}
