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

import java.io.IOException;

import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * A context object encapsulating all of the data related to a TCPChannel
 * write data request. This context can be otained via the getWriteInterface
 * method on the TCPConnectionContext.
 * 
 * @ibm-spi
 */

public interface TCPWriteRequestContext extends TCPRequestContext {

    /**
     * Performs writes on the connection until at least the specified number of
     * bytes have been written.
     * This call is always synchronous, and will result in blocking the thread
     * until the
     * data has been written. A numBytes value of 0 will cause the write to return
     * immediately. Upon completion of the write the WsByteBuffer(s) position will
     * be
     * set to the end of the data the was written. If timeout is set equal to
     * IMMED_TIMEOUT,
     * then an attempt to immediately timeout the previous write will be made,
     * and this write will return 0.
     * 
     * @param numBytes
     *            - the minimum number of bytes to write, Max value for numBytes is
     *            2147483647
     * @param timeout
     *            - timeout value to associate with this request (milliseconds)
     * @return long - number of bytes written
     * @throws IOException
     */
    long write(long numBytes, int timeout) throws IOException;

    /**
     * Performs write on the connection until at least the specified number of
     * bytes have been written.
     * If the write can be done immediately then the VirtualConnection object is
     * passed back,
     * after the data has been written. If the data can not be written
     * immediately, then null is returned,
     * the request is queued, and when the data has been written the TCP Channel
     * will call the registered TCPWriteCompletedCallback.
     * Upon completion of the write the WsByteBuffer(s) position will be
     * set to the end of the data the was written.
     * This is the preferred method
     * for writing data, as it never results in blocking the thread while waiting
     * for data.
     * The callback may or may not be invoked, if invoked it will be invoked on a
     * different thread.
     * If the data is written immediately, then the VirtualConnection that is
     * returned will be the
     * same VirtualConnection that applied to this TCPWriteRequestContext before
     * the writeAsynch was
     * called, it is returned as a convienince for the calling code to invoke the
     * callback complete
     * method in the same way the TCP Channel would have invoked it.
     * If timeout is set equal to IMMED_TIMEOUT, then an attempt to immediately
     * timeout
     * the previous write will be made, and this write will return null.
     * 
     * @param numBytes
     *            - the minimum number of bytes to write, must be > 0, Max value for
     *            numBytes is 2147483647
     * @param callback
     *            - an implementation of the TCPWriteCompletedCallback class
     * @param forceQueue
     *            - force request to be queued and callback called from another
     *            thread
     * @param timeout
     *            - timeout value to associate with this request (milliseconds)
     * @return VirtualConnection - if at least numBytes were written immediately,
     *         null if numBytes
     *         of data were not written immediately and therefore the callback
     *         will be invoked.
     */
    VirtualConnection write(long numBytes, TCPWriteCompletedCallback callback, boolean forceQueue, int timeout);

    /**
     * A special value for the numBytes parm used on the write method calls.
     * Specifying this value will cause the TCPChannel to write
     * all of the data in the buffer(s).
     */
    long WRITE_ALL_DATA = -1;

}
