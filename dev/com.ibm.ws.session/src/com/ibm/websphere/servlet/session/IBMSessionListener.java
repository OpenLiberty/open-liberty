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

import javax.servlet.http.HttpSessionListener;

/**
 * The IBMSessionListener interface extends the
 * javax.servlet.http.HttpSessionListener
 * interface of the Servlet API to notify an application that its session has
 * been
 * removed from the server's cache. This could be because:
 * <ul>
 * <li>the session timed out
 * <li>the session was programatically invalidated
 * <li>the session cache is full and this is the least-recently-used session
 * (distributed environment only)
 * </ul>
 * <p>
 * A session will eventually time out on every server that accesses the session,
 * and therefore sessionRemovedFromCache() will be called on all of these
 * servers. sessionDestroyed() is only called during session invalidation, which
 * only happens on one server. Further, the server that invalidates and calls
 * sessionDestroyed() may or may not be the same server that created a session
 * and called sessionCreated().
 * </p>
 * 
 * @ibm-api
 */
public interface IBMSessionListener extends HttpSessionListener {

    public void sessionRemovedFromCache(String sessionId);

}
