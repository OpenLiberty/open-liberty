/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.Utils;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

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

    @Trivial
    public String getRecoveryIdentity() {
        return _recoveryIdentity;
    }

    /**
     * @return the _leaseTime
     */
    @Trivial
    public long getLeaseTime() {
        return _leaseTime;
    }

    /**
     * Has the peer expired?
     */
    @Trivial
    public boolean isExpired() {
        long curTime = System.currentTimeMillis();

        if (curTime - _leaseTime > _leaseTimeout * 1000) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Lease for " + _recoveryIdentity + " expired at " + Utils.traceTime(_leaseTime + _leaseTimeout));
            }
            return true;
        } else {
            if (tc.isDebugEnabled()) {
                int secondsLeft = (int) ((curTime + _leaseTimeout - _leaseTime) / 1000);
                Tr.debug(tc, "Lease for " + _recoveryIdentity + " has not expired. " + secondsLeft + " second" + (secondsLeft != 1 ? "s" : "") + " left.");
            }
            return false;
        }
    }
}
