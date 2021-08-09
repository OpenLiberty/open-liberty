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

import com.ibm.ws.session.SessionStatistics;

/**
 * This class defines the contract between the base Session Manager and its
 * backend
 * store.
 * 
 */
public interface IStore {

    // XD methods
    /**
     * Allows for store specific shutdown processing.
     * */
    public ISession createSession(String id, Object multiProtocolCorrelator);

    public void shutdown();

    public ISession getSession(String id, Object multiProtocolCorrelator);

    /**
     * For stores which replicate, but do not allow remote fetch, this method
     * tests to see if a corrleator is owned by this IStore, and if it is not,
     * tells the session manager to redirect the request such that it can
     * be routed to the correct IStore
     * 
     * @param multiProtocolCorrelator
     *            - key used for routing
     * @return boolean - whether the request needs to be redirected
     */
    public boolean needToRedirect(Object multiProtocolCorrelator);

    public boolean idExists(String id, Object multiProtocolCorrelator);

    public void refreshSession(String sessionID, Object multiProtocolCorrelator);

    public void refreshSession(String sessionID, int currentVersion, Object multiProtocolCorrelator);

    // end of XD methods

    // ----------------------------------------
    // Public Methods
    // ----------------------------------------

    /**
     * Allows the store ID to be set. The convention is to name
     * the store with the name of the webapp (context root) whose sessions are
     * stored in this store. This activity can normally only be done on the first
     * request.
     * 
     * @param String
     *            storeID - Identifier to be assigned to the store.
     */
    public void setID(String storeID);

    /**
     * Allows the retrieval of the Store Identifier. The convention is that the
     * name of the store
     * is the name of the webapp (context root)
     * 
     * @return String
     */
    public String getId();

    /**
     * Creates an ISession and returns a reference to it.
     * It throws a SessionExistsException (RuntimeException) if the session
     * already exists.
     * 
     * @param String
     *            id - Session id of the ISession
     * @param boolean newId - tells us if this is a new Id
     * @return ISession
     */
    public ISession createSession(String id, boolean newId);

    /**
     * Returns an ISession with the specified id if one already exists.
     * It returns null otherwise.
     * 
     * @param String
     *            id - Session id of the ISession.
     * @param int version - Version of the ISession
     * @return ISession
     */
    public ISession getSession(String id, int version, boolean isSessionAccess, Object xdCorrelator);

    /**
     * Removes a session with the specified id
     * 
     * @param String
     *            id - Session id of the ISession.
     */
    public void removeSession(String id);

    /**
     * Returns true if the specified session id is in use by this
     * session manager or a session manager belonging to another web module.
     * 
     * @param String
     *            id - Session id of the ISession.
     * @return boolean true or false.
     */
    public boolean idExists(String id);

    /**
     * Refreshes the state of the session. It will
     * use the last update time related information to determine the
     * staleness of its local copy and accordingly reload the session
     * as required. This method (minus the currentVersion) may
     * be used by the Session manager when using URL rewriting
     * as the mode of affinity maintenance, when the version and
     * clone information may not be retrievable from the incoming request.
     * 
     * @param String
     *            sessionID - Session id of the requested session
     */
    public void refreshSession(String sessionID);

    /**
     * Refreshes the state of the session. The caller
     * passes in the currentVersion of the session as requested by
     * the client browser, which is presumed to be the latest (or most
     * current) version. If the in-memory copy of the session does not
     * have the same version as the currentVersion, the session
     * will be reloaded. This method is typically used when cookies
     * are the mode of affinity maintenance, and the session manager
     * has access to the version and clone id related information off
     * the incoming request.
     * 
     * @param String
     *            id - Session id of the requested session
     * @param int currentVersion - version of the ISession as perceived from the
     *        incoming request
     */
    public void refreshSession(String sessionID, int currentVersion);

    /*
     * Store behavior customization methods
     */
    /**
     * Allows the sessionmanager to set an object loader on this
     * backend store. The Loader indirectly provides the classloader that can be
     * used to
     * load this object from a backend store.
     * 
     * @param ILoader
     *            loader
     */
    public void setLoader(ILoader loader);

    /**
     * Allows the sessionManager to register a storeCallBack
     * object whose methods can be invoked by the store to intimate the
     * sessionManager of the happening of certain events such as session
     * invalidations.
     * 
     * @param IStoreCallback
     *            callback - Registered callback for the store to invoke on events.
     */
    public void setStoreCallback(IStoreCallback callback);

    /**
     * Allows the store creation process to customize the behavior of the store
     * so that it can share a store (and its associated session manager)
     * for the purpose of storing sessions belonging to different webapplication.s
     * 
     * @param boolean flag - has value true if the store must permit sharing of
     *        ISessions across webapps.
     */
    public void setShareAcrossWebapp(boolean flag);

    /**
     * Allows the store creation process to customize the behavior of the store
     * so that the store can spread out its contents across several partitions.
     * This configuration may only apply to certain kinds of stores that support
     * partitioning.
     * 
     * @param int num - number of partitions.
     */
    public void setNumOfPartitions(int num);

    /**
     * Allows the store creation process to change the max inactive
     * interval associated with sessions in this store, depending on
     * the setting in the web.xml
     * 
     * @param int interval - Inactivity interval
     */
    public void setMaxInactiveInterval(int interval);

    // /**
    // * Gets the Elector associated with this store.
    // * @return IElector
    // */
    // public IElector getElector();

    /**
     * For when we are not using affinity, this tells the store there
     * is one less reference on this session. When there are no references
     * on the session, the store will release the lock in the backing
     * store for the session
     * 
     * @param ISession
     *            session - Session to be released at the end of a request.
     */
    public void releaseSession(ISession session);

    /**
     * Method used to perform shutdown processing
     */
    public void stop();

    /**
     * Method used to write session attributes and other session specific
     * information to the backend.
     * This is only executed if we are writing these at certain time intervals
     */
    public void runTimeBasedWrites();

    /**
     * Method used to Invalidate sessions which have timed out. This typically
     * gets called by an alarm that is activated
     * at certain time intervals
     */
    public void runInvalidation();

    /**
     * Method used to determine if a session has timed out and then invalidate it.
     */
    public boolean checkSessionStillValid(ISession s, long accessedTime);

    /**
     * Method used to prepare the Thread to handle requests. This should set an appropriate classpath on the thread.
     * 
     */
    public void setThreadContext();

    /**
     * Method used to change the Thread's properties back to what they were before
     * a setThreadContext was called.
     * 
     */
    public void unsetThreadContext();

    /**
     * Method used to get the sessionStatistics instance of this store
     * 
     * @return SessionStatistics
     */
    public SessionStatistics getSessionStatistics();

    /**
     * Method used to set the sessionStatistics instance of this store
     * 
     * @return
     */
    public void setSessionStatistics(SessionStatistics ss);

    /**
     * Method used to get at the storeCallback instance of this store
     * 
     * @return IStoreCallback
     */
    public IStoreCallback getStoreCallback();

    /**
     * Method used to determine if we should reuse the id that we received if we
     * create a new session
     * 
     * @return boolean
     */
    public boolean getShouldReuseId();

    /**
     * Method used to get the object from Memory. If we are using a persistent
     * store, we will not look in the backend.
     * 
     * @param key
     * @return Object
     */
    public Object getFromMemory(Object key);

    /**
     * Method used to tell us if a session with this id is present in the external
     * store.
     * 
     * @param key
     * @return boolean
     */
    public boolean isPresentInExternalStore(String id);

    /**
     * Method to return whether this app has an HttpSessionListener implemented
     * 
     * @return boolean
     */
    public boolean isHttpSessionListener();

    /**
     * Method to set whether this app has an HttpSessionListener implemented
     * 
     * @param b
     *            boolean
     */
    public void setHttpSessionListener(boolean b);

    
    //NOT NEEDED since this won't be called upon invalidation
    //Method to set whether this app has an HttpSessionIdListener implemented
    //* 
    // * @param b
    // *            boolean
    //
    //public void setHttpSessionIdListener(boolean b);

    
    /**
     * Method to get an alternate affinity token for this session key.
     * Default token in WebSphere is the encoded server id, called the clone id.
     * The id+appName equals the key to use
     */
    public String getAffinityToken(String id, String appName);

    /**
     * Method to set whether or not this app is marked as distributable
     * 
     * @param b
     */
    public void setDistributable(boolean b);

    /**
     * Method to return whether this app is marked as distributable
     */
    public boolean isDistributable();

    public void removeFromMemory(String id);
    
    public void updateSessionId(String oldId, ISession newSession);
}