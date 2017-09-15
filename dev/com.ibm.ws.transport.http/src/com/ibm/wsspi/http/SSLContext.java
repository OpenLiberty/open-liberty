/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.http;

import javax.net.ssl.SSLSession;

/**
 * SSL information available for an HTTP connection.
 */
public interface SSLContext {

    /**
     * get the list of enabled cipher suites
     * 
     * @return String[]
     */
    String[] getEnabledCipherSuites();

    /**
     * get a list of the enabled protocols.
     * 
     * @return String[]
     */
    String[] getEnabledProtocols();

    /**
     * configured to require client authentication
     * 
     * @return boolean
     */
    boolean getNeedClientAuth();

    /**
     * get the SSLSession that is associated with this session.
     * 
     * @return javax.net.ssl.SSLSession
     */
    SSLSession getSession();

    /**
     * returns true if the socket requires client mode in its first handshake.
     * 
     * @return boolean
     */
    boolean getUseClientMode();

    /**
     * whether the socket would like the client to authenticate
     * 
     * @return boolean
     */
    boolean getWantClientAuth();

}
