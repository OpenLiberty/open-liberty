/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.session;

/**
 * This interface provides a handle to query statistics about the sessions that
 * are
 * being managed by a Session Manager. These will be available as methods off
 * of a management interface (like an MBean) for external querying.
 * 
 */
public interface IPerformanceMetrics {

    /**
     * Returns the total number of sessions that are created (CountStatistic).
     */
    public long getSessionsCreated();

    /**
     * Returns the total number of sessions that are invalidated (CountStatistic).
     */
    public long getInvalidatedSessions();

    /**
     * Returns the total number of sessions that are currently accessed by
     * requests (RangeStatistic).
     */
    public long getActiveSessions();

    /**
     * Returns the total number of sessions that currently live in memory
     * (RangeStatistic).
     */
    public long getMemoryCount();

    /**
     * Returns the total number of session objects that are forced out of the
     * cache (CountStatistic).
     */
    public long getCacheDiscards();

    /**
     * Returns the total number of HTTP session affinities that are broken, not
     * counting WebSphere Application Server intentional breaks of session
     * affinity (CountStatistic).
     */
    public long getAffinityBreaks();

    /**
     * Returns the total number of sessions that are invalidated with timeout
     * (CountStatistic).
     */
    public long getInvalidatedByTimeout();

    /**
     * Returns the total number of requests for a session that no longer exists,
     * presumably because the session timed out (CountStatistic).
     */
    public long getAccessToNonExistentSession();

    /**
     * Returns the total total number of requests for valid sessions (session that
     * exists)
     * 
     */
    public long getSessionAccessCount();

}
