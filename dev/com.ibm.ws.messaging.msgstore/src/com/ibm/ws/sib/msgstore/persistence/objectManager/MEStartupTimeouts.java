package com.ibm.ws.sib.msgstore.persistence.objectManager;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;

public class MEStartupTimeouts
{
    private static TraceComponent tc = SibTr.register(MEStartupTimeouts.class, 
                                                      MessageStoreConstants.MSG_GROUP, 
                                                      MessageStoreConstants.MSG_BUNDLE);

    private long _retryTimeLimit;
    private long _retryWaitTime;


    public MEStartupTimeouts(MessageStoreImpl MS)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", "MS="+MS);

        try
        {
            if (MS != null)
            {
                // We need to work out whether to use the default
                // values or those supplied in the properties file
                // if any exist.
                String retryTimeLimit = MS.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_RETRY_TIME_LIMIT, 
                                                       MessageStoreConstants.PROP_OBJECT_MANAGER_RETRY_TIME_LIMIT_DEFAULT);
                    
                _retryTimeLimit = Long.parseLong(retryTimeLimit);
                
                
                String retryWaitTime = MS.getProperty(MessageStoreConstants.PROP_OBJECT_MANAGER_RETRY_WAIT_TIME, 
                                                      MessageStoreConstants.PROP_OBJECT_MANAGER_RETRY_WAIT_TIME_DEFAULT);
    
                _retryWaitTime = Long.parseLong(retryWaitTime);
            }
            else
            {
                // If we have a null MS then use the defaults
                // not that we are going to get very far.
                _retryTimeLimit = Long.parseLong(MessageStoreConstants.PROP_OBJECT_MANAGER_RETRY_TIME_LIMIT_DEFAULT);
                _retryWaitTime  = Long.parseLong(MessageStoreConstants.PROP_OBJECT_MANAGER_RETRY_WAIT_TIME_DEFAULT);
            }
        }
        catch (NumberFormatException nfe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(nfe, "com.ibm.ws.sib.msgstore.persistence.objectManager.MEStartupTimeouts.<init>", "1:72:1.3", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "NumberFormatException caught parsing properties! Using safe defaults.", nfe);

            // We were unable to parse what we have been given so lets go
            // with some safe values. Values were the defaults as of 2008
            _retryTimeLimit = 900000;  // 15 minutes
            _retryWaitTime  = 5000;    // 5 seconds
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, MessageStoreConstants.PROP_OBJECT_MANAGER_RETRY_TIME_LIMIT+"="+_retryTimeLimit);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, MessageStoreConstants.PROP_OBJECT_MANAGER_RETRY_WAIT_TIME+"="+_retryWaitTime);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }

    public long getRetryTimeLimit()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getRetryTimeLimit");
            SibTr.exit(this, tc, "getRetryTimeLimit", _retryTimeLimit);
        }
        return _retryTimeLimit;
    }

    public long getRetryWaitTime()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getRetryWaitTime");
            SibTr.exit(this, tc, "getRetryWaitTime", _retryWaitTime);
        }
        return _retryWaitTime;
    }
}
