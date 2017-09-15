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
package com.ibm.ws.session;

import com.ibm.wsspi.session.IPerformanceMetrics;
import com.ibm.wsspi.session.ISession;
import com.ibm.wsspi.session.ISessionObserver;

public class SessionStatistics implements ISessionObserver, IPerformanceMetrics {

    protected long _created = 0;
    protected long _accessed = 0;
    protected long _active = 0;
    protected long _live = 0;
    protected long _invalidated = 0;
    protected long _invalidatedByTO = 0;
    protected long _invalidAccess = 0;
    protected long _affinityBreaks = 0;
    protected long _cacheDiscards = 0;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionCreated(com.ibm.wsspi.session
     * .ISession)
     */
    public void sessionCreated(ISession session) {
        synchronized (this) {
            _created++;
            _active++;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionAccessed(com.ibm.wsspi.session
     * .ISession)
     */
    public void sessionAccessed(ISession session) {
        synchronized (this) {
            _accessed++;
            _active++;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionDestroyed(com.ibm.wsspi.session
     * .ISession)
     */
    public void sessionDestroyed(ISession session) {
        synchronized (this) {
            _invalidated++;
            // TODO should it decrement active?
            // I think we will only increment active if it's accessed on a request
            // Therefore, backround invalidations shouldn't decrement and we should
            // decrement during a post-invoke even if the session has been invalidated
            // during a request
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionReleased(com.ibm.wsspi.session
     * .ISession)
     */
    public void sessionReleased(ISession session) {
        synchronized (this) {
            _active--;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionFlushed(com.ibm.wsspi.session
     * .ISession)
     */
    public void sessionFlushed(ISession session) {}

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionDidActivate(com.ibm.wsspi
     * .session.ISession)
     */
    public void sessionDidActivate(ISession session) {}

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionWillPassivate(com.ibm.wsspi
     * .session.ISession)
     */
    public void sessionWillPassivate(ISession session) {}

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.ISessionObserver#getId()
     */
    public String getId() {
        return "default statistic module";
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionDestroyedByTimeout(com.ibm
     * .wsspi.session.ISession)
     */
    public void sessionDestroyedByTimeout(ISession session) {
        synchronized (this) {
            _invalidatedByTO++;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionAccessUnknownKey(java.lang
     * .Object)
     */
    public void sessionAccessUnknownKey(Object key) {
        synchronized (this) {
            _invalidAccess++;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionAffinityBroke(com.ibm.wsspi
     * .session.ISession)
     */
    public void sessionAffinityBroke(ISession session) {
        synchronized (this) {
            _affinityBreaks++;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionCacheDiscard(java.lang.Object
     * )
     */
    public void sessionCacheDiscard(Object value) {
        synchronized (this) {
            _cacheDiscards++;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionLiveCountInc(java.lang.Object
     * )
     */
    public void sessionLiveCountInc(Object value) {
        synchronized (this) {
            _live++;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.ISessionObserver#sessionLiveCountDec(java.lang.Object
     * )
     */
    public void sessionLiveCountDec(Object value) {
        synchronized (this) {
            _live--;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.IPerformanceMetrics#getSessionsCreated()
     */
    public long getSessionsCreated() {
        return _created;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.IPerformanceMetrics#getInvalidatedSessions()
     */
    public long getInvalidatedSessions() {
        return _invalidated;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.IPerformanceMetrics#getActiveSessions()
     */
    public long getActiveSessions() {
        return _active;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.IPerformanceMetrics#getMemoryCount()
     */
    public long getMemoryCount() {
        return _live;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.IPerformanceMetrics#getCacheDiscards()
     */
    public long getCacheDiscards() {
        return _cacheDiscards;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.IPerformanceMetrics#getAffinityBreaks()
     */
    public long getAffinityBreaks() {
        return _affinityBreaks;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.IPerformanceMetrics#getInvalidatedByTimeout()
     */
    public long getInvalidatedByTimeout() {
        return _invalidatedByTO;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.wsspi.session.IPerformanceMetrics#getAccessToNonExistentSession()
     */
    public long getAccessToNonExistentSession() {
        return _invalidAccess;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.session.IPerformanceMetrics#getSessionAccessCount()
     */
    public long getSessionAccessCount() {
        return _accessed;
    }

    // The following are overwritten by our PMISessionStatistics class when in
    // WebSphere
    public void incSessionGarbageCollected(long invalidationTime) {}

    public void incNoRoomForNewSession() {}

    public void readTimes(long size, long time) {}

    public void writeTimes(long size, long time) {}

    public void incInvalidatorExecutedCount() {}

    public String toHTML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<br><b>Sessions Created:</b>").append(this._created).append("<br><b>Active Count:</b>").append(this._active).append("<br><b>Session Access Count:</b>").append(this._accessed).append("<br><b>Invalidated Sessions Count:</b>").append(this._invalidated).append("<br><b>Invalidated By SessionManager:</b>").append(this._invalidatedByTO).append("<br><b>SessionAffinity Breaks:</b>").append(this._affinityBreaks).append("<br><b>Cache Discards:</b>").append(this._cacheDiscards).append("<br><b>Attempts to access non-existent sessions:</b>").append(this._invalidAccess).append("<br><b>Session count </b>").append(this._live).append("<br>");
        return buf.toString();
    }
    
    /* (non-Javadoc)
     * @see com.ibm.wsspi.session.ISessionObserver#sessionIdChanged(java.lang.String, com.ibm.wsspi.session.ISession)
     */
    @Override
    public void sessionIdChanged(String oldId, ISession session) {
        //no stats right now        
    }

}
