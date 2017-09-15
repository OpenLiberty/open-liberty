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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * TCP channel's NIO implementation of a write context.
 */
public class NioTCPWriteRequestContextImpl extends TCPWriteRequestContextImpl {

    private static final TraceComponent tc = Tr.register(NioTCPWriteRequestContextImpl.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    /**
     * Constructor.
     * 
     * @param link
     */
    protected NioTCPWriteRequestContextImpl(TCPConnLink link) {
        super(link);
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.TCPWriteRequestContextImpl#
     * processSyncWriteRequest(long, int)
     */
    @Override
    public long processSyncWriteRequest(long numBytes, int timeout) throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "processSyncRequest(" + numBytes + "," + timeout + ")");
        }

        long numUserBytesWritten = 0; // no. of user bytes written

        if (numBytes != 0) {
            if (this.blockWait == null) {
                this.blockWait = new SimpleSync();
            }
            this.blockingIOError = null;

            // before we write, signal that we want to do the write ourselves
            // and not the worker threads.
            this.blockedThread = true;

            VirtualConnection vc = writeInternal(numBytes, null, false, timeout);

            while (vc == null) {
                // block until we are told to write
                this.blockWait.simpleWait();

                if (this.blockingIOError == null) {
                    vc = ((NioTCPChannel) getTCPConnLink().getTCPChannel()).getWorkQueueManager().processWork(this, 1);
                } else {
                    break;
                }
            }

            this.blockedThread = false;
            if (this.blockingIOError != null) {
                throw this.blockingIOError;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getIOCompleteAmount() -->" + String.valueOf(getIOCompleteAmount()));
            }
            numUserBytesWritten = getIOCompleteAmount();
        } else {
            WsByteBuffer buffers[] = getBuffers();
            NioSocketIOChannel channel = (NioSocketIOChannel) getTCPConnLink().getSocketIOChannel();
            if (buffers.length == 1) {
                numUserBytesWritten = channel.write(buffers[0].getWrappedByteBufferNonSafe());
            } else {
                numUserBytesWritten = channel.write(getByteBufferArray());
            }
        }

        // return the number of bytes written
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "processSyncRequest", Long.valueOf(numUserBytesWritten));
        }
        return (numUserBytesWritten);
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.TCPWriteRequestContextImpl#
     * processAsyncWriteRequest()
     */
    @Override
    public VirtualConnection processAsyncWriteRequest() {
        return ((NioTCPChannel) getTCPConnLink().getTCPChannel()).getWorkQueueManager().processWork(this, 0);
    }

    /*
     * @see
     * com.ibm.ws.tcpchannel.internal.TCPWriteRequestContextImpl#immediateTimeout
     * ()
     */
    @Override
    protected void immediateTimeout() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "immediateTimeout");
        }

        // if we do not have a selector, then there has not been a write yet
        // on this connection. If we have a selector, it doesn't mean there's
        // an active write; however, waking up the selector with a false timeout
        // attempt is not optimal but not a big deal
        ChannelSelector sel = ((NioSocketIOChannel) getTCPConnLink().getSocketIOChannel()).getChannelSelectorWrite();
        if (null != sel) {
            // selector uses granularity of 1 second, so subtract 2 seconds
            // to guarantee the timeout will fire immediately
            this.timeoutTime = System.currentTimeMillis() - 2000L;
            sel.resetTimeout(this.timeoutTime);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "No write selector, ignoring immediate timeout");
            }
        }
    }

}
