/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.Transaction.JTA;

import java.util.Hashtable;

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.tx.TranConstants;

import com.ibm.ws.recoverylog.spi.FailureScope;

//------------------------------------------------------------------------------
// Class: FailureScopeLifeCycleHelper
//------------------------------------------------------------------------------
/**
* 
*/
public class FailureScopeLifeCycleHelper
{
    private static final TraceComponent tc = Tr.register(FailureScopeLifeCycleHelper.class,TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    // Dummy string which will be overwritten by WAS implementation.
    protected static String _non_null_identityString = "_local_transaction_service_identityString";
    protected static final Hashtable<String, FailureScopeLifeCycle> _activeFSLC = new Hashtable<String, FailureScopeLifeCycle>();

    public static FailureScopeLifeCycle addToActiveList(FailureScope fs, boolean isLocal)
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "addToActiveList",new Object[]{fs, new Boolean(isLocal)});

        if(fs == null)
        {
            if (tc.isEntryEnabled()) Tr.exit(tc, "addToActiveList", null);
            return null;
        }

        // We can skip explicit synchronization between activation and deactivation threads
        // since removeFromActiveList() is only called AFTER prepareToShutdown().
        final FailureScopeLifeCycle fslc = new FailureScopeLifeCycle(_non_null_identityString, isLocal);
        _activeFSLC.put(_non_null_identityString, fslc);

        if (tc.isEntryEnabled()) Tr.exit(tc, "addToActiveList", fslc);
        return fslc;
    }

    public static void removeFromActiveList(FailureScopeLifeCycle fslc)
    {
        /*
         * Caller must ensure that this method is not called before addToActiveList for the same failureScope
         */
        if (tc.isEntryEnabled()) Tr.entry(tc, "removeFromActiveList", fslc);        

        if(fslc!=null)
        {
            synchronized(fslc)
            {
                fslc.stopAcceptingWork();  
                while(fslc.getActivityCount() >0)
                {
                    try
                    {
                        fslc.wait();
                    }
                    catch(InterruptedException ie)
                    {
                    }
                }

                _activeFSLC.remove(fslc.getIdentityString());    
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "removeFromActiveList");
    }
}
