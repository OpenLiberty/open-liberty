/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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

import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.udpchannel.UDPBuffer;

/**
 * @author mjohnson
 */
public class UDPBufferImpl implements UDPBuffer {
    private WsByteBuffer buffer = null;
    private SocketAddress address = null;
    private UDPBufferFactory udpBufferFactory = null;

    /**
     * Constructor.
     */
    public UDPBufferImpl() {
        // do nothing
    }

    /**
     * Constructor.
     * 
     * @param factory
     */
    public UDPBufferImpl(UDPBufferFactory factory) {
        this.udpBufferFactory = factory;
    }

    /*
     * @see com.ibm.wsspi.udpchannel.UDPBuffer#getBuffer()
     */
    @Override
    public WsByteBuffer getBuffer() {
        return this.buffer;
    }

    /*
     * @see com.ibm.wsspi.udpchannel.UDPBuffer#getAddress()
     */
    @Override
    public SocketAddress getAddress() {
        return this.address;
    }

    protected void set(WsByteBuffer buffer, SocketAddress address) {
        this.buffer = buffer;
        this.address = address;
    }

    /*
     * @see com.ibm.wsspi.udpchannel.UDPBuffer#release()
     */
    @Override
    public void release() {
        if (udpBufferFactory != null) {
            udpBufferFactory.release(this);
        }
    }

    /**
     * Clear the contents of this buffer.
     */
    public void clear() {
        this.buffer = null;
        this.address = null;
    }
}
