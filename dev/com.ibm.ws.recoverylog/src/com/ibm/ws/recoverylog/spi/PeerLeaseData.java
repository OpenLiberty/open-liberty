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
import com.ibm.tx.util.Utils;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class PeerLeaseData {
    private static final TraceComponent tc = Tr.register(PeerLeaseData.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);
    private final String _recoveryIdentity;
    private final long _leaseTime;
    private final int _leaseTimeout;

    public PeerLeaseData(String recoveryIdentity, long leaseTime, int leaseTimeout) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "PeerLeaseData", new Object[] { recoveryIdentity, Utils.traceTime(leaseTime), leaseTimeout });
        this._recoveryIdentity = recoveryIdentity;
        this._leaseTime = leaseTime;
        this._leaseTimeout = leaseTimeout;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "PeerLeaseData");
    }

    public String getRecoveryIdentity() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getRecoveryIdentity", _recoveryIdentity);
        return _recoveryIdentity;
    }

    /**
     * @return the _leaseTime
     */
    public long getLeaseTime() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getLeaseTime", Utils.traceTime(_leaseTime));
        return _leaseTime;
    }

    /**
     * Has the peer expired?
     */
    public boolean isExpired() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isExpired", new Object[] { _leaseTimeout });
        boolean expired = false;
        long curTime = System.currentTimeMillis();

        if (curTime - _leaseTime > _leaseTimeout * 1000) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Lease has EXPIRED for " + _recoveryIdentity + ", currenttime: " + Utils.traceTime(curTime) + ", storedTime: " + Utils.traceTime(_leaseTime) + " ("
                             + (curTime - _leaseTime) / 1000 + "s)");
            }
            expired = true;
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Lease has not expired for " + _recoveryIdentity + ", currenttime: " + Utils.traceTime(curTime) + ", storedTime: " + Utils.traceTime(_leaseTime) + " ("
                             + (curTime - _leaseTime) / 1000 + "s)");
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isExpired", expired);
        return expired;
    }

}
