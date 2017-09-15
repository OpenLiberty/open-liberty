/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

/**
 *
 */
public class PeerLeaseData {
    private static final TraceComponent tc = Tr.register(PeerLeaseData.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);
    private final String _recoveryIdentity;
    private final long _leaseTime;
    private final int _leaseTimeout;

    public PeerLeaseData(String recoveryIdentity, long leaseTime, int leaseTimeout)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "PeerLeaseData", new Object[] { recoveryIdentity, leaseTime, leaseTimeout });
        this._recoveryIdentity = recoveryIdentity;
        this._leaseTime = leaseTime;
        this._leaseTimeout = leaseTimeout;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "PeerLeaseData");
    }

    public String getRecoveryIdentity()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getRecoveryIdentity");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getRecoveryIdentity", _recoveryIdentity);
        return _recoveryIdentity;
    }

    /**
     * @return the _leaseTime
     */
    public long getLeaseTime() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getLeaseTime");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getLeaseTime", _leaseTime);
        return _leaseTime;
    }

    /**
     * Has the peer expired?
     */
    public boolean isExpired()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isExpired");
        boolean expired = false;
        long curTime = System.currentTimeMillis();
        //TODO:
        if (curTime - _leaseTime > _leaseTimeout * 1000) //  30 seconds default for timeout
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Lease has EXPIRED for " + _recoveryIdentity + ", currenttime: " + curTime + ", storedTime: " + _leaseTime);
            expired = true;
        }
        else
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Lease has not expired for " + _recoveryIdentity + ", currenttime: " + curTime + ", storedTime: " + _leaseTime);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isExpired", expired);
        return expired;
    }

}
