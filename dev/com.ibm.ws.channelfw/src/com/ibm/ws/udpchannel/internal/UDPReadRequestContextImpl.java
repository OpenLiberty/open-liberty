/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.udpchannel.internal;

import java.net.SocketAddress;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.udpchannel.UDPBuffer;
import com.ibm.wsspi.udpchannel.UDPReadCompletedCallback;
import com.ibm.wsspi.udpchannel.UDPReadCompletedCallbackThreaded;
import com.ibm.wsspi.udpchannel.UDPReadRequestContext;

/**
 * UDP read specific context object for interaction with UDP channel users.
 * 
 * @author mjohnson
 */
public class UDPReadRequestContextImpl extends UDPRequestContextImpl implements UDPReadRequestContext {
    private static final TraceComponent tc = Tr.register(UDPReadRequestContextImpl.class, UDPMessages.TR_GROUP, UDPMessages.TR_MSGS);

    private boolean readCalled = false;
    private UDPReadCompletedCallback readCallback = null;
    private UDPReadCompletedCallbackThreaded readAlwaysCallback = null;
    private boolean bIsForceQueue = false;
    private UDPBufferImpl udpBuffer = UDPBufferFactory.getUDPBuffer();

    /**
     * Constructor.
     * 
     * @param udpContext
     * @param wqm
     */
    public UDPReadRequestContextImpl(UDPConnLink udpContext, WorkQueueManager wqm) {
        super(udpContext, wqm);
    }

    /*
     * @see
     * com.ibm.websphere.udp.channel.UDPReadRequestContext#read(com.ibm.websphere
     * .udp.channel.UDPReadCompletedCallback, boolean)
     */
    public VirtualConnection read(UDPReadCompletedCallback callback, boolean forceQueue) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "read(force=" + forceQueue + ")");
        }

        this.readCallback = callback;
        this.readCalled = true;
        this.bIsForceQueue = forceQueue;

        VirtualConnection vc = getWorkQueueManager().processWork(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "read(): " + vc);
        }

        return vc;
    }

    /*
     * @see
     * com.ibm.websphere.udp.channel.UDPReadRequestContext#readAlways(com.ibm.
     * websphere.udp.channel.UDPReadCompletedCallbackThreaded, boolean)
     */
    public void readAlways(UDPReadCompletedCallbackThreaded callback, boolean enable) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "readAlways(enable=" + enable + ")");
        }
        if (enable) {
            this.readAlwaysCallback = callback;
        } else {
            this.readAlwaysCallback = null;
        }

        getWorkQueueManager().processWork(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "readAlways()");
        }
    }

    /*
     * @see com.ibm.websphere.udp.channel.UDPReadRequestContext#getUDPBuffer()
     */
    public UDPBuffer getUDPBuffer() {
        this.readCalled = false;
        return this.udpBuffer;
    }

    //
    // Returns true for if it was able to set the buffer, false otherwise.
    //
    protected boolean setBuffer(WsByteBuffer buffer, SocketAddress address, boolean firstPacket) {
        boolean returnValue = true;

        if (readCalled || firstPacket || isReadAlwaysCalled()) {
            udpBuffer.set(buffer, address);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setBuffer called with " + buffer + " address " + address);
            }
        } else {
            returnValue = false;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setBuffer called but no read was called.");
            }
        }

        return returnValue;
    }

    protected void complete(UDPBuffer buffer) {
        if (isReadAlwaysCalled()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Calling multi-threaded read callback.");
            }
            this.readAlwaysCallback.complete(getConnLink().getVirtualConnection(), buffer);
        }
    }

    protected void complete() {
        if (isReadAlwaysCalled()) {
            UDPBufferImpl buffer = UDPBufferFactory.getUDPBuffer();
            buffer.set(udpBuffer.getBuffer(), udpBuffer.getAddress());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Calling multi-threaded read callback.");
            }
            this.readAlwaysCallback.complete(getConnLink().getVirtualConnection(), buffer);
        } else {
            if (this.readCallback != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Calling read callback.");
                }
                this.readCallback.complete(getConnLink().getVirtualConnection(), this);

            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No read callback set");
                }
            }
        }
    }

    /**
     * @return Returns the forceQueue.
     */
    public boolean isForceQueue() {
        return this.bIsForceQueue;
    }

    protected boolean isReadAlwaysCalled() {
        return null != this.readAlwaysCallback;
    }

    /*
     * @see com.ibm.ws.udp.channel.internal.UDPRequestContextImpl#isRead()
     */
    @Override
    public boolean isRead() {
        return true;
    }
}
