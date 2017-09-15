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
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.bytebuffer.internal.WsByteBufferImpl;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;
import com.ibm.wsspi.tcpchannel.TCPRequestContext;

/**
 * TCP request context for reading data from the network.
 */
public abstract class TCPReadRequestContextImpl extends TCPBaseRequestContext implements TCPReadRequestContext, FFDCSelfIntrospectable {

    private int jitAllocateSize = 0;
    private boolean jitAllocateAction = false;

    // Completion callback used to notify the requestor when their
    // request is complete.
    private TCPReadCompletedCallback callback;

    private static final TraceComponent tc = Tr.register(TCPReadRequestContextImpl.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    /**
     * Constructor.
     * 
     * @param link
     */
    public TCPReadRequestContextImpl(TCPConnLink link) {
        super(link);
        setRequestTypeRead(true);
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPReadRequestContext#read(long, int)
     */
    @Override
    public long read(long numBytes, int time) throws IOException {
        int timeout = time;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "read(" + numBytes + "," + timeout + ")");
        }
        long bytesRead = 0L;

        getTCPConnLink().incrementNumReads();
        if (getConfig().getDumpStatsInterval() > 0) {
            getTCPConnLink().getTCPChannel().totalSyncReads.incrementAndGet();
        }
        // always check external call parms
        checkForErrors(numBytes, false, timeout);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Socket socket = getTCPConnLink().getSocketIOChannel().getSocket();
            Tr.event(tc, "read (sync) requested for local: " + socket.getLocalSocketAddress() + " remote: " + socket.getRemoteSocketAddress());
        }

        if (isAborted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Previously aborted, unable to perform read");
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
            bytesRead = processSyncReadRequest(numBytes, timeout);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "read: " + bytesRead);
        }
        return bytesRead;
    }

    abstract protected long processSyncReadRequest(long numBytes, int timeout) throws IOException;

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPReadRequestContext#read(long,
     * com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback, boolean, int)
     */
    @Override
    public VirtualConnection read(long numBytes, TCPReadCompletedCallback readCallback, boolean forceQueue, int timeout) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "read(" + numBytes + ",..," + forceQueue + "," + timeout + ")");
        }
        getTCPConnLink().incrementNumReads();
        if (getConfig().getDumpStatsInterval() > 0) {
            getTCPConnLink().getTCPChannel().totalAsyncReads.incrementAndGet();
        }
        // always check external call parms
        checkForErrors(numBytes, true, timeout);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Socket socket = getTCPConnLink().getSocketIOChannel().getSocket();
            Tr.event(tc, "read (async) requested for local: " + socket.getLocalSocketAddress() + " remote: " + socket.getRemoteSocketAddress());
        }

        VirtualConnection vc = null;

        if (isAborted()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Previously aborted, unable to perform read");
            }
            // async errors can only be passed through the callback, which means
            // if they didn't provide one then we have no way of reporting the
            // failure
            if (null != readCallback) {
                IOException ioe = new IOException("Connection aborted by program");
                readCallback.error(getTCPConnLink().getVirtualConnection(), this, ioe);
            }
        } else {
            vc = readInternal(numBytes, readCallback, forceQueue, timeout);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "read: " + vc);
        }
        return vc;
    }

    // internal async read entry point
    protected VirtualConnection readInternal(long numBytes, TCPReadCompletedCallback readCallback, boolean forceQueue, int timeout) {
        if (timeout == IMMED_TIMEOUT) {
            immediateTimeout();
            return null;
        }
        if (timeout == ABORT_TIMEOUT) {
            abort();
            immediateTimeout();
            return null;
        }

        setIOAmount(numBytes);
        setLastIOAmt(0);
        setIODoneAmount(0);
        setReadCompletedCallback(readCallback);
        setForceQueue(forceQueue);
        setTimeoutTime(timeout);

        // IMPROVEMENT: buffers should be preprocessed before calling read,
        // postprocessed after
        return processAsyncReadRequest();
    }

    abstract protected VirtualConnection processAsyncReadRequest();

    private void checkForErrors(long numBytes, boolean isAsync, int _timeout) {
        if (_timeout == IMMED_TIMEOUT || _timeout == ABORT_TIMEOUT) {
            return;
        }
        String errorMsg = null;

        if (getBuffer() != null && (getBuffer().getStatus() & WsByteBuffer.STATUS_TRANSFER_TO) != 0) {
            // can't use FileChannel buffers for reads, so convert
            getBuffer().setStatus(WsByteBuffer.STATUS_BUFFER);
        }

        // see if max read size has been exceeded
        else if (numBytes > maxReadSize) {
            errorMsg = "Number of bytes requested to read: " + numBytes + " exceeds the maximum allowed for one read";
        }
        // see if numbytes is valid for request type
        else if ((numBytes < 0 && !isAsync) || (numBytes < 1 && isAsync)) {
            errorMsg = "Number of bytes requested to read: " + numBytes + " is less than minimum allowed (0 for sync, 1 for asynch)";
        }
        // if jitallocate to be used, see if numbytes is valid for jit allocate size
        else if (getJITAllocateSize() > 0 && getBuffers() == null) {
            if (numBytes > getJITAllocateSize()) {
                errorMsg = "Number of bytes requested: " + numBytes + " exceeds JIT allocated buffer size: " + getJITAllocateSize();
            }
        }
        // not jit allocate, see if buffers provided
        else if (getBuffers() == null || getBuffers().length == 0) {
            errorMsg = "No buffer(s) provided for reading data into";
        }
        // buffers to be used, see if they have room
        else {
            WsByteBuffer wsBuffArray[] = getBuffers();
            long bytesAvail = 0;
            for (int bufNum = 0; bufNum < wsBuffArray.length; bufNum++) {
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

        if (errorMsg != null) {
            IllegalArgumentException iae = new IllegalArgumentException(errorMsg);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, errorMsg);
            }
            FFDCFilter.processException(iae, getClass().getName(), "100", this);
            throw iae;
        }
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPReadRequestContext#setJITAllocateSize(int)
     */
    @Override
    public void setJITAllocateSize(int numBytes) {
        this.jitAllocateSize = numBytes;
    }

    /**
     * Get the JIT buffer allocation size.
     * 
     * @return int
     */
    protected int getJITAllocateSize() {
        return this.jitAllocateSize;
    }

    /*
     * @see com.ibm.wsspi.tcpchannel.TCPReadRequestContext#getJITAllocateAction()
     */
    @Override
    public boolean getJITAllocateAction() {
        return this.jitAllocateAction;
    }

    /**
     * Set the flag on whether the current buffer is JIT allocated or not.
     * 
     * @param flag
     */
    protected void setJITAllocateAction(boolean flag) {
        this.jitAllocateAction = flag;
    }

    protected void setReadCompletedCallback(TCPReadCompletedCallback cb) {
        this.callback = cb;
    }

    protected TCPReadCompletedCallback getReadCompletedCallback() {
        return this.callback;
    }

    // d235756 add new function
    abstract protected void immediateTimeout();

    /**
     * @return ByteBuffer[] to be used on the read
     */
    public ByteBuffer[] preProcessReadBuffers() {
        WsByteBuffer wsBuffArray[] = getBuffers();
        boolean containsNonDirect = false;

        // check if there are any non-direct elements
        for (int i = 0; i < wsBuffArray.length; i++) {
            if (wsBuffArray[i] == null) {
                break;
            }
            if (!wsBuffArray[i].isDirect() && wsBuffArray[i].hasArray()) {
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
                ((WsByteBufferImpl) wsBuffArray[i]).setParmsToDirectBuffer();
            }
        } catch (ClassCastException cce) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reading with non-WsByteBufferImpl, may hurt performance");
            }
            return (getByteBufferArray());
        }

        setBuffersToDirect(wsBuffArray);
        return (getByteBufferArrayDirect());
    }

    protected ByteBuffer preProcessOneReadBuffer() {
        // We can process the single buffer case more efficiently than if we
        // include it as a part of the multi-buffer logic
        WsByteBufferImpl wsBuffImpl = null;
        try {
            wsBuffImpl = (WsByteBufferImpl) getBuffer();
        } catch (ClassCastException cce) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Reading with a non-WsByteBufferImpl, may hurt performance");
            }
            return getBuffer().getWrappedByteBuffer();
        }

        if (!wsBuffImpl.isDirect() && wsBuffImpl.hasArray()) {
            wsBuffImpl.setParmsToDirectBuffer();
            return wsBuffImpl.oWsBBDirect;
        }
        return wsBuffImpl.getWrappedByteBufferNonSafe();
    }

    protected void postProcessReadBuffers(long dataRead) {

        if (getByteBufferArrayDirect() == null) {
            try {
                // see if we should post process just the one buffer case
                if (!getBuffer().isDirect()) {
                    ((WsByteBufferImpl) getBuffer()).copyFromDirectBuffer((int) dataRead);
                    // 298587 Add this return here to avoid another possible (useless)
                    // copy below
                    return;
                }
                // if no sideband direct buffers were used, there is nothing special to
                // to
                return;
            } catch (ClassCastException cce) {
                // Nothing to do here, already flagged this condition on the
                // pre-processing
                return;
            }
        }

        long dataLeft = dataRead;
        WsByteBuffer wsBuffArray[] = getBuffers();
        int bufferSpace;

        for (int i = 0; i < wsBuffArray.length; i++) {
            if (wsBuffArray[i] == null) {
                break;
            }
            bufferSpace = wsBuffArray[i].remaining();

            if (wsBuffArray[i].isDirect() == false) {
                try {

                    if (bufferSpace < dataLeft) {
                        // fill up everything from position to limit of the non-direct
                        // buffer
                        ((WsByteBufferImpl) wsBuffArray[i]).copyFromDirectBuffer(bufferSpace);
                    } else {
                        // fill up from position of the non-direct buffer only the data that
                        // has been read
                        ((WsByteBufferImpl) wsBuffArray[i]).copyFromDirectBuffer((int) dataLeft);

                        // and we are done copying
                        break;
                    }
                } catch (ClassCastException cce) {
                    // Nothing to do here, already flagged this condition on the
                    // pre-processing
                    return;
                }
            } else {
                if (bufferSpace >= dataLeft) {
                    // and we are done copying
                    break;
                }
            }
            dataLeft -= bufferSpace;
        }
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
        rc.add(prefix + "callback=" + this.callback);
        rc.add(prefix + "jitAllocateSize=" + this.jitAllocateSize);
        rc.add(prefix + "jitAllocateAction=" + this.jitAllocateAction);
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