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

import java.util.Date;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

/**
 *
 */
public class PeerLeaseData {
    private static final TraceComponent tc = Tr.register(PeerLeaseData.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);
    private final String _recoveryIdentity;
    private final long _leaseTime;

    public PeerLeaseData(String recoveryIdentity, long leaseTime) {
        this._recoveryIdentity = recoveryIdentity;
        this._leaseTime = leaseTime;
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
            Tr.debug(tc, "getLeaseTime", _leaseTime);
        return _leaseTime;
    }

    /**
     * Has the peer expired?
     */
    public boolean isExpired() {
        boolean expired = false;
        long curTime = System.currentTimeMillis();
        //TODO:
        if (curTime - _leaseTime > ConfigurationProviderManager.getConfigurationProvider().getLeaseLength() * 1000) //  30 seconds default for timeout
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Lease has EXPIRED for " + _recoveryIdentity + ", currenttime: " + curTime + ", storedTime: " + _leaseTime);
            expired = true;
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Lease has not expired for " + _recoveryIdentity + ", currenttime: " + curTime + ", storedTime: " + _leaseTime);
        }

        return expired;
    }

    @Override
    public String toString() {
        return _recoveryIdentity + ", lease time: " + new Date(_leaseTime);
    }
}