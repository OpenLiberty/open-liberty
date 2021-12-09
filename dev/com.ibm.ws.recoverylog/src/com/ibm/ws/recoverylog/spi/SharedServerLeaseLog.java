/*******************************************************************************
 * Copyright (c) 2010,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

//------------------------------------------------------------------------------
// Interface: SharedServerLeaseLog
//------------------------------------------------------------------------------
/**
 * <p>
 * The SharedServerLeaseLog interface provides methods for accessing shared
 * information on server leases.
 * </p>
 *
 */
public interface SharedServerLeaseLog {
    public void updateServerLease(String recoveryIdentity, String recoveryGroup, boolean isServerStartup) throws Exception;

    public void deleteServerLease(String recoveryIdentity) throws Exception;

    /**
     * @param recoveryIdentityToRecover
     * @param myRecoveryIdentity
     * @throws Exception
     */
    public boolean claimPeerLeaseForRecovery(String recoveryIdentityToRecover, String myRecoveryIdentity, LeaseInfo leaseInfo) throws Exception;

    /**
     * @param peerLeaseTable
     * @param recoveryGroup
     * @throws Exception
     */
    void getLeasesForPeers(final PeerLeaseTable peerLeaseTable, String recoveryGroup) throws Exception;

    public boolean lockLocalLease(String recoveryIdentity);

    public boolean releaseLocalLease(String recoveryIdentity) throws Exception;

    public boolean lockPeerLease(String recoveryIdentity);

    public boolean releasePeerLease(String recoveryIdentity) throws Exception;

    public void setPeerRecoveryLeaseTimeout(int leaseTimeout);

    /**
     * Inform the lease log that the server is stopping.
     */
    public void serverStopping();
}
