/*******************************************************************************
 * Copyright (c) 1997, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.h2internal;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPReadCompletedCallback;
import com.ibm.wsspi.tcpchannel.TCPReadRequestContext;

/**
 *
 */
public class H2TCPReadRequestContext implements TCPReadRequestContext {
    H2TCPConnectionContext h2TcpConnectionContext = null;
    Integer streamID = null;
    H2InboundLink muxLink = null;

    private WsByteBuffer[] buffers;
    private ByteBuffer byteBufferArray[] = null;
    private ByteBuffer byteBufferArrayDirect[] = null;

    // define reusable arrrays of most common sizes
    private ByteBuffer byteBufferArrayOf1[] = null;
    private ByteBuffer byteBufferArrayOf2[] = null;
    private ByteBuffer byteBufferArrayOf3[] = null;
    private ByteBuffer byteBufferArrayOf4[] = null;
    private final WsByteBuffer[] defaultBuffers = new WsByteBuffer[1];

    private final boolean jitAllocateAction = false;
    private int jitAllocateSize = 0;

    private static final TraceComponent tc = Tr.register(H2TCPReadRequestContext.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    public H2TCPReadRequestContext(Integer id, H2InboundLink link, H2TCPConnectionContext x) {
        streamID = id;
        muxLink = link;
        h2TcpConnectionContext = x;
    }

    @Override
    public VirtualConnection read(long numBytes, TCPReadCompletedCallback callback, boolean forceQueue, int timeout) {
        VirtualConnection vc = null;
        final H2StreamProcessor p = muxLink.getStreamProcessor(streamID);
        final long nB = numBytes;
        final TCPReadCompletedCallback cb = callback;
        if (jitAllocateSize > 0 && getBuffers() == null) {
            WsByteBuffer buf = ChannelFrameworkFactory.getBufferManager().allocate(jitAllocateSize);
            this.setBuffer(buf);
        }

        if (forceQueue != true) {
            vc = p.read(numBytes, this.getBuffers());
        }

        // for now I am going to kludge up the async reading, if vc is null, then to go async we will start another thread and have it poll the stream
        if (vc == null) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    doWork(nB, cb, p);
                }
            };
            new Thread(r).start();
        }

        return vc;
    }

    void doWork(long numBytes, TCPReadCompletedCallback callback, H2StreamProcessor p) {
        VirtualConnection vc = null;
        if (jitAllocateSize > 0 && getBuffers() == null) {
            WsByteBuffer buf = ChannelFrameworkFactory.getBufferManager().allocate(jitAllocateSize);
            this.setBuffer(buf);
        }
        while (true) {
            try {
                Thread.sleep(250);
                vc = p.read(numBytes, this.getBuffers());
                if (vc != null) {
                    callback.complete(vc, this);
                    return;
                }
            } catch (InterruptedException e) {

            }
        }
    }

    @Override
    public long read(long numBytes, int timeout) throws IOException {

        long readCount = 0;
        H2StreamProcessor p = muxLink.getStreamProcessor(streamID);

        while (true) {
            try {
                readCount = p.readCount(numBytes, this.getBuffers());
                if ((readCount != 0) || (numBytes == 0)) {
                    return readCount;
                }

                Thread.sleep(250);
            } catch (InterruptedException e) {

            }
        }
    }

    @Override
    public TCPConnectionContext getInterface() {
        return h2TcpConnectionContext;
    }

    @Override
    public void clearBuffers() {
        if (null != this.buffers) {
            for (int i = 0; i < this.buffers.length; i++) {
                this.buffers[i].clear();
            }
        }
    }

    @Override
    public WsByteBuffer[] getBuffers() {
        return this.buffers;
    }

    @Override
    public void setBuffers(WsByteBuffer[] bufs) {
        this.buffers = bufs;

        // reset arrays to free memory quicker. defect 457362
        if (this.byteBufferArray != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArray.length; i++) {
                this.byteBufferArray[i] = null;
            }
        }

        if (this.byteBufferArrayDirect != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArrayDirect.length; i++) {
                this.byteBufferArrayDirect[i] = null;
            }
            this.byteBufferArrayDirect = null;
        }

        if (bufs != null) {
            int numBufs;
            // reuse an existing byteBufferArray if one was already created
            // kind of hokey, but this allows us to avoid construction of a
            // new array object unless absolutely neccessary

            // following loop will count the number of buffers in
            // the input array rather than relying on the array length
            for (numBufs = 0; numBufs < bufs.length; numBufs++) {
                if (bufs[numBufs] == null) {
                    break;
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setBuffers number of buffers is " + numBufs);
            }

            if (numBufs == 1) {
                if (this.byteBufferArrayOf1 == null) {
                    this.byteBufferArrayOf1 = new ByteBuffer[1];
                }
                this.byteBufferArray = this.byteBufferArrayOf1;
            } else if (numBufs == 2) {
                if (this.byteBufferArrayOf2 == null) {
                    this.byteBufferArrayOf2 = new ByteBuffer[2];
                }
                this.byteBufferArray = this.byteBufferArrayOf2;
            } else if (numBufs == 3) {
                if (this.byteBufferArrayOf3 == null) {
                    this.byteBufferArrayOf3 = new ByteBuffer[3];
                }
                this.byteBufferArray = this.byteBufferArrayOf3;
            } else if (numBufs == 4) {
                if (this.byteBufferArrayOf4 == null) {
                    this.byteBufferArrayOf4 = new ByteBuffer[4];
                }
                this.byteBufferArray = this.byteBufferArrayOf4;

            } else {
                // more than 4 buffers in request, allocate array as needed
                this.byteBufferArray = new ByteBuffer[numBufs];
            }

            if (numBufs > 1) {
                for (int i = 0; i < numBufs; i++) {
                    this.byteBufferArray[i] = bufs[i].getWrappedByteBufferNonSafe();
                }
            } else if (numBufs == 1) {
                this.byteBufferArray[0] = bufs[0].getWrappedByteBufferNonSafe();
            }

        } else {
            // buffers == null, so set byteBufferArray to null also
            this.byteBufferArray = null;
        }
    }

    @Override
    public WsByteBuffer getBuffer() {
        if (this.buffers == null) {
            return null;
        }
        return this.buffers[0];
    }

    @Override
    public void setBuffer(WsByteBuffer buf) {

        //this.missedSet = false;

        // reset arrays to free memory quicker. defect 457362
        if (this.byteBufferArray != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArray.length; i++) {
                this.byteBufferArray[i] = null;
            }
        }
        if (this.byteBufferArrayDirect != null) {
            // reset references
            for (int i = 0; i < this.byteBufferArrayDirect.length; i++) {
                this.byteBufferArrayDirect[i] = null;
            }
            this.byteBufferArrayDirect = null;
        }
        this.defaultBuffers[0] = null; // reset reference

        if (buf != null) {
            this.buffers = this.defaultBuffers;
            this.buffers[0] = buf;

            if (this.byteBufferArrayOf1 == null) {
                this.byteBufferArrayOf1 = new ByteBuffer[1];
            }
            this.byteBufferArray = this.byteBufferArrayOf1;
            this.byteBufferArray[0] = buf.getWrappedByteBufferNonSafe();
        } else {
            this.buffers = null;
            this.byteBufferArray = null;
        }

    }

    @Override
    public void setJITAllocateSize(int numBytes) {
        this.jitAllocateSize = numBytes;
    }

    @Override
    public boolean getJITAllocateAction() {
        return this.jitAllocateAction;
    }

}
