/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
import com.ibm.wsspi.channelfw.objectpool.CircularObjectPool;
import com.ibm.wsspi.channelfw.objectpool.ObjectPool;

/**
 * Factory for creating and pooling UDPBuffer objects.
 */
public class UDPBufferFactory {
    private static TraceComponent tc = Tr.register(UDPBufferFactory.class, UDPMessages.TR_GROUP, UDPMessages.TR_MSGS);

    private static UDPBufferFactory ofInstance = null;
    private static final int OBJ_SIZE = 100;

    private static final ObjectPool udpBufferObjectPool = new CircularObjectPool(OBJ_SIZE);

    /**
     * Get a reference to the singleton instance of this class.
     * 
     * @return UDPBufferFactory
     */
    public static UDPBufferFactory getRef() {
        if (null == ofInstance) {
            synchronized (UDPBufferFactory.class) {
                if (null == ofInstance) {
                    ofInstance = new UDPBufferFactory();
                }
            }
        }
        return ofInstance;
    }

    /**
     * Get a UDPBuffer from the pool, or create one if necessary.
     * 
     * @return UDPBufferImpl
     */
    public static UDPBufferImpl getUDPBuffer() {
        return getRef().getUDPBufferImpl();
    }

    /**
     * Get a UDPBuffer that will encapsulate the provided information.
     * 
     * @param buffer
     * @param address
     * @return UDPBufferImpl
     */
    public static UDPBufferImpl getUDPBuffer(WsByteBuffer buffer, SocketAddress address) {
        UDPBufferImpl udpBuffer = getRef().getUDPBufferImpl();
        udpBuffer.set(buffer, address);
        return udpBuffer;
    }

    /**
     * Private constructor, use the getRef() api for access.
     */
    private UDPBufferFactory() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created UDPBuffer factory; " + this);
        }
    }

    /**
     * Retrieve an UDPBuffer object from the factory.
     * 
     * @return UDPBufferImpl
     */
    protected UDPBufferImpl getUDPBufferImpl() {
        UDPBufferImpl ret = (UDPBufferImpl) udpBufferObjectPool.get();
        if (ret == null) {
            ret = new UDPBufferImpl(this);
        }
        return ret;
    }

    /**
     * Return a UDPBuffer object to the factory for pooling.
     * 
     * @param buffer
     */
    protected void release(UDPBufferImpl buffer) {
        buffer.clear();
        udpBufferObjectPool.put(buffer);
    }

}
