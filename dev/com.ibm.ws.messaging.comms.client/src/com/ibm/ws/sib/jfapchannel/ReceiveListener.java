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
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;

/**
 * Used as a notification that some data has been received from our peer.
 * This is as a result of a previous request which has been sent to and
 * solicitied a response from our peer.
 * @author prestona
 */
public interface ReceiveListener
{
   /**
    * Notification that data was received.
    * @param data The data.
    * @param segmentType   The segment type associated.
    * @param requestNumber The request number associated with this
    *                       transmission at send time.
    * @param priority The priority the data was sent with.
    * @param allocatedFromBufferPool The data received was placed into a buffer
    *                                 allocated from the WS buffer pool.
    * @param partOfExchange A hint to our peer that the data received was sent as the initiating 
    *                       part of an exchange and thus a reply will be expected.
    * @param conversation The conversation associated with the data received.
    */
   void dataReceived(WsByteBuffer data, 
                     int segmentType,
                     int requestNumber,
                     int priority,
                     boolean allocatedFromBufferPool,
                     boolean partOfExchange,                                              // f181007
                     Conversation conversation);
   
   /**
    * Notification that an error occurred when we were expecting to receive
    * a response.  This method is used to "wake up" any conversations using
    * a connection for which an error occurres.  At the point this method is
    * invoked, the connection will already have been marked "invalid".
    * <p>
    * Where this method is implemented in the ConversationReceiveListener
    * interface (which extends this interface) it is used to notify 
    * the per conversation receive listener of (almost) all error conditions
    * encountered on the associated connection.
    * @see ConversationReceiveListener
    * @param exception The exception which occurred.
    * @param segmentType The segment type of the data (-1 if not known)
    * @param requestNumber The request number associated with the failing
    *                       request (-1 if not known)
    * @param priority The priority associated with the failing request
    *                  (-1 if not known).
    * @param conversation The conversation (null if not known)
    */
   void errorOccurred(SIConnectionLostException exception,                                // F174602 
                      int segmentType, 
                      int requestNumber,
                      int priority,
                      Conversation conversation);
                      
}
