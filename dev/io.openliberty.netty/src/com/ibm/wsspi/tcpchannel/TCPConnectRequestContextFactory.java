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

import java.net.InetSocketAddress;

/**
 * This is a factory for the creation of the connection request objects.
 */
public class TCPConnectRequestContextFactory {

    // Reference to sinlge instance of this class
    private static TCPConnectRequestContextFactory instanceRef = null;

    /**
     * Create a new connection request context based upon the input needed to
     * fully define
     * the context.
     * 
     * @param _localHostName
     *            host name of the local side of the connection. null is valid.
     * @param _localPort
     *            port to be used by the local side of the connection
     * @param _remoteHostName
     *            host name of the remote side of the connection
     * @param _remotePort
     *            port to be used by the remote side of the connection
     * @param _timeout
     *            timeout for waiting for the connection to complete
     * @return a connect request context to be used by the channel connection
     */
    public TCPConnectRequestContext createTCPConnectRequestContext(String _localHostName, int _localPort, String _remoteHostName, int _remotePort, int _timeout) {

        return new TCPConnectRequestContextImpl(_localHostName, _localPort, _remoteHostName, _remotePort, _timeout);
    }

    /**
     * Create a new connection request context based upon the input needed to
     * fully define
     * the context. The local address is assumed to be null, and the local port
     * will be an ephemeral port
     * 
     * @param _remoteHostName
     *            host name of the remote side of the connection
     * @param _remotePort
     *            port to be used by the remote side of the connection
     * @param _timeout
     *            timeout for waiting for the connection to complete
     * @return a connect request context to be used by the channel connection
     */
    public TCPConnectRequestContext createTCPConnectRequestContext(String _remoteHostName, int _remotePort, int _timeout) {

        return new TCPConnectRequestContextImpl(_remoteHostName, _remotePort, _timeout);
    }

    /**
     * Create the singleton instance of the class here
     * 
     */
    static private synchronized void createSingleton() {
        if (null == instanceRef) {
            instanceRef = new TCPConnectRequestContextFactory();
        }
    }

    /**
     * This class implements the singleton pattern. This method is provided
     * to return a reference to the single instance of this class in existence.
     * 
     * @return TCPConnectionRequestContextFactory
     */
    public static TCPConnectRequestContextFactory getRef() {
        if (instanceRef == null) {
            createSingleton();
        }

        return instanceRef;
    }

    /**
     * Implementataion of the connection request context
     */
    public static class TCPConnectRequestContextImpl implements TCPConnectRequestContext {

        private InetSocketAddress localAddress = null;
        private InetSocketAddress remoteAddress = null;
        private int timeout;

        /**
         * Construct a new connection request context based upon the input needed to
         * fully define
         * the context.
         * 
         * @param _localHostName
         *            host name of the local side of the connection. null is valid.
         * @param _localPort
         *            port to be used by the local side of the connection
         * @param _remoteHostName
         *            host name of the remote side of the connection
         * @param _remotePort
         *            port to be used by the remote side of the connection
         * @param _timeout
         *            timeout for waiting for the connection to complete
         */
        public TCPConnectRequestContextImpl(String _localHostName, int _localPort, String _remoteHostName, int _remotePort, int _timeout) {

            timeout = _timeout;

            if (_localHostName != null) {
                localAddress = new InetSocketAddress(_localHostName, _localPort);
            } else {
                localAddress = new InetSocketAddress(_localPort);
            }

            if (_remoteHostName != null) {
                remoteAddress = new InetSocketAddress(_remoteHostName, _remotePort);
            } else {
                remoteAddress = new InetSocketAddress(_remotePort);
            }
        }

        /**
         * Construct a new connection request context based upon the input needed to
         * fully define
         * the context. The local address is assumed to be null, and the local port
         * will be an ephemeral port
         * 
         * @param _remoteHostName
         *            host name of the remote side of the connection
         * @param _remotePort
         *            port to be used by the remote side of the connection
         * @param _timeout
         *            timeout for waiting for the connection to complete
         */
        public TCPConnectRequestContextImpl(String _remoteHostName, int _remotePort, int _timeout) {

            timeout = _timeout;

            if (_remoteHostName != null) {
                remoteAddress = new InetSocketAddress(_remoteHostName, _remotePort);
            } else {
                remoteAddress = new InetSocketAddress(_remotePort);
            }
        }

        /** @see com.ibm.wsspi.tcpchannel.TCPConnectRequestContext#getLocalAddress() */
        public InetSocketAddress getLocalAddress() {
            return localAddress;
        }

        /** @see com.ibm.wsspi.tcpchannel.TCPConnectRequestContext#getRemoteAddress() */
        public InetSocketAddress getRemoteAddress() {
            return remoteAddress;
        }

        /** @see com.ibm.wsspi.tcpchannel.TCPConnectRequestContext#getConnectTimeout() */
        public int getConnectTimeout() {
            return timeout;
        }
    }
}
