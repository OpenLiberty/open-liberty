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
package com.ibm.wsspi.udpchannel;

import java.net.SocketAddress;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * @author mjohnson
 */
public interface UDPBuffer {
    /**
     * Returns the read WsByteBuffer associated with this read request.
     * 
     * @return WsByteBuffer
     */
    WsByteBuffer getBuffer();

    /**
     * Returns the address of the sending client associated with this read request.
     * 
     * @return SocketAddress
     */
    SocketAddress getAddress();

    /**
     * Returns the object back to the object pool.
     */
    void release();

}
