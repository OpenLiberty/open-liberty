/*******************************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.Transaction.JTA;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

/*
 *
 * A helper class that coverts X/Open XA specification return codes into a text string
 *
 */

public final class XAReturnCodeHelper
{

    /*
     *
     * Convert the supplied XA return code into its String notation.
     *
     */
    public static String convertXACode(int xaRC)
    {
        switch (xaRC)
        {
            // XAER Codes. Most frequently used order.
            case XAException.XAER_NOTA:
                // The XID is not valid. This may mean that the RM has unilaterally rollbacked the Tx.
                return "XAER_NOTA";

            case XAException.XAER_PROTO:
                // Routine invoked in an improper context. i.e. not in the correct order
                return "XAER_PROTO";

            case XAException.XAER_RMERR:
                // An unspecified resource manager error occurred in the transaction branch
                return "XAER_RMERR";

            case XAException.XAER_RMFAIL:
                // The Resource Manager is unavailable
                return "XAER_RMFAIL";

            case XAException.XAER_ASYNC:
                // An Asynchronous operation is already outstanding
                return "XAER_ASYNC";

            case XAException.XAER_DUPID:
                // The XID already exists
                return "XAER_DUPID";

            case XAException.XAER_INVAL:
                // Invalid arguments were given to the call
                return "XAER_INVAL";

            case XAException.XAER_OUTSIDE:
                // Resource manager doing work outside
                return "XAER_OUTSIDE";


            // XA Rollback Codes.
            case XAException.XA_RBROLLBACK:
                // The rollback was caused by an unspecified reason
                return "XA_RBROLLBACK";

            case XAException.XA_RBCOMMFAIL:
                // The rollback was caused by a communication failure
                return "XA_RBCOMMFAIL";

            case XAException.XA_RBDEADLOCK:
                // The rollback was caused by a deadlock
                return "XA_RBDEADLOCK";

            case XAException.XA_RBINTEGRITY:
                // The rollback was caused by a condition that violates the integrity of the resources
                return "XA_RBINTEGRITY";

            case XAException.XA_RBOTHER:
                // The rollback was caused for a reason not described by any other XA_RB* code
                return "XA_RBOTHER";

            case XAException.XA_RBPROTO:
                // The rollback was caused by a protocol error in the resource manager
                return "XA_RBPROTO";

            case XAException.XA_RBTIMEOUT:
                // The rollback was caused by the transaction branch taking too long and it timed out
                return "XA_RBTIMEOUT";

            case XAException.XA_RBTRANSIENT:
                // The rollback was caused by a temporary problem, the transaction branch operation maybe retried.
                return "XA_RBTRANSIENT";


            // XA Codes.
            case XAException.XA_HEURHAZ:
                // The transaction branch may have been heuristically completed
                return "XA_HEURHAZ";

            case XAException.XA_HEURCOM:
                // The transaction branch has been heuristically committed
                return "XA_HEURCOM";

            case XAException.XA_HEURRB:
                // The transaction branch has been heuristically rolled back
                return "XA_HEURRB";

            case XAException.XA_HEURMIX:
                // The transaction branch has been heuristically committed and rolled back
                return "XA_HEURMIX";

            case XAException.XA_NOMIGRATE:
                // Resumption must occur where suspension occurred
                return "XA_NOMIGRATE";

            case XAException.XA_RETRY:
                // Routine returned with no effect and may be re-issued
                return "XA_RETRY";

            case XAException.XA_RDONLY:
                // The transaction branch was read-only and has been committed
                return "XA_RDONLY";

            case XAResource.XA_OK:
                // Normal execution
                return "XA_OK";

            default:
                // Return Code not known
                Integer i = new Integer(xaRC);
                return i.toString();
        }
    }
}

