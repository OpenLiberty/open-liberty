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
package com.ibm.ws.sib.processor.runtime;

import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPInvalidRuntimeIDException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;

/**
 * Interface to view and manipulate the message requests that we have 
 * received from a remote messaging engine.
 * 
 * This transmit flow is for controlling outgoing, requested messages as 
 * oppopsed to outgoing transmitted messages.
 */
public interface SIMPRemoteConsumerTransmitControllable extends SIMPDeliveryTransmitControllable
{
    
  /**
   * Provides an iterator over all of the requests for messages 
   * that have come in to this queue.
   *
   * @return  SIMPIterator over the request messages in the stream. The iterator contains a set of 
   *          requests for remote messages in ascending tick order starting with the oldest.
   *          The iterator gives up SIMPTransmitMessageRequestControllable objects
   */
  SIMPIterator getTransmitMessageRequestIterator();
  
  /**
   * Get an iterator over all of the messages on this local queue.
   * @throws SIMPRuntimeOperationFailedException
   * @return an iterator containing SIMPQueuedMessageControllable objects.
   */
  SIMPIterator getQueuedMessageIterator() throws SIMPRuntimeOperationFailedException;
  
  /**
   * Get a message based on the ID
   * 
   * @param ID
   * @return SIMPQueuedMessageControllable
   * @throws SIMPInvalidRuntimeIDException
   * @throws SIMPControllableNotFoundException
   * @throws SIMPException
   */
  SIMPQueuedMessageControllable getQueuedMessageByID(String ID)
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException;

  /**
   * Get the number of queued messages that are currently being sent to the
   * requesting messaging engine.
   * @return int
   */
  int getNumberOfQueuedMessages();  
  
  
  
  /** 
   * Get the number of requests for messages that have been satisfied
   * by this ME since it booted.
   * @return long
   */
  long getNumberOfCompletedRequests();
  
  /**
   * @return a long for the number of message requests that have been received
   * from this particular remote messaging engine.
   */
  long getNumberOfRequestsReceived();

  /**
   * Anycast:
   *  If the source has been restored from a backup it may have restored previously
   *   processed messages onto its queues. If the administrator wants to avoid resending 
   *   duplicates he must first deal with the restored duplicate messages.
   *   The streams should however be flushed to unlock any locked messages and allow 
   *   progress on the new stream. 
   *   If the RME is complete past the tick stored in a locked message it will reject 
   *   the message causeing the DME to unlock and potentially duplicate it.
   *
   * Unicast/PubSub:
   *  If the source is restored from a backup and we don't want to risk loosing future 
   *   messages because the target has already acknowledged a higher tick. 
   *   We  need to transmit and flush the existing stream and start a new one 
   *   for new messages.  The target will discard existing messages if it has 
   *   already seen them. Messages created after the backup was taken but not 
   *   transmitted will certainly be lost. If the source is already flushing, restart 
   *   its timers to make retry attempts.
   *   This is performed on all streams in the stream set independently.
   *   The request to do this is hardened and will complete after a restart if
   *   necessary. This presumes that we can start the ME in a safe mode where no new producers are
   *   allowed until a new stream is created. However we may receive the request to flush from 
   *   a target stream so, if there are attached producers the request is hardened and completes 
   *   when the last producer detaches from the stream. Any existing and new producers will be 
   *   attached to a new instance of the stream, non indoubt messages will be 
   *   reallocated to the new stream.   
   *
   * @throws invalidStreamTypeException  if the stream is not a source stream.
   *
   */
  void forceFlushAtSource(); //throws InvalidStreamTypeException;

  /**
   *
   * Anycast:
   *  If the target RME has been deleted, you need to get any locked messages unlocked
   *   and possibly removed. 
   *   Use INDOUBT_LEAVE to keep the unlocked messages and hence risk duplicating them.
   *   Use INDOUBT_DELETE to clear the messages and risk loosing them.  
   *              
   * Unicast PubSub:
   *  If the target system has been deleted calling forceFlushAtsource will not be 
   *   sufficient as the indoubt messages will never be cleared.  
   *   This routine will reallocate the not indoubt messages and then deal with the
   *   idoubts according to the indoubtAction. It will then perform the
   *   forceFlushAtSource processing.
   * 
   * @param indoubtAction  determines how indoubt messages are handled. 
   *             INDOUBT_DELETE causes indoubt messages to be discarded, risking their loss. 
   *             INDOUBT_LEAVE means no action is taken for indoubt messages, so that 
   *             the target system must recover for progress to be made. 
   *
   * @throws invalidStreamTypeException if the stream is not a source stream.
   *
   */
  void clearMessagesAtSource(IndoubtAction indoubtAction) //throws InvalidStreamTypeException;
    throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException;
    
  /**
   * All of the streams in a set are the same type.
   * @deprecated This is of no use in the new hierachy
   * @return DeliveryStreamType  The type of stream, source target p2p etc.
   */
  DeliveryStreamType getType(); 
  
  /**
   * Indicates whether the workload manager has made a decision about where to deliver the messages in
   *  the stream or whether they are just guesses.
   *
   * @deprecated: this value has been maintained for compatibility 
   * @return boolean  True if the messages in the stream are guesses, that is the workload manager did 
   *         not explicitly decide they should go to the localisation by the stream. 
   *         False if the messages are not guesses and the Workload manager has made a final choice
   *         about sending them to the chosen localisation.
   */
  boolean containsGuesses()
    throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException;

  /**
   * The number of requests that are waiting to be assigned messages.
   * @return
   * @author tpm
   */
  long getNumberOfCurrentRequests();  
  
  /**
   * Determines whether this remote get stream is used for gathering messages from multiple MEs.
   */
  boolean isGathering();
  
  /**
   * Obtains the destination/alias uuid that this stream is retrieving/sending messages
   * from/to. If gathering is on, the field may show an alias which identifies a subset
   * of the destination's MEs from which to gather from.
   */
  String getDestinationUuid();

}
