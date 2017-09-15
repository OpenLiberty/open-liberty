/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.impl.interfaces;

// Import required classes.
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * @author tevans
 */
/**
 * An interface class for the different types of inpt class.
 */

public interface UpstreamControl
{

  /**
   * Send a nack message upstream.
   * 
   * @param  startTick The start tick for the nack.
   * @param  endTick The end tick for the nack.
   * @param  priority The priority within the stream that the nack refers to.
   * @param  reliability The reliability within the stream that the nack refers to.
   * @param  stream The stream ID that this nack refers to.  Tihs field is also
   * used to determine the cellule where the message will be sent.
   */
  public void sendNackMessage(SIBUuid8   source,
                              SIBUuid12 destUuid,
                              SIBUuid8  busUuid,  
                              long startTick,
                              long endTick,
                              int priority,
                              Reliability reliability,
                              SIBUuid12 streamID)
    throws SIResourceException;

  /**
   * Send a nack message upstream.
   * 
   * @param  startTick The start tick for the nack.
   * @param  endTick The end tick for the nack.
   * @param  priority The priority within the stream that the nack refers to.
   * @param  reliability The reliability within the stream that the nack refers to.
   * @param  stream The stream ID that this nack refers to.  Tihs field is also
   * used to determine the cellule where the message will be sent.
   */
  public long sendNackMessageWithReturnValue(SIBUuid8   source,
                              SIBUuid12 destUuid,
                              SIBUuid8  busUuid,  
                              long startTick,
                              long endTick,
                              int priority,
                              Reliability reliability,
                              SIBUuid12 streamID)
    throws SIResourceException;
  

  /**
   * Send an ack message upstream.
   * 
   * @param ackPrefix The prefix for the ack.
   * @param priority The priority within the stream that the ack refers to.
   * @param reliability The reliability within the stream that the ack refers to.
   * @param stream The stream ID that this ack refers to.  This field is also used
   * to determine the cellule where the message will be sent.
   * @throws SIResourceException
   */
  public void sendAckMessage(SIBUuid8   source,
                             SIBUuid12 destUuid,
                             SIBUuid8  busUuid,  
                             long ackPrefix,
                             int priority,
                             Reliability reliability,
                             SIBUuid12 streamID,
                             boolean consolidate)
    throws SIResourceException;

  public void sendAreYouFlushedMessage(SIBUuid8   source,
                                       SIBUuid12 destUuid,
                                       SIBUuid8  busUuid,  
                                       long queryID,
                                       SIBUuid12 streamID)
    throws SIResourceException;
  
  public void sendRequestFlushMessage(SIBUuid8   source,
                                      SIBUuid12 destUuid,
                                      SIBUuid8  busUuid,  
                                      long queryID,
                                      SIBUuid12 streamID,
                                      boolean indoubtDiscard)
      throws SIResourceException;

}
