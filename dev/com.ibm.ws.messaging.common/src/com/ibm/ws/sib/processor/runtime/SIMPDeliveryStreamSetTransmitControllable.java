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
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;

/**
 * Interface to manipulate the set of streams of outgoing (remote put) 
 * messages going to a particular messaging engine
 */
public interface SIMPDeliveryStreamSetTransmitControllable extends SIMPDeliveryStreamSetControllable
{

  /**
   * Indicates whether the workload manager has made a decision about where to deliver the messages in
   *  the stream or whether they are just guesses.
   *
   * @return boolean  True if the messages in the stream are guesses, that is the workload manager did 
   *         not explicitly decide they should go to the localisation by the stream. 
   *         False if the messages are not guesses and the Workload manager has made a final choice
   *         about sending them to the chosen localisation.
   */
  boolean containsGuesses()
    throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException;

  /**
   * The maximum number of messages this stream set will permit at this priority and class of
   *  service. It may be less than GDConfig.GD_MAX_INDOUBT_MESSAGES if WLM currently has no 
   *  knowledge of the target. It may be more than GDConfig.GD_MAX_INDOUBT_MESSAGES if the 
   *  administrator has recently reduced GDConfig.GD_MAX_INDOUBT_MESSAGES.
   *
   * @param priority  is the priority of the ticks to be retrieved.
   * @param COS       is the class of service of the messages to be retrieved.
   * 
   * @return int the current maximum number of indoubt messages 
   */
  int getCurrentMaxIndoubtMessages(int priority, int COS);

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
   * @return The current aggregate number of messages awaiting transmission on 
   * these streams.
   * @author tpm
   */
  long getDepth() throws SIMPControllableNotFoundException;    
  
  /**
   * @return a SIMPIterator containing the SIMPDeliveryStreamTransmitControllable
   * streams in this stream set.
   * @author tpm
   */
  SIMPIterator getStreams()throws SIMPControllableNotFoundException;
  
  /**
   * 
   * @return the total number of messages that have been sent on this 
   * source stream
   */
  long getNumberOfMessagesSent() 
    throws SIMPControllableNotFoundException; 
  
  /**
   * Get the number of msgs on this trasmit itemstream that have been sent but are awaiting
   * acknowledgement from the remote ME
   * @return
   */
  long getNumberOfUnacknowledgedMessages();
  
  /**
   * Move all messages (either acknowledged or not) from this itemstream
   * If discard is set to true, delete them - else move to the exception destination 
   * @param discard
   * @throws SIMPControllableNotFoundException 
   * @throws SIMPRuntimeOperationFailedException 
   */
  void moveMessages (boolean discard) 
    throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException;
  
}
