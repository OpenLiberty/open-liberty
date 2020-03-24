/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.recoverylog.spi;

/**
 * The currency of a HeartbeatLog is maintained through a heartbeat() method. The currency can be tested through the isLogStale() method.
 *
 * This interface was introduced to allow a server to maintain ownership of its Transaction Recovery Logs where those logs are held in a
 * database. Rather than using a long duration locking scheme, the approach is to allow a first server to maintain its currency by updating
 * a timestamp in its DB table through the heartbeat() method. A second server can test whether the first server is still actively working
 * its recovery logs by calling isLogStale() against the appropriate table.
 *
 */
public interface HeartbeatLog {

    /**
     * Used to maintain the liveness of the Recovery Log.
     *
     * @throws LogClosedException
     *
     */
    public void heartBeat() throws LogClosedException;

    /**
     * Claim ownership of the Local Server's Recovery Logs.
     *
     * @return true if the log has been successfully claimed, false otherwise.
     */
    public boolean claimLocalRecoveryLogs();

    /**
     * Claim ownership of a Peer Server's Recovery Logs.
     *
     * @return true if the log has been successfully claimed, false otherwise.
     */
    public boolean claimPeerRecoveryLogs();

    /**
     * Set time interval that specifies how long
     * before a log goes stale under the HA DB Peer locking scheme.
     */
    public void setTimeBeforeLogStale(int timeBeforeStale);

    /**
     * Set the heartbeat time interval for the HA DB Peer locking
     * scheme.
     */
    public void setTimeBetweenHeartbeats(int timeBetweenHeartbeats);

    /**
     * Signals to the Recovery Log that the server is stopping.
     */
    public void serverStopping();
}
