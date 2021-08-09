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
package com.ibm.ws.sib.jfapchannel;

import com.ibm.ws.sib.jfapchannel.buffer.WsByteBuffer;

/**
 * An "unpacked" version of the data received back from an exchange call.
 * This is required so that the caller of exchange can recover information
 * like segment type from the call.
 * @author prestona
 */
public interface ReceivedData
{
   /**
    * The data received.
    * @return ByteBuffer
    */
   WsByteBuffer getBuffer();
   
   /**
    * The segment type of the data received.
    * @return int
    */
   int getSegmentType();
   
   /**
    * The request identifier of the data received.
    * @return int
    */
   int getRequestId();
   
   /**
    * The priority of the data received.
    * @return int
    */
   int getPriority();
   
   /**
    * Returns true iff the buffer being passed back as part of this
    * received data object was allocated from a buffer pool or not
    * @return boolean
    */
   boolean getAllocatedFromBufferPool();
   
   /**
    * Release the received data back to its underlying pool.
    */
   void release();
}
