/*******************************************************************************
 * Copyright (c) 2002, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Interface: ClientId
//------------------------------------------------------------------------------
/**
 * This interface contains static definitions of client service identifiers, names
 * and RecoveryAgent sequence values. These are owned by the RLS to ensure
 * uniqueness and compatibility between client services and are required for
 * client service registration.
 */
public interface ClientId {
    /**
     * Recovery Log Client Identifiers (RLCI)
     */
    public static final int RLCI_TRANSACTIONSERVICE = 1;
    public static final int RLCI_ACTIVITYSERVICE = 2;
    public static final int RLCI_CSCOPESERVICE = 3;
    public static final int RLCI_ZTRANSACTIONSERVICE = 4;
    public static final int RLCI_RECOVERYLOG = -1;

    /**
     * Recovery Log Client Names (RLCN)
     */
    public static final String RLCN_TRANSACTIONSERVICE = "transaction";
    public static final String RLCN_ACTIVITYSERVICE = "activity";
    public static final String RLCN_CSCOPESERVICE = "compensation";
    public static final String RLCN_ZTRANSACTIONSERVICE = "Transaction.ws390";

    /**
     * Recovery Agent Sequence Values
     */
    public static final int RASEQ_TRANSACTIONSERVICE = 2;
    public static final int RASEQ_ACTIVITYSERVICE = 1;
    public static final int RASEQ_CSCOPESERVICE = 3;
    public static final int RASEQ_ZTRANSACTIONSERVICE = 2;
}
