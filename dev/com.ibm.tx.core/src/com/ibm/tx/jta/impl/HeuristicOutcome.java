package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.ws.Transaction.JTA.ResourceWrapper;
import com.ibm.ws.Transaction.JTA.StatefulResource;

// This is a table of states used for combining heuristic outcomes.
// Assumes Statefulesource.NONE = 0!!!!!
public final class HeuristicOutcome
{
    private static final TraceComponent tc =
        Tr.register(
                HeuristicOutcome.class,
                TranConstants.TRACE_GROUP,
                TranConstants.NLS_FILE);
    
    private static final int states[][] = new int[StatefulResource.numStates][StatefulResource.numStates];

    static
    {
        states[StatefulResource.REGISTERED][StatefulResource.HEURISTIC_COMMIT] = StatefulResource.HEURISTIC_COMMIT;
        states[StatefulResource.REGISTERED][StatefulResource.HEURISTIC_ROLLBACK] = StatefulResource.HEURISTIC_ROLLBACK;
        states[StatefulResource.REGISTERED][StatefulResource.HEURISTIC_MIXED] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.REGISTERED][StatefulResource.HEURISTIC_HAZARD] = StatefulResource.HEURISTIC_HAZARD;

        states[StatefulResource.PREPARED][StatefulResource.HEURISTIC_COMMIT] = StatefulResource.HEURISTIC_COMMIT;
        states[StatefulResource.PREPARED][StatefulResource.HEURISTIC_ROLLBACK] = StatefulResource.HEURISTIC_ROLLBACK;
        states[StatefulResource.PREPARED][StatefulResource.HEURISTIC_MIXED] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.PREPARED][StatefulResource.HEURISTIC_HAZARD] = StatefulResource.HEURISTIC_HAZARD;

        states[StatefulResource.COMPLETING][StatefulResource.HEURISTIC_COMMIT] = StatefulResource.HEURISTIC_COMMIT;
        states[StatefulResource.COMPLETING][StatefulResource.HEURISTIC_ROLLBACK] = StatefulResource.HEURISTIC_ROLLBACK;
        states[StatefulResource.COMPLETING][StatefulResource.HEURISTIC_MIXED] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.COMPLETING][StatefulResource.HEURISTIC_HAZARD] = StatefulResource.HEURISTIC_HAZARD;

        states[StatefulResource.COMPLETED][StatefulResource.HEURISTIC_COMMIT] = StatefulResource.HEURISTIC_COMMIT;
        states[StatefulResource.COMPLETED][StatefulResource.HEURISTIC_ROLLBACK] = StatefulResource.HEURISTIC_ROLLBACK;
        states[StatefulResource.COMPLETED][StatefulResource.HEURISTIC_MIXED] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.COMPLETED][StatefulResource.HEURISTIC_HAZARD] = StatefulResource.HEURISTIC_HAZARD;

        states[StatefulResource.COMPLETING_ONE_PHASE][StatefulResource.HEURISTIC_COMMIT] = StatefulResource.HEURISTIC_COMMIT;
        states[StatefulResource.COMPLETING_ONE_PHASE][StatefulResource.HEURISTIC_ROLLBACK] = StatefulResource.HEURISTIC_ROLLBACK;
        states[StatefulResource.COMPLETING_ONE_PHASE][StatefulResource.HEURISTIC_MIXED] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.COMPLETING_ONE_PHASE][StatefulResource.HEURISTIC_HAZARD] = StatefulResource.HEURISTIC_HAZARD;

        states[StatefulResource.ROLLEDBACK][StatefulResource.HEURISTIC_COMMIT] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.ROLLEDBACK][StatefulResource.HEURISTIC_ROLLBACK] = StatefulResource.HEURISTIC_ROLLBACK;
        states[StatefulResource.ROLLEDBACK][StatefulResource.HEURISTIC_MIXED] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.ROLLEDBACK][StatefulResource.HEURISTIC_HAZARD] = StatefulResource.HEURISTIC_HAZARD;
        states[StatefulResource.ROLLEDBACK][StatefulResource.COMMITTED] = StatefulResource.HEURISTIC_MIXED;

        states[StatefulResource.COMMITTED][StatefulResource.HEURISTIC_COMMIT] = StatefulResource.HEURISTIC_COMMIT;
        states[StatefulResource.COMMITTED][StatefulResource.HEURISTIC_ROLLBACK] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.COMMITTED][StatefulResource.HEURISTIC_MIXED] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.COMMITTED][StatefulResource.HEURISTIC_HAZARD] = StatefulResource.HEURISTIC_HAZARD;

        states[StatefulResource.HEURISTIC_COMMIT][StatefulResource.HEURISTIC_ROLLBACK] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.HEURISTIC_COMMIT][StatefulResource.HEURISTIC_MIXED] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.HEURISTIC_COMMIT][StatefulResource.HEURISTIC_HAZARD] = StatefulResource.HEURISTIC_HAZARD;

        states[StatefulResource.HEURISTIC_ROLLBACK][StatefulResource.HEURISTIC_MIXED] = StatefulResource.HEURISTIC_MIXED;
        states[StatefulResource.HEURISTIC_ROLLBACK][StatefulResource.HEURISTIC_HAZARD] = StatefulResource.HEURISTIC_HAZARD;

        states[StatefulResource.HEURISTIC_MIXED][StatefulResource.HEURISTIC_HAZARD] = StatefulResource.HEURISTIC_MIXED;
    }

    public static int combineStates(int left, int right)
    {
        int result;

        // Assumes StatefulResource.NONE == 0
        if(left == StatefulResource.NONE || left == StatefulResource.COMPLETED)
        {
        	result = right;
        }
        else if(right == StatefulResource.NONE || right == StatefulResource.COMPLETED)
        {
        	result = left;
        }
        else if(left == right)
        {
            result = left;
        }
        else if(left < right) // We only filled in one half of the matrix
        {
            result = states[left][right];
        }
        else 
        {    
            result = states[right][left];
        }

        if(tc.isDebugEnabled())
        {
            Tr.debug(tc, ResourceWrapper.printResourceStatus(left) + " + " + ResourceWrapper.printResourceStatus(right) + " = " + ResourceWrapper.printResourceStatus(result));
        }
        
        return result;
    }

	/**
	 * @param outcome
	 * @return
	 */
	public static boolean isHeuristic(int outcome)
    {
        switch(outcome)
        {
        case StatefulResource.HEURISTIC_COMMIT:
        case StatefulResource.HEURISTIC_MIXED:
        case StatefulResource.HEURISTIC_HAZARD:
        case StatefulResource.HEURISTIC_ROLLBACK:
            
            return true;
            
        default:
            
            return false;
        }
	}
}
