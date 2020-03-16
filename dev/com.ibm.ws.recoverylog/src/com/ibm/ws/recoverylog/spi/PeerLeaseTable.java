/*******************************************************************************
 * Copyright (c) 2014,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

import java.util.ArrayList;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

/**
 *
 */
public class PeerLeaseTable {
    private static final TraceComponent tc = Tr.register(PeerLeaseTable.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected final ArrayList<PeerLeaseData> _peerLeaseTable;

    public PeerLeaseTable() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "PeerLeaseTable");

        _peerLeaseTable = new ArrayList<PeerLeaseData>();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "PeerLeaseTable");
    }

    public void addPeerEntry(PeerLeaseData leaseData) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "addPeerEntry", leaseData);

        _peerLeaseTable.add(leaseData);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "addPeerEntry");
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

    public int size() {
        return _peerLeaseTable.size();
    }

}
