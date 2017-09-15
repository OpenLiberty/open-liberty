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
package com.ibm.ws.tcpchannel.internal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * Generic socket channel wrapper for handling read/write IO attempts. This
 * is expected to be subclassed by IO specific types, such as NIO or AIO.
 */
public abstract class SocketIOChannel implements FFDCSelfIntrospectable {
    private static final TraceComponent tc = Tr.register(SocketIOChannel.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    protected Socket socket;
    protected SocketChannel channel = null;
    protected boolean closed = false;
    protected boolean processClose = true;

    protected TCPChannel tcpChannel = null;
    protected TCPChannelConfiguration cc = null;

    protected boolean checkCancel = false;

    protected enum IOResult {
        COMPLETE, NOT_COMPLETE, FAILED
    }

    /**
     * Constructor.
     * 
     * @param socket
     * @param _tcpChannel
     */
    protected SocketIOChannel(Socket socket, TCPChannel _tcpChannel) {
        this.socket = socket;
        this.tcpChannel = _tcpChannel;
        this.cc = _tcpChannel.getConfig();
        this.tcpChannel.incrementConnectionCount();
        this.channel = socket.getChannel();
    }

    private static class PrivConnect implements PrivilegedExceptionAction<Boolean> {
        private final SocketChannel ioSocket;
        private final InetSocketAddress address;

        /**
         * Constructor.
         * 
         * @param _ioSocket
         * @param _address
         */
        public PrivConnect(SocketChannel _ioSocket, InetSocketAddress _address) {
            this.ioSocket = _ioSocket;
            this.address = _address;
        }

        /*
         * @see java.security.PrivilegedExceptionAction#run()
         */
        public Boolean run() throws IOException {
            return Boolean.valueOf(ioSocket.connect(address));
        }
    }

    protected boolean connect(InetSocketAddress address) throws IOException {

        channel.configureBlocking(false);
        Object token = ThreadIdentityManager.runAsServer();
        PrivConnect attempt = new PrivConnect(channel, address);
        try {
            return AccessController.doPrivileged(attempt).booleanValue();
        } catch (PrivilegedActionException pae) {
            Throwable t = pae.getCause();
            if (!(t instanceof IOException)) {
                throw new IOException("Failed to connect", t);
            }
            throw (IOException) t;
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Attempt to read for the socket.
     * 
     * @param readReq
     * @param fromSelector
     * @return IOResult
     * @throws IOException
     */
    protected IOResult attemptReadFromSocket(TCPBaseRequestContext readReq, boolean fromSelector) throws IOException {

        IOResult rc = IOResult.NOT_COMPLETE;
        TCPReadRequestContextImpl req = (TCPReadRequestContextImpl) readReq;
        TCPConnLink conn = req.getTCPConnLink();

        long dataRead = 0;

        if (req.getJITAllocateSize() > 0 && req.getBuffers() == null) {
            // User wants us to allocate the buffer
            if (conn.getConfig().getAllocateBuffersDirect()) {
                req.setBuffer(ChannelFrameworkFactory.getBufferManager().allocateDirect(req.getJITAllocateSize()));
            } else {
                req.setBuffer(ChannelFrameworkFactory.getBufferManager().allocate(req.getJITAllocateSize()));
            }
            req.setJITAllocateAction(true);
        }

        WsByteBuffer wsBuffArray[] = req.getBuffers();

        dataRead = attemptReadFromSocketUsingNIO(req, wsBuffArray);

        req.setLastIOAmt(dataRead);
        req.setIODoneAmount(req.getIODoneAmount() + dataRead);
        if (req.getIODoneAmount() >= req.getIOAmount()) {
            rc = IOResult.COMPLETE;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Read " + dataRead + "(" + +req.getIODoneAmount() + ")" + " bytes, " + req.getIOAmount() + " requested on local: "
                               + getSocket().getLocalSocketAddress()
                               + " remote: " + getSocket().getRemoteSocketAddress());
        }

        // when request came from selector, it should always read at least 1 byte
        // if it does read 0 (saw this 1 time), just let it retry

        if (req.getLastIOAmt() < 0) {
            // read did not find any data, though the read key was selected
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() && !conn.getConfig().isInbound()) {
                Tr.event(this, tc, "Empty read on outbound.");
            }
            if (req.getJITAllocateAction()) {
                req.getBuffer().release();
                req.setBuffer(null);
                req.setJITAllocateAction(false);
            }
            return IOResult.FAILED;
        }

        if (rc == IOResult.COMPLETE) {
            req.setIOCompleteAmount(req.getIODoneAmount());
            req.setIODoneAmount(0);
        } else if (rc == IOResult.NOT_COMPLETE && !fromSelector && req.getJITAllocateAction() && req.getLastIOAmt() == 0) {
            // Did not read any data on immediate read, so release
            // the buffers used for the jitallocation so that
            // we will re-allocate them again later.
            req.getBuffer().release();
            req.setBuffers(null);
            req.setJITAllocateAction(true);
        }
        return rc;
    }

    protected long attemptReadFromSocketUsingNIO(TCPReadRequestContextImpl req, WsByteBuffer[] wsBuffArray) throws IOException {
        // needs to be overridden by NIO code which extends this object
        throw new IOException("attemptReadFromSocketUsingNIO not overridden");
    }

    /**
     * Attempt to write information stored in the active buffers to the network.
     * 
     * @param req
     * @return IOResult
     * @throws IOException
     */
    protected IOResult attemptWriteToSocket(TCPBaseRequestContext req) throws IOException {
        IOResult rc = IOResult.NOT_COMPLETE;

        WsByteBuffer wsBuffArray[] = req.getBuffers();

        long bytesWritten = attemptWriteToSocketUsingNIO(req, wsBuffArray);

        req.setLastIOAmt(bytesWritten);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Wrote " + bytesWritten + " bytes, " + req.getIOAmount() + " requested on local: " + getSocket().getLocalSocketAddress() + " remote: "
                               + getSocket().getRemoteSocketAddress());
        }

        // when request came from selector, it should always write at least 1 byte
        // if it does write 0 (saw this 1 time), just let it retry

        if (bytesWritten < 0) {
            // invalid value returned for number of bytes written
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() && !req.getTCPConnLink().getConfig().isInbound()) {
                Tr.event(this, tc, "invalid value returned for bytes written");
            }
            return IOResult.FAILED;
        }

        // Determine if we should consider this operation
        // complete or not.
        if (req.getIOAmount() == TCPWriteRequestContext.WRITE_ALL_DATA) {
            ByteBuffer[] buffers = req.getByteBufferArray();
            rc = IOResult.COMPLETE;
            for (int i = 0; i < buffers.length; i++) {
                if (buffers[i].hasRemaining()) {
                    rc = IOResult.NOT_COMPLETE;
                    break; // out of loop
                }
            }

            req.setIODoneAmount(req.getIODoneAmount() + bytesWritten);

        } else {
            req.setIODoneAmount(req.getIODoneAmount() + bytesWritten);
            if (req.getIODoneAmount() >= req.getIOAmount()) {
                rc = IOResult.COMPLETE;
            }
        }

        if (rc == IOResult.COMPLETE) {
            req.setIOCompleteAmount(req.getIODoneAmount());
            req.setIODoneAmount(0);
        }
        return rc;
    }

    protected long attemptWriteToSocketUsingNIO(TCPBaseRequestContext req, WsByteBuffer[] wsBuffArray) throws IOException {
        // needs to be overridden by NIO code which extends this object
        throw new IOException("attemptWriteToSocketUsingNIO not overridden");
    }

    /**
     * Close the underlying IO transport
     */
    public void close() {
    // do nothings
    }

    /**
     * Get the Socket associated with this channel.
     * 
     * @return Socket
     */
    public Socket getSocket() {
        return this.socket;
    }

    /**
     * Get the SocketChannel associated with this channel.
     * 
     * @return SocketChannel
     */
    protected SocketChannel getChannel() {
        return this.channel;
    }

    protected void connectActions() throws IOException {
    // nothing to do
    }

    /**
     * Introspect this object for FFDC output.
     * 
     * @return List<String>
     */
    public List<String> introspect() {
        List<String> rc = new LinkedList<String>();
        String prefix = getClass().getSimpleName() + "@" + hashCode() + ": ";
        rc.add(prefix + "closed=" + this.closed);
        rc.add(prefix + "processClose=" + this.processClose);
        rc.add(prefix + "checkCancel=" + this.checkCancel);
        rc.add(prefix + "tcpChannel=" + this.tcpChannel);
        rc.add(prefix + "socket=" + this.socket);
        if (null != this.socket) {
            rc.add(prefix + "remoteAddr=" + this.socket.getInetAddress());
            rc.add(prefix + "remotePort=" + this.socket.getPort());
            rc.add(prefix + "localAddr=" + this.socket.getLocalAddress());
            rc.add(prefix + "localPort=" + this.socket.getLocalPort());
        }
        rc.add(prefix + "channel=" + this.channel);
        return rc;
    }

    /*
     * @see com.ibm.ws.ffdc.FFDCSelfIntrospectable#introspectSelf()
     */
    public String[] introspectSelf() {
        List<String> rc = introspect();
        return rc.toArray(new String[rc.size()]);
    }
}
