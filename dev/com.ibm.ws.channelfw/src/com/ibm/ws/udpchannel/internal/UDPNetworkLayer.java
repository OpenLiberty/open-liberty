/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.udpchannel.internal;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.udpchannel.UDPConfigConstants;

/**
 * @author mjohnson
 */
public class UDPNetworkLayer {
    private static final TraceComponent tc = Tr.register(UDPNetworkLayer.class, UDPMessages.TR_GROUP, UDPMessages.TR_MSGS);

    private UDPChannel udpChannel = null;
    private UDPSelectorMonitor selectorMonitor = null;
    protected DatagramChannel datagramChannel = null;
    private UDPConnLink connLink = null;
    private String localBindAddress = null;
    private String configuredLocalBindAddress = null;
    private int localBindPort = 0;

    /**
     * Constructor.
     * 
     * @param channel
     * @param selectorMonitor
     * @param localBindAddress
     * @param localPort
     */
    protected UDPNetworkLayer(UDPChannel channel, UDPSelectorMonitor selectorMonitor, String localBindAddress, int localPort) {
        this.udpChannel = channel;
        this.selectorMonitor = selectorMonitor;
        this.localBindAddress = localBindAddress;
        configuredLocalBindAddress = localBindAddress;
        this.localBindPort = localPort;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Constructor: localBindAddress = " + localBindAddress + " localBindPort = " + localPort);
        }
    }

    /**
     * Initializes the datagram socket associated with this end point.
     * 
     * @param vc
     * @return DatagramSocket
     */
    protected DatagramSocket initDatagramSocket(VirtualConnection vc) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Create new DatagramSocket: " + vc);
        }
        UDPChannelConfiguration channelConfig = udpChannel.getConfig();
        InetSocketAddress address = null;

        if (localBindAddress != null) {
            if (localBindAddress.equals("*")) {
                localBindAddress = "0.0.0.0";
            }
            address = new InetSocketAddress(localBindAddress, localBindPort);

            if (address.isUnresolved()) {
                String displayableHostName = channelConfig.getHostname();
                if (displayableHostName == null) {
                    displayableHostName = "*";
                }
                Tr.error(tc, "CWUDP0004E", new Object[] { channelConfig.getExternalName(), displayableHostName, String.valueOf(channelConfig.getPort()) });
                throw (new IOException("local address unresolved"));
            }
        }

        datagramChannel = DatagramChannel.open();
        DatagramSocket datagramSocket = datagramChannel.socket();

        // never reuse - only one app can have the port open
        datagramSocket.setReuseAddress(false);
        datagramChannel.configureBlocking(false);

        // receieve buffer size for sockets is set on datagramSocket,
        // send buffer size is set on individual sockets

        if ((channelConfig.getReceiveBufferSize() >= UDPConfigConstants.RECEIVE_BUFFER_SIZE_MIN)
            && (channelConfig.getReceiveBufferSize() <= UDPConfigConstants.RECEIVE_BUFFER_SIZE_MAX)) {
            int size = datagramSocket.getReceiveBufferSize();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setting receive buffer to size " + channelConfig.getReceiveBufferSize());
            }
            try {
                datagramSocket.setReceiveBufferSize(channelConfig.getReceiveBufferSize());
                int size1 = datagramSocket.getReceiveBufferSize();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (size == size1) {
                        Tr.debug(tc, "setting receive buffer had no effect, still the same size ");
                    }
                }
            } catch (SocketException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught SocketException " + e.getMessage() + " while trying to setReceiveBufferSize to " + channelConfig.getReceiveBufferSize());
                }
            } catch (IllegalArgumentException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught IllegalArgumentException " + e.getMessage() + " while trying to setReceiveBufferSize to " + channelConfig.getReceiveBufferSize());
                }
            }
        }
        if ((channelConfig.getSendBufferSize() >= UDPConfigConstants.SEND_BUFFER_SIZE_MIN) && (channelConfig.getSendBufferSize() <= UDPConfigConstants.SEND_BUFFER_SIZE_MAX)) {
            int size = datagramSocket.getSendBufferSize();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setting send buffer to size " + channelConfig.getSendBufferSize());
            }
            try {
                datagramSocket.setSendBufferSize(channelConfig.getSendBufferSize());
                int size1 = datagramSocket.getSendBufferSize();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (size == size1) {
                        Tr.debug(tc, "setting send buffer had no effect, still the same size ");
                    }
                }
            } catch (SocketException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught SocketException " + e.getMessage() + " while trying to setSendBufferSize to " + channelConfig.getSendBufferSize());
                }
            } catch (IllegalArgumentException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Caught IllegalArgumentException " + e.getMessage() + " while trying to setSendBufferSize to " + channelConfig.getSendBufferSize());
                }
            }
        }
        try {
            datagramSocket.bind(address);
            selectorMonitor.setChannel(datagramChannel, this);
        } catch (IOException ioe) {
            String displayableHostName = channelConfig.getHostname();
            if (displayableHostName == null) {
                displayableHostName = "*";
            }
            Tr.error(tc, "CWUDP0005E", new Object[] { channelConfig.getExternalName(), displayableHostName, String.valueOf(channelConfig.getPort()) });
            throw new IOException(ioe.getMessage());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "new ServerSocket successfully created");
        }

        return datagramChannel.socket();
    }

    public int getListenPort() {
        DatagramSocket datagramSocket = datagramChannel.socket();

        // never reuse - only one app can have the port open
        int port = localBindPort;

        if (datagramSocket != null)
            port = datagramSocket.getLocalPort();

        return port;
    }

    public DatagramSocket getDatagramSocket() {
        return this.datagramChannel.socket();
    }

    public DatagramChannel getDatagramChannel() {
        return this.datagramChannel;
    }

    public String getConfiguredBindAddress() {
        return this.configuredLocalBindAddress;
    }

    public synchronized int send(WsByteBuffer buffer, SocketAddress remoteAddress) throws IOException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Sending buffer " + buffer + " to " + ((InetSocketAddress) remoteAddress).getAddress());
        }
        int numWritten = datagramChannel.send(buffer.getWrappedByteBuffer(), remoteAddress);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Bytes written=" + numWritten);
        }

        return numWritten;
    }

    private static class Result {
        public IOException ex = null;
        public SocketAddress ret = null;

        protected Result() {
            // nothing to do
        }
    }

    private class PrivReceive implements PrivilegedAction<Result> {
        private ByteBuffer buf = null;

        public PrivReceive(ByteBuffer _buf) {
            this.buf = _buf;
        }

        public Result run() {
            Result result = new Result();

            try {
                result.ret = datagramChannel.receive(buf);
                result.ex = null;
            } catch (IOException x) {
                result.ex = x;
                result.ret = null;
            }

            return result;
        }
    }

    public synchronized SocketAddress receive(WsByteBuffer buffer) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "receive");
        }

        // to this in a security sort of way:
        // SocketAddress returnAddress =
        // datagramChannel.receive(buffer.getWrappedByteBuffer());
        // TODO why is this a priv check?
        SocketAddress returnAddress = null;
        PrivReceive privThread = new PrivReceive(buffer.getWrappedByteBuffer());
        Result res = AccessController.doPrivileged(privThread);
        if (res.ex == null) {
            returnAddress = res.ret;
        } else {
            throw res.ex;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (returnAddress != null)
                Tr.debug(tc, "Received buffer " + buffer + " from " + ((InetSocketAddress) returnAddress).getAddress());
            else
                Tr.debug(tc, "Received buffer " + buffer + " from returnAddress == null");
        }

        if (returnAddress != null) {
            boolean validated = getUDPChannel().verifySender(((InetSocketAddress) returnAddress).getAddress());
            if (!validated) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Received buffer from sender not in include list or is in exclude list. " + ((InetSocketAddress) returnAddress).getAddress());
                }
                returnAddress = null;
                buffer.flip();
                buffer.clear();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "receive");
        }

        return returnAddress;
    }

    /**
     * Returns the UDPChannel associated with this UDPNetworkLayer object.
     * 
     * @return UDPChannel
     */
    public UDPChannel getUDPChannel() {
        return this.udpChannel;
    }

    public synchronized void destroy() {
        try {
            datagramChannel.close();
            datagramChannel = null;
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error closing datagram channel " + e);
            }
        }
    }

    /**
     * @return UDPConnLink
     */
    public UDPConnLink getConnLink() {
        return this.connLink;
    }

    /**
     * @param link
     */
    public void setConnLink(UDPConnLink link) {
        this.connLink = link;
    }

}
