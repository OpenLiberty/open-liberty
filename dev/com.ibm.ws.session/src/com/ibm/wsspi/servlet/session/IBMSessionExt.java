/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.servlet.session;

import com.ibm.websphere.servlet.session.IBMSession;

/**
 * The IBMSessionExt interface extends the javax.servlet.http.HttpSession
 * interface of the
 * Servlet API to
 * <ui> <li>Invalidate all sessions with the same session id as the current
 * session. </ui>
 * <p>
 * WebSphere implementation of http session object implements this interface. <br>
 * <br>
 * Example code: <br>
 * <br>
 * <code>
 * IBMSessionExt sessExt = (IBMSessionExt)request.getSession();<br>
 * sessExt.invalidateAll(true);
 * </code>
 * </p>
 * 
 * @ibm-spi
 */

public interface IBMSessionExt extends IBMSession {
    /**
     * To invalidate all session across web applications with the same session id
     * as the current session.
     * 
     * <br>
     * If "remote" is false, the scope is limited to the JVM. However, if
     * persistent sessions
     * are enabled, the session is removed from the back-end store. This will
     * prevent any other
     * servers from retrieving that session should that server not have the
     * session cached or
     * if its cached copy is determined to be invalid. <br>
     * If "remote" is true, the invalidation
     * request is also broadcast to all other application servers
     * that are part of the same High Available Manager (HAMgr) core-group. This
     * defaults to the
     * cell, but there may be multiple core-groups in a cell if explicitly
     * configured.
     * A single core-group can never span multiple cells. <br>
     * Remote invalidations are not processed immediately, but on the next run of
     * the background
     * session invalidator thread. This will happen in at most six minutes, unless
     * the
     * HttpSessionReaperPollInterval property is set, in which case this property
     * setting specifies
     * the maximun time interval. If a remotely invalidated session is requested
     * (with req.getSession())
     * prior to this interval expiring, the session will be immediately
     * invalidated
     * so that it will not be given to the application. <br>
     * If Java 2 security is enabled, accessing this method requires
     * com.ibm.websphere.security.WebSphereRuntimePermission with target name
     * "accessInvalidateAll".
     * 
     * 
     * @return void
     * @throws java.lang.SecurityException
     * @since WAS 6.1
     * 
     */
    public void invalidateAll(boolean remote) throws java.lang.SecurityException;

    /**
     * Same as invalidateAll(false) <br>
     * If Java 2 security is enabled, accessing this method requires
     * com.ibm.websphere.security.WebSphereRuntimePermission with target name
     * "accessInvalidateAll".
     * 
     * @param remote
     *            boolean that indicates if request should be broadcast across core
     *            group
     * @return void
     * @throws java.lang.SecurityException
     * @since WAS 6.1
     * 
     */
    public void invalidateAll() throws java.lang.SecurityException;

}
