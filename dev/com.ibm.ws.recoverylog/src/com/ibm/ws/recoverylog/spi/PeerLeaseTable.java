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

import java.util.ArrayList;
import java.util.HashMap;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class PeerLeaseTable {
    private static final TraceComponent tc = Tr.register(PeerLeaseTable.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected final ArrayList<PeerLeaseData> _peerLeaseTable;

    private static HashMap<String, Integer> _expiryCounts = new HashMap<String, Integer>();

    public PeerLeaseTable() {
        _peerLeaseTable = new ArrayList<PeerLeaseData>();
    }

    public void addPeerEntry(PeerLeaseData leaseData) {
        _peerLeaseTable.add(leaseData);
    }

    public ArrayList<String> getExpiredPeers() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getExpiredPeers");
        ArrayList<String> expiredPeers = new ArrayList<String>();

        for (PeerLeaseData p : _peerLeaseTable) {
            // Has the peer expired
            if (p.isExpired()) {
                expiredPeers.add(p.getRecoveryIdentity());
            }
        }

        // Prune the expiry counts
        _expiryCounts.keySet().retainAll(expiredPeers);

        final int expiryThreshold = ConfigurationProviderManager.getConfigurationProvider().getLeaseExpiryThreshold();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Expiry threshold: {0}, {1}", expiryThreshold, _expiryCounts);

        if (expiryThreshold > 1) {
            final ArrayList<String> peersToRecover = new ArrayList<String>();

            // Record that we've seen them and check whether the threshold is reached
            for (String recoveryId : expiredPeers) {
                _expiryCounts.compute(recoveryId, (k, v) -> v == null ? 1 : v + 1);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Expiry counts: {0}", _expiryCounts);

                if (_expiryCounts.get(recoveryId) >= expiryThreshold) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Number of times {0} has been seen expired: {1}", recoveryId, _expiryCounts.get(recoveryId));
                    peersToRecover.add(recoveryId);
                }
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "getExpiredPeers", peersToRecover);
            return peersToRecover;
        } else {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getExpiredPeers", expiredPeers);
            return expiredPeers;
        }
    }

    @Trivial
    public int size() {
        return _peerLeaseTable.size();
    }

    public void clear() {
        _peerLeaseTable.clear();
    }
}
