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

import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;

/**
 * Interface to manipulate an individual stream of outgoing messages from this
 * messaging engine.
 * This is for transmitted (remote put) messages as opposed to 
 * requested (remote get) messages.
 * @author tpm100
 */
public interface SIMPDeliveryStreamTransmitControllable extends SIMPDeliveryTransmitControllable
{
  
  /**
   * @return the reliability of this source stream.
   * @author tpm
   */
  Reliability getReliability();
  
  /**
   * @return the priority of this source stream.
   * @author tpm
   */
  int getPriority();
  
  /**
   * @return the number of active messages on the source stream.
   * @author tpm
   */
  int getNumberOfActiveMessages();
  
  /**
   * @param id
   * @return a SIMPTransmitMessageControllable for the message with
   * the specified ID
   * @throws SIMPRuntimeOperationFailedException
   * @author tpm
   */
  SIMPTransmitMessageControllable getTransmitMessageByID(String id) 
    throws SIMPRuntimeOperationFailedException;
  
  /**
   * @return a SIMPIterator that enumerates the active messages awaiting
   * transmission.
   * Throws up SIMPTransmitMessageControllable
   * @author tpm
   */
  SIMPIterator getTransmitMessagesIterator(int maxMsgs);
  
  /**
   * @return a long for the total number of messages that have been
   * sent from this particular stream
   * @author tpm
   */
  long getNumberOfMessagesSent();
  
  /**
   * Move the message (either acked or unacked) with the corresponding ID.
   * If discard is set to true, delete it - else move to the exception destination
   * @param msgId
   * @param discard
   */  
  void moveMessage(String msgId, boolean discard) 
  throws SIMPRuntimeOperationFailedException, SIMPControllableNotFoundException;  
  

}
