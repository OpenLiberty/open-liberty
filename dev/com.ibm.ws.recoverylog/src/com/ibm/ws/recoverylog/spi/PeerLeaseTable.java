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

import com.ibm.tx.TranConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public class PeerLeaseTable {
    private static final TraceComponent tc = Tr.register(PeerLeaseTable.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected final ArrayList<PeerLeaseData> _peerLeaseTable;

    public PeerLeaseTable() {
        _peerLeaseTable = new ArrayList<PeerLeaseData>();
    }

    public void addPeerEntry(PeerLeaseData leaseData) {
        _peerLeaseTable.add(leaseData);
    }

    public ArrayList<String> getExpiredPeers() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getExpiredPeers");
        ArrayList<String> peersToRecover = new ArrayList<String>();

        for (PeerLeaseData p : _peerLeaseTable) {
            // Has the peer expired
            if (p.isExpired()) {
                peersToRecover.add(p.getRecoveryIdentity());
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getExpiredPeers", peersToRecover);
        return peersToRecover;
    }

    @Trivial
    public int size() {
        return _peerLeaseTable.size();
    }

    public void clear() {
        _peerLeaseTable.clear();
    }
}
