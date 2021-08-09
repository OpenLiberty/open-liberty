/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.session.monitor;

/**
 * Management interface for the MBean "WebSphere:type=SessionStats".
 * The Liberty profile makes this MBean available in its platform MBean server when the monitor-1.0 feature is
 * enabled to allow monitoring of the Session.
 * 
 * @ibm-api
 * 
 */
public interface SessionStatsMXBean {

    /**
     * The number of concurrently active sessions.
     * A session is active if the WebSphere Application Server is currently processing a request that uses that session.
     */
    public long getActiveCount();

    /**
     * The number of local sessions that are currently cached in memory
     * from the time at which this metric is enabled
     */
    public long getLiveCount();

    /**
     * The number of sessions that were created
     */
    public long getCreateCount();

    /**
     * The number of sessions that were invalidated caused by timeout
     */
    public long getInvalidatedCountbyTimeout();

    /**
     * The number of sessions that were invalidated
     */
    public long getInvalidatedCount();
}
