/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.tcpchannel;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

/**
 * This interface describes the SSL context for a connection.
 * It is made available via the TCPConnectionContext interface.
 *
 * @ibm-spi
 *
 */
public interface SSLConnectionContext {

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
     * set the list of enabled protocols
     *
     * @param args
     */
    void setEnabledProtocols(String args[]);

    /**
     * This returns true if new sessions may be established on this connection.
     *
     * @return boolean
     */
    boolean getEnableSessionCreation();

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

    /**
     * set the suites available for this socket. Throws an exception if the socket
     * is already established.
     *
     * @param suites
     * @throws SSLException
     */
    void setEnabledCipherSuites(String[] suites) throws SSLException;

    /**
     * sets whether new sessions may be created by this socket. Throws an
     * exception if the socket is already established.
     *
     * @param flag
     * @throws SSLException
     */
    void setEnableSessionCreation(boolean flag) throws SSLException;

    /**
     * sets the requirement for client authentication. Throws an exception if the
     * socket is already established.
     *
     * @param flag
     * @throws SSLException
     */
    void setNeedClientAuth(boolean flag) throws SSLException;

    /**
     * set the preference to have client authentication. Throws an exception if
     * the socket is already established.
     *
     * @param flag
     * @throws SSLException
     */
    void setWantClientAuth(boolean flag) throws SSLException;

    /**
     * Configures the connection to use client (or server) mode when handshaking.
     * This method must be called before any handshaking occurs.
     * Once handshaking has begun, the mode can not be reset for the
     * life of this connection. Servers normally authenticate themselves,
     * and clients are not required to do so.
     *
     * @param flag
     * @throws SSLException
     */
    void setUseClientMode(boolean flag) throws SSLException;

    /**
     * Used after changing parameters via other methods in this interface. Those
     * settings won't take hold until this method is called which kicks of a
     * new SSL handshake with the other end of the connection.
     */
    void renegotiate();

    /**
     * the name of the ALPN protocol selected for this connection
     * 
     * @return String
     */
    String getAlpnProtocol();

    /**
     * Set the ALPN protocol chosen for this connection
     * 
     * @param protocol
     */
    void setAlpnProtocol(String protocol);

}
