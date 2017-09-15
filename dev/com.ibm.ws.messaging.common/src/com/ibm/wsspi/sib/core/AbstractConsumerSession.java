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

package com.ibm.wsspi.sib.core;

import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 AbstractConsumerSession is the parent of ConsumerSession and 
 BifurcatedConsumerSession, containing exactly those methods that are common
 to both. It is used as one of the arguments to the Core SPI MDB's 
 onMessage method, enabling the signature to be common on distributed platforms
 (non-bifurcated) and on z/OS (the bifurcated case).  
 <p>
 This class has no direct security implications. ConsumerSession and
 BifurcatedConsumerSession extend this class, see those classes for
 further security implications.

***/
public interface AbstractConsumerSession extends DestinationSession 
{

  /**
   Acknowledges receipt of the messages whose identifiers are given in 
   msgHandles. The elements of the array msgHandles should be obtained using 
   SIBusMessage.getMessageHandle(). Each element in the array is processed in 
   turn, and is either deleted (if no transaction is supplied) or its deletion 
   is added to the unit of work. If a handle is encountered that does not 
   represent a message locked for this consumer, then processing continues with 
   next message, but SIMessageNotLockedException will be thrown once the whole
   array has been examined. This will leave some messages deleted, and other 
   messages locked. The method SIMessageNotLockedException.getUnlockedMesssages 
   can be used to determine which messages were not deleted. If there was a 
   transaction, the caller can decide whether to commit the delete of the 
   processed messages, or to roll back, leaving them all messages unlocked.
      
   @param msgHandles identifies the messages to be deleted
   @param tran the transaction under which the delete is to occur
  
   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.wsspi.sib.core.exception.SILimitExceededException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException
  */
  public void deleteSet(
      SIMessageHandle[] msgHandles, 
      SITransaction tran)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException, SILimitExceededException,
           SIIncorrectCallException, 
           SIMessageNotLockedException;
  
  /**
   Unlocks the messages whose identiifed by the msgHandles parameter. The 
   elements of the array msgHandles should be obtained using 
   SIBusMessage.getMessageHandle(). If any of the msgHandles does not identify 
   a message locked for the consumer, then SIMessageNotLockedException is thrown 
   after unlocking all messages for which valid ids handles given.
   <p>
   It should be noted that any invocation of unlockSet can cause messages to be 
   delivered out of sequence (just as with a transaction rollback). When a
   message is unlocked in this way, its redeliveryCount is incremented.
  
   @param msgHandles identifies the messages to be deleted
  
   @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
   @throws com.ibm.websphere.sib.exception.SIResourceException
   @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
   @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
   @throws com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException
  */
  public void unlockSet(SIMessageHandle[] msgHandles)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException, 
           SIIncorrectCallException, 
           SIMessageNotLockedException;

  /**
  Same function as public void unlockSet(SIMessageHandle[] msgHandles) but adds
  the ability to indicate if the lock count should be incremented or not for this
  message. The lock count will have an impact on the delivery count.
 
  @param msgHandles identifies the messages to be unlocked
  @param incrementLockCount indicates whether the lock conet should be incremented
 
  @throws com.ibm.wsspi.sib.core.exception.SISessionUnavailableException
  @throws com.ibm.wsspi.sib.core.exception.SISessionDroppedException
  @throws com.ibm.wsspi.sib.core.exception.SIConnectionUnavailableException
  @throws com.ibm.wsspi.sib.core.exception.SIConnectionDroppedException
  @throws com.ibm.websphere.sib.exception.SIResourceException
  @throws com.ibm.wsspi.sib.core.exception.SIConnectionLostException
  @throws com.ibm.websphere.sib.exception.SIIncorrectCallException
  @throws com.ibm.wsspi.sib.core.exception.SIMessageNotLockedException
 */
  public void unlockSet(SIMessageHandle[] msgHandles, boolean incrementLockCount)
    throws SISessionUnavailableException, SISessionDroppedException,
           SIConnectionUnavailableException, SIConnectionDroppedException,
           SIResourceException, SIConnectionLostException, 
           SIIncorrectCallException, 
           SIMessageNotLockedException;
}
