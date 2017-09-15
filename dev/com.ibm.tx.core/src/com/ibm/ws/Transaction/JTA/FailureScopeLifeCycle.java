
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

import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.TranConstants;

//------------------------------------------------------------------------------
// Class: FailureScopeLifeCycle
//------------------------------------------------------------------------------
/**
* <p>
* 
* </p>
*/
public class FailureScopeLifeCycle
{
    // The count of requests currently on server for this FailureScope
    private int _activityCount;

    private boolean _disabled;

    private boolean _isLocal;

    private String _idStr;

    private static final TraceComponent tc = Tr.register(FailureScopeLifeCycle.class,TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    public FailureScopeLifeCycle(String idStr, boolean isLocal)
    {
        _isLocal = isLocal;
        _idStr = new String(idStr);
    }

    public boolean isLocal()
    {
        return _isLocal;
    }

    // This method determines whether the failureScope is currently processing new requests and returns
    // whether it is or not.  
    // If the failureScope is accepting requests it increments the count of transactions currently
    // processing 2PC requests on this server for this RM's failureScope.
    // If the failureScope is not accepting requests the caller should act upon the false boolean return value
    // to cease processing of the current request and take the necessary action for this condition.
    // NOTE this does not include active transactions.
    public synchronized boolean ifAcceptingWorkIncrementActivityCount()
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "isAcceptingWork", new Boolean(!_disabled));
        if(!_disabled)
        {   
            _activityCount++;
            if (tc.isDebugEnabled()) Tr.debug(tc, "_activityCount", new Object[]{this, new Integer(_activityCount)});
            return true;
        } 
        return false;
    }

    // This decrements the count of transactions currently processing 2PC requests
    // on this server for this RM's failureScope.
    // NOTE this does not include active transactions.
    public synchronized void decrementActivityCount()
    {
        _activityCount--;
        if(_activityCount == 0)
        {
            this.notifyAll();
        }
        if (tc.isDebugEnabled()) Tr.debug(tc, "decrementActivityCount", new Object[]{this, new Integer(_activityCount)});
    }

    public int getActivityCount()
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "getActivityCount", new Object[]{this, new Integer(_activityCount)});
        return _activityCount;
    }

    public void stopAcceptingWork()
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "stopAcceptingWork", this);
        _disabled = true;
    }

    public String getIdentityString()
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "getIdentityString", new Object[]{this,_idStr});
        return _idStr;
    }

}
