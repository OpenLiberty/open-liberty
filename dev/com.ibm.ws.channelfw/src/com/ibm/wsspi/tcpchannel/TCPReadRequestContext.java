/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.tcpchannel;

import java.io.IOException;

import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * A context object encapsulating all of the data related to a TCPChannel
 * read data request. This context can be otained via the getReadInterface
 * method on the TCPConnectionContext.
 * 
 * @ibm-spi
 */

public interface TCPReadRequestContext extends TCPRequestContext {

    /**
     * Performs reads on the connection until at least the specified number of
     * bytes have been read.
     * This call is always synchronous, and will result in blocking the thread
     * until the
     * minimum number of bytes has been read. A numBytes value of 0 will cause the
     * read to return
     * immediately. Upon completion of the read, WsByteBuffer(s) position will be
     * set
     * to the end of the data. If timeout is set equal to IMMED_TIMEOUT, then an
     * attempt
     * to immediately timeout the previous read will be made, and this read will
     * return 0.
     * 
     * @param numBytes
     *            - minimum number of bytes to read. Max value for numBytes is
     *            2147483647
     * @param timeout
     *            - timeout value to associate with this request (milliseconds)
     * @return long - number of bytes read
     * @throws IOException
     */
    long read(long numBytes, int timeout) throws IOException;

    /**
     * Performs reads on the connection until at least the specified number of
     * bytes have been read.
     * If the read can be done immediately then the VirtualConnection object is
     * passed back,
     * and the data is ready for use by the application. if the data can not be
     * read immediately,
     * then null is returned, the request will be queued, and data will be
     * available when the TCP Channel
     * calls the registered TCPReadCompletedCallback.
     * Upon completion of the read, WsByteBuffer(s) position will be set
     * to the end of the data. This is the preferred method
     * for reading data, as it never results in blocking the thread while waiting
     * for data.
     * The callback may or may not be invoked, if invoked it will be invoked on a
     * different thread.
     * If the data is read immediately, then the VirtualConnection that is
     * returned will be the
     * same VirtualConnection that applied to this TCPReadRequestContext before
     * the readAsynch is
     * called, it is return as a convenience for the calling code to invoke the
     * callback complete
     * method in the same way the TCP Channel would have invoked it.
     * If you don't know how many bytes to expect on a read, you should pass
     * either a '1' or some minimum number of bytes you know you will always
     * need. If timeout is set equal to IMMED_TIMEOUT, then an attempt
     * to immediately timeout the previous read will be made, and this read will
     * return null.
     * 
     * @param numBytes
     *            - the minimum number of bytes to read, must be > 0. Max value for
     *            numBytes is 2147483647
     * @param callback
     *            - an implementation of the TCPReadCompletedCallback class
     * @param forceQueue
     *            - force request to be queued and callback called from another
     *            thread
     * @param timeout
     *            - timeout value to associate with this request (milliseconds)
     * @return VirtualConnection - if at least numBytes were read immediately,
     *         null if numBytes
     *         of data were not read immediately and therefore the callback will
     *         be invoked.
     */
    VirtualConnection read(long numBytes, TCPReadCompletedCallback callback, boolean forceQueue, int timeout);

    /**
     * Set the size of the buffer that the TCP Channel should allocate
     * on the applciations behalf when data is to be read.
     * 
     * @param numBytes
     *            - the number of bytes to allocate to hold the read data
     */
    void setJITAllocateSize(int numBytes);

    /**
     * Returns a flag denoting if a buffer was allocate on THIS request.
     * If a buffer was allocated, it is the responsibility of the application,
     * NOT THE TCP CHANNEL, to release the buffer when the buffer is no
     * longer needed. Failure to release the buffer will cause memory leaks.
     * 
     * @return boolean - True if buffer was allocated by the TCP channel for
     *         the current read request, False if the buffre was not allocated by
     *         the
     *         TCP channel for the current read request.
     */
    boolean getJITAllocateAction();

}
