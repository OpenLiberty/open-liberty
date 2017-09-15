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
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * TCP read request specific for the NIO channel.
 */
public class NioTCPReadRequestContextImpl extends TCPReadRequestContextImpl {

    private static final TraceComponent tc = Tr.register(NioTCPReadRequestContextImpl.class, TCPChannelMessageConstants.TCP_TRACE_NAME, TCPChannelMessageConstants.TCP_BUNDLE);

    /**
     * Constructor.
     * 
     * @param link
     */
    public NioTCPReadRequestContextImpl(TCPConnLink link) {
        super(link);
    }

    /*
     * @see
     * com.ibm.ws.tcpchannel.internal.TCPReadRequestContextImpl#processSyncReadRequest
     * (long, int)
     */
    @Override
    public long processSyncReadRequest(long numBytes, int timeout) throws IOException {
        long bytesRead = 0L;
        if (numBytes != 0L) {
            if (this.blockWait == null) {
                this.blockWait = new SimpleSync();
            }

            this.blockingIOError = null;

            // before we read, signal that we want to do the read ourselves
            // and not the worker threads.
            this.blockedThread = true;

            VirtualConnection vc = readInternal(numBytes, null, false, timeout);

            while (vc == null) {
                // block until we are told to read
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
            // return the number of bytes read
            bytesRead = getIOCompleteAmount();
        } else {
            // read immediately and return
            setJITAllocateAction(false);

            if ((getJITAllocateSize() > 0) && (getBuffers() == null)) {
                // User wants us to allocate the buffer
                if (getConfig().getAllocateBuffersDirect()) {
                    setBuffer(ChannelFrameworkFactory.getBufferManager().allocateDirect(getJITAllocateSize()));
                } else {
                    setBuffer(ChannelFrameworkFactory.getBufferManager().allocate(getJITAllocateSize()));
                }
                setJITAllocateAction(true);
            }
            WsByteBuffer wsBuffArray[] = getBuffers();
            NioSocketIOChannel channel = (NioSocketIOChannel) getTCPConnLink().getSocketIOChannel();
            if (wsBuffArray.length == 1) {
                bytesRead = channel.read(wsBuffArray[0].getWrappedByteBufferNonSafe());
            } else {
                bytesRead = channel.read(getByteBufferArray());
            }

            if (bytesRead < 0) {
                if (getJITAllocateAction()) {
                    getBuffer().release();
                    setBuffer(null);
                    setJITAllocateAction(false);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Sync read throwing IOException");
                }

                throw new IOException("Read failed.  End of data reached.");
            }
        }
        return bytesRead;
    }

    /*
     * @see com.ibm.ws.tcpchannel.internal.TCPReadRequestContextImpl#
     * processAsyncReadRequest()
     */
    @Override
    public VirtualConnection processAsyncReadRequest() {
        return ((NioTCPChannel) getTCPConnLink().getTCPChannel()).getWorkQueueManager().processWork(this, 0);
    }

    /*
     * @see
     * com.ibm.ws.tcpchannel.internal.TCPReadRequestContextImpl#immediateTimeout()
     */
    @Override
    protected void immediateTimeout() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "immediateTimeout");
        }

        // if we do not have a selector, then there has not been a read yet
        // on this connection. If we have a selector, it doesn't mean there's
        // an active read; however, waking up the selector with a false timeout
        // attempt is not optimal but not a big deal
        ChannelSelector sel = ((NioSocketIOChannel) getTCPConnLink().getSocketIOChannel()).getChannelSelectorRead();
        if (null != sel) {
            // selector uses granularity of 1 second, so subtract 2 seconds
            // to guarantee the timeout will fire immediately
            this.timeoutTime = System.currentTimeMillis() - 2000L;
            sel.resetTimeout(this.timeoutTime);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "No read selector, ignoring immediate timeout");
            }
        }

    }

}