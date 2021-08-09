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

public interface IStoreCallback {

    // XD methods
    public void sessionAttributeSet(ISession session, Object name, Object oldValue, Object newValue);

    public void sessionAttributeRemoved(ISession session, Object name, Object value);

    // end of XD methods

    /**
     * Allows the store to register an observer with the session manager.
     */
    public void registerSessionObserver(ISessionObserver observer);

    /*
     * Session related callback methods
     */
    /**
     * Invoked when a session is loaded by the IStore from persistent
     * storage into memory.
     * 
     * @param ISession
     *            session that got activated
     */
    public void sessionDidActivate(ISession session);

    /**
     * Invoked just before a session is flushed out to disk probably because it
     * is one that is the least recently used session.
     * 
     * @param ISession
     *            session that will passivate
     * @return boolean
     */
    public boolean sessionWillPassivate(ISession session);

    /**
     * Invoked after a session is flushed out to disk.
     * 
     * @param ISession
     *            session that was flushed out
     */
    public void sessionFlushed(ISession session);

    /**
     * Invoked after a session was invalidated. Note that in this paradigm
     * invalidations are driven by the store. Certain stores may do this by using
     * a dedicated thread or timer, while others like Objectgrid may rely on the
     * persistence mechanism to get prompted with notifications about
     * invalidations.
     * 
     * @param ISession
     *            session that will shortly be invalidated.
     * @return boolean
     */
    public boolean sessionInvalidated(ISession session);

    /**
     * 
     * @param session
     */
    public boolean sessionInvalidatedByTimeout(ISession session);

    public void sessionAffinityBroke(ISession session);

    /*
     * Session attributes and internals related callback methods.
     */
    /**
     * Invoked when an attribute is set on the ISession.
     * 
     * @param ISession
     *            session on which an attribute it set.
     * @param Object
     *            name of the attribute that was set.
     * @param Object
     *            oldValue of the attribute that was just modified.
     * @param Object
     *            newValue that the attribute was set to.
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionAttributeSet(com.ibm.wsspi.session.ISession, java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public void sessionAttributeSet(ISession session, Object name, Object oldValue, Boolean oldIsListener, Object newValue, Boolean newIsListener);

    /**
     * Invoked when an attribute is removed from the ISession.
     * 
     * @param ISession
     *            session whose attribute was removed.
     * @param Object
     *            name of the attribute that was removed.
     * @param Object
     *            value of the removed attribute.
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionAttributeRemoved(com.ibm.wsspi.session.ISession, java.lang.Object, java.lang.Object)
     */
    public void sessionAttributeRemoved(ISession session, Object name, Object value, Boolean oldIsBindingListener);

    /**
     * Invoked when an attribute from the ISession is accessed.
     * 
     * @param ISession
     *            session whose attribute was accessed.
     * @param Object
     *            key of the attribute that was accessed.
     * @param Object
     *            value of the accessed attribute.
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionAttributeAccessed(com.ibm.wsspi.session.ISession, java.lang.Object, java.lang.Object)
     */
    public void sessionAttributeAccessed(ISession session, Object key, Object value);

    /**
     * Invoked when the user name is set on the ISession.
     * 
     * @param ISession
     *            session on which the user name is set
     * @param String
     *            oldUserName that was replaced, null if this was set for the first
     *            time
     * @param String
     *            newUserName that was used to replace the user name.
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionUserNameSet(com.ibm.wsspi.session.ISession, java.lang.String, java.lang.String)
     */
    public void sessionUserNameSet(ISession session, String oldUserName, String newUserName);

    /**
     * Invoked by the IStore when the last access time is set on the ISession
     * 
     * @param ISession
     *            session on which the last access time is set
     * @param long old access time that was replaced, 0 if this was set for the
     *        first time
     * @param long newaccess time that was used to replace the original access
     *        time.
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionLastAccessTimeSet(com.ibm.wsspi.session.ISession, long, long)
     */
    public void sessionLastAccessTimeSet(ISession session, long old, long newaccess);

    /**
     * Invoked by the IStore when the max inactive time is set on the ISession
     * 
     * @param ISession
     *            session on which the max inactive time is set
     * @param int old max inactive time that was replaced, 0 if this was set for
     *        the first time
     * @param long newval of the inactive time that was used to replace the
     *        original inactive time.
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionMaxInactiveTimeSet(com.ibm.wsspi.session.ISession, int, int)
     */
    public void sessionMaxInactiveTimeSet(ISession session, int old, int newval);

    /**
     * Invoked by the IStore when the expiry time is set on the ISession. The
     * expiry time
     * is an absolute value as against the max inactive interval that only defines
     * a
     * period of inactivity after which the session is to be invalidated.
     * 
     * @param ISession
     *            session on which the expiry time is set
     * @param long old expiry time that was replaced, 0 if this was set for the
     *        first time
     * @param long newone expiry time that was used to replace the original
     *        inactive time.
     * @see com.ibm.wsspi.session.ISessionStateObserver#sessionExpiryTimeSet(com.ibm.wsspi.session.ISession, long, long)
     */
    public void sessionExpiryTimeSet(ISession session, long old, long newone);

    /**
     * Invoked by the IStore when an ISession is discarded from the store.
     * 
     * @param Object
     *            value of the Session that was discarded
     */
    public void sessionCacheDiscard(Object value);

    /**
     * Invoked by the IStore when it increments its count of the number of
     * live references to the ISession that exist.
     * 
     * @param Object
     *            value of the Session that was inserted
     */
    public void sessionLiveCountInc(Object value);

    /**
     * Invoked by the IStore when it decrements its count of the number of live
     * references to the ISession, when an active request completes.
     * 
     * @param Object
     *            value of the Session that was discarded
     */
    public void sessionLiveCountDec(Object value);
    
    /**
     * Invoked by the IStore when a session was application invalidated
     * @param ISession session which was application invalidated
     */
    public void sessionReleased(ISession session); //Introduced by PM66889 in tWAS, adding in as part of PM87133

}