/*******************************************************************************
 * Copyright (c) 2015, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config.ssl.yoko;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.yoko.orb.OCI.IIOP.Util;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.NoProtection;
import org.omg.CSIIOP.TAG_CSI_SEC_MECH_LIST;
import org.omg.CSIIOP.TransportAddress;
import org.omg.CORBA.Policy;
import org.omg.CORBA.TRANSIENT;
import org.omg.IOP.TaggedComponent;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.csiv2.config.CompatibleMechanisms;
import com.ibm.ws.security.csiv2.config.ssl.SSLConfig;
import com.ibm.ws.security.csiv2.config.tss.ServerTransportAddress;
import com.ibm.ws.security.csiv2.util.SecurityServices;
import com.ibm.ws.transport.iiop.security.ClientPolicy;
import com.ibm.ws.transport.iiop.security.config.css.CSSConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSTransportMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.OptionsKey;
import com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechListConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSSSLTransportConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSTransportMechConfig;
import com.ibm.ws.transport.iiop.yoko.helper.SocketFactoryHelper;

/**
 * Socket factory instance used to interface openejb2
 * with the Yoko ORB. Also enables the ORB for
 * SSL-type connections.
 *
 * @version $Revision: 505035 $ $Date: 2007-02-08 16:01:06 -0500 (Thu, 08 Feb 2007) $
 */
public class SocketFactory extends SocketFactoryHelper {
    private static final TraceComponent tc = Tr.register(SocketFactory.class);
    private static final String HOST_PROTOCOL = "ssl";

    private final Map<String, SSLSocketFactory> socketFactoryMap = new HashMap<String, SSLSocketFactory>(1);
    private final Map<String, SSLServerSocketFactory> serverSocketFactoryMap = new HashMap<String, SSLServerSocketFactory>(1);
    // The initialized SSLConfig we use to retrieve the SSL socket factories.
    private final SSLConfig sslConfig;

    private static final class SocketInfo {
        final InetAddress addr;
        final int port;
        final OptionsKey key;
        final String sslConfigName;

        /**
         * @param addr
         * @param port
         * @param key
         * @param sslConfigName
         */
        public SocketInfo(InetAddress addr, int port, OptionsKey key, String sslConfigName) {
            super();
            this.addr = addr;
            this.port = port;
            this.key = key;
            this.sslConfigName = sslConfigName;
        }

    }

    //Liberty TODO remove socketInfos when server sockets are closed???
    private final List<SocketInfo> socketInfos = new ArrayList<SocketInfo>();

    public SocketFactory() {
        super(tc);
        sslConfig = SecurityServices.getSSLConfig();
    }

    /**
     * Create a client socket of the appropriate
     * type using the provided address and port information.
     *
     * @return A Socket (either plain or SSL) configured for connection
     *         to the target.
     */
    @Override
    public Socket createSocket(String host, int port) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "SocketFactory attempting to create socket for host: " + host + " port: " + port);

        // check for SSL addresses
        if (Util.isEncodedHost(host, HOST_PROTOCOL)) {
            String sslConfigName = Util.decodeHostInfo(host);
            host = Util.decodeHost(host);
            return createSSLSocket(host, (char) port, sslConfigName);
        } else {
            return createPlainSocket(host, port);
        }
    }

    private static CSSConfig getCssConfig(Policy[] policies) {
        CSSConfig cssConfig = null;
        for (Policy policy : policies) {
            if (policy instanceof ClientPolicy) {
                cssConfig = ((ClientPolicy) policy).getConfig();
                break;
            }
        }
        return cssConfig;
    }

    private List<CompatibleMechanisms> getCompatibleMechanisms(CSSConfig cssConfig, TaggedComponent comp) {
        // decode and pull the transport information.
        TSSCompoundSecMechListConfig config;
        try {
            config = TSSCompoundSecMechListConfig.decodeIOR(codec, comp);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "looking at tss: " + config);
            return cssConfig.findCompatibleList(config);
        } catch (Exception e) {
            // TODO: should we have Liberty-specific minor codes?
            throw (TRANSIENT) new TRANSIENT("Could not decode IOR TSSCompoundSecMechListConfig").initCause(e);
        }
    }

    /**
     * Create a loopback connection to the hosting
     * ORB.
     *
     * @param address The address information for the server.
     * @param port The target port.
     *
     * @return An appropriately configured socket based on the
     *         listener characteristics.
     * @exception IOException
     * @exception ConnectException
     */
    @Override
    @FFDCIgnore(IOException.class)
    public Socket createSelfConnection(InetAddress address, int port) throws IOException {
        try {
            SocketInfo info = null;
            for (SocketInfo test : socketInfos) {
                if (test.port == port && test.addr.equals(address)) {
                    info = test;
                }
            }
            if (info == null) {
                throw new IOException("No inbound socket matching address " + address + " and port " + port);
            }
            OptionsKey key = info.key;
            // the requires information tells us whether we created a plain or SSL listener.  We need to create one
            // of the matching type.

            if ((NoProtection.value & key.requires) == NoProtection.value) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.debug(tc, "Created plain endpoint to " + address.getHostName() + ":" + port);
                return new Socket(address, port);
            } else {
                return createSSLSocket(address.getHostName(), port, info.sslConfigName);
            }
        } catch (IOException ex) {
            Tr.error(tc, "Exception creating a client socket to " + address.getHostName() + ":" + port, ex);
            throw ex;
        }
    }

    /**
     * Create a server socket listening on the given port.
     *
     * @param port The target listening port.
     * @param backlog The desired backlog value.
     *
     * @return An appropriate server socket for this connection.
     * @exception IOException
     * @exception ConnectException
     */
    @Override
    public ServerSocket createServerSocket(int port, int backlog, String[] params) throws IOException {
        return createServerSocket(port, backlog, null, params);
    }

    /**
     * Create a server socket for this connection.
     *
     * @param port The target listener port.
     * @param backlog The requested backlog value for the connection.
     * @param address The host address information we're publishing under.
     *
     * @return An appropriately configured ServerSocket for this
     *         connection.
     * @exception IOException
     * @exception ConnectException
     */
    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress address, String[] params) throws IOException {
        try {
            ServerSocket socket;
            String sslConfigName = null;
            boolean soReuseAddr = true;
            for (int i = 0; i < params.length - 1; i++) {
                String param = params[i];
                if ("--sslConfigName".equals(param)) {
                    sslConfigName = params[++i];
                }
                if ("--soReuseAddr".equals(param)) {
                    soReuseAddr = Boolean.parseBoolean(params[++i]);
                }
            }
            OptionsKey options = sslConfig.getAssociationOptions(sslConfigName);
            // if no protection is required, just create a plain socket.
            if ((NoProtection.value & options.requires) == NoProtection.value) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.debug(tc, "Created plain server socket for port " + port);
                socket = new ServerSocket();
            } else {
                // SSL is required.  Create one from the SSLServerFactory retrieved from the config.  This will
                // require additional QOS configuration after creation.
                SSLServerSocketFactory serverSocketFactory = getServerSocketFactory(sslConfigName);
                SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket();
                configureServerSocket(serverSocket, serverSocketFactory, sslConfigName, options);
                socket = serverSocket;
            }
            // there is a situation that yoko closes and opens a server socket quickly upon updating
            // the configuration, and occasionally, the openSocket is invoked while closeSocket is processing.
            // To avoid the issue, try binding the socket a few times. Since this is the error scenario,
            // it is less impact for the performance.
            IOException bindError = null;
            for (int i = 0; i < 3; i++) {
                bindError = openSocket(port, backlog, address, socket, soReuseAddr);
                if (bindError == null) {
                    break;
                }
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.debug(tc, "bind error, retry binding... count : " + i);
                    Thread.sleep(500L);
                } catch (Exception e) {
                    Tr.debug(tc, "An exception is caught while retrying binding. the error message is  " + e.getMessage());
                }
            }
            if (bindError == null) {
                // listen port can be different than config port if configed port is '0'
                int listenPort = socket.getLocalPort();
                SocketInfo info = new SocketInfo(address, listenPort, options, sslConfigName);
                socketInfos.add(info);
            } else {
                Tr.error(tc, "SOCKET_BIND_ERROR", address.getHostName(), port, bindError.getLocalizedMessage());
                throw bindError;
            }

            return socket;
        } catch (SSLException e) {
            throw new IOException("Could not retrieve association options from ssl configuration", e);
        }
    }

    /**
     * On-demand creation of an SSL socket factory for the ssl alias provided
     *
     * @return The SSLSocketFactory this connection should be using to create
     *         secure connections.
     * @throws java.io.IOException if we can't get a socket factory
     */
    private SSLSocketFactory getSocketFactory(String id) throws IOException {
        // first use?
        SSLSocketFactory socketFactory = socketFactoryMap.get(id);
        if (socketFactory == null) {
            // the SSLConfig is optional, so if it's not there, use the default SSLSocketFactory.
            if (id == null) {
                socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            } else {
                // ask the SSLConfig bean to create a factory for us.
                try {
                    socketFactory = sslConfig.createSSLFactory(id);
                } catch (Exception e) {
                    Tr.error(tc, "Unable to create client SSL socket factory", e);
                    throw (IOException) new IOException("Unable to create client SSL socket factory: " + e.getMessage()).initCause(e);
                }
            }
            socketFactoryMap.put(id, socketFactory);
        }
        return socketFactory;
    }

    /**
     * On-demand creation of an SSL server socket factory for an ssl alias
     *
     * @return The SSLServerSocketFactory this connection should be using to create
     *         secure connections.
     * @throws java.io.IOException if we can't get a server socket factory
     */
    private SSLServerSocketFactory getServerSocketFactory(String id) throws IOException {
        // first use?
        SSLServerSocketFactory serverSocketFactory = serverSocketFactoryMap.get(id);
        if (serverSocketFactory == null) {
            // the SSLConfig is optional, so if it's not there, use the default SSLSocketFactory.
            if (id == null) {
                serverSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            } else {
                try {
                    serverSocketFactory = sslConfig.createSSLServerFactory(id);
                } catch (Exception e) {
                    Tr.error(tc, "Unable to create server SSL socket factory", e);
                    throw (IOException) new IOException("Unable to create server SSL socket factory: " + e.getMessage()).initCause(e);
                }
                serverSocketFactoryMap.put(id, serverSocketFactory);
            }
            // There's a bit of a timing problem with server-side ORBs.  Part of the ORB shutdown is to
            // establish a self-connection to shutdown the acceptor threads.  This requires a client
            // SSL socket factory.  Unfortunately, if this is occurring during server shutdown, the
            // FileKeystoreManager will get a NullPointerException because some name queries fail because
            // things are getting shutdown.  Therefore, if we need the server factory, assume we'll also
            // need the client factory to shutdown, and request it now.
            getSocketFactory(id);
        }
        return serverSocketFactory;
    }

    /**
     * Set the server socket configuration to our required
     * QOS values.
     *
     * A small experiment shows that setting either (want, need) parameter to either true or false sets the
     * other parameter to false.
     *
     * @param serverSocket
     *            The newly created SSLServerSocket.
     * @param sslConfigName name of the sslConfig used to select cipher suites
     * @param options supported/required flags
     * @throws IOException if server socket can't be configured
     * @throws SSLException
     */
    private void configureServerSocket(SSLServerSocket serverSocket, SSLServerSocketFactory serverSocketFactory, String sslConfigName, OptionsKey options) throws IOException {
        try {

            // Get the ssl properties, need information form the properties to set socket information
            Properties sslProps = sslConfig.getSSLCfgProperties(sslConfigName);

            String[] cipherSuites = sslConfig.getCipherSuites(sslConfigName, serverSocketFactory.getSupportedCipherSuites(), sslProps);

            SSLParameters sslParameters = serverSocket.getSSLParameters();

            //serverSocket.setEnabledCipherSuites(cipherSuites);
            sslParameters.setCipherSuites(cipherSuites);

            // set use cipher order on the ssl parameters
            boolean enforceCipherOrder = Boolean.valueOf(sslProps.getProperty(Constants.SSLPROP_ENFORCE_CIPHER_ORDER, "false"));
            sslParameters.setUseCipherSuitesOrder(enforceCipherOrder);

            // set the SSL protocol on the server socket
            String[] protocols = sslConfig.getSSLProtocol(sslProps);
            if (protocols != null) {
                sslParameters.setProtocols(protocols);
            }

            boolean clientAuthRequired = ((options.requires & EstablishTrustInClient.value) == EstablishTrustInClient.value);
            boolean clientAuthSupported = ((options.supports & EstablishTrustInClient.value) == EstablishTrustInClient.value);
            if (clientAuthRequired) {
                sslParameters.setNeedClientAuth(true);
            } else if (clientAuthSupported) {
                sslParameters.setWantClientAuth(true);
            } else {
                sslParameters.setNeedClientAuth(false);
            }
            serverSocket.setSoTimeout(60 * 1000);
            serverSocket.setSSLParameters(sslParameters);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.debug(tc, "Created SSL server socket on port " + serverSocket.getLocalPort());
                Tr.debug(tc, "    client authentication " + (clientAuthSupported ? "SUPPORTED" : "UNSUPPORTED"));
                Tr.debug(tc, "    client authentication " + (clientAuthRequired ? "REQUIRED" : "OPTIONAL"));
                Tr.debug(tc, "    cipher suites:");

                for (int i = 0; i < cipherSuites.length; i++) {
                    Tr.debug(tc, "    " + cipherSuites[i]);
                }
            }
        } catch (SSLException e) {
            throw new IOException("Could not configure server socket", e);
        }
    }

    /**
     * Create an SSL client socket using the IOR-encoded
     * security characteristics.
     * Setting want/need client auth on a client socket has no effect so all we can do is use the right host, port, ciphers
     *
     * @param host The target host name.
     * @param port The target connection port.
     * @param clientSSLConfigName name of the sslConfig used for cipher suite selection
     * @return An appropriately configured client SSLSocket.
     * @exception IOException if ssl socket can't be obtained and configured.
     */
    private Socket createSSLSocket(String host, int port, final String clientSSLConfigName) throws IOException {
        final SSLSocketFactory factory = getSocketFactory(clientSSLConfigName);
        SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

        socket.setSoTimeout(60 * 1000);

        // Get the ssl properties, need information form the properties to set socket information
        Properties sslProps = sslConfig.getSSLCfgProperties(clientSSLConfigName);

        // get a set of cipher suites appropriate for this connections requirements.
        // We request this for each connection, since the outgoing IOR's requirements may be different from
        // our server listener requirements.
        String[] iorSuites;
        try {
            iorSuites = sslConfig.getCipherSuites(clientSSLConfigName, factory.getSupportedCipherSuites(), sslProps);
        } catch (SSLException e) {
            throw new IOException("Could not set ciphers on socket:", e);
        }

        SSLParameters params = socket.getSSLParameters();

        // Check to see if hostname verification needs to be enabled
        boolean verifyHostname = Boolean.valueOf(sslProps.getProperty(Constants.SSLPROP_HOSTNAME_VERIFICATION, "true"));
        if (verifyHostname) {
            params.setEndpointIdentificationAlgorithm("HTTPS");
        }

        params.setCipherSuites(iorSuites);

        try {
            // set the SSL protocol on the server socket
            String[] protocols = sslConfig.getSSLProtocol(sslProps);
            if (protocols != null) {
                params.setProtocols(protocols);
            }
        } catch (SSLException e) {
            throw new IOException("Could not set protocols on socket:", e);
        }

        socket.setSSLParameters(params);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.debug(tc, "Created SSL socket to " + host + ":" + port);
            Tr.debug(tc, "    cipher suites:");

            for (int i = 0; i < iorSuites.length; i++) {
                Tr.debug(tc, "    " + iorSuites[i]);
            }
            socket.addHandshakeCompletedListener(new HandshakeCompletedListener() {

                @Override
                public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
                    Certificate[] certs = handshakeCompletedEvent.getLocalCertificates();
                    if (certs != null) {
                        Tr.debug(tc, "handshake returned local certs count: " + certs.length);
                        for (int i = 0; i < certs.length; i++) {
                            Certificate cert = certs[i];
                            Tr.debug(tc, "cert: " + cert.toString());
                        }
                    } else {
                        Tr.debug(tc, "handshake returned no local certs");
                    }
                }
            });
        }
        return socket;
    }

    @Override
    public int[] tags() {
        return new int[] { TAG_CSI_SEC_MECH_LIST.value };
    }

    private static TransportAddress createPlainTransportAddress(String host, short port) {
        return new TransportAddress(host, port);
    }

    private static TransportAddress createSslTransportAddress(String host, short port, String sslConfigName) {
        String encodedHost = Util.encodeHost(host, HOST_PROTOCOL, sslConfigName);
        return new TransportAddress(encodedHost, port);
    }

    @Override
    public TransportAddress[] getEndpoints(TaggedComponent tagComponent, Policy[] policies) {

        final CSSConfig cssConfig = getCssConfig(policies);
        List<TransportAddress> addresses = new ArrayList<TransportAddress>();
        for (CompatibleMechanisms compatibleMechanisms : getCompatibleMechanisms(cssConfig, tagComponent)) {
            Map<ServerTransportAddress, CSSTransportMechConfig> cssTransport_mechs = compatibleMechanisms.getCSSCompoundSecMechConfig().getTransportMechMap();

            TSSTransportMechConfig transport_mech = compatibleMechanisms.getTSSCompoundSecMechConfig().getTransport_mech();
            // only handle TSSSSLTransportConfig mechanisms here
            if (!!!(transport_mech instanceof TSSSSLTransportConfig))
                continue;
            TSSSSLTransportConfig transportConfig = (TSSSSLTransportConfig) transport_mech;
            // TLS is configured.  Unless this is explicitly NoProtection, treat the configured port as an SSL port.
            final boolean useProtection = (NoProtection.value & transportConfig.getRequires()) == 0;

            // if cssTransport_mechs is empty then we are not dealing with dynamic SSL config
            if (cssTransport_mechs.isEmpty()) {
                String sslConfigName = compatibleMechanisms.getCSSCompoundSecMechConfig().getTransport_mech().getSslConfigName();

                for (TransportAddress addr : transportConfig.getTransportAddresses()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "IOR to target " + addr.host_name + ":" + (int) (char) addr.port + " using client sslConfig " + sslConfigName);
                    addresses.add(useProtection ? createSslTransportAddress(addr.host_name, addr.port, sslConfigName) : createPlainTransportAddress(addr.host_name, addr.port));
                }
            } else {
                for (Map.Entry<ServerTransportAddress, CSSTransportMechConfig> entry : cssTransport_mechs.entrySet()) {

                    ServerTransportAddress addr = entry.getKey();
                    CSSTransportMechConfig mech_cfg = entry.getValue();

                    String sslConfigName = mech_cfg.getSslConfigName();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "IOR to target " + addr.getHost() + ":" + (int) (char) addr.getPort() + " using client sslConfig " + sslConfigName);
                    addresses.add(useProtection ? createSslTransportAddress(addr.getHost(), addr.getPort(), sslConfigName) : createPlainTransportAddress(addr.getHost(),
                                                                                                                                                         addr.getPort()));
                }
            }
        }
        return addresses.toArray(new TransportAddress[addresses.size()]);
    }

}
