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

import java.util.Collection;
import java.util.Iterator;

/**
 * The IBMApplicationSession interface is used for SIP/HTTP Converged
 * Applications
 * as well as for sharing session data in http applications.
 * HTTP Servlets can call session.getIBMApplicationSession, whereas SIP Servlets
 * call session.getApplicationSession. In a converged application, both of these
 * calls will return the same SipApplicationSession object.
 * 
 * @ibm-api
 */

public interface IBMApplicationSession {

    /**
     * Encodes the ID of this IBMApplicationSession into the specified SIP URI.
     * 
     * @param URI
     *            object expected to be an instance of javax.servlet.sip.URI
     */
    public void encodeURI(Object URI);

    /**
     * Encodes the ID of this IBMApplicationSession into the specified HTTP URI.
     * 
     * @param URI
     *            string representation of the http uri
     * @return string with encoded application session id added
     */
    public String encodeURI(String URI);

    /**
     * Returns the object bound with the specified name in this session, or null
     * if no object is bound under the name.
     * 
     * @param name
     *            name of the attribute to retrieve
     */
    public Object getAttribute(String name);

    /**
     * Returns an Iterator over the String objects containing the names of all the
     * objects bound to this session.
     */
    public Iterator getAttributeNames();

    /**
     * Returns the time when this session was created, measured in milliseconds
     * since midnight January 1, 1970 GMT.
     */
    public long getCreationTime();

    /**
     * Returns a string containing the unique identifier assigned to this session.
     */
    public String getId();

    /**
     * Returns the last time an event occurred on this application session.
     */
    public long getLastAccessedTime();

    /**
     * Returns an Iterator over all "protocol" sessions associated with this
     * application session.
     */
    public Iterator getSessions();

    /**
     * Returns an Iterator over the "protocol" session objects associated of the
     * specified protocol
     * associated with this application session.
     * 
     * @param protocol
     *            string representation of protocol, either "SIP" or "HTTP"
     */
    public Iterator getSessions(String protocol);

    /**
     * Returns all active timers associated with this application session.
     */
    public Collection getTimers();

    /**
     * Invalidates this application session.
     */
    public void invalidate();

    /**
     * Removes the object bound with the specified name from this session.
     * 
     * @param name
     *            name of the attribute to remove
     */
    public void removeAttribute(String name);

    /**
     * Binds an object to this session, using the name specified.
     * 
     * @param name
     *            name of the attribute to set
     * @param attribute
     *            value of the attribute to set
     */
    public void setAttribute(String name, Object attribute);

    /**
     * Sets the time of expiry for this application session.
     * 
     * @param deltaMinutes
     *            the number of minutes that the lifetime of this
     *            IBMApplicationSession is extended
     * @return actual number of minutes the lifetime of this session is extended,
     *         or 0 if it wasn't extended
     */
    public int setExpires(int deltaMinutes);

    /**
     * Forces replication of application session changes
     */
    public void sync();


}
