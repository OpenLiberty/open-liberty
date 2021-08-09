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
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.bytebuffer.internal.WsByteBufferImpl;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * NIO specific implementation of a SocketIOChannel.
 */
public class NioSocketIOChannel extends SocketIOChannel {

    private static final TraceComponent tc = Tr.register(NioSocketIOChannel.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    private ChannelSelector channelSelectorRead = null;
    private ChannelSelector channelSelectorWrite = null;

    /**
     * Constructor.
     * 
     * @param socket
     * @param _tcpChannel
     */
    protected NioSocketIOChannel(Socket socket, TCPChannel _tcpChannel) {
        super(socket, _tcpChannel);
        this.checkCancel = TCPFactoryConfiguration.getCancelKeyOnClose();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Created NioSocketIOChannel");
        }
    }

    protected static SocketIOChannel createIOChannel(Socket _socket, TCPChannel _tcpChannel) {
        return new NioSocketIOChannel(_socket, _tcpChannel);
    }

    protected SelectionKey register(Selector sel, int ops, Object att) throws ClosedChannelException {
        return channel.register(sel, ops, att);
    }

    protected void setChannelSelectorRead(ChannelSelector sr) {
        this.channelSelectorRead = sr;
    }

    protected void setChannelSelectorWrite(ChannelSelector sr) {
        this.channelSelectorWrite = sr;
    }

    /**
     * @return ChannelSelector
     */
    protected ChannelSelector getChannelSelectorRead() {
        return this.channelSelectorRead;
    }

    /**
     * @return ChannelSelector
     */
    protected ChannelSelector getChannelSelectorWrite() {
        return this.channelSelectorWrite;
    }

    protected long attemptReadFromSocketUsingNIO(TCPReadRequestContextImpl req, WsByteBuffer[] wsBuffArray) throws IOException {

        long dataRead = 0;

        if (wsBuffArray.length == 1) {

            if ((!wsBuffArray[0].isDirect()) && (wsBuffArray[0].hasArray())) {
                // To avoid lots of casting use a local var.
                // Can't cast the array
                try {
                    WsByteBufferImpl wsBuffImpl = (WsByteBufferImpl) wsBuffArray[0];
                    wsBuffImpl.setParmsToDirectBuffer();

                    dataRead = read(wsBuffImpl.oWsBBDirect);

                    wsBuffImpl.copyFromDirectBuffer((int) dataRead);
                } catch (ClassCastException cce) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "Reading with a non-WsByteBufferImpl, may hurt performance");
                    }
                    dataRead = read(wsBuffArray[0].getWrappedByteBufferNonSafe());
                }
            } else {
                dataRead = read(wsBuffArray[0].getWrappedByteBufferNonSafe());
            }
        } else {
            ByteBuffer readBuffArray[] = req.preProcessReadBuffers();
            dataRead = read(readBuffArray);
            req.postProcessReadBuffers(dataRead);
        }

        return dataRead;
    }

    protected long attemptWriteToSocketUsingNIO(TCPBaseRequestContext req, WsByteBuffer[] wsBuffArray) throws IOException {

        long bytesWritten = 0;

        if (wsBuffArray.length == 1) {
            if ((!wsBuffArray[0].isDirect()) && (wsBuffArray[0].hasArray())) {
                // To avoid lots of casting use a local var.
                // Can't cast the array
                WsByteBufferImpl wsBuffImpl = null;
                try {
                    wsBuffImpl = (WsByteBufferImpl) wsBuffArray[0];

                    wsBuffImpl.copyToDirectBuffer();

                    bytesWritten = write(wsBuffImpl.oWsBBDirect);

                    wsBuffImpl.setParmsFromDirectBuffer();
                } catch (ClassCastException cce) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "Writing with a non-WsByteBufferImpl, may hurt performance");
                    }
                    bytesWritten = write(wsBuffArray[0].getWrappedByteBufferNonSafe());
                }
            } else {

                bytesWritten = write(wsBuffArray[0].getWrappedByteBufferNonSafe());
            }
        } else {

            boolean containsNonDirect = false;
            // check if there are any non-direct elements
            for (int i = 0; i < wsBuffArray.length; i++) {
                if (wsBuffArray[i] == null) {
                    break;
                }
                if ((!wsBuffArray[i].isDirect()) && (wsBuffArray[i].hasArray())) {
                    containsNonDirect = true;
                    break;
                }
            }
            if (!containsNonDirect) {
                // Buffers are all direct - use them as is
                bytesWritten = write(req.getByteBufferArray());
            } else {
                // copy non-Direct to Direct buffers, to save GC, since
                // the JDK will use (a temporary) direct if we don't.
                try {
                    for (int i = 0; i < wsBuffArray.length; i++) {
                        if (wsBuffArray[i] == null) {
                            break;
                        }
                        ((WsByteBufferImpl) (wsBuffArray[i])).copyToDirectBuffer();
                    }

                    req.setBuffersToDirect(wsBuffArray);
                    bytesWritten = write(req.getByteBufferArrayDirect());

                    for (int i = 0; i < wsBuffArray.length; i++) {
                        if (wsBuffArray[i] == null) {
                            break;
                        }

                        ((WsByteBufferImpl) wsBuffArray[i]).setParmsFromDirectBuffer();
                    }
                } catch (ClassCastException cce) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(this, tc, "Writing with non-WsByteBufferImpl, may hurt performance");
                    }

                    bytesWritten = write(req.getByteBufferArray());
                }

            }
        }

        return bytesWritten;
    }

    protected int read(ByteBuffer dst) throws IOException {
        return channel.read(dst);
    }

    protected long read(ByteBuffer[] dst) throws IOException {
        return channel.read(dst);
    }

    protected int write(ByteBuffer dst) throws IOException {
        return channel.write(dst);
    }

    protected long write(ByteBuffer[] dst) throws IOException {
        return channel.write(dst);
    }

    /**
     * Close the socket
     */
    public void close() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "close");
        }

        // Doing a channel.close() will actually put the keys associated with this
        // channel
        // into the associated selectors cancelledKey list, but they don't get
        // removed until the
        // next time the selector runs. In some cases, we need to make sure they are
        // really gone before we
        // we close the channel, because another thread may have requested another
        // operation
        // on the channel at the same time, which causes problems on some OS'es.
        // So, cancel them manually and wait for the selector to run once before
        // continuing

        // call super.close() to see if we are using Regular Sockets, and if so, let
        // the parent class handle closing the socket
        super.close();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "SocketChannel closing, local: " + socket.getLocalSocketAddress() + " remote: " + socket.getRemoteSocketAddress());
        }

        // synchronize on this SocketIOChannel to prevent duplicate closes from
        // being processed
        synchronized (this) {
            if (closed) {
                processClose = false;
            }
            closed = true;
        }

        if (processClose) {

            // checkCancel is only on if we need to manually cancel keys as
            // the socket close() is supposed to do that for us
            if (checkCancel) {
                // Remove socket from current selectors.
                if (channelSelectorRead != null) {
                    SelectionKey k = channelSelectorRead.getKey(channel);
                    if (k != null) {
                        CancelRequest cr = new CancelRequest(k);
                        synchronized (cr) {
                            channelSelectorRead.addCancelRequest(cr);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "waiting for read key to be canceled, key is " + k);
                            }
                            try {
                                cr.wait();
                            } catch (InterruptedException e) {
                                // No FFDC code needed
                            }
                        } // end-sync
                    }
                }
                if (channelSelectorWrite != null) {
                    SelectionKey k = channelSelectorWrite.getKey(channel);
                    if (k != null) {
                        CancelRequest cr = new CancelRequest(k);
                        synchronized (cr) {
                            channelSelectorWrite.addCancelRequest(cr);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "waiting for write key to be canceled, key is " + k);
                            }
                            try {
                                cr.wait();
                            } catch (InterruptedException e) {
                                // No FFDC code needed
                            }
                        } // end-sync
                    }
                }
            }

            // retry a few times then ignore close failures
            for (int retry_count=0;3>retry_count;++retry_count) {
              try {
                  if (channel != null) {
                      channel.close();
                  }

                  // need to make sure the keys get cancelled after the close call
                  if (channelSelectorRead != null) {
                      channelSelectorRead.wakeup();
                      channelSelectorRead = null;
                  }
                  if (channelSelectorWrite != null) {
                      channelSelectorWrite.wakeup();
                      channelSelectorWrite = null;
                  }
                  break;
              } catch (Throwable t) {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                      Tr.debug(this, tc, "(" + retry_count + ") Error closing channel: " + t);
                  }
                  // if we get repeated exceptions try waiting a second
                  if (1==retry_count) try { Thread.sleep(1000); } catch (InterruptedException ie) {}
              }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "close");
        }
    }

}
