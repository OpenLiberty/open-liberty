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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class defines the interface that is to be implemented by any protocol
 * Session Manager. The HTTP Session manager is one that will be implemented
 * currently, but the contract will be generic enough to support other Session
 * Managers.
 * <p>
 * 
 * @author Aditya Desai
 * 
 */
public interface IGenericSessionManager {

    // XD methods
    public IProtocolAdapter getProtocolAdapter();

    /**
     * Starting with the 6.1 in-core objectgrid support, while sessions are
     * replicated, the request
     * must be routed to the owning objectgrid for a given session (as OG will not
     * "fetch" the
     * session from a remote OG). As such, if the UCF updates on a failover have
     * not reached
     * the ODR before requests come in for a failed over session, and hence the
     * ODR sends to session
     * to a JVM which does not own the given session's shard, then the filter will
     * redirect the
     * session (up to a configured number of retries) with the idea that when the
     * reqeust comes back
     * to the ODR, it will have updated partition table.
     * <p>
     * 
     * @param ServletRequest
     *            req associated with the session.
     * @param AffinityContext
     *            affinityContext associated with a request
     * @return boolean need to do a http redirect
     */
    public boolean needToRedirect(ServletRequest req, SessionAffinityContext affinityContext, Object session);

    /**
     * This method is invoked by the user of the Session manager to determine
     * if the session id is valid in this sessions context or not.
     * <p>
     * 
     * @param String
     *            sessionID
     * @param int version number of the session if one is available.
     * @return boolean
     */
    public boolean isRequestedSessionIDValid(ServletRequest request, String sessionID, int version);

    // end of XD methods

    // ----------------------------------------
    // Public Methods
    // ----------------------------------------

    /**
     * Returns the String ID of this session manager.
     * Note that this id is not immutable, and that it
     * can be changed at a later point in time (for e.g.
     * when the first request is received)
     * 
     * @return String id of the session manager.
     */
    public String getID();

    /**
     * This method returns the HttpSessionManagerCustomizer object corresponding
     * to
     * this genericSessionManager.
     * <p>
     * 
     * @return ISessionManagerCustomizer
     */
    public ISessionManagerCustomizer getSessionManagerCustomizer();

    /**
     * Returns true or false depending on whether this Session Manager is shared
     * across web applications. The current granularity of this sharing is across
     * applications.
     * <p>
     * 
     * @return boolean
     */
    public boolean isSharedAcrossWebApps();

    /**
     * Returns the SessionAffinityManager associated with this session manager
     * if one has been registered with it.
     * <p>
     * 
     * @return ISessionAffinityManager
     */
    public ISessionAffinityManager getAffinityManager();

    /**
     * This method is invoked by the user of the Session Manager in order to
     * obtain an existing or newly created session. The received implementation
     * will already be in an adapted form, depending on the adapter registered
     * with the session manager customizer.
     * <p>
     * 
     * @param ServletRequest
     *            req associated with the session.
     * @param ServletResponse
     *            res associated with the session
     * @param AffinityContext
     *            affinityContext associated with a request
     * @param boolean create with values of true or false depending on whether the
     *        session should be created if absent.
     * @return Object Adapted form of the session.
     */
    public Object getSession(ServletRequest req, ServletResponse res, SessionAffinityContext affinityContext, boolean create);

    /**
     * This method is called to create a session. Event though getSesssion (above)
     * can optionally create
     * a session, it may be necessary to know whether the session exists, take
     * some action, and then create
     * the session. Calling getSession again to create the session would be
     * inefficient as it would again
     * check if the session exists, potentially accessing an external store.
     * 
     * @param ServletRequest
     *            req
     * @param ServletResponse
     *            res
     * @param SessionAffinityContext
     *            sac
     * @param boolean reuseId whether to unconditionally reuse the requesed id
     *        from the sac
     * @return Object Adapted form of the session
     */
    public Object createSession(ServletRequest req, ServletResponse res, SessionAffinityContext sac, boolean reuseId);

    /**
     * Get the ISession for the input id
     * 
     * @param id
     * @return ISession
     */
    public ISession getISession(String id);
    
    public Object generateNewId(HttpServletRequest _request, HttpServletResponse _response, SessionAffinityContext sac, ISession existingSession);

    /**
     * This method is used to get at the Protocol Specific Session Object
     * 
     * @param id
     * @return Object
     */
    public Object getSession(String id);

    public Object getSession(String id, boolean isSessionAccess);

    /**
     * This method is invoked by the user of the Session manager to determine
     * if the session id is valid in this sessions context or not.
     * <p>
     * 
     * @param String
     *            sessionID
     * @param int version number of the session if one is available.
     * @return boolean
     */
    public boolean isRequestedSessionIDValid(String sessionID, int version);

    /**
     * This method is invoked by the user of the Session manager to indicate the
     * end of processing of the request that had previously requested a session
     * object. This informs the Session manager that it is ok to passivate the
     * session if needed.
     * <p>
     * 
     * @param Object
     *            session Adapted form of the session that needs to be released.
     * @param SessionAffinityContext
     *            affinityContext object associated with a given request
     */
    public void releaseSession(Object session, SessionAffinityContext affinityContext);

    /**
     * This method is called when the session manager is being shutdown.
     */
    public void shutdown();

    /**
     * Get the IStore for this session manager
     * 
     * @return IStore
     */
    public IStore getIStore();

}