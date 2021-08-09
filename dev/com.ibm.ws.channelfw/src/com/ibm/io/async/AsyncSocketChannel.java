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
package com.ibm.io.async;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.tcpchannel.internal.TCPChannelMessageConstants;

/**
 * A socket channel that provides asynchronous read and write operations.
 * <p>
 * <code>AsyncSocketChannels</code> don't directly provide all the capabilities of network sockets. Operations such as Binding, shutdown,
 * and the setting/querying of socket options must be done using a {@link java.net.Socket Socket}object retrieved from the
 * <code>AsyncSocketChannel</code> using the {@link #socket() socket()}method. Note that an existing <code>socket</code> object cannot be
 * used to create an <code>AsyncSocketChannel</code>. However, manipulation of the underlying socket object must be done carefully and
 * not all operations on the socket are supported by AsyncSocketChannel. Forbidden operations include:
 * <ul>
 * <li>Changing the blocking mode of the socket. The AsyncSocketChannel sets the socket into a particular blocking mode to suit its
 * needs. This mode must not be changed by the application.</li>
 * <li>Closure operations directly on the socket should not be done. Closing the AsyncSocketChannel should only be done by calling
 * the <code>close()</code> method of the AsyncSocketChannel.</li>
 * </ul>
 * Use of these operations will cause undefined results.
 * </p>
 * <p>
 * <code>AsyncSocketChannels</code> are safe for use by multiple concurrent threads. They support concurrent reading and writing, but do
 * not support multiple simultaneous reads or multiple simultaneous writes (often referred to as "overlapped IO") on the same channel. An
 * attempt to overlap IO on a socket results in an <code>IOPending</code> exception.
 * </p>
 */
public class AsyncSocketChannel extends AbstractAsyncChannel {

    protected static final TraceComponent tc = Tr.register(AsyncSocketChannel.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    private static final long INVALID_SOCKET = -1L;

    private static IAsyncProvider provider = AsyncLibrary.getInstance();

    protected static boolean nioUtilsClassChecked = false;
    protected static Class<?> nioUtilsClass = null;
    protected static Method getFileDescriptorMethod = null;

    protected static class ConnectReturn {
        protected boolean isConnected = false;
        protected IOException ioe = null;
    }

    protected static class FieldReturn {
        protected long val;
        protected AsyncException e = null;
    }

    class PrivFieldCheck implements PrivilegedAction<FieldReturn> {
        /** socket channel reference */
        private SocketChannel ioSocket;

        /**
         * Constructor.
         * 
         * @param _ioSocket
         */
        public PrivFieldCheck(SocketChannel _ioSocket) {
            this.ioSocket = _ioSocket;
        }

        public FieldReturn run() {
            FieldReturn ret = new FieldReturn();

            // one time check to see if NIOUtils class and getFileDescriptor method exist
            if (nioUtilsClassChecked == false) {
                /*
                 * See if the NIO Utils class exists. If so, we can use it later to get
                 * the file descriptors for SocketChannels. If it doesn't exist, then
                 * we will use the getDeclaredField approach
                 */
                try {
                    nioUtilsClass = Class.forName("com.ibm.nio.NIOUtils"); //$NON-NLS-1$
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "NIOUtils class was found");
                    }
                    getFileDescriptorMethod = nioUtilsClass.getDeclaredMethod("getFileDescriptor", new Class[] { SocketChannel.class });
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "NIOUtils getFileDescriptor method was found");
                    }
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Problem loading NIOUtils class/method: " + e.getMessage());
                    }
                    // do nothing, NIOUtils class doesn't exist
                } // end try
                nioUtilsClassChecked = true;
            }
            try {
                // if NIO utililties class exists, use it to get file descriptor
                if (getFileDescriptorMethod != null) {
                    ret.val = ((java.lang.Long) getFileDescriptorMethod.invoke(nioUtilsClass, new Object[] { channel })).longValue();
                } else {
                    Field fdValField = this.ioSocket.getClass().getDeclaredField("fdVal"); //$NON-NLS-1$
                    fdValField.setAccessible(true);
                    ret.val = fdValField.getInt(this.ioSocket);
                }
            } catch (Exception e) {
                // A problem occured getting the underlying file handle, raise an
                // exception
                ret.e = new AsyncException(e.getLocalizedMessage());
            }
            // check for invalid value returned for fd
            if (ret.val == -1) {
                ret.e = new AsyncException("Invalid fd val obtained from JDK");
                FFDCFilter.processException(ret.e, "com.ibm.io.async.AsyncSocketChannel", "161");
            }
            return ret;
        }
    }

    /**
     * Opens a new <code>AsyncSocketChannel</code>.
     * 
     * @param asyncChannelGroup
     * @return The new, open asynchronous socket channel.
     * @throws AsyncException
     *             if a problem occurred with initializing the async library
     * @throws IOException
     *             if a problem occurs opening the underlying socket channel
     */

    public static AsyncSocketChannel open(AsyncChannelGroup asyncChannelGroup) throws AsyncException, IOException {
        return new AsyncSocketChannel(SocketChannel.open(), asyncChannelGroup);
    }

    // The underlying (synchronous) channel implementation.
    protected SocketChannel channel;

    // Marks this channel as having a socket prepared for async IO operations.
    private boolean prepared = false;

    /**
     * Public constructor: Initializes a new instance of an AsyncSocketChannel.
     * 
     * @param channel
     *            the wrapped NIO socket channel
     * @param asyncChannelGroup
     * @throws IOException
     *             if problem occurs preparing the underlying socket for async IO calls
     */

    public AsyncSocketChannel(SocketChannel channel, AsyncChannelGroup asyncChannelGroup) throws IOException {

        super(asyncChannelGroup);
        this.channel = channel;
        // The channel may be already connected if it came from a server channel.
        if (this.channel.isConnected()) {
            this.prepareSocket();
        }

        // Single reads & writes at a time only for sockets
        // singleWriteOnly = true;
        // singleReadOnly = true;
    }

    /**
     * @return SocketChannel that is being used
     */
    public SocketChannel getSocketChannel() {
        return this.channel;
    }

    /**
     * Closes this channel.
     * <p>
     * Any outstanding IO operations are completed with an <code>AsyncException</code> containing a platform specific
     * return code.
     * </p>
     * <p>
     * Once the channel is closed, no further IO operations can be performed on the channel.
     * Any read or write operation on a closed channel will result in a <code>ClosedChannelException</code>.
     * </p>
     * <p>
     * Closing the channel also closes the underlying platform socket.
     * </p>
     * 
     * @throws IOException
     *             if a problem occurs closing the underlying platform socket.
     */
    public synchronized void close() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "close");
        }
        if (prepared) {
            // If the channel was prepared, clean up the CompletionKeys
            // and dispose of the channelID
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "close - disposing of socket, channel id: " + channelIdentifier +
                             ", local: " + channel.socket().getLocalSocketAddress() +
                             ", remote: " + channel.socket().getRemoteSocketAddress());
            }

            // cancel any timeout entries, since we are closing.
            // since read/writes can be outstanding during channel shutdown
            // we need to at least remove the timeout entries, even if input
            // tracking is not being used.
            if (readFuture != null) {
                if (readFuture.getTimeoutWorkItem() != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "cancelling timeout entry. readFuture.getTimeoutWorkItem().state: " + readFuture.getTimeoutWorkItem().state);
                    }
                    readFuture.getTimeoutWorkItem().state = TimerWorkItem.ENTRY_CANCELLED;
                }
            }
            if (writeFuture != null) {
                if (writeFuture.getTimeoutWorkItem() != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "cancelling timeout entry writeFuture.getTimeoutWorkItem().state: " + writeFuture.getTimeoutWorkItem().state);
                    }
                    writeFuture.getTimeoutWorkItem().state = TimerWorkItem.ENTRY_CANCELLED;
                }
            }

            if (channelVCI != null) {
                if (channelVCI.isInputStateTrackingOperational()) {
                    // need to make sure that any outstanding reads or writes
                    // have been cancelled at the native layer, before
                    // de-initializing objects

                    // if channelVCI is not null, then multiIO has been called,
                    // and channelIdentifier must be valid

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Input Tracking (Permission logic) is on");
                    }

                    int rc;

                    if (channelVCI.isCloseWithReadOutstanding()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(
                                     tc,
                                     "calling cancel2 on readiocb: channel id: "
                                                     + channelIdentifier
                                                     + "  call id: "
                                                     + readIOCB.getCallIdentifier());
                        }

                        rc = provider.cancel2(channelIdentifier, readIOCB.getCallIdentifier());

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "return from cancel2 rc (0 - success): " + rc);
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "read was not outstanding");
                        }
                    }

                    if (channelVCI.isCloseWithWriteOutstanding()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(
                                     tc,
                                     "calling cancel2 on writeiocb channel id: "
                                                     + channelIdentifier
                                                     + " call id: "
                                                     + writeIOCB.getCallIdentifier());
                        }

                        rc = provider.cancel2(channelIdentifier, writeIOCB.getCallIdentifier());

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "return from cancel2 rc (0 - success): " + rc);
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "write was not outstanding");
                        }
                    }
                }
            }

            provider.terminateIOCB(readIOCB);
            // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            // Tr.debug(tc, "putting object in pool, hc: " + readIOCB.hashCode());
            AsyncLibrary.completionKeyPool.put(readIOCB);

            provider.terminateIOCB(writeIOCB);
            // if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            // Tr.debug(tc, "putting object in pool, hc: " + writeIOCB.hashCode());
            AsyncLibrary.completionKeyPool.put(writeIOCB);

            provider.dispose(channelIdentifier);
            prepared = false;
        } // end if

        channel.close();

        super.close();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "close");
        }
    } // end method close()

    protected long getFileDescriptor() throws AsyncException {
        FieldReturn fRet = AccessController.doPrivileged(new PrivFieldCheck(channel));
        if (fRet.e != null) {
            throw fRet.e;
        }
        return fRet.val;
    }

    /**
     * Returns whether the <code>AsyncSocketChannel</code> is connected.
     * 
     * @return <code>true</code> if the channel is connected, <code>false</code> otherwise.
     */
    public boolean isConnected() {
        return this.channel.isConnected();
    }

    /**
     * Returns whether this <code>AsyncSocketChannel</code> is open.
     * 
     * @return <code>true</code> if the channel is open, <code>false</code> otherwise.
     */
    public boolean isOpen() {
        return this.channel.isOpen();
    }

    /**
     * Perform initialization steps for this new connection.
     * 
     * @throws IOException
     */
    public synchronized void prepareSocket() throws IOException {
        if (!prepared) {
            final long fd = getFileDescriptor();
            if (fd == INVALID_SOCKET) {
                throw new AsyncException(AsyncProperties.aio_handle_unavailable);
            }

            channelIdentifier = provider.prepare2(fd, asyncChannelGroup.getCompletionPort());

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "prepareSocket - socket prepared, fd = " + fd
                             + " channel id: " + channelIdentifier + " "
                             + ", local: " + channel.socket().getLocalSocketAddress()
                             + ", remote: " + channel.socket().getRemoteSocketAddress());
            }
            long callid = 0; // init to zero, reset when IO requested

            readIOCB = (CompletionKey) AsyncLibrary.completionKeyPool.get();
            if (readIOCB != null) {
                // initialize the IOCB from the pool
                readIOCB.initializePoolEntry(channelIdentifier, callid);

                writeIOCB = (CompletionKey) AsyncLibrary.completionKeyPool.get();
                if (writeIOCB != null) {
                    // initialize the IOCB from the pool
                    writeIOCB.initializePoolEntry(channelIdentifier, callid);
                } else {
                    writeIOCB = new CompletionKey(channelIdentifier, callid, defaultBufferCount);
                }
            } else {
                readIOCB = new CompletionKey(channelIdentifier, callid, defaultBufferCount);
                writeIOCB = new CompletionKey(channelIdentifier, callid, defaultBufferCount);
            }

            provider.initializeIOCB(readIOCB);
            provider.initializeIOCB(writeIOCB);
            prepared = true;
        }
    }

    /**
     * Retrieves the underlying {@link java.net.Socket socket}associated with this channel.
     * <p>
     * Any operations performed on the socket are visible to the channel.
     * </p>
     * 
     * @return the socket.
     */
    public Socket socket() {
        return this.channel.socket();
    }

}