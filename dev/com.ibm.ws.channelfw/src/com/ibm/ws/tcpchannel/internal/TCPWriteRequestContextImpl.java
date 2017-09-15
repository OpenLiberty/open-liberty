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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.bytebuffer.internal.FCWsByteBufferImpl;
import com.ibm.ws.bytebuffer.internal.WsByteBufferImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;
import com.ibm.wsspi.tcpchannel.TCPWriteCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPWriteRequestContext;

/**
 * TCP request context for writing data on the network.
 */
public abstract class TCPWriteRequestContextImpl extends TCPBaseRequestContext implements TCPWriteRequestContext {

    private static final long FILE_CHANNEL_SEGMENT_SIZE = 4096000L;

    // Completion callback used to notify the requestor when their
    // request is complete.
    private TCPWriteCompletedCallback callback;

    private static final TraceComponent tc = Tr.register(TCPWriteRequestContextImpl.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    protected TCPWriteRequestContextImpl(TCPConnLink value) {
        super(value);
        setRequestTypeRead(false);
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPWriteRequestContext#write(long, int)
     */
    @Override
    public long write(long numBytes, int time) throws IOException {
        int timeout = time;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "write(" + numBytes + "," + timeout + ")");
        }
        getTCPConnLink().incrementNumWrites();
        if (getConfig().getDumpStatsInterval() > 0) {
            getTCPConnLink().getTCPChannel().totalSyncWrites.incrementAndGet();
        }
        // always check external call parms
        checkForErrors(numBytes, false, timeout);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Socket socket = getTCPConnLink().getSocketIOChannel().getSocket();
            Tr.event(tc, "write (sync) requested for local: " + socket.getLocalSocketAddress() + " remote: " + socket.getRemoteSocketAddress());
        }

        long numUserBytesWritten = 0L; // no. of user bytes written

        if (isAborted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Previously aborted, unable to perform write");
            }
            throw new IOException("Connection aborted by program");
        }

        if (timeout == IMMED_TIMEOUT) {
            immediateTimeout();
        } else if (timeout == ABORT_TIMEOUT) {
            abort();
            immediateTimeout();
        } else {
            // if using channel timeout, reset to that value
            if (timeout == TCPRequestContext.USE_CHANNEL_TIMEOUT) {
                timeout = getConfig().getInactivityTimeout();
            }
            // check if this is a FileChannel write request
            if (isFileChannelWriteToBeDone()) {
                numUserBytesWritten = fileChannelWrite(numBytes, timeout);
            } else {
                numUserBytesWritten = processSyncWriteRequest(numBytes, timeout);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "write(sync): " + numUserBytesWritten);
        }
        return (numUserBytesWritten);
    }

    private boolean isFileChannelWriteToBeDone() {
        // first check the buffer, before accessing the VC, try to avoid cache hits
        if ((getBuffer().getStatus() & WsByteBuffer.STATUS_TRANSFER_TO) != 0) {

            if (getTCPConnLink().getVirtualConnection().isFileChannelCapable()) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "FileChannel writing is enabled");
                }
                return true;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "FileChannel writing is disabled");
            }

            int _status = getBuffer().getStatus();

            // TRASFER_TO status is current on, so turn if OFF
            _status = _status & (~WsByteBuffer.STATUS_TRANSFER_TO);
            // and turn on BUFFER status
            _status = _status | WsByteBuffer.STATUS_BUFFER;

            getBuffer().setStatus(_status);
        }
        return false;
    }

    private long fileChannelWrite(long minBytesToWrite, long timeout) throws IOException {

        long maxToWrite = 0;
        long totalWritten = 0;
        long numWritten = 0;
        long size = 0;
        long startPosition = 0;
        long minAllWrites = 0;
        long startTime = 0;
        boolean possibleTimeout = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "fileChannelWrite(" + minBytesToWrite + ", " + timeout + ")");
        }

        SocketChannel sc = getTCPConnLink().getSocketIOChannel().getChannel();
        FCWsByteBufferImpl fcb = (FCWsByteBufferImpl) getBuffer();
        FileChannel fc = fcb.getFileChannel();

        startTime = CHFWBundle.getApproxTime();

        try {
            size = fc.size();
            startPosition = fc.position();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "size:" + size + " pos:" + startPosition);
            }

            if (minBytesToWrite == TCPWriteRequestContext.WRITE_ALL_DATA) {
                minAllWrites = size - startPosition;
            } else {
                minAllWrites = minBytesToWrite;
            }

            // keep going until done since each transferTo can write random
            // amounts
            while (totalWritten < minAllWrites) {
                maxToWrite = size - totalWritten - startPosition;
                if (maxToWrite > FILE_CHANNEL_SEGMENT_SIZE) {
                    maxToWrite = FILE_CHANNEL_SEGMENT_SIZE;
                }
                try {
                    numWritten = fc.transferTo(startPosition + totalWritten, maxToWrite, sc);
                } catch (IOException ioe) {
                    // http://bugs.sun.com/view_bug.do?bug_id=5103988
                    // Linux throws this exception for EAGAIN by mistake (fixed in JDK7)
                    if (ioe.getMessage().contains("Resource temporarily unavailable")) {
                        numWritten = 0;
                    } else {
                        throw ioe;
                    }
                }
                if (numWritten == 0) {
                    // TCP network buffers are probably full
                    Thread.yield();
                } else {
                    totalWritten += numWritten;
                }

                if ((CHFWBundle.getApproxTime() - startTime) > timeout) {
                    possibleTimeout = true;
                    break;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "transferTo wrote: " + numWritten + " total:" + totalWritten);
                }
            } // end-while
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "IOException: " + e);
            }
            // only throw the exception if we have not successfully written any bytes.
            // If we have written some successfully, then update the user with that
            // info
            // and don't give the user back an error (exception)
            if (totalWritten < minAllWrites) {
                FFDCFilter.processException(e, getClass().getName() + ".fileChannelWrite", "190", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "fileChannelWrite: IOException Thrown");
                }
                throw e;
            }
        }

        // adjust the position to reflect how much was written
        fc.position(startPosition + totalWritten);

        // check if this should give a timeout error
        if (possibleTimeout && totalWritten < minAllWrites) {
            IOException e = new SocketTimeoutException("Socket operation, used by a File Channel, timed out before it could be completed");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "fileChannelWrite: SocketTimeoutException Thrown");
            }
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "fileChannelWrite: " + totalWritten);
        }
        return totalWritten;
    }

    protected String getStackTrace(Throwable oThrowable) {
        // Return the stack trace as a string
        if (null == oThrowable) {
            return null;
        }
        StringWriter oStringWriter = new StringWriter();
        PrintWriter oPrintWriter = new PrintWriter(oStringWriter);
        oThrowable.printStackTrace(oPrintWriter);
        oPrintWriter.close();
        return oStringWriter.toString();
    }

    /**
     * Process a synchyronous write attempt with the input information.
     * 
     * @param numBytes
     * @param timeout
     * @return long - number of bytes written
     * @throws IOException
     */
    abstract public long processSyncWriteRequest(long numBytes, int timeout) throws IOException;

    // external async write
    @Override
    public VirtualConnection write(long numBytes, TCPWriteCompletedCallback writeCallback, boolean forceQueue, int timeout) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "write(" + numBytes + ",..," + forceQueue + "," + timeout + ")");
        }
        getTCPConnLink().incrementNumWrites();
        if (getConfig().getDumpStatsInterval() > 0) {
            getTCPConnLink().getTCPChannel().totalAsyncWrites.incrementAndGet();
        }
        // always check external call parms
        checkForErrors(numBytes, true, timeout);
        // reset
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Socket socket = getTCPConnLink().getSocketIOChannel().getSocket();
            Tr.event(tc, "write (async) requested for local: " + socket.getLocalSocketAddress() + " remote: " + socket.getRemoteSocketAddress());
        }
        VirtualConnection vc = null;
        if (isAborted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Previously aborted, unable to perform write");
            }
            // async errors can only be passed through the callback, which means
            // if they didn't provide one then we have no way of reporting the
            // failure
            if (null != writeCallback) {
                IOException ioe = new IOException("Connection aborted by program");
                writeCallback.error(getTCPConnLink().getVirtualConnection(), this, ioe);
            }
        } else {
            vc = writeInternal(numBytes, writeCallback, forceQueue, timeout);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "write: " + vc);
        }
        return vc;
    }

    // internal async write
    protected VirtualConnection writeInternal(long numBytes, TCPWriteCompletedCallback writeCallback, boolean forceQueue, int time) {
        int timeout = time;
        if (timeout == IMMED_TIMEOUT) {
            immediateTimeout();
            return null;
        } else if (timeout == ABORT_TIMEOUT) {
            abort();
            immediateTimeout();
            return null;
        }

        // if using channel timeout, reset to that value
        if (timeout == TCPRequestContext.USE_CHANNEL_TIMEOUT) {
            timeout = getConfig().getInactivityTimeout();
        }
        setIOAmount(numBytes);
        setLastIOAmt(0);
        setIODoneAmount(0);
        setWriteCompletedCallback(writeCallback);
        setForceQueue(forceQueue);
        setTimeoutTime(timeout);

        return processAsyncWriteRequest();
    }

    /**
     * Process an asynchronous write request.
     * 
     * @return VirtualConnection - null if this went async, non-null if complete
     */
    abstract public VirtualConnection processAsyncWriteRequest();

    private void checkForErrors(long numBytes, boolean isAsync, int _timeout) {
        if (_timeout == IMMED_TIMEOUT || _timeout == ABORT_TIMEOUT) {
            return;
        }
        String errorMsg = null;

        // see if max read size has been exceeded
        if (numBytes > maxWriteSize) {
            errorMsg = "Number of bytes to requested to write: " + numBytes + " exceeds the maximum allowed for one write";
        }

        if (isAsync && getBuffer() != null && (getBuffer().getStatus() & WsByteBuffer.STATUS_TRANSFER_TO) != 0) {
            // can't use FileChannel buffers for Async writes, so convert
            getBuffer().setStatus(WsByteBuffer.STATUS_BUFFER);
        }

        if (getBuffers() == null || getBuffers().length == 0) {
            errorMsg = "No buffer(s) provided for writing data from";
        } else if ((getBuffer().getStatus() & WsByteBuffer.STATUS_TRANSFER_TO) == 0) {
            // an FCWsByteBufferImpl is not being used, or if used it is not in
            // TransferTo mode
            // see if buffer was provided
            if (getBuffers() == null || getBuffers().length == 0) {
                errorMsg = "No buffer(s) provided for writing data from";
            }
            // buffers not null, check length specified
            else if (numBytes < -1 || (numBytes == 0 && isAsync)) {
                errorMsg = "Number of bytes requested to write: " + numBytes + " is not valid";
            } else {

                WsByteBuffer wsBuffArray[] = getBuffers();
                long bytesAvail = 0;

                for (int bufNum = 0; bufNum < getBuffers().length; bufNum++) {
                    // passing an array with only some of the elements used is OK
                    if (wsBuffArray[bufNum] != null) {
                        bytesAvail += wsBuffArray[bufNum].limit() - wsBuffArray[bufNum].position();
                    } else {
                        break;
                    }
                }
                if (numBytes > bytesAvail || bytesAvail == 0) {
                    errorMsg = "Number of bytes requested: " + numBytes + " exceeds space remaining in the buffers provided: " + bytesAvail;
                }
            }
        } else {
            // an FCWsByteBufferImpl is being used and is in TransferTo mode
            long space;
            FCWsByteBufferImpl fcb = (FCWsByteBufferImpl) getBuffer();
            try {
                // some NIO methods cannot handle "longs", only 32-bit sizes
                if (fcb.getFileChannel().size() > maxWriteSize) {
                    errorMsg = "Number of possible bytes in the File Channel:  " + fcb.getFileChannel().size() + " exceeds maximum allowed: " + maxWriteSize;
                }

                space = fcb.limit() - fcb.position();

                if (numBytes > (int) space) {
                    errorMsg = "Number of bytes requested: " + numBytes + " exceeds bytes remaining in the FileChannel: " + space;
                }
            } catch (IOException ioe) {
                FFDCFilter.processException(ioe, getClass().getName() + ".checkForErrors", "377", this);
                errorMsg = "space remaining in FileChannel cannot be determined.";
            }
        }

        if (errorMsg != null) {
            IllegalArgumentException iae = new IllegalArgumentException(errorMsg);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, errorMsg);
            }
            FFDCFilter.processException(iae, getClass().getName(), "100", this);
            throw iae;
        }
    }

    protected void setWriteCompletedCallback(TCPWriteCompletedCallback cb) {
        this.callback = cb;
    }

    /**
     * Access the current write callback for this context.
     * 
     * @return TCPWriteCompletedCallback
     */
    public TCPWriteCompletedCallback getWriteCompletedCallback() {
        return this.callback;
    }

    // d235756 add new function
    abstract protected void immediateTimeout();

    /**
     * Before attempting a write of one buffer, perform any work required
     * and return the proper NIO buffer to write out.
     * 
     * @return ByteBuffer
     */
    public ByteBuffer preProcessOneWriteBuffer() {
        // We can process the single buffer case more efficiently than if we
        // include it as a part of the multi-buffer logic
        WsByteBufferImpl wsBuffImpl = null;
        try {
            wsBuffImpl = (WsByteBufferImpl) getBuffer();
        } catch (ClassCastException cce) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Writing with a non-WsByteBufferImpl");
            }
            return getBuffer().getWrappedByteBuffer();
        }

        if (!wsBuffImpl.isDirect() && wsBuffImpl.hasArray()) {
            wsBuffImpl.copyToDirectBuffer();
            return wsBuffImpl.oWsBBDirect;
        }
        return wsBuffImpl.getWrappedByteBufferNonSafe();
    }

    /**
     * @return ByteBuffer[] to be used on the read
     */
    public ByteBuffer[] preProcessWriteBuffers() {
        WsByteBuffer wsBuffArray[] = getBuffers();
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
            return (getByteBufferArray());
        }
        // copy non-Direct to Direct buffers, to save GC, since
        // the JDK will use (a temporary) direct if we don't.
        try {
            for (int i = 0; i < wsBuffArray.length; i++) {
                if (wsBuffArray[i] == null) {
                    break;
                }
                ((WsByteBufferImpl) wsBuffArray[i]).copyToDirectBuffer();
            }
        } catch (ClassCastException cce) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Writing with a non-WsByteBufferImpl, may hurt performance");
            }
            return (getByteBufferArray());
        }

        setBuffersToDirect(wsBuffArray);

        return (getByteBufferArrayDirect());
    }

    /**
     * After the write call, perform any actions required on the write buffers.
     * 
     * @param dataWritten
     */
    public void postProcessWriteBuffers(long dataWritten) {

        if (getByteBufferArrayDirect() == null) {
            try {
                // see if we should post process just the one buffer case
                if (((WsByteBufferImpl) getBuffer()).oWsBBDirect != null) {
                    ((WsByteBufferImpl) getBuffer()).setParmsFromDirectBuffer();
                }
            } catch (ClassCastException cce) {
                // Nothing to do here, already flagged this condition on the
                // pre-processing
            }
            return;
        }

        WsByteBuffer wsBuffArray[] = getBuffers();
        for (int i = 0; i < wsBuffArray.length; i++) {
            if (wsBuffArray[i] == null) {
                break;
            }
            try {
                ((WsByteBufferImpl) wsBuffArray[i]).setParmsFromDirectBuffer();
            } catch (ClassCastException cce) {
                // Nothing to do here, already flagged this condition on the
                // pre-processing
                return;
            }

        }
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.TCPBaseRequestContext#introspect()
     */
    @Override
    public List<String> introspect() {
        List<String> rc = super.introspect();
        String prefix = getClass().getSimpleName() + "@" + hashCode() + ": ";
        rc.add(prefix + "callback=" + callback);
        return rc;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.TCPBaseRequestContext#introspectSelf()
     */
    @Override
    public String[] introspectSelf() {
        List<String> rc = introspect();
        return rc.toArray(new String[rc.size()]);
    }
}
