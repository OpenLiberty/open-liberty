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

/**
 * Interface to manipulate the stream of incoming messages from a remote 
 * messaging engine.
 * This is remote PUT receiving (as opposed to remote GET receiving)
 * @author tpm100
 */
public interface SIMPDeliveryStreamReceiverControllable extends SIMPDeliveryReceiverControllable
{

  /**
   * @return the reliability of this target stream.
   * @author tpm
   */
  Reliability getReliability();
  
  /**
   * @return the priority of this target stream.
   * @author tpm
   */
  int getPriority();
  
  /**
   * @return the number of active messages on the target stream.
   * @author tpm
   */
  int getNumberOfActiveMessages();
  
  /**
   * Get an iterator over all of the active received messages from this
   * remote messaging engine
   * @return an iterator containing SIMPReceivedMessageControllable objects.
   */
  public SIMPIterator getReceivedMessageIterator(int maxMsgs);
  
  /**
   * Return the given message with the corresponding id
   * @param id
   * @return
   */
  public SIMPReceivedMessageControllable getReceivedMessageByID(String id);
  
  /**
   * @return the sequence id of the last message to be delivered to this
   * receiver.
   */
  public long getLastDeliveredMessageSequenceId();
  
  /**
   * Get the number of messages received by this stream since the ME was last started
   * @return noOfMsgs
   */
  public long getNumberOfMessagesReceived();

}
