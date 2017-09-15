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
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.List;

import com.ibm.io.async.AsyncSocketChannel;
import com.ibm.io.async.AsyncSocketChannelHelper;
import com.ibm.io.async.IAsyncFuture;
import com.ibm.io.async.ICompletionListener;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * AIO specific implementation of a SocketIOChannel.
 */
public class AioSocketIOChannel extends SocketIOChannel {
    private static final TraceComponent tc = Tr.register(AioSocketIOChannel.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    private AsyncSocketChannel asyncChannel = null;
    private AsyncSocketChannelHelper asyncHelper = null;
    private IAsyncFuture readFuture = null;
    private IAsyncFuture writeFuture = null;
    private final ByteBuffer[] singleReadBuffer = new ByteBuffer[1];
    private final ByteBuffer[] singleWriteBuffer = new ByteBuffer[1];

    /**
     * Constructor.
     * 
     * @param socket
     * @param achannel
     * @param _tcpChannel
     */
    protected AioSocketIOChannel(Socket socket, AsyncSocketChannel achannel, TCPChannel _tcpChannel) {
        super(socket, _tcpChannel);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "AioSocketIOChannel");
        }
        asyncChannel = achannel;
        asyncHelper = new AsyncSocketChannelHelper(asyncChannel);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "AioSocketIOChannel");
        }
    }

    protected static SocketIOChannel createIOChannel(Socket _socket, AsyncSocketChannel achannel, TCPChannel _tcpChannel) {
        AioSocketIOChannel ioSocket = new AioSocketIOChannel(_socket, achannel, _tcpChannel);
        return ioSocket;
    }

    @Override
    public void connectActions() throws IOException {
        asyncChannel.prepareSocket();
    }

    protected boolean readAIO(AioTCPReadRequestContextImpl req, boolean force, long timeout) throws IOException {
        ICompletionListener readAIOListener = AioTCPChannel.getAioReadCompletionListener();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "readAIO");
        }
        boolean forceQueue = force;
        boolean complete = false;
        long byteCount = 0;
        boolean useJITinNative = false;
        ByteBuffer bufArray[] = null;
        VirtualConnection vci = req.getTCPConnLink().getVirtualConnection();

        if (req.getBuffers().length == 1) {
            singleReadBuffer[0] = req.preProcessOneReadBuffer();
            bufArray = singleReadBuffer;
        } else {
            bufArray = req.preProcessReadBuffers();
        }
        // if jit is supprted by native code (not on windows) and if we jit
        // allocated a buffer
        // and it hasn't been used yet, and its an 8k buffer, we can
        // let the native code use the jit buffer passed on ev2 if the request
        // doesn't complete
        // immediately.
        if (AioTCPChannel.getJitSupportedByNative() && req.getJITAllocatedDirect() == true && req.getIODoneAmount() == 0 && req.getJITAllocateSize() == 8192) {
            useJITinNative = true;
        }

        readFuture = asyncHelper.read(bufArray, timeout, forceQueue, req.getIOAmount() - req.getIODoneAmount(), useJITinNative, vci, true);

        if (readFuture == null) {
            // will be null if permission logic won't allow completion
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "readAIO: false");
            }
            return false;
        }

        if (readFuture.isCompleted() && forceQueue == false) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "data already available and forceQueue is false");
            }
            try {
                byteCount = readFuture.getByteCount();
            } catch (InterruptedException ie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "InterruptedException received on immediate async read " + ie.getMessage());
                }

                FFDCFilter.processException(ie, getClass().getName() + ".readAIO", "138");

                IOException newIOE = new IOException(ie.getMessage());
                newIOE.initCause(ie);
                throw newIOE;
            }
            req.postProcessReadBuffers(byteCount);
            complete = req.updateIOCounts(byteCount, 0);
            if (!complete) {
                // don't need to call preProcessReadBuffers again, since
                // bufArray should
                // still be in sync with req buffers.
                readFuture = asyncHelper.read(bufArray, timeout, forceQueue, req.getIOAmount(), false, vci, true);
                if (tcpChannel.getConfig().getDumpStatsInterval() > 0) {
                    tcpChannel.totalAsyncReadRetries.incrementAndGet();
                }
            }
        }
        if (!complete) {
            // force queue is on or read is not complete
            // if JIT will be used in native to read data and no data read yet,
            // release the JIT buffer alloced
            if (useJITinNative && byteCount == 0 && !readFuture.isCompleted()) {
                // safe to release and null buffer because we can't get called
                // back until listener is added
                req.getBuffer().release();
                req.setBuffer(null);
            }
            // add listener to be called back later on pooled thread
            readFuture.addCompletionListener(readAIOListener, req);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "added completion listener to read future");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "readAIO: " + complete);
        }
        return complete;
    }

    protected long readAIOSync(long numBytes, TCPReadRequestContextImpl req) throws IOException {

        VirtualConnection vci = req.getTCPConnLink().getVirtualConnection();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "readAIOSync");
        }
        boolean complete = false;
        long byteCount = 0;
        long remainingTimeout = req.getTimeoutInterval();
        while (!complete) {
            ByteBuffer bufArray[] = null;
            if (req.getBuffers().length == 1) {
                singleReadBuffer[0] = req.preProcessOneReadBuffer();
                bufArray = singleReadBuffer;
            } else {
                bufArray = req.preProcessReadBuffers();
            }
            // Do the read, enforce the timeout later
            // since thread is ties up anyway, no special JIT processing of
            // buffer needed
            readFuture = asyncHelper.read(bufArray, false, numBytes, false, vci, false);
            // readFuture should never be null, since permission logic is not
            // used with sync reads/writes. Don't even check, to avoid needless
            // main line code, and an illogical return path

            try {
                if (numBytes != 0) {
                    // get the byteCount read, wait for completion by providing
                    // a timeout
                    byteCount = readFuture.getByteCount(remainingTimeout);
                    req.postProcessReadBuffers(byteCount);
                } else {
                    // request is to read bytes if any are available and to
                    // return right away.
                    if (readFuture.isCompleted()) {
                        // there was data to be read right away, or
                        // an error condition occurred right away.
                        byteCount = readFuture.getByteCount();
                        req.postProcessReadBuffers(byteCount);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                            Tr.exit(tc, "readAIOSync: " + byteCount);
                        }
                        return byteCount;
                    }
                    // no bytes available to read right away.
                    // return back to the user
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "readAIOSync: 0");
                    }
                    return 0;
                }
            } catch (InterruptedException ie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "InterruptedException caught while doing getByteCount");
                }
                FFDCFilter.processException(ie, getClass().getName() + ".readAIOSync", "234");
                IOException newIOE = new IOException(ie.getMessage());
                newIOE.initCause(ie);
                throw newIOE;
            }
            complete = req.updateIOCounts(byteCount, 0);
            if (!complete) {
                // update partial read stats
                if (tcpChannel.getConfig().getDumpStatsInterval() > 0) {
                    tcpChannel.totalPartialSyncReads.incrementAndGet();
                }
                // need to do another read, so reset the remaining timeout value if not
                // infinite
                if (req.getTimeoutInterval() != 0) {

                    remainingTimeout = req.getTimeoutTime() - System.currentTimeMillis();
                    if (remainingTimeout <= 0) {
                        IOException ioe = new SocketTimeoutException("Sync read timed out after reading partial data");
                        throw ioe;
                    }
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "readAIOSync: " + req.getIODoneAmount());
        }
        return req.getIODoneAmount();
    }

    protected boolean writeAIO(TCPWriteRequestContextImpl req, boolean force, long timeout) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "writeAIO");
        }
        ICompletionListener writeAIOListener = AioTCPChannel.getAioWriteCompletionListener();
        boolean forceQueue = force;
        boolean complete = false;
        long byteCount;
        ByteBuffer bufArray[] = null;
        VirtualConnection vci = req.getTCPConnLink().getVirtualConnection();

        if (req.getBuffers().length == 1) {
            singleWriteBuffer[0] = req.preProcessOneWriteBuffer();
            bufArray = singleWriteBuffer;
        } else {
            bufArray = req.preProcessWriteBuffers();
        }
        writeFuture = asyncHelper.write(bufArray, req.getTimeoutInterval(), forceQueue, req.getIOAmount(), vci, true);

        if (writeFuture == null) {
            // will be null if permission logic won't allow completion
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "writeAIO");
            }
            return false;
        }

        if (writeFuture.isCompleted() && forceQueue == false) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "data already written and forceQueue is false");
            }
            try {
                byteCount = writeFuture.getByteCount();
            } catch (InterruptedException ie) {
                FFDCFilter.processException(ie, getClass().getName() + ".writeAIO", "290");
                IOException newIOE = new IOException(ie.getMessage());
                newIOE.initCause(ie);
                throw newIOE;
            }
            req.postProcessWriteBuffers(byteCount);

            if (req.getIOAmount() == TCPWriteRequestContext.WRITE_ALL_DATA) {

                complete = ((AioTCPWriteRequestContextImpl) req).updateForAllData(byteCount);
            } else {

                complete = req.updateIOCounts(byteCount, 1);
            }
            if (!complete) {
                // don't need to call preProcessWriteBuffers again, since
                // bufArray should
                // still be in sync with req buffers.
                writeFuture = asyncHelper.write(bufArray, req.getTimeoutInterval(), forceQueue, req.getIOAmount(), vci, true);
                if (tcpChannel.getConfig().getDumpStatsInterval() > 0) {
                    tcpChannel.totalAsyncWriteRetries.incrementAndGet();
                }
            }
        }
        if (!complete) {
            // add listener to be called back later on pooled thread
            writeFuture.addCompletionListener(writeAIOListener, req);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "added completion listener to write future");
            }

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "writeAIO");
        }
        return complete;
    }

    protected long writeAIOSync(TCPWriteRequestContextImpl req) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "writeAIOSync");
        }
        VirtualConnection vci = req.getTCPConnLink().getVirtualConnection();
        boolean complete = false;
        long byteCount = 0;
        long remainingTimeout = req.getTimeoutInterval();
        while (!complete) {
            ByteBuffer bufArray[] = null;
            if (req.getBuffers().length == 1) {
                singleWriteBuffer[0] = req.preProcessOneWriteBuffer();
                bufArray = singleWriteBuffer;
            } else {
                bufArray = req.preProcessWriteBuffers();
            }
            // Do the write, enforce the timeout later
            writeFuture = asyncHelper.write(bufArray, false, req.getIOAmount(), vci, false);
            // writeFuture should never be null, since permission logic is not
            // used with sync reads/writes. Don't even check, to avoid needless
            // main line code, and an illogical return path

            try {
                // get the byteCount written, wait for completion by providing a
                // timeout
                byteCount = writeFuture.getByteCount(remainingTimeout);
                req.postProcessWriteBuffers(byteCount);
            } catch (InterruptedException ie) {
                FFDCFilter.processException(ie, getClass().getName() + ".writeAIOSync", "358");
                IOException newIOE = new IOException(ie.getMessage());
                newIOE.initCause(ie);
                throw newIOE;
            }
            if (req.getIOAmount() == TCPWriteRequestContext.WRITE_ALL_DATA) {

                complete = ((AioTCPWriteRequestContextImpl) req).updateForAllData(byteCount);
            } else {
                complete = req.updateIOCounts(byteCount, 1);
            }
            if (!complete) {
                // update partial write stats
                if (tcpChannel.getConfig().getDumpStatsInterval() > 0) {
                    tcpChannel.totalPartialSyncWrites.incrementAndGet();
                }
                // need to do another write, so reset the remaining timeout value if not
                // infinite
                if (req.getTimeoutInterval() != 0) {
                    remainingTimeout = req.getTimeoutTime() - System.currentTimeMillis();
                    if (remainingTimeout <= 0) {
                        IOException ioe = new SocketTimeoutException("Sync write timed out after writing partial data");
                        throw ioe;
                    }
                }

            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "writeAIOSync");
        }
        return req.getIODoneAmount();
    }

    protected void timeoutReadFuture() {
        if (readFuture != null && !readFuture.isCompleted()) {
            readFuture.cancel(new SocketTimeoutException("Socket read operation timed out by application request"));
        }
    }

    protected void timeoutWriteFuture() {
        if (writeFuture != null && !writeFuture.isCompleted()) {
            writeFuture.cancel(new SocketTimeoutException("Socket write operation timed out by application request"));
        }
    }

    /**
     * Close the socket
     */
    @Override
    public void close() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "close");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "SocketChannel close starting, local: " + socket.getLocalSocketAddress() + " remote: " + socket.getRemoteSocketAddress());
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
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "AsyncSocketChannel close, local: " + socket.getLocalSocketAddress() + " remote: " + socket.getRemoteSocketAddress());
                if (asyncChannel != null) {
                    asyncChannel.close();
                }
            } catch (IOException ioe) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "IOException while closing channel " + ioe.getMessage());
                }
            } finally {
                super.close();
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "close called on channel already closed, local: " + socket.getLocalSocketAddress() + " remote: " + socket.getRemoteSocketAddress());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "close");
        }
    }

    protected AsyncSocketChannel getAsyncChannel() {
        return this.asyncChannel;
    }

    /**
     * Introspect this object for FFDC output.
     * 
     * @return List<String>
     */
    @Override
    public List<String> introspect() {
        List<String> rc = super.introspect();
        String prefix = getClass().getSimpleName() + "@" + hashCode() + ": ";
        rc.add(prefix + "asyncChannel=" + this.asyncChannel);
        rc.add(prefix + "asyncHelper=" + this.asyncHelper);
        rc.add(prefix + "readFuture=" + this.readFuture);
        rc.add(prefix + "writeFuture=" + this.writeFuture);
        return rc;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.SocketIOChannel#introspectSelf()
     */
    @Override
    public String[] introspectSelf() {
        List<String> rc = introspect();
        return rc.toArray(new String[rc.size()]);
    }
}
