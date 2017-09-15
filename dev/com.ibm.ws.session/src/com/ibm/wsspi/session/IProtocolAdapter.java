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

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

/**
 * Interface that lists methods that may be called to adapt a session to one
 * of two protocols HTTP and SIP. An implementation for adaptation to HTTP
 * is currently available.
 */
public interface IProtocolAdapter {

    // XD methods
    /**
     * Constant for WAS extensions (IBMSession, IBMSessionExt) to HttpSession
     * that allow for IBMApplicationSession
     */
    public final static Integer HTTP_EXT = new Integer(3);

    /**
     * Returns an adapted version of a session, adapted to a certain
     * protocol. This will be a javax.servlet.http.HttpSession object for HTTP.
     * 
     * @return Object adapted version of the ISession.
     */
    public Object adapt(ISession session, Integer protocol);

    /**
     * Returns an adapted version of a session, adapter to a certain protocol.
     * This method was added to help deal with converged, multi-protocol sessions.
     * 
     * @param session
     * @param protocol
     * @param request
     * @param context
     * @return
     */
    public Object adapt(ISession session, Integer protocol, ServletRequest request, ServletContext context);

    /**
     * This method allows for the returning of a correlator ID to be used during
     * multi-protocol scenarios or partition/hot failover based routing scenarios
     * 
     */
    public Object getCorrelator(ServletRequest request, Object session);

    public Integer getProtocol(Object session);

    // end of XD methods

    // ----------------------------------------
    // Public Members
    // ----------------------------------------
    /**
     * HTTP Protocol Constant, used to adaptation to HTTP.
     */
    public final static Integer HTTP = new Integer(1);

    /**
     * SIP Protocol Constant, used for adaptation to SIP.
     */
    public final static Integer SIP = new Integer(2);

    // ----------------------------------------
    // Public Methods
    // ----------------------------------------

    /**
     * Returns an adapted version of a session, adapted to the protocol of the
     * implementing class.
     * This will be a javax.servlet.http.HttpSession object for HTTP.
     * 
     * @return Object adapted version of the ISession.
     */
    public Object adapt(ISession session);

    public Object adaptToAppSession(ISession session);

}