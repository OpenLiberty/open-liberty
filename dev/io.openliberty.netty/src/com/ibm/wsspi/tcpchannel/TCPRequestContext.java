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
package com.ibm.wsspi.tcpchannel;

import com.ibm.wsspi.bytebuffer.WsByteBuffer;

/**
 * A context object encapsulating all of the data related to a TCPChannel
 * read data request. This context can be obtained via the getReadInterface
 * method on the TCPConnectionContext.
 * 
 * @ibm-spi
 */

public interface TCPRequestContext {

    /**
     * Link back to the TCPConnectionContext from whence this interface came.
     * 
     * @return TCPConnectionContext - originating context
     */
    TCPConnectionContext getInterface();

    /**
     * Calls the clear method of all read buffers associated with this
     * request - as per the documentation for the clear method on the
     * ByteBuffer class.
     */
    void clearBuffers();

    /**
     * Returns the set of read buffers associated with this
     * request.
     * 
     * @return WsByteBuffer[]
     */
    WsByteBuffer[] getBuffers();

    /**
     * Sets the array of read buffers associated with this request.
     * 
     * @param bufs
     *            - the array of WsByteBuffers to be set
     */
    void setBuffers(WsByteBuffer[] bufs);

    /**
     * Returns the first read buffer in the buffer array associated with this
     * request.
     * This is a convenience method for when only one buffer is needed.
     * 
     * @return WsByteBuffer
     */
    WsByteBuffer getBuffer();

    /**
     * Sets the first read buffer in the buffer array associated with this
     * request. This is
     * a convenience method for when only one buffer is needed
     * 
     * @param buf
     */
    void setBuffer(WsByteBuffer buf);

    /**
     * A special value for the timeout parm used on the request calls.
     * Specifying this value will cause the TCPChannel to use the default
     * timeout value that is configured for the channel for timing out this
     * request.
     */
    int USE_CHANNEL_TIMEOUT = 0;

    /**
     * A special value for the timeout parm used on the request calls.
     * Specifying this value will cause the TCPChannel to not timeout this
     * request.
     */
    int NO_TIMEOUT = -1;

    /**
     * A special value for the timeout parm.
     * Specifying this value will cause the TCPChannel to attempt to immediately
     * timeout
     * the previous read or write request. Therefore, the previous request should
     * return complete or return with a timeout error.
     * The request which passes this value will not attempt to do a read or write,
     * but
     * will only attempt to immediately timeout the previous request. Therefore
     * the
     * request which passes this value will always return immediately. If this
     * value is not supported by an implementation of this interface, then an
     * IllegalArgumentException will be thrown when this value is used.
     */
    int IMMED_TIMEOUT = -2;

    /**
     * A special value that will both immediately timeout IO attempts and will
     * also attempt to block future IO attempts. That is a best-effort at blocking
     * future IO with very small timing windows if the IO is being requested by
     * another thread at the same time as this abort attempt.
     */
    int ABORT_TIMEOUT = -3;
}
