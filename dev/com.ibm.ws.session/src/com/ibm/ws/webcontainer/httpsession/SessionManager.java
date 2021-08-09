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

package com.ibm.ws.webcontainer.httpsession;

import com.ibm.ws.session.SessionContextRegistry;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.session.SessionStoreService;

/**
 * @author asisin
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface SessionManager {

    void start(SessionContextRegistry scr);

    /**
     * The unique identifier for this server among a group of other servers.
     * 
     * @return a unique server identifier
     */
    public String getCloneID();

    /**
     * Gets the character used to delimit clone IDs in session cookies.
     * Usually either ':' or '+' will be returned.
     * 
     * @return the character used to delimit clone IDs in session cookies
     */
    public char getCloneSeparator();

    /**
     * Gets the value of the "rewriteId" property, used for URL rewriting.
     * 
     * @return the rewriteId, such as jsessionid
     */
    public String getAffinityUrlIdentifier();

    /**
     * Gets the default name of session cookies. Note that this value can be
     * overridden by individual applications at run time. Whenever possible,
     * the exact cookie name should be read from the ServletContext instead
     * of this value.
     * 
     * @return the default name of session cookies
     */
    public String getDefaultAffinityCookie();

    /**
     * Retrieve the session manager configuration information at the web container/server level.
     * 
     * @return session manager configuration information at the web container/server level
     */
    public SessionManagerConfig getServerConfig();

    /**
     * Retrieves the store service associated with this instance.
     * 
     * @return the store service, or null if the service is unavailable or invalid
     */
    public SessionStoreService getSessionStoreService();
    
}
