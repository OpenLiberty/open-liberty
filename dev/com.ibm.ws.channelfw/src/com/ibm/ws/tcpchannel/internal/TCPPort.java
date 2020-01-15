/*******************************************************************************
 * Copyright (c) 2005, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.tcpchannel.internal;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.Callable;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ConnectionDescriptorImpl;
import com.ibm.wsspi.channelfw.ConnectionDescriptor;
import com.ibm.wsspi.channelfw.InboundVirtualConnectionFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.RetryableChannelException;
import com.ibm.wsspi.tcpchannel.TCPConfigConstants;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;

/**
 * A class which represents a specific end point. This class
 * links together a protocol name and an end point name. It is also
 * the "natural" place to manage the set of protocol inspectors which
 * get chance to look at an inbound request to decide how to handle
 * it.
 */
public class TCPPort {
    private TCPChannel tcpChannel = null;
    private ServerSocket serverSocket = null;
    protected InboundVirtualConnectionFactory vcf = null;
    private TCPReadCompletedCallback cc = null;
    private int listenPort = 0;

    private TCPChannelConfiguration channelConfigX = null;
    private InetSocketAddress socketAddressX = null;

    private final int timeBetweenRetriesMsec = 1000; // make this non-configurable

    private static final TraceComponent tc = Tr.register(TCPPort.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    /**
     * Constructor.
     *
     * @param _tcpChannel
     * @param _vcf
     */
    protected TCPPort(TCPChannel _tcpChannel, VirtualConnectionFactory _vcf) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "TCPPort");
        }
        this.tcpChannel = _tcpChannel;
        this.vcf = (InboundVirtualConnectionFactory) _vcf;
        this.cc = new NewConnectionInitialReadCallback(tcpChannel);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "TCPPort");
        }
    }

    /**
     * Returns the server socket associated with this end point.
     *
     * @return ServerSocket
     */
    protected synchronized ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    /**
     * Attempt a socket bind to the input address with the given re-use option
     * flag.
     *
     * @param address
     * @param reuseflag
     * @throws IOException
     */
    private void attemptSocketBind(InetSocketAddress address, boolean reuseflag) throws IOException {
        this.serverSocket.setReuseAddress(reuseflag);
        this.serverSocket.bind(address, this.tcpChannel.getConfig().getListenBacklog());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ServerSocket bind worked, reuse=" + this.serverSocket.getReuseAddress());
        }
    }

    private BindInfo portBoundEarly(int port) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "portBoundEarly(int): " + port);
        }

        Map binds = TCPFactoryConfiguration.getEarlyBinds();

        if (binds != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Got Map of early binds");
            }

            BindInfo b = (BindInfo) binds.get(Integer.valueOf(port));

            if (b != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found Bind: " + b);
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "portBoundEarly(int)");
            }

            return b;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "portBoundEarly(int)");
        }
        return null;
    }

    /**
     * Open the listening server socket.
     *
     * @return ServerSocket
     * @throws IOException
     */
    protected ServerSocket openServerSocket() throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        return ssc.socket();
    }

    /**
     * Destroys the server socket associated with this end point.
     */
    protected synchronized void destroyServerSocket() {
        if (null == this.serverSocket) {
            // already closed
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ServerSocket being closed for port " + this.listenPort);
        }
        closeServerSocket();
        this.serverSocket = null;
    }

    /**
     * Destroys the server socket associated with this end point.
     */
    protected void closeServerSocket() {
        try {
            this.serverSocket.close();
        } catch (IOException ioe) {
            // no need to do anything except log it and dereference
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "IOExeption on ServerSocket.close " + ioe.getMessage());
            }
        }
    }

    /**
     * Returns the channel associated with this port.
     *
     * @return TCPChannel
     */
    protected TCPChannel getTCPChannel() {
        return this.tcpChannel;
    }

    /**
     * Processes a new connection by scheduling initial read.
     *
     * @param socket
     */
    public void processNewConnection(SocketIOChannel socket) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "processNewConnection");
        }

        VirtualConnection vc = vcf.createConnection();
        TCPConnLink bc = (TCPConnLink) tcpChannel.getConnectionLink(vc);
        TCPReadRequestContextImpl bcRead = bc.getTCPReadConnLink();

        bc.setSocketIOChannel(socket);

        ConnectionDescriptor cd = vc.getConnectionDescriptor();
        Socket s = socket.getSocket();
        InetAddress remote = s.getInetAddress();
        InetAddress local = s.getLocalAddress();

        if (cd != null) {
            cd.setAddrs(remote, local);
        } else {
            ConnectionDescriptorImpl cdi = new ConnectionDescriptorImpl(remote, local);

            vc.setConnectionDescriptor(cdi);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Processing Connection: " + vc.getConnectionDescriptor());
        }

        int rc = vc.attemptToSetFileChannelCapable(VirtualConnection.FILE_CHANNEL_CAPABLE_ENABLED);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "FileChannelCapable set in VC to: " + rc);
        }

        bcRead.setJITAllocateSize(bc.getConfig().getNewConnectionBufferSize());
        int timeout = bc.getConfig().getInactivityTimeout();
        if (timeout == ValidateUtils.INACTIVITY_TIMEOUT_NO_TIMEOUT) {
            timeout = TCPRequestContext.NO_TIMEOUT;
        }

        // Set a chain property that is used later by the SSL channel on Z only
        // (CRA)
        // This is needed to support a Z unique client certificate mapping function
        // which may be moved to non Z platforms in the future.
        vc.getStateMap().put("REMOTE_ADDRESS", bc.getRemoteAddress().getHostAddress());

        bcRead.read(1, cc, true, timeout);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "processNewConnection");
        }
    }

    /**
     * Query the listening port number.
     *
     * @return int
     */
    protected int getListenPort() {
        return this.listenPort;
    }

    /**
     * Initializes the server socket associated with this end point.
     *
     * @return ServerSocket
     * @throws IOException
     * @throws RetryableChannelException
     */
    protected synchronized ServerSocket initServerSocket() throws IOException, RetryableChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ServerSocket called, new ServerSocket needs to be created");
        }

        TCPChannelConfiguration channelConfig = tcpChannel.getConfig();
        channelConfigX = channelConfig;

        BindInfo earlyBind = portBoundEarly(channelConfig.getPort());

        if (earlyBind == null) {
            InetSocketAddress socketAddress = null;

            if (channelConfig.getHostname() == null) {
                socketAddress = new InetSocketAddress((InetAddress) null, channelConfig.getPort());
            } else {
                socketAddress = new InetSocketAddress(channelConfig.getHostname(), channelConfig.getPort());
            }

            if (!socketAddress.isUnresolved()) {

                socketAddressX = socketAddress;
                // initially set the list port to what is configured, since the port opening will be delayed.
                listenPort = channelConfig.getPort();
                serverSocket = openServerSocket();

                if (channelConfig.getWaitToAccept() != true) {
                    try {
                        CHFWBundle.runWhenServerStarted(new Callable<Void>() {
                            @Override
                            public Void call() {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "CHFW signaled- finishInitServerSocket() to be called");
                                }
                                try {
                                    finishInitServerSocket();
                                } catch (Exception x) {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "CHFW signaled- caught exception from finishInitServerSocket(): " + x);
                                    }

                                    tcpChannel.takeDownChain();

                                }

                                return null;
                            }
                        });
                    } catch (Exception x) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "CHFW signaled- caught exception:: " + x);
                        }
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "CHFW signaled- waitToAccept is true - finishInitServerSocket()");
                    }
                    finishInitServerSocket();
                }

            } else { // unresolved socket address
                String displayableHostName = channelConfig.getDisplayableHostname();
                Tr.error(tc, TCPChannelMessageConstants.LOCAL_HOST_UNRESOLVED,
                         new Object[] { channelConfig.getChannelData().getExternalName(), displayableHostName, String.valueOf(channelConfig.getPort()) });

                throw new RetryableChannelException(new IOException("local address unresolved"));
            }

        } else {
            // this port was bound earlier by a different service
            Exception e = earlyBind.getBindException();
            if (e == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found early bind, setting serverSocket and listenPort");
                }

                serverSocket = earlyBind.getServerSocket();
                // listen port can be different than config port if configed port is '0'
                listenPort = serverSocket.getLocalPort();
            } else {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Early Bind generated the following exception: " + e);
                }

                // add in the exception message to help in problem diagnostics without tracing
                Tr.error(tc, TCPChannelMessageConstants.BIND_ERROR,
                         new Object[] { channelConfig.getChannelData().getExternalName(), earlyBind.getHostname(), String.valueOf(earlyBind.getPort()), e.getMessage() });

                if (e instanceof IOException) {
                    throw (IOException) e;
                }
                if (e instanceof RetryableChannelException) {
                    throw (RetryableChannelException) e;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "new ServerSocket successfully created");
        }

        return this.serverSocket;
    }

    public synchronized void finishInitServerSocket() throws IOException, RetryableChannelException {

        int currentTry = 1;
        int portOpenRetries = channelConfigX.getPortOpenRetries(); // make this configurable - default is 0

        while (currentTry <= portOpenRetries + 1) {
            IOException bindError = null;

            // because opening a channel/port during startup is now a two step process, the channel could have been destroyed
            // and this port destroyed before we get here.   So need to check if the serverSocket is still around
            if (serverSocket == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ServerSocket for this port has already been destroyed, return");
                }
                return;
            }

            // receieve buffer size for accepted sockets is set on serverSocket,
            // send buffer size is set on individual sockets

            if ((channelConfigX.getReceiveBufferSize() >= TCPConfigConstants.RECEIVE_BUFFER_SIZE_MIN)
                && (channelConfigX.getReceiveBufferSize() <= TCPConfigConstants.RECEIVE_BUFFER_SIZE_MAX)) {
                serverSocket.setReceiveBufferSize(channelConfigX.getReceiveBufferSize());
            }

            if (!channelConfigX.getSoReuseAddress()) {
                //Forced re-use==false custom property
                try {
                    attemptSocketBind(socketAddressX, false);
                } catch (IOException e) {
                    // no FFDC
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Forced re-use==false bind attempt failed, ioe=" + e);
                    }
                    bindError = e;
                }
            } else {
                //re-use==true (default)
                // try the standard startup attempts
                try {
                    attemptSocketBind(socketAddressX, false);
                    //If we are not on Windows and the bind succeeded, we should set reuseAddr=true
                    //for future binds.
                    if (!TCPFactoryConfiguration.isWindows()) {
                        this.serverSocket.setReuseAddress(true);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "ServerSocket reuse set to true to allow for later override");
                        }
                    }
                } catch (IOException ioe) {
                    // See if we got the error because the port is in waiting to be cleaned up.
                    // If so, no one should be accepting connections on it, and open should fail.
                    // If that's the case, we can set ReuseAddr to expedite the bind process.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "ServerSocket bind failed on first attempt with IOException: " + ioe.getMessage());
                    }
                    bindError = ioe;
                    try {
                        String hostName = channelConfigX.getHostname();
                        if (hostName == null) {
                            hostName = "localhost";
                        }
                        InetSocketAddress testAddr = new InetSocketAddress(hostName, channelConfigX.getPort());
                        // PK40741 - test for localhost being resolvable before using it
                        if (!testAddr.isUnresolved()) {
                            SocketChannel testChannel = SocketChannel.open(testAddr);
                            // if we get here, socket opened successfully, which means
                            // someone is really listening
                            // so close connection and don't bother trying to bind again
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "attempt to connect to port to check listen status worked, someone else is using the port!");
                            }
                            testChannel.close();
                        } else {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Test connection addr is unresolvable; " + testAddr);
                            }
                        }
                    } catch (IOException testioe) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "attempt to connect to port to check listen status failed with IOException: " + testioe.getMessage());
                        }
                        try {
                            // open (or close) got IOException, retry with reuseAddr on
                            attemptSocketBind(socketAddressX, true);
                            bindError = null;

                        } catch (IOException newioe) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "ServerSocket bind failed on second attempt with IOException: " + newioe.getMessage());
                            }
                            bindError = newioe;
                        }
                    }
                }
            }

            if (bindError == null) {
                // listen port can be different than config port if configed port is '0'
                listenPort = serverSocket.getLocalPort();

                String hostName = null;
                String IPvType = "IPv4";

                Tr.debug(tc, "serverSocket getInetAddress is: " + serverSocket.getInetAddress());
                Tr.debug(tc, "serverSocket getLocalSocketAddress is: " + serverSocket.getLocalSocketAddress());
                Tr.debug(tc, "serverSocket getInetAddress hostname is: " + serverSocket.getInetAddress().getHostName());
                Tr.debug(tc, "serverSocket getInetAddress address is: " + serverSocket.getInetAddress().getHostAddress());
                Tr.debug(tc, "channelConfig.getHostname() is: " + tcpChannel.getConfig().getHostname());
                Tr.debug(tc, "channelConfig.getPort() is: " + tcpChannel.getConfig().getPort());

                if (serverSocket.getInetAddress() instanceof Inet6Address) {
                    IPvType = "IPv6";
                }

                if (tcpChannel.config.getHostname() == null) {
                    hostName = "*  (" + IPvType + ")";
                } else {
                    hostName = serverSocket.getInetAddress().getHostName() + "  (" + IPvType + ": "
                               + serverSocket.getInetAddress().getHostAddress() + ")";
                }

                tcpChannel.setDisplayableHostName(hostName);

                outputBindMessage(channelConfigX.getChannelData().getExternalName(), hostName, listenPort);

                return;

            } else if (currentTry > portOpenRetries) {
                String displayableHostName = channelConfigX.getDisplayableHostname();
                tcpChannel.setDisplayableHostName(displayableHostName);

                // add in the exception message to help in problem diagnostics without tracing
                Tr.error(tc, TCPChannelMessageConstants.BIND_ERROR,
                         new Object[] { channelConfigX.getChannelData().getExternalName(), displayableHostName, String.valueOf(channelConfigX.getPort()),
                                        bindError.getMessage() });

                throw new RetryableChannelException(bindError.getMessage());
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "attempt " + currentTry + " of " + (portOpenRetries + 1) + " failed to open the port, will try again after wait interval");
                }

                currentTry++;

                try {
                    Thread.sleep(timeBetweenRetriesMsec);
                } catch (InterruptedException x) {
                    // do nothing but debug
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "sleep caught InterruptedException.  will proceed.");
                    }
                }
            }
        }
    }

    public void outputBindMessage(String channelName, String hostName, int port) {
        Tr.info(tc, TCPChannelMessageConstants.TCP_CHANNEL_STARTED, new Object[] { channelName, hostName, String.valueOf(port) });
    }

}
