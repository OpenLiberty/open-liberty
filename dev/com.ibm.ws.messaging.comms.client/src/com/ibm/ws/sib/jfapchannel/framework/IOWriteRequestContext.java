/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.framework;

import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;

/**
 * Provides contextual information for a request to read data from the network.
 * Users of this package would typically obtain an implementation of this
 * interface by invoking the getReadInterface method from an implementation
 * of IOConnectionContext.
 * 
 * @see com.ibm.ws.sib.jfapchannel.framework.IOConnectionContext
 */
public interface IOWriteRequestContext
{
   /**
    * Specifies the buffer from which data will be written.  This is semantically
    * equivalent to invoking setBuffers(new WsByteBuffer[]{buffer}).  This
    * method must only be invoked when no write request is in progress.
    * @param buffer the buffer to use for subsequent write requests.
    */
   void setBuffer(WsByteBuffer buffer);
   
   /**
    * Specifies a set of buffers from which data will be written.  This method must
    * only be invoked when no wrote request is in progress.
    * @param buffers a set of buffers to use for subsequent write requests.
    */
   void setBuffers(WsByteBuffer[] buffers);
   
   /** 
    * @return the buffer (or first buffer from the set of buffers) associated
    * with this write request.  This is semantically equivalent to invoking
    * getBuffers()[0].
    */
   WsByteBuffer getBuffer();
   
   /**
    * @return the set of buffers associated with this write request.
    */
   WsByteBuffer[] getBuffers();
   
   /**
    * Request to write data to the network.
    * @param amountToWrite the minimum amount of data to write before considering
    * that the request has been satisified.
    * @param completionCallback the callback to notify when the request completes
    * @param forceQueue must the write request be performed on another thread?  When
    * a value of true is specified then the write operation must not block the
    * thread invoking this method.  A value of false allows (but does not require)
    * the implementation to perform write operations using the calling thread.
    * @param timeout the number of milliseconds to wait for enough data to be written
    * such that the request is considered satisified.  A value of zero means 
    * "return immediately".  A timeout manifests itself as a call to the error method 
    * of the specified callback - passing a SocketTimeoutException.  Even when a timeout 
    * occures it is possible that some data will have been written. 
    * @return a network connection object if a subsequent write operation should be
    * attempted on the same thread - otherwise a value of null is returned.  This is
    * used as a mechanism to avoid the need for recursion if a value of true is
    * supplied to the forceQueue argument and the write request is being performed
    * on the calling thread.
    */
   NetworkConnection write(int amountToWrite, 
                           IOWriteCompletedCallback completionCallback, 
                           boolean queueRequest, 
                           int timeout);
   
   /**
    * Constant meaning "all data in the buffers" which may be specified as the
    * "amountToWrite" argument of the write method. 
    */
   final int WRITE_ALL_DATA = 0;
   
   /**
    * Constant meaning "wait forever" which may be specified as the "timeout"
    * argument of the write method.
    */
   final int NO_TIMEOUT = -1;
}
