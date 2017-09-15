/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.protocol;



import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;

import javax.net.SocketFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ssl.config.SSLConfigManager;

/**
 * <p>
 * This class is for creating SSLSockets. It utilizes the com.ibm.websphere.ssl.JSSEHelper
 * APIs to retrieve WebSphere SSL configuration information. You can pass in
 * a Properties object containing SSL properties, or an SSL alias, or allow it to
 * return the default SSLSocketFactory.
 * </p>
 *
 **/

public class LibertySSLSocketFactory extends javax.net.ssl.SSLSocketFactory {
    private static final TraceComponent tc = Tr.register(LibertySSLSocketFactory.class, "SSL", "com.ibm.ws.ssl.resources.ssl");
    protected java.util.Properties props;
    private javax.net.ssl.SSLSocketFactory default_factory = null;

    protected boolean default_constructor = true;
    private static com.ibm.ws.ssl.protocol.LibertySSLSocketFactory thisClass = null;

    /***
     * <p>
     * This is the default constructor which will retrieve the default SSL configuration
     * for creating Sockets. It first tries to find the javax.net.ssl.* properties
     * and if not present will choose the default SSL configuration.
     * </p>
     ***/
    public LibertySSLSocketFactory() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "LibertySSLSocketFactory");
        try {
            java.util.Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);

            props = SSLConfigManager.getInstance().getDefaultSystemProperties(true);
            if (props == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Getting default SSL properties from WebSphere configuration.");
                props = JSSEHelper.getInstance().getProperties(null, connectionInfo, null);
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Getting javax.net.ssl.* SSL System properties.");
                default_constructor = false;
            }

            if (props != null)
                default_factory = JSSEHelper.getInstance().getSSLSocketFactory(connectionInfo, props);
            else
                default_factory = (SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();

        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "LibertySSLSocketFactory exception getting default SSL properties.", new Object[] { e });
            FFDCFilter.processException(e, getClass().getName(), "LibertySSLSocketFactory", this);
            props = null;
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "LibertySSLSocketFactory");
    }

    /***
     * <p>
     * This constructor allows you to pass in an SSL alias to retrieve an
     * SSL configuration other than the default for creating Sockets.
     * If the alias is not found, the default SSL configuration will be used.
     * </p>
     *
     * @param String alias
     * @throws SSLException
     ***/
    public LibertySSLSocketFactory(String alias) throws SSLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "LibertySSLSocketFactory", new Object[] { alias });
        try {
            default_constructor = false;
            java.util.Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
            props = JSSEHelper.getInstance().getProperties(alias, connectionInfo, null);
            default_factory = JSSEHelper.getInstance().getSSLSocketFactory(connectionInfo, props);
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "LibertySSLSocketFactory exception getting SSL factory from alias.", new Object[] { e });
            props = null;
            throw new javax.net.ssl.SSLException(e.getMessage());
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "LibertySSLSocketFactory");
    }

    /***
     * <p>
     * This constructor allows you to pass in a set of SSL properties used
     * for creating Sockets.
     * </p>
     *
     * @param java.util.Properties sslprops
     * @throws SSLException
     ***/
    public LibertySSLSocketFactory(java.util.Properties sslprops) throws SSLException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "LibertySSLSocketFactory", new Object[] { sslprops });

        try {
            default_constructor = false;
            props = sslprops;
            java.util.Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
            default_factory = JSSEHelper.getInstance().getSSLSocketFactory(connectionInfo, props);
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "LibertySSLSocketFactory exception getting SSL properties from properties.", new Object[] { e });
            props = null;
            throw new javax.net.ssl.SSLException(e.getMessage());
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "LibertySSLSocketFactory");
    }

    /***
     * <p>
     * This constructor allows you to pass in an SSL alias, and/or connectionInfo.
     * depending upon what is available. You may specified null for any parameters.
     * If the connectionInfo is specified, a check will be made to determine if a
     * dynamic association between protocol/host/port has been made for this
     * particular protocol and target host/port. If there is not dynamic
     * association and an SSL alias is specified, a direct selection will occur.
     * Finally, a group selection will occur where the localEndPoint (contained in
     * the connectionInfo) will be used to determine if an SSL config is
     * associated with that endpoint. If not, it will move up the topology to
     * determine the next management scope that has an SSL configuration
     * associated which this EndPoint falls under (e.g., server -> node -> cell,
     * etc.).
     * </p>
     *
     * @param String alias
     * @param java.util.Map connectionInfo
     * @see com.ibm.websphere.ssl.JSSEHelper
     ***/
    public LibertySSLSocketFactory(String alias, java.util.Map<String, Object> connectionInfo) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "LibertySSLSocketFactory", new Object[] { alias, connectionInfo });
        try {
            default_constructor = false;

            props = JSSEHelper.getInstance().getProperties(alias, connectionInfo, null);
            default_factory = JSSEHelper.getInstance().getSSLSocketFactory(connectionInfo, props);
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "LibertySSLSocketFactory exception getting SSL properties from selections.", new Object[] { e });
            FFDCFilter.processException(e, getClass().getName(), "LibertySSLSocketFactory", this, new Object[] { alias, connectionInfo });
            props = null;
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "LibertySSLSocketFactory");
    }

    /***
     * <p>
     * Returns a copy of the environment's default socket factory.
     * </p>
     ***/
    public static javax.net.SocketFactory getDefault() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getDefault");

        if (thisClass == null) {
            try {
                thisClass = new com.ibm.ws.ssl.protocol.LibertySSLSocketFactory();
            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "SSLSocketFactory exception getting default socket factory.", new Object[] { e });
                FFDCFilter.processException(e, "SSLSocketFactory", "getDefault", thisClass);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getDefault");
        return thisClass;
    }

    /***
     * <p>
     * Returns the list of cipher suites which are enabled by default.
     * </p>
     ***/
    @Override
    public String[] getDefaultCipherSuites() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getDefaultCipherSuites");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "default_factory: " + default_factory);
        String[] output = null;
        if (default_factory != null) {
            output = default_factory.getDefaultCipherSuites();
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getDefaultCipherSuites", new Object[] { output });
        return output;
    }

    /***
     * Returns the names of the cipher suites which could be enabled for
     * use on an SSL connection created by this factory.
     ***/
    @Override
    public String[] getSupportedCipherSuites() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getSupportedCipherSuites");
        if (tc.isDebugEnabled())
            Tr.debug(tc, "default_factory: " + default_factory);
        String[] output = null;
        if (default_factory != null) {
            output = default_factory.getSupportedCipherSuites();
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getSupportedCipherSuites", new Object[] { output });
        return output;
    }

    /***
     * <p>
     * Creates an unconnected socket.
     * </p>
     *
     * @return java.net.Socket
     * @throws java.io.IOException
     ***/
    @Override
    public java.net.Socket createSocket() throws IOException {
        // This is the code to support outbound ssl config for an unconnected socket.
        // To make this code work, the caller side needs to set either sslProperties by using setSSLPropertiesOnThread
        // or outbound connection info with host/port information by using setOutboundConnectionInfo.
        // Either case, the thread attached information needs to be cleared after creating a socket.
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createSocket");
        javax.net.ssl.SSLSocketFactory currentFactory = default_factory;
        javax.net.ssl.SSLSocket socket = null;
        java.util.Properties sslProps = props;
        if (default_constructor) {
            try {
                java.util.Properties sslPropsOnThread = JSSEHelper.getInstance().getSSLPropertiesOnThread();
                java.util.Map<String, Object> currentConnectionInfo = JSSEHelper.getInstance().getOutboundConnectionInfo();
                if ((sslPropsOnThread != null) || (currentConnectionInfo != null)) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "either SSLProperties on thread or connectionInfo is available.");
                    if (currentConnectionInfo == null) {
                        currentConnectionInfo = new HashMap<String, Object>();
                        currentConnectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
                    }
                    sslProps = JSSEHelper.getInstance().getProperties(null, currentConnectionInfo, null);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Getting SSLSocketFactory");
                    currentFactory = JSSEHelper.getInstance().getSSLSocketFactory(currentConnectionInfo, sslProps);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Got SSLSocketFactory", new Object[] { currentFactory });
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Neither SSLProperties nor outboundConnectionInfo is set and this is an unconnected socket so create a WSSocket.");
                    java.net.Socket ws_socket = (Socket) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {

                            return (new com.ibm.ws.ssl.config.WSSocket(new java.net.Socket()));
                        }
                    });
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "createSocket");
                    return ws_socket;
                }

            } catch (Exception e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Exception getting SSLSocketFactory. Fall back to the default SSLSocketFactory", new Object[] { e });
                FFDCFilter.processException(e, getClass().getName(), "createSocket", this);
                currentFactory = default_factory;
            }
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Use default SSLSocketFactory - the default constructor was not used");
        }
        if (currentFactory != null) {
            socket = (javax.net.ssl.SSLSocket) currentFactory.createSocket();
            socket = SSLConfigManager.getInstance().setCipherListOnSocket(sslProps, socket);
        } else
            throw new javax.net.ssl.SSLException("SSLSocketFactory is null. This can occur if javax.net.ssl.SSLSocketFactory.getDefault() is called to create a socket and javax.net.ssl.* properties are not set.");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createSocket");
        return socket;
    }

    /***
     * <p>
     * Returns a socket layered over an existing socket connected to the
     * named host, at the given port. The properties used to create the
     * SSL socket are based upon the what was passed into the SSLSocketFactory
     * constructor.
     * </p>
     *
     * @param java.net.Socket s - existing socket
     * @param String host - target host
     * @param int port - target port
     * @param boolean autoClose - close the underlying socket when this socket is closed
     * @return java.net.Socket
     * @throws java.io.IOException
     ***/
    @Override
    public java.net.Socket createSocket(java.net.Socket s, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createSocket", new Object[] { s, host, Integer.valueOf(port), Boolean.valueOf(autoClose) });
        javax.net.ssl.SSLSocketFactory factory = default_factory;
        javax.net.ssl.SSLSocket socket = null;

        // use the props set from the constructor
        java.util.Properties sslprops = props;

        try {
            java.util.Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, Integer.toString(port));

            if (default_constructor) {
                sslprops = JSSEHelper.getInstance().getProperties(null, connectionInfo, null);
            }

            if (sslprops != null)
                factory = JSSEHelper.getInstance().getSSLSocketFactory(connectionInfo, sslprops);
        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception getting SSLSocketFactory.", new Object[] { e });
            FFDCFilter.processException(e, getClass().getName(), "checkClientTrusted", this, new Object[] { socket, host, port });
        }

        if (factory != null) {
            socket = (javax.net.ssl.SSLSocket) factory.createSocket(s, host, port, autoClose);
            socket = SSLConfigManager.getInstance().setCipherListOnSocket(sslprops, socket);
        } else
            throw new javax.net.ssl.SSLException("SSLSocketFactory is null. This can occur if javax.net.ssl.SSLSocketFactory.getDefault() is called to create a socket and javax.net.ssl.* properties are not set.");
        if (tc.isEntryEnabled())
            Tr.exit(tc, "createSocket");
        return socket;
    }

    /***
     * <p>
     * Creates a socket and connects it to the specified port number at the
     * specified address. The properties used to create the SSL socket are
     * based upon the what was passed into the SSLSocketFactory constructor.
     * </p>
     *
     * @param InetAddress host - target host
     * @param int port - target port
     * @return java.net.Socket
     * @throws java.io.IOException
     ***/
    @Override
    public java.net.Socket createSocket(InetAddress host, int port) throws IOException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createSocket", new Object[] { host, Integer.valueOf(port) });
        javax.net.ssl.SSLSocketFactory factory = default_factory;
        javax.net.ssl.SSLSocket socket = null;

        // use the props set from the constructor
        java.util.Properties sslprops = props;

        try {
            String remoteHostName = null;
            final InetAddress host_final = host;
            final Integer port_final = Integer.valueOf(port);
            if (host_final != null) {
                remoteHostName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String remoteHost = null;
                        try {
                            remoteHost = host_final.getHostName();

                            if (remoteHost == null)
                                remoteHost = host_final.getCanonicalHostName();
                            return remoteHost;
                        } catch (Throwable e) {
                            remoteHost = host_final.getHostAddress();
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Exception getting hostname from socket.", new Object[] { e });
                            FFDCFilter.processException(e, getClass().getName(), "checkClientTrusted", this, new Object[] { host_final, port_final });
                            return remoteHost;
                        }
                    }
                });
            }

            java.util.Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, remoteHostName);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, Integer.toString(port));

            if (default_constructor) {
                sslprops = JSSEHelper.getInstance().getProperties(null, connectionInfo, null);
            }

            if (sslprops != null)
                factory = JSSEHelper.getInstance().getSSLSocketFactory(connectionInfo, sslprops);

        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception getting SSLSocketFactory.", new Object[] { e });
            FFDCFilter.processException(e, getClass().getName(), "createSocket", this, new Object[] { host, port });
        }

        if (factory != null) {
            socket = (javax.net.ssl.SSLSocket) factory.createSocket(host, port);
            socket = SSLConfigManager.getInstance().setCipherListOnSocket(sslprops, socket);
        } else
            throw new javax.net.ssl.SSLException("SSLSocketFactory is null. This can occur if javax.net.ssl.SSLSocketFactory.getDefault() is called to create a socket and javax.net.ssl.* properties are not set.");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createSocket");
        return socket;
    }

    /***
     * <p>
     * Creates a socket and connects it to the specified remote host on the
     * specified remote port. The socket will also be bound to the local
     * address and port supplied. This socket is configured using the socket
     * options established for this factory. The properties used to create
     * the SSL socket are based upon the what was passed into the
     * SSLSocketFactory constructor.
     * </p>
     *
     * @param InetAddress host - target host
     * @param int port - target port
     * @param InetAddress localAddress - local host
     * @param int localPort - local port
     * @return java.net.Socket
     * @throws java.io.IOException
     ***/
    @Override
    public java.net.Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createSocket", new Object[] { address, Integer.valueOf(port), localAddress, Integer.valueOf(localPort) });
        javax.net.ssl.SSLSocketFactory factory = default_factory;
        javax.net.ssl.SSLSocket socket = null;

        // use the props set from the constructor
        java.util.Properties sslprops = props;

        try {
            String remoteHostName = null;
            final InetAddress address_final = address;
            final Integer port_final = Integer.valueOf(port);
            final InetAddress localAddress_final = localAddress;
            final Integer localPort_final = Integer.valueOf(localPort);
            if (address != null) {
                remoteHostName = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        String remoteHost = null;
                        try {
                            remoteHost = address_final.getHostName();

                            if (remoteHost == null)
                                remoteHost = address_final.getCanonicalHostName();
                            return remoteHost;
                        } catch (Throwable e) {
                            remoteHost = address_final.getHostAddress();
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Exception getting hostname from socket.", new Object[] { e });
                            FFDCFilter.processException(e, getClass().getName(), "createSocket", this, new Object[] { address_final, port_final, localAddress_final,
                                                                                                                      localPort_final });
                            return remoteHost;
                        }
                    }
                });
            }

            HashMap<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, remoteHostName);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, Integer.toString(port));

            if (default_constructor) {
                sslprops = JSSEHelper.getInstance().getProperties(null, connectionInfo, null);
            }

            if (sslprops != null)
                factory = JSSEHelper.getInstance().getSSLSocketFactory(connectionInfo, sslprops);

        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception getting SSLSocketFactory.", new Object[] { e });
            FFDCFilter.processException(e, getClass().getName(), "checkClientTrusted", this, new Object[] { address, port, localAddress, localPort });
        }

        if (factory != null) {
            socket = (javax.net.ssl.SSLSocket) factory.createSocket(address, port, localAddress, localPort);
            socket = SSLConfigManager.getInstance().setCipherListOnSocket(sslprops, socket);
        } else
            throw new javax.net.ssl.SSLException("SSLSocketFactory is null. This can occur if javax.net.ssl.SSLSocketFactory.getDefault() is called to create a socket and javax.net.ssl.* properties are not set.");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createSocket");
        return socket;
    }

    /***
     * <p>
     * Creates a socket and connects it to the specified remote host at the
     * specified remote port. This socket is configured using the socket
     * options established for this factory. The properties used to create
     * the SSL socket are based upon the what was passed into the
     * SSLSocketFactory constructor.
     * </p>
     *
     * @param InetAddress host - target host
     * @param int port - target port
     * @param InetAddress localAddress - local host
     * @param int localPort - local port
     * @return java.net.Socket
     * @throws java.io.IOException
     ***/
    @Override
    public java.net.Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createSocket", new Object[] { host, Integer.valueOf(port) });
        javax.net.ssl.SSLSocketFactory factory = default_factory;
        javax.net.ssl.SSLSocket socket = null;

        // use the props set from the constructor
        java.util.Properties sslprops = props;

        try {
            java.util.Map<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, Integer.toString(port));

            if (default_constructor) {
                sslprops = JSSEHelper.getInstance().getProperties(null, connectionInfo, null);
            }

            if (sslprops != null)
                factory = JSSEHelper.getInstance().getSSLSocketFactory(connectionInfo, sslprops);

        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception getting SSLSocketFactory.", new Object[] { e });
            FFDCFilter.processException(e, getClass().getName(), "createSocket", this, new Object[] { host, port });
        }

        if (factory != null) {
            socket = (javax.net.ssl.SSLSocket) factory.createSocket(host, port);
            socket = SSLConfigManager.getInstance().setCipherListOnSocket(sslprops, socket);
        } else
            throw new javax.net.ssl.SSLException("SSLSocketFactory is null. This can occur if javax.net.ssl.SSLSocketFactory.getDefault() is called to create a socket and javax.net.ssl.* properties are not set.");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createSocket");
        return socket;
    }

    /***
     * <p>
     * Creates a socket and connects it to the specified remote host on the
     * specified remote port. The socket will also be bound to the local
     * address and port supplied. This socket is configured using the socket
     * options established for this factory. The properties used to create
     * the SSL socket are based upon the what was passed into the
     * SSLSocketFactory constructor.
     * </p>
     *
     * @param String host - target host
     * @param int port - target port
     * @param InetAddress localAddress - local host
     * @param int localPort - local port
     * @return java.net.Socket
     * @throws java.io.IOException
     ***/
    @Override
    public java.net.Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createSocket", new Object[] { host, Integer.valueOf(port), localHost, Integer.valueOf(localPort) });
        javax.net.ssl.SSLSocketFactory factory = default_factory;
        javax.net.ssl.SSLSocket socket = null;

        // use the props set from the constructor
        java.util.Properties sslprops = props;

        try {
            HashMap<String, Object> connectionInfo = new HashMap<String, Object>();
            connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_HOST, host);
            connectionInfo.put(Constants.CONNECTION_INFO_REMOTE_PORT, Integer.toString(port));

            if (default_constructor) {
                sslprops = JSSEHelper.getInstance().getProperties(null, connectionInfo, null);
            }

            if (sslprops != null)
                factory = JSSEHelper.getInstance().getSSLSocketFactory(connectionInfo, sslprops);

        } catch (Exception e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception getting SSLSocketFactory.", new Object[] { e });
            FFDCFilter.processException(e, getClass().getName(), "createSocket", this, new Object[] { host, port, localHost, localPort });
        }

        if (factory != null) {
            socket = (javax.net.ssl.SSLSocket) factory.createSocket(host, port, localHost, localPort);
            socket = SSLConfigManager.getInstance().setCipherListOnSocket(sslprops, socket);
        } else
            throw new javax.net.ssl.SSLException("SSLSocketFactory is null. This can occur if javax.net.ssl.SSLSocketFactory.getDefault() is called to create a socket and javax.net.ssl.* properties are not set.");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createSocket");
        return socket;
    }

    public int compare(SocketFactory sf1, SocketFactory sf2) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "compare", new Object[] { sf1, sf2 });

        if (sf1 != null && sf2 != null) {
            int sf1hash = sf1.hashCode();
            int sf2hash = sf2.hashCode();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "compare is invoked: \nsf1hash : " + sf1hash + " \nsf2hash : " + sf2hash);
            int output = sf1hash - sf2hash;
            if (tc.isEntryEnabled())
                Tr.exit(tc, ("compare : " + output));
            return output;
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "one of parameters is null, throwing NullPointerException.");
            throw new java.lang.NullPointerException();
        }
    }
}
