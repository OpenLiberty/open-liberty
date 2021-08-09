/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jtc.adapter;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Interface defining methods used to access internals of the
 * Java class libraries.
 */
public interface IPlatformAdapter {
    /**
     * Get the native address for the specified DirectByteBuffer
     * 
     * @param byteBuffer
     *            Reference to a DirectByteBuffer
     * 
     * @return The native address of the specified DirectByteBuffer
     * 
     * @throws IllegalArgumentException
     *             If the specified buffer is not direct
     */
    long getByteBufferAddress(ByteBuffer byteBuffer);

    /**
     * Get the socket channel handle.
     * 
     * @param socketChannel
     *            The socket channel to get the handle from.
     * 
     * @return The handle for the specified socket channel
     */
    long getSocketChannelHandle(SocketChannel socketChannel);

    /**
     * Clean up the thread locals for the specified Thread.
     * 
     * @param thread
     *            The thread to clean up
     */
    void cleanThreadLocals(Thread thread);
}
