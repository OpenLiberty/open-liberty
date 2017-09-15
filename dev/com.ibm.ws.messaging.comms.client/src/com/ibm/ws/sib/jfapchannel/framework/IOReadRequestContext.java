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
public interface IOReadRequestContext
{   
   /**
    * Specifies the buffer into which data will be read.  This is semantically
    * equivalent to invoking setBuffers(new WsByteBuffer[]{buffer}).  This
    * method must only be invoked when no read request is in progress.
    * @param buffer the buffer to use for subsequent read requests.
    */
   void setBuffer(WsByteBuffer buffer);
   
   /**
    * Specifies a set of buffers into which data will be read.  This method must
    * only be invoked when no read request is in progress.
    * @param buffers a set of buffers to use for subsequent read requests.
    */
   void setBuffers(WsByteBuffer[] buffers);
   
   /** 
    * @return the buffer (or first buffer from the set of buffers) associated
    * with this read request.  This is semantically equivalent to invoking
    * getBuffers()[0].
    */
   WsByteBuffer getBuffer();
   
   /**
    * @return the set of buffers associated with this read request.
    */
   WsByteBuffer[] getBuffers();

   /**
    * Request to read data from the network.
    * @param amountToRead the minimum amount of data to read before considering
    * that the request has been satisified.
    * @param completionCallback the callback to notify when the request completes
    * @param forceQueue must the read request be performed on another thread?  When
    * a value of true is specified then the read operation must not block the
    * thread invoking this method.  A value of false allows (but does not require)
    * the implementation to perform read operations using the calling thread.
    * @param timeout the number of milliseconds to wait for enough data to become
    * available to satisify the request.  A value of zero means "return immediately".
    * A timeout manifests itself as a call to the error method of the specified
    * callback - passing a SocketTimeoutException.  Even when a timeout occures it
    * is possible that some data will have been read. 
    * @return a network connection object if a subsequent read operation should be
    * attempted on the same thread - otherwise a value of null is returned.  This is
    * used as a mechanism to avoid the need for recursion if a value of true is
    * supplied to the forceQueue argument and the read request is being performed
    * on the calling thread.
    */
   NetworkConnection read(int amountToRead, 
                          IOReadCompletedCallback completionCallback, 
                          boolean forceQueue, 
                          int timeout);
   
   /**
    * A "wait forever" value for the timeout parameter of the read method.
    */
   final static int NO_TIMEOUT = -1;
}
