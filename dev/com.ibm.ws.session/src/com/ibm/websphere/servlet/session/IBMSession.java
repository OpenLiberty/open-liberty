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
package com.ibm.websphere.servlet.session;

import javax.servlet.http.HttpSession;
import java.io.Externalizable;

/**
 * The IBMSession interface extends the javax.servlet.http.HttpSession interface of the
 * Servlet API to
 * <ui>
 * <li> limit the number of sessions in memory
 * <li> get at the user identity associated with session.
 * <li> control the timing of writing session changes to an external place.
 * </ui>
 * <p>
 * The WebSphere implementation of the http session object implements this interface.
 * </p>
 * <p>
 * With regards to overflow: When overflow is turned off in a non-persistent session mode, an invalid
 * session object is returned when the maximum capacity, specified by Max In Memory Session Count property, has been reached.
 * Example code:
 * <code>
 * IBMSession sess = (IBMSession) request.getSession();
 * if(sess.isOverFlow())
 * throw new ServletException("Maximum number of sessions reached on the server");
 * </code>
 * </p>
 * <p>
 * With regards to security: When security integration is turned on in the SessionManager,
 * WebSphere Application Server maintains the notion of an authenticated or unauthenticated
 * owner of a session. If a session is owned by an unauthenticated user (which we internally
 * denote via the user name of "anonymous"), then a servlet operating under the credentials
 * of any user can access the session, provided the request has the session identifier(from either cookie or
 * rewritten url). However, if the session is marked as being owned by an authenticated user
 * (where the user name is provided by the WebSphere Security API's and
 * management), then a servlet must be operating under the credentials of the
 * same user in order for WebSphere to return the requested session to the
 * servlet. A session gets denoted one time with the first authenticated user name
 * seen by the Application Server while processing the session. This can
 * either happen if the user has already been authenticated on the Http Request
 * which leads to the creation of the session, or it can happen on the first
 * authenticated user name seen after an "anonymous" session is created.
 * Example code:
 * <code>
 * IBMSession sess = (IBMSession) request.getSession();
 * String userName = sess.getUserName();
 * </code>
 * </p>
 * <p>
 * With regard to sync in persistent sessions mode(both database and memory-to-memory): The application
 * can control when to persist the http session updates to external store by calling the sync method on
 * this extension. Starting with WebSphere version 5.0, this can be called independent of the write
 * frequency selected in the SessionManager.
 * Example code:
 * <code>
 * IBMSession sess = (IBMSession) request.getSession();
 * sess.sync();
 * </code>
 * </p>
 * 
 * @see com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException
 * @see javax.servlet.http.HttpSession
 * @ibm-api
 */

public interface IBMSession extends HttpSession, Externalizable {
    /**
     * To get at the user identity associated with this session. When security integration is
     * turned off on the SessionManager, this method will always return "anonymous".
     * When security integration is turned on and the session is accessed in an
     * authenticated page, then this method returns the authenticated user name as set by the security
     * context. Otherwise, this method returns "anonymous".
     */
    public String getUserName();

    /**
     * To persist the session updates to external store. This is applicable only in persistent
     * sessions mode.
     */
    public void sync();

    /**
     * To determine if the number of sessions in memory has exceeded the value specified by the Max
     * In Memory Session Count property on the SessionManager. This is applicable only in non-persistent
     * sessions mode. In persistent sessions mode this method always returns false.
     */
    public boolean isOverflow();

    /*
     * To get the IBMApplicationSession associated with the session.
     * This can be called when in a converged (HTTP/SIP) application or when sharing session data.
     * The IBMApplicationSession will be created if it does not exist.
     * 
     * @return IBMApplicationSession
     * @since WAS 6.1
     */
    public IBMApplicationSession getIBMApplicationSession();

    /*
     * To get the IBMApplicationSession associated with the session.
     * This can be called when in a converged (HTTP/SIP) application or when sharing session data.
     * 
     * If the create parameter is true, this will return the IBMApplicationSession associated with the
     * session. The IBMApplicationSession will be created if it does not exist.
     * 
     * If the create parameter is false, this will return the IBMApplicationSession that has been
     * associated with the session. Otherwise, it returns null.
     * Note: In a converged (HTTP/SIP) application, if the IBMApplicationsession has not been associated
     * with the httpsession prior to this method being called, this will return null. The
     * IBMApplicationSession might not have been associated with the httpsession if cookies are
     * not enabled and the httpsession id is not in the encoded URL. If this is the case, the
     * getIBMApplicationSession method that does not include the parameter should be used.
     * 
     * @param create boolean that indicates if the IBMApplicationSession should be created
     * @return IBMApplicationSession
     * @since WAS 6.1
     * 
     */
    public IBMApplicationSession getIBMApplicationSession(boolean create);

}
